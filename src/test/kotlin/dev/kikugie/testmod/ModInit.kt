package dev.kikugie.testmod

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

object ModInit : ModInitializer {
    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register(RenderCommand::register)
    }
}