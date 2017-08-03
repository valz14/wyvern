/**
 * @author vzhao
 */
package wyvern.target.corewyvernIL.expression;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import wyvern.target.corewyvernIL.astvisitor.ASTVisitor;
import wyvern.target.corewyvernIL.decltype.DeclType;
import wyvern.target.corewyvernIL.decltype.EffectDeclType;
import wyvern.target.corewyvernIL.support.EvalContext;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.support.View;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.FileLocation;

public class Effect {
	private Path path;
	private String name;
	private FileLocation loc;
	
	public Effect(Variable p, String n, FileLocation l) {
		path = p;
		name = n;
		loc = l;
	}

	public Variable getPath() {
		return (Variable) path;
	}
	
	public void setPath(Path p) { // for effects defined in the same signature (whose paths are null until typechecked)
		path = p;
	}
	
	public String getName() {
		return name;
	}
	
	public FileLocation getLocation() {
		return loc;
	}
	
	public void doPrettyPrint(Appendable dest, String indent) throws IOException {
		dest.append(indent).append(getPath().getName()).append(".").append(getName());
		dest.append('\n');
	}
	
	@Override
	public String toString() {
		return (path==null? "" : getPath().getName() + ".") + getName(); 
	}
	
	public DeclType getDeclType(Set<Effect> effects) {
		return new EffectDeclType(getName(), effects, getLocation());
	}
	
	public Path adapt(View v) {
		if (getName()=="send") 
			System.out.println("send");
		return getPath().adapt(v);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj==null) return false;
		if (!(obj instanceof Effect)) return false;	
		
		Effect eObj = (Effect) obj;
		if (eObj.getName().equals(getName()) &&
				eObj.getPath().equals(getPath())) return true;
		return false;
	}
}