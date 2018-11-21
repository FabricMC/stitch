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

package net.fabricmc.stitch.util;

import net.fabricmc.stitch.commands.CommandProposeFieldNames;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class FieldNameFinder {
    private class VClass extends ClassVisitor {
        private final List<MethodNode> nodes = new ArrayList<>();

        public VClass(int api) {
            super(api);
        }

        @Override
        public MethodVisitor visitMethod(
                final int access,
                final String name,
                final String descriptor,
                final String signature,
                final String[] exceptions) {
            if ("<clinit>".equals(name)) {
                MethodNode node = new MethodNode(api, access, name, descriptor, signature, exceptions);
                nodes.add(node);
                return node;
            } else {
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }
    }

    public Map<String, String> find(File file) {
        Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
        Map<String, String> fieldNames = new HashMap<>();
        Map<String, Set<String>> fieldNamesUsed = new HashMap<>();

        try {
            try (FileInputStream fis = new FileInputStream(file)) {
                JarInputStream jis = new JarInputStream(fis);
                JarEntry entry;

                while ((entry = jis.getNextJarEntry()) != null) {
                    if (!(entry.getName().endsWith(".class"))) {
                        continue;
                    }

                    ClassReader reader = new ClassReader(jis);
                    String owner = reader.getClassName();
                    VClass vClass = new VClass(Opcodes.ASM7);
                    reader.accept(vClass, ClassReader.SKIP_FRAMES);

                    for (MethodNode mn : vClass.nodes) {
                        Frame<SourceValue>[] frames = analyzer.analyze(owner, mn);

                        InsnList instrs = mn.instructions;
                        for (int i = 1; i < instrs.size(); i++) {
                            AbstractInsnNode instr1 = instrs.get(i - 1);
                            AbstractInsnNode instr2 = instrs.get(i);
                            int stringsFound = 0;
                            String s = null;

                            if (instr2.getOpcode() == Opcodes.PUTSTATIC && ((FieldInsnNode) instr2).owner.equals(owner)
                                    && instr1 instanceof MethodInsnNode && ((MethodInsnNode) instr1).owner.equals(owner)
                                    && (instr1.getOpcode() == Opcodes.INVOKESTATIC || (instr1.getOpcode() == Opcodes.INVOKESPECIAL && "<init>".equals(((MethodInsnNode) instr1).name)))) {

                                for (int j = 0; j < frames[i - 1].getStackSize(); j++) {
                                    SourceValue sv = frames[i - 1].getStack(j);
                                    for (AbstractInsnNode ci : sv.insns) {
                                        if (ci instanceof LdcInsnNode && ((LdcInsnNode) ci).cst instanceof String) {
                                            if (s == null || !s.equals(((LdcInsnNode) ci).cst)) {
                                                s = (String) (((LdcInsnNode) ci).cst);
                                                stringsFound++;
                                            }
                                        }
                                    }
                                }
                            }

                            if (s != null) {
                                if (s.contains(":")) {
                                    s = s.substring(s.indexOf(':') + 1);
                                }

                                if (s.contains("/")) {
                                    String sFirst = s.substring(0, s.indexOf('/'));
                                    String sLast;
                                    if (s.contains(".")) {
                                        sLast = s.substring(s.indexOf('/') + 1, s.indexOf('.'));
                                    } else {
                                        sLast = s.substring(s.indexOf('/') + 1);
                                    }
                                    if (sFirst.endsWith("s")) {
                                        sFirst = sFirst.substring(0, sFirst.length() - 1);
                                    }
                                    s = sLast + "_" + sFirst;
                                }

                                if (s != null) {
                                    String oldS = s;
                                    boolean hasAlpha = false;

                                    for (int j = 0; j < s.length(); j++) {
                                        char c = s.charAt(j);

                                        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                                            hasAlpha = true;
                                        }

                                        if (!(c >= 'A' && c <= 'Z') && !(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9') && !(c == '_')) {
                                            s = s.substring(0, j) + "_" + s.substring(j + 1);
                                        } else if (j > 0 && Character.isUpperCase(s.charAt(j)) && Character.isLowerCase(s.charAt(j - 1))) {
                                            s = s.substring(0, j) + "_" + s.substring(j, j + 1).toLowerCase() + s.substring(j + 1);
                                        }
                                    }

                                    if (hasAlpha) {
                                        s = s.toUpperCase();

                                        Set<String> usedNames = fieldNamesUsed.computeIfAbsent(((FieldInsnNode) instr2).owner, (a) -> new HashSet<>());
                                        if (!usedNames.add(s)) {
                                            throw new RuntimeException("Duplicate key: " + s + " (" + oldS + ")!");
                                        }
                                        fieldNames.put(((FieldInsnNode) instr2).owner + ";;" + ((FieldInsnNode) instr2).name, s);
                                    }
                                }
                            }
                        }
                    }
                }

                jis.close();
            }

            return fieldNames;
        } catch (IOException | AnalyzerException e) {
            throw new RuntimeException(e);
        }
    }
}
