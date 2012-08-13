/**
 * 
 */
package compiler.test;

import static compiler.simplewyvern.SimpleWyvernLexer.*;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import compiler.Symbol;
import compiler.SymbolType;
import compiler.Utils;

/**
 * @author Michael
 *
 */
public class SimpleWyvernLexerTests {

	private static void test(String text, SymbolType... symbolTypes) {
		List<Symbol> symbols = Utils.toList(LEXER.lex(new StringReader(text)));
		String info = String.format(" (found: %s, expected: %s)", symbols, Arrays.toString(symbolTypes));
		
		for (int i = 0; i < symbolTypes.length; i++) {
			Utils.check(symbols.get(i).type().equals(symbolTypes[i]), symbols.get(i) + " is not of type " + symbolTypes[i] + info);
		}
		Utils.check(Utils.last(symbols).type().equals(CONTEXT.eofType()));
		Utils.check(symbols.size() == symbolTypes.length + 1, String.format("found %s symbols, but expected %s%s", symbols.size(), symbolTypes.length + 1, info));
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		test("1", INT_LITERAL);
		test("1.0", REAL_LITERAL);
		test("'a'", CHAR_LITERAL);
		test("\"a\"", STRING_TERMINATOR, STRING_TEXT, STRING_TERMINATOR);
		test("String.format(\"%sabc\\\"\", -10.07)", TYPE_IDENTIFIER, ACCESS, IDENTIFIER, LPAREN, STRING_TERMINATOR, STRING_TEXT, ESCAPED_CHAR, STRING_TERMINATOR, COMMA, REAL_LITERAL, RPAREN);
		test("abc // hey this is cool\r\n=>", IDENTIFIER, SINGLE_LINE_COMMENT, LAMBDA_OPERATOR);
		
		System.out.println("All simple wyvern lexer tests passed!");
	}

}
