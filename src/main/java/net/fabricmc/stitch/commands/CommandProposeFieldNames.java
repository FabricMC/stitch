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

import net.fabricmc.mappings.EntryTriple;
import net.fabricmc.mappings.FieldEntry;
import net.fabricmc.mappings.Mappings;
import net.fabricmc.mappings.MappingsProvider;
import net.fabricmc.stitch.Command;
import net.fabricmc.stitch.util.FieldNameFinder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

public class CommandProposeFieldNames extends Command {
    public CommandProposeFieldNames() {
        super("proposeFieldNames");
    }

    @Override
    public String getHelpString() {
		return "<input jar> <input mappings> <output mappings> [<input jar namespace>] [<output namespace>]";
    }

    @Override
    public boolean isArgumentCountValid(int count) {
		return count >= 3 && count <= 5;
    }

    @Override
    public void run(String[] args) throws Exception {
        // entrytriple from the input jar namespace
        Map<EntryTriple, String> fieldNamesO = new FieldNameFinder().findNames(new File(args[0]));

        System.err.println("Found " + fieldNamesO.size() + " interesting names.");

        // i didn't fuss too much on this... this needs a rewrite once we get a mapping writer library
        // entrytriple from the first column namespace
        Map<EntryTriple, String> fieldNames = new HashMap<>();

        Mappings mappings;
        try (FileInputStream fileIn = new FileInputStream(new File(args[1]))) {
            mappings = MappingsProvider.readTinyMappings(fileIn, false);
        }

		// the namespace used by the input jar
		String inputNamespace = args.length > 3 ? args[3] : "official";
		// the namespace written to; write to input namespace if not specified
		String outputNamespace = args.length > 4 ? args[4] : args.length == 4 ? inputNamespace : "named";

		int replaceCount = 0;
        try (FileInputStream fileIn = new FileInputStream(new File(args[1]));
            FileOutputStream fileOut = new FileOutputStream(new File(args[2]));
            InputStreamReader fileInReader = new InputStreamReader(fileIn);
            OutputStreamWriter fileOutWriter = new OutputStreamWriter(fileOut);
            BufferedReader reader = new BufferedReader(fileInReader);
            BufferedWriter writer = new BufferedWriter(fileOutWriter)) {

            int headerPos = -1;

            String line;
            while ((line = reader.readLine()) != null) {
                String[] tabSplit = line.split("\t");

                if (headerPos < 0) {
                    // first line
                    if (tabSplit.length < 3) {
                        throw new RuntimeException("Invalid mapping file!");
                    }

                    for (int i = 2; i < tabSplit.length; i++) {
						if (tabSplit[i].equals(outputNamespace)) {
                            headerPos = i;
                            break;
                        }
                    }

                    if (headerPos < 0) {
						throw new RuntimeException("Could not find mapping position for output namespace '" + outputNamespace + "'!");
                    }

					if (!tabSplit[1].equals(inputNamespace)) {
                        for (FieldEntry e : mappings.getFieldEntries()) {
							EntryTriple inputMapping = e.get(inputNamespace);
							String name = fieldNamesO.get(inputMapping);
                            if (name != null) {
                                fieldNames.put(e.get(tabSplit[1]), name);
                            }
                        }
                    } else {
                        fieldNames = fieldNamesO;
                    }

                    mappings = null; // save memory
                } else {
                    // second+ line
                    if (tabSplit[0].equals("FIELD")) {
                        EntryTriple key = new EntryTriple(tabSplit[1], tabSplit[3], tabSplit[2]);
						if (fieldNames.containsKey(key)) {
                            tabSplit[headerPos + 2] = fieldNames.get(key);

                            StringBuilder builder = new StringBuilder(tabSplit[0]);
                            for (int i = 1; i < tabSplit.length; i++) {
                                builder.append('\t');
                                builder.append(tabSplit[i]);
                            }
                            line = builder.toString();

                            replaceCount++;
                        }
                    }
                }

                if (!line.endsWith("\n")) {
                    line = line + "\n";
                }

                writer.write(line);
            }
        }

        System.err.println("Replaced " + replaceCount + " names in the mappings.");
    }
}
