package de.codesourcery.tinyscript.ast;

import java.util.Iterator;

import de.codesourcery.tinyscript.Identifier;

public class FunctionCallNode extends ASTNode {

	public final Identifier functionName;
	
	public FunctionCallNode(Identifier functionName) {
		this.functionName = functionName;
	}
	
	@Override
	public String toString() 
	{
		StringBuilder buffer = new StringBuilder();
		for (Iterator<ASTNode> it = children().iterator(); it.hasNext();) 
		{
			final ASTNode child = it.next();
			buffer.append( child.toString() );
			if ( it.hasNext() ) {
				buffer.append(",");
			}
		}
		return functionName+"("+buffer+")";
	}	
}
