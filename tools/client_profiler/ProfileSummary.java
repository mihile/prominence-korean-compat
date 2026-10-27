import jdk.jfr.consumer.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Offline streaming JFR summary. Heap occupancy is not process/system RAM. */
public final class ProfileSummary {
    static String trace(RecordedEvent e){
        if(e.getStackTrace()==null)return "";
        var names=new ArrayList<String>();for(var f:e.getStackTrace().getFrames())names.add(f.getMethod().getType().getName()+"."+f.getMethod().getName()+":"+f.getLineNumber());
        return String.join(" <- ",names);
    }
    static void rows(Path p,Map<String,Long> values,String header)throws Exception{
        try(var out=Files.newBufferedWriter(p)){out.write(header+"\n");for(var row:values.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).toList())out.write(row.getValue()+"\t"+row.getKey()+"\n");}
    }
    public static void main(String[]args)throws Exception{
        Path out=Path.of(args[1]);Files.createDirectories(out);long maxHeap=Long.parseLong(args[2]),peakAfter=0;double pauses=0,maxPause=0;int full=0;
        Map<String,Long> stacks=new HashMap<>(),allocations=new HashMap<>();Set<Long> firstAllocation=new HashSet<>();
        try(var file=new RecordingFile(Path.of(args[0]));var heap=Files.newBufferedWriter(out.resolve("heap.tsv"));var gc=Files.newBufferedWriter(out.resolve("gc.tsv"));var cpu=Files.newBufferedWriter(out.resolve("cpu.tsv"));var waits=Files.newBufferedWriter(out.resolve("render-waits.tsv"))){
            heap.write("time\twhen\tusedBytes\tmaxHeapBytes\tpercentOfMax\n");gc.write("time\tevent\tdurationMs\tname\tcause\n");cpu.write("time\tjvmUser\tjvmSystem\tmachineTotal\n");waits.write("time\tevent\tdurationMs\tstack\n");
            while(file.hasMoreEvents()){
                var e=file.readEvent();String type=e.getEventType().getName();
                if(type.equals("jdk.GCHeapSummary")){
                    long used=e.getLong("heapUsed");String when=e.getString("when");if(when.equals("After GC"))peakAfter=Math.max(peakAfter,used);
                    heap.write(e.getStartTime()+"\t"+when+"\t"+used+"\t"+maxHeap+"\t"+(maxHeap==0?"unknown":100.0*used/maxHeap)+"\n");
                }else if(type.equals("jdk.GCPhasePause")){
                    double ms=e.getDuration().toNanos()/1e6;pauses+=ms;maxPause=Math.max(maxPause,ms);gc.write(e.getStartTime()+"\t"+type+"\t"+ms+"\t\t\n");
                }else if(type.equals("jdk.GarbageCollection")){
                    String name=e.getString("name");if(name.equals("G1Full")||name.equals("MarkSweepCompact")||name.equals("PSMarkSweep"))full++;
                    gc.write(e.getStartTime()+"\t"+type+"\t"+e.getDuration().toNanos()/1e6+"\t"+name+"\t"+e.getString("cause")+"\n");
                }else if(type.equals("jdk.CPULoad"))cpu.write(e.getStartTime()+"\t"+e.getFloat("jvmUser")+"\t"+e.getFloat("jvmSystem")+"\t"+e.getFloat("machineTotal")+"\n");
                else if(type.equals("jdk.ExecutionSample")||type.equals("jdk.NativeMethodSample")){
                    var thread=e.getThread("sampledThread");if(thread!=null)stacks.merge(thread.getJavaName()+"\t"+trace(e),1L,Long::sum);
                }else if(type.equals("jdk.ObjectAllocationSample")){
                    var thread=e.getThread();if(thread!=null&&firstAllocation.add(thread.getJavaThreadId()))continue;
                    allocations.merge((thread==null?"unknown":thread.getJavaName())+"\t"+e.getClass("objectClass").getName()+"\t"+trace(e),e.getLong("weight"),Long::sum);
                }else if(Set.of("jdk.ThreadPark","jdk.JavaMonitorEnter","jdk.FileRead","jdk.FileWrite").contains(type)){
                    var thread=e.getThread();if(thread!=null&&thread.getJavaName().equals("Render thread"))waits.write(e.getStartTime()+"\t"+type+"\t"+e.getDuration().toNanos()/1e6+"\t"+trace(e)+"\n");
                }
            }
        }
        rows(out.resolve("execution.tsv"),stacks,"samples\tthread\tstack");rows(out.resolve("allocation.tsv"),allocations,"estimatedBytes\tthread\tclass\tstack");
        Files.writeString(out.resolve("heap-summary.json"),"{\"maxHeapBytes\":"+maxHeap+",\"peakAfterGcBytes\":"+peakAfter+",\"gcPauseMs\":"+pauses+",\"longestGcPauseMs\":"+maxPause+",\"fullGcDuringRecording\":"+full+",\"heapDumpExcluded\":true,\"systemRamUsedAsHeapEvidence\":false}");
        System.out.println("Saved offline JFR/heap summary; heap limit="+maxHeap+", peak after GC="+peakAfter);
    }
}
