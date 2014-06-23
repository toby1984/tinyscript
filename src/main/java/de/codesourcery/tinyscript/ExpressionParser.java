package de.codesourcery.tinyscript;

import java.text.ParseException;

public class ExpressionParser<T> {

	public static final boolean DEBUG = false;
	
	private Lexer lexer;
	private IParseContext<T> context;

	private int lastErrorOffset = -1;
	private String lastErrorMsg;	

	public ExpressionParser() {
	}
	
	public void parse(Lexer lexer,IParseContext<T> context) throws ParseException 
	{
		this.lexer = lexer;
		this.context = context;

		lastErrorOffset = -1;
		lastErrorMsg = null;	

		if ( ! evaluate() ) {
			throw new ParseException("Syntax error: "+lastErrorMsg+" at offset "+lastErrorOffset,lastErrorOffset);
		}
	}
	
	private boolean success(String s) {
		if ( DEBUG ) {
			System.out.println("* ");
			System.out.println("* SUCCESS: "+s);
			System.out.println("* ");
		}
		return true;
	}

	private boolean evaluate() 
	{
		boolean result = true;
		while ( ! lexer.eof() && result) {
			result = expression(); 
		}
		return result;
	}

	// OR
	private boolean expression() 
	{
		boolean success = false;
		if ( parseAtom() ) 
		{
			success = true;
			while ( lexer.peek(TokenType.OPERATOR )  ) 
			{
				final OperatorType operator = OperatorType.getExactMatch( lexer.next().text );
				context.pushOperator( operator );
				if ( ! parseAtom() ) {
					break;
				}
			}
		} 
		else if ( lexer.peek(TokenType.OPERATOR ) ) 
		{
			success = true;			
			
			do {
				final OperatorType operator = OperatorType.getExactMatch( lexer.next().text );
				context.pushOperator( operator );
				if ( ! parseAtom() ) {
					break;
				}
			} while ( lexer.peek(TokenType.OPERATOR ) );
		}
		return success;
	}
	
	private boolean parseAtom() 
	{
		if ( parseFunctionInvocation() ) {
			return true;
		}
		
		if ( parseIdentifier() ) {
			return true;
		}

		if ( parseBoolean() || parseNumber() || parseString() ) {
			return true;
		}

		if ( consume( TokenType.PARENS_OPEN ) ) 
		{
			this.context.pushOpeningParens();		
			if ( expression() && consume(TokenType.PARENS_CLOSE) ) 
			{
				this.context.pushClosingParens();
				return success(" '(' expr ')' ");
			}
			return false;
		}		
		return errorLater("Expected either a number, a string , a boolean value , function invocation or opening parens");		
	}		

	private boolean parseIdentifier() 
	{
		if ( peek(TokenType.IDENTIFIER ) ) {
			Identifier id = new Identifier( lexer.next().text );
			context.pushValue( id );
			return success("Identifier: "+id);
		}
		return false;
	}

	private boolean parseString() 
	{
		if ( peek(TokenType.STRING_DELIMITER ) ) 
		{
			final String expectedDelimiter = lexer.next(TokenType.STRING_DELIMITER).text;
			final boolean oldWhitespace = lexer.isSkipWhitespace();
			if ( oldWhitespace ) {
				lexer.setSkipWhitespace( false );
			}
			final StringBuffer buffer = new StringBuffer();			
			try 
			{
				boolean quoted = false;
				while ( ! lexer.eof() ) 
				{
					Token tok = lexer.peek();
					if ( ! quoted && consume( TokenType.ESCAPE_CHARACTER ) ) 
					{
						quoted = true;
						continue;
					}
					if ( quoted || ! (tok.hasType(TokenType.STRING_DELIMITER ) && tok.text.equals( expectedDelimiter ) ) ) {
						buffer.append( lexer.next().text );
						quoted = false;							
					} else {
						break;
					}
				}
				if ( lexer.eof() ) {
					return error("Unterminated string");
				}
				consume(TokenType.STRING_DELIMITER);
				context.pushValue( buffer.toString() );
			} finally {
				lexer.setSkipWhitespace(oldWhitespace);
			}
			return success("String: "+buffer);			
		}
		return false;
	}

	private boolean parseBoolean() 
	{
		if ( consume(TokenType.TRUE) ) {
			context.pushValue( Boolean.TRUE );
			return success("Boolean: TRUE");
		}
		if ( consume(TokenType.FALSE) ) {
			context.pushValue( Boolean.FALSE );
			return success("Boolean: FALSE");			
		}		
		return false;
	}

	private boolean parseFunctionInvocation() 
	{
		if ( peek(TokenType.IDENTIFIER ) ) 
		{
			final String identifier = lexer.next(TokenType.IDENTIFIER).text;
			if ( consume( TokenType.PARENS_OPEN ) ) 
			{
				context.pushFunctionInvocation(identifier);					
				context.pushOpeningParens();
				if ( parseArgumentList() && consume( TokenType.PARENS_CLOSE ) ) 
				{
					context.pushClosingParens();
					return success("Function invocation: "+identifier+"(...)");
				}
				return false;
			}
			context.pushValue( new Identifier(identifier) );
			return true;
		}
		return false;
	}

	private boolean parseArgumentList() 
	{
		boolean result = false;
		if ( expression() ) 
		{
			result = true;
			while (consume(TokenType.COMMA)) 
			{
				context.pushArgumentDelimiter();
				if ( ! expression() ) 
				{
					result = false;
					break;
				}
			}
		}
		if ( result ) {
			return success("Argument list");
		} 
		return false;
	}

	private boolean peek(TokenType type) {
		if ( !lexer.eof() && lexer.peek().hasType(type) ) {
			return true;
		}
		return error("Expected token type "+type);
	}

	private boolean consume(TokenType type) 
	{
		if ( ! lexer.eof() && lexer.peek().hasType(type) ) {
			lexer.next(type);
			return success("TokenType "+type);
		}
		return error("Expected token type "+type);
	}	

	private boolean parseNumber() 
	{
		if ( peek(TokenType.NUMBER ) ) 
		{
			String num = lexer.next().text;
			if ( peek(TokenType.DOT ) ) 
			{
				consume(TokenType.DOT);
				if ( ! peek(TokenType.NUMBER ) ) {
					return error("Invalid floating point number");
				}
				num += "."+lexer.next().text;
				context.pushValue( Double.parseDouble( num ) );
			} else {
				context.pushValue( Integer.parseInt( num ) );
			}
			return success("Number "+num);
		}
		return error("Expected a number");
	}	

	private boolean error(String message) 
	{
		if ( lexer.offset() >= lastErrorOffset ) 
		{
			if ( DEBUG ) {
				System.out.println("ERROR: "+message);
			}					
			lastErrorOffset = lexer.offset();
			lastErrorMsg = message;
		}
		return false;
	}
	
	private boolean errorLater(String message) 
	{
		if ( lexer.offset() > lastErrorOffset ) 
		{
			if ( DEBUG ) {
				System.out.println("ERROR: "+message);
			}				
			lastErrorOffset = lexer.offset();
			lastErrorMsg = message;
		}
		return false;
	}	
}