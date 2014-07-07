package wyvern.tools.typedAST.core.expressions;

import wyvern.tools.errors.FileLocation;
import wyvern.tools.typedAST.abs.AbstractTypedAST;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.extensions.TypeApp;
import wyvern.tools.types.extensions.TypeLambda;
import wyvern.tools.util.TreeWriter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TypeApplication extends AbstractTypedAST {
	private final TypedAST src;
	private final List<Type> args;
	private final FileLocation fileLocation;

	public TypeApplication(TypedAST src, List<Type> args, FileLocation fileLocation) {
		this.src = src;
		this.args = args;
		this.fileLocation = fileLocation;
	}

	@Override
	public Type getType() {
		return null;
	}

	@Override
	public Type typecheck(Environment env, Optional<Type> expected) {
		Type recType = src.typecheck(env, expected); //TODO

		return TypeApp.getAppliedType(args, recType);
	}

	@Override
	public Value evaluate(Environment env) {
		return src.evaluate(env);
	}

	@Override
	public Map<String, TypedAST> getChildren() {
		HashMap<String,TypedAST> res = new HashMap<>();
		res.put("src",src);
		return res;
	}

	@Override
	public TypedAST cloneWithChildren(Map<String, TypedAST> newChildren) {
		return new TypeApplication(newChildren.get("src"), args, fileLocation);
	}

	@Override
	public FileLocation getLocation() {
		return fileLocation;
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {
		writer.writeArgs(src);
	}
}
