import jdk.jfr.consumer.*;
import java.nio.file.*;
import java.util.*;
public class TraceWindows {
 public static void main(String[] args)throws Exception {
  Map<String,Map<String,Integer>> windows=new TreeMap<>();
  try(var file=new RecordingFile(Path.of(args[0]))) {
   while(file.hasMoreEvents()) {
    var e=file.readEvent();String type=e.getEventType().getName();
    if(!type.equals("jdk.ExecutionSample")&&!type.equals("jdk.NativeMethodSample"))continue;
    RecordedThread t=e.getThread("sampledThread");if(t==null||e.getStackTrace()==null)continue;
    String name=t.getJavaName();if(!name.equals("Render thread")&&!name.contains("shader prewarm"))continue;
    String time=e.getStartTime().toString();String bucket=time.substring(0,18)+"0Z "+name;
    StringBuilder stack=new StringBuilder();int n=0;
    for(var f:e.getStackTrace().getFrames()) {if(n++==12)break;var m=f.getMethod();stack.append(m.getType().getName()).append('.').append(m.getName()).append(':').append(f.getLineNumber()).append(" <- ");}
    windows.computeIfAbsent(bucket,k->new HashMap<>()).merge(stack.toString(),1,Integer::sum);
   }
  }
  for(var w:windows.entrySet()) {System.out.println("WINDOW "+w.getKey());w.getValue().entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed()).limit(8).forEach(x->System.out.println(x.getValue()+"\t"+x.getKey()));}
 }
}
