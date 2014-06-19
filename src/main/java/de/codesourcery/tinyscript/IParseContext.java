package de.codesourcery.tinyscript;

import java.util.List;

public interface IParseContext<T> 
{
	public void saveState();

	public void recallState();

	public void dropState();

	public void pushValue(Object value);

	public List<T> popN(int n);
	
	public T pop();

	public void applyOperator(OperatorType op);

	public void pushFunctionInvocation(String functionName,List<T> args);
}