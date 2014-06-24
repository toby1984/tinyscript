package de.codesourcery.tinyscript.parser;

import de.codesourcery.tinyscript.ast.AST;
import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.ast.BooleanNode;
import de.codesourcery.tinyscript.ast.FunctionCallNode;
import de.codesourcery.tinyscript.ast.NumberNode;
import de.codesourcery.tinyscript.ast.OperatorNode;
import de.codesourcery.tinyscript.ast.StringNode;
import de.codesourcery.tinyscript.ast.VariableNode;
import de.codesourcery.tinyscript.eval.Identifier;
import de.codesourcery.tinyscript.eval.OperatorType;
import de.codesourcery.tinyscript.parser.ExpressionToken.ExpressionTokenType;

public class ASTBuilder implements IParseListener {

	private static final boolean DEBUG = false;
	
	private ShuntingYard yard = new ShuntingYard();
	
	private final AST ast = new AST();
	
	public ASTBuilder() {
	}
	
	private void debug(String message) {
		if ( DEBUG ) {
			System.out.println("VALUE STACK: "+message);	
		}
	}
	
	public AST getResult() 
	{
		if ( ! yard.isEmpty() ) {
			pushExpressionDelimiter();
		}
		return ast;
	}
	
	public void pushExpressionDelimiter() 
	{
		ASTNode result = yard.getResult(-1);
		if (result != null ) {
			ast.add( result );
		}
		yard = new ShuntingYard();
	}
	
	@Override
	public void pushValue(Object value) 
	{
		ASTNode node = toNode(value);
		yard.pushValue( node  );
		debug("PUSH: "+node);
	}
	
	private ASTNode toNode(Object value) {
		final ASTNode node;
		if ( value instanceof String) {
			node = new StringNode( (String) value );
		} else if ( value instanceof Number) {
			node = new NumberNode( (Number) value );
		} else if ( value instanceof Boolean) {
			node = new BooleanNode( (Boolean) value );
		}  else if ( value instanceof Identifier) {
			node = new VariableNode( (Identifier) value );
		} else {
			throw new RuntimeException("Unhandled value: "+value);
		}
		return node;
	}
	
	@Override
	public void pushOperator(OperatorType op) 
	{
		yard.pushOperator( new ExpressionToken(ExpressionTokenType.OPERATOR,new OperatorNode( op ) ) );
	}
	
	@Override
	public void pushFunctionInvocation(String functionName) 
	{
		debug("CALL FUNCTION: "+functionName+"( ... )");
		final FunctionCallNode node = new FunctionCallNode( new Identifier(functionName ) );
		yard.pushOperator( new ExpressionToken(ExpressionTokenType.FUNCTION , node ) );
	}

	@Override
	public void pushOpeningParens() {
		yard.pushOperator( new ExpressionToken(ExpressionTokenType.PARENS_OPEN , -1 ) );
	}

	@Override
	public void pushClosingParens() {
		yard.pushOperator( new ExpressionToken(ExpressionTokenType.PARENS_CLOSE , -1 ) );		
	}

	@Override
	public void pushArgumentDelimiter() {
		yard.pushOperator( new ExpressionToken(ExpressionTokenType.ARGUMENT_DELIMITER , -1 ) );
	}
}