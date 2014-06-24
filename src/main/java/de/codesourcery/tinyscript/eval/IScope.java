package de.codesourcery.tinyscript.eval;

public interface IScope 
{
	public Object readVariable(Identifier name);
	
	public void writeVariable(Identifier name,Object value);	
	
	public Class<?> getDataType(Identifier name);
}