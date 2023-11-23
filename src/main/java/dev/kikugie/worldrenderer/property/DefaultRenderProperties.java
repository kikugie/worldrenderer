package dev.kikugie.worldrenderer.property;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public class DefaultRenderProperties implements RenderPropertyBundle {
    private final DoubleProperty scale;
    private final IntegerProperty rotation;
    private final IntegerProperty slant;
    private final IntegerProperty lightAngle;
    private final IntegerProperty xOffset;
    private final IntegerProperty yOffset;

    public DefaultRenderProperties(int maxScale, int defaultRotation, int defaultSlant) {
        this.scale = new DoubleProperty(100, 0, maxScale);
        this.rotation = new IntegerProperty(defaultRotation, 0, 360);
        this.slant = new IntegerProperty(defaultSlant, -90, 90);
        this.xOffset = new IntegerProperty(0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.yOffset = new IntegerProperty(0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.lightAngle = new IntegerProperty(-45, -45, 45);
    }

    @Override
    public DoubleProperty scale() {
        return this.scale;
    }

    @Override
    public IntegerProperty rotation() {
        return this.rotation;
    }

    @Override
    public IntegerProperty slant() {
        return this.slant;
    }

    @Override
    public IntegerProperty lightAngle() {
        return this.lightAngle;
    }

    @Override
    public IntegerProperty xOffset() {
        return this.xOffset;
    }

    @Override
    public IntegerProperty yOffset() {
        return this.yOffset;
    }

    @Override
    public void applyToViewMatrix(MatrixStack modelViewStack) {
        final float scale = (float) (this.scale.get() / 1000d);
        modelViewStack.scale(scale, scale, scale);

        modelViewStack.translate(this.xOffset.get() / 26000d, this.yOffset.get() / -26000d, 0);

        modelViewStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.slant.get()));
        modelViewStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotation.get()));
    }
}