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

package net.fabricmc.stitch;

import net.fabricmc.stitch.commands.*;
import net.fabricmc.stitch.commands.tinyv2.CommandMergeTinyV2;
import net.fabricmc.stitch.commands.tinyv2.CommandProposeV2FieldNames;
import net.fabricmc.stitch.commands.tinyv2.CommandReorderTinyV2;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class Main {
    private static final Map<String, Command> COMMAND_MAP = new TreeMap<>();

    public static void addCommand(Command command) {
        COMMAND_MAP.put(command.name.toLowerCase(Locale.ROOT), command);
    }

    static {
        addCommand(new CommandAsmTrace());
        addCommand(new CommandGenerateIntermediary());
        addCommand(new CommandGeneratePrefixRemapper());
        addCommand(new CommandMatcherToTiny());
        addCommand(new CommandMergeJar());
        addCommand(new CommandMergeTiny());
        addCommand(new CommandProposeFieldNames());
        addCommand(new CommandReorderTiny());
        addCommand(new CommandRewriteIntermediary());
        addCommand(new CommandUpdateIntermediary());
        addCommand(new CommandReorderTinyV2());
        addCommand(new CommandMergeTinyV2());
        addCommand(new CommandProposeV2FieldNames());
        addCommand(new CommandValidateRecords());
    }

    public static void main(String[] args) {
        if (args.length == 0
                || !COMMAND_MAP.containsKey(args[0].toLowerCase(Locale.ROOT))
                || !COMMAND_MAP.get(args[0].toLowerCase(Locale.ROOT)).isArgumentCountValid(args.length - 1)) {
            if (args.length > 0) {
                System.out.println("Invalid command: " + args[0]);
            }
            System.out.println("Available commands:");
            for (Command command : COMMAND_MAP.values()) {
                System.out.println("\t" + command.name + " " + command.getHelpString());
            }
            System.out.println();
            return;
        }

        try {
            String[] argsCommand = new String[args.length - 1];
            if (args.length > 1) {
                System.arraycopy(args, 1, argsCommand, 0, argsCommand.length);
            }
            COMMAND_MAP.get(args[0].toLowerCase(Locale.ROOT)).run(argsCommand);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
