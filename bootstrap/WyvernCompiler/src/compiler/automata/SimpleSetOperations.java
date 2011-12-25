/**
 * 
 */
package compiler.automata;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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

		Set<Collection<T>> singletons = new HashSet<Collection<T>>();
		for (T value : allValues) {
			singletons.add(Collections.singleton(value));
		}

		return Utils.immutableCopy(singletons);
	}
}
