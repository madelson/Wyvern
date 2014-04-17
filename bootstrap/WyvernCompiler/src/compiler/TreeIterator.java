/**
 * 
 */
package compiler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * @author Michael
 * 
 */
public class TreeIterator<T> implements Iterator<T> {
	private final TreeAdapter<T> adapter;
	private final Deque<T> nodeStack = new ArrayDeque<T>();

	public TreeIterator(TreeAdapter<T> adapter, T root) {
		this.adapter = adapter;
		this.nodeStack.push(root);
	}

	public TreeAdapter<T> adapter() {
		return this.adapter;
	}

	@Override
	public boolean hasNext() {
		return !this.nodeStack.isEmpty();
	}

	@Override
	public T next() {
		T top = this.nodeStack.pop();
		if (!this.adapter().isLeaf(top)) {
			for (T child : this.adapter().childrenOf(top)) {
				this.nodeStack.push(child);
			}
		}

		return top;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("remove");
	}
}
