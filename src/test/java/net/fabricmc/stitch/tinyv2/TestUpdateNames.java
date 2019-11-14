package net.fabricmc.stitch.tinyv2;

import net.fabricmc.stitch.commands.tinyv2.CommandUpdateNames;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestUpdateNames {
	private static final String DIR = new File(TestUpdateNames.class.getClassLoader().getResource("updatemappings").getPath()).getAbsolutePath();

	@Test
	public void testWithReplace() throws IOException {
		Path expected = Paths.get(DIR, "updated-replace.tiny");
		Path actual = Files.createTempFile("actual-updated-replace", ".tiny");
		new CommandUpdateNames().run(
				Paths.get(DIR, "old.tiny"),
				Paths.get(DIR, "new.tiny"),
				actual,
				true
		);

		Assertions.assertEquals(new String(Files.readAllBytes(expected)), new String(Files.readAllBytes(actual)));
	}

	@Test
	public void testWithoutReplace() throws IOException {
		Path expected = Paths.get(DIR, "updated-no-replace.tiny");
		Path actual = Files.createTempFile("actual-updated-no-replace", ".tiny");
		new CommandUpdateNames().run(
				Paths.get(DIR, "old.tiny"),
				Paths.get(DIR, "new.tiny"),
				actual,
				false
		);
		Assertions.assertEquals(new String(Files.readAllBytes(expected)), new String(Files.readAllBytes(actual)));
	}
}
