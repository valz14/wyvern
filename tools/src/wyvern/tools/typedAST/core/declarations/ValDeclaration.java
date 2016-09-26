package wyvern.tools.typedAST.core.declarations;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import wyvern.target.corewyvernIL.decltype.DeclType;
import wyvern.target.corewyvernIL.decltype.ValDeclType;
import wyvern.target.corewyvernIL.expression.Expression;
import wyvern.target.corewyvernIL.expression.Let;
import wyvern.target.corewyvernIL.expression.Variable;
import wyvern.target.corewyvernIL.modules.TypedModuleSpec;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.target.corewyvernIL.support.TopLevelContext;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;
import wyvern.tools.typedAST.abs.Declaration;
import wyvern.tools.typedAST.core.binding.NameBinding;
import wyvern.tools.typedAST.core.binding.NameBindingImpl;
import wyvern.tools.typedAST.core.binding.StaticTypeBinding;
import wyvern.tools.typedAST.core.binding.evaluation.ValueBinding;
import wyvern.tools.typedAST.core.expressions.TaggedInfo;
import wyvern.tools.typedAST.interfaces.CoreAST;
import wyvern.tools.typedAST.interfaces.ExpressionAST;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.typedAST.transformers.ExpressionWriter;
import wyvern.tools.typedAST.transformers.GenerationEnvironment;
import wyvern.tools.typedAST.transformers.ILWriter;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.TypeResolver;
import wyvern.tools.types.UnresolvedType;
import wyvern.tools.types.extensions.TypeInv;
import wyvern.tools.util.EvaluationEnvironment;
import wyvern.tools.util.TreeWriter;

public class ValDeclaration extends Declaration implements CoreAST {
	ExpressionAST definition;
	Type definitionType;
	NameBinding binding;
	
	Type declaredType;
	String declaredTypeName;
	
	String variableName;

	private TaggedInfo ti;

	private boolean isClass;
	public boolean isClassMember() {
		return isClass;
	}

	public ValDeclaration(String name, TypedAST definition, FileLocation location) {
		this.definition=(ExpressionAST) definition;
		binding = new NameBindingImpl(name, null);
		this.location = location;
	}

	public ValDeclaration(String name, Type type, TypedAST definition, FileLocation location) {
		if (type instanceof UnresolvedType) {
			UnresolvedType t = (UnresolvedType) type;
			TaggedInfo tag = TaggedInfo.lookupTagByType(t); // FIXME:
            ti = tag;
            variableName = name;
            declaredType = type; // Record this.
		}

		this.definition=(ExpressionAST)definition;
		binding = new NameBindingImpl(name, type);
		this.location = location;
	}

	public NameBinding getBinding() {
		return binding;
	}

	public Type getType() {
		return binding.getType();
	}

	@Override
	public String getName() {
		return binding.getName();
	}
	
	public ExpressionAST getDefinition() {
		return definition;
	}

	@Override
	public Map<String, TypedAST> getChildren() {
		Hashtable<String, TypedAST> children = new Hashtable<>();
		if (definition != null)
			children.put("definition", definition);
		return children;
	}

	@Override
	public TypedAST cloneWithChildren(Map<String, TypedAST> nc) {
		if (nc.containsKey("definition"))
			return new ValDeclaration(getName(), binding.getType(), nc.get("definition"), location);
		return new ValDeclaration(getName(), binding.getType(), null, location);
	}
  
	private FileLocation location = FileLocation.UNKNOWN;
	public FileLocation getLocation() {
		return this.location; //TODO
	}

	@Override
	public void genTopLevel(TopLevelContext tlc) {
		ValueType declType = getILValueType(tlc.getContext());
		tlc.addLet(getName(), getILValueType(tlc.getContext()), definition.generateIL(tlc.getContext(), declType, tlc.getDependencies()), false);
	}

	@Override
	public DeclType genILType(GenContext ctx) {
		ValueType vt = getILValueType(ctx);
		return new ValDeclType(getName(), vt);
	}

	private ValueType getILValueType(GenContext ctx) {
		ValueType vt;
		if (declaredType != null) {
			// convert the declared type if there is one
			vt = declaredType.getILType(ctx);
		} else {

            final Type type = this.binding.getType();
            if (type != null) {
			
			// then there is no proper R-value
			//if(definition == null) {
				vt = type.getILType(ctx);
			} else {
				// convert the declaration and typecheck it
				vt = definition.generateIL(ctx, null, null).typeCheck(ctx);
			}
		}
		return vt;
	}

	@Override
	public wyvern.target.corewyvernIL.decl.Declaration generateDecl(GenContext ctx, GenContext thisContext) {
		
		ValueType expectedType = getILValueType(thisContext);
		/* uses ctx for generating the definition, as the selfName is not in scope */
		return new wyvern.target.corewyvernIL.decl.ValDeclaration(getName(), expectedType, definition.generateIL(ctx, expectedType, null), location);
	}

	@Override
	public wyvern.target.corewyvernIL.decl.Declaration topLevelGen(GenContext ctx, List<TypedModuleSpec> dependencies) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addModuleDecl(TopLevelContext tlc) {
		wyvern.target.corewyvernIL.decl.Declaration decl =
			new wyvern.target.corewyvernIL.decl.ValDeclaration(getName(),
					getILValueType(tlc.getContext()),
					new wyvern.target.corewyvernIL.expression.Variable(getName()), location);
		DeclType dt = genILType(tlc.getContext());
		tlc.addModuleDecl(decl,dt);
	}
}
