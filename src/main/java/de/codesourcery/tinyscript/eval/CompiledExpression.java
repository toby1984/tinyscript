package de.codesourcery.tinyscript.eval;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class CompiledExpression<T> 
{
	protected final Class<T> targetClass;
	protected final T target;
	protected final IScope variableResolver;

	private final Map<Long,List<Method>> methodCache = new HashMap<>();	
	
	public CompiledExpression(T target, Class<T> targetClass,IScope variableResolver) 
	{
		this.target = target;
		this.targetClass = targetClass;
		this.variableResolver = variableResolver;
	}

	public abstract Object apply();
	
	protected final Object invokeMethodOnTarget(String name,Object[] parameters) 
	{
		try {
			return getMethodOnTarget(name, parameters).invoke(target,parameters);
		} 
		catch(Exception e) 
		{
			System.err.println("Failed to invoke method "+name+" with "+parameters);
			Throwable cause = e;
			if ( e instanceof InvocationTargetException) {
				cause = ((InvocationTargetException)e).getTargetException();
			}
			cause.printStackTrace();
			if ( cause instanceof Error) {
				throw (Error) cause;
			}
			if ( cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			throw new RuntimeException("Failed to invoke method "+name+" with "+Arrays.toString(parameters),cause);
		}
	}
	
	private final Method getMethodOnTarget(String name,Object[] parameters) {
		
		final Long hash = Evaluator.hash(name, parameters );
		List<Method> methods = methodCache.get(hash);
		if ( methods != null ) 
		{
			if ( methods.size() == 1 ) {
				return methods.get(0);
			}
			return Evaluator.findMethod( new Identifier(name) , Arrays.asList(parameters), methods.toArray( new Method[ methods.size() ] ) );
		}
		Method m = Evaluator.findMethod( new Identifier(name) , Arrays.asList(parameters), target.getClass().getDeclaredMethods() );
		if ( methods == null ) {
			methods = new ArrayList<>();
			methodCache.put( hash , methods );
		}
		methods.add( m );
		return m;
	}
}