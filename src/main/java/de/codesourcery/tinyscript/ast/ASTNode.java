package de.codesourcery.tinyscript.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ASTNode {

	private final List<ASTNode> children = new ArrayList<>();
	
	public List<ASTNode> children() {
		return children;
	}
	
	public ASTNode child(int index) {
		return children.get(index);
	}
	
	public ASTNode add(ASTNode child) {
		children.add( child );
		return child;
	}
	
	public void addAll(List<ASTNode> children) {
		this.children.addAll( children );
	}	
	
	public boolean visit(Function<ASTNode, Boolean> func) {
		
		if ( ! func.apply( this ) ) {
			return false; 
		}
		for ( ASTNode child : children ) {
			if ( ! child.visit( func ) ) {
				return false;
			}
		}
		return true;
	}
	
	public void visitSimple(Consumer<ASTNode> func) 
	{
		final Function<ASTNode, Boolean> wrapper = (node) -> {
			func.accept( node );
			return true;
		};
		visit( wrapper);
	}	
}
