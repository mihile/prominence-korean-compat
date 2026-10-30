package local.prominence.work;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import java.util.IdentityHashMap;
import java.util.function.Supplier;
import net.minecraft.class_1792;
import net.minecraft.class_1935;
import org.slf4j.LoggerFactory;

/** Private, bounded templates for native anvil indexing; every caller receives its own stack. */
public final class EmiStackBuildScope implements AutoCloseable {
    private static final ThreadLocal<EmiStackBuildScope> CURRENT = new ThreadLocal<>();
    private static final int MAX_ITEMS = 8192, MAX_BOOKS = 65536;
    private static final ClassValue<Boolean> PLAIN_DEFAULT = new ClassValue<>() {
        protected Boolean computeValue(Class<?> type) {
            try { return type.getMethod("method_7854").getDeclaringClass() == class_1792.class; }
            catch (ReflectiveOperationException | LinkageError e) { return false; }
        }
    };
    private final EmiStackBuildScope previous = CURRENT.get();
    private final IdentityHashMap<class_1792, EmiStack> items = new IdentityHashMap<>();
    private final IdentityHashMap<Object, EmiStack> books = new IdentityHashMap<>();
    private final String phase;
    private long itemHits, bookHits, itemFallbacks, bookFallbacks;
    private boolean closed;

    private EmiStackBuildScope(String phase) { this.phase = phase; CURRENT.set(this); }
    public static EmiStackBuildScope begin(String phase) { return new EmiStackBuildScope(phase); }

    public static EmiStack item(class_1935 convertible, Supplier<EmiStack> original) {
        EmiStackBuildScope scope = CURRENT.get();
        if (scope == null || !(convertible instanceof class_1792 item) || !PLAIN_DEFAULT.get(item.getClass()))
            return original.get();
        EmiStack saved = scope.items.get(item);
        if (saved != null) { scope.itemHits++; return saved.copy(); }
        EmiStack value = original.get();
        // Constructor/other-mod NBT and custom defaults are intentionally evaluated every time.
        if (plain(value, item) && scope.items.size() < MAX_ITEMS) scope.items.put(item, value.copy());
        else scope.itemFallbacks++;
        return value;
    }

    public static EmiStack book(Object recipe, class_1792 tool, Supplier<EmiStack> original) {
        EmiStackBuildScope scope = CURRENT.get();
        if (scope == null || tool == null || !PLAIN_DEFAULT.get(tool.getClass())) return original.get();
        // A successful native plain-tool observation is required before book memoization.
        // Custom/innately enchanted defaults therefore retain getTool()/first-enchantment semantics.
        if (!scope.items.containsKey(tool)) { scope.bookFallbacks++; return original.get(); }
        EmiStack saved = scope.books.get(recipe);
        if (saved != null) { scope.bookHits++; return saved.copy(); }
        EmiStack value = original.get();
        if (value != null && value.getClass() == ItemEmiStack.class && scope.books.size() < MAX_BOOKS)
            scope.books.put(recipe, value.copy());
        else scope.bookFallbacks++;
        return value;
    }

    private static boolean plain(EmiStack value, class_1792 item) {
        return value != null && value.getClass() == ItemEmiStack.class && value.getKey() == item
            && value.getNbt() == null && value.getAmount() == 1 && value.getRemainder() == EmiStack.EMPTY;
    }

    public void close() {
        if (closed) return;
        closed = true;
        int itemEntries = items.size(), bookEntries = books.size();
        items.clear(); books.clear();
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        LoggerFactory.getLogger("Prominence compatibility").info(
            "EMI native stack preparation: phase={}, itemHits={}, bookHits={}, itemEntries={}, bookEntries={}, itemFallbacks={}, bookFallbacks={}",
            phase, itemHits, bookHits, itemEntries, bookEntries, itemFallbacks, bookFallbacks);
    }
}
