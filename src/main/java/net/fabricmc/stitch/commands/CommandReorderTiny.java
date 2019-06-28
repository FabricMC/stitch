/*
 * Copyright (c) 2016, 2017, 2018, 2019 Adrian Siekierka
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

import com.google.common.base.Joiner;
import net.fabricmc.mappings.ClassEntry;
import net.fabricmc.mappings.EntryTriple;
import net.fabricmc.mappings.FieldEntry;
import net.fabricmc.mappings.Mappings;
import net.fabricmc.mappings.MappingsProvider;
import net.fabricmc.mappings.MethodEntry;
import net.fabricmc.stitch.Command;
import net.fabricmc.stitch.representation.JarReader;
import net.fabricmc.stitch.representation.JarRootEntry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Locale;

public class CommandReorderTiny extends Command {
    public CommandReorderTiny() {
        super("reorderTiny");
    }

    @Override
    public String getHelpString() {
        return "<old-mapping-file> <new-mapping-file> [name order...]";
    }

    @Override
    public boolean isArgumentCountValid(int count) {
        return count >= 4;
    }

    @Override
    public void run(String[] args) throws Exception {
        File fileOld = new File(args[0]);
        File fileNew = new File(args[1]);
        String[] names = new String[args.length - 2];
        System.arraycopy(args, 2, names, 0, names.length);

        System.err.println("Loading mapping file...");

        Mappings input;
        try (FileInputStream stream = new FileInputStream(fileOld)) {
            input = MappingsProvider.readTinyMappings(stream, false);
        }

        System.err.println("Rewriting mappings...");

        try (FileOutputStream stream = new FileOutputStream(fileNew);
             OutputStreamWriter osw = new OutputStreamWriter(stream);
             BufferedWriter writer = new BufferedWriter(osw)) {

            StringBuilder firstLineBuilder = new StringBuilder("v1");
            for (String name : names) {
                firstLineBuilder.append('\t').append(name);
            }
            writer.write(firstLineBuilder.append('\n').toString());
            for (ClassEntry entry : input.getClassEntries()) {
                StringBuilder s = new StringBuilder("CLASS");
                for (String name : names) {
                    s.append('\t').append(entry.get(name));
                }
                writer.write(s.append('\n').toString());
            }
            for (FieldEntry entry : input.getFieldEntries()) {
                StringBuilder s = new StringBuilder("FIELD");
                EntryTriple first = entry.get(names[0]);
                s.append('\t').append(first.getOwner()).append('\t').append(first.getDesc());
                for (String name : names) {
                    s.append('\t').append(entry.get(name).getName());
                }
                writer.write(s.append('\n').toString());
            }
            for (MethodEntry entry : input.getMethodEntries()) {
                StringBuilder s = new StringBuilder("METHOD");
                EntryTriple first = entry.get(names[0]);
                s.append('\t').append(first.getOwner()).append('\t').append(first.getDesc());
                for (String name : names) {
                    s.append('\t').append(entry.get(name).getName());
                }
                writer.write(s.append('\n').toString());
            }
        }

        System.err.println("Done!");
    }

}
