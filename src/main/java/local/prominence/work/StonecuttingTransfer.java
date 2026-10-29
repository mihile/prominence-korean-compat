package local.prominence.work;

/** Order shared by the live adapter and its offline packet-order fixture. */
public final class StonecuttingTransfer {
    public interface Steps {
        boolean fill();
        int recipeIndex();
        void select(int index);
        boolean correctOutput();
        void take();
    }
    private StonecuttingTransfer() {}
    public static boolean execute(Steps steps,boolean take) {
        if(!steps.fill()) return false;
        int index=steps.recipeIndex();
        if(index<0) return false;
        steps.select(index);
        if(!steps.correctOutput()) return false;
        if(take) steps.take();
        return true;
    }
}
