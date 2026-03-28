package com.techwerx.labelprocessing.prestige;

public class CheckProductProperties {

	public static final String productNameNeededKey = "productname.fixed";
	public static final String productNameKey = "product.name";
	public static boolean productIsGiven = false;
	public static int givenProductPrice = 0;

	public static boolean checkIfProductIsGiven() {

		boolean givenProduct = "true".equals(System.getProperty(productNameNeededKey));
		if (givenProduct != productIsGiven) {
			System.out.println("productname.fixed changed from " + productIsGiven + " to " + givenProduct);
		}
		productIsGiven = givenProduct;
		givenProductPrice = 0;
		boolean productFound = false;
		String whichProduct = null;
		if (productIsGiven) {
			whichProduct = System.getProperty(productNameKey, "").trim();
			int index = 0;
			for (String originalProductString : ProductPriceData.originalProductStrings) {
				if (originalProductString.equals(whichProduct)) {
					givenProductPrice = ProductPriceData.prices.get(index);
					productFound = true;
					break;
				}
				++index;
			}
		} else {
			productFound = false;
			givenProductPrice = 0;
		}
		if ((!productFound) && productIsGiven) {
			System.out.println("-------------------------------");
			System.out.println("Incorrect product name " + whichProduct
					+ " in product.properties. Product name in product.properties must match exactly with a product name from Price Master Summary.");
		}
		return productIsGiven;
	}

	public static void reset() {
		System.setProperty(productNameKey, "");
		System.setProperty(productNameNeededKey, "false");
	}

}
