package com.cobblestoner.mixin;

import com.cobblestoner.CoatingManager;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Applies a sword's coating to its primary target here - Player#causeExtraKnockback -
// instead of at the very start of the swing (e.g. Fabric API's AttackEntityCallback,
// which used to be used here and fires before damage/knockback are resolved at all).
// Two reasons: this only runs once the attack actually connected (causeExtraKnockback
// is only called from inside attack()'s `if (wasHurt)` branch), and it runs *after*
// vanilla's own knockback call on the target. The latter matters specifically for
// WIND_CHARGE: LivingEntity#knockback *replaces* (not adds to) the target's vertical
// velocity while it's on the ground (capped at 0.4), so a wind-charge push applied
// earlier in the swing used to get silently overwritten back down to vanilla's own
// small on-ground knockback - it only ever "worked" once the target had already been
// bounced into the air by a previous hit, taking onGround() out of the equation.
// Applying our push after causeExtraKnockback has already run leaves nothing
// downstream to clobber it.
@Mixin(Player.class)
public class TippedSwordsAttackMixin {
    @Inject(method = "causeExtraKnockback", at = @At("TAIL"))
    private void tippedSwords$onCauseExtraKnockback(Entity entity, float knockbackAmount, Vec3 oldMovement, CallbackInfo info) {
        if (!(entity instanceof LivingEntity target)) return;

        Player attacker = (Player) (Object) this;
        ItemStack stack = attacker.getItemInHand(InteractionHand.MAIN_HAND);
        if (!stack.is(ItemTags.SWORDS)) return;

        CoatingManager.hit(stack, target);
    }
}
