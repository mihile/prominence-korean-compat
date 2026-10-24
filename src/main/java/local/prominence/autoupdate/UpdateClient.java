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
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UpdateClient implements ClientModInitializer {
    public enum Phase{AVAILABLE,DOWNLOADING,QUEUED,FAILED}
    private static final Logger LOG=LoggerFactory.getLogger("Prominence updater");
    private static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"Prominence Update Check");t.setDaemon(true);return t;});
    private static final AtomicBoolean CHECKING=new AtomicBoolean(),INSTALLING=new AtomicBoolean();
    private static final Set<Long> SHOWN=new HashSet<>();
    static volatile Phase phase=Phase.AVAILABLE;
    static volatile long downloaded;
    static volatile SignedRelease release;
    static long build;
    static String label;
    private static String helperHash;
    private static Path game,target,root,preferences;
    private static String originalHash;
    private static boolean enabled=true;
    private static volatile SignedRelease pending;
    private static long lastCheck;
    private static int joinedTicks;

    @Override public void onInitializeClient(){
        try{
            var loader=FabricLoader.getInstance();game=loader.getGameDir().toAbsolutePath().normalize();
            target=UpdateFiles.target(game);root=UpdateFiles.directory(game.resolve(".prominence-update"));
            var origin=loader.getModContainer("prominence_korean_compat").orElseThrow().getOrigin().getPaths();
            if(origin.size()!=1 || !Files.isSameFile(target,origin.get(0)))throw new IOException("Compatibility JAR must use the original overwrite filename");
            originalHash=UpdateFiles.sha(target);
            try(var in=UpdateClient.class.getResourceAsStream("/assets/prominence_korean_compat/update-build.properties")){
                var p=SignedRelease.properties(Objects.requireNonNull(in).readAllBytes());build=Long.parseLong(SignedRelease.required(p,"build"));label=SignedRelease.required(p,"label");helperHash=SignedRelease.required(p,"helperSha256");
            }
            preferences=loader.getConfigDir().resolve("prominence-korean-compat-updater.properties");
            if(Files.isRegularFile(preferences))enabled=Boolean.parseBoolean(SignedRelease.properties(Files.readAllBytes(preferences)).getProperty("enabled","true"));
            ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->{joinedTicks=0;if(enabled)check(false);});
            ClientTickEvents.END_CLIENT_TICK.register(client->{
                if(client.field_1687==null || client.field_1724==null){joinedTicks=0;return;}
                joinedTicks++;
                SignedRelease offer=pending;
                if(offer!=null && joinedTicks>=40 && client.field_1755==null){
                    pending=null;if(SHOWN.add(offer.build()))client.method_1507(new UpdateScreen(offer));
                }
            });
            ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->dispatcher.register(ClientCommandManager.literal("kpatch")
                .executes(c->{check(true);return 1;})
                .then(ClientCommandManager.literal("update").executes(c->{check(true);return 1;}))
                .then(ClientCommandManager.literal("auto")
                    .then(ClientCommandManager.literal("on").executes(c->{setEnabled(true);return 1;}))
                    .then(ClientCommandManager.literal("off").executes(c->{setEnabled(false);return 1;})))));
            LOG.info("Signed client updater ready: build={}, automatic={}; server and launcher hooks unchanged",build,enabled);
            IO.execute(UpdateClient::reportPrevious);
        }catch(Exception e){LOG.warn("Automatic updater disabled; compatibility features are unaffected",e);}
    }
    private static void setEnabled(boolean value){
        try{Properties p=new Properties();p.setProperty("enabled",Boolean.toString(value));UpdateFiles.writeProperties(preferences,p);enabled=value;if(!value)pending=null;message(value?"auto_on":"auto_off");}
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
    private static void check(boolean manual){
        if(phase==Phase.QUEUED || phase==Phase.DOWNLOADING){if(manual)message(phase==Phase.QUEUED?"ready":"downloading");return;}
        if(!manual && System.nanoTime()-lastCheck<TimeUnit.MINUTES.toNanos(3) && lastCheck!=0)return;
        if(!CHECKING.compareAndSet(false,true))return;
        lastCheck=System.nanoTime();
        IO.execute(()->{
            try{
                SignedRelease next=SignedRelease.verify(new SafeHttp().manifest(),SignedRelease.publicKey());
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
            }catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();LOG.warn("Update check unavailable; no files changed: {}",e.getClass().getSimpleName());if(manual)message("check_failed");}
            finally{CHECKING.set(false);}
        });
    }
    static void prepare(SignedRelease next){
        if(next.build()<=build)return;
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
                var self=ProcessHandle.current();Properties p=new Properties();p.setProperty("game",game.toRealPath().toString());p.setProperty("build",Long.toString(next.build()));p.setProperty("oldSha256",originalHash);p.setProperty("parentPid",Long.toString(self.pid()));p.setProperty("parentStarted",self.info().startInstant().orElseThrow().toString());
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
