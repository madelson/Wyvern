/**
 * 
 */
package compiler.test;

import static compiler.wyvern.WyvernLexer.*;
import static compiler.wyvern.WyvernParser.*;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import compiler.Symbol;
import compiler.SymbolType;
import compiler.SymbolVisitor;
import compiler.Tuples;
import compiler.Utils;
import compiler.canonicalize.AutoGeneratedSymbolTypeCanonicalizer;
import compiler.parse.Parser;
import compiler.wyvern.WyvernComments;

/**
 * @author Michael
 * 
 */
public class WyvernGrammarTests {
	private static class TestCase extends
			Tuples.Trio<String, String, SymbolType> {
		private TestCase(String program, String programPart, SymbolType partType) {
			super(program, programPart, partType);
		}
		
		@Override
		public String toString() {
			return String.format("in '%s', '%s' is '%s'", this.item1(), this.item2(), this.item3());
		}
	}
	
	private static TestCase make(String program, String programPart, SymbolType partType) {
		return new TestCase(program, programPart, partType);
	}

	private static final List<TestCase> TEST_CASES;

	static {
		List<TestCase> testCases = new ArrayList<TestCase>();

		testCases.add(make("a.b<foo>;", "<foo>", GENERIC_PARAMETERS));
		testCases.add(make("a.b.c;", "a.b.c", EXPRESSION));
		String typeDeclaration = "private sql type Foo { Bar a; Baz b = 7; }";
		testCases.add(make(typeDeclaration, "Bar a;", MEMBER_DECL));
		testCases.add(make(typeDeclaration, "sql", ATTRIBUTE));
		testCases.add(make(typeDeclaration, "Baz b = 7;", MEMBER_DECL));
		
		TEST_CASES = Collections.unmodifiableList(testCases);
	}
	
	public static void parserTests() {
		Utils.check(LEXER != null, "lexer DNE");
		Utils.check(PARSER != null, "parser DNE");
		
		for (final TestCase testCase : TEST_CASES) {
			List<Symbol> tokens = Utils.toList(LEXER
					.lex(new StringReader(testCase.item1())));
			Map<Symbol, Symbol> map = new HashMap<Symbol, Symbol>();
			List<Symbol> withoutComments = WyvernComments.stripComments(tokens, map);
			Parser.Result result = PARSER.parse(withoutComments.iterator());
			
			if (testCase.item2() != null) {
				Utils.check(result.succeeded(), "Parse failed for " + testCase);
				
				SymbolFinder finder = new SymbolFinder(testCase.item2());
				Symbol parseTree = result.parseTree(),
					canonicalParseTree = AutoGeneratedSymbolTypeCanonicalizer.canonicalize(parseTree);
				Symbol match = finder.visit(canonicalParseTree);
				Utils.check(match != null, "Failed to find match for " + testCase);
				Utils.check(match.type().equals(testCase.item3()), "Match was the wrong type for " + testCase + ": was " + match.type());
			}
		}
	}
	
	private static class SymbolFinder extends SymbolVisitor<Symbol> {
		private final String text;
		private Symbol match;
		
		public SymbolFinder(String text) {
			this.text = text;
		}
		
		@Override
		public Symbol visit(Symbol symbol) {
			if (this.match == null && symbol.text().equals(text)) {
				this.match = symbol;
			}
			super.visit(symbol);
			return this.match;
		}
		
	}

	public static void main(String[] args) {
		parserTests();
		
		System.out.println("All wyvern grammar tests passed!");
	}
}
