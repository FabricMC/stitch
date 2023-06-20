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

package net.fabricmc.stitch.plugins;

import java.util.regex.Pattern;

import net.fabricmc.stitch.plugin.StitchPlugin;
import net.fabricmc.stitch.representation.ClassStorage;
import net.fabricmc.stitch.representation.JarClassEntry;
import net.fabricmc.stitch.representation.JarFieldEntry;
import net.fabricmc.stitch.representation.JarMethodEntry;

public class MinecraftPlugin implements StitchPlugin {
	// Minecraft classes without a package are obfuscated.
	private static Pattern classObfuscationPattern = Pattern.compile("^[^/]*$");

	@Override
	public int needsIntermediaryName(ClassStorage storage, JarClassEntry cls) {
		return StitchPlugin.super.needsIntermediaryName(storage, cls) > 0
				&& classObfuscationPattern.matcher(cls.getName()).matches() ? 2 : -2;
	}

	@Override
	public int needsIntermediaryName(ClassStorage storage, JarClassEntry cls, JarFieldEntry fld) {
		String name = fld.getName();

		return name.length() <= 2 || (name.length() == 3 && name.charAt(2) == '_') ? 2 : -2;
	}

	@Override
	public int needsIntermediaryName(ClassStorage storage, JarClassEntry cls, JarMethodEntry mth) {
		String name = mth.getName();

		return (name.length() <= 2 || (name.length() == 3 && name.charAt(2) == '_'))
				&& StitchPlugin.super.needsIntermediaryName(storage, cls, mth) > 0 ? 2 : -2;
	}

	@Override
	public String getIntermediaryTargetPackage() {
		return "net/minecraft/";
	}
}
