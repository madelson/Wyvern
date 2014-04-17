/**
 * 
 */
package compiler;

/**
 * @author Michael
 *
 */
public abstract class SymbolVisitor<TResult> {
	protected TResult visit(Symbol symbol) {
		TResult result = symbol.type().isTerminal()
			? this.visitTerminal(symbol)
			: this.visitNonTerminal(symbol);
		return result;
	}
	
	protected TResult visitNonTerminal(Symbol symbol) {
		for (Symbol child : symbol.children()) {
			this.visit(child);
		}
		
		return null;
	}
	
	protected TResult visitTerminal(Symbol symbol) {
		return null;
	}
}
