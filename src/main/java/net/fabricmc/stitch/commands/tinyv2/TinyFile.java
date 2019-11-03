package net.fabricmc.stitch.commands.tinyv2;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * All lists and collections in this AST are mutable.
 */
public class TinyFile {
	private final TinyHeader header;
	private final Collection<TinyClass> classEntries;

	public TinyFile(TinyHeader header, Collection<TinyClass> classEntries) {
		this.header = header;
		this.classEntries = classEntries;
	}

	/**
	 * The key will be the name of the class in the first namespace, the value is the same as classEntries.
	 * Useful for quickly retrieving a class based on a known name in the first namespace.
	 */
	public Map<String, TinyClass> mapClassesByFirstNamespace() {
		return classEntries.stream().collect(Collectors.toMap(c -> c.getClassNames().get(0), c -> c));
	}

	public TinyHeader getHeader() {
		return header;
	}

	public Collection<TinyClass> getClassEntries() {
		return classEntries;
	}
}
