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
import net.fabricmc.mappings.Mappings;
import net.fabricmc.mappings.MappingsProvider;
import net.fabricmc.stitch.Command;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Comparator;

import cuchaz.enigma.command.InvertMappingsCommand;

public class CommandReorderTinyV2 extends Command {
    public CommandReorderTinyV2() {
        super("reorderTinyV2");
    }

    @Override
    public String getHelpString() {
        return "<old-mapping-file> <new-mapping-file> [new name order...]";
    }

    @Override
    public boolean isArgumentCountValid(int count) {
        return count >= 4;
    }

    private int compareTriples(EntryTriple a, EntryTriple b) {
        int c = a.getOwner().compareTo(b.getOwner());
        if (c == 0) {
            c = a.getDesc().compareTo(b.getDesc());
            if (c == 0) {
                c = a.getName().compareTo(b.getName());
            }
        }
        return c;
    }

    @Override
    public void run(String[] args) throws Exception {
        String[] enigmaArgs = {
                "tinyv2",
                args[0],
                String.format("tinyv2:%s:%s",args[2],args[3]),
                args[1]
        };
        new InvertMappingsCommand().run(enigmaArgs);
    }

}
