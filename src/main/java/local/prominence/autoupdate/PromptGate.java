package local.prominence.autoupdate;

/** Automatic offers belong to the title menu, never to an active or loading world. */
public final class PromptGate {
    private PromptGate(){}
    public static boolean canOpen(boolean title,boolean world,boolean player,boolean noScreen,boolean overlay,int readyTicks){
        return title && !world && !player && !overlay && readyTicks>=1;
    }
}
