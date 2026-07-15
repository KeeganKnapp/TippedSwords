package com.cobblestoner.recipe;

import java.util.Optional;

import com.cobblestoner.CoatingData;
import com.cobblestoner.CoatingManager;
import com.cobblestoner.items.ModItems;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// A potion surrounded by 8 empty vials (full 3x3 grid) yields 8 vials coated per
// CoatingManager.fromPotion. Wind Charge, Fire Charge, and Wither Rose don't fill
// vials - they're crafted directly onto a sword instead (see SwordTippingRecipe).
public class VialFillingRecipe extends CustomRecipe {
    public static final VialFillingRecipe INSTANCE = new VialFillingRecipe();
    public static final MapCodec<VialFillingRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, VialFillingRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<VialFillingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private static final Ingredient VIAL = Ingredient.of(ModItems.VIAL);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3 || input.ingredientCount() != 9) {
            return false;
        }

        ItemStack center = input.getItem(1, 1);
        if (CoatingManager.fromPotion(center).isEmpty()) {
            return false;
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (x == 1 && y == 1) continue;

                ItemStack stack = input.getItem(x, y);
                if (!VIAL.test(stack) || CoatingManager.get(stack).isPresent()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Optional<CoatingData> coating = CoatingManager.fromPotion(input.getItem(1, 1));
        if (coating.isEmpty()) return ItemStack.EMPTY;

        ItemStack result = new ItemStack(ModItems.VIAL, 8);
        CoatingManager.set(result, coating.get());
        return result;
    }

    // Items#POTION has no vanilla crafting remainder (nothing in vanilla ever consumes
    // a potion in a crafting grid), so the default remainder logic would just delete
    // the bottle. Leave a glass bottle in the center slot to match how every other
    // bottle-emptying interaction in the game behaves.
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = CraftingRecipe.defaultCraftingReminder(input);
        remaining.set(1 + 1 * input.width(), new ItemStack(Items.GLASS_BOTTLE));
        return remaining;
    }

    @Override
    public RecipeSerializer<VialFillingRecipe> getSerializer() {
        return SERIALIZER;
    }
}
