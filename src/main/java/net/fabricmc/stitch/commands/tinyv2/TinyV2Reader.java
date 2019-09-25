package net.fabricmc.stitch.commands.tinyv2;

import net.fabricmc.mapping.reader.v2.MappingGetter;
import net.fabricmc.mapping.reader.v2.TinyMetadata;
import net.fabricmc.mapping.reader.v2.TinyV2Factory;
import net.fabricmc.mapping.reader.v2.TinyVisitor;


import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
           return new ArrayList<>(Arrays.asList(getter.getRawNames()));
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
