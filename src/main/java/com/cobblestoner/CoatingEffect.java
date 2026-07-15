package com.cobblestoner;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffect;

// One effect within a POTION-kind CoatingData - the amplifier a single hit applies for
// that effect, at the coating's shared per-hit duration (CoatingData#ticksPerHit).
// A potion with several effects (e.g. Turtle Master) becomes several CoatingEffects.
public record CoatingEffect(Holder<MobEffect> effect, int amplifier) {
    public static final Codec<CoatingEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MobEffect.CODEC.fieldOf("effect").forGetter(CoatingEffect::effect),
            Codec.INT.fieldOf("amplifier").forGetter(CoatingEffect::amplifier)
    ).apply(instance, CoatingEffect::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoatingEffect> STREAM_CODEC = StreamCodec.composite(
            MobEffect.STREAM_CODEC, CoatingEffect::effect,
            ByteBufCodecs.VAR_INT, CoatingEffect::amplifier,
            CoatingEffect::new
    );
}
