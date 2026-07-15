# Tipped Swords

A Fabric mod that lets you coat a sword with potion effects, Wind Charges, or Fire
Charges - and have them land on every hit.

## How it works

- **Fill a vial**: surround a brewed potion with empty vials in a crafting grid to
  fill them with that potion's effect(s).
- **Tip a sword**: combine a sword with 1-3 filled vials (and/or a raw Wind Charge,
  Fire Charge, or Wither Rose, which skip the vial step) to coat it. A sword can carry
  up to 3 distinct effects at once, but never both Wind Charge and Fire Charge
  together.
- **Land the hit**: coated swords apply their effect(s) on every hit - including
  vanilla's sweep attack, at a fair, scaled-down share instead of full strength.
  Instant effects (Harming, Healing) are split evenly across a vial's hits rather than
  re-applying their full dose each time.
- **Throw a vial**: a filled vial can also be thrown like a splash potion.
- **Clean a sword**: a water bucket plus a coated sword washes it back to plain -
  the bucket empties out like any other crafting-grid bucket use.

Coated items are disguised as vanilla items via Polymer, so other players don't need
the mod installed to see them correctly.

## Setup

For dev environment setup instructions, see the [Fabric Documentation
page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up)
for the IDE you're using.

## License

MIT - see [LICENSE](LICENSE).
