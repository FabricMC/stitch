package net.fabricmc.stitch.commands.tinyv2;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class TinyMethod implements Comparable<TinyMethod> {

    /**
     * For example when we have official -> named mappings the descriptor will be in official, but in named -> official
     * the descriptor will be in named.
     */
    private final String methodDescriptorInFirstNamespace;
    private final List<String> methodNames;
    private final Set<TinyMethodParameter> parameters;
    private final Set<TinyLocalVariable> localVariables;
    private final Collection<String> comments;

    public TinyMethod(String methodDescriptorInFirstNamespace, List<String> methodNames, Set<TinyMethodParameter> parameters, Set<TinyLocalVariable> localVariables, Collection<String> comments) {
        this.methodDescriptorInFirstNamespace = methodDescriptorInFirstNamespace;
        this.methodNames = methodNames;
        this.parameters = parameters;
        this.localVariables = localVariables;
        this.comments = comments;
    }

    public String getMethodDescriptorInFirstNamespace() {
        return methodDescriptorInFirstNamespace;
    }

    public List<String> getMethodNames() {
        return methodNames;
    }

    public Set<TinyMethodParameter> getParameters() {
        return parameters;
    }

    public Set<TinyLocalVariable> getLocalVariables() {
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
}
