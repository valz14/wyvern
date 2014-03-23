package wyvern.tools.bytecode.visitors;

import java.util.ArrayList;
import java.util.List;

import wyvern.targets.Common.WyvernIL.Imm.BoolValue;
import wyvern.targets.Common.WyvernIL.Imm.IntValue;
import wyvern.targets.Common.WyvernIL.Imm.Operand;
import wyvern.targets.Common.WyvernIL.Imm.StringValue;
import wyvern.targets.Common.WyvernIL.Imm.TupleValue;
import wyvern.targets.Common.WyvernIL.Imm.UnitValue;
import wyvern.targets.Common.WyvernIL.Imm.VarRef;
import wyvern.targets.Common.WyvernIL.visitor.OperandVisitor;
import wyvern.tools.bytecode.core.BytecodeContext;
import wyvern.tools.bytecode.values.BytecodeBoolean;
import wyvern.tools.bytecode.values.BytecodeInt;
import wyvern.tools.bytecode.values.BytecodeString;
import wyvern.tools.bytecode.values.BytecodeTuple;
import wyvern.tools.bytecode.values.BytecodeValue;

/**
 * an OperandVisitor for the IL interpreter
 * @author Tal Man
 *
 */
public class BytecodeOperandVisitor implements OperandVisitor<BytecodeValue> {

	private final BytecodeContext context;
	
	/**
	 * sets up the visitor with a context to work with
	 * @param visContext
	 * 		the context of the program at this point
	 */
	public BytecodeOperandVisitor(BytecodeContext visContext) {
		context = visContext;
	}
	
	@Override
	public BytecodeValue visit(BoolValue boolValue) {
		return new BytecodeBoolean(boolValue.isValue());
	}

	@Override
	public BytecodeValue visit(IntValue intValue) {
		return new BytecodeInt(intValue.getValue());
	}

	@Override
	public BytecodeValue visit(VarRef varRef) {
		// probably going to rework this after i understand the system better
		return context.getValue(varRef.getName()).dereference();
	}

	@Override
	public BytecodeValue visit(StringValue stringValue) {
		return new BytecodeString(stringValue.isValue());
	}

	@Override
	public BytecodeValue visit(TupleValue tupleValue) {
		List<Operand> operands = tupleValue.getOperands();
		List<BytecodeValue> values = new ArrayList<BytecodeValue>();
		for(Operand op : operands) {
			values.add(op.accept(this));
		}
		return new BytecodeTuple(values);
	}

	/*
	 *  unit value represented as an empty tuple
	 */
	@Override
	public BytecodeValue visit(UnitValue unitValue) {
		return new BytecodeTuple(new ArrayList<BytecodeValue>());
	}

}