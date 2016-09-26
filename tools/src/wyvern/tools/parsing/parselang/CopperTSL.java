package wyvern.tools.parsing.parselang;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.tools.JavaCompiler;
import javax.tools.StandardLocation;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.CheckMethodAdapter;

import edu.umn.cs.melt.copper.compiletime.logging.CompilerLogMessage;
import edu.umn.cs.melt.copper.compiletime.logging.CompilerLogger;
import edu.umn.cs.melt.copper.compiletime.logging.PrintCompilerLogHandler;
import edu.umn.cs.melt.copper.compiletime.logging.messages.GrammarSyntaxError;
import edu.umn.cs.melt.copper.compiletime.spec.grammarbeans.CopperElementName;
import edu.umn.cs.melt.copper.compiletime.spec.grammarbeans.CopperElementType;
import edu.umn.cs.melt.copper.compiletime.spec.grammarbeans.DisambiguationFunction;
import edu.umn.cs.melt.copper.compiletime.spec.grammarbeans.GrammarElement;
import edu.umn.cs.melt.copper.compiletime.spec.grammarbeans.NonTerminal;
import edu.umn.cs.melt.copper.compiletime.spec.grammarbeans.ParserBean;
import edu.umn.cs.melt.copper.compiletime.spec.grammarbeans.Production;
import edu.umn.cs.melt.copper.compiletime.spec.grammarbeans.Terminal;
import edu.umn.cs.melt.copper.main.CopperDumpControl;
import edu.umn.cs.melt.copper.main.CopperDumpType;
import edu.umn.cs.melt.copper.main.CopperIOType;
import edu.umn.cs.melt.copper.main.ParserCompiler;
import edu.umn.cs.melt.copper.main.ParserCompilerParameters;
import edu.umn.cs.melt.copper.runtime.auxiliary.Pair;
import edu.umn.cs.melt.copper.runtime.engines.single.SingleDFAEngine;
import edu.umn.cs.melt.copper.runtime.logging.CopperException;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.parsing.ParseBuffer;
import wyvern.tools.parsing.parselang.java.StoringClassLoader;
import wyvern.tools.parsing.parselang.java.StoringFileManager;
import wyvern.tools.parsing.parselang.java.StringFileObject;
import wyvern.tools.typedAST.core.Sequence;
import wyvern.tools.typedAST.core.binding.NameBinding;
import wyvern.tools.typedAST.core.binding.NameBindingImpl;
import wyvern.tools.typedAST.core.declarations.ClassDeclaration;
import wyvern.tools.typedAST.core.declarations.DeclSequence;
import wyvern.tools.typedAST.core.declarations.DefDeclaration;
import wyvern.tools.typedAST.core.declarations.ValDeclaration;
import wyvern.tools.typedAST.core.expressions.Application;
import wyvern.tools.typedAST.core.expressions.Invocation;
import wyvern.tools.typedAST.core.expressions.New;
import wyvern.tools.typedAST.core.expressions.TupleObject;
import wyvern.tools.typedAST.core.expressions.Variable;
import wyvern.tools.typedAST.core.values.StringConstant;
import wyvern.tools.typedAST.core.values.UnitVal;
import wyvern.tools.typedAST.extensions.ExternalFunction;
import wyvern.tools.typedAST.extensions.SpliceBindExn;
import wyvern.tools.typedAST.extensions.interop.java.Util;
import wyvern.tools.typedAST.extensions.interop.java.typedAST.JavaClassDecl;
import wyvern.tools.typedAST.interfaces.ExpressionAST;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.UnresolvedType;
import wyvern.tools.types.extensions.Arrow;
import wyvern.tools.types.extensions.ClassType;
import wyvern.tools.types.extensions.Int;
import wyvern.tools.types.extensions.Str;
import wyvern.tools.types.extensions.Tuple;
import wyvern.tools.types.extensions.Unit;
import wyvern.tools.util.LangUtil;
import wyvern.tools.util.Reference;

public class CopperTSL {
	private int foo;
	public CopperTSL(int k) {
		foo = 0;
	}
	public CopperTSL() {

	}

	private static final String PAIRED_OBJECT_NAME = "innerObj$wyv";

	private static class IParseBuffer extends ParseBuffer {
		IParseBuffer(String str) {
			super(str);
		}
	}

	private static class CopperGrammarException extends RuntimeException {
		private GrammarSyntaxError gse;

		public CopperGrammarException(GrammarSyntaxError gse) {

			this.gse = gse;
		}


		public GrammarSyntaxError getGse() {
			return gse;
		}
	}

	private Consumer<? super DisambiguationFunction> updateDisambiguationCode(HashMap<String, Pair<Type, SpliceBindExn>> toGen,
																			  Environment ntEnv, Reference<Integer> methNum) {
		return (dis) -> {
			String disambiguationCode = dis.getCode();

			List<NameBinding> argNames = dis.getMembers().stream().map(cer->cer.getName().toString())
					.map(name -> new NameBindingImpl(name, new Int())).collect(Collectors.toList());

			argNames.add(new NameBindingImpl("lexeme", new Str()));

			SpliceBindExn spliced = LangUtil.spliceBinding(new IParseBuffer(disambiguationCode), argNames, dis.getDisplayName());

			CopperElementName newName = dis.getName();
			String nextName = getNextName(methNum, newName);
			toGen.put(nextName, new Pair<>(new Int(), spliced));
			dis.setCode(String.format("return ((IntegerConstant)Util.invokeValueVarargs(%s, \"%s\", %s)).getValue();", PAIRED_OBJECT_NAME, nextName,
					argNames.stream().map(str -> (str.getType() instanceof Int) ? "new IntegerConstant(" + str.getName() + ")" : "new StringConstant(" + str.getName() + ")").reduce((a, b) -> a + ", " + b).get()));
		};
	}

	private Pair<String, Type> parseType(GrammarElement elem) {
		if (elem instanceof Terminal)
			return this.parseType((Terminal)elem);
		if (elem instanceof NonTerminal)
			return this.parseType((NonTerminal)elem);
		throw new RuntimeException();
	}

	private Consumer<Terminal> updateTerminalCode(HashMap<String, Pair<Type,SpliceBindExn>> toGen, Environment lhsEnv, Reference<Integer> methNum, String javaTypeName, List<BiConsumer<Type,Type>> splicers) {
		return (term) -> {
			String oCode = term.getCode();

			CopperElementName termName = term.getName();
			String newName = getNextName(methNum, termName);

			Type resType = lhsEnv.lookup(term.getName().toString()).getType();

			splicers.add((termClassType,termObjType) -> {
						SpliceBindExn spliced = LangUtil.spliceBinding(new IParseBuffer(oCode), Arrays.asList(new NameBinding[]{
								new NameBindingImpl("lexeme", new Str()),
								new NameBindingImpl("pushToken", new Arrow(new Tuple(termObjType, new Str()), new Unit())),
								new NameBindingImpl("Terminals", termClassType)}), term.getDisplayName());

						toGen.put(newName, new Pair<>(resType, spliced));
					});

			String newCode = String.format("RESULT = Util.invokeValueVarargs(%s, \"%s\", %s);", PAIRED_OBJECT_NAME, newName, "new StringConstant(lexeme), pushTokenV, terminals");
			term.setCode(newCode);
		};
	}

	private String getNextName(Reference<Integer> methNum, CopperElementName termName) {
		String newName = termName + "GEN" + methNum.get();
		methNum.set(methNum.get() + 1);
		return newName;
	}

	private Consumer<Pair<Production, List<NameBinding>>> updateCode(HashMap<String, Pair<Type,SpliceBindExn>> toGen, Environment lhsEnv, Reference<Integer> methNum, String thisTypeName) {
		return (Pair<Production, List<NameBinding>> inp) -> {
			Production prod = inp.first();
			List<NameBinding> bindings = inp.second();
			Util.javaToWyvDecl(CupSkinParser.Terminals.class);
			//Generate the new Wyvern method name
			String newName = getNextName(methNum, prod.getName());

			//Parse the input code
			SpliceBindExn spliced = LangUtil.spliceBinding(new IParseBuffer(prod.getCode()), bindings);

			Type resType = lhsEnv.lookup(prod.getLhs().getName().toString()).getType();

			//Save it to the external dict
			toGen.put(newName, new Pair<>(resType, spliced));

			//Code to invoke the equivalent function
			String argsStr = bindings.stream().map(nb->nb.getName()).reduce((a,b)->a+", "+b)
					.map(arg -> ", " + arg).orElseGet(() -> "");
			String newCode = "RESULT = Util.invokeValueVarargs(" + PAIRED_OBJECT_NAME + ", \"" + newName +"\"" + argsStr + ");";

			prod.setCode(newCode);
		};
	}

	//Via stackoverflow and the old Java zip
	private static<A, B, C> Stream<C> zip(Stream<? extends A> a,
										 Stream<? extends B> b,
										 BiFunction<? super A, ? super B, ? extends C> zipper) {
		Objects.requireNonNull(zipper);
		@SuppressWarnings("unchecked")
		Spliterator<A> aSpliterator = (Spliterator<A>) Objects.requireNonNull(a).spliterator();
		@SuppressWarnings("unchecked")
		Spliterator<B> bSpliterator = (Spliterator<B>) Objects.requireNonNull(b).spliterator();

		// Zipping looses DISTINCT and SORTED characteristics
		int both = aSpliterator.characteristics() & bSpliterator.characteristics() &
				~(Spliterator.DISTINCT | Spliterator.SORTED);
		int characteristics = both;

		long zipSize = ((characteristics & Spliterator.SIZED) != 0)
				? Math.min(aSpliterator.getExactSizeIfKnown(), bSpliterator.getExactSizeIfKnown())
				: -1;

		Iterator<A> aIterator = Spliterators.iterator(aSpliterator);
		Iterator<B> bIterator = Spliterators.iterator(bSpliterator);
		Iterator<C> cIterator = new Iterator<C>() {
			@Override
			public boolean hasNext() {
				return aIterator.hasNext() && bIterator.hasNext();
			}

			@Override
			public C next() {
				return zipper.apply(aIterator.next(), bIterator.next());
			}
		};

		Spliterator<C> split = Spliterators.spliterator(cIterator, zipSize, characteristics);
		return (a.isParallel() || b.isParallel())
				? StreamSupport.stream(split, true)
				: StreamSupport.stream(split, false);
	}

}
