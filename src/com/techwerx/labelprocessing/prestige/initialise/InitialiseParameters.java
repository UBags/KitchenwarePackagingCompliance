package com.techwerx.labelprocessing.prestige.initialise;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.sun.jna.Platform;
import com.techwerx.image.utils.SysOutController;
import com.techwerx.labelprocessing.prestige.CheckProductProperties;
import com.techwerx.labelprocessing.prestige.ProductPriceData;

public class InitialiseParameters {

	static {
		System.setProperty("jna.debug_load", "true");
	}

	private static String initialisationFile =
			"E:/TechWerx/Java/Working/Input/Initialisation Properties/config.properties";
	private static String productSettingsFile =
			"E:/TechWerx/Java/Working/Input/Initialisation Properties/product.properties";
	private static final String externalLibFolderKey = "externallib.folder";
	private static final String inputFolderKey = "input.folder";
	private static long productPropertiesTimestamp = 0L;

	private InitialiseParameters() {
	}

	// =========================================================================
	// Public API
	// =========================================================================

	/** Initialises from the hard-coded default config file path. */
	public static boolean initialise() {
		Properties props = loadPropertiesFromFile(new File(initialisationFile), true);
		if (props == null) {
			return false;
		}
		return applyAndLoadNative(props);
	}

	/**
	 * Initialises from a supplied file path, with an optional fallback to the
	 * default path.
	 *
	 * @param file                          path to the config file; may be null/empty
	 * @param useFallbackInitialisationOption if true and {@code file} is blank,
	 *                                       fall back to the default path
	 */
	public static boolean initialise(String file, boolean useFallbackInitialisationOption) {
		File initFile = resolveConfigFile(file, initialisationFile, useFallbackInitialisationOption);
		if (initFile == null) {
			SysOutController.resetSysOut();
			System.out.println("");
			System.out.println("Could not find initialisation file - "
					+ System.getProperty("user.dir") + File.separator + file);
			return false;
		}
		Properties props = loadPropertiesFromFile(initFile, false);
		if (props == null) {
			return false;
		}
		System.out.println("tesseractLib = " + System.getProperty("tesseractLib")
				+ "; leptLib = " + System.getProperty("leptLib"));
		return applyAndLoadNative(props);
	}

	/**
	 * Reads product settings from a supplied file, re-loading only when the file
	 * has been modified since the last load.
	 */
	public static boolean getProductSetting(String file, boolean useFallbackInitialisationOption) {
		File prodFile = resolveConfigFile(file, productSettingsFile, useFallbackInitialisationOption);
		if (prodFile == null) {
			SysOutController.resetSysOut();
			System.out.println("");
			System.out.println("Could not find product settings file - " + file);
			return false;
		}
		if (!prodFile.exists()) {
			return false;
		}
		if (prodFile.lastModified() == productPropertiesTimestamp) {
			return true;
		}
		CheckProductProperties.reset();
		Properties props = loadPropertiesFromFile(prodFile, false);
		if (props == null) {
			return false;
		}
		productPropertiesTimestamp = prodFile.lastModified();
		System.err.println("Reloaded product properties");
		applySystemProperties(props);
		reportProductSettings(props);
		ProductPriceData.loadMonths();
		ProductPriceData.loadYears();
		return true;
	}

	// =========================================================================
	// Private helpers
	// =========================================================================

	/**
	 * Resolves which File to use: the supplied path, or the fallback path,
	 * or null if neither is available.
	 */
	private static File resolveConfigFile(String file, String fallbackPath,
			boolean useFallback) {
		if ((file != null) && !("".equals(file))) {
			return new File(file);
		}
		if (useFallback) {
			return new File(fallbackPath);
		}
		return null;
	}

	/**
	 * Opens {@code f} and loads it into a {@link Properties} object.
	 *
	 * @param silent if true, error messages are printed to stdout only (not
	 *               after resetting SysOut); used for the no-argument
	 *               {@link #initialise()} path which has no prior SysOut state
	 * @return the loaded Properties, or null on any failure
	 */
	private static Properties loadPropertiesFromFile(File f, boolean silent) {
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(f);
		} catch (Exception e) {
			if (!silent) {
				SysOutController.resetSysOut();
			}
			e.printStackTrace();
			System.out.println("");
			System.out.println("Could not find initialisation file - " + f.getAbsolutePath());
			return null;
		}
		if (inputStream == null) {
			System.out.println("InputStream = null");
			System.out.println("Property initialisation file " + f.getAbsolutePath()
					+ " not found in the classpath " + System.getProperty("java.class.path"));
			return null;
		}
		Properties props = new Properties();
		try {
			props.load(inputStream);
			if (!silent) {
				System.out.println("Read initialisation file - " + f.getAbsolutePath()
						+ " and loaded properties - " + inputStream);
			}
		} catch (Exception e) {
			if (!silent) {
				SysOutController.resetSysOut();
			}
			e.printStackTrace();
			System.out.println("");
			System.out.println("Error reading property initialisation file "
					+ f.getAbsolutePath() + " in the classpath "
					+ System.getProperty("java.class.path"));
			return null;
		}
		return props;
	}

	/**
	 * Iterates all entries in {@code props}, sets each as a system property,
	 * builds the external library path, and sets TESSDATA_PREFIX.
	 *
	 * @return the resolved absolute path to the external library folder, or null
	 *         if the {@code externallib.folder} key was not present
	 */
	private static String applySystemProperties(Properties props) {
		Set<Map.Entry<Object, Object>> entries = props.entrySet();
		boolean externalLibFolderFound = false;
		String externalLibFolder = null;

		for (Map.Entry<Object, Object> entry : entries) {
			System.setProperty(entry.getKey().toString(), entry.getValue().toString());

			if (externalLibFolderKey.equals(entry.getKey().toString())) {
				externalLibFolderFound = true;
				externalLibFolder = new File(entry.getValue().toString()).getAbsolutePath();
				externalLibFolder = setupLibraryPaths(externalLibFolder);
			}
			if ("tesseract.datapath".equals(entry.getKey().toString())) {
				System.setProperty("TESSDATA_PREFIX", entry.getValue().toString());
			}
		}

		if (!externalLibFolderFound) {
			// Temp dir is where the JVM copies native libraries for loading
			String fallback = System.getProperty(inputFolderKey) + "/External Libraries";
			System.setProperty("java.io.tmpdir", fallback);
			System.setProperty("java.library.path", fallback);
		}

		return externalLibFolder;
	}

	/**
	 * Prepends {@code externalLibFolder} to {@code java.library.path} and
	 * injects it into the ClassLoader's internal {@code sys_paths} array so that
	 * subsequent {@code System.loadLibrary()} calls can find the native binaries.
	 *
	 * <p>The reflection-based {@code sys_paths} override is a necessary workaround:
	 * the JVM caches the library search path at first use of
	 * {@code System.loadLibrary()}, and updating {@code java.library.path} alone
	 * has no effect after that point.
	 *
	 * @return {@code externalLibFolder} unchanged (for chaining convenience)
	 */
	private static String setupLibraryPaths(String externalLibFolder) {
		String libPath = System.getProperty("java.library.path");
		String newPath;
		if ((libPath == null) || libPath.isEmpty()) {
			newPath = externalLibFolder;
		} else {
			newPath = externalLibFolder + File.pathSeparator + libPath;
		}
		System.setProperty("java.library.path", newPath);

		// Subvert the JVM's cached sys_paths so loadLibrary() sees the new folder
		try {
			Field field = ClassLoader.class.getDeclaredField("sys_paths");
			field.setAccessible(true);
			ClassLoader classLoader = ClassLoader.getSystemClassLoader();
			List<String> newSysPaths = new ArrayList<>();
			newSysPaths.add(externalLibFolder);
			newSysPaths.addAll(Arrays.asList((String[]) field.get(classLoader)));
			field.set(classLoader, newSysPaths.toArray(new String[0]));
		} catch (Exception e) {
			// Silently ignored: if the reflection fails, System.load() with the full
			// path below will still work on most JVM/OS combinations
		}

		return externalLibFolder;
	}

	/**
	 * Loads the Leptonica and Tesseract native libraries by their full absolute
	 * paths, using {@code System.load()} rather than {@code System.loadLibrary()}
	 * to avoid dependency on the library search path being correct at call time.
	 */
	private static void loadNativeLibraries(String externalLibFolder, String leptLib,
			String tesseractLib, String extension) {
		try {
			System.load(externalLibFolder + File.separator + leptLib + extension);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Could not load library " + leptLib);
		}
		try {
			System.load(externalLibFolder + File.separator + tesseractLib + extension);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Could not load library " + tesseractLib);
		}
	}

	/**
	 * Applies system properties from {@code props}, sets up library paths, loads
	 * native libraries, and sets {@code java.util.Arrays.useLegacyMergeSort}.
	 * Common final step shared by both {@link #initialise()} overloads.
	 */
	private static boolean applyAndLoadNative(Properties props) {
		boolean isWindows = Platform.isWindows();
		String extension = isWindows ? ".dll" : ".so";
		String tesseractLib = System.getProperty("tesseractLib");
		String leptLib = System.getProperty("leptLib");

		String externalLibFolder = applySystemProperties(props);

		// Solves IllegalArgumentException in Comparator.sort on some JVMs
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

		if (externalLibFolder != null) {
			loadNativeLibraries(externalLibFolder, leptLib, tesseractLib, extension);
		}

		return true;
	}

	/**
	 * Prints a summary of the product property settings to stdout,
	 * temporarily re-enabling SysOut if it has been silenced.
	 */
	private static void reportProductSettings(Properties props) {
		Set<Map.Entry<Object, Object>> entries = props.entrySet();
		boolean fixedProductName = false;
		for (Map.Entry<Object, Object> entry : entries) {
			if ("productname.fixed".equals(entry.getKey().toString())) {
				fixedProductName = "true".equals(entry.getValue().toString());
			}
		}

		boolean sysOut = SysOutController.isWritingToSysout();
		if (!sysOut) {
			SysOutController.resetSysOut();
		}
		System.out.println("----------------------");

		boolean productNameGivenForFixedProduct = false;
		for (Map.Entry<Object, Object> entry : entries) {
			if (!fixedProductName) {
				productNameGivenForFixedProduct = true;
				if ("productname.fixed".equals(entry.getKey().toString())) {
					System.out.println(entry.getKey() + "=" + entry.getValue());
				}
			} else {
				System.out.println(entry.getKey() + "=" + entry.getValue());
				if ("product.name".equals(entry.getKey().toString())) {
					productNameGivenForFixedProduct = true;
					if ("".equals(entry.getValue().toString())) {
						System.out.println("-------------------------------");
						System.out.println(
								"Need a valid product name specified against product.name parameter "
								+ "in product.properties, when productname.fixed=true");
					}
				}
			}
		}

		if (!productNameGivenForFixedProduct) {
			System.out.println("-------------------------------");
			System.out.println(
					"Need a valid product name specified against product.name parameter "
					+ "in product.properties, when productname.fixed=true");
		}

		if (!sysOut) {
			SysOutController.ignoreSysout();
		}
	}

}