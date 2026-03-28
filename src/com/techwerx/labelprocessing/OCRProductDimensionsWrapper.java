package com.techwerx.labelprocessing;

import java.awt.Rectangle;
import java.util.ArrayList;

public class OCRProductDimensionsWrapper extends OCRDimensionsWrapper {

	public OCRProductDimensionsWrapper(int likelyPixelHeight, String ocrString, ArrayList<Rectangle> boundingBoxes) {
		super(likelyPixelHeight, ocrString, boundingBoxes);
		this.setType(OCRDimensionsWrapper.PRODUCT);
	}

	@Override
	public void process(int baselinePixelHeight, double baselineActualHeight) {
		if ((baselineActualHeight != 0) && (baselinePixelHeight != 0)) {
			this.likelyActualHeight = (baselineActualHeight * this.likelyPixelHeight) / baselinePixelHeight;
		}
	}

}
