package wyvern.tools.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import wyvern.target.corewyvernIL.support.Util;
import wyvern.tools.imports.extensions.WyvernResolver;
import wyvern.tools.parsing.coreparser.ParseException;
import wyvern.tools.tests.suites.CurrentlyBroken;
import wyvern.tools.tests.suites.RegressionTests;

//import wyvern.target.corewyvernIL.support.Util;
//import wyvern.tools.parsing.coreparser.ParseException;
//import wyvern.tools.tests.suites.RegressionTests;

/**
 * Test suite for the effect system (adapted from ExampleTests.java).
 * Test cases numbered with "x0" (ex. 10) do not have effect annotations; 
 * those numbered with "xn" (ex. 12) are versions of "x0" test cases 
 * annotated with effects. 
 * 
 * Successful test cases have the following printout format:
 * "data sent: Network%d%d with(out) effects
 * data received"
 * 
 * Test cases that should be broken are in the category of 
 * @Category(CurrentlyBroken.class); test cases that should
 * be broken but pass for now due to the unimplemented checking
 * of effects in methods, and are commented as "work-in-progress".
 * 
 * Comments related to effects: "declaration, definition, method annotation"
 * Appearance in Wyvern:
 * effect "declared_effect" = {"its_defined_effects"}
 * def method_name() : {"method_annotation_of_effects"} return_type 
 * 
 * @author vzhao
 */
@Category(RegressionTests.class)
public class EffectSystemTests {
    private static final String PATH = TestUtil.BASE_PATH;
    
    @BeforeClass public static void setupResolver() {
        TestUtil.setPaths();
        WyvernResolver.getInstance().addPath(PATH);
    }
    
    @Test
    public void testEffectNetwork00() throws ParseException {
    	/* Type & module def with no annotations. */
    	TestUtil.doTestScriptModularly(PATH, "effects.testNetwork00", Util.unitType(), Util.unitValue());
    }

    @Test
     public void testEffectNetwork01() throws ParseException {
    	/* Declared in type + module def;
    	 * Defined in module def;
    	 * Method annotations in both. */ 
    	TestUtil.doTestScriptModularly(PATH, "effects.testNetwork01", Util.unitType(), Util.unitValue());
	}
    
    @Test
    @Category(CurrentlyBroken.class) 
    // **Work-in-progress: passes despite "undefined" effect used for method**
    public void testEffectNetwork02() throws ParseException {
    	/* No declarations. Undefined method annotations in module def
    	 * that does not correspond to the method annotations in type. */
    	TestUtil.doTestScriptModularly(PATH, "effects.testNetwork02", Util.unitType(), Util.unitValue());
	}
  
    @Test
    public void testEffectNetwork03() throws ParseException {
    	/* In addition to declarations (not defined) & method annotations in type, additional declaration &
    	 * definition in module def. */
    	TestUtil.doTestScriptModularly(PATH, "effects.testNetwork03", Util.unitType(), Util.unitValue());
	}
    
    @Test
    @Category(CurrentlyBroken.class) // Parse error
    public void testEffectNetwork04() throws ParseException {
    	/* "gibberish" where "{}" should be in type's method header annotation. */
    	TestUtil.doTestScriptModularly(PATH, "effects.testNetwork04", Util.unitType(), Util.unitValue());
	}
    
    @Test
    @Category(CurrentlyBroken.class) // Parse error
    public void testEffectNetwork05() throws ParseException {
    	/* "gibberish" where "{}" should be in module def's method header annotation. */
    	TestUtil.doTestScriptModularly(PATH, "effects.testNetwork05", Util.unitType(), Util.unitValue());
	}
    
    @Test
    @Category(CurrentlyBroken.class) // Parse error
    public void testEffectNetwork06() throws ParseException {
    	/* Bogus declaration ("effect send = stdout") in module def. */ 
    	TestUtil.doTestScriptModularly(PATH, "effects.testNetwork06", Util.unitType(), Util.unitValue());
	}
    
    @Test
    public void testEffectNetwork07() throws ParseException {
    	/* Declarations + 1 defined in type;
    	 * Declarations + definitions in module def;
    	 * Method annotations in both.
    	 */
    	TestUtil.doTestScriptModularly(PATH, "effects.testNetwork07", Util.unitType(), Util.unitValue());
	}
    
    @Test
    @Category(CurrentlyBroken.class) // Invalid effect (actually DSL block instead)
    public void testEffectNetwork08() throws ParseException {
    	/* Like network07, but "effect receive = {{}}" */
    	TestUtil.doTestScriptModularly(PATH, "effects.testNetwork08", Util.unitType(), Util.unitValue());
	}
    
    @Test
    public void testEffectNetwork09() throws ParseException {
    	/* No method annotations despite declarations in type + module def. */
    	TestUtil.doTestScriptModularly(PATH, "effects.testNetwork09", Util.unitType(), Util.unitValue());
    }
    
    @Test
    @Category(CurrentlyBroken.class) // Parse error (undefined effects in module def are taken care of by the parser)
    public void testEffectNetwork0A() throws ParseException {
    	/* Effect undefined in module def. */
    	TestUtil.doTestScriptModularly(PATH, "effects.testNetwork0A", Util.unitType(), Util.unitValue());
    }
    
    @Test
    @Category(CurrentlyBroken.class) // **Work-in-progress: passes but shouldn't**
    public void testEffectNetwork0B() throws ParseException { // dependent on incorporating EffectDeclaration into DefDeclaration
    	/* Nonexistent effect in method annotation in type (not in module def, but error should be reported before module def is evaluated). */
    	TestUtil.doTestScriptModularly(PATH, "effects.testNetwork0B", Util.unitType(), Util.unitValue());
    }
    
    @Test
    @Category(CurrentlyBroken.class) // **Work-in-progress: passes but shouldn't**
    public void testEffectNetwork0C() throws ParseException { // dependent on incorporating EffectDeclaration into DefDeclaration
    	/* Int included as effect in module annotation of type (not in module def, but error should be reported before module def is evaluated). */
    	TestUtil.doTestScriptModularly(PATH, "effects.testNetwork0C", Util.unitType(), Util.unitValue());
    }
    
    @Test
    @Category(CurrentlyBroken.class) 
    public void testEffectNetwork0D() throws ParseException {
    	/* Bogus declaration ("effect send = {something.hi}") in module def (something being not an object defined)
    	 * for an effect that was left undefined in the type and not actually used 
    	 * in a method (so that we can be sure that the checking is happening upon declaration). */ 
    	TestUtil.doTestScriptModularly(PATH, "effects.testNetwork0D", Util.unitType(), Util.unitValue());
	}
    
    @Test
    @Category(CurrentlyBroken.class) 
    public void testEffectNetwork0E() throws ParseException {
    	/* Bad declaration ("effect send = {stdout.hi}") in module def (stdout being an actual object that does
    	 * not have the effect "hi" defined) for an effect that was left undefined in the type and not actually used 
    	 * in a method (so that we can be sure that the checking is happening upon declaration). */ 
    	TestUtil.doTestScriptModularly(PATH, "effects.testNetwork0E", Util.unitType(), Util.unitValue());
	}
    
//    @Test
//    public void testEffectNetwork11() throws ParseException {
//    	/* Same as network01 but without type ascription (for testing DataProcessor). */
//    	TestUtil.doTestScriptModularly(PATH, "effects.testNetwork11", Util.unitType(), Util.unitValue());
//    }
    
    @Test
    public void testDataProcessor() throws ParseException {
    	/* Involve real effect abstraction ("effect process = {net.receive}"). */
    	TestUtil.doTestScriptModularly(PATH, "effects.testDataProcessor", Util.unitType(), Util.unitValue());
    }
    
    @Test
    public void testDataProcessor2() throws ParseException {
    	/* Involve even more effect abstractions ("effect send = {net.send}, effect process = {net.receive, send}"). */
    	TestUtil.doTestScriptModularly(PATH, "effects.testDataProcessor2", Util.unitType(), Util.unitValue());
    }
    
    @Test
    @Category(CurrentlyBroken.class) 
    public void testDataProcessor3() throws ParseException {
    	/* Shorter version of dataProcessor2, with "effect process = {net.receive, gibberish}". */
    	TestUtil.doTestScriptModularly(PATH, "effects.testDataProcessor3", Util.unitType(), Util.unitValue());
    }
    
    @Test
    @Category(CurrentlyBroken.class) 
    public void testDataProcessor4() throws ParseException {
    	/* Shorter version of dataProcessor2, with "effect process = {net.gibberish, process}" (i.e. recursive). */
    	TestUtil.doTestScriptModularly(PATH, "effects.testDataProcessor4", Util.unitType(), Util.unitValue());
    }
    
    @Test
    public void testDataProcessor5() throws ParseException {
    	/* Like dataProcessor2, but has "effect receive = {}" in addition to the use of net.receive (i.e. same name, dif paths). */
    	TestUtil.doTestScriptModularly(PATH, "effects.testDataProcessor5", Util.unitType(), Util.unitValue());
    }
    
    // another test in which a third module takes in a data processor which takes in a network, so that the there's multiple (external) layers of effect abstraction?
    
    @Test
    public void testEffectObjNetwork00() throws ParseException {
    	/* Object notation with no effect annotations. */
    	TestUtil.doTestScriptModularly(PATH, "effects.objNetwork00", Util.unitType(), Util.unitValue());
	}

    @Test
    public void testEffectObjNetwork01() throws ParseException {
    	/* Except for the "new" notation, should otherwise use the same a parser code as modules. */
    	TestUtil.doTestScriptModularly(PATH, "effects.objNetwork01", Util.unitType(), Util.unitValue());
	}
}