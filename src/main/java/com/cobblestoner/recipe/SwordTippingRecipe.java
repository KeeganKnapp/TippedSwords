package com.cobblestoner.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.cobblestoner.CoatingData;
import com.cobblestoner.CoatingManager;
import com.cobblestoner.Models.CoatingType;
import com.cobblestoner.items.ModItems;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// One sword plus 1-3 filled vials and/or raw Wind Charge/Fire Charge/Wither Rose
// items (anywhere in the grid, nothing else present) tips the sword with their
// coatings, summing hits for matching type+level. Wind Charge/Fire Charge/Wither
// Rose skip the vial step entirely (see CoatingManager#fromDirectIngredient) -
// each one contributes a fixed hit count directly, same as a vial slot would.
public class SwordTippingRecipe extends CustomRecipe {
    public static final SwordTippingRecipe INSTANCE = new SwordTippingRecipe();
    public static final MapCodec<SwordTippingRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, SwordTippingRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<SwordTippingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private static final int MIN_VIALS = 1;
    private static final int MAX_VIALS = CoatingManager.MAX_VIALS_PER_COATING;

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findMerge(input).isPresent();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Optional<Merge> merge = findMerge(input);
        if (merge.isEmpty()) return ItemStack.EMPTY;

        ItemStack result = merge.get().sword().copy();
        result.setCount(1);
        CoatingManager.setAll(result, merge.get().coatings());
        return result;
    }

    private Optional<Merge> findMerge(CraftingInput input) {
        ItemStack sword = ItemStack.EMPTY;
        List<CoatingData> vialCoatings = new ArrayList<>();

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;

            if (stack.is(ItemTags.SWORDS)) {
                if (!sword.isEmpty()) return Optional.empty(); // more than one sword
                sword = stack;
            } else if (stack.is(ModItems.VIAL) && CoatingManager.get(stack).isPresent()) {
                vialCoatings.add(CoatingManager.get(stack).get());
            } else {
                Optional<CoatingData> direct = CoatingManager.fromDirectIngredient(stack);
                if (direct.isEmpty()) return Optional.empty(); // unrelated item in the grid
                vialCoatings.add(direct.get());
            }
        }

        if (sword.isEmpty()) return Optional.empty();
        if (vialCoatings.size() < MIN_VIALS || vialCoatings.size() > MAX_VIALS) return Optional.empty();

        List<CoatingData> merged = CoatingManager.merge(CoatingManager.getAll(sword), vialCoatings);
        if (merged.size() > CoatingManager.MAX_DISTINCT_COATINGS) return Optional.empty();

        // Wind Charge (launches the target) and Fire Charge (ignites it) read as
        // opposed effects, so a sword can carry one or the other but never both.
        boolean hasWindCharge = merged.stream().anyMatch(coating -> coating.type() == CoatingType.WIND_CHARGE);
        boolean hasFireCharge = merged.stream().anyMatch(coating -> coating.type() == CoatingType.FIRE_CHARGE);
        if (hasWindCharge && hasFireCharge) return Optional.empty();

        return Optional.of(new Merge(sword, merged));
    }

    private record Merge(ItemStack sword, List<CoatingData> coatings) {}

    @Override
    public RecipeSerializer<SwordTippingRecipe> getSerializer() {
        return SERIALIZER;
    }
}
