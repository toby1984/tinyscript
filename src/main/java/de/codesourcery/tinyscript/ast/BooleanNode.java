package de.codesourcery.tinyscript.ast;


public final class BooleanNode extends ASTNode implements ILiteralNode {

	public Boolean value;
	
	public BooleanNode(Boolean value) {
		super(NodeType.BOOLEAN);
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "Boolean[ "+value+" ]";
	}
	
	public boolean isLiteralValue() { return true; }
	
	@Override
	public Boolean value() { return value; }

	@Override
	public BooleanNode copyNodeHook() {
		return new BooleanNode(this.value);
	}	
}
