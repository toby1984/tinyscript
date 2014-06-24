package de.codesourcery.tinyscript.ast;


public final class StringNode extends ASTNode implements ILiteralNode {

	public String value;

	public StringNode(String value) {
		super(NodeType.STRING);
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "String[ "+value+" ]";
	}	
	
	public boolean isLiteralValue() { return true; }
	
	@Override
	public String value() { return value; }		

	@Override
	public StringNode copyNodeHook() {
		return new StringNode(this.value);
	}		
}
