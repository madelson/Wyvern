/**
 * 
 */
package compiler.wyvern;

import compiler.Context;
import compiler.SymbolType;
import compiler.parse.Grammar;
import compiler.parse.LALRGenerator;
import compiler.parse.Parser;
import compiler.parse.ParserGenerator;

/**
 * @author Michael
 *
 */
public class WyvernParser {
	public static Context CONTEXT = WyvernLexer.CONTEXT;
	public static Grammar GRAMMAR;
	public static Parser PARSER;
	
	// non-terminal symbols
	public static SymbolType EXP = CONTEXT.getNonTerminalSymbolType("exp"),
			STMT = CONTEXT.getNonTerminalSymbolType("stmt"),
			PROGRAM = CONTEXT.getNonTerminalSymbolType("program");
	
	static {
		GRAMMAR = buildGrammar();
		ParserGenerator gen = new LALRGenerator();
		ParserGenerator.Result result = gen.generate(GRAMMAR);
		PARSER = result.parser();
	}
	
	private static Grammar buildGrammar() {
		return null;
	}
}
