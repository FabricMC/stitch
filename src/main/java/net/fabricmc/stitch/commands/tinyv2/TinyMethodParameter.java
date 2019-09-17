package net.fabricmc.stitch.commands.tinyv2;

import java.util.Collection;
import java.util.List;

public class TinyMethodParameter implements Comparable<TinyMethodParameter> {
    private final int lvIndex;
    private final List<String> parameterNames;
    private final Collection<String> comments;

    public TinyMethodParameter(int lvIndex, List<String> parameterNames, Collection<String> comments) {
        this.lvIndex = lvIndex;
        this.parameterNames = parameterNames;
        this.comments = comments;
    }

    public int getLvIndex() {
        return lvIndex;
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }

    public Collection<String> getComments() {
        return comments;
    }

    @Override
    public int compareTo(TinyMethodParameter o) {
        return lvIndex - o.lvIndex;
    }
}
