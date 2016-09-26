package wyvern.tools.typedAST.core.declarations;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import wyvern.target.corewyvernIL.ASTNode;
import wyvern.target.corewyvernIL.decl.TypeDeclaration;
import wyvern.target.corewyvernIL.decl.ValDeclaration;
import wyvern.target.corewyvernIL.decltype.DeclType;
import wyvern.target.corewyvernIL.expression.Expression;
import wyvern.target.corewyvernIL.expression.Let;
import wyvern.target.corewyvernIL.expression.New;
import wyvern.target.corewyvernIL.expression.Variable;
import wyvern.target.corewyvernIL.modules.TypedModuleSpec;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;
import wyvern.tools.typedAST.abs.Declaration;
import wyvern.tools.typedAST.core.binding.NameBinding;
import wyvern.tools.typedAST.core.binding.NameBindingImpl;
import wyvern.tools.typedAST.core.binding.evaluation.HackForArtifactTaggedInfoBinding;
import wyvern.tools.typedAST.core.binding.evaluation.LateValueBinding;
import wyvern.tools.typedAST.core.binding.evaluation.ValueBinding;
import wyvern.tools.typedAST.core.binding.objects.ClassBinding;
import wyvern.tools.typedAST.core.binding.objects.TypeDeclBinding;
import wyvern.tools.typedAST.core.binding.typechecking.LateTypeBinding;
import wyvern.tools.typedAST.core.binding.typechecking.TypeBinding;
import wyvern.tools.typedAST.core.expressions.TaggedInfo;
import wyvern.tools.typedAST.core.values.Obj;
import wyvern.tools.typedAST.interfaces.CoreAST;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.typedAST.transformers.GenerationEnvironment;
import wyvern.tools.typedAST.transformers.ILWriter;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.UnresolvedType;
import wyvern.tools.types.extensions.ClassType;
import wyvern.tools.types.extensions.TypeDeclUtils;
import wyvern.tools.types.extensions.TypeInv;
import wyvern.tools.types.extensions.TypeType;
import wyvern.tools.types.extensions.Unit;
import wyvern.tools.util.EvaluationEnvironment;
import wyvern.tools.util.Pair;
import wyvern.tools.util.Reference;
import wyvern.tools.util.TreeWriter;

public class ClassDeclaration extends AbstractTypeDeclaration implements CoreAST {
	protected DeclSequence decls = new DeclSequence(new LinkedList<Declaration>());
	private List<String> typeParams;
	// protected DeclSequence classDecls;

	private NameBinding nameBinding;
	protected TypeBinding typeBinding;

	private String implementsName;
	private String implementsClassName;

	private TypeBinding nameImplements;

	protected EvaluationEnvironment declEvalEnv;

	private TypeType equivalentType = null;
	private TypeType equivalentClassType = null;
	private Reference<Environment> typeEquivalentEnvironmentRef;
	protected Reference<Environment> classMembersEnv;

	private Reference<Environment> instanceMembersEnv = new Reference<>(Environment.getEmptyEnvironment());
	protected Environment getObjEnvV() { return instanceMembersEnv.get(); }
	protected void setInstanceMembersEnv(Environment newEnv) { instanceMembersEnv.set(newEnv); }

	private ClassType objType;

	public ClassType getOType() {
		return objType;
	}

	public ClassDeclaration(String name,
							String implementsName,
							String implementsClassName,
							DeclSequence decls,
							Environment declEnv,
							List<String> typeParams,
							FileLocation location) {
        this(name, implementsName, implementsClassName, decls, typeParams, location);
		classMembersEnv.set(declEnv);
    }

	public ClassDeclaration(String name,
			TaggedInfo taggedInfo,
			String implementsName,
			String implementsClassName,
			DeclSequence decls,
			FileLocation location) {
		this(name, implementsName, implementsClassName, decls, new LinkedList<String>(), location);

		// System.out.println("Creating class declaration for: " + name + " with decls " + decls);

		objType = new ClassType(instanceMembersEnv, new Reference<>(), new LinkedList<>(), taggedInfo, "");
		typeBinding = new TypeBinding(name, getObjType());
		setupTags(name, typeBinding, taggedInfo);
		nameBinding = new NameBindingImpl(name, getClassType());
	}

	public ClassDeclaration(String name,
							String implementsName,
							String implementsClassName,
							DeclSequence decls,
							FileLocation location) {
		this(name, implementsName, implementsClassName, decls, new LinkedList<String>(), location);

	}

    public ClassDeclaration(String name,
							String implementsName,
							String implementsClassName,
							DeclSequence decls,
							List<String> typeParams,
							FileLocation location) {

    	//System.out.println("Made class: " + name);

    	this.decls = decls;
		this.typeParams = typeParams;
		typeEquivalentEnvironmentRef = new Reference<>();
		classMembersEnv = new Reference<>();
		nameBinding = new NameBindingImpl(name, null);
		objType = new ClassType(instanceMembersEnv, new Reference<>(), new LinkedList<>(), null, "");
		typeBinding = new TypeBinding(name, getObjType());
		nameBinding = new NameBindingImpl(name, getClassType());
		this.implementsName = implementsName;
		this.implementsClassName = implementsClassName;
		this.location = location;
	}

	protected ClassType getObjType() {
		return objType;
	}

	protected Type getClassType() {
		return new ClassType(this);
	}

	private Type getObjectType() {
		Environment declEnv = getInstanceMembersEnv();
		Environment objTee = TypeDeclUtils.getTypeEquivalentEnvironment(declEnv);
		return new ClassType(instanceMembersEnv, new Reference<Environment>(objTee) {
			@Override
			public Environment get() {
				return TypeDeclUtils.getTypeEquivalentEnvironment(instanceMembersEnv.get());
			}

			@Override
			public void set(Environment e) {
				throw new RuntimeException();
			}
		}, new LinkedList<>(), getTaggedInfo(), this.getName());
	}

	public Environment getInstanceMembersEnv() {
		return instanceMembersEnv.get();
	}

	public DeclSequence getDecls() {
		return decls;
	}

	@Override
	public String getName() {
		return nameBinding.getName();
	}

	private FileLocation location = FileLocation.UNKNOWN;

	@Override
	public FileLocation getLocation() {
		return location; // TODO: NOT IMPLEMENTED YET.
	}

	public Environment getDeclEnv() {
		return classMembersEnv.get();
	}

	public Reference<Environment> getTypeEquivalentEnvironmentReference() {
		return typeEquivalentEnvironmentRef;
	}

	public Reference<Environment> getClassMembersEnv() {
		return classMembersEnv;
	}

	@Override
	public Map<String, TypedAST> getChildren() {
		Hashtable<String, TypedAST> children = new Hashtable<>();
		int i = 0;
		for (TypedAST ast : decls) {
			children.put(i++ + "decl", ast);
		}
		return children;
	}

	@Override
	public TypedAST cloneWithChildren(Map<String, TypedAST> nc) {
		List<Declaration> decls = new ArrayList<Declaration>(nc.size());
		Iterable<String> keys = nc.keySet().stream().filter(key->key.endsWith("decl"))
				.map(key->new Pair<String,Integer>(key, Integer.parseInt(key.substring(0,key.length() - 4))))
				.<Pair<String,Integer>>sorted((a,b)->a.second-b.second)
				.map(pair->pair.first)::iterator;
		for (String key : keys) {
			if (!key.endsWith("decl"))
				continue;
			int idx = Integer.parseInt(key.substring(0,key.length() - 4));
			decls.add(idx, (Declaration)nc.get(key));
		}
		ClassDeclaration classDeclaration = new ClassDeclaration(nameBinding.getName(), implementsName, implementsClassName,
				new DeclSequence(decls), classMembersEnv.get(), typeParams, location);
		classDeclaration.setupTags(nameBinding.getName(), classDeclaration.typeBinding, getTaggedInfo());
		return classDeclaration;
	}

    public List<String> getTypeParams() {
		return typeParams;
	}

	@Override
	public DeclType genILType(GenContext ctx) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public wyvern.target.corewyvernIL.decl.Declaration generateDecl(GenContext ctx, GenContext thisContext) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public wyvern.target.corewyvernIL.decl.Declaration topLevelGen(GenContext ctx, List<TypedModuleSpec> dependencies) {
		// TODO Auto-generated method stub
		return null;
	}

    public Type getType() {
        return this.typeBinding.getType();
    }

    /* TODO delete in a future date, after decoupling it with Java interop
    public EvaluationEnvironment getFilledBody(AtomicReference<Value> objRef) {
        return evaluateDeclarations(
            EvaluationEnvironment.EMPTY
                .extend(new LateValueBinding("this", objRef, getType())));
    }
    */
}
