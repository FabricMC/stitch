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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import net.fabricmc.mapping.reader.v2.MappingGetter;
import net.fabricmc.mapping.reader.v2.TinyMetadata;
import net.fabricmc.mapping.reader.v2.TinyV2Factory;
import net.fabricmc.mapping.reader.v2.TinyVisitor;

public class TinyV2Reader {
	private static class Visitor implements TinyVisitor {
		private enum CommentType {
			CLASS,
			FIELD,
			METHOD,
			PARAMETER,
			LOCAL_VARIABLE
		}

		private TinyHeader header;
		private int namespaceAmount;
		//        private String
		private Set<TinyClass> classes = new HashSet<>();

		private TinyClass currentClass;
		private TinyField currentField;
		private TinyMethod currentMethod;
		private TinyMethodParameter currentParameter;
		private TinyLocalVariable currentLocalVariable;
		private CommentType currentCommentType;


		private List<String> getNames(MappingGetter getter) {
			return Lists.newArrayList(getter.getRawNames());
		}

		@Override
		public void start(TinyMetadata metadata) {
			header = new TinyHeader(new ArrayList<>(metadata.getNamespaces()), metadata.getMajorVersion(), metadata.getMinorVersion(),
							metadata.getProperties());
			namespaceAmount = header.getNamespaces().size();
		}

		@Override
		public void pushClass(MappingGetter name) {
			currentClass = new TinyClass(getNames(name), new HashSet<>(), new HashSet<>(), new ArrayList<>());
			classes.add(currentClass);
			currentCommentType = CommentType.CLASS;
		}

		@Override
		public void pushField(MappingGetter name, String descriptor) {
			currentField = new TinyField(descriptor, getNames(name), new ArrayList<>());
			currentClass.getFields().add(currentField);
			currentCommentType = CommentType.FIELD;
		}

		@Override
		public void pushMethod(MappingGetter name, String descriptor) {
			currentMethod = new TinyMethod(
							descriptor, getNames(name), new HashSet<>(), new HashSet<>(), new ArrayList<>()
			);
			currentClass.getMethods().add(currentMethod);
			currentCommentType = CommentType.METHOD;
		}

		@Override
		public void pushParameter(MappingGetter name, int localVariableIndex) {
			currentParameter = new TinyMethodParameter(
							localVariableIndex, getNames(name), new ArrayList<>()
			);
			currentMethod.getParameters().add(currentParameter);
			currentCommentType = CommentType.PARAMETER;
		}

		@Override
		public void pushLocalVariable(MappingGetter name, int localVariableIndex, int localVariableStartOffset, int localVariableTableIndex) {
			currentLocalVariable = new TinyLocalVariable(
							localVariableIndex, localVariableStartOffset, localVariableTableIndex, getNames(name), new ArrayList<>()
			);
			currentMethod.getLocalVariables().add(currentLocalVariable);
			currentCommentType = CommentType.LOCAL_VARIABLE;
		}

		@Override
		public void pushComment(String comment) {
			switch (currentCommentType) {
			case CLASS:
				currentClass.getComments().add(comment);
				break;
			case FIELD:
				currentField.getComments().add(comment);
				break;
			case METHOD:
				currentMethod.getComments().add(comment);
				break;
			case PARAMETER:
				currentParameter.getComments().add(comment);
				break;
			case LOCAL_VARIABLE:
				currentLocalVariable.getComments().add(comment);
				break;
			default:
				throw new RuntimeException("unexpected comment without parent");
			}
		}

		@Override
		public void pop(int count) {

		}

		private TinyFile getAST() {
			return new TinyFile(header, classes);
		}
	}

	public static TinyFile read(Path readFrom) throws IOException {
		Visitor visitor = new Visitor();
		try (BufferedReader reader = Files.newBufferedReader(readFrom)) {
			TinyV2Factory.visit(reader, visitor);
		}

		return visitor.getAST();
	}
}
