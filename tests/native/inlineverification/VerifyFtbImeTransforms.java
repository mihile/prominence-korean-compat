package inlineverification;
import batchverification.VerifyMixinTransforms;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.io.File;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.*;

public final class VerifyFtbImeTransforms {
    static List<String> calls(MethodNode method){var out=new ArrayList<String>();for(var i:method.instructions)if(i instanceof MethodInsnNode c)out.add(c.owner+"."+c.name+c.desc);return out;}
    static ClassNode read(ClassLoader loader,String target)throws Exception{try(var in=loader.getResourceAsStream(target.replace('.','/')+".class")){var n=new ClassNode();new ClassReader(in.readAllBytes()).accept(n,0);return n;}}
    public static void main(String[]args)throws Exception{
        Path root=Path.of(args[0]),work=Path.of(args[1]);Files.createDirectories(work);Path configs=work.resolve("configs");Files.createDirectories(configs);
        Files.writeString(configs.resolve("offline-ftb-ime.mixins.json"),Files.readString(root.resolve("prominence-ftb-ime.client.mixins.json")).replace("local.prominence.ftbime.FtbImeGuard",VerifyMixinTransforms.OfflinePlugin.class.getName()));
        var urls=new ArrayList<URL>();urls.add(configs.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))if(!p.isBlank())urls.add(Path.of(p).toUri().toURL());
        var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyFtbImeTransforms.class.getClassLoader());var f=VerifyMixinTransforms.class.getDeclaredField("LOADER");f.setAccessible(true);f.set(null,resources);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        Mixins.addConfiguration("offline-ftb-ime.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");f=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");f.setAccessible(true);var transformer=(IMixinTransformer)f.get(null);
        int verified=0;
        for(String target:List.of("dev.ftb.mods.ftblibrary.ui.Widget","dev.ftb.mods.ftblibrary.ui.TextBox")){
            byte[] raw;try(var in=resources.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
            byte[] fixed=transformer.transformClassBytes(target,target,raw);if(Arrays.equals(raw,fixed))throw new AssertionError("No transform "+target);
            var before=new ClassNode();var after=new ClassNode();new ClassReader(raw).accept(before,0);new ClassReader(fixed).accept(after,0);
            for(var method:after.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name,method);verified++;}
            if(target.endsWith("TextBox")){
                if(!after.interfaces.contains("local/prominence/ftbime/ImeTarget"))throw new AssertionError("Target API not added");
                for(String name:List.of("setFocused","setText","insertText","deleteCharsToPos","setCursorPosition","setCursorPos","setSelectionPos","draw")){
                    if(after.methods.stream().filter(m->m.name.equals(name)).noneMatch(m->calls(m).stream().anyMatch(c->c.contains("prominence$"))))throw new AssertionError("Missing hook "+name);
                }
                for(String name:List.of("keyPressed","charTyped","onTextChanged","isValid")){
                    var a=before.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();var b=after.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();if(!calls(a).equals(calls(b)))throw new AssertionError("Native keyboard/validation path changed "+name);
                }
                var preview=after.methods.stream().filter(m->m.name.equals("prominence$imePreview")).findFirst().orElseThrow();
                if(calls(preview).stream().anyMatch(c->c.contains("onTextChanged")||c.contains(".setText")))throw new AssertionError("Preview must not dispatch search changes");
                var insert=after.methods.stream().filter(m->m.name.equals("prominence$imeInsert")).findFirst().orElseThrow();
                if(calls(insert).stream().noneMatch(c->c.contains("TextBox.insertText")))throw new AssertionError("Commit must delegate to native FTB insertion");
            }else{
                var close=after.methods.stream().filter(m->m.name.equals("onClosed")).findFirst().orElseThrow();if(calls(close).stream().noneMatch(c->c.contains("releaseIme")))throw new AssertionError("Closed widgets retain native focus");
            }
            var file=work.resolve(target.replace('.','/')+".class");Files.createDirectories(file.getParent());Files.write(file,fixed);
        }
        // Verify the user's Ctrl+F path reaches the shared custom TextBox, not a vanilla edit box.
        var quests=read(resources,"dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen");
        if(quests.methods.stream().filter(m->m.name.equals("keyPressed")).noneMatch(m->calls(m).stream().anyMatch(c->c.contains("openQuestSelectionGUI"))))throw new AssertionError("Quest search entry changed");
        var selection=read(resources,"dev.ftb.mods.ftbquests.client.gui.SelectQuestObjectScreen");
        if(!selection.superName.equals("dev/ftb/mods/ftblibrary/ui/misc/AbstractButtonListScreen"))throw new AssertionError("Search inheritance changed");
        var list=read(resources,"dev.ftb.mods.ftblibrary.ui.misc.AbstractButtonListScreen");
        if(list.fields.stream().noneMatch(field->field.name.equals("searchBox")&&field.desc.equals("Ldev/ftb/mods/ftblibrary/ui/TextBox;")))throw new AssertionError("Search input changed");
        Files.writeString(work.resolve("report.json"),"{\"status\":\"passed\",\"nativeTargets\":2,\"nativeMethodsVerified\":"+verified+",\"ctrlFCustomTextBoxPathVerified\":true,\"nativeKeyCharValidationPathsPreserved\":true,\"gameStarted\":false}");
        System.out.println("PASS 2 real FTB Mixin targets / "+verified+" verified methods; Ctrl+F custom TextBox, focus/edit/caret/closure hooks and native keyboard/validation preservation");
    }
}
