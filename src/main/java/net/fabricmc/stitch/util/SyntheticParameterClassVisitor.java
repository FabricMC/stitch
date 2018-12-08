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

import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

/**
 * ProGuard has a bug where parameter annotations are applied incorrectly in the presence of
 * synthetic arguments. This causes javac to balk when trying to load affected classes.
 *
 * We compute the offset between a method's descriptor and signature to determine the number
 * of synthetic arguments, then subtract that from each parameter annotation.
 */
public class SyntheticParameterClassVisitor extends ClassVisitor {
    static class SyntheticMethodVisitor extends MethodVisitor {
        private final String descriptor;
        private final String signature;
        private int offset = -1;

        SyntheticMethodVisitor(int api, String descriptor, String signature, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
            this.descriptor = descriptor;
            this.signature = signature;
        }

        private int getOffset() {
            if (offset >= 0) return offset;

            ArgumentSignatureCounter signatureCounter = new ArgumentSignatureCounter();
            new SignatureReader(signature).accept(signatureCounter);
            int parameters = Type.getArgumentTypes(descriptor).length;

            return this.offset = parameters - signatureCounter.count;
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
            return super.visitParameterAnnotation(parameter - getOffset(), descriptor, visible);
        }

        @Override
        public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
            super.visitAnnotableParameterCount(parameterCount - getOffset(), visible);
        }
    }

    private boolean skip;

    public SyntheticParameterClassVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        skip = (access & Opcodes.ACC_ENUM) == 0;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String descriptor,
        final String signature,
        final String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return signature == null || mv == null || skip
            ? mv : new SyntheticMethodVisitor(api, descriptor, signature, mv);
    }

    private static final class ArgumentSignatureCounter extends SignatureVisitor {
        int count;

        ArgumentSignatureCounter() {
            super(Opcodes.ASM7);
        }

        @Override
        public SignatureVisitor visitParameterType() {
            count++;
            return super.visitParameterType();
        }
    }
}
