package de.codesourcery.tinyscript.eval;

import java.io.File;
import java.io.FileOutputStream;
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

public class ByteCodeCompilerTest extends TestCase {

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
	
	public void test() throws Exception {
		
		ByteCodeCompiler comp = new ByteCodeCompiler("TestClass");
		
		final AST ast = parse("1+2");
		
		ast.prettyPrint();
		
		byte[] data = comp.compile( ast );
		
		final FileOutputStream out = new FileOutputStream( new File("/tmp/TestClass.class" ) );
		out.write( data );
		out.close();
		System.out.println( data.length+" bytes written.");
		
		final CompiledExpression instance = compile( data );
		System.out.println("RESULT = "+instance.apply());
	}
	
	private CompiledExpression compile(byte[] bytecode) throws Exception 
	{
		final Class<?> cl = defineClass(bytecode);
		return (CompiledExpression) cl.getConstructor( Object.class , IScope.class ).newInstance( null , null );
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
	
	private AST transform(AST ast) {
		return new SSARewriter().rewriteAST( ast );
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
		
		AST ast = (AST) simplifier.simplify( result ,  target );
		ast = transform(ast);
		new Typer(target,scope).type( ast );
		return ast;
	}	
}