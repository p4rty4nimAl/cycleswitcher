package com.p4.cycleswitcher;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class CycleSwitcher implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("cycleswitcher");

    @Override
    public void onInitializeClient() {

    }
}
