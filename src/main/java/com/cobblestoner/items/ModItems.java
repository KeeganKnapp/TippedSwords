package com.cobblestoner.items;

import com.cobblestoner.TippedSwords;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final Item VIAL = Registry.register(
            BuiltInRegistries.ITEM,
            TippedSwords.id("vial"),
	    new VialItem(new Item.Properties()
		    .setId(ResourceKey.create(Registries.ITEM, TippedSwords.id("vial")))
		    .stacksTo(64))
    );

    public static void register() {
        // items registered via static fields above
    }
}
