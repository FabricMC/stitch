package net.fabricmc.stitch.commands.tinyv2;

import com.strobel.core.Comparer;

import java.util.Collection;
import java.util.List;

public class TinyField implements Comparable<TinyField> {

    /**
     * For example when we have official -> named mappings the descriptor will be in official, but in named -> official
     * the descriptor will be in named.
     */
    private final String fieldDescriptorInFirstNamespace;
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
}
