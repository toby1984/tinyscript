package de.codesourcery.tinyscript.eval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import de.codesourcery.tinyscript.ast.ASTNode;

public enum OperatorType 
{
	/*
	 * Note that operator precedence and associativity is implicitly handled by the parser. 
	 */
	GT(">",2,4,DataType.NUMBER) 
	{
		@Override public Object applyHook(Object left,Object... right)		
		{
			return compare(left,right[0], (a,b) -> NumericType.compare(a,b) > 0 );			
		}

		@Override protected Class<?> calculateType(List<Class<?>> data) { return Boolean.class; }
	},
	LT("<",2,4,DataType.NUMBER) 
	{
		@Override public Object applyHook(Object left,Object... right)		
		{
			return compare(left,right[0], (a,b) -> NumericType.compare(a,b) < 0 );
		}		
		@Override protected Class<?> calculateType(List<Class<?>> data) { return Boolean.class; }	
	},
	GTE(">=",2,4,DataType.NUMBER) {
		@Override public Object applyHook(Object left,Object... right)		
		{
			return compare(left,right[0], (a,b) -> NumericType.compare(a,b) >= 0 );
		}
		@Override protected Class<?> calculateType(List<Class<?>> data) { return Boolean.class; }		
	},
	LTE("<=",2,4,DataType.NUMBER) {
		@Override public Object applyHook(Object left,Object... right)		
		{
			return compare(left,right[0], (a,b) -> NumericType.compare(a,b) <= 0 );
		}	
		@Override protected Class<?> calculateType(List<Class<?>> data) { return Boolean.class; }		
	},	
	ASSIGNMENT("=",2,0,DataType.BOOLEAN,DataType.values()) 
	{
		public Object apply(Object arg1,Object... additional) {
			
			if ( arg1 == null || !(arg1 instanceof Identifier ) ) {
				throw new IllegalArgumentException("LHS operand of assignment needs to be an identifier");
			}
			if ( additional == null || additional.length != 1 ) {
				throw new IllegalArgumentException("RHS operand of assignment needs to be exactly one value, got "+(additional== null?0:additional.length));
			}			
			return additional[0];
		}
		
		public final Class<?> getResultType(List<ASTNode> data) 
		{
			return data.get(1).getDataType();
		}
		
		@Override protected Class<?> calculateType(List<Class<?>> data) {
			throw new RuntimeException("Shouldn't been called");
		}		
		
		@Override
		public Object applyHook(Object left, Object... rightArgs) 
		{
			throw new RuntimeException("Should never be called"); // ...because apply() is overridden
		}
	},
	EQ("==",2,3,DataType.BOOLEAN,DataType.NUMBER,DataType.STRING) {

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
		@Override protected Class<?> calculateType(List<Class<?>> data) { return Boolean.class; }			
	},
	NEQ("!=",2,3,DataType.BOOLEAN,DataType.NUMBER,DataType.STRING) {

		@Override
		public Object applyHook(Object arg1, Object... additional) 
		{
			return ! ((Boolean) EQ.apply(arg1,additional));
		}
		@Override protected Class<?> calculateType(List<Class<?>> data) { return Boolean.class; }				
	},
	PLUS("+",2,5,DataType.NUMBER,DataType.STRING) { 
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
		@Override protected Class<?> calculateType(List<Class<?>> data) 
		{ 
			if ( data.get(0) == String.class || data.get(1) == String.class ) {
				return String.class;
			}
			return NumericType.getWiderType( data.get(0) , data.get(1) ).getJavaType();
		}			
	},
	MINUS("-",2,5,DataType.NUMBER) {
		@Override public Object applyHook(Object left,Object... right)		
		{
			return numericBinaryOp(left,right[0], (a,b) -> NumericType.getType(a).minus( a, b)  );
		}
		@Override protected Class<?> calculateType(List<Class<?>> data) 
		{ 
			return NumericType.getWiderType( data.get(0) , data.get(1) ).getJavaType();
		}			
	},
	TIMES("*",2,6,DataType.NUMBER) { 
		@Override public Object applyHook(Object left,Object... right)		
		{
			return numericBinaryOp(left,right[0], (a,b) -> NumericType.getType(a).times( a, b)  );
		}
		@Override protected Class<?> calculateType(List<Class<?>> data) 
		{ 
			return NumericType.getWiderType( data.get(0) , data.get(1) ).getJavaType();
		}			
	},
	DIVIDE("/",2,6,DataType.NUMBER) {
		@Override public Object applyHook(Object left,Object... right)		
		{
			return numericBinaryOp(left,right[0], (a,b) -> NumericType.getType(a).divide( a, b)  );
		}
		@Override protected Class<?> calculateType(List<Class<?>> data) 
		{ 
			return NumericType.getWiderType( data.get(0) , data.get(1) ).getJavaType();
		}			
	},
	NOT("not",1,7,DataType.BOOLEAN) 
	{
		@Override public Object applyHook(Object left,Object... right)		
		{
			return ! ((Boolean) left);
		}
		public boolean isLeftAssociative() {
			return false;
		}		
		@Override protected Class<?> calculateType(List<Class<?>> data) { return Boolean.class; }			
	},
	AND("and",2,2,DataType.BOOLEAN) {
		@Override public Object applyHook(Object left,Object... right)		
		{
			return ((Boolean) left) && ((Boolean) right[0]);
		}
		@Override protected Class<?> calculateType(List<Class<?>> data) { return Boolean.class; }			
	},
	OR("or",2,1,DataType.BOOLEAN) 
	{
		@Override public Object applyHook(Object left,Object... right)		
		{
			return ((Boolean) left) || ((Boolean) right[0]);
		}
		@Override protected Class<?> calculateType(List<Class<?>> data) { return Boolean.class; }			
	};
	
	private final String symbol;
	private Set<DataType> supportedTypes;
	private final int argumentCount;
	private final int precedence;	
	
	private OperatorType(String op,int argumentCount,int precedence,DataType supportedType1,DataType... supportedTypes) {
		this.symbol=op;
		this.precedence = precedence;
		this.argumentCount = argumentCount;
		this.supportedTypes = new HashSet<>();
		this.supportedTypes.add( supportedType1 );
		if ( supportedTypes != null ) {
			this.supportedTypes.addAll( Arrays.asList( supportedTypes ) );
		}
	}	
	
	public boolean isLeftAssociative() {
		return true;
	}
	
	public boolean isRightAssociative() {
		return ! isLeftAssociative();
	}	
	
	public int getPrecedence() {
		return precedence;
	}
	
	public boolean isArithmeticOperator() {
		return false;
	}
	
	public String toPrettyString() {
		return symbol;
	}
	
	public int getArgumentCount() {
		return argumentCount;
	}
	
	public String getSymbol() {
		return symbol;
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
	
	protected final void assertSupportedArguments(Class<?>[] clazzes) 
	{
		int i = 0;
		for ( Class<?> cl : clazzes) {
			if ( ! supportedTypes.contains( DataType.getDataType( cl ) ) ) {
				throw new IllegalArgumentException("Operand "+i+" with type "+cl+" is not supported for operator "+this+" ( supported types are: "+supportedTypes+")");
			}
			i++;
		}
	}
	
	protected final void assertSupportedArguments(List<DataType> list) 
	{
		for ( int i = 0 ; i < list.size() ; i++ ) {
			if ( ! supportedTypes.contains( list.get(i) ) ) 
			{
				throw new IllegalArgumentException("Operand "+i+" with type "+list.get(i)+" is not supported for operator "+this+" ( supported types are: "+supportedTypes+")");
			}
		}
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
	
	public Object apply(Object arg1,Object... additional) 
	{
		int count = 1 + ( additional != null ? additional.length : 0);
		if ( count != argumentCount ) {
			throw new UnsupportedOperationException("Operator "+this+" cannot be called with "+count+" arguments (requires "+argumentCount+")");
		}
		final Class<?>[] types = new Class<?>[count];
		int i = 0;
		types[i++]= arg1.getClass();
		if ( additional != null ) {
			for ( Object obj : additional) {
				types[i++] = obj.getClass();
			}
		}
		assertSupportedArguments(types);
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
	
	protected abstract Class<?> calculateType(List<Class<?>> data);
	
	public Class<?> getResultType(List<ASTNode> data) {

		if ( data.size() != getArgumentCount() ) {
			throw new IllegalArgumentException("Operator "+this+" requires "+getArgumentCount()+" arguments, got "+data.size());			
		}
		
		final Class<?>[] types = new Class<?>[ data.size() ];
		int i = 0;
		for ( ASTNode n : data ) 
		{
			if ( n.getDataType() == null ) {
				throw new IllegalArgumentException("Node "+n+" has no data type set");
			}
			System.out.println("Node "+n+" has data type "+n.getDataType());
			types[i++]=n.getDataType();
		}
		assertSupportedArguments( types );
		return calculateType(Arrays.asList( types ) );
	}
	
	protected final DataType getDataType(ASTNode node) 
	{
		switch(node.getNodeType()) 
		{
			case BOOLEAN:
				return DataType.BOOLEAN;
			case NUMBER:
				return DataType.NUMBER;
			case STRING:
				return DataType.STRING;
			default:
				throw new RuntimeException("Unhandled node: "+node);
		}
	}
}