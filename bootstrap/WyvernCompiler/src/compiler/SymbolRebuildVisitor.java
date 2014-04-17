/**
 * 
 */
package compiler;

/**
 * @author Michael
 *
 */
public abstract class SymbolRebuildVisitor extends SymbolVisitor<Symbol> {
	@Override
	protected Symbol visitTerminal(Symbol symbol) {
		return symbol;
	}
	
	@Override
	protected Symbol visitNonTerminal(Symbol symbol) {
		Symbol[] rebuiltChildren = null;
		for (int i = 0; i < symbol.children().size(); ++i) {
			Symbol child = symbol.children().get(i),
				visited = this.visit(child);
			if (rebuiltChildren != null) {
				rebuiltChildren[i] = visited;
			} else if (child != visited) {
				rebuiltChildren = new Symbol[symbol.children().size()];
				for (int j = 0; j < i; ++j) {
					rebuiltChildren[j] = symbol.children().get(j);
				}
				rebuiltChildren[i] = visited;
			}			
		}
		
		if (rebuiltChildren != null) {
			Symbol rebuilt = symbol.type().createSymbol(rebuiltChildren);
			return rebuilt;
		}
		return symbol;
	}
}
