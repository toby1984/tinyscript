package de.codesourcery.tinyscript.ast;

public class NumberNode extends ASTNode {

	public Number value;
	
	public NumberNode(Number number) {
		this.value = number;
	}
	
	@Override
	public String toString() {
		return "Number[ "+value+" ]";
	}	
}
