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

import net.fabricmc.stitch.Command;
import net.fabricmc.stitch.util.RecordValidator;

import java.io.File;
import java.io.FileNotFoundException;

public class CommandValidateRecords extends Command {
	public CommandValidateRecords() {
		super("validateRecords");
	}

	@Override
	public String getHelpString() {
		return "<jar>";
	}

	@Override
	public boolean isArgumentCountValid(int count) {
		return count == 1;
	}

	@Override
	public void run(String[] args) throws Exception {
		File file = new File(args[0]);

		if (!file.exists() || !file.isFile()) {
			throw new FileNotFoundException("JAR could not be found!");
		}

		try (RecordValidator validator = new RecordValidator(file, true)) {
			try {
				validator.validate();
			} catch (RecordValidator.RecordValidationException e) {
				for (String error : e.errors) {
					System.err.println(error);
				}
				throw e;
			}
		}

		System.out.println("Record validation successful!");
	}
}
