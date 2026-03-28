package com.techwerx.text;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class OCRBufferedImageWrapper {

	public int likelyPixelHeight;
	public BufferedImage image;
	public ArrayList<Rectangle> boundingBoxes;

	public OCRBufferedImageWrapper(int likelyHeight, BufferedImage image, ArrayList<Rectangle> boundingBoxes) {
		this.likelyPixelHeight = likelyHeight;
		this.image = image;
		this.boundingBoxes = boundingBoxes;
	}

	/**
	 * @return the likelyPixelHeight
	 */
	public int getLikelyPixelHeight() {
		return this.likelyPixelHeight;
	}

	/**
	 * @param likelyPixelHeight the likelyPixelHeight to set
	 */
	public void setLikelyPixelHeight(int likelyPixelHeight) {
		this.likelyPixelHeight = likelyPixelHeight;
	}

	/**
	 * @return the image
	 */
	public BufferedImage getImage() {
		return this.image;
	}

	/**
	 * @param image the image to set
	 */
	public void setImage(BufferedImage image) {
		this.image = image;
	}

	/**
	 * @return the boundingBoxes
	 */
	public ArrayList<Rectangle> getBoundingBoxes() {
		return this.boundingBoxes;
	}

	/**
	 * @param boundingBoxes the boundingBoxes to set
	 */
	public void setBoundingBoxes(ArrayList<Rectangle> boundingBoxes) {
		this.boundingBoxes = boundingBoxes;
	}

}
