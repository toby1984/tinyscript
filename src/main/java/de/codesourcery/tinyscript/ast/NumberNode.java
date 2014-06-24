package de.codesourcery.tinyscript.ast;


public final class NumberNode extends ASTNode implements ILiteralNode {

	public Number value;
	
	public NumberNode(Number number) {
		super(NodeType.NUMBER);
		this.value = number;
	}
	
	@Override
	public String toString() {
		return "Number[ "+value+" ]";
	}	
	
	public boolean isLiteralValue() { return true; }
	
	@Override
	public Number value() { return value; }
	
	@Override
	public NumberNode copyNodeHook() {
		return new NumberNode(this.value);
	}		
}
