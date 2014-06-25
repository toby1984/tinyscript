package de.codesourcery.tinyscript.eval;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.sun.xml.internal.ws.org.objectweb.asm.Type;

import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.ast.FunctionCallNode;
import de.codesourcery.tinyscript.ast.ILiteralNode;
import de.codesourcery.tinyscript.ast.OperatorNode;

public class StackBuilder 
{
	protected final Map<Identifier,Class<?>> varTypes = new HashMap<>();
	protected final Map<Identifier,Integer> varIndices = new HashMap<>();

	protected final Stack<Object> valueStack = new Stack<>();
	protected final Stack<AbstractOperation> operationStack = new Stack<>();

	protected int currentLabelIndex=1;
	protected int currentVarIndex=1;

	public int maxStackSize = 0;

	protected final Class<?> targetClass;

	public StackBuilder(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	protected static final class InstructionResultType 
	{
		public final Class<?> type;

		public InstructionResultType(Class<?> type) {
			if (type==null) throw new IllegalArgumentException("type must not be NULL");
			this.type = type;
		}
		
		@Override
		public String toString() {
			return "InstructionResult("+type.getName()+")";
		}
	}

	protected abstract class AbstractOperation {

		private final String name;
		
		protected AbstractOperation(String name) {
			this.name = name;
		}
		
		@Override
		public java.lang.String toString() {
			return name;
		}
		
		protected void createArray(List<Object> values,Class<?> componentType, MethodVisitor methodVisitor) {
			/*
			 *  0: iconst_3      
			 *  1: anewarray     #20                 // class java/lang/Object
			 *  4: dup           
			 *  5: iconst_0      
			 *  6: iconst_1      
			 *  7: invokestatic  #22                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
			 * 10: aastore       
			 * 11: dup           
			 * 12: iconst_1      
			 * 13: iconst_2      
			 * 14: invokestatic  #22                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
			 * 17: aastore       
			 * 18: dup           
			 * 19: iconst_2      
			 * 20: iconst_3      
			 * 21: invokestatic  #22                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
			 * 24: aastore       
			 * 25: astore_1
			 */
			outputValue( values.size() , Integer.class , methodVisitor );
			methodVisitor.visitTypeInsn( Opcodes.ANEWARRAY, componentType.getName().replace(".","/" ) );

			if ( values.size() == 0 ) {
				return;
			}

			int index = 0;
			for ( Object value : values ) 
			{
				methodVisitor.visitInsn(Opcodes.DUP); // array ref
				outputValue( index++ , Integer.class , methodVisitor ); // index
				outputValue( value , value.getClass(), true , methodVisitor ); // value
				methodVisitor.visitInsn( Opcodes.AASTORE );
			}
		}

		public abstract Class<?> output(MethodVisitor methodVisitor,StackBuilder builder);

		protected <T> T assertType(Object value,Class<T> clazz) {
			if ( value.getClass() != clazz ) {
				throw new RuntimeException("Operation "+this+" expected a "+clazz.getName()+" on stack but got "+value);
			}
			return (T) value;
		}

		protected Class<?> outputConditional(StackBuilder builder,int branchOnOperandsOpcode, int branchOnResultOpcode, MethodVisitor methodVisitor) 
		{
			final Object rhs = builder.popValue();
			final Object lhs = builder.popValue();

			final Class<?> targetType = getWidestType(lhs,rhs);

			StackBuilder.outputValue( lhs , targetType , methodVisitor );
			StackBuilder.outputValue( rhs , targetType , methodVisitor );

			if ( targetType == Long.class ) 
			{
				methodVisitor.visitInsn(Opcodes.LCMP);
				outputConditional(branchOnResultOpcode,methodVisitor);
			} else if ( targetType == Integer.class || targetType == Short.class || targetType == Byte.class) {
				outputConditional(branchOnOperandsOpcode,methodVisitor);	
			} else if ( targetType == Double.class ) 
			{
				// TODO: DCMPG or DCMPL only differ in their treatment of NaN
				methodVisitor.visitInsn( Opcodes.DCMPG );	
				outputConditional(branchOnResultOpcode,methodVisitor);					
			}  else if ( targetType == Float.class ) {
				// TODO: FCMPG or FCMPL only differ in their treatment of NaN
				methodVisitor.visitInsn( Opcodes.FCMPG );	
				outputConditional(branchOnResultOpcode,methodVisitor);							
			} else {
				throw new RuntimeException("I have no xADD instruction for type "+targetType);
			}		
			builder.pushInstructionResultType(Integer.class);
			return Integer.class;
		}

		protected void loadReferenceToScope(MethodVisitor methodVisitor) {
			loadField(CompiledExpression.class,"scope",IScope.class,methodVisitor);
		}	

		protected void loadField(Class<?> owningClass,String fieldName,Class<?> fieldType,MethodVisitor methodVisitor) {
			methodVisitor.visitFieldInsn(Opcodes.GETFIELD, owningClass.getName().replace(".","/") , fieldName , Type.getDescriptor(fieldType) );
		}

		protected void invokeVirtualMethod(Class<?> owningClass,String methodName,String signature, MethodVisitor methodVisitor) {
			methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owningClass.getName().replace(".","/"), methodName, signature);
		}

		protected void outputConditional(int conditionalOpcode,MethodVisitor methodVisitor) 
		{
			/**
			 *     CONDITIONAL
			 *     LDC 0
			 *     JMP continueLabel:
			 *  trueLabel: // TRUE case
			 *     LDC 1
			 *  continueLabel:
			 */			
			final Label trueLabel = new Label();
			final Label continueLabel = new Label();

			methodVisitor.visitJumpInsn(conditionalOpcode, trueLabel);
			outputValue(Integer.valueOf(0) , Integer.class , methodVisitor );
			methodVisitor.visitJumpInsn( Opcodes.GOTO , continueLabel);

			methodVisitor.visitLabel(trueLabel);
			outputValue(Integer.valueOf(1) , Integer.class , methodVisitor );

			methodVisitor.visitLabel( continueLabel );			
		}

		protected Class<?> getWidestType(Object a,Object b) 
		{
			return NumericType.getWiderType(a, b).getJavaType();
		}

		protected void outputBoolean(Object value,MethodVisitor methodVisitor) 
		{
			assertType( value , Boolean.class);			
			if ( Boolean.TRUE.equals(value) ) {
				methodVisitor.visitInsn(Opcodes.ICONST_1);
			} else {
				methodVisitor.visitInsn(Opcodes.ICONST_0);
			}
		}		
	}

	protected final AbstractOperation AND = new AbstractOperation("&") 
	{
		@Override
		public Class<?> output(MethodVisitor methodVisitor,StackBuilder builder) 
		{
			final Object rhs = builder.popValue();
			final Object lhs = builder.popValue();
			outputBoolean( lhs , methodVisitor );
			outputBoolean( rhs , methodVisitor );
			methodVisitor.visitInsn( Opcodes.IAND );
			builder.pushInstructionResultType(Boolean.class);
			return Boolean.class;
		}
	};

	protected final AbstractOperation ASSIGNMENT = new AbstractOperation("=") {
		@Override
		public Class<?> output(MethodVisitor methodVisitor,StackBuilder builder) 
		{
			throw new RuntimeException("assignments not implemented yet");
		}
	};

	protected final AbstractOperation DIVIDE = new AbstractOperation("/") {
		@Override
		public Class<?> output(MethodVisitor methodVisitor,StackBuilder builder) 
		{
			final Object rhs = builder.popValue();
			final Object lhs = builder.popValue();
			final Class<?> targetType = getWidestType(lhs,rhs);
			StackBuilder.outputValue( lhs , targetType , methodVisitor );
			StackBuilder.outputValue( rhs , targetType , methodVisitor );
			if ( targetType == Long.class ) {
				methodVisitor.visitInsn( Opcodes.LDIV);	
			} else if ( targetType == Integer.class || targetType == Short.class || targetType == Byte.class) {
				methodVisitor.visitInsn( Opcodes.IDIV );	
			} else if ( targetType == Double.class ) {
				methodVisitor.visitInsn( Opcodes.DDIV );	
			}  else if ( targetType == Float.class ) {
				methodVisitor.visitInsn( Opcodes.FDIV );	
			} else {
				throw new RuntimeException("I have no xADD instruction for type "+targetType);
			}
			final Class<?> resultType = toPrimitiveType( targetType );
			builder.pushInstructionResultType(resultType);
			return resultType;
		}				
	};

	protected  final AbstractOperation EQ = new AbstractOperation("==") {
		@Override
		public Class<?> output(MethodVisitor methodVisitor,StackBuilder builder) 
		{
			return outputConditional( builder , Opcodes.IF_ICMPEQ , Opcodes.IFEQ , methodVisitor );				
		}			
	};

	protected  final AbstractOperation GT = new AbstractOperation(">") {
		@Override
		public Class<?> output(MethodVisitor methodVisitor,StackBuilder builder) 
		{
			return outputConditional( builder , Opcodes.IF_ICMPGT , Opcodes.IFGT , methodVisitor );				
		}			
	};

	protected  final AbstractOperation GTE  = new AbstractOperation(">=") {
		@Override
		public Class<?> output(MethodVisitor methodVisitor,StackBuilder builder) 
		{
			return outputConditional( builder , Opcodes.IF_ICMPGE , Opcodes.IFGE , methodVisitor );				
		}			
	};

	protected  final AbstractOperation LT  = new AbstractOperation("<") {
		@Override
		public Class<?> output(MethodVisitor methodVisitor,StackBuilder builder) 
		{
			return outputConditional( builder , Opcodes.IF_ICMPLT , Opcodes.IFLT , methodVisitor );				
		}			
	};

	protected  final AbstractOperation LTE = new AbstractOperation("<=") {
		@Override
		public Class<?> output(MethodVisitor methodVisitor,StackBuilder builder) 
		{
			return outputConditional( builder, Opcodes.IF_ICMPLE , Opcodes.IFLE , methodVisitor );				
		}			
	};

	protected  final AbstractOperation MINUS = new AbstractOperation("-") {
		@Override
		public Class<?> output(MethodVisitor methodVisitor,StackBuilder builder) 
		{
			final Object rhs = builder.popValue();
			final Object lhs = builder.popValue();
			final Class<?> targetType = getWidestType(lhs,rhs);
			StackBuilder.outputValue( lhs , targetType , methodVisitor );
			StackBuilder.outputValue( rhs , targetType , methodVisitor );
			if ( targetType == Long.class ) {
				methodVisitor.visitInsn( Opcodes.LSUB);	
			} else if ( targetType == Integer.class || targetType == Short.class || targetType == Byte.class) {
				methodVisitor.visitInsn( Opcodes.ISUB );	
			} else if ( targetType == Double.class ) {
				methodVisitor.visitInsn( Opcodes.DSUB );	
			}  else if ( targetType == Float.class ) {
				methodVisitor.visitInsn( Opcodes.FSUB );	
			} else {
				throw new RuntimeException("I have no xADD instruction for type "+targetType);
			}
			final Class<?> resultType = toPrimitiveType( targetType );
			builder.pushInstructionResultType(resultType);
			return resultType;
		}				
	};

	protected  final AbstractOperation NEQ = new AbstractOperation("!=") {
		@Override
		public Class<?> output(MethodVisitor methodVisitor,StackBuilder builder) 
		{
			return outputConditional( builder, Opcodes.IF_ICMPNE , Opcodes.IFNE , methodVisitor );
		}			
	};
	protected  final AbstractOperation NOT = new AbstractOperation("!") {
		@Override
		public Class<?> output(MethodVisitor methodVisitor,StackBuilder builder) 
		{
			outputBoolean( builder.popValue() , methodVisitor );
			methodVisitor.visitInsn( Opcodes.INEG );
			builder.pushInstructionResultType(Boolean.class);
			return Boolean.class;
		}			
	};

	protected  final AbstractOperation OR = new AbstractOperation("|") {
		@Override
		public Class<?> output(MethodVisitor methodVisitor,StackBuilder builder) 
		{
			final Object rhs = builder.popValue();
			final Object lhs = builder.popValue();
			outputBoolean( lhs , methodVisitor );
			outputBoolean( rhs , methodVisitor );
			methodVisitor.visitInsn( Opcodes.IAND );
			builder.pushInstructionResultType(Boolean.class);
			return Boolean.class;
		}			
	};
	protected  final AbstractOperation PLUS = new AbstractOperation("+") {
		@Override
		public Class<?> output(MethodVisitor methodVisitor,StackBuilder builder) 
		{
			final Object rhs = builder.popValue();
			final Object lhs = builder.popValue();
			final Class<?> targetType = getWidestType(lhs,rhs);
			StackBuilder.outputValue( lhs , targetType , methodVisitor );
			StackBuilder.outputValue( rhs , targetType , methodVisitor );
			if ( targetType == Long.class ) {
				methodVisitor.visitInsn( Opcodes.LADD );	
			} else if ( targetType == Integer.class || targetType == Short.class || targetType == Byte.class) {
				methodVisitor.visitInsn( Opcodes.IADD );	
			} else if ( targetType == Double.class ) {
				methodVisitor.visitInsn( Opcodes.DADD );	
			}  else if ( targetType == Float.class ) {
				methodVisitor.visitInsn( Opcodes.FADD );	
			} else {
				throw new RuntimeException("I have no xADD instruction for type "+targetType);
			}
			final Class<?> resultType = toPrimitiveType( targetType );
			builder.pushInstructionResultType(resultType);
			return resultType;
		}			
	};

	protected  final AbstractOperation TIMES = new AbstractOperation("*") {
		@Override
		public Class<?> output(MethodVisitor methodVisitor,StackBuilder builder) 
		{
			final Object rhs = builder.popValue();
			final Object lhs = builder.popValue();
			final Class<?> targetType = getWidestType(lhs,rhs);
			StackBuilder.outputValue( lhs , targetType , methodVisitor );
			StackBuilder.outputValue( rhs , targetType , methodVisitor );
			if ( targetType == Long.class ) {
				methodVisitor.visitInsn( Opcodes.LMUL );	
			} else if ( targetType == Integer.class || targetType == Short.class || targetType == Byte.class) {
				methodVisitor.visitInsn( Opcodes.IMUL  );	
			} else if ( targetType == Double.class ) {
				methodVisitor.visitInsn( Opcodes.DMUL );	
			}  else if ( targetType == Float.class ) {
				methodVisitor.visitInsn( Opcodes.FMUL );	
			} else {
				throw new RuntimeException("I have no xADD instruction for type "+targetType);
			}
			final Class<?> resultType = toPrimitiveType( targetType );
			builder.pushInstructionResultType(resultType);
			return resultType;
		}				
	};

	protected final AbstractOperation PREPARE_METHOD_INVOCATION = new AbstractOperation("PREPARE_METHOD_INVOCATION") {

		@Override
		public Class<?> output(MethodVisitor methodVisitor, StackBuilder builder) 
		{
			loadReferenceToTargetObject(methodVisitor);			
			return null;
		}
	};	
	
	protected  final class InvokeMethodOnTarget extends AbstractOperation 
	{
		public final Identifier functionName;
		public final int argCount;
		public final Class<?>[] argumentTypes;

		public InvokeMethodOnTarget(Identifier functionName, final Class<?>[] argumentTypes) {
			super("InvokeMethodOnTarget( "+functionName.getSymbol()+")");
			this.functionName = functionName;
			this.argCount = argumentTypes.length;
			this.argumentTypes = argumentTypes;
		}

		@Override
		public String toString() {
			return functionName.getSymbol()+"("+Arrays.toString(argumentTypes)+")";
		}

		@Override
		public Class<?> output(MethodVisitor methodVisitor,StackBuilder builder) 
		{
			final List<Object> arguments = new ArrayList<>();
			for ( int i = 0 ; i < argCount ; i++ ) 
			{
				arguments.add( builder.popValue() );
			}

			Collections.reverse(arguments);

			final Method method = Evaluator.findMethod( functionName , argumentTypes , builder.targetClass.getMethods() );				
			for ( Object value : arguments ) 
			{
				outputValue( value ,  value.getClass() ,  methodVisitor );
			}
			methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, builder.targetClass.getName().replace(".","/"), functionName.getSymbol() , Type.getMethodDescriptor( method ) );
			System.out.println("Method return type: "+method.getReturnType());
			builder.pushInstructionResultType( method.getReturnType() );
			return method.getReturnType();				
		}			
	};

	protected static Class<?> outputValue(Object value,Class<?> targetType,MethodVisitor visitor) 
	{
		return outputValue(value,targetType,false,visitor);
	}

	protected static Class<?> outputValue(Object value,Class<?> targetType,boolean convertToObject,MethodVisitor visitor) 
	{
		if ( StackBuilder.isInstructionResult( value ) ) {
			return targetType;
		}
		if ( targetType == String.class ) 
		{
			if ( value.getClass() != String.class ) {
				throw new IllegalArgumentException("Don't know how to convert "+value+" to "+targetType.getName());
			}
			visitor.visitLdcInsn( value );
			return String.class;
		} else if ( targetType == Short.class ) {
			final int v = ((Number) value).shortValue();
			visitor.visitIntInsn( Opcodes.SIPUSH , v );
		} else if ( targetType == Byte.class ) {
			final int v = ((Number) value).byteValue();
			visitor.visitIntInsn( Opcodes.BIPUSH , v );		
		} else if ( targetType == Integer.class ) {
			final int v = ((Number) value).intValue();
			if ( v == -1 ) {
				visitor.visitInsn(Opcodes.ICONST_M1);
			} else if ( v >= 0 && v <= 5 ) {
				visitor.visitInsn(Opcodes.ICONST_0+v);
			} else {
				visitor.visitLdcInsn( v );
			}
		} else if ( targetType == Long.class ) {
			final long v = ((Number) value).longValue();
			visitor.visitLdcInsn( v );		
		} else if ( targetType == Float.class ) {
			final float v = ((Number) value).floatValue();				
			visitor.visitLdcInsn( v );				
		} else if ( targetType == Double.class ) {
			final double v = ((Number) value).doubleValue();				
			visitor.visitLdcInsn( v );
		} else {
			throw new RuntimeException("Unhandled target type: "+targetType);
		}
		if ( convertToObject ) {
			ByteCodeCompiler.outputConversionToObject( targetType , visitor );
			return toObjectType( targetType );
		}		
		return toPrimitiveType( targetType );
	}	

	protected Object popValue() {
		return valueStack.pop(); 
	}

	protected int variableIndex(Identifier name,boolean createIfMissing) {
		Integer result = varIndices.get(name);
		if ( result == null ) 
		{
			if ( ! createIfMissing ) {
				throw new RuntimeException("Internal error, no index for variable "+name);
			}
			result = currentVarIndex;
			varIndices.put(name, result);
			currentVarIndex++;
		}
		return result;
	}

	public void buildStack(ASTNode node) {

		switch( node.getNodeType() ) 
		{
		case AST:
			node.children().stream().forEach( n -> buildStack(n) );
			return;
		case BOOLEAN:
		case NUMBER:
		case STRING:
			pushValue( ((ILiteralNode) node).value() );
			return;
		case EXPRESSION:
			node.children().stream().forEach( n -> buildStack(n) );				
			return;
		case FUNCTION_CALL:
			final FunctionCallNode fn= (FunctionCallNode) node;
			final int argCount = node.getChildCount();

			final Class<?>[] types = new Class<?>[ argCount ];
			for ( int i = 0 ; i < types.length ; i++ ) {
				types[i] = node.child(i).getDataType();
				if ( types[i] == null ) {
					throw new IllegalStateException("AST lacks type information on function argument "+node.child(i));
				}
			}

			operationStack.push( new InvokeMethodOnTarget( fn.functionName ,types) );	
			
			for ( int i = node.getChildCount()-1 ; i >= 0 ; i--) {
				buildStack( node.child(i) );
			}
			
			operationStack.push( PREPARE_METHOD_INVOCATION );	

			printStacks();
			return;
		case OPERATOR:
			pushOperator((OperatorNode) node);
			return;
		}
		throw new RuntimeException("Internal error,unhandled AST node "+node);
	}

	private void pushValue(Object object) 
	{
		System.out.println("PUSH VALUE: "+object);
		valueStack.push(object);
		if ( valueStack.size() > maxStackSize ) {
			maxStackSize = valueStack.size();
		}
	}

	private void pushOperator(OperatorNode node) 
	{
		System.out.println("PUSH OPERATOR: "+node);		
		final AbstractOperation abstractOP;
		switch( node.type ) 
		{
		case AND:
			abstractOP = AND;
			break;
		case ASSIGNMENT:
			abstractOP = ASSIGNMENT;
			break;
		case DIVIDE:
			abstractOP = DIVIDE;
			break;
		case EQ:
			abstractOP = EQ;
			break;
		case GT:
			abstractOP = GT;
			break;
		case GTE:
			abstractOP = GTE;
			break;
		case LT:
			abstractOP = LT;
			break;
		case LTE:
			abstractOP = LTE;
			break;
		case MINUS:
			abstractOP = MINUS;
			break;
		case NEQ:
			abstractOP = NEQ;
			break;
		case NOT:
			abstractOP = NOT;
			break;
		case OR:
			abstractOP = OR;
			break;
		case PLUS:
			abstractOP = PLUS;
			break;
		case TIMES:
			abstractOP = TIMES;
			break;
		default:
			throw new RuntimeException("Don't know how to map operation "+node+" to abstract operation");
		}
		operationStack.push( abstractOP );
		printStacks();

		node.children().stream().forEach( child -> buildStack( child ) );
	}

	private void printStacks() 
	{
		System.out.println("\n=== OPERATIONS ===\n");

		int i = 0;
		for (Iterator<AbstractOperation> it = operationStack.iterator(); it.hasNext();i++) 
		{
			Object obj = it.next();
			System.out.println( i+" : "+obj);
		}

		System.out.println("\n=== VALUES ===\n");

		i = 0;
		for (Iterator<Object> it = valueStack.iterator(); it.hasNext();i++) 
		{
			Object obj = it.next();
			System.out.println( i+" : "+obj);
		}		
	}

//	private Class<?> outputValue(Object value,MethodVisitor visitor) {
//		visitor.visitLdcInsn( value );
//		return value.getClass();
//	}

	protected void pushInstructionResultType(Class<?> type) {
		valueStack.push( new InstructionResultType(type) );
	}

	public Class<?> output(MethodVisitor visitor) 
	{
		System.out.println("=========== OUTPUT =========");
		System.out.println("=========== OUTPUT =========");
		System.out.println("=========== OUTPUT =========");
		
		Class<?> lastTypeOnStack = null;
		while ( ! operationStack.isEmpty() ) 
		{
			final AbstractOperation operation = operationStack.pop();
			System.out.println("BEFORE Executing: "+operation);
			
			printStacks();			
			lastTypeOnStack = operation.output( visitor , this );
			
			System.out.println("AFTER Executing: "+operation);
			printStacks();
		}
		for (Iterator<Object> it = valueStack.iterator(); it.hasNext();) {
			Object obj = it.next();
			if ( isInstructionResult( obj ) ) 
			{
				lastTypeOnStack = instructionResult(obj).type;
				it.remove();
			}
		}
		if ( valueStack.size() > 1 ) {
			throw new IllegalStateException("Still "+valueStack.size()+" operations on value stack after output of all instructions");
		}		
		if ( valueStack.size() == 1 ) 
		{
			Object value = valueStack.pop();
			lastTypeOnStack = outputValue( value , value.getClass() , visitor );
		} 
		return lastTypeOnStack;
	}

	protected static InstructionResultType instructionResult(Object o) {
		return ((InstructionResultType) o);
	}

	protected static boolean isInstructionResult(Object o) {
		return o instanceof InstructionResultType;
	}

	protected void loadReferenceToTargetObject(MethodVisitor methodVisitor) 
	{
		// arg 0 : load object reference to invoke method on
		methodVisitor.visitVarInsn(Opcodes.ALOAD, 0 ); // 'this' pointer
		methodVisitor.visitFieldInsn(Opcodes.GETFIELD,
				CompiledExpression.class.getName().replace(".","/" ), "target" , 
				"Ljava/lang/Object;"  
				);

		methodVisitor.visitTypeInsn( Opcodes.CHECKCAST , targetClass.getName().replace(".","/" ) );			
	}
	
	public static Class<?> toPrimitiveType(Class<?> cl) {
		if ( cl.isPrimitive() ) {
			return cl;
		}
		if ( cl == Boolean.class ) {
			return Boolean.TYPE;
		} else if ( cl == Long.class ) {
			return Long.TYPE;
		} else if ( cl == Integer.class ) {
			return Integer.TYPE;
		} else if ( cl == Short.class ) {
			return Short.TYPE;
		} else if ( cl == Byte.class ) {
			return Byte.TYPE;
		} else if ( cl == Float.class ) {
			return Float.TYPE;
		} else if ( cl == Double.class ) {
			return Double.TYPE;
		} else {
			throw new IllegalArgumentException("No primitive available for "+cl.getName());
		}
	}
	
	public static Class<?> toObjectType(Class<?> cl) 
	{
		if ( ! cl.isPrimitive() ) {
			return cl;
		}
		if ( cl == Boolean.TYPE ) {
			return Boolean.class;
		} else if ( cl == Long.TYPE ) {
			return Long.TYPE;
		} else if ( cl == Integer.TYPE ) {
			return Integer.class;
		} else if ( cl == Short.TYPE ) {
			return Short.class;
		} else if ( cl == Byte.TYPE ) {
			return Byte.class;
		} else if ( cl == Float.TYPE ) {
			return Float.class;
		} else if ( cl == Double.TYPE ) {
			return Double.class;
		} else {
			throw new IllegalArgumentException("No primitive available for "+cl.getName());
		}
	}	
}