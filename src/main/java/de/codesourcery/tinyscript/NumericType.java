package de.codesourcery.tinyscript;

import java.util.Arrays;

public enum NumericType
{
	DOUBLE(Double.class,true,8) {
		@Override public Object plus(Object a, Object b) { return   ((Double) a) + ((Double) b ); }
		@Override public Object minus(Object a, Object b) {return   ((Double) a) - ((Double) b ); }
		@Override public Object times(Object a, Object b) { return  ((Double) a) * ((Double) b ); }
		@Override public Object divide(Object a, Object b) { return ((Double) a) / ((Double) b ); }
		@Override public int compareHook(Object a, Object b) { return Double.compare( (Double) a, (Double) b); }		
	},
	FLOAT(Float.class,true,4) {
		@Override public Object plus(Object a, Object b) { return   ((Float) a) + ((Float) b ); }
		@Override public Object minus(Object a, Object b) {return   ((Float) a) - ((Float) b ); }
		@Override public Object times(Object a, Object b) { return  ((Float) a) * ((Float) b ); }
		@Override public Object divide(Object a, Object b) { return ((Float) a) / ((Float) b ); }	
		@Override public int compareHook(Object a, Object b) { return Float.compare( (Float) a, (Float) b); }			
	},
	LONG(Long.class,false,8) {
		@Override public Object plus(Object a, Object b) { return   ((Long) a) + ((Long) b ); }
		@Override public Object minus(Object a, Object b) {return   ((Long) a) - ((Long) b ); }
		@Override public Object times(Object a, Object b) { return  ((Long) a) * ((Long) b ); }
		@Override public Object divide(Object a, Object b) { return ((Long) a) / ((Long) b ); }		
		@Override public int compareHook(Object a, Object b) { return Long.compare( (Long) a, (Long) b); }				
	},
	INT(Integer.class,false,4) {
		@Override public Object plus(Object a, Object b) { return   ((Integer) a) + ((Integer) b ); }
		@Override public Object minus(Object a, Object b) {return   ((Integer) a) - ((Integer) b ); }
		@Override public Object times(Object a, Object b) { return  ((Integer) a) * ((Integer) b ); }
		@Override public Object divide(Object a, Object b) { return ((Integer) a) / ((Integer) b ); }		
		@Override public int compareHook(Object a, Object b) { return Integer.compare( (Integer) a, (Integer) b); }					
	},		
	SHORT(Short.class,false,2) {
		@Override public Object plus(Object a, Object b) { return   ((Short) a) + ((Short) b ); }
		@Override public Object minus(Object a, Object b) {return   ((Short) a) - ((Short) b ); }
		@Override public Object times(Object a, Object b) { return  ((Short) a) * ((Short) b ); }
		@Override public Object divide(Object a, Object b) { return ((Short) a) / ((Short) b ); }		
		@Override public int compareHook(Object a, Object b) { return Short.compare( (Short) a, (Short) b); }				
	},
	BYTE(Byte.class,false,1) {
		@Override public Object plus(Object a, Object b) { return   ((Byte) a) + ((Byte) b ); }
		@Override public Object minus(Object a, Object b) {return   ((Byte) a) - ((Byte) b ); }
		@Override public Object times(Object a, Object b) { return  ((Byte) a) * ((Byte) b ); }
		@Override public Object divide(Object a, Object b) { return ((Byte) a) / ((Byte) b ); }		
		@Override public int compareHook(Object a, Object b) { return Byte.compare( (Byte) a, (Byte) b); }			
	};
	
	private final Class<?> clazz;
	private final boolean isFloatingPoint;
	private final int size;
	
	private NumericType(Class<?> clazz,boolean isFloatingPoint,int size) {
		this.clazz=clazz;
		this.isFloatingPoint=isFloatingPoint;
		this.size = size;
	}
	
	public static NumericType getType(Object o) {
		return Arrays.stream( values() ).filter( type -> type.clazz == o.getClass() ).findFirst().get();
	}
	
	public abstract Object plus(Object a,Object b);
	
	public abstract Object minus(Object a,Object b);
	
	public abstract Object times(Object a,Object b);
	
	public abstract Object divide(Object a,Object b);		
	
	public abstract int compareHook(Object a,Object b);
	
	public static final int compare(Object a,Object b) 
	{
		Object left = a;
		Object right = b;
		final NumericType type; 
		if ( left.getClass() != right.getClass() ) 
		{
			type = getWiderType( left ,  right );
			left = type.convert( left );
			right = type.convert( right );
		} else {
			type = getType( left );
		}
		return type.compareHook(left,right);
	}
	
	public Object convert(Object a) {
		switch(this) {
			case BYTE:
				return ((Number) a).byteValue();
			case DOUBLE:
				return ((Number) a).doubleValue();
			case FLOAT:
				return ((Number) a).floatValue();
			case INT:
				return ((Number) a).intValue();
			case LONG:
				return ((Number) a).longValue();
			case SHORT:
				return ((Number) a).shortValue();
		}
		throw new RuntimeException("Internal error,unreachable code reached");
	}
	
	public static NumericType getWiderType(Object a,Object b) 
	{
		final NumericType typeA = getType(a);
		final NumericType typeB = getType(b);
		if ( typeA == null || typeB == null ) {
			return null;
		}
		if ( typeA.isFloatingPoint == typeB.isFloatingPoint ) {
			return typeA.size > typeB.size ? typeA : typeB;
		}
		return typeA.isFloatingPoint ? typeA : typeB;
	}

	public static boolean eq(Object a, Object b) 
	{
		Object left = a;
		Object right = b;
		if ( left.getClass() != right.getClass() ) 
		{
			final NumericType type = getWiderType( left ,  right );
			left = type.convert( left );
			right = type.convert( right );
		}
		return left.equals(right);		
	}
}