/**
 * 
 */
package compiler.test;

import static compiler.wyvern.WyvernLexer.*;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import compiler.Context;
import compiler.Symbol;
import compiler.SymbolType;
import compiler.Tuples;
import compiler.Utils;
import compiler.wyvern.WyvernComments;
import compiler.wyvern.WyvernLexer;

/**
 * @author Michael
 * 
 */
public class WyvernGrammarTests {
	private static class TestCase extends
			Tuples.Trio<String, SymbolType[], SymbolType> {
		public TestCase(String item1, SymbolType[] item2, SymbolType item3) {
			super(item1, item2, item3);
		}

		public static TestCase make(String program, SymbolType overallType,
				SymbolType... tokenTypes) {
			return new TestCase(program, tokenTypes.length > 0 ? tokenTypes
					: null, overallType);
		}
	}

	private static final List<TestCase> TEST_CASES;

	static {
		List<TestCase> testCases = new ArrayList<TestCase>();

		// lexical tests
		testCases.addAll(Utils.set(TestCase.make("int", null, INT_ALIAS),
				TestCase.make("obj 2 + 20.25 i280.2", null, OBJECT_ALIAS, INT,
						PLUS, REAL, WyvernLexer.IDENTIFIER, WyvernLexer.REAL),
				TestCase.make("'a''\\n'/*x/**/*/", null, WyvernLexer.CHAR,
						WyvernLexer.CHAR, WyvernLexer.COMMENT_START,
						WyvernLexer.COMMENT_TEXT, WyvernLexer.COMMENT_START,
						WyvernLexer.COMMENT_END, WyvernLexer.COMMENT_END),
				TestCase.make("\n\n\t \" \\\"\"", null,
						WyvernLexer.STRING_TERMINATOR, WyvernLexer.STRING_TEXT,
						WyvernLexer.ESCAPE, WyvernLexer.STRING_TEXT,
						WyvernLexer.STRING_TERMINATOR), TestCase.make(
						"a.b.C, str", null, WyvernLexer.IDENTIFIER,
						WyvernLexer.ACCESS, WyvernLexer.IDENTIFIER,
						WyvernLexer.ACCESS, WyvernLexer.TYPE_NAME,
						WyvernLexer.COMMA, WyvernLexer.STRING_ALIAS)));

		TEST_CASES = Collections.unmodifiableList(testCases);
	}

	public static void lexerTests() {
		Utils.check(WyvernLexer.LEXER != null, "lexer DNE");

		for (TestCase testCase : TEST_CASES) {
			if (testCase.item2() != null) {
				List<Symbol> symbols = Utils.toList(WyvernLexer.LEXER
						.lex(new StringReader(testCase.item1())));
				List<SymbolType> types = new ArrayList<SymbolType>(
						symbols.size());
				for (Symbol symbol : symbols) {
					if (!symbol.type().equals(WyvernLexer.CONTEXT.eofType())) {
						types.add(symbol.type());
					}
				}

				Utils.check(types.equals(Arrays.asList(testCase.item2())),
						types + " != " + Arrays.asList(testCase.item2()));
			}
		}
	}

	public static void commentTests() {
		String prog;
		Map<Symbol, Symbol> comments;
		List<Symbol> tokens;

		prog = "/*text*/int a";
		comments = new LinkedHashMap<Symbol, Symbol>();
		tokens = WyvernComments.stripComments(
				Utils.toList(WyvernLexer.LEXER.lex(new StringReader(prog))),
				comments);
		Utils.check(tokens.size() == 3);
		Utils.check(tokens.get(0).type().equals(INT_ALIAS));
		Utils.check(comments.size() == 1);
		Utils.check(comments.values().iterator().next() == tokens.get(0));

		prog = "/**//*/*/text**/int a*/";
		comments = new LinkedHashMap<Symbol, Symbol>();
		tokens = WyvernComments.stripComments(
				Utils.toList(WyvernLexer.LEXER.lex(new StringReader(prog))),
				comments);
		Utils.check(tokens.size() == 1);
		Utils.check(tokens.get(0).type().equals(CONTEXT.eofType()));
		Utils.check(comments.size() == 2);
		Utils.check(comments.values().iterator().next() == null);
		Utils.check(comments.keySet().iterator().next().text().equals("/**/"));

	}

	public static void main(String[] args) {
		lexerTests();

		commentTests();

		System.out.println("All wyvern grammar tests passed!");
	}
}
