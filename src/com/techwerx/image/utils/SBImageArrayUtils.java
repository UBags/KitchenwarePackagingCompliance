package com.techwerx.image.utils;

import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.util.FastMath;

import com.techwerx.image.SBImage;
import com.techwerx.image.streams.FastByteArrayOutputStream;

public class SBImageArrayUtils {

	// Takes around 16-20 ms to flatten a 2D to 1D for a 3500 x 5500 pixel array, of
	// which 9-10 ms is for allocating memory
	public static byte[] flatten(byte[][] input) {
		final byte[] out = new byte[input.length * input[0].length];
		final int width = input[0].length;
		// final int height = input.length;
		int i = 0;
		for (byte[] row : input) {
			System.arraycopy(row, 0, out, width * i++, width);
		}
		return out;
	}

	// Takes around 70-90 ms to flatten a 2D to 1D for a 3500 x 5500 pixel array, of
	// which 9-10 ms is for allocating memory
	public static int[] flatten(int[][] input) {

		final int[] out = new int[input.length * input[0].length];
		final int width = input[0].length;
		// final int height = input.length;
		int i = 0;
		for (int[] row : input) {
			System.arraycopy(row, 0, out, width * i++, width);
		}
		Arrays.parallelSetAll(out, e -> ((out[e] & 0xFF) << 16) | ((out[e] & 0xFF) << 8) | ((out[e] & 0xFF) << 0));
		return out;
	}

	// Takes 20 ms for flattening a 3500 x 5500 array; almost same as the one that
	// uses System.arrayCopy(), out of which
	// allocating memory for 18M cells takes 9-10 ms
	public static byte[] iterateFlatten(byte[][] input) {
		final int height = input.length;
		final int width = input[0].length;
		final byte[] out = new byte[height * input[0].length];
		int i = 0;
		for (final byte[] row : input) {
			int j = 0;
			for (final byte b : row) {
				out[(i * width) + j++] = b;
			}
			++i;
		}
		return out;
	}

	// Takes 55 ms for flattening a 3500 x 5500 array; almost same as the one that
	// uses System.arrayCopy(), out of which
	// allocating memory for 18M cells takes 9-10 ms
	public static int[] iterateFlatten(int[][] input) {

		final int height = input.length;
		final int width = input[0].length;
		final int[] out = new int[height * input[0].length];
		int i = 0;
		for (final int[] row : input) {
			int j = 0;
			for (final int b : row) {
				out[(i * width) + j++] = ((b & 0xFF) << 16) | ((b & 0xFF) << 8) | ((b & 0xFF) << 0);
			}
			++i;
		}
		return out;
	}

	public static int[] parallelIterateFlatten(final int[][] input) {

		final int height = input.length;
		final int width = input[0].length;
		final int[] out = new int[height * input[0].length];
		final int divisions = 3;
		final int heightEachDivision = height / divisions;
		ExecutorService threadService = Executors.newFixedThreadPool(divisions);
		final ArrayList<CompletableFuture<Object>> threads = new ArrayList<CompletableFuture<Object>>(divisions);
		for (int divNo = 0; divNo < divisions; ++divNo) {
			final int startingY = divNo * heightEachDivision;
			final int endingY = (divNo == (divisions - 1)) ? height : (divNo + 1) * heightEachDivision;
			threads.add(CompletableFuture.supplyAsync(() -> {
				for (int y = startingY; y < endingY; ++y) {
					final int[] row = input[y];
					int j = 0;
					for (final int b : row) {
						out[(y * width) + j++] = ((b & 0xFF) << 16) | ((b & 0xFF) << 8) | ((b & 0xFF) << 0);
					}
				}
				return Boolean.TRUE;
			}, threadService));
		}
		CompletableFuture.allOf(threads.toArray(new CompletableFuture[threads.size()])).join();
		threadService.shutdown();

		return out;
	}

	public static byte[] iterateFlattenToByteArray(int[][] input) {

		final int height = input.length;
		final int width = input[0].length;
		final byte[] out = new byte[height * width];
		int i = 0;
		for (final int[] row : input) {
			int j = 0;
			for (final int b : row) {
				out[(i * width) + j++] = (byte) b;
			}
			++i;
		}
		// System.out.println(Arrays.toString(out));
		return out;
	}

	public static IntStream intStream(int[] array) {
		return IntStream.range(0, array.length).parallel().map(index -> array[index]);
	}

	public static byte[] toByteArray(IntStream stream) {
		return stream.collect(ByteArrayOutputStream::new, (baos, i) -> baos.write((byte) i),
				(baos1, baos2) -> baos1.write(baos2.toByteArray(), 0, baos2.size())).toByteArray();
	}

	public static int[] addIntToAll(int[] array, int addend) {
		return intStream(array).parallel().map(b -> b + addend).toArray();
	}

	public static int[] toIntArrayUsingStreams(int[] array) {
		return SBImageArrayUtils.addIntToAll(array, -SBImage.MAX);
	}

	// Takes 40-45 ms for expanding 18M cells into a 3500 x 5500 array, out of which
	// allocating memory for 18M cells takes 9-10 ms
	public static byte[][] from1DTo2D(final byte[] input, final int height) {
		int width = input.length / height;
		final byte[][] bytes = new byte[height][width];

		for (int i = 0; i < height; i++) {
			System.arraycopy(input, i * width, bytes[i], 0, width);
		}
		return bytes;
	}

	// Takes 275 ms for expanding 18M cells into a 3500 x 5500 array, out of which
	// allocating memory for 18M cells takes 9-10 ms
	public static int[][] from1DTo2D(final int[] input, final int height) {
		// Note: This method modifies the input array. It works for me, but may not be
		// suitable for others

		final int width = input.length / height;
		final int[][] out = new int[height][width];
		Arrays.parallelSetAll(input, index -> (input[index] & 0xFF));

		for (int i = 0; i < height; i++) {
			System.arraycopy(input, i * width, out[i], 0, width);
		}
		// for (int[] row : out) {
		// Arrays.parallelSetAll(row, e -> (row[e] & 0xFF));
		// }
		return out;
	}

	// Takes 220 ms for expanding 18M cells into a 3500 x 5500 array, out of which
	// allocating memory for 18M cells takes 9-10 ms
	public static int[][] iterateFrom1DTo2D(final int[] input, final int height) {
		final int length = input.length;
		final int width = length / height;
		final int[][] out = new int[height][width];
		final int divisions = 2; // 1 or 2 are optimal for plain copying type stuff - Tested and checked !!
		final Semaphore semaphore = new Semaphore(divisions);
		final ArrayList<Integer> integer = new ArrayList<>(divisions);
		final int subHeight = height / divisions;
		for (int divNo = 0; divNo < divisions; ++divNo) {
			integer.add(divNo);
		}
		final Iterator<Integer> iterator = integer.iterator();
		for (int divNo = 0; divNo < divisions; ++divNo) {
			new Runnable() {
				@Override
				public void run() {
					try {
						semaphore.acquire();
					} catch (InterruptedException e) {
						return;
					}
					final int index = iterator.next();
					// System.out.println(index);
					final int end = (index + 1);
					final int yStart = index * subHeight;
					final int yEnd = (end == (divisions - 1)) ? end * subHeight : height;
					int x = 0;
					for (int y = yStart; y < yEnd;) {
						out[y][x] = input[(y * width) + x] & 0xFF;
						y = (x == (width - 1)) ? ++y : y;
						x = (x == (width - 1)) ? 0 : ++x;
					}
					semaphore.release();
				}
			}.run();
		}
		while (semaphore.availablePermits() != divisions) {
			// Thread.onSpinWait();
			try {
				Thread.sleep(5);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
		// System.out.println(Arrays.deepToString(out));
		return out;
	}

	// Takes 40-45 ms for expanding 18M cells into a 3500 x 5500 array, out of which
	// allocating memory for 18M cells takes 9-10 ms
	public static byte[][] iterateFrom1DTo2D(final byte[] input, final int height) {
		final int width = input.length / height;
		final byte[][] bytes = new byte[height][width];
		int i = 0;
		do {
			bytes[i] = Arrays.copyOfRange(input, i * width, (++i * width));
		} while (i < height);
		return bytes;
	}

	// Takes 220 ms for expanding 18M cells into a 3500 x 5500 array, out of which
	// allocating memory for 18M cells takes 9-10 ms
	public static int[][] parallelFrom1DTo2D(final int[] input, final int height) {
		// final int length = input.length;
		final int width = input.length / height;
		final int[][] out = new int[height][width];
		final int yDivisions = ((height < 1000) ? 2
				: ((height < 2000) ? 2 : ((height < 3000) ? 2 : ((height < 4000) ? 2 : 2))));
		final int subImageHeight = height / yDivisions;
		ExecutorService threadService = Executors.newFixedThreadPool((yDivisions / 3) + 1);
		final ArrayList<CompletableFuture<Object>> cfs = new ArrayList<>(yDivisions);
		for (int yCount = 0; yCount < yDivisions; ++yCount) {
			final int cHeight = !(yCount == (yDivisions - 1)) ? height / yDivisions
					: height - ((height / yDivisions) * (yDivisions - 1));
			final int yStart = yCount * subImageHeight;
			cfs.add(CompletableFuture.supplyAsync(() -> {
				for (int i = yStart; i < (yStart + cHeight); ++i) {
					for (int j = 0; j < width; ++j) {
						out[i][j] = input[(i * width) + j] & 0xFF;
					}
				}
				return Boolean.TRUE;
			}, threadService));
		}
		// System.out.println("Waiting for all threads to finish");
		// CompletableFuture<Void> allFutures = CompletableFuture.allOf(cfs.toArray(new
		// CompletableFuture[cfs.size()]));
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		threadService.shutdown();

		// Completa bleFuture<List<Object>> allCFFuture = allFutures.thenApply(v -> {
		// return cfs.stream().map(cf -> cf.join()).collect(Collectors.toList());
		// });

		// Object temp = null;
		/*
		 * for (CompletableFuture<Object> cf : cfs) { try { cf.get(); } catch
		 * (InterruptedException e) { e.printStackTrace(); } catch (ExecutionException
		 * e) { e.printStackTrace(); } }
		 */
		return out;
	}

	public static int[][] parallelFrom1DTo2D(final byte[] input, final int height) {
		final int width = input.length / height;
		final int[][] out = new int[height][width];
		final int yDivisions = ((height < 1000) ? 2
				: ((height < 2000) ? 2 : ((height < 3000) ? 2 : ((height < 4000) ? 2 : 2))));
		final int subImageHeight = height / yDivisions;
		ExecutorService threadService = Executors.newFixedThreadPool((yDivisions / 2) + 1);
		final ArrayList<CompletableFuture<Object>> cfs = new ArrayList<>(yDivisions);
		for (int yCount = 0; yCount < yDivisions; yCount++) {
			final int cHeight = !(yCount == (yDivisions - 1)) ? height / yDivisions
					: height - ((height / yDivisions) * (yDivisions - 1));
			final int yStart = yCount * subImageHeight;
			cfs.add(CompletableFuture.supplyAsync(() -> {
				for (int i = yStart; i < (yStart + cHeight); i++) {
					for (int j = 0; j < width; ++j) {
						int index = (i * width) + j;
						out[i][j] = input[index] >= 0 ? input[index] : 256 + input[index];
					}
				}
				return Boolean.TRUE;
			}, threadService));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		threadService.shutdown();
		return out;
	}

	// Takes 220 ms for expanding 18M cells into a 3500 x 5500 array, out of which
	// allocating memory for 18M cells takes 9-10 ms
	public static int[][] parallelSystemArraycopyFrom1DTo2D(final int[] input, final int height) {
		Arrays.parallelSetAll(input, index -> input[index] & 0xFF);
		final int width = input.length / height;
		final int[][] out = new int[height][width];
		final int yDivisions = ((height < 1000) ? 2
				: ((height < 2000) ? 2 : ((height < 3000) ? 3 : ((height < 4000) ? 3 : 4))));
		final int subImageHeight = height / yDivisions;
		ExecutorService threadService = Executors.newFixedThreadPool((yDivisions / 2) + 1);
		final ArrayList<CompletableFuture<Object>> cfs = new ArrayList<>(yDivisions);
		for (int yCount = 0; yCount < yDivisions; ++yCount) {
			final int cHeight = !(yCount == (yDivisions - 1)) ? height / yDivisions
					: height - ((height / yDivisions) * (yDivisions - 1));
			final int yStart = yCount * subImageHeight;
			cfs.add(CompletableFuture.supplyAsync(() -> {
				for (int i = yStart; i < (yStart + cHeight); i++) {
					System.arraycopy(input, yStart * width, out[i], 0, width);
				}
				return Boolean.TRUE;
			}, threadService));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		threadService.shutdown();
		return out;
	}

	// Shallow copy of a 2D object array. Cannot avoid clone, as it has security
	// risks - http://www.javapractices.com/topic/TopicAction.do?Id=71
	public static <T> T[][] arrayCopy(T[][] array) {
		return Arrays.stream(array).map(el -> el.clone()).toArray(a -> array.clone());
		// return Arrays.stream(matrix).map(T[]::clone).toArray(T[][]::new);
		// return Arrays.stream(array).map(a -> Arrays.copyOf(a,
		// a.length)).toArray(T[][]::new);
	}

	// Takes 38-40 ms for copying a 3500 x 5500 matrix with values ranging from
	// 0-255.
	// We should avoid clone, as it has security risks -
	// http://www.javapractices.com/topic/TopicAction.do?Id=71
	public static int[][] arrayCopy(int[][] array) {
		// return Arrays.stream(array).map(el -> el.clone()).toArray(a ->
		// array.clone());
		// return Arrays.stream(matrix).map(int[]::clone).toArray(int[][]::new);
		// return Arrays.stream(array).map(a -> Arrays.copyOf(a,
		// a.length)).toArray(int[][]::new);
		int out[][] = new int[array.length][array[0].length];
		int i = 0;
		for (int[] row : array) {
			System.arraycopy(row, 0, out[i], 0, row.length);
			++i;
		}
		return out;
	}

	public static double[][] arrayCopy(double[][] array) {
		// return Arrays.stream(array).map(el -> el.clone()).toArray(a ->
		// array.clone());
		// return Arrays.stream(matrix).map(int[]::clone).toArray(int[][]::new);
		double out[][] = new double[array.length][array[0].length];
		int i = 0;
		for (double[] row : array) {
			System.arraycopy(row, 0, out[i], 0, row.length);
			++i;
		}
		return out;
	}

	// Takes 38-40 ms for copying a 3500 x 5500 matrix with values ranging from
	// 0-255.
	// We should avoid clone, as it has security risks -
	// http://www.javapractices.com/topic/TopicAction.do?Id=71
	public static float[][] arrayCopyIntToFloat(int[][] array) {
		// return Arrays.stream(array).map(el -> el.clone()).toArray(a ->
		// array.clone());
		// return Arrays.stream(matrix).map(int[]::clone).toArray(int[][]::new);
		// for a 1D array : Arrays.stream(population).map(d -> (float) d).toArray();
		// return Arrays.stream(array).map(a -> Arrays.copyOf(a,
		// a.length)).toArray(float[][]::new);
		float out[][] = new float[array.length][array[0].length];
		int i = 0;
		for (int[] row : array) {
			int j = -1;
			for (int b : row) {
				out[i][++j] = 1.0f * b;
			}
			++i;
		}
		return out;
	}

	public static int[][] arrayCopyFloatToInt(float[][] array) {
		// return Arrays.stream(array).map(el -> el.clone()).toArray(a ->
		// array.clone());
		// return Arrays.stream(matrix).map(int[]::clone).toArray(int[][]::new);
		// for a 1D array : Arrays.stream(population).map(d -> (float) d).toArray();
		// return Arrays.stream(array).map(a -> Arrays.copyOf(a,
		// a.length)).toArray(float[][]::new);
		int out[][] = new int[array.length][array[0].length];
		int i = 0;
		for (float[] row : array) {
			int j = -1;
			for (float b : row) {
				out[i][++j] = (int) b;
			}
			++i;
		}
		return out;
	}

	public static int[][] arrayCopyDoubleToInt(double[][] array) {
		// return Arrays.stream(array).map(el -> el.clone()).toArray(a ->
		// array.clone());
		// return Arrays.stream(matrix).map(int[]::clone).toArray(int[][]::new);
		// for a 1D array : Arrays.stream(population).map(d -> (float) d).toArray();
		// return Arrays.stream(array).map(a -> Arrays.copyOf(a,
		// a.length)).toArray(float[][]::new);
		int out[][] = new int[array.length][array[0].length];
		int i = 0;
		for (double[] row : array) {
			int j = -1;
			for (double b : row) {
				out[i][++j] = (int) b;
			}
			++i;
		}
		return out;
	}

	public static double[][] arrayCopyIntToDouble(int[][] array) {
		// return Arrays.stream(array).map(el -> el.clone()).toArray(a ->
		// array.clone());
		// return Arrays.stream(matrix).map(int[]::clone).toArray(int[][]::new);
		// for a 1D array : Arrays.stream(population).map(d -> (float) d).toArray();
		// return Arrays.stream(array).map(a -> Arrays.copyOf(a,
		// a.length)).toArray(double[][]::new);
		double out[][] = new double[array.length][array[0].length];
		int i = 0;
		for (int[] row : array) {
			int j = -1;
			for (int b : row) {
				out[i][++j] = 1.0 * b;
			}
			++i;
		}
		return out;
	}

	// Takes 38-40 ms for copying a 3500 x 5500 matrix with values ranging from
	// 0-255.
	// We should avoid clone, as it has security risks -
	// http://www.javapractices.com/topic/TopicAction.do?Id=71
	public static byte[][] arrayCopy(byte[][] array) {
		// return Arrays.stream(array).map(el -> el.clone()).toArray(a ->
		// array.clone());
		// return Arrays.stream(matrix).map(byte[]::clone).toArray(byte[][]::new);
		return Arrays.stream(array).map(a -> Arrays.copyOf(a, a.length)).toArray(byte[][]::new);
	}

	// shallowCopy of the objects in the array
	public static <T> T[] arrayCopy(T[] array) {
		return Arrays.copyOf(array, array.length);
	}

	// deepCopy of an array of 1D Objects
	public static <T> T[] deepCopy(T[] array) {
		T[] obj = null;
		try {
			// Write the object out to a byte array
			FastByteArrayOutputStream fbos = new FastByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(fbos);
			out.writeObject(array);
			out.flush();
			out.close();

			// Retrieve an input stream from the byte array and read
			// a copy of the object back in.
			ObjectInputStream in = new ObjectInputStream(fbos.getInputStream());
			obj = (T[]) in.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
		return obj;
	}

	// deepCopy of an array of 2D Objects. Takes 185-200 ms for a 2D array of size
	// 18 M cells (3500 x 5500)
	public static <T> T[][] deepCopy(T[][] array) {
		T[][] obj = null;
		try {
			// Write the object out to a byte array
			FastByteArrayOutputStream fbos = new FastByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(fbos);
			out.writeObject(array);
			out.flush();
			out.close();

			// Retrieve an input stream from the byte array and read
			// a copy of the object back in.
			ObjectInputStream in = new ObjectInputStream(fbos.getInputStream());
			obj = (T[][]) in.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
		return obj;
	}

	// All 3 methods - arraycopy, clone, and iteratively copying - average 38-40 ms
	// for
	// copying a 3500 x 5500 matrix with values ranging from 0-255.
	// We should avoid clone, as it has security risks -
	// http://www.javapractices.com/topic/TopicAction.do?Id=71
	public static int[] arrayCopy(int[] array) {
		final int[] array1 = new int[array.length];
		System.arraycopy(array, 0, array1, 0, array.length);
		return array1;
	}

	// All 3 methods - arraycopy, clone, and iteratively copying - average 38-40 ms
	// for
	// copying a 3500 x 5500 matrix with values ranging from 0-255.
	// We should avoid clone, as it has security risks -
	// http://www.javapractices.com/topic/TopicAction.do?Id=71
	public static byte[] arrayCopy(byte[] array) {
		final byte[] array1 = new byte[array.length];
		System.arraycopy(array, 0, array1, 0, array.length);
		return array1;
	}

	/*
	 * public static ByteBuffer cloneByteBuffer(final ByteBuffer original) { //
	 * Create clone with same capacity as original. final ByteBuffer clone =
	 * (original.isDirect()) ? ByteBuffer.allocateDirect(original.capacity()) :
	 * ByteBuffer.allocate(original.capacity());
	 *
	 * // Create a read-only copy of the original. // This allows reading from the
	 * original without modifying it. final ByteBuffer readOnlyCopy =
	 * original.asReadOnlyBuffer();
	 *
	 * // Flip and read from the original. readOnlyCopy.flip();
	 * clone.put(readOnlyCopy); clone.position(original.position());
	 * clone.limit(original.limit()); clone.order(original.order());
	 *
	 * return clone; }
	 */

	public static NumberTriplet rowStatistics(int[] imageRow, boolean findMedian) {
		// return ImageStatsHelper.getStats(imageRow, findMedian);
		double[] input = new double[imageRow.length];
		for (int i = 0; i < imageRow.length; ++i) {
			input[i] = imageRow[i];
		}
		double average = 0.0;
		double variance = 0.0;
		for (double p : input) {
			average += p;
			variance += p * p;
		}
		average = (average * 1.0) / input.length;
		variance = ((variance * 1.0) / input.length) - (average * average);
		variance = FastMath.sqrt(variance);

		double median = findMedian ? new Median().evaluate(input) : Double.MIN_VALUE;
		return new NumberTriplet(average, variance, median);
	}

	public static NumberTriplet imageStatistics(SBImage image, boolean findMedian) {
		// return ImageStatsHelper.getStats(imageRow, findMedian);
		final Object monitor = new Object();
		final Double median;
		class MedianRunner implements Runnable {
			double med;

			@Override
			public void run() {
				synchronized (monitor) {
					double[] pixelArray = new double[image.height * image.width];
					int i = 0;
					for (final int[] row : image.pixels) {
						int j = 0;
						for (final int b : row) {
							pixelArray[(i * image.width) + j++] = b & 0xFF;
						}
						++i;
					}
					this.med = new Median().evaluate(pixelArray);
				}
			}
		}
		;
		MedianRunner medianRunner = new MedianRunner();
		if (findMedian) {
			new Thread(medianRunner).start();
		}
		double average = 0.0;
		double variance = 0.0;
		for (final int[] row : image.pixels) {
			for (final int b : row) {
				average += b;
				variance += b * b;
			}
		}
		average = (average * 1.0) / (image.height * image.width);
		variance = ((variance * 1.0) / (image.height * image.width)) - (average * average);
		variance = FastMath.sqrt(variance);

		if (findMedian) {
			synchronized (monitor) {
				median = medianRunner.med;
			}
		} else {
			median = Double.MIN_VALUE;
		}
		return new NumberTriplet(average, variance, median);
	}

	public static NumberTriplet imageStatistics(int[][] pixels, boolean findMedian) {
		// return ImageStatsHelper.getStats(imageRow, findMedian);
		final Object monitor = new Object();
		final Double median;
		class MedianRunner implements Runnable {
			double med;

			@Override
			public void run() {
				synchronized (monitor) {
					double[] pixelArray = new double[pixels.length * pixels[0].length];
					int i = 0;
					for (final int[] row : pixels) {
						int j = 0;
						for (final int b : row) {
							pixelArray[(i * pixels[0].length) + j++] = b & 0xFF;
						}
						++i;
					}
					this.med = new Median().evaluate(pixelArray);
				}
			}
		}
		;
		MedianRunner medianRunner = new MedianRunner();
		if (findMedian) {
			new Thread(medianRunner).start();
		}
		double average = 0.0;
		double variance = 0.0;
		for (final int[] row : pixels) {
			for (final int b : row) {
				average += b;
				variance += b * b;
			}
		}
		average = (average * 1.0) / (pixels.length * pixels[0].length);
		variance = ((variance * 1.0) / (pixels.length * pixels[0].length)) - (average * average);
		variance = FastMath.sqrt(variance);

		if (findMedian) {
			synchronized (monitor) {
				median = medianRunner.med;
			}
		} else {
			median = Double.MIN_VALUE;
		}
		return new NumberTriplet(average, variance, median);
	}

	public static void setBorderValues(int[][] original, int[][] target, int kernelHeight, int kernelWidth)
			throws Exception {

		if ((original.length != target.length) || (original[0].length != target[0].length)) {
			throw new Exception("Original and Target matrices should be on equal dimensions");
		}
		for (int y = 0; y < (kernelHeight / 2); ++y) {
			for (int x = 0; x < original[0].length; ++x) {
				target[y][x] = original[y][x];
			}
		}
		for (int y = original.length - (kernelHeight / 2); y < original.length; ++y) {
			for (int x = 0; x < original[0].length; ++x) {
				target[y][x] = original[y][x];
			}
		}
		for (int y = kernelHeight / 2; y < (original.length - 1 - (kernelHeight / 2)); ++y) {
			for (int x = 0; x < (kernelWidth / 2); ++x) {
				target[y][x] = original[y][x];
			}
		}
		for (int y = kernelHeight / 2; y < (original.length - 1 - (kernelHeight / 2)); ++y) {
			for (int x = original[0].length - (kernelWidth / 2); x < original[0].length; ++x) {
				target[y][x] = original[y][x];
			}
		}
	}

	public void setBorderValues(float[][] original, float[][] target, int kernelHeight, int kernelWidth)
			throws Exception {

		if ((original.length != target.length) || (original[0].length != target[0].length)) {
			throw new Exception("Original and Target matrices should be on equal dimensions");
		}
		for (int y = 0; y < (kernelHeight / 2); ++y) {
			for (int x = 0; x < original[0].length; ++x) {
				target[y][x] = original[y][x];
			}
		}
		for (int y = original.length - (kernelHeight / 2); y < original.length; ++y) {
			for (int x = 0; x < original[0].length; ++x) {
				target[y][x] = original[y][x];
			}
		}
		for (int y = kernelHeight / 2; y < (original.length - 1 - (kernelHeight / 2)); ++y) {
			for (int x = 0; x < (kernelWidth / 2); ++x) {
				target[y][x] = original[y][x];
			}
		}
		for (int y = kernelHeight / 2; y < (original.length - 1 - (kernelHeight / 2)); ++y) {
			for (int x = original[0].length - (kernelWidth / 2); x < original[0].length; ++x) {
				target[y][x] = original[y][x];
			}
		}
	}

	public void setBorderValues(double[][] original, double[][] target, int kernelHeight, int kernelWidth)
			throws Exception {

		if ((original.length != target.length) || (original[0].length != target[0].length)) {
			throw new Exception("Original and Target matrices should be on equal dimensions");
		}
		for (int y = 0; y < (kernelHeight / 2); ++y) {
			for (int x = 0; x < original[0].length; ++x) {
				target[y][x] = original[y][x];
			}
		}
		for (int y = original.length - (kernelHeight / 2); y < original.length; ++y) {
			for (int x = 0; x < original[0].length; ++x) {
				target[y][x] = original[y][x];
			}
		}
		for (int y = kernelHeight / 2; y < (original.length - 1 - (kernelHeight / 2)); ++y) {
			for (int x = 0; x < (kernelWidth / 2); ++x) {
				target[y][x] = original[y][x];
			}
		}
		for (int y = kernelHeight / 2; y < (original.length - 1 - (kernelHeight / 2)); ++y) {
			for (int x = original[0].length - (kernelWidth / 2); x < original[0].length; ++x) {
				target[y][x] = original[y][x];
			}
		}
	}

	// this is 40% faster than transpose, even though parallel threads have been
	// yanked out of the code
	public static int[][] parallelTranspose(int[][] input) {
		int yDivisions = (input.length / 5) + 1;
		int xDivisions = (input[0].length / 5) + 1;
		// XYDivisions xyDiv = new XYDivisions(xDivisions, yDivisions);
		int noOfDivisions = xDivisions * yDivisions;
		Rectangle[] rectangles = new Rectangle[noOfDivisions];
		for (int i = 0; i < noOfDivisions; i++) {
			final boolean topEdge = ((i / xDivisions) == 0);
			final boolean rightEdge = ((i % xDivisions) == (xDivisions - 1));
			final boolean bottomEdge = (xDivisions >= (noOfDivisions - i));
			final boolean leftEdge = ((i % xDivisions) == 0);
			final int cx = leftEdge ? 0 : ((i % xDivisions) * (input[0].length / xDivisions));
			final int cy = topEdge ? 0 : ((i / xDivisions) * (input.length / yDivisions));
			final int ex = (rightEdge || (xDivisions == 1)) ? input[0].length - 1
					: ((input[0].length / xDivisions) * ((i + 1) % xDivisions)) - 1;
			final int ey = (bottomEdge || (yDivisions == 1)) ? input.length - 1
					: ((input.length / yDivisions) * ((i / xDivisions) + 1)) - 1;
			final int cWidth = (ex - cx) + 1;
			final int cHeight = (ey - cy) + 1;
			rectangles[i] = new Rectangle(cx, cy, cWidth, cHeight);
		}
		final int[][] out = new int[input[0].length][input.length];
		// ArrayList<CompletableFuture<Boolean>> cfs = new
		// ArrayList<CompletableFuture<Boolean>>(noOfDivisions);
		for (int i = 0; i < yDivisions; ++i) {
			for (int j = 0; j < xDivisions; ++j) {
				final Rectangle rectangle = rectangles[(i * xDivisions) + j];
//				cfs.add(CompletableFuture.supplyAsync(() -> {
				for (int y = rectangle.y; y < (rectangle.y + rectangle.height); ++y) {
					for (int x = rectangle.x; x < (rectangle.x + rectangle.width); ++x) {
						out[x][y] = input[y][x];
					}
				}
//					return Boolean.TRUE;
//				}));
			}
		}
		// CompletableFuture.allOf(cfs.toArray(new
		// CompletableFuture[cfs.size()])).join();
		return out;
	}

	public static int[][] transpose(int[][] input) {
		int height = input.length;
		int width = input[0].length;
		int[][] transposed = new int[width][height];
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				transposed[x][y] = input[y][x];
			}
		}
		return transposed;
	}
}