package dev.kikugie.testmod;

import dev.kikugie.testmod.command.RenderCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class ModInit implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> RenderCommand.register(dispatcher)));
    }
}