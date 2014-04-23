/**
 * 
 */
package compiler.wyvern;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import compiler.Symbol;

/**
 * @author Michael
 *
 */
public class WyvernComments {	
	/**
	 * Filters out comment constructs from the given list of tokens. The provided map will be filled with mappings from
	 * comments to the tokens they comment.
	 */
	public static List<Symbol> stripComments(List<Symbol> tokens, Map<Symbol, Symbol> commentedTokenMap) {
		List<Symbol> nonComments = new ArrayList<Symbol>();
		
		Symbol lastComment = null;
		for (Symbol token : tokens) {
			if (isComment(token)) {	
				lastComment = token;
			} else {
				if (lastComment != null) {
					commentedTokenMap.put(lastComment, token);
					lastComment = null;	
				}
				nonComments.add(token);
			}
		}
	
		return nonComments;
	}
	
	private static boolean isComment(Symbol token) {
		return token.type().equals(WyvernLexer.SINGLE_LINE_COMMENT)
			|| token.type().equals(WyvernLexer.MULTI_LINE_COMMENT);
	}
}
