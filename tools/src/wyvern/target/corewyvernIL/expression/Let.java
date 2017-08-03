package wyvern.target.corewyvernIL.expression;

import static wyvern.tools.errors.ToolError.reportError;

import java.io.IOException;
import java.util.Set;

import wyvern.target.corewyvernIL.VarBinding;
import wyvern.target.corewyvernIL.astvisitor.ASTVisitor;
import wyvern.target.corewyvernIL.effects.EffectAccumulator;
import wyvern.target.corewyvernIL.support.EvalContext;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.ErrorMessage;

public class Let extends Expression {
	private VarBinding binding;
	private IExpr inExpr;

	public Let(String varName, ValueType type, IExpr toReplace, IExpr inExpr) {
		this(new VarBinding(varName, type, toReplace), inExpr);
	}

	public Let(VarBinding binding, IExpr inExpr) {
		super();
		this.binding = binding;
    if (this.getVarType() == null)
        throw new RuntimeException("Let created with null variable type");
		if (inExpr == null) throw new RuntimeException();
		this.inExpr = inExpr;
	}

	public String getVarName() {
		return binding.getVarName();
	}

    public ValueType getVarType() {
        return binding.getType();
    }

	public IExpr getToReplace() {
		return binding.getExpression();
	}

	public Expression getInExpr() {
		return (Expression) inExpr;
	}

	@Override
	public ValueType typeCheck(TypeContext ctx, EffectAccumulator effectAccumulator) {
		ValueType t = getToReplace().typeCheck(ctx, effectAccumulator);
		if (!t.isSubtypeOf(binding.getType(), ctx)) {
			ValueType q = binding.getType();
			t.isSubtypeOf(q, ctx);
			reportError(ErrorMessage.NOT_SUBTYPE, this, t.toString(), binding.getType().toString());
		}
		final TypeContext extendedCtx = ctx.extend(getVarName(), binding.getType());
		final ValueType exprType = inExpr.typeCheck(extendedCtx, effectAccumulator);
		final ValueType cleanExprType = exprType.avoid(binding.getVarName(), extendedCtx);
		//cleanExprType.checkWellFormed(ctx);
		this.setExprType(cleanExprType);
		return getExprType();
	}

	@Override
	public void doPrettyPrint(Appendable dest, String indent) throws IOException {
		String newIndent = indent + "    ";
		dest.append("let\n");
		
		binding.doPrettyPrint(dest, newIndent);
		
		dest.append(indent).append("in ");
		inExpr.doPrettyPrint(dest,indent);
	}

	@Override
	public <S, T> T acceptVisitor(ASTVisitor <S, T> emitILVisitor,
			S state) {
		return emitILVisitor.visit(state, this);
	}

	@Override
	public Value interpret(EvalContext ctx) {
		Value v = getToReplace().interpret(ctx);
		return inExpr.interpret(ctx.extend(getVarName(), v));
	}

	@Override
	public Set<String> getFreeVariables() {
		// Get free variables in the sub-expressions.
		Set<String> freeVars = inExpr.getFreeVariables();
		// Remove the name that just became bound.
		freeVars.remove(getVarName());
		freeVars.addAll(getToReplace().getFreeVariables());
		return freeVars;
	}
}
