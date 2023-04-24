package io.github.silverandro.shiv.mixin;

import io.github.silverandro.shiv.Bootstrap;
import io.github.silverandro.shiv.ShivInit;
import io.github.silverandro.shiv.MatchEngine;
import io.github.silverandro.shiv.internal.UnusedTargetClass;
import io.github.silverandro.shiv.mixin_util.ArbitraryTransform;
import io.github.silverandro.shiv.mixin_util.IfMatch;
import io.github.silverandro.shiv.mixin_util.MixinReplace;
import io.github.silverandro.shiv.mixin_util.meta.ReplacedMixin;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.quiltmc.loader.api.QuiltLoader;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Constants;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ShivPlugin implements IMixinConfigPlugin {
	private static final String IF_MATCH_DESC = Type.getDescriptor(IfMatch.class);
	private static final String MIXIN_REPLACE_DESC = Type.getDescriptor(MixinReplace.class);
	private static final String ARBITRARY_TRANSFORM_DESC = Type.getDescriptor(ArbitraryTransform.class);

	@Override
	public void onLoad(String mixinPackage) {
		try {
			Bootstrap.boostrap();
		} catch (NoSuchFieldException | IllegalAccessException | UnmodifiableClassException | ClassNotFoundException |
				 InvocationTargetException | NoSuchMethodException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		try {
			// TODO: Better way to do this? Could cause classloader issues
			var ifMatch = Class.forName(mixinClassName).getAnnotation(IfMatch.class);
			if (ifMatch == null) {
				return true;
			}

			if (MatchEngine.isMatch(ifMatch.matchString())) {
				ShivInit.LOGGER.info(ifMatch.message());
				return true;
			} else {
				ShivInit.LOGGER.info("Not applying " + mixinClassName);
				return false;
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		try {
			replaceMixins(targetClass, mixinInfo.getClassNode(0));
			applyArbitraryTransforms(targetClass, mixinInfo.getClassNode(0));
			removeCustomAnnotations(targetClass);
			removeCustomInjections(targetClass);
		} catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException |
				 ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private <T> T getAnnotationArg(AnnotationNode node, String name) {
		//noinspection unchecked
		return (T)node.values.get(node.values.indexOf(name) + 1);
	}

	private void replaceMixins(ClassNode targetNode, ClassNode mixinNode) {
		for (var replacement : mixinNode.methods) {
			if (replacement.visibleAnnotations == null) {
				continue;
			}

			for (var annotation : replacement.visibleAnnotations) {
				if (annotation.desc.equals(MIXIN_REPLACE_DESC)) {
					String origin = getAnnotationArg(annotation, "origin");
					String name = getAnnotationArg(annotation, "name");

					boolean didFind = false;

					for (var target : targetNode.methods) {
						if (target.name.endsWith(name)) {
							var targetAnnotation = Annotations.get(target.visibleAnnotations, Type.getDescriptor(MixinMerged.class));
							if (targetAnnotation != null) {
								String targetOrigin = getAnnotationArg(targetAnnotation, "mixin");
								if (targetOrigin.equals(origin)) {
									// mark found
									didFind = true;

									// copy the method
									target.instructions = replacement.instructions;
									target.tryCatchBlocks = replacement.tryCatchBlocks;

									// add a mark that its been replaced
									Annotations.setVisible(target, ReplacedMixin.class,
										"value", "L" + mixinNode.name + ";" + replacement.name + replacement.desc
									);
								}
							}
						}
					}

					if (!didFind) {
						throw new MixinException("Failed to find target for MixinReplace " + replacement.desc);
					}
				}
			}
		}
	}

	// We cant load the mixin class (re-entry requirements), so instead we copy the methods into a *new* synthetic class
	private void applyArbitraryTransforms(ClassNode targetNode, ClassNode mixinNode) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException, ClassNotFoundException {
		var transformMethods = new ArrayList<MethodNode>();
		for (var mixinMethod : mixinNode.methods) {
			var arbitraryTransform = Annotations.get(mixinMethod.visibleAnnotations, ARBITRARY_TRANSFORM_DESC);
			if (arbitraryTransform != null) {
				if ((mixinMethod.access & Opcodes.ACC_STATIC) == 0) {
					throw new MixinException("Arbitrary transform method " + mixinMethod.name + " is not static");
				}
				transformMethods.add(mixinMethod);
			}
		}

		if (transformMethods.isEmpty()) {
			return;
		}

		var newClass = new ClassNode();
		var name = "ArbitraryTransform" + (Long.toHexString(new Random().nextLong()));
		newClass.name = "io/github/silverandro/shiv/internal/" + name;
		newClass.version = MixinEnvironment.getCompatibilityLevel().getClassVersion();
		newClass.access = Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC;
		newClass.superName = Constants.OBJECT;

		var initMethod = new MethodNode();
		initMethod.name = "<init>";
		initMethod.desc = "()V";
		initMethod.access = Opcodes.ACC_PRIVATE;
		initMethod.instructions.add(new IntInsnNode(Opcodes.ALOAD, 0));
		initMethod.instructions.add(new MethodInsnNode(
			Opcodes.INVOKESPECIAL,
			"java/lang/Object",
			"<init>",
			"()V"
		));
		initMethod.instructions.add(new InsnNode(Opcodes.RETURN));
		newClass.methods.add(initMethod);

		// Make sure theyre always public
		transformMethods.forEach(methodNode -> {
			methodNode.access = methodNode.access | Opcodes.ACC_PUBLIC;
			methodNode.access = methodNode.access & (~Opcodes.ACC_PRIVATE);
		});
		newClass.methods.addAll(transformMethods);

		var lookup = MethodHandles.privateLookupIn(UnusedTargetClass.class, MethodHandles.lookup());
		var classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		newClass.accept(classWriter);

		var out = QuiltLoader.getGameDir().resolve(".shiv/synth");
		out.toFile().mkdirs();
		out = out.resolve(name + ".class");
		try {
			out.toFile().createNewFile();
			var fos = new FileOutputStream(out.toFile());
			fos.write(classWriter.toByteArray());
			fos.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		var clazz = lookup.defineClass(classWriter.toByteArray());
		transformMethods.forEach(methodNode -> {
			try {
				clazz.getMethod(methodNode.name, ClassNode.class).invoke(null, targetNode);
			} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void removeCustomAnnotations(ClassNode targetNode) {
		if (targetNode.visibleAnnotations == null) return;
		targetNode.visibleAnnotations.removeIf(annotationNode -> annotationNode.desc.equals(IF_MATCH_DESC));
	}

	@SuppressWarnings("RedundantIfStatement")
	private void removeCustomInjections(ClassNode targetNode) {
		targetNode.methods.removeIf(methodNode -> {
			var mixinReplace = Annotations.get(methodNode.visibleAnnotations, MIXIN_REPLACE_DESC);
			if (mixinReplace != null) return true;

			var arbitraryTransform = Annotations.get(methodNode.visibleAnnotations, ARBITRARY_TRANSFORM_DESC);
			if (arbitraryTransform != null) return true;

			return false;
		});
	}
}
