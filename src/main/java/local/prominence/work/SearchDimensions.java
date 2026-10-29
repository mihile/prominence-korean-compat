package local.prominence.work;
import java.util.*;import java.util.function.Function;
public final class SearchDimensions {
 private SearchDimensions(){}
 public static String append(String displayed,String id,List<String> dimensions,Function<String,String> translated){var text=new StringBuilder(displayed==null?"":displayed);text.append('\0').append(id).append('\0').append(id.substring(id.indexOf(':')+1).replace('_',' '));
 for(String d:dimensions){text.append('\0').append(d).append('\0').append(translated.apply(d));if(d.equals("minecraft:the_nether"))text.append("\0네더 지옥 nether");else if(d.equals("minecraft:the_end"))text.append("\0엔드 엔더 end");else if(d.equals("minecraft:overworld"))text.append("\0오버월드 지상 overworld");}return text.toString();}
}
