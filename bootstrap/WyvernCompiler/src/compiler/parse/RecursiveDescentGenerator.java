/**
 * 
 */
package compiler.parse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import compiler.Symbol;
import compiler.SymbolType;
import compiler.Tuples;
import compiler.Utils;

/**
 * @author mikea_000
 *
 */
public class RecursiveDescentGenerator implements ParserGenerator {

	/* (non-Javadoc)
	 * @see compiler.parse.ParserGenerator#generate(compiler.parse.Grammar)
	 */
	@Override
	public Result generate(final Grammar grammar) {	
		// Build the LL(1) predictive table. Unlike true LL(1), however, we'll try all valid productions instead
		// of becoming stuck when we can't "predict" the right one. this table tells which productions 
		// X -> a are valid to attempt for a next symbol s. A production is valid if s is in FIRST(a) or if a is nullable
		// and s is in FOLLOW(a)
		final Map<SymbolType, Map<SymbolType, List<Production>>> parseTable = new HashMap<SymbolType, Map<SymbolType, List<Production>>>();
		for (Production production : grammar.productions()) {
			if (production.childTypes().isEmpty()) {
				// X -> nothing should be valid for any token type
				for (SymbolType tokenType : grammar.terminalSymbolTypes()) {
					addToParseTable(parseTable, tokenType, production);
				}
			}
			else {
				for (SymbolType firstType : grammar.nff().first(production.childTypes())) {
					addToParseTable(parseTable, firstType, production);
				}
				
				boolean isNullable = true;
				for (SymbolType childType : production.childTypes()) {
					if (!grammar.nff().nullableSet().contains(childType)) {
						isNullable = false;
						break;
					}
				}
				if (isNullable) {
					for (SymbolType followType : grammar.nff().followSets().get(Utils.last(production.childTypes()))) {
						addToParseTable(parseTable, followType, production);
					}
				}	
			}			
		}
		
		final Parser parser = new Parser() {

			@Override
			public boolean isCompiled() {
				return false;
			}

			@Override
			public Result parse(Iterator<Symbol> tokenStream) {
				List<Symbol> tokens = Utils.toList(tokenStream);
				
				ParserInstance instance = new ParserInstance(parseTable, tokens);
				final Symbol parseTree = instance.tryParse(grammar.startSymbolType());
				
				return new Result() {

					@Override
					public List<String> errors() {
						return parseTree == null 
							? Arrays.asList("Failed to parse")
							: Collections.<String>emptyList();
					}

					@Override
					public List<String> warnings() {
						return Collections.emptyList();
					}

					@Override
					public Symbol parseTree() {
						return parseTree;
					}
					
				};
			}
			
		};
		
		return new Result() {

			@Override
			public List<String> errors() {
				return Collections.emptyList();
			}

			@Override
			public List<String> warnings() {
				return Collections.emptyList();
			}

			@Override
			public Parser parser() {
				return parser;
			}
			
		};
	}
	
	private static void addToParseTable(Map<SymbolType, Map<SymbolType, List<Production>>> parseTable, SymbolType tokenType, Production production) {
		Utils.check(tokenType.isTerminal(), "tokenType: must be terminal");
		
		Map<SymbolType, List<Production>> productionMap = parseTable.get(tokenType);
		if (productionMap == null) {
			parseTable.put(tokenType, productionMap = new HashMap<SymbolType, List<Production>>());
		}
		
		List<Production> productions = productionMap.get(production.symbolType());
		if (productions == null) {
			productionMap.put(production.symbolType(), productions = new ArrayList<Production>());
		}
		
		productions.add(production);		
	}
	
	private static final class ParserInstance {
		private final ParseCache cache = new ParseCache();
		private final Map<SymbolType, Map<SymbolType, List<Production>>> parseTable;
		private final List<Symbol> tokens;
		
		private List<ProductionIndexPair> bannedProductions = new ArrayList<ProductionIndexPair>();
		private int tokenIndex;	
		
		public ParserInstance(Map<SymbolType, Map<SymbolType, List<Production>>> parseTable, List<Symbol> tokens) {
			this.parseTable = parseTable;
			this.tokens = tokens;
		}
	
		public Symbol tryParse(SymbolType symbolType) {
			int startTokenIndex = this.tokenIndex;
			
			// todo not right because of banning: context matters!
			// check the cache
//			int cacheIndex = this.cache.get(startTokenIndex, symbolType);
//			if (cacheIndex >= 0) {
//				this.tokenIndex = this.cache.nextIndexCache[cacheIndex];
//				return this.cache.symbolCache[cacheIndex];
//			}
			
			// use the parse table to determine the productions to be considered for parsing the given type
			SymbolType nextTokenType = this.tokens.get(startTokenIndex).type();
			Map<SymbolType, List<Production>> tableEntry = this.parseTable.get(nextTokenType);
			if (tableEntry == null) {
				return null;
			}	
			List<Production> productions = tableEntry.get(symbolType);
			if (productions == null) {
				return null;
			}
			
			for (int i = 0; i < productions.size(); ++i) {
				Production production = productions.get(i);
				
				System.out.println("Trying " + production + " @ " + startTokenIndex);
				
				// determine if the production is banned. This prevents us from ever trying the same production at the same index within
				// a recursion stack, thus putting a cap on how deep we can recurse and also permitting left-recursion
				boolean banned = false;
				for (int j = this.bannedProductions.size() - 1; j >= 0 && this.bannedProductions.get(j).tokenIndex == startTokenIndex; --j) {
					if (production == this.bannedProductions.get(j).production) {
						banned = true;
						System.out.println(production  + " banned @ " + startTokenIndex);
						continue;
					}
				}
				
				if (!banned) {
					Symbol parsed = this.tryParse(production);					
					
					if (parsed == null) {
						// backtrack
						this.tokenIndex = startTokenIndex;
					} else {			
						// populate the cache on success
						this.cache.put(startTokenIndex, this.tokenIndex, symbolType, parsed);
						System.out.println(production + " worked @ " + startTokenIndex);
						return parsed;
					}
				}
			}
			
			// populate the cache on failure
			this.cache.put(startTokenIndex, startTokenIndex, symbolType, null);
			
			System.out.println("failed @ " + startTokenIndex);
			return null;
		}
		
		private Symbol tryParse(Production production) {			
			List<Symbol> parsedChildren = null;
			for (int i = 0; i < production.childTypes().size(); ++i) {
				SymbolType childType = production.childTypes().get(i);
				// if matching a token, try to eat the token
				if (childType.isTerminal()) {
					if (this.tokenIndex >= this.tokens.size()) {
						return null;
					} 
					Symbol currentToken = this.tokens.get(this.tokenIndex);
					if (!currentToken.type().equals(childType)) {
						return null;
					}
					
					if (parsedChildren == null) {
						parsedChildren = new ArrayList<Symbol>();
					}
					parsedChildren.add(currentToken);
					++this.tokenIndex;
				}
				else {
					// parse a non-terminal recursively
					
					if (i == 0) {
						// if this is the first symbol type in the production, protect against infinite left-recursion
						// by banning further use of the production at this index
						this.bannedProductions.add(new ProductionIndexPair(production, this.tokenIndex));
					}
					
					Symbol parsed = this.tryParse(childType);					
					
					if (i == 0) {
						this.bannedProductions.remove(this.bannedProductions.size() - 1);
					}				
					
					if (parsed == null) {
						return null;
					}
					
					if (parsedChildren == null) {
						parsedChildren = new ArrayList<Symbol>();
					}
					parsedChildren.add(parsed);
				}				
			}
			
			return production.symbolType().createSymbol(parsedChildren != null ? parsedChildren : Collections.<Symbol>emptyList()); 
		}
		
		private static final class ProductionIndexPair {
			public final Production production;
			public final int tokenIndex;
			
			public ProductionIndexPair(Production production, int tokenIndex) {
				this.production = production;
				this.tokenIndex = tokenIndex;
			}
		}
		
		private static final class ParseCache {
			private static final int CACHE_SIZE = 1 << 10;
			
			public final Symbol[] symbolCache = new Symbol[CACHE_SIZE];
			public final int[] nextIndexCache = new int[CACHE_SIZE];
			private final SymbolType[] symbolTypeCache = new SymbolType[CACHE_SIZE];
			private final int[] indexCache = new int[CACHE_SIZE];
			
			public ParseCache() {
				Arrays.fill(this.indexCache, -1);
			}
			
			public int get(int index, SymbolType symbolType) {
				int hash = hash(index, symbolType);
				if (this.indexCache[hash] == index
					&& symbolType.equals(this.symbolTypeCache[hash])) {
					return hash;
				}
				
				return -1;
			}
			
			public void put(int startIndex, int endIndex, SymbolType symbolType, Symbol symbol) {
				int hash = hash(startIndex, symbolType);
				this.symbolCache[hash] = symbol;
				this.nextIndexCache[hash] = endIndex;
				this.indexCache[hash] = startIndex;
				this.symbolTypeCache[hash] = symbolType;
			}
			
			private static int hash(int index, SymbolType symbolType) {
				// suggestion from http://stackoverflow.com/questions/5889238/why-is-xor-the-default-way-to-combine-hashes
				// the & does both abs and mod, since CACHE_SIZE is a power of 2
				return ((3 * index) + symbolType.hashCode()) & (CACHE_SIZE - 1);
			}
		}
	}	
}
