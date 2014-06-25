package de.codesourcery.tinyscript.eval;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.ast.FunctionCallNode;
import de.codesourcery.tinyscript.ast.NumberNode;
import de.codesourcery.tinyscript.ast.OperatorNode;
import de.codesourcery.tinyscript.ast.VariableNode;

public class Typer {

	private final Class<?> targetClass;
	private final IScope scope;
	
	private Map<Identifier,Class<?>> types = new HashMap<>();
	
	public Typer(IScope scope,Class<?> targetClass) 
	{
		this.scope = scope;
		this.targetClass = targetClass;
	}
	
	public void type(ASTNode tree) {
		types.clear();
		calculateTypes( tree );
	}
	
	private void calculateTypes(ASTNode tree) {
		
		switch(tree.getNodeType() ) 
		{
			case AST:
				Class<?> last = null;
				for ( ASTNode child : tree.children() ) {
					calculateTypes( child );
					last = child.getDataType();
				}
				tree.setDataType( last );
				return;
			case BOOLEAN:
				tree.setDataType( Boolean.class );
				return;
			case FUNCTION_CALL:
				final Class<?> returnType;
				for ( ASTNode child : tree.children() ) {
					calculateTypes( child );
				}
				
				{
					final Class<?>[] types = new Class<?>[ tree.getChildCount() ];
					int i = 0;
					for ( ASTNode child : tree.children() ) 
					{
						if ( child.getDataType() == null ) {
							throw new RuntimeException("Failed to determine type of function argument for "+tree);
						}
						types[i++] = child.getDataType();
					}
					
					final FunctionCallNode fn = (FunctionCallNode) tree;
					final Method method = Evaluator.findMethod( fn.getFunctionName() , types , targetClass.getMethods() );
					fn.targetMethod = method;
					returnType = method.getReturnType();
					
					if ( returnType != Void.class && returnType != Void.TYPE ) 
					{
						tree.setDataType( returnType );
					}
				}
				return;
			case NUMBER:
				tree.setDataType( NumericType.getType( ((NumberNode) tree).value ).getJavaType() );
				return;
			case OPERATOR:
				
				OperatorNode n = (OperatorNode) tree;
				if ( n.type == OperatorType.ASSIGNMENT ) 
				{
					final VariableNode lhs = (VariableNode) n.child(0);
					final ASTNode rhs = n.child(1);
					calculateTypes( rhs );
					
					final Class<?> type = rhs.getDataType();
					final Class<?> existingType = types.get( lhs.name );
					if ( existingType != null ) {
						if ( type != existingType ) {
							throw new RuntimeException("Internal error, type mismatch for variable "+lhs+": previous = "+existingType+" , current = "+type);
						}
					} else {
						types.put( lhs.name ,  type );
					}
					tree.setDataType( type );
					return;
				}
				for ( ASTNode child : tree.children() ) {
					calculateTypes( child );
				}				
				tree.setDataType( n.type.getResultType( n.children() ) );
				return;
			case STRING:
				tree.setDataType( String.class );
				return;				
			case VARIABLE:
				
				Class<?> type = types.get( ((VariableNode) tree).name );
				if ( type == null ) {
					type = scope.getDataType( ((VariableNode) tree).name );
				}
				tree.setDataType( type );
				return;
			default:
				break;
		}
	}
}