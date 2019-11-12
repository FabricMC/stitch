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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * All lists and collections in this AST are mutable.
 */
public class TinyFile {
	private final TinyHeader header;
	private final Collection<TinyClass> classEntries;

	public TinyFile(TinyHeader header, Collection<TinyClass> classEntries) {
		this.header = header;
		this.classEntries = classEntries;
	}

	/**
	 * The key will be the name of the class in the first namespace, the value is the same as classEntries.
	 * Useful for quickly retrieving a class based on a known name in the first namespace.
	 */
	public Map<String, TinyClass> mapClassesByFirstNamespace() {
		return mapClassesByNamespace(0);
	}

	/**
	 * The key will be the name of the class in the first namespace, the value is the same as classEntries.
	 * Useful for quickly retrieving a class based on a known name in the first namespace.
	 */
	public Map<String, TinyClass> mapClassesByNamespace(int namespaceIndex) {
		return classEntries.stream().collect(Collectors.toMap(c -> c.getClassNames().get(namespaceIndex), c -> c));
	}

	public TinyHeader getHeader() {
		return header;
	}

	public Collection<TinyClass> getClassEntries() {
		return classEntries;
	}
}
