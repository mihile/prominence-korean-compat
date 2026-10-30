package local.prominence.arrival.client;

/** Loading screens and slow frames reset the streak; readiness is sent on the following client tick. */
public final class ArrivalClientGate {
    private long previous;private int frames;private boolean rendered,observed;
    public void frame(boolean destinationReady,long now){
        if(!destinationReady){observed=false;frames=0;rendered=false;return;}
        frames=observed&&now-previous>=0&&now-previous<=250_000_000L?frames+1:1;
        previous=now;observed=true;rendered=frames>=3;
    }
    public boolean rendered(){return rendered;}
}
