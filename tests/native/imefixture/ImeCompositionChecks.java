package imefixture;
import java.lang.ref.WeakReference;
import local.prominence.ftbime.*;
import moe.caramel.chat.Main;
import moe.caramel.chat.util.Rect;
import moe.caramel.chat.wrapper.AbstractIMEWrapper.InputStatus;

public final class ImeCompositionChecks {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static final class Target implements ImeTarget {
        String text="";int cursor,anchor,limit=1024,commits;boolean focused=true,enabled=true,reject;FtbImeAdapter adapter;
        public String prominence$imeText(){return text;}
        public int prominence$imeCursor(){return cursor;}
        public int prominence$imeAnchor(){return anchor;}
        public int prominence$imeLimit(){return limit;}
        public boolean prominence$imeEditable(){return focused&&enabled;}
        public void prominence$imePreview(String value){text=value;}
        public void prominence$imeInsert(String value){
            adapter.beforeEdit();
            try{
                int start=Math.min(cursor,anchor),end=Math.max(cursor,anchor);int space=limit-text.length()+end-start;
                if(space<=0||reject)return;
                String accepted=value.substring(0,Math.min(value.length(),space));text=text.substring(0,start)+accepted+text.substring(end);cursor=anchor=start+accepted.length();commits++;
            }finally{adapter.afterEdit();}
        }
        public Rect prominence$imeRect(int caret){return new Rect(caret,10,80,12);}
        public void prominence$imeClosed(){focused=false;adapter.focus(false);}
        void plain(String value,int caret,int selected){adapter.beforeEdit();text=value;cursor=caret;anchor=selected;adapter.afterEdit();}
    }
    static Target target(String value,int cursor,int anchor){var t=new Target();t.text=value;t.cursor=cursor;t.anchor=anchor;t.adapter=new FtbImeAdapter(t);t.adapter.focus(true);return t;}
    public static void main(String[]args)throws Exception{
        var t=target("",0,0);var a=t.adapter;
        check(a.getIme().isFocused(),"Native operator enabled when FTB field focuses");
        for(String preview:new String[]{"ㄱ","가","강"}){a.appendPreviewText(preview);check(t.text.equals(preview),"Native Korean preedit visible");check(t.commits==0,"Preedit must not trigger search callback");check(t.cursor==0,"Preedit preserves commit anchor");}
        check(a.visualCursor(0)==1,"Visible caret follows composing syllable");a.insertText("강");
        check(t.text.equals("강")&&t.cursor==1&&t.anchor==1,"Native commit inserts once with correct cursor");check(t.commits==1&&a.getStatus()==InputStatus.NONE,"Search callback exactly once after commit");
        a.appendPreviewText("타");a.insertText("타");check(t.text.equals("강타")&&t.commits==2,"Repeated syllables do not duplicate");
        t=target("가나다",1,1);a=t.adapter;a.appendPreviewText("한");check(t.text.equals("가한나다"),"Mid-string preedit");a.appendPreviewText("한글");check(t.text.equals("가한글나다"),"Replacing preedit keeps suffix");a.insertText("한글");check(t.text.equals("가한글나다")&&t.cursor==3&&t.commits==1,"Mid-string native commit");
        t=target("가나다",1,2);a=t.adapter;a.appendPreviewText("한");check(t.text.equals("가한다"),"Selected text replaced by composition");a.insertText("한");check(t.text.equals("가한다")&&t.cursor==2,"Selection commit is not duplicated");
        t=target("test",4,4);a=t.adapter;a.appendPreviewText("ㄱ");t.prominence$imeClosed();check(t.text.equals("test")&&!a.getIme().isFocused(),"Closing cancels uncommitted composition and releases operator");a.insertText("늦게");check(t.text.equals("test"),"Late callback cannot change a closed field");
        t=target("ab",2,2);a=t.adapter;a.appendPreviewText("한");t.plain("paste",5,5);a.appendPreviewText("글");a.insertText("글");check(t.text.equals("paste글"),"Programmatic/paste changes update composition origin");
        t=target("a",1,1);a=t.adapter;t.limit=1;a.appendPreviewText("나");a.insertText("나");check(t.commits==0,"Full field blocks native commit");a.focus(false);check(t.text.equals("a"),"Full-field preview restores original on blur");
        t=target("abcd",1,3);a=t.adapter;t.limit=4;a.appendPreviewText("한");a.insertText("한");check(t.text.equals("a한d"),"Selection frees space at length limit");
        t=target("x",1,1);a=t.adapter;t.reject=true;a.insertText("한");check(t.text.equals("x"),"Existing validation retained by target insertion");
        var first=target("",0,0);var second=target("",0,0);check(Main.current==second.adapter.getIme(),"Focus moves to next field");first.prominence$imeClosed();check(Main.current==second.adapter.getIme(),"Closing old field must not disable newly focused field");second.prominence$imeClosed();check(Main.current==null,"Last field close releases native focus");
        WeakReference<Target> old;FtbImeAdapter held;
        {var temporary=target("world",5,5);old=new WeakReference<>(temporary);held=temporary.adapter;temporary=null;}
        for(int i=0;i<30&&old.get()!=null;i++){System.gc();Thread.sleep(10);}
        check(old.get()==null,"Native wrapper must not retain old FTB screen target");check(!held.editable(),"Collected target rejects callbacks");held.appendPreviewText("한");held.insertText("한");check(held.getRect().copy().length==4,"Collected target safely answers native rectangle callback");
        System.out.println("PASS: "+checks+" production adapter + actual CaramelChat composition/focus/commit/selection/limit/closure/weak-lifetime checks; JNI and game replaced only by test controller facade");
    }
}
