package moe.caramel.chat;
import moe.caramel.chat.driver.*;
import moe.caramel.chat.wrapper.AbstractIMEWrapper;
import net.minecraft.class_437;
/** Test classpath facade only. Never distributed. Avoids JNI, GLFW and a Minecraft process. */
public final class Main {
    public static Operator current;
    private static final IController CONTROLLER=new IController(){
        public IOperator createOperator(AbstractIMEWrapper wrapper){return new Operator(this);}
        public void changeFocusedScreen(class_437 screen){}
        public void setFocus(boolean focused){}
    };
    public static IController getController(){return CONTROLLER;}
    public static final class Operator implements IOperator {
        private final IController owner;private boolean focused;
        Operator(IController owner){this.owner=owner;}
        public IController getController(){return owner;}
        public void setFocused(boolean value){focused=value;if(value)current=this;else if(current==this)current=null;}
        public boolean isFocused(){return focused;}
    }
}
