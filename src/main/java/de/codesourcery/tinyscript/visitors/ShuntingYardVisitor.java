package de.codesourcery.tinyscript.visitors;

import de.codesourcery.tinyscript.ExpressionToken;
import de.codesourcery.tinyscript.ExpressionToken.ExpressionTokenType;
import de.codesourcery.tinyscript.IParseContext;
import de.codesourcery.tinyscript.Identifier;
import de.codesourcery.tinyscript.OperatorType;
import de.codesourcery.tinyscript.ShuntingYard;
import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.ast.BooleanNode;
import de.codesourcery.tinyscript.ast.FunctionCallNode;
import de.codesourcery.tinyscript.ast.NumberNode;
import de.codesourcery.tinyscript.ast.OperatorNode;
import de.codesourcery.tinyscript.ast.StringNode;
import de.codesourcery.tinyscript.ast.VariableNode;

public class ShuntingYardVisitor implements IParseContext<ASTNode> {

	private static final boolean DEBUG = true;
	
	private final ShuntingYard yard = new ShuntingYard();
	
	public ShuntingYardVisitor() {
	}
	
	private void debug(String message) {
		if ( DEBUG ) {
			System.out.println("VALUE STACK: "+message);	
		}
	}
	
	public ASTNode getResult() 
	{
		return yard.getResult( -1 );
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