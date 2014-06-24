package de.codesourcery.tinyscript.eval;

public abstract class CompiledExpression 
{
	protected final Object target;
	protected final IScope variableResolver;
	
	
	public CompiledExpression(Object target, IScope variableResolver) 
	{
		this.target = target;
		this.variableResolver = variableResolver;
	}

	public abstract Object evaluate(Object target);
}
