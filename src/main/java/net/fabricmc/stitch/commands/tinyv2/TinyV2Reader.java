package net.fabricmc.stitch.commands.tinyv2;

import net.fabricmc.tinyv2.MappingGetter;
import net.fabricmc.tinyv2.TinyMetadata;
import net.fabricmc.tinyv2.TinyV2Factory;
import net.fabricmc.tinyv2.TinyVisitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

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
        private List<TinyClass> classes = new ArrayList<>();

        private TinyClass currentClass;
        private TinyField currentField;
        private TinyMethod currentMethod;
        private TinyMethodParameter currentParameter;
        private TinyLocalVariable currentLocalVariable;
        private CommentType currentCommentType;


        @Override
        public void start(TinyMetadata metadata) {
            header = new TinyHeader(metadata.getNamespaces(), metadata.getMajorVersion(), metadata.getMinorVersion(),
                    metadata.getProperties());
        }

        @Override
        public void pushClass(MappingGetter name) {
            currentClass = new TinyClass(Arrays.asList(name.getAll()), new HashSet<>(), new HashSet<>(), new ArrayList<>());
            classes.add(currentClass);
            currentCommentType = CommentType.CLASS;
        }

        @Override
        public void pushField(MappingGetter name, String descriptor) {
            currentField = new TinyField(descriptor, Arrays.asList(name.getAll()), new ArrayList<>());
            currentClass.getFields().add(currentField);
            currentCommentType = CommentType.FIELD;
        }

        @Override
        public void pushMethod(MappingGetter name, String descriptor) {
            currentMethod = new TinyMethod(
                    descriptor, Arrays.asList(name.getAll()), new HashSet<>(), new HashSet<>(), new ArrayList<>()
            );
            currentClass.getMethods().add(currentMethod);
            currentCommentType = CommentType.METHOD;
        }

        @Override
        public void pushParameter(MappingGetter name, int localVariableIndex) {
            currentParameter = new TinyMethodParameter(
                    localVariableIndex, Arrays.asList(name.getAll()), new ArrayList<>()
            );
            currentMethod.getParameters().add(currentParameter);
            currentCommentType = CommentType.PARAMETER;
        }

        @Override
        public void pushLocalVariable(MappingGetter name, int localVariableIndex, int localVariableStartOffset, int localVariableTableIndex) {
            currentLocalVariable = new TinyLocalVariable(
                    localVariableIndex, localVariableStartOffset, localVariableTableIndex, Arrays.asList(name.getAll()), new ArrayList<>()
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
            return new TinyFile(header,classes);
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
