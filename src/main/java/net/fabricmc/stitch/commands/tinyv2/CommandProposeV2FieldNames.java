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

import net.fabricmc.mappings.EntryTriple;
import net.fabricmc.stitch.Command;
import net.fabricmc.stitch.util.FieldNameFinder;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

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
     */
    @Override
    public String getHelpString() {
        return "<input jar> <input mappings> <output mappings>";
    }

    @Override
    public boolean isArgumentCountValid(int count) {
        return count == 3;
    }

    @Override
    public void run(String[] args) throws Exception {
        // entrytriple from the input jar namespace
        Map<EntryTriple, String> fieldNames = new FieldNameFinder().findNames(new File(args[0]));
        System.err.println("Found " + fieldNames.size() + " interesting names.");

        TinyFile tinyFile = TinyV2Reader.read(Paths.get(args[1]));
        Path newMappingsLocation = Paths.get(args[2]);
        int namedIndex = tinyFile.getHeader().getNamespaces().indexOf("named");
        if (namedIndex == -1) {
            throw new IllegalArgumentException("The tiny mappings don't have a 'named' namespace.");
        }
        int replaceCount = 0;
        for (TinyClass tinyClass : tinyFile.getClassEntries()) {
            for (TinyField field : tinyClass.getFields()) {
                EntryTriple key = new EntryTriple(tinyClass.getClassNames().get(0), field.getFieldNames().get(0),
                        field.getFieldDescriptorInFirstNamespace());
                String suggestedName = fieldNames.get(key);
                if (suggestedName != null) {
                    replaceCount++;
                    field.getFieldNames().set(namedIndex, suggestedName);
                }
            }
        }
        System.err.println("Replaced " + replaceCount + " names in the mappings.");

        TinyV2Writer.write(tinyFile, newMappingsLocation);
    }
}
