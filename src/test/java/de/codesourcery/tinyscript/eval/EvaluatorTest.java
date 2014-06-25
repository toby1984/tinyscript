package de.codesourcery.tinyscript.eval;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import junit.framework.TestCase;
import de.codesourcery.tinyscript.ast.AST;
import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.ast.NumberNode;
import de.codesourcery.tinyscript.eval.Evaluator.Result;
import de.codesourcery.tinyscript.parser.ASTBuilder;
import de.codesourcery.tinyscript.parser.ExpressionParser;
import de.codesourcery.tinyscript.parser.Lexer;
import de.codesourcery.tinyscript.parser.Scanner;

public class EvaluatorTest extends TestCase {

	private Evaluator evaluator;
	
	protected IScope resolver;
	
	@Override
	protected void setUp() throws Exception 
	{
		super.setUp();
		evaluator = new Evaluator(null);
		evaluator.setVariableResolver( new IScope() {
			
			@Override
			public Object readVariable(Identifier name) {
				return resolver.readVariable( name );
			}
			
			@Override
			public void writeVariable(Identifier name, Object value) {
				resolver.writeVariable( name ,  value );
			}
			
			@Override
			public Class<?> getDataType(Identifier name) {
				throw new RuntimeException("getDataType("+name+") not implemented");
			}			
		});
	}
	
	public static final class TestTarget1 
	{
		public int voidMethodInvocationCount;
		public int inliningTestMethodInvocationCount;
		public int noInliningTestMethodInvocationCount;		
		
		public Integer apply(Integer a,Integer b) {
			return a+b;
		}
		
		public Object apply(Object a,Object b) {
			throw new RuntimeException("Should not have been called");
		}
		
		public String apply(String a,String b) {
			return a+b;
		}		
		
		public void voidMethod() {
			voidMethodInvocationCount++;
		}
		
		public Object inliningTest(Integer a,Integer b) { // do NOT rename method, referenced in parse() method of this class
			inliningTestMethodInvocationCount++;
			return a*b;
		}
		
		public void noInliningTest(Integer a,Integer b) { // do NOT rename method, referenced in parse() method of this class
			noInliningTestMethodInvocationCount++;
		}		
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		evaluator = null;
	}
	
	public void testAssignment() 
	{
		final Map<Identifier,Object> vars = new HashMap<>();

		resolver = new IScope() 
		{
			@Override
			public void writeVariable(Identifier name, Object value) {
				vars.put( name ,  value );
			}
			
			@Override
			public Object readVariable(Identifier name) 
			{
				final Object result = vars.get(name);
				if ( result == null ) {
					throw new RuntimeException("Unknown variable "+name+" (value: "+result+")");
				}
				return result;
			}
			
			@Override
			public Class<?> getDataType(Identifier name) {
				throw new RuntimeException("getDataType("+name+") not implemented");
			}			
		};
		
		Result result = eval("a=1+2");
		assertFalse( result.isVoid() );
		final int iValue = ((Number) result.value()).intValue();
		assertEquals( (int) 3 , iValue  );
		
		assertEquals( (int) 3 , vars.get( new Identifier("a" ) ) );
	}
	
	public void testAdd1() {
		Result result = eval("1+2");
		assertFalse( result.isVoid() );
		final int iValue = ((Number) result.value()).intValue();
		assertEquals( (int) 3 , iValue  );
	}
	
	public void testAdd2() {
		Result result = eval("1+2+3");
		assertFalse( result.isVoid() );
		final int iValue = ((Number) result.value()).intValue();
		assertEquals( (int) 6 , iValue  );
	}	
	
	public void testSub1() {
		Result result = eval("4-3");
		assertFalse( result.isVoid() );
		final int iValue = ((Number) result.value()).intValue();
		assertEquals( (int) 1 , iValue  );
	}	
	
	public void testSub2() {
		Result result = eval("7-3-2");
		assertFalse( result.isVoid() );
		final int iValue = ((Number) result.value()).intValue();
		assertEquals( (int) 2 , iValue  );
	}	
	
	public void testParens1() {
		Result result = eval("12+((1+3)*3)");
		assertFalse( result.isVoid() );
		final int iValue = ((Number) result.value()).intValue();
		assertEquals( (int) 24 , iValue  );
	}	
	
	public void testSimplify() 
	{
		final AST result = (AST) parse( "12+((1+3)*3)" , true );
		
		assertEquals( 1 , result.getChildCount() );
		assertTrue( "Expected a NumberNode but got "+result, result.child(0) instanceof NumberNode );
		
		final int iValue = ((NumberNode) result.child(0)).value().intValue();
		assertEquals( (int) 24 , iValue  );
	}		
	
	public void testAddSub1() {
		Result result = eval("3+5-2");
		assertFalse( result.isVoid() );
		final int iValue = ((Number) result.value()).intValue();
		assertEquals( (int) 6 , iValue  );
	}		
	
	public void testStringAdd() {
		Result result = eval("'a'+'b'");
		assertFalse( result.isVoid() );
		assertEquals( "ab" , (String) result.value() );
	}
	
	public void testAND() {
		testBooleanExpression("A AND B" , (a,b,c) -> a & b );
	}
	
	public void testOR() {
		testBooleanExpression("A OR B" , (a,b,c) -> a | b );
	}	
	
	public void testNOT() {
		testBooleanExpression("NOT A" , (a,b,c) -> ! a );
	}	
	
	public void testBooleanExpressionPrecedence1() {
		testBooleanExpression("A AND B OR C" , (a,b,c) -> a & b | c );
	}
	
	public void testBooleanExpressionPrecedence2() {
		testBooleanExpression("C OR A AND B" , (a,b,c) -> c | a & b );
	}	
	
	public void testMethodInvocation1() {
		
		final BiFunction<Integer,Integer,Integer> func = (Integer a,Integer b) -> a+b;
		evaluator.setTarget( func );
		Result result = eval("apply(1,2)");
		assertFalse(result.isVoid());
		final int value = ((Number) result.value()).intValue();
		assertEquals( (int) 3 , value );
	}
	
	public void testMethodInvocation2() {
		
		evaluator.setTarget( new TestTarget1() );
		Result result = eval("apply(1,2)");
		assertFalse(result.isVoid());
		final int value = ((Number) result.value()).intValue();
		assertEquals( (int) 3 , value );
	}
	
	public void testMethodInvocation3() {
		
		evaluator.setTarget( new TestTarget1() );
		Result result = eval("apply('1','2')");
		assertEquals( "12" , (String) result.value());
	}	
	
	public void testMethodInvocation4() 
	{
		evaluator.setTarget( new TestTarget1() );
		Result result = eval("apply(1+3,2+3)");
		assertFalse(result.isVoid());
		final int value = ((Number) result.value()).intValue();
		assertEquals( (int) 9 , value );
	}	
	
	public void testMethodInlining() {
		
		evaluator.setTarget( new TestTarget1() );
		Result result = eval("apply(1,2)");
		assertFalse(result.isVoid());
		final int value = ((Number) result.value()).intValue();
		assertEquals( (int) 3 , value );
	}
	
	public void testInvokeVoidMethod() {
		
		final TestTarget1 target = new TestTarget1();
		evaluator.setTarget( target );
		Result result = eval("voidMethod()");
		if ( ! result.isVoid() ) {
			fail("Expected a void result but got "+result.value());
		}
		assertEquals( 1 , target.voidMethodInvocationCount );
	}		
	
	public void testPureMethodInlining() {
		
		final TestTarget1 target = new TestTarget1();
		evaluator.setTarget( target );
		
		final ASTNode ast = parse( "inliningTest(3,7)" );
		
		Result result = eval( ast );
		result = eval( ast );
		result = eval( ast );
		
		assertEquals( 1 , target.inliningTestMethodInvocationCount );
		
		assertFalse(result.isVoid());
		final int value = ((Number) result.value()).intValue();
		assertEquals( (int) 21 , value );
	}			
	
	public void testImpureMethodInlining() {
		
		final TestTarget1 target = new TestTarget1();
		evaluator.setTarget( target );
		
		final ASTNode ast = parse( "noInliningTest(3,7)" );
		
		Result result = eval( ast );
		result = eval( ast );
		result = eval( ast );
		
		assertEquals( 3 , target.noInliningTestMethodInvocationCount );
		assertTrue(result.isVoid());
	}	
	
	private Result eval(String expression) {
		return eval( parse( expression ) );
	}
	
	private Result eval(ASTNode node) {
		return evaluator.evaluate( node );
	}	
	
	@FunctionalInterface
	protected interface TriFunction<A,B,C,D> 
	{
		public D apply(A a,B b,C c);
	}
	
	private void testBooleanExpression(String expression,TriFunction<Boolean,Boolean,Boolean,Boolean> func) 
	{
		final Boolean[] a = { null };
		final Boolean[] b = { null };
		final Boolean[] c = { null };		
		resolver = new IScope() {
			
			@Override
			public Object readVariable(Identifier name) {
				if ( "A".equals( name.getSymbol() ) ) {
					return a[0];
				}
				if ( "B".equals( name.getSymbol() ) ) {
					return b[0];
				}	
				if ( "C".equals( name.getSymbol() ) ) {
					return c[0];
				}			
				throw new RuntimeException("Unknown identifier "+name);				
			}
			
			@Override
			public void writeVariable(Identifier name, Object value) {
				throw new UnsupportedOperationException("Assignment of "+value+" to "+name+" not implemented");
			}
			
			@Override
			public Class<?> getDataType(Identifier name) {
				throw new RuntimeException("getDataType("+name+") not implemented");
			}			
		};
		
		final ASTNode astNode = parse( expression );
		
		for ( int i = 0 ; i <= 1 ; i++ ) 
		{
			a[0] = i == 0 ? false : true;
			for ( int j = 0 ; j <= 1 ; j++ ) 
			{
				b[0] = j == 0 ? false : true;
				for ( int k = 0 ; k <= 1 ; k++ ) 
				{
					c[0] = k == 0 ? false : true;
					final Boolean expected = func.apply(a[0],b[0],c[0]);
					Result result = evaluator.evaluate( astNode );
					assertFalse(result.isVoid());
					assertEquals( expected , (Boolean) result.value() );
				}
			}			
		}
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
		
		final AST result = builder.getResult();
		
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
				return EvaluatorTest.this.resolver.readVariable(name);
			}
			
			@Override
			public void writeVariable(Identifier name, Object value) {
				throw new UnsupportedOperationException("Assignment of "+value+" to "+name+" not implemented");
			}
			
			@Override
			public Class<?> getDataType(Identifier name) {
				throw new RuntimeException("getDataType("+name+") not implemented");
			}			
		} );
		simplifier.setResolveVariables(resolveVariables);
		
		return (AST) simplifier.simplify( result ,  evaluator.getTarget() );
	}
}