/**
 * 
 */
package compiler.parse;

import compiler.*;

import java.util.*;

/**
 * @author Michael
 * 
 */
public class LALRGenerator extends LR1Generator {

	/**
	 * Merges states which differ only by lookahead sets
	 */
	@Override
	protected Map<State, State> mergeStates(Set<State> states) {
		Map<Set<Item>, List<State>> groupedStates = new LinkedHashMap<Set<Item>, List<State>>();

		// map each item by its item set with lookahead removed
		for (State state : states) {
			Set<Item> itemsWithoutLookahead = new LinkedHashSet<Item>();
			for (Item itemWithLookahead : state.items())
				itemsWithoutLookahead.add(new Item(itemWithLookahead
						.production(), null, itemWithLookahead.position()));
			Utils.put(groupedStates, ArrayList.class,
					Collections.unmodifiableSet(itemsWithoutLookahead), state);
		}

		// for each mapped group, create a merged state
		Map<State, State> mergeMap = new LinkedHashMap<State, State>();
		for (List<State> group : groupedStates.values()) {
			Set<Item> mergedItems = new LinkedHashSet<Item>();
			StringBuilder nameBuilder = new StringBuilder();
			for (State state : group) {
				mergedItems.addAll(state.items());
				nameBuilder.append(state.name()).append('-');
				states.remove(state);
			}

			State mergedState = new State(nameBuilder.toString().substring(0,
					nameBuilder.length() - 1), mergedItems);
			for (State state : group)
				mergeMap.put(state, mergedState);
			states.add(mergedState);
		}
		
		return mergeMap;
	}
}
