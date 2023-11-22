package dev.kikugie.worldrenderer.property;

import net.minecraft.client.util.math.MatrixStack;

public interface RenderPropertyBundle {
    DoubleProperty scale();

    IntegerProperty rotation();

    IntegerProperty slant();

    IntegerProperty lightAngle();

    IntegerProperty xOffset();

    IntegerProperty yOffset();

    void applyToViewMatrix(MatrixStack modelViewStack);
}