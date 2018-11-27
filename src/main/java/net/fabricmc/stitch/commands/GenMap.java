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
import net.fabricmc.tinyremapper.TinyUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class GenMap {
    public static class DescEntry {
        private final String owner;
        private final String name;
        private final String desc;

        public DescEntry(String owner, String name, String desc) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
        }

        public DescEntry(TinyUtils.Mapping mapping) {
            this.owner = mapping.owner;
            this.name = mapping.name;
            this.desc = mapping.desc;
        }

        public String getOwner() {
            return owner;
        }

        public String getName() {
            return name;
        }

        public String getDesc() {
            return desc;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof DescEntry)) {
                return false;
            } else {
                DescEntry other = (DescEntry) o;
                return owner.equals(other.owner) && name.equals(other.name) && desc.equals(other.desc);
            }
        }

        @Override
        public int hashCode() {
            return 19 * name.hashCode() + desc.hashCode();
        }
    }

    private static class Class {
        private final String name;
        private final Map<DescEntry, DescEntry> fieldMaps = new HashMap<>();
        private final Map<DescEntry, DescEntry> methodMaps = new HashMap<>();

        public Class(String name) {
            this.name = name;
        }
    }

    private final Map<String, Class> map = new HashMap<>();
    private final boolean inverse;

    public GenMap(boolean inverse) {
        this.inverse = inverse;
    }

    public void addClass(String from, String to) {
        if (inverse) {
            if (!map.containsKey(to)) {
                map.put(to, new Class(from));
            }
        } else {
            if (!map.containsKey(from)) {
                map.put(from, new Class(to));
            }
        }
    }

    public void addField(TinyUtils.Mapping from, TinyUtils.Mapping to) {
        if (inverse) {
            TinyUtils.Mapping tmp = from;
            from = to;
            to = tmp;
        }

        if (!map.containsKey(from.owner)) {
            throw new RuntimeException("!?");
        }

        map.get(from.owner).fieldMaps.put(new DescEntry(from), new DescEntry(to));
    }

    public void addMethod(TinyUtils.Mapping from, TinyUtils.Mapping to) {
        if (inverse) {
            TinyUtils.Mapping tmp = from;
            from = to;
            to = tmp;
        }

        if (!map.containsKey(from.owner)) {
            throw new RuntimeException("!?");
        }

        map.get(from.owner).methodMaps.put(new DescEntry(from), new DescEntry(to));
    }

    @Nullable
    public String getClass(String from) {
        return map.containsKey(from) ? map.get(from).name : null;
    }

    @Nullable
    private DescEntry get(DescEntry entry, Function<Class, Map<DescEntry, DescEntry>> mapGetter) {
        if (map.containsKey(entry.owner)) {
            return mapGetter.apply(map.get(entry.owner)).get(entry);
        }

        return null;
    }

    @Nullable
    public DescEntry getField(String owner, String name, String desc) {
        return get(new DescEntry(owner, name, desc), (c) -> c.fieldMaps);
    }

    @Nullable
    public DescEntry getField(DescEntry entry) {
        return get(entry, (c) -> c.fieldMaps);
    }

    @Nullable
    public DescEntry getMethod(String owner, String name, String desc) {
        return get(new DescEntry(owner, name, desc), (c) -> c.methodMaps);
    }

    @Nullable
    public DescEntry getMethod(DescEntry entry) {
        return get(entry, (c) -> c.methodMaps);
    }
}
