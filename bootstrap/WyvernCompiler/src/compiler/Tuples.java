/**
 * 
 */
package compiler;

/**
 * @author Michael
 * 
 */
public class Tuples {

	public static class Duo<T1, T2> {
		private final T1 item1;
		private final T2 item2;
		protected int hashCode = Integer.MAX_VALUE;

		public Duo(T1 item1, T2 item2) {
			this.item1 = item1;
			this.item2 = item2;
		}

		public final T1 item1() {
			return this.item1;
		}

		public final T2 item2() {
			return this.item2;
		}

		@SuppressWarnings("unchecked")
		@Override
		public final boolean equals(Object obj) {
			if (this == obj)
				return true;
			Duo<T1, T2> that = Utils.cast(obj, Duo.class);
			return that != null && Utils.equals(this.item1(), that.item1())
					&& Utils.equals(this.item2(), that.item2());
		}

		@Override
		public final int hashCode() {
			return this.hashCode == Integer.MAX_VALUE ? (this.hashCode = Utils
					.hashCode(this.item1()) ^ Utils.hashCode(this.item2()))
					: this.hashCode;
		}

		@Override
		public String toString() {
			return String.format("(%s, %s)", this.item1(), this.item2());
		}
	}

	public static class Trio<T1, T2, T3> {
		private final T1 item1;
		private final T2 item2;
		private final T3 item3;
		protected int hashCode = Integer.MAX_VALUE;

		public Trio(T1 item1, T2 item2, T3 item3) {
			this.item1 = item1;
			this.item2 = item2;
			this.item3 = item3;
		}

		public final T1 item1() {
			return this.item1;
		}

		public final T2 item2() {
			return this.item2;
		}

		public final T3 item3() {
			return this.item3;
		}

		@SuppressWarnings("unchecked")
		@Override
		public final boolean equals(Object obj) {
			if (this == obj)
				return true;
			Trio<T1, T2, T3> that = Utils.cast(obj, Trio.class);
			return that != null && Utils.equals(this.item1(), that.item1())
					&& Utils.equals(this.item2(), that.item2())
					&& Utils.equals(this.item3(), that.item3());
		}

		@Override
		public final int hashCode() {
			return this.hashCode == Integer.MAX_VALUE ? (this.hashCode = Utils
					.hashCode(this.item1())
					^ Utils.hashCode(this.item2())
					^ Utils.hashCode(this.item3())) : this.hashCode;
		}

		@Override
		public String toString() {
			String name = this.getClass() == Trio.class ? "" : this.getClass()
					.getSimpleName();

			return String.format("%s(%s, %s, %s)", name, this.item1(),
					this.item2(), this.item3());
		}
	}

	private Tuples() {
	}
}
