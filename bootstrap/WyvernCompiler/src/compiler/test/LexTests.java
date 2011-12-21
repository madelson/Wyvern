/**
 * 
 */
package compiler.test;

import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import compiler.Context;
import compiler.Symbol;
import compiler.SymbolType;
import compiler.Utils;
import compiler.lex.CharLexerGenerator;
import compiler.lex.Lexer;
import compiler.lex.LexerAction;
import compiler.lex.Regex;

/**
 * @author Michael
 * 
 */
public class LexTests {
	public static void charLexerGeneratorTest() {
		Context c = new Context();
		SymbolType a = c.getTerminalSymbolType("X"), b = c
				.getTerminalSymbolType("Y"), any = c
				.getTerminalSymbolType("ANY_CHAR");

		LinkedHashSet<LexerAction> actions = new LinkedHashSet<LexerAction>();
		actions.add(LexerAction.lexToken("a", a));
		actions.add(LexerAction.lexToken("b", b));
		actions.add(LexerAction.enter(Utils.set(Lexer.DEFAULT_STATE, "inside"),
				"(", null, "inside"));
		actions.add(LexerAction.leave(Utils.set("inside"), ")", null));
		actions.add(LexerAction.lexToken(
				Utils.set(Lexer.DEFAULT_STATE, "inside"), "", any));

		Lexer lexer = new CharLexerGenerator().generate(c, actions).lexer();

		String text = "abbaXXXbabaX";
		Reader reader = new StringReader(text);
		Iterator<Symbol> it = lexer.lex(reader);
		int i = 0;
		while (it.hasNext()) {
			Symbol s = it.next();
			if (s.type() == c.eofType())
				Utils.check(i == text.length());
			else if (s.type() == a)
				Utils.check(text.charAt(i) == 'a');
			else if (s.type() == b)
				Utils.check(text.charAt(i) == 'b');
			else
				Utils.check(s.type() == any);
			i++;
		}

		text = "abX(abab)b(())bbbXa(X)";
		reader = new StringReader(text);
		List<Symbol> tokens = Utils.toList(lexer.lex(reader));
		Utils.check(tokens.size() == text.replaceAll("\\(", "")
				.replaceAll("\\)", "").length() + 1);
		SymbolType[] types = { a, b, any, any, any, any, any, b, b, b, b, any,
				a, any, c.eofType() };
		for (i = 0; i < types.length; i++)
			Utils.check(tokens.get(i).type().equals(types[i]), i + ": "
					+ tokens.get(i).type() + " != " + types[i]);

	}

	public static void basicRegexTest() {
		Symbol parseTree = Regex.canonicalize(Regex.parse("a").parseTree());
		Utils.check(parseTree.children().get(0).children().get(0).type()
				.equals(Regex.CHAR));

		String regex = "[\\-\\+]?[0-9]*\\.?[0-9]+([eE][\\-\\+]?[0-9]+)?";
		parseTree = Regex.canonicalize(Regex.parse(regex).parseTree());
		Utils.check(parseTree.type().equals(Regex.REGEX_LIST));
		Utils.check(parseTree.children().size() == 5);
		Utils.check(parseTree.children().get(0).children().get(0).children()
				.get(1).type().equals(Regex.SET_LIST));

		regex = "\\\\.";
		parseTree = Regex.canonicalize(Regex.parse(regex).parseTree());
		Utils.check(parseTree.type().equals(Regex.REGEX_LIST));
		Utils.check(parseTree.children().get(0).children().get(0).type()
				.equals(Regex.ESCAPED));
		Utils.check(parseTree.children().get(1).children().get(0).type()
				.equals(Regex.WILDCARD));
	}

	public static void regexNfaTest() {
		LinkedHashSet<Regex.DfaState> stateCollection = Utils.set();
		Set<Regex.DfaEdge> edgeCollection = Utils.set();

		createNfa("a", stateCollection, edgeCollection);
		checkNfa(stateCollection, edgeCollection, 3, 2, 1);

		createNfa("", stateCollection, edgeCollection);
		checkNfa(stateCollection, edgeCollection, 3, 2, 2);

		createNfa("a|b", stateCollection, edgeCollection);
		checkNfa(stateCollection, edgeCollection, 6, 6, 4);

		createNfa("ab", stateCollection, edgeCollection);
		checkNfa(stateCollection, edgeCollection, 4, 3, 1);

		createNfa("a*", stateCollection, edgeCollection);
		checkNfa(stateCollection, edgeCollection, 4, 4, 3);

		createNfa("a+", stateCollection, edgeCollection);
		checkNfa(stateCollection, edgeCollection, 5, 5, 3);
		
		createNfa("a?", stateCollection, edgeCollection);
		checkNfa(stateCollection, edgeCollection, 6, 6, 5);

		createNfa("[abc]", stateCollection, edgeCollection);
		checkNfa(stateCollection, edgeCollection, 4, 5, 2);
		
		createNfa("[a-z]", stateCollection, edgeCollection);
		checkNfa(stateCollection, edgeCollection, 4, 3, 2);
		
		createNfa("\\n", stateCollection, edgeCollection);
		checkNfa(stateCollection, edgeCollection, 3, 2, 1);		
	}

	private static Regex.DfaState createNfa(String regex,
			LinkedHashSet<Regex.DfaState> stateCollection,
			Set<Regex.DfaEdge> edgeCollection) {
		stateCollection.clear();
		edgeCollection.clear();

		Symbol parseTree = Regex.canonicalize(Regex.parse(regex).parseTree());
		System.out.println("Parse tree: " + parseTree);

		return Regex.nfaFor(new Context().getTerminalSymbolType("TOKEN"),
				parseTree, stateCollection, edgeCollection);
	}

	private static void checkNfa(LinkedHashSet<Regex.DfaState> stateCollection,
			Set<Regex.DfaEdge> edgeCollection, int expectedStateCount,
			int expectedEdgeCount, int expectedEpsilonEdgeCount) {
		System.out.println(stateCollection);
		System.out.println(edgeCollection);
		System.out.println();
		Utils.check(stateCollection.size() == expectedStateCount,
				"Bad state count!");
		Utils.check(edgeCollection.size() == expectedEdgeCount,
				"Bad edge count!");

		int epsilonEdgeCount = 0;
		for (Regex.DfaEdge edge : edgeCollection) {
			if (edge.character() == null) {
				epsilonEdgeCount++;
			}
		}
		Utils.check(epsilonEdgeCount == expectedEpsilonEdgeCount,
				"Bad epsilon edge count!");

		int acceptCount = 0;
		for (Regex.DfaState state : stateCollection) {
			if (state.symbolType() != null) {
				acceptCount++;
			}
		}
		Utils.check(acceptCount == 1, "Bad accept count!");
	}

	public static void main(String[] args) {
		charLexerGeneratorTest();

		basicRegexTest();

		regexNfaTest();

		System.out.println("All lex tests passed!");
	}
}
