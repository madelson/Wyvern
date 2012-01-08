package compiler.test;

import java.util.*;

import compiler.*;

public class BasicTests {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Context and symbols
		Context c = new Context();

		SymbolType tt = c.getTerminalSymbolType("token");
		Utils.check(tt.isTerminal());
		Utils.check(tt.name().equals("token"));
		Utils.check(tt.context() == c);
		Utils.check(tt == c.getTerminalSymbolType("token"));

		boolean threw = false;
		try {
			c.getNonTerminalSymbolType("token");
		} catch (Throwable t) {
			threw = true;
		}
		Utils.check(threw);

		Symbol token = tt.createSymbol("text", 1, 1);
		Symbol symbol = c.getNonTerminalSymbolType("symbol").createSymbol(token);
		Utils.check(symbol.children().get(0).equals(token));
		
		token = tt.createSymbol("\nab\n", 3, 5);
		Utils.check(token.endLine() == 4);
		Utils.check(token.endPosition() == 3);
		
		token = tt.createSymbol("a\n\nb", 1, 1);
		Utils.check(token.endLine() == 3);
		Utils.check(token.endPosition() == 1);
		
		// Utils
		Map<String, Map<String, Integer>> map = new HashMap<String, Map<String, Integer>>();
		Utils.check(Utils.put(map, HashMap.class, "a", "b", 3) == null);
		Utils.check(map.get("a").get("b") == 3);
		
		Map<String, Set<String>> map2 = new HashMap<String, Set<String>>();
		Utils.check(Utils.put(map2, HashSet.class, "a", "b"));
		Utils.check(map2.get("a").contains("b"));
		
		Map<String, Set<String>> immutable = Utils.deepImmutableCopy(map2);
		Utils.check(immutable.getClass() == Collections.unmodifiableMap(immutable).getClass());
		Utils.check(immutable.get("a").getClass() == Collections.unmodifiableSet(immutable.get("a")).getClass());
		Utils.check(immutable.size() == 1);
		Utils.check(immutable.get("a").size() == 1);
		
		String[] split = Utils.split("abcdaa", "a");
		Utils.check(Arrays.equals(split, new String[] { "", "bcd", "", "" }));
				
		System.out.println("All basic tests passed!");
	}

}
