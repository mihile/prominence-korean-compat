package reachverification;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import local.prominence.combatreach.AttackReachGuard;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;

public final class ReachGuardChecks {
    private static int checks;
    private record Mod(String version, Path root) implements ModContainer {
        public ModMetadata getMetadata() { return () -> () -> version; }
        public Optional<Path> findPath(String path) { Path file = root.resolve(path); return Files.exists(file) ? Optional.of(file) : Optional.empty(); }
    }
    private static void expect(boolean expected) {
        var guard = new AttackReachGuard(); guard.onLoad("local.prominence.combatreach.mixin");
        if (guard.shouldApplyMixin("target", "mixin") != expected) throw new AssertionError("guard check " + checks);
        checks++;
    }
    public static void main(String[] args) throws Exception {
        Path pins = Path.of(args[0]);
        Map<String, ModContainer> mods = new HashMap<>();
        mods.put("minecraft", new Mod("1.20.1", pins));
        mods.put("bettercombat", new Mod("1.9.0+1.20.1", pins.resolve("bettercombat")));
        mods.put("reach-entity-attributes", new Mod("2.4.0", pins.resolve("reach-entity-attributes")));
        FabricLoader.Holder.INSTANCE = id -> Optional.ofNullable(mods.get(id));
        System.clearProperty("prominence.combat.reach"); expect(true);
        System.setProperty("prominence.combat.reach", "false"); expect(false);
        System.clearProperty("prominence.combat.reach");
        for (String id : new String[]{"minecraft", "bettercombat", "reach-entity-attributes"}) {
            var original = mods.remove(id); expect(false); mods.put(id, original);
            mods.put(id, new Mod("unknown-version", ((Mod) original).root())); expect(false); mods.put(id, original);
        }
        for (var id : new String[]{"bettercombat", "reach-entity-attributes"}) {
            Path file;
            try (var paths = Files.walk(pins.resolve(id))) { file = paths.filter(Files::isRegularFile).findFirst().orElseThrow(); }
            byte[] bytes = Files.readAllBytes(file);
            try { byte[] bad = bytes.clone(); bad[0] ^= 1; Files.write(file, bad); expect(false); }
            finally { Files.write(file, bytes); }
            Path moved = file.resolveSibling(file.getFileName() + ".hidden"); Files.move(file, moved);
            try { expect(false); } finally { Files.move(moved, file); }
        }
        expect(true);
        FabricLoader.Holder.INSTANCE = id -> { throw new NoClassDefFoundError("fixture dependency missing"); };
        expect(false);
        System.out.println("PASS: " + checks + " production guard activation/version/hash/missing-dependency/linkage/opt-out checks");
    }
}
