/**
 * 
 */
package compiler.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import compiler.Utils;
import compiler.automata.Characters;
import compiler.automata.SetOperations;
import compiler.automata.SimpleSetOperations;

/**
 * @author Michael
 * 
 */
public class AutomataTests {
	// private static final SetFunction<Character> all = SetFunction.all(),
	// empty = SetFunction.empty(), c = SetFunction.singleton('c'),
	// C = SetFunction.singleton('C'), az = SetFunction.range('a', 'z',
	// new Function<Character, Character>() {
	//
	// @Override
	// public Character invoke(Character arg) {
	// return (char) (arg + 1);
	// }
	//
	// }),
	//
	// union = az.union(C);
	//
	// public static void setFunctionTest() {
	// setFunctionContainsTestCase('c', new boolean[] { true, false, true,
	// false, true, true });
	// setFunctionContainsTestCase('C', new boolean[] { true, false, false,
	// true, false, true });
	// setFunctionContainsTestCase('d', new boolean[] { true, false, false,
	// false, true, true });
	// setFunctionContainsTestCase('D', new boolean[] { true, false, false,
	// false, false, false });
	// }
	//
	// private static void setFunctionContainsTestCase(Character ch,
	// boolean[] expectedResults) {
	// Utils.check(all.contains(ch) == expectedResults[0]);
	// Utils.check(empty.contains(ch) == expectedResults[1]);
	// Utils.check(c.contains(ch) == expectedResults[2]);
	// Utils.check(C.contains(ch) == expectedResults[3]);
	// Utils.check(az.contains(ch) == expectedResults[4]);
	// Utils.check(union.contains(ch) == expectedResults[5]);
	// }
	private static final Characters.Range all = Characters.allCharacters(),
			az = Characters.range('a', 'z'), be = Characters.range('b', 'e');
	private static final Set<Character> empty = Collections.emptySet(),
			c = Collections.singleton('c'), C = Collections.singleton('C');

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
			Utils.check(totalSize == Character.MAX_VALUE + 1, "Bad total result count!");
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
			Utils.check(originalChars.equals(resultChars), "Unions are not equal!");
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		rangeTest();

		setOperationsTest(new SimpleSetOperations<Character>(), false);
		setOperationsTest(Characters.setOperations(), true);

		// setFunctionTest();

		System.out.println("All automata tests passed!");
	}

}
