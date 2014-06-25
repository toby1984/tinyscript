package de.codesourcery.tinyscript.eval;


public class TestClass extends CompiledExpression<Integer> {

	public TestClass(Integer target, IScope variableResolver) {
		super(target, Integer.class,variableResolver);
	}

	@Override
	public Object apply() 
	{
		boolean a = true;
		boolean b = false;
		return ! a || b;
	}	
}