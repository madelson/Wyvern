/**
 * 
 */
package compiler.parse;

/**
 * 
 * @author Michael
 */
public enum Associativity {
	/**
	 * A symbol * is LEFT associative if E * E * E is interpreted as (E * E) * E
	 */
	Left,

	/**
	 * A symbol * is RIGHT associative if E * E * E is interpreted as E * (E *
	 * E)
	 */
	Right,

	/**
	 * A symbol * is NON-associative if E * E * E is interpreted as an error
	 */
	NonAssociative
}
