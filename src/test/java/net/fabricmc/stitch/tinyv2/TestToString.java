/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
