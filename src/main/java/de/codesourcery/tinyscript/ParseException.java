package de.codesourcery.tinyscript;

public class ParseException extends RuntimeException {

	public ParseException(String message,int offset) {
		super(message+"( offset: "+offset+")");
	}
}
