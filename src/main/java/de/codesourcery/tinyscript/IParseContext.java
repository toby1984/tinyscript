package de.codesourcery.tinyscript;


public interface IParseContext<T> 
{
	public void pushValue(Object value);

	public void pushOperator(OperatorType op);
	
	public void pushArgumentDelimiter();
	
	public void pushOpeningParens();
	
	public void pushClosingParens();	
	
	public void pushFunctionInvocation(String functionName);
}