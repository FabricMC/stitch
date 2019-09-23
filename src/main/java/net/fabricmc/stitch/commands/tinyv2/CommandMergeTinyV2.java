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

import net.fabricmc.stitch.Command;
import net.fabricmc.stitch.util.Pair;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CommandMergeTinyV2 extends Command {
    public CommandMergeTinyV2() {
        super("mergeTinyV2");
    }

    @Override
    public String getHelpString() {
        //TODO: mappingsBlankFillOrder?
        return "<input-a> <input-b> <output>";
    }

    @Override
    public boolean isArgumentCountValid(int count) {
        return count == 3;
    }

    @Override
    public void run(String[] args) throws IOException {
        Path inputA = Paths.get(args[0]);
        Path inputB = Paths.get(args[1]);
        System.out.println("Reading " + inputA);
        TinyFile tinyFileA = TinyV2Reader.read(inputA);
        System.out.println("Reading " + inputB);
        TinyFile tinyFileB = TinyV2Reader.read(inputB);
        TinyHeader headerA = tinyFileA.getHeader();
        TinyHeader headerB = tinyFileB.getHeader();
        if (headerA.getNamespaces().size() != 2) {
            throw new IllegalArgumentException(inputA + " must have exactly 2 namespaces.");
        }
        if (headerB.getNamespaces().size() != 2) {
            throw new IllegalArgumentException(inputB + " must have exactly 2 namespaces.");
        }

        if (!headerA.getNamespaces().get(0).equals(headerB.getNamespaces().get(0))) {
            throw new IllegalArgumentException(
                    String.format("The input tiny files must have the same namespaces as the first column. " +
                                    "(%s has %s while %s has %s)",
                            inputA, headerA.getNamespaces().get(0), inputB, headerB.getNamespaces().get(0))
            );
        }
        System.out.println("Merging " + inputA + " with " + inputB);
        TinyFile mergedFile = merge(tinyFileA, tinyFileB);

        TinyV2Writer.write(mergedFile, Paths.get(args[2]));
        System.out.println("Merged mappings written to " + Paths.get(args[2]));
    }


    private TinyFile merge(TinyFile inputA, TinyFile inputB) {
        //TODO: how to merge properties?

        TinyHeader mergedHeader = mergeHeaders(inputA.getHeader(), inputB.getHeader());

        List<String> keyUnion = keyUnion(inputA.getClassEntries(), inputB.getClassEntries());

        Map<String, TinyClass> inputAClasses = inputA.mapClassesByFirstNamespace();
        Map<String, TinyClass> inputBClasses = inputB.mapClassesByFirstNamespace();
        List<TinyClass> mergedClasses = map(keyUnion, key -> {
            TinyClass classA = inputAClasses.get(key);
            TinyClass classB = inputBClasses.get(key);

            classA = matchEnclosingClassIfNeeded(key, classA, inputAClasses);
            classB = matchEnclosingClassIfNeeded(key, classB, inputBClasses);
            return mergeClasses(key, classA, classB);
        });

        return new TinyFile(mergedHeader, mergedClasses);
    }

    private TinyClass matchEnclosingClassIfNeeded(String key, TinyClass tinyClass, Map<String, TinyClass> mappings) {
        if (tinyClass == null) {
            String partlyMatchedClassName = matchEnclosingClass(key, mappings);
                return new TinyClass(Arrays.asList(key, partlyMatchedClassName));
        } else {
            return tinyClass;
        }
    }

    /**
     * Takes something like net/minecraft/class_123$class_124 that doesn't have a mapping, tries to find net/minecraft/class_123
     * , say the mapping of net/minecraft/class_123 is path/to/someclass and then returns a class of the form
     * path/to/someclass$class124
     */
    @Nonnull
    private String matchEnclosingClass(String sharedName, Map<String, TinyClass> inputBClassBySharedNamespace) {
        String[] path = sharedName.split(escape("$"));
        int parts = path.length;
        for (int i = parts - 2; i >= 0; i--) {
            String currentPath = String.join("$", Arrays.copyOfRange(path, 0, i + 1));
            TinyClass match = inputBClassBySharedNamespace.get(currentPath);

            if (match != null && !match.getClassNames().get(1).isEmpty()) {
                return match.getClassNames().get(1)
                        + "$" + String.join("$", Arrays.copyOfRange(path, i + 1, path.length));

            }
        }

        return sharedName;
    }


    private TinyClass mergeClasses(String sharedClassName, @Nonnull TinyClass classA, @Nonnull TinyClass classB) {
        List<String> mergedNames = mergeNames(sharedClassName, classA, classB);
        List<String> mergedComments = mergeComments(classA.getComments(), classB.getComments());

        List<Pair<String,String>> methodKeyUnion = union(mapToFirstNamespaceAndDescriptor(classA), mapToFirstNamespaceAndDescriptor(classB));
        Map<Pair<String,String>, TinyMethod> methodsA = classA.mapMethodsByFirstNamespaceAndDescriptor();
        Map<Pair<String,String>, TinyMethod> methodsB = classB.mapMethodsByFirstNamespaceAndDescriptor();
        List<TinyMethod> mergedMethods = map(methodKeyUnion,
                (Pair<String,String> k) -> mergeMethods(k.getLeft(), methodsA.get(k), methodsB.get(k)));

        List<String> fieldKeyUnion = keyUnion(classA.getFields(), classB.getFields());
        Map<String, TinyField> fieldsA = classA.mapFieldsByFirstNamespace();
        Map<String, TinyField> fieldsB = classB.mapFieldsByFirstNamespace();
        List<TinyField> mergedFields = map(fieldKeyUnion, k -> mergeFields(k, fieldsA.get(k), fieldsB.get(k)));

        return new TinyClass(mergedNames, mergedMethods, mergedFields, mergedComments);
    }

    private static final TinyMethod EMPTY_METHOD = new TinyMethod(null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());


    private TinyMethod mergeMethods(String sharedMethodName, @Nullable TinyMethod methodA, @Nullable TinyMethod methodB) {
        List<String> mergedNames = mergeNames(sharedMethodName, methodA, methodB);
        if (methodA == null) methodA = EMPTY_METHOD;
        if (methodB == null) methodB = EMPTY_METHOD;
        List<String> mergedComments = mergeComments(methodA.getComments(), methodB.getComments());

        String descriptor = methodA.getMethodDescriptorInFirstNamespace() != null ? methodA.getMethodDescriptorInFirstNamespace()
                : methodB.getMethodDescriptorInFirstNamespace();
        if (descriptor == null) throw new RuntimeException("no descriptor for key " + sharedMethodName);


        //TODO: this won't work too well when the first namespace is named or there is more than one named namespace (hack)
        List<TinyMethodParameter> mergedParameters = new ArrayList<>();
        addParameters(methodA, mergedParameters, 2);
        addParameters(methodB, mergedParameters, 1);

        List<TinyLocalVariable> mergedLocalVariables = new ArrayList<>();
        addLocalVariables(methodA,mergedLocalVariables,2);
        addLocalVariables(methodB,mergedLocalVariables,1);

        return new TinyMethod(descriptor,mergedNames,mergedParameters,mergedLocalVariables,mergedComments);
    }

    private void addParameters(TinyMethod method, List<TinyMethodParameter> addTo, int emptySpacePos) {
        for (TinyMethodParameter localVariable : method.getParameters()) {
            List<String> names = new ArrayList<>(localVariable.getParameterNames());
            names.add(emptySpacePos, "");
            addTo.add(new TinyMethodParameter(localVariable.getLvIndex(), names, localVariable.getComments()));
        }
    }

    private void addLocalVariables(TinyMethod method, List<TinyLocalVariable> addTo, int emptySpacePos) {
        for (TinyLocalVariable localVariable : method.getLocalVariables()) {
            List<String> names = new ArrayList<>(localVariable.getLocalVariableNames());
            names.add(emptySpacePos, "");
            addTo.add(new TinyLocalVariable(localVariable.getLvIndex(), localVariable.getLvStartOffset(),
                    localVariable.getLvTableIndex(), names, localVariable.getComments()));
        }
    }


    private TinyField mergeFields(String sharedFieldName, @Nullable TinyField fieldA, @Nullable TinyField fieldB) {
        List<String> mergedNames =  mergeNames(sharedFieldName,fieldA, fieldB);
        List<String> mergedComments = mergeComments(fieldA != null? fieldA.getComments() : Collections.emptyList(),
                fieldB != null ?  fieldB.getComments() : Collections.emptyList());

        String descriptor = fieldA != null ? fieldA.getFieldDescriptorInFirstNamespace()
                : fieldB != null ? fieldB.getFieldDescriptorInFirstNamespace() : null;
        if (descriptor == null) throw new RuntimeException("no descriptor for key " + sharedFieldName);

        return new TinyField(descriptor,mergedNames,mergedComments);
    }

    private TinyHeader mergeHeaders(TinyHeader headerA, TinyHeader headerB) {
        List<String> namespaces = new ArrayList<>(headerA.getNamespaces());
        namespaces.add(headerB.getNamespaces().get(1));
        // TODO: how should versions and properties be merged?
        return new TinyHeader(namespaces, headerA.getMajorVersion(), headerA.getMinorVersion(), headerA.getProperties());
    }

    private List<String> mergeComments(Collection<String> commentsA, Collection<String> commentsB) {
        return union(commentsA, commentsB);
    }

    private <T extends Mapping> List<String> keyUnion(Collection<T> mappingsA, Collection<T> mappingB) {
        return union(mappingsA.stream().map(m -> m.getMapping().get(0)), mappingB.stream().map(m -> m.getMapping().get(0)));
    }

    private Stream<Pair<String,String>> mapToFirstNamespaceAndDescriptor(TinyClass tinyClass) {
        return tinyClass.getMethods().stream().map(m -> Pair.of(m.getMapping().get(0), m.getMethodDescriptorInFirstNamespace()));
    }


    private List<String> mergeNames(String key, @Nullable Mapping mappingA, @Nullable Mapping mappingB) {
        List<String> merged = new ArrayList<>();
        merged.add(key);
        merged.add(mappingExists(mappingA) ? mappingA.getMapping().get(1) : key);
        merged.add(mappingExists(mappingB) ? mappingB.getMapping().get(1) : key);

        return merged;
    }

    private boolean mappingExists(@Nullable Mapping mapping) {
        return mapping != null && !mapping.getMapping().get(1).isEmpty();
    }

    private <T> List<T> union(Stream<T> list1, Stream<T> list2) {
        return union(list1.collect(Collectors.toList()), list2.collect(Collectors.toList()));
    }

    private <T> List<T> union(Collection<T> list1, Collection<T> list2) {
        Set<T> set = new HashSet<T>();

        set.addAll(list1);
        set.addAll(list2);

        return new ArrayList<T>(set);
    }

    private static String escape(String str) {
        return Pattern.quote(str);
    }

    private <S,E> List<E> map(List<S> from, Function<S, E> mapper) {
        return from.stream().map(mapper).collect(Collectors.toList());
    }

}
