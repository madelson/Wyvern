/**
 * 
 */
package compiler.parse;

import compiler.SymbolType;

/**
 * @author Michael
 *
 */
public interface PrecedenceFunction {
	public SymbolType precedenceSymbolFor(Production production);
	public Integer precedenceOf(SymbolType symbolType);
	public Associativity associativityOf(SymbolType symbolType);
}
