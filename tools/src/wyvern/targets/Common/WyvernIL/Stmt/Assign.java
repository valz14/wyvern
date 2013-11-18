package wyvern.targets.Common.WyvernIL.Stmt;

import wyvern.targets.Common.WyvernIL.Expr.Expression;
import wyvern.targets.Common.WyvernIL.Imm.Operand;
import wyvern.targets.Common.WyvernIL.Imm.VarRef;
import wyvern.targets.Common.WyvernIL.visitor.StatementVisitor;

public class Assign implements Statement {

	private VarRef dest;
	private Operand src;

	public Assign(VarRef dest, Operand src) {
		this.dest = dest;
		this.src = src;
	}

	@Override
	public <R> R accept(StatementVisitor<R> visitor) {
		return visitor.visit(this);
	}

	public VarRef getDest() {
		return dest;
	}

	public Operand getSrc() {
		return src;
	}
}
