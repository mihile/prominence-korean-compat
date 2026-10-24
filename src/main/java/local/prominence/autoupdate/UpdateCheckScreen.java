package local.prominence.autoupdate;

import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;

/** A visible loading/error/retry route that works before a world is entered. */
public final class UpdateCheckScreen extends class_437 {
    private final class_437 parent;
    private UpdateLayout box;
    private String status="checking";
    private class_4185 retry;
    public UpdateCheckScreen(class_437 parent){super(text("title"));this.parent=parent;}
    private static class_2561 text(String key){return class_2561.method_43471("prominence.update."+key);}
    public void failed(){status="check_failed";retry.field_22763=true;}
    public void latest(){status="latest";retry.field_22763=true;}
    @Override protected void method_25426(){
        box=UpdateLayout.of(field_22789,field_22790);
        retry=method_37063(class_4185.method_46430(text("check_again"),b->UpdateClient.openCheck(parent)).method_46434(box.x()+12,box.buttonsY(),box.buttonWidth(),20).method_46431());retry.field_22763=!status.equals("checking");
        method_37063(class_4185.method_46430(text("github"),b->class_156.method_668().method_670("https://github.com/"+SignedRelease.REPO+"/releases/latest")).method_46434(box.x()+18+box.buttonWidth(),box.buttonsY(),box.buttonWidth(),20).method_46431());
        method_37063(class_4185.method_46430(text("close"),b->method_25419()).method_46434(box.x()+24+box.buttonWidth()*2,box.buttonsY(),box.buttonWidth(),20).method_46431());
    }
    @Override public void method_25394(class_332 draw,int mx,int my,float delta){
        UpdateRender.background(draw,parent,field_22789,field_22790,delta);
        int x=box.x(),y=box.y(),w=box.width(),h=box.height();UpdateRender.panel(draw,box);
        draw.method_27535(field_22793,field_22785,x+14,y+14,0xFFF1E7FF);
        int line=y+65;for(var row:field_22793.method_1728(text(status),w-32)){draw.method_51430(field_22793,row,x+16,line,0xFFE3DFE8,false);line+=12;}
        super.method_25394(draw,mx,my,delta);
        draw.method_51452();
    }
    @Override public void method_25419(){field_22787.method_1507(parent);}
    @Override public boolean method_25421(){return true;}
}
