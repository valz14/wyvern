package wyvern.target.corewyvernIL.decl;

import java.io.IOException;
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
import wyvern.target.corewyvernIL.expression.Variable;
import wyvern.target.corewyvernIL.support.GenContext;
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
		if (effectSet != null) {effectSet.toString().replace("[", "{").replace("]", "}");	}
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
		if (!this.containsResource(methodCtx)) {
			for (String freeVar : this.getFreeVariables()) {
				ValueType t = (new Variable(freeVar)).typeCheck(methodCtx, null);
				if (t != null && t.isResource(methodCtx)) {
					this.setHasResource(true);
					break;
				}
			}
		}
		
		// if the method makes no claim about the effects it has, do not check its calls for effects (i.e. null)
		EffectAccumulator effectAccumulator = (effectSet==null) ? null : new EffectAccumulator();
		
		ValueType bodyType = body.typeCheck(methodCtx, effectAccumulator);
		
		/* If in the process of generating code, and the method has effect annotations, check its effects
		 * (A hack to avoid missing info in certain stages of context) */
		if ((methodCtx instanceof GenContext) && (effectSet != null)) { 
			GenContext methodGenCtx = (GenContext) methodCtx; // hack
			Set<Effect> actualEffectSet = effectAccumulator.getEffectSet();
			
			/* fill in empty paths for effects (necessary to place here, instead of 
			 * in typedAST.DefDeclaration.generateDecl() for obj defn) */
			effectSet.stream().forEach(e -> e.addPath(methodGenCtx));
//			actualEffectSet.stream().forEach(e -> e.addPath(methodGenCtx)); // not necessary
		
			EffectDeclType actualEffects = new EffectDeclType(getName()+" actualEffects", effectSet, getLocation());
			EffectDeclType annotatedEffects = new EffectDeclType(getName()+" annotatedEffects", actualEffectSet, getLocation());
			if (!actualEffects.isSubtypeOf(annotatedEffects, ctx)) {
				ToolError.reportError(ErrorMessage.NOT_SUBTYPE, getLocation(), 
						"set of effects from the method calls ("+effectSet.toString().replace("[", "{").replace("]", "}")+")",
						"set of effects specified by "+getName()+"("+actualEffectSet.toString().replace("[", "{").replace("]", "}")+")");
			}
		}	
		
		if (!bodyType.isSubtypeOf(getType(), methodCtx)) {
			// for debugging
			ValueType resultType = getType();
			bodyType.isSubtypeOf(resultType, methodCtx);
			ToolError.reportError(ErrorMessage.NOT_SUBTYPE, this, "method body's type", "declared type");;
			
		}
		return new DefDeclType(getName(), type, formalArgs, effectSet);
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
