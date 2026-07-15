package com.cobblestoner.client;

import com.cobblestoner.entity.ModEntities;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class TippedSwordsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(ModEntities.THROWN_VIAL, ThrownItemRenderer::new);
	}
}
