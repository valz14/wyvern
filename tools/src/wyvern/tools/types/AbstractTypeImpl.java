package wyvern.tools.types;

import wyvern.tools.typedAST.transformers.Types.TypeTransformer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractTypeImpl implements Type {
	@Override
	public Optional<Environment> subtype(Type other, Environment input) {
		return this.subtype(other, input, new HashSet<>());
	}

	@Override
	public Optional<Environment> subtype(Type other, Environment input, HashSet<SubtypeRelation> subtypes) {
		// S-Refl
		if (this.equals(other)) {
			return Optional.of(input);
		}

		// S-Assumption
		if (subtypes.contains(new SubtypeRelation(this, other))) {
			return Optional.of(input);
		}

		// S-Trans
		HashSet<Type> t2s = new HashSet<Type>();
		for (SubtypeRelation sr : subtypes) {
			if (sr.getSubtype().equals(this)) {
				t2s.add(sr.getSupertype());
			}
		}
		for (Type t : t2s) {
			if (subtypes.contains(new SubtypeRelation(t, other))) {
				return Optional.of(input);
			}
		}

		return Optional.empty();
	}
	
	public boolean isSimple() {
		return true; // default is correct for most types
	}
	@Override
	public Map<String, Type> getChildren() {
		return new HashMap<>();
	}

	@Override
	public Type cloneWithChildren(Map<String, Type> newChildren, TypeTransformer transformer) {
		return this;
	}
}