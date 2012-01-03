/**
 * 
 */
package compiler.automata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compiler.Tuples;
import compiler.Utils;

/**
 * @author Michael
 * 
 */
public class FiniteAutomaton<TState, TTransition>
		extends
		Tuples.Trio<Set<State<TState>>, Set<Edge<TState, TTransition>>, State<TState>> {
	private final SetOperations<TTransition> setOperations;
	private final Map<State<TState>, Set<Edge<TState, TTransition>>> groupedEdges;

	public FiniteAutomaton(LinkedHashSet<State<TState>> states,
			Set<Edge<TState, TTransition>> edges,
			SetOperations<TTransition> setOperations) {
		super(Utils.immutableCopy(states), Utils.immutableCopy(edges), states
				.iterator().next());

		this.setOperations = setOperations;

		Map<State<TState>, Set<Edge<TState, TTransition>>> grouped = new HashMap<State<TState>, Set<Edge<TState, TTransition>>>();

		for (Edge<TState, TTransition> edge : edges) {
			Utils.put(grouped, HashSet.class, edge.from(), edge);
		}

		this.groupedEdges = Utils.deepImmutableCopy(grouped);
	}

	public Set<State<TState>> states() {
		return this.item1();
	}

	public Set<Edge<TState, TTransition>> edges() {
		return this.item2();
	}

	public Set<Edge<TState, TTransition>> edgesFrom(State<TState> state) {
		Set<Edge<TState, TTransition>> edges = this.groupedEdges.get(state);
		if (edges == null) {
			return Collections.emptySet();
		}

		return edges;
	}

	public State<TState> startState() {
		return this.item3();
	}

	public FiniteAutomaton<TState, TTransition> toDfa(
			LinkedHashSet<TState> valuesByPrecedence) {
		List<Set<State<TState>>> dfaStates = new ArrayList<Set<State<TState>>>();
		Map<Set<State<TState>>, Integer> dfaStateIndices = new HashMap<Set<State<TState>>, Integer>();
		Map<Integer, Map<Collection<TTransition>, Integer>> transitions = new HashMap<Integer, Map<Collection<TTransition>, Integer>>();

		dfaStates.add(this.closure(Collections.singleton(this.startState())));
		dfaStateIndices.put(dfaStates.get(0), 0);

		for (int j = 0; j < dfaStates.size(); j++) {
			// get the "alphabet"
			Set<Collection<TTransition>> transitionSets = new HashSet<Collection<TTransition>>();
			for (State<TState> state : dfaStates.get(j)) {
				for (Edge<TState, TTransition> edge : this.edgesFrom(state)) {
					// ignore epsilon transitions, since we're working with an
					// epsilon closure
					if (edge.transitionOnSet() != null) {
						transitionSets.add(edge.transitionOnSet());
					}
				}
			}

			Set<Collection<TTransition>> transitionSetAlphabet = this.setOperations
					.partitionedUnion(transitionSets);

			for (Collection<TTransition> transitionSet : transitionSetAlphabet) {
				Set<State<TState>> reachableStates = this.reachableStates(
						dfaStates.get(j), transitionSet);
				int dfaStateIndex;

				// if the set of reachable states is a dfa state we've already
				// found, update it's transitions
				if (dfaStateIndices.containsKey(reachableStates)) {
					dfaStateIndex = dfaStateIndices.get(reachableStates);
					Utils.check(dfaStateIndex <= dfaStates.size()); // sanity
																	// check
				}
				// otherwise, add a new dfa state
				else {
					dfaStateIndex = dfaStates.size();
					dfaStates.add(reachableStates);
					dfaStateIndices.put(reachableStates, dfaStateIndex);
				}
				Utils.check(Utils.put(transitions, HashMap.class, j,
						transitionSet, dfaStateIndex) == null);
			}
		}

		// build the new dfa
		Builder<TState, TTransition> builder = builder(this.setOperations);
		List<State<TState>> combinedStates = new ArrayList<State<TState>>(
				dfaStates.size());

		// create the combined states
		for (Set<State<TState>> dfaState : dfaStates) {
			combinedStates.add(builder.newState(getValueForCombinedState(
					dfaState, valuesByPrecedence)));
		}

		// create the edges
		for (int fromStateIndex : transitions.keySet()) {
			Map<Collection<TTransition>, Integer> edges = transitions
					.get(fromStateIndex);
			for (Collection<TTransition> transitionSet : edges.keySet()) {
				int toStateIndex = edges.get(transitionSet);
				builder.createEdge(combinedStates.get(fromStateIndex),
						transitionSet, combinedStates.get(toStateIndex));
			}
		}

		return builder.toFiniteAutomaton();
	}

	private static <S> S getValueForCombinedState(Set<State<S>> states,
			LinkedHashSet<S> valuesByPrecedence) {
		Set<S> values = new HashSet<S>();
		for (State<S> state : states) {
			values.add(state.value());
		}

		for (S value : valuesByPrecedence) {
			if (values.contains(value)) {
				return value;
			}
		}

		return null;
	}

	/**
	 * Returns the set of states reachable from any of the given states without
	 * consuming any input. For a dfa, this should always be {state}
	 */
	public Set<State<TState>> closure(Set<State<TState>> states) {
		/*
		 * returns T = states U (U {edge(s, epsilon) for s in T})
		 */
		Set<State<TState>> closure = new HashSet<State<TState>>(states);

		boolean changed;
		do {
			changed = false;

			for (State<TState> closureState : new ArrayList<State<TState>>(
					closure)) {
				for (Edge<TState, TTransition> edge : this
						.edgesFrom(closureState)) {
					if (edge.transitionOnSet() == null) {
						changed |= closure.add(edge.to());
					}
				}
			}
		} while (changed);

		return closure;
	}

	/**
	 * Returns the set of states reachable from any of the given set of states
	 * by consuming a symbol from the given set of inputs. The provided state
	 * set must be a an epsilon closure, while the provided input set must
	 * either not intersect with or be a subset of each of the transition sets
	 * of the edges leading outward from the provided states
	 */
	public Set<State<TState>> reachableStates(Set<State<TState>> fromStates,
			Collection<TTransition> inputSet) {
		/*
		 * returns closure(U {edge(s, transitionOnSet) for s in dfaState})
		 */
		Set<State<TState>> reachableStates = new HashSet<State<TState>>();

		for (State<TState> state : new ArrayList<State<TState>>(fromStates)) {
			for (Edge<TState, TTransition> edge : this.edgesFrom(state)) {
				// ignore epsilon edges since we assume that the provided state
				// set is an epsilon closure
				if (edge.transitionOnSet() != null
						&& edge.transitionOnSet().containsAll(inputSet)) {
					reachableStates.add(edge.to());
				}
			}
		}

		return this.closure(reachableStates);
	}

	public static <TState, TTransition> Builder<TState, TTransition> builder(
			SetOperations<TTransition> setOperations) {
		return new Builder<TState, TTransition>(setOperations);
	}

	public static class Builder<TState, TTransition> {
		private final SetOperations<TTransition> setOperations;
		private final LinkedHashSet<State<TState>> states = new LinkedHashSet<State<TState>>();
		private final Set<Edge<TState, TTransition>> edges = new HashSet<Edge<TState, TTransition>>();

		public Builder(SetOperations<TTransition> setOperations) {
			this.setOperations = setOperations;
		}

		public FiniteAutomaton<TState, TTransition> toFiniteAutomaton() {
			return new FiniteAutomaton<TState, TTransition>(this.states,
					this.edges, this.setOperations);
		}

		public State<TState> newState(TState value) {
			State<TState> newState = new State<TState>(
					String.valueOf(this.states.size()), value);
			this.states.add(newState);

			return newState;
		}

		public State<TState> newState() {
			return this.newState(null);
		}

		public Edge<TState, TTransition> createEdge(State<TState> fromState,
				Collection<TTransition> transitionOnSet, State<TState> toState) {
			Edge<TState, TTransition> newEdge = new Edge<TState, TTransition>(
					fromState, transitionOnSet, toState);
			this.edges.add(newEdge);

			return newEdge;
		}

		public Edge<TState, TTransition> createEdge(State<TState> fromState,
				State<TState> toState) {
			return this.createEdge(fromState, null, toState);
		}
	}
}
