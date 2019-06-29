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
import net.fabricmc.stitch.representation.JarRootEntry;
import net.fabricmc.stitch.representation.JarReader;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class CommandRewriteIntermediary extends Command {
    public CommandRewriteIntermediary() {
        super("rewriteIntermediary");
    }

    @Override
    public String getHelpString() {
        return "<jar> <old-mapping-file> <new-mapping-file> [--writeAll]";
    }

    @Override
    public boolean isArgumentCountValid(int count) {
        return count >= 3;
    }

    @Override
    public void run(String[] args) throws Exception {
        File fileOld = new File(args[0]);
        JarRootEntry jarOld = new JarRootEntry(fileOld);
        try {
            JarReader reader = new JarReader(jarOld);
            reader.apply();
        } catch (IOException e) {
            e.printStackTrace();
        }

        GenState state = new GenState();

        for (int i = 3; i < args.length; i++) {
            switch (args[i].toLowerCase(Locale.ROOT)) {
                case "--writeall":
                    state.setWriteAll(true);
                    break;
            }
        }

        System.err.println("Loading mapping file...");
        state.prepareRewrite(new File(args[1]));

        File outFile = new File(args[2]);
        if (outFile.exists()) {
            outFile.delete();
        }

        System.err.println("Rewriting mappings...");
        state.generate(outFile, jarOld, jarOld);
        System.err.println("Done!");
    }

}
