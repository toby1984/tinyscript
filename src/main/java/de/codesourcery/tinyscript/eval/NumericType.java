package de.codesourcery.tinyscript.eval;

import java.util.Arrays;
import java.util.Optional;

public enum NumericType
{
	DOUBLE(true,8,Double.class,Double.TYPE) {
		@Override public Object plus(Object a, Object b) { return   ((Double) a) + ((Double) b ); }
		@Override public Object minus(Object a, Object b) {return   ((Double) a) - ((Double) b ); }
		@Override public Object times(Object a, Object b) { return  ((Double) a) * ((Double) b ); }
		@Override public Object divide(Object a, Object b) { return ((Double) a) / ((Double) b ); }
		@Override public int compareHook(Object a, Object b) { return Double.compare( (Double) a, (Double) b); }		
		@Override public Class<?> getJavaType() { return Double.class; }
	},
	FLOAT(true,4,Float.class,Float.TYPE) {
		@Override public Object plus(Object a, Object b) { return   ((Float) a) + ((Float) b ); }
		@Override public Object minus(Object a, Object b) {return   ((Float) a) - ((Float) b ); }
		@Override public Object times(Object a, Object b) { return  ((Float) a) * ((Float) b ); }
		@Override public Object divide(Object a, Object b) { return ((Float) a) / ((Float) b ); }	
		@Override public int compareHook(Object a, Object b) { return Float.compare( (Float) a, (Float) b); }			
		@Override public Class<?> getJavaType() { return Float.class; }
	},
	LONG(false,8,Long.class,Long.TYPE) {
		@Override public Object plus(Object a, Object b) { return   ((Long) a) + ((Long) b ); }
		@Override public Object minus(Object a, Object b) {return   ((Long) a) - ((Long) b ); }
		@Override public Object times(Object a, Object b) { return  ((Long) a) * ((Long) b ); }
		@Override public Object divide(Object a, Object b) { return ((Long) a) / ((Long) b ); }		
		@Override public int compareHook(Object a, Object b) { return Long.compare( (Long) a, (Long) b); }		
		@Override public Class<?> getJavaType() { return Long.class; }		
	},
	INT(false,4,Integer.class,Integer.TYPE) {
		@Override public Object plus(Object a, Object b) { return   ((Integer) a) + ((Integer) b ); }
		@Override public Object minus(Object a, Object b) {return   ((Integer) a) - ((Integer) b ); }
		@Override public Object times(Object a, Object b) { return  ((Integer) a) * ((Integer) b ); }
		@Override public Object divide(Object a, Object b) { return ((Integer) a) / ((Integer) b ); }		
		@Override public int compareHook(Object a, Object b) { return Integer.compare( (Integer) a, (Integer) b); }		
		@Override public Class<?> getJavaType() { return Integer.class; }				
	},		
	SHORT(false,2,Short.class,Short.TYPE) {
		@Override public Object plus(Object a, Object b) { return   ((Short) a) + ((Short) b ); }
		@Override public Object minus(Object a, Object b) {return   ((Short) a) - ((Short) b ); }
		@Override public Object times(Object a, Object b) { return  ((Short) a) * ((Short) b ); }
		@Override public Object divide(Object a, Object b) { return ((Short) a) / ((Short) b ); }		
		@Override public int compareHook(Object a, Object b) { return Short.compare( (Short) a, (Short) b); }			
		@Override public Class<?> getJavaType() { return Short.class; }	
	},
	BYTE(false,1,Byte.class,Byte.TYPE) {
		@Override public Object plus(Object a, Object b) { return   ((Byte) a) + ((Byte) b ); }
		@Override public Object minus(Object a, Object b) {return   ((Byte) a) - ((Byte) b ); }
		@Override public Object times(Object a, Object b) { return  ((Byte) a) * ((Byte) b ); }
		@Override public Object divide(Object a, Object b) { return ((Byte) a) / ((Byte) b ); }		
		@Override public int compareHook(Object a, Object b) { return Byte.compare( (Byte) a, (Byte) b); }		
		@Override public Class<?> getJavaType() { return Byte.class; }			
	};
	
	private final Class<?>[] clazzes;
	private final boolean isFloatingPoint;
	private final int size;
	
	private NumericType(boolean isFloatingPoint,int size,Class<?>... clazzes) {
		this.clazzes=clazzes;
		this.isFloatingPoint=isFloatingPoint;
		this.size = size;
	}
	
	private boolean matches(Class<?> clazz) {
		for ( Class<?> cl : this.clazzes ) {
			if ( cl == clazz ) {
				return true;
			}
		}
		return false;
	}
	
	public static NumericType getType(Class<?> clazz) {
		Optional<NumericType> result = Arrays.stream( values() ).filter( type -> type.matches(clazz) ).findFirst();
		if ( ! result.isPresent() ) {
			throw new IllegalArgumentException("Found no numeric type for "+clazz);
		}
		return result.get();
	}	
	
	public static NumericType getType(Object o) {
		return getType( o.getClass() );
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
	
	public static NumericType getWiderType(Class<?> a,Class<?> b) 
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
	
	public static NumericType getWiderType(Object a,Object b) 
	{
		return getWiderType(a.getClass() , b.getClass() );
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
	
	public static NumericType fromJavaType(Class<?> clazz) {
		for ( NumericType t : values() ) {
			if ( t.getJavaType().isAssignableFrom( clazz ) ) {
				return t;
			}
		}
		throw new IllegalArgumentException("Found no NumericType that is assignable from "+clazz);
	}

	public abstract Class<?> getJavaType();
	
	public boolean isAssignableFrom(Class<?> rhs) {
		
		NumericType rhsType = fromJavaType(rhs);
		if ( isFloatingPoint ) { // float = float / double = float are OK
			return this.size >= rhsType.size;
		}
		if ( rhsType.isFloatingPoint ) { // int = float requires explicit conversion
			return false;
		}
		return this.size >= rhsType.size;
	}
}