package net.fabricmc.stitch.tinyv2;

import org.junit.jupiter.api.Test;

public class Snapshot37a {
    private static final String DIRECTORY = "src/test/resources/snapshot-37a/";

    @Test
    public void testMerge() throws Exception {
        Commands.merge(DIRECTORY + "intermediate-mappings-inverted-stitch.tinyv2",
                DIRECTORY + "yarn-mappings-stitch.tinyv2",
                DIRECTORY + "merged-unordered.tinyv2"
        );
    }

    @Test
    public void testReorder2() throws Exception {
        Commands.reorder(DIRECTORY + "intermediate-mappings-stitch.tinyv2",
                DIRECTORY + "intermediate-mappings-inverted-stitch.tinyv2",
                "intermediary", "official"
        );
    }

    @Test
    public void testReorder3() throws Exception {
        Commands.reorder(DIRECTORY + "merged-unordered.tinyv2",
                DIRECTORY + "merged.tinyv2",
                "official", "intermediary", "named"
        );
    }

    @Test
    public void testFieldNameProposal() throws Exception {
        Commands.proposeFieldNames("local/19w37a-merged.jar",
                DIRECTORY + "merged.tinyv2", DIRECTORY + "merged-proposed.tinyv2");
    }
}
