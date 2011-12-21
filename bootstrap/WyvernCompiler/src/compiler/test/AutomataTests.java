/**
 * 
 */
package compiler.test;

import compiler.Utils;
import compiler.automata.SetFunction;

/**
 * @author Michael
 * 
 */
public class AutomataTests {
	private static final SetFunction<Character> all = SetFunction.all(),
			empty = SetFunction.empty(), c = SetFunction.singleton('c'),
			C = SetFunction.singleton('C'), az = SetFunction.range('a', 'z'),
			union = az.union(C);

	public static void setFunctionTest() {		
		setFunctionContainsTestCase('c', new boolean[] { true, false, true,
				false, true, true });
		setFunctionContainsTestCase('C', new boolean[] { true, false, false,
				true, false, true });
		setFunctionContainsTestCase('d', new boolean[] { true, false, false,
				false, true, true });
		setFunctionContainsTestCase('D', new boolean[] { true, false, false,
				false, false, false });
	}

	private static void setFunctionContainsTestCase(Character ch,
			boolean[] expectedResults) {
		Utils.check(all.contains(ch) == expectedResults[0]);
		Utils.check(empty.contains(ch) == expectedResults[1]);
		Utils.check(c.contains(ch) == expectedResults[2]);
		Utils.check(C.contains(ch) == expectedResults[3]);
		Utils.check(az.contains(ch) == expectedResults[4]);
		Utils.check(union.contains(ch) == expectedResults[5]);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		setFunctionTest();

		System.out.println("All automata tests passed!");
	}

}
