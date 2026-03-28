/**
 *
 */
package com.techwerx.text;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.techwerx.image.utils.SBImageUtils;

/**
 * @author Admin
 *
 */
public class BIWrapperForOCR {

	public BufferedImage image;
	public ByteBuffer byteBuffer;
	public int bytesPerPixel;
	public int bytesPerLine;

	public BIWrapperForOCR(BufferedImage image) {
		if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
			this.image = image;
		} else {
			BufferedImage byteImage = new BufferedImage(image.getWidth(), image.getHeight(),
					BufferedImage.TYPE_BYTE_GRAY);
			Graphics2D g2d = byteImage.createGraphics();
			SBImageUtils.setGraphics2DParameters(g2d);
			g2d.drawImage(image, 0, 0, null);
			g2d.dispose();
			this.image = byteImage;
		}
		DataBuffer buffOut = image.getRaster().getDataBuffer();
		byte[] pixelDataOut = ((DataBufferByte) buffOut).getData();
		this.byteBuffer = ByteBuffer.allocateDirect(pixelDataOut.length);
		this.byteBuffer.order(ByteOrder.nativeOrder());
		this.byteBuffer.put(pixelDataOut);
		((Buffer) this.byteBuffer).flip();
		int bppOut = image.getColorModel().getPixelSize();
		this.bytesPerPixel = bppOut / 8;
		this.bytesPerLine = (int) Math.ceil((image.getWidth() * bppOut) / 8.0);
	}

}
