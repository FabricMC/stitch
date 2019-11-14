package net.fabricmc.stitch.commands.tinyv2;

import com.google.common.collect.Lists;

import net.fabricmc.stitch.Command;
import net.fabricmc.stitch.util.ParseUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public class CommandUpdateNames extends Command {
	public CommandUpdateNames() {
		super("updateNames");
	}

	private int oldNamedIndex;
	private int oldIntIndex;
	private int newIntIndex;
	private int newNamedIndex;
	private boolean replaceNames;


	//TODO: accept merged mappings. If we keep using unmerged, just apply after merging. If not, then only accepting merged will work.

	/**
	 * <old-mappings>: Merged Official to intermediary to named mappings
	 * <new-mappings>: Newer intermediary to named mappings. Can be of any mc version.
	 * <replace-names>: If true, newer names and comments will always be used in place of old ones.
	 * If false, existing names won't get replaced, but things that don't have names or comments yet will get names from the newer mappings.
	 * Updated mappings will be outputted to <updated-mappings>.
	 */
	@Override
	public String getHelpString() {
		return "<old-mappings> <new-mappings> <updated-mappings> <replace-names>";
	}

	@Override
	public boolean isArgumentCountValid(int count) {
		return count == 4;
	}

	@Override
	public void run(String[] args) throws Exception {
		Path oldMappingsPath = Paths.get(args[0]);
		Path newMappingsPath = Paths.get(args[1]);
		Path updatedMappingsPath = Paths.get(args[2]);
		Boolean replaceNames = ParseUtils.parseBooleanOrNull(args[3]);
		if (replaceNames == null) throw new IllegalArgumentException("<should replace> must be 'true' or 'false'");
		run(oldMappingsPath, newMappingsPath, updatedMappingsPath, replaceNames);
	}

	public void run(Path oldMappingsPath, Path newMappingsPath, Path updatedMappingsPath, boolean replaceNames) throws IOException {
		if (!Files.exists(oldMappingsPath)) throw new IllegalArgumentException("Old mappings not found in " + oldMappingsPath);
		if (!Files.exists(newMappingsPath)) throw new IllegalArgumentException("New mappings not found in " + newMappingsPath);
		if (Files.exists(updatedMappingsPath)) System.out.println("File at " + updatedMappingsPath + " will be replaced.");

		TinyFile oldMappings = TinyV2Reader.read(oldMappingsPath);
		TinyFile newMappings = TinyV2Reader.read(newMappingsPath);

		List<String> oldNamespaces = oldMappings.getHeader().getNamespaces();
		this.oldNamedIndex = oldNamespaces.indexOf("named");
		this.oldIntIndex = oldNamespaces.indexOf("intermediary");
		List<String> newNamespaces = newMappings.getHeader().getNamespaces();
		this.newNamedIndex = newNamespaces.indexOf("named");
		this.newIntIndex = newNamespaces.indexOf("intermediary");
		this.replaceNames = replaceNames;

		updateFile(oldMappings, newMappings);
		TinyV2Writer.write(oldMappings, updatedMappingsPath);
	}

	// Mutates the old mappings
	private void updateFile(TinyFile oldMappings, TinyFile newMappings) {
		// The second namespace has the intermediary names
		update(oldMappings.mapClassesByNamespace(oldIntIndex),
				newMappings.mapClassesByNamespace(newIntIndex), this::updateClass);
	}

	interface UpdateFunc<T> {
		void update(T oldMappings, T newMappings);
	}


	private void updateClass(TinyClass oldClass, TinyClass newClass) {
		update(oldClass.mapFieldsByNamespace(oldIntIndex), newClass.mapFieldsByNamespace(newIntIndex), this::updateField);
		update(oldClass.mapMethodsByNamespace(oldIntIndex), newClass.mapMethodsByNamespace(newIntIndex), this::updateMethod);
	}

	private void updateField(TinyField oldField, TinyField newField) {
		// Fields don't have children, so it's fine to just use the new one.
		// There is a possibility to have a more sophisticated update process than "just use the new one" though.
	}

	private void updateMethod(TinyMethod oldMethod, TinyMethod newMethod) {
		Map<Integer, TinyMethodParameter> oldParams = oldMethod.mapParametersByLvIndex();
		Map<Integer, TinyLocalVariable> oldVariables = oldMethod.mapLocalVariablesByLvIndex();
		Map<Integer, TinyMethodParameter> newParams = newMethod.mapParametersByLvIndex();
		Map<Integer, TinyLocalVariable> newVariables = newMethod.mapLocalVariablesByLvIndex();

		// New mappings might have additional param/variable entries that don't exist in the old ones at all, so we add them here.
		newParams.forEach((k, v) -> {
			if (!oldParams.containsKey(k)) {
				TinyMethodParameter parameter = new TinyMethodParameter(v.getLvIndex(), Lists.newArrayList("", "", ""), new ArrayList<>());
				oldParams.put(k, parameter);
				oldMethod.getParameters().add(parameter);
			}
		});

		newVariables.forEach((k, v) -> {
			if (!oldVariables.containsKey(k)) {
				TinyLocalVariable variable = new TinyLocalVariable(v.getLvIndex(), v.getLvStartOffset(), v.getLvTableIndex(),
						Lists.newArrayList("", "", ""), new ArrayList<>());
				oldVariables.put(k, variable);
				oldMethod.getLocalVariables().add(variable);
			}
		});

		update(oldParams, newParams, this::updateParameter);
		update(oldVariables, newVariables, this::updateLocalVariable);
	}

	private void updateLocalVariable(TinyLocalVariable oldVar, TinyLocalVariable newVar) {
		// variables don't have children
	}

	private void updateParameter(TinyMethodParameter oldParam, TinyMethodParameter newParam) {
		// parameters don't have children
	}


	private <T extends Mapping, K> void update(Map<K, T> oldMappings, Map<K, T> newMappings,
											   UpdateFunc<T> childrenUpdateFunc) {
		oldMappings.forEach((key, oldMapping) -> {
			List<String> oldNames = oldMapping.getMapping();
			Collection<String> oldComments = oldMapping.getComments();

			String oldName = oldNames.get(oldNamedIndex);
			T newMapping = newMappings.get(key);
			if (newMapping == null) return;
			if (replaceNames || oldName.isEmpty() || oldName.equals(key)) {
				oldNames.set(oldNamedIndex, newMapping.getMapping().get(newNamedIndex));
			}
			if (replaceNames || oldComments.isEmpty()) {
				oldComments.clear();
				oldComments.addAll(newMapping.getComments());
			}

			childrenUpdateFunc.update(oldMapping, newMapping);
		});

	}


}
