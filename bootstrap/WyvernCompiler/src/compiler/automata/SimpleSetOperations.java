/**
 * 
 */
package compiler.automata;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compiler.Utils;

/**
 * @author Michael
 * 
 */
public class SimpleSetOperations<T> implements SetOperations<T> {

	@Override
	public Set<Collection<T>> partitionedUnion(Collection<Collection<T>> sets) {
		Set<T> allValues = new HashSet<T>();
		for (Collection<T> set : sets) {
			allValues.addAll(set);
		}

		return new HashSet<Collection<T>>(groupByContains(allValues,
				new ArrayList<Collection<T>>(sets)));
	}

	/**
	 * Groups the elements in values by the set of collections in sets that
	 * contain them.
	 */
	public static <T> Collection<Set<T>> groupByContains(Set<T> values,
			List<Collection<T>> sets) {
		Map<BitSet, Set<T>> groups = new HashMap<BitSet, Set<T>>();
		for (T value : values) {
			BitSet containsSet = new BitSet(sets.size());
			for (int i = 0; i < sets.size(); i++) {
				if (sets.get(i).contains(value)) {
					containsSet.set(i);
				}
			}
			Utils.put(groups, HashSet.class, containsSet, value);
		}

		return groups.values();
	}
}
