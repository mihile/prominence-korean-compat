package local.prominence.work;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.registry.EmiRecipeFiller;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.*;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter.StonecutterUpgradeContainer;

/** Uses the backpack's existing slots and selection packet; no custom server protocol. */
public final class BackpackStonecutting implements StandardRecipeHandler<StorageContainerMenuBase<?>> {
    public static final BackpackStonecutting INSTANCE = new BackpackStonecutting();

    public static StonecutterUpgradeContainer find(StorageContainerMenuBase<?> menu) {
        var open=menu.getOpenContainer().orElse(null);
        if(open instanceof StonecutterUpgradeContainer stone && menu.getUpgradeEnabled(stone.getUpgradeContainerId())) return stone;
        return menu.getUpgradeContainers().values().stream()
            .filter(c->c instanceof StonecutterUpgradeContainer && menu.getUpgradeEnabled(c.getUpgradeContainerId()))
            .map(c->(StonecutterUpgradeContainer)c)
            .min(Comparator.comparingInt(c->c.getUpgradeContainerId())).orElse(null);
    }
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe.getCategory()==VanillaEmiRecipeCategories.STONECUTTING && recipe.supportsRecipeTree()
            && recipe.getId()!=null && recipe.getInputs().size()==1 && recipe.getOutputs().size()==1;
    }
    public List<class_1735> getInputSources(StorageContainerMenuBase<?> menu) {
        var slots=new ArrayList<class_1735>(menu.realInventorySlots);
        var stone=find(menu);
        if(stone!=null && !slots.contains(stone.getRecipeContainer().getInputSlot())) slots.add(stone.getRecipeContainer().getInputSlot());
        return slots;
    }
    public List<class_1735> getCraftingSlots(StorageContainerMenuBase<?> menu) {
        var stone=find(menu);
        return stone==null?List.of():List.of(stone.getRecipeContainer().getInputSlot());
    }
    public class_1735 getOutputSlot(StorageContainerMenuBase<?> menu) {
        var stone=find(menu);
        return stone==null?null:stone.getRecipeContainer().getOutputSlot();
    }
    public boolean canCraft(EmiRecipe recipe,EmiCraftContext<StorageContainerMenuBase<?>> context) {
        var menu=context.getScreenHandler();
        return supportsRecipe(recipe) && find(menu)!=null && menu.method_34255().method_7960()
            && EmiRecipeFiller.getStacks(this,recipe,context.getScreen(),context.getAmount())!=null;
    }
    public boolean craft(EmiRecipe recipe,EmiCraftContext<StorageContainerMenuBase<?>> context) {
        var client=class_310.method_1551();var menu=context.getScreenHandler();
        if(client.field_1724==null || client.field_1761==null || client.field_1687==null || client.field_1724.field_7512!=menu || !canCraft(recipe,context)) return false;
        var stone=find(menu);
        var stacks=EmiRecipeFiller.getStacks(this,recipe,context.getScreen(),context.getAmount());
        if(stacks==null || stacks.size()!=1 || stacks.get(0).method_7960()) return false;
        // Reject stale/decorative EMI recipes before moving any items.
        var matches=client.field_1687.method_8433().method_17877(class_3956.field_17641,new class_1277(stacks.get(0)),client.field_1687);
        if(matches.stream().noneMatch(r->recipe.getId().equals(r.method_8114()))) return false;
        if(!stone.isOpen()) {
            menu.getOpenContainer().ifPresent(open->{open.setIsOpen(false);menu.setOpenTabId(-1);});
            stone.setIsOpen(true);menu.setOpenTabId(stone.getUpgradeContainerId());
        }
        client.method_1507(context.getScreen());
        // Vanilla click packets update the local input synchronously and preserve packet order:
        // move ingredient -> native backpack recipe selection -> optional result click.
        // The optimized fill packet cannot select which stonecutting result to take.
        var container=stone.getRecipeContainer();
        return StonecuttingTransfer.execute(new StonecuttingTransfer.Steps() {
            public boolean fill() { return EmiRecipeFiller.clientFill(BackpackStonecutting.this,recipe,context.getScreen(),stacks,EmiCraftContext.Destination.NONE); }
            public int recipeIndex() {
                var available=container.getRecipeList();
                for(int i=0;i<available.size();i++) if(recipe.getId().equals(available.get(i).method_8114())) return i;
                return -1;
            }
            public void select(int index) {container.selectRecipe(index);}
            public boolean correctOutput() {
                var actual=container.getOutputSlot().method_7677();var expected=recipe.getOutputs().get(0).getItemStack();
                return class_1799.method_31577(actual,expected) && actual.method_7947()==expected.method_7947();
            }
            public void take() {
                var action=context.getDestination()==EmiCraftContext.Destination.CURSOR?class_1713.field_7790:class_1713.field_7794;
                client.field_1761.method_2906(menu.field_7763,container.getOutputSlot().field_7874,0,action,client.field_1724);
            }
        },context.getDestination()!=EmiCraftContext.Destination.NONE);
    }
}
