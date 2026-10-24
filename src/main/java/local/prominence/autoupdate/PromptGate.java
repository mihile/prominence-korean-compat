package local.prominence.autoupdate;

/** Startup must work without a world/player. Other menus and loading overlays are left alone. */
public final class PromptGate {
    private PromptGate(){}
    public static boolean canOpen(boolean title,boolean world,boolean player,boolean noScreen,boolean overlay,int readyTicks){
        return !overlay && readyTicks>=20 && (title || (world && player && noScreen));
    }
}
