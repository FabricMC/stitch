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
import net.fabricmc.stitch.merge.JarMerger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public class CommandMergeJar extends Command {
    public CommandMergeJar() {
        super("mergeJar");
    }

    @Override
    public String getHelpString() {
        return "<client-jar> <server-jar> <output> [--removeSnowman] [--syntheticparams]";
    }

    @Override
    public boolean isArgumentCountValid(int count) {
        return count >= 3;
    }

    @Override
    public void run(String[] args) throws Exception {
        File in1f = new File(args[0]);
        File in2f = new File(args[1]);
        File outf = new File(args[2]);
        boolean removeSnowman = false, syntheticParams = false;

        for (int i = 3; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                switch (args[i].substring(2).toLowerCase(Locale.ROOT)) {
                    case "removesnowman":
                        removeSnowman = true;
                        break;
                    case "syntheticparams":
                        syntheticParams = true;
                        break;
                }
            }
        }

        if (!in1f.exists() || !in1f.isFile()) {
            throw new FileNotFoundException("Client JAR could not be found!");
        }

        if (!in2f.exists() || !in2f.isFile()) {
            throw new FileNotFoundException("Server JAR could not be found!");
        }

        JarMerger merger = null;
        try {
            merger = new JarMerger(in1f, in2f, outf);
            if (removeSnowman) {
                merger.enableSnowmanRemoval();
            }

            if (syntheticParams) {
                merger.enableSyntheticParamsOffset();
            }

            System.out.println("Merging...");

            merger.merge();

            System.out.println("Merge completed!");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (merger != null) {
                merger.close();
            }
        }
    }
}
