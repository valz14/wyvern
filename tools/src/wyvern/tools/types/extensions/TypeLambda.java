package wyvern.tools.types.extensions;

import wyvern.tools.typedAST.transformers.Types.TypeTransformer;
import wyvern.tools.types.SubtypeRelation;
import wyvern.tools.types.Type;
import wyvern.tools.util.AbstractTreeWritable;
import wyvern.tools.util.TreeWritable;
import wyvern.tools.util.TreeWriter;

import java.util.*;

public class TypeLambda extends AbstractTreeWritable implements Type, TreeWritable {
	private List<TypeVar> bindings = new LinkedList<>();
	private final Type body;

	public TypeLambda(TypeVar binding, Type body) {
		this.bindings.add(binding);
		this.body = body;
	}

	public TypeLambda(List<TypeVar> bindings, Type body) {
		this.bindings = bindings;
		this.body = body;
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
		Map<String, Type> out = new HashMap<>();
		out.put("body", body);
		return out;
	}

	@Override
	public Type cloneWithChildren(Map<String, Type> newChildren, TypeTransformer transformer) {
		return new TypeLambda(bindings, newChildren.get("body"));
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {
		writer.writeArgs(bindings, body);
	}

	public List<TypeVar> getArguments() {
		return bindings;
	}

	public Type getBody() {
		return body;
	}
}
