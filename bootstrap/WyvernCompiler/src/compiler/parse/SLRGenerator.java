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
public class SLRGenerator extends LR0Generator {
	@Override
	protected Set<Reduction> reductions(Grammar grammar, State state) {
		Set<Reduction> reductions = new LinkedHashSet<Reduction>();

		for (Item item : state.items())
			if (!item.hasNextSymbolType())
				for (SymbolType followToken : grammar.nff().followSets()
						.get(item.production().symbolType()))
					reductions.add(new Reduction(state, followToken,
							item.production()));

		return Collections.unmodifiableSet(reductions);
	}
}
