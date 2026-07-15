package com.cobblestoner;


import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.effect.MobEffects;

import com.cobblestoner.Models.CoatingType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Note: CoatingManager's ItemStack-based methods (get/set/fromPotion) aren't covered
// here because constructing an ItemStack for a registered Item requires the full
// data-driven component-binding bootstrap (WorldLoader/RegistryLayer), which
// Bootstrap.bootStrap() alone doesn't perform in this test environment. merge() is
// pure data and fully testable without that machinery.
public class CoatingManagerTest {
	@BeforeAll
	static void setupMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static CoatingData poison(int amplifier, int hits) {
		return new CoatingData(CoatingType.POTION, List.of(new CoatingEffect(MobEffects.POISON, amplifier)), hits, 110);
	}

	private static CoatingData harming(int hits) {
		return new CoatingData(CoatingType.POTION, List.of(new CoatingEffect(MobEffects.INSTANT_DAMAGE, 0)), hits, 1);
	}

	@Test
	void mergeSumsHitsForSameCoating() {
		List<CoatingData> base = List.of(poison(0, 4));
		List<CoatingData> additions = List.of(poison(0, 8));

		List<CoatingData> merged = CoatingManager.merge(base, additions);

		assertEquals(1, merged.size());
		assertEquals(12, merged.get(0).hits());
	}

	@Test
	void mergeKeepsDifferentLevelsAsSeparateEntries() {
		// A Poison I vial and a Poison II vial aren't the same coating, so both
		// survive as their own entries rather than being averaged or rejected.
		List<CoatingData> base = List.of(poison(0, 4));
		List<CoatingData> additions = List.of(poison(1, 4));

		List<CoatingData> merged = CoatingManager.merge(base, additions);

		assertEquals(2, merged.size());
	}

	@Test
	void mergeKeepsDifferentTypesAsSeparateEntries() {
		List<CoatingData> base = List.of(poison(0, 4));
		List<CoatingData> additions = List.of(harming(4));

		List<CoatingData> merged = CoatingManager.merge(base, additions);

		assertEquals(2, merged.size());
	}

	@Test
	void mergeWithNoAdditionsReturnsBaseUnchanged() {
		List<CoatingData> base = List.of(poison(0, 4));

		List<CoatingData> merged = CoatingManager.merge(base, List.of());

		assertEquals(base, merged);
	}

	@Test
	void mergeCapsHitsAtThreeVialsWorth() {
		// Each vial of this potion is worth 8 hits, so 3 vials' worth caps at 24 -
		// mirrors a harming sword (8 instant hits/vial * 3 vials = 24 max).
		List<CoatingData> base = List.of(harming(16));
		List<CoatingData> additions = List.of(harming(8));

		List<CoatingData> merged = CoatingManager.merge(base, additions);

		assertEquals(24, merged.get(0).hits());
	}

	@Test
	void mergeStaysCappedAcrossRepeatedRecrafts() {
		// Sword is already at the 3-vial cap; re-tipping with 3 more vials must not
		// push it past 24 (it shouldn't be possible to stack past "3 vials' worth"
		// just by crafting again).
		List<CoatingData> base = List.of(harming(24));
		List<CoatingData> additions = List.of(harming(8), harming(8), harming(8));

		List<CoatingData> merged = CoatingManager.merge(base, additions);

		assertEquals(24, merged.get(0).hits());
	}

	@Test
	void mergeKeepsWindAndFireChargesAsSeparateEntries() {
		List<CoatingData> base = List.of(CoatingManager.fromWindCharge());
		List<CoatingData> additions = List.of(CoatingManager.fromFireCharge());

		List<CoatingData> merged = CoatingManager.merge(base, additions);

		assertEquals(2, merged.size());
	}

	@Test
	void witherRoseIsWitherLevelOneScaledLikeAPotion() {
		CoatingData coating = CoatingManager.fromWitherRose();

		assertEquals(CoatingType.POTION, coating.type());
		assertEquals(MobEffects.WITHER, coating.effects().get(0).effect());
		assertEquals(0, coating.effects().get(0).amplifier());
		assertTrue(coating.hits() > 0);
	}
}
