package com.techwerx.image;

public class SBSubImageCoordinates {

	public final int startX;
	public final int startY;
	public final int height;
	public final int width;
	public final boolean topIsEdge;
	public final boolean rightIsEdge;
	public final boolean bottomIsEdge;
	public final boolean leftIsEdge;

	public SBSubImageCoordinates(int startX, int startY, int height, int width, boolean topIsEdge, boolean rightIsEdge,
			boolean bottomIsEdge, boolean leftIsEdge) {
		this.startX = startX;
		this.startY = startY;
		this.height = height;
		this.width = width;
		this.topIsEdge = topIsEdge;
		this.rightIsEdge = rightIsEdge;
		this.bottomIsEdge = bottomIsEdge;
		this.leftIsEdge = leftIsEdge;
	}

	@Override
	public String toString() {
		return new StringBuffer("X :").append(this.startX).append("; Y :").append(this.startY).append("; Height :")
				.append(this.height).append("; Width :").append(this.width).append("; topIsEdge :")
				.append(this.topIsEdge).append("; rightIsEdge :").append(this.rightIsEdge).append("; bottomIsEdge :")
				.append(this.bottomIsEdge).append("; leftIsEdge :").append(this.leftIsEdge).toString();
	}
}
