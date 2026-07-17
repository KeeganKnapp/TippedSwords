package com.cobblestoner.entity;

import com.cobblestoner.TippedSwords;

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;

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
        // Unlike PolymerItem, implementing PolymerEntity on ThrownVialEntity doesn't by
        // itself exclude the entity TYPE from Fabric's registry sync - vanilla clients
        // still get offered "tipped_sword:thrown_vial" and reject it. This opts it out.
        PolymerEntityUtils.registerType(THROWN_VIAL);
    }
}
