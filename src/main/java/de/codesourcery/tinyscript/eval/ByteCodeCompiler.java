package de.codesourcery.tinyscript.eval;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import com.sun.xml.internal.ws.org.objectweb.asm.Type;

import de.codesourcery.tinyscript.ast.AST;
import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.ast.ILiteralNode;
import de.codesourcery.tinyscript.ast.OperatorNode;
import de.codesourcery.tinyscript.ast.VariableNode;

public class ByteCodeCompiler {

	private final String className;
	private ClassVisitor classWriter;

	private MethodVisitor mv;
	private Class<?> returnType;

	private Object topOfStack;

	private final Map<Identifier,Class<?>> varTypes = new HashMap<>();
	private final Map<Identifier,Integer> varIndices = new HashMap<>();	

	private int currentVarIndex=1;

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

	public ByteCodeCompiler(String className) {
		this.className = convertClassName(className);
	}

	public byte[] compile(AST ssa) 
	{
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		final PrintWriter printWriter = new PrintWriter(System.out , true );
		classWriter = new TraceClassVisitor( writer , printWriter );
		returnType = ssa.getDataType();

		try {
			startMethod();
			for ( ASTNode expr: ssa.children() ) {
				doCompile(expr);			
			}
			endMethod();
		} 
		finally {
			System.out.println(((TraceClassVisitor) classWriter).text);
		}

		byte[] byteArray = writer.toByteArray();

		// verify
		ClassReader cr = new ClassReader(byteArray);
		cr.accept(new CheckClassAdapter(new ClassWriter(0)), 0);		

		return byteArray;
	}

	private static String convertClassName(String name) {
		return name.replace('.', '/');
	}

	private static String convertClassName(Class<?> cl) {
		return cl.getName().replace('.', '/');
	}	

	private void doCompile(ASTNode node) 
	{
		switch( node.getNodeType() ) 
		{
		case BOOLEAN:
		case NUMBER:
		case STRING:
			outputLiteral((ILiteralNode) node);
			break;
		case VARIABLE:
			break;			
		case FAST_METHOD_INVOCATION:
			break;
		case FUNCTION_CALL:
			break;
		case OPERATOR:
			outputOperator((OperatorNode) node);
			break;
		default:
			throw new RuntimeException("Unhandled node: "+node);
		}
	}

	private void outputOperator(OperatorNode node) {

		switch( node.type ) 
		{
		case AND:
			break;
		case ASSIGNMENT:
			final VariableNode lhs = (VariableNode) node.child(0);
			final ASTNode rhs = node.child(1);

			doCompile( rhs ); // evaluate RHS
			outputStore( lhs.name , rhs.getDataType() );
			return;
		case DIVIDE:
			break;
		case EQ:
			break;
		case GT:
			break;
		case GTE:
			break;
		case LT:
			break;
		case LTE:
			break;
		case MINUS:
			break;
		case NEQ:
			break;
		case NOT:
			break;
		case OR:
			break;
		case PLUS:
			Class<?> result = node.getDataType();
			if ( result == Long.class ) {
				mv.visitInsn(Opcodes.LADD);	
			} else if ( result == Integer.class ) {
				mv.visitInsn(Opcodes.IADD);	
			} else if ( result == Float.class ) {
				mv.visitInsn(Opcodes.FADD);	
			} else if ( result == Double.class ) {
				mv.visitInsn(Opcodes.DADD);	
			} else {
				throw new RuntimeException("Don't know how to output ADD for "+result);
			}
			return;
		case TIMES:
			break;
		default:
			break;
		}
		throw new RuntimeException("Don't know how to output operator "+node);
	}

	private void startMethod() 
	{
		// start visiting class
		final String superClass = convertClassName(CompiledExpression.class);

		classWriter.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, convertClassName( className ) , null, superClass , null);

		mv = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/lang/Object;Lde/codesourcery/tinyscript/eval/IScope;)V", null, null);
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitVarInsn(Opcodes.ALOAD, 2);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "de/codesourcery/tinyscript/eval/CompiledExpression", "<init>", "(Ljava/lang/Object;Lde/codesourcery/tinyscript/eval/IScope;)V");
		mv.visitInsn(Opcodes.RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		mv = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "apply", "()Ljava/lang/Object;", null, null);
		mv.visitCode();
	}

	private static String classToType(Class<?> clazz) {
		return Type.getDescriptor( clazz );
	}

	private void endMethod() 
	{
		if ( returnType != null ) 
		{
			outputConversionToObject();
			mv.visitInsn(Opcodes.ARETURN);
		} else {
			// void method, just return NULL
			mv.visitInsn(Opcodes.ACONST_NULL);
			mv.visitInsn(Opcodes.ARETURN);			
		}
		mv.visitMaxs(0, 0);		
		mv.visitEnd(); // end of method
		mv.visitEnd(); // end of class
	}

	private void outputLiteral(ILiteralNode node) 
	{
		Object value = node.value();
		topOfStack = value;
		outputValue(value);
	}
	private void outputValue(Object value) 
	{
		if ( value instanceof String ) {
			mv.visitLdcInsn( value );
		} else if ( value instanceof Integer) {
			mv.visitLdcInsn( value );		
			outputConversionToObject();
		} else if ( value instanceof Long) {
			mv.visitLdcInsn( value );
			outputConversionToObject();
		} else if ( value instanceof Float) {
			mv.visitLdcInsn( value );
			outputConversionToObject();
		} else if ( value instanceof Double) {
			mv.visitLdcInsn( value );
			outputConversionToObject();
		} else if ( value instanceof Boolean) {
			mv.visitLdcInsn( value );
			outputConversionToObject();
		} else {
			throw new RuntimeException("Don't know how to load "+value);
		}
	}

	private void outputStore(Identifier identifier,Class<?> targetType) 
	{
		if ( targetType == null ) {
			throw new IllegalArgumentException("targetType must not be NULL");
		}
		if ( identifier == null ) {
			throw new IllegalArgumentException("identifier must not be NULL");
		}
		Class<?> existingType;
		if ( varTypes.containsKey(identifier) ) {
			existingType = varTypes.get(identifier);
		} else {
			existingType = targetType;
		}
		if ( existingType != targetType ) {
			throw new RuntimeException("Trying to store variable "+identifier+" using type "+targetType+" that was previously stored as "+existingType);
		}
		varTypes.put(identifier, targetType);
		mv.visitInsn(Opcodes.DUP);
		if ( targetType == Long.class ) {
			mv.visitVarInsn(Opcodes.ASTORE, variableIndex( identifier , true ) );
		} else if ( targetType == Integer.class ) {
			mv.visitVarInsn(Opcodes.ASTORE, variableIndex( identifier , true ) );			
		} else if ( targetType == Short.class ) {
			mv.visitVarInsn(Opcodes.ASTORE, variableIndex( identifier, true  ) );			
		} else if ( targetType == Byte.class ) {
			mv.visitVarInsn(Opcodes.ASTORE, variableIndex( identifier , true ) );			
		} else if ( targetType == String.class ) {
			mv.visitVarInsn(Opcodes.ASTORE, variableIndex( identifier , true ) );			
		} else if ( targetType == Boolean.class ) {
			mv.visitVarInsn(Opcodes.ASTORE, variableIndex( identifier , true ) );			
		} else {
			throw new RuntimeException("Don't know how to store "+identifier+" with type "+targetType);
		}
	}	

	private void outputLoad(Identifier identifier) {
		int index = variableIndex(identifier, false);
		mv.visitVarInsn(Opcodes.ALOAD, index );	
	}

	private void outputConversionToObject() {
		if ( topOfStack == null ) {
			return;
		}
		Object value = topOfStack;
		if ( value instanceof String ) 
		{
			// nothing to do here
		} else if ( value instanceof Integer) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;" );
		} else if ( value instanceof Long) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;" );
		} else if ( value instanceof Float) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;" );
		} else if ( value instanceof Double) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;" );
		} else if ( value instanceof Boolean) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;" );
		} else {
			throw new RuntimeException("Don't know how to load "+value);
		}		
	}
}