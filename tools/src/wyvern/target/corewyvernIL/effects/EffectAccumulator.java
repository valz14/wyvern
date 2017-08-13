package wyvern.target.corewyvernIL.effects;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/* TODO: Use in corewyvernIL.decl.DefDeclaration.typecheck() to
 * collect effects of all method calls in a method.
 */
public class EffectAccumulator {
	private Set<Effect> effectSet;
	
	public EffectAccumulator() {
		effectSet = new HashSet<Effect>(); // start off empty (never null)
	}	
	
	public void addEffect(Effect e) {
		effectSet.add(e);
	}
	
	public void addEffects(Set<Effect> effects) {
		effectSet.addAll(effects);
	}
	
	public Set<Effect> getEffectSet() {
		return effectSet;
	}
	
	@Override
	public String toString() {
		String s = effectSet.toString(); 
		return s.replace("[", "{").replace("]", "}");
	}
	
//	@Override
//	public void doPrettyPrint(Appendable dest, String indent) throws IOException {
//		dest.append(indent).append("effect ").append(getName()).append(" = ");
//		if (effectSet != null) {
//			dest.append("{");
//			effectSet.stream().forEach(e -> {
//				try {
//					dest.append(e.toString());
//				} catch (IOException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
//			});
//			dest.append("} ");
//		}
//		dest.append('\n');
//	}
}