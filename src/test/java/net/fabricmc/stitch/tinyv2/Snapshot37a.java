package net.fabricmc.stitch.tinyv2;

import org.junit.jupiter.api.Test;

import java.io.File;

public class Snapshot37a {
    private static final String DIR = new File(Stable1_14_4.class.getClassLoader().getResource("/snapshot-37a").getPath()).getAbsolutePath() + "/";

    @Test
    public void testMerge() throws Exception {
        Commands.merge(DIR + "intermediate-mappings-inverted-stitch.tinyv2",
                DIR + "yarn-mappings-stitch.tinyv2",
                DIR + "merged-unordered.tinyv2"
        );
    }

    @Test
    public void testReorder2() throws Exception {
        Commands.reorder(DIR + "intermediate-mappings-stitch.tinyv2",
                DIR + "intermediate-mappings-inverted-stitch.tinyv2",
                "intermediary", "official"
        );
    }

    @Test
    public void testReorder3() throws Exception {
        Commands.reorder(DIR + "merged-unordered.tinyv2",
                DIR + "merged.tinyv2",
                "official", "intermediary", "named"
        );
    }

    @Test
    public void testFieldNameProposal() throws Exception {
        Commands.proposeFieldNames("local/19w37a-merged.jar",
                DIR + "merged.tinyv2", DIR + "merged-proposed.tinyv2");
    }
}
