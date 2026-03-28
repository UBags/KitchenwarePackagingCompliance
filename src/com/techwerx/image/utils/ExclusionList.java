package com.techwerx.image.utils;

import java.util.ArrayList;

@SuppressWarnings("hiding")
public class ExclusionList<Pair> extends ArrayList<Pair> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public ExclusionList() {

	}

	@Override
	public boolean add(Pair pair) {
		return super.add(pair);
	}

	public boolean has(Pair pair) {
		if (this.isEmpty()) {
			return false;
		}
		for (Pair p : this) {
			if (p.equals(pair)) {
				return true;
			}
		}
		return false;
	}
}
