package local.prominence.memory;
import java.util.ArrayList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Only source-identified MMV visual callbacks; never entity save/network/breeding code. */
public final class VariantNbtRewrite {
    private VariantNbtRewrite(){}
    private static boolean visualSource(MethodNode method) {
        if(method.visibleAnnotations==null)return false;
        for(var a:method.visibleAnnotations)if(a.desc.equals("Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;")&&a.values!=null)
            for(int i=0;i<a.values.size()-1;i+=2)if(a.values.get(i).equals("mixin")&&a.values.get(i+1) instanceof String s)
                if(s.startsWith("com.github.nyuppo.mixin.")&&(s.endsWith("RendererMixin")||s.endsWith("CustomEyesFeatureMixin")||s.endsWith("SheepWoolFeatureMixin")))return true;
        return false;
    }
    public static int rewrite(ClassNode target) {
        var calls=new ArrayList<MethodInsnNode>();
        for(var method:target.methods)if(visualSource(method))for(var i:method.instructions)
            if(i instanceof MethodInsnNode call&&call.getOpcode()==Opcodes.INVOKEVIRTUAL&&call.owner.startsWith("net/minecraft/")) {
                if(call.name.equals("method_5647")&&call.desc.equals("(Lnet/minecraft/class_2487;)Lnet/minecraft/class_2487;"))calls.add(call);
                if(call.name.equals("method_5652")&&call.desc.equals("(Lnet/minecraft/class_2487;)V"))calls.add(call);
            }
        if(calls.size()!=1)return 0;
        var call=calls.get(0);boolean full=call.name.equals("method_5647");
        call.setOpcode(Opcodes.INVOKESTATIC);call.owner="local/prominence/memory/VariantProjection";
        call.name=full?"writeFull":"writeCustom";
        call.desc=full?"(Lnet/minecraft/class_1297;Lnet/minecraft/class_2487;)Lnet/minecraft/class_2487;":"(Lnet/minecraft/class_1309;Lnet/minecraft/class_2487;)V";
        call.itf=false;return 1;
    }
}
