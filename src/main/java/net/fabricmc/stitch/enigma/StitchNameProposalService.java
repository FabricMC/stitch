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

package net.fabricmc.stitch.enigma;

import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.api.EnigmaPluginContext;
import cuchaz.enigma.api.service.JarIndexerService;
import cuchaz.enigma.api.service.NameProposalService;
import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import net.fabricmc.mappings.EntryTriple;
import net.fabricmc.stitch.util.FieldNameFinder;
import net.fabricmc.stitch.util.NameFinderVisitor;
import net.fabricmc.stitch.util.StitchUtil;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class StitchNameProposalService {
	private Map<EntryTriple, String> fieldNames;

	private StitchNameProposalService(EnigmaPluginContext ctx) {
		ctx.registerService("stitch:jar_indexer", JarIndexerService.TYPE, ctx1 -> new JarIndexerService() {
			@Override
			public void acceptJar(Set<String> classNames, ClassProvider classProvider, JarIndex jarIndex) {

				Map<String, Set<String>> enumFields = new HashMap<>();
				Map<String, List<MethodNode>> methods = new HashMap<>();

				for (String className : classNames) {
					classProvider.get(className).accept(new NameFinderVisitor(StitchUtil.ASM_VERSION, enumFields, methods));
				}

				try {
					fieldNames = new FieldNameFinder().findNames(enumFields, methods);
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
