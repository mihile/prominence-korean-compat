package reachverification;

import java.nio.file.Files;
import java.nio.file.Path;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

/** Copies native method bodies into API-double shells; never launches Minecraft. */
public final class PrepareReachFixture {
    private static ClassNode read(byte[] data) { ClassNode node = new ClassNode(); new ClassReader(data).accept(node, 0); return node; }
    private static byte[] write(ClassNode node) { ClassWriter writer = new ClassWriter(0); node.accept(writer); return writer.toByteArray(); }
    public static void main(String[] args) throws Exception {
        Path work = Path.of(args[0]), classes = Path.of(args[1]);
        String target = "net/bettercombat/client/collision/TargetFinder.class";
        byte[] shell = Files.readAllBytes(classes.resolve(target));
        for (String mode : new String[]{"native", "fixed"}) {
            var node = read(shell); node.methods.removeIf(m -> m.name.equals("findAttackTargetResult"));
            var source = read(Files.readAllBytes(work.resolve(mode + "-target.class")));
            for (var method : source.methods) if (method.name.equals("findAttackTargetResult")
                    || method.name.contains("includeAttackReach") || method.name.equals("applyAttackRangeModifiers")
                    || method.name.startsWith("lambda$applyAttackRangeModifiers")) node.methods.add(method);
            Path file = work.resolve(mode + "-fixture").resolve(target); Files.createDirectories(file.getParent()); Files.write(file, write(node));
        }
        String api = "com/jamieswhiteshirt/reachentityattributes/ReachEntityAttributes.class";
        var node = read(Files.readAllBytes(classes.resolve(api)));
        node.methods.removeIf(m -> m.name.equals("getAttackRange"));
        var source = read(Files.readAllBytes(work.resolve("pins/reach-entity-attributes").resolve(api)));
        node.methods.add(source.methods.stream().filter(m -> m.name.equals("getAttackRange")).findFirst().orElseThrow());
        Files.write(classes.resolve(api), write(node));
        System.out.println("Prepared actual native/fixed targeting methods and actual native reach API with small world/geometry doubles");
    }
}
