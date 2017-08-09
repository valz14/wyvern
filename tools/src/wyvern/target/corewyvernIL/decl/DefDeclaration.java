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
		
		EffectAccumulator effectAccumulator = new EffectAccumulator(null);
		ValueType bodyType = body.typeCheck(methodCtx, effectAccumulator);
//		if (getName().equals("processData")) {
//			if (getEffectSet() != null) {
//				System.out.println(effectAccumulator.toString()+"vs."+getEffectSet().toString());
//			} else {
//				System.out.println(effectAccumulator.toString()+"vs."+"null");
//			}
//		}
		
		/* If null, then the method did not claim to have any set (or lack) of effects, so it is allowed 
		 * to have any set of effects from its method calls. If it isn't null, then its annotated effect
		 * set must be checked against the set from its method calls.
		 * 
		 * TODO: This is a problem for stdio method calls that don't specify effects... and require looking
		 * more into the effect accumulation code in MethodCall.typecheck()
		 */
//		if (effectSet !=  null) { 
//			if (effectAccumulator.getEffectSet()==null) throw new RuntimeException("Method with effect annotations attempted to call methods without."); // need to report error
//			
//			Set<Effect> annotatedEffects = new HashSet<Effect>();
//			Set<Effect> actualEffects = new HashSet<Effect>();
//			for (Effect e : effectSet) {
//				ValueType vt = null;
//				
//				// Without try/catch, this could result in a runtime exception due to EmptyGenContext 
//				// (which doesn't have FileLocation or HasLocation to call ToolError.reportError())
//				try {  
//					vt = e.getPath().typeCheck(ctx, null); // due to addPath() in generateDecl() in typedAST, e.getPath() will never be null
//				} catch (RuntimeException ex) { 
//					// also for a recursive effect declaration (ex. effect process = {process}), variable name would be "var_##"
//					// (could use regex to distinguish the two? May mistake a variable that is really named var_## though)
//					ToolError.reportError(ErrorMessage.VARIABLE_NOT_DECLARED, this, e.getPath().getName()); 
//				}
//				
//				String eName = e.getName(); // "read"
//				DeclType eDT = vt.findDecl(eName, ctx); // the effect definition as appeared in the type (ex. "effect receive = ")
//				if ((eDT==null) || (!(eDT instanceof EffectDeclType))){
//					ToolError.reportError(ErrorMessage.EFFECT_OF_VAR_NOT_FOUND, this, eName, e.getPath().getName());
//				annotatedEffects.add(e)
//				}
//			}
//		}
		
		if (!bodyType.isSubtypeOf(getType(), methodCtx)) {
			// for debugging
			ValueType resultType = getType();
			bodyType.isSubtypeOf(resultType, methodCtx);
			ToolError.reportError(ErrorMessage.NOT_SUBTYPE, this, "method body's type", "declared type");;
			
		}
		return new DefDeclType(getName(), type, formalArgs, getEffectSet());
	}
	
//	public [] recursiveEffectCheck(TypeContext ctx, TypeContext thisCtx, Set<Effect> effects) {
//		if (effects != null) {
//			for (Effect e : effects) {
//				e.effectCheck(ctx);
//			}
//		}
//	}

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
