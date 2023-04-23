package io.github.silverandro.fiwb.mixin.compat.armored_elytra;

import io.github.silverandro.fiwb.mixin_util.ArbitraryTransform;
import io.github.silverandro.fiwb.mixin_util.IfMatch;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ArmorFeatureRenderer.class)
@IfMatch(
	matchString = "armored-elytra quilt_entity_rendering",
	message = "Making sure that the mixin ordering between armored elytra and quilt is correct"
)
public class EnsureArmoredElytraOrdering {
	@ArbitraryTransform
	private static void ensureMixinOrdering(ClassNode targetClass) {
		for (var method : targetClass.methods) {
			if (method.name.equals("method_17157")) {
				// TODO, patch
				return;
			}
		}
	}
}
