/**
 * 
 */
package compiler;

import java.util.*;

/**
 * @author Michael
 * 
 */
public class Context {
	private final SymbolType eof, unrecognized, start;
	private final Map<String, SymbolType> types = new LinkedHashMap<String, SymbolType>();

	public Context() {
		this.eof = this.getTerminalSymbolType("EOF");
		this.unrecognized = this.getTerminalSymbolType("UNRECOGNIZED");
		this.start = this.getNonTerminalSymbolType("START");
	}

	/**
	 * Returns the types currently registered on the context
	 */
	public Collection<SymbolType> types() {
		return this.types.values();
	}

	/**
	 * The start symbol for all grammars
	 */
	public SymbolType startType() {
		return this.start;
	}

	/**
	 * The last token in a file
	 */
	public SymbolType eofType() {
		return this.eof;
	}

	/**
	 * Represents a lexing error
	 */
	public SymbolType unrecognizedType() {
		return this.unrecognized;
	}

	/**
	 * Retrieves a terminal symbol type for this context
	 */
	public SymbolType getTerminalSymbolType(String name) {
		return this.getSymbolType(name, true);
	}

	/**
	 * Retrieves a non-terminal symbol type for this context
	 */
	public SymbolType getNonTerminalSymbolType(String name) {
		return this.getSymbolType(name, false);
	}

	private SymbolType getSymbolType(String name, boolean isTerminal) {
		if (Utils.isNullOrEmpty(name))
			throw Utils.err("Invalid name");

		if (!this.types.containsKey(name)) {
			SymbolType type = isTerminal ? createTerminalSymbolType(name)
					: createNonTerminalSymbolType(name);
			this.types.put(name, type);

			return type;
		}

		SymbolType type = this.types.get(name);
		if (type.isTerminal() != isTerminal)
			throw Utils.err("Name " + name
					+ " is already in use by a different symbol");

		return type;
	}

	private SymbolType createTerminalSymbolType(final String name) {
		return new SymbolType() {
			@Override
			public Context context() {
				return Context.this;
			}

			@Override
			public String name() {
				return name;
			}

			@Override
			public boolean isTerminal() {
				return true;
			}

			@Override
			public Symbol createSymbol(Symbol... children) {
				throw Utils.err("Terminal symbols have no children!");
			}

			@Override
			public Symbol createSymbol(Iterable<Symbol> children) {
				return this.createSymbol();
			}

			@Override
			public Symbol createSymbol(final String text, final int line,
					final int position) {
				final SymbolType thisType = this;
				return new Symbol() {

					@Override
					public int line() {
						return line;
					}

					@Override
					public int endLine() {
						/*
						 * (1) Start with line. (2) Add the line terminator
						 * count. (3) Subtract 1 if text ends with a line
						 * terminator (since the line terminator is considered
						 * to be the last character on its line).
						 */
						return line + (Utils.split(text, "\n").length - 1)
								- (text.endsWith("\n") ? 1 : 0);
					}

					@Override
					public int position() {
						return position;
					}

					@Override
					public int endPosition() {
						// for empty or 1-length text, the end position is the
						// same as the start position
						if (text.length() <= 1) {
							return position;
						}

						int lastLineTerminator = text.lastIndexOf("\n");

						// if it's a one-liner, just add the text length
						if (lastLineTerminator < 0) {
							return position + text.length() - 1;
						}

						// if text ends with a line terminator, use the previous
						// line terminator
						if (lastLineTerminator == text.length() - 1) {
							lastLineTerminator = text.lastIndexOf("\n",
									lastLineTerminator - 1);
							// if there's no other last line terminator, it's a
							// one-liner!
							if (lastLineTerminator < 0) {
								return position + text.length() - 1;
							}
						}

						return text.length() - lastLineTerminator - 1;
					}

					@Override
					public String text() {
						return text;
					}

					@Override
					public SymbolType type() {
						return thisType;
					}

					@Override
					public List<Symbol> children() {
						throw Utils.err("Terminal symbols have no children!");
					}

					@Override
					public String toString() {
						String nameAndText;
						if (this.type().name().compareToIgnoreCase(this.text()) == 0)
							nameAndText = '"' + this.text() + '"';
						else if (this.text().length() == 0)
							nameAndText = this.type().name();
						else
							nameAndText = String.format("%s(\"%s\")", this
									.type().name(), this.text());

						return String.format("%s @%s:%s", nameAndText,
								this.line(), this.position());
					}
				};
			}

			@Override
			public String toString() {
				return symbolTypeToString(this);
			}
		};
	}

	private SymbolType createNonTerminalSymbolType(final String name) {
		return new SymbolType() {

			@Override
			public Context context() {
				return Context.this;
			}

			@Override
			public String name() {
				return name;
			}

			@Override
			public boolean isTerminal() {
				return false;
			}

			@Override
			public Symbol createSymbol(Symbol... children) {
				return createSymbolFromList(Utils.immutableCopy(Arrays
						.asList(children)));
			}

			@Override
			public Symbol createSymbol(Iterable<Symbol> children) {
				return createSymbolFromList(Collections.unmodifiableList(Utils
						.toList(children)));
			}

			private Symbol createSymbolFromList(final List<Symbol> children) {
				final SymbolType thisType = this;
				return new Symbol() {

					@Override
					public SymbolType type() {
						return thisType;
					}

					@Override
					public int line() {
						// MA 4/21/12 we have to be careful here, since we don't
						// want to return -1
						// just because the symbol at the beginning/end of the
						// list is empty (e. g. a
						// linked-list terminator). Thus, we walk the list and
						// take the first symbol
						// with a known position. A TODO for the future would be
						// to have a way to
						// initialize empty symbols with line numbers and
						// positions instead of children
						for (Symbol child : children) {
							int line = child.line();
							if (line != -1) {
								return line;
							}
						}

						return -1;
					}

					@Override
					public int endLine() {
						// MA 4/21/12 we have to be careful here, since we don't
						// want to return -1
						// just because the symbol at the beginning/end of the
						// list is empty (e. g. a
						// linked-list terminator). Thus, we walk the list and
						// take the first symbol
						// with a known position. A TODO for the future would be
						// to have a way to
						// initialize empty symbols with line numbers and
						// positions instead of children
						for (int i = children.size() - 1; i >= 0; i--) {
							int endLine = children.get(i).endLine();
							if (endLine != -1) {
								return endLine;
							}
						}

						return -1;
					}

					@Override
					public int position() {
						// MA 4/21/12 we have to be careful here, since we don't
						// want to return -1
						// just because the symbol at the beginning/end of the
						// list is empty (e. g. a
						// linked-list terminator). Thus, we walk the list and
						// take the first symbol
						// with a known position. A TODO for the future would be
						// to have a way to
						// initialize empty symbols with line numbers and
						// positions instead of children
						for (Symbol child : children) {
							int pos = child.position();
							if (pos != -1) {
								return pos;
							}
						}

						return -1;
					}

					@Override
					public int endPosition() {
						// MA 4/21/12 we have to be careful here, since we don't
						// want to return -1
						// just because the symbol at the beginning/end of the
						// list is empty (e. g. a
						// linked-list terminator). Thus, we walk the list and
						// take the first symbol
						// with a known position. A TODO for the future would be
						// to have a way to
						// initialize empty symbols with line numbers and
						// positions instead of children
						for (int i = children.size() - 1; i >= 0; i--) {
							int endPos = children.get(i).endPosition();
							if (endPos != -1) {
								return endPos;
							}
						}

						return -1;
					}

					@Override
					public String text() {
						StringBuilder sb = new StringBuilder();
						Symbol lastChild = null;

						for (Symbol child : children) {
							if (child.text().isEmpty())
								continue;
							if (lastChild == null) {
								sb.append(child.text());
								lastChild = child;
								continue;
							}

							// TODO make this ignore -1 line and position numbers
							// catch up in lines and spaces
							int lineDiff = child.line() - lastChild.endLine(), posDiff = child
									.position()
									- (lineDiff > 0 ? 0 : lastChild
											.endPosition() + 1);
							for (int i = 0; i < lineDiff; i++)
								sb.append(Utils.NL);
							for (int i = 0; i < posDiff; i++)
								sb.append(' ');
							sb.append(child.text());
							lastChild = child;
						}

						return sb.toString();
					}

					@Override
					public List<Symbol> children() {
						return children;
					}

					@Override
					public String toString() {
						StringBuilder sb = new StringBuilder();
						sb.append(this.type().name()).append('(');
						for (Symbol child : this.children()) {
							sb.append(child).append(' ');
						}
						if (sb.charAt(sb.length() - 1) == ' ') {
							sb.setCharAt(sb.length() - 1, ')');
						} else {
							sb.append(')');
						}

						return sb.toString();
					}
				};
			}

			@Override
			public Symbol createSymbol(String text, int line, int position) {
				throw Utils
						.err("A non-terminal symbol cannot be created from raw text!");
			}

			@Override
			public String toString() {
				return symbolTypeToString(this);
			}
		};
	}

	private static String symbolTypeToString(SymbolType type) {
		return String.format("%s (%s)", type.name(),
				type.isTerminal() ? "terminal" : "non-terminal");
	}
}
