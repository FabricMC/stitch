package net.fabricmc.stitch.tinyv2tests;

import net.fabricmc.stitch.commands.tinyv2.CommandMergeTinyV2;
import net.fabricmc.stitch.commands.tinyv2.CommandReorderTinyV2;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Commands {
    private void testReorder(String toInvert, String outputTo, String... newOrder) throws Exception {
        List<String> args = new ArrayList<>();
        args.add(toInvert);
        args.add(outputTo);
        args.addAll(Arrays.asList(newOrder));

        new CommandReorderTinyV2().run(args.toArray(new String[0]));
    }

    @Test
    public void testReorder2() throws Exception {
        testReorder("src/test/resources/intermediate-mappings-stitch.tinyv2", "src/test/resources/intermediate-mappings-inverted-stitch.tinyv2",
                "intermediary", "official"
        );
    }

    @Test
    public void testReorder3() throws Exception {
        testReorder("src/test/resources/merged-unordered.tinyv2",
                "src/test/resources/merged.tinyv2",
                "official", "intermediary","named"
        );
    }

    private void testMerge(String mappingA, String mappingB, String mergedLocation) throws Exception {
        new CommandMergeTinyV2().run(new String[]{mappingA, mappingB, mergedLocation});
    }

    @Test
    public void testMerge() throws Exception {
        testMerge("src/test/resources/intermediate-mappings-inverted-stitch.tinyv2",
                "src/test/resources/yarn-mappings-stitch.tinyv2",
                "src/test/resources/merged-unordered.tinyv2"
        );
    }
}
