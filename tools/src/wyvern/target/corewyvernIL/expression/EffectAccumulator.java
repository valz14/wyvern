package wyvern.target.corewyvernIL.expression;

import java.util.HashSet;
import java.util.Set;

public class EffectAccumulator {
	private Set<Effect> effectSet;
	
	public EffectAccumulator(Set<Effect> effectSet) {
		this.effectSet = effectSet;
	}
	
	public void addEffects(Set<Effect> effects) {
		if (this.effectSet==null) {
			this.effectSet = new HashSet<Effect>();
		}
		this.effectSet.addAll(effects);
	}
	
	public Set<Effect> getEffectSet() {
		return effectSet;
	}
}