package wyvern.tools.typedAST.extensions.interop.java.types;

import wyvern.tools.typedAST.transformers.Types.TypeTransformer;
import wyvern.tools.types.Environment;
import wyvern.tools.types.SubtypeRelation;
import wyvern.tools.types.Type;
import wyvern.tools.util.TreeWriter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

public class JNullType implements Type {
	@Override
	public Optional<Environment> subtype(Type other, Environment input, HashSet<SubtypeRelation> subtypes) {
		return (other instanceof JavaClassType)?Optional.of(input):Optional.<Environment>empty();
	}

	@Override
	public Optional<Environment> subtype(Type other, Environment input) {
		return (other instanceof JavaClassType)?Optional.of(input):Optional.<Environment>empty();
	}
	@Override
	public boolean isSimple() {
		return true;
	}

	@Override
	public Map<String, Type> getChildren() {
		return new HashMap<>();
	}

	@Override
	public Type cloneWithChildren(Map<String, Type> newChildren, TypeTransformer transformer) {
		return this;
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {
	}
}
