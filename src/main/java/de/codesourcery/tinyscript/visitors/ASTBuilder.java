package de.codesourcery.tinyscript.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import de.codesourcery.tinyscript.IParseContext;
import de.codesourcery.tinyscript.Identifier;
import de.codesourcery.tinyscript.OperatorType;
import de.codesourcery.tinyscript.ast.AST;
import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.ast.BooleanNode;
import de.codesourcery.tinyscript.ast.FunctionCallNode;
import de.codesourcery.tinyscript.ast.NumberNode;
import de.codesourcery.tinyscript.ast.OperatorNode;
import de.codesourcery.tinyscript.ast.StringNode;
import de.codesourcery.tinyscript.ast.VariableNode;

public class ASTBuilder implements IParseContext<ASTNode> {

	private static final boolean DEBUG = true;
	
	private Stack<ASTNode> values = new Stack<>();
	
	private final Stack<Stack<ASTNode>> state = new Stack<>();
	
	private final AST ast = new AST();
	
	public ASTBuilder() {
	}
	
	@Override
	public void saveState() 
	{
		final Stack<ASTNode> copy = new Stack<>();
		values.stream().forEach( value -> copy.push(value) );
		state.push( copy );
		debug("--- saveState --- ");	
	}
	
	private void debug(String message) {
		if ( DEBUG ) {
			System.out.println("VALUE STACK: "+message);	
		}
	}
	
	@Override
	public void recallState() {
		values = state.pop();
		debug("--- recallState --- ");
	}
	
	@Override
	public void dropState() {
		state.pop();
		debug("--- dropState --- ");		
	}	
	
	public AST getResult() 
	{
		if ( ! values.isEmpty() ) 
		{
			final List<ASTNode> list = new ArrayList<>(values);
			Collections.reverse(list);
			for ( ASTNode v : list ) {
				ast.add( v );
			}
		}
		return ast;
	}
	
	@Override
	public void pushValue(Object value) 
	{
		final ASTNode node = toNode(value);
		values.push( node );
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
	public List<ASTNode> popN(int n) 
	{
		final List<ASTNode> result = new ArrayList<>( n );
		for ( int i = 0 ;i < n ; i++) {
			result.add( values.pop() );
		}
		Collections.reverse(result);
		debug("POP("+n+"): "+result);
		return result;
	}
	
	@Override
	public ASTNode pop() {
		ASTNode result = values.pop();
		debug("POP: "+result);
		return result;
	}
	
	@Override
	public void applyOperator(OperatorType op) 
	{
		if ( values.size() < op.getArgumentCount() ) {
			throw new RuntimeException("Too few arguments for operator "+op+" (required: "+op.getArgumentCount()+" arguments)");
		}
		final List<ASTNode> arguments = popN( op.getArgumentCount() );
		debug("APPLY OPERATOR: "+op+"( "+arguments+" )");
		final OperatorNode node = new OperatorNode(op);
		node.addAll( arguments );
		values.push( node );
	}
	
	@Override
	public void pushFunctionInvocation(String functionName, List<ASTNode> args) {
		debug("CALL FUNCTION: "+functionName+"( "+args+" )");
		final FunctionCallNode node = new FunctionCallNode(new Identifier(functionName ));
		node.addAll(args);
		values.push( node );
	}	
}