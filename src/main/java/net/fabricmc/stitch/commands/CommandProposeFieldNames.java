/*
 * Copyright (c) 2016, 2017, 2018 Adrian Siekierka
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
import net.fabricmc.stitch.representation.JarReader;
import net.fabricmc.stitch.util.FieldNameFinder;
import net.fabricmc.stitch.util.StitchUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class CommandProposeFieldNames extends Command {
    public CommandProposeFieldNames() {
        super("proposeFieldNames");
    }

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
        Map<String, String> fieldNames = new FieldNameFinder().find(new File(args[0]));

        System.err.println("Found " + fieldNames.size() + " interesting names.");

        try (FileInputStream fileIn = new FileInputStream(new File(args[1]))) {
            try (FileOutputStream fileOut = new FileOutputStream(new File(args[2]))) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(fileIn));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fileOut));

                int headerPos = -1;

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] tabSplit = line.split("\t");

                    if (headerPos < 0) {
                        // first line
                        if (tabSplit.length < 3 || !(tabSplit[1].equals("official"))) {
                            throw new RuntimeException("Invalid mapping file!");
                        }

                        for (int i = 2; i < tabSplit.length; i++) {
                            if (tabSplit[i].equals("named")) {
                                headerPos = i;
                                break;
                            }
                        }

                        if (headerPos < 0) {
                            throw new RuntimeException("Could not find 'named' mapping position!");
                        }
                    } else {
                        // second+ line
                        if (tabSplit[0].equals("FIELD")) {
                            String key = tabSplit[1] + ";;" + tabSplit[3];
                            String value = tabSplit[headerPos + 2];
                            if (value.startsWith("field_") && fieldNames.containsKey(key)) {
                                tabSplit[headerPos + 2] = fieldNames.get(key);

                                StringBuilder builder = new StringBuilder(tabSplit[0]);
                                for (int i = 1; i < tabSplit.length; i++) {
                                    builder.append('\t');
                                    builder.append(tabSplit[i]);
                                }
                                line = builder.toString();
                            }
                        }
                    }

                    if (!line.endsWith("\n")) {
                        line = line + "\n";
                    }

                    writer.write(line);
                }

                reader.close();
                writer.close();
            }
        }
    }
}
