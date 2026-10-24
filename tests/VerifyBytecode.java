import java.nio.file.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
public final class VerifyBytecode {
    public static void main(String[] args)throws Exception {
        int classes=0,methods=0;
        try(var paths=Files.walk(Path.of(args[0]))){
            for(Path file:paths.filter(p->p.toString().endsWith(".class")).toList()){
                ClassNode node=new ClassNode();new ClassReader(Files.readAllBytes(file)).accept(node,ClassReader.EXPAND_FRAMES);if(node.version!=Opcodes.V17)throw new AssertionError("Not Java17");
                for(var m:node.methods)if((m.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))==0){new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,m);methods++;}
                classes++;
            }
        }
        System.out.println("PASS: "+classes+" Java17 updater classes / "+methods+" methods; ASM BasicVerifier; no game classes loaded.");
    }
}
