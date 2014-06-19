package de.codesourcery.tinyscript.ast;

import de.codesourcery.tinyscript.Identifier;

public class VariableNode extends ASTNode {

	public Identifier name;
	
	public VariableNode(Identifier name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return "Variable[ "+name+" ]";
	}	
}
