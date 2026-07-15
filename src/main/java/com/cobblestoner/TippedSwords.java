package com.cobblestoner;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cobblestoner.entity.ModEntities;
import com.cobblestoner.items.ModItems;
import com.cobblestoner.recipe.ModRecipes;

public class TippedSwords implements ModInitializer {
	public static final String MOD_ID = "tipped_sword";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ModItems.register();
		ModComponents.register();
		ModEntities.register();
		ModRecipes.register();

		LOGGER.info("Hello Fabric world!");
		// Primary-hit coating application lives in TippedSwordsAttackMixin, not here -
		// see that class for why (needs to run after vanilla's own knockback resolves).
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
