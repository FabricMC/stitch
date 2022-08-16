/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
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

import net.fabricmc.mappings.EntryTriple;
import net.fabricmc.mappings.MappingsProvider;
import net.fabricmc.stitch.representation.*;
import net.fabricmc.stitch.util.MatcherUtil;
import net.fabricmc.stitch.util.Pair;
import net.fabricmc.stitch.util.StitchUtil;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.Opcodes;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class GenState {
    private final Map<String, Integer> counters = new HashMap<>();
    private final Map<AbstractJarEntry, Integer> values = new IdentityHashMap<>();
    private GenMap oldToIntermediary, newToOld;
    private GenMap newToIntermediary;
    private boolean interactive = true;
    private boolean writeAll = false;
    private Scanner scanner = new Scanner(System.in);

    private String targetNamespace = "net/minecraft/";
    private final List<Pattern> obfuscatedClassPatterns = new ArrayList<>();
    private final List<Pattern> obfuscatedFieldPatterns = new ArrayList<>();
    private final List<Pattern> obfuscatedMethodPatterns = new ArrayList<>();

    public GenState() {
        this.obfuscatedClassPatterns.add(Pattern.compile("^[^/]*$")); // Default ofbfuscation. Minecraft classes without a package are obfuscated.
        this.obfuscatedFieldPatterns.add(Pattern.compile("^.{0,2}_?$"));
        this.obfuscatedMethodPatterns.add(Pattern.compile("^[^<].?_?$"));
    }

    public void setWriteAll(boolean writeAll) {
        this.writeAll = writeAll;
    }

    public void disableInteractive() {
        interactive = false;
    }

    public String next(AbstractJarEntry entry, String name) {
        return name + "_" + values.computeIfAbsent(entry, (e) -> {
            int v = counters.getOrDefault(name, 1);
            counters.put(name, v + 1);
            return v;
        });
    }

    public void setTargetNamespace(final String namespace) {
        if (namespace.lastIndexOf("/") != (namespace.length() - 1))
            this.targetNamespace = namespace + "/";
        else
            this.targetNamespace = namespace;
    }

    public void clearObfuscatedClassPatterns() {
        this.obfuscatedClassPatterns.clear();
    }

    public void addObfuscatedClassPattern(String regex) throws PatternSyntaxException {
        this.obfuscatedClassPatterns.add(Pattern.compile(regex));
    }

    public void clearObfuscatedFieldPatterns() {
        this.obfuscatedFieldPatterns.clear();
    }

    public void addObfuscatedFieldPattern(String regex) throws PatternSyntaxException {
        this.obfuscatedFieldPatterns.add(Pattern.compile(regex));
    }

    public void clearObfuscatedMethodPatterns() {
        this.obfuscatedMethodPatterns.clear();
    }

    public void addObfuscatedMethodPattern(String regex) throws PatternSyntaxException {
        this.obfuscatedMethodPatterns.add(Pattern.compile(regex));
    }

    public void setCounter(String key, int value) {
        counters.put(key, value);
    }

    public Map<String, Integer> getCounters() {
        return Collections.unmodifiableMap(counters);
    }

    public void generate(File file, JarRootEntry jarEntry, JarRootEntry jarOld) throws IOException {
        if (file.exists()) {
            System.err.println("Target file exists - loading...");
            newToIntermediary = new GenMap();
            try (FileInputStream inputStream = new FileInputStream(file)) {
                newToIntermediary.load(
                        MappingsProvider.readTinyMappings(inputStream),
                        "official",
                        "intermediary"
                );
            }
        }

        try (FileWriter fileWriter = new FileWriter(file)) {
            try (BufferedWriter writer = new BufferedWriter(fileWriter)) {
                writer.write("v1\tofficial\tintermediary\n");

                for (JarClassEntry c : jarEntry.getClasses()) {
                    addClass(writer, c, jarOld, jarEntry, this.targetNamespace);
                }

                writeCounters(writer);
            }
        }
    }

    public static boolean isMappedClass(ClassStorage storage, JarClassEntry c) {
        return !c.isAnonymous();
    }

    public boolean isMappedField(ClassStorage storage, JarClassEntry c, JarFieldEntry f) {
        return isUnmappedFieldName(f.getName());
    }

    public boolean isUnmappedFieldName(String name) {
        return this.obfuscatedFieldPatterns.stream().anyMatch(p -> p.matcher(name).matches());
    }

    public boolean isMappedMethod(ClassStorage storage, JarClassEntry c, JarMethodEntry m) {
        return isUnmappedMethodName(m.getName()) && m.isSource(storage, c);
    }

    public boolean isUnmappedMethodName(String name) {
       return this.obfuscatedMethodPatterns.stream().anyMatch(p -> p.matcher(name).matches());
    }

    @Nullable
    private String getFieldName(ClassStorage storage, JarClassEntry c, JarFieldEntry f) {
        if (!isMappedField(storage, c, f)) {
            return null;
        }

        if (newToIntermediary != null) {
            EntryTriple findEntry = newToIntermediary.getField(c.getFullyQualifiedName(), f.getName(), f.getDescriptor());
            if (findEntry != null) {
                if (findEntry.getName().contains("field_")) {
                    return findEntry.getName();
                } else {
                    String newName = next(f, "field");
                    System.out.println(findEntry.getName() + " is now " + newName);
                    return newName;
                }
            }
        }

        if (newToOld != null) {
            EntryTriple findEntry = newToOld.getField(c.getFullyQualifiedName(), f.getName(), f.getDescriptor());
            if (findEntry != null) {
                findEntry = oldToIntermediary.getField(findEntry);
                if (findEntry != null) {
                    if (findEntry.getName().contains("field_")) {
                        return findEntry.getName();
                    } else {
                        String newName = next(f, "field");
                        System.out.println(findEntry.getName() + " is now " + newName);
                        return newName;
                    }
                }
            }
        }

        return next(f, "field");
    }

    private final Map<JarMethodEntry, String> methodNames = new IdentityHashMap<>();

    private String getPropagation(ClassStorage storage, JarClassEntry classEntry) {
        if (classEntry == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder(classEntry.getFullyQualifiedName());
        List<String> strings = new ArrayList<>();
        String scs = getPropagation(storage, classEntry.getSuperClass(storage));
        if (!scs.isEmpty()) {
            strings.add(scs);
        }

        for (JarClassEntry ce : classEntry.getInterfaces(storage)) {
            scs = getPropagation(storage, ce);
            if (!scs.isEmpty()) {
                strings.add(scs);
            }
        }

        if (!strings.isEmpty()) {
            builder.append("<-");
            if (strings.size() == 1) {
                builder.append(strings.get(0));
            } else {
                builder.append("[");
                builder.append(StitchUtil.join(",", strings));
                builder.append("]");
            }
        }

        return builder.toString();
    }

    private String getNamesListEntry(ClassStorage storage, JarClassEntry classEntry) {
        StringBuilder builder = new StringBuilder(getPropagation(storage, classEntry));
        if (classEntry.isInterface()) {
            builder.append("(itf)");
        }

        return builder.toString();
    }

    private Set<JarMethodEntry> findNames(ClassStorage storageOld, ClassStorage storageNew, JarClassEntry c, JarMethodEntry m, Map<String, Set<String>> names) {
        Set<JarMethodEntry> allEntries = new HashSet<>();
        findNames(storageOld, storageNew, c, m, names, allEntries);
        return allEntries;
    }

    private void findNames(ClassStorage storageOld, ClassStorage storageNew, JarClassEntry c, JarMethodEntry m, Map<String, Set<String>> names, Set<JarMethodEntry> usedMethods) {
        if (!usedMethods.add(m)) {
            return;
        }

        String suffix = "." + m.getName() + m.getDescriptor();

        if ((m.getAccess() & Opcodes.ACC_BRIDGE) != 0) {
            suffix += "(bridge)";
        }

        List<JarClassEntry> ccList = m.getMatchingEntries(storageNew, c);

        for (JarClassEntry cc : ccList) {
            EntryTriple findEntry = null;
            if (newToIntermediary != null) {
                findEntry = newToIntermediary.getMethod(cc.getFullyQualifiedName(), m.getName(), m.getDescriptor());
                if (findEntry != null) {
                    names.computeIfAbsent(findEntry.getName(), (s) -> new TreeSet<>()).add(getNamesListEntry(storageNew, cc) + suffix);
                }
            }

            if (findEntry == null && newToOld != null) {
                findEntry = newToOld.getMethod(cc.getFullyQualifiedName(), m.getName(), m.getDescriptor());
                if (findEntry != null) {
                    EntryTriple newToOldEntry = findEntry;
                    findEntry = oldToIntermediary.getMethod(newToOldEntry);
                    if (findEntry != null) {
                        names.computeIfAbsent(findEntry.getName(), (s) -> new TreeSet<>()).add(getNamesListEntry(storageNew, cc) + suffix);
                    } else {
                        // more involved...
                        JarClassEntry oldBase = storageOld.getClass(newToOldEntry.getOwner(), false);
                        if (oldBase != null) {
                            JarMethodEntry oldM = oldBase.getMethod(newToOldEntry.getName() + newToOldEntry.getDesc());
                            List<JarClassEntry> cccList = oldM.getMatchingEntries(storageOld, oldBase);

                            for (JarClassEntry ccc : cccList) {
                                findEntry = oldToIntermediary.getMethod(ccc.getFullyQualifiedName(), oldM.getName(), oldM.getDescriptor());
                                if (findEntry != null) {
                                    names.computeIfAbsent(findEntry.getName(), (s) -> new TreeSet<>()).add(getNamesListEntry(storageOld, ccc) + suffix);
                                }
                            }
                        }
                    }
                }
            }
        }

        for (JarClassEntry mc : ccList) {
            for (Pair<JarClassEntry, String> pair : mc.getRelatedMethods(m)) {
                findNames(storageOld, storageNew, pair.getLeft(), pair.getLeft().getMethod(pair.getRight()), names, usedMethods);
            }
        }
    }

    @Nullable
    private String getMethodName(ClassStorage storageOld, ClassStorage storageNew, JarClassEntry c, JarMethodEntry m) {
        if (!isMappedMethod(storageNew, c, m)) {
            return null;
        }

        if (methodNames.containsKey(m)) {
            return methodNames.get(m);
        }

        if (newToOld != null || newToIntermediary != null) {
            Map<String, Set<String>> names = new HashMap<>();
            Set<JarMethodEntry> allEntries = findNames(storageOld, storageNew, c, m, names);
            for (JarMethodEntry mm : allEntries) {
                if (methodNames.containsKey(mm)) {
                    return methodNames.get(mm);
                }
            }

            if (names.size() > 1) {
                System.out.println("Conflict detected - matched same target name!");
                List<String> nameList = new ArrayList<>(names.keySet());
                Collections.sort(nameList);

                for (int i = 0; i < nameList.size(); i++) {
                    String s = nameList.get(i);
                    System.out.println((i+1) + ") " + s + " <- " + StitchUtil.join(", ", names.get(s)));
                }

                if (!interactive) {
                    throw new RuntimeException("Conflict detected!");
                }

                while (true) {
                    String cmd = scanner.nextLine();
                    int i;
                    try {
                        i = Integer.parseInt(cmd);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        continue;
                    }

                    if (i >= 1 && i <= nameList.size()) {
                        for (JarMethodEntry mm : allEntries) {
                            methodNames.put(mm, nameList.get(i - 1));
                        }
                        System.out.println("OK!");
                        return nameList.get(i - 1);
                    }
                }
            } else if (names.size() == 1) {
                String s = names.keySet().iterator().next();
                for (JarMethodEntry mm : allEntries) {
                    methodNames.put(mm, s);
                }
                if (s.contains("method_")) {
                    return s;
                } else {
                    String newName = next(m, "method");
                    System.out.println(s + " is now " + newName);
                    return newName;
                }
            }
        }

        return next(m, "method");
    }

    private void addClass(BufferedWriter writer, JarClassEntry c, ClassStorage storageOld, ClassStorage storage, String translatedPrefix) throws IOException {
        String className = c.getName();
        String cname = "";
        String prefixSaved = translatedPrefix;

        if(!this.obfuscatedClassPatterns.stream().anyMatch(p -> p.matcher(className).matches())) {
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

                if (cname != null && !cname.contains("class_")) {
                    String newName = next(c, "class");
                    System.out.println(cname + " is now " + newName);
                    cname = newName;
                    translatedPrefix = prefixSaved;
                }

                if (cname == null) {
                    cname = next(c, "class");
                }
            }
        }

        writer.write("CLASS\t" + c.getFullyQualifiedName() + "\t" + translatedPrefix + cname + "\n");

        for (JarFieldEntry f : c.getFields()) {
            String fName = getFieldName(storage, c, f);
            if (fName == null) {
                fName = f.getName();
            }

            if (fName != null) {
                writer.write("FIELD\t" + c.getFullyQualifiedName()
                        + "\t" + f.getDescriptor()
                        + "\t" + f.getName()
                        + "\t" + fName + "\n");
            }
        }

        for (JarMethodEntry m : c.getMethods()) {
            String mName = getMethodName(storageOld, storage, c, m);
            if (mName == null) {
                if (!m.getName().startsWith("<") && m.isSource(storage, c)) {
                   mName = m.getName();
                }
            }

            if (mName != null) {
                writer.write("METHOD\t" + c.getFullyQualifiedName()
                        + "\t" + m.getDescriptor()
                        + "\t" + m.getName()
                        + "\t" + mName + "\n");
            }
        }

        for (JarClassEntry cc : c.getInnerClasses()) {
            addClass(writer, cc, storageOld, storage, translatedPrefix + cname + "$");
        }
    }

    public void prepareRewrite(File oldMappings) throws IOException {
        oldToIntermediary = new GenMap();
        newToOld = new GenMap.Dummy();

        // TODO: only read once
        readCounters(oldMappings);

        try (FileInputStream inputStream = new FileInputStream(oldMappings)) {
            oldToIntermediary.load(
                    MappingsProvider.readTinyMappings(inputStream),
                    "official",
                    "intermediary"
            );
        }
    }

    public void prepareUpdate(File oldMappings, File matches) throws IOException {
        oldToIntermediary = new GenMap();
        newToOld = new GenMap();

        // TODO: only read once
        readCounters(oldMappings);

        try (FileInputStream inputStream = new FileInputStream(oldMappings)) {
            oldToIntermediary.load(
                    MappingsProvider.readTinyMappings(inputStream),
                    "official",
                    "intermediary"
            );
        }

        try (FileReader fileReader = new FileReader(matches)) {
            try (BufferedReader reader = new BufferedReader(fileReader)) {
                MatcherUtil.read(reader, true, newToOld::addClass, newToOld::addField, newToOld::addMethod);
            }
        }
    }

    private void readCounters(File counterFile) throws IOException {
        Path counterPath = getExternalCounterFile();

        if (counterPath != null && Files.exists(counterPath)) {
            counterFile = counterPath.toFile();
        }

        try (FileReader fileReader = new FileReader(counterFile)) {
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
    }

    private void writeCounters(BufferedWriter writer) throws IOException {
        StringJoiner counterLines = new StringJoiner("\n");

        for (Map.Entry<String, Integer> counter : counters.entrySet()) {
            counterLines.add("# INTERMEDIARY-COUNTER " + counter.getKey() + " " + counter.getValue());
        }

        writer.write(counterLines.toString());
        Path counterPath = getExternalCounterFile();

        if (counterPath != null) {
            Files.write(counterPath, counterLines.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private Path getExternalCounterFile() {
        if (System.getProperty("stitch.counter") != null) {
            return Paths.get(System.getProperty("stitch.counter"));
        }
        return null;
    }
}
