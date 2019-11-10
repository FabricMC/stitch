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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import net.fabricmc.mappings.EntryTriple;
import net.fabricmc.stitch.Command;
import net.fabricmc.stitch.util.FieldNameFinder;

import javax.annotation.Nullable;

/**
 * Java stores the names of enums in the bytecode, and obfuscation doesn't get rid of it. We can use this for easy mappings.
 * This command adds all of the field mappings that FieldNameFinder finds (it overwrites existing mappings for those names).
 * This gets called as the last step after merging and reordering in loom.
 */
public class CommandProposeV2FieldNames extends Command {
	public CommandProposeV2FieldNames() {
		super("proposeV2FieldNames");
	}

	/**
	 * <input jar> is any Minecraft jar, and <input mappings> are mappings of that jar (the same version).
	 * <input mappings> with the additional field names will be written to <output mappings>.+
	 * Assumes the input mappings are intermediary->yarn mappings!
	 * <should replace> is a boolean ("true" or "false") deciding if existing yarn names should be replaced by the generated names.
	 */
	@Override
	public String getHelpString() {
		return "<input jar> <input mappings> <output mappings> <should replace>";
	}

	@Override
	public boolean isArgumentCountValid(int count) {
		return count == 4;
	}

	private Map<EntryTriple, TinyField> generatedNamesOfClass(TinyClass tinyClass) {
		return tinyClass.getFields().stream().collect(Collectors.toMap(
				(TinyField field) -> new EntryTriple(tinyClass.getClassNames().get(0), field.getFieldNames().get(0), field.getFieldDescriptorInFirstNamespace())
				, field -> field));
	}

	@Override
	public void run(String[] args) throws Exception {
		File inputJar = new File(args[0]);
		Path inputMappings = Paths.get(args[1]);
		Path outputMappings = Paths.get(args[2]);
		Boolean shouldReplace = parseBooleanOrNull(args[3]);

		// Validation
		if(!inputJar.exists()) throw new IllegalArgumentException("Cannot find input jar at " + inputJar);
		if(!Files.exists(inputMappings)) throw new IllegalArgumentException("Cannot find input mappings at " + inputMappings);
		if(Files.exists(outputMappings)) System.out.println("Warning: existing file will be replaced by output mappings");
		if(shouldReplace == null) throw new IllegalArgumentException("<should replace> must be 'true' or 'false'");

		// entrytriple from the input jar namespace
		Map<EntryTriple, String> generatedFieldNames = new FieldNameFinder().findNames(new File(args[0]));
		System.err.println("Found " + generatedFieldNames.size() + " interesting names.");

		TinyFile tinyFile = TinyV2Reader.read(Paths.get(args[1]));
		Map<EntryTriple, TinyField> fieldsMap = new HashMap<>();
		tinyFile.getClassEntries().stream().map(this::generatedNamesOfClass).forEach(map -> map.forEach(fieldsMap::put));
		Map<String, TinyClass> classMap = tinyFile.mapClassesByFirstNamespace();

		int replaceCount = 0;
		for (Map.Entry<EntryTriple, String> entry : generatedFieldNames.entrySet()) {
			EntryTriple key = entry.getKey();
			String newName = entry.getValue();
			TinyField field = fieldsMap.get(key);
			// If the field name exists, replace the name with the auto-generated name, as long as <should replace> is true.
			if (field != null) {
				if (shouldReplace) {
					field.getFieldNames().set(1, newName);
					replaceCount++;
				}
			} else {
				TinyClass tinyClass = classMap.get(key.getOwner());
				// If field name does not exist, but its class does exist, create a new mapping with the supplied generated name.
				if (tinyClass != null) {
					tinyClass.getFields().add(new TinyField(key.getDesc(), Lists.newArrayList(key.getName(), newName), Lists.newArrayList()));
					replaceCount++;
				}
			}

		}

		System.err.println("Replaced " + replaceCount + " names in the mappings.");

		Path newMappingsLocation = Paths.get(args[2]);

		TinyV2Writer.write(tinyFile, newMappingsLocation);
	}

	@Nullable
	private Boolean parseBooleanOrNull(String booleanLiteral) {
		String lowerCase = booleanLiteral.toLowerCase();
		if(lowerCase.equals("true")) return Boolean.TRUE;
		else if(lowerCase.equals("false")) return Boolean.FALSE;
		else return null;
	}
}
