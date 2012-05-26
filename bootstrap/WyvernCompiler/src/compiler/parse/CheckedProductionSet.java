/**
 * 
 */
package compiler.parse;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
			if (!childType.isTerminal() && !this.definedSymbols.contains(childType))
			{				
				SymbolType optionInnerType = childType.context().getOptionInnerType(childType);
				Set<SymbolType> oneOfInnerTypes = childType.context().getOneOfInnerTypes(childType);
				List<SymbolType> tupleInnerTypes = childType.context().getTupleInnerTypes(childType);
				
				// if it's an option we can just define it
				if (optionInnerType != null) {
					this.addAll(Production.makeOption(optionInnerType));
				}
				// if it's an OR we can just define it
				else if (oneOfInnerTypes != null) {
					this.addAll(Production.makeOneOf(oneOfInnerTypes.toArray(new SymbolType[oneOfInnerTypes.size()])));
				}
				// if it's a tuple we can just define it
				else if (tupleInnerTypes != null) {
					this.addAll(Production.makeTuple(tupleInnerTypes.toArray(new SymbolType[tupleInnerTypes.size()])));
				}
				// if it's a recursive definition, allow it but don't count the type as defined
				else if (childType.equals(production.symbolType())) {
					isRecursiveDefinition = true;
				} else {
					Utils.err(String.format("Tried to add production \"%s\" relying on \"%s\" but there is no existing production that produces that type!", production, childType.name()));
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
		boolean changed = false;
		for (Production production : productions) {
			changed |= this.add(production);
		}
		
		return changed;
	}
}
