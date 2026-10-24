package local.prominence.autoupdate;

import java.lang.reflect.Proxy;

/** Optional public FancyMenu API; no configuration files or global customization switch are modified. */
public final class FancyMenuIsolation {
    private FancyMenuIsolation() {}
    private static boolean registered;
    public static boolean isUpdaterScreen(String name){
        return "local.prominence.autoupdate.UpdateScreen".equals(name) ||
               "local.prominence.autoupdate.UpdateCheckScreen".equals(name);
    }
    public static Object rule(Class<?> ruleType){
        return Proxy.newProxyInstance(ruleType.getClassLoader(),new Class<?>[]{ruleType},(proxy,method,args)->{
            if(method.getName().equals("isScreenBlacklisted") && args!=null && args.length==1)
                return isUpdaterScreen((String)args[0]);
            return switch(method.getName()){
                case "toString" -> "Prominence updater screens only";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> args!=null && args.length==1 && proxy==args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        });
    }
    public static synchronized void register()throws ReflectiveOperationException{
        if(registered)return;
        Class<?> api=Class.forName("de.keksuccino.fancymenu.customization.ScreenCustomization");
        Class<?> type=Class.forName("de.keksuccino.fancymenu.customization.ScreenCustomization$ScreenBlacklistRule");
        api.getMethod("addScreenBlacklistRule",type).invoke(null,rule(type));
        registered=true;
    }
}
