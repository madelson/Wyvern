/**
 * 
 */
package compiler.parse;

import java.util.*;

import compiler.*;

/**
 * The results of computing the nullable, first, and follow sets of a set of
 * productions
 * 
 * @author madelson
 */
public class NullableFirstFollow {
	private final Set<SymbolType> nullableSet;
	private final Map<SymbolType, Set<SymbolType>> firstSets;
	private final Map<SymbolType, Set<SymbolType>> followSets;

	public NullableFirstFollow(Iterable<Production> grammar) {
		// compute nullables
		Set<SymbolType> nullableSet = new HashSet<SymbolType>();
		boolean changed;
		do {
			changed = false;
			for (Production production : grammar)
				if (nullableSet.containsAll(production.childTypes()))
					changed |= nullableSet.add(production.symbolType());
		} while (changed);

		// compute first sets
		Map<SymbolType, Set<SymbolType>> firstSets = new HashMap<SymbolType, Set<SymbolType>>();
		for (SymbolType tokenType : Production.allTerminalSymbolTypes(grammar))
			firstSets.put(tokenType, Collections.singleton(tokenType));
		for (SymbolType symbolType : Production.allNonTerminalSymbolTypes(grammar))
			firstSets.put(symbolType, new LinkedHashSet<SymbolType>());
		do {
			changed = false;
			for (Production production : grammar)
				for (SymbolType type : production.childTypes()) {
					changed |= firstSets.get(production.symbolType()).addAll(
							firstSets.get(type));
					if (!nullableSet.contains(type))
						break;
				}
		} while (changed);

		// compute follow sets
		Map<SymbolType, Set<SymbolType>> followSets = new HashMap<SymbolType, Set<SymbolType>>();
		for (SymbolType type : Production.allSymbolTypes(grammar))
			followSets.put(type, new HashSet<SymbolType>());
		do {
			changed = false;
			for (Production production : grammar) {
				boolean tailStillNullable = true;
				for (int i = production.childTypes().size() - 1; i >= 0; i--) {
					// if y_i+1 to end are nullable, follow(y_i) U=
					// follow(x)
					if (tailStillNullable) {
						changed |= followSets.get(
								production.childTypes().get(i)).addAll(
								followSets.get(production.symbolType()));
						tailStillNullable = nullableSet.contains(production
								.childTypes().get(i));
					}

					for (SymbolType follower : production.childTypes()
							.subList(i + 1, production.childTypes().size())) {
						changed |= followSets.get(
								production.childTypes().get(i)).addAll(
								firstSets.get(follower));
						if (!nullableSet.contains(follower))
							break;
					}
				}
			}
		} while (changed);

		// make immutable
		this.nullableSet = Collections.unmodifiableSet(nullableSet);
		this.firstSets = Utils.deepImmutableCopy(firstSets);
		this.followSets = Utils.deepImmutableCopy(followSets);
	}

	/**
	 * The set of nullable symbols
	 */
	public Set<SymbolType> nullableSet() {
		return this.nullableSet;
	}

	/**
	 * The set of first tokens for each symbol
	 */
	public Map<SymbolType, Set<SymbolType>> firstSets() {
		return this.firstSets;
	}

	/**
	 * The set of follow tokens for each symbol
	 */
	public Map<SymbolType, Set<SymbolType>> followSets() {
		return this.followSets;
	}

	/**
	 * Returns the first set for a sequence of elements
	 */
	public Set<SymbolType> first(List<SymbolType> types) {
		Set<SymbolType> firstSet = new LinkedHashSet<SymbolType>(types.size());
		for (SymbolType type : types) {
			firstSet.addAll(this.firstSets().get(type));
			if (!this.nullableSet().contains(type))
				break;
		}

		return Collections.unmodifiableSet(firstSet);
	}
}
