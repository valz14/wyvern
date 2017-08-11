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
import wyvern.target.corewyvernIL.effects.Effect;
import wyvern.target.corewyvernIL.effects.EffectAccumulator;
import wyvern.target.corewyvernIL.expression.IExpr;
import wyvern.target.corewyvernIL.expression.Path;
import wyvern.target.corewyvernIL.expression.Variable;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.support.VarBindingContext;
import wyvern.target.corewyvernIL.type.NominalType;
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
		if (effectSet != null) {
			dest.append("{");
			effectSet.stream().forEach(e -> {
				try {
					dest.append(e.toString());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			});
//			dest.append(effectSet.toString()); // [] instead of {}, hopefully won't be too confusing
			dest.append("} ");
		}
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
		if (!this.containsResource(methodCtx)) {
			for (String freeVar : this.getFreeVariables()) {
				ValueType t = (new Variable(freeVar)).typeCheck(methodCtx, null);
				if (t != null && t.isResource(methodCtx)) {
					this.setHasResource(true);
					break;
				}
			}
		}
		
		// if the method makes no claim about the effects it has, do not check its calls for effects
		EffectAccumulator effectAccumulator = (effectSet==null) ? null : new EffectAccumulator();
//		if (getName().equals("processData")) { // remove
//			System.out.println("here--IL.DefDecl");
//		}
		ValueType bodyType = body.typeCheck(methodCtx, effectAccumulator);
		
		if ((methodCtx instanceof GenContext) && (effectSet != null)) { // hack to avoid missing info in context 
			GenContext methodGenCtx = (GenContext) methodCtx; // hack
			effectSet.stream().forEach(e -> e.addPath(methodGenCtx)); // ctx also work here for some reason
			effectAccumulator.getEffectSet().stream().forEach(e -> e.addPath(methodGenCtx)); // need null check too?

			if ((getEffectSet() != null) && (effectAccumulator != null)) {
				Set<Effect> methodCallsE = recursiveEffectCheck(ctx, effectAccumulator.getEffectSet());
				Set<Effect> annotatedE = recursiveEffectCheck(ctx, getEffectSet());
//				System.out.println(getName()+": "+effectAccumulator.toString()+" vs. "+getEffectSet().toString());
				System.out.println(getName()+": "+methodCallsE.toString()+" vs. "+annotatedE.toString());
			}
		}	
		
		if (!bodyType.isSubtypeOf(getType(), methodCtx)) {
			// for debugging
			ValueType resultType = getType();
			bodyType.isSubtypeOf(resultType, methodCtx);
			ToolError.reportError(ErrorMessage.NOT_SUBTYPE, this, "method body's type", "declared type");;
			
		}
		return new DefDeclType(getName(), type, formalArgs, getEffectSet());
	}
	
	public Set<Effect> recursiveEffectCheck(TypeContext ctx, Set<Effect> effects) {
		Set<Effect> allEffects =  new HashSet<Effect>();
		for (Effect e : effects) { // would it be more efficient to do a !e.effectsCheck.isEmpty() here?
			Set<Effect> moreEffects = e.effectsCheck(ctx); // effectCheck() returns the effectSet defined by EffectDeclType
			if (moreEffects != null) {
				allEffects.addAll(moreEffects);
			}
		}
		if (!allEffects.isEmpty()) { // need to be changed when built-in, base-level effects are implemented
			allEffects = recursiveEffectCheck(ctx, allEffects);
		}
		return allEffects;
	}

	public Set<Effect> getEffectSet() {
		return effectSet;
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
		return new DefDeclType(getName(), type, formalArgs, getEffectSet());
	}
}
