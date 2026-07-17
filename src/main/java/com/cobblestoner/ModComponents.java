package com.cobblestoner;

import java.util.List;

import eu.pb4.polymer.core.api.other.PolymerComponent;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public class ModComponents {
    public static final DataComponentType<List<CoatingData>> COATINGS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            TippedSwords.id("coatings"),
            DataComponentType.<List<CoatingData>>builder()
                    .persistent(CoatingData.CODEC.listOf())
                    .networkSynchronized(CoatingData.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build()
    );

    public static void register() {
        // Custom DataComponentType entries are otherwise offered to vanilla clients as
        // unknown registry entries and rejected during registry sync. This opts them out.
        PolymerComponent.registerDataComponent(COATINGS);
    }
}
