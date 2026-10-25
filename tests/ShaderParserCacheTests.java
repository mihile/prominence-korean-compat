package local.prominence.memory;

import io.github.douira.glsl_transformer.ast.print.ASTPrinter;
import io.github.douira.glsl_transformer.ast.query.RootSupplier;
import io.github.douira.glsl_transformer.ast.transform.ASTParser;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

/** Runs the shipped GLSL parser, never Minecraft/OpenGL or a mock parsing algorithm. */
public final class ShaderParserCacheTests {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static Object field(Object object, Class<?> type, String name) throws Exception {
        Field field=type.getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    private static String parse(ASTParser parser, String source) {
        return ASTPrinter.printSimple(parser.parseTranslationUnit(RootSupplier.DEFAULT.get(), source));
    }
    public static void main(String[] args) throws Exception {
        ASTParser parser=new ASTParser();
        parser.parseExternalDeclaration(RootSupplier.DEFAULT.get(), "uniform float exposure;");
        Object buildCache=field(parser, ASTParser.class, "buildCache");
        int buildSize=((Map<?,?>)buildCache).size();
        Object enhanced=field(parser, ASTParser.class, "parser");
        Object cache=field(enhanced, enhanced.getClass(), "parseCache");
        Field cap=cache.getClass().getSuperclass().getDeclaredField("maxSize"); cap.setAccessible(true);
        int limit=(Integer)cap.get(cache);
        var sources=new ArrayList<String>();
        var expected=new ArrayList<String>();
        for(int i=0;i<180;i++) {
            String source="#version 330 core\nuniform sampler2D tex; out vec4 col; void main(){ float v="+i+".0; col=vec4(v); }\n";
            sources.add(source); expected.add(parse(parser,source));
        }
        int before=((Map<?,?>)cache).size();
        check(before>=180, "Real CST cache was not populated");
        check(ShaderParseCacheRelease.clearCompletedParser(parser)==before, "Wrong removed count");
        check(((Map<?,?>)cache).isEmpty(), "CST entries still retained");
        check(cache==field(enhanced, enhanced.getClass(), "parseCache"), "Native cache replaced");
        check((Integer)cap.get(cache)==limit && limit==400, "Native LRU policy changed");
        check(buildCache==field(parser, ASTParser.class, "buildCache"), "AST snippet cache replaced");
        check(((Map<?,?>)buildCache).size()==buildSize, "AST snippet entries removed");
        for(int i=0;i<sources.size();i++) check(parse(parser,sources.get(i)).equals(expected.get(i)), "GLSL output changed at "+i);
        check(((Map<?,?>)cache).size()>=180, "Subsequent shader builds cannot reparse");
        check(ShaderParseCacheRelease.clearCompletedParser(parser)>=180, "Repeated release failed");
        check(ShaderParseCacheRelease.clearCompletedParser(parser)==0, "Empty cache not harmless");
        parser.setParsingCacheStrategy(ASTParser.ParsingCacheStrategy.NONE);
        check(ShaderParseCacheRelease.clearCompletedParser(parser)==0, "Uncached parser must remain untouched");
        check(parse(parser,sources.get(0)).equals(expected.get(0)), "Uncached parsing changed");
        System.out.println("PASS: "+checks+" real GLSL parser cache/output/lifecycle checks; no game or OpenGL");
    }
}
