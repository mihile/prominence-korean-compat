package local.prominence.join;

import java.util.Locale;

/** Exact replacement for the native three-string resource filename format; no cache. */
public final class ResourceFilename {
    private ResourceFilename() {}
    public static String tryFormat(Locale locale, String format, Object[] args) {
        if (!Locale.ROOT.equals(locale) || !"%s/%s/%s".equals(format) || args == null || args.length != 3) return null;
        for (Object value : args) if (value != null && !(value instanceof String)) return null;
        return (String) args[0] + "/" + (String) args[1] + "/" + (String) args[2];
    }
}
