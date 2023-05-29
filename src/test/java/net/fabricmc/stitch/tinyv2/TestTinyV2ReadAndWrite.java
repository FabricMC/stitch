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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.fabricmc.stitch.commands.tinyv2.TinyFile;
import net.fabricmc.stitch.commands.tinyv2.TinyV2Reader;
import net.fabricmc.stitch.commands.tinyv2.TinyV2Writer;

public class TestTinyV2ReadAndWrite {
	private static final String DIR = new File(TestTinyV2ReadAndWrite.class.getClassLoader().getResource("sorted").getPath()).getAbsolutePath() + "/";

	private void tryToReadAndWrite(String path) throws IOException {
		Path intMappings = Paths.get(path);
		TinyFile tinyFile = TinyV2Reader.read(intMappings);

		Path tempLocation = Paths.get(path + ".temp");
		// tempLocation.toFile().deleteOnExit();
		TinyV2Writer.write(tinyFile, tempLocation);
		String originalIntMappings = new String(Files.readAllBytes(intMappings));
		String writtenIntMappings = new String(Files.readAllBytes(tempLocation));

		// Ensure the file has not changed
		Assertions.assertEquals(originalIntMappings.replace("\r\n", "\n"), writtenIntMappings.replace("\r\n", "\n"));
	}

	@Test
	public void ReadingAndWritingAV2FileLeavesItUnchanged() throws IOException {
		tryToReadAndWrite(DIR + "intermediary-mappings.tinyv2");
		tryToReadAndWrite(DIR + "yarn-mappings.tinyv2");
		tryToReadAndWrite(DIR + "merged-proposed.tinyv2");
		tryToReadAndWrite(DIR + "test-skip");
	}
}
