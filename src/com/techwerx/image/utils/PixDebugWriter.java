package com.techwerx.image.utils;

import net.sourceforge.lept4j.ILeptonica;
import net.sourceforge.lept4j.Leptonica1;
import net.sourceforge.lept4j.Pix;

/**
 * Static helper that centralises the repeated pattern:
 *
 * <pre>
 * if (debugL &lt;= threshold) {
 *     Leptonica1.pixWrite(path, pix, IFF_PNG);
 * }
 * </pre>
 *
 * Callers replace that block with a single call to
 * {@link #writeIfDebug(int, int, Pix, String)}.
 * The Pix ownership and lifecycle are entirely the caller's responsibility;
 * this class never disposes anything.
 */
public final class PixDebugWriter {

	private PixDebugWriter() {
	}

	/**
	 * Writes {@code pix} to {@code path} as a PNG if {@code debugL <= threshold}.
	 *
	 * @param debugL    current debug level
	 * @param threshold level at or below which the write is performed
	 * @param pix       the Leptonica Pix to write (may be null; call is skipped if so)
	 * @param path      destination file path
	 */
	public static void writeIfDebug(int debugL, int threshold, Pix pix, String path) {
		if ((debugL <= threshold) && (pix != null)) {
			Leptonica1.pixWrite(path, pix, ILeptonica.IFF_PNG);
		}
	}

}