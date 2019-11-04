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

public class TinyField implements Comparable<TinyField>, Mapping {

	/**
	 * For example when we have official -> named mappings the descriptor will be in official, but in named -> official
	 * the descriptor will be in named.
	 */
	private String fieldDescriptorInFirstNamespace;
	private final List<String> fieldNames;
	private final Collection<String> comments;

	public TinyField(String fieldDescriptorInFirstNamespace, List<String> fieldNames, Collection<String> comments) {
		this.fieldDescriptorInFirstNamespace = fieldDescriptorInFirstNamespace;
		this.fieldNames = fieldNames;
		this.comments = comments;
	}

	public String getFieldDescriptorInFirstNamespace() {
		return fieldDescriptorInFirstNamespace;
	}

	public List<String> getFieldNames() {
		return fieldNames;
	}

	public Collection<String> getComments() {
		return comments;
	}

	@Override
	public int compareTo(TinyField o) {
		return fieldNames.get(0).compareTo(o.fieldNames.get(0));
	}

	public void setFieldDescriptorInFirstNamespace(String fieldDescriptorInFirstNamespace) {
		this.fieldDescriptorInFirstNamespace = fieldDescriptorInFirstNamespace;
	}

	@Override
	public List<String> getMapping() {
		return fieldNames;
	}
}
