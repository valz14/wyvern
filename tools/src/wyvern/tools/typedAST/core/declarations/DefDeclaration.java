package wyvern.tools.typedAST.core.declarations;

import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;
import wyvern.tools.typedAST.abs.Declaration;
import wyvern.tools.typedAST.core.binding.typechecking.TypeBinding;
import wyvern.tools.typedAST.core.evaluation.Closure;
import wyvern.tools.typedAST.core.binding.NameBinding;
import wyvern.tools.typedAST.core.binding.NameBindingImpl;
import wyvern.tools.typedAST.core.binding.evaluation.ValueBinding;
import wyvern.tools.typedAST.interfaces.BoundCode;
import wyvern.tools.typedAST.interfaces.CoreAST;
import wyvern.tools.typedAST.interfaces.CoreASTVisitor;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.TypeResolver;
import wyvern.tools.types.extensions.*;
import wyvern.tools.util.Pair;
import wyvern.tools.util.TreeWritable;
import wyvern.tools.util.TreeWriter;

import java.util.*;
import java.util.stream.Collectors;

//Def's canonical form is: def NAME : TYPE where def m() : R -> def : m : Unit -> R

public class DefDeclaration extends Declaration implements CoreAST, BoundCode, TreeWritable {
	protected TypedAST body; // HACK
	private String name;
	private Type type;
	private List<NameBinding> argNames; // Stored to preserve their names mostly for environments etc.
	private List<Pair<String, TypeVar>> typeArgNames;

	public DefDeclaration(String name, Type fullType, List<NameBinding> argNames,
						  TypedAST body, boolean isClassDef, FileLocation location) {
		this(name, getMethodType(argNames, fullType, new LinkedList<>()), argNames, new LinkedList<>(), body, isClassDef, location, false);
	}



	public DefDeclaration(String name, Type fullType, List<NameBinding> argNames, List<String> typeArgNames,
						   TypedAST body, boolean isClassDef, FileLocation location) {
		this(name, null, argNames, typeArgNames, body, isClassDef, location, false);
		List<Pair<String, TypeVar>> typeArgs = getTypeArgs(typeArgNames);
		this.type = getMethodType(argNames, fullType, typeArgs);
		this.typeArgNames = typeArgs;
	}

	public static List<Pair<String, TypeVar>> getTypeArgs(List<String> typeArgNames) {
		return typeArgNames.stream()
				.map(iname -> new Pair<>(iname, new TypeVar())).collect(Collectors.toList());
	}

	public DefDeclaration(String name, Type fullType, List<NameBinding> argNames,
						   TypedAST body, boolean isClassDef) {
		this(name, fullType, argNames, new LinkedList<>(), body, isClassDef, FileLocation.UNKNOWN, false);
	}

	private DefDeclaration(String name, Type fullType, List<NameBinding> argNames, List<String> typeArgNames,
						   TypedAST body, boolean isClassDef, FileLocation location, boolean placeholder) {
		this(name, fullType, argNames, getTypeArgs(typeArgNames), body, isClassDef, FileLocation.UNKNOWN, 0);
	}
	private DefDeclaration(String name, Type fullType, List<NameBinding> argNames, List<Pair<String,TypeVar>> typeArgNames,
						   TypedAST body, boolean isClassDef, FileLocation location, int placeholder) {
		if (argNames == null) { argNames = new LinkedList<NameBinding>(); }
		this.type = fullType;
		this.name = name;
		this.body = body;
		this.argNames = argNames;
		this.isClass = isClassDef;
		this.location = location;
		this.typeArgNames = typeArgNames;
	}


	public static Type getMethodType(List<NameBinding> args, Type returnType, List<Pair<String,TypeVar>> typeArgs) {
		Type argType = null;
		if (args.size() == 0) {
			argType = Unit.getInstance();
		} else if (args.size() == 1) {
			argType = args.get(0).getType();
		} else {
			argType = new Tuple(args);
		}
		Arrow ires = new Arrow(argType, returnType);
		if (typeArgs.isEmpty())
			return ires;
		List<TypeVar> tvs = typeArgs.stream().map(p -> p.second).collect(Collectors.toList());
		return new TypeLambda(tvs, ires);
	}

	private Environment getTypeVarEnv() {
		return typeArgNames.stream().map(p->new TypeBinding(p.first,p.second)).reduce(Environment.getEmptyEnvironment(), (a,b)->a.extend(b), (a,b)->a);
	}
	

	private boolean isClass;
	public boolean isClass() {
		return isClass;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {
		writer.writeArgs(name, type, body);
	}

	@Override
	public void accept(CoreASTVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public Map<String, TypedAST> getChildren() {
		Map<String, TypedAST> childMap = new HashMap<>();
		if (body != null)
			childMap.put("body", body);
		return childMap;
	}

	@Override
	public TypedAST cloneWithChildren(Map<String, TypedAST> newChildren) {
		return new DefDeclaration(name, type, argNames, typeArgNames, newChildren.get("body"), isClass, location, 0);
	}

	private Type getResultType(Type tpe) {
		while (tpe instanceof TypeLambda)
			tpe = ((TypeLambda) tpe).getBody();
		return ((Arrow) tpe).getResult();
	}

	@Override
	protected Type doTypecheck(Environment env) {
		Environment extEnv = env;
		for (NameBinding bind : argNames) {
			extEnv = extEnv.extend(bind);
		}
		if (body != null) {
			Type bodyType = body.typecheck(extEnv.extend(getTypeVarEnv()), Optional.of(getResultType(type))); // Can be null for def inside type!
			type = TypeResolver.resolve(type, env.extend(getTypeVarEnv()));
			
			Type retType = getResultType(type);
			
			// System.out.println("bodyType = " + bodyType);
			// System.out.println("retType = " + retType);
			
			if (bodyType != null && !bodyType.subtype(retType))
				ToolError.reportError(ErrorMessage.NOT_SUBTYPE, this, bodyType.toString(), getResultType(type).toString());
		}
		return type;
	}

	@Override
	protected Environment doExtend(Environment old, Environment against) {
		return extendName(old, against);
	}

	@Override
	public List<NameBinding> getArgBindings() {
		return argNames;
	}

	@Override
	public TypedAST getBody() {
		return body;
	}

	@Override
	public Environment extendWithValue(Environment old) {
		Environment newEnv = old.extend(new ValueBinding(name, type));
		return newEnv;
	}

	@Override
	public void evalDecl(Environment evalEnv, Environment declEnv) {
		Closure closure = new Closure(this, evalEnv);
		ValueBinding vb = (ValueBinding) declEnv.lookup(name);
		vb.setValue(closure);
	}

	private FileLocation location = FileLocation.UNKNOWN;
	
	@Override
	public FileLocation getLocation() {
		return location; 
	}

	@Override
	public Environment extendType(Environment env, Environment against) {
		return env;
	}

	Type resolvedType = null;
	@Override
	public Environment extendName(Environment env, Environment against) {
		for (int i = 0; i < argNames.size(); i++) {
			NameBinding oldBinding = argNames.get(i);
			argNames.set(i, new NameBindingImpl(oldBinding.getName(), TypeResolver.resolve(oldBinding.getType(), against.extend(getTypeVarEnv()))));
		}
		if (resolvedType == null)
			resolvedType = TypeResolver.resolve(type, against.extend(getTypeVarEnv()));
		return env.extend(new NameBindingImpl(name, resolvedType));
	}
}