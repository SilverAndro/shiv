package io.github.silverandro.shiv.mixin.compat.decorativeblocks;

import io.github.silverandro.shiv.mixin_util.IfMatch;
import io.github.silverandro.shiv.mixin_util.MixinReplace;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@IfMatch(
	matchString = "decorative_blocks (quilt_registry_entry_attachment | quilt_registry_entry_attachment)",
	message = "Removing decorative blocks builtin fuel registration"
)
@Mixin(value = AbstractFurnaceBlockEntity.class)
public class PreventBadFuelRegistration {
	@MixinReplace(
		origin = "lilypuree.decorative_blocks.mixin.AbstractFurnaceTileEntityMixin",
		name = "onGetBurnTime"
	)
	private static void removeDirectCallToFuelRegistration() {}
}
