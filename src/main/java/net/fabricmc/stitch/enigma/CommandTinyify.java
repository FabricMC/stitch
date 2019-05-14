/*
 * Copyright (c) 2016, 2017, 2018 FabricMC
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

package net.fabricmc.stitch.enigma;

import com.google.common.collect.Lists;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.ParsedJar;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.MappingsChecker;
import cuchaz.enigma.translation.mapping.VoidEntryResolver;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingsWriter;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import net.fabricmc.stitch.Command;
import net.fabricmc.stitch.util.StitchUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

public class CommandTinyify extends Command {

	public CommandTinyify() {
		super("tinyify");
	}

	@Override
	public String getHelpString() {
		return "<input-jar> <enigma-mappings> <output-tiny> [name-obf] [name-deobf]";
	}

	@Override
	public boolean isArgumentCountValid(int count) {
		return count >= 3 && count <= 5;
	}

	@Override
	public void run(String[] args) throws Exception {
		File injf = new File(args[0]);
		File inf = new File(args[1]);
		File outf = new File(args[2]);
		String nameObf = args.length > 3 ? args[3] : "official";
		String nameDeobf = args.length > 4 ? args[4] : "named";

		if (!injf.exists() || !injf.isFile()) {
			throw new FileNotFoundException("Input JAR could not be found!");
		}

		if (!inf.exists()) {
			throw new FileNotFoundException("Enigma mappings could not be found!");
		}

		System.out.println("Reading JAR file...");

		JarIndex index = JarIndex.empty();
		index.indexJar(new ParsedJar(new JarFile(injf)), s -> {
		});

		System.out.println("Reading Enigma mappings...");
		MappingFormat format = inf.isDirectory() ? MappingFormat.ENIGMA_DIRECTORY : MappingFormat.ENIGMA_FILE;
		EntryTree<EntryMapping> mappings = format.read(inf.toPath(), ProgressListener.VOID);

		MappingsChecker checker = new MappingsChecker(index, mappings);
		checker.dropBrokenMappings(ProgressListener.VOID);

		System.out.println("Writing Tiny mappings...");

		MappingsWriter writer = new TinyMappingsWriter(nameObf, nameDeobf);
		writer.write(mappings, MappingDelta.added(mappings), outf.toPath(), ProgressListener.VOID);
	}

	private static class TinyMappingsWriter implements MappingsWriter {

		private static final String VERSION_CONSTANT = "v1";

		// HACK: as of enigma 0.13.1, some fields seem to appear duplicated?
		private final Set<String> writtenLines = new HashSet<>();
		private final String nameObf;
		private final String nameDeobf;

		private TinyMappingsWriter(String nameObf, String nameDeobf) {
			this.nameObf = nameObf;
			this.nameDeobf = nameDeobf;
		}

		@Override
		public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress) {
			try {
				Files.deleteIfExists(path);
				Files.createFile(path);
			} catch (IOException e) {
				e.printStackTrace();
			}

			try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				writeLine(writer, new String[]{VERSION_CONSTANT, nameObf, nameDeobf});

				Lists.newArrayList(mappings).stream()
						.map(EntryTreeNode::getEntry).sorted(Comparator.comparing(Object::toString))
						.forEach(entry -> writeEntry(writer, mappings, entry));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void writeEntry(Writer writer, EntryTree<EntryMapping> mappings, Entry<?> entry) {
			EntryTreeNode<EntryMapping> node = mappings.findNode(entry);
			if (node == null) {
				return;
			}

			Translator translator = new MappingTranslator(mappings, VoidEntryResolver.INSTANCE);

			EntryMapping mapping = mappings.get(entry);
			if (mapping != null && !entry.getName().equals(mapping.getTargetName())) {
				if (entry instanceof ClassEntry) {
					writeClass(writer, (ClassEntry) entry, translator);
				} else if (entry instanceof FieldEntry) {
					writeLine(writer, EnigmaUtil.serializeEntry(entry, true, mapping.getTargetName()));
				} else if (entry instanceof MethodEntry) {
					writeLine(writer, EnigmaUtil.serializeEntry(entry, true, mapping.getTargetName()));
				}
			}

			writeChildren(writer, mappings, node);
		}

		private void writeChildren(Writer writer, EntryTree<EntryMapping> mappings, EntryTreeNode<EntryMapping> node) {
			node.getChildren().stream()
					.filter(e -> e instanceof FieldEntry).sorted()
					.forEach(child -> writeEntry(writer, mappings, child));

			node.getChildren().stream()
					.filter(e -> e instanceof MethodEntry).sorted()
					.forEach(child -> writeEntry(writer, mappings, child));

			node.getChildren().stream()
					.filter(e -> e instanceof ClassEntry).sorted()
					.forEach(child -> writeEntry(writer, mappings, child));
		}

		private void writeClass(Writer writer, ClassEntry entry, Translator translator) {
			ClassEntry translatedEntry = translator.translate(entry);

			String obfClassName = StitchUtil.NONE_PREFIX_REMOVER.map(entry.getFullName());
			String deobfClassName = StitchUtil.NONE_PREFIX_REMOVER.map(translatedEntry.getFullName());
			writeLine(writer, new String[]{"CLASS", obfClassName, deobfClassName});
		}

		private void writeLine(Writer writer, String[] data) {
			try {
				String line = StitchUtil.TAB_JOINER.join(data) + "\n";
				if (writtenLines.add(line)) {
					writer.write(line);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
