package com.techwerx.image;

import net.sourceforge.lept4j.ILeptonica;
import net.sourceforge.lept4j.Leptonica;
import net.sourceforge.lept4j.Leptonica1;
import net.sourceforge.lept4j.Pix;
import net.sourceforge.lept4j.Sel;
import net.sourceforge.lept4j.util.LeptUtils;

import com.techwerx.image.utils.PixDebugWriter;

/**
 * Static utilities for morphological cleaning of binarised Leptonica Pix
 * objects.
 *
 * <p>Both methods follow the same ownership contract:
 * <ul>
 *   <li>The caller retains ownership of the input Pix — it is never disposed
 *       here.</li>
 *   <li>All intermediate Pix objects are created and disposed within the
 *       method.</li>
 *   <li>The returned Pix is owned by the caller, who is responsible for
 *       disposing it when done.</li>
 * </ul>
 */
public final class PixCleaningUtils {

	private PixCleaningUtils() {
	}

	/**
	 * Removes isolated salt-and-pepper noise pixels from a binarised image using
	 * a sequence of Hit-Miss Transform (HMT) and subtraction passes with
	 * structuring elements of increasing size.
	 *
	 * @param pixs      input Pix (1-bpp or 8-bpp; converted to 1-bpp internally)
	 * @param debugL    debug level; pass to {@code PixDebugWriter}
	 * @param debugDir  directory for debug image output
	 * @return a new 1-bpp Pix with noise removed; caller owns this Pix
	 */
	public static Pix removeSaltPepper(Pix pixs, int debugL, String debugDir) {
		Pix pix1 = null, pix2 = null, pix3 = null, pix4 = null, pix5 = null;
		Sel sel1 = null, sel2 = null, sel3 = null, sel4 = null, sel5 = null;

		final String selString1 = "ooooCoooo";
		final String selString2 = "ooooo Coo  ooooo";
		final String selString3 = "oooooo   oo C oo   oooooo";
		final String selString4 = "oooooooo     oo     oo  C  oo     oo     oooooooo";

		if (Leptonica.INSTANCE.pixGetDepth(pixs) != 1) {
			// 232 = SBImageUtils.GREY_REPLACEMENT_PIXEL
			pix1 = Leptonica1.pixConvertTo1(pixs, 232 - 80);
		} else {
			pix1 = Leptonica1.pixCopy(null, pixs);
		}

		sel1 = Leptonica1.selCreateFromString(selString1, 3, 3, "saltAndPepper1");
		sel2 = Leptonica1.selCreateFromString(selString2, 4, 4, "saltAndPepper2");
		sel3 = Leptonica1.selCreateFromString(selString3, 5, 5, "saltAndPepper3");
		sel4 = Leptonica1.selCreateBrick(2, 2, 0, 0, ILeptonica.SEL_HIT);
		sel5 = Leptonica1.selCreateFromString(selString4, 7, 7, "saltAndPepper4");

		pix2 = Leptonica1.pixHMT(null, pix1, sel1.getPointer());
		Pix pix21 = Leptonica1.pixDilate(null, pix2, sel4.getPointer());
		Pix pix22 = Leptonica1.pixSubtract(null, pix1, pix21);
		LeptUtils.dispose(pix1);
		LeptUtils.dispose(pix2);
		LeptUtils.dispose(pix21);
		if (debugL <= 1) {
			System.out.println("Reached - 1 in removeSaltPepper");
			PixDebugWriter.writeIfDebug(debugL, 1, pix22, debugDir + "/" + "saltPepper - 1.png");
			System.out.println("Pix 22 = " + pix22);
		}

		pix3 = Leptonica1.pixHMT(null, pix22, sel2.getPointer());
		Pix pix31 = Leptonica1.pixDilate(null, pix3, sel4.getPointer());
		Pix pix32 = Leptonica1.pixSubtract(null, pix22, pix31);
		LeptUtils.dispose(pix22);
		LeptUtils.dispose(pix3);
		LeptUtils.dispose(pix31);
		if (debugL <= 1) {
			System.out.println("Reached - 2 in removeSaltPepper");
			PixDebugWriter.writeIfDebug(debugL, 1, pix32, debugDir + "/" + "saltPepper - 2.png");
			System.out.println("Pix 32 = " + pix32);
		}

		pix4 = Leptonica1.pixHMT(null, pix32, sel3.getPointer());
		Pix pix41 = Leptonica1.pixDilate(null, pix4, sel4.getPointer());
		Pix pix42 = Leptonica1.pixSubtract(null, pix32, pix41);
		LeptUtils.dispose(pix32);
		LeptUtils.dispose(pix4);
		LeptUtils.dispose(pix41);
		if (debugL <= 1) {
			System.out.println("Reached - 3 in removeSaltPepper");
			PixDebugWriter.writeIfDebug(debugL, 1, pix42, debugDir + "/" + "saltPepper - 3.png");
			System.out.println("Pix 42 = " + pix42);
		}

		pix5 = Leptonica1.pixHMT(null, pix42, sel5.getPointer());
		Pix pix51 = Leptonica1.pixDilate(null, pix5, sel4.getPointer());
		Pix pix52 = Leptonica1.pixSubtract(null, pix42, pix51);
		LeptUtils.dispose(pix42);
		LeptUtils.dispose(pix5);
		LeptUtils.dispose(pix51);
		if (debugL <= 1) {
			System.out.println("Reached - 4 in removeSaltPepper");
			PixDebugWriter.writeIfDebug(debugL, 1, pix52, debugDir + "/" + "saltPepper - 4.png");
			System.out.println("Pix 52 = " + pix52);
		}

		LeptUtils.dispose(sel1);
		LeptUtils.dispose(sel2);
		LeptUtils.dispose(sel3);
		LeptUtils.dispose(sel4);
		LeptUtils.dispose(sel5);

		return pix52;
	}

	/**
	 * Removes horizontal and vertical lines from a binarised image using
	 * seed-fill elimination, operating on a padded copy to avoid border artefacts.
	 *
	 * @param inputPix  input 1-bpp Pix
	 * @param debugL    debug level
	 * @param debugDir  directory for debug image output
	 * @param tryNumber try-pass number used in debug image filenames
	 * @return a new 1-bpp Pix with lines removed; caller owns this Pix
	 */
	public static Pix removeLines(Pix inputPix, int debugL, String debugDir, int tryNumber) {

		// horizontal line seed, binary fill, and removal
		final int selDimension = 125;
		Pix firstOrderCleanedImage = Leptonica1.pixConvertTo1(inputPix, 162);
		int depth = Leptonica1.pixGetDepth(firstOrderCleanedImage);
		int whitePixel = (depth == 1) ? 0 : 255;

		Pix paddedBorderCleanedPix = Leptonica1.pixAddBorder(firstOrderCleanedImage, selDimension / 2, whitePixel);
		Pix pix1 = Leptonica1.pixOpenCompBrick(null, paddedBorderCleanedPix, selDimension, 1);
		PixDebugWriter.writeIfDebug(debugL, 2, pix1,
				debugDir + "/" + "pixOpenLine-" + tryNumber + ".png");

		Pix paddedBorderDilated = Leptonica1.pixDilateCompBrick(null, paddedBorderCleanedPix, 6, 1);
		PixDebugWriter.writeIfDebug(debugL, 2, paddedBorderDilated,
				debugDir + "/" + "pixPaddedBorderDilated-" + tryNumber + ".png");

		Pix seedfillBinaryPix = Leptonica1.pixSeedfillBinary(null, pix1, paddedBorderDilated, 4);
		PixDebugWriter.writeIfDebug(debugL, 2, seedfillBinaryPix,
				debugDir + "/" + "pixSeedfillBinaryPix-" + tryNumber + ".png");

		Pix horizontalLinesEliminatedPix = Leptonica1.pixSubtract(null, paddedBorderCleanedPix, seedfillBinaryPix);
		PixDebugWriter.writeIfDebug(debugL, 2, horizontalLinesEliminatedPix,
				debugDir + "/" + "pixHorizontalLinesEliminatedPix-" + tryNumber + ".png");

		LeptUtils.dispose(firstOrderCleanedImage);
		LeptUtils.dispose(paddedBorderCleanedPix);
		LeptUtils.dispose(pix1);
		LeptUtils.dispose(paddedBorderDilated);
		LeptUtils.dispose(seedfillBinaryPix);

		// vertical line seed, binary fill, and removal
		final int vertSelDimension = selDimension;
		Pix pix2 = Leptonica1.pixOpenCompBrick(null, horizontalLinesEliminatedPix, 1, vertSelDimension);
		PixDebugWriter.writeIfDebug(debugL, 2, pix2,
				debugDir + "/" + "pixVerticalOpenLine-" + tryNumber + ".png");

		Pix horLinesEliminatedDilated = Leptonica1.pixDilateCompBrick(null, horizontalLinesEliminatedPix, 1, 5);
		PixDebugWriter.writeIfDebug(debugL, 2, horLinesEliminatedDilated,
				debugDir + "/" + "pixVerticalPaddedBorderDilated-" + tryNumber + ".png");

		Pix seedfillBinaryPix1 = Leptonica1.pixSeedfillBinary(null, pix2, horLinesEliminatedDilated, 4);
		PixDebugWriter.writeIfDebug(debugL, 2, seedfillBinaryPix1,
				debugDir + "/" + "pixVerticalSeedfillBinaryPix-" + tryNumber + ".png");

		Pix linesEliminatedPix = Leptonica1.pixSubtract(null, horizontalLinesEliminatedPix, seedfillBinaryPix1);
		PixDebugWriter.writeIfDebug(debugL, 2, linesEliminatedPix,
				debugDir + "/" + "pixVerticalLinesEliminatedPix-" + tryNumber + ".png");

		LeptUtils.dispose(horizontalLinesEliminatedPix);
		LeptUtils.dispose(pix2);
		LeptUtils.dispose(horLinesEliminatedDilated);
		LeptUtils.dispose(seedfillBinaryPix1);

		Pix finalPix = Leptonica1.pixRemoveBorder(linesEliminatedPix, selDimension / 2);
		PixDebugWriter.writeIfDebug(debugL, 2, finalPix,
				debugDir + "/" + "pixFinalLinesCleanedAfterRemovingBorder-" + tryNumber + ".png");

		LeptUtils.dispose(linesEliminatedPix);

		return finalPix;
	}

}