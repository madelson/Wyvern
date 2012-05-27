/**
 * 
 */
package compiler.lex;

import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import compiler.Context;
import compiler.Symbol;
import compiler.SymbolType;
import compiler.Utils;
import compiler.automata.Characters;
import compiler.automata.FiniteAutomaton;
import compiler.automata.State;
import compiler.canonicalize.Canonicalize;
import compiler.parse.Associativity;
import compiler.parse.Grammar;
import compiler.parse.LALRGenerator;
import compiler.parse.Parser;
import compiler.parse.Precedence;
import compiler.parse.Precedence.ProductionPrecedence;
import compiler.parse.Production;

/**
 * @author Michael
 * 
 */
public class Regex {
	private static final Context context = new Context();

	public static final SymbolType KLEENE_CLOSURE = context
			.getTerminalSymbolType("*"), OR = context
			.getTerminalSymbolType("|"), ESCAPE = context
			.getTerminalSymbolType("\\"), LPAREN = context
			.getTerminalSymbolType("("), RPAREN = context
			.getTerminalSymbolType(")"), ZERO_OR_ONE = context
			.getTerminalSymbolType("?"), ONE_PLUS = context
			.getTerminalSymbolType("+"), RANGE_OPERATOR = context
			.getTerminalSymbolType("-"), LBRACKET = context
			.getTerminalSymbolType("["), RBRACKET = context
			.getTerminalSymbolType("]"), WILDCARD = context
			.getTerminalSymbolType("."), CHAR = context
			.getTerminalSymbolType("CHAR");

	public static final SymbolType REGEX = context
			.getNonTerminalSymbolType("REGEX"), REGEX_LIST = context
			.getNonTerminalSymbolType("REGEX_LIST"), ESCAPED = context
			.getNonTerminalSymbolType("ESCAPED"), RANGE = context
			.getNonTerminalSymbolType("RANGE"), SET = context
			.getNonTerminalSymbolType("SET"), SET_LIST = context
			.getNonTerminalSymbolType("SET_LIST");

	public static final Grammar GRAMMAR;
	private static final Lexer lexer;
	private static final Parser parser;

	static {
		LinkedHashSet<Production> productions = new LinkedHashSet<Production>();

		for (SymbolType terminalType : Utils.set(KLEENE_CLOSURE, OR, ESCAPE,
				LPAREN, RPAREN, ZERO_OR_ONE, ONE_PLUS, RANGE_OPERATOR,
				LBRACKET, RBRACKET, WILDCARD, CHAR)) {
			productions.add(new Production(ESCAPED, ESCAPE, terminalType));
		}

		productions.add(new Production(RANGE, CHAR, RANGE_OPERATOR, CHAR));

		productions.addAll(Production.makeList(SET_LIST, SET, null, Context.ListOption.AllowEmpty));

		productions.add(new Production(SET, RANGE));
		productions.add(new Production(SET, ESCAPED));
		productions.add(new Production(SET, CHAR));

		productions.addAll(Production.makeList(REGEX_LIST, REGEX, null, Context.ListOption.AllowEmpty));

		productions.add(new Production(REGEX, ESCAPED));
		productions.add(new Production(REGEX, LPAREN, REGEX_LIST, RPAREN));
		productions.add(new Production(REGEX, REGEX, OR, REGEX));
		productions.add(new Production(REGEX, REGEX, KLEENE_CLOSURE));
		productions.add(new Production(REGEX, REGEX, ZERO_OR_ONE));
		productions.add(new Production(REGEX, REGEX, ONE_PLUS));
		productions.add(new Production(REGEX, LBRACKET, SET_LIST, RBRACKET));
		productions.add(new Production(REGEX, WILDCARD));
		productions.add(new Production(REGEX, CHAR));

		LinkedHashMap<Set<SymbolType>, Associativity> precedence = new LinkedHashMap<Set<SymbolType>, Associativity>();
		precedence.put(Utils.set(KLEENE_CLOSURE, ZERO_OR_ONE, ONE_PLUS),
				Associativity.NonAssociative);
		precedence.put(Utils.set(OR), Associativity.Left);

		GRAMMAR = new Grammar(context, "Regex", REGEX_LIST, productions,
				Precedence.createFunction(precedence,
						ProductionPrecedence.LeftmostTerminal,
						Collections.<Production, SymbolType> emptyMap()));

		LinkedHashSet<LexerAction> actions = new LinkedHashSet<LexerAction>();
		for (SymbolType terminalType : GRAMMAR.terminalSymbolTypes())
			if (terminalType.equals(CHAR))
				actions.add(LexerAction.lexToken("", CHAR));
			else if (!terminalType.equals(context.eofType()))
				actions.add(LexerAction.lexToken(terminalType.name(),
						terminalType));
		lexer = new CharLexerGenerator().generate(context, actions).lexer();

		parser = new LALRGenerator().generate(GRAMMAR).parser();
	}

	public static Lexer lexer() {
		return lexer;
	}

	public static Parser parser() {
		return parser;
	}

	public static Parser.Result parse(String regex) {
		return parser().parse(lexer().lex(new StringReader(regex)));
	}

	public static Symbol canonicalize(Symbol regexParseTree) {
		Utils.check(regexParseTree.type().equals(REGEX_LIST));

		Map<SymbolType, SymbolType> listTypes = new HashMap<SymbolType, SymbolType>();
		listTypes.put(REGEX_LIST, null);
		listTypes.put(SET_LIST, null);

		return Canonicalize.flattenLists(regexParseTree, listTypes, false);
	}

	/**
	 * Builds an NFA for the given accept value and regex, and returns the start
	 * state (if the builder already has states, then this will not be the same
	 * as the eventual automata's start state).
	 */
	public static <T> State<T> buildNfaFor(
			FiniteAutomaton.Builder<T, Character> builder, T acceptValue,
			Symbol regexSymbol) {
		State<T> startState = builder.newState();
		State<T> acceptState = builder.newState(acceptValue);
		State<T> headState = buildNfa(regexSymbol, startState, builder);
		builder.createEdge(headState, acceptState);

		return startState;
	}

	/**
	 * Creates an NFA with a tail leading from the provided start state to a
	 * head state, which is returned.
	 */
	private static <T> State<T> buildNfa(Symbol regexSymbol,
			State<T> startState, FiniteAutomaton.Builder<T, Character> builder) {
		// build the list by creating intermediate start and end states and
		// filling the gaps
		if (regexSymbol.type().equals(REGEX_LIST)) {
			// for strict compliance with MCI, an empty list goes to
			// start - epsilon -> head
			if (regexSymbol.children().isEmpty()) {
				State<T> headState = builder.newState();
				builder.createEdge(startState, headState);
				return headState;
			}

			// start - R1 - R2 ...
			State<T> listHead = startState;
			for (Symbol child : regexSymbol.children()) {
				State<T> newListHead = buildNfa(child, listHead, builder);
				listHead = newListHead;
			}

			return listHead;
		}
		// For a regex, we look at it's type and construct the appropriate DFA.
		if (regexSymbol.type().equals(REGEX)) {
			SymbolType type;
			switch (regexSymbol.children().size()) {
			case 1:
				type = regexSymbol.children().get(0).type();
				if (type.equals(ESCAPED)) {
					char equivalentValue = getChar(regexSymbol.children()
							.get(0));

					// equivalent to 'equivalentCharacter'
					return buildNfa(
							REGEX.createSymbol(CHAR.createSymbol(
									String.valueOf(equivalentValue), 1, 1)),
							startState, builder);
				}
				if (type.equals(CHAR)) {
					// start - ch -> head
					State<T> headState = builder.newState();
					builder.createEdge(startState, Collections
							.singleton(getChar(regexSymbol.children().get(0))),
							headState);
					return headState;
				}
				if (type.equals(WILDCARD)) {
					// start - any -> head
					State<T> headState = builder.newState();
					builder.createEdge(startState, Characters.allCharacters(),
							headState);
					return headState;
				}
				throw Utils.err("Should never get here!");
			case 2:
				type = regexSymbol.children().get(1).type();
				if (type.equals(ZERO_OR_ONE)) {
					// same as R | ()
					return buildNfa(REGEX.createSymbol(regexSymbol.children()
							.get(0), OR.createSymbol("FAKE", 1, 1), REGEX
							.createSymbol(LPAREN.createSymbol("FAKE", 1, 1),
									REGEX_LIST.createSymbol(),
									RPAREN.createSymbol("FAKE", 1, 1))),
							startState, builder);
				}
				if (type.equals(ONE_PLUS)) {
					// same as RR*
					Symbol innerRegex = regexSymbol.children().get(0);
					return buildNfa(
							REGEX_LIST.createSymbol(innerRegex, REGEX
									.createSymbol(innerRegex, KLEENE_CLOSURE
											.createSymbol("FAKE", 1, 1))),
							startState, builder);
				}
				if (type.equals(KLEENE_CLOSURE)) {
					/*
					 * start - epsilon -> head AND head - R - epsilon -> head
					 */
					State<T> headState = builder.newState();
					State<T> innerHeadState = buildNfa(regexSymbol.children()
							.get(0), headState, builder);
					builder.createEdge(startState, headState);
					builder.createEdge(innerHeadState, headState);
					return headState;
				}
				throw Utils.err("Should never get here!");
			case 3:
				type = regexSymbol.children().get(1).type();
				if (type.equals(OR)) {
					/*
					 * start - epsilon -> tail AND ( tail - R1 - epsilon -> head
					 * OR tail - R2 - epsilon -> head )
					 */
					State<T> tailState = builder.newState();
					State<T> headState = builder.newState();
					State<T> innerHeadState1 = buildNfa(regexSymbol.children()
							.get(0), tailState, builder);
					State<T> innerHeadState2 = buildNfa(regexSymbol.children()
							.get(2), tailState, builder);
					builder.createEdge(startState, tailState);
					builder.createEdge(innerHeadState1, headState);
					builder.createEdge(innerHeadState2, headState);
					return headState;
				}
				if (type.equals(REGEX_LIST)) {
					// recurse on the list
					return buildNfa(regexSymbol.children().get(1), startState,
							builder);
				}
				if (type.equals(SET_LIST)) {
					/*
					 * start - epsilon -> tail AND ( tail - S1 -> head OR tail -
					 * S2 -> head ... )
					 */
					State<T> tailState = builder.newState();
					State<T> headState = builder.newState();
					for (Symbol setChild : regexSymbol.children().get(1)
							.children()) {
						Symbol setSymbol = setChild.children().get(0);
						Collection<Character> charSet;
						if (setSymbol.type().equals(CHAR)) {
							// tail - ch -> head
							charSet = Collections.singleton(getChar(setSymbol));
						} else if (setSymbol.type().equals(ESCAPED)) {
							// tail - \ch -> head
							charSet = Collections.singleton(getChar(setSymbol));
						} else if (setSymbol.type().equals(RANGE)) {
							// start - [chars] -> end
							char min = getChar(setSymbol.children().get(0)), max = getChar(setSymbol
									.children().get(2));
							charSet = Characters.range(min, max);
						} else {
							throw Utils.err("Should never get here!");
						}
						builder.createEdge(tailState, charSet, headState);
					}
					builder.createEdge(startState, tailState);
					return headState;
				}
				throw Utils.err("Should never get here!");
			default:
				throw Utils.err("Should never get here!");
			}
		}
		throw Utils.err("Should never get here!");
	}

	private static char getChar(Symbol singleCharSymbol) {
		if (singleCharSymbol.type().equals(ESCAPED)) {
			char escapedChar = getChar(singleCharSymbol.children().get(1)), equivalentValue;
			switch (escapedChar) {
			case 'n': // newline
				equivalentValue = '\n';
				break;
			case 't':
				equivalentValue = '\t';
				break;
			case 'r':
				equivalentValue = '\r';
				break;
			default:
				equivalentValue = escapedChar;
				break;
			}

			return equivalentValue;
		}

		// sanity check, until we support more complex character types
		Utils.check(singleCharSymbol.text().length() == 1);

		return singleCharSymbol.text().charAt(0);
	}
	
	/**
	 * Returns a pattern that matches the literal pattern
	 */
	public static String escape(String pattern) {
		Iterator<Symbol> symbols = lexer().lex(new StringReader(pattern));
		StringBuilder sb = new StringBuilder();
		for (Symbol next = symbols.next(); !next.type().equals(context.eofType()); next = symbols.next()) {
			if (!next.type().equals(CHAR)) {
				sb.append(ESCAPE.name());
			}
			sb.append(next.text());
		}
		
		return sb.toString();
	}
}
