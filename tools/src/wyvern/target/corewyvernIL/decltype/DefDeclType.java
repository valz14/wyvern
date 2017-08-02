package wyvern.target.corewyvernIL.decltype;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

import wyvern.tools.typedAST.core.declarations.DefDeclaration;
import wyvern.target.corewyvernIL.FormalArg;
import wyvern.target.corewyvernIL.astvisitor.ASTVisitor;
import wyvern.target.corewyvernIL.expression.Effect;
import wyvern.target.corewyvernIL.expression.EffectAccumulator;
import wyvern.target.corewyvernIL.expression.Variable;
import wyvern.target.corewyvernIL.support.ReceiverView;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.support.View;
import wyvern.target.corewyvernIL.support.ViewExtension;
import wyvern.target.corewyvernIL.type.NominalType;
import wyvern.target.corewyvernIL.type.ValueType;


public class DefDeclType extends DeclTypeWithResult {

	private List<FormalArg> args;
	private Set<Effect> effects;
	
	public DefDeclType(String method, ValueType returnType, List<FormalArg> args) {
		this(method, returnType, args, null);
	}
	
	public DefDeclType(String method, ValueType returnType, List<FormalArg> args, Set<Effect> effects) {
		super(method, returnType);
		this.args = args;
		this.effects = effects;
	}

	public List<FormalArg> getFormalArgs ()
	{
		return args;
	}

	public Set<Effect> getEffectSet() {
		return effects;
	}
	
	public void setEffects(Set<Effect> e) {
		if (!e.equals(effects)) { effects = e;}
	}
	
	@Override
	public <S, T> T acceptVisitor(ASTVisitor <S, T> emitILVisitor,
			S state) {
		return emitILVisitor.visit(state, this);
	}

	@Override
	public boolean isSubtypeOf(DeclType dt, TypeContext ctx) {
		if (!(dt instanceof DefDeclType)) {
			return false;
		}
		DefDeclType ddt = (DefDeclType) dt;
		if (args.size() != ddt.args.size() || !ddt.getName().equals(getName()))
			return false;
		View adaptationView = null;
		for (int i = 0; i < args.size(); ++i) {
			// x:A * B -> C <: y:A' * B' -> C'
			// x in scope when analyzing B, B', C, and C'
			// not y, because y's assumptions might be stronger than x's
			// so replace y with x in B' and C'
			// here y "from" is dt, x "to" is this
			FormalArg myArg = args.get(i);
			FormalArg theirArg = ddt.args.get(i);
			ValueType theirType = theirArg.getType();
			if (adaptationView == null) {
				adaptationView = new ReceiverView(new Variable(theirArg.getName()), new Variable(myArg.getName()));
			} else {
				theirType = theirType.adapt(adaptationView);
				adaptationView = new ViewExtension(new Variable(theirArg.getName()), new Variable(myArg.getName()), adaptationView);
			}
			if (! (theirType.isSubtypeOf(myArg.getType(), ctx))) {
				return false;
			}
			ctx = ctx.extend(myArg.getName(), myArg.getType());
		}
		ValueType rawResultType = this.getRawResultType();
		ValueType otherRawResultType = ddt.getRawResultType();
		if (adaptationView != null)
			otherRawResultType = otherRawResultType.adapt(adaptationView);
		return rawResultType.isSubtypeOf(otherRawResultType, ctx);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
		result = prime * result + ((getRawResultType() == null) ? 0 : getRawResultType().hashCode());
		result = prime * result + ((args == null) ? 0 : args.hashCode());
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
		DefDeclType other = (DefDeclType) obj;
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
		if (args == null) {
			if (other.args != null)
				return false;
		} else if (!args.equals(other.args))
			return false;
		return true;
	}

	@Override
	public void doPrettyPrint(Appendable dest, String indent) throws IOException {
		dest.append(indent).append("def ").append(getName()).append('(');
		boolean first = true;
		for (FormalArg arg: args) {
			if (first)
				first = false;
			else
				dest.append(", ");
			arg.doPrettyPrint(dest, indent);
		}
		String newIndent = indent+"    ";
		dest.append(") : ");
		getRawResultType().doPrettyPrint(dest, newIndent);
		dest.append('\n');
	}

	@Override
	public DeclType adapt(View v) {
		List<FormalArg> newArgs = new LinkedList<FormalArg>();
		for (FormalArg a : args) {
			newArgs.add(new FormalArg(a.getName(), a.getType().adapt(v)));
		}
		return new DefDeclType(this.getName(), this.getRawResultType().adapt(v), newArgs);
	}
	
	@Override
	public void checkWellFormed(TypeContext ctx) {
		for (FormalArg arg : args) {
			arg.getType().checkWellFormed(ctx);
		}
		super.checkWellFormed(ctx);
	}

	@Override
	public DeclType doAvoid(String varName, TypeContext ctx, int count) {
		boolean changed = false;
		ValueType t = this.getRawResultType().doAvoid(varName, ctx, count);
		if (!(t.equals(this.getRawResultType()))) {
			changed = true;
		}
		List<FormalArg> newArgs = new LinkedList<FormalArg>();
		for (FormalArg arg : args) {
			ValueType argT = arg.getType().doAvoid(varName, ctx, count);
			if (!argT.equals(arg.getType())) {
				changed = true;
			}
			newArgs.add(new FormalArg(arg.getName(), argT));
		}
		if (!changed)
			return this;
		else
			return new DefDeclType(this.getName(), t, newArgs);
	}
	
	@Override
	public boolean isTypeDecl() {
		return false;
	}

    /**
        genericMapping returns a map from each generic arguments the position in the formals list where the argument is used as a type
        If the argument is used as the result type, then the position is len(formals), i.e. the position appended to the end of the list
        Note that you can't infer from the result type, because evaluating the result type depends on the actuals, which are what we are trying to infer.
    */
    public  Map<Integer, List<Integer>> genericMapping() {
        Map<Integer, List<Integer>> inferenceMap = new HashMap<Integer, List<Integer>>();
        List<FormalArg> args = this.getFormalArgs();
        ValueType rawResultType = this.getRawResultType();

        for(int i = 0; i < args.size(); i++) {
            FormalArg arg = args.get(i);
            // Break out of the loop if we're done looking at generics
            if(!DefDeclaration.isGeneric(arg)) {
                break;
            }

            // Collect the symbolic identifier for this generic type
            String identifier = arg.getName().
                substring(DefDeclaration.GENERIC_PREFIX.length());

            // Now, see if we can find a location in the formals list
            // where this argument is used as a type
            for(int j = i; j < args.size(); j++) {
                ValueType maybeGeneric = args.get(j).getType();
                if(matchesGeneric(maybeGeneric, identifier)) {
                    // Then we can add this position to the inference map
                    append(inferenceMap, i, j);
                }
            }
        }
        return inferenceMap;
    }

    /**
     * Appends the element provided to list of mapped values in the hashmap provided
     * If the key isn't already in the map, then this function allocates the list and adds the element to it.
     * Otherwise, the element is appended to the end of the list.
     */
    private static <K, E> void  append(Map<K, List<E>> map, K key, E elem) {
        if(map.get(key) == null) {
            List<E> singleton = new LinkedList<>();
            singleton.add(elem);
            map.put(key, singleton);
        } else {
            map.get(key).add(elem);
        }
    }
    

    /**
    * @param maybeGeneric is the ValueType we're checking to see if it's a generic or not
    * @param identifier is the identifier (usually a single letter) for the generic type
    */
    private boolean matchesGeneric(ValueType maybeGeneric, String identifier) {
        if(maybeGeneric instanceof NominalType) {
            NominalType t = (NominalType) maybeGeneric;
            String mem = t.getTypeMember();

            // Check if the type member's name is the same as the generic type member
            return mem.equals(identifier);
        }
        return false;
    }

	@Override
	public ValueType typeCheck(TypeContext ctx, EffectAccumulator effectAccumulator) {
		// TODO Auto-generated method stub
		return null;
	}
}
