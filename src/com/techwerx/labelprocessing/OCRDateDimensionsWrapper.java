package com.techwerx.labelprocessing;

import java.awt.Rectangle;
import java.util.ArrayList;

public class OCRDateDimensionsWrapper extends OCRDimensionsWrapper {

	public static final String minPriceHeightKey = "date.minimumheight";
	public static final String dateAndMonthHeightErrorKey = "dateandmonthheight.error";

	public OCRDateDimensionsWrapper(int likelyPixelHeight, String ocrString, ArrayList<Rectangle> boundingBoxes) {
		super(likelyPixelHeight, ocrString, boundingBoxes);
		this.minimumAllowedHeight = Integer.parseInt(System.getProperty(minPriceHeightKey));
		this.setType(OCRDimensionsWrapper.DATE);
	}

	@Override
	public void process(int baselinePixelHeight, double baselineActualHeight) {
		this.percentError = ((baselineActualHeight * 1.0) / Math.max(1, this.likelyActualHeight))
				* ((Math.abs(this.likelyPixelHeight - baselinePixelHeight) * 1.0) / Math.max(1, baselinePixelHeight));
		if (this.debugLevel <= 2) {
			System.out.println("In OCRDateDimensionsWrapper.process() : ocrString = " + this.ocrString);
			System.out.println("In OCRDateDimensionsWrapper.process() : likelyPixelHeight = " + this.likelyPixelHeight);
			System.out.println("In OCRDateDimensionsWrapper.process() : boundingBoxes = " + this.boundingBoxes);
			System.out.println("In OCRDateDimensionsWrapper.process() : baselineActualHeight = " + baselineActualHeight
					+ "; baselinePixelHeight = " + baselinePixelHeight);
		}

		if ((baselineActualHeight != 0) && (baselinePixelHeight != 0)) {
			this.likelyActualHeight = (baselineActualHeight * this.likelyPixelHeight) / baselinePixelHeight;
			this.likelyActualHeight = (1 + this.percentError) * this.likelyActualHeight;
			if (this.likelyActualHeight >= this.minimumAllowedHeight) {
				this.heightOK = true;
			} else {
				this.heightOK = false;
				this.reasonForRejection = this.reasonForRejection
						+ String.format(System.getProperty(dateAndMonthHeightErrorKey), this.likelyActualHeight,
								this.minimumAllowedHeight);
			}
		}
	}

}