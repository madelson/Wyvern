/**
 * 
 */
package compiler.automata;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import compiler.Utils;

/**
 * @author Michael
 * 
 */
public class Characters {
	private static final SetOperations<Character> simpleSetOperations = new SimpleSetOperations<Character>();

	private static final SetOperations<Character> setOperations = new SetOperations<Character>() {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * compiler.automata.SetOperations#partitionedUnion(java.util.Collection
		 * )
		 */
		@Override
		public Set<Collection<Character>> partitionedUnion(
				Collection<Collection<Character>> sets) {
			List<Collection<Character>> result = new ArrayList<Collection<Character>>(
					sets);
			boolean changed;
			do {
				changed = false;

				OUTER: for (int i = 0; i < result.size(); i++) {
					for (int j = i + 1; j < result.size(); j++) {
						Set<Collection<Character>> pairResult = this
								.partitionedUnion(result.get(i), result.get(j));
						if (pairResult != null) {
							result.remove(j); // remove at j first since j > i
							result.remove(i);
							result.addAll(pairResult);
							changed = true;
							break OUTER;
						}
					}
				}
			} while (changed);

			return new HashSet<Collection<Character>>(result);
		}

		/**
		 * Performs the partitioned union operation on just two sets, returning
		 * null the result would be { set1, set2 }.
		 */
		private Set<Collection<Character>> partitionedUnion(
				Collection<Character> set1, Collection<Character> set2) {
			// optimize for ranges
			if (set1 instanceof Range) {
				Range range1 = (Range) set1;

				// range overlap
				if (set2 instanceof Range) {
					Range range2 = (Range) set2;
					if (range1.overlaps(range2)) {
						char[] boundaries = new char[] { range1.min(),
								range1.max(), range2.min(), range2.max() };
						Arrays.sort(boundaries);
						Set<Collection<Character>> result = new HashSet<Collection<Character>>();
						
						// wrong
						for (int i = 0; i < boundaries.length - 1; i++) {
							if (boundaries[i] != boundaries[i + 1]) {
								result.add(range((char) (boundaries[i] + 1),
										boundaries[i + 1]));
							}
						}

						return result;
					}
					return null; // no change
				}

				// normal case
				Collection<Character> beforeChars = new ArrayList<Character>(), afterChars = new ArrayList<Character>(), overlapChars = new ArrayList<Character>();
				for (Character ch : set2) {
					if (ch < range1.min()) {
						beforeChars.add(ch);
					} else if (ch > range1.max()) {
						afterChars.add(ch);
					} else {
						overlapChars.add(ch);
					}
				}

				if (overlapChars.isEmpty()) {
					return null; // no change
				}

				Set<Collection<Character>> result = new HashSet<Collection<Character>>();
				if (!beforeChars.isEmpty()) {
					result.add(beforeChars);
				}
				if (!afterChars.isEmpty()) {
					result.add(afterChars);
				}

				overlapChars.add(range1.min());
				overlapChars.add(range1.max());
				// sort and dedup
				overlapChars = new TreeSet<Character>(overlapChars);

				// wrong
				Character prev = null;
				for (Character ch : overlapChars) {
					if (prev != null && ch > prev) {
						result.add(new Range(prev, ch));
						prev = (char) (ch + 1);
					} else {
						prev = ch;
					}
				}
				Utils.check(!result.isEmpty()); // sanity check
				return result;
			}

			// be indifferent to argument order
			if (set2 instanceof Range) {
				return this.partitionedUnion(set2, set1);
			}

			// no ranges, so just use the simple set operations logic
			List<Collection<Character>> sets = new ArrayList<Collection<Character>>(
					2);
			sets.add(set1);
			sets.add(set2);
			return simpleSetOperations.partitionedUnion(sets);
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