package de.codesourcery.tinyscript.ast;

import static de.codesourcery.tinyscript.ast.ASTNode.NodeType.VARIABLE;
import de.codesourcery.tinyscript.eval.Identifier;

public final class VariableNode extends ASTNode {

	public Identifier name;
	public boolean isGenerated;
	
	public VariableNode(Identifier name) {
		super(VARIABLE);
		this.name = name;
	}
	
	@Override
	public String toString() {
		return "Variable[ "+name+" ]";
	}

	@Override
	public boolean isLiteralValue() {
		return false;
	}	
	
	@Override
	public VariableNode copyNodeHook() {
		final VariableNode  result = new VariableNode(this.name);
		result.isGenerated = this.isGenerated;
		return result;
	}		
}