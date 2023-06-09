package io.github.silverandro.shiv.mixin.compat.decorativeblocks;

import io.github.silverandro.shiv.mixin_util.IfMatch;
import lilypuree.decorative_blocks.DecorativeBlocks;
import lilypuree.decorative_blocks.core.DBBlocks;
import lilypuree.decorative_blocks.core.DBTags;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@IfMatch(
	matchString = "decorative_blocks (quilt_registry_entry_attachment | quilt_registry_entry_attachment)",
	message = "Injecting proper fuel registration into decorative blocks"
)
@Mixin(value = DecorativeBlocks.class, remap = false)
public class RegisterFuelTimesProperly {
	@SuppressWarnings("deprecation")
	@Inject(method = "onInitialize", at = @At("RETURN"), remap = false)
	public void registerFuelProperly(CallbackInfo ci) {
		FuelRegistry.INSTANCE.add(DBTags.Items.BEAMS, 300);
		FuelRegistry.INSTANCE.add(DBTags.Items.PALISADES, 300);
		FuelRegistry.INSTANCE.add(DBTags.Items.SEATS, 300);
		FuelRegistry.INSTANCE.add(DBTags.Items.SUPPORTS, 300);
		FuelRegistry.INSTANCE.add(DBTags.Items.CHANDELIERS, 1600);
		FuelRegistry.INSTANCE.add(DBBlocks.LATTICE, 100);
	}
}
