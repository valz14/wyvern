package wyvern.target.corewyvernIL.effects;

import java.util.HashSet;
import java.util.Set;

/* TODO: Use in corewyvernIL.decl.DefDeclaration.typecheck() to
 * collect effects of all method calls in a method.
 */
public class EffectAccumulator {
	private Set<Effect> effectSet;
	
	public EffectAccumulator() {
		effectSet = new HashSet<Effect>(); // start off empty
	}
	
//	public void initializeSet() {
//		if (effectSet==null) {
//			effectSet = new HashSet<Effect>();
//		}
//	}
	
	
	public void addEffect(Effect e) {
//		initializeSet();
		effectSet.add(e);
	}
	
	public void addEffects(Set<Effect> effects) {
//		initializeSet();
		effectSet.addAll(effects);
	}
	
	public Set<Effect> getEffectSet() {
		return effectSet;
	}
	
	@Override
	public String toString() {
		return effectSet.toString();
	}
	
//	@Override
//	public void doPrettyPrint(Appendable dest, String indent) throws IOException {
//		dest.append(indent).append("effect ").append(getName()).append(" = ");
//		if (effectSet != null)
//			dest.append(effectSet.toString());
//		dest.append('\n');
//	}
}