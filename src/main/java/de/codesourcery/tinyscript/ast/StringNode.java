package de.codesourcery.tinyscript.ast;

public class StringNode extends ASTNode {

	public String value;

	public StringNode(String value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "String[ "+value+" ]";
	}	
}
