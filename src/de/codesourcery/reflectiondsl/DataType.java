package de.codesourcery.reflectiondsl;

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

	public static DataType getDataType(Object o) {
		if ( o instanceof Number) {
			return NUMBER;
		}
		if ( o instanceof String) {
			return STRING;
		}
		if ( o instanceof Boolean) {
			return BOOLEAN;
		}		
		throw new IllegalArgumentException("Cannot determine datatype for "+o);
	}
}