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

import java.io.File;
import java.util.*;

public class JarEntry extends Entry implements ClassStorage {
    final Object syncObject = new Object();
    final File file;
    final Map<String, ClassEntry> classTree;
    final List<ClassEntry> allClasses;

    public JarEntry(File file) {
        super(file.getName());

        this.file = file;
        this.classTree = new HashMap<>();
        this.allClasses = new ArrayList<>();
    }

    @Override
    public ClassEntry getClass(String name, boolean create) {
        if (name == null) {
            return null;
        }

        String[] nameSplit = name.split("\\$");
        int i = 0;

        ClassEntry parent;
        ClassEntry entry = classTree.get(nameSplit[i++]);
        if (entry == null && create) {
            entry = new ClassEntry(nameSplit[0], nameSplit[0]);
            synchronized (syncObject) {
                allClasses.add(entry);
                classTree.put(entry.getName(), entry);
            }
        }

        StringBuilder fullyQualifiedBuilder = new StringBuilder(nameSplit[0]);

        while (i < nameSplit.length && entry != null) {
            fullyQualifiedBuilder.append('$');
            fullyQualifiedBuilder.append(nameSplit[i]);

            parent = entry;
            entry = entry.getInnerClass(nameSplit[i++]);

            if (entry == null && create) {
                entry = new ClassEntry(nameSplit[i - 1], fullyQualifiedBuilder.toString());
                synchronized (syncObject) {
                    allClasses.add(entry);
                    parent.innerClasses.put(entry.getName(), entry);
                }
            }
        }

        return entry;
    }

    public Collection<ClassEntry> getClasses() {
        return classTree.values();
    }

    public Collection<ClassEntry> getAllClasses() {
        return Collections.unmodifiableList(allClasses);
    }
}
