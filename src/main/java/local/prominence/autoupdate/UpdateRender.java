package local.prominence.autoupdate;

import net.minecraft.class_332;

/** Independent modal surface. Never re-enter a different Screen's render/FancyMenu lifecycle. */
public final class UpdateRender {
    private UpdateRender() {}
    public static void background(class_332 draw,int width,int height,UpdateModalScreen screen){
        draw.method_51452();
        if(screen.snapshot!=null)screen.snapshot.value().draw(draw,width,height);
        else draw.method_25294(0,0,width,height,0xFF17141E);
        // Leave the captured menu visible, while keeping changelog contrast inside the panel.
        draw.method_25294(0,0,width,height,0x66000000);
    }
    public static void panel(class_332 draw,UpdateLayout box){
        int x=box.x(),y=box.y(),w=box.width(),h=box.height();
        draw.method_25294(x-1,y-1,x+w+1,y+h+1,0xFF75688E);
        draw.method_25294(x,y,x+w,y+h,0xFF211E29);
        draw.method_25294(x,y,x+w,y+3,0xFFB59BE0);
    }
}
