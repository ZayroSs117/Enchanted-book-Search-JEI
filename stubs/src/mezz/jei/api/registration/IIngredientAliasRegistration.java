package mezz.jei.api.registration;
import mezz.jei.api.ingredients.IIngredientType;
import java.util.Collection;
public interface IIngredientAliasRegistration {
    <I> void addAlias(IIngredientType<I> type, I ingredient, String alias);
    <I> void addAliases(IIngredientType<I> type, Collection<I> ingredients, String alias);
}
