package local.prominence.memory;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.*;
import mod.azure.azurelib.core.molang.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Native AzureLib parser/memoizer/variable objects, ordinary owner stand-ins, no game/GL. */
public final class AzureQueryPrunerTests {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static final class World {final boolean client;final double value;World(boolean c,double v){client=c;value=v;}}
    static final class Entity {final World world;Entity(World w){world=w;}}
    static DoubleSupplier world(World owner){return ()->owner.value;}
    static DoubleSupplier entity(Entity owner){return ()->owner.world.value;}
    static Predicate<Object> obsolete(World current){return owner->owner instanceof World w&&w.client&&w!=current||owner instanceof Entity e&&e.world.client&&e.world!=current;}
    static WeakReference<World> retained(MolangParser parser){
        World previous=new World(true,93);parser.setMemoizedValue(MolangQueries.ACTOR_COUNT,world(previous));return new WeakReference<>(previous);
    }
    static void collect(WeakReference<?> reference)throws Exception{for(int i=0;i<40&&reference.get()!=null;i++){System.gc();Thread.sleep(15);}}
    static byte[] raw(String name)throws Exception{try(var in=AzureQueryPrunerTests.class.getClassLoader().getResourceAsStream(name)){return in.readAllBytes();}}
    public static void main(String[]args)throws Exception{
        var parser=MolangParser.INSTANCE;var vars=MolangParser.VARIABLES;var cleanup=new AzureQueryPruner(AzureQueryPrunerTests.class);
        var current=new World(true,22);var old=new World(true,11);var server=new World(false,33);
        parser.setMemoizedValue(MolangQueries.ACTOR_COUNT,world(old));parser.setMemoizedValue(MolangQueries.MOON_PHASE,world(current));
        parser.setMemoizedValue(MolangQueries.HEALTH,entity(new Entity(old)));parser.setMemoizedValue(MolangQueries.MAX_HEALTH,entity(new Entity(current)));
        parser.setMemoizedValue(MolangQueries.IS_ON_FIRE,entity(new Entity(server)));parser.setMemoizedValue("query.addon_private",world(old));
        parser.setMemoizedValue(MolangQueries.IS_IN_WATER,OtherAzureQueryOwner.provider(old));
        parser.setValue(MolangQueries.YAW_SPEED,world(old)); // Unknown nonmemoized direct provider stays untouched.
        LazyVariable actor=parser.getVariable(MolangQueries.ACTOR_COUNT),active=parser.getVariable(MolangQueries.MOON_PHASE);
        check(actor.get()==11,"native memoized value before cleanup");check(active.get()==22,"active native value");
        check(cleanup.release(vars,obsolete(current))==2,"only native old world/entity providers");
        check(actor==parser.getVariable(MolangQueries.ACTOR_COUNT)&&actor.get()==0,"parsed-variable identity/value preserved");
        check(active==parser.getVariable(MolangQueries.MOON_PHASE)&&active.get()==22,"current query changed");
        check(parser.getVariable(MolangQueries.MAX_HEALTH).get()==22,"active entity changed");
        check(parser.getVariable(MolangQueries.IS_ON_FIRE).get()==33,"server owner changed");
        check(parser.getVariable("query.addon_private").get()==11,"custom query changed");
        check(parser.getVariable(MolangQueries.IS_IN_WATER).get()==11,"third-party provider replaced");
        check(parser.getVariable(MolangQueries.YAW_SPEED).get()==11,"direct supplier changed");
        check(cleanup.release(vars,obsolete(current))==0,"repeat cleanup changed stable values");
        parser.setMemoizedValue(MolangQueries.ACTOR_COUNT,world(current));
        check(actor.get()==22,"native registration did not repopulate same variable");
        parser.setMemoizedValue(MolangQueries.ACTOR_COUNT,world(old));
        check(cleanup.release(vars,value->{if(value==old)parser.setMemoizedValue(MolangQueries.ACTOR_COUNT,world(current));return value==old;})==0,"reentrant replacement cleared");
        check(actor.get()==22,"replacement value lost");
        // Drop all deliberately preserved old references before checking the actual retained graph.
        vars.clear();WeakReference<World> reference=retained(parser);LazyVariable held=parser.getVariable(MolangQueries.ACTOR_COUNT);
        collect(reference);check(reference.get()!=null,"native retained-owner fixture missing");
        check(cleanup.release(vars,obsolete(current))==1,"native retained query not released");collect(reference);
        check(reference.get()==null&&held.get()==0,"world retained while parsed LazyVariable stays alive");
        check(cleanup.release(vars,obsolete(null))==0,"released provider processed again");
        parser.setMemoizedValue(MolangQueries.MOON_PHASE,world(current));
        check(cleanup.release(vars,obsolete(null))==1,"disconnect did not release last client owner");
        check(parser.getVariable(MolangQueries.MOON_PHASE).get()==0,"disconnect neutral value incorrect");
        // Guard includes all native objects traversed, plus the lambda producer and fixed key list.
        var versions=new HashMap<String,String>();var bytes=new HashMap<String,byte[]>();
        for(var pin:WorldCacheGuard.pins("azureQueries")){
            versions.put(pin.mod(),pin.version());for(var p:pin.classes().entrySet())bytes.put(pin.mod()+"/"+p.getKey(),raw(p.getKey()));
        }
        check(WorldCacheGuard.matches("azureQueries",versions,bytes),"native pins mismatch");
        var changed=new HashMap<>(bytes);String first=changed.keySet().iterator().next();changed.put(first,new byte[]{1});check(!WorldCacheGuard.matches("azureQueries",versions,changed),"changed native class accepted");
        var missing=new HashMap<>(bytes);missing.remove(first);check(!WorldCacheGuard.matches("azureQueries",versions,missing),"missing native class accepted");
        versions.put("azurelib","3.0.28");check(!WorldCacheGuard.matches("azureQueries",versions,bytes),"version drift accepted");
        // Inspect actual GeoModel lambda bootstrap captures, not decompiler's rewritten source.
        var model=new ClassNode();new ClassReader(raw("mod/azure/azurelib/model/GeoModel.class")).accept(model,0);int worldCaptures=0,entityCaptures=0;
        var apply=model.methods.stream().filter(m->m.name.equals("applyMolangQueries")).findFirst().orElseThrow();
        for(var insn:apply.instructions)if(insn instanceof InvokeDynamicInsnNode dynamic){
            if(dynamic.desc.startsWith("(Lnet/minecraft/class_638;"))worldCaptures++;
            if(dynamic.desc.startsWith("(Lnet/minecraft/class_1297;")||dynamic.desc.startsWith("(Lnet/minecraft/class_1309;"))entityCaptures++;
        }
        check(worldCaptures==2,"native world capture contract differs");check(entityCaptures>=8,"native entity capture contract differs");
        vars.clear();System.out.println("PASS: "+checks+" native AzureLib memoizer/parsed-variable lifecycle, GC retention/release, current/server/custom/replacement preservation and bytecode/pin checks; no game/world/GL");
    }
}

final class OtherAzureQueryOwner {
    static DoubleSupplier provider(AzureQueryPrunerTests.World owner){return ()->owner.value;}
}
