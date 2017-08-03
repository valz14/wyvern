package wyvern.target.corewyvernIL.decltype;

import wyvern.target.corewyvernIL.IASTNode;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import wyvern.target.corewyvernIL.astvisitor.ASTVisitor;
import wyvern.target.corewyvernIL.expression.Effect;
import wyvern.target.corewyvernIL.expression.EffectAccumulator;
import wyvern.target.corewyvernIL.expression.Variable;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.support.View;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.FileLocation;


public class EffectDeclType extends DeclType implements IASTNode {
	private Set<Effect> effectSet;
	private FileLocation loc;
	
	public EffectDeclType(String name, Set<Effect> effectSet, FileLocation loc) {
		super(name);
		this.effectSet = effectSet;
		this.loc = loc;
	}
	
	@Override
	public <S, T> T acceptVisitor(ASTVisitor<S, T> visitor, S state) {
		return null; //visitor.visit(state, this);
	}
	
	/* check to see if all elements of effects are in effectSet */
	public boolean containsAll(Set<Effect> effects) { // replace HashSet.containsAll()
		for (Effect e1 : effects) { // contains() doesn't work either...
			boolean e1InEffectSet = false;
			for (Effect e2 : effectSet) {
				if (e1.equals(e2)){
					e1InEffectSet = true;
					break;
				};
			}
			if (!e1InEffectSet) return false;
		}
		return true;
	}

	@Override
	public boolean isSubtypeOf(DeclType dt, TypeContext ctx) { 
		if (!(dt instanceof EffectDeclType)) {
			return false;
		}
		EffectDeclType edt = (EffectDeclType) dt;
		
		// candidateDT.isSubtypeOf(dt, extendedCtx))
		// candidateDT: effect process = [net.receive, var_17.send], var_17 just have effect send
		// dt: effect process = [var_26.send, net.receive], var_26 is basically the entire type ("this", only in the extendedContext which is here)
		// var_17.send == var_26.send
		// get var_26 from the context, get its send from the EffectDeclType "effect send"
		// get var_17 from the context, get its send from its EffectDeclType,
		// make sure that candidateDT/var_17 is subtype/subset of the other
		// candidateDT == getEffectSet()
		
		/* edt == effect declared (and possibly defined) in the type,
		 * this == effect declared and defined in the module def.
		 * If effect undefined in the type, anything defined in the module
		 * def works; if defined in the type, then the effect in the module
		 * def can only be defined using a subset of the definition in the type.
		 */
		if (edt.getEffectSet()!=null) { // HashSet.containsAll() has abnormal behavior here
			if (!edt.containsAll(getEffectSet())) 
				return false; // effect E = S ("this") <: effect E = S' (edt)	if S <= S' (both are concrete)	
		}
		return true; // if edt.getEffectSet()==null (i.e. undefined in the type), anything is a subtype
		// i.e. effect E = {} (concrete "this") <: effect E (abstract dt which is undefined)
	}

	public Set<Effect> getEffectSet() {
		return effectSet;
	}
	
	@Override
	public void checkWellFormed(TypeContext ctx) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EffectDeclType other = (EffectDeclType) obj;
		if (getName() == null) {
			if (other.getName() != null)
				return false;
		} else if (!getName().equals(other.getName()))
			return false;
		if (getEffectSet() == null) {
			if (other.getEffectSet() != null)
				return false;
		} else if (!getEffectSet().equals(other.getEffectSet()) ||
				!getLocation().equals(other.getLocation())) { //||
//				!getPath().equals(other.getPath())) {
			return false;
		}
		return true;
	}

	@Override
	public void doPrettyPrint(Appendable dest, String indent) throws IOException {
		dest.append(indent).append("effect ").append(getName()).append(" = ");
		if (effectSet != null) {
			dest.append(effectSet.toString());
//			String result = "{";
//			for (Effect e : effectSet) {
//				result += e.toString()+", "; // fix later
//			}
//			result += "}";
//			dest.append(result);
		}
		dest.append('\n');
	}

	@Override
	public DeclType adapt(View v) {
		Set<Effect> adaptedEffectSet = null; 
		if (effectSet != null) {
			adaptedEffectSet = new HashSet<Effect>();
			for (Effect e : effectSet) {
				adaptedEffectSet.add(new Effect(((Variable)(e.adapt(v))), e.getName(), e.getLocation()));
			}
		}
//		return new EffectDeclType(getName(), this.getRawResultType().adapt(v));
		return new EffectDeclType(getName(), adaptedEffectSet, getLocation());
	}

	@Override
	public DeclType doAvoid(String varName, TypeContext ctx, int count) {
//		ValueType t = this.getRawResultType().doAvoid(varName, ctx, count);
//		if (t.equals(this.getRawResultType())) {
//			return this;
//		} else {
//			
//		}
		if (varName.equals("var_26"))
			System.out.println("var_26");
		ValueType vt = ctx.lookupTypeOf(varName);
		DeclType dt = vt.findDecl(getName(), ctx);
		if ((dt==null) || (!(dt instanceof EffectDeclType)))
			return new EffectDeclType(getName(), getEffectSet(), getLocation());
		EffectDeclType edt = (EffectDeclType) dt;
		return new EffectDeclType(getName(), edt.getEffectSet(), getLocation());
	}
	
	@Override
	public boolean isTypeDecl() {
		return false;
	}

	@Override
	public ValueType typeCheck(TypeContext ctx, EffectAccumulator effectAccumulator) {
		// TODO Auto-generated method stub
		return null;
	}
	
}