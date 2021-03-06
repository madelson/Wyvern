/**
 * 
 */
package compiler.test;

import java.io.StringReader;
import java.util.*;

import compiler.*;
import compiler.lex.*;
import compiler.parse.*;
import compiler.parse.Precedence.ProductionPrecedence;

/**
 * @author Michael
 * 
 */
public class ParseTests {

	private static void nffTest() {
		Context ctxt = new Context();
		SymbolType a = ctxt.getTerminalSymbolType("a"), c = ctxt
				.getTerminalSymbolType("c"), d = ctxt
				.getTerminalSymbolType("d"), X = ctxt
				.getNonTerminalSymbolType("X"), Y = ctxt
				.getNonTerminalSymbolType("Y"), Z = ctxt
				.getNonTerminalSymbolType("Z");
		Production[] productions = new Production[] { new Production(Z, d),
				new Production(Z, X, Y, Z), new Production(Y),
				new Production(Y, c), new Production(X, Y),
				new Production(X, a) };

		NullableFirstFollow nff = new NullableFirstFollow(
				Arrays.asList(productions));

		Utils.check(nff.nullableSet().equals(Utils.set(X, Y)));

		Utils.check(nff.firstSets().get(X).equals(Utils.set(a, c)));
		Utils.check(nff.firstSets().get(Y).equals(Utils.set(c)));
		Utils.check(nff.firstSets().get(Z).equals(Utils.set(a, c, d)));

		Utils.check(nff.followSets().get(X).equals(Utils.set(a, c, d)));
		Utils.check(nff.followSets().get(Y).equals(Utils.set(a, c, d)));
		Utils.check(nff.followSets().get(Z).equals(Utils.set()));
		
		Utils.check(nff.toString().contains(Utils.NL), "make sure toString() doesn't throw");
	}

	private static final Context c = new Context();
	private static final SymbolType lp = c.getTerminalSymbolType("("), rp = c
			.getTerminalSymbolType(")"), x = c.getTerminalSymbolType("x"),
			comma = c.getTerminalSymbolType(","), plus = c
					.getTerminalSymbolType("+"), star = c
					.getTerminalSymbolType("*"), equals = c
					.getTerminalSymbolType("="), dash = c
					.getTerminalSymbolType("-"), num = c
					.getTerminalSymbolType("1"), uminus = c
					.getTerminalSymbolType("uminus");
	private static final SymbolType S = c.getNonTerminalSymbolType("S"), L = c
			.getNonTerminalSymbolType("L"),
			E = c.getNonTerminalSymbolType("E"), T = c
					.getNonTerminalSymbolType("T"), V = c
					.getNonTerminalSymbolType("V");
	private static final Lexer lexer;

	static {
		CharLexerGenerator clg = new CharLexerGenerator();
		LinkedHashSet<LexerAction> actions = new LinkedHashSet<LexerAction>();
		for (SymbolType type : Utils.set(lp, rp, x, comma, plus, star, equals,
				dash, num))
			actions.add(LexerAction.lexToken(type.name(), type));
		lexer = clg.generate(c, actions).lexer();
	}

	private static class LRResultInfo {
		public final int entryCount, acceptCount, shiftCount, gotoCount,
				reduceCount;

		public LRResultInfo(LRGenerator.Result result) {
			int entryCount = 0, acceptCount = 0, shiftCount = 0, gotoCount = 0, reduceCount = 0;

			for (Map.Entry<LRGenerator.State, Map<SymbolType, Object>> e : result
					.parseTable().entrySet()) {
				entryCount += e.getValue().size();
				for (SymbolType type : e.getValue().keySet()) {
					Object action = e.getValue().get(type);
					if (action instanceof LRGenerator.Edge) {
						if (type.isTerminal())
							shiftCount++;
						else
							gotoCount++;
					} else if (action instanceof LRGenerator.Reduction)
						reduceCount++;
					else
						acceptCount++;
				}
			}

			this.entryCount = entryCount;
			this.acceptCount = acceptCount;
			this.shiftCount = shiftCount;
			this.gotoCount = gotoCount;
			this.reduceCount = reduceCount;
		}

		public void check(boolean exact, int expectedEntryCount,
				int expectedAcceptCount, int expectedShiftCount,
				int expectedGotoCount, int expectedReduceCount) {
			int[] actuals = { this.entryCount, this.acceptCount,
					this.shiftCount, this.gotoCount, this.reduceCount }, expecteds = {
					expectedEntryCount, expectedAcceptCount,
					expectedShiftCount, expectedGotoCount, expectedReduceCount };
			String[] texts = { "total entry", "accept     ", "shift      ",
					"goto       ", "reduce     " };

			StringBuilder sb = new StringBuilder();
			boolean succeeded = true;
			for (int i = 0; i < actuals.length; i++) {
				boolean checkSucceeded = exact ? actuals[i] == expecteds[i]
						: actuals[i] <= expecteds[i];
				succeeded &= checkSucceeded;
				sb.append(Utils.NL).append(
						String.format("%s %s: (%d / %d)", checkSucceeded ? " "
								: "!", texts[i], actuals[i], expecteds[i]));
			}

			Utils.check(succeeded, sb.toString());
		}
	}

	public static void productionTest() {
		Production production = new Production(S, S, lp, x, x, lp, L, L, x, rp,
				L);
		Utils.check(production.rightmostTerminalSymbolType().equals(rp),
				"Bad rightmost type!");
		Utils.check(production.leftmostTerminalSymbolType().equals(lp),
				"Bad leftmost type!");
	}

	public static void check320(ParserGenerator generator, boolean expected) {
		Set<Production> productions = Utils.set(new Production(S, lp, L, rp),
				new Production(S, x), new Production(L, S), new Production(L,
						L, comma, S));
		Grammar g = new Grammar(c, "3.20", S, productions,
				Precedence.defaultFunction());

		ParserGenerator.Result rawResult = generator.generate(g);

		if (!expected) {
			Utils.check(!rawResult.succeeded(), "Shouldn't have succeeded!");
			return;
		}

		Utils.check(rawResult.succeeded(), "Did not succeed!");

		if (generator instanceof LRGenerator) {
			LRGenerator.Result result = (LRGenerator.Result) rawResult;
			LRResultInfo info = new LRResultInfo(result);

			// table check invalid with a larger number of states
			if (result.dfaStates().size() <= 9)
				info.check(generator.getClass() == LR0Generator.class, 33, 1,
						8, 4, 20);

			if (generator.getClass() == LR0Generator.class) {
				Utils.check(result.dfaStates().size() == 9, "Bad state count!");
				Utils.check(result.dfaStartState().items().size() == 3,
						"Bad item count in start state!");
				Utils.check(result.dfaEdges().size() == 12, "Bad edge count!");
			}
		}

		String program = "(x,(x,x))";
		Symbol root = rawResult.parser()
				.parse(lexer.lex(new StringReader(program))).parseTree();
		Utils.check(root.type().equals(g.startSymbolType()), "Bad root type!");
		Utils.check(root.children().size() == 3, "Bad root child count");
		Utils.check(root.children().get(0).type().equals(lp),
				"Bad first root child!");
		Utils.check(root.children().get(1).type().equals(L),
				"Bad second root child!");
		Utils.check(root.children().get(2).type().equals(rp),
				"Bad third root child!");
		Utils.check(root.text().equals(program), "Bad text!");
	}

	public static void check323(ParserGenerator generator, boolean expected) {
		Set<Production> productions = Utils.set(new Production(E, T),
				new Production(E, T, plus, E), new Production(T, x));
		Grammar g = new Grammar(c, "3.23", E, productions,
				Precedence.defaultFunction());

		ParserGenerator.Result rawResult = generator.generate(g);

		if (!expected) {
			Utils.check(!rawResult.succeeded(), "Shouldn't have succeeded!");
			return;
		}

		Utils.check(rawResult.succeeded(), "Did not succeed!");

		if (generator instanceof LRGenerator) {
			LRGenerator.Result result = (LRGenerator.Result) rawResult;
			LRResultInfo info = new LRResultInfo(result);

			// table check invalid with a larger number of states
			if (result.dfaStates().size() <= 6)
				info.check(generator.getClass() == SLRGenerator.class, 12, 1,
						3, 4, 4);

			if (generator.getClass() == SLRGenerator.class) {
				Utils.check(result.dfaStates().size() == 6, "Bad state count!");
				Utils.check(result.dfaStartState().items().size() == 4,
						"Bad item count in start state!");
				Utils.check(result.dfaEdges().size() == 7, "Bad edge count!");
			}
		}

		String program = "x+x+x+x+x+x+x+x";
		Symbol root = rawResult.parser()
				.parse(lexer.lex(new StringReader(program))).parseTree();
		Utils.check(root.type().equals(g.startSymbolType()), "Bad root type!");
		Utils.check(root.children().size() == 3, "Bad root child count");
		Utils.check(root.children().get(0).type().equals(T),
				"Bad first root child!");
		Utils.check(root.children().get(1).type().equals(plus),
				"Bad second root child!");
		Utils.check(root.children().get(2).type().equals(E),
				"Bad third root child!");
		Utils.check(root.text().equals(program), "Bad text!");
	}

	public static void check326(ParserGenerator generator, boolean expected) {
		Set<Production> productions = Utils.set(
				new Production(S, V, equals, E), new Production(S, E),
				new Production(E, V), new Production(V, x), new Production(V,
						star, E));
		Grammar g = new Grammar(c, "3.26", S, productions,
				Precedence.defaultFunction());

		ParserGenerator.Result rawResult = generator.generate(g);

		if (!expected) {
			Utils.check(!rawResult.succeeded(), "Shouldn't have succeeded!");
			return;
		}

		Utils.check(rawResult.succeeded(), "Did not succeed!");

		if (generator instanceof LRGenerator) {
			LRGenerator.Result result = (LRGenerator.Result) rawResult;
			LRResultInfo info = new LRResultInfo(result);

			// table check invalid with a larger number of states
			if (result.dfaStates().size() <= 14)
				info.check(generator.getClass() == LR1Generator.class, 31, 1,
						9, 9, 12);

			if (generator.getClass() == LR1Generator.class) {
				Utils.check(result.dfaStates().size() == 14, "Bad state count!");
				Utils.check(result.dfaStartState().items().size() == 8,
						"Bad item count in start state!");
				Utils.check(result.dfaEdges().size() == 18, "Bad edge count!");
			}

			// table check invalid with a larger number of states
			if (result.dfaStates().size() <= 10)
				info.check(generator.getClass() == LALRGenerator.class, 24, 1,
						7, 7, 9);

			if (generator.getClass() == LALRGenerator.class) {
				Utils.check(result.dfaStates().size() == 10, "Bad state count!");
				Utils.check(result.dfaStartState().items().size() == 8,
						"Bad item count in start state!");
				Utils.check(result.dfaEdges().size() == 14, "Bad edge count!");
			}
		}

		String program = "**x=***x";
		Symbol root = rawResult.parser()
				.parse(lexer.lex(new StringReader(program))).parseTree();
		Utils.check(root.type().equals(g.startSymbolType()), "Bad root type!");
		Utils.check(root.text().equals(program), "Bad text!");
	}

	public static void checkAssociativity(ParserGenerator generator,
			boolean expected) {
		Set<Production> productions = Utils.set(new Production(E, E, plus, E),
				new Production(E, x));
		String program = "x+x+x";
		boolean leftSucceeded = false, rightSucceeded = false;

		LinkedHashMap<Set<SymbolType>, Associativity> assoc = new LinkedHashMap<Set<SymbolType>, Associativity>();
		Map<Production, SymbolType> overrides = Collections.emptyMap();

		// left
		assoc.put(Collections.singleton(plus), Associativity.Left);
		PrecedenceFunction precedence = Precedence.createFunction(assoc,
				ProductionPrecedence.LeftmostTerminal, overrides);
		Grammar g = new Grammar(c, "left", E, productions, precedence);

		ParserGenerator.Result result = generator.generate(g);
		if (leftSucceeded = result.succeeded()) {
			Symbol root = result.parser()
					.parse(lexer.lex(new StringReader(program))).parseTree();
			Utils.check(root.type() == E);
			Utils.check(root.children().size() == 3);
			Utils.check(root.children().get(0).children().size() == 3);
			Utils.check(root.children().get(2).children().size() == 1);
		}

		// right
		assoc.put(Collections.singleton(plus), Associativity.Right);
		precedence = Precedence.createFunction(assoc,
				ProductionPrecedence.LeftmostTerminal, overrides);
		g = new Grammar(c, "right", E, productions, precedence);

		result = generator.generate(g);
		if (rightSucceeded = result.succeeded()) {
			Symbol root = result.parser()
					.parse(lexer.lex(new StringReader(program))).parseTree();
			Utils.check(root.type() == E);
			Utils.check(root.children().size() == 3);
			Utils.check(root.children().get(0).children().size() == 1);
			Utils.check(root.children().get(2).children().size() == 3);
		}

		if (!expected) {
			Utils.check(!leftSucceeded && !rightSucceeded,
					"Shouldn't have succeeded!");
		} else {
			Utils.check(leftSucceeded, "Left test failed!");
			Utils.check(rightSucceeded, "Right test failed!");
		}

		// non-associative
		assoc.put(Collections.singleton(plus), Associativity.NonAssociative);
		precedence = Precedence.createFunction(assoc,
				ProductionPrecedence.LeftmostTerminal, overrides);
		g = new Grammar(c, "non-associative", E, productions, precedence);

		result = generator.generate(g);
		Utils.check(!result.succeeded(), "Should have failed!");
	}

	public static void check335(ParserGenerator generator, boolean expected) {
		Production unaryMinus = new Production(E, dash, E);
		Set<Production> productions = Utils.set(new Production(E, num),
				new Production(E, E, star, E), new Production(E, E, plus, E),
				new Production(E, E, dash, E), unaryMinus);
		Grammar g = new Grammar(c, "3.5", E, productions,
				Precedence.defaultFunction());

		ParserGenerator.Result rawResult = generator.generate(g);
		Utils.check(!rawResult.succeeded(),
				"Ambiguous: shouldn't have succeeded!");

		LinkedHashMap<Set<SymbolType>, Associativity> precedence = new LinkedHashMap<Set<SymbolType>, Associativity>();
		precedence.put(Utils.set(plus, dash), Associativity.Left);
		precedence.put(Utils.set(star), Associativity.Left);
		precedence.put(Utils.set(uminus), Associativity.Left);

		g = new Grammar(c, "3.5 with precedence", E, productions,
				Precedence.createFunction(precedence,
						ProductionPrecedence.LeftmostTerminal,
						Collections.singletonMap(unaryMinus, uminus)));
		rawResult = generator.generate(g);
		if (!expected) {
			Utils.check(!rawResult.succeeded(), "Shouldn't have succeeded!");
		}

		Utils.check(rawResult.succeeded());
		String program = "1-1+1*1+1*-1--1";
		Symbol root = rawResult.parser()
				.parse(lexer.lex(new StringReader(program))).parseTree();
		Utils.check(program.equals(root.text()));
	}

	public static void makeListTest(ParserGenerator generator, boolean expected) {
		SymbolType sep = comma, el = x, list = L;
		List<Production> productions;
		String single = "x", separatedListNoTrailingSeparator = "x,x,x,x", separatedListTrailingSeparator = "x,x,x,", unseparatedList = "xxxx", justSeparator = ",", leadingSeparator = ",x,x";
		Map<String, Boolean> map = new HashMap<String, Boolean>();
		map.put(single, true);
		map.put(justSeparator, false);
		map.put(leadingSeparator, false);
		
		// list with separator, can be empty, can have trailing
		productions = Production.makeList(list, el, sep, Context.ListOption.AllowEmpty, Context.ListOption.AllowTrailingSeparator);
		map.put("", true);
		map.put(separatedListNoTrailingSeparator, true);
		map.put(separatedListTrailingSeparator, true);
		map.put(unseparatedList, false);		
		testParser(generator, list, productions, map, expected);
		
		// list with separator, can be empty, cannot have trailing
		productions = Production.makeList(list, el, sep, Context.ListOption.AllowEmpty);
		map.put("", true);
		map.put(separatedListNoTrailingSeparator, true);
		map.put(separatedListTrailingSeparator, false);
		map.put(unseparatedList, false);		
		testParser(generator, list, productions, map, expected);

		// list with separator, cannot be empty, can have trailing
		productions = Production.makeList(list, el, sep, Context.ListOption.AllowTrailingSeparator);
		map.put("", false);
		map.put(separatedListNoTrailingSeparator, true);
		map.put(separatedListTrailingSeparator, true);
		map.put(unseparatedList, false);		
		testParser(generator, list, productions, map, expected);
		
		// list with separator, cannot be empty, cannot have trailing
		productions = Production.makeList(list, el, sep);
		map.put("", false);
		map.put(separatedListNoTrailingSeparator, true);
		map.put(separatedListTrailingSeparator, false);
		map.put(unseparatedList, false);		
		testParser(generator, list, productions, map, expected);
		
		// list with no separator, can be empty
		productions = Production.makeList(list, el, null, Context.ListOption.AllowEmpty);
		map.put("", true);
		map.put(separatedListNoTrailingSeparator, false);
		map.put(separatedListTrailingSeparator, false);
		map.put(unseparatedList, true);		
		testParser(generator, list, productions, map, expected);

		// list with no separator, cannot be empty
		productions = Production.makeList(list, el, null);
		map.put("", false);
		map.put(separatedListNoTrailingSeparator, false);
		map.put(separatedListTrailingSeparator, false);
		map.put(unseparatedList, true);		
		testParser(generator, list, productions, map, expected);
	}
	
	public static void makeOptionTest(ParserGenerator generator, boolean expected) {
		List<Production> productions;
		String px = "x", pstar = "*", pxx = "xx", pxstar = "x*", pstarx = "*x", pstarstar = "**";  
		Map<String, Boolean> map = new HashMap<String, Boolean>();
		
		productions = Utils.toList(Arrays.asList(new Production[] { new Production(S, c.optional(x), c.optional(star)) }));
		productions.addAll(Production.makeOption(x));
		productions.addAll(Production.makeOption(star));
		map.put("", true);
		map.put(px, true);
		map.put(pstar, true);
		map.put(pxx, false);
		map.put(pxstar, true);
		map.put(pstarx, false);
		map.put(pstarstar, false);
		testParser(generator, S, productions, map, expected);
	}
	
	public static void makeOneOfTest(ParserGenerator generator, boolean expected) {
		Collection<Production> productions;
		Map<String, Boolean> map = new HashMap<String, Boolean>();
		
		productions = Production.makeOneOf(x, star);
		map.put("", false);
		map.put(",", false);
		map.put("x", true);
		map.put("*", true);
		map.put("x*", false);
		map.put("xx", false);
		testParser(generator, c.oneOf(x, star), productions, map, expected);
	}

	public static void makeTupleTest(ParserGenerator generator, boolean expected) {
		Collection<Production> productions;
		Map<String, Boolean> map = new HashMap<String, Boolean>();
		
		productions = Production.makeTuple(x, star);
		map.put("", false);
		map.put(",", false);
		map.put("x", false);
		map.put("*", false);
		map.put("x*", true);
		map.put("xx", false);
		map.put("*x", false);
		map.put("xx*", false);
		map.put("x**", false);
		testParser(generator, c.tuple(x, star), productions, map, expected);
	}

	public static void testMethodCallGrammar(ParserGenerator generator, boolean expected) {
		CheckedProductionSet productions = new CheckedProductionSet();
		productions.add(new Production(E, x));
		productions.add(new Production(L, x, lp, c.listOf(E, comma, Context.ListOption.AllowEmpty), rp));
		productions.add(new Production(E, L));
		productions.add(new Production(E, E, plus, x));
		productions.add(new Production(E, E, plus, L));
		
		Map<String, Boolean> map = new LinkedHashMap<String, Boolean>();
		map.put("x", true);
		map.put("x+x", true);
		map.put("x+x+x", true);
		map.put("x()", true);
		map.put("x+x()", true);
		map.put("x+x+x()", true);
		map.put("(x)", false);
		map.put("x()+x", true);
		map.put("x(x)", true);
		map.put("x(x,x)", true);
		map.put("x+x(x,x+x())", true);
		map.put("xx", false);
		testParser(generator, E, productions, map, expected);
	}
	
	private static void testParser(ParserGenerator generator,
			SymbolType startSymbol, Collection<Production> productions,
			Map<String, Boolean> programStrings, boolean expected) {
		Grammar g = new Grammar(c, "test", startSymbol, productions,
				Precedence.defaultFunction());
		ParserGenerator.Result result = generator.generate(g);
//		if (startSymbol == S) {
//			LRGenerator.Result res = (LRGenerator.Result) result;
//			for (Object o : res.dfaStates()) {
//				System.out.println(o);
//			}
//			for (String e : res.errors()) {
//				System.out.println(e);
//			}
//		}
		Utils.check(expected == (result.parser() != null));
		if (!expected) {
			return;
		}
		
		Parser parser = result.parser();
		for (String prog : programStrings.keySet()) {
			boolean succeeded = false;
			try {
				Parser.Result parserResult = parser.parse(lexer.lex(new StringReader(prog)));
				succeeded = parserResult.parseTree() != null;
				if (succeeded) {
					Symbol root = parserResult.parseTree();
					Utils.check(prog.equals(root.text()));
				}
			} catch (Exception ex) {
				succeeded = false;
			}
			Utils.check(succeeded == programStrings.get(prog), prog + " succeeded = " + succeeded);
		}		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		productionTest();
		nffTest();

		LRGenerator lr0 = new LR0Generator(), slr = new SLRGenerator(), lr1 = new LR1Generator(), lalr = new LALRGenerator();

		check320(lr0, true);
		check320(slr, true);
		check320(lr1, true);
		check320(lalr, true);

		check323(lr0, false);
		check323(slr, true);
		check323(lr1, true);
		check323(lalr, true);

		check326(lr0, false);
		check326(slr, false);
		check326(lr1, true);
		check326(lalr, true);

		checkAssociativity(lr0, true);
		checkAssociativity(slr, true);
		checkAssociativity(lr1, true);
		checkAssociativity(lalr, true);

		check335(lr0, true);
		check335(slr, true);
		check335(lr1, true);
		check335(lalr, true);
		
		makeListTest(lr0, false);
		makeListTest(slr, true);
		makeListTest(lalr, true);
		makeListTest(lr1, true);
		
		makeOptionTest(lr0, false);
		makeOptionTest(slr, true);
		makeOptionTest(lalr, true);
		makeOptionTest(lr1, true);
		
		makeOneOfTest(lr0, true);
		makeOneOfTest(slr, true);
		makeOneOfTest(lalr, true);
		makeOneOfTest(lr1, true);		
		
		makeTupleTest(lr0, true);
		makeTupleTest(slr, true);
		makeTupleTest(lalr, true);
		makeTupleTest(lr1, true);	
		
		testMethodCallGrammar(lr0, false);
		testMethodCallGrammar(slr, true);
		testMethodCallGrammar(lalr, true);
		testMethodCallGrammar(lr1, true);

		System.out.println("All parse tests passed!");
	}

}
