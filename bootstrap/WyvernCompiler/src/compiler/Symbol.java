/**
 * 
 */
package compiler;

import java.util.*;

/**
 * @author Michael
 * 
 */
public interface Symbol {
	/**
	 * Represents the type of element
	 */
	public SymbolType type();

	/**
	 * The 1-based line number where the element begins in the source code
	 */
	public int line();

	/**
	 * The 1-based line number where the element ends in the source code
	 */
	public int endLine();

	/**
	 * The 1-based character position in a line where the element begins in the
	 * source code
	 */
	public int position();

	/**
	 * The 1-based character position in a line where the element ends in the
	 * source code
	 */
	public int endPosition();

	/**
	 * The (possibly modified) source code text for the element
	 */
	public String text();

	/**
	 * The symbols which make up this symbol if it is non-terminal.
	 */
	public List<Symbol> children();
}
