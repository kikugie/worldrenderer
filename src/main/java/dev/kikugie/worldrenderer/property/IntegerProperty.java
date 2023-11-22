package dev.kikugie.worldrenderer.property;

import com.google.common.base.Preconditions;
import net.minecraft.util.math.MathHelper;

public class IntegerProperty extends Property<Integer> {
    private final int min;
    private final int max;
    private final int span;

    private boolean allowRollover = false;

    public IntegerProperty(int defaultValue, int min, int max) {
        super(defaultValue);
        Preconditions.checkArgument(min <= max);

        this.min = min;
        this.max = max;
        this.span = max - min;
    }

    public IntegerProperty withRollover() {
        this.allowRollover = true;
        return this;
    }

    public void modify(int by) {
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
        return (this.value - this.min) / (double) this.span;
    }

    public void setProgress(double progress) {
        this.value = (int) Math.round(this.min + progress * this.span);
        this.invokeListeners();
    }

    public int getMax() {
        return this.max;
    }

    public int getMin() {
        return this.min;
    }
}