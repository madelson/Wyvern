/**
 * 
 */
package compiler.automata;

import java.util.Collection;
import java.util.Set;

/**
 * @author Michael
 * 
 */
public interface SetOperations<T> {
	/**
	 * Converts the given set collection to a set of non-overlapping sets whose
	 * union is the same as the union of the collection sets. Furthermore, for
	 * each (given set, returned set) pair, if the given set contains any
	 * elements from the returned set, then it contains all elements from the
	 * returned set.
	 */
	public Set<Collection<T>> partitionedUnion(Collection<Collection<T>> sets);
}
