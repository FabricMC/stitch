/*
 * Copyright (c) 2016, 2017, 2018, 2019 Adrian Siekierka
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
import net.fabricmc.stitch.util.MatcherUtil;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class CommandMatcherToTiny extends Command {
	public CommandMatcherToTiny() {
		super("matcherToTiny");
	}

	@Override
	public String getHelpString() {
		return "<in> <out> <src-name> <dst-name>";
	}

	@Override
	public boolean isArgumentCountValid(int count) {
		return count == 4;
	}

	@Override
	public void run(String[] args) throws Exception {
		Map<String, String> classNames = new HashMap<>();
		Map<String, String> fieldNames = new HashMap<>();
		Map<String, String> methodNames = new HashMap<>();

		System.out.println("Loading...");
		try (
				FileInputStream fis = new FileInputStream(new File(args[0]));
				InputStreamReader isr = new InputStreamReader(fis);
				BufferedReader reader = new BufferedReader(isr)
				) {

			MatcherUtil.read(reader, false,
					classNames::put,
					(src, dst) -> fieldNames.put(src.getOwner() + "\t" + src.getDesc() + "\t" + src.getName(), dst.getName()),
					(src, dst) -> methodNames.put(src.getOwner() + "\t" + src.getDesc() + "\t" + src.getName(), dst.getName())
			);
		}

		System.out.println("Saving...");
		try (
				FileOutputStream fos = new FileOutputStream(new File(args[1]));
				OutputStreamWriter osw = new OutputStreamWriter(fos);
				BufferedWriter writer = new BufferedWriter(osw)
				) {

			writer.write("v1\t" + args[2] + "\t" + args[3] + "\n");

			for (String s : classNames.keySet()) {
				writer.write("CLASS\t" + s + "\t" + classNames.get(s) + "\n");
			}

			for (String s : fieldNames.keySet()) {
				writer.write("FIELD\t" + s + "\t" + fieldNames.get(s) + "\n");
			}

			for (String s : methodNames.keySet()) {
				writer.write("METHOD\t" + s + "\t" + methodNames.get(s) + "\n");
			}
		}

		System.out.println("Done!");
	}
}
