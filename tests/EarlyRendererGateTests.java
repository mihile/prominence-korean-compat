package local.prominence.work;

public final class EarlyRendererGateTests {
    public static void main(String[] args) {
        int[] calls={0};
        EarlyRendererGate.invoke(true,()->calls[0]++);
        if(calls[0]!=0)throw new AssertionError("shader startup must defer");
        EarlyRendererGate.invoke(false,()->calls[0]++);
        if(calls[0]!=1)throw new AssertionError("non-shader original must run exactly once");
        try {EarlyRendererGate.invoke(false,()->{throw new IllegalStateException("original");});throw new AssertionError("exception lost");}
        catch(IllegalStateException expected){if(!"original".equals(expected.getMessage()))throw expected;}
        System.out.println("PASS: shader deferral and non-shader original/exception behavior");
    }
}
