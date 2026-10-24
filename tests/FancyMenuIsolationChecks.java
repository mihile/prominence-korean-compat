import local.prominence.autoupdate.FancyMenuIsolation;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/** Exercise our rule against FancyMenu's actual public SAM loaded from its installed JAR. */
public final class FancyMenuIsolationChecks {
    public static void main(String[] args)throws Exception{
        try(URLClassLoader loader=new URLClassLoader(new URL[]{Path.of(args[0]).toUri().toURL()},null)){
            Class<?> type=Class.forName("de.keksuccino.fancymenu.customization.ScreenCustomization$ScreenBlacklistRule",false,loader);
            var method=type.getMethod("isScreenBlacklisted",String.class);
            Object rule=FancyMenuIsolation.rule(type);int checks=0;
            for(String name:List.of("local.prominence.autoupdate.UpdateScreen","local.prominence.autoupdate.UpdateCheckScreen")){
                if(!Boolean.TRUE.equals(method.invoke(rule,name)))throw new AssertionError("Updater not isolated: "+name);checks++;
            }
            for(String name:List.of("net.minecraft.class_442","net.minecraft.class_437","net.minecraft.class_490","net.minecraft.class_433","local.prominence.autoupdate.UpdateScreenOther","other.UpdateScreen")){
                if(!Boolean.FALSE.equals(method.invoke(rule,name)))throw new AssertionError("Other screen was disabled: "+name);checks++;
            }
            if(!rule.equals(rule)||rule.equals(new Object())||rule.toString().isBlank())throw new AssertionError("Broken public rule object");checks++;
            System.out.println("PASS: "+checks+" isolation checks using FancyMenu's actual interface; no game initialized.");
        }
    }
}
