package mezz.jei.api.registration;
import mezz.jei.api.ingredients.IIngredientType;
import net.minecraft.world.item.ItemStack;
import java.util.Collection;
public interface IExtraIngredientRegistration {
    default void addExtraItemStacks(Collection<ItemStack> extraItemStacks) {
        addExtraIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, extraItemStacks);
    }
    <V> void addExtraIngredients(IIngredientType<V> ingredientType, Collection<V> extraIngredients);
}
