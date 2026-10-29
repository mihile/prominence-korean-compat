package local.prominence.autoupdate;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_437;
import net.minecraft.class_442;
import net.minecraft.class_4185;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UpdateClient implements ClientModInitializer {
    public enum Phase{AVAILABLE,DOWNLOADING,QUEUED,FAILED}
    private static final Logger LOG=LoggerFactory.getLogger("Prominence updater");
    private static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"Prominence Update Check");t.setDaemon(true);return t;});
    private static final AtomicBoolean INSTALLING=new AtomicBoolean();
    private static CompletableFuture<SignedRelease> inFlight;
    private static final Set<Long> SHOWN=new HashSet<>();
    private static class_437 renderedTitle;
    static volatile Phase phase=Phase.AVAILABLE;
    static volatile long downloaded;
    static volatile SignedRelease release;
    static long build;
    static String label;
    private static String helperHash;
    private static Path game,target,root,preferences;
    private static String originalHash;
    private static boolean enabled=true;
    static String track="main", installedTrack="main";
    private static String inFlightTrack;
    private static volatile SignedRelease pending;
    private static long lastCheck;
    private static int readyTicks;

    @Override public void onInitializeClient(){
        try{
            var loader=FabricLoader.getInstance();game=loader.getGameDir().toAbsolutePath().normalize();
            if(loader.isModLoaded("fancymenu")){
                try{FancyMenuIsolation.register();LOG.info("Updater screen isolation registered with FancyMenu");}
                catch(ReflectiveOperationException|LinkageError e){LOG.warn("FancyMenu screen isolation unavailable; independent updater rendering remains enabled",e);}
            }
            target=UpdateFiles.target(game);root=UpdateFiles.directory(game.resolve(".prominence-update"));
            var origin=loader.getModContainer("prominence_korean_compat").orElseThrow().getOrigin().getPaths();
            if(origin.size()!=1 || !Files.isSameFile(target,origin.get(0)))throw new IOException("Compatibility JAR must use the original overwrite filename");
            originalHash=UpdateFiles.sha(target);
            try(var in=UpdateClient.class.getResourceAsStream("/assets/prominence_korean_compat/update-build.properties")){
                var p=SignedRelease.properties(Objects.requireNonNull(in).readAllBytes());build=Long.parseLong(SignedRelease.required(p,"build"));label=SignedRelease.required(p,"label");helperHash=SignedRelease.required(p,"helperSha256");
            }
            installedTrack=UpdateFiles.installedTrack(target);
            preferences=loader.getConfigDir().resolve("prominence-korean-compat-updater.properties");
            if(Files.isRegularFile(preferences)){
                Properties pref=SignedRelease.properties(Files.readAllBytes(preferences));
                enabled=Boolean.parseBoolean(pref.getProperty("enabled","true"));
                if("nightly".equals(pref.getProperty("track")))track="nightly";
            }
            ClientLifecycleEvents.CLIENT_STARTED.register(client->{if(enabled)check(false);});
            ScreenEvents.AFTER_INIT.register((client,screen,width,height)->{
                if(screen instanceof class_442){
                    renderedTitle=null;
                    // Fabric recreates per-screen events on init/resize, so register on every init.
                    ScreenEvents.afterRender(screen).register((active,draw,mx,my,delta)->{
                        if(client.field_1755==active && client.method_18506()==null)renderedTitle=active;
                    });
                    var buttons=Screens.getButtons(screen);
                    int x=Math.max(6,width-104),y=6;
                    // Prefer an unused corner over existing native menu controls.
                    for(int candidate:new int[]{6,32,58,height-48}){
                        boolean used=false;
                        for(var button:buttons)if(button.field_22764 && x<button.method_46426()+button.method_25368() && x+98>button.method_46426() && candidate<button.method_46427()+button.method_25364() && candidate+20>button.method_46427()){used=true;break;}
                        if(!used){y=candidate;break;}
                    }
                    buttons.add(class_4185.method_46430(class_2561.method_43471("prominence.update.menu"),b->openCheck(screen)).method_46434(x,y,98,20).method_46431());
                    if(enabled)check(false);
                }
            });
            ClientTickEvents.END_CLIENT_TICK.register(client->{
                boolean title=client.field_1755 instanceof class_442;
                boolean world=client.field_1687!=null,player=client.field_1724!=null,noScreen=client.field_1755==null,overlay=client.method_18506()!=null;
                if(title && client.field_1755==renderedTitle && !world && !player && !overlay)readyTicks=1;else readyTicks=0;
                if(!title || overlay)renderedTitle=null;
                SignedRelease offer=pending;
                if(offer!=null && PromptGate.canOpen(title,world,player,noScreen,overlay,readyTicks)){
                    pending=null;if(SHOWN.add(offer.build()))client.method_1507(new UpdateScreen(offer,client.field_1755));
                }
            });
            ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->dispatcher.register(ClientCommandManager.literal("kpatch")
                .executes(c->{check(true);return 1;})
                .then(ClientCommandManager.literal("update").executes(c->{check(true);return 1;}))
                .then(ClientCommandManager.literal("auto")
                    .then(ClientCommandManager.literal("on").executes(c->{setEnabled(true);return 1;}))
                    .then(ClientCommandManager.literal("off").executes(c->{setEnabled(false);return 1;})))));
            LOG.info("Signed client updater ready: build={}, automatic={}; startup and title-menu checks enabled; server and launcher hooks unchanged",build,enabled);
            IO.execute(UpdateClient::reportPrevious);
        }catch(Exception e){LOG.warn("Automatic updater disabled; compatibility features are unaffected",e);}
    }
    private static void setEnabled(boolean value){
        try{Properties p=new Properties();p.setProperty("track",track);p.setProperty("enabled",Boolean.toString(value));UpdateFiles.writeProperties(preferences,p);enabled=value;if(!value)pending=null;message(value?"auto_on":"auto_off");}
        catch(IOException e){LOG.warn("Cannot save updater preference",e);message("failed");}
    }
    private static void reportPrevious(){
        try{
            Path p=root.resolve("last-update.properties");if(!Files.isRegularFile(p))return;
            Properties receipt=SignedRelease.properties(Files.readAllBytes(p));LOG.info("Previous update result: status={}, build={}",receipt.getProperty("status"),receipt.getProperty("build"));
        }catch(IOException e){LOG.debug("Cannot read previous update receipt",e);}
    }
    private static void message(String key){
        class_310.method_1551().execute(()->{var player=class_310.method_1551().field_1724;if(player!=null)player.method_7353(class_2561.method_43471("prominence.update."+key),false);});
    }
    static void setTrack(String selected)throws IOException {
        if(!selected.equals("main") && !selected.equals("nightly"))throw new IOException("Invalid track");
        Properties p=new Properties();p.setProperty("enabled",Boolean.toString(enabled));p.setProperty("track",selected);
        UpdateFiles.writeProperties(preferences,p);track=selected;pending=null;lastCheck=0;SHOWN.clear();
    }
    private static synchronized CompletableFuture<SignedRelease> request(){
        String selected=track;
        if(inFlight==null || inFlight.isDone() || !selected.equals(inFlightTrack)){
            inFlightTrack=selected;inFlight=CompletableFuture.supplyAsync(()->{
            try{SignedRelease result=SignedRelease.verify(new SafeHttp().manifest(selected),SignedRelease.publicKey());if(!result.track().equals(selected))throw new IOException("Wrong update track");return result;}
            catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new CompletionException(e);}
        },IO);}
        return inFlight;
    }
    static void openCheck(class_437 parent){
        var client=class_310.method_1551();
        if(release!=null && (phase==Phase.QUEUED || phase==Phase.DOWNLOADING)){client.method_1507(new UpdateScreen(release,parent));return;}
        var screen=new UpdateCheckScreen(parent);client.method_1507(screen);lookup(screen,parent);
    }
    static void openChoose(class_437 parent){
        var client=class_310.method_1551();
        if(phase==Phase.QUEUED || phase==Phase.DOWNLOADING)return;
        client.method_1507(new UpdateCheckScreen(parent));
    }
    static void lookup(UpdateCheckScreen screen,class_437 parent){
        var client=class_310.method_1551();
        screen.checking();
        LOG.info("Checking client update from title/menu control: track={}",track);
        request().whenComplete((next,error)->client.execute(()->{
            if(client.field_1755!=screen || !screen.track().equals(track))return;
            if(error!=null){LOG.warn("Menu update check unavailable; current JAR unchanged",error);screen.failed();return;}
            LOG.info("Menu update result: installed={}, published={}",build,next.build());
            if(next.build()<build && !(track.equals("main") && installedTrack.equals("nightly"))){screen.latest();return;}
            release=next;SHOWN.add(next.build());pending=null;
            if(phase!=Phase.QUEUED && phase!=Phase.DOWNLOADING)phase=Phase.AVAILABLE;
            client.method_1507(new UpdateScreen(next,parent));
        }));
    }
    private static void check(boolean manual){
        String requested=track;
        if(phase==Phase.QUEUED || phase==Phase.DOWNLOADING){if(manual)message(phase==Phase.QUEUED?"ready":"downloading");return;}
        if(!manual && System.nanoTime()-lastCheck<TimeUnit.MINUTES.toNanos(3) && lastCheck!=0)return;
        lastCheck=System.nanoTime();
        LOG.info("Checking client update: source={}",manual?"command":"startup/title");
        request().whenComplete((next,error)->class_310.method_1551().execute(()->{
                if(!requested.equals(track))return;
                if(error!=null){LOG.warn("Update check unavailable; no files changed",error);if(manual)message("check_failed");return;}
                LOG.info("Client update result: installed={}, published={}",build,next.build());
                if(next.build()<=build){
                    if(manual && next.build()==build){
                        release=next;class_310.method_1551().execute(()->{SHOWN.remove(next.build());pending=next;});
                    }else if(manual)message("latest");
                    return;
                }
                release=next;
                class_310.method_1551().execute(()->{
                    if(!manual && !enabled)return;
                    if(manual)SHOWN.remove(next.build());
                    if(phase!=Phase.QUEUED && phase!=Phase.DOWNLOADING)phase=Phase.AVAILABLE;
                    pending=next;
                });
        }));
    }
    static void prepare(SignedRelease next){
        if(next.build()<=build && !(track.equals("main") && installedTrack.equals("nightly") && next.track().equals("main")))return;
        if(!INSTALLING.compareAndSet(false,true))return;
        phase=Phase.DOWNLOADING;downloaded=0;
        IO.execute(()->{
            try{
                if(!UpdateFiles.sha(target).equals(originalHash))throw new IOException("Installed JAR was changed externally");
                Path plan=root.resolve("pending.properties");
                if(Files.exists(plan)){
                    Path pidFile=root.resolve("worker.properties");
                    if(Files.isRegularFile(pidFile)){
                        Properties p=SignedRelease.properties(Files.readAllBytes(pidFile));
                        var handle=ProcessHandle.of(Long.parseLong(p.getProperty("pid","-1")));
                        if(handle.isPresent() && handle.get().isAlive() && handle.get().info().startInstant().map(i->i.toString().equals(p.getProperty("started"))).orElse(false)){phase=Phase.QUEUED;return;}
                    }
                }
                new SafeHttp().download(next,root.resolve("incoming"),n->downloaded=n);
                Path helpers=UpdateFiles.directory(root.resolve("helpers"));Path helper=helpers.resolve("apply-"+helperHash+".jar");
                if(!Files.exists(helper))try(var in=UpdateClient.class.getResourceAsStream("/assets/prominence_korean_compat/update-helper.jar")){
                    if(in==null)throw new IOException("Missing updater helper");Files.copy(in,helper);
                }
                if(Files.isSymbolicLink(helper) || !UpdateFiles.sha(helper).equals(helperHash))throw new IOException("Invalid bundled helper");
                UpdateFiles.writeBytes(root.resolve("manifest.properties"),next.envelope());
                var self=ProcessHandle.current();Properties p=new Properties();p.setProperty("approvedMainReturn",Boolean.toString(next.build()<=build && track.equals("main") && installedTrack.equals("nightly") && next.track().equals("main")));p.setProperty("game",game.toRealPath().toString());p.setProperty("build",Long.toString(next.build()));p.setProperty("oldSha256",originalHash);p.setProperty("parentPid",Long.toString(self.pid()));p.setProperty("parentStarted",self.info().startInstant().orElseThrow().toString());
                UpdateFiles.writeProperties(plan,p);
                Path java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name","").startsWith("Windows")?"javaw.exe":"java");
                if(!Files.isRegularFile(java))throw new IOException("Background Java runtime is missing");
                if(Files.isSymbolicLink(root.resolve("apply.log")))throw new IOException("Linked update log is not supported");
                Process child=new ProcessBuilder(java.toString(),"-Xms8m","-Xmx64m","-Djava.awt.headless=true","-cp",helper.toString(),ApplyUpdate.class.getName(),plan.toString())
                    .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(root.resolve("apply.log").toFile())).start();
                Properties worker=new Properties();worker.setProperty("pid",Long.toString(child.pid()));worker.setProperty("started",child.toHandle().info().startInstant().orElseThrow().toString());UpdateFiles.writeProperties(root.resolve("worker.properties"),worker);
                Thread.sleep(350);
                if(!child.isAlive())throw new IOException("Update helper stopped before game exit");
                phase=Phase.QUEUED;LOG.info("Verified update queued: build={}; replacement waits for this JVM to exit",next.build());
            }catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();phase=Phase.FAILED;LOG.warn("Update preparation failed; current JAR preserved",e);}
            finally{INSTALLING.set(false);}
        });
    }
}
