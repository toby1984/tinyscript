package de.codesourcery.tinyscript.eval;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import de.codesourcery.tinyscript.ast.AST;
import de.codesourcery.tinyscript.ast.ASTNode;
import de.codesourcery.tinyscript.ast.ILiteralNode;

public class ByteCodeCompiler {

	private ClassWriter classWriter;
	private MethodVisitor mv;
	
	public ByteCodeCompiler() {
	}
	
	public byte[] compile(AST ssa) 
	{
		classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
		startMethod();
		for ( ASTNode expr: ssa.children() ) {
			doCompile(expr);			
		}
		endMethod();
		return classWriter.toByteArray();
	}
	
	private void doCompile(ASTNode node) 
	{
		switch( node.getNodeType() ) 
		{
			case BOOLEAN:
			case NUMBER:
			case STRING:
				outputLiteral((ILiteralNode) node);
				return;
			case VARIABLE:
				break;			
			case FAST_METHOD_INVOCATION:
				break;
			case FUNCTION_CALL:
				break;
			case OPERATOR:
				break;
			default:
				throw new RuntimeException("Unhandled node: "+node);
		}
	}
	
	private void startMethod() 
	{
		// start visiting class
		classWriter.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, "com/gannon/ASMInterpreterMain/HelloWorldOutPut", null, "java/lang/Object", null);
		
		// insert constructor
		mv=classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        
        // insert main method
        mv=classWriter.visitMethod(Opcodes.ACC_PUBLIC+ Opcodes.ACC_STATIC, "Main", "([Ljava/lang/String;)V", null, null);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn("Test");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();		
	}
	
	private void endMethod() 
	{
        mv.visitEnd();		
	}
	
	private void outputLiteral(ILiteralNode node) {
	}
}