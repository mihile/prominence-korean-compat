package local.prominence.swingscope;

import java.util.Arrays;
import net.minecraft.class_1309;
import net.minecraft.class_3222;

/** Better Combat already dispatches weapon powers from its authoritative attack handler.
 * Vanilla arm animations also represent mining/interacting and must not dispatch them twice. */
public final class SwingScope {
    private static final ThreadLocal<Integer> ANIMATION_DEPTH=new ThreadLocal<>();
    public static void enterAnimation(){var d=ANIMATION_DEPTH.get();ANIMATION_DEPTH.set(d==null?1:d+1);}
    public static void exitAnimation(){var d=ANIMATION_DEPTH.get();if(d==null||d<=1)ANIMATION_DEPTH.remove();else ANIMATION_DEPTH.set(d-1);}
    static boolean animationActive(){return ANIMATION_DEPTH.get()!=null;}
    private static final class CombatHook {static final boolean PRESENT=findHook();}
    private static boolean findHook(){
        try {
            var network=Class.forName("net.bettercombat.network.ServerNetwork",false,SwingScope.class.getClassLoader());
            boolean present=Arrays.stream(network.getDeclaredMethods()).anyMatch(m->m.getName().contains("simplyswords$triggerRunicSlashFromBetterCombat"));
            org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("Weapon swing scope: {}",present?"using verified Better Combat attack hook":"fallback to original swing hook");
            return present;
        }catch(ReflectiveOperationException|LinkageError unavailable){return false;}
    }
    public static boolean blockAnimationPower(class_1309 actor){return animationActive()&&!allowAnimation(actor instanceof class_3222,CombatHook.PRESENT);}
    static boolean allowAnimation(boolean serverPlayer,boolean attackHookPresent){return !serverPlayer||!attackHookPresent;}
}
