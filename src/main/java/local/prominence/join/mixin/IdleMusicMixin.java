package local.prominence.join.mixin;

import circuitlord.reactivemusic.PlayerThread;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.prominence.join.IdleSongWait;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="circuitlord.reactivemusic.PlayerThread", remap=false)
public abstract class IdleMusicMixin {
    @Shadow private volatile boolean queued;
    @WrapOperation(method="run", at=@At(value="FIELD", target="Lcircuitlord/reactivemusic/PlayerThread;kill:Z", opcode=Opcodes.GETFIELD), remap=false)
    private boolean prominence$waitForQueuedSong(PlayerThread self, Operation<Boolean> original) {
        return IdleSongWait.await(self, ()->queued, ()->original.call(self));
    }
    @Inject(method="play", at=@At("RETURN"), remap=false, require=1)
    private void prominence$wakeQueuedSong(String song, CallbackInfo ci) {
        IdleSongWait.signal((Thread)(Object)this);
    }
}
