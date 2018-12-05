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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.Opcodes;

import java.io.*;
import java.util.*;

class GenState {
    private final Map<String, Integer> counters = new HashMap<>();
    private final Map<Entry, Integer> values = new IdentityHashMap<>();
    private GenMap oldToIntermediary, newToOld;
    private GenMap newToIntermediary;
    private boolean rewriteMode = false;

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
        if (file.exists()) {
            System.err.println("Target file exists - loading...");
            newToIntermediary = new GenMap(false);
            try (FileReader fileReader = new FileReader(file)) {
                try (BufferedReader reader = new BufferedReader(fileReader)) {
                    TinyUtils.read(reader, "official", "intermediary", newToIntermediary::addClass, newToIntermediary::addField, newToIntermediary::addMethod);
                }
            }
        }

        try (FileWriter fileWriter = new FileWriter(file)) {
            try (BufferedWriter writer = new BufferedWriter(fileWriter)) {
                writer.write("v1\tofficial\tintermediary\n");

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
        return f.getName().length() <= 2 || (f.getName().length() == 3 && f.getName().charAt(2) == '_');
    }

    public static boolean isMappedMethod(ClassStorage storage, ClassEntry c, MethodEntry m) {
        return (m.getName().length() <= 2 || (m.getName().length() == 3 && m.getName().charAt(2) == '_')) && m.getName().charAt(0) != '<' && m.isSource(storage, c);
    }

    @Nullable
    private String getFieldName(ClassStorage storage, ClassEntry c, FieldEntry f) {
        if (!isMappedField(storage, c, f)) {
            return null;
        }

        if (newToIntermediary != null) {
            GenMap.DescEntry findEntry = newToIntermediary.getField(c.getFullyQualifiedName(), f.getName(), f.getDescriptor());
            if (findEntry != null) {
                return findEntry.getName();
            }
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

    private final Map<MethodEntry, String> methodNames = new IdentityHashMap<>();

    @Nullable
    private String getMethodName(ClassStorage storageOld, ClassStorage storageNew, ClassEntry c, MethodEntry m) {
        if (!isMappedMethod(storageNew, c, m)) {
            return null;
        }

        if (methodNames.containsKey(m)) {
            return methodNames.get(m);
        }

        if (newToOld != null || newToIntermediary != null) {
            List<ClassEntry> ccList = m.getMatchingEntries(storageNew, c);
            Set<String> names = new HashSet<>();

            for (ClassEntry cc : ccList) {
                GenMap.DescEntry findEntry = null;
                if (newToIntermediary != null) {
                    findEntry = newToIntermediary.getMethod(cc.getFullyQualifiedName(), m.getName(), m.getDescriptor());
                    if (findEntry != null) {
                        names.add(findEntry.getName());
                    }
                }

                if (findEntry == null && newToOld != null) {
                    findEntry = newToOld.getMethod(cc.getFullyQualifiedName(), m.getName(), m.getDescriptor());
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
            }

            if (names.size() > 1) {
            	if (rewriteMode) {
		            int lowestNum = Integer.MAX_VALUE;
		            for (String s : names) {
						if (!s.startsWith("method_")) {
							throw new RuntimeException("Could not rewrite method: " + s);
						}

						int v = new Integer(s.split("_")[1]);
						if (v < lowestNum) {
							lowestNum = v;
						}
		            }
		            return "method_" + lowestNum;
	            }

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
                String s = names.iterator().next();
                methodNames.put(m, s);
                return s;
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

                if (newToIntermediary != null) {
                    String findName = newToIntermediary.getClass(c.getFullyQualifiedName());
                    if (findName != null) {
                        String[] r = findName.split("\\$");
                        cname = r[r.length - 1];
                        if (r.length == 1) {
                            translatedPrefix = "";
                        }
                    }
                }

                if (cname == null && newToOld != null) {
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

    public void prepareRewrite(File oldMappings) throws IOException {
        oldToIntermediary = new GenMap(false);
        newToOld = new GenMap.Dummy(false);

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
                TinyUtils.read(reader, "official", "intermediary", oldToIntermediary::addClass, oldToIntermediary::addField, oldToIntermediary::addMethod);
            }
        }

        rewriteMode = true;
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
                TinyUtils.read(reader, "official", "intermediary", oldToIntermediary::addClass, oldToIntermediary::addField, oldToIntermediary::addMethod);
            }
        }

        try (FileReader fileReader = new FileReader(matches)) {
            try (BufferedReader reader = new BufferedReader(fileReader)) {
                MatcherUtil.read(reader, newToOld::addClass, newToOld::addField, newToOld::addMethod);
            }
        }
    }
}
