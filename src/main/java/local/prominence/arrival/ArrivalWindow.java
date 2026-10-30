package local.prominence.arrival;

/** One server-owned teleport window. Tick subtraction also works across int wraparound. */
public final class ArrivalWindow {
    public static final long MAX_LOADING_NANOS=60_000_000_000L;
    public static final int GRACE_TICKS=60, LEGACY_TICKS=200;
    public enum Phase { MOVING, LOADING, GRACE }
    public final long nonce, started;
    private Phase phase=Phase.MOVING;
    private int graceStart, graceTicks;
    public ArrivalWindow(long nonce,long now){this.nonce=nonce;started=now;}
    public Phase phase(){return phase;}
    public void arrived(boolean hasPeer,int tick){
        if(phase!=Phase.MOVING)return;
        if(hasPeer)phase=Phase.LOADING;else grace(tick,LEGACY_TICKS);
    }
    private void grace(int tick,int ticks){phase=Phase.GRACE;graceStart=tick;graceTicks=ticks;}
    public void legacy(int tick){grace(tick,LEGACY_TICKS);}
    public boolean active(int tick,long now){
        if(phase==Phase.MOVING)return now-started<MAX_LOADING_NANOS;
        if(phase==Phase.LOADING&&now-started>=MAX_LOADING_NANOS)grace(tick,GRACE_TICKS);
        return phase==Phase.LOADING||tick-graceStart<graceTicks;
    }
    public boolean ready(long token,int tick,long now){
        if(!active(tick,now)||phase!=Phase.LOADING||token!=nonce)return false;
        grace(tick,GRACE_TICKS);return true;
    }
}
