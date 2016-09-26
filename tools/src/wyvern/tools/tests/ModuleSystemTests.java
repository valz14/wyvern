package wyvern.tools.tests;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import wyvern.stdlib.Globals;
import wyvern.target.corewyvernIL.expression.Expression;
import wyvern.target.corewyvernIL.expression.IExpr;
import wyvern.target.corewyvernIL.expression.IntegerLiteral;
import wyvern.target.corewyvernIL.expression.Variable;
import wyvern.target.corewyvernIL.modules.TypedModuleSpec;
import wyvern.target.corewyvernIL.support.EvalContext;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.target.corewyvernIL.support.GenUtil;
import wyvern.target.corewyvernIL.support.TypeContext;
import wyvern.target.corewyvernIL.support.TypeGenContext;
import wyvern.target.corewyvernIL.support.Util;
import wyvern.target.corewyvernIL.type.NominalType;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.ToolError;
import wyvern.tools.imports.extensions.WyvernResolver;
import wyvern.tools.parsing.coreparser.ParseException;
import wyvern.tools.tests.suites.CurrentlyBroken;
import wyvern.tools.tests.suites.RegressionTests;
import wyvern.tools.tests.TestUtil;
import wyvern.tools.typedAST.abs.Declaration;
import wyvern.tools.typedAST.interfaces.ExpressionAST;
import wyvern.tools.typedAST.interfaces.TypedAST;

@Category(RegressionTests.class)
public class ModuleSystemTests {

	private static final String BASE_PATH = TestUtil.BASE_PATH;
	private static final String PATH = BASE_PATH + "modules/";

    @BeforeClass public static void setupResolver() {
    	TestUtil.setPaths();
		WyvernResolver.getInstance().addPath(PATH);
    }

	@Test
	public void testResource() throws ParseException {
		String program = TestUtil.readFile(PATH + "testModule.wyv");
        TestUtil.interpret(program);
	}

	@Test
	public void testImport() throws ParseException {
		String program = TestUtil.readFile(PATH + "import.wyv");
        TestUtil.doTestTypeFail(program);
	}

	@Test
	public void testRequire() throws ParseException {
		String program = TestUtil.readFile(PATH + "require.wyv");
        TestUtil.interpret(program);
	}

	@Test
	public void testRsType() throws ParseException {
		String program = TestUtil.readFile(PATH + "rsType.wyv");
        TestUtil.interpret(program);
	}

	@Test
	public void testWyt() throws ParseException {
		String program = TestUtil.readFile(PATH + "Log.wyt");
        TestUtil.interpret(program);
	}

	@Test
	public void testInst() throws ParseException {
		String program = TestUtil.readFile(PATH + "inst.wyv");
        TestUtil.interpret(program);
	}

	@Test
	public void testADT() throws ParseException {
		TestUtil.doTestScriptModularly("modules.ListClient",
            Util.intType(),
            new IntegerLiteral(5));
	}
	
	@Test
	@Category(CurrentlyBroken.class)
	public void testTransitiveAuthorityBad() throws ParseException {

		String[] fileList = {"Database.wyv", "DatabaseProxy.wyv", "DatabaseClientBad.wyv"};
		GenContext genCtx = GenContext.empty().extend("system", new Variable("system"), new NominalType("", "system"));
		genCtx = new TypeGenContext("Int", "system", genCtx);
		genCtx = new TypeGenContext("Unit", "system", genCtx);
		
		List<wyvern.target.corewyvernIL.decl.Declaration> decls = new LinkedList<wyvern.target.corewyvernIL.decl.Declaration>();
		
		for(String fileName : fileList) {
			String source = TestUtil.readFile(PATH + fileName);
			TypedAST ast = TestUtil.getNewAST(source, "test input");
			wyvern.target.corewyvernIL.decl.Declaration decl = ((Declaration) ast).topLevelGen(genCtx, null);
			decls.add(decl);
			genCtx = GenUtil.link(genCtx, decl);
		}

		// Should give some compilation error, but top-level vars are not implemented yet.
		// (21/12/2015)
	}
	
	@Test
	@Category(CurrentlyBroken.class)
	public void testTransitiveAuthorityGood() throws ParseException {

		String[] fileList = {"Database.wyv", "DatabaseProxy.wyv", "DatabaseClientGood.wyv"};
		GenContext genCtx = GenContext.empty().extend("system", new Variable("system"), new NominalType("", "system"));
		genCtx = new TypeGenContext("Int", "system", genCtx);
		genCtx = new TypeGenContext("Unit", "system", genCtx);
		
		List<wyvern.target.corewyvernIL.decl.Declaration> decls = new LinkedList<wyvern.target.corewyvernIL.decl.Declaration>();
		
		for(String fileName : fileList) {
			System.out.println(fileName);
			String source = TestUtil.readFile(PATH + fileName);
			TypedAST ast = TestUtil.getNewAST(source, "test input");
			wyvern.target.corewyvernIL.decl.Declaration decl = ((Declaration) ast).topLevelGen(genCtx, null);
			decls.add(decl);
			genCtx = GenUtil.link(genCtx, decl);
		}
		
		// Should compile OK, but top-level vars not implemented yet.
		// (21/12/2015)
	}
	
	@Test
	@Category(CurrentlyBroken.class)
	public void testTopLevelVars () throws ParseException {
		
		GenContext genCtx = GenContext.empty().extend("system", new Variable("system"), null);
		genCtx = new TypeGenContext("Int", "system", genCtx);
		genCtx = new TypeGenContext("Unit", "system", genCtx);
	
		// Load and link Database.wyv.
		TypedAST astDatabase = TestUtil.getNewAST(TestUtil.readFile(PATH + "Database.wyv"), "test input");
		wyvern.target.corewyvernIL.decl.Declaration decl = ((Declaration) astDatabase).topLevelGen(genCtx, null);
		genCtx = GenUtil.link(genCtx, decl);
		
		// Interpret DatabaseUser.wyv with Database.wyv in the context.
		String source = TestUtil.readFile(PATH + "DatabaseUser.wyv");
		ExpressionAST ast = (ExpressionAST) TestUtil.getNewAST(source, "test input");
		IExpr program = ast.generateIL(genCtx, Util.intType(), null);
		TypeContext ctx = TypeContext.empty();
		ValueType t = program.typeCheck(ctx);
		Assert.assertEquals(Util.intType(), t);
		wyvern.target.corewyvernIL.expression.Value result = program.interpret(EvalContext.empty());
		Assert.assertEquals(new IntegerLiteral(10), result);
		
	}
	
	@Test
	@Category(CurrentlyBroken.class)
	public void testTopLevelVarsWithAliasing () throws ParseException {
		
		GenContext genCtx = GenContext.empty().extend("system", new Variable("system"), null);
		genCtx = new TypeGenContext("Int", "system", genCtx);
		genCtx = new TypeGenContext("Unit", "system", genCtx);
	
		// Load and link Database.wyv.
		TypedAST astDatabase = TestUtil.getNewAST(TestUtil.readFile(PATH + "Database.wyv"), "test input");
		wyvern.target.corewyvernIL.decl.Declaration decl = ((Declaration) astDatabase).topLevelGen(genCtx, null);
		genCtx = GenUtil.link(genCtx, decl);
		
		// Interpret DatabaseUser.wyv with Database.wyv in the context.
		String source = TestUtil.readFile(PATH + "DatabaseUserTricky.wyv");
		ExpressionAST ast = (ExpressionAST) TestUtil.getNewAST(source, "test input");
		IExpr program = ast.generateIL(genCtx, Util.intType(), new LinkedList<TypedModuleSpec>());
		TypeContext ctx = TypeContext.empty();
		ValueType t = program.typeCheck(ctx);
		Assert.assertEquals(Util.intType(), t);
		wyvern.target.corewyvernIL.expression.Value result = program.interpret(EvalContext.empty());
		Assert.assertEquals(new IntegerLiteral(10), result);
		
	}
	
	@Test
	public void testTopLevelVarGet () throws ParseException {
		GenContext genCtx = Globals.getStandardGenContext();
		/*GenContext.empty().extend("system", new Variable("system"), null);
		genCtx = new TypeGenContext("Int", "system", genCtx);
		genCtx = new TypeGenContext("Unit", "system", genCtx);*/
	
		String source = "var v : Int = 5\n"
					  + "v\n";
		
		// Generate code to be evaluated.
		ExpressionAST ast = (ExpressionAST) TestUtil.getNewAST(source, "test input");
		IExpr program = ast.generateIL(genCtx, Util.intType(), null);
		
		// Evaluate.
		wyvern.target.corewyvernIL.expression.Value result = program.interpret(Globals.getStandardEvalContext());
		Assert.assertEquals(new IntegerLiteral(5), result);
		
	}
	
	@Test
	public void testTopLevelVarSet () throws ParseException {
		GenContext genCtx = Globals.getStandardGenContext();
	
		String source = "var v : Int = 5\n"
					  + "v = 10\n"
					  + "v\n";
		
		// Generate code to be evaluated.
		ExpressionAST ast = (ExpressionAST) TestUtil.getNewAST(source, "test input");
		IExpr program = ast.generateIL(genCtx, Util.intType(), null);
		
		// Evaluate.
		wyvern.target.corewyvernIL.expression.Value result = program.interpret(Globals.getStandardEvalContext());
		Assert.assertEquals(new IntegerLiteral(10), result);
		
	}
	
	@Test
	public void testSimpleADT() throws ParseException {
		TestUtil.doTestScriptModularly("modules.simpleADTdriver", Util.intType(), new IntegerLiteral(5));
	}
	
}
