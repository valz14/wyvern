package wyvern.tools.typedAST.core.expressions;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import wyvern.stdlib.Globals;
import wyvern.target.corewyvernIL.expression.IExpr;
import wyvern.target.corewyvernIL.modules.TypedModuleSpec;
import wyvern.target.corewyvernIL.support.CallableExprGenerator;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.target.corewyvernIL.support.InvocationExprGenerator;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.ErrorMessage;
import static wyvern.tools.errors.ErrorMessage.CANNOT_INVOKE;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;
import static wyvern.tools.errors.ToolError.reportEvalError;
import wyvern.tools.typedAST.abs.CachingTypedAST;
import wyvern.tools.typedAST.core.binding.typechecking.AssignableNameBinding;
import wyvern.tools.typedAST.core.values.VarValue;
import wyvern.tools.typedAST.interfaces.Assignable;
import wyvern.tools.typedAST.interfaces.CoreAST;
import wyvern.tools.typedAST.interfaces.ExpressionAST;
import wyvern.tools.typedAST.interfaces.InvokableValue;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.types.Environment;
import wyvern.tools.types.OperatableType;
import wyvern.tools.types.Type;
import wyvern.tools.types.extensions.ClassType;
import wyvern.tools.util.EvaluationEnvironment;

public class Invocation extends CachingTypedAST implements CoreAST, Assignable {

    private String operationName;
    private ExpressionAST receiver;
    private TypedAST argument;
    private FileLocation location = FileLocation.UNKNOWN;

    /**
      * Invocation of an operation on two operands.
      *
      * @param op1 the first operand
      * @param op2 the second operand
      * @param operatorName the operator invoked.
      * @param fileLocation the location in the source where the operation occurs
      */
    public Invocation(TypedAST op1, String operatorName, TypedAST op2, FileLocation fileLocation) {
        this.receiver = (ExpressionAST) op1;

        this.argument = op2;
        this.operationName = operatorName;
        this.location = fileLocation;
    }

    public TypedAST getArgument() {
        return argument;
    }

    public TypedAST getReceiver() {
        return receiver;
    }

    public String getOperationName() {
        return operationName;
    }

    @Override
    public Map<String, TypedAST> getChildren() {
        Hashtable<String, TypedAST> children = new Hashtable<>();
        if (receiver != null) {
            children.put("receiver", receiver);
        }
        if (argument != null) {
            children.put("argument", argument);
        }
        return children;
    }

    @Override
    public ExpressionAST doClone(Map<String, TypedAST> nc) {
        return new Invocation(
            nc.get("receiver"),
            operationName,
            nc.get("argument"),
            location
        );
    }

    public FileLocation getLocation() {
        return this.location;
    }

    @Override
    public IExpr generateIL(
            GenContext ctx,
            ValueType expectedType,
            List<TypedModuleSpec> dependencies) {

        CallableExprGenerator generator = getCallableExpr(ctx);

        if (argument != null) {
            IExpr arg  = ((ExpressionAST) argument)
                .generateIL(ctx, null, dependencies);

            List<IExpr> args = new ArrayList<IExpr>();
            args.add(arg);

            return generator.genExprWithArgs(args, this);
        } else {
            return generator.genExpr();
        }
    }

    @Override
    public CallableExprGenerator getCallableExpr(GenContext genCtx) {
        return new InvocationExprGenerator(
            receiver.generateIL(genCtx, null, null),
            operationName,
            genCtx,
            getLocation()
        );
    }
}

