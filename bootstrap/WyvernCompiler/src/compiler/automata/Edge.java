/**
 * 
 */
package compiler.automata;

import java.util.Collection;

import compiler.Tuples;

/**
 * @author Michael
 * 
 */
public class Edge<TState, TTransition> extends
		Tuples.Trio<State<TState>, Collection<TTransition>, State<TState>> {

	public Edge(State<TState> from, Collection<TTransition> transitionOnSet,
			State<TState> to) {
		super(from, transitionOnSet, to);
	}

	public State<TState> from() {
		return this.item1();
	}

	public Collection<TTransition> transitionOnSet() {
		return this.item2();
	}

	public State<TState> to() {
		return this.item3();
	}
}
