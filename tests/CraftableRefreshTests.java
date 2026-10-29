package local.prominence.work;

public final class CraftableRefreshTests {
    public static void main(String[] args) {
        CraftableRefresh refresh=new CraftableRefresh();
        if(refresh.needsRefresh(true))throw new AssertionError("Unchanged initial state");
        for(int i=0;i<1000;i++)if(refresh.shouldCompute(false))throw new AssertionError("Hidden inventory change computed");
        if(refresh.needsRefresh(false)||!refresh.needsRefresh(true))throw new AssertionError("Visibility did not invalidate the old inventory");
        if(!refresh.shouldCompute(true)||refresh.needsRefresh(true))throw new AssertionError("Visible fresh calculation not committed");
        if(!refresh.shouldCompute(true))throw new AssertionError("Visible inventory changes were skipped");
        if(refresh.shouldCompute(false)||!refresh.needsRefresh(true))throw new AssertionError("Second hide/show lost update");
        System.out.println("PASS hidden craftables defer repeated inventory changes and refresh on exposure; no inventory retained");
    }
}
