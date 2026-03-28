package com.techwerx.labelprocessing;

import java.awt.Rectangle;
import java.util.ArrayList;

public class OCRStringWrapper {

	public int likelyPixelHeight;
	public String ocrString;
	public ArrayList<Rectangle> boundingBoxes;

	public OCRStringWrapper(int likelyHeight, String ocrString, ArrayList<Rectangle> boundingBoxes) {
		this.likelyPixelHeight = likelyHeight;
		this.ocrString = ocrString;
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
	 * @return the ocrString
	 */
	public String getOcrString() {
		return this.ocrString;
	}

	/**
	 * @param ocrString the ocrString to set
	 */
	public void setOcrString(String ocrString) {
		this.ocrString = ocrString;
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
