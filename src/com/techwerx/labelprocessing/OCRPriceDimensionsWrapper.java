package com.techwerx.labelprocessing;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.techwerx.labelprocessing.prestige.CheckProductProperties;
import com.techwerx.labelprocessing.prestige.ProductPriceData;

public class OCRPriceDimensionsWrapper extends OCRDimensionsWrapper {

	public static final String minPriceHeightKey = "price.minimumheight";
	public static final String minimumMultipleOfOtherGapsKey = "spacing.minimummultipleofgaps";
	public static final String minimumMultipleOfAverageWidthKey = "spacing.minimummultipleofchars";
	public static final String priceHeightErrorKey = "priceheight.error";
	public static final String priceSpacingErrorDueToWidthKey = "pricespacingwidth.error";
	public static final String priceSpacingErrorDueToGapKey = "pricespacinggap.error";
	public static final String priceSpacingsNeededKey = "needed.pricespacingsok";
	public static final String bothPriceSpacingsNeededValue = "both";

	public static final double minimumMultipleOfOtherGaps = Double
			.parseDouble(System.getProperty(minimumMultipleOfOtherGapsKey));
	public static final double minimumMultipleOfAverageWidth = Double
			.parseDouble(System.getProperty(minimumMultipleOfAverageWidthKey));
	public static final boolean bothPriceSpacingsNeeded = bothPriceSpacingsNeededValue
			.equals(System.getProperty(priceSpacingsNeededKey));

	public double gapBetween1And2 = -1;
	public int gapBetweenRest = -1;
	public int widthOfNumbers = -1;
	public boolean distanceOK = false;
	public String reasonForDistanceRejection = "";
	public String price = null;

	public OCRPriceDimensionsWrapper(int likelyPixelHeight, String ocrString, ArrayList<Rectangle> boundingBoxes,
			String price) {
		super(likelyPixelHeight, ocrString, boundingBoxes);
		this.minimumAllowedHeight = Integer.parseInt(System.getProperty(minPriceHeightKey));
		this.setType(OCRDimensionsWrapper.PRICE);
		this.price = price;
	}

	@Override
	public void process(int baselinePixelHeight, double baselineActualHeight) {
		if (this.debugLevel <= 2) {
			System.out.println("In OCRPriceDimensionsWrapper.process() : ocrString = " + this.ocrString);
			System.out.println("In OCRPriceDimensionsWrapper.process() : price received = " + this.price);
			System.out
					.println("In OCRPriceDimensionsWrapper.process() : likelyPixelHeight = " + this.likelyPixelHeight);
			System.out.println("In OCRPriceDimensionsWrapper.process() : boundingBoxes = " + this.boundingBoxes);
			System.out.println("In OCRPriceDimensionsWrapper.process() : baselineActualHeight = " + baselineActualHeight
					+ "; baselinePixelHeight = " + baselinePixelHeight);
		}
		if ((baselineActualHeight != 0) && (baselinePixelHeight != 0) && (this.boundingBoxes != null)) {
			// This section is valid only for the case when product is not given
			// Do not execute this section if the product is given.

			if (this.debugLevel <= 2) {
				System.out.println("In OCRPriceDimensionsWrapper.process() - entering the main block of if ()");
			}

			// clean the bounding boxes to ensure that small boxes corresponding to -,_,.
			// are removed. This is because the ocrString has been stripped off these as
			// well

			// remove small boxes
			int nBoundingBoxes = this.boundingBoxes.size();
			int startingY = 0;
			for (int i = nBoundingBoxes - 1; i >= 0; --i) {
				if (this.boundingBoxes.get(i).height < (this.likelyPixelHeight * 0.5)) {
					this.boundingBoxes.remove(i);
				} else {
					startingY += this.boundingBoxes.get(i).y;
				}
			}
			// remove boxes that start half-way below the top
			nBoundingBoxes = this.boundingBoxes.size();
			if (nBoundingBoxes > 0) {
				startingY = startingY / nBoundingBoxes;
				for (int i = nBoundingBoxes - 1; i >= 0; --i) {
					if (this.boundingBoxes.get(i).y > (startingY + (this.likelyPixelHeight * 0.5))) {
						this.boundingBoxes.remove(i);
					}
				}
			}
			// sort the remaining bounding boxes by x-coordinate
			synchronized (this.boundingBoxes) {
				Collections.sort(this.boundingBoxes, new Comparator<Rectangle>() {
					@Override
					public int compare(Rectangle o1, Rectangle o2) {
						return (o1.x) - (o2.x);
					}
				});
			}
			if (this.debugLevel <= 2) {
				System.out.println(
						"In OCRPriceDimensionsWrapper.process() : boundingBoxes after sorting = " + this.boundingBoxes);
			}

			this.likelyActualHeight = (baselineActualHeight * this.likelyPixelHeight) / baselinePixelHeight;
			this.percentError = ((baselineActualHeight * 1.0) / Math.max(1, this.likelyActualHeight))
					* ((Math.abs(this.likelyPixelHeight - baselinePixelHeight) * 1.0)
							/ Math.max(1, baselinePixelHeight));

			if (this.debugLevel <= 2) {
				System.out.println("In OCRPriceDimensionsWrapper.process() : percentError = " + this.percentError);
			}

			if ((this.likelyActualHeight * (1 + this.percentError)) >= this.minimumAllowedHeight) {
				this.heightOK = true;
			} else {
				this.heightOK = false;
				this.reasonForRejection = this.reasonForRejection + String.format(
						System.getProperty(priceHeightErrorKey), this.likelyActualHeight, this.minimumAllowedHeight);
			}

			// String newOcrString = this.ocrString.replace(" ", "").trim();
			String newOcrString = this.ocrString.toUpperCase().replace(" ", "").replace("_", "").replace("-", "")
					.replace("O", "0").replace(",", "").replace("(", " ").replace(")", " ").replace("[", " ")
					.replace("]", " ").replace("'", " ").replace("\"", " ").replace("-", " ").replace("+", "")
					.replace(":", "").replace("/", "").replace("=", "").replace("|", "1").replace("\\", "")
					.replace("Z", "2").replace("D", "0").replace("{", " ").replace("}", " ").replace("I", "1")
					.replace("T", "7").replace("1U", "").replace("2U", "").replace("3U", "").replace("4U", "")
					.replace("5U", "").replace("6U", "").replace("7U", "").replace("8U", "").trim();

			Matcher priceMatcher = ProductPriceData.pricePattern.matcher(newOcrString);
			if (priceMatcher.find()) {
				int start = priceMatcher.start();
				int end = priceMatcher.end();
				String tobeReplaced = newOcrString.substring(start, end).trim();
				newOcrString = newOcrString.replace(tobeReplaced, "");
			}

			Matcher retailMatcher = ProductPriceData.retailPattern.matcher(newOcrString);
			if (retailMatcher.find()) {
				int start = retailMatcher.start();
				int end = retailMatcher.end();
				String tobeReplaced = newOcrString.substring(start, end).trim();
				newOcrString = newOcrString.replace(tobeReplaced, "");
			}

			Matcher taxesMatcher = ProductPriceData.taxesPattern.matcher(newOcrString);
			if (taxesMatcher.find()) {
				int start = taxesMatcher.start();
				int end = taxesMatcher.end();
				String tobeReplaced = newOcrString.substring(start, end).trim();
				newOcrString = newOcrString.replace(tobeReplaced, "");
			}

			Matcher ofAllMatcher = ProductPriceData.ofAllPattern.matcher(newOcrString);
			if (ofAllMatcher.find()) {
				int start = ofAllMatcher.start();
				int end = ofAllMatcher.end();
				String tobeReplaced = newOcrString.substring(start, end).trim();
				newOcrString = newOcrString.replace(tobeReplaced, "");
			}

			Matcher rsMatcher = ProductPriceData.rsPattern.matcher(newOcrString);
			if (rsMatcher.find()) {
				int start = rsMatcher.start();
				int end = rsMatcher.end();
				String tobeReplaced = newOcrString.substring(start, end).trim();
				// remove Rs, and change "S" to "5" in the string
				newOcrString = newOcrString.replace(tobeReplaced, "").replace("S", "5");
				// Add back RS, by first checking for a 4-digit sequence
				Pattern sequence = Pattern.compile("\\d{4,5}");
				Matcher aMatcher = sequence.matcher(newOcrString);
				if (aMatcher.find()) {
					start = aMatcher.start();
					newOcrString = newOcrString.substring(0, start) + "RS" + newOcrString.substring(start);
				}
			}

			for (Pattern aPattern : ProductPriceData.ttkPrestigeLtd) {
				Matcher aMatcher = aPattern.matcher(newOcrString);
				if (aMatcher.find()) {
					int start = aMatcher.start();
					int end = aMatcher.end();
					String tobeReplaced = newOcrString.substring(start, end).trim();
					newOcrString = newOcrString.replace(tobeReplaced, "");
				}
			}

			if (this.debugLevel <= 2) {
				System.out.println("In OCRPriceDimensionsWrapper.process() : newOcrString = " + newOcrString);
			}
			Pattern numberSequence1 = Pattern.compile("\\d{3,}$"); // check for sequence of 3 or more numbers, followed
																	// by end of line
			Pattern numberSequence2 = Pattern.compile("\\d{3,}\\D"); // check for sequence of 3 or more numbers,
																		// followed by a non-digit character (. or _)

			Matcher matcher = numberSequence1.matcher(newOcrString);
			String priceAsString = null;
			int index = -1;
			if (matcher.find()) {
				priceAsString = matcher.group();
				index = matcher.start();
				if (this.debugLevel <= 2) {
					System.out.println("In OCRPriceDimensionsWrapper.process() : priceAsString = " + priceAsString
							+ "; index = " + index);
				}
			}
			if (index == -1) {
				matcher = numberSequence2.matcher(newOcrString);
				if (matcher.find()) {
					priceAsString = matcher.group();
					index = matcher.start();
					if (this.debugLevel <= 2) {
						System.out.println("In OCRPriceDimensionsWrapper.process() : priceAsString = " + priceAsString
								+ "; index = " + index);
					}
				}
			}
			if (index == -1) {
				this.gapBetween1And2 = -1;
				this.gapBetweenRest = -1;
				this.widthOfNumbers = -1;
				this.distanceOK = false;
				this.reasonForDistanceRejection = "Could not compare gaps";
				return;
			}

			/*
			 * newOcrString = newOcrString.replace(".", "").replace("_", "").replace("-",
			 * "").replace(",", "") .replace("+", "").replace(":", "").replace(" ",
			 * "").replace("Z", "2").replace("L", "1") .replace("B", "8").replace("C",
			 * "0").replace("D", "0").replace("I", "1").trim(); // // replace("S", // "5")
			 * removed // as it // replaces the // s in Rs 1120
			 */

			/*
			 * priceAsString = priceAsString.replace(".", "").replace("_", "").replace("-",
			 * "").replace(",", "") .replace("+", "").replace(":", "").replace(" ",
			 * "").trim();
			 */
			priceAsString = this.price;
			index = newOcrString.indexOf(priceAsString);

			DescriptiveStatistics wStats = new DescriptiveStatistics();
			for (Rectangle box : this.boundingBoxes) {
				wStats.addValue(box.width);
			}
			double averageCharWidth;
			if ((wStats.getStandardDeviation() / wStats.getMean()) < 0.2) {
				averageCharWidth = wStats.getMean();
			} else {
				averageCharWidth = (wStats.getN() * wStats.getMean()) / newOcrString.length();
			}

			try {
				int firstDigitBox = 0;
				int lastDigitBox = 0;

				// IF newOCRString ENDS IN 00, THEN COUNT FROM THE BACK
				// ELSE, COUNT FROM THE FRONT

				if (this.debugLevel <= 2) {
					System.out.println("In OCRPriceDimensionsWrapper.process() : expectedNumberOfBoxesAfterPrice = "
							+ (newOcrString.length() - index - priceAsString.length()) + "; newOcrString.length() = "
							+ newOcrString.length() + "; newOcrString = " + newOcrString + "; priceAsString = "
							+ priceAsString + "; index = " + index + "; averageCharWidth = " + averageCharWidth);
				}

				int expectedNumberOfBoxesBeforePrice = index;
				if (this.debugLevel <= 2) {
					System.out.println("In OCRPriceDimensionsWrapper.process() : expectedNumberOfBoxesBeforePrice = "
							+ expectedNumberOfBoxesBeforePrice + "; newOcrString.length() = " + newOcrString.length()
							+ "; newOcrString = " + newOcrString + "; priceAsString = " + priceAsString + "; index = "
							+ index + "; averageCharWidth = " + averageCharWidth);
				}
				int numberOfIntegers = priceAsString.length();
				if (this.debugLevel <= 2) {
					System.out.println("numberOfIntegers in price = " + numberOfIntegers);
				}

				// ArrayList<Rectangle> boundingBoxesInFocus = new ArrayList<>();
				firstDigitBox = 0;
				if (expectedNumberOfBoxesBeforePrice != 0) {
					int boxesCounted = 0;
					for (int i = 0; i < expectedNumberOfBoxesBeforePrice; ++i) {
						if (i == (expectedNumberOfBoxesBeforePrice - 1)) {
							firstDigitBox = i;
							break;
						}
						try {
							int boxesInThisLoop = (int) Math
									.round((this.boundingBoxes.get(i).width * 1.0) / averageCharWidth);
							boxesCounted += boxesInThisLoop;
							if (boxesCounted >= expectedNumberOfBoxesBeforePrice) {
								firstDigitBox = i + 1;
								break;
							}
						} catch (Exception e) {
							break;
						}
					}
				}
				// next, find the lastDigitBox
				lastDigitBox = firstDigitBox;
				int priceBoxesCounted = 0;
				for (int i = firstDigitBox; i < this.boundingBoxes.size(); ++i) {
					if (i == (this.boundingBoxes.size() - 1)) {
						lastDigitBox = i;
						break;
					}
					int pricesBoxesInThisLoop = (int) Math
							.round((this.boundingBoxes.get(i).width * 1.0) / averageCharWidth);
					priceBoxesCounted = priceBoxesCounted + pricesBoxesInThisLoop;
					if (priceBoxesCounted >= priceAsString.length()) {
						lastDigitBox = i;
						break;
					}
				}

				boolean canCompareGaps = true;
				int boxDistance = (lastDigitBox - firstDigitBox) + 1;
				if (boxDistance < priceAsString.length()) {
					canCompareGaps = false;
				}

				boolean firstDigitIsAlone = false;
				try {
					if ((this.boundingBoxes.get(firstDigitBox).width / averageCharWidth) < 1.25) {
						firstDigitIsAlone = true;
					}
				} catch (Exception e) {
					firstDigitIsAlone = false;
				}

				if ((firstDigitIsAlone) && ((firstDigitBox - lastDigitBox) >= 2)) {
					canCompareGaps = true;
				}

				boolean canProcessWidths = true;
				if (firstDigitBox == lastDigitBox) {
					canProcessWidths = true;
				}

				if (this.debugLevel <= 2) {
					System.out.println("In OCRPriceDimensionsWrapper.process() : canProcessWidths = " + canProcessWidths
							+ "; firstDigitIsAlone = " + firstDigitIsAlone + "; firstDigitBox = " + firstDigitBox
							+ "; lastDigitBox = " + lastDigitBox + "; canCompareGaps = " + canCompareGaps);
				}

				if (canProcessWidths) {

					try {
						for (int i = firstDigitBox; i <= lastDigitBox; ++i) {
							try {
								this.widthOfNumbers += this.boundingBoxes.get(i).width;
							} catch (Exception e) {

							}
						}
						this.widthOfNumbers = (int) ((this.widthOfNumbers * 1.0) / priceAsString.length());

						boolean multipleWidthOK = false;
						double widthRatio = 0.0;
						StringBuffer rejectReason = new StringBuffer();
						if (firstDigitIsAlone) {
							try {
								this.gapBetween1And2 = (this.boundingBoxes.get(firstDigitBox + 1).x
										- this.boundingBoxes.get(firstDigitBox).x
										- this.boundingBoxes.get(firstDigitBox).width) + 1;
								widthRatio = (this.gapBetween1And2 * 1.0) / this.widthOfNumbers;
							} catch (Exception e) {
								widthRatio = 0.0;
							}
							if ((widthRatio * (1 + this.percentError)) < minimumMultipleOfAverageWidth) {
								// this.distanceOK = false;
								multipleWidthOK = false;
								/*
								 * this.reasonForDistanceRejection = this.reasonForDistanceRejection +
								 * String.format(System.getProperty(priceSpacingErrorDueToWidthKey),
								 * ((this.gapBetween1And2 * baselineActualHeight) / baselinePixelHeight),
								 * minimumMultipleOfAverageWidth ((this.widthOfNumbers * baselineActualHeight) /
								 * baselinePixelHeight), minimumMultipleOfAverageWidth);
								 */
								String reason = String.format(System.getProperty(priceSpacingErrorDueToWidthKey),
										((this.gapBetween1And2 * (1 + this.percentError) * baselineActualHeight)
												/ baselinePixelHeight),
										minimumMultipleOfAverageWidth
												* ((this.widthOfNumbers * baselineActualHeight) / baselinePixelHeight),
										minimumMultipleOfAverageWidth);
								rejectReason.append(reason);
								if (this.debugLevel <= 2) {
									System.out.println(
											"In OCRPriceDimensionsWrapper.process() - rejection reason = " + reason);
								}
							} else {
								// this.distanceOK = true;
								multipleWidthOK = true;
							}

						}
						if (this.debugLevel <= 2) {
							System.out.println("In OCRPriceDimensionsWrapper.process() : widthofNumbers = "
									+ this.widthOfNumbers + "; gapBetween1And2 = " + this.gapBetween1And2
									+ "; distanceOK = " + this.distanceOK);
						}
						boolean multipleGapOK = true; // if we cannot compare with other gaps, it means the other gaps
														// are
														// small, which implies it is most likely that the gap between
														// the
														// first
														// and second digits is sufficient
						double gapRatio = minimumMultipleOfOtherGaps;
						if (firstDigitIsAlone && canCompareGaps) {

							for (int i = firstDigitBox + 1; i <= (lastDigitBox - 1); ++i) {
								try {
									this.gapBetweenRest += (this.boundingBoxes.get(i + 1).x
											- this.boundingBoxes.get(i).x - this.boundingBoxes.get(i).width) + 1;
								} catch (Exception e) {
									break;
								}
							}
							this.gapBetweenRest = (int) ((this.gapBetweenRest * 1.0) / (priceAsString.length() - 1));

							gapRatio = (this.gapBetween1And2 * 1.0) / this.gapBetweenRest;
							if ((gapRatio * (1 + this.percentError)) >= minimumMultipleOfOtherGaps) {
								// this.distanceOK = this.distanceOK && true;
								multipleGapOK = true;
							} else {
								// this.distanceOK = false;
								multipleGapOK = false;
								String reason = String.format(System.getProperty(priceSpacingErrorDueToGapKey),
										((this.gapBetween1And2 * (1 + this.percentError) * baselineActualHeight)
												/ baselinePixelHeight),
										minimumMultipleOfOtherGaps
												* ((this.gapBetweenRest * baselineActualHeight) / baselinePixelHeight));
								rejectReason.append(reason);
								if (this.debugLevel <= 2) {
									System.out.println(
											"In OCRPriceDimensionsWrapper.process() - rejection reason = " + reason);
								}
								/*
								 * this.reasonForDistanceRejection = this.reasonForDistanceRejection +
								 * String.format(System.getProperty(priceSpacingErrorDueToGapKey),
								 * ((this.gapBetween1And2 * baselineActualHeight) / baselinePixelHeight),
								 * minimumMultipleOfOtherGaps ((this.gapBetweenRest * baselineActualHeight) /
								 * baselinePixelHeight));
								 */
							}
						}
						if ((widthRatio < 0.3) || (gapRatio < 0.3)) {
							// needs to be redone, from the back
							if (this.debugLevel <= 2) {
								System.out.println(
										"Recalculating price boxes from the back as calculating from the front failed");
							}
							rejectReason = new StringBuffer();

							boolean endsWith00 = this.ocrString.endsWith(".00");
							boolean endsWith0 = this.ocrString.endsWith(".0");
							index = this.ocrString.lastIndexOf(".0");
							newOcrString = this.ocrString.replace(".0", "0");
							int expectedNumberOfBoxesAfterPrice = newOcrString.length() - index;
							newOcrString = this.ocrString.replace(".", "").replace("_", "").replace("-", "")
									.replace(",", "").replace("+", "").replace(":", "").replace(" ", "").trim();

							if ((endsWith00) || (endsWith0)) {

								numberOfIntegers = priceAsString.length();
								if (this.debugLevel <= 2) {
									System.out.println("numberOfIntegers in price = " + numberOfIntegers);
								}

								// ArrayList<Rectangle> boundingBoxesInFocus = new ArrayList<>();
								lastDigitBox = this.boundingBoxes.size() - 1;
								if (expectedNumberOfBoxesAfterPrice != 0) {
									int widthCounted = 0;
									int boxesCounted = 0;
									for (int i = this.boundingBoxes.size() - 1; i >= 0; --i) {
										if (i == 0) {
											lastDigitBox = i;
											break;
										}
										// widthCounted += this.boundingBoxes.get(i).width;
										// if (((widthCounted * 1.0) / averageCharWidth) >
										// (expectedNumberOfBoxesAfterPrice - 0.15)) {
										// lastDigitBox = i;
										// break;
										// }
										int boxesInThisLoop = 0;
										try {
											boxesInThisLoop = (int) Math
													.round((this.boundingBoxes.get(i).width * 1.0) / averageCharWidth);
										} catch (Exception e) {
											boxesInThisLoop = 0;
										}
										boxesCounted += boxesInThisLoop;
										if (boxesCounted >= (expectedNumberOfBoxesAfterPrice - 0.15)) {
											lastDigitBox = i - 1;
											break;
										}
									}
								}
								// find the firstDigitBox
								firstDigitBox = lastDigitBox;
								int priceWidth = 0;
								priceBoxesCounted = 0;
								for (int i = lastDigitBox; i >= 0; --i) {
									if (i == 0) {
										firstDigitBox = i;
										break;
									}
									int pricesBoxesInThisLoop = 0;
									try {
										pricesBoxesInThisLoop = (int) Math
												.round((this.boundingBoxes.get(i).width * 1.0) / averageCharWidth);
									} catch (Exception e) {
										pricesBoxesInThisLoop = 0;
									}
									priceBoxesCounted = priceBoxesCounted + pricesBoxesInThisLoop;
									if (priceBoxesCounted >= priceAsString.length()) {
										firstDigitBox = i;
										break;
									}
								}
							}

							canCompareGaps = true;
							boxDistance = (lastDigitBox - firstDigitBox) + 1;
							if (boxDistance < priceAsString.length()) {
								canCompareGaps = false;
							}

							firstDigitIsAlone = false;
							try {
								if ((this.boundingBoxes.get(firstDigitBox).width / averageCharWidth) < 1.25) {
									firstDigitIsAlone = true;
								}
							} catch (Exception e) {

							}

							if ((firstDigitIsAlone) && ((firstDigitBox - lastDigitBox) >= 2)) {
								canCompareGaps = true;
							}

							canProcessWidths = true;
							if (firstDigitBox == lastDigitBox) {
								canProcessWidths = true;
							}

							if (this.debugLevel <= 2) {
								System.out.println("In OCRPriceDimensionsWrapper.process() : canProcessWidths = "
										+ canProcessWidths + "; firstDigitIsAlone = " + firstDigitIsAlone
										+ "; firstDigitBox = " + firstDigitBox + "; lastDigitBox = " + lastDigitBox
										+ "; canCompareGaps = " + canCompareGaps);
							}

							if (canProcessWidths) {

								for (int i = firstDigitBox; i <= lastDigitBox; ++i) {
									try {
										this.widthOfNumbers += this.boundingBoxes.get(i).width;
									} catch (Exception e) {
										break;
									}
								}
								this.widthOfNumbers = (int) ((this.widthOfNumbers * 1.0) / priceAsString.length());

								multipleWidthOK = false;
								widthRatio = 0.0;
								if (firstDigitIsAlone) {
									this.gapBetween1And2 = 0;
									try {
										this.gapBetween1And2 = (this.boundingBoxes.get(firstDigitBox + 1).x
												- this.boundingBoxes.get(firstDigitBox).x
												- this.boundingBoxes.get(firstDigitBox).width) + 1;
									} catch (Exception e) {

									}
									widthRatio = (this.gapBetween1And2 * 1.0) / this.widthOfNumbers;
									if (widthRatio < minimumMultipleOfAverageWidth) {
										// this.distanceOK = false;
										multipleWidthOK = false;
										String reason = String.format(
												System.getProperty(priceSpacingErrorDueToWidthKey),
												((this.gapBetween1And2 * baselineActualHeight) / baselinePixelHeight),
												minimumMultipleOfAverageWidth
														* ((this.widthOfNumbers * baselineActualHeight)
																/ baselinePixelHeight),
												minimumMultipleOfAverageWidth);
										rejectReason.append(reason);
										if (this.debugLevel <= 2) {
											System.out.println(
													"In OCRPriceDimensionsWrapper.process() - rejection reason = "
															+ reason);
										}

									} else {
										multipleWidthOK = true;
									}

								}
								if (this.debugLevel <= 2) {
									System.out.println("In OCRPriceDimensionsWrapper.process() : widthofNumbers = "
											+ this.widthOfNumbers + "; gapBetween1And2 = " + this.gapBetween1And2
											+ "; distanceOK = " + this.distanceOK);
								}
								multipleGapOK = true; // if we cannot compare with other gaps, it means the other gaps
														// are
														// small, which implies it is most likely that the gap between
														// the
														// first
														// and second digits is sufficient
								gapRatio = minimumMultipleOfOtherGaps;
								if (firstDigitIsAlone && canCompareGaps) {

									for (int i = firstDigitBox + 1; i <= (lastDigitBox - 1); ++i) {
										try {
											this.gapBetweenRest += (this.boundingBoxes.get(i + 1).x
													- this.boundingBoxes.get(i).x - this.boundingBoxes.get(i).width)
													+ 1;
										} catch (Exception e) {
											break;
										}
									}
									this.gapBetweenRest = (int) ((this.gapBetweenRest * 1.0)
											/ (priceAsString.length() - 1));

									gapRatio = (this.gapBetween1And2 * 1.0) / this.gapBetweenRest;
									if ((gapRatio * (1 + this.percentError)) >= minimumMultipleOfOtherGaps) {
										multipleGapOK = true;
									} else {
										multipleGapOK = false;
										String reason = String.format(System.getProperty(priceSpacingErrorDueToGapKey),
												((this.gapBetween1And2 * (1 + this.percentError) * baselineActualHeight)
														/ baselinePixelHeight),
												minimumMultipleOfOtherGaps
														* ((this.gapBetweenRest * baselineActualHeight)
																/ baselinePixelHeight));
										rejectReason.append(reason);
										if (this.debugLevel <= 2) {
											System.out.println(
													"In OCRPriceDimensionsWrapper.process() - rejection reason = "
															+ reason);
										}
									}
								}
							}
						}
						if (bothPriceSpacingsNeeded) {
							this.distanceOK = multipleGapOK && multipleWidthOK;
						} else {
							this.distanceOK = multipleGapOK || multipleWidthOK;
						}
						if (!this.distanceOK) {
							this.reasonForDistanceRejection = this.reasonForDistanceRejection + rejectReason.toString();
						}
						this.gapBetween1And2 = ((this.gapBetween1And2 * (1 + this.percentError) * baselineActualHeight)
								/ baselinePixelHeight);
					} catch (ArrayIndexOutOfBoundsException e) {
						this.distanceOK = false;
						this.reasonForDistanceRejection = this.reasonForDistanceRejection
								+ "Could not calculate the distance between digits 1 and 2 due to an error";
					}
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				this.distanceOK = false;
				this.reasonForDistanceRejection = this.reasonForDistanceRejection
						+ "Could not calculate the distance between digits 1 and 2 due to an error";
			}
		} else {
			// This section is valid only for the case when product is given
			// Do not execute this section if the product is not given.
			// The productIsGiven check ensures that this section is not entered when a
			// product-price match has
			// not been found
			if (CheckProductProperties.productIsGiven) {

				// if no baseline is given for checking height, then only do a check for
				// distance between thousands and hundredths digits

				// clean the bounding boxes to ensure that small boxes corresponding to -,_,.
				// are removed. This is because the ocrString has been stripped off these as
				// well

				// remove small boxes
				if (this.debugLevel <= 2) {
					System.out.println(
							"In OCRPriceDimensionsWrapper.process() - entering 'CheckProductProperties.productIsGiven'");
				}

				int nBoundingBoxes = this.boundingBoxes.size();
				int startingY = 0;
				for (int i = nBoundingBoxes - 1; i >= 0; --i) {
					if (this.boundingBoxes.get(i).height < (this.likelyPixelHeight * 0.5)) {
						this.boundingBoxes.remove(i);
					} else {
						startingY += this.boundingBoxes.get(i).y;
					}
				}
				// remove boxes that start half-way below the top
				nBoundingBoxes = this.boundingBoxes.size();
				if (nBoundingBoxes > 0) {
					startingY = startingY / nBoundingBoxes;
					for (int i = nBoundingBoxes - 1; i >= 0; --i) {
						if (this.boundingBoxes.get(i).y > (startingY + (this.likelyPixelHeight * 0.5))) {
							this.boundingBoxes.remove(i);
						}
					}
				}
				// sort the remaining bounding boxes by x-coordinate
				Collections.sort(this.boundingBoxes, new Comparator<Rectangle>() {
					@Override
					public int compare(Rectangle o1, Rectangle o2) {
						return (o1.x) - (o2.x);
					}
				});
				if (this.debugLevel <= 2) {
					System.out.println("In OCRPriceDimensionsWrapper.process() [else]: boundingBoxes after sorting = "
							+ this.boundingBoxes);
				}

				this.heightOK = true;

				String newOcrString = this.ocrString.toUpperCase().replace(" ", "").replace("_", "").replace("-", "")
						.replace("O", "0").replace(",", "").replace("(", " ").replace(")", " ").replace("[", " ")
						.replace("]", " ").replace("'", " ").replace("\"", " ").replace("-", " ").replace("+", "")
						.replace(":", "").replace("/", "").replace("=", "").replace("|", "1").replace("\\", "")
						.replace("Z", "2").replace("D", "0").replace("{", " ").replace("}", " ").replace("I", "1")
						.replace("T", "7").replace("1U", "").replace("2U", "").replace("3U", "").replace("4U", "")
						.replace("5U", "").replace("6U", "").replace("7U", "").replace("8U", "").trim();
				if (CheckProductProperties.productIsGiven) {
					if (CheckProductProperties.givenProductPrice != 0) {
						String productPrice = "" + CheckProductProperties.givenProductPrice;
						if (productPrice.indexOf("2") != -1) {
							newOcrString = newOcrString.replace("Z", "2");
						}
						if ((productPrice.indexOf("7") != -1) && (productPrice.indexOf("1") != -1)) {
						} else {
							if (productPrice.indexOf("7") == -1) {
								newOcrString = newOcrString.replace("7", "1");
							}
							if (productPrice.indexOf("1") == -1) {
								newOcrString = newOcrString.replace("1", "7");
							}
						}
						if (this.debugLevel <= 2) {
							System.out
									.println("In OCRPriceDimensionsWrapper.process() : productPrice = " + productPrice);
						}
						if ((productPrice.indexOf("5") != -1) && (productPrice.indexOf("6") == -1)) {
							if (this.debugLevel <= 2) {
								System.out.println("In OCRPriceDimensionsWrapper.process() : Inside replace 6 with 5");
							}
							newOcrString = newOcrString.replace("6", "5");
						}
						if ((productPrice.indexOf("9") != -1) && (productPrice.indexOf("8") == -1)) {
							newOcrString = newOcrString.replace("8", "9");
						}
					}
				}
				Matcher priceMatcher = ProductPriceData.pricePattern.matcher(newOcrString);
				if (priceMatcher.find()) {
					int start = priceMatcher.start();
					int end = priceMatcher.end();
					String tobeReplaced = newOcrString.substring(start, end).trim();
					newOcrString = newOcrString.replace(tobeReplaced, "");
				}

				Matcher retailMatcher = ProductPriceData.retailPattern.matcher(newOcrString);
				if (retailMatcher.find()) {
					int start = retailMatcher.start();
					int end = retailMatcher.end();
					String tobeReplaced = newOcrString.substring(start, end).trim();
					newOcrString = newOcrString.replace(tobeReplaced, "");
				}

				Matcher taxesMatcher = ProductPriceData.taxesPattern.matcher(newOcrString);
				if (taxesMatcher.find()) {
					int start = taxesMatcher.start();
					int end = taxesMatcher.end();
					String tobeReplaced = newOcrString.substring(start, end).trim();
					newOcrString = newOcrString.replace(tobeReplaced, "");
				}

				Matcher ofAllMatcher = ProductPriceData.ofAllPattern.matcher(newOcrString);
				if (ofAllMatcher.find()) {
					int start = ofAllMatcher.start();
					int end = ofAllMatcher.end();
					String tobeReplaced = newOcrString.substring(start, end).trim();
					newOcrString = newOcrString.replace(tobeReplaced, "");
				}

				Matcher rsMatcher = ProductPriceData.rsPattern.matcher(newOcrString);
				if (rsMatcher.find()) {
					int start = rsMatcher.start();
					int end = rsMatcher.end();
					String tobeReplaced = newOcrString.substring(start, end).trim();
					// remove Rs, and change "S" to "5" in the string
					newOcrString = newOcrString.replace(tobeReplaced, "").replace("S", "5");
					// Add back RS, by first checking for a 4-digit sequence
					Pattern sequence = Pattern.compile("\\d{4,5}");
					Matcher aMatcher = sequence.matcher(newOcrString);
					if (aMatcher.find()) {
						start = aMatcher.start();
						newOcrString = newOcrString.substring(0, start) + "RS" + newOcrString.substring(start);
					}
				}

				for (Pattern aPattern : ProductPriceData.ttkPrestigeLtd) {
					Matcher aMatcher = aPattern.matcher(newOcrString);
					if (aMatcher.find()) {
						int start = aMatcher.start();
						int end = aMatcher.end();
						String tobeReplaced = newOcrString.substring(start, end).trim();
						newOcrString = newOcrString.replace(tobeReplaced, "");
					}
				}

				/*
				 * String newOcrString = this.ocrString.toUpperCase().replace(" ",
				 * "").replace("_", ".").replace("-", ".") .replace("O", "0").replace(",",
				 * ".").replace("+", ".").replace(":", ".").replace("/", "") .replace("=",
				 * ".").replace("|", "").replace("\\", "").replace("Z", "2").replace("L", "1")
				 * .replace("B", "8").replace("C", "0").replace("D", "0").replace("I",
				 * "1").trim(); // replace("S", // "5") // removed // as it // replaces // the s
				 * in // Rs 1120
				 */

				if (this.debugLevel <= 2) {
					System.out.println("In OCRPriceDimensionsWrapper.process() [else]: newOcrString = " + newOcrString);
				}

				Pattern numberSequence1 = Pattern.compile("\\d{3,}$"); // check for sequence of 3 or more numbers,
																		// followed
																		// by end of line
				Pattern numberSequence2 = Pattern.compile("\\d{3,}\\D"); // check for sequence of 3 or more numbers,
																			// followed by a non-digit character (. or
																			// _)

				Matcher matcher = numberSequence1.matcher(newOcrString);
				String priceAsString = null;
				int index = -1;
				if (matcher.find()) {
					priceAsString = matcher.group();
					index = matcher.start();
					if (this.debugLevel <= 2) {
						System.out.println("In OCRPriceDimensionsWrapper.process() [else]: priceAsString = "
								+ priceAsString + "; index = " + index);
					}
				}
				if (index == -1) {
					matcher = numberSequence2.matcher(newOcrString);
					if (matcher.find()) {
						priceAsString = matcher.group();
						index = matcher.start();
						if (this.debugLevel <= 2) {
							System.out.println("In OCRPriceDimensionsWrapper.process() [else]: priceAsString = "
									+ priceAsString + "; index = " + index);
						}
					}
				}
				if (index == -1) {
					this.gapBetween1And2 = -1;
					this.gapBetweenRest = -1;
					this.widthOfNumbers = -1;
					this.distanceOK = false;
					this.reasonForDistanceRejection = "Could not compare gaps";
					return;
				}

				// newOcrString = newOcrString.replace(".", "").replace("_", "").replace("-",
				// "").replace(",", "")
				// .replace("+", "").replace(":", "").replace(" ", "").trim();
				newOcrString = newOcrString.replace("_", ".").replace("-", ".").replace(",", ".").replace("+", ".")
						.replace(":", ".").replace(" ", "").trim();

				/*
				 * priceAsString = priceAsString.replace(".", "").replace("_", "").replace("-",
				 * "").replace(",", "") .replace("+", "").replace(":", "").replace(" ",
				 * "").trim();
				 */
				priceAsString = this.price;
				index = newOcrString.indexOf(priceAsString);

				DescriptiveStatistics wStats = new DescriptiveStatistics();
				for (Rectangle box : this.boundingBoxes) {
					wStats.addValue(box.width);
				}
				double averageCharWidth;
				if ((wStats.getStandardDeviation() / wStats.getMean()) < 0.2) {
					averageCharWidth = wStats.getMean();
				} else {
					averageCharWidth = (wStats.getN() * wStats.getMean()) / newOcrString.length();
				}

				try {
					int firstDigitBox = 0;
					int lastDigitBox = 0;

					// IF newOCRString ENDS IN 00, THEN COUNT FROM THE BACK
					// ELSE, COUNT FROM THE FRONT

					if (this.debugLevel <= 2) {
						System.out.println(
								"In OCRPriceDimensionsWrapper.process() [else]: expectedNumberOfBoxesAfterPrice = "
										+ (newOcrString.length() - index - priceAsString.length())
										+ "; newOcrString.length() = " + newOcrString.length() + "; newOcrString = "
										+ newOcrString + "; priceAsString = " + priceAsString + "; index = " + index
										+ "; averageCharWidth = " + averageCharWidth);
					}

					int expectedNumberOfBoxesBeforePrice = index;
					if (this.debugLevel <= 2) {
						System.out.println(
								"In OCRPriceDimensionsWrapper.process() [else]: expectedNumberOfBoxesBeforePrice = "
										+ expectedNumberOfBoxesBeforePrice + "; newOcrString.length() = "
										+ newOcrString.length() + "; newOcrString = " + newOcrString
										+ "; priceAsString = " + priceAsString + "; index = " + index
										+ "; averageCharWidth = " + averageCharWidth);
					}
					int numberOfIntegers = priceAsString.length();
					if (this.debugLevel <= 2) {
						System.out.println("numberOfIntegers in price [else]= " + numberOfIntegers);
					}

					// ArrayList<Rectangle> boundingBoxesInFocus = new ArrayList<>();
					firstDigitBox = 0;
					if (expectedNumberOfBoxesBeforePrice != 0) {
						int boxesCounted = 0;
						for (int i = 0; i < expectedNumberOfBoxesBeforePrice; ++i) {
							if (i == (expectedNumberOfBoxesBeforePrice - 1)) {
								firstDigitBox = i;
								break;
							}
							int boxesInThisLoop = 0;
							try {
								boxesInThisLoop = (int) Math
										.round((this.boundingBoxes.get(i).width * 1.0) / averageCharWidth);
							} catch (Exception e) {

							}
							boxesCounted += boxesInThisLoop;
							if (boxesCounted >= expectedNumberOfBoxesBeforePrice) {
								firstDigitBox = i + 1;
								break;
							}
						}
					}
					// next, find the lastDigitBox
					lastDigitBox = firstDigitBox;
					int priceBoxesCounted = 0;
					for (int i = firstDigitBox; i < this.boundingBoxes.size(); ++i) {
						if (i == (this.boundingBoxes.size() - 1)) {
							lastDigitBox = i;
							break;
						}
						int pricesBoxesInThisLoop = 0;
						try {
							pricesBoxesInThisLoop = (int) Math
									.round((this.boundingBoxes.get(i).width * 1.0) / averageCharWidth);
						} catch (Exception e) {

						}
						priceBoxesCounted = priceBoxesCounted + pricesBoxesInThisLoop;
						if (priceBoxesCounted >= priceAsString.length()) {
							lastDigitBox = i;
							break;
						}
					}

					boolean canCompareGaps = true;
					int boxDistance = (lastDigitBox - firstDigitBox) + 1;
					if (boxDistance < priceAsString.length()) {
						canCompareGaps = false;
					}

					boolean firstDigitIsAlone = false;
					try {
						if ((this.boundingBoxes.get(firstDigitBox).width / averageCharWidth) < 1.25) {
							firstDigitIsAlone = true;
						}
					} catch (Exception e) {

					}

					if ((firstDigitIsAlone) && ((firstDigitBox - lastDigitBox) >= 2)) {
						canCompareGaps = true;
					}

					boolean canProcessWidths = true;
					if (firstDigitBox == lastDigitBox) {
						canProcessWidths = true;
					}

					if (this.debugLevel <= 2) {
						System.out.println("In OCRPriceDimensionsWrapper.process() : canProcessWidths = "
								+ canProcessWidths + "; firstDigitIsAlone = " + firstDigitIsAlone + "; firstDigitBox = "
								+ firstDigitBox + "; lastDigitBox = " + lastDigitBox + "; canCompareGaps = "
								+ canCompareGaps);
					}

					if (canProcessWidths) {

						try {
							for (int i = firstDigitBox; i <= lastDigitBox; ++i) {
								try {
									this.widthOfNumbers += this.boundingBoxes.get(i).width;
								} catch (Exception e) {
									break;
								}
							}
							this.widthOfNumbers = (int) ((this.widthOfNumbers * 1.0) / priceAsString.length());

							boolean multipleWidthOK = false;
							double widthRatio = 0.0;
							StringBuffer rejectReason = new StringBuffer();
							if (firstDigitIsAlone) {

								try {
									this.gapBetween1And2 = (this.boundingBoxes.get(firstDigitBox + 1).x
											- this.boundingBoxes.get(firstDigitBox).x
											- this.boundingBoxes.get(firstDigitBox).width) + 1;
								} catch (Exception e) {

								}
								widthRatio = (this.gapBetween1And2 * 1.0) / this.widthOfNumbers;
								if ((widthRatio * (1 + this.percentError)) < minimumMultipleOfAverageWidth) {
									// this.distanceOK = false;
									multipleWidthOK = false;
									/*
									 * this.reasonForDistanceRejection = this.reasonForDistanceRejection +
									 * String.format(System.getProperty(priceSpacingErrorDueToWidthKey),
									 * ((this.gapBetween1And2 * baselineActualHeight) / baselinePixelHeight),
									 * minimumMultipleOfAverageWidth ((this.widthOfNumbers * baselineActualHeight) /
									 * baselinePixelHeight), minimumMultipleOfAverageWidth);
									 */
									String reason = String.format(System.getProperty(priceSpacingErrorDueToWidthKey),
											this.gapBetween1And2 * (1 + this.percentError),
											minimumMultipleOfAverageWidth * this.widthOfNumbers,
											minimumMultipleOfAverageWidth);
									rejectReason.append(reason);
									if (this.debugLevel <= 2) {
										System.out
												.println("In OCRPriceDimensionsWrapper.process() - rejection reason = "
														+ reason);
									}
								} else {
									// this.distanceOK = true;
									multipleWidthOK = true;
								}

							}
							if (this.debugLevel <= 2) {
								System.out.println("In OCRPriceDimensionsWrapper.process() : widthofNumbers = "
										+ this.widthOfNumbers + "; gapBetween1And2 = " + this.gapBetween1And2
										+ "; distanceOK = " + this.distanceOK);
							}
							boolean multipleGapOK = true; // if we cannot compare with other gaps, it means the other
															// gaps
															// are
															// small, which implies it is most likely that the gap
															// between
															// the
															// first
															// and second digits is sufficient
							double gapRatio = minimumMultipleOfOtherGaps;
							if (firstDigitIsAlone && canCompareGaps) {

								for (int i = firstDigitBox + 1; i <= (lastDigitBox - 1); ++i) {
									try {
										this.gapBetweenRest += (this.boundingBoxes.get(i + 1).x
												- this.boundingBoxes.get(i).x - this.boundingBoxes.get(i).width) + 1;
									} catch (Exception e) {

									}
								}
								this.gapBetweenRest = (int) ((this.gapBetweenRest * 1.0)
										/ (priceAsString.length() - 1));

								gapRatio = (this.gapBetween1And2 * 1.0) / this.gapBetweenRest;
								if (gapRatio >= minimumMultipleOfOtherGaps) {
									// this.distanceOK = this.distanceOK && true;
									multipleGapOK = true;
								} else {
									// this.distanceOK = false;
									multipleGapOK = false;
									String reason = String.format(System.getProperty(priceSpacingErrorDueToGapKey),
											this.gapBetween1And2 * (1 + this.percentError),
											minimumMultipleOfOtherGaps * this.gapBetweenRest);
									rejectReason.append(reason);
									if (this.debugLevel <= 2) {
										System.out
												.println("In OCRPriceDimensionsWrapper.process() - rejection reason = "
														+ reason);
									}
									/*
									 * this.reasonForDistanceRejection = this.reasonForDistanceRejection +
									 * String.format(System.getProperty(priceSpacingErrorDueToGapKey),
									 * ((this.gapBetween1And2 * baselineActualHeight) / baselinePixelHeight),
									 * minimumMultipleOfOtherGaps ((this.gapBetweenRest * baselineActualHeight) /
									 * baselinePixelHeight));
									 */
								}
							}
							if ((widthRatio < 0.3) || (gapRatio < 0.3)) {
								// needs to be redone, from the back
								if (this.debugLevel <= 2) {
									System.out.println(
											"Recalculating price boxes from the back as calculating from the front failed");
								}
								rejectReason = new StringBuffer();

								boolean endsWith00 = this.ocrString.endsWith(".00");
								boolean endsWith0 = this.ocrString.endsWith(".0");
								index = this.ocrString.lastIndexOf(".0");
								newOcrString = this.ocrString.replace(".0", "0");
								int expectedNumberOfBoxesAfterPrice = newOcrString.length() - index;
								newOcrString = this.ocrString.replace(".", "").replace("_", "").replace("-", "")
										.replace(",", "").replace("+", "").replace(":", "").replace(" ", "").trim();

								if ((endsWith00) || (endsWith0)) {

									numberOfIntegers = priceAsString.length();
									if (this.debugLevel <= 2) {
										System.out.println("numberOfIntegers in price = " + numberOfIntegers);
									}

									// ArrayList<Rectangle> boundingBoxesInFocus = new ArrayList<>();
									lastDigitBox = this.boundingBoxes.size() - 1;
									if (expectedNumberOfBoxesAfterPrice != 0) {
										int widthCounted = 0;
										int boxesCounted = 0;
										for (int i = this.boundingBoxes.size() - 1; i >= 0; --i) {
											if (i == 0) {
												lastDigitBox = i;
												break;
											}
											// widthCounted += this.boundingBoxes.get(i).width;
											// if (((widthCounted * 1.0) / averageCharWidth) >
											// (expectedNumberOfBoxesAfterPrice - 0.15)) {
											// lastDigitBox = i;
											// break;
											// }
											int boxesInThisLoop = 0;
											try {
												boxesInThisLoop = (int) Math.round(
														(this.boundingBoxes.get(i).width * 1.0) / averageCharWidth);
											} catch (Exception e) {

											}
											boxesCounted += boxesInThisLoop;
											if (boxesCounted >= (expectedNumberOfBoxesAfterPrice - 0.15)) {
												lastDigitBox = i - 1;
												break;
											}
										}
									}
									// find the firstDigitBox
									firstDigitBox = lastDigitBox;
									int priceWidth = 0;
									priceBoxesCounted = 0;
									for (int i = lastDigitBox; i >= 0; --i) {
										if (i == 0) {
											firstDigitBox = i;
											break;
										}
										int pricesBoxesInThisLoop = 0;
										try {
											pricesBoxesInThisLoop = (int) Math
													.round((this.boundingBoxes.get(i).width * 1.0) / averageCharWidth);
										} catch (Exception e) {

										}
										priceBoxesCounted = priceBoxesCounted + pricesBoxesInThisLoop;
										if (priceBoxesCounted >= priceAsString.length()) {
											firstDigitBox = i;
											break;
										}
									}
								}

								canCompareGaps = true;
								boxDistance = (lastDigitBox - firstDigitBox) + 1;
								if (boxDistance < priceAsString.length()) {
									canCompareGaps = false;
								}

								firstDigitIsAlone = false;
								try {
									if ((this.boundingBoxes.get(firstDigitBox).width / averageCharWidth) < 1.25) {
										firstDigitIsAlone = true;
									}
								} catch (Exception e) {

								}

								if ((firstDigitIsAlone) && ((firstDigitBox - lastDigitBox) >= 2)) {
									canCompareGaps = true;
								}

								canProcessWidths = true;
								if (firstDigitBox == lastDigitBox) {
									canProcessWidths = true;
								}

								if (this.debugLevel <= 2) {
									System.out.println("In OCRPriceDimensionsWrapper.process() : canProcessWidths = "
											+ canProcessWidths + "; firstDigitIsAlone = " + firstDigitIsAlone
											+ "; firstDigitBox = " + firstDigitBox + "; lastDigitBox = " + lastDigitBox
											+ "; canCompareGaps = " + canCompareGaps);
								}

								if (canProcessWidths) {

									for (int i = firstDigitBox; i <= lastDigitBox; ++i) {
										try {
											this.widthOfNumbers += this.boundingBoxes.get(i).width;
										} catch (Exception e) {
											break;
										}
									}
									this.widthOfNumbers = (int) ((this.widthOfNumbers * 1.0) / priceAsString.length());

									multipleWidthOK = false;
									widthRatio = 0.0;
									if (firstDigitIsAlone) {
										try {
											this.gapBetween1And2 = (this.boundingBoxes.get(firstDigitBox + 1).x
													- this.boundingBoxes.get(firstDigitBox).x
													- this.boundingBoxes.get(firstDigitBox).width) + 1;
										} catch (Exception e) {

										}
										widthRatio = (this.gapBetween1And2 * 1.0) / this.widthOfNumbers;
										if ((widthRatio * (1 + this.percentError)) < minimumMultipleOfAverageWidth) {
											// this.distanceOK = false;
											multipleWidthOK = false;
											String reason = String.format(
													System.getProperty(priceSpacingErrorDueToWidthKey),
													this.gapBetween1And2 * (1 + this.percentError),
													minimumMultipleOfAverageWidth * this.widthOfNumbers,
													minimumMultipleOfAverageWidth);
											rejectReason.append(reason);
											if (this.debugLevel <= 2) {
												System.out.println(
														"In OCRPriceDimensionsWrapper.process() - rejection reason = "
																+ reason);
											}
										} else {
											multipleWidthOK = true;
										}

									}
									if (this.debugLevel <= 2) {
										System.out.println("In OCRPriceDimensionsWrapper.process() : widthofNumbers = "
												+ this.widthOfNumbers + "; gapBetween1And2 = " + this.gapBetween1And2
												+ "; distanceOK = " + this.distanceOK);
									}
									multipleGapOK = true; // if we cannot compare with other gaps, it means the other
															// gaps
															// are
															// small, which implies it is most likely that the gap
															// between
															// the
															// first
															// and second digits is sufficient
									gapRatio = minimumMultipleOfOtherGaps;
									if (firstDigitIsAlone && canCompareGaps) {

										for (int i = firstDigitBox + 1; i <= (lastDigitBox - 1); ++i) {
											try {
												this.gapBetweenRest += (this.boundingBoxes.get(i + 1).x
														- this.boundingBoxes.get(i).x - this.boundingBoxes.get(i).width)
														+ 1;
											} catch (Exception e) {
												break;
											}
										}
										this.gapBetweenRest = (int) ((this.gapBetweenRest * 1.0)
												/ (priceAsString.length() - 1));

										gapRatio = (this.gapBetween1And2 * 1.0) / this.gapBetweenRest;
										if ((gapRatio * (1 + this.percentError)) >= minimumMultipleOfOtherGaps) {
											multipleGapOK = true;
										} else {
											multipleGapOK = false;
											String reason = String.format(
													System.getProperty(priceSpacingErrorDueToGapKey),
													this.gapBetween1And2 * (1 + this.percentError),
													minimumMultipleOfOtherGaps * this.gapBetweenRest);
											rejectReason.append(reason);
											if (this.debugLevel <= 2) {
												System.out.println(
														"In OCRPriceDimensionsWrapper.process() - rejection reason = "
																+ reason);
											}
										}
									}
								}
							}
							if (bothPriceSpacingsNeeded) {
								this.distanceOK = multipleGapOK && multipleWidthOK;
							} else {
								this.distanceOK = multipleGapOK || multipleWidthOK;
							}
							if (!this.distanceOK) {
								this.reasonForDistanceRejection = this.reasonForDistanceRejection
										+ rejectReason.toString();
								this.reasonForDistanceRejection = this.reasonForDistanceRejection.replace("mm",
										"pixels");
								if (this.debugLevel <= 2) {
									System.out.println("In OCRPriceDimensionsWrapper.process() - rejection reason = "
											+ this.reasonForDistanceRejection);
								}
							}
						} catch (ArrayIndexOutOfBoundsException e) {
							this.distanceOK = false;
							this.reasonForDistanceRejection = this.reasonForDistanceRejection
									+ "Could not calculate the distance between digits 1 and 2 due to an error";
						}
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					this.distanceOK = false;
					this.reasonForDistanceRejection = this.reasonForDistanceRejection
							+ "Could not calculate the distance between digits 1 and 2 due to an error";
				}
			}
		}
	}
}