package com.cobblestoner.recipe;

import java.util.Optional;

import com.cobblestoner.CoatingManager;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// A water bucket plus exactly one coated sword (anywhere in the grid, nothing else
// present) washes every coating off, returning the sword to its plain state. The
// bucket empties out on its own via WATER_BUCKET's vanilla crafting remainder (see
// Items#WATER_BUCKET) - CraftingRecipe's default getRemainingItems already handles
// that, so no override is needed here (contrast VialFillingRecipe, whose potion item
// has no such remainder set by vanilla).
public class SwordCleaningRecipe extends CustomRecipe {
    public static final SwordCleaningRecipe INSTANCE = new SwordCleaningRecipe();
    public static final MapCodec<SwordCleaningRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, SwordCleaningRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<SwordCleaningRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<SwordCleaningRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SwordCleaningRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    public SwordCleaningRecipe() {
        super(CraftingBookCategory.MISC);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findSword(input).isPresent();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return findSword(input).map(sword -> {
            ItemStack result = sword.copy();
            result.setCount(1);
            CoatingManager.clear(result);
            return result;
        }).orElse(ItemStack.EMPTY);
    }

    private Optional<ItemStack> findSword(CraftingInput input) {
        ItemStack sword = ItemStack.EMPTY;
        boolean sawWaterBucket = false;

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;

            if (stack.is(ItemTags.SWORDS)) {
                if (!sword.isEmpty() || CoatingManager.get(stack).isEmpty()) return Optional.empty();
                sword = stack;
            } else if (stack.is(Items.WATER_BUCKET)) {
                if (sawWaterBucket) return Optional.empty();
                sawWaterBucket = true;
            } else {
                return Optional.empty(); // unrelated item in the grid
            }
        }

        return sword.isEmpty() || !sawWaterBucket ? Optional.empty() : Optional.of(sword);
    }

    @Override
    public RecipeSerializer<SwordCleaningRecipe> getSerializer() {
        return SERIALIZER;
    }
}
