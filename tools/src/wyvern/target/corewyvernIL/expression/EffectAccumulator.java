package wyvern.target.corewyvernIL.expression;

import java.util.HashSet;
import java.util.Set;

public class EffectAccumulator {
	private Set<Effect> effectSet;
	
	public EffectAccumulator(Set<Effect> effectSet) { // hmm...
		this.effectSet = effectSet;
	}
	
	public void initializeEffectSet() {
		if (effectSet == null) { effectSet = new HashSet<Effect>(); }
	}
	
	public void addEffects(Set<Effect> effects) {
		initializeEffectSet();
		this.effectSet.addAll(effects);
	}
	
	public Set<Effect> getEffectSet() {
		return effectSet;
	}
}