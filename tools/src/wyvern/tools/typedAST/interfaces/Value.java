package wyvern.tools.typedAST.interfaces;

import wyvern.tools.types.Type;

public interface Value extends ExpressionAST {
    public Type getType();
}
