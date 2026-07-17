package net.minecraftforge.registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
public final class ForgeRegistries {
    private ForgeRegistries() {}
    private static final List<Enchantment> VALUES = new ArrayList<>();
    private static final Map<Enchantment, ResourceLocation> KEYS = new IdentityHashMap<>();
    public static final IForgeRegistry<Enchantment> ENCHANTMENTS = new IForgeRegistry<>() {
        @Override public Collection<Enchantment> getValues() { return VALUES; }
        @Override public ResourceLocation getKey(Enchantment value) { return KEYS.get(value); }
    };
    public static void setTestEnchantments(Collection<Enchantment> enchantments) {
        VALUES.clear(); KEYS.clear();
        int index = 0;
        for (Enchantment enchantment : enchantments) {
            VALUES.add(enchantment);
            String id = enchantment.getDescriptionId();
            String path = id == null ? "test_" + index : id.substring(id.lastIndexOf('.') + 1);
            KEYS.put(enchantment, new ResourceLocation("test", path));
            index++;
        }
    }
}
