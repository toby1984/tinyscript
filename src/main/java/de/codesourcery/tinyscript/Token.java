package de.codesourcery.tinyscript;

public final class Token {

	public final TokenType type;
	public final String text;
	public final int offset;
	
	public Token(TokenType type,String text,int offset) 
	{
		if ( type == null ) throw new IllegalArgumentException("type must not be NULL");
		if ( text == null ) throw new IllegalArgumentException("text must not be NULL");
		if ( offset < 0 ) throw new IllegalArgumentException("offset must be >= 0");		
		this.type = type;
		this.text = text;
		this.offset=offset;
	}
	
	public boolean hasType(TokenType t) {
		return this.type == t;
	}

	@Override
	public String toString() {
		return text+ " [ " + type + " (" + offset+") ]";
	}
}