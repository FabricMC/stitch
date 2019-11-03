package net.fabricmc.stitch.commands.tinyv2;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TinyMethod implements Comparable<TinyMethod>, Mapping {

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

	public Map<String, TinyMethodParameter> mapParametersByFirstNamespace() {
		return parameters.stream().collect(Collectors.toMap(p -> p.getParameterNames().get(0), p -> p));
	}

	public Map<String, TinyLocalVariable> mapLocalVariablesByFirstNamespace() {
		return localVariables.stream().collect(Collectors.toMap(lv -> lv.getLocalVariableNames().get(0), lv -> lv));
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
