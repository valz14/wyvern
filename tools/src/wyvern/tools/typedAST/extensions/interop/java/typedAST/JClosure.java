package wyvern.tools.typedAST.extensions.interop.java.typedAST;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import wyvern.target.corewyvernIL.expression.Expression;
import wyvern.target.corewyvernIL.modules.TypedModuleSpec;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.WyvernException;
import wyvern.tools.typedAST.abs.AbstractValue;
import wyvern.tools.typedAST.core.expressions.Application;
import wyvern.tools.typedAST.core.values.TupleValue;
import wyvern.tools.typedAST.core.values.UnitVal;
import wyvern.tools.typedAST.extensions.interop.java.Util;
import wyvern.tools.typedAST.interfaces.ApplyableValue;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.typedAST.transformers.GenerationEnvironment;
import wyvern.tools.typedAST.transformers.ILWriter;
import wyvern.tools.types.Type;
import wyvern.tools.types.extensions.Tuple;
import wyvern.tools.types.extensions.Unit;
import wyvern.tools.util.EvaluationEnvironment;
import wyvern.tools.util.TreeWriter;

public class JClosure extends AbstractValue implements ApplyableValue {
	private static AtomicInteger uidGen = new AtomicInteger();
	private int uid = uidGen.getAndIncrement();

	public static class JavaInvokableMethod {
		private final Class[] paramaterTypes;
		private Boolean classMeth;
		private final MethodHandle handle;
		private final Type returnType;
		private final List<String> paramNames;
		private Class srcClass;

		public JavaInvokableMethod(Class[] parameterTypes, Class ret, MethodHandle handle, List<String> paramNames, Boolean classMeth, Class source) {
			this.handle = handle;
			this.paramNames = paramNames;
			this.paramaterTypes = parameterTypes;
			this.classMeth = classMeth;
			returnType = Util.javaToWyvType(ret);
			this.srcClass = source;
		}

		@Override
		public String toString() {
			return handle.toString();
		}

		public MethodHandle getHandle() {
			return handle;
		}

		public List<String> getParamNames() {
			return paramNames;
		}

		public Class[] getParameterTypes() {
			return paramaterTypes;
		}

		public Boolean getClassMeth() {
			return classMeth;
		}

		public Class getSrcClass() {
			return srcClass;
		}

		public Type getReturnType() {
			return returnType;
		}
	}

	List<JavaInvokableMethod> methods = new ArrayList<>();
	private EvaluationEnvironment evalEnv;

	public JClosure(List<JavaInvokableMethod> methods, EvaluationEnvironment evalEnv) {
		this.methods = methods;
		this.evalEnv = evalEnv;
		Collections.sort(this.methods, new Comparator<JavaInvokableMethod>() {
			@Override
			public int compare(JavaInvokableMethod o1, JavaInvokableMethod o2) {
				if (o1.getParameterTypes().length > o2.getParameterTypes().length)
					return 1;
				else if (o1.getParameterTypes().length < o2.getParameterTypes().length)
					return -1;

				Class[] args1 = o1.getParameterTypes();
				Class[] args2 = o2.getParameterTypes();
				for (int i = 0; i < args1.length; i++) {
					if (!args2[i].isAssignableFrom(args1[i])) {
						return 1;
					}
				}
				return -1;
			}
		});
	}

	private Type[] fromValue(Value value) {
		if (value.getType() instanceof Tuple) {
			return ((Tuple) value.getType()).getTypeArray();
		} else {
			return new Type[] { value.getType() };
		}
	}

	private Value[] vFromV(Value value) {
		if (value instanceof TupleValue) {
			return ((TupleValue)value).getValues();
		} else {
			return new Value[] { value };
		}
	}

	@Override
	public Type getType() {
		return null;
	}

	@Override
	public Map<String, TypedAST> getChildren() {
		return new HashMap<>();
	}

	@Override
	public TypedAST cloneWithChildren(Map<String, TypedAST> newChildren) {
		return this;
	}

    @Override
	public FileLocation getLocation() {
		return null;
	}

	@Override
	public Expression generateIL(GenContext ctx, ValueType expectedType, List<TypedModuleSpec> dependencies) {
		// TODO Auto-generated method stub
		return null;
	}
}
