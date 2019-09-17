package net.fabricmc.stitch.commands.tinyv2;

import java.util.List;

public class TinyFile {
    private final TinyHeader header;
    private final List<TinyClass> classEntries;

    public TinyFile(TinyHeader header, List<TinyClass> classEntries) {
        this.header = header;
        this.classEntries = classEntries;
    }

    public TinyHeader getHeader() {
        return header;
    }

    public List<TinyClass> getClassEntries() {
        return classEntries;
    }
}
