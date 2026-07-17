package mezz.jei.api;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
public interface IModPlugin {
    ResourceLocation getPluginUid();
    default void registerExtraIngredients(IExtraIngredientRegistration registration) {}
    default void registerIngredientAliases(IIngredientAliasRegistration registration) {}
    default void registerRecipes(IRecipeRegistration registration) {}
}
