/**
 * 
 */
package compiler.parse;

import java.util.*;

import compiler.SymbolType;
import compiler.Tuples;
import compiler.Utils;

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
		/**
		 * <pre>
		 * The official algorithm is: Closure(I) = 
		 * 	repeat for any item (A -> _.XB, z) in I 
		 * 		for any production X -> y 
		 * 			for any w in FIRST(Bz) 
		 * 				I = I U {(X -> .y, w)} 
		 * 	until I does not change return I
		 * </pre>
		 * 
		 * However, notice that looping over any item twice will always yield
		 * the same set of productions. Thus, rather than repeatedly looping
		 * over the entirety of I we instead just iterate from i = 0 ->
		 * I.size(), where I.size() grows as we add more items. Whenever I stops
		 * growing, i will catch up and the algorithm will terminate.
		 */

		/*
		 * Items T -> A.XB, z that differ only by lookahead and which have
		 * non-nullable B's don't need to be processed twice for different
		 * lookahead symbols. Thus, we cache them hear for performance
		 */
		Set<Tuples.Duo<Production, Integer>> irrelevantLookaheadItems = new HashSet<Tuples.Duo<Production, Integer>>();

		/*
		 * There's no point in trying all productions for any symbol X with the
		 * same lookahead symbol w in FIRST(Bz) more than once. Thus, we cache
		 * such attempts here for performance.
		 */
		Map<SymbolType, Set<SymbolType>> symbolFirstSetCache = new HashMap<SymbolType, Set<SymbolType>>();

		// for any item A -> _.XB, z in items
		List<Item> itemList = new ArrayList<Item>(items);
		for (int i = 0; i < itemList.size(); ++i) {
			Item item = itemList.get(i);
			if (item.hasNextSymbolType() && !item.nextSymbolType().isTerminal()) {

				// performance caching of irrelevant lookahead items
				Tuples.Duo<Production, Integer> irrelevantLookaheadItemKey = new Tuples.Duo<Production, Integer>(
						item.production(), item.position());
				if (irrelevantLookaheadItems
						.contains(irrelevantLookaheadItemKey)) {
					// don't visit the item again
					continue;
				}
				// otherwise, determine whether the item is an irrelevant
				// lookahead item
				for (int j = item.production().childTypes().size() - 1; j > item
						.position(); --j) {
					SymbolType remainingType = item.production().childTypes()
							.get(j);
					if (remainingType.isTerminal()
							|| !grammar.nff().nullableSet()
									.contains(remainingType)) {
						irrelevantLookaheadItems
								.add(irrelevantLookaheadItemKey);
						break;
					}
				}

				Set<SymbolType> firstOfRemaining = grammar.nff().first(
						item.remaining());
				Set<SymbolType> alreadyTriedFirstSymbols = symbolFirstSetCache
						.get(item.nextSymbolType());
				if (alreadyTriedFirstSymbols == null) {
					// put a copy here because we'll need to modify it and the
					// return value of first() may not be modifiable
					symbolFirstSetCache.put(item.nextSymbolType(),
							new HashSet<SymbolType>(firstOfRemaining));
				}
				Set<Production> productions = null;

				// for any w in FIRST(Bz)
				for (SymbolType tokenType : firstOfRemaining) {

					// first symbol/lookahead caching
					// note that to make this efficient we've reversed the
					// nesting of the loops so that we iterate over FIRST(Bz)
					// and then productions. That way, when we end up being able
					// to skip a (symbol, firstSymbol) combination we can
					// skip looping over all associated productions
					if (alreadyTriedFirstSymbols != null
							&& !alreadyTriedFirstSymbols.add(tokenType)) {
						// avoid trying again if when we add the token to the
						// cache it's already there
						continue;
					}
					if (productions == null) {
						productions = grammar.productions(tokenType);
					}

					// for any production X -> .something
					for (Production production : grammar.productions(item
							.nextSymbolType())) {
						// items <- items U { X -> .something, w }
						Item newItem = new Item(production, tokenType, 0);
						if (items.add(newItem)) {
							// if we found a new item, queue it for iteration
							itemList.add(newItem);
						} else {
							Utils.err("With first symbol lookahead caching, we should never fail to add a symbol!");
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
