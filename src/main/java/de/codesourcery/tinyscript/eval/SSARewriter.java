package de.codesourcery.tinyscript.eval;

import java.util.ArrayList;
import java.util.List;

import de.codesourcery.tinyscript.ast.AST;
import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.ast.ASTNode.NodeType;
import de.codesourcery.tinyscript.ast.FunctionCallNode;
import de.codesourcery.tinyscript.ast.OperatorNode;
import de.codesourcery.tinyscript.ast.VariableNode;

public class SSARewriter 
{
	protected static final class VariableEntry 
	{
		public final Identifier name;
		public final int count;
		
		public VariableEntry(Identifier name, int count) {
			this.name = name;
			this.count = count;
		}
	}
	
	private int currentId = -1;
	
	private final AST ast = new AST();
	
	private Identifier newVariable() 
	{
		currentId++;
		return new Identifier("tmp_"+currentId);
	}
	
	public AST rewriteAST(AST ast) 
	{
		rewrite(ast);
		return this.ast;
	}
	
	private Identifier rewrite(ASTNode tree) 
	{
		switch(tree.getNodeType() ) 
		{
			case AST:
				for ( ASTNode child : tree.children() ) {
					rewrite( child );
				}
				return null;		
			case BOOLEAN:
			case NUMBER:
			case STRING:
				return addAssignment( tree );
			case VARIABLE:
				return ((VariableNode) tree).name;
			case FAST_METHOD_INVOCATION:
				return addAssignment( tree );				
			case FUNCTION_CALL:
			case OPERATOR:
				if ( isAssignment( tree ) ) 
				{
					ast.add( tree.copySubtree() );
					return ((VariableNode) tree.child(0)).name;
				}
				
				final List<ASTNode> ids = new ArrayList<>();
				for ( ASTNode child : tree.children() ) 
				{
					if ( child.isLiteralValue() ) {
						ids.add( child.copySubtree() );
						continue;
					}
					Identifier id = rewrite( child );
					if ( id == null ) {
						throw new RuntimeException("Internal error");
					}
					ids.add(new VariableNode( id ) );
				}
				
				final ASTNode newNode;
				if ( tree instanceof OperatorNode) {
					newNode = ((OperatorNode) tree).copyNode();					
				} else {
					newNode = ((FunctionCallNode) tree).copyNode();
				}
				for ( ASTNode id : ids ) {
					newNode.add( id );					
				}
				return addAssignment( newNode );
			default:
				throw new RuntimeException("Unhandled switch/case: "+tree);
		}
	}
	
	private Identifier addAssignment(ASTNode rhs) 
	{
		final Identifier newVariable = newVariable();
		
		final OperatorNode newNode = new OperatorNode(OperatorType.ASSIGNMENT);
		final VariableNode lhs = new VariableNode( newVariable );
		lhs.isGenerated = true;
		newNode.add( lhs );
		lhs.setDataType( rhs.getDataType() );
		newNode.add( rhs );
		ast.add( newNode );
		return newVariable;
	}
	
	private static boolean isAssignment(ASTNode node) {
		return node.getNodeType() == NodeType.OPERATOR && ((OperatorNode) node).type == OperatorType.ASSIGNMENT;
	}
}