package local.prominence.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/** Reads real installed native bytes without initializing the game or loader. */
public final class BetterDaysGuardTests {
    private static int checks;
    private static void check(boolean value){checks++;if(!value)throw new AssertionError("Guard check "+checks);}
    public static void main(String[] args) throws Exception {
        Map<String,byte[]> bytes=new HashMap<>();
        for(String name:List.of("betterdays/client/TimeInterpolator.class","betterdays/mixin/MinecraftClientMixin.class","betterdays/mixin/ClientLevelMixin.class","betterdays/wrappers/ClientLevelWrapper.class")) {
            try(var in=BetterDaysGuardTests.class.getClassLoader().getResourceAsStream(name)) {
                if(in==null)throw new AssertionError("Missing installed dependency: "+name);bytes.put(name,in.readAllBytes());
            }
        }
        check(BetterDaysLifecycleGuard.matches(bytes));
        for(String name:List.copyOf(bytes.keySet())) {
            byte[] nativeBytes=bytes.get(name),altered=nativeBytes.clone();altered[altered.length/2]^=1;
            bytes.put(name,altered);check(!BetterDaysLifecycleGuard.matches(bytes));
            bytes.remove(name);check(!BetterDaysLifecycleGuard.matches(bytes));
            bytes.put(name,nativeBytes);check(BetterDaysLifecycleGuard.matches(bytes));
        }
        check(!BetterDaysLifecycleGuard.matches(Map.of()));
        System.out.println("PASS: "+checks+" actual BetterDays fingerprints/missing/modified guards");
    }
}
