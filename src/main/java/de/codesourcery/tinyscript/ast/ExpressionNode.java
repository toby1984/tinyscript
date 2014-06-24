package de.codesourcery.tinyscript.ast;

import static de.codesourcery.tinyscript.ast.ASTNode.NodeType.EXPRESSION;

public final class ExpressionNode extends ASTNode {

	public ExpressionNode() {
		super(EXPRESSION);
	}

	@Override
	public boolean isLiteralValue() {
		return false;
	}
	
	@Override
	public ExpressionNode copyNodeHook() {
		return new ExpressionNode();
	}		
}