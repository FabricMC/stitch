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

package net.fabricmc.stitch.representation;

import net.fabricmc.stitch.util.Pair;
import org.objectweb.asm.commons.Remapper;

import java.util.*;
import java.util.stream.Collectors;

public class JarClassEntry extends AbstractJarEntry {
    String fullyQualifiedName;
    final Map<String, JarClassEntry> innerClasses;
    final Map<String, JarFieldEntry> fields;
    final Map<String, JarMethodEntry> methods;
    final Map<String, Set<Pair<JarClassEntry, String>>> relatedMethods;

    String signature;
    String superclass;
    List<String> interfaces;
    List<String> subclasses;
    List<String> implementers;

    protected JarClassEntry(String name, String fullyQualifiedName) {
        super(name);

        this.fullyQualifiedName = fullyQualifiedName;
        this.innerClasses = new TreeMap<>(Comparator.naturalOrder());
        this.fields = new TreeMap<>(Comparator.naturalOrder());
        this.methods = new TreeMap<>(Comparator.naturalOrder());
        this.relatedMethods = new HashMap<>();

        this.subclasses = new ArrayList<>();
        this.implementers = new ArrayList<>();
    }

    protected void populate(int access, String signature, String superclass, String[] interfaces) {
        this.setAccess(access);
        this.signature = signature;
        this.superclass = superclass;
        this.interfaces = Arrays.asList(interfaces);
    }

    protected void populateParents(ClassStorage storage) {
        JarClassEntry superEntry = getSuperClass(storage);
        if (superEntry != null) {
            superEntry.subclasses.add(fullyQualifiedName);
        }

        for (JarClassEntry itf : getInterfaces(storage)) {
            if (itf != null) {
                itf.implementers.add(fullyQualifiedName);
            }
        }
    }

    // unstable
    public Collection<Pair<JarClassEntry, String>> getRelatedMethods(JarMethodEntry m) {
        //noinspection unchecked
        return relatedMethods.getOrDefault(m.getKey(), Collections.EMPTY_SET);
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public String getSignature() {
        return signature;
    }

    public String getSuperClassName() {
        return superclass;
    }

    public JarClassEntry getSuperClass(ClassStorage storage) {
        return storage.getClass(superclass, false);
    }

    public List<String> getInterfaceNames() {
        return Collections.unmodifiableList(interfaces);
    }

    public List<JarClassEntry> getInterfaces(ClassStorage storage) {
        return toClassEntryList(storage, interfaces);
    }

    public List<String> getSubclassNames() {
        return Collections.unmodifiableList(subclasses);
    }

    public List<JarClassEntry> getSubclasses(ClassStorage storage) {
        return toClassEntryList(storage, subclasses);
    }

    public List<String> getImplementerNames() {
        return Collections.unmodifiableList(implementers);
    }

    public List<JarClassEntry> getImplementers(ClassStorage storage) {
        return toClassEntryList(storage, implementers);
    }

    private List<JarClassEntry> toClassEntryList(ClassStorage storage, List<String> stringList) {
        if (stringList == null) {
            return Collections.emptyList();
        }

        return stringList.stream()
                .map((s) -> storage.getClass(s, false))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public JarClassEntry getInnerClass(String name) {
        return innerClasses.get(name);
    }

    public JarFieldEntry getField(String name) {
        return fields.get(name);
    }

    public JarMethodEntry getMethod(String name) {
        return methods.get(name);
    }

    public Collection<JarClassEntry> getInnerClasses() {
        return innerClasses.values();
    }

    public Collection<JarFieldEntry> getFields() {
        return fields.values();
    }

    public Collection<JarMethodEntry> getMethods() {
        return methods.values();
    }

    public boolean isInterface() {
        return Access.isInterface(getAccess());
    }

    public boolean isAnonymous() {
        return getName().matches("[0-9]+");
    }

    @Override
    public String getKey() {
        return getFullyQualifiedName();
    }

    public void remap(Remapper remapper) {
        String oldName = fullyQualifiedName;
        fullyQualifiedName = remapper.map(fullyQualifiedName);
        String[] s = fullyQualifiedName.split("\\$");
        name = s[s.length - 1];

        if (superclass != null) {
            superclass = remapper.map(superclass);
        }

        interfaces = interfaces.stream().map(remapper::map).collect(Collectors.toList());
        subclasses = subclasses.stream().map(remapper::map).collect(Collectors.toList());
        implementers = implementers.stream().map(remapper::map).collect(Collectors.toList());

        Map<String, JarClassEntry> innerClassOld = new HashMap<>(innerClasses);
        Map<String, JarFieldEntry> fieldsOld = new HashMap<>(fields);
        Map<String, JarMethodEntry> methodsOld = new HashMap<>(methods);
        Map<String, String> methodKeyRemaps = new HashMap<>();

        innerClasses.clear();
        fields.clear();
        methods.clear();

        for (Map.Entry<String, JarClassEntry> entry : innerClassOld.entrySet()) {
            entry.getValue().remap(remapper);
            innerClasses.put(entry.getValue().name, entry.getValue());
        }

        for (Map.Entry<String, JarFieldEntry> entry : fieldsOld.entrySet()) {
            entry.getValue().remap(this, oldName, remapper);
            fields.put(entry.getValue().getKey(), entry.getValue());
        }

        for (Map.Entry<String, JarMethodEntry> entry : methodsOld.entrySet()) {
            entry.getValue().remap(this, oldName, remapper);
            methods.put(entry.getValue().getKey(), entry.getValue());
            methodKeyRemaps.put(entry.getKey(), entry.getValue().getKey());
        }

        // TODO: remap relatedMethods strings???
        Map<String, Set<Pair<JarClassEntry, String>>> relatedMethodsOld = new HashMap<>(relatedMethods);
        relatedMethods.clear();

        for (Map.Entry<String, Set<Pair<JarClassEntry, String>>> entry : relatedMethodsOld.entrySet()) {
            relatedMethods.put(methodKeyRemaps.getOrDefault(entry.getKey(), entry.getKey()), entry.getValue());
        }
    }
}
