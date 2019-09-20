package net.fabricmc.stitch.tinyv2;

import org.junit.jupiter.api.Test;

public class Stable1_14_4 {
    private static final String DIRECTORY = "src/test/resources/stable-1.14.4/";

    @Test
    public void testReorder2() throws Exception {
        Commands.reorder(DIRECTORY + "intermediary-mappings.tinyv2",
                DIRECTORY + "intermediary-mappings-inverted.tinyv2",
                "intermediary", "official"
        );
    }

    @Test
    public void testMerge() throws Exception {
        Commands.merge(DIRECTORY + "intermediary-mappings-inverted.tinyv2",
                DIRECTORY + "yarn-mappings.tinyv2",
                DIRECTORY + "merged-unordered.tinyv2"
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

    // Requirements:
    // - Official -> Intermediary mappings "intermediary-mappings.tinyv2" from matcher
    // - Intermediary -> Named mappings "yarn-mappings.tinyv2" from yarn
    @Test
    public void testFullProcess() throws Exception{
        testReorder2();
        testMerge();
        testReorder3();
        testFieldNameProposal();
    }
}
