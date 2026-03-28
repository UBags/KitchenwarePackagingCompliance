package com.techwerx.labelprocessing;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.techwerx.labelprocessing.prestige.CheckProductProperties;
import com.techwerx.labelprocessing.prestige.ProductPriceData;

public class ProductDescription {

	public static final int ALL_OK = 0;
	public static final int ERROR_ONLY_ONE_THING_NOT_FOUND_OR_SOME_OTHER_ISSUE = 1;
	public static final int ERROR_PRODUCT_AND_PRICE_NOT_FOUND = 2;
	public static final int ERROR_DIMENSION_PROBLEM = 3;
	public static final int BAD_ERROR_WILL_NEED_REPROCESSING = 4;
	public static final String separator = " ; ";

	public String fileName = "";
	public String productName = "";
	public int productCharacterHeightActual;
	public int productCharacterHeightPixels;

	public ArrayList<Double> price = new ArrayList<>();
	public String finalPrice = "";
	public double priceCharacterHeightActual;
	public int priceCharacterHeightPixels = 0;
	public double priceGapBetween1And2;

	public String month = "";
	public String year = "";
	public double dateCharacterHeightActual;
	public int dateCharacterHeightPixels = 0;

	public int productOK;
	public String rejectionReason = "";
	public static int serialNo = 0; // line number count on the front end and in the consolidated results file

	public boolean foundPrice = false;
	public boolean priceHasADot = false;

	public static final ProductDescription ERROR_NO_PRODUCT_SPECIFIED = new ProductDescription()
			.setProductOK(BAD_ERROR_WILL_NEED_REPROCESSING).setRejectionReason(
					"Incorrect product name in product.properties. Product name in product.properties must match exactly with a product name from Price Master Summary. ");

	public static final ProductDescription getEmptyShell() {
		ProductDescription emptyShell = new ProductDescription();
		// emptyShell.setProductOK(ERROR_ONLY_ONE_THING_NOT_FOUND_OR_SOME_OTHER_ISSUE);
		return emptyShell;
	}

	public static final ProductDescription copy(ProductDescription master) {
		ProductDescription slave = new ProductDescription();
		slave.fileName = master.fileName;
		slave.productName = master.productName;
		slave.productCharacterHeightActual = master.productCharacterHeightActual;
		slave.productCharacterHeightPixels = master.productCharacterHeightPixels;

		slave.price = master.price;
		slave.finalPrice = master.finalPrice;
		slave.priceCharacterHeightActual = master.priceCharacterHeightActual;
		slave.priceCharacterHeightPixels = master.priceCharacterHeightPixels;
		slave.priceGapBetween1And2 = master.priceGapBetween1And2;

		slave.month = master.month;
		slave.year = master.year;
		slave.dateCharacterHeightActual = master.dateCharacterHeightActual;
		slave.dateCharacterHeightPixels = master.dateCharacterHeightPixels;

		slave.productOK = master.productOK;
		slave.rejectionReason = master.rejectionReason;
		slave.serialNo = master.serialNo; // line number count on the front end and in the consolidated results file

		slave.foundPrice = master.foundPrice;
		slave.priceHasADot = master.priceHasADot;
		return slave;
	}

	public ProductDescription merge(ProductDescription secondaryProduct) {

		// System.out.println("Entering merge");
		// System.out.println("Base = " + this);
		// System.out.println("Secondary = " + secondaryProduct);

		if ("".equals(this.productName)) {
			this.productName = secondaryProduct.productName;
			if (!("".equals(this.productName))) {
				this.setRejectionReason(removeProductError(this.getRejectionReason()));
				secondaryProduct.setRejectionReason(removeProductError(secondaryProduct.getRejectionReason()));
			} else {
				this.setRejectionReason(addProductError(this.getRejectionReason()));
			}
		} else {
			// we don't need the product error rejection reason any longer
			this.setRejectionReason(removeProductError(this.getRejectionReason()));
			secondaryProduct.setRejectionReason(removeProductError(secondaryProduct.getRejectionReason()));
		}

		if (this.productCharacterHeightActual == 0) {
			this.productCharacterHeightActual = secondaryProduct.productCharacterHeightActual;
		}
		if (this.productCharacterHeightPixels == 0) {
			this.productCharacterHeightPixels = secondaryProduct.productCharacterHeightPixels;
		}

		if (this.price.size() == 0) {
			this.price = secondaryProduct.price;
		}

		/*
		 * if (!CheckProductProperties.productIsGiven) { if
		 * (("".equals(this.finalPrice)) || ("0".equals(this.finalPrice))) {
		 * this.finalPrice = secondaryProduct.finalPrice; } } else { if
		 * (("".equals(this.finalPrice)) || ("0".equals(this.finalPrice))) {
		 * this.finalPrice = secondaryProduct.finalPrice;
		 * this.setRejectionReason(addPriceError(this.getRejectionReason())); } if
		 * (!(("".equals(this.finalPrice)) || ("0".equals(this.finalPrice)))) {
		 * this.setRejectionReason(removePriceError(this.getRejectionReason()));
		 * secondaryProduct.setRejectionReason(removePriceError(secondaryProduct.
		 * getRejectionReason())); } } ;
		 */

		if (("".equals(this.finalPrice)) || ("0".equals(this.finalPrice))) {
			this.finalPrice = secondaryProduct.finalPrice;
			if (("".equals(this.finalPrice)) || ("0".equals(this.finalPrice))) {
				this.foundPrice = false;
				this.setRejectionReason(addPriceError(this.getRejectionReason()));
			} else {
				this.foundPrice = true;
				// System.out.println("Removing price error string");
				this.setRejectionReason(removePriceError(this.getRejectionReason()));
				secondaryProduct.setRejectionReason(removePriceError(secondaryProduct.getRejectionReason()));
			}
		} else {
			// we don't need the price error rejection reason any longer
			this.setRejectionReason(removePriceError(this.getRejectionReason()));
			secondaryProduct.setRejectionReason(removePriceError(secondaryProduct.getRejectionReason()));
		}

		if (this.priceCharacterHeightActual == 0.0) {
			this.priceCharacterHeightActual = secondaryProduct.priceCharacterHeightActual;
		}

		if (this.priceCharacterHeightPixels == 0) {
			this.priceCharacterHeightPixels = secondaryProduct.priceCharacterHeightPixels;
		}

		if (this.priceGapBetween1And2 == 0.0) {
			this.priceGapBetween1And2 = secondaryProduct.priceGapBetween1And2;
		}

		// Note : month may be populated, but there may still be a rejection reason
		// attributable to month, as month may be populated in a secondary loop by a
		// match with last few characters.
		// This special case needs to be handled.
		if ("".equals(this.month)) {
			this.month = secondaryProduct.month;
			if (!("".equals(this.month))) {
				if (secondaryProduct.rejectionReason
						.indexOf(System.getProperty(ProductPriceData.monthErrorKey)) == -1) {
					this.setRejectionReason(removeMonthError(this.getRejectionReason()));
					secondaryProduct.setRejectionReason(removeMonthError(secondaryProduct.getRejectionReason()));
				}
			} else {
				this.setRejectionReason(addMonthError(this.getRejectionReason()));
			}
		} else {
			// we don't need the month error rejection reason any longer
			secondaryProduct.setRejectionReason(removeMonthError(secondaryProduct.getRejectionReason()));
			// the below if statement is the change required by the special case
			if (!("".equals(secondaryProduct.month))) {
				this.setRejectionReason(removeMonthError(this.getRejectionReason()));
			}

		}

		if ("".equals(this.year)) {
			this.year = secondaryProduct.year;
			if (!("".equals(this.year))) {
				this.setRejectionReason(removeYearError(this.getRejectionReason()));
				secondaryProduct.setRejectionReason(removeYearError(secondaryProduct.getRejectionReason()));
			} else {
				this.setRejectionReason(addYearError(this.getRejectionReason()));
			}
		} else {
			// we don't need the year error rejection reason any longer
			this.setRejectionReason(removeYearError(this.getRejectionReason()));
			secondaryProduct.setRejectionReason(removeYearError(secondaryProduct.getRejectionReason()));
		}

		if (this.dateCharacterHeightActual == 0.0) {
			this.dateCharacterHeightActual = secondaryProduct.dateCharacterHeightActual;
		}

		if (this.dateCharacterHeightPixels == 0) {
			this.dateCharacterHeightPixels = secondaryProduct.dateCharacterHeightPixels;
		}

		// removing, as the logic for this is unclear
		// if ("".equals(this.rejectionReason)) {
		// this.year = secondaryProduct.year;
		// }

		// Note: It is exactly opposite of all the above
		// if ("".equals(secondaryProduct.rejectionReason)) {
		// this.rejectionReason = "";
		// }

		if (!this.priceHasADot) {
			this.priceHasADot = secondaryProduct.priceHasADot;
			if (this.priceHasADot) {
				this.setRejectionReason(removePriceDotError(this.getRejectionReason()));
				secondaryProduct.setRejectionReason(removePriceDotError(secondaryProduct.getRejectionReason()));
			} else {
				this.setRejectionReason(addPriceDotError(this.getRejectionReason()));
			}
		} else {
			// we don't need the priceDot error rejection reason any longer
			this.setRejectionReason(removePriceDotError(this.getRejectionReason()));
			secondaryProduct.setRejectionReason(removePriceDotError(secondaryProduct.getRejectionReason()));
		}

		// when an empty shell is created, the following 4 error strings are not set.
		// So, the boolean values will be set to false.
		// The checks of "(== 0)" help overcome this issue because in an empty shell,
		// these values are 0.
		boolean previousPriceHeightError = (secondaryProduct.priceCharacterHeightPixels == 1)
				|| (secondaryProduct.priceCharacterHeightPixels == 0)
				|| (secondaryProduct.getRejectionReason().indexOf("Price height is") >= -1);
		boolean previousDateHeightError = (secondaryProduct.dateCharacterHeightPixels == 1)
				|| (secondaryProduct.dateCharacterHeightPixels == 0)
				|| (secondaryProduct.getRejectionReason().indexOf("Month and year height is") >= -1);
		boolean previousSpacingGapError = (secondaryProduct.priceGapBetween1And2 == -1.0)
				|| (secondaryProduct.getRejectionReason().indexOf("average gap") >= -1);
		boolean previousSpacingWidthError = (secondaryProduct.priceGapBetween1And2 == -1.0)
				|| (secondaryProduct.getRejectionReason().indexOf("average width") >= -1);

		boolean currentPriceHeightError = (this.rejectionReason.indexOf("Price height is") >= -1);
		boolean currentDateHeightError = (this.rejectionReason.indexOf("Month and year height is") >= -1);
		boolean currentSpacingGapError = (this.rejectionReason.indexOf("average gap") >= -1);
		boolean currentSpacingWidthError = (secondaryProduct.getRejectionReason().indexOf("average width") >= -1);

		if (CheckProductProperties.productIsGiven) {
			// deal only with spacing errors
			if (currentSpacingGapError) {
				if (!previousSpacingGapError) {
					String error = this.extractGapSpacingErrorString(this.rejectionReason);
					this.setRejectionReason(this.rejectionReason.replace(error, ""));
				}
			}
			if (currentSpacingWidthError) {
				if (!previousSpacingWidthError) {
					String error = this.extractWidthSpacingErrorString(this.rejectionReason);
					this.setRejectionReason(this.rejectionReason.replace(error, ""));
				}
			}
		} else {
			// deal with all 4 errors
			if (currentPriceHeightError) {
				if (!previousPriceHeightError) {
					String error = this.extractPriceHeightErrorString(this.rejectionReason);
					this.setRejectionReason(this.rejectionReason.replace(error, ""));
				}
			}
			if (currentDateHeightError) {
				if (!previousDateHeightError) {
					String error = this.extractDateHeightErrorString(this.rejectionReason);
					this.setRejectionReason(this.rejectionReason.replace(error, ""));
				}
			}
			if (currentSpacingGapError) {
				if (!previousSpacingGapError) {
					String error = this.extractGapSpacingErrorString(this.rejectionReason);
					this.setRejectionReason(this.rejectionReason.replace(error, ""));
				}
			}
			if (currentSpacingWidthError) {
				if (!previousSpacingWidthError) {
					String error = this.extractWidthSpacingErrorString(this.rejectionReason);
					this.setRejectionReason(this.rejectionReason.replace(error, ""));
				}
			}
		}

		if (this.foundPrice) {

		}

		this.productOK = this.deriveProductOK();

		return this;
	}

	private int deriveProductOK() {
		if (!CheckProductProperties.productIsGiven) {
			if (!"".equals(this.rejectionReason)) {
				if (!(this.productName.equals("")) && !(this.finalPrice.equals("")) && !(this.finalPrice.equals("0"))
						&& !(this.month.equals("")) && !(this.year.equals("")) && this.priceHasADot) {
					return ERROR_DIMENSION_PROBLEM;
				}
				// if (!(this.productName.equals("")) && !(this.finalPrice.equals("")) &&
				// !(this.month.equals(""))
				// && !(this.year.equals("")) && (!this.rejectionReason.equals(""))) {
				// return ERROR_DIMENSION_PROBLEM;
				// }
				if ((this.productName.equals("")) && ((this.finalPrice.equals("")) || (this.finalPrice.equals("0")))
						&& !(this.month.equals("")) && !(this.year.equals(""))) {
					return ERROR_PRODUCT_AND_PRICE_NOT_FOUND;
				}
				if (!this.priceHasADot) {
					return ERROR_ONLY_ONE_THING_NOT_FOUND_OR_SOME_OTHER_ISSUE;
				}
				return ERROR_ONLY_ONE_THING_NOT_FOUND_OR_SOME_OTHER_ISSUE;
			} else {

			}
		} else {
			if (!"".equals(this.rejectionReason)) {
				if (!((this.finalPrice.equals("")) || (this.finalPrice.equals("0"))) && !(this.month.equals(""))
						&& !(this.year.equals("")) && this.priceHasADot) {
					return ERROR_DIMENSION_PROBLEM;
				}
				if (((this.finalPrice.equals("")) || (this.finalPrice.equals("0"))) && !(this.month.equals(""))
						&& !(this.year.equals(""))) {
					return ERROR_PRODUCT_AND_PRICE_NOT_FOUND;
				}
				if (!this.priceHasADot) {
					return ERROR_ONLY_ONE_THING_NOT_FOUND_OR_SOME_OTHER_ISSUE;
				}
				return ERROR_ONLY_ONE_THING_NOT_FOUND_OR_SOME_OTHER_ISSUE;
			}
		}
		return ALL_OK;
	}

	@Override
	public String toString() {

		StringBuffer out = new StringBuffer();
		out.append(serialNo).append(separator);
		out.append(this.fileName).append(separator);
		out.append(this.productName).append(separator);
		double thisPrice = 0;

		if (!this.foundPrice) {
			// if (!((this.finalPrice != null) && !("".equals(this.finalPrice)))) {
			if (this.price.size() <= 1) {
				thisPrice = (this.price.size() == 1) ? (double) this.price.get(0) : 0;
				this.finalPrice = String.format("%.0f", thisPrice);
			} else {
				ArrayList<Double> prices = new ArrayList<>();
				ArrayList<Integer> priceCounter = new ArrayList<>();
				for (Double aPrice : this.price) {
					if (prices.contains(aPrice)) {
						int index = prices.indexOf(aPrice);
						int counter = priceCounter.get(index).intValue();
						priceCounter.set(index, counter);
					} else {
						prices.add(aPrice);
						priceCounter.add(1);
					}
				}
				int indexOfMax = 0;
				int maxCount = 0;
				for (int i = 0; i < priceCounter.size(); ++i) {
					if (priceCounter.get(i) >= maxCount) {
						indexOfMax = i;
					}
				}
				thisPrice = prices.get(indexOfMax);
				this.finalPrice = String.format("%.0f", thisPrice);
			}
		}
		out.append("Rs ");
		// out.append(String.format("%.0f", thisPrice)).append(separator);
		out.append(this.finalPrice).append(separator);
		out.append(this.month).append(separator);
		out.append(this.year).append(separator);
		out.append((this.productOK == 0) ? "OK" : "Not OK").append(separator);
		out.append(this.rejectionReason.trim()).append(separator);
		if (this.priceCharacterHeightActual > 0) {
			out.append(String.format("%.1f", this.priceCharacterHeightActual)).append(" mm").append(separator);
		} else {
			out.append(String.format("%.1f", 0.0)).append(" mm").append(separator);
		}
		if (this.dateCharacterHeightActual > 0) {
			out.append(String.format("%.1f", this.dateCharacterHeightActual)).append(" mm").append(separator);
		} else {
			out.append(String.format("%.1f", 0.0)).append(" mm").append(separator);
		}
		if (this.priceGapBetween1And2 > 0) {
			out.append(String.format("%.1f", this.priceGapBetween1And2))
					.append(CheckProductProperties.productIsGiven ? " pixels" : " mm");
		} else {
			out.append(String.format("%.1f", 0.0)).append(" mm");
		}
		return out.toString();
	}

	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return this.fileName;
	}

	/**
	 * @param fileName the fileName to set
	 */
	public ProductDescription setFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	/**
	 * @return the productName
	 */
	public String getProductName() {
		return this.productName;
	}

	/**
	 * @param productName the productName to set
	 */
	public ProductDescription setProductName(String productName) {
		this.productName = productName;
		return this;
	}

	/**
	 * @return the productCharacterHeightActual
	 */
	public int getProductCharacterHeightActual() {
		return this.productCharacterHeightActual;
	}

	/**
	 * @param productCharacterHeightActual the productCharacterHeightActual to set
	 */
	public ProductDescription setProductCharacterHeightActual(int productCharacterHeightActual) {
		this.productCharacterHeightActual = productCharacterHeightActual;
		return this;
	}

	/**
	 * @return the productCharacterHeightPixels
	 */
	public int getProductCharacterHeightPixels() {
		return this.productCharacterHeightPixels;
	}

	/**
	 * @param productCharacterHeightPixels the productCharacterHeightPixels to set
	 */
	public ProductDescription setProductCharacterHeightPixels(int productCharacterHeightPixels) {
		this.productCharacterHeightPixels = productCharacterHeightPixels;
		return this;
	}

	/**
	 * @return the price
	 */
	public ArrayList<Double> getPrice() {
		return this.price;
	}

	/**
	 * @param price the price to set
	 */
	public ProductDescription setPrice(ArrayList<Double> price) {
		this.price = price;
		return this;
	}

	/**
	 * @return the priceCharacterHeightActual
	 */
	public double getPriceCharacterHeightActual() {
		return this.priceCharacterHeightActual;
	}

	/**
	 * @param priceCharacterHeightActual the priceCharacterHeightActual to set
	 */
	public ProductDescription setPriceCharacterHeightActual(double priceCharacterHeightActual) {
		this.priceCharacterHeightActual = priceCharacterHeightActual;
		return this;
	}

	/**
	 * @return the priceCharacterHeightPixels
	 */
	public int getPriceCharacterHeightPixels() {
		return this.priceCharacterHeightPixels;
	}

	/**
	 * @param priceCharacterHeightPixels the priceCharacterHeightPixels to set
	 */
	public ProductDescription setPriceCharacterHeightPixels(int priceCharacterHeightPixels) {
		this.priceCharacterHeightPixels = priceCharacterHeightPixels;
		return this;
	}

	/**
	 * @return the priceGapBetween1And2
	 */
	public double getPriceGapBetween1And2() {
		return this.priceGapBetween1And2;
	}

	/**
	 * @param priceGapBetween1And2 the priceGapBetween1And2 to set
	 */
	public ProductDescription setPriceGapBetween1And2(double priceGapBetween1And2) {
		this.priceGapBetween1And2 = priceGapBetween1And2;
		return this;
	}

	/**
	 * @return the month
	 */
	public String getMonth() {
		return this.month;
	}

	/**
	 * @param month the month to set
	 */
	public ProductDescription setMonth(String month) {
		this.month = month;
		return this;
	}

	/**
	 * @return the year
	 */
	public String getYear() {
		return this.year;
	}

	/**
	 * @param year the year to set
	 */
	public ProductDescription setYear(String year) {
		this.year = year;
		return this;
	}

	/**
	 * @return the finalPrice
	 */
	public String getFinalPrice() {
		return this.finalPrice;
	}

	/**
	 * @param finalPrice the finalPrice to set
	 */
	public ProductDescription setFinalPrice(String finalPrice) {
		this.finalPrice = finalPrice;
		return this;
	}

	/**
	 * @return the priceHasADot
	 */
	public boolean isPriceHasADot() {
		return this.priceHasADot;
	}

	/**
	 * @param priceHasADot the priceHasADot to set
	 */
	public void setPriceHasADot(boolean priceHasADot) {
		this.priceHasADot = priceHasADot;
	}

	/**
	 * @return the dateCharacterHeightActual
	 */
	public double getDateCharacterHeightActual() {
		return this.dateCharacterHeightActual;
	}

	/**
	 * @param dateCharacterHeightActual the dateCharacterHeightActual to set
	 */
	public ProductDescription setDateCharacterHeightActual(double dateCharacterHeightActual) {
		this.dateCharacterHeightActual = dateCharacterHeightActual;
		return this;
	}

	/**
	 * @return the dateCharacterHeightPixels
	 */
	public int getDateCharacterHeightPixels() {
		return this.dateCharacterHeightPixels;
	}

	/**
	 * @param dateCharacterHeightPixels the dateCharacterHeightPixels to set
	 */
	public ProductDescription setDateCharacterHeightPixels(int dateCharacterHeightPixels) {
		this.dateCharacterHeightPixels = dateCharacterHeightPixels;
		return this;
	}

	/**
	 * @return the productOK
	 */
	public int getProductOK() {
		return this.productOK;
	}

	/**
	 * @param productOK the productOK to set
	 */
	public ProductDescription setProductOK(int productOK) {
		this.productOK = productOK;
		return this;
	}

	/**
	 * @return the rejectionReason
	 */
	public String getRejectionReason() {
		return this.rejectionReason;
	}

	/**
	 * @param rejectionReason the rejectionReason to set
	 */
	public ProductDescription setRejectionReason(String rejectionReason) {
		this.rejectionReason = rejectionReason;
		return this;
	}

	public static String errorString(String fName) {

		StringBuffer out = new StringBuffer();
		out.append(fName).append(separator);
		out.append("").append(separator);
		out.append("Not OK").append(separator);
		out.append("Rs ");
		out.append(0).append(separator);
		out.append("").append(separator);
		out.append("0 mm").append(separator);
		out.append("0 mm").append(separator);
		out.append("Could not process because of an error");
		return out.toString();
	}

	public static void incrementSerialNo() {
		++serialNo;
	}

	private static String removeMonthError(String errorString) {
		String error = System.getProperty(ProductPriceData.monthErrorKey);
		int index = errorString.indexOf(error);
		if (index == -1) {
			return errorString;
		}
		String error1 = errorString.substring(0, index);
		String error2 = errorString.substring(index + error.length());
		return error1 + error2;
	}

	private static String removeYearError(String errorString) {
		String error = System.getProperty(ProductPriceData.yearErrorKey);
		int index = errorString.indexOf(error);
		if (index == -1) {
			return errorString;
		}
		String error1 = errorString.substring(0, index);
		String error2 = errorString.substring(index + error.length());
		return error1 + error2;
	}

	private static String removePriceError(String errorString) {
		String error = System.getProperty(ProductPriceData.priceErrorKey);
		int index = errorString.indexOf(error);
		if (index == -1) {
			return errorString;
		}
		String error1 = errorString.substring(0, index);
		String error2 = errorString.substring(index + error.length());
		return error1 + error2;
	}

	private static String removePriceDotError(String errorString) {
		String error = System.getProperty(ProductPriceData.priceDotErrorKey);
		int index = errorString.indexOf(error);
		if (index == -1) {
			return errorString;
		}
		String error1 = errorString.substring(0, index);
		String error2 = errorString.substring(index + error.length());
		return error1 + error2;
	}

	private static String removeProductError(String errorString) {
		String error = System.getProperty(ProductPriceData.productErrorKey);
		int index = errorString.indexOf(error);
		if (index == -1) {
			return errorString;
		}
		String error1 = errorString.substring(0, index);
		String error2 = errorString.substring(index + error.length());
		return error1 + error2;
	}

	private static String addMonthError(String errorString) {
		// ensure that monthError is not repeated twice in the rejectionReason (=
		// errorString)
		String errorStringAfterRemovingMonthError = removeMonthError(errorString);
		String errorStringWithMonthErrorAddedBack = errorStringAfterRemovingMonthError
				+ System.getProperty(ProductPriceData.monthErrorKey);
		return errorStringWithMonthErrorAddedBack;
	}

	private static String addYearError(String errorString) {
		// ensure that yearError is not repeated twice in the rejectionReason (=
		// errorString)
		String errorStringAfterRemovingYearError = removeYearError(errorString);
		String errorStringWithYearErrorAddedBack = errorStringAfterRemovingYearError
				+ System.getProperty(ProductPriceData.yearErrorKey);
		return errorStringWithYearErrorAddedBack;
	}

	private static String addPriceError(String errorString) {
		// ensure that priceError is not repeated twice in the rejectionReason (=
		// errorString)
		String errorStringAfterRemovingPriceError = removePriceError(errorString);
		String errorStringWithPriceErrorAddedBack = errorStringAfterRemovingPriceError
				+ System.getProperty(ProductPriceData.priceErrorKey);
		return errorStringWithPriceErrorAddedBack;
	}

	private static String addPriceDotError(String errorString) {
		// ensure that priceDotError is not repeated twice in the rejectionReason (=
		// errorString)
		String errorStringAfterRemovingPriceDotError = removePriceDotError(errorString);
		String errorStringWithPriceDotErrorAddedBack = errorStringAfterRemovingPriceDotError
				+ System.getProperty(ProductPriceData.priceDotErrorKey);
		return errorStringWithPriceDotErrorAddedBack;
	}

	private static String addProductError(String errorString) {
		// ensure that productError is not repeated twice in the rejectionReason (=
		// errorString)
		String errorStringAfterRemovingProductError = removeProductError(errorString);
		String errorStringWithProductErrorAddedBack = errorStringAfterRemovingProductError
				+ System.getProperty(ProductPriceData.productErrorKey);
		return errorStringWithProductErrorAddedBack;
	}

	public String print() {

		StringBuffer out = new StringBuffer();
		out.append("SerialNo = ").append(serialNo).append(System.lineSeparator());
		out.append("FileName = ").append(this.fileName).append(System.lineSeparator());
		out.append("ProductName = ").append(this.productName).append(System.lineSeparator());
		double thisPrice = 0;

		if (!this.foundPrice) {
			// if (!((this.finalPrice != null) && !("".equals(this.finalPrice)))) {
			if (this.price.size() <= 1) {
				thisPrice = (this.price.size() == 1) ? (double) this.price.get(0) : 0;
				this.finalPrice = String.format("%.0f", thisPrice);
			} else {
				ArrayList<Double> prices = new ArrayList<>();
				ArrayList<Integer> priceCounter = new ArrayList<>();
				for (Double aPrice : this.price) {
					if (prices.contains(aPrice)) {
						int index = prices.indexOf(aPrice);
						int counter = priceCounter.get(index).intValue();
						priceCounter.set(index, counter);
					} else {
						prices.add(aPrice);
						priceCounter.add(1);
					}
				}
				int indexOfMax = 0;
				int maxCount = 0;
				for (int i = 0; i < priceCounter.size(); ++i) {
					if (priceCounter.get(i) >= maxCount) {
						indexOfMax = i;
					}
				}
				thisPrice = prices.get(indexOfMax);
				this.finalPrice = String.format("%.0f", thisPrice);
			}
		}
		out.append("Price = ").append("Rs ");
		// out.append(String.format("%.0f", thisPrice)).append(separator);
		out.append("FinalPrice = ").append(this.finalPrice).append(System.lineSeparator());
		out.append("Month = ").append(this.month).append(System.lineSeparator());
		out.append("Year = ").append(this.year).append(System.lineSeparator());
		out.append("ProductOK = ").append((this.productOK == 0) ? "OK" : "Not OK").append(System.lineSeparator());
		out.append("RejectionReason = ").append(this.rejectionReason.trim()).append(System.lineSeparator());
		if (this.priceCharacterHeightActual > 0) {
			out.append("PriceHeight = ").append(String.format("%.1f", this.priceCharacterHeightActual)).append(" mm")
					.append(System.lineSeparator());
		} else {
			out.append("PriceHeight = ").append(String.format("%.1f", 0.0)).append(" mm")
					.append(System.lineSeparator());
		}
		if (this.dateCharacterHeightActual > 0) {
			out.append("DateHeight = ").append(String.format("%.1f", this.dateCharacterHeightActual)).append(" mm")
					.append(System.lineSeparator());
		} else {
			out.append("DateHeight = ").append(String.format("%.1f", 0.0)).append(" mm").append(System.lineSeparator());
		}
		if (this.priceGapBetween1And2 > 0) {
			out.append("PriceGap = ").append(String.format("%.1f", this.priceGapBetween1And2))
					.append(CheckProductProperties.productIsGiven ? " pixels" : " mm").append(System.lineSeparator());
		} else {
			out.append("PriceGap = ").append(String.format("%.1f", 0.0)).append(" mm").append(System.lineSeparator());
		}
		return out.toString();
	}

	/*
	 * public String amalgamateRejectionReason(String anotherRejectionReason) {
	 * boolean previousMonthError = (anotherRejectionReason
	 * .indexOf(System.getProperty(ProductPriceData.monthErrorKey)) >= -1); boolean
	 * previousYearError = (anotherRejectionReason
	 * .indexOf(System.getProperty(ProductPriceData.yearErrorKey)) >= -1); boolean
	 * previousPriceError = (anotherRejectionReason
	 * .indexOf(System.getProperty(ProductPriceData.priceErrorKey)) >= -1); boolean
	 * previousPriceDotError = (anotherRejectionReason
	 * .indexOf(System.getProperty(ProductPriceData.priceDotErrorKey)) >= -1);
	 * boolean previousPriceHeightError =
	 * (anotherRejectionReason.indexOf("Price height is") >= -1); boolean
	 * previousDateAndMonthHeightError =
	 * (anotherRejectionReason.indexOf("Month and year height is") >= -1); boolean
	 * previousSpacingGapError = (anotherRejectionReason.indexOf("Spacing is") >=
	 * -1);
	 *
	 * boolean currentMonthError = (this.rejectionReason
	 * .indexOf(System.getProperty(ProductPriceData.monthErrorKey)) >= -1); boolean
	 * currentYearError = (this.rejectionReason
	 * .indexOf(System.getProperty(ProductPriceData.yearErrorKey)) >= -1); boolean
	 * currentPriceError = (this.rejectionReason
	 * .indexOf(System.getProperty(ProductPriceData.priceErrorKey)) >= -1); boolean
	 * currentPriceDotError = (this.rejectionReason
	 * .indexOf(System.getProperty(ProductPriceData.priceDotErrorKey)) >= -1);
	 * boolean currentPriceHeightError =
	 * (this.rejectionReason.indexOf("Price height is") >= -1); boolean
	 * currentDateAndMonthHeightError =
	 * (this.rejectionReason.indexOf("Month and year height is") >= -1); boolean
	 * currentSpacingGapError = (this.rejectionReason.indexOf("Spacing is") >= -1);
	 *
	 * }
	 */

	private String extractPriceHeightErrorString(String errorString) {
		String phErrorString = System.getProperty(OCRPriceDimensionsWrapper.priceHeightErrorKey);
		phErrorString = phErrorString.replace("%.1f", "\\d{1+}\\.\\d");
		Pattern phError = Pattern.compile(phErrorString);
		Matcher match = phError.matcher(phErrorString);
		if (match.find()) {
			int start = match.start();
			int end = match.end();
			String priceHeightError = phErrorString.substring(start, end).trim();
			return priceHeightError;
		}
		return "";
	}

	private String extractDateHeightErrorString(String errorString) {
		String dhErrorString = System.getProperty(OCRDateDimensionsWrapper.dateAndMonthHeightErrorKey);
		dhErrorString = dhErrorString.replace("%.1f", "\\d{1+}\\.\\d");
		Pattern phError = Pattern.compile(dhErrorString);
		Matcher match = phError.matcher(dhErrorString);
		if (match.find()) {
			int start = match.start();
			int end = match.end();
			String priceHeightError = dhErrorString.substring(start, end).trim();
			return priceHeightError;
		}
		return "";
	}

	private String extractWidthSpacingErrorString(String errorString) {
		String wsErrorString = System.getProperty(OCRPriceDimensionsWrapper.priceSpacingErrorDueToWidthKey);
		wsErrorString = wsErrorString.replace("%.1f", "\\d{1+}\\.\\d");
		Pattern wsError = Pattern.compile(wsErrorString);
		Matcher match = wsError.matcher(wsErrorString);
		if (match.find()) {
			int start = match.start();
			int end = match.end();
			String priceHeightError = wsErrorString.substring(start, end).trim();
			return priceHeightError;
		}
		return "";
	}

	private String extractGapSpacingErrorString(String errorString) {
		String gsErrorString = System.getProperty(OCRPriceDimensionsWrapper.priceSpacingErrorDueToGapKey);
		gsErrorString = gsErrorString.replace("%.1f", "\\d{1+}\\.\\d");
		Pattern gsError = Pattern.compile(gsErrorString);
		Matcher match = gsError.matcher(gsErrorString);
		if (match.find()) {
			int start = match.start();
			int end = match.end();
			String priceHeightError = gsErrorString.substring(start, end).trim();
			return priceHeightError;
		}
		return "";
	}
}
