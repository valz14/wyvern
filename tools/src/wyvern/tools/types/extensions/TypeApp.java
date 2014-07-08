package wyvern.tools.types.extensions;

import wyvern.tools.typedAST.transformers.Types.AbstractTypeTransformer;
import wyvern.tools.typedAST.transformers.Types.TypeTransformer;
import wyvern.tools.types.SubtypeRelation;
import wyvern.tools.types.Type;
import wyvern.tools.types.TypeResolver;
import wyvern.tools.util.TreeWriter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TypeApp implements Type, TypeResolver.Resolvable {
	private final Type rec;
	private final List<Type> list;

	public TypeApp(Type rec, List<Type> list) {
		this.rec = rec;
		this.list = list;
	}

	@Override
	public boolean subtype(Type other, HashSet<SubtypeRelation> subtypes) {
		return false;
	}

	@Override
	public boolean subtype(Type other) {
		return false;
	}

	@Override
	public boolean isSimple() {
		return false;
	}

	@Override
	public Map<String, Type> getChildren() {
		return null;
	}

	@Override
	public Type cloneWithChildren(Map<String, Type> newChildren, TypeTransformer transformer) {
		return null;
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {

	}

	private static class ArgReplacer extends AbstractTypeTransformer {
		private Map<TypeVar, Type> replacements;

		public ArgReplacer(Map<TypeVar, Type> replacements) {
			this.replacements = replacements;
		}

		@Override
		public Type transform(Type type) {
			if (type instanceof TypeVar && replacements.containsKey(type))
				return replacements.get(type);
			return super.defaultTransformation(type);
		}
	}

	@Override
	public Map<String, Type> getTypes() {
		HashMap<String, Type> res = new HashMap<>();

		res.put("tgt", rec);
		AtomicInteger argn = new AtomicInteger(0);
		res.putAll(list.stream().collect(Collectors.toMap(e->"arg"+ argn.incrementAndGet(), e->e)));

		return res;
	}

	@Override
	public Type setTypes(Map<String, Type> newTypes) {
		List<Type> newList = newTypes.entrySet().stream().filter(entry->entry.getKey().startsWith("arg"))
				.sorted(Comparator.comparing(e->Integer.parseInt(e.getKey().substring(3)), Integer::compare))
				.map(Map.Entry::getValue).collect(Collectors.toList());

		Type rec = newTypes.get("tgt");

		return getAppliedType(newList, rec);
	}

	public static Type getAppliedType(List<Type> newList, Type rec) {
		Iterator<Type> inp = newList.iterator();
		if (!(rec instanceof TypeLambda))
			throw new RuntimeException();

		if (((TypeLambda) rec).getArguments().size() != newList.size())
			throw new RuntimeException();

		return new ArgReplacer(((TypeLambda) rec).getArguments().stream().collect(Collectors.toMap(e -> e, e -> inp.next())))
				.transform(((TypeLambda) rec).getBody());
	}
}
