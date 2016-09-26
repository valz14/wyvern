package wyvern.tools.tests;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import edu.umn.cs.melt.copper.runtime.logging.CopperParserException;
import wyvern.tools.imports.extensions.WyvernResolver;
import wyvern.tools.parsing.coreparser.ParseException;
import wyvern.tools.tests.suites.CurrentlyBroken;
import wyvern.tools.tests.suites.RegressionTests;
import wyvern.tools.tests.TestUtil;
import wyvern.tools.typedAST.interfaces.TypedAST;

@Category(RegressionTests.class)
public class RossettaCodeTests {
    @BeforeClass public static void setupResolver() {
    	TestUtil.setPaths();
		WyvernResolver.getInstance().addPath(PATH);
    }

	@Test
	public void testHello() throws ParseException {
		String program = TestUtil.readFile(PATH + "hello.wyv");
		TestUtil.interpret(program);
	}
	
	private static final String BASE_PATH = TestUtil.BASE_PATH;
	private static final String PATH = BASE_PATH + "rosetta2/";
	private static final String OLD_PATH = BASE_PATH + "rosetta-old/";
	
	@Test
	public void testNewHello() throws ParseException {		
		TestUtil.doTestScriptModularly("rosetta.hello", null, null);
	}
	
	@Test
	public void testExplicitHello() throws ParseException {		
		TestUtil.doTestScriptModularly("rosetta.hello-explicit", null, null);
	}
	
	@Test
	public void testFib() throws ParseException {		
		TestUtil.doTestScriptModularly("rosetta.fibonacci", null, null);
	}
	
	@Test
	public void testFactorial() throws ParseException {		
		TestUtil.doTestScriptModularly("rosetta.factorial", null, null);
	}
}
