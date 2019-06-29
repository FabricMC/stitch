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

package net.fabricmc.stitch.util;

import java.util.Objects;

public final class Pair<K, V> {
    private final K left;
    private final V right;

    private Pair(K left, V right) {
        this.left = left;
        this.right = right;
    }

    public static <K, V> Pair<K, V> of(K left, V right) {
        return new Pair<>(left, right);
    }

    public K getLeft() {
        return left;
    }

    public V getRight() {
        return right;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        //noinspection unchecked
        return new Pair(left, right);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair)) {
            return false;
        } else {
            Pair other = (Pair) o;
            return Objects.equals(other.left, left) && Objects.equals(other.right, right);
        }
    }

    @Override
    public int hashCode() {
        if (left == null && right == null) {
            return 0;
        } else if (left == null) {
            return right.hashCode();
        } else if (right == null) {
            return left.hashCode();
        } else {
            return left.hashCode() * 19 + right.hashCode();
        }
    }

    @Override
    public String toString() {
        return "Pair(" + left + "," + right + ")";
    }
}
