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
import net.fabricmc.stitch.util.StitchUtil;
import org.objectweb.asm.commons.Remapper;

import javax.annotation.Nullable;
import java.util.*;

public class MethodEntry extends Entry {
    protected String desc;
    protected String signature;

    protected MethodEntry(int access, String name, String desc, String signature) {
        super(name);
        this.setAccess(access);
        this.desc = desc;
        this.signature = signature;
    }

    public String getDescriptor() {
        return desc;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    protected String getKey() {
        return super.getKey() + desc;
    }

    public boolean isSource(ClassStorage storage, ClassEntry c) {
        Set<ClassEntry> entries = StitchUtil.newIdentityHashSet();
        entries.add(c);
        getMatchingSources(entries, storage, c);
        return entries.size() == 1;
    }

    public List<ClassEntry> getMatchingEntries(ClassStorage storage, ClassEntry c) {
        if (Access.isPrivateOrStatic(getAccess())) {
            return Collections.singletonList(c);
        }

        Set<ClassEntry> entries = StitchUtil.newIdentityHashSet();
        Set<ClassEntry> entriesNew = StitchUtil.newIdentityHashSet();
        entries.add(c);
        int lastSize = 0;

        while (entries.size() > lastSize) {
            lastSize = entries.size();

            for (ClassEntry cc : entries) {
                getMatchingSources(entriesNew, storage, cc);
            }
            entries.addAll(entriesNew);
            entriesNew.clear();

            for (ClassEntry cc : entries) {
                getMatchingEntries(entriesNew, storage, cc, 0);
            }
            entries.addAll(entriesNew);
            entriesNew.clear();
        }

        return new ArrayList<>(entries);
    }

    void getMatchingSources(Collection<ClassEntry> entries, ClassStorage storage, ClassEntry c) {
        MethodEntry m = c.getMethod(getKey());
        if (m != null) {
            if (!Access.isPrivateOrStatic(m.getAccess())) {
                entries.add(c);
            }
        }

        ClassEntry superClass = c.getSuperClass(storage);
        if (superClass != null) {
            getMatchingSources(entries, storage, superClass);
        }

        for (ClassEntry itf : c.getInterfaces(storage)) {
            getMatchingSources(entries, storage, itf);
        }
    }

    void getMatchingEntries(Collection<ClassEntry> entries, ClassStorage storage, ClassEntry c, int indent) {
        MethodEntry m = c.getMethod(getKey());
        if (m != null) {
            if (!Access.isPrivateOrStatic(m.getAccess())) {
                entries.add(c);
            }
        }

        for (ClassEntry cc : c.getSubclasses(storage)) {
            getMatchingEntries(entries, storage, cc, indent + 1);
        }

        for (ClassEntry cc : c.getImplementers(storage)) {
            getMatchingEntries(entries, storage, cc, indent + 1);
        }
    }

    public void remap(ClassEntry classEntry, String oldOwner, Remapper remapper) {
        String pastDesc = desc;

        name = remapper.mapMethodName(oldOwner, name, pastDesc);
        desc = remapper.mapMethodDesc(pastDesc);
    }
}