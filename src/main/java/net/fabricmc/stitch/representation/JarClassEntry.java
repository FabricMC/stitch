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

package net.fabricmc.stitch.representation;

import net.fabricmc.stitch.util.Pair;
import org.objectweb.asm.commons.Remapper;

import java.util.*;
import java.util.stream.Collectors;

public class JarClassEntry extends AbstractJarEntry {
    final Map<String, JarClassEntry> innerClasses;
    final Map<String, JarFieldEntry> fields;
    final Map<String, JarMethodEntry> methods;
    final Map<String, Set<Pair<JarClassEntry, String>>> relatedMethods;

    String signature;
    String superclass;
    List<String> interfaces;
    List<String> subclasses;
    List<String> implementers;
    /** outer class for inner classes */
    String declaringClass;
    /** outer class for anonymous and local classes */
    String enclosingClass;
    String enclosingMethod;
    String enclosingMethodDesc;
    String innerName;

    protected JarClassEntry(String name) {
        super(name);

        this.innerClasses = new TreeMap<>(Comparator.naturalOrder());
        this.fields = new TreeMap<>(Comparator.naturalOrder());
        this.methods = new TreeMap<>(Comparator.naturalOrder());
        this.relatedMethods = new HashMap<>();

        this.subclasses = new ArrayList<>();
        this.implementers = new ArrayList<>();
    }

    protected void populate(Populator populator) {
        this.setAccess(populator.access);
        this.signature = populator.signature;
        this.superclass = populator.superclass;
        this.interfaces = Arrays.asList(populator.interfaces);
        if (populator.nested) {
            this.declaringClass = populator.declaringClass;
            this.enclosingClass = populator.enclosingClass;
            this.enclosingMethod = populator.enclosingMethod;
            this.enclosingMethodDesc = populator.enclosingMethodDesc;
            this.innerName = populator.innerName;
        }
    }

    protected void populateRelations(ClassStorage storage) {
        JarClassEntry superEntry = getSuperClass(storage);
        if (superEntry != null) {
            superEntry.subclasses.add(name);
        }

        for (JarClassEntry itf : getInterfaces(storage)) {
            if (itf != null) {
                itf.implementers.add(name);
            }
        }

        JarClassEntry declaringEntry = getDeclaringClass(storage);
        if (declaringEntry != null) {
            declaringEntry.innerClasses.put(name, this);
        }
        JarClassEntry enclosingEntry = getEnclosingClass(storage);
        if (enclosingEntry != null) {
            enclosingEntry.innerClasses.put(name, this);
        }
    }

    // unstable
    public Collection<Pair<JarClassEntry, String>> getRelatedMethods(JarMethodEntry m) {
        //noinspection unchecked
        return relatedMethods.getOrDefault(m.getKey(), Collections.EMPTY_SET);
    }

    public String getSignature() {
        return signature;
    }

    public String getSuperClassName() {
        return superclass;
    }

    public JarClassEntry getSuperClass(ClassStorage storage) {
        return storage.getClass(superclass, null);
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
                .map((s) -> storage.getClass(s, null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public String getDeclaringClassName() {
        return declaringClass;
    }

    public JarClassEntry getDeclaringClass(ClassStorage storage) {
        return hasDeclaringClass() ? storage.getClass(declaringClass, null) : null;
    }

    public String getEnclosingClassName() {
        return enclosingClass;
    }

    public JarClassEntry getEnclosingClass(ClassStorage storage) {
        return hasEnclosingClass() ? storage.getClass(enclosingClass, null) : null;
    }

    public String getEnclosingMethodName() {
        return enclosingMethod;
    }

    public String getEnclosingMethodDescriptor() {
        return enclosingMethodDesc;
    }

    public JarMethodEntry getEnclosingMethod(JarRootEntry storage) {
        return hasEnclosingClass() && hasEnclosingMethod() ? getEnclosingClass(storage).getMethod(enclosingMethod + enclosingMethodDesc) : null;
    }

    public String getInnerName() {
        return innerName;
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

    public boolean hasDeclaringClass() {
        return declaringClass != null;
    }

    public boolean hasEnclosingClass() {
        return enclosingClass != null;
    }

    public boolean hasEnclosingMethod() {
        return enclosingMethod != null;
    }

    public boolean isAnonymous() {
        return hasEnclosingClass() && innerName == null;
    }

    public boolean isLocal() {
        return hasEnclosingClass() && innerName != null;
    }

    public void remap(Remapper remapper) {
        String oldName = name;
        name = remapper.map(name);

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

    public static class Populator {
        public int access;
        public String name;
        public String signature;
        public String superclass;
        public String[] interfaces;
        public String declaringClass;
        public String enclosingClass;
        public String enclosingMethod;
        public String enclosingMethodDesc;
        public String innerName;

        boolean nested;

        public Populator(int access, String name, String signature, String superclass, String[] interfaces) {
            this.access = access;
            this.name = name;
            this.signature = signature;
            this.superclass = superclass;
            this.interfaces = interfaces;
        }
    }
}
