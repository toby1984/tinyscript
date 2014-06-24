package de.codesourcery.tinyscript.parser;

public class Scanner 
{
	private final String buffer;
	private int offset;
	
	public Scanner(String s) {
		this.buffer = s;
	}
	
	public int offset() {
		return offset;
	}
	
	public void reset(int offset) {
		this.offset = offset;
	}
	
	public boolean eof() {
		return offset >= buffer.length();
	}
	
	public char peek() {
		return buffer.charAt(offset);
	}
	
	public char next() {
		return buffer.charAt(offset++);
	}
}