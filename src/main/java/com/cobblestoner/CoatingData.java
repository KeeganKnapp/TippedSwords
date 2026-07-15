package com.cobblestoner;

import java.util.List;

import com.cobblestoner.Models.CoatingType;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// effects is only populated for CoatingType.POTION (empty for WIND_CHARGE/FIRE_CHARGE,
// which have no backing MobEffect - see CoatingManager#applyCoating). ticksPerHit is the
// duration applied to every entry in effects on each hit; hits is the number of swings
// (or splash uses) remaining before the coating is spent.
public record CoatingData(CoatingType type, List<CoatingEffect> effects, int hits, int ticksPerHit) {
    public static final Codec<CoatingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CoatingType.CODEC.fieldOf("type").forGetter(CoatingData::type),
            CoatingEffect.CODEC.listOf().fieldOf("effects").forGetter(CoatingData::effects),
            Codec.INT.fieldOf("hits").forGetter(CoatingData::hits),
            Codec.INT.fieldOf("ticksPerHit").forGetter(CoatingData::ticksPerHit)
    ).apply(instance, CoatingData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoatingData> STREAM_CODEC = StreamCodec.composite(
            CoatingType.STREAM_CODEC, CoatingData::type,
            CoatingEffect.STREAM_CODEC.apply(ByteBufCodecs.list()), CoatingData::effects,
            ByteBufCodecs.VAR_INT, CoatingData::hits,
            ByteBufCodecs.VAR_INT, CoatingData::ticksPerHit,
            CoatingData::new
    );
}
