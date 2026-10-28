package local.prominence.lootcredit;
import java.util.UUID;import net.minecraft.class_2487;
public final class LootCreditTests {
 public static void main(String[] args){var id=UUID.randomUUID();var a=new Credit(id,3,2);var b=new Credit(UUID.randomUUID(),1,0);
 if(CreditScope.current()!=null)throw new AssertionError();
 try(var outer=CreditScope.enter(a,true)){if(CreditScope.current().credit()!=a)throw new AssertionError();try(var inner=CreditScope.enter(b,false)){if(CreditScope.current().credit()!=b||CreditScope.current().anonymous())throw new AssertionError();}if(CreditScope.current().credit()!=a||!CreditScope.current().anonymous())throw new AssertionError();
 try(var empty=CreditScope.enter(null,false)){if(CreditScope.current().credit()!=null)throw new AssertionError();}
 try{try(var failure=CreditScope.enter(b,true)){throw new IllegalStateException();}}catch(IllegalStateException expected){}if(CreditScope.current().credit()!=a)throw new AssertionError();}
 if(CreditScope.current()!=null)throw new AssertionError("scope leaked");
 var tag=new class_2487();a.write(tag);if(!a.equals(Credit.read(tag)))throw new AssertionError("NBT roundtrip");if(Credit.read(new class_2487())!=null)throw new AssertionError("legacy NBT");
 System.out.println("PASS: immutable original bonuses, nested/reactive/unattributed scopes, exception restoration and real NBT persistence/legacy fallback");
 }
}
