package wyvern.tools.types.extensions;

import java.util.LinkedList;

import wyvern.tools.errors.FileLocation;
import wyvern.tools.typedAST.abs.Declaration;
import wyvern.tools.typedAST.core.binding.Binding;
import wyvern.tools.typedAST.core.binding.NameBinding;
import wyvern.tools.typedAST.core.binding.NameBindingImpl;
import wyvern.tools.typedAST.core.binding.typechecking.AssignableNameBinding;
import wyvern.tools.typedAST.core.binding.typechecking.TypeBinding;
import wyvern.tools.typedAST.core.declarations.ClassDeclaration;
import wyvern.tools.typedAST.core.declarations.DeclSequence;
import wyvern.tools.typedAST.core.declarations.DefDeclaration;
import wyvern.tools.typedAST.core.declarations.TypeDeclaration;
import wyvern.tools.typedAST.core.declarations.ValDeclaration;
import wyvern.tools.typedAST.core.declarations.VarDeclaration;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;

/**
 * Useful type functionality
 */
public class TypeDeclUtils {
	public static Environment getTypeEquivalentEnvironment(Environment src) {
		Environment tev = Environment.getEmptyEnvironment();

		for (Binding b : src.getBindings()) {
			// System.out.println("Processing: " + b + " which class is " + b.getClass());
			
			if (b instanceof AssignableNameBinding) {
				//Indicates that there is a settable value
				String name = b.getName();
				Type type = b.getType();
				tev = tev.extend(
						new NameBindingImpl("set" + name.substring(0,1).toUpperCase() + name.substring(1),
						new Arrow(type, new Unit())));
				continue;
			}

			if (b instanceof TypeBinding) {
				if (b.getType() instanceof TypeType) {
					tev = tev.extend(b);
					continue;
				}
				if (b.getType() instanceof ClassType) {
					TypeType tt = ((ClassType) b.getType()).getEquivType();
					tev = tev.extend(new NameBindingImpl(b.getName(), tt));
					continue;
				}
				continue;
			}

			if (!(b instanceof NameBinding))
				continue;

			if (b.getType() instanceof Arrow) {
				tev = tev.extend(b);
				continue;
			}
			
			if (b.getType() instanceof TypeType || b.getType() instanceof ClassType) {
				tev = tev.extend(b);
				continue;
			}
			
			/* used to assume it was a getter here, but that seemed unjustified.
			 * Fixed to instead just copy the binding over

			// System.out.println("Assume it is a getter even if it is wrong! :-)");
			
			String propName = b.getName();
			Type type = b.getType();

			DefDeclaration getter = new DefDeclaration(propName, type,
					new LinkedList<NameBinding>(), null, false, FileLocation.UNKNOWN);

			tev = getter.extend(tev, tev);
			 */
			tev = tev.extend(b);
		}
		return tev;
	}
}
