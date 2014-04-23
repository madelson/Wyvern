/**
 * 
 */
package compiler.test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import compiler.Symbol;
import compiler.SymbolType;
import compiler.Utils;
import static compiler.wyvern.WyvernLexer.*;

/**
 * @author mikea_000
 *
 */
public class WyvernLexerTests {

	public static void lexCommentTest() {
		check("/* a */", MULTI_LINE_COMMENT);
		check("/*/*/", MULTI_LINE_COMMENT);
		check("/***/", MULTI_LINE_COMMENT);
		check("/*\n* this is a multi-line comment!\r\n*/", MULTI_LINE_COMMENT);
		check("// /* x */\n/*\n// a \r\n*/", SINGLE_LINE_COMMENT, MULTI_LINE_COMMENT);
	}
	
	public static void lexTextLiteralTest() {
		check("\"a\"", TEXT_LITERAL);
		check("\"\"", TEXT_LITERAL);
		check("\"\"\" a b c \\\" \"", TEXT_LITERAL, TEXT_LITERAL);
	}
	
	private static List<Symbol> lex(String s) {
		List<Symbol> list = Utils.toList(LEXER.lex(new StringReader(s)));
		return list;
	}
	
	private static List<Symbol> check(String s, SymbolType... expected) {
		List<Symbol> tokens = lex(s);
		StringBuilder sb = new StringBuilder();
		sb.append("Expected:");
		for (int i = 0; i < expected.length; ++i) {
			sb.append(' ').append(expected[i]);
		}
		sb.append(Utils.NL).append("Found:");
		for (int i = 0; i < tokens.size(); ++i) {
			sb.append(' ').append(tokens.get(i));
		}
		String info = sb.toString();
		
		Utils.check(expected.length + 1 == tokens.size(), "Bad token count! " + info);
		for (int i = 0; i < expected.length; ++i) {
			Utils.check(tokens.get(i).type().equals(expected[i]), "Bad type at " + i + ": " + info);
		}
		
		return tokens;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		lexCommentTest();
		lexTextLiteralTest();
		
		System.out.println("All Wyvern Lexer tests passed!");
	}
}
