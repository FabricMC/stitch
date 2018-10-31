/*
 * Copyright (c) 2016, 2017, 2018 Adrian Siekierka
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

package net.fabricmc.stitch.merge;

import net.fabricmc.stitch.util.StitchUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

public class JarMerger {
    public class Entry {
        public final JarEntry metadata;
        public final byte[] data;

        public Entry(JarEntry metadata, byte[] data) {
            this.metadata = new JarEntry(metadata.getName());
            this.metadata.setTime(metadata.getTime());

            this.data = data;
        }
    }

    private static final ClassMerger CLASS_MERGER = new ClassMerger();
    private final JarInputStream inputClient, inputServer;
    private final JarOutputStream output;
    private final Map<String, Entry> entriesClient, entriesServer;
    private final Set<String> entriesAll;

    public JarMerger(JarInputStream inputClient, JarInputStream inputServer, JarOutputStream output) {
        this.inputClient = inputClient;
        this.inputServer = inputServer;
        this.output = output;

        this.entriesClient = new HashMap<>();
        this.entriesServer = new HashMap<>();
        this.entriesAll = new TreeSet<>();
    }

    public JarMerger(InputStream inputClient, InputStream inputServer, OutputStream output) throws IOException {
        this(new JarInputStream(inputClient), new JarInputStream(inputServer), new JarOutputStream(output));
    }

    public void close() throws IOException {
        inputClient.close();
        inputServer.close();
        output.close();
    }

    private void readToMap(Map<String, Entry> map, JarInputStream input) throws IOException {
        JarEntry entry;
        while ((entry = input.getNextJarEntry()) != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int l;
            while ((l = input.read(buffer, 0, buffer.length)) > 0) {
                stream.write(buffer, 0, l);
            }

            map.put(entry.getName(), new Entry(entry, stream.toByteArray()));
        }

        entriesAll.addAll(map.keySet());
    }

    private void add(JarOutputStream output, Entry entry) throws IOException {
        output.putNextEntry(entry.metadata);
        output.write(entry.data);
    }

    public void merge() throws IOException {
        readToMap(entriesClient, inputClient);
        readToMap(entriesServer, inputServer);

        List<Entry> entries = entriesAll.parallelStream().map((entry) -> {
            boolean isClass = entry.endsWith(".class");
            boolean isMinecraft = entry.startsWith("net/minecraft") || !entry.contains("/");
            Entry result;
            String side = null;

            if (isClass && !isMinecraft) {
                // Server bundles libraries, client doesn't - skip them
                return null;
            }

            Entry entry1 = entriesClient.get(entry);
            Entry entry2 = entriesServer.get(entry);

            if (entry1 != null && entry2 != null) {
                if (Arrays.equals(entry1.data, entry2.data)) {
                    result = entry1;
                } else {
                    if (isClass) {
                        JarEntry metadata = new JarEntry(entry1.metadata);
                        metadata.setLastModifiedTime(FileTime.fromMillis(StitchUtil.getTime()));

                        result = new Entry(metadata, CLASS_MERGER.merge(entry1.data, entry2.data));
                    } else {
                        // FIXME: More heuristics?
                        result = entry1;
                    }
                }
            } else if ((result = entry1) != null) {
                side = "CLIENT";
            } else if ((result = entry2) != null) {
                side = "SERVER";
            }

            if (result != null) {
                if (isMinecraft && isClass && side != null) {
                    result = new Entry(result.metadata, CLASS_MERGER.addSideInformation(result.data, side));
                }

                return result;
            } else {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        for (Entry e : entries) {
            add(output, e);
        }
;    }
}
