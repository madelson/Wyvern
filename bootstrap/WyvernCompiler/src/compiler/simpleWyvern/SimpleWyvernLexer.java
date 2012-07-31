/**
 * 
 */
package compiler.simpleWyvern;

import java.util.Collections;
import java.util.LinkedHashSet;

import compiler.Context;
import compiler.SymbolType;
import compiler.Utils;
import compiler.lex.Lexer;
import compiler.lex.LexerAction;
import compiler.lex.LexerGenerator;
import compiler.lex.Regex;
import compiler.lex.RegexLexerGenerator;

/**
 * @author Michael Adelson
 *
 */
public class SimpleWyvernLexer {
	public static final Context CONTEXT = new Context();
	public static final Lexer LEXER;
	public static final String IN_STRING = "IN_STRING", IN_STRING_ESCAPE = "IN_STRING_ESCAPE";

	/**
	 * The terminal symbol types for the SimpleWyvern language
	 */
	public static final SymbolType LPAREN = CONTEXT.getTerminalSymbolType("("),
			RPAREN = CONTEXT.getTerminalSymbolType(")"),
			LBRACE = CONTEXT.getTerminalSymbolType("{"),
			RBRACE = CONTEXT.getTerminalSymbolType("}"),
			LCARET = CONTEXT.getTerminalSymbolType("<"),
			RCARET = CONTEXT.getTerminalSymbolType(">"),
			LAMBDA_OPERATOR = CONTEXT.getTerminalSymbolType("=>"),
			ASSIGN = CONTEXT.getTerminalSymbolType("="),
			QUESTION_MARK = CONTEXT.getTerminalSymbolType("?"),
			AND = CONTEXT.getTerminalSymbolType("and"),
			OR = CONTEXT.getTerminalSymbolType("or"),
			TYPE = CONTEXT.getTerminalSymbolType("type"),
			IS = CONTEXT.getTerminalSymbolType("is"),
			WHERE = CONTEXT.getTerminalSymbolType("where"),
			WHILE = CONTEXT.getTerminalSymbolType("while"),
			TRY = CONTEXT.getTerminalSymbolType("try"),
			CATCH = CONTEXT.getTerminalSymbolType("catch"),
			FINALLY = CONTEXT.getTerminalSymbolType("finally"),
			BREAK = CONTEXT.getTerminalSymbolType("break"),
			CONTINUE = CONTEXT.getTerminalSymbolType("continue"),
			CASE = CONTEXT.getTerminalSymbolType("case"),
			WHEN = CONTEXT.getTerminalSymbolType("when"),
			PRIVATE = CONTEXT.getTerminalSymbolType("private"),
			FAMILY = CONTEXT.getTerminalSymbolType("family"),
			DEFAULT = CONTEXT.getTerminalSymbolType("default"),
			ACCESS = CONTEXT.getTerminalSymbolType("."),
			COMMA = CONTEXT.getTerminalSymbolType(","),
			REAL_LITERAL = CONTEXT.getTerminalSymbolType("real-literal"),
			INT_LITERAL = CONTEXT.getTerminalSymbolType("int-literal"),
			CHAR_LITERAL = CONTEXT.getTerminalSymbolType("char-literal"),
			STRING_TERMINATOR = CONTEXT.getTerminalSymbolType("\""),
			STRING_TEXT = CONTEXT.getTerminalSymbolType("string-text"),
			ESCAPE = CONTEXT.getTerminalSymbolType("\\"),
			TYPE_IDENTIFIER = CONTEXT.getTerminalSymbolType("type-identifier"),
			IDENTIFIER = CONTEXT.getTerminalSymbolType("identifier"),
			STMT_END = CONTEXT.getTerminalSymbolType(";"),
			PACKAGE = CONTEXT.getTerminalSymbolType("package"),
			SINGLE_LINE_COMMENT = CONTEXT.getTerminalSymbolType("single-line-comment");
	
	static {
		LinkedHashSet<LexerAction> actions = Utils.<LexerAction>set();
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
		return Utils.set(
			LexerAction.lexToken("//.*\n", SINGLE_LINE_COMMENT)
		);
	}
	
	private static LinkedHashSet<LexerAction> getStringActions() {
		return Utils.set(
			LexerAction.enter(LexerAction.DEFAULT_SET, STRING_TERMINATOR.name(), STRING_TERMINATOR, IN_STRING),
			LexerAction.enter(Collections.singleton(IN_STRING), "\\\\", ESCAPE, IN_STRING_ESCAPE),
			LexerAction.leave(Collections.singleton(IN_STRING_ESCAPE), ".", STRING_TEXT),
			LexerAction.leave(Collections.singleton(IN_STRING), STRING_TERMINATOR.name(), STRING_TERMINATOR),
			LexerAction.lexToken(Collections.singleton(IN_STRING), "([a-zA-Z0-9\n\\+\\-\\*/\\. ]+)|.", STRING_TEXT)
		);
	}
	
	private static LinkedHashSet<LexerAction> getSimpleSymbolTypeActions() {
		SymbolType[] simpleTypes = new SymbolType[] {
			LPAREN, RPAREN, LBRACE, RBRACE, LCARET, RCARET,
			LAMBDA_OPERATOR, ASSIGN, AND, OR, STMT_END, ACCESS, COMMA, QUESTION_MARK,
			CASE, WHEN, BREAK, CONTINUE, TYPE, PRIVATE, FAMILY, DEFAULT, TRY, CATCH, FINALLY, PACKAGE, IS, WHERE
		};
		
		LinkedHashSet<LexerAction> actions = Utils.set();
		for (SymbolType simpleType : simpleTypes) {
			actions.add(LexerAction.lexToken(Regex.escape(simpleType.name()), simpleType));
		}
		
		return actions;
	}
	
	private static LinkedHashSet<LexerAction> getRegexSymbolTypeActions() {
		return Utils.set(
			LexerAction.lexToken("[\\+\\-]?[0-9]*\\.[0-9]+", REAL_LITERAL),
			LexerAction.lexToken("[a-z][a-zA-Z0-9]*", IDENTIFIER),
			LexerAction.lexToken("[A-Z][a-zA-z0-9]*", TYPE_IDENTIFIER),
			LexerAction.lexToken("[\\+\\-]?[0-9]+", INT_LITERAL),
			LexerAction.lexToken("'\\\\?.'", CHAR_LITERAL)
		);
	}
	
	private static LinkedHashSet<LexerAction> getWhitespaceActions() {
		return Utils.set(
			LexerAction.skip(LexerAction.DEFAULT_SET, "[ \\r\\n\\t]+")
		);
	}
}
