/**
 *
 */
package com.techwerx.image;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import com.techwerx.image.utils.SBImageUtils;

import net.sourceforge.tess4j.util.ImageHelper;

/**
 * @author Uddipan Bagchi
 *
 */
public class DeSkewSBImage {

	// private static final org.slf4j.Logger logger = LoggerFactory.getLogger(new
	// LoggHelper().toString());

	/**
	 * Representation of a line in the image.
	 */
	public class HoughLine {

		// count of points in the line
		public int count = 0;
		// index in matrix.
		public int index = 0;
		// the line is represented as all x, y that solve y * cos(alpha) - x *
		// sin(alpha) = d
		public double alpha;
		public double d;
	}

	// the source image
	private SBImage cImage;
	// the range of angles to search for lines
	private static double cAlphaStart = -30;
	private static double cAlphaStep = 0.2;
	private static int cSteps = 500;
	// pre-calculation of sin and cos
	private static double[] cSinA;
	private static double[] cCosA;
	// range of d
	private double cDMin;
	private double cDStep = 1.0;
	private int cDCount;
	// count of points that fit in a line
	private int[] cHMatrix;

	private int height;
	private int width;

	static {
		double angle;

		// pre-calculation of sin and cos
		cSinA = new double[cSteps - 1];
		cCosA = new double[cSteps - 1];

		for (int i = 0; i < (cSteps - 1); i++) {
			angle = (getAlpha(i) * Math.PI) / 180.0;
			cSinA[i] = Math.sin(angle);
			cCosA[i] = Math.cos(angle);
		}
	}

	/**
	 * Constructor.
	 *
	 * @param image
	 */
	public DeSkewSBImage(SBImage image) {
		this.cImage = image;
		this.height = image.height;
		this.width = image.width;
	}

	// Hough Transformation
	private void calc() {
		int hMin = (int) (this.height / 4.0);
		int hMax = (int) ((this.height * 3.0) / 4.0);
		this.init();

		for (int y = hMin; y < hMax; y++) {
			for (int x = 1; x < (this.width - 2); x++) {
				// only lower edges are considered
				if (this.cImage.pixels[y][x] == SBImageUtils.BLACK_PIXEL_REPLACEMENT) {
					if (this.cImage.pixels[y + 1][x] != SBImageUtils.BLACK_PIXEL_REPLACEMENT) {
						this.calc(x, y);
					}
				}
			}
		}

	}

	// calculate all lines through the point (x,y)
	private void calc(int x, int y) {
		double d;
		int dIndex;
		int index;

		for (int alpha = 0; alpha < (cSteps - 1); alpha++) {
			d = (y * cCosA[alpha]) - (x * cSinA[alpha]);
			dIndex = (int) (d - this.cDMin);
			index = (dIndex * cSteps) + alpha;
			try {
				this.cHMatrix[index] += 1;
			} catch (Exception e) {
				// logger.warn("", e);
			}
		}
	}

	public static double getAlpha(int index) {
		return cAlphaStart + (index * cAlphaStep);
	}

	/**
	 * Calculates the skew angle of the image cImage.
	 *
	 * @return
	 */
	public double getSkewAngle() {
		DeSkewSBImage.HoughLine[] hl;
		double sum = 0.0;
		int count = 0;

		// perform Hough Transformation
		this.calc();
		// top 10 of the detected lines in the image
		hl = this.getTop(10);

		if (hl.length >= 10) {
			// average angle of the lines
			for (int i = 0; i < 10; i++) {
				sum += hl[i].alpha;
				count++;
			}
			return (sum / count);
		} else {
			return 0.0d;
		}
	}

	// calculate the count lines in the image with most points
	private DeSkewSBImage.HoughLine[] getTop(int count) {

		DeSkewSBImage.HoughLine[] hl = new DeSkewSBImage.HoughLine[count];
		for (int i = 0; i < count; i++) {
			hl[i] = new DeSkewSBImage.HoughLine();
		}

		DeSkewSBImage.HoughLine tmp;

		for (int i = 0; i < (this.cHMatrix.length - 1); i++) {
			if (this.cHMatrix[i] > hl[count - 1].count) {
				hl[count - 1].count = this.cHMatrix[i];
				hl[count - 1].index = i;
				int j = count - 1;
				while ((j > 0) && (hl[j].count > hl[j - 1].count)) {
					tmp = hl[j];
					hl[j] = hl[j - 1];
					hl[j - 1] = tmp;
					j--;
				}
			}
		}

		int alphaIndex;
		int dIndex;

		for (int i = 0; i < count; i++) {
			dIndex = hl[i].index / cSteps; // integer division, no
			// remainder
			alphaIndex = hl[i].index - (dIndex * cSteps);
			hl[i].alpha = getAlpha(alphaIndex);
			hl[i].d = dIndex + this.cDMin;
		}

		return hl;
	}

	private void init() {
		// range of d
		this.cDMin = -this.width;
		this.cDCount = (int) ((2.0 * ((this.width + this.height))) / this.cDStep);
		this.cHMatrix = new int[this.cDCount * cSteps];
	}

	public static void main(String[] args) throws Exception {
		String dir = "E:\\TechWerx\\Java\\Working\\";
		String originalFile = "Skewed Image.jpg";
		BufferedImage image = ImageIO.read(new File(dir + originalFile));
		DeSkewSBImage ds = new DeSkewSBImage(SBImage.getSBImageFromBufferedImage(image));
		double deskewAngle = ds.getSkewAngle();
		System.out.println("Deskew Angle = " + deskewAngle);
		BufferedImage out = ImageHelper.rotateImage(image, -deskewAngle);
		SBImageUtils.writeFile(out, "png", dir + "BI" + ".png", 300);
	}
}
