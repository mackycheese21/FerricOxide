package mackycheese21.ferricoxide.ast;

import mackycheese21.ferricoxide.ConcreteType;
import mackycheese21.ferricoxide.GlobalContext;
import mackycheese21.ferricoxide.Variables;
import org.bytedeco.llvm.LLVM.LLVMBuilderRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import static org.bytedeco.llvm.global.LLVM.*;

public class DeclareVar extends Ast {

    private final String name;
    private final Ast value;

    public DeclareVar(String name, Ast value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public ConcreteType getConcreteType(GlobalContext globalContext, Variables variables) {
        return ConcreteType.NONE;
    }

    @Override
    public LLVMValueRef generateIR(GlobalContext globalContext, Variables variables, LLVMBuilderRef builder) {
        ConcreteType type = value.getConcreteType(globalContext, variables);
        LLVMValueRef alloca = LLVMBuildAlloca(builder, type.llvmTypeRef(), "declarevar");
        LLVMBuildStore(builder, value.generateIR(globalContext, variables, builder), alloca);
        variables.mapAdd(name, new Variables.Entry(alloca, type));
        return null;
    }
}
