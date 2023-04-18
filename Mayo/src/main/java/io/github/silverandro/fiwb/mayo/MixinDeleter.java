package io.github.silverandro.fiwb.mayo;

import java.util.Arrays;

@SuppressWarnings("unused")
public class MixinDeleter {
	private static final String[] mixinsToDelete = new String[] {
		"com.dsfhdshdjtsb.CombatEnchants.mixin.CenchantsBowItemMixin"
	};

	public static boolean shouldApplyOtherMixin(String otherMixinClassName) {
		if (Arrays.asList(mixinsToDelete).contains(otherMixinClassName)) {
			System.out.println("Preventing mixin " + otherMixinClassName + " from applying");
			return false;
		}
		return true;
	}
}
