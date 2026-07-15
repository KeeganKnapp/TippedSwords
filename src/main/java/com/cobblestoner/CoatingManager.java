package com.cobblestoner;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.cobblestoner.Models.CoatingType;
import com.cobblestoner.TippedSwords;
import com.cobblestoner.items.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;

public class CoatingManager {
    // Duration (in ticks) an applied potion effect keeps per hit, and the divisor used to
    // convert a source potion's duration into a hit count. Kept as one constant so
    // hits * TICKS_PER_HIT stays close to the original potion's total duration.
    // Static across every effect and level - only the hit *count* varies with the
    // source potion's duration/level, never this per-hit window.
    private static final int TICKS_PER_HIT = 110;

    // Potions with an instantaneous effect (harming, healing) have no meaningful
    // duration, so they get a fixed hit count instead of duration/TICKS_PER_HIT.
    // Each hit only applies 1/INSTANT_HITS of the effect's magnitude (see
    // applyPotionEffect), so a fully-used vial deals the same total damage/healing as
    // one dose of the source potion, not INSTANT_HITS doses of it. Wind Charge and
    // Fire Charge coatings are likewise one-shot and share this hit count, but aren't
    // magnitude-scaled since they're not effect-magnitude based.
    private static final int INSTANT_HITS = 8;

    // Fraction of an instantaneous potion effect's full-strength magnitude applied per
    // hit, so INSTANT_HITS hits together add up to one full dose (see applyPotionEffect).
    private static final double INSTANT_HIT_SCALE = 1.0 / INSTANT_HITS;

    // Hard cap on the hit count derived from a single source potion/effect (one vial's
    // worth), so a long-duration brew (e.g. Slowness IV) doesn't produce absurd hit
    // counts (~46+ hits observed pre-cap). When the raw duration/TICKS_PER_HIT count
    // would exceed this, the hits are capped here and ticksPerHit is stretched instead
    // so hits * ticksPerHit still totals close to the source duration. This is a
    // per-vial cap only - merge() can still stack up to MAX_VIALS_PER_COATING vials of
    // the same coating, so a fully stacked sword can exceed this by design.
    private static final int MAX_HITS_PER_VIAL = 8;

    // Upward velocity applied on a Wind Charge hit, tuned to read as "launched into the
    // air" (well above a normal jump/knockback) without being a full explosion knockup.
    private static final double WIND_LAUNCH_VELOCITY = 1.35;

    // Matches the ignite duration vanilla gives a Flame-enchanted arrow's target.
    private static final float FIRE_IGNITE_SECONDS = 5.0F;

    // Empty vials stack like any other junk item; a filled vial is a distinct
    // dose, so it's capped low like vanilla potions.
    private static final int FILLED_VIAL_STACK_SIZE = 16;

    // A single sword-tipping craft accepts at most this many vials (see
    // SwordTippingRecipe). It's also the hard cap on accumulated hits per matching
    // coating: since every vial of a given coating always carries the same per-vial
    // hit count, capping combined hits at per-vial-hits * this constant is equivalent
    // to "3 vials' worth", and merge() enforces it so repeated re-tipping can't stack
    // past it.
    public static final int MAX_VIALS_PER_COATING = 3;

    // A sword can never carry more than this many *distinct* coatings at once
    // (different type, level, or effect combo), regardless of how many separate
    // re-tipping crafts it took to get there - enforced by SwordTippingRecipe
    // rejecting any craft whose merged result would exceed it.
    public static final int MAX_DISTINCT_COATINGS = 3;

    private static final float TICKRATE = 20.0F;

    private static final String HITS_TRANSLATION_KEY = TippedSwords.MOD_ID + ".tooltip.hits";
    private static final String HITS_DURATION_TRANSLATION_KEY = TippedSwords.MOD_ID + ".tooltip.hits_duration";
    private static final String VIAL_OF_TRANSLATION_KEY = TippedSwords.MOD_ID + ".vial.of";
    private static final String WIND_CHARGE_NAME_KEY = TippedSwords.MOD_ID + ".effect.wind_charge";
    private static final String FIRE_CHARGE_NAME_KEY = TippedSwords.MOD_ID + ".effect.fire_charge";

    public static List<CoatingData> getAll(ItemStack stack) {
        return stack.getOrDefault(ModComponents.COATINGS, List.of());
    }

    // Coatings are only ever visible to real players through Polymer-disguised
    // vanilla items, so the tooltip can't rely on client-side code (e.g. a mixin
    // into Item.appendHoverText) - it has to be data the vanilla client already
    // knows how to render on its own. Lore is exactly that: a networked component
    // baked in here and displayed by unmodified clients with no mod-specific code.
    //
    // POTION_CONTENTS is set alongside it purely so a resource pack can tint the
    // sword/vial model: vanilla's "minecraft:potion" item-model tint source reads
    // this component and already knows how to blend multiple effects' colors
    // (weighted by amplifier), which is exactly the coloring behavior we want and
    // requires no mod-specific rendering code on the client. But PotionContents
    // also implements TooltipProvider, so vanilla's generic tooltip renderer adds
    // its own "Slowness II (0:05)" line for *any* item carrying the component -
    // duplicating (with less info) the lore line built below. TOOLTIP_DISPLAY hides
    // that component from the tooltip so only the lore line (which includes hits)
    // is shown.
    public static void setAll(ItemStack stack, List<CoatingData> coatings) {
        boolean isVial = stack.is(ModItems.VIAL);

        if (coatings.isEmpty()) {
            stack.remove(ModComponents.COATINGS);
            stack.remove(ModComponents.HAS_FIRE_CHARGE);
            stack.remove(ModComponents.HAS_WIND_CHARGE);
            stack.remove(DataComponents.LORE);
            stack.remove(DataComponents.POTION_CONTENTS);
            stack.remove(DataComponents.TOOLTIP_DISPLAY);
            if (isVial) {
                stack.remove(DataComponents.ITEM_NAME);
                stack.remove(DataComponents.MAX_STACK_SIZE);
            }
        } else {
            stack.set(ModComponents.COATINGS, coatings);
            stack.set(DataComponents.LORE, new ItemLore(buildTooltipLines(coatings)));

            // Fire/Wind Charge coatings get their own dedicated texture layer (see
            // fire_charge_overlay/wind_charge_overlay item models) selected by these
            // presence-only markers, independent of whether the sword also carries a
            // potion-tinted coating.
            setOrRemove(stack, ModComponents.HAS_FIRE_CHARGE, hasType(coatings, CoatingType.FIRE_CHARGE));
            setOrRemove(stack, ModComponents.HAS_WIND_CHARGE, hasType(coatings, CoatingType.WIND_CHARGE));

            // Only set POTION_CONTENTS (and hide it from the tooltip) when there's an
            // actual potion effect to tint with - otherwise (a sword coated purely with
            // Fire/Wind Charge) the "minecraft:potion" tint source would fall back to
            // its untinted default and paint the shared coating_overlay a flat white
            // underneath the new fire/wind texture layer.
            if (hasPotionEffects(coatings)) {
                stack.set(DataComponents.POTION_CONTENTS, buildPotionContentsForTint(coatings));
                stack.set(DataComponents.TOOLTIP_DISPLAY,
                        stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT)
                                .withHidden(DataComponents.POTION_CONTENTS, true));
            } else {
                stack.remove(DataComponents.POTION_CONTENTS);
                stack.remove(DataComponents.TOOLTIP_DISPLAY);
            }

            if (isVial) {
                stack.set(DataComponents.ITEM_NAME, vialName(coatings.get(0)));
                stack.set(DataComponents.MAX_STACK_SIZE, FILLED_VIAL_STACK_SIZE);
            }
        }
    }

    private static void setOrRemove(ItemStack stack, DataComponentType<Unit> component, boolean present) {
        if (present) {
            stack.set(component, Unit.INSTANCE);
        } else {
            stack.remove(component);
        }
    }

    private static boolean hasType(List<CoatingData> coatings, CoatingType type) {
        return coatings.stream().anyMatch(coating -> coating.type() == type);
    }

    private static boolean hasPotionEffects(List<CoatingData> coatings) {
        return coatings.stream().anyMatch(coating -> coating.type() == CoatingType.POTION && !coating.effects().isEmpty());
    }

    // Vials only ever hold real potions now (see fromPotion / VialFillingRecipe) -
    // Wind Charge/Fire Charge/Wither Rose go straight onto the sword instead (see
    // fromDirectIngredient), so those CoatingTypes never reach a vial in practice.
    private static Component vialName(CoatingData coating) {
        return switch (coating.type()) {
            case POTION -> potionVialName(coating.effects());
            case WIND_CHARGE, FIRE_CHARGE, NONE -> Component.translatable("item.tipped_sword.vial");
        };
    }

    private static Component potionVialName(List<CoatingEffect> effects) {
        if (effects.isEmpty()) return Component.translatable("item.tipped_sword.vial");

        MutableComponent joined = effects.get(0).effect().value().getDisplayName().copy();
        for (int i = 1; i < effects.size(); i++) {
            joined.append(" & ").append(effects.get(i).effect().value().getDisplayName());
        }
        return Component.translatable(VIAL_OF_TRANSLATION_KEY, joined);
    }

    private static PotionContents buildPotionContentsForTint(List<CoatingData> coatings) {
        List<MobEffectInstance> effects = new ArrayList<>();
        for (CoatingData coating : coatings) {
            for (CoatingEffect effect : coating.effects()) {
                effects.add(new MobEffectInstance(effect.effect(), coating.ticksPerHit(), effect.amplifier()));
            }
        }
        return new PotionContents(Optional.empty(), Optional.empty(), effects, Optional.empty());
    }

    public static Optional<CoatingData> get(ItemStack stack) {
        List<CoatingData> coatings = getAll(stack);
        return coatings.isEmpty() ? Optional.empty() : Optional.of(coatings.get(0));
    }

    public static void set(ItemStack stack, CoatingData coating) {
        setAll(stack, List.of(coating));
    }

    public static void clear(ItemStack stack) {
        setAll(stack, List.of());
    }

    public static void hit(ItemStack stack, LivingEntity target) {
        List<CoatingData> coatings = getAll(stack);
        if (coatings.isEmpty()) return;

        List<CoatingData> remaining = new ArrayList<>();
        for (CoatingData coating : coatings) {
            TippedSwords.LOGGER.info("hit entity with coating {}, hits {}", coating.type(), coating.hits());

            applyCoating(target, coating);

            int newHits = coating.hits() - 1;
            if (newHits > 0) {
                remaining.add(new CoatingData(coating.type(), coating.effects(), newHits, coating.ticksPerHit()));
            }
        }

        setAll(stack, remaining);
    }

    // Applies a single coating's worth of effect to a target at full strength. Shared
    // by the melee hit path (above) and thrown vials landing (ThrownVialEntity), so
    // both behave identically for a given CoatingData.
    public static void applyCoating(LivingEntity target, CoatingData coating) {
        applyCoating(target, coating, 1.0);
    }

    // Applies every coating currently on `stack` to `target` at `scale` strength
    // without touching the stack's stored hits - used for vanilla's sweep-attack side
    // hits (see TippedSwordsSweepMixin), which are a bonus of the same swing that
    // already spent a hit on the primary target via hit(), not a separate use of the
    // coating.
    public static void sweepHit(ItemStack stack, LivingEntity target, double scale) {
        for (CoatingData coating : getAll(stack)) {
            applyCoating(target, coating, scale);
        }
    }

    // scale < 1.0 is how sweep-attack side hits (see sweepHit) apply a fraction of a
    // direct hit's effect instead of duplicating it in full against every nearby mob.
    private static void applyCoating(LivingEntity target, CoatingData coating, double scale) {
        switch (coating.type()) {
            case POTION -> {
                for (CoatingEffect effect : coating.effects()) {
                    applyPotionEffect(target, effect, coating.ticksPerHit(), scale);
                }
            }
            case WIND_CHARGE -> {
                target.push(0.0, WIND_LAUNCH_VELOCITY * scale, 0.0);
                // push() only syncs velocity to other players tracking the target, not to
                // the target's own client (players are client-authoritative for movement).
                // hurtMarked additionally syncs to the target itself - see ServerEntity's
                // handling of Entity#hurtMarked vs Entity#needsSync.
                target.hurtMarked = true;

                // Same sound + particle a real Wind Charge entity uses on impact (see
                // WindCharge#explode) - played/spawned directly rather than via
                // Level#explode, since we only want the cosmetic burst here, not an actual
                // explosion (block interaction, its own separate knockback, etc).
                if (target.level() instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.WIND_CHARGE_BURST, target.getSoundSource(), 1.0F, 1.0F);
                    serverLevel.sendParticles(ParticleTypes.GUST_EMITTER_SMALL,
                            target.getX(), target.getY(0.5), target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            case FIRE_CHARGE -> target.igniteForSeconds((float) (FIRE_IGNITE_SECONDS * scale));
            case NONE -> {
            }
        }
    }

    // Instantaneous effects (e.g. Harming/Healing) apply their full vanilla magnitude
    // in one shot - addEffect()'ing one at full amplifier on every remaining hit would
    // deal INSTANT_HITS doses' worth from a single vial instead of one dose split fairly.
    // applyInstantenousEffect's `scale` param is the same mechanism vanilla splash
    // potions use for distance falloff (see ThrownSplashPotion#onHit) - passing
    // INSTANT_HIT_SCALE here applies a fair fraction of the dose per hit instead, so
    // INSTANT_HITS hits total to one dose, matching how duration-based effects are
    // already fairly split over time via ticksPerHit. The extra `scale` factor (1.0
    // for a direct hit) further reduces a sweep-attack side hit's share - see
    // applyCoating(LivingEntity, CoatingData, double).
    private static void applyPotionEffect(LivingEntity target, CoatingEffect effect, int ticksPerHit, double scale) {
        MobEffect mobEffect = effect.effect().value();
        if (mobEffect.isInstantenous()) {
            if (target.level() instanceof ServerLevel serverLevel) {
                mobEffect.applyInstantenousEffect(serverLevel, null, null, target, effect.amplifier(), INSTANT_HIT_SCALE * scale);
            }
        } else {
            int duration = Math.max(1, (int) Math.round(ticksPerHit * scale));
            target.addEffect(new MobEffectInstance(effect.effect(), duration, effect.amplifier()));
        }
    }

    // Builds the "Slowness II (16 hits, 0:05)" style tooltip lines for a set of
    // coatings (swords and vials alike - both go through setAll), styled the same
    // way a potion's own effect lines are (color by beneficial/harmful/neutral
    // category, roman-numeral level). The duration shown is the per-hit duration
    // (ticksPerHit) applied to the target on each hit; instant effects have no
    // meaningful duration, so only their hit count is shown.
    private static List<Component> buildTooltipLines(List<CoatingData> coatings) {
        List<Component> lines = new ArrayList<>();
        for (CoatingData coating : coatings) {
            switch (coating.type()) {
                case POTION -> lines.addAll(potionTooltipLines(coating));
                case WIND_CHARGE -> lines.add(instantTooltipLine(WIND_CHARGE_NAME_KEY, coating));
                case FIRE_CHARGE -> lines.add(instantTooltipLine(FIRE_CHARGE_NAME_KEY, coating));
                case NONE -> {
                }
            }
        }
        return lines;
    }

    private static List<Component> potionTooltipLines(CoatingData coating) {
        boolean instant = isInstant(coating);
        List<Component> lines = new ArrayList<>();
        for (CoatingEffect effect : coating.effects()) {
            MutableComponent nameAndLevel = PotionContents.getPotionDescription(effect.effect(), effect.amplifier());
            Component hitsInfo = instant
                    ? Component.translatable(HITS_TRANSLATION_KEY, coating.hits())
                    : Component.translatable(HITS_DURATION_TRANSLATION_KEY, coating.hits(),
                            StringUtil.formatTickDuration(coating.ticksPerHit(), TICKRATE));
            lines.add(Component.translatable("potion.withDuration", nameAndLevel, hitsInfo)
                    .withStyle(effect.effect().value().getCategory().getTooltipFormatting())
                    .withStyle(style -> style.withItalic(false)));
        }
        return lines;
    }

    private static Component instantTooltipLine(String nameKey, CoatingData coating) {
        Component hitsInfo = Component.translatable(HITS_TRANSLATION_KEY, coating.hits());
        return Component.translatable("potion.withDuration", Component.translatable(nameKey), hitsInfo)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withItalic(false));
    }

    // Wind Charge, Fire Charge, and Wither Rose skip the vial step entirely and are
    // crafted straight onto the sword (see SwordTippingRecipe) - unlike potions they
    // have no vial-tintable PotionContents of their own, so a vial of them never
    // rendered with any real color. Only actual potions still go through vials
    // (see fromPotion / VialFillingRecipe).
    public static Optional<CoatingData> fromDirectIngredient(ItemStack stack) {
        if (stack.is(Items.WIND_CHARGE)) return Optional.of(fromWindCharge());
        if (stack.is(Items.FIRE_CHARGE)) return Optional.of(fromFireCharge());
        if (stack.is(Items.WITHER_ROSE)) return Optional.of(fromWitherRose());
        return Optional.empty();
    }

    // Mirrors how tipped arrows pick up a potion's effects: every MobEffectInstance
    // the potion carries (Potion#getEffects), not just a fixed set we recognize by
    // name. This is what lets any brewable potion - vanilla or added by a data pack -
    // work as a sword coating.
    public static Optional<CoatingData> fromPotion(ItemStack potionStack) {
        PotionContents contents = potionStack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return Optional.empty();

        Optional<Holder<Potion>> potionHolder = contents.potion();
        if (potionHolder.isEmpty()) return Optional.empty();

        Potion potion = potionHolder.get().value();
        List<MobEffectInstance> potionEffects = potion.getEffects();
        if (potionEffects.isEmpty()) return Optional.empty(); // water/awkward/mundane/thick - nothing to coat with

        List<CoatingEffect> effects = new ArrayList<>();
        int maxDuration = 0;
        for (MobEffectInstance instance : potionEffects) {
            effects.add(new CoatingEffect(instance.getEffect(), instance.getAmplifier()));
            maxDuration = Math.max(maxDuration, instance.getDuration());
        }

        if (potion.hasInstantEffects()) {
            return Optional.of(new CoatingData(CoatingType.POTION, effects, INSTANT_HITS, 1));
        }

        int[] hitsAndTicksPerHit = hitsFromDuration(maxDuration);
        return Optional.of(new CoatingData(CoatingType.POTION, effects, hitsAndTicksPerHit[0], hitsAndTicksPerHit[1]));
    }

    // Converts a source duration (ticks) into a (hits, ticksPerHit) pair, capping hits
    // at MAX_HITS_PER_VIAL and stretching ticksPerHit to compensate so the total
    // applied duration (hits * ticksPerHit) still tracks the source duration.
    private static int[] hitsFromDuration(int sourceDuration) {
        int hits = Math.max(1, sourceDuration / TICKS_PER_HIT);
        int ticksPerHit = TICKS_PER_HIT;
        if (hits > MAX_HITS_PER_VIAL) {
            hits = MAX_HITS_PER_VIAL;
            ticksPerHit = Math.max(1, sourceDuration / hits);
        }
        return new int[] { hits, ticksPerHit };
    }

    public static CoatingData fromWindCharge() {
        return new CoatingData(CoatingType.WIND_CHARGE, List.of(), INSTANT_HITS, 1);
    }

    public static CoatingData fromFireCharge() {
        return new CoatingData(CoatingType.FIRE_CHARGE, List.of(), INSTANT_HITS, 1);
    }

    // Wither Rose is crafted directly onto the sword like Wind/Fire Charge (see
    // fromDirectIngredient), so it gets the same fixed INSTANT_HITS hit count; the
    // Wither effect itself still applies for the standard TICKS_PER_HIT window per hit.
    public static CoatingData fromWitherRose() {
        return new CoatingData(CoatingType.POTION, List.of(new CoatingEffect(MobEffects.WITHER, 0)),
                INSTANT_HITS, TICKS_PER_HIT);
    }

    // Merges an existing set of coatings (e.g. already on a sword) with a new batch
    // (e.g. from vials being crafted in). Coatings that are identical in every way
    // except hit count (same type, same effects, same per-hit duration) have their
    // hits summed, capped at that vial's per-vial hits * MAX_VIALS_PER_COATING
    // ("3 vials' worth") so repeated re-tipping can't stack past the cap - a harming
    // sword tops out at 4 hits/vial * 3 = 12, regardless of how many separate craft
    // operations it took. Anything else (a different potion, a different level, a
    // different coating kind) simply becomes its own entry in the list - a sword can
    // carry several distinct coatings side by side, each applied on every hit.
    public static List<CoatingData> merge(List<CoatingData> base, List<CoatingData> additions) {
        Map<CoatingKey, CoatingData> merged = new LinkedHashMap<>();

        for (CoatingData coating : base) {
            merged.put(CoatingKey.of(coating), coating);
        }

        for (CoatingData coating : additions) {
            CoatingKey key = CoatingKey.of(coating);
            CoatingData existing = merged.get(key);
            if (existing == null) {
                merged.put(key, coating);
            } else {
                int cap = coating.hits() * MAX_VIALS_PER_COATING;
                int hits = Math.min(existing.hits() + coating.hits(), cap);
                merged.put(key, new CoatingData(coating.type(), coating.effects(), hits, coating.ticksPerHit()));
            }
        }

        return new ArrayList<>(merged.values());
    }

    private record CoatingKey(CoatingType type, List<CoatingEffect> effects, int ticksPerHit) {
        static CoatingKey of(CoatingData coating) {
            return new CoatingKey(coating.type(), coating.effects(), coating.ticksPerHit());
        }
    }

    private static boolean isInstant(CoatingData coating) {
        return coating.type() == CoatingType.POTION
                && coating.effects().stream().anyMatch(effect -> effect.effect().value().isInstantenous());
    }
}
