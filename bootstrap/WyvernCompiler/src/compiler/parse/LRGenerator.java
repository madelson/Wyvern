/**
 * 
 */
package compiler.parse;

import java.util.*;

import compiler.*;

/**
 * @author Michael
 * 
 */
public abstract class LRGenerator implements ParserGenerator {

	/**
	 * Computes the closure of a set of items, creating a state.
	 * TODO: for performance we could have this return an ImmutableSet<T> so that
	 * State wouldn't have to create a copy
	 */
	protected abstract Set<Item> closure(Grammar grammar, Set<Item> items);

	/**
	 * Computes the state to which we will transition upon seeing the given
	 * symbol at the given state
	 */
	protected abstract Set<Item> transition(Grammar grammar, State state,
			SymbolType symbolType);

	/**
	 * Computes the set of reduce actions for a state
	 */
	protected abstract Set<Reduction> reductions(Grammar grammar, State state);

	/**
	 * May merge some states. Modifies the input set appropriately, and returns
	 * a map of old to new state so that edges can be updated
	 */
	protected abstract Map<State, State> mergeStates(Set<State> states);

	/*
	 * (non-Javadoc)
	 * 
	 * @see compiler.parse.ParserGenerator#generate(compiler.parse.Grammar)
	 */
	@Override
	public Result generate(final Grammar grammar) {
		// generate the dfa
		final Set<State> states = new LinkedHashSet<State>();
		final Set<Edge> edges = new LinkedHashSet<Edge>();
		final Set<Reduction> reductions = new LinkedHashSet<Reduction>();
		final State startState = this.createDFA(grammar, states, edges,
				reductions);

		// build table
		final List<String> errors = new ArrayList<String>(), warnings = new ArrayList<String>();
		final Map<State, Map<SymbolType, Object>> table = new HashMap<State, Map<SymbolType, Object>>();

		// add shifts
		for (Edge edge : edges) {
			Object action = Utils.put(table, HashMap.class, edge.from(),
					edge.symbolType(), (Object) edge);
			if (action != null) {
				// no precedence check, since we haven't added reductions yet
				// and precendence only helps resolve shift-reduce
				errors.add(formatError(action, edge));
			}
		}

		// add reductions
		for (Reduction reduction : reductions) {
			Set<SymbolType> types = reduction.symbolType() == null ? grammar
					.terminalSymbolTypes() : Collections.singleton(reduction
					.symbolType());

			for (SymbolType type : types) {
				Object action = Utils.put(table, HashMap.class,
						reduction.state(), type, (Object) reduction);
				if (action != null) {
					Object preferred = maxByPrecedence(grammar.precedence(),
							action, reduction);
					if (preferred != null) {
						table.get(reduction.state()).put(type, preferred);
					} else {
						errors.add(formatError(action, reduction));
					}
				}
			}
		}

		final Object accept = new Object() {
			@Override
			public String toString() {
				return "accept";
			}
		};

		// accepts
		for (State state : states)
			for (Item item : state.items())
				if (item.production().symbolType()
						.equals(grammar.context().startType())
						&& item.hasNextSymbolType()
						&& item.nextSymbolType().equals(
								grammar.context().eofType())) {
					Object action = Utils.put(table, HashMap.class, state,
							grammar.context().eofType(), accept);
					if (action != null)
						errors.add(formatError(action, accept));
					break;
				}

		// System.out.println(Utils.NL + grammar.name() + " "
		// + this.getClass().getName());
		// for (State s : table.keySet())
		// for (SymbolType t : table.get(s).keySet()) {
		// Object action = table.get(s).get(t);
		// String entry = "  ";
		// if (action instanceof Edge)
		// entry = "s" + ((Edge) action).to().name();
		// else if (action instanceof Reduction)
		// entry = "r";
		// else if (action != null)
		// entry = "a";
		// System.out.println(String.format("%s\t%s\t%s", s.name(),
		// t.name(), entry));
		// }
		// System.out.flush();

		// parser instance class
		class LRInstance {
			final Deque<Symbol> symbolStack = new ArrayDeque<Symbol>();
			final Deque<State> stateStack = new ArrayDeque<State>();

			Parser.Result parse(Iterator<Symbol> tokens) {
				this.stateStack.push(startState);
				Symbol token = tokens.next();

				while (true) {
					Object action = table.get(this.stateStack.getFirst()).get(
							token.type());

					if (action == null) {
						// System.err.println("No action found:");
						// System.err.println("at " +
						// this.stateStack.getFirst());
						// System.err.println("on token " + token);
						// System.out.println(this.stateStack.getFirst());
						// for (Map.Entry<SymbolType, Object> e : table.get(
						// this.stateStack.getFirst()).entrySet())
						// System.out
						// .println(e.getKey() + ": " + e.getValue());
						// System.out.println(this.symbolStack.getFirst());
						// System.out.println(token);
						// System.out
						// .println(table.get(this.stateStack.getFirst()));
						// System.out.println(grammar
						// .nff()
						// .followSets()
						// .get(grammar.context().getTerminalSymbolType(
						// "x")));
						// System.out.println(grammar.nff().first(
						// Collections.EMPTY_LIST));
						// System.out.flush();
					}

					if (action == accept) // accept
						break;

					if (action instanceof Edge) { // shift
						this.symbolStack.push(token);
						this.stateStack.push(((Edge) action).to());
						token = tokens.next();
					} else { // reduce
						Reduction reduction = (Reduction) action;
						Symbol[] children = new Symbol[reduction.production()
								.childTypes().size()];
						for (int i = children.length - 1; i >= 0; i--) {
							this.stateStack.pop();
							children[i] = this.symbolStack.pop();
						}

						this.symbolStack.push(reduction.production()
								.symbolType().createSymbol(children));
						this.stateStack.push(((Edge) table.get(
								this.stateStack.getFirst()).get(
								this.symbolStack.peekFirst().type())).to());
					}
				}

				return new Parser.Result() {

					@Override
					public List<String> warnings() {
						return Collections.emptyList();
					}

					@Override
					public Symbol parseTree() {
						return LRInstance.this.symbolStack.peekFirst();
					}

					@Override
					public List<String> errors() {
						return Collections.emptyList();
					}
				};
			}
		}

		// parser
		final Parser parser = new Parser() {

			@Override
			public boolean isCompiled() {
				return false;
			}

			@Override
			public Parser.Result parse(Iterator<Symbol> tokens) {
				return new LRInstance().parse(tokens);
			}
		};

		return new Result() {

			@Override
			public List<String> warnings() {
				return Collections.unmodifiableList(warnings);
			}

			@Override
			public List<String> errors() {
				return Collections.unmodifiableList(errors);
			}

			@Override
			public Parser parser() {
				return this.succeeded() ? parser : null;
			}

			@Override
			public Set<State> dfaStates() {
				return Collections.unmodifiableSet(states);
			}

			@Override
			public Set<Edge> dfaEdges() {
				return Collections.unmodifiableSet(edges);
			}

			@Override
			public Set<Reduction> dfaReductions() {
				return Collections.unmodifiableSet(reductions);
			}

			@Override
			public State dfaStartState() {
				return startState;
			}

			@Override
			public Map<State, Map<SymbolType, Object>> parseTable() {
				return Utils.deepImmutableCopy(table);
			}
		};
	}

	private static Object maxByPrecedence(PrecedenceFunction precedence,
			Object a, Object b) {
		if (a instanceof Edge && b instanceof Reduction) {
			SymbolType aType = ((Edge) a).symbolType(), bType = precedence
					.precedenceSymbolFor(((Reduction) b).production());
			if (aType == null || bType == null) {
				return null;
			}

			Integer aPrecedence = precedence.precedenceOf(aType), bPrecedence = precedence
					.precedenceOf(bType);
			if (aPrecedence == null || bPrecedence == null) {
				return null;
			}
			if (aPrecedence > bPrecedence) {
				return a;
			}
			if (bPrecedence > aPrecedence) {
				return b;
			}
			switch (precedence.associativityOf(aType)) {
			case Left:
				return b;
			case Right:
				return a;
			default:
				return null;
			}

		} else if (a instanceof Reduction && b instanceof Edge) {
			return maxByPrecedence(precedence, b, a);
		}

		return null;
	}

	private static String formatError(Object action1, Object action2) {
		Object[] actions = new Object[] { action1, action2 };
		Arrays.sort(actions, new Comparator<Object>() {
			@Override
			public int compare(Object a, Object b) {
				return this.classVal(a.getClass())
						- this.classVal(b.getClass());
			}

			private int classVal(Class<?> cls) {
				if (cls == Edge.class)
					return -1;
				if (cls == Reduction.class)
					return 1;
				return 0;
			}
		});

		String[] actionNames = new String[2];
		for (int i = 0; i < actions.length; i++)
			actionNames[i] = actions[i] instanceof Edge ? "shift"
					: (actions[i] instanceof Reduction ? "reduce" : "accept");

		return String.format("%s/%s conflict between \"%s\" and \"%s\"",
				actionNames[0], actionNames[1], actions[0], actions[1]);
	}

	/**
	 * Creates the set of states, edges, and reductions that form the LR DFA.
	 * Returns the start state.
	 */
	private State createDFA(Grammar grammar, final Set<State> states,
			final Set<Edge> edges, final Set<Reduction> reductions) {
		// create start state
		Set<Item> startItems = new LinkedHashSet<Item>();
		startItems.add(new Item(new Production(grammar.context().startType(),
				grammar.startSymbolType(), grammar.context().eofType()),
				grammar.context().eofType(), 0));
		startItems = this.closure(grammar, startItems);
		State startState = new State("1", startItems);

		// compute all states

		/*
		 * Note: the original algorithm repeats looping over the entires set of
		 * states until neither the state set nor the edge set changes. However,
		 * for any state/transition symbol type combination we will always
		 * produce the same state/edge, and thus there's no point in visiting a
		 * state twice. Thus, instead we keep the queue of states to process in
		 * a list and loop from i = 0 -> list.size(), where list.size() grows as
		 * we discover new states. When i catches up with list.size(), the
		 * algorithm has completed.
		 * 
		 * This was found to be substantially more performant that the simple
		 * translation of the algorithm.
		 */

		// maps unique item sets to labeled states. Caching states this way
		// allows us
		// to have two state/transition symbol combinations that map to
		// equivalent states
		// use the same state object (thus slowing new state generation and
		// allowing the algorithm
		// to terminate)
		Map<Set<Item>, State> itemsToStates = new HashMap<Set<Item>, State>();
		itemsToStates.put(startState.items(), startState);

		List<State> stateList = new ArrayList<State>(itemsToStates.values());
		// for each state I in T
		for (int i = 0; i < stateList.size(); ++i) {
			State fromState = stateList.get(i);
			// for each X in an item A -> A.XB in I
			for (SymbolType symbolType : fromState.transitionSymbolTypes()) {
				Set<Item> toStateItems = this.transition(grammar, fromState,
						symbolType);

				State toState = itemsToStates.get(toStateItems);

				// T <- T U {J}
				// create a new state if necessary
				if (toState == null) {
					toState = new State(
							String.valueOf(itemsToStates.size() + 1),
							toStateItems);
					itemsToStates.put(toState.items(), toState);
					stateList.add(toState); // queue the new state for further
											// processing
				}

				// E <- E U {I -X-> J}
				// add the edge, if it's new
				edges.add(new Edge(fromState, symbolType, toState));
			}
		}

		// store states
		states.addAll(stateList);

		// possibly merge states
		Map<State, State> conversions = this.mergeStates(states);
		for (Edge edge : new ArrayList<Edge>(edges)) {
			State newFrom = conversions.get(edge.from()), newTo = conversions
					.get(edge.to());
			if (newFrom != null || newTo != null) {
				edges.remove(edge);
				edges.add(new Edge(newFrom, edge.symbolType(), newTo));
			}
		}
		if (conversions.containsKey(startState))
			startState = conversions.get(startState);

		// get reductions
		for (State state : states)
			reductions.addAll(this.reductions(grammar, state));

		return startState;
	}

	/* Classes */
	public static abstract class Result extends ParserGenerator.Result {
		public abstract State dfaStartState();

		public abstract Set<State> dfaStates();

		public abstract Set<Edge> dfaEdges();

		public abstract Set<Reduction> dfaReductions();

		public abstract Map<State, Map<SymbolType, Object>> parseTable();

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();

			sb.append("States:" + Utils.NL + Utils.NL);
			for (State state : this.dfaStates())
				sb.append(state).append(Utils.NL + Utils.NL);

			sb.append(Utils.NL + "Edges:" + Utils.NL + Utils.NL);
			for (Edge edge : this.dfaEdges())
				sb.append(edge).append(Utils.NL);

			sb.append(Utils.NL + "Reductions:" + Utils.NL);
			for (Reduction reduction : this.dfaReductions())
				sb.append(reduction).append(Utils.NL);

			if (!this.errors().isEmpty()) {
				sb.append(Utils.NL + "Errors:" + Utils.NL);
				for (String error : this.errors())
					sb.append(error).append(Utils.NL);
			}

			if (!this.warnings().isEmpty()) {
				sb.append(Utils.NL + "Warnings:" + Utils.NL);
				for (String warning : this.warnings())
					sb.append(warning).append(Utils.NL);
			}

			return sb.toString();
		}
	}

	public static final class Item extends
			Tuples.Trio<Production, SymbolType, Integer> {
		private List<SymbolType> remaining;

		public Item(Production production, SymbolType lookahead,
				Integer position) {
			super(production, lookahead, position);
		}

		public Production production() {
			return this.item1();
		}

		public SymbolType lookahead() {
			return this.item2();
		}

		public int position() {
			return this.item3();
		}

		/**
		 * Returns a list of the symbol types which occur after the current type
		 * to be parsed. E. G. for A -> B.CF with lookahead z, remaining = [F,
		 * z]
		 */
		public List<SymbolType> remaining() {
			if (this.remaining == null)
				this.initRemaining();

			return this.remaining;
		}

		private void initRemaining() {
			Utils.check(this.lookahead() != null,
					"Remaining has no meaning with no lookahead");

			List<SymbolType> remaining = new ArrayList<SymbolType>(this
					.production()
					.childTypes()
					.subList(this.position() + 1,
							this.production().childTypes().size()));
			remaining.add(this.lookahead());
			this.remaining = Collections.unmodifiableList(remaining);
		}

		public Item advance() {
			Utils.check(this.hasNextSymbolType(), "Advanced too far!");

			return new Item(this.production(), this.lookahead(),
					this.position() + 1);
		}

		public boolean hasNextSymbolType() {
			return this.position() < this.production().childTypes().size();
		}

		public SymbolType nextSymbolType() {
			return this.production().childTypes().get(this.position());
		}

		@Override
		public String toString() {
			if (this.position() >= this.production().childTypes().size())
				return this.production().toString() + '.';

			StringBuilder sb = new StringBuilder();
			sb.append(this.production().symbolType().name()).append(" ->");
			for (int i = 0; i < this.position(); i++)
				sb.append(' ').append(
						this.production().childTypes().get(i).name());
			sb.append(" .").append(
					this.production().childTypes().get(this.position()).name());
			for (int i = this.position() + 1; i < this.production()
					.childTypes().size(); i++)
				sb.append(' ').append(
						this.production().childTypes().get(i).name());

			if (this.lookahead() != null)
				sb.append(" \t").append(this.lookahead().name());

			return sb.toString();
		}
	}

	public static final class State extends Tuples.Duo<String, Set<Item>> {
		private Map<SymbolType, List<Item>> transitionItems;

		public State(String name, Set<Item> items) {
			super(name, Utils.immutableCopy(items));
		}

		public String name() {
			return this.item1();
		}

		public Set<Item> items() {
			return this.item2();
		}

		public List<Item> transitionItems(SymbolType type) {
			if (this.transitionItems == null)
				this.initTransitionItems();

			List<Item> items = this.transitionItems.get(type);
			return items != null ? items : Collections.<Item> emptyList();
		}

		public Set<SymbolType> transitionSymbolTypes() {
			if (this.transitionItems == null)
				this.initTransitionItems();

			return this.transitionItems.keySet();
		}

		private void initTransitionItems() {
			Map<SymbolType, List<Item>> transitionItems = new HashMap<SymbolType, List<Item>>();
			for (Item item : this.items())
				if (item.hasNextSymbolType()
						&& !item.nextSymbolType().equals(
								item.nextSymbolType().context().eofType()))
					Utils.put(transitionItems, ArrayList.class,
							item.nextSymbolType(), item);

			this.transitionItems = Utils.deepImmutableCopy(transitionItems);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("State ").append(this.name());

			Map<Tuples.Duo<Production, Integer>, Set<SymbolType>> compressedItems = new LinkedHashMap<Tuples.Duo<Production, Integer>, Set<SymbolType>>();
			for (Item item : this.items()) {
				Tuples.Duo<Production, Integer> key = new Tuples.Duo<Production, Integer>(
						item.production(), item.position());
				if (item.lookahead() != null)
					Utils.put(compressedItems, LinkedHashSet.class, key,
							item.lookahead());
				else if (!compressedItems.containsKey(key))
					compressedItems.put(key, new LinkedHashSet<SymbolType>());
			}
			int maxItemStringLength = Integer.MIN_VALUE;
			for (Tuples.Duo<Production, Integer> k : compressedItems.keySet())
				maxItemStringLength = Math.max(maxItemStringLength, k.item1()
						.toString().length() + 1);
			for (Map.Entry<Tuples.Duo<Production, Integer>, Set<SymbolType>> e : compressedItems
					.entrySet()) {
				String itemString = new Item(e.getKey().item1(), null, e
						.getKey().item2()).toString();
				sb.append(Utils.NL).append(itemString);
				for (int i = 0; i < maxItemStringLength - itemString.length(); i++)
					sb.append(' ');
				if (!e.getValue().isEmpty()) {
					sb.append('\t');
					for (SymbolType lookaheadType : e.getValue())
						sb.append('"').append(lookaheadType.name()).append('"')
								.append(',');
					sb.setLength(sb.length() - 1);
				}
			}

			return sb.toString();
		}
	}

	public static final class Edge extends
			Tuples.Trio<State, SymbolType, State> {

		public Edge(State from, SymbolType symbolType, State to) {
			super(from, symbolType, to);
		}

		public State from() {
			return this.item1();
		}

		public SymbolType symbolType() {
			return this.item2();
		}

		public State to() {
			return this.item3();
		}

		@Override
		public String toString() {
			return String.format("%s -- %s --> %s", this.from().name(), this
					.symbolType().name(), this.to().name());
		}
	}

	public static final class Reduction extends
			Tuples.Trio<State, SymbolType, Production> {

		public Reduction(State state, SymbolType symbolType,
				Production production) {
			super(state, symbolType, production);
			Utils.check(symbolType == null || symbolType.isTerminal(),
					"Can't reduce on non-terminal type!");
		}

		public State state() {
			return this.item1();
		}

		public SymbolType symbolType() {
			return this.item2();
		}

		public Production production() {
			return this.item3();
		}

		@Override
		public String toString() {
			return String.format("at %s on %s reduce by %s", this.state()
					.name(), this.symbolType() != null ? this.symbolType()
					.name() : "any symbol", this.production());
		}
	}
}
