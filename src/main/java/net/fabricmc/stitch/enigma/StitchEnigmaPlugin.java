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

import cuchaz.enigma.api.EnigmaPlugin;
import cuchaz.enigma.api.EnigmaPluginContext;
import cuchaz.enigma.api.service.ObfuscationTestService;

public class StitchEnigmaPlugin implements EnigmaPlugin {

	@Override
	public void init(EnigmaPluginContext ctx) {
		StitchNameProposalService.register(ctx);
		ctx.registerService("stitch:intermediary_obfuscation_test", ObfuscationTestService.TYPE, StitchIntermediaryObfuscationTestService::new);
	}

}
