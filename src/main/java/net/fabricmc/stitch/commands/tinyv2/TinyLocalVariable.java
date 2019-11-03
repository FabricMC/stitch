package net.fabricmc.stitch.commands.tinyv2;

import java.util.Collection;
import java.util.List;

public class TinyLocalVariable implements Comparable<TinyLocalVariable>, Mapping {

	private final int lvIndex;
	private final int lvStartOffset;
	/**
	 * Will be -1 when there is no lvt index
	 */
	private final int lvTableIndex;
	private final List<String> localVariableNames;
	private final Collection<String> comments;

	public TinyLocalVariable(int lvIndex, int lvStartOffset, int lvTableIndex, List<String> localVariableNames, Collection<String> comments) {
		this.lvIndex = lvIndex;
		this.lvStartOffset = lvStartOffset;
		this.lvTableIndex = lvTableIndex;
		this.localVariableNames = localVariableNames;
		this.comments = comments;
	}


	public int getLvIndex() {
		return lvIndex;
	}

	public int getLvStartOffset() {
		return lvStartOffset;
	}

	public int getLvTableIndex() {
		return lvTableIndex;
	}

	public List<String> getLocalVariableNames() {
		return localVariableNames;
	}

	public Collection<String> getComments() {
		return comments;
	}

	@Override
	public int compareTo(TinyLocalVariable o) {
		return localVariableNames.get(0).compareTo(o.localVariableNames.get(0));
	}

	@Override
	public List<String> getMapping() {
		return localVariableNames;
	}
}
