package local.prominence.swingscope;
public final class SwingScopeTests {
    public static void main(String[]args){
        if(SwingScope.allowAnimation(true,true))throw new AssertionError("Player mining/chest animation must not dispatch combat powers");
        if(!SwingScope.allowAnimation(false,true))throw new AssertionError("Non-player swing behavior preserved");
        if(!SwingScope.allowAnimation(true,false))throw new AssertionError("Missing real attack hook must preserve original behavior");
        if(!SwingScope.allowAnimation(false,false))throw new AssertionError("Native fallback");
        if(SwingScope.animationActive())throw new AssertionError("Starts outside animation");
        SwingScope.enterAnimation();SwingScope.enterAnimation();SwingScope.exitAnimation();
        if(!SwingScope.animationActive())throw new AssertionError("Nested scope retained");
        SwingScope.exitAnimation();if(SwingScope.animationActive())throw new AssertionError("No stale thread local");
        System.out.println("PASS 7 swing dispatch safety checks");
    }
}
