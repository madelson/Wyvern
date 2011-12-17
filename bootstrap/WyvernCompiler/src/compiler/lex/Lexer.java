package compiler.lex;

import java.io.Reader;
import java.util.*;

import compiler.Symbol;

/**
 * @author madelson
 */
public interface Lexer {
	public static final String DEFAULT_STATE = "DEFAULT_STATE";
	
	/**
	 * Is this lexer compiled instead of dynamically generated?
	 */
	boolean isCompiled();

	/**
	 * Lex the stream into a stream of tokens
	 */
	Iterator<Symbol> lex(Reader reader);
}
