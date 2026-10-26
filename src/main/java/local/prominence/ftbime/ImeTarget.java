package local.prominence.ftbime;
import moe.caramel.chat.util.Rect;
/** Adapter boundary; no screen/world is retained by the native IME operator. */
public interface ImeTarget {
    String prominence$imeText();
    int prominence$imeCursor();
    int prominence$imeAnchor();
    int prominence$imeLimit();
    boolean prominence$imeEditable();
    void prominence$imePreview(String text);
    void prominence$imeInsert(String text);
    Rect prominence$imeRect(int cursor);
    void prominence$imeClosed();
}
