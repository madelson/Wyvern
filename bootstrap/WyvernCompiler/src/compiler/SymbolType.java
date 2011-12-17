/**
 * 
 */
package compiler;

/**
 * @author Michael
 *
 */
public interface SymbolType {
	public Context context();
	public String name();
	public boolean isTerminal();
	public Symbol createSymbol(Symbol... children);
	public Symbol createSymbol(Iterable<Symbol> children);
	public Symbol createSymbol(String text, int line, int position);
}
