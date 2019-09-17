package net.fabricmc.stitch.commands.tinyv2;

import java.util.List;
import java.util.Map;

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
