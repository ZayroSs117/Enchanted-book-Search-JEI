package net.minecraft.world.item.enchantment;
public class EnchantmentInstance {
    public final Enchantment enchantment;
    public final int level;
    public EnchantmentInstance(Enchantment enchantment, int level) {
        this.enchantment = enchantment;
        this.level = level;
    }
}
