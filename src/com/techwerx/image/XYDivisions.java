package com.techwerx.image;

public class XYDivisions {

	public int xDivisions = 1;
	public int yDivisions = 1;

	public XYDivisions(int xDivisions, int yDivisions) {
		this.xDivisions = Math.max(1, xDivisions);
		this.yDivisions = Math.max(1, yDivisions);
	}

}
