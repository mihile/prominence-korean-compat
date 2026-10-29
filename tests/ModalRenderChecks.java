import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Check the actual compiled render paths, without initializing Minecraft or a window. */
public final class ModalRenderChecks {
    static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    static ClassNode read(Path root,String name)throws Exception{
        ClassNode node=new ClassNode();new ClassReader(Files.readAllBytes(root.resolve("local/prominence/autoupdate/"+name+".class"))).accept(node,0);return node;
    }
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]);int checks=0;
        MethodNode bg=read(root,"UpdateRender").methods.stream().filter(m->m.name.equals("background")).findFirst().orElseThrow();
        List<String> calls=new ArrayList<>();
        for(var i:bg.instructions)if(i instanceof MethodInsnNode m)calls.add(m.owner+"."+m.name);
        int flush=calls.indexOf("net/minecraft/class_332.method_51452"),overlay=calls.indexOf("net/minecraft/class_332.method_25294");
        check(calls.stream().noneMatch(s->s.startsWith("net/minecraft/class_437.")),"Parent render/FancyMenu lifecycle is still entered");checks++;
        check(calls.contains("local/prominence/autoupdate/UpdateBackdrop.draw"),"Captured menu is missing");checks++;
        check(flush>=0 && overlay>flush,"Missing independent background boundary");checks++;
        List<Integer> backgroundColors=new ArrayList<>();for(var i:bg.instructions)if(i instanceof LdcInsnNode l&&l.cst instanceof Integer color)backgroundColors.add(color);
        check(backgroundColors.stream().anyMatch(c->(c>>>24)>0 && (c>>>24)<255),"No translucent dim layer above the menu snapshot");checks++;
        MethodNode panel=read(root,"UpdateRender").methods.stream().filter(m->m.name.equals("panel")).findFirst().orElseThrow();
        List<Integer> colors=new ArrayList<>();for(var i:panel.instructions)if(i instanceof LdcInsnNode l&&l.cst instanceof Integer color)colors.add(color);
        check(colors.size()==3 && colors.stream().allMatch(c->(c>>>24)==255),"Panel leaks parent pixels");checks++;
        for(String name:List.of("UpdateScreen","UpdateCheckScreen")){
            MethodNode render=read(root,name).methods.stream().filter(m->m.name.equals("method_25394")).findFirst().orElseThrow();
            calls=new ArrayList<>();
            for(var i:render.instructions)if(i instanceof MethodInsnNode m){
                calls.add(m.owner+"."+m.name);
                check(!(m.owner.equals("net/minecraft/class_437")&&m.name.equals("method_25394")&&m.getOpcode()!=Opcodes.INVOKESPECIAL),"Direct parent render bypassed batch boundary");
            }
            check(calls.get(0).equals("local/prominence/autoupdate/UpdateRender.background"),"Modal draws before parent boundary: "+name);checks++;
            check(calls.contains("local/prominence/autoupdate/UpdateRender.panel"),"Missing opaque panel: "+name);checks++;
            check(calls.get(calls.size()-1).equals("net/minecraft/class_332.method_51452"),"Modal leaves deferred text/widgets behind: "+name);checks++;
            check(read(root,name).methods.stream().anyMatch(m->m.name.equals("method_25419")),"Parent navigation removed");checks++;
        }
        var backdrop=read(root,"UpdateBackdrop");
        check(backdrop.methods.stream().flatMap(m->java.util.stream.StreamSupport.stream(m.instructions.spliterator(),false)).anyMatch(i->i instanceof MethodInsnNode call && call.name.equals("glCopyTexImage2D")),"Menu capture is missing GPU copy");checks++;
        check(backdrop.methods.stream().flatMap(m->java.util.stream.StreamSupport.stream(m.instructions.spliterator(),false)).noneMatch(i->i instanceof MethodInsnNode call && call.name.equals("glReadPixels")),"CPU screenshot readback introduced");checks++;
        check(read(root,"UpdateModalScreen").methods.stream().filter(m->m.name.equals("method_25432")).anyMatch(m->java.util.stream.StreamSupport.stream(m.instructions.spliterator(),false).anyMatch(i->i instanceof MethodInsnNode call && call.name.equals("release"))),"Closing modal leaks capture");checks++;
        var client=read(root,"UpdateClient");
        check(client.methods.stream().flatMap(m->java.util.stream.StreamSupport.stream(m.instructions.spliterator(),false)).noneMatch(i->i instanceof FieldInsnNode field && field.owner.contains("ClientPlayConnectionEvents") && field.name.equals("JOIN")),"World join update callback remains");checks++;
        check(client.methods.stream().flatMap(m->java.util.stream.StreamSupport.stream(m.instructions.spliterator(),false)).anyMatch(i->i instanceof MethodInsnNode call && call.owner.endsWith("ScreenEvents") && call.name.equals("afterRender")),"No completed-menu-frame guard before snapshot");checks++;
        System.out.println("PASS: "+checks+" compiled popup snapshot/dim/release/startup-only boundaries; no game initialized.");
    }
}
