package de.codesourcery.tinyscript.ast;

public class AST extends ASTNode {

	public AST() {
		super(NodeType.AST);
	}

	@Override
	public AST copyNodeHook() {
		return new AST();
	}

	@Override
	public boolean isLiteralValue() {
		return false;
	}
}
