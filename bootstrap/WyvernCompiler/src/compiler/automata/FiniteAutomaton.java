/**
 * 
 */
package compiler.automata;

import java.util.HashSet;
import java.util.Set;

import compiler.Tuples;
import compiler.Utils;

/**
 * @author Michael
 * 
 */
public class FiniteAutomaton<TState, TTransition> extends
		Tuples.Duo<Set<State<TState>>, Set<Edge<TState, TTransition>>> {

	public FiniteAutomaton(Set<State<TState>> states,
			Set<Edge<TState, TTransition>> edges) {
		super(Utils.immutableCopy(states), Utils.immutableCopy(edges));
	}

	public Set<State<TState>> states() {
		return this.item1();
	}

	public Set<Edge<TState, TTransition>> edges() {
		return this.item2();
	}
	
	public boolean isDeterministic() {
		for (Edge<?, ?> edge : this.edges()) {
			if (edge.transitionOnSet() == null) {
				return false;
			}
		}
		
		return true;
	}

	public static <TState, TTransition> Builder<TState, TTransition> dfaBuilder() {
		return new Builder<TState, TTransition>(false);
	}

	public static <TState, TTransition> Builder<TState, TTransition> nfaBuilder() {
		return new Builder<TState, TTransition>(true);
	}
	
	public static class Builder<TState, TTransition> {
		private final boolean allowEpsilonTransitions;
		private final Set<State<TState>> states = new HashSet<State<TState>>();
		private final Set<Edge<TState, TTransition>> edges = new HashSet<Edge<TState, TTransition>>();

		public Builder(boolean allowEpsilonTransitions) {
			this.allowEpsilonTransitions = allowEpsilonTransitions;
		}

		public FiniteAutomaton<TState, TTransition> toFiniteAutomaton() {
			return new FiniteAutomaton<TState, TTransition>(this.states,
					this.edges);
		}

		public State<TState> createState(TState value) {
			State<TState> newState = new State<TState>(String.valueOf(this.states.size()), value);
			this.states.add(newState);

			return newState;
		}

		public State<TState> createState() {
			return this.createState(null);
		}

		public Edge<TState, TTransition> createEdge(State<TState> fromState,
				SetFunction<TTransition> transitionOnSet, State<TState> toState) {
			Utils.check(transitionOnSet != null || this.allowEpsilonTransitions);

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