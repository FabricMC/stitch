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

package net.fabricmc.stitch.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;

import net.fabricmc.mapping.util.EntryTriple;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MappingTree.MemberMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;

public class GenMap {
	private static class Class {
		private final String name;
		private final Map<EntryTriple, EntryTriple> fieldMaps = new HashMap<>();
		private final Map<EntryTriple, EntryTriple> methodMaps = new HashMap<>();

		Class(String name) {
			this.name = name;
		}
	}

	private final Map<String, Class> map = new HashMap<>();

	public GenMap() {
	}

	public void addClass(String from, String to) {
		map.put(from, new Class(to));
	}

	public void addField(EntryTriple from, EntryTriple to) {
		map.get(from.getOwner()).fieldMaps.put(from, to);
	}

	public void addMethod(EntryTriple from, EntryTriple to) {
		map.get(from.getOwner()).methodMaps.put(from, to);
	}

	public void load(MappingTree mappings) {
		for (ClassMapping cls : mappings.getClasses()) {
			map.put(cls.getSrcName(), new Class(cls.getDstName(0)));

			for (FieldMapping fld : cls.getFields()) {
				map.get(fld.getOwner().getSrcName()).fieldMaps.put(srcEntryTripleOf(fld), dstEntryTripleOf(fld));
			}

			for (MethodMapping mth : cls.getMethods()) {
				map.get(mth.getOwner().getSrcName()).methodMaps.put(srcEntryTripleOf(mth), dstEntryTripleOf(mth));
			}
		}
	}

	private EntryTriple srcEntryTripleOf(MemberMapping member) {
		return new EntryTriple(member.getOwner().getSrcName(), member.getSrcName(), member.getSrcDesc());
	}

	private EntryTriple dstEntryTripleOf(MemberMapping member) {
		return new EntryTriple(member.getOwner().getDstName(0), member.getDstName(0), member.getDstDesc(0));
	}

	@Nullable
	public String getClass(String from) {
		return map.containsKey(from) ? map.get(from).name : null;
	}

	@Nullable
	private EntryTriple get(EntryTriple entry, Function<Class, Map<EntryTriple, EntryTriple>> mapGetter) {
		if (map.containsKey(entry.getOwner())) {
			return mapGetter.apply(map.get(entry.getOwner())).get(entry);
		}

		return null;
	}

	@Nullable
	public EntryTriple getField(String owner, String name, String desc) {
		return get(new EntryTriple(owner, name, desc), (c) -> c.fieldMaps);
	}

	@Nullable
	public EntryTriple getField(EntryTriple entry) {
		return get(entry, (c) -> c.fieldMaps);
	}

	@Nullable
	public EntryTriple getMethod(String owner, String name, String desc) {
		return get(new EntryTriple(owner, name, desc), (c) -> c.methodMaps);
	}

	@Nullable
	public EntryTriple getMethod(EntryTriple entry) {
		return get(entry, (c) -> c.methodMaps);
	}

	public static class Dummy extends GenMap {
		public Dummy() {
		}

		@Nullable
		@Override
		public String getClass(String from) {
			return from;
		}

		@Nullable
		@Override
		public EntryTriple getField(String owner, String name, String desc) {
			return new EntryTriple(owner, name, desc);
		}

		@Nullable
		@Override
		public EntryTriple getField(EntryTriple entry) {
			return entry;
		}

		@Nullable
		@Override
		public EntryTriple getMethod(String owner, String name, String desc) {
			return new EntryTriple(owner, name, desc);
		}

		@Nullable
		@Override
		public EntryTriple getMethod(EntryTriple entry) {
			return entry;
		}
	}
}
