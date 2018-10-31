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

public abstract class Entry {
    private final String name;
    private int access;

    public Entry(String name) {
        this.name = name;
    }

    public int getAccess() {
        return access;
    }

    protected void setAccess(int value) {
        this.access = value;
    }

    public String getName() {
        return name;
    }

    protected String getKey() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        return other != null && other.getClass() == getClass() && ((Entry) other).getKey().equals(getKey());
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getKey() + ")";
    }
}
