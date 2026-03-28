package com.techwerx.image.streams;

/**
 * ByteArrayInputStream implementation that does not synchronize methods.
 */
public class FastByteArrayInputStreamData {
	/**
	 * Our byte buffer
	 */
	protected byte[] buf;
	/**
	 * Number of bytes that we can read from the buffer
	 */
	protected long count;
	/**
	 * Number of bytes that have been read from the buffer
	 */
	protected long pos;

	public FastByteArrayInputStreamData(byte[] buf, long count, long pos) {
		this.buf = buf;
		this.count = count;
		this.pos = pos;
	}

	public byte[] getBuf() {
		return this.buf;
	}

	public void setBuf(byte[] buf) {
		this.buf = buf;
	}

	public long getCount() {
		return this.count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public long getPos() {
		return this.pos;
	}

	public void setPos(long pos) {
		this.pos = pos;
	}
}