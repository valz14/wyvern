package wyvern.tools.typedAST.core.expressions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import wyvern.target.corewyvernIL.expression.Expression;
import wyvern.target.corewyvernIL.modules.TypedModuleSpec;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;
import wyvern.tools.typedAST.abs.CachingTypedAST;
import wyvern.tools.typedAST.core.binding.NameBinding;
import wyvern.tools.typedAST.core.binding.StaticTypeBinding;
import wyvern.tools.typedAST.core.binding.evaluation.HackForArtifactTaggedInfoBinding;
import wyvern.tools.typedAST.core.values.Obj;
import wyvern.tools.typedAST.interfaces.CoreAST;
import wyvern.tools.typedAST.interfaces.ExpressionAST;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.typedAST.transformers.GenerationEnvironment;
import wyvern.tools.typedAST.transformers.ILWriter;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.UnresolvedType;
import wyvern.tools.types.extensions.ClassType;
import wyvern.tools.types.extensions.TypeInv;
import wyvern.tools.util.EvaluationEnvironment;
import wyvern.tools.util.TreeWriter;

/**
 * Represents a match statement in Wyvern.
 *
 * @author Troy Shaw
 */
public class Match extends CachingTypedAST implements CoreAST {

	private TypedAST matchingOver;

	private List<Case> cases;
	private Case defaultCase;

	/** Original list which preserves the order and contents. Needed for checking. */
	private List<Case> originalCaseList;

	private FileLocation location;

	public String toString() {
		return "Match: " + matchingOver + " with " + cases + " cases and default: " + defaultCase;
	}

	public Match(TypedAST matchingOver, List<Case> cases, FileLocation location) {
		//clone original list so we have a canonical copy
		this.originalCaseList = new ArrayList<Case>(cases);

		this.matchingOver = matchingOver;
		this.cases = cases;

		//find the default case and remove it from the typed cases
		for (Case c : cases) {
			if (c.isDefault()) {
				defaultCase = c;
				break;
			}
		}

		cases.remove(defaultCase);

		this.location = location;
	}

	/**
	 * Internal constructor to save from finding the default case again.
	 *
	 * @param matchingOver
	 * @param cases
	 * @param defaultCase
	 * @param location
	 */
	private Match(TypedAST matchingOver, List<Case> cases, Case defaultCase, FileLocation location) {
		this.matchingOver = matchingOver;
		this.cases = cases;
		this.defaultCase = defaultCase;
		this.location = location;
	}

	/**
	 * Checks if matchingOver is a subtag of matchTarget.
	 *
	 * Searches recursively to see if what we are matching over is a sub-tag of the given target.
	 *
	 * @return
	 */
	//TODO: rename this method to something like isSubtag()
	private boolean isSubtag(TaggedInfo matchingOver, TaggedInfo matchTarget) {
		if (matchingOver == null) throw new NullPointerException("Matching Binding cannot be null");
		if (matchTarget == null) throw new NullPointerException("match target cannot be null");

		// String matchingOverTag = matchingOver.getTagName();
		// String matchTargetTag = matchTarget.getTagName();

		// System.out.println("matchingOverTag = " + matchingOverTag + " and matchTargetTag = " + matchTargetTag);

		// FIXME: Why do equals when that may not correspond to the tags being the same? Only reference == is safe I guess?
		// if (matchingOverTag.equals(matchTargetTag)) return true;
		if (matchingOver == matchTarget) return true;

		// If caseOf is hopelessly broken, this is a "fix": return false; :-)d

		TaggedInfo ti = matchingOver.getCaseOfTaggedInfo();

		if (ti == null) return false;
		return isSubtag(ti, matchTarget); // FIXME:
	}

	@Override
	public Map<String, TypedAST> getChildren() {
		Map<String, TypedAST> children = new HashMap<>();

		for (Case c : cases) {
			//is there a proper convention for names in children?
			children.put("match case: " + c.getTaggedTypeMatch(), c.getAST());
		}

		if (defaultCase != null) {
			children.put("match default-case: " + defaultCase.getTaggedTypeMatch(), defaultCase.getAST());
		}


		return children;
	}

	@Override
	public ExpressionAST cloneWithChildren(Map<String, TypedAST> newChildren) {
		return new Match(matchingOver, cases, defaultCase, location);
	}

	@Override
	public FileLocation getLocation() {
		return location;
	}

	public void resolve(Environment env) {
		if (this.defaultCase != null)
			this.defaultCase.resolve(env, this);

		for (Case c : this.cases) {
			c.resolve(env, this);
		}
	}

	/**
	 * Checks there is not more than one default.
	 */
	private void checkNotMultipleDefaults() {
		for (int numDefaults = 0, i = 0; i < originalCaseList.size(); i++) {
			Case c = originalCaseList.get(i);

			if (c.isDefault()) {
				numDefaults++;

				if (numDefaults > 1) {
					ToolError.reportError(ErrorMessage.MULTIPLE_DEFAULTS, matchingOver);
				}
			}
		}
	}

	/**
	 * Checks that if a default is present it is last.
	 */
	private void checkDefaultLast() {
		//check default is last (do this after counting so user gets more specific error message)
		for (int i = 0; i < originalCaseList.size(); i++) {
			if (originalCaseList.get(i).isDefault() && i != originalCaseList.size() - 1) {
				ToolError.reportError(ErrorMessage.DEFAULT_NOT_LAST, matchingOver);
			}
		}
	}

	private void checkAllCasesAreTagged(Environment env) {
		//All things we match over must be tagged types
		for (Case c : cases) {
			if (c.isDefault()) continue;

			Type tagName = c.getTaggedTypeMatch();

			if (tagName instanceof UnresolvedType) {
				UnresolvedType ut = (UnresolvedType) tagName;
				// System.out.println("ut = " + ut.resolve(env));
			}

			if (tagName instanceof TypeInv) {
				// TypeInv ti = (TypeInv) tagName;
				// System.out.println("ti = " + ti.resolve(env));
				// tagName = ti.resolve(env);
				// if (tagName instanceof UnresolvedType) {
					// tagName = ((UnresolvedType) tagName).resolve(env);
				// } DO NOT UNCOMMENT THIS AS BREAKS CASES
				return; // FIXME: Assume TypeInv will sort itself out during runtime.
			}

			// System.out.println(tagName);

			//check type exists
			// TypeBinding type = env.lookupValue(tagName.toString()); // FIXME:

			// if (type == null) {
			//	ToolError.reportError(ErrorMessage.TYPE_NOT_DECLARED, this, tagName.toString());
			// }

			//check it is tagged
			TaggedInfo info = TaggedInfo.lookupTagByType(tagName); // FIXME:

			if (info == null) {
				ToolError.reportError(ErrorMessage.TYPE_NOT_TAGGED, matchingOver, tagName.toString());
			}
		}

	}

	private void checkAllCasesAreUnique() {
		// All tagged types must be unique
		Set<Type> caseSet = new HashSet<Type>();

		for (Case c : cases) {
			if (c.isTyped()) caseSet.add(c.getTaggedTypeMatch());
		}

		if (caseSet.size() != cases.size()) {
			ToolError.reportError(ErrorMessage.DUPLICATE_TAG, matchingOver);
		}
	}

	private void checkSubtagsPreceedSupertags() {
		//A tag cannot be earlier than one of its subtags
		for (int i = 0; i < cases.size() - 1; i++) {
			Case beforeCase = cases.get(i);
			TaggedInfo beforeTag = TaggedInfo.lookupTagByType(beforeCase.getTaggedTypeMatch()); // FIXME:

			for (int j = i + 1; j < cases.size(); j++) {
				Case afterCase = cases.get(j);

				if (afterCase.isDefault()) break;

				TaggedInfo afterTag = TaggedInfo.lookupTagByType(afterCase.getTaggedTypeMatch()); // FIXME:
				//TagBinding afterBinding = TagBinding.get(afterCase.getTaggedTypeMatch());

				if (afterTag != null && beforeTag != null && isSubtag(afterTag, beforeTag)) {
					ToolError.reportError(ErrorMessage.SUPERTAG_PRECEEDS_SUBTAG, matchingOver, beforeTag.getTagName(), afterTag.getTagName());
				}
			}
		}
	}

	private void checkBoundedAndUnbounded(TaggedInfo matchTaggedInfo) {

		// If we're an unbounded type, check default exists
		if (!matchTaggedInfo.hasComprises()) {
			if (defaultCase == null) {
				ToolError.reportError(ErrorMessage.UNBOUNDED_WITHOUT_DEFAULT, matchingOver);
			}
		} else {
			//we're bounded. Check if comprises is satisfied
			boolean comprisesSatisfied = comprisesSatisfied(matchTaggedInfo);

			//if comprises is satisfied, default must be excluded
			if (comprisesSatisfied && defaultCase != null) {
				ToolError.reportError(ErrorMessage.BOUNDED_EXHAUSTIVE_WITH_DEFAULT, matchingOver);
			}

			//if comprises is not satisfied, default must be present
			if (!comprisesSatisfied && defaultCase == null) {
				ToolError.reportError(ErrorMessage.BOUNDED_INEXHAUSTIVE_WITHOUT_DEFAULT, matchingOver);
			}
		}
	}

	/**
	 * Checks that the tag we are matching over is a supertag of
	 * every tag in the case-list.
	 *
	 * This ensures that each tag could actually have a match and that case is
	 * not unreachable code.
	 *
	 * @param matchingOverTag
	 */
	private void checkStaticSubtags(TaggedInfo matchingOver) {
		for (Case c : cases) {
			TaggedInfo matchTarget = TaggedInfo.lookupTagByType(c.getTaggedTypeMatch()); // FIXME:

			if (!isSubtag(matchTarget, matchingOver)) {
				ToolError.reportError(ErrorMessage.UNMATCHABLE_CASE, this.matchingOver, matchingOver.getTagName(), matchTarget.getTagName());
			}
		}
	}

	private StaticTypeBinding getStaticTypeBinding(TypedAST varAST, Environment env) {
		if (varAST instanceof Variable) {
			Variable var = (Variable) varAST;

			StaticTypeBinding binding = env.lookupStaticType(var.getName());

			return binding;
		}

		return null;
	}

	private boolean comprisesSatisfied(TaggedInfo matchBinding) {
		List<Type> comprisesTags = matchBinding.getComprisesTags();

		//add this tag because it needs to be included too
		comprisesTags.add(matchBinding.getTagType());

		//check that each tag is present
		for (Type t : comprisesTags) {
			if (containsTagBinding(cases, t)) continue;

			//tag wasn't present
			return false;
		}

		//we made it through them all
		return true;
	}

	/**
	 * Helper method to simplify checking for a tag.
	 * Returns true if the given binding tag is present in the list of cases.
	 *
	 * @param cases
	 * @param binding
	 * @return
	 */
	private boolean containsTagBinding(List<Case> cases, Type tagName) {
		for (Case c : cases) {
			//Found a match, this tag is present
			if (c.getTaggedTypeMatch().equals(tagName)) return true;
		}

		return false;
	}

	@Override
	protected ExpressionAST doClone(Map<String, TypedAST> nc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Expression generateIL(GenContext ctx, ValueType expectedType, List<TypedModuleSpec> dependencies) {
		// TODO Auto-generated method stub
		return null;
	}
}
