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

import java.io.*;

public class CommandGenerateIntermediary extends Command {
    public CommandGenerateIntermediary() {
        super("generateIntermediary");
    }

    @Override
    public String getHelpString() {
        return "<input-jar> <mapping-name>";
    }

    @Override
    public boolean isArgumentCountValid(int count) {
        return count == 2;
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

        System.err.println("Generating new mappings...");
        GenState state = new GenState();
        state.generate(new File(args[1]), jarEntry, null);
        System.err.println("Done!");
    }
}
