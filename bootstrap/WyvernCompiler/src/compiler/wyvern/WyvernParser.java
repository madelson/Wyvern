/**
 * 
 */
package compiler.wyvern;

import static compiler.wyvern.WyvernLexer.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import compiler.Context;
import compiler.SymbolType;
import compiler.parse.Associativity;
import compiler.parse.CheckedProductionSet;
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

	private static SymbolType symbol(String name) {
		return CONTEXT.getNonTerminalSymbolType(name);
	}

	// non-terminal symbols
	public static final SymbolType STATEMENT = symbol("statement"), EXPRESSION = symbol("expression"),
			BLOCK = symbol("block"), TYPE_DECL = symbol("type-decl"), METHOD_DECL = symbol("method-decl"),
			METHOD_BODY = symbol("method-body"), PROPERTY_DECL = symbol("property-decl"),
			GENERIC_PARAMETERS = symbol("generic-parameters"), NAME_PART = symbol("name-part"), NAME = symbol("name"), PROGRAM = symbol("program");

	static {
		GRAMMAR = buildGrammar();
		ParserGenerator gen = new LALRGenerator();
		ParserGenerator.Result result = gen.generate(GRAMMAR);
		if (!result.errors().isEmpty()) {
			throw new RuntimeException("Failed to generate parser: " + result.errors().get(0));
		}
		PARSER = result.parser();
	}

	private static Grammar buildGrammar() {
		CheckedProductionSet productions = new CheckedProductionSet();
		LinkedHashMap<Set<SymbolType>, Associativity> symbolTypePrecedences = buildSymbolTypePrecedences();
		Map<Production, SymbolType> precedenceAssignments = new LinkedHashMap<Production, SymbolType>();
		
		// TODO: instead of this fake production CPS should allow defining option/one-of if one side is defined
		productions.add(new Production(NAME, CONTEXT.getTerminalSymbolType("allow-circular-definition")));
		productions.add(new Production(GENERIC_PARAMETERS, LT, CONTEXT.listOf(NAME, COMMA, Context.ListOption.AllowEmpty), GT));
		productions.add(new Production(NAME_PART, CONTEXT.oneOf(IDENTIFIER, GET, SET), CONTEXT.optional(GENERIC_PARAMETERS)));		
		productions.add(new Production(NAME, CONTEXT.listOf(NAME_PART, DOT)));
		
		productions.add(new Production(EXPRESSION, NAME));
		productions.add(new Production(STATEMENT, EXPRESSION, SEMICOLON));
		
		productions.add(new Production(PROGRAM, STATEMENT));
		
		return new Grammar(CONTEXT, "Wyvern", PROGRAM, productions, Precedence.createFunction(symbolTypePrecedences,
				ProductionPrecedence.LeftmostTerminal, precedenceAssignments));
	}

	private static LinkedHashMap<Set<SymbolType>, Associativity> buildSymbolTypePrecedences() {
		LinkedHashMap<Set<SymbolType>, Associativity> map = new LinkedHashMap<Set<SymbolType>, Associativity>();

		return map;
	}
}
