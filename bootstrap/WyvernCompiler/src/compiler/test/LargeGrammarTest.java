/**
 * 
 */
package compiler.test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import compiler.Context;
import compiler.SymbolType;
import compiler.Utils;
import compiler.parse.Associativity;
import compiler.parse.CheckedProductionSet;
import compiler.parse.Grammar;
import compiler.parse.LALRGenerator;
import compiler.parse.LRGenerator;
import compiler.parse.Precedence;
import compiler.parse.Precedence.ProductionPrecedence;
import compiler.parse.PrecedenceFunction;
import compiler.parse.Production;

/**
 * @author Michael
 *
 */
public class LargeGrammarTest {
	private static final Context c = new Context();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Grammar g = buildGrammar();
		LRGenerator.Result result = new LALRGenerator().generate(g);
		Utils.check(result.parser() != null, "failed to build a parser! " + result.errors());
		
		System.out.println("All large grammar tests passed!");
	}

	private static final Grammar buildGrammar() {
		CheckedProductionSet productions = new CheckedProductionSet();
		LinkedHashMap<Set<SymbolType>, Associativity> symbolTypePrecedences = new LinkedHashMap<Set<SymbolType>, Associativity>();
		
		SymbolType exp = c.getNonTerminalSymbolType("expression");
		
		for (char ch = 'a'; ch <= 'z'; ch++) {
			for (int i = 0; i < 10; ++i) {
				productions.add(new Production(exp, c.getTerminalSymbolType(String.valueOf(ch) + i)));
			}
		}
		
		for (String binop : new String[] { "+", "-", "*", "/", "%", "<", "<=", "=", ">=", ">", "!=", "<<", ">>" }) {
			productions.add(new Production(exp, exp, c.getTerminalSymbolType(binop), exp));
			symbolTypePrecedences.put(Collections.singleton(c.getTerminalSymbolType(binop)), Associativity.Left);
		}
		
		productions.add(new Production(exp, c.getTerminalSymbolType("("), exp, c.getTerminalSymbolType(")")));
		productions.add(new Production(exp, c.getTerminalSymbolType("{"), c.listOf(exp, c.getTerminalSymbolType(",")), c.getTerminalSymbolType("}")));
		productions.add(new Production(exp, c.getTerminalSymbolType("IF"), exp, c.getTerminalSymbolType("THEN"), exp, c.getTerminalSymbolType("ELSE"), exp));
		symbolTypePrecedences.put(Utils.set(c.getTerminalSymbolType("IF"), c.getTerminalSymbolType("THEN"), c.getTerminalSymbolType("ELSE")), Associativity.NonAssociative);
		
		//SymbolType fc = c.getNonTerminalSymbolType("from-clause"), wc = c.getNonTerminalSymbolType("where-clause"), obc = c.getNonTerminalSymbolType("order-by-clause"),
		//		q = c.getNonTerminalSymbolType("query");
		//Production fcp = new Production(fc, c.getTerminalSymbolType("FROM"), exp, c.getTerminalSymbolType("IN"), exp);
		//symbolTypePrecedences.put(Collections.singleton(c.getTerminalSymbolType("FROM")), value)
		//productions.add(new Production(wc, c.getTerminalSymbolType("WHERE"), exp));
		//productions.add(new Production(obc, c.getTerminalSymbolType("ORDER BY"), exp));
		//symbolTypePrecedences.put(Utils.set(c.getTerminalSymbolType("FROM"), c.getTerminalSymbolType("WHERE"), c.getTerminalSymbolType("ORDER_BY")), Associativity.Left);		
		//productions.add(new Production(q, fc));
		//productions.add(new Production(exp, q));
		
		PrecedenceFunction precedence = Precedence.createFunction(symbolTypePrecedences, ProductionPrecedence.LeftmostTerminal, new LinkedHashMap<Production, SymbolType>());
		Grammar result = new Grammar(c, "large", exp, productions, precedence);
		return result;
	}
}
