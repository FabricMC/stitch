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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.fabricmc.stitch.util.Pair;

public class TinyClass implements Comparable<TinyClass>, Mapping {
	@Override
	public String toString() {
		return "TinyClass(names = [" + String.join(", ", classNames) + "], " + methods.size() + " methods, "
						+ fields.size() + " fields, " + comments.size() + " comments)";
	}

	private final List<String> classNames;
	private final Collection<TinyMethod> methods;
	private final Collection<TinyField> fields;
	private final Collection<String> comments;

	public TinyClass(List<String> classNames, Collection<TinyMethod> methods, Collection<TinyField> fields, Collection<String> comments) {
		this.classNames = classNames;
		this.methods = methods;
		this.fields = fields;
		this.comments = comments;
	}

	public TinyClass(List<String> classNames) {
		this.classNames = classNames;
		this.methods = new ArrayList<>();
		this.fields = new ArrayList<>();
		this.comments = new ArrayList<>();
	}


	/**
	 * Descriptors are also taken into account because methods can overload.
	 * The key format is firstMethodName + descriptor
	 */
	public Map<Pair<String, String>, TinyMethod> mapMethodsByFirstNamespaceAndDescriptor() {
		return methods.stream().collect(Collectors.toMap(m -> Pair.of(m.getMethodNames().get(0), m.getMethodDescriptorInFirstNamespace()), m -> m));
	}

	/**
	 * This will only work well on intermediary methods because they are unique
	 */
	public Map<String, TinyMethod> mapMethodsByNamespace(int namespaceIndex) {
		return methods.stream().collect(Collectors.toMap(m -> m.getMethodNames().get(namespaceIndex), m -> m));
	}

	public Map<String, TinyField> mapFieldsByFirstNamespace() {
		return mapFieldsByNamespace(0);
	}
	public Map<String, TinyField> mapFieldsByNamespace(int namespaceIndex) {
		return fields.stream().collect(Collectors.toMap(f -> f.getFieldNames().get(namespaceIndex), f -> f));
	}


	public List<String> getClassNames() {
		return classNames;
	}

	public Collection<TinyMethod> getMethods() {
		return methods;
	}

	public Collection<TinyField> getFields() {
		return fields;
	}

	@Override
	public Collection<String> getComments() {
		return comments;
	}

	@Override
	public int compareTo(TinyClass o) {
		return classNames.get(0).compareTo(o.classNames.get(0));
	}

	@Override
	public List<String> getMapping() {
		return classNames;
	}
}
