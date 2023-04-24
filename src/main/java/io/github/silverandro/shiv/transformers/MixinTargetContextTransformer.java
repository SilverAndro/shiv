package io.github.silverandro.shiv.transformers;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.quiltmc.loader.api.QuiltLoader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Objects;

public class MixinTargetContextTransformer implements ClassFileTransformer {
	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Override
	public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		if (!Objects.equals(className, "org/spongepowered/asm/mixin/transformer/MixinTargetContext")) {
			return null;
		}

		var node = new ClassNode();
		var reader = new ClassReader(classfileBuffer);
		reader.accept(node, 0);

		node.methods.forEach(methodNode -> {
			if (methodNode.name.equals("getMethods")) {
				InsnList loadMixinName = new InsnList();
				InsnList patchFilter = new InsnList();

				loadMixinName.add(new IntInsnNode(Opcodes.ALOAD, 0));
				loadMixinName.add(new FieldInsnNode(
					Opcodes.GETFIELD,
					"org/spongepowered/asm/mixin/transformer/MixinTargetContext",
					"classNode",
					"Lorg/objectweb/asm/tree/ClassNode;"
				));
				loadMixinName.add(new FieldInsnNode(
					Opcodes.GETFIELD,
					"org/objectweb/asm/tree/ClassNode",
					"name",
					"Ljava/lang/String;"
				));

				patchFilter.add(new MethodInsnNode(
					Opcodes.INVOKESTATIC,
                        "io/github/silverandro/shiv/mayo/MixinDeleter",
					"filterMethods",
					"(Ljava/lang/String;Ljava/util/List;)Ljava/util/List;"
				));

				methodNode.instructions.insert(loadMixinName);
				methodNode.instructions.insertBefore(methodNode.instructions.getLast().getPrevious(), patchFilter);
			}
		});

		var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		node.accept(writer);
		byte[] bytes = writer.toByteArray();

		var out = QuiltLoader.getGameDir().resolve(".shiv");
		out.toFile().mkdirs();
		out = out.resolve("MixinTargetContext.class");
		try {
			out.toFile().createNewFile();
			var fos = new FileOutputStream(out.toFile());
			fos.write(bytes);
			fos.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return bytes;
	}
}
