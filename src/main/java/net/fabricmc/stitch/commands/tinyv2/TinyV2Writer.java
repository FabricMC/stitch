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

package net.fabricmc.stitch.commands.tinyv2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TinyV2Writer {
	public static void write(TinyFile tinyFile, Path writeTo) throws IOException {
		new TinyV2Writer().instanceWrite(tinyFile, writeTo);
	}

	private static class Prefixes {
		private Prefixes() {
		}

		public static final String PARAMETER = "p";
		public static final String METHOD = "m";
		public static final String VARIABLE = "v";
		public static final String HEADER = "tiny";
		public static final String FIELD = "f";
		public static final String COMMENT = "c";
		public static final String CLASS = "c";
	}

	private static class Indents {
		private Indents() {
		}

		public static final int HEADER = 0;
		public static final int PROPERTY = 1;
		public static final int CLASS = 0;
		public static final int METHOD = 1;
		public static final int FIELD = 1;
		public static final int PARAMETER = 2;
		public static final int LOCAL_VARIABLE = 2;

		public static final int CLASS_COMMENT = 1;
		public static final int METHOD_COMMENT = 2;
		public static final int FIELD_COMMENT = 2;
		public static final int PARAMETER_COMMENT = 3;
		public static final int LOCAL_VARIABLE_COMMENT = 3;


	}

	private TinyV2Writer() {
	}

	private BufferedWriter writer;

	private void instanceWrite(TinyFile tinyFile, Path writeTo) throws IOException {
		writer = Files.newBufferedWriter(writeTo);
		writeHeader(tinyFile.getHeader());
		tinyFile.getClassEntries().stream().sorted().forEach(this::writeClass);

		writer.close();
	}


	private void writeHeader(TinyHeader header) {
		writeLine(Indents.HEADER, header.getNamespaces(), Prefixes.HEADER,
						Integer.toString(header.getMajorVersion()), Integer.toString(header.getMinorVersion()));
		header.getProperties().forEach((key, value) -> writeLine(Indents.PROPERTY, value));
	}

	private void writeClass(TinyClass tinyClass) {
		writeLine(Indents.CLASS, tinyClass.getClassNames(), Prefixes.CLASS);

		tinyClass.getMethods().stream().sorted().forEach(this::writeMethod);
		tinyClass.getFields().stream().sorted().forEach(this::writeField);

		for (String comment : tinyClass.getComments()) writeComment(Indents.CLASS_COMMENT, comment);

	}

	private void writeMethod(TinyMethod method) {
		writeLine(Indents.METHOD, method.getMethodNames(), Prefixes.METHOD, method.getMethodDescriptorInFirstNamespace());

		method.getParameters().stream().sorted().forEach(this::writeMethodParameter);
		method.getLocalVariables().stream().sorted().forEach(this::writeLocalVariable);

		for (String comment : method.getComments()) writeComment(Indents.METHOD_COMMENT, comment);

	}

	private void writeMethodParameter(TinyMethodParameter parameter) {
		writeLine(Indents.PARAMETER, parameter.getParameterNames(), Prefixes.PARAMETER, Integer.toString(parameter.getLvIndex()));
		for (String comment : parameter.getComments()) {
			writeComment(Indents.PARAMETER_COMMENT, comment);
		}

	}

	private void writeLocalVariable(TinyLocalVariable localVariable) {
		writeLine(Indents.LOCAL_VARIABLE, localVariable.getLocalVariableNames(), Prefixes.VARIABLE,
						Integer.toString(localVariable.getLvIndex()), Integer.toString(localVariable.getLvStartOffset()),
						Integer.toString(localVariable.getLvTableIndex())
		);

		for (String comment : localVariable.getComments()) {
			writeComment(Indents.LOCAL_VARIABLE_COMMENT, comment);
		}
	}

	private void writeField(TinyField field) {
		writeLine(Indents.FIELD, field.getFieldNames(), Prefixes.FIELD, field.getFieldDescriptorInFirstNamespace());
		for (String comment : field.getComments()) writeComment(Indents.FIELD_COMMENT, comment);
	}


	private void writeComment(int indentLevel, String comment) {
		writeLine(indentLevel, Prefixes.COMMENT, comment);
	}


	private void write(int indentLevel, String... tabSeparatedStrings) {
		try {
			for (int i = 0; i < indentLevel; i++) writer.write('\t');

			writer.write(String.join("\t", tabSeparatedStrings));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// names are written after the other strings
	private void write(int indentLevel, List<String> names, String... tabSeparatedStrings) {
		try {
			write(indentLevel, tabSeparatedStrings);
			writer.write("\t");
			writer.write(String.join("\t", names));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeLine() {
		try {
			writer.write('\n');
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeLine(int indentLevel, List<String> names, String... tabSeparatedStrings) {
		write(indentLevel, names, tabSeparatedStrings);
		writeLine();
	}

	private void writeLine(int indentLevel, String... tabSeparatedStrings) {
		write(indentLevel, tabSeparatedStrings);
		writeLine();
	}
}
