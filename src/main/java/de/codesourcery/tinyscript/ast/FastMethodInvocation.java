package de.codesourcery.tinyscript.ast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import de.codesourcery.tinyscript.eval.Identifier;

public final class FastMethodInvocation extends ASTNode 
{
	private final Object target;
	public final Method methodToInvoke;
	public final Object[] arguments;
	private final boolean isVoidMethod;
	
	public FastMethodInvocation(Object target, Method methodToInvoke,Object[] arguments) 
	{
		super(NodeType.FAST_METHOD_INVOCATION);
		
		if ( target == null ) throw new IllegalArgumentException("target must not be NULL");
		if ( methodToInvoke == null ) throw new IllegalArgumentException("methodToInvoke must not be NULL");
		if ( arguments == null ) throw new IllegalArgumentException("arguments must not be NULL");
		
		this.target = target;
		this.methodToInvoke = methodToInvoke;
		methodToInvoke.setAccessible(true);
		this.arguments = arguments;
		isVoidMethod = methodToInvoke.getReturnType() == Void.TYPE || methodToInvoke.getReturnType() == Void.class;
	}
	
	public Identifier getFunctionName() {
		return new Identifier( methodToInvoke.getName() );
	}
	
	public FastMethodInvocation(Object target, Method methodToInvoke,List<Object> arguments) 
	{
		this( target , methodToInvoke , arguments.toArray( new Object[ arguments.size() ] ) );
	}
	
	public Object invoke() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException 
	{
		return methodToInvoke.invoke( target ,  arguments );
	}
	
	public boolean isVoidMethod() {
		return isVoidMethod;
	}
	
	@Override
	public String toString() {
		return "FastMethodInvocation on "+target+" , method "+methodToInvoke+" , arguments = "+Arrays.toString( arguments );
	}

	@Override
	public boolean isLiteralValue() {
		return false;
	}

	@Override
	public FastMethodInvocation copyNodeHook() 
	{
		final Object[] newargs = new Object[ this.arguments.length ];
		System.arraycopy( this.arguments ,  0 ,  newargs ,  0, newargs.length );
		return new FastMethodInvocation(this.target,this.methodToInvoke,newargs);
	}
}