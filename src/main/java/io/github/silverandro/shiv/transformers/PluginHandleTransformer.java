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

public class PluginHandleTransformer implements ClassFileTransformer {
	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Override
	public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		if (!Objects.equals(className, "org/spongepowered/asm/mixin/transformer/PluginHandle")) {
			return null;
		}

		var node = new ClassNode();
		var reader = new ClassReader(classfileBuffer);
		reader.accept(node, 0);

		node.methods.forEach(methodNode -> {
			if (methodNode.name.equals("shouldApplyMixin")) {
				InsnList patch = new InsnList();
				LabelNode jump = new LabelNode();

				patch.add(new IntInsnNode(Opcodes.ALOAD, 2));
				patch.add(new MethodInsnNode(
					Opcodes.INVOKESTATIC,
                        "io/github/silverandro/shiv/mayo/MixinDeleter",
					"shouldApplyOtherMixin",
					"(Ljava/lang/String;)Z"
				));
				patch.add(new JumpInsnNode(Opcodes.IFNE, jump));
				patch.add(new InsnNode(Opcodes.ICONST_0));
				patch.add(new InsnNode(Opcodes.IRETURN));
				patch.add(jump);

				methodNode.instructions.insert(patch);
			}
		});

		var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		node.accept(writer);
		byte[] bytes = writer.toByteArray();

		var out = QuiltLoader.getGameDir().resolve(".shiv");
		out.toFile().mkdirs();
		out = out.resolve("PluginHandle.class");
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
