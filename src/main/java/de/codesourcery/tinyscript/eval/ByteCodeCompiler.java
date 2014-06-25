package de.codesourcery.tinyscript.eval;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.TypeVariable;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import de.codesourcery.tinyscript.ast.AST;

public class ByteCodeCompiler {

	private final String className;
	private ClassVisitor classWriter;

	private MethodVisitor mv;

	public ByteCodeCompiler(String className) {
		this.className = convertClassName(className);
	}

	public byte[] compile(AST ssa,Class<?> targetClass) 
	{
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		final PrintWriter printWriter = new PrintWriter(System.out , true );
		classWriter = new TraceClassVisitor( writer , printWriter );

		try {
			startMethod();
			
			final StackBuilder builder = new StackBuilder(targetClass);
			builder.buildStack( ssa );
			final Class<?> returnType = builder.output( mv );
			System.out.println("==> end method: "+returnType+" (is_primitive: "+returnType.isPrimitive()+")");
			endMethod(returnType);
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

	private void startMethod() 
	{
		// start visiting class
		final String superClass = convertClassName(CompiledExpression.class);

		classWriter.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, convertClassName( className ) , 
				"<T:Ljava/lang/Object;>L"+superClass+";", // signature
				superClass , 
				null // interfaces
				);

		mv = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/lang/Object;Ljava/lang/Class;Lde/codesourcery/tinyscript/eval/IScope;)V", 
				null, // signature
		        null); // exceptions
		
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ALOAD, 0); // 'this' pointer
		
		// 	public CompiledExpression(T target, Class<T> targetClass,IScope variableResolver) 		
		mv.visitVarInsn(Opcodes.ALOAD, 1); // target
		mv.visitVarInsn(Opcodes.ALOAD, 2); // targetClass
		mv.visitVarInsn(Opcodes.ALOAD, 3); // variableResolver

		// invoke constructor

		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "de/codesourcery/tinyscript/eval/CompiledExpression", 
				"<init>", 
				"(Ljava/lang/Object;Ljava/lang/Class;Lde/codesourcery/tinyscript/eval/IScope;)V");
		mv.visitInsn(Opcodes.RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		mv = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "apply", "()Ljava/lang/Object;", null, null);
		mv.visitCode();
	}

	private void endMethod(Class<?> returnType) 
	{
		if ( returnType != null ) 
		{
			if ( returnType.isPrimitive() ) {
				outputConversionToObject( StackBuilder.toObjectType( returnType ) , mv);
			}
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

	public static void outputConversionToObject(Class<?> targetType,MethodVisitor mv) 
	{
		if ( targetType == String.class || targetType == Object.class ) 
		{
			// nothing to do here
		} else if ( targetType == Integer.class) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;" );
		} else if ( targetType == Long.class) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;" );
		} else if ( targetType == Short.class) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(J)Ljava/lang/Short;" );	
		} else if ( targetType == Byte.class) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(J)Ljava/lang/Byte;" );				
		} else if ( targetType == Float.class) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;" );
		} else if ( targetType == Double.class) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;" );
		} else if ( targetType == Boolean.class) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;" );
		} else {
			throw new RuntimeException("Don't know how to load "+targetType);
		}		
	}
}