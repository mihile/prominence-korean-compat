package local.prominence.backpackinline.client;

import com.mojang.blaze3d.systems.RenderSystem;
import local.prominence.backpackinline.InlineState;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_5244;
import net.minecraft.class_7919;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

public final class InlinePanel extends WidgetBase {
    private static final class_2960 TEXTURE=new class_2960("tiered","textures/gui/reforging_screen.png");
    private final int id;private final ForgeButton button;
    public InlinePanel(Position p,int id){super(p,new Dimension(97,64));this.id=id;button=new ForgeButton(p.x()+40,p.y()+32,b->InlineReforgeClient.send(id,2));}
    private InlineState state(){return InlineReforgeClient.state(id);}
    private boolean active(){var s=state();return s!=null&&s.mode();}
    @Override protected void renderBg(class_332 draw,class_310 client,int mx,int my){
        if(!active())return;
        // Reuse only the three native cells; cropping the whole screen also captures a stray hammer edge.
        draw.method_25302(TEXTURE,x+5,y+22,44,46,18,18);
        draw.method_25302(TEXTURE,x+40,y+9,79,33,18,18);
        draw.method_25302(TEXTURE,x+75,y+22,114,46,18,18);
    }
    @Override protected void renderWidget(class_332 draw,int mx,int my,float delta){
        if(!active())return;var s=state();button.method_46421(x+40);button.method_46419(y+32);button.field_22763=s.ready();
        button.method_47400(class_7919.method_47407(class_2561.method_43471(s.ready()?"prominence.inline.reforge":"prominence.inline.inputs")));
        button.method_25394(draw,mx,my,delta);
        var label=class_2561.method_43471(s.ready()?"prominence.inline.ready":"prominence.inline.slots");
        draw.method_51439(minecraft.field_1772,label,x+(97-minecraft.field_1772.method_27525(label))/2,y+56,s.ready()?0x347326:0x404040,false);
    }
    @Override public boolean method_25402(double mx,double my,int key){return visible&&active()&&button.method_25402(mx,my,key);}
    @Override public boolean method_25404(int key,int scan,int modifiers){return visible&&active()&&button.method_25404(key,scan,modifiers);}
    @Override public void method_25365(boolean focus){super.method_25365(focus);button.method_25365(focus);}
    private static final class ForgeButton extends class_4185 {
        ForgeButton(int x,int y,class_4241 press){super(x,y,18,18,class_5244.field_39003,press,field_40754);}
        @Override public void method_48579(class_332 draw,int mx,int my,float delta){
            RenderSystem.setShaderColor(1,1,1,1);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.enableDepthTest();
            int u=176+(!field_22763?36:method_49606()?18:0);
            draw.method_25302(TEXTURE,method_46426(),method_46427(),u,0,18,18);
        }
    }
}
