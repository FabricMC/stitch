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
