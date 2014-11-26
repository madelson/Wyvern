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
public class LR0Generator extends LRGenerator {

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
		 * 	for any item (A -> _.XB) in I
		 * 		for any production X -> y
		 * 			I = I U {(X -> .y)}
		 * until I does not change
		 * return I
		 * 
		 * However, notice that looping over any item twice will always yield the same set
		 * of productions. Thus, rather than repeatedly looping over the entirety of I we instead
		 * just iterate from i = 0 -> I.size(), where I.size() grows as we add more items. Whenever
		 * I stops growing, i will catch up and the algorithm will terminate.
		 */
		
		// for any item A -> _.XB in items
		List<Item> itemList = new ArrayList<Item>(items);
		for (int i = 0; i < itemList.size(); ++i) {
			Item item = itemList.get(i);
			if (item.hasNextSymbolType() && !item.nextSymbolType().isTerminal()) {
				// TODO PERF: it seems like we are doing extra work here, because we're 
				// re-looping over all productions for a particular symbol type unecessarily. We could
				// potentially remember the symbol types we've already looped over and use that to avoid
				// the scan
				
				// for any production X -> .something
				for (Production production : grammar.productions(item.nextSymbolType())) {
					// items <- items U { X -> .something }
					Item newItem = new Item(production, null, 0);
					if (items.add(newItem)) {
						// if we found a new item, queue it for iteration
						itemList.add(newItem);
					}
 				}
			}
		}

		return items;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see compiler.parse.LRGenerator#transition(compiler.parse.Grammar,
	 * compiler.parse.LRGenerator.State, compiler.SymbolType)
	 */
	@Override
	protected Set<Item> transition(Grammar grammar, State state,
			SymbolType symbolType) {
		// given state I, symbolType X
		
		// J <- {}
		Set<Item> items = new LinkedHashSet<Item>();
		// for any item (A -> _.XB, z) in I
		for (Item item : state.transitionItems(symbolType)) {
			// add (A -> _X.B, z) to J
			items.add(item.advance());
		}

		// return CLOSURE(J)
		return this.closure(grammar, items);
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

		for (Item item : state.items())
			if (!item.hasNextSymbolType())
				reductions.add(new Reduction(state, null, item.production()));

		return Collections.unmodifiableSet(reductions);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see compiler.parse.LRGenerator#mergeStates(java.util.Set)
	 */
	@Override
	protected Map<State, State> mergeStates(Set<State> states) {
		return Collections.emptyMap();
	}

}
