package net.fabricmc.stitch.tinyv2;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.fabricmc.stitch.commands.tinyv2.TinyClass;
import net.fabricmc.stitch.commands.tinyv2.TinyField;
import net.fabricmc.stitch.commands.tinyv2.TinyMethod;

public class TestToString {

	@Test
	public void testTinyClassToString() {
		TinyClass tinyClass = new TinyClass(
						Arrays.asList("name1", "name2", "name3"),
						Collections.singletonList(new TinyMethod("",
										Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList())),
						Collections.singletonList(new TinyField("", Collections.emptyList(), Collections.emptyList())),
						Collections.singletonList("Asdf")
		);

		String expected = "TinyClass(names = [name1, name2, name3], 1 methods, 1 fields, 1 comments)";

		Assertions.assertEquals(expected, tinyClass.toString());
	}

}
