package wyvern.tools.typedAST.abs;

import java.util.Map;

import wyvern.tools.typedAST.interfaces.ExpressionAST;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.types.Type;

public abstract class CachingTypedAST extends AbstractExpressionAST implements ExpressionAST {
    private Type type;

    public final Type getType() {
        if (type == null) {
            throw new RuntimeException("called getType() before typechecking");
        }
        return type;
    }

    protected abstract ExpressionAST doClone(Map<String, TypedAST> nc);

    @Override
    public ExpressionAST cloneWithChildren(Map<String, TypedAST> nc) {
        ExpressionAST res = doClone(nc);
        if (res instanceof CachingTypedAST) {
            ((CachingTypedAST) res).type = type;
        }
        return res;
    }
}
