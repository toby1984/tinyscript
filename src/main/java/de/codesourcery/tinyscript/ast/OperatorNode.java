package de.codesourcery.tinyscript.ast;

import static de.codesourcery.tinyscript.ast.ASTNode.NodeType.OPERATOR;
import de.codesourcery.tinyscript.eval.OperatorType;

public final class OperatorNode extends ASTNode {

	public OperatorType type;
	
	public OperatorNode(OperatorType op) {
		super(OPERATOR);
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

	@Override
	public boolean isLiteralValue() {
		return false;
	}

	@Override
	public OperatorNode copyNodeHook() {
		return new OperatorNode(this.type);
	}
}