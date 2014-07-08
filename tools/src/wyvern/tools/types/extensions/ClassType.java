package wyvern.tools.types.extensions;

import wyvern.tools.typedAST.core.binding.evaluation.VarValueBinding;
import wyvern.tools.typedAST.core.binding.typechecking.AssignableNameBinding;
import wyvern.tools.typedAST.core.expressions.Invocation;
import wyvern.tools.typedAST.core.binding.Binding;
import wyvern.tools.typedAST.core.binding.NameBinding;
import wyvern.tools.typedAST.core.binding.NameBindingImpl;
import wyvern.tools.typedAST.core.binding.typechecking.TypeBinding;
import wyvern.tools.typedAST.core.declarations.ClassDeclaration;
import wyvern.tools.typedAST.transformers.Types.TypeTransformer;
import wyvern.tools.types.*;
import wyvern.tools.util.Reference;
import wyvern.tools.util.TreeWriter;

import java.util.*;
import java.util.function.Supplier;

import static wyvern.tools.errors.ErrorMessage.OPERATOR_DOES_NOT_APPLY;
import static wyvern.tools.errors.ToolError.reportError;

public class ClassType extends AbstractTypeImpl implements OperatableType, RecordType, ParameterizableType {
	private ClassDeclaration decl = null;
	protected Reference<Environment> declEnv;
	protected Reference<Environment> typeEquivalentEnv = new Reference<>();
	private List<String> params;
	private String name;


	public ClassType(ClassDeclaration td) {
		this(td.getDeclEnvRef(),
				td.getTypeEquivalentEnvironmentReference(),
				td.getTypeParams(),
				td.getName());
		this.decl = td;
	}

	public ClassType(Reference<Environment> declEnv,
					 Reference<Environment> typeEquivalentEnv,
					 List<String> typeParams,
					 String name) {
		this.declEnv = declEnv;
		this.typeEquivalentEnv = typeEquivalentEnv;
		this.params = typeParams;
		this.name = name;
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {
		// nothing to write		
	}

	private boolean recursive = false;
	@Override
	public String toString() {
		if (declEnv.get() != null) {
			if (!recursive) {
				recursive = true;
				String op = "CLASS(" + declEnv.get().toString() + ")";
				recursive = false;
				return op;
			} else {
				return "CLASS(Recursive)";
			}
		} else {
			return "CLASS()";
		}
	}

	@Override
	public Type checkOperator(Invocation opExp, Environment env) {
		// should not be any arguments - that is in a separate application at present
		if (opExp.getArgument() != null)
			throw new RuntimeException(opExp.getLocation().toString());
		assert opExp.getArgument() == null;
		
		// the operation should exist
		String opName = opExp.getOperationName();
		NameBinding m = declEnv.get().lookup(opName);

		if (m == null)
			reportError(OPERATOR_DOES_NOT_APPLY, opExp, opName, this.toString());
		
		// TODO Auto-generated method stub
		return m.getType();
	}

	public ClassDeclaration getDecl() {
		return decl;
	}

	private TypeType equivType = null;
	public TypeType getEquivType() {
		if (typeEquivalentEnv == null || typeEquivalentEnv.get() == null) {
			if (declEnv.get() != null) {
				if (typeEquivalentEnv == null)
					typeEquivalentEnv = new Reference<>();
				typeEquivalentEnv.set(TypeDeclUtils.getTypeEquivalentEnvironment(declEnv.get()));
			} else
				throw new RuntimeException();
		}

		if (equivType == null)
			equivType = new TypeType(typeEquivalentEnv.get());
		return equivType;
	}

	// FIXME: Do something similar here to TypeType maybe and maybe try to integrate the above
	// implements checks into here and change ClassDeclaration to use this instead.
	@Override
	public boolean subtype(Type other, HashSet<SubtypeRelation> subtypes) {
		if (super.subtype(other, subtypes)) {
			return true;
		}

		if (other instanceof TypeType) {
			// System.out.println("Is\n" + this.getEquivType() + "\n a subtype of \n" + other + "\n?");
			return getEquivType().subtype(other);
		} else if (other instanceof ClassType) {
			return getEquivType().subtype(((ClassType) other).getEquivType());
		}
		
		return false;
	}

	@Override
	public Type getInnerType(String name) {
		return declEnv.get().lookupType(name).getType();
	}


	public Environment getEnv() {
		return declEnv.get();
	}

	@Override
	public Type checkParameters(List<Type> params) {
		return null;
	}
	@Override
	public Map<String, Type> getChildren() {
		return new HashMap<>();
	}


	@Override
	public Type cloneWithChildren(Map<String, Type> newChildren, TypeTransformer transformer) {

		return new ClassType(new Reference<>(cloneEnv(transformer, declEnv::get)),
				             new Reference<>(cloneEnv(transformer, typeEquivalentEnv::get)), params, getName());
	}

	private Supplier<Environment> cloneEnv(TypeTransformer transformer, Supplier<Environment> oenv) {
		return () -> {
			Environment nenv = Environment.getEmptyEnvironment();

			List<Binding> oldBindings = new ArrayList<>(oenv.get().getBindings());
			Collections.reverse(oldBindings);
			for (Binding b : oldBindings) {
				if (b instanceof AssignableNameBinding)
					nenv = nenv.extend(new AssignableNameBinding(b.getName(), transformer.transform(b.getType())));
				else if (b instanceof NameBindingImpl)
					nenv = nenv.extend(new NameBindingImpl(b.getName(), transformer.transform(b.getType())));
				else if (b instanceof TypeBinding)
					nenv = nenv.extend(new TypeBinding(b.getName(), transformer.transform(b.getType())));
				else
					throw new RuntimeException();
			}
			return nenv;
		};
	}

	public String getName() {
		return name;
	}
}