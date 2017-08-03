package wyvern.target.corewyvernIL.decl;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import wyvern.target.corewyvernIL.FormalArg;
import wyvern.target.corewyvernIL.astvisitor.ASTVisitor;
import wyvern.target.corewyvernIL.decltype.DeclType;
import wyvern.target.corewyvernIL.decltype.DefDeclType;
import wyvern.target.corewyvernIL.decltype.EffectDeclType;
import wyvern.target.corewyvernIL.expression.Effect;
import wyvern.target.corewyvernIL.expression.EffectAccumulator;
import wyvern.target.corewyvernIL.expression.IExpr;
import wyvern.target.corewyvernIL.expression.MethodCall;
import wyvern.target.corewyvernIL.expression.Variable;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;

public class DefDeclaration extends NamedDeclaration {
	private List<FormalArg> formalArgs;
	private ValueType type;
	private IExpr body;
	private boolean hasResource = false;
	private Set<Effect> effectSet;

	public DefDeclaration(String methodName, List<FormalArg> formalArgs,
			ValueType type, IExpr iExpr, FileLocation loc) {
		this(methodName, formalArgs, type, iExpr, loc, null);
	}
	
	public DefDeclaration(String methodName, List<FormalArg> formalArgs,
			ValueType type, IExpr iExpr, FileLocation loc, Set<Effect> effects) {
		super(methodName, loc);
		this.formalArgs = formalArgs;
		if (type == null) throw new RuntimeException();
		this.type = type;
		this.body = iExpr;
		this.effectSet = effects;
	}

	@Override
	public boolean containsResource(TypeContext ctx) {
		return this.hasResource;
	}

	private void setHasResource(boolean hasResource) {
		this.hasResource = hasResource;
	}

	@Override
	public void doPrettyPrint(Appendable dest, String indent) throws IOException {
		dest.append(indent).append("def ").append(getName()).append('(');
		boolean first = true;
		for (FormalArg arg: formalArgs) {
			if (first)
				first = false;
			else
				dest.append(", ");
			arg.doPrettyPrint(dest, indent);
		}
		String newIndent = indent+"    ";
		dest.append(") : ");
		type.doPrettyPrint(dest, newIndent);
		dest.append('\n').append(newIndent);
		body.doPrettyPrint(dest,newIndent);
		dest.append('\n');
	}

	/*@Override
	public String toString() {
		return "DefDeclaration[" + getName() + "(...) : " + type + " = " + body + "]";
	}*/

	public List<FormalArg> getFormalArgs() {
		return formalArgs;
	}

	public ValueType getType() {
		return type;
	}

	public IExpr getBody() {
		return body;
	}
	
	public Set<Effect> getEffectSet() {
		return effectSet;
	}

	@Override
	public <S, T> T acceptVisitor(ASTVisitor <S, T> emitILVisitor,
			S state) {
		return emitILVisitor.visit(state, this);
	}

	@Override
	public DeclType typeCheck(TypeContext ctx, TypeContext thisCtx) {
		TypeContext methodCtx = thisCtx;
		for (FormalArg arg : formalArgs) {
			methodCtx = methodCtx.extend(arg.getName(), arg.getType());
		}
		
//		if (getName().equals("var_17")) 
//			System.out.println("here");
		if (!this.containsResource(methodCtx)) {
			for (String freeVar : this.getFreeVariables()) {
				ValueType t = (new Variable(freeVar)).typeCheck(methodCtx, null);
				if (t != null && t.isResource(methodCtx)) {
					this.setHasResource(true);
					break;
				}
			}
		}
		
//		if (getName().equals("processData")) 
//			System.out.println("processData");
		// There are problems with passing a simple Set to collect the effects
		// because Java passes arguments by values
		EffectAccumulator methodCallsEffects = new EffectAccumulator(null);
//		ValueType bodyType = body.typeCheck(methodCtx);
		ValueType bodyType = body.typeCheck(methodCtx, methodCallsEffects);

		if (getName().equals("processData") && methodCallsEffects.getEffectSet().size() != 0)
			System.out.println(methodCallsEffects.getEffectSet()+" vs. "+effectSet); // for testing accumulator
		
		if (!bodyType.isSubtypeOf(getType(), methodCtx)) {
			// for debugging
			ValueType resultType = getType();
			bodyType.isSubtypeOf(resultType, methodCtx);
			ToolError.reportError(ErrorMessage.NOT_SUBTYPE, this, "method body's type", "declared type");
			
		}
		return new DefDeclType(getName(), type, formalArgs, methodCallsEffects.getEffectSet());
	}

	@Override
	public Set<String> getFreeVariables() {
		// Get all free variables in the body of the method.
		Set<String> freeVars = body.getFreeVariables();
		
		// Remove variables that became bound in this method's scope.
		for (FormalArg farg : formalArgs) {
			freeVars.remove(farg.getName());
		}
		return freeVars;
	}
	
	@Override
	public DeclType getDeclType() {
		return new DefDeclType(getName(), type, formalArgs, effectSet);
	}

	@Override
	public ValueType typeCheck(TypeContext ctx, EffectAccumulator effectAccumulator) {
		// TODO Auto-generated method stub
		return null;
	}
}
