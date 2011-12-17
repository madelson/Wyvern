/**
 * 
 */
package compiler.parse;

import java.util.*;

import compiler.*;

/**
 * @author Michael
 * 
 */
public class Precedence {
	public static enum ProductionPrecedence {
		None, LeftmostTerminal;

		public SymbolType precedenceSymbolFor(Production production) {
			switch (this) {
			case LeftmostTerminal:
				return production.leftmostTerminalSymbolType();
			default:
				return null;
			}
		}
	}

	public static PrecedenceFunction createFunction(
			LinkedHashMap<Set<SymbolType>, Associativity> symbolTypePrecedences,
			final ProductionPrecedence productionPrecedence,
			Map<Production, SymbolType> precedenceAssignments) {
		final Map<SymbolType, Integer> precedenceMap = new HashMap<SymbolType, Integer>();
		final Map<SymbolType, Associativity> associativityMap = new HashMap<SymbolType, Associativity>();
		int precedence = symbolTypePrecedences.size();
		for (Map.Entry<Set<SymbolType>, Associativity> e : symbolTypePrecedences
				.entrySet()) {
			for (SymbolType symbolType : e.getKey()) {
				Utils.check(precedenceMap.put(symbolType, precedence) == null);
				associativityMap.put(symbolType, e.getValue());
			}
			precedence--;
		}

		final Map<Production, SymbolType> precedenceAssignmentsCopy = Utils
				.immutableCopy(precedenceAssignments);

		return new PrecedenceFunction() {

			@Override
			public SymbolType precedenceSymbolFor(Production production) {
				return precedenceAssignmentsCopy.containsKey(production) ? precedenceAssignmentsCopy
						.get(production) : productionPrecedence
						.precedenceSymbolFor(production);
			}

			@Override
			public Integer precedenceOf(SymbolType symbolType) {
				return precedenceMap.get(symbolType);
			}

			@Override
			public Associativity associativityOf(SymbolType symbolType) {
				Associativity associativity = associativityMap.get(symbolType);

				return associativity != null ? associativity
						: Associativity.NonAssociative;
			}
		};
	}
	
	public static PrecedenceFunction defaultFunction() {
		return new PrecedenceFunction() {
			@Override
			public SymbolType precedenceSymbolFor(Production production) {
				return null;
			}

			@Override
			public Integer precedenceOf(SymbolType symbolType) {
				return 0;
			}

			@Override
			public Associativity associativityOf(SymbolType symbolType) {
				return Associativity.NonAssociative;
			}			
		};
	}
	
	private Precedence() {
	}
}
