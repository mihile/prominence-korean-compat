package local.prominence.memory;
import java.lang.ref.WeakReference;
import java.util.function.DoubleSupplier;
import java.util.function.ToDoubleFunction;
/** The function must be non-capturing; active owners remain strongly held by the game. */
public final class WeakDoubleSupplier<T> extends WeakReference<T> implements DoubleSupplier {
    private final ToDoubleFunction<T> function;
    public WeakDoubleSupplier(T owner,ToDoubleFunction<T> function){super(owner);this.function=function;}
    public double getAsDouble(){T owner=get();return owner==null?0:function.applyAsDouble(owner);}
}
