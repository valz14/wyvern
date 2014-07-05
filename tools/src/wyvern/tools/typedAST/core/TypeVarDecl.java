package wyvern.tools.typedAST.core;

import wyvern.tools.errors.FileLocation;
import wyvern.tools.typedAST.abs.Declaration;
import wyvern.tools.typedAST.core.binding.typechecking.TypeBinding;
import wyvern.tools.typedAST.core.declarations.DeclSequence;
import wyvern.tools.typedAST.core.declarations.TypeDeclaration;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.extensions.TypeVar;
import wyvern.tools.types.extensions.Unit;
import wyvern.tools.util.TreeWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TypeVarDecl extends Declaration {
	private final String name;
	private final Optional<TypeDeclaration> body;
	private final FileLocation fileLocation;
	private TypeVar typeVar = new TypeVar();

	public TypeVarDecl(String name, DeclSequence body, FileLocation fileLocation) {
		this.name = name;
		this.body = Optional.of(body).map(seq->new TypeDeclaration(name, seq, fileLocation));
		this.fileLocation = fileLocation;
	}

	public TypeVarDecl(String name, FileLocation fileLocation) {
		this.body = Optional.empty();
		this.name = name;
		this.fileLocation = fileLocation;
	}

	public boolean isAbstract() { return !body.isPresent(); }

	public TypeVar getTypeVar() { return typeVar; }

	@Override
	public String getName() {
		return name;
	}

	@Override
	protected Type doTypecheck(Environment env) {
		return body.map(decl->decl.typecheck(env, Optional.empty())).orElseGet(()-> Unit.getInstance());
	}

	@Override
	protected Environment doExtend(Environment old, Environment against) {
		return body.map(decl->decl.extend(old,against)).orElse(old.extend(new TypeBinding(name, typeVar)));
	}

	@Override
	public Environment extendWithValue(Environment old) {
		return body.map(decl->decl.extendWithValue(old)).orElse(old);
	}

	@Override
	public void evalDecl(Environment evalEnv, Environment declEnv) {
		body.ifPresent(decl->decl.evalDecl(evalEnv,declEnv));
	}

	@Override
	public Environment extendType(Environment env, Environment against) {
		return body.map(tpe->tpe.extendType(env,against)).orElse(env.extend(new TypeBinding(name, typeVar)));
	}

	@Override
	public Environment extendName(Environment env, Environment against) {
		return body.map(tpe->tpe.extendName(env, against)).orElse(env);
	}

	@Override
	public Type getType() {
		return null;
	}

	@Override
	public Map<String, TypedAST> getChildren() {
		return body.map(TypeDeclaration::getChildren).orElse(new HashMap<>());
	}

	@Override
	public TypedAST cloneWithChildren(Map<String, TypedAST> newChildren) {
		return body.map(td->td.cloneWithChildren(newChildren)).orElse(this);
	}

	@Override
	public FileLocation getLocation() {
		return null;
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {

	}
}
