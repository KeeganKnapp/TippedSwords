package com.cobblestoner.entity;

import com.cobblestoner.TippedSwords;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final EntityType<ThrownVialEntity> THROWN_VIAL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            TippedSwords.id("thrown_vial"),
            EntityType.Builder.<ThrownVialEntity>of(ThrownVialEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, TippedSwords.id("thrown_vial")))
    );

    public static void register() {
        // entity type registered via static field above
    }
}
