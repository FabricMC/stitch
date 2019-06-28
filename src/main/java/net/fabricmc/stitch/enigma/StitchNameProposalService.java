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

package net.fabricmc.stitch.enigma;

import cuchaz.enigma.analysis.ClassCache;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.api.EnigmaPluginContext;
import cuchaz.enigma.api.service.EnigmaServiceContext;
import cuchaz.enigma.api.service.JarIndexerService;
import cuchaz.enigma.api.service.NameProposalService;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import net.fabricmc.mappings.EntryTriple;
import net.fabricmc.stitch.util.FieldNameFinder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class StitchNameProposalService {
	private Map<EntryTriple, String> fieldNames;

	private StitchNameProposalService(EnigmaPluginContext ctx) {
		ctx.registerService("stitch:jar_indexer", JarIndexerService.TYPE, ctx1 -> new JarIndexerService() {
			@Override
			public void acceptJar(ClassCache classCache, JarIndex jarIndex) {

				Map<String, List<MethodNode>> methods = new HashMap<>();

				classCache.visit(new Supplier<ClassVisitor>() {
					@Override
					public ClassVisitor get() {
						return new ClassVisitor(Opcodes.ASM7) {

							String owner;

							@Override
							public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
								this.owner = name;
								super.visit(version, access, name, signature, superName, interfaces);
							}

							@Override
							public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
								if ("<clinit>".equals(name)) {
									MethodNode node = new MethodNode(api, access, name, descriptor, signature, exceptions);

									methods.computeIfAbsent(owner, s -> new ArrayList<>()).add(node);

									return node;
								} else {
									return super.visitMethod(access, name, descriptor, signature, exceptions);
								}
							}
						};
					}
				}, ClassReader.SKIP_FRAMES);

				try {
					fieldNames = new FieldNameFinder().findNames(methods);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		ctx.registerService("stitch:name_proposal", NameProposalService.TYPE, ctx12 -> (obfEntry, remapper) -> {
			if(obfEntry instanceof FieldEntry){
				FieldEntry fieldEntry = (FieldEntry) obfEntry;
				EntryTriple key = new EntryTriple(fieldEntry.getContainingClass().getFullName(), fieldEntry.getName(), fieldEntry.getDesc().toString());
				return Optional.ofNullable(fieldNames.get(key));
			}
			return Optional.empty();
		});
	}

	public static void register(EnigmaPluginContext ctx) {
		new StitchNameProposalService(ctx);
	}
}
