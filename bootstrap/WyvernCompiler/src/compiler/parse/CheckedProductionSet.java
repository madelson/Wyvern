/**
 * 
 */
package compiler.parse;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import compiler.Context;
import compiler.SymbolType;
import compiler.Utils;

/**
 * @author Michael
 * 
 */
@SuppressWarnings("serial")
public class CheckedProductionSet extends LinkedHashSet<Production> {
	private final Set<SymbolType> definedSymbols = new HashSet<SymbolType>();

	@Override
	public boolean add(Production production) {
		boolean isRecursiveDefinition = false;

		for (SymbolType childType : production.childTypes()) {
			// if we come across an undefined non-terminal
			if (!childType.isTerminal()
					&& !this.definedSymbols.contains(childType)) {
				SymbolType optionComponentType = childType.context()
						.getOptionComponentType(childType);
				Set<SymbolType> oneOfComponentTypes = childType.context()
						.getOneOfComponentTypes(childType);
				List<SymbolType> tupleComponentTypes = childType.context()
						.getTupleComponentTypes(childType);
				SymbolType listElementType = childType.context()
						.getListElementType(childType);

				// if it's a recursive definition, allow it but don't count the
				// type as defined
				if (childType.equals(production.symbolType())) {
					isRecursiveDefinition = true;
				}
				// if it's an option we can just define it
				else if (optionComponentType != null) {
					this.addAll(Production.makeOption(optionComponentType));
				}
				// if it's an OR we can just define it
				else if (oneOfComponentTypes != null) {
					this.addAll(Production.makeOneOf(oneOfComponentTypes
							.toArray(new SymbolType[oneOfComponentTypes.size()])));
				}
				// if it's a tuple we can just define it
				else if (tupleComponentTypes != null) {
					this.addAll(Production.makeTuple(tupleComponentTypes
							.toArray(new SymbolType[tupleComponentTypes.size()])));
				}
				// if it's a list, we can just define it
				else if (listElementType != null) {
					this.addAll(Production
							.makeList(
									childType,
									listElementType,
									childType.context().getListSeparatorType(
											childType),
									childType.context()
											.getListOptions(childType)
											.toArray(new Context.ListOption[0])));
				}
				// otherwise throw an error since we insist that types be
				// defined in order
				else {
					Utils.err(String
							.format("Tried to add production \"%s\" relying on \"%s\" but there is no existing production that produces that type!",
									production, childType.name()));
				}
			}
		}

		if (!isRecursiveDefinition) {
			this.definedSymbols.add(production.symbolType());
		}
		return super.add(production);
	}

	@Override
	public boolean addAll(Collection<? extends Production> productions) {
		// TODO consider making this support mutually recursive definitions by
		// adding all symbol types defined by the productions to the defined
		// list BEFORE calling add
		boolean changed = false;
		for (Production production : productions) {
			changed |= this.add(production);
		}

		return changed;
	}
}
