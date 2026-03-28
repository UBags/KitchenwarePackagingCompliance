package com.techwerx.image;

/**
 * @author Admin
 *
 */

public class DimensionScaling {

	public double heightScaleFactor;
	public double widthScaleFactor;

	public DimensionScaling(double heightScaleFactor, double widthScaleFactor) {
		this.heightScaleFactor = heightScaleFactor;
		this.widthScaleFactor = widthScaleFactor;
	}

	@Override
	public String toString() {
		StringBuffer out = new StringBuffer();
		return out.append("widthScale = ").append(this.widthScaleFactor).append("; heightScale = ")
				.append(this.heightScaleFactor).toString();
	}

}
