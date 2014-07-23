package wyvern.tools.typedAST.abs;

import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.util.Pair;

import java.util.Optional;

public abstract class AbstractValue extends AbstractTypedAST implements Value {

	@Override
	public Environment analyze(Type expected, Environment env) {
		return getType().subtype(expected,env).orElseThrow(RuntimeException::new);
	}

	@Override
	public Pair<Type, Environment> synthesize(Environment env) {
		return new Pair<>(getType(), env);
	}

	@Override
	public Value evaluate(Environment env) {
		return this;
	}
}
