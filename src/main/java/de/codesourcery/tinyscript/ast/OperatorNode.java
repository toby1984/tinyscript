package de.codesourcery.tinyscript.ast;

import de.codesourcery.tinyscript.OperatorType;

public class OperatorNode extends ASTNode {

	public OperatorType type;
	
	public OperatorNode(OperatorType op) {
		this.type =op;
	}
	
	public OperatorType getOperatorType() {
		return type;
	}

	@Override
	public String toString() 
	{
		return ""+type;
	}	
	
	public boolean hasAllOperands() {
		return type.getArgumentCount() == children().size();
	}
	
	public String toPrettyString() {
		return type == null ? "NULL" : type.toPrettyString();
	}
}
