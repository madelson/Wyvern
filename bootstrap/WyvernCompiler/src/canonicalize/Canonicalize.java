/**
 * Copy
 */
package canonicalize;

import java.util.*;

import compiler.*;

/**
 * @author Michael
 * 
 */
public class Canonicalize {
	/**
	 * Converts linked-list structures to normal lists, optionally removing
	 * separators.
	 */
	public static Symbol flattenLists(Symbol parseTree,
			Map<SymbolType, SymbolType> listTypesAndSeparators,
			boolean removeSeparators) {
		if (listTypesAndSeparators.containsKey(parseTree.type())) {
			// flatten
			return parseTree.type().createSymbol(
					collectListChildren(parseTree, new ArrayList<Symbol>(),
							listTypesAndSeparators, removeSeparators));
		} else if (parseTree.type().isTerminal()
				|| parseTree.children().isEmpty()) {
			// base case
			return parseTree;
		}

		// recurse on all children
		List<Symbol> flattenedChildren = new ArrayList<Symbol>(parseTree
				.children().size());
		for (Symbol child : parseTree.children()) {
			flattenedChildren.add(flattenLists(child, listTypesAndSeparators,
					removeSeparators));
		}

		return parseTree.type().createSymbol(flattenedChildren);
	}

	private static List<Symbol> collectListChildren(Symbol listSymbol,
			List<Symbol> collectedChildren,
			Map<SymbolType, SymbolType> listTypesAndSeparators,
			boolean removeSeparators) {
		switch (listSymbol.children().size()) {
		case 0: // empty base case
			return collectedChildren;
		case 1: // single item list base case
			collectedChildren.add(flattenLists(listSymbol.children().get(0),
					listTypesAndSeparators, removeSeparators));
			return collectedChildren;
		case 2: // item and next node, no separator
			collectedChildren.add(flattenLists(listSymbol.children().get(0),
					listTypesAndSeparators, removeSeparators));
			break;
		case 3: // item, separator, and next node
			// sanity check
			SymbolType separatorType = listTypesAndSeparators.get(listSymbol
					.type());
			Utils.check(separatorType != null
					&& listSymbol.children().get(1).type()
							.equals(separatorType));

			collectedChildren.add(flattenLists(listSymbol.children().get(0),
					listTypesAndSeparators, removeSeparators));
			if (!removeSeparators) {
				collectedChildren.add(flattenLists(
						listSymbol.children().get(1), listTypesAndSeparators,
						removeSeparators));
			}
			break;
		default:
			throw Utils.err("Invalid child count for list node!");
		}

		Symbol nextListSymbol = Utils.last(listSymbol.children());

		// sanity check
		Utils.check(nextListSymbol.type().equals(listSymbol.type()));

		collectListChildren(nextListSymbol, collectedChildren,
				listTypesAndSeparators, removeSeparators);
		return collectedChildren;
	}

	private Canonicalize() {
	}
}
