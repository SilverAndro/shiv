package io.github.silverandro.fiwb;

import com.dsfhdshdjtsb.CombatEnchants.CombatEnchants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.base.api.util.TriState;
import org.quiltmc.qsl.entity.effect.api.StatusEffectEvents;
import org.quiltmc.qsl.entity.effect.api.StatusEffectRemovalReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FuckItWeBall implements PreLaunchEntrypoint, ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("FIWB");

	@Override
	public void onPreLaunch(ModContainer mod) {
		LOGGER.info("Reached pre-init! (even though we should have complete control over mixin and other classes by now)");
	}

	@Override
	public void onInitialize(ModContainer mod) {
		LOGGER.info("Reached main init! Registering game level mod compat.");

		if (MatchEngine.isMatch("bigbeacons quilt_status_effect")) {
			LOGGER.info("Adding status effect code for big beacons");
			StatusEffectEvents.ON_REMOVED.register((entity, effect, reason) -> {
				if (reason != StatusEffectRemovalReason.UPGRADE_REAPPLYING) {
					if (entity instanceof PlayerEntity) {
						ServerPlayerEntity player = (ServerPlayerEntity)entity;
						if (!player.isSpectator() && !player.isCreative()) {
							player.getAbilities().allowFlying = false;
							player.getAbilities().flying = false;
							player.sendAbilitiesUpdate();
							player.server.getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_GAME_MODE, player));
						}
					}
				}
			});
		}

		if (MatchEngine.isMatch("cenchants quilt_status_effect")) {
			LOGGER.info("Adding status effect code for combat enchantments");
			StatusEffectEvents.SHOULD_REMOVE.register((entity, effect, reason) -> {
				if (effect.getEffectType().equals(CombatEnchants.LIFELINE_COOLDOWN_EFFECT)) {
					return TriState.FALSE;
				}

				if (effect.getEffectType().equals(CombatEnchants.LIFESTEAL_COOLDOWN_EFFECT)) {
					return TriState.FALSE;
				}

				if (effect.getEffectType().equals(CombatEnchants.SHIELDING_COOLDOWN_EFFECT)) {
					return TriState.FALSE;
				}

				return TriState.DEFAULT;
			});
		}
	}
}
