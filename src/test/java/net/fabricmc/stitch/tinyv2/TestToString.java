package net.fabricmc.stitch.tinyv2;

import net.fabricmc.stitch.commands.tinyv2.TinyClass;
import net.fabricmc.stitch.commands.tinyv2.TinyField;
import net.fabricmc.stitch.commands.tinyv2.TinyMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

public class TestToString {

	@Test
	public void testTinyClassToString() {
		TinyClass tinyClass = new TinyClass(
				Arrays.asList("name1", "name2", "name3"),
				Arrays.asList(new TinyMethod("",
						Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),Collections.emptyList())),
				Arrays.asList(new TinyField("",Collections.emptyList(),Collections.emptyList())),
				Arrays.asList("Asdf")
		);

		String expected = "TinyClass(names = [name1, name2, name3], 1 methods, 1 fields, 1 comments)";

		Assertions.assertEquals(expected,tinyClass.toString());
	}

}
