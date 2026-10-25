package local.prominence.weaponhud;

/** One physical use-key press cannot both interact with the world/menu and activate a weapon. */
public final class UsePressFence {
    private boolean consumed;
    public void observe(boolean pressed,boolean screenOpen){if(!pressed)consumed=false;else if(screenOpen)consumed=true;}
    public void consume(){consumed=true;}
    public boolean blocks(boolean sameAsUseKey,boolean pressed,boolean containerTarget,boolean sneaking){
        return sameAsUseKey&&pressed&&(consumed||containerTarget&&!sneaking);
    }
    public void reset(){consumed=false;}
}
