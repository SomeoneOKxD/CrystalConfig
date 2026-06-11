package dev.someoneok.crystalconfig;

import dev.someoneok.crystalconfig.autoconfig.MinecraftAutoConfig;
import net.fabricmc.api.ClientModInitializer;

public class CrystalConfig implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MinecraftAutoConfig.register();
    }
}
