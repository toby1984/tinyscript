package de.codesourcery.tinyscript.eval;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.ast.FunctionCallNode;
import de.codesourcery.tinyscript.ast.ILiteralNode;
import de.codesourcery.tinyscript.ast.OperatorNode;
import de.codesourcery.tinyscript.ast.VariableNode;

public class Evaluator {

	private static final Result VOID_RESULT = new Result(null) 
	{
		public Object value() {
			throw new UnsupportedOperationException("value() invoked on VOID result");
		}		
		
		public boolean isVoid() {
			return true;
		}
	};
	
	private Object target;
	private final Map<Long,List<Method>> methodCache = new HashMap<>();
	
	private IScope variableResolver = new IScope() {

		@Override
		public Object readVariable(Identifier name) {
			throw new RuntimeException("Unknown variable "+name);
		}

		@Override
		public void writeVariable(Identifier name, Object value) {
			throw new RuntimeException("Assignment of "+value+" to "+name+" not implemented");			
		}

		@Override
		public Class<?> getDataType(Identifier name) {
			throw new RuntimeException("getDataType("+name+") not implemented");
		}
	};		
	
	public static final long hash(Identifier methodName,List<Object> arguments) 
	{
		return hash(methodName.getSymbol() , arguments );
	}
	
	public static final long hash(String methodName,List<Object> arguments) 
	{
		int result = 31 + methodName.hashCode();
		final int len = arguments.size();
		for ( int i = 0 ; i < len ; i++) 
		{
			final Class<?> clazz = arguments.get(i).getClass();
			result = 31 * result + clazz.getName().hashCode();
		}
		return result;		
	}
	
	public static final long hash(String methodName,Object[] arguments) 
	{
		int result = 31 + methodName.hashCode();
		final int len = arguments.length;
		for ( int i = 0 ; i < len ; i++) 
		{
			final Class<?> clazz = arguments[i].getClass();
			result = 31 * result + clazz.getName().hashCode();
		}
		return result;		
	}	
	
	public void setTarget(Object target) 
	{
		if ( ! this.methodCache.isEmpty() ) {
			if ( target == null || target.getClass() != this.target.getClass() ) {
				this.methodCache.clear();
			}
		}
		this.target = target;
	}
	
	public Evaluator(Object target) {
		this.target=target;
	}
	
	public Object getTarget() {
		return target;
	}
	
	protected static class Result 
	{
		private final Object value;
		
		public Result(Object value) {
			this.value = value;
		}
		
		public Object value() {
			return value;
		}
		
		public boolean isVoid() {
			return false;
		}
		
		@Override
		public String toString() {
			return isVoid() ? "<VOID>" : ""+value();
		}
	}
	
	public Result evaluate(ASTNode node) {
		
		switch( node.getNodeType() ) 
		{
			case AST:
				if ( node.hasNoChildren() ) {
					return VOID_RESULT;
				}
				if ( node.getChildCount() == 1 ) {
					return evaluate( node.child(0) );
				}
				throw new IllegalArgumentException("Don't know how to evaluate AST with "+node.getChildCount()+" node");
			case FUNCTION_CALL:
				return evalFunctionCall( node );			
			case BOOLEAN:
			case NUMBER:
			case STRING:
				return result( ((ILiteralNode) node).value() );
			case OPERATOR:
				return evalOperator(node);
			case VARIABLE:
				return result( variableResolver.readVariable( ((VariableNode) node).name  ) );
			default:
				throw new RuntimeException("Internal error, unhandled node: "+node);		
		}
	}
	
	private Result evalFunctionCall(ASTNode node) 
	{
		final FunctionCallNode func = (FunctionCallNode) node;
		final List<Object> operands = new ArrayList<>();
		for ( ASTNode child : func.children() ) 
		{
			Result result = evaluate( child );
			if ( result.isVoid() ) {
				throw new RuntimeException("Internal error,evaluating node "+child+" yielded VOID but function call "+func+" required a value");
			}
			operands.add( result.value() );
		}
		return invokeFunction( func.getFunctionName() , operands );		
	}

	private Result evalOperator(ASTNode node) 
	{
		final OperatorNode operatorNode = (OperatorNode) node;

		// special case: assignment always needs to be executed (because it's a side-effect)		
		if ( operatorNode.type == OperatorType.ASSIGNMENT ) 
		{
			final Result rhs = evaluate( node.child(1 ) );
			if ( rhs.isVoid() ) {
				throw new IllegalArgumentException("Cannot assign VOID value to "+node.child(0) );
			}
			if ( !( node.child(0) instanceof VariableNode ) ) {
				throw new IllegalArgumentException("LHS of assignment is no variable but "+node.child(0));
			}
			variableResolver.writeVariable( ((VariableNode) node.child(0)).name , rhs.value );
			return rhs;
		}		
		
		final List<Object> operands = new ArrayList<>();
		for ( ASTNode child : node.children() ) {
			Result result = evaluate( child );
			if ( result.isVoid() ) {
				throw new RuntimeException("Internal error,evaluating node "+child+" yielded VOID but operand "+node+" required some value");
			}
			operands.add( result.value() );
		}
		return result( operatorNode.type.apply( operands ) );
	}
	
	private static Result result(Object object) {
		return new Result(object);
	}
	
	protected Result invokeFunction(Identifier functionName,List<Object> arguments) 
	{
		if ( target == null ) {
			throw new RuntimeException("Cannot perform function call, no target object set");
		}
		
		final Method m = getMethod(functionName,arguments);
		
		final Object[] realArguments = arguments.toArray( new Object[arguments.size()] );
		final Object result;
		try {
			result = m.invoke( target ,  realArguments );
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to invoke "+functionName+"("+arguments+") on "+target+" (method: "+m+")");
		}
		if ( m.getReturnType() != Void.TYPE && m.getReturnType() != Void.class ) {
			return result( result );
		}
		return VOID_RESULT;
	}
	
	protected Method getMethod(Identifier functionName,List<Object> arguments) 
	{
		final Long key = hash( functionName , arguments);
		
		List<Method> candidates = methodCache.get(key);
		if ( candidates != null ) {
			final int len = candidates.size();
			for ( int i = 0 ; i < len ; i++ ) 
			{
				final Method m = candidates.get(i);
				if ( m.getName().equals( functionName.getSymbol() ) ) {
					return m;
				}
			}
		} 
		
		final Method m = findMethod(functionName,arguments,target.getClass().getMethods());
		if ( candidates == null ) {
			candidates = new ArrayList<Method>();
			methodCache.put(key,  candidates );
		}
		candidates.add( m );
		return m;
	}
	
	public static Method findMethod(Identifier functionName,List<Object> arguments,Method[] availableMethods) 
	{
		final int argCount = arguments.size();
		
		final Class<?>[] argumentTypes = new Class<?>[arguments.size()];
		for ( int i = 0 ; i < argCount ; i++) 
		{
			argumentTypes[i] = arguments.get(i).getClass();
		}
		return findMethod( functionName , argumentTypes , availableMethods );
	}
	
	public static Method findMethod(Identifier functionName,Class<?>[] argumentTypes,Method[] availableMethods) 
	{
		final List<Method> candidates = new ArrayList<Method>();
		final int argCount = argumentTypes.length;
		
outer:		
		for ( Method m : availableMethods ) 
		{
			if ( m.getName().equals( functionName.getSymbol() ) ) 
			{
				final int modifiers = m.getModifiers();
				final boolean isStatic = Modifier.isStatic( modifiers );
				final boolean isAbstract = Modifier.isAbstract ( modifiers );
				if (  ! isStatic  && ! isAbstract && ! Modifier.isPrivate( modifiers ) && ! Modifier.isProtected( modifiers ) ) 
				{
					if ( argCount == m.getParameterCount() ) 
					{
						for ( int i = 0 ; i < argCount ; i++) 
						{
							final Class<?> cl1 = m.getParameterTypes()[i];
							final Class<?> cl2 = argumentTypes[i];
							if ( ! isAssignableFrom( cl1 , cl2 ) ) {
								continue outer;
							}
						}
						candidates.add( m );						
					} 
					else if ( isVarArgsMethod(m) ) 
					{
						Class<?> cl1 = m.getParameterTypes()[0].getComponentType();
						for ( int i = 0 ; i < argCount ; i++ ) 
						{
							Class<?> cl2 = argumentTypes[i];
							if ( ! isAssignableFrom( cl1 , cl2 ) ) {
								continue outer;
							}
						}
						candidates.add( m );
					}
				}
			}
		}	
		
		if ( candidates.isEmpty() ) {
			throw new RuntimeException("Found no suitable candidates for "+functionName+"("+Arrays.toString( argumentTypes )+")");
		}
		
		if ( candidates.size() == 1 ) 
		{
			return candidates.get(0);
		}
		
		// find most specific method
		int bestDistance = Integer.MAX_VALUE;
		Method bestMatch = null;
		for ( Method m : candidates ) 
		{
			final int distance;
			if ( isVarArgsMethod( m ) ) {
				distance = 100000;
			} else {
				distance = distance( m , argumentTypes );
			}
			if ( bestMatch == null || distance < bestDistance ) {
				bestMatch = m;
				bestDistance = distance;
			}
		}
		return bestMatch;
	}
	
	private static boolean isAssignableFrom(Class<?> lhs,Class<?> rhs) {
		if ( lhs.isAssignableFrom( rhs ) ) {
			return true;
		}
		if ( lhs.isPrimitive() != rhs.isPrimitive() ) {
			Class<?> t1 = toObject(lhs);
			Class<?> t2 = toObject(rhs);
			return t1.isAssignableFrom( t2 );
		}
		return false;
	}
	
	private static Class<?> toObject(Class<?> cl) {
		if ( cl.isPrimitive() ) {
			if ( cl == Boolean.TYPE ) {
				return Boolean.class;
			} 
			if ( cl == Long.TYPE ) {
				return Long.class;
			} 
			if ( cl == Integer.TYPE ) {
				return Integer.class;
			} 
			if ( cl == Short.TYPE ) {
				return Integer.class;
			} 
			if ( cl == Byte.TYPE ) {
				return Integer.class;
			} 
			if ( cl == Double.TYPE ) {
				return Integer.class;
			} 
			if ( cl == Float.TYPE ) {
				return Integer.class;
			}  
		}
		return cl;
	}
	
	private static int distance(final Method m,final Class<?>[] arguments) {
	
		int distance = 0;
		for ( int i = 0 ; i < arguments.length ; i++ ) {
			distance += distance( m.getParameterTypes()[i] , arguments[i] );
		}
		return distance;
	}
	
	private static int distance(Class<?> actual,Class<?> expected) {
		
		int result = 0;
		Class<?> current = actual;

		if ( expected.isInterface() ) 
		{
			while( current != null )
			{
				if ( current == expected ) {
					return result;
				}
				final int count = current.getInterfaces().length;
				for ( int i = 0 ; i < count ; i++) 
				{
					if ( current.getInterfaces()[i] == expected ) {
						return result;
					}
				}
				result += 1;				
				current = current.getSuperclass();
			}
			return result;
		} 
		
		while( current != null && current != expected ) 
		{
			result+=1;
			current = current.getSuperclass();
		}
		return result;
	}
	
	private static boolean isVarArgsMethod(Method m) {
		return m.getParameterCount() == 1 && m.getParameterTypes()[0].isArray();
	}
	
	public void setVariableResolver(IScope variableResolver) {
		this.variableResolver = variableResolver;
	}
}