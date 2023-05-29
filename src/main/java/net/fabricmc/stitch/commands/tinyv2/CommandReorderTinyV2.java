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

package net.fabricmc.stitch.commands.tinyv2;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import net.fabricmc.stitch.Command;

/**
 * <ul>
 * 	<li>Reorders the columns in the tiny file</li>
 * 	<li>Remaps the descriptors to use the newly first column</li>
 * </ul>
 *
 * <p>For example, this:
 * <pre><code>
 * intermediary	named	official
 * c	net/minecraft/class_123	net/minecraft/somePackage/someClass	a
 * m	(Lnet/minecraft/class_124;)V	method_1234 someMethod	a
 * </code></pre>
 * Reordered to official intermediary named:
 * <pre><code>
 * official	intermediary	named
 * c	a	net/minecraft/class_123	net/minecraft/somePackage/someClass
 * m	(La;)V	a	method_1234	someMethod
 * </code></pre>
 * </p>
 *
 * <p>
 * This is used to reorder the the official-intermediary mappings to be intermediary-official, so they can be merged with
 * intermediary-named in CommandMergeTinyV2, and then reorder the outputted intermediary-official-named to official-intermediary-named.
 * </p>
 */
public class CommandReorderTinyV2 extends Command {
	public CommandReorderTinyV2() {
		super("reorderTinyV2");
	}

	/**
	 * Reorders the columns in {@code <old-mapping-file>} according to {@code [new name order...]} and puts the result in {@code <new-mapping-file>}.
	 * {@code new name order} is, for example, {@code official intermediary named}.
	 */
	@Override
	public String getHelpString() {
		return "<old-mapping-file> <new-mapping-file> [new name order...]";
	}

	@Override
	public boolean isArgumentCountValid(int count) {
		return count >= 4;
	}

	@Override
	public void run(String[] args) throws Exception {
		Path oldMappingFile = Paths.get(args[0]);
		Path newMappingFile = Paths.get(args[1]);
		List<String> newOrder = Arrays.asList(Arrays.copyOfRange(args, 2, args.length));

		TinyFile tinyFile = TinyV2Reader.read(oldMappingFile);
		validateNamespaces(newOrder, tinyFile);

		Map<String, TinyClass> mappingCopy = tinyFile.getClassEntries().stream()
				.collect(Collectors.toMap(c -> c.getClassNames().get(0),
						c -> new TinyClass(new ArrayList<>(c.getClassNames()), c.getMethods(), c.getFields(), c.getComments())));
		int newFirstNamespaceOldIndex = tinyFile.getHeader().getNamespaces().indexOf(newOrder.get(0));

		reorder(tinyFile, newOrder);
		remapDescriptors(tinyFile, mappingCopy, newFirstNamespaceOldIndex);

		TinyV2Writer.write(tinyFile, newMappingFile);
	}

	private void validateNamespaces(List<String> newOrder, TinyFile tinyFile) {
		HashSet<String> fileNamespacesOrderless = new HashSet<>(tinyFile.getHeader().getNamespaces());
		HashSet<String> providedNamespacesOrderless = new HashSet<>(newOrder);

		if (!fileNamespacesOrderless.equals(providedNamespacesOrderless)) {
			throw new IllegalArgumentException("The tiny file has different namespaces than those specified."
					+ " specified: " + providedNamespacesOrderless.toString() + ", file: " + fileNamespacesOrderless.toString());
		}
	}

	private void reorder(TinyFile tinyFile, List<String> newOrder) {
		Map<Integer, Integer> indexMapping = new HashMap<>();

		for (int i = 0; i < newOrder.size(); i++) {
			indexMapping.put(tinyFile.getHeader().getNamespaces().indexOf(newOrder.get(i)), i);
		}

		visitNames(tinyFile, (names) -> {
			// This way empty names won't be skipped
			for (int i = names.size(); i < newOrder.size(); i++) {
				names.add("");
			}

			List<String> namesCopy = new ArrayList<>(names);

			for (int i = 0; i < namesCopy.size(); i++) {
				names.set(indexMapping.get(i), namesCopy.get(i));
			}
		});
	}

	private void remapDescriptors(TinyFile tinyFile, Map<String, TinyClass> mappings, int targetNamespace) {
		for (TinyClass tinyClass : tinyFile.getClassEntries()) {
			for (TinyMethod method : tinyClass.getMethods()) {
				remapMethodDescriptor(method, mappings, targetNamespace);
			}

			for (TinyField field : tinyClass.getFields()) {
				remapFieldDescriptor(field, mappings, targetNamespace);
			}
		}
	}

	/**
	 * In this case the visitor is not a nice man and reorganizes the house as he sees fit.
	 */
	private void visitNames(TinyFile tinyFile, Consumer<List<String>> namesVisitor) {
		namesVisitor.accept(tinyFile.getHeader().getNamespaces());

		for (TinyClass tinyClass : tinyFile.getClassEntries()) {
			namesVisitor.accept(tinyClass.getClassNames());

			for (TinyMethod method : tinyClass.getMethods()) {
				namesVisitor.accept(method.getMethodNames());

				for (TinyMethodParameter parameter : method.getParameters()) {
					namesVisitor.accept(parameter.getParameterNames());
				}

				for (TinyLocalVariable localVariable : method.getLocalVariables()) {
					namesVisitor.accept(localVariable.getLocalVariableNames());
				}
			}

			for (TinyField field : tinyClass.getFields()) {
				namesVisitor.accept(field.getFieldNames());
			}
		}
	}

	private void remapFieldDescriptor(TinyField field, Map<String, TinyClass> mappings, int targetNamespace) {
		String newDescriptor = remapType(field.getFieldDescriptorInFirstNamespace(), mappings, targetNamespace);
		field.setFieldDescriptorInFirstNamespace(newDescriptor);
	}

	////////////////// This part can be replaced with a descriptor parser library
	// (I already have one, not sure if I should add it)

	private void remapMethodDescriptor(TinyMethod method, Map<String, TinyClass> mappings, int targetNamespace) {
		String descriptor = method.getMethodDescriptorInFirstNamespace();
		String[] paramsAndReturnType = descriptor.split(Pattern.quote(")"));

		if (paramsAndReturnType.length != 2) {
			throw new IllegalArgumentException("method descriptor '" + descriptor + "' is of an unknown format.");
		}

		List<String> params = parseParameterDescriptors(paramsAndReturnType[0].substring(1));
		String returnType = paramsAndReturnType[1];
		List<String> paramsMapped = params.stream().map(p -> remapType(p, mappings, targetNamespace)).collect(Collectors.toList());
		String returnTypeMapped = returnType.equals("V") ? "V" : remapType(returnType, mappings, targetNamespace);
		String newDescriptor = "(" + String.join("", paramsMapped) + ")" + returnTypeMapped;

		method.setMethodDescriptorInFirstNamespace(newDescriptor);
	}

	private static final Collection<String> primitiveTypeNames = Arrays.asList("B", "C", "D", "F", "I", "J", "S", "Z");

	private List<String> parseParameterDescriptors(String concatenatedParameterDescriptors) {
		List<String> parameterDescriptors = new ArrayList<>();
		boolean inClassName = false;
		int inArrayNestingLevel = 0;
		StringBuilder currentClassName = new StringBuilder();

		for (int i = 0; i < concatenatedParameterDescriptors.length(); i++) {
			char c = concatenatedParameterDescriptors.charAt(i);

			if (inClassName) {
				if (c == ';') {
					if (currentClassName.length() == 0) {
						throw new IllegalArgumentException("Empty class name in parameter list " + concatenatedParameterDescriptors
								+ " at position " + i);
					}

					parameterDescriptors.add(Strings.repeat("[", inArrayNestingLevel) + "L" + currentClassName.toString() + ";");
					inArrayNestingLevel = 0;
					currentClassName = new StringBuilder();
					inClassName = false;
				} else {
					currentClassName.append(c);
				}
			} else {
				if (primitiveTypeNames.contains(Character.toString(c))) {
					parameterDescriptors.add(Strings.repeat("[", inArrayNestingLevel) + c);
					inArrayNestingLevel = 0;
				} else if (c == '[') {
					inArrayNestingLevel++;
				} else if (c == 'L') {
					inClassName = true;
				} else {
					throw new IllegalArgumentException("Unexpected special character " + c + " in parameter descriptor list "
							+ concatenatedParameterDescriptors);
				}
			}
		}

		return parameterDescriptors;
	}

	/**
	 * Remaps type from namespace X, to the namespace of targetNamespaceIndex in mappings, where mappings
	 * is a mapping from names in namespace X to the names in all other namespaces.
	 */
	private String remapType(String type, Map<String, TinyClass> mappings, int targetNamespaceIndex) {
		if (type.isEmpty()) throw new IllegalArgumentException("types cannot be empty strings");
		if (primitiveTypeNames.contains(type)) return type;

		if (type.charAt(0) == '[') {
			return "[" + remapType(type.substring(1), mappings, targetNamespaceIndex);
		}

		if (type.charAt(0) == 'L' && type.charAt(type.length() - 1) == ';') {
			String className = type.substring(1, type.length() - 1);
			TinyClass mapping = mappings.get(className);
			String remappedName = mapping != null ? mapping.getClassNames().get(targetNamespaceIndex) : className;
			return "L" + remappedName + ";";
		}

		throw new IllegalArgumentException("type descriptor '" + type + "' is of an unknown format.");
	}
}
