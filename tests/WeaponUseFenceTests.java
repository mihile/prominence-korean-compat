package local.prominence.weaponhud;
public final class WeaponUseFenceTests {
    private static int checks;
    static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    public static void main(String[] args){
        var f=new UsePressFence();
        check(net.minecraft.class_1269.field_5811.name().equals("PASS"),"Native PASS mapping");
        check(!f.blocks(true,true,false,false),"New air use allowed");
        check(f.blocks(true,true,true,false),"Container use belongs to container");
        check(!f.blocks(false,true,true,false),"Dedicated different ability key retains intent");
        check(!f.blocks(true,true,true,true),"Sneak bypass preserved before any interaction");
        f.consume();check(f.blocks(true,true,false,false),"Handled entity/block prevents same-press fallback");
        check(f.blocks(true,true,false,true),"Crouching does not resurrect already consumed press");
        check(!f.blocks(false,true,false,false),"Dedicated key remains independent after interaction");
        f.observe(true,true);f.observe(true,false);check(f.blocks(true,true,false,false),"Closing a screen while holding use must not fire");
        f.observe(false,false);check(!f.blocks(true,true,false,false),"Release then press restores normal use");
        for(int i=0;i<100;i++){f.observe(true,false);f.consume();f.observe(true,true);f.observe(true,false);check(f.blocks(true,true,false,false),"Repeated open/close stays consumed");f.observe(false,false);check(!f.blocks(true,true,false,false),"Each real release resets");}
        f.consume();f.reset();check(!f.blocks(true,true,false,false),"New connection resets old intent");
        System.out.println("PASS: "+checks+" weapon use intent checks; container/entity/screen, release, dedicated binding and reset; no game");
    }
}
