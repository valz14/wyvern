package wyvern.tools.typedAST.core.expressions;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import wyvern.target.corewyvernIL.expression.IExpr;
import wyvern.target.corewyvernIL.modules.TypedModuleSpec;
import wyvern.target.corewyvernIL.support.CallableExprGenerator;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.target.corewyvernIL.type.ValueType;
import static wyvern.tools.errors.ErrorMessage.VARIABLE_NOT_DECLARED;
import wyvern.tools.errors.FileLocation;
import static wyvern.tools.errors.ToolError.reportError;
import wyvern.tools.errors.ToolError;
import wyvern.tools.typedAST.abs.AbstractExpressionAST;
import wyvern.tools.typedAST.core.binding.AssignableValueBinding;
import wyvern.tools.typedAST.core.binding.NameBinding;
import wyvern.tools.typedAST.core.binding.typechecking.AssignableNameBinding;
import wyvern.tools.typedAST.core.values.VarValue;
import wyvern.tools.typedAST.interfaces.Assignable;
import wyvern.tools.typedAST.interfaces.CoreAST;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.TypeResolver;
import wyvern.tools.types.extensions.TypeType;
import wyvern.tools.util.EvaluationEnvironment;


public class Variable extends AbstractExpressionAST implements CoreAST, Assignable {

    private NameBinding binding;
    private FileLocation location = FileLocation.UNKNOWN;

    public Variable(NameBinding binding, FileLocation location) {
        this.binding = binding;
        this.location = location;
    }

    public String getName() {
        return this.binding.getName();
    }

    @Override
    public Map<String, TypedAST> getChildren() {
        return new Hashtable<>();
    }

    @Override
    public TypedAST cloneWithChildren(Map<String, TypedAST> nc) {
        return new Variable(binding, location);
    }

    public FileLocation getLocation() {
        return this.location;
    }

    @Override
    public IExpr generateIL(
            GenContext ctx,
            ValueType expectedType,
            List<TypedModuleSpec> dependencies) {
        return ctx.lookupExp(getName(), location);
    }

    @Override
    public CallableExprGenerator getCallableExpr(GenContext ctx) {
        try {
            return ctx.getCallableExpr(getName());
        } catch (RuntimeException e) {
            ToolError.reportError(VARIABLE_NOT_DECLARED, location, getName());
            throw new RuntimeException("impossible");
        }
    }
}
