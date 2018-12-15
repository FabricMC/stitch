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

package net.fabricmc.stitch.util;

import java.util.*;

public final class StitchUtil {
    private StitchUtil() {

    }

    public static String join(String joiner, Collection<String> c) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (String s : c) {
            if ((i++) > 0) {
                builder.append(joiner);
            }

            builder.append(s);
        }
        return builder.toString();
    }

    public static <T> Set<T> newIdentityHashSet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    public static List<String> mergePreserveOrder(List<String> first, List<String> second) {
        List<String> out = new ArrayList<>();
        int i = 0;
        int j = 0;

        while (i < first.size() || j < second.size()) {
            while (i < first.size() && j < second.size()
                    && first.get(i).equals(second.get(j))) {
                out.add(first.get(i));
                i++;
                j++;
            }

            while (i < first.size() && !second.contains(first.get(i))) {
                out.add(first.get(i));
                i++;
            }

            while (j < second.size() && !first.contains(second.get(j))) {
                out.add(second.get(j));
                j++;
            }
        }

        return out;
    }

    public static long getTime() {
        return new Date().getTime();
    }
}
