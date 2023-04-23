package io.github.silverandro.fiwb.mixin.compat.armored_elytra;

import io.github.silverandro.fiwb.mixin_util.ArbitraryTransform;
import io.github.silverandro.fiwb.mixin_util.IfMatch;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
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
			exit:
			if (method.name.equals("method_17157")) {
				MethodInsnNode elytraIndex = null;
				MethodInsnNode quiltIndex = null;

				for (var abstractInsnNode : method.instructions) {
					if (abstractInsnNode instanceof MethodInsnNode methodInsnNode) {
						var name = methodInsnNode.name;
						if (name.endsWith("armored-elytra$render")) {
							if (elytraIndex == null) {
								elytraIndex = methodInsnNode;
							} else {
								// Either somehow doubled up, or already in the right order
								return;
							}
						}

						if (name.endsWith("quilt$captureEntity")) {
							if (elytraIndex != null && quiltIndex == null) {
								quiltIndex = methodInsnNode;
							} else {
								// Either doubled up or right order already
								return;
							}
						}
					}
				}

				// idfk
				if (elytraIndex == null || quiltIndex == null) {
					return;
				}

				// Swap quilt and armored elytra methods
				var quiltNext = quiltIndex.getNext();
				var elytraPrevious = elytraIndex.getPrevious();

				method.instructions.remove(quiltIndex);
				method.instructions.remove(elytraIndex);

				method.instructions.insertBefore(elytraPrevious.getNext(), quiltIndex);
				method.instructions.insertBefore(quiltNext, elytraIndex);

				return;
			}
		}
	}
}
