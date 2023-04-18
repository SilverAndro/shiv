package io.github.silverandro.fiwb;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FuckItWeBall implements PreLaunchEntrypoint {
	public static final Logger LOGGER = LoggerFactory.getLogger("FIWB");

	@Override
	public void onPreLaunch(ModContainer mod) {
		LOGGER.info("Reached pre-init! (even though we should have complete control over mixin and other classes by now)");
	}
}
