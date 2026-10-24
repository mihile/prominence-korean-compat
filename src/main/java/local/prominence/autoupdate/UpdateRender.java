package local.prominence.autoupdate;

import net.minecraft.class_332;
import net.minecraft.class_437;

/** Keep parent text/item batches behind the modal instead of flushing them over its panel. */
public final class UpdateRender {
    private UpdateRender() {}
    public static void background(class_332 draw,class_437 parent,int width,int height,float delta){
        if(parent!=null)parent.method_25394(draw,-1,-1,delta);
        // DrawContext buffers GUI, text and item render layers separately. Finish ALL parent layers first.
        draw.method_51452();
        draw.method_25294(0,0,width,height,0x88000000);
    }
    public static void panel(class_332 draw,UpdateLayout box){
        int x=box.x(),y=box.y(),w=box.width(),h=box.height();
        draw.method_25294(x-1,y-1,x+w+1,y+h+1,0xFF75688E);
        draw.method_25294(x,y,x+w,y+h,0xFF211E29);
        draw.method_25294(x,y,x+w,y+3,0xFFB59BE0);
    }
}
