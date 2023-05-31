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

package net.fabricmc.stitch.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;

import net.fabricmc.mapping.util.EntryTriple;
import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.adapter.MappingDstNsReorder;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;
import net.fabricmc.stitch.plugin.PluginLoader;
import net.fabricmc.stitch.plugin.StitchPlugin;
import net.fabricmc.stitch.representation.AbstractJarEntry;
import net.fabricmc.stitch.representation.ClassStorage;
import net.fabricmc.stitch.representation.JarClassEntry;
import net.fabricmc.stitch.representation.JarFieldEntry;
import net.fabricmc.stitch.representation.JarMethodEntry;
import net.fabricmc.stitch.representation.JarRootEntry;
import net.fabricmc.stitch.util.MatcherUtil;
import net.fabricmc.stitch.util.Pair;
import net.fabricmc.stitch.util.StitchUtil;

class GenState {
	private static final String official = "official";
	private static final String intermediary = "intermediary";
	private static final int officialIndex = -1;
	private static final int intermediaryIndex = 0;
	private final MemoryMappingTree mappingTree = new MemoryMappingTree();
	private final Map<IntermediaryType, Integer> counters = new HashMap<>();
	private final Map<AbstractJarEntry, Integer> values = new IdentityHashMap<>();
	private MappingFormat counterFileFormat = MappingFormat.TINY;
	private MappingFormat mappingFileFormat = MappingFormat.TINY;
	private GenMap oldToIntermediary, newToOld;
	private boolean targetFileMappingsPresent;
	private boolean interactive = true;
	private boolean writeAll = false;
	private Scanner scanner = new Scanner(System.in);
	private String targetPackage = "";

	public enum IntermediaryType {
		CLASS("class"),
		FIELD("field"),
		METHOD("method");

		public final String prefix;

		IntermediaryType(String prefix) {
			this.prefix = prefix;
		}
	}

	GenState() throws IOException {
		mappingTree.visitNamespaces(official, Arrays.asList(intermediary));

		// Set target package
		List<String> pkgs = PluginLoader.getLoadedPlugins().stream()
				.map(plugin -> plugin.getIntermediaryTargetPackage())
				.filter(pkg -> pkg != null && !pkg.isEmpty())
				.collect(Collectors.toList());
		if (pkgs.size() > 1) {
			throw new IllegalStateException("More than one plugin has declared an intermediary target package!");
		} else if (pkgs.size() == 1) {
			setTargetPackage(pkgs.get(0));
		}
	}

	public void setTargetPackage(String pkg) {
		if (pkg.lastIndexOf("/") != (pkg.length() - 1)) {
			this.targetPackage = pkg + "/";
		} else {
			this.targetPackage = pkg;
		}
	}

	private void validateNamespaces(MappingTree tree) {
		if (tree.getDstNamespaces().size() != 1
				|| !tree.getSrcNamespace().equals(official)
				|| !tree.getDstNamespaces().contains(intermediary)) {
			throw new RuntimeException("Existing namespaces don't match '" + official + "' + '" + intermediary + "'!");
		}
	}

	// TODO: Remove this once mapping-io#30 is merged
	private void clearCounterMetadata() {
		boolean removedAny;

		do {
			removedAny = false;
			removedAny |= mappingTree.removeMetadata("next-intermediary-class") != null;
			removedAny |= mappingTree.removeMetadata("next-intermediary-field") != null;
			removedAny |= mappingTree.removeMetadata("next-intermediary-method") != null;
		} while (removedAny);
	}

	public void setWriteAll(boolean writeAll) {
		this.writeAll = writeAll;
	}

	public void disableInteractive() {
		interactive = false;
	}

	public String next(AbstractJarEntry entry, IntermediaryType type) {
		return type.prefix + "_" + values.computeIfAbsent(entry, (e) -> {
			int v = counters.getOrDefault(type, 1);
			counters.put(type, v + 1);
			return v;
		});
	}

	public void setCounter(IntermediaryType key, int value) {
		counters.put(key, value);
	}

	public Map<IntermediaryType, Integer> getCounters() {
		return Collections.unmodifiableMap(counters);
	}

	public void generate(File file, JarRootEntry jarEntry, JarRootEntry jarOld) throws IOException {
		if (file.exists()) {
			// Target file already exists, re-use contained mappings.
			System.out.println("Target file exists - loading...");

			mappingFileFormat = MappingReader.detectFormat(file.toPath());
			MemoryMappingTree tempTree = new MemoryMappingTree();
			MappingReader.read(file.toPath(), mappingFileFormat, tempTree);
			validateNamespaces(tempTree);

			if (tempTree.getClasses().size() > 0) {
				targetFileMappingsPresent = true;
			}

			readCountersFromTree(tempTree);
			clearCounterMetadata();
			tempTree.accept(mappingTree);
		}

		readCounterFileIfPresent();

		for (JarClassEntry c : jarEntry.getClasses()) {
			MappingTree cMappingTree = addClass(c, jarOld, jarEntry, this.targetPackage);

			if (cMappingTree != null) {
				cMappingTree.accept(mappingTree);
			}
		}

		writeCounters();
		mappingTree.visitEnd();

		MappingWriter writer = MappingWriter.create(file.toPath(), mappingFileFormat);
		MemoryMappingTree treeToWrite = new MemoryMappingTree();
		mappingTree.accept(new MappingDstNsReorder(treeToWrite, intermediary));
		treeToWrite.accept(writer);
		writer.close();
	}

	private boolean needsIntermediaryName(Function<StitchPlugin, Integer> priorityGetter) {
		List<Integer> results = PluginLoader.getLoadedPlugins().stream()
				.map(plugin -> priorityGetter.apply(plugin))
				.sorted((a, b) -> Math.abs(b) - Math.abs(a))
				.collect(Collectors.toList());

		int first = results.get(0);

		if (results.size() > 1) {
			int second = results.get(1);

			if (first != 0
					&& first != second
					&& Math.abs(first) == Math.abs(second)) {
				throw new IllegalStateException("Got conflicting scores of the same priority!");
			}
		}

		return first > 0;
	}

	private boolean needsIntermediaryName(ClassStorage storage, JarClassEntry cls) {
		return needsIntermediaryName((plugin) -> plugin.needsIntermediaryName(storage, cls));
	}

	private boolean needsIntermediaryName(ClassStorage storage, JarClassEntry cls, JarFieldEntry fld) {
		return needsIntermediaryName((plugin) -> plugin.needsIntermediaryName(storage, cls, fld));
	}

	private boolean needsIntermediaryName(ClassStorage storage, JarClassEntry cls, JarMethodEntry mth) {
		return needsIntermediaryName((plugin) -> plugin.needsIntermediaryName(storage, cls, mth));
	}

	@Nullable
	private String getFieldName(ClassStorage storage, JarClassEntry c, JarFieldEntry f) {
		if (!needsIntermediaryName(storage, c, f)) {
			return null;
		}

		Object existingMapping;
		String existingName = null;

		// Check for existing name from target file
		if ((existingMapping = mappingTree.getField(c.getFullyQualifiedName(), f.getName(), f.getDescriptor())) != null) {
			existingName = ((FieldMapping) existingMapping).getDstName(intermediaryIndex);
		}

		// Check for existing name from supplied old mappings file
		if (existingName == null
				&& newToOld != null
				&& (existingMapping = newToOld.getField(c.getFullyQualifiedName(), f.getName(), f.getDescriptor())) != null
				&& (existingMapping = oldToIntermediary.getField((EntryTriple) existingMapping)) != null) {
			existingName = ((EntryTriple) existingMapping).getName();
		}

		if (existingName != null) {
			if (existingName.contains(IntermediaryType.FIELD.prefix + "_")) {
				return existingName;
			} else {
				String newName = next(f, IntermediaryType.FIELD);
				System.out.println(existingName + " is now " + newName);
				return newName;
			}
		}

		return next(f, IntermediaryType.FIELD);
	}

	private final Map<JarMethodEntry, String> methodNames = new IdentityHashMap<>();

	private String getPropagation(ClassStorage storage, JarClassEntry classEntry) {
		if (classEntry == null) {
			return "";
		}

		StringBuilder builder = new StringBuilder(classEntry.getFullyQualifiedName());
		List<String> strings = new ArrayList<>();
		String scs = getPropagation(storage, classEntry.getSuperClass(storage));

		if (!scs.isEmpty()) {
			strings.add(scs);
		}

		for (JarClassEntry ce : classEntry.getInterfaces(storage)) {
			scs = getPropagation(storage, ce);

			if (!scs.isEmpty()) {
				strings.add(scs);
			}
		}

		if (!strings.isEmpty()) {
			builder.append("<-");

			if (strings.size() == 1) {
				builder.append(strings.get(0));
			} else {
				builder.append("[");
				builder.append(StitchUtil.join(",", strings));
				builder.append("]");
			}
		}

		return builder.toString();
	}

	private String getNamesListEntry(ClassStorage storage, JarClassEntry classEntry) {
		StringBuilder builder = new StringBuilder(getPropagation(storage, classEntry));

		if (classEntry.isInterface()) {
			builder.append("(itf)");
		}

		return builder.toString();
	}

	private Set<JarMethodEntry> findNames(ClassStorage storageOld, ClassStorage storageNew, JarClassEntry c, JarMethodEntry m, Map<String, Set<String>> names) {
		Set<JarMethodEntry> allEntries = new HashSet<>();
		findNames(storageOld, storageNew, c, m, names, allEntries);
		return allEntries;
	}

	private void findNames(ClassStorage storageOld, ClassStorage storageNew, JarClassEntry c, JarMethodEntry m, Map<String, Set<String>> names, Set<JarMethodEntry> usedMethods) {
		if (!usedMethods.add(m)) {
			return;
		}

		String suffix = "." + m.getName() + m.getDescriptor();

		if ((m.getAccess() & Opcodes.ACC_BRIDGE) != 0) {
			suffix += "(bridge)";
		}

		List<JarClassEntry> ccList = m.getMatchingEntries(storageNew, c);

		for (JarClassEntry cc : ccList) {
			Object existingMapping = mappingTree.getMethod(cc.getFullyQualifiedName(), m.getName(), m.getDescriptor());
			String existingName = null;

			// Check for existing name from target file
			if ((existingMapping = mappingTree.getMethod(cc.getFullyQualifiedName(), m.getName(), m.getDescriptor())) != null) {
				existingName = ((MethodMapping) existingMapping).getDstName(intermediaryIndex);
			}

			// Check for existing name from supplied old mappings file
			if (existingName == null
					&& newToOld != null
					&& (existingMapping = newToOld.getMethod(cc.getFullyQualifiedName(), m.getName(), m.getDescriptor())) != null) {
				EntryTriple mapping = oldToIntermediary.getMethod((EntryTriple) existingMapping);

				if (mapping != null) {
					existingName = ((EntryTriple) mapping).getName();
				}
			}

			if (existingName != null) {
				names.computeIfAbsent(existingName, (s) -> new TreeSet<>()).add(getNamesListEntry(storageNew, cc) + suffix);
			} else if (existingMapping != null) {
				// Check old method's entire hierarchy for potential mappings
				EntryTriple entry = (EntryTriple) existingMapping;
				JarClassEntry oldBase = storageOld.getClass(entry.getOwner(), false);

				if (oldBase != null) {
					JarMethodEntry oldM = oldBase.getMethod(entry.getName() + entry.getDescriptor());
					List<JarClassEntry> cccList = oldM.getMatchingEntries(storageOld, oldBase);

					for (JarClassEntry ccc : cccList) {
						entry = oldToIntermediary.getMethod(ccc.getFullyQualifiedName(), oldM.getName(), oldM.getDescriptor());

						if (entry != null) {
							names.computeIfAbsent(entry.getName(), (s) -> new TreeSet<>()).add(getNamesListEntry(storageOld, ccc) + suffix);
						}
					}
				}
			}
		}

		for (JarClassEntry mc : ccList) {
			for (Pair<JarClassEntry, String> pair : mc.getRelatedMethods(m)) {
				findNames(storageOld, storageNew, pair.getLeft(), pair.getLeft().getMethod(pair.getRight()), names, usedMethods);
			}
		}
	}

	@Nullable
	private String getMethodName(ClassStorage storageOld, ClassStorage storageNew, JarClassEntry c, JarMethodEntry m) {
		if (!needsIntermediaryName(storageNew, c, m)) {
			return null;
		}

		if (methodNames.containsKey(m)) {
			return methodNames.get(m);
		}

		if (newToOld != null || targetFileMappingsPresent) {
			Map<String, Set<String>> names = new HashMap<>();
			Set<JarMethodEntry> allEntries = findNames(storageOld, storageNew, c, m, names);

			for (JarMethodEntry mm : allEntries) {
				if (methodNames.containsKey(mm)) {
					return methodNames.get(mm);
				}
			}

			if (names.size() > 1) {
				System.out.println("Conflict detected - matched same target name!");
				List<String> nameList = new ArrayList<>(names.keySet());
				Collections.sort(nameList);

				for (int i = 0; i < nameList.size(); i++) {
					String s = nameList.get(i);
					System.out.println((i+1) + ") " + s + " <- " + StitchUtil.join(", ", names.get(s)));
				}

				if (!interactive) {
					throw new RuntimeException("Conflict detected!");
				}

				while (true) {
					String cmd = scanner.nextLine();
					int i;

					try {
						i = Integer.parseInt(cmd);
					} catch (NumberFormatException e) {
						e.printStackTrace();
						continue;
					}

					if (i >= 1 && i <= nameList.size()) {
						for (JarMethodEntry mm : allEntries) {
							methodNames.put(mm, nameList.get(i - 1));
						}

						System.out.println("OK!");
						return nameList.get(i - 1);
					}
				}
			} else if (names.size() == 1) {
				String s = names.keySet().iterator().next();

				for (JarMethodEntry mm : allEntries) {
					methodNames.put(mm, s);
				}

				if (s.contains(IntermediaryType.METHOD.prefix + "_")) {
					return s;
				} else {
					String newName = next(m, IntermediaryType.METHOD);
					System.out.println(s + " is now " + newName);
					return newName;
				}
			}
		}

		return next(m, IntermediaryType.METHOD);
	}

	private MappingTree addClass(JarClassEntry c, ClassStorage storageOld, ClassStorage storage, String prefix) throws IOException {
		String cName = "";
		String origPrefix = prefix;
		boolean anyIntermediaries = false;

		if (!needsIntermediaryName(storage, c)) {
			cName = c.getName();
		} else {
			cName = null;

			if (newToOld != null || targetFileMappingsPresent) {
				Object existingMapping = mappingTree.getClass(c.getFullyQualifiedName());
				String existingName = null;

				// Check for existing name from target file
				if (existingMapping != null) {
					existingName = ((ClassMapping) existingMapping).getDstName(intermediaryIndex);
				}

				// Check for existing name from supplied old mappings file
				if (existingName == null
						&& newToOld != null
						&& (existingMapping = newToOld.getClass(c.getFullyQualifiedName())) != null) {
					existingName = oldToIntermediary.getClass((String) existingMapping);
				}

				if (existingName != null) {
					// There is an existing name, so we reuse that.
					// If we're looking at a subclass, only reuse the
					// subclass's name, not the parent classes' ones too.
					String[] r = existingName.split("\\$");
					cName = r[r.length - 1];

					if (r.length == 1) {
						// We aren't looking at a subclass;
						// reuse entire fully qualified name.
						prefix = "";
					}
				}
			}

			if (cName != null && !cName.contains(IntermediaryType.CLASS.prefix + "_")) {
				System.out.println(cName + " is now " + (cName = next(c, IntermediaryType.CLASS)));
				prefix = origPrefix;
				anyIntermediaries = true;
			} else if (cName == null) {
				cName = next(c, IntermediaryType.CLASS);
				anyIntermediaries = true;
			}
		}

		MemoryMappingTree mappingTree = new MemoryMappingTree();
		mappingTree.visitNamespaces(official, Arrays.asList(intermediary));

		mappingTree.visitClass(c.getFullyQualifiedName());
		mappingTree.visitDstName(MappedElementKind.CLASS, intermediaryIndex, prefix + cName);

		for (JarFieldEntry f : c.getFields()) {
			String fName = getFieldName(storage, c, f);

			if (fName != null || writeAll) {
				anyIntermediaries = true;
				if (fName == null) fName = f.getName();
			}

			if (fName != null) {
				mappingTree.visitField(f.getName(), f.getDescriptor());
				mappingTree.visitDstName(MappedElementKind.FIELD, intermediaryIndex, fName);
			}
		}

		for (JarMethodEntry m : c.getMethods()) {
			String mName = getMethodName(storageOld, storage, c, m);

			if (mName != null || writeAll) {
				anyIntermediaries = true;
				if (mName == null) mName = m.getName();
			}

			if (mName != null) {
				mappingTree.visitMethod(m.getName(), m.getDescriptor());
				mappingTree.visitDstName(MappedElementKind.METHOD, intermediaryIndex, mName);
			}
		}

		for (JarClassEntry cc : c.getInnerClasses()) {
			MappingTree ccMappingTree = addClass(cc, storageOld, storage, prefix + cName + "$");

			if (ccMappingTree != null) {
				ccMappingTree.accept(mappingTree);
				anyIntermediaries = true;
			}
		}

		return anyIntermediaries || writeAll ? mappingTree : null;
	}

	public void prepareRewrite(File oldMappings) throws IOException {
		oldToIntermediary = new GenMap();
		newToOld = new GenMap.Dummy();

		readOldMappings(oldMappings).accept(mappingTree);
	}

	public void prepareUpdate(File oldMappings, File matches) throws IOException {
		oldToIntermediary = new GenMap();
		newToOld = new GenMap();

		oldToIntermediary.load(readOldMappings(oldMappings));

		try (FileReader fileReader = new FileReader(matches)) {
			try (BufferedReader reader = new BufferedReader(fileReader)) {
				MatcherUtil.read(reader, true, newToOld::addClass, newToOld::addField, newToOld::addMethod);
			}
		}
	}

	private MappingTree readOldMappings(File oldMappings) throws IOException {
		MemoryMappingTree tempTree = new MemoryMappingTree();
		mappingFileFormat = MappingReader.detectFormat(oldMappings.toPath());
		MappingReader.read(oldMappings.toPath(), mappingFileFormat, tempTree);

		validateNamespaces(tempTree);
		readCountersFromTree(tempTree);

		return tempTree;
	}

	private void readCounterFileIfPresent() throws IOException {
		Path counterPath = getExternalCounterFile();

		if (counterPath == null || !Files.exists(counterPath)) {
			return;
		}

		MappingFormat format = MappingReader.detectFormat(counterPath);

		if (format != null) {
			MemoryMappingTree mappingTree = new MemoryMappingTree();
			MappingReader.read(counterPath, format, mappingTree);
			readCountersFromTree(mappingTree);
			counterFileFormat = format;
			return;
		}

		System.err.println("Counter file isn't a valid mapping file! Switching to fallback mode...");

		try (FileReader fileReader = new FileReader(counterPath.toFile())) {
			try (BufferedReader reader = new BufferedReader(fileReader)) {
				String line;

				while ((line = reader.readLine()) != null) {
					if (line.startsWith("# INTERMEDIARY-COUNTER")) {
						String[] parts = line.split(" ");
						IntermediaryType type;

						try {
							type = IntermediaryType.valueOf(parts[2].toUpperCase(Locale.ROOT));
							counters.put(type, Integer.parseInt(parts[3]));
						} catch (IllegalArgumentException e) {
							System.err.println("Encountered unknown intermediary type '" + parts[2] + "'");
						}
					}
				}
			}
		}
	}

	private void readCountersFromTree(MappingTree tree) {
		String counter = tree.getMetadata("next-intermediary-class");
		if (counter != null) counters.put(IntermediaryType.CLASS, Integer.parseInt(counter));

		counter = tree.getMetadata("next-intermediary-field");
		if (counter != null) counters.put(IntermediaryType.FIELD, Integer.parseInt(counter));

		counter = tree.getMetadata("next-intermediary-method");
		if (counter != null) counters.put(IntermediaryType.METHOD, Integer.parseInt(counter));
	}

	private void writeCounters() throws IOException {
		clearCounterMetadata();
		Path counterPath = getExternalCounterFile();
		MemoryMappingTree mappingTree;

		if (counterPath == null) {
			mappingTree = this.mappingTree;
		} else {
			mappingTree = new MemoryMappingTree();
			mappingTree.visitNamespaces(official, Arrays.asList(intermediary));
		}

		mappingTree.visitMetadata("next-intermediary-class", counters.getOrDefault(IntermediaryType.CLASS, 0).toString());
		mappingTree.visitMetadata("next-intermediary-field", counters.getOrDefault(IntermediaryType.FIELD, 0).toString());
		mappingTree.visitMetadata("next-intermediary-method", counters.getOrDefault(IntermediaryType.METHOD, 0).toString());

		if (counterPath != null) {
			MappingWriter writer = MappingWriter.create(counterPath, counterFileFormat);
			mappingTree.accept(writer);
			writer.close();
		}
	}

	private Path getExternalCounterFile() {
		if (System.getProperty("stitch.counter") != null) {
			return Paths.get(System.getProperty("stitch.counter"));
		}

		return null;
	}
}
