package compiler.test;

import static compiler.simplewyvern.SimpleWyvernParser.*;
import static compiler.simplewyvern.SimpleWyvernLexer.*;

import compiler.Utils;

/**
 * 
 * @author Michael
 *
 */
public class SimpleWyvernParserTests {
	public static void main(String[] args) {
		Utils.check(PARSER != null);
		
		System.out.println("All simple wyvern parser tests passed!");
	}
}
