package local.prominence.ftbime;

import java.lang.ref.WeakReference;
import moe.caramel.chat.util.Rect;
import moe.caramel.chat.wrapper.AbstractIMEWrapper;

/** Uses installed CaramelChat's native composition and commit API, not a second IME hook. */
public final class FtbImeAdapter extends AbstractIMEWrapper {
    private final WeakReference<ImeTarget> target;
    private boolean internal;
    public FtbImeAdapter(ImeTarget target){super(target.prominence$imeText());this.target=new WeakReference<>(target);}
    public void beforeEdit(){if(!internal&&getStatus()==InputStatus.PREVIEW)setToNoneStatus();}
    public void afterEdit(){var t=target.get();if(!internal&&t!=null)setOrigin(t.prominence$imeText());}
    public void focus(boolean focused){
        if(!focused){setFocused(false);beforeEdit();return;}
        beforeEdit();afterEdit();setFocused(editable());
    }
    public int visualCursor(int nativeCursor){
        var t=target.get();return getStatus()==InputStatus.PREVIEW&&t!=null?Math.max(0,Math.min(getSecondStartPos(),t.prominence$imeText().length())):nativeCursor;
    }
    protected void insert(String value){
        var t=target.get();if(t==null)return;
        internal=true;try{t.prominence$imeInsert(value);}finally{internal=false;}
    }
    protected int getCursorPos(){var t=target.get();return t==null?0:t.prominence$imeCursor();}
    protected int getHighlightPos(){var t=target.get();return t==null?0:t.prominence$imeAnchor();}
    public boolean editable(){var t=target.get();return t!=null&&t.prominence$imeEditable();}
    public boolean blockTyping(){
        var t=target.get();return t==null||!editable()||t.prominence$imeLimit()-getOrigin().length()+Math.abs(getCursorPos()-getHighlightPos())<=0;
    }
    protected String getTextWithPreview(){var t=target.get();return t==null?getOrigin():t.prominence$imeText();}
    protected void setPreviewText(String value){var t=target.get();if(t!=null)t.prominence$imePreview(value);}
    public Rect getRect(){var t=target.get();return t==null?new Rect(0,0,0,0):t.prominence$imeRect(visualCursor(getCursorPos()));}
}
