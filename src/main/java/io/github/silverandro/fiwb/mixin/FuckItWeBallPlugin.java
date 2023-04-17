package io.github.silverandro.fiwb.mixin;

import io.github.silverandro.fiwb.Bootstrap;
import io.github.silverandro.fiwb.FuckItWeBall;
import io.github.silverandro.fiwb.MatchEngine;
import io.github.silverandro.fiwb.mixin_util.IfMatch;
import io.github.silverandro.fiwb.mixin_util.MixinReplace;
import io.github.silverandro.fiwb.mixin_util.meta.ReplacedMixin;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.util.Annotations;

import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

public class FuckItWeBallPlugin implements IMixinConfigPlugin {
	private static final String IF_MATCH_DESC = Type.getDescriptor(IfMatch.class);
	private static final String MIXIN_REPLACE_DESC = Type.getDescriptor(MixinReplace.class);

	@Override
	public void onLoad(String mixinPackage) {
		try {
			Bootstrap.boostrap();
		} catch (NoSuchFieldException | IllegalAccessException | UnmodifiableClassException | ClassNotFoundException |
				 InvocationTargetException | NoSuchMethodException e) {
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
			var ifMatch = Class.forName(mixinClassName).getAnnotation(IfMatch.class);
			if (ifMatch == null) {
				return true;
			}

			if (MatchEngine.isMatch(ifMatch.matchString())) {
				FuckItWeBall.LOGGER.info(ifMatch.message());
				return true;
			} else {
				FuckItWeBall.LOGGER.info("Not applying " + mixinClassName);
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
		replaceMixins(targetClass, mixinInfo.getClassNode(0));
		removeCustomAnnotations(targetClass);
		removeCustomInjections(targetClass);
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

	private void removeCustomAnnotations(ClassNode targetNode) {
		if (targetNode.visibleAnnotations == null) return;
		targetNode.visibleAnnotations.removeIf(annotationNode -> annotationNode.desc.equals(IF_MATCH_DESC));
	}

	private void removeCustomInjections(ClassNode targetNode) {
		targetNode.methods.removeIf(methodNode -> {
			var result = Annotations.get(methodNode.visibleAnnotations, MIXIN_REPLACE_DESC);
			return result != null;
		});
	}
}
