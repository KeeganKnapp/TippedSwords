package com.cobblestoner;

import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.Unit;

public class ModComponents {
    public static final DataComponentType<List<CoatingData>> COATINGS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            TippedSwords.id("coatings"),
            DataComponentType.<List<CoatingData>>builder()
                    .persistent(CoatingData.CODEC.listOf())
                    .networkSynchronized(CoatingData.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build()
    );

    // Presence-only markers read by the item models (see coating_overlay-adjacent
    // fire_charge_overlay/wind_charge_overlay models) to pick the custom fire/wind
    // texture layer independently of whatever potion-tint the sword also carries.
    public static final DataComponentType<Unit> HAS_FIRE_CHARGE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            TippedSwords.id("has_fire_charge"),
            DataComponentType.<Unit>builder()
                    .persistent(Unit.CODEC)
                    .networkSynchronized(Unit.STREAM_CODEC)
                    .build()
    );

    public static final DataComponentType<Unit> HAS_WIND_CHARGE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            TippedSwords.id("has_wind_charge"),
            DataComponentType.<Unit>builder()
                    .persistent(Unit.CODEC)
                    .networkSynchronized(Unit.STREAM_CODEC)
                    .build()
    );

    public static void register() {
        // components registered via static fields above
    }
}
