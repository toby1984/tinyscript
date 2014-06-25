package de.codesourcery.tinyscript.eval;


public abstract class CompiledExpression<T> 
{
	protected final T target;
	protected final IScope variableResolver;

	public CompiledExpression(T target, IScope variableResolver) 
	{
		this.target = target;
		this.variableResolver = variableResolver;
	}

	public abstract Object apply();
}