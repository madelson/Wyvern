/**
 * 
 */
package compiler.wyvern;

import static compiler.wyvern.WyvernLexer.COMMENT_END;
import static compiler.wyvern.WyvernLexer.COMMENT_START;
import static compiler.wyvern.WyvernLexer.COMMENT_TEXT;
import static compiler.wyvern.WyvernLexer.CONTEXT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compiler.Context;
import compiler.Symbol;
import compiler.SymbolType;
import compiler.Utils;
import compiler.parse.Associativity;
import compiler.parse.Grammar;
import compiler.parse.LALRGenerator;
import compiler.parse.Parser;
import compiler.parse.Precedence;
import compiler.parse.PrecedenceFunction;
import compiler.parse.Production;
import compiler.parse.Precedence.ProductionPrecedence;

/**
 * @author Michael
 *
 */
public class WyvernComments {
	// TODO: make the commment nt symbol lexed directly for inline comments once we support inverse sets. The regex is simple: //[^\n]*\n
	public static final SymbolType COMMENT = CONTEXT.getNonTerminalSymbolType("comment"),
			COMMENT_PART_LIST = CONTEXT.getNonTerminalSymbolType("comment-part-list"),
			COMMENT_PART = CONTEXT.getNonTerminalSymbolType("comment-part"),
			INNER_COMMENT = CONTEXT.getNonTerminalSymbolType("inner-comment"),
			COMMENTED_TOKEN = CONTEXT.getNonTerminalSymbolType("commmented-token"),
			COMMENTED_TOKEN_LIST = CONTEXT.getNonTerminalSymbolType("commented-token-list");
	private static final Parser COMMENT_PARSER; 
	
	static {
		Set<Production> productions = new LinkedHashSet<Production>();

		productions.addAll(Production.makeList(COMMENTED_TOKEN_LIST, COMMENTED_TOKEN, null, Context.ListOption.AllowEmpty));
		
		// parse non-commented tokens as commented tokens with or without a comment
		Set<SymbolType> tokenTypes = Utils.set();
		Set<SymbolType> terminalsToIgnore = Utils.set(COMMENT_START, COMMENT_END, COMMENT_TEXT, CONTEXT.eofType());
		for (SymbolType symbolType : CONTEXT.types()) {
			if (symbolType.isTerminal() && !terminalsToIgnore.contains(symbolType)) {
				tokenTypes.add(symbolType);
				productions.add(new Production(COMMENTED_TOKEN, symbolType));
				productions.add(new Production(COMMENTED_TOKEN, COMMENT, symbolType));
			}
		}

		Production commentedTokenCanBeAComment = new Production(COMMENTED_TOKEN, COMMENT); 
		productions.add(commentedTokenCanBeAComment);
		
		// the outer comment is /* ... */
		productions.add(new Production(COMMENT, COMMENT_START, COMMENT_PART_LIST, COMMENT_END));
		
		productions.addAll(Production.makeList(COMMENT_PART_LIST, COMMENT_PART, null, Context.ListOption.AllowEmpty));
		
		// a comment part is either comment text or an inner comment, which has the same structure as the outer comment
		productions.add(new Production(COMMENT_PART, COMMENT_TEXT));
		productions.add(new Production(COMMENT_PART, INNER_COMMENT));
		productions.add(new Production(INNER_COMMENT, COMMENT_START, COMMENT_PART_LIST, COMMENT_END));
		
		// we want outer-comment -> commented-token to have lower precedence than outer-comment token -> commented token
		LinkedHashMap<Set<SymbolType>, Associativity> precedences = new LinkedHashMap<Set<SymbolType>, Associativity>();
		precedences.put(tokenTypes, Associativity.NonAssociative);
		precedences.put(terminalsToIgnore, Associativity.NonAssociative);
		PrecedenceFunction precedence = Precedence.createFunction(precedences, ProductionPrecedence.LeftmostTerminal, Collections.singletonMap(commentedTokenCanBeAComment, COMMENT_START));
		
		Grammar grammar = new Grammar(CONTEXT, "Wyvern comments", COMMENTED_TOKEN_LIST, productions, precedence);
		COMMENT_PARSER = new LALRGenerator().generate(grammar).parser();
	}
	
	/**
	 * Filters out comment constructs from the given list of tokens. The provided map will be filled with mappings from
	 * comments to the tokens they comment. A comment followed by another comment or by EOF will be mapped to a null value.
	 */
	public static List<Symbol> stripComments(List<Symbol> tokens, Map<Symbol, Symbol> commentedTokenMap) {
		List<Symbol> nonComments = new ArrayList<Symbol>();
		Symbol parseTree = COMMENT_PARSER.parse(tokens.iterator()).parseTree();
		
		stripComments(parseTree, nonComments, commentedTokenMap);
		
		// add back EOF
		nonComments.add(Utils.last(tokens));
		
		return nonComments;
	}

	private static void stripComments(Symbol parseTree, List<Symbol> nonComments, Map<Symbol, Symbol> commentedTokenMap) {
		SymbolType type = parseTree.type();
		if (type.equals(COMMENTED_TOKEN_LIST)) {
			for (Symbol symbol : parseTree.children()) {
				stripComments(symbol, nonComments, commentedTokenMap);
			}
		} else if (type.equals(COMMENTED_TOKEN)) {
			// commented token
			if (parseTree.children().size() == 2) {
				commentedTokenMap.put(parseTree.children().get(0), parseTree.children().get(1));
				nonComments.add(parseTree.children().get(1));
			} 
			// lone comment
			else if (parseTree.children().get(0).type().equals(COMMENT)) {
				commentedTokenMap.put(parseTree.children().get(0), null);
			}
			// uncommented token
			else {
				nonComments.add(parseTree.children().get(0));
			}
		} else {
			throw Utils.err("Should never get here!");
		}
	}
}
