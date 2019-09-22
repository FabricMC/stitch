package net.fabricmc.stitch.tinyv2;

import org.junit.jupiter.api.Test;

import java.io.File;

import cuchaz.enigma.command.ConvertMappingsCommand;

public class Stable1_14_4 {
    private static final String DIR = new File(Stable1_14_4.class.getClassLoader().getResource("stable-1.14.4").getPath()).getAbsolutePath() + "/";

    @Test
    public void testReorder2() throws Exception {
        Commands.reorder(DIR + "intermediary-mappings.tinyv2",
                DIR + "intermediary-mappings-inverted.tinyv2",
                "intermediary", "official"
        );
    }

    @Test
    public void testMerge() throws Exception {
        Commands.merge(DIR + "intermediary-mappings-inverted.tinyv2",
                DIR + "yarn-mappings.tinyv2",
                DIR + "merged-unordered.tinyv2"
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

    @Test
    public void testConversion() throws Exception{
        new ConvertMappingsCommand().run("tiny", DIR + "unmerged-yarn.tinyv1","tinyv2:intermediary:named", DIR + "unmerged-yarn.tinyv2");
    }
}
