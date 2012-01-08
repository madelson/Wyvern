/**
 * 
 */
package compiler;

import java.util.*;

/**
 * @author Michael
 * 
 */
public class Utils {
	public static final String NL = System.getProperty("line.separator");

	public static boolean isNullOrEmpty(String s) {
		return s == null || s.length() == 0;
	}

	public static <T> T coalesce(T obj, T... others) {
		T retVal = obj;

		for (int i = 0; retVal == null && i < others.length; i++) {
			retVal = others[i];
		}

		return retVal;
	}

	/**
	 * A simple implementation of split where the length of the returned array
	 * is always 1 + the number of unique instances of the separator string.
	 */
	public static String[] split(String text, String seperator) {
		Utils.check(!seperator.isEmpty(),
				"Splitting on the empty string is undefined!");

		List<String> parts = new ArrayList<String>();

		int startSearchPos = 0, pos;
		while ((pos = text.indexOf(seperator, startSearchPos)) >= 0) {
			parts.add(text.substring(startSearchPos, pos));
			startSearchPos = pos + seperator.length();
		}
		parts.add(text.substring(startSearchPos, text.length()));
		
		return parts.toArray(new String[parts.size()]);
	}

	/**
	 * Type-safe cast which returns null for null or wrongly typed items.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T cast(Object obj, Class<T> cls) {
		return (obj != null && cls.isInstance(obj)) ? (T) obj : null;
	}

	/**
	 * Creates an immutable copy of the list.
	 */
	public static <T> List<T> immutableCopy(List<T> list) {
		return Collections.unmodifiableList(new ArrayList<T>(list));
	}

	/**
	 * Creates an immutable copy of the set, preserving iteration ordering.
	 */
	public static <T> Set<T> immutableCopy(Set<T> set) {
		return Collections.unmodifiableSet(new LinkedHashSet<T>(set));
	}

	/**
	 * Creates an immutable copy of the map, preserving iteration ordering.
	 */
	public static <K, V> Map<K, V> immutableCopy(Map<K, V> map) {
		return Collections.unmodifiableMap(new LinkedHashMap<K, V>(map));
	}

	/**
	 * Adds the iterable to the collection, returning the collection
	 */
	public static <T, C extends Collection<T>> C addAll(C collection,
			Iterable<T> iterable) {
		for (T t : iterable)
			collection.add(t);

		return collection;
	}

	/**
	 * Creates a list from an iterable
	 */
	public static <T> List<T> toList(Iterable<T> iterable) {
		List<T> list = new ArrayList<T>();

		return addAll(list, iterable);
	}

	/**
	 * Returns the contents of the iterator as a list
	 */
	public static <T> List<T> toList(Iterator<T> iterator) {
		List<T> list = new ArrayList<T>();
		while (iterator.hasNext())
			list.add(iterator.next());

		return list;
	}

	/**
	 * Convenience constructor for a set. Maintains iteration order.
	 */
	public static <T> LinkedHashSet<T> set(T... items) {
		return new LinkedHashSet<T>(Arrays.asList(items));
	}

	/**
	 * Returns the last item in the list
	 */
	public static <T> T last(List<T> list) {
		return list.get(list.size() - 1);
	}

	/**
	 * Do the two collections intersect?
	 */
	public static boolean intersects(Collection<?> collection1,
			Collection<?> collection2) {
		if (collection2.size() < collection1.size()) {
			return intersects(collection2, collection1);
		}
		for (Object obj : collection1) {
			if (collection2.contains(obj)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * A convenience method for consistent error handling
	 */
	public static RuntimeException err(String msg) {
		throw new RuntimeException(msg);
	}

	/**
	 * A convenience method for consistent error handling
	 */
	public static RuntimeException err(Throwable error) {
		throw new RuntimeException(error);
	}

	/**
	 * A convenience method for assertions
	 */
	public static void check(boolean condition, String msg) {
		if (!condition)
			Utils.err(msg);
	}

	/**
	 * A convenience method for assertions
	 */
	public static void check(boolean condition) {
		check(condition, "Check failed!");
	}

	/**
	 * As the regular equals method, but handles nulls properly
	 */
	public static boolean equals(Object a, Object b) {
		return a == null ? b == null : a.equals(b);
	}

	/**
	 * As the regular hashCode method, but handles nulls properly
	 */
	public static int hashCode(Object obj) {
		return obj == null ? -1 : obj.hashCode();
	}

	public static <T> T newInstance(Class<T> cls) {
		try {
			return cls.newInstance();
		} catch (Exception ex) {
			throw Utils.err(ex);
		}
	}

	@SuppressWarnings("unchecked")
	public static <K, V, C extends Collection<V>> boolean put(Map<K, C> map,
			Class<?> innerCollectionClass, K key, V value) {
		if (!map.containsKey(key))
			map.put(key, (C) newInstance(innerCollectionClass));

		return map.get(key).add(value);
	}

	@SuppressWarnings("unchecked")
	public static <K1, K2, V, M extends Map<K2, V>> V put(Map<K1, M> map,
			Class<?> innerMapClass, K1 key1, K2 key2, V value) {
		M innerMap = map.get(key1);
		if (innerMap == null) {
			innerMap = (M) newInstance(innerMapClass);
			map.put(key1, innerMap);
		}

		return innerMap.put(key2, value);
	}

	public static <K, V> V getOrDefault(Map<K, V> map, K key, V defaultValue) {
		return map.containsKey(key) ? map.get(key) : defaultValue;
	}

	public static <T> T deepImmutableCopy(T objects) {
		return deepImmutableCopy(objects, true);
	}

	@SuppressWarnings("unchecked")
	private static <T> T deepImmutableCopy(T obj, boolean checked) {
		if (obj instanceof Map)
			return (T) deepImmutableMapCopy((Map<?, ?>) obj);
		if (obj instanceof Collection)
			return (T) deepImmutableCollectionCopy((Collection<?>) obj);
		else if (checked)
			throw Utils.err("Argument must be a collection or a map");
		return obj;
	}

	private static <K, V> Map<K, V> deepImmutableMapCopy(Map<K, V> map) {
		Map<K, V> copy = new LinkedHashMap<K, V>();

		for (Map.Entry<K, V> e : map.entrySet())
			copy.put(deepImmutableCopy(e.getKey(), false),
					deepImmutableCopy(e.getValue(), false));

		return Collections.unmodifiableMap(copy);
	}

	@SuppressWarnings("unchecked")
	private static <T, C extends Collection<T>> C deepImmutableCollectionCopy(
			C collection) {
		C copy, immutableCopy;
		if (collection instanceof Set) {
			copy = (C) new LinkedHashSet<T>();
			immutableCopy = (C) Collections.unmodifiableSet((Set<T>) copy);
		} else if (collection instanceof List) {
			copy = (C) new ArrayList<T>();
			immutableCopy = (C) Collections.unmodifiableList((List<T>) copy);
		} else {
			copy = (C) newInstance(collection.getClass());
			immutableCopy = (C) Collections.unmodifiableCollection(copy);
		}

		for (T item : collection)
			copy.add(deepImmutableCopy(item, false));

		return immutableCopy;
	}

	public static boolean symbolsAreEquivalent(Symbol a, Symbol b) {
		if (!a.type().equals(b.type()))
			return false;
		if (a.type().isTerminal())
			return true;
		if (a.children().size() != b.children().size())
			return false;
		for (int i = 0; i < a.children().size(); i++)
			if (!symbolsAreEquivalent(a.children().get(i), b.children().get(i)))
				return false;
		return true;
	}
}
