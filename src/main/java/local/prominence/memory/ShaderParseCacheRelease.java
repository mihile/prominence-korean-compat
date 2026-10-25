package local.prominence.memory;

import java.lang.reflect.Field;
import java.util.Map;
import org.slf4j.LoggerFactory;

/** Drops only rebuildable CST/token scratch data after all pipeline shaders are built. */
public final class ShaderParseCacheRelease {
    private static boolean failed;
    private ShaderParseCacheRelease() {}

    public static void afterPipelineBuilt() {
        if (failed) return;
        try {
            // This runs on the same thread as Iris' completed constructor. No background clear,
            // no tick polling, no global ASTParser instance, and no forced garbage collection.
            Class<?> patcher = Class.forName("net.irisshaders.iris.pipeline.transform.TransformPatcher",
                false, ShaderParseCacheRelease.class.getClassLoader());
            Field field = patcher.getDeclaredField("transformer");
            field.setAccessible(true);
            int removed = clearCompletedParser(field.get(null));
            LoggerFactory.getLogger("Prominence compatibility").info(
                "Client shader parser scratch cache released: entries={} (native result cache and compiled shaders kept)", removed);
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            LoggerFactory.getLogger("Prominence compatibility").warn(
                "Cannot release Iris parser scratch cache; disabling release and retaining native behavior", e);
        }
    }

    static int clearCompletedParser(Object transformer) throws ReflectiveOperationException {
        ClassLoader loader = transformer.getClass().getClassLoader();
        Class<?> astParser = Class.forName("io.github.douira.glsl_transformer.ast.transform.ASTParser", false, loader);
        Field parserField = astParser.getDeclaredField("parser");
        parserField.setAccessible(true);
        Object parser = parserField.get(transformer);
        Class<?> cachingParser = Class.forName("io.github.douira.glsl_transformer.parser.CachingParser", false, loader);
        if (!cachingParser.isInstance(parser)) return 0;
        Field cacheField = cachingParser.getDeclaredField("parseCache");
        cacheField.setAccessible(true);
        Map<?, ?> cache = (Map<?, ?>) cacheField.get(parser);
        int entries = cache.size();
        // Keep the existing map, its capacity and eviction policy. Only obsolete entries go.
        // Small reusable AST snippets live in a different buildCache and remain untouched.
        cache.clear();
        return entries;
    }
}
