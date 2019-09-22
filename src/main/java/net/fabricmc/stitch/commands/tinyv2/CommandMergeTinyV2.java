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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
        merge(tinyFileA, tinyFileB);

        TinyV2Writer.write(tinyFileA, Paths.get(args[2]));
    }

    // Mutates inputA to be the merged result
    private void merge(TinyFile inputA, TinyFile inputB) {
        //TODO: how to merge properties?

        System.out.println("Merging " + inputA + " with " + inputB);
        mergeNames(inputA.getHeader().getNamespaces(), inputB.getHeader().getNamespaces());

        Map<String, TinyClass> inputBClassesByFirstNamespaceName = inputB.mapClassesByFirstNamespace();
        for (TinyClass tinyClass : inputA.getClassEntries()) {
            String sharedName = tinyClass.getClassNames().get(0);
            TinyClass matchingClass = inputBClassesByFirstNamespaceName.get(sharedName);
            //TODO: remove
            if (sharedName.equals("net/minecraft/class_481$class_482")) {
                int x = 2;
            }
            if (matchingClass == null || matchingClass.getClassNames().get(1).equals("")) {
                String partlyMatchedClassName = matchEnclosingClass(sharedName, inputBClassesByFirstNamespaceName);
                if (matchingClass == null) {
                    matchingClass = new TinyClass(Arrays.asList(sharedName, partlyMatchedClassName));
                } else matchingClass.getClassNames().set(1, partlyMatchedClassName);
            }


            mergeClasses(tinyClass, matchingClass);
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
            String currentPath = String.join("$", Arrays.copyOfRange(path, i, parts - 1));
            TinyClass match = inputBClassBySharedNamespace.get(currentPath);

            if (match != null) {
                String matchingInnerClassName = match.getClassNames().get(1)
                        + "$" + String.join("$", Arrays.copyOfRange(path, i + 1, path.length));
                return matchingInnerClassName;

            }
        }

        return sharedName;
    }

    private static String escape(String str) {
        return Pattern.quote(str);
    }

    private void mergeClasses(TinyClass classA, @Nonnull TinyClass classB) {

        //TODO: it should be acceptable to leave the space empty if it's null but right now we just insert the first namespace.
        mergeNames(classA.getClassNames(), classB.getClassNames());
        mergeComments(classA.getComments(), classB.getComments());


        Map<String, TinyMethod> classBMethodsByFirstNamespaceName = classB.mapMethodsByFirstNamespaceAndDescriptor();

        for (TinyMethod method : classA.getMethods()) {
            mergeMethods(method, classBMethodsByFirstNamespaceName.get(
                    method.getMethodNames().get(0) + method.getMethodDescriptorInFirstNamespace()
            ));
        }

        Map<String, TinyField> classBFieldsByFirstNamespaceName = classB.mapFieldsByFirstNamespace();
        for (TinyField field : classA.getFields()) {
            mergeFields(field, classBFieldsByFirstNamespaceName.get(field.getFieldNames().get(0)));
        }

    }

    private void mergeMethods(TinyMethod methodA, @Nullable TinyMethod methodB) {
        if (methodB == null) {
            methodA.getMethodNames().add(methodA.getMethodNames().get(0));
            return;
        }
        mergeNames(methodA.getMethodNames(), methodB.getMethodNames());
        mergeComments(methodA.getComments(), methodB.getComments());
        // Descriptors remain as-is


        //TODO: this won't work too well when the first namespace is named or there is more than one named namespace (hack)

        int namedIndexParams = methodA.getParameters().isEmpty() ? 0 : 2;
        methodA.getParameters().addAll(methodB.getParameters());
        for (TinyMethodParameter parameter : methodA.getParameters()) {
            parameter.getParameterNames().add(namedIndexParams, "");
        }

        int namedIndexLv = methodA.getLocalVariables().isEmpty() ? 0 : 2;
        methodA.getLocalVariables().addAll(methodB.getLocalVariables());
        for (TinyLocalVariable localVariable : methodA.getLocalVariables()) {
            localVariable.getLocalVariableNames().add(namedIndexLv, "");
        }

    }

    private void mergeFields(TinyField fieldA, @Nullable TinyField fieldB) {
        if (fieldB == null) {
            fieldA.getFieldNames().add(fieldA.getFieldNames().get(0));
            return;
        }
        mergeNames(fieldA.getFieldNames(), fieldB.getFieldNames());
        mergeComments(fieldA.getComments(), fieldB.getComments());
        // Descriptors remain as-is
    }


    private void mergeNames(List<String> namesA, List<String> namesB) {
        String toAdd = namesB.size() >= 2 ? namesB.get(1) : namesB.get(0);
        namesA.add(toAdd);
    }

    private void mergeComments(Collection<String> commentsA, Collection<String> commentsB) {
        commentsA.addAll(commentsB);
    }


}
