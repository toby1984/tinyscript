package de.codesourcery.tinyscript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public enum OperatorType 
{
	/*
	 * Note that operator precedence and associativity is implicitly handled by the parser. 
	 */
	GT(">",2,DataType.NUMBER) 
	{
		@Override public Object applyHook(Object left,Object... right)		
		{
			return compare(left,right[0], (a,b) -> NumericType.compare(a,b) > 0 );			
		}
	},
	LT("<",2,DataType.NUMBER) 
	{
		@Override public Object applyHook(Object left,Object... right)		
		{
			return compare(left,right[0], (a,b) -> NumericType.compare(a,b) < 0 );
		}		
	},
	GTE(">=",2,DataType.NUMBER) {
		@Override public Object applyHook(Object left,Object... right)		
		{
			return compare(left,right[0], (a,b) -> NumericType.compare(a,b) >= 0 );
		}			
	},
	LTE("<=",2,DataType.BOOLEAN) {
		@Override public Object applyHook(Object left,Object... right)		
		{
			return compare(left,right[0], (a,b) -> NumericType.compare(a,b) <= 0 );
		}			
	},	
	EQ("==",2,DataType.BOOLEAN,DataType.NUMBER,DataType.STRING) {

		@Override
		public Object applyHook(Object arg1, Object... additional) 
		{
			Object left = arg1;
			Object right = additional[0];
			
			DataType tLeft = DataType.getDataType( arg1 );
			DataType tRight  = DataType.getDataType( right );
			if ( tLeft != tRight ) {
				return false;
			}
			switch( tLeft ) 
			{
				case BOOLEAN:
					return ((Boolean) left).booleanValue() == ((Boolean) right).booleanValue();
				case NUMBER:
					return (Boolean) NumericType.eq( left , right );
				case STRING:
					return ((String) left).equals( right );
				default:
					throw new RuntimeException("Internal error,unhandled data type "+tLeft);
			}
		}
	},
	NEQ("!=",2,DataType.BOOLEAN,DataType.NUMBER,DataType.STRING) {

		@Override
		public Object applyHook(Object arg1, Object... additional) 
		{
			return ! ((Boolean) EQ.apply(arg1,additional));
		}
	},
	PLUS("+",2,DataType.NUMBER,DataType.STRING) {
		@Override public Object applyHook(Object left,Object... right)		
		{
			if ( DataType.getDataType( left ) == DataType.STRING ) 
			{
				final String r = (String) DataType.STRING.convert( right[0] );
				return ((String) left) + r;
			} 
			else if ( DataType.getDataType( right[0] ) == DataType.STRING ) {
				final String l = (String) DataType.STRING.convert( left );
				return l + ((String) right[0] ) ;				
			}
			return numericBinaryOp(left,right[0], (a,b) -> NumericType.getType(a).plus( a, b)  );
		}			
	},
	MINUS("-",2,DataType.NUMBER) {
		@Override public Object applyHook(Object left,Object... right)		
		{
			return numericBinaryOp(left,right[0], (a,b) -> NumericType.getType(a).minus( a, b)  );
		}		
	},
	TIMES("*",2,DataType.NUMBER) {
		@Override public Object applyHook(Object left,Object... right)		
		{
			return numericBinaryOp(left,right[0], (a,b) -> NumericType.getType(a).times( a, b)  );
		}		
	},
	DIVIDE("/",2,DataType.NUMBER) {
		@Override public Object applyHook(Object left,Object... right)		
		{
			return numericBinaryOp(left,right[0], (a,b) -> NumericType.getType(a).divide( a, b)  );
		}		
	},
	NOT("not",1,DataType.BOOLEAN) 
	{
		@Override public Object applyHook(Object left,Object... right)		
		{
			return ! ((Boolean) left);
		}
	},
	AND("and",2,DataType.BOOLEAN) {
		@Override public Object applyHook(Object left,Object... right)		
		{
			return ((Boolean) left) && ((Boolean) right[0]);
		}
	},
	OR("or",2,DataType.BOOLEAN) 
	{
		@Override public Object applyHook(Object left,Object... right)		
		{
			return ((Boolean) left) || ((Boolean) right[0]);
		}		
	};
	
	private final String symbol;
	private Set<DataType> supportedTypes;
	private final int argumentCount;
	
	private OperatorType(String op,int argumentCount,DataType supportedType1,DataType... supportedTypes) {
		this.symbol=op;
		this.argumentCount = argumentCount;
		this.supportedTypes = new HashSet<>();
		this.supportedTypes.add( supportedType1 );
		if ( supportedTypes != null ) {
			this.supportedTypes.addAll( Arrays.asList( supportedTypes ) );
		}
	}	
	
	public int getArgumentCount() {
		return argumentCount;
	}
	
	private static boolean compare(Object left,Object right,BiFunction<Object, Object , Boolean > func ) 
	{
		final NumericType type = NumericType.getWiderType( left ,  right );
		final Object a = type.convert(left); 
		final Object b = type.convert(right);
		return func.apply( a ,  b );
	}
	
	private static Object numericBinaryOp(Object left,Object right,BiFunction<Object, Object , Object > op) 
	{
		final NumericType type = NumericType.getWiderType( left ,  right );
		final Object a = type.convert(left); 
		final Object b = type.convert(right);
		return op.apply( a ,  b );
	}	
	
	public final boolean matchesSymbol(String s) {
		return s.equalsIgnoreCase( symbol );
	}
	
	protected final void assertSupportedArguments(Object value1,Object... values) 
	{
		final List<Object> list = new ArrayList<>();
		list.add(value1);
		if ( values != null ) {
			list.addAll(Arrays.asList(values));
		}
		list.forEach( value -> 
		{
			final DataType valueType = DataType.getDataType( value );
			if ( ! supportedTypes.contains(valueType) ) 
			{
				throw new IllegalArgumentException("Value "+value+" (type: "+valueType+") is not supported for operator "+this+" ( supported types are: "+supportedTypes+")");
			}
		});
	}
	
	public final Object apply(List<Object> arguments) {
		if ( arguments.size() == 1 ) {
			return apply(arguments.get(0));
		} 
		if ( arguments.size() > 1 ) {
			return apply(arguments.get(0) , arguments.subList(1 ,  arguments.size() ).toArray(new Object[ arguments.size()-1 ] ) );
		}
		throw new IllegalArgumentException("Operator requires "+argumentCount+" arguments, got "+arguments);
	}
	
	public final Object apply(Object arg1,Object... additional) 
	{
		int count = 1 + ( additional != null ? additional.length : 0);
		if ( count != argumentCount ) {
			throw new UnsupportedOperationException("Operator "+this+" cannot be called with "+count+" arguments (requires "+argumentCount+")");
		}
		assertSupportedArguments(arg1, additional);
		return applyHook(arg1,additional);
	}	
	
	public abstract Object applyHook(Object arg1,Object... additional);
	
	public static OperatorType getExactMatch(String input) 
	{
		final List<OperatorType> candidates = Arrays.stream( values() ).filter( operator -> operator.matchesSymbol( input ) ).collect(Collectors.toList());
		if ( candidates.size() > 1 ) {
			throw new IllegalArgumentException("Found "+candidates.size()+" matching operators for symbol '"+input+"' , expected exactly one");
		}
		return candidates.isEmpty() ? null : candidates.get(0);
	}	
	
	public static boolean mayBeOperator(String input) {
		final String s = input.toLowerCase();
		return Arrays.stream( values() ).anyMatch( operator -> operator.symbol.startsWith(s) );
	}
}