package de.codesourcery.tinyscript;

import java.util.regex.Pattern;

public class Identifier {

	private static final Pattern VALID_IDENTIFIER = Pattern.compile("[_a-zA-Z]{1}[_0-9a-zA-Z]*");
	
	private final String name;
	
	public Identifier(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		return 31 + ((name == null) ? 0 : name.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Identifier) {
			return name.equals(((Identifier) obj).name);
		}
		return false;
	}
	
	public static boolean isValidIdentifier(String s) {
		return s != null && VALID_IDENTIFIER.matcher(s).matches();
	}
	
	@Override
	public String toString() {
		return "Identifier[ "+name+" ]";
	}
}
