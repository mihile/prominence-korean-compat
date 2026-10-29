package local.prominence.autoupdate;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.class_1044;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3300;
import net.minecraft.class_332;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL12;

/** GPU-only snapshot: no parent Screen.render recursion, screenshot file or CPU image. */
final class UpdateBackdrop extends class_1044 {
    private static long sequence;
    private final class_2960 id=new class_2960("prominence_korean_compat","update_backdrop/"+(++sequence));
    private final int width,height;
    private UpdateBackdrop(){
        RenderSystem.assertOnRenderThread();
        var client=class_310.method_1551();var framebuffer=client.method_1522();
        width=framebuffer.field_1482;height=framebuffer.field_1481;
        if(width<=0 || height<=0)throw new IllegalStateException("Menu framebuffer is not ready");
        int texture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        try {
            GlStateManager._bindTexture(method_4624());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,framebuffer.field_1476);
            GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,0,0,width,height,0);
        }catch(RuntimeException|Error e){method_4528();throw e;}
        finally{GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);GlStateManager._bindTexture(texture);}
        client.method_1531().method_4616(id,this);
    }
    static SnapshotLease<UpdateBackdrop> capture(){
        var current=class_310.method_1551().field_1755;
        if(current instanceof UpdateModalScreen modal)return modal.snapshot==null?null:modal.snapshot.retain();
        try{return new SnapshotLease<>(new UpdateBackdrop(),background->class_310.method_1551().method_1531().method_4615(background.id));}
        catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger("Prominence updater").warn("Cannot capture menu backdrop; using a plain modal backdrop",e);return null;}
    }
    void draw(class_332 draw,int screenWidth,int screenHeight){
        RenderSystem.setShaderColor(1,1,1,1);
        // The framebuffer origin is bottom-left; GUI texture coordinates start at the top.
        draw.method_25293(id,0,0,screenWidth,screenHeight,0,(float)height,width,-height,width,height);
    }
    @Override public void method_4625(class_3300 resources){}
}
