package wyvern.tools.typedAST.core.declarations;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import wyvern.target.corewyvernIL.decltype.DeclType;
import wyvern.target.corewyvernIL.modules.TypedModuleSpec;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.WyvernException;
import wyvern.tools.typedAST.abs.Declaration;
import wyvern.tools.typedAST.core.binding.NameBinding;
import wyvern.tools.typedAST.core.binding.NameBindingImpl;
import wyvern.tools.typedAST.core.binding.evaluation.ValueBinding;
import wyvern.tools.typedAST.core.binding.objects.TypeDeclBinding;
import wyvern.tools.typedAST.core.binding.typechecking.LateNameBinding;
import wyvern.tools.typedAST.core.binding.typechecking.TypeBinding;
import wyvern.tools.typedAST.core.expressions.New;
import wyvern.tools.typedAST.core.expressions.TaggedInfo;
import wyvern.tools.typedAST.interfaces.CoreAST;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.typedAST.transformers.GenerationEnvironment;
import wyvern.tools.typedAST.transformers.ILWriter;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.extensions.TypeType;
import wyvern.tools.util.EvaluationEnvironment;
import wyvern.tools.util.Reference;
import wyvern.tools.util.TreeWriter;


public class TypeDeclaration extends AbstractTypeDeclaration implements CoreAST {
	private String name;
	protected DeclSequence decls;
	private Reference<Optional<TypedAST>> metadata;
	private NameBinding nameBinding;
	private TypeBinding typeBinding;
	
	private EvaluationEnvironment declEvalEnv;
    protected Reference<Environment> declEnv = new Reference<>(Environment.getEmptyEnvironment());
	protected Reference<Environment> attrEnv = new Reference<>(Environment.getEmptyEnvironment());
	
	public static EvaluationEnvironment attrEvalEnv = EvaluationEnvironment.EMPTY; // HACK
	private Reference<Value> metaValue = new Reference<>();

	public TypeDeclaration(String name, DeclSequence decls, Reference<Value> metadata, TaggedInfo taggedInfo, FileLocation clsNameLine) {
		// System.out.println("Initialising TypeDeclaration ( " + name + "): decls" + decls);
		this.name = name;
		this.decls = decls;
		nameBinding = new NameBindingImpl(name, null);
		typeBinding = new TypeBinding(name, null, metadata);
		Type objectType = new TypeType(this);

		attrEnv.set(attrEnv.get().extend(new TypeDeclBinding("type", this)));

		nameBinding = new LateNameBinding(nameBinding.getName(), () ->
            metadata.get().getType());
		typeBinding = new TypeBinding(nameBinding.getName(), objectType, metadata);

		setupTags(name, typeBinding, taggedInfo);
		// System.out.println("TypeDeclaration: " + nameBinding.getName() + " is now bound to type: " + objectType);

		this.location = clsNameLine;
		this.metaValue = metadata;
	}
	
    public TypeDeclaration(String name, DeclSequence decls, Reference<Value> metadata, FileLocation clsNameLine) {
		this(name, decls, metadata, null, clsNameLine);
	}

	public Type getType() {
		return this.typeBinding.getType();
	}

	@Override
	public Map<String, TypedAST> getChildren() {
		Map<String, TypedAST> childMap = new HashMap<>();
		childMap.put("decls", decls);
		return childMap;
	}

	@Override
	public TypedAST cloneWithChildren(Map<String, TypedAST> newChildren) {
		TypeDeclaration decls1 = new TypeDeclaration(nameBinding.getName(), (DeclSequence) newChildren.get("decls"), metaValue, getTaggedInfo(), location);
		return decls1;
	}

	protected Environment doExtend(Environment old, Environment against) {
		Environment newEnv = old.extend(nameBinding).extend(typeBinding);
		return newEnv;
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
		return location; 
	}

    public NameBinding lookupDecl(String name) {
        return declEnv.get().lookup(name);
    }


	public Reference<Environment> getDeclEnv() {
		return declEnv;
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

	public List<DeclType> genDeclTypeSeq(GenContext ctx){
		List<DeclType> declts = new LinkedList<DeclType>();
		for(Declaration d : decls.getDeclIterator()) {
			declts.add(d.genILType(ctx));
		}
		
		return declts;
	}
	
}
