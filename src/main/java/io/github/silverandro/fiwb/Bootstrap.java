package io.github.silverandro.fiwb;

import io.github.silverandro.fiwb.transformers.MixinTargetContextTransformer;
import io.github.silverandro.fiwb.transformers.PluginHandleTransformer;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.quiltmc.loader.api.QuiltLoader;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import static io.github.silverandro.fiwb.FuckItWeBall.LOGGER;

public class Bootstrap {
	private static boolean didBootstrap = false;

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static void boostrap() throws NoSuchFieldException, IllegalAccessException, UnmodifiableClassException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IOException {
		if (didBootstrap) {
			return;
		}

		didBootstrap = true;

		LOGGER.info("FUCK IT WE BALL");
		LOGGER.info("FUCK IT WE BALL");
		LOGGER.info("FUCK IT WE BALL");
		LOGGER.info("FUCK IT WE BALL");

		LOGGER.info("Classloder: " + Bootstrap.class.getClassLoader());

		// Clear out the synth classes
		var synthFiles = QuiltLoader.getGameDir().resolve(".fiwb/synth").toFile();
		deleteFolder(synthFiles);

		// Add to the env
		QuiltLoader.getAllMods().forEach(modContainer -> {
			LOGGER.debug("Added " + modContainer.metadata().id() + " " + modContainer.metadata().version().raw() + " to the match engine");
			MatchEngine.register(modContainer.metadata().id(), modContainer.metadata().version().raw());
		});

		// Attach our instrumentation agent
		try (var stream = Bootstrap.class.getClassLoader().getResourceAsStream("/META-INF/jars/Mayo-1.0.0.jar")) {
			if (stream == null) {
				throw new IllegalStateException("Could not get instrumentation agent file!");
			}

			File tmpFile = QuiltLoader.getGameDir().resolve(".fiwb").toFile();
			tmpFile.mkdirs();
			tmpFile = tmpFile.toPath().resolve("mayo.jar").toFile();
			tmpFile.createNewFile();

			var outputFileStream = new FileOutputStream(tmpFile);
			outputFileStream.write(stream.readAllBytes());
			outputFileStream.close();

			ByteBuddyAgent.attach(tmpFile, String.valueOf(ProcessHandle.current().pid()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Smuggle instrumentation through the Knot/App classloader barrier
		Class<?> mayoClass = Class.forName("io.github.silverandro.fiwb.mayo.Mayo", false, QuiltLoader.class.getClassLoader());
		final Field field = mayoClass.getDeclaredField("instrument");
		field.setAccessible(true);
		var instrument = (Instrumentation)field.get(null);
		if (instrument == null) {
			throw new IllegalStateException("Failed to smuggle instrumentation");
		}
		LOGGER.info("Smuggled out instrumentation object: " + instrument);

		// Hijack mixin plugin decisions
		// We need to work our way to an instance of the plugin handle class
		// You can get one from Mixins.getConfigs() -> Set<Config>[0] -> private field `config` -> private field plugin
		Config config = (Config)Mixins.getConfigs().toArray()[0];
		Field mixinConfigField = config.getClass().getDeclaredField("config");
		mixinConfigField.setAccessible(true);
		Object mixinConfigObject = mixinConfigField.get(config);
		Field pluginHandleField = mixinConfigObject.getClass().getDeclaredField("plugin");
		pluginHandleField.setAccessible(true);
		Class<?> pluginHandleClass = pluginHandleField.getType();

		// Have a clean error if we cant actually do the boostrapping we need
		if (instrument.isRetransformClassesSupported()) {
			if (instrument.isModifiableClass(pluginHandleClass)) {
				// Patch the plugin handle class
				LOGGER.info("Patching PluginHandle to check with MixinDeleter");
				var pluginHandlePatcher = new PluginHandleTransformer();
				instrument.addTransformer(pluginHandlePatcher, true);
				instrument.retransformClasses(pluginHandleClass);
				instrument.removeTransformer(pluginHandlePatcher);

				// Patch MixinTargetContext
				LOGGER.info("Patching MixinTargetContext to hide mixin methods");
				var targetContextPatcher = new MixinTargetContextTransformer();
				instrument.addTransformer(targetContextPatcher, true);
				instrument.retransformClasses(MixinTargetContext.class);
				instrument.removeTransformer(targetContextPatcher);
			} else {
				throw new IllegalStateException("Cannot modify plugin handle class! PluginHandle class is not modifiable");
			}
		} else {
			throw new IllegalStateException("Cannot modify plugin handle class! This JVM does not support retransformation");
		}

		LOGGER.info("Successfully bootstrapped!");
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if(files!=null) { //some JVMs return null for empty dirs
			for(File f: files) {
				if(f.isDirectory()) {
					deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}
}
