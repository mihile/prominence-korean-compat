package local.prominence.weaponhud;

import net.minecraft.*;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.item.RunicSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.power.GemPower;

public final class WeaponSupport {
    private WeaponSupport(){}
    private static final class_6862<class_1792> SPEARS=class_6862.method_40092(class_7924.field_41197,new class_2960("simplyswords","spears"));
    public static boolean defaultSpearThrow(class_1799 stack){return stack.method_7909() instanceof RunicSwordItem&&stack.method_31573(SPEARS);}
    private static final ClassValue<Boolean> ACTIVE_POWER=new ClassValue<>(){
        protected Boolean computeValue(Class<?> type){
            for(Class<?> t=type;t!=null&&t!=GemPower.class;t=t.getSuperclass())try{
                t.getDeclaredMethod("use",class_1937.class,class_1657.class,class_1268.class,class_1799.class);return true;
            }catch(NoSuchMethodException ignored){}
            return false;
        }
    };
    private static final ClassValue<Boolean> REVIEWED_USE=new ClassValue<>(){
        protected Boolean computeValue(Class<?> type){for(Class<?> t=type;t!=null;t=t.getSuperclass())if(WeaponHudGuard.extraClasses().contains(t.getName()))return true;return false;}
    };
    public static boolean weapon(class_1799 stack){if(stack==null||stack.method_7960())return false;var item=stack.method_7909();return item instanceof class_1829||item instanceof class_1743||item instanceof class_1835||active(stack);}
    public static boolean active(class_1799 stack){
        if(stack==null||stack.method_7960())return false;
        var item=stack.method_7909();if(item instanceof UniqueWeaponActiveAbility)return true;
        if(item instanceof RunicSwordItem){
            if(stack.method_31573(SPEARS))return true;
            var c=SimplySwordsAPI.getComponent(stack);return ACTIVE_POWER.get(c.runic().getClass())||ACTIVE_POWER.get(c.nether().getClass());
        }
        return REVIEWED_USE.get(item.getClass());
    }
}
