package local.prominence.work;
import com.dripps.voxyserver.network.*;
import io.netty.buffer.Unpooled;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import java.util.*;
public final class LodBulkDecodeTests {
 static int checks;
 static void ok(boolean x,String msg){checks++;if(!x)throw new AssertionError(msg);}
 static byte[] encode(LODBulkPayload value){var raw=Unpooled.buffer();try{var b=new class_2540(raw);LODBulkPayload.CODEC.encode(b,value);byte[] data=new byte[b.readableBytes()];b.readBytes(data);return data;}finally{raw.release();}}
 static LODBulkPayload nativeRead(byte[] data){var raw=Unpooled.wrappedBuffer(data);try{return LODBulkPayload.CODEC.decode(new class_2540(raw));}finally{raw.release();}}
 static LODBulkPayload optimized(byte[] data){var raw=Unpooled.wrappedBuffer(data);try{return LodBulkDecode.read(new class_2540(raw));}finally{raw.release();}}
 static void same(LODBulkPayload a,LODBulkPayload b){
  ok(a.dimension().equals(b.dimension()) && a.sections().size()==b.sections().size(),"header");
  for(int i=0;i<a.sections().size();i++){
   var x=a.sections().get(i);var y=b.sections().get(i);
   ok(x.dimension().equals(y.dimension()) && x.sectionKey()==y.sectionKey() && x.contentHash()==y.contentHash(),"metadata");
   ok(Arrays.equals(x.lutBlockStateIds(),y.lutBlockStateIds())&&Arrays.equals(x.lutBiomeIds(),y.lutBiomeIds())&&Arrays.equals(x.lutLight(),y.lutLight())&&Arrays.equals(x.indexArray(),y.indexArray()),"content");
  }
 }
 public static void main(String[] args){
  var rnd=new Random(909);var dim=new class_2960("minecraft","overworld");
  for(int trial=0;trial<100;trial++){
   int lut=1+rnd.nextInt(80),n=trial%4==0?32768:1+rnd.nextInt(500);var sections=new ArrayList<LODSectionPayload>();
   for(int j=0;j<1+trial%5;j++){
    int[] blocks=new int[lut],biomes=new int[lut];byte[] light=new byte[lut];short[] indices=new short[n];
    for(int k=0;k<lut;k++){blocks[k]=rnd.nextInt(9000);biomes[k]=rnd.nextInt(300);light[k]=(byte)rnd.nextInt();}
    for(int k=0;k<n;k++)indices[k]=(short)rnd.nextInt(lut);
    sections.add(new LODSectionPayload(dim,rnd.nextLong(),blocks,biomes,light,indices,rnd.nextLong()));
   }
   byte[] wire=encode(new LODBulkPayload(dim,sections));var expected=nativeRead(wire);
   try(var scope=RemoteIngestScratch.begin()){
    var actual=optimized(wire);same(expected,actual);
    if(n==32768 && actual.sections().size()>1)ok(actual.sections().get(0).indexArray()!=actual.sections().get(1).indexArray(),"alias within batch");
    for(int cut:new int[]{0,wire.length/2,wire.length-1}){
      byte[] broken=Arrays.copyOf(wire,cut);boolean nfail=false,ofail=false;
      try{nativeRead(broken);}catch(RuntimeException e){nfail=true;}try{optimized(broken);}catch(RuntimeException e){ofail=true;}ok(nfail&&ofail,"truncated accepted");
    }
   }
  }
  short[] first;try(var a=RemoteIngestScratch.begin()){first=RemoteIngestScratch.indices(32768);var other=RemoteIngestScratch.indices(32768);ok(first!=other,"borrow alias");try(var nested=RemoteIngestScratch.begin()){ok(RemoteIngestScratch.indices(32768)!=first,"nested alias");}}
  try(var a=RemoteIngestScratch.begin()){var n=RemoteIngestScratch.indices(32768);ok(n.length==32768,"recycle length");}
  ok(RemoteIngestScratch.indices(9)!=RemoteIngestScratch.indices(9),"unscoped");
  var ids=Collections.newSetFromMap(new IdentityHashMap<short[],Boolean>());
  for(int t=0;t<100;t++){try(var scope=RemoteIngestScratch.begin()){for(int i=0;i<16;i++)ids.add(RemoteIngestScratch.indices(32768));}}
  ok(ids.size()<=32,"pool failed to recycle: "+ids.size());
  var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
  try {var latch=new java.util.concurrent.CountDownLatch(2);var gate=new java.util.concurrent.CountDownLatch(1);
   java.util.concurrent.Callable<short[]> task=()->{try(var scope=RemoteIngestScratch.begin()){short[] data=RemoteIngestScratch.indices(32768);latch.countDown();gate.await();return data;}};
   var a=pool.submit(task);var b=pool.submit(task);latch.await();gate.countDown();ok(a.get()!=b.get(),"concurrent alias");
  }catch(Exception e){throw new AssertionError(e);}finally{pool.shutdown();}
  System.out.println("PASS "+checks+" actual native wire codec vs scoped decoder; randomized batches, bounds, truncation, nesting; no game/world");
 }
}
