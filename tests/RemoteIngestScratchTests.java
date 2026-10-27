package local.prominence.work;
import me.cortex.voxy.common.voxelization.VoxelizedSection;
import java.util.concurrent.*;
public final class RemoteIngestScratchTests {
 static int n; static void ok(boolean x){if(!x)throw new AssertionError("check "+n);n++;}
 public static void main(String[] a)throws Exception{
  VoxelizedSection outside=RemoteIngestScratch.acquire();ok(outside!=RemoteIngestScratch.acquire());
  VoxelizedSection outer;
  try(var scope=RemoteIngestScratch.begin()){
   outer=RemoteIngestScratch.acquire();outer.section[0]=123;outer.section[outer.section.length-1]=456;outer.lvl0NonAirCount=5;outer.setPosition(1,2,3);
   var reused=RemoteIngestScratch.acquire();ok(outer==reused);ok(reused.section[0]==0 && reused.section[reused.section.length-1]==0 && reused.lvl0NonAirCount==0);
   try(var inner=RemoteIngestScratch.begin()){ok(RemoteIngestScratch.acquire()!=outer);}
   ok(RemoteIngestScratch.acquire()==outer);
   var ex=Executors.newSingleThreadExecutor();try {ok(ex.submit(()->{try(var t=RemoteIngestScratch.begin()){return RemoteIngestScratch.acquire()!=outer;}}).get());}finally{ex.shutdown();}
  }
  ok(RemoteIngestScratch.acquire()!=outer);
  try{try(var scope=RemoteIngestScratch.begin()){RemoteIngestScratch.acquire();throw new IllegalStateException();}}catch(IllegalStateException expected){}
  ok(RemoteIngestScratch.acquire()!=RemoteIngestScratch.acquire());
  var random=new java.util.Random(91731);
  try(var scope=RemoteIngestScratch.begin()){
   for(int iteration=0;iteration<120;iteration++){
    long[] input=new long[32768];for(int i=0;i<input.length;i++)input[i]=random.nextInt(9)==0?0:random.nextInt(4000)+1;
    for(int y=0;y<2;y++)for(int z=0;z<2;z++)for(int x=0;x<2;x++){
     var fresh=VoxelizedSection.createEmpty();var reused=RemoteIngestScratch.acquire();fresh.setPosition(x,y,z);reused.setPosition(x,y,z);
     int count=0;
     for(int vy=0;vy<16;vy++)for(int vz=0;vz<16;vz++)for(int vx=0;vx<16;vx++){
      int src=((y*16+vy)<<10)|((z*16+vz)<<5)|(x*16+vx),dst=(vy<<8)|(vz<<4)|vx;
      long id=input[src];fresh.section[dst]=id;reused.section[dst]=id;if(id!=0)count++;
     }
     fresh.lvl0NonAirCount=reused.lvl0NonAirCount=count;
     ok(java.util.Arrays.equals(fresh.section,reused.section) && fresh.lvl0NonAirCount==reused.lvl0NonAirCount);
     reused.section[reused.section.length-1]=99; // simulate derived mip data from prior use
    }
   }
  }
  System.out.println("PASS "+n+" native voxel section scope/reset/thread/exception checks; no game or world");
 }
}
