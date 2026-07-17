package dev.shadowsoffire.apotheosis.ench.asm;

import net.minecraft.world.item.enchantment.Enchantment;

public final class EnchHooks {
    private EnchHooks() {}

    public static int getMaxLevel(Enchantment enchantment) {
        return switch (enchantment.getDescriptionId()) {
            case "enchantment.minecraft.protection" -> 5;
            case "enchantment.minecraft.fire_protection" -> 5;
            case "enchantment.minecraft.lure" -> 8;
            case "enchantment.minecraft.mending" -> 1;
            case "enchantment.example.mod_enchant" -> 6;
            default -> enchantment.m_max_obfuscated();
        };
    }
}
