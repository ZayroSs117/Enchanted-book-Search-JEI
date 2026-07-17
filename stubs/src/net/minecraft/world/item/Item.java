package net.minecraft.world.item;
public class Item {
    private final String id;
    public Item() { this("item"); }
    public Item(String id) { this.id = id; }
    @Override public String toString() { return id; }
}
