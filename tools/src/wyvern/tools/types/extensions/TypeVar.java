package wyvern.tools.types.extensions;

import wyvern.tools.types.SubtypeRelation;
import wyvern.tools.types.Type;
import wyvern.tools.util.AbstractTreeWritable;
import wyvern.tools.util.TreeWriter;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TypeVar extends AbstractTreeWritable implements Type {
	private static AtomicInteger genNum = new AtomicInteger(0);
	private int inum = genNum.getAndIncrement();

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
	public Type cloneWithChildren(Map<String, Type> newChildren) {
		return null;
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {
		writer.writeArgs(inum);
	}
}
