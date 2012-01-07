/**
 * 
 */
package compiler.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import compiler.Utils;
import compiler.automata.Characters;
import compiler.automata.DfaSimulator;
import compiler.automata.FiniteAutomaton;
import compiler.automata.SetOperations;
import compiler.automata.SimpleSetOperations;
import compiler.automata.Simulator;
import compiler.automata.SimulatorState;
import compiler.automata.State;

/**
 * @author Michael
 * 
 */
public class AutomataTests {

	private static void rangeTest() {
		// test all
		List<Character> chars = new ArrayList<Character>(Utils.set(
				Character.MIN_VALUE, 'a', Character.MAX_VALUE));
		Utils.check(Characters.allCharacters().containsAll(chars));

		// test single-char range
		Characters.Range range = Characters.range('a', 'a');
		Utils.check(range.contains('a'));
		Utils.check(!range.contains('b'));
		Utils.check(range.size() == 1);

		// test overlaps
		range = Characters.range('l', 'p');
		Utils.check(range.overlaps(Characters.range('a', 'l')));
		Utils.check(range.overlaps(Characters.range('p', 'z')));
		Utils.check(range.overlaps(Characters.range('k', 'l')));
		Utils.check(range.overlaps(Characters.range('p', 'q')));
		Utils.check(range.overlaps(Characters.range('m', 'n')));
		Utils.check(range.overlaps(Characters.range('a', 'l')));
		Utils.check(!range.overlaps(Characters.range(Character.MIN_VALUE, 'k')));
		Utils.check(!range.overlaps(Characters.range('q', Character.MAX_VALUE)));

		// test iterator
		Utils.check(new ArrayList<Character>(Characters.range('a', 'c'))
				.equals(Arrays.asList(new Character[] { 'a', 'b', 'c' })));

		// test containsAll
		range = Characters.range('a', 'z');
		Utils.check(range.containsAll(Characters.range('a', 'm')));
		Utils.check(range.containsAll(Characters.range('m', 'z')));
		Utils.check(range.containsAll(Characters.range('b', 'c')));
		Utils.check(!range.containsAll(Characters.allCharacters()));
		Utils.check(!range.containsAll(Characters.range((char) ('a' - 1), 'a')));
		Utils.check(!range.containsAll(Characters.range('z', (char) ('z' + 1))));
		Utils.check(!range.containsAll(Characters.range('A', 'Z')));
	}

	public static void setOperationsTest(SetOperations<Character> ops,
			boolean tryLargeRange) {
		Collection<Collection<Character>> sets = new ArrayList<Collection<Character>>();
		sets.add(Utils.set('a'));
		sets.add(Utils.set('m'));
		sets.add(Characters.range('a', 'z'));
		sets.add(Utils.set('x', 'y'));
		sets.add(Characters.range('p', 's'));
		if (tryLargeRange) {
			sets.add(Characters.allCharacters());
		}
		Set<Collection<Character>> result = ops.partitionedUnion(sets);

		// test no overlap
		for (Collection<Character> collection1 : result) {
			for (Collection<Character> collection2 : result) {
				Characters.Range range1 = Utils.cast(collection1,
						Characters.Range.class), range2 = Utils.cast(
						collection2, Characters.Range.class);
				if (collection1.equals(collection2)) {
					continue;
				}
				if (range1 != null && range2 != null) {
					Utils.check(!range1.overlaps(range2));
				} else if (range2 == null) {
					Utils.check(!Utils.intersects(collection1, collection2));
				} else {
					Utils.check(!Utils.intersects(collection2, collection1));
				}
			}
		}

		// test if contains any then contains all
		for (Collection<Character> set : sets) {
			for (Collection<Character> resultCollection : result) {
				Characters.Range setRange = Utils.cast(set,
						Characters.Range.class), resultRange = Utils.cast(
						resultCollection, Characters.Range.class);
				if (setRange != null && resultRange != null) {
					Utils.check(setRange.overlaps(resultRange) == setRange
							.containsAll(resultRange));
				} else {
					Utils.check(Utils.intersects(set, result) == set
							.containsAll(result));
				}
			}
		}

		/*
		 * First half of the "unions are the same" test.
		 */
		for (Collection<Character> resultCollection : result) {
			boolean found = false;
			for (Collection<Character> set : sets) {
				if (set.containsAll(resultCollection)) {
					found = true;
					break;
				}
			}
			Utils.check(found, "Result contains extra characters!");
		}

		/*
		 * The large range is all characters, so we know that the total count
		 * should be max(char) + 1.
		 */
		if (tryLargeRange) {
			int totalSize = 0;
			for (Collection<Character> resultCollection : result) {
				totalSize += resultCollection.size();
			}
			Utils.check(totalSize == Character.MAX_VALUE + 1,
					"Bad total result count!");
		}
		// simple union check
		else {
			Set<Character> originalChars = new HashSet<Character>(), resultChars = new HashSet<Character>();
			for (Collection<Character> set : sets) {
				originalChars.addAll(set);
			}
			for (Collection<Character> resultCollection : result) {
				resultChars.addAll(resultCollection);
			}
			Utils.check(originalChars.equals(resultChars),
					"Unions are not equal!");
		}
	}

	public static void dfaConversionTest() {
		LinkedHashSet<String> values = Utils.set("aa", "bb", "cc", "dd");

		// dfa.toDfa() == reachable states in dfa
		FiniteAutomaton.Builder<String, Character> builder = FiniteAutomaton
				.builder(Characters.setOperations());
		State<String> s1 = builder.newState();

		// minimal test
		Utils.check(builder.toFiniteAutomaton().equals(
				builder.toFiniteAutomaton().toDfa(values)));

		// simple dfa
		State<String> s2 = builder.newState();
		builder.createEdge(s1, Characters.range('a', 'c'), s2);
		builder.createEdge(s2, Collections.singleton('A'), s1);
		Utils.check(builder.toFiniteAutomaton().equals(
				builder.toFiniteAutomaton().toDfa(values)));

		// an unreachable state should be removed
		State<String> s3 = builder.newState("dd");
		Utils.check(!builder.toFiniteAutomaton().equals(
				builder.toFiniteAutomaton().toDfa(values)));
		Utils.check(builder.toFiniteAutomaton().edges()
				.equals(builder.toFiniteAutomaton().toDfa(values).edges()));
		Utils.check(builder.toFiniteAutomaton().startState()
				.equals(builder.toFiniteAutomaton().toDfa(values).startState()));
		Set<State<String>> states = new HashSet<State<String>>(builder
				.toFiniteAutomaton().states());
		states.remove(s3);
		Utils.check(states.equals(builder.toFiniteAutomaton().toDfa(values)
				.states()));

		// actual nfa -> dfa conversion
		builder.createEdge(s1, Characters.range('b', 'd'), s3);
		State<String> s4 = builder.newState("aa");
		builder.createEdge(s3, s4);
		FiniteAutomaton<String, Character> auto = builder.toFiniteAutomaton()
				.toDfa(values);
		Utils.check(auto.states().size() == 4);
		Utils.check(auto.edges().size() == 5);
		Utils.check(auto.startState().value() == null);
		int nullCount = 0, aaCount = 0;
		for (State<String> state : auto.states()) {
			if (state.value() == null) {
				nullCount++;
			} else if (state.value().equals("aa")) {
				aaCount++;
			} else {
				Utils.err("Bad value!");
			}
		}
		Utils.check(nullCount == 2);
		Utils.check(aaCount == 2);
	}

	public static void simulatorTest() {
		FiniteAutomaton.Builder<Integer, Character> builder = FiniteAutomaton
				.builder(Characters.setOperations());
		State<Integer> s1 = builder.newState(), s2 = builder.newState(), s3 = builder
				.newState(3), s4 = builder.newState(), s5 = builder.newState(5);
		Characters.Range digits = Characters.range('0', '9');
		Set<Character> dot = Collections.singleton('.');
		builder.createEdge(s1, digits, s2);
		builder.createEdge(s2, digits, s2);
		builder.createEdge(s2, dot, s3);
		builder.createEdge(s3, digits, s3);
		builder.createEdge(s1, dot, s4);
		builder.createEdge(s4, digits, s5);
		builder.createEdge(s5, digits, s5);

		FiniteAutomaton<Integer, Character> auto = builder.toFiniteAutomaton();
		Simulator<Integer, Character> sim = new DfaSimulator<Integer, Character>(
				auto);

		checkSimulator(sim, "", new Integer[] { null },
				new SimulatorState[] { SimulatorState.Reject });
		checkSimulator(sim, "2.3", new Integer[] { null, null, 3, 3 },
				new SimulatorState[] { SimulatorState.Reject,
						SimulatorState.Reject, SimulatorState.Accept,
						SimulatorState.Accept });
		checkSimulator(sim, ".35a", new Integer[] { null, null, 5, 5, -1 },
				new SimulatorState[] { SimulatorState.Reject,
						SimulatorState.Reject, SimulatorState.Accept,
						SimulatorState.Accept, SimulatorState.Error });

	}

	private static void checkSimulator(Simulator<Integer, Character> sim,
			String inputs, Integer[] outputs, SimulatorState[] states) {
		sim.reset();

		for (int i = 0; i < outputs.length; i++) {
			Utils.check(sim.simulatorState().equals(states[i]));
			if (sim.simulatorState() != SimulatorState.Error) {
				Utils.check(Utils.equals(sim.currentValue(), outputs[i]));
			}
			if (i < inputs.length()) {
				sim.consume(inputs.charAt(i));
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		rangeTest();

		setOperationsTest(new SimpleSetOperations<Character>(), false);
		setOperationsTest(Characters.setOperations(), true);

		dfaConversionTest();

		simulatorTest();

		System.out.println("All automata tests passed!");
	}

}
