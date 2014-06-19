package de.codesourcery.reflectiondsl;

import java.text.ParseException;
import java.util.List;

public class ExpressionParser {

	private Lexer lexer;
	private IParseContext context;

	private int lastErrorOffset = -1;
	private String lastErrorMsg;	

	public ExpressionParser() {
	}
	
	public static void main(String[] args) throws ParseException {
		
		final String expression = "a"; 
		Lexer lexer = new Lexer(new Scanner(expression) );
		
		final ExpressionParser parser = new ExpressionParser();
		IParseContext ctx = new ParseContext( parser );
		parser.parse( lexer ,  ctx );
		System.out.println("Result: "+ctx.getResult());
	}

	public void parse(Lexer lexer,IParseContext context) throws ParseException 
	{
		this.lexer = lexer;
		this.context = context;

		lastErrorOffset = -1;
		lastErrorMsg = null;	

		if ( ! evaluate() ) {
			throw new ParseException("Syntax error: "+lastErrorMsg+" at offset "+lastErrorOffset,lastErrorOffset);
		}
	}

	private boolean evaluate() 
	{
		boolean result = true;
		while ( ! lexer.eof() && result) {
			result = expression(); 
		}
		return result;
	}

	private boolean expression() 
	{
		return parsePrecedence1();		
	}

	// OR
	private boolean parsePrecedence1() 
	{
		if ( parsePrecedence11() ) 
		{
			return tryParse( () -> 
			{
				final OperatorType op;
				if ( peekOperator( OperatorType.OR ) ) {
					op=OperatorType.OR;
				} else {
					return true;
				}
				consume(TokenType.OPERATOR);
				if ( ! parsePrecedence11() ) {
					return false;
				}
				context.applyOperator( op );
				return true;
			});
		}
		return false;
	}

	// AND
	private boolean parsePrecedence11() 
	{
		if ( parsePrecedence12() ) 
		{
			return tryParse( () -> 
			{
				final OperatorType op;
				if ( peekOperator( OperatorType.AND ) ) {
					op=OperatorType.AND;
				} else {
					return true;
				}
				consume(TokenType.OPERATOR);
				if ( ! parsePrecedence12() ) {
					return false;
				}
				context.applyOperator( op );
				return true;
			});
		}
		return false;
	}	

	// EQ,NEQ
	private boolean parsePrecedence12() 
	{
		if ( parsePrecedence13() ) 
		{
			return tryParse( () -> 
			{
				final OperatorType op;
				if ( peekOperator( OperatorType.EQ ) ) {
					op=OperatorType.EQ;
				} else if ( peekOperator( OperatorType.NEQ ) ) {
					op=OperatorType.NEQ;					
				} else {
					return true;
				}
				consume(TokenType.OPERATOR);
				if ( ! parsePrecedence13() ) {
					return false;
				}
				context.applyOperator( op );
				return true;
			});
		}
		return false;
	}	

	//  < <= > >=
	private boolean parsePrecedence13() 
	{
		if ( parsePrecedence2() ) 
		{
			return tryParse( () -> 
			{
				final OperatorType op;
				if ( peekOperator( OperatorType.LT ) ) {
					op=OperatorType.LT;
				} else if ( peekOperator( OperatorType.LTE ) ) {
					op=OperatorType.LTE;		
				} else if ( peekOperator( OperatorType.GT ) ) {
					op=OperatorType.GT;
				} else if ( peekOperator( OperatorType.GTE ) ) {
					op=OperatorType.GTE;					
				} else {
					return true;
				}
				consume(TokenType.OPERATOR);
				if ( ! parsePrecedence2() ) {
					return false;
				}
				context.applyOperator( op );
				return true;
			});
		}
		return false;
	}

	//   PLUS , MINUS 
	private boolean parsePrecedence2() 
	{
		if ( parsePrecedence3() ) 
		{
			return tryParse( () -> 
			{
				final OperatorType op;
				if ( peekOperator( OperatorType.PLUS ) ) {
					op=OperatorType.PLUS;
				} else if ( peekOperator( OperatorType.MINUS ) ) {
					op=OperatorType.MINUS;
				} else {
					return true;
				}
				consume(TokenType.OPERATOR);
				if ( ! parsePrecedence3() ) {
					return false;
				}
				context.applyOperator( op );
				return true;
			});
		}
		return false;
	}	

	// TIMES , DIVIDE
	private boolean parsePrecedence3() 
	{
		if ( parsePrecedence4() ) 
		{
			return tryParse( () -> 
			{
				final OperatorType op;
				if ( peekOperator( OperatorType.TIMES ) ) {
					op=OperatorType.TIMES;
				} else if ( peekOperator( OperatorType.DIVIDE) ) {
					op=OperatorType.DIVIDE;
				} else {
					return true;
				}
				consume(TokenType.OPERATOR);
				if ( ! parsePrecedence4() ) {
					return false;
				}
				context.applyOperator( op );
				return true;
			});
		}
		return false;
	}	

	// NOT
	private boolean parsePrecedence4() 
	{
		if ( parseAtom() ) 
		{
			return true;
		}
		return tryParse( () -> 
		{
			final OperatorType op; 
			if ( peekOperator( OperatorType.NOT ) ) {
				op= OperatorType.NOT;
			} else {
				return false;
			}
			consume(TokenType.OPERATOR);
			if ( ! parsePrecedence4() ) {
				return false;
			}
			context.applyOperator(op);
			return true;
		});
	}	

	private boolean parseAtom() 
	{
		if ( parseFunctionInvocation() ) {
			return true;
		}

		if ( parseBoolean() || parseNumber() || parseString() ) {
			return true;
		}

		if ( peek( TokenType.PARENS_OPEN ) ) 
		{
			return tryParse( () -> 
			{
				consume(TokenType.PARENS_OPEN);
				return parsePrecedence1() && consume(TokenType.PARENS_CLOSE);
			});
		}		
		return errorLater("Expected either a number, a string , a boolean value , function invocation or opening parens");		
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
			try 
			{
				final StringBuffer buffer = new StringBuffer();
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
			return true;
		}
		return false;
	}

	private boolean parseBoolean() 
	{
		if ( consume(TokenType.TRUE) ) {
			context.pushValue( Boolean.TRUE );
			return true;
		}
		if ( consume(TokenType.FALSE) ) {
			context.pushValue( Boolean.FALSE );
			return true;
		}		
		return false;
	}

	private boolean peekOperator(OperatorType op) {

		return ! lexer.eof() && lexer.peek().hasType(TokenType.OPERATOR ) && op.matchesSymbol( lexer.peek().text );
	}

	private boolean parseFunctionInvocation() 
	{
		if ( peek(TokenType.IDENTIFIER ) ) 
		{
			return tryParse( () -> 
			{ 			
				final String functionName = lexer.next(TokenType.IDENTIFIER).text;
				if ( consume( TokenType.PARENS_OPEN ) && parseArgumentList() && consume( TokenType.PARENS_CLOSE ) ) 
				{
					final List<Object> args = (List<Object>) context.pop();
					context.pushFunctionInvocation(functionName,args);
					return true;
				}
				return false;
			});
		}
		return false;
	}

	private boolean parseArgumentList() 
	{
		boolean result = false;
		int argCount = 0;
		if ( expression() ) 
		{
			result = true;
			argCount++;
			while (consume(TokenType.COMMA)) 
			{
				if ( ! expression() ) 
				{
					result = false;
					break;
				}
				argCount++;
			}
		}
		if ( result ) {
			context.pushValue( context.popN( argCount ) );
		} else {
			context.popN( argCount );
		}
		return result;
	}

	@FunctionalInterface
	private interface Block {
		public boolean apply();
	}

	private boolean tryParse(Block block) 
	{
		context.saveState();
		lexer.saveState();
		boolean success = false;
		try {
			success = block.apply();
		} 
		finally 
		{
			if ( ! success ) 
			{
				context.recallState();
				lexer.recallState();
			} else {
				context.dropState();
				lexer.dropState();
			}
		}
		return success;
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
			return true;
		}
		return error("Expected token type "+type);
	}	

	private boolean parseNumber() 
	{
		return tryParse( () -> 
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
				return true;
			}
			return error("Expected a number");
		});
	}	

	private boolean error(String message) 
	{
		if ( lexer.offset() >= lastErrorOffset ) {
			lastErrorOffset = lexer.offset();
			lastErrorMsg = message;
		}
		return false;
	}
	
	private boolean errorLater(String message) 
	{
		if ( lexer.offset() > lastErrorOffset ) {
			lastErrorOffset = lexer.offset();
			lastErrorMsg = message;
		}
		return false;
	}	
}