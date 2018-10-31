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
import net.fabricmc.stitch.representation.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class CommandUpdate extends Command {
    public CommandUpdate() {
        super("updateIntermediary");
    }

    @Override
    public String getHelpString() {
        return "<old-jar> <new-jar> <old-mapping-file> <new-mapping-file> <match-file>";
    }

    @Override
    public boolean isArgumentCountValid(int count) {
        return count == 5;
    }

    @Override
    public void run(String[] args) throws Exception {
        File fileOld = new File(args[0]);
        JarEntry jarOld = new JarEntry(fileOld);
        try {
            JarReader reader = new JarReader(jarOld);
            reader.apply();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File fileNew = new File(args[1]);
        JarEntry jarNew = new JarEntry(fileNew);
        try {
            JarReader reader = new JarReader(jarNew);
            reader.apply();
        } catch (IOException e) {
            e.printStackTrace();
        }

        GenState state = new GenState();
        System.err.println("Loading remapping files...");
        state.prepareUpdate(new File(args[2]), new File(args[4]));

        System.err.println("Generating new mappings...");
        state.generate(new File(args[3]), jarNew, jarOld);
        System.err.println("Done!");
    }

}
