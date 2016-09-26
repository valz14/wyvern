package wyvern.tools.types.extensions;

import static wyvern.tools.errors.ErrorMessage.OPERATOR_DOES_NOT_APPLY;
import static wyvern.tools.errors.ErrorMessage.OPERATOR_DOES_NOT_APPLY2;
import static wyvern.tools.errors.ToolError.reportError;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.typedAST.core.expressions.Invocation;
import wyvern.tools.typedAST.extensions.interop.java.Util;
import wyvern.tools.typedAST.extensions.interop.java.types.JavaClassType;
import wyvern.tools.types.AbstractTypeImpl;
import wyvern.tools.types.Environment;
import wyvern.tools.types.OperatableType;
import wyvern.tools.types.SubtypeRelation;
import wyvern.tools.types.Type;
import wyvern.tools.util.TreeWriter;


public class Int extends AbstractTypeImpl implements OperatableType {
	public Int() { }
	
	private static final Set<String> legalOperators = new HashSet<String>(Arrays.asList(new String[] {
			"+",
			"-",
			"*",
			"/",
			">",
			"<",
			">=",
			"<=",
			"==",
			"!=",
	}));
	
	@Override
	public String toString() {
		return "Int";
	}

	private boolean isRelationalOperator(String operatorName) {
		return operatorName.equals(">") || operatorName.equals("<") || operatorName.equals("!=")
			|| operatorName.equals(">=") || operatorName.equals("<=") || operatorName.equals("==")	
			|| operatorName.equals("!=");
	}

	@Override
	public boolean subtype(Type other, HashSet<SubtypeRelation> subtypes) {
		if (other instanceof JavaClassType) {
			if (Util.javaToWyvType(Integer.class).subtype(other, subtypes))
				return true;
			return ((JavaClassType) other).getInnerClass().equals(Object.class);
		}
		return other instanceof Int;
	}


	@Override
	public Map<String, Type> getChildren() {
		return new HashMap<>();
	}

	@Override
	public Type cloneWithChildren(Map<String, Type> newChildren) {
		return this;
	}

    @Override
    public ValueType generateILType() {
        return wyvern.target.corewyvernIL.support.Util.intType();
    }

    @Override
	public boolean equals(Object other) { return other instanceof Int; }

	@Override
	public ValueType getILType(GenContext ctx) {
		return wyvern.target.corewyvernIL.support.Util.intType();
	}
}
