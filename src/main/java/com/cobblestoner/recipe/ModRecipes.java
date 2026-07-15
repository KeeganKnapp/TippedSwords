package com.cobblestoner.recipe;

import com.cobblestoner.TippedSwords;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class ModRecipes {
    public static void register() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, TippedSwords.id("vial_filling"), VialFillingRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, TippedSwords.id("sword_tipping"), SwordTippingRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, TippedSwords.id("sword_cleaning"), SwordCleaningRecipe.SERIALIZER);
    }
}
