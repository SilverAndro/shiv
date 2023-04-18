package io.github.silverandro.fiwb.mayo;

import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class MixinDeleter {
	private static final String[] mixinsToDelete = new String[] {
		"com.dsfhdshdjtsb.CombatEnchants.mixin.CenchantsBowItemMixin",
		"easton.bigbeacons.mixin.LivingEntityMixin"
	};

	private static final HashMap<String, ArrayList<String>> methodsToHide = new HashMap<>();

	static {
		var cenchantsLivingEntityMixins = new ArrayList<String>();
		cenchantsLivingEntityMixins.add("method_6012");
		cenchantsLivingEntityMixins.add("clearStatusEffects");
		methodsToHide.put("com/dsfhdshdjtsb/CombatEnchants/mixin/CenchantsLivingEntityMixin", cenchantsLivingEntityMixins);
	}

	public static List<MethodNode> filterMethods(String mixinClassName, List<MethodNode> methods) {
		var list = methodsToHide.get(mixinClassName);
		if (list == null) return methods;

		return methods.stream().filter(methodNode -> !list.contains(methodNode.name)).collect(Collectors.toList());
	}

	public static boolean shouldApplyOtherMixin(String otherMixinClassName) {
		if (Arrays.asList(mixinsToDelete).contains(otherMixinClassName)) {
			System.out.println("Preventing mixin " + otherMixinClassName + " from applying");
			return false;
		}
		return true;
	}
}
