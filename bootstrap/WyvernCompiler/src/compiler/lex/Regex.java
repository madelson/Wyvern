/**
 * 
 */
package compiler.lex;

import java.io.StringReader;
import java.util.*;

import compiler.*;
import compiler.canonicalize.Canonicalize;
import compiler.parse.*;
import compiler.parse.Precedence.ProductionPrecedence;

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

		productions.add(new Production(SET_LIST, SET, SET_LIST));
		productions.add(new Production(SET_LIST));

		productions.add(new Production(SET, RANGE));
		productions.add(new Production(SET, ESCAPED));
		productions.add(new Production(SET, CHAR));

		productions.add(new Production(REGEX_LIST, REGEX, REGEX_LIST));
		productions.add(new Production(REGEX_LIST));

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
	
	public static DfaState nfaFor(SymbolType symbolType, Symbol regexSymbol, LinkedHashSet<DfaState> stateCollection, Set<DfaEdge> edgeCollection) {
		DfaState startState = DfaState.create(stateCollection);
		DfaState acceptState = new DfaState(String.valueOf(stateCollection.size()), symbolType);
		stateCollection.add(acceptState);
		DfaState headState = toNfa(regexSymbol, startState, stateCollection, edgeCollection);
		edgeCollection.add(new DfaEdge(headState, null, acceptState));
		
		return startState;
	}

	/**
	 * Creates an NFA with a tail leading from the provided start state to a
	 * head state, which is returned. All new states and edges are recorded in
	 * the corresponding collections.
	 */
	private static DfaState toNfa(Symbol regexSymbol, DfaState startState,
			LinkedHashSet<DfaState> stateCollection, Set<DfaEdge> edgeCollection) {
		// build the list by creating intermediate start and end states and
		// filling the gaps
		if (regexSymbol.type().equals(REGEX_LIST)) {
			// for strict compliance with MCI, an empty list goes to
			// start - epsilon -> head
			if (regexSymbol.children().isEmpty()) {
				DfaState headState = DfaState.create(stateCollection);
				edgeCollection.add(new DfaEdge(startState, null, headState));
				return headState;
			}

			// start - R1 - R2 ...
			DfaState listHead = startState;
			for (Symbol child : regexSymbol.children()) {
				DfaState newListHead = toNfa(child, listHead, stateCollection,
						edgeCollection);
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
					return toNfa(
							REGEX.createSymbol(CHAR.createSymbol(
									String.valueOf(equivalentValue), 1, 1)),
							startState, stateCollection, edgeCollection);
				}
				if (type.equals(CHAR)) {
					// start - ch -> head
					DfaState headState = DfaState.create(stateCollection);
					edgeCollection.add(new DfaEdge(startState, CharSet
							.single(getChar(regexSymbol.children().get(0))),
							headState));
					return headState;
				}
				if (type.equals(WILDCARD)) {
					// start - any -> head
					DfaState headState = DfaState.create(stateCollection);
					edgeCollection.add(new DfaEdge(startState, CharSet.all(),
							headState));
					return headState;
				}
				throw Utils.err("Should never get here!");
			case 2:
				type = regexSymbol.children().get(1).type();
				if (type.equals(ZERO_OR_ONE)) {
					// same as R | ()
					return toNfa(REGEX.createSymbol(
							regexSymbol.children().get(0), OR.createSymbol(
									"FAKE", 1, 1), REGEX.createSymbol(
									LPAREN.createSymbol("FAKE", 1, 1),
									REGEX_LIST.createSymbol(),
									RPAREN.createSymbol("FAKE", 1, 1))),
							startState, stateCollection, edgeCollection);
				}
				if (type.equals(ONE_PLUS)) {
					// same as RR*
					Symbol innerRegex = regexSymbol.children().get(0);
					return toNfa(
							REGEX_LIST.createSymbol(innerRegex, REGEX
									.createSymbol(innerRegex, KLEENE_CLOSURE
											.createSymbol("FAKE", 1, 1))),
							startState, stateCollection, edgeCollection);
				}
				if (type.equals(KLEENE_CLOSURE)) {
					/*
					 * start - epsilon -> head AND head - R - epsilon -> head
					 */
					DfaState headState = DfaState.create(stateCollection);
					DfaState innerHeadState = toNfa(
							regexSymbol.children().get(0), headState,
							stateCollection, edgeCollection);
					edgeCollection
							.add(new DfaEdge(startState, null, headState));
					edgeCollection.add(new DfaEdge(innerHeadState, null,
							headState));
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
					DfaState tailState = DfaState.create(stateCollection);
					DfaState headState = DfaState.create(stateCollection);
					DfaState innerHeadState1 = toNfa(regexSymbol.children()
							.get(0), tailState, stateCollection, edgeCollection);
					DfaState innerHeadState2 = toNfa(regexSymbol.children()
							.get(2), tailState, stateCollection, edgeCollection);
					edgeCollection
							.add(new DfaEdge(startState, null, tailState));
					edgeCollection.add(new DfaEdge(innerHeadState1, null,
							headState));
					edgeCollection.add(new DfaEdge(innerHeadState2, null,
							headState));
					return headState;
				}
				if (type.equals(REGEX_LIST)) {
					// recurse on the list
					return toNfa(regexSymbol.children().get(1), startState,
							stateCollection, edgeCollection);
				}
				if (type.equals(SET_LIST)) {
					/*
					 * start - epsilon -> tail AND ( tail - S1 -> head OR tail -
					 * S2 -> head ... )
					 */
					DfaState tailState = DfaState.create(stateCollection);
					DfaState headState = DfaState.create(stateCollection);
					for (Symbol setChild : regexSymbol.children().get(1)
							.children()) {
						Symbol setSymbol = setChild.children().get(0);
						CharSet charSet;
						if (setSymbol.type().equals(CHAR)) {
							// tail - ch -> head
							charSet = CharSet.single(getChar(setSymbol));
						} else if (setSymbol.type().equals(ESCAPED)) {
							// tail - \ch -> head
							charSet = CharSet.single(getChar(setSymbol));
						} else if (setSymbol.type().equals(RANGE)) {
							// start - [chars] -> end
							char min = getChar(setSymbol.children().get(0)), max = getChar(setSymbol
									.children().get(2));
							charSet = CharSet.range(min, max);
						} else {
							throw Utils.err("Should never get here!");
						}
						edgeCollection.add(new DfaEdge(tailState, charSet,
								headState));
					}
					edgeCollection
							.add(new DfaEdge(startState, null, tailState));
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

	public static class DfaEdge extends
			Tuples.Trio<DfaState, CharSet, DfaState> {
		public DfaEdge(DfaState from, CharSet charSet, DfaState to) {
			super(from, charSet, to);
		}

		public DfaState from() {
			return this.item1();
		}

		public CharSet character() {
			return this.item2();
		}

		public DfaState to() {
			return this.item3();
		}
	}

	public static class DfaState extends Tuples.Duo<String, SymbolType> {
		public DfaState(String name, SymbolType symbolType) {
			super(name, symbolType);
		}

		public String name() {
			return this.item1();
		}

		public SymbolType symbolType() {
			return this.item2();
		}

		public static DfaState create(Set<DfaState> stateCollection) {
			DfaState newState = new DfaState(String.valueOf(stateCollection
					.size()), null);
			stateCollection.add(newState);

			return newState;
		}
	}

	public static abstract class CharSet {
		public abstract boolean contains(char ch);

		public CharSet union(final CharSet that) {
			return new CharSet() {

				@Override
				public boolean contains(char ch) {
					return this.contains(ch) || that.contains(ch);
				}
				
				@Override
				public String toString() {
					return this + " U " + that;
				}

			};
		}

		public static CharSet union(final Iterable<CharSet> charSets) {
			CharSet unionSet = empty();
			for (CharSet charSet : charSets) {
				unionSet = charSet.union(unionSet);
			}

			return unionSet;
		}

		public static CharSet single(final char member) {
			return new CharSet() {

				@Override
				public boolean contains(char ch) {
					return ch == member;
				}
				
				@Override
				public String toString() {
					return String.valueOf(member);
				}
			};
		}

		public static CharSet range(final char min, final char max) {
			Utils.check(
					min <= max,
					"The character at the beginning of a range must come before the character at the end of it");

			return new CharSet() {

				@Override
				public boolean contains(char ch) {
					return min <= ch && max >= ch;
				}

				@Override
				public String toString() {
					return min + " - " + max;
				}
			};
		}

		public static CharSet all() {
			return new CharSet() {

				@Override
				public boolean contains(char ch) {
					return true;
				}

				@Override
				public String toString() {
					return "any";
				}
			};
		}

		public static CharSet empty() {
			return new CharSet() {

				@Override
				public boolean contains(char ch) {
					return false;
				}

				@Override
				public String toString() {
					return "{ }";
				}
			};
		}
	}
}
