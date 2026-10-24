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
        check(!bg.desc.contains("class_437"),"Background still accepts a parent Screen");checks++;
        check(flush>=0 && overlay>flush,"Missing independent background boundary");checks++;
        List<Integer> backgroundColors=new ArrayList<>();for(var i:bg.instructions)if(i instanceof LdcInsnNode l&&l.cst instanceof Integer color)backgroundColors.add(color);
        check(backgroundColors.size()==1 && (backgroundColors.get(0)>>>24)==255,"Full background still exposes the previous screen");checks++;
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
        System.out.println("PASS: "+checks+" compiled modal render boundary checks; no game initialized.");
    }
}
