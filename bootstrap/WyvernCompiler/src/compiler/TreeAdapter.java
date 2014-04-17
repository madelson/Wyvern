/**
 * 
 */
package compiler;

/**
 * @author Michael
 *
 */
public interface TreeAdapter<T> {
	public boolean isLeaf(T node);
	public Iterable<T> childrenOf(T node);
}
