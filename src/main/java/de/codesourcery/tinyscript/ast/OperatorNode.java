package de.codesourcery.tinyscript.ast;

import java.util.Iterator;

import de.codesourcery.tinyscript.OperatorType;

public class OperatorNode extends ASTNode {

	public OperatorType type;
	
	public OperatorNode(OperatorType op) {
		this.type =op;
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
		return type+"("+buffer+")";
	}	
}
