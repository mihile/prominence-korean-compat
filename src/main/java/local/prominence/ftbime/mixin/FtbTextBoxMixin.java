package local.prominence.ftbime.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.ftb.mods.ftblibrary.ui.*;
import local.prominence.ftbime.*;
import moe.caramel.chat.util.Rect;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=TextBox.class,remap=false)
public abstract class FtbTextBoxMixin implements ImeTarget {
    @Shadow private String text;
    @Shadow private int cursorPos;
    @Shadow private int highlightPos;
    @Shadow private int displayPos;
    @Shadow private int maxLength;
    @Shadow private void scrollTo(int pos){throw new AssertionError();}
    @Unique private FtbImeAdapter prominence$ime;

    @Inject(method="setFocused",at=@At("RETURN"))
    private void prominence$focus(boolean focused,CallbackInfo ci){
        if(focused&&prominence$ime==null)prominence$ime=new FtbImeAdapter(this);
        if(prominence$ime!=null)prominence$ime.focus(focused);
    }
    @Inject(method={"setText(Ljava/lang/String;Z)V","insertText","deleteCharsToPos","setCursorPosition","setCursorPos","setSelectionPos"},at=@At("HEAD"))
    private void prominence$beforeEdit(CallbackInfo ci){if(prominence$ime!=null)prominence$ime.beforeEdit();}
    @Inject(method={"setText(Ljava/lang/String;Z)V","insertText","deleteCharsToPos","setCursorPosition","setCursorPos","setSelectionPos"},at=@At("RETURN"))
    private void prominence$afterEdit(CallbackInfo ci){if(prominence$ime!=null)prominence$ime.afterEdit();}
    @ModifyExpressionValue(method="draw",at=@At(value="FIELD",target="Ldev/ftb/mods/ftblibrary/ui/TextBox;cursorPos:I"))
    private int prominence$compositionCaret(int cursor){return prominence$ime==null?cursor:prominence$ime.visualCursor(cursor);}
    @ModifyExpressionValue(method="draw",at=@At(value="FIELD",target="Ldev/ftb/mods/ftblibrary/ui/TextBox;highlightPos:I"))
    private int prominence$compositionSelection(int anchor){return prominence$ime==null?anchor:prominence$ime.visualCursor(anchor);}

    @Unique private TextBox prominence$box(){return (TextBox)(Object)this;}
    public String prominence$imeText(){return text;}
    public int prominence$imeCursor(){return cursorPos;}
    public int prominence$imeAnchor(){return highlightPos;}
    public int prominence$imeLimit(){return maxLength;}
    public boolean prominence$imeEditable(){return prominence$box().isFocused()&&prominence$box().isEnabled()&&prominence$box().allowInput();}
    public void prominence$imePreview(String value){
        // Preview must not fire onTextChanged, validate an incomplete syllable, or move the commit anchor.
        text=value;displayPos=Math.max(0,Math.min(displayPos,value.length()));
        scrollTo(prominence$ime==null?cursorPos:prominence$ime.visualCursor(cursorPos));
    }
    public void prominence$imeInsert(String value){prominence$box().insertText(value);}
    public Rect prominence$imeRect(int caret){
        var box=prominence$box();var theme=box.getGui().getTheme();var font=theme.getFont();
        int start=Math.max(0,Math.min(displayPos,text.length()));int end=Math.max(start,Math.min(caret,text.length()));
        int offset=font==null?0:font.method_1727(text.substring(start,end));
        float x=box.getX()+4+Math.min(offset,Math.max(0,box.width-4));
        float y=box.getY()+(box.height-8)/2f+theme.getFontHeight();
        return new Rect(x,y,Math.max(1,box.width),Math.max(1,box.height));
    }
    public void prominence$imeClosed(){
        if(prominence$ime!=null){var old=prominence$ime;prominence$ime=null;prominence$box().setFocused(false);old.focus(false);}
    }
}
