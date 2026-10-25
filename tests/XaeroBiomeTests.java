package local.prominence.memory;
import java.util.*;
public final class XaeroBiomeTests {
    public static void main(String[]args)throws Exception {
        var bytes=new HashMap<String,byte[]>();int checks=0;
        for(String n:List.of("xaero/common/minimap/write/MinimapWriter.class","xaero/common/minimap/write/biome/BiomeBlendCalculator.class")){
            try(var in=XaeroBiomeTests.class.getClassLoader().getResourceAsStream(n)){if(in==null)throw new AssertionError("Native fixture missing "+n);bytes.put(n,in.readAllBytes());}
        }
        if(!XaeroBiomeGuard.matches("1.20.1","26.6.0","0.5.4",bytes))throw new AssertionError("Actual pins rejected");checks++;
        for(String[] versions:List.of(new String[]{"1.20.2","26.6.0","0.5.4"},new String[]{"1.20.1","26.6.1","0.5.4"},new String[]{"1.20.1","26.6.0","0.5.3"})){
            if(XaeroBiomeGuard.matches(versions[0],versions[1],versions[2],bytes))throw new AssertionError("Wrong version accepted");checks++;
        }
        for(String key:List.copyOf(bytes.keySet())){
            byte[] original=bytes.remove(key);if(XaeroBiomeGuard.matches("1.20.1","26.6.0","0.5.4",bytes))throw new AssertionError("Missing class accepted");checks++;
            byte[] changed=original.clone();changed[changed.length-1]^=1;bytes.put(key,changed);if(XaeroBiomeGuard.matches("1.20.1","26.6.0","0.5.4",bytes))throw new AssertionError("Changed class accepted");checks++;bytes.put(key,original);
        }
        String prior=System.getProperty("prominence.xaeroBiomeScope");try{
            System.setProperty("prominence.xaeroBiomeScope","false");var guard=new XaeroBiomeGuard();guard.onLoad("fixture");
            if(guard.shouldApplyMixin("any","any"))throw new AssertionError("Opt-out ignored");checks++;
        }finally{if(prior==null)System.clearProperty("prominence.xaeroBiomeScope");else System.setProperty("prominence.xaeroBiomeScope",prior);}
        System.out.println("PASS: "+checks+" Xaero native versions/fingerprints/missing/modified/opt-out checks; no world/game.");
    }
}
