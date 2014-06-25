package de.codesourcery.tinyscript.eval;


public class TestClass extends CompiledExpression<Integer> {

	public TestClass(Integer target, IScope variableResolver) {
		super(target, Integer.class,variableResolver);
	}

	@Override
	public Object apply() 
	{
		return target.byteValue();
	}
	
	protected Object myMethod(String name,Object[] args) {
		return null;
	}
}
