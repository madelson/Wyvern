/**
 * 
 */
package compiler.parse;

import compiler.Symbol;
import compiler.SymbolType;

/**
 * @author mikea_000
 * 
 */
final class RecursiveDescentParsingCache {
	private static final int CACHE_SIZE = 1024;

	private final CacheEntryImpl[] entries = new CacheEntryImpl[CACHE_SIZE];

	public void put(SymbolType symbolType, int tokenIndex, int precedenceContextVersion, Symbol result,
			int nextTokenIndex) {
		int hash = hash(symbolType, tokenIndex, precedenceContextVersion);

		CacheEntryImpl entry, currentEntry = this.entries[hash];
		if (currentEntry == null) {
			this.entries[hash] = entry = new CacheEntryImpl();
		} else {
			entry = currentEntry;
		}

		entry.symbolType = symbolType;
		entry.tokenIndex = tokenIndex;
		entry.precedenceContextVersion = precedenceContextVersion;
		entry.symbol = result;
		entry.nextTokenIndex = nextTokenIndex;
	}

	public CacheEntry get(SymbolType symbolType, int tokenIndex, int precedenceContextVersion) {
		int hash = hash(symbolType, tokenIndex, precedenceContextVersion);

		CacheEntryImpl entry = this.entries[hash];
		return entry == null || tokenIndex != entry.tokenIndex
				|| precedenceContextVersion != entry.precedenceContextVersion || !symbolType.equals(entry.symbolType) ? null
				: entry;
	}

	private static int hash(SymbolType symbolType, int tokenIndex, int precedenceContextVersion) {
		// suggestion from
		// http://stackoverflow.com/questions/5889238/why-is-xor-the-default-way-to-combine-hashes
		int rawHash = (3 * ((3 * precedenceContextVersion) + tokenIndex)) + symbolType.hashCode();

		// the & does both abs and mod, since CACHE_SIZE is a power of 2
		return rawHash & (CACHE_SIZE - 1);
	}

	public static abstract class CacheEntry {
		public abstract Symbol symbol();

		public abstract int nextTokenIndex();
	}

	private static final class CacheEntryImpl extends CacheEntry {
		public Symbol symbol;
		public int nextTokenIndex;

		public SymbolType symbolType;
		public int tokenIndex;
		public int precedenceContextVersion;

		@Override
		public Symbol symbol() {
			return this.symbol;
		}

		@Override
		public int nextTokenIndex() {
			return this.nextTokenIndex;
		}
	}
}
