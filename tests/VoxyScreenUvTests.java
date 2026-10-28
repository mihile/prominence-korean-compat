package local.prominence.work;
import java.nio.file.*;
public final class VoxyScreenUvTests {
 public static void main(String[] args)throws Exception {
  String source="void voxy_emitFragment() {}\nvec4 GetVolumetricLight() { float entityLinearDepth = texture(colortex13, texCoord).r; return pow2(entityLinearDepth) * far; }";
  String[] uniforms={"viewWidth","viewHeight"};
  String fixed=VoxyScreenUvFix.fix(source,uniforms);
  if(fixed.equals(source)||!fixed.contains("gl_FragCoord.xy / vec2(viewWidth, viewHeight)"))throw new AssertionError("Unbound depth not corrected");
  if(!VoxyScreenUvFix.fix(fixed,uniforms).equals(fixed))throw new AssertionError("Not idempotent");
  for(String input:new String[]{"vec2 texCoord;\n"+source,source.replace(".r;",".a;"),source.replace("colortex13","otherTexture")})
   if(!VoxyScreenUvFix.fix(input,uniforms).equals(input))throw new AssertionError("Unrelated source changed");
  if(!VoxyScreenUvFix.fix(source,new String[]{"viewWidth"}).equals(source))throw new AssertionError("Missing uniform changed");
  if(args.length>0){String dump=Files.readString(Path.of(args[0]));if(VoxyScreenUvFix.fix(dump,uniforms).equals(dump))throw new AssertionError("Actual failing shader not corrected");}
  System.out.println("PASS: Unbound lookup, guards, idempotence and optional real shader fixture");
 }
}
