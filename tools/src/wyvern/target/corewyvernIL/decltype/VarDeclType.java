package wyvern.target.corewyvernIL.decltype;

import java.io.IOException;

import wyvern.target.corewyvernIL.IASTNode;
import wyvern.target.corewyvernIL.astvisitor.ASTVisitor;
import wyvern.target.corewyvernIL.expression.EffectAccumulator;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.support.View;
import wyvern.target.corewyvernIL.type.ValueType;

public class VarDeclType extends DeclTypeWithResult implements IASTNode{
	
	public VarDeclType(String name, ValueType type) {
		super(name, type);
	}

	@Override
	public <S, T> T acceptVisitor(ASTVisitor <S, T> emitILVisitor,
			S state) {
		return emitILVisitor.visit(state, this);
	}

	@Override
	public boolean isSubtypeOf(DeclType dt, TypeContext ctx) {
		if (!(dt instanceof VarDeclType))
			if (dt instanceof ValDeclType) {
				ValDeclType vdt = (ValDeclType) dt;
				return vdt.getName().equals(getName()) && this.getRawResultType().isSubtypeOf(vdt.getRawResultType(), ctx);
			} else {
				return false;
			}
		VarDeclType vdt = (VarDeclType) dt;
		return vdt.getName().equals(getName()) && this.getRawResultType().equalsInContext(vdt.getRawResultType(), ctx);
	}

	@Override
	public DeclType adapt(View v) {
		return new VarDeclType(getName(), this.getRawResultType().adapt(v));
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
		result = prime * result + ((getRawResultType() == null) ? 0 : getRawResultType().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VarDeclType other = (VarDeclType) obj;
		if (getName() == null) {
			if (other.getName() != null)
				return false;
		} else if (!getName().equals(other.getName()))
			return false;
		if (getRawResultType() == null) {
			if (other.getRawResultType() != null)
				return false;
		} else if (!getRawResultType().equals(other.getRawResultType()))
			return false;
		return true;
	}

	@Override
	public void doPrettyPrint(Appendable dest, String indent) throws IOException {
		dest.append(indent).append("var ").append(getName()).append(" : ");
		getRawResultType().doPrettyPrint(dest, indent);
		dest.append('\n');
	}
	@Override
	public DeclType doAvoid(String varName, TypeContext ctx, int count) {
		ValueType t = this.getRawResultType().doAvoid(varName, ctx, count);
		if (t.equals(this.getRawResultType())) {
			return this;
		} else {
			return new VarDeclType(this.getName(),t);
		}
	}
	
	@Override
	public boolean isTypeDecl() {
		return false;
	}

	@Override
	public ValueType typeCheck(TypeContext ctx, EffectAccumulator effectAccumulator) {
		// TODO Auto-generated method stub
		return null;
	}
}
