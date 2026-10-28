package local.prominence.work;
import java.util.*;

public final class StonecuttingTransferTests {
    static class Fixture implements StonecuttingTransfer.Steps {
        final List<String> packets=new ArrayList<>();boolean filled,failFill,wrongOutput;int selected=-1;int target=3;
        public boolean fill(){packets.add("fill");filled=!failFill;return filled;}
        public int recipeIndex(){if(!filled)throw new AssertionError("selection before input");return target;}
        public void select(int index){selected=index;packets.add("select:"+index);}
        public boolean correctOutput(){return !wrongOutput&&selected==target;}
        public void take(){if(!correctOutput())throw new AssertionError("wrong output taken");packets.add("take");}
    }
    public static void main(String[] args) {
        for(boolean take:new boolean[]{false,true}) {
            var f=new Fixture();if(!StonecuttingTransfer.execute(f,take))throw new AssertionError();
            if(!f.packets.equals(take?List.of("fill","select:3","take"):List.of("fill","select:3")))throw new AssertionError(f.packets);
        }
        var missing=new Fixture();missing.failFill=true;
        if(StonecuttingTransfer.execute(missing,true)||!missing.packets.equals(List.of("fill")))throw new AssertionError("failed fill crafted");
        var stale=new Fixture();stale.target=-1;
        if(StonecuttingTransfer.execute(stale,true)||!stale.packets.equals(List.of("fill")))throw new AssertionError("stale recipe crafted");
        var wrong=new Fixture();wrong.wrongOutput=true;
        if(StonecuttingTransfer.execute(wrong,true)||wrong.packets.contains("take"))throw new AssertionError("wrong output crafted");
        System.out.println("PASS: stonecutter fill/selection/take order, fill-only, missing materials, stale recipe and wrong-output stop");
    }
}
