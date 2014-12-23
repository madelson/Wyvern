/**
 * 
 */
package compiler.parse;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compiler.SymbolType;
import compiler.Utils;

/**
 * @author mikea_000
 * 
 */
final class RecursiveDescentAnalyzer {
	public enum Rule {
		None, AllowGreaterPrecedence, AllowGreaterOrEqualPrecedence,
	}

	private final Grammar grammar;
	private final Comparator<Production> precedence;
	private final Map<Production, Rule[]> ruleMap;

	public RecursiveDescentAnalyzer(Grammar grammar) {
		Utils.check(grammar != null, "grammar");

		this.grammar = grammar;
		this.precedence = this.createPrecedence();
		this.ruleMap = this.buildRuleMap();
	}

	private Comparator<Production> createPrecedence() {
		// build the precedence comparer. X -> a has precedence over X -> b iff
		// X -> a comes first in the grammar:
		List<Production> productionsList = Utils.toList(this.grammar.productions());
		final Map<Production, Integer> productionIndices = new HashMap<Production, Integer>();
		for (int i = 0; i < productionsList.size(); ++i) {
			productionIndices.put(productionsList.get(i), i);
		}

		return new Comparator<Production>() {

			@Override
			public int compare(Production a, Production b) {
				if (a == b) {
					return 0; // fast path
				}

				return -productionIndices.get(a).compareTo(productionIndices.get(b));
			}

		};
	}

	private Map<Production, Rule[]> buildRuleMap() {
		Map<Production, Rule[]> ruleMap = new HashMap<Production, Rule[]>();
		Map<SymbolType, Set<Production>> initialProductionsCache = new HashMap<SymbolType, Set<Production>>();
		for (Production production : this.grammar.productions()) {
			Rule[] positionRules = new Rule[production.childTypes().size()];

			// renames are ignored
			if (isRename(production)) {
				Arrays.fill(positionRules, Rule.None);
			} else {
				int[] leadingTrailingInfo = this.findLeadingAndTrailingPositions(production);

				for (int i = 0; i < positionRules.length; ++i) {
					if (leadingTrailingInfo[i] != 0) {
						SymbolType childType = production.childTypes().get(i);
						Set<Production> cachedInitialProductions = initialProductionsCache.get(childType), initialProductions;
						if (cachedInitialProductions != null) {
							initialProductions = cachedInitialProductions;
						} else {
							initialProductionsCache.put(childType,
									initialProductions = this.getAllInitialProductions(childType));
						}

						for (Production initialProduction : initialProductions) {
							if (!isRename(initialProduction)) {
								int cmp = this.precedence.compare(initialProduction, production);
								if ((leadingTrailingInfo[i] & LEADING_FLAG) != 0) {
									if (cmp <= 0) {
										positionRules[i] = Rule.AllowGreaterPrecedence;
										break;
									}
								} else {
									if (cmp < 0) {
										positionRules[i] = Rule.AllowGreaterOrEqualPrecedence;
										break;
									}
								}
							}
						}
					}

					if (positionRules[i] == null) {
						positionRules[i] = Rule.None;
					}
				}
			}

			ruleMap.put(production, positionRules);
		}

		return ruleMap;

		// Map<Production, Rule[]> ruleMap = new HashMap<Production, Rule[]>();
		// for (Production production : this.grammar.productions()) {
		// Rule[] positionVulnerabilities = new
		// Rule[production.childTypes().size()];
		// for (int i = 0; i < positionVulnerabilities.length; ++i) {
		// positionVulnerabilities[i] = this.getVulnerability(production, i);
		// }
		//
		// ruleMap.put(production, positionVulnerabilities);
		// }
		//
		// return ruleMap;
	}

	private static final int LEADING_FLAG = 1 << 0, TRAILING_FLAG = 1 << 1;

	/**
	 * Find the positions where symbols in a production could be the first or
	 * last symbol, using nullability
	 */
	private int[] findLeadingAndTrailingPositions(Production production) {
		int[] result = new int[production.childTypes().size()];

		// find leading positions
		for (int i = 0; i < result.length; ++i) {
			result[i] |= LEADING_FLAG;
			if (!this.grammar.nff().nullableSet().contains(production.childTypes().get(i))) {
				break; // stop at first non-nullable
			}
		}

		// find trailing positions
		for (int i = result.length - 1; i >= 0; --i) {
			result[i] |= TRAILING_FLAG;
			if (!this.grammar.nff().nullableSet().contains(production.childTypes().get(i))) {
				break; // stop at first non-nullable
			}
		}

		return result;
	}

	private static boolean isRename(Production production) {
		boolean isRenameProduction = production.childTypes().size() == 1
				&& !production.childTypes().get(0).isTerminal();
		return isRenameProduction;
	}

	private Set<Production> getAllInitialProductions(SymbolType symbolType) {
		Set<Production> result = new HashSet<Production>();
		this.gatherInitialProductions(symbolType, result);
		return result;
	}

	private void gatherInitialProductions(SymbolType symbolType, Set<Production> result) {
		if (symbolType.isTerminal()) {
			return;
		}

		for (Production production : this.grammar.productions(symbolType)) {
			// add the current production
			if (!result.add(production)) {
				return; // already saw this symbolType
			}

			for (SymbolType childType : production.childTypes()) {
				this.gatherInitialProductions(childType, result);
				if (!this.grammar.nff().nullableSet().contains(childType)) {
					break; // once we reach a nullable, cut off the recursion
				}
			}
		}
	}

	public Rule getRule(Production production, int position) {
		Rule[] positionVunerabilities = this.ruleMap.get(production);
		return positionVunerabilities[position];
	}

	public boolean allow(Production current, Rule rule, Production other) {
		if (isRename(current)) {
			return true;
		}

		int cmp = this.precedence.compare(current, other);
		switch (rule) {
		case AllowGreaterPrecedence:
			return cmp > 0;
		case AllowGreaterOrEqualPrecedence:
			return cmp >= 0;
		default:
			throw Utils.err("bad rule");
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Rules:").append(Utils.NL);
		for (Production production : this.grammar.productions()) {
			sb.append('\t').append(production.symbolType().name()).append(" -> ");
			for (int i = 0; i < this.ruleMap.get(production).length; ++i) {
				String format;
				switch (this.ruleMap.get(production)[i]) {
				case None:
					format = "%s";
					break;
				case AllowGreaterPrecedence:
					format = "[%s]";
					break;
				case AllowGreaterOrEqualPrecedence:
					format = "{%s}";
					break;
				default:
					throw Utils.err("bad rule");
				}
				sb.append(i > 0 ? ", " : "").append(String.format(format, production.childTypes().get(i).name()));
			}
			sb.append(Utils.NL);
		}

		return sb.toString();
	}
}
