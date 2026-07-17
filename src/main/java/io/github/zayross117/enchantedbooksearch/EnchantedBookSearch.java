package io.github.zayross117.enchantedbooksearch;

import net.minecraftforge.fml.common.Mod;

/**
 * Client-side Forge entry point.
 *
 * <p>The actual feature is implemented by {@link EnchantedBookSearchJeiPlugin}.</p>
 */
@Mod(EnchantedBookSearch.MOD_ID)
public final class EnchantedBookSearch {
    public static final String MOD_ID = "enchantedbooksearch";

    public EnchantedBookSearch() {
        // No event registration is needed. JEI discovers the @JeiPlugin class itself.
    }
}
