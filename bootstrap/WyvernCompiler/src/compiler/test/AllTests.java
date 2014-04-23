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
		long originalStart = System.currentTimeMillis(), start = originalStart;
		BasicTests.main(args);
		printTime(start);
		
		start = System.currentTimeMillis();
		ParseTests.main(args);
		printTime(start);
		
		start = System.currentTimeMillis();
		CanonicalizeTests.main(args);
		printTime(start);
		
		start = System.currentTimeMillis();
		AutomataTests.main(args);
		printTime(start);
		
		start = System.currentTimeMillis();
		LexTests.main(args);
		printTime(start);
		
		start = System.currentTimeMillis();
		LargeGrammarTest.main(args);
		printTime(start);	

		start = System.currentTimeMillis();
		WyvernLexerTests.main(args);
		printTime(start);	
		
//		start = System.currentTimeMillis();
//		SimpleWyvernLexerTests.main(args);
//		printTime(start);
//		
//		start = System.currentTimeMillis();
//		SimpleWyvernParserTests.main(args);
//		printTime(start);	
//		
//		start = System.currentTimeMillis();
//		System.out.println("Skipping Wyvern Grammar tests");
//		//WyvernGrammarTests.main(args);
//		printTime(start);
		
		System.out.println("\nAll tests passed!");
		printTime(originalStart);
	}

	private static void printTime(long start) {
		System.out.println(String.format("(%s seconds)",
				(System.currentTimeMillis() - start) / 1000.0));
	}

}
