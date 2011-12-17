/**
 * 
 */
package compiler.test;

/**
 * @author Michael
 *
 */
public class AllTests {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long start = System.currentTimeMillis();		
		BasicTests.main(args);
		printTime(start);
		
		start = System.currentTimeMillis();
		ParseTests.main(args);
		printTime(start);
		
		start = System.currentTimeMillis();
		CanonicalizeTests.main(args);
		printTime(start);
		
		start = System.currentTimeMillis();
		LexTests.main(args);
		printTime(start);
	}
	
	private static void printTime(long start) {
		System.out.println(String.format("(%s seconds)", (System.currentTimeMillis() - start) / 1000.0));
	}

}
