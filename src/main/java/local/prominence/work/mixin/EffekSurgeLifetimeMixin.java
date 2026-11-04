package local.prominence.work.mixin;
import local.prominence.work.EffekGcScope;import mod.chloeprime.aaaparticles.api.client.EffectHolder;import mod.chloeprime.aaaparticles.api.client.EffectRegistry;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.Shadow;import org.spongepowered.asm.mixin.injection.At;import org.spongepowered.asm.mixin.injection.Inject;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=EffectHolder.class,remap=false) public abstract class EffekSurgeLifetimeMixin {
 @Shadow public abstract long getLastUsed();
 @Inject(method="unload",at=@At("HEAD"),cancellable=true) private void prominence$keepActiveSurge(CallbackInfo ci){var id=EffectRegistry.getKey((EffectHolder)(Object)this);if(id!=null&&EffekGcScope.protect(id.toString(),getLastUsed(),System.currentTimeMillis()))ci.cancel();}
}
