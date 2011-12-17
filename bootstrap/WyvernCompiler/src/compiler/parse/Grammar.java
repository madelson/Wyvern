/**
 * 
 */
package compiler.parse;

import java.util.*;

import compiler.*;

/**
 * Represents a fully-specified grammar
 * 
 * @author madelson
 */
public class Grammar {
	private final Context context;
	private final String name;
	private final Set<Production> productions;
	private final SymbolType startSymbolType;
	private final Map<SymbolType, Set<Production>> symbolsToProductions;
	private final NullableFirstFollow nff;
	private final Set<SymbolType> symbolTypes, terminalSymbolTypes,
			nonTerminalSymbolTypes;
	private final PrecedenceFunction precedence;

	public Grammar(Context context, String name, SymbolType startSymbol,
			Iterable<Production> productions,
			PrecedenceFunction precedence) {
		Utils.check(startSymbol != null, "Null start symbol!");

		this.context = context;
		this.name = name;
		this.startSymbolType = startSymbol;

		Set<Production> productionSet = new LinkedHashSet<Production>();
		Utils.addAll(productionSet, productions).add(
				new Production(this.context.startType(), this.startSymbolType,
						this.context.eofType()));
		this.productions = Collections.unmodifiableSet(productionSet);

		Map<SymbolType, Set<Production>> symbolsToProductions = new HashMap<SymbolType, Set<Production>>();
		for (Production production : this.productions) {
			Utils.put(symbolsToProductions, LinkedHashSet.class,
					production.symbolType(), production);
		}
		this.symbolsToProductions = Utils
				.deepImmutableCopy(symbolsToProductions);

		this.nff = new NullableFirstFollow(this.productions);

		this.symbolTypes = Production.allSymbolTypes(this.productions);
		this.terminalSymbolTypes = Production
				.allTerminalSymbolTypes(this.productions);
		this.nonTerminalSymbolTypes = Production
				.allNonTerminalSymbolTypes(this.productions);

		this.precedence = precedence;
	}

	/**
	 * A name for the grammar
	 */
	public String name() {
		return this.name;
	};

	/**
	 * The start symbol for the grammar
	 */
	public SymbolType startSymbolType() {
		return this.startSymbolType;
	}

	/**
	 * The set of productions in the grammar
	 */
	public Set<Production> productions() {
		return this.productions;
	}

	/**
	 * The context of the grammar
	 */
	public Context context() {
		return this.context;
	}

	/**
	 * The set of productions for the given symbol type
	 */
	public Set<Production> productions(SymbolType symbolType) {
		Set<Production> productions = this.symbolsToProductions.get(symbolType);

		return productions != null ? productions : Collections
				.<Production> emptySet();
	}

	/**
	 * The nullable, first, and follow computations
	 */
	public NullableFirstFollow nff() {
		return this.nff;
	}

	/**
	 * The set of element types mentioned in all productions
	 */
	public Set<SymbolType> symbolTypes() {
		return this.symbolTypes;
	}

	/**
	 * The set of token types mentioned in all productions
	 */
	public Set<SymbolType> terminalSymbolTypes() {
		return this.terminalSymbolTypes;
	}

	/**
	 * The set of symbol types mentioned in all productions
	 */
	public Set<SymbolType> nonTerminalSymbolTypes() {
		return this.nonTerminalSymbolTypes;
	}

	/**
	 * An object that defines precedence and associativity for symbols
	 */
	public PrecedenceFunction precedence() {
		return this.precedence;
	}

	/**
	 * Returns a yacc-like string representation of the current grammar
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("Grammar ").append(this.name()).append(Utils.NL);
		for (SymbolType symbolType : this.nonTerminalSymbolTypes()) {
			sb.append(symbolType.name()).append(':').append(Utils.NL);
			boolean first = true;
			for (Production production : this.productions(symbolType)) {
				sb.append('\t');
				if (first)
					first = false;
				else
					sb.append('|');

				for (SymbolType type : production.childTypes())
					sb.append(' ').append(type.name());
				sb.append(Utils.NL);
			}
		}

		return sb.toString();
	}
}
