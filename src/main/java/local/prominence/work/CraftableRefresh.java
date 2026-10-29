package local.prominence.work;

/** One render-thread flag, with no retained inventory, recipes or screen. */
public final class CraftableRefresh {
    private boolean deferred;
    public boolean shouldCompute(boolean visible) {
        if(!visible){deferred=true;return false;}
        deferred=false;return true;
    }
    public boolean needsRefresh(boolean visible){return deferred && visible;}
}
