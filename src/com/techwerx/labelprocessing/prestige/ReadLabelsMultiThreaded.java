package com.techwerx.labelprocessing.prestige;

/* Starting Program arguments are :
	initFile="E:/TechWerx/Java/Working/Input/Initialisation Properties/config.properties"
	prodFile="E:/TechWerx/Java/Working/Input/Initialisation Properties/product.properties"
 *
 *
 * Starting VM arguments are :
	-DtesseractLib=libtesseract411
	-DleptLib=liblept1790
	-Xms2048M
	-Xmx3072M
	--module-path E:\\TechWerx\\Java\\javafx-sdk-13.0.1\\lib
	--add-modules=javafx.controls,javafx.media,javafx.fxml
*/

import static net.sourceforge.lept4j.ILeptonica.IFF_TIFF;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.FloatBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.github.jaiimageio.plugins.tiff.BaselineTIFFTagSet;
import com.github.jaiimageio.plugins.tiff.TIFFDirectory;
import com.github.jaiimageio.plugins.tiff.TIFFField;
import com.github.jaiimageio.plugins.tiff.TIFFTag;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import com.sun.jna.ptr.PointerByReference;
import com.techwerx.image.PixAutoCloseable;
import com.techwerx.image.PixCleaningUtils;
import com.techwerx.image.SBImage;
import com.techwerx.image.utils.PixDebugWriter;
import com.techwerx.image.utils.SBImageUtils;
import com.techwerx.image.utils.SysOutController;
import com.techwerx.labelprocessing.OCRStringWrapper;
import com.techwerx.labelprocessing.ProcessingData;
import com.techwerx.labelprocessing.ProductDescription;
import com.techwerx.labelprocessing.prestige.initialise.InitialiseParameters;
import com.techwerx.tesseract.TechWerxTesseract;
import com.techwerx.tesseract.TechWerxTesseractHandle;
import com.techwerx.tesseract.TechWerxTesseractHandleFactory;
import com.techwerx.tesseract.TechWerxTesseractHandlePool;
import com.techwerx.text.OCRBufferedImageWrapper;
import com.techwerx.text.ProcessDataWrapper;
import com.techwerx.text.Resources;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.sourceforge.lept4j.Box;
import net.sourceforge.lept4j.Boxa;
import net.sourceforge.lept4j.ILeptonica;
import net.sourceforge.lept4j.ILeptonica.FILE;
import net.sourceforge.lept4j.L_Bmf;
import net.sourceforge.lept4j.Leptonica;
import net.sourceforge.lept4j.Leptonica1;
import net.sourceforge.lept4j.Pix;
import net.sourceforge.lept4j.Sel;
import net.sourceforge.lept4j.util.LeptUtils;

public class ReadLabelsMultiThreaded extends Application {

	// ThreadPoolExecutor threadPool = null;

	private static ExecutorService parallelThreadPool = Executors.newFixedThreadPool(10);

	private static boolean initialised = false;
	private static Pix seedPix = null;

	private static final String runModeKey = "run.mode";
	private static final String DEBUGGINGIMAGES_DIRECTORY_KEY = "debuggingimages.folder";
	// private static final String DEBUGLEVEL_KEY = "dl";
	// private static final String idealCharHeightKey = "ich";
	// private static final String idealCharWidthKey = "icw";

	private static String DEFAULT_DEBUGGINGIMAGES_DIRECTORY = "";

	private static final String SYSTEM_OUT_IGNORE_KEY = "system.out";
	private static final String SYSTEM_OUT_ACTIVATE_VALUE = "activate";

	private static final String AUTO_REPORT_AFTER_EVERY_WATCH_CYCLE_KEY = "auto.report";
	private static final String AUTO_REPORT_DISABLE = "false";
	private static boolean autoReport = true;

	private static volatile boolean loop = true;
	private static volatile boolean doingProcessingNow = false;
	private static volatile boolean anotherEventOccurred = false;
	private static volatile boolean iteration1Done = false;

	// private static CompletableFuture originalTesseractThread = null;
	private static Pix originalPix8 = null;

	private static L_Bmf bbNumberingFont = Leptonica1.bmfCreate(null, 4);

	private static boolean DEBUG = true;
	public static int debugLevel = 13; // 1 for print all images
	// 2 for certain critical images
	// 3 for tesseract images
	// 4 for symbol images (the ones with bounding box) and the final images for
	// tesseract
	// 5 for only final images and output
	// 6 for output only
	// 7 for output only
	// 8 for final output
	// 9 for no images, no output
	// 10 for no images, no output

	// private static int cleanedImageCounter = 1;
	// private static int symbolBoxNumber = 1;
	// private static int cdfCounter = 1;

	public static final ExecutorService outerThreadService = Executors.newFixedThreadPool(30);
	public static final ExecutorService innerThreadService = Executors.newFixedThreadPool(60);

	// public static final ExecutorService tesseractThreadService = Executors
	// .newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	private static TechWerxTesseractHandlePool techwerxTesseractPool = null;
	private static TechWerxTesseractHandlePool originalImagesTesseractPool = null;

	static {
		// JFXPanel fxPanel = new JFXPanel();
		// Application.launch();
	}

	public static ProcessDataWrapper processDataWrapper = new ProcessDataWrapper();
	private static int idealCharHeight;
	private static int idealCharWidth;

	private static String productFileArgument = null;

	// private static int tempCounter = 1;
	private static String currentFileName = "";
	private static long timeWaitingForImage = 0;
	// private static boolean imageIsEmpty = false;
	private static int tryNumber = 1;
	private static volatile boolean firstShotDone = false;

	private static int targetHeight = 400;
	private static int targetWidth = 600;

	private static Pix contrastNormalisedImage = null;
	private static Pix backgroundNormalisedImage = null;
	private static Pix unsharpMaskedImage = null;
	private static float try1RotationAngle = 0.0f;

	private static Pix try1PreBBImage = null;
	private static Pix try2PreBBImage = null;
	private static Pix try3PreBBImage = null;

	private static Pix try1TesseractImage = null;
	private static Pix try2TesseractImage = null;
	private static Pix try3TesseractImage = null;
	private static Pix try4TesseractImage1 = null;
	private static Pix try4TesseractImage2 = null;
	private static final Object try4TesseractImage1Lock = new Object();
	private static final Object try4TesseractImage2Lock = new Object();

	private static Pix try1BBImage = null;
	private static Pix try2BBImage = null;
	private static Pix try3BBImage = null;

	private static ArrayList<ArrayList<Rectangle>> try1BoundingBoxes = null;
	private static ArrayList<ArrayList<Rectangle>> try2BoundingBoxes = null;
	private static ArrayList<ArrayList<Rectangle>> try3BoundingBoxes = null;

	private static CompletableFuture<Boolean> try1Thread = null;
	private static CompletableFuture<Boolean> try2Thread = null;
	private static CompletableFuture<Boolean> try3Thread = null;
	private static volatile boolean try1ThreadFinished = false;
	private static volatile boolean try2ThreadFinished = false;
	private static volatile boolean try3ThreadFinished = false;

	private static CompletableFuture<Boolean> getImagesAndBoundingBoxesThread1 = null;
	private static CompletableFuture<Boolean> getImagesAndBoundingBoxesThread2 = null;
	private static CompletableFuture<Boolean> getImagesAndBoundingBoxesThread3 = null;
	private static volatile boolean getImagesAndBBThread1Finished = false;
	private static volatile boolean getImagesAndBBThread2Finished = false;
	private static volatile boolean getImagesAndBBThread3Finished = false;

	private static volatile boolean interruptParallelThreads = false;
	private static TechWerxTesseract bnImageTesseract = null;
	private static TechWerxTesseract cnImageTesseract = null;
	private static TechWerxTesseract umImageTesseract = null;

	private static boolean windowMinimised = false;

	private static int isWindows = 2;

	private static Object interruptLock = new Object();
	private static Object originalImagesLock = new Object();

	/*
	 * private static float heightScalingForPoorImages = 0.0f; private static float
	 * widthScalingForPoorImages = 0.0f; private static boolean
	 * foundHeightAndWidthScaling = false; private static int numberOfLines = 0;
	 */

	private static ArrayList<Rectangle> allBoxes = new ArrayList<>();

	private static void resetBoundingBoxes() {
		synchronized (interruptLock) {
			try1BoundingBoxes = null;
			try2BoundingBoxes = null;
			try3BoundingBoxes = null;
		}
	}

	private static void resetInterruptParallelThreads() {
		synchronized (interruptLock) {
			interruptParallelThreads = false;
		}
	}

	private static void setInterruptParallelThreads() {
		synchronized (interruptLock) {
			interruptParallelThreads = true;
		}
	}

	private static boolean getInterruptParallelThreads() {
		synchronized (interruptLock) {
			return interruptParallelThreads;
		}
	}

	private static void resetTryImageThreads() {
		// Check if threads have finished. If not, finish them, and set to null
		if (try1ThreadFinished) {
			try1Thread = null;
			try1ThreadFinished = false;
		} else {
			if (try1Thread != null) {
				try1Thread.join();
			}
			try1Thread = null;
		}
		if (try2ThreadFinished) {
			try2Thread = null;
			try2ThreadFinished = false;
		} else {
			// try2Thread is null when product is NOT given
			if (try2Thread != null) {
				try2Thread.join();
			}
			try2Thread = null;
		}
		if (try3ThreadFinished) {
			try3Thread = null;
			try3ThreadFinished = false;
		} else {
			if (try3Thread != null) {
				try3Thread.join();
			}
			try3Thread = null;
		}
	}

	private static void resetImagesAndBBCreationThreads() {
		// Check if threads have finished. If not, finish them, and set to null
		firstShotDone = true; // releases threads, if any are waiting
		// iteration1Done = false;
		if (getImagesAndBBThread1Finished) {
			getImagesAndBoundingBoxesThread1 = null;
			getImagesAndBBThread1Finished = false;
		} else {
			if (getImagesAndBoundingBoxesThread1 != null) {
				getImagesAndBoundingBoxesThread1.join();
			}
			getImagesAndBoundingBoxesThread1 = null;
		}
		if (getImagesAndBBThread2Finished) {
			getImagesAndBoundingBoxesThread2 = null;
			getImagesAndBBThread2Finished = false;
		} else {
			if (getImagesAndBoundingBoxesThread2 != null) {
				getImagesAndBoundingBoxesThread2.join();
			}
			getImagesAndBoundingBoxesThread2 = null;
		}
		if (getImagesAndBBThread3Finished) {
			getImagesAndBoundingBoxesThread3 = null;
			getImagesAndBBThread3Finished = false;
		} else {
			if (getImagesAndBoundingBoxesThread3 != null) {
				getImagesAndBoundingBoxesThread3.join();
			}
			getImagesAndBoundingBoxesThread3 = null;
		}
		firstShotDone = false;
	}

	private static void clearBaseImages() {
		synchronized (interruptLock) {
			LeptUtils.dispose(originalPix8);
			LeptUtils.dispose(contrastNormalisedImage);
			LeptUtils.dispose(backgroundNormalisedImage);
			LeptUtils.dispose(unsharpMaskedImage);
			contrastNormalisedImage = null;
			backgroundNormalisedImage = null;
			unsharpMaskedImage = null;
		}
	}

	private static void clearPreBBImages() {
		synchronized (interruptLock) {
			LeptUtils.dispose(try1PreBBImage);
			LeptUtils.dispose(try2PreBBImage);
			LeptUtils.dispose(try3PreBBImage);
			try1PreBBImage = null;
			try2PreBBImage = null;
			try3PreBBImage = null;
		}
	}

	private static void clearBBImages() {
		synchronized (interruptLock) {
			LeptUtils.dispose(try1BBImage);
			LeptUtils.dispose(try2BBImage);
			LeptUtils.dispose(try3BBImage);
			try1BBImage = null;
			try2BBImage = null;
			try3BBImage = null;
		}
	}

	private static void clearTesseractImages() {
		synchronized (interruptLock) {
			LeptUtils.dispose(try1TesseractImage);
			LeptUtils.dispose(try2TesseractImage);
			LeptUtils.dispose(try3TesseractImage);
			LeptUtils.dispose(try4TesseractImage1);
			LeptUtils.dispose(try4TesseractImage2);

			try1TesseractImage = null;
			try2TesseractImage = null;
			try3TesseractImage = null;
			try4TesseractImage1 = null;
			try4TesseractImage2 = null;

		}
		// heightScalingForPoorImages = 0.0f;
		// widthScalingForPoorImages = 0.0f;
		// foundHeightAndWidthScaling = false;
		// numberOfLines = 0;
		allBoxes = new ArrayList<>();
	}

	public static void initialiseProperties(String initFileArgument, String prodFileArgument) {
		productFileArgument = prodFileArgument;
		// System.out.println("Initialising from " + initFileArgument + ", and " +
		// prodFileArgument);
		if (initFileArgument != null) {
			String[] arguments = initFileArgument.split("=");
			if ("initFile".equals(arguments[0])) {
				// System.out.println("Initialising from file - " + arguments[1]);
				initialised = InitialiseParameters.initialise(arguments[1], false);
			} else {
				initialised = InitialiseParameters.initialise();
			}
		} else {
			initialised = InitialiseParameters.initialise();
		}

		if (!initialised) {
			SysOutController.resetSysOut();
			System.out.println("");
			System.out.println("");
			System.out.println(
					"Failed to initialise the application as config.properties was not found. Closing down the application ");
			System.exit(0);
		}

		initialised = initialiseProductParameters(prodFileArgument) && initialised;

		if (!initialised) {
			SysOutController.resetSysOut();
			System.out.println("");
			System.out.println(
					"Failed to initialise the application as product.properties was not found. Closing down the application ");
			System.exit(0);
		}

		// String debugL = System.getProperty(DEBUGLEVEL_KEY);
		String debugL = System.getProperty("dl");
		if ((debugL == null) || "".equals(debugL)) {
			debugLevel = 99;
		} else {
			// debugLevel = Integer.parseInt(System.getProperty(DEBUGLEVEL_KEY));
			debugLevel = Integer.parseInt(System.getProperty("dl"));
		}
		// System.out.println("debugLevel = " + debugLevel);
		// idealCharHeight = Integer.parseInt(System.getProperty(idealCharHeightKey));
		// idealCharWidth = Integer.parseInt(System.getProperty(idealCharWidthKey));
		idealCharHeight = Integer.parseInt(System.getProperty("ich"));
		idealCharWidth = Integer.parseInt(System.getProperty("icw"));
		// System.out.println("ich = " + idealCharHeight);
		// System.out.println("icw = " + idealCharWidth);

		if (!SYSTEM_OUT_ACTIVATE_VALUE.equals(System.getProperty(SYSTEM_OUT_IGNORE_KEY))) {
			SysOutController.ignoreSysout();
		}
		if (AUTO_REPORT_DISABLE.equals(System.getProperty(AUTO_REPORT_AFTER_EVERY_WATCH_CYCLE_KEY))) {
			autoReport = false;
		}

		ProductPriceData.initialise();

		// Properties props = System.getProperties();
		/*
		 * for (Object key : Collections.list(props.propertyNames())) {
		 * System.out.print(key + " : "); System.out.println(props.get(key)); }
		 */
	}

	private static boolean initialiseProductParameters(String prodFileArgument) {
		if (prodFileArgument != null) {
			String[] arguments = prodFileArgument.split("=");
			if ("prodFile".equals(arguments[0])) {
				if (debugLevel <= 2) {
					System.out.println("Initialising product configuration from file - " + arguments[1]);
				}
				return InitialiseParameters.getProductSetting(arguments[1], false);
			} else {
				return InitialiseParameters.getProductSetting("product.properties", false);
			}
		} else {
			return InitialiseParameters.getProductSetting(null, true);
		}

	}

	public static void getBaseImages(Pix pix, int debugL) {

		if (debugL <= 2) {
			System.out.println("Entered createBaseImages()");
		}

		PixDebugWriter.writeIfDebug(debugL, 3, pix,
				DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + currentFileName + "-inputImage.png");

		// Intermediate Pix objects are created and disposed below.
		// The three output static fields (contrastNormalisedImage,
		// backgroundNormalisedImage, originalPix8, unsharpMaskedImage)
		// are assigned inside this block but intentionally NOT wrapped --
		// they are owned by the caller and disposed via clearBaseImages().

		Pix interimImageRaw = (pix != null) ? Leptonica1.pixConvertTo8(pix, 0) : null;
		try (PixAutoCloseable interimImage = new PixAutoCloseable(interimImageRaw)) {

			PixDebugWriter.writeIfDebug(debugL, 3,
					interimImage.get(), DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "interimImage8.png");

			try (PixAutoCloseable anImage1 = new PixAutoCloseable(
					Leptonica1.pixUnsharpMaskingGray(interimImage.get(), 3, 0.7f))) {

				PixDebugWriter.writeIfDebug(debugL, 3,
						anImage1.get(), DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "originalSharpened1.png");

				try (PixAutoCloseable anImage2 = new PixAutoCloseable(
						Leptonica1.pixUnsharpMaskingGray(anImage1.get(), 3, 0.7f))) {

					PixDebugWriter.writeIfDebug(debugL, 3,
							anImage2.get(), DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "originalSharpened2.png");

					try (PixAutoCloseable anImage3 = new PixAutoCloseable(
							Leptonica1.pixUnsharpMaskingGray(anImage2.get(), 3, 0.7f))) {

						PixDebugWriter.writeIfDebug(debugL, 3,
								anImage3.get(), DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "originalSharpened3.png");

						Pix anImage0Raw = CheckProductProperties.productIsGiven
								? Leptonica1.pixBlockconvGray(anImage3.get(), null, 2, 1)
								: Leptonica1.pixCopy(null, anImage3.get());

						try (PixAutoCloseable anImage0 = new PixAutoCloseable(anImage0Raw)) {

							Pix interimBN = Leptonica1.pixBackgroundNormFlex(anImage0.get(), 7, 7, 1, 1, 160);

							if (CheckProductProperties.productIsGiven) {
								contrastNormalisedImage = Leptonica1.pixContrastNorm(
										null, interimBN, 18, anImage3.get().h / 5, 100, 2, 2);
							} else {
								contrastNormalisedImage = Leptonica1.pixContrastNorm(
										null, interimBN, 24, 24, 100, 2, 2);
							}
							PixDebugWriter.writeIfDebug(debugL, 3,
									contrastNormalisedImage,
									DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "contrastNormalisedPix.png");

							Pix interimPix2Raw = Leptonica1.pixAddBorder(contrastNormalisedImage, 2, 255);

							backgroundNormalisedImage = Leptonica1.pixCopy(null, interimBN);
							PixDebugWriter.writeIfDebug(debugL, 3,
									backgroundNormalisedImage,
									DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "backgroundNormalisedPix.png");

							LeptUtils.dispose(interimBN);
							LeptUtils.dispose(interimPix2Raw);

							originalPix8 = Leptonica1.pixUnsharpMaskingGray(backgroundNormalisedImage, 5, 0.7f);
							PixDebugWriter.writeIfDebug(debugL, 3,
									originalPix8,
									DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "originalPix8.png");

							Pix anotherInterimRaw = CheckProductProperties.productIsGiven
									? Leptonica1.pixBlockconvGray(originalPix8, null, 1, 2)
									: Leptonica1.pixCopy(null, originalPix8);

							unsharpMaskedImage = Leptonica1.pixBackgroundNormFlex(
									anotherInterimRaw, 5, 5, 1, 1, 60);
							PixDebugWriter.writeIfDebug(debugL, 3,
									unsharpMaskedImage,
									DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "unsharpMasked.png");

							LeptUtils.dispose(anotherInterimRaw);
						}
					}
				}
			}
		}

		if (debugL <= 2) {
			System.out.println("Exiting createBaseImages()");
		}

	}

	public static void getBaseImagesAlternate(Pix pix, int debugL) {

		float reductionFactorWhenProductIsGiven = 1.0f;
		if (debugL <= 2) {
			System.out.println("Entered createBaseImages()");
		}

		if (debugL <= 3) {
   PixDebugWriter.writeIfDebug(debugL, 3, pix, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + currentFileName + "-inputImage.png");
		}

		Pix interimImage = null;
		Pix interimPix1 = null;
		Pix interimPix2 = null;

		if (pix != null) {
			interimImage = Leptonica1.pixConvertTo8(pix, 0);
		}

		Pix firstCutBlurImage = Leptonica1.pixCloseGray(interimImage, 1, 3);

		Pix firstCutUnsharpImage = Leptonica1.pixUnsharpMaskingGray(firstCutBlurImage, 7, 0.3f);

		contrastNormalisedImage = Leptonica1.pixContrastNorm(null, firstCutUnsharpImage, 24, 24, 75, 2, 2);
		PixDebugWriter.writeIfDebug(debugL, 3, contrastNormalisedImage, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "contrastNormalisedPix.png");

		if (CheckProductProperties.productIsGiven) {
			interimPix1 = Leptonica1.pixScale(contrastNormalisedImage, 1 / reductionFactorWhenProductIsGiven,
					1 / reductionFactorWhenProductIsGiven);
		} else {
			interimPix1 = Leptonica1.pixCopy(null, contrastNormalisedImage);
		}

		interimPix2 = Leptonica1.pixAddBorder(interimPix1, 2, 255);

		backgroundNormalisedImage = Leptonica1.pixBackgroundNormFlex(interimPix2, 7, 7, 3, 3, 0);
		PixDebugWriter.writeIfDebug(debugL, 3, backgroundNormalisedImage, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "backgroundNormalisedPix.png");

		Pix anImage = Leptonica1.pixUnsharpMaskingGray(backgroundNormalisedImage, 3, 0.7f);
		if (debugL <= 3) {
   PixDebugWriter.writeIfDebug(debugL, 3, anImage, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "unsharpMaskedPix.png");
		}

		unsharpMaskedImage = Leptonica1.pixBlockconvGray(anImage, null, 1, 1);
		if (debugL <= 3) {
   PixDebugWriter.writeIfDebug(debugL, 3, unsharpMaskedImage, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "unsharpBlurred.png");
		}

		LeptUtils.dispose(interimImage);
		LeptUtils.dispose(firstCutUnsharpImage);
		LeptUtils.dispose(firstCutBlurImage);
		LeptUtils.dispose(interimPix1);
		LeptUtils.dispose(interimPix2);
		LeptUtils.dispose(anImage);
		if (debugL <= 2) {
			System.out.println("Exiting createBaseImages()");
		}

	}

	private static void getDerivativeImagesAndBoundingBoxes(int debugL) {

		if (debugL <= 2) {
			System.out.println("Entered getDerivativeImagesAndBoundingBoxes()");
		}

		final int xDivisionLength = CheckProductProperties.productIsGiven ? 100 : 120;
		final int yDivisionLength = CheckProductProperties.productIsGiven ? 100 : 120;

		// parameters
		// PIG = ProductIsGiven
		// NPIG = !(PigIsGiven)
		// T1 = Try1
		// T2 = Try2
		// T3 = Try3

		final int PIG_T1_nXPixels_OG = 1;
		final int PIG_T1_nYPixels_OG = 2;
		final int PIG_T1_nXPixels_EG = 1;
		final int PIG_T1_nYPixels_EG = 1;
		final int PIG_T1_nXPixels_BBErode = 1;
		final int PIG_T1_nYPixels_BBErode = 2;
		final float PIG_T1_sauvolaFactor = 0.005f; // 0.175f

		final int PIG_T2_nXPixels_OG = 2;
		final int PIG_T2_nYPixels_OG = 3;
		final int PIG_T2_nXPixels_EG = 1;
		final int PIG_T2_nYPixels_EG = 2; // 1
		final int PIG_T2_nXPixels_BBErode = 1;
		final int PIG_T2_nYPixels_BBErode = 2;
		final float PIG_T2_sauvolaFactor = 0.10f; // 0.25f

		final int PIG_T3_nXPixels_OG = 2;
		final int PIG_T3_nYPixels_OG = 2;
		final int PIG_T3_nXPixels_EG = 2;
		final int PIG_T3_nYPixels_EG = 2;
		final int PIG_T3_nXPixels_BBErode = 2;
		final int PIG_T3_nYPixels_BBErode = 1;
		// final float PIG_T3_sauvolaFactor = 0.25f;

		final int NPIG_T1_nXPixels_OG = 1;
		final int NPIG_T1_nYPixels_OG = 2;
		final int NPIG_T1_nXPixels_EG = 1;
		final int NPIG_T1_nYPixels_EG = 1;
		final int NPIG_T1_nXPixels_BBErode = 1;
		final int NPIG_T1_nYPixels_BBErode = 2;
		final float NPIG_T1_sauvolaFactor = 0.30f; // 0.125f

		final int NPIG_T3_nXPixels_OG = 1;
		final int NPIG_T3_nYPixels_OG = 2;
		final int NPIG_T3_nXPixels_EG = 2;
		final int NPIG_T3_nYPixels_EG = 1;
		final int NPIG_T3_nXPixels_BBErode = 1;
		final int NPIG_T3_nYPixels_BBErode = 2;
		final float NPIG_T3_sauvolaFactor = 0.30f; // 0.125f

		// get the base image for Tesseract, BB, and angle determination
		// for BB and angle determination, this image needs to have lines removed

		if (!CheckProductProperties.productIsGiven) {
			// Product not given
			try1Thread = CompletableFuture.supplyAsync(() -> {
				if (debugL <= 2) {
					System.out
							.println("Entered getDerivativeImagesAndBoundingBoxes() Product-Is-Not-Given - try1Thread");
				}

				Pix unsharpMaskedCopy = Leptonica1.pixCopy(null, unsharpMaskedImage);
				Pix pixOpenLow = null;
				Pix pixOpenErodedLow = null;
				// Pix pixGaussianBlurredLow = null;
				PointerByReference pbrSauvolaLow = null;
				Pix pixSauvolaCleaned = null;
				Pix pixSaltPepperRemoved = null;

				pixOpenLow = Leptonica1.pixOpenGray(unsharpMaskedCopy, NPIG_T1_nXPixels_OG, NPIG_T1_nYPixels_OG);
				pixOpenErodedLow = Leptonica1.pixErodeGray(pixOpenLow, NPIG_T1_nXPixels_EG, NPIG_T1_nYPixels_EG);
				synchronized (try4TesseractImage1Lock) {
					try4TesseractImage1 = Leptonica1.pixBilateralGray(pixOpenErodedLow, 25f, 50f, 16, 4);
				}
				PixDebugWriter.writeIfDebug(debugL, 3, try4TesseractImage1, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "sauvolaPrepBlurredLow-" + 1 + ".png");

				pbrSauvolaLow = new PointerByReference();
				int success = 1;
				synchronized (try4TesseractImage1Lock) {
					success = Leptonica1.pixSauvolaBinarizeTiled(try4TesseractImage1, 11, NPIG_T1_sauvolaFactor,
							(try4TesseractImage1.w / xDivisionLength) + 1,
							(try4TesseractImage1.h / yDivisionLength) + 1, null, pbrSauvolaLow);
				}
				if (debugL <= 2) {
					System.out.println(
							"In getDerivativeImagesAndBoundingBoxes() Product-Is-Not-Given : Sauvola success in try1Thread in Product-is-not-given = "
									+ success);
				}

				// DOES NOT need to be disposed. Instead, pbrSauvola needs to be disposed
				pixSauvolaCleaned = new Pix(pbrSauvolaLow.getValue());
				PixDebugWriter.writeIfDebug(debugL, 3, pixSauvolaCleaned, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "cleanedSauvolaPix-" + 1 + ".png");
				pixSaltPepperRemoved = PixCleaningUtils.removeSaltPepper(pixSauvolaCleaned, 12, DEFAULT_DEBUGGINGIMAGES_DIRECTORY);
				try1PreBBImage = PixCleaningUtils.removeLines(pixSaltPepperRemoved, 12, DEFAULT_DEBUGGINGIMAGES_DIRECTORY, tryNumber);
				// try2PreBBImage = Leptonica1.pixCopy(null, try1PreBBImage);

				LeptUtils.dispose(unsharpMaskedCopy);
				LeptUtils.dispose(pixOpenLow);
				LeptUtils.dispose(pixOpenErodedLow);
				// LeptUtils.dispose(pixGaussianBlurredLow);
				Leptonica1.pixDestroy(pbrSauvolaLow);
				LeptUtils.dispose(pixSaltPepperRemoved);
				if (debugL <= 2) {
					System.out.println(
							"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Not-Given with Boolean.TRUE- try1Thread");
				}
				return Boolean.TRUE;
			});
			try2Thread = CompletableFuture.supplyAsync(() -> {
				return Boolean.TRUE;
			});
			try3Thread = CompletableFuture.supplyAsync(() -> {
				if (debugL <= 2) {
					System.out
							.println("Entered getDerivativeImagesAndBoundingBoxes() Product-Is-Not-Given - try3Thread");
				}
				Pix unsharpMaskedCopy = null;
				while (!firstShotDone) {
					try {
						TimeUnit.MILLISECONDS.sleep(10L);
					} catch (InterruptedException e) {
						throw new IllegalStateException(e);
					}
				}
				synchronized (interruptLock) {
					if (unsharpMaskedImage != null) {
						unsharpMaskedCopy = Leptonica1.pixCopy(null, unsharpMaskedImage);
					}
				}
				if (unsharpMaskedCopy == null) {
					LeptUtils.dispose(unsharpMaskedCopy);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Not-Given - try3Thread at 1");
					}
					return Boolean.FALSE;
				}
				Pix pixOpenLow = null;
				Pix pixOpenErodedLow = null;
				// Pix pixGaussianBlurredLow = null;
				PointerByReference pbrSauvolaLow = null;
				Pix pixSauvolaCleaned = null;
				Pix pixSaltPepperRemoved = null;

				if (getInterruptParallelThreads()) {
					LeptUtils.dispose(unsharpMaskedCopy);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Not-Given - try3Thread at 2");
					}
					return Boolean.FALSE;
				} else {
					pixOpenLow = Leptonica1.pixOpenGray(unsharpMaskedCopy, NPIG_T3_nXPixels_OG, NPIG_T3_nYPixels_OG);
				}

				if (getInterruptParallelThreads()) {
					LeptUtils.dispose(unsharpMaskedCopy);
					LeptUtils.dispose(pixOpenLow);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Not-Given - try3Thread ta 3");
					}
					return Boolean.FALSE;
				} else {
					pixOpenErodedLow = Leptonica1.pixErodeGray(pixOpenLow, NPIG_T3_nXPixels_EG, NPIG_T3_nYPixels_EG);
				}

				if (getInterruptParallelThreads()) {
					LeptUtils.dispose(unsharpMaskedCopy);
					LeptUtils.dispose(pixOpenLow);
					LeptUtils.dispose(pixOpenErodedLow);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Not-Given - try3Thread at 4");
					}
					return Boolean.FALSE;
				} else {
					synchronized (try4TesseractImage2Lock) {
						try4TesseractImage2 = Leptonica1.pixBilateralGray(pixOpenErodedLow, 25f, 50f, 16, 4);
					}
				}

				PixDebugWriter.writeIfDebug(debugL, 3, try4TesseractImage2, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "sauvolaPrepBlurredLow-" + 3 + ".png");

				if (getInterruptParallelThreads()) {
					LeptUtils.dispose(unsharpMaskedCopy);
					LeptUtils.dispose(pixOpenLow);
					LeptUtils.dispose(pixOpenErodedLow);
					// LeptUtils.dispose(pixGaussianBlurredLow);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Not-Given - try3Thread at 5");
					}
					return Boolean.FALSE;
				} else {
					pbrSauvolaLow = new PointerByReference();
					int success = 1;
					synchronized (try4TesseractImage2Lock) {
						success = Leptonica1.pixSauvolaBinarizeTiled(try4TesseractImage2, 11, NPIG_T3_sauvolaFactor,
								(try4TesseractImage2.w / xDivisionLength) + 1,
								(try4TesseractImage2.h / yDivisionLength) + 1, null, pbrSauvolaLow);
					}
					if (debugL <= 2) {
						System.out.println(
								"In getDerivativeImagesAndBoundingBoxes() Product-Is-Not-Given : Sauvola success in try3Thread in Product-is-not-given = "
										+ success);
					}
				}
				// DOES NOT need to be disposed. Instead, pbrSauvola needs to be disposed

				if (getInterruptParallelThreads()) {
					LeptUtils.dispose(unsharpMaskedCopy);
					LeptUtils.dispose(pixOpenLow);
					LeptUtils.dispose(pixOpenErodedLow);
					// LeptUtils.dispose(pixGaussianBlurredLow);
					Leptonica1.pixDestroy(pbrSauvolaLow);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Not-Given - try3Thread at 6");
					}
					return Boolean.FALSE;
				} else {
					pixSauvolaCleaned = new Pix(pbrSauvolaLow.getValue());
					PixDebugWriter.writeIfDebug(debugL, 3, pixSauvolaCleaned, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "cleanedSauvolaPix-" + 3 + ".png");
					pixSaltPepperRemoved = PixCleaningUtils.removeSaltPepper(pixSauvolaCleaned, 12, DEFAULT_DEBUGGINGIMAGES_DIRECTORY);
				}

				if (getInterruptParallelThreads()) {
					LeptUtils.dispose(unsharpMaskedCopy);
					LeptUtils.dispose(pixOpenLow);
					LeptUtils.dispose(pixOpenErodedLow);
					// LeptUtils.dispose(pixGaussianBlurredLow);
					Leptonica1.pixDestroy(pbrSauvolaLow);
					LeptUtils.dispose(pixSaltPepperRemoved);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Not-Given - try3Thread at 7");
					}
					return Boolean.FALSE;
				} else {
					synchronized (interruptLock) {
						try3PreBBImage = PixCleaningUtils.removeLines(pixSaltPepperRemoved, 12, DEFAULT_DEBUGGINGIMAGES_DIRECTORY, tryNumber);
					}
				}
				LeptUtils.dispose(unsharpMaskedCopy);
				LeptUtils.dispose(pixOpenLow);
				LeptUtils.dispose(pixOpenErodedLow);
				// LeptUtils.dispose(pixGaussianBlurredLow);
				Leptonica1.pixDestroy(pbrSauvolaLow);
				LeptUtils.dispose(pixSaltPepperRemoved);
				if (debugL <= 2) {
					System.out.println(
							"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Not-Given with Boolean.TRUE - try3Thread");
				}

				return Boolean.TRUE;
			}, parallelThreadPool);
		} else {
			// Product given and firstCall
			try1Thread = CompletableFuture.supplyAsync(() -> {
				if (debugL <= 2) {
					System.out.println("Entered getDerivativeImagesAndBoundingBoxes() Product-Is-Given - try1Thread");
				}
				Pix unsharpMaskedCopy = Leptonica1.pixCopy(null, unsharpMaskedImage);
				Pix pixOpenLow = null;
				// Pix pixOpenErodedLow = null;
				// Pix pixGaussianBlurredLow = null;
				PointerByReference pbrSauvolaLow = null;
				Pix pixSauvolaCleaned = null;
				Pix pixSaltPepperRemoved = null;

				pixOpenLow = Leptonica1.pixOpenGray(unsharpMaskedCopy, PIG_T1_nXPixels_OG, PIG_T1_nYPixels_OG);
				synchronized (try4TesseractImage1Lock) {
					try4TesseractImage1 = Leptonica1.pixErodeGray(pixOpenLow, PIG_T1_nXPixels_EG, PIG_T1_nYPixels_EG);
				}
				// pixGaussianBlurredLow = Leptonica1.pixBilateralGray(pixOpenErodedLow, 5f,
				// 25f, 4, 2);
				// pixGaussianBlurredLow = Leptonica1.pixBilateralGray(pixOpenErodedLow, 5f,
				// 25f, 4, 2);
				// pixGaussianBlurredLow = Leptonica1.pixBilateralGray(pixOpenErodedLow, 10f,
				// 50f, 16, 4);
				// if (debugL <= 3) {
				// Leptonica1.pixWrite(DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" +
				// "sauvolaPrepBlurredLow-" + 1 + ".png",
				// pixGaussianBlurredLow, ILeptonica.IFF_PNG);
				// }

				PixDebugWriter.writeIfDebug(debugL, 3, try4TesseractImage1, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "sauvolaPrepPix-" + 1 + ".png");


				// Pix twoMaskedPix =
				// Leptonica1.pixMaskedThreshOnBackgroundNorm(pixOpenErodedLow, null, 8, 8, 200,
				// 50,
				// 0, 0, 0.1f, null);

				// if (debugL <= 3) {
				// Leptonica1.pixWrite(DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" +
				// "twoMaskedBinarizedPix-" + 1 + ".png",
				// twoMaskedPix, ILeptonica.IFF_PNG);
				// }

				// pbrSauvolaLow = new PointerByReference();
				// int success = 1;
				// success = Leptonica1.pixSauvolaBinarizeTiled(pixGaussianBlurredLow, 11,
				// PIG_T1_sauvolaFactor,
				// // 0.275f, 0.25f, 0.225f, 0.175f, 0.125f
				// (pixGaussianBlurredLow.w / xDivisionLength) + 1,
				// (pixGaussianBlurredLow.h / yDivisionLength) + 1, null, pbrSauvolaLow);
				// if (debugL <= 2) {
				// System.out.println(
				// "In getDerivativeImagesAndBoundingBoxes() Product-Is-Given : Sauvola success
				// in try1Thread = "
				// + success);
				// }

				// DOES NOT need to be disposed. Instead, pbrSauvolaLow needs to be disposed
				// pixSauvolaCleaned = new Pix(pbrSauvolaLow.getValue());

				synchronized (try4TesseractImage1Lock) {
					pixSauvolaCleaned = Leptonica1.pixMaskedThreshOnBackgroundNorm(try4TesseractImage1, null, 8, 8,
							(int) (averagePixelValue(try4TesseractImage1) * 0.9), 50, 2, 2, 0.1f, null);
				}

				PixDebugWriter.writeIfDebug(debugL, 3, pixSauvolaCleaned, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "cleanedSauvolaPix-" + 1 + ".png");
				pixSaltPepperRemoved = PixCleaningUtils.removeSaltPepper(pixSauvolaCleaned, 12, DEFAULT_DEBUGGINGIMAGES_DIRECTORY);
				synchronized (interruptLock) {
					try1PreBBImage = PixCleaningUtils.removeLines(pixSaltPepperRemoved, 12, DEFAULT_DEBUGGINGIMAGES_DIRECTORY, tryNumber);
				}

				LeptUtils.dispose(unsharpMaskedCopy);
				LeptUtils.dispose(pixOpenLow);
				// LeptUtils.dispose(pixOpenErodedLow);
				// LeptUtils.dispose(pixGaussianBlurredLow);
				// Leptonica1.pixDestroy(pbrSauvolaLow);
				LeptUtils.dispose(pixSaltPepperRemoved);
				// LeptUtils.dispose(twoMaskedPix);
				LeptUtils.dispose(pixSauvolaCleaned);
				if (debugL <= 2) {
					System.out.println(
							"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Given with Boolean.TRUE - try1Thread");
				}
				return Boolean.TRUE;
			});

			try2Thread = CompletableFuture.supplyAsync(() -> {
				if (debugL <= 2) {
					System.out.println("Entered getDerivativeImagesAndBoundingBoxes() Product-Is-Given - try2Thread");
				}
				Pix backgroundNormalisedCopy = null;
				while (!firstShotDone) {
					try {
						TimeUnit.MILLISECONDS.sleep(10L);
					} catch (InterruptedException e) {
						throw new IllegalStateException(e);
					}
				}
				synchronized (interruptLock) {
					if (backgroundNormalisedImage != null) {
						backgroundNormalisedCopy = Leptonica1.pixCopy(null, backgroundNormalisedImage);
					}
				}
				if (backgroundNormalisedCopy == null) {
					LeptUtils.dispose(backgroundNormalisedCopy);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Given - try2Thread at 1");
					}
					return Boolean.FALSE;
				}

				Pix pixOpenLow = null;
				Pix pixOpenErodedLow = null;
				Pix pixGaussianBlurredLow = null;
				PointerByReference pbrSauvolaLow = null;
				Pix pixSauvolaCleaned = null;

				if (getInterruptParallelThreads()) {
					LeptUtils.dispose(backgroundNormalisedCopy);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Given - try2Thread at 2");
					}
					return Boolean.FALSE;
				} else {
					pixOpenLow = Leptonica1.pixOpenGray(backgroundNormalisedCopy, PIG_T2_nXPixels_OG,
							PIG_T2_nYPixels_OG);
				}

				if (getInterruptParallelThreads()) {
					LeptUtils.dispose(backgroundNormalisedCopy);
					LeptUtils.dispose(pixOpenLow);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Given - try2Thread at 3");
					}
					return Boolean.FALSE;
				} else {
					pixOpenErodedLow = Leptonica1.pixErodeGray(pixOpenLow, PIG_T2_nXPixels_EG, PIG_T2_nYPixels_EG);
				}

				if (getInterruptParallelThreads()) {
					LeptUtils.dispose(backgroundNormalisedCopy);
					LeptUtils.dispose(pixOpenLow);
					LeptUtils.dispose(pixOpenErodedLow);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Given - try2Thread at 4");
					}
					return Boolean.FALSE;
				} else {
					Pix pixGaussianBlurredLow_Temp1 = Leptonica1.pixOpenGray(pixOpenErodedLow, 2, 1);
					pixGaussianBlurredLow = Leptonica1.pixUnsharpMaskingGray(pixGaussianBlurredLow_Temp1, 5, 0.7f);
					LeptUtils.dispose(pixGaussianBlurredLow_Temp1);
				}

				PixDebugWriter.writeIfDebug(debugL, 3, pixGaussianBlurredLow, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "sauvolaPrepBlurredLow-" + 2 + ".png");

				if (getInterruptParallelThreads()) {
					LeptUtils.dispose(backgroundNormalisedCopy);
					LeptUtils.dispose(pixOpenLow);
					LeptUtils.dispose(pixOpenErodedLow);
					LeptUtils.dispose(pixGaussianBlurredLow);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Given - try2Thread at 5");
					}
					return Boolean.FALSE;
				} else {
					// DOES NOT need to be disposed. Instead, pbrSauvola needs to be disposed
					pbrSauvolaLow = new PointerByReference();
					int success = Leptonica1.pixSauvolaBinarizeTiled(pixGaussianBlurredLow, 11, PIG_T2_sauvolaFactor,
							// 0.275f, 0.25f, 0.225f, 0.175f, 0.125f
							(pixGaussianBlurredLow.w / (int) (xDivisionLength * 2.0 / 3)) + 1,
							(pixGaussianBlurredLow.h / (int) (yDivisionLength * 2.0 / 3)) + 1, null, pbrSauvolaLow);
					if (debugL <= 2) {
						System.out.println(
								"In getDerivativeImagesAndBoundingBoxes() Product-Is-Given : Sauvola success 2 in try2Thread= "
										+ success);
					}
				}

				if (getInterruptParallelThreads()) {
					LeptUtils.dispose(backgroundNormalisedCopy);
					LeptUtils.dispose(pixOpenLow);
					LeptUtils.dispose(pixOpenErodedLow);
					LeptUtils.dispose(pixGaussianBlurredLow);
					Leptonica1.pixDestroy(pbrSauvolaLow);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Given - try2Thread at 6");
					}
					return Boolean.FALSE;
				} else {
					pixSauvolaCleaned = new Pix(pbrSauvolaLow.getValue());
					PixDebugWriter.writeIfDebug(debugL, 3, pixSauvolaCleaned, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "cleanedSauvolaPix-" + 2 + ".png");
					synchronized (interruptLock) {
						try2PreBBImage = PixCleaningUtils.removeSaltPepper(pixSauvolaCleaned, 12, DEFAULT_DEBUGGINGIMAGES_DIRECTORY);
					}
				}
				LeptUtils.dispose(backgroundNormalisedCopy);
				LeptUtils.dispose(pixOpenLow);
				LeptUtils.dispose(pixOpenErodedLow);
				LeptUtils.dispose(pixGaussianBlurredLow);
				Leptonica1.pixDestroy(pbrSauvolaLow);
				if (debugL <= 2) {
					System.out.println(
							"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Given with Boolean.TRUE - try2Thread");
				}
				// no need to remove lines for image 2 of productIsGiven
				return Boolean.TRUE;
			}, parallelThreadPool);

			try3Thread = CompletableFuture.supplyAsync(() -> {
				if (debugL <= 2) {
					System.out.println("Entering getDerivativeImagesAndBoundingBoxes() Product-Is-Given - try3Thread");
				}

				Pix unsharpMaskedCopy = null;
				while (!firstShotDone) {
					try {
						TimeUnit.MILLISECONDS.sleep(10L);
					} catch (InterruptedException e) {
						throw new IllegalStateException(e);
					}
				}
				synchronized (interruptLock) {
					if (unsharpMaskedImage != null) {
						unsharpMaskedCopy = Leptonica1.pixCopy(null, unsharpMaskedImage);
					}
				}
				if (unsharpMaskedCopy == null) {
					LeptUtils.dispose(unsharpMaskedCopy);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Given - try3Thread at 0");
					}
					return Boolean.FALSE;
				}

				Pix pixOpenLow = null;
				Pix pixOpenErodedLow = null;
				// Pix pixGaussianBlurredLow = null;
				Pix pixSauvolaCleaned = null;

				if (getInterruptParallelThreads()) {
					LeptUtils.dispose(unsharpMaskedCopy);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Given - try3Thread at 1");
					}
					return Boolean.FALSE;
				} else {
					pixOpenLow = Leptonica1.pixOpenGray(unsharpMaskedCopy, PIG_T3_nXPixels_OG, PIG_T3_nYPixels_OG);
				}

				if (getInterruptParallelThreads()) {
					LeptUtils.dispose(unsharpMaskedCopy);
					LeptUtils.dispose(pixOpenLow);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Given - try3Thread at 2");
					}
					return Boolean.FALSE;
				} else {
					pixOpenErodedLow = Leptonica1.pixErodeGray(pixOpenLow, PIG_T3_nXPixels_EG, PIG_T3_nYPixels_EG);
				}

				if (getInterruptParallelThreads()) {
					LeptUtils.dispose(unsharpMaskedCopy);
					LeptUtils.dispose(pixOpenLow);
					LeptUtils.dispose(pixOpenErodedLow);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Given - try3Thread at 3");
					}
					return Boolean.FALSE;
				} else {
					synchronized (try4TesseractImage2Lock) {
						try4TesseractImage2 = Leptonica1.pixUnsharpMaskingGray(pixOpenErodedLow, 3, 0.7f);
					}
				}

				PixDebugWriter.writeIfDebug(debugL, 3, try4TesseractImage2, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "pixGaussianBlurredLow-" + 3 + ".png");

				if (getInterruptParallelThreads()) {
					LeptUtils.dispose(unsharpMaskedCopy);
					LeptUtils.dispose(pixOpenLow);
					LeptUtils.dispose(pixOpenErodedLow);
					// LeptUtils.dispose(pixGaussianBlurredLow);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Given - try3Thread at 4");
					}
					return Boolean.FALSE;
				} else {
					// pixSauvolaCleaned = Leptonica1.pixConvertTo1(pixGaussianBlurredLow, 144);
					// pixSauvolaCleaned = Leptonica1.pixConvertTo1(pixGaussianBlurredLow, 160);

					synchronized (try4TesseractImage2Lock) {
						pixSauvolaCleaned = Leptonica1.pixConvertTo1(try4TesseractImage2,
								(int) (averagePixelValue(try4TesseractImage2) * 0.775));
					}
					// pixSauvolaCleaned = Leptonica1.pixConvertTo1(pixGaussianBlurredLow,
					// (int) (averagePixelValue(unsharpMaskedImage) * 0.9));
					PixDebugWriter.writeIfDebug(debugL, 3, pixSauvolaCleaned, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "cleanedSauvolaPix-" + 3 + ".png");
					synchronized (interruptLock) {
						try3PreBBImage = PixCleaningUtils.removeSaltPepper(pixSauvolaCleaned, 12, DEFAULT_DEBUGGINGIMAGES_DIRECTORY);
					}
				}
				LeptUtils.dispose(unsharpMaskedCopy);
				LeptUtils.dispose(pixOpenLow);
				LeptUtils.dispose(pixOpenErodedLow);
				// LeptUtils.dispose(pixGaussianBlurredLow);
				LeptUtils.dispose(pixSauvolaCleaned);

				if (debugL <= 2) {
					System.out.println(
							"Exiting getDerivativeImagesAndBoundingBoxes() Product-Is-Given with Boolean.TRUE - try3Thread");
				}
				return Boolean.TRUE;
			}, parallelThreadPool);
		}

		try1ThreadFinished = try1Thread.join(); // wait for thread 1 to complete
		PixDebugWriter.writeIfDebug(debugL, 3, try1PreBBImage, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "pixImageForRotationAngle-" + 1 + ".png");

		// get the rotation angle

		try1RotationAngle = 0f;
		float angle = 0.0f;
		double borderSnip = 0.06;
		Box clipBox = new Box();
		clipBox.x = (int) (try1PreBBImage.w * borderSnip);
		clipBox.y = (int) (try1PreBBImage.h * borderSnip);
		clipBox.w = (int) (try1PreBBImage.w * (1 - (2 * borderSnip)));
		clipBox.h = (int) (try1PreBBImage.h * (1 - (2 * borderSnip)));
		Pix pixForSkewAngle = Leptonica1.pixClipRectangle(try1PreBBImage, clipBox, null);
		PixDebugWriter.writeIfDebug(debugL, 2, pixForSkewAngle, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "borderClippedImageForRotationAngle.png");
		FloatBuffer sauvolaAlternativeAngle2 = FloatBuffer.allocate(1);
		FloatBuffer conf2 = FloatBuffer.allocate(1);
		Leptonica1.pixFindSkewOrthogonalRange(pixForSkewAngle, sauvolaAlternativeAngle2, conf2, 4, 4, 47.0f, 0.30f,
				0.30f, 5.0f);
		float sauvolaAltAngle2 = sauvolaAlternativeAngle2.get(0);
		LeptUtils.dispose(pixForSkewAngle);

		angle = sauvolaAltAngle2;
		try1RotationAngle = angle;

		if (debugL <= 11) {
			System.out.println("	getDerivativeImagesAndBoundingBoxes(): In " + currentFileName
					+ ": Final angle of deskew in degrees = " + try1RotationAngle);
		}

		// rotate the image to get the BufferedImage for Tesseract

//		Thread iteration1Thread = new Thread() {
//			
//			public void run() {
//				if (debugL <= 2) {
//					System.out.println("Entering getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread1");
//				}
//				BufferedImage cleanedBIForBB = null;
//				try {
//					cleanedBIForBB = LeptUtils.convertPixToImage(try1PreBBImage);
//					if (debugL <= 2) {
//						System.out.println("getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread1 - created cleaned BI for Tesseract");
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//					iteration1Done = true;
//					return;
//				}
//				final double rads1 = Math.toRadians(try1RotationAngle);
//				final double sin1 = Math.abs(Math.sin(rads1));
//				final double cos1 = Math.abs(Math.cos(rads1));
//				final int w1 = (int) Math.floor((cleanedBIForBB.getWidth() * cos1) + (cleanedBIForBB.getHeight() * sin1));
//				final int h1 = (int) Math.floor((cleanedBIForBB.getHeight() * cos1) + (cleanedBIForBB.getWidth() * sin1));
//				BufferedImage rotSauvolaImage = new BufferedImage(w1, h1, BufferedImage.TYPE_BYTE_GRAY);
//				Graphics2D g1 = rotSauvolaImage.createGraphics();
//				g1.setColor(Color.WHITE);
//				g1.fillRect(0, 0, w1, h1);
//				RenderingHints rh3 = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
//						RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
//				g1.setRenderingHints(rh3);
//				RenderingHints rh4 = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
//						RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//				g1.setRenderingHints(rh4);
//				RenderingHints rh5 = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//				g1.setRenderingHints(rh5);
//				final AffineTransform at1 = new AffineTransform();
//				at1.rotate(rads1, w1 / 2, h1 / 2);
//				g1.drawImage(cleanedBIForBB, at1, null);
//				g1.dispose();
//				Pix pixCleaned8bpp = null;
//				try {
//					pixCleaned8bpp = SBImage.getPixFromBufferedImage(rotSauvolaImage);
//					if (debugL <= 3) {
//						Leptonica1.pixWrite(DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "rotatedPreBB8bppImage-" + 1 + ".png",
//								pixCleaned8bpp, ILeptonica.IFF_PNG);
//					}
//					synchronized (interruptLock) {
//						try1TesseractImage = Leptonica1.pixConvertTo1(pixCleaned8bpp, 136);
//					}
//					if (debugL <= 2) {
//						System.out.println("getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread1 - created rotated Pix try1TesseractImage for Tesseract");
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//					iteration1Done = true;
//					return;
//				}
//				if (debugL <= 3) {
//					Leptonica1.pixWrite(DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "imageForTesseract_1bpp-" + 1 + ".png",
//							try1TesseractImage, ILeptonica.IFF_PNG);
//				}
//
//				// erode and get the pix for bounding boxes
//				Pix pixCleanedBB_8bpp = null;
//				if (CheckProductProperties.productIsGiven) {
//					pixCleanedBB_8bpp = Leptonica1.pixErodeGray(pixCleaned8bpp, PIG_T1_nXPixels_BBErode,
//							PIG_T1_nYPixels_BBErode);
//				} else {
//					pixCleanedBB_8bpp = Leptonica1.pixErodeGray(pixCleaned8bpp, NPIG_T1_nXPixels_BBErode,
//							NPIG_T1_nYPixels_BBErode);
//				}
//				synchronized (interruptLock) {
//					try1BBImage = Leptonica1.pixConvertTo1(pixCleanedBB_8bpp, 136);
//				}
//				if (debugL <= 2) {
//					System.out.println("getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread1 - created rotated Pix try1BBImage for bounding boxes");
//				}
//				LeptUtils.dispose(pixCleaned8bpp);
//				LeptUtils.dispose(pixCleanedBB_8bpp);
//				if (debugL <= 3) {
//					Leptonica1.pixWrite(DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "imageForBB_1bpp-" + 1 + ".png",
//							try1BBImage, ILeptonica.IFF_PNG);
//				}
//
//				if (try1BBImage != null) {
//					try1BoundingBoxes = getBoundingBoxes(try1BBImage, 1, debugL);
//				}
//				if (debugL <= 2) {
//					System.out.println("Exiting getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread1 with Boolean.TRUE - got bounding boxes");
//				}
//				getImagesAndBBThread1Finished = true;
//				iteration1Done = true;
//			}
//		};
//		
//		iteration1Thread.setPriority(10);
//		iteration1Thread.start();

		getImagesAndBoundingBoxesThread1 = CompletableFuture.supplyAsync(() -> {
			if (debugL <= 2) {
				System.out.println("Entering getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread1");
			}
			BufferedImage cleanedBIForBB = null;
			try {
				cleanedBIForBB = LeptUtils.convertPixToImage(try1PreBBImage);
				if (debugL <= 2) {
					System.out.println(
							"getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread1 - created cleaned BI for Tesseract");
				}
			} catch (Exception e) {
				e.printStackTrace();
				return Boolean.FALSE;
			}
			final double rads1 = Math.toRadians(try1RotationAngle);
			final double sin1 = Math.abs(Math.sin(rads1));
			final double cos1 = Math.abs(Math.cos(rads1));
			final int w1 = (int) Math.floor((cleanedBIForBB.getWidth() * cos1) + (cleanedBIForBB.getHeight() * sin1));
			final int h1 = (int) Math.floor((cleanedBIForBB.getHeight() * cos1) + (cleanedBIForBB.getWidth() * sin1));
			BufferedImage rotSauvolaImage = new BufferedImage(w1, h1, BufferedImage.TYPE_BYTE_GRAY);
			Graphics2D g1 = rotSauvolaImage.createGraphics();
			g1.setColor(Color.WHITE);
			g1.fillRect(0, 0, w1, h1);
			RenderingHints rh3 = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g1.setRenderingHints(rh3);
			RenderingHints rh4 = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g1.setRenderingHints(rh4);
			RenderingHints rh5 = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g1.setRenderingHints(rh5);
			final AffineTransform at1 = new AffineTransform();
			at1.rotate(rads1, w1 / 2, h1 / 2);
			g1.drawImage(cleanedBIForBB, at1, null);
			g1.dispose();
			Pix pixCleaned8bpp = null;
			try {
				pixCleaned8bpp = SBImage.getPixFromBufferedImage(rotSauvolaImage);
				PixDebugWriter.writeIfDebug(debugL, 3, pixCleaned8bpp, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "rotatedPreBB8bppImage-" + 1 + ".png");
				synchronized (interruptLock) {
					try1TesseractImage = Leptonica1.pixConvertTo1(pixCleaned8bpp, 136);
				}
				if (debugL <= 2) {
					System.out.println(
							"getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread1 - created rotated Pix try1TesseractImage for Tesseract");
				}
			} catch (Exception e) {
				e.printStackTrace();
				return Boolean.FALSE;
			}
			PixDebugWriter.writeIfDebug(debugL, 3, try1TesseractImage, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "imageForTesseract_1bpp-" + 1 + ".png");

			// erode and get the pix for bounding boxes
			Pix pixCleanedBB_8bpp = null;
			if (CheckProductProperties.productIsGiven) {
				pixCleanedBB_8bpp = Leptonica1.pixErodeGray(pixCleaned8bpp, PIG_T1_nXPixels_BBErode,
						PIG_T1_nYPixels_BBErode);
			} else {
				pixCleanedBB_8bpp = Leptonica1.pixErodeGray(pixCleaned8bpp, NPIG_T1_nXPixels_BBErode,
						NPIG_T1_nYPixels_BBErode);
			}
			synchronized (interruptLock) {
				try1BBImage = Leptonica1.pixConvertTo1(pixCleanedBB_8bpp, 136);
			}
			if (debugL <= 2) {
				System.out.println(
						"getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread1 - created rotated Pix try1BBImage for bounding boxes");
			}
			LeptUtils.dispose(pixCleaned8bpp);
			LeptUtils.dispose(pixCleanedBB_8bpp);
			PixDebugWriter.writeIfDebug(debugL, 3, try1BBImage, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "imageForBB_1bpp-" + 1 + ".png");

			if (try1BBImage != null) {
				try1BoundingBoxes = getBoundingBoxes(try1BBImage, 1, debugL);
			}
			if (debugL <= 2) {
				System.out.println(
						"Exiting getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread1 with Boolean.TRUE - got bounding boxes");
			}
			return Boolean.TRUE;
		});

		// rotate image# 2 to get the BufferedImage for Tesseract
		getImagesAndBoundingBoxesThread2 = CompletableFuture.supplyAsync(() -> {
			if (!CheckProductProperties.productIsGiven) {
				return Boolean.TRUE;
			}
			if (debugL <= 2) {
				System.out.println("Entering getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread2");
			}
			try2ThreadFinished = try2Thread.join(); // wait for try2Thread to complete
			Pix pixCleaned8bpp = null;
			if (!getInterruptParallelThreads() && try2ThreadFinished) {
				BufferedImage cleanedBIForBB = null;
				try {
					synchronized (interruptLock) {
						if (try2PreBBImage != null) {
							cleanedBIForBB = LeptUtils.convertPixToImage(try2PreBBImage);
						} else {
							return Boolean.FALSE;
						}
					}
					if (debugL <= 2) {
						System.out.println(
								"getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread2 - created cleaned BI for Tesseract");
					}
				} catch (Exception e) {
					e.printStackTrace();
					return Boolean.FALSE;
				}
				final double rads1 = Math.toRadians(try1RotationAngle);
				final double sin1 = Math.abs(Math.sin(rads1));
				final double cos1 = Math.abs(Math.cos(rads1));
				final int w1 = (int) Math
						.floor((cleanedBIForBB.getWidth() * cos1) + (cleanedBIForBB.getHeight() * sin1));
				final int h1 = (int) Math
						.floor((cleanedBIForBB.getHeight() * cos1) + (cleanedBIForBB.getWidth() * sin1));
				BufferedImage rotSauvolaImage = new BufferedImage(w1, h1, BufferedImage.TYPE_BYTE_GRAY);
				Graphics2D g1 = rotSauvolaImage.createGraphics();
				g1.setColor(Color.WHITE);
				g1.fillRect(0, 0, w1, h1);
				RenderingHints rh3 = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
						RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g1.setRenderingHints(rh3);
				RenderingHints rh4 = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				g1.setRenderingHints(rh4);
				RenderingHints rh5 = new RenderingHints(RenderingHints.KEY_RENDERING,
						RenderingHints.VALUE_RENDER_QUALITY);
				g1.setRenderingHints(rh5);
				final AffineTransform at1 = new AffineTransform();
				at1.rotate(rads1, w1 / 2, h1 / 2);
				g1.drawImage(cleanedBIForBB, at1, null);
				g1.dispose();
				try {
					pixCleaned8bpp = SBImage.getPixFromBufferedImage(rotSauvolaImage);
					synchronized (interruptLock) {
						try2TesseractImage = Leptonica1.pixConvertTo1(pixCleaned8bpp, 136);
					}
					if (debugL <= 2) {
						System.out.println(
								"getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread2 - created rotated Pix try2TesseractImage for Tesseract");
					}
				} catch (Exception e) {
					e.printStackTrace();
					return Boolean.FALSE;
				}
				PixDebugWriter.writeIfDebug(debugL, 3, try2TesseractImage, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "imageForTesseract_1bpp-" + 2 + ".png");
			} else {
				LeptUtils.dispose(pixCleaned8bpp);
				if (debugL <= 2) {
					System.out.println(
							"Exiting getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread2 - 1");
				}
				return Boolean.FALSE;
			}

			if (getInterruptParallelThreads()) {
				LeptUtils.dispose(pixCleaned8bpp);
				if (debugL <= 2) {
					System.out.println(
							"Exiting getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread2 - 2");
				}
				return Boolean.FALSE;
			} else {
				// erode and get the pix for bounding boxes
				Pix pixCleanedBB_8bpp = Leptonica1.pixErodeGray(pixCleaned8bpp, PIG_T2_nXPixels_BBErode,
						PIG_T2_nYPixels_BBErode);
				synchronized (interruptLock) {
					try2BBImage = Leptonica1.pixConvertTo1(pixCleanedBB_8bpp, 136);
				}
				if (debugL <= 2) {
					System.out.println(
							"getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread2 - created rotated Pix try2BBImage for BoundingBoxes");
				}
				LeptUtils.dispose(pixCleaned8bpp);
				LeptUtils.dispose(pixCleanedBB_8bpp);
				PixDebugWriter.writeIfDebug(debugL, 3, try2BBImage, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "imageForBB_1bpp-" + 2 + ".png");
			}
			if (getInterruptParallelThreads()) {
				if (debugL <= 2) {
					System.out.println(
							"Exiting getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread2 - 3");
				}
				return Boolean.FALSE;
			} else {
				Pix aTempImage = null;
				synchronized (interruptLock) {
					if (try2BBImage != null) {
						aTempImage = Leptonica1.pixCopy(null, try2BBImage);
					}
				}
				if (aTempImage == null) {
					LeptUtils.dispose(aTempImage);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread2 - 4");
					}
					return Boolean.FALSE;
				} else {
					try2BoundingBoxes = getBoundingBoxes(aTempImage, 2, debugL);
					if (debugL <= 2) {
						System.out.println(
								"getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread2 - got bounding boxes");
					}
					LeptUtils.dispose(aTempImage);
				}
			}
			if (debugL <= 2) {
				System.out.println(
						"Exiting getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread2 with Boolean.TRUE - 5");
			}
			return Boolean.TRUE;
		}, innerThreadService);

		// rotate image# 3 to get the BufferedImage for Tesseract
		getImagesAndBoundingBoxesThread3 = CompletableFuture.supplyAsync(() -> {
			if (debugL <= 2) {
				System.out.println("Entering getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread3");
			}
			try3ThreadFinished = try3Thread.join(); // wait for thread 2 to complete
			Pix pixCleaned8bpp = null;
			if (!getInterruptParallelThreads() && try3ThreadFinished) {
				BufferedImage cleanedBIForBB = null;
				try {
					synchronized (interruptLock) {
						if (try3PreBBImage != null) {
							cleanedBIForBB = LeptUtils.convertPixToImage(try3PreBBImage);
						} else {
							return Boolean.FALSE;
						}
					}
					if (debugL <= 2) {
						System.out.println(
								"getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread3 - created cleaned BI for Tesseract");
					}
				} catch (Exception e) {
					e.printStackTrace();
					return Boolean.FALSE;
				}
				final double rads1 = Math.toRadians(try1RotationAngle);
				final double sin1 = Math.abs(Math.sin(rads1));
				final double cos1 = Math.abs(Math.cos(rads1));
				final int w1 = (int) Math
						.floor((cleanedBIForBB.getWidth() * cos1) + (cleanedBIForBB.getHeight() * sin1));
				final int h1 = (int) Math
						.floor((cleanedBIForBB.getHeight() * cos1) + (cleanedBIForBB.getWidth() * sin1));
				BufferedImage rotSauvolaImage = new BufferedImage(w1, h1, BufferedImage.TYPE_BYTE_GRAY);
				Graphics2D g1 = rotSauvolaImage.createGraphics();
				g1.setColor(Color.WHITE);
				g1.fillRect(0, 0, w1, h1);
				RenderingHints rh3 = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
						RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g1.setRenderingHints(rh3);
				RenderingHints rh4 = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				g1.setRenderingHints(rh4);
				RenderingHints rh5 = new RenderingHints(RenderingHints.KEY_RENDERING,
						RenderingHints.VALUE_RENDER_QUALITY);
				g1.setRenderingHints(rh5);
				final AffineTransform at1 = new AffineTransform();
				at1.rotate(rads1, w1 / 2, h1 / 2);
				g1.drawImage(cleanedBIForBB, at1, null);
				g1.dispose();
				try {
					pixCleaned8bpp = SBImage.getPixFromBufferedImage(rotSauvolaImage);
					synchronized (interruptLock) {
						try3TesseractImage = Leptonica1.pixConvertTo1(pixCleaned8bpp, 136);
					}
					if (debugL <= 2) {
						System.out.println(
								"getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread3 - created rotated Pix try3TesseractImage for Tesseract");
					}
				} catch (Exception e) {
					e.printStackTrace();
					return Boolean.FALSE;
				}
				PixDebugWriter.writeIfDebug(debugL, 3, try3TesseractImage, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "imageForTesseract_1bpp-" + 3 + ".png");
			} else {
				LeptUtils.dispose(pixCleaned8bpp);
				if (debugL <= 2) {
					System.out.println(
							"Exiting getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread3 - 1");
				}
				return Boolean.FALSE;
			}

			if (getInterruptParallelThreads()) {
				LeptUtils.dispose(pixCleaned8bpp);
				if (debugL <= 2) {
					System.out.println(
							"Exiting getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread3 - 2");
				}
				return Boolean.FALSE;
			} else {
				// erode and get the pix for bounding boxes
				Pix pixCleanedBB_8bpp = Leptonica1.pixErodeGray(pixCleaned8bpp, PIG_T2_nXPixels_BBErode,
						PIG_T2_nYPixels_BBErode);
				synchronized (interruptLock) {
					try3BBImage = Leptonica1.pixConvertTo1(pixCleanedBB_8bpp, 136);
				}
				if (debugL <= 2) {
					System.out.println(
							"getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread3 - created rotated Pix try3BBImage for BoundingBoxes");
				}
				LeptUtils.dispose(pixCleaned8bpp);
				LeptUtils.dispose(pixCleanedBB_8bpp);
				PixDebugWriter.writeIfDebug(debugL, 3, try3BBImage, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "imageForBB_1bpp-" + 3 + ".png");
			}

			if (getInterruptParallelThreads()) {
				if (debugL <= 2) {
					System.out.println(
							"Exiting getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread3 - 3");
				}
				return Boolean.FALSE;
			} else {
				Pix aTempImage = null;
				synchronized (interruptLock) {
					if (try3BBImage != null) {
						aTempImage = Leptonica1.pixCopy(null, try3BBImage);
					}
				}
				if (aTempImage == null) {
					LeptUtils.dispose(aTempImage);
					if (debugL <= 2) {
						System.out.println(
								"Exiting getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread3 - 4");
					}
					return Boolean.FALSE;
				} else {
					try3BoundingBoxes = getBoundingBoxes(aTempImage, 3, debugL);
					if (debugL <= 2) {
						System.out.println(
								"getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread3 - got bounding boxes");
					}
					LeptUtils.dispose(aTempImage);
				}
			}
			if (debugL <= 2) {
				System.out.println(
						"Exiting getDerivativeImagesAndBoundingBoxes().getImagesAndBoundingBoxesThread3 with Boolean.TRUE - 5");
			}
			return Boolean.TRUE;
		}, innerThreadService);

		/*
		 * try3ThreadFinished = try3Thread.join(); // wait for thread 3 to complete Pix
		 * pixCleaned8bpp = null; if (!getInterruptParallelThreads() &&
		 * try3ThreadFinished) { BufferedImage cleanedBIForBB = null; try { synchronized
		 * (interruptLock) { if (try2PreBBImage != null) { cleanedBIForBB =
		 * LeptUtils.convertPixToImage(try3PreBBImage); } else { return Boolean.FALSE; }
		 * } } catch (Exception e) { e.printStackTrace(); return Boolean.FALSE; } final
		 * double rads1 = Math.toRadians(try1RotationAngle); final double sin1 =
		 * Math.abs(Math.sin(rads1)); final double cos1 = Math.abs(Math.cos(rads1));
		 * final int w1 = (int) Math .floor((cleanedBIForBB.getWidth() * cos1) +
		 * (cleanedBIForBB.getHeight() * sin1)); final int h1 = (int) Math
		 * .floor((cleanedBIForBB.getHeight() * cos1) + (cleanedBIForBB.getWidth() *
		 * sin1)); BufferedImage rotSauvolaImage = new BufferedImage(w1, h1,
		 * BufferedImage.TYPE_BYTE_GRAY); Graphics2D g1 =
		 * rotSauvolaImage.createGraphics(); g1.setColor(Color.WHITE); g1.fillRect(0, 0,
		 * w1, h1); RenderingHints rh3 = new
		 * RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
		 * RenderingHints.VALUE_TEXT_ANTIALIAS_ON); g1.setRenderingHints(rh3);
		 * RenderingHints rh4 = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
		 * RenderingHints.VALUE_INTERPOLATION_BICUBIC); g1.setRenderingHints(rh4);
		 * RenderingHints rh5 = new RenderingHints(RenderingHints.KEY_RENDERING,
		 * RenderingHints.VALUE_RENDER_QUALITY); g1.setRenderingHints(rh5); final
		 * AffineTransform at1 = new AffineTransform(); at1.rotate(rads1, w1 / 2, h1 /
		 * 2); g1.drawImage(cleanedBIForBB, at1, null); g1.dispose(); try {
		 * pixCleaned8bpp = SBImage.getPixFromBufferedImage(rotSauvolaImage);
		 * synchronized (interruptLock) { try3TesseractImage =
		 * Leptonica1.pixConvertTo1(pixCleaned8bpp, 136); } } catch (Exception e) {
		 * e.printStackTrace(); return Boolean.FALSE; } if (debugL <= 3) {
		 * Leptonica1.pixWrite( DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" +
		 * "imageForTesseract_1bpp-" + 3 + ".png", try3TesseractImage,
		 * ILeptonica.IFF_PNG); } } else { return Boolean.FALSE; }
		 * 
		 * if (getInterruptParallelThreads()) { LeptUtils.dispose(pixCleaned8bpp);
		 * return Boolean.FALSE; } else { // erode and get the pix for bounding boxes
		 * Pix pixCleanedBB_8bpp = null; if (CheckProductProperties.productIsGiven) {
		 * pixCleanedBB_8bpp = Leptonica1.pixErodeGray(pixCleaned8bpp,
		 * PIG_T3_nXPixels_BBErode, PIG_T3_nYPixels_BBErode); synchronized
		 * (interruptLock) { try3BBImage = Leptonica1.pixConvertTo1(pixCleanedBB_8bpp,
		 * 136); } } else { pixCleanedBB_8bpp = Leptonica1.pixErodeGray(pixCleaned8bpp,
		 * NPIG_T3_nXPixels_BBErode, NPIG_T3_nYPixels_BBErode); synchronized
		 * (interruptLock) { try3BBImage = Leptonica1.pixConvertTo1(pixCleanedBB_8bpp,
		 * 136); } } LeptUtils.dispose(pixCleaned8bpp);
		 * LeptUtils.dispose(pixCleanedBB_8bpp); if (debugL <= 3) {
		 * Leptonica1.pixWrite(DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" +* "imageForBB_1bpp-" + 3 + ".png", try3BBImage, ILeptonica.IFF_PNG); } } if
		 * (getInterruptParallelThreads()) { return Boolean.FALSE; } else { Pix
		 * aTempImage = null; synchronized (interruptLock) { if (try3BBImage != null) {
		 * aTempImage = Leptonica1.pixCopy(null, try3BBImage); } } if (aTempImage ==
		 * null) { LeptUtils.dispose(aTempImage); return Boolean.FALSE; } else {
		 * try3BoundingBoxes = getBoundingBoxes(aTempImage, 2, debugL);
		 * LeptUtils.dispose(aTempImage); } }
		 * 
		 * return Boolean.TRUE; });
		 */

//		while (!iteration1Done) {
//			Thread.onSpinWait();
//		}
	}

	public static ProductDescription getFinalProductDescriptions(int debugL) {

		// if PRODUCT IS GIVEN
		ProductDescription combinedResult = new ProductDescription();
		if (CheckProductProperties.productIsGiven) {
			getImagesAndBBThread1Finished = getImagesAndBoundingBoxesThread1.join();
//			while (!iteration1Done) {
//				Thread.onSpinWait();
//			}
			CompletableFuture<ArrayList<OCRStringWrapper>> firstTry = CompletableFuture.supplyAsync(() -> {
				ArrayList<OCRStringWrapper> resultWrapper = new ArrayList<>();
				if (!getImagesAndBBThread1Finished) {
					firstShotDone = true;
					return resultWrapper; // return null
				}
				try {
					ArrayList<OCRBufferedImageWrapper> biWrapper = getOCRBufferedImageWrapperArrayFast(
							try1TesseractImage, try1BoundingBoxes, processDataWrapper, outerThreadService,
							innerThreadService, 1, debugL);
					resultWrapper = ocrBIWrapperArray(biWrapper, outerThreadService, 1, debugLevel);
					if (debugL <= 12) {
						System.out.println("For try 1 :");
						System.out.println("-----------");
					}
					writeOCRStrings(resultWrapper);
				} catch (Exception e) {
					e.printStackTrace();
					return resultWrapper; // return null
				}
				return resultWrapper;
			});
			CompletableFuture<ArrayList<OCRStringWrapper>> secondTry = CompletableFuture.supplyAsync(() -> {
				getImagesAndBBThread2Finished = getImagesAndBoundingBoxesThread2.join();
				ArrayList<OCRStringWrapper> resultWrapper = new ArrayList<OCRStringWrapper>();
				if (!getImagesAndBBThread2Finished) {
					// firstShotDone = true;
					return resultWrapper; // return null
				}
				if (getInterruptParallelThreads()) {
					// firstShotDone = true;
					return resultWrapper;
				}
				try {
					if (debugL <= 12) {
						System.out.println("For try 2 : ProductIsGiven");
						System.out.println("try2TesseractImage = " + try2TesseractImage);
						System.out.println("try2BoundingBoxes = " + try2BoundingBoxes);
					}
					ArrayList<OCRBufferedImageWrapper> biWrapper = getOCRBufferedImageWrapperArrayFast(
							try2TesseractImage, try2BoundingBoxes, processDataWrapper, outerThreadService,
							innerThreadService, 2, debugL);
					if (getInterruptParallelThreads()) {
						return resultWrapper;
					}
					resultWrapper = ocrBIWrapperArray(biWrapper, outerThreadService, 2, debugLevel);
					if (debugL <= 12) {
						System.out.println("For try 2 :");
						System.out.println("-----------");
					}
					writeOCRStrings(resultWrapper);
				} catch (Exception e) {
					e.printStackTrace();
					return resultWrapper; // return null
				}
				return resultWrapper;
			}, innerThreadService);
			ArrayList<OCRStringWrapper> firstResultWrapper = firstTry.join();
			firstShotDone = true;
			ProductDescription productFirstTry = ProductPriceData.validateLabels(firstResultWrapper, debugLevel);
			if (debugL <= 12) {
				System.out.println("Try 1 : Product = " + productFirstTry);
			}
			if (productFirstTry != null) {
				combinedResult = productFirstTry.merge(combinedResult);
			}
			if (debugL <= 13) {
				System.out.println("	Try 1 : Product = " + combinedResult);
			}
			if (combinedResult.productOK == ProductDescription.ALL_OK) {
				if (debugL <= 2) {
					System.out.println("Try 1 : Entered ifelse condition \"Product is OK \"");
				}
				tryNumber = 1;
				setInterruptParallelThreads();
				CompletableFuture<Void> discardingFuture = CompletableFuture.runAsync(() -> {
					getImagesAndBBThread3Finished = false;
					getImagesAndBoundingBoxesThread3.join();
					try {
						secondTry.join();
						// secondTry.getNow(new ArrayList<OCRStringWrapper>());
					} catch (Exception e) {

					}
				});
				return combinedResult;
			} else {
				if (debugL <= 2) {
					System.out.println("Try 1 : Product is not OK ");
				}
			}
			ArrayList<OCRStringWrapper> secondResultWrapper = secondTry.join();
			if (secondResultWrapper != null) {
				ProductDescription productSecondTry = ProductPriceData.validateLabels(secondResultWrapper, debugLevel);
				if (debugL <= 12) {
					System.out.println("Try 2 : Product = " + productSecondTry);
				}
				if (productSecondTry != null) {
					combinedResult = productSecondTry.merge(combinedResult);
				}
				if (debugL <= 13) {
					System.out.println("	Try 2 : Product = " + combinedResult);
				}
			} else {
				if (debugL <= 2) {
					System.out.println("Try 2 : secondResultWrapper is null ");
				}
			}
			if (combinedResult.productOK == ProductDescription.ALL_OK) {
				tryNumber = 2;
				setInterruptParallelThreads();
				CompletableFuture<Void> discardingFuture = CompletableFuture.runAsync(() -> {
					getImagesAndBBThread3Finished = false;
					getImagesAndBoundingBoxesThread3.join();
				});

				return combinedResult;
			}
			boolean worthARetry = areViableBoundingBoxesPresent(try1BoundingBoxes)
					|| areSomeCharactersLikelyPresent(try1TesseractImage);
			if (debugLevel <= 12) {
				System.out.println("In getFinalProduct() - productIsGiven : worthARetry = " + worthARetry);
				System.out.println(
						"In getFinalProduct() - productIsGiven : checking if parallel runs of 3 and 4 can be done : currentProduct = "
								+ combinedResult);
			}
			if ((combinedResult.productOK != ProductDescription.ALL_OK) && (worthARetry)
					&& (("".equals(combinedResult.finalPrice)) || ("".equals(combinedResult.month))
							|| ("0".equals(combinedResult.finalPrice)) || ("".equals(combinedResult.year))
							|| (!combinedResult.priceHasADot) || !("".equals(combinedResult.rejectionReason)))) {
				if (debugLevel <= 4) {
					System.out.println("NEED TO DO parallel runs 3 and 4 as "
							+ (("".equals(combinedResult.finalPrice)) ? ("finalPrice = ;") : (""))
							+ (("0".equals(combinedResult.finalPrice)) ? ("finalPrice = 0;") : (""))
							+ (("".equals(combinedResult.month)) ? ("month = ;") : (""))
							+ (("".equals(combinedResult.year)) ? ("year = ;") : ("")));
					System.out.println("*************************************************");
					System.out.println("*****************Doing parallel runs 3 and 4*****");
				}

				CompletableFuture<ArrayList<OCRStringWrapper>> thirdTry = CompletableFuture.supplyAsync(() -> {
					getImagesAndBBThread3Finished = getImagesAndBoundingBoxesThread3.join();
					ArrayList<OCRStringWrapper> resultWrapper = new ArrayList<>();
					if (!getImagesAndBBThread3Finished) {
						return resultWrapper; // return null
					}
					try {
						ArrayList<OCRBufferedImageWrapper> biWrapper = getOCRBufferedImageWrapperArrayFast(
								try3TesseractImage, try3BoundingBoxes, processDataWrapper, outerThreadService,
								innerThreadService, 3, debugL);
						resultWrapper = ocrBIWrapperArray(biWrapper, outerThreadService, 3, debugLevel);
						if (debugL <= 12) {
							System.out.println("For try 3 :");
							System.out.println("-----------");
						}
						writeOCRStrings(resultWrapper);
					} catch (Exception e) {
						e.printStackTrace();
						return resultWrapper; // return null
					}
					return resultWrapper;
				}, innerThreadService);
				CompletableFuture<ArrayList<OCRStringWrapper>> fourthTry = CompletableFuture.supplyAsync(() -> {
					ArrayList<OCRStringWrapper> resultWrapper = new ArrayList<>();
					if (getInterruptParallelThreads()) {
						return resultWrapper;
					}
					try {
						resultWrapper = ocrBNandCNImages(debugLevel);
						if (debugL <= 12) {
							System.out.println("For try 4 :");
							System.out.println("-----------");
						}
						writeOCRStrings(resultWrapper);
					} catch (Exception e) {
						e.printStackTrace();
						return resultWrapper; // return null
					}
					return resultWrapper;
				}, innerThreadService);
				ArrayList<OCRStringWrapper> thirdResultWrapper = thirdTry.join();
				if (thirdResultWrapper != null) {
					ProductDescription productThirdTry = ProductPriceData.validateLabels(thirdResultWrapper,
							debugLevel);
					if (debugL <= 12) {
						System.out.println("Try 3 : Product = " + productThirdTry);
					}
					if (productThirdTry != null) {
						combinedResult = productThirdTry.merge(combinedResult);
					}
					if (debugL <= 13) {
						System.out.println("	Try 3 : Product = " + combinedResult);
					}
				}
				if (combinedResult.productOK == ProductDescription.ALL_OK) {
					tryNumber = 3;
					CompletableFuture<Void> discardingFuture = CompletableFuture.runAsync(() -> {
						// fourthTry.join();
						try {
							fourthTry.join();
							// fourthTry.getNow(new ArrayList<OCRStringWrapper>());
						} catch (Exception e) {

						}
					});
					return combinedResult;
				}
				ArrayList<OCRStringWrapper> fourthResultWrapper = fourthTry.join();
				if (fourthResultWrapper != null) {
					ProductDescription productFourthTry = ProductPriceData.validateLabels(fourthResultWrapper,
							debugLevel);
					if (debugL <= 12) {
						System.out.println("Try 4 : Product = " + productFourthTry);
					}
					if (productFourthTry != null) {
						combinedResult = productFourthTry.merge(combinedResult);
					}
					if (debugL <= 13) {
						System.out.println("	Try 4 : Product = " + combinedResult);
					}
				}
				if (debugLevel <= 4) {
					System.out.println("*****************Finished with parallel runs 3 and 4****************");
					System.out.println("*************************************************");
				}
				tryNumber = 4;
				return combinedResult;
			} else {
				if (debugL <= 12) {
					System.out.println("Determined that no further tries are either needed or possible.");
				}
				setInterruptParallelThreads();
				CompletableFuture<Void> discardingFuture = CompletableFuture.runAsync(() -> {
					getImagesAndBBThread3Finished = false;
					getImagesAndBoundingBoxesThread3.join();
				});
				return combinedResult;
			}
		}

		// if PRODUCT IS NOT GIVEN
		if (!CheckProductProperties.productIsGiven) {
//			while (!iteration1Done) {
//				Thread.onSpinWait();
//			}

			getImagesAndBBThread1Finished = getImagesAndBoundingBoxesThread1.join();
			// Thread2 has no processing. So, there is no loss of time to have this thread
			// join the main thread here.
			getImagesAndBBThread2Finished = getImagesAndBoundingBoxesThread2.join();
			// this wrapper is common to tries 1 and 2
			final ArrayList<OCRBufferedImageWrapper> biWrapper = getOCRBufferedImageWrapperArrayFast(try1TesseractImage,
					try1BoundingBoxes, processDataWrapper, outerThreadService, innerThreadService, 1, debugL);
			CompletableFuture<ArrayList<OCRStringWrapper>> firstTry = CompletableFuture.supplyAsync(() -> {
				ArrayList<OCRStringWrapper> resultWrapper = new ArrayList<>();
				if (!getImagesAndBBThread1Finished) {
					firstShotDone = true;
					return resultWrapper; // return null
				}
				try {
					resultWrapper = ocrBIWrapperArray(biWrapper, outerThreadService, 1, debugLevel);
					if (debugL <= 12) {
						System.out.println("For try 1 :");
						System.out.println("-----------");
					}
					writeOCRStrings(resultWrapper);
				} catch (Exception e) {
					e.printStackTrace();
					return resultWrapper; // return null
				}
				return resultWrapper;
			});
			CompletableFuture<ArrayList<OCRStringWrapper>> secondTry = CompletableFuture.supplyAsync(() -> {
				ArrayList<OCRStringWrapper> resultWrapper = new ArrayList<>();
				if (!getImagesAndBBThread1Finished) { // since thread 2 uses the images from thread 1
					return resultWrapper;
				}
				if (getInterruptParallelThreads()) {
					return resultWrapper;
				}
				try {
					resultWrapper = ocrBIWrapperArray(biWrapper, innerThreadService, 2, debugLevel);
					if (debugL <= 12) {
						System.out.println("For try 2 :");
						System.out.println("-----------");
					}
					writeOCRStrings(resultWrapper);
				} catch (Exception e) {
					e.printStackTrace();
					return resultWrapper; // return null
				}
				return resultWrapper;
			}, innerThreadService);
			ArrayList<OCRStringWrapper> firstResultWrapper = firstTry.join();
			firstShotDone = true;
			ProductDescription productFirstTry = ProductPriceData.validateLabels(firstResultWrapper, debugLevel);
			if (debugL <= 12) {
				System.out.println("Try 1 no product given : Product = " + productFirstTry);
			}
			if (productFirstTry != null) {
				combinedResult = productFirstTry.merge(combinedResult);
			}
			if (debugL <= 13) {
				System.out.println("	Try 1 : Product = " + combinedResult);
			}

			if (combinedResult.productOK == ProductDescription.ALL_OK) {
				if (debugL <= 2) {
					System.out.println("Try 1 no product given : ProductOK = ALL_OK");
				}
				tryNumber = 1;
				setInterruptParallelThreads();
				CompletableFuture<Void> discardingFuture = CompletableFuture.runAsync(() -> {
					getImagesAndBBThread3Finished = false;
					getImagesAndBoundingBoxesThread3.join();
					try {
						secondTry.join();
						// secondTry.getNow(new ArrayList<OCRStringWrapper>());
					} catch (Exception e) {

					}
				});
				return combinedResult;
			} else {
				if (debugL <= 2) {
					System.out.println("Try 1 no product given : Product is not OK");
				}
			}
			ArrayList<OCRStringWrapper> secondResultWrapper = secondTry.join();
			if (secondResultWrapper != null) {
				ProductDescription productSecondTry = ProductPriceData.validateLabels(secondResultWrapper, debugLevel);
				if (debugL <= 12) {
					System.out.println("Try 2 : Product = " + productSecondTry);
				}
				if (productSecondTry != null) {
					combinedResult = productSecondTry.merge(combinedResult);
				}
				if (debugL <= 13) {
					System.out.println("	Try 2 : Product = " + combinedResult);
				}
			}
			if (combinedResult.productOK == ProductDescription.ALL_OK) {
				tryNumber = 2;
				setInterruptParallelThreads();
				CompletableFuture<Void> discardingFuture = CompletableFuture.runAsync(() -> {
					getImagesAndBBThread3Finished = false;
					getImagesAndBoundingBoxesThread3.join();
				});
				return combinedResult;
			}

			boolean worthARetry = areViableBoundingBoxesPresent(try1BoundingBoxes)
					|| areSomeCharactersLikelyPresent(try1TesseractImage);
			if (debugLevel <= 2) {
				System.out.println("In getFinalProduct() - productIsGiven : worthARetry = " + worthARetry);
				System.out.println(
						"In getFinalProduct() - productIsGiven : checking if run 3 can be done : currentProduct = "
								+ combinedResult);
			}
			if ((combinedResult.productOK != ProductDescription.ALL_OK) && (worthARetry)
					&& (("".equals(combinedResult.finalPrice)) || ("".equals(combinedResult.month))
							|| ("0".equals(combinedResult.finalPrice)) || ("".equals(combinedResult.year))
							|| (!combinedResult.priceHasADot) || !("".equals(combinedResult.rejectionReason)))) {
				if (debugLevel <= 4) {
					System.out.println("NEED TO DO run 3 as "
							+ (("".equals(combinedResult.finalPrice)) ? ("finalPrice = ;") : (""))
							+ (("0".equals(combinedResult.finalPrice)) ? ("finalPrice = 0;") : (""))
							+ (("".equals(combinedResult.month)) ? ("month = ;") : (""))
							+ (("".equals(combinedResult.year)) ? ("year = ;") : ("")));
					System.out.println("*************************************************");
					System.out.println("*****************Doing run 3 *****");
				}
				getImagesAndBBThread3Finished = getImagesAndBoundingBoxesThread3.join();
				CompletableFuture<ProductDescription> thirdTry = CompletableFuture.supplyAsync(() -> {
					ProductDescription product = null;
					if (!getImagesAndBBThread3Finished) {
						return product; // return null
					}
					try {
						ArrayList<OCRBufferedImageWrapper> biWrapper1 = getOCRBufferedImageWrapperArrayFast(
								try3TesseractImage, try3BoundingBoxes, processDataWrapper, outerThreadService,
								innerThreadService, 3, debugL);
						ArrayList<OCRStringWrapper> resultWrapper = ocrBIWrapperArray(biWrapper1, outerThreadService, 3,
								debugLevel);
						if (debugL <= 12) {
							System.out.println("For try 3 :");
							System.out.println("-----------");
						}
						writeOCRStrings(resultWrapper);
						product = ProductPriceData.validateLabels(resultWrapper, debugLevel);
					} catch (Exception e) {
						e.printStackTrace();
						return product; // return null
					}
					return product;
				}, innerThreadService);
				ProductDescription productThirdTry = thirdTry.join();
				if (debugL <= 12) {
					System.out.println("Try 3 : Product = " + productThirdTry);
				}
				if (productThirdTry != null) {
					combinedResult = productThirdTry.merge(combinedResult);
				}
				if (debugL <= 13) {
					System.out.println("	Try 3 : Product = " + combinedResult);
				}
				if (debugLevel <= 4) {
					System.out.println("*****************Finished with parallel run 3 ****************");
					System.out.println("*************************************************");
				}
				tryNumber = 3;
				return combinedResult;
			} else {
				if (debugL <= 12) {
					System.out.println("Determined that no further tries are either not needed or not possible");
				}
				tryNumber = 2;
				setInterruptParallelThreads();
				CompletableFuture<Void> discardingFuture = CompletableFuture.runAsync(() -> {
					getImagesAndBBThread3Finished = false;
					getImagesAndBoundingBoxesThread3.join();
				});
				return combinedResult;
			}
		}
		return combinedResult;
	}

	public static ArrayList<OCRStringWrapper> ocrBNandCNImages(int debugL) {

		float hsFactor = 1f;
		float wsFactor = 1f;

		if (CheckProductProperties.productIsGiven) {
			DescriptiveStatistics heightStats = new DescriptiveStatistics();
			for (Rectangle aBox : allBoxes) {
				if (aBox != null) {
					heightStats.addValue(aBox.height);
				}
			}

			double medianHeight = 0;
			if (heightStats.getN() > 0) {
				medianHeight = heightStats.getPercentile(50.0);
			}
			if (debugL <= 12) {
				System.out.println("Thread 4 : ocrBNandCNImages: Median Height is : " + medianHeight);
			}

			if (medianHeight != 0) {
				hsFactor = (float) (idealCharHeight / medianHeight);
			}

			DescriptiveStatistics widthStats = new DescriptiveStatistics();
			for (Rectangle aBox : allBoxes) {
				if (aBox != null) {
					if ((aBox.height <= 1.075 * medianHeight) && (aBox.height >= 0.925 * medianHeight)) {
						widthStats.addValue(aBox.width);
					}
				}
			}

			double medianWidth = 0;
			if (widthStats.getN() > 0) {
				widthStats.getPercentile(50.0);
			}

			if (medianHeight != 0) {
				if (medianWidth / medianHeight < 1.75) {
					// then, we have the right width
					wsFactor = (float) (idealCharWidth / medianWidth);
				}
			}

			if (debugL <= 12) {
				System.out.println("Thread 4 : ocrBNandCNImages: Median Width is : " + medianWidth);
			}

		}

		final float heightScaleFactor = hsFactor;
		final float widthScaleFactor = wsFactor;

		final ArrayList<OCRStringWrapper> result = new ArrayList<>();

		CompletableFuture<Boolean> cnTesseract = CompletableFuture.supplyAsync(() -> {
			if (debugL <= 5) {
				System.out.println(
						"Thread 4 : ocrBNandCNImages: In tesseractBNandCNImages() : Starting processing of contrastNormalisedImage");
			}

			boolean success = true;
			String aResult = "";

			try {
				if (getInterruptParallelThreads()) {
					OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
					aWrapper.setOcrString(aResult);
					result.add(aWrapper);
					return Boolean.FALSE;
				}
				Pix cnCopy = null;
				Pix cnErodedImage = null;
				synchronized (interruptLock) {
					cnCopy = Leptonica1.pixScaleGeneral(contrastNormalisedImage, widthScaleFactor, heightScaleFactor,
							0.0f, 1);
				}
				cnErodedImage = Leptonica1.pixErodeGray(cnCopy, 2, 2);
				LeptUtils.dispose(cnCopy);
				if (cnErodedImage == null) {
					LeptUtils.dispose(cnErodedImage);
					OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
					aWrapper.setOcrString(aResult);
					result.add(aWrapper);
					return Boolean.FALSE;
				}
				BufferedImage image = LeptUtils.convertPixToImage(cnErodedImage);
				LeptUtils.dispose(cnErodedImage);
				if (getInterruptParallelThreads()) {
					OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
					aWrapper.setOcrString(aResult);
					result.add(aWrapper);
					return Boolean.FALSE;
				}
				BufferedImage rotated = rotate(image);

				TechWerxTesseractHandle instanceHandle = null;
				try {
					synchronized (originalImagesLock) {
						instanceHandle = (TechWerxTesseractHandle) originalImagesTesseractPool.borrowObject();
					}
				} catch (Exception e) {
					e.printStackTrace();
					try {
						synchronized (originalImagesLock) {
							originalImagesTesseractPool.returnObject(instanceHandle);
						}
					} catch (Exception e1) {
					}
					return Boolean.FALSE;
				}
				TechWerxTesseract instance = instanceHandle.getHandle();

				try {
					if (getInterruptParallelThreads()) {
						OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
						aWrapper.setOcrString(aResult);
						result.add(aWrapper);
						synchronized (originalImagesLock) {
							originalImagesTesseractPool.returnObject(instanceHandle);
						}
						return Boolean.FALSE;
					}
					aResult = instance.doOCR(rotated);
				} catch (Exception e) {
					success = false;
				}
				synchronized (originalImagesLock) {
					originalImagesTesseractPool.returnObject(instanceHandle);
				}
				aResult = aResult.replace(System.lineSeparator(), "  ");
				aResult = aResult.replace("\r\n", "  ");
				if (debugL <= 12) {
					System.out.println("Thread 4 - In tesseractBNandCNImages() : contrastNormalisedImage : " + aResult);
				}
			} catch (Exception e) {
				success = false;
				return Boolean.FALSE;
			}
			if (success) {
				if (debugL <= 9) {
					System.out.println(
							"Thread 4 : ocrBNandCNImages: In tesseractBNandCNImages() : the result from contrastNormalisedimage is - "
									+ aResult);
				}
				OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
				aWrapper.setOcrString(aResult);
				result.add(aWrapper);
			}
			return Boolean.TRUE;
		}, outerThreadService);

		CompletableFuture<Boolean> bnTesseract = CompletableFuture.supplyAsync(() -> {
			if (debugL <= 5) {
				System.out.println(
						"Thread 4 : ocrBNandCNImages: In tesseractBNandCNImages() : Starting processing of backgroundNormalisedImage");
			}

			boolean success = true;
			String aResult = "";

			try {
				Pix bnCopy = null;
				Pix bnErodedImage = null;
				synchronized (interruptLock) {
					bnCopy = Leptonica1.pixScaleGeneral(backgroundNormalisedImage, widthScaleFactor, heightScaleFactor,
							0.0f, 1);
				}
				bnErodedImage = Leptonica1.pixErodeGray(bnCopy, 2, 2);
				LeptUtils.dispose(bnCopy);
				if (bnErodedImage == null) {
					LeptUtils.dispose(bnErodedImage);
					OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
					aWrapper.setOcrString(aResult);
					result.add(aWrapper);
					return Boolean.FALSE;
				}
				BufferedImage image = LeptUtils.convertPixToImage(bnErodedImage);
				LeptUtils.dispose(bnErodedImage);
				if (getInterruptParallelThreads()) {
					OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
					aWrapper.setOcrString(aResult);
					result.add(aWrapper);
					return Boolean.FALSE;
				}
				BufferedImage rotated = rotate(image);
				if (getInterruptParallelThreads()) {
					OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
					aWrapper.setOcrString(aResult);
					result.add(aWrapper);
					return Boolean.FALSE;
				}
				TechWerxTesseractHandle instanceHandle = null;
				try {
					synchronized (originalImagesLock) {
						instanceHandle = (TechWerxTesseractHandle) originalImagesTesseractPool.borrowObject();
					}
				} catch (Exception e) {
					e.printStackTrace();
					try {
						synchronized (originalImagesLock) {
							originalImagesTesseractPool.returnObject(instanceHandle);
						}
					} catch (Exception e1) {
					}
					return Boolean.FALSE;
				}
				TechWerxTesseract instance = instanceHandle.getHandle();

				try {
					if (getInterruptParallelThreads()) {
						OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
						aWrapper.setOcrString(aResult);
						result.add(aWrapper);
						synchronized (originalImagesLock) {
							originalImagesTesseractPool.returnObject(instanceHandle);
						}
						return Boolean.FALSE;
					}
					aResult = instance.doOCR(rotated);
				} catch (Exception e) {
					success = false;
				}
				synchronized (originalImagesLock) {
					originalImagesTesseractPool.returnObject(instanceHandle);
				}

				aResult = aResult.replace(System.lineSeparator(), "  ");
				aResult = aResult.replace("\r\n", "  ");
				if (debugL <= 12) {
					System.out
							.println(
									"Thread 4 : ocrBNandCNImages: In tesseractBNandCNImages() : backgroundNormalisedImage : "
											+ aResult);
				}
			} catch (Exception e) {
				success = false;
				return Boolean.FALSE;
			}
			if (success) {
				if (debugL <= 9) {
					System.out.println(
							"Thread 4 : ocrBNandCNImages: In tesseractBNandCNImages() : the result from backgroundNormalisedImage is - "
									+ aResult);
				}
				OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
				aWrapper.setOcrString(aResult);
				result.add(aWrapper);
			}
			return Boolean.TRUE;
		}, outerThreadService);

		CompletableFuture<Boolean> umTesseract = CompletableFuture.supplyAsync(() -> {
			if (debugL <= 5) {
				System.out
						.println(
								"Thread 4 : ocrBNandCNImages: ocrBNandCNImages: In tesseractBNandCNImages() : Starting processing of unsharpMaskedImage");
			}

			boolean success = true;
			String aResult = "";

			try {
				// Pix unsharpCopy = Leptonica1.pixCopy(null, unsharpMaskedImage);
				// Pix umErodedImage = Leptonica1.pixErodeGray(unsharpCopy, 2, 2);
				if (getInterruptParallelThreads()) {
					OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
					aWrapper.setOcrString(aResult);
					result.add(aWrapper);
					return Boolean.FALSE;
				}
				// Pix umCopy = null;
				// Pix umErodedImage = null;
				// synchronized (interruptLock) {
				// 	umCopy = Leptonica1.pixCopy(null, unsharpMaskedImage);
				// }
				// umErodedImage = Leptonica1.pixErodeGray(umCopy, 2, 2);
				// LeptUtils.dispose(umCopy);
				// if (umErodedImage == null) {
				// 	LeptUtils.dispose(umErodedImage);
				// 	OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
				// 	aWrapper.setOcrString(aResult);
				// 	result.add(aWrapper);
				//	return Boolean.FALSE;
				// }
				Pix umCopy = null;
				synchronized (interruptLock) {
					umCopy = Leptonica1.pixScaleGeneral(unsharpMaskedImage, widthScaleFactor, heightScaleFactor, 0.0f,
							1);
				}
				// BufferedImage image = LeptUtils.convertPixToImage(umErodedImage);
				BufferedImage image = LeptUtils.convertPixToImage(umCopy);
				// LeptUtils.dispose(umErodedImage);
				// LeptUtils.dispose(unsharpCopy);
				LeptUtils.dispose(umCopy);
				if (getInterruptParallelThreads()) {
					OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
					aWrapper.setOcrString(aResult);
					result.add(aWrapper);
					return Boolean.FALSE;
				}
				BufferedImage rotated = rotate(image);
				TechWerxTesseractHandle instanceHandle = null;
				try {
					synchronized (originalImagesLock) {
						instanceHandle = (TechWerxTesseractHandle) originalImagesTesseractPool.borrowObject();
					}
				} catch (Exception e) {
					e.printStackTrace();
					try {
						synchronized (originalImagesLock) {
							originalImagesTesseractPool.returnObject(instanceHandle);
						}
					} catch (Exception e1) {
					}
					return Boolean.FALSE;
				}
				TechWerxTesseract instance = instanceHandle.getHandle();

				try {
					if (getInterruptParallelThreads()) {
						OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
						aWrapper.setOcrString(aResult);
						result.add(aWrapper);
						synchronized (originalImagesLock) {
							originalImagesTesseractPool.returnObject(instanceHandle);
						}
						return Boolean.FALSE;
					}
					aResult = instance.doOCR(rotated);
				} catch (Exception e) {
					success = false;
				}
				synchronized (originalImagesLock) {
					originalImagesTesseractPool.returnObject(instanceHandle);
				}

				aResult = aResult.replace(System.lineSeparator(), "  ");
				aResult = aResult.replace("\r\n", "  ");
				if (debugL <= 12) {
					System.out
							.println("Thread 4 : ocrBNandCNImages: In tesseractBNandCNImages() : unsharpMaskedImage : "
									+ aResult);
				}
			} catch (Exception e) {
				success = false;
				return Boolean.FALSE;
			}
			if (success) {
				if (debugL <= 9) {
					System.out.println(
							"Thread 4 : ocrBNandCNImages: In tesseractBNandCNImages() : the result from backgroundNormalisedImage is - "
									+ aResult);
				}
				OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
				aWrapper.setOcrString(aResult);
				result.add(aWrapper);
			}
			return Boolean.TRUE;
		}, outerThreadService);

		CompletableFuture<Boolean> originalTesseract1 = CompletableFuture.supplyAsync(() -> {
			if (debugL <= 5) {
				System.out.println(
						"Thread 4 : ocrBNandCNImages: In tesseractBNandCNImages() : Starting processing of tesseractImage1");
			}

			boolean success = true;
			String aResult = "";

			try {
				if (getInterruptParallelThreads()) {
					OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
					aWrapper.setOcrString(aResult);
					result.add(aWrapper);
					return Boolean.FALSE;
				}
				Pix aCopy = null;
				Pix binarized = null;
				Pix originalScaledCopy = null;
				// Pix originalErodedImage1 = null;
				// Pix originalErodedImage2 = null;
				// Pix originalErodedImage3 = null;
				while (try4TesseractImage1 == null) {
					try {
						TimeUnit.MILLISECONDS.sleep(10L);
					} catch (InterruptedException e) {
						throw new IllegalStateException(e);
					}
				}
				synchronized (try4TesseractImage1Lock) {
					aCopy = Leptonica1.pixErodeGray(try4TesseractImage1, 1, 2);
				}
				// originalErodedImage = Leptonica1.pixErodeGray(originalCopy, 2, 2);
				binarized = Leptonica1.pixMaskedThreshOnBackgroundNorm(aCopy, null, 8, 8,
						(int) (averagePixelValue(aCopy) * 1.10), 50, 2, 2, 0.2f, null);
				originalScaledCopy = Leptonica1.pixScaleGeneral(binarized, widthScaleFactor, heightScaleFactor, 0.0f,
						1);
				// originalErodedImage1 = Leptonica1.pixDilateGray(originalCopy, 1, 3);
				// originalErodedImage2 = Leptonica1.pixDilateGray(originalErodedImage1, 1, 3);
				// originalErodedImage3 = Leptonica1.pixErodeGray(originalErodedImage1, 2, 1);
				// LeptUtils.dispose(originalCopy);

				if (originalScaledCopy == null) {
					LeptUtils.dispose(aCopy);
					LeptUtils.dispose(binarized);
					LeptUtils.dispose(originalScaledCopy);
					// LeptUtils.dispose(originalErodedImage1);
					// LeptUtils.dispose(originalErodedImage2);
					// LeptUtils.dispose(originalErodedImage3);
					OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
					aWrapper.setOcrString(aResult);
					result.add(aWrapper);
					return Boolean.FALSE;
				}
				BufferedImage image = LeptUtils.convertPixToImage(originalScaledCopy);
				LeptUtils.dispose(aCopy);
				LeptUtils.dispose(binarized);
				LeptUtils.dispose(originalScaledCopy);
				// LeptUtils.dispose(originalErodedImage1);
				// LeptUtils.dispose(originalErodedImage2);
				// LeptUtils.dispose(originalErodedImage3);
				if (getInterruptParallelThreads()) {
					OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
					aWrapper.setOcrString(aResult);
					result.add(aWrapper);
					return Boolean.FALSE;
				}
				BufferedImage rotated = rotate(image);
				if (debugL <= 3) {
					SBImageUtils.writeFile(rotated, "png",
							DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + currentFileName + "-imageForTesseract1.png");
				}

				TechWerxTesseractHandle instanceHandle = null;
				try {
					synchronized (originalImagesLock) {
						instanceHandle = (TechWerxTesseractHandle) originalImagesTesseractPool.borrowObject();
					}
				} catch (Exception e) {
					e.printStackTrace();
					try {
						synchronized (originalImagesLock) {
							originalImagesTesseractPool.returnObject(instanceHandle);
						}
					} catch (Exception e1) {
					}
					return Boolean.FALSE;
				}
				TechWerxTesseract instance = instanceHandle.getHandle();

				try {
					if (getInterruptParallelThreads()) {
						OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
						aWrapper.setOcrString(aResult);
						result.add(aWrapper);
						synchronized (originalImagesLock) {
							originalImagesTesseractPool.returnObject(instanceHandle);
						}
						return Boolean.FALSE;
					}
					aResult = instance.doOCR(rotated);
				} catch (Exception e) {
					success = false;
				}
				synchronized (originalImagesLock) {
					originalImagesTesseractPool.returnObject(instanceHandle);
				}
				aResult = aResult.replace(System.lineSeparator(), "  ");
				aResult = aResult.replace("\r\n", "  ");
				if (debugL <= 12) {
					System.out.println(
							"Thread 4 : ocrBNandCNImages: In tesseractBNandCNImages() : tesseractImage1 : " + aResult);
				}
			} catch (Exception e) {
				success = false;
				return Boolean.FALSE;
			}
			if (success) {
				if (debugL <= 9) {
					System.out.println(
							"Thread 4 : ocrBNandCNImages: In tesseractBNandCNImages() : the result from teseractImage1 is - "
									+ aResult);
				}
				OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
				aWrapper.setOcrString(aResult);
				result.add(aWrapper);
			}
			return Boolean.TRUE;
		}, outerThreadService);

		CompletableFuture<Boolean> originalTesseract2 = CompletableFuture.supplyAsync(() -> {
			if (debugL <= 5) {
				System.out.println(
						"Thread 4 : ocrBNandCNImages: In tesseractBNandCNImages() : Starting processing of tesseractImage2");
			}

			boolean success = true;
			String aResult = "";

			try {
				if (getInterruptParallelThreads()) {
					OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
					aWrapper.setOcrString(aResult);
					result.add(aWrapper);
					return Boolean.FALSE;
				}
				Pix aCopy = null;
				Pix binarized = null;
				while (try4TesseractImage2 == null) {
					try {
						TimeUnit.MILLISECONDS.sleep(10L);
					} catch (InterruptedException e) {
						throw new IllegalStateException(e);
					}
				}
				synchronized (try4TesseractImage2Lock) {
					aCopy = Leptonica1.pixErodeGray(try4TesseractImage2, 1, 1);
				}
				binarized = Leptonica1.pixMaskedThreshOnBackgroundNorm(aCopy, null, 8, 8,
						(int) (averagePixelValue(aCopy) * 1.15), 50, 2, 2, 0.2f, null);
				Pix originalScaledCopy = Leptonica1.pixScaleGeneral(binarized, widthScaleFactor, heightScaleFactor,
						0.0f, 1);
				// Pix originalErodedImage1 = null;
				// synchronized (interruptLock) {
				// originalCopy = Leptonica1.pixCopy(null, originalPix8);
				// }
				// originalErodedImage = Leptonica1.pixErodeGray(originalCopy, 2, 2);
				// originalCopy = Leptonica1.pixScaleGeneral(aCopy, widthScaleFactor,
				// heightScaleFactor, 0.0f, 1);
				// originalErodedImage1 = Leptonica1.pixErodeGray(originalCopy, 2, 1);

				// LeptUtils.dispose(originalCopy);
				if (originalScaledCopy == null) {
					LeptUtils.dispose(aCopy);
					LeptUtils.dispose(binarized);
					LeptUtils.dispose(originalScaledCopy);
					// LeptUtils.dispose(originalErodedImage1);
					OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
					aWrapper.setOcrString(aResult);
					result.add(aWrapper);
					return Boolean.FALSE;
				}
				BufferedImage image = LeptUtils.convertPixToImage(originalScaledCopy);
				LeptUtils.dispose(aCopy);
				LeptUtils.dispose(binarized);
				LeptUtils.dispose(originalScaledCopy);
				// LeptUtils.dispose(originalErodedImage1);
				if (getInterruptParallelThreads()) {
					OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
					aWrapper.setOcrString(aResult);
					result.add(aWrapper);
					return Boolean.FALSE;
				}
				BufferedImage rotated = rotate(image);
				if (debugL <= 3) {
					SBImageUtils.writeFile(rotated, "png",
							DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + currentFileName + "-imageForTesseract2.png");
				}

				TechWerxTesseractHandle instanceHandle = null;
				try {
					synchronized (originalImagesLock) {
						instanceHandle = (TechWerxTesseractHandle) originalImagesTesseractPool.borrowObject();
					}
				} catch (Exception e) {
					e.printStackTrace();
					try {
						synchronized (originalImagesLock) {
							originalImagesTesseractPool.returnObject(instanceHandle);
						}
					} catch (Exception e1) {
					}
					return Boolean.FALSE;
				}
				TechWerxTesseract instance = instanceHandle.getHandle();

				try {
					if (getInterruptParallelThreads()) {
						OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
						aWrapper.setOcrString(aResult);
						result.add(aWrapper);
						synchronized (originalImagesLock) {
							originalImagesTesseractPool.returnObject(instanceHandle);
						}
						return Boolean.FALSE;
					}
					aResult = instance.doOCR(rotated);
				} catch (Exception e) {
					success = false;
				}
				synchronized (originalImagesLock) {
					originalImagesTesseractPool.returnObject(instanceHandle);
				}
				aResult = aResult.replace(System.lineSeparator(), "  ");
				aResult = aResult.replace("\r\n", "  ");
				if (debugL <= 12) {
					System.out.println(
							"Thread 4 : ocrBNandCNImages: In tesseractBNandCNImages() : tesseractImage2 : " + aResult);
				}
			} catch (Exception e) {
				success = false;
				return Boolean.FALSE;
			}
			if (success) {
				if (debugL <= 9) {
					System.out.println(
							"Thread 4 : ocrBNandCNImages: In tesseractBNandCNImages() : the result from tesseractImage2 is - "
									+ aResult);
				}
				OCRStringWrapper aWrapper = new OCRStringWrapper(0, null, new ArrayList<Rectangle>());
				aWrapper.setOcrString(aResult);
				result.add(aWrapper);
			}
			return Boolean.TRUE;
		}, outerThreadService);

		bnTesseract.join();
		cnTesseract.join();
		umTesseract.join();
		originalTesseract1.join();
		originalTesseract2.join();

		return result;
	}

	private static void debugRenderBoundingBoxRound(
			Pix source,
			ArrayList<ArrayList<Rectangle>> lines,
			int round,
			int threadNumber,
			int debugL,
			L_Bmf font) {

		int threshold = (round == 9) ? 11 : 10;
		if (debugL > threshold) {
			return;
		}
		Pix roundPix = Leptonica1.pixConvertTo32(source);
		int ln = 0;
		for (ArrayList<Rectangle> line : lines) {
			++ln;
			for (Rectangle letter : line) {
				Box box = new Box(letter.x, letter.y, letter.width, letter.height, 0);
				Leptonica1.pixRenderBoxArb(roundPix, box, 1, (byte) 255, (byte) 0, (byte) 0);
				Leptonica1.pixSetTextline(roundPix, font, "" + ln, 0xFF000000,
						letter.x, letter.y - 1, null, null);
			}
		}
		PixDebugWriter.writeIfDebug(debugL, (round == 9 ? 11 : 10), roundPix,
				DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + currentFileName
						+ "-Round" + round + "BB-" + threadNumber + ".png");
		LeptUtils.dispose(roundPix);
	}

	public static ArrayList<ArrayList<Rectangle>> getBoundingBoxes(Pix pixForBoundingBoxes, int threadNumber,
			int debugL) {

		L_Bmf font = Leptonica1.bmfCreate(null, 4);

		Instant t10 = Instant.now();

		// ROUND 1
		ArrayList<ArrayList<Rectangle>> defaultBoxes = getBoundingBoxes(
				pixForBoundingBoxes, processDataWrapper, threadNumber, debugLevel);
		debugRenderBoundingBoxRound(pixForBoundingBoxes, defaultBoxes, 1, threadNumber, debugL, font);

		// ROUND 2
		ArrayList<ArrayList<Rectangle>> reallocatedLines = reallocateLines(
				defaultBoxes, processDataWrapper, threadNumber, -1, debugLevel);
		debugRenderBoundingBoxRound(pixForBoundingBoxes, reallocatedLines, 2, threadNumber, debugL, font);

		// ROUND 3
		reallocatedLines = reallocateLines(reallocatedLines, processDataWrapper, threadNumber, -1, debugLevel);
		debugRenderBoundingBoxRound(pixForBoundingBoxes, reallocatedLines, 3, threadNumber, debugL, font);

		// ROUND 4
		reallocatedLines = removeLargeBoxesAndRedistribute(reallocatedLines, threadNumber);
		debugRenderBoundingBoxRound(pixForBoundingBoxes, reallocatedLines, 4, threadNumber, debugL, font);

		// ROUND 5
		reallocatedLines = reallocateLines(reallocatedLines, processDataWrapper, threadNumber, -1, debugLevel);
		debugRenderBoundingBoxRound(pixForBoundingBoxes, reallocatedLines, 5, threadNumber, debugL, font);

		// ROUND 6
		reallocatedLines = reallocateLinesAgain(reallocatedLines, processDataWrapper, threadNumber, debugLevel);
		debugRenderBoundingBoxRound(pixForBoundingBoxes, reallocatedLines, 6, threadNumber, debugL, font);

		// ROUND 7
		reallocatedLines = splitLinesByYCoordinate(reallocatedLines, processDataWrapper, threadNumber, debugLevel);
		debugRenderBoundingBoxRound(pixForBoundingBoxes, reallocatedLines, 7, threadNumber, debugL, font);

		// ROUND 8
		reallocatedLines = reallocateVerticalBoxes(reallocatedLines, processDataWrapper, threadNumber, debugLevel);
		debugRenderBoundingBoxRound(pixForBoundingBoxes, reallocatedLines, 8, threadNumber, debugL, font);

		// ROUND 9
		reallocatedLines = removeEdgeKissingBoundingBoxes(reallocatedLines, pixForBoundingBoxes, threadNumber, 5);
		debugRenderBoundingBoxRound(pixForBoundingBoxes, reallocatedLines, 9, threadNumber, debugL, font);

		long boxManipulationTime = Duration.between(t10, Instant.now()).toMillis();
		if (debugL <= 2) {
			System.out.println("Box manipulation time = " + boxManipulationTime);
		}

		return reallocatedLines;

	}

	public static ArrayList<OCRBufferedImageWrapper> getOCRBufferedImageWrapperArrayFast(final Pix pixIn,
			ArrayList<ArrayList<Rectangle>> input, ProcessDataWrapper processDataWrapper,
			ExecutorService outerThreadService, ExecutorService innerThreadService, int iterationNumber, int debugL) {
		// NOTE: input Pix is a 1bpp image

		final ArrayList<OCRBufferedImageWrapper> biWrapperArray = new ArrayList<>();
		Pix pixTemp = null;
		synchronized (interruptLock) {
			pixTemp = Leptonica1.pixCopy(null, pixIn);
		}
					PixDebugWriter.writeIfDebug(debugL, 3, pixTemp, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "cleanedPixInsideGetWrapperArray-"
					+ iterationNumber + ".png");

		if (getInterruptParallelThreads() || (pixTemp == null)) {
			LeptUtils.dispose(pixTemp);
			if (debugL <= 9) {
				System.out.println("Thread " + iterationNumber
						+ " - Exiting getOCRBufferedImageWrapperArrayFast() : Exit point 1");
			}
			return biWrapperArray;
		}

		final Pix pix = Leptonica1.pixCopy(null, pixTemp);
		LeptUtils.dispose(pixTemp);
		// get the coordinates of the large clipping box for each line
		// by merging the rectangles in each line
		ArrayList<Rectangle> lines = new ArrayList<>();
		for (ArrayList<Rectangle> line : input) {
			int leftTopX = Integer.MAX_VALUE;
			int leftTopY = Integer.MAX_VALUE;
			int rightBotX = Integer.MIN_VALUE;
			int rightBotY = Integer.MIN_VALUE;
			for (Rectangle letter : line) {
				leftTopX = Math.min(leftTopX, letter.x);
				leftTopY = Math.min(leftTopY, letter.y);
				rightBotX = Math.max(rightBotX, letter.x + letter.width);
				rightBotY = Math.max(rightBotY, letter.y + letter.height);
			}
			lines.add(new Rectangle(leftTopX, leftTopY, rightBotX - leftTopX, rightBotY - leftTopY));
		}
		final ArrayList<Integer> likelyHeight = new ArrayList<>();
		final ArrayList<Integer> referenceWidth = new ArrayList<>();
		int index = 0;

		for (ArrayList<Rectangle> line : input) {
			DescriptiveStatistics hStats = new DescriptiveStatistics();
			DescriptiveStatistics wStats = new DescriptiveStatistics();
			for (Rectangle letter : line) {
				hStats.addValue(letter.height);
				wStats.addValue(letter.width);
			}

			// double referenceHeight = processDataWrapper.kdeData.mostLikelyHeight;
			double heightStatsMedian = hStats.getPercentile(50);
			double referenceHeight = heightStatsMedian;
			double heightStatsSD = hStats.getStandardDeviation();
			double widthStatsMedian = wStats.getPercentile(50);
			double widthStatsSD = wStats.getStandardDeviation();

			likelyHeight.add((int) heightStatsMedian);

			if (hStats.getN() >= 3) {
				// If the heightStats has a low variance, then it's likely that these are
				// individual characters with a reasonably uniform height
				// Hence, this height should be used instead of the mostLikelyHeight
				// if (((heightStatsSD / heightStatsMedian) < 0.45)
				// && ((heightStatsMedian / processDataWrapper.kdeData.mostLikelyHeight) < 1.5))
				// {
				// referenceHeight = heightStatsMedian;
				// }
				if (((widthStatsSD / widthStatsMedian) < 0.5) && ((widthStatsMedian / referenceHeight) < 1.5)) {
					referenceWidth.add((int) widthStatsMedian);
				} else {
					referenceWidth.add((int) (referenceHeight * ((idealCharWidth * 1.0) / idealCharHeight)));
				}
			} else {
				referenceWidth.add((int) (referenceHeight * ((idealCharWidth * 1.0) / idealCharHeight)));
			}

			if (debugL <= 3) {
				System.out.println("Thread " + iterationNumber + " - getOCRBufferedImageWrapperArrayFast() : Line - "
						+ index++ + "; heightMedian = " + heightStatsMedian + "; heightStatsSD / heightStatsMedian = "
						+ (heightStatsSD / heightStatsMedian)
						+ "; heightStatsMedian / processDataWrapper.kdeData.mostLikelyHeight = "
						+ (heightStatsMedian / processDataWrapper.kdeData.mostLikelyHeight)
						+ "; widthStatsSD / widthStatsMean = " + (widthStatsSD / wStats.getMean()) + "; widthMedian = "
						+ widthStatsMedian);
			}

		}

		if (getInterruptParallelThreads()) {
			LeptUtils.dispose(pix);
			if (debugL <= 9) {
				System.out.println("Thread " + iterationNumber
						+ " - Exiting getOCRBufferedImageWrapperArrayFast() : Exit point 2");
			}
			return biWrapperArray;
		}
		final int pixHeight = Leptonica1.pixGetHeight(pix);
		final int pixWidth = Leptonica1.pixGetWidth(pix);
		final int verticalGap = 20;
		final int border = 20;
		int count = 0;
		final Object pixArrayMonitor = new Object();
		if (getInterruptParallelThreads()) {
			LeptUtils.dispose(pix);
			if (debugL <= 9) {
				System.out.println("Thread " + iterationNumber
						+ " - Exiting getOCRBufferedImageWrapperArrayFast() : Exit point 3");
			}
			return biWrapperArray;
		}

		final ArrayList<CompletableFuture<Boolean>> outerCFS = new ArrayList<CompletableFuture<Boolean>>();
		for (final Rectangle line : lines) {
			final int lineCount = count++;
			outerCFS.add(CompletableFuture.supplyAsync(() -> {
				Pix pixCopy = Leptonica1.pixCopy(null, pix);
				if (debugL <= 9) {
					System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
							+ " - getOCRBufferedImageWrapperArrayFast() : Started outer thread for line "
							+ (lineCount + 1));
				}
				try {
					int topBottomBorderAdjustment = 2;
					int leftRightAdjustmentPixels = Math.max(5, (int) ((referenceWidth.get(lineCount) * 1.0) / 3));
					Box box = Leptonica1.boxCreate(Math.max(0, line.x - leftRightAdjustmentPixels),
							Math.max(0, line.y - topBottomBorderAdjustment),
							Math.min(line.width + (2 * leftRightAdjustmentPixels),
									pixWidth - 1 - (line.x - leftRightAdjustmentPixels)),
							Math.min(line.height + (2 * topBottomBorderAdjustment),
									pixHeight - 1 - (line.y - topBottomBorderAdjustment)));

					if (getInterruptParallelThreads()) {
						// LeptUtils.dispose(pix);
						LeptUtils.dispose(pixCopy);
						LeptUtils.dispose(box);
						if (debugL <= 9) {
							System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
									+ " - Exiting getOCRBufferedImageWrapperArrayFast() : Exit point 4");
						}
						return Boolean.FALSE;
					}
					Pix pixOriginalX = Leptonica1.pixClipRectangle(pixCopy, box, null);

					if (debugL <= 3) {
      PixDebugWriter.writeIfDebug(debugL, 3, pixOriginalX, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "pixOriginalClipped-"+ iterationNumber + "-" + (lineCount + 1) + ".png");
					}

					// convert from 1 bpp to 8 bpp, as erodeGray / dilateGray type operations are
					// used on the pixes
					if ((pixOriginalX == null) || getInterruptParallelThreads()) {
						// LeptUtils.dispose(pix);
						LeptUtils.dispose(pixCopy);
						LeptUtils.dispose(box);
						LeptUtils.dispose(pixOriginalX);
						if (debugL <= 9) {
							System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
									+ " - Exiting getOCRBufferedImageWrapperArrayFast() : Exit point 5");
						}
						return Boolean.FALSE;
					}
					Pix pixOriginalX8 = Leptonica1.pixConvertTo8(pixOriginalX, 0);
					int smallBorder = 4;
					if (getInterruptParallelThreads()) {
						// LeptUtils.dispose(pix);
						LeptUtils.dispose(pixCopy);
						LeptUtils.dispose(box);
						LeptUtils.dispose(pixOriginalX);
						LeptUtils.dispose(pixOriginalX8);
						if (debugL <= 9) {
							System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
									+ " - Exiting getOCRBufferedImageWrapperArrayFast() : Exit point 6");
						}
						return Boolean.FALSE;
					}
					Pix pixInterim = Leptonica1.pixCreate(Leptonica1.pixGetWidth(pixOriginalX8) + (2 * smallBorder),
							Leptonica1.pixGetHeight(pixOriginalX8) + (2 * smallBorder), 8);
					Leptonica1.pixSetBlackOrWhite(pixInterim, ILeptonica.L_SET_WHITE);
					Leptonica1.pixRasterop(pixInterim, smallBorder, smallBorder,
							Leptonica1.pixGetWidth(pixOriginalX8) + 2, Leptonica1.pixGetHeight(pixOriginalX8),
							ILeptonica.PIX_SRC, pixOriginalX8, 0, 0);
											PixDebugWriter.writeIfDebug(debugL, 6, pixInterim, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "pixOriginal8WithBorder-"
								+ iterationNumber + "-" + lineCount + ".png");

					if (debugL <= 9) {
						// System.out.println("****************************************************");
						System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
								+ " - getOCRBufferedImageWrapperArrayFast() : About to get bounding boxes for line - "
								+ (lineCount + 1));
					}
					// Before getting bounding boxes, dilate the pix by (2,1) if tryNumber = 3.
					// This is done because the erosion done earlier makes the bounding boxes stick
					// to each other

					ArrayList<Rectangle> boxes = null;

					if (getInterruptParallelThreads()) {
						// LeptUtils.dispose(pix);
						LeptUtils.dispose(pixCopy);
						LeptUtils.dispose(box);
						LeptUtils.dispose(pixOriginalX);
						LeptUtils.dispose(pixOriginalX8);
						LeptUtils.dispose(pixInterim);
						if (debugL <= 9) {
							System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
									+ " - Exiting getOCRBufferedImageWrapperArrayFast() : Exit point 7");
						}
						return Boolean.FALSE;
					}
					if (iterationNumber < 3) {
						boxes = getBoxesInFinalPix(pixInterim, likelyHeight.get(lineCount),
								referenceWidth.get(lineCount), true, iterationNumber, (lineCount + 1), debugL);
					} else {
						Pix boundingBoxesPix = Leptonica1.pixDilateGray(pixInterim, 2, 1);
						boxes = getBoxesInFinalPix(boundingBoxesPix, likelyHeight.get(lineCount),
								referenceWidth.get(lineCount), true, iterationNumber, (lineCount + 1), debugL);
						LeptUtils.dispose(boundingBoxesPix);
					}
					if (debugL <= 9) {
						System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
								+ " - getOCRBufferedImageWrapperArrayFast() : Bounding boxes for line - "
								+ (lineCount + 1) + " are " + boxes);
					}

					// if there no bounding boxes, then return without adding anything to the
					// WrapperArray
					if (boxes.size() == 0) {
						if (debugL <= 9) {
							System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
									+ " - Exiting getOCRBufferedImageWrapperArrayFast() : Exit point 7A - no bounding boxes for this line");
						}
						return Boolean.FALSE;
					}

					// ADDING CODE FROM HERE
					ArrayList<ArrayList<Rectangle>> boxRects = new ArrayList<>();
					boxRects.add(new ArrayList<>());
					for (Rectangle aBox : boxes) {
						boxRects.get(0).add(aBox);
					}

					if (debugL <= 9) {
						System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
								+ " - getOCRBufferedImageWrapperArrayFast() : About to call segregateFinalBoxesIntoLines()");
					}
					boxRects = segregateFinalBoxesIntoLines(boxRects, iterationNumber, (lineCount + 1), debugL);
					if (debugL <= 9) {
						System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
								+ " - getOCRBufferedImageWrapperArrayFast() : Bounding boxes for line - "
								+ (lineCount + 1) + " after segregating them into lines are " + boxRects);
					}

					// if there no bounding boxes, then return without adding anything to the
					// WrapperArray
					int nBoxes = 0;
					for (ArrayList<Rectangle> aLine : boxRects) {
						nBoxes += aLine.size();
					}
					if (nBoxes == 0) {
						return Boolean.FALSE;
					}

					boxRects = reallocateLines(boxRects, processDataWrapper, iterationNumber, (lineCount + 1), debugL);
					if (debugL <= 9) {
						System.out.println("Thread " + iterationNumber
								+ " - getOCRBufferedImageWrapperArrayFast() : Bounding boxes for line - "
								+ (lineCount + 1) + " after reallocating them into lines are " + boxRects);
					}

					boxRects = mergeVerticalBoxes(boxRects, processDataWrapper, iterationNumber, (lineCount + 1),
							debugL);

					// if there are no bounding boxes, then return without adding anything to the
					// WrapperArray
					nBoxes = 0;
					for (ArrayList<Rectangle> aLine : boxRects) {
						nBoxes += aLine.size();
					}
					if (nBoxes == 0) {
						return Boolean.FALSE;
					}

					// DETERMINE IF THE PIX NEEDS TO BE ROTATED
					// CHECK THE BASE OF THE FIRST AND LAST BOX IN EACH LINE TO DETERMINE THE ANGLE
					// OF ROTATION
					// TAKE THE AVERAGE AND ROTATE THE PIX

					ArrayList<Double> angles = new ArrayList<>();
					ArrayList<Integer> angleWeights = new ArrayList<>();
					for (ArrayList<Rectangle> aLine : boxRects) {
						Rectangle leftMostWord = null;
						Rectangle rightMostWord = null;
						innerloop: for (Rectangle aWord : aLine) {
							// IF THE WORD IS SLIGHTLY SMALLER THAN THE LIKELY HEIGHT, THEN IGNORE IT.
							// ENSURES THAT SMALL BOXES PICKED UP IN A SKEWED IMAGE ARE IGNORED FOR THE
							// PURPOSE OF DETERMINING ROTATION ANGLE
							if (aWord.height < (0.8 * likelyHeight.get(lineCount))) {
								continue innerloop;
							}
							if (leftMostWord == null) {
								leftMostWord = aWord;
							} else {
								if (aWord.x < leftMostWord.x) {
									leftMostWord = aWord;
								}
							}
							if (rightMostWord == null) {
								rightMostWord = aWord;
							} else {
								if (aWord.x > rightMostWord.x) {
									rightMostWord = aWord;
								}
							}
						}
						double angle = 0.0;
						if ((aLine.size() > 1) && (leftMostWord != null) && (rightMostWord != null)) {
							angle = Math.atan(
									(((leftMostWord.y + leftMostWord.height) - rightMostWord.y - rightMostWord.height)
											* 1.0) / ((rightMostWord.x + rightMostWord.width) - leftMostWord.x));
						}
						angles.add(angle);
						if ((aLine.size() > 1) && (leftMostWord != null) && (rightMostWord != null)) {
							angleWeights.add((rightMostWord.x + rightMostWord.width) - leftMostWord.x);
						} else {
							angleWeights.add(1);
						}
					}

					double rotationAngle = 0.0;
					int sumOfWeights = 0;
					if (angles.size() > 0) {
						for (int k = 0; k < angles.size(); ++k) {
							rotationAngle += angles.get(k) * angleWeights.get(k);
							sumOfWeights += angleWeights.get(k);
						}
						rotationAngle = rotationAngle / sumOfWeights;
					}

					int xLeft = Integer.MAX_VALUE;
					int yTop = Integer.MAX_VALUE;
					int xRight = Integer.MIN_VALUE;
					int yBot = Integer.MIN_VALUE;
					for (Rectangle aBox : boxes) {
						xLeft = Math.min(xLeft, aBox.x);
						yTop = Math.min(yTop, aBox.y);
						xRight = Math.max(xRight, aBox.x + aBox.width);
						yBot = Math.max(yBot, aBox.y + aBox.height);
					}

					OCRBufferedImageWrapper biWrapper = null;
					ArrayList<Rectangle> bBoxes = new ArrayList<>();
					if (boxRects.size() != 0) {
						if (bBoxes.size() == 1) {
							bBoxes = boxRects.get(0);
						} else {
							for (ArrayList<Rectangle> aLine : boxRects) {
								if (aLine.size() > bBoxes.size()) {
									bBoxes = aLine;
								}
							}
						}
						biWrapper = new OCRBufferedImageWrapper(likelyHeight.get(lineCount), null, bBoxes);
					} else {
						biWrapper = new OCRBufferedImageWrapper(likelyHeight.get(lineCount), null, bBoxes);
					}

					Box newBox = Leptonica1.boxCreate(xLeft, yTop, (xRight - xLeft) + 1, (yBot - yTop) + 1);
					Pix pixClippedUnrotated = Leptonica1.pixClipRectangle(pixInterim, newBox, null);
					Pix pixClipped = Leptonica1.pixRotate(pixClippedUnrotated, (float) rotationAngle,
							ILeptonica.L_ROTATE_AREA_MAP, ILeptonica.L_BRING_IN_WHITE, 0, 0);
					Pix pixOriginal = Leptonica1.pixCreate(Leptonica1.pixGetWidth(pixClipped) + (2 * smallBorder),
							Leptonica1.pixGetHeight(pixClipped) + (2 * smallBorder), 8);
					Leptonica1.pixSetBlackOrWhite(pixOriginal, ILeptonica.L_SET_WHITE);
					Leptonica1.pixRasterop(pixOriginal, smallBorder, smallBorder,
							Leptonica1.pixGetWidth(pixClipped) + 2, Leptonica1.pixGetHeight(pixClipped) + 1,
							ILeptonica.PIX_SRC, pixClipped, 0, 0);

					if (getInterruptParallelThreads()) {
						// LeptUtils.dispose(pix);
						LeptUtils.dispose(pixCopy);
						LeptUtils.dispose(box);
						LeptUtils.dispose(pixOriginalX);
						LeptUtils.dispose(pixOriginalX8);
						LeptUtils.dispose(pixInterim);
						LeptUtils.dispose(newBox);
						LeptUtils.dispose(pixClippedUnrotated);
						LeptUtils.dispose(pixClipped);
						LeptUtils.dispose(pixOriginal);
						if (debugL <= 9) {
							System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
									+ " - Exiting getOCRBufferedImageWrapperArrayFast() : Exit point 8");
						}
						return Boolean.FALSE;
					}

					if (boxes.size() == 0) {
						boxes = input.get(lineCount);
					}
					if (debugL <= 2) {
						drawBoundingBoxesOnPix(pixInterim, boxRects, 100 + lineCount, debugL, "", iterationNumber);
					}

					LeptUtils.dispose(pixCopy);
					LeptUtils.dispose(pixInterim);
					LeptUtils.dispose(newBox);
					LeptUtils.dispose(pixClippedUnrotated);
					LeptUtils.dispose(pixClipped);

					DescriptiveStatistics wStats = new DescriptiveStatistics();
					// DescriptiveStatistics hStats = new DescriptiveStatistics();
					ArrayList<Integer> interimStats = new ArrayList<>();

					for (Rectangle aBox : boxes) {
						interimStats.add(aBox.width);
						allBoxes.add(aBox);
					}
					Collections.sort(interimStats);
					if (interimStats.size() > 0) {
						interimStats.remove(0);
					}
					if (interimStats.size() > 0) {
						interimStats.remove(interimStats.size() - 1);
					}

					float likelyWidth = referenceWidth.get(lineCount);

					if (interimStats.size() > 1) {
						for (Integer aBoxWidth : interimStats) {
							wStats.addValue(aBoxWidth);
						}

						if (debugL <= 9) {
							System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
									+ " - getOCRBufferedImageWrapperArrayFast() : For line " + (lineCount + 1)
									+ "; likelyWidth (referenceWidth.get(" + lineCount + ") = " + likelyWidth
									+ "; Sigma (width) = " + String.format("%.3f", wStats.getStandardDeviation())
									+ "; Mean (width) = " + String.format("%.3f", wStats.getMean())
									+ "; Median (width) = " + String.format("%.3f", wStats.getPercentile(50)));
						}
						if ((wStats.getN() >= 5) && ((wStats.getStandardDeviation() / wStats.getMean()) < 0.8)) {
							likelyWidth = (float) wStats.getPercentile(50);
						} else {
							if ((wStats.getN() >= 2) && ((wStats.getStandardDeviation() / wStats.getMean()) < 0.55)) {
								likelyWidth = (float) wStats.getPercentile(50);
							}
						}
					}

					if (debugL <= 3) {
      PixDebugWriter.writeIfDebug(debugL, 3, pixOriginal, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "originalPix-" + iterationNumber+ "-" + (lineCount + 1) + ".png");
					}

					if (debugL <= 9) {
						System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
								+ " - getOCRBufferedImageWrapperArrayFast() : Main 2 stats For line " + (lineCount + 1)
								+ " : likelyHeight = " + likelyHeight.get(lineCount) + "; likelyWidth = "
								+ likelyWidth);
					}

					float heightScaleFactor = (idealCharHeight * 1.0f) / likelyHeight.get(lineCount);
					float widthScaleFactor = (idealCharWidth * 1.0f) / likelyWidth;
					
//					if (CheckProductProperties.productIsGiven) {
//						if (!foundHeightAndWidthScaling) {
//							heightScalingForPoorImages += heightScaleFactor;
//							widthScalingForPoorImages += widthScaleFactor;
//							++numberOfLines;
//							// if price line found, then set height scaling and width scaling of full image
//							// to this particular line's height and width scale factor
//							if ((wStats.getN() >= 5) && (wStats.getN() <= 8)
//									&& ((wStats.getStandardDeviation() / wStats.getMean()) < 0.8)) {
//								heightScalingForPoorImages = heightScaleFactor;
//								widthScalingForPoorImages = widthScaleFactor;
//								foundHeightAndWidthScaling = true;
//							}
//						}
//					}

					Pix pixSource1 = Leptonica1.pixScaleGeneral(pixOriginal, widthScaleFactor, heightScaleFactor, 0.0f,
							1);

					if (debugL <= 9) {
						System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
								+ " - getOCRBufferedImageWrapperArrayFast() : Created pixSource1");
					}

					if (debugL <= 3) {
      PixDebugWriter.writeIfDebug(debugL, 3, pixSource1, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "scaledPix-" + iterationNumber+ "-" + (lineCount + 1) + ".png");
					}

					final int originalWidth = Leptonica1.pixGetWidth(pixOriginal);
					final int originalHeight = Leptonica1.pixGetHeight(pixOriginal);
					final int width = Leptonica1.pixGetWidth(pixSource1);
					final int height = Leptonica1.pixGetHeight(pixSource1);
					final int newHeight1 = (int) (2.25 * originalHeight) + (int) (height * 2.25) + (3 * verticalGap)
							+ (2 * border) + idealCharHeight + verticalGap;
					final int newWidth1 = Math.max(Math.max((int) (width * 2.0), originalWidth),
							(int) ((originalWidth * widthScaleFactor * 1.5) / heightScaleFactor)) + (3 * border);

					// Pix targetPix32 = null;
					Pix targetPix8 = null;
					Pix pixScaled1 = null;
					Pix pixScaled2A = null;
					Pix pixScaled2B = null;
					Pix pixScaled2 = null;
					Pix pixScaled3A = null;
					Pix pixScaled3 = null;
					Pix pixScaled4A = null;
					Pix pixScaled4 = null;
					Pix pixScaled5 = null;
					Pix pixScaled6 = null;
					Pix pixScaled7 = null;
					Pix pixScaled8 = null;
					Pix pixScaled9 = null;

					if (getInterruptParallelThreads()) {
						// LeptUtils.dispose(pix);
						LeptUtils.dispose(box);
						LeptUtils.dispose(pixOriginalX);
						LeptUtils.dispose(pixOriginalX8);
						LeptUtils.dispose(pixOriginal);
						LeptUtils.dispose(pixSource1);
						if (debugL <= 9) {
							System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
									+ " - Exiting getOCRBufferedImageWrapperArrayFast() : Exit point 9");
						}
						return Boolean.FALSE;
					}

					// targetPix32 = Leptonica1.pixCreate(newWidth1, newHeight1, 32);
					targetPix8 = Leptonica1.pixCreate(newWidth1, newHeight1, 8);
					// Leptonica1.pixSetBlackOrWhite(targetPix32, ILeptonica.L_SET_WHITE);
					Leptonica1.pixSetBlackOrWhite(targetPix8, ILeptonica.L_SET_WHITE);

					if (debugL <= 9) {
						System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
								+ " - getOCRBufferedImageWrapperArrayFast() : Created targetPix8");
					}

					if (getInterruptParallelThreads()) {
						// LeptUtils.dispose(pix);
						LeptUtils.dispose(box);
						LeptUtils.dispose(pixOriginalX);
						LeptUtils.dispose(pixOriginalX8);
						LeptUtils.dispose(pixOriginal);
						LeptUtils.dispose(pixSource1);
						LeptUtils.dispose(targetPix8);
						if (debugL <= 9) {
							System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
									+ " - Exiting getOCRBufferedImageWrapperArrayFast() : Exit point 10");
						}
						return Boolean.FALSE;
					}

					// int fontSize = Math.max(((idealCharHeight / 3) / 2) * 2, 4);
					// L_Bmf font = Leptonica1.bmfCreate("", 6);
					// Leptonica1.pixSetTextline(targetPix8, font, "2G 5J 9X", 0, border,
					// border + idealCharHeight, null, null);
					// if (debugL <= 3) {
					// Leptonica1.pixWrite(// DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "seedPix32 -" + (lineCount + 1) +
					// ".png",
					// targetPix32, ILeptonica.IFF_PNG);
					// }

					// targetPix8 = Leptonica1.pixConvertTo8(targetPix32, 0);

					// Leptonica1.pixRasterop(targetPix8, border, border, seedPix.w + 2, seedPix.h +
					// 2, ILeptonica.PIX_SRC,
					// seedPix, 0, 0);

					Leptonica1.pixRasterop(targetPix8, 2 * border, border, width + 2, height + 2, ILeptonica.PIX_SRC,
							pixSource1, 0, 0);

					pixScaled1 = Leptonica1.pixHShear(null, pixSource1, 0, (float) Math.toRadians(5),
							ILeptonica.L_BRING_IN_WHITE);
					pixScaled2A = Leptonica1.pixScaleGeneral(pixScaled1, 2.0f, 2.0f, 0.7f, 1);
					pixScaled2B = Leptonica1.pixErodeGray(pixScaled2A, 3, 3);
					pixScaled2 = Leptonica1.pixScaleGeneral(pixScaled2B, 0.625f, 0.625f, 0.7f, 1);
					Leptonica1.pixRasterop(targetPix8, 2 * border, border + height + verticalGap,
							(int) ((width * 1.25f) + 2), (int) (height * 1.25f) + 2, ILeptonica.PIX_SRC, pixScaled2, 0,
							0);

					if (debugL <= 9) {
						System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
								+ " - getOCRBufferedImageWrapperArrayFast() : Created pixScaled2");
					}

					if (getInterruptParallelThreads()) {
						// LeptUtils.dispose(pix);
						LeptUtils.dispose(box);
						LeptUtils.dispose(pixOriginalX);
						LeptUtils.dispose(pixOriginalX8);
						LeptUtils.dispose(pixOriginal);
						LeptUtils.dispose(pixSource1);
						LeptUtils.dispose(targetPix8);
						LeptUtils.dispose(pixScaled1);
						LeptUtils.dispose(pixScaled2A);
						LeptUtils.dispose(pixScaled2B);
						LeptUtils.dispose(pixScaled2);
						if (debugL <= 9) {
							System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
									+ " - Exiting getOCRBufferedImageWrapperArrayFast() : Exit point 11");
						}
						return Boolean.FALSE;
					}

					float multiplicationFactor = 3.0f;
					pixScaled3A = Leptonica1.pixScaleGeneral(pixOriginal, multiplicationFactor * 1.33f,
							multiplicationFactor, 0.7f, 1);
					pixScaled3 = Leptonica1.pixErodeGray(pixScaled3A, 5, 5);
					pixScaled4A = Leptonica1.pixScaleGeneral(pixScaled3, widthScaleFactor / multiplicationFactor,
							heightScaleFactor / multiplicationFactor, 0.7f, 1);
					pixScaled4 = Leptonica1.pixHShear(null, pixScaled4A, 0, (float) Math.toRadians(2.5),
							ILeptonica.L_BRING_IN_WHITE);
					Leptonica1.pixRasterop(targetPix8, 2 * border, border + (int) (2.25 * height) + (2 * verticalGap),
							(int) (width * 1.5) + 2, height + 2, ILeptonica.PIX_SRC, pixScaled4, 0, 0);

					if (debugL <= 9) {
						System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
								+ " - getOCRBufferedImageWrapperArrayFast() : Created pixScaled4");
					}

					if (getInterruptParallelThreads()) {
						// LeptUtils.dispose(pix);
						LeptUtils.dispose(box);
						LeptUtils.dispose(pixOriginalX);
						LeptUtils.dispose(pixOriginalX8);
						LeptUtils.dispose(pixOriginal);
						LeptUtils.dispose(pixSource1);
						LeptUtils.dispose(targetPix8);
						LeptUtils.dispose(pixScaled1);
						LeptUtils.dispose(pixScaled2A);
						LeptUtils.dispose(pixScaled2B);
						LeptUtils.dispose(pixScaled2);
						LeptUtils.dispose(pixScaled3A);
						LeptUtils.dispose(pixScaled3);
						LeptUtils.dispose(pixScaled4A);
						LeptUtils.dispose(pixScaled4);
						if (debugL <= 9) {
							System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
									+ " - Exiting getOCRBufferedImageWrapperArrayFast() : Exit point 12");
						}
						return Boolean.FALSE;
					}

					pixScaled5 = Leptonica1.pixScaleGeneral(pixOriginal, multiplicationFactor * 1.33f,
							multiplicationFactor * 1.33f, 0.7f, 1);
					pixScaled6 = Leptonica1.pixErodeGray(pixScaled5, 6, 6);
					pixScaled7 = Leptonica1.pixScaleGeneral(pixScaled6, widthScaleFactor / multiplicationFactor,
							heightScaleFactor / multiplicationFactor, 0.7f, 1);
					Leptonica1.pixRasterop(targetPix8, 2 * border,
							border + originalHeight + (int) (2.25 * height) + (3 * verticalGap), (width * 2) + 2,
							(int) (height * 1.5) + 2, ILeptonica.PIX_SRC, pixScaled7, 0, 0);

					if (debugL <= 9) {
						System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
								+ " - getOCRBufferedImageWrapperArrayFast() : Created pixScaled7");
					}

					if (getInterruptParallelThreads()) {
						// LeptUtils.dispose(pix);
						LeptUtils.dispose(box);
						LeptUtils.dispose(pixOriginalX);
						LeptUtils.dispose(pixOriginalX8);
						LeptUtils.dispose(pixOriginal);
						LeptUtils.dispose(pixSource1);
						LeptUtils.dispose(targetPix8);
						LeptUtils.dispose(pixScaled1);
						LeptUtils.dispose(pixScaled2A);
						LeptUtils.dispose(pixScaled2B);
						LeptUtils.dispose(pixScaled2);
						LeptUtils.dispose(pixScaled3A);
						LeptUtils.dispose(pixScaled3);
						LeptUtils.dispose(pixScaled4A);
						LeptUtils.dispose(pixScaled4);
						LeptUtils.dispose(pixScaled5);
						LeptUtils.dispose(pixScaled6);
						LeptUtils.dispose(pixScaled7);
						if (debugL <= 9) {
							System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
									+ " - Exiting getOCRBufferedImageWrapperArrayFast() : Exit point 13");
						}
						return Boolean.FALSE;
					}

					pixScaled8 = Leptonica1.pixErodeGray(pixOriginal, 4, 3);
					pixScaled9 = Leptonica1.pixScaleGeneral(pixScaled8, widthScaleFactor * 1.25f,
							heightScaleFactor * 1.25f, 0.7f, 1);
					Leptonica1.pixRasterop(targetPix8, 2 * border,
							border + originalHeight + (int) (3.25 * height) + (4 * verticalGap),
							(int) (originalWidth * 1.25f) + 2, (int) (originalHeight * 1.25f) + 2, ILeptonica.PIX_SRC,
							pixScaled9, 0, 0);

					if (debugL <= 9) {
						System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
								+ " - getOCRBufferedImageWrapperArrayFast() : Created pixScaled9");
					}

					if (getInterruptParallelThreads()) {
						// LeptUtils.dispose(pix);
						LeptUtils.dispose(box);
						LeptUtils.dispose(pixOriginalX);
						LeptUtils.dispose(pixOriginalX8);
						LeptUtils.dispose(pixOriginal);
						LeptUtils.dispose(pixSource1);
						LeptUtils.dispose(targetPix8);
						LeptUtils.dispose(pixScaled1);
						LeptUtils.dispose(pixScaled2A);
						LeptUtils.dispose(pixScaled2B);
						LeptUtils.dispose(pixScaled2);
						LeptUtils.dispose(pixScaled3A);
						LeptUtils.dispose(pixScaled3);
						LeptUtils.dispose(pixScaled4A);
						LeptUtils.dispose(pixScaled4);
						LeptUtils.dispose(pixScaled5);
						LeptUtils.dispose(pixScaled6);
						LeptUtils.dispose(pixScaled7);
						LeptUtils.dispose(pixScaled8);
						LeptUtils.dispose(pixScaled9);
						if (debugL <= 9) {
							System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
									+ " - Exiting getOCRBufferedImageWrapperArrayFast() : Exit point 14");
						}
						return Boolean.FALSE;
					}

					Pix targetPix1 = Leptonica1.pixConvertTo1(targetPix8, 136);
					BufferedImage image = LeptUtils.convertPixToImage(targetPix8);

					synchronized (pixArrayMonitor) {
						biWrapper.setImage(image);
						biWrapperArray.add(biWrapper);
					}

					if (debugL <= 9) {
						System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
								+ " - getOCRBufferedImageWrapperArrayFast() : Created targetPix1 and BI image for Tesseract");
					}

					if (debugL <= 6) {
      PixDebugWriter.writeIfDebug(debugL, 6, targetPix8, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "targetPix-" + iterationNumber+ "-" + (lineCount + 1) + ".png");
					}

					// LeptUtils.dispose(pix);
					LeptUtils.dispose(pixOriginalX);
					LeptUtils.dispose(pixOriginalX8);
					LeptUtils.dispose(pixOriginal);
					LeptUtils.dispose(pixSource1);
					LeptUtils.dispose(pixScaled1);
					LeptUtils.dispose(pixScaled2A);
					LeptUtils.dispose(pixScaled2B);
					LeptUtils.dispose(pixScaled2);
					LeptUtils.dispose(pixScaled3A);
					LeptUtils.dispose(pixScaled3);
					LeptUtils.dispose(pixScaled4A);
					LeptUtils.dispose(pixScaled4);
					LeptUtils.dispose(pixScaled5);
					LeptUtils.dispose(pixScaled6);
					LeptUtils.dispose(pixScaled7);
					LeptUtils.dispose(pixScaled8);
					LeptUtils.dispose(pixScaled9);
					// LeptUtils.dispose(targetPix32);
					LeptUtils.dispose(targetPix1);
					LeptUtils.dispose(targetPix8);
					LeptUtils.dispose(box);
					// LeptUtils.dispose(font);

					if (debugL <= 9) {
						System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
								+ " - getOCRBufferedImageWrapperArrayFast() : Finishing outer thread for iterationNumber and line "
								+ iterationNumber + "-" + (lineCount + 1));
						writeFile(image, "png", DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "finalBI-" + iterationNumber
								+ "-" + (lineCount + 1) + ".png");
					}
					if (debugL <= 9) {
						System.out.println("Thread " + iterationNumber + " Image " + (lineCount + 1)
								+ " - Exiting getOCRBufferedImageWrapperArrayFast() with Boolean.TRUE");
					}
					return Boolean.TRUE;
				} catch (Exception e) {
					e.printStackTrace();
					return Boolean.FALSE;
				}
			}, outerThreadService));
		}
		CompletableFuture.allOf(outerCFS.toArray(new CompletableFuture[outerCFS.size()])).join();
		if (debugL <= 9) {
			System.out.println("Thread " + iterationNumber
					+ " - getOCRBufferedImageWrapperArrayFast() : Finishing all processing for thread "
					+ iterationNumber);
		}
		LeptUtils.dispose(pix);
		return biWrapperArray;
	}

	public static void writeOCRStrings(ArrayList<OCRStringWrapper> biWrapper) {
		if ((DEBUG) && (debugLevel <= 12)) {
			System.out.println("Writing out the strings from OCR :");
			System.out.println("----------------------------------");
			for (OCRStringWrapper wrapper : biWrapper) {
				System.out.println(wrapper.getOcrString());
			}
			System.out.println("----------------------------------");
		}
	}

	public static ProductDescription processFileMultiThreadedFast(String path) throws Exception {

		Instant start = Instant.now();
		if ((DEFAULT_DEBUGGINGIMAGES_DIRECTORY == null) || ("".equals(DEFAULT_DEBUGGINGIMAGES_DIRECTORY))) {
			DEFAULT_DEBUGGINGIMAGES_DIRECTORY = System.getProperty(DEBUGGINGIMAGES_DIRECTORY_KEY);
		}

		processDataWrapper.reset();
		// long baseImagesPreparationTime = 0;
		long pixCleaningAndBoundingBoxPreparationTime = 0;
		long finalPicsAndTesseractAndValidationTime = 0;

		String pollingTimeString = System.getProperty("polling.time");
		long pollingTime = 5L;
		try {
			pollingTime = Long.parseLong(pollingTimeString);
		} catch (Exception e) {
			pollingTime = 5L;
		}

		File file = new File(path);
		isWindows = isWindows();

		if (isWindows == 1) {
			while (true) {
				Thread.currentThread().sleep(pollingTime);
				pollingTime = 5L;
				if (isFileReadyForReadingInWindows(file)) {
					break;
				}
			}
		} else {
			while (true) {
				Thread.currentThread().sleep(pollingTime);
				pollingTime = 5L;
				if (isFileReadyForReadingInUnix(file)) {
					break;
				}
			}
		}

		timeWaitingForImage = Duration.between(start, Instant.now()).toMillis();

		start = Instant.now();

		boolean isGif = (path.indexOf("gif") != -1);

		Pix originalPix1 = null;

		if (!isGif) {
			originalPix1 = Leptonica.INSTANCE.pixRead(path);
		} else {
			System.out.println("Found gif file : " + path);
			boolean loadedImage = false;
			BufferedImage biImage = null;
			try {
				// byte[] bytes = Files.readAllBytes(Paths.get(path));
				biImage = ImageIO.read(new File(path));
				loadedImage = true;
			} catch (IOException e) {
				// Deal with read error here.
			}
			if (!loadedImage) {
				// this code has a memory leak and does not work well
				FILE leptFile = Leptonica1.fopenReadStream(path);
				originalPix1 = Leptonica1.pixReadStreamGif(leptFile);
				Leptonica1.lept_fclose(leptFile);
				// Leptonica1.lept_free(leptFile.getPointer());
			} else {
				try {
					originalPix1 = SBImage.getPixFromBufferedImage(biImage);
				} catch (Exception e) {

				}
			}
		}

		if (debugLevel <= 8) {
			System.out.println("In processFileMultiThreadedFast(): Started processing of = " + currentFileName);
		}

		int originalH = originalPix1.h;
		int originalW = originalPix1.w;
		float heightFactor = 1.0f;
		float widthFactor = 1.0f;

		if ((originalH > targetHeight) && (originalW > targetWidth)) {
			if (originalH > targetHeight) {
				heightFactor = Math.min(heightFactor, (targetHeight * 1.0f) / originalH);
			}
			if (originalW > targetWidth) {
				widthFactor = Math.min(widthFactor, (targetWidth * 1.0f) / originalW);
			}
		}

		Pix originalPix = Leptonica1.pixScaleGeneral(originalPix1, Math.max(heightFactor, widthFactor),
				Math.max(heightFactor, widthFactor), 0.9f, 0);
		LeptUtils.dispose(originalPix1);

		getBaseImages(originalPix, debugLevel);
		if (debugLevel <= 3) {
			System.out.println("In processFileMultiThreadedFast(): Got base images");
		}

		// baseImagesPreparationTime = Duration.between(start,
		// Instant.now()).toMillis();

		start = Instant.now();
		getDerivativeImagesAndBoundingBoxes(debugLevel);
		if (debugLevel <= 3) {
			System.out.println("In processFileMultiThreadedFast(): Got derivative images and bounding boxes");
		}
		pixCleaningAndBoundingBoxPreparationTime = Duration.between(start, Instant.now()).toMillis();

		start = Instant.now();
		ProductDescription finalProduct = getFinalProductDescriptions(debugLevel);
		finalPicsAndTesseractAndValidationTime = Duration.between(start, Instant.now()).toMillis();

		if ((DEBUG) && (debugLevel <= 11)) {
			System.out.println("Time taken waiting for image : " + timeWaitingForImage
					+ "; Time taken for cleaning and getting bounding boxes : "
					+ pixCleaningAndBoundingBoxPreparationTime + "; pixArray + tesseract + string validation time: "
					+ finalPicsAndTesseractAndValidationTime + "; tries : " + (tryNumber));
		}

		LeptUtils.dispose(originalPix);

		int separatorIndex = path.lastIndexOf(File.separator);
		String imageFileWithExtension = path.substring(separatorIndex + 1);
		finalProduct.setFileName(imageFileWithExtension);
		resetTryImageThreads();
		resetImagesAndBBCreationThreads();
		resetBoundingBoxes();
		return finalProduct;
	}

	public static List<String> listFiles(String dir, int depth) throws IOException {
		try (Stream<Path> stream = Files.walk(Paths.get(dir), depth)) {
			return stream.filter(file -> !Files.isDirectory(file)).map(Path::toAbsolutePath).map(Path::toString)
					.collect(Collectors.toList());
		}
	}

	public static void processAllFilesInDirectory(final Resources resources, final TextFlow textFlow,
			final ProcessingData processingData) {
		// int filesHandled = 0;
		// int totalTimeTaken = 0;

		// Found a vexing NullPointer exception when the first image is loaded
		// Originates at getPixFromufferedImage
		// This snippet helps overcome the issue
		final Path inputDirectoryPath = resources.inputDirectoryPath;
		if (seedPix == null) {
			String seedImageFile = inputDirectoryPath.toString() + File.separator + "Initialisation Properties"
					+ File.separator + "Initialisation.png";
			Pix seedPix1 = Leptonica.INSTANCE.pixRead(seedImageFile);
			seedPix = Leptonica1.pixConvertTo8(seedPix1, 0);
			LeptUtils.dispose(seedPix1);
			if (seedPix == null) {
				SysOutController.resetSysOut();
				System.out.println(
						"Initialisation image (Initialisation.png) not found. It should be located in the directory [{input.folder}/Initialisation Properties]");
				System.out.println("Exiting the program");
				System.exit(0);
			} else {
				try {
					boolean currentSysoutSetting = SysOutController.isWritingToSysout();
					BufferedImage bi = LeptUtils.convertPixToImage(seedPix);
					SysOutController.resetSysOut();
					if (debugLevel <= 10) {
						System.out.println("Converted Pix to BufferedImage successfully");
					}
					Pix aPix = SBImage.getPixFromBufferedImage(bi);
					if (debugLevel <= 10) {
						System.out.println("Converted BufferedImage to Pix successfully");
					}
					LeptUtils.dispose(aPix);
					if (debugLevel <= 10) {
						System.out.println("Disposed Pix successfully");
					}
					if (!currentSysoutSetting) {
						SysOutController.ignoreSysout();
					} else {
						if (Resources.getLogFile() != null) {
							SysOutController.redirectSysOut(Resources.getLogFile());
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}

		Thread thread1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					WatchService watchService = inputDirectoryPath.getFileSystem().newWatchService();
					inputDirectoryPath.register(watchService,
							new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_CREATE },
							com.sun.nio.file.SensitivityWatchEventModifier.HIGH);
					// Start infinite loop to watch changes on the directory
					while (loop) {
						WatchKey watchKey = watchService.take();
						// poll for file system events on the WatchKey
						for (final WatchEvent<?> event : watchKey.pollEvents()) {
							// Kind<?> kind = event.kind();
							// if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
							// Path entryCreated = (Path) event.context();
							// }
							// Calling method

							if (!doingProcessingNow) {
								anotherEventOccurred = false;
								if (debugLevel <= 4) {
									System.out.println("Entering process() from thread 1");
								}
								ProcessingData pd = processAFile(resources, textFlow);
								// ProcessingData pd = processUsingServer(resources, textArea);
								processingData.update(pd);
							} else {
								anotherEventOccurred = true;
							}
							// System.out.println("1. Inside WatchEvent = " + pd.reportPerformance());

							// System.out.println("2. Inside WatchEvent = " +
							// processingData.reportPerformance());
						}
						// Break out of the loop if watch directory got deleted
						if (!watchKey.reset() || !loop) {
							if (debugLevel <= 4) {
								System.out.println("loop = " + loop);
								System.out.println("watchKey.reset() = " + watchKey.reset());
							}
							watchKey.cancel();
							watchService.close();
							System.out.println("Stopping watching the input directory");
							// Break out from the loop
							break;
						}
					}
				} catch (InterruptedException interruptedException) {
					System.out.println("Watching thread was interrupted:" + interruptedException);
					return;
				} catch (Exception exception) {
					exception.printStackTrace();
					return;
				}
			}

		});
		thread1.start();
		Thread thread2 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (loop) {
						if ((!doingProcessingNow) && anotherEventOccurred) {
							anotherEventOccurred = false;
							if (debugLevel <= 4) {
								System.out.println("Entering process() from thread 2");
							}
							ProcessingData pd = processAFile(resources, textFlow);
							// ProcessingData pd = processUsingServer(resources, textArea);
							processingData.update(pd);
						}
						Thread.sleep(100);
					}
				} catch (InterruptedException interruptedException) {
					System.out.println("Watching thread was interrupted:" + interruptedException);
					return;
				} catch (Exception exception) {
					exception.printStackTrace();
					return;
				}
			}
		});
		thread2.start();
		if (debugLevel <= 10) {
			System.out.println("Ready to process input images");
		}
	}

	private static ProcessingData processAFile(Resources resources, TextFlow textFlow) {

		if (doingProcessingNow) {
			return new ProcessingData(0, 0, 0);
		}
		doingProcessingNow = true;
		Instant t = null;
		long timeTaken = 0;
		StringBuffer outcome = null;
		ProductDescription productDescription = null;
		int filesHandled = 0;
		int totalTimeTaken = 0;
		int okFiles = 0;

		List<String> files = resources.listFiles(1);
		if (debugLevel <= 9) {
			System.out.println("Found files - " + files);
		}
		for (String file : files) {
			t = Instant.now();
			outcome = new StringBuffer();
			try {
				File thisFile = new File(file);
				int dotIndex = thisFile.getName().lastIndexOf(".");
				currentFileName = thisFile.getName().substring(0, dotIndex);
				initialiseProductParameters(productFileArgument);
				CheckProductProperties.checkIfProductIsGiven();
				// System.out.println("Product given = " +
				// System.getProperty(CheckProductProperties.productNameKey));
				// System.out.println("Price given = " +
				// CheckProductProperties.givenProductPrice);
				// productDescription = processFileMultiThreaded(file);
				resetInterruptParallelThreads();
				clearBaseImages();
				clearPreBBImages();
				clearBBImages();
				clearTesseractImages();
				// imageIsEmpty = false;
				tryNumber = 1;
				ProductDescription.incrementSerialNo();
				try1RotationAngle = 0.0f;
				if (CheckProductProperties.productIsGiven && (CheckProductProperties.givenProductPrice == 0)) {
					productDescription = ProductDescription.ERROR_NO_PRODUCT_SPECIFIED;
					productDescription.fileName = currentFileName;
				} else {
					productDescription = processFileMultiThreadedFast(file);
					++filesHandled;
					okFiles += (productDescription.productOK == ProductDescription.ALL_OK) ? 1 : 0;
				}
				String result = productDescription.toString();
				// outcome.append(file).append(ProductDescription.separator).append(result);
				outcome.append(result);
				// if (!(CheckProductProperties.productIsGiven &&
				// (CheckProductProperties.givenProductPrice == 0))) {
				// int indexOfDot = productDescription.getFileName().lastIndexOf('.');
				// String imageFile = productDescription.getFileName().substring(0, indexOfDot);
				if (productDescription.productOK != ProductDescription.BAD_ERROR_WILL_NEED_REPROCESSING) {
					resources.moveFile(file);
				} else {
					resources.moveErrorFile(file);
				}
				// resources.writeResultsFile(imageFile + ".txt", result);
				// } // else {
				resources.writeResultsFile(currentFileName + ".txt", result);
				// }

			} catch (Exception e) {
				e.printStackTrace();
				outcome.append(file).append(ProductDescription.separator).append("Error while handling file - ")
						.append(e);
				resources.moveErrorFile(file);
				int separatorIndex = file.lastIndexOf(File.separator);
				String imageFileWithExtension = file.substring(separatorIndex + 1);
				int indexOfDot = imageFileWithExtension.lastIndexOf('.');
				String imageFile = imageFileWithExtension.substring(0, indexOfDot);
				resources.writeResultsFile(imageFile + ".txt", ProductDescription.errorString(imageFile));
			}
			timeTaken = Duration.between(t, Instant.now()).toMillis();
			totalTimeTaken += timeTaken;
			outcome.append(ProductDescription.separator).append(timeTaken).append(" ms")
					.append(ProductDescription.separator).append(tryNumber);
			resources.println(outcome.toString());
			resources.flush();
			int errorLevel = 0;
			if (productDescription != null) {
				if (productDescription.productOK != ProductDescription.ALL_OK) {
					errorLevel = 1;
				}
			} else {
				errorLevel = 2;
			}
			if (productDescription != null) {
				if ("".equals(productDescription.month) || "".equals(productDescription.year)
						|| "".equals(productDescription.finalPrice)
						|| ((!CheckProductProperties.productIsGiven) && ("".equals(productDescription.productName)))) {
					errorLevel = 2;
				}
				if (productDescription.productOK == ProductDescription.BAD_ERROR_WILL_NEED_REPROCESSING) {
					errorLevel = 2;
				}
				if (debugLevel == 2) {
					System.out.println("Error Level = " + errorLevel + "; " + productDescription.productName + "; "
							+ productDescription.finalPrice + "; " + productDescription.month + "; "
							+ productDescription.year);
				} else {
					System.out.println(productDescription.productName + "; " + productDescription.finalPrice + "; "
							+ productDescription.month + "; " + productDescription.year);
				}
			} else {
				errorLevel = 2;
			}
			if (textFlow != null) {
				if (errorLevel == 0) {
					appendGreenMessage(textFlow,
							outcome.toString() + " - " + timeWaitingForImage + " ms" + System.lineSeparator());
				} else {
					if (errorLevel == 1) {
						if (CheckProductProperties.productIsGiven) {
							if ((!productDescription.foundPrice) || (productDescription.year.equals(""))
									|| (productDescription.month.equals(""))) {
								appendRedMessage(textFlow, outcome.toString() + " - " + timeWaitingForImage + " ms"
										+ System.lineSeparator());
							} else {
								appendAmberMessage(textFlow, outcome.toString() + " - " + timeWaitingForImage + " ms"
										+ System.lineSeparator());
							}
						} else {
							appendAmberMessage(textFlow,
									outcome.toString() + " - " + timeWaitingForImage + " ms" + System.lineSeparator());
						}
					} else {
						if (errorLevel == 2) {
							appendRedMessage(textFlow,
									outcome.toString() + " - " + timeWaitingForImage + " ms" + System.lineSeparator());
						}
					}
				}
			}
			System.out.println(outcome.toString());
		}
		ProcessingData pd = new ProcessingData(filesHandled, totalTimeTaken, okFiles);

		if (autoReport) {
			if (filesHandled != 0) {
				if (textFlow != null) {
					appendNormalMessage(textFlow, pd.reportPerformance() + System.lineSeparator());
				}
				System.out.println(pd.reportPerformance());
			}
		}
		doingProcessingNow = false;
		return pd;
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		// System.out.println("Entered start");
		Button startButton = new Button("Start Processing");
		Button endButton = new Button("End Processing");
		Button reportStatsButton = new Button("Report Stats");
		// final Resources resources = new Resources(workingDir);
		final Resources resources = new Resources();
		HBox hbox = new HBox(startButton, endButton, reportStatsButton);
		Separator separator = new Separator(Orientation.HORIZONTAL);
		// final TextArea textArea = new TextArea();
		ScrollPane scrollPane = new ScrollPane();
		final TextFlow textFlow = new TextFlow();
		scrollPane.setId("presentationScrollPane");
		scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
		scrollPane.setFitToWidth(true);
		scrollPane.setContent(textFlow);
		// textArea.setEditable(false);
		// textArea.setWrapText(true);
		VBox vbox = new VBox(hbox, separator);
		VBox bottom = new VBox(scrollPane);
		VBox.setVgrow(scrollPane, Priority.ALWAYS);
		BorderPane pane = new BorderPane();
		pane.setTop(vbox);
		pane.setCenter(bottom);
		StackPane root = new StackPane();
		root.getChildren().add(pane);
		textFlow.getChildren().addListener((ListChangeListener<Node>) ((change) -> {
			textFlow.requestLayout();
			scrollPane.requestLayout();
			scrollPane.setVvalue(1.0f);
		}));

		Scene scene = new Scene(root, 1100, 550);
		primaryStage.setTitle("Read Labels");
		primaryStage.setScene(scene);
		primaryStage.sizeToScene();
		primaryStage.show();

		startButton.setDisable(true);
		endButton.setDisable(true);
		reportStatsButton.setDisable(true);

		if (debugLevel <= 2) {
			System.out.println("Please wait. Initialising...");
		}
		// appendNormalMessage(textFlow, "Please wait. Initialising...");
		// appendNormalMessage(textFlow, System.lineSeparator());
		// textArea.appendText("Please wait. Initialising...");
		// textArea.appendText(System.lineSeparator());

		// alarmProcess = runProcess("javaw.exe", "com.techwerx.textprocessing.Alarm",
		// alarmProcessParams.toString(),
		// "-Xms128M", "-Xmx256M",
		// "--module-path E:\\TechWerx\\Java\\javafx-sdk-13.0.1\\lib
		// --add-modules=javafx.controls,javafx.media,javafx.fxml,javafx.embed.swing");
		startButton.setDisable(false);
		reportStatsButton.setDisable(false);

		if (debugLevel <= 2) {
			System.out.println(
					"Ready for OCR process. Whenever you \"End Processing\", you should click the \"Start Processing\" button to re-start.");
		}
		// appendNormalMessage(textFlow,
		// "Ready for OCR process. Whenever you \"End Processing\", you should click the
		// \"Start Processing\" button to re-start.");
		// appendNormalMessage(textFlow, System.lineSeparator());

		// textArea.appendText("Ready for OCR process. Click the \"Start Processing\"
		// button to start.");
		// textArea.appendText(System.lineSeparator());

		final ProcessingData processingData = new ProcessingData(0, 0, 0);
		startButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				// don't take any action if a false event is fired when the window is in
				// minimised state. (Sigh ! That happens !!)
				if (windowMinimised) {
					return;
				}
				loop = true; // enables the watchService in infinite loop
				startButton.setDisable(true);
				endButton.setDisable(false);
				resources.initialiseWriter();
				if (!resources.isValid()) {
					System.out.println("Resources are invalid");
					return;
				}
				// processFiles(resources, textArea, processingData);
				processAllFilesInDirectory(resources, textFlow, processingData);
			}
		});
		endButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				// don't take any action if a false event is fired when the window is in
				// minimised state. (Sigh ! That happens !!)
				if (windowMinimised) {
					return;
				}
				// System.out.println("Caught event in End Button");
				loop = false;
				// System.out.println("Set loop to " + loop);
				resources.close();

				appendNormalMessage(textFlow, processingData.reportPerformance() + System.lineSeparator());
				appendNormalMessage(textFlow, "For consolidated final results for the day, look up file - "
						+ resources.ocrFilePath.toAbsolutePath().toString() + System.lineSeparator());

				// textArea.appendText(processingData.reportPerformance() +
				// System.lineSeparator());
				// textArea.appendText("For final results, look up file - "
				// + resources.ocrFilePath.toAbsolutePath().toString() +
				// System.lineSeparator());
				processingData.reset();
				endButton.setDisable(true);
				startButton.setText("Re-start Processing");
				startButton.setDisable(false);
			}
		});
		reportStatsButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				// don't take any action if a false event is fired when the window is in
				// minimised state. (Sigh ! That happens !!)
				if (windowMinimised) {
					return;
				}
				appendNormalMessage(textFlow, processingData.reportPerformance() + System.lineSeparator());
				// textArea.appendText(processingData.reportPerformance() +
				// System.lineSeparator());
			}
		});

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent e) {
				int response = ReadLabelsMultiThreaded.this.closeWindowEvent(primaryStage);
				if (response != -1) {
					System.out.println("Shutting down the application");
					outerThreadService.shutdown();
					innerThreadService.shutdown();
					// tesseractThreadService.shutdown();
					parallelThreadPool.shutdown();
					// alarmProcess.destroyForcibly();
					Platform.exit();
					System.exit(0);
				} else {
					e.consume();
				}
			}
		});
		primaryStage.iconifiedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
				// System.out.println("minimized:" + t1.booleanValue());
				windowMinimised = t1.booleanValue();
			}
		});
		// System.out.println("About to fire start button");
		startButton.fire();

	}

	private int closeWindowEvent(Stage primaryStage) {
		System.out.println("Window close request ...");

		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.getButtonTypes().remove(ButtonType.OK);
		alert.getButtonTypes().add(ButtonType.CANCEL);
		alert.getButtonTypes().add(ButtonType.YES);
		alert.setTitle("Quit application");
		alert.setContentText(String.format("Close the application and Exit ?"));
		alert.initOwner(primaryStage.getOwner());
		Optional<ButtonType> res = alert.showAndWait();

		if (res.isPresent()) {
			if (res.get().equals(ButtonType.CANCEL)) {
				return -1;
			}
		}
		return 1;

	}

	public static void main(String[] args) throws Exception {

		Leptonica1.setLeptDebugOK(0);
		Leptonica1.setMsgSeverity(ILeptonica.L_SEVERITY_NONE);
		int length = args.length;
		if (length == 0) {
			System.out.println("Expecting command line values for 2 arguments - initFile and prodFile");
			System.exit(0);
		} else {
			if (length == 1) {
				System.out.println("Expecting command line values for both arguments - initFile and prodFile");
				boolean initFileProvided = (args[0].indexOf("initFile") > -1);
				if (initFileProvided) {
					// System.out.println(
					// "Found initFile specified in " + args[0] + ". Proceeding with default for
					// prodFile");
					initialiseProperties(args[0], null);
				} else {
					System.out.println("Did not find initFile value. Exiting program.");
				}
			} else {
				boolean initFileFirst = (args[0].indexOf("initFile") > -1);
				if (initFileFirst) {
					// System.out.println(
					// "Found initFile specified in " + args[0] + ", and prodFile specified in " +
					// args[1]);
					initialiseProperties(args[0], args[1]);
				} else {
					// System.out.println(
					// "Found initFile specified in " + args[1] + ", and prodFile specified in " +
					// args[0]);
					initialiseProperties(args[1], args[0]);
				}
			}
		}

		techwerxTesseractPool = new TechWerxTesseractHandlePool(new TechWerxTesseractHandleFactory(1, debugLevel),
				TechWerxTesseractHandlePool.oneMachineConfig, 1, debugLevel);

		originalImagesTesseractPool = new TechWerxTesseractHandlePool(
				new TechWerxTesseractHandleFactory(true, 1, debugLevel), TechWerxTesseractHandlePool.smallPoolConfig, 1,
				debugLevel);
		// bnImageTesseract = new TechWerxTesseract(true, debugLevel);
		// cnImageTesseract = new TechWerxTesseract(true, debugLevel);
		// umImageTesseract = new TechWerxTesseract(true, debugLevel);
		Leptonica.INSTANCE.setLeptDebugOK(0);
		/*
		 * threadPool = new ThreadPoolExecutor(4, 10, 0L, TimeUnit.MILLISECONDS, new
		 * LinkedBlockingQueue<Runnable>()); threadPool.setThreadFactory(new
		 * OpJobThreadFactory(Thread.NORM_PRIORITY-2));
		 */

		if ("gui".contentEquals(System.getProperty(runModeKey))) {
			// System.out.println("About to launch GUI");
			launch(args);
		} else {
			Thread closeChildThread = new Thread() {
				@Override
				public void run() {
					System.out.println("Shutting down the thread services");
					outerThreadService.shutdown();
					innerThreadService.shutdown();
					// tesseractThreadService.shutdown();
					LeptUtils.dispose(bbNumberingFont);
					// killProcesses();
					// alarmProcess.destroyForcibly();
					System.exit(0);
				}
			};
			Runtime.getRuntime().addShutdownHook(closeChildThread);
			// processFiles(new Resources(), (TextArea) null, new ProcessingData(0, 0, 0));
			processAllFilesInDirectory(new Resources(), (TextFlow) null, new ProcessingData(0, 0, 0));
		}

	}

	public static byte[] pixToBytes(Pix pix) {
		PointerByReference pdata = new PointerByReference();
		NativeSizeByReference psize = new NativeSizeByReference();
		int format = IFF_TIFF;
		Leptonica1.pixWriteMem(pdata, psize, pix, format);
		byte[] bytes = pdata.getValue().getByteArray(0, psize.getValue().intValue());
		Leptonica1.lept_free(pdata.getValue());
		return bytes;
	}

	public static double averagePixelValue(Pix pix) {

		FloatBuffer average = FloatBuffer.allocate(1);
		if (pix == null) {
			if (debugLevel <= 2) {
				System.out.println("In averagePixelValue(): about to return 0 as pix in null");
			}
			return 0;
		}
		if (pix.d == 8) {
			if (debugLevel <= 2) {
				System.out.println("In averagePixelValue(): entered pix.d == 8");
			}
			// Leptonica1.pixGetPixelAverage(pix, null, 0, 0, 1, average);
			Leptonica1.pixAverageInRect(pix, null, null, 0, 255, 1, average);
			float avg = average.get(0);
			if (debugLevel <= 2) {
				System.out.println("Average Pixel = " + avg);
			}
			return avg * 1.0;
		}
		if (debugLevel <= 2) {
			System.out.println("In averagePixelValue(): entered pix.d != 8");
		}
		Pix pix8 = Leptonica1.pixConvertTo8(pix, 0);
		Leptonica1.pixAverageInRect(pix, null, null, 0, 255, 1, average);
		float avg = average.get(0);
		if (debugLevel <= 2) {
			System.out.println("Average Pixel = " + avg);
		}
		LeptUtils.dispose(pix8);
		return avg * 1.0;
	}

	public static void drawBoundingBoxesOnPix(Pix pix, ArrayList<ArrayList<Rectangle>> words, int suffixNo, int debugL,
			String identifier, int iterationNumber) {
		if (debugL <= 6) {

			Pix pix1 = Leptonica1.pixConvertTo32(pix);
			int lineNo = 1;
			for (ArrayList<Rectangle> word : words) {
				for (Rectangle letterBox : word) {
					Box box = new Box(letterBox.x, letterBox.y, letterBox.width, letterBox.height, 0);
					// this box will be gc'ed because it is on the JVM heap
					Leptonica1.pixRenderBox(pix1, box, 1, ILeptonica.L_FLIP_PIXELS);
					Leptonica1.pixSetTextline(pix1, bbNumberingFont, "" + lineNo, 0xFF000000, letterBox.x,
							letterBox.y - 1, null, null);
				}
				++lineNo;
			}

			Leptonica1.pixSetTextline(pix1, bbNumberingFont, identifier, 0xFF000000, 3, 10, null, null);
   PixDebugWriter.writeIfDebug(debugL, 6, pix1, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "symbolBoxes-" + iterationNumber + "-"+ suffixNo + ".png");
			// LeptUtils.dispose(font);
			LeptUtils.dispose(pix1);
			// System.gc();
		}
	}

	public static ArrayList<OCRStringWrapper> ocrBIWrapperArray(ArrayList<OCRBufferedImageWrapper> biArray,
			ExecutorService outer, int threadNumber, int debugL) throws Exception {

		if (DEBUG && (debugLevel <= 2)) {
			System.out.println("Thread " + threadNumber + " - Entering ocrBIWrapperArray() :");
		}
		int noOfImages = biArray.size();
		final ArrayList<OCRStringWrapper> pageResult = new ArrayList<>(noOfImages);
		ArrayList<CompletableFuture<Boolean>> cfs = new ArrayList<CompletableFuture<Boolean>>(noOfImages);
		mainloop: for (int i = 0; i < noOfImages; ++i) {
			final int imageNumber = i + 1;
			final BufferedImage image = biArray.get(imageNumber - 1).getImage();
			final OCRStringWrapper resultWrapper = new OCRStringWrapper(
					biArray.get(imageNumber - 1).getLikelyPixelHeight(), "",
					biArray.get(imageNumber - 1).getBoundingBoxes());
			if (DEBUG && (debugLevel <= 2)) {
				System.out.println("Thread " + threadNumber + " - In ocrBIWrapperArray() : pixelHeight = "
						+ resultWrapper.likelyPixelHeight);
			}
			if (getInterruptParallelThreads()) {
				if (debugL <= 5) {
					System.out
							.println("Thread " + threadNumber + " - In ocrBIWrapperArray() : Exiting processing image "
									+ imageNumber + " due to interrupt");
				}
				break mainloop;
			}

			cfs.add(CompletableFuture.supplyAsync(() -> {
				if (debugL <= 5) {
					System.out.println("Thread " + threadNumber + " - In ocrBIWrapperArray() : Starting processing of "
							+ imageNumber);
				}
				pageResult.add(resultWrapper);

				TechWerxTesseractHandle instanceHandle = null;
				try {
					synchronized (interruptLock) {
						instanceHandle = (TechWerxTesseractHandle) techwerxTesseractPool.borrowObject();
					}
					if (debugL <= 5) {
						System.out.println("Thread " + threadNumber + " - In ocrBIWrapperArray() : For image "
								+ imageNumber + " got TechWerxTesseract handle - " + instanceHandle);
					}
				} catch (Exception e) {
					e.printStackTrace();
					try {
						synchronized (interruptLock) {
							if (debugL <= 5) {
								System.out.println("Thread " + threadNumber
										+ " - In ocrBIWrapperArray() : Problem 1 - Returning TechWerxTesseract handle - "
										+ instanceHandle);
							}
							techwerxTesseractPool.returnObject(instanceHandle);
						}
					} catch (Exception e1) {
						System.out.println("Thread " + threadNumber
								+ " - In ocrBIWrapperArray() : Problem 2 - Exception encountered while returning object instanceHandle to tesseract pool - "
								+ instanceHandle);
					}
					return Boolean.FALSE;
				}
				TechWerxTesseract instance = instanceHandle.getHandle();

				boolean success = true;
				String result = "";
				try {
					if (getInterruptParallelThreads()) {
						resultWrapper.setOcrString(result);
						synchronized (interruptLock) {
							if (debugL <= 5) {
								System.out.println("Thread " + threadNumber
										+ " - In ocrBIWrapperArray() : Interrupted - Returning TechWerxTesseract handle - "
										+ instanceHandle);
							}
							techwerxTesseractPool.returnObject(instanceHandle);
						}
						return Boolean.FALSE;
					}
					result = instance.doOCR(image);
				} catch (Exception e) {
					success = false;
				}
				if (success) {
					if (debugL <= 9) {
						System.out
								.println("Thread " + threadNumber + " - In ocrBIWrapperArray() : the result from image "
										+ imageNumber + " is - " + result);
						System.out.println("Thread " + threadNumber
								+ " - In ocrBIWrapperArray() : the bounding boxes for this image are "
								+ biArray.get(imageNumber - 1).getBoundingBoxes());
					}
					// lineResult.add(result, 0.0f);
					resultWrapper.setOcrString(result);
				}
				synchronized (interruptLock) {
					if (debugL <= 5) {
						System.out.println("Thread " + threadNumber
								+ " - In ocrBIWrapperArray() : After successful OCR - Returning TechWerxTesseract handle - "
								+ instanceHandle);
					}
					techwerxTesseractPool.returnObject(instanceHandle);
				}
				return Boolean.TRUE;
			}, outer));
		}
		CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
		return pageResult;
	}

	public static ArrayList<ArrayList<Rectangle>> getBoundingBoxes(Pix pix, ProcessDataWrapper processDataWrapper,
			int threadNumber, int debugL) {

		Rectangle[] bboxes = getDefaultBoxes(pix, threadNumber, debugL);
		ArrayList<ArrayList<Rectangle>> lines = segregateBoxesIntoLines(bboxes, processDataWrapper, threadNumber,
				debugL);
		int height = processDataWrapper.kdeData.mostLikelyHeight;
		int width = processDataWrapper.kdeData.mostLikelyWidth;
		// remove only inordinately large boxes
		// no need to remove others, as there is a routine later to remove large boxes
		// from a line
		double heightCutOff = 5.0;
		double widthCutOff = 100.0; // 15.0
		for (int i = 0; i < lines.size(); ++i) {
			for (int j = lines.get(i).size() - 1; j >= 0; --j) {
				Rectangle r = lines.get(i).get(j);
				if ((r.width > (width * widthCutOff)) || (r.height > (heightCutOff * height))) {
					Rectangle r1 = lines.get(i).remove(j);
					if (debugL <= 10) {
						System.out.print("In getBoundingBoxes() : Dropped box - " + r1 + "  because ");
						System.out.print(
								"either height > " + (heightCutOff * height) + " or width > " + (width * widthCutOff));
					}
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

	}

	public static Rectangle[] getDefaultBoxes(Pix pix, int threadNumber, int debugL) {

		Sel sel1 = null, sel2 = null;
		Pix pix1 = null, pix2 = null;

		sel1 = Leptonica1.selCreateBrick(3, 3, 1, 1, ILeptonica.SEL_HIT);
		sel2 = Leptonica1.selCreateBrick(3, 3, 1, 1, ILeptonica.SEL_HIT);
		pix1 = Leptonica1.pixClose(null, pix, sel1.getPointer());
		pix2 = Leptonica1.pixDilate(null, pix1, sel2.getPointer());

		int connectivity = 4;
		Boxa result = Leptonica1.pixConnCompBB(pix2, connectivity);
		Boxa sortedResult = Leptonica1.boxaSort(result, ILeptonica.L_SORT_BY_Y, ILeptonica.L_SORT_INCREASING, null);

		LeptUtils.dispose(sel1);
		LeptUtils.dispose(sel2);
		LeptUtils.dispose(pix1);
		LeptUtils.dispose(pix2);

		int numberOfBoxes = Leptonica1.boxaGetCount(sortedResult);

		// add rectangles to an arraylist, but remove small boxes of height 6 or below,
		// and also remove boxes of width 5 or belo
		int absoluteHeightCutOff = 6;
		int absoluteWidthCutOff = 6;
		ArrayList<Rectangle> wordRectangles = new ArrayList<>();
		for (int j = 0; j < numberOfBoxes; ++j) {
			Box box = Leptonica1.boxaGetBox(sortedResult, j, ILeptonica.L_CLONE);
			if ((box.h > absoluteHeightCutOff) && (box.w > absoluteWidthCutOff)) {
				wordRectangles.add(new Rectangle(box.x, box.y, box.w, box.h));
			}
			LeptUtils.dispose(box);
		}

		if (debugL <= 2) {
			Pix pix3 = Leptonica1.pixCopy(null, pix);
   PixDebugWriter.writeIfDebug(debugL, 2, pix3, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "bbOnDilated-" + tryNumber + ".png");
			LeptUtils.dispose(pix3);
			System.out.println("Thread : " + threadNumber + " - Box rectangles found in getDefaultBoxes() are - "
					+ wordRectangles);
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
					System.out.println("Thread : " + threadNumber
							+ " - Removed suspect word at edge in getDefaultBoxes() " + removed);
				}
				if (debugL <= 10) {
					System.out.println(
							"Thread : " + threadNumber + " - In getDefaultBoxes() : Dropped box at edge - " + removed);
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

		// remove words of small height from the words that are not at the edge
		double referenceHeight = nonSuspectHeightStats.getPercentile(50);
		int initialSize = finalWords.size();
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber + " - referenceHeight = " + referenceHeight);
			System.out.println("Thread : " + threadNumber + " - In getDefaultBoxes(), the round 0 number of words = "
					+ initialSize);
			System.out.println("Thread : " + threadNumber + " - Before round 0 removal, the boxes are - " + finalWords);
		}
		for (int i = initialSize - 1; i >= 0; --i) {
			if (finalWords.get(i).height < (0.35 * referenceHeight)) {
				Rectangle r = finalWords.remove(i);
				if (debugL <= 10) {
					System.out.println("Thread : " + threadNumber
							+ " - In getDefaultBoxes() : Dropped non-edge box due to height - " + r);
				}
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
   PixDebugWriter.writeIfDebug(debugL, 3, newPix, DEFAULT_DEBUGGINGIMAGES_DIRECTORY + "/" + "newPixForDefaultBoxes.png");
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
			System.out.println("Thread : " + threadNumber + " - Changed referenceHeight = " + referenceHeight);
			System.out.println("Thread : " + threadNumber
					+ " - In getDefaultBoxes(), the round 1 number of words in newWordRectangles = " + initialSize);
			System.out.println("Thread : " + threadNumber
					+ " - Before round 1 removal, the boxes in newWordRectangles are - " + newWordRectangles);
		}

		// do another round of small box removals with the new reference height
		for (int i = initialSize - 1; i >= 0; --i) {
			if (newWordRectangles.get(i).height < (0.35 * referenceHeight)) {
				Rectangle r = newWordRectangles.remove(i);
				if (debugL <= 10) {
					System.out.println(
							"Thread : " + threadNumber + " - In getDefaultBoxes() round 1 : Dropped box - " + r);
				}
			}
		}
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber
					+ " - In getDefaultBoxes(), the round 1 number of words after removing small height words = "
					+ newWordRectangles.size());
			System.out.println("Thread : " + threadNumber + " - After round 1, the boxes are - " + newWordRectangles);
		}

		if (((newWordRectangles.size() * 1.0) / (initialSize + 1)) > 0.95) {
			LeptUtils.dispose(newPix);
			return newWordRectangles.toArray(new Rectangle[finalWords.size()]);
		}

		Pix pix2D = null;
		Boxa result2 = null;
		Boxa sortedResult2 = null;

		// if majority of the boxes have been chucked out, then dilate the pic with a
		// (2,3) sel and repopulate the wordRectangles

		newWordRectangles.clear();
		int xD = 1;
		int yD = 2;
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

		initialSize = newWordRectangles.size();
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber + " - In getDefaultBoxes(), the round 2 number of words = "
					+ initialSize);
		}
		for (int i = initialSize - 1; i >= 0; --i) {
			if (newWordRectangles.get(i).height < (0.35 * referenceHeight)) {
				Rectangle r = newWordRectangles.remove(i);
				if (debugL <= 10) {
					System.out.println(
							"Thread : " + threadNumber + " - In getDefaultBoxes() round 2 : Dropped box - " + r);
				}
			}
		}
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber
					+ " - In getDefaultBoxes(), the round 2 number of words after removing small height words = "
					+ newWordRectangles.size());
		}
		// if majority of the boxes are retained, then return newWordRectangles
		// else, return finalWords

		if (((newWordRectangles.size() * 1.0) / (initialSize + 1)) >= 0.825) {
			LeptUtils.dispose(newPix);
			return newWordRectangles.toArray(new Rectangle[finalWords.size()]);
		}

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

		initialSize = newWordRectangles.size();
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber + " - In getDefaultBoxes(), the round 3 number of words = "
					+ initialSize);
		}
		for (int i = initialSize - 1; i >= 0; --i) {
			if (newWordRectangles.get(i).height < (0.35 * referenceHeight)) {
				Rectangle r = newWordRectangles.remove(i);
				if (debugL <= 10) {
					System.out.println(
							"Thread : " + threadNumber + " - In getDefaultBoxes() round 3 : Dropped box - " + r);
				}
			}
		}
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber
					+ " - In getDefaultBoxes(), the round 3 number of words after removing small height words = "
					+ newWordRectangles.size());
		}
		// if majority of the boxes are retained, then return newWordRectangles
		// else, return finalWords

		if (((newWordRectangles.size() * 1.0) / (initialSize + 1)) >= 0.825) {
			LeptUtils.dispose(newPix);
			return newWordRectangles.toArray(new Rectangle[finalWords.size()]);
		}

		// if the above doesn't work, then try again with higher yD

		Pix pix3D = null;
		Boxa result3 = null;
		Boxa sortedResult3 = null;

		newWordRectangles.clear();
		xD = 2;
		yD = 4;
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

		initialSize = newWordRectangles.size();
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber + " - In getDefaultBoxes(), the round 4 number of words = "
					+ initialSize);
		}
		for (int i = initialSize - 1; i >= 0; --i) {
			if (newWordRectangles.get(i).height < (0.35 * referenceHeight)) {
				Rectangle r = newWordRectangles.remove(i);
				if (debugL <= 10) {
					System.out.println(
							"Thread : " + threadNumber + " - In getDefaultBoxes() round 4 : Dropped box - " + r);
				}
			}
		}
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber
					+ " - In getDefaultBoxes(), the round 4 number of words after removing small height words = "
					+ newWordRectangles.size());
		}
		// if majority of the boxes are retained, then return newWordRectangles
		// else, return finalWords

		if (((newWordRectangles.size() * 1.0) / (initialSize + 1)) >= 0.825) {
			LeptUtils.dispose(newPix);
			return newWordRectangles.toArray(new Rectangle[finalWords.size()]);
		}

		// if the above doesn't work, then try again with higher xD

		Pix pix4D = null;
		Boxa result4 = null;
		Boxa sortedResult4 = null;

		newWordRectangles.clear();
		xD = 3;
		yD = 4;
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

		initialSize = newWordRectangles.size();
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber + " - In getDefaultBoxes(), the round 5 number of words = "
					+ initialSize);
		}
		for (int i = initialSize - 1; i >= 0; --i) {
			if (newWordRectangles.get(i).height < (0.35 * referenceHeight)) {
				Rectangle r = newWordRectangles.remove(i);
				if (debugL <= 10) {
					System.out.println(
							"Thread : " + threadNumber + " - In getDefaultBoxes() round 5 : Dropped box - " + r);
				}

			}
		}
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber
					+ " - In getDefaultBoxes(), the round 5 number of words after removing small height words = "
					+ newWordRectangles.size());
		}
		// if majority of the boxes are retained, then return newWordRectangles
		// else, return finalWords

		if (((newWordRectangles.size() * 1.0) / (initialSize + 1)) >= 0.825) {
			LeptUtils.dispose(newPix);
			return newWordRectangles.toArray(new Rectangle[finalWords.size()]);
		}
		LeptUtils.dispose(newPix);
		return finalWords.toArray(new Rectangle[finalWords.size()]);

	}

	public static ArrayList<ArrayList<Rectangle>> segregateBoxesIntoLines(Rectangle[] letters,
			ProcessDataWrapper processDataWrapper, int threadNumber, int debugL) {

		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber + " - Entered segregateBoxesIntoLines() with "
					+ letters.length + " boxes");
			for (Rectangle letter : letters) {
				System.out.println(letter);
			}
		}

		DescriptiveStatistics heightStats = new DescriptiveStatistics();
		DescriptiveStatistics widthStats = new DescriptiveStatistics();

		for (Rectangle word : letters) {
			if (word != null) {
				heightStats.addValue(word.height);
				// widthStats.addValue(word.width);
			}
		}

		double medHeight = heightStats.getMean();
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber
					+ " - In segregateBoxesIntoLines : First cut medheight for eliminating small boxes = " + medHeight);
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
		if (kde <= 1) {
			kde = 2;
		}
		if (debugL <= 5) {
			System.out.println("Thread : " + threadNumber + " - In segregateBoxesIntoLines : Height KDE = " + kde);
		}
		processDataWrapper.kdeData.setHeightKDE(kde);
		int numberOfBins = ((1600 % kde) == 0) ? 1600 / kde : ((1600 / kde) + 1);
		int histogram[] = new int[numberOfBins];
		for (Rectangle letter : letters) {
			if (letter != null) {
				if (letter.height > (medHeight * 0.5)) {
					int binNumber = ((letter.height % kde) != 0) ? (letter.height / kde) : ((letter.height / kde) - 1);
					if (binNumber < 0) {
						if (debugL <= 5) {
							System.out.println("Thread : " + threadNumber
									+ " - In segregateBoxesIntoLines : Letter height = " + letter.height);
						}
						binNumber = 0;
					}
					++histogram[binNumber];
				}
			}
		}

		for (int i = 1; i < (numberOfBins - 1); ++i) {
			if ((histogram[i] > 4) && (histogram[i] >= histogram[i - 1]) && (histogram[i] >= histogram[i + 1])) {
				modes.add(i);
				++numberOfModes;
			}
		}

		if (modes.size() == 1) {
			processDataWrapper.kdeData.mostLikelyHeight = (int) ((modes.get(0) + 0.5) * kde);
		}

		/*
		 * double averageHeight1 = 0.0; double averageHeight2 = 0.0; if (modes.size() ==
		 * 2) { int mode1 = modes.get(0); int mode2 = modes.get(1); if (mode1 == 0) {
		 * averageHeight1 = ((((0.5 * kde) * histogram[0]) + ((1.5 * kde) *
		 * histogram[1])) * 1.0) / (histogram[0] + histogram[1]); } else {
		 * averageHeight1 = (((((mode1 - 0.5) * kde) * histogram[mode1 - 1]) + (((mode1
		 * + 0.5) * kde) * histogram[mode1]) + (((mode1 + 1.5) * kde) * histogram[mode1
		 * + 1])) 1.0) / (histogram[mode1 - 1] + histogram[mode1] + histogram[mode1 +
		 * 1]); } averageHeight2 = (((((mode2 - 0.5) * kde) * histogram[mode2 - 1]) +
		 * (((mode2 + 0.5) * kde) * histogram[mode2]) + (((mode2 + 1.5) * kde) *
		 * histogram[mode2 + 1])) 1.0) / (histogram[mode2 - 1] + histogram[mode2] +
		 * histogram[mode2 + 1]);
		 *
		 * }
		 */

		if (modes.size() >= 2) {
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

			/*
			 * if (mode1 == 0) { averageHeight1 = ((((0.5 * kde) * histogram[0]) + ((1.5 *
			 * kde) * histogram[1])) * 1.0) / (histogram[0] + histogram[1]); } else {
			 * averageHeight1 = (((((mode1 - 0.5) * kde) * histogram[mode1 - 1]) + (((mode1
			 * + 0.5) * kde) * histogram[mode1]) + (((mode1 + 1.5) * kde) * histogram[mode1
			 * + 1])) 1.0) / (histogram[mode1 - 1] + histogram[mode1] + histogram[mode1 +
			 * 1]); } averageHeight2 = (((((mode2 - 0.5) * kde) * histogram[mode2 - 1]) +
			 * (((mode2 + 0.5) * kde) * histogram[mode2]) + (((mode2 + 1.5) * kde) *
			 * histogram[mode2 + 1])) 1.0) / (histogram[mode2 - 1] + histogram[mode2] +
			 * histogram[mode2 + 1]);
			 */
		}

		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber + " - In segregateBoxesIntoLines : Histogram Array = "
					+ Arrays.toString(histogram));
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
			System.out.println("Thread : " + threadNumber
					+ " - In segregateBoxesIntoLines : Most Likely Height Index = " + mostLikelyHeightIndex);
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
			System.out.println("Thread : " + threadNumber + " - In segregateBoxesIntoLines : Number of Height Modes = "
					+ numberOfModes + ", which are - " + Arrays.toString(heightModalValues));
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
		int wHistogram[] = new int[numberOfWidthBins];
		for (Rectangle letter : letters) {
			if (letter != null) {
				if (letter.height > (medHeight * 0.5)) {
					int binNumber = ((letter.width % kdew) != 0) ? (letter.width / kdew) : ((letter.width / kdew) - 1);
					if (binNumber < 0) {
						if (debugL <= 2) {
							System.out.println("Thread : " + threadNumber
									+ " - In segregateBoxesIntoLines : Letter width = " + letter.width);
						}
						binNumber = 0;
					}
					++wHistogram[binNumber];
				}
			}
		}
		for (int i = 1; i < (numberOfWidthBins - 1); ++i) {
			if ((wHistogram[i] > 4) && (wHistogram[i] >= wHistogram[i - 1]) && (wHistogram[i] >= wHistogram[i + 1])) {
				widthModes.add(i);
				++numberOfWidthModes;
			}
		}
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber + " - In segregateBoxesIntoLines : wHistogram Array = "
					+ Arrays.toString(wHistogram));
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
			System.out.println("Thread : " + threadNumber + " - In segregateBoxesIntoLines : Most Likely Width Index = "
					+ mostLikelyWidthIndex);
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
			System.out.println("Thread : " + threadNumber + " - In segregateBoxesIntoLines : Width KDE = " + kdew);
			System.out.println(Arrays.toString(wHistogram));
			System.out.println("Thread : " + threadNumber + " - In segregateBoxesIntoLines : Number of Width Modes = "
					+ numberOfWidthModes + ", which are - " + Arrays.toString(widthModalValues));
		}

		if (debugL <= 7) {
			System.out.println("Thread : " + threadNumber + " - In segregateBoxesIntoLines : Likely Height = "
					+ processDataWrapper.kdeData.mostLikelyHeight + "; Likely Width = "
					+ processDataWrapper.kdeData.mostLikelyWidth + "; (Original Width) = " + originalKDEW);
		}

		int minYDifference = 3;
		double medianHeight = processDataWrapper.kdeData.mostLikelyHeight;
		double medianWidth = processDataWrapper.kdeData.mostLikelyWidth;
		double heightCutoff = 0.45;
		double widthCutoff = 0.5; // 0.5

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
							System.out.println("Thread : " + threadNumber
									+ " - In segregateBoxesIntoLines : Ignored small box : " + box);
						}
						continue loop3;
					}
					// ignore the box if its height is too large
					// changed the cutoff from 2.5 to 3.5
					if (box.height > (medianHeight * 3.5)) {
						if (debugL <= 1) {
							System.out.println("Thread : " + threadNumber
									+ " - In segregateBoxesIntoLines : Ignored large box : " + box);
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
			System.out.println("Thread : " + threadNumber
					+ " - In segregateBoxesIntoLines : In segregateBoxesIntoLines : number of lines = " + lines.size());
		}
		for (int k = lines.size() - 1; k >= 0; --k) {
			if ((lines.get(k) == null) || (lines.get(k).size() == 0)) {
				if (debugL <= 2) {
					System.out.println("Thread : " + threadNumber
							+ " - In segregateBoxesIntoLines : About to remove line due to size being 0 : "
							+ lines.get(k));
				}
				lines.remove(k);
			}
		}
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber
					+ " - In segregateBoxesIntoLines : number of lines after removal of empty lines = " + lines.size());
		}
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber
					+ " - In segregateBoxesIntoLines :Before Collections.sort(ArrayList<Rectangle>, ArrayList<Rectangle>) : ");
			int lineNumber = 0;
			for (ArrayList<Rectangle> line : lines) {
				System.out.print(lineNumber++ + ": ");
				System.out.println(line);
			}
		}

		// sort the lines by y-coordinate
		Collections.sort(lines, new Comparator<ArrayList<Rectangle>>() {

			@Override
			public int compare(ArrayList<Rectangle> line1, ArrayList<Rectangle> line2) {

				if (debugLevel <= 1) {
					System.out.println("Comparing - " + line1 + " ; and " + line2);
				}
				if ((line1.size() == 0) && (line2.size() == 0)) {
					return 0;
				}
				if ((line1.size() > 0) && (line2.size() == 0)) {
					return 1;
				}
				if ((line1.size() == 0) && (line2.size() > 0)) {
					return -1;
				}
				if ((line1.get(0).y - line2.get(0).y) < -5) {
					return -1;
				}
				if ((line1.get(0).y - line2.get(0).y) > 5) {
					return 1;
				}
				return ((line1.get(0).x - line2.get(0).x) > 5 ? 1 : ((line1.get(0).x - line2.get(0).x) < -5 ? -1 : 0));
			}
		});

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
		return

		orderAndArrangeLines(removeSmallBoxes(lines, heightStats, widthStats, processDataWrapper, threadNumber, debugL),
				heightStats, widthStats, processDataWrapper, threadNumber, debugL);
	}

	public static ArrayList<ArrayList<Rectangle>> removeSmallBoxes(ArrayList<ArrayList<Rectangle>> inputLines,
			DescriptiveStatistics hStats, DescriptiveStatistics wStats, ProcessDataWrapper processDataWrapper,
			int threadNumber, int debugL) {

		double referenceHeight = processDataWrapper.kdeData.mostLikelyHeight;
		double referenceWidth = processDataWrapper.kdeData.mostLikelyWidth;
		double heightLowerCutoff = 0.35;
		// double heightHigherCutoff = 1.875;
		double heightHigherCutoff = 3.0; // changed to 3.0
		double widthCutoff = 0.2;

		ArrayList<ArrayList<Rectangle>> smallBoxRemovedLines = new ArrayList<>();
		for (ArrayList<Rectangle> line : inputLines) {
			ArrayList<Rectangle> newLine = new ArrayList<>();
			boolean wordsAdded = false;
			for (Rectangle word : line) {

				// changed this part to remove only remove small boxes
				// their is another routine that is called later to remove large boxes
				if ((word.height >= (referenceHeight * heightLowerCutoff))
						&& (word.width >= (referenceWidth * widthCutoff))
				// && (word.height <= (referenceHeight * heightHigherCutoff))
				) {
					newLine.add(word);
					wordsAdded = true;
				} else {
					if (debugL <= 10) {
						System.out.print("Thread : " + threadNumber + " - In removeSmallBoxes : Dropped box - " + word
								+ " because ");
						System.out.print(
								"one of height " + word.height + "<=" + (referenceHeight * heightLowerCutoff) + " OR ");
						System.out.print(" width " + word.width + "<=" + (referenceWidth * widthCutoff) + " OR ");
						System.out.print(
								" height " + word.height + ">=" + (referenceWidth * heightHigherCutoff) + ", where ");
						System.out.println("referenceHeight =" + referenceHeight + ", referenceWidth =" + referenceWidth
								+ ", height cutoffs are [" + heightLowerCutoff + "," + heightHigherCutoff + "]"
								+ " and widthCutoff is " + widthCutoff);
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
			int threadNumber, int debugL) {
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
			System.out.println(
					"Thread : " + threadNumber + " - In orderAndArrangeLines : At 1 in orderAndArrangeLines()");
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
			System.out
					.println("Thread : " + threadNumber + " - In orderAndArrangeLines :At 2 in orderAndArrangeLines()");
			System.out.println(finalLines.toString());
		}

		// now check for boxes within a line whose x-coordinates are close to each
		// other. If they are, then merge them into a single box

		int xTolerance = 4;
		ArrayList<ArrayList<Rectangle>> resultLines1 = new ArrayList<>();
		class MergePair {
			Rectangle firstBox;
			Rectangle secondBox;

			MergePair(Rectangle i, Rectangle j) {
				this.firstBox = i;
				this.secondBox = j;
			}
		}
		ArrayList<MergePair> boxesToBeMerged = new ArrayList<>();

		for (ArrayList<Rectangle> line : finalLines) {
			boxesToBeMerged.clear();
			for (int i = 0; i < line.size(); ++i) {
				Rectangle box = line.get(i);
				for (int j = i + 1; j < line.size(); ++j) {
					Rectangle otherBox = line.get(j);
					if (Math.abs(box.x - otherBox.x) < xTolerance) {
						boxesToBeMerged.add(new MergePair(box, otherBox));
					}
				}
			}
			if (boxesToBeMerged.size() == 0) {
				resultLines1.add(line);
			} else {
				ArrayList<Rectangle> newRectangles = new ArrayList<>();
				Set<Rectangle> rectanglesToBeRemoved = new java.util.HashSet<Rectangle>();
				for (MergePair pair : boxesToBeMerged) {
					int xCoord = Math.min(pair.firstBox.x, pair.secondBox.x);
					int yCoord = Math.min(pair.firstBox.y, pair.secondBox.y);
					int width = Math.max(pair.firstBox.x + pair.firstBox.width, pair.secondBox.x + pair.secondBox.width)
							- xCoord;
					int height = Math.max(pair.firstBox.y + pair.firstBox.height,
							pair.secondBox.y + pair.secondBox.height) - yCoord;
					Rectangle newRectangle = new Rectangle(xCoord, yCoord, width, height);
					newRectangles.add(newRectangle);
					rectanglesToBeRemoved.add(pair.firstBox);
					rectanglesToBeRemoved.add(pair.secondBox);
				}
				for (Rectangle toBeRemoved : rectanglesToBeRemoved) {
					line.remove(toBeRemoved);
				}
				for (Rectangle toBeAdded : newRectangles) {
					line.add(toBeAdded);
				}
				resultLines1.add(line);
			}
		}

		// now sort the boxes in each line based on the x-coordinates of the boxes

		for (ArrayList<Rectangle> line : resultLines1) {
			Collections.sort(line, new Comparator<Rectangle>() {

				@Override
				public int compare(Rectangle letter1, Rectangle letter2) {
					return (letter1.x - letter2.x);
				}
			});
		}

		// now sort the lines based on the y-coordinate and x-coordinate of the first
		// box in that line

		Collections.sort(resultLines1, new Comparator<ArrayList<Rectangle>>() {

			@Override
			public int compare(ArrayList<Rectangle> line1, ArrayList<Rectangle> line2) {
				if ((line1.size() > 0) && (line2.size() > 0)) {
					if ((line1.get(0).y - line2.get(0).y) < -5) {
						return -1;
					}
					if ((line1.get(0).y - line2.get(0).y) > 5) {
						return 1;
					}
				}
				if (line1.size() == 0) {
					return -1;
				}
				if (line2.size() == 0) {
					return 1;
				}
				return (line1.get(0).x - line2.get(0).x);
			}

		});

		return resultLines1;
	}

	public static ArrayList<ArrayList<Rectangle>> reallocateLines(ArrayList<ArrayList<Rectangle>> input,
			ProcessDataWrapper processDataWrapper, int threadNumber, int imageNumber, int debugL) {

		// do reallocation of lines to other lines, figuring out if they are in the same
		// line

		// ArrayList<Integer> medianWidths = new ArrayList<>();
		ArrayList<Integer> medianHeights = new ArrayList<>();
		ArrayList<Integer> topEdge = new ArrayList<>();
		ArrayList<Integer> bottomEdge = new ArrayList<>();

		for (ArrayList<Rectangle> line : input) {
			// DescriptiveStatistics widthStats = new DescriptiveStatistics();
			DescriptiveStatistics heightStats = new DescriptiveStatistics();
			DescriptiveStatistics topEdgeStats = new DescriptiveStatistics();
			DescriptiveStatistics bottomEdgeStats = new DescriptiveStatistics();
			for (Rectangle letter : line) {
				// widthStats.addValue(letter.width);
				heightStats.addValue(letter.height);
				topEdgeStats.addValue(letter.y);
				bottomEdgeStats.addValue(letter.y + letter.height);
			}
			// medianWidths.add((int) widthStats.getPercentile(50));
			medianHeights.add((int) heightStats.getPercentile(50));
			topEdge.add((int) topEdgeStats.getPercentile(50));
			bottomEdge.add((int) bottomEdgeStats.getPercentile(50));
		}

		ArrayList<ArrayList<Rectangle>> resultLines = new ArrayList<>();
		// int topEdgeTolerance = 14;
		ArrayList<Integer> linesAccountedFor = new ArrayList<Integer>();
		// double acceptableGapMultiple = 4.0;
		// double acceptableWidthMultiple = 2.0;
		double acceptableGapAsHeightMultiple = 2.75;
		double overlapCutoff = 0.375; // 33% is a pretty reasonable cutoff; 37.5% is safer

		currentLineLoop: for (int i = 0; i < input.size(); ++i) {
			if (linesAccountedFor.contains(Integer.valueOf(i))) {
				continue currentLineLoop;
			}

			ArrayList<Rectangle> currentLine = input.get(i);

			Collections.sort(currentLine, new Comparator<Rectangle>() {

				@Override
				public int compare(Rectangle letter1, Rectangle letter2) {
					return (letter1.x - letter2.x);
				}
			});

			if (i == (input.size() - 1)) {
				resultLines.add(currentLine);
				break currentLineLoop;
			}
			nextLineLoop: for (int j = i + 1; j < input.size(); ++j) {
				if (linesAccountedFor.contains(Integer.valueOf(j))) {
					continue nextLineLoop;
				}
				ArrayList<Rectangle> nextLine = input.get(j);

				Collections.sort(nextLine, new Comparator<Rectangle>() {

					@Override
					public int compare(Rectangle letter1, Rectangle letter2) {
						return (letter1.x - letter2.x);
					}
				});

				boolean inSameLine = false;
				// find lowest box in current line within tolerance of 5 from box 0
				Rectangle lowestBoxInCurrentLine = currentLine.get(0);
				if (currentLine.get(currentLine.size() - 1).y > lowestBoxInCurrentLine.y) {
					lowestBoxInCurrentLine = currentLine.get(currentLine.size() - 1);
				}
				// for (Rectangle letter : currentLine) {
				// if (((letter.y - currentLine.get(0).y) <= topEdgeTolerance)
				// && (letter.y > lowestBoxInCurrentLine.y)) {
				// lowestBoxInCurrentLine = letter;
				// }
				// }
				Rectangle highestBoxInCurrentLine = currentLine.get(0);
				if (currentLine.get(currentLine.size() - 1).y < highestBoxInCurrentLine.y) {
					highestBoxInCurrentLine = currentLine.get(currentLine.size() - 1);
				}
				// for (Rectangle letter : currentLine) {
				// if (((currentLine.get(0).y - letter.y) <= topEdgeTolerance)
				// && (letter.y < highestBoxInCurrentLine.y)) {
				// highestBoxInCurrentLine = letter;
				// }
				// }

				// find highest box in next line within tolerance of topEdgeTolerance from it's
				// lines box 0
				Rectangle highestBoxInNextLine = nextLine.get(0);
				if (nextLine.get(nextLine.size() - 1).y < highestBoxInNextLine.y) {
					highestBoxInNextLine = nextLine.get(nextLine.size() - 1);
				}

				// for (Rectangle letter : nextLine) {
				// if (((nextLine.get(0).y - letter.y) <= topEdgeTolerance) && (letter.y <
				// highestBoxInNextLine.y)) {
				// highestBoxInNextLine = letter;
				// }
				// }
				Rectangle lowestBoxInNextLine = nextLine.get(0);
				if (nextLine.get(nextLine.size() - 1).y > lowestBoxInNextLine.y) {
					lowestBoxInNextLine = nextLine.get(nextLine.size() - 1);
				}

				// for (Rectangle letter : nextLine) {
				// if (((letter.y - nextLine.get(0).y) <= topEdgeTolerance) && (letter.y >
				// lowestBoxInNextLine.y)) {
				// lowestBoxInNextLine = letter;
				// }
				// }

				int bottomEnd = Math.max(bottomEdge.get(i), bottomEdge.get(j));
				int topEnd = Math.min(topEdge.get(i), topEdge.get(j));
				// overlap = h1 + h2 - d
				int overlap = Math.max(0, (medianHeights.get(i) + medianHeights.get(j)) - (bottomEnd - topEnd));
				double overlapPercent = (overlap * 1.0) / (bottomEnd - topEnd); // = overlap / fullRange

				// if (Math.abs(lowestBoxInCurrentLine.y - highestBoxInNextLine.y) <=
				// topEdgeTolerance) {
				// inSameLine = true;
				// }
				// if (Math.abs(lowestBoxInNextLine.y - highestBoxInCurrentLine.y) <=
				// topEdgeTolerance) {
				// inSameLine = true;
				// }

				if (overlapPercent > overlapCutoff) {
					inSameLine = true;
				}

				if (!inSameLine) {
					if (debugL <= 1) {
						System.out.println(
								"Thread : " + threadNumber + ((imageNumber == -1) ? "" : " Image " + imageNumber)
										+ " - In reallocateLines : Found that line " + (j + 1)
										+ " is not in the same line as line " + (i + 1));
					}
					// if the nextLine is the last line, then add the last line and break out of the
					// loop
					if (j == (input.size() - 1)) {
						resultLines.add(currentLine);
						linesAccountedFor.add(i);
						if (debugL <= 3) {
							System.out.println(
									"Thread : " + threadNumber + ((imageNumber == -1) ? "" : " Image " + imageNumber)
											+ " - In reallocateLines : Adding line " + (i + 1));
						}
					}
					if (i == (input.size() - 2)) { // second last line reached
						resultLines.add(nextLine);
						linesAccountedFor.add(j);
						if (debugL <= 3) {
							System.out.print("Thread : " + threadNumber
									+ ((imageNumber == -1) ? "" : " Image " + imageNumber)
									+ " - In reallocateLines : Reached second last line for comparison and found that last line is in a different line. ");
							System.out.println("In rellocateLines(): Adding line " + (j + 1));
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

				/*
				 * ArrayList<Integer> widths = new ArrayList<>(); ArrayList<Integer> gapMeasures
				 * = new ArrayList<>(); for (int l = 0; l < currentLine.size(); ++l) {
				 * widths.add(currentLine.get(l).width); if (l > 0) {
				 * gapMeasures.add(currentLine.get(l).x - currentLine.get(l - 1).x -
				 * currentLine.get(l - 1).width); } } for (int l = 0; l < nextLine.size(); ++l)
				 * { widths.add(nextLine.get(l).width); if (l > 0) {
				 * gapMeasures.add(nextLine.get(l).x - nextLine.get(l - 1).x - nextLine.get(l -
				 * 1).width); } } Collections.sort(widths); Collections.sort(gapMeasures); int
				 * medianWidth = 0; if (widths.size() > 0) { medianWidth = ((widths.size() % 2)
				 * == 0) ? (int) ((widths.get(widths.size() / 2) + widths.get((widths.size() /
				 * 2) - 1)) / 2) : widths.get(widths.size() / 2); } int medianGap = 0; if
				 * (gapMeasures.size() > 0) { medianGap = ((gapMeasures.size() % 2) == 0) ?
				 * (int) ((gapMeasures.get(gapMeasures.size() / 2) +
				 * gapMeasures.get((gapMeasures.size() / 2) - 1)) / 2) :
				 * gapMeasures.get(gapMeasures.size() / 2); } if (widths.size() > 0) {
				 * widths.remove(widths.size() - 1); } if (widths.size() > 0) {
				 * widths.remove(0); } if (widths.size() > 0) { widths.remove(widths.size() -
				 * 1); } if (gapMeasures.size() > 0) { gapMeasures.remove(gapMeasures.size() -
				 * 1); } if (gapMeasures.size() > 0) { gapMeasures.remove(0); } if
				 * (gapMeasures.size() > 0) { gapMeasures.remove(gapMeasures.size() - 1); } if
				 * (widths.size() >= 1) { medianWidth = ((widths.size() % 2) == 0) ? (int)
				 * ((widths.get(widths.size() / 2) + widths.get((widths.size() / 2) - 1)) / 2) :
				 * widths.get(widths.size() / 2); } if (gapMeasures.size() >= 1) { medianGap =
				 * ((gapMeasures.size() % 2) == 0) ? (int) ((gapMeasures.get(gapMeasures.size()
				 * / 2) + gapMeasures.get((gapMeasures.size() / 2) - 1)) / 2) :
				 * gapMeasures.get(gapMeasures.size() / 2); } double acceptableGap =
				 * Math.min(medianWidth * acceptableWidthMultiple, medianGap *
				 * acceptableGapMultiple);
				 */

				double acceptableGap = (((medianHeights.get(i) + medianHeights.get(j)) * 1.0) / 2)
						* acceptableGapAsHeightMultiple;
				if (debugL <= 2) {
					System.out.println("Thread : " + threadNumber + ((imageNumber == -1) ? "" : " Image " + imageNumber)
							+ " - In reallocateLines : Acceptable Gap for judging if line " + (j + 1)
							+ " can be added to line " + (i + 1) + " is " + acceptableGap);
				}

				// DescriptiveStatistics widthStats = new DescriptiveStatistics();
				// DescriptiveStatistics gapStats = new DescriptiveStatistics();
				// for (int p = 0; p < currentLine.size(); ++p) {
				// widthStats.addValue(currentLine.get(p).width);
				// if (p > 0) {
				// gapStats.addValue(
				// currentLine.get(p).x - currentLine.get(p - 1).width - currentLine.get(p -
				// 1).x);
				// }
				// }
				// for (int q = 0; q < nextLine.size(); ++q) {
				// widthStats.addValue(nextLine.get(q).width);
				// if (q > 0) {
				// gapStats.addValue(nextLine.get(q).x - nextLine.get(q - 1).width -
				// nextLine.get(q - 1).x);
				// }
				// }
				// double widthStatsMean = widthStats.getMean();
				// double widthStatsSD = widthStats.getStandardDeviation();
				// double referenceWidth = processDataWrapper.kdeData.mostLikelyWidth;
				// double referenceWidth = widthStats.getPercentile(50);
				// double referenceGap = gapStats.getPercentile(50);
				// If the widthStats has a low variance, then it's likely that these are
				// individual characters with a reasonably uniform width
				// Hence, this width should be used instead of the mostLikelyWidth
				// if (((widthStatsSD / widthStatsMean) < 0.5)
				// && ((widthStatsMean / processDataWrapper.kdeData.mostLikelyWidth) < 4.0)) {
				// referenceWidth = widthStatsMean;
				// }
				// if (((widthStatsSD / widthStatsMean) < 0.5) && ((widthStatsMean /
				// referenceWidth) < 2.0)) {
				// referenceWidth = widthStatsMean;
				// }
				// if (((widthStatsSD / widthStatsMean) < 0.5) && ((widthStatsMean /
				// referenceWidth) < 4.0)) {
				// referenceWidth = widthStatsMean;
				// }
				// if (debugL <= 3) {
				// System.out.println("In rellocateLines(): Reference Width For checking fitment
				// of line " + (j + 1)
				// + " in line " + (i + 1) + " is " + referenceWidth + ". widthStatsMean = " +
				// widthStatsMean
				// + ", and widthStatsSD = " + widthStatsSD + " ratio = " + (widthStatsSD /
				// widthStatsMean));
				// }
				boolean nextLineCanBeAddedToCurrent = false;
				boxClosenessCheckLoop: for (int p = 0; p < currentLine.size(); ++p) {
					Rectangle letterInCurrentLine = currentLine.get(p);
					// int allowableGap = Math.max((int) (referenceWidth * acceptableWidthMultiple),
					// (int) (referenceGap * acceptableGapMultiple));
					double allowableGap = acceptableGap;
					for (int q = 0; q < nextLine.size(); ++q) {
						Rectangle letterInNextLine = nextLine.get(q);
						if (Math.abs((letterInCurrentLine.x + letterInCurrentLine.width)
								- letterInNextLine.x) < (allowableGap)) {
							nextLineCanBeAddedToCurrent = true;
							break boxClosenessCheckLoop;
						}
						if (Math.abs((letterInNextLine.x + letterInNextLine.width)
								- letterInCurrentLine.x) < (allowableGap)) {
							nextLineCanBeAddedToCurrent = true;
							break boxClosenessCheckLoop;
						}
					}
				}
				if (nextLineCanBeAddedToCurrent) {
					if (debugL <= 3) {
						System.out.println(
								"Thread : " + threadNumber + ((imageNumber == -1) ? "" : " Image " + imageNumber)
										+ " - In reallocateLines : Determined that line " + (j + 1)
										+ " can be added to line " + (i + 1));
						System.out.println(
								"Thread : " + threadNumber + ((imageNumber == -1) ? "" : " Image " + imageNumber)
										+ " - In reallocateLines : Current Line before addition is : " + currentLine);
					}
					for (int q = 0; q < nextLine.size(); ++q) {
						currentLine.add(nextLine.get(q));
					}
					linesAccountedFor.add(j);
					if (debugL <= 3) {
						System.out.println(
								"Thread : " + threadNumber + ((imageNumber == -1) ? "" : " Image " + imageNumber)
										+ " - In reallocateLines : Current Line after addition is : " + currentLine);
					}
					if (j == (input.size() - 1)) {
						resultLines.add(currentLine);
						linesAccountedFor.add(i);
						// break currentLineLoop;
						continue currentLineLoop;
					}
					continue nextLineLoop;
				}

				if (j == (input.size() - 1)) {
					resultLines.add(currentLine);
					linesAccountedFor.add(i);
					continue currentLineLoop;
				}
			}
		}

		if (debugL <= 3) {
			System.out.println("Thread : " + threadNumber + ((imageNumber == -1) ? "" : " Image " + imageNumber)
					+ " - In reallocateLines : RESULT LINES IS - " + resultLines);
		}

		return resultLines;

	}

	public static ArrayList<ArrayList<Rectangle>> reallocateLinesAgain(ArrayList<ArrayList<Rectangle>> input,
			ProcessDataWrapper processDataWrapper, int threadNumber, int debugL) {

		// do reallocation of lines to other lines again, figuring out if they are in
		// the same line

		ArrayList<ArrayList<Rectangle>> resultLines = new ArrayList<>();
		// int topEdgeTolerance = 7;
		double heightOverlapCutoff = 0.7; // At least 70% of one line needs to overlap with the other line
		ArrayList<Integer> linesAccountedFor = new ArrayList<Integer>();
		ArrayList<Integer> topEdge = new ArrayList<>();
		ArrayList<Integer> bottomEdge = new ArrayList<>();
		ArrayList<Integer> gaps = new ArrayList<>();
		ArrayList<Integer> heights = new ArrayList<>();

		for (ArrayList<Rectangle> line : input) {
			DescriptiveStatistics topEdgeStats = new DescriptiveStatistics();
			DescriptiveStatistics bottomEdgeStats = new DescriptiveStatistics();
			DescriptiveStatistics gapStats = new DescriptiveStatistics();
			DescriptiveStatistics heightStats = new DescriptiveStatistics();
			for (int i = 0; i < line.size(); ++i) {
				topEdgeStats.addValue(line.get(i).y);
				heightStats.addValue(line.get(i).height);
				if (i > 0) {
					gapStats.addValue(line.get(i).x - line.get(i - 1).x - line.get(i - 1).width);
				}
				bottomEdgeStats.addValue(line.get(i).y + line.get(i).height);
			}
			topEdge.add((int) topEdgeStats.getPercentile(50));
			bottomEdge.add((int) bottomEdgeStats.getPercentile(50));
			gaps.add((int) gapStats.getPercentile(50));
			heights.add((int) heightStats.getPercentile(50));
		}

		if (debugL <= 2) {
			System.out
					.println("Thread : " + threadNumber + " - In reallocateLinesAgain() : topEdgeStats are " + topEdge);
			System.out.println(
					"Thread : " + threadNumber + " - In reallocateLinesAgain() : bottomEdgeStats are " + bottomEdge);
			System.out.println("Thread : " + threadNumber + " - In reallocateLinesAgain() : gapStats are " + gaps);
			System.out
					.println("Thread : " + threadNumber + " - In reallocateLinesAgain() : heightStats are " + heights);
		}

		currentLineLoop: for (int i = 0; i < input.size(); ++i) {
			if (debugL <= 2) {
				System.out.println(
						"Thread : " + threadNumber + " - In reallocateLinesAgain() : Evaluating line " + (i + 1));
			}
			if (linesAccountedFor.contains(Integer.valueOf(i))) {
				if (debugL <= 2) {
					System.out.println("Thread : " + threadNumber + " - In reallocateLinesAgain() : Skipping line "
							+ (i + 1) + " as it is already accounted for");
				}
				continue currentLineLoop;
			}
			// double acceptableGapMultiple = 3.0;
			double acceptableGapAsHeightMultiple = 4.0;
			if (CheckProductProperties.productIsGiven) {
				acceptableGapAsHeightMultiple = 5.5;
			}
			ArrayList<Rectangle> currentLine = input.get(i);
			Collections.sort(currentLine, new Comparator<Rectangle>() {
				@Override
				public int compare(Rectangle letter1, Rectangle letter2) {
					return (letter1.x - letter2.x);
				}
			});

			if (i == (input.size() - 1)) {
				if (debugL <= 2) {
					System.out.println(
							"Thread : " + threadNumber + " - In reallocateLinesAgain() : Adding line " + (i + 1));
				}
				resultLines.add(currentLine);
				break currentLineLoop;
			}
			nextLineLoop: for (int j = i + 1; j < input.size(); ++j) {
				// if the last line is the "nextLine", then the currentLine anyhow needs to be
				// added
				if (j == (input.size() - 1)) {
					resultLines.add(currentLine);
					linesAccountedFor.add(i);
					if (debugL <= 3) {
						System.out.println(
								"Thread : " + threadNumber + " - In reallocateLinesAgain() : Adding line " + (i + 1));
					}
				}
				if (linesAccountedFor.contains(Integer.valueOf(j))) {
					if (debugL <= 2) {
						System.out
								.println("Thread : " + threadNumber + " - In reallocateLinesAgain() : Skipping nextine "
										+ (j + 1) + " as it is already accounted for");
					}
					continue nextLineLoop;
				}
				int topOfRange = Math.min(topEdge.get(i), topEdge.get(j));
				int bottomOfRange = Math.max(bottomEdge.get(i), bottomEdge.get(j));
				int overlap = Math.max(0, (((bottomEdge.get(i) - topEdge.get(i)) + (bottomEdge.get(j) - topEdge.get(j)))
						- (bottomOfRange - topOfRange))); // overlap = Math.max(h1 + h2 - d,0)
				double overlapPercent = (overlap * 1.0) / (bottomOfRange - topOfRange);
				overlapPercent = Math.max(overlapPercent, (overlap * 1.0) / (bottomEdge.get(i) - topEdge.get(i)));
				overlapPercent = Math.max(overlapPercent, (overlap * 1.0) / (bottomEdge.get(j) - topEdge.get(j)));
				// if (Math.abs(topEdge.get(i) - topEdge.get(j)) > topEdgeTolerance) {
				// if (debugL <= 2) {
				// System.out.println("In reallocateLinesAgain(): Skipping nextLine " + (j + 1)
				// + " as it is not in the same y vicinity");
				// }
				// continue nextLineLoop;
				// }
				if (overlapPercent < heightOverlapCutoff) {
					if (debugL <= 2) {
						System.out.println("Thread : " + threadNumber
								+ " - In reallocateLinesAgain() : Skipping nextLine " + (j + 1) + " as overlap is "
								+ overlapPercent + " between lines " + (i + 1) + " and " + (j + 1));
					}
					continue nextLineLoop;
				} else {
					if (debugL <= 2) {
						System.out.println("Thread : " + threadNumber + " - In reallocateLinesAgain() : Overlap of "
								+ overlapPercent + " between lines " + (i + 1) + " and " + (j + 1));
					}
				}
				if (debugL <= 2) {
					System.out.println("Thread : " + threadNumber + " - In reallocateLinesAgain() : nextLine " + (j + 1)
							+ " is not excluded. Hence, evaluating gap for inclusion in line " + (i + 1));
				}
				ArrayList<Rectangle> nextLine = input.get(j);
				Collections.sort(nextLine, new Comparator<Rectangle>() {
					@Override
					public int compare(Rectangle letter1, Rectangle letter2) {
						return (letter1.x - letter2.x);
					}
				});

				boolean inSameLine = false;
				// find last box in current line within tolerance of 5 from box 0
				// Rectangle lastBoxInCurrentLine = currentLine.get(0);
				Rectangle lastBoxInCurrentLine = currentLine.get(currentLine.size() - 1);
				Rectangle firstBoxInCurrentLine = currentLine.get(0);
				// DescriptiveStatistics widthStats = new DescriptiveStatistics();

				/*
				 * for (int k = 1; k < currentLine.size(); ++k) { Rectangle letter =
				 * currentLine.get(k); // widthStats.addValue((letter.x - currentLine.get(k -
				 * 1).x) + currentLine.get(k // - 1).width); if ((letter.x -
				 * lastBoxInCurrentLine.x) > 0) { lastBoxInCurrentLine = letter; } }
				 *
				 */
				if (debugL <= 2) {
					System.out.println("Thread : " + threadNumber + " - In reallocateLinesAgain() : all boxes in line "
							+ (i + 1) + " are " + currentLine);
					System.out.println("Thread : " + threadNumber + " - In reallocateLinesAgain() : last box in line "
							+ (i + 1) + " is " + lastBoxInCurrentLine);
				}
				// find first box in next line
				Rectangle firstBoxInNextLine = nextLine.get(0);
				Rectangle lastBoxInNextLine = nextLine.get(nextLine.size() - 1);

				/*
				 * for (int k = 1; k < nextLine.size(); ++k) { Rectangle letter =
				 * nextLine.get(k); // widthStats.addValue((letter.x - nextLine.get(k - 1).x) +
				 * nextLine.get(k - // 1).width); if ((letter.x - firstBoxInNextLine.x) < 0) {
				 * firstBoxInNextLine = letter; } }
				 *
				 */
				if (debugL <= 2) {
					System.out.println("Thread : " + threadNumber + " - In reallocateLinesAgain() : all boxes in line "
							+ (j + 1) + " are " + nextLine);
					System.out.println(
							"Thread : " + threadNumber + " - In reallocateLinesAgain() : first box in nextLine "
									+ (j + 1) + " is " + firstBoxInNextLine);
				}

				// double medianWidth = widthStats.getPercentile(50);
				// double medianGap = ((gaps.get(i) + gaps.get(j)) * 1.0) / 2;

				// if (Math.abs((lastBoxInCurrentLine.x + lastBoxInCurrentLine.width)
				// - firstBoxInNextLine.x) <= (acceptableGapMultiple * medianWidth)) {
				// if (debugL <= 2) {
				// System.out.println("In reallocateLinesAgain(): acceptableGap in line " + (i +
				// 1) + " = "
				// + (acceptableGapMultiple * medianWidth));
				// }
				// inSameLine = true;
				// }

				double averageHeight = ((heights.get(i) + heights.get(j)) * 1.0) / 2;

				if (Math.abs((lastBoxInCurrentLine.x + lastBoxInCurrentLine.width)
						// - firstBoxInNextLine.x) <= (acceptableGapMultiple * medianGap)) { ***Removed
						// gapMultiple as it is unreliable. Height multiple is a more consistent
						- firstBoxInNextLine.x) <= (acceptableGapAsHeightMultiple * averageHeight)) {
					if (debugL <= 2) {
						System.out.println("Thread : " + threadNumber
								+ " - In reallocateLinesAgain() : acceptableGap in line " + (i + 1) + " and line "
								+ (j + 1) + " is " + (acceptableGapAsHeightMultiple * averageHeight));
					}
					inSameLine = true;
				}

				// added this block of code
				// ***********
				if (Math.abs((lastBoxInNextLine.x + lastBoxInNextLine.width)
						// - firstBoxInNextLine.x) <= (acceptableGapMultiple * medianGap)) { ***Removed
						// gapMultiple as it is unreliable. Height multiple is a more consistent
						- firstBoxInCurrentLine.x) <= (acceptableGapAsHeightMultiple * averageHeight)) {
					if (debugL <= 2) {
						System.out.println("Thread : " + threadNumber
								+ " - In reallocateLinesAgain() : acceptableGap in line " + (i + 1) + " and line "
								+ (j + 1) + " is " + (acceptableGapAsHeightMultiple * averageHeight));
					}
					inSameLine = true;
				}
				// ***********

				if (!inSameLine) {
					if (debugL <= 1) {
						System.out
								.println("Thread : " + threadNumber + " - In reallocateLinesAgain() : Found that line "
										+ (i + 1) + " is too far from the next line " + (j + 1));
					}
					// if the nextLine is the last line, then add the last line and break out of the
					// loop
					if (i == (input.size() - 2)) { // currentLine is second last line reached
						// Note: current line does not need to be added as it is added as soon as last
						// line is being evaluated
						resultLines.add(nextLine);
						linesAccountedFor.add(j); // not needed as both evaluation loops are over, but kept for
													// completeness
						if (debugL <= 3) {
							System.out.print("Thread : " + threadNumber
									+ " - In reallocateLinesAgain() : Reached second last line for comparison and found that last line is in a different line. ");
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

				if (debugL <= 3) {
					System.out
							.println("Thread : " + threadNumber + " - In reallocateLinesAgain() : Determined that line "
									+ (j + 1) + " can be added to line " + (i + 1));
					System.out.println("Thread : " + threadNumber + " - In reallocateLinesAgain() : Current Line "
							+ (i + 1) + " before addition is : " + currentLine);
				}
				for (int q = 0; q < nextLine.size(); ++q) {
					currentLine.add(nextLine.get(q));
				}
				linesAccountedFor.add(j);
				if (debugL <= 3) {
					System.out.println("Thread : " + threadNumber + " - In reallocateLinesAgain() : Current Line "
							+ (i + 1) + " before addition is : " + currentLine);
				}
				/*
				 * if (j == (input.size() - 1)) { resultLines.add(currentLine);
				 * linesAccountedFor.add(i); // break currentLineLoop; continue currentLineLoop;
				 * }
				 */
				continue nextLineLoop;
			}
		}

		// The ABOVE code MERGES 2 or more lines into 1.
		// The code BELOW SEPARATES 1 line into 2 or more lines

		// Do a splitting of lines if there is an inordinate gap between words
		// in a line. Split only those lines which are longer than 8 words in length.
		// Since it is difficult to know at this juncture how many bounding boxes
		// correspond to 8 words,
		// making a guess that the routine should be executed only if there are at least
		// 7 boxes in the line
		// This is to ensure that the bounding boxes of a typical price line of Rs112000
		// (Rs.1120.00) is not split up

		// Also, split lines that have a considerable gap in between and the number of
		// words on either side is <= 2. Determine number of words as a multiple of
		// width / (referenceWidth)

		ArrayList<ArrayList<Rectangle>> interimLines1 = new ArrayList<>();
		// double acceptableHeightMultiple = 3.25;
		double acceptableHeightMultiple = 4.25; // height multiple seems to be more consistent than gap and width
												// multiples
		// double acceptableHeightMultipleForEdgeWords = 2.25;
		double acceptableHeightMultipleForEdgeWords = 3.25;
		if (CheckProductProperties.productIsGiven) {
			acceptableHeightMultiple = 5.5;
			acceptableHeightMultipleForEdgeWords = 4.5;
		}
		int maximumNoOfEdgeWords = 2;
		int minimumLineLengthForSplitting = 9;
		int ln = 0;
		outerloop: for (ArrayList<Rectangle> line : resultLines) {
			++ln;
			Collections.sort(line, new Comparator<Rectangle>() {
				@Override
				public int compare(Rectangle letter1, Rectangle letter2) {
					return (letter1.x - letter2.x);
				}
			});
			if (debugL <= 2) {
				System.out.println("Thread : " + threadNumber + " - In reallocateLinesAgain() : size of line " + ln
						+ " is " + line.size() + " which is to be compared with "
						+ (minimumLineLengthForSplitting - maximumNoOfEdgeWords));
			}
			if (line.size() < (minimumLineLengthForSplitting - maximumNoOfEdgeWords)) {
				interimLines1.add(line);
				continue outerloop;
			}
			heights.clear();
			ArrayList<Integer> gapStats = new ArrayList<>();
			for (int i = 0; i < line.size(); ++i) {
				heights.add(line.get(i).height);
				if (i > 0) {
					gapStats.add(line.get(i).x - line.get(i - 1).x - line.get(i - 1).width);
				}
			}
			Collections.sort(heights);
			double medianHeight = ((heights.size() % 2) == 0)
					? ((heights.get(heights.size() / 2) + heights.get((heights.size() / 2) - 1)) / 2)
					: heights.get(heights.size() / 2);

			Collections.sort(gapStats);
			if (gapStats.size() > 1) {
				gapStats.remove(gapStats.size() - 1);
			}
			if (gapStats.size() > 1) {
				gapStats.remove(0);
			}
			if (gapStats.size() > 1) {
				gapStats.remove(gapStats.size() - 1);
			}
			if (gapStats.size() > 1) {
				gapStats.remove(0);
			}
			double medianGap = ((gapStats.size() % 2) == 0)
					? ((gapStats.get(gapStats.size() / 2) + gapStats.get((gapStats.size() / 2) - 1)) / 2)
					: gapStats.get(gapStats.size() / 2);

			// ArrayList<Integer> widths = new ArrayList<>();
			// ArrayList<Integer> gapMeasures = new ArrayList<>();
			// for (int i = 0; i < line.size(); ++i) {
			// widths.add(line.get(i).width);
			// if (i > 0) {
			// gapMeasures.add(line.get(i).x - line.get(i - 1).x - line.get(i - 1).width);
			// }
			// }
			// Collections.sort(widths);
			// Collections.sort(gapMeasures);
			// int medianWidth = 0;
			// if (widths.size() > 0) {
			// medianWidth = ((widths.size() % 2) == 0)
			// ? (int) ((widths.get(widths.size() / 2) + widths.get((widths.size() / 2) -
			// 1)) / 2)
			// : widths.get(widths.size() / 2);
			// }
			// int medianGap = 0;
			// if (gapMeasures.size() > 0) {
			// medianGap = ((gapMeasures.size() % 2) == 0)
			// ? (int) ((gapMeasures.get(gapMeasures.size() / 2)
			// + gapMeasures.get((gapMeasures.size() / 2) - 1)) / 2)
			// : gapMeasures.get(gapMeasures.size() / 2);
			// }
			// if (widths.size() > 0) {
			// widths.remove(widths.size() - 1);
			// }
			// if (widths.size() > 0) {
			// widths.remove(0);
			// }
			// if (widths.size() > 0) {
			// widths.remove(widths.size() - 1);
			// }
			// if (gapMeasures.size() > 0) {
			// gapMeasures.remove(gapMeasures.size() - 1);
			// }
			// if (gapMeasures.size() > 0) {
			// gapMeasures.remove(0);
			// }
			// if (gapMeasures.size() > 0) {
			// gapMeasures.remove(gapMeasures.size() - 1);
			// }
			// if (widths.size() >= 1) {
			// medianWidth = ((widths.size() % 2) == 0)
			// ? (int) ((widths.get(widths.size() / 2) + widths.get((widths.size() / 2) -
			// 1)) / 2)
			// : widths.get(widths.size() / 2);
			// }
			// if (gapMeasures.size() >= 1) {
			// medianGap = ((gapMeasures.size() % 2) == 0)
			// ? (int) ((gapMeasures.get(gapMeasures.size() / 2)
			// + gapMeasures.get((gapMeasures.size() / 2) - 1)) / 2)
			// : gapMeasures.get(gapMeasures.size() / 2);
			// }
			// double acceptableGap = Math.max(medianWidth * acceptableWidthMultiple,
			// medianGap * acceptableGapMultiple);
			double acceptableGap = medianHeight * acceptableHeightMultiple;
			double acceptableEdgeGap = medianHeight * acceptableHeightMultipleForEdgeWords;

			double acceptableGapMultiple = 6;
			// empirically seen
			double acceptableGapAsMultipleOfGaps = Math.max(acceptableGapMultiple * medianGap, 2.0 * medianHeight);

			if (debugL <= 2) {
				System.out
						.println("Thread : " + threadNumber + " - In reallocateLinesAgain() : Acceptable Gap for line "
								+ ln + " by heightMultiple is " + acceptableGap);
				System.out
						.println("Thread : " + threadNumber + " - In reallocateLinesAgain() : Acceptable Gap for line "
								+ ln + " by gapMultiple is " + acceptableGapAsMultipleOfGaps);
			}
			ArrayList<Rectangle> newLine = new ArrayList<>();
			boolean firstWord = true;
			innerloop: for (int i = 0; i < line.size(); ++i) {
				if (firstWord) {
					newLine.add(line.get(i));
					firstWord = false;
					continue innerloop;
				}
				int currentGap = line.get(i).x - line.get(i - 1).x - line.get(i - 1).width;
				if (((currentGap > acceptableGap) || (currentGap > acceptableGapAsMultipleOfGaps))
						&& ((line.size() - i) >= (minimumLineLengthForSplitting - 1))) {
					if (debugL <= 2) {
						System.out.println(
								"Thread : " + threadNumber + " - In reallocateLinesAgain() : Splitting line " + ln);
					}
					interimLines1.add(newLine);
					newLine = new ArrayList<>();
				} else {
					if (((currentGap > acceptableEdgeGap) || (currentGap > acceptableGapAsMultipleOfGaps))
							&& (((line.size() - i) <= maximumNoOfEdgeWords) || (i < maximumNoOfEdgeWords))) {
						if (debugL <= 2) {
							System.out.println(
									"Thread : " + threadNumber + " - In reallocateLinesAgain() : Splitting line " + ln);
						}
						interimLines1.add(newLine);
						newLine = new ArrayList<>();
					}
				}
				newLine.add(line.get(i));
			}
			interimLines1.add(newLine);
		}

		if (debugL <= 1)

		{
			System.out.println(
					"Thread : " + threadNumber + " - In reallocateLinesAgain() : At 4 in reallocateLinesAgain()");
			System.out.println(interimLines1.toString());
		}

		// **************************

		// do a round of cleanup to eliminate small length lines <= 2 that are unlikely
		// to
		// have meaningful characters

		ArrayList<ArrayList<Rectangle>> resultLines1 = new ArrayList<>();

		for (ArrayList<Rectangle> line : interimLines1) {
			if (line.size() <= 2) {
				boolean markedForDeletion = false;
				double averageHeight = 0.0;
				double totalWidth = 0.0;
				for (Rectangle letter : line) {
					averageHeight += letter.height;
					totalWidth += letter.width;
				}
				averageHeight = averageHeight / line.size();
				double likelyWidthOfAWord = (averageHeight * idealCharWidth) / idealCharHeight;
				int likelyNumberOfWords = (int) Math.round(totalWidth / likelyWidthOfAWord);
				// for (Rectangle letter : line) {
				/*
				 * if ((letter.width < (processDataWrapper.kdeData.mostLikelyWidth * 0.5)) ||
				 * (letter.height < (processDataWrapper.kdeData.mostLikelyHeight * 0.45))) { if
				 * (debugL <= 2) { System.out.println("Line marked for deletion : " + line); }
				 * markedForDeletion = markedForDeletion && true; } else { if (debugL <= 2) {
				 * System.out.println("Line unmarked (removed from marking) for deletion : " +
				 * line); } markedForDeletion = markedForDeletion && false; }
				 */
				if (likelyNumberOfWords <= 2) {
					markedForDeletion = true;
				}
				// }
				if (!markedForDeletion) {
					resultLines1.add(line);
				} else {
					if (debugL <= 10) {
						System.out.println("Thread : " + threadNumber
								+ " - In reallocateLinesAgain() : Dropped boxes in line - " + line);
					}
				}
			} else {
				resultLines1.add(line);
			}
		}

		if (debugL <= 1) {
			System.out.println(
					"Thread : " + threadNumber + " - In reallocateLinesAgain() : At 3 in reallocateLinesAgain()");
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
				} else {
					if (debugL <= 10) {
						System.out.println("Thread : " + threadNumber
								+ " - In reallocateLinesAgain() : Dropped boxes in line - " + line);
					}
				}
			} else {
				resultLines2.add(line);
			}
		}

		// **************************

		if (debugL <= 3) {
			System.out.println(
					"Thread : " + threadNumber + " - In reallocateLinesAgain() : FINAL LINES IS - " + resultLines2);
		}

		return resultLines2;

	}

	public static ArrayList<ArrayList<Rectangle>> splitLinesByYCoordinate(ArrayList<ArrayList<Rectangle>> input,
			ProcessDataWrapper processDataWrapper, int threadNumber, int debugL) {

		// split long lines into other lines, figuring out if the boxes in that long
		// line should remain in the same
		// line

		// sort the boxes in input based on the x-coordinates of the boxes

		for (ArrayList<Rectangle> line : input) {
			Collections.sort(line, new Comparator<Rectangle>() {

				@Override
				public int compare(Rectangle letter1, Rectangle letter2) {
					return (letter1.x - letter2.x);
				}
			});
		}

		// sort the input lines based on the y-coordinate and x-coordinate of the first
		// box in that line

		Collections.sort(input, new Comparator<ArrayList<Rectangle>>() {

			@Override
			public int compare(ArrayList<Rectangle> line1, ArrayList<Rectangle> line2) {
				if ((line1.size() > 0) && (line2.size() > 0)) {
					if ((line1.get(0).y - line2.get(0).y) < -5) {
						return -1;
					}
					if ((line1.get(0).y - line2.get(0).y) > 5) {
						return 1;
					}
				}
				if (line1.size() == 0) {
					return -1;
				}
				if (line2.size() == 0) {
					return 1;
				}
				return (line1.get(0).x - line2.get(0).x);
			}
		});

		ArrayList<ArrayList<Rectangle>> resultLines = new ArrayList<>();

		for (int i = 0; i < input.size(); ++i) {
			// int numberOfLinesToBeSplitInto = 1;
			ArrayList<Rectangle> currentLine = input.get(i);
			ArrayList<Rectangle> referenceLetters = new ArrayList<>();
			ArrayList<ArrayList<Rectangle>> splitLines = new ArrayList<>();
			innerloop: for (Rectangle letter : currentLine) {
				if (referenceLetters.size() == 0) {
					referenceLetters.add(letter);
					ArrayList<Rectangle> newLine = new ArrayList<>();
					newLine.add(letter);
					splitLines.add(newLine);
					continue innerloop;
				}
				for (int j = 0; j < referenceLetters.size(); ++j) {
					Rectangle referenceLetter = referenceLetters.get(j);
					double totalRange = Math.max(referenceLetter.y + referenceLetter.height, letter.y + letter.height)
							- Math.min(referenceLetter.y, letter.y);
					double sumOfRanges = (referenceLetter.height) + (letter.height);
					if (sumOfRanges > totalRange) {
						splitLines.get(j).add(letter);
						continue innerloop;
					}
				}
				referenceLetters.add(letter);
				ArrayList<Rectangle> newLine = new ArrayList<>();
				newLine.add(letter);
				splitLines.add(newLine);
			}
			for (ArrayList<Rectangle> line : splitLines) {
				resultLines.add(line);
			}
		}

		// now sort the boxes in each line based on the x-coordinates of the boxes

		for (ArrayList<Rectangle> line : resultLines) {
			Collections.sort(line, new Comparator<Rectangle>() {

				@Override
				public int compare(Rectangle letter1, Rectangle letter2) {
					return (letter1.x - letter2.x);
				}
			});
		}

		// now sort the lines based on the y-coordinate and x-coordinate of the first
		// box in that line

		Collections.sort(resultLines, new Comparator<ArrayList<Rectangle>>() {

			@Override
			public int compare(ArrayList<Rectangle> line1, ArrayList<Rectangle> line2) {
				if ((line1.size() > 0) && (line2.size() > 0)) {
					if ((line1.get(0).y - line2.get(0).y) < -5) {
						return -1;
					}
					if ((line1.get(0).y - line2.get(0).y) > 5) {
						return 1;
					}
				}
				if (line1.size() == 0) {
					return -1;
				}
				if (line2.size() == 0) {
					return 1;
				}
				return (line1.get(0).x - line2.get(0).x);
			}
		});

		if (debugL <= 3) {
			System.out.println(
					"Thread : " + threadNumber + " - In splitLinesByYCoordinate() : RESULT LINES IS - " + resultLines);
		}

		return resultLines;

	}

	public static ArrayList<ArrayList<Rectangle>> reallocateVerticalBoxes(ArrayList<ArrayList<Rectangle>> input,
			ProcessDataWrapper processDataWrapper, int threadNumber, int debugL) {

		// on rare occasions, boxes that have been vertically split are not aligned with
		// their correct lines. This
		// routine corrects that anomaly

		// Assumption : input lines are sorted by x and y coordinates

		ArrayList<Rectangle> anomalyBoxes = new ArrayList<>();
		double desiredMinimumOverlap = 0.6; // boxes of normal size
		double smallBoxHeightCutoff = 0.45;
		// double smallBoxWidthCutoff = 0.45;
		double subsumedCutoff = 0.9;

		// iterate through each line
		// if a box is out of place wrt its previous 1 / 2 / 3 and/or its next 1 / 2
		// boxes, then remove it and dump into the anomaly box arraylist
		for (int i = 0; i < input.size(); ++i) {
			if (debugL <= 2) {
				System.out.println(
						"Thread : " + threadNumber + " - In reallocateVerticalBoxes() : Evaluating line - " + (i + 1));
			}

			ArrayList<Rectangle> currentLine = input.get(i);
			if (currentLine.size() <= 3) {
				continue;
			}
			DescriptiveStatistics heightStats = new DescriptiveStatistics();
			for (Rectangle letter : currentLine) {
				heightStats.addValue(letter.height);
			}
			double medianHeight = heightStats.getPercentile(50);
			if (debugL <= 2) {
				System.out.println(
						"Thread : " + threadNumber + " - In reallocateVerticalBoxes() : Median height of line - "
								+ (i + 1) + " is " + medianHeight);
			}
			if (debugL <= 2) {
				System.out.println(
						"Thread : " + threadNumber + " - In reallocateVerticalBoxes() : Diving into line - " + (i + 1));
			}
			// int letterNumber = -1;
			innerloop: for (int j = currentLine.size() - 1; j >= 0; --j) {
				Rectangle letter = currentLine.get(j);
				// ++letterNumber;
				int letterNumber = j;
				if (debugL <= 2) {
					System.out.println(
							"   Thread : " + threadNumber + " - In reallocateVerticalBoxes() : Evaluating letter - "
									+ (j + 1) + " which is " + letter);
				}
				// ignore small boxes
				if (letter.height < (smallBoxHeightCutoff * medianHeight)) {
					continue innerloop;
				}
				if (letter.width < ((smallBoxHeightCutoff * medianHeight * idealCharWidth) / idealCharHeight)) {
					continue innerloop;
				}
				// check overlap with neighbouring letters
				int neighbouringLetterNumber = -1;
				for (Rectangle neighbouringLetter : currentLine) {
					++neighbouringLetterNumber;
					// ignore small boxes for comparison
					if (neighbouringLetter.height < (smallBoxHeightCutoff * medianHeight)) {
						continue innerloop;
					}
					if (neighbouringLetter.width < ((smallBoxHeightCutoff * medianHeight * idealCharWidth)
							/ idealCharHeight)) {
						continue innerloop;
					}
					// check if it's a neighbouring letter
					if ((Math.abs(neighbouringLetterNumber - letterNumber) <= 3)
							&& (neighbouringLetterNumber != letterNumber)) {
						// ...then, calculate a) overlap, and b) if either of the boxes is almost fully
						// subsumed within the other box
						double totalRange = Math.max(letter.y + letter.height,
								neighbouringLetter.y + neighbouringLetter.height)
								- Math.min(neighbouringLetter.y, letter.y);
						double sumOfRanges = (neighbouringLetter.height) + (letter.height);
						double overlap = Math.max(0, sumOfRanges - totalRange);
						boolean subsumed = ((overlap / letter.height) > subsumedCutoff)
								|| ((overlap / neighbouringLetter.height) > subsumedCutoff);
						if (((overlap / totalRange) > desiredMinimumOverlap) || subsumed) {
							// found at least one letter with which there is sufficient overlap
							if (debugL <= 2) {
								System.out.println("      Thread : " + threadNumber
										+ " - In reallocateVerticalBoxes() : Found overlap of letter - " + (j + 1)
										+ " with letter " + (neighbouringLetterNumber + 1) + " which is "
										+ neighbouringLetter + " because overlap % = " + (overlap / totalRange)
										+ " and subsumed = " + subsumed);
							}
							continue innerloop;
						}
					}
				}
				// if the code reaches here, it means there is no overlap for the letter with
				// any neighbouring letters
				anomalyBoxes.add(letter);
				currentLine.remove(letter);
				if (debugL <= 2) {
					System.out.println("Thread : " + threadNumber
							+ " - In reallocateVerticalBoxes() : removed letter - " + letter + " from line " + (i + 1));
				}

			}
		}

		// now, iterate through the letters in anomalyBoxes and find the appropriate
		// line where they can be slotted

		anomalyLoop: for (Rectangle anomalyBox : anomalyBoxes) {
			for (ArrayList<Rectangle> line : input) {
				for (Rectangle letter : line) {
					double totalRange = Math.max(letter.y + letter.height, anomalyBox.y + anomalyBox.height)
							- Math.min(anomalyBox.y, letter.y);
					double sumOfRanges = (anomalyBox.height) + (letter.height);
					double overlap = Math.max(0, sumOfRanges - totalRange);
					if ((overlap / totalRange) > desiredMinimumOverlap) {
						// found at least one letter with which there is sufficient overlap
						line.add(anomalyBox);
						continue anomalyLoop;
					}
				}
			}
		}

		// now sort the boxes in each line based on the x-coordinates of the boxes

		for (ArrayList<Rectangle> line : input) {
			Collections.sort(line, new Comparator<Rectangle>() {

				@Override
				public int compare(Rectangle letter1, Rectangle letter2) {
					return (letter1.x - letter2.x);
				}
			});
		}

		// now sort the lines based on the y-coordinate and x-coordinate of the first
		// box in that line

		Collections.sort(input, new Comparator<ArrayList<Rectangle>>() {

			@Override
			public int compare(ArrayList<Rectangle> line1, ArrayList<Rectangle> line2) {
				if ((line1.size() > 0) && (line2.size() > 0)) {
					if ((line1.get(0).y - line2.get(0).y) < -5) {
						return -1;
					}
					if ((line1.get(0).y - line2.get(0).y) > 5) {
						return 1;
					}
				}
				if (line1.size() == 0) {
					return -1;
				}
				if (line2.size() == 0) {
					return 1;
				}
				return (line1.get(0).x - line2.get(0).x);
			}

		});

		if (debugL <= 3) {
			System.out.println(
					"Thread : " + threadNumber + " - In reallocateVerticalBoxes() : RESULT LINES IS - " + input);
		}

		return input;

	}

	// to be used in the final bounding boxes found on the clipped rectangles for
	// each line
	public static ArrayList<ArrayList<Rectangle>> mergeVerticalBoxes(ArrayList<ArrayList<Rectangle>> input,
			ProcessDataWrapper processDataWrapper, int threadNumber, int imageNumber, int debugL) {

		// For highly pixelated pictures, some boxes are vertically stacked.
		// Merge such boxes

		// ArrayList<Rectangle> mergedBoxes = new ArrayList<>();
		double desiredMinimumOverlap = 0.2;

		// iterate through each line
		for (int i = input.size() - 1; i >= 0; --i) {
			if (debugL <= 2) {
				System.out.println("Thread : " + threadNumber + "Image " + imageNumber
						+ " - In mergeVerticalBoxes() : Evaluating line - " + (i + 1));
			}
			ArrayList<Rectangle> currentLine = input.get(i);
			innerLoop: for (int j = currentLine.size() - 1; j >= 0; --j) {
				Rectangle letter = currentLine.get(j);
				// check if there are other letters that are just above or below this letter
				comparisonLoop: for (Rectangle anotherLetter : currentLine) {
					if (anotherLetter == letter) {
						continue comparisonLoop;
					}
					double totalRange = Math.max(letter.x + letter.width, anotherLetter.x + anotherLetter.width)
							- Math.min(anotherLetter.x, letter.x);
					double sumOfRanges = (anotherLetter.width) + (letter.width);
					double overlap = Math.max(0, sumOfRanges - totalRange);
					boolean subsumed = ((overlap / letter.width) > 0.8) || ((overlap / anotherLetter.width) > 0.8);
					if (((overlap / totalRange) > desiredMinimumOverlap) || (subsumed)) {
						// found a letter with which there is sufficient overlap
						Rectangle newLetter = new Rectangle(Math.min(letter.x, anotherLetter.x),
								Math.min(letter.y, anotherLetter.y), (int) totalRange,
								Math.max(letter.y + letter.height, anotherLetter.y + anotherLetter.height)
										- Math.min(anotherLetter.y, letter.y));
						currentLine.remove(letter);
						int index = currentLine.indexOf(anotherLetter);
						currentLine.remove(anotherLetter);
						currentLine.add(index, newLetter);
						continue innerLoop;
					}
				}
			}
		}

		if (debugL <= 3) {
			System.out.println("Thread : " + threadNumber + "Image " + imageNumber
					+ " - In mergeVerticalBoxes() : RESULT LINES IS - " + input);
		}

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

	public static ArrayList<Rectangle> getBoxesInFinalPix(Pix pix, int referenceHeight, int referenceWidth,
			boolean dilateAndDo, int threadNumber, int imageNumber, int debugL) {
		if (pix == null) {
			System.out.println("Thread : " + threadNumber + " Image " + imageNumber
					+ " - In getBoxesInFinalPix() : input pix = null");
			return new ArrayList<Rectangle>();
		}
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber + " Image " + imageNumber
					+ " - In getBoxesInFinalPix() : Depth of pix = " + Leptonica1.pixGetDepth(pix));
			System.out.println("Thread : " + threadNumber + " Image " + imageNumber
					+ " - In getBoxesInFinalPix() : referenceHeight = " + referenceHeight);
			System.out.println("Thread : " + threadNumber + " Image " + imageNumber
					+ " - In getBoxesInFinalPix() : referenceWidth = " + referenceWidth);
		}

		int connectivity = 4;
		ArrayList<Rectangle> wordRectangles = new ArrayList<>();

		Pix pix1D = Leptonica1.pixConvertTo1(pix, 100);
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber + " Image " + imageNumber
					+ " - In getBoxesInFinalPix() : pix1D before getting result = " + pix1D);
		}
		Boxa result = Leptonica1.pixConnCompBB(pix1D, connectivity);
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber + " Image " + imageNumber
					+ " - In getBoxesInFinalPix() : after getting result = " + result);
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

		int initialSize = wordRectangles.size();
		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber + " Image " + imageNumber
					+ " - In getBoxesInFinalPix() : referenceHeight = " + referenceHeight);
			System.out.println("Thread : " + threadNumber + " Image " + imageNumber
					+ " - In getBoxesInFinalPix() : the round 1 number of words = " + initialSize);
			System.out.println("Thread : " + threadNumber + " Image " + imageNumber
					+ " - In getBoxesInFinalPix() : Before round 1 removal, the boxes are - " + wordRectangles);
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
			System.out.println("Thread : " + threadNumber + " Image " + imageNumber
					+ " - In getBoxesInFinalPix() : the round 1 number of words after removing small height words = "
					+ wordRectangles.size());
			System.out.println("Thread : " + threadNumber + " Image " + imageNumber
					+ " - In getBoxesInFinalPix() : After round 1, the boxes are - " + wordRectangles);
		}

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
			pix2Gray = Leptonica1.pixErodeGray(pix, xD, yD);
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
				System.out.println("Thread : " + threadNumber + " Image " + imageNumber
						+ " - In getBoxesInFinalPix() : the round 2 number of words = " + initialSize);
			}
			for (int i = initialSize - 1; i >= 0; --i) {
				if ((wordRectangles.get(i).height < (0.4 * referenceHeight))
						|| (wordRectangles.get(i).width < (0.3 * referenceWidth))) {
					wordRectangles.remove(i);
				}
			}
			if (debugL <= 2) {
				System.out.println("Thread : " + threadNumber + " Image " + imageNumber
						+ " - In getBoxesInFinalPix() : the round 2 number of words after removing small height words = "
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
						System.out.println("Thread : " + threadNumber + " Image " + imageNumber
								+ " - In getBoxesInFinalPix() : the round 3 number of words = "
								+ wordRectangles.size());
					}
				} else {
					wordRectangles = null;
				}
			}
		}

		if (debugL <= 2) {
			System.out.println("Thread : " + threadNumber + " Image " + imageNumber
					+ " - In getBoxesInFinalPix() : Box rectangles found in getBoxesInFinalPix() are - "
					+ wordRectangles);
		}

		// from wordrectangles, remove boxes that are > 1.75 * medianHeight
		DescriptiveStatistics heightStats = new DescriptiveStatistics();
		for (Rectangle word : wordRectangles) {
			heightStats.addValue(word.height);
		}

		double medianHeight = heightStats.getPercentile(50);

		for (int k = wordRectangles.size() - 1; k >= 0; --k) {
			// if (wordRectangles.get(k).height > (1.75 * medianHeight)) {
			if (wordRectangles.get(k).height >= (1.5 * medianHeight)) {
				wordRectangles.remove(k);
			}
		}
		return wordRectangles;

	}

	public static boolean writeFile(RenderedImage images, String formatName, String localOutputFile) {
		return writeFile(images, formatName, localOutputFile, 300, 0.5f);
	}

	public static boolean writeFile(RenderedImage images, String formatName, String localOutputFile, int dpi,
			float compressionQuality) {

		try {
			if (images == null) {
				// throw new IllegalArgumentException("No images available for writing to : " +
				// formatName + " file");
				return false;
			}
			Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);

			if (!writers.hasNext()) {
				// throw new IllegalArgumentException("No writer available for: " + formatName +
				// " files");
				return false;
			}

			IIOImage temp = null;
			ImageTypeSpecifier its = null;
			IIOMetadata md = null;
			ImageWriter writer = null;
			ImageWriteParam writeParam = null;
			ImageOutputStream output = null;
			its = ImageTypeSpecifier.createFromRenderedImage(images);
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
					// throw new IllegalArgumentException("No suitable writer found. Metadata of all
					// writers for : "
					// + formatName
					// + " files are either Read-Only or don't support standard metadata format.
					// Supported formats are : "
					// + sb);
					return false;
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
						TIFFField fieldXRes = new TIFFField(tagXRes, TIFFTag.TIFF_RATIONAL, 1,
								new long[][] { { dpi, 1 } });
						TIFFField fieldYRes = new TIFFField(tagYRes, TIFFTag.TIFF_RATIONAL, 1,
								new long[][] { { dpi, 1 } });

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

					temp = new IIOImage(images, null, md);
					writer.write(null, temp, writeParam);

					/*
					 * if (!writer.canInsertImage(1)) { throw new
					 * IllegalArgumentException("The writer for " + formatName +
					 * " files is not able to add more than one image to the file : " +
					 * localOutputFile); } else { temp = new IIOImage(images, null, md);
					 * writer.writeInsert(0, temp, writeParam); }
					 */
				} finally {
					// Close stream in finally block to avoid resource leaks
					if (output != null) {
						output.close();
					}
				}
			} finally {
				if (writer != null) {
					writer.dispose();
				}
			}
		} catch (Exception exc) {
			return false;
		}
		return true;
	}

	public static void appendNormalMessage(TextFlow flow, String message) {
		append(flow, message, "-fx-font: 18px Serif; -fx-stroke: blue");
	}

	public static void appendGreenMessage(TextFlow flow, String message) {
		// append(flow, message, "-fx-font: 12px Tahoma; -fx-font-weight: normal;
		// -fx-stroke: black");
		// append(flow, message, "-fx-font: 16px Serif; -fx-stroke: black");
		append(flow, message, "-fx-font: 14px Serif; -fx-stroke: black");
	}

	public static void appendRedMessage(TextFlow flow, String message) {
		// append(flow, message, "-fx-font: 14px Tahoma; -fx-font-weight: italic;
		// -fx-stroke: red; -fx-fill: red");
		// append(flow, message, "-fx-font: 12px Tahoma; -fx-font-weight: bold;
		// -fx-stroke: red");
		// append(flow, message, "-fx-font: italic 16px Serif; -fx-stroke: red");
		append(flow, message, "-fx-font: italic 14px Serif; -fx-stroke: red");
	}

	public static void appendAmberMessage(TextFlow flow, String message) {
		// append(flow, message, "-fx-font: 14px Tahoma; -fx-font-weight: italic;
		// -fx-stroke: orange; -fx-fill: orange");
		// append(flow, message, "-fx-font: 12px Tahoma; -fx-font-weight: italic;
		// -fx-stroke: orange");
		// append(flow, message, "-fx-font: italic 16px Serif; -fx-stroke: orange");
		append(flow, message, "-fx-font: italic 14px Serif; -fx-stroke: orange");

	}

	private static synchronized void append(TextFlow flow, String msg, String style) {
		Platform.runLater(() -> {
			Text t = new Text(msg);
			// t.setFont(Font.font("Tahoma"));
			if (!("".equals(style)) && (style != null)) {
				t.setStyle(style);
			}
			flow.getChildren().add(t);
		});
	}

	public static ArrayList<ArrayList<Rectangle>> segregateFinalBoxesIntoLines(
			ArrayList<ArrayList<Rectangle>> inputLines, int threadNumber, int imageNumber, int debugL) {

		if (debugL <= 2) {
			System.out.println("Thread " + threadNumber + " Image " + imageNumber
					+ " : Entered segregateFinalBoxesIntoLines() with " + inputLines.size() + " lines");
			for (ArrayList<Rectangle> line : inputLines) {
				for (Rectangle letter : line) {
					System.out.println(letter);
				}
			}
		}

		DescriptiveStatistics heightStats = new DescriptiveStatistics();

		for (ArrayList<Rectangle> line : inputLines) {
			for (Rectangle letter : line) {
				heightStats.addValue(letter.height);
			}
		}

		double medianHeight = heightStats.getPercentile(50);
		int totalLetters = (int) heightStats.getN();
		// heightStats.clear();

		// int minYDifference = (int) (medianHeight * 0.6);
		int minYDifference = 8; // original value
		double heightCutoff = 0.35;

		ArrayList<ArrayList<Rectangle>> lines = new ArrayList<>();
		Set<Integer> lineNumbersWhereFitmentPossible = new TreeSet<>();

		Rectangle[] letters = new Rectangle[totalLetters];
		int anIndex = 0;
		for (ArrayList<Rectangle> line : inputLines) {
			for (Rectangle letter : line) {
				letters[anIndex++] = letter;
			}
		}

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
					if (box.height < (medianHeight * heightCutoff)) {
						if (debugL <= 1) {
							System.out.println("Thread " + threadNumber + " Image " + imageNumber
									+ " : Ignored small box : " + box);
						}
						continue loop3;
					}
					// ignore the box if its height is too large
					if (box.height > (medianHeight * 1.75)) {
						if (debugL <= 1) {
							System.out.println("Thread " + threadNumber + " Image " + imageNumber
									+ " : Ignored large box : " + box);
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

			int acceptableGap = 1000; // any horizontal gap is OK

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

		}

		// sort each line of 'words' by x-coordinate

		for (ArrayList<Rectangle> line : lines) {
			Collections.sort(line, new Comparator<Rectangle>() {

				@Override
				public int compare(Rectangle r1, Rectangle r2) {
					return (r1.x - r2.x);
				}

			});
		}

		// sort the lines by y-coordinate

		if (lines.size() > 1) {
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
		}

		int tabSpace = 3;
		if (debugL <= 1) {
			for (ArrayList<Rectangle> line : lines) {
				for (int n = 0; n < tabSpace; ++n) {
					System.out.print(" ");
				}
				System.out.print("Thread " + threadNumber + " Image " + imageNumber + " : New Line - ");
				System.out.println(Arrays.toString(line.toArray(new Rectangle[line.size()])));
			}
		}
		// return finalLines;
		return lines;
	}

	public static ArrayList<ArrayList<Rectangle>> removeLargeBoxesAndRedistribute(
			ArrayList<ArrayList<Rectangle>> inputLines, int threadNumber) {
		// recurse through each line. find the median height of each line
		// in any line, if there are large boxes, in relation to the other boxes in the
		// line, then remove the box, put the other boxes into an array of Rectangles
		// and redistribute those boxes into an another arraylist of multiple lines.
		// thereafter, add this array into the original arraylist

		// check if the left-most box is a large box. If yes, split it into 2. The
		// left-most box is at index 0.
		// Check also if the right-most box is a large box. If yes, split it into 2. The
		// index of right-most box has to be found out
		// Likewise, do this for the second left-most and second right-most boxes as
		// well
		// This ensures that in most cases, both the top and bottom whole words are
		// eventually picked for Tesseract

		ArrayList<Integer> medianHeights = new ArrayList<>();
		ArrayList<Integer> medianWidths = new ArrayList<>();
		for (ArrayList<Rectangle> line : inputLines) {
			DescriptiveStatistics lHeight = new DescriptiveStatistics();
			DescriptiveStatistics lWidth = new DescriptiveStatistics();
			for (Rectangle letter : line) {
				lHeight.addValue(letter.height);
				lWidth.addValue(letter.width);
			}
			medianHeights.add((int) lHeight.getPercentile(50));
			medianWidths.add((int) lWidth.getPercentile(50));
		}

		double heightCutOff = 1.6;

		ArrayList<Boolean> lineToBeSplit = new ArrayList<>();

		for (int i = 0; i < inputLines.size(); ++i) {
			ArrayList<Rectangle> line = inputLines.get(i);
			// if the first word is a large word, then split it into 2
			ArrayList<Rectangle> splitBoxes = new ArrayList<>();

			int leftmostLargeBoxIndex = -1;
			int rightmostLargeBoxIndex = -1;

			for (int j = 0; j < line.size(); ++j) {
				if (((line.get(j).height * 1.0) / medianHeights.get(i)) > heightCutOff) {
					leftmostLargeBoxIndex = j;
					break;
				}
			}

			if (debugLevel <= 2) {
				System.out.println("Thread : " + threadNumber
						+ " removeLargeBoxesAndRedistribute() : leftmostLargeBoxIndex - " + leftmostLargeBoxIndex);
			}

			if (leftmostLargeBoxIndex != -1) {
				// find the bottom of the other boxes that are in the same line
				DescriptiveStatistics bottomY = new DescriptiveStatistics();
				DescriptiveStatistics otherBoxesBottomY = new DescriptiveStatistics();
				for (int j = 0; j < line.size(); ++j) {
					// if the new box is not a large box, and if the current large boxes
					// y-coordinate lies between the top and bottom of the new box
					if (((((line.get(j).height * 1.0) / medianHeights.get(i)) < 1.1))
							&& (line.get(leftmostLargeBoxIndex).y > (line.get(j).y - 6))
							&& (line.get(leftmostLargeBoxIndex).y < (line.get(j).y + line.get(j).height))) {
						bottomY.addValue(line.get(j).y + line.get(j).height);
					} else {
						otherBoxesBottomY.addValue(line.get(j).y + line.get(j).height);
					}
				}
				int bottomCoord = (int) bottomY.getMean();
				int upperBoxYCoord = Math.min(line.get(leftmostLargeBoxIndex).y,
						bottomCoord - medianHeights.get(i) - 1);
				int upperBoxHeight = bottomCoord - upperBoxYCoord;
				int lowerBoxExpectedBottom = (int) otherBoxesBottomY.getMean();
				int lowerBoxBottom = Math.max(
						line.get(leftmostLargeBoxIndex).y + line.get(leftmostLargeBoxIndex).height,
						lowerBoxExpectedBottom);
				int lowerBoxY = Math.min(lowerBoxBottom - medianHeights.get(i) - 1,
						upperBoxYCoord + upperBoxHeight + 4);
				splitBoxes.add(new Rectangle(line.get(leftmostLargeBoxIndex).x, upperBoxYCoord,
						line.get(leftmostLargeBoxIndex).width, upperBoxHeight));
				splitBoxes.add(new Rectangle(line.get(leftmostLargeBoxIndex).x, lowerBoxY,
						line.get(leftmostLargeBoxIndex).width, lowerBoxBottom - lowerBoxY));
				Rectangle removed = line.remove(leftmostLargeBoxIndex);
				if (debugLevel <= 2) {
					System.out.println("Thread : " + threadNumber
							+ " removeLargeBoxesAndRedistribute() : Removed large box rectangle - " + removed);
				}
			}
			for (Rectangle newLetter : splitBoxes) {
				if (debugLevel <= 2) {
					System.out.println("Thread : " + threadNumber + " removeLargeBoxesAndRedistribute() : Added box  - "
							+ newLetter);
				}
				line.add(newLetter);
			}
			splitBoxes.clear();

			// repeating for the right-most box

			for (int j = line.size() - 1; j >= 0; --j) {
				if (((line.get(j).height * 1.0) / medianHeights.get(i)) > heightCutOff) {
					rightmostLargeBoxIndex = j;
					break;
				}
			}

			if (debugLevel <= 2) {
				System.out.println("Thread : " + threadNumber
						+ " removeLargeBoxesAndRedistribute() : rightmostLargeBoxIndex - " + rightmostLargeBoxIndex);
			}

			// if (((line.get(lastIndex).height * 1.0) / medianHeights.get(i)) > 1.5) {
			if (rightmostLargeBoxIndex != -1) {
				// find the bottom of the other boxes that are in the same line
				DescriptiveStatistics bottomY = new DescriptiveStatistics();
				DescriptiveStatistics otherBoxesBottomY = new DescriptiveStatistics();
				for (int j = 0; j < line.size(); ++j) {
					// if the new box is not a large box, and if the current large boxes
					// y-coordinate lies between the top and bottom of the new box
					if (((((line.get(j).height * 1.0) / medianHeights.get(i)) < 1.1))
							&& (line.get(rightmostLargeBoxIndex).y > (line.get(j).y - 6))
							&& (line.get(rightmostLargeBoxIndex).y < (line.get(j).y + line.get(j).height))) {
						bottomY.addValue(line.get(j).y + line.get(j).height);
					} else {
						otherBoxesBottomY.addValue(line.get(j).y + line.get(j).height);
					}
				}
				int bottomCoord = (int) bottomY.getMean();
				int upperBoxYCoord = Math.min(line.get(rightmostLargeBoxIndex).y,
						bottomCoord - medianHeights.get(i) - 1);
				int upperBoxHeight = bottomCoord - upperBoxYCoord;
				int lowerBoxExpectedBottom = (int) otherBoxesBottomY.getMean();
				int lowerBoxBottom = Math.max(
						line.get(rightmostLargeBoxIndex).y + line.get(rightmostLargeBoxIndex).height,
						lowerBoxExpectedBottom);
				int lowerBoxY = Math.min(lowerBoxBottom - medianHeights.get(i) - 1,
						upperBoxYCoord + upperBoxHeight + 4);
				splitBoxes.add(new Rectangle(line.get(rightmostLargeBoxIndex).x, upperBoxYCoord,
						line.get(rightmostLargeBoxIndex).width, upperBoxHeight));
				splitBoxes.add(new Rectangle(line.get(rightmostLargeBoxIndex).x, lowerBoxY,
						line.get(rightmostLargeBoxIndex).width, lowerBoxBottom - lowerBoxY));
				Rectangle removed = line.remove(rightmostLargeBoxIndex);
				if (debugLevel <= 2) {
					System.out.println("Thread : " + threadNumber
							+ " removeLargeBoxesAndRedistribute() : Removed large box rectangle - " + removed);
				}
			}
			for (Rectangle newLetter : splitBoxes) {
				if (debugLevel <= 2) {
					System.out.println("Thread : " + threadNumber
							+ " removeLargeBoxesAndRedistribute() : Added large box rectangle - " + newLetter);
				}
				line.add(newLetter);
			}

			// repeat for all large boxes in between
			if (rightmostLargeBoxIndex != leftmostLargeBoxIndex) {
				// find the bottom of the other boxes that are in the same line
				DescriptiveStatistics bottomY = new DescriptiveStatistics();
				DescriptiveStatistics otherBoxesBottomY = new DescriptiveStatistics();
				for (int boxIndex = rightmostLargeBoxIndex - 1; boxIndex >= leftmostLargeBoxIndex; --boxIndex) {
					if (line.get(boxIndex).height < heightCutOff * medianHeights.get(i)) {
						continue;
					}
					for (int j = 0; j < line.size(); ++j) {
						// if the new box is not a large box, and if the current large boxes
						// y-coordinate lies between the top and bottom of the new box
						if (((((line.get(j).height * 1.0) / medianHeights.get(i)) < 1.1))
								&& (line.get(boxIndex).y > (line.get(j).y - 6))
								&& (line.get(boxIndex).y < (line.get(j).y + line.get(j).height))) {
							bottomY.addValue(line.get(j).y + line.get(j).height);
						} else {
							otherBoxesBottomY.addValue(line.get(j).y + line.get(j).height);
						}
					}
					int bottomCoord = (int) bottomY.getMean();
					int upperBoxYCoord = Math.min(line.get(boxIndex).y,
							bottomCoord - medianHeights.get(i) - 1);
					int upperBoxHeight = bottomCoord - upperBoxYCoord;
					int lowerBoxExpectedBottom = (int) otherBoxesBottomY.getMean();
					int lowerBoxBottom = Math.max(
							line.get(boxIndex).y + line.get(boxIndex).height,
							lowerBoxExpectedBottom);
					int lowerBoxY = Math.min(lowerBoxBottom - medianHeights.get(i) - 1,
							upperBoxYCoord + upperBoxHeight + 4);
					splitBoxes.add(new Rectangle(line.get(boxIndex).x, upperBoxYCoord, line.get(boxIndex).width,
							upperBoxHeight));
					splitBoxes.add(new Rectangle(line.get(boxIndex).x, lowerBoxY, line.get(boxIndex).width,
							lowerBoxBottom - lowerBoxY));
					Rectangle removed = line.remove(boxIndex);
					if (debugLevel <= 2) {
						System.out.println("Thread : " + threadNumber
								+ " removeLargeBoxesAndRedistribute() : Removed large box rectangle - " + removed);
					}
				}
			}
			for (Rectangle newLetter : splitBoxes) {
				if (debugLevel <= 2) {
					System.out.println("Thread : " + threadNumber
							+ " removeLargeBoxesAndRedistribute() : Added large box rectangle - " + newLetter);
				}
				line.add(newLetter);
			}

			if ((leftmostLargeBoxIndex != -1) || (rightmostLargeBoxIndex != -1)) {
				lineToBeSplit.add(Boolean.TRUE);
			} else {
				lineToBeSplit.add(Boolean.FALSE);
			}
		}

		int acceptableGap = 0;

		for (int lineNumber = inputLines.size() - 1; lineNumber >= 0; --lineNumber) {
			acceptableGap = (int) (medianWidths.get(lineNumber) * 3.5);
			if (debugLevel <= 2) {

				System.out.println(
						"Thread : " + threadNumber + " removeLargeBoxesAndRedistribute() : acceptableGap of line "
								+ lineNumber + " is : " + acceptableGap);

				System.out.println(
						"Thread : " + threadNumber + " removeLargeBoxesAndRedistribute() : Median Height of line : "
								+ lineNumber + " is " + medianHeights.get(lineNumber));
			}
			boolean toBeSplit = lineToBeSplit.get(lineNumber);
			ArrayList<Rectangle> line = inputLines.get(lineNumber);
			for (int letterNumber = line.size() - 1; letterNumber >= 0; --letterNumber) {
				Rectangle aLetter = line.get(letterNumber);
				if (aLetter.height > (heightCutOff * medianHeights.get(lineNumber))) {
					line.remove(letterNumber);
					if (debugLevel <= 2) {
						System.out.println(
								"Thread : " + threadNumber + " removeLargeBoxesAndRedistribute() : Removing letter : "
										+ aLetter + " because its height of " + aLetter.height + " is more than "
										+ (heightCutOff * medianHeights.get(lineNumber)));
					}
					toBeSplit = true;
					if (debugLevel <= 2) {
						System.out.println(
								"Thread : " + threadNumber + " removeLargeBoxesAndRedistribute() : Set toBeSplit to : "
										+ toBeSplit + " for line " + (lineNumber + 1));
					}
				}
			}
			int minYDifference = 10; // one can give a large number here because we want to minimise the number of
										// new lines formed in this method

			if (toBeSplit) {
				inputLines.remove(lineNumber);
				ArrayList<ArrayList<Rectangle>> splitLines = new ArrayList<>();
				Set<Integer> splitLineNumbersWhereFitmentPossible = new TreeSet<>();

				mainloop: for (Rectangle letter : line) {
					if (letter == null) {
						continue mainloop;
					}
					int index = 0;
					splitLineNumbersWhereFitmentPossible.clear();
					if (splitLines.size() == 0) { // the loop is starting
						ArrayList<Rectangle> newLine = new ArrayList<Rectangle>();
						splitLines.add(newLine);
						newLine.add(letter);
						continue mainloop;
					}
					loop2: for (ArrayList<Rectangle> aLine : splitLines) {
						for (Rectangle box : aLine) {
							// Note: letter is the new Rectangle picked up for fitment,
							// while box is an already slotted Rectangle in the lines ArrayList

							if ((Math.abs(letter.y - box.y) <= minYDifference)
									|| (Math.abs((letter.y + letter.height) - (box.y + box.height)) <= 4)) {
								splitLineNumbersWhereFitmentPossible.add(index);
								++index;
								continue loop2;
							}
							if ((letter.y >= box.y) && ((letter.y + letter.height) <= (box.y + box.height))) {
								splitLineNumbersWhereFitmentPossible.add(index);
								++index;
								continue loop2;
							}
						}
						++index;
					}
					if (splitLineNumbersWhereFitmentPossible.size() == 0) { // based on y-coordinates, did not find a
						// potential
						// set
						// of words where it can fit
						ArrayList<Rectangle> newLine = new ArrayList<Rectangle>();
						splitLines.add(newLine);
						newLine.add(letter);
						continue mainloop;
					}

					// First, check which existing lineNumber is a better fit for the box.
					// Essentially, find the distance between the current box and the boxes in the
					// line and choose that line where the distance is minimum

					// ArrayList<Integer> minimumXDistances = new ArrayList<>();
					int bestFitLine = -1;
					int minXDistance = Integer.MAX_VALUE;
					outerloop: for (int aLineNumber : splitLineNumbersWhereFitmentPossible) {
						ArrayList<Rectangle> aLine = splitLines.get(aLineNumber); // get the current list of letters at
																					// the
						// lineNumber
						for (Rectangle box : aLine) {
							int xDistance = (letter.x > box.x) ? Math.abs(letter.x - (box.x + box.width))
									: Math.abs(box.x - (letter.x + letter.width));
							if (xDistance < minXDistance) {
								minXDistance = xDistance;
								bestFitLine = aLineNumber;
								continue outerloop;
							}
						}
					}

					if ((bestFitLine != -1) && (minXDistance < acceptableGap)) {
						ArrayList<Rectangle> bestLine = splitLines.get(bestFitLine); // get the current list of letters
																						// at the lineNumber
						bestLine.add(0, letter);
					} else {
						// create a new line and add the letter
						ArrayList<Rectangle> newLine = new ArrayList<Rectangle>();
						splitLines.add(newLine);
						newLine.add(letter);
						continue mainloop;
					}
				}

				for (ArrayList<Rectangle> newLine : splitLines) {
					inputLines.add(newLine);
				}
			}
		}

		// remove all lines with length 0. It seems that some such lines are still in
		// the mix. Need to clear these somewhere above, but will do it here for now

		if (debugLevel <= 2) {
			System.out.println("Thread : " + threadNumber + " removeLargeBoxesAndRedistribute() : number of lines = "
					+ inputLines.size());
		}
		for (int k = inputLines.size() - 1; k >= 0; --k) {
			if ((inputLines.get(k) == null) || (inputLines.get(k).size() == 0)) {
				if (debugLevel <= 2) {
					System.out.println("Thread : " + threadNumber
							+ " removeLargeBoxesAndRedistribute() : About to remove line due to size being 0 : "
							+ inputLines.get(k));
				}
				inputLines.remove(k);
			}
		}
		if (debugLevel <= 2) {
			System.out.println("Thread : " + threadNumber
					+ " removeLargeBoxesAndRedistribute() : number of lines after removal of empty lines = "
					+ inputLines.size());
		}
		if (debugLevel <= 2) {
			System.out.println("Thread : " + threadNumber
					+ " removeLargeBoxesAndRedistribute() : Before Collections.sort(ArrayList<Rectangle>, ArrayList<Rectangle>) : ");
			int lineNumber = 0;
			for (ArrayList<Rectangle> line : inputLines) {
				System.out.print(lineNumber++ + ": ");
				System.out.println(line);
			}
		}

		// sort the boxes in each line
		// sort the lines by y-coordinate
		for (ArrayList<Rectangle> aLine : inputLines) {
			Collections.sort(aLine, new Comparator<Rectangle>() {

				@Override
				public int compare(Rectangle letter1, Rectangle letter2) {

					return (letter1.x - letter2.x);
				}
			});
		}

		// separate out the lines that have a box that is too far apart

		ArrayList<ArrayList<Rectangle>> finalLines = new ArrayList<>();
		medianWidths.clear();
		for (ArrayList<Rectangle> line : inputLines) {
			DescriptiveStatistics lWidth = new DescriptiveStatistics();
			for (Rectangle letter : line) {
				lWidth.addValue(letter.width);
			}
			medianWidths.add((int) lWidth.getPercentile(50));
		}

		int lNumber = -1;
		for (ArrayList<Rectangle> aLine : inputLines) {
			++lNumber;
			acceptableGap = (int) (medianWidths.get(lNumber) * 3.5);
			for (int k = aLine.size() - 1; k > 0; --k) {
				if ((aLine.get(k).x - aLine.get(k - 1).x - aLine.get(k - 1).width) > acceptableGap) {
					if (debugLevel <= 2) {
						System.out.println("Thread : " + threadNumber
								+ " removeLargeBoxesAndRedistribute() : About to split line : " + lNumber + " at index "
								+ k + " because " + aLine.get(k).x + "-" + aLine.get(k - 1).x + "="
								+ (aLine.get(k).x - aLine.get(k - 1).x) + ">" + acceptableGap);
					}
					ArrayList<Rectangle> newLine = new ArrayList<>();
					for (int splitIndex = aLine.size() - 1; splitIndex >= k; --splitIndex) {
						newLine.add(aLine.remove(splitIndex));
					}
					finalLines.add(newLine);
				}
			}
			finalLines.add(aLine);
		}

		// sort the lines by y-coordinate
		Collections.sort(finalLines, new Comparator<ArrayList<Rectangle>>() {

			@Override
			public int compare(ArrayList<Rectangle> line1, ArrayList<Rectangle> line2) {

				if (debugLevel <= 1) {
					System.out.println("Comparing - " + line1 + " ; and " + line2);
				}
				if ((line1.size() == 0) && (line2.size() == 0)) {
					return 0;
				}
				if ((line1.size() > 0) && (line2.size() == 0)) {
					return 1;
				}
				if ((line1.size() == 0) && (line2.size() > 0)) {
					return -1;
				}
				if ((line1.get(0).y - line2.get(0).y) < -5) {
					return -1;
				}
				if ((line1.get(0).y - line2.get(0).y) > 5) {
					return 1;
				}
				return ((line1.get(0).x - line2.get(0).x) > 5 ? 1 : ((line1.get(0).x - line2.get(0).x) < -5 ? -1 : 0));
			}
		});

		int tabSpace = 3;
		if (debugLevel <= 1) {
			for (ArrayList<Rectangle> line : finalLines) {
				for (int n = 0; n < tabSpace; ++n) {
					System.out.print(" ");
				}
				System.out.print("Thread : " + threadNumber + " removeLargeBoxesAndRedistribute() : New Line - ");
				System.out.println(Arrays.toString(line.toArray(new Rectangle[line.size()])));
			}
		}

		return finalLines;

	}

	static class DeskewedOriginalAndRectangles {
		BufferedImage deskewedOriginal;
		BufferedImage deskewedCleanedOriginal;
		ArrayList<ArrayList<Rectangle>> lines;

		public DeskewedOriginalAndRectangles(BufferedImage deskewed, BufferedImage cleaned,
				ArrayList<ArrayList<Rectangle>> inputLines) {
			this.deskewedOriginal = deskewed;
			this.deskewedCleanedOriginal = cleaned;
			this.lines = inputLines;
		}
	}

	static class DeskewedPixesAndRectangles {
		ArrayList<BufferedImage> deskewed;
		ArrayList<ArrayList<Rectangle>> lines;

		public DeskewedPixesAndRectangles(ArrayList<BufferedImage> deskewed,
				ArrayList<ArrayList<Rectangle>> inputLines) {
			this.deskewed = deskewed;
			this.lines = inputLines;
		}
	}

	static class DeskewedPixAndRectangles {
		Pix deskewed;
		ArrayList<ArrayList<Rectangle>> lines;

		public DeskewedPixAndRectangles(Pix deskewed, ArrayList<ArrayList<Rectangle>> inputLines) {
			this.deskewed = deskewed;
			this.lines = inputLines;
		}
	}

	/*
	 * public static boolean isFileReadyForReadingInUnix(File file) { Instant t =
	 * Instant.now(); Process plsof = null; BufferedReader reader = null; try { //
	 * plsof = new ProcessBuilder(new String[] { "lsof", "-e", //
	 * "/run/user/1000/gvfs", "-f", "--", file.getAbsolutePath(), "|", "grep", //
	 * file.getAbsolutePath() }).start(); plsof = new ProcessBuilder( new String[] {
	 * "lsof", "-e", "/run/user/1000/gvfs", "-f", "--", file.getAbsolutePath()
	 * }).start(); reader = new BufferedReader(new
	 * InputStreamReader(plsof.getInputStream())); String line; int lineNumber = 1;
	 * while ((line = reader.readLine()) != null) { System.out.println(lineNumber++
	 * + " : " + line); if (line.contains(file.getAbsolutePath())) { reader.close();
	 * plsof.destroy(); System.out.println( "About to return false after " +
	 * Duration.between(t, Instant.now()).toMillis() + " ms"); return false; } } }
	 * catch (Exception ex) { System.out.println("Encountered exception after " +
	 * Duration.between(t, Instant.now()).toMillis() + " ms");
	 *
	 * // TODO: handle exception ... } if (reader != null) { try { reader.close(); }
	 * catch (IOException ioe) { } } if (plsof != null) { plsof.destroy(); }
	 * System.out.println("About to return true after " + Duration.between(t,
	 * Instant.now()).toMillis() + " ms"); return true; }
	 */

	public static boolean isFileReadyForReadingInUnix(File file) {
		Instant t = Instant.now();
		RandomAccessFile in = null;
		FileLock lock = null;
		try {
			in = new RandomAccessFile(file, "rw");
			lock = in.getChannel().lock();
		} catch (Exception e) {
			System.out.println("About to return false after " + Duration.between(t, Instant.now()).toMillis() + " ms");
			return false;
		} finally {
			try {
				if (lock != null) {
					lock.release();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				System.out.println(
						"About to return false after " + Duration.between(t, Instant.now()).toMillis() + " ms");
				return false;
			}
		}
		System.out.println("About to return true after " + Duration.between(t, Instant.now()).toMillis() + " ms");
		return true;
	}

	public static boolean isFileReadyForReadingInWindows(File file) {
		boolean closed;
		Channel channel = null;
		try {
			channel = new RandomAccessFile(file, "rw").getChannel();
			closed = true;
		} catch (Exception ex) {
			closed = false;
		} finally {
			if (channel != null) {
				try {
					channel.close();
				} catch (IOException ex) {
					// exception handling
				}
			}
		}
		return closed;
	}

	public static int isWindows() {

		if (isWindows != 2) {
			return isWindows;
		}
		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.indexOf("win") >= 0) {
			isWindows = 1;
		} else {
			isWindows = 0;
			Process plsof = null;
			// if Unix, then umount debugfs to save time during the loop that checks if a
			// file change triggered by watchservice response has finished being in writable
			// state
			try {
				plsof = new ProcessBuilder(new String[] { "sudo", "mount", "|", "grep", "debugfs" }).start();
				plsof.destroy();
			} catch (Exception ex) {
				// TODO: handle exception ...
			}
			try {
				plsof = new ProcessBuilder(new String[] { "sudo", "umount", "$(mount", "|", "grep", "debugfs", "|",
						"awk", "'{print", "$3}')" }).start();
				plsof.destroy();
			} catch (Exception ex) {
				// TODO: handle exception ...
			}
		}
		return isWindows;

	}

	private static boolean areViableBoundingBoxesPresent(ArrayList<ArrayList<Rectangle>> lines) {
		int count = 0;
		// double viableHeightCutOff = 0.5;
		// keeping a low number because in many dot-matrix no-product-name images,
		// the first try leads to many large boxes, the total count of which is less
		// than 5
		int minHeight = 6; // min height required of the characters for them to be considered as characters
		if (CheckProductProperties.productIsGiven) {
			minHeight = 11;
		}
		int viableNumberCutOff = 4;
		for (ArrayList<Rectangle> line : lines) {
			for (Rectangle word : line) {
				// if ((word.height > (viableHeightCutOff * idealCharHeight)) || (word.height >=
				// minHeight)) {
				if (word.height >= minHeight) {
					++count;
				}
			}
		}
		if (debugLevel <= 2) {
			System.out.println("In areViableBoundingBoxesPresent() : count of boundingBoxes = " + count);
		}
		if (count >= viableNumberCutOff) {
			return true;
		}
		return false;
	}

	private static boolean areSomeCharactersLikelyPresent(Pix cleanedPix) {
		// check the %age of pixels that are black; if there are more than 3.5% black
		// cells, then it's likely that there are some characters present
		// The cutoff percentage needs to be checked empirically
		double cutOff = 0.035;
		double viableNumberCutOff = 255 * (1 - cutOff);
		// int depth = Leptonica1.pixGetDepth(cleanedPix);
		// if (depth > 1) {
		// viableNumberCutOff = (Math.pow(2, depth) - 1) * (1 - cutOff);
		// } else {
		// viableNumberCutOff = cutOff;
		// }
		double count = averagePixelValue(cleanedPix);
		if (debugLevel <= 2) {
			System.out.println("In areSomeCharactersLikelyPresent() : averagePixelValue = " + count);
		}

		/*
		 * if (depth > 1) { if (count >= viableNumberCutOff) { if (debugLevel <= 2) {
		 * System.out.
		 * println("In areSomeCharactersLikelyPresent() : Another try is unviable."); }
		 * return false; } } else { if (count <= viableNumberCutOff) { if (debugLevel <=
		 * 2) { System.out.
		 * println("In areSomeCharactersLikelyPresent() : Another try is unviable."); }
		 * return false; } }
		 */
		if (count > viableNumberCutOff) {
			if (debugLevel <= 2) {
				System.out.println("In areSomeCharactersLikelyPresent() : Another try is unviable.");
			}
			return false;
		}
		if (debugLevel <= 2) {
			System.out.println("In areSomeCharactersLikelyPresent() : Another try is viable.");
		}
		return true;
	}

	public static ArrayList<ArrayList<Rectangle>> removeEdgeKissingBoundingBoxes(ArrayList<ArrayList<Rectangle>> input,
			Pix oneBppPix, int border, int threadNumber) {
		// remove edge kissing boxes in a line if they do not have any other boxes in
		// that line close to them i.e if
		// they are lone rangers
		if (oneBppPix == null) {
			return input;
		}
		double maximumGapBetweenBoxes = 2.0;
		double xLeft = border;
		double xRight = oneBppPix.w - xLeft;
		double yTop = border;
		double yBot = oneBppPix.h - yTop;
		for (ArrayList<Rectangle> line : input) {
			for (int i = line.size() - 1; i >= 0; --i) {
				Rectangle boundingBox = line.get(i);
				if (boundingBox.x < xLeft) {
					if ((i + 1) <= (line.size() - 1)) {
						Rectangle nextBoundingBox = line.get(i + 1);
						if (Math.abs(
								(nextBoundingBox.x - boundingBox.x - boundingBox.width)) > (boundingBox.height
										* maximumGapBetweenBoxes)) {
							line.remove(i);
							continue;
						}
					}
				}
				if (boundingBox.y < yTop) {
					if ((i + 1) <= (line.size() - 1)) {
						Rectangle nextBoundingBox = line.get(i + 1);
						if (Math.abs((nextBoundingBox.x - boundingBox.x - boundingBox.width)) > (boundingBox.height
								* maximumGapBetweenBoxes)) {
							line.remove(i);
							continue;
						}
					}
				}
				if ((boundingBox.x + boundingBox.width) > xRight) {
					if ((i - 1) >= 0) {
						Rectangle previousBoundingBox = line.get(i - 1);
						if (Math.abs((boundingBox.x - previousBoundingBox.x
								- previousBoundingBox.width)) > (boundingBox.height * maximumGapBetweenBoxes)) {
							line.remove(i);
							continue;
						}
					}
				}
				if ((boundingBox.y + boundingBox.height) > yBot) {
					if ((i + 1) <= (line.size() - 1)) {
						Rectangle nextBoundingBox = line.get(i + 1);
						if (Math.abs((nextBoundingBox.x - boundingBox.x - boundingBox.width)) > (boundingBox.height
								* maximumGapBetweenBoxes)) {
							line.remove(i);
							continue;
						}
					}
				}
			}
		}
		return input;
	}

	public static BufferedImage rotate(BufferedImage input) {
		final double rads1 = Math.toRadians(try1RotationAngle);
		final double sin1 = Math.abs(Math.sin(rads1));
		final double cos1 = Math.abs(Math.cos(rads1));
		final int w1 = (int) Math.floor((input.getWidth() * cos1) + (input.getHeight() * sin1));
		final int h1 = (int) Math.floor((input.getHeight() * cos1) + (input.getWidth() * sin1));
		BufferedImage rotated = new BufferedImage(w1, h1, BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g1 = rotated.createGraphics();
		g1.setColor(Color.WHITE);
		g1.fillRect(0, 0, w1, h1);
		RenderingHints rh3 = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g1.setRenderingHints(rh3);
		RenderingHints rh4 = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g1.setRenderingHints(rh4);
		RenderingHints rh5 = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g1.setRenderingHints(rh5);
		final AffineTransform at1 = new AffineTransform();
		at1.rotate(rads1, w1 / 2, h1 / 2);
		g1.drawImage(input, at1, null);
		g1.dispose();
		return rotated;
	}

}