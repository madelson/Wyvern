/**
 * 
 */
package compiler.lex;

import java.util.*;

import compiler.*;

/**
 * @author Michael
 * 
 */
public interface LexerGenerator {
	public static abstract class Result {
		public abstract List<String> errors();

		public abstract List<String> warnings();

		public abstract Lexer lexer();

		public boolean succeeded() {
			return this.errors().isEmpty();
		}
	}

	/**
	 * Create a lexer from the String->SymbolType mapping
	 */
	LexerGenerator.Result generate(Context context, LinkedHashSet<LexerAction> actions);

	public abstract class AbstractLexerGenerator implements LexerGenerator {
		@Override
		public final LexerGenerator.Result generate(Context context,
				LinkedHashSet<LexerAction> actions) {
			Map<String, LinkedHashMap<String, LexerAction>> groupedActions = new HashMap<String, LinkedHashMap<String, LexerAction>>();
			for (LexerAction lexerAction : actions) {
				Utils.check(lexerAction.symbolType() == null
						|| lexerAction.symbolType().context().equals(context),
						"Bad context!");
				for (String validState : lexerAction.validStates()) {
					if (Utils.put(groupedActions, LinkedHashMap.class, validState, lexerAction.pattern(), lexerAction) != null)	
						Utils.err("Cannot have two lexer actions for a given (state, pattern) pair!");
				}
			}

			return this.generateImpl(context, new LinkedHashSet<LexerAction>(actions), groupedActions);
		}

		protected abstract LexerGenerator.Result generateImpl(Context context,
				LinkedHashSet<LexerAction> allActions,
				Map<String, LinkedHashMap<String, LexerAction>> groupedActions);
	}
}
