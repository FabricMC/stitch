package net.fabricmc.stitch.util;

import javax.annotation.Nullable;

public class ParseUtils {
	@Nullable
	public static Boolean parseBooleanOrNull(String booleanLiteral) {
		String lowerCase = booleanLiteral.toLowerCase();
		if (lowerCase.equals("true")) return Boolean.TRUE;
		else if (lowerCase.equals("false")) return Boolean.FALSE;
		else return null;
	}
}
