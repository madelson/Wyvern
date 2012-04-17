/**
 * 
 */
package compiler.wyvern;

import static compiler.wyvern.WyvernLexer.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import compiler.SymbolType;
import compiler.Utils;
import compiler.parse.Associativity;
import compiler.parse.Grammar;
import compiler.parse.LALRGenerator;
import compiler.parse.Parser;
import compiler.parse.ParserGenerator;
import compiler.parse.Precedence;
import compiler.parse.Precedence.ProductionPrecedence;
import compiler.parse.Production;

/**
 * @author Michael
 *
 */
public class WyvernParser {
	public static Grammar GRAMMAR;
	public static Parser PARSER;

	// non-terminal symbols
	public static SymbolType EXP = CONTEXT.getNonTerminalSymbolType("exp"),
			STMT = CONTEXT.getNonTerminalSymbolType("stmt"),
			STMT_LIST = CONTEXT.getNonTerminalSymbolType("stmt list"),
			PROGRAM = CONTEXT.getNonTerminalSymbolType("program"),
			USING_STMT = CONTEXT.getNonTerminalSymbolType("using stmt"),
			PACKAGE_STMT = CONTEXT.getNonTerminalSymbolType("package stmt"),
			USING_STMT_LIST = CONTEXT.getNonTerminalSymbolType("using stmt list"),
			TYPE_DECL = CONTEXT.getNonTerminalSymbolType("type decl"),
			ATTRIBUTE = CONTEXT.getNonTerminalSymbolType("attribute"),
			ATTRIBUTE_LIST = CONTEXT.getNonTerminalSymbolType("attribute list"),
			GENERIC_PARAMETERS = CONTEXT.getNonTerminalSymbolType("generic list"),
			PROPERTY_DECL = CONTEXT.getNonTerminalSymbolType("property decl"),
			METHOD_DECL = CONTEXT.getNonTerminalSymbolType("method decl"),
			MEMBER_DECL = CONTEXT.getNonTerminalSymbolType("member decl"),
			PACKAGE_NAME = CONTEXT.getNonTerminalSymbolType("package name");
	
	static {
		GRAMMAR = buildGrammar();
		ParserGenerator gen = new LALRGenerator();
		ParserGenerator.Result result = gen.generate(GRAMMAR);
		PARSER = result.parser();
	}
	
	private static Grammar buildGrammar() {
		Set<Production> productions = Utils.<Production>set();
		LinkedHashMap<Set<SymbolType>, Associativity> symbolTypePrecedences = buildSymbolTypePrecedences();
		Map<Production, SymbolType> precedenceAssignments = new LinkedHashMap<Production, SymbolType>();
		
		/* PROGRAM */
		// a program specifies the package and imports, then optionally has the file type, then has the main script
		productions.add(new Production(PROGRAM, PACKAGE_STMT, USING_STMT_LIST, TYPE_DECL, STMT_LIST));
		productions.add(new Production(PROGRAM, PACKAGE_STMT, USING_STMT_LIST, STMT_LIST));
		
		/* PACKAGING/USING STATEMENTS */
		productions.add(new Production(PACKAGE_STMT, PACKAGE, PACKAGE_NAME, STMT_END));
		
		// possibly empty sequence of using statements
		productions.addAll(Production.makeList(USING_STMT_LIST, USING_STMT, null, Production.ListOptions.AllowEmpty));
		
		// a using statement is using a package for now (e. g. using wyvern.collections;)
		productions.add(new Production(USING_STMT, USING, PACKAGE_NAME, STMT_END));
		
		// a package is just a.b.c...
		productions.addAll(Production.makeList(PACKAGE_NAME, IDENTIFIER, ACCESS));
		
		/* TYPE DECLARATION */
		
		
		return new Grammar(CONTEXT, "Wyvern", PROGRAM, productions, Precedence.createFunction(symbolTypePrecedences, ProductionPrecedence.LeftmostTerminal, precedenceAssignments));
	}
	
	private static LinkedHashMap<Set<SymbolType>, Associativity> buildSymbolTypePrecedences() {
		LinkedHashMap<Set<SymbolType>, Associativity> map = new LinkedHashMap<Set<SymbolType>, Associativity>();
		
		return map;
	}
}
