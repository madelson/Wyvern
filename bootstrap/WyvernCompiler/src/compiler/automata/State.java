/**
 * 
 */
package compiler.automata;

import compiler.Tuples;

/**
 * @author Michael
 * 
 */
public class State<T> extends Tuples.Duo<String, T> {

	public State(String name, T value) {
		super(name, value);
	}

	public String name() {
		return this.item1();
	}

	public T value() {
		return this.item2();
	}
}
