package local.prominence.backpackcompat.client;

import local.prominence.backpackinline.client.InlineReforgeClient;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_7919;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

/** Legacy constructor is inert; the guarded inline tab owns the replacement control. */
public final class PortableReforgeButton extends WidgetBase {
    private final class_4185 button;
    private final int upgradeId;
    public PortableReforgeButton(Position position,int upgradeId){this(position,upgradeId,false);}
    public PortableReforgeButton(Position position,int upgradeId,boolean inlineOwner){
        super(position,new Dimension(97,20));this.upgradeId=upgradeId;
        button=!inlineOwner?null:class_4185.method_46430(class_2561.method_43471("prominence.inline.reforge"),b->{
            var state=InlineReforgeClient.state(upgradeId);if(state!=null)InlineReforgeClient.send(upgradeId,state.mode()?0:1);
        }).method_46434(position.x(),position.y(),97,20).method_46431();
    }
    private void positionButton(){
        if(button==null)return;var state=InlineReforgeClient.state(upgradeId);
        button.method_46421(x);button.method_46419(y);button.field_22763=state!=null;
        button.method_25355(class_2561.method_43471(state!=null&&state.mode()?"prominence.inline.anvil":"prominence.inline.reforge"));
        button.method_47400(class_7919.method_47407(class_2561.method_43471(state==null?
            InlineReforgeClient.advertised()?"prominence.inline.reopen":"prominence.inline.server_required":"prominence.inline.switch")));
    }
    @Override protected void renderBg(class_332 draw,class_310 client,int mx,int my){}
    @Override protected void renderWidget(class_332 draw,int mx,int my,float delta){if(button!=null){positionButton();button.method_25394(draw,mx,my,delta);}}
    @Override public boolean method_25402(double x,double y,int key){positionButton();return visible&&button!=null&&button.method_25402(x,y,key);}
    @Override public boolean method_25404(int key,int scan,int mod){return visible&&button!=null&&button.method_25404(key,scan,mod);}
    @Override public void method_25365(boolean focus){super.method_25365(focus);if(button!=null)button.method_25365(focus);}
}
