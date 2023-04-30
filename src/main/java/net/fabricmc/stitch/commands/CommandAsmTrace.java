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

package net.fabricmc.stitch.commands;

import java.io.FileInputStream;
import java.io.PrintWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.TraceClassVisitor;

import net.fabricmc.stitch.Command;

public class CommandAsmTrace extends Command {
	public CommandAsmTrace() {
		super("asmTrace");
	}

	@Override
	public String getHelpString() {
		return "<class-file>";
	}

	@Override
	public boolean isArgumentCountValid(int count) {
		return count == 1;
	}

	@Override
	public void run(String[] args) throws Exception {
		ClassReader cr = new ClassReader(new FileInputStream(args[0]));
		ClassVisitor tcv = new TraceClassVisitor(new PrintWriter(System.out));
		cr.accept(tcv, 0);
	}
}
