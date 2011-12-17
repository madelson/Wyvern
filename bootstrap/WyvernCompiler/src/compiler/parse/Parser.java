/**
 * 
 */
package compiler.parse;

import java.util.*;

import compiler.*;

/**
 * @author Michael
 * 
 */
public interface Parser {
	public static abstract class Result {
		public abstract List<String> errors();

		public abstract List<String> warnings();

		public abstract Symbol parseTree();

		public boolean succeeded() {
			return this.errors().isEmpty();
		}
	}

	/**
	 * Is this parser compiled instead of dynamically generated?
	 */
	boolean isCompiled();
	
	/**
	 * Attempts to parse the token stream
	 */
	public Parser.Result parse(Iterator<Symbol> tokens);
}
