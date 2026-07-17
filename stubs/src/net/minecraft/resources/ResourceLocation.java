package net.minecraft.resources;
public class ResourceLocation {
    private final String namespace;
    private final String path;
    public ResourceLocation(String namespace, String path) { this.namespace = namespace; this.path = path; }
    @Override public String toString() { return namespace + ":" + path; }
}
