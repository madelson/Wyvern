/**
 * 
 */
package compiler.test;

import java.util.Collections;

import compiler.*;
import compiler.canonicalize.Canonicalize;

/**
 * @author Michael
 * 
 */
public class CanonicalizeTests {
	public static void testFlattenLists() {
		Context c = new Context();
		SymbolType list = c.getNonTerminalSymbolType("list"), comma = c
				.getTerminalSymbolType(","), exp = c
				.getNonTerminalSymbolType("exp");

		Symbol sep = comma.createSymbol(",", 1, 1), e = exp.createSymbol();
		Symbol parseTree = exp.createSymbol(
				e,
				list.createSymbol(
						exp.createSymbol(),
						sep,
						list.createSymbol(
								exp.createSymbol(list.createSymbol(e,
										list.createSymbol())), sep,
								list.createSymbol(e))), e);
		
		// test removing separators
		Symbol flattened = Canonicalize.flattenLists(parseTree, Collections.singletonMap(list, comma), true);
		
		Utils.check(flattened.children().size() == 3);
		Symbol listSymbol = flattened.children().get(1);
		Utils.check(listSymbol.type().equals(list));
		Utils.check(listSymbol.children().size() == 3);
		Symbol innerList = listSymbol.children().get(1).children().get(0);
		Utils.check(innerList.type().equals(list));
		Utils.check(innerList.children().size() == 1);
		Utils.check(innerList.children().get(0) == e);
		
		// test keeping separators
		flattened = Canonicalize.flattenLists(parseTree, Collections.singletonMap(list, comma), false);
		listSymbol = flattened.children().get(1);
		Utils.check(listSymbol.type().equals(list));
		Utils.check(listSymbol.children().size() == 5);
		Utils.check(listSymbol.children().get(1) == sep && listSymbol.children().get(3)== sep);
		innerList = listSymbol.children().get(2).children().get(0);
		Utils.check(innerList.type().equals(list));
		Utils.check(innerList.children().size() == 1);
		Utils.check(innerList.children().get(0) == e);		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		testFlattenLists();

		System.out.println("All canonicalize tests passed!");
	}

}
