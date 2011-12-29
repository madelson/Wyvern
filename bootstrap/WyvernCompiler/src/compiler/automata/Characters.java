/**
 * 
 */
package compiler.automata;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
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
	private static final SetOperations<Character> simpleSetOperations = new SimpleSetOperations<Character>();

	private static final SetOperations<Character> setOperations = new SetOperations<Character>() {

		@Override
		public Set<Collection<Character>> partitionedUnion(
				Collection<Collection<Character>> sets) {
			/*
			 * Organize the given sets by their members. Ranges are collected
			 * separately for efficiency
			 */
			Set<Range> ranges = new HashSet<Range>();
			SortedMap<Character, Set<Collection<Character>>> collectionsByCharacter = new TreeMap<Character, Set<Collection<Character>>>();
			for (Collection<Character> set : sets) {
				if (set instanceof Range) {
					ranges.add((Range) set);
				} else {
					for (Character ch : set) {
						Utils.put(collectionsByCharacter, HashSet.class, ch,
								set);
					}
				}
			}

			// partition the ranges

			Set<Collection<Character>> result = new LinkedHashSet<Collection<Character>>();

			// find any ranges before the first character
			Character lowerBound = null;
			Character upperBound = collectionsByCharacter.isEmpty() ? null
					: collectionsByCharacter.firstKey();
			Set<Range> activeRanges = this.getActiveRanges(ranges, lowerBound,
					upperBound);
			result.addAll(this.partitionedRangeUnion(activeRanges, lowerBound,
					upperBound));

			// scan through the collected characters and ranges, adding
			// collections as necessary
			Set<Collection<Character>> currentSets = new HashSet<Collection<Character>>();
			Set<Character> currentChars = new HashSet<Character>();
			Character prev = null;
			for (Character ch : collectionsByCharacter.keySet()) {
				// find ranges between the last character and the current one
				if (prev != null) {
					activeRanges = this.getActiveRanges(ranges, prev, ch);
					result.addAll(this.partitionedRangeUnion(activeRanges,
							prev, ch));
				}

				// if the current character is associated with all the same sets
				// as the previous one, just add it
				if (collectionsByCharacter.get(ch).equals(currentSets)) {
					currentChars.add(ch);
				}

				// otherwise, create a set for all previous characters (if any)
				// and save the current character and collection set
				else {
					if (!currentChars.isEmpty()) {
						result.add(currentChars);
						currentChars = new HashSet<Character>();
						currentSets.clear();
					}
					currentChars.add(ch);
					currentSets.addAll(collectionsByCharacter.get(ch));
				}

				prev = ch;
			}

			// finish creating any remaining sets
			if (!currentChars.isEmpty()) {
				result.add(currentChars);
			}

			/*
			 * find ranges between the last character and the end of the
			 * alphabet Note that this won't run for an empty
			 * collectionsByCharacter set, which is what we want since in that
			 * case the
			 */
			if (prev != null) {
				activeRanges = this.getActiveRanges(ranges, prev, null);
				result.addAll(this.partitionedRangeUnion(activeRanges, prev,
						null));
			}
		}

		private Set<Range> getActiveRanges(Set<Range> ranges,
				Character lowerBound, Character upperBound) {
			if (ranges.isEmpty()) {
				return Collections.emptySet();
			}

			Set<Range> activeRanges = new HashSet<Range>();
			for (Range range : ranges) {
				if ((lowerBound == null || range.min() > lowerBound)
						&& (upperBound == null || range.max() < upperBound)) {
					activeRanges.add(range);
				}
			}

			return activeRanges;
		}

		/**
		 * As normal range partitioning, but operates only on a set of Ranges
		 */
		private Set<Collection<Character>> partitionedRangeUnion(
				Collection<Range> ranges, Character lowerBound,
				Character upperBound) {
			if (ranges.isEmpty()) {
				return Collections.emptySet();
			}

			// associate each point with the number of ranges which start and
			// end on it
			SortedMap<Character, Integer> minValues = new TreeMap<Character, Integer>(), maxValues = new TreeMap<Character, Integer>();
			SortedSet<Character> allValues = new TreeSet<Character>();
			for (Range range : ranges) {
				Range trimmed = this.trim(range, lowerBound, upperBound);
				minValues.put(trimmed.min(),
						Utils.getOrDefault(minValues, trimmed.min(), 0) + 1);
				maxValues.put(trimmed.max(),
						Utils.getOrDefault(maxValues, trimmed.max(), 0) + 1);
				allValues.add(trimmed.min());
				allValues.add(trimmed.max());
			}

			/*
			 * Scan through the points, creating a range when some range covers
			 * that region.
			 */
			Set<Collection<Character>> result = new LinkedHashSet<Collection<Character>>();
			// the last char which is not yet represented by a set in result
			char lastUnrepresentedChar = allValues.first();
			// the number of ranges which are active at lastUnrepresentedChar
			int activeRangeCount = 0;
			for (Character ch : allValues) {
				int startingRangeCount = Utils.getOrDefault(minValues, ch, 0);
				int endingRangeCount = Utils.getOrDefault(maxValues, ch, 0);

				/*
				 * If ranges start at ch, we need a gap range through ch - 1 if
				 * there are active ranges and we need a singleton set for ch if
				 * ranges also end at ch.
				 */
				if (startingRangeCount > 0) {
					if (activeRangeCount > 0) {
						// sanity check
						Utils.check(lastUnrepresentedChar <= ch - 1);

						result.add(lastUnrepresentedChar < ch - 1 ? range(
								lastUnrepresentedChar, (char) (ch - 1))
								: Collections.singleton((char) (ch - 1)));
					}

					if (endingRangeCount > 0) {
						Utils.check(activeRangeCount > 0); // sanity check

						result.add(Collections.singleton(ch));
						lastUnrepresentedChar = (char) (ch + 1); // ch already
																	// represented
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

					result.add(lastUnrepresentedChar == ch ? Collections
							.singleton(ch) : range(lastUnrepresentedChar, ch));
					lastUnrepresentedChar = (char) (ch + 1); // ch already
																// represented
				}

				activeRangeCount += (startingRangeCount - endingRangeCount);
			}

			return result;
		}

		private Range trim(Range range, Character lowerBound,
				Character upperBound) {
			char min = lowerBound != null ? (char) Math.max(range.min(),
					lowerBound) : range.min();
			char max = upperBound != null ? (char) Math.min(range.max(),
					upperBound) : range.max();

			if (min == range.min() && max == range.max()) {
				return range;
			}
			return range(min, max);
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

		public boolean overlaps(Range that) {
			return !(this.max < that.min || that.max < this.min);
		}
	}
}