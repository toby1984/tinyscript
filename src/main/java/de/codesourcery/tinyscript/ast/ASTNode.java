package de.codesourcery.tinyscript.ast;

import java.util.ArrayList;
import java.util.List;

public abstract class ASTNode 
{
	public static enum NodeType 
	{
		AST,
		OPERATOR,
		FUNCTION_CALL,
		BOOLEAN,
		NUMBER,		
		STRING,
		VARIABLE,
		EXPRESSION
	}
	
	private final List<ASTNode> children = new ArrayList<>();
	
	public ASTNode parent;
	
	private final NodeType type;
	private Class<?> dataType;
	
	public ASTNode(NodeType type) {
		this.type = type;
	}
	
	public void setDataType(Class<?> dataType) {
		this.dataType = dataType;
	}
	
	public Class<?> getDataType() {
		return dataType;
	}
	
	public final ASTNode copyNode() {
		ASTNode result = copyNodeHook();
		result.dataType = this.dataType;
		return result;
	}
	
	public abstract ASTNode copyNodeHook();
	
	public final ASTNode copySubtree() 
	{
		final ASTNode result = copyNode();
		for ( ASTNode child : children ) {
			result.add( child.copyNode() );
		}
		return result;
	}
	
	public final boolean hasParent() {
		return parent != null;
	}
	
	public final ASTNode replaceWith(ASTNode other) {
		if ( ! hasParent() ) {
			throw new IllegalStateException("Cannot execute replaceWith() on root node");
		}
		final int idx = parent.indexOf( this );
		return parent.replaceChild( idx , other );
	}
	
	public final ASTNode replaceChild(int index,ASTNode newChild) {
		children.set(index, newChild );
		newChild.parent = this;
		return newChild;
	}
	
	public final int indexOf(ASTNode child) 
	{
		final int len = children.size();
		for ( int i =0 ; i < len ; i++ ) {
			if ( child.equals( children.get(i) ) ) {
				return i;
			}
		}
		throw new RuntimeException(child+" is no child of "+this);
	}
	
	public final ASTNode getParent() {
		return parent;
	}
	
	public final NodeType getNodeType() {
		return type;
	}
	
	public final List<ASTNode> children() {
		return children;
	}
	
	public final boolean hasChildren() {
		return ! children.isEmpty();
	}
	
	public final boolean hasNoChildren() {
		return children.isEmpty();
	}
	
	public final ASTNode child(int index) {
		return children.get(index);
	}
	
	public final void setChildren(List<ASTNode> children) {
		this.children.clear();
		this.children.addAll(children);
		for ( ASTNode child : children ) {
			child.parent = this;
		}
	}
	
	public final int getChildCount() {
		return children.size();
	}
	
	public final ASTNode add(ASTNode child) {
		children.add( child );
		child.parent=this;
		return child;
	}
	
	public abstract boolean isLiteralValue();
	
	public void addAll(List<ASTNode> children) {
		this.children.addAll( children );
		for ( ASTNode child : children ) {
			child.parent = this;
		}
	}	
	
	public final ASTNode insertChild(int index,ASTNode child) {
		children.add(index ,  child );
		child.parent = this;
		return child;
	}
	
	public final void prettyPrint()
	{
		prettyPrint("    " , false );
	}
	
	private final void prettyPrint(String indent, boolean last)
	{
		System.out.print(indent);
		if (last)
		{
			System.out.print("\\-");
			indent += "  ";
		}
		else
		{
			System.out.print("|-");
			indent += "| ";
		}
		
		if ( getDataType() != null ) {
			System.out.print( toString() );
			System.out.println("{"+getDataType().getSimpleName()+"}");
		} else {
			System.out.println( toString() );
		}
		

		for (int i = 0; i < children.size() ; i++)
			children.get(i).prettyPrint(indent, i == children.size() - 1);
	}	
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}