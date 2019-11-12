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

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TinyHeader {

	private final List<String> namespaces;
	private final int majorVersion;
	private final int minorVersion;
	private final Map<String,/*nullable*/ String> properties;

	public TinyHeader(List<String> namespaces, int majorVersion, int minorVersion, Map<String, String> properties) {
		this.namespaces = namespaces;
		this.majorVersion = majorVersion;
		this.minorVersion = minorVersion;
		this.properties = properties;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TinyHeader that = (TinyHeader) o;
		return majorVersion == that.majorVersion &&
				minorVersion == that.minorVersion &&
				namespaces.equals(that.namespaces) &&
				properties.equals(that.properties);
	}

	@Override
	public int hashCode() {
		return Objects.hash(namespaces, majorVersion, minorVersion, properties);
	}

	public List<String> getNamespaces() {
		return namespaces;
	}

	public int getMajorVersion() {
		return majorVersion;
	}

	public int getMinorVersion() {
		return minorVersion;
	}

	public Map<String, String> getProperties() {
		return properties;
	}
}
