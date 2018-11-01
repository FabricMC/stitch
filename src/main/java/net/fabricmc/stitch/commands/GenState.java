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

package net.fabricmc.stitch.commands;

import net.fabricmc.stitch.representation.*;
import net.fabricmc.stitch.util.MatcherUtil;
import net.fabricmc.tinyremapper.TinyUtils;
import org.objectweb.asm.Opcodes;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;

class GenState {
    private final Map<String, Integer> counters = new HashMap<>();
    private final Map<Entry, Integer> values = new IdentityHashMap<>();
    private GenMap oldToIntermediary, newToOld;

    public String next(Entry entry, String name) {
        return name + "_" + values.computeIfAbsent(entry, (e) -> {
            int v = counters.getOrDefault(name, 1);
            counters.put(name, v + 1);
            return v;
        });
    }

    public void setCounter(String key, int value) {
        counters.put(key, value);
    }

    public Map<String, Integer> getCounters() {
        return Collections.unmodifiableMap(counters);
    }

    public void generate(File file, JarEntry jarEntry, JarEntry jarOld) throws IOException {
        try (FileWriter fileWriter = new FileWriter(file)) {
            try (BufferedWriter writer = new BufferedWriter(fileWriter)) {
                writer.write("v1\tmojang\tintermediary\n");

                for (ClassEntry c : jarEntry.getClasses()) {
                    addClass(writer, c, jarOld, jarEntry, "net/minecraft/");
                }

                for (Map.Entry<String, Integer> counter : counters.entrySet()) {
                    writer.write("# INTERMEDIARY-COUNTER " + counter.getKey() + " " + counter.getValue() + "\n");
                }
            }
        }
    }

    public static boolean isMappedClass(ClassStorage storage, ClassEntry c) {
        return !c.isAnonymous();
    }

    public static boolean isMappedField(ClassStorage storage, ClassEntry c, FieldEntry f) {
        return f.getName().length() <= 2;
    }

    public static boolean isMappedMethod(ClassStorage storage, ClassEntry c, MethodEntry m) {
        return m.getName().length() <= 2 && m.getName().charAt(0) != '<' && m.isSource(storage, c);
    }

    @Nullable
    private String getFieldName(ClassStorage storage, ClassEntry c, FieldEntry f) {
        if (!isMappedField(storage, c, f)) {
            return null;
        }

        if (newToOld != null) {
            GenMap.DescEntry findEntry = newToOld.getField(c.getFullyQualifiedName(), f.getName(), f.getDescriptor());
            if (findEntry != null) {
                findEntry = oldToIntermediary.getField(findEntry);
                if (findEntry != null) {
                    return findEntry.getName();
                }
            }
        }

        return next(f, "field");
    }

    @Nullable
    private String getMethodName(ClassStorage storageOld, ClassStorage storageNew, ClassEntry c, MethodEntry m) {
        if (!isMappedMethod(storageNew, c, m)) {
            return null;
        }

        if (newToOld != null) {
            List<ClassEntry> ccList = m.getMatchingEntries(storageNew, c);
            Set<String> names = new HashSet<>();

            for (ClassEntry cc : ccList) {
                GenMap.DescEntry findEntry = newToOld.getMethod(cc.getFullyQualifiedName(), m.getName(), m.getDescriptor());
                if (findEntry != null) {
                    GenMap.DescEntry newToOldEntry = findEntry;
                    findEntry = oldToIntermediary.getMethod(newToOldEntry);
                    if (findEntry != null) {
                        names.add(findEntry.getName());
                    } else {
                        // more involved...
                        ClassEntry oldBase = storageOld.getClass(newToOldEntry.getOwner(), false);
                        if (oldBase != null) {
                            MethodEntry oldM = oldBase.getMethod(newToOldEntry.getName() + newToOldEntry.getDesc());
                            List<ClassEntry> cccList = oldM.getMatchingEntries(storageOld, oldBase);

                            for (ClassEntry ccc : cccList) {
                                findEntry = oldToIntermediary.getMethod(ccc.getFullyQualifiedName(), oldM.getName(), oldM.getDescriptor());
                                if (findEntry != null) {
                                    names.add(findEntry.getName());
                                }
                            }
                        }
                    }
                }
            }

            if (names.size() > 1) {
                StringBuilder builder = new StringBuilder("Conflict: ");
                int i = 0;
                for (String s : names) {
                    if ((i++) > 0) {
                        builder.append(", ");
                    }
                    builder.append(s);
                }

                throw new RuntimeException(builder.toString());
            } else if (names.size() == 1) {
                return names.iterator().next();
            }
        }

        return next(m, "method");
    }

    private void addClass(BufferedWriter writer, ClassEntry c, ClassStorage storageOld, ClassStorage storage, String translatedPrefix) throws IOException {
        String cname = "";

        if (c.getName().contains("/")) {
            translatedPrefix = c.getFullyQualifiedName();
        } else {
            if (!isMappedClass(storage, c)) {
                cname = c.getName();
            } else {
                cname = null;

                if (newToOld != null) {
                    String findName = newToOld.getClass(c.getFullyQualifiedName());
                    if (findName != null) {
                        findName = oldToIntermediary.getClass(findName);
                        if (findName != null) {
                            String[] r = findName.split("\\$");
                            cname = r[r.length - 1];
                            if (r.length == 1) {
                                translatedPrefix = "";
                            }

                        }
                    }
                }

                if (cname == null) {
                    cname = next(c, "class");
                }
            }
        }

        writer.write("CLASS\t" + c.getFullyQualifiedName() + "\t" + translatedPrefix + cname + "\n");

        for (FieldEntry f : c.getFields()) {
            String fName = getFieldName(storage, c, f);
            if (fName != null) {
                writer.write("FIELD\t" + c.getFullyQualifiedName()
                        + "\t" + f.getDescriptor()
                        + "\t" + f.getName()
                        + "\t" + fName + "\n");
            }
        }

        for (MethodEntry m : c.getMethods()) {
            String mName = getMethodName(storageOld, storage, c, m);
            if (mName != null) {
                writer.write("METHOD\t" + c.getFullyQualifiedName()
                        + "\t" + m.getDescriptor()
                        + "\t" + m.getName()
                        + "\t" + mName + "\n");
            }
        }

        for (ClassEntry cc : c.getInnerClasses()) {
            addClass(writer, cc, storageOld, storage, translatedPrefix + cname + "$");
        }
    }

    public void prepareUpdate(File oldMappings, File matches) throws IOException {
        oldToIntermediary = new GenMap(false);
        newToOld = new GenMap(true);

        // TODO: only read once
        try (FileReader fileReader = new FileReader(oldMappings)) {
            try (BufferedReader reader = new BufferedReader(fileReader)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("# INTERMEDIARY-COUNTER")) {
                        String[] parts = line.split(" ");
                        counters.put(parts[2], Integer.parseInt(parts[3]));
                    }
                }
            }
        }

        try (FileReader fileReader = new FileReader(oldMappings)) {
            try (BufferedReader reader = new BufferedReader(fileReader)) {
                TinyUtils.read(reader, "mojang", "intermediary", oldToIntermediary::addClass, oldToIntermediary::addField, oldToIntermediary::addMethod);
            }
        }

        try (FileReader fileReader = new FileReader(matches)) {
            try (BufferedReader reader = new BufferedReader(fileReader)) {
                MatcherUtil.read(reader, newToOld::addClass, newToOld::addField, newToOld::addMethod);
            }
        }
    }
}
