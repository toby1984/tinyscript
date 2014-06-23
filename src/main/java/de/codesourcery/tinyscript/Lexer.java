package de.codesourcery.tinyscript;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

public class Lexer {

	private static final Pattern INTEGER = Pattern.compile("[0-9]+");
	
	private static final boolean DEBUG = false; 
	
	protected final Scanner scanner;
	private final StringBuilder buffer = new StringBuilder();
	private final StringBuilder tmpBuffer = new StringBuilder();	
	
	protected final List<Token> tokens=new ArrayList<>();
	protected boolean skipWhitespace = true;
	
	public Lexer(Scanner scanner) {
		this.scanner = scanner;
	}
	
	@Override
	public String toString() 
	{
		return eof() ? "<EOF>" : tokens.get(0).toString();
	}
	
	public Token peek() 
	{
		maybeParseTokens();
		return logDebug( "peek()" , tokens.get(0) );
	}
	
	public boolean peek(TokenType type) 
	{
		maybeParseTokens();
		return tokens.isEmpty() ? false : tokens.get(0).hasType( type );
	}	
	
	public Token next() 
	{
		maybeParseTokens();
		return logDebug( "next()" , tokens.remove(0) );
	}
	
	private static Token logDebug(String message,Token token) {
		if ( DEBUG ) {
			System.out.println("LEXER: "+message+" - "+token);
		}
		return token;
	}
	
	public Token next(TokenType type) 
	{
		if ( eof()  ) {
			throw new IllegalStateException("Unexpected EOF while looking for token type "+type);
		}
		if ( tokens.get(0).hasType( type ) ) {
			return logDebug("next("+type+")" , tokens.remove(0) );
		}
		throw new IllegalStateException("Found "+tokens.get(0)+" but expected token type "+type);		
	}	
	
	public boolean eof() 
	{
		maybeParseTokens();
		return tokens.isEmpty();
	}
	
	public int offset() {
		return eof() ? scanner.offset() : tokens.get(0).offset;
	}
	
	public boolean isSkipWhitespace() {
		return this.skipWhitespace;
	}
	
	public void setSkipWhitespace(boolean yesNo) 
	{
		if ( this.skipWhitespace != yesNo ) 
		{
			if ( ! tokens.isEmpty() ) {
				logDebug( "skip_whitespace = "+yesNo+" , resetting to ", tokens.get(0) );
				scanner.reset( tokens.get(0).offset );
				tokens.clear();
			}
			this.skipWhitespace = yesNo;
		}
	}
	
	private boolean isWhitespace(char c) {
		return c == ' ' || c == '\t';
	}
	
	private boolean isEOL(char c) {
		return c == '\r' || c == '\n'; 
	}
	
	private void maybeParseTokens() 
	{
		if ( ! tokens.isEmpty() || scanner.eof() ) {
			return;
		}
		
		buffer.setLength(0);
		
		while ( ! scanner.eof() ) 
		{
			final char c = scanner.peek();
			if ( ! ( isEOL( c ) || ( skipWhitespace && isWhitespace( c ) ) ) ) 
			{			
				break;
			}
			scanner.next();
		}
		
		final int parseStartOffset = scanner.offset();
loop:		
		while( ! scanner.eof() ) 
		{
			char c = scanner.peek();
			if ( isEOL( c ) ) {
				break;
			}
			if ( skipWhitespace && isWhitespace( c ) ) 
			{
				break;
			}
			
			scanner.next();
			
			if ( OperatorType.mayBeOperator( Character.toString(c) ) ) 
			{
				final int opOffset = parseStartOffset;
				tmpBuffer.setLength(0);
				tmpBuffer.append( c );
				while ( ! scanner.eof() && OperatorType.mayBeOperator( tmpBuffer.toString() + scanner.peek() )  ) 
				{
					tmpBuffer.append( scanner.next() );
				}
				final OperatorType match = OperatorType.getExactMatch( tmpBuffer.toString() );
				if ( match != null ) 
				{
					parseBuffer(parseStartOffset);
					pushToken(TokenType.OPERATOR ,  tmpBuffer.toString(), opOffset);
					return;
				}
				buffer.append( tmpBuffer.toString() );
				continue;
			}
			
			final Token newToken;
			switch(c) 
			{
			    case '\\': newToken = newToken(TokenType.ESCAPE_CHARACTER,c,scanner.offset()-1); break;					
				case '.':  newToken = newToken(TokenType.DOT,c,scanner.offset()-1); break;						
				case '(':  newToken = newToken(TokenType.PARENS_OPEN,c,scanner.offset()-1); break;					
				case ')':  newToken = newToken(TokenType.PARENS_CLOSE,c,scanner.offset()-1); break;	
				case ',':  newToken = newToken(TokenType.COMMA,c,scanner.offset()-1); break;
				case '\'': newToken = newToken(TokenType.STRING_DELIMITER,c,scanner.offset()-1); break;				
				case '"':  newToken = newToken(TokenType.STRING_DELIMITER,c,scanner.offset()-1); break;
				default:
					buffer.append( c );
					continue loop;
			}
			parseBuffer(parseStartOffset);
			pushToken(newToken);
			return;
		}
		parseBuffer(parseStartOffset);
	}
	
	private void pushToken(TokenType t,String value,int offset) {
		pushToken( newToken(t,value,offset ) );
	}
	
	protected void pushToken(TokenType t,char value,int offset) {
		pushToken( newToken(t,Character.toString(value),offset ) );
	}	
	
	private Token newToken(TokenType t,String value,int offset) {
		return new Token(t,value,offset );
	}
	
	private Token newToken(TokenType t,char value,int offset) {
		return new Token(t,Character.toString(value),offset );
	}	
	
	private void pushToken(Token t) {
		logDebug( "Parsed " , t);
		tokens.add( t );
	}
	
	private void parseBuffer(int offset) {
		
		final String s = buffer.toString();
		buffer.setLength(0);
		
		if ( s.length() == 0 ) {
			return;
		}
		
		if ( "true".equalsIgnoreCase( s ) ) {
			pushToken(TokenType.TRUE,"true",offset);
		} 
		else if ( "false".equalsIgnoreCase( s ) ) {
			pushToken(TokenType.FALSE,"false",offset);			
		} 
		else if ( INTEGER.matcher( s ).matches() ) {
			pushToken(TokenType.NUMBER,s,offset);
		} 
		else if ( Identifier.isValidIdentifier( s ) ) {
			pushToken(TokenType.IDENTIFIER,s,offset);			
		} 
		else {
			pushToken(TokenType.TEXT,s,offset);
		}
	}	
}