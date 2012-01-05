/**
 * 
 */
package compiler.automata;

import compiler.Utils;

/**
 * @author Michael
 * 
 */
public class DfaSimulator<TState, TTransition> implements
		Simulator<TState, TTransition> {
	private final FiniteAutomaton<TState, TTransition> automaton;
	private State<TState> currentState;

	public DfaSimulator(FiniteAutomaton<TState, TTransition> automaton) {
		this.automaton = automaton;
		this.currentState = this.automaton.startState();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see compiler.automata.Simulator#consume(java.lang.Object)
	 */
	@Override
	public SimulatorState consume(TTransition input) {
		if (this.currentState != null) {
			for (Edge<TState, TTransition> edge : this.automaton
					.edgesFrom(this.currentState)) {
				if (edge.transitionOnSet().contains(input)) {
					this.currentState = edge.to();
					return this.currentState.value() != null ? SimulatorState.Accept
							: SimulatorState.Reject;
				}
			}

			this.currentState = null;
		}
		return SimulatorState.Error;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see compiler.automata.Simulator#simulatorState()
	 */
	@Override
	public SimulatorState simulatorState() {
		return this.currentState != null ? (this.currentState.value() != null ? SimulatorState.Accept
				: SimulatorState.Reject)
				: SimulatorState.Error;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see compiler.automata.Simulator#currentValue()
	 */
	@Override
	public TState currentValue() {
		Utils.check(this.currentState != null, "The DFA is in an error state!");
		return this.currentState.value();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see compiler.automata.Simulator#reset()
	 */
	@Override
	public SimulatorState reset() {
		this.currentState = this.automaton.startState();
		return this.simulatorState();
	}

}
