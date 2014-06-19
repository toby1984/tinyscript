package de.codesourcery.tinyscript;

import java.text.ParseException;

import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.visitors.ASTBuilder;

public class ParserTest {

	public static void main(String[] args) throws ParseException {
		
		System.out.println("Starting");
		
		final String expression = "1+2+x"; 
		Lexer lexer = new Lexer(new Scanner(expression) );
		
		final ExpressionParser<ASTNode> parser = new ExpressionParser<ASTNode>();
		final ASTBuilder ctx = new ASTBuilder();
		parser.parse( lexer ,  ctx );
		
		ctx.getResult().visitSimple( (node) -> System.out.println(node) );
	}	
}
