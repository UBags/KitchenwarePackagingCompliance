/**
 * Copyright (c) 2011, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.techwerx.image.thresholds;

import java.awt.image.BufferedImage;

import com.techwerx.image.SBImage;
import com.techwerx.image.XYDivisions;
import com.techwerx.image.utils.NumberTriplet;
import com.techwerx.image.utils.SBImageArrayUtils;
import com.techwerx.pdf.PDFHandlerChartFactory;

/**
 * Otsu's adaptive thresholding algorithm.
 *
 * @see <a href=
 *      "http://en.wikipedia.org/wiki/Otsu's_method">http://en.wikipedia.org/wiki/Otsu&apos;s_method</a>
 *
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 */
/*
 * @Reference(type = ReferenceType.Article, author = { "Nobuyuki Otsu" }, title
 * = "A Threshold Selection Method from Gray-Level Histograms", year = "1979",
 * journal = "Systems, Man and Cybernetics, IEEE Transactions on", pages = {
 * "62", "66" }, number = "1", volume = "9", customData = { "keywords",
 * "Displays;Gaussian distribution;Histograms;Least squares approximation;Marine vehicles;Q measurement;Radar tracking;Sea measurements;Surveillance;Target tracking"
 * , "doi", "10.1109/TSMC.1979.4310076", "ISSN", "0018-9472" })
 */

public class OtsuThreshold {
	private static final int DEFAULT_NUM_BINS = 256;
	int numBins = DEFAULT_NUM_BINS;
	static int index = 0; // remove later. Purpose is only to be able to draw the histogram
	public static final boolean debug = false;

	/**
	 * Default constructor
	 */
	private OtsuThreshold() {
	}

	/**
	 * Construct with the given number of histogram bins
	 *
	 * @param numBins the number of histogram bins
	 */
	public OtsuThreshold(int numBins) {
		this.numBins = numBins;
	}

	public static int[] makeHistogram(BufferedImage img) {
		return makeHistogram(img, DEFAULT_NUM_BINS);
	}

	public static int[] makeHistogram(BufferedImage img, int numBins) {
		try {
			return makeHistogram(SBImage.getSBImageFromBufferedImage(img), numBins);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static int[] makeHistogram(SBImage img) {
		return makeHistogram(img, DEFAULT_NUM_BINS);
	}

	public static int[] makeHistogram(SBImage img, int numBins) {

		final int[] histData = new int[numBins];
		for (final int[] row : img.pixels) {
			for (final int pixel : row) {
				histData[pixel]++;
			}
		}
		return histData;
	}

	public static int[] makeMinMaxHistogram(BufferedImage bi, int numBins) {
		try {
			return makeMinMaxHistogram(SBImage.getSBImageFromBufferedImage(bi).pixels, numBins);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static int[] makeMinMaxHistogram(BufferedImage bi) {
		try {
			return makeMinMaxHistogram(SBImage.getSBImageFromBufferedImage(bi).pixels, DEFAULT_NUM_BINS);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static int[] makeMinMaxHistogram(SBImage sbid) {
		return makeMinMaxHistogram(sbid.pixels, DEFAULT_NUM_BINS);
	}

	public static int[] makeMinMaxHistogram(SBImage sbid, int numBins) {
		return makeMinMaxHistogram(sbid.pixels, numBins);
	}

	public static int[] makeMinMaxHistogram(int[][] data, int numBins) {
		final int[] histData = new int[numBins];
		NumberTriplet ffp = NumberTriplet.findMinMax(data, 0, 0);
		// System.out.println("Min and Max are : " + ffp.first + " and " + ffp.second);
		for (int[] row : data) {
			for (int pixel : row) {
				int d = (ffp.first != ffp.second)
						? (int) ((((pixel - ffp.first) * 1.0) / (ffp.second - ffp.first)) * (numBins - 1))
						: pixel;
				histData[d]++;
			}
		}
		// System.out.println(Arrays.toString(histData));
		return histData;
	}

	public static int[] makeMinMaxHistogram(double[] data, int numBins) {
		final int[] histData = new int[numBins];
		NumberTriplet ffp = NumberTriplet.findMinMax(data, 0);
		for (double pixel : data) {
			int d = (ffp.first != ffp.second)
					? (int) (((pixel - ffp.first) * 1.0) / (ffp.second - ffp.first)) * (numBins - 1)
					: (int) pixel;
			histData[d]++;
		}
		return histData;
	}

	public static int[] makeMinMaxHistogram(double[] data, int numBins, double min, double max) {
		final int[] histData = new int[numBins];
		for (double pixel : data) {
			int d = (min != max) ? (int) (((pixel - min) * 1.0) / (max - min)) * (numBins - 1) : (int) pixel;
			histData[d]++;
		}
		return histData;
	}

	public static int calculateThreshold(SBImage img, int debugL) throws Exception {

		return calculateThreshold(img, DEFAULT_NUM_BINS, debugL);
	}

	/**
	 * Estimate the threshold for the given image.
	 *
	 * @param img     the image
	 * @param numBins the number of histogram bins
	 * @return the estimated threshold
	 */
	public static int calculateThreshold(SBImage img, int imageNumber, int debugL) throws Exception {
		final int[] histData = makeMinMaxHistogram(img, 256);

		// Total number of pixels
		final int total = img.width * img.height;
		final int threshold = (int) computeThresholdFromHistogram(histData, total);
		if (debugL <= 3) {
			// if (debugL <= 4) {
			PDFHandlerChartFactory.drawBarChart(histData, threshold,
					"E:\\TechWerx\\Java\\Working\\" + index++ + "-histData.png", 3500, 1000, "Histogram plot",
					"Histogram plot", "Pixel values", "No Of Occurences");
			System.out.println(threshold);
		}
		return threshold;
	}

	/**
	 * Estimate the threshold for the given image, pixel-bin size = 4.
	 *
	 * @param img     the image
	 * @param numBins the number of histogram bins
	 * @return the estimated threshold
	 */
	public static int calculateThreshold1(SBImage img, int imageNumber, int debugL) throws Exception {
		final int[] histData = makeMinMaxHistogram(img, 256);

		// Total number of pixels
		final int total = img.width * img.height;
		int threshold = (int) computeThresholdFromHistogram1(histData, total, imageNumber, debugL);
		// re-scaling needed, as the original threshold has been calculated after
		// scaling contrast (see makeMinMaxHistogram's code)
		NumberTriplet ffp = NumberTriplet.findMinMax(img.pixels, 0, 0);
		double min = ffp.first;
		double max = ffp.second;
		if (min < max) {
			threshold = (int) (((threshold * (max - min)) / 255.0) + min);
		}
		return threshold;
	}

	/**
	 * Estimate the threshold for the given data.
	 * <p>
	 * Internally, the data will be min-max normalised before the histogram is
	 * built, and the specified number of bins will cover the entire
	 * <code>max-min</code> range. The returned threshold will have <code>min</code>
	 * added to it to return it to the original range.
	 *
	 * @param data    the data
	 * @param numBins the number of histogram bins
	 * @return the estimated threshold
	 */
	public static double calculateThreshold(int[][] data, int numBins) {
		final int[] histData = makeMinMaxHistogram(data, numBins);
		return computeThresholdFromHistogram(histData, data.length) + NumberTriplet.findMinMax(data, 0, 0).first;
	}

	/**
	 * Estimate the threshold and inter-class variance for the given data.
	 * <p>
	 * Internally, the data will be min-max normalised before the histogram is
	 * built, and the specified number of bins will cover the entire
	 * <code>max-min</code> range. The returned threshold will have <code>min</code>
	 * added to it to return it to the original range.
	 *
	 * @param data    the data
	 * @param numBins the number of histogram bins
	 * @return the estimated threshold and variance
	 */
	public static NumberTriplet calculateThresholdAndVariance(double[] data, int numBins) {
		NumberTriplet ffp = NumberTriplet.findMinMax(data, 0);
		final int[] histData = makeMinMaxHistogram(data, numBins, ffp.first, ffp.second);
		final NumberTriplet result = computeThresholdAndVarianceFromHistogram(histData, data.length);
		return new NumberTriplet(result.first + ffp.first, result.second);
	}

	/**
	 * Estimate the threshold for the given histogram.
	 *
	 * @param histData the histogram
	 * @param total    the total number of items in the histogram
	 * @return the estimated threshold
	 */
	public static double computeThresholdFromHistogram(int[] histData, int total) {
		return computeThresholdAndVarianceFromHistogram(histData, total).first;
	}

	/**
	 * Estimate the threshold and inter-class variance for the given histogram.
	 *
	 * @param histData the histogram
	 * @param total    the total number of items in the histogram
	 * @return the estimated threshold and variance
	 */
	public static NumberTriplet computeThresholdAndVarianceFromHistogram(int[] histData, int total) {
		final int numBins = histData.length;
		double sum = 0;
		// Weighted sum of all the pixels
		for (int t = 0; t < numBins; t++) {
			sum += t * histData[t];
		}

		double sumB = 0;
		int wB = 0;
		int wF = 0;

		double varMax = 0;
		double threshold = 0;

		for (int t = 0; t < numBins; t++) {
			// if (histData[t] == 0) {
			// continue;
			// }

			wB += histData[t]; // Weight Background
			if (wB == 0) {
				continue;
			}

			wF = total - wB; // Weight Foreground
			if (wF == 0) {
				break;
			}

			sumB += (t * histData[t]);

			final double mB = sumB / wB; // Mean Background
			final double mF = (sum - sumB) / wF; // Mean Foreground

			// Calculate Between Class Variance
			final double varBetween = (double) wB * (double) wF * (mB - mF) * (mB - mF);

			// Check if new maximum found
			if (varBetween > varMax) {
				varMax = varBetween;
				threshold = t;
			}
		}
		// System.out.println("Threshold = " + threshold + "; ImageStatsHelper = " +
		// (varMax /
		// total / total));
		// return new NumberTriplet(threshold / (numBins - 1), varMax / total / total);
		return new NumberTriplet(threshold, varMax / total / total);
	}

	/**
	 * Estimate the threshold for the given histogram, pixel-bin size of 4
	 *
	 * @param histData the histogram
	 * @param total    the total number of items in the histogram
	 * @return the estimated threshold
	 */
	public static double computeThresholdFromHistogram1(int[] histData, int total, int imageNumber, int debugL) {
		return computeThresholdAndVarianceFromHistogram1(histData, total, imageNumber, debugL).first;
	}

	/**
	 * Estimate the threshold and inter-class variance for the given histogram,
	 * after consolidating the histogram into groups of 4
	 *
	 * @param histData the original 1-pixel histogram
	 * @param total    the total number of items in the histogram
	 * @return the estimated threshold and variance
	 */
	public static NumberTriplet computeThresholdAndVarianceFromHistogram1(int[] origHistData, int total,
			int imageNumber, int debugL) {
		int binSize = 8;
		final int originalNumBins = origHistData.length;
		final int numBins = ((originalNumBins % binSize) != 0) ? (originalNumBins / binSize) + 1
				: (originalNumBins / binSize);

		int[] histData = new int[numBins];
		if ((originalNumBins % binSize) != 0) {
			for (int i = 0; i < (numBins - 1); i++) {
				int binTotal = 0;
				for (int j = 0; j < binSize; ++j) {
					binTotal += origHistData[(binSize * i) + j];
				}
				histData[i] = binTotal;
			}
			int remainingBars = originalNumBins % binSize;
			int lastBarCount = 0;
			for (int i = 0; i <= remainingBars; i++) {
				lastBarCount += origHistData[originalNumBins - 1 - i];
			}
			histData[numBins - 1] = lastBarCount;
		} else {
			for (int i = 0; i < numBins; i++) {
				int binTotal = 0;
				for (int j = 0; j < binSize; ++j) {
					binTotal += origHistData[(binSize * i) + j];
				}
				histData[i] = binTotal;
			}
		}

		double sum = 0;
		// Weighted sum of all the pixels
		for (int t = 0; t < numBins; t++) {
			sum += t * histData[t];
		}

		double sumB = 0;
		int wB = 0;
		int wF = 0;

		double varMax = 0;
		int threshold = 0;

		for (int t = 0; t < numBins; t++) {
			// if (histData[t] == 0) {
			// continue;
			// }

			wB += histData[t]; // Weight Background
			if (wB == 0) {
				continue;
			}

			wF = total - wB; // Weight Foreground
			if (wF == 0) {
				break;
			}

			sumB += (t * histData[t]);

			final double mB = sumB / wB; // Mean Background
			final double mF = (sum - sumB) / wF; // Mean Foreground

			// Calculate Between Class Variance
			final double varBetween = (double) wB * (double) wF * (mB - mF) * (mB - mF);

			// Check if new maximum found
			if (varBetween > varMax) {
				varMax = varBetween;
				threshold = t;
			}
		}

		int currentOtsuThreshold = (binSize * threshold) + (binSize / 2);

		// add 15 to the current Otsu threshold, or 10% of current Threshold (experience
		// suggests that it is
		// generally better to have a higher threshold than calculated)
		currentOtsuThreshold = Math.min(Math.min(currentOtsuThreshold + 15, (int) (currentOtsuThreshold * 1.1)), 255);

		// if (debugL <= 3) {
		if (debugL <= 1) {
			try {
				System.out.println("Initial Otsu Threshold for Image " + imageNumber + " = " + currentOtsuThreshold);
			} catch (Exception e) {
			}
		}

		// construct cdf of CI 4 bars, find the average slope between 4 bars of the cdf,
		// and find that bar beyond the Otsu threshold where the slope is double that of
		// the average

		// create the cdf

		final int classInterval = 4;
		int binSize1 = classInterval;
		final int numBins1 = ((originalNumBins % binSize1) != 0) ? (originalNumBins / binSize1) + 1
				: (originalNumBins / binSize1);

		int[] histData1 = new int[numBins1];
		if ((originalNumBins % binSize1) != 0) {
			for (int i = 0; i < (numBins1 - 1); i++) {
				int binTotal1 = 0;
				for (int j = 0; j < binSize1; ++j) {
					binTotal1 += origHistData[(binSize1 * i) + j];
				}
				histData1[i] = binTotal1;
			}
			int remainingBars1 = originalNumBins % binSize1;
			int lastBarCount1 = 0;
			for (int i = 0; i <= remainingBars1; i++) {
				lastBarCount1 += origHistData[originalNumBins - 1 - i];
			}
			histData1[numBins1 - 1] = lastBarCount1;
		} else {
			for (int i = 0; i < numBins1; i++) {
				int binTotal1 = 0;
				for (int j = 0; j < binSize1; ++j) {
					binTotal1 += origHistData[(binSize1 * i) + j];
				}
				histData1[i] = binTotal1;
			}
		}

		int[] cdf = new int[histData1.length];
		cdf[0] = histData1[0];
		for (int i = 1; i < histData1.length; ++i) {
			cdf[i] = cdf[i - 1] + histData1[i];
		}

		// find which bin the Otsu threshold is in
		int currentOtsuBin = (currentOtsuThreshold) / classInterval;

		int startingBin = Math.max(0, Math.min(11, currentOtsuBin - 10));

		int cdfDifference = cdf[currentOtsuBin] - cdf[startingBin];
		int nBins = currentOtsuBin - startingBin;
		double averageSlope = (cdfDifference * 1.0) / nBins;

		if (debugL <= 1) {
			System.out.println("startingBin = " + startingBin + "; currentOtsuBin = " + currentOtsuBin
					+ "; averageSlope = " + averageSlope);
		}

		int binDistance = 5;
		int requiredBin = currentOtsuBin;
		for (int i = 0; i < (cdf.length - currentOtsuBin - binDistance); ++i) {
			requiredBin = i + currentOtsuBin;
			if ((((cdf[i + binDistance + currentOtsuBin] - cdf[i + currentOtsuBin]) * 1.0) / binDistance) > (1.75
					* averageSlope)) {
				if (debugL <= 1) {
					System.out.println("Found new bin at " + (i + currentOtsuBin) + " where slope = "
							+ (((cdf[i + binDistance + currentOtsuBin] - cdf[i + currentOtsuBin]) * 1.0)
									/ binDistance));
				}
				break;
			}
		}

		int newThreshold = (requiredBin * classInterval) + (classInterval / 2);

		// adjust threshold to 2/3rd distance between Otsu threshold and new threshold
		double finalThreshold = ((2.0 / 3) * newThreshold) + ((1.0 / 3) * currentOtsuThreshold);

		// check if there is a nearby minima (within +/10%) in the histData chart

		/*
		 * int tolerance = (int) (threshold * 0.1); int minimum = Math.max(0, threshold
		 * - tolerance); // int minimum = Math.max(0, threshold); int maximum =
		 * Math.min(256 / binSize, threshold + tolerance);
		 *
		 * int currentMinima = histData[threshold]; for (int i = minimum; i <= maximum;
		 * ++i) { if (histData[i] < currentMinima) { currentMinima = histData[i];
		 * threshold = i; } }
		 */

		// if (debugL <= 3) {
		if (debugL <= 1) {
			try {
				PDFHandlerChartFactory.drawBarChart(histData, (int) finalThreshold,
						"E:\\TechWerx\\Java\\Working\\" + imageNumber + "-histData.png", 3500, 1000, "Histogram plot",
						"Histogram plot", "Pixel values", "No Of Occurences");
				// System.out.println("Final Otsu Threshold = " + ((binSize * threshold) +
				// (binSize / 2)));
				System.out.println("Final Otsu Threshold = " + threshold);
			} catch (Exception e) {
			}
		}

		// System.out.println("Threshold = " + threshold + "; ImageStatsHelper = " +
		// (varMax /
		// total / total));
		// return new NumberTriplet(threshold / (numBins - 1), varMax / total / total);
		// return new NumberTriplet((binSize * threshold) + (binSize / 2) + 20, (varMax
		// / binSize) / total / total);
		return new NumberTriplet(finalThreshold, (varMax / binSize) / total / total);
	}

	public static SBImage processSubImages(SBImage image) throws Exception {
		return processSubImages(image, DEFAULT_NUM_BINS);
	}

	public static SBImage processSubImages(SBImage image, int numBins) throws Exception {
		SBImage[] subImages = image.createSubImageArray();
		SBImage[] processedImages = new SBImage[subImages.length];
		int i = 0;
		for (SBImage subImage : subImages) {
			final double threshold = calculateThreshold(subImage, numBins);
			processedImages[i++] = otsuThresholdSBImage(subImage, threshold, true);
		}
		return SBImage.stitchSubImages(processedImages, image.xDivisions, image.yDivisions);
	}

	public static SBImage processSubImages(SBImage image, int xDivisions, int yDivisions, int debugL) throws Exception {
		return processSubImages(image, xDivisions, yDivisions, DEFAULT_NUM_BINS, debugL);
	}

	public static SBImage processSubImages(SBImage image, int xDivisions, int yDivisions, int numBins, int debugL)
			throws Exception {
		XYDivisions divs = new XYDivisions(xDivisions, yDivisions);
		SBImage[] subImages = image.createSubImageArray(divs);
		SBImage[] processedImages = new SBImage[subImages.length];
		int i = 0;
		for (SBImage subImage : subImages) {
			final double threshold = calculateThreshold(subImage, numBins);
			if (debugL <= 3) {
				System.out.println("Otsu Threshold = " + threshold);
			}
			processedImages[i++] = otsuThresholdSBImage(subImage, threshold, true);
		}
		return SBImage.stitchSubImages(processedImages, xDivisions, yDivisions);
	}

	public static SBImage processSubImages1(SBImage image, int xDivisions, int yDivisions, int debugL)
			throws Exception {
		return processSubImages1(image, xDivisions, yDivisions, DEFAULT_NUM_BINS, debugL);
	}

	public static SBImage processSubImages1(SBImage image, int xDivisions, int yDivisions, int imageNumber, int debugL)
			throws Exception {
		return processSubImages1(image, 0, xDivisions, yDivisions, imageNumber, debugL);
	}

	public static SBImage processSubImages1(SBImage image, int thresholdBuffer, int xDivisions, int yDivisions,
			int imageNumber, int debugL) throws Exception {
		XYDivisions divs = new XYDivisions(xDivisions, yDivisions);
		SBImage[] subImages = image.createSubImageArray(divs);
		if (debugL <= 3) {
			System.out.println("Image : " + imageNumber + " has " + subImages.length + " subimages ");
		}
		SBImage[] processedImages = new SBImage[subImages.length];
		int i = 0;
		for (SBImage subImage : subImages) {
			final double threshold = calculateThreshold1(subImage, (imageNumber * 100) + i, debugL);
			if (debugL <= 3) {
				System.out.println("Image : " + imageNumber + ": subImage - " + i + " - Otsu Threshold = " + threshold);
			}
			processedImages[i++] = otsuThresholdSBImage(subImage, threshold + thresholdBuffer, true);
		}
		return SBImage.stitchSubImages(processedImages, xDivisions, yDivisions);
	}

	public static SBImage processImage(SBImage image) throws Exception {
		return processImage(image, DEFAULT_NUM_BINS, false);
	}

	public static SBImage processImage(SBImage image, int numBins) throws Exception {
		return processImage(image, numBins, false);
	}

	public static SBImage processImage(SBImage image, int numBins, boolean processSubImages) throws Exception {
		if (processSubImages) {
			return processSubImages(image, numBins);
		}
		final double threshold = calculateThreshold(image, numBins);
		return otsuThresholdSBImage(image, threshold, true);
	}

	public static SBImage otsuThresholdSBImage(SBImage sbid, double threshold, boolean recurse) {
		int[][] pixels = SBImageArrayUtils.arrayCopy(sbid.pixels);
		int total = 0;
		for (int[] row : pixels) {
			int i = 0;
			for (int pixel : row) {
				if (pixel < threshold) {
					row[i] = 0;
				} else {
					row[i] = 255;
					total += 255;
				}
				++i;
			}
		}
		double pixelAverage = (total * 1.0) / (sbid.height * sbid.width);
		if ((pixelAverage < 128) && recurse) {
			return otsuThresholdSBImage(sbid, 255 - threshold, false);
		}
		try {
			return new SBImage(pixels, false, false, false);
		} catch (InterruptedException ie) {
		}
		return null;
	}
}
