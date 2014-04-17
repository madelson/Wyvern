package compiler.simplewyvern;

import static compiler.wyvern.WyvernLexer.CONTEXT;

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
import compiler.parse.Production;
import compiler.parse.Precedence.ProductionPrecedence;

import static compiler.simplewyvern.SimpleWyvernLexer.*;

/**
 * 
 * @author Michael
 * 
 */
public class SimpleWyvernParser {
	public static final Grammar GRAMMAR;
	public static final Parser PARSER;

	private static final SymbolType PROGRAM = CONTEXT
			.getNonTerminalSymbolType("program"), GENERIC_PARAMETERS = CONTEXT
			.getNonTerminalSymbolType("generic-parameters"),
			TYPE_NAME = CONTEXT.getNonTerminalSymbolType("type-name");

	static {
		GRAMMAR = buildGrammar();
		ParserGenerator gen = new LALRGenerator();
		ParserGenerator.Result result = gen.generate(GRAMMAR);
		PARSER = result.parser();
	}

	private static Grammar buildGrammar() {
		CheckedProductionSet productions = new CheckedProductionSet();
		LinkedHashMap<Set<SymbolType>, Associativity> symbolTypePrecedences = new LinkedHashMap<Set<SymbolType>, Associativity>();
		Map<Production, SymbolType> precedenceAssignments = new LinkedHashMap<Production, SymbolType>();

		// initial definition of generic parameters which allows us to define
		// them before type name
		productions.add(new Production(GENERIC_PARAMETERS, LCARET, CONTEXT.listOf(COMMA, Context.ListOption.AllowEmpty), RCARET));

		// defines type name as:
		// ["."-separated "identifier" list .]"."-separated "type-identifier[generics]" list
		productions.add(new Production(TYPE_NAME, CONTEXT.optional(CONTEXT
				.tuple(CONTEXT.listOf(IDENTIFIER, ACCESS), ACCESS)), CONTEXT
				.listOf(CONTEXT.tuple(TYPE_IDENTIFIER,
						CONTEXT.optional(GENERIC_PARAMETERS)), ACCESS)));

		// expands the definition of generics to include type names
		productions.add(new Production(GENERIC_PARAMETERS, LCARET, CONTEXT
				.listOf(TYPE_NAME, COMMA)));

		return new Grammar(CONTEXT, "Wyvern", PROGRAM, productions,
				Precedence.createFunction(symbolTypePrecedences,
						ProductionPrecedence.LeftmostTerminal,
						precedenceAssignments));
	}
}
