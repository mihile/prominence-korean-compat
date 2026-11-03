package local.prominence.memory;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.spell_engine.api.spell.SpellContainer;

/** One synchronous HUD render, no time-based inventory cache or cross-frame player references. */
public final class HudSpellReadScope {
    private static final ThreadLocal<Frame> CURRENT=new ThreadLocal<>();
    public static final class Frame {
        private class_1657 player;
        private class_1799 stack;
        private SpellContainer snapshot;
        private boolean read;
        private void clear(){player=null;stack=null;snapshot=null;read=false;}
    }
    public static Frame enter(){Frame previous=CURRENT.get();CURRENT.set(new Frame());return previous;}
    public static void leave(Frame previous){Frame frame=CURRENT.get();if(frame!=null)frame.clear();if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
    public static SpellContainer equipped(class_1799 stack,class_1657 player,Operation<SpellContainer> original) {
        Frame frame=CURRENT.get();if(frame==null)return original.call(stack,player);
        if(frame.read&&frame.player==player&&frame.stack==stack)return frame.snapshot==null?null:frame.snapshot.copy();
        SpellContainer value=original.call(stack,player);
        // Unknown subtypes or malformed addon data keep their native semantics.
        if(value!=null&&(value.getClass()!=SpellContainer.class||value.spell_ids==null))return value;
        frame.player=player;frame.stack=stack;frame.snapshot=value==null?null:value.copy();frame.read=true;
        return value;
    }
    private HudSpellReadScope(){}
}
