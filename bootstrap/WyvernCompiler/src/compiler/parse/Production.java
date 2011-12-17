/**
 * 
 */
package compiler.parse;

import java.util.*;

import compiler.*;

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
	 * The leftmost terminal symbol type, or null if the production
	 * contains no terminal symbol types
	 */
	public SymbolType leftmostTerminalSymbolType() {
		for (SymbolType type : this.childTypes())
			if (type.isTerminal())
				return type;
		return null;
	}
	
	/**
	 * The rightmost terminal symbol type, or null if the production
	 * contains no terminal symbol types
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

		Production that = Utils.cast(obj, Production.class);
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
}
