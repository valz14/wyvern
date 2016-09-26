package wyvern.tools.typedAST.core.values;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import wyvern.target.corewyvernIL.expression.Expression;
import wyvern.target.corewyvernIL.modules.TypedModuleSpec;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.WyvernException;
import wyvern.tools.typedAST.abs.AbstractValue;
import wyvern.tools.typedAST.core.binding.AssignableValueBinding;
import wyvern.tools.typedAST.core.binding.evaluation.ValueBinding;
import wyvern.tools.typedAST.core.expressions.Assignment;
import wyvern.tools.typedAST.core.expressions.Invocation;
import wyvern.tools.typedAST.core.expressions.TaggedInfo;
import wyvern.tools.typedAST.interfaces.Assignable;
import wyvern.tools.typedAST.interfaces.InvokableValue;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.typedAST.transformers.GenerationEnvironment;
import wyvern.tools.typedAST.transformers.ILWriter;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.extensions.ClassType;
import wyvern.tools.types.extensions.TypeDeclUtils;
import wyvern.tools.util.EvaluationEnvironment;
import wyvern.tools.util.Reference;
import wyvern.tools.util.TreeWriter;

public class Obj extends AbstractValue implements InvokableValue, Assignable {
	protected Reference<EvaluationEnvironment> intEnv;
	private TaggedInfo taggedInfo;
	private Environment typeEquivEnv;
	
	public Obj(EvaluationEnvironment declEnv, TaggedInfo taggedInfo) {
		this.taggedInfo = taggedInfo;
		this.intEnv = new Reference<>(declEnv);
	}

    public Obj(Reference<EvaluationEnvironment> declEnv, TaggedInfo taggedInfo) {
		intEnv = declEnv;
		this.taggedInfo = taggedInfo;
	}

    private void updateTee() {
        typeEquivEnv = TypeDeclUtils.getTypeEquivalentEnvironment(intEnv.get().toTypeEnv());
    }

	public Type getType() {
		if (typeEquivEnv == null) {
        	updateTee();
        }
		return new ClassType(intEnv.map(EvaluationEnvironment::toTypeEnv), new Reference<>(typeEquivEnv), new LinkedList<String>(), taggedInfo, null);
	}

	@Override
	public Map<String, TypedAST> getChildren() {
		return new HashMap<>();
	}

	@Override
	public TypedAST cloneWithChildren(Map<String, TypedAST> newChildren) {
		return this;
	}

    @Deprecated
	public EvaluationEnvironment getIntEnv() {
		return intEnv.get();
	}

	private FileLocation location = FileLocation.UNKNOWN;
	public FileLocation getLocation() {
		return this.location;
	}

	public TaggedInfo getTaggedInfo() {
		return taggedInfo;
	}

	@Override
	public Expression generateIL(GenContext ctx, ValueType expectedType, List<TypedModuleSpec> dependencies) {
		// TODO Auto-generated method stub
		return null;
	}
}
