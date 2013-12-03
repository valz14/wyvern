package wyvern.tools.parsing.resolvers;

import wyvern.DSL.DSL;
import wyvern.tools.parsing.ContParser;
import wyvern.tools.typedAST.core.binding.*;
import wyvern.tools.typedAST.core.declarations.ClassDeclaration;
import wyvern.tools.typedAST.extensions.interop.java.Util;
import wyvern.tools.typedAST.extensions.interop.java.typedAST.JavaClassDecl;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.extensions.ClassType;
import wyvern.tools.util.CompilationContext;
import wyvern.tools.util.Pair;
import wyvern.tools.util.Reference;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class JavaImportResolver implements ImportEnvResolver {
	@Override
	public boolean checkURI(URI path) {
		return path.getScheme().equals("java");
	}

	@Override
	public String getDefaultName(URI ref) {
		String[] pathParts = ref.getSchemeSpecificPart().split("\\.");
		return pathParts[pathParts.length - 1];
	}

	private HashMap<String, ClassDeclaration> resolved = new HashMap<>();

	@Override
	public Pair<Environment, ContParser> resolveImport(URI uri, List<DSL> dsls, CompilationContext ctx) throws ClassNotFoundException {
		String path = uri.getSchemeSpecificPart();
		if (resolved.containsKey(path)) {
			ClassDeclaration declaration = resolved.get(path);
			return new Pair<Environment, ContParser>(declaration.extend(Environment.getEmptyEnvironment()), new ContParser.EmptyWithAST(declaration));
		}
		Class toImport = JavaImportResolver.class.getClassLoader().loadClass(path);
		ClassDeclaration javaClassDecl = Util.javaToWyvDecl(toImport);
		resolved.put(path, javaClassDecl);
		return new Pair<Environment, ContParser>(javaClassDecl.extend(Environment.getEmptyEnvironment()), new ContParser.EmptyWithAST(javaClassDecl));
	}

	@Override
	public TypedAST initalize(URI uri, ArrayList<DSL> dsls, CompilationContext ctx) {
		String path = uri.getSchemeSpecificPart();
		if (resolved.containsKey(path)) {
			ClassDeclaration declaration = resolved.get(path);
			return declaration;
		}
		Pair<Environment, ContParser> resolve = null;
		try {
			resolve = resolveImport(uri,dsls,ctx);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		return resolve.second.parse(new ContParser.SimpleResolver(ctx.getEnv()));
	}

	@Override
	public Environment doExtend(Environment old, String src, final Reference<TypedAST> typedAST) {
		if (typedAST.get() == null) {
			LateBinder<Type> binder = new LateBinder<Type>() {
				@Override
				public Type get() {
					if (!(typedAST.get() instanceof JavaClassDecl))
						throw new RuntimeException();
					JavaClassDecl jcd = (JavaClassDecl) typedAST.get();
					return jcd.getClassType();
				}
			};
			return old.extend(new LateNameBinding(src, binder)).extend(new LateTypeBinding(src, binder));
		}
		if (!(typedAST.get() instanceof JavaClassDecl))
			throw new RuntimeException();
		JavaClassDecl jcd = (JavaClassDecl) typedAST.get();
		return jcd.extend(old);
	}

	@Override
	public Environment extendWithValue(Environment old, TypedAST typedAST) {
		if (!(typedAST instanceof JavaClassDecl))
			throw new RuntimeException();
		JavaClassDecl jcd = (JavaClassDecl) typedAST;
		return jcd.extendWithValue(old);
	}

	@Override
	public void evalDecl(Environment evalEnv, Environment declEnv, TypedAST typedAST) {
		if (!(typedAST instanceof JavaClassDecl))
			throw new RuntimeException();
		JavaClassDecl jcd = (JavaClassDecl) typedAST;
		jcd.evalDecl(evalEnv, declEnv);
	}
}
