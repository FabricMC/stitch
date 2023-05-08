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

import java.io.File;
import java.util.*;

public class JarRootEntry extends AbstractJarEntry implements ClassStorage {
    final Object syncObject = new Object();
    final File file;
    final Map<String, JarClassEntry> classTree;
    final Map<String, JarClassEntry> allClasses;

    public JarRootEntry(File file) {
        super(file.getName());

        this.file = file;
        this.classTree = new TreeMap<>(Comparator.naturalOrder());
        this.allClasses = new TreeMap<>();
    }

    @Override
    public JarClassEntry getClass(String name, JarClassEntry.Populator populator) {
        if (name == null) {
            return null;
        }

        JarClassEntry entry = allClasses.get(name);
        if (entry == null && populator != null) {
            entry = new JarClassEntry(name);
            entry.populate(populator);
            synchronized (syncObject) {
                allClasses.put(name, entry);
                if (!entry.hasDeclaringClass() && !entry.hasEnclosingClass()) {
                    classTree.put(name, entry);
                }
            }
        }

        return entry;
    }

    public Collection<JarClassEntry> getClasses() {
        return classTree.values();
    }

    public Collection<JarClassEntry> getAllClasses() {
        return allClasses.values();
    }
}
