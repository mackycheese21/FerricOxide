package mackycheese21.ferricoxide.compile;

import mackycheese21.ferricoxide.ast.ConcreteType;
import mackycheese21.ferricoxide.ast.IdentifierMap;
import mackycheese21.ferricoxide.ast.expr.AccessVar;
import mackycheese21.ferricoxide.ast.stmt.*;
import mackycheese21.ferricoxide.ast.visitor.StatementVisitor;
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMBuilderRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import static org.bytedeco.llvm.global.LLVM.*;

public class CompileStatementVisitor implements StatementVisitor<Void> {

    private final LLVMBuilderRef builder;
    private final LLVMValueRef currentFunction;
    public final IdentifierMap<ConcreteType> variableTypes;
    public final IdentifierMap<LLVMValueRef> variableRefs;
    private final CompileExpressionVisitor compileExpression;

    public CompileStatementVisitor(LLVMBuilderRef builder, LLVMValueRef currentFunction, IdentifierMap<ConcreteType.Function> functionTypes, IdentifierMap<LLVMValueRef> functionRefs) {
        this.builder = builder;
        this.currentFunction = currentFunction;
        this.variableTypes = new IdentifierMap<>(null);
        this.variableRefs = new IdentifierMap<>(null);
        this.compileExpression = new CompileExpressionVisitor(builder, currentFunction, variableTypes, variableRefs, functionTypes, functionRefs);
    }

    @Override
    public Void visitAssign(Assign assign) {
        if (assign.a instanceof AccessVar a) {
            LLVMBuildStore(builder, assign.b.visit(compileExpression), variableRefs.mapGet(a.name));
            return null;
        } else {
            throw new UnsupportedOperationException("assigning lvalues other than var access not yet supported :((");
        }
    }

    @Override
    public Void visitIfStmt(IfStmt ifStmt) {
        LLVMBasicBlockRef then = LLVMAppendBasicBlock(currentFunction, "IfStmt.then");
        LLVMBasicBlockRef end = LLVMAppendBasicBlock(currentFunction, "IfStmt.end");
        LLVMValueRef condition = ifStmt.condition.visit(compileExpression);
        if (ifStmt.otherwise == null) {
            LLVMBuildCondBr(builder, condition, then, end);

            LLVMPositionBuilderAtEnd(builder, then);
            ifStmt.then.visit(this);
        } else {
            LLVMBasicBlockRef otherwise = LLVMAppendBasicBlock(currentFunction, "IfStmt.otherwise");
            LLVMBuildCondBr(builder, condition, then, otherwise);

            LLVMPositionBuilderAtEnd(builder, then);
            ifStmt.then.visit(this);
            LLVMBuildBr(builder, end);

            LLVMPositionBuilderAtEnd(builder, otherwise);
            ifStmt.otherwise.visit(this);
        }
        LLVMBuildBr(builder, end);
        LLVMPositionBuilderAtEnd(builder, end);
        return null;
    }

    @Override
    public Void visitBlock(Block blockStmt) {
        blockStmt.statements.forEach(stmt -> stmt.visit(this));
        return null;
    }

    @Override
    public Void visitReturnStmt(ReturnStmt returnStmt) {
        LLVMBuildRet(builder, returnStmt.value.visit(compileExpression));
        return null;
    }

    @Override
    public Void visitDeclareVar(DeclareVar declareVar) {
        LLVMValueRef alloc = LLVMBuildAlloca(builder, declareVar.type.llvmTypeRef(), "DeclareVar");
        LLVMBuildStore(builder, declareVar.value.visit(compileExpression), alloc);
        variableTypes.mapAdd(declareVar.name, declareVar.type);
        variableRefs.mapAdd(declareVar.name, alloc);
        return null;
    }

    @Override
    public Void visitWhileStmt(WhileStmt whileStmt) {
        LLVMBasicBlockRef cond = LLVMAppendBasicBlock(currentFunction, "WhileStmt.cond");
        LLVMBasicBlockRef start = LLVMAppendBasicBlock(currentFunction, "WhileStmt.start");
        LLVMBasicBlockRef end = LLVMAppendBasicBlock(currentFunction, "WhileStmt.end");

        LLVMBuildBr(builder, cond);

        LLVMPositionBuilderAtEnd(builder, cond);
        LLVMBuildCondBr(builder, whileStmt.condition.visit(compileExpression), start, end);

        LLVMPositionBuilderAtEnd(builder, start);
        whileStmt.body.visit(this);
        LLVMBuildBr(builder, cond);

        LLVMPositionBuilderAtEnd(builder, end);
        return null;
    }

    @Override
    public Void visitCallStmt(CallStmt callStmt) {
        callStmt.callExpr.visit(compileExpression);
        return null;
    }
}