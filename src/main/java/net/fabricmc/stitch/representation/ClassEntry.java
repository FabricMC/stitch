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

import java.util.*;
import java.util.stream.Collectors;

public class ClassEntry extends Entry {
    final String fullyQualifiedName;
    final Map<String, ClassEntry> innerClasses;
    final Map<String, FieldEntry> fields;
    final Map<String, MethodEntry> methods;

    String signature;
    String superclass;
    List<String> interfaces;
    List<String> subclasses;
    List<String> implementers;

    protected ClassEntry(String name, String fullyQualifiedName) {
        super(name);

        this.fullyQualifiedName = fullyQualifiedName;
        this.innerClasses = new HashMap<>();
        this.fields = new HashMap<>();
        this.methods = new HashMap<>();

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
        ClassEntry superEntry = getSuperClass(storage);
        if (superEntry != null) {
            superEntry.subclasses.add(fullyQualifiedName);
        }

        for (ClassEntry itf : getInterfaces(storage)) {
            if (itf != null) {
                itf.implementers.add(fullyQualifiedName);
            }
        }
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

    public ClassEntry getSuperClass(ClassStorage storage) {
        return storage.getClass(superclass, false);
    }

    public List<String> getInterfaceNames() {
        return Collections.unmodifiableList(interfaces);
    }

    public List<ClassEntry> getInterfaces(ClassStorage storage) {
        return toClassEntryList(storage, interfaces);
    }

    public List<String> getSubclassNames() {
        return Collections.unmodifiableList(subclasses);
    }

    public List<ClassEntry> getSubclasses(ClassStorage storage) {
        return toClassEntryList(storage, subclasses);
    }

    public List<String> getImplementerNames() {
        return Collections.unmodifiableList(implementers);
    }

    public List<ClassEntry> getImplementers(ClassStorage storage) {
        return toClassEntryList(storage, implementers);
    }

    private List<ClassEntry> toClassEntryList(ClassStorage storage, List<String> stringList) {
        return stringList.stream()
                .map((s) -> storage.getClass(s, false))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public ClassEntry getInnerClass(String name) {
        return innerClasses.get(name);
    }

    public FieldEntry getField(String name) {
        return fields.get(name);
    }

    public MethodEntry getMethod(String name) {
        return methods.get(name);
    }

    public Collection<ClassEntry> getInnerClasses() {
        return innerClasses.values();
    }

    public Collection<FieldEntry> getFields() {
        return fields.values();
    }

    public Collection<MethodEntry> getMethods() {
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
}
