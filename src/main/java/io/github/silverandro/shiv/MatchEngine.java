package io.github.silverandro.shiv;

import com.unascribed.flexver.FlexVerComparator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MatchEngine {
	private MatchEngine() {
		throw new IllegalStateException("Match engine cannot be instantiated");
	}

	private static final Map<String, String> matchMap = new HashMap<>();

	public static void deleteAll() {
		matchMap.clear();
	}

	public static String get(String key) {
		return matchMap.get(key);
	}

	public static void register(String key, String value) {
		if (matchMap.containsKey(key)) {
			throw new IllegalArgumentException("Attempted to redefine " + key + " from " + matchMap.get(key) + " to " + value);
		} else {
			matchMap.put(key, value);
		}
	}

	public static boolean isMatch(String matchString) {
		ArrayList<MatchEntry> stack = new ArrayList<>();

		StringBuilder builder = new StringBuilder();
		String id = "";

		int size = matchString.length();
		for (int i = 0; i < size; i++) {
			char mChar = matchString.charAt(i);
			if (mChar == ' ' || mChar == '&' || mChar == '|' || mChar == '(' || mChar == ')') {
				if (!id.isEmpty() && !builder.toString().isEmpty()) {
					stack.add(new MatchEntry.Lookup(id, builder.toString()));
					id = "";
					builder = new StringBuilder();
				} else if (!id.isEmpty()) {
					stack.add(new MatchEntry.Contains(id));
					id = "";
					builder = new StringBuilder();
				} else if (!builder.toString().isEmpty()) {
					stack.add(new MatchEntry.Contains(builder.toString()));
					id = "";
					builder = new StringBuilder();
				}
			}

			if (mChar == '!') {
				stack.add(MatchEntry.INVERT);
			} else if (mChar == '|') {
				stack.add(MatchEntry.OR);
			} else if (mChar == '&') {
				stack.add(MatchEntry.AND);
			} else if (mChar == '(') {
				stack.add(MatchEntry.GROUP_OPEN);
			} else if (mChar == ')') {
				stack.add(MatchEntry.GROUP_CLOSE);
			} else if (mChar == ':') {
				id = builder.toString();
				builder = new StringBuilder();
			} else if (mChar != ' ') {
				builder.append(mChar);
			}
		}

		if (!id.isEmpty() && !builder.toString().isEmpty()) {
			stack.add(new MatchEntry.Lookup(id, builder.toString()));
		} else if (!id.isEmpty()) {
			stack.add(new MatchEntry.Contains(id));
		} else if (!builder.toString().isEmpty()) {
			stack.add(new MatchEntry.Contains(builder.toString()));
		}

		return reduce(stack);
	}

	private static boolean reduce(ArrayList<MatchEntry> stack) {
		// Resolve all lookups and contains
		int size = stack.size();
		for (int i = 0; i < size; i++) {
			MatchEntry entry = stack.get(i);
			if (entry instanceof MatchEntry.Lookup lookup) {
				if (matchMap.containsKey(lookup.id)) {
					String expected = lookup.expected;
					if (expected.startsWith(">") || expected.startsWith("<")) {
						int compare = FlexVerComparator.compare(matchMap.get(lookup.id), expected.replaceFirst("[<>]=?", ""));
						if (expected.charAt(1) == '=' && compare == 0) {
							stack.set(i, MatchEntry.ABS_TRUE);
						} else if (expected.startsWith(">") && compare > 0) {
							stack.set(i, MatchEntry.ABS_TRUE);
						} else if (expected.startsWith("<") && compare < 0) {
							stack.set(i, MatchEntry.ABS_TRUE);
						} else {
							stack.set(i, MatchEntry.ABS_FALSE);
						}
					} else {
						stack.set(i, MatchEntry.BooleanType.from(expected.equals(matchMap.get(lookup.id))));
					}
				} else {
					throw new IllegalStateException("Missing env value for " + lookup.id);
				}
			} else if (entry instanceof MatchEntry.Contains contains) {
				if (matchMap.containsKey(contains.id)) {
					stack.set(i, MatchEntry.ABS_TRUE);
				} else {
					stack.set(i, MatchEntry.ABS_FALSE);
				}
			}
		}

		// Collapse the logic down to a single absolute boolean
		int max = 10_000;
		reduce:
		while (stack.size() > 1 && max-- > 0) {
			int currentSize = stack.size();
			for (int i = 0; i < currentSize; i++) {
				MatchEntry entry = stack.get(i);

				// Reduce a group wrapping a single ABS to just the ABS
				if (entry == MatchEntry.GROUP_OPEN) {
					MatchEntry next = stack.get(i+1);
					MatchEntry nextNext = stack.get(i+2);
					if (nextNext == MatchEntry.GROUP_CLOSE && next instanceof MatchEntry.BooleanType) {
						stack.remove(i+2);
						stack.remove(i+1);
						stack.set(i, next);
						continue reduce;
					}
				}

				// Simple inversion, if you have an INVERT, just flip and reduce
				if (entry == MatchEntry.INVERT) {
					MatchEntry next = stack.get(i+1);
					if (next == MatchEntry.ABS_TRUE) {
						stack.remove(i+1);
						stack.set(i, MatchEntry.ABS_FALSE);
						continue reduce;
					} else if (next == MatchEntry.ABS_FALSE) {
						stack.remove(i+1);
						stack.set(i, MatchEntry.ABS_TRUE);
						continue reduce;
					}
				}

				// Simple AND, OR, and implicit AND
				if (entry instanceof MatchEntry.BooleanType) {
					if (i < size - 1) {
						MatchEntry next = stack.get(i+1);
						if (next instanceof MatchEntry.Operator) {
							MatchEntry nextNext = stack.get(i+2);
							if (nextNext instanceof MatchEntry.BooleanType) {
								if (next == MatchEntry.OR) {
									stack.remove(i+2);
									stack.remove(i+1);
									stack.set(i,
										MatchEntry.BooleanType.from(
											((MatchEntry.BooleanType)entry).val || ((MatchEntry.BooleanType)nextNext).val
										)
									);
									continue reduce;
								}

								if (next == MatchEntry.AND) {
									stack.remove(i+2);
									stack.remove(i+1);
									stack.set(i,
										MatchEntry.BooleanType.from(
											((MatchEntry.BooleanType)entry).val && ((MatchEntry.BooleanType)nextNext).val
										)
									);
									continue reduce;
								}
							}
						}

						if (next instanceof MatchEntry.BooleanType) {
							stack.remove(i+1);
							stack.set(i,
								MatchEntry.BooleanType.from(
									((MatchEntry.BooleanType)entry).val && ((MatchEntry.BooleanType)next).val
								)
							);
							continue reduce;
						}
					}
				}
			}
		}

		if (max == 0) {
			throw new IllegalStateException("Infinite reduction!");
		}

		return stack.get(0) == MatchEntry.ABS_TRUE;
	}

	private interface MatchEntry {
		MatchEntry INVERT = new Static();
		MatchEntry GROUP_OPEN = new Static();
		MatchEntry GROUP_CLOSE = new Static();
		MatchEntry AND = new Operator();
		MatchEntry OR = new Operator();
		MatchEntry ABS_TRUE = new BooleanType(true);
		MatchEntry ABS_FALSE = new BooleanType(false);

		class Static implements MatchEntry { protected Static() {} }

		class Operator implements MatchEntry { protected Operator() {} }

		class BooleanType implements MatchEntry {
			final boolean val;

			protected BooleanType(boolean val) {
				this.val = val;
			}

			static protected BooleanType from(boolean val) {
				return (BooleanType)(val ? ABS_TRUE : ABS_FALSE);
			}
		}

		class Lookup implements MatchEntry {
			final String id;
			final String expected;

			protected Lookup(String id, String expected) {
				this.id = id;
				this.expected = expected;
			}
		}

		class Contains implements MatchEntry {
			final String id;

			protected Contains(String id) {
				this.id = id;
			}
		}
	}
}
