package local.prominence.memory;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Correct only two verified native lifecycle handlers, after their mixin merged. */
public final class BetterDaysLifecycleRewrite {
    private BetterDaysLifecycleRewrite() {}
    private static final String SOURCE="betterdays.mixin.MinecraftClientMixin";
    private static final String OWNER="betterdays/client/TimeInterpolator";
    private static final String DESC="(Lnet/minecraft/class_1936;)V";

    private static boolean nativeHandler(MethodNode method) {
        if(method.visibleAnnotations==null)return false;
        for(AnnotationNode annotation:method.visibleAnnotations) {
            if(!annotation.desc.equals("Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;")||annotation.values==null)continue;
            for(int i=0;i<annotation.values.size()-1;i+=2)
                if(annotation.values.get(i).equals("mixin")&&annotation.values.get(i+1).equals(SOURCE))return true;
        }
        return false;
    }

    /** Atomic validation: return zero and leave native code intact on any mismatch. */
    public static int rewrite(ClassNode target) {
        if(!target.name.equals("net/minecraft/class_310"))return 0;
        List<MethodInsnNode> calls=new ArrayList<>();
        int set=0,clear=0;
        for(MethodNode method:target.methods) {
            if(!nativeHandler(method))continue;
            boolean isSet=method.name.contains("$betterdaysInjectSetLevel")
                &&method.desc.equals("(Lnet/minecraft/class_638;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");
            boolean isClear=method.name.contains("$betterdaysInjectClearLevel")
                &&method.desc.equals("(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");
            if(!isSet&&!isClear)continue;
            if(isSet)set++;else clear++;
            int count=0;
            for(var instruction:method.instructions) {
                if(instruction instanceof MethodInsnNode call && call.owner.equals(OWNER)) {
                    if(call.getOpcode()!=Opcodes.INVOKESTATIC||!call.name.equals("onWorldLoad")||!call.desc.equals(DESC))return 0;
                    calls.add(call);count++;
                }
            }
            if(count!=1)return 0;
        }
        if(set!=1||clear!=1||calls.size()!=2)return 0;
        for(var call:calls)call.name="onWorldUnload";
        return calls.size();
    }
}
