package com.cobblestoner.items;

import com.cobblestoner.entity.ThrownVialEntity;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class VialItem extends Item implements PolymerItem {
    // Prevents spamming out a rapid stream of vials, mirroring the throttle every
    // other vanilla throwable (snowballs excepted) effectively has via wind-up/use time.
    private static final int THROW_COOLDOWN_TICKS = 10;

    public VialItem(Properties properties) {
        super(properties);
    }

    // Polymer: show as snowball on the client
    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.SNOWBALL;
    }

    // Throw the vial like a snowball/potion. On landing, ThrownVialEntity splashes
    // whatever coatings this vial carries (if any - an empty vial just breaks).
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
        );

        if (level instanceof ServerLevel serverLevel) {
            Projectile.spawnProjectileFromRotation(ThrownVialEntity::new, serverLevel, stack, player, 0.0F, 1.5F, 1.0F);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(stack, THROW_COOLDOWN_TICKS);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
