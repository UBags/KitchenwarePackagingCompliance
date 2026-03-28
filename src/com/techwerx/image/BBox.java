package com.techwerx.image;

import java.awt.Rectangle;

public class BBox {
	public Rectangle rectangle;
	public float confidence;
	public String word;

	public BBox(Rectangle rectangle, float confidence, String word) {
		this.rectangle = rectangle;
		this.confidence = confidence;
		this.word = word;
	}

	@Override
	public String toString() {
		return (new StringBuffer("Rectangle : [x1,y1] = [").append(this.rectangle.x).append(",")
				.append(this.rectangle.y).append("] ; width = ").append(this.rectangle.width).append(" ; height = ")
				.append(this.rectangle.height).append(" ; confidence = ").append(this.confidence)).append(" ; Word = ")
						.append(this.word).toString();
	}
}
