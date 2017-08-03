package wyvern.target.corewyvernIL.decltype;

import wyvern.target.corewyvernIL.IASTNode;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import wyvern.target.corewyvernIL.astvisitor.ASTVisitor;
import wyvern.target.corewyvernIL.effects.Effect;
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

	@Override
	public boolean isSubtypeOf(DeclType dt, TypeContext ctx) { 
		if (!(dt instanceof EffectDeclType)) {
			return false;
		}
		EffectDeclType edt = (EffectDeclType) dt;
		
		/* edt == effect declared (and possibly defined in the type),
		 * this == effect declared and defined in the module def.
		 * If effect undefined in the type, anything defined in the module
		 * def works; if defined in the type, then the effect in the module
		 * def can only be defined using a subset of the definition in the type.
		 */
		if (edt.getEffectSet()!=null) { 
			if (!edt.getEffectSet().containsAll(getEffectSet())) 
				return false; // effect E = S ("this") <: effect E = S' (edt)	if S <= S' (both are concrete)	
		}
		return true; // if edt.getEffectSet()==null (i.e. undefined in the type), anything is a subtype
		// i.e. effect E = {} (concrete "this") <: effect E (abstract dt which is undefined)
		
//			if (getEffectSet()!=null) {
//				return false;
//			} else {
//				return true; // need to shorten logic
//			}
//		}
//		if (getEffectSet()==null) // equal 
//			throw new RuntimeException();
//		throw new RuntimeException("nope");
//		if (edt.getEffectSet()==null) {
//			return true;
//		} else {
//			if (edt.getEffectSet().containsAll(getEffectSet())) { // allow module def to define more effects in the set?
//				return true;
//			}
//		}
////		if (getEffectSet().containsAll(edt.getEffectSet())) 
////			return true; // different from: effect E = S <: effect E = S'	if S <= S' (both are concrete)
//		return false;
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
//		return new EffectDeclType(getName(), this.getRawResultType().adapt(v));
		return new EffectDeclType(getName(), getEffectSet(), getLocation());
	}

	@Override
	public DeclType doAvoid(String varName, TypeContext ctx, int count) {
//		ValueType t = this.getRawResultType().doAvoid(varName, ctx, count);
//		if (t.equals(this.getRawResultType())) {
//			return this;
//		} else {
//			
//		}
		return new EffectDeclType(getName(), getEffectSet(), getLocation());
	}

	@Override
	public boolean isTypeDecl() {
		return false;
	}
	
}