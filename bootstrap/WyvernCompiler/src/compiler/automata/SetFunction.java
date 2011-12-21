/**
 * 
 */
package compiler.automata;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import compiler.Utils;

/**
 * @author Michael
 * 
 * An abstract set which is not iterable but which can support arbitrary contains() logic
 */
public abstract class SetFunction<T> {
	private Set<T> knownMembers = null;

	/**
	 * Does the set contain obj?
	 */
	public abstract boolean contains(T obj);

	/**
	 * A set of iterable members of the set function
	 */
	public Set<T> knownMembers() {
		if (this.knownMembers == null) {
			this.knownMembers = this.getKnownMembers();
		}

		return this.knownMembers;
	}

	/**
	 * Are all of of the set functions' members in knownMembers?
	 */
	public boolean knowsAllMembers() {
		return true;
	}

	protected abstract Set<T> getKnownMembers();

	public SetFunction<T> union(final SetFunction<T> that) {
		final SetFunction<T> self = this;

		return new SetFunction<T>() {

			@Override
			public boolean contains(T obj) {
				return self.contains(obj) || that.contains(obj);
			}

			@Override
			protected Set<T> getKnownMembers() {
				Set<T> union = new HashSet<T>(self.knownMembers());
				union.addAll(that.knownMembers());

				return Collections.unmodifiableSet(union);
			}

			@Override
			public boolean knowsAllMembers() {
				return self.knowsAllMembers() && that.knowsAllMembers();
			}

			@Override
			public String toString() {
				return String.format("%s U %s", self, that);
			}
		};
	};

	public static <T> SetFunction<T> all() {
		return new SetFunction<T>() {

			@Override
			public boolean contains(T obj) {
				return true;
			}

			@Override
			protected Set<T> getKnownMembers() {
				return Collections.emptySet();
			}

			@Override
			public boolean knowsAllMembers() {
				return false;
			}

			@Override
			public String toString() {
				return "{ all }";
			}
		};
	}

	public static <T> SetFunction<T> singleton(final T value) {
		return new SetFunction<T>() {

			@Override
			public boolean contains(T obj) {
				return Utils.equals(obj, value);
			}

			@Override
			protected Set<T> getKnownMembers() {
				return Collections.singleton(value);
			}

			@Override
			public String toString() {
				return String.format("{ %s }", value);
			}
		};
	}

	public static <T extends Comparable<T>> SetFunction<T> range(final T min,
			final T max) {
		Utils.check(min.compareTo(max) <= 0);

		return new SetFunction<T>() {

			@Override
			public boolean contains(T obj) {
				return min.compareTo(obj) <= 0 && max.compareTo(obj) >= 0;
			}

			@Override
			protected Set<T> getKnownMembers() {
				Set<T> knownMembers = new HashSet<T>();
				knownMembers.add(min);
				knownMembers.add(max);

				return Collections.unmodifiableSet(knownMembers);
			}

			@Override
			public boolean knowsAllMembers() {
				return false;
			}

			@Override
			public String toString() {
				return String.format("{ %s .. %s }", min, max);
			}
		};
	}

	public static <T> SetFunction<T> empty() {
		return new SetFunction<T>() {

			@Override
			public boolean contains(T obj) {
				return false;
			}

			@Override
			protected Set<T> getKnownMembers() {
				return Collections.emptySet();
			}

			@Override
			public String toString() {
				return "{ }";
			}
		};
	}

}
