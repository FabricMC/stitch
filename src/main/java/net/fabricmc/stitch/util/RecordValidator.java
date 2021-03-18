/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.stitch.util;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

public class RecordValidator implements AutoCloseable {
	private static final String[] REQUIRED_METHOD_SIGNATURES = new String[]{
			"toString()Ljava/lang/String;",
			"hashCode()I",
			"equals(Ljava/lang/Object;)Z"
	};

	private final StitchUtil.FileSystemDelegate inputFs;
	private final Path inputJar;
	private final boolean printInfo;

	private final List<String> errors = new LinkedList<>();

	public RecordValidator(File jarFile, boolean printInfo) throws IOException {
		this.inputJar = (inputFs = StitchUtil.getJarFileSystem(jarFile, false)).get().getPath("/");
		this.printInfo = printInfo;
	}

	public void validate() throws IOException, RecordValidationException {
		Files.walkFileTree(inputJar, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (attrs.isDirectory()) {
					return FileVisitResult.CONTINUE;
				}

				if (file.getFileName().toString().endsWith(".class")) {
					byte[] classBytes = Files.readAllBytes(file);
					validateClass(classBytes);
				}

				return FileVisitResult.CONTINUE;
			}
		});

		if (!errors.isEmpty()) {
			throw new RecordValidationException(errors);
		}
	}

	// Returns true when a record
	private boolean validateClass(byte[] classBytes) {
		ClassNode classNode = new ClassNode(StitchUtil.ASM_VERSION);
		ClassReader classReader = new ClassReader(classBytes);
		classReader.accept(classNode, 0);

		if ((classNode.access & Opcodes.ACC_RECORD) == 0) {
			// Not a record
			return false;
		}

		for (RecordComponentNode component : classNode.recordComponents) {
			// Ensure that a matching method is present
			boolean foundMethod = false;
			for (MethodNode method : classNode.methods) {
				if (method.name.equals(component.name) && method.desc.equals("()" +component.descriptor)) {
					foundMethod = true;
					break;
				}
			}

			// Ensure that a matching field is present
			boolean foundField = false;
			for (FieldNode field : classNode.fields) {
				if (field.name.equals(component.name) && field.desc.equals(component.descriptor)) {
					foundField = true;
					break;
				}
			}

			if (!foundMethod) {
				errors.add(String.format("Could not find matching getter method for %s()%s in %s", component.name, component.descriptor, classNode.name));
			}

			if (!foundField) {
				errors.add(String.format("Could not find matching field for %s;%s in %s", component.name, component.descriptor, classNode.name));
			}
		}

		// Ensure that all of the expected methods are present
		for (String requiredMethodSignature : REQUIRED_METHOD_SIGNATURES) {
			boolean foundMethod = false;
			for (MethodNode method : classNode.methods) {
				if ((method.name + method.desc).equals(requiredMethodSignature)) {
					foundMethod = true;
					break;
				}
			}

			if (!foundMethod) {
				errors.add(String.format("Could not find required method %s in %s", requiredMethodSignature, classNode.name));
			}
		}

		if (printInfo) {
			printInfo(classNode);
		}

		// This is a record
		return true;
	}

	// Just print some info out about the record.
	private void printInfo(ClassNode classNode) {
		StringBuilder sb = new StringBuilder();

		sb.append("Found record ").append(classNode.name).append(" with components:\n");

		for (RecordComponentNode componentNode : classNode.recordComponents) {
			sb.append('\t').append(componentNode.name).append("\t").append(componentNode.descriptor).append('\n');
		}

		String toString = extractToString(classNode);

		if (toString != null) {
			sb.append("toString: ").append(toString).append('\n');
		}

		System.out.print(sb.append('\n').toString());
	}

	// Pulls out the string used in the toString call, can hopefully be used to auto populate these names.
	private String extractToString(ClassNode classNode) {
		MethodNode methodNode = null;

		for (MethodNode method : classNode.methods) {
			if ((method.name + method.desc).equals("toString()Ljava/lang/String;")) {
				methodNode = method;
				break;
			}
		}

		if (methodNode == null) {
			return null;
		}

		for (AbstractInsnNode insnNode : methodNode.instructions) {
			if (insnNode instanceof InvokeDynamicInsnNode) {
				InvokeDynamicInsnNode invokeDynamic = (InvokeDynamicInsnNode) insnNode;
				if (
					!invokeDynamic.name.equals("toString") ||
					!invokeDynamic.desc.equals(String.format("(L%s;)Ljava/lang/String;", classNode.name)) ||
					!invokeDynamic.bsm.getName().equals("bootstrap") ||
					!invokeDynamic.bsm.getOwner().equals("java/lang/runtime/ObjectMethods")
				) {
					// Not what we are looking for
					continue;
				}

				for (Object bsmArg : invokeDynamic.bsmArgs) {
					if (bsmArg instanceof String) {
						return (String) bsmArg;
					}
				}
			}
		}

		return null;
	}

	@Override
	public void close() throws Exception {
		inputFs.close();
	}

	public static class RecordValidationException extends Exception {
		public final List<String> errors;

		public RecordValidationException(List<String> errors) {
			this.errors = errors;
		}
	}
}
