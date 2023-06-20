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

package net.fabricmc.stitch.plugin;

import net.fabricmc.stitch.representation.ClassStorage;
import net.fabricmc.stitch.representation.JarClassEntry;
import net.fabricmc.stitch.representation.JarFieldEntry;
import net.fabricmc.stitch.representation.JarMethodEntry;

public interface StitchPlugin {
	/**
	 * Whether or not the passed class needs an intermediary name.
	 * A positive number means true, a negative number means false.
	 * The returned integer's value represents the result's priority;
	 * a higher priority will overwrite other plugins' results.
	 * Return at least +/-2 to overwrite the default plugin.
	 */
	default int needsIntermediaryName(ClassStorage storage, JarClassEntry cls) {
		return cls.isAnonymous() ? -1 : 1;
	}

	/**
	 * Whether or not the passed field needs an intermediary name.
	 * A positive number means true, a negative number means false.
	 * The returned integer's value represents the result's priority;
	 * a higher priority will overwrite other plugins' results.
	 * Return at least +/-2 to overwrite the default plugin.
	 */
	default int needsIntermediaryName(ClassStorage storage, JarClassEntry cls, JarFieldEntry fld) {
		return 1;
	}

	/**
	 * Whether or not the passed method needs an intermediary name.
	 * A positive number means true, a negative number means false.
	 * The returned integer's value represents the result's priority;
	 * a higher priority will overwrite other plugins' results.
	 * Return at least +/-2 to overwrite the default plugin.
	 */
	default int needsIntermediaryName(ClassStorage storage, JarClassEntry cls, JarMethodEntry mth) {
		String name = mth.getName();

		return name.charAt(0) != '<' && mth.isSource(storage, cls) ? 1 : -1;
	}

	/**
	 * Determines the target package where intermediary classes
	 * get moved into. Must have a trailing slash.
	 * Example: {@code my/toplevel/package/}
	 */
	default String getIntermediaryTargetPackage() {
		return null;
	}
}
