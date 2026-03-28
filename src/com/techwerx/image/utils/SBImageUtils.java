package com.techwerx.image.utils;

import static net.sourceforge.lept4j.ILeptonica.IFF_TIFF;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.FastMath;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.github.jaiimageio.plugins.tiff.BaselineTIFFTagSet;
import com.github.jaiimageio.plugins.tiff.TIFFDirectory;
import com.github.jaiimageio.plugins.tiff.TIFFField;
import com.github.jaiimageio.plugins.tiff.TIFFTag;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import com.techwerx.image.BBox;
import com.techwerx.image.DeSkewSBImage;
import com.techwerx.image.DimensionScaling;
import com.techwerx.image.SBImage;
import com.techwerx.image.XYDivisions;
import com.techwerx.image.thresholds.OtsuThreshold;
import com.techwerx.pdf.PDFHandlerChartFactory;
import com.techwerx.tesseract.TechWerxTesseract;
import com.techwerx.tesseract.TechWerxTesseractHandle;
import com.techwerx.tesseract.TechWerxTesseractHandleFactory;
import com.techwerx.tesseract.TechWerxTesseractHandlePool;
import com.techwerx.text.BIWrapperForOCR;
import com.techwerx.text.OCRResultBI;
import com.techwerx.text.OCRResultPix;
import com.techwerx.text.ProcessDataWrapper;

import net.sourceforge.lept4j.Box;
import net.sourceforge.lept4j.Boxa;
import net.sourceforge.lept4j.ILeptonica;
import net.sourceforge.lept4j.L_Bmf;
import net.sourceforge.lept4j.L_Dewarp;
import net.sourceforge.lept4j.L_Dewarpa;
import net.sourceforge.lept4j.Leptonica;
import net.sourceforge.lept4j.Leptonica1;
import net.sourceforge.lept4j.Pix;
import net.sourceforge.lept4j.Pixa;
import net.sourceforge.lept4j.Sel;
import net.sourceforge.lept4j.util.LeptUtils;
import net.sourceforge.tess4j.ITessAPI.ETEXT_DESC;
import net.sourceforge.tess4j.ITessAPI.TessBaseAPI;
import net.sourceforge.tess4j.ITessAPI.TessPageIterator;
import net.sourceforge.tess4j.ITessAPI.TessPageSegMode;
import net.sourceforge.tess4j.ITessAPI.TessResultIterator;
import net.sourceforge.tess4j.TessAPI1;
import net.sourceforge.tess4j.Tesseract;

public class SBImageUtils {

	public static final String baseTestDir = "E:\\TechWerx\\Java\\Working\\";

	public static final int idealCharHeight = 21;
	public static final int idealCharWidth = 12;

	public static final double[] positiveETable = new double[300];
	public static final double[] negativeETable = new double[300];
	public static final double[] powerHalf = new double[300];
	public static final double[] power2 = new double[300];
	public static final double[] power3 = new double[300];
	public static final double[] power4 = new double[300];
	public static final double[] power5 = new double[300];
	public static final double[] power6 = new double[300];
	public static final double[] power7 = new double[300];
	public static final double[] power8 = new double[300];
	public static final double[] inverseHalf = new double[300];
	public static final double[] inverse1 = new double[300];
	public static final double[] inverse2 = new double[300];
	public static final double[] inverse3 = new double[300];
	public static final double[] inverse4 = new double[300];
	public static int ANISOTROPIC_DIFFUSION_CONSTANT = 40;

	public static final double scaleDown = 0.7;
	public static final double scaleUp = 1.42;
	public static final int verticalPadding = 96;
	public static final int horizontalPadding = 96;

	public static final int BLACK_PIXEL_REPLACEMENT = 40;
	public static final int GRAY_PIXEL_REPLACEMENT = 232;
	public static final int WHITE_PIXEL_REPLACEMENT = 255;

	public static final int ERODE_DILATE_THICKNESS = 5;

	public static final int SMALL_SIZE = 1;
	public static final int MEDIUM_SIZE = 2;
	public static final int BIG_SIZE = 3;

	// private static final Leptonica lInstance = Leptonica.INSTANCE; // not used.
	// Statement inserted only to ensure that
	// the library is loaded
	// private static final TessAPI tapi = TessAPI.INSTANCE; // not used. Statement
	// inserted only to ensure that the
	// library is loaded

	static {
		positiveETable[0] = 1.0;
		negativeETable[0] = 1.0;
		powerHalf[0] = 1.0;
		power2[0] = 1.0;
		power3[0] = 1.0;
		power4[0] = 1.0;
		power5[0] = 1.0;
		power5[0] = 1.0;
		power7[0] = 1.0;
		power8[0] = 1.0;
		inverseHalf[0] = 1.0;
		inverse1[0] = 1.0;
		inverse2[0] = 1.0;
		inverse3[0] = 1.0;
		inverse4[0] = 1.0;
		for (int i = 1; i < 300; i++) {
			positiveETable[i] = Math.pow(Math.E, i / ANISOTROPIC_DIFFUSION_CONSTANT);
			negativeETable[i] = Math.pow(Math.E, -i / ANISOTROPIC_DIFFUSION_CONSTANT);
			powerHalf[i] = Math.sqrt(i);
			power2[i] = 1.0 * i * i;
			power3[i] = 1.0 * i * i * i;
			power4[i] = 1.0 * i * i * i * i;
			power5[i] = 1.0 * i * i * i * i * i;
			power6[i] = 1.0 * i * i * i * i * i * i;
			power7[i] = 1.0 * i * i * i * i * i * i * i;
			power8[i] = 1.0 * i * i * i * i * i * i * i * i;
			inverseHalf[i] = 1.0 / Math.sqrt(i);
			inverse1[i] = 1.0 / i;
			inverse2[i] = 1.0 / (i * i);
			inverse3[i] = 1.0 / (i * i * i);
			inverse4[i] = 1.0 / (i * i * i * i);
		}
	}

	// public static final TesseractHandlePool tesseractPool = new
	// TesseractHandlePool(new TesseractHandleFactory());

	private static TechWerxTesseractHandlePool techwerxTesseractPool = null;
	// public static TechWerxTesseractHandlePool techwerxTesseractPool = new
	// TechWerxTesseractHandlePool(
	// new TechWerxTesseractHandleFactory(1000),
	// TechWerxTesseractHandlePool.oneMachineConfig, 1000);
	private static boolean tesseractPoolInitialised = false;

	public static final float[][] GAUSSIAN3X = new float[][] { { 0.305163f, 0.389673f, 0.305163f } };
	public static final float[][] GAUSSIAN5X = new float[][] {
			{ 0.113318f, 0.236003f, 0.30136f, 0.236003f, 0.113318f } };
	public static final float[][] GAUSSIAN7X = new float[][] {
			{ 0.031251f, 0.106235f, 0.221252f, 0.282524f, 0.221252f, 0.106235f, 0.031251f } };
	public static final float[][] GAUSSIAN9X = new float[][] {
			{ 0.005563f, 0.030904f, 0.105053f, 0.21879f, 0.27938f, 0.21879f, 0.105053f, 0.030904f, 0.005563f } };
	public static final float[][] GAUSSIAN3Y = new float[][] { { 0.305163f }, { 0.389673f }, { 0.305163f } };
	public static final float[][] GAUSSIAN5Y = new float[][] { { 0.113318f }, { 0.236003f }, { 0.30136f },
			{ 0.236003f }, { 0.113318f } };
	public static final float[][] GAUSSIAN7Y = new float[][] { { 0.031251f }, { 0.106235f }, { 0.221252f },
			{ 0.282524f }, { 0.221252f }, { 0.106235f }, { 0.031251f } };
	public static final float[][] GAUSSIAN9Y = new float[][] { { 0.005563f }, { 0.030904f }, { 0.105053f },
			{ 0.21879f }, { 0.27938f }, { 0.21879f }, { 0.105053f }, { 0.030904f }, { 0.005563f } };

	// public static final ExecutorService ocrThreadService =
	// Executors.newFixedThreadPool(12);

	public synchronized static void initialiseTesseractPool(int debugL) {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ie) {

		}
		if (!tesseractPoolInitialised) {
			techwerxTesseractPool = new TechWerxTesseractHandlePool(new TechWerxTesseractHandleFactory(1, debugL),
					TechWerxTesseractHandlePool.singletonConfig, 1, debugL);
			tesseractPoolInitialised = true;
		}
	}

	public static SBImage parallelGaussian(SBImage input, int kernelSize) throws Exception {
		// int noOfDivisions = input.height / 500;
		int noOfDivisions = 3;
		XYDivisions divisions = new XYDivisions(1, noOfDivisions);
		final SBImage[] subImages = input.createSubImageArray(divisions, kernelSize, kernelSize);
		final SBImage[] results = new SBImage[subImages.length];
		final ExecutorService threadService = Executors.newFixedThreadPool(Math.min(subImages.length, 30));
		final ArrayList<CompletableFuture<Object>> cfs = new ArrayList<>(subImages.length);
		for (int count = 0; count < subImages.length; ++count) {
			final int counter = count;
			cfs.add(CompletableFuture.supplyAsync(() -> {
				results[counter] = gaussian(subImages[counter], kernelSize);
				return Boolean.TRUE;
			}, threadService));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		threadService.shutdown();
		return SBImage.stitchSubImages(results, divisions.xDivisions, divisions.yDivisions, kernelSize, kernelSize);
	}

	public static SBImage gaussian(SBImage input, int kernelSize) {
		float[][] kernel = null;
		switch (kernelSize) {
		case 3:
			kernel = GAUSSIAN3X;
			break;
		case 5:
			kernel = GAUSSIAN5X;
			break;
		case 7:
			kernel = GAUSSIAN7X;
			break;
		case 9:
			kernel = GAUSSIAN9X;
			break;
		default:
			kernel = GAUSSIAN5X;
		}
		int[][] temp = new int[input.height][input.width];
		try {
			SBImageArrayUtils.setBorderValues(input.pixels, temp, 1, kernelSize);
		} catch (Exception e) {
		}
		for (int y = kernel.length / 2; y < (input.height - (kernel.length / 2)); y++) {
			for (int x = kernel[0].length / 2; x < (input.width - (kernel[0].length / 2)); x++) {
				float sum = 0.0f;
				for (int dy = -kernel.length / 2; dy <= (kernel.length / 2); dy++) {
					for (int dx = -kernel[0].length / 2; dx <= (kernel[0].length / 2); dx++) {
						sum += input.pixels[y + dy][x + dx]
								* kernel[dy + (kernel.length / 2)][dx + (kernel[0].length / 2)];
					}
				}
				temp[y][x] = (int) sum;
			}
		}

		switch (kernelSize) {
		case 3:
			kernel = GAUSSIAN3Y;
			break;
		case 5:
			kernel = GAUSSIAN5Y;
			break;
		case 7:
			kernel = GAUSSIAN7Y;
			break;
		case 9:
			kernel = GAUSSIAN9Y;
			break;
		default:
			kernel = GAUSSIAN5Y;
		}
		int[][] out = new int[input.height][input.width];
		try {
			SBImageArrayUtils.setBorderValues(temp, out, kernelSize, 1);
		} catch (Exception e) {
		}
		for (int y = kernel.length / 2; y < (input.height - (kernel.length / 2)); y++) {
			for (int x = kernel[0].length / 2; x < (input.width - (kernel[0].length / 2)); x++) {
				float sum = 0.0f;
				for (int dy = -kernel.length / 2; dy <= (kernel.length / 2); dy++) {
					for (int dx = -kernel[0].length / 2; dx <= (kernel[0].length / 2); dx++) {
						sum += temp[y + dy][x + dx] * kernel[dy + (kernel.length / 2)][dx + (kernel[0].length / 2)];
					}
				}
				out[y][x] = (int) sum;
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage L1Normalize(SBImage image, int kernelSize) {
		double[][] temp = new double[image.height][image.width];
		setBorderValues(null, temp, 1.0 / (kernelSize * kernelSize), kernelSize, kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				double sum = 0.0f;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						sum += (image.pixels[y + dy][x + dx] + 1);
					}
				}
				final double interim = ((image.pixels[y][x] + 1) * 1.0) / sum;
				temp[y][x] = interim * interim * interim * interim;
			}
		}
		NumberTriplet dt = NumberTriplet.findMinMax(temp, kernelSize, kernelSize);
		double min = dt.first;
		double max = Math.min(dt.second, dt.third);
		System.out.println("Min = " + min + " ; Max = " + max);
		int[][] out = new int[image.height][image.width];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				final double newPixel = (((temp[y][x] - min) * 255.0f) / (max - min));
				out[y][x] = Math.min(Math.max(0, (int) newPixel), 255);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;

	}

	public static SBImage L1WeightedNormalize(SBImage image, int kernelSize) {
		double[][] temp = new double[image.height][image.width];
		setBorderValues(null, temp, 1.0 / (kernelSize * kernelSize), kernelSize, kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				double numerator = 0.0;
				double denominator = 0.0;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						final int where = Math.max(Math.abs(dy), Math.abs(dx));
						final int weight = ((kernelSize / 2) + 1) - where;
						numerator += weight;
						denominator += weight * (image.pixels[y + dy][x + dx] + 1);
					}
				}
				final double interim = (numerator * (image.pixels[y][x] + 1) * 1.0) / denominator;
				temp[y][x] = interim * interim * interim * interim;
			}
		}
		NumberTriplet dt = NumberTriplet.findMinMax(temp, kernelSize, kernelSize);
		double min = dt.first;
		double max = Math.min(dt.second, dt.third);
		System.out.println("Min = " + min + " ; Max = " + max);
		int[][] out = new int[image.height][image.width];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				final double newPixel = (((temp[y][x] - min) * 255.0) / (max - min));
				out[y][x] = Math.min(Math.max(0, (int) newPixel), 255);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;

	}

	public static SBImage L1SquareRoot(SBImage image, int kernelSize) {
		/*
		 * float[][] temp1 = new float[image.height][image.width]; setBorderValues(null,
		 * temp1, 1.0f / (kernelSize * kernelSize), kernelSize, kernelSize); for (int y
		 * = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) { for (int x =
		 * kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) { float sum =
		 * 0.0f; for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) { for (int
		 * dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) { sum += image.pixels[y +
		 * dy][x + dx]; } } temp1[y][x] = (image.pixels[y][x] * 1.0f) / sum; } }
		 * float[][] temp2 = new float[image.height][image.width]; setBorderValues(null,
		 * temp2, 1.0f / kernelSize, kernelSize, kernelSize); for (int y = kernelSize /
		 * 2; y < (image.height - (kernelSize / 2)); y++) { for (int x = kernelSize / 2;
		 * x < (image.width - (kernelSize / 2)); x++) { float sum = 0.0f; for (int dy =
		 * -kernelSize / 2; dy <= (kernelSize / 2); dy++) { for (int dx = -kernelSize /
		 * 2; dx <= (kernelSize / 2); dx++) { sum += temp1[y + dy][x + dx]; } }
		 * temp2[y][x] = (float) Math.sqrt((temp1[y][x] * 1.0f) / sum); } }
		 */
		double[][] temp2 = new double[image.height][image.width];
		setBorderValues(null, temp2, 1.0 / (kernelSize * kernelSize * kernelSize * kernelSize), kernelSize, kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				double sum = 0.0f;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						sum += inverseHalf[image.pixels[y + dy][x + dx]];
					}
				}
				final double interim = ((image.pixels[y][x] + 1) * 1.0) / (1.0 * sum * sum);
				temp2[y][x] = interim * interim * interim * interim;
				// if (((y == 2269) || (y == 2270) || (y == 2272) || (y == 2306))
				// && ((x == 2661) || (x == 2662) || (x == 2790) || (x == 445) || (x == 1570)))
				// {
				// System.out.println("Value at [" + y + "," + x + "] = " + temp2[y][x] + " ;
				// sum = " + sum
				// + "; pixel = " + image.pixels[y][x]);
				// }
			}
		}
		// System.out.println(Arrays.deepToString(temp2));

		NumberTriplet dt = NumberTriplet.findMinMax(temp2, kernelSize, kernelSize);

		double min = dt.first;
		double max = Math.min(dt.second, dt.third);
		System.out.println("Min = " + min + " ; Max = " + max);
		int[][] out = new int[image.height][image.width];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				final double newPixel = (((temp2[y][x] - min) * 255.0) / (max - min));
				out[y][x] = Math.min(Math.max(0, (int) newPixel), 255);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage harmonicFilter(SBImage image, int kernelSize) {
		int[][] out = new int[image.height][image.width];
		setBorderValues(null, out, 127, kernelSize, kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				float numerator = 0.0f;
				float denominator = 0.0f;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						final int where = Math.max(Math.abs(dy), Math.abs(dx));
						final int weight = ((kernelSize / 2) + 1) - where;
						numerator += weight;
						denominator += (weight * 1.0f) * inverse1[image.pixels[y + dy][x + dx]];
					}
				}
				out[y][x] = (int) (numerator / denominator) - 1;
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage lehmerMinus1Filter(SBImage image, int kernelSize) {
		int[][] out = new int[image.height][image.width];
		setBorderValues(null, out, 127, kernelSize, kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				float numerator = 0.0f;
				float denominator = 0.0f;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						final int where = Math.max(Math.abs(dy), Math.abs(dx));
						final int weight = ((kernelSize / 2) + 1) - where;
						numerator += (weight * 1.0f) * inverse1[image.pixels[y + dy][x + dx]];
						denominator += (weight * 1.0f) * inverse2[image.pixels[y + dy][x + dx]];
					}
				}
				out[y][x] = (int) (((numerator * 1.0f) / denominator) - 1);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage lehmerMinus2Filter(SBImage image, int kernelSize) {
		int[][] out = new int[image.height][image.width];
		setBorderValues(null, out, 127, kernelSize, kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				float numerator = 0.0f;
				float denominator = 0.0f;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						final int where = Math.max(Math.abs(dy), Math.abs(dx));
						final int weight = ((kernelSize / 2) + 1) - where;
						numerator += (weight * 1.0f) * inverse2[image.pixels[y + dy][x + dx]];
						denominator += (weight * 1.0f) * inverse3[image.pixels[y + dy][x + dx]];
					}
				}
				out[y][x] = (int) (((numerator * 1.0f) / denominator) - 1);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static void setBorderValues(SBImage image, float[][] input, float value, int kernelHeight, int kernelWidth) {
		// System.out.println("Setting Border Value to : " + value);
		// set top border
		for (int y = 0; y < (kernelHeight / 2); y++) {
			for (int x = 0; x < input[0].length; x++) {
				if (value != Float.MIN_VALUE) {
					input[y][x] = value;
				} else {
					input[y][x] = image.pixels[y][x];
				}
			}
		}
		// set bottom border
		for (int y = input.length - 1; y > (input.length - 1 - (kernelHeight / 2)); y--) {
			for (int x = 0; x < input[0].length; x++) {
				if (value != Float.MIN_VALUE) {
					input[y][x] = value;
				} else {
					input[y][x] = image.pixels[y][x];
				}

			}
		}
		// set left border
		for (int y = 0; y < input.length; y++) {
			for (int x = 0; x < (kernelWidth / 2); x++) {
				if (value != Float.MIN_VALUE) {
					input[y][x] = value;
				} else {
					input[y][x] = image.pixels[y][x];
				}
			}
		}
		// set right border
		for (int y = 0; y < input.length; y++) {
			for (int x = input[0].length - 1; x > (input[0].length - 1 - (kernelWidth / 2)); x--) {
				if (value != Float.MIN_VALUE) {
					input[y][x] = value;
				} else {
					input[y][x] = image.pixels[y][x];
				}
			}
		}
	}

	public static void setBorderValues(SBImage image, double[][] input, double value, int kernelHeight,
			int kernelWidth) {
		// System.out.println("Setting Border Value to : " + value);
		// set top border
		for (int y = 0; y < (kernelHeight / 2); y++) {
			for (int x = 0; x < input[0].length; x++) {
				if (value != Double.MIN_VALUE) {
					input[y][x] = value;
				} else {
					input[y][x] = image.pixels[y][x];
				}
			}
		}
		// set bottom border
		for (int y = input.length - 1; y > (input.length - 1 - (kernelHeight / 2)); y--) {
			for (int x = 0; x < input[0].length; x++) {
				if (value != Double.MIN_VALUE) {
					input[y][x] = value;
				} else {
					input[y][x] = image.pixels[y][x];
				}

			}
		}
		// set left border
		for (int y = 0; y < input.length; y++) {
			for (int x = 0; x < (kernelWidth / 2); x++) {
				if (value != Double.MIN_VALUE) {
					input[y][x] = value;
				} else {
					input[y][x] = image.pixels[y][x];
				}
			}
		}
		// set right border
		for (int y = 0; y < input.length; y++) {
			for (int x = input[0].length - 1; x > (input[0].length - 1 - (kernelWidth / 2)); x--) {
				if (value != Double.MIN_VALUE) {
					input[y][x] = value;
				} else {
					input[y][x] = image.pixels[y][x];
				}
			}
		}
	}

	public static void setBorderValues(SBImage image, int[][] input, int value, int kernelHeight, int kernelWidth) {
		// set top border
		for (int y = 0; y < (kernelHeight / 2); y++) {
			for (int x = 0; x < input[0].length; x++) {
				if (value != Integer.MIN_VALUE) {
					input[y][x] = value;
				} else {
					input[y][x] = image.pixels[y][x];
				}
			}
		}
		// set bottom border
		for (int y = input.length - 1; y > (input.length - 1 - (kernelHeight / 2)); y--) {
			for (int x = 0; x < input[0].length; x++) {
				if (value != Integer.MIN_VALUE) {
					input[y][x] = value;
				} else {
					input[y][x] = image.pixels[y][x];
				}

			}
		}
		// set left border
		for (int y = 0; y < input.length; y++) {
			for (int x = 0; x < (kernelWidth / 2); x++) {
				if (value != Integer.MIN_VALUE) {
					input[y][x] = value;
				} else {
					input[y][x] = image.pixels[y][x];
				}

			}
		}
		// set right border
		for (int y = 0; y < input.length; y++) {
			for (int x = input[0].length - 1; x > (input[0].length - 1 - (kernelWidth / 2)); x--) {
				if (value != Integer.MIN_VALUE) {
					input[y][x] = value;
				} else {
					input[y][x] = image.pixels[y][x];
				}
			}
		}
	}

	public static ArrayList<Integer> getHistogram(SBImage image) {
		ArrayList<Integer> histogram = new ArrayList<>(256);
		for (int i = 0; i < 256; i++) {
			histogram.add(0);
		}
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				int oldValue = histogram.get(image.pixels[y][x]);
				// System.out.println("About to set value at index " + image.pixels[y][x] + " as
				// " + oldValue + 1);
				histogram.set(image.pixels[y][x], oldValue + 1);
			}
		}
		return histogram;
	}

	public static void drawHistogram(SBImage image, String fileName) throws Exception {
		drawHistogram(image, fileName, false);
	}

	public static void drawHistogram(SBImage image, String fileName, boolean ignore0And255) throws Exception {
		int[] histogram = new int[256];
		int[] cdf = new int[256];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				histogram[image.pixels[y][x]]++;
			}
		}
		if (ignore0And255) {
			histogram[0] = 0;
			histogram[255] = 0;
		}
		cdf[0] = histogram[0];
		for (int i = 1; i < 256; i++) {
			cdf[i] = cdf[i - 1] + histogram[i];
		}

		PDFHandlerChartFactory.drawBarChart(histogram, 20, "E:\\TechWerx\\Java\\Working\\" + fileName + "-histData.png",
				3500, 1000, "Histogram plot", "Histogram plot", "Pixel values", "No Of Occurences");
		PDFHandlerChartFactory.drawBarChart(cdf, 20, "E:\\TechWerx\\Java\\Working\\" + fileName + "-cdfData.png", 3500,
				1000, "Histogram plot", "Histogram plot", "Pixel values", "No Of Occurences");

	}

	public static void drawHistogram4(SBImage image, String fileName, boolean ignore0And255) throws Exception {
		int bucketSize = 4;
		int[] histogram = new int[256 / bucketSize];
		int[] cdf = new int[256 / bucketSize];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				histogram[image.pixels[y][x] / bucketSize]++;
			}
		}
		if (ignore0And255) {
			histogram[0] = 0;
			histogram[255 / bucketSize] = 0;
		}
		cdf[0] = histogram[0];
		for (int i = 1; i < (256 / bucketSize); i++) {
			cdf[i] = cdf[i - 1] + histogram[i];
		}

		PDFHandlerChartFactory.drawBarChart(histogram, 20, "E:\\TechWerx\\Java\\Working\\" + fileName + "-histData.png",
				3500, 1000, "Histogram plot", "Histogram plot", "Pixel values", "No Of Occurences");
		PDFHandlerChartFactory.drawBarChart(cdf, 20, "E:\\TechWerx\\Java\\Working\\" + fileName + "-cdfData.png", 3500,
				1000, "Histogram plot", "Histogram plot", "Pixel values", "No Of Occurences");

	}

	public static SBImage[] bagchiTrinarization(SBImage image) throws Exception {
		double stdMultiplier;
		SBImage[] subImages = image.createSubImageArray();
		// int i = 0;
		for (SBImage subImage : subImages) {
			SBImage temp = SBImageUtils.histEq_Scaling(subImage);
			// double mean = subImage.getMeanAndSD().first;
			// double stdDev = subImage.getMeanAndSD().second;
			double mean = temp.getMeanAndSD().first;
			double stdDev = temp.getMeanAndSD().second;
			double lowerCutOff;
			int y = 0;
			// for (int[] row : subImage.pixels) {
			for (int[] row : temp.pixels) {
				int x = 0;
				for (int pixel : row) {
					if (mean < 208) {
						stdMultiplier = 2.5;
						lowerCutOff = (int) (mean - (stdDev * stdMultiplier));
						if (pixel < lowerCutOff) {
							subImage.pixels[y][x] = 64;
						} else {
							if (pixel < mean) {
								subImage.pixels[y][x] = 208;
							} else {
								subImage.pixels[y][x] = 255;
							}
						}
					} else {
						stdMultiplier = 1.0;
						lowerCutOff = (int) (mean - (stdDev * stdMultiplier));
						if (pixel < lowerCutOff) {
							subImage.pixels[y][x] = 64;
						} else {
							subImage.pixels[y][x] = 255;
						}
					}
					++x;
				}
				++y;
			}
			// subImages[i++] = SauvolaThreshold.processImage(subImage);
		}
		return subImages;
	}

	public static SBImage[] bagchiTrinarizationForPK(SBImage image) throws Exception {
		return bagchiTrinarizationForPK(image, image.xDivisions, image.yDivisions);
	}

	public static SBImage[] bagchiTrinarizationForPK(SBImage image, int xDivisions, int yDivisions) throws Exception {
		XYDivisions divs = new XYDivisions(xDivisions, yDivisions);
		SBImage[] subImages = image.createSubImageArray(divs);
		int subImageCount = 0;
		// int imageCount = 1;
		for (SBImage subImage : subImages) {
			int total = subImage.width * subImage.height;
			int bucketSize = 4;
			int[] histogram = new int[256 / bucketSize];
			int[] cdf = new int[256 / bucketSize];
			for (int y = 0; y < subImage.height; y++) {
				for (int x = 0; x < subImage.width; x++) {
					histogram[subImage.pixels[y][x] / bucketSize]++;
				}
			}
			histogram[0] = 0;
			histogram[255 / bucketSize] = 0;
			cdf[0] = histogram[0];
			double percentCutoff = 0.05;
			for (int i = 1; i < (256 / bucketSize); i++) {
				if (histogram[i] > ((total * percentCutoff * 1.0) / 100)) {
					cdf[i] = cdf[i - 1] + histogram[i];
				} else {
					cdf[i] = cdf[i - 1];
				}
			}
			int bucketDelta = 3;
			int cutoffBucket = 0;
			for (int i = bucketDelta - 1; i < ((256 / bucketSize) - (bucketDelta - 1)); ++i) {
				cutoffBucket = i;
				if ((cdf[i] == 0) || (cdf[i - 1] == 0) || (i < 10)) {
					continue;
				}
				double leftSideSlope = cdf[i] - cdf[i - 2];
				double rightSideSlope = cdf[i + 2] - cdf[i];
				if (rightSideSlope > (3.0 * leftSideSlope)) {
					break;
				}
			}
			int originalleftCutoff = ((cutoffBucket - 1) * bucketSize) + (bucketSize - 1);
			int originalrightCutoff = ((cutoffBucket + 2) * bucketSize) + (bucketSize - 1);

			final double mean = subImage.getMeanAndSD().first;
			final double sigma = subImage.getMeanAndSD().second;
			int originalleftCutoffND;
			int originalrightCutoffND;
			if ((mean / sigma) >= 10) {
				originalleftCutoffND = (int) (mean - (2.75 * sigma));
				originalrightCutoffND = (int) (mean - (2 * sigma));
				// leftCutoff = (int) (mean - (2.5 * sigma));
				// rightCutoff = (int) (mean - (1.5 * sigma));
			} else {
				if ((mean / sigma) >= 5) {
					originalleftCutoffND = (int) (mean - 25);
					// leftCutoff = (int) (mean - 20);
					originalrightCutoffND = (int) (mean - 10);
				} else {
					originalleftCutoffND = (int) (mean - 35);
					// rightCutoff = (int) (mean - 25);
					originalrightCutoffND = (int) (mean - 10);
				}
			}

			int newSum = 0;
			int loopNo = 0;
			int leftCutoff;
			int rightCutoff;
			int leftCutoffND;
			int rightCutoffND;
			int[][] out = new int[subImage.height][subImage.width];
			do {
				newSum = 0;
				// leftCutoff = originalleftCutoff - (5 * loopNo);
				leftCutoff = originalleftCutoff - (3 * loopNo);
				// rightCutoff = originalrightCutoff - (int) (4.5 * loopNo);
				rightCutoff = originalrightCutoff - (2 * loopNo);
				// leftCutoffND = originalleftCutoffND - (5 * loopNo);
				leftCutoffND = originalleftCutoffND - (3 * loopNo);
				// rightCutoffND = originalrightCutoffND - (int) (4.5 * loopNo);
				rightCutoffND = originalrightCutoffND - (2 * loopNo);
				int y = 0;
				// System.out.println("Entering loop no = " + loopNo + " with leftCutoff = " +
				// leftCutoff
				// + "; rightCutoff = " + rightCutoff);
				for (int[] row : subImage.pixels) {
					int x = 0;
					for (int pixel : row) {
						// if (pixel >= 180) {
						if (pixel >= 170) {
							leftCutoff = leftCutoffND;
							rightCutoff = rightCutoffND;
							// if (loopNo > 0) {
							// System.out.println("Changed Cutoffs to CutoffNDs");
							// }
						}

						if (pixel < leftCutoff) {
							out[y][x] = BLACK_PIXEL_REPLACEMENT;
						} else {
							if (pixel < rightCutoff) {
								// subImage.pixels[y][x] = 242;
								out[y][x] = GRAY_PIXEL_REPLACEMENT;
							} else {
								out[y][x] = WHITE_PIXEL_REPLACEMENT;
							}
						}
						newSum += out[y][x];
						++x;
					}
					++y;
				}
				// System.out.println("Mean = " + ((newSum * 1.0) / (subImage.width *
				// subImage.height)));
				++loopNo;

			} while ((((newSum * 1.0) / (subImage.width * subImage.height)) <= 208) // Earlier, 144. Experiment with 160
																					// as well
					&& (leftCutoff > ((mean * 1.0) / 3)));
			// System.out.println("Image : " + imageCount++ + " - Leftcutoff: " + leftCutoff
			// + "; Rightcutoff: "
			// + rightCutoff + "; Mean : " + ((newSum * 1.0) / (subImage.width *
			// subImage.height)));
			subImages[subImageCount++] = new SBImage(out);
		}
		return subImages;
	}

	// to be used on the last leg after the line pix's have been clipped from the
	// original pix. Not used now. Current implementation is based on OtsuThreshold
	public static SBImage bagchiBinarizationForPK1(SBImage image, int xDivisions, int yDivisions, int imageNumber,
			int debugL) throws Exception {
		XYDivisions divs = new XYDivisions(xDivisions, yDivisions);
		SBImage[] subImages = image.createSubImageArray(divs);
		int subImageCount = 0;
		for (SBImage subImage : subImages) {
			System.out.println("Starting processing of subimage : " + ((imageNumber * 100) + subImageCount));

			int total = subImage.width * subImage.height;
			int bucketSize = 4;
			int[] histogram = new int[256 / bucketSize];
			int[] cdf = new int[256 / bucketSize];
			for (int y = 0; y < subImage.height; y++) {
				for (int x = 0; x < subImage.width; x++) {
					histogram[subImage.pixels[y][x] / bucketSize]++;
				}
			}
			histogram[0] = 0;
			histogram[255 / bucketSize] = 0;
			cdf[0] = histogram[0];
			double percentCutoff = 0.025; // changed from 0.05 in the main method
			int skipped = 0;
			for (int i = 1; i < (256 / bucketSize); i++) {
				if (histogram[i] > ((total * percentCutoff * 1.0) / 100)) {
					cdf[i] = cdf[i - 1] + histogram[i];
					skipped = 0;
				} else {
					if (skipped == 0) {
						cdf[i] = cdf[i - 1];
						++skipped;
						continue;
					} else {
						int subtotal = 0;
						for (int j = skipped; j >= 0; --j) {
							subtotal += histogram[i - j];
						}
						if (subtotal > ((total * percentCutoff * 1.0) / 100)) {
							cdf[i] = cdf[i - 1] + subtotal;
							skipped = 0;
						} else {
							cdf[i] = cdf[i - 1];
							++skipped;
						}
					}
				}
			}
			if (debugL <= 3) {
				PDFHandlerChartFactory.drawBarChart(histogram, 20,
						SBImageUtils.baseTestDir + "histData-" + ((imageNumber * 100) + subImageCount) + ".png", 3500,
						1000, "Histogram plot", "Histogram plot", "Pixel values", "No Of Occurences");
			}
			if (debugL <= 3) {
				PDFHandlerChartFactory.drawBarChart(cdf, 20,
						SBImageUtils.baseTestDir + "cdfData-" + ((imageNumber * 100) + subImageCount) + ".png", 3500,
						1000, "Histogram plot", "Histogram plot", "Pixel values", "No Of Occurences");
			}

			int originalleftCutoff = 245;

			final double mean = subImage.getMeanAndSD().first;
			final double sigma = subImage.getMeanAndSD().second;
			if (debugL <= 4) {
				System.out.println("Original mean and sigma are : " + mean + " , " + sigma);
			}
			int newSum = 0;
			int loopNo = 0;
			int leftCutoff;
			int[][] out = new int[subImage.height][subImage.width];
			do {
				newSum = 0;
				leftCutoff = originalleftCutoff - (3 * loopNo);
				int y = 0;
				for (int[] row : subImage.pixels) {
					int x = 0;
					for (int pixel : row) {
						if (pixel < leftCutoff) {
							out[y][x] = BLACK_PIXEL_REPLACEMENT;
						} else {
							out[y][x] = WHITE_PIXEL_REPLACEMENT;
						}
						newSum += out[y][x];
						++x;
					}
					++y;
				}
				if (debugL <= 3) {
					System.out.println("LoopNo : " + loopNo + " - Mean = "
							+ ((newSum * 1.0) / (subImage.width * subImage.height)) + " ; leftCutoff = " + leftCutoff);
				}
				++loopNo;
			} // Earlier, 208. Reduced now, as white pixels lesser.
				// 1/3 * 40 + 2 / 3 * 255 = 183 (25%); 0.25 * 40 + 0.75 * 255 = 201 (40%)
				// 0.2 * 40 + 0.8 * 255 = 212 (20%); 0.16 * 40 + 0.84 * 255 = 220 (15%)
				// Weighted average of 183, 201, 212, 220 is 201
			while ((((newSum * 1.0) / (subImage.width * subImage.height)) <= 201) && (leftCutoff > ((mean * 1.0) / 3)));
			subImages[subImageCount++] = new SBImage(out);
		}
		return SBImage.stitchSubImages(subImages, xDivisions, yDivisions);
	}

	// to be used on the last leg after the line pix's have been clipped from the
	// original pix
	public static SBImage bagchiBinarizationForPK2(SBImage image, int xDivisions, int yDivisions, int imageNumber,
			int debugL) throws Exception {
		if (debugL <= 3) {
			System.out.println("Starting processing of SBImage - " + imageNumber);
		}
		return OtsuThreshold.processSubImages1(image, xDivisions, yDivisions, imageNumber, debugL);
	}

	public static SBImage[] bagchiTrinarizationForPKAlternate(SBImage image, int xDivisions, int yDivisions)
			throws Exception {
		XYDivisions divs = new XYDivisions(xDivisions, yDivisions);
		SBImage[] subImages = image.createSubImageArray(divs);
		int subImageCount = 0;
		// int p = 0;
		// int q = 0;
		// int i = 0;
		for (SBImage subImage : subImages) {
			// int total = subImage.width * subImage.height;
			final double mean = subImage.getMeanAndSD().first;
			final double sigma = subImage.getMeanAndSD().second;
			double multiplier = 1.0;
			if (mean > 10) {
				if ((sigma / mean) > 0.5) {
					multiplier = 0.33;
				} else {
					if ((sigma / mean) > 0.4) {
						multiplier = 0.4;
					} else {
						if ((sigma / mean) > 0.3) {
							multiplier = 0.5;
						} else {
							if ((sigma / mean) > 0.2) {
								multiplier = 0.6;
							} else {
								if ((sigma / mean) > 0.1) {
									multiplier = 0.8;
								}
							}
						}
					}
				}
			}
			if (mean > 232) {
				multiplier = 0.5;
			}
			// System.out.println(i + " - Mean : " + mean + "; Sigma : " + sigma);

			int newSum = 0;
			int loopNo = 0;

			int leftCutoff = Math.max(0, (int) (mean - (multiplier * sigma)));
			int rightCutoff = Math.min(255, (int) (mean - (Math.max(0.2, multiplier - 0.8) * sigma)));
			if (sigma < 15) {
				if (mean > 236) {
					leftCutoff = (int) (mean - (9 * sigma));
					rightCutoff = Math.min(255, (int) (mean - (3 * sigma)));
				} else {
					leftCutoff = Math.max(0, (int) (mean - (3 * sigma)));
					rightCutoff = Math.min(255, (int) (mean - (1 * sigma)));
				}
			}
			// else {
			// if (mean < 120) {
			// if ((sigma / mean) < 0.35) {
			// leftCutoff = Math.max(0, (int) (mean - (2 * sigma)));
			// rightCutoff = Math.min(255, (int) (mean - (1 * sigma)));
			// }
			// }
			// }
			int[][] out = new int[subImage.height][subImage.width];
			do {
				newSum = 0;
				leftCutoff = Math.max(0, leftCutoff - (int) (0.25 * loopNo));
				rightCutoff = Math.max(leftCutoff, rightCutoff - (1 * loopNo));
				int y = 0;
				for (int[] row : subImage.pixels) {
					int x = 0;
					for (int pixel : row) {
						if (pixel >= rightCutoff) {
							out[y][x] = WHITE_PIXEL_REPLACEMENT;
						} else {
							if (pixel < leftCutoff) {
								out[y][x] = BLACK_PIXEL_REPLACEMENT;
							} else {
								out[y][x] = GRAY_PIXEL_REPLACEMENT;
							}
						}
						newSum += out[y][x];
						++x;
					}
					++y;
				}
				++loopNo;
			} while ((((newSum * 1.0) / (subImage.width * subImage.height)) <= 248)
					&& (rightCutoff > (leftCutoff + 3)));
			// System.out.println("Looped in image - " + i + " " + loopNo + " times");
			// i++;

			// ImageUtils.writeFile(new SBImage(out), "png", "E:\\TechWerx\\Java\\Working\\"
			// + "AA" + p++ + ".png", 300);

			/*
			 * if (sigma < 20) { rightCutoff = Math.min((int) (leftCutoff + (4 * sigma)),
			 * 250); } else { if (sigma < 40) { rightCutoff = Math.min((int) (leftCutoff +
			 * (3 * sigma)), 250); } else { rightCutoff = Math.min((int) (leftCutoff + (2 *
			 * sigma)), 250); } }
			 */

			/*
			 * loopNo = 0; int[][] outFinal = new int[subImage.height][subImage.width]; do {
			 * newSum = 0; // leftCutoff = (int) (leftCutoff - (0.05 * loopNo)); rightCutoff
			 * = rightCutoff - (1 * loopNo); int y = 0; for (int[] row : subImage.pixels) {
			 * int x = 0; for (int pixel : row) { if (pixel >= rightCutoff) { outFinal[y][x]
			 * = WHITE_PIXEL_REPLACEMENT; } else { if (pixel < leftCutoff) { outFinal[y][x]
			 * = BLACK_PIXEL_REPLACEMENT; } else { outFinal[y][x] = GRAY_PIXEL_REPLACEMENT;
			 * } } newSum += outFinal[y][x]; ++x; } ++y; } ++loopNo; } while ((((newSum *
			 * 1.0) / (subImage.width * subImage.height)) <= 252) && (rightCutoff >
			 * (leftCutoff + 3)));
			 */

			// ImageUtils.writeFile(new SBImage(outFinal), "png",
			// "E:\\TechWerx\\Java\\Working\\" + "CC" + q++ + ".png",
			// 300);
			// subImages[subImageCount++] = new SBImage(outFinal);
			// SBImageUtils.makeOnePixelBorder(out);
			subImages[subImageCount++] = new SBImage(out);
		}
		return subImages;
	}

	public static SBImage[] bagchiTrinarizationForPKInitial(SBImage image, int xDivisions, int yDivisions)
			throws Exception {
		XYDivisions divs = new XYDivisions(xDivisions, yDivisions);
		SBImage[] subImages = image.createSubImageArray(divs);
		int subImageCount = 0;
		// int imageCount = 1;
		for (SBImage subImage : subImages) {
			int total = subImage.width * subImage.height;
			int bucketSize = 4;
			int[] histogram = new int[256 / bucketSize];
			int[] cdf = new int[256 / bucketSize];
			for (int y = 0; y < subImage.height; y++) {
				for (int x = 0; x < subImage.width; x++) {
					histogram[subImage.pixels[y][x] / bucketSize]++;
				}
			}
			histogram[0] = 0;
			histogram[255 / bucketSize] = 0;
			cdf[0] = histogram[0];
			double percentCutoff = 0.05;
			for (int i = 1; i < (256 / bucketSize); i++) {
				if (histogram[i] > ((total * percentCutoff * 1.0) / 100)) {
					cdf[i] = cdf[i - 1] + histogram[i];
				} else {
					cdf[i] = cdf[i - 1];
				}
			}
			int bucketDelta = 3;
			int cutoffBucket = 0;
			for (int i = bucketDelta - 1; i < ((256 / bucketSize) - (bucketDelta - 1)); ++i) {
				cutoffBucket = i;
				if ((cdf[i] == 0) || (cdf[i - 1] == 0) || (i < 10)) {
					continue;
				}
				double leftSideSlope = cdf[i] - cdf[i - 2];
				double rightSideSlope = cdf[i + 2] - cdf[i];
				if (rightSideSlope > (3.0 * leftSideSlope)) {
					break;
				}
			}
			int originalleftCutoff = ((cutoffBucket - 1) * bucketSize) + (bucketSize - 1);
			int originalrightCutoff = ((cutoffBucket + 2) * bucketSize) + (bucketSize - 1);

			final double mean = subImage.getMeanAndSD().first;
			final double sigma = subImage.getMeanAndSD().second;
			int originalleftCutoffND;
			int originalrightCutoffND;
			if ((mean / sigma) >= 10) {
				originalleftCutoffND = (int) (mean - (2.75 * sigma));
				originalrightCutoffND = (int) (mean - (2 * sigma));
				// leftCutoff = (int) (mean - (2.5 * sigma));
				// rightCutoff = (int) (mean - (1.5 * sigma));
			} else {
				if ((mean / sigma) >= 5) {
					originalleftCutoffND = (int) (mean - 25);
					// leftCutoff = (int) (mean - 20);
					originalrightCutoffND = (int) (mean - 10);
				} else {
					originalleftCutoffND = (int) (mean - 35);
					// rightCutoff = (int) (mean - 25);
					originalrightCutoffND = (int) (mean - 10);
				}
			}

			int newSum = 0;
			int loopNo = 0;
			int leftCutoff;
			int rightCutoff;
			int leftCutoffND;
			int rightCutoffND;
			int[][] out = new int[subImage.height][subImage.width];
			do {
				newSum = 0;
				// leftCutoff = originalleftCutoff - (5 * loopNo);
				leftCutoff = originalleftCutoff - (3 * loopNo);
				// rightCutoff = originalrightCutoff - (int) (4.5 * loopNo);
				rightCutoff = originalrightCutoff - (2 * loopNo);
				// leftCutoffND = originalleftCutoffND - (5 * loopNo);
				leftCutoffND = originalleftCutoffND - (3 * loopNo);
				// rightCutoffND = originalrightCutoffND - (int) (4.5 * loopNo);
				rightCutoffND = originalrightCutoffND - (2 * loopNo);
				int y = 0;
				// System.out.println("Entering loop no = " + loopNo + " with leftCutoff = " +
				// leftCutoff
				// + "; rightCutoff = " + rightCutoff);
				for (int[] row : subImage.pixels) {
					int x = 0;
					for (int pixel : row) {
						// if (pixel >= 180) {
						if (pixel >= 170) {
							leftCutoff = leftCutoffND;
							rightCutoff = rightCutoffND;
							// if (loopNo > 0) {
							// System.out.println("Changed Cutoffs to CutoffNDs");
							// }
						}

						if (pixel < leftCutoff) {
							out[y][x] = BLACK_PIXEL_REPLACEMENT;
						} else {
							if (pixel < rightCutoff) {
								// subImage.pixels[y][x] = 242;
								out[y][x] = GRAY_PIXEL_REPLACEMENT;
							} else {
								out[y][x] = WHITE_PIXEL_REPLACEMENT;
							}
						}
						newSum += out[y][x];
						++x;
					}
					++y;
				}
				// System.out.println("Mean = " + ((newSum * 1.0) / (subImage.width *
				// subImage.height)));
				++loopNo;

			} while ((((newSum * 1.0) / (subImage.width * subImage.height)) <= 160) // Was 204. Changed to ensure light
																					// words
																					// are shortlisted
																					// Tried 220 - it creates problems
																					// of letters on same line getting
																					// broken, leading to overlapping
																					// boxes. Such overlapping boxes
																					// might lead to elimination of the
																					// line.
																					// Earlier, 208. Experiment with 160
																					// as well
					&& (leftCutoff > ((mean * 1.0) / 3)));
			// System.out.println("Image : " + imageCount++ + " - Leftcutoff: " + leftCutoff
			// + "; Rightcutoff: "
			// + rightCutoff + "; Mean : " + ((newSum * 1.0) / (subImage.width *
			// subImage.height)));
			subImages[subImageCount++] = new SBImage(out);
		}
		return subImages;
	}

	public static SBImage[] bagchiTrinarizationGeneral(SBImage image, int xDivisions, int yDivisions) throws Exception {
		XYDivisions divs = new XYDivisions(xDivisions, yDivisions);
		SBImage[] subImages = image.createSubImageArray(divs);
		int subImageCount = 0;
		// int p = 0;
		// int q = 0;
		int i = 0;
		for (SBImage subImage : subImages) {
			// int total = subImage.width * subImage.height;
			double mean = subImage.getMeanAndSD().first;
			double sigma = subImage.getMeanAndSD().second;
			double multiplier = 1.0;
			int newSum = 0;
			int loopNo = 0;
			int leftCutoff = 0;
			int rightCutoff = 0;
			if (mean < 95) {
				subImage = SBImageUtils.lighten(subImage, 3, 3);
				mean = subImage.getMeanAndSD().first;
				sigma = subImage.getMeanAndSD().second;
			}
			if (mean > 240) {
				subImage = SBImageUtils.darken(subImage, 3, 3);
				mean = subImage.getMeanAndSD().first;
				sigma = subImage.getMeanAndSD().second;
			}
			if ((sigma / mean) > 0.4) {
				System.out.println("Initial Image: " + i + " - Mean : " + mean + "; Sigma : " + sigma);
				subImage = SBImageUtils.lighten(subImage, 5, 5);
				subImage = SBImageUtils.lighten(subImage, 3, 3);
				mean = subImage.getMeanAndSD().first;
				sigma = subImage.getMeanAndSD().second;
				// ImageUtils.writeFile(subImage, "png", baseTestDir + "LiLiSI5533" + i +
				// ".png", 300);
				// System.out.println("Image: " + i + " - Eroded once with as sigma/mean >
				// 0.4");
			} else {
				if ((sigma / mean) > 0.3) {
					System.out.println("Initial Image: " + i + " - Mean : " + mean + "; Sigma : " + sigma);
					subImage = SBImageUtils.lighten(subImage, 5, 5);
					// subImage = SBImageUtils.lighten(subImage, 3, 3);
					mean = subImage.getMeanAndSD().first;
					sigma = subImage.getMeanAndSD().second;
					// ImageUtils.writeFile(subImage, "png", baseTestDir + "LiSI55" + i + ".png",
					// 300);
					// System.out.println("Image: " + i + " - Eroded once with as sigma/mean >
					// 0.4");
				} else {
					if ((sigma / mean) > 0.20) {
						System.out.println("Initial Image: " + i + " - Mean : " + mean + "; Sigma : " + sigma);
						subImage = SBImageUtils.lighten(subImage, 3, 3);
						mean = subImage.getMeanAndSD().first;
						sigma = subImage.getMeanAndSD().second;
						// ImageUtils.writeFile(subImage, "png", baseTestDir + "LiSI33" + i + ".png",
						// 300);
						// System.out.println("Image: " + i + " - Lightened once as sigma/mean > 0.2");
					} else {
						if ((sigma / mean) < 0.005) {
							System.out.println("Initial Image: " + i + " - Mean : " + mean + "; Sigma : " + sigma);
							subImage = SBImageUtils.dilate(subImage, 3, 3);
							mean = subImage.getMeanAndSD().first;
							sigma = subImage.getMeanAndSD().second;
							// ImageUtils.writeFile(subImage, "png", baseTestDir + "DiSI33" + i + ".png",
							// 300);
							// System.out.println("Image: " + i + " - Dilated once as sigma/mean < 0.005");
						} else {
							if ((sigma / mean) < 0.075) {
								System.out.println("Initial Image: " + i + " - Mean : " + mean + "; Sigma : " + sigma);
								subImage = SBImageUtils.darken(subImage, 3, 3);
								mean = subImage.getMeanAndSD().first;
								sigma = subImage.getMeanAndSD().second;
								// ImageUtils.writeFile(subImage, "png", baseTestDir + "DaSI33" + i + ".png",
								// 300);
								// System.out.println("Image: " + i + " - Darkened once as sigma/mean < 0.075");
							}
						}
					}
				}
			}
			if (mean > 10) {
				if ((sigma / mean) > 0.5) {
					multiplier = 0.3;
				} else {
					if ((sigma / mean) > 0.4) {
						multiplier = 0.4;
					} else {
						if ((sigma / mean) > 0.3) {
							multiplier = 0.5;
						} else {
							if ((sigma / mean) > 0.2) {
								multiplier = 0.6;
							} else {
								if ((sigma / mean) > 0.1) {
									multiplier = 0.8;
								}
							}
						}
					}
				}
			}
			if (mean > 232) {
				multiplier = 0.5;
			}
			// System.out.println(i + " - Mean : " + mean + "; Sigma : " + sigma);

			leftCutoff = Math.max(0, (int) (mean - (multiplier * sigma)));
			rightCutoff = Math.min(255, (int) (mean - (Math.max(0.2, multiplier - 0.8) * sigma)));
			if (sigma < 15) {
				if (mean > 236) {
					leftCutoff = (int) (mean - (9 * sigma));
					rightCutoff = Math.min(255, (int) (mean - (3 * sigma)));
				} else {
					leftCutoff = Math.max(0, (int) (mean - (3 * sigma)));
					rightCutoff = Math.min(255, (int) (mean - (1 * sigma)));
				}
			}

			// else {
			// if (mean < 120) {
			// if ((sigma / mean) < 0.35) {
			// leftCutoff = Math.max(0, (int) (mean - (2 * sigma)));
			// rightCutoff = Math.min(255, (int) (mean - (1 * sigma)));
			// }
			// }
			// }
			int[][] out = new int[subImage.height][subImage.width];
			do {
				newSum = 0;
				leftCutoff = Math.max(0, leftCutoff - (int) (0.25 * loopNo));
				rightCutoff = Math.max(leftCutoff, rightCutoff - (1 * loopNo));
				int y = 0;
				for (int[] row : subImage.pixels) {
					int x = 0;
					for (int pixel : row) {
						if (pixel >= rightCutoff) {
							out[y][x] = WHITE_PIXEL_REPLACEMENT;
						} else {
							if (pixel < leftCutoff) {
								out[y][x] = BLACK_PIXEL_REPLACEMENT;
							} else {
								out[y][x] = GRAY_PIXEL_REPLACEMENT;
							}
						}
						newSum += out[y][x];
						++x;
					}
					++y;
				}
				++loopNo;
			} while ((((newSum * 1.0) / (subImage.width * subImage.height)) <= 251)
					&& (rightCutoff > (leftCutoff + 3)));
			// System.out.println("Looped in image - " + i + " " + loopNo + " times");
			i++;

			// ImageUtils.writeFile(new SBImage(out), "png", "E:\\TechWerx\\Java\\Working\\"
			// + "AA" + p++ + ".png", 300);

			/*
			 * if (sigma < 20) { rightCutoff = Math.min((int) (leftCutoff + (4 * sigma)),
			 * 250); } else { if (sigma < 40) { rightCutoff = Math.min((int) (leftCutoff +
			 * (3 * sigma)), 250); } else { rightCutoff = Math.min((int) (leftCutoff + (2 *
			 * sigma)), 250); } }
			 */

			/*
			 * loopNo = 0; int[][] outFinal = new int[subImage.height][subImage.width]; do {
			 * newSum = 0; // leftCutoff = (int) (leftCutoff - (0.05 * loopNo)); rightCutoff
			 * = rightCutoff - (1 * loopNo); int y = 0; for (int[] row : subImage.pixels) {
			 * int x = 0; for (int pixel : row) { if (pixel >= rightCutoff) { outFinal[y][x]
			 * = WHITE_PIXEL_REPLACEMENT; } else { if (pixel < leftCutoff) { outFinal[y][x]
			 * = BLACK_PIXEL_REPLACEMENT; } else { outFinal[y][x] = GRAY_PIXEL_REPLACEMENT;
			 * } } newSum += outFinal[y][x]; ++x; } ++y; } ++loopNo; } while ((((newSum *
			 * 1.0) / (subImage.width * subImage.height)) <= 252) && (rightCutoff >
			 * (leftCutoff + 3)));
			 */

			// ImageUtils.writeFile(new SBImage(outFinal), "png",
			// "E:\\TechWerx\\Java\\Working\\" + "CC" + q++ + ".png",
			// 300);
			// subImages[subImageCount++] = new SBImage(outFinal);
			// SBImageUtils.makeOnePixelBorder(out);
			subImages[subImageCount++] = new SBImage(out);
		}
		return subImages;
	}

	public static SBImage[] parallelBagchiTrinarizationForPK(SBImage image) throws Exception {
		SBImage[] subImages = image.createSubImageArray();
		int subImageCount = 0;
		for (SBImage subImage : subImages) {
			int total = subImage.width * subImage.height;
			int bucketSize = 4;
			int[] histogram = new int[256 / bucketSize];
			int[] cdf = new int[256 / bucketSize];
			for (int y = 0; y < subImage.height; y++) {
				for (int x = 0; x < subImage.width; x++) {
					histogram[subImage.pixels[y][x] / bucketSize]++;
				}
			}
			histogram[0] = 0;
			histogram[255 / bucketSize] = 0;
			cdf[0] = histogram[0];
			double percentCutoff = 0.05;
			for (int i = 1; i < (256 / bucketSize); i++) {
				if (histogram[i] > ((total * percentCutoff * 1.0) / 100)) {
					cdf[i] = cdf[i - 1] + histogram[i];
				} else {
					cdf[i] = cdf[i - 1];
				}
			}
			int bucketDelta = 3;
			int cutoffBucket = 0;
			for (int i = bucketDelta - 1; i < ((256 / bucketSize) - (bucketDelta - 1)); ++i) {
				cutoffBucket = i;
				if ((cdf[i] == 0) || (cdf[i - 1] == 0) || (i < 10)) {
					continue;
				}
				double leftSideSlope = cdf[i] - cdf[i - 2];
				double rightSideSlope = cdf[i + 2] - cdf[i];
				if (rightSideSlope > (3.0 * leftSideSlope)) {
					break;
				}
			}
			int originalleftCutoff = ((cutoffBucket - 1) * bucketSize) + (bucketSize - 1);
			int originalrightCutoff = ((cutoffBucket + 2) * bucketSize) + (bucketSize - 1);

			final double mean = subImage.getMeanAndSD().first;
			final double sigma = subImage.getMeanAndSD().second;
			int originalleftCutoffND;
			int originalrightCutoffND;
			if ((mean / sigma) >= 10) {
				originalleftCutoffND = (int) (mean - (2.75 * sigma));
				originalrightCutoffND = (int) (mean - (2 * sigma));
				// leftCutoff = (int) (mean - (2.5 * sigma));
				// rightCutoff = (int) (mean - (1.5 * sigma));
			} else {
				if ((mean / sigma) >= 5) {
					originalleftCutoffND = (int) (mean - 25);
					// leftCutoff = (int) (mean - 20);
					originalrightCutoffND = (int) (mean - 10);
				} else {
					originalleftCutoffND = (int) (mean - 35);
					// rightCutoff = (int) (mean - 25);
					originalrightCutoffND = (int) (mean - 10);
				}
			}

			int newSum = 0;
			int loopNo = 0;
			int leftCutoff;
			int rightCutoff;
			int leftCutoffND;
			int rightCutoffND;
			int[][] out = new int[subImage.height][subImage.width];
			do {
				newSum = 0;
				leftCutoff = originalleftCutoff - (5 * loopNo);
				rightCutoff = originalrightCutoff - (int) (4.5 * loopNo);
				leftCutoffND = originalleftCutoffND - (5 * loopNo);
				rightCutoffND = originalrightCutoffND - (int) (4.5 * loopNo);
				int y = 0;
				// System.out.println("Entering loop no = " + loopNo + " with leftCutoff = " +
				// leftCutoff
				// + "; rightCutoff = " + rightCutoff);
				for (int[] row : subImage.pixels) {
					int x = 0;
					for (int pixel : row) {
						// if (pixel >= 180) {
						if (pixel >= 170) {
							leftCutoff = leftCutoffND;
							rightCutoff = rightCutoffND;
							// if (loopNo > 0) {
							// System.out.println("Changed Cutoffs to CutoffNDs");
							// }
						}

						if (pixel < leftCutoff) {
							out[y][x] = BLACK_PIXEL_REPLACEMENT;
						} else {
							if (pixel < rightCutoff) {
								// subImage.pixels[y][x] = 242;
								out[y][x] = GRAY_PIXEL_REPLACEMENT;
							} else {
								out[y][x] = WHITE_PIXEL_REPLACEMENT;
							}
						}
						newSum += out[y][x];
						++x;
					}
					++y;
				}
				// System.out.println("Mean = " + ((newSum * 1.0) / (subImage.width *
				// subImage.height)));
				++loopNo;

			} while ((((newSum * 1.0) / (subImage.width * subImage.height)) <= 144) // Experiment with 160 as well
					&& (leftCutoff > ((mean * 2.0) / 3)));
			subImages[subImageCount++] = new SBImage(out);
		}
		return subImages;
	}

	public static SBImage meanAdaptiveCleaning(SBImage image) {
		int[][] out = new int[image.height][image.width];
		setBorderValues(image, out, Integer.MIN_VALUE, 3, 3);
		for (int y = 1; y < (image.height - 1); y++) {
			for (int x = 1; x < (image.width - 1); x++) {
				double mean = 0.0;
				for (int dy = -1; dy <= 1; dy++) {
					for (int dx = -1; dx <= 1; dx++) {
						mean += image.pixels[y + dy][x + dx];
					}
				}
				mean = (mean * 1.0) / 9;
				final int pixel = image.pixels[y][x];
				if (pixel > 128) {
					if (pixel < (0.975 * mean)) {
						out[y][x] = 255;
					} else {
						out[y][x] = (int) mean;
					}
				} else {
					out[y][x] = pixel;
				}
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage histEqPostNorm_Sigma(SBImage image) {

		int[][] out = new int[image.height][image.width];
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage histEqPostNormalisation(SBImage image) {

		int[][] out = new int[image.height][image.width];
		ArrayList<Integer> histogram = SBImageUtils.getHistogram(image);
		int modeAt = 0;
		int modeFreq = 0;

		// ignore pixel 0 and the last few pixels for determination of mode
		for (int i = 1; i < 250; ++i) {
			if (histogram.get(i) > modeFreq) {
				modeAt = i;
				modeFreq = histogram.get(i);
			}
		}

		int newMode = (modeAt + 255) / 2; // shift mode to the right, to mid-point between current and 255
		double oldSD = image.getMeanAndSD().second;
		double scale = (255 - newMode) / (3 * oldSD);

		HashMap<Integer, Integer> pixelMapping = new HashMap<Integer, Integer>(256);
		for (int i = 0; i < modeAt; ++i) {
			pixelMapping.put(i, i);
		}
		pixelMapping.put(modeAt, newMode);
		for (int i = modeAt + 1; i < (int) (modeAt + (3 * oldSD)); ++i) {
			int newValue = newMode + (int) (scale * 1.0 * (i - modeAt));
			pixelMapping.put(i, newValue);
		}

		for (int i = (int) (modeAt + (3 * oldSD)); i < 256; ++i) {
			pixelMapping.put(i, 255);
		}

		int[] newPixelValues = new int[256];
		newPixelValues[255] = 255;
		for (int i = 254; i >= 0; i--) {
			Integer newValue = pixelMapping.get(i);
			if (newValue == null) {
				newPixelValues[i] = newPixelValues[i + 1];
			} else {
				newPixelValues[i] = newValue;
			}
		}

		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				out[y][x] = newPixelValues[image.pixels[y][x]];
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage histEqPostNorm_Scaling_Old(SBImage image) {

		final int MAX_PIXELS = 150;
		final int NEW_RIGHT_MODE = 192;
		final int NEW_LEFT_MODE = 160;

		int totalPixels = image.width * image.height;
		int[][] out = new int[image.height][image.width];
		ArrayList<Integer> histogram = SBImageUtils.getHistogram(image);
		int MODE = 0;
		boolean modeFound = false;
		int maxValue = Integer.MIN_VALUE;

		ArrayList<Integer> pixelList = new ArrayList<Integer>();
		double cutOffPercentage = 0.25;

		// Instant t = Instant.now();
		while ((pixelList.size() < MAX_PIXELS) && (cutOffPercentage > 0.0025)) {
			pixelList.clear();
			// Exclude i = 0, as those pixels are OK
			for (int i = 1; i < 256; i++) {
				final int pixelCount = histogram.get(i);
				if (!modeFound) {
					if (pixelCount > maxValue) {
						MODE = i;
						maxValue = pixelCount;
					}
				}
				if (pixelCount > ((totalPixels * cutOffPercentage) / 100.0)) {
					pixelList.add(Integer.valueOf(i));
				}
			}
			cutOffPercentage -= 0.00025;
			modeFound = true;
		}
		// long timeTaken = Duration.between(t, Instant.now()).toMillis();
		// System.out.println("Time taken to collect pixel count = " + timeTaken);

		// System.out.format("The final cut off percentage reached is : %.7f percent",
		// cutOffPercentage / 100).println();

		int countBelowMode = 1; // since pixel 0 is already factored in and hence, i starts from 0 in the loop
								// below
		for (int i = 1; i < MODE; i++) {
			if (pixelList.contains(i)) {
				countBelowMode++;
			}
		}
		double belowModeMultiplier = (NEW_LEFT_MODE * 1.0) / countBelowMode;
		// System.out.println("countBelowMode = " + countBelowMode + ";
		// belowModeMultiplier = " + belowModeMultiplier);

		int countAboveMode = 0;
		for (int i = MODE + 1; i < 256; i++) {
			if (pixelList.contains(i)) {
				countAboveMode++;
			}
		}
		double aboveModeMultiplier = ((255 - NEW_RIGHT_MODE) * 1.0) / countAboveMode;
		// System.out.println("countAboveMode = " + countAboveMode + ";
		// aboveModeMultiplier = " + aboveModeMultiplier);

		HashMap<Integer, Integer> pixelMapping = new HashMap<>();
		pixelMapping.put(MODE, NEW_RIGHT_MODE);
		// pixelMapping.put(MODE - 1, NEW_RIGHT_MODE);

		for (int i = 0; i < MODE; i++) {
			final int newPixel = (int) Math.max(0, NEW_LEFT_MODE - (belowModeMultiplier * (MODE - i)));
			pixelMapping.put(i, newPixel);
		}

		for (int i = MODE + 1; i < 256; i++) {
			final int newPixel = (int) Math.min(255, NEW_RIGHT_MODE + (aboveModeMultiplier * (i - MODE)));
			pixelMapping.put(i, newPixel);
		}

		int[] newPixelValues = pixelMapping.values().stream().mapToInt(i -> i).toArray();

		// t = Instant.now();
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				// out[y][x] = pixelMapping.get(image.pixels[y][x]);
				out[y][x] = newPixelValues[image.pixels[y][x]];
			}
		}
		// timeTaken = Duration.between(t, Instant.now()).toMillis();
		// System.out.println("Time taken to set new pixels = " + timeTaken);
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage histEq_Scaling(SBImage image) {
		return histEq_Scaling(image, 0, 0, 0, 0);
	}

	public static SBImage histEq_Scaling(SBImage image, int left_Cutoff, int first_Right, int second_Left,
			int right_Cutoff) {

		// classify every between 0-31 as 31; and, every pixel between 224-255 as 224;
		// classify pixels between 160-176 as 160; and, pixels between 176-192 as 192
		// then stretch the 2 histogram on both sides

		// System.out.println("Mean = " + image.getMeanAndSD().first + "; StdDev = " +
		// image.getMeanAndSD().second);
		// final int ITERATIONS = 6;

		final int LEFT_CUTOFF = (left_Cutoff == 0 ? 48 : left_Cutoff);
		final int FIRST_RIGHT = (first_Right == 0 ? 160 : first_Right);
		final int SECOND_LEFT = (second_Left == 0 ? 180 : second_Left);
		final int RIGHT_CUTOFF = (right_Cutoff == 0 ? 240 : right_Cutoff);

		// final int LEFT_ITERATION_DECREMENT = LEFT_CUTOFF / ITERATIONS;
		// final int RIGHT_ITERATION_INCREMENT = (SECOND_LEFT - FIRST_RIGHT) /
		// ITERATIONS;

		// final int ITERATION1_LEFT_CUTOFF = LEFT_CUTOFF - ((LEFT_CUTOFF + 1) /
		// ITERATIONS);
		// final int ITERATION1_FIRST_RIGHT = FIRST_RIGHT + (((SECOND_LEFT -
		// FIRST_RIGHT) + 1) / ITERATIONS);

		int[][] out = new int[image.height][image.width];
		int[] histogram = new int[256];

		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				if (image.pixels[y][x] <= LEFT_CUTOFF) {
					out[y][x] = LEFT_CUTOFF;
					histogram[out[y][x]]++;
					continue;
				}
				if (image.pixels[y][x] >= RIGHT_CUTOFF) {
					out[y][x] = RIGHT_CUTOFF;
					histogram[out[y][x]]++;
					continue;
				}
				if (image.pixels[y][x] >= FIRST_RIGHT) {
					if (image.pixels[y][x] < ((FIRST_RIGHT + SECOND_LEFT) / 2)) {
						out[y][x] = FIRST_RIGHT;
						histogram[out[y][x]]++;
						continue;
					}
				}
				if (image.pixels[y][x] >= ((FIRST_RIGHT + SECOND_LEFT) / 2)) {
					if (image.pixels[y][x] < SECOND_LEFT) {
						out[y][x] = SECOND_LEFT;
						histogram[out[y][x]]++;
						continue;
					}
				}
				out[y][x] = image.pixels[y][x];
				histogram[out[y][x]]++;
			}
		}

		int leftModeValue = 95;
		int leftModeFreq = 0;
		int rightModeValue = 224;
		int rightModeFreq = 0;

		for (int i = LEFT_CUTOFF + 1; i < FIRST_RIGHT; i++) {
			if (histogram[i] >= leftModeFreq) {
				leftModeValue = i;
				leftModeFreq = histogram[i];
			}
		}

		for (int i = SECOND_LEFT + 1; i < RIGHT_CUTOFF; i++) {
			if (histogram[i] >= rightModeFreq) {
				rightModeValue = i;
				rightModeFreq = histogram[i];
			}
		}
		HashMap<Integer, Integer> pixelMapping = new HashMap<>(256);

		/*
		 * // Original code
		 *
		 * pixelMapping.put(LEFT_CUTOFF, 0);
		 *
		 * double lls = ((leftModeValue - 0.0)) * 1.0/ (leftModeValue - LEFT_CUTOFF);
		 * for (int i = LEFT_CUTOFF + 1; i < leftModeValue; i++) { int value = (int)
		 * (leftModeValue - ((leftModeValue - i) * lls)); pixelMapping.put(i, value); }
		 *
		 * // deliberately not multiplying numerator by 1.0, as it improves output
		 * double lrs = (((((FIRST_RIGHT + SECOND_LEFT) * 1.0) / 2) - leftModeValue)) /
		 * (FIRST_RIGHT - leftModeValue); for (int i = leftModeValue; i < FIRST_RIGHT;
		 * i++) { int value = (int) (((i - leftModeValue) * lrs) + leftModeValue);
		 * pixelMapping.put(i, value); }
		 *
		 * pixelMapping.replace(FIRST_RIGHT, (FIRST_RIGHT + SECOND_LEFT) / 2);
		 * pixelMapping.put(SECOND_LEFT, ((FIRST_RIGHT + SECOND_LEFT) / 2) + 1);
		 *
		 * double rls = ((rightModeValue - (((FIRST_RIGHT + SECOND_LEFT) * 1.0) / 2))) /
		 * (rightModeValue - SECOND_LEFT); for (int i = SECOND_LEFT + 1; i <
		 * rightModeValue; i++) { int value = (int) (rightModeValue - ((rightModeValue -
		 * i) * rls)); pixelMapping.put(i, value); }
		 *
		 * double rrs = ((255 - rightModeValue)) *1.0 / (RIGHT_CUTOFF - rightModeValue);
		 * for (int i = rightModeValue; i < RIGHT_CUTOFF; i++) { int value = (int) (((i
		 * - rightModeValue) * rrs) + rightModeValue); pixelMapping.put(i, value); }
		 *
		 * pixelMapping.put(RIGHT_CUTOFF, 255);
		 *
		 * // This block of code was the original mapping scheme, where pixels are
		 * stretched on both sides of the mode for both black and white portions
		 */

		pixelMapping.put(LEFT_CUTOFF, 0);

		double lls = ((leftModeValue - 0.0) * 1.0) / (leftModeValue - LEFT_CUTOFF);
		for (int i = LEFT_CUTOFF + 1; i < leftModeValue; i++) {
			int value = (int) (leftModeValue - ((leftModeValue - i) * lls));
			pixelMapping.put(i, value);
		}

		double lrs = ((SECOND_LEFT - leftModeValue) * 1.0) / (FIRST_RIGHT - leftModeValue);
		for (int i = leftModeValue; i < FIRST_RIGHT; i++) {
			int value = (int) (((i - leftModeValue) * lrs) + leftModeValue);
			pixelMapping.put(i, value);
		}

		pixelMapping.replace(FIRST_RIGHT, SECOND_LEFT);
		pixelMapping.put(SECOND_LEFT, SECOND_LEFT);

		for (int i = SECOND_LEFT + 1; i < rightModeValue; i++) {
			pixelMapping.put(i, i);
		}

		double rrs = ((254 - rightModeValue)) / (RIGHT_CUTOFF - rightModeValue);
		for (int i = rightModeValue; i < RIGHT_CUTOFF; i++) {
			int value = (int) (((i - rightModeValue) * rrs) + rightModeValue);
			pixelMapping.put(i, value);
		}

		pixelMapping.put(RIGHT_CUTOFF, 255);

		int[] newPixelValues = new int[256];
		newPixelValues[255] = 255;
		for (int i = 254; i >= 0; i--) {
			Integer newValue = pixelMapping.get(i);
			if (newValue == null) {
				newPixelValues[i] = newPixelValues[i + 1];
			} else {
				newPixelValues[i] = newValue;
			}
		}

		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				out[y][x] = newPixelValues[image.pixels[y][x]];
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static NumberTriplet getMode(SBImage image) throws Exception {
		int[] histogram = new int[256];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				histogram[image.pixels[y][x]]++;
			}
		}
		int maxFrequencyAt = 0;
		int maxValue = 0;
		int secondHighestAt = 0;
		for (int i = 0; i < 256; i++) {
			if (histogram[i] > maxValue) {
				maxValue = histogram[i];
				secondHighestAt = maxFrequencyAt;
				maxFrequencyAt = i;
			}
		}
		return new NumberTriplet(maxFrequencyAt, secondHighestAt, maxValue);
	}

	public static SBImage iterativeHistEq_Scaling(SBImage image) {

		// classify every between 0-31 as 31; and, every pixel between 224-255 as 224;
		// classify pixels between 160-176 as 160; and, pixels between 176-192 as 192
		// then stretch the 2 histogram on both sides

		// System.out.println("Mean = " + image.getMeanAndSD().first + "; StdDev = " +
		// image.getMeanAndSD().second);

		final int ITERATIONS = 6;

		final int LEFT_CUTOFF = 30;
		final int RIGHT_CUTOFF = 240;
		final int FIRST_RIGHT = 172;
		final int SECOND_LEFT = 208;

		// final int LEFT_ITERATION_DECREMENT = LEFT_CUTOFF / ITERATIONS;
		// final int RIGHT_ITERATION_INCREMENT = (SECOND_LEFT - FIRST_RIGHT) /
		// ITERATIONS;

		// final int ITERATION1_LEFT_CUTOFF = LEFT_CUTOFF - ((LEFT_CUTOFF + 1) /
		// ITERATIONS);
		// final int ITERATION1_FIRST_RIGHT = FIRST_RIGHT + (((SECOND_LEFT -
		// FIRST_RIGHT) + 1) / ITERATIONS);

		int[][] out = new int[image.height][image.width];
		int[] histogram = new int[256];

		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				if (image.pixels[y][x] <= LEFT_CUTOFF) {
					out[y][x] = LEFT_CUTOFF;
					histogram[out[y][x]]++;
					continue;
				}
				if (image.pixels[y][x] >= RIGHT_CUTOFF) {
					out[y][x] = RIGHT_CUTOFF;
					histogram[out[y][x]]++;
					continue;
				}
				if (image.pixels[y][x] >= FIRST_RIGHT) {
					if (image.pixels[y][x] < ((FIRST_RIGHT + SECOND_LEFT) / 2)) {
						out[y][x] = FIRST_RIGHT;
						histogram[out[y][x]]++;
						continue;
					}
				}
				if (image.pixels[y][x] >= ((FIRST_RIGHT + SECOND_LEFT) / 2)) {
					if (image.pixels[y][x] < SECOND_LEFT) {
						out[y][x] = SECOND_LEFT;
						histogram[out[y][x]]++;
						continue;
					}
				}
				out[y][x] = image.pixels[y][x];
				histogram[out[y][x]]++;
			}
		}

		int leftModeValue = 95;
		int leftModeFreq = 0;
		int rightModeValue = 224;
		int rightModeFreq = 0;

		for (int i = LEFT_CUTOFF + 1; i < FIRST_RIGHT; i++) {
			if (histogram[i] >= leftModeFreq) {
				leftModeValue = i;
				leftModeFreq = histogram[i];
			}
		}

		for (int i = SECOND_LEFT + 1; i < RIGHT_CUTOFF; i++) {
			if (histogram[i] >= rightModeFreq) {
				rightModeValue = i;
				rightModeFreq = histogram[i];
			}
		}

		// iterate the pixel mapping of the imaginary left camel hump

		int leftCutOff;
		int leftTarget;
		int firstRight;
		int rightTarget;

		List<HashMap<Integer, Integer>> interimPixelMapping = new ArrayList<HashMap<Integer, Integer>>(ITERATIONS);
		for (int i = 0; i < ITERATIONS; ++i) {
			interimPixelMapping.add(new HashMap<Integer, Integer>());
		}

		for (int i = 0; i < ITERATIONS; ++i) {
			leftCutOff = LEFT_CUTOFF - (i * (LEFT_CUTOFF / ITERATIONS));
			leftTarget = Math.max(0, LEFT_CUTOFF - ((i + 1) * (LEFT_CUTOFF / ITERATIONS)));
			interimPixelMapping.get(i).put(leftCutOff, leftTarget);

			double lls = ((leftModeValue - leftTarget) * 1.0) / (leftModeValue - leftCutOff);
			for (int j = leftCutOff + 1; j < leftModeValue; ++j) {
				int value = (int) (leftModeValue - ((leftModeValue - j) * lls));
				interimPixelMapping.get(i).put(j, value);
			}

			firstRight = FIRST_RIGHT + ((i * (SECOND_LEFT - FIRST_RIGHT)) / ITERATIONS);
			rightTarget = FIRST_RIGHT + (((i + 1) * (SECOND_LEFT - FIRST_RIGHT)) / ITERATIONS);

			double lrs = ((rightTarget - leftModeValue) * 1.0) / (firstRight - leftModeValue);
			for (int j = leftModeValue; j <= firstRight; ++j) {
				int value = (int) (((j - leftModeValue) * lrs) + leftModeValue);
				interimPixelMapping.get(i).put(j, value);
			}
		}

		HashMap<Integer, Integer> pixelMapping = new HashMap<>();

		for (int i = LEFT_CUTOFF; i <= FIRST_RIGHT; ++i) {
			int value = i;
			for (int j = 0; j < ITERATIONS; ++j) {
				value = interimPixelMapping.get(j).get(value);
			}
			pixelMapping.put(i, value);
		}

		pixelMapping.replace(FIRST_RIGHT, SECOND_LEFT);
		pixelMapping.put(SECOND_LEFT, SECOND_LEFT);

		for (int i = SECOND_LEFT + 1; i < rightModeValue; i++) {
			pixelMapping.put(i, i);
		}

		double rrs = ((254 - rightModeValue)) / (RIGHT_CUTOFF - rightModeValue);
		for (int i = rightModeValue; i < RIGHT_CUTOFF; i++) {
			int value = (int) (((i - rightModeValue) * rrs) + rightModeValue);
			pixelMapping.put(i, value);
		}

		pixelMapping.put(RIGHT_CUTOFF, 255);

		int[] newPixelValues = new int[256];
		newPixelValues[255] = 255;
		for (int i = 254; i >= 0; i--) {
			Integer newValue = pixelMapping.get(i);
			if (newValue == null) {
				newPixelValues[i] = newPixelValues[i + 1];
			} else {
				newPixelValues[i] = newValue;
			}
		}

		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				out[y][x] = newPixelValues[image.pixels[y][x]];
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static void replace(SBImage image, int targetPixelValue, int replacementValue) {
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				if (image.pixels[y][x] == targetPixelValue) {
					image.pixels[y][x] = replacementValue;
				}
			}
		}
	}

	public static SBImage L2Normalize(SBImage image, int kernelSize) {
		double[][] temp = new double[image.height][image.width];
		setBorderValues(null, temp, 1.0 / kernelSize, kernelSize, kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				double sum = 0.0f;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						sum += power2[image.pixels[y + dy][x + dx]];
					}
				}
				final double interim = ((image.pixels[y][x] + 1) * 1.0) / (FastMath.sqrt(sum));
				temp[y][x] = interim * interim * interim * interim;
			}
		}
		NumberTriplet dt = NumberTriplet.findMinMax(temp, kernelSize, kernelSize);
		double min = dt.first;
		double max = Math.min(dt.second, dt.third);
		System.out.println("Min = " + min + " ; Max = " + max);
		int[][] out = new int[image.height][image.width];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				final double newPixel = (((temp[y][x] - min) * 255.0) / (max - min));
				out[y][x] = Math.min(Math.max(0, (int) newPixel), 255);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage L2WeightedNormalize(SBImage image, int kernelSize) {
		double[][] temp = new double[image.height][image.width];
		setBorderValues(null, temp, 1.0 / kernelSize, kernelSize, kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				double numerator = 0.0;
				double denominator = 0.0;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						final int where = Math.max(Math.abs(dy), Math.abs(dx));
						final int weight = ((kernelSize / 2) + 1) - where;
						numerator += weight;
						denominator += weight * weight * power2[image.pixels[y + dy][x + dx]];
						;
					}
				}
				final double interim = (numerator * (image.pixels[y][x] + 1) * 1.0) / (FastMath.sqrt(denominator));
				temp[y][x] = interim * interim * interim * interim;
			}
		}
		NumberTriplet dt = NumberTriplet.findMinMax(temp, kernelSize, kernelSize);
		double min = dt.first;
		double max = Math.min(dt.second, dt.third);
		System.out.println("Min = " + min + " ; Max = " + max);
		int[][] out = new int[image.height][image.width];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				final double newPixel = (((temp[y][x] - min) * 255.0) / (max - min));
				out[y][x] = Math.min(Math.max(0, (int) newPixel), 255);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage L3Normalize(SBImage image, int kernelSize) {
		double[][] temp = new double[image.height][image.width];
		setBorderValues(null, temp, 1.0f / FastMath.pow(kernelSize * kernelSize, 1.0 / 3), kernelSize, kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				double sum = 0.0;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						sum += power3[image.pixels[y + dy][x + dx]];
					}
				}
				final double interim = ((image.pixels[y][x] + 1) * 1.0) / (FastMath.pow(sum, (1.0 / 3)));
				temp[y][x] = interim * interim * interim * interim;
			}
		}
		NumberTriplet dt = NumberTriplet.findMinMax(temp, kernelSize, kernelSize);
		double min = dt.first;
		double max = Math.min(dt.second, dt.third);
		System.out.println("Min = " + min + " ; Max = " + max);
		int[][] out = new int[image.height][image.width];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				final double newPixel = (((temp[y][x] - min) * 255.0) / (max - min));
				out[y][x] = Math.min(Math.max(0, (int) newPixel), 255);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage L4Normalize(SBImage image, int kernelSize) {
		double[][] temp = new double[image.height][image.width];
		setBorderValues(null, temp, (float) (1.0 / FastMath.sqrt(kernelSize)), kernelSize, kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				double sum = 0.0;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						sum += power4[image.pixels[y + dy][x + dx]];
					}
				}
				final double interim = ((image.pixels[y][x] + 1) * 1.0) / (FastMath.pow(sum, (1.0 / 4)));
				temp[y][x] = interim * interim * interim * interim;
				// if (Double.isNaN(temp[y][x])) {
				// System.out.format("pixels[%d,%d] = %d ; power4 = %.2f; interim = %.10f;
				// temp[y,x] = ", y, x,
				// image.pixels[y][x], power4[image.pixels[y][x]], interim,
				// temp[y][x]).println();
				// }
			}
		}
		// for (double[] row : temp) {
		// System.out.println(Arrays.toString(row));
		// }
		NumberTriplet dt = NumberTriplet.findMinMax(temp, kernelSize, kernelSize);
		double min = dt.first;
		double max = Math.min(dt.second, dt.third);
		System.out.println("Min = " + min + " ; Max = " + max);
		int[][] out = new int[image.height][image.width];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				final double newPixel = (((temp[y][x] - min) * 255.0) / (max - min));
				out[y][x] = Math.min(Math.max(0, (int) newPixel), 255);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage L8Normalize(SBImage image, int kernelSize) {
		double[][] temp = new double[image.height][image.width];
		setBorderValues(null, temp, (float) (1.0 / FastMath.sqrt(FastMath.sqrt(kernelSize))), kernelSize, kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				double sum = 0.0;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						sum += power8[image.pixels[y + dy][x + dx]];
					}
				}
				final double interim = ((image.pixels[y][x] + 1) * 1.0) / (FastMath.pow(sum, (1.0 / 8)));
				temp[y][x] = interim * interim * interim * interim;
				// if (Double.isNaN(temp[y][x])) {
				// System.out.format("pixels[%d,%d] = %d ; power4 = %.2f; interim = %.10f;
				// temp[y,x] = ", y, x,
				// image.pixels[y][x], power4[image.pixels[y][x]], interim,
				// temp[y][x]).println();
				// }
			}
		}
		// for (double[] row : temp) {
		// System.out.println(Arrays.toString(row));
		// }
		NumberTriplet dt = NumberTriplet.findMinMax(temp, kernelSize, kernelSize);
		double min = dt.first;
		double max = Math.min(dt.second, dt.third);
		System.out.println("Min = " + min + " ; Max = " + max);
		int[][] out = new int[image.height][image.width];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				final double newPixel = (((temp[y][x] - min) * 255.0) / (max - min));
				out[y][x] = Math.min(Math.max(0, (int) newPixel), 255);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage LMinus1Normalize(SBImage image, int kernelSize) {
		double[][] temp = new double[image.height][image.width];
		setBorderValues(null, temp, 1.0 / (kernelSize * kernelSize), kernelSize, kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				double sum = 0.0;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						sum += (1.0 * inverse1[image.pixels[y + dy][x + dx]]);
					}
				}
				final double interim = (1.0 / (image.pixels[y][x] + 1)) / sum;
				temp[y][x] = interim * interim * interim * interim;
			}
		}
		// System.out.println(Arrays.deepToString(temp));
		NumberTriplet dt = NumberTriplet.findMinMax(temp, kernelSize, kernelSize);
		double min = dt.first;
		double max = Math.min(dt.second, dt.third);
		System.out.println("Min = " + min + " ; Max = " + max);
		int[][] out = new int[image.height][image.width];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				final double newPixel = (((temp[y][x] - min) * 255.0) / (max - min));
				out[y][x] = Math.min(Math.max(0, (int) newPixel), 255);
				// if ((out[y][x] < 0) || (out[y][x] > 255)) {
				// System.out.println("Pixel value at [" + y + "," + x + "] = " + out[y][x]);
				// }
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage LMinus1WeightedNormalize(SBImage image, int kernelSize) {
		double[][] temp = new double[image.height][image.width];
		setBorderValues(null, temp, 1.0 / (kernelSize * kernelSize), kernelSize, kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				double numerator = 0.0;
				double denominator = 0.0;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						final int where = Math.max(Math.abs(dy), Math.abs(dx));
						final int weight = ((kernelSize / 2) + 1) - where;
						numerator += weight;
						denominator += (weight * 1.0 * inverse1[image.pixels[y + dy][x + dx]]);
					}
				}
				final double interim = (numerator * 1.0 * inverse1[image.pixels[y][x]]) / denominator;
				temp[y][x] = interim * interim * interim * interim;
			}
		}
		// System.out.println(Arrays.deepToString(temp));
		NumberTriplet dt = NumberTriplet.findMinMax(temp, kernelSize, kernelSize);
		double min = dt.first;
		double max = Math.min(dt.second, dt.third);
		System.out.println("Min = " + min + " ; Max = " + max);
		int[][] out = new int[image.height][image.width];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				final double newPixel = (((temp[y][x] - min) * 255.0) / (max - min));
				out[y][x] = Math.min(Math.max(0, (int) newPixel), 255);
				// if ((out[y][x] < 0) || (out[y][x] > 255)) {
				// System.out.println("Pixel value at [" + y + "," + x + "] = " + out[y][x]);
				// }
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage LMinus2Normalize(SBImage image, int kernelSize) {
		double[][] temp = new double[image.height][image.width];
		setBorderValues(null, temp, 1.0 / kernelSize, kernelSize, kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				double sum = 0.0;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						sum += (1.0 * inverse2[image.pixels[y + dy][x + dx]]);
					}
				}
				final double interim = FastMath.sqrt(inverse2[image.pixels[y][x]] / sum);
				temp[y][x] = interim * interim * interim * interim;
			}
		}
		NumberTriplet dt = NumberTriplet.findMinMax(temp, kernelSize, kernelSize);
		System.out.println(dt);
		double min = dt.first;
		double max = Math.min(dt.second, dt.third);
		System.out.println("Min = " + min + " ; Max = " + max);
		int[][] out = new int[image.height][image.width];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				final double newPixel = (((temp[y][x] - min) * 255.0f) / (max - min));
				out[y][x] = Math.min(Math.max(0, (int) newPixel), 255);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage LMinus2WeightedNormalize(SBImage image, int kernelSize) {
		double[][] temp = new double[image.height][image.width];
		setBorderValues(null, temp, 1.0 / kernelSize, kernelSize, kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				double numerator = 0.0;
				double denominator = 0.0;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						final int where = Math.max(Math.abs(dy), Math.abs(dx));
						final int weight = ((kernelSize / 2) + 1) - where;
						numerator += weight;
						denominator += weight * weight * inverse2[image.pixels[y + dy][x + dx]];
					}
				}
				final double interim = FastMath.sqrt((numerator * 1.0 * inverse2[image.pixels[y][x]]) / denominator);
				temp[y][x] = interim * interim * interim * interim;
			}
		}
		NumberTriplet dt = NumberTriplet.findMinMax(temp, kernelSize, kernelSize);
		double min = dt.first;
		double max = Math.min(dt.second, dt.third);
		System.out.println("Min = " + min + " ; Max = " + max);
		int[][] out = new int[image.height][image.width];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				final double newPixel = (((temp[y][x] - min) * 255.0) / (max - min));
				out[y][x] = Math.min(Math.max(0, (int) newPixel), 255);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage LNNormalize(SBImage image, int kernelSize, double order) {
		double[][] temp = new double[image.height][image.width];
		setBorderValues(null, temp, 1.0 / FastMath.pow(kernelSize * kernelSize, Math.abs(1.0 / order)), kernelSize,
				kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				double sum = 0.0f;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						sum += FastMath.pow(image.pixels[y + dy][x + dx] + 1, order);
					}
				}
				final double interim;
				if (order >= 0) {
					interim = (image.pixels[y][x] * 1.0) / ((FastMath.pow(sum, Math.abs(1.0 / order))));
				} else {
					interim = (1.0 / (image.pixels[y][x] + 1)) / ((FastMath.pow(sum, Math.abs(1.0 / order))));
				}
				temp[y][x] = interim * interim * interim * interim;
			}
		}
		NumberTriplet dt = NumberTriplet.findMinMax(temp, kernelSize, kernelSize);
		double min = dt.first;
		double max = Math.min(dt.second, dt.third);
		System.out.println("Min = " + min + " ; Max = " + max);
		int[][] out = new int[image.height][image.width];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				final double newPixel = (((temp[y][x] - min) * 255.0) / (max - min));
				out[y][x] = Math.min(Math.max(0, (int) newPixel), 255);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage parallelXErode(SBImage image, int kernelHeight, int kernelWidth, int threshold) {

		// int noOfDivisions = Math.max(1, image.height / 125);
		int noOfDivisions = 3;
		XYDivisions divisions = new XYDivisions(1, noOfDivisions);
		SBImage[] subImages = null;
		try {
			subImages = image.createSubImageArray(divisions, kernelHeight, kernelWidth);
		} catch (Exception e) {
		}
		final SBImage[] results = new SBImage[subImages.length];
		final ExecutorService threadService = Executors.newFixedThreadPool(Math.min(subImages.length, 90));
		final ArrayList<CompletableFuture<Object>> cfs = new ArrayList<>(subImages.length);
		for (int count = 0; count < subImages.length; ++count) {
			final int counter = count;
			final SBImage[] si = subImages;
			cfs.add(CompletableFuture.supplyAsync(() -> {
				results[counter] = erode(si[counter], kernelHeight, kernelWidth, threshold);
				return Boolean.TRUE;
			}, threadService));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		threadService.shutdown();
		try {
			return SBImage.stitchSubImages(results, divisions.xDivisions, divisions.yDivisions, kernelHeight,
					kernelWidth);
		} catch (Exception e) {
			return null;
		}
	}

	public static SBImage parallelYErode(SBImage image, int kernelHeight, int kernelWidth, int threshold) {

		// int noOfDivisions = Math.max(1, image.width / 125);
		int noOfDivisions = 3;
		XYDivisions divisions = new XYDivisions(1, noOfDivisions);
		SBImage transposedImage = new SBImage(SBImageArrayUtils.transpose(image.pixels), false, false, false, false);
		SBImage[] subImages = null;
		try {
			subImages = transposedImage.createSubImageArray(divisions, kernelHeight, kernelWidth);
		} catch (Exception e) {
		}
		final SBImage[] results = new SBImage[subImages.length];
		final ExecutorService threadService = Executors.newFixedThreadPool(Math.min(subImages.length, 90));
		final ArrayList<CompletableFuture<Object>> cfs = new ArrayList<>(subImages.length);
		for (int count = 0; count < subImages.length; ++count) {
			final int counter = count;
			final SBImage[] si = subImages;
			cfs.add(CompletableFuture.supplyAsync(() -> {
				results[counter] = erode(si[counter], kernelHeight, kernelWidth, threshold);
				return Boolean.TRUE;
			}, threadService));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		threadService.shutdown();
		try {
			SBImage newImage = SBImage.stitchSubImages(results, divisions.xDivisions, divisions.yDivisions,
					kernelHeight, kernelWidth);
			return new SBImage(SBImageArrayUtils.transpose(newImage.pixels), false, false, false, false);
		} catch (Exception e) {
			return null;
		}
	}

	public static SBImage parallelXDilate(SBImage image, int kernelHeight, int kernelWidth, int threshold) {

		// int noOfDivisions = Math.max(1, image.height / 125);
		int noOfDivisions = 3;
		XYDivisions divisions = new XYDivisions(1, noOfDivisions);
		SBImage[] subImages = null;
		try {
			subImages = image.createSubImageArray(divisions, kernelHeight, kernelWidth);
		} catch (Exception e) {
			System.out.println(e);
		}
		final SBImage[] results = new SBImage[subImages.length];
		final ExecutorService threadService = Executors.newFixedThreadPool(Math.min(subImages.length, 90));
		final ArrayList<CompletableFuture<Object>> cfs = new ArrayList<>(subImages.length);
		for (int count = 0; count < subImages.length; ++count) {
			final int counter = count;
			final SBImage[] si = subImages;
			cfs.add(CompletableFuture.supplyAsync(() -> {
				results[counter] = dilate(si[counter], kernelHeight, kernelWidth, threshold);
				return Boolean.TRUE;
			}, threadService));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		threadService.shutdown();
		try {
			return SBImage.stitchSubImages(results, divisions.xDivisions, divisions.yDivisions, kernelHeight,
					kernelWidth);
		} catch (Exception e) {
			return null;
		}
	}

	public static SBImage parallelYDilate(SBImage image, int kernelHeight, int kernelWidth, int threshold) {

		// int noOfDivisions = Math.max(1, image.width / 125);
		int noOfDivisions = 3;
		XYDivisions divisions = new XYDivisions(1, noOfDivisions);
		SBImage transposedImage = new SBImage(SBImageArrayUtils.transpose(image.pixels), false, false, false, false);
		SBImage[] subImages = null;
		try {
			subImages = transposedImage.createSubImageArray(divisions, kernelHeight, kernelWidth);
		} catch (Exception e) {
			return null;
		}
		final SBImage[] results = new SBImage[subImages.length];
		final ArrayList<CompletableFuture<Object>> cfs = new ArrayList<>(subImages.length);
		final ExecutorService threadService = Executors.newFixedThreadPool(Math.min(subImages.length, 90));
		for (int count = 0; count < subImages.length; ++count) {
			final int counter = count;
			final SBImage[] si = subImages;
			cfs.add(CompletableFuture.supplyAsync(() -> {
				results[counter] = dilate(si[counter], kernelHeight, kernelWidth, threshold);
				return Boolean.TRUE;
			}, threadService));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		threadService.shutdown();
		try {
			SBImage newImage = SBImage.stitchSubImages(results, divisions.xDivisions, divisions.yDivisions,
					kernelHeight, kernelWidth);
			return new SBImage(SBImageArrayUtils.transpose(newImage.pixels), false, false, false, false);
		} catch (Exception e) {
			return null;
		}
	}

	public static SBImage erode(SBImage image, int kernelHeight, int kernelWidth) {

		int[][] out = new int[image.height][image.width];
		SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelHeight, kernelWidth);
		for (int y = kernelHeight / 2; y < (image.height - (kernelHeight / 2)); y++) {
			for (int x = kernelWidth / 2; x < (image.width - (kernelWidth / 2)); x++) {
				int max = Integer.MIN_VALUE;
				loop1: for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); dx++) {
						max = Math.max(max, image.pixels[y + dy][x + dx]);
						if (max >= (SBImageUtils.WHITE_PIXEL_REPLACEMENT - 5)) {
							break loop1;
						}
					}
				}
				out[y][x] = max;
				// System.out.println("Set out[" + y + "," + x + "] to " + max + " from " +
				// image.pixels[y][x]);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage erode(SBImage image, int kernelHeight, int kernelWidth, int threshold) {

		int[][] out = new int[image.height][image.width];
		SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelHeight, kernelWidth);
		for (int y = kernelHeight / 2; y < (image.height - (kernelHeight / 2)); y++) {
			for (int x = kernelWidth / 2; x < (image.width - (kernelWidth / 2)); x++) {
				int max = Integer.MIN_VALUE;
				loop1: for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); dx++) {
						max = Math.max(max, image.pixels[y + dy][x + dx]);
						if (max >= threshold) {
							break loop1;
						}
					}
				}
				out[y][x] = max;
				// System.out.println("Set out[" + y + "," + x + "] to " + max + " from " +
				// image.pixels[y][x]);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage erode(SBImage image, int kernelHeight, int kernelWidth, Rectangle bounds) {

		int[][] out = new int[image.height][image.width];
		SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelHeight, kernelWidth);
		for (int y = Math.max(kernelHeight / 2, bounds.y); y < Math.min(bounds.y + bounds.height,
				(image.height - (kernelHeight / 2))); ++y) {
			for (int x = Math.max(kernelWidth / 2, bounds.x); x < Math.min(bounds.x + bounds.width,
					(image.width - (kernelWidth / 2))); ++x) {
				int max = Integer.MIN_VALUE;
				loop1: for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); dx++) {
						max = Math.max(max, image.pixels[y + dy][x + dx]);
						if (max >= (SBImageUtils.WHITE_PIXEL_REPLACEMENT - 5)) {
							break loop1;
						}
					}
				}
				out[y][x] = max;
				// System.out.println("Set out[" + y + "," + x + "] to " + max + " from " +
				// image.pixels[y][x]);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage erode(SBImage image, int kernelHeight, int kernelWidth, int threshold, Rectangle bounds) {

		int[][] out = new int[image.height][image.width];
		if ((bounds.x < (kernelWidth / 2)) || (bounds.y < (kernelHeight / 2))
				|| ((bounds.x + bounds.width) > (image.width - (kernelWidth / 2)))
				|| ((bounds.y + bounds.height) > (image.height - (kernelHeight / 2)))) {
			SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelHeight, kernelWidth);
		}
		for (int y = Math.max(kernelHeight / 2, bounds.y); y < Math.min(bounds.y + bounds.height,
				(image.height - (kernelHeight / 2))); ++y) {
			for (int x = Math.max(kernelWidth / 2, bounds.x); x < Math.min(bounds.x + bounds.width,
					(image.width - (kernelWidth / 2))); ++x) {
				int max = Integer.MIN_VALUE;
				loop1: for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); dx++) {
						max = Math.max(max, image.pixels[y + dy][x + dx]);
						if (max >= threshold) {
							break loop1;
						}
					}
				}
				out[y][x] = max;
				// System.out.println("Set out[" + y + "," + x + "] to " + max + " from " +
				// image.pixels[y][x]);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage dilate(SBImage image, int kernelHeight, int kernelWidth) {

		int[][] out = new int[image.height][image.width];
		SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelHeight, kernelWidth);
		for (int y = kernelHeight / 2; y < (image.height - (kernelHeight / 2)); y++) {
			for (int x = kernelWidth / 2; x < (image.width - (kernelWidth / 2)); x++) {
				int min = Integer.MAX_VALUE;
				loop1: for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); dx++) {
						min = Math.min(min, image.pixels[y + dy][x + dx]);
						if (min <= (SBImageUtils.BLACK_PIXEL_REPLACEMENT + 5)) {
							break loop1;
						}
					}
				}
				out[y][x] = min;
				// System.out.println("Set out[" + y + "," + x + "] to " + min + " from " +
				// image.pixels[y][x]);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage dilate(SBImage image, int kernelHeight, int kernelWidth, int threshold) {

		int[][] out = new int[image.height][image.width];
		SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelHeight, kernelWidth);
		for (int y = kernelHeight / 2; y < (image.height - (kernelHeight / 2)); y++) {
			for (int x = kernelWidth / 2; x < (image.width - (kernelWidth / 2)); x++) {
				int min = Integer.MAX_VALUE;
				loop1: for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); dx++) {
						min = Math.min(min, image.pixels[y + dy][x + dx]);
						if (min <= threshold) {
							break loop1;
						}
					}
				}
				out[y][x] = min;
				// System.out.println("Set out[" + y + "," + x + "] to " + min + " from " +
				// image.pixels[y][x]);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage dilate(SBImage image, int kernelHeight, int kernelWidth, int threshold, Rectangle bounds) {

		int[][] out = new int[image.height][image.width];
		if ((bounds.x < (kernelWidth / 2)) || (bounds.y < (kernelHeight / 2))
				|| ((bounds.x + bounds.width) > (image.width - (kernelWidth / 2)))
				|| ((bounds.y + bounds.height) > (image.height - (kernelHeight / 2)))) {
			SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelHeight, kernelWidth);
		}
		for (int y = Math.max(kernelHeight / 2, bounds.y); y < Math.min(bounds.y + bounds.height,
				(image.height - (kernelHeight / 2))); ++y) {
			for (int x = Math.max(kernelWidth / 2, bounds.x); x < Math.min(bounds.x + bounds.width,
					(image.width - (kernelWidth / 2))); ++x) {
				int min = Integer.MAX_VALUE;
				loop1: for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); dx++) {
						min = Math.min(min, image.pixels[y + dy][x + dx]);
						if (min <= threshold) {
							break loop1;
						}
					}
				}
				out[y][x] = min;
				// System.out.println("Set out[" + y + "," + x + "] to " + min + " from " +
				// image.pixels[y][x]);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage dilate(SBImage image, int kernelHeight, int kernelWidth, Rectangle bounds) {

		int[][] out = new int[image.height][image.width];
		if ((bounds.x < (kernelWidth / 2)) || (bounds.y < (kernelHeight / 2))
				|| ((bounds.x + bounds.width) > (image.width - (kernelWidth / 2)))
				|| ((bounds.y + bounds.height) > (image.height - (kernelHeight / 2)))) {
			SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelHeight, kernelWidth);
		}
		for (int y = Math.max(kernelHeight / 2, bounds.y); y < Math.min(bounds.y + bounds.height,
				(image.height - (kernelHeight / 2))); ++y) {
			for (int x = Math.max(kernelWidth / 2, bounds.x); x < Math.min(bounds.x + bounds.width,
					(image.width - (kernelWidth / 2))); ++x) {
				int min = Integer.MAX_VALUE;
				loop1: for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); dx++) {
						min = Math.min(min, image.pixels[y + dy][x + dx]);
						if (min <= (SBImageUtils.BLACK_PIXEL_REPLACEMENT + 5)) {
							break loop1;
						}
					}
				}
				out[y][x] = min;
				// System.out.println("Set out[" + y + "," + x + "] to " + min + " from " +
				// image.pixels[y][x]);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage medianFilter(SBImage image, int kernelHeight, int kernelWidth) {

		int[][] out = new int[image.height][image.width];
		SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelHeight, kernelWidth);
		final int length = kernelWidth * kernelHeight;
		int[] row = new int[length];
		for (int y = kernelHeight / 2; y < (image.height - (kernelHeight / 2)); y++) {
			for (int x = kernelWidth / 2; x < (image.width - (kernelWidth / 2)); x++) {
				for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					System.arraycopy(image.pixels[y + dy], x - (kernelWidth / 2), row,
							(dy + (kernelHeight / 2)) * kernelWidth, kernelWidth);
				}
				Arrays.sort(row);
				out[y][x] = row[length / 2];
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage invert(SBImage image) {

		int[][] out = new int[image.height][image.width];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				out[y][x] = 255 - image.pixels[y][x];
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage invertInPlace(SBImage image) {

		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				image.pixels[y][x] = 255 - image.pixels[y][x];
			}
		}
		return image;
	}

	public static SBImage rmsBlend(SBImage image1, SBImage image2) throws Exception {
		if ((image1.width != image2.width) || (image1.height != image2.height)) {
			throw new Exception("Image dimensions must be equal");
		}
		int[][] out = new int[image1.height][image1.width];
		for (int y = 0; y < image1.height; y++) {
			for (int x = 0; x < image1.width; x++) {
				out[y][x] = (int) (Math.sqrt((image1.pixels[y][x] + 1) * (image2.pixels[y][x] + 1)) - 1);
			}
		}
		return new SBImage(out);
	}

	public static SBImage XOR(SBImage image1, SBImage image2) throws Exception {
		if ((image1.width != image2.width) || (image1.height != image2.height)) {
			throw new Exception("Image dimensions must be equal");
		}
		int[][] out = new int[image1.height][image1.width];
		for (int y = 0; y < image1.height; y++) {
			for (int x = 0; x < image1.width; x++) {
				out[y][x] = (image1.pixels[y][x]) ^ (image2.pixels[y][x]);
			}
		}
		return new SBImage(out);
	}

	public static SBImage xorAndInvert(SBImage image1, SBImage image2) throws Exception {
		if ((image1.width != image2.width) || (image1.height != image2.height)) {
			throw new Exception("Image dimensions must be equal");
		}
		int[][] out = new int[image1.height][image1.width];
		for (int y = 0; y < image1.height; y++) {
			for (int x = 0; x < image1.width; x++) {
				out[y][x] = 255 - ((image1.pixels[y][x]) ^ (image2.pixels[y][x]));
			}
		}
		return new SBImage(out);
	}

	public static SBImage xorAndInvert(SBImage image1, SBImage image2, int blackThreshold, int whiteThreshold)
			throws Exception {
		if ((image1.width != image2.width) || (image1.height != image2.height)) {
			throw new Exception("Image dimensions must be equal");
		}
		int[][] out = new int[image1.height][image1.width];
		for (int y = 0; y < image1.height; y++) {
			for (int x = 0; x < image1.width; x++) {
				final int pix = 255 - ((image1.pixels[y][x]) ^ (image2.pixels[y][x]));
				out[y][x] = pix >= whiteThreshold ? WHITE_PIXEL_REPLACEMENT
						: pix <= blackThreshold ? BLACK_PIXEL_REPLACEMENT : GRAY_PIXEL_REPLACEMENT;
			}
		}
		return new SBImage(out);
	}

	public static SBImage blur(SBImage image, int kernelSize) {
		int[][] out = new int[image.height][image.width];
		SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelSize, kernelSize);
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				float numerator = 0.0f;
				float denominator = 0.0f;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						final int where = Math.max(Math.abs(dy), Math.abs(dx));
						final int weight = ((kernelSize / 2) + 1) - where;
						numerator += weight * image.pixels[y][x];
						denominator += weight;
					}
				}
				out[y][x] = (int) (numerator / denominator);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage minMeanMaxFilter(SBImage image, int kernelSize) {
		int[][] out = new int[image.height][image.width];
		SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelSize, kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				float sum = 0.0f;
				// float variance = 0.0f;
				float sumOfWeights = 0.0f;
				float min = 255f;
				float max = 0.0f;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						final int where = Math.max(Math.abs(dy), Math.abs(dx));
						final int weight = ((kernelSize / 2) + 1) - where;
						sum += weight * image.pixels[y + dy][x + dx];
						// variance += image.pixels[y][x] * image.pixels[y][x];
						sumOfWeights += weight;
						max = Math.max(max, image.pixels[y + dy][x + dx]);
						min = Math.min(min, image.pixels[y + dy][x + dx]);
					}
				}
				out[y][x] = (int) (((min + max + ((2 * sum * 1.0) / sumOfWeights)) * 1.0) / 4);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage anisotropicDiffusion(SBImage image) {
		float lambda = 0.15f;
		int noOfIterations = 60;
		int[][] out = SBImageArrayUtils.arrayCopy(image.pixels);
		int pixelsChanged = Integer.MAX_VALUE;
		int iterationCount = 0;
		int[] row = new int[5];
		int medianDelta = 0;
		float oldPixel = 0;
		loop1: while (iterationCount++ < noOfIterations) {
			pixelsChanged = 0;
			for (int y = 1; y < (image.height - 1); y++) {
				for (int x = 1; x < (image.width - 1); x++) {
					row[0] = out[y - 1][x];
					row[1] = out[y][x];
					row[2] = out[y + 1][x];
					row[3] = out[y][x - 1];
					row[4] = out[y][x + 1];
					Arrays.sort(row);
					medianDelta = row[2] - out[y][x];
					oldPixel = out[y][x];
					// if (medianDelta > 0) {
					out[y][x] = Math.max(0, Math.min(
							out[y][x] + (int) (lambda * negativeETable[Math.abs(medianDelta)] * medianDelta), 255));
					// } else {
					// out[y][x] = Math.max(0, Math.min(
					// out[y][x] + (int) (lambda * negativeETable[Math.abs(medianDelta)] *
					// medianDelta), 255));
					// }
					if (oldPixel != out[y][x]) {
						pixelsChanged++;
					}
				}
			}
			// System.out.println("Pixels changed = " + pixelsChanged);
			if (((pixelsChanged * 100 * 1.0) / (image.height * image.height)) < 0.005) {
				break loop1;
			}
		}
		// System.out.println("Exiting Anisotropic Diffusion after " + iterationCount +
		// " iterations.");
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static void setGraphics2DParameters(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_DEFAULT);
		g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
	}

	/*
	 * void homomorphicFilter(SBImage image) {
	 *
	 * FImage fimage = new FImage(SBImageArrayUtils.iterateFlatten(image.pixels),
	 * image.width, image.height, ARGBPlane.BLUE)); FourierTransformComplex ftc =
	 * new FourierTransformComplex(fimage, true);
	 *
	 * // Convert image to complex image. FourierTransform ft = new
	 * FourierTransform(fastBitmap); ComplexNumber[][] complex = ft.getData(); int
	 * width = fastBitmap.getWidth(); int height = fastBitmap.getHeight();
	 *
	 * // Compute log transform for (int x = 0; x < height; x++) { for (int y = 0; y
	 * < width; y++) { complex[x][y].real = Math.log(complex[x][y].real + 1); } }
	 *
	 * // Forward Fast Fourier Transform ft.setData(complex); ft.Forward();
	 *
	 * // Frequency filter FrequencyFilter freq = new FrequencyFilter(range);
	 * freq.ApplyInPlace(ft);
	 *
	 * // Backward Fourier Transform ft.Backward();
	 *
	 * // Inverse log transform (exponencial) complex = ft.getData(); for (int x =
	 * 0; x < height; x++) { for (int y = 0; y < width; y++) { complex[x][y].real =
	 * Math.exp(complex[x][y].real - 1); } } ft.setData(complex);
	 *
	 * fastBitmap.setImage(ft.toFastBitmap()); } } }
	 */

	public static SBImage clone(SBImage image) {
		if (image != null) {
			return image.clone();
		}
		return null;
	}

	/*
	 * public static SBImage wienerFilter(SBImage image) { try { return new
	 * SBImage(SBImageArrayUtils.arrayCopyDoubleToInt(new
	 * WienerFilter(image.pixels).process())); } catch (InterruptedException ie) { }
	 * return null; }
	 */

	public static SBImage open(SBImage image, int kernelHeight, int kernelWidth) {
		int[][] out = new int[image.height][image.width];
		SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelHeight, kernelWidth);
		for (int y = kernelHeight / 2; y < (image.height - (kernelHeight / 2)); y++) {
			for (int x = kernelWidth / 2; x < (image.width - (kernelWidth / 2)); x++) {
				int max = Integer.MIN_VALUE;
				loop1: for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); dx++) {
						max = Math.max(max, image.pixels[y + dy][x + dx]);
						if (max == 255) {
							break loop1;
						}
					}
				}
				out[y][x] = max;
			}
		}
		for (int y = kernelHeight / 2; y < (image.height - (kernelHeight / 2)); y++) {
			for (int x = kernelWidth / 2; x < (image.width - (kernelWidth / 2)); x++) {
				int min = Integer.MAX_VALUE;
				loop1: for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); dx++) {
						min = Math.min(min, image.pixels[y + dy][x + dx]);
						if (min == 0) {
							break loop1;
						}
					}
				}
				out[y][x] = min;
				// System.out.println("Set out[" + y + "," + x + "] to " + min + " from " +
				// image.pixels[y][x]);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage close(SBImage image, int kernelHeight, int kernelWidth) {
		int[][] out = new int[image.height][image.width];
		SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelHeight, kernelWidth);
		for (int y = kernelHeight / 2; y < (image.height - (kernelHeight / 2)); y++) {
			for (int x = kernelWidth / 2; x < (image.width - (kernelWidth / 2)); x++) {
				int min = Integer.MAX_VALUE;
				loop1: for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); dx++) {
						min = Math.min(min, image.pixels[y + dy][x + dx]);
						if (min == 0) {
							break loop1;
						}
					}
				}
				out[y][x] = min;
				// System.out.println("Set out[" + y + "," + x + "] to " + min + " from " +
				// image.pixels[y][x]);
			}
		}
		for (int y = kernelHeight / 2; y < (image.height - (kernelHeight / 2)); y++) {
			for (int x = kernelWidth / 2; x < (image.width - (kernelWidth / 2)); x++) {
				int max = Integer.MIN_VALUE;
				loop1: for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); dx++) {
						max = Math.max(max, image.pixels[y + dy][x + dx]);
						if (max == 255) {
							break loop1;
						}
					}
				}
				out[y][x] = max;
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage edgeWhitening(SBImage image) {
		return edgeWhitening(image, 5, 5, 0.4);
	}

	public static SBImage edgeWhitening(SBImage image, double tolerance) {
		return edgeWhitening(image, 5, 5, tolerance);
	}

	public static SBImage edgeWhitening(SBImage image, int kernelHeight, int kernelWidth) {
		return edgeWhitening(image, kernelHeight, kernelWidth, 0.4);
	}

	public static SBImage edgeWhitening(SBImage image, int kernelHeight, int kernelWidth, double tolerance) {
		int[][] out = new int[image.height][image.width];
		SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelHeight, kernelWidth);
		int cellCount = kernelHeight * kernelWidth;
		for (int y = kernelHeight / 2; y < (image.height - (kernelHeight / 2)); y++) {
			for (int x = kernelWidth / 2; x < (image.width - (kernelWidth / 2)); x++) {
				int max = Integer.MIN_VALUE;
				int min = Integer.MAX_VALUE;
				int sum = 0;
				int sumSquared = 0;
				for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); dx++) {
						max = Math.max(max, image.pixels[y + dy][x + dx]);
						min = Math.min(min, image.pixels[y + dy][x + dx]);
						sum += image.pixels[y + dy][x + dx];
						sumSquared += image.pixels[y + dy][x + dx] * image.pixels[y + dy][x + dx];
					}
				}
				double mean = (sum * 1.0) / cellCount;
				double stdDev = FastMath.sqrt((sumSquared / cellCount) - (mean * mean));
				if (image.pixels[y][x] > (mean + (tolerance * stdDev))) {
					out[y][x] = 255;
				} else {
					out[y][x] = image.pixels[y][x];
				}
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage contrastImage(SBImage image, int kernelSize) {
		return contrastImage(image, kernelSize, true);
	}

	public static SBImage contrastImage(SBImage image, int kernelSize, boolean relative) {
		// if relative, then use the value at image.pixels[y][x]; else, work with only
		// min & max
		int[][] out = new int[image.height][image.width];
		SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelSize, kernelSize);
		for (int y = kernelSize / 2; y < (image.height - (kernelSize / 2)); y++) {
			for (int x = kernelSize / 2; x < (image.width - (kernelSize / 2)); x++) {
				int max = Integer.MIN_VALUE;
				int min = Integer.MAX_VALUE;
				for (int dy = -kernelSize / 2; dy <= (kernelSize / 2); dy++) {
					for (int dx = -kernelSize / 2; dx <= (kernelSize / 2); dx++) {
						max = Math.max(max, image.pixels[y + dy][x + dx]);
						min = Math.min(min, image.pixels[y + dy][x + dx]);
					}
				}
				int error = (max == 0) ? 1 : 0;
				if (!relative) {
					out[y][x] = ((max - min) * 255) / (max + min + error);
				} else {
					out[y][x] = ((image.pixels[y][x] - min) * 255) / (max + min + error);
				}
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	/*
	 * public static ArrayList<BBox> findBoundingBoxes(SBImage image) throws
	 * Exception { ArrayList<BBox> al = new ArrayList<BBox>(64);
	 * image.populateByteBuffer(); TesseractHandle wrapperHandle = (TesseractHandle)
	 * tesseractPool.borrowObject(); TessBaseAPI handle = wrapperHandle.getHandle();
	 * // System.out.println("Handle = " + handle); //
	 * System.out.println(Arrays.toString(image.byteBuffer.array()));
	 * TessAPI1.TessBaseAPISetImage(handle, image.byteBuffer, image.width,
	 * image.height, image.bytesPerPixel, image.bytesPerLine); ETEXT_DESC monitor =
	 * new ETEXT_DESC(); TessAPI1.TessBaseAPIRecognize(handle, monitor);
	 * TessResultIterator ri = TessAPI1.TessBaseAPIGetIterator(handle);
	 * TessPageIterator pi = TessAPI1.TessResultIteratorGetPageIterator(ri);
	 * TessAPI1.TessPageIteratorBegin(pi); int level =
	 * TessAPI1.TessPageIteratorLevel.RIL_WORD; // int level =
	 * TessAPI1.TessPageIteratorLevel.RIL_TEXTLINE; boolean handleReturned = false;
	 * do { Pointer ptr = TessAPI1.TessResultIteratorGetUTF8Text(ri, level); if (ptr
	 * == null) { // System.out.println("Pointer is null");
	 * tesseractPool.returnObject(wrapperHandle); handleReturned = true; break; }
	 * String word = ptr.getString(0); TessAPI1.TessDeleteText(ptr); float
	 * confidence = TessAPI1.TessResultIteratorConfidence(ri, level); IntBuffer
	 * leftB = IntBuffer.allocate(1); IntBuffer topB = IntBuffer.allocate(1);
	 * IntBuffer rightB = IntBuffer.allocate(1); IntBuffer bottomB =
	 * IntBuffer.allocate(1); TessAPI1.TessPageIteratorBoundingBox(pi, level, leftB,
	 * topB, rightB, bottomB); int left = leftB.get(); int top = topB.get(); int
	 * right = rightB.get(); int bottom = bottomB.get(); al.add(new BBox(new
	 * Rectangle(left, top, right - left, bottom - top), confidence, word)); } while
	 * (TessAPI1.TessPageIteratorNext(pi, level) == TRUE); if (!handleReturned) {
	 * tesseractPool.returnObject(wrapperHandle); } return al; }
	 *
	 */

	/*
	 * public static ArrayList<BBox> findBoundingBoxes(BufferedImage image) throws
	 * Exception { ArrayList<BBox> al = new ArrayList<BBox>(64); ByteBuffer
	 * byteBuffer; int bpp; int bytesPerPixel; int bytesPerLine; if (image.getType()
	 * == BufferedImage.TYPE_BYTE_GRAY) { DataBuffer buff =
	 * image.getRaster().getDataBuffer(); byte[] pixelData = ((DataBufferByte)
	 * buff).getData(); byteBuffer = ByteBuffer.allocateDirect(pixelData.length);
	 * byteBuffer.order(ByteOrder.nativeOrder()); byteBuffer.put(pixelData);
	 * ((Buffer) byteBuffer).flip(); bpp = image.getColorModel().getPixelSize();
	 * bytesPerPixel = bpp / 8; bytesPerLine = (int) Math.ceil((image.getWidth() *
	 * bpp) / 8.0); } else { final BufferedImage temp2 = new
	 * BufferedImage(image.getWidth(), image.getHeight(),
	 * BufferedImage.TYPE_BYTE_GRAY); Graphics2D g = temp2.createGraphics();
	 * g.drawImage(image, 0, 0, null); g.dispose(); DataBuffer buff =
	 * temp2.getRaster().getDataBuffer(); byte[] pixelData = ((DataBufferByte)
	 * buff).getData(); byteBuffer = ByteBuffer.allocateDirect(pixelData.length);
	 * byteBuffer.order(ByteOrder.nativeOrder()); byteBuffer.put(pixelData);
	 * ((Buffer) byteBuffer).flip(); bpp = temp2.getColorModel().getPixelSize();
	 * bytesPerPixel = bpp / 8; bytesPerLine = (int) Math.ceil((image.getWidth() *
	 * bpp) / 8.0); } TesseractHandle handleWrapper = (TesseractHandle)
	 * tesseractPool.borrowObject(); TessBaseAPI handle = handleWrapper.getHandle();
	 * // System.out.format("bpp = %d, bytesPerPixel = %d, bytesPerLine = ", bpp, //
	 * bytesPerPixel, bytesPerLine); // System.out.println("Handle = " + handle); //
	 * System.out.println(Arrays.toString(image.byteBuffer.array()));
	 * TessAPI1.TessBaseAPISetImage(handle, byteBuffer, image.getWidth(),
	 * image.getHeight(), bytesPerPixel, bytesPerLine); ETEXT_DESC monitor = new
	 * ETEXT_DESC(); TessAPI1.TessBaseAPIRecognize(handle, monitor);
	 * TessResultIterator ri = TessAPI1.TessBaseAPIGetIterator(handle); //
	 * System.out.println("Result Iterator = " + ri); TessPageIterator pi =
	 * TessAPI1.TessResultIteratorGetPageIterator(ri); //
	 * System.out.println("Page Iterator = " + pi);
	 * TessAPI1.TessPageIteratorBegin(pi); int level =
	 * TessAPI1.TessPageIteratorLevel.RIL_WORD; // int level =
	 * TessAPI1.TessPageIteratorLevel.RIL_TEXTLINE; boolean handleReturned = false;
	 * do { Pointer ptr = TessAPI1.TessResultIteratorGetUTF8Text(ri, level); if (ptr
	 * == null) { // System.out.println("Pointer is null");
	 * tesseractPool.returnObject(handleWrapper); handleReturned = true; break; }
	 * String word = ptr.getString(0); TessAPI1.TessDeleteText(ptr); float
	 * confidence = TessAPI1.TessResultIteratorConfidence(ri, level); IntBuffer
	 * leftB = IntBuffer.allocate(1); IntBuffer topB = IntBuffer.allocate(1);
	 * IntBuffer rightB = IntBuffer.allocate(1); IntBuffer bottomB =
	 * IntBuffer.allocate(1); TessAPI1.TessPageIteratorBoundingBox(pi, level, leftB,
	 * topB, rightB, bottomB); int left = leftB.get(); int top = topB.get(); int
	 * right = rightB.get(); int bottom = bottomB.get(); al.add(new BBox(new
	 * Rectangle(left, top, right - left, bottom - top), confidence, word));
	 *
	 * } while (TessAPI1.TessPageIteratorNext(pi, level) == TRUE); if
	 * (!handleReturned) { tesseractPool.returnObject(handleWrapper); } return al; }
	 *
	 */

	public static SBImage deskewToSBImage(SBImage input) throws Exception {
		BufferedImage temp1 = SBImage.getBufferedImageFromSBImage(input);
		double rotationAngle = -1 * new DeSkewSBImage(input).getSkewAngle();
		double theta = Math.toRadians(rotationAngle);
		// double sin = Math.abs(Math.sin(theta));
		// double cos = Math.abs(Math.cos(theta));
		// int w = input.width;
		// int h = input.height;
		// int newW = (int) Math.floor((w * cos) + (h * sin));
		// int newH = (int) Math.floor((h * cos) + (w * sin));

		// create the output BufferedImage and set its pixels to white. Ensures that the
		// final image is clean

		// int[] pixelData = new int[input.height * input.width];
		// int whitePixel = ((255 & 0xFF) << 16) | ((255 & 0xFF) << 8) | ((255 & 0xFF)
		// << 0);
		// for (int count = 0; count < pixelData.length; ++count) {
		// pixelData[count] = whitePixel;
		// }
		// tmp.getRaster().setDataElements(0, 0, input.width, input.height, pixelData);

		// BufferedImage tmp = new BufferedImage(rotatedBounds.width,
		// rotatedBounds.height, BufferedImage.TYPE_INT_RGB);
		// Graphics2D g2d = tmp.createGraphics();
		// g2d.setBackground(Color.WHITE);
		// g2d.translate((newW - w) / 2, (newH - h) / 2);
		// g2d.rotate(theta, w / 2, h / 2);
		// g2d.drawImage(temp1, 0, 0, null);
		// g2d.dispose();

		final AffineTransform at = AffineTransform.getRotateInstance(theta, input.width * 0.5, input.height * 0.5);
		final Rectangle rotatedBounds = at.createTransformedShape(new Rectangle(0, 0, input.width, input.height))
				.getBounds();
		final BufferedImage tmp = new BufferedImage(rotatedBounds.width, rotatedBounds.height,
				BufferedImage.TYPE_BYTE_GRAY);
		final Graphics2D g2d = tmp.createGraphics();
		final ExecutorService threadService = Executors.newFixedThreadPool(2);
		CompletableFuture<Boolean> cf1 = CompletableFuture.supplyAsync(() -> {
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			// setbackground and fill have to be called before transform and drawImage !!!!
			g2d.setBackground(Color.WHITE);
			g2d.fillRect(0, 0, rotatedBounds.width, rotatedBounds.height); // needed to fill the new portions with
																			// white,
																			// else they get filled with black which
																			// mucks
																			// up the following parts of the algo
			at.preConcatenate(AffineTransform.getTranslateInstance(-rotatedBounds.x, -rotatedBounds.y));
			g2d.transform(at);
			return Boolean.TRUE;
		}, threadService);

		final int finalImageWidth = ((rotatedBounds.width + horizontalPadding) / 32) * 32;
		final int finalImageHeight = ((rotatedBounds.height + verticalPadding) / 32) * 32;
		final BufferedImage newImage = new BufferedImage(finalImageWidth, finalImageHeight,
				BufferedImage.TYPE_BYTE_GRAY);
		// BufferedImage smallerImage = new BufferedImage(0.5*newImageWidth,
		// newImageHeight,
		// image.underlyingBuffImage.getType());
		final Graphics2D g = newImage.createGraphics();
		CompletableFuture<Boolean> cf2 = CompletableFuture.supplyAsync(() -> {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setBackground(Color.WHITE);
			g.fillRect(0, 0, finalImageWidth, finalImageHeight);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
			return Boolean.TRUE;
		}, threadService);
		CompletableFuture.allOf(cf1, cf2).join();
		threadService.shutdown();
		g2d.drawImage(temp1, 0, 0, null);
		g2d.dispose();
		g.drawImage(tmp, ((finalImageWidth / 2) - (rotatedBounds.width / 2)),
				(finalImageHeight / 2) - (rotatedBounds.height / 2), null);
		g.dispose();

		// System.out.println("About to call getSBImage..()");
		SBImage out = SBImage.getSBImageFromBufferedImage(newImage, false);
		out.deskewed = true;
		out.underlyingBuffImage = newImage;
		return out;
	}

	public static SBImage removeLines(final SBImage deskewed) throws Exception {
		return removeLines(deskewed, 65);
	}

	public static SBImage removeLines(final SBImage deskewed, int kernelSize) throws Exception {

		final ExecutorService threadService = Executors.newFixedThreadPool(2);
		CompletableFuture<SBImage> cf1 = CompletableFuture.supplyAsync(() -> {
			SBImage erodedHorizontal = SBImageUtils.parallelXErode(deskewed, 1, kernelSize,
					WHITE_PIXEL_REPLACEMENT - 2);
			erodedHorizontal = SBImageUtils.parallelXErode(erodedHorizontal, 1, 11, WHITE_PIXEL_REPLACEMENT - 2);
			SBImage dH = SBImageUtils.parallelXDilate(erodedHorizontal, ERODE_DILATE_THICKNESS, 11,
					BLACK_PIXEL_REPLACEMENT + 5);
			return dH = SBImageUtils.parallelXDilate(dH, 1, kernelSize, BLACK_PIXEL_REPLACEMENT + 5);
		}, threadService);

		CompletableFuture<SBImage> cf2 = CompletableFuture.supplyAsync(() -> {
			SBImage newImage = new SBImage(SBImageArrayUtils.transpose(deskewed.pixels), false, false, false, false);
			// try {
			// ImageUtils.writeFile(newImage, "png", "E:\\TechWerx\\Java\\Working\\" +
			// "BB49.png", 300);
			// } catch (Exception e) {
			//
			// }
			SBImage erodedVertical = SBImageUtils.parallelXErode(newImage, 1, kernelSize, WHITE_PIXEL_REPLACEMENT - 2);
			erodedVertical = SBImageUtils.parallelXErode(erodedVertical, 1, 11, WHITE_PIXEL_REPLACEMENT - 2);
			SBImage dV = SBImageUtils.parallelXDilate(erodedVertical, ERODE_DILATE_THICKNESS, 11,
					BLACK_PIXEL_REPLACEMENT + 5);
			dV = SBImageUtils.parallelXDilate(dV, 1, kernelSize, BLACK_PIXEL_REPLACEMENT + 5);
			// try {
			// ImageUtils.writeFile(dV, "png", "E:\\TechWerx\\Java\\Working\\" + "BB50.png",
			// 300);
			// } catch (Exception e) {
			//
			// }
			return new SBImage(SBImageArrayUtils.transpose(dV.pixels), false, false, false, false);
		}, threadService);

		CompletableFuture.allOf(cf1, cf2).join();
		threadService.shutdown();
		// List<SBImage> results =
		// Stream.of(cf2).map(CompletableFuture::join).collect(Collectors.toList());
		// SBImage dilatedHorizontal = results.get(0);
		// SBImage dilatedVertical = results.get(1);
		SBImage dilatedHorizontal = cf1.get();
		SBImage dilatedVertical = cf2.get();

		SBImage out = SBImageUtils.subtract(deskewed, dilatedHorizontal, dilatedVertical);
		// SBImage out = SBImageUtils.subtract(deskewed, dilatedHorizontal,
		// dilatedHorizontal);

		// SBImage out = SBImageUtils.subtractHorizontal(deskewed, dilatedHorizontal);
		// SBImage out = SBImageUtils.subtractVertical(deskewed, dilatedHorizontal);
		out.deskewed = deskewed.deskewed;
		// out.underlyingBuffImage = SBImage.getBufferedImageFromSBImage(out);
		return out;
	}

	public static SBImage removeHorizontalLines(final SBImage deskewed) throws Exception {

		SBImage erodedHorizontal = SBImageUtils.parallelXErode(deskewed, 1, 61, WHITE_PIXEL_REPLACEMENT - 2);
		erodedHorizontal = SBImageUtils.parallelXErode(erodedHorizontal, 1, 11, WHITE_PIXEL_REPLACEMENT - 2);
		SBImage dilatedHorizontal = SBImageUtils.parallelXDilate(erodedHorizontal, 1, 61, BLACK_PIXEL_REPLACEMENT + 5);
		dilatedHorizontal = SBImageUtils.parallelXDilate(dilatedHorizontal, ERODE_DILATE_THICKNESS, 21,
				BLACK_PIXEL_REPLACEMENT + 5);

		SBImage out = SBImageUtils.subtractHorizontal(deskewed, dilatedHorizontal);
		out.deskewed = deskewed.deskewed;
		// out.underlyingBuffImage = SBImage.getBufferedImageFromSBImage(out);
		return out;
	}

	public static SBImage removeVerticalLines(final SBImage deskewed) throws Exception {

		SBImage erodedVertical = SBImageUtils.parallelYErode(deskewed, 41, 1, WHITE_PIXEL_REPLACEMENT - 2);
		erodedVertical = SBImageUtils.parallelYErode(erodedVertical, 21, 1, WHITE_PIXEL_REPLACEMENT - 2);
		SBImage dilatedVertical = SBImageUtils.parallelYDilate(erodedVertical, 41, 1, BLACK_PIXEL_REPLACEMENT + 5);
		dilatedVertical = SBImageUtils.parallelYDilate(dilatedVertical, 21, ERODE_DILATE_THICKNESS,
				BLACK_PIXEL_REPLACEMENT + 5);

		SBImage out = SBImageUtils.subtractVertical(deskewed, dilatedVertical);
		out.deskewed = deskewed.deskewed;
		// out.underlyingBuffImage = SBImage.getBufferedImageFromSBImage(out);
		return out;
	}

	/*
	 * // This method is not needed, as deskewToSBImage which uses AffineTransforms
	 * is just as fast
	 *
	 * public static SBImage deskewToSBImage1(SBImage input) throws Exception {
	 * BufferedImage temp1 = SBImage.getBufferedImageFromSBImage(input); double
	 * rotationAngle = -1 * new DeSkewSBImage(input).getSkewAngle(); double theta =
	 * Math.toRadians(rotationAngle); double sin = Math.abs(Math.sin(theta)); double
	 * cos = Math.abs(Math.cos(theta)); int w = input.width; int h = input.height;
	 * int newW = (int) Math.floor((w * cos) + (h * sin)); int newH = (int)
	 * Math.floor((h * cos) + (w * sin));
	 *
	 * BufferedImage tmp = new BufferedImage(newW, newH,
	 * BufferedImage.TYPE_BYTE_GRAY); Graphics2D g2d = tmp.createGraphics();
	 * g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
	 * RenderingHints.VALUE_INTERPOLATION_BICUBIC); g2d.setBackground(Color.WHITE);
	 * g2d.fillRect(0, 0, newW, newH); g2d.translate((newW - w) / 2, (newH - h) /
	 * 2); g2d.rotate(theta, w / 2, h / 2); g2d.drawImage(temp1, 0, 0, null);
	 * g2d.dispose(); // System.out.println("About to call getSBImage..()"); SBImage
	 * out = SBImage.getSBImageFromBufferedImage(tmp, false); out.deskewed = true;
	 * out.underlyingBuffImage = tmp; return out; }
	 */

	public static BufferedImage deskewToBufferedImage(SBImage input) throws Exception {
		BufferedImage temp1 = SBImage.getBufferedImageFromSBImage(input);
		double rotationAngle = -1 * new DeSkewSBImage(input).getSkewAngle();
		double theta = Math.toRadians(rotationAngle);
		// double sin = Math.abs(Math.sin(theta));
		// double cos = Math.abs(Math.cos(theta));
		// int w = input.width;
		// int h = input.height;
		// int newW = (int) Math.floor((w * cos) + (h * sin));
		// int newH = (int) Math.floor((h * cos) + (w * sin));

		// create the output BufferedImage and set its pixels to white. Ensures that the
		// final image is clean

		// int[] pixelData = new int[input.height * input.width];
		// int whitePixel = ((255 & 0xFF) << 16) | ((255 & 0xFF) << 8) | ((255 & 0xFF)
		// << 0);
		// for (int count = 0; count < pixelData.length; ++count) {
		// pixelData[count] = whitePixel;
		// }
		// tmp.getRaster().setDataElements(0, 0, input.width, input.height, pixelData);

		// BufferedImage tmp = new BufferedImage(rotatedBounds.width,
		// rotatedBounds.height, BufferedImage.TYPE_INT_RGB);
		// Graphics2D g2d = tmp.createGraphics();
		// g2d.setBackground(Color.WHITE);
		// g2d.translate((newW - w) / 2, (newH - h) / 2);
		// g2d.rotate(theta, w / 2, h / 2);
		// g2d.drawImage(temp1, 0, 0, null);
		// g2d.dispose();

		AffineTransform at = AffineTransform.getRotateInstance(theta, input.width * 0.5, input.height * 0.5);
		Rectangle rotatedBounds = at.createTransformedShape(new Rectangle(0, 0, input.width, input.height)).getBounds();
		BufferedImage tmp = new BufferedImage(rotatedBounds.width, rotatedBounds.height, BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g2d = tmp.createGraphics();

		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		// setbackground and fill have to be called before transform and drawImage !!!!
		g2d.setBackground(Color.WHITE);
		g2d.fillRect(0, 0, rotatedBounds.width, rotatedBounds.height); // needed to fill the new portions with white,
																		// else they get filled with black which mucks
																		// up the following parts of the algo
		at.preConcatenate(AffineTransform.getTranslateInstance(-rotatedBounds.x, -rotatedBounds.y));
		g2d.transform(at);
		g2d.drawImage(temp1, 0, 0, null);
		g2d.dispose();

		int finalImageWidth = ((rotatedBounds.width + horizontalPadding) / 32) * 32;
		int finalImageHeight = ((rotatedBounds.height + verticalPadding) / 32) * 32;
		BufferedImage newImage = new BufferedImage(finalImageWidth, finalImageHeight, BufferedImage.TYPE_BYTE_GRAY);
		// BufferedImage smallerImage = new BufferedImage(0.5*newImageWidth,
		// newImageHeight,
		// image.underlyingBuffImage.getType());
		Graphics2D g = newImage.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setBackground(Color.WHITE);
		g.fillRect(0, 0, finalImageWidth, finalImageHeight);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
		g.drawImage(tmp, ((finalImageWidth / 2) - (rotatedBounds.width / 2)),
				(finalImageHeight / 2) - (rotatedBounds.height / 2), null);
		g.dispose();

		return newImage;
	}

	/*
	 * // This method is not needed, as deskewToSBImage which uses AffineTransforms
	 * // is just as fast
	 *
	 * public static BufferedImage deskewToBufferedImage1(SBImage input) throws
	 * Exception { BufferedImage temp1 = SBImage.getBufferedImageFromSBImage(input);
	 * double rotationAngle = -1 * new DeSkewSBImage(input).getSkewAngle(); double
	 * theta = Math.toRadians(rotationAngle); double sin =
	 * Math.abs(Math.sin(theta)); double cos = Math.abs(Math.cos(theta)); int w =
	 * input.width; int h = input.height; int newW = (int) Math.floor((w * cos) + (h
	 * * sin)); int newH = (int) Math.floor((h * cos) + (w * sin));
	 *
	 * BufferedImage tmp = new BufferedImage(newW, newH,
	 * BufferedImage.TYPE_BYTE_GRAY); Graphics2D g2d = tmp.createGraphics();
	 * g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
	 * RenderingHints.VALUE_INTERPOLATION_BICUBIC); g2d.setBackground(Color.WHITE);
	 * g2d.fillRect(0, 0, newW, newH); g2d.translate((newW - w) / 2, (newH - h) /
	 * 2); g2d.rotate(theta, w / 2, h / 2); g2d.drawImage(temp1, 0, 0, null);
	 * g2d.dispose(); return tmp; }
	 *
	 */

	public static ArrayList<ArrayList<SBImage>> populateBoundingBoxes(final ArrayList<ArrayList<SBImage>> images) {
		return populateBoundingBoxes(images, 10);
	}

	public static ArrayList<ArrayList<SBImage>> populateBoundingBoxes(final ArrayList<ArrayList<SBImage>> images,
			int heightCutOff) {
		final ExecutorService threadService = Executors.newFixedThreadPool(Math.min(images.size(), 30));
		final ArrayList<CompletableFuture<Boolean>> cfs = new ArrayList<CompletableFuture<Boolean>>(images.size());
		for (ArrayList<SBImage> subImageArray : images) {
			cfs.add(CompletableFuture.supplyAsync(() -> {
				final ArrayList<CompletableFuture<Boolean>> subCFS = new ArrayList<CompletableFuture<Boolean>>(
						subImageArray.size());
				for (SBImage subImage : subImageArray) {
					subCFS.add(CompletableFuture.supplyAsync(() -> {
						SBImageUtils.populateBBoxes(subImage, heightCutOff);
						return Boolean.TRUE;
					}));
				}
				if (subCFS.size() > 0) {
					CompletableFuture.allOf(subCFS.toArray(new CompletableFuture[subImageArray.size()])).join();
				}
				return Boolean.TRUE;
			}, threadService));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[images.size()])).join();
		threadService.shutdown();
		return images;
	}

	public static SBImage populateBBoxes(SBImage image) {
		return populateBBoxes(image, 10, MEDIUM_SIZE);
	}

	public static SBImage populateBBoxes(SBImage image, int smallMediumBigSize) {
		return populateBBoxes(image, 10, smallMediumBigSize);
	}

	public static SBImage populateBBoxes(SBImage image, int heightCutOff, int smallMediumBigSize) {

		int dilationPixels = 35;
		SBImage sbi1 = SBImageUtils.parallelXDilate(image, 1, dilationPixels, SBImageUtils.BLACK_PIXEL_REPLACEMENT + 5);
		// SBImage sbi2 = SBImageUtils.parallelYDilate(sbi1, 3, 1,
		// SBImageUtils.BLACK_PIXEL_REPLACEMENT + 5);
		// sbi1 = SBImageUtils.parallelXDilate(sbi2, 1, 3,
		// SBImageUtils.BLACK_PIXEL_REPLACEMENT + 5);
		ArrayList<Integer> yCoords = SBImageUtils.getYCoordinatesForTextRegions(sbi1);
		ArrayList<Integer> xCoords = SBImageUtils.getXCoordinatesForTextRegions(sbi1, smallMediumBigSize);
		ArrayList<BBox> bb = new ArrayList<BBox>();
		int yPadding = 0;
		int y1 = yCoords.get(0);
		int height = Math.min((yCoords.get(yCoords.size() - 1) - y1) + yPadding, image.height);
		int widthAdjustment = (int) ((dilationPixels * 2.0) / 4);
		int x1Adjustment = (int) ((dilationPixels * 1.0) / 4);
		if (height < heightCutOff) {
			image.boundingBoxes = bb;
			return image;
		}
		for (int x = 0; x < xCoords.size(); x = x + 2) {
			if (x == (xCoords.size() - 1)) {
				break;
			}
			int x1 = xCoords.get(x);
			int width = (xCoords.get(x + 1) - x1) + 1;
			bb.add(new BBox(
					new Rectangle(x1 + x1Adjustment, (y1 + 2) - (yPadding / 2), width - widthAdjustment, height), 0f,
					null));
		}
		image.boundingBoxes = bb;
		System.out.println(bb);
		return image;
	}

	public static ArrayList<Integer> getXCoordinatesForTextRegions(SBImage image) {
		return getXCoordinatesForTextRegions(image, MEDIUM_SIZE);
	}

	public static ArrayList<Integer> getXCoordinatesForTextRegions(SBImage image, int smallMediumBigSize) {
		ArrayList<Integer> boundaries = new ArrayList<Integer>();
		// boolean firstIsWhite = true;
		boolean inWhiteZone = true;
		// boolean inBlackZone = false;
		// boolean changed = false;
		int runningWhiteCount = 0;
		int runningBlackCount = 0;
		int blackLinesThreshold = 4;
		int whiteLinesThreshold = 4;
		if (smallMediumBigSize == SMALL_SIZE) {
			blackLinesThreshold = 2;
			whiteLinesThreshold = 2;
		}
		if (smallMediumBigSize == BIG_SIZE) {
			blackLinesThreshold = 6;
			whiteLinesThreshold = 6;
		}
		final double blackPixelsPercentCutoff = 0.015;
		outerloop: for (int x = 0; x < image.width; ++x) {
			int blackPixelsCount = 0;
			// changed = false;
			for (int y = 0; y < image.height; ++y) {
				if (image.pixels[y][x] <= (SBImageUtils.BLACK_PIXEL_REPLACEMENT + 1)) {
					++blackPixelsCount;
				}
				// if row has sufficient black pixels (character ends)
				if ((blackPixelsCount) > (image.height * blackPixelsPercentCutoff)) {
					++runningBlackCount;
					if (inWhiteZone) {
						++runningWhiteCount;
					} else {
						runningWhiteCount = 0;
					}
					if ((inWhiteZone) && (runningBlackCount >= blackLinesThreshold)) {
						boundaries.add(Math.max(0, x - blackLinesThreshold));
						inWhiteZone = false;
						runningWhiteCount = 0;
						// inBlackZone = true;
					}
					// if (y == 0) {
					// firstIsWhite = false;
					// }
					continue outerloop;
				}
			} // end of x-loop
			++runningWhiteCount;
//			if (!inWhiteZone) {
//				++runningBlackCount;
//			} else {
//				runningBlackCount = 0;
//			}
			++runningBlackCount;
			if (inWhiteZone) {
				continue outerloop;
			} else {
				if (runningWhiteCount >= whiteLinesThreshold) {
					inWhiteZone = true;
					// inBlackZone = false;
					boundaries.add(Math.max(0, x - whiteLinesThreshold));
					runningBlackCount = 0;
				} else {
					continue outerloop;
				}
			} // end of y-loop
		}
		// System.out.println(boundaries);
		return boundaries;
	}

	public static ArrayList<Integer> getYCoordinatesForTextRegions(SBImage deskewedImage) {
		// returns y Coordinates that are tightly aligned to the top and bottom of a
		// line i.e. No gaps between y coordinate and the top / bottom of a line
		ArrayList<Integer> boundaries = new ArrayList<Integer>();
		// boolean firstIsWhite = true;
		boolean inWhiteZone = true;
		// boolean inBlackZone = false;
		// boolean changed = false;
		int runningWhiteCount = 0;
		int runningBlackCount = 0;
		final int blackLinesThreshold = 5;
		final int whiteLinesThreshold = 5;
		final double blackPixelsPercentCutoff = 0.0075;
		outerloop: for (int y = 0; y < deskewedImage.height; ++y) {
			int blackPixelsCount = 0;
			// changed = false;
			for (int x = 0; x < deskewedImage.width; ++x) {
				if (deskewedImage.pixels[y][x] <= (SBImageUtils.BLACK_PIXEL_REPLACEMENT + 1)) {
					++blackPixelsCount;
				}
				// if row has sufficient black pixels (character ends)
				if ((blackPixelsCount) > (deskewedImage.width * blackPixelsPercentCutoff)) {
					++runningBlackCount;
					if (inWhiteZone) {
						++runningWhiteCount;
					} else {
						runningWhiteCount = 0;
					}
					if ((inWhiteZone) && (runningBlackCount >= blackLinesThreshold)) {
						boundaries.add(Math.max(0, y - blackLinesThreshold));
						inWhiteZone = false;
						runningWhiteCount = 0;
						// inBlackZone = true;
					}
					// if (y == 0) {
					// firstIsWhite = false;
					// }
					continue outerloop;
				}
			} // end of x-loop
			++runningWhiteCount;
//			if (!inWhiteZone) {
//				++runningBlackCount;
//			} else {
//				runningBlackCount = 0;
//			}
			++runningBlackCount;
			if (inWhiteZone) {
				continue outerloop;
			} else {
				if (runningWhiteCount >= whiteLinesThreshold) {
					inWhiteZone = true;
					// inBlackZone = false;
					boundaries.add(Math.max(0, y - whiteLinesThreshold));
					runningBlackCount = 0;
				} else {
					continue outerloop;
				}
			} // end of y-loop
		}
		// System.out.println(boundaries);
		return boundaries;
	}

	public static ArrayList<Rectangle> getBoundariesForTextRegions(SBImage image) {
		return getBoundariesForTextRegions(image, 10);
	}

	public static ArrayList<Rectangle> getBoundariesForTextRegions(SBImage image, int heightCutOff) {
		// returns rectangles with a minimal gap of 2 between the rectangle top and top
		// of line, as well as a gap of 2 between the bottom of the line & the bottom of
		// the rectangle
		if (!image.deskewed) {
			try {
				image = SBImageUtils.deskewToSBImage(image);
			} catch (Exception e) {
				// do nothing
			}
		}
		ArrayList<Integer> boundaries = getYCoordinatesForTextRegions(image);

		int topAndBottomGap = 2;
		ArrayList<Rectangle> rectangles = new ArrayList<Rectangle>();
		int yTop = 0, yBot = 0;
		for (int i = 0; i < boundaries.size(); i = i + 2) {
			yTop = Math.max(0, boundaries.get(i) - topAndBottomGap);
			yBot = Math.min(image.height - 1, boundaries.get(i + 1) + topAndBottomGap);
			if (((yBot - yTop) + 1) >= heightCutOff) {
				rectangles.add(new Rectangle(0, yTop, image.width, (yBot - yTop) + 1));
			}
		}
		// System.out.println(rectangles);
		return rectangles;
	}

	public static ArrayList<SBImage> getSBImagesForTextRegions(SBImage image) {
		return getSBImagesForTextRegions(image, 10);
	}

	public static ArrayList<SBImage> getSBImagesForTextRegions(SBImage image, int heightCutOff) {
		// returns an SBImage array with a gap of 6 between the rectangle top
		// and top of line, as well as a gap of 6 between the bottom of the line & the
		// bottom of the rectangle

		if (!image.deskewed) {
			try {
				image = SBImageUtils.deskewToSBImage(image);
			} catch (Exception e) {
				// do nothing
			}
		}
		ArrayList<Integer> boundaries = getYCoordinatesForTextRegions(image);
		int topAndBottomGap = 2;
		ArrayList<SBImage> sbImages = new ArrayList<SBImage>();
		int yTop = 0, yBot = 0;
		for (int i = 0; i < boundaries.size(); i = i + 2) {
			yTop = Math.max(0, boundaries.get(i) - topAndBottomGap);
			yBot = Math.min(image.height - 1, boundaries.get(i + 1) + topAndBottomGap);
			if (((yBot - yTop) + 1) >= heightCutOff) {
				// System.out.println("Adding a rectangle with height = " + ((yBot - yTop) +
				// 1));
				SBImage extractedImage = new SBImage(
						SBImage.extractRectangle(image.pixels, 0, yTop, (yBot - yTop) + 1, image.width), false, false,
						false, false);
				sbImages.add(extractedImage);
				// try {
				// ImageUtils.writeFile(extractedImage, "png", baseTestDir + "EI" + i + ".png",
				// 300);
				// } catch (Exception e) {
				// }
			}

			// else {
			// System.out.println("Overlooked a y-Boundary pair");
			// }
		}
		// System.out.println(rectangles);
		return sbImages;
	}

	public static ArrayList<SBImage> getImagesforTextRegions(SBImage image) {
		return getImagesforTextRegions(image);
	}

	// public static ArrayList<ArrayList<BufferedImage>>
	// getImagesforTextRegions(SBImage image) {
	public static ArrayList<SBImage> getSBAndBIImagesforTextRegions(SBImage image, int heightCutOff) {
		if (!image.deskewed) {
			try {
				image = SBImageUtils.deskewToSBImage(image);
			} catch (Exception e) {
				// do nothing
			}
		}
		final ArrayList<Integer> boundaries = getYCoordinatesForTextRegions(image);

		// ArrayList<ArrayList<BufferedImage>> imageRectangles = new
		// ArrayList<ArrayList<BufferedImage>>();
		// ArrayList<BufferedImage> imageRow;
		// ArrayList<ArrayList<BufferedImage>> imageRectangles = new
		// ArrayList<ArrayList<BufferedImage>>();
		final ArrayList<SBImage> imageRectangles = new ArrayList<SBImage>();
		final ArrayList<CompletableFuture<Object>> cfs = new ArrayList<>(boundaries.size());
		final ExecutorService threadService = Executors.newFixedThreadPool(Math.min(boundaries.size(), 30));
		for (int i = 0; i < boundaries.size(); i = i + 2) {
			/*
			 * // This check is not needed due to the try-catch below if (i ==
			 * (boundaries.size() - 1)) { break; }
			 */ final int count = i;
			final SBImage image1 = image;
			cfs.add(CompletableFuture.supplyAsync(() -> {
				final int yTop = Math.max(0, boundaries.get(count) - 2);
				boolean exceptionRaised = false;
				int tempNumber = 0;
				try {
					tempNumber = boundaries.get(count + 1);
					exceptionRaised = false;
				} catch (Exception e) {
					exceptionRaised = true;
				}
				final int yBot = exceptionRaised ? image1.height - 1 : Math.min(image1.height - 1, tempNumber + 2);
				if (((yBot - yTop) + 1) < heightCutOff) {
					return Boolean.FALSE;
				}
				final int newImageHeight = (((yBot - yTop) + verticalPadding) / 32) * 32; // preparing for EAST which
																							// needs
																							// dimensions
				// that are multiples of 32
				final int newImageWidth = ((image1.width + horizontalPadding) / 32) * 32; // preparing for EAST which
																							// needs
																							// dimensions
				// that are multiples of 32

				final BufferedImage childImage = image1.underlyingBuffImage.getSubimage(0, yTop, image1.width,
						(yBot - yTop) + 1);
				final BufferedImage newImage = new BufferedImage(newImageWidth, newImageHeight,
						image1.underlyingBuffImage.getType());
				// BufferedImage smallerImage = new BufferedImage(0.5*newImageWidth,
				// newImageHeight,
				// image.underlyingBuffImage.getType());
				final Graphics2D g2d = newImage.createGraphics();
				g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				g2d.setBackground(Color.WHITE);
				g2d.fillRect(0, 0, newImageWidth, newImageHeight);
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				g2d.drawImage(childImage, ((newImageWidth / 2) - (image1.width / 2)),
						(newImageHeight / 2) - (((yBot - yTop) + 1) / 2), null);
				g2d.dispose();
				SBImage outImage = null;
				try {
					outImage = SBImage.getSBImageFromBufferedImage(newImage, false, false);
				} catch (InterruptedException ie) {
				}
				outImage.underlyingBuffImage = newImage;
				DataBuffer buffOut = newImage.getRaster().getDataBuffer();
				byte[] pixelDataOut = ((DataBufferByte) buffOut).getData();
				outImage.byteBuffer = ByteBuffer.allocateDirect(pixelDataOut.length);
				outImage.byteBuffer.order(ByteOrder.nativeOrder());
				outImage.byteBuffer.put(pixelDataOut);
				((Buffer) outImage.byteBuffer).flip();
				int bppOut = newImage.getColorModel().getPixelSize();
				outImage.bytesPerPixel = bppOut / 8;
				outImage.bytesPerLine = (int) Math.ceil((outImage.width * bppOut) / 8.0);

				// imageRow = new ArrayList<BufferedImage>();
				/*
				 * final WritableRaster mainRaster = image.underlyingBuffImage.getRaster();
				 * final Raster childRaster = mainRaster.createChild(0, yTop, image.width, (yBot
				 * - yTop) + 1, 0, 0, null); final BufferedImage newImage = new
				 * BufferedImage(image.width, (yBot - yTop) + 1,
				 * image.underlyingBuffImage.getType()); newImage.setData(childRaster);
				 *
				 */

				// final int[][] data2D = SBImage.extractRectangle(image.pixels, 0, yTop,
				// (yBot - yTop) + 1, image.width );
				// final int[] data1D = SBImageArrayUtils.iterateFlatten(data2D);
				// final BufferedImage newImage = new BufferedImage(image.width, (yBot - yTop) +
				// 1,
				// BufferedImage.TYPE_INT_RGB);
				// newImage.getRaster().setDataElements(0, 0, image.width, (yBot - yTop) + 1,
				// data1D);
				// imageRow
				imageRectangles.add(outImage);
				return Boolean.TRUE;
			}, threadService));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		threadService.shutdown();
		return imageRectangles;
	}

	public static ArrayList<ArrayList<SBImage>> getScaledImagesforTextRegions(SBImage image) throws Exception {
		return getScaledImagesforTextRegions(image, 10);
	}

	public static ArrayList<ArrayList<SBImage>> getScaledImagesforTextRegions(SBImage image, int heightCutOff)
			throws Exception {
		if (!image.deskewed) {
			try {
				image = SBImageUtils.deskewToSBImage(image);
			} catch (Exception e) {
				// do nothing
			}
		}

		final ArrayList<Integer> boundaries = getYCoordinatesForTextRegions(image);
		final ArrayList<ArrayList<SBImage>> imageRectangles = new ArrayList<ArrayList<SBImage>>();
		ArrayList<CompletableFuture<Boolean>> cfs = new ArrayList<CompletableFuture<Boolean>>(boundaries.size());
		final ExecutorService threadService = Executors.newFixedThreadPool(Math.min(3 * boundaries.size(), 30));
		for (int i = 0; i < boundaries.size(); i = i + 2) {
			/*
			 * // This check is not needed due to the try-catch below if (i ==
			 * (boundaries.size() - 1)) { break; }
			 */
			final int counter = i;
			final SBImage image1 = image;
			cfs.add(CompletableFuture.supplyAsync(() -> {
				final int yTop = Math.max(0, boundaries.get(counter) - 2);
				int yBot = 0;
				try {
					final int tempNumber = boundaries.get(counter + 1);
					yBot = Math.min(image1.height - 1, tempNumber + 2);
				} catch (Exception e) {
					yBot = image1.height - 1;
				}

				if (((yBot - yTop) + 1) < heightCutOff) {
					return Boolean.FALSE;
				}

				final int newImageHeight = (((yBot - yTop) + verticalPadding) / 32) * 32; // prepping for EAST
				final int newImageWidth = ((image1.width + horizontalPadding) / 32) * 32; // prepping for EAST

				final ArrayList<SBImage> imageRow = new ArrayList<SBImage>();

				final BufferedImage childImage = image1.underlyingBuffImage.getSubimage(0, yTop, image1.width,
						(yBot - yTop) + 1);
				final BufferedImage newImage = new BufferedImage(newImageWidth, newImageHeight,
						image1.underlyingBuffImage.getType());
				// BufferedImage smallerImage = new BufferedImage(0.5*newImageWidth,
				// newImageHeight,
				// image.underlyingBuffImage.getType());
				Graphics2D g2d = newImage.createGraphics();
				g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2d.setBackground(Color.WHITE);
				g2d.fillRect(0, 0, newImageWidth, newImageHeight);
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				g2d.drawImage(childImage, ((newImageWidth / 2) - (image1.width / 2)),
						(newImageHeight / 2) - (((yBot - yTop) + 1) / 2), null);
				g2d.dispose();
				SBImage outImage = null;
				try {
					outImage = SBImage.getSBImageFromBufferedImage(newImage, false, false);
				} catch (InterruptedException ie) {
				}
				outImage.underlyingBuffImage = newImage;
				DataBuffer buffOut = newImage.getRaster().getDataBuffer();
				byte[] pixelDataOut = ((DataBufferByte) buffOut).getData();
				outImage.byteBuffer = ByteBuffer.allocateDirect(pixelDataOut.length);
				outImage.byteBuffer.order(ByteOrder.nativeOrder());
				outImage.byteBuffer.put(pixelDataOut);
				((Buffer) outImage.byteBuffer).flip();
				int bppOut = newImage.getColorModel().getPixelSize();
				outImage.bytesPerPixel = bppOut / 8;
				outImage.bytesPerLine = (int) Math.ceil((outImage.width * bppOut) / 8.0);

				CompletableFuture<SBImage> cf1 = CompletableFuture.supplyAsync(() -> {
					int scaledDownWidth = (((int) ((newImage.getWidth() * scaleDown) + (horizontalPadding / 2))) / 32)
							* 32; // preparing
									// for
									// EAST
					// detection
					int scaledDownHeight = (((int) ((newImage.getHeight() * scaleDown) + (verticalPadding / 2))) / 32)
							* 32; // preparing
									// for
									// EAST
					// detection
					BufferedImage scaledDownImage = new BufferedImage(scaledDownWidth, scaledDownHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledDownImage.createGraphics();
					gsd.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
					gsd.drawImage(newImage, 0, 0, scaledDownWidth, scaledDownHeight, null);
					gsd.dispose();
					SBImage out = null;
					try {
						out = SBImage.getSBImageFromBufferedImage(scaledDownImage, false, false);
					} catch (InterruptedException ie) {

					}
					out.underlyingBuffImage = scaledDownImage;
					DataBuffer buff = scaledDownImage.getRaster().getDataBuffer();
					byte[] pixelData = ((DataBufferByte) buff).getData();
					out.byteBuffer = ByteBuffer.allocateDirect(pixelData.length);
					out.byteBuffer.order(ByteOrder.nativeOrder());
					out.byteBuffer.put(pixelData);
					((Buffer) out.byteBuffer).flip();
					int bpp = scaledDownImage.getColorModel().getPixelSize();
					out.bytesPerPixel = bpp / 8;
					out.bytesPerLine = (int) Math.ceil((out.width * bpp) / 8.0);
					return out;
				}, threadService);

				CompletableFuture<SBImage> cf2 = CompletableFuture.supplyAsync(() -> {
					int scaledUpWidth = (((int) ((newImage.getWidth() * scaleUp) + (horizontalPadding / 2))) / 32) * 32; // preparing
																															// for
																															// EAST
					// detection
					int scaledUpHeight = (((int) ((newImage.getHeight() * scaleUp) + (verticalPadding / 2))) / 32) * 32; // preparing
																															// for
																															// EAST
					// detection
					BufferedImage scaledUpImage = new BufferedImage(scaledUpWidth, scaledUpHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsu = scaledUpImage.createGraphics();
					gsu.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
					gsu.drawImage(newImage, 0, 0, scaledUpWidth, scaledUpHeight, null);
					gsu.dispose();
					SBImage out = null;
					try {
						out = SBImage.getSBImageFromBufferedImage(scaledUpImage, false, false);
					} catch (InterruptedException ie) {

					}
					out.underlyingBuffImage = scaledUpImage;
					DataBuffer buff = scaledUpImage.getRaster().getDataBuffer();
					byte[] pixelData = ((DataBufferByte) buff).getData();
					out.byteBuffer = ByteBuffer.allocateDirect(pixelData.length);
					out.byteBuffer.order(ByteOrder.nativeOrder());
					out.byteBuffer.put(pixelData);
					((Buffer) out.byteBuffer).flip();
					int bpp = scaledUpImage.getColorModel().getPixelSize();
					out.bytesPerPixel = bpp / 8;
					out.bytesPerLine = (int) Math.ceil((out.width * bpp) / 8.0);
					return out;
				}, threadService);
				CompletableFuture.allOf(cf1, cf2).join();

				try {
					imageRow.add(cf1.get());
				} catch (Exception ee) {
				}
				imageRow.add(outImage);
				try {
					imageRow.add(cf2.get());
				} catch (Exception ee) {
				}

				// imageRow = new ArrayList<BufferedImage>();
				/*
				 * final WritableRaster mainRaster = image.underlyingBuffImage.getRaster();
				 * final Raster childRaster = mainRaster.createChild(0, yTop, image.width, (yBot
				 * - yTop) + 1, 0, 0, null); final BufferedImage newImage = new
				 * BufferedImage(image.width, (yBot - yTop) + 1,
				 * image.underlyingBuffImage.getType()); newImage.setData(childRaster);
				 *
				 */

				// final int[][] data2D = SBImage.extractRectangle(image.pixels, 0, yTop,
				// (yBot - yTop) + 1, image.width);
				// final int[] data1D = SBImageArrayUtils.iterateFlatten(data2D);
				// final BufferedImage newImage = new BufferedImage(image.width, (yBot - yTop) +
				// 1,
				// BufferedImage.TYPE_INT_RGB);
				// newImage.getRaster().setDataElements(0, 0, image.width, (yBot - yTop) + 1,
				// data1D);
				imageRectangles.add(imageRow);
				return Boolean.TRUE;
			}, threadService));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		threadService.shutdown();
		return imageRectangles;
	}

	public static ArrayList<ArrayList<SBImage>> getImagesforOCR(SBImage image) throws Exception {
		return getImagesforOCR(image, 10);
	}

	public static ArrayList<ArrayList<SBImage>> getImagesforOCR(SBImage image, final int heightCutOff)
			throws Exception {
		if (!image.deskewed) {
			try {
				image = SBImageUtils.deskewToSBImage(image);
			} catch (Exception e) {
				// do nothing
			}
		}

		final ArrayList<SBImage> subImages = getSBImagesForTextRegions(image);
		final ArrayList<ArrayList<SBImage>> imageRectangles = new ArrayList<ArrayList<SBImage>>();
		ArrayList<CompletableFuture<Boolean>> cfs = new ArrayList<CompletableFuture<Boolean>>(subImages.size());
		final ExecutorService threadService = Executors.newFixedThreadPool(Math.min(5 * subImages.size(), 50));
		for (final SBImage subImage : subImages) {
			cfs.add(CompletableFuture.supplyAsync(() -> {
				if (subImage.height < heightCutOff) {
					return Boolean.FALSE;
				}
				final int dimensionIncrement = 60;
				final ArrayList<SBImage> imageRow = new ArrayList<SBImage>();

				final int imageHeight1 = (subImage.height + dimensionIncrement);
				final int imageWidth1 = (subImage.width + dimensionIncrement);
				final SBImage image1 = overwrite(createWhiteSBImage(imageHeight1, imageWidth1), subImage,
						dimensionIncrement / 2, dimensionIncrement / 2);
				image1.underlyingBuffImage = SBImage.getByteBufferedImageFromSBImage(image1);
				CompletableFuture<SBImage> cf1 = CompletableFuture.supplyAsync(() -> {
					populateBBoxes(image1, heightCutOff);
					DataBuffer buffOut = image1.underlyingBuffImage.getRaster().getDataBuffer();
					byte[] pixelDataOut = ((DataBufferByte) buffOut).getData();
					image1.byteBuffer = ByteBuffer.allocateDirect(pixelDataOut.length);
					image1.byteBuffer.order(ByteOrder.nativeOrder());
					image1.byteBuffer.put(pixelDataOut);
					((Buffer) image1.byteBuffer).flip();
					int bppOut = image1.underlyingBuffImage.getColorModel().getPixelSize();
					image1.bytesPerPixel = bppOut / 8;
					image1.bytesPerLine = (int) Math.ceil((image1.width * bppOut) / 8.0);
					return image1;
				}, threadService);

				/*
				 * CompletableFuture<SBImage> cf2 = CompletableFuture.supplyAsync(() -> {
				 * SBImage eroded = SBImageUtils.erode(image1, 3, 3); BufferedImage erodedBI =
				 * SBImage.getBufferedImageFromSBImage(eroded); double scaleFactor = 18.0 /
				 * (subImage.height + 2); int scaledDownWidth = (int) (eroded.width *
				 * scaleFactor); int scaledDownHeight = (int) (eroded.height * scaleFactor);
				 * BufferedImage scaledDownImage = new BufferedImage(scaledDownWidth,
				 * scaledDownHeight, BufferedImage.TYPE_BYTE_GRAY); Graphics2D gsd =
				 * scaledDownImage.createGraphics(); setGraphics2DParameters(gsd);
				 * gsd.drawImage(erodedBI, 0, 0, scaledDownWidth, scaledDownHeight, null);
				 * gsd.dispose(); SBImage out =
				 * SBImage.getSBImageFromBufferedImageNoParallelise(scaledDownImage, false,
				 * false); populateBBoxes(out, heightCutOff); out.underlyingBuffImage =
				 * scaledDownImage; DataBuffer buff =
				 * scaledDownImage.getRaster().getDataBuffer(); byte[] pixelData =
				 * ((DataBufferByte) buff).getData(); out.byteBuffer =
				 * ByteBuffer.allocateDirect(pixelData.length);
				 * out.byteBuffer.order(ByteOrder.nativeOrder()); out.byteBuffer.put(pixelData);
				 * ((Buffer) out.byteBuffer).flip(); int bpp =
				 * scaledDownImage.getColorModel().getPixelSize(); out.bytesPerPixel = bpp / 8;
				 * out.bytesPerLine = (int) Math.ceil((out.width * bpp) / 8.0); return out; });
				 */

				CompletableFuture<SBImage> cf2 = CompletableFuture.supplyAsync(() -> {
					double scaleFactor = 24.0 / (subImage.height - 2);
					int scaledDownWidth = (int) (image1.width * scaleFactor);
					int scaledDownHeight = (int) (image1.height * scaleFactor);
					BufferedImage scaledDownImage = new BufferedImage(scaledDownWidth, scaledDownHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledDownImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(image1.underlyingBuffImage, 0, 0, scaledDownWidth, scaledDownHeight, null);
					gsd.dispose();
					SBImage out = SBImage.getSBImageFromBufferedImageNoParallelise(scaledDownImage, false, false);
					populateBBoxes(out, heightCutOff, SMALL_SIZE);
					out.underlyingBuffImage = scaledDownImage;
					DataBuffer buff = scaledDownImage.getRaster().getDataBuffer();
					byte[] pixelData = ((DataBufferByte) buff).getData();
					out.byteBuffer = ByteBuffer.allocateDirect(pixelData.length);
					out.byteBuffer.order(ByteOrder.nativeOrder());
					out.byteBuffer.put(pixelData);
					((Buffer) out.byteBuffer).flip();
					int bpp = scaledDownImage.getColorModel().getPixelSize();
					out.bytesPerPixel = bpp / 8;
					out.bytesPerLine = (int) Math.ceil((out.width * bpp) / 8.0);
					return out;
				}, threadService);

				CompletableFuture<SBImage> cf3 = CompletableFuture.supplyAsync(() -> {
					SBImage dilated = SBImageUtils.dilate(image1, 3, 1);
					dilated.underlyingBuffImage = SBImage.getByteBufferedImageFromSBImage(dilated);
					double scaleFactor = 24.0 / (subImage.height - 2);
					int scaledDownWidth = (int) (dilated.width * scaleFactor);
					int scaledDownHeight = (int) (dilated.height * scaleFactor);
					BufferedImage scaledDownImage = new BufferedImage(scaledDownWidth, scaledDownHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledDownImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(dilated.underlyingBuffImage, 0, 0, scaledDownWidth, scaledDownHeight, null);
					gsd.dispose();
					SBImage out = SBImage.getSBImageFromBufferedImageNoParallelise(scaledDownImage, false, false);
					populateBBoxes(out, heightCutOff, SMALL_SIZE);
					out.underlyingBuffImage = scaledDownImage;
					DataBuffer buff = out.underlyingBuffImage.getRaster().getDataBuffer();
					byte[] pixelData = ((DataBufferByte) buff).getData();
					out.byteBuffer = ByteBuffer.allocateDirect(pixelData.length);
					out.byteBuffer.order(ByteOrder.nativeOrder());
					out.byteBuffer.put(pixelData);
					((Buffer) out.byteBuffer).flip();
					int bpp = out.underlyingBuffImage.getColorModel().getPixelSize();
					out.bytesPerPixel = bpp / 8;
					out.bytesPerLine = (int) Math.ceil((out.width * bpp) / 8.0);
					return out;
				}, threadService);

				CompletableFuture<SBImage> cf4 = CompletableFuture.supplyAsync(() -> {
					SBImage dilated = SBImageUtils.dilate(image1, 3, 1);
					BufferedImage dilatedBI = SBImage.getByteBufferedImageFromSBImage(dilated);
					double scaleFactor = 24.0 / (subImage.height - 2);
					int scaledUpWidth = dilated.width;
					int scaledUpHeight = (int) (dilated.height * scaleFactor);
					BufferedImage scaledUpImage = new BufferedImage(scaledUpWidth, scaledUpHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsu = scaledUpImage.createGraphics();
					gsu.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
					gsu.drawImage(dilatedBI, 0, 0, scaledUpWidth, scaledUpHeight, null);
					gsu.dispose();
					SBImage out = SBImage.getSBImageFromBufferedImageNoParallelise(scaledUpImage, false, false);
					populateBBoxes(out, heightCutOff, SMALL_SIZE);
					out.underlyingBuffImage = scaledUpImage;
					DataBuffer buff = scaledUpImage.getRaster().getDataBuffer();
					byte[] pixelData = ((DataBufferByte) buff).getData();
					out.byteBuffer = ByteBuffer.allocateDirect(pixelData.length);
					out.byteBuffer.order(ByteOrder.nativeOrder());
					out.byteBuffer.put(pixelData);
					((Buffer) out.byteBuffer).flip();
					int bpp = scaledUpImage.getColorModel().getPixelSize();
					out.bytesPerPixel = bpp / 8;
					out.bytesPerLine = (int) Math.ceil((out.width * bpp) / 8.0);
					return out;
				}, threadService);
				CompletableFuture.allOf(cf1, cf2, cf3, cf4).join();
				imageRow.add(image1);
				try {
					imageRow.add(cf2.get());
				} catch (Exception ee) {
				}
				try {
					imageRow.add(cf3.get());
				} catch (Exception ee) {
				}
				try {
					imageRow.add(cf4.get());
				} catch (Exception ee) {
				}
				imageRectangles.add(imageRow);
				return Boolean.TRUE;
			}, threadService));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		threadService.shutdown();
		return imageRectangles;
	}

	public static ArrayList<ArrayList<BIWrapperForOCR>> getBufferedImagesforOCR(SBImage image) throws Exception {
		return getBufferedImagesforOCR(image, 10);
	}

	public static ArrayList<ArrayList<BIWrapperForOCR>> getBufferedImagesforOCR(SBImage image, final int heightCutOff)
			throws Exception {
		if (!image.deskewed) {
			try {
				image = SBImageUtils.deskewToSBImage(image);
			} catch (Exception e) {
				// do nothing
			}
		}
		final ArrayList<SBImage> subImages = getSBImagesForTextRegions(image, heightCutOff);
		final ArrayList<ArrayList<BIWrapperForOCR>> imageRectangles = new ArrayList<ArrayList<BIWrapperForOCR>>();
		ArrayList<CompletableFuture<Boolean>> cfs = new ArrayList<CompletableFuture<Boolean>>(subImages.size());
		final ExecutorService threadService = Executors.newFixedThreadPool(subImages.size());
		for (final SBImage subImage : subImages) {
			cfs.add(CompletableFuture.supplyAsync(() -> {
				if (subImage.height < heightCutOff) {
					return Boolean.FALSE;
				}
				final int dimensionIncrement = 80;
				final ArrayList<BIWrapperForOCR> imageRow = new ArrayList<BIWrapperForOCR>();

				// create the base SBImage
				final int imageHeight = (subImage.height + dimensionIncrement);
				final int imageWidth = (subImage.width + dimensionIncrement);
				final SBImage baseSBImage = overwrite(createWhiteSBImage(imageHeight, imageWidth), subImage,
						dimensionIncrement / 2, dimensionIncrement / 2);

				// create the base BufferedImage
				final BufferedImage image1 = SBImage.getByteBufferedImageFromSBImage(baseSBImage);
				ArrayList<CompletableFuture<BIWrapperForOCR>> innerCFS = new ArrayList<CompletableFuture<BIWrapperForOCR>>();
				final ExecutorService innerThreadService = Executors.newFixedThreadPool(9);

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					return new BIWrapperForOCR(image1);
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					double scaleFactor = 24.0 / (subImage.height - 2);
					int scaledWidth = (int) (image1.getWidth() * scaleFactor);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(image1, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return new BIWrapperForOCR(scaledImage);
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					double scaleFactor = 32.0 / (subImage.height - 2);
					int scaledWidth = (int) (image1.getWidth() * scaleFactor);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(image1, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return new BIWrapperForOCR(scaledImage);
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					double scaleFactor = 32.0 / (subImage.height - 2);
					int scaledWidth = (int) ((image1.getWidth() * scaleFactor * 3.0) / 4);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(image1, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return new BIWrapperForOCR(scaledImage);
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					double scaleFactor = 32.0 / (subImage.height - 2);
					int scaledWidth = (int) ((image1.getWidth() * scaleFactor * 3.0) / 5);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(image1, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return new BIWrapperForOCR(scaledImage);
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					SBImage dilated = SBImageUtils.dilate(baseSBImage, 3, 1, SBImageUtils.BLACK_PIXEL_REPLACEMENT + 5);
					final BufferedImage tempImage = SBImage.getByteBufferedImageFromSBImage(dilated);
					double scaleFactor = 32.0 / (subImage.height - 2);
					int scaledWidth = (int) (image1.getWidth() * scaleFactor);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(tempImage, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return new BIWrapperForOCR(scaledImage);
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					SBImage dilated = SBImageUtils.dilate(baseSBImage, 3, 3, SBImageUtils.BLACK_PIXEL_REPLACEMENT + 5);
					final BufferedImage tempImage = SBImage.getByteBufferedImageFromSBImage(dilated);
					double scaleFactor = 32.0 / (subImage.height - 2);
					int scaledWidth = (int) (image1.getWidth() * 0.75);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(tempImage, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return new BIWrapperForOCR(scaledImage);
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					SBImage dilated = SBImageUtils.dilate(baseSBImage, 3, 3, SBImageUtils.BLACK_PIXEL_REPLACEMENT + 5);
					final BufferedImage tempImage = SBImage.getByteBufferedImageFromSBImage(dilated);
					double scaleFactor = 32.0 / (subImage.height - 2);
					int scaledWidth = (int) (image1.getWidth() * 0.6);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(tempImage, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return new BIWrapperForOCR(scaledImage);
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					SBImage dilated = SBImageUtils.dilate(baseSBImage, 5, 1, SBImageUtils.BLACK_PIXEL_REPLACEMENT + 5);
					final BufferedImage tempImage = SBImage.getByteBufferedImageFromSBImage(dilated);
					double scaleFactor = 32.0 / (subImage.height - 2);
					int scaledWidth = (int) (image1.getWidth() * 0.6);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(tempImage, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return new BIWrapperForOCR(scaledImage);
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					SBImage dilated = SBImageUtils.dilate(baseSBImage, 5, 3, SBImageUtils.BLACK_PIXEL_REPLACEMENT + 5);
					final BufferedImage tempImage = SBImage.getByteBufferedImageFromSBImage(dilated);
					double scaleFactor = 32.0 / (subImage.height - 2);
					int scaledWidth = (int) (image1.getWidth() * 0.6);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(tempImage, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return new BIWrapperForOCR(scaledImage);
				}, innerThreadService));

				CompletableFuture.allOf(innerCFS.toArray(new CompletableFuture[innerCFS.size()])).join();
				innerThreadService.shutdown();

				for (CompletableFuture<BIWrapperForOCR> cf : innerCFS) {
					try {
						imageRow.add(cf.get());
					} catch (Exception ee) {
					}
				}
				imageRectangles.add(imageRow);
				return Boolean.TRUE;
			}, threadService));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		threadService.shutdown();
		return imageRectangles;
	}

	public static ArrayList<BIWrapperForOCR> getOCRImages(SBImage image, final int heightCutOff) throws Exception {
		if (!image.deskewed) {
			try {
				image = SBImageUtils.deskewToSBImage(image);
			} catch (Exception e) {
				// do nothing
			}
		}
		final ArrayList<SBImage> subImages = getSBImagesForTextRegions(image, heightCutOff);

		final ArrayList<BIWrapperForOCR> imageRectangles = new ArrayList<BIWrapperForOCR>();
		ArrayList<CompletableFuture<Boolean>> cfs = new ArrayList<CompletableFuture<Boolean>>(subImages.size());
		final ExecutorService threadService = Executors.newFixedThreadPool(Math.min(subImages.size(), 30));
		for (final SBImage subImage : subImages) {
			cfs.add(CompletableFuture.supplyAsync(() -> {
				if (subImage.height < heightCutOff) {
					return Boolean.FALSE;
				}
				final int dimensionIncrement = 80;
				final ArrayList<SBImage> imageRow = new ArrayList<SBImage>();

				// create the base SBImage
				final int imageHeight = (subImage.height + dimensionIncrement);
				final int imageWidth = (subImage.width + dimensionIncrement);
				final SBImage baseSBImage = overwrite(createWhiteSBImage(imageHeight, imageWidth), subImage,
						dimensionIncrement / 2, dimensionIncrement / 2);

				// create the base BufferedImage
				final BufferedImage image1 = SBImage.getByteBufferedImageFromSBImage(baseSBImage);
				ArrayList<CompletableFuture<SBImage>> innerCFS = new ArrayList<CompletableFuture<SBImage>>();
				final ExecutorService innerThreadService = Executors.newFixedThreadPool(10);

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					// return SBImage.getSBImageFromBufferedImageNoParallelise(image1, false,
					// false);
					return baseSBImage;
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					double scaleFactor = 24.0 / (subImage.height - 2);
					int scaledWidth = (int) (image1.getWidth() * scaleFactor);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(image1, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return SBImage.getSBImageFromBufferedImageNoParallelise(scaledImage, false, false);
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					double scaleFactor = 32.0 / (subImage.height - 2);
					int scaledWidth = (int) (image1.getWidth() * scaleFactor);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(image1, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return SBImage.getSBImageFromBufferedImageNoParallelise(scaledImage, false, false);
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					double scaleFactor = 32.0 / (subImage.height - 2);
					int scaledWidth = (int) ((image1.getWidth() * scaleFactor * 3.0) / 4);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(image1, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return SBImage.getSBImageFromBufferedImageNoParallelise(scaledImage, false, false);
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					double scaleFactor = 32.0 / (subImage.height - 2);
					int scaledWidth = (int) ((image1.getWidth() * scaleFactor * 3.0) / 5);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(image1, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return SBImage.getSBImageFromBufferedImageNoParallelise(scaledImage, false, false);
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					SBImage dilated = SBImageUtils.dilate(baseSBImage, 3, 1, SBImageUtils.BLACK_PIXEL_REPLACEMENT + 5);
					final BufferedImage tempImage = SBImage.getByteBufferedImageFromSBImage(dilated);
					double scaleFactor = 32.0 / (subImage.height - 2);
					int scaledWidth = (int) (image1.getWidth() * scaleFactor);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(tempImage, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return SBImage.getSBImageFromBufferedImageNoParallelise(scaledImage, false, false);
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					SBImage dilated = SBImageUtils.dilate(baseSBImage, 3, 3, SBImageUtils.BLACK_PIXEL_REPLACEMENT + 5);
					final BufferedImage tempImage = SBImage.getByteBufferedImageFromSBImage(dilated);
					double scaleFactor = 32.0 / (subImage.height - 2);
					int scaledWidth = (int) (image1.getWidth() * 0.75);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(tempImage, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return SBImage.getSBImageFromBufferedImageNoParallelise(scaledImage, false, false);
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					SBImage dilated = SBImageUtils.dilate(baseSBImage, 3, 3, SBImageUtils.BLACK_PIXEL_REPLACEMENT + 5);
					final BufferedImage tempImage = SBImage.getByteBufferedImageFromSBImage(dilated);
					double scaleFactor = 32.0 / (subImage.height - 2);
					int scaledWidth = (int) (image1.getWidth() * 0.6);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(tempImage, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return SBImage.getSBImageFromBufferedImageNoParallelise(scaledImage, false, false);
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					SBImage dilated = SBImageUtils.dilate(baseSBImage, 5, 1, SBImageUtils.BLACK_PIXEL_REPLACEMENT + 5);
					final BufferedImage tempImage = SBImage.getByteBufferedImageFromSBImage(dilated);
					double scaleFactor = 32.0 / (subImage.height - 2);
					int scaledWidth = (int) (image1.getWidth() * 0.6);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(tempImage, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return SBImage.getSBImageFromBufferedImageNoParallelise(scaledImage, false, false);
				}, innerThreadService));

				innerCFS.add(CompletableFuture.supplyAsync(() -> {
					SBImage dilated = SBImageUtils.dilate(baseSBImage, 5, 3, SBImageUtils.BLACK_PIXEL_REPLACEMENT + 5);
					final BufferedImage tempImage = SBImage.getByteBufferedImageFromSBImage(dilated);
					double scaleFactor = 32.0 / (subImage.height - 2);
					int scaledWidth = (int) (image1.getWidth() * 0.6);
					int scaledHeight = (int) (image1.getHeight() * scaleFactor);
					BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D gsd = scaledImage.createGraphics();
					setGraphics2DParameters(gsd);
					gsd.drawImage(tempImage, 0, 0, scaledWidth, scaledHeight, null);
					gsd.dispose();
					return SBImage.getSBImageFromBufferedImageNoParallelise(scaledImage, false, false);
				}, innerThreadService));

				CompletableFuture.allOf(innerCFS.toArray(new CompletableFuture[innerCFS.size()])).join();
				innerThreadService.shutdown();

				for (CompletableFuture<SBImage> cf : innerCFS) {
					try {
						imageRow.add(cf.get());
					} catch (Exception ee) {
					}
				}

				SBImage finalSBImage = merge(imageRow);
				imageRectangles.add(new BIWrapperForOCR(SBImage.getByteBufferedImageFromSBImage(finalSBImage)));
				return Boolean.TRUE;
			}, threadService));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		threadService.shutdown();
		return imageRectangles;
	}

	public static ArrayList<ArrayList<BufferedImage>> getScaledImagesforTextRegions1(SBImage image) {
		if (!image.deskewed) {
			try {
				image = SBImageUtils.deskewToSBImage(image);
			} catch (Exception e) {
				// do nothing
			}
		}
		ArrayList<Integer> boundaries = getYCoordinatesForTextRegions(image);
		ArrayList<BufferedImage> imageRow;
		ArrayList<ArrayList<BufferedImage>> imageRectangles = new ArrayList<ArrayList<BufferedImage>>();

		int yTop = 0, yBot = 0;
		for (int i = 0; i < boundaries.size(); i = i + 2) {
			yTop = Math.max(0, boundaries.get(i) - 2);
			yBot = Math.min(image.height - 1, boundaries.get(i + 1) + 2);
			final int newImageHeight = (yBot - yTop) + verticalPadding;
			final int newImageWidth = image.width + horizontalPadding;

			imageRow = new ArrayList<BufferedImage>();

			final byte[] dataChildImage = ((DataBufferByte) image.underlyingBuffImage
					.getData(new Rectangle(0, yTop, image.width, (yBot - yTop) + 1)).getDataBuffer()).getData();
			BufferedImage newImage = new BufferedImage(newImageWidth, newImageHeight,
					image.underlyingBuffImage.getType());
			final byte[] dataNewImage = new byte[newImageHeight * newImageWidth];
			byte white = (byte) 255;

			for (int j = 0; j < dataNewImage.length; ++j) {
				dataNewImage[j] = white;
			}
			final int jStart = (newImageHeight - ((yBot - yTop) + 1)) / 2;
			final int jEnd = jStart + (yBot - yTop) + 1;
			for (int j = jStart; j < jEnd; ++j) {
				System.arraycopy(dataChildImage, (j - jStart) * image.width, dataNewImage,
						(j * newImageWidth) + ((newImageWidth - image.width) / 2), image.width);
			}
			newImage.getRaster().setDataElements(0, 0, newImageWidth, newImageHeight, dataNewImage);

			int scaledDownWidth = (int) ((newImage.getWidth() * scaleDown) + (horizontalPadding / 2));
			int scaledDownHeight = (int) ((newImage.getHeight() * scaleDown) + (verticalPadding / 2));
			BufferedImage scaledDownImage = new BufferedImage(scaledDownWidth, scaledDownHeight,
					BufferedImage.TYPE_BYTE_GRAY);
			Graphics2D gsd = scaledDownImage.createGraphics();
			gsd.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			gsd.drawImage(newImage, 0, 0, scaledDownWidth, scaledDownHeight, null);
			gsd.dispose();

			int scaledUpWidth = (int) ((newImage.getWidth() * scaleUp) + (horizontalPadding / 2));
			int scaledUpHeight = (int) ((newImage.getHeight() * scaleUp) + (verticalPadding / 2));
			BufferedImage scaledUpImage = new BufferedImage(scaledUpWidth, scaledUpHeight,
					BufferedImage.TYPE_BYTE_GRAY);
			Graphics2D gsu = scaledUpImage.createGraphics();
			gsu.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			gsu.drawImage(newImage, 0, 0, scaledUpWidth, scaledUpHeight, null);
			gsu.dispose();

			imageRow.add(scaledDownImage);
			imageRow.add(newImage);
			imageRow.add(scaledUpImage);

			// imageRow = new ArrayList<BufferedImage>();
			/*
			 * final WritableRaster mainRaster = image.underlyingBuffImage.getRaster();
			 * final Raster childRaster = mainRaster.createChild(0, yTop, image.width, (yBot
			 * - yTop) + 1, 0, 0, null); final BufferedImage newImage = new
			 * BufferedImage(image.width, (yBot - yTop) + 1,
			 * image.underlyingBuffImage.getType()); newImage.setData(childRaster);
			 *
			 */

			// final int[][] data2D = SBImage.extractRectangle(image.pixels, 0, yTop,
			// (yBot - yTop) + 1, image.width);
			// final int[] data1D = SBImageArrayUtils.iterateFlatten(data2D);
			// final BufferedImage newImage = new BufferedImage(image.width, (yBot - yTop) +
			// 1,
			// BufferedImage.TYPE_INT_RGB);
			// newImage.getRaster().setDataElements(0, 0, image.width, (yBot - yTop) + 1,
			// data1D);
			imageRectangles.add(imageRow);
		}
		return imageRectangles;
	}

	public static SBImage removePepper(SBImage image, int kernelHeight, int kernelWidth) {
		int[][] out = new int[image.height][image.width];
		SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelHeight, kernelWidth);
		for (int y = kernelHeight / 2; y < (image.height - (kernelHeight / 2)); ++y) {
			for (int x = kernelWidth / 2; x < (image.width - (kernelWidth / 2)); ++x) {
				boolean white = true;
				boolean gray = true;
				for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); ++dy) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); ++dx) {
						if ((Math.abs(dy) == (kernelHeight / 2)) || (Math.abs(dx) == (kernelWidth / 2))) {
							white = white && (image.pixels[y + dy][x + dx] >= (GRAY_PIXEL_REPLACEMENT + 5));
							gray = (gray && (image.pixels[y + dy][x + dx] >= (BLACK_PIXEL_REPLACEMENT + 20)))
									&& (image.pixels[y + dy][x + dx] < (GRAY_PIXEL_REPLACEMENT + 5));
						}
					}
				}
				if (white) {
					out[y][x] = WHITE_PIXEL_REPLACEMENT;
				} else {
					if (gray) {
						out[y][x] = GRAY_PIXEL_REPLACEMENT;
					} else {
						out[y][x] = image.pixels[y][x];
					}
				}
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage parallelRemovePepper(final SBImage image, final int kernelHeight, final int kernelWidth)
			throws Exception {

		final SBImage[] subImages = image.createSubImageArray(kernelHeight, kernelWidth);
		final SBImage[] results = new SBImage[subImages.length];
		final ArrayList<CompletableFuture<Object>> cfs = new ArrayList<>(subImages.length);
		final ExecutorService threadService = Executors.newFixedThreadPool(Math.min(subImages.length, 30));
		for (int count = 0; count < subImages.length; ++count) {
			final int counter = count;
			cfs.add(CompletableFuture.supplyAsync(() -> {
				results[counter] = removePepper(subImages[counter], kernelHeight, kernelWidth);
				return Boolean.TRUE;
			}, threadService));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		threadService.shutdown();
		return SBImage.stitchSubImages(results, image.xDivisions, image.yDivisions, kernelHeight, kernelWidth);
	}

	public static SBImage removePepperFromStitchedImage(SBImage image) throws Exception {
		SBImage sbid1 = SBImageUtils.parallelRemovePepper(image, 3, 3);
		SBImage sbid2 = SBImageUtils.parallelRemovePepper(sbid1, 5, 5);
		sbid1 = SBImageUtils.parallelRemovePepper(sbid2, 7, 7);
		// sbid2 = SBImageUtils.parallelRemovePepper(sbid1, 3, 3);
		// sbid1 = SBImageUtils.parallelRemovePepper(sbid2, 5, 5);
		sbid1.deskewed = image.deskewed;
		return sbid1;
		// return SBImageUtils.removePepper(sbid1, 7, 7);
	}

	public static SBImage subtractHorizontal(SBImage original, SBImage onlyLines) throws Exception {
		if ((original.width != onlyLines.width) || (original.height != onlyLines.height)) {
			throw new Exception("Image dimensions must be equal");
		}
		int[][] out = new int[original.height][original.width];
		for (int y = 0; y < original.height; ++y) {
			for (int x = 0; x < original.width; ++x) {
				if (onlyLines.pixels[y][x] <= 144) {
					out[y][x] = WHITE_PIXEL_REPLACEMENT;
				} else {
					out[y][x] = original.pixels[y][x];
				}
			}
		}
		setBorderValues(original, out, Integer.MIN_VALUE, 1, 5);
		return new SBImage(out);
	}

	public static SBImage subtractVertical(SBImage original, SBImage onlyLines) throws Exception {
		if ((original.width != onlyLines.width) || (original.height != onlyLines.height)) {
			throw new Exception("Image dimensions must be equal");
		}
		int[][] out = new int[original.height][original.width];
		for (int y = 0; y < original.height; ++y) {
			for (int x = 0; x < original.width; ++x) {
				if (onlyLines.pixels[y][x] <= 144) {
					out[y][x] = WHITE_PIXEL_REPLACEMENT;
				} else {
					out[y][x] = original.pixels[y][x];
				}
			}
		}
		setBorderValues(original, out, Integer.MIN_VALUE, 5, 1);
		return new SBImage(out);
	}

	public static SBImage subtract(SBImage original, SBImage horizontalLines, SBImage verticalLines) throws Exception {
		if ((original.width != horizontalLines.width) || (original.height != horizontalLines.height)) {
			throw new Exception("Image dimensions must be equal to horizontalLines image");
		}
		if ((original.width != verticalLines.width) || (original.height != verticalLines.height)) {
			throw new Exception("Image dimensions must be equal to verticalLines image");
		}
		int[][] out = new int[original.height][original.width];
		for (int y = 0; y < original.height; ++y) {
			for (int x = 0; x < original.width; ++x) {
				if (horizontalLines.pixels[y][x] <= 144) {
					out[y][x] = WHITE_PIXEL_REPLACEMENT;
				} else {
					if (verticalLines.pixels[y][x] <= 144) {
						out[y][x] = WHITE_PIXEL_REPLACEMENT;
					} else {
						out[y][x] = original.pixels[y][x];
					}
				}
			}
		}
		setBorderValues(original, out, Integer.MIN_VALUE, 5, 5);
		return new SBImage(out);
	}

	public static SBImage subtract(SBImage original, SBImage horizontalLines, SBImage verticalLines,
			int pixelsImpactedUpDownRightLeft) throws Exception {
		if ((original.width != horizontalLines.width) || (original.height != horizontalLines.height)) {
			throw new Exception("Image dimensions must be equal to horizontalLines image");
		}
		if ((original.width != verticalLines.width) || (original.height != verticalLines.height)) {
			throw new Exception("Image dimensions must be equal to verticalLines image");
		}

		int[][] out = new int[original.height][original.width];
		for (int y = pixelsImpactedUpDownRightLeft + 1; y < (original.height - pixelsImpactedUpDownRightLeft
				- 1); ++y) {
			for (int x = pixelsImpactedUpDownRightLeft + 1; x < (original.width - pixelsImpactedUpDownRightLeft
					- 1); ++x) {
				// Code has to be written to ensure that if the lines are cutting through
				// characters, then retain those pixels.

				// Has to be further fine-tuned to ensure that intersection points of lines are
				// accounted for properly

				// Both of the above reqs are fairly complex

				// set pixels of verticalKernel for horizontalLines
				if (horizontalLines.pixels[y][x] <= 144) {
					for (int dy = -pixelsImpactedUpDownRightLeft; dy <= pixelsImpactedUpDownRightLeft; ++dy) {
						out[y + dy][x] = WHITE_PIXEL_REPLACEMENT;
					}
				} else {
					// set pixels of horizontalKernel for verticalLines
					if (verticalLines.pixels[y][x] <= 144) {
						for (int dx = -pixelsImpactedUpDownRightLeft; dx <= pixelsImpactedUpDownRightLeft; ++dx) {
							out[y][x + dx] = WHITE_PIXEL_REPLACEMENT;
						}
					} else {
						out[y][x] = original.pixels[y][x];
					}
				}
			}
		}
		setBorderValues(original, out, Integer.MIN_VALUE, (2 * pixelsImpactedUpDownRightLeft) + 1,
				(2 * pixelsImpactedUpDownRightLeft) + 1);
		return new SBImage(out);
	}

	public static SBImage createWhiteSBImage(int height, int width) {
		int[][] pixels = new int[height][width];
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				pixels[y][x] = 255;
			}
		}
		return new SBImage(pixels, false, false, false, false);
	}

	public static SBImage overwrite(SBImage target, SBImage toBeCopied, int xCoord, int yCoord) {
		if ((xCoord > target.width) || (yCoord > target.height)) {
			return target;
		}
		if ((xCoord < 0) || (yCoord < 0)) {
			return target;
		}
		int lastX = Math.min((xCoord + toBeCopied.width) - 1, target.width - 1);
		int lastY = Math.min((yCoord + toBeCopied.height) - 1, target.height - 1);
		for (int x = xCoord; x < lastX; ++x) {
			for (int y = yCoord; y < lastY; ++y) {
				target.pixels[y][x] = toBeCopied.pixels[y - yCoord][x - xCoord];
			}
		}
		return target;
	}

	/*
	 * public static ArrayList<ArrayList<ArrayList<OCRResultPix>>>
	 * doOCR1(ArrayList<ArrayList<BIWrapperForOCR>> textRegions) throws Exception {
	 *
	 * final ArrayList<ArrayList<ArrayList<OCRResultPix>>> pageResult = new
	 * ArrayList<ArrayList<ArrayList<OCRResultPix>>>( textRegions.size()); for
	 * (ArrayList<BIWrapperForOCR> textRegion : textRegions) { final
	 * ArrayList<ArrayList<OCRResultPix>> lineResult = new
	 * ArrayList<ArrayList<OCRResultPix>>(textRegion.size());
	 * pageResult.add(lineResult); TesseractHandle handleWrapper = (TesseractHandle)
	 * tesseractPool.borrowObject(); TessBaseAPI handle = handleWrapper.getHandle();
	 * int i = 1; for (BIWrapperForOCR wrapper : textRegion) { final
	 * ArrayList<OCRResultPix> lineAvatar = new ArrayList<OCRResultPix>();
	 * lineResult.add(lineAvatar); Instant t = Instant.now();
	 * TessAPI1.TessBaseAPISetImage(handle, wrapper.byteBuffer,
	 * wrapper.image.getWidth(), wrapper.image.getHeight(), wrapper.bytesPerPixel,
	 * wrapper.bytesPerLine); // int res =
	 * TessAPI1.TessBaseAPIGetSourceYResolution(handle); // if (res < 70) { //
	 * TessAPI1.TessBaseAPISetSourceResolution(handle, 70); // } ETEXT_DESC monitor
	 * = new ETEXT_DESC(); TessAPI1.TessBaseAPIRecognize(handle, monitor);
	 * TessResultIterator ri = TessAPI1.TessBaseAPIGetIterator(handle);
	 * TessPageIterator pi = TessAPI1.TessResultIteratorGetPageIterator(ri);
	 * TessAPI1.TessPageIteratorBegin(pi); int level =
	 * TessAPI1.TessPageIteratorLevel.RIL_TEXTLINE; do { Pointer ptr =
	 * TessAPI1.TessResultIteratorGetUTF8Text(ri, level); if (ptr == null) { break;
	 * } String words = ptr.getString(0); TessAPI1.TessDeleteText(ptr); //
	 * System.gc(); float confidence = TessAPI1.TessResultIteratorConfidence(ri,
	 * level); // System.out.println(words + " - confidence = " + (int) confidence);
	 * lineAvatar.add(new OCRResultPix(words, confidence));
	 * System.out.println(words); } while (TessAPI1.TessPageIteratorNext(pi, level)
	 * == TRUE); System.out.println("Time taken = " + Duration.between(t,
	 * Instant.now()).toMillis()); t = Instant.now();
	 * TessAPI1.TessPageIteratorDelete(pi); // System.gc(); //
	 * TessAPI1.TessResultIteratorDelete(ri); // TessAPI1.TessBaseAPIClear(handle);
	 * TessAPI1.TessBaseAPIClearAdaptiveClassifier(handle); // System.gc();
	 * System.out.println("Time taken = " + Duration.between(t,
	 * Instant.now()).toMillis()); }
	 * TessAPI1.TessBaseAPIPrintVariablesToFile(handle,
	 * "E:\\TechWerx\\Java\\Working\\" + i++ + ".data");
	 * tesseractPool.returnObject(handleWrapper); } return pageResult; }
	 *
	 */

	/*
	 * public static ArrayList<OCRResultPix> doOCR(ArrayList<BIWrapperForOCR>
	 * textRegions) throws Exception {
	 *
	 * final ArrayList<OCRResultPix> pageResult = new
	 * ArrayList<OCRResultPix>(textRegions.size());
	 * ArrayList<CompletableFuture<Boolean>> cfs = new
	 * ArrayList<CompletableFuture<Boolean>>(textRegions.size()); // Long time1 =
	 * 0L; // Long time2 = 0L; // Long time3 = 0L; // Long time4 = 0L; // Long time5
	 * = 0L; // Long time6 = 0L; final ExecutorService threadService =
	 * Executors.newFixedThreadPool(Math.min(textRegions.size(), 30)); for
	 * (BIWrapperForOCR textRegion : textRegions) {
	 * cfs.add(CompletableFuture.supplyAsync(() -> { final OCRResultPix lineResult =
	 * new OCRResultPix(); pageResult.add(lineResult); TesseractHandle handleWrapper
	 * = null; // Instant t = Instant.now(); try { handleWrapper = (TesseractHandle)
	 * tesseractPool.borrowObject(); } catch (Exception e) { e.printStackTrace();
	 *
	 * } TessBaseAPI tesseractHandle = handleWrapper.getHandle(); //
	 * System.out.println( // textRegion + ": Time to get handle = " +
	 * Duration.between(t, // Instant.now()).toMillis()); //
	 * System.out.println("Using handle " + tesseractHandle); //
	 * System.out.println(tesseractHandle); // int i = 1; // Instant t =
	 * Instant.now(); // t = Instant.now();
	 * TessAPI1.TessBaseAPISetImage(tesseractHandle, textRegion.byteBuffer,
	 * textRegion.image.getWidth(), textRegion.image.getHeight(),
	 * textRegion.bytesPerPixel, textRegion.bytesPerLine); // System.out //
	 * .println(textRegion + ": Time to set image = " + Duration.between(t, //
	 * Instant.now()).toMillis()); // t = Instant.now(); int res =
	 * TessAPI1.TessBaseAPIGetSourceYResolution(tesseractHandle); if (res < 70) {
	 * TessAPI1.TessBaseAPISetSourceResolution(tesseractHandle, 70); } //
	 * System.out.println( // textRegion + ": Time to set resolution = " +
	 * Duration.between(t, // Instant.now()).toMillis()); ETEXT_DESC monitor = new
	 * ETEXT_DESC(); Instant t = Instant.now();
	 * TessAPI1.TessBaseAPIRecognize(tesseractHandle, monitor); System.out
	 * .println(textRegion + ": Time to recognize = " + Duration.between(t,
	 * Instant.now()).toMillis()); // t = Instant.now(); TessResultIterator ri =
	 * TessAPI1.TessBaseAPIGetIterator(tesseractHandle); //
	 * System.out.println(textRegion + ": Time to get result iterator = " // +
	 * Duration.between(t, Instant.now()).toMillis()); // t = Instant.now();
	 * TessPageIterator pi = TessAPI1.TessResultIteratorGetPageIterator(ri); //
	 * System.out.println( // textRegion + ": Time to get page iterator = " +
	 * Duration.between(t, // Instant.now()).toMillis()); // t = Instant.now();
	 * TessAPI1.TessPageIteratorBegin(pi); // System.out.println(textRegion +
	 * ": Time to get begin page iteration = " // + Duration.between(t,
	 * Instant.now()).toMillis());
	 *
	 * // System.out.println(textRegion + ": Time to recognize, get RI, PI and begin
	 * = // " // + Duration.between(t, Instant.now()).toMillis()); // int level =
	 * TessAPI1.TessPageIteratorLevel.RIL_TEXTLINE; int level =
	 * TessAPI1.TessPageIteratorLevel.RIL_TEXTLINE; // t = Instant.now(); do {
	 * Pointer ptr = TessAPI1.TessResultIteratorGetUTF8Text(ri, level); if (ptr ==
	 * null) { break; } String words = ptr.getString(0); float confidence =
	 * TessAPI1.TessResultIteratorConfidence(ri, level); // System.gc(); //
	 * System.out.println(words + " - confidence = " + (int) confidence);
	 * lineResult.add(words, confidence); TessAPI1.TessDeleteText(ptr); } while
	 * (TessAPI1.TessPageIteratorNext(pi, level) == TRUE); //
	 * System.out.println(textRegion + ": Time to iterate through the textlines = "
	 * // + Duration.between(t, Instant.now()).toMillis()); //
	 * System.out.println("Time taken = " + Duration.between(t, //
	 * Instant.now()).toMillis()); // t = Instant.now(); // t = Instant.now();
	 * TessAPI1.TessPageIteratorDelete(pi); // System.out.println(textRegion +
	 * ": Time to delete page iterator = " // + Duration.between(t,
	 * Instant.now()).toMillis()); // System.gc(); //
	 * TessAPI1.TessResultIteratorDelete(ri); // TessAPI1.TessBaseAPIClear(handle);
	 * // TessAPI1.TessBaseAPIClearAdaptiveClassifier(handle); // t = Instant.now();
	 * tesseractPool.returnObject(handleWrapper); // System.out.println( //
	 * textRegion + ": Time to return handle = " + Duration.between(t, //
	 * Instant.now()).toMillis()); new Runnable() {
	 *
	 * @Override public void run() { System.gc(); } }.run(); return Boolean.TRUE; //
	 * System.out.println("Time taken = " + Duration.between(t, //
	 * Instant.now()).toMillis()); }, threadService)); } Instant t = Instant.now();
	 * CompletableFuture.allOf(cfs.toArray(new
	 * CompletableFuture[cfs.size()])).join(); threadService.shutdown();
	 * System.out.println("Time waiting for join = " + Duration.between(t,
	 * Instant.now()).toMillis()); // System.out.println("Done an image"); //
	 * TessAPI1.TessBaseAPIPrintVariablesToFile(handle, //
	 * "E:\\TechWerx\\Java\\Working\\" + i++ + ".data"); return pageResult; }
	 *
	 */

	public static SBImage merge(ArrayList<SBImage> images) {
		int width = 0;
		int height = 0;
		for (SBImage image : images) {
			width = Math.max(width, image.width);
			height += image.height;
		}
		SBImage out = createWhiteSBImage(height, width);
		int yCoord = 0;
		for (SBImage image : images) {
			int xCoord = (width - image.width) / 2;
			SBImageUtils.overwrite(out, image, xCoord, yCoord);
			yCoord += image.height;
		}
		return out;
	}

	public static SBImage lighten(SBImage image, int kernelHeight, int kernelWidth) {

		int[][] out = new int[image.height][image.width];
		SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelHeight, kernelWidth);
		for (int y = kernelHeight / 2; y < (image.height - (kernelHeight / 2)); y++) {
			for (int x = kernelWidth / 2; x < (image.width - (kernelWidth / 2)); x++) {
				int max = Integer.MIN_VALUE;
				loop1: for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); dx++) {
						max = Math.max(max, image.pixels[y + dy][x + dx]);
						if (max >= (SBImageUtils.WHITE_PIXEL_REPLACEMENT - 5)) {
							break loop1;
						}
					}
				}
				out[y][x] = (max + image.pixels[y][x]) / 2;
				// System.out.println("Set out[" + y + "," + x + "] to " + max + " from " +
				// image.pixels[y][x]);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static SBImage darken(SBImage image, int kernelHeight, int kernelWidth) {

		int[][] out = new int[image.height][image.width];
		SBImageUtils.setBorderValues(image, out, Integer.MIN_VALUE, kernelHeight, kernelWidth);
		for (int y = kernelHeight / 2; y < (image.height - (kernelHeight / 2)); y++) {
			for (int x = kernelWidth / 2; x < (image.width - (kernelWidth / 2)); x++) {
				int min = Integer.MAX_VALUE;
				loop1: for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); dx++) {
						min = Math.min(min, image.pixels[y + dy][x + dx]);
						if (min <= (SBImageUtils.BLACK_PIXEL_REPLACEMENT + 5)) {
							break loop1;
						}
					}
				}
				out[y][x] = (min + image.pixels[y][x]) / 2;
				// System.out.println("Set out[" + y + "," + x + "] to " + min + " from " +
				// image.pixels[y][x]);
			}
		}
		try {
			return new SBImage(out);
		} catch (InterruptedException ie) {
		}
		return null;
	}

	public static int[][] darken(int[][] input, int kernelHeight, int kernelWidth) {

		int[][] out = new int[input.length][input[0].length];
		for (int y = 0; y < (kernelHeight / 2); y++) {
			for (int x = 0; x < input[0].length; x++) {
				out[y][x] = input[y][x];
			}
		}
		// set bottom border
		for (int y = input.length - 1; y > (input.length - 1 - (kernelHeight / 2)); y--) {
			for (int x = 0; x < input[0].length; x++) {
				out[y][x] = input[y][x];
			}
		}
		// set left border
		for (int y = 0; y < input.length; y++) {
			for (int x = 0; x < (kernelWidth / 2); x++) {
				out[y][x] = input[y][x];
			}
		}
		// set right border
		for (int y = 0; y < input.length; y++) {
			for (int x = input[0].length - 1; x > (input[0].length - 1 - (kernelWidth / 2)); x--) {
				out[y][x] = input[y][x];
			}
		}

		for (int y = kernelHeight / 2; y < (input.length - (kernelHeight / 2)); y++) {
			for (int x = kernelWidth / 2; x < (input[0].length - (kernelWidth / 2)); x++) {
				int min = Integer.MAX_VALUE;
				loop1: for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); dx++) {
						min = Math.min(min, input[y + dy][x + dx]);
						if (min <= (SBImageUtils.BLACK_PIXEL_REPLACEMENT + 5)) {
							break loop1;
						}
					}
				}
				out[y][x] = (min + input[y][x]) / 2;
				// System.out.println("Set out[" + y + "," + x + "] to " + min + " from " +
				// image.pixels[y][x]);
			}
		}
		return out;
	}

	public static int[][] lighten(int[][] input, int kernelHeight, int kernelWidth) {

		int[][] out = new int[input.length][input[0].length];
		for (int y = 0; y < (kernelHeight / 2); y++) {
			for (int x = 0; x < input[0].length; x++) {
				out[y][x] = input[y][x];
			}
		}
		// set bottom border
		for (int y = input.length - 1; y > (input.length - 1 - (kernelHeight / 2)); y--) {
			for (int x = 0; x < input[0].length; x++) {
				out[y][x] = input[y][x];
			}
		}
		// set left border
		for (int y = 0; y < input.length; y++) {
			for (int x = 0; x < (kernelWidth / 2); x++) {
				out[y][x] = input[y][x];
			}
		}
		// set right border
		for (int y = 0; y < input.length; y++) {
			for (int x = input[0].length - 1; x > (input[0].length - 1 - (kernelWidth / 2)); x--) {
				out[y][x] = input[y][x];
			}
		}

		for (int y = kernelHeight / 2; y < (input.length - (kernelHeight / 2)); y++) {
			for (int x = kernelWidth / 2; x < (input[0].length - (kernelWidth / 2)); x++) {
				int max = Integer.MIN_VALUE;
				loop1: for (int dy = -kernelHeight / 2; dy <= (kernelHeight / 2); dy++) {
					for (int dx = -kernelWidth / 2; dx <= (kernelWidth / 2); dx++) {
						max = Math.max(max, input[y + dy][x + dx]);
						if (max >= (SBImageUtils.WHITE_PIXEL_REPLACEMENT - 5)) {
							break loop1;
						}
					}
				}
				out[y][x] = (max + input[y][x]) / 2;
				// System.out.println("Set out[" + y + "," + x + "] to " + max + " from " +
				// image.pixels[y][x]);
			}
		}
		return out;
	}

	public static int[][] makeOnePixelBorder(int[][] input) {
		for (int y = 0; y < 1; y++) {
			for (int x = 0; x < input[0].length; x++) {
				input[y][x] = 0;
			}
		}
		// set bottom border
		for (int y = input.length - 1; y > (input.length - 2); y--) {
			for (int x = 0; x < input[0].length; x++) {
				input[y][x] = 0;
			}
		}
		// set left border
		for (int y = 0; y < input.length; y++) {
			for (int x = 0; x < 1; x++) {
				input[y][x] = 0;
			}
		}
		// set right border
		for (int y = 0; y < input.length; y++) {
			for (int x = input[0].length - 1; x > (input[0].length - 2); x--) {
				input[y][x] = 0;
			}
		}

		return input;

	}

	public static Pix removeSaltPepper(Pix pixs, int debugL) {
		// selSize = kernelSize;
		Pix pix1 = null, pix2 = null, pix3 = null, pix4 = null, pix5 = null, pix6 = null, pix7 = null, pix8 = null,
				pix9 = null, pix10 = null;
		Sel sel1 = null, sel2 = null, sel3 = null, sel4 = null, sel5 = null, sel6 = null, sel7 = null, sel8 = null;

		String selString1 = "oooooC oo  ooooo";
		String selString2 = "oooooo C oo   oo   oooooo";
		String selString3 = "ooooooo    oo C  oo    oo    ooooooo";
		String selString4 = "ooooooooooo   C    oo        ooooooooooo";
		String selString5 = "oooooooo  C  oo     oooooooo";
		String selString6 = "oooooooo     oo   C oooooooo";

		if (Leptonica.INSTANCE.pixGetDepth(pixs) != 1) {
			pix1 = Leptonica1.pixConvertTo1(pixs, SBImageUtils.GRAY_PIXEL_REPLACEMENT - 80);
		} else {
			pix1 = Leptonica1.pixCopy(null, pixs);
		}

		/* Remove the speckle noise up to selSize x selSize */
		sel1 = Leptonica1.selCreateFromString(selString1, 4, 4, "saltAndPepper1");
		sel2 = Leptonica1.selCreateFromString(selString2, 5, 5, "saltAndPepper2");
		sel3 = Leptonica1.selCreateFromString(selString3, 6, 6, "saltAndPepper3");
		// The last parameter is either SEL_HIT or SEL_MISS or SEL_DONT_CARE
		sel4 = Leptonica1.selCreateBrick(2, 2, 0, 0, ILeptonica.SEL_HIT);
		sel5 = Leptonica1.selCreateFromString(selString4, 10, 4, "saltAndPepper4");
		sel6 = Leptonica1.selCreateBrick(3, 3, 1, 1, ILeptonica.SEL_HIT);
		sel7 = Leptonica1.selCreateFromString(selString5, 7, 4, "saltAndPepper5");
		sel8 = Leptonica1.selCreateFromString(selString6, 7, 4, "saltAndPepper6");

		pix2 = Leptonica1.pixHMT(null, pix1, sel1.getPointer());
		Pix pix21 = Leptonica1.pixDilate(null, pix2, sel4.getPointer());
		// pix2 = Leptonica1.pixDilateBrick(null, pix2, 2, 2);
		Pix pix22 = Leptonica1.pixSubtract(null, pix1, pix21);
		LeptUtils.dispose(pix1);
		LeptUtils.dispose(pix2);
		LeptUtils.dispose(pix21);
		if (debugL <= 1) {
			System.out.println("Reached - 1 in removeSaltPepper");
			System.out.println("Pix 22 = " + pix22);
		}

		pix3 = Leptonica1.pixHMT(null, pix22, sel2.getPointer());
		Pix pix31 = Leptonica1.pixDilate(null, pix3, sel4.getPointer());
		// pix3 = Leptonica1.pixDilateBrick(null, pix3, 2, 2);
		Pix pix32 = Leptonica1.pixSubtract(null, pix22, pix31);
		LeptUtils.dispose(pix22);
		LeptUtils.dispose(pix3);
		LeptUtils.dispose(pix31);
		if (debugL <= 1) {
			System.out.println("Reached - 2 in removeSaltPepper");
			System.out.println("Pix 32 = " + pix32);
		}

		pix4 = Leptonica1.pixHMT(null, pix32, sel3.getPointer());
		Pix pix41 = Leptonica1.pixDilate(null, pix4, sel4.getPointer());
		// pix4 = Leptonica1.pixDilateBrick(null, pix4, 2, 2);
		Pix pix42 = Leptonica1.pixSubtract(null, pix32, pix41);
		LeptUtils.dispose(pix32);
		LeptUtils.dispose(pix4);
		LeptUtils.dispose(pix41);
		if (debugL <= 1) {
			System.out.println("Reached - 3 in removeSaltPepper");
			System.out.println("Pix 42 = " + pix42);
		}

		pix5 = Leptonica1.pixHMT(null, pix42, sel1.getPointer());
		Pix pix51 = Leptonica1.pixDilate(null, pix5, sel4.getPointer());
		// pix5 = Leptonica1.pixDilateBrick(null, pix5, 2, 2);
		Pix pix52 = Leptonica1.pixSubtract(null, pix42, pix51);
		LeptUtils.dispose(pix42);
		LeptUtils.dispose(pix5);
		LeptUtils.dispose(pix51);
		if (debugL <= 1) {
			System.out.println("Reached - 4 in removeSaltPepper");
			System.out.println("Pix 52 = " + pix52);
		}

		pix6 = Leptonica1.pixHMT(null, pix52, sel2.getPointer());
		Pix pix61 = Leptonica1.pixDilate(null, pix6, sel4.getPointer());
		// pix6 = Leptonica1.pixDilateBrick(null, pix6, 2, 2);
		Pix pix62 = Leptonica1.pixSubtract(null, pix52, pix61);
		LeptUtils.dispose(pix52);
		LeptUtils.dispose(pix6);
		LeptUtils.dispose(pix61);
		if (debugL <= 1) {
			System.out.println("Reached - 5 in removeSaltPepper");
			System.out.println("Pix 62 = " + pix62);
		}

		pix7 = Leptonica1.pixHMT(null, pix62, sel5.getPointer());
		Pix pix71 = Leptonica1.pixDilate(null, pix7, sel4.getPointer());
		// pix7 = Leptonica1.pixDilateBrick(null, pix7, 2, 2);
		Pix pix72 = Leptonica1.pixSubtract(null, pix62, pix71);
		LeptUtils.dispose(pix62);
		LeptUtils.dispose(pix7);
		LeptUtils.dispose(pix71);
		if (debugL <= 1) {
			System.out.println("Reached - 6 in removeSaltPepper");
			System.out.println("Pix 72 = " + pix72);
		}

		pix8 = Leptonica1.pixHMT(null, pix72, sel2.getPointer());
		Pix pix81 = Leptonica1.pixDilate(null, pix8, sel4.getPointer());
		// pix8 = Leptonica1.pixDilateBrick(null, pix8, 2, 2);
		Pix pix82 = Leptonica1.pixSubtract(null, pix72, pix81);
		LeptUtils.dispose(pix81);
		LeptUtils.dispose(pix8);
		if (debugL <= 1) {
			System.out.println("Reached - 7 in removeSaltPepper");
			System.out.println("Pix 82 = " + pix82);
		}

		Pix pix73 = Leptonica1.pixHMT(null, pix82, sel2.getPointer());
		Pix pix74 = Leptonica1.pixDilate(null, pix72, sel4.getPointer());
		// pix7 = Leptonica1.pixDilateBrick(null, pix7, 2, 2);
		Pix pix75 = Leptonica1.pixSubtract(null, pix82, pix74);
		LeptUtils.dispose(pix73);
		LeptUtils.dispose(pix74);
		if (debugL <= 1) {
			System.out.println("Reached - 8 in removeSaltPepper");
			System.out.println("Pix 75 = " + pix75);
		}

		Pix pix83 = Leptonica1.pixHMT(null, pix75, sel3.getPointer());
		Pix pix84 = Leptonica1.pixDilate(null, pix83, sel6.getPointer());
		// pix8 = Leptonica1.pixDilateBrick(null, pix8, 3, 3);
		Pix pix85 = Leptonica1.pixSubtract(null, pix75, pix84);
		LeptUtils.dispose(pix72);
		LeptUtils.dispose(pix75);
		LeptUtils.dispose(pix82);
		LeptUtils.dispose(pix83);
		LeptUtils.dispose(pix84);
		if (debugL <= 1) {
			System.out.println("Reached - 9 in removeSaltPepper");
			System.out.println("Pix 85 = " + pix85);
		}

		pix9 = Leptonica1.pixInvert(null, pix85);
		Pix pix91 = Leptonica1.pixHMT(null, pix9, sel7.getPointer());
		Pix pix92 = Leptonica1.pixOpen(null, pix91, sel4.getPointer());
		// pix9 = Leptonica1.pixOpenCompBrick(null, pix9, 2, 2);
		LeptUtils.dispose(pix9);
		LeptUtils.dispose(pix91);
		if (debugL <= 1) {
			System.out.println("Reached - 10 in removeSaltPepper");
			System.out.println("Pix 92 = " + pix92);
		}

		Pix pix93 = Leptonica1.pixInvert(null, pix92);
		Pix pix94 = Leptonica1.pixSubtract(null, pix93, pix85);
		LeptUtils.dispose(pix85);
		LeptUtils.dispose(pix92);
		LeptUtils.dispose(pix93);
		if (debugL <= 1) {
			System.out.println("Reached - 11 in removeSaltPepper");
			System.out.println("Pix 94 = " + pix94);
		}

		pix10 = Leptonica1.pixInvert(null, pix94);
		Pix pix101 = Leptonica1.pixHMT(null, pix10, sel8.getPointer());
		Pix pix102 = Leptonica1.pixOpen(null, pix101, sel4.getPointer());
		// pix10 = Leptonica1.pixOpenCompBrick(null, pix10, 2, 2);
		Pix pix103 = Leptonica1.pixInvert(null, pix102);
		Pix pix104 = Leptonica1.pixSubtract(null, pix103, pix94);
		LeptUtils.dispose(pix94);
		LeptUtils.dispose(pix10);
		LeptUtils.dispose(pix101);
		LeptUtils.dispose(pix102);
		LeptUtils.dispose(pix103);

		if (debugL <= 1) {
			System.out.println("Reached - 12 in removeSaltPepper");
			System.out.println("Pix 104 = " + pix104);
		}

		LeptUtils.dispose(sel1);
		LeptUtils.dispose(sel2);
		LeptUtils.dispose(sel3);
		LeptUtils.dispose(sel4);
		LeptUtils.dispose(sel5);
		LeptUtils.dispose(sel6);
		LeptUtils.dispose(sel7);
		LeptUtils.dispose(sel8);

		return pix104;
	}

	public static Pix deWarp(Pix pix) throws Exception {

		if (Leptonica1.pixGetDepth(pix) != 1) {
			throw new Exception("The input image needs to be a binary image i.e. depth of 1");
		}
		L_Dewarp dew1;
		L_Dewarpa dewa;
		Pix grayImage, pixd;
		// Pix pixt1, pixt2;

		/* Run the basic functions */
		// dewa = Leptonica.INSTANCE.dewarpaCreate(2, 30, 1, 10, 30);
		dewa = Leptonica.INSTANCE.dewarpaCreate(0, 0, 1, 0, 0);
		Leptonica1.dewarpaUseBothArrays(dewa, 1);
		dew1 = Leptonica1.dewarpCreate(pix, 35);
		Leptonica1.dewarpaInsertDewarp(dewa, dew1);
		Leptonica1.dewarpaInsertRefModels(dewa, 0, 1);
		// debug file = debug_dewarp.pdf; requires IrfanView to be installed and
		// included in the path
		// Leptonica1.dewarpBuildPageModel(dew1, SBImageUtils.baseTestDir +
		// "debug_dewarp.pdf");
		Leptonica1.dewarpBuildPageModel(dew1, null);
		PointerByReference ppixd = new PointerByReference();
		grayImage = Leptonica1.pixConvertTo8(pix, 0);
		// debug file = debug_dewarp_apply.pdf; requires IrfanView to be installed and
		// included in the path
		// Leptonica1.dewarpaApplyDisparity(dewa, 35, grayImage, 200, 0, 0, ppixd,
		// SBImageUtils.baseTestDir + "debug_dewarp_apply.pdf");
		Leptonica1.dewarpaApplyDisparity(dewa, 35, grayImage, 200, 0, 0, ppixd, (String) null);

		pixd = new Pix(ppixd.getValue());
		pixd = Leptonica1.pixConvertTo1(pixd, SBImageUtils.GRAY_PIXEL_REPLACEMENT - 40);

		/* Write out some of the files to be imaged */
		/*
		 * Leptonica1.pixWrite(SBImageUtils.baseTestDir + "005.jpg", pixd,
		 * IFF_JFIF_JPEG); pixt1 = Leptonica1.pixRead("/tmp/lept/dewmod/0020.png");
		 * Leptonica1.pixWrite(SBImageUtils.baseTestDir + "006.png", pixt1, IFF_PNG);
		 * LeptUtils.dispose(pixt1); pixt1 =
		 * Leptonica1.pixRead("/tmp/lept/dewmod/0030.png");
		 * Leptonica1.pixWrite(SBImageUtils.baseTestDir + "007.png", pixt1, IFF_PNG);
		 * LeptUtils.dispose(pixt1); pixt1 =
		 * Leptonica1.pixRead("/tmp/lept/dewmod/0060.png");
		 * Leptonica1.pixWrite(SBImageUtils.baseTestDir + "008.png", pixt1, IFF_PNG);
		 * LeptUtils.dispose(pixt1); pixt1 = Leptonica1.pixRead(SBImageUtils.baseTestDir
		 * + "0070.png"); Leptonica1.pixWrite(SBImageUtils.baseTestDir + "009.png",
		 * pixt1, IFF_PNG); LeptUtils.dispose(pixt1); pixt1 =
		 * Leptonica1.pixRead("/tmp/lept/dewapply/002.png");
		 * Leptonica1.pixWrite(SBImageUtils.baseTestDir + "010.png", pixt1, IFF_PNG);
		 * LeptUtils.dispose(pixt1); pixt1 =
		 * Leptonica1.pixRead("/tmp/lept/dewapply/003.png");
		 * Leptonica1.pixWrite(SBImageUtils.baseTestDir + "011.png", pixt1, IFF_PNG);
		 * pixt2 = Leptonica1.pixThresholdToBinary(pixt1, 130);
		 * Leptonica1.pixWrite(SBImageUtils.baseTestDir + "012.png", pixt2,
		 * IFF_TIFF_G4); LeptUtils.dispose(pixt1); LeptUtils.dispose(pixt2); pixt1 =
		 * Leptonica1.pixRead("/tmp/lept/dewmod/0041.png");
		 * Leptonica1.pixWrite(SBImageUtils.baseTestDir + "013.png", pixt1, IFF_PNG);
		 * LeptUtils.dispose(pixt1); pixt1 =
		 * Leptonica1.pixRead("/tmp/lept/dewmod/0042.png");
		 * Leptonica1.pixWrite(SBImageUtils.baseTestDir + "014.png", pixt1, IFF_PNG);
		 * LeptUtils.dispose(pixt1); pixt1 =
		 * Leptonica1.pixRead("/tmp/lept/dewmod/0051.png");
		 * Leptonica1.pixWrite(SBImageUtils.baseTestDir + "015.png", pixt1, IFF_PNG);
		 * LeptUtils.dispose(pixt1); pixt1 =
		 * Leptonica1.pixRead("/tmp/lept/dewmod/0052.png");
		 * Leptonica1.pixWrite(SBImageUtils.baseTestDir + "016.png", pixt1, IFF_PNG);
		 * LeptUtils.dispose(pixt1);
		 */
		LeptUtils.dispose(dew1);
		LeptUtils.dispose(dewa);
		LeptUtils.dispose(grayImage);
		Leptonica1.lept_free(ppixd.getValue());
		return pixd;

		/*
		 * // Generate the big pdf file
		 * Leptonica1.convertFilesToPdf("/tmp/lept/dewtest", null, 135, 1.0f, 0, 0,
		 * "Dewarp Test", "/tmp/lept/dewarptest1.pdf");
		 *
		 * if (System.getProperty("os.name").toLowerCase().contains("win")) { System.out
		 * .println("pdf file made: " + System.getProperty("java.io.tmpdir") +
		 * "lept\\model\\dewarptest1.pdf"); } else {
		 * System.out.println("pdf file made: /tmp/lept/model/dewarptest1.pdf"); }
		 */

	}

	public static Pix getCleanImage(String absoluteFilePath, int divisionLength, int debug, boolean dewarp)
			throws IOException, InterruptedException, Exception {

		Pix pix = null, pix1 = null, pix2 = null, pix3 = null, pix4 = null, temp = null;
		SBImage sbi1 = null, sbi2 = null;

		Leptonica.INSTANCE.setLeptDebugOK(debug); // loads Leptonica native libraries and sets debug mode

		pix = Leptonica.INSTANCE.pixRead(absoluteFilePath);
		if (Leptonica1.pixGetDepth(pix) != 8) {
			pix = Leptonica1.pixConvertTo8(pix, 0);
		}

		// pixContrastNorm, pixBackgroundNormFlex, and pixDeskew 3 require depth of 8
		pix1 = Leptonica1.pixContrastNorm(null, pix, 96, 96, 75, 4, 4);
		pix1 = Leptonica1.pixBackgroundNormFlex(pix1, 7, 7, 3, 3, 0);
		pix1 = Leptonica1.pixDeskew(pix1, 0);

		/*
		 * System.out.println(deskewed.w + " " + deskewed.h + " " + deskewed.d + " " +
		 * deskewed.spp + " " + deskewed.wpl + " " + deskewed.refcount + " " +
		 * deskewed.xres + " " + deskewed.yres + " " + deskewed.informat + " " +
		 * deskewed.special + " " + deskewed.text + " " + deskewed.colormap + " " +
		 * deskewed.data);
		 */

		/*
		 * System.out.println(pix1.w + " " + pix1.h + " " + pix1.d + " " + pix1.spp +
		 * " " + pix1.wpl + " " + pix1.refcount + " " + pix1.xres + " " + pix1.yres +
		 * " " + pix1.informat + " " + pix1.special + " " + pix1.text + " " +
		 * pix1.colormap + " " + pix1.data);
		 */

		sbi1 = SBImage.getSBImageFromPix(pix1, debug);
		SBImage[] si = SBImageUtils.bagchiTrinarizationForPK(sbi1, sbi1.width / divisionLength,
				sbi1.height / divisionLength);
		sbi2 = SBImage.parallelStitchSubImages(si, sbi1.width / divisionLength, sbi1.height / divisionLength);

		// pix2 = Leptonica1.pixOtsuThreshOnBackgroundNorm(pix1, null, 64, 64, 90, 1024,
		// 250, 2, 2, 0.5f, null);
		// pix2 = Leptonica1.pixOtsuThreshOnBackgroundNorm(pix1, null, 32, 32, 128, 512,
		// 245, 2, 2, 0.1f, null);
		// Leptonica1.pixSauvolaBinarizeTiled(deskewed, 32, 0.3f, 1,
		// 1, null, pbr);

		pix2 = SBImage.getPixFromSBImage(sbi2);
		if (pix2.d != 1) {
			pix2 = Leptonica1.pixConvertTo1(pix2, SBImageUtils.GRAY_PIXEL_REPLACEMENT - 80);
		}
		// Hereon, the Leptonica1 functions need depth of 1
		// Occasionally, pixDeskew doesn't work. So, do a pixDeskewLocal as well.
		// Check if the result is null

		temp = Leptonica1.pixDeskewLocal(pix2, 0, 0, 0, 0.0f, 0.0f, 0.0f);
		if (temp != null) {
			pix2 = Leptonica1.pixCopy(pix2, temp);
		}
		pix3 = Leptonica1.pixOpenCompBrickDwa(null, pix2, 91, 1);
		pix3 = Leptonica1.pixDilateBrick(null, pix3, 1, 5);
		pix3 = Leptonica1.pixSubtract(null, pix2, pix3);
		pix4 = Leptonica1.pixRotate90(pix3, 1);
		pix4 = Leptonica1.pixOpenCompBrickDwa(null, pix4, 91, 1);
		pix4 = Leptonica1.pixDilateBrick(null, pix4, 1, 5);
		pix4 = Leptonica1.pixRotate90(pix4, -1);
		pix4 = Leptonica1.pixSubtract(null, pix3, pix4);
		pix4 = SBImageUtils.removeSaltPepper(pix4, 3);
		if (dewarp) {
			pix4 = SBImageUtils.deWarp(pix4);
		}

		// resource cleanup
		LeptUtils.dispose(pix);
		LeptUtils.dispose(pix1);
		LeptUtils.dispose(pix2);
		LeptUtils.dispose(temp);
		LeptUtils.dispose(pix3);

		return pix4;

	}

	public static ArrayList<ArrayList<Rectangle>> getBoundingBoxes(Pix pix, ProcessDataWrapper processDataWrapper,
			int debugL) {
		/*
		 * try { ImageUtils.writeFile(pix1, "png", SBImageUtils.baseTestDir +
		 * "BB-1.png"); } catch (Exception e) {
		 *
		 * }
		 */

		Rectangle[] bboxes = SBImageUtils.getDefaultBoxes(pix, debugL);
		ArrayList<ArrayList<Rectangle>> lines = SBImageUtils.segregateBoxesIntoLines(bboxes, processDataWrapper,
				debugL);
		int height = processDataWrapper.kdeData.mostLikelyHeight;
		int width = processDataWrapper.kdeData.mostLikelyWidth;
		// remove large boxes
		for (int i = 0; i < lines.size(); ++i) {
			for (int j = lines.get(i).size() - 1; j >= 0; --j) {
				Rectangle r = lines.get(i).get(j);
				if ((r.width > (15 * width)) || (r.height > (3 * height))) {
					lines.get(i).remove(j);
				}
			}
		}
		// remove empty lines
		for (int i = lines.size() - 1; i >= 0; --i) {
			if (lines.get(i).size() == 0) {
				lines.remove(i);
			}
		}
		// sort boxes in each line by the x-coordinate
		for (ArrayList<Rectangle> line : lines) {
			Collections.sort(line, new Comparator<Rectangle>() {

				@Override
				public int compare(Rectangle r1, Rectangle r2) {
					return (r1.x - r2.x);
				}

			});
		}

		return lines;

		// System.out.println(Arrays.toString(bboxes));

		/*
		 * sel1 = Leptonica1.selCreateBrick(3, 3, 0, 0, ILeptonica.SEL_HIT); sel2 =
		 * Leptonica1.selCreateBrick(3, 3, 0, 0, ILeptonica.SEL_HIT); pix1 =
		 * Leptonica1.pixClose(null, pix, sel1.getPointer()); pix1 =
		 * Leptonica1.pixClose(null, pix1, sel2.getPointer()); PointerByReference pixa =
		 * null; PointerByReference blockids = null; TesseractHandle tesseractHandle =
		 * SBImageUtils.getTesseractHandle(); TessBaseAPI handle =
		 * tesseractHandle.getHandle(); TessAPI1.TessBaseAPISetImage2(handle, pix1);
		 * Boxa boxes = TessAPI1.TessBaseAPIGetComponentImages(handle,
		 * TessPageIteratorLevel.RIL_SYMBOL, ITessAPI.TRUE, pixa, blockids); int
		 * boxCount = Leptonica1.boxaGetCount(boxes); System.out.println("Box Count = "
		 * + boxCount); pix2 = Leptonica1.pixCopy(null, pix); for (int j = 0; j <
		 * boxCount; ++j) { Box box = Leptonica1.boxaGetBox(boxes, j,
		 * ILeptonica.L_CLONE); if (box == null) { continue; }
		 * System.out.println("Box - " + box.x + "; " + box.y + "; " + box.h + "; " +
		 * box.w); Leptonica1.pixRenderBox(pix1, box, 1, ILeptonica.L_FLIP_PIXELS);
		 * LeptUtils.dispose(box); }
		 *
		 * try { ImageUtils.writeFile(pix1, "png", SBImageUtils.baseTestDir +
		 * "pix6.png"); } catch (Exception e) {
		 *
		 * } SBImageUtils.releaseTesseractHandle(tesseractHandle);
		 */

		/*
		 * sel3 = Leptonica1.selCreateBrick(2, 2, 0, 0, ILeptonica.SEL_HIT); sel4 =
		 * Leptonica1.selCreateBrick(2, 2, 0, 0, ILeptonica.SEL_HIT); pix3 =
		 * Leptonica1.pixClose(null, pix, sel3.getPointer()); pix3 =
		 * Leptonica1.pixClose(null, pix3, sel4.getPointer());
		 *
		 * tesseractHandle = SBImageUtils.getTesseractHandle(); handle =
		 * tesseractHandle.getHandle(); TessAPI1.TessBaseAPISetImage2(handle, pix3);
		 *
		 * PointerByReference pixa1 = null; PointerByReference blockids1 = null; Boxa
		 * boxes1 = TessAPI1.TessBaseAPIGetComponentImages(handle,
		 * TessPageIteratorLevel.RIL_SYMBOL, ITessAPI.TRUE, pixa1, blockids1); boxCount
		 * = Leptonica1.boxaGetCount(boxes1); System.out.println("Box Count = " +
		 * boxCount); pix4 = Leptonica1.pixCopy(null, pix); for (int j = 0; j <
		 * boxCount; ++j) { Box box = Leptonica1.boxaGetBox(boxes1, j,
		 * ILeptonica.L_CLONE); if (box == null) { continue; }
		 * System.out.println("Box - " + box.x + "; " + box.y + "; " + box.h + "; " +
		 * box.w); Leptonica1.pixRenderBox(pix3, box, 1, ILeptonica.L_FLIP_PIXELS);
		 * LeptUtils.dispose(box); }
		 *
		 * try { ImageUtils.writeFile(pix3, "png", SBImageUtils.baseTestDir +
		 * "pix7.png"); } catch (Exception e) {
		 *
		 * }
		 */

		/*
		 * LeptUtils.dispose(sel1); LeptUtils.dispose(sel2); LeptUtils.dispose(sel3);
		 * LeptUtils.dispose(sel4); LeptUtils.dispose(pix1); LeptUtils.dispose(pix2);
		 * LeptUtils.dispose(pix3); LeptUtils.dispose(pix4);
		 */
	}

	public static Pix getCleanImage(File file, int divisionLength, int debug, boolean dewarp)
			throws IOException, InterruptedException, Exception {
		return getCleanImage(file.getAbsolutePath(), divisionLength, debug, dewarp);
	}

	public static Rectangle[] getDefaultBoxesFromTesseract(Pix pix) {
		// PointerByReference ppixa = new PointerByReference();
		System.out.println(TessAPI1.TessVersion());
		TessBaseAPI handle = TessAPI1.TessBaseAPICreate();
		String datapath = "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata";
		String language = "eng";
		TessAPI1.TessBaseAPIInit3(handle, datapath, language);
		TessAPI1.TessBaseAPISetPageSegMode(handle, TessPageSegMode.PSM_AUTO_OSD);
		TessAPI1.TessBaseAPISetImage2(handle, pix);
		TessResultIterator ri = TessAPI1.TessBaseAPIGetIterator(handle);
		TessPageIterator pi = TessAPI1.TessResultIteratorGetPageIterator(ri);
		Pix newPix = TessAPI1.TessPageIteratorGetBinaryImage(pi, TessAPI1.TessPageIteratorLevel.RIL_TEXTLINE);
		int connectivity = 8;
		Leptonica lept = Leptonica.INSTANCE;
		Boxa result = Leptonica1.pixConnCompBB(newPix, connectivity);
		// TessAPI1.TessBaseAPISetImage2(handle, newPix);
		// Boxa result = TessAPI1.TessBaseAPIGetConnectedComponents(handle, ppixa);
		Boxa sortedResult = Leptonica1.boxaSort(result, ILeptonica.L_SORT_BY_Y, ILeptonica.L_SORT_INCREASING, null);
		LeptUtils.dispose(newPix);
		// Leptonica1.lept_free(ppixa.getValue());
		// System.out.println("Count of sorted boxes = " +
		// Leptonica1.boxaGetCount(sortedResult));
		// Leptonica1.boxaWrite(dir + "Boxes.ba", sortedResult);
		// System.out.println("Count of valid boxes = " +
		// Leptonica1.boxaGetValidCount(sortedResult));

		Rectangle[] wordRectangles = new Rectangle[Leptonica1.boxaGetCount(sortedResult)];
		for (int j = 0; j < wordRectangles.length; ++j) {
			Box box = Leptonica1.boxaGetBox(sortedResult, j, ILeptonica.L_CLONE);
			/*
			 * Leptonica1.pixRenderBox(pix, box, 1, ILeptonica.L_FLIP_PIXELS); // to draw
			 * the boxes on the Pix
			 */
			// IntBuffer x = IntBuffer.allocate(1);
			// IntBuffer y = IntBuffer.allocate(1);
			// IntBuffer w = IntBuffer.allocate(1);
			// IntBuffer h = IntBuffer.allocate(1);
			// Leptonica1.boxGetGeometry(box, x, y, w, h);
			// wordRectangles[j] = new Rectangle(x.get(), y.get(), w.get(), h.get());
			wordRectangles[j] = new Rectangle(box.x, box.y, box.w, box.h);
			LeptUtils.dispose(box);
		}

		LeptUtils.dispose(result);
		LeptUtils.dispose(sortedResult);

		return wordRectangles;

	}

	public static ArrayList<Rectangle> getBoxesInFinalPix(Pix pix, int referenceHeight, int referenceWidth,
			boolean dilateAndDo, int debugL) {
		if (pix == null) {
			System.out.println("In getBoxesInFinalPix() : input pix = null");
			return new ArrayList<Rectangle>();
		}
		if (debugL <= 2) {
			System.out.println("Depth of pix = " + Leptonica1.pixGetDepth(pix));
			System.out.println("referenceHeight = " + referenceHeight);
			System.out.println("referenceWidth = " + referenceWidth);
		}
		// PointerByReference ppixa = new PointerByReference();
		int connectivity = 4;
		ArrayList<Rectangle> wordRectangles = new ArrayList<>();

		Pix pix1D = Leptonica1.pixConvertTo1(pix, 100);
		if (debugL <= 2) {
			System.out.println("In getBoxesInFinalPix() : pix1D before getting result = " + pix1D);
		}
		Boxa result = Leptonica1.pixConnCompBB(pix1D, connectivity);
		if (debugL <= 2) {
			System.out.println("In getBoxesInFinalPix() : after getting result = " + result);
		}
		Boxa sortedResult = Leptonica1.boxaSort(result, ILeptonica.L_SORT_BY_Y, ILeptonica.L_SORT_INCREASING, null);

		for (int j = 0; j < Leptonica1.boxaGetCount(sortedResult); ++j) {
			Box box = Leptonica1.boxaGetBox(sortedResult, j, ILeptonica.L_CLONE);
			wordRectangles.add(new Rectangle(box.x, box.y, box.w, box.h));
			LeptUtils.dispose(box);
		}

		LeptUtils.dispose(pix1D);
		LeptUtils.dispose(result);
		LeptUtils.dispose(sortedResult);
		// Leptonica1.lept_free(ppixa.getValue());

		int initialSize = wordRectangles.size();
		if (debugL <= 2) {
			System.out.println("referenceHeight = " + referenceHeight);
			System.out.println("In getBoxesInFinalPix(), the round 1 number of words = " + initialSize);
			System.out.println("Before round 1 removal, the boxes are - " + wordRectangles);
		}

		for (int i = initialSize - 1; i >= 0; --i) {
			// 66% * referenceHeight * 20% (rounded to the nearest 0.05
			if (wordRectangles.get(i).width < (0.15 * referenceHeight)) {
				wordRectangles.remove(i);
			}
		}

		initialSize = wordRectangles.size();
		for (int i = initialSize - 1; i >= 0; --i) {
			if ((wordRectangles.get(i).height < (0.6 * referenceHeight))
					|| (((wordRectangles.get(i).width < (0.3 * referenceWidth)))
							&& (wordRectangles.get(i).height < (0.75 * referenceHeight)))
					|| ((wordRectangles.get(i).width < (0.2 * referenceWidth)))) {
				wordRectangles.remove(i);
			}
		}
		if (debugL <= 2) {
			System.out
					.println("In getBoxesInFinalPix(), the round 1 number of words after removing small height words = "
							+ wordRectangles.size());
			System.out.println("After round 1, the boxes are - " + wordRectangles);
		}

		// Pix finalPix = null, finalPix8 = null, finalPix1 = null;
		Pix pix2Gray = null, pix2D = null;
		Pix pix3Gray = null, pix3D = null;
		Boxa result1 = null, result2 = null;
		Boxa sortedResult1 = null, sortedResult2 = null;

		// if majority of the boxes have been chucked out, then dilate the pic with a
		// (2,3) sel and
		// repopulate the wordRectangles
		if (((wordRectangles.size() * 1.0) / (initialSize + 1)) < 0.8) {
			wordRectangles.clear();
			int xD = 2;
			int yD = 2;
			// PointerByReference ppixb = new PointerByReference();
			pix2Gray = Leptonica1.pixErodeGray(pix, xD, yD);
			/*
			 * finalPix8 = Leptonica1.pixCreate(Leptonica1.pixGetWidth(pix1) + (2 * border),
			 * Leptonica1.pixGetHeight(pix1) + (2 * border), 8);
			 * Leptonica1.pixSetBlackOrWhite(finalPix8, ILeptonica.L_SET_WHITE);
			 * Leptonica1.pixRasterop(finalPix8, border, border,
			 * Leptonica1.pixGetWidth(pix1), Leptonica1.pixGetHeight(pix1),
			 * ILeptonica.PIX_PAINT, pix1, 0, 0);
			 */
			pix2D = Leptonica1.pixConvertTo1(pix2Gray, 136);
			result1 = Leptonica1.pixConnCompBB(pix2D, connectivity);
			sortedResult1 = Leptonica1.boxaSort(result1, ILeptonica.L_SORT_BY_Y, ILeptonica.L_SORT_INCREASING, null);
			for (int j = 0; j < Leptonica1.boxaGetCount(sortedResult1); ++j) {
				Box box = Leptonica1.boxaGetBox(sortedResult1, j, ILeptonica.L_CLONE);
				wordRectangles
						.add(new Rectangle(box.x + (xD / 2), box.y + (yD / 2), box.w - (xD - 1), box.h - (yD - 1)));
				LeptUtils.dispose(box);
			}

			LeptUtils.dispose(pix2Gray);
			LeptUtils.dispose(pix2D);
			LeptUtils.dispose(result1);
			LeptUtils.dispose(sortedResult1);
			// Leptonica1.lept_free(ppixb.getValue());

			initialSize = wordRectangles.size();

			for (int i = initialSize - 1; i >= 0; --i) {
				if (wordRectangles.get(i).width < (0.2 * referenceHeight)) {
					wordRectangles.remove(i);
				}
			}

			initialSize = wordRectangles.size();

			if (debugL <= 2) {
				System.out.println("In getBoxesInFinalPix(), the round 2 number of words = " + initialSize);
			}
			for (int i = initialSize - 1; i >= 0; --i) {
				if ((wordRectangles.get(i).height < (0.4 * referenceHeight))
						|| (wordRectangles.get(i).width < (0.3 * referenceWidth))) {
					wordRectangles.remove(i);
				}
			}
			if (debugL <= 2) {
				System.out.println(
						"In getBoxesInFinalPix(), the round 2 number of words after removing small height words = "
								+ wordRectangles.size());
			}
			// if majority of the boxes have still been chucked out, then dilate the pic
			// with 2 (3,3) sels and
			// repopulate the wordRectangles

			if (((wordRectangles.size() * 1.0) / (initialSize + 1)) < 0.8) {
				if (dilateAndDo) {
					wordRectangles.clear();
					xD = 2;
					yD = 4;
					// PointerByReference ppixc = new PointerByReference();
					pix3Gray = Leptonica1.pixErodeGray(pix, xD, yD);
					pix3D = Leptonica1.pixConvertTo1(pix3Gray, 136);
					result2 = Leptonica1.pixConnCompBB(pix3D, connectivity);
					sortedResult2 = Leptonica1.boxaSort(result2, ILeptonica.L_SORT_BY_Y, ILeptonica.L_SORT_INCREASING,
							null);
					for (int j = 0; j < Leptonica1.boxaGetCount(sortedResult2); ++j) {
						Box box = Leptonica1.boxaGetBox(sortedResult2, j, ILeptonica.L_CLONE);
						wordRectangles.add(
								new Rectangle(box.x + (xD / 2), box.y + (yD / 2), box.w - (xD - 1), box.h - (yD - 1)));
						LeptUtils.dispose(box);
					}

					LeptUtils.dispose(pix3Gray);
					LeptUtils.dispose(pix3D);
					LeptUtils.dispose(result2);
					LeptUtils.dispose(sortedResult2);
					// Leptonica1.lept_free(ppixc.getValue());

					initialSize = wordRectangles.size();

					for (int i = initialSize - 1; i >= 0; --i) {
						if (wordRectangles.get(i).width < (0.2 * referenceHeight)) {
							wordRectangles.remove(i);
						}
					}

					initialSize = wordRectangles.size();

					for (int i = initialSize - 1; i >= 0; --i) {
						if ((wordRectangles.get(i).height < (0.4 * referenceHeight))
								|| (wordRectangles.get(i).width < (0.3 * referenceWidth))) {
							wordRectangles.remove(i);
						}
					}
					if (debugL <= 2) {
						System.out.println(
								"In getBoxesInFinalPix(), the round 3 number of words = " + wordRectangles.size());
					}
				} else {
					wordRectangles = null;
				}
			}
		}

		if (debugL <= 2) {
			System.out.println("Box rectangles found in getBoxesInFinalPix() are - " + wordRectangles);
		}

		return wordRectangles;

	}

	public static Rectangle[] getDefaultBoxes(Pix pix, int debugL) {

		Sel sel1 = null, sel2 = null;
		Pix pix1 = null, pix2 = null;

		sel1 = Leptonica1.selCreateBrick(3, 3, 1, 1, ILeptonica.SEL_HIT);
		sel2 = Leptonica1.selCreateBrick(3, 3, 1, 1, ILeptonica.SEL_HIT);
		pix1 = Leptonica1.pixClose(null, pix, sel1.getPointer());
		pix2 = Leptonica1.pixDilate(null, pix1, sel2.getPointer());

		// PointerByReference ppixa = new PointerByReference();
		int connectivity = 4;
		Boxa result = Leptonica1.pixConnCompBB(pix2, connectivity);
		Boxa sortedResult = Leptonica1.boxaSort(result, ILeptonica.L_SORT_BY_Y, ILeptonica.L_SORT_INCREASING, null);
		// System.out.println("Count of sorted boxes = " +
		// Leptonica1.boxaGetCount(sortedResult));
		// Leptonica1.boxaWrite(dir + "Boxes.ba", sortedResult);
		// System.out.println("Count of valid boxes = " +
		// Leptonica1.boxaGetValidCount(sortedResult));

		LeptUtils.dispose(sel1);
		LeptUtils.dispose(sel2);
		LeptUtils.dispose(pix1);
		LeptUtils.dispose(pix2);
		// Leptonica1.lept_free(ppixa.getValue());

		int numberOfBoxes = Leptonica1.boxaGetCount(sortedResult);

		ArrayList<Rectangle> wordRectangles = new ArrayList<>();
		for (int j = 0; j < numberOfBoxes; ++j) {
			Box box = Leptonica1.boxaGetBox(sortedResult, j, ILeptonica.L_CLONE);
			/*
			 * Leptonica1.pixRenderBox(pix, box, 1, ILeptonica.L_FLIP_PIXELS); // to draw
			 * the boxes on the Pix
			 */
			// IntBuffer x = IntBuffer.allocate(1);
			// IntBuffer y = IntBuffer.allocate(1);
			// IntBuffer w = IntBuffer.allocate(1);
			// IntBuffer h = IntBuffer.allocate(1);
			// Leptonica1.boxGetGeometry(box, x, y, w, h);
			// wordRectangles[j] = new Rectangle(x.get(), y.get(), w.get(), h.get());
			wordRectangles.add(new Rectangle(box.x, box.y, box.w, box.h));
			LeptUtils.dispose(box);
		}

		if (debugL <= 2) {
			Pix pix3 = Leptonica1.pixCopy(null, pix);
			Leptonica1.pixWrite(SBImageUtils.baseTestDir + "bbOnDilated.png", pix3, ILeptonica.IFF_PNG);
			LeptUtils.dispose(pix3);
			System.out.println("Box rectangles found in getDefaultBoxes() are - " + wordRectangles);
		}

		LeptUtils.dispose(result);
		LeptUtils.dispose(sortedResult);

		// remove boxes of dubious height that are touching the Pix Top or Bottom edge

		DescriptiveStatistics nonSuspectHeightStats = new DescriptiveStatistics();
		DescriptiveStatistics suspectHeightStats = new DescriptiveStatistics();
		ArrayList<Rectangle> suspectWordsAtEdge = new ArrayList<>();

		int edgeTolerance = 5;
		int pixHeight = Leptonica1.pixGetHeight(pix);
		for (Rectangle word : wordRectangles) {
			if ((word.y <= edgeTolerance) || ((word.y + word.height) >= (pixHeight - edgeTolerance))) {
				suspectWordsAtEdge.add(word);
				suspectHeightStats.addValue(word.height);
			} else {
				nonSuspectHeightStats.addValue(word.height);
			}
		}

		double nonSuspectMedianHeight = nonSuspectHeightStats.getPercentile(50);
		double nonSuspectMeanHeight = nonSuspectHeightStats.getMean();
		double comparisonHeight = Math.max(nonSuspectMedianHeight, nonSuspectMeanHeight);
		double minAcceptableRatioForHeight = 0.7;

		for (int i = suspectWordsAtEdge.size() - 1; i >= 0; --i) {
			if (suspectWordsAtEdge.get(i).height >= (minAcceptableRatioForHeight * comparisonHeight)) {
				// remove the ok words, leaving behind all the suspect words
				Rectangle removed = suspectWordsAtEdge.remove(i);
				if (debugL <= 2) {
					System.out.println("Removed suspect word at edge in getDefaultBoxes() " + removed);
				}
			}
		}

		ArrayList<Rectangle> finalWords = new ArrayList<>();
		nonSuspectHeightStats.clear();
		for (Rectangle word : wordRectangles) {
			if (suspectWordsAtEdge.indexOf(word) == -1) {
				finalWords.add(word);
				nonSuspectHeightStats.addValue(word.height);
			}
		}

		double referenceHeight = nonSuspectHeightStats.getPercentile(50);
		int initialSize = finalWords.size();
		if (debugL <= 2) {
			System.out.println("referenceHeight = " + referenceHeight);
			System.out.println("In getDefaultBoxes(), the round 0 number of words = " + initialSize);
			System.out.println("Before round 0 removal, the boxes are - " + finalWords);
		}
		for (int i = initialSize - 1; i >= 0; --i) {
			if (finalWords.get(i).height < (0.35 * referenceHeight)) {
				finalWords.remove(i);
			}
		}

		Pix newPix = Leptonica1.pixCreate(Leptonica1.pixGetWidth(pix), Leptonica1.pixGetHeight(pix), 1);
		Leptonica1.pixSetBlackOrWhite(newPix, ILeptonica.L_SET_WHITE);
		for (Rectangle word : finalWords) {
			Box aBox = Leptonica1.boxCreate(word.x, word.y, word.width, word.height);
			Pix aPix = Leptonica1.pixClipRectangle(pix, aBox, null);
			Leptonica1.pixRasterop(newPix, word.x, word.y, word.width, word.height, ILeptonica.PIX_SRC, aPix, 0, 0);
			LeptUtils.dispose(aBox);
			LeptUtils.dispose(aPix);
		}

		if (debugL <= 3) {
			Leptonica1.pixWrite(SBImageUtils.baseTestDir + "newPixForDefaultBoxes.png", newPix, ILeptonica.IFF_PNG);
		}

		Boxa result1 = Leptonica1.pixConnCompBB(newPix, connectivity);
		Boxa sortedResult1 = Leptonica1.boxaSort(result1, ILeptonica.L_SORT_BY_Y, ILeptonica.L_SORT_INCREASING, null);

		ArrayList<Rectangle> newWordRectangles = new ArrayList<>();
		nonSuspectHeightStats.clear();
		for (int j = 0; j < Leptonica1.boxaGetCount(sortedResult1); ++j) {
			Box box = Leptonica1.boxaGetBox(sortedResult1, j, ILeptonica.L_CLONE);
			newWordRectangles.add(new Rectangle(box.x, box.y, box.w, box.h));
			nonSuspectHeightStats.addValue(box.h);
			LeptUtils.dispose(box);
		}

		LeptUtils.dispose(result1);
		LeptUtils.dispose(sortedResult1);

		if (nonSuspectHeightStats.getPercentile(50) > (0.7 * referenceHeight)) {
			referenceHeight = nonSuspectHeightStats.getPercentile(50);
		}

		initialSize = newWordRectangles.size();
		if (debugL <= 2) {
			System.out.println("Changed referenceHeight = " + referenceHeight);
			System.out
					.println("In getDefaultBoxes(), the round 1 number of words in newWordRectangles = " + initialSize);
			System.out.println("Before round 1 removal, the boxes in newWordRectangles are - " + newWordRectangles);
		}

		for (int i = initialSize - 1; i >= 0; --i) {
			if (newWordRectangles.get(i).height < (0.35 * referenceHeight)) {
				newWordRectangles.remove(i);
			}
		}
		if (debugL <= 2) {
			System.out.println("In getDefaultBoxes(), the round 1 number of words after removing small height words = "
					+ newWordRectangles.size());
			System.out.println("After round 1, the boxes are - " + newWordRectangles);
		}

		if (((newWordRectangles.size() * 1.0) / (initialSize + 1)) > 0.95) {
			LeptUtils.dispose(newPix);
			return newWordRectangles.toArray(new Rectangle[finalWords.size()]);
		}

		// Pix finalPix = null, finalPix8 = null, finalPix1 = null;
		Pix pix2D = null;
		Boxa result2 = null;
		Boxa sortedResult2 = null;

		// if majority of the boxes have been chucked out, then dilate the pic with a
		// (2,3) sel and
		// repopulate the wordRectangles

		// if (((newWordRectangles.size() * 1.0) / (initialSize + 1)) < 0.85) {
		newWordRectangles.clear();
		int xD = 1;
		int yD = 2;
		// PointerByReference ppixb = new PointerByReference();
		pix2D = Leptonica1.pixDilateBrick(null, newPix, xD, yD);
		result2 = Leptonica1.pixConnCompBB(pix2D, connectivity);
		sortedResult2 = Leptonica1.boxaSort(result2, ILeptonica.L_SORT_BY_Y, ILeptonica.L_SORT_INCREASING, null);
		for (int j = 0; j < Leptonica1.boxaGetCount(sortedResult2); ++j) {
			Box box = Leptonica1.boxaGetBox(sortedResult2, j, ILeptonica.L_CLONE);
			newWordRectangles
					.add(new Rectangle(box.x + (xD / 2), box.y + (yD / 2), box.w - (xD - 1), box.h - (yD - 1)));
			LeptUtils.dispose(box);
		}

		LeptUtils.dispose(pix2D);
		LeptUtils.dispose(result2);
		LeptUtils.dispose(sortedResult2);
		// Leptonica1.lept_free(ppixb.getValue());

		initialSize = newWordRectangles.size();
		if (debugL <= 2) {
			System.out.println("In getDefaultBoxes(), the round 2 number of words = " + initialSize);
		}
		for (int i = initialSize - 1; i >= 0; --i) {
			if (newWordRectangles.get(i).height < (0.35 * referenceHeight)) {
				newWordRectangles.remove(i);
			}
		}
		if (debugL <= 2) {
			System.out.println("In getDefaultBoxes(), the round 2 number of words after removing small height words = "
					+ newWordRectangles.size());
		}
		// if majority of the boxes are retained, then return newWordRectangles
		// else, return finalWords

		if (((newWordRectangles.size() * 1.0) / (initialSize + 1)) >= 0.825) {
			LeptUtils.dispose(newPix);
			return newWordRectangles.toArray(new Rectangle[finalWords.size()]);
		}
		// }

		// if the above doesn't work, then try again with higher xD

		Pix pix5D = null;
		Boxa result5 = null;
		Boxa sortedResult5 = null;

		newWordRectangles.clear();
		xD = 2;
		yD = 3;
		// PointerByReference ppixe = new PointerByReference();
		pix5D = Leptonica1.pixDilateBrick(null, newPix, xD, yD);
		result5 = Leptonica1.pixConnCompBB(pix5D, connectivity);
		sortedResult5 = Leptonica1.boxaSort(result5, ILeptonica.L_SORT_BY_Y, ILeptonica.L_SORT_INCREASING, null);
		for (int j = 0; j < Leptonica1.boxaGetCount(sortedResult5); ++j) {
			Box box = Leptonica1.boxaGetBox(sortedResult5, j, ILeptonica.L_CLONE);
			newWordRectangles
					.add(new Rectangle(box.x + (xD / 2), box.y + (yD / 2), box.w - (xD - 1), box.h - (yD - 1)));
			LeptUtils.dispose(box);
		}

		LeptUtils.dispose(pix5D);
		LeptUtils.dispose(result5);
		LeptUtils.dispose(sortedResult5);
		// Leptonica1.lept_free(ppixe.getValue());

		initialSize = newWordRectangles.size();
		if (debugL <= 2) {
			System.out.println("In getDefaultBoxes(), the round 3 number of words = " + initialSize);
		}
		for (int i = initialSize - 1; i >= 0; --i) {
			if (newWordRectangles.get(i).height < (0.35 * referenceHeight)) {
				newWordRectangles.remove(i);
			}
		}
		if (debugL <= 2) {
			System.out.println("In getDefaultBoxes(), the round 3 number of words after removing small height words = "
					+ newWordRectangles.size());
		}
		// if majority of the boxes are retained, then return newWordRectangles
		// else, return finalWords

		if (((newWordRectangles.size() * 1.0) / (initialSize + 1)) >= 0.825) {
			LeptUtils.dispose(newPix);
			return newWordRectangles.toArray(new Rectangle[finalWords.size()]);
		}
		// }

		// if the above doesn't work, then try again with higher yD

		Pix pix3D = null;
		Boxa result3 = null;
		Boxa sortedResult3 = null;

		newWordRectangles.clear();
		xD = 2;
		yD = 4;
		// PointerByReference ppixc = new PointerByReference();
		pix3D = Leptonica1.pixDilateBrick(null, newPix, xD, yD);
		result3 = Leptonica1.pixConnCompBB(pix3D, connectivity);
		sortedResult3 = Leptonica1.boxaSort(result3, ILeptonica.L_SORT_BY_Y, ILeptonica.L_SORT_INCREASING, null);
		for (int j = 0; j < Leptonica1.boxaGetCount(sortedResult3); ++j) {
			Box box = Leptonica1.boxaGetBox(sortedResult3, j, ILeptonica.L_CLONE);
			newWordRectangles
					.add(new Rectangle(box.x + (xD / 2), box.y + (yD / 2), box.w - (xD - 1), box.h - (yD - 1)));
			LeptUtils.dispose(box);
		}

		LeptUtils.dispose(pix3D);
		LeptUtils.dispose(result3);
		LeptUtils.dispose(sortedResult3);
		// Leptonica1.lept_free(ppixc.getValue());

		initialSize = newWordRectangles.size();
		if (debugL <= 2) {
			System.out.println("In getDefaultBoxes(), the round 4 number of words = " + initialSize);
		}
		for (int i = initialSize - 1; i >= 0; --i) {
			if (newWordRectangles.get(i).height < (0.35 * referenceHeight)) {
				newWordRectangles.remove(i);
			}
		}
		if (debugL <= 2) {
			System.out.println("In getDefaultBoxes(), the round 4 number of words after removing small height words = "
					+ newWordRectangles.size());
		}
		// if majority of the boxes are retained, then return newWordRectangles
		// else, return finalWords

		if (((newWordRectangles.size() * 1.0) / (initialSize + 1)) >= 0.825) {
			LeptUtils.dispose(newPix);
			return newWordRectangles.toArray(new Rectangle[finalWords.size()]);
		}
		// }

		// if the above doesn't work, then try again with higher xD

		Pix pix4D = null;
		Boxa result4 = null;
		Boxa sortedResult4 = null;

		newWordRectangles.clear();
		xD = 3;
		yD = 4;
		// PointerByReference ppixd = new PointerByReference();
		pix4D = Leptonica1.pixDilateBrick(null, newPix, xD, yD);
		result4 = Leptonica1.pixConnCompBB(pix4D, connectivity);
		sortedResult4 = Leptonica1.boxaSort(result4, ILeptonica.L_SORT_BY_Y, ILeptonica.L_SORT_INCREASING, null);
		for (int j = 0; j < Leptonica1.boxaGetCount(sortedResult4); ++j) {
			Box box = Leptonica1.boxaGetBox(sortedResult4, j, ILeptonica.L_CLONE);
			newWordRectangles
					.add(new Rectangle(box.x + (xD / 2), box.y + (yD / 2), box.w - (xD - 1), box.h - (yD - 1)));
			LeptUtils.dispose(box);
		}

		LeptUtils.dispose(pix4D);
		LeptUtils.dispose(result4);
		LeptUtils.dispose(sortedResult4);
		// Leptonica1.lept_free(ppixd.getValue());

		initialSize = newWordRectangles.size();
		if (debugL <= 2) {
			System.out.println("In getDefaultBoxes(), the round 5 number of words = " + initialSize);
		}
		for (int i = initialSize - 1; i >= 0; --i) {
			if (newWordRectangles.get(i).height < (0.35 * referenceHeight)) {
				newWordRectangles.remove(i);
			}
		}
		if (debugL <= 2) {
			System.out.println("In getDefaultBoxes(), the round 5 number of words after removing small height words = "
					+ newWordRectangles.size());
		}
		// if majority of the boxes are retained, then return newWordRectangles
		// else, return finalWords

		if (((newWordRectangles.size() * 1.0) / (initialSize + 1)) >= 0.825) {
			LeptUtils.dispose(newPix);
			return newWordRectangles.toArray(new Rectangle[finalWords.size()]);
		}
		// }
		LeptUtils.dispose(newPix);
		return finalWords.toArray(new Rectangle[finalWords.size()]);

	}

	public static Rectangle[] getDefaultBoxes1(Pix pix, int debugL) {
		// PointerByReference ppixa = new PointerByReference();
		int connectivity = 8;
		Pix pix1 = Leptonica1.pixDilateBrick(null, pix, 2, 5);
		Boxa result = Leptonica1.pixConnCompBB(pix1, connectivity);
		Boxa sortedResult = Leptonica1.boxaSort(result, ILeptonica.L_SORT_BY_Y, ILeptonica.L_SORT_INCREASING, null);
		// System.out.println("Count of sorted boxes = " +
		// Leptonica1.boxaGetCount(sortedResult));
		// Leptonica1.boxaWrite(dir + "Boxes.ba", sortedResult);
		// System.out.println("Count of valid boxes = " +
		// Leptonica1.boxaGetValidCount(sortedResult));

		Rectangle[] wordRectangles = new Rectangle[Leptonica1.boxaGetCount(sortedResult)];
		for (int j = 0; j < wordRectangles.length; ++j) {
			Box box = Leptonica1.boxaGetBox(sortedResult, j, ILeptonica.L_CLONE);
			/*
			 * Leptonica1.pixRenderBox(pix, box, 1, ILeptonica.L_FLIP_PIXELS); // to draw
			 * the boxes on the Pix
			 */
			// IntBuffer x = IntBuffer.allocate(1);
			// IntBuffer y = IntBuffer.allocate(1);
			// IntBuffer w = IntBuffer.allocate(1);
			// IntBuffer h = IntBuffer.allocate(1);
			// Leptonica1.boxGetGeometry(box, x, y, w, h);
			// wordRectangles[j] = new Rectangle(x.get(), y.get(), w.get(), h.get());
			wordRectangles[j] = new Rectangle(box.x, box.y, box.w, box.h);
			LeptUtils.dispose(box);
		}

		if (debugL <= 2) {
			Pix pix2 = Leptonica1.pixCopy(null, pix);
			Leptonica1.pixWrite(SBImageUtils.baseTestDir + "bbOnDilated.png", pix2, ILeptonica.IFF_PNG);
			LeptUtils.dispose(pix2);
		}

		LeptUtils.dispose(pix1);
		LeptUtils.dispose(result);
		LeptUtils.dispose(sortedResult);
		// Leptonica1.lept_free(ppixa.getValue());

		return wordRectangles;

	}

	public static Pix[] getDefaultPixs(Pix pix) {
		// be careful to call disposePixArray() after using the pixes from this method
		PointerByReference ppixa = new PointerByReference();
		int connectivity = 8;
		Boxa result = Leptonica1.pixConnComp(pix, ppixa, connectivity);
		Boxa sortedResult = Leptonica1.boxaSort(result, ILeptonica.L_SORT_BY_Y, ILeptonica.L_SORT_INCREASING, null);
		int noOfBoxes = Leptonica1.boxaGetCount(sortedResult);
		Pixa pixArray = new Pixa(ppixa.getValue());
		// Leptonica1.pixaWrite(dir + "Pixes.ba", pixArray);
		Pixa sortedPixes = Leptonica1.pixaSort(pixArray, ILeptonica.L_SORT_BY_Y, ILeptonica.L_SORT_INCREASING, null,
				ILeptonica.L_COPY);
		Pix[] pixes = new Pix[noOfBoxes];
		for (int j = 0; j < noOfBoxes; ++j) {
			pixes[j] = Leptonica1.pixaGetPix(sortedPixes, j, ILeptonica.L_COPY);
			// ImageUtils.writeFile(newPix, "png", dir + "pix-" + j + ".png");
		}
		LeptUtils.dispose(result);
		LeptUtils.dispose(sortedResult);
		LeptUtils.dispose(pixArray);
		LeptUtils.dispose(sortedPixes);
		Leptonica1.lept_free(ppixa.getValue());
		return pixes;
	}

	public static void disposePixArray(Pix[] pixes) {
		for (Pix pix : pixes) {
			LeptUtils.dispose(pix);
		}
	}

	/*
	 * public static TesseractHandle getTesseractHandle() { TesseractHandle
	 * handleWrapper = null; try { handleWrapper = (TesseractHandle)
	 * tesseractPool.borrowObject(); } catch (Exception e) { e.printStackTrace(); }
	 * return handleWrapper; }
	 */

	/*
	 * public static void releaseTesseractHandle(TesseractHandle handle) {
	 * tesseractPool.returnObject(handle); }
	 *
	 */

	public static Rectangle[] mergeRectanglesTrial(Rectangle[] letters) {
		// Given the rectangles corresponding to Box coordinates of letters, it returns
		// the Rectangles of the words
		// TBD

		for (Rectangle letter : letters) {
			System.out.println(letter);
		}

		int minYDifference = 8;
		int minHeight = 10;
		// int minWidth = 10;

		ArrayList<Integer> yStartingCoords = new ArrayList<>();
		ArrayList<Integer> yEndingCoords = new ArrayList<>();
		for (Rectangle letter : letters) {
			if (letter.height >= minHeight) {
				yStartingCoords.add(letter.y);
				yEndingCoords.add(letter.y + letter.height);
			}
		}

		int index = 0;
		for (int yCoord : yStartingCoords) {
			if (index == 0) {
				++index;
				continue;
			}
			if ((yCoord - yStartingCoords.get(index - 1)) < minYDifference) {
				yStartingCoords.set(index, yStartingCoords.get(index - 1));
			}
			++index;
		}

		index = 0;
		for (int yCoord : yEndingCoords) {
			if (index == 0) {
				++index;
				continue;
			}
			if ((yCoord - yEndingCoords.get(index - 1)) < minYDifference) {
				yEndingCoords.set(index, yEndingCoords.get(index - 1));
			}
			++index;
		}

		Integer[] uniqueYStarts = Arrays.stream(yStartingCoords.toArray(new Integer[yStartingCoords.size()])).distinct()
				.toArray(Integer[]::new);
		Integer[] uniqueYEnds = Arrays.stream(yEndingCoords.toArray(new Integer[yEndingCoords.size()])).distinct()
				.toArray(Integer[]::new);

		System.out.println(Arrays.toString(uniqueYStarts));
		System.out.println(Arrays.toString(uniqueYEnds));

		return new Rectangle[10];
	}

	public static ArrayList<ArrayList<Rectangle>> segregateBoxesIntoLines(Rectangle[] letters,
			ProcessDataWrapper processDataWrapper, int debugL) {
		// Input: Rectangles sorted in increasing order of Y, and increasing sub-order
		// of X
		// Given the rectangles corresponding to Box coordinates of letters, it returns
		// the Rectangles of the words
		// TBD

		if (debugL <= 2) {
			System.out.println("Entered segregateBoxesIntoLines() with " + letters.length + " boxes");
			for (Rectangle letter : letters) {
				System.out.println(letter);
			}
		}

		// DescriptiveStatistics hStats = new DescriptiveStatistics();
		// DescriptiveStatistics wStats = new DescriptiveStatistics();
		// for (Rectangle letter : letters) {
		// hStats.addValue(letter.height);
		// wStats.addValue(letter.width);
		// }

		// code added to estimate if height distribution is unimodal or multimodal
		// first, estimate kernel size (bin size for histogram) using KDE (Kernel
		// Density Estimation) approximation; see
		// https://en.wikipedia.org/wiki/Kernel_density_estimation

		DescriptiveStatistics heightStats = new DescriptiveStatistics();
		DescriptiveStatistics widthStats = new DescriptiveStatistics();

		// for (ArrayList<Rectangle> line : cleanedLines) {
		for (Rectangle word : letters) {
			if (word != null) {
				heightStats.addValue(word.height);
				// widthStats.addValue(word.width);
			}
		}
		// }

		// double medHeight = heightStats.getPercentile(50);
		double medHeight = heightStats.getMean();
		if (debugL <= 2) {
			System.out.println("First cut medheight for eliminating small boxes = " + medHeight);
		}
		heightStats.clear();
		widthStats.clear();
		for (Rectangle word : letters) {
			if (word != null) {
				if (word.height >= (medHeight * 0.5)) {
					heightStats.addValue(word.height);
					widthStats.addValue(word.width);
				}
			}
		}

		int numberOfModes = 0;
		ArrayList<Integer> modes = new ArrayList<>();
		int kde = 0;
		if (heightStats.getN() > 8) {
			kde = (int) (0.9 * heightStats.getStandardDeviation() * Math.pow(heightStats.getN(), -0.2));
		}
		// System.out.println("Initial Height KDE = " + kde);
		if (kde <= 1) {
			kde = 2;
		}
		if (debugL <= 5) {
			System.out.println("Height KDE = " + kde);
		}
		processDataWrapper.kdeData.setHeightKDE(kde);
		int numberOfBins = ((1600 % kde) == 0) ? 1600 / kde : ((1600 / kde) + 1);
		int histogram[] = new int[numberOfBins];
		// for (ArrayList<Rectangle> line : cleanedLines) {
		for (Rectangle letter : letters) {
			if (letter != null) {
				if (letter.height > (medHeight * 0.5)) {
					int binNumber = ((letter.height % kde) != 0) ? (letter.height / kde) : ((letter.height / kde) - 1);
					if (binNumber < 0) {
						if (debugL <= 5) {
							System.out.println("Letter height = " + letter.height);
						}
						binNumber = 0;
					}
					++histogram[binNumber];
				}
			}
		}
		// }
		for (int i = 1; i < (numberOfBins - 1); ++i) {
			/*
			 * if ((histogram[i] > 0) && (histogram[i] >= histogram[i - 1]) && (histogram[i]
			 * > histogram[i - 2]) && (histogram[i] >= histogram[i + 1]) && (histogram[i] >
			 * histogram[i + 2])) {
			 */
			if ((histogram[i] > 4) && (histogram[i] >= histogram[i - 1]) && (histogram[i] >= histogram[i + 1])) {
				modes.add(i);
				++numberOfModes;
			}
		}

		// find most likely height
		// if 1 mode, it's straightforward
		// if 2 or more modes, calculate for the 2 highest modes, the average height
		// after including the 2 histogram on either side of each mode
		if (modes.size() == 1) {
			processDataWrapper.kdeData.mostLikelyHeight = (int) ((modes.get(0) + 0.5) * kde);
		}

		double averageHeight1 = 0.0;
		double averageHeight2 = 0.0;
		if (modes.size() == 2) {
			int mode1 = modes.get(0);
			int mode2 = modes.get(1);
			if (mode1 == 0) {
				averageHeight1 = ((((0.5 * kde) * histogram[0]) + ((1.5 * kde) * histogram[1])) * 1.0)
						/ (histogram[0] + histogram[1]);
			} else {
				averageHeight1 = (((((mode1 - 0.5) * kde) * histogram[mode1 - 1])
						+ (((mode1 + 0.5) * kde) * histogram[mode1]) + (((mode1 + 1.5) * kde) * histogram[mode1 + 1]))
						* 1.0) / (histogram[mode1 - 1] + histogram[mode1] + histogram[mode1 + 1]);
			}
			averageHeight2 = (((((mode2 - 0.5) * kde) * histogram[mode2 - 1])
					+ (((mode2 + 0.5) * kde) * histogram[mode2]) + (((mode2 + 1.5) * kde) * histogram[mode2 + 1]))
					* 1.0) / (histogram[mode2 - 1] + histogram[mode2] + histogram[mode2 + 1]);

		}

		if (modes.size() >= 2) {
			// int[] arr = modes.stream().filter(i -> i != null).mapToInt(i -> i).toArray();
			// Arrays.sort(arr);
			// int mode1 = arr[arr.length-1];
			// int mode2 = arr[arr.length-2];
			int mode1 = modes.get(0);
			int mode2 = modes.get(1);
			// order mode1 and mode2 in ascending order of their bin count
			if (histogram[mode1] > histogram[mode2]) {
				int temp = mode2;
				mode2 = mode1;
				mode1 = temp;
			}
			for (int i = 2; i < modes.size(); ++i) {
				if (histogram[i] > histogram[mode2]) {
					mode1 = mode2;
					mode2 = i;
				} else {
					if (histogram[i] > histogram[mode1]) {
						mode1 = i;
					}
				}
			}
			if (mode1 == 0) {
				averageHeight1 = ((((0.5 * kde) * histogram[0]) + ((1.5 * kde) * histogram[1])) * 1.0)
						/ (histogram[0] + histogram[1]);
			} else {
				averageHeight1 = (((((mode1 - 0.5) * kde) * histogram[mode1 - 1])
						+ (((mode1 + 0.5) * kde) * histogram[mode1]) + (((mode1 + 1.5) * kde) * histogram[mode1 + 1]))
						* 1.0) / (histogram[mode1 - 1] + histogram[mode1] + histogram[mode1 + 1]);
			}
			averageHeight2 = (((((mode2 - 0.5) * kde) * histogram[mode2 - 1])
					+ (((mode2 + 0.5) * kde) * histogram[mode2]) + (((mode2 + 1.5) * kde) * histogram[mode2 + 1]))
					* 1.0) / (histogram[mode2 - 1] + histogram[mode2] + histogram[mode2 + 1]);
		}

		if (debugL <= 2) {
			System.out.println("Histogram Array = " + Arrays.toString(histogram));
		}
		int mostLikelyHeightIndex = 1;
		int heightTotal = histogram[0] + histogram[1];
		for (int i = 1; i < (histogram.length - 2); ++i) {
			int newTotal = histogram[i - 1] + histogram[i] + histogram[i + 1];
			if ((newTotal > heightTotal) && (histogram[i] >= histogram[i - 1]) && (histogram[i] >= histogram[i + 1])) {
				mostLikelyHeightIndex = i;
				heightTotal = newTotal;
			}
		}
		if (debugL <= 2) {
			System.out.println("Most Likely Height Index = " + mostLikelyHeightIndex);
		}

		if (mostLikelyHeightIndex == 1) {
			int h = (int) (((histogram[mostLikelyHeightIndex] * (mostLikelyHeightIndex + 0.5) * kde)
					+ (histogram[mostLikelyHeightIndex + 1] * (mostLikelyHeightIndex + 1.5) * kde))
					/ (histogram[mostLikelyHeightIndex] + histogram[mostLikelyHeightIndex + 1]));
			processDataWrapper.kdeData.setMostLikelyHeight(h);
		} else {
			if (histogram[mostLikelyHeightIndex - 1] > histogram[mostLikelyHeightIndex + 1]) {
				int h = (int) (((histogram[mostLikelyHeightIndex] * (mostLikelyHeightIndex + 0.5) * kde)
						+ (histogram[mostLikelyHeightIndex - 1] * (mostLikelyHeightIndex - 0.5) * kde))
						/ (histogram[mostLikelyHeightIndex] + histogram[mostLikelyHeightIndex - 1]));
				processDataWrapper.kdeData.setMostLikelyHeight(h);
			} else {
				int h = (int) (((histogram[mostLikelyHeightIndex] * (mostLikelyHeightIndex + 0.5) * kde)
						+ (histogram[mostLikelyHeightIndex + 1] * (mostLikelyHeightIndex + 1.5) * kde))
						/ (histogram[mostLikelyHeightIndex] + histogram[mostLikelyHeightIndex + 1]));
				processDataWrapper.kdeData.setMostLikelyHeight(h);
			}
		}

		int[] heightModalValues = new int[modes.size()];
		int idx = 0;
		for (Integer mode : modes) {
			heightModalValues[idx++] = (int) ((mode + 0.5) * kde);
		}

		processDataWrapper.kdeData
				.setHeightHistogram(IntStream.of(histogram).boxed().collect(Collectors.toCollection(ArrayList::new)));
		processDataWrapper.kdeData.setHeightModes(
				IntStream.of(heightModalValues).boxed().collect(Collectors.toCollection(ArrayList::new)));

		if (debugL <= 2) {
			System.out.println(Arrays.toString(histogram));
			System.out.println("Number of Height Modes = " + numberOfModes + ", which are - "
					+ Arrays.toString(heightModalValues));
		}

		int numberOfWidthModes = 0;
		ArrayList<Integer> widthModes = new ArrayList<>();
		int kdew = 2;
		if (widthStats.getN() > 8) {
			kdew = (int) (0.9 * widthStats.getStandardDeviation() * Math.pow(widthStats.getN(), -0.2));
		}
		if (kdew <= 1) {
			kdew = 2;
		}
		int originalKDEW = kdew;
		if (kdew > 9) {
			kdew = 9;
		}

		processDataWrapper.kdeData.setWidthKDE(kdew);

		int numberOfWidthBins = ((1600 % kdew) == 0) ? 1600 / kdew : ((1600 / kdew) + 1);
		// int numberOfWidthBins = (500 / kdew) + 1;
		int wHistogram[] = new int[numberOfWidthBins];
		// for (ArrayList<Rectangle> line : cleanedLines) {
		for (Rectangle letter : letters) {
			if (letter != null) {
				if (letter.height > (medHeight * 0.5)) {
					int binNumber = ((letter.width % kdew) != 0) ? (letter.width / kdew) : ((letter.width / kdew) - 1);
					if (binNumber < 0) {
						if (debugL <= 2) {
							System.out.println("Letter width = " + letter.width);
						}
						binNumber = 0;
					}
					++wHistogram[binNumber];
				}
			}
		}
		// }
		for (int i = 1; i < (numberOfWidthBins - 1); ++i) {
			/*
			 * if ((wHistogram[i] > 0) && (wHistogram[i] >= wHistogram[i - 1]) &&
			 * (wHistogram[i] > wHistogram[i - 2]) && (wHistogram[i] >= wHistogram[i + 1])
			 * && (wHistogram[i] > wHistogram[i + 2])) {
			 */
			if ((wHistogram[i] > 4) && (wHistogram[i] >= wHistogram[i - 1]) && (wHistogram[i] >= wHistogram[i + 1])) {
				widthModes.add(i);
				++numberOfWidthModes;
			}
		}
		if (debugL <= 2) {
			System.out.println("wHistogram Array = " + Arrays.toString(wHistogram));
		}
		int mostLikelyWidthIndex = 1;
		int widthTotal = wHistogram[0] + wHistogram[1];
		for (int i = 1; i < (wHistogram.length - 2); ++i) {
			int newTotal = wHistogram[i - 1] + wHistogram[i] + wHistogram[i + 1];
			if ((newTotal > widthTotal) && (wHistogram[i] >= wHistogram[i - 1])
					&& (wHistogram[i] >= wHistogram[i + 1])) {
				mostLikelyWidthIndex = i;
				widthTotal = newTotal;
			}
		}

		if (debugL <= 2) {
			System.out.println("Most Likely Width Index = " + mostLikelyWidthIndex);
		}

		if (mostLikelyWidthIndex == 1) {
			int w = (int) (((wHistogram[mostLikelyWidthIndex] * (mostLikelyWidthIndex + 0.5) * kdew)
					+ (wHistogram[mostLikelyWidthIndex + 1] * (mostLikelyWidthIndex + 1.5) * kdew))
					/ (wHistogram[mostLikelyWidthIndex] + wHistogram[mostLikelyWidthIndex + 1]));
			processDataWrapper.kdeData.setMostLikelyWidth(w);
		} else {
			if (wHistogram[mostLikelyWidthIndex - 1] > wHistogram[mostLikelyWidthIndex + 1]) {
				int w = (int) (((wHistogram[mostLikelyWidthIndex] * (mostLikelyWidthIndex + 0.5) * kdew)
						+ (wHistogram[mostLikelyWidthIndex - 1] * (mostLikelyWidthIndex - 0.5) * kdew))
						/ (wHistogram[mostLikelyWidthIndex] + wHistogram[mostLikelyWidthIndex - 1]));
				processDataWrapper.kdeData.setMostLikelyWidth(w);
			} else {
				int w = (int) (((wHistogram[mostLikelyWidthIndex] * (mostLikelyWidthIndex + 0.5) * kdew)
						+ (wHistogram[mostLikelyWidthIndex + 1] * (mostLikelyWidthIndex + 1.5) * kdew))
						/ (wHistogram[mostLikelyWidthIndex] + wHistogram[mostLikelyWidthIndex + 1]));
				processDataWrapper.kdeData.setMostLikelyWidth(w);
			}
		}

		int[] widthModalValues = new int[widthModes.size()];
		int idx1 = 0;
		for (Integer mode : widthModes) {
			widthModalValues[idx1++] = (int) ((mode + 0.5) * kdew);
		}
		processDataWrapper.kdeData
				.setWidthHistogram(IntStream.of(wHistogram).boxed().collect(Collectors.toCollection(ArrayList::new)));
		processDataWrapper.kdeData
				.setWidthModes(IntStream.of(widthModalValues).boxed().collect(Collectors.toCollection(ArrayList::new)));

		if (debugL <= 5) {
			System.out.println("Width KDE = " + kdew);
			System.out.println(Arrays.toString(wHistogram));
			System.out.println("Number of Width Modes = " + numberOfWidthModes + ", which are - "
					+ Arrays.toString(widthModalValues));
		}

		if (debugL <= 7) {
			System.out.println("Likely Height = " + processDataWrapper.kdeData.mostLikelyHeight + "; Likely Width = "
					+ processDataWrapper.kdeData.mostLikelyWidth + "; (Original Width) = " + originalKDEW);
		}

		int minYDifference = 3;
		// double medianHeight = hStats.getPercentile(50);
		double medianHeight = processDataWrapper.kdeData.mostLikelyHeight;
		// double medianWidth = wStats.getPercentile(50);
		double medianWidth = processDataWrapper.kdeData.mostLikelyWidth;
		double heightCutoff = 0.45;
		double widthCutoff = 0.5;

		ArrayList<ArrayList<Rectangle>> lines = new ArrayList<>();

		Set<Integer> lineNumbersWhereFitmentPossible = new TreeSet<>();

		mainloop: for (Rectangle letter : letters) {
			if (letter == null) {
				continue mainloop;
			}
			int index = 0;
			lineNumbersWhereFitmentPossible.clear();
			if (lines.size() == 0) { // the loop is starting
				ArrayList<Rectangle> newLine = new ArrayList<Rectangle>();
				lines.add(newLine);
				newLine.add(letter);
				continue mainloop;
			}
			loop2: for (ArrayList<Rectangle> line : lines) {
				loop3: for (Rectangle box : line) {
					// Note: letter is the new Rectangle picked up for fitment,
					// while box is an already slotted Rectangle in the lines ArrayList

					// ignore the box if its dimensions are small
					if ((box.height < (medianHeight * heightCutoff)) || (box.width < (medianWidth * widthCutoff))) {
						if (debugL <= 1) {
							System.out.println("Ignored small box : " + box);
						}
						continue loop3;
					}
					// ignore the box if its height is too large
					if (box.height > (medianHeight * 2.5)) {
						if (debugL <= 1) {
							System.out.println("Ignored large box : " + box);
						}
						continue loop3;
					}
					if ((Math.abs(letter.y - box.y) < minYDifference)
							|| (Math.abs((letter.y + letter.height) - (box.y + box.height)) < minYDifference)) {
						lineNumbersWhereFitmentPossible.add(index);
						++index;
						continue loop2;
					}
					if ((letter.y >= box.y) && ((letter.y + letter.height) <= (box.y + box.height))) {
						lineNumbersWhereFitmentPossible.add(index);
						++index;
						continue loop2;
					}
				}
				++index;
			}
			if (lineNumbersWhereFitmentPossible.size() == 0) { // based on y-coordinates, did not find a potential set
				// of words where it can fit
				ArrayList<Rectangle> newLine = new ArrayList<Rectangle>();
				lines.add(newLine);
				newLine.add(letter);
				continue mainloop;
			}

			// First, check which existing lineNumber is a better fit for the box.
			// Essentially, find the distance between the current box and the boxes in the
			// line and choose that line where the distance is minimum

			// ArrayList<Integer> minimumXDistances = new ArrayList<>();
			int bestFitLine = -1;
			int minXDistance = Integer.MAX_VALUE;
			outerloop: for (int lineNumber : lineNumbersWhereFitmentPossible) {
				ArrayList<Rectangle> line = lines.get(lineNumber); // get the current list of letters at the lineNumber
				for (Rectangle box : line) {
					int xDistance = (letter.x > box.x) ? Math.abs(letter.x - (box.x + box.width))
							: Math.abs(box.x - (letter.x + letter.width));
					if (xDistance < minXDistance) {
						minXDistance = xDistance;
						bestFitLine = lineNumber;
						continue outerloop;
					}
				}
			}

			int acceptableGap = (int) (processDataWrapper.kdeData.mostLikelyWidth * 2.5);

			if ((bestFitLine != -1) && (minXDistance < acceptableGap)) {
				ArrayList<Rectangle> bestLine = lines.get(bestFitLine); // get the current list of letters at the
																		// lineNumber
				bestLine.add(0, letter);
			} else {
				// create a new line and add the letter
				ArrayList<Rectangle> newLine = new ArrayList<Rectangle>();
				lines.add(newLine);
				newLine.add(letter);
				continue mainloop;
			}

			/*
			 * int indexWithinWord = 0; for (Rectangle box : bestLine) { if ((letter.x +
			 * letter.width) <= (box.x + 10)) { bestLine.add(indexWithinWord, letter);
			 * continue mainloop; } ++indexWithinWord; // added - was missing earlier }
			 */

			// If the code has reached here, then the letter could not be fitted into an
			// existing line
			// Hence, check if it fits as the last word in the existing line (depending on
			// maxSpacing = height * 0.8 * 4), else fit it into a new line

			/*
			 * boolean fitInSameLine = ((letter.x - (bestLine.get(indexWithinWord - 1).x +
			 * bestLine.get(indexWithinWord - 1).width)) < (medianHeight 0.8 * 3)); if
			 * (fitInSameLine) { bestLine.add(indexWithinWord, letter); } else {
			 * ArrayList<Rectangle> newLine = new ArrayList<Rectangle>();
			 * lines.add(newLine); newLine.add(letter); }
			 */
		}

		// sort each line of 'words' by x-coordinate and add to lines
		// then, return the sorted ArrayList lines

		for (ArrayList<Rectangle> line : lines) {
			Collections.sort(line, new Comparator<Rectangle>() {

				@Override
				public int compare(Rectangle r1, Rectangle r2) {
					return (r1.x - r2.x);
				}

			});
		}

		// remove all lines with length 0. It seems that some such lines are still in
		// the mix. Need to clear these somewhere above, but will do it here for now

		if (debugL <= 2) {
			System.out.println("In segregateBoxesIntoLines : number of lines = " + lines.size());
		}
		for (int k = lines.size() - 1; k >= 0; --k) {
			if (lines.get(k).size() == 0) {
				if (debugL <= 2) {
					System.out.println("About to remove line due to size being 0 : " + lines.get(k));
				}
				lines.remove(k);
			}
		}
		if (debugL <= 2) {
			System.out.println(
					"In segregateBoxesIntoLines : number of lines after removal of empty lines = " + lines.size());
		}

		Collections.sort(lines, new Comparator<ArrayList<Rectangle>>() {

			@Override
			public int compare(ArrayList<Rectangle> line1, ArrayList<Rectangle> line2) {
				if ((line1.size() > 0) && (line2.size() == 0)) {
					return 1;
				}
				if ((line1.size() == 0) && (line2.size() > 0)) {
					return -1;
				}
				if ((line1.size() == 0) && (line2.size() == 0)) {
					return 1;
				}
				if ((line1.get(0).y - line2.get(0).y) < -5) {
					return -1;
				}
				if ((line1.get(0).y - line2.get(0).y) > 5) {
					return 1;
				}
				return (line1.get(0).x - line2.get(0).x);
			}
		});

		/*
		 * ArrayList<ArrayList<Rectangle>> finalLines = new ArrayList<>(); for
		 * (ArrayList<Rectangle> line : lines) { ArrayList<Rectangle> newLine = new
		 * ArrayList<>(); for (Rectangle word : line) { int index = 0; innerloop: for
		 * (Rectangle newWord : newLine) { if (word.x > newWord.x) { ++index; } else {
		 * break innerloop; } } newLine.add(index, word); } finalLines.add(newLine); }
		 */

		int tabSpace = 3;
		if (debugL <= 1) {
			for (ArrayList<Rectangle> line : lines) {
				for (int n = 0; n < tabSpace; ++n) {
					System.out.print(" ");
				}
				System.out.print("New Line - ");
				System.out.println(Arrays.toString(line.toArray(new Rectangle[line.size()])));
			}
		}
		// return finalLines;
		return orderAndArrangeLines(removeSmallBoxes(lines, heightStats, widthStats, processDataWrapper, debugL),
				heightStats, widthStats, processDataWrapper, debugL);
	}

	public static ArrayList<ArrayList<Rectangle>> segregateBoxesIntoLines1(Rectangle[] letters,
			ProcessDataWrapper processDataWrapper, int debugL) {
		// Input: Rectangles sorted in increasing order of Y, and increasing sub-order
		// of X
		// Given the rectangles corresponding to Box coordinates of letters, it returns
		// the Rectangles of the words
		// TBD

		if (debugL <= 1) {
			for (Rectangle letter : letters) {
				System.out.println(letter);
			}
		}

		// DescriptiveStatistics hStats = new DescriptiveStatistics();
		// DescriptiveStatistics wStats = new DescriptiveStatistics();
		// for (Rectangle letter : letters) {
		// hStats.addValue(letter.height);
		// wStats.addValue(letter.width);
		// }

		// code added to estimate if height distribution is unimodal or multimodal
		// first, estimate kernel size (bin size for histogram) using KDE (Kernel
		// Density Estimation) approximation; see
		// https://en.wikipedia.org/wiki/Kernel_density_estimation

		DescriptiveStatistics heightStats = new DescriptiveStatistics();
		DescriptiveStatistics widthStats = new DescriptiveStatistics();

		// for (ArrayList<Rectangle> line : cleanedLines) {
		for (Rectangle word : letters) {
			heightStats.addValue(word.height);
			// widthStats.addValue(word.width);
		}
		// }

		double medHeight = heightStats.getPercentile(50);
		heightStats.clear();
		widthStats.clear();
		for (Rectangle word : letters) {
			if (word.height >= (medHeight * 0.5)) {
				heightStats.addValue(word.height);
				widthStats.addValue(word.width);
			}
		}

		int numberOfModes = 0;
		ArrayList<Integer> modes = new ArrayList<>();
		int kde = 0;
		if (heightStats.getN() > 8) {
			kde = (int) (0.9 * heightStats.getStandardDeviation() * Math.pow(heightStats.getN(), -0.2));
		}
		// System.out.println("Initial Height KDE = " + kde);
		if (kde <= 1) {
			kde = 2;
		}
		if (debugL <= 5) {
			System.out.println("Height KDE = " + kde);
		}
		processDataWrapper.kdeData.setHeightKDE(kde);
		int numberOfBins = ((200 % kde) == 0) ? 200 / kde : ((200 / kde) + 1);
		int histogram[] = new int[numberOfBins];
		// for (ArrayList<Rectangle> line : cleanedLines) {
		for (Rectangle letter : letters) {
			int binNumber = ((letter.height % kde) != 0) ? (letter.height / kde) : ((letter.height / kde) - 1);
			if (binNumber < 0) {
				if (debugL <= 5) {
					System.out.println("Letter height = " + letter.height);
				}
				binNumber = 0;
			}
			++histogram[binNumber];
		}
		// }
		for (int i = 1; i < (numberOfBins - 1); ++i) {
			/*
			 * if ((histogram[i] > 0) && (histogram[i] >= histogram[i - 1]) && (histogram[i]
			 * > histogram[i - 2]) && (histogram[i] >= histogram[i + 1]) && (histogram[i] >
			 * histogram[i + 2])) {
			 */
			if ((histogram[i] > 4) && (histogram[i] >= histogram[i - 1]) && (histogram[i] >= histogram[i + 1])) {
				modes.add(i);
				++numberOfModes;
			}
		}

		// find most likely height
		// if 1 mode, it's straightforward
		// if 2 or more modes, calculate for the 2 highest modes, the average height
		// after including the 2 histogram on either side of each mode
		if (modes.size() == 1) {
			processDataWrapper.kdeData.mostLikelyHeight = (int) ((modes.get(0) + 0.5) * kde);
		}

		double averageHeight1 = 0.0;
		double averageHeight2 = 0.0;
		if (modes.size() == 2) {
			int mode1 = modes.get(0);
			int mode2 = modes.get(1);
			if (mode1 == 0) {
				averageHeight1 = ((((0.5 * kde) * histogram[0]) + ((1.5 * kde) * histogram[1])) * 1.0)
						/ (histogram[0] + histogram[1]);
			} else {
				averageHeight1 = (((((mode1 - 0.5) * kde) * histogram[mode1 - 1])
						+ (((mode1 + 0.5) * kde) * histogram[mode1]) + (((mode1 + 1.5) * kde) * histogram[mode1 + 1]))
						* 1.0) / (histogram[mode1 - 1] + histogram[mode1] + histogram[mode1 + 1]);
			}
			averageHeight2 = (((((mode2 - 0.5) * kde) * histogram[mode2 - 1])
					+ (((mode2 + 0.5) * kde) * histogram[mode2]) + (((mode2 + 1.5) * kde) * histogram[mode2 + 1]))
					* 1.0) / (histogram[mode2 - 1] + histogram[mode2] + histogram[mode2 + 1]);

		}

		if (modes.size() >= 2) {
			// int[] arr = modes.stream().filter(i -> i != null).mapToInt(i -> i).toArray();
			// Arrays.sort(arr);
			// int mode1 = arr[arr.length-1];
			// int mode2 = arr[arr.length-2];
			int mode1 = modes.get(0);
			int mode2 = modes.get(1);
			// order mode1 and mode2 in ascending order of their bin count
			if (histogram[mode1] > histogram[mode2]) {
				int temp = mode2;
				mode2 = mode1;
				mode1 = temp;
			}
			for (int i = 2; i < modes.size(); ++i) {
				if (histogram[i] > histogram[mode2]) {
					mode1 = mode2;
					mode2 = i;
				} else {
					if (histogram[i] > histogram[mode1]) {
						mode1 = i;
					}
				}
			}
			if (mode1 == 0) {
				averageHeight1 = ((((0.5 * kde) * histogram[0]) + ((1.5 * kde) * histogram[1])) * 1.0)
						/ (histogram[0] + histogram[1]);
			} else {
				averageHeight1 = (((((mode1 - 0.5) * kde) * histogram[mode1 - 1])
						+ (((mode1 + 0.5) * kde) * histogram[mode1]) + (((mode1 + 1.5) * kde) * histogram[mode1 + 1]))
						* 1.0) / (histogram[mode1 - 1] + histogram[mode1] + histogram[mode1 + 1]);
			}
			averageHeight2 = (((((mode2 - 0.5) * kde) * histogram[mode2 - 1])
					+ (((mode2 + 0.5) * kde) * histogram[mode2]) + (((mode2 + 1.5) * kde) * histogram[mode2 + 1]))
					* 1.0) / (histogram[mode2 - 1] + histogram[mode2] + histogram[mode2 + 1]);
		}

		if (debugL <= 2) {
			System.out.println("Histogram Array = " + Arrays.toString(histogram));
		}
		int mostLikelyHeightIndex = 1;
		int heightTotal = histogram[0] + histogram[1];
		for (int i = 1; i < (histogram.length - 2); ++i) {
			int newTotal = histogram[i - 1] + histogram[i] + histogram[i + 1];
			if ((newTotal > heightTotal) && (histogram[i] >= histogram[i - 1]) && (histogram[i] >= histogram[i + 1])) {
				mostLikelyHeightIndex = i;
				heightTotal = newTotal;
			}
		}
		if (debugL <= 2) {
			System.out.println("Most Likely Height Index = " + mostLikelyHeightIndex);
		}

		// go over the histogram again
		// if there is a bin with a frequency > 1.5 of the frequency of
		// mostLikelyHeightIndex, then change the mostLikelyHeightIndex to that index

		for (int i = 1; i < (histogram.length - 2); ++i) {
			if ((histogram[i] >= (1.5 * histogram[mostLikelyHeightIndex]))) {
				mostLikelyHeightIndex = i;
			}
		}

		if (histogram[mostLikelyHeightIndex - 1] > histogram[mostLikelyHeightIndex + 1]) {
			// index 0 corresponds to kde/2, index 1 to 3*kde/2, 2 to 5*kde/2, n to
			// (2n+1)*kde/2.
			// Therefore, (2n+1)/2 + 1/2 = (n+1); and, ((2n+1)/2 - 1/2 = n
			processDataWrapper.kdeData.setMostLikelyHeight((mostLikelyHeightIndex) * kde);
		} else {
			processDataWrapper.kdeData.setMostLikelyHeight((int) ((mostLikelyHeightIndex + 1.0) * kde));
		}

		int[] heightModalValues = new int[modes.size()];
		int idx = 0;
		for (Integer mode : modes) {
			heightModalValues[idx++] = (int) ((mode + 0.5) * kde);
		}

		processDataWrapper.kdeData
				.setHeightHistogram(IntStream.of(histogram).boxed().collect(Collectors.toCollection(ArrayList::new)));
		processDataWrapper.kdeData.setHeightModes(
				IntStream.of(heightModalValues).boxed().collect(Collectors.toCollection(ArrayList::new)));

		if (debugL <= 2) {
			System.out.println(Arrays.toString(histogram));
			System.out.println("Number of Height Modes = " + numberOfModes + ", which are - "
					+ Arrays.toString(heightModalValues));
		}

		int numberOfWidthModes = 0;
		ArrayList<Integer> widthModes = new ArrayList<>();
		int kdew = 2;
		if (widthStats.getN() > 8) {
			kdew = (int) (0.9 * widthStats.getStandardDeviation() * Math.pow(widthStats.getN(), -0.2));
		}
		if (kdew <= 1) {
			kdew = 2;
		}

		processDataWrapper.kdeData.setWidthKDE(kdew);
		int numberOfWidthBins = ((200 % kdew) == 0) ? 200 / kdew : ((200 / kdew) + 1);
		int wHistogram[] = new int[numberOfWidthBins];
		// for (ArrayList<Rectangle> line : cleanedLines) {
		for (Rectangle letter : letters) {

			int binNumber = ((letter.width % kdew) != 0) ? (letter.width / kdew) : ((letter.width / kdew) - 1);
			if (binNumber < 0) {
				if (debugL <= 2) {
					System.out.println("Letter width = " + letter.width);
				}
				binNumber = 0;
			}
			++wHistogram[binNumber];
		}
		// }
		for (int i = 1; i < (numberOfWidthBins - 1); ++i) {
			/*
			 * if ((wHistogram[i] > 0) && (wHistogram[i] >= wHistogram[i - 1]) &&
			 * (wHistogram[i] > wHistogram[i - 2]) && (wHistogram[i] >= wHistogram[i + 1])
			 * && (wHistogram[i] > wHistogram[i + 2])) {
			 */
			if ((wHistogram[i] > 4) && (wHistogram[i] >= wHistogram[i - 1]) && (wHistogram[i] >= wHistogram[i + 1])) {
				widthModes.add(i);
				++numberOfWidthModes;
			}
		}
		if (debugL <= 2) {
			System.out.println("wHistogram Array = " + Arrays.toString(wHistogram));
		}
		int mostLikelyWidthIndex = 1;
		int widthTotal = wHistogram[0] + wHistogram[1];
		for (int i = 1; i < (wHistogram.length - 2); ++i) {
			int newTotal = wHistogram[i - 1] + wHistogram[i] + wHistogram[i + 1];
			if ((newTotal > widthTotal) && (wHistogram[i] >= wHistogram[i - 1])
					&& (wHistogram[i] >= wHistogram[i + 1])) {
				mostLikelyWidthIndex = i;
				widthTotal = newTotal;
			}
		}

		if (debugL <= 2) {
			System.out.println("Most Likely Width Index = " + mostLikelyWidthIndex);
		}

		if (wHistogram[mostLikelyWidthIndex - 1] > wHistogram[mostLikelyWidthIndex + 1]) {
			processDataWrapper.kdeData.setMostLikelyWidth((mostLikelyWidthIndex) * kdew);
		} else {
			processDataWrapper.kdeData.setMostLikelyWidth((mostLikelyWidthIndex + 1) * kdew);
		}

		int[] widthModalValues = new int[widthModes.size()];
		int idx1 = 0;
		for (Integer mode : widthModes) {
			widthModalValues[idx1++] = (int) ((mode + 0.5) * kdew);
		}
		processDataWrapper.kdeData
				.setWidthHistogram(IntStream.of(wHistogram).boxed().collect(Collectors.toCollection(ArrayList::new)));
		processDataWrapper.kdeData
				.setWidthModes(IntStream.of(widthModalValues).boxed().collect(Collectors.toCollection(ArrayList::new)));

		if (debugL <= 5) {
			System.out.println("Width KDE = " + kdew);
			System.out.println(Arrays.toString(wHistogram));
			System.out.println("Number of Width Modes = " + numberOfWidthModes + ", which are - "
					+ Arrays.toString(widthModalValues));
		}

		if (debugL <= 7) {
			System.out.println("Likely Height = " + processDataWrapper.kdeData.mostLikelyHeight + "; Likely Width = "
					+ processDataWrapper.kdeData.mostLikelyWidth);
		}

		int minYDifference = 5;
		// double medianHeight = hStats.getPercentile(50);
		double medianHeight = processDataWrapper.kdeData.mostLikelyHeight;
		// double medianWidth = wStats.getPercentile(50);
		double medianWidth = processDataWrapper.kdeData.mostLikelyWidth;
		double heightCutoff = 0.5;
		double widthCutoff = 0.5;

		ArrayList<ArrayList<Rectangle>> lines = new ArrayList<>();

		Set<Integer> lineNumbersWhereFitmentPossible = new TreeSet<>();

		mainloop: for (Rectangle letter : letters) {
			int index = 0;
			lineNumbersWhereFitmentPossible.clear();
			if (lines.size() == 0) { // the loop is starting
				ArrayList<Rectangle> newLine = new ArrayList<Rectangle>();
				lines.add(newLine);
				newLine.add(letter);
				continue mainloop;
			}
			loop2: for (ArrayList<Rectangle> line : lines) {
				loop3: for (Rectangle box : line) {
					// Note: letter is the new Rectangle picked up for fitment,
					// while box is an already slotted Rectangle in the lines ArrayList

					// ignore the box if its dimensions are small
					if ((box.height < (medianHeight * heightCutoff)) || (box.width < (medianWidth * widthCutoff))) {
						continue loop3;
					}
					if ((Math.abs(letter.y - box.y) < minYDifference)
							|| (Math.abs((letter.y + letter.height) - (box.y + box.height)) < minYDifference)) {
						lineNumbersWhereFitmentPossible.add(index);
						++index;
						continue loop2;
					}
					if ((letter.y >= box.y) && ((letter.y + letter.height) <= (box.y + box.height))) {
						lineNumbersWhereFitmentPossible.add(index);
						++index;
						continue loop2;
					}
				}
				++index;
			}
			if (lineNumbersWhereFitmentPossible.size() == 0) { // based on y-coordinates, did not find a potential set
				// of words where it can fit
				ArrayList<Rectangle> newLine = new ArrayList<Rectangle>();
				lines.add(newLine);
				newLine.add(letter);
				continue mainloop;
			}

			// First, check which existing lineNumber is a better fit for the box.
			// Essentially, find the distance between the current box and the boxes in the
			// line and choose that line where the distance is minimum

			// ArrayList<Integer> minimumXDistances = new ArrayList<>();
			int bestFitLine = -1;
			int minXDistance = Integer.MAX_VALUE;
			outerloop: for (int lineNumber : lineNumbersWhereFitmentPossible) {
				ArrayList<Rectangle> line = lines.get(lineNumber); // get the current list of letters at the lineNumber
				for (Rectangle box : line) {
					int xDistance = (letter.x > box.x) ? Math.abs(letter.x - (box.x + box.width))
							: Math.abs(box.x - (letter.x + letter.width));
					if (xDistance < minXDistance) {
						minXDistance = xDistance;
						bestFitLine = lineNumber;
						continue outerloop;
					}
				}
			}

			ArrayList<Rectangle> bestLine = lines.get(bestFitLine); // get the current list of letters at the
																	// lineNumber
			int indexWithinWord = 0;
			for (Rectangle box : bestLine) {
				if ((letter.x + letter.width) <= (box.x + 10)) {
					bestLine.add(indexWithinWord, letter);
					continue mainloop;
				}
				++indexWithinWord; // added - was missing earlier
			}

			// If the code has reached here, then the letter could not be fitted into an
			// existing line
			// Hence, check if it fits as the last word in the existing line (depending on
			// maxSpacing = height * 0.8 * 4), else fit it into a new line

			boolean fitInSameLine = ((letter.x
					- (bestLine.get(indexWithinWord - 1).x + bestLine.get(indexWithinWord - 1).width)) < (medianHeight
							* 0.8 * 3));
			if (fitInSameLine) {
				bestLine.add(indexWithinWord, letter);
			} else {
				ArrayList<Rectangle> newLine = new ArrayList<Rectangle>();
				lines.add(newLine);
				newLine.add(letter);
			}
		}

		// sort each line of 'words' by x-coordinate and add to lines
		// then, return the sorted ArrayList lines
		ArrayList<ArrayList<Rectangle>> finalLines = new ArrayList<>();
		for (ArrayList<Rectangle> line : lines) {
			ArrayList<Rectangle> newLine = new ArrayList<>();
			for (Rectangle word : line) {
				int index = 0;
				innerloop: for (Rectangle newWord : newLine) {
					if (word.x > newWord.x) {
						++index;
					} else {
						break innerloop;
					}
				}
				newLine.add(index, word);
			}
			finalLines.add(newLine);
		}

		int tabSpace = 3;
		if (debugL <= 1) {
			for (ArrayList<Rectangle> line : finalLines) {
				for (int n = 0; n < tabSpace; ++n) {
					System.out.print(" ");
				}
				System.out.print("New Line - ");
				System.out.println(Arrays.toString(line.toArray(new Rectangle[line.size()])));
			}
		}
		// return finalLines;
		return orderAndArrangeLines(removeSmallBoxes(finalLines, heightStats, widthStats, processDataWrapper, debugL),
				heightStats, widthStats, processDataWrapper, debugL);
	}

	public static ArrayList<ArrayList<Rectangle>> removeSmallBoxes(ArrayList<ArrayList<Rectangle>> inputLines,
			DescriptiveStatistics hStats, DescriptiveStatistics wStats, ProcessDataWrapper processDataWrapper,
			int debugL) {

		// ArrayList<ArrayList<Rectangle>> cleanedLines = new ArrayList<>();
		/*
		 * double medianHeight = hStats.getPercentile(50); double averageHeight =
		 * hStats.getMean(); double referenceHeight = medianHeight; if ((averageHeight /
		 * medianHeight) > 1.5) { referenceHeight = averageHeight; }
		 */
		double referenceHeight = processDataWrapper.kdeData.mostLikelyHeight;
		double referenceWidth = processDataWrapper.kdeData.mostLikelyWidth;
		double heightLowerCutoff = 0.35;
		double heightHigherCutoff = 1.875;
		double widthCutoff = 0.2;
		// System.out.println("Reference Height = " + referenceHeight + "; averageHeight
		// = " + averageHeight
		// + "; medianHeight = " + medianHeight + "; cutOffHeight = " + (referenceHeight
		// * heightCutoff));

		/*
		 * for (ArrayList<Rectangle> line : inputLines) { ArrayList<Rectangle> newLine =
		 * new ArrayList<>(); boolean wordsAdded = false; for (Rectangle word : line) {
		 * if (word.height > (referenceHeight * heightCutoff)) { newLine.add(word);
		 * wordsAdded = true; } } if (wordsAdded) { cleanedLines.add(newLine); } }
		 */

		ArrayList<ArrayList<Rectangle>> smallBoxRemovedLines = new ArrayList<>();
		for (ArrayList<Rectangle> line : inputLines) {
			ArrayList<Rectangle> newLine = new ArrayList<>();
			boolean wordsAdded = false;
			for (Rectangle word : line) {
				if ((word.height >= (referenceHeight * heightLowerCutoff))
						&& (word.width >= (referenceWidth * widthCutoff))
						&& (word.height <= (referenceHeight * heightHigherCutoff))) {
					newLine.add(word);
					wordsAdded = true;
				} else {
					if (debugL <= 2) {
						System.out.println("Dropped word in removeSmallBoxes() " + word);
					}
				}
			}
			if (wordsAdded) {
				smallBoxRemovedLines.add(newLine);
			}
		}

		return smallBoxRemovedLines;
	}

	public static ArrayList<ArrayList<Rectangle>> orderAndArrangeLines(ArrayList<ArrayList<Rectangle>> inputLines,
			DescriptiveStatistics hStats, DescriptiveStatistics wStats, ProcessDataWrapper processDataWrapper,
			int debugL) {
		int minDifference = 3; // minimum pixel deviation
		double assumedWidthRatio = 0.8;
		double acceptableGapMultiple = 3.5;
		double medianHeight = processDataWrapper.kdeData.mostLikelyHeight;
		double medianWidth = processDataWrapper.kdeData.mostLikelyWidth;

		ArrayList<ArrayList<Rectangle>> newLines = new ArrayList<>();
		int lineIndex = 0;
		int newIndex = 0;
		mainloop: for (ArrayList<Rectangle> line : inputLines) {
			if (lineIndex == 0) {
				newLines.add(line);
				++lineIndex;
				++newIndex;
				continue;
			}
			ArrayList<Rectangle> previousLine = newLines.get(newIndex - 1);
			Rectangle lastBoxInPreviousLine = previousLine.get(previousLine.size() - 1);
			boolean firstWord = true;
			innerloop: for (Rectangle word : line) {
				if (firstWord) {
					boolean sufficientDistance = (word.x > lastBoxInPreviousLine.x) && (Math
							.abs(word.x - (lastBoxInPreviousLine.x + lastBoxInPreviousLine.width)) > (medianWidth
									* acceptableGapMultiple));
					if (sufficientDistance) {
						break innerloop;
					}
				}

				firstWord = false;
				innermostloop: for (Rectangle aWord : previousLine) {
					if (aWord.height < (medianHeight * 0.45)) {
						continue innermostloop;
					}
					boolean fitFound = (Math.abs(word.y - aWord.y) <= minDifference)
							|| (Math.abs((word.y + word.height) - (aWord.y + aWord.height)) <= minDifference);
					if (fitFound) {
						ArrayList<Rectangle> newLine = mergeAndSort(previousLine, line);
						newLines.set(newIndex - 1, newLine);
						++lineIndex;
						continue mainloop;
					}
				}
			}
			// if the line doesn't fit into an existing line, then the code reaches here
			newLines.add(line);
			++lineIndex;
			++newIndex;
		}

		if (debugL <= 1) {
			System.out.println("At 1 in orderAndArrangeLines()");
			System.out.println(newLines.toString());
		}

		// lines would have been allocated properly by now
		// now, one has to split each line into separate pockets of lines depending on
		// gap

		ArrayList<ArrayList<Rectangle>> finalLines = new ArrayList<>();
		// Rectangle previousRectangle = null;
		for (ArrayList<Rectangle> line : newLines) {
			ArrayList<Rectangle> newLine = new ArrayList<>();
			finalLines.add(newLine);
			int index = 0;
			wordloop: for (Rectangle word : line) {
				if (index == 0) {
					newLine.add(word);
					++index;
					continue wordloop;
				}
				if ((word.x - (newLine.get(index - 1).x + newLine.get(index - 1).width)) < (medianHeight
						* assumedWidthRatio * acceptableGapMultiple)) {
					newLine.add(word);
					++index;
				} else {
					newLine = new ArrayList<>();
					finalLines.add(newLine);
					newLine.add(word);
					index = 1;
				}
			}
		}

		if (debugL <= 1) {
			System.out.println("At 2 in orderAndArrangeLines()");
			System.out.println(finalLines.toString());
		}

		ArrayList<ArrayList<Rectangle>> resultLines1 = new ArrayList<>();

		for (ArrayList<Rectangle> line : finalLines) {
			if (line.size() <= 2) {
				boolean markedForDeletion = true;
				for (Rectangle letter : line) {
					if ((letter.width < (processDataWrapper.kdeData.mostLikelyWidth * 0.5))
							|| (letter.height < (processDataWrapper.kdeData.mostLikelyHeight * 0.45))) {
						if (debugL <= 2) {
							System.out.println("Line marked for deletion : " + line);
						}
						markedForDeletion = markedForDeletion && true;
					} else {
						if (debugL <= 2) {
							System.out.println("Line unmarked (removed from marking) for deletion : " + line);
						}
						markedForDeletion = markedForDeletion && false;
					}
				}
				if (!markedForDeletion) {
					resultLines1.add(line);
				}
			} else {
				resultLines1.add(line);
			}
		}

		if (debugL <= 1) {
			System.out.println("At 3 in orderAndArrangeLines()");
			System.out.println(resultLines1.toString());
		}

		// do another round of cleanup to eliminate single-box lines with box of small
		// width (< mostLikelyWidth) or 0.6*mostLikelyHeight

		ArrayList<ArrayList<Rectangle>> resultLines2 = new ArrayList<>();

		for (ArrayList<Rectangle> line : resultLines1) {
			if (line.size() <= 1) {
				boolean markedForDeletion = true;
				for (Rectangle letter : line) {
					if ((letter.width <= (processDataWrapper.kdeData.mostLikelyWidth))
							|| (letter.height <= (processDataWrapper.kdeData.mostLikelyHeight * 0.5))) {
						markedForDeletion = markedForDeletion && true;
					} else {
						markedForDeletion = markedForDeletion && false;
					}
				}
				if (!markedForDeletion) {
					resultLines2.add(line);
				}
			} else {
				resultLines2.add(line);
			}
		}

		if (debugL <= 1) {
			System.out.println("At 4 in orderAndArrangeLines()");
			System.out.println(resultLines2.toString());
		}

		// now sort the lines based on the y-coordinate and x-coordinate of the first
		// box in that line

		Collections.sort(resultLines2, new Comparator<ArrayList<Rectangle>>() {

			@Override
			public int compare(ArrayList<Rectangle> line1, ArrayList<Rectangle> line2) {
				if ((line1.get(0).y - line2.get(0).y) < -5) {
					return -1;
				}
				;
				if ((line1.get(0).y - line2.get(0).y) > 5) {
					return 1;
				}
				;
				return (line1.get(0).x - line2.get(0).x);
			}
		});

		return resultLines2;
	}

	public static ArrayList<ArrayList<Rectangle>> reallocateLines(ArrayList<ArrayList<Rectangle>> input,
			ProcessDataWrapper processDataWrapper, int debugL) {

		// do reallocation of lines to other lines, figuring out if they are in the same
		// line

		ArrayList<ArrayList<Rectangle>> resultLines = new ArrayList<>();
		int topEdgeTolerance = 7;
		ArrayList<Integer> linesAccountedFor = new ArrayList<Integer>();

		currentLineLoop: for (int i = 0; i < input.size(); ++i) {
			if (linesAccountedFor.contains(Integer.valueOf(i))) {
				continue currentLineLoop;
			}
			double acceptableGapMultiple = 3.5;
			ArrayList<Rectangle> currentLine = input.get(i);
			if (i == (input.size() - 1)) {
				resultLines.add(currentLine);
				break currentLineLoop;
			}
			nextLineLoop: for (int j = i + 1; j < input.size(); ++j) {
				if (linesAccountedFor.contains(Integer.valueOf(j))) {
					continue nextLineLoop;
				}
				ArrayList<Rectangle> nextLine = input.get(j);
				boolean inSameLine = false;
				// find lowest box in current line within tolerance of 5 from box 0
				Rectangle lowestBoxInCurrentLine = currentLine.get(0);
				for (Rectangle letter : currentLine) {
					if (((letter.y - currentLine.get(0).y) <= topEdgeTolerance)
							&& (letter.y > lowestBoxInCurrentLine.y)) {
						lowestBoxInCurrentLine = letter;
					}
				}
				Rectangle highestBoxInCurrentLine = currentLine.get(0);
				for (Rectangle letter : currentLine) {
					if (((currentLine.get(0).y - letter.y) <= topEdgeTolerance)
							&& (letter.y < highestBoxInCurrentLine.y)) {
						highestBoxInCurrentLine = letter;
					}
				}

				// find highest box in next line within tolerance of 5 from it's lines box 0
				Rectangle highestBoxInNextLine = nextLine.get(0);
				for (Rectangle letter : nextLine) {
					if (((nextLine.get(0).y - letter.y) <= topEdgeTolerance) && (letter.y < highestBoxInNextLine.y)) {
						highestBoxInNextLine = letter;
					}
				}
				Rectangle lowestBoxInNextLine = nextLine.get(0);
				for (Rectangle letter : nextLine) {
					if (((letter.y - nextLine.get(0).y) <= topEdgeTolerance) && (letter.y > lowestBoxInNextLine.y)) {
						lowestBoxInNextLine = letter;
					}
				}
				if (Math.abs(lowestBoxInCurrentLine.y - highestBoxInNextLine.y) <= topEdgeTolerance) {
					inSameLine = true;
				}
				if (Math.abs(lowestBoxInNextLine.y - highestBoxInCurrentLine.y) <= topEdgeTolerance) {
					inSameLine = true;
				}
				// ADDED CODE TO CHECK IF THE X-COORDINATE OF THE BOX IN NEXT LINE IS VERY CLOSE
				// TO THE BOX IN PREVIOUS LINE
				/*
				 * for (Rectangle letter : nextLine) { if (Math.abs(letter.y -
				 * highestBoxInNextLine.y) <= topEdgeTolerance) { inSameLine = true; } }
				 */

				// if the next line is in the same y-zone as the current line, determine if the
				// nextLine can be fitted in the same line. If it can be fitted, then add the
				// boxes of the next line into the current line, and check fitment of the line
				// that follows the nextLine, into the current line
				// when you reach a "next line" that cannot be fitted in the current line, add
				// the current line to the results, set i to j so that teh loop continues from
				// that line which cannot be fitted

				/*
				 * if (!inSameLine) { if (debugL <= 3) { System.out.println("Adding line " + i +
				 * " as line " + j + " is not in the same line"); }
				 * resultLines.add(currentLine); // if the nextLine is the last line, then add
				 * the last line and break out of the // loop if (j == (input.size() - 1)) {
				 * resultLines.add(nextLine); break currentLineLoop; } i = j; // since i is not
				 * being incremented, ensure that this is done continue currentLineLoop; }
				 */

				if (!inSameLine) {
					if (debugL <= 1) {
						System.out
								.println("Found that line " + (j + 1) + " is not in the same line as line " + (i + 1));
					}
					// if the nextLine is the last line, then add the last line and break out of the
					// loop
					if (j == (input.size() - 1)) {
						resultLines.add(currentLine);
						linesAccountedFor.add(i);
						if (debugL <= 3) {
							System.out.println("Adding line " + (i + 1));
						}
					}
					if (i == (input.size() - 2)) { // second last line reached
						resultLines.add(nextLine);
						linesAccountedFor.add(j);
						if (debugL <= 3) {
							System.out.print(
									"Reached second last line for comparison and found that last line is in a different line. ");
							System.out.println("Adding line " + (j + 1));
						}
						break currentLineLoop; // add the last line and exit the loop
					}
					if (j < (input.size() - 1)) {
						continue nextLineLoop;
					}
					continue currentLineLoop;
				}

				// if the next line looks to be in the same line, then the code continues from
				// here

				// if the next line is in the same y-zone as the current line, check if any
				// boxes in these 2 lines are close to each other. If yes, the next line can be
				// fitted into the current line
				// first, check if the medianWidth can be used or a different width setting has
				// to be applied
				DescriptiveStatistics widthStats = new DescriptiveStatistics();
				for (int p = 0; p < currentLine.size(); ++p) {
					widthStats.addValue(currentLine.get(p).width);
				}
				for (int q = 0; q < nextLine.size(); ++q) {
					widthStats.addValue(nextLine.get(q).width);
				}
				double widthStatsMean = widthStats.getMean();
				double widthStatsSD = widthStats.getStandardDeviation();
				double referenceWidth = processDataWrapper.kdeData.mostLikelyWidth;
				// If the widthStats has a low variance, then it's likely that these are
				// individual characters with a reasonably uniform width
				// Hence, this width should be used instead of the mostLikelyWidth
				if (((widthStatsSD / widthStatsMean) < 0.5)
						&& ((widthStatsMean / processDataWrapper.kdeData.mostLikelyWidth) < 4.0)) {
					referenceWidth = widthStatsMean;
				}
				if (debugL <= 3) {
					System.out.println("Reference Width For checking fitment of line " + (j + 1) + " in line " + (i + 1)
							+ " is " + referenceWidth + ". widthStatsMean = " + widthStatsMean + ", and widthStatsSD = "
							+ widthStatsSD + " ratio = " + (widthStatsSD / widthStatsMean));
				}
				boolean nextLineCanBeAddedToCurrent = false;
				boxClosenessCheckLoop: for (int p = 0; p < currentLine.size(); ++p) {
					Rectangle letterInCurrentLine = currentLine.get(p);
					for (int q = 0; q < nextLine.size(); ++q) {
						Rectangle letterInNextLine = nextLine.get(q);
						if (Math.abs((letterInCurrentLine.x + letterInCurrentLine.width)
								- letterInNextLine.x) < (referenceWidth * acceptableGapMultiple)) {
							nextLineCanBeAddedToCurrent = true;
							break boxClosenessCheckLoop;
						}
						if (Math.abs((letterInNextLine.x + letterInNextLine.width)
								- letterInCurrentLine.x) < (referenceWidth * acceptableGapMultiple)) {
							nextLineCanBeAddedToCurrent = true;
							break boxClosenessCheckLoop;
						}
					}
				}
				if (nextLineCanBeAddedToCurrent) {
					if (debugL <= 3) {
						System.out.println("Determined that line " + (j + 1) + " can be added to line " + (i + 1));
						System.out.println("Current Line before addition is : " + currentLine);
					}
					for (int q = 0; q < nextLine.size(); ++q) {
						currentLine.add(nextLine.get(q));
					}
					linesAccountedFor.add(j);
					if (debugL <= 3) {
						System.out.println("Current Line after addition is : " + currentLine);
					}
					if (j == (input.size() - 1)) {
						resultLines.add(currentLine);
						linesAccountedFor.add(i);
						// break currentLineLoop;
						continue currentLineLoop;
					}
					continue nextLineLoop;
				}
				// resultLines.add(currentLine);
				if (j == (input.size() - 1)) {
					resultLines.add(currentLine);
					linesAccountedFor.add(i);
					continue currentLineLoop;
				}
				// i = j; // since i is not being incremented, ensure that this is done
				// continue currentLineLoop;
			}
		}

		if (debugL <= 3) {
			System.out.println("RESULT LINES IS - " + resultLines);
		}

		/*
		 * ArrayList<ArrayList<Rectangle>> finalLines = new ArrayList<>();
		 *
		 * currentLineLoop: for (int i = 0; i < (input.size() - 1);) { double
		 * acceptableGapMultiple = 3.0; ArrayList<Rectangle> currentLine = input.get(i);
		 * nextLineLoop: for (int j = i + 2; j < input.size(); ++j) { // starting from
		 * (i+2) - key change from above loop. In some cases, it is observed that the
		 * line 2 lines after the current one can be merged into the current line
		 * ArrayList<Rectangle> nextLine = input.get(j); boolean inSameLine = false; //
		 * find lowest box in current line within tolerance of 5 from box 0 Rectangle
		 * lowestBoxInCurrentLine = currentLine.get(0); for (Rectangle letter :
		 * currentLine) { if (((letter.y - currentLine.get(0).y) <= 5) && (letter.y >
		 * lowestBoxInCurrentLine.y)) { lowestBoxInCurrentLine = letter; } } // find
		 * highest box in next line within tolerance of 5 from it's lines box 0
		 * Rectangle highestBoxInNextLine = nextLine.get(0); for (Rectangle letter :
		 * nextLine) { if (((nextLine.get(0).y - letter.y) <= 5) && (letter.y <
		 * highestBoxInNextLine.y)) { highestBoxInNextLine = letter; } } if
		 * (Math.abs(lowestBoxInCurrentLine.y - highestBoxInNextLine.y) <=
		 * topEdgeTolerance) { inSameLine = true; } // if the next line is in the same
		 * y-zone as the current line, determine if the // nextLine can be fitted in the
		 * same line. If it can be fitted, then add the // boxes of the next line into
		 * the current line, and check fitment of the line // that follows the nextLine,
		 * into the current line // when you reach a "next line" that cannot be fitted
		 * in the current line, add // the current line to the results, set i to j so
		 * that teh loop continues from // that line which cannot be fitted if
		 * (!inSameLine) { if (debugL <= 3) { System.out.println("Adding line " + i +
		 * " as line " + j + " is not in the same line"); }
		 * resultLines.add(currentLine); // if the nextLine is the last line, then add
		 * the last line and break out of the // loop if (j == (input.size() - 1)) {
		 * resultLines.add(nextLine); break currentLineLoop; } i = j; // since i is not
		 * being incremented, ensure that this is done continue currentLineLoop; }
		 *
		 * // if the next line looks to be in the same line, then the code continues
		 * from // here
		 *
		 * // if the next line is in the same y-zone as the current line, check if any
		 * // boxes in these 2 lines are close to each other. If yes, the next line can
		 * be // fitted into the current line // first, check if the medianWidth can be
		 * used or a different width setting has // to be applied DescriptiveStatistics
		 * widthStats = new DescriptiveStatistics(); for (int p = 0; p <
		 * currentLine.size(); ++p) { widthStats.addValue(currentLine.get(p).width); }
		 * for (int q = 0; q < nextLine.size(); ++q) {
		 * widthStats.addValue(nextLine.get(q).width); } double widthStatsMean =
		 * widthStats.getMean(); double widthStatsSD =
		 * widthStats.getStandardDeviation(); double referenceWidth =
		 * processDataWrapper.kdeData.mostLikelyWidth; // If the widthStats has a low
		 * variance, then it's likely that these are // individual characters with a
		 * reasonably uniform width // Hence, this width should be used instead of the
		 * mostLikelyWidth if (((widthStatsSD / widthStatsMean) < 0.25) &&
		 * ((widthStatsMean / processDataWrapper.kdeData.mostLikelyWidth) < 4.0)) {
		 * referenceWidth = widthStatsMean; } if (debugL <= 3) {
		 * System.out.println("Reference Width For checking fitment of line " + j +
		 * " in line " + i + " is " + referenceWidth + ". widthStatsMean = " +
		 * widthStatsMean + ", and widthStatsSD = " + widthStatsSD + " ratio = " +
		 * (widthStatsSD / widthStatsMean)); } boolean nextLineCanBeAddedToCurrent =
		 * false; boxClosenessCheckLoop: for (int p = 0; p < currentLine.size(); ++p) {
		 * Rectangle letterInCurrentLine = currentLine.get(p); for (int q = 0; q <
		 * nextLine.size(); ++q) { Rectangle letterInNextLine = nextLine.get(q); if
		 * (Math.abs((letterInCurrentLine.x + letterInCurrentLine.width) -
		 * letterInNextLine.x) < (referenceWidth * acceptableGapMultiple)) {
		 * nextLineCanBeAddedToCurrent = true; break boxClosenessCheckLoop; } if
		 * (Math.abs((letterInNextLine.x + letterInNextLine.width) -
		 * letterInCurrentLine.x) < (referenceWidth * acceptableGapMultiple)) {
		 * nextLineCanBeAddedToCurrent = true; break boxClosenessCheckLoop; } } } if
		 * (nextLineCanBeAddedToCurrent) { if (debugL <= 3) {
		 * System.out.println("Determined that line " + j + " can be added to line " +
		 * i); System.out.println("Current Line before addition is : " + currentLine); }
		 * for (int q = 0; q < nextLine.size(); ++q) { currentLine.add(nextLine.get(q));
		 * } if (debugL <= 3) { System.out.println("Current Line after addition is : " +
		 * currentLine); } if (j == (input.size() - 1)) { resultLines.add(currentLine);
		 * break currentLineLoop; } continue nextLineLoop; }
		 * resultLines.add(currentLine); if (j == (input.size() - 1)) {
		 * resultLines.add(nextLine); break currentLineLoop; } i = j; // since i is not
		 * being incremented, ensure that this is done continue currentLineLoop; } }
		 */

		return resultLines;

		/*
		 * ArrayList<ArrayList<Rectangle>> resultLines1 = new ArrayList<>();
		 * topEdgeTolerance = 7; ArrayList<Integer> linesAccountedFor1 = new
		 * ArrayList<Integer>();
		 *
		 * currentLineLoop: for (int i = 0; i < resultLines.size(); ++i) { if
		 * (linesAccountedFor1.contains(Integer.valueOf(i))) { continue currentLineLoop;
		 * } double acceptableGapMultiple = 3.5; ArrayList<Rectangle> currentLine =
		 * resultLines.get(i); if (i == (resultLines.size() - 2)) {
		 * resultLines1.add(currentLine); resultLines1.add(resultLines.get(i + 1));
		 * break currentLineLoop; } nextLineLoop: for (int j = i + 2; j <
		 * resultLines.size(); ++j) { if
		 * (linesAccountedFor1.contains(Integer.valueOf(j))) { continue nextLineLoop; }
		 * ArrayList<Rectangle> nextLine = resultLines.get(j); boolean inSameLine =
		 * false; // find lowest box in current line within tolerance of 5 from box 0
		 * Rectangle lowestBoxInCurrentLine = currentLine.get(0); for (Rectangle letter
		 * : currentLine) { if (((letter.y - currentLine.get(0).y) <= topEdgeTolerance)
		 * && (letter.y > lowestBoxInCurrentLine.y)) { lowestBoxInCurrentLine = letter;
		 * } } Rectangle highestBoxInCurrentLine = currentLine.get(0); for (Rectangle
		 * letter : currentLine) { if (((currentLine.get(0).y - letter.y) <=
		 * topEdgeTolerance) && (letter.y < highestBoxInCurrentLine.y)) {
		 * highestBoxInCurrentLine = letter; } }
		 *
		 * // find highest box in next line within tolerance of 5 from it's lines box 0
		 * Rectangle highestBoxInNextLine = nextLine.get(0); for (Rectangle letter :
		 * nextLine) { if (((nextLine.get(0).y - letter.y) <= topEdgeTolerance) &&
		 * (letter.y < highestBoxInNextLine.y)) { highestBoxInNextLine = letter; } }
		 * Rectangle lowestBoxInNextLine = nextLine.get(0); for (Rectangle letter :
		 * nextLine) { if (((letter.y - nextLine.get(0).y) <= topEdgeTolerance) &&
		 * (letter.y > lowestBoxInNextLine.y)) { lowestBoxInNextLine = letter; } } if
		 * (Math.abs(lowestBoxInCurrentLine.y - highestBoxInNextLine.y) <=
		 * topEdgeTolerance) { inSameLine = true; } if (Math.abs(lowestBoxInNextLine.y -
		 * highestBoxInCurrentLine.y) <= topEdgeTolerance) { inSameLine = true; } //
		 * ADDED CODE TO CHECK IF THE X-COORDINATE OF THE BOX IN NEXT LINE IS VERY CLOSE
		 * // TO THE BOX IN PREVIOUS LINE
		 *
		 * for (Rectangle letter : nextLine) { if (Math.abs(letter.y -
		 * highestBoxInNextLine.y) <= topEdgeTolerance) { inSameLine = true; } }
		 *
		 *
		 * // if the next line is in the same y-zone as the current line, determine if
		 * the // nextLine can be fitted in the same line. If it can be fitted, then add
		 * the // boxes of the next line into the current line, and check fitment of the
		 * line // that follows the nextLine, into the current line // when you reach a
		 * "next line" that cannot be fitted in the current line, add // the current
		 * line to the results, set i to j so that teh loop continues from // that line
		 * which cannot be fitted
		 *
		 *
		 * if (!inSameLine) { if (debugL <= 3) { System.out.println("Adding line " + i +
		 * " as line " + j + " is not in the same line"); }
		 * resultLines1.add(currentLine); // if the nextLine is the last line, then add
		 * the last line and break out of the // loop if (j == (resultLines.size() - 1))
		 * { resultLines1.add(nextLine); break currentLineLoop; } i = j; // since i is
		 * not being incremented, ensure that this is done continue currentLineLoop; }
		 *
		 *
		 * if (!inSameLine) { if (debugL <= 1) { System.out .println("Found that line "
		 * + (j + 1) + " is not in the same line as line " + (i + 1)); } // if the
		 * nextLine is the last line, then add the last line and break out of the //
		 * loop if (j == (resultLines.size() - 1)) { if
		 * (!linesAccountedFor1.contains(j)) { resultLines1.add(currentLine);
		 * linesAccountedFor1.add(j); } if (debugL <= 3) {
		 * System.out.println("Adding line " + (i + 1)); } } if (i ==
		 * (resultLines.size() - 2)) { // second last line reached - add the if
		 * (!linesAccountedFor1.contains(1)) { resultLines1.add(nextLine);
		 * linesAccountedFor1.add(j); resultLines1.add(resultLines.get(i + 1));
		 * linesAccountedFor1.add(i + 1); } if (debugL <= 3) { System.out.print(
		 * "Reached second last line for comparison and found that last line is in a different line. "
		 * ); System.out.println("Adding line " + (j + 1)); } break currentLineLoop; //
		 * add the last line and exit the loop } if (j < (resultLines.size() - 1)) {
		 * continue nextLineLoop; } continue currentLineLoop; }
		 *
		 * // if the next line looks to be in the same line, then the code continues
		 * from // here
		 *
		 * // if the next line is in the same y-zone as the current line, check if any
		 * // boxes in these 2 lines are close to each other. If yes, the next line can
		 * be // fitted into the current line // first, check if the medianWidth can be
		 * used or a different width setting has // to be applied DescriptiveStatistics
		 * widthStats = new DescriptiveStatistics(); for (int p = 0; p <
		 * currentLine.size(); ++p) { widthStats.addValue(currentLine.get(p).width); }
		 * for (int q = 0; q < nextLine.size(); ++q) {
		 * widthStats.addValue(nextLine.get(q).width); } double widthStatsMean =
		 * widthStats.getMean(); double widthStatsSD =
		 * widthStats.getStandardDeviation(); double referenceWidth =
		 * processDataWrapper.kdeData.mostLikelyWidth; // If the widthStats has a low
		 * variance, then it's likely that these are // individual characters with a
		 * reasonably uniform width // Hence, this width should be used instead of the
		 * mostLikelyWidth if (((widthStatsSD / widthStatsMean) < 0.5) &&
		 * ((widthStatsMean / processDataWrapper.kdeData.mostLikelyWidth) < 4.0)) {
		 * referenceWidth = widthStatsMean; } if (debugL <= 3) {
		 * System.out.println("Reference Width For checking fitment of line " + (j + 1)
		 * + " in line " + (i + 1) + " is " + referenceWidth + ". widthStatsMean = " +
		 * widthStatsMean + ", and widthStatsSD = " + widthStatsSD + " ratio = " +
		 * (widthStatsSD / widthStatsMean)); } boolean nextLineCanBeAddedToCurrent =
		 * false; boxClosenessCheckLoop: for (int p = 0; p < currentLine.size(); ++p) {
		 * Rectangle letterInCurrentLine = currentLine.get(p); for (int q = 0; q <
		 * nextLine.size(); ++q) { Rectangle letterInNextLine = nextLine.get(q); if
		 * (Math.abs((letterInCurrentLine.x + letterInCurrentLine.width) -
		 * letterInNextLine.x) < (referenceWidth * acceptableGapMultiple)) {
		 * nextLineCanBeAddedToCurrent = true; break boxClosenessCheckLoop; } if
		 * (Math.abs((letterInNextLine.x + letterInNextLine.width) -
		 * letterInCurrentLine.x) < (referenceWidth * acceptableGapMultiple)) {
		 * nextLineCanBeAddedToCurrent = true; break boxClosenessCheckLoop; } } } if
		 * (nextLineCanBeAddedToCurrent) { if (debugL <= 3) {
		 * System.out.println("Determined that line " + (j + 1) +
		 * " can be added to line " + (i + 1));
		 * System.out.println("Current Line before addition is : " + currentLine); } for
		 * (int q = 0; q < nextLine.size(); ++q) { currentLine.add(nextLine.get(q)); }
		 * linesAccountedFor1.add(j); if (debugL <= 3) {
		 * System.out.println("Current Line after addition is : " + currentLine); } if
		 * (j == (resultLines.size() - 1)) { resultLines1.add(currentLine);
		 * linesAccountedFor1.add(i); // break currentLineLoop; continue
		 * currentLineLoop; } continue nextLineLoop; } // resultLines1.add(currentLine);
		 * if (j == (resultLines.size() - 1)) { resultLines1.add(currentLine);
		 * linesAccountedFor1.add(i); continue currentLineLoop; } // i = j; // since i
		 * is not being incremented, ensure that this is done // continue
		 * currentLineLoop; } }
		 *
		 * // HACK - remove null entries from resultLines1 - somehow, the first element
		 * is // turning out to be null; need to check the second part of the above code
		 * to // see where it is messed up
		 *
		 * for (int i = resultLines1.size() - 1; i >= 0; --i) { if (resultLines1.get(i)
		 * == null) { resultLines1.remove(i); } }
		 *
		 * System.out.println("RESULT LINES 1 IS - " + resultLines1);
		 *
		 * return resultLines1;
		 */
	}

	public static ArrayList<ArrayList<Rectangle>> splitLines(ArrayList<ArrayList<Rectangle>> input,
			ProcessDataWrapper processDataWrapper, int debugL) {

		ArrayList<Boolean> needsToBeSplit = new ArrayList<>(input.size());
		ArrayList<Boolean> hasAnOverlap = new ArrayList<>(input.size());
		for (int i = 0; i < input.size(); ++i) {
			hasAnOverlap.add(Boolean.FALSE);
		}
		int medianHeight = processDataWrapper.kdeData.mostLikelyHeight;
		// if (debugL <= 2) {
		// System.out.println("Median Height = " + medianHeight);
		// }
		// iterate through the lines and investigate if they need to be split because of
		// large boxes, and if that line has an overlap with the next line
		int lineNo = 1;

		if (debugL <= 2) {
			for (ArrayList<Rectangle> line : input) {
				System.out.println("Before splitting - Line " + lineNo + " : " + line);
				++lineNo;
			}
		}

		lineNo = 0;
		for (ArrayList<Rectangle> line : input) {
			// int boxNo = 0;
			// int normalHeightBoxes = 0;
			int abnormalHeightBoxes = 0;
			for (Rectangle letter : line) {
				if (((letter.height * 1.0) / medianHeight) > 1.4) {
					++abnormalHeightBoxes;
				}
				/*
				 * else { ++normalHeightBoxes; }
				 */

				/*
				 * if (lineNo < (input.size() - 1)) { ArrayList<Rectangle> nextLine =
				 * input.get(lineNo); Rectangle firstBoxInNextLine = nextLine.get(0); if
				 * ((((letter.y + letter.height) < (firstBoxInNextLine.y)) ||
				 * ((firstBoxInNextLine.y + firstBoxInNextLine.height) < (letter.y))) &&
				 * (Math.abs(firstBoxInNextLine.x - letter.x) < (kdeData.widthModes.get(0) *
				 * 2.0))) { hasAnOverlap.set(lineNo - 1, Boolean.TRUE); } }
				 */
			}
			if (abnormalHeightBoxes > 0) {
				needsToBeSplit.add(Boolean.TRUE);
			} else {
				needsToBeSplit.add(Boolean.FALSE);
			}
			++lineNo;
		}

		// iterate through the lines that need to be split and add the split boxes into
		// new lines in a new arraylist
		lineNo = 0;
		ArrayList<ArrayList<Rectangle>> allNewLines = new ArrayList<>();
		outerloop: for (Boolean split : needsToBeSplit) {
			if (split.booleanValue()) {
				ArrayList<Rectangle> line = input.get(lineNo);
				// first, check if there is an overlap between the boxes within the line. If the
				// overlap is large, then exit the loop
				for (Rectangle aBox : line) {
					for (Rectangle anotherBox : line) {
						if (!aBox.equals(anotherBox)) {
							// check overlap
							int overlapArea = overlapArea(aBox, anotherBox);
							double percentOverlap = Math.max((overlapArea * 1.0) / (aBox.height * aBox.width),
									(overlapArea * 1.0) / (anotherBox.height * anotherBox.width));
							if (debugL <= 2) {
								System.out.println("Overlap = " + percentOverlap);
							}
							if (percentOverlap > 0.4) {
								processDataWrapper.linesNeededSplitting = true;
								processDataWrapper.linesNotSplitDueToHighOverlap = true;
								break outerloop;
							}
						}
					}
				}

				// next, get how many lines this line should be broken into and what are the
				// topY and bottomY coordinates of the current line
				int numberOfReplacementLines = 1;
				int topEdge = Integer.MAX_VALUE;
				int bottomEdge = Integer.MIN_VALUE;
				for (int index = line.size() - 1; index >= 0; --index) {
					Rectangle letter = line.get(index);
					topEdge = Math.min(letter.y, topEdge);
					bottomEdge = Math.max(letter.y + letter.height, bottomEdge);
					int multiple = Math.round((letter.height * 1.0f) / medianHeight);
					numberOfReplacementLines = Math.max(numberOfReplacementLines, multiple);
				}
				if (debugL <= 2) {
					System.out.println("Number of replacement lines for line number " + (lineNo + 1) + " is "
							+ numberOfReplacementLines);
				}
				// int tolerance = medianHeight / 6;
				int tolerance = 0;

				// create the number of required new lines
				// note that the new lines are added into a temporary array first, because
				// allocating the boxes into new lines is dependent on the characteristics of
				// the specific line being split - how many split lines, and which particular
				// split line would a box fit in
				ArrayList<ArrayList<Rectangle>> newLines = new ArrayList<>();
				for (int i = 0; i < numberOfReplacementLines; ++i) {
					newLines.add(new ArrayList<Rectangle>());
				}
				// calculate the size of each line from the boxes in the line that have not had
				// to be divided
				ArrayList<ArrayList<Integer>> sizes = new ArrayList<>(numberOfReplacementLines);
				ArrayList<ArrayList<Integer>> startYs = new ArrayList<>(numberOfReplacementLines);
				for (int i = 0; i < numberOfReplacementLines; ++i) {
					sizes.add(new ArrayList<Integer>());
					startYs.add(new ArrayList<Integer>());
				}
				for (int index = 0; index < line.size(); ++index) {
					Rectangle letter = line.get(index);
					int startingPlacement = Math.round((((letter.y + tolerance) - topEdge) * 1.0f) / medianHeight);
					if (startingPlacement >= numberOfReplacementLines) {
						startingPlacement = numberOfReplacementLines - 1;
					}
					int divisions = Math.round(((letter.height + tolerance) * 1.0f) / medianHeight);
					if (divisions == 1) {
						sizes.get(startingPlacement).add(letter.height);
						startYs.get(startingPlacement).add(letter.y);
					}
				}

				ArrayList<Integer> likelyHeights = new ArrayList<>(numberOfReplacementLines);
				for (int i = 0; i < numberOfReplacementLines; ++i) {
					ArrayList<Integer> letterSizesInThisLine = sizes.get(i);
					int sum = 0;
					for (Integer size : letterSizesInThisLine) {
						sum += size;
					}
					int likelyHeight;
					if (letterSizesInThisLine.size() != 0) {
						likelyHeight = sum / letterSizesInThisLine.size();
					} else {
						likelyHeight = medianHeight;
					}
					likelyHeights.add(i, likelyHeight);
				}
				if (debugL <= 2) {
					System.out.println("Likely Heights = " + likelyHeights.toString());
				}

				ArrayList<Integer> startingYs = new ArrayList<>(numberOfReplacementLines);
				for (int i = 0; i < numberOfReplacementLines; ++i) {
					ArrayList<Integer> startYsInThisLine = startYs.get(i);
					int sum = 0;
					for (Integer startY : startYsInThisLine) {
						sum += startY;
					}
					int likelyStartY;
					if (startYsInThisLine.size() != 0) {
						likelyStartY = sum / startYsInThisLine.size();
					} else {
						if (i != 0) {
							likelyStartY = startingYs.get(i - 1) + medianHeight;
						} else {
							likelyStartY = line.get(0).y;
						}
					}
					startingYs.add(i, likelyStartY);
				}
				if (debugL <= 2) {
					System.out.println("StartingYs = " + startingYs.toString());
				}

				for (int index = 0; index < line.size(); ++index) {
					Rectangle letter = line.get(index);
					int startingPlacement = Math.round((((letter.y + tolerance) - topEdge) * 1.0f) / medianHeight);
					if (debugL <= 2) {
						System.out.println("Starting placement = " + startingPlacement);
					}
					int divisions = Math.max(1, Math.round(((letter.height + tolerance) * 1.0f) / medianHeight));
					if (debugL <= 2) {
						System.out.println("Divisions = " + divisions);
					}
					for (int thisDivision = 0; thisDivision < divisions; ++thisDivision) {
						int placement = startingPlacement + thisDivision;
						if (placement >= numberOfReplacementLines) {
							placement = numberOfReplacementLines - 1;
						}
						if (debugL <= 2) {
							System.out.println("At placement index " + placement);
						}
						int startingY = (thisDivision == 0) ? letter.y : startingYs.get(placement);
						Rectangle newRect = new Rectangle(letter.x, startingY, letter.width,
								Math.min(likelyHeights.get(placement) + 2, bottomEdge - startingY));
						if (debugL <= 2) {
							System.out.println(
									"At placement index " + placement + ", about to add a new box - " + newRect);
						}
						newLines.get(placement).add(newRect);
						processDataWrapper.linesNeededSplitting = true;
						if (debugL <= 2) {
							System.out.println("At placement index " + placement + ", added a new box - " + newRect);
						}
					}
				}

				// add the new lines found into the overall new lines array
				for (ArrayList<Rectangle> newLine : newLines) {
					allNewLines.add(newLine);
				}
			}
			++lineNo;
		}

		// iterate through the input lines and remove all those where there were large
		// boxes. After that, iterate through all the new lines, and add them to the
		// input
		if (debugL <= 2) {
			System.out.println("Needs to be split arraylist is - " + needsToBeSplit);
		}
		for (int i = needsToBeSplit.size() - 1; i >= 0; --i) {
			if (needsToBeSplit.get(i).booleanValue()) {
				if (debugL <= 2) {
					System.out.println("About to remove object in input at index - " + i);
				}
				input.remove(i);
			}
		}

		lineNo = 1;
		if (debugL <= 2) {
			System.out.println("After removal of large boxes :");
			for (ArrayList<Rectangle> line : input) {
				System.out.println("                      Line " + lineNo + " : " + line);
				++lineNo;
			}
		}

		for (ArrayList<Rectangle> line : allNewLines) {
			input.add(line);
		}

		lineNo = 1;
		if (debugL <= 2) {
			System.out.println("After addition of new boxes : ");
			for (ArrayList<Rectangle> line : input) {
				if (debugL <= 2) {
					System.out.println("                     Line " + lineNo + " : " + line);
				}
				++lineNo;
			}
		}

		// now sort the lines based on the y-coordinate of the first box in that line

		Collections.sort(input, new Comparator<ArrayList<Rectangle>>() {

			@Override
			public int compare(ArrayList<Rectangle> line1, ArrayList<Rectangle> line2) {
				if ((line1.get(0).y - line2.get(0).y) < -5) {
					return -1;
				}
				;
				if ((line1.get(0).y - line2.get(0).y) > 5) {
					return 1;
				}
				;
				return (line1.get(0).x - line2.get(0).x);
			}
		});

		return input;
	}

	public static ArrayList<Rectangle> mergeAndSort(ArrayList<Rectangle> line1, ArrayList<Rectangle> line2) {
		ArrayList<Rectangle> result = new ArrayList<Rectangle>();
		for (Rectangle word : line1) {
			result.add(word);
		}
		for (Rectangle word : line2) {
			int index = 0;
			innerloop: for (Rectangle existingWord : result) {
				if (word.x > existingWord.x) {
					++index;
				} else {
					break innerloop;
				}
			}
			result.add(index, word);
		}
		return result;
	}

	public static ArrayList<ArrayList<Rectangle>> segregateBoxesIntoLines(ArrayList<Rectangle> letters, int debugL) {
		Rectangle[] letterRectangles = letters.toArray(new Rectangle[letters.size()]);

		DescriptiveStatistics boxHeightStats = new DescriptiveStatistics();
		DescriptiveStatistics boxWidthStats = new DescriptiveStatistics();
		for (Rectangle letter : letterRectangles) {
			boxHeightStats.addValue(letter.height);
			boxWidthStats.addValue(letter.width);
		}

		int medianHeight = (int) boxHeightStats.getPercentile(50);
		int medianWidth = (int) boxWidthStats.getPercentile(50);

		if (debugL <= 1) {
			for (Rectangle letter : letterRectangles) {
				System.out.println(letter);
			}
		}

		int minYDifference = (int) (medianHeight * 0.75);

		// sort the array in increasing order of y, sub-ordered by x coordinates
		Arrays.sort(letterRectangles, new Comparator<Rectangle>() {

			@Override
			public int compare(Rectangle first, Rectangle second) {

				// ascending order
				return first.y - second.y;
			}

		});

		// consolidate the y-sorted boxes into lines based on y-coordinates and
		// medianHeight
		ArrayList<ArrayList<Rectangle>> tempLines = new ArrayList<>();
		int lineIndex = -1;
		int previousStartY = -1;
		int previousEndY = -1;
		loop1: for (Rectangle letter : letterRectangles) {
			ArrayList<Rectangle> line = null;
			if (lineIndex == -1) {
				++lineIndex;
				line = new ArrayList<>();
				tempLines.add(line);
				line.add(letter);
				previousStartY = letter.y;
				previousEndY = letter.y + letter.height;
				continue loop1;
			}
			if ((Math.abs(letter.y - previousStartY) < minYDifference) || ((letter.y + letter.height) < previousEndY)) {
				tempLines.get(lineIndex).add(letter);
				previousEndY = Math.max(previousEndY, letter.y + letter.height);
				continue loop1;
			}
			line = new ArrayList<>();
			tempLines.add(line);
			line.add(letter);
			previousStartY = letter.y;
			previousEndY = letter.y + letter.height;
			++lineIndex;
		}

		// sort each line in tempLines based on x-coordinate
		ArrayList<ArrayList<Rectangle>> lines = new ArrayList<>();
		lineIndex = 0;
		for (ArrayList<Rectangle> line : tempLines) {
			Rectangle[] wordArray = line.toArray(new Rectangle[line.size()]);
			Arrays.sort(wordArray, new Comparator<Rectangle>() {
				@Override
				public int compare(Rectangle first, Rectangle second) {
					return first.x - second.x;
				}
			});
			lines.add(new ArrayList<Rectangle>(Arrays.asList(wordArray)));
		}

		// segregate symbols that are far apart into new lines, based on medianGap

		DescriptiveStatistics boxGapStats = new DescriptiveStatistics();
		boolean newLine = false;
		for (ArrayList<Rectangle> line : lines) {
			newLine = true;
			int wordIndex = 0;
			wordLoop: for (Rectangle word : line) {
				if (newLine) {
					newLine = false;
					++wordIndex;
					continue wordLoop;
				}
				boxGapStats.addValue(word.x - line.get(wordIndex - 1).x - line.get(wordIndex - 1).width);
				++wordIndex;
			}
		}

		int medianGap = (int) boxGapStats.getPercentile(50);
		int acceptableGap = medianGap <= 2 ? 20 * medianGap : (medianGap <= 3 ? 17 * medianGap : 13 * medianGap);
		acceptableGap = Math.max(medianWidth * 3, acceptableGap);

		ArrayList<ArrayList<Rectangle>> finalLines = new ArrayList<>();

		for (ArrayList<Rectangle> line : lines) {
			int wordIndex = 0;
			ArrayList<Rectangle> aLine = new ArrayList<>();
			finalLines.add(aLine);
			wordLoop: for (Rectangle word : line) {
				if (wordIndex == 0) {
					aLine.add(word);
					++wordIndex;
					continue wordLoop;
				}
				if ((word.x - line.get(wordIndex - 1).x - line.get(wordIndex - 1).width) < acceptableGap) {
					aLine.add(word);
					++wordIndex;
					continue wordLoop;
				}
				aLine = new ArrayList<>();
				finalLines.add(aLine);
				aLine.add(word);
				++wordIndex;
			}
		}

		int tabSpace = 3;
		if (debugL <= 1) {
			for (ArrayList<Rectangle> line : finalLines) {
				for (int n = 0; n < tabSpace; ++n) {
					System.out.print(" ");
				}
				System.out.print("New Line - ");
				System.out.println(Arrays.toString(line.toArray(new Rectangle[line.size()])));
			}
		}

		return finalLines;

	}

	public static ArrayList<ArrayList<Rectangle>> rectifyBoxes(ArrayList<ArrayList<Rectangle>> lines, int debugL) {

		// Having segregated the boxes into new lines, do a second pass for cleanup
		// 1. Remove loner small dimension boxes (Done)
		// 2A. Change the dilate sel centre; position it at the center of the sel, and
		// ensure that the box coordinates are reduced by the dilation pixels (Done)
		// 2. Remove boxes with low connected components and high black pixel %age (TBD)
		// 3. Introduce space boxes (SpaceBox extends Box) to isolate words (TBD)
		// 4. Split boxes, if you determine that 2 lines have coalesced (TBD)
		// 5. After boxes have been identified, create new word boxes (TBD)

		// double boxWidthAverage = 0.0;
		// double boxHeightAverage = 0.0;
		// double boxWidthSigma = 0.0;
		// double boxHeightSigma = 0.0;
		ArrayList<DescriptiveStatistics> widthStats = new ArrayList<>();
		ArrayList<DescriptiveStatistics> heightStats = new ArrayList<>();
		ArrayList<DescriptiveStatistics> widthGapStats = new ArrayList<>();
		ArrayList<DescriptiveStatistics> heightStartStats = new ArrayList<>();

		DescriptiveStatistics overallWidthStats = new DescriptiveStatistics();
		DescriptiveStatistics overallHeightStats = new DescriptiveStatistics();
		DescriptiveStatistics overallGapStats = new DescriptiveStatistics();

		// int previousBoxEndX = 0;
		// int previousLineEndY = 0;

		int currentLine = -1;

		// Iterate through the rectangles and get the DescriptiveStatistics

		for (ArrayList<Rectangle> sentence : lines) {
			DescriptiveStatistics currentWidthStats = new DescriptiveStatistics();
			DescriptiveStatistics currentHeightStats = new DescriptiveStatistics();
			DescriptiveStatistics currentWidthGapStats = new DescriptiveStatistics();
			DescriptiveStatistics currentHeightStartStats = new DescriptiveStatistics();
			++currentLine;
			int wordIndex = 0;
			for (Rectangle word : sentence) {
				currentWidthStats.addValue(word.width);
				currentHeightStats.addValue(word.height);
				overallWidthStats.addValue(word.width);
				overallHeightStats.addValue(word.height);
				if (wordIndex != 0) {
					int gap = Math.max(0, word.x - sentence.get(wordIndex - 1).x - sentence.get(wordIndex - 1).width);
					// System.out.println(gap + " = " + word.x + " - " + sentence.get(wordIndex -
					// 1).x + " - "
					// + sentence.get(wordIndex - 1).width);
					currentWidthGapStats.addValue(gap);
					overallGapStats.addValue(gap);
				}
				currentHeightStartStats.addValue(word.y);
				++wordIndex;
			}
			widthStats.add(currentWidthStats);
			heightStats.add(currentHeightStats);
			widthGapStats.add(currentWidthGapStats);
			heightStartStats.add(currentHeightStartStats);
		}

		double overallHeightMedian = overallHeightStats.getPercentile(50);
		// double overallHeightMean = overallHeightStats.getMean();
		double overallWidthMedian = overallWidthStats.getPercentile(50);
		double overallWidthMean = overallWidthStats.getMean();
		double overallGapMedian = overallGapStats.getPercentile(50);
		double gapTolerancePercent = 5.0; // 5.0 = 500 % tolerance

		// Iterate through the rectangles and remove the loner small ones
		currentLine = -1;
		ArrayList<ArrayList<Rectangle>> newLines = new ArrayList<>();
		for (ArrayList<Rectangle> sentence : lines) {
			ArrayList<Rectangle> newLine = new ArrayList<>();
			newLines.add(newLine);
			++currentLine;
			int wordIndex = 0;
			for (Rectangle word : sentence) {
				boolean heightGood = (word.height > (overallHeightMedian * 0.85));
				boolean heightOK = (word.height > (overallHeightMedian * 0.5));
				boolean widthOK = (word.width > (overallWidthMedian * 0.75));
				boolean widthTooSmall = (word.width < (overallWidthMedian * 0.2));
				boolean heightTooSmall = (word.height < (overallHeightMedian * 0.2));
				if (widthTooSmall && heightTooSmall) {
					if (debugL <= 2) {
						System.out.print("Dropping box : ");
						System.out.print(word + " - as ");
						System.out.print("widthTooSmall = " + widthTooSmall + "; ");
						System.out.println("heightTooSmall = " + heightTooSmall);
					}
					continue;
				}
				boolean gapOK = false;
				Rectangle previousWord = null;
				Rectangle nextWord = null;
				try {
					previousWord = sentence.get(wordIndex - 1);
				} catch (Exception e) {
				}
				try {
					nextWord = sentence.get(wordIndex + 1);
				} catch (Exception e) {
				}
				if (previousWord != null) {
					if (widthStats.get(currentLine).getElement(wordIndex) < (0.5 * overallWidthMean)) {
						gapOK = gapOK || (widthGapStats.get(currentLine)
								.getElement(wordIndex - 1) < (overallGapMedian * (1 + gapTolerancePercent)));
						gapOK = gapOK && (heightOK || (previousWord.height > (overallHeightMedian * 0.7)));
					}
				}
				if (!gapOK && (nextWord != null)) {
					if (widthStats.get(currentLine).getElement(wordIndex) < (0.5 * overallWidthMean)) {
						gapOK = gapOK || (widthGapStats.get(currentLine)
								.getElement(wordIndex) < (overallGapMedian * (1 + gapTolerancePercent)));
						gapOK = gapOK && (heightOK || (nextWord.height > (overallHeightMedian * 0.7)));
					}
				}
				// if the box is surrounded on both sides by boxes of good height, then accept
				// the box

				if ((previousWord != null) && (nextWord != null)) {
					if ((previousWord.height > (overallHeightMedian * 0.85))
							&& (nextWord.height > (overallHeightMedian * 0.85))) {
						gapOK = true;
					}
				}

				if (gapOK) {
					newLine.add(word);
					++wordIndex;
					if (!heightOK) { // debugging why a box of unusual height is being retained
						if (debugL <= 2) {
							System.out.print("Retaining box : ");
							System.out.print(word + " - as ");
							System.out.print("gapOK = " + gapOK + "; ");
							System.out.print("heightOK = " + heightOK + "; ");
							System.out.print("heightGood = " + heightGood + "; ");
							System.out.print("widthOk = " + widthOK + "; ");
							System.out.print("overallGapMedian = " + overallGapMedian + "; ");
							System.out.print("overallWidthMedian = " + overallWidthMedian + "; ");
							System.out.println("overallHeightMedian = " + overallHeightMedian + "; ");
						}
					}
					continue;
				}
				if (heightGood) {
					newLine.add(word);
					++wordIndex;
					if (!heightOK) { // debugging why a box of unusual height is being retained
						if (debugL <= 2) {
							System.out.print("Retaining box : ");
							System.out.print(word + " - as ");
							System.out.print("gapOK = " + gapOK + "; ");
							System.out.print("heightOK = " + heightOK + "; ");
							System.out.print("heightGood = " + heightGood + "; ");
							System.out.print("widthOk = " + widthOK + "; ");
							System.out.print("overallGapMedian = " + overallGapMedian + "; ");
							System.out.print("overallWidthMedian = " + overallWidthMedian + "; ");
							System.out.println("overallHeightMedian = " + overallHeightMedian + "; ");
						}
					}
					continue;
				}
				if (heightOK && widthOK) {
					newLine.add(word);
					++wordIndex;
					continue;
				}
				// check now if there is a box in the next, same, or previous line that has
				// similar x-coordinates, and has y-coordinates within the median height
				ArrayList<Rectangle> previousLine = (currentLine == 0) ? null : lines.get(currentLine - 1);
				ArrayList<Rectangle> nextLine = (currentLine == (lines.size() - 1)) ? null : lines.get(currentLine + 1);
				if (debugL <= 1) {
					System.out.println(word.x + "," + word.y + " : " + "previousLine = " + previousLine
							+ "; nextLine = " + nextLine);
				}
				boolean found = false;
				int xTolerance = 3;
				if (previousLine != null) { // check in previous line
					Rectangle checkWord = null;
					innerloop1: for (Rectangle element : previousLine) {
						if (Math.abs(word.x - element.x) <= xTolerance) {
							checkWord = element;
							if (debugL <= 2) {
								System.out.println(
										word.x + "," + word.y + " : " + "checkWord in previousLine = " + checkWord);
							}
							break innerloop1;
						}
					}
					if (checkWord != null) {
						// if (Math.abs(checkWord.y - word.y) <= (overallHeightMedian * 0.75)) {
						if ((Math.abs(checkWord.y - word.y) <= Math.max(overallHeightMedian,
								heightStats.get(currentLine - 1).getPercentile(50) * 0.80))
								&& (checkWord.height < (overallHeightMedian * 0.75))
								&& (checkWord.width >= (overallWidthMedian * 0.2))
								&& (checkWord.height >= (overallHeightMedian * 0.2))) {
							found = true;
							if (debugL <= 2) {
								System.out.println(word.x + "," + word.y + " : " + "found = " + found);
							}
						}
					}
				}

				if (!found) { // now, check in the same line
					Rectangle checkWord = null;
					innerloop1: for (Rectangle element : sentence) {
						if (element != word) { // if the box in the same sentence is not the current word
							if (Math.abs(word.x - element.x) <= xTolerance) {
								checkWord = element;
								if (debugL <= 2) {
									System.out.println(
											word.x + "," + word.y + " : " + "checkWord in same line = " + checkWord);
								}
								break innerloop1;
							}
						}
					}
					if (checkWord != null) {
						// if (Math.abs(checkWord.y - word.y) <= (overallHeightMedian * 0.75)) {
						if ((Math.abs(checkWord.y - word.y) <= Math.max(overallHeightMedian,
								heightStats.get(currentLine).getPercentile(50) * 0.80))
								&& (checkWord.height < (overallHeightMedian * 0.75))
								&& (checkWord.width >= (overallWidthMedian * 0.2))
								&& (checkWord.height >= (overallHeightMedian * 0.2))) {
							found = true;
							if (debugL <= 2) {
								System.out.println(word.x + "," + word.y + " : " + "found = " + found);
							}
						}
					}
				}
				boolean foundInNextLine = false;
				int indexInNextLine = 0;
				if (!found) {
					if (nextLine != null) { // now, check in the next line
						Rectangle checkWord = null;
						innerloop1: for (Rectangle element : nextLine) {
							if (Math.abs(word.x - element.x) <= xTolerance) {
								checkWord = element;
								if (debugL <= 2) {
									System.out.println(
											word.x + "," + word.y + " : " + "checkWord in next line = " + checkWord);
								}
								break innerloop1;
							}
							++indexInNextLine;
						}
						if (checkWord != null) {
							// if (Math.abs(checkWord.y - word.y) <= (overallHeightMedian * 0.8)) {
							if ((Math.abs(checkWord.y - word.y) <= Math.max(overallHeightMedian,
									heightStats.get(currentLine + 1).getPercentile(50) * 0.80))
									&& (checkWord.height < (overallHeightMedian * 0.75))
									&& (checkWord.width >= (overallWidthMedian * 0.2))
									&& (checkWord.height >= (overallHeightMedian * 0.2))) {
								found = true;
								foundInNextLine = true;
								if (debugL <= 2) {
									System.out.println(word.x + "," + word.y + " : " + "found = " + found);
								}
							}
						}
					}
				}

				if (found) {
					++wordIndex;
					if (foundInNextLine) {
						Rectangle theWord = nextLine.get(indexInNextLine);
						nextLine.remove(theWord);
						if (word.x >= theWord.x) {
							newLine.add(theWord);
							newLine.add(word);
						} else {
							newLine.add(word);
							newLine.add(theWord);
						}
					} else {
						newLine.add(word);
					}
					if (!heightOK) { // debugging why a box of unusual height is being retained
						if (debugL <= 2) {
							System.out.print("Retaining box : ");
							System.out.print(word + " - as ");
							System.out.print("gapOK = " + gapOK + "; ");
							System.out.print("heightOK = " + heightOK + "; ");
							System.out.print("heightGood = " + heightGood + "; ");
							System.out.print("widthOk = " + widthOK + "; ");
							System.out.print("overallGapMedian = " + overallGapMedian + "; ");
							System.out.print("overallWidthMedian = " + overallWidthMedian + "; ");
							System.out.println("overallHeightMedian = " + overallHeightMedian + "; ");
						}
					}
					continue;
				}
				if (debugL <= 2) {
					System.out.print("Dropping box : ");
					System.out.print(word + " - as ");
					System.out.print("gapOK = " + gapOK + "; ");
					System.out.print("heightOK = " + heightOK + "; ");
					System.out.print("heightGood = " + heightGood + "; ");
					System.out.print("widthOk = " + widthOK + "; ");
					System.out.print("overallGapMedian = " + overallGapMedian + "; ");
					System.out.print("overallWidthMedian = " + overallWidthMedian + "; ");
					System.out.println("overallHeightMedian = " + overallHeightMedian + "; ");
				}
				++wordIndex;
			}
		}

		// Remove lines that do not have any rectangle
		int index = 0;
		ArrayList<Integer> elementsToBeRemoved = new ArrayList<>();
		for (ArrayList<Rectangle> sentence : newLines) {
			if (sentence.size() == 0) {
				elementsToBeRemoved.add(0, index);
			}
			++index;
		}
		for (int removeIndex : elementsToBeRemoved) {
			newLines.remove(removeIndex);
		}

		// Reassign overlapping loners to a line that has many boxes
		index = 0;
		// ArrayList<Integer> elementsToBeReallocated = new ArrayList<>();
		currentLine = 0;
		for (ArrayList<Rectangle> sentence : newLines) {
			boolean reallocateFirstToPrevious = false;
			boolean reallocateFirstToNext = false;
			boolean reallocateSecondToPrevious = false;
			boolean reallocateSecondToNext = false;
			ArrayList<Rectangle> previousLine = null;
			ArrayList<Rectangle> nextLine = null;
			try {
				previousLine = newLines.get(currentLine - 1);
			} catch (Exception e) {
			}
			try {
				nextLine = newLines.get(currentLine + 1);
			} catch (Exception e) {
			}
			if (sentence.size() <= 2) {
				// check overlap with previous and next lines
				Rectangle word1 = sentence.get(0);
				Rectangle word2 = null;
				try {
					word2 = sentence.get(1);
				} catch (Exception e) {
				}
				if (previousLine != null) {
					for (Rectangle word : previousLine) {
						if ((word1.y >= word.y) && (word1.y <= (word.y + word.height))) {
							reallocateFirstToPrevious = true;
						}
						if (word2 != null) {
							if ((word2.y >= word.y) && (word2.y <= (word.y + word.height))) {
								reallocateSecondToPrevious = true;
							}
						}
					}
				}
				if (nextLine != null) {
					for (Rectangle word : nextLine) {
						if ((word1.y >= word.y) && (word1.y <= (word.y + word.height))) {
							reallocateFirstToNext = true;
						}
						if (word2 != null) {
							if ((word2.y >= word.y) && (word2.y <= (word.y + word.height))) {
								reallocateSecondToNext = true;
							}
						}
					}
				}
			}
			if (reallocateFirstToPrevious) {
				Rectangle word = sentence.get(0);
				sentence.remove(word);
				index = 0;
				innerloop1: for (Rectangle checkWord : previousLine) {
					if (checkWord.x > word.x) {
						break innerloop1;
					}
					++index;
				}
				previousLine.add(index, word);
			}
			if (reallocateSecondToPrevious) {
				Rectangle word = sentence.get(1);
				sentence.remove(word);
				index = 0;
				innerloop1: for (Rectangle checkWord : previousLine) {
					if (checkWord.x > word.x) {
						break innerloop1;
					}
					++index;
				}
				previousLine.add(index, word);
			}
			if (reallocateFirstToNext) {
				Rectangle word = sentence.get(0);
				sentence.remove(word);
				index = 0;
				innerloop1: for (Rectangle checkWord : nextLine) {
					if (checkWord.x > word.x) {
						break innerloop1;
					}
					++index;
				}
				nextLine.add(index, word);
			}
			if (reallocateSecondToNext) {
				Rectangle word = sentence.get(1);
				sentence.remove(word);
				index = 0;
				innerloop1: for (Rectangle checkWord : previousLine) {
					if (checkWord.x > word.x) {
						break innerloop1;
					}
					++index;
				}
				nextLine.add(index, word);
			}
			++currentLine;
		}

		// Again, remove lines that do not have any rectangle
		index = 0;
		elementsToBeRemoved = new ArrayList<>();
		for (ArrayList<Rectangle> sentence : newLines) {
			if (sentence.size() == 0) {
				elementsToBeRemoved.add(0, index);
			}
			++index;
		}
		for (int removeIndex : elementsToBeRemoved) {
			newLines.remove(removeIndex);
		}

		// Recalibrate the stats from the remaining boxes; check if this is now needed

		/*
		 * widthStats = new ArrayList<>(); heightStats = new ArrayList<>();
		 * widthGapStats = new ArrayList<>(); heightStartStats = new ArrayList<>();
		 *
		 * overallWidthStats = new DescriptiveStatistics(); overallHeightStats = new
		 * DescriptiveStatistics(); overallGapStats = new DescriptiveStatistics();
		 *
		 * currentLine = -1; for (ArrayList<Rectangle> sentence : lines) {
		 * DescriptiveStatistics currentWidthStats = new DescriptiveStatistics();
		 * DescriptiveStatistics currentHeightStats = new DescriptiveStatistics();
		 * DescriptiveStatistics currentWidthGapStats = new DescriptiveStatistics();
		 * DescriptiveStatistics currentHeightStartStats = new DescriptiveStatistics();
		 * ++currentLine; int wordIndex = 0; for (Rectangle word : sentence) {
		 * currentWidthStats.addValue(word.width);
		 * currentHeightStats.addValue(word.height);
		 * overallWidthStats.addValue(word.width);
		 * overallHeightStats.addValue(word.height); if (wordIndex++ != 0) { int gap =
		 * word.x - sentence.get(wordIndex - 1).x - sentence.get(wordIndex - 1).width;
		 * currentWidthGapStats.addValue(gap); overallGapStats.addValue(gap); }
		 * currentHeightStartStats.addValue(word.y); }
		 * widthStats.add(currentWidthStats); heightStats.add(currentHeightStats);
		 * widthGapStats.add(currentWidthGapStats);
		 * heightStartStats.add(currentHeightStartStats); }
		 *
		 * overallHeightMedian = overallHeightStats.getPercentile(50); overallHeightMean
		 * = overallHeightStats.getMean(); overallWidthMedian =
		 * overallWidthStats.getPercentile(50); overallWidthMean =
		 * overallWidthStats.getMean(); overallGapMedian =
		 * overallGapStats.getPercentile(50);
		 */

		if (debugL <= 1) {
			for (ArrayList<Rectangle> line : newLines) {
				System.out.print("New Line - ");
				System.out.println(Arrays.toString(line.toArray(new Rectangle[line.size()])));
			}
		}
		return newLines;
	}

	public static ArrayList<ArrayList<Rectangle>> rectifyBoxesForPK1(ArrayList<ArrayList<Rectangle>> lines,
			int debugL) {

		// This was the original method. Changed in rectifyBoxesForPK to incorporate
		// height cut off at 0.4 and nesting of the code that reallocates boxes of loner
		// sentences

		// Having segregated the boxes into new lines, do a second pass for cleanup
		// 1. Remove loner small dimension boxes (Done)
		// 2A. Change the dilate sel centre; position it at the center of the sel, and
		// ensure that the box coordinates are reduced by the dilation pixels (Done)
		// 3. Remove colon, semi-colon, comma, dash, equals, underscore, apostrophes,
		// etc
		// 2. Remove boxes with low connected components and high black pixel %age (TBD)
		// 3. Introduce space boxes (SpaceBox extends Box) to isolate words (TBD)
		// 4. Split boxes, if you determine that 2 lines have coalesced (TBD)
		// 5. After boxes have been identified, create new word boxes (TBD)

		// double boxWidthAverage = 0.0;
		// double boxHeightAverage = 0.0;
		// double boxWidthSigma = 0.0;
		// double boxHeightSigma = 0.0;
		ArrayList<DescriptiveStatistics> widthStats = new ArrayList<>();
		ArrayList<DescriptiveStatistics> heightStats = new ArrayList<>();
		ArrayList<DescriptiveStatistics> widthGapStats = new ArrayList<>();
		ArrayList<DescriptiveStatistics> heightStartStats = new ArrayList<>();

		DescriptiveStatistics overallWidthStats = new DescriptiveStatistics();
		DescriptiveStatistics overallHeightStats = new DescriptiveStatistics();
		DescriptiveStatistics overallGapStats = new DescriptiveStatistics();

		int currentLine = -1;

		// Iterate through the rectangles and get the DescriptiveStatistics

		for (ArrayList<Rectangle> sentence : lines) {
			DescriptiveStatistics currentWidthStats = new DescriptiveStatistics();
			DescriptiveStatistics currentHeightStats = new DescriptiveStatistics();
			DescriptiveStatistics currentWidthGapStats = new DescriptiveStatistics();
			DescriptiveStatistics currentHeightStartStats = new DescriptiveStatistics();
			++currentLine;
			int wordIndex = 0;
			for (Rectangle word : sentence) {
				currentWidthStats.addValue(word.width);
				currentHeightStats.addValue(word.height);
				overallWidthStats.addValue(word.width);
				overallHeightStats.addValue(word.height);
				if (wordIndex != 0) {
					int gap = Math.max(0, word.x - sentence.get(wordIndex - 1).x - sentence.get(wordIndex - 1).width);
					currentWidthGapStats.addValue(gap);
					overallGapStats.addValue(gap);
				}
				currentHeightStartStats.addValue(word.y);
				++wordIndex;
			}
			widthStats.add(currentWidthStats);
			heightStats.add(currentHeightStats);
			widthGapStats.add(currentWidthGapStats);
			heightStartStats.add(currentHeightStartStats);
		}

		double overallHeightMedian = overallHeightStats.getPercentile(50);
		// double overallHeightMean = overallHeightStats.getMean();
		double overallWidthMedian = overallWidthStats.getPercentile(50);
		// double overallWidthMean = overallWidthStats.getMean();
		double overallGapMedian = overallGapStats.getPercentile(50);
		// double gapTolerancePercent = 10.0; // 5.0 = 500 % tolerance

		// Iterate through the rectangles and remove the loner small ones
		currentLine = -1;
		ArrayList<ArrayList<Rectangle>> newLines = new ArrayList<>();
		for (ArrayList<Rectangle> sentence : lines) {
			ArrayList<Rectangle> newLine = new ArrayList<>();
			newLines.add(newLine);
			++currentLine;
			int wordIndex = 0;
			for (Rectangle word : sentence) {
				boolean heightOK = (word.height >= (overallHeightMedian * 0.575));
				boolean widthOK = (word.width >= (overallWidthMedian * 0.4));
				boolean gapOK = false;
				Rectangle previousWord = null;
				Rectangle nextWord = null;
				try {
					previousWord = sentence.get(wordIndex - 1);
				} catch (Exception e) {
				}
				try {
					nextWord = sentence.get(wordIndex + 1);
				} catch (Exception e) {
				}
				// if the box is surrounded on both sides by boxes of good height, then accept
				// the box
				if ((previousWord != null) && (nextWord != null)) {
					if ((previousWord.height > (overallHeightMedian * 0.6))
							&& (nextWord.height > (overallHeightMedian * 0.6))) {
						gapOK = true;
					}
				}

				if (gapOK) {
					newLine.add(word);
					++wordIndex;
					continue;
				}
				if (!heightOK) {
					if (debugL <= 2) {
						System.out.print("Dropping box as height not OK: ");
						System.out.print(word + " - as ");
						System.out.print("gapOK = " + gapOK + "; ");
						System.out.print("heightOK = " + heightOK + "; ");
						System.out.print("widthOk = " + widthOK + "; ");
						System.out.print("overallGapMedian = " + overallGapMedian + "; ");
						System.out.print("overallWidthMedian = " + overallWidthMedian + "; ");
						System.out.println("overallHeightMedian = " + overallHeightMedian + "; ");
					}
					++wordIndex;
					continue;
				}
				if (heightOK && widthOK) {
					newLine.add(word);
					++wordIndex;
					continue;
				}
				if (debugL <= 2) {
					System.out.print("Dropping box : ");
					System.out.print(word + " - as ");
					System.out.print("gapOK = " + gapOK + "; ");
					System.out.print("heightOK = " + heightOK + "; ");
					System.out.print("widthOk = " + widthOK + "; ");
					System.out.print("overallGapMedian = " + overallGapMedian + "; ");
					System.out.print("overallWidthMedian = " + overallWidthMedian + "; ");
					System.out.println("overallHeightMedian = " + overallHeightMedian + "; ");
				}
				++wordIndex;
			}
		}

		// Remove lines that do not have any rectangle
		int index = 0;
		ArrayList<Integer> elementsToBeRemoved = new ArrayList<>();
		for (ArrayList<Rectangle> sentence : newLines) {
			if (sentence.size() == 0) {
				elementsToBeRemoved.add(0, index);
			}
			++index;
		}
		for (int removeIndex : elementsToBeRemoved) {
			newLines.remove(removeIndex);
		}

		// Reassign overlapping loners to a line that has many boxes
		index = 0;
		// ArrayList<Integer> elementsToBeReallocated = new ArrayList<>();
		currentLine = 0;
		for (ArrayList<Rectangle> sentence : newLines) {
			boolean reallocateFirstToPrevious = false;
			boolean reallocateFirstToNext = false;
			boolean reallocateSecondToPrevious = false;
			boolean reallocateSecondToNext = false;
			ArrayList<Rectangle> previousLine = null;
			ArrayList<Rectangle> nextLine = null;
			try {
				previousLine = newLines.get(currentLine - 1);
			} catch (Exception e) {
			}
			try {
				nextLine = newLines.get(currentLine + 1);
			} catch (Exception e) {
			}
			if (sentence.size() <= 2) {
				// check overlap with previous and next lines
				Rectangle word1 = sentence.get(0);
				Rectangle word2 = null;
				try {
					word2 = sentence.get(1);
				} catch (Exception e) {
				}
				if (previousLine != null) {
					for (Rectangle word : previousLine) {
						if ((word1.y >= word.y) && (word1.y <= (word.y + word.height))) {
							reallocateFirstToPrevious = true;
						}
						if (word2 != null) {
							if ((word2.y >= word.y) && (word2.y <= (word.y + word.height))) {
								reallocateSecondToPrevious = true;
							}
						}
					}
				}
				if (nextLine != null) {
					for (Rectangle word : nextLine) {
						if ((word1.y >= word.y) && (word1.y <= (word.y + word.height))) {
							reallocateFirstToNext = true;
						}
						if (word2 != null) {
							if ((word2.y >= word.y) && (word2.y <= (word.y + word.height))) {
								reallocateSecondToNext = true;
							}
						}
					}
				}
			}
			if (reallocateFirstToPrevious) {
				Rectangle word = sentence.get(0);
				sentence.remove(word);
				index = 0;
				innerloop1: for (Rectangle checkWord : previousLine) {
					if (checkWord.x > word.x) {
						break innerloop1;
					}
					++index;
				}
				previousLine.add(index, word);
			}
			if (reallocateSecondToPrevious) {
				Rectangle word = sentence.get(0);
				sentence.remove(word);
				index = 0;
				innerloop1: for (Rectangle checkWord : previousLine) {
					if (checkWord.x > word.x) {
						break innerloop1;
					}
					++index;
				}
				previousLine.add(index, word);
			}
			if (reallocateFirstToNext) {
				Rectangle word = sentence.get(0);
				sentence.remove(word);
				index = 0;
				innerloop1: for (Rectangle checkWord : nextLine) {
					if (checkWord.x > word.x) {
						break innerloop1;
					}
					++index;
				}
				nextLine.add(index, word);
			}
			if (reallocateSecondToNext) {
				Rectangle word = sentence.get(0);
				sentence.remove(word);
				index = 0;
				innerloop1: for (Rectangle checkWord : previousLine) {
					if (checkWord.x > word.x) {
						break innerloop1;
					}
					++index;
				}
				nextLine.add(index, word);
			}
			++currentLine;
		}

		// Again, remove lines that do not have any rectangle
		index = 0;
		elementsToBeRemoved = new ArrayList<>();
		for (ArrayList<Rectangle> sentence : newLines) {
			if (sentence.size() == 0) {
				elementsToBeRemoved.add(0, index);
			}
			++index;
		}
		for (int removeIndex : elementsToBeRemoved) {
			newLines.remove(removeIndex);
		}

		if (debugL <= 1) {
			for (ArrayList<Rectangle> line : newLines) {
				System.out.print("New Line - ");
				System.out.println(Arrays.toString(line.toArray(new Rectangle[line.size()])));
			}
		}
		return newLines;
	}

	public static ArrayList<ArrayList<Rectangle>> rectifyBoxesForPK(ArrayList<ArrayList<Rectangle>> lines, int debugL) {

		// Having segregated the boxes into new lines, do a second pass for cleanup
		// 1. Remove loner small dimension boxes (Done)
		// 2. Remove colon, semi-colon, comma, dash, equals, underscore, apostrophes,
		// etc
		// 3. Remove boxes with low connected components and high black pixel %age (TBD)
		// 4. Introduce space boxes (SpaceBox extends Box) to isolate words (TBD)
		// 5. Split boxes, if you determine that 2 lines have coalesced (TBD)
		// 6. After boxes have been identified, create new word boxes (TBD)

		// double boxWidthAverage = 0.0;
		// double boxHeightAverage = 0.0;
		// double boxWidthSigma = 0.0;
		// double boxHeightSigma = 0.0;
		ArrayList<DescriptiveStatistics> widthStats = new ArrayList<>();
		ArrayList<DescriptiveStatistics> heightStats = new ArrayList<>();
		ArrayList<DescriptiveStatistics> widthGapStats = new ArrayList<>();
		ArrayList<DescriptiveStatistics> heightStartStats = new ArrayList<>();

		DescriptiveStatistics overallWidthStats = new DescriptiveStatistics();
		DescriptiveStatistics overallHeightStats = new DescriptiveStatistics();
		DescriptiveStatistics overallGapStats = new DescriptiveStatistics();

		int currentLine = -1;

		// Iterate through the rectangles and get the DescriptiveStatistics

		for (ArrayList<Rectangle> sentence : lines) {
			DescriptiveStatistics currentWidthStats = new DescriptiveStatistics();
			DescriptiveStatistics currentHeightStats = new DescriptiveStatistics();
			DescriptiveStatistics currentWidthGapStats = new DescriptiveStatistics();
			DescriptiveStatistics currentHeightStartStats = new DescriptiveStatistics();
			++currentLine;
			int wordIndex = 0;
			for (Rectangle word : sentence) {
				currentWidthStats.addValue(word.width);
				currentHeightStats.addValue(word.height);
				overallWidthStats.addValue(word.width);
				overallHeightStats.addValue(word.height);
				if (wordIndex != 0) {
					int gap = Math.max(0, word.x - sentence.get(wordIndex - 1).x - sentence.get(wordIndex - 1).width);
					currentWidthGapStats.addValue(gap);
					overallGapStats.addValue(gap);
				}
				currentHeightStartStats.addValue(word.y);
				++wordIndex;
			}
			widthStats.add(currentWidthStats);
			heightStats.add(currentHeightStats);
			widthGapStats.add(currentWidthGapStats);
			heightStartStats.add(currentHeightStartStats);
		}

		double overallHeightMedian = overallHeightStats.getPercentile(50);
		// double overallHeightMean = overallHeightStats.getMean();
		double overallWidthMedian = overallWidthStats.getPercentile(50);
		// double overallWidthMean = overallWidthStats.getMean();
		double overallGapMedian = overallGapStats.getPercentile(50);
		// double gapTolerancePercent = 10.0; // where, 5.0 = 500 % tolerance

		// Iterate through the rectangles and remove the loner small ones
		currentLine = -1;
		ArrayList<ArrayList<Rectangle>> newLines = new ArrayList<>();
		for (ArrayList<Rectangle> sentence : lines) {
			ArrayList<Rectangle> newLine = new ArrayList<>();
			newLines.add(newLine);
			++currentLine;
			int wordIndex = 0;
			for (Rectangle word : sentence) {
				boolean heightOK = (word.height >= (overallHeightMedian * 0.4));
				boolean widthOK = (word.width >= (overallWidthMedian * 0.4));

				// don't discard a box of small dimensions immediately. First, check if it is
				// wedged between 2 boxes of good height because then it might a full stop,
				// comma, etc
				boolean gapOK = false;
				Rectangle previousWord = null;
				Rectangle nextWord = null;

				// get the previous word and next word
				try {
					previousWord = sentence.get(wordIndex - 1);
				} catch (Exception e) {
				}
				try {
					nextWord = sentence.get(wordIndex + 1);
				} catch (Exception e) {
				}
				// if the box is surrounded on both sides by boxes of good height, then accept
				// the box
				if ((previousWord != null) && (nextWord != null)) {
					if ((previousWord.height > (overallHeightMedian * 0.6))
							&& (nextWord.height > (overallHeightMedian * 0.6))) {
						gapOK = true;
					}
				}

				if (gapOK) {
					newLine.add(word);
					++wordIndex;
					continue;
				}
				if (!heightOK) {
					if (debugL <= 2) {
						System.out.print("Dropping box as height not OK: ");
						System.out.print(word + " - as ");
						System.out.print("gapOK = " + gapOK + "; ");
						System.out.print("heightOK = " + heightOK + "; ");
						System.out.print("widthOk = " + widthOK + "; ");
						System.out.print("overallGapMedian = " + overallGapMedian + "; ");
						System.out.print("overallWidthMedian = " + overallWidthMedian + "; ");
						System.out.println("overallHeightMedian = " + overallHeightMedian + "; ");
					}
					++wordIndex;
					continue;
				}
				if (heightOK && widthOK) {
					newLine.add(word);
					++wordIndex;
					continue;
				}
				// drop boxes whose height is not OK and width is not OK
				if (debugL <= 2) {
					System.out.print("Dropping box : ");
					System.out.print(word + " - as ");
					System.out.print("gapOK = " + gapOK + "; ");
					System.out.print("heightOK = " + heightOK + "; ");
					System.out.print("widthOk = " + widthOK + "; ");
					System.out.print("overallGapMedian = " + overallGapMedian + "; ");
					System.out.print("overallWidthMedian = " + overallWidthMedian + "; ");
					System.out.println("overallHeightMedian = " + overallHeightMedian + "; ");
				}
				++wordIndex;
			}
		}

		// Remove lines that do not have any rectangle
		int index = 0;
		ArrayList<Integer> elementsToBeRemoved = new ArrayList<>();
		for (ArrayList<Rectangle> sentence : newLines) {
			if (sentence.size() == 0) {
				elementsToBeRemoved.add(0, index);
			}
			++index;
		}
		for (int removeIndex : elementsToBeRemoved) {
			newLines.remove(removeIndex);
		}

		// Reassign overlapping loners to a line that has many boxes
		index = 0;
		// ArrayList<Integer> elementsToBeReallocated = new ArrayList<>();
		currentLine = 0;
		for (ArrayList<Rectangle> sentence : newLines) {
			boolean reallocateFirstToPrevious = false;
			boolean reallocateFirstToNext = false;
			boolean reallocateSecondToPrevious = false;
			boolean reallocateSecondToNext = false;
			ArrayList<Rectangle> previousLine = null;
			ArrayList<Rectangle> nextLine = null;

			// get previous line and next line
			try {
				previousLine = newLines.get(currentLine - 1);
			} catch (Exception e) {
			}
			try {
				nextLine = newLines.get(currentLine + 1);
			} catch (Exception e) {
			}
			// start reassignment of loners i.e. size of sentence = 1 or 2
			if (sentence.size() <= 2) {
				// check overlap with previous and next lines
				Rectangle word1 = sentence.get(0); // at least 1 word will be there, as empty lines have been deleted
				Rectangle word2 = null;
				try {
					word2 = sentence.get(1);
				} catch (Exception e) {
				}
				if (previousLine != null) {
					for (Rectangle word : previousLine) {
						if ((word1.y >= word.y) && (word1.y <= (word.y + word.height))) {
							reallocateFirstToPrevious = true;
						}
						if (word2 != null) {
							if ((word2.y >= word.y) && (word2.y <= (word.y + word.height))) {
								reallocateSecondToPrevious = true;
							}
						}
					}
				}
				if (nextLine != null) {
					for (Rectangle word : nextLine) {
						if ((word1.y >= word.y) && (word1.y <= (word.y + word.height))) {
							reallocateFirstToNext = true;
						}
						if (word2 != null) {
							if ((word2.y >= word.y) && (word2.y <= (word.y + word.height))) {
								reallocateSecondToNext = true;
							}
						}
					}
				}
			}
			if (reallocateFirstToPrevious) {
				Rectangle word = sentence.get(0);
				sentence.remove(word);
				index = 0;
				innerloop1: for (Rectangle checkWord : previousLine) {
					if (checkWord.x > word.x) {
						break innerloop1;
					}
					++index;
				}
				previousLine.add(index, word);
			} else {
				if (reallocateFirstToNext) {
					Rectangle word = sentence.get(0);
					sentence.remove(word);
					index = 0;
					innerloop1: for (Rectangle checkWord : nextLine) {
						if (checkWord.x > word.x) {
							break innerloop1;
						}
						++index;
					}
					nextLine.add(index, word);
				}
			}
			if (reallocateSecondToPrevious) {
				Rectangle word = sentence.get(0);
				sentence.remove(word);
				index = 0;
				innerloop1: for (Rectangle checkWord : previousLine) {
					if (checkWord.x > word.x) {
						break innerloop1;
					}
					++index;
				}
				previousLine.add(index, word);
			} else {
				if (reallocateSecondToNext) {
					Rectangle word = sentence.get(0);
					sentence.remove(word);
					index = 0;
					innerloop1: for (Rectangle checkWord : previousLine) {
						if (checkWord.x > word.x) {
							break innerloop1;
						}
						++index;
					}
					nextLine.add(index, word);
				}
			}
			++currentLine;
		}

		// Again, remove lines that do not have any rectangle
		index = 0;
		elementsToBeRemoved = new ArrayList<>();
		for (ArrayList<Rectangle> sentence : newLines) {
			if (sentence.size() == 0) {
				elementsToBeRemoved.add(0, index);
			}
			++index;
		}
		for (int removeIndex : elementsToBeRemoved) {
			newLines.remove(removeIndex);
		}

		if (debugL <= 1) {
			for (ArrayList<Rectangle> line : newLines) {
				System.out.print("New Line - ");
				System.out.println(Arrays.toString(line.toArray(new Rectangle[line.size()])));
			}
		}

		// now, check if there are boxes in a line that are far apart. If yes, separate
		// them into new lines

		ArrayList<ArrayList<Rectangle>> finalLines = new ArrayList<>();
		ArrayList<Rectangle> aNewLine;
		DescriptiveStatistics hStats = new DescriptiveStatistics();
		DescriptiveStatistics gapStats = new DescriptiveStatistics();
		for (ArrayList<Rectangle> sentence : newLines) {
			hStats.clear();
			gapStats.clear();
			int idx = 0;
			for (Rectangle word : sentence) {
				hStats.addValue(word.height);
				if (idx > 0) {
					gapStats.addValue(word.x - sentence.get(idx - 1).x - sentence.get(idx - 1).width);
				}
				++idx;
			}
			double averageWidth = hStats.getMean() * 0.75;
			// double averageGap = gapStats.getPercentile(50);
			idx = 0;
			// the line needs to be split at places where the gap is > 3*averageWidth
			aNewLine = new ArrayList<>();
			for (Rectangle word : sentence) {
				if (idx == 0) {
					aNewLine.add(word);
					++idx;
					continue;
				}
				if ((word.x - sentence.get(idx - 1).x - sentence.get(idx - 1).width) > (3 * averageWidth)) {
					finalLines.add(aNewLine);
					aNewLine = new ArrayList<>();
					aNewLine.add(word);
				} else {
					aNewLine.add(word);
				}
				++idx;
			}
			finalLines.add(aNewLine);
		}

		return finalLines;
	}

	public static ArrayList<ArrayList<Rectangle>> mergeBoxesIntoWords(ArrayList<ArrayList<Rectangle>> lines,
			int debugL) {
		// To Do
		// Process the symbol boxes and create word boxes, based on gaps between symbol
		// boxes
		return lines;
	}

	public static ArrayList<Rectangle> mergeBoxesIntoLines(Pix cleanedPix, ArrayList<ArrayList<Rectangle>> lines,
			int debugL) {

		// SBImage linesCleanedSBImage = null;
		// try {
		// SBImage.getSBImageFromPix(cleanedPix);
		// } catch (Exception e) {
		// }

		ArrayList<DescriptiveStatistics> widthStats = new ArrayList<>();
		ArrayList<DescriptiveStatistics> heightStats = new ArrayList<>();
		ArrayList<DescriptiveStatistics> widthGapStats = new ArrayList<>();
		// ArrayList<DescriptiveStatistics> heightStartStats = new ArrayList<>();

		DescriptiveStatistics overallWidthStats = new DescriptiveStatistics();
		DescriptiveStatistics overallHeightStats = new DescriptiveStatistics();
		DescriptiveStatistics overallGapStats = new DescriptiveStatistics();

		int currentLine = -1;

		// Iterate through the rectangles and get the DescriptiveStatistics

		for (ArrayList<Rectangle> sentence : lines) {
			DescriptiveStatistics currentWidthStats = new DescriptiveStatistics();
			DescriptiveStatistics currentHeightStats = new DescriptiveStatistics();
			DescriptiveStatistics currentWidthGapStats = new DescriptiveStatistics();
			// DescriptiveStatistics currentHeightStartStats = new DescriptiveStatistics();
			++currentLine;
			int wordIndex = -1;
			for (Rectangle word : sentence) {
				++wordIndex;
				currentWidthStats.addValue(word.width);
				currentHeightStats.addValue(word.height);
				overallWidthStats.addValue(word.width);
				overallHeightStats.addValue(word.height);
				if (wordIndex != 0) {
					int gap = Math.max(0, word.x - sentence.get(wordIndex - 1).x - sentence.get(wordIndex - 1).width);
					currentWidthGapStats.addValue(gap);
					overallGapStats.addValue(gap);
				}
				// currentHeightStartStats.addValue(word.y);
			}
			widthStats.add(currentWidthStats);
			heightStats.add(currentHeightStats);
			widthGapStats.add(currentWidthGapStats);
			// heightStartStats.add(currentHeightStartStats);
		}

		// double overallHeightMedian = overallHeightStats.getPercentile(50);
		double overallHeightMean = overallHeightStats.getMean();
		double firstLevelHeightMean = overallHeightMean;
		double firstLevelWidthMean = overallWidthStats.getMean();
		// double overallWidthMedian = overallWidthStats.getPercentile(50);
		// double overallWidthMean = overallWidthStats.getMean();
		// double overallGapMedian = overallGapStats.getPercentile(50);
		// double gapTolerancePercent = 10.0; // 5.0 = 500 % tolerance

		// 2. if a box is unusually large in a line, look for a split point somewhere
		// between 20% to 80% of height and split the box
		// to put it into a different line

		ArrayList<ArrayList<Rectangle>> realignedBoxLines = new ArrayList<>();
		currentLine = -1;
		for (ArrayList<Rectangle> line : lines) {
			++currentLine;
			DescriptiveStatistics lineGapStats = widthGapStats.get(currentLine);
			int medianGap = Math.max((int) overallGapStats.getPercentile(50), (int) lineGapStats.getPercentile(50));
			// Acceptable gap is the gap between words. Note that the multiplication factors
			// are large because we are multiplying the median value, not the mean value
			int acceptableGap = (medianGap > 3) ? medianGap * 13 : (medianGap == 3 ? medianGap * 17 : medianGap * 24);
			acceptableGap = Math.max((int) (firstLevelWidthMean * 3), acceptableGap);
			ArrayList<Rectangle> newLine = new ArrayList<>();
			realignedBoxLines.add(newLine);
			int wordCount = -1;
			for (Rectangle word : line) {
				++wordCount;
				if (wordCount == 0) {
					newLine.add(word);
				} else {
					if (lineGapStats.getElement(wordCount - 1) <= acceptableGap) {
						newLine.add(word);
					} else {
						newLine = new ArrayList<Rectangle>();
						realignedBoxLines.add(newLine);
						newLine.add(word);
						if (debugL <= 2) {
							System.out.println("Added a new line as word gap of "
									+ lineGapStats.getElement(wordCount - 1) + " is greater than " + acceptableGap);
						}
					}
				}
			}
		}
		String codeMarker = "This is the 1st call to SBImageUtils.drawBoundingBoxesOnPix";
		drawBoundingBoxesOnPix(cleanedPix, realignedBoxLines, ImageNumbers.getSymbolBoxNumber(), debugL, codeMarker);

		// recalculate the stats, after the realignment of boxes into lines. While the
		// overall stats won't change, the stats for each line would change as new lines
		// have been introduced
		widthStats = new ArrayList<>();
		heightStats = new ArrayList<>();
		widthGapStats = new ArrayList<>();
		// heightStartStats = new ArrayList<>();

		overallWidthStats = new DescriptiveStatistics();
		overallHeightStats = new DescriptiveStatistics();
		overallGapStats = new DescriptiveStatistics();

		currentLine = -1;

		// Iterate through the rectangles again and get the DescriptiveStatistics

		for (ArrayList<Rectangle> sentence : realignedBoxLines) {
			DescriptiveStatistics currentWidthStats = new DescriptiveStatistics();
			DescriptiveStatistics currentHeightStats = new DescriptiveStatistics();
			DescriptiveStatistics currentWidthGapStats = new DescriptiveStatistics();
			// DescriptiveStatistics currentHeightStartStats = new DescriptiveStatistics();
			++currentLine;
			int wordIndex = 0;
			for (Rectangle word : sentence) {
				currentWidthStats.addValue(word.width);
				currentHeightStats.addValue(word.height);
				overallWidthStats.addValue(word.width);
				overallHeightStats.addValue(word.height);
				if (wordIndex != 0) {
					int gap = Math.max(0, word.x - sentence.get(wordIndex - 1).x - sentence.get(wordIndex - 1).width);
					currentWidthGapStats.addValue(gap);
					overallGapStats.addValue(gap);
				}
				// currentHeightStartStats.addValue(word.y);
				++wordIndex;
			}
			widthStats.add(currentWidthStats);
			heightStats.add(currentHeightStats);
			widthGapStats.add(currentWidthGapStats);
			// heightStartStats.add(currentHeightStartStats);
		}

		// overallHeightMedian = overallHeightStats.getPercentile(50);
		overallHeightMean = overallHeightStats.getMean();

		// Identify the lines that have boxes of unusually large height. No more than 2
		// such boxes per line. If there are 3 or more such boxes, delete the line

		// Also, boxes must not be overlapping with other boxes in the same line
		// If such boxes exist, then split them down the middle where the no of black
		// pixels is least in a line

		ArrayList<ArrayList<Integer>> unusuallyLargeBoxes = new ArrayList<>();
		ArrayList<ArrayList<Boolean>> largeBoxesOverlapWithOthersInLine = new ArrayList<>();
		currentLine = -1;
		for (ArrayList<Rectangle> line : realignedBoxLines) {
			++currentLine;
			ArrayList<Integer> boxIndices = new ArrayList<>();
			ArrayList<Boolean> overlapIndicator = new ArrayList<>();
			unusuallyLargeBoxes.add(boxIndices);
			largeBoxesOverlapWithOthersInLine.add(overlapIndicator);
			int wordCount = -1;
			// unusually large boxes added
			for (Rectangle word : line) {
				++wordCount;
				if (word.height > (1.5 * overallHeightMean)) {
					if (debugL <= 2) {
						System.out.println("Found a large box in line : " + currentLine);
					}
					boxIndices.add(wordCount);
				}
			}

			int boxCount = -1;
			innerloop: for (Integer boxIndex : boxIndices) {
				++boxCount;
				overlapIndicator.add(Boolean.FALSE);
				Rectangle largeBox = line.get(boxIndex);
				for (Rectangle word : line) {
					if (((word.x <= largeBox.x) && (largeBox.x < (word.x + word.width)))
							|| ((word.x >= largeBox.x) && (word.x < (largeBox.x + largeBox.width)))) {
						overlapIndicator.set(boxCount, Boolean.TRUE);
						if (debugL <= 2) {
							System.out.println("Found an overlapping box with a large box in line : " + currentLine
									+ " at boxCount : " + boxCount);
						}
						continue innerloop;
					}
				}
			}
		}

		// Having got the count of unusually large boxes and the overlap count,
		// go through the lines again :-
		// a) if the no of large boxes >= 3, delete the line. If the no of large boxes >
		// 1, then chances are high that the line is all jumbled up. Break up the large
		// boxes and create smaller boxes
		// b) if the no of overlaps > 1, then chances are high that the line is all
		// jumbled up. However, this needs to be postponed to after the large boxes have
		// been broken up and reallocated
		// c)

		ArrayList<Integer> linesToBeDeleted = new ArrayList<>();
		ArrayList<Rectangle> newBoxes = new ArrayList<>();
		for (int i = 0; i < realignedBoxLines.size(); ++i) {
			ArrayList<Rectangle> line = realignedBoxLines.get(i);
			ArrayList<Integer> boxIndices = unusuallyLargeBoxes.get(i);
			int noOfUnusuallyLargeHeightBoxes = boxIndices.size();
			// if no of large boxes >= 3, delete the line
			if (noOfUnusuallyLargeHeightBoxes >= 3) {
				linesToBeDeleted.add(i);
				if (debugL <= 2) {
					System.out.println("Marked line : " + i + " for deletion as no of large boxes >= 3");
				}
				continue;
			}
			if (((noOfUnusuallyLargeHeightBoxes * 1.0) / Math.max(1, line.size())) >= 0.5) {
				if (debugL <= 2) {
					System.out.println("Marked line : " + i + " for deletion as %age of large boxes >= 50%");
				}
				linesToBeDeleted.add(i);
				continue;
			}
			// if there are no large boxes, keep the line as is
			if (noOfUnusuallyLargeHeightBoxes == 0) {
				continue;
			}
			// find the average height of boxes in the line, after excluding the large boxes
			int totalHeight = 0;
			int noOfBoxes = 0;
			for (int j = 0; j < line.size(); ++j) {
				if (boxIndices.indexOf(Integer.valueOf(j)) != -1) {
					totalHeight += line.get(j).height;
					++noOfBoxes;
				}
			}
			int averageHeight = totalHeight / (Math.max(1, noOfBoxes)); // changed from double to int
			int whitePixel = 0;
			int depth = Leptonica1.pixGetDepth(cleanedPix);
			if (depth != 1) {
				whitePixel = (int) Math.pow(2, depth) - 1;
			}
			for (Integer boxIndex : boxIndices) {
				// cleanedImage has to be passed as a parameter
				if (boxIndex.intValue() < line.size()) {
					Rectangle r = line.get(boxIndex.intValue());
					// int noOfVerticalDivisionsNeeded = (int) Math.round(r.height / averageHeight);
					int noOfVerticalDivisionsNeeded = (r.height / averageHeight) + 1;
					Box box = new Box(r.x, r.y, r.width, r.height, 0);
					Pix pix = Leptonica1.pixClipRectangle(cleanedPix, box, null);
					int[][] boxPixels = new int[r.height][r.width];
					for (int y = 0; y < r.height; ++y) {
						for (int x = 0; x < r.width; ++x) {
							IntBuffer value = IntBuffer.allocate(1);
							Leptonica1.pixGetPixel(pix, x, y, value);
							boxPixels[y][x] = value.get();
						}
					}
					// LeptUtils.dispose(box); Not required as the Box is created in the VM !!
					LeptUtils.dispose(pix);

					// the algo is that we make a starting guess for the box break based on average
					// height of normal sized boxes, and iterate +/-20%
					// from there to find a horizontal line with minimum black pixels. That line is
					// our break line

					// Get all the breaklines for each large box; then, create new boxes
					// corresponding to
					// these breaklines, and finally add these new boxes to a newboxes arraylist

					// add all boxes belonging to the line of the largebox to the newboxes arraylist
					// as they have to be reallocated to new lines
					double searchTolerancePercentage = 0.4 / noOfVerticalDivisionsNeeded;
					int searchTolerance = (int) (r.height * searchTolerancePercentage);
					ArrayList<Integer> boxBreaks = new ArrayList<>();
					for (int d = 1; d < noOfVerticalDivisionsNeeded; ++d) {
						int yMid = (r.height * d) / noOfVerticalDivisionsNeeded;
						int yStart = Math.max(0, yMid - searchTolerance);
						int yEnd = Math.min(r.height - 1, yMid + searchTolerance);
						int yTarget = yStart;
						int leastBlackPixelCount = Integer.MAX_VALUE;
						// find the line for which the blackPixelCount is least
						for (int y = yStart; y <= yEnd; ++y) {
							int blackPixelCount = 0;
							for (int x = 0; x < r.width; ++x) {
								if (boxPixels[y][x] != whitePixel) {
									++blackPixelCount;
								}
							}
							if (blackPixelCount <= leastBlackPixelCount) {
								leastBlackPixelCount = blackPixelCount;
								yTarget = y;
							}
						}
						// compare with the blackPixelCount for yMid
						// set yTarget to yMid, if there is not much difference between the 2
						// blackPixelCounts
						int yMidBlackPixelCount = 0;
						for (int x = 0; x < r.width; ++x) {
							if (boxPixels[yMid][x] != whitePixel) {
								++yMidBlackPixelCount;
							}
						}
						double noDifferenceTolerance = 0.1; // diff in black pixel count between yMid and least line
															// should
															// be > 10%
						double ratio = leastBlackPixelCount / Math.max(1, yMidBlackPixelCount);
						if ((ratio < (1 + noDifferenceTolerance)) && (ratio > (1 - noDifferenceTolerance))) {
							yTarget = yMid;
						}
						if (debugL <= 2) {
							System.out
									.println("Adding yTarget of " + yTarget + " to ArrayList boxBreaks for line " + i);
						}
						boxBreaks.add(yTarget);
					}
					if (debugL <= 2) {
						System.out.println(
								"Number of breaks required for large box = Size of box breaks : " + boxBreaks.size());
					}
					if (boxBreaks.size() > 0) {
						boxBreakLoop: for (int j = 0; j <= boxBreaks.size(); ++j) {
							if (j == 0) {

								newBoxes.add(new Rectangle(r.x, r.y + 1, r.width, boxBreaks.get(j) - 1));
								continue boxBreakLoop;
							}
							if (j == boxBreaks.size()) {
								newBoxes.add(new Rectangle(r.x, r.y + boxBreaks.get(j - 1) + 1, r.width,
										r.height - boxBreaks.get(j - 1) - 1));
								break boxBreakLoop;
							}
							newBoxes.add(new Rectangle(r.x, r.y + boxBreaks.get(j - 1), r.width,
									boxBreaks.get(j) - boxBreaks.get(j - 1) - 1));
						}
						// After new boxes created for a large box, and added to newBoxes ArrayList, now
						// we delete the large boxes, and
						line.remove(boxIndex.intValue());
					}
				}
			}
			// remove all remaining boxes from the line, and add them to newboxes, because
			// these boxes have to be redistributed to new lines
			int noOfRemainingBoxes = line.size();
			for (int j = noOfRemainingBoxes - 1; j >= 0; --j) {
				newBoxes.add(line.get(j));
				line.remove(j);
			}
		}

		// remove empty lines from realignedBoxLines
		int size = realignedBoxLines.size();
		for (int j = size - 1; j >= 0; --j) {
			if (realignedBoxLines.get(j).size() == 0) {
				realignedBoxLines.remove(j);
			}
		}
		codeMarker = "This is the 2nd call to SBImageUtils.drawBoundingBoxesOnPix";
		drawBoundingBoxesOnPix(cleanedPix, realignedBoxLines, ImageNumbers.getSymbolBoxNumber(), debugL, codeMarker);

		// Allocate the new boxes to the appropriate line in the arraylist
		// Find the closest box to each box, storing them in an arraylist. As you trawl
		// through the boxes, if you find a box that is closer than the current one,
		// remove the previous box from the arraylist and replace with the new one
		// Store also the index of the line in which they need to be fitted

		ArrayList<ArrayList<Rectangle>> newBoxLines = segregateBoxesIntoLines(newBoxes, debugL);
		// ensure that there are no empty lines in both newBoxLines
		size = newBoxLines.size();
		for (int j = size - 1; j >= 0; --j) {
			if (newBoxLines.get(j).size() == 0) {
				newBoxLines.remove(j);
			}
		}
		codeMarker = "This is the 3rd call to SBImageUtils.drawBoundingBoxesOnPix";
		drawBoundingBoxesOnPix(cleanedPix, newBoxLines, ImageNumbers.getSymbolBoxNumber(), debugL, codeMarker);

		/*
		 * ArrayList<Rectangle> closestBox = new ArrayList<>(); ArrayList<Integer>
		 * closestBoxInWhichLine = new ArrayList<>(); int yTolerance = 5; // y-tolerance
		 * on box.y - closestBox.y int currentBoxIndex = -1; for (Rectangle newBox :
		 * newBoxes) { currentLine = -1; ++currentBoxIndex; lineLoop: for
		 * (ArrayList<Rectangle> line : realignedBoxLines) { ++currentLine; boxLoop: for
		 * (Rectangle box : line) { // if the box is not in the current line, skip the
		 * box if (Math.abs(newBox.y - box.y) >= yTolerance) { continue boxLoop; } //
		 * seed the closestBox arraylist at the currentBoxIndex if (closestBox.size() <=
		 * currentBoxIndex) { closestBox.add(currentBoxIndex, box);
		 * closestBoxInWhichLine.add(currentBoxIndex, currentLine); continue boxLoop; }
		 *
		 * // find the current gap between the orphaned box and its closest box. Since
		 * the // closest box changes in the loop, this distance needs to be
		 * recalculated // everytime in the loop int currentClosestXGap = Math.min(
		 * Math.abs(closestBox.get(currentBoxIndex).x - newBox.x - newBox.width),
		 * Math.abs((closestBox.get(currentBoxIndex).x +
		 * closestBox.get(currentBoxIndex).width) - newBox.x));
		 *
		 * // find the current gap between the orphaned box and the box in a line int
		 * currentXGap = Math.min(Math.abs(box.x - newBox.x - newBox.width),
		 * Math.abs((box.x + box.width) - newBox.x));
		 *
		 * if (currentXGap < currentClosestXGap) { // replace the closest box with this
		 * box closestBox.remove(currentBoxIndex); closestBox.add(currentBoxIndex, box);
		 * closestBoxInWhichLine.remove(currentBoxIndex);
		 * closestBoxInWhichLine.add(currentBoxIndex, currentLine); } } } // if the new
		 * box doesn't fit into any existing line // then add another line, containing
		 * the new box, to realignedBoxLines if (closestBox.size() <= currentBoxIndex) {
		 * ArrayList<Rectangle> aNewLine = new ArrayList<>(); aNewLine.add(newBox);
		 * realignedBoxLines.add(aNewLine); closestBox.add(currentBoxIndex, null);
		 * closestBoxInWhichLine.add(currentBoxIndex, -1); } }
		 *
		 * // After looping through all the new boxes and finding their closest boxes,
		 * find // where in the line they should fit and insert them there
		 *
		 * currentBoxIndex = -1; boxAddingLoop: for (Rectangle newBox : newBoxes) {
		 * ++currentBoxIndex; int lineForBox =
		 * closestBoxInWhichLine.get(currentBoxIndex); if (lineForBox == -1) { continue
		 * boxAddingLoop; } Rectangle nearestBox = closestBox.get(currentBoxIndex);
		 * ArrayList<Rectangle> line = realignedBoxLines.get(lineForBox); int
		 * nearestBoxIndex = line.indexOf(nearestBox); if (newBox.x < nearestBox.x) {
		 * line.add(nearestBoxIndex, newBox); } else { line.add(nearestBoxIndex + 1,
		 * newBox); } }
		 */

		// add the new lines to the original line array
		for (ArrayList<Rectangle> line : newBoxLines) {
			int index = -1;
			searchLoop: for (ArrayList<Rectangle> oldLine : realignedBoxLines) {
				++index;
				if (line.get(0).y < oldLine.get(0).y) {
					break searchLoop;
				}
			}
			if (index >= 0) {
				realignedBoxLines.add(index, line);
			}
		}

		// remove lines where there are overlapping boxes
		ArrayList<Boolean> boxesOverlapInLine = new ArrayList<>();
		currentLine = -1;
		int xTolerance = 4;
		for (ArrayList<Rectangle> line : realignedBoxLines) {
			++currentLine;
			boxesOverlapInLine.add(Boolean.FALSE);
			innerloop: for (Rectangle word : line) {
				for (Rectangle otherWord : line) {
					if ((otherWord != word) && (((word.x < (otherWord.x - xTolerance))
							&& ((otherWord.x + xTolerance) < (word.x + word.width)))
							|| ((word.x < ((otherWord.x + otherWord.width) - xTolerance))
									&& ((word.x + word.width) > (otherWord.x + otherWord.width + xTolerance))))) {
						// check if the smaller word is completely subsumed within the larger word
						boolean subsumed = false;
						Rectangle smallerWord = null, largerWord = null;
						if (otherWord.width <= word.width) {
							smallerWord = otherWord;
							largerWord = word;
						} else {
							smallerWord = word;
							largerWord = otherWord;
						}
						int tolerance = 2;
						int imageWidth = Leptonica1.pixGetWidth(cleanedPix);
						int imageHeight = Leptonica1.pixGetHeight(cleanedPix);
						if ((Math.max(0, largerWord.x - tolerance) <= smallerWord.x)
								&& (Math.min(imageWidth,
										largerWord.x + largerWord.width
												+ tolerance) >= (smallerWord.x + smallerWord.width))
								&& (Math.max(0, largerWord.y - tolerance) < smallerWord.y)
								&& (Math.min(imageHeight, largerWord.y + largerWord.height + tolerance) > (smallerWord.y
										+ smallerWord.height))) {
							subsumed = true;
						}
						if (!subsumed) {
							boxesOverlapInLine.set(currentLine, Boolean.TRUE);
							if (debugL <= 2) {
								System.out.println("Found an overlapping box in line : " + currentLine);
							}
							break innerloop;
						}
					}
				}
			}
		}

		for (int index = boxesOverlapInLine.size() - 1; index >= 0; --index) {
			if (boxesOverlapInLine.get(index)) {
				realignedBoxLines.remove(index);
			}
		}

		// in the remaining lines, merge all symbol boxes within the same line into one
		// line box

		DescriptiveStatistics lineHeights = new DescriptiveStatistics();
		ArrayList<Rectangle> newLines = new ArrayList<>();
		for (ArrayList<Rectangle> line : realignedBoxLines) {
			int yTop = Integer.MAX_VALUE;
			int yBot = 0;
			int xLeft = Integer.MAX_VALUE;
			int xRight = 0;
			for (Rectangle word : line) {
				yTop = Math.min(yTop, word.y);
				yBot = Math.max(yBot, word.y + word.height);
				xLeft = Math.min(xLeft, word.x);
				xRight = Math.max(xRight, word.x + word.width);
			}
			Rectangle newBox = new Rectangle(xLeft, yTop, xRight - xLeft, yBot - yTop);
			lineHeights.addValue(yBot - yTop);
			newLines.add(newBox);
		}

		overallHeightMean = (int) lineHeights.getMean();
		int medianHeight = (int) lineHeights.getPercentile(50);

		// remove lines that are too large in height or too small
		ArrayList<Integer> unusualHeightLines = new ArrayList<>();
		for (int i = 0; i < newLines.size(); ++i) {
			// if ((newLines.get(i).height < (overallHeightMean * 0.667))
			// || (newLines.get(i).height > (overallHeightMean * 1.5))) {
			if (debugL <= 2) {
				System.out.println("Line " + i + " height is " + newLines.get(i).height);
			}
			if ((newLines.get(i).height < (medianHeight * 0.667)) || (newLines.get(i).height > (medianHeight * 2.125))
					|| (newLines.get(i).height < (firstLevelHeightMean * 0.5))) {
				unusualHeightLines.add(i);
				if (debugL <= 2) {
					System.out.println("Added line " + i + " for removal because height is " + newLines.get(i).height
							+ "while height cutoffs are " + (int) (medianHeight * 0.667) + " and "
							+ (int) (medianHeight * 2.125));
				}
			}
		}
		// lop off lines starting from the end of the arraylist
		for (int i = unusualHeightLines.size() - 1; i >= 0; --i) {
			newLines.remove(unusualHeightLines.get(i).intValue());
		}

		// 1. Iterate twice through the lines and merge those that are on the same line,
		// provided the lines are separated by less than 2*height
		// 2. Iterate through the lines, and merge those that have a significant area
		// overlap

		// merging lines that are more or less on the same line, and not so distant from
		// each other
		ArrayList<Rectangle> semifinal1 = new ArrayList<>();
		int oldIdx = 0;
		int newIdx = 0;
		for (Rectangle line : newLines) {
			if (oldIdx == 0) {
				semifinal1.add(line);
				++oldIdx;
				++newIdx;
				continue;
			}
			if (((line.y < Math.max(0, semifinal1.get(newIdx - 1).y - (line.height * 0.5)))
					&& ((line.y + line.height) < Math.max(0,
							semifinal1.get(newIdx - 1).y + (semifinal1.get(newIdx - 1).height * 1.4))))
					&& ((Math.abs(line.x - semifinal1.get(newIdx - 1).x - semifinal1.get(newIdx - 1).width) < (3
							* line.height))
							|| (Math.abs((line.x + line.width) - semifinal1.get(newIdx - 1).x) < (3 * line.height)))) {
				int newX = Math.min(line.x, semifinal1.get(newIdx - 1).x);
				int newY = Math.min(line.y, semifinal1.get(newIdx - 1).y);
				int newWidth = Math.max(line.x + line.width,
						semifinal1.get(newIdx - 1).x + semifinal1.get(newIdx - 1).width) - newX;
				int newHeight = Math.max(line.y + line.height,
						semifinal1.get(newIdx - 1).y + semifinal1.get(newIdx - 1).height) - newY;
				Rectangle newRect = new Rectangle(newX, newY, newWidth, newHeight);
				semifinal1.set(newIdx - 1, newRect);
				++oldIdx;
				continue;
			} else {
				semifinal1.add(line);
				++oldIdx;
				++newIdx;
			}
		}

		// if there is a significant area overlap with a previous line, then merge with
		// previous line
		ArrayList<Rectangle> finalLines = new ArrayList<>();
		oldIdx = 0;
		newIdx = 0;
		for (Rectangle line : semifinal1) {
			if (oldIdx == 0) {
				finalLines.add(line);
				++oldIdx;
				++newIdx;
				continue;
			}
			Rectangle previousLine = finalLines.get(newIdx - 1);
			/*
			 * boolean noOverlap = (line.x > (previousBox.x + previousBox.width)); noOverlap
			 * = noOverlap || (previousBox.x > (line.x + line.width)); noOverlap = noOverlap
			 * || (line.y < (previousBox.y + previousBox.height)); noOverlap = noOverlap ||
			 * (previousBox.y < (line.y + line.height)); if (noOverlap) {
			 * semifinal2.add(line); ++oldIdx; ++newIdx; continue; } int currentArea =
			 * line.width * line.height; boolean topLeftIsInside = ((line.x >
			 * semifinal2.get(newIdx - 1).x) && (line.x < (semifinal2.get(newIdx - 1).x +
			 * semifinal2.get(newIdx - 1).width)) && (line.y > semifinal2.get(newIdx - 1).y)
			 * && (line.y < (semifinal2.get(newIdx - 1).y + semifinal2.get(newIdx -
			 * 1).height))); boolean topRightIsInside = (((line.x + line.width) >
			 * semifinal2.get(newIdx - 1).x) && ((line.x + line.width) <
			 * (semifinal2.get(newIdx - 1).x + semifinal2.get(newIdx - 1).width)) &&
			 * ((line.y + line.height) > semifinal2.get(newIdx - 1).y) && ((line.y +
			 * line.height) < (semifinal2.get(newIdx - 1).y + semifinal2.get(newIdx -
			 * 1).height))); boolean bottomLeftIsInside = ((line.x > semifinal2.get(newIdx -
			 * 1).x) && (line.x < (semifinal2.get(newIdx - 1).x + semifinal2.get(newIdx -
			 * 1).width)) && (line.y > semifinal2.get(newIdx - 1).y) && (line.y <
			 * (semifinal2.get(newIdx - 1).y + semifinal2.get(newIdx - 1).height)));
			 */
			int xOverlap = Math.max(0, Math.min(line.x + line.width, previousLine.x + previousLine.width)
					- Math.max(line.x, previousLine.x));
			int yOverlap = Math.max(0, Math.min(line.y + line.height, previousLine.y + previousLine.height)
					- Math.max(line.y, previousLine.y));
			int overlapArea = xOverlap * yOverlap;
			int currentArea1 = line.width * line.height;
			int currentArea2 = previousLine.width * previousLine.height;
			double overlapPercent1 = overlapArea / currentArea1;
			double overlapPercent2 = overlapArea / currentArea2;
			double cutoffPercent = 0.33;
			if ((overlapPercent1 < cutoffPercent) && (overlapPercent2 < cutoffPercent)) {
				finalLines.add(line);
				++oldIdx;
				++newIdx;
				continue;
			}
			int newX = Math.min(line.x, previousLine.x);
			int newY = Math.min(line.y, previousLine.y);
			int newWidth = Math.max(line.x + line.width, previousLine.x + previousLine.width) - newX;
			int newHeight = Math.max(line.y + line.height, previousLine.y + previousLine.height) - newY;
			finalLines.set(newIdx - 1, new Rectangle(newX, newY, newWidth, newHeight));
			++oldIdx;
		}

		return finalLines;
	}

	public static ArrayList<Rectangle> mergeBoxesIntoLines1(Pix cleanedPix, ArrayList<ArrayList<Rectangle>> lines,
			int debugL) {

		// SBImage linesCleanedSBImage = null;
		// try {
		// SBImage.getSBImageFromPix(cleanedPix);
		// } catch (Exception e) {
		// }

		ArrayList<DescriptiveStatistics> widthStats = new ArrayList<>();
		ArrayList<DescriptiveStatistics> heightStats = new ArrayList<>();
		ArrayList<DescriptiveStatistics> widthGapStats = new ArrayList<>();
		// ArrayList<DescriptiveStatistics> heightStartStats = new ArrayList<>();

		DescriptiveStatistics overallWidthStats = new DescriptiveStatistics();
		DescriptiveStatistics overallHeightStats = new DescriptiveStatistics();
		DescriptiveStatistics overallGapStats = new DescriptiveStatistics();

		int currentLine = -1;

		// Iterate through the rectangles and get the DescriptiveStatistics

		for (ArrayList<Rectangle> sentence : lines) {
			DescriptiveStatistics currentWidthStats = new DescriptiveStatistics();
			DescriptiveStatistics currentHeightStats = new DescriptiveStatistics();
			DescriptiveStatistics currentWidthGapStats = new DescriptiveStatistics();
			// DescriptiveStatistics currentHeightStartStats = new DescriptiveStatistics();
			++currentLine;
			int wordIndex = -1;
			for (Rectangle word : sentence) {
				++wordIndex;
				currentWidthStats.addValue(word.width);
				currentHeightStats.addValue(word.height);
				overallWidthStats.addValue(word.width);
				overallHeightStats.addValue(word.height);
				if (wordIndex != 0) {
					int gap = Math.max(0, word.x - sentence.get(wordIndex - 1).x - sentence.get(wordIndex - 1).width);
					currentWidthGapStats.addValue(gap);
					overallGapStats.addValue(gap);
				}
				// currentHeightStartStats.addValue(word.y);
			}
			widthStats.add(currentWidthStats);
			heightStats.add(currentHeightStats);
			widthGapStats.add(currentWidthGapStats);
			// heightStartStats.add(currentHeightStartStats);
		}

		// double overallHeightMedian = overallHeightStats.getPercentile(50);
		double overallHeightMean = overallHeightStats.getMean();
		double firstLevelHeightMean = overallHeightMean;
		double firstLevelWidthMean = overallWidthStats.getMean();
		// double overallWidthMedian = overallWidthStats.getPercentile(50);
		// double overallWidthMean = overallWidthStats.getMean();
		// double overallGapMedian = overallGapStats.getPercentile(50);
		// double gapTolerancePercent = 10.0; // 5.0 = 500 % tolerance

		// 2. if a box is unusually large in a line, look for a split point somewhere
		// between 20% to 80% of height and split the box
		// to put it into a different line

		ArrayList<ArrayList<Rectangle>> realignedBoxLines = new ArrayList<>();
		currentLine = -1;
		for (ArrayList<Rectangle> line : lines) {
			++currentLine;
			DescriptiveStatistics lineGapStats = widthGapStats.get(currentLine);
			int medianGap = Math.max((int) overallGapStats.getPercentile(50), (int) lineGapStats.getPercentile(50));
			// Acceptable gap is the gap between words. Note that the multiplication factors
			// are large because we are multiplying the median value, not the mean value
			int acceptableGap = (medianGap > 3) ? medianGap * 13 : (medianGap == 3 ? medianGap * 17 : medianGap * 24);
			acceptableGap = Math.max((int) (firstLevelWidthMean * 3), acceptableGap);
			ArrayList<Rectangle> newLine = new ArrayList<>();
			realignedBoxLines.add(newLine);
			int wordCount = -1;
			for (Rectangle word : line) {
				++wordCount;
				if (wordCount == 0) {
					newLine.add(word);
				} else {
					if (lineGapStats.getElement(wordCount - 1) <= acceptableGap) {
						newLine.add(word);
					} else {
						newLine = new ArrayList<Rectangle>();
						realignedBoxLines.add(newLine);
						newLine.add(word);
						if (debugL <= 2) {
							System.out.println("Added a new line as word gap of "
									+ lineGapStats.getElement(wordCount - 1) + " is greater than " + acceptableGap);
						}
					}
				}
			}
		}
		String codeMarker = "This is the 1st call to SBImageUtils.drawBoundingBoxesOnPix";
		drawBoundingBoxesOnPix(cleanedPix, realignedBoxLines, ImageNumbers.getSymbolBoxNumber(), debugL, codeMarker);

		// recalculate the stats, after the realignment of boxes into lines. While the
		// overall stats won't change, the stats for each line would change as new lines
		// have been introduced
		widthStats = new ArrayList<>();
		heightStats = new ArrayList<>();
		widthGapStats = new ArrayList<>();
		// heightStartStats = new ArrayList<>();

		overallWidthStats = new DescriptiveStatistics();
		overallHeightStats = new DescriptiveStatistics();
		overallGapStats = new DescriptiveStatistics();

		currentLine = -1;

		// Iterate through the rectangles again and get the DescriptiveStatistics

		for (ArrayList<Rectangle> sentence : realignedBoxLines) {
			DescriptiveStatistics currentWidthStats = new DescriptiveStatistics();
			DescriptiveStatistics currentHeightStats = new DescriptiveStatistics();
			DescriptiveStatistics currentWidthGapStats = new DescriptiveStatistics();
			// DescriptiveStatistics currentHeightStartStats = new DescriptiveStatistics();
			++currentLine;
			int wordIndex = 0;
			for (Rectangle word : sentence) {
				currentWidthStats.addValue(word.width);
				currentHeightStats.addValue(word.height);
				overallWidthStats.addValue(word.width);
				overallHeightStats.addValue(word.height);
				if (wordIndex != 0) {
					int gap = Math.max(0, word.x - sentence.get(wordIndex - 1).x - sentence.get(wordIndex - 1).width);
					currentWidthGapStats.addValue(gap);
					overallGapStats.addValue(gap);
				}
				// currentHeightStartStats.addValue(word.y);
				++wordIndex;
			}
			widthStats.add(currentWidthStats);
			heightStats.add(currentHeightStats);
			widthGapStats.add(currentWidthGapStats);
			// heightStartStats.add(currentHeightStartStats);
		}

		// overallHeightMedian = overallHeightStats.getPercentile(50);
		overallHeightMean = overallHeightStats.getMean();

		// Identify the lines that have boxes of unusually large height. No more than 5
		// such boxes per line. If there are 6 or more such boxes, delete the line

		// Also, boxes must not be overlapping with other boxes in the same line
		// If such boxes exist, then split them down the middle where the no of black
		// pixels is least in a line

		ArrayList<ArrayList<Integer>> unusuallyLargeBoxes = new ArrayList<>();
		ArrayList<ArrayList<Boolean>> largeBoxesOverlapWithOthersInLine = new ArrayList<>();
		currentLine = -1;
		for (ArrayList<Rectangle> line : realignedBoxLines) {
			++currentLine;
			ArrayList<Integer> boxIndices = new ArrayList<>();
			ArrayList<Boolean> overlapIndicator = new ArrayList<>();
			unusuallyLargeBoxes.add(boxIndices);
			largeBoxesOverlapWithOthersInLine.add(overlapIndicator);
			int wordCount = -1;
			// unusually large boxes added
			for (Rectangle word : line) {
				++wordCount;
				if (word.height > (1.5 * overallHeightMean)) {
					if (debugL <= 2) {
						System.out.println("Found a large box in line : " + currentLine);
					}
					boxIndices.add(wordCount);
				}
			}

			int boxCount = -1;
			innerloop: for (Integer boxIndex : boxIndices) {
				++boxCount;
				overlapIndicator.add(Boolean.FALSE);
				Rectangle largeBox = line.get(boxIndex);
				for (Rectangle word : line) {
					if (((word.x <= largeBox.x) && (largeBox.x < (word.x + word.width)))
							|| ((word.x >= largeBox.x) && (word.x < (largeBox.x + largeBox.width)))) {
						overlapIndicator.set(boxCount, Boolean.TRUE);
						if (debugL <= 2) {
							System.out.println("Found an overlapping box with a large box in line : " + currentLine
									+ " at boxCount : " + boxCount);
						}
						continue innerloop;
					}
				}
			}
		}

		// Having got the count of unusually large boxes and the overlap count,
		// go through the lines again :-
		// a) if the no of large boxes >= 6 (key change from the method
		// mergeBoxesIntoLines()), delete the line. If the no of large boxes >
		// 1, then chances are high that the line is all jumbled up. Break up the large
		// boxes and create smaller boxes
		// b) if the no of overlaps > 1, then chances are high that the line is all
		// jumbled up. However, this needs to be postponed to after the large boxes have
		// been broken up and reallocated
		// c)

		ArrayList<Integer> linesToBeDeleted = new ArrayList<>();
		ArrayList<Rectangle> newBoxes = new ArrayList<>();
		for (int i = 0; i < realignedBoxLines.size(); ++i) {
			ArrayList<Rectangle> line = realignedBoxLines.get(i);
			ArrayList<Integer> boxIndices = unusuallyLargeBoxes.get(i);
			int noOfUnusuallyLargeHeightBoxes = boxIndices.size();
			// if no of large boxes >= 6, delete the line
			if (noOfUnusuallyLargeHeightBoxes >= 6) {
				linesToBeDeleted.add(i);
				if (debugL <= 2) {
					System.out.println("Marked line : " + i + " for deletion as no of large boxes >= 3");
				}
				continue;
			}
			if (((noOfUnusuallyLargeHeightBoxes * 1.0) / Math.max(1, line.size())) >= 0.66) { // changed to 0.66 from
																								// 0.5
				if (debugL <= 2) {
					System.out.println("Marked line : " + i + " for deletion as %age of large boxes >= 66%");
				}
				linesToBeDeleted.add(i);
				continue;
			}
			// if there are no large boxes, keep the line as is
			if (noOfUnusuallyLargeHeightBoxes == 0) {
				continue;
			}
			// find the average height of boxes in the line, after excluding the large boxes
			int totalHeight = 0;
			int noOfBoxes = 0;
			for (int j = 0; j < line.size(); ++j) {
				if (boxIndices.indexOf(Integer.valueOf(j)) != -1) {
					totalHeight += line.get(j).height;
					++noOfBoxes;
				}
			}
			int averageHeight = totalHeight / (Math.max(1, noOfBoxes)); // changed from double to int
			int whitePixel = 0;
			int depth = Leptonica1.pixGetDepth(cleanedPix);
			if (depth != 1) {
				whitePixel = (int) Math.pow(2, depth) - 1;
			}
			for (Integer boxIndex : boxIndices) {
				// cleanedImage has to be passed as a parameter
				if (boxIndex.intValue() < line.size()) {
					Rectangle r = line.get(boxIndex.intValue());
					// int noOfVerticalDivisionsNeeded = (int) Math.round(r.height / averageHeight);
					int noOfVerticalDivisionsNeeded = (r.height / averageHeight) + 1;
					Box box = new Box(r.x, r.y, r.width, r.height, 0);
					Pix pix = Leptonica1.pixClipRectangle(cleanedPix, box, null);
					int[][] boxPixels = new int[r.height][r.width];
					for (int y = 0; y < r.height; ++y) {
						for (int x = 0; x < r.width; ++x) {
							IntBuffer value = IntBuffer.allocate(1);
							Leptonica1.pixGetPixel(pix, x, y, value);
							boxPixels[y][x] = value.get();
						}
					}
					// LeptUtils.dispose(box); Not required as the Box is created in the VM !!
					LeptUtils.dispose(pix);

					// the algo is that we make a starting guess for the box break based on average
					// height of normal sized boxes, and iterate +/-20%
					// from there to find a horizontal line with minimum black pixels. That line is
					// our break line

					// Get all the breaklines for each large box; then, create new boxes
					// corresponding to
					// these breaklines, and finally add these new boxes to a newboxes arraylist

					// add all boxes belonging to the line of the largebox to the newboxes arraylist
					// as they have to be reallocated to new lines
					double searchTolerancePercentage = 0.4 / noOfVerticalDivisionsNeeded;
					int searchTolerance = (int) (r.height * searchTolerancePercentage);
					ArrayList<Integer> boxBreaks = new ArrayList<>();
					for (int d = 1; d < noOfVerticalDivisionsNeeded; ++d) {
						int yMid = (r.height * d) / noOfVerticalDivisionsNeeded;
						int yStart = Math.max(0, yMid - searchTolerance);
						int yEnd = Math.min(r.height - 1, yMid + searchTolerance);
						int yTarget = yStart;
						int leastBlackPixelCount = Integer.MAX_VALUE;
						// find the line for which the blackPixelCount is least
						for (int y = yStart; y <= yEnd; ++y) {
							int blackPixelCount = 0;
							for (int x = 0; x < r.width; ++x) {
								if (boxPixels[y][x] != whitePixel) {
									++blackPixelCount;
								}
							}
							if (blackPixelCount <= leastBlackPixelCount) {
								leastBlackPixelCount = blackPixelCount;
								yTarget = y;
							}
						}
						// compare with the blackPixelCount for yMid
						// set yTarget to yMid, if there is not much difference between the 2
						// blackPixelCounts
						int yMidBlackPixelCount = 0;
						for (int x = 0; x < r.width; ++x) {
							if (boxPixels[yMid][x] != whitePixel) {
								++yMidBlackPixelCount;
							}
						}
						double noDifferenceTolerance = 0.1; // diff in black pixel count between yMid and least line
															// should
															// be > 10%
						double ratio = leastBlackPixelCount / Math.max(1, yMidBlackPixelCount);
						if ((ratio < (1 + noDifferenceTolerance)) && (ratio > (1 - noDifferenceTolerance))) {
							yTarget = yMid;
						}
						if (debugL <= 2) {
							System.out
									.println("Adding yTarget of " + yTarget + " to ArrayList boxBreaks for line " + i);
						}
						boxBreaks.add(yTarget);
					}
					if (debugL <= 2) {
						System.out.println(
								"Number of breaks required for large box = Size of box breaks : " + boxBreaks.size());
					}
					if (boxBreaks.size() > 0) {
						boxBreakLoop: for (int j = 0; j <= boxBreaks.size(); ++j) {
							if (j == 0) {

								newBoxes.add(new Rectangle(r.x, r.y + 1, r.width, boxBreaks.get(j) - 1));
								continue boxBreakLoop;
							}
							if (j == boxBreaks.size()) {
								newBoxes.add(new Rectangle(r.x, r.y + boxBreaks.get(j - 1) + 1, r.width,
										r.height - boxBreaks.get(j - 1) - 1));
								break boxBreakLoop;
							}
							newBoxes.add(new Rectangle(r.x, r.y + boxBreaks.get(j - 1), r.width,
									boxBreaks.get(j) - boxBreaks.get(j - 1) - 1));
						}
						// After new boxes created for a large box, and added to newBoxes ArrayList, now
						// we delete the large boxes, and
						line.remove(boxIndex.intValue());
					}
				}
			}
			// remove all remaining boxes from the line, and add them to newboxes, because
			// these boxes have to be redistributed to new lines
			int noOfRemainingBoxes = line.size();
			for (int j = noOfRemainingBoxes - 1; j >= 0; --j) {
				newBoxes.add(line.get(j));
				line.remove(j);
			}
		}

		// remove empty lines from realignedBoxLines
		int size = realignedBoxLines.size();
		for (int j = size - 1; j >= 0; --j) {
			if (realignedBoxLines.get(j).size() == 0) {
				realignedBoxLines.remove(j);
			}
		}
		codeMarker = "This is the 2nd call to SBImageUtils.drawBoundingBoxesOnPix";
		drawBoundingBoxesOnPix(cleanedPix, realignedBoxLines, ImageNumbers.getSymbolBoxNumber(), debugL, codeMarker);

		// Allocate the new boxes to the appropriate line in the arraylist
		// Find the closest box to each box, storing them in an arraylist. As you trawl
		// through the boxes, if you find a box that is closer than the current one,
		// remove the previous box from the arraylist and replace with the new one
		// Store also the index of the line in which they need to be fitted

		ArrayList<ArrayList<Rectangle>> newBoxLines = segregateBoxesIntoLines(newBoxes, debugL);
		// ensure that there are no empty lines in both newBoxLines
		size = newBoxLines.size();
		for (int j = size - 1; j >= 0; --j) {
			if (newBoxLines.get(j).size() == 0) {
				newBoxLines.remove(j);
			}
		}
		codeMarker = "This is the 3rd call to SBImageUtils.drawBoundingBoxesOnPix";
		drawBoundingBoxesOnPix(cleanedPix, newBoxLines, ImageNumbers.getSymbolBoxNumber(), debugL, codeMarker);

		/*
		 * ArrayList<Rectangle> closestBox = new ArrayList<>(); ArrayList<Integer>
		 * closestBoxInWhichLine = new ArrayList<>(); int yTolerance = 5; // y-tolerance
		 * on box.y - closestBox.y int currentBoxIndex = -1; for (Rectangle newBox :
		 * newBoxes) { currentLine = -1; ++currentBoxIndex; lineLoop: for
		 * (ArrayList<Rectangle> line : realignedBoxLines) { ++currentLine; boxLoop: for
		 * (Rectangle box : line) { // if the box is not in the current line, skip the
		 * box if (Math.abs(newBox.y - box.y) >= yTolerance) { continue boxLoop; } //
		 * seed the closestBox arraylist at the currentBoxIndex if (closestBox.size() <=
		 * currentBoxIndex) { closestBox.add(currentBoxIndex, box);
		 * closestBoxInWhichLine.add(currentBoxIndex, currentLine); continue boxLoop; }
		 *
		 * // find the current gap between the orphaned box and its closest box. Since
		 * the // closest box changes in the loop, this distance needs to be
		 * recalculated // everytime in the loop int currentClosestXGap = Math.min(
		 * Math.abs(closestBox.get(currentBoxIndex).x - newBox.x - newBox.width),
		 * Math.abs((closestBox.get(currentBoxIndex).x +
		 * closestBox.get(currentBoxIndex).width) - newBox.x));
		 *
		 * // find the current gap between the orphaned box and the box in a line int
		 * currentXGap = Math.min(Math.abs(box.x - newBox.x - newBox.width),
		 * Math.abs((box.x + box.width) - newBox.x));
		 *
		 * if (currentXGap < currentClosestXGap) { // replace the closest box with this
		 * box closestBox.remove(currentBoxIndex); closestBox.add(currentBoxIndex, box);
		 * closestBoxInWhichLine.remove(currentBoxIndex);
		 * closestBoxInWhichLine.add(currentBoxIndex, currentLine); } } } // if the new
		 * box doesn't fit into any existing line // then add another line, containing
		 * the new box, to realignedBoxLines if (closestBox.size() <= currentBoxIndex) {
		 * ArrayList<Rectangle> aNewLine = new ArrayList<>(); aNewLine.add(newBox);
		 * realignedBoxLines.add(aNewLine); closestBox.add(currentBoxIndex, null);
		 * closestBoxInWhichLine.add(currentBoxIndex, -1); } }
		 *
		 * // After looping through all the new boxes and finding their closest boxes,
		 * find // where in the line they should fit and insert them there
		 *
		 * currentBoxIndex = -1; boxAddingLoop: for (Rectangle newBox : newBoxes) {
		 * ++currentBoxIndex; int lineForBox =
		 * closestBoxInWhichLine.get(currentBoxIndex); if (lineForBox == -1) { continue
		 * boxAddingLoop; } Rectangle nearestBox = closestBox.get(currentBoxIndex);
		 * ArrayList<Rectangle> line = realignedBoxLines.get(lineForBox); int
		 * nearestBoxIndex = line.indexOf(nearestBox); if (newBox.x < nearestBox.x) {
		 * line.add(nearestBoxIndex, newBox); } else { line.add(nearestBoxIndex + 1,
		 * newBox); } }
		 */

		// add the new lines to the original line array
		for (ArrayList<Rectangle> line : newBoxLines) {
			int index = -1;
			searchLoop: for (ArrayList<Rectangle> oldLine : realignedBoxLines) {
				++index;
				if (line.get(0).y < oldLine.get(0).y) {
					break searchLoop;
				}
			}
			if (index >= 0) {
				realignedBoxLines.add(index, line);
			}
		}

		// remove lines where there are overlapping boxes
		ArrayList<Boolean> boxesOverlapInLine = new ArrayList<>();
		currentLine = -1;
		int xTolerance = 4;
		for (ArrayList<Rectangle> line : realignedBoxLines) {
			++currentLine;
			boxesOverlapInLine.add(Boolean.FALSE);
			innerloop: for (Rectangle word : line) {
				for (Rectangle otherWord : line) {
					if ((otherWord != word) && (((word.x < (otherWord.x - xTolerance))
							&& ((otherWord.x + xTolerance) < (word.x + word.width)))
							|| ((word.x < ((otherWord.x + otherWord.width) - xTolerance))
									&& ((word.x + word.width) > (otherWord.x + otherWord.width + xTolerance))))) {
						// check if the smaller word is completely subsumed within the larger word
						boolean subsumed = false;
						Rectangle smallerWord = null, largerWord = null;
						if (otherWord.width <= word.width) {
							smallerWord = otherWord;
							largerWord = word;
						} else {
							smallerWord = word;
							largerWord = otherWord;
						}
						int tolerance = 2;
						int imageWidth = Leptonica1.pixGetWidth(cleanedPix);
						int imageHeight = Leptonica1.pixGetHeight(cleanedPix);
						if ((Math.max(0, largerWord.x - tolerance) <= smallerWord.x)
								&& (Math.min(imageWidth,
										largerWord.x + largerWord.width
												+ tolerance) >= (smallerWord.x + smallerWord.width))
								&& (Math.max(0, largerWord.y - tolerance) < smallerWord.y)
								&& (Math.min(imageHeight, largerWord.y + largerWord.height + tolerance) > (smallerWord.y
										+ smallerWord.height))) {
							subsumed = true;
						}
						if (!subsumed) {
							boxesOverlapInLine.set(currentLine, Boolean.TRUE);
							if (debugL <= 2) {
								System.out.println("Found an overlapping box in line : " + currentLine);
							}
							break innerloop;
						}
					}
				}
			}
		}

		for (int index = boxesOverlapInLine.size() - 1; index >= 0; --index) {
			if (boxesOverlapInLine.get(index)) {
				realignedBoxLines.remove(index);
			}
		}

		// in the remaining lines, merge all symbol boxes within the same line into one
		// line box

		DescriptiveStatistics lineHeights = new DescriptiveStatistics();
		ArrayList<Rectangle> newLines = new ArrayList<>();
		for (ArrayList<Rectangle> line : realignedBoxLines) {
			int yTop = Integer.MAX_VALUE;
			int yBot = 0;
			int xLeft = Integer.MAX_VALUE;
			int xRight = 0;
			for (Rectangle word : line) {
				yTop = Math.min(yTop, word.y);
				yBot = Math.max(yBot, word.y + word.height);
				xLeft = Math.min(xLeft, word.x);
				xRight = Math.max(xRight, word.x + word.width);
			}
			Rectangle newBox = new Rectangle(xLeft, yTop, xRight - xLeft, yBot - yTop);
			lineHeights.addValue(yBot - yTop);
			newLines.add(newBox);
		}

		overallHeightMean = (int) lineHeights.getMean();
		int medianHeight = (int) lineHeights.getPercentile(50);

		// remove lines that are too large in height or too small
		ArrayList<Integer> unusualHeightLines = new ArrayList<>();
		for (int i = 0; i < newLines.size(); ++i) {
			// if ((newLines.get(i).height < (overallHeightMean * 0.667))
			// || (newLines.get(i).height > (overallHeightMean * 1.5))) {
			if (debugL <= 2) {
				System.out.println("Line " + i + " height is " + newLines.get(i).height);
			}
			if ((newLines.get(i).height < (medianHeight * 0.667)) || (newLines.get(i).height > (medianHeight * 2.125))
					|| (newLines.get(i).height < (firstLevelHeightMean * 0.5))) {
				// unusualHeightLines.add(i); (Removed. Keep all lines, as the trade off between
				// incremental time for OCR (minor change) vs potential for losing a good line
				// (enormous loss) is lop-sided. Better to keep all lines, to avoid the
				// possibility of losing any information.
				if (debugL <= 2) {
					System.out.println("Added line " + i + " for removal because height is " + newLines.get(i).height
							+ "while height cutoffs are " + (int) (medianHeight * 0.667) + " and "
							+ (int) (medianHeight * 2.125));
				}
			}
		}
		// lop off lines starting from the end of the arraylist
		for (int i = unusualHeightLines.size() - 1; i >= 0; --i) {
			newLines.remove(unusualHeightLines.get(i).intValue());
		}
		return newLines;
	}

	public static Pixa getPixArray1(Pix pix, ArrayList<Rectangle> lines, int debugL) throws Exception {
		final Pixa pixa = Leptonica.INSTANCE.pixaCreate(lines.size());
		final int verticalGap = 10;
		final int border = 20;
		// int pixCount = 1;
		final int pixHeight = Leptonica1.pixGetHeight(pix);
		final int pixWidth = Leptonica1.pixGetWidth(pix);
		final ExecutorService outerThreadService = Executors.newFixedThreadPool(6);
		final ExecutorService innerThreadService = Executors.newFixedThreadPool(12);
		int count = 0;
		for (Rectangle line : lines) {
			final int lineCount = ++count;
			ArrayList<CompletableFuture<Boolean>> innerCFS = new ArrayList<CompletableFuture<Boolean>>();
			final int width = line.width;
			final int height = line.height;
			final int newHeight1 = (int) ((height * (1 + 0.75)) + (verticalGap) + (2 * border));
			final int newHeight2 = (int) ((height * (0.5 + 0.375)) + (verticalGap) + (2 * border));
			final int newWidth1 = width + (2 * border);
			final int newWidth2 = (int) (width * 0.5) + (2 * border);
			Box box = Leptonica1.boxCreate(Math.max(0, line.x - 3), Math.max(0, line.y - 3),
					Math.min(line.width + 6, pixWidth - (line.x - 3)),
					Math.min(line.height + 6, pixHeight - (line.y - 3)));
			Pix pixTemp = Leptonica1.pixClipRectangle(pix, box, null);
			final SBImage sbi1 = SBImage.getSBImageFromPix(pixTemp, debugL);
			LeptUtils.dispose(pixTemp);
			final SBImage[] si = SBImageUtils.bagchiTrinarizationForPK(sbi1, sbi1.width / 4, sbi1.height / 1);
			// final SBImage sbi2 = SBImage.parallelStitchSubImages(si, sbi1.width / 4,
			// sbi1.height / 1);
			final SBImage sbi2 = SBImage.stitchSubImages(si, sbi1.width / 4, sbi1.height / 1);
			final Pix pixSource = SBImage.getPixFromSBImage(sbi2);
			if (debugL <= 4) {
				Leptonica1.pixWrite(SBImageUtils.baseTestDir + "symbolBoxes-" + width + "" + height + ".png", pixSource,
						ILeptonica.IFF_PNG);
			}
			innerCFS.add(CompletableFuture.supplyAsync(() -> {
				if (debugL <= 2) {
					System.out.println("Starting inner thread 1 for line " + lineCount);
				}
				Pix targetPix1 = Leptonica1.pixCreate(newWidth1, newHeight1, 1);
				Leptonica1.pixSetBlackOrWhite(targetPix1, ILeptonica.L_SET_WHITE);
				Pix pixScaledDown = Leptonica1.pixScale(pixSource, 0.75f, 0.75f);
				Leptonica1.pixRasterop(targetPix1, border, border, width, height, ILeptonica.PIX_PAINT, pixSource, 0,
						0);
				Leptonica1.pixRasterop(targetPix1, border, border + height + verticalGap, (int) (width * 0.75),
						(int) (height * 0.75), ILeptonica.PIX_PAINT, pixScaledDown, 0, 0);
				Leptonica1.pixaAddPix(pixa, targetPix1, ILeptonica.L_COPY);

				// Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixs-" + pixCount++ + ".png",
				// targetPix1,
				// ILeptonica.IFF_PNG);
				LeptUtils.dispose(pixScaledDown);
				LeptUtils.dispose(targetPix1);
				if (debugL <= 2) {
					System.out.println("Finishing inner thread 1 for line " + lineCount);
				}
				return Boolean.TRUE;
			}, innerThreadService));

			innerCFS.add(CompletableFuture.supplyAsync(() -> {
				if (debugL <= 2) {
					System.out.println("Starting inner thread 2 for line " + lineCount);
				}
				Pix targetPix2 = Leptonica1.pixCreate(newWidth2, newHeight2, 1);
				Leptonica1.pixSetBlackOrWhite(targetPix2, ILeptonica.L_SET_WHITE);
				// Pix pixReducedDilated = Leptonica1.pixReduceRankBinary2(pixSource, 1, null);
				// Pix pixReducedDilated1 = Leptonica1.pixReduceRankBinary2(pixReducedDilated,
				// 1, null);
				Pix pixDilated = Leptonica1.pixDilateBrick(null, pixSource, 2, 2);
				Pix pixReducedDilated = Leptonica1.pixScale(pixDilated, 0.5f, 0.5f);
				Leptonica1.pixRasterop(targetPix2, border, border, (int) (width * 0.5), (int) (height * 0.5),
						ILeptonica.PIX_PAINT, pixReducedDilated, 0, 0);
				pixDilated = Leptonica1.pixDilateBrick(null, pixDilated, 2, 2);
				pixReducedDilated = Leptonica1.pixScale(pixDilated, 0.375f, 0.375f);
				Leptonica1.pixRasterop(targetPix2, border, border + (int) (height * 0.5) + (verticalGap),
						(int) (width * 0.375), (int) (height * 0.375), ILeptonica.PIX_PAINT, pixReducedDilated, 0, 0);
				// Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixs-" + pixCount++ + ".png",
				// targetPix2,
				// ILeptonica.IFF_PNG);
				Leptonica1.pixaAddPix(pixa, targetPix2, ILeptonica.L_COPY);
				LeptUtils.dispose(pixDilated);
				LeptUtils.dispose(pixReducedDilated);
				LeptUtils.dispose(targetPix2);
				if (debugL <= 2) {
					System.out.println("Finishing inner thread 2 for line " + lineCount);
				}
				return Boolean.TRUE;
			}, innerThreadService));
			CompletableFuture.allOf(innerCFS.toArray(new CompletableFuture[innerCFS.size()])).join();
			LeptUtils.dispose(box);
			LeptUtils.dispose(pixSource);
			if (debugL <= 2) {
				System.out.println("Finishing outer thread for line " + lineCount);
			}

			System.gc();
		}
		outerThreadService.shutdown();
		innerThreadService.shutdown();
		System.gc();
		return pixa;
	}

	public static Pixa getPixArray(Pix pix, ArrayList<Rectangle> lines, ExecutorService outerThreadService,
			ExecutorService innerThreadService, int debugL) throws Exception {
		final Pixa pixa = Leptonica.INSTANCE.pixaCreate(lines.size());
		final int pixHeight = Leptonica1.pixGetHeight(pix);
		final int pixWidth = Leptonica1.pixGetWidth(pix);
		final int verticalGap = 10;
		final int border = 20;
		// int pixCount = 1;
		int count = 0;
		for (final Rectangle line : lines) {
			final int lineCount = ++count;
			ArrayList<CompletableFuture<Boolean>> outerCFS = new ArrayList<CompletableFuture<Boolean>>();
			outerCFS.add(CompletableFuture.supplyAsync(() -> {
				if (debugL <= 2) {
					System.out.println("Started outer thread for line " + lineCount);
				}
				try {
					ArrayList<CompletableFuture<Boolean>> innerCFS = new ArrayList<CompletableFuture<Boolean>>();
					// Box box = Leptonica1.boxCreate(Math.max(0, line.x - 5), line.y, line.width +
					// 5, line.height + 3);
					Box box = Leptonica1.boxCreate(Math.max(0, line.x - 3), Math.max(0, line.y - 3),
							Math.min(line.width + 6, pixWidth - (line.x - 3)),
							Math.min(line.height + 6, pixHeight - (line.y - 3)));
					Pix pixOriginal = Leptonica1.pixClipRectangle(pix, box, null);
					// LeptUtils.dispose(box);
					if (Leptonica1.pixGetDepth(pixOriginal) != 8) {
						pixOriginal = Leptonica1.pixConvertTo8(pixOriginal, 0);
					}
					if (debugL <= 4) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "originalPix -" + lineCount + ".png",
								pixOriginal, ILeptonica.IFF_PNG);
					}

					// Pix contrastNormalisedPix = Leptonica1.pixContrastNorm(null, pixOriginal, 6,
					// 6, 40, 2, 2);
					// Pix backgroundNormalisedPix =
					// Leptonica1.pixBackgroundNormFlex(contrastNormalisedPix, 5, 5, 2, 2,
					// 0);

					Pix bilateralPixFirst = Leptonica1.pixBilateralGray(pixOriginal, 0.8f, 50.0f, 10, 1);
					Pix unsharpMaskedPix = Leptonica1.pixUnsharpMaskingGray(bilateralPixFirst, 1, 0.5f);
					Pix backgroundNormalisedPix = Leptonica1.pixBackgroundNormFlex(unsharpMaskedPix, 5, 5, 2, 2, 0);
					// LeptUtils.dispose(bilateralPixFirst);
					// LeptUtils.dispose(unsharpMaskedPix);
					Pix bilateralPix = Leptonica1.pixBilateralGray(backgroundNormalisedPix, 0.8f, 50.0f, 10, 1);
					// LeptUtils.dispose(backgroundNormalisedPix);

					// Pix contrastNormalisedPix = Leptonica1.pixContrastNorm(null,
					// unsharpMaskedPix, 6, 6, 40, 2, 2);

					// contrastNormalisedPix = Leptonica1.pixAddBorder(contrastNormalisedPix, 3,
					// 255);

					if (debugL <= 4) {
						System.out.println("For line " + lineCount + " : " + "bilateralPix is " + bilateralPix);
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "bilateralPix -" + lineCount + ".png",
								bilateralPix, ILeptonica.IFF_PNG);
					}
					if (debugL <= 4) {
						if (bilateralPix == null) {
							System.out.println("For line " + lineCount + " : " + "bilateralPix is null");
						}
					}

					// if bilateralPix is null, because the dimensions of the pic is too small for
					// normalisation, then use the original pix
					if (bilateralPix == null) {
						bilateralPix = Leptonica1.pixCopy(null, pixOriginal);
					}
					// LeptUtils.dispose(pixOriginal);
					// clean the image, and convert to 1 to enable the other Leptonica processing
					final SBImage sbi1 = SBImage.getSBImageFromPix(bilateralPix, debugL);
					// LeptUtils.dispose(bilateralPix);
					if (debugL <= 4) {
						SBImageUtils.writeFile(sbi1, "png",
								SBImageUtils.baseTestDir + "bilateralSBImage -" + lineCount + ".png");
					}
					// final SBImage[] si = SBImageUtils.bagchiBinarizationForPK1(sbi1, (sbi1.width
					// / 150) + 1,
					// sbi1.height / sbi1.height, lineCount, debugL);
					final SBImage sbi2 = SBImageUtils.bagchiBinarizationForPK2(sbi1, (sbi1.width / 200) + 1, 1,
							lineCount, debugL);
					Pix pixOriginalCleaned = SBImage.getPixFromSBImage(sbi2);
					pixOriginalCleaned = Leptonica1.pixConvertTo1(pixOriginalCleaned, 128);

					/*
					 * int height1 = Leptonica1.pixGetHeight(contrastNormalisedPix); int width1 =
					 * Leptonica1.pixGetWidth(contrastNormalisedPix); int windowSize =
					 * Math.min(height1, width1); PointerByReference poc = new PointerByReference();
					 * Leptonica1.pixSauvolaBinarizeTiled(contrastNormalisedPix, (windowSize - 2) /
					 * 2, 0.4f, 1, 1, null, poc); // pixOriginalCleaned =
					 * Leptonica1.pixConvertTo1(pixOriginalCleaned, 128); Pix pixOriginalCleaned =
					 * new Pix(poc.getValue()); height1 =
					 * Leptonica1.pixGetHeight(pixOriginalCleaned); width1 =
					 * Leptonica1.pixGetWidth(pixOriginalCleaned); if ((height1 == 0) || (width1 ==
					 * 0)) { height1 = Leptonica1.pixGetHeight(contrastNormalisedPix); width1 =
					 * Leptonica1.pixGetWidth(contrastNormalisedPix);
					 * LeptUtils.dispose(pixOriginalCleaned);
					 * System.out.println("Doing Otsu Thresholding for image " + lineCount); //
					 * IntBuffer threshold = IntBuffer.allocate(1);
					 * Leptonica1.pixOtsuAdaptiveThreshold(contrastNormalisedPix, Math.max(width1,
					 * 16), Math.max(height1, 16), 0, 0, 0.1f, null, poc);
					 *
					 * // Leptonica1.pixThresholdByConnComp(contrastNormalisedPix, null, 30, 240, 3,
					 * // 0.0f, 0.0f, // threshold, poc, 0); //
					 * System.out.println("Threshold for image " + lineCount + " is " + //
					 * threshold.get()); pixOriginalCleaned = new Pix(poc.getValue()); }
					 */
					// if (debugL <= 3) {
					if (debugL <= 4) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixOriginalCleaned-" + lineCount + ".png",
								pixOriginalCleaned, ILeptonica.IFF_PNG);
					}

					// rotate pix
					Pix pixRotated = Leptonica1.pixRotate90(pixOriginalCleaned, -1);

					// Add Borders
					int pixDepth = Leptonica1.pixGetDepth(pixOriginalCleaned);
					int targetPixelValue = 0;
					if (pixDepth > 1) {
						targetPixelValue = (int) Math.pow(2, pixDepth) - 1;
					}
					if (debugL <= 1) {
						System.out.println(
								"Target pixel value for border in thread - " + lineCount + " is : " + targetPixelValue);
					}
					int bordersAdded = 10;
					pixRotated = Leptonica1.pixAddBorder(pixRotated, bordersAdded, targetPixelValue);

					// scale pix for deskewing
					int prWidth = Leptonica1.pixGetWidth(pixRotated);
					int prHeight = Leptonica1.pixGetHeight(pixRotated);
					float targetDimensionX = 500f; // width
					float targetDimensionY = 1200f; // height
					float scaleX = targetDimensionX / prWidth;
					float scaleY = targetDimensionY / prHeight;
					float maxScale = Math.max(scaleX, scaleY);
					int sharpWidth = (maxScale < 0.7) ? 1 : 2;
					Pix pixRotatedScaled = Leptonica1.pixScaleGeneral(pixRotated, scaleX, scaleY, 0.0f, sharpWidth);

					// Dilate, then deskew
					int xDilation = 30;
					int yDilation = 2;
					pixRotatedScaled = Leptonica1.pixDilateCompBrick(null, pixRotatedScaled, xDilation, yDilation);
					if (debugL <= 3) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixRotatedScaled-" + lineCount + ".png",
								pixRotatedScaled, ILeptonica.IFF_PNG);
					}
					// PointerByReference skewAngles = new PointerByReference();
					// PointerByReference skewIntercepts = new PointerByReference();
					// Leptonica1.pixGetLocalSkewTransform(pixRotated, 0, 0, 0, 0.0f, 0.0f, 0.0f,
					// skewAngles,
					// skewIntercepts);
					// Pix pixRotatedDeskewed = Leptonica1.pixProjectiveSampledPta(pixRotated,
					// new Pta(skewIntercepts.getPointer().getPointer(0)),
					// new Pta(skewAngles.getPointer().getPointer(0)), ILeptonica.L_BRING_IN_WHITE);
					// Leptonica1.ptaDestroy(skewAngles);
					// Leptonica1.ptaDestroy(skewIntercepts);

					// find angle of shear and then shear vertically around the vertical centre
					FloatBuffer angle = FloatBuffer.allocate(1);
					FloatBuffer confidence = FloatBuffer.allocate(1);
					Leptonica1.pixFindSkew(pixRotatedScaled, angle, confidence);
					float angleForRotation = (angle.get() * (float) Math.PI) / 180;
					if (debugL <= 2) {
						System.out.println("Angle for rotation - " + lineCount + " is : " + angleForRotation);
					}
					int sign = (angleForRotation > 0) ? 1 : -1;
					double tanTheta = (Math.tan(Math.abs(angleForRotation)) * scaleX) / scaleY;
					double rotationAngleForOriginal = Math.atan(tanTheta) * sign;
					// Pix pixRotatedDeskewed = Leptonica1.pixVShear(null, pixRotatedScaled, (int)
					// (targetDimensionX / 2),
					// angleForRotation, ILeptonica.L_BRING_IN_WHITE);
					Pix pixRotatedDeskewed = Leptonica1.pixVShear(null, pixRotated, prWidth / 2,
							(float) rotationAngleForOriginal, ILeptonica.L_BRING_IN_WHITE);

					// rotate the large image back
					// Pix pixDeskewed = Leptonica1.pixRotate90(pixRotatedDeskewed, 1);
					Pix pixDeskewed = Leptonica1.pixRotate90(pixRotatedDeskewed, 1);

					/*
					 * if (debugL <= 3) { Leptonica1.pixWrite( SBImageUtils.baseTestDir +
					 * "pixAfterShearAndRotateBack-" + lineCount + ".png", pixDeskewed,
					 * ILeptonica.IFF_PNG); }
					 */

					// Pix pixRotatedDeskewed = Leptonica1.pixRotate(pixRotated, angleForRotation,
					// ILeptonica.L_ROTATE_SHEAR, ILeptonica.L_BRING_IN_WHITE, 0, 0);

					// Erode the image, as it was dilated earlier
					// Note: yDilation and xDilation switched as pix is rotated back before erosion
					// pixDeskewed = Leptonica1.pixErodeCompBrick(null, pixDeskewed, yDilation,
					// xDilation);

					/*
					 * if (debugL <= 3) { Leptonica1.pixWrite(SBImageUtils.baseTestDir +
					 * "pixDeskewedScaled-" + lineCount + ".png", pixDeskewed, ILeptonica.IFF_PNG);
					 * }
					 */

					if (debugL <= 3) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "originalPixDeskewed-" + lineCount + ".png",
								pixDeskewed, ILeptonica.IFF_PNG);
					}

					// scale it down to original size
					// scaleY and scaleX switched as pix is rotated back before downscaling

					/*
					 * maxScale = Math.max(1 / scaleX, 1 / scaleY); sharpWidth = (maxScale < 0.7) ?
					 * 1 : 2; Pix pixDeskewedDescaled = Leptonica1.pixScaleGeneral(pixDeskewed, 1 /
					 * scaleY, 1 / scaleX, 0.0f, sharpWidth);
					 */

					// final Pix pixSource = Leptonica1.pixRemoveBorder(pixDeskewedDescaled,
					// bordersAdded - 1);
					final Pix pixSource = Leptonica1.pixRemoveBorder(pixDeskewed, bordersAdded - 1);
					if (debugL <= 3) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "finalDeskewedPix-" + lineCount + ".png",
								pixSource, ILeptonica.IFF_PNG);
					}

					final int width = Leptonica1.pixGetWidth(pixSource);
					final int height = Leptonica1.pixGetHeight(pixSource);
					final int newHeight1 = (int) ((height * (1 + 1.5)) + (2 * verticalGap) + (2 * border));
					final int newWidth1 = width + (2 * border);
					final int newHeight2 = (int) ((height * (0.5 + 0.375)) + (verticalGap) + (2 * border));
					final int newWidth2 = (int) (width * 0.5) + (2 * border);

					innerCFS.add(CompletableFuture.supplyAsync(() -> {
						if (debugL <= 2) {
							System.out.println("Starting inner thread 1 for line " + lineCount);
						}
						Pix targetPix1 = Leptonica1.pixCreate(newWidth1, newHeight1, 1);
						Leptonica1.pixSetBlackOrWhite(targetPix1, ILeptonica.L_SET_WHITE);
						Pix pixScaledDown = Leptonica1.pixScale(pixSource, 0.75f, 0.75f);
						pixScaledDown = Leptonica1.pixDilateBrick(null, pixScaledDown, 1, 2);
						Pix pixScaledDown1 = Leptonica1.pixScale(pixScaledDown, 0.5f, 1.0f);
						Leptonica1.pixRasterop(targetPix1, border, border, width, height, ILeptonica.PIX_PAINT,
								pixSource, 0, 0);
						Leptonica1.pixRasterop(targetPix1, border, border + height + verticalGap, (int) (width * 0.75),
								(int) (height * 0.75), ILeptonica.PIX_PAINT, pixScaledDown, 0, 0);
						Leptonica1.pixRasterop(targetPix1, border,
								(int) (border + (1.75 * height) + (1.75 * verticalGap)), (int) (width * 0.75 * 0.5),
								(int) (height * 0.75), ILeptonica.PIX_PAINT, pixScaledDown1, 0, 0);
						Leptonica1.pixaAddPix(pixa, targetPix1, ILeptonica.L_COPY);

						// Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixs-" + pixCount++ + ".png",
						// targetPix1,
						// ILeptonica.IFF_PNG);
						LeptUtils.dispose(pixScaledDown);
						LeptUtils.dispose(pixScaledDown1);
						LeptUtils.dispose(targetPix1);
						if (debugL <= 2) {
							System.out.println("Finishing inner thread 1 for line " + lineCount);
						}
						return Boolean.TRUE;
					}, innerThreadService));

					innerCFS.add(CompletableFuture.supplyAsync(() -> {
						if (debugL <= 2) {
							System.out.println("Starting inner thread 2 for line " + lineCount);
						}
						Pix targetPix2 = Leptonica1.pixCreate(newWidth2, newHeight2, 1);
						Leptonica1.pixSetBlackOrWhite(targetPix2, ILeptonica.L_SET_WHITE);
						// Pix pixReducedDilated = Leptonica1.pixReduceRankBinary2(pixSource, 1, null);
						// Pix pixReducedDilated1 = Leptonica1.pixReduceRankBinary2(pixReducedDilated,
						// 1, null);
						Pix pixDilated = Leptonica1.pixDilateBrick(null, pixSource, 3, 4);
						Pix pixReducedDilated = Leptonica1.pixScale(pixDilated, 0.5f, 0.5f);
						Leptonica1.pixRasterop(targetPix2, border, border, (int) (width * 0.5), (int) (height * 0.5),
								ILeptonica.PIX_PAINT, pixReducedDilated, 0, 0);
						pixDilated = Leptonica1.pixDilateBrick(null, pixDilated, 1, 2);
						pixReducedDilated = Leptonica1.pixScale(pixDilated, 0.375f, 0.375f);
						Leptonica1.pixRasterop(targetPix2, border, border + (int) (height * 0.5) + (verticalGap),
								(int) (width * 0.375), (int) (height * 0.375), ILeptonica.PIX_PAINT, pixReducedDilated,
								0, 0);
						// Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixs-" + pixCount++ + ".png",
						// targetPix2,
						// ILeptonica.IFF_PNG);
						Leptonica1.pixaAddPix(pixa, targetPix2, ILeptonica.L_COPY);
						LeptUtils.dispose(pixDilated);
						LeptUtils.dispose(pixReducedDilated);
						LeptUtils.dispose(targetPix2);
						if (debugL <= 2) {
							System.out.println("Finishing inner thread 2 for line " + lineCount);
						}
						return Boolean.TRUE;
					}, innerThreadService));
					CompletableFuture.allOf(innerCFS.toArray(new CompletableFuture[innerCFS.size()])).join();
					LeptUtils.dispose(box);
					LeptUtils.dispose(pixOriginal);
					// LeptUtils.dispose(contrastNormalisedPix);
					LeptUtils.dispose(bilateralPixFirst);
					LeptUtils.dispose(unsharpMaskedPix);
					LeptUtils.dispose(backgroundNormalisedPix);
					LeptUtils.dispose(bilateralPix);
					LeptUtils.dispose(pixOriginalCleaned);
					LeptUtils.dispose(pixRotated);
					LeptUtils.dispose(pixRotatedScaled);
					LeptUtils.dispose(pixRotatedDeskewed);
					LeptUtils.dispose(pixDeskewed);
					// LeptUtils.dispose(pixDeskewedDescaled);
					LeptUtils.dispose(pixSource);
					if (debugL <= 2) {
						System.out.println("Finishing outer thread for line " + lineCount);
					}
					return Boolean.TRUE;
				} catch (Exception e) {
					e.printStackTrace();
					return Boolean.FALSE;
				}
			}, outerThreadService));
			CompletableFuture.allOf(outerCFS.toArray(new CompletableFuture[outerCFS.size()])).join();
			if (debugL <= 2) {
				System.out.println("Finishing all threads for line " + lineCount);
			}
			System.gc();
		}
		System.gc();
		return pixa;
	}

	public static Pixa getPixArray2(Pix pix, ArrayList<Rectangle> lines, ExecutorService outerThreadService,
			ExecutorService innerThreadService, int debugL) throws Exception {
		final Pixa pixa = Leptonica.INSTANCE.pixaCreate(lines.size());
		final int pixHeight = Leptonica1.pixGetHeight(pix);
		final int pixWidth = Leptonica1.pixGetWidth(pix);
		final int verticalGap = 10;
		final int border = 20;
		// int pixCount = 1;
		int count = 0;
		final Object pixArrayMonitor = new Object();
		for (final Rectangle line : lines) {
			final int lineCount = ++count;
			ArrayList<CompletableFuture<Boolean>> outerCFS = new ArrayList<CompletableFuture<Boolean>>();
			outerCFS.add(CompletableFuture.supplyAsync(() -> {
				if (debugL <= 2) {
					System.out.println("Started outer thread for line " + lineCount);
				}
				try {
					Box box = null;
					Pix pixOriginal = null;
					Pix bilateralPixFirst = null;
					Pix unsharpMaskedPix = null;
					Pix backgroundNormalisedPix = null;
					Pix bilateralPix = null;
					Pix pixOriginalCleaned = null;
					Pix pixRotated = null;
					Pix pixRotatedScaled = null;
					Pix pixRotatedDeskewed = null;
					Pix pixOriginalDeskewed = null;

					ArrayList<CompletableFuture<Boolean>> innerCFS = new ArrayList<CompletableFuture<Boolean>>();
					box = Leptonica1.boxCreate(Math.max(0, line.x - 3), Math.max(0, line.y - 3),
							Math.min(line.width + 6, pixWidth - 1 - (line.x - 3)),
							Math.min(line.height + 6, pixHeight - 1 - (line.y - 3)));
					Pix pixOriginal1 = Leptonica1.pixClipRectangle(pix, box, null);
					pixOriginal = Leptonica1.pixConvertTo8(pixOriginal1, 0);
					LeptUtils.dispose(pixOriginal1);

					if (debugL <= 4) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "originalPix -" + lineCount + ".png",
								pixOriginal, ILeptonica.IFF_PNG);
					}

					bilateralPixFirst = Leptonica1.pixBilateralGray(pixOriginal, 0.8f, 50.0f, 10, 1);
					unsharpMaskedPix = Leptonica1.pixUnsharpMaskingGray(bilateralPixFirst, 1, 0.5f);
					backgroundNormalisedPix = Leptonica1.pixBackgroundNormFlex(unsharpMaskedPix, 5, 5, 2, 2, 0);
					bilateralPix = Leptonica1.pixBilateralGray(backgroundNormalisedPix, 0.8f, 25.0f, 5, 1);

					if (debugL <= 4) {
						System.out.println("For line " + lineCount + " : " + "bilateralPix is " + bilateralPix);
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "bilateralPix -" + lineCount + ".png",
								bilateralPix, ILeptonica.IFF_PNG);
					}

					// if bilateralPix is null, because the dimensions of the pix is too small for
					// normalisation, then use the original pix
					if (bilateralPix == null) {
						bilateralPix = Leptonica1.pixCopy(null, pixOriginal);
					}

					// clean the image, and convert to 1 to enable the other Leptonica processing
					final SBImage sbi1 = SBImage.getSBImageFromPix(bilateralPix, debugL);
					if (debugL <= 4) {
						SBImageUtils.writeFile(sbi1, "png",
								SBImageUtils.baseTestDir + "bilateralSBImage -" + lineCount + ".png");
					}
					// final SBImage sbi2 = SBImageUtils.bagchiBinarizationForPK1(sbi1,
					// Math.min(4, (sbi1.width / 350) + 1), 1, lineCount, debugL);
					final SBImage sbi2 = SBImageUtils.bagchiBinarizationForPK2(sbi1,
							Math.min(4, (sbi1.width / 150) + 1), 1, lineCount, debugL);
					if (debugL <= 3) {
						System.out.println("About to get Pix from SBImage for image - " + lineCount);
					}
					Pix pixOriginalCleaned1 = SBImage.getPixFromSBImage(sbi2);
					pixOriginalCleaned = Leptonica1.pixConvertTo1(pixOriginalCleaned1, 128);
					LeptUtils.dispose(pixOriginalCleaned1);

					if (debugL <= 4) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixOriginalCleaned-" + lineCount + ".png",
								pixOriginalCleaned, ILeptonica.IFF_PNG);
					}

					// rotate pix
					Pix pixRotated1 = Leptonica1.pixRotate90(pixOriginalCleaned, -1);

					// Add Borders
					int pixDepth = Leptonica1.pixGetDepth(pixOriginalCleaned);
					int targetPixelValue = 0;
					if (pixDepth > 1) {
						targetPixelValue = (int) Math.pow(2, pixDepth) - 1;
					}

					if (debugL <= 1) {
						System.out.println(
								"Target pixel value for border in thread - " + lineCount + " is : " + targetPixelValue);
					}

					int bordersAdded = 10;
					pixRotated = Leptonica1.pixAddBorder(pixRotated1, bordersAdded, targetPixelValue);
					LeptUtils.dispose(pixRotated1);

					// scale pix for deskewing
					int prWidth = Leptonica1.pixGetWidth(pixRotated);
					int prHeight = Leptonica1.pixGetHeight(pixRotated);
					float targetDimensionX = 500f; // width
					float targetDimensionY = 1200f; // height
					float scaleX = targetDimensionX / prWidth;
					float scaleY = targetDimensionY / prHeight;
					float maxScale = Math.max(scaleX, scaleY);
					int sharpWidth = (maxScale < 0.7) ? 1 : 2;
					Pix pixRotatedScaled1 = Leptonica1.pixScaleGeneral(pixRotated, scaleX, scaleY, 0.0f, sharpWidth);

					// Dilate, then deskew
					int xDilation = 30;
					int yDilation = 2;
					pixRotatedScaled = Leptonica1.pixDilateCompBrick(null, pixRotatedScaled1, xDilation, yDilation);
					LeptUtils.dispose(pixRotatedScaled1);
					if (debugL <= 3) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixRotatedScaled-" + lineCount + ".png",
								pixRotatedScaled, ILeptonica.IFF_PNG);
					}

					// find deskew angle for scaled image. From that, determine the deskew angle for
					// the original image and, do the rotation
					FloatBuffer angle = FloatBuffer.allocate(1);
					FloatBuffer confidence = FloatBuffer.allocate(1);
					Leptonica1.pixFindSkew(pixRotatedScaled, angle, confidence);
					float angleForRotation = (angle.get() * (float) Math.PI) / 180; // in radians
					if (debugL <= 2) {
						System.out.println("Angle for rotation - " + lineCount + " is : " + angleForRotation);
					}
					int sign = (angleForRotation > 0) ? 1 : -1;
					double tanTheta = Math.tan(Math.abs(angleForRotation)) * (scaleX / scaleY);
					double rotationAngleForOriginal = Math.atan(tanTheta) * sign;
					pixRotatedDeskewed = Leptonica1.pixVShear(null, pixRotated, prWidth / 2,
							(float) rotationAngleForOriginal, ILeptonica.L_BRING_IN_WHITE);

					// rotate the deskewed image back
					pixOriginalDeskewed = Leptonica1.pixRotate90(pixRotatedDeskewed, 1);
					if (debugL <= 3) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixOriginalDeskewed-" + lineCount + ".png",
								pixOriginalDeskewed, ILeptonica.IFF_PNG);
					}

					final Pix pixSource = Leptonica1.pixRemoveBorder(pixOriginalDeskewed, bordersAdded - 1);
					Pix pixSource1 = Leptonica1.pixCopy(null, pixSource);
					if (debugL <= 3) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "finalDeskewedPix-" + lineCount + ".png",
								pixSource, ILeptonica.IFF_PNG);
					}

					LeptUtils.dispose(box);
					LeptUtils.dispose(pixOriginal);
					LeptUtils.dispose(bilateralPixFirst);
					LeptUtils.dispose(unsharpMaskedPix);
					LeptUtils.dispose(backgroundNormalisedPix);
					LeptUtils.dispose(bilateralPix);
					LeptUtils.dispose(pixOriginalCleaned);
					LeptUtils.dispose(pixRotated);
					LeptUtils.dispose(pixRotatedScaled);
					LeptUtils.dispose(pixRotatedDeskewed);
					LeptUtils.dispose(pixOriginalDeskewed);
					// System.gc();

					final int width = Leptonica1.pixGetWidth(pixSource);
					final int height = Leptonica1.pixGetHeight(pixSource);
					final int newHeight1 = (int) ((height * (1 + 1.5)) + (2 * verticalGap) + (2 * border));
					final int newWidth1 = width + (2 * border);
					final int newHeight2 = (int) ((height * (0.5 + 0.375)) + (verticalGap) + (2 * border));
					final int newWidth2 = (int) (width * 0.5) + (2 * border);

					innerCFS.add(CompletableFuture.supplyAsync(() -> {
						if (debugL <= 2) {
							System.out.println("Starting inner thread 1 for line " + lineCount);
						}
						Pix targetPix1 = null;
						Pix pixScaledDown = null;
						Pix pixScaledDown1 = null;
						Pix pixScaledDown2 = null;
						targetPix1 = Leptonica1.pixCreate(newWidth1, newHeight1, 1);
						Leptonica1.pixSetBlackOrWhite(targetPix1, ILeptonica.L_SET_WHITE);
						pixScaledDown = Leptonica1.pixScale(pixSource1, 0.75f, 0.75f);
						pixScaledDown1 = Leptonica1.pixDilateBrick(null, pixScaledDown, 1, 2);
						pixScaledDown2 = Leptonica1.pixScale(pixScaledDown1, 0.5f, 1.0f);
						Leptonica1.pixRasterop(targetPix1, border, border, width, height, ILeptonica.PIX_PAINT,
								pixSource, 0, 0);
						Leptonica1.pixRasterop(targetPix1, border, border + height + verticalGap, (int) (width * 0.75),
								(int) (height * 0.75), ILeptonica.PIX_PAINT, pixScaledDown1, 0, 0);
						Leptonica1.pixRasterop(targetPix1, border,
								(int) (border + (1.75 * height) + (1.75 * verticalGap)), (int) (width * 0.75 * 0.5),
								(int) (height * 0.75), ILeptonica.PIX_PAINT, pixScaledDown2, 0, 0);
						// Leptonica1.pixaAddPix(pixa, targetPix1, ILeptonica.L_CLONE);
						synchronized (pixArrayMonitor) {
							Leptonica1.pixaAddPix(pixa, targetPix1, ILeptonica.L_COPY);
						}

						LeptUtils.dispose(pixSource1);
						LeptUtils.dispose(pixScaledDown);
						LeptUtils.dispose(pixScaledDown1);
						LeptUtils.dispose(pixScaledDown2);
						LeptUtils.dispose(targetPix1);
						if (debugL <= 2) {
							System.out.println("Finishing inner thread 1 for line " + lineCount);
						}
						return Boolean.TRUE;
					}, innerThreadService));

					innerCFS.add(CompletableFuture.supplyAsync(() -> {
						if (debugL <= 2) {
							System.out.println("Starting inner thread 2 for line " + lineCount);
						}
						Pix targetPix2 = null;
						Pix pixDilated1 = null;
						Pix pixReducedDilated1 = null;
						Pix pixDilated2 = null;
						Pix pixReducedDilated2 = null;

						targetPix2 = Leptonica1.pixCreate(newWidth2, newHeight2, 1);
						Leptonica1.pixSetBlackOrWhite(targetPix2, ILeptonica.L_SET_WHITE);
						pixDilated1 = Leptonica1.pixDilateBrick(null, pixSource, 3, 4);
						pixReducedDilated1 = Leptonica1.pixScale(pixDilated1, 0.5f, 0.5f);
						Leptonica1.pixRasterop(targetPix2, border, border, (int) (width * 0.5), (int) (height * 0.5),
								ILeptonica.PIX_PAINT, pixReducedDilated1, 0, 0);
						pixDilated2 = Leptonica1.pixDilateBrick(null, pixDilated1, 1, 2);
						pixReducedDilated2 = Leptonica1.pixScale(pixDilated2, 0.375f, 0.375f);
						Leptonica1.pixRasterop(targetPix2, border, border + (int) (height * 0.5) + (verticalGap),
								(int) (width * 0.375), (int) (height * 0.375), ILeptonica.PIX_PAINT, pixReducedDilated2,
								0, 0);
						synchronized (pixArrayMonitor) {
							Leptonica1.pixaAddPix(pixa, targetPix2, ILeptonica.L_COPY);
						}
						// Leptonica1.pixaAddPix(pixa, targetPix2, ILeptonica.L_CLONE);
						LeptUtils.dispose(pixDilated1);
						LeptUtils.dispose(pixReducedDilated1);
						LeptUtils.dispose(pixDilated2);
						LeptUtils.dispose(pixReducedDilated2);
						LeptUtils.dispose(targetPix2);
						if (debugL <= 2) {
							System.out.println("Finishing inner thread 2 for line " + lineCount);
						}
						return Boolean.TRUE;
					}, innerThreadService));
					CompletableFuture.allOf(innerCFS.toArray(new CompletableFuture[innerCFS.size()])).join();
					LeptUtils.dispose(pixSource);
					if (debugL <= 2) {
						System.out.println("Finishing outer thread for line " + lineCount);
					}
					return Boolean.TRUE;
				} catch (Exception e) {
					e.printStackTrace();
					return Boolean.FALSE;
				}
			}, outerThreadService));
			CompletableFuture.allOf(outerCFS.toArray(new CompletableFuture[outerCFS.size()])).join();
			if (debugL <= 2) {
				System.out.println("Finishing all threads for line " + lineCount);
			}
			System.gc();
		}
		System.gc();
		return pixa;
	}

	public static Pixa getPixArray3(Pix pix, ArrayList<Rectangle> lines, ExecutorService outerThreadService,
			ExecutorService innerThreadService, int debugL) throws Exception {
		final Pixa pixa = Leptonica.INSTANCE.pixaCreate(lines.size());
		final int pixHeight = Leptonica1.pixGetHeight(pix);
		final int pixWidth = Leptonica1.pixGetWidth(pix);
		final int verticalGap = 10;
		final int border = 20;
		// int pixCount = 1;
		int count = 0;
		final Object pixArrayMonitor = new Object();
		for (final Rectangle line : lines) {
			final int lineCount = ++count;
			ArrayList<CompletableFuture<Boolean>> outerCFS = new ArrayList<CompletableFuture<Boolean>>();
			outerCFS.add(CompletableFuture.supplyAsync(() -> {
				if (debugL <= 2) {
					System.out.println("Started outer thread for line " + lineCount);
				}
				try {
					Box box = null;
					Pix pixOriginal = null;
					Pix bilateralPixFirst = null;
					Pix unsharpMaskedPix = null;
					Pix backgroundNormalisedPix = null;
					Pix bilateralPix = null;
					Pix pixOriginalCleaned = null;
					Pix pixRotated = null;
					Pix pixRotatedScaled = null;
					Pix pixRotatedDeskewed = null;
					Pix pixOriginalDeskewed = null;

					// ArrayList<CompletableFuture<Boolean>> innerCFS = new
					// ArrayList<CompletableFuture<Boolean>>();
					box = Leptonica1.boxCreate(Math.max(0, line.x - 3), Math.max(0, line.y - 3),
							Math.min(line.width + 6, pixWidth - 1 - (line.x - 3)),
							Math.min(line.height + 6, pixHeight - 1 - (line.y - 3)));
					Pix pixOriginal1 = Leptonica1.pixClipRectangle(pix, box, null);
					pixOriginal = Leptonica1.pixConvertTo8(pixOriginal1, 0);
					LeptUtils.dispose(pixOriginal1);

					if (debugL <= 4) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "originalPix -" + lineCount + ".png",
								pixOriginal, ILeptonica.IFF_PNG);
					}

					bilateralPixFirst = Leptonica1.pixBilateralGray(pixOriginal, 0.8f, 50.0f, 10, 1);
					unsharpMaskedPix = Leptonica1.pixUnsharpMaskingGray(bilateralPixFirst, 1, 0.5f);
					backgroundNormalisedPix = Leptonica1.pixBackgroundNormFlex(unsharpMaskedPix, 5, 5, 2, 2, 0);
					bilateralPix = Leptonica1.pixBilateralGray(backgroundNormalisedPix, 0.8f, 25.0f, 5, 1);

					if (debugL <= 4) {
						System.out.println("For line " + lineCount + " : " + "bilateralPix is " + bilateralPix);
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "bilateralPix -" + lineCount + ".png",
								bilateralPix, ILeptonica.IFF_PNG);
					}

					// if bilateralPix is null, because the dimensions of the pix is too small for
					// normalisation, then use the original pix
					if (bilateralPix == null) {
						bilateralPix = Leptonica1.pixCopy(null, pixOriginal);
					}

					// clean the image, and convert to 1 to enable the other Leptonica processing
					final SBImage sbi1 = SBImage.getSBImageFromPix(bilateralPix, debugL);
					if (debugL <= 4) {
						SBImageUtils.writeFile(sbi1, "png",
								SBImageUtils.baseTestDir + "bilateralSBImage -" + lineCount + ".png");
					}
					// final SBImage sbi2 = SBImageUtils.bagchiBinarizationForPK1(sbi1,
					// Math.min(4, (sbi1.width / 350) + 1), 1, lineCount, debugL);
					final SBImage sbi2 = SBImageUtils.bagchiBinarizationForPK2(sbi1,
							Math.min(4, (sbi1.width / 100) + 1), 1, lineCount, debugL);
					if (debugL <= 3) {
						System.out.println("About to get Pix from SBImage for image - " + lineCount);
					}
					Pix pixOriginalCleaned1 = SBImage.getPixFromSBImage(sbi2);
					pixOriginalCleaned = Leptonica1.pixConvertTo1(pixOriginalCleaned1, 128);
					LeptUtils.dispose(pixOriginalCleaned1);

					if (debugL <= 4) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixOriginalCleaned-" + lineCount + ".png",
								pixOriginalCleaned, ILeptonica.IFF_PNG);
					}

					// rotate pix
					Pix pixRotated1 = Leptonica1.pixRotate90(pixOriginalCleaned, -1);

					// Add Borders
					int pixDepth = Leptonica1.pixGetDepth(pixOriginalCleaned);
					int targetPixelValue = 0;
					if (pixDepth > 1) {
						targetPixelValue = (int) Math.pow(2, pixDepth) - 1;
					}

					if (debugL <= 1) {
						System.out.println(
								"Target pixel value for border in thread - " + lineCount + " is : " + targetPixelValue);
					}

					int bordersAdded = 10;
					pixRotated = Leptonica1.pixAddBorder(pixRotated1, bordersAdded, targetPixelValue);
					LeptUtils.dispose(pixRotated1);

					// scale pix for deskewing
					int prWidth = Leptonica1.pixGetWidth(pixRotated);
					int prHeight = Leptonica1.pixGetHeight(pixRotated);
					float targetDimensionX = 500f; // width
					float targetDimensionY = 1200f; // height
					float scaleX = targetDimensionX / prWidth;
					float scaleY = targetDimensionY / prHeight;
					float maxScale = Math.max(scaleX, scaleY);
					int sharpWidth = (maxScale < 0.7) ? 1 : 2;
					Pix pixRotatedScaled1 = Leptonica1.pixScaleGeneral(pixRotated, scaleX, scaleY, 0.0f, sharpWidth);

					// Dilate, then deskew
					int xDilation = 30;
					int yDilation = 2;
					pixRotatedScaled = Leptonica1.pixDilateCompBrick(null, pixRotatedScaled1, xDilation, yDilation);
					LeptUtils.dispose(pixRotatedScaled1);
					if (debugL <= 3) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixRotatedScaled-" + lineCount + ".png",
								pixRotatedScaled, ILeptonica.IFF_PNG);
					}

					// find deskew angle for scaled image. From that, determine the deskew angle for
					// the original image and, do the rotation
					FloatBuffer angle = FloatBuffer.allocate(1);
					FloatBuffer confidence = FloatBuffer.allocate(1);
					Leptonica1.pixFindSkew(pixRotatedScaled, angle, confidence);
					float angleForRotation = (angle.get() * (float) Math.PI) / 180; // in radians
					if (debugL <= 2) {
						System.out.println("Angle for rotation - " + lineCount + " is : " + angleForRotation);
					}
					int sign = (angleForRotation > 0) ? 1 : -1;
					double tanTheta = Math.tan(Math.abs(angleForRotation)) * (scaleX / scaleY);
					double rotationAngleForOriginal = Math.atan(tanTheta) * sign;
					pixRotatedDeskewed = Leptonica1.pixVShear(null, pixRotated, prWidth / 2,
							(float) rotationAngleForOriginal, ILeptonica.L_BRING_IN_WHITE);

					// rotate the deskewed image back
					pixOriginalDeskewed = Leptonica1.pixRotate90(pixRotatedDeskewed, 1);
					if (debugL <= 3) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixOriginalDeskewed-" + lineCount + ".png",
								pixOriginalDeskewed, ILeptonica.IFF_PNG);
					}

					final Pix pixSource = Leptonica1.pixRemoveBorder(pixOriginalDeskewed, bordersAdded - 1);
					if (debugL <= 3) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "finalDeskewedPix-" + lineCount + ".png",
								pixSource, ILeptonica.IFF_PNG);
					}

					LeptUtils.dispose(box);
					LeptUtils.dispose(pixOriginal);
					LeptUtils.dispose(bilateralPixFirst);
					LeptUtils.dispose(unsharpMaskedPix);
					LeptUtils.dispose(backgroundNormalisedPix);
					LeptUtils.dispose(bilateralPix);
					LeptUtils.dispose(pixOriginalCleaned);
					LeptUtils.dispose(pixRotated);
					LeptUtils.dispose(pixRotatedScaled);
					LeptUtils.dispose(pixRotatedDeskewed);
					LeptUtils.dispose(pixOriginalDeskewed);
					// System.gc();

					final int width = Leptonica1.pixGetWidth(pixSource);
					final int height = Leptonica1.pixGetHeight(pixSource);
					final int newHeight1 = (int) ((height * (1 + 1.5)) + (2 * verticalGap) + (2 * border));
					final int newWidth1 = width + (2 * border);
					final int newHeight2 = (int) ((height * (0.5 + 0.375)) + (verticalGap) + (2 * border));
					final int newWidth2 = (int) (width * 0.5) + (2 * border);

					Pix targetPix1 = null;
					Pix pixScaledDown = null;
					Pix pixScaledDown1 = null;
					Pix pixScaledDown2 = null;
					targetPix1 = Leptonica1.pixCreate(newWidth1, newHeight1, 1);
					Leptonica1.pixSetBlackOrWhite(targetPix1, ILeptonica.L_SET_WHITE);
					pixScaledDown = Leptonica1.pixScale(pixSource, 0.75f, 0.75f);
					pixScaledDown1 = Leptonica1.pixDilateBrick(null, pixScaledDown, 1, 2);
					pixScaledDown2 = Leptonica1.pixScale(pixScaledDown1, 0.5f, 1.0f);
					Leptonica1.pixRasterop(targetPix1, border, border, width, height, ILeptonica.PIX_PAINT, pixSource,
							0, 0);
					Leptonica1.pixRasterop(targetPix1, border, border + height + verticalGap, (int) (width * 0.75),
							(int) (height * 0.75), ILeptonica.PIX_PAINT, pixScaledDown1, 0, 0);
					Leptonica1.pixRasterop(targetPix1, border, (int) (border + (1.75 * height) + (1.75 * verticalGap)),
							(int) (width * 0.75 * 0.5), (int) (height * 0.75), ILeptonica.PIX_PAINT, pixScaledDown2, 0,
							0);
					// Leptonica1.pixaAddPix(pixa, targetPix1, ILeptonica.L_CLONE);
					synchronized (pixArrayMonitor) {
						Leptonica1.pixaAddPix(pixa, targetPix1, ILeptonica.L_COPY);
					}

					LeptUtils.dispose(pixScaledDown);
					LeptUtils.dispose(pixScaledDown1);
					LeptUtils.dispose(pixScaledDown2);
					LeptUtils.dispose(targetPix1);

					Pix targetPix2 = null;
					Pix pixDilated1 = null;
					Pix pixReducedDilated1 = null;
					Pix pixDilated2 = null;
					Pix pixReducedDilated2 = null;
					targetPix2 = Leptonica1.pixCreate(newWidth2, newHeight2, 1);
					Leptonica1.pixSetBlackOrWhite(targetPix2, ILeptonica.L_SET_WHITE);
					pixDilated1 = Leptonica1.pixDilateBrick(null, pixSource, 3, 4);
					pixReducedDilated1 = Leptonica1.pixScale(pixDilated1, 0.5f, 0.5f);
					Leptonica1.pixRasterop(targetPix2, border, border, (int) (width * 0.5), (int) (height * 0.5),
							ILeptonica.PIX_PAINT, pixReducedDilated1, 0, 0);
					pixDilated2 = Leptonica1.pixDilateBrick(null, pixDilated1, 1, 2);
					pixReducedDilated2 = Leptonica1.pixScale(pixDilated2, 0.375f, 0.375f);
					Leptonica1.pixRasterop(targetPix2, border, border + (int) (height * 0.5) + (verticalGap),
							(int) (width * 0.375), (int) (height * 0.375), ILeptonica.PIX_PAINT, pixReducedDilated2, 0,
							0);
					synchronized (pixArrayMonitor) {
						Leptonica1.pixaAddPix(pixa, targetPix2, ILeptonica.L_COPY);
					}
					// Leptonica1.pixaAddPix(pixa, targetPix2, ILeptonica.L_CLONE);
					LeptUtils.dispose(pixDilated1);
					LeptUtils.dispose(pixReducedDilated1);
					LeptUtils.dispose(pixDilated2);
					LeptUtils.dispose(pixReducedDilated2);
					LeptUtils.dispose(targetPix2);
					LeptUtils.dispose(pixSource);
					if (debugL <= 2) {
						System.out.println("Finishing outer thread for line " + lineCount);
					}
					return Boolean.TRUE;
				} catch (Exception e) {
					e.printStackTrace();
					return Boolean.FALSE;
				}
			}, outerThreadService));
			CompletableFuture.allOf(outerCFS.toArray(new CompletableFuture[outerCFS.size()])).join();
			if (debugL <= 2) {
				System.out.println("Finishing all threads for line " + lineCount);
			}
			System.gc();
		}
		System.gc();
		return pixa;
	}

	public static Pixa getPixArray4(Pix pix, ArrayList<Rectangle> lines, ExecutorService outerThreadService,
			ExecutorService innerThreadService, int debugL) throws Exception {
		final Pixa pixa = Leptonica.INSTANCE.pixaCreate(lines.size());
		final int pixHeight = Leptonica1.pixGetHeight(pix);
		final int pixWidth = Leptonica1.pixGetWidth(pix);
		final int verticalGap = 10;
		final int border = 20;
		// int pixCount = 1;
		int count = 0;
		final Object pixArrayMonitor = new Object();
		for (final Rectangle line : lines) {
			final int lineCount = ++count;
			ArrayList<CompletableFuture<Boolean>> outerCFS = new ArrayList<CompletableFuture<Boolean>>();
			outerCFS.add(CompletableFuture.supplyAsync(() -> {
				if (debugL <= 2) {
					System.out.println("Started outer thread for line " + lineCount);
				}
				try {
					Box box = null;
					Pix pixOriginal = null;
					Pix bilateralPixFirst = null;
					Pix unsharpMaskedPix = null;
					Pix backgroundNormalisedPix = null;
					Pix bilateralPix = null;
					Pix pixOriginalCleaned = null;
					Pix pixRotated = null;
					Pix pixRotatedScaled = null;
					Pix pixRotatedDeskewed = null;
					Pix pixOriginalDeskewed = null;

					int borderAdjustment = 3;
					// ArrayList<CompletableFuture<Boolean>> innerCFS = new
					// ArrayList<CompletableFuture<Boolean>>();
					box = Leptonica1.boxCreate(Math.max(0, line.x - borderAdjustment),
							Math.max(0, line.y - borderAdjustment),
							Math.min(line.width + (2 * borderAdjustment), pixWidth - 1 - (line.x - borderAdjustment)),
							Math.min(line.height + (2 * borderAdjustment),
									pixHeight - 1 - (line.y - borderAdjustment)));
					Pix pixOriginal1 = Leptonica1.pixClipRectangle(pix, box, null);
					pixOriginal = Leptonica1.pixConvertTo8(pixOriginal1, 0);
					LeptUtils.dispose(pixOriginal1);

					if (debugL <= 4) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "originalPix -" + lineCount + ".png",
								pixOriginal, ILeptonica.IFF_PNG);
					}

					bilateralPixFirst = Leptonica1.pixBilateralGray(pixOriginal, 0.8f, 50.0f, 10, 1);
					unsharpMaskedPix = Leptonica1.pixUnsharpMaskingGray(bilateralPixFirst, 1, 0.5f);
					backgroundNormalisedPix = Leptonica1.pixBackgroundNormFlex(unsharpMaskedPix, 5, 5, 2, 2, 0);
					bilateralPix = Leptonica1.pixBilateralGray(backgroundNormalisedPix, 0.8f, 25.0f, 5, 1);

					if (debugL <= 4) {
						System.out.println("For line " + lineCount + " : " + "bilateralPix is " + bilateralPix);
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "bilateralPix -" + lineCount + ".png",
								bilateralPix, ILeptonica.IFF_PNG);
					}

					// if bilateralPix is null, because the dimensions of the pix is too small for
					// normalisation, then use the original pix
					if (bilateralPix == null) {
						bilateralPix = Leptonica1.pixCopy(null, pixOriginal);
					}

					// clean the image, and convert to 1 to enable the other Leptonica processing
					final SBImage sbi1 = SBImage.getSBImageFromPix(bilateralPix, debugL);
					if (debugL <= 4) {
						SBImageUtils.writeFile(sbi1, "png",
								SBImageUtils.baseTestDir + "bilateralSBImage -" + lineCount + ".png");
					}
					// final SBImage sbi2 = SBImageUtils.bagchiBinarizationForPK1(sbi1,
					// Math.min(4, (sbi1.width / 350) + 1), 1, lineCount, debugL);
					final SBImage sbi2 = SBImageUtils.bagchiBinarizationForPK2(sbi1,
							Math.min(4, (sbi1.width / 100) + 1), 1, lineCount, debugL);
					if (debugL <= 3) {
						System.out.println("About to get Pix from SBImage for image - " + lineCount);
					}
					Pix pixOriginalCleaned1 = SBImage.getPixFromSBImage(sbi2);
					pixOriginalCleaned = Leptonica1.pixConvertTo1(pixOriginalCleaned1, 128);
					LeptUtils.dispose(pixOriginalCleaned1);

					if (debugL <= 4) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixOriginalCleaned-" + lineCount + ".png",
								pixOriginalCleaned, ILeptonica.IFF_PNG);
					}

					// rotate pix
					Pix pixRotated1 = Leptonica1.pixRotate90(pixOriginalCleaned, -1);

					// Add Borders
					int pixDepth = Leptonica1.pixGetDepth(pixOriginalCleaned);
					int targetPixelValue = 0;
					if (pixDepth > 1) {
						targetPixelValue = (int) Math.pow(2, pixDepth) - 1;
					}

					if (debugL <= 1) {
						System.out.println(
								"Target pixel value for border in thread - " + lineCount + " is : " + targetPixelValue);
					}

					int bordersAdded = 10;
					pixRotated = Leptonica1.pixAddBorder(pixRotated1, bordersAdded, targetPixelValue);
					LeptUtils.dispose(pixRotated1);

					// scale pix for deskewing
					int prWidth = Leptonica1.pixGetWidth(pixRotated);
					int prHeight = Leptonica1.pixGetHeight(pixRotated);
					float targetDimensionX = 500f; // width
					float targetDimensionY = 1200f; // height
					float scaleX = targetDimensionX / prWidth;
					float scaleY = targetDimensionY / prHeight;
					float maxScale = Math.max(scaleX, scaleY);
					int sharpWidth = (maxScale < 0.7) ? 1 : 2;
					Pix pixRotatedScaled1 = Leptonica1.pixScaleGeneral(pixRotated, scaleX, scaleY, 0.0f, sharpWidth);

					// Dilate, then deskew
					int xDilation = 30;
					int yDilation = 2;
					pixRotatedScaled = Leptonica1.pixDilateCompBrick(null, pixRotatedScaled1, xDilation, yDilation);
					LeptUtils.dispose(pixRotatedScaled1);
					if (debugL <= 3) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixRotatedScaled-" + lineCount + ".png",
								pixRotatedScaled, ILeptonica.IFF_PNG);
					}

					// find deskew angle for scaled image. From that, determine the deskew angle for
					// the original image and, do the rotation
					FloatBuffer angle = FloatBuffer.allocate(1);
					FloatBuffer confidence = FloatBuffer.allocate(1);
					Leptonica1.pixFindSkew(pixRotatedScaled, angle, confidence);
					float angleForRotation = (angle.get() * (float) Math.PI) / 180; // in radians
					if (debugL <= 2) {
						System.out.println("Angle for rotation - " + lineCount + " is : " + angleForRotation);
					}
					int sign = (angleForRotation > 0) ? 1 : -1;
					double tanTheta = Math.tan(Math.abs(angleForRotation)) * (scaleX / scaleY);
					double rotationAngleForOriginal = Math.atan(tanTheta) * sign;
					pixRotatedDeskewed = Leptonica1.pixVShear(null, pixRotated, prWidth / 2,
							(float) rotationAngleForOriginal, ILeptonica.L_BRING_IN_WHITE);

					// rotate the deskewed image back
					pixOriginalDeskewed = Leptonica1.pixRotate90(pixRotatedDeskewed, 1);
					if (debugL <= 3) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixOriginalDeskewed-" + lineCount + ".png",
								pixOriginalDeskewed, ILeptonica.IFF_PNG);
					}

					Pix pixSource = Leptonica1.pixRemoveBorder(pixOriginalDeskewed,
							(bordersAdded + borderAdjustment) - 1);
					if (debugL <= 4) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "finalDeskewedPix-" + lineCount + ".png",
								pixSource, ILeptonica.IFF_PNG);
					}

					LeptUtils.dispose(box);
					LeptUtils.dispose(pixOriginal);
					LeptUtils.dispose(bilateralPixFirst);
					LeptUtils.dispose(unsharpMaskedPix);
					LeptUtils.dispose(backgroundNormalisedPix);
					LeptUtils.dispose(bilateralPix);
					LeptUtils.dispose(pixOriginalCleaned);
					LeptUtils.dispose(pixRotated);
					LeptUtils.dispose(pixRotatedScaled);
					LeptUtils.dispose(pixRotatedDeskewed);
					LeptUtils.dispose(pixOriginalDeskewed);
					// System.gc();

					Rectangle[] wordBoxes = SBImageUtils.getDefaultBoxes1(pixSource, debugL);
					DimensionScaling scalingFactor = SBImageUtils.getScalingFactors(wordBoxes, debugL);
					if (debugL <= 4) {
						System.out.println(scalingFactor);
					}
					Pix pixSource1 = Leptonica1.pixScale(pixSource, (float) scalingFactor.widthScaleFactor,
							(float) scalingFactor.heightScaleFactor);
					if (debugL <= 4) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "finalDeskewedScaledPix-" + lineCount + ".png",
								pixSource1, ILeptonica.IFF_PNG);
					}
					final int width = Leptonica1.pixGetWidth(pixSource1);
					final int height = Leptonica1.pixGetHeight(pixSource1);
					final int newHeight1 = (height * 3) + (2 * verticalGap) + (2 * border);
					final int newWidth1 = width + (2 * border);
					final int newHeight2 = (height * (2)) + (verticalGap) + (2 * border);
					final int newWidth2 = width + (2 * border);

					Pix targetPix1 = null;
					Pix pixScaledDown1 = null;
					Pix pixScaledDown2 = null;
					Pix pixScaledDown3 = null;
					Pix pixScaledDown4 = null;
					Pix pixScaledDown5 = null;

					targetPix1 = Leptonica1.pixCreate(newWidth1, newHeight1, 1);
					Leptonica1.pixSetBlackOrWhite(targetPix1, ILeptonica.L_SET_WHITE);
					pixScaledDown1 = Leptonica1.pixDilateBrick(null, pixSource, 2, 4);
					pixScaledDown2 = Leptonica1.pixScale(pixScaledDown1, (float) scalingFactor.widthScaleFactor,
							(float) scalingFactor.heightScaleFactor);
					pixScaledDown3 = Leptonica1.pixDilateBrick(null, pixSource, 3, 2);
					pixScaledDown4 = Leptonica1.pixDilateBrick(null, pixScaledDown3, 1, 3);
					pixScaledDown5 = Leptonica1.pixScale(pixScaledDown4, (float) scalingFactor.widthScaleFactor,
							(float) scalingFactor.heightScaleFactor);
					Leptonica1.pixRasterop(targetPix1, border, border, width, height, ILeptonica.PIX_PAINT, pixSource1,
							0, 0);
					Leptonica1.pixRasterop(targetPix1, border, border + height + verticalGap, width, height,
							ILeptonica.PIX_PAINT, pixScaledDown2, 0, 0);
					Leptonica1.pixRasterop(targetPix1, border, border + (2 * height) + (2 * verticalGap), width, height,
							ILeptonica.PIX_PAINT, pixScaledDown5, 0, 0);
					synchronized (pixArrayMonitor) {
						Leptonica1.pixaAddPix(pixa, targetPix1, ILeptonica.L_COPY);
					}

					LeptUtils.dispose(targetPix1);
					LeptUtils.dispose(pixScaledDown1);
					LeptUtils.dispose(pixScaledDown2);
					LeptUtils.dispose(pixScaledDown3);
					LeptUtils.dispose(pixScaledDown4);
					LeptUtils.dispose(pixScaledDown5);

					Pix targetPix2 = null;
					Pix pixDilated1 = null;
					Pix pixReducedDilated1 = null;
					Pix pixDilated2 = null;
					Pix pixReducedDilated2 = null;
					targetPix2 = Leptonica1.pixCreate(newWidth2, newHeight2, 1);
					Leptonica1.pixSetBlackOrWhite(targetPix2, ILeptonica.L_SET_WHITE);
					pixDilated1 = Leptonica1.pixDilateBrick(null, pixSource, 3, 4);
					pixReducedDilated1 = Leptonica1.pixScale(pixDilated1, (float) scalingFactor.widthScaleFactor,
							(float) scalingFactor.heightScaleFactor);
					Leptonica1.pixRasterop(targetPix2, border, border, width, height, ILeptonica.PIX_PAINT,
							pixReducedDilated1, 0, 0);
					pixDilated2 = Leptonica1.pixDilateBrick(null, pixDilated1, 1, 2);
					pixReducedDilated2 = Leptonica1.pixScale(pixDilated2, (float) scalingFactor.widthScaleFactor,
							(float) scalingFactor.heightScaleFactor);
					Leptonica1.pixRasterop(targetPix2, border, border + height + verticalGap, width, height,
							ILeptonica.PIX_PAINT, pixReducedDilated2, 0, 0);
					synchronized (pixArrayMonitor) {
						Leptonica1.pixaAddPix(pixa, targetPix2, ILeptonica.L_COPY);
					}
					// Leptonica1.pixaAddPix(pixa, targetPix2, ILeptonica.L_CLONE);
					LeptUtils.dispose(pixDilated1);
					LeptUtils.dispose(pixReducedDilated1);
					LeptUtils.dispose(pixDilated2);
					LeptUtils.dispose(pixReducedDilated2);
					LeptUtils.dispose(targetPix2);

					LeptUtils.dispose(pixSource);
					LeptUtils.dispose(pixSource1);

					if (debugL <= 2) {
						System.out.println("Finishing outer thread for line " + lineCount);
					}
					return Boolean.TRUE;
				} catch (Exception e) {
					e.printStackTrace();
					return Boolean.FALSE;
				}
			}, outerThreadService));
			CompletableFuture.allOf(outerCFS.toArray(new CompletableFuture[outerCFS.size()])).join();
			if (debugL <= 2) {
				System.out.println("Finishing all threads for line " + lineCount);
			}
			System.gc();
		}
		System.gc();
		return pixa;
	}

	public static Pixa getPixArray5(Pix pix, ArrayList<Rectangle> lines, ExecutorService outerThreadService,
			ExecutorService innerThreadService, int debugL) throws Exception {
		final Pixa pixa = Leptonica.INSTANCE.pixaCreate(lines.size());
		final int pixHeight = Leptonica1.pixGetHeight(pix);
		final int pixWidth = Leptonica1.pixGetWidth(pix);
		final int verticalGap = 20;
		final int border = 20;
		int count = 0;
		final Object pixArrayMonitor = new Object();
		for (final Rectangle line : lines) {
			final int lineCount = ++count;
			ArrayList<CompletableFuture<Boolean>> outerCFS = new ArrayList<CompletableFuture<Boolean>>();
			outerCFS.add(CompletableFuture.supplyAsync(() -> {
				if (debugL <= 2) {
					System.out.println("Started outer thread for line " + lineCount);
				}
				try {
					Box box = null;
					Pix pixOriginal = null;
					Pix bilateralPixFirst = null;
					Pix unsharpMaskedPix = null;
					Pix backgroundNormalisedPix = null;
					Pix bilateralPix = null;
					Pix pixOriginalCleaned = null;
					Pix pixRotated = null;
					Pix pixRotatedScaled = null;
					Pix pixRotatedDeskewed = null;
					Pix pixOriginalDeskewed = null;
					Pix pixSource = null;

					int borderAdjustment = 3;
					// ArrayList<CompletableFuture<Boolean>> innerCFS = new
					// ArrayList<CompletableFuture<Boolean>>();
					box = Leptonica1.boxCreate(Math.max(0, line.x - borderAdjustment),
							Math.max(0, line.y - borderAdjustment),
							Math.min(line.width + (2 * borderAdjustment), pixWidth - 1 - (line.x - borderAdjustment)),
							Math.min(line.height + (2 * borderAdjustment),
									pixHeight - 1 - (line.y - borderAdjustment)));
					Pix pixOriginal1 = Leptonica1.pixClipRectangle(pix, box, null);
					pixOriginal = Leptonica1.pixConvertTo8(pixOriginal1, 0);
					LeptUtils.dispose(pixOriginal1);

					if (debugL <= 4) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "originalPix -" + lineCount + ".png",
								pixOriginal, ILeptonica.IFF_PNG);
					}

					// OK till here

					bilateralPixFirst = Leptonica1.pixBilateralGray(pixOriginal, 0.8f, 50.0f, 10, 1);
					unsharpMaskedPix = Leptonica1.pixUnsharpMaskingGray(bilateralPixFirst, 1, 0.5f);
					backgroundNormalisedPix = Leptonica1.pixBackgroundNormFlex(unsharpMaskedPix, 5, 5, 2, 2, 0);
					bilateralPix = Leptonica1.pixBilateralGray(backgroundNormalisedPix, 0.8f, 25.0f, 5, 1);

					if (debugL <= 4) {
						System.out.println("For line " + lineCount + " : " + "bilateralPix is " + bilateralPix);
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "bilateralPix -" + lineCount + ".png",
								bilateralPix, ILeptonica.IFF_PNG);
					}

					// OK till here

					// if bilateralPix is null, because the dimensions of the pix is too small for
					// normalisation, then use the original pix
					if (bilateralPix == null) {
						bilateralPix = Leptonica1.pixCopy(null, pixOriginal);
					}

					// clean the image, and convert to 1 to enable the other Leptonica processing
					final SBImage sbi1 = SBImage.getSBImageFromPix(bilateralPix, debugL);
					if (debugL <= 4) {
						SBImageUtils.writeFile(sbi1, "png",
								SBImageUtils.baseTestDir + "bilateralSBImage -" + lineCount + ".png");
					}
					// final SBImage sbi2 = SBImageUtils.bagchiBinarizationForPK1(sbi1,
					// Math.min(4, (sbi1.width / 350) + 1), 1, lineCount, debugL);
					final SBImage sbi2 = SBImageUtils.bagchiBinarizationForPK2(sbi1, Math.max(2, (sbi1.width / 45) + 1),
							1, lineCount, debugL);
					if (debugL <= 3) {
						System.out.println("About to get Pix from SBImage for image - " + lineCount);
					}
					Pix pixOriginalCleaned1 = SBImage.getPixFromSBImage(sbi2);
					pixOriginalCleaned = Leptonica1.pixConvertTo1(pixOriginalCleaned1, 128);
					LeptUtils.dispose(pixOriginalCleaned1);

					if (debugL <= 4) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixOriginalCleaned-" + lineCount + ".png",
								pixOriginalCleaned, ILeptonica.IFF_PNG);
					}

					// OK till here

					// ---------------------------------------------
					// Cleaned Pix got
					// Now, deskew it, if needed

					// rotate pix, as it is a long strip with a small height
					Pix pixRotated1 = Leptonica1.pixRotate90(pixOriginalCleaned, -1);

					// Add Borders, to ensure the algo works better
					int pixDepth = Leptonica1.pixGetDepth(pixOriginalCleaned);
					int targetPixelValue = 0;
					if (pixDepth > 1) {
						targetPixelValue = (int) Math.pow(2, pixDepth) - 1;
					}

					if (debugL <= 1) {
						System.out.println(
								"Target pixel value for border in thread - " + lineCount + " is : " + targetPixelValue);
					}

					int bordersAdded = 10;
					pixRotated = Leptonica1.pixAddBorder(pixRotated1, bordersAdded, targetPixelValue);
					LeptUtils.dispose(pixRotated1);

					// scale pix for deskewing, to ensure the angle of skew is determined better
					int prWidth = Leptonica1.pixGetWidth(pixRotated);
					int prHeight = Leptonica1.pixGetHeight(pixRotated);
					float targetDimensionX = 500f; // width
					float targetDimensionY = 1200f; // height
					float scaleX = targetDimensionX / prWidth;
					float scaleY = targetDimensionY / prHeight;
					float maxScale = Math.max(scaleX, scaleY);
					int sharpWidth = (maxScale < 0.7) ? 1 : 2;
					Pix pixRotatedScaled1 = Leptonica1.pixScaleGeneral(pixRotated, scaleX, scaleY, 0.0f, sharpWidth);

					// Dilate, then deskew the pix
					int xDilation = 30;
					int yDilation = 2;
					pixRotatedScaled = Leptonica1.pixDilateCompBrick(null, pixRotatedScaled1, xDilation, yDilation);
					LeptUtils.dispose(pixRotatedScaled1);
					if (debugL <= 3) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixRotatedScaled-" + lineCount + ".png",
								pixRotatedScaled, ILeptonica.IFF_PNG);
					}

					// OK till here

					// find deskew angle for scaled image. From that, determine the deskew angle for
					// the original image and, do the rotation
					FloatBuffer angle = FloatBuffer.allocate(1);
					FloatBuffer confidence = FloatBuffer.allocate(1);
					Leptonica1.pixFindSkew(pixRotatedScaled, angle, confidence);
					float angleInDegrees = angle.get(); // in degrees

					if (angleInDegrees > 2) {
						float angleForRotation = (angleInDegrees * (float) Math.PI) / 180; // in radians
						if (debugL <= 2) {
							System.out.println("Angle for rotation - " + lineCount + " is : " + angleForRotation);
						}
						int sign = (angleForRotation > 0) ? 1 : -1;
						double tanTheta = Math.tan(Math.abs(angleForRotation)) * (scaleX / scaleY);
						double rotationAngleForOriginal = Math.atan(tanTheta) * sign;
						pixRotatedDeskewed = Leptonica1.pixVShear(null, pixRotated, 0, (float) rotationAngleForOriginal,
								ILeptonica.L_BRING_IN_WHITE);

						// rotate the deskewed image back
						pixOriginalDeskewed = Leptonica1.pixRotate90(pixRotatedDeskewed, 1);
						if (debugL <= 3) {
							Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixOriginalDeskewed-" + lineCount + ".png",
									pixOriginalDeskewed, ILeptonica.IFF_PNG);
						}

						Pix pixSourceNoBorder = Leptonica1.pixRemoveBorder(pixOriginalDeskewed,
								(bordersAdded + borderAdjustment) - 1);
						pixSource = Leptonica1.pixDeskew(pixSourceNoBorder, 0);
						if (debugL <= 4) {
							Leptonica1.pixWrite(SBImageUtils.baseTestDir + "finalDeskewedPix-" + lineCount + ".png",
									pixSource, ILeptonica.IFF_PNG);
						}
						LeptUtils.dispose(pixRotatedDeskewed);
						LeptUtils.dispose(pixOriginalDeskewed);
						LeptUtils.dispose(pixSourceNoBorder);
					} else {
						pixSource = Leptonica1.pixCopy(null, pixOriginalCleaned);
					}

					LeptUtils.dispose(box);
					LeptUtils.dispose(pixOriginal);
					LeptUtils.dispose(bilateralPixFirst);
					LeptUtils.dispose(unsharpMaskedPix);
					LeptUtils.dispose(backgroundNormalisedPix);
					LeptUtils.dispose(bilateralPix);
					LeptUtils.dispose(pixOriginalCleaned);
					LeptUtils.dispose(pixRotated);
					LeptUtils.dispose(pixRotatedScaled);
					// System.gc();

					Rectangle[] wordBoxes = SBImageUtils.getDefaultBoxes1(pixSource, debugL);
					DimensionScaling scalingFactor = SBImageUtils.getScalingFactors(wordBoxes, debugL);
					if (debugL <= 4) {
						System.out.println(scalingFactor);
					}
					Pix pixSource1 = Leptonica1.pixScale(pixSource, (float) (scalingFactor.widthScaleFactor),
							(float) (scalingFactor.heightScaleFactor));
					if (debugL <= 4) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "finalDeskewedScaledPix-" + lineCount + ".png",
								pixSource1, ILeptonica.IFF_PNG);
					}

					final int originalWidth = Leptonica1.pixGetWidth(pixSource);
					final int originalHeight = Leptonica1.pixGetHeight(pixSource);
					final int width = Leptonica1.pixGetWidth(pixSource1);
					final int height = Leptonica1.pixGetHeight(pixSource1);
					final int newHeight1 = originalHeight + (int) (height * 6.25) + (6 * verticalGap) + (2 * border);
					final int newWidth1 = Math.max(Math.max((int) (width * 1.25), originalWidth),
							(int) ((originalWidth * scalingFactor.widthScaleFactor * 1.25)
									/ scalingFactor.heightScaleFactor))
							+ (2 * border);

					Pix targetPix1 = null;
					Pix pixScaled1 = null;
					Pix pixScaled2 = null;
					Pix pixScaled3 = null;
					Pix pixScaled4 = null;
					Pix pixScaled5 = null;

					targetPix1 = Leptonica1.pixCreate(newWidth1, newHeight1, 1);
					Leptonica1.pixSetBlackOrWhite(targetPix1, ILeptonica.L_SET_WHITE);

					Leptonica1.pixRasterop(targetPix1, border, border, width + 2, height, ILeptonica.PIX_PAINT,
							pixSource1, 0, 0);

					pixScaled1 = Leptonica1.pixDilateBrick(null, pixSource, 2, 4);
					pixScaled2 = Leptonica1.pixScale(pixScaled1, (float) scalingFactor.widthScaleFactor,
							(float) scalingFactor.heightScaleFactor);
					Leptonica1.pixRasterop(targetPix1, border, border + height + verticalGap, width + 2, height,
							ILeptonica.PIX_PAINT, pixScaled2, 0, 0);

					pixScaled3 = Leptonica1.pixDilateBrick(null, pixSource, 3, 2);
					pixScaled4 = Leptonica1.pixDilateBrick(null, pixScaled3, 1, 3);
					pixScaled5 = Leptonica1.pixScale(pixScaled4, (float) scalingFactor.widthScaleFactor,
							(float) scalingFactor.heightScaleFactor);
					Leptonica1.pixRasterop(targetPix1, border, border + (2 * height) + (2 * verticalGap), width + 2,
							height, ILeptonica.PIX_PAINT, pixScaled5, 0, 0);

					LeptUtils.dispose(pixScaled1);
					LeptUtils.dispose(pixScaled2);
					LeptUtils.dispose(pixScaled3);
					LeptUtils.dispose(pixScaled4);
					LeptUtils.dispose(pixScaled5);

					Pix pixScaled6 = null;
					Pix pixScaled7 = null;
					Pix pixScaled8 = null;
					Pix pixScaled9 = null;
					Pix pixScaled10 = null;

					pixScaled6 = Leptonica1.pixDilateBrick(null, pixSource, 3, 4);
					pixScaled7 = Leptonica1.pixScale(pixScaled6, (float) scalingFactor.widthScaleFactor * 1.15f,
							(float) scalingFactor.heightScaleFactor);
					Leptonica1.pixRasterop(targetPix1, border, border + (3 * height) + (3 * verticalGap),
							(int) (width * 1.15) + 2, height, ILeptonica.PIX_PAINT, pixScaled7, 0, 0);

					pixScaled8 = Leptonica1.pixScale(pixSource1, 1.25f, 1.25f);
					pixScaled9 = Leptonica1.pixDilateBrick(null, pixScaled8, 3, 4);
					Leptonica1.pixRasterop(targetPix1, border, border + (4 * height) + (4 * verticalGap),
							(int) ((width * 1.25) + 2), (int) (height * 1.25), ILeptonica.PIX_PAINT, pixScaled9, 0, 0);

					if ((((width * 1.0) / originalWidth) < 0.6)
							&& ((scalingFactor.heightScaleFactor / scalingFactor.widthScaleFactor) > 1.55)) {
						Pix pixScaled11 = Leptonica1.pixScale(pixSource, 0.7f, 1.0f);
						Leptonica1.pixRasterop(targetPix1, border, border + (int) (5.25 * height) + (5 * verticalGap),
								(int) (originalWidth * 0.7), originalHeight, ILeptonica.PIX_PAINT, pixScaled11, 0, 0);
						LeptUtils.dispose(pixScaled11);
					} else {
						Leptonica1.pixRasterop(targetPix1, border, border + (int) (5.25 * height) + (5 * verticalGap),
								originalWidth, originalHeight, ILeptonica.PIX_PAINT, pixSource, 0, 0);
					}

					pixScaled10 = Leptonica1.pixDilateBrick(null, pixSource1, 2, 2);
					Leptonica1.pixRasterop(targetPix1, border,
							border + originalHeight + (int) (5.25 * height) + (6 * verticalGap), width + 2, height,
							ILeptonica.PIX_PAINT, pixScaled10, 0, 0);

					synchronized (pixArrayMonitor) {
						Leptonica1.pixaAddPix(pixa, targetPix1, ILeptonica.L_COPY);
					}

					LeptUtils.dispose(pixScaled6);
					LeptUtils.dispose(pixScaled7);
					LeptUtils.dispose(pixScaled8);
					LeptUtils.dispose(pixScaled9);
					LeptUtils.dispose(pixScaled10);
					LeptUtils.dispose(targetPix1);

					LeptUtils.dispose(pixSource);
					LeptUtils.dispose(pixSource1);

					if (debugL <= 2) {
						System.out.println("Finishing outer thread for line " + lineCount);
					}
					return Boolean.TRUE;
				} catch (Exception e) {
					e.printStackTrace();
					return Boolean.FALSE;
				}
			}, outerThreadService));
			CompletableFuture.allOf(outerCFS.toArray(new CompletableFuture[outerCFS.size()])).join();
			if (debugL <= 2) {
				System.out.println("Finishing all threads for line " + lineCount);
			}
			System.gc();
		}
		System.gc();
		return pixa;
	}

	// ===========================================================

	public static Pixa getPixArray6(Pix pix, ArrayList<Rectangle> lines, ExecutorService outerThreadService,
			ExecutorService innerThreadService, int debugL) throws Exception {
		final Pixa pixa = Leptonica.INSTANCE.pixaCreate(lines.size());
		final int pixHeight = Leptonica1.pixGetHeight(pix);
		final int pixWidth = Leptonica1.pixGetWidth(pix);
		final int verticalGap = 20;
		final int border = 20;
		int count = 0;
		final Object pixArrayMonitor = new Object();
		for (final Rectangle line : lines) {
			final int lineCount = ++count;
			ArrayList<CompletableFuture<Boolean>> outerCFS = new ArrayList<CompletableFuture<Boolean>>();
			outerCFS.add(CompletableFuture.supplyAsync(() -> {
				if (debugL <= 2) {
					System.out.println("Started outer thread for line " + lineCount);
				}
				try {
					Box box = null;
					Pix pixOriginal = null;
					Pix bilateralPixFirst = null;
					Pix unsharpMaskedPix = null;
					Pix backgroundNormalisedPix = null;
					Pix bilateralPix = null;
					Pix pixOriginalCleaned = null;
					Pix pixRotated = null;
					Pix pixRotatedScaled = null;
					Pix pixRotatedDeskewed = null;
					Pix pixOriginalDeskewed = null;
					Pix pixSource = null;

					int borderAdjustment = 3;
					// ArrayList<CompletableFuture<Boolean>> innerCFS = new
					// ArrayList<CompletableFuture<Boolean>>();
					box = Leptonica1.boxCreate(Math.max(0, line.x - borderAdjustment),
							Math.max(0, line.y - borderAdjustment),
							Math.min(line.width + (2 * borderAdjustment), pixWidth - 1 - (line.x - borderAdjustment)),
							Math.min(line.height + (2 * borderAdjustment),
									pixHeight - 1 - (line.y - borderAdjustment)));
					Pix pixOriginal1 = Leptonica1.pixClipRectangle(pix, box, null);
					pixOriginal = Leptonica1.pixConvertTo8(pixOriginal1, 0);
					LeptUtils.dispose(pixOriginal1);

					if (debugL <= 4) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "originalPix -" + lineCount + ".png",
								pixOriginal, ILeptonica.IFF_PNG);
					}

					// OK till here

					bilateralPixFirst = Leptonica1.pixBilateralGray(pixOriginal, 0.8f, 50.0f, 10, 1);
					unsharpMaskedPix = Leptonica1.pixUnsharpMaskingGray(bilateralPixFirst, 1, 0.5f);
					backgroundNormalisedPix = Leptonica1.pixBackgroundNormFlex(unsharpMaskedPix, 5, 5, 2, 2, 0);
					bilateralPix = Leptonica1.pixBilateralGray(backgroundNormalisedPix, 0.8f, 25.0f, 5, 1);

					if (debugL <= 4) {
						System.out.println("For line " + lineCount + " : " + "bilateralPix is " + bilateralPix);
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "bilateralPix -" + lineCount + ".png",
								bilateralPix, ILeptonica.IFF_PNG);
					}

					// OK till here

					// if bilateralPix is null, because the dimensions of the pix is too small for
					// normalisation, then use the original pix
					if (bilateralPix == null) {
						bilateralPix = Leptonica1.pixCopy(null, pixOriginal);
					}

					// clean the image, and convert to 1 to enable the other Leptonica processing
					final SBImage sbi1 = SBImage.getSBImageFromPix(bilateralPix, debugL);
					if (debugL <= 4) {
						SBImageUtils.writeFile(sbi1, "png",
								SBImageUtils.baseTestDir + "bilateralSBImage -" + lineCount + ".png");
					}
					// final SBImage sbi2 = SBImageUtils.bagchiBinarizationForPK1(sbi1,
					// Math.min(4, (sbi1.width / 350) + 1), 1, lineCount, debugL);
					final SBImage sbi2 = SBImageUtils.bagchiBinarizationForPK2(sbi1, Math.max(2, (sbi1.width / 45) + 1),
							1, lineCount, debugL);
					if (debugL <= 3) {
						System.out.println("About to get Pix from SBImage for image - " + lineCount);
					}
					Pix pixOriginalCleaned1 = SBImage.getPixFromSBImage(sbi2);
					pixOriginalCleaned = Leptonica1.pixConvertTo1(pixOriginalCleaned1, 128);
					LeptUtils.dispose(pixOriginalCleaned1);

					if (debugL <= 4) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixOriginalCleaned-" + lineCount + ".png",
								pixOriginalCleaned, ILeptonica.IFF_PNG);
					}

					// OK till here

					// ---------------------------------------------
					// Cleaned Pix got
					// Now, deskew it, if needed

					// rotate pix, as it is a long strip with a small height
					Pix pixRotated1 = Leptonica1.pixRotate90(pixOriginalCleaned, -1);

					// Add Borders, to ensure the algo works better
					int pixDepth = Leptonica1.pixGetDepth(pixOriginalCleaned);
					int targetPixelValue = 0;
					if (pixDepth > 1) {
						targetPixelValue = (int) Math.pow(2, pixDepth) - 1;
					}

					if (debugL <= 1) {
						System.out.println(
								"Target pixel value for border in thread - " + lineCount + " is : " + targetPixelValue);
					}

					int bordersAdded = 10;
					pixRotated = Leptonica1.pixAddBorder(pixRotated1, bordersAdded, targetPixelValue);
					LeptUtils.dispose(pixRotated1);

					// scale pix for deskewing, to ensure the angle of skew is determined better
					int prWidth = Leptonica1.pixGetWidth(pixRotated);
					int prHeight = Leptonica1.pixGetHeight(pixRotated);
					float targetDimensionX = 500f; // width
					float targetDimensionY = 1200f; // height
					float scaleX = targetDimensionX / prWidth;
					float scaleY = targetDimensionY / prHeight;
					float maxScale = Math.max(scaleX, scaleY);
					int sharpWidth = (maxScale < 0.7) ? 1 : 2;
					Pix pixRotatedScaled1 = Leptonica1.pixScaleGeneral(pixRotated, scaleX, scaleY, 0.0f, sharpWidth);

					// Dilate, then deskew the pix
					int xDilation = 30;
					int yDilation = 2;
					pixRotatedScaled = Leptonica1.pixDilateCompBrick(null, pixRotatedScaled1, xDilation, yDilation);
					LeptUtils.dispose(pixRotatedScaled1);
					if (debugL <= 3) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixRotatedScaled-" + lineCount + ".png",
								pixRotatedScaled, ILeptonica.IFF_PNG);
					}

					// OK till here

					// find deskew angle for scaled image. From that, determine the deskew angle for
					// the original image and, do the rotation
					FloatBuffer angle = FloatBuffer.allocate(1);
					FloatBuffer confidence = FloatBuffer.allocate(1);
					Leptonica1.pixFindSkew(pixRotatedScaled, angle, confidence);
					float angleInDegrees = angle.get(); // in degrees

					if (angleInDegrees > 2) {
						float angleForRotation = (angleInDegrees * (float) Math.PI) / 180; // in radians
						if (debugL <= 2) {
							System.out.println("Angle for rotation - " + lineCount + " is : " + angleForRotation);
						}
						int sign = (angleForRotation > 0) ? 1 : -1;
						double tanTheta = Math.tan(Math.abs(angleForRotation)) * (scaleX / scaleY);
						double rotationAngleForOriginal = Math.atan(tanTheta) * sign;
						pixRotatedDeskewed = Leptonica1.pixVShear(null, pixRotated, 0, (float) rotationAngleForOriginal,
								ILeptonica.L_BRING_IN_WHITE);

						// rotate the deskewed image back
						pixOriginalDeskewed = Leptonica1.pixRotate90(pixRotatedDeskewed, 1);
						if (debugL <= 3) {
							Leptonica1.pixWrite(SBImageUtils.baseTestDir + "pixOriginalDeskewed-" + lineCount + ".png",
									pixOriginalDeskewed, ILeptonica.IFF_PNG);
						}

						Pix pixSourceNoBorder = Leptonica1.pixRemoveBorder(pixOriginalDeskewed,
								(bordersAdded + borderAdjustment) - 1);
						pixSource = Leptonica1.pixDeskew(pixSourceNoBorder, 0);
						if (debugL <= 4) {
							Leptonica1.pixWrite(SBImageUtils.baseTestDir + "finalDeskewedPix-" + lineCount + ".png",
									pixSource, ILeptonica.IFF_PNG);
						}
						LeptUtils.dispose(pixRotatedDeskewed);
						LeptUtils.dispose(pixOriginalDeskewed);
						LeptUtils.dispose(pixSourceNoBorder);
					} else {
						pixSource = Leptonica1.pixCopy(null, pixOriginalCleaned);
					}

					LeptUtils.dispose(box);
					LeptUtils.dispose(pixOriginal);
					LeptUtils.dispose(bilateralPixFirst);
					LeptUtils.dispose(unsharpMaskedPix);
					LeptUtils.dispose(backgroundNormalisedPix);
					LeptUtils.dispose(bilateralPix);
					LeptUtils.dispose(pixOriginalCleaned);
					LeptUtils.dispose(pixRotated);
					LeptUtils.dispose(pixRotatedScaled);
					// System.gc();

					Rectangle[] wordBoxes = SBImageUtils.getDefaultBoxes1(pixSource, debugL);
					DimensionScaling scalingFactor = SBImageUtils.getScalingFactors(wordBoxes, debugL);
					if (debugL <= 4) {
						System.out.println(scalingFactor);
					}
					Pix pixSource1 = Leptonica1.pixScale(pixSource, (float) (scalingFactor.widthScaleFactor),
							(float) (scalingFactor.heightScaleFactor));
					if (debugL <= 4) {
						Leptonica1.pixWrite(SBImageUtils.baseTestDir + "finalDeskewedScaledPix-" + lineCount + ".png",
								pixSource1, ILeptonica.IFF_PNG);
					}

					final int originalWidth = Leptonica1.pixGetWidth(pixSource);
					final int originalHeight = Leptonica1.pixGetHeight(pixSource);
					final int width = Leptonica1.pixGetWidth(pixSource1);
					final int height = Leptonica1.pixGetHeight(pixSource1);
					final int newHeight1 = originalHeight + (int) (height * 6.25) + (6 * verticalGap) + (2 * border);
					final int newWidth1 = Math.max(Math.max((int) (width * 1.25), originalWidth),
							(int) ((originalWidth * scalingFactor.widthScaleFactor * 1.25)
									/ scalingFactor.heightScaleFactor))
							+ (2 * border);

					Pix targetPix1 = null;
					Pix pixScaled1 = null;
					Pix pixScaled2 = null;
					Pix pixScaled3 = null;
					Pix pixScaled4 = null;
					Pix pixScaled5 = null;

					targetPix1 = Leptonica1.pixCreate(newWidth1, newHeight1, 1);
					Leptonica1.pixSetBlackOrWhite(targetPix1, ILeptonica.L_SET_WHITE);

					Leptonica1.pixRasterop(targetPix1, border, border, width + 2, height, ILeptonica.PIX_PAINT,
							pixSource1, 0, 0);

					pixScaled1 = Leptonica1.pixDilateBrick(null, pixSource, 2, 4);
					pixScaled2 = Leptonica1.pixScale(pixScaled1, (float) scalingFactor.widthScaleFactor,
							(float) scalingFactor.heightScaleFactor);
					Leptonica1.pixRasterop(targetPix1, border, border + height + verticalGap, width + 2, height,
							ILeptonica.PIX_PAINT, pixScaled2, 0, 0);

					pixScaled3 = Leptonica1.pixDilateBrick(null, pixSource, 3, 2);
					pixScaled4 = Leptonica1.pixDilateBrick(null, pixScaled3, 1, 3);
					pixScaled5 = Leptonica1.pixScale(pixScaled4, (float) scalingFactor.widthScaleFactor,
							(float) scalingFactor.heightScaleFactor);
					Leptonica1.pixRasterop(targetPix1, border, border + (2 * height) + (2 * verticalGap), width + 2,
							height, ILeptonica.PIX_PAINT, pixScaled5, 0, 0);

					LeptUtils.dispose(pixScaled1);
					LeptUtils.dispose(pixScaled2);
					LeptUtils.dispose(pixScaled3);
					LeptUtils.dispose(pixScaled4);
					LeptUtils.dispose(pixScaled5);

					Pix pixScaled6 = null;
					Pix pixScaled7 = null;
					Pix pixScaled8 = null;
					Pix pixScaled9 = null;
					Pix pixScaled10 = null;

					pixScaled6 = Leptonica1.pixDilateBrick(null, pixSource, 3, 4);
					pixScaled7 = Leptonica1.pixScale(pixScaled6, (float) scalingFactor.widthScaleFactor * 1.15f,
							(float) scalingFactor.heightScaleFactor);
					Leptonica1.pixRasterop(targetPix1, border, border + (3 * height) + (3 * verticalGap),
							(int) (width * 1.15) + 2, height, ILeptonica.PIX_PAINT, pixScaled7, 0, 0);

					pixScaled8 = Leptonica1.pixScale(pixSource1, 1.25f, 1.25f);
					pixScaled9 = Leptonica1.pixDilateBrick(null, pixScaled8, 3, 4);
					Leptonica1.pixRasterop(targetPix1, border, border + (4 * height) + (4 * verticalGap),
							(int) ((width * 1.25) + 2), (int) (height * 1.25), ILeptonica.PIX_PAINT, pixScaled9, 0, 0);

					if ((((width * 1.0) / originalWidth) < 0.6)
							&& ((scalingFactor.heightScaleFactor / scalingFactor.widthScaleFactor) > 1.55)) {
						Pix pixScaled11 = Leptonica1.pixScale(pixSource, 0.7f, 1.0f);
						Leptonica1.pixRasterop(targetPix1, border, border + (int) (5.25 * height) + (5 * verticalGap),
								(int) (originalWidth * 0.7), originalHeight, ILeptonica.PIX_PAINT, pixScaled11, 0, 0);
						LeptUtils.dispose(pixScaled11);
					} else {
						Leptonica1.pixRasterop(targetPix1, border, border + (int) (5.25 * height) + (5 * verticalGap),
								originalWidth, originalHeight, ILeptonica.PIX_PAINT, pixSource, 0, 0);
					}

					pixScaled10 = Leptonica1.pixDilateBrick(null, pixSource1, 2, 2);
					Leptonica1.pixRasterop(targetPix1, border,
							border + originalHeight + (int) (5.25 * height) + (6 * verticalGap), width + 2, height,
							ILeptonica.PIX_PAINT, pixScaled10, 0, 0);

					synchronized (pixArrayMonitor) {
						Leptonica1.pixaAddPix(pixa, targetPix1, ILeptonica.L_COPY);
					}

					LeptUtils.dispose(pixScaled6);
					LeptUtils.dispose(pixScaled7);
					LeptUtils.dispose(pixScaled8);
					LeptUtils.dispose(pixScaled9);
					LeptUtils.dispose(pixScaled10);
					LeptUtils.dispose(targetPix1);

					LeptUtils.dispose(pixSource);
					LeptUtils.dispose(pixSource1);

					if (debugL <= 2) {
						System.out.println("Finishing outer thread for line " + lineCount);
					}
					return Boolean.TRUE;
				} catch (Exception e) {
					e.printStackTrace();
					return Boolean.FALSE;
				}
			}, outerThreadService));
			CompletableFuture.allOf(outerCFS.toArray(new CompletableFuture[outerCFS.size()])).join();
			if (debugL <= 2) {
				System.out.println("Finishing all threads for line " + lineCount);
			}
			System.gc();
		}
		System.gc();
		return pixa;
	}

	// ========================================================

	/*
	 * public static ArrayList<OCRResultPix> ocrPixa(ArrayList<BIWrapperForOCR>
	 * textRegions) throws Exception {
	 *
	 * final ArrayList<OCRResultPix> pageResult = new
	 * ArrayList<OCRResultPix>(textRegions.size());
	 * ArrayList<CompletableFuture<Boolean>> cfs = new
	 * ArrayList<CompletableFuture<Boolean>>(textRegions.size()); // Long time1 =
	 * 0L; // Long time2 = 0L; // Long time3 = 0L; // Long time4 = 0L; // Long time5
	 * = 0L; // Long time6 = 0L; final ExecutorService threadService =
	 * Executors.newFixedThreadPool(Math.min(textRegions.size(), 30)); for
	 * (BIWrapperForOCR textRegion : textRegions) {
	 * cfs.add(CompletableFuture.supplyAsync(() -> { final OCRResultPix lineResult =
	 * new OCRResultPix(); pageResult.add(lineResult); TesseractHandle handleWrapper
	 * = null; // Instant t = Instant.now(); try { handleWrapper = (TesseractHandle)
	 * tesseractPool.borrowObject(); } catch (Exception e) { e.printStackTrace();
	 *
	 * } TessBaseAPI tesseractHandle = handleWrapper.getHandle(); //
	 * System.out.println( // textRegion + ": Time to get handle = " +
	 * Duration.between(t, // Instant.now()).toMillis()); //
	 * System.out.println("Using handle " + tesseractHandle); //
	 * System.out.println(tesseractHandle); // int i = 1; // Instant t =
	 * Instant.now(); // t = Instant.now();
	 * TessAPI1.TessBaseAPISetImage(tesseractHandle, textRegion.byteBuffer,
	 * textRegion.image.getWidth(), textRegion.image.getHeight(),
	 * textRegion.bytesPerPixel, textRegion.bytesPerLine); // System.out //
	 * .println(textRegion + ": Time to set image = " + Duration.between(t, //
	 * Instant.now()).toMillis()); // t = Instant.now(); int res =
	 * TessAPI1.TessBaseAPIGetSourceYResolution(tesseractHandle); if (res < 70) {
	 * TessAPI1.TessBaseAPISetSourceResolution(tesseractHandle, 70); } //
	 * System.out.println( // textRegion + ": Time to set resolution = " +
	 * Duration.between(t, // Instant.now()).toMillis()); ETEXT_DESC monitor = new
	 * ETEXT_DESC(); Instant t = Instant.now();
	 * TessAPI1.TessBaseAPIRecognize(tesseractHandle, monitor); System.out
	 * .println(textRegion + ": Time to recognize = " + Duration.between(t,
	 * Instant.now()).toMillis()); // t = Instant.now(); TessResultIterator ri =
	 * TessAPI1.TessBaseAPIGetIterator(tesseractHandle); //
	 * System.out.println(textRegion + ": Time to get result iterator = " // +
	 * Duration.between(t, Instant.now()).toMillis()); // t = Instant.now();
	 * TessPageIterator pi = TessAPI1.TessResultIteratorGetPageIterator(ri); //
	 * System.out.println( // textRegion + ": Time to get page iterator = " +
	 * Duration.between(t, // Instant.now()).toMillis()); // t = Instant.now();
	 * TessAPI1.TessPageIteratorBegin(pi); // System.out.println(textRegion +
	 * ": Time to get begin page iteration = " // + Duration.between(t,
	 * Instant.now()).toMillis());
	 *
	 * // System.out.println(textRegion + ": Time to recognize, get RI, PI and begin
	 * = // " // + Duration.between(t, Instant.now()).toMillis()); // int level =
	 * TessAPI1.TessPageIteratorLevel.RIL_TEXTLINE; int level =
	 * TessAPI1.TessPageIteratorLevel.RIL_TEXTLINE; // t = Instant.now(); do {
	 * Pointer ptr = TessAPI1.TessResultIteratorGetUTF8Text(ri, level); if (ptr ==
	 * null) { break; } String words = ptr.getString(0); float confidence =
	 * TessAPI1.TessResultIteratorConfidence(ri, level); // System.gc(); //
	 * System.out.println(words + " - confidence = " + (int) confidence);
	 * lineResult.add(words, confidence); TessAPI1.TessDeleteText(ptr); } while
	 * (TessAPI1.TessPageIteratorNext(pi, level) == TRUE); //
	 * System.out.println(textRegion + ": Time to iterate through the textlines = "
	 * // + Duration.between(t, Instant.now()).toMillis()); //
	 * System.out.println("Time taken = " + Duration.between(t, //
	 * Instant.now()).toMillis()); // t = Instant.now(); // t = Instant.now();
	 * TessAPI1.TessPageIteratorDelete(pi); // System.out.println(textRegion +
	 * ": Time to delete page iterator = " // + Duration.between(t,
	 * Instant.now()).toMillis()); // System.gc(); //
	 * TessAPI1.TessResultIteratorDelete(ri); // TessAPI1.TessBaseAPIClear(handle);
	 * // TessAPI1.TessBaseAPIClearAdaptiveClassifier(handle); // t = Instant.now();
	 * tesseractPool.returnObject(handleWrapper); // System.out.println( //
	 * textRegion + ": Time to return handle = " + Duration.between(t, //
	 * Instant.now()).toMillis()); new Runnable() {
	 *
	 * @Override public void run() { System.gc(); } }.run(); return Boolean.TRUE; //
	 * System.out.println("Time taken = " + Duration.between(t, //
	 * Instant.now()).toMillis()); }, threadService)); } Instant t = Instant.now();
	 * CompletableFuture.allOf(cfs.toArray(new
	 * CompletableFuture[cfs.size()])).join(); threadService.shutdown();
	 * System.out.println("Time waiting for join = " + Duration.between(t,
	 * Instant.now()).toMillis()); // System.out.println("Done an image"); //
	 * TessAPI1.TessBaseAPIPrintVariablesToFile(handle, //
	 * "E:\\TechWerx\\Java\\Working\\" + i++ + ".data"); return pageResult; }
	 *
	 */

	public static ArrayList<OCRResultPix> ocrPixa(Pixa pixArray, ExecutorService outer, int debugL) throws Exception {

		int noOfImages = Leptonica1.pixaGetCount(pixArray);
		final ArrayList<OCRResultPix> pageResult = new ArrayList<OCRResultPix>(noOfImages);
		ArrayList<CompletableFuture<Boolean>> cfs = new ArrayList<CompletableFuture<Boolean>>(noOfImages);

		for (int i = 0; i < noOfImages; ++i) {
			final Pix pix = Leptonica1.pixaGetPix(pixArray, i, ILeptonica.L_COPY);
			if (debugL <= 5) {
				Leptonica1.pixWrite(SBImageUtils.baseTestDir + "finalPixInOCR-" + i + ".png", pix, ILeptonica.IFF_PNG);
			}
			final int imageNumber = i;
			cfs.add(CompletableFuture.supplyAsync(() -> {
				if (debugL <= 5) {
					System.out.println("Starting processing of " + imageNumber);
				}
				final OCRResultPix lineResult = new OCRResultPix();
				pageResult.add(lineResult);
				TessBaseAPI tesseractHandle = TessAPI1.TessBaseAPICreate();
				TessAPI1.TessBaseAPISetImage2(tesseractHandle, pix);
				IntBuffer xRes = IntBuffer.allocate(1);
				// IntBuffer yRes = IntBuffer.allocate(1);
				// int res = Leptonica1.pixGetResolution(pix, xRes, yRes);
				TessAPI1.TessBaseAPISetSourceResolution(tesseractHandle, xRes.get());
				ETEXT_DESC monitor = new ETEXT_DESC();
				TessAPI1.TessBaseAPIRecognize(tesseractHandle, monitor);
				TessResultIterator ri = TessAPI1.TessBaseAPIGetIterator(tesseractHandle);
				TessPageIterator pi = TessAPI1.TessResultIteratorGetPageIterator(ri);
				TessAPI1.TessPageIteratorBegin(pi);
				int level = TessAPI1.TessPageIteratorLevel.RIL_TEXTLINE;
				do {
					Pointer ptr = TessAPI1.TessResultIteratorGetUTF8Text(ri, level);
					if (ptr == null) {
						break;
					}
					String words = ptr.getString(0);
					float confidence = TessAPI1.TessResultIteratorConfidence(ri, level);
					if (debugL <= 5) {
						System.out.println(words + " : " + confidence);
					}
					lineResult.add(words, confidence);
					TessAPI1.TessDeleteText(ptr);
				} while (TessAPI1.TessPageIteratorNext(pi, level) == 1);
				TessAPI1.TessPageIteratorDelete(pi);
				LeptUtils.dispose(pix);
				return Boolean.TRUE;
			}, outer));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		return pageResult;
	}

	/*
	 * public static ArrayList<OCRResultPix> ocrPixa1(Pixa pixArray, ExecutorService
	 * outer, int debugL) throws Exception {
	 *
	 * int noOfImages = Leptonica1.pixaGetCount(pixArray); final
	 * ArrayList<OCRResultPix> pageResult = new ArrayList<OCRResultPix>(noOfImages);
	 * ArrayList<CompletableFuture<Boolean>> cfs = new
	 * ArrayList<CompletableFuture<Boolean>>(noOfImages); for (int i = 0; i <
	 * noOfImages; ++i) { final Pix pix = Leptonica1.pixaGetPix(pixArray, i,
	 * ILeptonica.L_COPY); final int imageNumber = i;
	 * cfs.add(CompletableFuture.supplyAsync(() -> { if (debugL <= 5) {
	 * System.out.println("Starting processing of " + imageNumber); } final
	 * OCRResultPix lineResult = new OCRResultPix(); pageResult.add(lineResult);
	 * TesseractHandle handleWrapper = null; try { handleWrapper = (TesseractHandle)
	 * tesseractPool.borrowObject(); } catch (Exception e) { e.printStackTrace(); }
	 * TessBaseAPI tesseractHandle = handleWrapper.getHandle();
	 * TessAPI1.TessBaseAPISetImage2(tesseractHandle, pix); IntBuffer xRes =
	 * IntBuffer.allocate(1); IntBuffer yRes = IntBuffer.allocate(1);
	 * Leptonica1.pixGetResolution(pix, xRes, yRes);
	 * TessAPI1.TessBaseAPISetSourceResolution(tesseractHandle, xRes.get()); Pointer
	 * textPtr = TessAPI1.TessBaseAPIGetUTF8Text(tesseractHandle); String words =
	 * textPtr.getString(0); lineResult.add(words, 0.0f);
	 * TessAPI1.TessDeleteText(textPtr); tesseractPool.returnObject(handleWrapper);
	 * LeptUtils.dispose(pix); return Boolean.TRUE; }, outer)); }
	 * CompletableFuture.allOf(cfs.toArray(new
	 * CompletableFuture[cfs.size()])).join(); return pageResult; }
	 *
	 */

	public static ArrayList<OCRResultPix> ocrPixa2(Pixa pixArray, ExecutorService outer, int debugL) throws Exception {

		int noOfImages = Leptonica1.pixaGetCount(pixArray);
		final ArrayList<OCRResultPix> pageResult = new ArrayList<OCRResultPix>(noOfImages);
		ArrayList<CompletableFuture<Boolean>> cfs = new ArrayList<CompletableFuture<Boolean>>(noOfImages);
		for (int i = 0; i < noOfImages; ++i) {
			final Pix pix = Leptonica1.pixaGetPix(pixArray, i, ILeptonica.L_COPY);
			final int imageNumber = i;
			cfs.add(CompletableFuture.supplyAsync(() -> {
				if (debugL <= 5) {
					System.out.println("Starting processing of " + imageNumber);
				}
				final OCRResultPix lineResult = new OCRResultPix();
				pageResult.add(lineResult);
				Tesseract instance = new Tesseract();
				// processInstance.setDatapath("C:\\Program Files
				// (x86)\\Tesseract-OCR\\tessdata");
				instance.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
				instance.setLanguage("eng");
				instance.setTessVariable("tessedit_ocr_engine_mode", "2");
				instance.setTessVariable("tessedit_pageseg_mode", "2");
				instance.setTessVariable("tessedit_parallelize", "1");
				instance.setTessVariable("debug_file", "/dev/null");
				// processInstance.setTessVariable("segment_segcost_rating", "F");
				// processInstance.setTessVariable("enable_new_segsearch", "0");
				// processInstance.setTessVariable("language_model_ngram_on", "0");
				// processInstance.setTessVariable("textord_force_make_prop_words", "F");
				// processInstance.setTessVariable("edges_max_children_per_outline", "40");
				// --------------------------------------
				// Unsure about these, but they seem useful sometimes
				// processInstance.setTessVariable("textord_tabfind_find_tables", "0");
				// processInstance.setTessVariable("textord_biased_skewcalc", "0");
				// processInstance.setTessVariable("textord_interpolating_skew", "0");

				// ---------------------------------------

				// processInstance.setTessVariable("applybox_debug", "0");
				// processInstance.setTessVariable("tessedit_page_number", "1");
				// processInstance.setTessVariable("enable_noise_removal", "0");
				// processInstance.setTessVariable("edges_max_children_per_outline", "5");
				// processInstance.setTessVariable("tessedit_resegment_from_line_boxes", "0");
				// processInstance.setTessVariable("tessedit_train_line_recognizer", "0");
				// processInstance.setTessVariable("tessedit_make_boxes_from_boxes", "0");
				// processInstance.setTessVariable("tessedit_train_from_boxes", "0");
				// processInstance.setTessVariable("tessedit_abigs_training", "0");
				// processInstance.setTessVariable("user_words_file", "...");
				// processInstance.setTessVariable("chop_enable", "T");
				// processInstance.setTessVariable("use_new_state_cost", "F");

				// don't use the ones below this
				// -----------------------------
				// processInstance.setTessVariable("textord_restore_underlines", "0");
				// processInstance.setTessVariable("tessedit_char_whitelist",
				// "€$0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz,.'\\\"-/+%");
				// processInstance.setTessVariable("tessedit_char_blacklist", "0");
				// processInstance.setTessVariable("textord_restore_underlines", "0");

				IntBuffer xRes = IntBuffer.allocate(1);
				IntBuffer yRes = IntBuffer.allocate(1);
				Leptonica1.pixGetResolution(pix, xRes, yRes);
				boolean success = true;
				String result = null;
				try {
					BufferedImage image = SBImageUtils.convertPixToImage(pix, debugL);
					result = instance.doOCR(image);
				} catch (Exception e) {
					success = false;
				}
				if (success) {
					lineResult.add(result, 0.0f);
				}
				LeptUtils.dispose(pix);
				return Boolean.TRUE;
			}, outer));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		return pageResult;
	}

	public static ArrayList<OCRResultPix> ocrPixa3(Pixa pixArray, ExecutorService outer, int debugL) throws Exception {

		int noOfImages = Leptonica1.pixaGetCount(pixArray);
		final ArrayList<OCRResultPix> pageResult = new ArrayList<OCRResultPix>(noOfImages);
		ArrayList<CompletableFuture<Boolean>> cfs = new ArrayList<CompletableFuture<Boolean>>(noOfImages);
		for (int i = 0; i < noOfImages; ++i) {
			final Pix pix = Leptonica1.pixaGetPix(pixArray, i, ILeptonica.L_COPY);
			final int imageNumber = i;
			cfs.add(CompletableFuture.supplyAsync(() -> {
				if (debugL <= 5) {
					System.out.println("Starting processing of " + imageNumber);
				}
				final OCRResultPix lineResult = new OCRResultPix();
				pageResult.add(lineResult);
				TechWerxTesseractHandle instanceHandle = null;

				try {
					instanceHandle = (TechWerxTesseractHandle) techwerxTesseractPool.borrowObject();
				} catch (Exception e) {
					e.printStackTrace();
					try {
						techwerxTesseractPool.returnObject(instanceHandle);
					} catch (Exception e1) {
						System.out.println(
								"Exception encountered while returning object instanceHandle to tesseract pool - "
										+ instanceHandle);
					}
					return Boolean.FALSE;
				}
				TechWerxTesseract instance = instanceHandle.getHandle();

				IntBuffer xRes = IntBuffer.allocate(1);
				IntBuffer yRes = IntBuffer.allocate(1);
				Leptonica1.pixGetResolution(pix, xRes, yRes);
				boolean success = true;
				String result = null;
				try {
					BufferedImage image = SBImageUtils.convertPixToImage(pix, debugL);
					result = instance.doOCR(image);
				} catch (Exception e) {
					success = false;
				}
				if (success) {
					lineResult.add(result, 0.0f);
				}
				LeptUtils.dispose(pix);
				techwerxTesseractPool.returnObject(instanceHandle);
				return Boolean.TRUE;
			}, outer));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		return pageResult;
	}

	public static ArrayList<OCRResultBI> ocrBufferedImageArray(ArrayList<BufferedImage> biArray, ExecutorService outer,
			int debugL) throws Exception {

		int noOfImages = biArray.size();
		final ArrayList<OCRResultBI> pageResult = new ArrayList<OCRResultBI>(noOfImages);
		ArrayList<CompletableFuture<Boolean>> cfs = new ArrayList<CompletableFuture<Boolean>>(noOfImages);
		for (int i = 0; i < noOfImages; ++i) {
			final BufferedImage image = biArray.get(i);
			final int imageNumber = i;
			cfs.add(CompletableFuture.supplyAsync(() -> {
				if (debugL <= 5) {
					System.out.println("Starting processing of " + imageNumber);
				}
				final OCRResultBI lineResult = new OCRResultBI();
				pageResult.add(lineResult);

				/*
				 * TechWerxTesseractHandle instanceHandle = null; try { instanceHandle =
				 * (TechWerxTesseractHandle) techwerxTesseractPool.borrowObject(); } catch
				 * (Exception e) { e.printStackTrace(); try {
				 * techwerxTesseractPool.returnObject(instanceHandle); } catch (Exception e1) {
				 * System.out.println(
				 * "Exception encountered while returning object instanceHandle to tesseract pool - "
				 * + instanceHandle); } return Boolean.FALSE; } TechWerxTesseract
				 * processInstance = instanceHandle.getHandle();
				 */

				Tesseract instance = new Tesseract();
				instance.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
				instance.setLanguage("eng");
				instance.setTessVariable("tessedit_parallelize", "1");
				instance.setTessVariable("debug_file", "/dev/null");
				System.setProperty("jna.library.path", "");

				boolean success = true;
				String result = null;
				try {
					result = instance.doOCR(image);
				} catch (Exception e) {
					success = false;
				}
				if (success) {
					if (debugL <= 9) {
						System.out.println(result);
					}
					lineResult.add(result, 0.0f);
				}
				// techwerxTesseractPool.returnObject(instanceHandle);
				return Boolean.TRUE;
			}, outer));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		return pageResult;
	}

	public static void drawBoundingBoxesOnPix(Pix pix, ArrayList<ArrayList<Rectangle>> words, int debugPictureNo,
			int debugL, String identifier) {
		if (debugL <= 4) {

			Pix pix1 = Leptonica1.pixConvertTo32(pix);
			int lineNo = 1;
			L_Bmf font = Leptonica1.bmfCreate(null, 4);
			for (ArrayList<Rectangle> word : words) {
				for (Rectangle letterBox : word) {
					Box box = new Box(letterBox.x, letterBox.y, letterBox.width, letterBox.height, 0);
					Leptonica1.pixRenderBox(pix1, box, 1, ILeptonica.L_FLIP_PIXELS);
					Leptonica1.pixSetTextline(pix1, font, "" + lineNo, 0xFF000000, letterBox.x, letterBox.y - 1, null,
							null);
					// LeptUtils.dispose(box); Not required as the box is created on the VM heap !!
				}
				++lineNo;
			}
			Leptonica1.pixSetTextline(pix1, font, identifier, 0xFF000000, 3, 10, null, null);
			Leptonica1.pixWrite(SBImageUtils.baseTestDir + "symbolBoxes-" + debugPictureNo + ".png", pix1,
					ILeptonica.IFF_PNG);
			LeptUtils.dispose(font);
			LeptUtils.dispose(pix1);
			// System.gc();
		}
	}

	public static DimensionScaling getScalingFactors(Rectangle[] boxes, int debugL) {

		if (debugL <= 2) {
			System.out.println(boxes);
		}
		DescriptiveStatistics widthStats = new DescriptiveStatistics();
		DescriptiveStatistics heightStats = new DescriptiveStatistics();
		for (Rectangle box : boxes) {
			heightStats.addValue(box.height);
		}

		double medianHeight = heightStats.getPercentile(50);
		double averageHeight = heightStats.getMean();
		double height = Math.min(medianHeight, averageHeight);

		// remove the boxes that are small in height - eliminates .-_,

		double heightCutOff = 0.4;
		ArrayList<Rectangle> curatedBoxes = new ArrayList<>();
		for (Rectangle box : boxes) {
			if (box.height > (height * heightCutOff)) {
				curatedBoxes.add(box);
			}
		}

		// re-populate and recalculate the stats
		heightStats.clear();
		for (Rectangle box : curatedBoxes) {
			heightStats.addValue(box.height);
			widthStats.addValue(box.width);
		}

		int noOfBoxes = curatedBoxes.size();

		double medianWidth = widthStats.getPercentile(50);
		medianHeight = heightStats.getPercentile(50);

		double averageWidth = widthStats.getMean();
		averageHeight = heightStats.getMean();

		double width = Math.min(medianWidth, averageWidth);
		height = Math.min(medianHeight, averageHeight);

		double heightScaling = idealCharHeight / height;
		double widthScaling = idealCharWidth / width;

		// if (((medianHeight / averageHeight) < 1.2) || ((medianHeight / averageHeight)
		// > 0.8)) {

		// If no of boxes <=5, there's too little data
		// Take the median height and average height to figure out the scaling factors
		int noOfDataPoints = 5;
		if (noOfBoxes < noOfDataPoints) {
			return new DimensionScaling(heightScaling, widthScaling);
		}

		double[] widths = widthStats.getValues();
		Arrays.sort(widths);

		/*
		 * double[] widthsNormalised = new double[widths.length]; for (int i = 0; i <
		 * widths.length; ++i) { widthsNormalised[i] = widths[i] / widths[0]; }
		 *
		 * int index = 0; width = widths[0]; double multiple = 1.0; for (int i = 0; i <
		 * widths.length; ++i) { if ((widthsNormalised[i] >= 1.3) &&
		 * (widthsNormalised[i] <= 1.5)) { index = i; } if ((widthsNormalised[i] >= 2.3)
		 * && (widthsNormalised[i] <= 2.5)) { multiple = 1.4; } if ((widthsNormalised[i]
		 * >= 2.6) && (widthsNormalised[i] <= 2.9)) { multiple = 1.4; } if
		 * ((widthsNormalised[i] >= 3.9) && (widthsNormalised[i] <= 4.4)) { multiple =
		 * 1.4; } }
		 *
		 * if (index != 0) { width = widths[index]; } else { if (multiple == 1.4) {
		 * width = widths[0] * 1.4; } }
		 *
		 * System.out.println(Arrays.toString(widths));
		 * System.out.println(Arrays.toString(widthsNormalised));
		 * System.out.println("width = " + width + " ; index = " + index +
		 * " ; multiple = " + multiple);
		 */

		// If no of boxes >=6, take the average height of first 6 to figure out the
		// scaling factors
		// Then, take the closest width, after checking either side of the index, to the
		// average from the widths array
		// If widthScaling is > 1.15 and the ratio of widthScaling to heightScaling is
		// greater than 1.25, then repeat

		int loopNo = 1;
		boolean lastLoop = false;
		do {
			double multiplicationFactor = (loopNo == 1) ? 0 : ((loopNo == 2) ? 1 : (loopNo * 0.5));
			int startingIndex = Math.min(widths.length - noOfDataPoints, (int) (multiplicationFactor * noOfDataPoints));
			if (startingIndex == (widths.length - noOfDataPoints)) {
				lastLoop = true;
			}
			double tot = 0.0;
			for (int i = startingIndex; i < (startingIndex + noOfDataPoints); ++i) {
				tot += widths[i];
			}

			int index = startingIndex;
			for (int i = startingIndex; i < (startingIndex + noOfDataPoints); ++i) {
				if (widths[i] <= (tot / noOfDataPoints)) {
					index = i;
				} else {
					break;
				}
			}

			if (index < (widths.length - 1)) {
				if (Math.abs(widths[index] - (tot / noOfDataPoints)) < Math
						.abs(widths[index + 1] - (tot / noOfDataPoints))) {
					width = widths[index];
				} else {
					width = widths[index + 1];
				}
			} else {
				width = widths[index];
			}
			heightScaling = (idealCharHeight * 1.0) / height;
			widthScaling = (idealCharWidth * 1.0) / width;
			++loopNo;

		} while ((widthScaling > 1.25) && ((widthScaling / heightScaling) > 1.5) && (!lastLoop));

		if (debugL <= 4) {
			System.out.println(Arrays.toString(widths));
			// System.out.println("width = " + width + " ; average = " + (tot / 6));
			System.out.println("width = " + width);
		}

		// double expectedWidth = height * 0.7;
		// double ratio = width / expectedWidth;
		// if (ratio > 1.25) {
		// width = expectedWidth;
		// }

		heightScaling = (idealCharHeight * 1.0) / height;
		widthScaling = (idealCharWidth * 1.0) / width;

		// System.out.println("widthScaling = " + widthScaling + " ; heightScaling = " +
		// heightScaling);

		return new DimensionScaling(heightScaling, widthScaling);

	}

	public static int getHCF(int a, int b) {
		if ((a == 0) && (b == 0)) {
			return 0;
		}

		// if (a == 0) {
		// return b;
		// }
		// if (b == 0) {
		// return a;
		// }

		// if ((a == 1) || (b == 1)) {
		// return 1;
		// }
		if (a < b) {
			return getHCF(b, a);
		}
		// if (b % a == 0) {
		// return a;
		// }
		// return getHCF(a, b % a);

		while (b > 0) {
			int temp = b;
			b = a % b;
			a = temp;
		}
		return a;
	}

	public static int getHCF(int[] input) {
		int result = input[0];
		for (int i = 1; i < input.length; ++i) {
			result = getHCF(result, input[i]);
		}
		return result;
	}

	public static SBImage seed(SBImage original, SBImage dilated) {
		if ((original.width != dilated.width) || (original.height != dilated.height)) {
			return original;
		}
		int[][] out = new int[original.height][original.width];
		for (int i = 0; i < original.height; ++i) {
			for (int j = 0; j < original.width; ++j) {
				if (original.pixels[i][j] != dilated.pixels[i][j]) {
					out[i][j] = 255;
				} else {
					out[i][j] = original.pixels[i][j];
				}
			}
		}
		SBImage result = null;
		try {
			result = new SBImage(out);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;

	}

	public static void cleanWithKernel(SBImage input, int kHeight, int kWidth) {
		int height = input.height;
		int width = input.width;
		// int loopNo = 0;
		// System.out.println("Entering cleanWithKernel() with height = " + kHeight + "
		// and width = " + kWidth);
		for (int j = kHeight / 2; j < (height - (kHeight / 2)); ++j) {
			for (int i = kWidth / 2; i < (width - (kWidth / 2)); ++i) {
				if (checkBoundary(input, kHeight, kWidth, j, i)) {
					whitenPixels(input, kHeight, kWidth, j, i);
				}
			}
		}
		// System.out.println("Done cleanWithKernel() with height = " + kHeight + " and
		// width = " + kWidth);
	}

	private static boolean checkBoundary(SBImage input, int kHeight, int kWidth, int y, int x) {

		for (int i = x - (kWidth / 2); i <= (x + (kWidth / 2)); ++i) {
			if (input.pixels[y - (kHeight / 2)][i] < 255) {
				return false;
			}
		}
		for (int i = x - (kWidth / 2); i <= (x + (kWidth / 2)); ++i) {
			if (input.pixels[y + (kHeight / 2)][i] < 255) {
				return false;
			}
		}
		for (int j = (y - (kHeight / 2)) + 1; j <= ((y + (kHeight / 2)) - 1); ++j) {
			if (input.pixels[j][x - (kWidth / 2)] < 255) {
				return false;
			}
		}
		for (int j = (y - (kHeight / 2)) + 1; j <= ((y + (kHeight / 2)) - 1); ++j) {
			if (input.pixels[j][x + (kWidth / 2)] < 255) {
				return false;
			}
		}
		return true;
	}

	private static void whitenPixels(SBImage input, int kHeight, int kWidth, int y, int x) {

		for (int j = (y - (kHeight / 2)) + 1; j <= ((y + (kHeight / 2)) - 1); ++j) {
			for (int i = (x - (kWidth / 2)) + 1; i <= ((x + (kWidth / 2)) - 1); ++i) {
				input.pixels[j][i] = 255;
			}
		}
	}

	public static void cleanBorder(SBImage input, int kHeight, int kWidth) {
		// cleans the border, plus 1 additional pixel width all across
		for (int j = 0; j <= (kHeight / 2); ++j) {
			for (int i = 0; i < input.width; ++i) {
				input.pixels[j][i] = 255;
			}
		}
		for (int j = input.height - 1; j >= (input.height - (kHeight / 2) - 1); --j) {
			for (int i = 0; i < input.width; ++i) {
				input.pixels[j][i] = 255;
			}
		}
		for (int j = (kHeight / 2) + 1; j < (input.height - (kHeight / 2) - 1); ++j) {
			for (int i = 0; i <= (kWidth / 2); ++i) {
				input.pixels[j][i] = 255;
			}
		}
		for (int j = (kHeight / 2) + 1; j < (input.height - (kHeight / 2) - 1); ++j) {
			for (int i = input.width - 1; i >= (input.width - (kWidth / 2) - 1); --i) {
				input.pixels[j][i] = 255;
			}
		}
	}

	public static SBImage cleanWithSmallKernels(SBImage seed) {
		SBImage input = SBImageUtils.clone(seed);
		SBImageUtils.cleanBorder(input, 7, 7);
		SBImageUtils.cleanWithKernel(input, 3, 3);
		SBImageUtils.cleanWithKernel(input, 5, 3);
		SBImageUtils.cleanWithKernel(input, 3, 5);
		SBImageUtils.cleanWithKernel(input, 5, 5);
		SBImageUtils.cleanWithKernel(input, 5, 7);
		SBImageUtils.cleanWithKernel(input, 7, 5);
		SBImageUtils.cleanWithKernel(input, 7, 7);
		SBImageUtils.cleanWithKernel(input, 9, 9);
		SBImageUtils.cleanWithKernel(input, 9, 7);
		SBImageUtils.cleanWithKernel(input, 7, 9);
		SBImageUtils.cleanWithKernel(input, 9, 9);
		SBImageUtils.cleanWithKernel(input, 3, 3);
		SBImageUtils.cleanWithKernel(input, 5, 5);
		SBImageUtils.cleanWithKernel(input, 7, 7);
		SBImageUtils.cleanWithKernel(input, 9, 9);
		SBImageUtils.cleanWithKernel(input, 11, 11);
		SBImageUtils.cleanWithKernel(input, 13, 13);
		SBImageUtils.cleanWithKernel(input, 13, 15);
		SBImageUtils.cleanWithKernel(input, 15, 13);
		SBImageUtils.cleanWithKernel(input, 15, 17);
		SBImageUtils.cleanWithKernel(input, 17, 15);
		SBImageUtils.cleanWithKernel(input, 19, 17);
		SBImageUtils.cleanWithKernel(input, 17, 19);
		SBImageUtils.cleanWithKernel(input, 19, 19);
		return input;
	}

	public static SBImage cleanWithSmallAndMediumKernels(SBImage seed) {
		SBImage input = SBImageUtils.clone(seed);
		SBImageUtils.cleanWithKernel(input, 3, 3);
		SBImageUtils.cleanWithKernel(input, 5, 3);
		SBImageUtils.cleanWithKernel(input, 3, 5);
		SBImageUtils.cleanWithKernel(input, 5, 5);
		SBImageUtils.cleanWithKernel(input, 5, 7);
		SBImageUtils.cleanWithKernel(input, 7, 5);
		SBImageUtils.cleanWithKernel(input, 7, 7);
		SBImageUtils.cleanWithKernel(input, 9, 9);
		SBImageUtils.cleanWithKernel(input, 9, 7);
		SBImageUtils.cleanWithKernel(input, 7, 9);
		SBImageUtils.cleanWithKernel(input, 9, 9);
		SBImageUtils.cleanWithKernel(input, 3, 3);
		SBImageUtils.cleanWithKernel(input, 5, 5);
		SBImageUtils.cleanWithKernel(input, 7, 7);
		SBImageUtils.cleanWithKernel(input, 9, 9);
		SBImageUtils.cleanWithKernel(input, 11, 11);
		SBImageUtils.cleanWithKernel(input, 13, 13);
		SBImageUtils.cleanWithKernel(input, 15, 15);
		SBImageUtils.cleanWithKernel(input, 17, 17);
		SBImageUtils.cleanWithKernel(input, 19, 19);
		SBImageUtils.cleanWithKernel(input, 21, 21);
		SBImageUtils.cleanWithKernel(input, 23, 23);
		SBImageUtils.cleanWithKernel(input, 25, 25);
		SBImageUtils.cleanWithKernel(input, 27, 27);
		SBImageUtils.cleanWithKernel(input, 29, 29);
		SBImageUtils.cleanWithKernel(input, 31, 31);
		SBImageUtils.cleanWithKernel(input, 33, 33);
		SBImageUtils.cleanWithKernel(input, 35, 35);
		SBImageUtils.cleanWithKernel(input, 37, 37);
		SBImageUtils.cleanWithKernel(input, 29, 37);
		SBImageUtils.cleanWithKernel(input, 37, 29);
		SBImageUtils.cleanWithKernel(input, 39, 39);
		SBImageUtils.cleanWithKernel(input, 43, 43);
		SBImageUtils.cleanWithKernel(input, 47, 47);
		SBImageUtils.cleanWithKernel(input, 51, 51);
		return input;
	}

	public static SBImage cleanSBImageSmallKernels(SBImage seed) {
		SBImage input = SBImageUtils.clone(seed);
		SBImageUtils.cleanBorder(input, 7, 7);
		SBImageUtils.cleanWithKernel(input, 3, 3);
		SBImageUtils.cleanWithKernel(input, 5, 3);
		SBImageUtils.cleanWithKernel(input, 3, 5);
		SBImageUtils.cleanWithKernel(input, 5, 5);
		SBImageUtils.cleanWithKernel(input, 5, 7);
		SBImageUtils.cleanWithKernel(input, 7, 5);
		SBImageUtils.cleanWithKernel(input, 7, 7);
		SBImageUtils.cleanWithKernel(input, 3, 3);
		SBImageUtils.cleanWithKernel(input, 5, 3);
		SBImageUtils.cleanWithKernel(input, 3, 5);
		SBImageUtils.cleanWithKernel(input, 5, 5);
		SBImageUtils.cleanWithKernel(input, 5, 7);
		SBImageUtils.cleanWithKernel(input, 7, 5);
		SBImageUtils.cleanWithKernel(input, 7, 7);
		input = SBImageUtils.dilate(input, 5, 5);
		SBImageUtils.cleanWithKernel(input, 9, 9);
		SBImageUtils.cleanWithKernel(input, 9, 7);
		SBImageUtils.cleanWithKernel(input, 7, 9);
		SBImageUtils.cleanWithKernel(input, 9, 9);
		SBImageUtils.cleanWithKernel(input, 3, 3);
		SBImageUtils.cleanWithKernel(input, 5, 5);
		SBImageUtils.cleanWithKernel(input, 7, 7);
		SBImageUtils.cleanWithKernel(input, 9, 9);
		SBImageUtils.cleanWithKernel(input, 9, 7);
		SBImageUtils.cleanWithKernel(input, 7, 9);
		SBImageUtils.cleanWithKernel(input, 9, 9);
		SBImageUtils.cleanWithKernel(input, 9, 9);
		SBImageUtils.cleanWithKernel(input, 11, 11);
		SBImageUtils.cleanWithKernel(input, 13, 13);
		SBImageUtils.cleanWithKernel(input, 13, 15);
		SBImageUtils.cleanWithKernel(input, 15, 13);
		SBImageUtils.cleanWithKernel(input, 15, 17);
		SBImageUtils.cleanWithKernel(input, 17, 15);
		SBImageUtils.cleanWithKernel(input, 19, 17);
		SBImageUtils.cleanWithKernel(input, 17, 19);
		SBImageUtils.cleanWithKernel(input, 19, 19);
		input = SBImageUtils.dilate(input, 3, 3);
		SBImageUtils.cleanWithKernel(input, 19, 21);
		SBImageUtils.cleanWithKernel(input, 21, 19);
		SBImageUtils.cleanWithKernel(input, 21, 21);
		SBImageUtils.cleanWithKernel(input, 19, 23);
		SBImageUtils.cleanWithKernel(input, 23, 19);
		SBImageUtils.cleanWithKernel(input, 23, 23);
		input = SBImageUtils.erode(input, 3, 3);
		input = SBImageUtils.erode(input, 5, 5);
		SBImageUtils.cleanWithKernel(input, 5, 5);
		SBImageUtils.cleanWithKernel(input, 5, 7);
		SBImageUtils.cleanWithKernel(input, 7, 5);
		SBImageUtils.cleanWithKernel(input, 7, 7);
		SBImageUtils.cleanWithKernel(input, 3, 3);
		SBImageUtils.cleanWithKernel(input, 5, 3);
		SBImageUtils.cleanWithKernel(input, 3, 5);
		SBImageUtils.cleanWithKernel(input, 5, 5);
		SBImageUtils.cleanWithKernel(input, 5, 7);
		SBImageUtils.cleanWithKernel(input, 7, 5);
		SBImageUtils.cleanWithKernel(input, 7, 7);
		SBImageUtils.cleanWithKernel(input, 9, 7);
		SBImageUtils.cleanWithKernel(input, 7, 9);
		SBImageUtils.cleanWithKernel(input, 9, 9);
		return input;
	}

	public static SBImage cleanSBImageMediumKernels(SBImage seed) {
		SBImage input = SBImageUtils.clone(seed);
		SBImageUtils.cleanWithKernel(input, 11, 11);
		SBImageUtils.cleanWithKernel(input, 13, 13);
		SBImageUtils.cleanWithKernel(input, 15, 15);
		SBImageUtils.cleanWithKernel(input, 17, 17);
		SBImageUtils.cleanWithKernel(input, 19, 19);
		input = SBImageUtils.dilate(input, 5, 5);
		SBImageUtils.cleanWithKernel(input, 21, 21);
		SBImageUtils.cleanWithKernel(input, 17, 21);
		SBImageUtils.cleanWithKernel(input, 21, 17);
		SBImageUtils.cleanWithKernel(input, 23, 23);
		SBImageUtils.cleanWithKernel(input, 23, 19);
		SBImageUtils.cleanWithKernel(input, 19, 23);
		SBImageUtils.cleanWithKernel(input, 25, 25);
		SBImageUtils.cleanWithKernel(input, 27, 27);
		SBImageUtils.cleanWithKernel(input, 27, 23);
		SBImageUtils.cleanWithKernel(input, 23, 27);
		SBImageUtils.cleanWithKernel(input, 29, 29);
		SBImageUtils.cleanWithKernel(input, 31, 31);
		SBImageUtils.cleanWithKernel(input, 33, 33);
		SBImageUtils.cleanWithKernel(input, 33, 29);
		SBImageUtils.cleanWithKernel(input, 29, 33);
		SBImageUtils.cleanWithKernel(input, 35, 35);
		SBImageUtils.cleanWithKernel(input, 37, 37);
		SBImageUtils.cleanWithKernel(input, 29, 37);
		SBImageUtils.cleanWithKernel(input, 37, 29);
		SBImageUtils.cleanWithKernel(input, 39, 39);
		SBImageUtils.cleanWithKernel(input, 43, 43);
		SBImageUtils.cleanWithKernel(input, 47, 47);
		SBImageUtils.cleanWithKernel(input, 51, 51);
		input = SBImageUtils.erode(input, 5, 5);
		return input;
	}

	public static SBImage cleanSBImageLargeKernels(SBImage seed) {
		SBImage input = SBImageUtils.clone(seed);
		input = SBImageUtils.dilate(input, 5, 5);
		SBImageUtils.cleanWithKernel(input, 51, 51);
		SBImageUtils.cleanWithKernel(input, 91, 91);
		SBImageUtils.cleanWithKernel(input, 25, 41);
		SBImageUtils.cleanWithKernel(input, 41, 25);
		SBImageUtils.cleanWithKernel(input, 35, 91);
		SBImageUtils.cleanWithKernel(input, 91, 35);
		SBImageUtils.cleanWithKernel(input, 49, 97);
		SBImageUtils.cleanWithKernel(input, 97, 49);
		SBImageUtils.cleanWithKernel(input, 71, 91);
		input = SBImageUtils.erode(input, 5, 5);
		return input;
	}

	public static Pix removeLines(Pix pix) {
		Pix pixs = null;
		if (Leptonica1.pixGetDepth(pix) == 1) {
			pixs = Leptonica1.pixCopy(null, pix);
		} else {
			pixs = Leptonica1.pixConvertTo1(pix, 128);
		}
		FloatBuffer angle = FloatBuffer.allocate(1);
		FloatBuffer conf = FloatBuffer.allocate(1);
		Leptonica1.pixFindSkew(pix, angle, conf);
		float angleInDegrees = angle.get();
		Pix rotated = null;
		if (angleInDegrees != 0) {
			rotated = Leptonica1.pixRotate(pixs, angleInDegrees, ILeptonica.L_ROTATE_AREA_MAP,
					ILeptonica.L_BRING_IN_WHITE, 0, 0);
		} else {
			rotated = Leptonica1.pixCopy(null, pixs);
		}

		Pix opened = Leptonica1.pixOpenCompBrick(null, rotated, 70, 1);
		Pix seedfill = Leptonica1.pixSeedfillBinary(null, opened, rotated, 4);
		Pix pre_result = Leptonica1.pixSubtract(null, rotated, seedfill);
		Pix result = Leptonica1.pixRotate(pre_result, -angleInDegrees, ILeptonica.L_ROTATE_AREA_MAP,
				ILeptonica.L_BRING_IN_WHITE, 0, 0);

		LeptUtils.dispose(pixs);
		LeptUtils.dispose(rotated);
		LeptUtils.dispose(opened);
		LeptUtils.dispose(seedfill);
		LeptUtils.dispose(pre_result);

		return result;
	}

	public static SBImage threshold(SBImage input, int threshold) {
		int[][] out = new int[input.height][input.width];
		for (int y = 0; y < input.pixels.length; ++y) {
			for (int x = 0; x < input.pixels[0].length; ++x) {
				if (input.pixels[y][x] < threshold) {
					out[y][x] = 0;
				} else {
					out[y][x] = 255;
				}
			}
		}
		SBImage result = null;
		try {
			result = new SBImage(out);
		} catch (Exception e) {
			System.out.println(e);
		}
		return result;
	}

	public static SBImage and(SBImage input1, SBImage input2, int tolerance) {
		// tolerance is useful for gray images, not needed for binary images
		if ((input1.height != input2.height) || (input1.width != input2.width)) {
			return null;
		}
		int[][] out = new int[input1.height][input1.width];
		for (int y = 0; y < input1.pixels.length; ++y) {
			for (int x = 0; x < input1.pixels[0].length; ++x) {
				if (Math.abs(input1.pixels[y][x] - input2.pixels[y][x]) <= tolerance) {
					out[y][x] = input1.pixels[y][x];
				} else {
					out[y][x] = 255;
				}
			}
		}
		SBImage result = null;
		try {
			result = new SBImage(out);
		} catch (Exception e) {
			System.out.println(e);
		}
		return result;
	}

	public static SBImage erode22(SBImage input) {
		int[][] out = new int[input.height][input.width];
		for (int y = 0; y < input.pixels.length; ++y) {
			for (int x = 0; x < input.pixels[0].length; ++x) {
				out[y][x] = input.pixels[y][x];
			}
		}
		for (int y = 1; y < (input.pixels.length - 1); ++y) {
			for (int x = 1; x < (input.pixels[0].length - 1); ++x) {
				if (input.pixels[y][x] == 255) {
					out[y - 1][x] = 255;
					out[y + 1][x] = 255;
					out[y][x + 1] = 255;
					out[y][x - 1] = 255;
				}
			}
		}
		SBImage result = null;
		try {
			result = new SBImage(out);
		} catch (Exception e) {
			System.out.println(e);
		}
		return result;
	}

	public static SBImage dilate22(SBImage input) {
		int[][] out = new int[input.height][input.width];
		for (int y = 0; y < input.pixels.length; ++y) {
			for (int x = 0; x < input.pixels[0].length; ++x) {
				out[y][x] = input.pixels[y][x];
			}
		}
		for (int y = 1; y < (input.pixels.length - 1); ++y) {
			for (int x = 1; x < (input.pixels[0].length - 1); ++x) {
				if (input.pixels[y][x] == 0) {
					out[y - 1][x] = 0;
					out[y + 1][x] = 0;
					out[y][x + 1] = 0;
					out[y][x - 1] = 0;
				}
			}
		}
		SBImage result = null;
		try {
			result = new SBImage(out);
		} catch (Exception e) {
			System.out.println(e);
		}
		return result;
	}

	public static int overlapArea(Rectangle rect1, Rectangle rect2) {
		int x_overlap = Math.max(0,
				Math.min(rect1.x + rect1.width, rect2.x + rect2.width) - Math.max(rect1.x, rect2.x));
		int y_overlap = Math.max(0,
				Math.min(rect1.y + rect1.height, rect2.y + rect2.height) - Math.max(rect1.y, rect2.y));
		return x_overlap * y_overlap;
	}

	/**
	 * Converts Leptonica <code>Pix</code> to <code>BufferedImage</code>.
	 *
	 * @param pix source pix
	 * @return BufferedImage output image
	 * @throws IOException
	 */
	public static BufferedImage convertPixToImage(Pix pix, int debugL) throws IOException {
		PointerByReference pdata = new PointerByReference();
		NativeSizeByReference psize = new NativeSizeByReference();
		int format = IFF_TIFF;
		Leptonica1.pixWriteMem(pdata, psize, pix, format);
		// Leptonica1.pixWriteMemPng(pdata, psize, pix, 0.0f);
		if (debugL <= 2) {
			System.out.println("pix = " + pix);
			System.out.println("pix size = " + pix.size());
			System.out.println("pdata in PixToImage = " + pdata);
			System.out.println("psize in PixToImage = " + psize);
			System.out.println("psize.getValue() in PixToImage = " + psize.getValue());
			System.out.println("psize.getValue().intValue() in PixToImage = " + psize.getValue().intValue());
		}
		byte[] b = pdata.getValue().getByteArray(0, psize.getValue().intValue());
		InputStream in = new ByteArrayInputStream(b);
		BufferedImage bi = ImageIO.read(in);
		in.close();
		Leptonica1.lept_free(pdata.getValue());
		return bi;
	}

	public static boolean writeFile(Pix pix, String formatName, String localOutputFile) throws Exception {
		return writeFile(pix, formatName, localOutputFile, 300, 0.5f);
	}

	public static boolean writeFile(Pix pix, String formatName, String localOutputFile, int dpi) throws Exception {
		return writeFile(pix, formatName, localOutputFile, dpi, 0.5f);
	}

	public static boolean writeFile(Pix pix, String formatName, String localOutputFile, int dpi,
			float compressionQuality) throws Exception {
		if (pix == null) {
			return false;
		}
		return writeFile(LeptUtils.convertPixToImage(pix), formatName, localOutputFile, dpi, compressionQuality);
	}

	public static boolean writeFile(SBImage sbid, String formatName, String localOutputFile) throws Exception {
		return writeFile(SBImage.getBufferedImageFromSBImage(sbid), formatName, localOutputFile, 300, 0.5f);
	}

	public static boolean writeFile(SBImage sbid, String formatName, String localOutputFile, int dpi) throws Exception {
		return writeFile(SBImage.getBufferedImageFromSBImage(sbid), formatName, localOutputFile, dpi, 0.5f);
	}

	public static boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile)
			throws Exception {
		return writeFile(bufferedImage, formatName, localOutputFile, 300);
	}

	public static boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile, int dpi)
			throws Exception {
		return writeFile(bufferedImage, formatName, localOutputFile, dpi, 0.5f);
	}

	public static boolean writeFile(SBImage image, String formatName, String localOutputFile, int dpi,
			float compressionQuality) throws Exception {

		return writeFile(SBImage.getBufferedImageFromSBImage(image), formatName, localOutputFile, dpi,
				compressionQuality);
	}

	public static boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile, int dpi,
			float compressionQuality) throws Exception {

		if (bufferedImage == null) {
			return false;
		}
		RenderedImage[] input = new RenderedImage[1];
		input[0] = bufferedImage;
		return writeFile(input, formatName, localOutputFile, dpi, compressionQuality);
	}

	public static boolean writeFile(SBImage[] images, String formatName, String localOutputFile, int dpi)
			throws Exception {
		if (images == null) {
			return false;
		}
		BufferedImage[] ims = new BufferedImage[images.length];
		int i = 0;
		for (SBImage image : images) {
			ims[i++] = SBImage.getBufferedImageFromSBImage(image);
		}
		return writeFile(ims, formatName, localOutputFile, dpi, 0.5f);
	}

	public static boolean writeFile(RenderedImage[] images, String formatName, String localOutputFile, int dpi)
			throws Exception {
		return writeFile(images, formatName, localOutputFile, dpi, 0.5f);
	}

	public static boolean writeFile(SBImage[] images, String formatName, String localOutputFile, int dpi,
			float compressionQuality) throws Exception {
		if (images == null) {
			return false;
		}
		BufferedImage[] ims = new BufferedImage[images.length];
		int i = 0;
		for (SBImage sbid : images) {
			ims[i++] = SBImage.getBufferedImageFromSBImage(sbid);
		}
		return writeFile(ims, formatName, localOutputFile, dpi, compressionQuality);
	}

	public static boolean writeFile(RenderedImage[] images, String formatName, String localOutputFile, int dpi,
			float compressionQuality) throws Exception {

		if (images == null) {
			throw new IllegalArgumentException("No images available for writing to : " + formatName + " file");
		}
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);

		if (!writers.hasNext()) {
			throw new IllegalArgumentException("No writer available for: " + formatName + " files");
		}

		IIOImage temp = null;
		ImageTypeSpecifier its = null;
		IIOMetadata md = null;
		ImageWriter writer = null;
		ImageWriteParam writeParam = null;
		ImageOutputStream output = null;
		its = ImageTypeSpecifier.createFromRenderedImage(images[0]);
		boolean writerFound = false;

		try {
			// Loop until we get the best driver, i.e. one that supports
			// setting dpi in the standard metadata format; however we'd also
			// accept a driver that can't, if a better one can't be found
			for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName(formatName); iw.hasNext();) {
				if (writer != null) {
					writer.dispose();
				}
				writer = iw.next();
				if (writer == null) {
					continue;
				}
				writeParam = writer.getDefaultWriteParam();
				md = writer.getDefaultImageMetadata(its, writeParam);
				if (md == null) {
					continue;
				}
				if (md.isReadOnly() || !md.isStandardMetadataFormatSupported()) {
					writerFound = false;
				} else {
					writerFound = true;
					break;
				}
			}

			if (!writerFound) {
				StringBuilder sb = new StringBuilder();
				String[] writerFormatNames = ImageIO.getWriterFormatNames();
				for (String fmt : writerFormatNames) {
					sb.append(fmt);
					sb.append(' ');
				}
				throw new IllegalArgumentException("No suitable writer found. Metadata of all writers for : "
						+ formatName
						+ " files are either Read-Only or don't support standard metadata format. Supported formats are : "
						+ sb);
			}

			try {

				// compression
				if ((writeParam != null) && writeParam.canWriteCompressed()) {
					writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					if (formatName.toLowerCase().startsWith("tif")) {
						writeParam.setCompressionType("LZW"); // org.apache.pdfbox.filter.TIFFExtension.COMPRESSION_LZW
						writeParam.setCompressionQuality(compressionQuality);
					}
				}

				if (formatName.toLowerCase().startsWith("tif")) {
					// TIFF metadata
					// Convert default metadata to TIFF metadata
					TIFFDirectory dir = TIFFDirectory.createFromMetadata(md);

					// Get {X,Y} resolution tags
					BaselineTIFFTagSet base = BaselineTIFFTagSet.getInstance();
					TIFFTag tagXRes = base.getTag(BaselineTIFFTagSet.TAG_X_RESOLUTION);
					TIFFTag tagYRes = base.getTag(BaselineTIFFTagSet.TAG_Y_RESOLUTION);

					// Create {X,Y} resolution fields
					TIFFField resolution = new TIFFField(base.getTag(BaselineTIFFTagSet.TAG_RESOLUTION_UNIT),
							BaselineTIFFTagSet.RESOLUTION_UNIT_INCH);
					TIFFField fieldXRes = new TIFFField(tagXRes, TIFFTag.TIFF_RATIONAL, 1, new long[][] { { dpi, 1 } });
					TIFFField fieldYRes = new TIFFField(tagYRes, TIFFTag.TIFF_RATIONAL, 1, new long[][] { { dpi, 1 } });

					// Add {X,Y} resolution fields to TIFFDirectory
					dir.addTIFFField(resolution);
					dir.addTIFFField(fieldXRes);
					dir.addTIFFField(fieldYRes);

					// Add unit field to TIFFDirectory (change to RESOLUTION_UNIT_CENTIMETER if
					// necessary)
					dir.addTIFFField(new TIFFField(base.getTag(BaselineTIFFTagSet.TAG_RESOLUTION_UNIT),
							BaselineTIFFTagSet.RESOLUTION_UNIT_INCH));

					// assign the new dir as the new IIOImageMetadata
					md = dir.getAsMetadata();
				} else if ("jpeg".equals(formatName.toLowerCase()) || "jpg".equals(formatName.toLowerCase())) {
					// This segment must be run before other meta operations,
					// or else "IIOInvalidTreeException: Invalid node: app0JFIF"
					// The other (general) "meta" methods may not be used, because
					// this will break the reading of the meta data in tests
					Element root = (Element) md.getAsTree("javax_imageio_jpeg_image_1.0");
					NodeList jvarNodeList = root.getElementsByTagName("JPEGvariety");
					Element jvarChild;
					if (jvarNodeList.getLength() == 0) {
						jvarChild = new IIOMetadataNode("JPEGvariety");
						root.appendChild(jvarChild);
					} else {
						jvarChild = (Element) jvarNodeList.item(0);
					}

					NodeList jfifNodeList = jvarChild.getElementsByTagName("app0JFIF");
					Element jfifChild;
					if (jfifNodeList.getLength() == 0) {
						jfifChild = new IIOMetadataNode("app0JFIF");
						jvarChild.appendChild(jfifChild);
					} else {
						jfifChild = (Element) jfifNodeList.item(0);
					}
					if (jfifChild.getAttribute("majorVersion").isEmpty()) {
						jfifChild.setAttribute("majorVersion", "1");
					}
					if (jfifChild.getAttribute("minorVersion").isEmpty()) {
						jfifChild.setAttribute("minorVersion", "2");
					}
					jfifChild.setAttribute("resUnits", "1"); // inch
					jfifChild.setAttribute("Xdensity", Integer.toString(dpi));
					jfifChild.setAttribute("Ydensity", Integer.toString(dpi));
					if (jfifChild.getAttribute("thumbWidth").isEmpty()) {
						jfifChild.setAttribute("thumbWidth", "0");
					}
					if (jfifChild.getAttribute("thumbHeight").isEmpty()) {
						jfifChild.setAttribute("thumbHeight", "0");
					}

					// mergeTree doesn't work for ARGB
					md.setFromTree("javax_imageio_jpeg_image_1.0", root);

				} else {
					// write metadata is possible
					if ((md != null) && !md.isReadOnly() && md.isStandardMetadataFormatSupported()) {
						IIOMetadataNode root = (IIOMetadataNode) md.getAsTree("javax_imageio_1.0");

						IIOMetadataNode dimension = null;
						NodeList nodeList = root.getElementsByTagName("Dimension");
						if (nodeList.getLength() > 0) {
							dimension = (IIOMetadataNode) nodeList.item(0);
						} else {
							dimension = new IIOMetadataNode("Dimension");
							root.appendChild(dimension);
						}

						// PNG writer doesn't conform to the spec which is
						// "The width of a pixel, in millimeters"
						// but instead counts the pixels per millimeter
						float res = "PNG".equals(formatName.toUpperCase()) ? dpi / 25.4f : 25.4f / dpi;

						IIOMetadataNode hps = null;

						nodeList = dimension.getElementsByTagName("HorizontalPixelSize");
						if (nodeList.getLength() > 0) {
							hps = (IIOMetadataNode) nodeList.item(0);
						} else {
							hps = new IIOMetadataNode("HorizontalPixelSize");
							dimension.appendChild(hps);
						}

						hps.setAttribute("value", Double.toString(res));

						IIOMetadataNode vps = null;

						nodeList = dimension.getElementsByTagName("VerticalPixelSize");
						if (nodeList.getLength() > 0) {
							vps = (IIOMetadataNode) nodeList.item(0);
						} else {
							vps = new IIOMetadataNode("VerticalPixelSize");
							dimension.appendChild(vps);
						}

						vps.setAttribute("value", Double.toString(res));

						md.mergeTree("javax_imageio_1.0", root);
					}
				}

				// Create output stream
				output = ImageIO.createImageOutputStream(new File(localOutputFile));

				writer.setOutput(output);

				// Optionally, listen to progress, warnings, etc.

				// writeParam = writer.getDefaultWriteParam();
				if (images.length > 1) {
					writer.prepareWriteSequence(md);
				}

				// Optionally, control format specific settings of param (requires casting), or
				// control generic write settings like sub sampling, source region, output type
				// etc.

				// Optionally, provide thumbnails and image/stream metadata

				/*
				 * final String pngMetadataFormatName = "javax_imageio_1.0";
				 *
				 * // Convert dpi (dots per inch) to dots per meter final double metersToInches
				 * = 39.3701; int dotsPerMeter = (int) Math.round(dpi * metersToInches);
				 *
				 * IIOMetadataNode pHYs_node = new IIOMetadataNode("pHYs");
				 * pHYs_node.setAttribute("pixelsPerUnitXAxis", Integer.toString(dotsPerMeter));
				 * pHYs_node.setAttribute("pixelsPerUnitYAxis", Integer.toString(dotsPerMeter));
				 * pHYs_node.setAttribute("unitSpecifier", "meter");
				 *
				 * IIOMetadataNode root = new IIOMetadataNode(pngMetadataFormatName);
				 * root.appendChild(pHYs_node);
				 *
				 * md.mergeTree(pngMetadataFormatName, root);
				 */

				/*
				 * double dotsPerMilli = ((1.0 * dpi) / 10) / 2.54; IIOMetadataNode horiz = new
				 * IIOMetadataNode("HorizontalPixelSize"); horiz.setAttribute("value",
				 * Double.toString(dotsPerMilli)); IIOMetadataNode vert = new
				 * IIOMetadataNode("VerticalPixelSize"); vert.setAttribute("value",
				 * Double.toString(dotsPerMilli)); IIOMetadataNode dim = new
				 * IIOMetadataNode("Dimension"); dim.appendChild(horiz); dim.appendChild(vert);
				 * IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
				 * root.appendChild(dim); md.mergeTree("javax_imageio_1.0", root);
				 */

				// writer.prepareWriteEmpty(md, its, images[0].getWidth(),
				// images[0].getHeight(), null, null, writeParam);
				temp = new IIOImage(images[0], null, md);
				writer.write(null, temp, writeParam);
				// writer.endWriteEmpty();
				if (images.length > 1) {
					if (!writer.canInsertImage(1)) {
						throw new IllegalArgumentException("The writer for " + formatName
								+ " files is not able to add more than one image to the file : " + localOutputFile);
					} else {
						for (int i = 1; i < images.length; i++) {
							// writer.prepareWriteEmpty(md, its, images[i].getWidth(),
							// images[i].getHeight(), null, null,
							// writeParam);
							temp = new IIOImage(images[i], null, md);
							writer.writeInsert(i, temp, writeParam);
							// writer.endWriteEmpty();
						}
					}
				}
				// writer.endWriteSequence();
			} finally

			{
				// Close stream in finally block to avoid resource leaks
				if (output != null) {
					output.close();
				}
			}
		} finally

		{
			// Dispose writer in finally block to avoid memory leaks
			if (writer != null) {
				writer.dispose();
			}
		}
		return true;
	}

	public static ArrayList<String> getWordsAsSortedList(String input) {
		if ((input == null) || ("".equals(input))) {
			return new ArrayList<>();
		}
		String[] searchStrings = input.split("\\s+");
		Set<String> searchStringsSet = new HashSet<>();
		for (String s : searchStrings) {
			searchStringsSet.add(s);
		}
		ArrayList<String> searchStringsList = new ArrayList<>(searchStringsSet);
		Collections.sort(searchStringsList);
		return searchStringsList;
	}

	public static String getWordsAsSortedSentence(String input) {

		ArrayList<String> searchStringsList = getWordsAsSortedList(input);
		StringBuffer output = new StringBuffer();
		for (String word : searchStringsList) {
			output.append(word).append(" ");
		}
		return output.toString();
	}

}
