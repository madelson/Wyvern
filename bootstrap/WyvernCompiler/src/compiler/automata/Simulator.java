package compiler.automata;

/**
 * An interface for simulating a finite automaton.
 * 
 * @author Michael
 * 
 */
public interface Simulator<TState, TTransition> {
	public SimulatorState consume(TTransition input);

	public SimulatorState simulatorState();

	public TState currentValue();

	public SimulatorState reset();
}
