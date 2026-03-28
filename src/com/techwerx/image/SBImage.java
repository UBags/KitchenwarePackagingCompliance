/**
 *
 */
package com.techwerx.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;

import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.techwerx.image.utils.NumberTriplet;
import com.techwerx.image.utils.SBImageArrayUtils;
import com.techwerx.image.utils.SBImageUtils;

import net.sourceforge.lept4j.Leptonica;
import net.sourceforge.lept4j.Leptonica1;
import net.sourceforge.lept4j.Pix;

/**
 * @author Uddipan Bagchi
 *
 */
public class SBImage implements Cloneable {

	// Aligns pixels to the actual image; What You See Is What You Have in the pixel
	// array; Done so that we can do processing properly; only thing to be kept in
	// mind is that we will have to access the array as [j][i]

	private Semaphore byteBufferObserver = new Semaphore(1, true);
	public final int[][] pixels;
	public ByteBuffer byteBuffer;
	public int bytesPerPixel;
	public int bytesPerLine;
	public final int width;
	public final int height;
	public static final int MAX = 255;
	public static final int MIN = 0;
	public final int xDivisions;
	public final int yDivisions;
	// public final HashMap<Integer, SBImage> componentArrays = new
	// HashMap<>(25);
	// private static SBImage dummyInstance = new SBImage();
	public final ArrayList<SBSubImageCoordinates> subImageCoordinates = new ArrayList<>(25);
	private NumberTriplet meanAndSD = new NumberTriplet(0.0, 0.0, 0.0);
	public BufferedImage underlyingBuffImage;
	public boolean deskewed = false;
	public ArrayList<BBox> boundingBoxes;

	public static final int[] whitePixels = new int[] { 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245,
			246, 247, 248, 249, 250, 251, 252, 253, 254, 255 };
	public static final int[] blackPixels = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
			18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35 };

	public class SBImageComponent implements Supplier<Object> {

		int componentNo;
		int xPos;
		int yPos;
		int width;
		int height;

		public SBImageComponent(int componentNo, int xPos, int yPos, int height, int width) {
			this.componentNo = componentNo;
			this.xPos = xPos;
			this.yPos = yPos;
			this.width = width;
			this.height = height;
		}

		@Override
		public Object get() {
			// System.out.println("Entered get() for " + this.componentNo);
			// System.out.println(
			// "Processing componentNo " + this.componentNo + " in Thread " +
			// Thread.currentThread().toString());
			int[][] subimage = new int[this.height][this.width];
			for (int i = this.xPos; i < (this.xPos + this.width); i++) {
				for (int j = this.yPos; j < (this.yPos + this.height); j++) {
					subimage[j - this.yPos][i - this.xPos] = SBImage.this.pixels[j][i];
				}
			}
			// System.out.println("Populating the HashMap for component " +
			// this.componentNo);

			// SBImage.this.componentArrays.put(this.componentNo, new
			// SBImage(subimage, false));
			return Boolean.TRUE;
		}
	}

	private SBImage() {
		this.pixels = null;
		this.width = 0;
		this.height = 0;
		this.xDivisions = 0;
		this.yDivisions = 0;
		// this.componentArrays = null;
	}

	public SBImage(DataBufferInt pixels, int height) throws InterruptedException {
		this(pixels, height, true, true, false);
	}

	public SBImage(DataBufferInt pixels, int height, boolean removeBorders) throws InterruptedException {
		this(pixels, height, true, removeBorders, false);
	}

	public SBImage(DataBufferInt pixels, int height, boolean subdivide, boolean removeBorders, boolean createByteBuffer)
			throws InterruptedException {
		// System.out.println("Width = " + width + " and height = " + height);
		final int[] b = pixels.getData();

		// System.out.println("Total pixels = " + b.length + " and by calculation is " +
		// (width * height));
		// final byte[][] bytes = new byte[width][height];
		// for (int i = 0; i < width; i++) {
		// // System.out.println(j);
		// System.arraycopy(b, i * height, bytes[i], 0, height);
		// }
		int[][] temp = SBImageArrayUtils.parallelFrom1DTo2D(b, height);
		// int[][] temp = SBImageArrayUtils.iterateFrom1DTo2D(b, height);
		if (removeBorders) {
			this.pixels = SBImage.removeBorders(temp);
		} else {
			this.pixels = temp;
		}
		this.width = this.pixels[0].length;
		this.height = this.pixels.length;
		if (createByteBuffer) {
			this.byteBufferObserver.acquire();
			new Runnable() {
				@Override
				public void run() {
					SBImage.this.populateByteBuffer();
					SBImage.this.byteBufferObserver.release();
				}
			}.run();
		}
		new Runnable() {

			@Override
			public void run() {
				synchronized (SBImage.this.meanAndSD) {
					SBImage.this.meanAndSD = SBImageArrayUtils.imageStatistics(SBImage.this.pixels, false);
				}
			}
		}.run();

		// this.pixels = SBImageArrayUtils.from1DTo2D(b, height);
		// this.pixels = SBImageArrayUtils.parallelSystemArraycopyFrom1DTo2D(b, height);

//		if (this.width < 1000) {
//			this.xDivisions = 2;
//		} else {
//			if (this.width < 2000) {
//				this.xDivisions = 3;
//			} else {
//				if (this.width < 3000) {
//					this.xDivisions = 4;
//				} else {
//					this.xDivisions = 5;
//				}
//			}
//		}

		if (this.width < 1000) {
			this.xDivisions = 4;
		} else {
			if (this.width < 2000) {
				this.xDivisions = 8;
			} else {
				if (this.width < 3000) {
					this.xDivisions = 12;
				} else {
					this.xDivisions = 18;
				}
			}
		}

//		if (this.height < 1000) {
//			this.yDivisions = 2;
//		} else {
//			if (this.height < 2000) {
//				this.yDivisions = 3;
//			} else {
//				if (this.height < 3000) {
//					this.yDivisions = 4;
//				} else {
//					this.yDivisions = 5;
//				}
//			}
//		}

		if (this.height < 1000) {
			this.yDivisions = 4;
		} else {
			if (this.height < 2000) {
				this.yDivisions = 8;
			} else {
				if (this.height < 3000) {
					this.yDivisions = 12;
				} else {
					this.yDivisions = 18;
				}
			}
		}

		/*
		 * if (this.width < 600) { this.xDivisions = 3; } else { if (this.width < 1000)
		 * { this.xDivisions = 4; } else { if (this.width < 2000) { this.xDivisions = 6;
		 * } else { if (this.width < 3000) { this.xDivisions = 8; } else { if
		 * (this.width < 4000) { this.xDivisions = 10; } else { this.xDivisions = 12; }
		 * } } } } if (this.height < 600) { this.yDivisions = 3; } else { if
		 * (this.height < 1000) { this.yDivisions = 4; } else { if (this.height < 2000)
		 * { this.yDivisions = 6; } else { if (this.height < 3000) { this.yDivisions =
		 * 8; } else { if (this.height < 4000) { this.yDivisions = 10; } else {
		 * this.yDivisions = 12; } } } } }
		 */

		int noOfDivisions = this.xDivisions * this.yDivisions;

		if (subdivide) {
			for (int i = 0; i < noOfDivisions; i++) {
				final boolean topEdge = ((i / this.xDivisions) == 0);
				final boolean rightEdge = ((i % this.xDivisions) == (this.xDivisions - 1));
				final boolean bottomEdge = (this.xDivisions >= (noOfDivisions - i));
				final boolean leftEdge = ((i % this.xDivisions) == 0);
				final int cx = (i % this.xDivisions) * (this.width / this.xDivisions);
				final int cy = (i / this.xDivisions) * (this.height / this.yDivisions);
				final int cWidth = !rightEdge ? (this.width / this.xDivisions)
						: this.width - ((this.width / this.xDivisions) * (this.xDivisions - 1));
				final int cHeight = !bottomEdge ? this.height / this.yDivisions
						: this.height - ((this.height / this.yDivisions) * (this.yDivisions - 1));
				this.subImageCoordinates.add(
						new SBSubImageCoordinates(cx, cy, cHeight, cWidth, topEdge, rightEdge, bottomEdge, leftEdge));
			}
			/*
			 * ArrayList<CompletableFuture<Object>> cfs = new ArrayList<>(noOfDivisions);
			 * for (int i = 0; i < (this.xDivisions * this.yDivisions); i++) { final boolean
			 * xEdge = ((i % this.xDivisions) == (this.xDivisions - 1)); final boolean yEdge
			 * = ((i / this.xDivisions) == (this.yDivisions - 1)); final int cWidth = !xEdge
			 * ? (this.width / this.xDivisions) : this.width - ((this.width /
			 * this.xDivisions) * (this.xDivisions - 1)); final int cHeight = !yEdge ?
			 * this.height / this.yDivisions : this.height - ((this.height /
			 * this.yDivisions) * (this.yDivisions - 1)); final int cx = (i %
			 * this.xDivisions) * (this.width / this.xDivisions); final int cy = (i /
			 * this.xDivisions) * (this.height / this.yDivisions); //
			 * System.out.format("Calling CF componentNo %d with %d, %d, %d, %d", i, cx, cy,
			 * // cWidth, cHeight) // .println(); cfs.add(CompletableFuture
			 * .supplyAsync(SBImage.this.new SBImageComponent(i, cx, cy, cHeight, cWidth)));
			 * } // System.out.println("Waiting for all threads to finish"); //
			 * CompletableFuture<Void> allFutures = CompletableFuture //
			 * .allOf(cfs.toArray(new CompletableFuture[cfs.size()]));
			 * CompletableFuture.allOf(cfs.toArray(new
			 * CompletableFuture[cfs.size()])).join();
			 *
			 * // CompletableFuture<List<Object>> allCFFuture = allFutures.thenApply(v -> {
			 * // return cfs.stream().map(cf -> cf.join()).collect(Collectors.toList()); //
			 * });
			 *
			 * // Object temp = null;
			 *
			 * for (CompletableFuture<Object> cf : cfs) { try { cf.get(); } catch
			 * (InterruptedException e) { e.printStackTrace(); } catch (ExecutionException
			 * e) { e.printStackTrace(); } }
			 *
			 */
		}
		if (createByteBuffer) {
			this.byteBufferObserver.acquire();
			this.byteBufferObserver.release();
		}
		// System.out.println(Arrays.deepToString(this.pixels));
	}

	public SBImage(DataBufferInt pixels, int height, boolean subdivide, boolean removeBorders, boolean createByteBuffer,
			boolean noParallel) {
		final int[] b = pixels.getData();

		int[][] temp = SBImageArrayUtils.iterateFrom1DTo2D(b, height);
		if (removeBorders) {
			this.pixels = SBImage.removeBorders(temp);
		} else {
			this.pixels = temp;
		}
		this.width = this.pixels[0].length;
		this.height = this.pixels.length;
		if (createByteBuffer) {
			this.populateByteBuffer();
		}
		new Runnable() {
			@Override
			public void run() {
				synchronized (SBImage.this.meanAndSD) {
					SBImage.this.meanAndSD = SBImageArrayUtils.imageStatistics(SBImage.this.pixels, false);
				}
			}
		}.run();

		if (this.width < 1000) {
			this.xDivisions = 4;
		} else {
			if (this.width < 2000) {
				this.xDivisions = 8;
			} else {
				if (this.width < 3000) {
					this.xDivisions = 12;
				} else {
					this.xDivisions = 18;
				}
			}
		}

		if (this.height < 1000) {
			this.yDivisions = 4;
		} else {
			if (this.height < 2000) {
				this.yDivisions = 8;
			} else {
				if (this.height < 3000) {
					this.yDivisions = 12;
				} else {
					this.yDivisions = 18;
				}
			}
		}

		int noOfDivisions = this.xDivisions * this.yDivisions;

		if (subdivide) {
			for (int i = 0; i < noOfDivisions; i++) {
				final boolean topEdge = ((i / this.xDivisions) == 0);
				final boolean rightEdge = ((i % this.xDivisions) == (this.xDivisions - 1));
				final boolean bottomEdge = (this.xDivisions >= (noOfDivisions - i));
				final boolean leftEdge = ((i % this.xDivisions) == 0);
				final int cx = (i % this.xDivisions) * (this.width / this.xDivisions);
				final int cy = (i / this.xDivisions) * (this.height / this.yDivisions);
				final int cWidth = !rightEdge ? (this.width / this.xDivisions)
						: this.width - ((this.width / this.xDivisions) * (this.xDivisions - 1));
				final int cHeight = !bottomEdge ? this.height / this.yDivisions
						: this.height - ((this.height / this.yDivisions) * (this.yDivisions - 1));
				this.subImageCoordinates.add(
						new SBSubImageCoordinates(cx, cy, cHeight, cWidth, topEdge, rightEdge, bottomEdge, leftEdge));
			}
		}
		// System.out.println(Arrays.deepToString(this.pixels));
	}

	// if SBImage is being formed from a 2D array, removeBorders has already been
	// done in an earlier step; so, default the parameter removeBorders to false
	public SBImage(int[][] data) throws InterruptedException {
		this(data, true, false, false);
	}

	public SBImage(int[][] data, boolean removeBorders) throws InterruptedException {
		this(data, true, removeBorders, false);
	}

	public SBImage(int[][] data, boolean subdivide, boolean removeBorders, boolean createByteBuffer)
			throws InterruptedException {
		if (removeBorders) {
			this.pixels = SBImage.removeBorders(data);
		} else {
			this.pixels = data;
		}
		this.height = this.pixels.length;
		this.width = this.pixels[0].length;
		if (createByteBuffer) {
			this.byteBufferObserver.acquire();
			new Runnable() {
				@Override
				public void run() {
					SBImage.this.populateByteBuffer();
					SBImage.this.byteBufferObserver.release();
				}
			}.run();
		}
		new Runnable() {

			@Override
			public void run() {
				synchronized (SBImage.this.meanAndSD) {
					SBImage.this.meanAndSD = SBImageArrayUtils.imageStatistics(SBImage.this.pixels, false);
				}
			}
		}.run();
		/*
		 * if (this.width < 600) { this.xDivisions = 2; } else { if (this.width < 1000)
		 * { this.xDivisions = 3; } else { if (this.width < 2000) { this.xDivisions = 4;
		 * } else { if (this.width < 3000) { this.xDivisions = 5; } else { if
		 * (this.width < 4000) { this.xDivisions = 7; } else { this.xDivisions = 10; } }
		 * } } } if (this.height < 600) { this.yDivisions = 2; } else { if (this.height
		 * < 1000) { this.yDivisions = 3; } else { if (this.height < 2000) {
		 * this.yDivisions = 4; } else { if (this.height < 3000) { this.yDivisions = 5;
		 * } else { if (this.height < 4000) { this.yDivisions = 7; } else {
		 * this.yDivisions = 10; } } } } }
		 */
//		if (this.width < 1000) {
//			this.xDivisions = 2;
//		} else {
//			if (this.width < 2000) {
//				this.xDivisions = 3;
//			} else {
//				if (this.width < 3000) {
//					this.xDivisions = 4;
//				} else {
//					this.xDivisions = 5;
//				}
//			}
//		}

		if (this.width < 1000) {
			this.xDivisions = 4;
		} else {
			if (this.width < 2000) {
				this.xDivisions = 8;
			} else {
				if (this.width < 3000) {
					this.xDivisions = 12;
				} else {
					this.xDivisions = 16;
				}
			}
		}

//		if (this.height < 1000) {
//			this.yDivisions = 2;
//		} else {
//			if (this.height < 2000) {
//				this.yDivisions = 3;
//			} else {
//				if (this.height < 3000) {
//					this.yDivisions = 4;
//				} else {
//					this.yDivisions = 5;
//				}
//			}
//		}

		if (this.height < 1000) {
			this.yDivisions = 4;
		} else {
			if (this.height < 2000) {
				this.yDivisions = 8;
			} else {
				if (this.height < 3000) {
					this.yDivisions = 12;
				} else {
					this.yDivisions = 16;
				}
			}
		}

		int noOfDivisions = this.xDivisions * this.yDivisions;
		// this.componentArrays = new HashMap<Integer, SBImage>(noOfDivisions);

		if (subdivide) {
			for (int i = 0; i < noOfDivisions; i++) {
				final boolean topEdge = ((i / this.xDivisions) == 0);
				final boolean rightEdge = ((i % this.xDivisions) == (this.xDivisions - 1));
				final boolean bottomEdge = (this.xDivisions >= (noOfDivisions - i));
				final boolean leftEdge = ((i % this.xDivisions) == 0);
				final int cx = (i % this.xDivisions) * (this.width / this.xDivisions);
				final int cy = (i / this.xDivisions) * (this.height / this.yDivisions);
				final int cWidth = !rightEdge ? (this.width / this.xDivisions)
						: this.width - ((this.width / this.xDivisions) * (this.xDivisions - 1));
				final int cHeight = !bottomEdge ? this.height / this.yDivisions
						: this.height - ((this.height / this.yDivisions) * (this.yDivisions - 1));
				this.subImageCoordinates.add(
						new SBSubImageCoordinates(cx, cy, cHeight, cWidth, topEdge, rightEdge, bottomEdge, leftEdge));
			}
			/*
			 * ArrayList<CompletableFuture<Object>> cfs = new ArrayList<>(noOfDivisions);
			 * for (int i = 0; i < (this.xDivisions * this.yDivisions); i++) { final boolean
			 * xEdge = ((i % this.xDivisions) == (this.xDivisions - 1)); final boolean yEdge
			 * = ((i / this.xDivisions) == (this.yDivisions - 1)); final int cWidth = !xEdge
			 * ? (this.width / this.xDivisions) : this.width - ((this.width /
			 * this.xDivisions) * (this.xDivisions - 1)); final int cHeight = !yEdge ?
			 * this.height / this.yDivisions : this.height - ((this.height /
			 * this.yDivisions) * (this.yDivisions - 1)); final int cx = (i %
			 * this.xDivisions) * (this.width / this.xDivisions); final int cy = (i /
			 * this.xDivisions) * (this.height / this.yDivisions); //
			 * System.out.format("Calling CF componentNo %d with %d, %d, %d, %d", i, cx, cy,
			 * // cWidth, cHeight) // .println(); cfs.add(CompletableFuture
			 * .supplyAsync(SBImage.this.new SBImageComponent(i, cx, cy, cHeight, cWidth)));
			 * } // System.out.println("Waiting for all threads to finish");
			 * CompletableFuture<Void> allFutures = CompletableFuture .allOf(cfs.toArray(new
			 * CompletableFuture[cfs.size()]));
			 *
			 * // CompletableFuture<List<Object>> allCFFuture = allFutures.thenApply(v -> {
			 * // return cfs.stream().map(cf -> cf.join()).collect(Collectors.toList()); //
			 * });
			 *
			 * // Object temp = null; for (CompletableFuture<Object> cf : cfs) { try {
			 * cf.get(); } catch (InterruptedException e) { e.printStackTrace(); } catch
			 * (ExecutionException e) { e.printStackTrace(); } }
			 */
			if (createByteBuffer) {
				this.byteBufferObserver.acquire();
				this.byteBufferObserver.release();
			}
		}
	}

	public SBImage(int[][] data, boolean subdivide, boolean removeBorders, boolean createByteBuffer, boolean parallel) {
		if (removeBorders) {
			this.pixels = SBImage.removeBorders(data);
		} else {
			this.pixels = data;
		}
		this.height = this.pixels.length;
		this.width = this.pixels[0].length;
		if (createByteBuffer) {
			this.populateByteBuffer();
		}
		new Runnable() {
			@Override
			public void run() {
				synchronized (SBImage.this.meanAndSD) {
					SBImage.this.meanAndSD = SBImageArrayUtils.imageStatistics(SBImage.this.pixels, false);
				}
			}
		}.run();

		if (this.width < 1000) {
			this.xDivisions = 4;
		} else {
			if (this.width < 2000) {
				this.xDivisions = 8;
			} else {
				if (this.width < 3000) {
					this.xDivisions = 12;
				} else {
					this.xDivisions = 16;
				}
			}
		}

		if (this.height < 1000) {
			this.yDivisions = 4;
		} else {
			if (this.height < 2000) {
				this.yDivisions = 8;
			} else {
				if (this.height < 3000) {
					this.yDivisions = 12;
				} else {
					this.yDivisions = 16;
				}
			}
		}

		int noOfDivisions = this.xDivisions * this.yDivisions;

		if (subdivide) {
			for (int i = 0; i < noOfDivisions; i++) {
				final boolean topEdge = ((i / this.xDivisions) == 0);
				final boolean rightEdge = ((i % this.xDivisions) == (this.xDivisions - 1));
				final boolean bottomEdge = (this.xDivisions >= (noOfDivisions - i));
				final boolean leftEdge = ((i % this.xDivisions) == 0);
				final int cx = (i % this.xDivisions) * (this.width / this.xDivisions);
				final int cy = (i / this.xDivisions) * (this.height / this.yDivisions);
				final int cWidth = !rightEdge ? (this.width / this.xDivisions)
						: this.width - ((this.width / this.xDivisions) * (this.xDivisions - 1));
				final int cHeight = !bottomEdge ? this.height / this.yDivisions
						: this.height - ((this.height / this.yDivisions) * (this.yDivisions - 1));
				this.subImageCoordinates.add(
						new SBSubImageCoordinates(cx, cy, cHeight, cWidth, topEdge, rightEdge, bottomEdge, leftEdge));
			}
		}
	}

	public void populateSubImageCoordinates() {
		/*
		 * if (this.componentArrays.size() != 0) { return; } int noOfDivisions =
		 * this.xDivisions * this.yDivisions; ArrayList<CompletableFuture<Object>> cfs =
		 * new ArrayList<>(noOfDivisions); for (int i = 0; i < (this.xDivisions *
		 * this.yDivisions); i++) { final boolean xEdge = ((i % this.xDivisions) ==
		 * (this.xDivisions - 1)); final boolean yEdge = ((i / this.xDivisions) ==
		 * (this.yDivisions - 1)); final int cWidth = !xEdge ? (this.width /
		 * this.xDivisions) : this.width - ((this.width / this.xDivisions) *
		 * (this.xDivisions - 1)); final int cHeight = !yEdge ? this.height /
		 * this.yDivisions : this.height - ((this.height / this.yDivisions) *
		 * (this.yDivisions - 1)); final int cx = (i % this.xDivisions) * (this.width /
		 * this.xDivisions); final int cy = (i / this.xDivisions) * (this.height /
		 * this.yDivisions); //
		 * System.out.format("Calling CF componentNo %d with %d, %d, %d, %d", i, cx, cy,
		 * // cWidth, cHeight) // .println();
		 * cfs.add(CompletableFuture.supplyAsync(SBImage.this.new SBImageComponent(i,
		 * cx, cy, cHeight, cWidth))); } //
		 * System.out.println("Waiting for all threads to finish");
		 * CompletableFuture<Void> allFutures = CompletableFuture.allOf(cfs.toArray(new
		 * CompletableFuture[cfs.size()]));
		 *
		 * // CompletableFuture<List<Object>> allCFFuture = allFutures.thenApply(v -> {
		 * // return cfs.stream().map(cf -> cf.join()).collect(Collectors.toList()); //
		 * });
		 *
		 * // Object temp = null; for (CompletableFuture<Object> cf : cfs) { try {
		 * cf.get(); } catch (InterruptedException e) { e.printStackTrace(); } catch
		 * (ExecutionException e) { e.printStackTrace(); } }
		 */

		/*
		 * Removed this part because for different kernels, we need different different
		 * coordinates. Also, this code executes so fast, it hardly takes any time if
		 * (this.subImageCoordinates.size() > 0) { return; }
		 */
		this.subImageCoordinates.clear();
		int noOfDivisions = this.xDivisions * this.yDivisions;
		for (int i = 0; i < noOfDivisions; i++) {
			final boolean topEdge = ((i / this.xDivisions) == 0);
			final boolean rightEdge = ((i % this.xDivisions) == (this.xDivisions - 1));
			final boolean bottomEdge = (this.xDivisions >= (noOfDivisions - i));
			final boolean leftEdge = ((i % this.xDivisions) == 0);
			final int cx = (i % this.xDivisions) * (this.width / this.xDivisions);
			final int cy = (i / this.xDivisions) * (this.height / this.yDivisions);
			final int cWidth = !rightEdge ? (this.width / this.xDivisions)
					: this.width - ((this.width / this.xDivisions) * (this.xDivisions - 1));
			final int cHeight = !bottomEdge ? this.height / this.yDivisions
					: this.height - ((this.height / this.yDivisions) * (this.yDivisions - 1));
			this.subImageCoordinates
					.add(new SBSubImageCoordinates(cx, cy, cHeight, cWidth, topEdge, rightEdge, bottomEdge, leftEdge));
		}
	}

	public ArrayList<SBSubImageCoordinates> calculateSubImageCoordinates(XYDivisions divisions) {

		ArrayList<SBSubImageCoordinates> al = new ArrayList<SBSubImageCoordinates>(64);
		int noOfDivisions = divisions.xDivisions * divisions.yDivisions;
		for (int i = 0; i < noOfDivisions; i++) {
			final boolean topEdge = ((i / divisions.xDivisions) == 0);
			final boolean rightEdge = ((i % divisions.xDivisions) == (divisions.xDivisions - 1));
			final boolean bottomEdge = (divisions.xDivisions >= (noOfDivisions - i));
			final boolean leftEdge = ((i % divisions.xDivisions) == 0);
			final int cx = (i % divisions.xDivisions) * (this.width / divisions.xDivisions);
			final int cy = (i / divisions.xDivisions) * (this.height / divisions.yDivisions);
			final int cWidth = !rightEdge ? (this.width / divisions.xDivisions)
					: this.width - ((this.width / divisions.xDivisions) * (divisions.xDivisions - 1));
			final int cHeight = !bottomEdge ? this.height / divisions.yDivisions
					: this.height - ((this.height / divisions.yDivisions) * (divisions.yDivisions - 1));
			al.add(new SBSubImageCoordinates(cx, cy, cHeight, cWidth, topEdge, rightEdge, bottomEdge, leftEdge));
		}
		return al;
	}

	public void populateSubImageCoordinates(int kernelHeight, int kernelWidth) throws Exception {
		if ((kernelWidth >= (this.width / this.xDivisions)) || (kernelHeight >= (this.height / this.yDivisions))) {
			throw new Exception("Kernel width or Kernel height exceeds dimensions of sub-images");
		}
		this.subImageCoordinates.clear();
		int noOfDivisions = this.xDivisions * this.yDivisions;
		for (int i = 0; i < noOfDivisions; i++) {
			final boolean topEdge = ((i / this.xDivisions) == 0);
			final boolean rightEdge = ((i % this.xDivisions) == (this.xDivisions - 1));
			final boolean bottomEdge = (this.xDivisions >= (noOfDivisions - i));
			final boolean leftEdge = ((i % this.xDivisions) == 0);
			final int cx = leftEdge ? 0 : ((i % this.xDivisions) * (this.width / this.xDivisions)) - (kernelWidth / 2);
			final int cy = topEdge ? 0 : ((i / this.xDivisions) * (this.height / this.yDivisions)) - (kernelHeight / 2);
			final int ex = (rightEdge || (this.xDivisions == 1)) ? this.width - 1
					: ((this.width / this.xDivisions) * ((i + 1) % this.xDivisions)) + ((kernelWidth / 2) - 1);
			final int ey = (bottomEdge || (this.yDivisions == 1)) ? this.height - 1
					: ((this.height / this.yDivisions) * ((i / this.xDivisions) + 1)) + ((kernelHeight / 2) - 1);
			final int cWidth = (ex - cx) + 1;
			final int cHeight = (ey - cy) + 1;
			this.subImageCoordinates
					.add(new SBSubImageCoordinates(cx, cy, cHeight, cWidth, topEdge, rightEdge, bottomEdge, leftEdge));
		}
	}

	public ArrayList<SBSubImageCoordinates> calculateSubImageCoordinates(XYDivisions divisions, int kernelHeight,
			int kernelWidth) throws Exception {
		if ((kernelWidth >= (this.width / divisions.xDivisions))
				|| (kernelHeight >= (this.height / divisions.yDivisions))) {
			throw new Exception("Kernel width or Kernel height exceeds dimensions of sub-images");
		}
		ArrayList<SBSubImageCoordinates> al = new ArrayList<SBSubImageCoordinates>(64);
		int noOfDivisions = divisions.xDivisions * divisions.yDivisions;
		for (int i = 0; i < noOfDivisions; i++) {
			final boolean topEdge = ((i / divisions.xDivisions) == 0);
			final boolean rightEdge = ((i % divisions.xDivisions) == (divisions.xDivisions - 1));
			final boolean bottomEdge = (divisions.xDivisions >= (noOfDivisions - i));
			final boolean leftEdge = ((i % divisions.xDivisions) == 0);
			final int cx = leftEdge ? 0
					: ((i % divisions.xDivisions) * (this.width / divisions.xDivisions)) - (kernelWidth / 2);
			final int cy = topEdge ? 0
					: ((i / divisions.xDivisions) * (this.height / divisions.yDivisions)) - (kernelHeight / 2);
			final int ex = (rightEdge || (divisions.xDivisions == 1)) ? this.width - 1
					: ((this.width / divisions.xDivisions) * ((i + 1) % divisions.xDivisions))
							+ ((kernelWidth / 2) - 1);
			final int ey = (bottomEdge || (divisions.yDivisions == 1)) ? this.height - 1
					: ((this.height / divisions.yDivisions) * ((i / divisions.xDivisions) + 1))
							+ ((kernelHeight / 2) - 1);
			final int cWidth = (ex - cx) + 1;
			final int cHeight = (ey - cy) + 1;
			al.add(new SBSubImageCoordinates(cx, cy, cHeight, cWidth, topEdge, rightEdge, bottomEdge, leftEdge));
		}
		return al;
	}

	public SBImage[] createSubImageArray() throws Exception {
		this.populateSubImageCoordinates();
		SBImage[] out = new SBImage[this.xDivisions * this.yDivisions];
		for (int i = 0; i < (this.xDivisions * this.yDivisions); i++) {
			out[i] = createSubImage(this, this.subImageCoordinates.get(i));
		}
		return out;
	}

	public SBImage[] createSubImageArray(XYDivisions divisions) throws Exception {
		ArrayList<SBSubImageCoordinates> siCoords = this.calculateSubImageCoordinates(divisions);
		SBImage[] out = new SBImage[divisions.xDivisions * divisions.yDivisions];
		for (int i = 0; i < (divisions.xDivisions * divisions.yDivisions); i++) {
			out[i] = createSubImage(this, siCoords.get(i));
		}
		return out;
	}

	public SBImage[] createSubImageArray(int kernelHeight, int kernelWidth) throws Exception {
		this.populateSubImageCoordinates(kernelHeight, kernelWidth);
		SBImage[] out = new SBImage[this.xDivisions * this.yDivisions];
		for (int i = 0; i < (this.xDivisions * this.yDivisions); i++) {
			out[i] = createSubImage(this, this.subImageCoordinates.get(i));
		}
		return out;
	}

	public SBImage[] createSubImageArray(XYDivisions divisions, int kernelHeight, int kernelWidth) throws Exception {
		ArrayList<SBSubImageCoordinates> siCoords = this.calculateSubImageCoordinates(divisions, kernelHeight,
				kernelWidth);
		// for (int i = 0; i < siCoords.size(); ++i) {
		// System.out.println(siCoords.get(i));
		// }
		SBImage[] out = new SBImage[divisions.xDivisions * divisions.yDivisions];
		for (int i = 0; i < (divisions.xDivisions * divisions.yDivisions); i++) {
			out[i] = createSubImage(this, siCoords.get(i));
		}
		return out;
	}

	public static SBImage[] createSubImageArray(SBImage image) throws Exception {
		return image.createSubImageArray();
	}

	public static SBImage[] createSubImageArray(SBImage image, int kernelHeight, int kernelWidth) throws Exception {
		return image.createSubImageArray(kernelHeight, kernelWidth);
	}

	public static SBImage getSBImageFromBufferedImage(BufferedImage input) throws InterruptedException {
		return getSBImageFromBufferedImage(input, true, false);
	}

	public static SBImage getSBImageFromBufferedImage(BufferedImage input, boolean removeBorders)
			throws InterruptedException {
		return getSBImageFromBufferedImage(input, true, removeBorders);
	}

	public static SBImage getSBImageFromBufferedImage(BufferedImage input, boolean subdivide, boolean removeBorders)
			throws InterruptedException {
		return getSBImageFromBufferedImage(input, subdivide, removeBorders, false);
	}

	public static SBImage getSBImageFromBufferedImageNoParallelise(BufferedImage input, boolean subdivide,
			boolean removeBorders) {
		return getSBImageFromBufferedImageNoParallelise(input, subdivide, removeBorders, false);
	}

	// Average time taken is 36-40 ms to create an SBImage from a BufferedImage
	public static SBImage getSBImageFromBufferedImage(BufferedImage input, boolean subdivide, boolean removeBorders,
			boolean createByteBuffer) throws InterruptedException {
		if ((input.getType() == BufferedImage.TYPE_INT_ARGB) || (input.getType() == BufferedImage.TYPE_INT_RGB)
				|| (input.getType() == BufferedImage.TYPE_INT_BGR) || (input.getType() == BufferedImage.TYPE_4BYTE_ABGR)
				|| (input.getType() == BufferedImage.TYPE_4BYTE_ABGR)
				|| (input.getType() == BufferedImage.TYPE_4BYTE_ABGR_PRE)
				|| (input.getType() == BufferedImage.TYPE_INT_ARGB_PRE)) {
			// Note: If the original DataBufferByte is wrapped and returned, then the
			// BufferedImage will not be garbage collected as it'll retain a reference to
			// the same DataBuffer, so remember to deepCopy the DataBuffer
			try {
				// DataBufferInt dbi = (DataBufferInt) (input.getData().getDataBuffer());
				DataBufferInt dbi = (DataBufferInt) (input.getRaster().getDataBuffer());
				// System.out.println("Here in getSBImageFromBufferedImage()");
				return new SBImage(dbi, input.getHeight(), subdivide, removeBorders, createByteBuffer);
			} catch (Exception e) {
				// No need to do anything. Execute the part after the end of this if statement
			}
		}
		// System.out.println("Here in getSBImageFromBufferedImage()");
		final BufferedImage temp = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = temp.createGraphics();
		setGraphics2DParameters(g);
		g.drawImage(input, 0, 0, null);
		g.dispose();
		// Note: If the original DataBufferByte is wrapped and returned, then the
		// BufferedImage will not be garbage collected as it'll retain a reference to
		// the same DataBuffer, so remember to deepCopy the DataBuffer
		// return new SBImage((DataBufferInt) temp.getData().getDataBuffer(),
		// temp.getHeight(), subdivide, true, true);
		return new SBImage((DataBufferInt) (temp.getRaster().getDataBuffer()), temp.getHeight(), subdivide,
				removeBorders, createByteBuffer);
	}

	public static SBImage getSBImageFromBufferedImageNoParallelise(BufferedImage input, boolean subdivide,
			boolean removeBorders, boolean createByteBuffer) {
		if ((input.getType() == BufferedImage.TYPE_INT_ARGB) || (input.getType() == BufferedImage.TYPE_INT_RGB)
				|| (input.getType() == BufferedImage.TYPE_INT_BGR) || (input.getType() == BufferedImage.TYPE_4BYTE_ABGR)
				|| (input.getType() == BufferedImage.TYPE_4BYTE_ABGR)
				|| (input.getType() == BufferedImage.TYPE_4BYTE_ABGR_PRE)
				|| (input.getType() == BufferedImage.TYPE_INT_ARGB_PRE)) {
			// Note: If the original DataBufferByte is wrapped and returned, then the
			// BufferedImage will not be garbage collected as it'll retain a reference to
			// the same DataBuffer, so remember to deepCopy the DataBuffer
			try {
				// DataBufferInt dbi = (DataBufferInt) (input.getData().getDataBuffer());
				DataBufferInt dbi = (DataBufferInt) (input.getRaster().getDataBuffer());
				// System.out.println("Here in getSBImageFromBufferedImage()");
				return new SBImage(dbi, input.getHeight(), subdivide, removeBorders, createByteBuffer, false);
			} catch (Exception e) {
				// No need to do anything. Execute the part after the end of this if statement
			}
		}
		// System.out.println("Here in getSBImageFromBufferedImage()");
		final BufferedImage temp = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = temp.createGraphics();
		setGraphics2DParameters(g);
		g.drawImage(input, 0, 0, null);
		g.dispose();
		// Note: If the original DataBufferByte is wrapped and returned, then the
		// BufferedImage will not be garbage collected as it'll retain a reference to
		// the same DataBuffer, so remember to deepCopy the DataBuffer
		// return new SBImage((DataBufferInt) temp.getData().getDataBuffer(),
		// temp.getHeight(), subdivide, true, true);
		return new SBImage((DataBufferInt) (temp.getRaster().getDataBuffer()), temp.getHeight(), subdivide,
				removeBorders, createByteBuffer, false);
	}

	// Average time taken is 14-16 ms to create a BufferedImage from an SBImage
	public static BufferedImage getBufferedImageFromSBImage(SBImage input) {
		final int[] pixels = SBImageArrayUtils.parallelIterateFlatten(input.pixels);
		final BufferedImage temp = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
		temp.getRaster().setDataElements(0, 0, input.width, input.height, pixels);
		temp.setRGB(0, 0, 255);
		return temp;
	}

	public static BufferedImage getByteBufferedImageFromSBImage(SBImage input) {
		final int[] pixels = SBImageArrayUtils.parallelIterateFlatten(input.pixels);
		final BufferedImage temp = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
		temp.getRaster().setDataElements(0, 0, input.width, input.height, pixels);
		temp.setRGB(0, 0, 255);
		final BufferedImage byteImage = new BufferedImage(input.width, input.height, BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g2d = byteImage.createGraphics();
		setGraphics2DParameters(g2d);
		g2d.drawImage(temp, 0, 0, null);
		g2d.dispose();
		return byteImage;
	}

	public int[][] getPixelArray() {
		return this.pixels;
	}

	/* Doesn't work as a byte[] array cannot be copied into an int[] array) */
	// public int[] toIntArrayUsingArrayCopy() {
	// int[] copy = new int[this.pixels.getData().length];
	// System.arraycopy(this.pixels.getData(), 0, copy, 0, copy.length);
	// Arrays.parallelSetAll(copy, (x) -> copy[x] - MIN);
	// return copy;
	// }

	// this "for loop" method is 75% faster than streams; takes 25 ms to convert a
	// byte array of about 20 M to int[][]
	// Basically, returns a copy of the array
	public int[][] toIntArray() {
		final int[][] intData = new int[this.width][this.height];
		int i = 0;
		int j = 0;
		for (int[] row : this.pixels) {
			j = 0;
			for (int element : row) {
				intData[i][j++] = element;
			}
			++i;
		}
		return intData;
	}

	public int getPixel(int x, int y) {
		// Note: The reversal, due to how arrays store data
		return this.pixels[y][x];
	}

	public IntStream getIntStream() {
		return SBImageArrayUtils.intStream(SBImageArrayUtils.flatten(this.pixels));
	}

	@Override
	public SBImage clone() {
		final int[][] data = new int[this.height][this.width];
		int i = 0;
		for (int[] row : this.pixels) {
			data[i++] = Arrays.copyOf(row, this.width);
		}
		try {
			return new SBImage(data);
		} catch (InterruptedException ie) {

		}
		return null;
	}

	public static SBImage createSubImage(SBImage parent, SBSubImageCoordinates subImage) throws InterruptedException {
		int[][] out = new int[subImage.height][subImage.width];
		for (int i = 0; i < subImage.height; i++) {
			int[] mainRow = parent.pixels[subImage.startY + i];
			System.arraycopy(mainRow, subImage.startX, out[i], 0, subImage.width);
		}
		return new SBImage(out);
	}

	public static SBImage parallelStitchSubImages(final SBImage[] images, final int xDivisions, final int yDivisions)
			throws InterruptedException {
		// System.out.println("Stitching " + images.length + " sub images.");
		int cWidth = 0;
		int cHeight = 0;
		for (int i = 0; i < xDivisions; i++) {
			cWidth += images[i].width;
		}
		for (int i = 0; i < yDivisions; i++) {
			cHeight += images[i * xDivisions].height;
		}
		final int width = cWidth; // hack to make width reference-able inside the CompletableFuture
		final int height = cHeight; // hack to make width reference-able inside the CompletableFuture
		final int[][] newImageData = new int[height][width];
		final ExecutorService threadService = Executors.newFixedThreadPool(yDivisions);
		final ArrayList<CompletableFuture<Object>> threads = new ArrayList<CompletableFuture<Object>>(yDivisions);
		for (int i = 0; i < yDivisions; i++) {
			final int baseY = i * (height / yDivisions);
			final int yDiv = i;
			threads.add(CompletableFuture.supplyAsync(() -> {
				for (int j = 0; j < xDivisions; j++) {
					final int baseX = j * (width / xDivisions);
					final int[][] temp = images[(yDiv * xDivisions) + j].pixels;
					int yOffset = 0;
					for (int[] row : temp) {
						final int cy = baseY + yOffset;
						int xOffset = 0;
						for (int element : row) {
							final int cx = baseX + xOffset;
							newImageData[cy][cx] = temp[yOffset][xOffset];
							++xOffset;
						}
						++yOffset;
					}
				}
				return Boolean.TRUE;
			}, threadService));
		}
		CompletableFuture.allOf(threads.toArray(new CompletableFuture[threads.size()])).join();
		threadService.shutdown();
		return new SBImage(newImageData);
	}

	public static SBImage stitchSubImages(final SBImage[] images, final int xDivisions, final int yDivisions)
			throws InterruptedException {
		// System.out.println("Stitching " + images.length + " sub images.");
		int cWidth = 0;
		int cHeight = 0;
		for (int i = 0; i < xDivisions; i++) {
			cWidth += images[i].width;
		}
		for (int i = 0; i < yDivisions; i++) {
			cHeight += images[i * xDivisions].height;
		}
		final int width = cWidth; // hack to make width reference-able inside the CompletableFuture
		final int height = cHeight; // hack to make width reference-able inside the CompletableFuture
		final int[][] newImageData = new int[height][width];
		for (int i = 0; i < yDivisions; i++) {
			final int baseY = i * (height / yDivisions);
			final int yDiv = i;
			for (int j = 0; j < xDivisions; j++) {
				final int baseX = j * (width / xDivisions);
				final int[][] temp = images[(yDiv * xDivisions) + j].pixels;
				int yOffset = 0;
				for (int[] row : temp) {
					final int cy = baseY + yOffset;
					int xOffset = 0;
					for (int element : row) {
						final int cx = baseX + xOffset;
						newImageData[cy][cx] = temp[yOffset][xOffset];
						++xOffset;
					}
					++yOffset;
				}
			}
		}
		return new SBImage(newImageData);
	}

	/*
	 * The basic idea is: SBImage[] subImages =
	 * image.createSubImageArray(kernelHeight, kernelWidth); SBImage[] newSubImages
	 * = new SBImage[image.xDivisions*image.yDivisions]; for (int i = 0; i <
	 * newSubImages.length; ++i) { // Inside the method someOperation, remember to
	 * set the borders of the newSubImage, as they would be needed for the edge
	 * tiles newSubImages[i] = SBImageUtils.someOperation(subImages[i],kernelHeight,
	 * kernelWidth); } SBImage processedImage =
	 * SBImage.stitchSubImages(newSubImages,kernelHeight, kernelWidth);
	 */

	public static SBImage parallelStitchSubImages(final SBImage[] images, final int xDivisions, final int yDivisions,
			final int kernelHeight, final int kernelWidth) throws InterruptedException {
		// System.out.println("Stitching " + images.length + " sub images.");
		int cWidth = 0;
		int cHeight = 0;
		for (int i = 0; i < xDivisions; i++) {
			cWidth += images[i].width;
		}
		// System.out.println("Width :" + width);
		final int width = cWidth - ((xDivisions - 1) * (kernelWidth / 2) * 2);
		for (int i = 0; i < yDivisions; i++) {
			cHeight += images[i * xDivisions].height;
		}
		// System.out.println("Height :" + height);
		final int height = cHeight - ((yDivisions - 1) * (kernelHeight / 2) * 2);
		// System.out.println("Height : " + height + "; Width :" + width);
		int[][] newImageData = new int[height][width];
		final ExecutorService threadService = Executors.newFixedThreadPool(yDivisions);
		final ArrayList<CompletableFuture<Object>> threads = new ArrayList<CompletableFuture<Object>>(yDivisions);
		for (int i = 0; i < yDivisions; ++i) {
			boolean topEdge = (i == 0);
			boolean bottomEdge = (i == (yDivisions - 1));
			final int baseY = i * (height / yDivisions);
			final int yOffset = topEdge ? 0 : (kernelHeight / 2);
			final int yDiv = i;
			threads.add(CompletableFuture.supplyAsync(() -> {
				for (int j = 0; j < xDivisions; ++j) {
					boolean leftEdge = (j == 0);
					boolean rightEdge = (j == (xDivisions - 1));
					final int baseX = j * (width / xDivisions);
					final int xOffset = leftEdge ? 0 : (kernelWidth / 2);
					final int[][] temp = images[(yDiv * xDivisions) + j].pixels;
					final int imageWidth = images[(yDiv * xDivisions) + j].width;
					final int imageHeight = images[(yDiv * xDivisions) + j].height;
					int rowCount = 0;
					for (int[] row : temp) {
						if (!bottomEdge) {
							if ((rowCount < yOffset) || (rowCount >= (imageHeight - (kernelHeight / 2)))) {
								++rowCount;
								continue;
							}
						} else {
							if (rowCount < yOffset) {
								++rowCount;
								continue;
							}
						}
						final int cy = (baseY + rowCount) - yOffset;
						int columnCount = 0;
						for (int element : row) {
							if (!rightEdge) {
								if ((columnCount < xOffset) || (columnCount >= (imageWidth - (kernelWidth / 2)))) {
									++columnCount;
									continue;
								}
							} else {
								if (columnCount < xOffset) {
									++columnCount;
									continue;
								}
							}
							final int cx = (baseX + columnCount) - xOffset;
							// newImageData[cy][cx] = temp[rowCount][columnCount];
							newImageData[cy][cx] = element;
							++columnCount;
						}
						++rowCount;
					}
				}
				return Boolean.TRUE;
			}, threadService));
		}
		CompletableFuture.allOf(threads.toArray(new CompletableFuture[threads.size()])).join();
		threadService.shutdown();
		return new SBImage(newImageData);
	}

	public static SBImage stitchSubImages(final SBImage[] images, final int xDivisions, final int yDivisions,
			final int kernelHeight, final int kernelWidth) throws InterruptedException {
		// System.out.println("Stitching " + images.length + " sub images.");
		int width = 0;
		int height = 0;
		for (int i = 0; i < xDivisions; i++) {
			width += images[i].width;
		}
		// System.out.println("Width :" + width);
		width = width - ((xDivisions - 1) * (kernelWidth / 2) * 2);
		for (int i = 0; i < yDivisions; i++) {
			height += images[i * xDivisions].height;
		}
		// System.out.println("Height :" + height);
		height = height - ((yDivisions - 1) * (kernelHeight / 2) * 2);
		// System.out.println("Height : " + height + "; Width :" + width);
		int[][] newImageData = new int[height][width];
		for (int i = 0; i < yDivisions; ++i) {
			final boolean topEdge = (i == 0);
			final boolean bottomEdge = (i == (yDivisions - 1));
			final int baseY = i * (height / yDivisions);
			final int yOffset = topEdge ? 0 : (kernelHeight / 2);
			for (int j = 0; j < xDivisions; ++j) {
				boolean leftEdge = (j == 0);
				boolean rightEdge = (j == (xDivisions - 1));
				final int baseX = j * (width / xDivisions);
				final int xOffset = leftEdge ? 0 : (kernelWidth / 2);
				final int[][] temp = images[(i * xDivisions) + j].pixels;
				final int imageWidth = images[(i * xDivisions) + j].width;
				final int imageHeight = images[(i * xDivisions) + j].height;
				int rowCount = 0;
				for (int[] row : temp) {
					if (!bottomEdge) {
						if ((rowCount < yOffset) || (rowCount >= (imageHeight - (kernelHeight / 2)))) {
							++rowCount;
							continue;
						}
					} else {
						if (rowCount < yOffset) {
							++rowCount;
							continue;
						}
					}
					final int cy = (baseY + rowCount) - yOffset;
					int columnCount = 0;
					for (int element : row) {
						if (!rightEdge) {
							if ((columnCount < xOffset) || (columnCount >= (imageWidth - (kernelWidth / 2)))) {
								++columnCount;
								continue;
							}
						} else {
							if (columnCount < xOffset) {
								++columnCount;
								continue;
							}
						}
						final int cx = (baseX + columnCount) - xOffset;
						// newImageData[cy][cx] = temp[rowCount][columnCount];
						newImageData[cy][cx] = element;
						++columnCount;
					}
					++rowCount;
				}
			}
		}
		return new SBImage(newImageData);
	}

	public static int findTopBorderWidth(SBImage image) {
		return findTopBorderWidth(image.pixels);
	}

	public static int findTopBorderWidth(int[][] image) {
		int borderHeight = -1;
		// int indexWhite;
		// int indexBlack;
		boolean allWhite = true;
		boolean allBlack = true;
		int whiteCount = 0;
		int blackCount = 0;
		mainloop: for (int[] row : image) {
			++borderHeight;
			whiteCount = 0;
			blackCount = 0;
			for (int pixel : row) {
				// indexWhite = Arrays.binarySearch(whitePixels, pixel);
				// if (indexWhite >= 0) {
				if (pixel >= whitePixels[0]) {
					whiteCount++;
				}
				// indexBlack = Arrays.binarySearch(blackPixels, pixel);
				// if (indexBlack >= 0) {
				if (pixel <= blackPixels[blackPixels.length - 1]) {
					blackCount++;
				}
			}
			if (((whiteCount * 1.0) / image[0].length) > 0.995) {
				allWhite = true;
			} else {
				allWhite = false;
			}
			// System.out.println("Row : " + deviantRow + " ; whiteCount % age : " +
			// ((whiteCount * 1.0) / image.width));
			if (((blackCount * 1.0) / image[0].length) > 0.995) {
				allBlack = true;
			} else {
				allBlack = false;
			}
			// System.out.println("Row : " + deviantRow + " ; blackCount % age : " +
			// ((blackCount * 1.0) / image.width));
			if ((((whiteCount + blackCount) * 1.0) / image[0].length) > 0.995) {
				continue;
			}
			if (!allWhite && !allBlack) {
				break mainloop;
			}
		}
		return borderHeight;
	}

	public static int findBottomBorderWidth(SBImage image) {
		return findBottomBorderWidth(image.pixels);
	}

	public static int findBottomBorderWidth(int[][] image) {
		// int indexWhite;
		// int indexBlack;
		boolean allWhite = true;
		boolean allBlack = true;
		int whiteCount = 0;
		int blackCount = 0;
		int height = image.length;
		mainloop: for (int i = 0; i < height; i++) {
			--height;
			whiteCount = 0;
			blackCount = 0;
			for (int pixel : image[height]) {
				// indexWhite = Arrays.binarySearch(whitePixels, pixel);
				// if (indexWhite >= 0) {
				if (pixel >= whitePixels[0]) {
					whiteCount++;
				}
				// indexBlack = Arrays.binarySearch(blackPixels, pixel);
				// if (indexBlack >= 0) {
				if (pixel <= blackPixels[blackPixels.length - 1]) {
					blackCount++;
				}
			}
			if (((whiteCount * 1.0) / image[0].length) > 0.995) {
				allWhite = true;
			} else {
				allWhite = false;
			}
			// System.out.println("Row : " + deviantRow + " ; whiteCount % age : " +
			// ((whiteCount * 1.0) / image.width));
			if (((blackCount * 1.0) / image[0].length) > 0.995) {
				allBlack = true;
			} else {
				allBlack = false;
			}
			// System.out.println("Row : " + deviantRow + " ; blackCount % age : " +
			// ((blackCount * 1.0) / image.width));
			if ((((whiteCount + blackCount) * 1.0) / image[0].length) > 0.995) {
				continue;
			}
			if (!allWhite && !allBlack) {
				break mainloop;
			}
		}
		return (image.length - height) - 1;
	}

	public static int findLeftBorderWidth(SBImage image) {
		return findLeftBorderWidth(image.pixels);
	}

	public static int findLeftBorderWidth(int[][] image) {
		// int indexWhite;
		// int indexBlack;
		boolean allWhite = true;
		boolean allBlack = true;
		int whiteCount = 0;
		int blackCount = 0;
		int borderWidth = -1;
		int pixel = 0;
		mainloop: for (int i = 0; i < image[0].length; i++) {
			++borderWidth;
			whiteCount = 0;
			blackCount = 0;
			for (int[] element : image) {
				pixel = element[borderWidth];
				// indexWhite = Arrays.binarySearch(whitePixels, pixel);
				// if (indexWhite >= 0) {
				if (pixel >= whitePixels[0]) {
					whiteCount++;
				}
				// indexBlack = Arrays.binarySearch(blackPixels, pixel);
				// if (indexBlack >= 0) {
				if (pixel <= blackPixels[blackPixels.length - 1]) {
					blackCount++;
				}
			}
			if (((whiteCount * 1.0) / image.length) > 0.995) {
				allWhite = true;
			} else {
				allWhite = false;
			}
			// System.out.println("Row : " + deviantRow + " ; whiteCount % age : " +
			// ((whiteCount * 1.0) / image.width));
			if (((blackCount * 1.0) / image.length) > 0.995) {
				allBlack = true;
			} else {
				allBlack = false;
			}
			// System.out.println("Row : " + deviantRow + " ; blackCount % age : " +
			// ((blackCount * 1.0) / image.width));
			if ((((whiteCount + blackCount) * 1.0) / image.length) > 0.995) {
				continue;
			}
			if (!allWhite && !allBlack) {
				break mainloop;
			}
		}
		return borderWidth;
	}

	public static int findRightBorderWidth(SBImage image) {
		return findRightBorderWidth(image.pixels);
	}

	public static int findRightBorderWidth(int[][] image) {
		// int indexWhite;
		// int indexBlack;
		boolean allWhite = true;
		boolean allBlack = true;
		int whiteCount = 0;
		int blackCount = 0;
		int borderLimit = image[0].length;
		int pixel = 0;
		mainloop: for (int i = 0; i < image[0].length; i++) {
			--borderLimit;
			whiteCount = 0;
			blackCount = 0;
			for (int[] element : image) {
				pixel = element[borderLimit];
				// indexWhite = Arrays.binarySearch(whitePixels, pixel);
				// if (indexWhite >= 0) {
				if (pixel >= whitePixels[0]) {
					whiteCount++;
				}
				// indexBlack = Arrays.binarySearch(blackPixels, pixel);
				// if (indexBlack >= 0) {
				if (pixel <= blackPixels[blackPixels.length - 1]) {
					blackCount++;
				}
			}
			if (((whiteCount * 1.0) / image.length) > 0.995) {
				allWhite = true;
			} else {
				allWhite = false;
			}
			// System.out.println("Row : " + deviantRow + " ; whiteCount % age : " +
			// ((whiteCount * 1.0) / image.width));
			if (((blackCount * 1.0) / image.length) > 0.995) {
				allBlack = true;
			} else {
				allBlack = false;
			}
			// System.out.println("Row : " + deviantRow + " ; blackCount % age : " +
			// ((blackCount * 1.0) / image.width));
			if ((((whiteCount + blackCount) * 1.0) / image.length) > 0.995) {
				continue;
			}
			if (!allWhite && !allBlack) {
				break mainloop;
			}
		}
		return (image[0].length - borderLimit) - 1;
	}

	public static int[][] removeBorders(int[][] image) {
		int leftBorder = findLeftBorderWidth(image);
		int topBorder = findTopBorderWidth(image);
		int rightBorder = findRightBorderWidth(image);
		int bottomBorder = findBottomBorderWidth(image);
		int subImageX = leftBorder;
		int subImageY = topBorder;
		int width = image[0].length - leftBorder - rightBorder;
		int height = image.length - topBorder - bottomBorder;
		// System.out.format("Calling extract with x = %d, y = %d, width = %d, height =
		// %d ", subImageX, subImageY, width,
		// height).println();
		return extractRectangle(image, subImageX, subImageY, height, width);
	}

	public static int[][] extractRectangle(int[][] image, int x, int y, int height, int width) {
		int[][] out = new int[height][width];
		for (int i = y; i < (y + height); i++) {
			int[] row = image[i];
			System.arraycopy(row, x, out[i - y], 0, width);
		}
		return out;
	}

	public NumberTriplet getMeanAndSD() {
		synchronized (SBImage.this.meanAndSD) {
			return this.meanAndSD;
		}
	}

	public void populateByteBuffer() {
		if (this.byteBuffer == null) {
			// byte[] pixelData = SBImageArrayUtils.iterateFlattenToByteArray(this.pixels);
			// System.out.println(Arrays.toString(pixelData));
			if (this.underlyingBuffImage == null) {
				// int[] pixs = SBImageArrayUtils.iterateFlatten(this.pixels);
				int[] pixs = SBImageArrayUtils.parallelIterateFlatten(this.pixels);
				final BufferedImage temp1 = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
				final BufferedImage temp2 = new BufferedImage(this.width, this.height, BufferedImage.TYPE_BYTE_GRAY);
				temp1.getRaster().setDataElements(0, 0, this.width, this.height, pixs);
				Graphics2D g = temp2.createGraphics();
				g.drawImage(temp1, 0, 0, null);
				g.dispose();
				DataBuffer buff = temp2.getRaster().getDataBuffer();
				byte[] pixelData = ((DataBufferByte) buff).getData();
				this.byteBuffer = ByteBuffer.allocateDirect(pixelData.length);
				this.byteBuffer.order(ByteOrder.nativeOrder());
				this.byteBuffer.put(pixelData);
				((Buffer) this.byteBuffer).flip();
				int bpp = temp2.getColorModel().getPixelSize();
				this.bytesPerPixel = bpp / 8;
				this.bytesPerLine = (int) Math.ceil((this.width * bpp) / 8.0);
				// System.out.println(Arrays.toString(this.byteBuffer.array()));
			} else {
				if (this.underlyingBuffImage.getType() == BufferedImage.TYPE_BYTE_GRAY) {
					DataBuffer buff = this.underlyingBuffImage.getRaster().getDataBuffer();
					byte[] pixelData = ((DataBufferByte) buff).getData();
					this.byteBuffer = ByteBuffer.allocateDirect(pixelData.length);
					this.byteBuffer.order(ByteOrder.nativeOrder());
					this.byteBuffer.put(pixelData);
					((Buffer) this.byteBuffer).flip();
					int bpp = this.underlyingBuffImage.getColorModel().getPixelSize();
					this.bytesPerPixel = bpp / 8;
					this.bytesPerLine = (int) Math.ceil((this.width * bpp) / 8.0);
				} else {
					final BufferedImage temp2 = new BufferedImage(this.width, this.height,
							BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D g = temp2.createGraphics();
					g.drawImage(this.underlyingBuffImage, 0, 0, null);
					g.dispose();
					DataBuffer buff = temp2.getRaster().getDataBuffer();
					byte[] pixelData = ((DataBufferByte) buff).getData();
					this.byteBuffer = ByteBuffer.allocateDirect(pixelData.length);
					this.byteBuffer.order(ByteOrder.nativeOrder());
					this.byteBuffer.put(pixelData);
					((Buffer) this.byteBuffer).flip();
					int bpp = temp2.getColorModel().getPixelSize();
					this.bytesPerPixel = bpp / 8;
					this.bytesPerLine = (int) Math.ceil((this.width * bpp) / 8.0);
					this.underlyingBuffImage = temp2;
				}
			}
		}
	}

	public static SBImage getSBImageFromPix(Pix pix, int debugL) throws InterruptedException, IOException {

		return getSBImageFromPix(pix, false, false, false, debugL);
	}

	public static SBImage getSBImageFromPix(Pix pix, boolean subdivide, boolean removeBorders, boolean createByteBuffer,
			int debugL) throws InterruptedException, IOException {
		/*
		 * PointerByReference pdata = new PointerByReference(); NativeSizeByReference
		 * psize = new NativeSizeByReference(); int format = IFF_TIFF;
		 * Leptonica.INSTANCE.pixWriteMem(pdata, psize, pix, format); byte[] b =
		 * pdata.getValue().getByteArray(0, psize.getValue().intValue()); int height =
		 * pix.h; int[][] data = SBImageArrayUtils.parallelFrom1DTo2D(b, height);
		 * Leptonica1.lept_free(pdata.getValue()); return new SBImage(data, subdivide,
		 * removeBorders, createByteBuffer);
		 */
		// return SBImage.getSBImageFromBufferedImage(LeptUtils.convertPixToImage(pix));
		return SBImage.getSBImageFromBufferedImage(SBImageUtils.convertPixToImage(pix, debugL));
	}

	public static SBImage getSBImageFromPix1(Pix pix) throws InterruptedException, IOException {

		return getSBImageFromPix1(pix, false, false, false);
	}

	public static SBImage getSBImageFromPix1(Pix pix, boolean subdivide, boolean removeBorders,
			boolean createByteBuffer) throws InterruptedException, IOException {
		int height = Leptonica.INSTANCE.pixGetHeight(pix);
		int width = Leptonica1.pixGetWidth(pix);
		int depth = Leptonica1.pixGetDepth(pix);
		int[][] pixels = new int[height][width];
		for (int i = 0; i < height; ++i) {
			for (int j = 0; j < width; ++j) {
				IntBuffer value = IntBuffer.allocate(1);
				Leptonica1.pixGetPixel(pix, j, i, value);
				if (depth == 8) {
					pixels[i][j] = value.get();
				}
				if (depth == 1) {
					pixels[i][j] = (value.get() == 1) ? 0 : 255;
				}
				if (depth == 32) {
					pixels[i][j] = value.get() & 0xFF;
				}
			}
		}
		return new SBImage(pixels, false, false, false);
	}

	public static Pix getPixFromSBImage1(SBImage image) {
		int[][] pixels = image.pixels;
		int height = pixels.length;
		int width = pixels[0].length;
		Pix pix = Leptonica.INSTANCE.pixCreate(width, height, 8);
		// Leptonica1.pixSetBlackOrWhite(pix, ILeptonica.L_SET_WHITE);
		for (int i = 0; i < height; ++i) {
			for (int j = 0; j < width; ++j) {
				Leptonica1.pixSetPixel(pix, j, i, pixels[i][j]);
			}
		}
		return pix;
	}

	public static Pix getPixFromSBImage(SBImage image) throws InterruptedException, IOException {
		// byte[] b = SBImageArrayUtils.iterateFlattenToByteArray(image.pixels);
		// final BufferedImage temp = new BufferedImage(image.width, image.height,
		// BufferedImage.TYPE_BYTE_GRAY);
		// temp.getRaster().setDataElements(0, 0, image.width, image.height, b);
		// return
		// LeptUtils.convertImageToPix(SBImage.getByteBufferedImageFromSBImage(image));

		// Get tif writer and set output to file
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("tiff");
		ImageWriter writer = null;
		boolean twelveMonkeysWriterFound = false;
		while (writers.hasNext()) {
			writer = writers.next();
			if (writer.getClass().toString().indexOf("twelvemonkeys") != -1) {
				twelveMonkeysWriterFound = true;
				break;
			}
		}

		if (!twelveMonkeysWriterFound) {
			// throw new RuntimeException(
			// "Need to install JAI Image I/O
			// package.\nhttps://github.com/jai-imageio/jai-imageio-core");
			throw new RuntimeException("Need to install TwelveMonkeys Image I/O package");
		}

		// sSystem.out.println(writer.toString());
		// Set up the writeParam
		ImageWriteParam tiffWriteParam = writer.getDefaultWriteParam();
		tiffWriteParam.setCompressionMode(ImageWriteParam.MODE_DISABLED);

		// Get the stream metadata
		IIOMetadata streamMetadata = writer.getDefaultStreamMetadata(tiffWriteParam);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
		writer.setOutput(ios);

		// TIFFImageWriter tiw
		// ios.seek(0);
		// ios.mark();
		writer.write(streamMetadata, new IIOImage(SBImage.getByteBufferedImageFromSBImage(image), null, null),
				tiffWriteParam);
		// writer.write(image);
		// writer.dispose();
		// ios.reset();
		// System.out.println("Stream Position = " + ios.getStreamPosition());
		// System.out.println("Flushed Position = " + ios.getFlushedPosition());
		byte[] b = null;
		try {
			ios.seek(0);
			b = new byte[(int) ios.length()];
			ios.read(b, 0, b.length);
		} catch (Exception e) {
			// e.printStackTrace();
			b = outputStream.toByteArray();
		}
		// System.out.println(Arrays.toString(b));
		ios.close();
		writer.dispose();

		ByteBuffer buf = ByteBuffer.allocateDirect(b.length);
		buf.order(ByteOrder.nativeOrder());
		buf.put(b);
		((Buffer) buf).flip();

		Pix pix = Leptonica1.pixReadMem(buf, new NativeSize(buf.capacity()));

		// System.out.println(pix.w + " " + pix.h + " " + pix.d + " " + pix.spp + " " +
		// pix.wpl + " " + pix.refcount + " "
		// + pix.xres + " " + pix.yres + " " + pix.informat + " " + pix.special + " " +
		// pix.text + " "
		// + pix.colormap + " " + pix.data);

		return pix;

		// return
		// LeptUtils.convertImageToPix(SBImage.getBufferedImageFromSBImage(image));
		// ByteBuffer buf = ByteBuffer.allocateDirect(b.length);
		// buf.order(ByteOrder.nativeOrder());
		// buf.put(b);
		// ((Buffer) buf).flip();
		// Pix pix = Leptonica1.pixReadMem(buf, new NativeSize(buf.capacity()));
		// return pix;
	}

	public static Pix getPixFromBufferedImage(BufferedImage image) throws InterruptedException, IOException {
		// byte[] b = SBImageArrayUtils.iterateFlattenToByteArray(image.pixels);
		// final BufferedImage temp = new BufferedImage(image.width, image.height,
		// BufferedImage.TYPE_BYTE_GRAY);
		// temp.getRaster().setDataElements(0, 0, image.width, image.height, b);
		// return
		// LeptUtils.convertImageToPix(SBImage.getByteBufferedImageFromSBImage(image));

		// Get tif writer and set output to file
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("tiff");
		ImageWriter writer = null;
		boolean twelveMonkeysWriterFound = false;
		while (writers.hasNext()) {
			writer = writers.next();
			if (writer.getClass().toString().indexOf("twelvemonkeys") != -1) {
				twelveMonkeysWriterFound = true;
				break;
			}
		}

		if (!twelveMonkeysWriterFound) {
			// throw new RuntimeException(
			// "Need to install JAI Image I/O
			// package.\nhttps://github.com/jai-imageio/jai-imageio-core");
			throw new RuntimeException("Need to install TwelveMonkeys Image I/O package");
		}

		// sSystem.out.println(writer.toString());
		// Set up the writeParam
		ImageWriteParam tiffWriteParam = writer.getDefaultWriteParam();
		tiffWriteParam.setCompressionMode(ImageWriteParam.MODE_DISABLED);

		// Get the stream metadata
		IIOMetadata streamMetadata = writer.getDefaultStreamMetadata(tiffWriteParam);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
		writer.setOutput(ios);

		// TIFFImageWriter tiw
		// ios.seek(0);
		// ios.mark();
		writer.write(streamMetadata, new IIOImage(image, null, null), tiffWriteParam);
		// writer.write(image);
		// writer.dispose();
		// ios.reset();
		// System.out.println("Stream Position = " + ios.getStreamPosition());
		// System.out.println("Flushed Position = " + ios.getFlushedPosition());
		byte[] b = null;
		try {
			ios.seek(0);
			b = new byte[(int) ios.length()];
			ios.read(b, 0, b.length);
		} catch (Exception e) {
			// e.printStackTrace();
			b = outputStream.toByteArray();
		}
		// System.out.println(Arrays.toString(b));
		ios.close();
		writer.dispose();

		ByteBuffer buf = ByteBuffer.allocateDirect(b.length);
		buf.order(ByteOrder.nativeOrder());
		buf.put(b);
		((Buffer) buf).flip();

		Pix pix = Leptonica1.pixReadMem(buf, new NativeSize(buf.capacity()));

		// System.out.println(pix.w + " " + pix.h + " " + pix.d + " " + pix.spp + " " +
		// pix.wpl + " " + pix.refcount + " "
		// + pix.xres + " " + pix.yres + " " + pix.informat + " " + pix.special + " " +
		// pix.text + " "
		// + pix.colormap + " " + pix.data);

		return pix;

		// return
		// LeptUtils.convertImageToPix(SBImage.getBufferedImageFromSBImage(image));
		// ByteBuffer buf = ByteBuffer.allocateDirect(b.length);
		// buf.order(ByteOrder.nativeOrder());
		// buf.put(b);
		// ((Buffer) buf).flip();
		// Pix pix = Leptonica1.pixReadMem(buf, new NativeSize(buf.capacity()));
		// return pix;
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

}