/**
 * @author vzhao
 */
package wyvern.target.corewyvernIL.effects;

import java.util.HashSet;
import java.util.Set;

import wyvern.target.corewyvernIL.astvisitor.ASTVisitor;
import wyvern.target.corewyvernIL.decltype.DeclType;
import wyvern.target.corewyvernIL.decltype.EffectDeclType;
import wyvern.target.corewyvernIL.expression.Path;
import wyvern.target.corewyvernIL.expression.Variable;
import wyvern.target.corewyvernIL.support.EvalContext;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.support.View;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;

public class Effect {
	private Path path;
	private String name;
	private FileLocation loc;
	
	public Effect(Variable p, String n, FileLocation l) {
		path = p;
		name = n;
		loc = l;
	}

	public Variable getPath() {
		return (Variable) path;
	}
	
	public void setPath(Path p) { // for effects defined in the same signature (whose paths are null until typechecked)
		path = p;
	}
	
	public String getName() {
		return name;
	}
	
	public FileLocation getLocation() {
		return loc;
	}
	
	public DeclType getDeclType(Set<Effect> effects) {
		return new EffectDeclType(getName(), effects, getLocation());
	}
	
	@Override
	public String toString() {
		return (path==null? "" : getPath().getName() + ".") + getName(); 
	}
	
	public Path adapt(View v) {
		return getPath().adapt(v);
	}	
	
	@Override
	public boolean equals(Object obj) {
		if (obj==null) return false;
		if (this == obj) return true;
	
		if (!(obj instanceof Effect)) return false;	
		
		Effect eObj = (Effect) obj;
		if (eObj.getName().equals(getName()) &&
				eObj.getPath().equals(getPath())) return true;
		return false;
	}
	
	public EffectDeclType effectCheck(TypeContext ctx) { // technically doesn't need thisCtx	
		ValueType vt = null;
		
		// Without try/catch, this could result in a runtime exception due to EmptyGenContext 
		// (which doesn't have FileLocation or HasLocation to call ToolError.reportError())
		try {  
			vt = getPath().typeCheck(ctx, null); // due to addPath() in generateDecl() in typedAST, e.getPath() will never be null
		} catch (RuntimeException ex) { 
			// also for a recursive effect declaration (ex. effect process = {process}), variable name would be "var_##"
			// (could use regex to distinguish the two? May mistake a variable that is really named var_## though)
			ToolError.reportError(ErrorMessage.VARIABLE_NOT_DECLARED, getLocation(), getPath().getName()); 
		}
		
		DeclType eDT = vt.findDecl(getName(), ctx); // the effect definition as appeared in the type (ex. "effect receive = ")
		if ((eDT==null) || (!(eDT instanceof EffectDeclType))){
			ToolError.reportError(ErrorMessage.EFFECT_OF_VAR_NOT_FOUND, getLocation(), getName(), getPath().getName());
		}
		
		return (EffectDeclType) eDT;
	}
}