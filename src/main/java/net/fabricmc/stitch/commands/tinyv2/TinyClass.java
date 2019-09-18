package net.fabricmc.stitch.commands.tinyv2;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TinyClass implements Comparable<TinyClass> {

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

    /**
     * Descriptors are also taken into account because methods can overload.
     * The key format is firstMethodName + descriptor
     */
    public Map<String, TinyMethod> mapMethodsByFirstNamespaceAndDescriptor() {
        return methods.stream().collect(Collectors.toMap(m -> m.getMethodNames().get(0) + m.getMethodDescriptorInFirstNamespace(), m -> m));
    }

    public Map<String, TinyField> mapFieldsByFirstNamespace() {
        return fields.stream().collect(Collectors.toMap(f -> f.getFieldNames().get(0), f -> f));
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

    public Collection<String> getComments() {
        return comments;
    }

    @Override
    public int compareTo(TinyClass o) {
        return classNames.get(0).compareTo(o.classNames.get(0));
    }
}
