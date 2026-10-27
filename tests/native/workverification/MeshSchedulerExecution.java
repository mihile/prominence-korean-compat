package workverification;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import me.cortex.voxy.common.thread.ServiceManager;
import me.cortex.voxy.common.util.Pair;
import local.prominence.work.InitialWork;
import local.prominence.work.VoxyMeshAdmission;
/** Exercises the transformed real Voxy scheduler with bounded jobs, no game/world/GL. */
public final class MeshSchedulerExecution {
 static void ok(boolean x,String msg){if(!x)throw new AssertionError(msg);}
 public static void main(String[] a)throws Exception {
  var m=new ServiceManager(n->{});var entered=new AtomicInteger();var peak=new AtomicInteger();var completed=new AtomicInteger();var aux=new AtomicInteger();var release=new CountDownLatch(1);
  var mesh=m.createService(()->new Pair<Runnable,Runnable>(() -> {int n=entered.incrementAndGet();peak.accumulateAndGet(n,Math::max);try{release.await();}catch(InterruptedException e){throw new RuntimeException(e);}finally{entered.decrementAndGet();completed.incrementAndGet();}},()->{}),10,"Section mesh generation service",VoxyMeshAdmission::allowed);
  var other=m.createService(()->new Pair<Runnable,Runnable>(aux::incrementAndGet,()->{}),10,"other");
  for(int i=0;i<24;i++){mesh.execute();other.execute();}
  var pool=Executors.newFixedThreadPool(12);var stop=new AtomicBoolean();
  try(var initial=InitialWork.begin()){
   for(int t=0;t<12;t++)pool.submit(()->{while(!stop.get()){int x=m.tryRunAJob();if(x!=0)try{Thread.sleep(1);}catch(InterruptedException e){Thread.currentThread().interrupt();return;}}});
   long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
   while((entered.get()<4 || aux.get()<24) && System.nanoTime()<until)Thread.sleep(5);
   ok(entered.get()==4,"mesh cap not applied: "+entered.get());ok(aux.get()==24,"other service starved: "+aux.get());ok(peak.get()==4,"race exceeded cap: "+peak.get());
   release.countDown();until=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
   while(completed.get()<24 && System.nanoTime()<until)Thread.sleep(5);
   ok(completed.get()==24 && mesh.numJobs()==0 && other.numJobs()==0,"jobs lost or stuck: "+completed.get());
  }finally{release.countDown();stop.set(true);pool.shutdownNow();ok(pool.awaitTermination(10,TimeUnit.SECONDS),"worker threads stuck");}
  ok(VoxyMeshAdmission.allowed(),"native throughput not restored");ok(VoxyMeshAdmission.enter(),"admission stuck");VoxyMeshAdmission.leave();
  var nativePool=new me.cortex.voxy.common.thread.UnifiedServiceThreadPool();nativePool.setNumThreads(8);
  var active=new AtomicInteger();var peak2=new AtomicInteger();var done=new AtomicInteger();var auxDone=new AtomicInteger();var gate=new CountDownLatch(1);
  var s=nativePool.serviceManager.createService(()->new Pair<Runnable,Runnable>(()->{int n=active.incrementAndGet();peak2.accumulateAndGet(n,Math::max);try{gate.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}finally{active.decrementAndGet();done.incrementAndGet();}},()->{}),10,"Section mesh generation service",VoxyMeshAdmission::allowed);
  var o=nativePool.serviceManager.createService(()->new Pair<Runnable,Runnable>(auxDone::incrementAndGet,()->{}),10,"other");
  try(var scope=InitialWork.begin()){
   for(int i=0;i<24;i++){s.execute();o.execute();}
   long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);while((active.get()<4||auxDone.get()<24)&&System.nanoTime()<end)Thread.sleep(5);
   ok(active.get()==4 && peak2.get()==4,"native pool failed cap: "+active.get());ok(auxDone.get()==24,"native pool starved other work: "+auxDone.get());
   gate.countDown();end=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);while(done.get()<24&&System.nanoTime()<end)Thread.sleep(5);ok(done.get()==24,"native pool lost tasks: "+done.get());
  }finally{gate.countDown();s.shutdown();o.shutdown();nativePool.shutdown();}
  var closing=new me.cortex.voxy.common.thread.UnifiedServiceThreadPool();closing.setNumThreads(8);
  var held=new CountDownLatch(1);var started=new AtomicInteger();
  var svc=closing.serviceManager.createService(()->new Pair<Runnable,Runnable>(()->{started.incrementAndGet();try{held.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}},()->{}),10,"Section mesh generation service",VoxyMeshAdmission::allowed);
  try(var scope=InitialWork.begin()){
   for(int i=0;i<30;i++)svc.execute();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);while(started.get()<4&&System.nanoTime()<end)Thread.sleep(5);ok(started.get()==4,"shutdown fixture not busy");
   var executor=Executors.newSingleThreadExecutor();try{var stopped=executor.submit(svc::shutdown);Thread.sleep(100);held.countDown();int skipped=stopped.get(10,TimeUnit.SECONDS);ok(skipped>=0,"shutdown drain failed");}finally{held.countDown();executor.shutdownNow();}
  }finally{held.countDown();closing.shutdown();}
  var restoring=new me.cortex.voxy.common.thread.UnifiedServiceThreadPool();restoring.setNumThreads(8);
  var running=new AtomicInteger();var total=new AtomicInteger();var unblock=new CountDownLatch(1);
  var restored=restoring.serviceManager.createService(()->new Pair<Runnable,Runnable>(()->{running.incrementAndGet();try{unblock.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}finally{running.decrementAndGet();total.incrementAndGet();}},()->{}),10,"Section mesh generation service",VoxyMeshAdmission::allowed);
  try {var scope=InitialWork.begin();for(int i=0;i<8;i++)restored.execute();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);while(running.get()<4 && System.nanoTime()<end)Thread.sleep(5);ok(running.get()==4,"initial cap absent");scope.close();end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);while(running.get()<8 && System.nanoTime()<end)Thread.sleep(5);ok(running.get()==8,"normal concurrency not restored: "+running.get());unblock.countDown();end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);while(total.get()<8 && System.nanoTime()<end)Thread.sleep(5);ok(total.get()==8,"restored work did not finish");
  }finally{unblock.countDown();restored.shutdown();restoring.shutdown();}
  System.out.println("PASS actual native scheduler + transformed service: cap=4, 24 mesh and 24 unrelated jobs, no starvation, counters or lost work; no game/world/GL");
 }
}
