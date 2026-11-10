package io.github.brooswitminecraft.layeredslopes;

import net.minecraft.util.StringRepresentable;

/** Which face of the block cell the layers are attached to. */
public enum Anchor implements StringRepresentable {
    FLOOR("floor"),
    CEILING("ceiling");

    private final String name;

    Anchor(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public boolean isCeiling() {
        return this == CEILING;
    }
}
