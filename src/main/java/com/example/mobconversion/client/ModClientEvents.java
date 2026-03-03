package com.example.mobconversion.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class ModClientEvents {

    public static void registerConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (client, parent) -> ClothConfigScreen.create(parent));
    }
}
