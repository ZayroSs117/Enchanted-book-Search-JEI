package net.minecraft.world.item;
import net.minecraft.world.item.enchantment.Enchantment;
public class ItemStack {
    public static final Item ENCHANTED_BOOK_ITEM = new Item("minecraft:enchanted_book");
    public final Enchantment enchantment;
    public final int level;
    public final Item item;
    public ItemStack(Enchantment enchantment, int level) {
        this(ENCHANTED_BOOK_ITEM, enchantment, level);
    }
    public ItemStack(Item item, Enchantment enchantment, int level) {
        this.item = item;
        this.enchantment = enchantment;
        this.level = level;
    }
    // Deliberately non-Mojmap name: validates signature-based item accessor lookup.
    public Item m_item_obfuscated() { return item; }
    @Override public String toString() {
        return item + ":" + enchantment.getDescriptionId() + "@" + level;
    }
}
