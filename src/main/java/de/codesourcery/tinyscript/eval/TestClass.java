package de.codesourcery.tinyscript.eval;

public class TestClass extends CompiledExpression {

	public TestClass(Object target, IScope variableResolver) {
		super(target, variableResolver);
	}

	@Override
	public Object apply() {
//		Integer result = 45;
		// String result = "abc";
        String s = "test";
		return s;
	}
}
