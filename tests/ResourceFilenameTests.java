package local.prominence.join;
import java.util.*;
public final class ResourceFilenameTests {
    static int checks;
    static void same(Object... args){String expected=String.format(Locale.ROOT,"%s/%s/%s",args);String actual=ResourceFilename.tryFormat(Locale.ROOT,"%s/%s/%s",args);checks++;if(!expected.equals(actual))throw new AssertionError(expected+" != "+actual);}
    public static void main(String[] args){
        String[] values={"assets","data","minecraft","a/b/c","textures/block/test_n.png","한글/경로","%s","",null,"a\nb","a\tb","\uD800","é"};
        for(String a:values)for(String b:values)for(String c:values)same(a,b,c);
        if(ResourceFilename.tryFormat(Locale.KOREAN,"%s/%s/%s",new Object[]{"a","b","c"})!=null)throw new AssertionError("locale fallback");
        if(ResourceFilename.tryFormat(Locale.ROOT,"%d",new Object[]{1})!=null)throw new AssertionError("format fallback");
        if(ResourceFilename.tryFormat(Locale.ROOT,"%s/%s/%s",new Object[]{new Object(),"b","c"})!=null)throw new AssertionError("custom formatting fallback");
        if(ResourceFilename.tryFormat(Locale.ROOT,"%s/%s/%s",null)!=null)throw new AssertionError("null array fallback");
        if(ResourceFilename.tryFormat(Locale.ROOT,"%s/%s/%s",new Object[]{"a"})!=null)throw new AssertionError("arity fallback");
        System.out.println("PASS "+checks+" native format equivalents; null, Unicode, separators, placeholders; unknown inputs keep original format. No resource cache or game.");
    }
}
