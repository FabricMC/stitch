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
import java.util.Map;
import java.util.stream.Collectors;

public class TinyMethod implements Comparable<TinyMethod>, Mapping {

	@Override
	public String toString() {
		return "TinyMethod(names = [" + String.join(", ", methodNames) + "], desc = " + methodDescriptorInFirstNamespace
				+ ", " + parameters.size() + " params, "
				+ localVariables.size() + " vars, " + comments.size() + " comments)";
	}

	/**
	 * For example when we have official -> named mappings the descriptor will be in official, but in named -> official
	 * the descriptor will be in named.
	 */
	private String methodDescriptorInFirstNamespace;
	private final List<String> methodNames;
	private final Collection<TinyMethodParameter> parameters;
	private final Collection<TinyLocalVariable> localVariables;
	private final Collection<String> comments;

	public TinyMethod(String methodDescriptorInFirstNamespace, List<String> methodNames, Collection<TinyMethodParameter> parameters, Collection<TinyLocalVariable> localVariables, Collection<String> comments) {
		this.methodDescriptorInFirstNamespace = methodDescriptorInFirstNamespace;
		this.methodNames = methodNames;
		this.parameters = parameters;
		this.localVariables = localVariables;
		this.comments = comments;
	}


	public Map<Integer, TinyMethodParameter> mapParametersByLvIndex() {
		return parameters.stream().collect(Collectors.toMap(TinyMethodParameter::getLvIndex, p -> p));
	}

	public Map<Integer, TinyLocalVariable> mapLocalVariablesByLvIndex() {
		return localVariables.stream().collect(Collectors.toMap(TinyLocalVariable::getLvIndex, lv -> lv));
	}

	public String getMethodDescriptorInFirstNamespace() {
		return methodDescriptorInFirstNamespace;
	}

	public List<String> getMethodNames() {
		return methodNames;
	}

	public Collection<TinyMethodParameter> getParameters() {
		return parameters;
	}

	public Collection<TinyLocalVariable> getLocalVariables() {
		return localVariables;
	}

	@Override
	public Collection<String> getComments() {
		return comments;
	}

	@Override
	public int compareTo(TinyMethod o) {
		return (methodNames.get(0) + methodDescriptorInFirstNamespace)
				.compareTo(o.methodNames.get(0) + o.methodDescriptorInFirstNamespace);
	}

	public void setMethodDescriptorInFirstNamespace(String methodDescriptorInFirstNamespace) {
		this.methodDescriptorInFirstNamespace = methodDescriptorInFirstNamespace;
	}

	@Override
	public List<String> getMapping() {
		return methodNames;
	}

}
