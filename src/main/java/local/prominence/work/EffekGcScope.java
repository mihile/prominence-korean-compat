package local.prominence.work;
/** Distinguishes memory-pressure GC from an explicit resource reload. */
public final class EffekGcScope {
 private static final ThreadLocal<Integer> DEPTH=ThreadLocal.withInitial(()->0);
 private EffekGcScope(){}
 public static void enter(){DEPTH.set(DEPTH.get()+1);}
 public static void exit(){int n=DEPTH.get()-1;if(n<=0)DEPTH.remove();else DEPTH.set(n);}
 public static boolean protect(String id,long lastUsed,long now){return DEPTH.get()>0 && "eldritch_end:shadowsurge".equals(id) && lastUsed>=0 && (now-lastUsed)<8000;}
}
