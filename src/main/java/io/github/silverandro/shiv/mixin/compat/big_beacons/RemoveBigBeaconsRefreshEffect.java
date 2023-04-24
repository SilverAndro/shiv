package io.github.silverandro.shiv.mixin.compat.big_beacons;

import easton.bigbeacons.FlightEffect;
import io.github.silverandro.shiv.mixin_util.IfMatch;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FlightEffect.class)
@IfMatch(
	matchString = "bigbeacons quilt_status_effect",
	message = "Replacing big beacons flight effect with api usage"
)
public class RemoveBigBeaconsRefreshEffect {
	/**
	 * @author Silver
	 * @reason Code relies on mixin that cant be trivially replicated and had to be removed
	 */
	@Overwrite
	public void onRemoved(LivingEntity entity, AttributeContainer attributes, int amplifier) {}
}
