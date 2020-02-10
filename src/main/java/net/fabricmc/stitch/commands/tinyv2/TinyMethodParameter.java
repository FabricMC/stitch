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

public class TinyMethodParameter implements Comparable<TinyMethodParameter>, Mapping {
	private final int lvIndex;
	private final List<String> parameterNames;
	private final Collection<String> comments;

	public TinyMethodParameter(int lvIndex, List<String> parameterNames, Collection<String> comments) {
		this.lvIndex = lvIndex;
		this.parameterNames = parameterNames;
		this.comments = comments;
	}

	@Override
	public String toString() {
		return "TinyMethodParameter{" +
				"lvIndex=" + lvIndex +
				", parameterNames=" + parameterNames +
				", comments=" + comments +
				'}';
	}

	public int getLvIndex() {
		return lvIndex;
	}

	public List<String> getParameterNames() {
		return parameterNames;
	}
	@Override
	public Collection<String> getComments() {
		return comments;
	}

	@Override
	public int compareTo(TinyMethodParameter o) {
		return lvIndex - o.lvIndex;
	}

	@Override
	public List<String> getMapping() {
		return parameterNames;
	}
}
