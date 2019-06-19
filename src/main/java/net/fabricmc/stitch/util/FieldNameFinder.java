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

import net.fabricmc.mappings.EntryTriple;
import net.fabricmc.stitch.commands.CommandProposeFieldNames;
import net.fabricmc.stitch.merge.JarMerger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.io.*;
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

    @Deprecated
    public Map<String, String> find(Iterable<byte[]> classes) throws Exception {
        Map<EntryTriple, String> src = findNames(classes);
        Map<String, String> result = new HashMap<>();
        Set<String> duplKeys = new HashSet<>();

        for (Map.Entry<EntryTriple, String> entry : src.entrySet()) {
            String k = entry.getKey().getOwner() + ";;" + entry.getKey().getName();
            if (!duplKeys.contains(k)) {
                if (result.containsKey(k)) {
                    System.err.println("Warning: Duplicate key (remedy with new API): " + k + " (" + result.get(k) + ")!");
                    duplKeys.add(k);
                    result.remove(k);
                } else {
                    result.put(k, entry.getValue());
                }
            }
        }

        return result;
    }

	public Map<EntryTriple, String> findNames(Iterable<byte[]> classes) throws Exception {
    	Map<String, List<MethodNode>> methods = new HashMap<>();

		for (byte[] data : classes) {
			ClassReader reader = new ClassReader(data);
			String owner = reader.getClassName();
			VClass vClass = new VClass(Opcodes.ASM7);
			reader.accept(vClass, ClassReader.SKIP_FRAMES);
			methods.put(owner, vClass.nodes);
		}

    	return findNames(methods);
	}

    public Map<EntryTriple, String> findNames(Map<String, List<MethodNode>> classes) throws Exception {
        Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
        Map<EntryTriple, String> fieldNames = new HashMap<>();
        Map<String, Set<String>> fieldNamesUsed = new HashMap<>();
        Map<String, Set<String>> fieldNamesDuplicate = new HashMap<>();

        for (Map.Entry<String, List<MethodNode>> entry : classes.entrySet()) {
        	String owner = entry.getKey();
            for (MethodNode mn : entry.getValue()) {
                Frame<SourceValue>[] frames = analyzer.analyze(owner, mn);

                InsnList instrs = mn.instructions;
                for (int i = 1; i < instrs.size(); i++) {
                    AbstractInsnNode instr1 = instrs.get(i - 1);
                    AbstractInsnNode instr2 = instrs.get(i);
                    String s = null;

                    if (instr2.getOpcode() == Opcodes.PUTSTATIC && ((FieldInsnNode) instr2).owner.equals(owner)
                            && instr1 instanceof MethodInsnNode && ((MethodInsnNode) instr1).owner.equals(owner)
                            && (instr1.getOpcode() == Opcodes.INVOKESTATIC || (instr1.getOpcode() == Opcodes.INVOKESPECIAL && "<init>".equals(((MethodInsnNode) instr1).name)))) {

                        for (int j = 0; j < frames[i - 1].getStackSize(); j++) {
                            SourceValue sv = frames[i - 1].getStack(j);
                            for (AbstractInsnNode ci : sv.insns) {
                                if (ci instanceof LdcInsnNode && ((LdcInsnNode) ci).cst instanceof String) {
                                    //if (s == null || !s.equals(((LdcInsnNode) ci).cst)) {
                                    if (s == null) {
                                        s = (String) (((LdcInsnNode) ci).cst);
                                        // stringsFound++;
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
                            int separator = s.indexOf('/');
                            String sFirst = s.substring(0, separator);
                            String sLast;
                            if (s.contains(".") && s.indexOf('.') > separator) {
                                sLast = s.substring(separator + 1, s.indexOf('.'));
                            } else {
                                sLast = s.substring(separator + 1);
                            }
                            if (sFirst.endsWith("s")) {
                                sFirst = sFirst.substring(0, sFirst.length() - 1);
                            }
                            s = sLast + "_" + sFirst;
                        }

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
                            Set<String> usedNamesDuplicate = fieldNamesDuplicate.computeIfAbsent(((FieldInsnNode) instr2).owner, (a) -> new HashSet<>());

                            if (!usedNamesDuplicate.contains(s)) {
                                if (!usedNames.add(s)) {
                                    System.err.println("Warning: Duplicate key: " + s + " (" + oldS + ")!");
                                    usedNamesDuplicate.add(s);
                                    usedNames.remove(s);
                                }
                            }

                            if (usedNames.contains(s)) {
                                fieldNames.put(new EntryTriple(((FieldInsnNode) instr2).owner, ((FieldInsnNode) instr2).name, ((FieldInsnNode) instr2).desc), s);
                            }
                        }
                    }
                }
            }
        }

        return fieldNames;
    }

    public Map<String, String> find(File file) {
        List<byte[]> byteArrays = new ArrayList<>();

        try {
            try (FileInputStream fis = new FileInputStream(file);
                 JarInputStream jis = new JarInputStream(fis)) {
                byte[] buffer = new byte[32768];
                JarEntry entry;

                while ((entry = jis.getNextJarEntry()) != null) {
                    if (!entry.getName().endsWith(".class")) {
                        continue;
                    }

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    int l;
                    while ((l = jis.read(buffer, 0, buffer.length)) > 0) {
                        stream.write(buffer, 0, l);
                    }

                    byteArrays.add(stream.toByteArray());
                }
            }

            return find(byteArrays);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
