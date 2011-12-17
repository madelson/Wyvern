/**
 * 
 */
package compiler.parse;

import java.util.*;

import compiler.SymbolType;

/**
 * @author Michael
 * 
 */
public class LR1Generator extends LR0Generator {

	/*
	 * (non-Javadoc)
	 * 
	 * @see compiler.parse.LRGenerator#closure(compiler.parse.Grammar,
	 * java.util.Set)
	 */
	@Override
	protected Set<Item> closure(Grammar grammar, Set<Item> items) {
		boolean changed;
		// repeat
		do {
			changed = false;

			// for any item A -> _.X?, z in items
			for (Item item : new ArrayList<Item>(items))
				if (item.hasNextSymbolType()
						&& !item.nextSymbolType().isTerminal()) {
					// for any production X -> .something
					for (Production production : grammar
							.productions(item.nextSymbolType())) {
						// for any w in FIRST(?z)
						for (SymbolType tokenType : grammar.nff().first(
								item.remaining()))
							// items <- items U { X -> .something, w }
							changed |= items.add(new Item(production,
									tokenType, 0));
					}
				}
		} while (changed);

		return items;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see compiler.parse.LRGenerator#reductions(compiler.parse.Grammar,
	 * compiler.parse.LRGenerator.State)
	 */
	@Override
	protected Set<Reduction> reductions(Grammar grammar, State state) {
		Set<Reduction> reductions = new LinkedHashSet<Reduction>();
		
		// for each item A -> _x. , z in state
		for (Item item : state.items())
			if (!item.hasNextSymbolType())
				// reductions <- reductions U { (state, z, A -> x) }
				reductions.add(new Reduction(state, item.lookahead(), item
						.production()));

		return Collections.unmodifiableSet(reductions);
	}
}
