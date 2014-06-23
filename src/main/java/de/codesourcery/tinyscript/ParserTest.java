package de.codesourcery.tinyscript;

import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.visitors.ShuntingYardVisitor;

public class ParserTest {

	public static void main(String[] args) throws Exception {
		new ParserTest().run();
	}
	
	public void run() throws Exception 
	{
		final String expression = "1 == 2";
		
		printMsg("Expression: "+expression);
		
		run3(expression);
	}	
	
	private static void printMsg(String msg) {
		System. out.println( "#############################################\n"+
                "# "+msg+"\n"+
	             "#############################################");		
	}
	
	public void run3(String expression) throws Exception 
	{
		Lexer lexer = new Lexer(new Scanner(expression) );
		
		final ExpressionParser<ASTNode> parser = new ExpressionParser<ASTNode>();
		final ShuntingYardVisitor ctx = new ShuntingYardVisitor();
		
		parser.parse( lexer ,  ctx );
		
		final ASTNode result = ctx.getResult();
		
		result.printPretty(" " , false );
	}	
	
	public int test(Integer a,Integer b) {
		return a+b;
	}
}