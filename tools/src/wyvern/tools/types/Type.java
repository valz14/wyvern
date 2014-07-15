package wyvern.tools.types;

import wyvern.tools.typedAST.transformers.Types.TypeTransformer;
import wyvern.tools.util.TreeWritable;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

public interface Type extends TreeWritable {
	/**
	 * This function should return Some(env) if and only if a derivation for
	 *     input |- this <: other -| env
	 * exists.
	 *
	 * @param other The type to check against
	 * @param input The input environment
	 * @param subtypes Prechecked subtypes.
	 * @return Some(env) if this is a subtype of other with output environment env, None otherwise.
	 */
	public Optional<Environment> subtype(Type other, Environment input, HashSet<SubtypeRelation> subtypes);

	/**
	 * This function should return Some(env) if and only if a derivation for
	 *     input |- this <: other -| env
	 * exists.
	 *
	 * @param other The type to check against
	 * @param input The input environment
	 * @return Some(env) if this is a subtype of other with output environment env, None otherwise.
	 */
	public Optional<Environment> subtype(Type other, Environment input);

	/**
	 * @return whether this type is simple or compound.  Used in toString().
	 */
	public boolean isSimple();

	/**
	 * Gets the children of a composite node
	 * @return The children of the node
	 */
	Map<String, Type> getChildren();
	/**
	 * Clones the current AST node with the given set of children
	 * @param newChildren The children to create
	 * @param transformer
	 * @return The deep-copied Type node
	 */
	Type cloneWithChildren(Map<String, Type> newChildren, TypeTransformer transformer);
}