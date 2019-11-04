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
import java.nio.file.Files;
import java.nio.file.Paths;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class Snapshot37a {

	private static final String DIR = new File(Snapshot37a.class.getClassLoader().getResource("snapshot-37a").getPath()).getAbsolutePath() + "/";
//
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
		String path = DIR + "merged-unordered.tinyv2";
		if(!Files.exists(Paths.get(path))) testMerge();
		Commands.reorder(path,
						DIR + "merged.tinyv2",
						"official", "intermediary", "named"
		);
	}

	/** You need the 19w37a-merged.jar file under local/ */
	@Test
	@Disabled
	public void testFieldNameProposal() throws Exception {
		Commands.proposeFieldNames("local/19w37a-merged.jar",
						DIR + "merged.tinyv2", DIR + "merged-proposed.tinyv2");
	}
}
