package wyvern.target.corewyvernIL.astvisitor;

import wyvern.target.corewyvernIL.Case;
import wyvern.target.corewyvernIL.FormalArg;
import wyvern.target.corewyvernIL.decl.DefDeclaration;
import wyvern.target.corewyvernIL.decl.DelegateDeclaration;
import wyvern.target.corewyvernIL.decl.EffectDeclaration;
import wyvern.target.corewyvernIL.decl.ModuleDeclaration;
import wyvern.target.corewyvernIL.decl.TypeDeclaration;
import wyvern.target.corewyvernIL.decl.ValDeclaration;
import wyvern.target.corewyvernIL.decl.VarDeclaration;
import wyvern.target.corewyvernIL.decltype.AbstractTypeMember;
import wyvern.target.corewyvernIL.decltype.ConcreteTypeMember;
import wyvern.target.corewyvernIL.decltype.DefDeclType;
import wyvern.target.corewyvernIL.decltype.EffectDeclType;
import wyvern.target.corewyvernIL.decltype.ValDeclType;
import wyvern.target.corewyvernIL.decltype.VarDeclType;
import wyvern.target.corewyvernIL.expression.Bind;
import wyvern.target.corewyvernIL.expression.BooleanLiteral;
import wyvern.target.corewyvernIL.expression.Cast;
import wyvern.target.corewyvernIL.expression.FFI;
import wyvern.target.corewyvernIL.expression.FFIImport;
import wyvern.target.corewyvernIL.expression.FieldGet;
import wyvern.target.corewyvernIL.expression.FieldSet;
import wyvern.target.corewyvernIL.expression.IntegerLiteral;
import wyvern.target.corewyvernIL.expression.Let;
import wyvern.target.corewyvernIL.expression.Match;
import wyvern.target.corewyvernIL.expression.MethodCall;
import wyvern.target.corewyvernIL.expression.New;
import wyvern.target.corewyvernIL.expression.RationalLiteral;
import wyvern.target.corewyvernIL.expression.StringLiteral;
import wyvern.target.corewyvernIL.expression.Variable;
import wyvern.target.corewyvernIL.type.DataType;
import wyvern.target.corewyvernIL.type.ExtensibleTagType;
import wyvern.target.corewyvernIL.type.NominalType;
import wyvern.target.corewyvernIL.type.StructuralType;
import wyvern.target.corewyvernIL.type.ValueType;

public abstract class ASTVisitor<S, T> {
	public abstract T visit(S state, New newExpr);
	public abstract T visit(S state, Case c);
	public abstract T visit(S state, MethodCall methodCall);
	public abstract T visit(S state, Match match);
	public abstract T visit(S state, FieldGet fieldGet);
	public abstract T visit(S state, Let let);
	public abstract T visit(S state, Bind bind);
	public abstract T visit(S state, FieldSet fieldSet);
	public abstract T visit(S state, Variable variable);
	public abstract T visit(S state, Cast cast);
	public abstract T visit(S state, VarDeclaration varDecl);
	public abstract T visit(S state, DefDeclaration defDecl);
	public abstract T visit(S state, ValDeclaration valDecl);
  public abstract T visit(S state, ModuleDeclaration moduleDecl);
	public abstract T visit(S state, IntegerLiteral integerLiteral);
  public abstract T visit(S state, BooleanLiteral booleanLiteral);
	public abstract T visit(S state, RationalLiteral rational);
	public abstract T visit(S state, FormalArg formalArg);
	public abstract T visit(S state, VarDeclType varDeclType);
	public abstract T visit(S state, ValDeclType valDeclType);
	//public abstract T visit(S state, DependentType dependentType);
	public abstract T visit(S state, DefDeclType defDeclType);
	public abstract T visit(S state, AbstractTypeMember abstractDeclType);
	public abstract T visit(S state, NominalType nominalType);
	public abstract T visit(S state, StructuralType structuralType);
	public abstract T visit(S state, StringLiteral stringLiteral);
	public abstract T visit(S state, DelegateDeclaration delegateDecl);
	public abstract T visit(S state, ConcreteTypeMember concreteTypeMember);
	public abstract T visit(S state, TypeDeclaration typeDecl);
  public abstract T visit(S state, ValueType valueType);
  public abstract T visit(S state, ExtensibleTagType extensibleTagType);
  public abstract T visit(S state, DataType dataType);
  public abstract T visit(S state, FFIImport ffiImport);
  public abstract T visit(S state, FFI ffi);
public  abstract T visit(S state, EffectDeclaration effectDeclaration) ;
public T visit(S state, EffectDeclType effectDeclType) {
	// TODO Auto-generated method stub
	return null;
}
}
