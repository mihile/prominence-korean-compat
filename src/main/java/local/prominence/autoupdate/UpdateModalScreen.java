package local.prominence.autoupdate;

import net.minecraft.class_2561;
import net.minecraft.class_437;

/** Owns a captured menu frame; only modal controls receive input. */
abstract class UpdateModalScreen extends class_437 {
    final SnapshotLease<UpdateBackdrop> snapshot;
    private boolean released;
    protected UpdateModalScreen(class_2561 title){super(title);snapshot=UpdateBackdrop.capture();}
    @Override public void method_25432(){
        try{super.method_25432();}finally{if(!released){released=true;if(snapshot!=null)snapshot.release();}}
    }
}
