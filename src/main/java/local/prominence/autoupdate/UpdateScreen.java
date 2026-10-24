package local.prominence.autoupdate;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_5481;

/** Changelog is immediately visible; only the GitHub release uses collapsible details. */
public final class UpdateScreen extends class_437 {
    private final SignedRelease release;
    private final class_437 parent;
    private UpdateLayout box;
    private int scroll;
    private List<class_5481> cachedLines;
    private class_4185 apply,later;
    public UpdateScreen(SignedRelease release,class_437 parent){super(text("title"));this.release=release;this.parent=parent;}
    private static class_2561 text(String key){return class_2561.method_43471("prominence.update."+key);}
    @Override protected void method_25426(){
        box=UpdateLayout.of(field_22789,field_22790);
        cachedLines=null;
        apply=method_37063(class_4185.method_46430(text("apply"),b->{
            if(UpdateClient.phase==UpdateClient.Phase.QUEUED)field_22787.method_1592();else UpdateClient.prepare(release);
        }).method_46434(box.x()+12,box.buttonsY(),box.buttonWidth(),20).method_46431());
        later=method_37063(class_4185.method_46430(text("later"),b->method_25419()).method_46434(box.x()+18+box.buttonWidth(),box.buttonsY(),box.buttonWidth(),20).method_46431());
        method_37063(class_4185.method_46430(text("github"),b->class_156.method_668().method_670(release.page().toString()))
            .method_46434(box.x()+24+box.buttonWidth()*2,box.buttonsY(),box.buttonWidth(),20).method_46431());
    }
    private List<class_5481> lines(){
        if(cachedLines!=null)return cachedLines;
        List<class_5481> rows=new ArrayList<>();
        for(String note:release.notes()){
            rows.addAll(field_22793.method_1728(class_2561.method_43470("• "+note),box.width()-38));
            rows.addAll(field_22793.method_1728(class_2561.method_43470(" "),box.width()-38));
        }
        return cachedLines=List.copyOf(rows);
    }
    @Override public void method_25394(class_332 draw,int mouseX,int mouseY,float delta){
        UpdateRender.background(draw,parent,field_22789,field_22790,delta);
        int x=box.x(),y=box.y(),w=box.width(),h=box.height();
        UpdateRender.panel(draw,box);
        draw.method_27535(field_22793,field_22785,x+14,y+13,0xFFF1E7FF);
        draw.method_25303(field_22793,release.build()==UpdateClient.build?release.label():UpdateClient.label+"  →  "+release.label(),x+14,y+31,0xFFACA6B8);
        draw.method_25294(x+12,y+49,x+w-12,y+50,0xFF494153);
        var rows=lines();int visible=box.notesBottom()-box.notesTop();int content=rows.size()*11;
        scroll=Math.max(0,Math.min(scroll,Math.max(0,content-visible)));
        draw.method_44379(x+12,box.notesTop(),x+w-12,box.notesBottom());
        int lineY=box.notesTop()-scroll;
        for(var line:rows){draw.method_51430(field_22793,line,x+16,lineY,0xFFE3DFE8,false);lineY+=11;}
        draw.method_44380();
        if(content>visible){
            int track=visible,thumb=Math.max(14,track*visible/content),offset=(track-thumb)*scroll/Math.max(1,content-visible);
            draw.method_25294(x+w-12,box.notesTop(),x+w-10,box.notesBottom(),0xFF3C3548);
            draw.method_25294(x+w-12,box.notesTop()+offset,x+w-10,box.notesTop()+offset+thumb,0xFFB39AD8);
        }
        var phase=UpdateClient.phase;
        String status=switch(phase){case AVAILABLE->"restart_hint";case DOWNLOADING->"downloading";case QUEUED->"ready";case FAILED->"failed";};
        boolean current=release.build()<=UpdateClient.build;
        draw.method_27535(field_22793,text(current?"latest":status),x+14,box.buttonsY()-19,phase==UpdateClient.Phase.FAILED?0xFFE8A0A0:0xFFB8A6D2);
        apply.field_22763=!current && phase!=UpdateClient.Phase.DOWNLOADING;
        apply.method_25355(text(current?"up_to_date":phase==UpdateClient.Phase.QUEUED?"quit":phase==UpdateClient.Phase.FAILED?"retry":"apply"));
        later.method_25355(text(phase==UpdateClient.Phase.QUEUED?"keep_playing":"later"));
        if(phase==UpdateClient.Phase.DOWNLOADING){
            int bar=w-28,filled=(int)Math.min(bar,bar*UpdateClient.downloaded/Math.max(1,release.size()));
            draw.method_25294(x+14,box.buttonsY()-7,x+14+bar,box.buttonsY()-5,0xFF40364C);
            draw.method_25294(x+14,box.buttonsY()-7,x+14+filled,box.buttonsY()-5,0xFFB59BE0);
        }
        super.method_25394(draw,mouseX,mouseY,delta);
        draw.method_51452();
    }
    @Override public boolean method_25401(double x,double y,double amount){
        if(x>=box.x() && x<=box.x()+box.width() && y>=box.notesTop() && y<=box.notesBottom()){scroll-=(int)(amount*22);return true;}
        return super.method_25401(x,y,amount);
    }
    @Override public boolean method_25421(){return true;}
    @Override public void method_25419(){field_22787.method_1507(parent);}
}
