package net.minecraftforge.registries;
import net.minecraft.resources.ResourceLocation;
import java.util.Collection;
public interface IForgeRegistry<T> {
    Collection<T> getValues();
    ResourceLocation getKey(T value);
}
