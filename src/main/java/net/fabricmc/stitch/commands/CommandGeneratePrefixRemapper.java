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

import net.fabricmc.stitch.Command;

import java.io.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class CommandGeneratePrefixRemapper extends Command {
	public CommandGeneratePrefixRemapper() {
		super("genPrefixedTiny");
	}

	@Override
	public String getHelpString() {
		return "<input JAR> <prefix> <output tinymap> [inname] [outname]";
	}

	@Override
	public boolean isArgumentCountValid(int count) {
		return count == 3 || count == 5;
	}

	@Override
	public void run(String[] args) throws Exception {
		try (FileInputStream fis = new FileInputStream(new File(args[0]));
		     FileOutputStream fos = new FileOutputStream(new File(args[2]));
		     OutputStreamWriter osw = new OutputStreamWriter(fos);
		     BufferedWriter writer = new BufferedWriter(osw)) {
			writer.write("v1\t" + (args.length >= 5 ? args[3] : "input")  + "\t" + (args.length >= 5 ? args[4] : "output") + "\n");

			JarInputStream jis = new JarInputStream(fis);
			JarEntry entry;

			while ((entry = jis.getNextJarEntry()) != null) {
				if (entry.getName().endsWith(".class") && entry.getName().indexOf('/') < 0) {
					String cn = entry.getName().substring(0, entry.getName().length() - 6);
					writer.write("CLASS\t" + cn + "\t" + args[1] + cn + "\n");
				}
			}
		}

		System.out.println("Done!");
	}
}
