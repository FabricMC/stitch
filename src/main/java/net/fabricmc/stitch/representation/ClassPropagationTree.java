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

import net.fabricmc.stitch.util.StitchUtil;

import java.util.*;

/**
 * TODO: This doesn't try to follow the JVM's logic at all.
 * It does work, but it might be keeping some names the same
 * where it could get away with naming them differently.
 */
public class ClassPropagationTree {
    private final ClassStorage jar;
    private final Set<JarClassEntry> relevantClasses;
    private final Set<JarClassEntry> topmostClasses;

    public ClassPropagationTree(ClassStorage jar, JarClassEntry baseClass) {
        this.jar = jar;
        relevantClasses = StitchUtil.newIdentityHashSet();
        topmostClasses = StitchUtil.newIdentityHashSet();

        LinkedList<JarClassEntry> queue = new LinkedList<>();
        queue.add(baseClass);

        while (!queue.isEmpty()) {
            JarClassEntry entry = queue.remove();
            if (entry == null || relevantClasses.contains(entry)) {
                continue;
            }
            relevantClasses.add(entry);

            int qSize = queue.size();
            queue.addAll(entry.getSubclasses(jar));
            queue.addAll(entry.getImplementers(jar));
            if (qSize == queue.size()) {
                topmostClasses.add(entry);
            }

            queue.addAll(entry.getInterfaces(jar));
            JarClassEntry superClass = entry.getSuperClass(jar);
            if (superClass != null) {
                queue.add(superClass);
            }
        }
    }

    public Collection<JarClassEntry> getClasses() {
        return Collections.unmodifiableSet(relevantClasses);
    }

    public Collection<JarClassEntry> getTopmostClasses() {
        return Collections.unmodifiableSet(topmostClasses);
    }
}
