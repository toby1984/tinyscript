package de.codesourcery.tinyscript.parser;

public class ParseException extends RuntimeException {

	public ParseException(String message,int offset) {
		super(message+"( offset: "+offset+")");
	}
}
