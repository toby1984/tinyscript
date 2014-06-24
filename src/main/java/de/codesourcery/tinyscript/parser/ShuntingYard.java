package de.codesourcery.tinyscript.parser;

import java.util.Stack;

import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.ast.ExpressionNode;
import de.codesourcery.tinyscript.ast.OperatorNode;
import de.codesourcery.tinyscript.eval.OperatorType;
import de.codesourcery.tinyscript.parser.ExpressionToken.ExpressionTokenType;

/**
 * This class implements Dijkstra's 'Shunting yard' algorithm 
 * with additional support for variable-argument functions (shamelessly stolen from 
 * http://www.kallisti.net.nz/blog/2008/02/extension-to-the-shunting-yard-algorithm-to-allow-variable-numbers-of-arguments-to-functions/).
 * 
 * @author tobias.gierke@voipfuture.com
 */
public final class ShuntingYard 
{
    private final Stack<ExpressionToken> valueQueue = new Stack<>();
    private final Stack<ExpressionToken> stack = new Stack<ExpressionToken>();

    private final Stack<Integer> argsCountStack = new Stack<Integer>();
    private final Stack<Boolean> argsMarkerStack = new Stack<Boolean>();        

    public ShuntingYard() {
    }
    
    public boolean isFunctionOnStack() 
    {
        for ( ExpressionToken t : stack ) {
            if ( t.isFunction() ) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isEmpty() {
        return valueQueue.isEmpty() && stack.isEmpty();
    }

    public void pushValue(ASTNode node) 
    {
        output( new ExpressionToken(ExpressionTokenType.VALUE , node ) );
        // If the were values stack has a value on it, pop it and push true
        if ( ! argsMarkerStack.isEmpty() ) {
            argsMarkerStack.pop();
            argsMarkerStack.push( Boolean.TRUE );
        }
    }        

    /*
    [X] Read a token.
    [X] If the token is a number, then add it to the output queue.  
    [X] If the token is a function token, then push it onto the stack. 
    [X] If the token is a left parenthesis, then push it onto the stack.

    [X] If the token is a function argument separator (e.g., a comma):

        Until the token at the top of the stack is a left parenthesis, pop operators off the stack onto the output queue. If no left parentheses are encountered, either the separator was misplaced or parentheses were mismatched.

    [X] If the token is an operator, o1, then:

        while there is an operator token, o2, at the top of the stack, and

                either o1 is left-associative and its precedence is less than or equal to that of o2,
                or o1 has precedence less than that of o2,

            pop o2 off the stack, onto the output queue;

        push o1 onto the stack.


    [X] If the token is a right parenthesis:

        Until the token at the top of the stack is a left parenthesis, pop operators off the stack onto the output queue.
        Pop the left parenthesis from the stack, but not onto the output queue.
        If the token at the top of the stack is a function token, pop it onto the output queue.
        If the stack runs out without finding a left parenthesis, then there are mismatched parentheses.

When there are no more tokens to read:

    While there are still operator tokens in the stack:

        If the operator token on the top of the stack is a parenthesis, then there are mismatched parentheses.
        Pop the operator onto the output queue.

     */

    public void pushOperator(ExpressionToken tok1) 
    {
        if ( tok1.isFunction() )
        {
            // Push 0 onto the arg count stack. If the were values stack has a value on it, pop it and push true. Push false onto were values.
            argsCountStack.push(Integer.valueOf(0));
            if ( ! argsMarkerStack.isEmpty() ) 
            {
                argsMarkerStack.pop();
                argsMarkerStack.push( Boolean.TRUE );
            }
            argsMarkerStack.push(Boolean.FALSE);
            stack.push(tok1);
            return;
        }

        if ( tok1.isParensOpen() )
        {
            stack.push(tok1);
            return;
        }       

        if ( tok1.isArgumentDelimiter() )
        {
            if ( ! isFunctionOnStack() ) {
                throw new ParseException("Unexpected argument delimiter",tok1.getTextRegion());
            }
            popUntil(ExpressionTokenType.PARENS_OPEN );

            // Pop were values into w. If w is true, pop arg count into a, increment a and push back into arg count. Push false into were values.
            if ( argsMarkerStack.pop() ) {
                argsCountStack.push(argsCountStack.pop() +  1 );
            }
            argsMarkerStack.push(Boolean.FALSE);
            return;
        }

        if ( tok1.isParensClose() ) 
        {
            int valueCountDelta = valueQueue.size();
            if ( ! popUntil(ExpressionTokenType.PARENS_OPEN ) ) {
                throw new ParseException("Mismatched closing parens",tok1.getTextRegion());
            }
            valueCountDelta = valueQueue.size() - valueCountDelta;

            stack.pop(); // pop opening parens

            // If the token at the top of the stack is a function token, pop it onto the output queue.
            if ( ! stack.isEmpty() && stack.peek().hasType(ExpressionTokenType.FUNCTION) ) 
            {
                output(stack.pop());
            } 
            else {
                if ( valueCountDelta >= 0 ) 
                {
                    // not a function invocation, wrap in ExpressionNode
                	ExpressionNode expr = new ExpressionNode();
                    while( ! valueQueue.isEmpty() && valueCountDelta >= 0 ) 
                    {
                        ExpressionToken pop = valueQueue.pop();
                        expr.insertChild( 0 , pop.getToken() );
                        valueCountDelta--;
                    }
                    valueQueue.push( new ExpressionToken( ExpressionTokenType.EXPRESSION , expr ) );
                } 
            }
            return;
        }

        /* If the token is an operator, o1, then:
         * 
         *    while there is an operator token, o2, at the top of the stack, and
         * 
         *            either o1 is left-associative and its precedence is less than or equal to that of o2,
         *            or o1 has precedence less than that of o2,
         * 
         *        pop o2 off the stack, onto the output queue;
         * 
         *    push o1 onto the stack.                     
         */            
        if ( tok1.isOperator() )
        {
            final OperatorType o1 = ((OperatorNode) tok1.getToken()).getOperatorType();
            
        	// System.out.println("**** Pushing "+( o1.isLeftAssociative() ? "left" : "right")+"-associate operator "+o1.toPrettyString()+" with precedence "+o1.getPrecedence());

            while ( ! stack.isEmpty() && stack.peek().isOperator() ) 
            {
                final ExpressionToken tok2 = stack.peek();
                final OperatorType o2 = ((OperatorNode) tok2.getToken()).getOperatorType();
                if ( ( o1.isLeftAssociative() && o1.getPrecedence() <= o2.getPrecedence() ) ||
                        o1.getPrecedence() < o2.getPrecedence() ) 
                {
                    output( stack.pop() );
                } else {
                    break;
                }
            }

            stack.push( tok1 );
            return;
        }

        throw new RuntimeException("Unreachable code reached");
    }
    
    public boolean isStackContainsOpeningParens() {
        for ( ExpressionToken tok : stack ) {
            if ( tok.isParensOpen() ) {
                return true;
            }
        }
        return false;
    }

    private boolean popUntil(ExpressionTokenType type) 
    {
        boolean found = false;
        while( true ) {
            if ( stack.isEmpty() ) {
                break;
            }
            if ( stack.peek().hasType( type ) ) {
                found = true;
                break;
            }
            output( stack.pop() );
        }

        return found;
    }

    private void output(ExpressionToken tok) 
    {
        if ( tok.isParensOpen() ) {
            throw new ParseException("No matching closing parens",tok.getTextRegion());
            //                return;
        }

        if ( tok.isValue() ) 
        {
            valueQueue.add( tok );
            return;
        }

        if ( tok.isFunction() ) 
        {
            /* Pop stack into f
             * Pop arg count into a
             * Pop were values into w
             * If w is true, increment a
             * Set the argument count of f to a
             * Push f onto output queue
             */                
            int argCount = argsCountStack.pop();
            if ( argsMarkerStack.pop() ) {
                argCount+=1;
            }

            while ( argCount > 0 ) {
                tok.getToken().insertChild( 0 , valueQueue.pop().getToken() );
                argCount--;
            }

            valueQueue.push( tok );
            return;
        }

        if ( tok.isOperator() ) 
        {
            while( ! tok.hasAllOperands() ) 
            {
                if ( valueQueue.isEmpty() ) {
                    throw new ParseException("Operator "+tok.getToken()+" lacks operand",tok.getTextRegion());
                }
                tok.getToken().insertChild( 0 , valueQueue.pop().getToken() );
            }
            valueQueue.push( tok );
            return;
        }
        throw new RuntimeException("Unreachable code reached");
    }

    public ASTNode getResult(int currentParseOffset) 
    {
        while ( ! stack.isEmpty() ) {
            output( stack.pop() );
        }

        if ( valueQueue.isEmpty() ) {
            throw new ParseException("Empty expression?",currentParseOffset);
        } 

        if ( valueQueue.size() != 1 ) 
        {
            if ( valueQueue.size() > 1 ) 
            {
                boolean onlyValues = true;
                for ( ExpressionToken tok : valueQueue ) {
                    if ( ! tok.isValue() ) {
                        onlyValues = false;
                    }
                }
                if ( onlyValues ) {
                    throw new ParseException("Values without operator ?", valueQueue.peek().getTextRegion() );
                }
            }
            throw new RuntimeException("Internal error,output queue has size "+valueQueue.size()+" , expected size: 1");
        }

        final ASTNode result = valueQueue.pop().getToken();
        if ( result == null ) {
            throw new RuntimeException("Internal error");
        }
        return result;
    }
}