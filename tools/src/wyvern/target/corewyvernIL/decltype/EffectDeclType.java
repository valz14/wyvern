/** @author vzhao */

package wyvern.target.corewyvernIL.decltype;

import wyvern.target.corewyvernIL.IASTNode;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import wyvern.target.corewyvernIL.astvisitor.ASTVisitor;
import wyvern.target.corewyvernIL.effects.Effect;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.support.View;
import wyvern.tools.errors.FileLocation;

/* TODO: adapt(), doAvoid() */
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
		return visitor.visit(state, this); // could return null here, though implication is unknown
	}

	/** Conduct semantics check, by decomposing the effect sets of the two
	 * (effect)DeclTypes before comparing them.
	 */
	@Override
	public boolean isSubtypeOf(DeclType dt, TypeContext ctx) { 
		// TODO: instead of the code below, implement semantics comparison
		if (!(dt instanceof EffectDeclType)) {
			return false;
		}
		EffectDeclType edt = (EffectDeclType) dt;
		
		/* edt == type or method annotations, vs. 
		 * this == module def or method calls:
		 * if edt.getEffectSet()==null: this.getEffectSet() can be anything (null or not)
		 * else: this.getEffectSet() can't be null (though can't happen in the first place), 
		 * and edt.getEffectSet().containsAll(this.getEffectSet())
		 */
		/* POTENTIAL BUG: if edt is an undefined effect in a type, but 
		 * "this" is a collection of effects from method calls of a method?
		 */
		if (edt.getEffectSet()!=null) { 
			Set<Effect> thisEffects = recursiveEffectCheck(ctx, getEffectSet());
			Set<Effect> edtEffects =  recursiveEffectCheck(ctx, edt.getEffectSet());
			if (!edtEffects.containsAll(thisEffects)) {
				return false; // "this" is not a subtype of dt, i.e. not all of its effects are covered by edt's effectSet
			} // effect E = S ("this") <: effect E = S' (edt)	if S <= S' (both are concrete)
		}
		
		/* if edt.getEffectSet()==null (i.e. undefined in the type, or no method anntations), 
		 * anything (defined in the module def, or the effeects of the method calls) is a subtype
		 */
		// i.e. effect E = {} (concrete "this") <: effect E (abstract dt which is undefined)
		return true; 
	}

	public Set<Effect> recursiveEffectCheck(TypeContext ctx, Set<Effect> effects) {
		if (effects==null) { return null; }
		
		Set<Effect> allEffects =  new HashSet<Effect>(); // collects lower-level effects from effectSets of arg "effects"
		Set<Effect> moreEffects = null; // get the effectSet belonging to an effect in arg "effects"
		for (Effect e : effects) {
			try {
				/* effectCheck() returns the effectSet defined by EffectDeclType, or reports an error
				 * if EffectDeclType for e is not found in the context */
				moreEffects = e.effectsCheck(ctx); 
			} catch (RuntimeException ex) { // seems to have reached the lowest-level effect in scope
				allEffects.add(e);
			}
			
			if (moreEffects != null) {	allEffects.addAll(moreEffects);	}
		}
		
		// if it is null, then everything in "effects" are of the lowest-level in scope
		if (moreEffects != null) { allEffects = recursiveEffectCheck(ctx, allEffects);	}
		return allEffects;
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
		if (effectSet != null)
			dest.append(effectSet.toString());
		dest.append('\n');
	}

	@Override
	public DeclType adapt(View v) {
		// TODO: the returned EffectDeclType should have, as its effect set, the set
		// of results from calling adapt(v) on each Effect in this.EffectSet
		
//		return new EffectDeclType(getName(), this.getRawResultType().adapt(v));
		return new EffectDeclType(getName(), getEffectSet(), getLocation());
	}

	@Override
	public DeclType doAvoid(String varName, TypeContext ctx, int count) {
		// TODO: similar to NominalType.doAvoid()
		return new EffectDeclType(getName(), getEffectSet(), getLocation());
	}

	@Override
	public boolean isTypeDecl() {
		return false;
	}	
}