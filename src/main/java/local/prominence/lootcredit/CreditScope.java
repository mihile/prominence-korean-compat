package local.prominence.lootcredit;
public final class CreditScope implements AutoCloseable {
 public record State(Credit credit,boolean anonymous){}
 private static final ThreadLocal<State> CURRENT=new ThreadLocal<>();private final State previous;
 private CreditScope(Credit c,boolean anonymous){previous=CURRENT.get();CURRENT.set(new State(c,anonymous));}
 public static CreditScope enter(Credit c,boolean anonymous){return new CreditScope(c,anonymous);}
 public static State current(){return CURRENT.get();}
 public void close(){if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
}
