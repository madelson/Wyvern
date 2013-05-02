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
		/*
		 * The official algorithm is:
		 * Closure(I) = 
		 * repeat
		 * 	for any item (A -> _.XB, z) in I
		 * 		for any production X -> y
		 * 			for any w in FIRST(Bz)
		 * 				I = I U {(X -> .y, w)}
		 * until I does not change
		 * return I
		 * 
		 * However, notice that looping over any item twice will always yield the same set
		 * of productions. Thus, rather than repeatedly looping over the entirety of I we instead
		 * just iterate from i = 0 -> I.size(), where I.size() grows as we add more items. Whenever
		 * I stops growing, i will catch up and the algorithm will terminate.
		 */
		
		// for any item A -> _.XB, z in items
		List<Item> itemList = new ArrayList<Item>(items);
		for (int i = 0; i < itemList.size(); ++i) {
			Item item = itemList.get(i);
			if (item.hasNextSymbolType() && !item.nextSymbolType().isTerminal()) {
				// for any production X -> .something
				for (Production production : grammar.productions(item.nextSymbolType())) {
					// for any w in FIRST(Bz)
					for (SymbolType tokenType : grammar.nff().first(item.remaining())) {
						// items <- items U { X -> .something, w }
						Item newItem = new Item(production, tokenType, 0);
						if (items.add(newItem)) {
							// if we found a new item, queue it for iteration
							itemList.add(newItem);
						}
 					}
 				}
			}
		}

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
