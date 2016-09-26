package wyvern.tools.tests;

import java.io.Reader;
import java.io.StringReader;
import java.util.Optional;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import wyvern.stdlib.Globals;
import wyvern.target.corewyvernIL.expression.IExpr;
import wyvern.target.corewyvernIL.expression.IntegerLiteral;
import wyvern.target.corewyvernIL.expression.Variable;
import wyvern.target.corewyvernIL.support.EvalContext;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.support.Util;
import wyvern.tools.parsing.coreparser.ParseException;
import wyvern.tools.parsing.coreparser.ParseUtils;
import wyvern.tools.parsing.coreparser.WyvernParser;
import wyvern.tools.tests.TestUtil;
import wyvern.tools.tests.suites.CurrentlyBroken;
import wyvern.tools.tests.suites.RegressionTests;
import wyvern.tools.typedAST.core.Sequence;
import wyvern.tools.typedAST.core.values.IntegerConstant;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.types.Type;
import wyvern.tools.types.extensions.Int;

@Category(RegressionTests.class)
public class CoreParserTests {
    @BeforeClass public static void setupResolver() {
    	TestUtil.setPaths();
    }

	@Test
	public void testIdentity() throws ParseException {
		String input = "fn x : Int => x";
		Reader r = new StringReader(input);
		//TokenManager tm = new WyvernTokenManager(r, "test input");
		WyvernParser<TypedAST,Type> wp = ParseUtils.makeParser("test input", r);//new WyvernParser(tm);
		wp.Expression(null);
	}
	
	@Test
	public void testIdentityApp() throws ParseException {
		String input = "((x : Int) => x)(3)";
		Reader r = new StringReader(input);
		//TokenManager tm = new WyvernTokenManager(r, "test input");
		//WyvernParser wp = new WyvernParser(tm);
		WyvernParser<TypedAST,Type> wp = ParseUtils.makeParser("test input", r);
		TypedAST testAST = wp.Expression(null);
       
        TestUtil.doTest(input, Util.intType(), new IntegerLiteral(3));
	}
	
	@Test
	public void testValVar() throws ParseException {
		String input = "require stdout\n\n"
					 + "val x = \"Hello, \"\n"
					 + "var y : Str = \"World\"\n"
					 + "val z : Str = \"!\"\n"
				     + "stdout.print(x)\n"
				     + "stdout.print(y)\n"
				     + "stdout.print(z)\n";
		TypedAST ast = TestUtil.getNewAST(input, "test input");
        GenContext genCtx = GenContext.empty().extend("system", new Variable("system"), null);
        IExpr program = ((Sequence) ast).generateIL(genCtx, null, null);
        TypeContext ctx = TypeContext.empty();
        program.interpret(EvalContext.empty());
	}
	
	@Test
	public void testNewInvoke() throws ParseException {
		String input = "val obj = new\n"
				     + "    def getValue():Int\n"
				     + "        5\n"
				     + "obj.getValue()\n"
				     ;
        TestUtil.doTest(input, Util.intType(), new IntegerLiteral(5));
	}
	
	@Test
	public void testFieldRead() throws ParseException {
		String input = "val obj = new\n"
				     + "    val v:Int = 5\n"
				     + "obj.v\n"
				     ;
        TestUtil.doTest(input, Util.intType(), new IntegerLiteral(5));
	}

	@Test
	public void testVarField() throws ParseException {
		String input = "val obj = new\n"
				     + "    var v:Int = 5\n"
				     + "obj.v = 3\n"
				     + "obj.v\n"
				     ;
        TestUtil.doTest(input, Util.intType(), new IntegerLiteral(3));
	}
	
	@Test
	public void testTypeDecl() throws ParseException {
		String input = ""
			         + "type ValHolder\n"
				     + "    def getValue():Int\n"
				     + "val obj : ValHolder = new\n"
				     + "    def getValue():Int\n"
				     + "        5\n"
				     + "obj.getValue()\n"
				     ;
        TestUtil.doTest(input, Util.intType(), new IntegerLiteral(5));
	}
}
