package com.cobblestoner.Models;

import java.util.Arrays;
import java.util.Optional;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// The kind of thing a coating does when it lands a hit. POTION covers every vanilla
// or data-driven MobEffect (see CoatingManager#fromPotion) - the specific effect(s)
// carried by a POTION coating live in CoatingData#effects, not in this enum. The other
// two are effects with no backing MobEffect, so they're handled as their own kind.
public enum CoatingType {
    POTION("potion"),
    WIND_CHARGE("wind_charge"),
    FIRE_CHARGE("fire_charge"),
    NONE("");

    public static final Codec<CoatingType> CODEC = Codec.STRING.xmap(
            id -> CoatingType.fromId(id).orElse(CoatingType.NONE),
            CoatingType::id
    );

    public static final StreamCodec<ByteBuf, CoatingType> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            id -> CoatingType.fromId(id).orElse(CoatingType.NONE),
            CoatingType::id
    );

    private final String id;

    CoatingType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<CoatingType> fromId(String id) {
        return Arrays.stream(values())
                .filter(c -> c.id.equals(id))
                .findFirst();
    }
}
