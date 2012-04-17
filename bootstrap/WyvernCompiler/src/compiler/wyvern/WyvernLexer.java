/**
 * 
 */
package compiler.wyvern;

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
 * @author Michael
 *
 */
public class WyvernLexer {
	public static final Context CONTEXT = new Context();
	public static final Lexer LEXER;
	public static final String IN_STRING = "IN_STRING", IN_STRING_ESCAPE = "IN_STRING_ESCAPE", IN_COMMENT = "IN_COMMENT";
	
	/**
	 * The terminal symbol types for the Wyvern language
	 */
	public static final SymbolType LPAREN = CONTEXT.getTerminalSymbolType("("),
			RPAREN = CONTEXT.getTerminalSymbolType(")"),
			LBRACE = CONTEXT.getTerminalSymbolType("{"),
			RBRACE = CONTEXT.getTerminalSymbolType("}"),
			LBRACKET = CONTEXT.getTerminalSymbolType("["),
			RBRACKET = CONTEXT.getTerminalSymbolType("]"),
			LCARET = CONTEXT.getTerminalSymbolType("<"),
			RCARET = CONTEXT.getTerminalSymbolType(">"),
			LAMBDA_OPERATOR = CONTEXT.getTerminalSymbolType("=>"),
			ASSIGN = CONTEXT.getTerminalSymbolType("="),
			EQUALS = CONTEXT.getTerminalSymbolType("=="),
			NOT = CONTEXT.getTerminalSymbolType("!"),
			PLUS = CONTEXT.getTerminalSymbolType("+"),
			MINUS = CONTEXT.getTerminalSymbolType("-"),
			TIMES = CONTEXT.getTerminalSymbolType("*"),
			DIVIDED_BY = CONTEXT.getTerminalSymbolType("/"),
			AND = CONTEXT.getTerminalSymbolType("and"),
			OR = CONTEXT.getTerminalSymbolType("or"),
			IF = CONTEXT.getTerminalSymbolType("if"),
			ELSE = CONTEXT.getTerminalSymbolType("else"),
			USING = CONTEXT.getTerminalSymbolType("using"),
			TYPE = CONTEXT.getTerminalSymbolType("type"),
			PRIVATE = CONTEXT.getTerminalSymbolType("private"),
			FAMILY = CONTEXT.getTerminalSymbolType("family"),
			ACCESS = CONTEXT.getTerminalSymbolType("."),
			COMMA = CONTEXT.getTerminalSymbolType(","),
			COLON = CONTEXT.getTerminalSymbolType(":"),
			STRING_TERMINATOR = CONTEXT.getTerminalSymbolType("\""),
			STRING_TEXT = CONTEXT.getTerminalSymbolType("string text"),
			ESCAPE = CONTEXT.getTerminalSymbolType("\\"),
			COMMENT_START = CONTEXT.getTerminalSymbolType("comment start"),
			COMMENT_END = CONTEXT.getTerminalSymbolType("comment end"),
			COMMENT_TEXT = CONTEXT.getTerminalSymbolType("comment text"),
			TYPE_NAME = CONTEXT.getTerminalSymbolType("type name"),
			IDENTIFIER = CONTEXT.getTerminalSymbolType("identifier"),
			INT = CONTEXT.getTerminalSymbolType("int"),
			REAL = CONTEXT.getTerminalSymbolType("real"),
			CHAR = CONTEXT.getTerminalSymbolType("char"),
			OBJECT_ALIAS = CONTEXT.getTerminalSymbolType("obj"),
			STRING_ALIAS = CONTEXT.getTerminalSymbolType("str"),
			INT_ALIAS = CONTEXT.getTerminalSymbolType("int"),
			CHAR_ALIAS = CONTEXT.getTerminalSymbolType("char"),
			TRUE = CONTEXT.getTerminalSymbolType("true"),
			FALSE = CONTEXT.getTerminalSymbolType("false"),
			BOOLEAN_ALIAS = CONTEXT.getTerminalSymbolType("bool"),
			SEQUENCE_ALIAS = CONTEXT.getTerminalSymbolType("seq"),
			STMT_END = CONTEXT.getTerminalSymbolType(";"),
			PACKAGE = CONTEXT.getTerminalSymbolType("package");
	
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
			LexerAction.enter(Utils.set(Lexer.DEFAULT_STATE, IN_COMMENT), "/\\*", COMMENT_START, IN_COMMENT),
			LexerAction.leave(Collections.singleton(IN_COMMENT), "\\*/", COMMENT_END),
			// pick up comment text (possibly in chunks) without mistakenly looking
			// past "*/"
			LexerAction.lexToken(Collections.singleton(IN_COMMENT), "([a-zA-Z0-9\n\\+\\-\\. ]+)|.", COMMENT_TEXT)
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
			LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET, LCARET, RCARET,
			LAMBDA_OPERATOR, ASSIGN, EQUALS, NOT, PLUS, MINUS, TIMES, DIVIDED_BY, AND, OR, STMT_END, ACCESS, COMMA, COLON,
			IF, ELSE, TYPE, PRIVATE, FAMILY, TRUE, FALSE, USING, PACKAGE,
			OBJECT_ALIAS, STRING_ALIAS, INT_ALIAS, BOOLEAN_ALIAS, CHAR_ALIAS, SEQUENCE_ALIAS
		};
		
		LinkedHashSet<LexerAction> actions = Utils.set();
		for (SymbolType simpleType : simpleTypes) {
			actions.add(LexerAction.lexToken(Regex.escape(simpleType.name()), simpleType));
		}
		
		return actions;
	}
	
	private static LinkedHashSet<LexerAction> getRegexSymbolTypeActions() {
		return Utils.set(
			LexerAction.lexToken("[\\+\\-]?[0-9]*\\.[0-9]+", REAL),
			LexerAction.lexToken("[a-z][a-zA-Z0-9]*", IDENTIFIER),
			LexerAction.lexToken("[A-Z][a-zA-z0-9]*", TYPE_NAME),
			LexerAction.lexToken("[\\+\\-]?[0-9]+", INT),
			LexerAction.lexToken("'\\\\?.'", CHAR)
		);
	}
	
	private static LinkedHashSet<LexerAction> getWhitespaceActions() {
		return Utils.set(
			LexerAction.skip(LexerAction.DEFAULT_SET, "[ \\n\\t]")
		);
	}
}
