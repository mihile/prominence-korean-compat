package local.prominence.memory;
public final class AzureRendererTracker {
    private static final WeakListenerSet<AzureRendererOwner> RENDERERS=new WeakListenerSet<>();
    private AzureRendererTracker(){}
    public static void track(AzureRendererOwner renderer){RENDERERS.add(renderer);}
    public static int release(Object currentWorld){int count=0;for(var renderer:RENDERERS.snapshot())if(renderer.prominence$clearObsoleteWorld(currentWorld))count++;return count;}
}
