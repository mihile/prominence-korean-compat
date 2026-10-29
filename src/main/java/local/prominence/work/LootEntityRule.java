package local.prominence.work;

import java.util.List;
import java.util.function.Function;

/** Reject only impossible positive victim-type predicates. Unknown predicates remain visible. */
public final class LootEntityRule {
    public enum Result { YES,NO,UNKNOWN }
    public record Node(String key,String literal,List<Node> args,List<Node> siblings) {
        public Node {args=List.copyOf(args);siblings=List.copyOf(siblings);}
    }
    private LootEntityRule(){}
    public static boolean contains(Node n,String key){return n.key().equals(key)||n.args().stream().anyMatch(s->contains(s,key))||n.siblings().stream().anyMatch(s->contains(s,key));}
    public static boolean mayDisplay(List<Node> nodes,String victim,Function<String,Result> tags){
        if(nodes.stream().noneMatch(n->contains(n,"item.botania.elementium_axe")))return true;
        return nodes.stream().noneMatch(n->evaluate(n,victim,tags)==Result.NO);
    }
    private static Result and(List<Result> values){boolean unknown=false;for(var v:values){if(v==Result.NO)return Result.NO;if(v==Result.UNKNOWN)unknown=true;}return unknown?Result.UNKNOWN:Result.YES;}
    private static Result or(List<Result> values){boolean unknown=false;for(var v:values){if(v==Result.YES)return Result.YES;if(v==Result.UNKNOWN)unknown=true;}return unknown?Result.UNKNOWN:Result.NO;}
    public static Result evaluate(Node node,String victimNameKey,Function<String,Result> tags){return evaluate(node,victimNameKey,tags,false,0);}
    private static Result evaluate(Node n,String victim,Function<String,Result> tags,boolean self,int depth){
        if(depth>32)return Result.UNKNOWN;
        String key=n.key();
        if(key.equals("emi_loot.condition.invert") || key.equals("emi_loot.entity_predicate.entity_killer"))return Result.UNKNOWN;
        // Three-or-more terms are represented by an empty root with sibling separator wrappers.
        if(key.isEmpty() && !n.siblings().isEmpty() && n.siblings().stream().allMatch(s->s.key().startsWith("emi_loot.condition.any_of")))
            return or(n.siblings().stream().map(s->evaluate(s,victim,tags,self,depth+1)).toList());
        Result result=Result.UNKNOWN;
        if(key.startsWith("emi_loot.condition.any_of"))result=or(n.args().stream().map(s->evaluate(s,victim,tags,self,depth+1)).toList());
        else if(key.startsWith("emi_loot.condition.all_of") || key.equals("emi_loot.condition.entity_props"))result=and(n.args().stream().map(s->evaluate(s,victim,tags,self,depth+1)).toList());
        else if(key.equals("emi_loot.entity_predicate.entity_this"))result=and(n.args().stream().map(s->evaluate(s,victim,tags,true,depth+1)).toList());
        else if(self && key.equals("emi_loot.entity_predicate.type_single") && n.args().size()==1){
            String required=n.args().get(0).key();
            if(required.startsWith("entity."))result=required.equals(victim)?Result.YES:Result.NO;
        }else if(self && key.equals("emi_loot.entity_predicate.type_tag") && n.args().size()==1){
            String tag=n.args().get(0).literal();if(tag.startsWith("#"))result=tags.apply(tag.substring(1));
        }
        // A compiled conjunction can append more predicates to its first Text node.
        if(!n.siblings().isEmpty()){
            var values=new java.util.ArrayList<Result>();values.add(result);
            for(var sibling:n.siblings())values.add(evaluate(sibling,victim,tags,self,depth+1));
            result=and(values);
        }
        return result;
    }
}
