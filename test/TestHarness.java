import fr.lilian.enchantedbooksearch.EnchantedBookSearchJeiPlugin;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestHarness {
    public static void main(String[] args) {
        Enchantment protection = new Enchantment("enchantment.minecraft.protection", 1, 4);
        Enchantment fireProtection = new Enchantment("enchantment.minecraft.fire_protection", 1, 4);
        Enchantment lure = new Enchantment("enchantment.minecraft.lure", 1, 3);
        Enchantment mending = new Enchantment("enchantment.minecraft.mending", 1, 1);
        Enchantment modEnchant = new Enchantment("enchantment.example.mod_enchant", 2, 3);
        Enchantment customBook = new Enchantment("enchantment.example.custom_combination", 1, 1);

        // Registry order is intentionally meaningful and must be preserved.
        ForgeRegistries.setTestEnchantments(List.of(
                protection,
                fireProtection,
                lure,
                mending,
                modEnchant
        ));

        EnchantedBookSearchJeiPlugin plugin = new EnchantedBookSearchJeiPlugin();
        TestIngredientManager manager = new TestIngredientManager();

        // Simulate JEI's existing maximum-level books, which caused VIII, I, II... before 1.0.6.
        manager.addRaw(new ItemStack(protection, 5));
        manager.addRaw(new ItemStack(fireProtection, 5));
        manager.addRaw(new ItemStack(lure, 8));
        // Simulate a non-standard enchanted book contributed by another mod.
        manager.addRaw(new ItemStack(customBook, 99));

        plugin.registerExtraIngredients(new IExtraIngredientRegistration() {
            @Override
            public <V> void addExtraIngredients(IIngredientType<V> type, Collection<V> values) {
                manager.addIngredientsAtRuntime(type, values);
            }
        });

        List<String> aliases = new ArrayList<>();
        plugin.registerIngredientAliases(new IIngredientAliasRegistration() {
            @Override
            public <I> void addAlias(IIngredientType<I> type, I ingredient, String alias) {
                aliases.add(alias + " -> " + ingredient);
            }

            @Override
            public <I> void addAliases(IIngredientType<I> type, Collection<I> ingredients, String alias) {
                for (I ingredient : ingredients) {
                    aliases.add(alias + " -> " + ingredient);
                }
            }
        });

        plugin.registerRecipes(new IRecipeRegistration() {
            @Override
            public IIngredientManager getIngredientManager() {
                return manager;
            }
        });

        List<ItemStack> finalBooks = new ArrayList<>(manager.getAllItemStacks());
        List<String> expectedPrefix = new ArrayList<>();
        addExpected(expectedPrefix, protection, 1, 5);
        addExpected(expectedPrefix, fireProtection, 1, 5);
        addExpected(expectedPrefix, lure, 1, 8);
        addExpected(expectedPrefix, mending, 1, 1);
        addExpected(expectedPrefix, modEnchant, 2, 6);

        if (finalBooks.size() != expectedPrefix.size() + 1) {
            throw new AssertionError("Expected " + (expectedPrefix.size() + 1)
                    + " final books, got " + finalBooks.size() + ": " + finalBooks);
        }

        for (int i = 0; i < expectedPrefix.size(); i++) {
            String actual = key(finalBooks.get(i));
            if (!expectedPrefix.get(i).equals(actual)) {
                throw new AssertionError("Wrong order at " + i + ": expected "
                        + expectedPrefix.get(i) + ", got " + actual + "\nAll=" + finalBooks);
            }
        }

        ItemStack last = finalBooks.get(finalBooks.size() - 1);
        if (last.enchantment != customBook || last.level != 99) {
            throw new AssertionError("Custom enchanted book was not preserved at the end: " + last);
        }

        long lureAliases = aliases.stream().filter(s -> s.startsWith("lure ->")).count();
        if (lureAliases != 8) {
            throw new AssertionError("Expected 8 lure aliases, got " + lureAliases);
        }

        System.out.println("UID=" + plugin.getPluginUid());
        System.out.println("Final books=" + finalBooks.size());
        System.out.println("First=" + finalBooks.get(0));
        System.out.println("Lure sequence=" + finalBooks.subList(10, 18));
        System.out.println("Last=" + last);
        System.out.println("TEST OK");
    }

    private static void addExpected(List<String> expected, Enchantment enchantment, int min, int max) {
        for (int level = min; level <= max; level++) {
            expected.add(enchantment.getDescriptionId() + "@" + level);
        }
    }

    private static String key(ItemStack stack) {
        return stack.enchantment.getDescriptionId() + "@" + stack.level;
    }

    private static final class TestIngredientManager implements IIngredientManager {
        private final Map<String, ItemStack> items = new LinkedHashMap<>();

        void addRaw(ItemStack stack) {
            items.putIfAbsent(key(stack), stack);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <V> Collection<V> getAllIngredients(IIngredientType<V> ingredientType) {
            return (Collection<V>) List.copyOf(items.values());
        }

        @Override
        public <V> void addIngredientsAtRuntime(IIngredientType<V> ingredientType, Collection<V> ingredients) {
            for (V value : ingredients) {
                ItemStack stack = (ItemStack) value;
                items.putIfAbsent(key(stack), stack);
            }
        }

        @Override
        public <V> void removeIngredientsAtRuntime(IIngredientType<V> ingredientType, Collection<V> ingredients) {
            for (V value : ingredients) {
                ItemStack stack = (ItemStack) value;
                items.remove(key(stack));
            }
        }
    }
}
