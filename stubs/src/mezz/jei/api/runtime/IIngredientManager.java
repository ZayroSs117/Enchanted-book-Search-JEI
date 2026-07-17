package mezz.jei.api.runtime;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientType;
import net.minecraft.world.item.ItemStack;
import java.util.Collection;
public interface IIngredientManager {
    default Collection<ItemStack> getAllItemStacks() {
        return getAllIngredients(VanillaTypes.ITEM_STACK);
    }
    <V> Collection<V> getAllIngredients(IIngredientType<V> ingredientType);
    <V> void addIngredientsAtRuntime(IIngredientType<V> ingredientType, Collection<V> ingredients);
    <V> void removeIngredientsAtRuntime(IIngredientType<V> ingredientType, Collection<V> ingredients);
}
