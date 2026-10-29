package local.prominence.rewards.client;
import java.util.concurrent.atomic.AtomicInteger;
public final class RewardPollCacheTests {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[] args){
  var c=new RewardPollCache<Integer>();var calls=new AtomicInteger();Object player=new Object(),other=new Object();
  for(long n=0;n<1_000_000_000L;n+=1_000_000L)c.poll(player,n,calls::incrementAndGet);
  check(calls.get()==4,"1000 hovers require only 4 equipment queries per second");
  check(c.poll(other,999_000_000L,calls::incrementAndGet)==5,"player replacement refreshes immediately");
  check(c.poll(other,1,calls::incrementAndGet)==6,"reversed clock cannot pin stale gear");
  c.clear();check(c.poll(other,2,calls::incrementAndGet)==7,"disconnect invalidates gear");
  byte[] packet={1,2,3};check(!c.duplicate(player,packet),"unvalidated packet is not cached");c.accepted(player,packet);
  check(c.duplicate(player,new byte[]{1,2,3}),"identical accepted bytes skipped");check(!c.duplicate(other,packet),"new connection must install same data");check(!c.duplicate(player,new byte[]{1,2,4}),"reload must install changed data");
  c.clear();check(!c.duplicate(player,packet),"disconnect invalidates packet");
  System.out.println("PASS reward cache: 1000 hovers / 4 gear queries, owner/clock/disconnect changes, accepted-only duplicate suppression");
 }
}
