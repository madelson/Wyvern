/**
 * 
 */
package compiler;

import java.util.Iterator;
import java.util.ListIterator;

/**
 * @author Michael
 *
 */
public class SymbolTreeIterator extends TreeIterator<Symbol> {
	private static final SymbolTreeAdapter ADAPTER = new SymbolTreeAdapter();
	
	public SymbolTreeIterator(Symbol root) {
		super(ADAPTER, root);
	}	

	private static class SymbolTreeAdapter implements TreeAdapter<Symbol> {

		@Override
		public boolean isLeaf(Symbol node) {
			return node.type().isTerminal();
		}

		@Override
		public Iterable<Symbol> childrenOf(final Symbol node) {			
			return new Iterable<Symbol>() {

				@Override
				public Iterator<Symbol> iterator() {
					final ListIterator<Symbol> listIterator = node.children().listIterator(node.children().size());
					return new Iterator<Symbol>() {

						@Override
						public boolean hasNext() {
							return listIterator.hasPrevious();
						}

						@Override
						public Symbol next() {
							return listIterator.previous();
						}

						@Override
						public void remove() {
							throw new UnsupportedOperationException("remove");
						}
					};
				}
				
			};
		}
		
	}
}
