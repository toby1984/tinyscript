package de.codesourcery.reflectiondsl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class ParseContext implements IParseContext {

	private static final boolean DEBUG = true;
	
	private Stack<Object> values = new Stack<>();
	
	private final Stack<Stack<Object>> state = new Stack<>();
	
	private final Object target;
	
	public ParseContext(Object target) {
		this.target = target;
	}
	
	@Override
	public void saveState() 
	{
		final Stack<Object> copy = new Stack<>();
		values.stream().forEach( value -> copy.push(value) );
		state.push( copy );
		debug("--- saveState --- ");	
	}
	
	private void debug(String message) {
		if ( DEBUG ) {
			System.out.println("VALUE STACK: "+message);	
		}
	}
	
	@Override
	public void recallState() {
		values = state.pop();
		debug("--- recallState --- ");
	}
	
	@Override
	public void dropState() {
		state.pop();
		debug("--- dropState --- ");		
	}	
	
	@Override
	public Object getResult() {
		if ( values.size() != 1 ) {
			throw new IllegalStateException("Bad value stack size: "+values.size());
		}
		return values.pop();
	}
	
	@Override
	public void pushValue(Object value) {
		values.push(value);
		debug("PUSH: "+value);
	}
	
	@Override
	public List<Object> popN(int n) 
	{
		final List<Object> result = new ArrayList<>( n );
		for ( int i = 0 ;i < n ; i++) {
			result.add( values.pop() );
		}
		Collections.reverse(result);
		debug("POP("+n+"): "+result);
		return result;
	}
	
	@Override
	public Object pop() {
		Object result = values.pop();
		debug("POP: "+result);
		return result;
	}
	
	@Override
	public void applyOperator(OperatorType op) 
	{
		if ( values.size() < op.getArgumentCount() ) {
			throw new RuntimeException("Too few arguments for operator "+op+" (required: "+op.getArgumentCount()+" arguments)");
		}
		final List<Object> arguments = popN( op.getArgumentCount() );
		debug("APPLY OPERATOR: "+op+"( "+arguments+" )");
		values.push( op.apply( arguments ) );
	}
	
	@Override
	public void pushFunctionInvocation(String functionName, List<Object> args) {
		debug("CALL FUNCTION: "+functionName+"( "+args+" )");		
		values.push( invokeMethod(functionName,args ) );
	}
	
	private Object invokeMethod(String name,List<Object> args) 
	{
		final List<Class<?>> paramTypes = args.stream().map( argument -> argument.getClass() ).collect( Collectors.toList() );
		
		try {
			Method m = target.getClass().getMethod( name ,  paramTypes.toArray( new Class[paramTypes.size()] ) );
			return m.invoke( target ,  args.toArray( new Object[args.size()] ) );
		} 
		catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException("Failed to invoke method "+name+" with "+args,e);
		}
	}
}