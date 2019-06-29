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

import net.fabricmc.stitch.Command;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;

// TODO: Remap descriptors on fields and methods.
public class CommandMergeTiny extends Command {
	public enum TinyEntryType {
		ROOT,
		CLASS,
		FIELD,
		METHOD;

		private static Map<String, TinyEntryType> BY_NAME = new HashMap<>();

		public static TinyEntryType byName(String s) {
			return BY_NAME.get(s);
		}

		static {
			for (TinyEntryType type : values()) {
				BY_NAME.put(type.name(), type);
			}
		}
	}

	public static class TinyEntry {
		public final TinyEntryType type;
		public final String header;
		// Map<index, name>
		public final Map<String, String> names = new HashMap<>();
		// Table<index, name, instance>
		private final Map<String, Map<String, TinyEntry>> children = new HashMap<>();
		private TinyEntry parent;

		public TinyEntry(TinyEntryType type, String header) {
			this.type = type;
			this.header = header;
		}

		public TinyEntry getParent() {
			return parent;
		}

		public boolean containsChild(String key, String value) {
			Map<String, TinyEntry> map = children.get(key);
			return map != null && map.containsKey(value);
		}

		public TinyEntry getChild(String key, String value) {
			Map<String, TinyEntry> map = children.get(key);
			return map != null ? map.get(value) : null;
		}

		public void putChild(String key, String value, TinyEntry entry) {
			children.computeIfAbsent(key, (s) -> new HashMap<>()).put(value, entry);
		}

		public void addChild(TinyEntry entry, String nameSuffix) {
			entry.parent = this;

			for (Map.Entry<String, String> e : entry.names.entrySet()) {
				String key = e.getKey();
				String value = e.getValue() + nameSuffix;

				if (containsChild(key, value)) {
					throw new RuntimeException("Duplicate TinyEntry: (" + key + ", " + value + ")!");
				}

				putChild(key, value, entry);
			}
		}

		public Map<String, TinyEntry> getChildRow(String key) {
			//noinspection unchecked
			return children.getOrDefault(key, Collections.EMPTY_MAP);
		}
	}

	public static class TinyFile {
		public final String[] indexList;
		public final TinyEntry root = new TinyEntry(TinyEntryType.ROOT, "");
		public final int typeCount;

		public TinyFile(File f) throws IOException {
			try (BufferedReader reader = Files.newBufferedReader(f.toPath(), Charset.forName("UTF-8"))) {
				String[] header = reader.readLine().trim().split("\t");
				if (header.length < 3 || !header[0].trim().equals("v1")) {
					throw new RuntimeException("Invalid header!");
				}

				typeCount = header.length - 1;
				indexList = new String[typeCount];
				for (int i = 0; i < typeCount; i++) {
					indexList[i] = header[i + 1].trim();
				}

				String line;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.length() == 0 || line.charAt(0) == '#') {
						continue;
					}

					String[] parts = line.split("\t");
					for (int i = 0; i < parts.length; i++) {
						parts[i] = parts[i].trim();
					}

					StringBuilder prefix = new StringBuilder();
					prefix.append(parts[0]);
					for (int i = 1; i < parts.length - typeCount; i++) {
						prefix.append('\t');
						prefix.append(parts[i]);
					}

					TinyEntryType type = TinyEntryType.byName(parts[0]);
					String[] path = parts[1].split("\\$");
					TinyEntry parent = root;

					for (int i = 0; i < (type == TinyEntryType.CLASS ? path.length - 1 : path.length); i++) {
						TinyEntry nextParent = parent.getChild(indexList[0], path[i]);
						if (nextParent == null) {
							nextParent = new TinyEntry(TinyEntryType.CLASS, "CLASS");
							nextParent.names.put(indexList[0], path[i]);
							parent.addChild(nextParent, "");
						}
						parent = nextParent;
					}

					TinyEntry entry;
					if (type == TinyEntryType.CLASS && parent.containsChild(indexList[0], path[path.length - 1])) {
						entry = parent.getChild(indexList[0], path[path.length - 1]);
					} else {
						entry = new TinyEntry(type, prefix.toString());
					}

					String[] names = new String[typeCount];
					for (int i = 0; i < typeCount; i++) {
						names[i] = parts[parts.length - typeCount + i];
						if (type == TinyEntryType.CLASS) {
							// add classes by their final inner class name
							String[] splitly = names[i].split("\\$");
							entry.names.put(indexList[i], splitly[splitly.length - 1]);
						} else {
							entry.names.put(indexList[i], names[i]);
						}
					}

					switch (type) {
						case CLASS:
							parent.addChild(entry, "");
							break;
						case FIELD:
						case METHOD:
							parent.addChild(entry, parts[2]);
							break;
					}
				}
			}
		}
/*
		public String match(String[] entries, String key) {
			if (indexMap.containsKey(key)) {
				return entries[indexMap.get(key)];
			} else {
				return null;
			}
		}
*/
	}

	private List<String> mappingBlankFillOrder = new ArrayList<>();
	private String sharedIndexName;

	public CommandMergeTiny() {
		super("mergeTiny");
	}

	@Override
	public String getHelpString() {
		return "<input-a> <input-b> <output> [mappingBlankFillOrder...]";
	}

	@Override
	public boolean isArgumentCountValid(int count) {
		return count >= 4;
	}

	private TinyFile inputA, inputB;

	private String fixMatch(TinyEntry a, TinyEntry b, String matchA, String index) {
		if (a == null || matchA == null) {
			return matchA;
		}

		if (a.type == TinyEntryType.CLASS && a.getParent() != null && a.getParent().type == TinyEntryType.CLASS) {
			// First, map to the shared index name (sharedIndexName)
			String officialPath = a.names.get(sharedIndexName);
			TinyEntry officialEntry = a.getParent();
			while (officialEntry.type == TinyEntryType.CLASS) {
				officialPath = officialEntry.names.get(sharedIndexName) + "$" + officialPath;
				officialEntry = officialEntry.getParent();
			}

			// Now, traverse it from the ROOT and get the names
			Set<String> matchingOrder = new LinkedHashSet<>();
			matchingOrder.add(index);
			matchingOrder.addAll(mappingBlankFillOrder);

			String[] path = officialPath.split("\\$");
			a = inputA.root;
			b = inputB.root;

			StringBuilder targetName = new StringBuilder();

			for (int i = 0; i < path.length; i++) {
				if (i > 0) {
					targetName.append('$');
				}

				a = a != null ? a.getChild(sharedIndexName, path[i]) : null;
				b = b != null ? b.getChild(sharedIndexName, path[i]) : null;
				boolean appended = false;

				for (String mName : matchingOrder) {
					String nameA = a != null ? a.names.get(mName) : null;
					String nameB = b != null ? b.names.get(mName) : null;

					if (nameA != null) {
						targetName.append(nameA);
						appended = true;
						break;
					} else if (nameB != null) {
						targetName.append(nameB);
						appended = true;
						break;
					}
				}

				if (!appended) {
					throw new RuntimeException("Could not find mapping for " + officialPath + "!");
				}
			}

			return targetName.toString();
		}

		return matchA;
	}

	private String getMatch(TinyEntry a, TinyEntry b, String index, String realIndex) {
		String matchA = a != null ? a.names.get(index) : null;
		String matchB = b != null ? b.names.get(index) : null;

		assert a == null || b == null || a.type == b.type;

		matchA = fixMatch(a, b, matchA, realIndex);
		matchB = fixMatch(b, a, matchB, realIndex);

		if (matchA != null) {
			if (matchB != null && !matchA.equals(matchB)) {
				throw new RuntimeException("No match: " + index + " " + matchA + " " + matchB);
			}

			return matchA;
		} else {
			return matchB;
		}
	}

	private String getEntry(TinyEntry a, TinyEntry b, List<String> totalIndexList) {
		if (a != null && b != null && !(a.header.equals(b.header))) {
			throw new RuntimeException("Header mismatch: " + a.header + " != " + b.header);
		} else if (a != null && b != null && a.type != b.type) {
			throw new RuntimeException("Type mismatch: " + a.type + " != " + b.type);
		}

		String header = a != null ? a.header : b.header;
		StringBuilder entry = new StringBuilder();
		entry.append(header);

		for (String index : totalIndexList) {
			entry.append('\t');

			String match = getMatch(a, b, index, index);
			if (match == null) {
				for (String s : mappingBlankFillOrder) {
					match = getMatch(a, b, s, index);
					if (match != null) {
						break;
					}
				}

				if (match == null) {
					throw new RuntimeException("TODO");
				}
			}

			entry.append(match);
		}

		entry.append('\n');
		return entry.toString();
	}

	public void write(TinyEntry inputA, TinyEntry inputB, String index, String c, BufferedWriter writer, List<String> totalIndexList, int indent) throws IOException {
		TinyEntry classA = inputA != null ? inputA.getChild(index, c) : null;
		TinyEntry classB = inputB != null ? inputB.getChild(index, c) : null;

		/* for (int i = 0; i <= indent; i++)
			System.out.print("-");
		System.out.println(" " + c + " " + (classA != null ? "Y" : "N") + " " + (classB != null ? "Y" : "N")); */

		if ((classA == null || classA.names.size() == 0) && (classB == null || classB.names.size() == 0)) {
			System.out.println("Warning: empty!");
			return;
		}

		writer.write(getEntry(classA, classB, totalIndexList));

		Set<String> subKeys = new TreeSet<>();
		if (classA != null) subKeys.addAll(classA.getChildRow(index).keySet());
		if (classB != null) subKeys.addAll(classB.getChildRow(index).keySet());
		for (String cc : subKeys) {
			write(classA, classB, index, cc, writer, totalIndexList, indent + 1);
		}
	}

	public void run(File inputAf, File inputBf, File outputf, String... mappingBlankFillOrderValues) throws IOException {
		for (String s : mappingBlankFillOrderValues) {
			if (!this.mappingBlankFillOrder.contains(s)) {
				this.mappingBlankFillOrder.add(s);
			}
		}

		System.out.println("Reading " + inputAf.getName());
		inputA = new TinyFile(inputAf);

		System.out.println("Reading " + inputBf.getName());
		inputB = new TinyFile(inputBf);

		System.out.println("Processing...");
		try (BufferedWriter writer = Files.newBufferedWriter(outputf.toPath(), Charset.forName("UTF-8"))) {
			if (!inputA.indexList[0].equals(inputB.indexList[0])) {
				throw new RuntimeException("TODO");
			}

			sharedIndexName = inputA.indexList[0];

			// Set<String> matchedIndexes = Sets.intersection(inputA.indexMap.keySet(), inputB.indexMap.keySet());
			Set<String> matchedIndexes = Collections.singleton(inputA.indexList[0]);
			List<String> totalIndexList = new ArrayList<>(Arrays.asList(inputA.indexList));
			for (String s : inputB.indexList) {
				if (!totalIndexList.contains(s)) {
					totalIndexList.add(s);
				}
			}
			int totalIndexCount = totalIndexList.size();

			// emit header
			StringBuilder header = new StringBuilder();
			header.append("v1");
			for (String s : totalIndexList) {
				header.append('\t');
				header.append(s);
			}
			writer.write(header.append('\n').toString());

			// collect classes
			String index = inputA.indexList[0];
			Set<String> classKeys = new TreeSet<>();
			classKeys.addAll(inputA.root.getChildRow(index).keySet());
			classKeys.addAll(inputB.root.getChildRow(index).keySet());

			// emit entries
			for (String c : classKeys) {
				write(inputA.root, inputB.root, index, c, writer, totalIndexList, 0);
			}
		}
		System.out.println("Done!");
	}

	@Override
	public void run(String[] args) throws Exception {
		File inputAf = new File(args[0]);
		File inputBf = new File(args[1]);
		File outputf = new File(args[2]);

		String[] mbforder = new String[args.length - 3];
		for (int i = 3; i < args.length; i++) {
			mbforder[i - 3] = args[i];
		}

		run(inputAf, inputBf, outputf, mbforder);
	}
}
