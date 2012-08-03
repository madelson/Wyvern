/**
 * 
 */
package compiler.automata;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import compiler.Utils;

/**
 * @author Michael
 * 
 */
public class Characters {
	private static final SetOperations<Character> setOperations = new SetOperations<Character>() {
		private final SetOperations<Character> simpleSetOperations = new SimpleSetOperations<Character>();
		
		@Override
		public Set<Collection<Character>> partitionedUnion(
				Collection<Collection<Character>> sets) {
			// re-represent all sets as ranges
			Set<Range> ranges = new HashSet<Range>();
			for (Collection<Character> set : sets) {
				if (set instanceof Range) {
					ranges.add((Range) set);
				} else {
					for (Character ch : set) {
						ranges.add(range(ch, ch));
					}
				}
			}

			// partition the ranges
			Set<Range> partitionedRanges = this.partitionedRangeUnion(ranges);

			// eliminate single-char ranges
			Set<Collection<Character>> result = new HashSet<Collection<Character>>();
			Set<Character> singletonChars = new HashSet<Character>();
			for (Range range : partitionedRanges) {
				if (range.size() == 1) {
					singletonChars.add(range.min());
				} else {
					result.add(range);
				}
			}

			// merge singletons where possible
			result.addAll(SimpleSetOperations.groupByContains(singletonChars,
					new ArrayList<Collection<Character>>(sets)));

			return result;
		}

		/**
		 * As normal range partitioning, but operates only on a set of Ranges
		 */
		private Set<Range> partitionedRangeUnion(Collection<Range> ranges) {
			if (ranges.isEmpty()) {
				return Collections.emptySet();
			}

			/*
			 * Associate each point with the number of ranges which start and
			 * end on it.
			 */
			SortedMap<Character, Integer> minValues = new TreeMap<Character, Integer>(), maxValues = new TreeMap<Character, Integer>();
			SortedSet<Character> allValues = new TreeSet<Character>();
			for (Range range : ranges) {
				minValues.put(range.min(),
						Utils.getOrDefault(minValues, range.min(), 0) + 1);
				maxValues.put(range.max(),
						Utils.getOrDefault(maxValues, range.max(), 0) + 1);
				Collections.addAll(allValues, range.min(), range.max());
			}

			/*
			 * Scan through the points, creating a range when some range covers
			 * that region.
			 */
			Set<Range> result = new LinkedHashSet<Range>();
			// the last char which is not yet represented by a set in result
			char lastUnrepresentedChar = allValues.first();
			// the number of ranges which are active at lastUnrepresentedChar
			int activeRangeCount = 0;
			for (Character ch : allValues) {
				int startingRangeCount = Utils.getOrDefault(minValues, ch, 0);
				int endingRangeCount = Utils.getOrDefault(maxValues, ch, 0);

				/*
				 * If ranges start at ch, we need a gap range through ch - 1 if
				 * there are active ranges and if at least one of those chars
				 * remains unrepresented. Also, we need a singleton set for ch
				 * if ranges also end at ch.
				 */
				if (startingRangeCount > 0) {
					if (activeRangeCount > 0 && lastUnrepresentedChar <= ch - 1) {
						result.add(range(lastUnrepresentedChar, (char) (ch - 1)));
					}

					if (endingRangeCount > 0) {
						result.add(range(ch, ch));
						// ch is already represented
						lastUnrepresentedChar = (char) (ch + 1);
					} else {
						lastUnrepresentedChar = ch;
					}
				}
				/*
				 * If ranges just end at ch, we need a gap range through ch.
				 */
				else {
					Utils.check(endingRangeCount > 0); // sanity check
					Utils.check(activeRangeCount > 0); // sanity check

					result.add(range(lastUnrepresentedChar, ch));
					// ch is already represented
					lastUnrepresentedChar = (char) (ch + 1);
				}

				activeRangeCount += (startingRangeCount - endingRangeCount);
			}

			return result;
		}

		@Override
		public Character min(Collection<Character> collection) {
			if (collection instanceof Range) {
				return ((Range)collection).min();
			}
			return this.simpleSetOperations.min(collection);
		}

		@Override
		public Character max(Collection<Character> collection) {
			if (collection instanceof Range) {
				return ((Range)collection).max();
			}
			return this.simpleSetOperations.max(collection);
		}
	};

	private static final Range allCharacters = new Range(Character.MIN_VALUE,
			Character.MAX_VALUE);

	public static SetOperations<Character> setOperations() {
		return setOperations;
	}

	public static Range allCharacters() {
		return allCharacters;
	}

	public static Range range(char min, char max) {
		return new Range(min, max);
	}

	/**
	 * Collection class which efficiently stores an arbitrarily large range of
	 * characters. Ranges represented by this class are inclusive. Although
	 * ranges are technically sets as well as lists, they only implement
	 * Collection due to the difficulty of efficiently implementing either
	 * Set.equals or List.equals.
	 */
	public static class Range extends AbstractCollection<Character> {
		private final char min, max;

		public Range(char min, char max) {
			this.min = min;
			this.max = max;

			Utils.check(this.min <= this.max);
		}

		public char min() {
			return this.min;
		}

		public char max() {
			return this.max;
		}

		@Override
		public Iterator<Character> iterator() {
			return new Iterator<Character>() {
				private char next = Range.this.min;

				@Override
				public boolean hasNext() {
					return Range.this.contains(this.next);
				}

				@Override
				public Character next() {
					if (!this.hasNext()) {
						throw new NoSuchElementException();
					}

					char ret = this.next;
					this.next++;
					return ret;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public int size() {
			return this.max - this.min + 1;
		}

		@Override
		public boolean contains(Object obj) {
			if (obj instanceof Character) {
				char value = (Character) obj;

				return value >= min && value <= max;
			}
			return false;
		}

		@Override
		public boolean containsAll(Collection<?> collection) {
			if (collection instanceof Range) {
				Range range = (Range) collection;

				return range.min >= this.min && range.max <= this.max;
			}
			return super.containsAll(collection);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Range) {
				Range that = (Range) obj;
				return that.min == this.min && that.max == this.max;
			}

			return false;
		}

		@Override
		public int hashCode() {
			return this.min * this.max;
		}

		@Override
		public String toString() {
			return String.format("[%s through %s]", this.min, this.max);
		}

		public boolean overlaps(Range that) {
			return !(this.max < that.min || that.max < this.min);
		}
	}
}