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

package net.fabricmc.stitch.commands.tinyv2;

import java.util.Collection;
import java.util.List;

public class TinyLocalVariable implements Comparable<TinyLocalVariable>, Mapping {

	private final int lvIndex;
	private final int lvStartOffset;
	/**
	 * Will be -1 when there is no lvt index
	 */
	private final int lvTableIndex;
	private final List<String> localVariableNames;
	private final Collection<String> comments;

	public TinyLocalVariable(int lvIndex, int lvStartOffset, int lvTableIndex, List<String> localVariableNames, Collection<String> comments) {
		this.lvIndex = lvIndex;
		this.lvStartOffset = lvStartOffset;
		this.lvTableIndex = lvTableIndex;
		this.localVariableNames = localVariableNames;
		this.comments = comments;
	}


	public int getLvIndex() {
		return lvIndex;
	}

	public int getLvStartOffset() {
		return lvStartOffset;
	}

	public int getLvTableIndex() {
		return lvTableIndex;
	}

	public List<String> getLocalVariableNames() {
		return localVariableNames;
	}

	public Collection<String> getComments() {
		return comments;
	}

	@Override
	public int compareTo(TinyLocalVariable o) {
		return localVariableNames.get(0).compareTo(o.localVariableNames.get(0));
	}

	@Override
	public List<String> getMapping() {
		return localVariableNames;
	}
}
