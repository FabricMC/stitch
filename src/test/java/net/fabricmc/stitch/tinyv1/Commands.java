package net.fabricmc.stitch.tinyv1;

import org.junit.jupiter.api.Test;

import net.fabricmc.stitch.commands.CommandProposeFieldNames;
import net.fabricmc.stitch.commands.CommandReorderTiny;

public class Commands {
	@Test
	public void testOrdering() throws Exception {
		String[] args = {
						"local\\unordered-merged-mappings.tiny",
						"local\\merged-mappings.tiny",
						"official", "intermediary", "named"
		};

		new CommandReorderTiny().run(args);
	}

	@Test
	public void testProposing() throws Exception {
		String[] args = {
						"local\\19w37a-merged.jar",
						"local\\merged-mappings.tiny",
						"local\\merged-mappings-proposed.tiny"
		};

		new CommandProposeFieldNames().run(args);
	}
}
