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
		boolean changed;
		do {
			changed = false;

			for (Item item : new ArrayList<Item>(items))
				if (item.hasNextSymbolType()
						&& !item.nextSymbolType().isTerminal()) {
					for (Production production : grammar.productions(item
							.nextSymbolType()))
						changed |= items.add(new Item(production, null, 0));
				}
		} while (changed);

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
