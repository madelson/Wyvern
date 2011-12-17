/**
 * 
 */
package compiler.parse;

import java.util.*;

/**
 * @author Michael
 * 
 */
public interface ParserGenerator {
	public static abstract class Result {
		public abstract List<String> errors();

		public abstract List<String> warnings();

		public abstract Parser parser();

		public boolean succeeded() {
			return this.errors().isEmpty();
		}
	}
	
	public ParserGenerator.Result generate(Grammar grammar);
}
