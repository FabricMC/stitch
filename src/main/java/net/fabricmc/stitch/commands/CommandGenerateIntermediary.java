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
import net.fabricmc.stitch.representation.*;

import java.io.*;
import java.util.Locale;

public class CommandGenerateIntermediary extends Command {
    public CommandGenerateIntermediary() {
        super("generateIntermediary");
    }

    @Override
    public String getHelpString() {
        return "<input-jar> <mapping-name> [-t|--target-namespace <namespace>] [-p|--obfuscation-class-pattern <regex pattern>]... [-f|--obfuscation-field-pattern <regex pattern>]... [-m|--obfuscation-method-pattern <regex pattern>]...";
    }

    @Override
    public boolean isArgumentCountValid(int count) {
        return count >= 2;
    }

    @Override
    public void run(String[] args) throws Exception {
        File file = new File(args[0]);
        JarRootEntry jarEntry = new JarRootEntry(file);
        try {
            JarReader reader = new JarReader(jarEntry);
            reader.apply();
        } catch (IOException e) {
            e.printStackTrace();
        }

        GenState state = new GenState();
        boolean clearedClassPatterns = false;
        boolean clearedFieldPatterns = false;
        boolean clearedMethodPatterns = false;

        for (int i = 2; i < args.length; i++) {
            switch (args[i].toLowerCase(Locale.ROOT)) {
                case "-t":
                case "--target-namespace":
                    state.setTargetNamespace(args[i + 1]);
                    i++;
                    break;
                case "-p":
                case "--obfuscation-pattern":
                case "--obfuscated-class-pattern":
                    if (!clearedClassPatterns)
                        state.clearObfuscatedClassPatterns();
                    clearedClassPatterns = true;

                    state.addObfuscatedClassPattern(args[i + 1]);
                    i++;
                    break;
                case "-f":
                case "--obfuscation-field-pattern":
                    if (!clearedFieldPatterns)
                        state.clearObfuscatedFieldPatterns();
                    clearedFieldPatterns = true;

                    state.addObfuscatedFieldPattern(args[i + 1]);
                    i++;
                    break;
                case "-m":
                case "--obfuscation-method-pattern":
                    if (!clearedMethodPatterns)
                        state.clearObfuscatedMethodPatterns();
                    clearedMethodPatterns = true;

                    state.addObfuscatedMethodPattern(args[i + 1]);
                    i++;
                    break;
            }
        }

        System.err.println("Generating new mappings...");
        state.generate(new File(args[1]), jarEntry, null);
        System.err.println("Done!");
    }
}
