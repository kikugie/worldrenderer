package dev.kikugie.testmod;

import dev.kikugie.worldrenderer.render.Renderable;
import dev.kikugie.worldrenderer.render.RenderableDispatcher;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class RenderScreen extends Screen {
    private final Renderable renderable;

    public RenderScreen(Text title, Renderable renderable) {
        super(title);
        this.renderable = renderable;
    }

    @Override
    public void render(
            /*?>=1.20 {?*//*
            net.minecraft.client.gui.DrawContext context,
            *//*} else {*/
            net.minecraft.client.util.math.MatrixStack context,
            /*?}?*/
            int mouseX,
            int mouseY,
            float delta) {
        super.render(context, mouseX, mouseY, delta);
        float ratio = width / (float) height;

        RenderableDispatcher.draw(renderable, ratio, client.getTickDelta(), $ -> {
        });
    }
}