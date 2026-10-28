package local.prominence.work;

/** Correct only the verified Unbound volumetric-depth lookup, not unrelated texture coordinates. */
public final class VoxyScreenUvFix {
    private VoxyScreenUvFix() {}
    public static String fix(String source,String[] uniforms) {
        if(source==null||uniforms==null||!source.contains("void voxy_emitFragment(")
                ||!source.contains("pow2(entityLinearDepth) * far"))return source;
        boolean width=false,height=false;
        for(String u:uniforms){width|="viewWidth".equals(u);height|="viewHeight".equals(u);}
        if(!width||!height||java.util.regex.Pattern.compile("\\bvec2\\s+texCoord\\b").matcher(source).find())return source;
        String lookup="float entityLinearDepth = texture(colortex13, texCoord).r;";
        int start=source.indexOf("vec4 GetVolumetricLight(");int pos=source.indexOf(lookup,start);
        if(start<0||pos<0||source.indexOf(lookup,pos+lookup.length())>=0)return source;
        return source.substring(0,pos)+lookup.replace("colortex13, texCoord","colortex13, gl_FragCoord.xy / vec2(viewWidth, viewHeight)")+source.substring(pos+lookup.length());
    }
}
