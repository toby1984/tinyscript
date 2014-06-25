package de.codesourcery.tinyscript.eval;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import junit.framework.TestCase;
import de.codesourcery.tinyscript.ast.AST;
import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.parser.ASTBuilder;
import de.codesourcery.tinyscript.parser.ExpressionParser;
import de.codesourcery.tinyscript.parser.Lexer;
import de.codesourcery.tinyscript.parser.Scanner;

public class ByteCodeCompilerTest extends TestCase {

	private static final Integer VALUE1 = new Integer(3);
	private static final Integer VALUE2 = new Integer(7);
	private IScope scope;
	private Object target;
	
	private final Map<Identifier,Object> vars = new HashMap<>();
	
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
	
	public static final class TestTarget {
		
		public Integer value1() {
			return VALUE1;
		}
		
		public Integer value2() {
			return VALUE2;
		}
		
		public Integer subInteger1(Integer a,Integer b) {
			System.out.println("==== Test method invoked ===");
			return a-b;
		}		
		
		public int subInteger2(int a,int b) {
			System.out.println("==== Test method invoked ===");
			return a-b;
		}	
		
		public int subInteger3(int a,Integer b) {
			System.out.println("==== Test method invoked ===");
			return a-b;
		}	
		
		public Integer subInteger4(int a,int b) {
			System.out.println("==== Test method invoked ===");
			return a-b;
		}			
	}
	
	public void test1() throws Exception {
		assertEquals( new Integer(24) , debug("12+((1+3)*3)") );
		assertEquals( new Integer(12) , debug("12") );
		assertEquals( new Integer(15) , debug("(1+4)*3") );
		assertEquals( new Integer(13) , debug("1+4*3") );
		assertEquals( new Integer(15) , debug("3*(1+4)") );
		assertEquals( new Integer(13) , debug("4*3+1") );
		assertEquals( new Integer(4) , debug("7-3" ) );
		assertEquals( VALUE1 , debug("value1()" ) );
		assertEquals( VALUE2 , debug("value2()" ) );
		assertEquals( VALUE1-VALUE2 , debug("subInteger1(value1(),value2())" ) );
		assertEquals( VALUE1-VALUE2 , debug("subInteger2(value1(),value2())" ) );
		assertEquals( VALUE1-VALUE2 , debug("subInteger3(value1(),value2())" ) );
		assertEquals( VALUE1-VALUE2 , debug("subInteger4(value1(),value2())" ) );
		
		assertEquals(Boolean.TRUE , debug("1 < 2" ) );
		assertEquals(Boolean.TRUE , debug("1 <= 2" ) );
		assertEquals(Boolean.TRUE , debug("2 > 1" ) );
		assertEquals(Boolean.TRUE , debug("2 >= 1" ) );
		assertEquals(Boolean.TRUE , debug("1 == 1" ) );
		assertEquals(Boolean.TRUE , debug("1 == 1" ) );
		
		assertEquals(Boolean.FALSE , debug("1 > 2" ) );
		assertEquals(Boolean.FALSE , debug("1 >= 2" ) );
		assertEquals(Boolean.FALSE , debug("2 < 1" ) );
		assertEquals(Boolean.FALSE , debug("2 <= 1" ) );
		assertEquals(Boolean.FALSE , debug("1 != 1" ) );
		assertEquals(Boolean.FALSE , debug("1 == 2" ) );		
		
		assertEquals(Boolean.FALSE , debug("false" ) );
		assertEquals(Boolean.TRUE , debug("true" ) );
		
		assertEquals(Boolean.TRUE , debug("NOT false" ) );
		assertEquals(Boolean.FALSE, debug("NOT true" ) );
		
		doTestBooleanOperators( "${A} OR ${B}", (a,b) -> a || b );
		doTestBooleanOperators( "${A} AND ${B}", (a,b) -> a && b );
		doTestBooleanOperators( "NOT ${A} OR ${B}", (a,b) -> ! a || b );	
	}			
	
	public void testBroken() throws Exception {

	}
	
	private void doTestBooleanOperators(String expr,BiFunction<Boolean, Boolean, Boolean> block) throws Exception {
		for ( int i = 0 ; i <= 1 ; i++) 
		{
			for ( int j = 0 ; j <= 1 ; j++) 
			{
				String tmpExpr = expr.replaceAll( Pattern.quote("${A}"), i == 0 ? "false" : "true");
				tmpExpr = tmpExpr.replaceAll( Pattern.quote("${B}"), j == 0 ? "false" : "true");
				assertEquals( block.apply( Boolean.valueOf( i==1 ), Boolean.valueOf( j==1 ) ) , debug( tmpExpr ) );
			}
		}
	}
	
	private Object debug(String expression) throws Exception 
	{
		System.out.println("###########################################");
		System.out.println("### Testing: "+expression);
		System.out.println("###########################################");
		target = new TestTarget();
		scope = null;
		
		ExpressionCompiler comp = new ExpressionCompiler("TestClass");
		
		final AST ast = parse( expression );
		
		ast.prettyPrint();
		
		byte[] data = comp.compile( ast , TestTarget.class  );
		
		final FileOutputStream out = new FileOutputStream( new File("/tmp/TestClass.class" ) );
		out.write( data );
		out.close();
		System.out.println( data.length+" bytes written.");

		final CompiledExpression<TestTarget> instance = compile( data , (TestTarget) target , scope );
		Object result = instance.apply();
		System.out.println("RESULT = "+result+" ("+(result==null?"NULL":result.getClass().getName())+")");
		return result;
	}
	
	private <T> CompiledExpression<T> compile(byte[] bytecode,T target,IScope scope) throws Exception 
	{
		final Class<?> cl = defineClass(bytecode);
		return (CompiledExpression<T>) cl.getConstructor( Object.class , IScope.class ).newInstance( target , scope );
	}
	
	private Class<?> defineClass(final byte[] bytecode) throws ClassNotFoundException  {
		
		final ClassLoader loader = new ClassLoader( getClass().getClassLoader() ) 
		{
			private final Map<String,Class<?>> loaded = new HashMap<>();
			
			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException 
			{
				try {
					return super.findClass(name);
				} 
				catch(ClassNotFoundException e) 
				{
					if ( name.equals("TestClass" ) ) 
					{
						Class<?> result = loaded.get( name );
						if ( result == null ) {
							result = defineClass( name ,  bytecode ,  0 ,  bytecode.length );
							loaded.put( name ,  result );
						}
						return result;
					} else {
						throw e;
					}
				}
			}
		};
		return loader.loadClass("TestClass");
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
				return ByteCodeCompilerTest.this.scope.readVariable(name);
			}
			
			@Override
			public void writeVariable(Identifier name, Object value) {
				ByteCodeCompilerTest.this.scope.writeVariable(name, value);
			}

			@Override
			public Class<?> getDataType(Identifier name) {
				return ByteCodeCompilerTest.this.scope.getDataType(name);
			}
		});
		
		simplifier.setResolveVariables(resolveVariables);
		
		simplifier.setFoldConstants( false ); // disable constant folding for testing purposes
		
		AST ast = (AST) simplifier.simplify( result ,  target );
		new Typer(scope,target == null ? Object.class : target.getClass() ).type( ast );
		return ast;
	}	
}