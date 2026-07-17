package net.minecraft.world.item.enchantment;
public class Enchantment {
    private final String descriptionId;
    private final int minLevel;
    private final int maxLevel;
    public Enchantment(String descriptionId, int minLevel, int maxLevel) {
        this.descriptionId = descriptionId;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }
    public String getDescriptionId() { return descriptionId; }
    // Deliberately non-Mojmap names: validates the plugin's signature-based lookup.
    public int m_min_obfuscated() { return minLevel; }
    public int m_max_obfuscated() { return maxLevel; }
}
