package com.techwerx.image.utils;

import org.apache.commons.math3.util.FastMath;

public class NumberTriplet {

	public final double first;
	public final double second;
	public final double third;
	public final int type;
	public static final int BYTE = 1;
	public static final int INT = 2;
	public static final int FLOAT = 3;
	public static final int DOUBLE = 4;
	public final String firstName;
	public final String secondName;
	public final String thirdName;
	public static final String UNDEFINED = "DOUBLETRIPLET.UNDEFINED";

	public NumberTriplet(double first, double second) {
		this(first, second, Double.MIN_VALUE, "", "", UNDEFINED);
	}

	public NumberTriplet(double first, double second, double third) {
		this.first = first;
		this.second = second;
		this.third = third;
		this.type = DOUBLE;
		this.firstName = "";
		this.secondName = "";
		this.thirdName = "";
	}

	public NumberTriplet(double first, double second, String firstName, String secondName) {
		this(first, second, Double.MIN_VALUE, firstName, secondName, UNDEFINED);
	}

	public NumberTriplet(double first, double second, double third, String firstName, String secondName,
			String thirdName) {
		this.first = first;
		this.second = second;
		this.third = third;
		this.type = DOUBLE;
		this.firstName = firstName;
		this.secondName = secondName;
		this.thirdName = thirdName;
	}

	public NumberTriplet(float first, float second) {
		this(first, second, Float.MIN_VALUE, "", "", UNDEFINED);
	}

	public NumberTriplet(float first, float second, float third) {
		this.first = first;
		this.second = second;
		this.third = third;
		this.type = FLOAT;
		this.firstName = "";
		this.secondName = "";
		this.thirdName = "";
	}

	public NumberTriplet(float first, float second, String firstName, String secondName) {
		this(first, second, Float.MIN_VALUE, firstName, secondName, UNDEFINED);
	}

	public NumberTriplet(float first, float second, float third, String firstName, String secondName,
			String thirdName) {
		this.first = first;
		this.second = second;
		this.third = third;
		this.type = FLOAT;
		this.firstName = firstName;
		this.secondName = secondName;
		this.thirdName = thirdName;
	}

	public NumberTriplet(int first, int second) {
		this(first, second, Integer.MIN_VALUE, "", "", UNDEFINED);
	}

	public NumberTriplet(int first, int second, int third) {
		this.first = first;
		this.second = second;
		this.third = third;
		this.type = INT;
		this.firstName = "";
		this.secondName = "";
		this.thirdName = "";
	}

	public NumberTriplet(int first, int second, String firstName, String secondName) {
		this(first, second, Integer.MIN_VALUE, firstName, secondName, UNDEFINED);
	}

	public NumberTriplet(int first, int second, int third, String firstName, String secondName, String thirdName) {
		this.first = first;
		this.second = second;
		this.third = third;
		this.type = INT;
		this.firstName = firstName;
		this.secondName = secondName;
		this.thirdName = thirdName;
	}

	public NumberTriplet(byte first, byte second) {
		this(first, second, Byte.MIN_VALUE, "", "", UNDEFINED);
	}

	public NumberTriplet(byte first, byte second, byte third) {
		this.first = first;
		this.second = second;
		this.third = third;
		this.type = BYTE;
		this.firstName = "";
		this.secondName = "";
		this.thirdName = "";
	}

	public NumberTriplet(byte first, byte second, String firstName, String secondName) {
		this(first, second, Byte.MIN_VALUE, firstName, secondName, UNDEFINED);
	}

	public NumberTriplet(byte first, byte second, byte third, String firstName, String secondName, String thirdName) {
		this.first = first;
		this.second = second;
		this.third = third;
		this.type = BYTE;
		this.firstName = firstName;
		this.secondName = secondName;
		this.thirdName = thirdName;
	}

	public static NumberTriplet findMinMax(Object input) throws Exception {
		if (input instanceof int[]) {
			return findMinMax(input);
		}
		if (input instanceof double[]) {
			return findMinMax(input);
		}
		if (input instanceof float[]) {
			return findMinMax(input);
		}
		if (input instanceof byte[]) {
			return findMinMax(input);
		}
		if (input instanceof int[][]) {
			return findMinMax(input);
		}
		if (input instanceof double[][]) {
			return findMinMax(input);
		}
		if (input instanceof float[][]) {
			return findMinMax(input);
		}
		if (input instanceof byte[][]) {
			return findMinMax(input);
		}
		throw new Exception("Can't determine Min and Max. Unsupported input");
	}

	// Takes 9 ms to find min and max of an array of length 3500 * 5500 cells i.e.
	// 18 M cells, values ranging from 0 -3000

	public static NumberTriplet findMinMax(double[] input, int kernelWidth) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		final int length = input.length;
		double sum = 0.0f;
		double sumsq = 0.0f;
		for (int i = kernelWidth / 2; i < (length - (kernelWidth / 2)); i++) {
			if (input[i] < min) {
				min = input[i];
			}
			if (input[i] > max) {
				max = input[i];
			}
			sum += input[i];
			sumsq += input[i] * input[i];
		}
		final double mean = (sum * 1.0) / (length - kernelWidth);
		final double stddev = FastMath.sqrt(((sumsq * 1.0) / (length - kernelWidth)) - (mean * mean));
		return new NumberTriplet(min, max, (mean + (5 * stddev)));
	}

	// Takes 9 ms to find min and max of an array of length 3500 * 5500 cells i.e.
	// 18 M cells, values ranging from 0 -3000

	public static NumberTriplet findMinMax(int[] input, int kernelWidth) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		final int length = input.length;
		double sum = 0.0f;
		double sumsq = 0.0f;
		for (int i = kernelWidth / 2; i < (length - (kernelWidth / 2)); i++) {
			if (input[i] < min) {
				min = input[i];
			}
			if (input[i] > max) {
				max = input[i];
			}
			sum += input[i];
			sumsq += input[i] * input[i];
		}
		final double mean = (sum * 1.0) / (length - kernelWidth);
		final double stddev = FastMath.sqrt(((sumsq * 1.0) / (length - kernelWidth)) - (mean * mean));
		return new NumberTriplet(min, max, (int) (mean + (5 * stddev)));
	}

	public static NumberTriplet findMinMax(byte[] input, int kernelWidth) {
		// Note: This cannot be used for pixels returned by BufferedImage.TYPE_BYTE_GRAY
		// This is because byte[] array in images stores pixels 128-255 as -128 to -1,
		// and
		// pixels 0-127 as 0-127
		byte min = Byte.MAX_VALUE;
		byte max = Byte.MIN_VALUE;
		final int length = input.length;
		double sum = 0.0f;
		double sumsq = 0.0f;
		for (int i = kernelWidth / 2; i < (length - (kernelWidth / 2)); i++) {
			if (Byte.compare(input[i], min) < 0) {
				min = input[i];
			}
			if (Byte.compare(input[i], max) > 0) {
				max = input[i];
			}
			sum += input[i];
			sumsq += input[i] * input[i];
		}
		double mean = (sum * 1.0) / (length - kernelWidth);
		double stddev = FastMath.sqrt(((sumsq * 1.0) / (length - kernelWidth)) - (mean * mean));
		return new NumberTriplet(min, max, (byte) (mean + (5 * stddev)));
	}

	public static NumberTriplet findMinMax(float[] input, int kernelWidth) {
		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;
		final int length = input.length;
		double sum = 0.0f;
		double sumsq = 0.0f;
		for (int i = kernelWidth / 2; i < (length - (kernelWidth / 2)); i++) {
			if (input[i] < min) {
				min = input[i];
			}
			if (input[i] > max) {
				max = input[i];
			}
			sum += input[i];
			sumsq += input[i] * input[i];
		}
		double mean = (sum * 1.0) / (length - kernelWidth);
		double stddev = FastMath.sqrt(((sumsq * 1.0) / (length - kernelWidth)) - (mean * mean));
		return new NumberTriplet(min, max, (float) (mean + (5 * stddev)));
	}

	/*
	 *
	 * // All 3 methods are too slow, as they spend too much time in sorting
	 *
	 * public static NumberTriplet findMinMax(int[] input) { final int[] inputClone
	 * = Arrays.copyOf(input, input.length); Arrays.parallelSort(inputClone); return
	 * new NumberTriplet(inputClone[0], inputClone[input.length - 1]); }
	 *
	 * public static NumberTriplet findMinMax(byte[] input) { final byte[]
	 * inputClone = Arrays.copyOf(input, input.length);
	 * Arrays.parallelSort(inputClone); return new NumberTriplet(inputClone[0],
	 * inputClone[input.length - 1]); }
	 *
	 * public static NumberTriplet findMinMax(float[] input) { final float[]
	 * inputClone = Arrays.copyOf(input, input.length);
	 * Arrays.parallelSort(inputClone); return new NumberTriplet(inputClone[0],
	 * inputClone[input.length - 1]); }
	 */

	/*
	 * // Too slow, as it is spending time to do sorting
	 *
	 * // 1. Takes 800 ms to find the min and max of a 3500 x 5500 matrix, if values
	 * // range // from 0-255 // Takes 1000 ms to find the min and max of a 3500 x
	 * 5500 matrix, if values // range from 0-35 million
	 *
	 * // 2. Comparatively, parallelSort of linear array of 3500 x 5500 matrix takes
	 * // 325 ms, if values are between 0-255
	 *
	 * public static NumberTriplet findMinMax(int[][] input) { final int[] rowMins =
	 * new int[input.length]; final int[] rowMaxs = new int[input.length]; final int
	 * width = input[0].length; int[] rowExtract = new int[width]; int j = 0; for
	 * (int[] row : input) { rowExtract = Arrays.copyOf(row, width);
	 * Arrays.parallelSort(rowExtract); rowMins[j] = rowExtract[0]; rowMaxs[j++] =
	 * rowExtract[width - 1]; } Arrays.parallelSort(rowMins);
	 * Arrays.parallelSort(rowMaxs); return new NumberTriplet(rowMins[0],
	 * rowMaxs[rowMaxs.length - 1]); }
	 */

	// Takes 15 ms to find min and max of an array of length 3500 * 5500 cells i.e.
	// 18 M cells, values ranging from 0 - 3000

	public static NumberTriplet findMinMax(double[][] input, int kernelHeight, int kernelWidth) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		final int height = input.length;
		final int width = input[0].length;
		double sum = 0.0f;
		double sumsq = 0.0f;
		for (int i = kernelHeight / 2; i < (height - (kernelHeight / 2)); i++) {
			for (int j = kernelWidth / 2; j < (width - (kernelWidth / 2)); j++) {
				if (input[i][j] < min) {
					min = input[i][j];
				}
				if (input[i][j] > max) {
					max = input[i][j];
				}
				sum += input[i][j];
				sumsq += input[i][j] * input[i][j];
			}
		}
		double mean = ((sum * 1.0) / ((height - kernelHeight) * (width - kernelWidth)));
		double stddev = FastMath
				.sqrt(((sumsq * 1.0) / ((height - kernelHeight) * (width - kernelWidth))) - (mean * mean));
		return new NumberTriplet(min, max, mean + (5 * stddev));
	}

	// Takes 15 ms to find min and max of an array of length 3500 * 5500 cells i.e.
	// 18 M cells, values ranging from 0 - 3000

	public static NumberTriplet findMinMax(int[][] input, int kernelHeight, int kernelWidth) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		final int height = input.length;
		final int width = input[0].length;
		double sum = 0.0f;
		double sumsq = 0.0f;
		// int no = 0;
		// DescriptiveStatistics ds = new DescriptiveStatistics();
		// float count = 0.00015f;
		// int[] histogram = new int[200];
		for (int i = kernelHeight / 2; i < (height - (kernelHeight / 2)); i++) {
			for (int j = kernelWidth / 2; j < (width - (kernelWidth / 2)); j++) {
				if (input[i][j] < min) {
					min = input[i][j];
				}
				if (input[i][j] > max) {
					max = input[i][j];
					// System.out.println("Found new max of " + max + " at [" + i + "," + j + "]");
				}
				sum += input[i][j];
				sumsq += input[i][j] * input[i][j];
				// no++;
				// ds.addValue(input[i][j]);
				// int bucket = (int) (input[i][j] / count);
				// histogram[bucket]++;
			}
		}
		double mean = ((sum * 1.0) / ((height - kernelHeight) * (width - kernelWidth)));
		double stddev = FastMath
				.sqrt(((sumsq * 1.0) / ((height - kernelHeight) * (width - kernelWidth))) - (mean * mean));
		// System.out.println("Mean = " + ((sum * 1.0) / no) + "; StdDev = "
		// + (((sumsq * 1.0) / no) - (((sum * 1.0) / no) * ((sum * 1.0) / no))));
		// System.out.println("Mean = " + ds.getMean() + "; StdDev = " +
		// ds.getStandardDeviation());
		// try {
		// PDFHandlerChartFactory.drawBarChart(histogram, 0,
		// "E:\\TechWerx\\Java\\Working\\1000-histData.png", 5000,
		// 1000, "Histogram plot", "Histogram plot", "Pixel values", "No Of
		// Occurences");
		// } catch (Exception e) {
		// }
		// System.out.println(Arrays.toString(histogram));
		return new NumberTriplet(min, max, (int) (mean + (5 * stddev)));
	}

	public static NumberTriplet findMinMax(float[][] input, int kernelHeight, int kernelWidth) {
		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;
		final int height = input.length;
		final int width = input[0].length;
		double sum = 0.0f;
		double sumsq = 0.0f;
		// int no = 0;
		// DescriptiveStatistics ds = new DescriptiveStatistics();
		// float count = 0.00015f;
		// int[] histogram = new int[200];
		for (int i = kernelHeight / 2; i < (height - (kernelHeight / 2)); i++) {
			for (int j = kernelWidth / 2; j < (width - (kernelWidth / 2)); j++) {
				if (input[i][j] < min) {
					min = input[i][j];
				}
				if (input[i][j] > max) {
					max = input[i][j];
					// System.out.println("Found new max of " + max + " at [" + i + "," + j + "]");
				}
				sum += input[i][j];
				sumsq += input[i][j] * input[i][j];
				// no++;
				// ds.addValue(input[i][j]);
				// int bucket = (int) (input[i][j] / count);
				// histogram[bucket]++;
			}
		}
		double mean = ((sum * 1.0) / ((height - kernelHeight) * (width - kernelWidth)));
		double stddev = FastMath
				.sqrt(((sumsq * 1.0) / ((height - kernelHeight) * (width - kernelWidth))) - (mean * mean));
		// System.out.println("Mean = " + ((sum * 1.0) / no) + "; StdDev = "
		// + (((sumsq * 1.0) / no) - (((sum * 1.0) / no) * ((sum * 1.0) / no))));
		// System.out.println("Mean = " + ds.getMean() + "; StdDev = " +
		// ds.getStandardDeviation());
		// try {
		// PDFHandlerChartFactory.drawBarChart(histogram, 0,
		// "E:\\TechWerx\\Java\\Working\\1000-histData.png", 5000,
		// 1000, "Histogram plot", "Histogram plot", "Pixel values", "No Of
		// Occurences");
		// } catch (Exception e) {
		// }
		// System.out.println(Arrays.toString(histogram));
		return new NumberTriplet(min, max, (float) (mean + (5 * stddev)));
	}

	public static NumberTriplet findMinMax(byte[][] input, int kernelHeight, int kernelWidth) {
		byte min = Byte.MAX_VALUE;
		byte max = Byte.MIN_VALUE;
		final int height = input.length;
		final int width = input[0].length;
		double sum = 0.0f;
		double sumsq = 0.0f;
		for (int i = kernelHeight / 2; i < (height - (kernelHeight / 2)); i++) {
			for (int j = kernelWidth / 2; j < (width - (kernelWidth / 2)); j++) {
				if (Byte.compare(input[i][j], min) < 0) {
					min = input[i][j];
				}
				if (Byte.compare(input[i][j], max) > 0) {
					max = input[i][j];
				}
				sum += input[i][j];
				sumsq += input[i][j] * input[i][j];
			}
		}
		double mean = (sum * 1.0) / ((width - kernelWidth) * (height - kernelHeight));
		double stddev = FastMath
				.sqrt(((sumsq * 1.0) / ((width - kernelWidth) * (height - kernelHeight))) - (mean * mean));
		return new NumberTriplet(min, max, (byte) (mean + (5 * stddev)));
	}

	/*
	 * // Too slow, as it is spending time to do sorting
	 *
	 * public static NumberTriplet findMinMax(byte[][] input) { final byte[] rowMins
	 * = new byte[input.length]; final byte[] rowMaxs = new byte[input.length];
	 * final int width = input[0].length; byte[] rowExtract = new byte[width]; int j
	 * = 0; for (byte[] row : input) { rowExtract = Arrays.copyOf(row, width);
	 * Arrays.parallelSort(rowExtract); rowMins[j] = rowExtract[0]; rowMaxs[j++] =
	 * rowExtract[width - 1]; } Arrays.parallelSort(rowMins);
	 * Arrays.parallelSort(rowMaxs); return new NumberTriplet(rowMins[0],
	 * rowMaxs[rowMaxs.length - 1]); }
	 */

	/*
	 * // Too slow, as it is spending time to do sorting
	 *
	 * public static NumberTriplet findMinMax(float[][] input) { final float[]
	 * rowMins = new float[input.length]; final float[] rowMaxs = new
	 * float[input.length]; final int width = input[0].length; float[] rowExtract =
	 * new float[width]; int j = 0; for (float[] row : input) { rowExtract =
	 * Arrays.copyOf(row, width); Arrays.parallelSort(rowExtract); rowMins[j] =
	 * rowExtract[0]; rowMaxs[j++] = rowExtract[width - 1]; }
	 * Arrays.parallelSort(rowMins); Arrays.parallelSort(rowMaxs); return new
	 * NumberTriplet(rowMins[0], rowMaxs[rowMaxs.length - 1]); }
	 */

	public Object getFirst() {
		if (this.firstName.contentEquals(UNDEFINED)) {
			return null;
		}
		if (this.type == INT) {
			return Integer.valueOf((int) this.first);
		}
		if (this.type == DOUBLE) {
			return Double.valueOf(this.first);
		}
		if (this.type == FLOAT) {
			return Float.valueOf((float) this.first);
		}
		if (this.type == BYTE) {
			return Byte.valueOf((byte) this.first);
		}
		return null;
	}

	public Object getSecond() {
		if (this.firstName.contentEquals(UNDEFINED)) {
			return null;
		}
		if (this.type == INT) {
			return Integer.valueOf((int) this.second);
		}
		if (this.type == DOUBLE) {
			return Double.valueOf(this.second);
		}
		if (this.type == FLOAT) {
			return Float.valueOf((float) this.second);
		}
		if (this.type == BYTE) {
			return Byte.valueOf((byte) this.second);
		}
		return null;
	}

	public Object getThird() {
		if (this.firstName.contentEquals(UNDEFINED)) {
			return null;
		}
		if (this.type == INT) {
			return Integer.valueOf((int) this.third);
		}
		if (this.type == DOUBLE) {
			return Double.valueOf(this.third);
		}
		if (this.type == FLOAT) {
			return Float.valueOf((float) this.third);
		}
		if (this.type == BYTE) {
			return Byte.valueOf((byte) this.third);
		}
		return null;
	}

	public Object get(String name) {
		if (this.firstName.contentEquals(name)) {
			if (this.type == INT) {
				return Integer.valueOf((int) this.first);
			}
			if (this.type == DOUBLE) {
				return Double.valueOf(this.first);
			}
			if (this.type == FLOAT) {
				return Float.valueOf((float) this.first);
			}
			if (this.type == BYTE) {
				return Byte.valueOf((byte) this.first);
			}
		}
		if (this.secondName.contentEquals(name)) {
			if (this.type == INT) {
				return Integer.valueOf((int) this.second);
			}
			if (this.type == DOUBLE) {
				return Double.valueOf(this.second);
			}
			if (this.type == FLOAT) {
				return Float.valueOf((float) this.second);
			}
			if (this.type == BYTE) {
				return Byte.valueOf((byte) this.second);
			}
		}
		if (this.thirdName.contentEquals(name)) {
			if (this.type == INT) {
				return Integer.valueOf((int) this.third);
			}
			if (this.type == DOUBLE) {
				return Double.valueOf(this.third);
			}
			if (this.type == FLOAT) {
				return Float.valueOf((float) this.third);
			}
			if (this.type == BYTE) {
				return Byte.valueOf((byte) this.third);
			}
		}
		return null;
	}

	public Number getMin() {
		if (this.type == INT) {
			if (!this.thirdName.contentEquals(UNDEFINED)) {
				return Integer.min(Integer.min((int) this.first, (int) this.second), (int) this.third);
			} else {
				return Integer.min((int) this.first, (int) this.second);
			}
		}
		if (this.type == DOUBLE) {
			if (!this.thirdName.contentEquals(UNDEFINED)) {
				return Double.min(Double.min(this.first, this.second), this.third);
			} else {
				return Double.min(this.first, this.second);
			}
		}
		if (this.type == FLOAT) {
			if (!this.thirdName.contentEquals(UNDEFINED)) {
				return Float.min(Float.min((float) this.first, (float) this.second), (float) this.third);
			} else {
				return Float.min((float) this.first, (float) this.second);
			}
		}
		if (this.type == BYTE) {
			if (!this.thirdName.contentEquals(UNDEFINED)) {
				if (Byte.compare((byte) this.first, (byte) this.second) < 0) {
					if (Byte.compare((byte) this.first, (byte) this.third) < 0) {
						return (byte) this.first;
					} else {
						return (byte) this.third;
					}
				} else {
					if (Byte.compare((byte) this.second, (byte) this.third) < 0) {
						return (byte) this.second;
					} else {
						return (byte) this.third;
					}
				}
			} else {
				if (Byte.compare((byte) this.first, (byte) this.second) < 0) {
					return (byte) this.first;
				} else {
					return (byte) this.second;
				}
			}
		}
		return null;
	}

	public Number getMax() {

		if (this.type == INT) {
			if (!this.thirdName.contentEquals(UNDEFINED)) {
				return Integer.max(Integer.max((int) this.first, (int) this.second), (int) this.third);
			} else {
				return Integer.max((int) this.first, (int) this.second);
			}
		}
		if (this.type == DOUBLE) {
			if (!this.thirdName.contentEquals(UNDEFINED)) {
				return Double.max(Double.max(this.first, this.second), this.third);
			} else {
				return Double.max(this.first, this.second);
			}
		}
		if (this.type == FLOAT) {
			if (!this.thirdName.contentEquals(UNDEFINED)) {
				return Float.max(Float.max((float) this.first, (float) this.second), (float) this.third);
			} else {
				return Float.max((float) this.first, (float) this.second);
			}
		}
		if (this.type == BYTE) {
			if (!this.thirdName.contentEquals(UNDEFINED)) {
				if (Byte.compare((byte) this.first, (byte) this.second) > 0) {
					if (Byte.compare((byte) this.first, (byte) this.third) > 0) {
						return (byte) this.first;
					} else {
						return (byte) this.third;
					}
				} else {
					if (Byte.compare((byte) this.second, (byte) this.third) > 0) {
						return (byte) this.second;
					} else {
						return (byte) this.third;
					}
				}
			} else {
				if (Byte.compare((byte) this.first, (byte) this.second) > 0) {
					return (byte) this.first;
				} else {
					return (byte) this.second;
				}
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return new StringBuffer("First : ").append(this.first).append("; Second : ").append(this.second)
				.append("; Third : ").append(this.third).toString();
	}
}
