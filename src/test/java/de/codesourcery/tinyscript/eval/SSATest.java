package de.codesourcery.tinyscript.eval;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import de.codesourcery.tinyscript.ast.AST;
import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.parser.ASTBuilder;
import de.codesourcery.tinyscript.parser.ExpressionParser;
import de.codesourcery.tinyscript.parser.Lexer;
import de.codesourcery.tinyscript.parser.Scanner;

public class SSATest extends TestCase {

	private IScope scope;
	private Object target;
	
	private final Map<Identifier,Object> vars = new HashMap<>();
	
	protected static class TestClass 
	{
		public long apply(long a,long b) {
			return a+b;
		}
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		vars.clear();
		scope = new IScope() 
		{
			@Override
			public void writeVariable(Identifier name, Object value) {
				vars.put(name,value);
			}
			
			@Override
			public Object readVariable(Identifier name) {
				Object result = vars.get(name);
				if ( result == null ) {
					if ( ! vars.containsKey( name ) ) {
						throw new RuntimeException("Unknown variable "+name);
					}
					throw new RuntimeException("Vvariable "+name+" has a NULL value?");
				}
				return result;
			}
			
			@Override
			public Class<?> getDataType(Identifier name) 
			{
				return readVariable( name ).getClass();
			}
		};
	}
	
	public void test1() 
	{
//		final String expr = "((1+2)*apply(1,2)+23)*3";
		
		vars.put(new Identifier("a") , 10L );
		vars.put(new Identifier("b") , 20L );
		final String expr = "apply(1+a,3*b)";
		target = new TestClass();
		AST result = parse(expr);
		
		new Typer(target,scope).type( result );
		
		result.prettyPrint();
		
		result = new SSARewriter().rewriteAST( result );
		
		System.out.println("Rewritten: "+new ASTPrinter().print( result ));
	}

	private AST parse(String expression) {
		return parse(expression,false);
	}
	
	private AST parse(String expression,boolean resolveVariables) {
		
		final Lexer l = new Lexer(new Scanner(expression ) );
		
		final ASTBuilder builder = new ASTBuilder();
		try {
			new ExpressionParser().parse( l ,  builder );
		} catch (ParseException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		final ASTNode result = builder.getResult();
		
		System.out.println(expression+" => "+new ASTPrinter().print( result ));		

		final ASTSimplifier simplifier = new ASTSimplifier() 
		{
			@Override
			protected boolean hasNoSideEffectsHook(Method method) 
			{
				return "inliningTest".equals( method.getName() );
			}
		};
		simplifier.setScope( new IScope() {
			
			@Override
			public Object readVariable(Identifier name) {
				return SSATest.this.scope.readVariable(name);
			}
			
			@Override
			public void writeVariable(Identifier name, Object value) {
				SSATest.this.scope.writeVariable(name, value);
			}

			@Override
			public Class<?> getDataType(Identifier name) {
				return SSATest.this.scope.getDataType(name);
			}
		} );
		simplifier.setResolveVariables(resolveVariables);
		
		return (AST) simplifier.simplify( result ,  target );
	}	
}