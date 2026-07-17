package io.github.zayross117.enchantedbooksearch;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adds every actually-supported enchantment level as an enchanted-book ingredient in JEI,
 * then associates each book with searchable aliases for the enchantment.
 *
 * <p>The vanilla level methods are invoked structurally through reflection so this small
 * standalone build remains compatible with Forge's production mappings. Apotheosis changes
 * effective maximum levels through its own hook instead of overriding Enchantment#getMaxLevel,
 * so that public hook is detected and invoked reflectively when available.</p>
 */
@JeiPlugin
public final class EnchantedBookSearchJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID =
            new ResourceLocation(EnchantedBookSearch.MOD_ID, "jei_plugin");

    /** Defensive guard against a malformed configuration generating thousands of stacks. */
    private static final int ABSOLUTE_MAX_GENERATED_LEVEL = 255;

    private static final String[] APOTHEOSIS_HOOK_CLASSES = {
            "dev.shadowsoffire.apotheosis.ench.asm.EnchHooks",
            "shadows.apotheosis.ench.asm.EnchHooks"
    };

    private static final String[] APOTHEOSIS_MODULE_CLASSES = {
            "dev.shadowsoffire.apotheosis.ench.EnchModule",
            "shadows.apotheosis.ench.EnchModule"
    };

    private static volatile List<BookGroup> cachedBookGroups;
    private static volatile List<Method> levelMethods;
    private static volatile Method createBookMethod;
    private static volatile Method itemStackItemMethod;

    private static volatile boolean apotheosisLookupComplete;
    private static volatile Method apotheosisMaxHook;
    private static volatile Method apotheosisGetInfo;
    private static volatile Method apotheosisInfoGetMax;
    private static volatile String apotheosisSourceName;
    private static volatile boolean apotheosisInvocationWarningPrinted;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        List<ItemStack> books = new ArrayList<>();
        for (BookGroup group : getBookGroups()) {
            books.addAll(group.books());
        }
        registration.addExtraItemStacks(books);
        System.out.println("[EnchantedBookSearch] Added " + books.size()
                + " enchanted-book variants to JEI.");
    }

    @Override
    public void registerIngredientAliases(IIngredientAliasRegistration registration) {
        for (BookGroup group : getBookGroups()) {
            registration.addAliases(VanillaTypes.ITEM_STACK, group.books(), group.translationKey());
            registration.addAliases(VanillaTypes.ITEM_STACK, group.books(), group.readableRegistryName());
            registration.addAliases(VanillaTypes.ITEM_STACK, group.books(), group.registryName());
            if (!group.pathAlias().equals(group.readableRegistryName())) {
                registration.addAliases(VanillaTypes.ITEM_STACK, group.books(), group.pathAlias());
            }
        }
        System.out.println("[EnchantedBookSearch] Registered aliases for "
                + getBookGroups().size() + " enchantments.");
    }

    /**
     * JEI already contributes one book per enchantment, normally the effective maximum level.
     * Extra ingredients are appended after those existing books, which produces orders such as
     * VIII, I, II, III... Rebuild the enchanted-book section once all ingredients are available:
     * our standard books are inserted first in registry order and ascending level order, then any
     * non-standard enchanted-book stacks from other mods are restored afterwards. Duplicate stacks
     * are ignored by JEI, so the original maximum-level books cannot jump back to the front.
     */
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        IIngredientManager ingredientManager = registration.getIngredientManager();
        List<ItemStack> orderedBooks = flattenBooks();
        if (orderedBooks.isEmpty()) {
            return;
        }

        try {
            Object enchantedBookItem = readStackItem(orderedBooks.get(0));
            List<ItemStack> existingEnchantedBooks = new ArrayList<>();
            for (ItemStack stack : ingredientManager.getAllItemStacks()) {
                if (stack != null && readStackItem(stack) == enchantedBookItem) {
                    existingEnchantedBooks.add(stack);
                }
            }

            if (!existingEnchantedBooks.isEmpty()) {
                ingredientManager.removeIngredientsAtRuntime(
                        VanillaTypes.ITEM_STACK, existingEnchantedBooks);
            }

            ingredientManager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, orderedBooks);

            // Preserve unusual books supplied by other mods (for example multi-enchantment books).
            // JEI de-duplicates the standard books that are already present in orderedBooks.
            if (!existingEnchantedBooks.isEmpty()) {
                ingredientManager.addIngredientsAtRuntime(
                        VanillaTypes.ITEM_STACK, existingEnchantedBooks);
            }

            System.out.println("[EnchantedBookSearch] Reordered " + orderedBooks.size()
                    + " standard enchanted books in ascending level order; preserved "
                    + existingEnchantedBooks.size() + " previous enchanted-book entries.");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            System.err.println("[EnchantedBookSearch] Could not reorder enchanted books: "
                    + rootCause(error));
        }
    }

    private static List<ItemStack> flattenBooks() {
        List<ItemStack> books = new ArrayList<>();
        for (BookGroup group : getBookGroups()) {
            books.addAll(group.books());
        }
        return books;
    }

    private static List<BookGroup> getBookGroups() {
        List<BookGroup> result = cachedBookGroups;
        if (result == null) {
            synchronized (EnchantedBookSearchJeiPlugin.class) {
                result = cachedBookGroups;
                if (result == null) {
                    result = List.copyOf(createBookGroups());
                    cachedBookGroups = result;
                }
            }
        }
        return result;
    }

    private static List<BookGroup> createBookGroups() {
        List<Enchantment> enchantments = new ArrayList<>();
        for (Enchantment enchantment : ForgeRegistries.ENCHANTMENTS.getValues()) {
            if (enchantment != null) {
                enchantments.add(enchantment);
            }
        }
        List<BookGroup> groups = new ArrayList<>(enchantments.size());
        int skipped = 0;
        int resolvedByApotheosis = 0;
        int resolvedByVanilla = 0;

        for (Enchantment enchantment : enchantments) {
            try {
                ResourceLocation key = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
                if (key == null) {
                    skipped++;
                    continue;
                }

                String registryName = key.toString();
                String path = extractPath(registryName);
                String namespace = extractNamespace(registryName);
                String translationKey = "enchantment." + namespace + "." + path;
                String readableRegistryName = humanize(path);

                int[] vanillaRange = readVanillaLevelRange(enchantment);
                int minimum = Math.max(1, vanillaRange[0]);
                MaximumResult maximumResult = resolveEffectiveMaximum(enchantment, vanillaRange[1]);
                int maximum = Math.min(ABSOLUTE_MAX_GENERATED_LEVEL, maximumResult.maximum());

                if (maximumResult.apotheosis()) {
                    resolvedByApotheosis++;
                } else {
                    resolvedByVanilla++;
                }

                if (maximum < minimum) {
                    skipped++;
                    continue;
                }

                List<ItemStack> books = new ArrayList<>(maximum - minimum + 1);
                for (int level = minimum; level <= maximum; level++) {
                    ItemStack book = createBook(enchantment, level);
                    if (book != null) {
                        books.add(book);
                    }
                }

                if (!books.isEmpty()) {
                    groups.add(new BookGroup(
                            translationKey,
                            registryName,
                            readableRegistryName,
                            path,
                            List.copyOf(books)
                    ));
                } else {
                    skipped++;
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                skipped++;
                System.err.println("[EnchantedBookSearch] Skipped enchantment "
                        + safeSortKey(enchantment) + ": " + rootCause(error));
            }
        }

        System.out.println("[EnchantedBookSearch] Prepared " + groups.size()
                + " enchantments; skipped " + skipped + ".");
        System.out.println("[EnchantedBookSearch] Maximum levels resolved through Apotheosis="
                + resolvedByApotheosis + ", vanilla/mod overrides=" + resolvedByVanilla + ".");
        return groups;
    }

    /**
     * Finds the public no-argument int methods declared by Enchantment. In 1.20.1,
     * the positive results are the minimum and vanilla maximum level methods.
     * Invoking Methods from the base class still dispatches to normal modded overrides.
     */
    private static int[] readVanillaLevelRange(Enchantment enchantment)
            throws ReflectiveOperationException {
        List<Method> methods = levelMethods;
        if (methods == null) {
            synchronized (EnchantedBookSearchJeiPlugin.class) {
                methods = levelMethods;
                if (methods == null) {
                    List<Method> found = new ArrayList<>();
                    for (Method method : Enchantment.class.getDeclaredMethods()) {
                        if (!Modifier.isStatic(method.getModifiers())
                                && Modifier.isPublic(method.getModifiers())
                                && method.getParameterCount() == 0
                                && method.getReturnType() == int.class) {
                            found.add(method);
                        }
                    }
                    if (found.size() < 2) {
                        throw new NoSuchMethodException(
                                "Could not identify Enchantment minimum/maximum level methods; found "
                                        + found.size());
                    }
                    levelMethods = List.copyOf(found);
                    methods = levelMethods;
                }
            }
        }

        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        for (Method method : methods) {
            int value = ((Number) method.invoke(enchantment)).intValue();
            if (value > 0) {
                minimum = Math.min(minimum, value);
                maximum = Math.max(maximum, value);
            }
        }

        if (minimum == Integer.MAX_VALUE || maximum == Integer.MIN_VALUE) {
            throw new IllegalStateException("Enchantment returned no positive level range");
        }
        return new int[]{minimum, maximum};
    }

    /**
     * Resolves the real configured maximum. Apotheosis redirects Minecraft call sites to
     * EnchHooks#getMaxLevel, so a reflective call to Enchantment#getMaxLevel alone would
     * only return the original vanilla value. We therefore use the same hook Apotheosis uses.
     */
    private static MaximumResult resolveEffectiveMaximum(Enchantment enchantment, int fallbackMaximum) {
        discoverApotheosisHooks();

        Method hook = apotheosisMaxHook;
        if (hook != null) {
            try {
                Object value = hook.invoke(null, enchantment);
                if (value instanceof Number number && number.intValue() > 0) {
                    return new MaximumResult(number.intValue(), true);
                }
            } catch (IllegalAccessException | InvocationTargetException | RuntimeException | LinkageError error) {
                printApotheosisWarningOnce(error);
            }
        }

        Method getInfo = apotheosisGetInfo;
        if (getInfo != null) {
            try {
                Object info = getInfo.invoke(null, enchantment);
                if (info != null) {
                    Method getMax = apotheosisInfoGetMax;
                    if (getMax == null || getMax.getDeclaringClass() != info.getClass()) {
                        getMax = findNoArgIntMethod(info.getClass(), "getMaxLevel");
                        apotheosisInfoGetMax = getMax;
                    }
                    if (getMax != null) {
                        Object value = getMax.invoke(info);
                        if (value instanceof Number number && number.intValue() > 0) {
                            return new MaximumResult(number.intValue(), true);
                        }
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException | RuntimeException | LinkageError error) {
                printApotheosisWarningOnce(error);
            }
        }

        return new MaximumResult(Math.max(1, fallbackMaximum), false);
    }

    private static void discoverApotheosisHooks() {
        if (apotheosisLookupComplete) {
            return;
        }
        synchronized (EnchantedBookSearchJeiPlugin.class) {
            if (apotheosisLookupComplete) {
                return;
            }

            for (String className : APOTHEOSIS_HOOK_CLASSES) {
                try {
                    Class<?> hookClass = Class.forName(
                            className, false, EnchantedBookSearchJeiPlugin.class.getClassLoader());
                    Method method = findStaticEnchantmentIntMethod(hookClass, "getMaxLevel");
                    if (method != null) {
                        apotheosisMaxHook = method;
                        apotheosisSourceName = className + "#" + method.getName();
                        break;
                    }
                } catch (ClassNotFoundException | LinkageError ignored) {
                    // Apotheosis is optional.
                }
            }

            if (apotheosisMaxHook == null) {
                for (String className : APOTHEOSIS_MODULE_CLASSES) {
                    try {
                        Class<?> moduleClass = Class.forName(
                                className, false, EnchantedBookSearchJeiPlugin.class.getClassLoader());
                        Method method = findStaticEnchantmentObjectMethod(moduleClass, "getEnchInfo");
                        if (method != null) {
                            apotheosisGetInfo = method;
                            apotheosisSourceName = className + "#" + method.getName();
                            break;
                        }
                    } catch (ClassNotFoundException | LinkageError ignored) {
                        // Apotheosis is optional.
                    }
                }
            }

            apotheosisLookupComplete = true;
            if (apotheosisSourceName != null) {
                System.out.println("[EnchantedBookSearch] Detected effective-level provider: "
                        + apotheosisSourceName + ".");
            } else {
                System.out.println("[EnchantedBookSearch] No Apotheosis level provider detected; "
                        + "using each enchantment's declared range.");
            }
        }
    }

    private static Method findStaticEnchantmentIntMethod(Class<?> owner, String preferredName) {
        Method fallback = null;
        for (Method method : owner.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                    || method.getParameterCount() != 1
                    || method.getReturnType() != int.class
                    || !method.getParameterTypes()[0].isAssignableFrom(Enchantment.class)) {
                continue;
            }
            makeAccessible(method);
            if (method.getName().equals(preferredName)) {
                return method;
            }
            if (fallback == null) {
                fallback = method;
            }
        }
        return fallback;
    }

    private static Method findStaticEnchantmentObjectMethod(Class<?> owner, String preferredName) {
        Method fallback = null;
        for (Method method : owner.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                    || method.getParameterCount() != 1
                    || method.getReturnType() == void.class
                    || method.getReturnType().isPrimitive()
                    || !method.getParameterTypes()[0].isAssignableFrom(Enchantment.class)) {
                continue;
            }
            makeAccessible(method);
            if (method.getName().equals(preferredName)) {
                return method;
            }
            if (fallback == null) {
                fallback = method;
            }
        }
        return fallback;
    }

    private static Method findNoArgIntMethod(Class<?> owner, String preferredName) {
        Method fallback = null;
        for (Method method : owner.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())
                    || method.getParameterCount() != 0
                    || method.getReturnType() != int.class) {
                continue;
            }
            makeAccessible(method);
            if (method.getName().equals(preferredName)) {
                return method;
            }
            if (fallback == null) {
                fallback = method;
            }
        }
        return fallback;
    }

    private static void makeAccessible(Method method) {
        try {
            method.trySetAccessible();
        } catch (RuntimeException ignored) {
            // Public methods remain invokable without opening the class.
        }
    }

    private static void printApotheosisWarningOnce(Throwable error) {
        if (!apotheosisInvocationWarningPrinted) {
            synchronized (EnchantedBookSearchJeiPlugin.class) {
                if (!apotheosisInvocationWarningPrinted) {
                    apotheosisInvocationWarningPrinted = true;
                    System.err.println("[EnchantedBookSearch] The detected Apotheosis level provider failed; "
                            + "falling back to declared levels: " + rootCause(error));
                }
            }
        }
    }


    /** Finds ItemStack's no-argument method that returns its underlying Item by signature. */
    private static Object readStackItem(ItemStack stack) throws ReflectiveOperationException {
        Method method = itemStackItemMethod;
        if (method == null) {
            synchronized (EnchantedBookSearchJeiPlugin.class) {
                method = itemStackItemMethod;
                if (method == null) {
                    for (Method candidate : ItemStack.class.getDeclaredMethods()) {
                        if (!Modifier.isStatic(candidate.getModifiers())
                                && candidate.getParameterCount() == 0
                                && candidate.getReturnType().getName()
                                        .equals("net.minecraft.world.item.Item")) {
                            method = candidate;
                            makeAccessible(method);
                            break;
                        }
                    }
                    if (method == null) {
                        throw new NoSuchMethodException(
                                "Could not find ItemStack item accessor");
                    }
                    itemStackItemMethod = method;
                }
            }
        }
        return method.invoke(stack);
    }

    /** Finds EnchantedBookItem's static (EnchantmentInstance) -> ItemStack factory by signature. */
    private static ItemStack createBook(Enchantment enchantment, int level)
            throws ReflectiveOperationException {
        Method method = createBookMethod;
        if (method == null) {
            synchronized (EnchantedBookSearchJeiPlugin.class) {
                method = createBookMethod;
                if (method == null) {
                    for (Method candidate : EnchantedBookItem.class.getDeclaredMethods()) {
                        if (Modifier.isStatic(candidate.getModifiers())
                                && candidate.getParameterCount() == 1
                                && candidate.getParameterTypes()[0] == EnchantmentInstance.class
                                && candidate.getReturnType() == ItemStack.class) {
                            method = candidate;
                            makeAccessible(method);
                            break;
                        }
                    }
                    if (method == null) {
                        throw new NoSuchMethodException(
                                "Could not find EnchantedBookItem enchantment-book factory");
                    }
                    createBookMethod = method;
                }
            }
        }

        Object result = method.invoke(null, new EnchantmentInstance(enchantment, level));
        return result instanceof ItemStack stack ? stack : null;
    }

    private static Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String safeSortKey(Enchantment enchantment) {
        try {
            ResourceLocation key = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            return key == null ? enchantment.getClass().getName() : key.toString();
        } catch (RuntimeException | LinkageError ignored) {
            return enchantment.getClass().getName();
        }
    }

    private static String extractNamespace(String registryName) {
        int separator = registryName.indexOf(':');
        return separator > 0 ? registryName.substring(0, separator) : "minecraft";
    }

    private static String extractPath(String registryName) {
        int separator = registryName.indexOf(':');
        return separator >= 0 && separator + 1 < registryName.length()
                ? registryName.substring(separator + 1)
                : registryName;
    }

    private static String humanize(String path) {
        return path.replace('_', ' ').replace('-', ' ').toLowerCase(Locale.ROOT);
    }

    private record MaximumResult(int maximum, boolean apotheosis) {
    }

    private record BookGroup(
            String translationKey,
            String registryName,
            String readableRegistryName,
            String pathAlias,
            List<ItemStack> books
    ) {
    }
}
