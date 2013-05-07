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

	// MA: this is a partial implementation of a potential major perf improvement for LALR.
	// Basically, right now we don't take advantage of LALR's merging until the very end when we could
	// try taking advantage of it as we go. The proposed algorithm is as follows: in the initial iteration, check
	// not only for rediscovering the same state but for discovering a state that can be merged. In that case, store the
	// extra items attached to the original state and don't recurse on the mergeable state. Then, after discovering all states,
	// proceed to, for each state, for each item to merge, add it, then follow each outgoing edge with the new items and continue
	// propagating. The hope is that this will allow us to recreate the lookahead symbols of the original LALR algorithm with much less
	// work because more duplicates will be eliminated upfront. As for how this could fit into the existing structure, I had been thinking
	// to make collectStatesAndEdges overridable and drop the overridable mergeStates method.
//	protected void collectStatesAndEdges(State startState, Set<State> states,
//			Set<Edge> edges, Grammar grammar) {
//		// compute all states
//
//		/*
//		 * Note: the original algorithm repeats looping over the entires set of
//		 * states until neither the state set nor the edge set changes. However,
//		 * for any state/transition symbol type combination we will always
//		 * produce the same state/edge, and thus there's no point in visiting a
//		 * state twice. Thus, instead we keep the queue of states to process in
//		 * a list and loop from i = 0 -> list.size(), where list.size() grows as
//		 * we discover new states. When i catches up with list.size(), the
//		 * algorithm has completed.
//		 * 
//		 * This was found to be substantially more performant that the simple
//		 * translation of the algorithm.
//		 */
//
//		// maps unique item sets to labeled states. Caching states this way
//		// allows us
//		// to have two state/transition symbol combinations that map to
//		// equivalent states
//		// use the same state object (thus slowing new state generation and
//		// allowing the algorithm
//		// to terminate)
//		Map<Set<Item>, State> itemsToStates = new HashMap<Set<Item>, State>();
//		Map<ItemSetWrapper, StatePreMergeContext> mergeContexts = new HashMap<ItemSetWrapper, StatePreMergeContext>();
//		Map<State, Set<Edge>> stateEdges = new HashMap<State, Set<Edge>>();
//		itemsToStates.put(startState.items(), startState);
//
//		List<State> stateList = new ArrayList<State>(itemsToStates.values());
//		// for each state I in T
//		for (int i = 0; i < stateList.size(); ++i) {
//			State fromState = stateList.get(i);
//			// for each X in an item A -> A.XB in I
//			for (SymbolType symbolType : fromState.transitionSymbolTypes()) {
//				Set<Item> toStateItems = this.transition(grammar, fromState,
//						symbolType);
//
//				State toState = itemsToStates.get(toStateItems);
//
//				// T <- T U {J}
//				// create a new state if necessary
//				if (toState == null) {
//					// when we come across a mergeable state, don't create & enqueue it
//					// but instead log its items for merging on the merge context of an already-created state
//					ItemSetWrapper wrapper = new ItemSetWrapper(toStateItems);
//					StatePreMergeContext mergeContext = mergeContexts.get(wrapper);
//					if (mergeContext != null) {
//						mergeContext.equeueMergeableItems(toStateItems);
//						itemsToStates.put(toStateItems, mergeContext.originalState());
//						continue;
//					}
//					
//					toState = new State(
//							String.valueOf(itemsToStates.size() + 1),
//							toStateItems);
//					itemsToStates.put(toState.items(), toState);
//					mergeContexts.put(wrapper, new StatePreMergeContext(toState, wrapper));
//					stateList.add(toState); // queue the new state for further
//											// processing
//				}
//
//				// E <- E U {I -X-> J}
//				// add the edge, if it's new
//				//edges.add(new Edge(fromState, symbolType, toState));
//				Edge edge = new Edge(fromState, symbolType, toState);
//				Set<Edge> fromStateEdges = stateEdges.get(fromState);
//				if (fromStateEdges == null) {
//					stateEdges.put(fromState, Utils.set(edge));
//				} else {
//					fromStateEdges.add(edge);
//				}
//			}
//		}
//		
//		// do merge propagation
//
//		// store edges
//		// store states
//		states.addAll(stateList);
//	}

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
	
//	private final class StatePreMergeContext {
//		private final State originalState;
//		private final ItemSetWrapper wrapper;
//		private Set<Item> itemsToMerge;
//		
//		public StatePreMergeContext(State originalState, ItemSetWrapper wrapper) {
//			this.originalState = originalState;
//			this.wrapper = wrapper;
//		}
//		
//		public State originalState() { return this.originalState; }
//		public ItemSetWrapper wrapper() { return this.wrapper; }
//		
//		public void equeueMergeableItems(Set<Item> items) {
//			if (this.itemsToMerge == null) {
//				this.itemsToMerge = new HashSet<Item>(items);
//			} else { 
//				for (Item item : items) {
//					if (!this.originalState().items().contains(item)) {
//						this.itemsToMerge.add(item);
//					}
//				}
//			}
//		}
//	}
//	
//	private final class ItemSetWrapper {
//		private final Set<Item> items;
//		private int hash = Integer.MAX_VALUE;
//		
//		public ItemSetWrapper(Set<Item> items) {
//			this.items = new HashSet<Item>(items.size());
//			for (Item item : items) {
//				this.items.add(new Item(item.production(), null, item.position()));
//			}
//		}
//		
//		@Override
//		public boolean equals(Object thatObj) {
//			if (this == thatObj) {
//				return true;
//			}
//			
//			ItemSetWrapper that = thatObj instanceof ItemSetWrapper ? (ItemSetWrapper)thatObj : null;
//			return that != null && this.items.equals(that.items);
//		}
//		
//		@Override
//		public int hashCode() {
//			if (this.hash == Integer.MAX_VALUE)
//				this.hash = this.items.hashCode();
//			return this.hash;
//		}
//		
//		@Override
//		public String toString() {
//			return this.items.toString();
//		}
//	}
}
