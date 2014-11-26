/**
 * 
 */
package compiler.wyvern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import compiler.Context;
import compiler.SymbolType;
import compiler.Utils;
import compiler.lex.Lexer;
import compiler.lex.LexerAction;
import compiler.lex.LexerGenerator;
import compiler.lex.Regex;
import compiler.lex.RegexLexerGenerator;

/**
 * @author Michael
 * 
 */
public class WyvernLexer {
	public static final Context CONTEXT = new Context();
	public static final Lexer LEXER;

	private static SymbolType token(String name) {
		return CONTEXT.getTerminalSymbolType(name);
	}

	private static <T> Set<T> set(T... elements) {
		return Collections.unmodifiableSet(Utils.set(elements));
	}

	/**
	 * The operator symbols
	 */
	public static final SymbolType PLUS = token("+"), MINUS = token("-"), TIMES = token("*"), DIV = token("/"),
			DOT = token("."), EQ = token("=="), NEQ = token("!="), LT = token("<"), LTE = token("<="),
			GTE = token(">="), GT = token(">"), NOT = token("!"), AND = token("and"), OR = token("or"),
			IS = token("is"), AS = token("as"), LAMBDA = token("=>"), SEMICOLON = token(";");
	public static final Set<SymbolType> OPERATORS = set(PLUS, MINUS, TIMES, DIV, DOT, EQ, NEQ, LT, LTE, GTE, GT, NOT,
			AND, OR, IS, AS, LAMBDA, SEMICOLON);

	/**
	 * Keywords
	 */
	public static final SymbolType IF = token("if"), ELSE = token("else"), WHILE = token("while"),
			TYPE = token("type"), INT = token("int"), TEXT = token("text"), NUM = token("num"), BOOL = token("bool"),
			OBJ = token("obj"), PRIVATE = token("private"), RETURN = token("return"), USING = token("using"),
			FALSE = token("false"), TRUE = token("true"), NULL = token("null");
	public static final Set<SymbolType> KEYWORDS = set(IF, ELSE, WHILE, TYPE, INT, TEXT, NUM, BOOL, OBJ, PRIVATE,
			RETURN, USING, FALSE, TRUE, NULL);

	/**
	 * Contextual keywords
	 */
	public static final SymbolType GET = token("get"), SET = token("set");
	public static final Set<SymbolType> CONTEXTUAL_KEYWORDS = set(GET, SET);
	
	/**
	 * Other symbols
	 */
	public static final SymbolType LPAREN = token("("), RPAREN = token(")"), LBRACE = token("{"), RBRACE = token("}"),
			LBRACKET = token("["), RBRACKET = token("]"), COMMA = token(","), IDENTIFIER = token("identifier");

	/**
	 * Literals
	 */
	public static final SymbolType INT_LITERAL = token("int-literal"), NUM_LITERAL = token("num-literal"),
			TEXT_LITERAL = token("text-literal");

	/**
	 * Comments
	 */
	public static final SymbolType SINGLE_LINE_COMMENT = token("single-line-comment"),
			MULTI_LINE_COMMENT = token("multi-line-comment");

	static {
		LinkedHashSet<LexerAction> actions = Utils.<LexerAction> set();
		actions.addAll(getCommentActions());
		actions.addAll(getStringActions());
		actions.addAll(getSimpleSymbolTypeActions());
		actions.addAll(getRegexSymbolTypeActions());
		actions.addAll(getWhitespaceActions());

		LexerGenerator gen = new RegexLexerGenerator();
		LexerGenerator.Result result = gen.generate(CONTEXT, actions);
		LEXER = result.lexer();
	}

	private static LinkedHashSet<LexerAction> getCommentActions() {
		return Utils.set(LexerAction.lexToken("//[^\n]*\n", SINGLE_LINE_COMMENT),
		/*
		 * Match the opening, then repeats of either any number of non-*
		 * characters or * + a non-slash character. To support /STSTST/, we also
		 * allow an optional trailing * before the closing
		 */
		LexerAction.lexToken("/\\*(([^\\*]+)|(\\*[^/]))*(\\*)?\\*/", MULTI_LINE_COMMENT));
	}

	private static LinkedHashSet<LexerAction> getStringActions() {
		// 5 backslashes => \\", which the regex engine parses as CHAR('\') CHAR('"')
		return Utils.set(LexerAction.lexToken("\"([^\"]|(\\\\\"))*\"", TEXT_LITERAL));
	}

	private static LinkedHashSet<LexerAction> getSimpleSymbolTypeActions() {
		List<SymbolType> simpleTypes = new ArrayList<SymbolType>();
		simpleTypes.addAll(OPERATORS);
		simpleTypes.addAll(KEYWORDS);
		simpleTypes.addAll(CONTEXTUAL_KEYWORDS);
		simpleTypes.addAll(set(LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET, COMMA));

		LinkedHashSet<LexerAction> actions = Utils.set();
		for (SymbolType simpleType : simpleTypes) {
			actions.add(LexerAction.lexToken(Regex.escape(simpleType.name()), simpleType));
		}

		return actions;
	}

	private static LinkedHashSet<LexerAction> getRegexSymbolTypeActions() {
		return Utils.set(LexerAction.lexToken("[\\+\\-]?[0-9]*\\.[0-9]+", NUM_LITERAL),
				LexerAction.lexToken("[a-zA-Z][a-zA-Z0-9]*", IDENTIFIER), LexerAction.lexToken("[\\+\\-]?[0-9]+", INT));
	}

	private static LinkedHashSet<LexerAction> getWhitespaceActions() {
		return Utils.set(LexerAction.skip(LexerAction.DEFAULT_SET, "[ \\r\\n\\t]"));
	}
}
