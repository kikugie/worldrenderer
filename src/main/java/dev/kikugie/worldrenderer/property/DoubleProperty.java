package dev.kikugie.worldrenderer.property;

import com.google.common.base.Preconditions;
import net.minecraft.util.math.MathHelper;

public class DoubleProperty extends Property<Double> {
    private final double min;
    private final double max;
    private final double span;

    private boolean allowRollover = false;

    public DoubleProperty(double defaultValue, double min, double max) {
        super(defaultValue);
        Preconditions.checkArgument(min <= max);

        this.min = min;
        this.max = max;
        this.span = max - min;
    }

    public DoubleProperty withRollover() {
        this.allowRollover = true;
        return this;
    }

    public void modify(double by) {
        if (this.allowRollover) {
            this.value += by;
            if (this.value > this.max) this.value -= this.span;
            if (this.value < this.min) this.value += this.span;
        } else {
            this.value = MathHelper.clamp(this.value + by, this.min, this.max);
        }

        this.invokeListeners();
    }

    public double getProgress() {
        return (this.value - this.min) / this.span;
    }

    public void setProgress(double progress) {
        this.value = this.min + progress * this.span;
        this.invokeListeners();
    }

    public double getMax() {
        return this.max;
    }

    public double getMin() {
        return this.min;
    }
}