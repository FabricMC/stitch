package net.fabricmc.stitch.tinyv2;

import net.fabricmc.stitch.commands.tinyv2.CommandMergeTinyV2;
import net.fabricmc.stitch.commands.tinyv2.CommandProposeV2FieldNames;
import net.fabricmc.stitch.commands.tinyv2.CommandReorderTinyV2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Commands {
    public static void reorder(String toInvert, String outputTo, String... newOrder) throws Exception {
        List<String> args = new ArrayList<>();
        args.add(toInvert);
        args.add(outputTo);
        args.addAll(Arrays.asList(newOrder));

        new CommandReorderTinyV2().run(args.toArray(new String[0]));
    }


    public static void merge(String mappingA, String mappingB, String mergedLocation) throws Exception {
        new CommandMergeTinyV2().run(new String[]{mappingA, mappingB, mergedLocation});
    }


    public static void proposeFieldNames(String mergedJar, String mergedTinyFile, String newTinyFile) throws Exception {
        new CommandProposeV2FieldNames().run(new String[]{mergedJar, mergedTinyFile, newTinyFile});
    }


}
