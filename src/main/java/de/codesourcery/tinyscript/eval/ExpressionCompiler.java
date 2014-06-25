package de.codesourcery.tinyscript.eval;

import java.io.PrintWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import de.codesourcery.tinyscript.ast.AST;

public class ExpressionCompiler {

	private final String className;
	private ClassVisitor classWriter;

	private MethodVisitor mv;

	public ExpressionCompiler(String className) {
		this.className = convertClassName(className);
	}

	public byte[] compile(AST ssa,Class<?> targetClass) 
	{
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		final PrintWriter printWriter = new PrintWriter(System.out , true );
		classWriter = new TraceClassVisitor( writer , printWriter );

		try {
			startMethod();
			
			final MethodBodyWriter builder = new MethodBodyWriter(targetClass);
			final Class<?> returnType = builder.generateMethodBody( ssa, mv );
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

		mv = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/lang/Object;Lde/codesourcery/tinyscript/eval/IScope;)V", 
				null, // signature
		        null); // exceptions
		
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ALOAD, 0); // 'this' pointer
		
		// 	public CompiledExpression(T target, IScope variableResolver) 		
		mv.visitVarInsn(Opcodes.ALOAD, 1); // target
		mv.visitVarInsn(Opcodes.ALOAD, 2); // scope

		// invoke constructor

		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "de/codesourcery/tinyscript/eval/CompiledExpression", 
				"<init>", 
				"(Ljava/lang/Object;Lde/codesourcery/tinyscript/eval/IScope;)V");
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
			MethodBodyWriter.box( returnType ,  mv );
			mv.visitInsn(Opcodes.ARETURN);
		} else {
			// void method, simply return NULL
			mv.visitInsn(Opcodes.ACONST_NULL);
			mv.visitInsn(Opcodes.ARETURN);			
		}
		mv.visitMaxs(0, 0);		
		mv.visitEnd(); // end of method
		mv.visitEnd(); // end of class
	}
}