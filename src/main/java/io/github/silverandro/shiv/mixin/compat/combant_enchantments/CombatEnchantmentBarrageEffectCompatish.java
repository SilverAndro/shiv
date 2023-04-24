package io.github.silverandro.shiv.mixin.compat.combant_enchantments;

import com.dsfhdshdjtsb.CombatEnchants.CombatEnchants;
import io.github.silverandro.shiv.mixin_util.IfMatch;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BowItem.class)
@IfMatch(
	matchString = "cenchants (quilt_item_extension | quilt_item_extensions)",
	message = "Injecting custom barrage effect code that is partially compatible with qsl"
)
public class CombatEnchantmentBarrageEffectCompatish {
	private LivingEntity centchant_compat_captured_user;

	@Inject(method = "onStoppedUsing", at = @At("HEAD"))
	public void captureBowItemUser(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
		centchant_compat_captured_user = user;
	}

	@ModifyVariable(
		method = "onStoppedUsing",
		at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/item/BowItem;getPullProgress(I)F"),
		index = 9
	)
	public float modifyPullSpeedWithCombatEnchantsBarrageEffect(float pullProgress) {
		StatusEffectInstance barrageEffectInstance = centchant_compat_captured_user.getStatusEffect(CombatEnchants.BARRAGE_EFFECT);
		if (barrageEffectInstance != null) {
			if (barrageEffectInstance.getDuration() >= 20) {
				centchant_compat_captured_user.removeStatusEffect(CombatEnchants.BARRAGE_EFFECT);
				centchant_compat_captured_user.addStatusEffect(new StatusEffectInstance(CombatEnchants.BARRAGE_EFFECT, 20));
			}

			return 1.0F;
		} else {
			return pullProgress;
		}
	}
}
