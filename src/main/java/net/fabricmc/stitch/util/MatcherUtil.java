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

import net.fabricmc.tinyremapper.TinyUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.BiConsumer;

public final class MatcherUtil {
    private MatcherUtil() {

    }

    public static void read(BufferedReader reader, BiConsumer<String, String> classMappingConsumer, BiConsumer<TinyUtils.Mapping, TinyUtils.Mapping> fieldMappingConsumer, BiConsumer<TinyUtils.Mapping, TinyUtils.Mapping> methodMappingConsumer) throws IOException {
        String line;
        String ownerFrom = null, ownerTo = null;

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("\t");

            if (parts[0].equals("c") && parts.length == 3) {
                // class
                ownerFrom = parts[1].substring(1, parts[1].length() - 1);
                ownerTo = parts[2].substring(1, parts[2].length() - 1);
                classMappingConsumer.accept(ownerFrom, ownerTo);
            } else if (parts[0].equals("") && ownerFrom != null && parts.length >= 2) {
                if (parts[1].equals("f") && parts.length == 4) {
                    String[] fieldFrom = parts[2].split(";;");
                    String[] fieldTo = parts[3].split(";;");
                    fieldMappingConsumer.accept(
                            new TinyUtils.Mapping(ownerFrom, fieldFrom[0], fieldFrom[1]),
                            new TinyUtils.Mapping(ownerTo, fieldTo[0], fieldTo[1])
                    );
                } else if (parts[1].equals("m") && parts.length == 4) {
                    String[] methodFrom = toMethodArray(parts[2]);
                    String[] methodTo = toMethodArray(parts[3]);
                    methodMappingConsumer.accept(
                            new TinyUtils.Mapping(ownerFrom, methodFrom[0], methodFrom[1]),
                            new TinyUtils.Mapping(ownerTo, methodTo[0], methodTo[1])
                    );
                }
            }
        }
    }

    private static String[] toMethodArray(String part) {
        int parenPos = part.indexOf('(');
        return new String[] {
                part.substring(0, parenPos),
                part.substring(parenPos)
        };
    }
}