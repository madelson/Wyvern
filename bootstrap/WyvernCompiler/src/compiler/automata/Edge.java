/**
 * 
 */
package compiler.automata;

import compiler.Tuples;

/**
 * @author Michael
 * 
 */
public class Edge<TState, TTransition> extends
		Tuples.Trio<State<TState>, SetFunction<TTransition>, State<TState>> {

	public Edge(State<TState> from, SetFunction<TTransition> transitionOnSet,
			State<TState> to) {
		super(from, transitionOnSet, to);
	}

	public State<TState> from() {
		return this.item1();
	}

	public SetFunction<TTransition> transitionOnSet() {
		return this.item2();
	}

	public State<TState> to() {
		return this.item3();
	}
}
