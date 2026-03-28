package com.techwerx.image;

import com.techwerx.image.utils.ExclusionList;

public class Pair {

	private final int a;
	private final int b;

	public Pair(int a, int b) {
		this.a = a;
		this.b = b;
	}

	public boolean containedIn(ExclusionList<Pair> list) {
		return list.has(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Pair)) {
			return false;
		}
		Pair other = (Pair) o;
		return this.a == other.a && this.b == other.b;
	}

	@Override
	public int hashCode() {
		return 31 * a + b;
	}

}