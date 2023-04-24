# Shiv: its like a shim but more dangerous

This mod uses a collection of hacks and tomfoolery to restore compatibility with other mods on quilt. This mostly involves modifying or removing mixins from other mods and replacing it with compatible code.

**You must be running minecraft in a JDK for this mod to work, the JRE does not support the attach API.**

---

Please dont complain to mod authors if their mod is broken while this mod is installed!
All crash reports should go on the [github](https://github.com/SilverAndro/shiv/issues).

## Compatibility fixes:
- Decorative blocks
- Combat Enchantments
- Big Beacons
- Armored Elytra
- Rpg Difficulty

## Internal toolkit:
- Injection overwriting
- Mixin control (shouldApplyMixin for other mods)
- Mixin method removal
- Arbitrary bytecode transformations
