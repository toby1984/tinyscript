package de.codesourcery.tinyscript.eval;

import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.ast.NumberNode;

public enum DataType 
{
	NUMBER {
		@Override public Object convert(Object input) 
		{
			switch( getDataType( input ) ) {
			case NUMBER:
				return input;
			default:
				throw new RuntimeException("Cannot convert "+input+" to "+this);
			}
		}
	},
	STRING {
		@Override public Object convert(Object input) 
		{
			switch( getDataType( input ) ) {
			case STRING:
				return input;
			case BOOLEAN:
			case NUMBER:
				return input.toString();
			default:
				throw new RuntimeException("Cannot convert "+input+" to "+this);
			}
		}		
	},BOOLEAN {
		@Override public Object convert(Object input) 
		{		
			switch( getDataType( input ) ) {
			case BOOLEAN:
				return input;
			default:
				throw new RuntimeException("Cannot convert "+input+" to "+this);
			}
		}		
	};

	public abstract Object convert(Object input);

	public static DataType getDataType(Object o) 
	{
		return getDataType( o.getClass() );
	}
	
	public static DataType getDataType(ASTNode node) 
	{
		switch( node.getNodeType() ) {
			case BOOLEAN:
				return DataType.BOOLEAN;
			case NUMBER:
				return DataType.NUMBER;
			case STRING:
				return DataType.STRING;
		}
		throw new IllegalArgumentException("Cannot determine datatype for "+node);	
	}
	
	public static DataType getDataType(Class<?> clazz) 
	{
		if ( clazz.isAssignableFrom( Number.class ) || 
			 clazz == Long.class || clazz == Integer.class || clazz == Short.class || clazz == Byte.class || clazz == Double.class || clazz == Float.class ||
			 clazz == Long.TYPE|| clazz == Integer.TYPE || clazz == Short.TYPE || clazz == Byte.TYPE || clazz == Double.TYPE|| clazz == Float.TYPE
		) 
		{
			return NUMBER;
		}
		if ( clazz.isAssignableFrom( String.class )) {
			return STRING;
		}
		if ( clazz.isAssignableFrom( Boolean.class )) {
			return BOOLEAN;
		}		
		throw new IllegalArgumentException("Cannot determine datatype for "+clazz);		
	}
}