package de.codesourcery.tinyscript.ast;

import java.lang.reflect.Method;
import java.util.Iterator;

import de.codesourcery.tinyscript.eval.Identifier;

public final class FunctionCallNode extends ASTNode {

	public final Identifier functionName;
	
	public Method targetMethod; // populated by Typer
	
	public FunctionCallNode(Identifier functionName) {
		super(NodeType.FUNCTION_CALL);
		this.functionName = functionName;
	}
	
	public Identifier getFunctionName() {
		return functionName;
	}
	
	@Override
	public String toString() 
	{
		StringBuilder buffer = new StringBuilder();
		for (Iterator<ASTNode> it = children().iterator(); it.hasNext();) 
		{
			final ASTNode child = it.next();
			buffer.append( child.toString() );
			if ( it.hasNext() ) {
				buffer.append(",");
			}
		}
		return functionName.getSymbol()+"("+buffer+")";
	}

	@Override
	public boolean isLiteralValue() {
		return false;
	}

	@Override
	public FunctionCallNode copyNodeHook() {
		FunctionCallNode result = new FunctionCallNode(this.functionName);
		result.targetMethod = targetMethod;
		return result;
	}	
}