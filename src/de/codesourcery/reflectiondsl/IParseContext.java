package de.codesourcery.reflectiondsl;

import java.util.List;

public interface IParseContext 
{
	public void saveState();

	public void recallState();

	public void dropState();

	public Object getResult();

	public void pushValue(Object value);

	public List<Object> popN(int n);

	public Object pop();

	public void applyOperator(OperatorType op);

	public void pushFunctionInvocation(String functionName,List<Object> args);
}