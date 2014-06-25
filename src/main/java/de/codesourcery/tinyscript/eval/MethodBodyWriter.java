package de.codesourcery.tinyscript.eval;

import java.lang.reflect.Method;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.sun.xml.internal.ws.org.objectweb.asm.Type;

import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.ast.FunctionCallNode;
import de.codesourcery.tinyscript.ast.ILiteralNode;
import de.codesourcery.tinyscript.ast.OperatorNode;

public class MethodBodyWriter 
{
	protected final Class<?> targetClass;

	public MethodBodyWriter(Class<?> targetClass) {
		this.targetClass = targetClass;
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
		
		protected void genArray(List<Object> values,Class<?> componentType, MethodVisitor methodVisitor) {
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
			genValue( values.size() , Integer.class , methodVisitor );
			methodVisitor.visitTypeInsn( Opcodes.ANEWARRAY, componentType.getName().replace(".","/" ) );

			if ( values.size() == 0 ) {
				return;
			}

			int index = 0;
			for ( Object value : values ) 
			{
				methodVisitor.visitInsn(Opcodes.DUP); // array ref
				genValue( index++ , Integer.class , methodVisitor ); // index
				genValue( value , value.getClass(), true , methodVisitor ); // value
				methodVisitor.visitInsn( Opcodes.AASTORE );
			}
		}

		public abstract Class<?> generate(MethodVisitor methodVisitor,ASTNode currentNode,MethodBodyWriter builder);

		protected <T> T assertType(Object value,Class<T> clazz) {
			if ( value.getClass() != clazz ) {
				throw new RuntimeException("Operation "+this+" expected a "+clazz.getName()+" on stack but got "+value);
			}
			return (T) value;
		}

		protected Class<?> generateConditional(MethodBodyWriter builder,int branchOnOperandsOpcode, int branchOnResultOpcode, ASTNode node,MethodVisitor methodVisitor) 
		{
			final Class<?> lhs = builder.generateMethodBody( node.child(0) , methodVisitor );
			final Class<?> rhs = builder.generateMethodBody( node.child(1) , methodVisitor );	
			
			final Class<?> targetType = getWidestType(lhs,rhs);
			return generateConditional(targetType,builder,branchOnOperandsOpcode,branchOnResultOpcode,methodVisitor);
		}
		
		protected Class<?> generateConditional(Class<?> targetType,MethodBodyWriter builder,int branchOnOperandsOpcode, int branchOnResultOpcode, MethodVisitor methodVisitor) 
		{
			if ( targetType == Long.class ) 
			{
				methodVisitor.visitInsn(Opcodes.LCMP);
				generateConditional(branchOnResultOpcode,methodVisitor);
			} else if ( targetType == Integer.class || targetType == Short.class || targetType == Byte.class) {
				generateConditional(branchOnOperandsOpcode,methodVisitor);	
			} else if ( targetType == Double.class ) 
			{
				// TODO: DCMPG or DCMPL only differ in their treatment of NaN
				methodVisitor.visitInsn( Opcodes.DCMPG );	
				generateConditional(branchOnResultOpcode,methodVisitor);					
			}  else if ( targetType == Float.class ) {
				// TODO: FCMPG or FCMPL only differ in their treatment of NaN
				methodVisitor.visitInsn( Opcodes.FCMPG );	
				generateConditional(branchOnResultOpcode,methodVisitor);							
			} else {
				throw new RuntimeException("I have no xCMP instruction for type "+targetType);
			}		
			return Boolean.TYPE;
		}

		protected void generateConditional(int conditionalOpcode,MethodVisitor methodVisitor) 
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
			genValue(Integer.valueOf(0) , Integer.TYPE , methodVisitor ); // FALSE
			methodVisitor.visitJumpInsn( Opcodes.GOTO , continueLabel);

			methodVisitor.visitLabel(trueLabel);
			genValue(Integer.valueOf(1) , Integer.TYPE , methodVisitor ); // TRUE

			methodVisitor.visitLabel( continueLabel );			
		}
		
		protected Class<?> getWidestType(Class<?> a,Class<?> b) 
		{
			return NumericType.getWiderType(a, b).getJavaType();
		}		

		protected Class<?> getWidestType(Object a,Object b) 
		{
			return NumericType.getWiderType(a, b).getJavaType();
		}

		protected void pushBooleanValue(Object value,MethodVisitor methodVisitor) 
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
		public Class<?> generate(MethodVisitor methodVisitor,ASTNode node, MethodBodyWriter builder) 
		{
			builder.generateMethodBody( node.child(0) , methodVisitor );
			builder.generateMethodBody( node.child(1) , methodVisitor );
			
			methodVisitor.visitInsn( Opcodes.IAND );
			return Boolean.TYPE;
		}
	};

	protected final AbstractOperation ASSIGNMENT = new AbstractOperation("=") {
		@Override
		public Class<?> generate(MethodVisitor methodVisitor,ASTNode node,MethodBodyWriter builder) 
		{
			throw new RuntimeException("assignments not implemented yet");
		}
	};

	protected final AbstractOperation DIVIDE = new AbstractOperation("/") {
		@Override
		public Class<?> generate(MethodVisitor methodVisitor,ASTNode node,MethodBodyWriter builder) 
		{
			Class<?> lhs = builder.generateMethodBody( node.child(0) , methodVisitor );
			Class<?> rhs = builder.generateMethodBody( node.child(1) , methodVisitor );			
			final Class<?> targetType = getWidestType(lhs,rhs);
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
			return getUnboxedType( targetType );
		}				
	};

	protected  final AbstractOperation EQ = new AbstractOperation("==") {
		@Override
		public Class<?> generate(MethodVisitor methodVisitor,ASTNode node,MethodBodyWriter builder) 
		{
			return generateConditional( builder , Opcodes.IF_ICMPEQ , Opcodes.IFEQ , node , methodVisitor );				
		}			
	};

	protected  final AbstractOperation GT = new AbstractOperation(">") {
		@Override
		public Class<?> generate(MethodVisitor methodVisitor,ASTNode node,MethodBodyWriter builder) 
		{
			return generateConditional( builder , Opcodes.IF_ICMPGT , Opcodes.IFGT , node , methodVisitor );				
		}			
	};

	protected  final AbstractOperation GTE  = new AbstractOperation(">=") {
		@Override
		public Class<?> generate(MethodVisitor methodVisitor,ASTNode node,MethodBodyWriter builder) 
		{
			return generateConditional( builder , Opcodes.IF_ICMPGE , Opcodes.IFGE , node, methodVisitor );				
		}			
	};

	protected  final AbstractOperation LT  = new AbstractOperation("<") {
		@Override
		public Class<?> generate(MethodVisitor methodVisitor,ASTNode node,MethodBodyWriter builder) 
		{
			return generateConditional( builder , Opcodes.IF_ICMPLT , Opcodes.IFLT , node , methodVisitor );				
		}			
	};

	protected  final AbstractOperation LTE = new AbstractOperation("<=") {
		@Override
		public Class<?> generate(MethodVisitor methodVisitor,ASTNode node,MethodBodyWriter builder) 
		{
			return generateConditional( builder, Opcodes.IF_ICMPLE , Opcodes.IFLE , node , methodVisitor );				
		}			
	};

	protected  final AbstractOperation MINUS = new AbstractOperation("-") 
	{
		@Override
		public Class<?> generate(MethodVisitor methodVisitor,ASTNode node,MethodBodyWriter builder) 
		{
			Class<?> lhs = builder.generateMethodBody( node.child(0) , methodVisitor );
			Class<?> rhs = builder.generateMethodBody( node.child(1) , methodVisitor );				
			final Class<?> targetType = getWidestType(lhs,rhs);
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
			return getUnboxedType( targetType );
		}				
	};

	protected  final AbstractOperation NEQ = new AbstractOperation("!=") {
		@Override
		public Class<?> generate(MethodVisitor methodVisitor,ASTNode node,MethodBodyWriter builder) 
		{
			return generateConditional( builder, Opcodes.IF_ICMPNE , Opcodes.IFNE , node,methodVisitor );
		}			
	};
	protected  final AbstractOperation NOT = new AbstractOperation("!") {
		@Override
		public Class<?> generate(MethodVisitor methodVisitor,ASTNode node,MethodBodyWriter builder) 
		{
			builder.generateMethodBody( node.child(0) , methodVisitor );
			generateConditional(Opcodes.IFEQ, methodVisitor);			
			return Boolean.TYPE;
		}			
	};

	protected  final AbstractOperation OR = new AbstractOperation("|") {
		@Override
		public Class<?> generate(MethodVisitor methodVisitor,ASTNode node,MethodBodyWriter builder) 
		{
			builder.generateMethodBody( node.child(0) , methodVisitor );
			builder.generateMethodBody( node.child(1) , methodVisitor );					
			methodVisitor.visitInsn( Opcodes.IOR );
			return Boolean.TYPE;
		}			
	};
	protected  final AbstractOperation PLUS = new AbstractOperation("+") {
		@Override
		public Class<?> generate(MethodVisitor methodVisitor,ASTNode node,MethodBodyWriter builder) 
		{
			Class<?> lhs = builder.generateMethodBody( node.child(0) , methodVisitor );
			Class<?> rhs = builder.generateMethodBody( node.child(1) , methodVisitor );		
			
			final Class<?> targetType = getWidestType(lhs,rhs);
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
			return getUnboxedType( targetType );
		}			
	};

	protected  final AbstractOperation TIMES = new AbstractOperation("*") {
		@Override
		public Class<?> generate(MethodVisitor methodVisitor,ASTNode node,MethodBodyWriter builder) 
		{
			final Class<?> lhs = builder.generateMethodBody( node.child(0) , methodVisitor );
			final Class<?> rhs = builder.generateMethodBody( node.child(1) , methodVisitor );					
			final Class<?> targetType = getWidestType(lhs,rhs);
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
			return getUnboxedType( targetType );
		}				
	};

	protected  final class InvokeMethodOnTarget extends AbstractOperation 
	{
		public final Identifier functionName;

		public InvokeMethodOnTarget(Identifier functionName) {
			super("InvokeMethodOnTarget( "+functionName.getSymbol()+")");
			this.functionName = functionName;
		}

		@Override
		public String toString() {
			return functionName.getSymbol()+"()";
		}

		@Override
		public Class<?> generate(MethodVisitor methodVisitor,ASTNode node,MethodBodyWriter builder) 
		{
			final FunctionCallNode fn = (FunctionCallNode) node;
			final Method method = fn.targetMethod;
			
			loadReferenceToTargetObject( methodVisitor );
			
			for ( int i = 0 ; i < node.getChildCount(); i++ ) 
			{
				final Class<?> argType = builder.generateMethodBody( node.child(i) ,  methodVisitor );
				final Class<?> expectedType = method.getParameterTypes()[i];
				if ( argType != expectedType ) 
				{
					if ( argType.isPrimitive() != expectedType.isPrimitive() ) 
					{
						if ( argType.isPrimitive() ) {
							box(argType,methodVisitor);
						} else {
							unbox(argType,methodVisitor);
						}
					} else if ( ! expectedType.isAssignableFrom( argType ) ) {
						throw new RuntimeException("Internal error, method "+method+" called with incompatible argument #"+(i+1)+", expected: "+expectedType+" , got "+argType);
					}
				}
			}

			if ( method == null ) {
				throw new RuntimeException("Internal error, target method not set on FunctionCallNode "+node+" - make sure the Typer ran!");
			}
			methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, builder.targetClass.getName().replace(".","/"), functionName.getSymbol() , Type.getMethodDescriptor( method ) );
			System.out.println("Method return type: "+method.getReturnType());
			return method.getReturnType();				
		}			
	};

	protected static Class<?> genValue(Object value,Class<?> targetType,MethodVisitor visitor) 
	{
		return genValue(value,targetType,false,visitor);
	}

	protected static Class<?> genValue(Object value,Class<?> targetType,boolean convertToObject,MethodVisitor visitor) 
	{
		if ( targetType == String.class ) 
		{
			if ( value.getClass() != String.class ) {
				throw new IllegalArgumentException("Don't know how to convert "+value+" to "+targetType.getName());
			}
			visitor.visitLdcInsn( value );
			return String.class;
		} else if ( targetType == Short.TYPE ) {
			final int v = ((Number) value).shortValue();
			visitor.visitIntInsn( Opcodes.SIPUSH , v );
		} else if ( targetType == Byte.TYPE ) {
			final int v = ((Number) value).byteValue();
			visitor.visitIntInsn( Opcodes.BIPUSH , v );		
		} else if ( targetType == Integer.TYPE ) {
			final int v = ((Number) value).intValue();
			if ( v == -1 ) {
				visitor.visitInsn(Opcodes.ICONST_M1);
			} else if ( v >= 0 && v <= 5 ) {
				visitor.visitInsn(Opcodes.ICONST_0+v);
			} else {
				visitor.visitLdcInsn( v );
			}
		} else if ( targetType == Long.TYPE ) {
			final long v = ((Number) value).longValue();
			visitor.visitLdcInsn( v );		
		} else if ( targetType == Float.TYPE  ) {
			final float v = ((Number) value).floatValue();				
			visitor.visitLdcInsn( v );				
		} else if ( targetType == Double.TYPE ) {
			final double v = ((Number) value).doubleValue();				
			visitor.visitLdcInsn( v );
		} 
		else if ( targetType == Boolean.TYPE ) 
		{
			if ( ((Boolean) value).booleanValue() ) {
				visitor.visitInsn(Opcodes.ICONST_1 );
			} else {
				visitor.visitInsn(Opcodes.ICONST_0 );
			}
		} else {
			throw new RuntimeException("Unhandled target type: "+targetType);
		}
		if ( convertToObject ) {
			box( targetType , visitor );
			return toObjectType( targetType );
		}		
		return getUnboxedType( targetType );
	}	

	public Class<?> generateMethodBody(ASTNode node,MethodVisitor visitor) {

		Class<?> lastType = null;
		switch( node.getNodeType() ) 
		{
			case AST:
			case EXPRESSION:
				for ( ASTNode child : node.children() ) {
					lastType = generateMethodBody( child , visitor );
				}
				return lastType;
			case BOOLEAN:
			case NUMBER:
			case STRING:
				final Object value = ((ILiteralNode) node).value() ;
				if ( value instanceof String) {
					return genValue( value , String.class, visitor );					
				}
				return genValue( value , getUnboxedType( value.getClass() ) , visitor );
			case FUNCTION_CALL:
				final FunctionCallNode fn= (FunctionCallNode) node;
				return new InvokeMethodOnTarget( fn.functionName ).generate( visitor ,  fn , this );
			case OPERATOR:
				return pushOperator((OperatorNode) node , visitor );
			}
			throw new RuntimeException("Internal error,unhandled AST node "+node);
	}

	private Class<?> pushOperator(OperatorNode node,MethodVisitor visitor) 
	{
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
		return abstractOP.generate( visitor ,  node , this );
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
	
	protected static void outputTypeConversion(Class<?> currentlyOnStack,Class<?> requiredType) {
		if ( currentlyOnStack == requiredType ) {
			return;
		}
		if ( currentlyOnStack.isPrimitive() && ! requiredType.isPrimitive() ) {
			
		}
	}
	
	public static Class<?> box(Class<?> currentType,MethodVisitor mv) 
	{
		if ( ! currentType.isPrimitive() ) {
			return currentType;
		}
			// nothing to do here
		if ( currentType == Integer.TYPE) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;" );
			return Integer.class;
		} 
		if ( currentType == Long.TYPE) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;" );
			return Long.class;
		} 
		if ( currentType == Short.TYPE) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(J)Ljava/lang/Short;" );
			return Short.class;
		} 
		if ( currentType == Byte.TYPE) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(J)Ljava/lang/Byte;" );
			return Byte.class;
		} 
		if ( currentType == Float.TYPE) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;" );
			return Float.class;
		} 
		if ( currentType == Double.TYPE) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;" );
			return Double.class;
		} 
		if ( currentType == Boolean.TYPE) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;" );
			return Boolean.class;
		} 
		throw new RuntimeException("Don't know how to box "+currentType);
	}
	
	public static Class<?> unbox(Class<?> currentType,MethodVisitor mv) 
	{
		if ( currentType.isPrimitive() ) {
			return currentType;
		}
			// nothing to do here
		if ( currentType == Integer.class) {
			mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL, typeName( Integer.class) , "intValue", "()I");				
			return Integer.TYPE;
		} 
		if ( currentType == Long.class) {
			mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL, typeName( Long.class) , "longValue", "()J");				
			return Long.TYPE;
		} 
		if ( currentType == Short.class) 
		{
			mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL, typeName( Short.class) , "shortValue", "()S");				
			return Short.TYPE;
		} 
		if ( currentType == Byte.class) {
			mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL, typeName( Byte.class) , "byteValue", "()B");					
			return Byte.TYPE;
		} 
		if ( currentType == Float.class) {
			mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL, typeName( Float.class) , "floatValue", "()F");				
			return Float.TYPE;
		} 
		if ( currentType == Double.class) {
			mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL, typeName( Double.class) , "doubleValue", "()D");			
			return Double.TYPE;
		} 
		if ( currentType == Boolean.class) {
			mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL, typeName( Boolean.class) , "booleanValue", "()Z");
			return Boolean.TYPE;
		} 
		throw new RuntimeException("Don't know how to unbox "+currentType);
	}	
	
	private static String typeName(Class<?> clazz) {
		return clazz.getName().replace(".","/");
	}
	
	public static Class<?> getUnboxedType(Class<?> cl) {
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