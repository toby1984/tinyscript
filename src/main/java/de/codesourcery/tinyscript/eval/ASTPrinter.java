package de.codesourcery.tinyscript.eval;

import java.util.Iterator;

import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.ast.FunctionCallNode;
import de.codesourcery.tinyscript.ast.ILiteralNode;
import de.codesourcery.tinyscript.ast.OperatorNode;
import de.codesourcery.tinyscript.ast.VariableNode;

public class ASTPrinter {

	public String print(ASTNode tree) 
	{
		final StringBuffer result = new StringBuffer();
		visit(tree,result);
		return result.toString();
	}
	
	private void maybeAppendType(ASTNode node,StringBuffer buffer) {
		if ( node.getDataType() != null ) {
			buffer.append("{"+node.getDataType().getSimpleName()+"}");
		}
	}
	
	private void visit(ASTNode node,StringBuffer buffer) {
		
		switch(node.getNodeType()) 
		{
		case AST:
			for (Iterator<ASTNode> iterator = node.children().iterator(); iterator .hasNext();) 
			{
				final ASTNode child = iterator.next();
				visit( child , buffer );
				if ( iterator.hasNext() ) {
					buffer.append("\n");
				}
			}
			return;
		case BOOLEAN:
		case NUMBER:
			buffer.append( ((ILiteralNode) node).value() );
			maybeAppendType(node,buffer);
			return;
		case STRING:
			buffer.append("'").append( ((ILiteralNode) node).value() ).append("'");		
			maybeAppendType(node,buffer);			
			return;
		case EXPRESSION:
			buffer.append("(");
			visit(node.child(0),buffer);
			buffer.append(")");					
			maybeAppendType(node,buffer);			
			return;
		case FUNCTION_CALL:
			final FunctionCallNode fn = (FunctionCallNode) node;			
			buffer.append( fn.functionName.getSymbol() ).append("(");
			for ( int i = 0 ; i < fn.getChildCount() ; i++ ) {
				visit( fn.child(i) , buffer );
				if ((i+1) < fn.getChildCount() ) {
					buffer.append(",");
				}
			}
			buffer.append(")");
			maybeAppendType(node,buffer);			
			return;			
		case OPERATOR:
			print((OperatorNode) node , buffer );
			return;			
		case VARIABLE:
			buffer.append(" ").append( ((VariableNode) node).name.getSymbol() ).append(" ");
			return;
		}
		throw new RuntimeException("Internal error,unhandled node: "+node);
	}
	
	public final void print(OperatorNode node,StringBuffer buffer) {
		
		final OperatorType type = node.type;
		switch ( type.getArgumentCount() ) 
		{
			case 1:
				buffer.append(  type.getSymbol() );
				maybeAppendType(node,buffer);						
				visit( node.child(0) , buffer);
				return;
			case 2:
				if ( type == OperatorType.ASSIGNMENT ) {
					buffer.append("(");
				}
				visit( node.child(0) , buffer);
				maybeAppendType(node.child(0),buffer);		
				buffer.append(  type.getSymbol() );
				visit( node.child(1) , buffer);
				if ( type == OperatorType.ASSIGNMENT ) {
					buffer.append(")");
				}				
				return;				
			default:
				buffer.append(  type.getSymbol() );
				maybeAppendType(node,buffer);		
				buffer.append("(");
				for ( ASTNode child :node.children()) {
					visit(child,buffer);
				}
				buffer.append(")");
		}
	}	
}
