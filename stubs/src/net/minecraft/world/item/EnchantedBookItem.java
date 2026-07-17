package net.minecraft.world.item;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
public class EnchantedBookItem {
    // Deliberately non-Mojmap name: validates signature-based factory lookup.
    public static ItemStack m_factory_obfuscated(EnchantmentInstance instance) {
        return new ItemStack(instance.enchantment, instance.level);
    }
}
