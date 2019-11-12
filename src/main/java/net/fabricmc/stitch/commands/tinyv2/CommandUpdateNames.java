package net.fabricmc.stitch.commands.tinyv2;

import net.fabricmc.stitch.Command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class CommandUpdateNames extends Command {
	public CommandUpdateNames() {
		super("updateNames");
	}

	/**
	 * <intermediaries>: Official to intermediary mappings of the old mapping's version.
	 * <old-mappings>: Intermediary to named mappings
	 * <new-mappings>: Newer intermediary to named mappings. Can be of any mc version.
	 */
	@Override
	public String getHelpString() {
		return "<intermediaries> <old-mappings> <new-mappings> <updated-mappings>";
	}

	@Override
	public boolean isArgumentCountValid(int count) {
		return count == 3;
	}

	@Override
	public void run(String[] args) throws Exception {
		Path intPath = Paths.get(args[0]);
		Path oldMappingsPath = Paths.get(args[1]);
		Path newMappingsPath = Paths.get(args[2]);
		Path updatedMappingsPath = Paths.get(args[3]);
		if (!Files.exists(intPath)) throw new IllegalArgumentException("Intermediaries not found in " + intPath);
		if (!Files.exists(oldMappingsPath)) throw new IllegalArgumentException("Old mappings not found in " + oldMappingsPath);
		if (!Files.exists(newMappingsPath)) throw new IllegalArgumentException("New mappings not found in " + newMappingsPath);
		if (Files.exists(updatedMappingsPath)) System.out.println("File at " + updatedMappingsPath + " will be replaced.");

		TinyFile intermediaries = TinyV2Reader.read(intPath);
		TinyFile oldMappings = TinyV2Reader.read(oldMappingsPath);
		TinyFile newMappings = TinyV2Reader.read(newMappingsPath);

		TinyFile updated = update(intermediaries, oldMappings, newMappings);
		TinyV2Writer.write(updated, updatedMappingsPath);
	}

	private TinyFile update(TinyFile intermediariesFile, TinyFile oldMappings, TinyFile newMappings) {
		// The second namespace has the intermediary names
		List<TinyClass> updatedClasses = merge(intermediariesFile.mapClassesByNamespace(1),
				oldMappings.mapClassesByFirstNamespace(), newMappings.mapClassesByFirstNamespace(), this::updateClass);

		return new TinyFile(oldMappings.getHeader(), updatedClasses);
	}

	interface UpdateFunc<T> {
		T update(T intermediaries, T oldMappings, T newMappings);
	}


	private TinyClass updateClass(TinyClass intermediariesClass, TinyClass oldClass, TinyClass newClass) {


		List<TinyField> updatedFields = merge(intermediariesClass.mapFieldsByNamespace(1),
				oldClass.mapFieldsByFirstNamespace(), newClass.mapFieldsByFirstNamespace(), this::updateField);
		List<TinyMethod> updatedMethods = merge(intermediariesClass.mapMethodsByNamespaceAndDescriptor(1),
				oldClass.mapMethodsByFirstNamespaceAndDescriptor(), newClass.mapMethodsByFirstNamespaceAndDescriptor(), this::updateMethod);

		// Note how we specifically use the new mapping name, this is what the whole update process is about.
		return new TinyClass(newClass.getClassNames(), updatedMethods, updatedFields, newClass.getComments());
	}

	private TinyField updateField(TinyField intField, TinyField oldField, TinyField newField) {
		// Fields don't have children, so it's fine to just use the new one.
		// There is a possibility to have a more sophisticated update process than "just use the new one" though.
		return newField;
	}

	private TinyMethod updateMethod(TinyMethod intMethod, TinyMethod oldMethod, TinyMethod newMethod) {
		List<TinyMethodParameter> updatedParams = merge(intMethod.mapParametersByNamespace(1),
				oldMethod.mapParametersByNamespace(0), newMethod.mapParametersByNamespace(0), this::updateParameter);

		List<TinyLocalVariable> updatedVariables = merge(intMethod.mapLocalVariablesByNamespace(1),
				oldMethod.mapLocalVariablesByNamespace(0), newMethod.mapLocalVariablesByNamespace(0), this::updateLocalVariable);

		assert oldMethod.getMethodDescriptorInFirstNamespace().equals(newMethod.getMethodDescriptorInFirstNamespace());
		return new TinyMethod(oldMethod.getMethodDescriptorInFirstNamespace(),
				newMethod.getMethodNames(), updatedParams, updatedVariables, newMethod.getComments());
	}

	private TinyLocalVariable updateLocalVariable(TinyLocalVariable intVar, TinyLocalVariable oldVar, TinyLocalVariable newVar) {
		// variables don't have children
		return newVar;
	}

	private TinyMethodParameter updateParameter(TinyMethodParameter intParam, TinyMethodParameter oldParam, TinyMethodParameter newParam) {
		// parameters don't have children
		return newParam;
	}

	private <T extends Mapping, K> List<T> merge(Map<K, T> intermediaries, Map<K, T> oldMappings, Map<K, T> newMappings,
												 UpdateFunc<T> updateFunc) {
		List<T> merged = new LinkedList<>();
		intermediaries.forEach((intermediary, intermediaryEntry) -> {
			T oldMapping = oldMappings.get(intermediary);
			T newMapping = newMappings.get(intermediary);
			if (oldMapping == null && newMapping == null) return;
			if (oldMapping == null) merged.add(newMapping);
			else if (newMapping == null) merged.add(oldMapping);
			else merged.add(updateFunc.update(intermediaryEntry, oldMapping, newMapping));
		});
		return merged;
	}


}
