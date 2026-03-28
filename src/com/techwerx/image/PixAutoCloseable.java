package com.techwerx.image;

import net.sourceforge.lept4j.Pix;
import net.sourceforge.lept4j.util.LeptUtils;

/**
 * A thin {@link AutoCloseable} wrapper around a Leptonica {@link Pix} that
 * calls {@link LeptUtils#dispose(Object)} on {@link #close()}.
 *
 * <p>Intended for use in try-with-resources blocks to guarantee disposal of
 * intermediate Pix objects even when an exception is thrown mid-method,
 * without altering the Leptonica call sequence or parameters.
 *
 * <p><strong>Important:</strong> do <em>not</em> wrap Pix objects that are
 * assigned to longer-lived fields (e.g. static class-level fields) — those
 * must continue to be disposed by their owners at the appropriate time.
 *
 * <pre>
 * try (PixAutoCloseable interim = new PixAutoCloseable(Leptonica1.pixConvertTo8(src, 0))) {
 *     // use interim.get() here
 * }
 * // interim is disposed here even if an exception was thrown above
 * </pre>
 */
public final class PixAutoCloseable implements AutoCloseable {

	private final Pix pix;

	/**
	 * @param pix the Pix to manage; may be null (close() will be a no-op)
	 */
	public PixAutoCloseable(Pix pix) {
		this.pix = pix;
	}

	/**
	 * Returns the wrapped Pix.
	 */
	public Pix get() {
		return this.pix;
	}

	/**
	 * Disposes the wrapped Pix via {@link LeptUtils#dispose(Object)}.
	 * Safe to call on a null Pix.
	 */
	@Override
	public void close() {
		LeptUtils.dispose(this.pix);
	}

}