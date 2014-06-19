package de.codesourcery.tinyscript.ast;


public class BooleanNode extends ASTNode {

	public Boolean value;
	
	public BooleanNode(Boolean value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "Boolean[ "+value+" ]";
	}
}
