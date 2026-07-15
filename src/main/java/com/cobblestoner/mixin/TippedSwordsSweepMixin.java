package com.cobblestoner.mixin;

import com.cobblestoner.CoatingManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Vanilla's sweep attack (Player#doSweepAttack) is the base sword mechanic that hits
// every living entity near the primary target on a non-critical, non-sprinting,
// on-ground swing - independent of the Sweeping Edge enchantment (see
// Player#isSweepAttack). This mirrors that same target selection to give each swept
// mob a share of the sword's coating too, scaled down the same way vanilla scales
// sweep damage down relative to a direct hit (see sweepScale) - so a sweep isn't a
// free full-strength coating hit against every nearby mob.
@Mixin(Player.class)
public class TippedSwordsSweepMixin {
    @Inject(method = "doSweepAttack", at = @At("TAIL"))
    private void tippedSwords$onSweepAttack(Entity primaryTarget, float baseDamage, DamageSource damageSource, float attackStrengthScale, CallbackInfo info) {
        Player attacker = (Player) (Object) this;
        if (!(attacker.level() instanceof ServerLevel)) return;

        ItemStack stack = attacker.getItemInHand(InteractionHand.MAIN_HAND);
        if (!stack.is(ItemTags.SWORDS)) return;

        double scale = sweepScale(attacker, baseDamage);
        if (scale <= 0.0) return;

        for (LivingEntity nearby : attacker.level().getEntitiesOfClass(LivingEntity.class, primaryTarget.getBoundingBox().inflate(1.0, 0.25, 1.0))) {
            if (nearby == attacker || nearby == primaryTarget) continue;
            if (attacker.isAlliedTo(nearby)) continue;
            if (nearby instanceof ArmorStand armorStand && armorStand.isMarker()) continue;
            if (attacker.distanceToSqr(nearby) >= 9.0) continue;

            CoatingManager.sweepHit(stack, nearby, scale);
        }
    }

    // Mirrors Player#doSweepAttack's own damage formula (1 + SWEEPING_DAMAGE_RATIO *
    // baseDamage, i.e. a flat 1 damage without Sweeping Edge) expressed as a fraction
    // of a direct hit's base weapon damage, then clamped to 1.0 in case a
    // heavily-enchanted sweep would otherwise exceed a full-strength dose.
    private static double sweepScale(Player attacker, float baseDamage) {
        if (baseDamage <= 0.0F) return 0.0;
        float sweepDamage = 1.0F + (float) attacker.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * baseDamage;
        return Math.min(1.0, sweepDamage / baseDamage);
    }
}
