package com.techwerx.image.streams;

import java.io.InputStream;

/**
 * ByteArrayInputStream implementation that does not synchronize methods.
 */
public class FastByteArrayInputStream extends InputStream {
	protected FastByteArrayInputStreamData data = new FastByteArrayInputStreamData(null, 0, 0);

	public FastByteArrayInputStream(byte[] buf, int count) {
		this.data.setBuf(buf);
		this.data.setCount(count);
	}

	@Override
	public final int available() {
		return (int) (this.data.getCount() - this.data.getPos());
	}

	@Override
	public final int read() {
		return (this.data.getPos() < this.data.getCount()) ? (int) (this.data.getBuf()[(int) this.data.pos++] & 0xff)
				: -1;
	}

	@Override
	public final int read(byte[] b, int off, int len) {
		if (this.data.getPos() >= this.data.getCount()) {
			return -1;
		}

		if ((this.data.getPos() + len) > this.data.getCount()) {
			len = (int) (this.data.getCount() - this.data.getPos());
		}

		System.arraycopy(this.data.getBuf(), (int) this.data.getPos(), b, off, len);
		this.data.setPos(this.data.getPos() + len);
		return len;
	}

	@Override
	public final long skip(long n) {
		if ((this.data.getPos() + n) > this.data.getCount()) {
			n = this.data.getCount() - this.data.getPos();
		}
		if (n < 0) {
			return 0;
		}
		this.data.setPos(this.data.getPos() + n);
		return n;
	}

}
