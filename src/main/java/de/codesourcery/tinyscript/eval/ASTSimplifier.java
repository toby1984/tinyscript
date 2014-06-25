package de.codesourcery.tinyscript.eval;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.codesourcery.tinyscript.ast.AST;
import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.ast.BooleanNode;
import de.codesourcery.tinyscript.ast.FunctionCallNode;
import de.codesourcery.tinyscript.ast.ILiteralNode;
import de.codesourcery.tinyscript.ast.NumberNode;
import de.codesourcery.tinyscript.ast.OperatorNode;
import de.codesourcery.tinyscript.ast.StringNode;
import de.codesourcery.tinyscript.ast.VariableNode;

public class ASTSimplifier 
{
	private boolean resolveVariables = true;
	private boolean foldConstants = true;
	
	private IScope scope = new IScope() {

		@Override
		public Object readVariable(Identifier name) {
			throw new RuntimeException("Unknown variable "+name);
		}

		@Override
		public void writeVariable(Identifier name, Object value) {
			throw new RuntimeException("Assignment of "+value+" to "+name+" not implemented");				
		}
		
		@Override
		public Class<?> getDataType(Identifier name) {
			throw new RuntimeException("getDataType("+name+") not implemented");
		}		
	};		
	
	public void setFoldConstants(boolean foldConstants) {
		this.foldConstants = foldConstants;
	}
	
	public ASTNode simplify(ASTNode node,Object target) 
	{
		switch( node.getNodeType() ) 
		{
			case AST:
				final AST result = (AST) node.copyNode();
				for ( ASTNode child : node.children() ) {
					result.add( simplify( child , target ) );
				}
				return result;
			case FUNCTION_CALL:
				return simplifyFunctionCall(node, target);				
			case OPERATOR:
				if ( foldConstants ) {
					return simplifyOperator(node, target);
				}
				return node;
			case BOOLEAN:
			case STRING:
			case NUMBER:
				return node;			
			case VARIABLE:
				if ( resolveVariables ) {
					return toLiteralNode( scope.readVariable( ((VariableNode) node).name ) );
				}
				return node;			
			default:
				throw new RuntimeException("Internal error, unhandled node: "+node);					
		}
	}

	private ASTNode simplifyFunctionCall(ASTNode node, Object target) 
	{
		if ( node == null ) {
			throw new IllegalArgumentException("Node must not be NULL");
		}
		final FunctionCallNode func = (FunctionCallNode) node;
		final List<Object> operands = new ArrayList<>();
		final List<ASTNode> newChildren = new ArrayList<>();
		for ( ASTNode child : func.children() ) 
		{
			ASTNode value = simplify( child , target );
			if ( value == null ) {
				throw new RuntimeException("Simplifying node "+child+" resolved to VOID value, expected a function argument");
			}				
			if ( value.isLiteralValue() ) {
				operands.add( ((ILiteralNode) value).value() );
			}
			newChildren.add( value );
		}
		node.setChildren( newChildren );
		return node;
	}

	private ASTNode simplifyOperator(ASTNode node, Object target) 
	{
		final OperatorNode operatorNode = (OperatorNode) node;
		
		// special case: assignment always needs to be executed (because it's a side-effect)
		if ( operatorNode.type == OperatorType.ASSIGNMENT ) 
		{
			final ASTNode rhs = simplify( node.child(1 ) , target );
			if ( !( node.child(0) instanceof VariableNode ) ) {
				throw new IllegalArgumentException("LHS of assignment is no variable but "+node.child(0));
			}
			operatorNode.setChildren( Arrays.asList( node.child(0) , rhs ) );
			return operatorNode;
		}	
		
		final List<Object> operands = new ArrayList<>();
		final List<ASTNode> newChildren = new ArrayList<>();
		for ( ASTNode child : node.children() ) 
		{
			ASTNode value = simplify( child , target );
			if ( value == null ) {
				throw new RuntimeException("Simplifying node "+child+" resolved to VOID value, expected an operator argument");
			}					
			if ( value.isLiteralValue() ) {
				operands.add( ((ILiteralNode) value).value() );
			}
			newChildren.add( value );
		}
		if ( operands.size() == node.getChildCount() ) {
			return toLiteralNode( operatorNode.type.apply( operands ) );
		} 
		node.setChildren( newChildren );
		return node;
	}
	
	public void setResolveVariables(boolean resolveVariables) {
		this.resolveVariables = resolveVariables;
	}
	
	public boolean isResolveVariables() {
		return resolveVariables;
	}
	
	protected static ASTNode toLiteralNode(Object obj) 
	{
		if ( obj instanceof String) {
			return new StringNode( (String) obj );
		}
		if ( obj instanceof Boolean ) {
			return new BooleanNode( (Boolean) obj );
		}	
		if ( obj instanceof Number ) {
			return new NumberNode( (Number) obj );
		}		
		throw new RuntimeException("Don't know how to convert literal "+obj+" to AST node");
	}
	
	@SuppressWarnings("unused")
	private final boolean hasNoSideEffects(Method method) 
	{
		if ( method.getReturnType() == Void.TYPE || method.getReturnType() == Void.class ) 
		{
			// VOID methods must have a side-effect, otherwise having them would be pointless in the first place 
			return false;
		}
		return hasNoSideEffectsHook(method);
	}
	
	protected boolean hasNoSideEffectsHook(Method method) 
	{
		return false;
	}
	
	public void setScope(IScope scope) {
		this.scope = scope;
	}
}