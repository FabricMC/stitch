package net.fabricmc.stitch.tinyv1;

import net.fabricmc.stitch.commands.CommandProposeFieldNames;
import net.fabricmc.stitch.commands.CommandReorderTiny;

import org.junit.jupiter.api.Test;

public class Commands {
    @Test
    public void testOrdering() throws Exception {
        String[] args = {
                "C:\\Users\\natan\\Desktop\\stitch\\local\\unordered-merged-mappings.tiny",
                "C:\\Users\\natan\\Desktop\\stitch\\local\\merged-mappings.tiny",
                "official", "intermediary", "named"
        };

        new CommandReorderTiny().run(args);
    }

    @Test
    public void testProposing() throws Exception {
        String[] args = {
                "C:\\Users\\natan\\Desktop\\stitch\\local\\19w37a-merged.jar",
                "C:\\Users\\natan\\Desktop\\stitch\\local\\merged-mappings.tiny",
                "C:\\Users\\natan\\Desktop\\stitch\\local\\merged-mappings-proposed.tiny"
        };

        new CommandProposeFieldNames().run(args);
    }
}
