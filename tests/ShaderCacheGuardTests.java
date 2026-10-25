package local.prominence.memory;

import java.lang.reflect.Method;

public final class ShaderCacheGuardTests {
    public static void main(String[] args) throws Exception {
        Method check=ShaderCacheGuard.class.getDeclaredMethod("dependencyClass",String.class,String.class);
        check.setAccessible(true);
        String target="io/github/douira/glsl_transformer/ast/transform/ASTParser.class";
        String good="219242ef81e466432fa6423027e45d5c05407849f616686ccaae805825c5c264";
        if(!(Boolean)check.invoke(null,target,good))throw new AssertionError("Shipped parser fingerprint rejected");
        if((Boolean)check.invoke(null,target,"0".repeat(64)))throw new AssertionError("Changed parser accepted");
        if((Boolean)check.invoke(null,"missing/parser.class",good))throw new AssertionError("Missing parser accepted");
        String previous=System.getProperty("prominence.shaderParseCacheRelease");
        try {
            System.setProperty("prominence.shaderParseCacheRelease","false");
            ShaderCacheGuard guard=new ShaderCacheGuard();guard.onLoad("local.prominence.memory.mixin");
            if(guard.shouldApplyMixin("net.irisshaders.iris.pipeline.IrisRenderingPipeline","IrisPipelineCacheLifetimeMixin"))
                throw new AssertionError("Explicit opt-out ignored");
        } finally {
            if(previous==null)System.clearProperty("prominence.shaderParseCacheRelease");
            else System.setProperty("prominence.shaderParseCacheRelease",previous);
        }
        System.out.println("PASS: 4 shader cache fingerprint/missing-target/opt-out checks; no game");
    }
}
