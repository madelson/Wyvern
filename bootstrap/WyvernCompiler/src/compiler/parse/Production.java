/**
 * 
 */
package compiler.parse;

import java.util.*;

import compiler.*;
import compiler.Context.ListOption;

/**
 * Represents a CFG production
 * 
 * @author madelson
 */
public class Production {
	private final SymbolType symbolType;
	private final List<SymbolType> childTypes;
	private int hash = Integer.MAX_VALUE;

	public Production(SymbolType symbolType, SymbolType... childTypes) {
		Utils.check(!symbolType.isTerminal(),
				"The left-hand side of a production cannot be terminal!");

		this.symbolType = symbolType;
		this.childTypes = Utils.immutableCopy(Arrays.asList(childTypes));
	}

	/**
	 * The result of the production
	 */
	public SymbolType symbolType() {
		return this.symbolType;
	}

	/**
	 * The arguments used to produce the result
	 */
	public List<SymbolType> childTypes() {
		return this.childTypes;
	}

	/**
	 * The leftmost terminal symbol type, or null if the production contains no
	 * terminal symbol types
	 */
	public SymbolType leftmostTerminalSymbolType() {
		for (SymbolType type : this.childTypes())
			if (type.isTerminal())
				return type;
		return null;
	}

	/**
	 * The rightmost terminal symbol type, or null if the production contains no
	 * terminal symbol types
	 */
	public SymbolType rightmostTerminalSymbolType() {
		for (int i = this.childTypes().size() - 1; i >= 0; i--)
			if (this.childTypes().get(i).isTerminal())
				return this.childTypes().get(i);
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		// MA: not using Utils.cast() here for efficiency, since productions are
		// often part of inner loop hash comparisons
		Production that = obj instanceof Production ? (Production) obj : null;
		return that != null && this.symbolType().equals(that.symbolType())
				&& this.childTypes().equals(that.childTypes());
	}

	@Override
	public int hashCode() {
		if (this.hash == Integer.MAX_VALUE)
			this.hash = this.symbolType().hashCode()
					^ this.childTypes().hashCode();

		return this.hash;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(this.symbolType().name()).append(" ->");
		for (SymbolType componentType : this.childTypes())
			sb.append(' ').append(componentType.name());
		if (this.childTypes().isEmpty())
			sb.append(" nothing");

		return sb.toString();
	}

	/**
	 * Retrieves the set of all types mentioned in a set of productions
	 */
	public static Set<SymbolType> allSymbolTypes(Iterable<Production> grammar) {
		Set<SymbolType> types = new LinkedHashSet<SymbolType>();
		for (Production production : grammar) {
			types.add(production.symbolType());
			for (SymbolType type : production.childTypes())
				types.add(type);
		}

		return types;
	}

	/**
	 * Retrieves the set of non-terminal symbol types mentioned in a set of
	 * productions
	 */
	public static Set<SymbolType> allNonTerminalSymbolTypes(
			Iterable<Production> grammar) {
		Set<SymbolType> types = allSymbolTypes(grammar);
		types.removeAll(allTerminalSymbolTypes(grammar));

		return types;
	}

	/**
	 * Retrieves the set of terminal symbol types mentioned in a set of
	 * productions
	 */
	public static Set<SymbolType> allTerminalSymbolTypes(
			Iterable<Production> grammar) {
		Set<SymbolType> types = new LinkedHashSet<SymbolType>();

		for (SymbolType type : allSymbolTypes(grammar))
			if (type.isTerminal())
				types.add(type);

		return types;
	}

	public static List<Production> makeOption(SymbolType symbolType) {
		List<Production> optionProductions = new ArrayList<Production>();
		optionProductions.add(new Production(symbolType.context().optional(
				symbolType), symbolType));
		optionProductions.add(new Production(symbolType.context().optional(
				symbolType)));

		return optionProductions;
	}

	/**
	 * TODO: support automatically adding common error types for unexpected
	 * trailing/leading separator or non-empty
	 */
	public static List<Production> makeList(SymbolType listType,
			SymbolType elementType, SymbolType separatorType,
			ListOption... listOptionsArray) {
		List<Production> listProductions = new ArrayList<Production>();
		List<ListOption> listOptions = Arrays.asList(listOptionsArray);

		if (listOptions.contains(ListOption.AllowTrailingSeparator)) {
			Utils.check(separatorType != null);

			if (listOptions.contains(ListOption.AllowEmpty)) {
				listProductions.add(new Production(listType, elementType,
						separatorType, listType));
				listProductions.add(new Production(listType, elementType));
				listProductions.add(new Production(listType));
			} else {
				listProductions.add(new Production(listType, elementType,
						separatorType, listType));
				listProductions.add(new Production(listType, elementType,
						separatorType));
				listProductions.add(new Production(listType, elementType));
			}
		} else {
			if (separatorType != null) {
				if (listOptions.contains(ListOption.AllowEmpty)) {
					// This is a tough case. You can't put any types before or
					// after listType, since it may be empty and
					// cannot have a trailing separator. Thus, we need to make
					// use of another symbol

					// make lists of generated type that can't be empty or have
					// trailing separators
					// TODO: support auto-generated types to solve cases like
					// this
					SymbolType generatedListType = listType.context()
							.getNonTerminalSymbolType(
									"__makeList:" + listType.name());
					listProductions.addAll(makeList(generatedListType,
							elementType, separatorType));

					// then allow a list to be a generatedListType or empty
					listProductions.add(new Production(listType,
							generatedListType));
					listProductions.add(new Production(listType));
				} else {
					listProductions.add(new Production(listType, elementType,
							separatorType, listType));
					listProductions.add(new Production(listType, elementType));
				}
			} else {
				if (listOptions.contains(ListOption.AllowEmpty)) {
					listProductions.add(new Production(listType, elementType,
							listType));
					listProductions.add(new Production(listType));
				} else {
					listProductions.add(new Production(listType, elementType,
							listType));
					listProductions.add(new Production(listType, elementType));
				}
			}
		}

		return listProductions;
	}

	public static Set<Production> makeOneOf(SymbolType... symbolTypes) {
		Utils.check(symbolTypes != null && symbolTypes.length > 0);

		Set<Production> orProductions = Utils.set();

		SymbolType or = symbolTypes[0].context().oneOf(symbolTypes);
		for (SymbolType symbolType : symbolTypes) {
			orProductions.add(new Production(or, symbolType));
		}

		return orProductions;
	}

	public static Set<Production> makeTuple(SymbolType... symbolTypes) {
		Utils.check(symbolTypes != null && symbolTypes.length > 0);

		SymbolType tuple = symbolTypes[0].context().tuple(symbolTypes);
		return Collections.singleton(new Production(tuple, symbolTypes));
	}
}
