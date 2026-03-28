package com.techwerx.labelprocessing;

import java.awt.Rectangle;
import java.util.ArrayList;

public abstract class OCRDimensionsWrapper {

	public static int PRODUCT = 1;
	public static int DATE = 2;
	public static int PRICE = 3;
	public static int UNKNOWN = 0;

	public int likelyPixelHeight = 0;
	public double likelyActualHeight = 0;
	public String ocrString = "";
	public ArrayList<Rectangle> boundingBoxes = null;
	public int type = UNKNOWN;

	public double minimumAllowedHeight = 9;
	public boolean heightOK = false;
	public String reasonForRejection = "";
	public int debugLevel;

	public static int allowableHeightDeviationInPixels = 3; // 3 pixel dimension deviation in bounding box (2 on 1 side
															// and 1 on the other)
	public double percentError = 0.0; // this needs to be calculated by each subclass
	// percentError = (baselineActualHeight * 1.0 / likelyActualHeight) *
	// (Math.abs(likelyPixelHeight - baselinePixelHeight) * 1.0 /
	// baselinePixelHeight)

	public OCRDimensionsWrapper(int likelyPixelHeight, String ocrString, ArrayList<Rectangle> boundingBoxes) {
		this.likelyPixelHeight = likelyPixelHeight;
		this.ocrString = ocrString;
		this.boundingBoxes = boundingBoxes;
	}

	protected abstract void process(int baselinePixelHeight, double baselineActualHeight);

	public void setType(int type) {
		this.type = type;
	}

	/**
	 * @return the debugLevel
	 */
	public int getDebugLevel() {
		return this.debugLevel;
	}

	/**
	 * @param debugLevel the debugLevel to set
	 */
	public void setDebugLevel(int debugLevel) {
		this.debugLevel = debugLevel;
	}
}
