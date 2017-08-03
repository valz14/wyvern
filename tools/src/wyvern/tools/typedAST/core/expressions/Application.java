package wyvern.tools.typedAST.core.expressions;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import wyvern.stdlib.Globals;
import wyvern.target.corewyvernIL.FormalArg;
import wyvern.target.corewyvernIL.decl.Declaration;
import wyvern.target.corewyvernIL.decl.TypeDeclaration;
import wyvern.target.corewyvernIL.decltype.ConcreteTypeMember;
import wyvern.target.corewyvernIL.decltype.DeclType;
import wyvern.target.corewyvernIL.decltype.DefDeclType;
import wyvern.target.corewyvernIL.expression.Expression;
import wyvern.target.corewyvernIL.expression.IExpr;
import wyvern.target.corewyvernIL.expression.MethodCall;
import wyvern.target.corewyvernIL.expression.New;
import wyvern.target.corewyvernIL.modules.TypedModuleSpec;
import wyvern.target.corewyvernIL.support.CallableExprGenerator;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.target.corewyvernIL.type.StructuralType;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.ErrorMessage;
import static wyvern.tools.errors.ErrorMessage.TYPE_CANNOT_BE_APPLIED;
import static wyvern.tools.errors.ErrorMessage.VALUE_CANNOT_BE_APPLIED;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;
import static wyvern.tools.errors.ToolError.reportError;
import static wyvern.tools.errors.ToolError.reportEvalError;
import wyvern.tools.typedAST.abs.CachingTypedAST;
import wyvern.tools.typedAST.core.declarations.DefDeclaration;
import wyvern.tools.typedAST.core.values.UnitVal;
import wyvern.tools.typedAST.interfaces.ApplyableValue;
import wyvern.tools.typedAST.interfaces.CoreAST;
import wyvern.tools.typedAST.interfaces.ExpressionAST;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.types.ApplyableType;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.extensions.Arrow;
import wyvern.tools.types.extensions.Intersection;
import wyvern.tools.util.EvaluationEnvironment;

public class Application extends CachingTypedAST implements CoreAST {
    private ExpressionAST function;
    private ExpressionAST argument;
    private List<Type> generics;
    private FileLocation location;

    public Application(TypedAST function, TypedAST argument, FileLocation location) {
        this(function, argument, location, null);
    }

    /**
      * Application represents a call cite for a function call.
      *
      * @param function the function that is called
      * @param argument the argument passed at the call site (may be a tuple, unit, or singleton
      * @param location the location of the call site in the source file
      * @param generics2 the vector of type parameters passed at the call site
      */
    public Application(TypedAST function, TypedAST argument,
            FileLocation location, List<Type> generics2) {
        this.function = (ExpressionAST) function;
        this.argument = (ExpressionAST) argument;
        this.location = location;
        this.generics = (generics2 != null) ? generics2 : new LinkedList<Type>();
    }

    @Override
    protected Type doTypecheck(Environment env, Optional<Type> expected) {
        Type fnType = function.typecheck(env, Optional.empty());

        Type argument = null;
        if (fnType instanceof Arrow) {
            argument = ((Arrow) fnType).getArgument();
        } else if (fnType instanceof Intersection) {
            List<Type> args = fnType.getChildren().values().stream()
                    .filter(tpe -> tpe instanceof Arrow).map(tpe -> ((Arrow)tpe).getArgument())
                    .collect(Collectors.toList());
            argument = new Intersection(args);
        }
        if (this.argument != null) {
            this.argument.typecheck(env, Optional.ofNullable(argument));
        }

        if (!(fnType instanceof ApplyableType)) {
            reportError(TYPE_CANNOT_BE_APPLIED, this, fnType.toString());
        }

        return ((ApplyableType) fnType).checkApplication(this, env);
    }

    public TypedAST getArgument() {
        return argument;
    }

    public TypedAST getFunction() {
        return function;
    }

    @Override
    @Deprecated
    public Value evaluate(EvaluationEnvironment env) {
        TypedAST lhs = function.evaluate(env);
        if (Globals.checkRuntimeTypes && !(lhs instanceof ApplyableValue)) {
            reportEvalError(VALUE_CANNOT_BE_APPLIED, lhs.toString(), this);
        }
        ApplyableValue fnValue = (ApplyableValue) lhs;

        return fnValue.evaluateApplication(this, env);
    }

    @Override
    public Map<String, TypedAST> getChildren() {
        Hashtable<String, TypedAST> children = new Hashtable<>();
        children.put("function", function);
        children.put("argument", argument);
        return children;
    }

    @Override
    public ExpressionAST doClone(Map<String, TypedAST> nc) {
        return new Application(
            (ExpressionAST) nc.get("function"),
            (ExpressionAST) nc.get("argument"),
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

        CallableExprGenerator exprGen = function.getCallableExpr(ctx);
        
        /* Method call on a dynamic object. We pretend there's an appropriate declaration,
         * and ignore the expression generator. */
        if (exprGen.getDeclType(ctx) == null) {

            // Generate code for the arguments.
            List<IExpr> args = new LinkedList<>();
            if (!(argument instanceof UnitVal)) {
                args.add(argument.generateIL(ctx, null, dependencies));
            }
    
            // Need to do this to find out what the method name is.
            if (!(function instanceof Invocation)) {
                throw new RuntimeException("Getting field of dynamic object,"
                                          + "which isn't an invocation.");
            }
            Invocation invocation = (Invocation) function;

            return new MethodCall(
                invocation.getReceiver().generateIL(ctx, null, dependencies),
                invocation.getOperationName(),
                args,
                this);
        }
        
        /* Otherwise look up declaration. Ensure arguments match the declaration. */
        DefDeclType ddt = exprGen.getDeclType(ctx);
        List<FormalArg> formals = ddt.getFormalArgs();
        List<IExpr> args = new LinkedList<IExpr>();

        // Add generic arguments to the argslist
        generateGenericArgs(ddt.getName(), args, formals, ctx, ddt, dependencies);

        if (argument instanceof TupleObject) {
            generateILForTuples(formals, args, ctx, dependencies);
        } else if (argument instanceof UnitVal) {
            // the method takes no arguments
        } else {
            // adding the last formal
            if (formals.size() != 1 + args.size()) {
                ToolError.reportError(
                    ErrorMessage.WRONG_NUMBER_OF_ARGUMENTS,
                    this,
                    "" + formals.size(),
                    new Integer(1 + args.size()).toString()
                );
            }
            
            // TODO: propagate type downward from the last formal
            args.add(argument.generateIL(ctx, formals.get(formals.size() - 1).getType(), null));
        }

        // generate the call
        return exprGen.genExprWithArgs(args, this);
    }

    private int countFormalGenerics(List<FormalArg> formals) {

        int count = 0;
        for (FormalArg formal : formals) {
            String name = formal.getName();
            if (!name.startsWith(DefDeclaration.GENERIC_PREFIX)) {
                // We're hit the end of the generic args!
                break;
            }
            count++;
        }
        return count;
    }

    private void addGenericToArgList(
          String formalName,
          Type generic,
          List<IExpr> args,
          GenContext ctx
    ) {
        
        String genericName = formalName
            .substring(DefDeclaration.GENERIC_PREFIX.length());

        ValueType vt = generic.getILType(ctx);
        args.add(
            new wyvern.target.corewyvernIL.expression.New(
                new TypeDeclaration(genericName, vt, this.location)
            )
        );
    }

    private void generateILForTuples(
            List<FormalArg> formals,
            List<IExpr> args, 
            GenContext ctx,
            List<TypedModuleSpec> dependencies
    ) {
        
        ExpressionAST[] rawArgs = ((TupleObject) this.argument).getObjects();
        if (formals.size() != rawArgs.length + args.size()) {
            ToolError.reportError(
                ErrorMessage.WRONG_NUMBER_OF_ARGUMENTS,
                this,
                "" + formals.size()
            );
        }
        for (int i = 0; i < rawArgs.length; i++) {
            ValueType expectedArgType = formals.get(i + this.generics.size()).getType();
            ExpressionAST ast = rawArgs[i];
            // TODO: propagate types downward from formals
            args.add(ast.generateIL(ctx, expectedArgType, dependencies));
        }
    }

    private void generateGenericArgs(
        String methodName,
        List<IExpr> args,
        List<FormalArg> formals,
        GenContext ctx,
        DefDeclType ddt,
        List<TypedModuleSpec> deps
    ) {
        int count = countFormalGenerics(formals);
        if (count < this.generics.size()) {
            // then the number of actual generics is greater than the number of formal generics
            // this is not permitted.
            ToolError.reportError(ErrorMessage.EXTRA_GENERICS_AT_CALL_SITE, this);
        } else if (count == this.generics.size()) {
            // then we can simply add each of the actual generics to the argument's list
            for (int i = 0; i < count; i++) {
                String formalName = formals.get(i).getName();
                Type generic = this.generics.get(i);
                addGenericToArgList(formalName, generic, args, ctx);    
            }
        } else {
            // this case executes when count > this.generics.size()
            // In this case, we can do type inference to determine what types have been elided
            inferGenericArgs(methodName, args, formals, ctx, ddt, deps);
        }
    }

    private void inferGenericArgs(
            String methodName,
            List<IExpr> args,
            List<FormalArg> formals,
            GenContext ctx,
            DefDeclType ddt,
            List<TypedModuleSpec> deps
    ) {
        // First, add any of the pre-existing generics to the argument list.
        addExistingGenerics(args, formals, ctx);

        // Now, try to infer the type of the remaining generics.

        // Collect the mapping from generic args to provided args
        Map<Integer, List<Integer>> inferenceMap = ddt.genericMapping();
        int count = countFormalGenerics(formals);

        for (int i = this.generics.size(); i < count; i++) {

            if (!inferenceMap.containsKey(i)) {
                // then we can't infer the type
                ToolError.reportError(ErrorMessage.MISSING_GENERICS_AT_CALL_SITE, this, methodName);
            }

            List<Integer> positions = inferenceMap.get(i);
            if (positions.isEmpty()) {
                ToolError.reportError(ErrorMessage.CANNOT_INFER_GENERIC, this);
            }
            // formal position tells you where in
            // the formals the argument that uses the generic is
            int formalPos = positions.get(0);

            // actual position tells you where in the actual
            // argument list the type should be
            int actualPos = formalPos - count;

            if (this.argument instanceof TupleObject) {
                ExpressionAST[] rawArgs = ((TupleObject) this.argument).getObjects();
                IExpr inferArg = rawArgs[actualPos].generateIL(ctx, null, deps);
                this.addInferredType(args, formals, ctx, inferArg.typeCheck(ctx, null), i);
            } else if (this.argument instanceof UnitVal) {
                // The arg is a unit value. We must be inferring from the result type
                throw new UnsupportedOperationException(
                    "Can't infer from the result type.");
            } else {
            
                // Then the arg must be a single element
                if (actualPos != 0) {
                    // Inferring from a formal arg that doesn't exist
                    throw new UnsupportedOperationException(
                        "Can't infer from the result type.");
                }

                // Now we know that the argument is the inferrable type.
                final IExpr argIL = this.argument.generateIL(ctx, null, deps);
                ValueType inferredType = argIL.typeCheck(ctx, null);
                this.addInferredType(args, formals, ctx, inferredType, i);
            }
        }
    }

    private void addInferredType(
            List<IExpr> args,
            List<FormalArg> formals,
            GenContext ctx,
            ValueType inferredType,
            int formalIndex
    ) {
        List<Declaration> members = new LinkedList<>();
        TypeDeclaration typeMember = new TypeDeclaration(
            formals.get(formalIndex).getName()
                .substring(
                    DefDeclaration.GENERIC_PREFIX.length()),
                inferredType,
                null
        );
        members.add(typeMember);
        List<DeclType> declTypes = new LinkedList<DeclType>();
        declTypes.add(
            new ConcreteTypeMember(
                formals.get(formalIndex).getName()
                    .substring(DefDeclaration.GENERIC_PREFIX.length()),
                inferredType)
        );
        ValueType actualArgType = new StructuralType("self", declTypes);
        Expression newExp = new New(members, "self", actualArgType, null);
        args.add(newExp);
    }

    private void addExistingGenerics(
            List<IExpr> args,
            List<FormalArg> formals,
            GenContext ctx
    ) {
        for (int i = 0; i < this.generics.size(); i++) {
            String formalName = formals.get(i).getName();
            Type generic = this.generics.get(i);
            addGenericToArgList(formalName, generic, args, ctx);    
        }
    }

    @Override
    public StringBuilder prettyPrint() {
        StringBuilder sb = new StringBuilder();
        sb.append("Application(function=");
        sb.append(function.prettyPrint());
        sb.append(", argument=");
        sb.append(argument.prettyPrint());
        sb.append(")");
        return sb;
    }
}
