/**
 * Typed AST of an effect; translates the definition of the effect 
 * from a String into a set of effects.
 * 
 * @author vzhao
 */

package wyvern.tools.typedAST.core.declarations;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import wyvern.target.corewyvernIL.decltype.DeclType;
import wyvern.target.corewyvernIL.decltype.EffectDeclType;
import wyvern.target.corewyvernIL.effects.Effect;
import wyvern.target.corewyvernIL.expression.Path;
import wyvern.target.corewyvernIL.modules.TypedModuleSpec;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.target.corewyvernIL.support.TopLevelContext;
import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;
import wyvern.tools.typedAST.abs.Declaration;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.util.EvaluationEnvironment;

public class EffectDeclaration extends Declaration {
	private String name;
	private Set<Effect> effectSet;
	private FileLocation loc;
	
	public EffectDeclaration(String name, String effects, FileLocation fileLocation) {	
		this.name = name;
		effectSet = Effect.parseEffects(name, effects, fileLocation);
		loc = fileLocation;
	}
	
	public Effect getEffect() {
		return new Effect(null, getName(), getLocation()); 
	}
	
	public void doPrettyPrint(Appendable dest, String indent) throws IOException {
		dest.append(indent).append("effect ").append(getName()).append(" = ");
		if (effectSet != null)
			dest.append(effectSet.toString());
		dest.append('\n');
	}
	
	@Override
	public FileLocation getLocation() {
		return loc;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public Set<Effect> getEffectSet() {
		return effectSet;
	}
	
	@Override
	public DeclType genILType(GenContext ctx) {
		return new EffectDeclType(getName(), getEffectSet(), getLocation());
	}
	
	@Override
	public wyvern.target.corewyvernIL.decl.Declaration generateDecl(GenContext ctx, GenContext thisContext) {
		for (Effect e : effectSet) {
			if (e.getPath() == null) { 
				// an effect that (if valid) was defined in the same type or module def 
				// (and therefore did not come with a path)
				addPath(e, ctx);
			}
		}
		return new wyvern.target.corewyvernIL.decl.EffectDeclaration(getName(), getEffectSet(), getLocation());
	}
	
	/** Add path to an effect if it doesn't already have one (i.e. if it's defined in the same type or module def). **/
	public void addPath(Effect e, GenContext ctx) {
		Path ePath = ctx.getContainerForTypeAbbrev(e.getName());
		if (ePath==null) { // effect not found
			ToolError.reportError(ErrorMessage.EFFECT_IN_SIG_NOT_FOUND, this, e.getName());
		}
		e.setPath(ePath);
	}
	
	@Override
	public wyvern.target.corewyvernIL.decl.Declaration topLevelGen(GenContext ctx, List<TypedModuleSpec> dependencies) {
		return generateDecl(ctx, ctx); // like in DefDeclaration
	}
	
	@Override
	public void addModuleDecl(TopLevelContext tlc) {
		wyvern.target.corewyvernIL.decl.Declaration decl = topLevelGen(tlc.getContext(), null);
		DeclType dt = genILType(tlc.getContext()); // tlc.getContext() isn't actually being used here...
		tlc.addModuleDecl(decl,dt);
	}
	
	
	/**** Secondary or obsolete (due to use of Environment) methods. ***/
	@Override
	public Environment extendType(Environment env, Environment against) {
		// TODO Auto-generated method stub
		throw new RuntimeException("extendType not implemented");
//		return null;
	}
	@Override
	public Environment extendName(Environment env, Environment against) {
		// TODO Auto-generated method stub
		throw new RuntimeException("extendName not implemented");
//		return null;
	}
	@Override
	public Type getType() { // effects have no parsed "type" like variables/values do
		throw new RuntimeException("extendName not implemented");
//		return null;
	}
	@Override
	public Map<String, TypedAST> getChildren() {
		// TODO Auto-generated method stub
		throw new RuntimeException("getChildren not implemented");
//		return null;
	}
	@Override
	public TypedAST cloneWithChildren(Map<String, TypedAST> newChildren) {
		// TODO Auto-generated method stub
		throw new RuntimeException("getChildren not implemented");
//		return null;
	}

	@Override
	protected Type doTypecheck(Environment env) { 
		throw new RuntimeException("doTypecheck not implemented");
//		return null;
	}
	@Override
	protected Environment doExtend(Environment old, Environment against) {
		// TODO Auto-generated method stub
		throw new RuntimeException("doExtend not implemented");
//		return null;
	}
	@Override
	public EvaluationEnvironment extendWithValue(EvaluationEnvironment old) {
		// TODO Auto-generated method stub
		throw new RuntimeException("extendWithValue not implemented");
//		return null;
	}
	@Override
	public void evalDecl(EvaluationEnvironment evalEnv, EvaluationEnvironment declEnv) {
		// TODO Auto-generated method stub
		throw new RuntimeException("evalDecl not implemented");
	}
	
}