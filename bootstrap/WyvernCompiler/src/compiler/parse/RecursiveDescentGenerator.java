/**
 * 
 */
package compiler.parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import compiler.Symbol;
import compiler.SymbolType;
import compiler.Utils;
import compiler.parse.RecursiveDescentAnalyzer.Rule;
import compiler.parse.RecursiveDescentParsingCache.CacheEntry;

/**
 * @author mikea_000
 * 
 *         Generates a recursive descent-style parser (LL(inf)).
 * 
 *         ** Common issues with such parsers: **
 * 
 *         1. left recursion: (e. g. EXP -> EXP + EXP). The naive parser tries
 *         to parse this production, then immediately tries to parse it again,
 *         leading infinite recursion
 * 
 *         2. precedence: (e. g. EXP -> EXP + EXP, EXP -> -EXP). Should -1 + 2
 *         be -(1 + 2) or (-1) + 2?
 * 
 *         3. lookahead: many grammars don't work with fixed lookahead
 * 
 *         4. associativity: sometimes we want LEFT: 1 + 2 + 3 => (1 + 2) + 3,
 *         sometimes we want RIGHT a ? b : c ? d : e should be a ? b : (c ? d :
 *         e). Sometimes, we may even want NO associativity (e. g. 1/1/1 is just
 *         confusing)
 * 
 *         ** Solutions: **
 * 
 *         1. we can solve left recursion by determining up-front which
 *         productions are vulnerable to it and in which positions they are
 *         vulnerable. Essentially, a production is vulnerable at a position if
 *         the child symbol type at that position is (1) preceeded by only
 *         nullable types (2) when expanded through available productions can
 *         find the current production and (3) is not a "rename" production (e.
 *         g. EXP -> BINOP). The reason for excluding renames is that these
 *         aren't actually and issue unless there's a circular loop of renames:
 *         instead, renames basically just include more productions which we
 *         might try. Once we determine the set of vulnerable
 *         productions/positions, we can prevent recursion by maintaining a
 *         stack of (production, token index) tuples which we push to whenever
 *         parsing a vulnerable production/position pair at that token index.
 *         Whenever we consider parsing a production at a token index, we first
 *         check the stack to ensure that that production isn't "banned".
 * 
 *         For example, let's say we have EXP -> BINOP, BINOP -> EXP + EXP, EXP
 *         -> 1 and we are parsing "1 + 1": We first consider EXP -> BINOP. We
 *         then consider BINOP -> EXP + EXP. This is vulnerable, so we push
 *         (BINOP -> EXP + EXP, 0). We then consider EXP -> BINOP. We then
 *         consider again BINOP -> EXP + EXP. This is banned, though, so we fail
 *         back and instead consider EXP -> 1. This works, so we parse that,
 *         then +. Finally, we consider how to parse the final 1. Here, we again
 *         consider EXP -> BINOP and BINOP -> EXP + EXP, eventually choosing EXP
 *         -> 1 because of the missing "+" (the token index is 2 so the ban is
 *         not in effect).
 * 
 *         2. LL parsers typically handle precedence by redefining the grammar
 *         such that ambiguous rules get different symbols. Thus instead, of EXP
 *         -> EXP + EXP and EXP -> EXP * EXP, they'd have EXP -> TERM, TERM ->
 *         FACTOR [+ Factor]*, FACTOR -> 1 [* 1]*. This is annoying since it
 *         requires you to redefine the grammar around precedence, instead of
 *         around more meaningful constructs like BINOP. It also means you get
 *         weird results like EXP(TERM(FACTOR(1))) instead of EXP(1).
 * 
 *         Precedence is conceptually similar to left recursion. Basically, we
 *         want to prevent the case where a production has a child who uses a
 *         lower-precedence rule: EXP(BINOP(EXP(1), *, EXP(BINOP(EXP(1), +,
 *         EXP(1))))). Note here that the indirection of EXP -> BINOP means that
 *         the lower-precedence production is NOT a direct child.
 * 
 *         We can address precedence using the same mechanism as for
 *         left-recursion, essentially saying that left-recursion is a special
 *         case of precedence where we don't even allow equal precedence in some
 *         cases. Thus, we again first analyze productions for vulnerability to
 *         precedence violation. A production is vulnerable to violation at a
 *         position if the child type at that position, when expanded through
 *         possible productions, can yield a lower-precedence production without
 *         advancing the token index. Rename productions are excluded here.
 *         FURTHERMORE, for now we limit such ambiguities to leading and
 *         trailing locations in the string. Thus, EXP -> ( EXP ) will not
 *         consider position 1 (EXP) to be vulnerable because it is surrounded
 *         by non-nullable parens. While I imagine that some example grammar
 *         exists where this NOT the interpretation we'd want, I believe it
 *         should be good enough for real PL grammars, where binary and unary
 *         operators are the primary causes of such ambiguity.
 * 
 *         Next, we once again maintain a stack of (production, token index)
 *         tuples. When parsing a vulnerable production at a vulnerable
 *         position, we push that production and the token index on the stack.
 *         Whenever we consider a production, we ban it if the stack contains an
 *         entry with matching position and higher precedence.
 * 
 *         3. While we build the LL(1) parsing table for the grammar, we don't
 *         fail hard if the table contains multiple entries for an expected
 *         symbol type and lookahead token. Instead, we simply include all
 *         entries and try each one in reverse-precedence order, back-tracking
 *         on failure.
 * 
 *         Of course, the disadvantage of backtracking is that it leads to
 *         performance issues from parsing subtrees multiple times. Ideally,
 *         we'd eliminate this via caching. However, this is difficult because
 *         the parsing context includes not just the symbol type and token index
 *         but also the additional context introduced by our
 *         left-recursion/precedence fixing stacks. We could hash the context by
 *         keeping a version counter which gets incremented with every new push.
 *         This counter would be part of the tuples, so popping would restore
 *         the previous counter. Another approach would be to never pop off the
 *         stack at all, but instead leave the "dead" items there and just
 *         decrement the pointer. That way, popping and repushing the same thing
 *         wouldn't change the version. It's unclear if this would ever help,
 *         though.
 * 
 *         4. Given backtracking, associativity can also be seen as a special
 *         case of precedence where the specified production cannot appear as
 *         the left, right or either child. However, due to the way top-down
 *         parsing works and the restrictions on left-recursion, there's no
 *         obvious way to achieve left associativity (we could achieve
 *         non-associativity). Thus, it seems preferable to leave associativity
 *         as a post-processing step.
 * 
 */
public class RecursiveDescentGenerator implements ParserGenerator {

	/*
	 * (non-Javadoc)
	 * 
	 * @see compiler.parse.ParserGenerator#generate(compiler.parse.Grammar)
	 */
	@Override
	public Result generate(final Grammar grammar) {
		// Build the LL(1) predictive table. Unlike true LL(1), however, we'll
		// try all valid productions instead
		// of becoming stuck when we can't "predict" the right one. this table
		// tells which productions
		// X -> a are valid to attempt for a next symbol s. A production is
		// valid if s is in FIRST(a) or if a is nullable
		// and s is in FOLLOW(a)
		final Map<SymbolType, Map<SymbolType, List<Production>>> parseTable = new HashMap<SymbolType, Map<SymbolType, List<Production>>>();
		for (Production production : grammar.productions()) {
			if (production.childTypes().isEmpty()) {
				// X -> nothing should be valid for any token type
				for (SymbolType tokenType : grammar.terminalSymbolTypes()) {
					addToParseTable(parseTable, tokenType, production);
				}
			} else {
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

		final RecursiveDescentAnalyzer analyzer = new RecursiveDescentAnalyzer(grammar);

		final Parser parser = new Parser() {

			@Override
			public boolean isCompiled() {
				return false;
			}

			@Override
			public Result parse(Iterator<Symbol> tokenStream) {
				List<Symbol> tokens = Utils.toList(tokenStream);

				ParserInstance instance = new ParserInstance(parseTable, analyzer, tokens);
				final Symbol parseTree = instance.tryParse(grammar.context().startType());

				return new Result() {

					@Override
					public List<String> errors() {
						return parseTree == null ? Arrays.asList("Failed to parse") : Collections.<String> emptyList();
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

	private static void addToParseTable(Map<SymbolType, Map<SymbolType, List<Production>>> parseTable,
			SymbolType tokenType, Production production) {
		Utils.check(tokenType.isTerminal(), "tokenType: must be terminal");

		Map<SymbolType, List<Production>> productionMap = parseTable.get(tokenType);
		if (productionMap == null) {
			parseTable.put(tokenType, productionMap = new HashMap<SymbolType, List<Production>>());
		}

		List<Production> productions = productionMap.get(production.symbolType());
		if (productions == null) {
			productionMap.put(production.symbolType(), productions = new ArrayList<Production>());
		}

		productions.add(0, production); // we want to try lowest-priority first
		// productions.add(production);
	}

	private static final class ParserInstance {
		private final RecursiveDescentParsingCache cache = new RecursiveDescentParsingCache();
		private final Map<SymbolType, Map<SymbolType, List<Production>>> parseTable;
		private final RecursiveDescentAnalyzer analyzer;
		private final List<Symbol> tokens;
		private final RecursiveDescentPrecedenceHelper precedenceHelper;

		private int tokenIndex;

		public ParserInstance(Map<SymbolType, Map<SymbolType, List<Production>>> parseTable,
				RecursiveDescentAnalyzer analyzer, List<Symbol> tokens) {
			this.parseTable = parseTable;
			this.analyzer = analyzer;
			this.tokens = tokens;
			this.precedenceHelper = new RecursiveDescentPrecedenceHelper(this.analyzer);
		}

		public Symbol tryParse(SymbolType symbolType) {
			int startTokenIndex = this.tokenIndex;

			// check the cache
			int precedenceContextVersion = this.precedenceHelper.cacheVersion();
			CacheEntry cacheEntry = this.cache.get(symbolType, startTokenIndex, precedenceContextVersion);
			if (cacheEntry != null) {
				//System.out.printf("Cache hit: %s @%s v%s -> %s (-> @%s)\n", symbolType.name(), startTokenIndex, precedenceContextVersion, cacheEntry.symbol(), cacheEntry.nextTokenIndex());
				this.tokenIndex = cacheEntry.nextTokenIndex();
				return cacheEntry.symbol();
			}

			// use the parse table to determine the productions to be considered
			// for parsing the given type
			SymbolType nextTokenType = this.tokens.get(startTokenIndex).type();
			Map<SymbolType, List<Production>> tableEntry = this.parseTable.get(nextTokenType);
			if (tableEntry == null) {
				return null;
			}
			List<Production> productions = tableEntry.get(symbolType);
			if (productions == null) {
				return null;
			}

			// attempt a parse with each possible production
			for (int i = 0; i < productions.size(); ++i) {
				Production production = productions.get(i);

				// determine if the production is banned for left-recursion or
				// precedence reasons
				if (!this.precedenceHelper.allow(production, startTokenIndex)) {
					continue;
				}

				Symbol parsed = this.tryParse(production);

				if (parsed == null) {
					// backtrack
					this.tokenIndex = startTokenIndex;
				} else {
					// populate the cache on success
					this.cache.put(symbolType, startTokenIndex, precedenceContextVersion, parsed, this.tokenIndex);
					return parsed;
				}
			}

			// populate the cache on failure
			this.cache.put(symbolType, startTokenIndex, precedenceContextVersion, null, startTokenIndex);
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
				} else {
					// parse a non-terminal recursively

					RecursiveDescentAnalyzer.Rule rule = this.analyzer.getRule(production, i);
					boolean requiresBan = rule != Rule.None;
					if (requiresBan) {
						this.precedenceHelper.pushRuleScope(this.tokenIndex, production, rule);
					}

					Symbol parsed = this.tryParse(childType);

					if (requiresBan) {
						this.precedenceHelper.popRuleScope();
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

			return production.symbolType().createSymbol(
					parsedChildren != null ? parsedChildren : Collections.<Symbol> emptyList());
		}
	}
}
