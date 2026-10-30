package aztech.modern_industrialization.compat.viewer.impl.emi;
import dev.emi.emi.api.recipe.*;import dev.emi.emi.api.stack.*;import net.minecraft.*;import java.util.*;
public class ViewerCategoryEmi {
 public static class ViewerRecipe implements EmiRecipe {
  private final List<EmiIngredient> inputs,catalysts;private final List<EmiStack> outputs;
  public ViewerRecipe(List<EmiIngredient> i,List<EmiIngredient> c,List<EmiStack> o){inputs=i;catalysts=c;outputs=o;}
  public List<EmiIngredient> getInputs(){return inputs;}public List<EmiIngredient> getCatalysts(){return catalysts;}public List<EmiStack> getOutputs(){return outputs;}
  public class_2960 getId(){return null;}public EmiRecipeCategory getCategory(){return null;}
 }
}
