package wyvern.tools.types.extensions;

import wyvern.tools.typedAST.transformers.Types.TypeTransformer;
import wyvern.tools.types.SubtypeRelation;
import wyvern.tools.types.Type;
import wyvern.tools.util.AbstractTreeWritable;
import wyvern.tools.util.TreeWriter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TypeVar extends AbstractTreeWritable implements Type {
	private static AtomicInteger genNum = new AtomicInteger(0);
	private int inum = genNum.getAndIncrement();

	@Override
	public boolean subtype(Type other, HashSet<SubtypeRelation> subtypes) {
		return other instanceof TypeVar && ((TypeVar) other).inum == inum;
	}

	@Override
	public boolean subtype(Type other) {

		return other instanceof TypeVar && ((TypeVar) other).inum == inum;
	}

	@Override
	public boolean isSimple() {
		return false;
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
		writer.writeArgs(inum);
	}
}
