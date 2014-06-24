package de.codesourcery.tinyscript.parser;

import de.codesourcery.tinyscript.eval.OperatorType;


public interface IParseListener
{
	public void pushValue(Object value);

	public void pushOperator(OperatorType op);
	
	public void pushArgumentDelimiter();
	
	public void pushOpeningParens();
	
	public void pushClosingParens();	
	
	public void pushFunctionInvocation(String functionName);
	
	public void pushExpressionDelimiter();
}