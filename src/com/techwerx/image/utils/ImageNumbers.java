package com.techwerx.image.utils;

public class ImageNumbers {

	private static int cleanedImageCounter = 1;
	private static int symbolBoxNumber = 1;
	private static int cdfCounter = 1;

	public static synchronized int getCleanedImageCounter() {
		if (cleanedImageCounter == 999) {
			cleanedImageCounter = 1;
		}
		return cleanedImageCounter++;
	}

	public static synchronized int getSymbolBoxNumber() {
		if (symbolBoxNumber == 999) {
			symbolBoxNumber = 1;
		}
		return symbolBoxNumber++;
	}

	public static synchronized int getCDFCounter() {
		if (cdfCounter == 999) {
			cdfCounter = 1;
		}
		return cdfCounter++;
	}

}
