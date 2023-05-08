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

package net.fabricmc.stitch.representation;

import net.fabricmc.stitch.util.StitchUtil;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Remapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarInputStream;

public class JarReader {
    public static class Builder {
        private final JarReader reader;

        private Builder(JarReader reader) {
            this.reader = reader;
        }

        public static Builder create(JarRootEntry jar) {
            return new Builder(new JarReader(jar));
        }

        public Builder joinMethodEntries(boolean value) {
            reader.joinMethodEntries = value;
            return this;
        }

        public Builder withRemapper(Remapper remapper) {
            reader.remapper = remapper;
            return this;
        }

        public JarReader build() {
            return reader;
        }
    }

    private final JarRootEntry jar;
    private boolean joinMethodEntries = true;
    private Remapper remapper;

    public JarReader(JarRootEntry jar) {
        this.jar = jar;
    }

    private class VisitorClass extends ClassVisitor {
        private JarClassEntry.Populator populator;
        private Set<JarFieldEntry> fields;
        private Set<JarMethodEntry> methods;

        public VisitorClass(int api, ClassVisitor classVisitor) {
            super(api, classVisitor);
        }

        @Override
        public void visit(final int version, final int access, final String name, final String signature,
                          final String superName, final String[] interfaces) {
            this.populator = new JarClassEntry.Populator(access, name, signature, superName, interfaces);
            this.fields = new LinkedHashSet<>();
            this.methods = new LinkedHashSet<>();

            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public void visitOuterClass(String owner, String name, String descriptor) {
            // attribute for the outer class and method,
            // used by anonymous and local classes
            populator.enclosingClass = owner;
            populator.enclosingMethod = name;
            populator.enclosingMethodDesc = descriptor;

            super.visitOuterClass(owner, name, descriptor);
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            // the inner class attributes can be added for any
            // inner class referenced from within this class
            if (populator.name.equals(name)) {
                // While the outer class attribute is only present
                // for anonymous and local classes, the inner class
                // attribute is present for all nested classes, thus
                // we only mark a class as nested if this attribute
                // is present.
                populator.nested = true;
                populator.declaringClass = outerName;
                populator.innerName = innerName;
            }

            super.visitInnerClass(name, outerName, innerName, access);
        }

        @Override
        public FieldVisitor visitField(final int access, final String name, final String descriptor,
                                       final String signature, final Object value) {
            this.fields.add(new JarFieldEntry(access, name, descriptor, signature));

            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(final int access, final String name, final String descriptor,
                                         final String signature, final String[] exceptions) {
            this.methods.add(new JarMethodEntry(access, name, descriptor, signature));

            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        @Override
        public void visitEnd() {
            JarClassEntry entry = jar.getClass(populator.name, populator);
            for (JarFieldEntry field : fields) {
                entry.fields.put(field.getKey(), field);
            }
            for (JarMethodEntry method : methods) {
                entry.methods.put(method.getKey(), method);
            }

            super.visitEnd();
        }
    }

    public void apply() throws IOException {
        // Stage 1: read .JAR class/field/method meta
        try (FileInputStream fileStream = new FileInputStream(jar.file)) {
            try (JarInputStream jarStream = new JarInputStream(fileStream)) {
                java.util.jar.JarEntry entry;

                while ((entry = jarStream.getNextJarEntry()) != null) {
                    if (!entry.getName().endsWith(".class")) {
                        continue;
                    }

                    ClassReader reader = new ClassReader(jarStream);
                    ClassVisitor visitor = new VisitorClass(StitchUtil.ASM_VERSION, null);
                    reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
        }

        System.err.println("Read " + this.jar.getAllClasses().size() + " (" + this.jar.getClasses().size() + ") classes.");

        // Stage 2: find subclasses and inner classes
        this.jar.getAllClasses().forEach((c) -> c.populateRelations(jar));
        System.err.println("Populated subclass and inner class entries.");

        // Stage 3: join identical MethodEntries
        if (joinMethodEntries) {
            System.err.println("Joining MethodEntries...");
            Set<JarClassEntry> traversedClasses = StitchUtil.newIdentityHashSet();

            int joinedMethods = 1;
            int uniqueMethods = 0;

            Collection<JarMethodEntry> checkedMethods = StitchUtil.newIdentityHashSet();

            for (JarClassEntry entry : jar.getAllClasses()) {
                if (traversedClasses.contains(entry)) {
                    continue;
                }

                ClassPropagationTree tree = new ClassPropagationTree(jar, entry);
                if (tree.getClasses().size() == 1) {
                    traversedClasses.add(entry);
                    continue;
                }

                for (JarClassEntry c : tree.getClasses()) {
                    for (JarMethodEntry m : c.getMethods()) {
                        if (!checkedMethods.add(m)) {
                            continue;
                        }

                        // get all matching entries
                        List<JarClassEntry> mList = m.getMatchingEntries(jar, c);

                        if (mList.size() > 1) {
                            for (int i = 0; i < mList.size(); i++) {
                                JarClassEntry key = mList.get(i);
                                JarMethodEntry value = key.getMethod(m.getKey());
                                if (value != m) {
                                    key.methods.put(m.getKey(), m);
                                    joinedMethods++;
                                }
                            }
                        }
                    }
                }

                traversedClasses.addAll(tree.getClasses());
            }

            System.err.println("Joined " + joinedMethods + " MethodEntries (" + uniqueMethods + " unique, " + traversedClasses.size() + " classes).");
        }

        System.err.println("Collecting additional information...");

        // Stage 4: collect additional info
        /* try (FileInputStream fileStream = new FileInputStream(jar.file)) {
            try (JarInputStream jarStream = new JarInputStream(fileStream)) {
                java.util.jar.JarEntry entry;

                while ((entry = jarStream.getNextJarEntry()) != null) {
                    if (!entry.getName().endsWith(".class")) {
                        continue;
                    }

                    ClassReader reader = new ClassReader(jarStream);
                    ClassVisitor visitor = new VisitorClassStageTwo(StitchUtil.ASM_VERSION, null);
                    reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
        } */

        if (remapper != null) {
            System.err.println("Remapping...");

            Map<String, JarClassEntry> classTree = new HashMap<>(jar.classTree);
            jar.classTree.clear();

            for (Map.Entry<String, JarClassEntry> entry : classTree.entrySet()) {
                entry.getValue().remap(remapper);
                jar.classTree.put(entry.getValue().getKey(), entry.getValue());
            }
        }

        System.err.println("- Done. -");
    }
}
