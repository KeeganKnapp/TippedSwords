package com.cobblestoner.entity;

import java.util.List;

import com.cobblestoner.CoatingData;
import com.cobblestoner.CoatingManager;
import com.cobblestoner.items.ModItems;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import xyz.nucleoid.packettweaker.PacketContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

// A thrown vial: on impact it splashes every coating it's carrying onto every living
// entity within SPLASH_RANGE, applying each exactly as CoatingManager#hit would on a
// melee swing (same effect(s), same per-hit duration/level) - just to a radius of
// entities instead of a single target. Disguised as a snowball for real clients via
// Polymer, matching VialItem's own disguise so the two are visually consistent. The
// impact itself reuses vanilla's own splash-potion level event (glass-break sound +
// colored particles, see AbstractThrownPotion#onHit) rather than a bespoke effect, so
// it lands exactly like a splash potion breaking - colored by whatever PotionContents
// CoatingManager#setAll put on this vial for tinting.
public class ThrownVialEntity extends ThrowableItemProjectile implements PolymerEntity {
    private static final double SPLASH_RANGE = 4.0;
    private static final double SPLASH_RANGE_SQ = SPLASH_RANGE * SPLASH_RANGE;

    public ThrownVialEntity(EntityType<? extends ThrownVialEntity> type, Level level) {
        super(type, level);
    }

    public ThrownVialEntity(Level level, LivingEntity owner, ItemStack itemStack) {
        super(ModEntities.THROWN_VIAL, owner, level, itemStack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.VIAL;
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (level() instanceof ServerLevel level) {
            List<CoatingData> coatings = CoatingManager.getAll(getItem());
            if (!coatings.isEmpty()) {
                AABB splashArea = getBoundingBox().inflate(SPLASH_RANGE);
                for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, splashArea)) {
                    if (distanceToSqr(target) > SPLASH_RANGE_SQ) continue;
                    for (CoatingData coating : coatings) {
                        CoatingManager.applyCoating(target, coating);
                    }
                }
            }

            PotionContents potionContents = getItem().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            level.levelEvent(LevelEvent.PARTICLES_SPELL_POTION_SPLASH, blockPosition(), potionContents.getColor());
            discard();
        }
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) {
        return EntityType.SNOWBALL;
    }
}
