package com.techwerx.labelprocessing.prestige;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.techwerx.image.utils.SBImageUtils;
import com.techwerx.text.OCRResult;
import com.techwerx.image.utils.SysOutController;
import com.techwerx.labelprocessing.OCRDateDimensionsWrapper;
import com.techwerx.labelprocessing.OCRPriceDimensionsWrapper;
import com.techwerx.labelprocessing.OCRProductDimensionsWrapper;
import com.techwerx.labelprocessing.OCRStringWrapper;
import com.techwerx.labelprocessing.ProductDescription;



import info.debatty.java.stringsimilarity.experimental.Sift4;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

public class ProductPriceData {

	public static final String priceDotErrorKey = "pricedot.error";
	public static final String monthErrorKey = "month.error";
	public static final String yearErrorKey = "year.error";
	public static final String productErrorKey = "product.error";
	public static final String priceErrorKey = "price.error";
	private static final String whichMonthKey = "months.allowed";
	private static final String inputFolderKey = "input.folder";
	private static final String pricemasterFolderKey = "pricemaster.folder";
	private static final String pricemasterFileKey = "pricemaster.file";
	private static final String pricemasterDefaultFile = "Price master summary.csv";
	private static final String pricemasterHeaderKey = "pricemaster.header";
	private static final String pricemasterHeaderDefault = "no";
	// private static final String productNameNeededKey = "productname.fixed";
	// private static final String productNameKey = "product.name";
	// private static int givenProductPrice = 0;

	private static final String pricemasterHasHeaderValue = "yes";
	private static final String useAllMonthsValue = "all";
	private static final String useMoreThanCurrentYear = "all";

	private static boolean useCurrentMonthOnly = !useAllMonthsValue.equals(System.getProperty(whichMonthKey));
	private static boolean useCurrentYearOnly = !useMoreThanCurrentYear.equals(System.getProperty(whichMonthKey));

	// private static String separator = ";";
	// private String datapath = "E:\\TechWerx\\Java\\Working\\Input\\Price
	// Master\\";
	// private String fileName = "Price master summary.csv";
	/*
	 * private String databasePath =
	 * "E:\\TechWerx\\Java\\Working\\Input\\Price Master\\"; private String dbName =
	 * "dbEVISION.App"; private String query =
	 * "SELECT DISTINCT tblGrpMaster.NAME AS GroupName, tblModelMaster.NAME AS ModelName, tblPriceMaster.PRICE AS Price, tblModelMaster.CH_HEIGHT as CharacterHeight "
	 * +
	 * "FROM ((tblModelMaster INNER JOIN tblGrpMaster ON tblModelMaster.GRPID = tblGrpMaster.ID) INNER JOIN "
	 * + "tblPriceMaster ON tblModelMaster.ID = tblPriceMaster.MODELID)";
	 */
	public static HashMap<String, Integer> productPriceMap = new HashMap<>();
	public static ArrayList<String> locations = new ArrayList<>();
	public static ArrayList<String> originalProductStrings = new ArrayList<>();
	public static ArrayList<Integer> noOfWordsInProduct = new ArrayList<>();
	public static ArrayList<Integer> originalProductHeights = new ArrayList<>();
	public static ArrayList<String> products = new ArrayList<>();
	public static ArrayList<Integer> prices = new ArrayList<>();
	// public ArrayList<Integer> priceDigits = new ArrayList<>();
	public static ArrayList<String> months = new ArrayList<>();
	public static ArrayList<Integer> years = new ArrayList<>();

	public static Pattern numberSequence = Pattern.compile("\\d{3,}"); // check for sequence of 3 or more numbers
	// Pattern PAT = Pattern.compile( "\\b(\\d{3,}[^\\s]*)" );
	public static final String productLine = "Product";
	public static final String priceLine = "Price";
	public static final String dateLine = "Date";
	public static final String neitherPriceNorDateLine = "neitherPriceNorDateLine";
	public static final String irrelevantLine = "Irrelevant";

	public static final Pattern[] productsBeginWith = { Pattern.compile("PRESTIGE"), Pattern.compile("JUDGE"),
			Pattern.compile("P[A-Z]{6}E"), Pattern.compile("J[A-Z]{3}E"), Pattern.compile(".R[A-Z]{5}E"),
			Pattern.compile(".U[A-Z]{2}E"), Pattern.compile(".R[A-Z]{4}G."), Pattern.compile(".U[A-Z]G."),
			Pattern.compile("C...E.") };
	public static final Pattern[] productsHave = { Pattern.compile("\\d.{0,3}L"),
			Pattern.compile("\\d\\s{0,2}\\+\\s{0,2}\\d"), Pattern.compile("JDPP"), Pattern.compile("[A-Z]{1,2}PP"),
			Pattern.compile("PRESSURE PAN"), Pattern.compile("P.{4}U.{2}\\s{1,2}P.{0,2}"),
			Pattern.compile("\\d\\s{0,1}\\.\\s{0,1}\\d\\s{0,2}L") };
	public static final Pattern[] ttk = { Pattern.compile("T{1,2}K"), Pattern.compile("T{2}K{0,1}") };
	public static final Pattern[] ttkPrestigeLtd = { Pattern.compile("T{1,2}K\\s{1,2}PRE.{0,5}"),
			Pattern.compile("T{1,2}K\\s{1,2}P.{0,7}"), Pattern.compile("T.K\\s{1,2}P.{0,7}"),
			Pattern.compile(".TK\\s{1,2}P.{0,7}"), Pattern.compile("\\sLTD"), Pattern.compile("\\sLIM.{0,4}") };
	public static final Pattern[] manufacturedBy = { Pattern.compile("MANUFACTURED\\s{1,3}BY"),
			Pattern.compile("[A-Z]{0,12}\\s{1,2}BY"), Pattern.compile("MAN[A-Z]{0,12}\\s{0,2}[A-Z]{0,2}"),
			Pattern.compile(".{0,7}TUR\\w{2}\\s{1,2}\\w{2}") };
	// non digit characters in ones and twos
	public static final Pattern[] oneAndTwoCharacters = { Pattern.compile("\\s\\D?[^L]\\s"),
			Pattern.compile("\\s\\D?[^L]\\b"), Pattern.compile("\\b\\D?[^L]\\s") };
	// public static final Pattern capacity1 =
	// Pattern.compile("\\s{0,3}\\d\\s{0,3}[A-Z]"); // 3L,5 L, 2 L, 1L
	public static final Pattern capacity1 = Pattern.compile("\\d{1,2}\\s{0,3}[LCMU]"); // 3L, 5 L, 2 L, 1L, 10 L
	// public static final Pattern capacity2 =
	// Pattern.compile("\\s{0,3}\\d.{1,3}\\d\\s{0,2}[A-Z]"); // 2.5L, 2.5 L, 2 _ 5
	// L, etc
	// public static final Pattern capacity2 =
	// Pattern.compile("\\d{0,2}.{0,3}\\d{1,2}.{1,3}\\d\\s{0,2}[A-Z]"); // 2.5L,
	public static final Pattern capacity2 = Pattern.compile("(\\d{1,2}\\D{0,3})+[LCM]($|\\s|O|T)"); // 2.5L,
	// 2.5L, 2 _ 5 L, 12 + 3 L, 12+5+3 L, 5 + 3 + 2 COMBI, etc

	private static final Set<String> uniqueProductStrings = new LinkedHashSet<>();
	private static final String[] uniqueProductStringExclusions = { "COMPLETE", "SEPARATELY" };
	private static final HashMap<String, Set<String>> capacityVariants = new HashMap<>();

	private static ProductPriceData pkInstance = null;

	// public static boolean productIsGiven;

	public static Pattern pricePattern = Pattern.compile("[A-Z]{0,2}ICE");
	public static Pattern retailPattern = Pattern.compile("[A-Z]{0,3}AIL");
	public static Pattern taxesPattern = Pattern.compile("[A-Z]{0,3}ES");
	public static Pattern ofAllPattern = Pattern.compile("[A-Z]{0,1}\\s{0,2}[A-Z]{0,1}LL");
	public static Pattern rsPattern = Pattern.compile("R[A-Za-z]");
	public static Pattern rsWildCardPattern = Pattern.compile("R.");
	public static Pattern dotZeroPattern = Pattern.compile("[.]{1}[0O]{2}");

	private static boolean inYearReplaceZWith2 = true;
	private static boolean inYearReplace7With1 = true;
	private static boolean inYearReplace1With7 = true;
	private static boolean inYearReplace6With5 = true;
	private static boolean inYearReplace8With9 = true;

	private static Object lock = new Object();

	public static void initialise() {
		// System.out.println("Entered ProductPriceData.initialise()");
		if (pkInstance == null) {
			pkInstance = new ProductPriceData();
		}
	}

	private ProductPriceData() {
		loadProductsAndPrices();
		loadMonths();
		loadYears();
	}

	public static void loadMonths() {
		useCurrentMonthOnly = !useAllMonthsValue.equals(System.getProperty(whichMonthKey));
		useCurrentYearOnly = !useMoreThanCurrentYear.equals(System.getProperty(whichMonthKey));
		months.clear();
		if (!useCurrentMonthOnly) {
			// LOAD ALL MONTHS

			// this.months.add("January");
			// this.months.add("February");
			// this.months.add("March");
			// this.months.add("April");
			// this.months.add("May");
			// this.months.add("June");
			// this.months.add("July");
			// this.months.add("August");
			// this.months.add("September");
			// this.months.add("October");
			// this.months.add("November");
			// this.months.add("December");

			months.add("JANUARY");
			months.add("FEBRUARY");
			months.add("MARCH");
			months.add("APRIL");
			months.add("MAY");
			months.add("JUNE");
			months.add("JULY");
			months.add("AUGUST");
			months.add("SEPTEMBER");
			months.add("OCTOBER");
			months.add("NOVEMBER");
			months.add("DECEMBER");

		} else {
			// load this month and previous month
			String[] monthName = { "DECEMBER", "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST",
					"SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER" };
			int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
			months.add(monthName[month]);
			months.add(monthName[month - 1]);
		}
	}

	public static void loadYears() {
		years.clear();
		inYearReplaceZWith2 = true;
		inYearReplace7With1 = true;
		inYearReplace1With7 = true;
		inYearReplace6With5 = true;
		inYearReplace8With9 = true;
		if (useCurrentYearOnly) {
			// LOAD THIS YEAR ONLY, EXCEPT IF THE CURRENT MONTH IS JANUARY, IN WHICH CASE
			// LOAD THE PREVIOUS YEAR AS WELL
			if (Calendar.getInstance().get(Calendar.MONTH) == 0) {
				years.add(Calendar.getInstance().get(Calendar.YEAR));
				years.add(Calendar.getInstance().get(Calendar.YEAR) - 1);
			} else {
				years.add(Calendar.getInstance().get(Calendar.YEAR));
			}
			StringBuffer sb = new StringBuffer();
			for (int year : years) {
				sb.append(year + " ");
			}
			String yearString = sb.toString();
			if (yearString.indexOf("2") != -1) {
				inYearReplaceZWith2 = true;
			} else {
				inYearReplaceZWith2 = false;
			}
			if ((yearString.indexOf("7") != -1) && (yearString.indexOf("1") != -1)) {
				inYearReplace7With1 = false;
				inYearReplace1With7 = false;
			} else {
				if ((yearString.indexOf("7") == -1) && (yearString.indexOf("1") != -1)) {
					inYearReplace7With1 = true;
					inYearReplace1With7 = false;
				} else {
					if ((yearString.indexOf("1") == -1) && (yearString.indexOf("7") != -1)) {
						inYearReplace1With7 = true;
						inYearReplace7With1 = false;
					} else {
						inYearReplace1With7 = true;
						inYearReplace7With1 = true;
					}
				}
			}
			if (yearString.indexOf("6") == -1) {
				inYearReplace6With5 = true;
			} else {
				inYearReplace6With5 = false;
			}
			if (yearString.indexOf("8") == -1) {
				inYearReplace8With9 = true;
			} else {
				inYearReplace8With9 = false;
			}
		} else {
			years.add(2017);
			years.add(2018);
			years.add(2019);
			years.add(2020);
			years.add(2021);
			years.add(2022);
			StringBuffer sb = new StringBuffer();
			for (int year : years) {
				sb.append(year + " ");
			}
			String yearString = sb.toString();
			if (yearString.indexOf("2") != -1) {
				inYearReplaceZWith2 = true;
			} else {
				inYearReplaceZWith2 = false;
			}
			if ((yearString.indexOf("7") != -1) && (yearString.indexOf("1") != -1)) {
				inYearReplace7With1 = false;
				inYearReplace1With7 = false;
			} else {
				if ((yearString.indexOf("7") == -1) && (yearString.indexOf("1") != -1)) {
					inYearReplace7With1 = true;
					inYearReplace1With7 = false;
				} else {
					if ((yearString.indexOf("1") == -1) && (yearString.indexOf("7") != -1)) {
						inYearReplace1With7 = true;
						inYearReplace7With1 = false;
					} else {
						inYearReplace1With7 = true;
						inYearReplace7With1 = true;
					}
				}
			}
			if (yearString.indexOf("6") == -1) {
				inYearReplace6With5 = true;
			} else {
				inYearReplace6With5 = false;
			}
			if (yearString.indexOf("8") == -1) {
				inYearReplace8With9 = true;
			} else {
				inYearReplace8With9 = false;
			}
		}
		// System.out.println("inYearReplaceZWith2 = " + inYearReplaceZWith2);
		// System.out.println("inYearReplace7With1 = " + inYearReplace7With1);
		// System.out.println("inYearReplace1With7 = " + inYearReplace1With7);
		// System.out.println("inYearReplace6With5 = " + inYearReplace6With5);
		// System.out.println("inYearReplace8With9 = " + inYearReplace8With9);
	}

	public static void loadProductsAndPrices() {
		List<String> lines = Collections.emptyList();

		/*
		 * try { Class.forName("net.ucanaccess.jdbc.UcanaccessDriver"); } catch
		 * (ClassNotFoundException cnfex) { System.out.println("Problem in loading" +
		 * " MS Access JDBC driver"); cnfex.printStackTrace(); }
		 *
		 * String databaseURL = "jdbc:ucanaccess://" + this.databasePath + this.dbName;
		 *
		 * try (Connection connection = DriverManager.getConnection(databaseURL)) {
		 *
		 * Statement statement = connection.createStatement(); ResultSet result =
		 * statement.executeQuery(this.query);
		 *
		 * while (result.next()) { String groupName = result.getString("GroupName");
		 * String modelName = result.getString("ModelName"); String price =
		 * result.getString("Price"); String characterHeight =
		 * result.getString("CharacterHeight"); String aLine = groupName + " " +
		 * modelName + "," + price + "," + characterHeight; lines.add(aLine); }
		 *
		 * } catch (SQLException ex) { ex.printStackTrace(); } // NOTE THAT finally
		 * BLOCK IS NOT REQUIRED FOR CLOSING RESULTSET, STATEMENT, AND // CONNECTION AS
		 * THE CONNECTION AND ASSOCIATE RESOURCES WILL AUTOMATICALLY GET // CLOSED
		 * BECAUSE OF "TRY WITH"
		 */

		try {
			lines = Files.readAllLines(Paths
					.get(System.getProperty(pricemasterFolderKey, System.getProperty(inputFolderKey) + "/Price Master")
							+ File.separator + System.getProperty(pricemasterFileKey, pricemasterDefaultFile)),
					StandardCharsets.UTF_8);
			// System.out.println("Read Price Master");
		} catch (IOException e) {
			SysOutController.resetSysOut();
			e.printStackTrace();
			System.out.println("");
			System.out.println("");
			System.out.println(
					"Could not find or read Price Master file - " + System.getProperty(inputFolderKey) + "/Price Master"
							+ File.separator + System.getProperty(pricemasterFileKey, pricemasterDefaultFile));
			System.out.println("Please check directory and file paths in initialisation file.");
			System.exit(0);
		}

		boolean hasHeader = pricemasterHasHeaderValue
				.contentEquals(System.getProperty(pricemasterHeaderKey, pricemasterHeaderDefault));

		boolean dataIsFine = true;
		ArrayList<String> problemLines = new ArrayList<>();
		String delimiters = "[,]";
		int lineNumber = 0;
		for (String line : lines) {
			// System.out.println(line);
			if (hasHeader && (lineNumber == 0)) {
				++lineNumber;
				continue;
			}
			String[] tokens = line.split(delimiters);
			int tokenNumber = 0;
			for (String token : tokens) {
				switch (tokenNumber) {
				case 0:
					locations.add(token.replace("(", " ").replace(")", " ").replace("    ", " ").replace("   ", " ")
							.replace("  ", " ").replace("[", " ").replace("]", " ").replace("{", " ").replace("}", " ")
							.replace("-", " ").replace("_", "").toUpperCase());
					++tokenNumber;
					break;
				case 1:
					originalProductStrings.add(token);
					products.add(token.replace("(", " ").replace(")", " ").replace("[", " ").replace("]", " ")
							.replace("{", " ").replace("}", " ").replace("-", " ").replace("_", "").replace("    ", " ")
							.replace("   ", " ").replace("  ", " ").trim().toUpperCase());
					++tokenNumber;
					break;
				case 2:
					int price = 0;
					try {
						price = Integer.parseInt(token);
						prices.add(price);
					} catch (Exception e) {
						try {
							float floatPrice = Float.parseFloat(token);
							price = (int) floatPrice;
						} catch (Exception e1) {
							dataIsFine = false;
							problemLines.add("Error in Price Master at product line - " + (lineNumber)
									+ ". Incorrect price. Didn't find a number.");
						}
					}
					// int noOfDigits = (int) Math.floor(Math.log10(price));
					// if (!this.priceDigits.contains(noOfDigits)) {
					// this.priceDigits.add(noOfDigits);
					// }
					++tokenNumber;
					break;
				case 3:
					int height = 0;
					try {
						height = Integer.parseInt(token);
						originalProductHeights.add(height);
					} catch (Exception e) {
						dataIsFine = false;
						problemLines.add("Error in Price Master at product line - " + (lineNumber)
								+ ". Incorrect height. Didn't find a number.");
					}
					// int noOfDigits = (int) Math.floor(Math.log10(price));
					// if (!this.priceDigits.contains(noOfDigits)) {
					// this.priceDigits.add(noOfDigits);
					// }
					++tokenNumber;
					break;
				default:
					++tokenNumber;
				}
			}
			if ((tokenNumber <= 3) && (tokenNumber != 0)) {
				dataIsFine = false;
				problemLines.add("Error in Price Master at product line - " + (lineNumber)
						+ ". Insufficient data points: Expected 4 or more. Found " + (tokenNumber));
			}
			++lineNumber;
			// Collections.sort(this.priceDigits);
		}

		if (!dataIsFine) {
			SysOutController.resetSysOut();
			System.out.println("");
			System.out.println("");
			for (String problem : problemLines) {
				System.out.println(problem);
			}
			System.exit(0);
		}

		for (String product : products) {
			Matcher matcher1 = null;
			Matcher matcher2 = null;
			synchronized (lock) {
				matcher1 = capacity1.matcher(product);
				matcher2 = capacity2.matcher(product);
			}
			String product1 = null;

			// first, do this match, then do matcher 1 ! Doing it the other way round is a
			// bug !!
			if (matcher2.find()) {
				int start = matcher2.start();
				int end = matcher2.end();
				String capacity = product.substring(start, end).trim();
				// System.out.println("Found a match in matcher2 : " + capacity);
				String capacityCrunched = capacity.replace(" ", "");
				Set<String> variants = capacityVariants.get(capacityCrunched);
				String alternateCapacity = null;
				if (capacityCrunched.length() >= 4) {
					if ((capacityCrunched.indexOf("M") == -1) && (capacityCrunched.indexOf("C") == -1)
							&& (capacityCrunched.indexOf("T") == -1)) {

						if ("0".equals(capacityCrunched.substring(2, 3))) {
							alternateCapacity = capacityCrunched.charAt(0) + "" + capacityCrunched.charAt(3);
						}
					}
				} else {
					if (capacityCrunched.length() == 2) {
						alternateCapacity = capacityCrunched.substring(0, 1) + ".0" + capacityCrunched.substring(1, 2);
					} else {
						if (capacityCrunched.indexOf("T") == -1) {
							alternateCapacity = capacityCrunched.substring(0, 1) + capacityCrunched.substring(1, 2)
									+ ".0" + capacityCrunched.substring(2, 3);
						} else {
							alternateCapacity = capacityCrunched.substring(0, 1) + ".0"
									+ capacityCrunched.substring(1, 3);
						}
					}
				}
				if (variants == null) {
					variants = new HashSet<>();
					variants.add(capacityCrunched);
					if (alternateCapacity != null) {
						variants.add(alternateCapacity);
					}
					capacityVariants.put(capacityCrunched, variants);
				} else {
					variants.add(capacityCrunched);
					if (alternateCapacity != null) {
						variants.add(alternateCapacity);
					}
				}
				product1 = product.replace(capacity, capacityCrunched);

			} else {
				if (matcher1.find()) {
					int start = matcher1.start();
					int end = matcher1.end();
					String capacity = product.substring(start, end).trim();
					String capacityCrunched = capacity.replace(" ", "");
					String alternateCapacity = null;
					if ((capacityCrunched.indexOf("M") == -1) && (capacityCrunched.indexOf("C") == -1)
							&& (capacityCrunched.indexOf("T") == -1)) {
						if (capacityCrunched.length() == 2) {
							alternateCapacity = capacityCrunched.charAt(0) + ".0" + capacityCrunched.charAt(1);
						} else {
							alternateCapacity = capacityCrunched.charAt(0) + capacityCrunched.charAt(1) + ".0"
									+ capacityCrunched.charAt(2);
						}
					}
					Set<String> variants = capacityVariants.get(capacityCrunched);
					if (variants == null) {
						variants = new HashSet<>();
						variants.add(capacityCrunched);
						if (alternateCapacity != null) {
							variants.add(alternateCapacity);
						}
						capacityVariants.put(capacityCrunched, variants);
					} else {
						variants.add(capacityCrunched);
						if (alternateCapacity != null) {
							variants.add(alternateCapacity);
						}
					}
					product1 = product.replace(capacity, capacityCrunched);
				} else {
					product1 = product;
				}
			}

			String[] words = product1.split("\\s+");
			for (String word : words) {
				if (word.length() > 1) {
					boolean validWord = true;
					for (String excluded : uniqueProductStringExclusions) {
						if (excluded.equals(word)) {
							validWord = false;
							break;
						}
					}
					if (validWord) {
						uniqueProductStrings.add(word);
					}
				}
			}
		}
		// System.out.println(this.locations);
		// System.out.println(this.products);
		// System.out.println(this.prices);
		// System.out.println(this.priceDigits);

		if (ReadLabelsMultiThreaded.debugLevel <= 2) {
			System.out.println("Unique Product Strings are : ");
			System.out.println(uniqueProductStrings);
			System.out.println("Capacity Variants are : ");
			System.out.println(capacityVariants);
		}

		if (ReadLabelsMultiThreaded.debugLevel <= 1) {
			System.out.println("Index" + " Product                                         " + "            Price");
			System.out.println("-----" + "-------------------------------------------------" + "-----------------");
			for (int i = 0; i < products.size(); ++i) {
				System.out.print(i + " ");
				System.out.print(products.get(i) + "    ");
				System.out.println(prices.get(i));
			}
		}

		for (String aProduct : originalProductStrings) {
			String[] noOfWords = aProduct.split("\\s+");
			noOfWordsInProduct.add(noOfWords.length);
		}

		CheckProductProperties.checkIfProductIsGiven();
	}

	/*
	 * private static void checkIfProductIsGiven() {
	 *
	 * boolean givenProduct =
	 * "true".equals(System.getProperty(productNameNeededKey)); if (givenProduct !=
	 * productIsGiven) { System.out.println("productname.fixed changed from " +
	 * productIsGiven + " to " + givenProduct); } productIsGiven = givenProduct;
	 * givenProductPrice = 0; boolean productFound = false; if (productIsGiven) {
	 * String whichProduct = System.getProperty(productNameKey); int index = 0; for
	 * (String originalProductString : originalProductStrings) { if
	 * (originalProductString.equals(whichProduct)) { givenProductPrice =
	 * prices.get(index); productFound = true; break; } ++index; } } else {
	 * productFound = true; } if (!productFound) {
	 * System.out.println("-------------------------------"); System.out.println(
	 * "Incorrect product name in product.properties. Product name in product.properties must match exactly with a product name from Price Master Summary."
	 * ); } }
	 */

	static class StringAndPixelHeight {
		String string2;
		int pixelHeight2;
		String originalString = "";
		boolean monthFound = true;
		ArrayList<Rectangle> boundingBoxes = null;

		StringAndPixelHeight(String string1, int pixelHeight1) {
			this.string2 = string1;
			this.pixelHeight2 = pixelHeight1;
		}

		StringAndPixelHeight(String string1, int pixelHeight1, String original) {
			this.string2 = string1;
			this.pixelHeight2 = pixelHeight1;
			this.originalString = original;
		}

		StringAndPixelHeight(String string1, int pixelHeight1, boolean monthFound1) {
			this.string2 = string1;
			this.pixelHeight2 = pixelHeight1;
			this.monthFound = monthFound1;
		}

		StringAndPixelHeight(String string1, int pixelHeight1, String original, boolean monthFound1) {
			this.string2 = string1;
			this.pixelHeight2 = pixelHeight1;
			this.monthFound = monthFound1;
			this.originalString = original;
		}

		@Override
		public String toString() {
			StringBuffer out = new StringBuffer();
			out.append(this.string2).append(";").append(this.pixelHeight2).append(" pixels;")
					.append(this.originalString).append(";").append("monthFound = ").append(this.monthFound).append(";")
					.append(this.boundingBoxes);
			return out.toString();
		}
	}

	public static Entry<Integer, StringAndPixelHeight> processForMonth(String line, int likelyPixelHeight, int debugL) {
		// String lineUpper = line.replace(" ", "").replace("(", "").replace(")",
		// "").replace("[", "").replace("]", "")
		// .replace("-", "").replace("_", "").replace(",", "").toUpperCase();
		String lineUpper = line.replace("(", " ").replace(")", " ").replace("[", " ").replace("]", " ")
				.replace("-", " ").replace("_", " ").replace(".", " ").replace(",", " ").replace("{", " ")
				.replace("'", " ").replace("\"", " ").replace("}", " ").replace(" ", "").replace("/", "")
				.replace("|", "").replace("\\", "").replace("1U", "").replace("2U", "").replace("3U", "")
				.replace("4U", "").replace("5U", "").trim().replace("PRICE", "").replace("RETAIL", "").toUpperCase();

		Matcher priceMatcher = null;
		synchronized (lock) {
			priceMatcher = pricePattern.matcher(lineUpper);
		}
		if (priceMatcher.find()) {
			int start = priceMatcher.start();
			int end = priceMatcher.end();
			String tobeReplaced = lineUpper.substring(start, end).trim();
			lineUpper = lineUpper.replace(tobeReplaced, "");
		}

		Matcher retailMatcher = null;
		synchronized (lock) {
			retailMatcher = retailPattern.matcher(lineUpper);
		}
		if (retailMatcher.find()) {
			int start = retailMatcher.start();
			int end = retailMatcher.end();
			String tobeReplaced = lineUpper.substring(start, end).trim();
			lineUpper = lineUpper.replace(tobeReplaced, "");
		}

		Matcher taxesMatcher = null;
		synchronized (lock) {
			taxesMatcher = taxesPattern.matcher(lineUpper);
		}
		if (taxesMatcher.find()) {
			int start = taxesMatcher.start();
			int end = taxesMatcher.end();
			String tobeReplaced = lineUpper.substring(start, end).trim();
			lineUpper = lineUpper.replace(tobeReplaced, "");
		}

		Matcher ofAllMatcher = null;
		synchronized (lock) {
			ofAllMatcher = ofAllPattern.matcher(lineUpper);
		}
		if (ofAllMatcher.find()) {
			int start = ofAllMatcher.start();
			int end = ofAllMatcher.end();
			String tobeReplaced = lineUpper.substring(start, end).trim();
			lineUpper = lineUpper.replace(tobeReplaced, "");
		}

		for (Pattern aPattern : ttkPrestigeLtd) {
			Matcher aMatcher = null;
			synchronized (lock) {
				aMatcher = aPattern.matcher(lineUpper);
			}
			if (aMatcher.find()) {
				int start = aMatcher.start();
				int end = aMatcher.end();
				String tobeReplaced = lineUpper.substring(start, end).trim();
				lineUpper = lineUpper.replace(tobeReplaced, "");
			}
		}

		if (debugL <= 2) {
			System.out.println("In processForMonth() : cleaned:" + line + " down to :" + lineUpper);
		}

		// System.out.println("Processing " + lineUpper + " for Date");
		if (lineUpper.length() >= 3) {
			// StringBuffer date = new StringBuffer();
			TreeMap<Integer, StringAndPixelHeight> options = new TreeMap<>();
			// hack for eliminating a match between CUST and AUGUST. If lineUpper contains
			// "CUST", then downgrade score by 15 points
			int scoreDowngrade = 0;
			if ((lineUpper.indexOf("CUST")) != -1) {
				scoreDowngrade = 15;
			}
			int cutOff = 65;
			for (String month : months) {
				int searchValue = FuzzySearch.weightedRatio(month, lineUpper) - scoreDowngrade;

				if (month.length() <= lineUpper.length()) {
					cutOff = 75;
				}
				if ((((lineUpper.length() * 1.0) / month.length()) < 0.75)) {
					searchValue = (int) ((searchValue * lineUpper.length() * 1.0) / month.length());
				}
				if (debugL <= 2) {
					System.out.println("Comparison between " + lineUpper + " and " + month + " gives searchValue = "
							+ searchValue);
				}
				// if ((searchValue > 58) && (month.length() <= lineUpper.length())) {
				if (searchValue > cutOff) {
					options.put(searchValue, new StringAndPixelHeight(month, likelyPixelHeight, line));
					// System.out
					// .println("Fuzzy Search value = " + searchValue + "; month = " + month + " in
					// " + lineUpper);
				}
			}
			if (debugL <= 2) {
				System.out.println("Month and Search values found are - " + options);
			}
			if (options.size() > 0) {
				// Entry<Integer, String> last = options.lastEntry();
				Entry<Integer, StringAndPixelHeight> last = options.floorEntry(100);
				// String bestMonthMatch = last.getValue();
				// date.append(bestMonthMatch).append(" ");
				// return date.toString();
				if (debugL <= 2) {
					System.out.println("Best month match found is - " + last);
				}
				return last;
			} else {
				// no month found that matches exactly
				// find if the line has 4 consecutive digits
				if (debugL <= 2) {
					System.out.println("No month match found. Starting secondary processing to find a month match.");
				}
				TreeMap<StringAndPixelHeight, Integer> monthOptions = new TreeMap<>(
						new Comparator<StringAndPixelHeight>() {
							@Override
							public int compare(StringAndPixelHeight a, StringAndPixelHeight b) {
								int comparison = a.string2.length() - b.string2.length();
								return comparison;
							}
						});
				Pattern sequence = Pattern.compile("\\d{4,5}");
				Matcher matcher = null;
				synchronized (lock) {
					matcher = sequence.matcher(lineUpper);
				}
				if (matcher.find()) {
					if (debugL <= 2) {
						System.out.println("Exploring a 4-digit match in line for month - " + lineUpper);
					}
					int start = matcher.start();
					int end = matcher.end();
					// remove year from lineUpper, leaving it with only the month
					String monthPrinted = (lineUpper.substring(0, start) + lineUpper.substring(end, lineUpper.length()))
							.trim();
					// iterate through the months to check if there is a match with a month
					if (debugL <= 2) {
						System.out.println("In ProductPriceData.processForMonth() - Now finding a month match for "
								+ monthPrinted);
					}
					// first check if the last 3 letters of the month match
					for (String month : months) {
						String last2Letters = null;
						if (monthPrinted.length() < 2) {
							continue;
						}
						last2Letters = month.substring(month.length() - 2);
						if (monthPrinted.endsWith(last2Letters)) {
							monthOptions.put(new StringAndPixelHeight(month, likelyPixelHeight, line, false), 40);
						}

						String last3Letters = null;
						if (monthPrinted.length() < 3) {
							continue;
						}
						last3Letters = month.substring(month.length() - 3);
						if (monthPrinted.endsWith(last3Letters)) {
							monthOptions.put(new StringAndPixelHeight(month, likelyPixelHeight, line, false), 50);
						}
						String last4Letters = null;
						if (monthPrinted.length() < 4) {
							continue;
						}
						if (month.length() >= 4) {
							last4Letters = month.substring(month.length() - 4);
							if (monthPrinted.endsWith(last4Letters)) {
								monthOptions.put(new StringAndPixelHeight(month, likelyPixelHeight, line, false), 60);
							}
						}
						String last5Letters = null;
						if (monthPrinted.length() < 5) {
							continue;
						}
						if (month.length() >= 5) {
							last5Letters = month.substring(month.length() - 5);
							if (monthPrinted.endsWith(last5Letters)) {
								monthOptions.put(new StringAndPixelHeight(month, likelyPixelHeight, line, false), 70);
							}
						}
						String last6Letters = null;
						if (monthPrinted.length() < 6) {
							continue;
						}
						if (month.length() >= 6) {
							last6Letters = month.substring(month.length() - 6);
							if (monthPrinted.endsWith(last6Letters)) {
								monthOptions.put(new StringAndPixelHeight(month, likelyPixelHeight, line, false), 80);
							}
						}
					}

					ArrayList<StringAndPixelHeight> possibleMonths = new ArrayList<>();

					// find the highest score of the shortlisted monthOptions
					Set<Map.Entry<StringAndPixelHeight, Integer>> optionEntries = monthOptions.entrySet();
					int highestScore = 0;
					for (Map.Entry<StringAndPixelHeight, Integer> anOption : optionEntries) {
						if (anOption.getValue() > highestScore) {
							highestScore = anOption.getValue();
						}
					}

					// get the month options that equal the highest score
					for (Map.Entry<StringAndPixelHeight, Integer> anOption : optionEntries) {
						if (anOption.getValue() == highestScore) {
							possibleMonths.add(anOption.getKey());
						}
					}

					// if only one month is found, then return that month e.g. if RUARY is read,
					// then return FEBRUARY. However, if UARY is read, then it could be JANUARY or
					// FEBRUARY - don't return any month in this case
					if (possibleMonths.size() == 1) {
						options.clear();
						options.put(highestScore, possibleMonths.get(0));
						Entry<Integer, StringAndPixelHeight> last = options.floorEntry(100);
						if (debugL <= 2) {
							System.out.println(
									"In ProductPriceData.processForMonth() secondary processing - About to return "
											+ last.getValue().string2);
						}
						return last;
					}
				} else {
					if (debugL <= 2) {
						System.out
								.println("No month match found. Finished secondary processing to find a month match.");
					}
				}
			}
		}
		return null;
	}

	public static String processForYear(String line, int debugL) {
		String lineUpper = line.replace("(", " ").replace(")", " ").replace("[", " ").replace("]", " ")
				.replace("-", " ").replace("_", " ").replace(",", " ").replace("{", " ").replace("}", " ")
				.replace("'", " ").replace("\"", " ").replace(" ", "").replace("S", "5").replace("s", "5").trim()
				.toUpperCase();

		if (debugL <= 2) {
			System.out.println("In processForYear() :");
			System.out.println("	inYearReplaceZWith2 = " + inYearReplaceZWith2);
			System.out.println("	inYearReplace7With1 = " + inYearReplace7With1);
			System.out.println("	inYearReplace1With7 = " + inYearReplace1With7);
			System.out.println("	inYearReplace6With5 = " + inYearReplace6With5);
			System.out.println("	inYearReplace8With9 = " + inYearReplace8With9);
		}

		if (inYearReplaceZWith2) {
			lineUpper = lineUpper.replace("Z", "2");
		}
		if (inYearReplace7With1) {
			lineUpper = lineUpper.replace("7", "1");
		}
		if (inYearReplace1With7) {
			lineUpper = lineUpper.replace("1", "7");
		}

		if (inYearReplace6With5) {
			lineUpper = lineUpper.replace("6", "5");
		}
		if (inYearReplace8With9) {
			lineUpper = lineUpper.replace("8", "9");
		}
		if (debugL <= 2) {
			System.out.println("In processForYear(): Changed original string : " + line
					+ " and processing year match in : " + lineUpper);
		}
		// System.out.println("Processing " + lineUpper + " for Date");
		// first, do a straightforward string matching
		if (lineUpper.length() >= 7) {
			StringBuffer date = new StringBuffer();
			for (Integer year : years) {
				if (lineUpper.indexOf(year.toString()) != -1) {
					date.append(year);
					// System.out.println(date);
					if (debugL <= 2) {
						System.out.println("In processForYear(): Processing year in " + lineUpper + ". Found year - "
								+ date.toString());
					}
					return date.toString();
				}
			}
		}

		// if the above doesn't return, then use pattern matching
		Pattern sequence = Pattern.compile("\\d{4}");
		Matcher matcher = null;
		synchronized (lock) {
			matcher = sequence.matcher(lineUpper);
		}
		if (matcher.find()) {
			StringBuffer date = new StringBuffer();
			int start = matcher.start();
			int end = matcher.end();
			// get year from lineUpper
			String yearPrinted = lineUpper.substring(start, end).trim();
			if (debugL <= 2) {
				System.out.println(
						"In processForYear(): Processing year in " + lineUpper + ". Found 4 digits - " + yearPrinted);
			}
			// System.out.println("IN ProductPriceData.PROCESSFORYEAR(). FOUND
			// yearPrinted
			// is - " + yearPrinted);
			// iterate through the years to check if there is a match with a year
			for (Integer year : years) {
				if (year.equals(Integer.parseInt(yearPrinted))) {
					date.append(year);
					// System.out.println(date);
					if (debugL <= 2) {
						System.out.println("In processForYear(): Processing year in " + lineUpper + ". Found year - "
								+ date.toString());
					}
					return date.toString();
				}
			}
			// if not found, replace a 4 in the 3rd digits place with a 1, and redo
			if ("4".equals(yearPrinted.substring(2, 3))) {
				// System.out.println("IN ProductPriceData.PROCESSFORYEAR(). FOUND
				// yearPrinted
				// has 4 as the 3rd digit");
				yearPrinted = yearPrinted.replace('4', '1');
			}
			// System.out.println("IN ProductPriceData.PROCESSFORYEAR(). CHANGED
			// yearPrinted
			// to - " + yearPrinted);
			for (Integer year : years) {
				if (year.equals(Integer.parseInt(yearPrinted))) {
					date.append(year);
					// System.out.println(date);
					if (debugL <= 2) {
						System.out.println("In processForYear(): Processing year in " + lineUpper + ". Found year - "
								+ date.toString());
					}
					return date.toString();
				}
			}
		}
		if (debugL <= 2) {
			System.out.println("In processForYear(): Processing year in " + lineUpper + ". Did not find year.");
		}
		return null;
	}

	static class IndexAndPixelHeight {
		int index;
		int pixelHeight1;
		ArrayList<Rectangle> boundingBoxes;
		String originalString;

		IndexAndPixelHeight(int index1, int pixelHeight2, ArrayList<Rectangle> boundingBoxes1) {
			this.index = index1;
			this.pixelHeight1 = pixelHeight2;
			this.boundingBoxes = boundingBoxes1;
		}

		IndexAndPixelHeight(int index1, int pixelHeight2, ArrayList<Rectangle> boundingBoxes1, String line) {
			this.index = index1;
			this.pixelHeight1 = pixelHeight2;
			this.boundingBoxes = boundingBoxes1;
			this.originalString = line;
		}

		@Override
		public String toString() {
			return new StringBuffer().append("Index = ").append(this.index).append("; Pixel Height = ")
					.append(this.pixelHeight1).append("; ").append(this.originalString).append("; ")
					.append(this.boundingBoxes).toString();
		}
	}

	public static ArrayList<IndexAndPixelHeight> processForPrice(String line, int pixelHeight,
			ArrayList<Rectangle> boundingBoxes, ArrayList<IndexAndPixelHeight> matchingIndices, int debugL) {
		String lineUpper = rationalisePriceSearchString(line);

		if (CheckProductProperties.productIsGiven) {
			String priceString = Integer.toString(CheckProductProperties.givenProductPrice);
			if (lineUpper.indexOf(priceString) == -1) {
				return matchingIndices;
			}
		}

		if (debugL <= 2) {
			System.out.println("In processForPrice() : Changed " + line + " to: " + lineUpper);
		}

		Matcher matcher = null;
		synchronized (lock) {
			matcher = numberSequence.matcher(lineUpper);
		}

		/**
		 * if (matcher.groupCount() >= 0) { // check if this should be > 0 return true;
		 * }
		 **/
		boolean foundPrice = false;
		while (matcher.find()) {
			String priceAsString = matcher.group();
			int price = 0;
			try {
				price = Integer.parseInt(priceAsString);
			} catch (Exception e) {
				price = 0;
			}
			// System.out.println("Found price = " + price);
			int noOfDigits = (int) Math.ceil(Math.log10(price));
			if (noOfDigits >= 4) {
				int difference = noOfDigits - 4;
				price = (int) (price / Math.pow(10, difference));
			}
			// System.out.println("Changed price to = " + price);
			if (CheckProductProperties.productIsGiven) {
				if (price != CheckProductProperties.givenProductPrice) {
					continue;
				}
			}
			int index = 0;
			for (int productPrice : prices) {
				if (price == productPrice) {
					foundPrice = true;
					boolean foundDuplicate = false;
					for (IndexAndPixelHeight entry : matchingIndices) {
						if (entry.index == index) {
							// removed this line - we need all lines to be shortlisted so that we can choose
							// the best out of them for OCRPriceDimensions
							// foundDuplicate = true;
						}
					}
					if (!foundDuplicate) {
						// System.out.println("Found price = Rs " + price + "; Adding index = " +
						// index);
						// System.out.println("Found price = Rs " + price);
						matchingIndices.add(new IndexAndPixelHeight(index, pixelHeight, boundingBoxes, line));
					}
				}
				++index;
			}
		}

		if (!foundPrice) {
			// if no price was found, then replace S with 5 and retry
			lineUpper = lineUpper.replace("S", "5");
			Matcher matcher1 = null;
			synchronized (lock) {
				matcher1 = numberSequence.matcher(lineUpper);
			}
			/**
			 * if (matcher.groupCount() >= 0) { // check if this should be > 0 return true;
			 * }
			 **/
			while (matcher1.find()) {
				String priceAsString = matcher1.group();
				int price = 0;
				try {
					price = Integer.parseInt(priceAsString);
				} catch (Exception e) {
					price = 0;
				}
				// System.out.println("Found price = " + price);
				int noOfDigits = (int) Math.ceil(Math.log10(price));
				if (noOfDigits >= 4) {
					int difference = noOfDigits - 4;
					price = (int) (price / Math.pow(10, difference));
				}
				// System.out.println("Changed price to = " + price);
				if (CheckProductProperties.productIsGiven) {
					if (price != CheckProductProperties.givenProductPrice) {
						continue;
					}
				}
				int index = 0;
				for (int productPrice : prices) {
					if (price == productPrice) {
						foundPrice = true;
						boolean foundDuplicate = false;
						for (IndexAndPixelHeight entry : matchingIndices) {
							if (entry.index == index) {
								// foundDuplicate = true;
							}
						}
						if (!foundDuplicate) {
							// System.out.println("Found price = Rs " + price + "; Adding index = " +
							// index);
							// System.out.println("Found price = Rs " + price);
							matchingIndices.add(new IndexAndPixelHeight(index, pixelHeight, boundingBoxes, line));
						}
					}
					++index;
				}
			}
		}
		return matchingIndices;
	}

	public int findProductIndexUsingSift4(String line) {
		String lineUpper = line.replace("    ", " ").replace("   ", " ").replace("  ", " ").replace("(", "")
				.replace(")", "").replace("[", "").replace("]", "").replace("-", "").replace("_", "").toUpperCase();
		// System.out.println("Processing " + lineUpper + " for Product");
		Sift4 sift4 = new Sift4();
		sift4.setMaxOffset(20);
		double ratio = 1.0;
		int currentIndex = 0;
		int bestIndex = -1;
		for (String product : products) {
			double currentResult = sift4.distance(lineUpper, product);
			double currentRatio = currentResult / product.length();
			if (currentRatio < ratio) {
				ratio = currentRatio;
				bestIndex = currentIndex;
			}
			++currentIndex;
		}

		if (ratio < 0.25) {
			return bestIndex;
		}

		return -1;
	}

	public static List<ExtractedResult> findProductIndexUsingFuzzyWuzzy(String line, int cutOffIndex, int debugL) {

		String lineUpper = line.trim().replace("    ", " ").replace("   ", " ").replace("  ", " ").replace("(", "")
				.replace(")", "").replace("[", "").replace("]", "").replace("-", "").replace("_", "").toUpperCase();
		// int result = 0;
		// int currentIndex = 0;
		// int bestIndex = -1;

		// for (String product : this.products) {
		// int currentResult = FuzzySearch.weightedRatio(product, lineUpper);
		// if (currentResult > result) {
		// result = currentResult;
		// bestIndex = currentIndex;
		// }
		// ++currentIndex;
		// }

		// if (result > 0.25) {
		// return bestIndex;
		// }
		if (debugL <= 2) {
			System.out.println("Finding product match for " + lineUpper);
		}
		List<ExtractedResult> topMatches = FuzzySearch.extractTop(lineUpper, products, 15, cutOffIndex);
		if (debugL <= 2) {
			System.out.println(topMatches);
		}
		return topMatches;

	}

	public static String rectifyString(String line) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < line.length(); ++i) {
			switch (line.charAt(i)) {
			case 'A':
			case 'B':
			case 'C':
			case 'D':
			case 'E':
			case 'F':
			case 'G':
			case 'H':
			case 'I':
			case 'J':
			case 'K':
			case 'L':
			case 'M':
			case 'N':
			case 'O':
			case 'P':
			case 'Q':
			case 'R':
			case 'S':
			case 'T':
			case 'U':
			case 'V':
			case 'W':
			case 'X':
			case 'Y':
			case 'Z':
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
			case '.':
			case ',':
			case '_':
			case '-':
			case ' ':
			case '+':
				sb.append(line.charAt(i));
			default:
				break;
			}
		}
		String[] chunks = sb.toString().split("\\s+");
		StringBuffer newString = new StringBuffer();

		for (String chunk : chunks) {
			// if (chunk.length() > 1) {
			newString.append(chunk).append(" ");
			// }
		}

		String lineUpper = newString.toString().replace("(", " ").replace(")", " ").replace("[", " ").replace("]", " ")
				.replace("-", "").replace("_", "").replace("    ", " ").replace("   ", " ").replace("  ", " ")
				.replace("'", " ").replace("\"", " ").trim().toUpperCase();
		return lineUpper;
	}

	public static boolean checkIfProductLine(String line, int debugL) {
		// assumption is that 'line' has gone through rectifyString()
		if (debugL <= 2) {
			System.out.println("Checking if the following is a product line : " + line);
		}

		boolean found = false;
		Matcher matcher = null;
		for (Pattern p : productsBeginWith) {
			synchronized (lock) {
				matcher = p.matcher(line);
			}
			// if (debugL <= 1) {
			// System.out.println("Group count for " + p + " is " + matcher.groupCount());
			// }
			if (matcher.find()) {
				if (debugL <= 2) {
					System.out.println("Found product match in line : " + line);
				}
				found = true;
				break;
			}
		}
		if (!found) {
			for (Pattern p : productsHave) {
				synchronized (lock) {
					matcher = p.matcher(line);
				}
				// if (debugL <= 2) {
				// System.out.println("Group count for " + p + " is " + matcher.groupCount());
				// }
				if (matcher.find()) {
					if (debugL <= 2) {
						System.out.println("Found product match in line : " + line);
					}
					found = true;
					break;
				}
			}
		}
		if (debugL <= 2) {
			System.out.println("Result of finding product match in : " + line + " is " + found);
		}
		return found;
	}

	public static String cleanProductLine(String line, int debugL) {

		// if product match is found in a line, clean the line to remove "TTK Prestige
		// Ltd" and "Manufactured By"
		if (debugL <= 2) {
			System.out.println("Cleaning product line : " + line);
		}
		Matcher matcher = null;
		boolean ttkPresent = false;
		for (Pattern p : ttk) {
			synchronized (lock) {
				matcher = p.matcher(line);
			}
			// if (debugL <= 2) {
			// System.out.println("Group count for " + p + " is " + matcher.groupCount());
			// }
			if (matcher.find()) {
				if (debugL <= 2) {
					System.out.println("Found TTK in line : " + line);
				}
				ttkPresent = true;
				break;
			}
		}
		String tempLine = line;
		if (ttkPresent) {
			for (Pattern p : ttkPrestigeLtd) {
				synchronized (lock) {
					matcher = p.matcher(tempLine);
				}
				// if (debugL <= 2) {
				// System.out.println("Group count for " + p + " is " + matcher.groupCount());
				// }
				if (matcher.find()) {
					if (debugL <= 2) {
						System.out.println("Found TTK PRESTIGE LTD in line : " + tempLine);
					}
					// matcher.find();
					int start = matcher.start();
					int end = matcher.end();
					tempLine = tempLine.substring(0, start) + tempLine.substring(end - 1, tempLine.length());
					if (debugL <= 2) {
						System.out.println("After removing TTK PRESTIGE LTD line is : " + tempLine);
					}
				}
			}
		}

		String newLine = tempLine;
		for (Pattern p : manufacturedBy) {
			// if (debugL <= 2) {
			// System.out.println("Group count for " + p + " is " + matcher.groupCount());
			// }
			synchronized (lock) {
				matcher = p.matcher(newLine);
			}
			if (matcher.find()) {
				if (debugL <= 2) {
					System.out.println("Found MANUFACTURED BY in line : " + newLine);
				}
				// matcher.find();
				int start = matcher.start();
				int end = matcher.end();
				newLine = newLine.substring(0, start) + newLine.substring(end - 1, newLine.length());
				if (debugL <= 2) {
					System.out.println("After removing MANUFACTURED BY line is : " + newLine);
				}
			}
		}

		if (newLine == null) {
			newLine = tempLine;
		}

		for (Pattern p : oneAndTwoCharacters) {
			// if (debugL <= 2) {
			// System.out.println("Group count for " + p + " is " + matcher.groupCount());
			// }
			synchronized (lock) {
				matcher = p.matcher(newLine);
			}
			if (matcher.find()) {
				if (debugL <= 2) {
					System.out.println("Found ONE OR TWO CHARACTERS in line : " + newLine);
				}
				// matcher.find();
				int start = matcher.start();
				int end = matcher.end();
				newLine = newLine.substring(0, start) + newLine.substring(end - 1, newLine.length());
				if (debugL <= 2) {
					System.out.println("After removing ONE OR TWO CHARACTERS, line is : " + newLine);
				}
			}
		}

		/*
		 * if (newLine == null) { if (debugL <= 2) {
		 * System.out.println("Cleaned up product line is : " + tempLine); } if
		 * (this.checkIfProductLine(tempLine, debugL)) { return tempLine; } else {
		 * return null; }
		 *
		 * }
		 */

		if (debugL <= 2) {
			System.out.println("Cleaned up product line is : " + newLine);
		}
		if (checkIfProductLine(newLine, debugL)) {
			return newLine;
		}
		return null;
	}

	// not used, as checkIfProductLine is more accurate
	public String checkIfLineIsDateOrPrice(String line, Entry<Integer, String> month, String year,
			ArrayList<Integer> priceIndices, int debugL) {
		// assumption is that 'line' has gone through rectifyString()
		// check if it is a date or year line
		String newLine = line.replace(" ", "");
		int monthSearchValue = 0;
		if (month != null) {
			monthSearchValue = (month.getValue() == null) ? 0
					: (("".equals(month.getValue())) ? 0 : FuzzySearch.weightedRatio(month.getValue(), newLine));
		}
		int yearSearchValue = (year == null) ? 0 : (("".equals(year)) ? 0 : FuzzySearch.weightedRatio(year, newLine));
		if ((monthSearchValue > 85) || (yearSearchValue > 85)) {
			return dateLine;
		}

		// check if it is a price line
		for (Integer index : priceIndices) {
			String price = Integer.toString(prices.get(index));
			int priceSearchValue = FuzzySearch.weightedRatio(price, newLine);
			if (priceSearchValue > 80) {
				return priceLine;
			}
		}

		// check if it is a line that contains (TTK or TK) and (PRESTIGE OR LTD)
		int tkPresent = FuzzySearch.weightedRatio("TK", newLine);
		int prestigePresent = FuzzySearch.weightedRatio("PRESTIGE", newLine);
		int ltdPresent = FuzzySearch.weightedRatio("LTD", newLine);

		if ((tkPresent > 85) && ((prestigePresent > 85) || (ltdPresent > 85))) {
			return irrelevantLine;
		}

		return neitherPriceNorDateLine;
	}

	public ProductDescription checkValidity(ArrayList<OCRResult> ocrResults, int debugL) {

		// StringBuffer output = new StringBuffer();
		ProductDescription product = new ProductDescription();

		boolean monthFound = false;
		boolean yearFound = false;
		StringBuffer monthAndYear = new StringBuffer();
		ArrayList<IndexAndPixelHeight> priceIndices = new ArrayList<>();

		// find the best match for date and year
		ArrayList<Entry<Integer, StringAndPixelHeight>> monthMatches = new ArrayList<>();
		for (OCRResult result : ocrResults) {
			for (String sentence : result.sentences) {
				String[] lines = sentence.split("\r\n|\r|\n");
				for (String line : lines) {
					Entry<Integer, StringAndPixelHeight> month = processForMonth(line, 0, debugL);
					if (month != null) {
						monthMatches.add(month);
					}
					// if (month != null) {
					// monthAndYear.append(month).append(" ");
					// monthFound = true;
					// break outerloop;
					// }
				}
			}
		}

		Entry<Integer, StringAndPixelHeight> bestMonthMatch = null;
		for (Entry<Integer, StringAndPixelHeight> aMonth : monthMatches) {
			if (bestMonthMatch == null) {
				bestMonthMatch = aMonth;
				continue;
			}
			if (aMonth.getKey() >= bestMonthMatch.getKey()) {
				bestMonthMatch = aMonth;
			}
		}

		if (bestMonthMatch != null) {
			monthAndYear.append(bestMonthMatch.getValue().string2).append(" ");
			monthFound = true;
		}

		String year = null;
		outerloop: for (OCRResult result : ocrResults) {
			for (String sentence : result.sentences) {
				String[] lines = sentence.split("\r\n|\r|\n");
				for (String line : lines) {
					year = processForYear(line, debugL);
					if (year != null) {
						monthAndYear.append(year);
						yearFound = true;
						break outerloop;
					}
				}
			}
		}

		// find all the matches for price
		for (OCRResult result : ocrResults) {
			for (String sentence : result.sentences) {
				String[] lines = sentence.split("\r\n|\r|\n");
				for (String line : lines) {
					processForPrice(line, 0, null, priceIndices, debugL);
				}
			}
		}

		if (debugL <= 2) {
			StringBuffer out = new StringBuffer();
			for (IndexAndPixelHeight entry : priceIndices) {
				out.append("Rs ").append(prices.get(entry.index)).append(" ; ");
			}
			System.out.println("Price choices are - " + out.toString());
		}

		// find the 3 best matches for product
		// first, find all the relevant strings from the OCR results that matter
		ArrayList<String> productSubStrings = new ArrayList<>();
		for (OCRResult ocrResult : ocrResults) {
			for (String sentence : ocrResult.sentences) {
				String[] lines = sentence.split("\r\n|\r|\n");
				for (String line : lines) {
					String lineUpper = rectifyString(line);
					// boolean couldBeProductLine = neitherPriceNorDateLine.equals(
					// this.checkIfLineIsDateOrPrice(lineUpper, bestMonthMatch, year, priceIndices,
					// debugL));
					boolean couldBeProductLine = checkIfProductLine(lineUpper, debugL);
					if (couldBeProductLine) {
						String rectifiedLine = cleanProductLine(lineUpper, debugL);
						if (rectifiedLine != null) {
							List<ExtractedResult> prodMatches = null;
							if (rectifiedLine.length() > 10) {
								prodMatches = findProductIndexUsingFuzzyWuzzy(rectifiedLine, 89, debugL);
								for (ExtractedResult result : prodMatches) {
									if (!productSubStrings.contains(rectifiedLine)) {
										productSubStrings.add(rectifiedLine);
									}
								}
							}
						}
					}
				}
			}
		}

		int size = productSubStrings.size();

		if (size == 0) {
			// repeat the above sequence with a lesser cut-off
			for (OCRResult ocrResult : ocrResults) {
				for (String sentence : ocrResult.sentences) {
					String[] lines = sentence.split("\r\n|\r|\n");
					for (String line : lines) {
						String lineUpper = rectifyString(line);
						boolean couldBeProductLine = checkIfProductLine(lineUpper, debugL);
						if (couldBeProductLine) {
							String rectifiedLine = cleanProductLine(lineUpper, debugL);
							if (rectifiedLine != null) {
								List<ExtractedResult> prodMatches = null;
								if (rectifiedLine.length() > 10) {
									prodMatches = findProductIndexUsingFuzzyWuzzy(rectifiedLine, 85, debugL);
									for (ExtractedResult result : prodMatches) {
										if (!productSubStrings.contains(rectifiedLine)) {
											productSubStrings.add(rectifiedLine);
										}
									}
								}
							}
						}
					}
				}
			}
		}

		size = productSubStrings.size();

		if (size == 0) {
			// repeat the above sequence with an even lesser cut-off
			for (OCRResult ocrResult : ocrResults) {
				for (String sentence : ocrResult.sentences) {
					String[] lines = sentence.split("\r\n|\r|\n");
					for (String line : lines) {
						String lineUpper = rectifyString(line);
						boolean couldBeProductLine = checkIfProductLine(lineUpper, debugL);
						if (couldBeProductLine) {
							String rectifiedLine = cleanProductLine(lineUpper, debugL);
							if (rectifiedLine != null) {
								List<ExtractedResult> prodMatches = null;
								if (rectifiedLine.length() > 10) {
									prodMatches = findProductIndexUsingFuzzyWuzzy(rectifiedLine, 80, debugL);
									for (ExtractedResult result : prodMatches) {
										if (!productSubStrings.contains(rectifiedLine)) {
											productSubStrings.add(rectifiedLine);
										}
									}
								}
							}
						}
					}
				}
			}
		}

		size = productSubStrings.size();

		if (size == 0) {
			if (debugL <= 8) {
				System.out.println("No match found for product");
				for (IndexAndPixelHeight priceIndex : priceIndices) {
					System.out.println("Price : Rs. " + prices.get(priceIndex.index));
				}
				if (monthFound && yearFound) {
					System.out.println("Month of Manufacture : " + monthAndYear);
				}
			}

			/*
			 * output.append("false").append(separator).append("").append(separator); for
			 * (int index : priceIndices) {
			 * output.append(this.prices.get(index)).append(separator); }
			 * output.append(monthAndYear);
			 *
			 * return output.toString();
			 */
			product.productOK = ProductDescription.ERROR_PRODUCT_AND_PRICE_NOT_FOUND;
			product.productName = "";
			product.setMonth(bestMonthMatch.getValue().string2);
			product.setYear(year);
			// unique values of price needed; hence, add values to a set, and then populate
			// the arraylist from the set
			Set<Double> shortlistedPrices = new TreeSet<>();
			for (IndexAndPixelHeight priceIndex : priceIndices) {
				shortlistedPrices.add(prices.get(priceIndex.index).doubleValue());
			}
			for (Double value : shortlistedPrices) {
				product.price.add(value);
			}
			// product.price.add(this.prices.get(index).doubleValue());
			return product;

		}

		// pick unique strings from productSubStrings, and populate them in a new array
		// called productStrings
		ArrayList<String> productTempStrings = new ArrayList<>();
		outerloop: for (int i = 0; i < size; ++i) {
			int newSize = productTempStrings.size();
			String ocrString = productSubStrings.get(i);
			for (int j = 0; j < newSize; ++j) {
				// if a product string is there in the array that already matches another that
				// has been chosen, then ignore it. However, if the new string is longer than
				// the current string, then replace the current string with the new string
				if (FuzzySearch.weightedRatio(ocrString, productTempStrings.get(j)) > 70) {
					if (ocrString.length() > productTempStrings.get(j).length()) {
						productTempStrings.remove(j);
						productTempStrings.add(j, ocrString.trim());
					} else {
						continue outerloop;
					}
				}
			}
			productTempStrings.add(ocrString);
		}

		// go through the productStrings array and remove duplicates & close matches
		ArrayList<String> productStrings = new ArrayList<>();
		// First, sort the arraylist by length of the elements
		for (int i = 0; i < productTempStrings.size(); ++i) {
			String productString = productTempStrings.get(i);
			int newSize = productStrings.size();
			int index = 0;
			innerloop: for (int j = 0; j < newSize; ++j) {
				if (productString.length() > productStrings.get(j).length()) {
					index = j;
					break innerloop;
				}
				++index;
			}
			productStrings.add(index, productString);
		}

		if (debugL <= 2) {
			System.out.println("Product Strings = " + productStrings);
		}

		// ...and, then, go through and eliminate strings that match closely (>= 96)

		ArrayList<String> finalProductStrings = new ArrayList<>();
		outerloop: for (int i = 0; i < productStrings.size(); ++i) {
			String productString = productStrings.get(i);
			int newSize = finalProductStrings.size();
			for (int j = 0; j < newSize; ++j) {
				// if a product string is there in the array that already matches another that
				// has been chosen, then ignore it
				if (FuzzySearch.weightedRatio(productString, productStrings.get(j)) > 95) {
					continue outerloop;
				}
			}
			finalProductStrings.add(productString);
		}

		if (debugL <= 2) {
			System.out.println("Final Product Strings = " + finalProductStrings);
		}

		// create the final search string by concatenating the strings from the array
		// productStrings
		StringBuffer finalSearchString = new StringBuffer();
		for (String string : finalProductStrings) {
			finalSearchString.append(string).append(" ");
		}

		if (debugL <= 7) {
			System.out.println("Final search string = " + finalSearchString.toString());
		}

		List<ExtractedResult> productMatches = FuzzySearch.extractTop(finalSearchString.toString(), products, 10, 70);
		if (debugL <= 7) {
			System.out.println(productMatches);
		}

		/*
		 * ArrayList<ExtractedResult> productMatches = new ArrayList<>(3); for
		 * (OCRResult ocrResult : ocrResults) { for (String sentence :
		 * ocrResult.sentences) { String[] lines =
		 * sentence.lines().toArray(String[]::new); for (String line : lines) {
		 * List<ExtractedResult> prodMatches =
		 * this.findProductIndexUsingFuzzyWuzzy(line); for (ExtractedResult result :
		 * prodMatches) { if (!productSubStrings.contains(line)) {
		 * productSubStrings.add(line); } int size = productMatches.size(); if (size <
		 * 3) { productMatches.add(result); } else { if (result.getScore() >
		 * productMatches.get(0).getScore()) { productMatches.add(0, result);
		 * productMatches.remove(3); } else { if (result.getScore() >
		 * productMatches.get(1).getScore()) { productMatches.add(1, result);
		 * productMatches.remove(3); } else { if (result.getScore() >
		 * productMatches.get(2).getScore()) { productMatches.add(2, result);
		 * productMatches.remove(3); } } } } } } } System.out.println(productMatches); }
		 */

		int finalPrice = 0;
		String finalProduct = null;
		boolean found = false;

		// find which product and price combination is valid
		outerloop: for (ExtractedResult result : productMatches) {
			int productIndex = result.getIndex();
			for (IndexAndPixelHeight priceIndex : priceIndices) {
				if (priceIndex.index == productIndex) {
					finalPrice = prices.get(priceIndex.index);
					finalProduct = originalProductStrings.get(productIndex);
					found = true;
					break outerloop;
				}
			}
		}

		if (debugL <= 8) {
			System.out.println("Product : " + finalProduct);
			System.out.println("Price : Rs. " + finalPrice);
			if (monthFound && yearFound) {
				System.out.println("Month of Manufacture : " + monthAndYear);
			}
		}

		if (found && monthFound && yearFound) {
			// output.append("true");
			product.productOK = ProductDescription.ALL_OK;
		} else {
			// output.append("false");
			product.productOK = ProductDescription.ERROR_ONLY_ONE_THING_NOT_FOUND_OR_SOME_OTHER_ISSUE;
		}

		product.productName = finalProduct;
		product.setMonth(bestMonthMatch.getValue().string2);
		product.setYear(year);
		product.price.add(Double.valueOf(finalPrice * 1.0));

		return product;

		/*
		 * output.append(separator).append(finalProduct).append(separator).append(
		 * finalPrice).append(separator) .append(monthAndYear); return
		 * output.toString();
		 */

	}

	public static ProductDescription doValidation(ArrayList<OCRResult> ocrResults, int debugL) {
		return pkInstance.checkValidity(ocrResults, debugL);
	}

	public static ProductDescription validate(ArrayList<OCRStringWrapper> ocrResults, int debugL) {

		ProductDescription product = new ProductDescription();
		String rejectionReason = "";

		// removed, since this check is done at the beginning in
		// ReadLabels.processFileMultiThreadedFast()
		// checkIfProductIsGiven();

		if (CheckProductProperties.productIsGiven) {
			if (CheckProductProperties.givenProductPrice == 0) {
				return ProductDescription.ERROR_NO_PRODUCT_SPECIFIED;
			}
		}

		ArrayList<Rectangle> monthYearBoundingBoxes = null;
		ArrayList<Rectangle> priceBoundingBoxes = null;
		ArrayList<Rectangle> productBoundingBoxes = null;

		boolean monthFound = false;
		boolean yearFound = false;
		StringBuffer monthAndYear = new StringBuffer();

		// ***************************************
		// find the best match for month
		// ***************************************
		ArrayList<Entry<Integer, StringAndPixelHeight>> monthMatches = new ArrayList<>();

		for (OCRStringWrapper wrapper : ocrResults) {
			for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
				String[] lines = sentence.split("\r\n|\r|\n");
				for (String line : lines) {
					Entry<Integer, StringAndPixelHeight> month = processForMonth(line, wrapper.getLikelyPixelHeight(),
							debugL);
					if (month != null) {
						monthMatches.add(month);
						if (wrapper.getBoundingBoxes() != null) {
							// monthYearBoundingBoxes = wrapper.getBoundingBoxes();
							month.getValue().boundingBoxes = wrapper.getBoundingBoxes();
						}
					}
				}
			}
		}

		Entry<Integer, StringAndPixelHeight> bestMonthMatch = null;

		for (Entry<Integer, StringAndPixelHeight> aMonth : monthMatches) {
			if (bestMonthMatch == null) {
				bestMonthMatch = aMonth;
				monthYearBoundingBoxes = aMonth.getValue().boundingBoxes;
				continue;
			}
			if (aMonth.getKey() > bestMonthMatch.getKey()) {
				bestMonthMatch = aMonth;
				monthYearBoundingBoxes = aMonth.getValue().boundingBoxes;
			} else {
				if (aMonth.getKey() == bestMonthMatch.getKey()) {
					int index1 = aMonth.getValue().originalString.toUpperCase().indexOf(aMonth.getValue().string2);
					Pattern yearPattern = Pattern.compile("\\d{4}");
					Matcher matcher1 = yearPattern.matcher(aMonth.getValue().originalString);
					Matcher matcher2 = yearPattern.matcher(bestMonthMatch.getValue().originalString);
					/*
					 * int index2 = bestMonthMatch.getValue().originalString.toUpperCase()
					 * .indexOf(bestMonthMatch.getValue().string2);
					 */
					if ((index1 != -1) && (matcher1.find()) && (!matcher2.find())) {
						bestMonthMatch = aMonth;
						monthYearBoundingBoxes = aMonth.getValue().boundingBoxes;
					}
				}
			}
		}

		if (bestMonthMatch != null) {
			monthAndYear.append(bestMonthMatch.getValue().string2).append(" ");
			monthFound = bestMonthMatch.getValue().monthFound;
			product.setMonth(bestMonthMatch.getValue().string2);
		}

		if (debugL <= 3) {
			if (bestMonthMatch != null) {
				System.out.println("1. In ProductPriceData.validate(). Month = " + monthAndYear + "; PixelHeight = "
						+ bestMonthMatch.getValue().pixelHeight2 + "; Rectangles = " + monthYearBoundingBoxes);
			}
		}

		// **************************************************
		// finished finding the best match for month
		// **************************************************

		// **************************************************
		// find the best match for year
		// **************************************************

		String year = null;
		ArrayList<Rectangle> yearBoundingBoxes = null;
		outerloop: for (OCRStringWrapper wrapper : ocrResults) {
			for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
				String[] lines = sentence.split("\r\n|\r|\n");
				HashMap<String, Integer> yearStrings = new HashMap<>();
				for (String line : lines) {
					if (yearStrings.containsKey(line)) {
						int count = yearStrings.get(line);
						yearStrings.put(line, ++count);
					} else {
						yearStrings.put(line, 1);
					}
				}
				List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(
						yearStrings.entrySet());
				Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
					@Override
					public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
						return (o2.getValue()).compareTo(o1.getValue());
					}
				});
				for (Map.Entry<String, Integer> line : list) {
					year = processForYear(line.getKey(), debugL);
					if (year != null) {
						monthAndYear.append(year);
						yearFound = true;
						yearBoundingBoxes = wrapper.getBoundingBoxes();
						break outerloop;
					}
				}
			}
		}

		// **************************************************
		// finished finding the best match for year
		// **************************************************

		// **************************************************
		// find all the matches for price
		// **************************************************

		ArrayList<IndexAndPixelHeight> priceIndices = new ArrayList<>();
		ArrayList<String> priceLines = new ArrayList<>();
		boolean priceHasADot = false;
		for (OCRStringWrapper wrapper : ocrResults) {
			int pixelHeight = wrapper.getLikelyPixelHeight();
			if (debugL <= 2) {
				System.out.println("In ProductPriceData.validate(): The bounding boxes for " + wrapper.getOcrString()
						+ " are : " + wrapper.getBoundingBoxes());
			}
			for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
				String[] lines = sentence.split("\r\n|\r|\n");
				for (String line : lines) {
					int priceIndicesLength = priceIndices.size();
					priceHasADot = priceHasADot || doesPriceHaveADot(line);
					processForPrice(line, pixelHeight, wrapper.getBoundingBoxes(), priceIndices, debugL);
					if (priceIndices.size() > priceIndicesLength) {
						priceLines.add(line);
					}
				}
			}
		}
		if (debugL <= 2) {
			System.out.println("In ProductPriceData.validate(): 1. The priceIndices are :");
			for (IndexAndPixelHeight anEntry : priceIndices) {
				System.out.println("	" + anEntry);
			}
			System.out.println();
			System.out.println("In ProductPriceData.validate(): 1. The priceLines are "
					+ java.util.Arrays.deepToString(priceLines.toArray()));
		}

		// count the price strings and get the count for each shortlisted price string
		HashMap<String, Integer> priceStringCount = new HashMap<>();
		if (priceLines.size() > 0) {
			// for (String[] priceLineArray : priceLines) {
			// for (String aPriceLine : priceLineArray) {
			for (String aPriceLine : priceLines) {
				Set<String> keys = priceStringCount.keySet();
				if (keys.contains(aPriceLine)) {
					int count = priceStringCount.get(aPriceLine);
					priceStringCount.put(aPriceLine, ++count);
					// priceHasADot = priceHasADot || doesPriceHaveADot(aPriceLine);
				} else {
					priceStringCount.put(aPriceLine, 1);
					// priceHasADot = priceHasADot || doesPriceHaveADot(aPriceLine);
				}
			}
			// }
		}

		if (debugL <= 2) {
			System.out.println("In ProductPriceData.validate(): 1. priceStringCount = " + priceStringCount);
		}

		// clean up the priceStringCount HashMap if productIsGiven, as price is already
		// known
		// This routine can't be done is product is not known, as price is as yet
		// undetermined
		// If a line has the price and has Rs (or, R*), then keep it and delete others
		// if there's none with Rs and the price, then keep all

		if (CheckProductProperties.productIsGiven) {
			boolean priceAndRsFound = false;
			String priceString = CheckProductProperties.givenProductPrice + "";
			Set<String> priceKeys = priceStringCount.keySet();
			for (String aKey : priceKeys) {
				String newKey = rationalisePriceSearchString(aKey);
				Matcher reMatcher = null;
				synchronized (lock) {
					reMatcher = rsWildCardPattern.matcher(newKey);
				}
				if ((newKey.indexOf(priceString) != -1) && reMatcher.find()) {
					priceAndRsFound = true;
					break;
				}
			}
			if (debugL <= 2) {
				System.out.println("In ProductPriceData.validate(): 1. priceAndRsFound = " + priceAndRsFound);
			}
			if (debugL <= 2) {
				System.out.println("In ProductPriceData.validate(): old proceStringCount = " + priceStringCount);
			}

			if (priceAndRsFound) {
				Object[] keys = priceStringCount.keySet().toArray();
				for (int i = keys.length - 1; i >= 0; --i) {
					String aKey = (String) keys[i];
					String newKey = rationalisePriceSearchString(aKey);
					Matcher reMatcher = null;
					synchronized (lock) {
						reMatcher = rsWildCardPattern.matcher(newKey);
					}
					if (debugL <= 2) {
						System.out.println("In ProductPriceData.validate(): newKey = " + newKey + " for key = " + aKey);
					}
					if (!((newKey.indexOf(priceString) != -1) && (reMatcher.find()))) {
						priceStringCount.remove(aKey);
					}
				}
			}
			if (debugL <= 2) {
				System.out.println("In ProductPriceData.validate(): new proceStringCount = " + priceStringCount);
			}
			// Also, if priceAndRsFound, clean up the priceIndices as well
			if (debugL <= 2) {
				System.out.println("In ProductPriceData.validate(): Old priceIndices are :");
				for (IndexAndPixelHeight anEntry : priceIndices) {
					System.out.println("	" + anEntry);
				}
			}
			if (priceAndRsFound) {
				int length = priceIndices.size();
				for (int i = length - 1; i >= 0; --i) {
					IndexAndPixelHeight entry = priceIndices.get(i);
					String originalString = entry.originalString;
					String newString = rationalisePriceSearchString(originalString);
					Matcher reMatcher = null;
					synchronized (lock) {
						reMatcher = rsWildCardPattern.matcher(newString);
					}
					if (debugL <= 2) {
						System.out.println("In ProductPriceData.validate(): newString = " + newString
								+ " for originalString = " + originalString);
					}
					if (!((newString.indexOf(priceString) != -1) && (reMatcher.find()))) {
						priceIndices.remove(i);
					}
				}
			}
			if (debugL <= 2) {
				System.out.println("In ProductPriceData.validate(): New priceIndices are :");
				for (IndexAndPixelHeight anEntry : priceIndices) {
					System.out.println("	" + anEntry);
				}
			}
		}

		// iterate through the price strings in priceIndices and check for a dot
		// **** This section of code may not be needed, as the check for priceHasADot is
		// done a few lines above
		// But, it has been kept as a backup
		for (IndexAndPixelHeight priceIndex : priceIndices) {
			String origString = priceIndex.originalString;
			priceHasADot = priceHasADot || doesPriceHaveADot(origString);
		}
		// ****

		if (debugL <= 2) {
			System.out.println("In ProductPriceData.validate(): priceHasADot - " + priceHasADot);
		}

		StringBuffer pChoices = new StringBuffer();
		for (IndexAndPixelHeight entry : priceIndices) {
			pChoices.append("Rs ").append(prices.get(entry.index)).append(" ; ");
		}
		if (debugL <= 2) {
			System.out.println("In ProductPriceData.validate(): Price choices are - " + pChoices.toString());
		}

		priceHasADot = priceHasADot && ("".equals(pChoices.toString()) ? false : true);
		product.setPriceHasADot(priceHasADot);

		if (debugL <= 2) {
			System.out.println("In ProductPriceData.validate(): priceStringCount HashMap is - " + priceStringCount);
		}

		if (debugL <= 2) {
			System.out.println("2. In ProductPriceData.validate(): each priceIndex is ");
			for (IndexAndPixelHeight entry : priceIndices) {
				System.out.println(entry);
			}
		}

		// **************************************************
		// finished finding all the matches for price; priceStringCount HashMap
		// initialised
		// **************************************************

		if (CheckProductProperties.productIsGiven) {
			// **************************************************
			// processing for the case when product is given
			// **************************************************
			product.setProductCharacterHeightActual(0);
			product.setProductCharacterHeightPixels(1);
			product.setMonth(bestMonthMatch == null ? ""
					: (bestMonthMatch.getValue() == null ? "" : bestMonthMatch.getValue().string2));
			if (debugL <= 2) {
				System.out.println(
						"In ProductPriceData.validate() - product is given : " + " set month to " + product.getMonth());
			}

			product.setYear(year == null ? "" : year);
			String likelyOriginalPriceString = null;
			int priceHeight = 1;
			int finalPrice = 0;
			ArrayList<Rectangle> boundingBoxes = null;
			boolean priceFound = false;
			if (debugL <= 2) {
				System.out.println("In ProductPriceData.validate() - priceIndices are : ");
				for (IndexAndPixelHeight entry : priceIndices) {
					System.out.println("	" + entry);
				}
			}
			for (IndexAndPixelHeight entry : priceIndices) {
				Integer currPrice = prices.get(entry.index);
				if (CheckProductProperties.givenProductPrice == currPrice.intValue()) {
					priceHeight = entry.pixelHeight1;
					boundingBoxes = entry.boundingBoxes;
					likelyOriginalPriceString = entry.originalString;
					finalPrice = currPrice.intValue();
					priceFound = true;
					product.foundPrice = true;
					break;
				}
			}

			// if price is not found, then we have to report what incorrect price we have
			// found.
			// to do that, count how many of the price shortlists are there in the
			// priceStringCount HashMap, and pick the topmost one as the price to be
			// reported

			HashMap<String, Integer> priceCounter = new HashMap<>();
			for (IndexAndPixelHeight entry : priceIndices) {
				priceCounter.put("" + prices.get(entry.index), 0);
			}

			if (debugL <= 2) {
				System.out.println(
						"In ProductPriceData.validate(): priceCounter HashMap is initialised to - " + priceCounter);
			}

			if (debugL <= 2) {
				System.out.println("In ProductPriceData.validate(): priceStringCount HashMap is initialised to - "
						+ priceStringCount);
			}

			Set<Map.Entry<String, Integer>> priceStringEntries = priceStringCount.entrySet();
			String numbers = "0123456789";
			for (Map.Entry<String, Integer> entry : priceStringEntries) {
				String key = entry.getKey();
				String cleanedKey = key.toUpperCase().replace(" ", "").replace("_", "").replace("-", "")
						.replace("O", "0").replace(",", "").replace("(", " ").replace(")", " ").replace("[", " ")
						.replace("]", " ").replace("'", " ").replace("\"", " ").replace("-", " ").replace("+", "")
						.replace(":", "").replace("/", "").replace("=", "").replace("|", "1").replace("\\", "")
						.replace("Z", "2").replace("D", "0").replace("{", " ").replace("}", " ").replace("I", "1")
						.replace("T", "7").replace("1U", "").replace("2U", "").replace("3U", "").replace("4U", "")
						.replace("5U", "").replace("6U", "").replace("7U", "").replace("8U", "").trim();
				if (CheckProductProperties.productIsGiven) {
					if (CheckProductProperties.givenProductPrice != 0) {
						String productPrice = "" + CheckProductProperties.givenProductPrice;
						if (productPrice.indexOf("2") != -1) {
							cleanedKey = cleanedKey.replace("Z", "2");
						}
						if ((productPrice.indexOf("7") != -1) && (productPrice.indexOf("1") != -1)) {
						} else {
							if (productPrice.indexOf("7") == -1) {
								cleanedKey = cleanedKey.replace("7", "1");
							}
							if (productPrice.indexOf("1") == -1) {
								cleanedKey = cleanedKey.replace("1", "7");
							}
						}
						if ((productPrice.indexOf("5") != -1) && (productPrice.indexOf("6") == -1)) {
							cleanedKey = cleanedKey.replace("6", "5");
						}
						if ((productPrice.indexOf("9") != -1) && (productPrice.indexOf("8") == -1)) {
							cleanedKey = cleanedKey.replace("8", "9");
						}
					}
				}
				Matcher priceMatcher = null;
				synchronized (lock) {
					priceMatcher = pricePattern.matcher(cleanedKey);
				}
				if (priceMatcher.find()) {
					int start = priceMatcher.start();
					int end = priceMatcher.end();
					String tobeReplaced = cleanedKey.substring(start, end).trim();
					cleanedKey = cleanedKey.replace(tobeReplaced, "");
				}
				Matcher retailMatcher = null;
				synchronized (lock) {
					retailMatcher = retailPattern.matcher(cleanedKey);
				}
				if (retailMatcher.find()) {
					int start = retailMatcher.start();
					int end = retailMatcher.end();
					String tobeReplaced = cleanedKey.substring(start, end).trim();
					cleanedKey = cleanedKey.replace(tobeReplaced, "");
				}
				Matcher taxesMatcher = null;
				synchronized (lock) {
					taxesMatcher = taxesPattern.matcher(cleanedKey);
				}
				if (taxesMatcher.find()) {
					int start = taxesMatcher.start();
					int end = taxesMatcher.end();
					String tobeReplaced = cleanedKey.substring(start, end).trim();
					cleanedKey = cleanedKey.replace(tobeReplaced, "");
				}
				Matcher ofAllMatcher = null;
				synchronized (lock) {
					ofAllMatcher = ofAllPattern.matcher(cleanedKey);
				}
				if (ofAllMatcher.find()) {
					int start = ofAllMatcher.start();
					int end = ofAllMatcher.end();
					String tobeReplaced = cleanedKey.substring(start, end).trim();
					cleanedKey = cleanedKey.replace(tobeReplaced, "");
				}
				Matcher rsMatcher = null;
				synchronized (lock) {
					rsMatcher = rsPattern.matcher(cleanedKey);
				}
				if (rsMatcher.find()) {
					int start = rsMatcher.start();
					int end = rsMatcher.end();
					String tobeReplaced = cleanedKey.substring(start, end).trim();
					// remove Rs, and change "S" to "5" in the string
					cleanedKey = cleanedKey.replace(tobeReplaced, "").replace("S", "5");
					// Add back RS, by first checking for a 4-digit sequence
					Pattern sequence = Pattern.compile("\\d{4,5}");
					Matcher aMatcher = sequence.matcher(cleanedKey);
					if (aMatcher.find()) {
						if (debugL <= 2) {
							System.out.println("Adding back RS in " + cleanedKey);
						}
						start = aMatcher.start();
						cleanedKey = cleanedKey.substring(0, start) + "RS" + cleanedKey.substring(start);
					}
				}

				for (Pattern aPattern : ttkPrestigeLtd) {
					Matcher aMatcher = null;
					synchronized (lock) {
						aMatcher = aPattern.matcher(cleanedKey);
					}
					if (aMatcher.find()) {
						int start = aMatcher.start();
						int end = aMatcher.end();
						String tobeReplaced = cleanedKey.substring(start, end).trim();
						cleanedKey = cleanedKey.replace(tobeReplaced, "");
					}
				}

				// now find the index of the first numeric digit in the cleanedKey
				int intIndex = -1;

				for (int indexChar = 0; indexChar < cleanedKey.length(); ++indexChar) {
					String charAtIndex = cleanedKey.substring(indexChar, indexChar + 1);
					if (numbers.indexOf(charAtIndex) != -1) {
						intIndex = indexChar;
						break;
					}
					/*
					 * // For some unknown reason, this piece of code is not working in the
					 * obfuscated jar from ProGuard // Hence, replaced with the current code try {
					 * if (debugL <= 2) { System.out.println("Checking if " + charAtIndex +
					 * " is a number"); } int isItAnInteger = Integer.parseInt(charAtIndex);
					 * intIndex = indexChar; break; } catch (Exception e) { // do nothing //
					 * continue to the next character and check if it is a number }
					 */
				}
				if (debugL <= 2) {
					System.out.println("In ProductPriceData.validate(): Cleaned priceString " + key + " to "
							+ cleanedKey + ", for which first integer is at index " + intIndex);
				}
				// if no integer found, continue to the next entry
				if (intIndex == -1) {
					continue;
				}
				// extract the rest of the substring starting from intIndex and check if it
				// starts with any of the keys in priceCounter
				// if it does, increment the priceCounter of that price by 1
				String priceValue = cleanedKey.substring(intIndex);
				Set<String> shortlistedPrices = priceCounter.keySet();
				for (String aPrice : shortlistedPrices) {
					if (priceValue.startsWith(aPrice)) {
						int currCount = priceCounter.get(aPrice);
						priceCounter.put(aPrice, currCount + entry.getValue());
					}
				}
			}

			if (debugL <= 2) {
				System.out.println("In ProductPriceData.validate(): priceCounter HashMap is - " + priceCounter);
			}

			// now that we have the clean counts, iterate through the HashMap and get the
			// price string with maximum occurrence

			Set<String> shortlistedPrices = priceCounter.keySet();
			int maxCounter = 0;
			String alternatePriceString = "";
			for (String aPrice : shortlistedPrices) {
				if (priceCounter.get(aPrice) > maxCounter) {
					maxCounter = priceCounter.get(aPrice);
					alternatePriceString = aPrice;
				}
			}

			boolean distanceIsOK = false;
			String rejReason = "";
			if (priceFound) {
				OCRPriceDimensionsWrapper priceDimensionsWrapper = new OCRPriceDimensionsWrapper(priceHeight,
						likelyOriginalPriceString == null ? new StringBuffer().append(finalPrice).toString()
								: likelyOriginalPriceString,
						boundingBoxes, "" + CheckProductProperties.givenProductPrice);
				priceDimensionsWrapper.setDebugLevel(debugL);
				priceDimensionsWrapper.process(0, 0);
				product.setPriceGapBetween1And2(priceDimensionsWrapper.gapBetween1And2);
				distanceIsOK = priceDimensionsWrapper.distanceOK;
				rejReason = priceDimensionsWrapper.reasonForDistanceRejection;
			} else {
				// for (IndexAndPixelHeight entry : priceIndices) {
				// Integer currPrice = prices.get(entry.index);
				// product.price.add(Double.valueOf(currPrice.intValue()));
				// }
				if (debugL <= 2) {
					System.out.println(
							"In ProductPriceData.validate(): alternatePriceString is = " + alternatePriceString);
				}

				try {
					product.price.add(Double.parseDouble(alternatePriceString));
				} catch (NumberFormatException nfe) {
					product.price.add(0.0);
				}
			}

			product.setFinalPrice("" + (priceFound ? finalPrice : alternatePriceString));
			product.setPriceCharacterHeightActual(0);
			product.setPriceCharacterHeightPixels(0);
			product.setDateCharacterHeightActual(0);
			product.setDateCharacterHeightPixels(0);
			product.setPriceHasADot(priceHasADot);

			if (debugL <= 2) {
				System.out.println("In ProductPriceData.validate(): monthFound = " + monthFound + "; yearFound = "
						+ yearFound + "; priceDistanceOK = " + (priceFound ? distanceIsOK : ""));
			}

			if (priceFound && monthFound && yearFound && priceHasADot) {
				// output.append("true");
				product.productOK = ProductDescription.ALL_OK;
				product.setProductName(System.getProperty(CheckProductProperties.productNameKey));
			} else {
				// output.append("false");
				if (priceFound) {
					product.setProductName(System.getProperty(CheckProductProperties.productNameKey));
				}
				product.productOK = ProductDescription.ERROR_ONLY_ONE_THING_NOT_FOUND_OR_SOME_OTHER_ISSUE;
				if (!priceFound) {
					rejectionReason += System.getProperty(priceErrorKey);
				}
				if (!monthFound) {
					rejectionReason += System.getProperty(monthErrorKey);
				}
				if (!yearFound) {
					rejectionReason += System.getProperty(yearErrorKey);
				}
				// if (priceFound && !priceHasADot) {
				if (!priceHasADot) {
					rejectionReason += System.getProperty(priceDotErrorKey);
				}
			}

			if (!distanceIsOK) {
				if (product.productOK == ProductDescription.ALL_OK) {
					product.productOK = ProductDescription.ERROR_DIMENSION_PROBLEM;
				}
				rejectionReason += rejReason;
			}

			product.setRejectionReason(rejectionReason);

			// **************************************************
			// finished processing for the case when product is given
			// **************************************************

		} else {

			// *********************************************************
			// starting processing for the case when product is NOT given
			// **********************************************************

			// find the 3 best matches for product
			// first, find all the relevant strings from the OCR results that matter

			// first get all the relevant product strings from the wrapper strings
			// The algo is :
			// 1. First get all the relevant product strings
			// 2. Then, find matching products
			// 3. Then, match the products with the shortlisted prices
			ArrayList<String> productSubStrings = new ArrayList<>();
			for (OCRStringWrapper wrapper : ocrResults) {
				if (debugL <= 1) {
					System.out.println("In ProductPriceData.validate(): String in wrapper is : ");
					System.out.println("----------------------");
					System.out.println(wrapper.ocrString);
				}
				String tempString = null;
				for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
					if (debugL <= 1) {
						System.out.println("In ProductPriceData.validate(): String in sentence is : ");
						System.out.println("------------------------");
						System.out.println(sentence);
					}
					String[] lines = sentence.split("\r\n|\r|\n");
					innerloop: for (String line : lines) {
						if (debugL <= 1) {
							System.out.println("In ProductPriceData.validate(): String in line is : ");
							System.out.println(line);
						}

						if (debugL <= 1) {
							System.out.print("In ProductPriceData.validate(): Rectifying string : " + line);
						}
						String lineUpper = rectifyString(line);
						if (debugL <= 7) {
							System.out.print(". Cleaning product line : " + lineUpper);
						}
						lineUpper = getCleanProductString(lineUpper, debugL);
						if (debugL <= 7) {
							System.out.println(" to get cleaned product line as : " + lineUpper);
						}
						boolean couldBeProductLine = lineUpper.length() > 0;
						if (couldBeProductLine) {
							// check if a product substring exists with same content
							// if no, then add this line; else, even if its exists, replace with this line
							// if this line is of longer length
							if (tempString == null) {
								tempString = lineUpper;
								continue innerloop;
							} else {
								int newScore = FuzzySearch.weightedRatio(tempString, lineUpper);
								if ((newScore > 89) && (lineUpper.length() > tempString.length())) {
									tempString = lineUpper;
									continue innerloop;
								}
							}
						}
					}
				}
				if (tempString != null) {
					productSubStrings.add(tempString);
				}
			}

			int size = productSubStrings.size();

			if (size <= 2) {
				// if no of strings found is less than 3, repeat the above sequence with a
				// lesser cut-off
				for (OCRStringWrapper wrapper : ocrResults) {
					if (debugL <= 1) {
						System.out.println("In ProductPriceData.validate(): String in wrapper is : ");
						System.out.println("----------------------");
						System.out.println(wrapper.ocrString);
					}
					String tempString = null;
					for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
						if (debugL <= 1) {
							System.out.println("In ProductPriceData.validate(): String in sentence is : ");
							System.out.println("------------------------");
							System.out.println(sentence);
						}
						String[] lines = sentence.split("\r\n|\r|\n");
						innerloop: for (String line : lines) {
							if (debugL <= 1) {
								System.out.println("In ProductPriceData.validate(): String in line is : ");
								System.out.println(line);
							}

							if (debugL <= 1) {
								System.out.print("Rectifying string : " + line);
							}
							String lineUpper = rectifyString(line);
							if (debugL <= 7) {
								System.out.print(". Cleaning product line : " + lineUpper);
							}
							lineUpper = getCleanProductString(lineUpper, debugL);
							if (debugL <= 7) {
								System.out.println(" to get cleaned product line as : " + lineUpper);
							}
							boolean couldBeProductLine = lineUpper.length() > 0;
							if (couldBeProductLine) {
								// check if a product substring exists with same content
								// if no, then add this line; else, even if its exists, replace with this line
								// if this line is of longer length
								if (tempString == null) {
									tempString = lineUpper;
									continue innerloop;
								} else {
									int newScore = FuzzySearch.weightedRatio(tempString, lineUpper);
									if ((newScore > 85) && (lineUpper.length() > tempString.length())) {
										tempString = lineUpper;
										continue innerloop;
									}
								}
							}
						}
					}
					if (tempString != null) {
						productSubStrings.add(tempString);
					}
				}
			}

			size = productSubStrings.size();

			if (size <= 2) {
				// if no of strings found is less than 3, repeat the above sequence with a
				// lesser cut-off
				for (OCRStringWrapper wrapper : ocrResults) {
					if (debugL <= 1) {
						System.out.println("In ProductPriceData.validate(): String in wrapper is : ");
						System.out.println("----------------------");
						System.out.println(wrapper.ocrString);
					}
					String tempString = null;
					for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
						if (debugL <= 1) {
							System.out.println("In ProductPriceData.validate(): String in sentence is : ");
							System.out.println("------------------------");
							System.out.println(sentence);
						}
						String[] lines = sentence.split("\r\n|\r|\n");
						innerloop: for (String line : lines) {
							if (debugL <= 1) {
								System.out.println("In ProductPriceData.validate(): String in line is : ");
								System.out.println(line);
							}

							if (debugL <= 1) {
								System.out.print("Rectifying string : " + line);
							}
							String lineUpper = rectifyString(line);
							if (debugL <= 7) {
								System.out.print(". Cleaning product line : " + lineUpper);
							}
							lineUpper = getCleanProductString(lineUpper, debugL);
							if (debugL <= 7) {
								System.out.println(" to get cleaned product line as : " + lineUpper);
							}
							boolean couldBeProductLine = lineUpper.length() > 0;
							if (couldBeProductLine) {
								// check if a product substring exists with same content
								// if no, then add this line; else, even if its exists, replace with this line
								// if this line is of longer length
								if (tempString == null) {
									tempString = lineUpper;
									continue innerloop;
								} else {
									int newScore = FuzzySearch.weightedRatio(tempString, lineUpper);
									if ((newScore > 80) && (lineUpper.length() > tempString.length())) {
										tempString = lineUpper;
										continue innerloop;
									}
								}
							}
						}
					}
					if (tempString != null) {
						productSubStrings.add(tempString);
					}
				}
			}

			ArrayList<String> finalProductStrings = new ArrayList<>();
			outerloop: for (int i = 0; i < productSubStrings.size(); ++i) {
				String productString = productSubStrings.get(i);
				int newSize = finalProductStrings.size();
				for (int j = 0; j < newSize; ++j) {
					// if a product string is there in the array that already matches another that
					// has been chosen, then ignore it
					if (FuzzySearch.weightedRatio(productString, finalProductStrings.get(j)) > 95) {
						continue outerloop;
					}
				}
				finalProductStrings.add(productString);
			}

			Set<String> uniqueProductSearchWords = new HashSet<>();
			for (String productWord : finalProductStrings) {
				uniqueProductSearchWords.add(productWord.trim());
			}

			StringBuffer allStrings = new StringBuffer(0);
			// Set<String> capacityVariantKeys = capacityVariants.keySet();
			for (String subString : uniqueProductSearchWords) {
				String[] subStringTokens = subString.split(" ");
				for (String token : subStringTokens) {
					Set<String> variants = capacityVariants.get(token);
					if (variants == null) {
						allStrings.append(token).append(" ");
					} else {
						for (String variant : variants) {
							allStrings.append(variant).append(" ");
						}
					}
				}
			}
			String finalSearchString = allStrings.toString();
			if (debugL <= 7) {
				System.out.println("****************************");
				System.out.println(
						"In ProductPriceData.validate(): Final search string before adjusting for removal of PRESTIGE and COM ="
								+ finalSearchString);
				System.out.println("****************************");
			}

			int indexOfPrestige = finalSearchString.indexOf("PRESTIGE");
			if (indexOfPrestige != -1) {
				double ratio = ("PRESTIGE".length() * 1.0) / finalSearchString.length();
				if (ratio > 0.45) {
					finalSearchString = finalSearchString.substring(0, indexOfPrestige) + finalSearchString
							.substring(indexOfPrestige + "PRESTIGE".length(), finalSearchString.length());
				}
			}

			// need to check if COM has to be removed as well - At the moment, I don't think
			// it needs to be removed

			// if 2 words or less have been shortlisted, then product is not found

			if (debugL <= 7) {
				System.out.println(
						"In ProductPriceData.validate(): Final search string after adjusting for removal of PRESTIGE = "
								+ finalSearchString);
			}

			String[] productWords = finalSearchString.split("\\s+");
			size = productWords.length;

			int finalPrice = 0;
			int pricePixelHeight = 0;
			String finalProduct = "";
			int actualProductHeight = 0;
			boolean productFound = false;
			int productPixelHeight = 0;
			boolean priceShortlistFound = true;

			if (size <= 2) {
				// if no of strings found is less than 3, then no product can be identified
				productFound = false;
				if (debugL <= 8) {
					System.out.println("In ProductPriceData.validate(): No match found for product");
					for (IndexAndPixelHeight price : priceIndices) {
						System.out.println("In ProductPriceData.validate(): Price : Rs. " + prices.get(price.index));
					}
					if (monthFound && yearFound) {
						System.out.println("In ProductPriceData.validate(): Month of Manufacture : " + monthAndYear);
					}
				}

				priceShortlistFound = false;
				// return product;

			}

			String likelyOriginalPriceString = null;
			ArrayList<Rectangle> likelyPriceBoundingBoxes = new ArrayList<>();

			if (priceShortlistFound) {
				// pick unique strings from productSubStrings, and populate them in a new array
				// called productStrings

				// ...and, then, loop through the final product strings array and include only
				// if the string doesn't closely match an existing string in the array (< 96)

				ArrayList<String> productShortlist = new ArrayList<>();
				ArrayList<Integer> shortlistedProductsIndex = new ArrayList<>();
				int productIndex = -1;

				for (IndexAndPixelHeight priceIndex : priceIndices) {
					if ((priceIndex.boundingBoxes != null) && (priceIndex.boundingBoxes.size() > 0)) {
						productShortlist.add(products.get(priceIndex.index));
						if (debugL <= 1) {
							System.out.println("In ProductPriceData.validate(): About to add " + priceIndex.index
									+ " to shortlistedProductsIndex");
						}
						shortlistedProductsIndex.add(priceIndex.index);
						if (debugL <= 1) {
							System.out.println(
									"In ProductPriceData.validate(): shortlistedProductsIndex after addition = "
											+ shortlistedProductsIndex);
						}
					}
				}

				if (debugL <= 2) {
					System.out.println("In ProductPriceData.validate(): productShortlist = " + productShortlist);
					System.out.println(
							"In ProductPriceData.validate(): shortlistedProductsIndex = " + shortlistedProductsIndex);
				}

				// List<ExtractedResult> productMatches =
				// FuzzySearch.extractTop(finalSearchString.toString(), products, 30,
				// 70);

				// List<ExtractedResult> productMatches =
				// FuzzySearch.extractTop(finalSearchString, products, 50, 70);
				List<ExtractedResult> productMatches = FuzzySearch.extractTop(finalSearchString, productShortlist, 20,
						70);

				if (debugL <= 7) {
					System.out.println("In ProductPriceData.validate(): productMatches = " + productMatches);
				}

				// in the productMatches list, first ensure that each product has the same
				// capacity variant as in the search string
				for (int k = productMatches.size() - 1; k >= 0; --k) {
					String productMatch = productMatches.get(k).getString();
					boolean capFound = false;
					Matcher matcher1 = null;
					Matcher matcher1A = null;
					synchronized (lock) {
						matcher1 = capacity1.matcher(productMatch);
						matcher1A = capacity1.matcher(finalSearchString);
					}
					/*
					 * if (matcher1.find()) { int start = matcher1.start(); int end =
					 * matcher1.end(); String capacity = productMatch.substring(start, end).trim();
					 * String newProductMatch = productMatch.replace(capacity, "").replace("  ",
					 * " "); productMatches.get(k).setString(newProductMatch); capFound = true; }
					 */
					Matcher matcher2 = null;
					Matcher matcher2A = null;
					synchronized (lock) {
						matcher2 = capacity2.matcher(productMatch);
						matcher2A = capacity2.matcher(finalSearchString);
					}
					/*
					 * if (matcher2.find()) { int start = matcher2.start(); int end =
					 * matcher2.end(); String capacity = productMatch.substring(start, end).trim();
					 * String newProductMatch = productMatch.replace(capacity, "").replace("  ",
					 * " "); productMatches.get(k).setString(newProductMatch); capFound = true; }
					 */
					capFound = (noOfWords(productMatch) <= 4) || (matcher1.find() && matcher1A.find())
							|| (matcher2.find() && matcher2A.find());
					if (!capFound) {
						productMatches.remove(k);
					}
				}

				// find which product matches the finalSearchString best

				String bestMatch = productMatches.size() == 1 ? productMatches.get(0).getString() : null;
				String searchStringsList = SBImageUtils.getWordsAsSortedSentence(finalSearchString);
				int nWordsSearchString = noOfWords(searchStringsList);
				if ((productMatches.size() > 1) && (finalSearchString.length() > 15)) {
					double matchingScore = 0;
					outerloop: for (ExtractedResult result : productMatches) {
						if ((finalProductStrings.size() - 1) > (noOfWordsInProduct.get(result.getIndex()))) {
							// the no of words shortlisted for product search must be <= number of words in
							// the product
							// The '-1' is to have a bit of wiggle room for error, though it creates a
							// problem in places where the difference between the choices is just 1 word
							continue;
						}
						String aProduct = SBImageUtils.getWordsAsSortedSentence(result.getString());
						int score = FuzzySearch.partialRatio(searchStringsList, aProduct);
						if (debugL <= 2) {
							System.out.println(
									"In ProductPriceData.validate(): Searching for best match inside loop with product = "
											+ aProduct + ": matching score =" + score);
						}

						double adjustedScore = (score * noOfWords(aProduct) * 1.0) / nWordsSearchString;
						if (adjustedScore >= matchingScore) {
							if (adjustedScore > matchingScore) {
								matchingScore = adjustedScore;
								bestMatch = aProduct;
								if (debugL <= 2) {
									System.out.println(
											"In ProductPriceData.validate(): Current resultIndex (in if condition) = "
													+ result.getIndex() + ": matching score =" + adjustedScore);
								}
								productIndex = shortlistedProductsIndex.get(result.getIndex());
							} else {
								if (aProduct.length() > bestMatch.length()) {
									bestMatch = aProduct;
									productIndex = shortlistedProductsIndex.get(result.getIndex());
									if (debugL <= 2) {
										System.out.println(
												"In ProductPriceData.validate(): Current resultIndex (in else condition) = "
														+ result.getIndex() + ": matching score =" + score);
									}
								}
							}
						}
					}
					if (debugL <= 2) {
						System.out.println("In ProductPriceData.validate(): searchStringsList = " + searchStringsList
								+ "; bestMatch = " + bestMatch);
					}

					/*
					 * for (int pIndex = 0; pIndex < products.size(); ++pIndex) { if
					 * (SBImageUtils.getWordsAsSortedSentence(products.get(pIndex)).equals(bestMatch
					 * )) { productIndex = pIndex; break; } }
					 */
				} else {
					if (bestMatch != null) {
						for (int pIndex = 0; pIndex < products.size(); ++pIndex) {
							if (products.get(pIndex).trim().equals(bestMatch.trim())) {
								productIndex = pIndex;
								break;
							}
						}
					}
				}

				if (productIndex != -1) {
					finalPrice = prices.get(productIndex);
					finalProduct = originalProductStrings.get(productIndex);
					actualProductHeight = originalProductHeights.get(productIndex);
				} else {
					finalPrice = 0;
					finalProduct = "";
					actualProductHeight = 0;
				}
				if (debugL <= 2) {
					System.out.println("In ProductPriceData.validate(): Product Index = " + productIndex);
					System.out.println("In ProductPriceData.validate(): priceIndices = " + priceIndices);
				}

				// productFound = !"".equals(finalProduct);
				aLoop: for (IndexAndPixelHeight priceIndex : priceIndices) {
					if (debugL <= 2) {
						System.out.println("In ProductPriceData.validate(): Price Index = " + priceIndex.index);
					}
					if (priceIndex.index == productIndex) {
						priceBoundingBoxes = priceIndex.boundingBoxes;
						pricePixelHeight = priceIndex.pixelHeight1;
						productFound = true;
						break aLoop;
					}
				}

				if (debugL <= 8) {
					System.out.println("In ProductPriceData.validate(): Product : " + finalProduct);
					System.out.println("In ProductPriceData.validate(): Price : Rs. " + finalPrice);
					if (monthFound && yearFound) {
						System.out.println("In ProductPriceData.validate(): Month of Manufacture : " + monthAndYear);
					}
				}

				if (productFound) {
					// determine now which OCRBufferedImageWrapper has a product name and get its
					// pixelHeight
					if (debugL <= 2) {
						System.out.println("1. In ProductPriceData.validate(). Comparing finalProduct = " + finalProduct
								+ " with the strings");
					}
					HashMap<OCRStringWrapper, Integer> bestProductFit = new HashMap<>();
					for (OCRStringWrapper wrapper : ocrResults) {
						for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
							String[] lines = sentence.split("\r\n|\r|\n");
							for (String line : lines) {
								if (line.length() > 15) {
									int weightedRatio = FuzzySearch.weightedRatio(finalProduct, line);
									if (debugL <= 2) {
										System.out.println("1. In ProductPriceData.validate(). Compared with line = "
												+ line + " to get FuzzySearch.weightedRatio() value of "
												+ weightedRatio);
									}
									if (weightedRatio >= 75) {
										if (bestProductFit.get(wrapper) == null) {
											bestProductFit.put(wrapper, weightedRatio);
										} else {
											int currentWeight = bestProductFit.get(wrapper);
											if (weightedRatio > currentWeight) {
												bestProductFit.put(wrapper, weightedRatio);
											}
										}
									}
								}
							}
						}
					}

					// Sort bestProductFit by weightedRatio

					List<Map.Entry<OCRStringWrapper, Integer>> productMatchList = new LinkedList<Map.Entry<OCRStringWrapper, Integer>>(
							bestProductFit.entrySet());
					// Sort the list
					Collections.sort(productMatchList, new Comparator<Map.Entry<OCRStringWrapper, Integer>>() {

						@Override
						public int compare(Map.Entry<OCRStringWrapper, Integer> o1,
								Map.Entry<OCRStringWrapper, Integer> o2) {
							return (o2.getValue()).compareTo(o1.getValue());
						}
					});

					OCRStringWrapper topProductFit = null;

					if (productMatchList.size() > 0) {
						topProductFit = productMatchList.get(0).getKey();
					}

					if (topProductFit != null) {
						productPixelHeight = topProductFit.likelyPixelHeight;
						productBoundingBoxes = topProductFit.getBoundingBoxes();
					} else {
						productPixelHeight = 0;
						productBoundingBoxes = new ArrayList<>();
					}

					// Finding the likelyOriginalPriceString if product is found
					// First, remove those entries which do not match the finalPrice
					if (debugL <= 2) {
						System.out.println(
								"In ProductPriceData.validate(): priceStringCount before removing irrelevant keys = "
										+ priceStringCount);
					}
					Set<String> keys = priceStringCount.keySet();
					List<String> keyList = keys.stream().collect(Collectors.toCollection(ArrayList::new));
					for (String key : keyList) {
						// String strippedDownKey = key.replace(" ", "").replace(".", "").replace("_",
						// "").replace("-", "")
						// .replace("O", "0").replace(",", "").replace(":", "").trim();
						String strippedDownKey = key.replace(" ", "").replace("_", ".").replace("-", ".")
								.replace("O", "0").replace(",", ".").replace("+", ".").replace(":", ".")
								.replace("/", "").replace("=", ".").replace("|", "").replace("\\", "").trim();

						int index = strippedDownKey.indexOf(new StringBuffer().append(finalPrice).toString());
						if (index == -1) {
							priceStringCount.remove(key);
						}
					}
					if (debugL <= 2) {
						System.out.println(
								"In ProductPriceData.validate(): priceStringCount after removing irrelevant keys = "
										+ priceStringCount);
					}
					// From the remaining strings in priceStringCount, sort the HashMap, pick the
					// one with most occurrences, clean it and assign its value to
					// likelyOriginalPriceString

					HashMap<String, Integer> priceStringCountCleaned = new HashMap<>();

					for (Map.Entry<String, Integer> entry : priceStringCount.entrySet()) {
						String originalString = entry.getKey();
						String newString = originalString.replace(" ", "").replace("_", ".").replace("-", ".")
								.replace("O", "0").replace(",", ".").replace("+", ".").replace(":", ".")
								.replace("/", "").replace("=", ".").replace("|", "").replace("\\", "").trim();
						if (priceStringCountCleaned.containsKey(newString)) {
							int currentCount = priceStringCountCleaned.get(newString);
							priceStringCountCleaned.put(newString, currentCount + 1);
						} else {
							priceStringCountCleaned.put(newString, entry.getValue());
						}
					}

					List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(
							priceStringCountCleaned.entrySet());
					// Sort the list
					Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
						@Override
						public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
							Pattern rupees = Pattern.compile("R.{0,1}");
							Matcher match1 = rupees.matcher(o1.getKey());
							Matcher match2 = rupees.matcher(o2.getKey());
							if (match1.find()) {
								if (!match2.find()) {
									return -1;
								}
							} else {
								if (match2.find()) {
									return 1;
								}
							}
							if (!(o1.getValue()).equals(o2.getValue())) {
								return (o1.getValue()).compareTo(o2.getValue());
							}
							return (o2.getKey().replace(" ", "")).compareTo(o1.getKey().replace(" ", ""));
						}
					});
					if (debugL <= 2) {
						System.out.println(
								"In ProductPriceData.validate(): priceStrings after cleaning and sorting = " + list);
					}
					if (list.size() > 0) {
						int maxCount = -10;
						for (Entry<String, Integer> anEntry : list) {
							if (anEntry.getValue() > maxCount) {
								likelyOriginalPriceString = anEntry.getKey();
							}
						}
						for (IndexAndPixelHeight pIndex : priceIndices) {
							if (pIndex.originalString.equals(likelyOriginalPriceString)) {
								if ((pIndex.boundingBoxes.size() > 0)
										&& (pIndex.boundingBoxes.size() > likelyPriceBoundingBoxes.size())) {
									likelyPriceBoundingBoxes = pIndex.boundingBoxes;
								}
							}
						}
					} else {
						likelyOriginalPriceString = "";
					}

					if (debugL <= 2) {
						System.out.println("1. In ProductPriceData.validate(). productFound = " + productFound
								+ "; Product PixelHeight = " + productPixelHeight + "; productActualHeight = "
								+ actualProductHeight + "; productBoundingBoxes = " + productBoundingBoxes);
					}

				} else {

					// Finding the likelyOriginalPriceString if product is NOT found
					// First, remove those entries which do not match the choices in the
					// priceIndices table
					Set<String> keys = priceStringCount.keySet();
					if (debugL <= 2) {
						System.out.println("1. In ProductPriceData.validate(). priceStringCount = " + priceStringCount);
					}
					List<String> keyList = keys.stream().collect(Collectors.toCollection(ArrayList::new));
					for (String key : keyList) {
						String strippedDownKey = key.replace(" ", "").replace("_", ".").replace("-", ".")
								.replace("O", "0").replace(",", ".").replace("+", ".").replace(":", ".")
								.replace("/", "").replace("=", ".").replace("|", "").replace("\\", "").trim();

						boolean pFound = false;
						innerloop: for (IndexAndPixelHeight pIndex : priceIndices) {
							int pr = prices.get(pIndex.index);
							int index = strippedDownKey.indexOf(new StringBuffer().append(pr).toString());
							if (index != -1) {
								pFound = true;
								break innerloop;
							}
						}
						if (!pFound) {
							priceStringCount.remove(key);
						}
					}
					// From the rest, sort the HashMap, pick the one with most occurrences, clean it
					// and assign its value to likelyOriginalPriceString

					List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(
							priceStringCount.entrySet());
					// Sort the list
					Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
						@Override
						public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
							return (o2.getValue()).compareTo(o1.getValue());
						}
					});

					if (list.size() > 0) {
						likelyOriginalPriceString = list.get(0).getKey();
						for (IndexAndPixelHeight pIndex : priceIndices) {
							if (pIndex.originalString.equals(likelyOriginalPriceString)) {
								if ((pIndex.boundingBoxes.size() > 0)
										&& (pIndex.boundingBoxes.size() > likelyPriceBoundingBoxes.size())) {
									likelyPriceBoundingBoxes = pIndex.boundingBoxes;
								}
							}
						}
					} else {
						likelyOriginalPriceString = "";
					}
				}

			}

			if (likelyOriginalPriceString == null) {
				// Finding the likelyOriginalPriceString if likelyOriginalPriceString is NOT
				// populated
				// First, remove those entries which do not match the choices in the
				// priceIndices table

				/*
				 * Set<String> keys = priceStringCount.keySet(); if (debugL <= 2) {
				 * System.out.println("2. In ProductPriceData.validate(). priceStringCount = " +
				 * priceStringCount); } List<String> keyList =
				 * keys.stream().collect(Collectors.toCollection(ArrayList::new)); for (String
				 * key : keyList) { String strippedDownKey = key.replace(" ", "").replace("_",
				 * ".").replace("-", ".").replace("O", "0") .replace(",", ".").replace("+",
				 * ".").replace(":", ".").replace("/", "").replace("=", ".") .replace("|",
				 * "").replace("\\", "").trim();
				 *
				 * boolean pFound = false; innerloop: for (IndexAndPixelHeight pIndex :
				 * priceIndices) { int pr = prices.get(pIndex.index); int index =
				 * strippedDownKey.indexOf(new StringBuffer().append(pr).toString()); if (index
				 * != -1) { pFound = true; break innerloop; } } if (!pFound) {
				 * priceStringCount.remove(key); } } // From the rest, sort the HashMap, pick
				 * the one with most occurrences, clean it // and assign its value to
				 * likelyOriginalPriceString
				 *
				 * List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String,
				 * Integer>>( priceStringCount.entrySet()); // Sort the list
				 * Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
				 *
				 * @Override public int compare(Map.Entry<String, Integer> o1, Map.Entry<String,
				 * Integer> o2) { return (o2.getValue()).compareTo(o1.getValue()); } });
				 *
				 * if ((list != null) && (list.size() > 0)) { likelyOriginalPriceString =
				 * list.get(0).getKey(); } else { likelyOriginalPriceString = ""; }
				 */
				HashMap<String, Integer> priceCounter = new HashMap<>();
				for (IndexAndPixelHeight entry : priceIndices) {
					priceCounter.put("" + prices.get(entry.index), 0);
				}

				if (debugL <= 2) {
					System.out.println(
							"In ProductPriceData.validate(): priceCounter HashMap is initialised to - " + priceCounter);
				}

				if (debugL <= 2) {
					System.out.println("In ProductPriceData.validate(): priceStringCount HashMap is initialised to - "
							+ priceStringCount);
				}

				Set<Map.Entry<String, Integer>> priceStringEntries = priceStringCount.entrySet();
				String numbers = "0123456789";
				for (Map.Entry<String, Integer> entry : priceStringEntries) {
					String key = entry.getKey();
					String cleanedKey = key.toUpperCase().replace(" ", "").replace("_", "").replace("-", "")
							.replace("O", "0").replace(",", "").replace("(", " ").replace(")", " ").replace("[", " ")
							.replace("]", " ").replace("'", " ").replace("\"", " ").replace("-", " ").replace("+", "")
							.replace(":", "").replace("/", "").replace("=", "").replace("|", "1").replace("\\", "")
							.replace("Z", "2").replace("D", "0").replace("{", " ").replace("}", " ").replace("I", "1")
							.replace("T", "7").replace("1U", "").replace("2U", "").replace("3U", "").replace("4U", "")
							.replace("5U", "").replace("6U", "").replace("7U", "").replace("8U", "").trim();
					if (CheckProductProperties.productIsGiven) {
						if (CheckProductProperties.givenProductPrice != 0) {
							String productPrice = "" + CheckProductProperties.givenProductPrice;
							if (productPrice.indexOf("2") != -1) {
								cleanedKey = cleanedKey.replace("Z", "2");
							}
							if ((productPrice.indexOf("7") != -1) && (productPrice.indexOf("1") != -1)) {
							} else {
								if (productPrice.indexOf("7") == -1) {
									cleanedKey = cleanedKey.replace("7", "1");
								}
								if (productPrice.indexOf("1") == -1) {
									cleanedKey = cleanedKey.replace("1", "7");
								}
							}
							if ((productPrice.indexOf("5") != -1) && (productPrice.indexOf("6") == -1)) {
								cleanedKey = cleanedKey.replace("6", "5");
							}
							if ((productPrice.indexOf("9") != -1) && (productPrice.indexOf("8") == -1)) {
								cleanedKey = cleanedKey.replace("8", "9");
							}
						}
					}
					Matcher priceMatcher = null;
					synchronized (lock) {
						priceMatcher = pricePattern.matcher(cleanedKey);
					}
					if (priceMatcher.find()) {
						int start = priceMatcher.start();
						int end = priceMatcher.end();
						String tobeReplaced = cleanedKey.substring(start, end).trim();
						cleanedKey = cleanedKey.replace(tobeReplaced, "");
					}

					Matcher retailMatcher = null;
					synchronized (lock) {
						retailMatcher = retailPattern.matcher(cleanedKey);
					}
					if (retailMatcher.find()) {
						int start = retailMatcher.start();
						int end = retailMatcher.end();
						String tobeReplaced = cleanedKey.substring(start, end).trim();
						cleanedKey = cleanedKey.replace(tobeReplaced, "");
					}
					Matcher taxesMatcher = null;
					synchronized (lock) {
						taxesMatcher = taxesPattern.matcher(cleanedKey);
					}
					if (taxesMatcher.find()) {
						int start = taxesMatcher.start();
						int end = taxesMatcher.end();
						String tobeReplaced = cleanedKey.substring(start, end).trim();
						cleanedKey = cleanedKey.replace(tobeReplaced, "");
					}

					Matcher ofAllMatcher = null;
					synchronized (lock) {
						ofAllMatcher = ofAllPattern.matcher(cleanedKey);
					}
					if (ofAllMatcher.find()) {
						int start = ofAllMatcher.start();
						int end = ofAllMatcher.end();
						String tobeReplaced = cleanedKey.substring(start, end).trim();
						cleanedKey = cleanedKey.replace(tobeReplaced, "");
					}

					Matcher rsMatcher = null;
					synchronized (lock) {
						rsMatcher = rsPattern.matcher(cleanedKey);
					}
					if (rsMatcher.find()) {
						int start = rsMatcher.start();
						int end = rsMatcher.end();
						String tobeReplaced = cleanedKey.substring(start, end).trim();
						// remove Rs, and change "S" to "5" in the string
						cleanedKey = cleanedKey.replace(tobeReplaced, "").replace("S", "5");
						// Add back RS, by first checking for a 4-digit sequence
						Pattern sequence = Pattern.compile("\\d{4,5}");
						Matcher aMatcher = sequence.matcher(cleanedKey);
						if (aMatcher.find()) {
							if (debugL <= 2) {
								System.out.println("Adding back RS in " + cleanedKey);
							}
							start = aMatcher.start();
							cleanedKey = cleanedKey.substring(0, start) + "RS" + cleanedKey.substring(start);
						}
					}

					for (Pattern aPattern : ttkPrestigeLtd) {
						Matcher aMatcher = null;
						synchronized (lock) {
							aMatcher = aPattern.matcher(cleanedKey);
						}
						if (aMatcher.find()) {
							int start = aMatcher.start();
							int end = aMatcher.end();
							String tobeReplaced = cleanedKey.substring(start, end).trim();
							cleanedKey = cleanedKey.replace(tobeReplaced, "");
						}
					}
					// now find the index of the first numeric digit in the cleanedKey
					int intIndex = -1;

					for (int indexChar = 0; indexChar < cleanedKey.length(); ++indexChar) {
						String charAtIndex = cleanedKey.substring(indexChar, indexChar + 1);
						if (numbers.indexOf(charAtIndex) != -1) {
							intIndex = indexChar;
							break;
						}
						/*
						 * // For some unknown reason, this piece of code is not working in the
						 * obfuscated jar from ProGuard // Hence, replaced with the current code try {
						 * if (debugL <= 2) { System.out.println("Checking if " + charAtIndex +
						 * " is a number"); } int isItAnInteger = Integer.parseInt(charAtIndex);
						 * intIndex = indexChar; break; } catch (Exception e) { // do nothing //
						 * continue to the next character and check if it is a number }
						 */
					}
					if (debugL <= 2) {
						System.out.println("In ProductPriceData.validate(): Cleaned priceString " + key + " to "
								+ cleanedKey + ", for which first integer is at index " + intIndex);
					}
					// if no integer found, continue to the next entry
					if (intIndex == -1) {
						continue;
					}
					// extract the rest of the substring starting from intIndex and check if it
					// starts with any of the keys in priceCounter
					// if it does, increment the priceCounter of that price by 1
					String priceValue = cleanedKey.substring(intIndex);
					Set<String> shortlistedPrices = priceCounter.keySet();
					for (String aPrice : shortlistedPrices) {
						if (priceValue.startsWith(aPrice)) {
							int currCount = priceCounter.get(aPrice);
							priceCounter.put(aPrice, currCount + entry.getValue());
						}
					}
				}

				if (debugL <= 2) {
					System.out.println("In ProductPriceData.validate(): priceCounter HashMap is - " + priceCounter);
				}

				// now that we have the counts, iterate through the HashMap and get the price
				// string with maximum occurrence

				Set<String> shortlistedPrices = priceCounter.keySet();
				int maxCounter = 0;
				String alternatePriceString = "";
				for (String aPrice : shortlistedPrices) {
					if (priceCounter.get(aPrice) > maxCounter) {
						maxCounter = priceCounter.get(aPrice);
						alternatePriceString = aPrice;
					}
				}
				if ((priceIndices != null) && (priceIndices.size() > 0)) {
					for (IndexAndPixelHeight pIndex : priceIndices) {
						if (pIndex.originalString.equals(alternatePriceString)) {
							if ((pIndex.boundingBoxes.size() > 0)
									&& (pIndex.boundingBoxes.size() > likelyPriceBoundingBoxes.size())) {
								likelyPriceBoundingBoxes = pIndex.boundingBoxes;
							}
						}
					}
				}
				likelyOriginalPriceString = alternatePriceString.replace(" ", "").trim();
			}

			if (debugL <= 2) {
				System.out.println(
						"In ProductPriceData.validate(): likelyOriginalPriceString = " + likelyOriginalPriceString);

			}

			// likelyOriginalPriceString = likelyOriginalPriceString.replace(" ",
			// "").trim();
			// } else {
			// likelyOriginalPriceString = "";
			// }

			if (priceShortlistFound && productFound) {
				product.price.add(Double.valueOf(finalPrice * 1.0));
			} else {

				Set<Double> shortlistedPrices = new TreeSet<>();
				for (IndexAndPixelHeight priceIndex : priceIndices) {
					shortlistedPrices.add(prices.get(priceIndex.index).doubleValue());
				}
				for (Double value : shortlistedPrices) {
					String pr = String.format("%.0f", value);
					if (likelyOriginalPriceString.indexOf(pr) > -1) {
						product.price.add(value);
					}
				}
			}
			double thePrice = 0.0;
			if (product.price.size() <= 1) {
				thePrice = (product.price.size() == 1) ? (double) product.price.get(0) : 0;
			} else {
				ArrayList<Double> prices = new ArrayList<>();
				ArrayList<Integer> priceCounter = new ArrayList<>();
				for (Double aPrice : product.price) {
					if (prices.contains(aPrice)) {
						int index = prices.indexOf(aPrice);
						int counter = priceCounter.get(index).intValue();
						priceCounter.set(index, counter);
					} else {
						prices.add(aPrice);
						priceCounter.add(1);
					}
				}
				int indexOfMax = 0;
				int maxCount = 0;
				for (int i = 0; i < priceCounter.size(); ++i) {
					if (priceCounter.get(i) >= maxCount) {
						indexOfMax = i;
					}
				}
				thePrice = prices.get(indexOfMax);
			}

			// Note that the productBoundingBoxes are not accurate as the product name is
			// usually split into multiple lines...this implementation only gets one of the
			// lines...it needs to be enhanced to get the bounding boxes of all the lines,
			// if that is ever required. Note though that the productName is the full valid
			// name of the product
			OCRProductDimensionsWrapper prodDimensionsWrapper = new OCRProductDimensionsWrapper(productPixelHeight,
					finalProduct, productBoundingBoxes);
			prodDimensionsWrapper.setDebugLevel(debugL);
			prodDimensionsWrapper.process(productPixelHeight, actualProductHeight);

			ArrayList<Rectangle> boundingBoxes = monthFound ? monthYearBoundingBoxes
					: (yearFound ? yearBoundingBoxes : null);

			int monthYearHeight = 0;
			if (boundingBoxes != null) {
				DescriptiveStatistics hStats = new DescriptiveStatistics();
				DescriptiveStatistics wStats = new DescriptiveStatistics();
				for (Rectangle box : boundingBoxes) {
					wStats.addValue(box.width);
				}
				double medianWidth = wStats.getPercentile(50);
				for (Rectangle box : boundingBoxes) {
					if (box.width > (0.35 * medianWidth)) {
						hStats.addValue(box.height);
					}
				}
				monthYearHeight = (int) hStats.getPercentile(50);
			}
			OCRDateDimensionsWrapper dateDimensionsWrapper = new OCRDateDimensionsWrapper(monthYearHeight,
					monthAndYear.toString(), boundingBoxes);
			dateDimensionsWrapper.setDebugLevel(debugL);
			dateDimensionsWrapper.process(productPixelHeight, actualProductHeight);

			int priceHeight = pricePixelHeight;
			if ((likelyPriceBoundingBoxes != null) && (likelyPriceBoundingBoxes.size() > 0)) {
				priceBoundingBoxes = likelyPriceBoundingBoxes;
			}
			if (priceBoundingBoxes != null) {
				DescriptiveStatistics hStats = new DescriptiveStatistics();
				DescriptiveStatistics wStats = new DescriptiveStatistics();
				for (Rectangle box : priceBoundingBoxes) {
					wStats.addValue(box.width);
					hStats.addValue(box.height);
				}
				double medianWidth = wStats.getPercentile(50);
				double medianHeight = hStats.getPercentile(50);
				hStats.clear();
				for (Rectangle box : priceBoundingBoxes) {
					if ((box.width > (0.35 * medianWidth)) && (box.height > (0.35 * medianHeight))) {
						hStats.addValue(box.height);
					}
				}
				priceHeight = (int) hStats.getPercentile(50);
			}
			OCRPriceDimensionsWrapper priceDimensionsWrapper = new OCRPriceDimensionsWrapper(priceHeight,
					likelyOriginalPriceString == null ? new StringBuffer().append(finalPrice).toString()
							: likelyOriginalPriceString,
					priceBoundingBoxes, "" + (int) thePrice);
			priceDimensionsWrapper.setDebugLevel(debugL);
			priceDimensionsWrapper.process(productPixelHeight, actualProductHeight);

			product.setProductName(finalProduct);
			product.setFinalPrice("" + (finalPrice != 0 ? finalPrice : ""));
			product.setProductCharacterHeightActual(actualProductHeight);
			product.setProductCharacterHeightPixels(productPixelHeight);
			product.setPriceCharacterHeightActual(priceDimensionsWrapper.likelyActualHeight);
			product.setPriceCharacterHeightPixels(priceDimensionsWrapper.likelyPixelHeight);
			product.setPriceGapBetween1And2(priceDimensionsWrapper.gapBetween1And2);
			product.setMonth(bestMonthMatch == null ? ""
					: (bestMonthMatch.getValue() == null ? "" : bestMonthMatch.getValue().string2));
			product.setYear(year == null ? "" : year);
			product.setDateCharacterHeightActual(dateDimensionsWrapper.likelyActualHeight);
			product.setDateCharacterHeightPixels(dateDimensionsWrapper.likelyPixelHeight);
			product.setPriceHasADot(priceHasADot);

			if (debugL <= 2) {
				System.out.println("In ProductPriceData.validate(): monthMatches = " + bestMonthMatch);
			}

			if (debugL <= 2) {
				System.out.println("In ProductPriceData.validate(): productFound = " + productFound + "; monthFound = "
						+ monthFound + "; yearFound = " + yearFound + "; dateHeightOK = "
						+ dateDimensionsWrapper.heightOK + "; priceHeightOK = " + priceDimensionsWrapper.heightOK
						+ "; priceDistanceOK = " + priceDimensionsWrapper.distanceOK);
			}

			if (productFound && monthFound && yearFound && priceHasADot) {
				// output.append("true");
				product.productOK = ProductDescription.ALL_OK;
			} else {
				// output.append("false");
				product.productOK = ProductDescription.ERROR_ONLY_ONE_THING_NOT_FOUND_OR_SOME_OTHER_ISSUE;
				if (!productFound) {
					rejectionReason += System.getProperty(productErrorKey);
				}
				if (priceIndices.size() == 0) {
					rejectionReason += System.getProperty(priceErrorKey);
				}
				if (!monthFound) {
					rejectionReason += System.getProperty(monthErrorKey);
				}
				if (!yearFound) {
					rejectionReason += System.getProperty(yearErrorKey);
				}
				// if (productFound && !priceHasADot) {
				if (!priceHasADot) {
					rejectionReason += System.getProperty(priceDotErrorKey);
				}
			}

			if (!dateDimensionsWrapper.heightOK) {
				if (product.productOK == ProductDescription.ALL_OK) {
					product.productOK = ProductDescription.ERROR_DIMENSION_PROBLEM;
				}
				rejectionReason += dateDimensionsWrapper.reasonForRejection;
			}

			if (!priceDimensionsWrapper.heightOK) {
				if (product.productOK == ProductDescription.ALL_OK) {
					product.productOK = ProductDescription.ERROR_DIMENSION_PROBLEM;
				}
				rejectionReason += priceDimensionsWrapper.reasonForRejection;
			}

			if (!priceDimensionsWrapper.distanceOK) {
				if (product.productOK == ProductDescription.ALL_OK) {
					product.productOK = ProductDescription.ERROR_DIMENSION_PROBLEM;
				}
				rejectionReason += priceDimensionsWrapper.reasonForDistanceRejection;
			}

			product.setRejectionReason(rejectionReason);
		}

		if (debugL <= 11) {
			System.out.println("	Just before returning product. Product Description = " + product);
		}

		return product;

	}

	public static ProductDescription validateLabels(ArrayList<OCRStringWrapper> ocrResults, int debugL) {
		if (pkInstance == null) {
			pkInstance = new ProductPriceData();
		}
		return validate(ocrResults, debugL);
	}

	public static String getCleanProductString(String input, int debugL) {
		StringBuffer out = new StringBuffer();
		Set<String> shortlistedStrings = new LinkedHashSet<>();
		Matcher matcher1 = null;
		Matcher matcher2 = null;
		synchronized (lock) {
			matcher1 = capacity1.matcher(input);
			matcher2 = capacity2.matcher(input);
		}
		String inputLine1 = null;
		String capacity = null;
		String capacityCrunched = null;
		// Do matcher2 first and then matcher1 - doing it the other way round is a bug
		// !!
		if (matcher2.find()) {
			int start = matcher2.start();
			int end = matcher2.end();
			capacity = input.substring(start, end).trim();
			capacityCrunched = capacity.replace(" ", "");
			inputLine1 = input.replace(capacity, capacityCrunched);
		} else {
			if (matcher1.find()) {
				int start = matcher1.start();
				int end = matcher1.end();
				// remove year from lineUpper, leaving it with only the month
				capacity = input.substring(start, end).trim();
				capacityCrunched = capacity.replace(" ", "");
				inputLine1 = input.replace(capacity, capacityCrunched);
			} else {
				inputLine1 = input;
			}
		}
		String[] words = inputLine1.split("\\s+");
		outerloop: for (String word : words) {
			for (String productString : uniqueProductStrings) {
				int cutOffValue = 100;
				if ((word.length() <= 3) || (productString.length() <= 3)) {
					cutOffValue = 100; // exact match needed for the shorter strings
				} else {
					if ((word.length() <= 4) || (productString.length() <= 4)) {
						// cutOffValue = 90; // exact match needed for the shorter strings
						// cutOffValue = 75;
						cutOffValue = 100; // even one word cannot be off
					} else {
						if ((word.length() <= 5) || (productString.length() <= 5)) {
							// cutOffValue = 91; // exact match needed for the shorter strings
							// cutOffValue = 80; // one word can be off
							cutOffValue = 100; // even one word cannot be off
						} else {
							if ((word.length() <= 6) || (productString.length() <= 6)) {
								cutOffValue = 83; // fuzzy match is ok; one character can be off
							} else {
								if ((word.length() <= 7) || (productString.length() <= 7)) {
									cutOffValue = 85; // fuzzy match is ok; one character can be off
								} else {
									if ((word.length() <= 8) || (productString.length() <= 8)) {
										cutOffValue = 87; // fuzzy match is ok; one character can be off
									} else {
										if ((word.length() <= 9) || (productString.length() <= 9)) {
											cutOffValue = 89; // fuzzy match is ok; one character can be off
										} else {
											cutOffValue = 93; // fuzzy match is ok; one character can be off
										}
									}
								}
							}
						}
					}

				}
				if (capacityCrunched == null) {
					// if the current statement does not have a capacity word like 3L, 2.5L etc
					// if (debugL <= 2) {
					// System.out.println("FuzzySearch.weightedRatio(" + word + ", " + productString
					// + ") = "
					// + FuzzySearch.weightedRatio(word, productString));
					// }
					if (FuzzySearch.weightedRatio(word, productString) >= cutOffValue) {
						shortlistedStrings.add(word);
						continue outerloop;
					}
				} else {
					// if the current statement has a capacity word like 2L, 1.0L, etc
					if (capacityCrunched.equals(word)) {
						// if the current word is a capacity word
						Set<String> variants = capacityVariants.get(capacityCrunched);
						if (variants != null) {
							for (String variant : variants) {
								if (FuzzySearch.weightedRatio(word, variant) >= 75) {
									shortlistedStrings.add(word);
									continue outerloop;
								}
							}
						}
					} else {
						if (FuzzySearch.weightedRatio(word, productString) >= cutOffValue) {
							// shortlistedStrings.add(word);
							shortlistedStrings.add(productString); // instead of adding the original word, add the
																	// actual productString
							continue outerloop;
						}
					}
				}
			}
		}
		for (String string : shortlistedStrings) {
			out.append(string).append(" ");
		}
		String finalCleanedString = out.toString();
		if (debugL <= 2) {
			System.out.println(" Cleaned product string from : [" + input + "] is - " + finalCleanedString + ". ");
		}
		return finalCleanedString;
	}

	public static ProductDescription validateOld1(ArrayList<OCRStringWrapper> ocrResults, int debugL) {

		// StringBuffer output = new StringBuffer();
		ProductDescription product = new ProductDescription();
		String rejectionReason = "";

		ArrayList<Rectangle> monthYearBoundingBoxes = null;
		ArrayList<Rectangle> priceBoundingBoxes = null;
		ArrayList<Rectangle> productBoundingBoxes = null;

		boolean monthFound = false;
		boolean yearFound = false;
		StringBuffer monthAndYear = new StringBuffer();
		// int monthPixelHeight = 0;

		// find the best match for date and year
		ArrayList<Entry<Integer, StringAndPixelHeight>> monthMatches = new ArrayList<>();

		for (OCRStringWrapper wrapper : ocrResults) {
			for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
				String[] lines = sentence.split("\r\n|\r|\n");
				for (String line : lines) {
					Entry<Integer, StringAndPixelHeight> month = processForMonth(line, wrapper.getLikelyPixelHeight(),
							debugL);
					if (month != null) {
						monthMatches.add(month);
						if (wrapper.getBoundingBoxes() != null) {
							// monthYearBoundingBoxes = wrapper.getBoundingBoxes();
							month.getValue().boundingBoxes = wrapper.getBoundingBoxes();
						}
					}
					// if (month != null) {
					// monthAndYear.append(month).append(" ");
					// monthFound = true;
					// break outerloop;
					// }
				}
			}
		}

		Entry<Integer, StringAndPixelHeight> bestMonthMatch = null;
		for (Entry<Integer, StringAndPixelHeight> aMonth : monthMatches) {
			if (bestMonthMatch == null) {
				bestMonthMatch = aMonth;
				monthYearBoundingBoxes = aMonth.getValue().boundingBoxes;
				continue;
			}
			if (aMonth.getKey() >= bestMonthMatch.getKey()) {
				bestMonthMatch = aMonth;
				monthYearBoundingBoxes = aMonth.getValue().boundingBoxes;
			}
		}

		if (bestMonthMatch != null) {
			monthAndYear.append(bestMonthMatch.getValue().string2).append(" ");
			monthFound = bestMonthMatch.getValue().monthFound;
			// if (!monthFound) {
			// rejectionReason += System.getProperty(monthErrorKey);
			// ;
			// }
		}

		if (debugL <= 3) {
			if (bestMonthMatch != null) {
				System.out.println("1. In ProductPriceData.validate(). Month = " + monthAndYear + "; PixelHeight = "
						+ bestMonthMatch.getValue().pixelHeight2 + "; Rectangles = " + monthYearBoundingBoxes);
			}
		}

		String year = null;
		ArrayList<Rectangle> yearBoundingBoxes = null;
		outerloop: for (OCRStringWrapper wrapper : ocrResults) {
			for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
				String[] lines = sentence.split("\r\n|\r|\n");
				HashMap<String, Integer> yearStrings = new HashMap<>();
				for (String line : lines) {
					if (yearStrings.containsKey(line)) {
						int count = yearStrings.get(line);
						yearStrings.put(line, ++count);
					} else {
						yearStrings.put(line, 1);
					}
				}
				List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(
						yearStrings.entrySet());
				Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
					@Override
					public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
						return (o2.getValue()).compareTo(o1.getValue());
					}
				});
				for (Map.Entry<String, Integer> line : list) {
					year = processForYear(line.getKey(), debugL);
					if (year != null) {
						monthAndYear.append(year);
						yearFound = true;
						yearBoundingBoxes = wrapper.getBoundingBoxes();
						break outerloop;
					}
				}
			}
		}

		// find all the matches for price
		ArrayList<IndexAndPixelHeight> priceIndices = new ArrayList<>();
		ArrayList<String[]> priceLines = new ArrayList<>();
		for (OCRStringWrapper wrapper : ocrResults) {
			int pixelHeight = wrapper.getLikelyPixelHeight();
			for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
				String[] lines = sentence.split("\r\n|\r|\n");
				int priceIndicesLength = priceIndices.size();
				for (String line : lines) {
					processForPrice(line, pixelHeight, wrapper.getBoundingBoxes(), priceIndices, debugL);
				}
				if (priceIndices.size() > priceIndicesLength) {
					priceLines.add(lines);
				}
			}
		}

		HashMap<String, Integer> priceStringCount = new HashMap<>();
		if (priceLines.size() > 0) {
			for (String[] priceLineArray : priceLines) {
				for (String aPriceLine : priceLineArray) {
					Set<String> keys = priceStringCount.keySet();
					if (keys.contains(aPriceLine)) {
						int count = priceStringCount.get(aPriceLine);
						priceStringCount.put(aPriceLine, ++count);
					} else {
						priceStringCount.put(aPriceLine, 1);
					}
				}
			}
		}

		if (debugL <= 2) {
			StringBuffer out = new StringBuffer();
			for (IndexAndPixelHeight entry : priceIndices) {
				out.append("Rs ").append(prices.get(entry.index)).append(" ; ");
			}
			System.out.println("Price choices are - " + out.toString());
		}

		if (debugL <= 2) {
			System.out.println("2. In ProductPriceData.validate(). ");
			for (IndexAndPixelHeight entry : priceIndices) {
				System.out.println(entry);
			}
		}

		// find the 3 best matches for product
		// first, find all the relevant strings from the OCR results that matter
		/*
		 * ArrayList<String> productSubStrings = new ArrayList<>(); for
		 * (OCRStringWrapper wrapper : ocrResults) { for (String sentence :
		 * wrapper.getOcrString().split(System.lineSeparator())) { String[] lines =
		 * sentence.split("\r\n|\r|\n"); for (String line : lines) { if (debugL <= 2) {
		 * System.out.print("Rectifying string : " + line); } String lineUpper =
		 * rectifyString(line); if (debugL <= 2) {
		 * System.out.print(". Cleaning product line : " + lineUpper); } lineUpper =
		 * getCleanProductString(lineUpper, debugL); if (debugL <= 2) {
		 * System.out.println(" to get cleaned product line as : " + lineUpper); } //
		 * boolean couldBeProductLine = neitherPriceNorDateLine.equals( //
		 * this.checkIfLineIsDateOrPrice(lineUpper, bestMonthMatch, year, priceIndices,
		 * // debugL)); // boolean couldBeProductLine = checkIfProductLine(lineUpper,
		 * debugL); boolean couldBeProductLine = lineUpper.length() > 0; if
		 * (couldBeProductLine) { // String rectifiedLine = cleanProductLine(lineUpper,
		 * debugL); String rectifiedLine = lineUpper; if (rectifiedLine != null) {
		 * List<ExtractedResult> prodMatches = null; if (rectifiedLine.length() > 10) {
		 * prodMatches = findProductIndexUsingFuzzyWuzzy(rectifiedLine, 89, debugL); for
		 * (ExtractedResult result : prodMatches) { if
		 * (!productSubStrings.contains(rectifiedLine)) {
		 * productSubStrings.add(rectifiedLine); } } } } } } } }
		 */

		ArrayList<String> productSubStrings = new ArrayList<>();
		for (OCRStringWrapper wrapper : ocrResults) {
			if (debugL <= 7) {
				System.out.println("String in wrapper is : ");
				System.out.println("----------------------");
				System.out.println(wrapper.ocrString);
			}
			String tempString = null;
			for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
				if (debugL <= 7) {
					System.out.println("String in sentence is : ");
					System.out.println("------------------------");
					System.out.println(sentence);
				}
				String[] lines = sentence.split("\r\n|\r|\n");
				innerloop: for (String line : lines) {
					if (debugL <= 7) {
						System.out.println("String in line is : ");
						System.out.println(line);
					}

					if (debugL <= 7) {
						System.out.print("Rectifying string : " + line);
					}
					String lineUpper = rectifyString(line);
					if (debugL <= 7) {
						System.out.print(". Cleaning product line : " + lineUpper);
					}
					lineUpper = getCleanProductString(lineUpper, debugL);
					if (debugL <= 7) {
						System.out.println(" to get cleaned product line as : " + lineUpper);
					}
					boolean couldBeProductLine = lineUpper.length() > 0;
					if (couldBeProductLine) {
						// check if a product substring exists with same content
						// if no, then add this line; else, even if its exists, replace with this line
						// if this line is of longer length
						if (tempString == null) {
							tempString = lineUpper;
							continue innerloop;
						} else {
							int newScore = FuzzySearch.weightedRatio(tempString, lineUpper);
							if ((newScore > 89) && (lineUpper.length() > tempString.length())) {
								tempString = lineUpper;
								continue innerloop;
							}
						}
					}
				}
			}
			if (tempString != null) {
				productSubStrings.add(tempString);
			}
		}

		int size = productSubStrings.size();

		if (size == 0) {
			// if no strings found, repeat the above sequence with a lesser cut-off
			/*
			 * for (OCRStringWrapper wrapper : ocrResults) { for (String sentence :
			 * wrapper.getOcrString().split(System.lineSeparator())) { String[] lines =
			 * sentence.split("\r\n|\r|\n"); for (String line : lines) { if (debugL <= 2) {
			 * System.out.print("Rectifying string : " + line); } String lineUpper =
			 * rectifyString(line); if (debugL <= 2) {
			 * System.out.print(". Cleaning product line : " + lineUpper); } lineUpper =
			 * getCleanProductString(lineUpper, debugL); if (debugL <= 2) {
			 * System.out.println(" to get cleaned product line as : " + lineUpper); } //
			 * boolean couldBeProductLine = checkIfProductLine(lineUpper, debugL); boolean
			 * couldBeProductLine = lineUpper.length() > 0; if (couldBeProductLine) { //
			 * String rectifiedLine = cleanProductLine(lineUpper, debugL); String
			 * rectifiedLine = lineUpper; if (rectifiedLine != null) { List<ExtractedResult>
			 * prodMatches = null; if (rectifiedLine.length() > 10) { prodMatches =
			 * findProductIndexUsingFuzzyWuzzy(rectifiedLine, 85, debugL); for
			 * (ExtractedResult result : prodMatches) { if
			 * (!productSubStrings.contains(rectifiedLine)) {
			 * productSubStrings.add(rectifiedLine); } } } } } } } }
			 */
			for (OCRStringWrapper wrapper : ocrResults) {
				if (debugL <= 7) {
					System.out.println("String in wrapper is : ");
					System.out.println("----------------------");
					System.out.println(wrapper.ocrString);
				}
				String tempString = null;
				for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
					if (debugL <= 7) {
						System.out.println("String in sentence is : ");
						System.out.println("------------------------");
						System.out.println(sentence);
					}
					String[] lines = sentence.split("\r\n|\r|\n");
					innerloop: for (String line : lines) {
						if (debugL <= 7) {
							System.out.println("String in line is : ");
							System.out.println(line);
						}

						if (debugL <= 7) {
							System.out.print("Rectifying string : " + line);
						}
						String lineUpper = rectifyString(line);
						if (debugL <= 7) {
							System.out.print(". Cleaning product line : " + lineUpper);
						}
						lineUpper = getCleanProductString(lineUpper, debugL);
						if (debugL <= 7) {
							System.out.println(" to get cleaned product line as : " + lineUpper);
						}
						boolean couldBeProductLine = lineUpper.length() > 0;
						if (couldBeProductLine) {
							// check if a product substring exists with same content
							// if no, then add this line; else, even if its exists, replace with this line
							// if this line is of longer length
							if (tempString == null) {
								tempString = lineUpper;
								continue innerloop;
							} else {
								int newScore = FuzzySearch.weightedRatio(tempString, lineUpper);
								if ((newScore > 85) && (lineUpper.length() > tempString.length())) {
									tempString = lineUpper;
									continue innerloop;
								}
							}
						}
					}
				}
				if (tempString != null) {
					productSubStrings.add(tempString);
				}
			}
		}

		size = productSubStrings.size();

		if (size == 0) {
			// if no strings found, repeat the above sequence with an even lesser cut-off
			/*
			 * for (OCRStringWrapper wrapper : ocrResults) { for (String sentence :
			 * wrapper.getOcrString().split(System.lineSeparator())) { String[] lines =
			 * sentence.split("\r\n|\r|\n"); for (String line : lines) { if (debugL <= 2) {
			 * System.out.print("Rectifying string : " + line); } String lineUpper =
			 * rectifyString(line); if (debugL <= 2) {
			 * System.out.print(". Cleaning product line : " + lineUpper); } lineUpper =
			 * getCleanProductString(lineUpper, debugL); if (debugL <= 2) {
			 * System.out.println(" to get cleaned product line as : " + lineUpper); } //
			 * boolean couldBeProductLine = checkIfProductLine(lineUpper, debugL); boolean
			 * couldBeProductLine = lineUpper.length() > 0; if (couldBeProductLine) { //
			 * String rectifiedLine = cleanProductLine(lineUpper, debugL); String
			 * rectifiedLine = lineUpper; if (rectifiedLine != null) { List<ExtractedResult>
			 * prodMatches = null; if (rectifiedLine.length() > 10) { prodMatches =
			 * findProductIndexUsingFuzzyWuzzy(rectifiedLine, 80, debugL); for
			 * (ExtractedResult result : prodMatches) { if
			 * (!productSubStrings.contains(rectifiedLine)) {
			 * productSubStrings.add(rectifiedLine); } } } } } } } // each wrapper finishes
			 * here }
			 */
			for (OCRStringWrapper wrapper : ocrResults) {
				if (debugL <= 7) {
					System.out.println("String in wrapper is : ");
					System.out.println("----------------------");
					System.out.println(wrapper.ocrString);
				}
				String tempString = null;
				for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
					if (debugL <= 7) {
						System.out.println("String in sentence is : ");
						System.out.println("------------------------");
						System.out.println(sentence);
					}
					String[] lines = sentence.split("\r\n|\r|\n");
					innerloop: for (String line : lines) {
						if (debugL <= 7) {
							System.out.println("String in line is : ");
							System.out.println(line);
						}

						if (debugL <= 7) {
							System.out.print("Rectifying string : " + line);
						}
						String lineUpper = rectifyString(line);
						if (debugL <= 7) {
							System.out.print(". Cleaning product line : " + lineUpper);
						}
						lineUpper = getCleanProductString(lineUpper, debugL);
						if (debugL <= 7) {
							System.out.println(" to get cleaned product line as : " + lineUpper);
						}
						boolean couldBeProductLine = lineUpper.length() > 0;
						if (couldBeProductLine) {
							// check if a product substring exists with same content
							// if no, then add this line; else, even if its exists, replace with this line
							// if this line is of longer length
							if (tempString == null) {
								tempString = lineUpper;
								continue innerloop;
							} else {
								int newScore = FuzzySearch.weightedRatio(tempString, lineUpper);
								if ((newScore > 80) && (lineUpper.length() > tempString.length())) {
									tempString = lineUpper;
									continue innerloop;
								}
							}
						}
					}
				}
				if (tempString != null) {
					productSubStrings.add(tempString);
				}
			}
		}

		size = productSubStrings.size();

		int finalPrice = 0;
		int pricePixelHeight = 0;
		String finalProduct = "";
		int actualProductHeight = 0;
		boolean productFound = false;
		int productPixelHeight = 0;
		boolean priceShortlistFound = true;

		if (size == 0) {
			// if no strings found, then no product match exists
			productFound = false;
			if (debugL <= 8) {
				System.out.println("No match found for product");
				for (IndexAndPixelHeight price : priceIndices) {
					System.out.println("Price : Rs. " + prices.get(price.index));
				}
				if (monthFound && yearFound) {
					System.out.println("Month of Manufacture : " + monthAndYear);
				}
			}

			/*
			 * output.append("false").append(separator).append("").append(separator); for
			 * (int index : priceIndices) {
			 * output.append(this.prices.get(index)).append(separator); }
			 * output.append(monthAndYear);
			 *
			 * return output.toString();
			 */
			// product.productOK = ProductDescription.ERROR_PRODUCT_AND_PRICE_NOT_FOUND;
			// product.productName = "";
			// product.monthAndYear = monthAndYear.toString();
			// unique values of price needed; hence, add values to a set, and then populate
			// the arraylist from the set
			// product.price.add(this.prices.get(index).doubleValue());
			priceShortlistFound = false;
			// return product;

		}

		String likelyOriginalPriceString = null;

		if (priceShortlistFound) {
			// pick unique strings from productSubStrings, and populate them in a new array
			// called productStrings
			/*
			 * ArrayList<String> productTempStrings = new ArrayList<>(); outerloop: for (int
			 * i = 0; i < size; ++i) { int newSize = productTempStrings.size(); String
			 * ocrString = productSubStrings.get(i); for (int j = 0; j < newSize; ++j) { //
			 * if a product string is there in the array that already matches another that
			 * // has been chosen, then ignore it. However, if the new string is longer than
			 * // the current string, then replace the current string with the new string if
			 * (FuzzySearch.weightedRatio(ocrString, productTempStrings.get(j)) > 70) { if
			 * (ocrString.length() > productTempStrings.get(j).length()) {
			 * productTempStrings.remove(j); productTempStrings.add(j, ocrString.trim()); }
			 * else { continue outerloop; } } } productTempStrings.add(ocrString); }
			 */
			// go through the productStrings array and remove duplicates & close matches
			// ArrayList<String> productStrings = new ArrayList<>();
			// First, sort the arraylist by length of the elements
			/*
			 * for (int i = 0; i < productTempStrings.size(); ++i) { String productString =
			 * productTempStrings.get(i); int newSize = productStrings.size(); int index =
			 * 0; innerloop: for (int j = 0; j < newSize; ++j) { if (productString.length()
			 * > productStrings.get(j).length()) { index = j; break innerloop; } ++index; }
			 * productStrings.add(index, productString); }
			 */
			/*
			 * if (debugL <= 2) { System.out.println("Product Strings = " + productStrings);
			 * }
			 */
			// ...and, then, go through and eliminate strings that match closely (>= 96)

			/*
			 * ArrayList<String> finalProductStrings = new ArrayList<>(); outerloop: for
			 * (int i = 0; i < productStrings.size(); ++i) { String productString =
			 * productStrings.get(i); int newSize = finalProductStrings.size(); for (int j =
			 * 0; j < newSize; ++j) { // if a product string is there in the array that
			 * already matches another that // has been chosen, then ignore it if
			 * (FuzzySearch.weightedRatio(productString, productStrings.get(j)) > 95) {
			 * continue outerloop; } } finalProductStrings.add(productString); }
			 *
			 * if (debugL <= 2) { System.out.println("Final Product Strings = " +
			 * finalProductStrings); }
			 *
			 * // create the final search string by concatenating the strings from the array
			 * // productStrings StringBuffer finalSearchString = new StringBuffer(); for
			 * (String string : finalProductStrings) {
			 * finalSearchString.append(string).append(" "); }
			 *
			 * if (debugL <= 7) { System.out.println("Final search string = " +
			 * finalSearchString.toString()); }
			 */

			ArrayList<String> finalProductStrings = new ArrayList<>();
			outerloop: for (int i = 0; i < productSubStrings.size(); ++i) {
				String productString = productSubStrings.get(i);
				int newSize = finalProductStrings.size();
				for (int j = 0; j < newSize; ++j) {
					// if a product string is there in the array that already matches another that
					// has been chosen, then ignore it
					if (FuzzySearch.weightedRatio(productString, finalProductStrings.get(j)) > 95) {
						continue outerloop;
					}
				}
				finalProductStrings.add(productString);
			}

			StringBuffer allStrings = new StringBuffer(0);
			// Set<String> capacityVariantKeys = capacityVariants.keySet();
			for (String subString : finalProductStrings) {
				String[] subStringTokens = subString.split(" ");
				for (String token : subStringTokens) {
					Set<String> variants = capacityVariants.get(token);
					if (variants == null) {
						allStrings.append(token).append(" ");
					} else {
						for (String variant : variants) {
							allStrings.append(variant).append(" ");
						}
					}
				}
			}
			String finalSearchString = allStrings.toString();
			if (debugL <= 7) {
				System.out.println("Final search string = " + finalSearchString);
			}

			/*
			 * List<ExtractedResult> prodMatches = null; prodMatches =
			 * findProductIndexUsingFuzzyWuzzy(finalString, 89, debugL); for
			 * (ExtractedResult result : prodMatches) {
			 * shortListedProducts.add(result.getString()); }
			 */
			// List<ExtractedResult> productMatches =
			// FuzzySearch.extractTop(finalSearchString.toString(), products, 30,
			// 70);
			List<ExtractedResult> productMatches = FuzzySearch.extractTop(finalSearchString, products, 50, 70);

			if (debugL <= 7) {
				System.out.println(productMatches);
			}

			/*
			 * ArrayList<ExtractedResult> productMatches = new ArrayList<>(3); for
			 * (OCRResult ocrResult : ocrResults) { for (String sentence :
			 * ocrResult.sentences) { String[] lines =
			 * sentence.lines().toArray(String[]::new); for (String line : lines) {
			 * List<ExtractedResult> prodMatches =
			 * this.findProductIndexUsingFuzzyWuzzy(line); for (ExtractedResult result :
			 * prodMatches) { if (!productSubStrings.contains(line)) {
			 * productSubStrings.add(line); } int size = productMatches.size(); if (size <
			 * 3) { productMatches.add(result); } else { if (result.getScore() >
			 * productMatches.get(0).getScore()) { productMatches.add(0, result);
			 * productMatches.remove(3); } else { if (result.getScore() >
			 * productMatches.get(1).getScore()) { productMatches.add(1, result);
			 * productMatches.remove(3); } else { if (result.getScore() >
			 * productMatches.get(2).getScore()) { productMatches.add(2, result);
			 * productMatches.remove(3); } } } } } } } System.out.println(productMatches); }
			 */

			// find which product and price combination is valid
			outerloop: for (ExtractedResult result : productMatches) {
				int productIndex = result.getIndex();
				for (IndexAndPixelHeight priceIndex : priceIndices) {
					if ((priceIndex.boundingBoxes != null) && (priceIndex.boundingBoxes.size() > 1)) {
						if (priceIndex.index == productIndex) {
							finalPrice = prices.get(priceIndex.index);
							finalProduct = originalProductStrings.get(productIndex);
							actualProductHeight = originalProductHeights.get(productIndex);
							priceBoundingBoxes = priceIndex.boundingBoxes;
							pricePixelHeight = priceIndex.pixelHeight1;
							productFound = true;
							break outerloop;
						}
					}
				}
			}

			if (debugL <= 8) {
				System.out.println("Product : " + finalProduct);
				System.out.println("Price : Rs. " + finalPrice);
				if (monthFound && yearFound) {
					System.out.println("Month of Manufacture : " + monthAndYear);
				}
			}

			if (productFound) {
				// determine now which OCRBufferedImageWrapper has a product name and get its
				// pixelHeight
				if (debugL <= 2) {
					System.out.println("1. In ProductPriceData.validate(). Comparing finalProduct = " + finalProduct
							+ " with the strings");
				}
				HashMap<OCRStringWrapper, Integer> bestProductFit = new HashMap<>();
				for (OCRStringWrapper wrapper : ocrResults) {
					for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
						String[] lines = sentence.split("\r\n|\r|\n");
						for (String line : lines) {
							if (line.length() > 15) {
								int weightedRatio = FuzzySearch.weightedRatio(finalProduct, line);
								if (debugL <= 2) {
									System.out.println("1. In ProductPriceData.validate(). Compared with line = " + line
											+ " to get FuzzySearch.weightedRatio() value of " + weightedRatio);
								}
								if (weightedRatio >= 75) {
									if (bestProductFit.get(wrapper) == null) {
										bestProductFit.put(wrapper, weightedRatio);
									} else {
										int currentWeight = bestProductFit.get(wrapper);
										if (weightedRatio > currentWeight) {
											bestProductFit.put(wrapper, weightedRatio);
										}
									}
									// OCRStringWrapper found
									// productPixelHeight = wrapper.likelyPixelHeight;
									// productBoundingBoxes = wrapper.getBoundingBoxes();
									// break outerloop;
								}
							}
						}
					}
				}

				// Sort bestProductFit by weightedRatio

				List<Map.Entry<OCRStringWrapper, Integer>> productMatchList = new LinkedList<Map.Entry<OCRStringWrapper, Integer>>(
						bestProductFit.entrySet());
				// Sort the list
				Collections.sort(productMatchList, new Comparator<Map.Entry<OCRStringWrapper, Integer>>() {

					@Override
					public int compare(Map.Entry<OCRStringWrapper, Integer> o1,
							Map.Entry<OCRStringWrapper, Integer> o2) {
						return (o2.getValue()).compareTo(o1.getValue());
					}
				});

				OCRStringWrapper topProductFit = null;
				if (productMatchList.size() > 0) {
					topProductFit = productMatchList.get(0).getKey();
				}

				if (topProductFit != null) {
					productPixelHeight = topProductFit.likelyPixelHeight;
					productBoundingBoxes = topProductFit.getBoundingBoxes();
				} else {
					productPixelHeight = 0;
					productBoundingBoxes = new ArrayList<>();
				}

				// Finding the likelyOriginalPriceString if product is found
				// First, remove those entries which do not match the finalPrice
				if (debugL <= 2) {
					System.out.println("priceStringCount before removing irrelevant keys = " + priceStringCount);
				}
				Set<String> keys = priceStringCount.keySet();
				List<String> keyList = keys.stream().collect(Collectors.toCollection(ArrayList::new));
				for (String key : keyList) {
					// String strippedDownKey = key.replace(" ", "").replace(".", "").replace("_",
					// "").replace("-", "")
					// .replace("O", "0").replace(",", "").replace(":", "").trim();
					String strippedDownKey = key.replace(" ", "").replace("_", ".").replace("-", ".").replace("O", "0")
							.replace(",", ".").replace("+", ".").replace(":", ".").replace("/", "").replace("=", ".")
							.replace("|", "").replace("\\", "").trim();

					int index = strippedDownKey.indexOf(new StringBuffer().append(finalPrice).toString());
					if (index == -1) {
						priceStringCount.remove(key);
					}
				}
				if (debugL <= 2) {
					System.out.println("priceStringCount after removing irrelevant keys = " + priceStringCount);
				}
				// From the remaining strings in priceStringCount, sort the HashMap, pick the
				// one with most occurrences, clean it and assign its value to
				// likelyOriginalPriceString

				HashMap<String, Integer> priceStringCountCleaned = new HashMap<>();

				for (Map.Entry<String, Integer> entry : priceStringCount.entrySet()) {
					String originalString = entry.getKey();
					// String newString = originalString.replace(" ", "").replace(".",
					// "").replace("_", "")
					// .replace("-", "").replace("O", "0").replace(",", "").replace("+",
					// "").replace(":", "")
					// .replace("/", "").replace("=", "").replace("|", "").replace("\\", "").trim();
					String newString = originalString.replace(" ", "").replace("_", ".").replace("-", ".")
							.replace("O", "0").replace(",", ".").replace("+", ".").replace(":", ".").replace("/", "")
							.replace("=", ".").replace("|", "").replace("\\", "").trim();
					if (priceStringCountCleaned.containsKey(newString)) {
						int currentCount = priceStringCountCleaned.get(newString);
						priceStringCountCleaned.put(newString, currentCount + 1);
					} else {
						priceStringCountCleaned.put(newString, entry.getValue());
					}
				}

				List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(
						priceStringCountCleaned.entrySet());
				// Sort the list
				Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
					@Override
					public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
						return (o2.getValue()).compareTo(o1.getValue());
					}
				});
				if (debugL <= 2) {
					System.out.println("priceStrings after cleaning and sorting = " + list);
				}
				if (list.size() > 0) {
					likelyOriginalPriceString = list.get(0).getKey();
				} else {
					likelyOriginalPriceString = "";
				}

				/*
				 * // put data from sorted list to hashmap HashMap<String, Integer> temp = new
				 * LinkedHashMap<String, Integer>(); for (Map.Entry<String, Integer> aa : list)
				 * { temp.put(aa.getKey(), aa.getValue()); }
				 */

				if (debugL <= 2) {
					System.out.println("1. In ProductPriceData.validate(). productFound = " + productFound
							+ "; Product PixelHeight = " + productPixelHeight + "; productActualHeight = "
							+ actualProductHeight + "; productBoundingBoxes = " + productBoundingBoxes);
				}

			} else {

				// Finding the likelyOriginalPriceString if product is NOT found
				// First, remove those entries which do not match the choices in the
				// priceIndices table
				Set<String> keys = priceStringCount.keySet();
				if (debugL <= 2) {
					System.out.println("1. In ProductPriceData.validate(). priceStringCount = " + priceStringCount);
				}
				List<String> keyList = keys.stream().collect(Collectors.toCollection(ArrayList::new));
				for (String key : keyList) {
					// String strippedDownKey = key.replace(" ", "").replace(".", "").replace("_",
					// "").replace("-", "")
					// .replace("O", "0").replace(",", "").replace("+", "").replace(":", "").trim();
					String strippedDownKey = key.replace(" ", "").replace("_", ".").replace("-", ".").replace("O", "0")
							.replace(",", ".").replace("+", ".").replace(":", ".").replace("/", "").replace("=", ".")
							.replace("|", "").replace("\\", "").trim();

					boolean pFound = false;
					innerloop: for (IndexAndPixelHeight pIndex : priceIndices) {
						int pr = prices.get(pIndex.index);
						int index = strippedDownKey.indexOf(new StringBuffer().append(pr).toString());
						if (index != -1) {
							pFound = true;
							break innerloop;
						}
					}
					if (!pFound) {
						priceStringCount.remove(key);
					}
				}
				// From the rest, sort the HashMap, pick the one with most occurrences, clean it
				// and assign its value to likelyOriginalPriceString

				List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(
						priceStringCount.entrySet());
				// Sort the list
				Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
					@Override
					public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
						return (o2.getValue()).compareTo(o1.getValue());
					}
				});

				if (list.size() > 0) {
					likelyOriginalPriceString = list.get(0).getKey();
				} else {
					likelyOriginalPriceString = "";
				}
			}

		}

		// likelyOriginalPriceString = likelyOriginalPriceString.replace(" ",
		// "").replace(".", "").replace("-", "")
		// .replace("_", "").replace(",", "").trim();
		// if (likelyOriginalPriceString != null) {

		if (likelyOriginalPriceString == null) {
			// Finding the likelyOriginalPriceString if likelyOriginalPriceString is NOT
			// populated
			// First, remove those entries which do not match the choices in the
			// priceIndices table
			Set<String> keys = priceStringCount.keySet();
			if (debugL <= 2) {
				System.out.println("1. In ProductPriceData.validate(). priceStringCount = " + priceStringCount);
			}
			List<String> keyList = keys.stream().collect(Collectors.toCollection(ArrayList::new));
			for (String key : keyList) {
				// String strippedDownKey = key.replace(" ", "").replace(".", "").replace("_",
				// "").replace("-", "")
				// .replace("O", "0").replace(",", "").replace("+", "").replace(":",
				// "").replace("=", "")
				// .replace("/", "").replace("|", "").trim();
				String strippedDownKey = key.replace(" ", "").replace("_", ".").replace("-", ".").replace("O", "0")
						.replace(",", ".").replace("+", ".").replace(":", ".").replace("/", "").replace("=", ".")
						.replace("|", "").replace("\\", "").trim();

				boolean pFound = false;
				innerloop: for (IndexAndPixelHeight pIndex : priceIndices) {
					int pr = prices.get(pIndex.index);
					int index = strippedDownKey.indexOf(new StringBuffer().append(pr).toString());
					if (index != -1) {
						pFound = true;
						break innerloop;
					}
				}
				if (!pFound) {
					priceStringCount.remove(key);
				}
			}
			// From the rest, sort the HashMap, pick the one with most occurrences, clean it
			// and assign its value to likelyOriginalPriceString

			List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(
					priceStringCount.entrySet());
			// Sort the list
			Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {

				@Override
				public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
					return (o2.getValue()).compareTo(o1.getValue());
				}
			});

			if ((list != null) && (list.size() > 0)) {
				likelyOriginalPriceString = list.get(0).getKey();
			} else {
				likelyOriginalPriceString = "";
			}
		}

		likelyOriginalPriceString = likelyOriginalPriceString.replace(" ", "").trim();
		// } else {
		// likelyOriginalPriceString = "";
		// }

		if (priceShortlistFound && productFound) {
			product.price.add(Double.valueOf(finalPrice * 1.0));
		} else {

			Set<Double> shortlistedPrices = new TreeSet<>();
			for (IndexAndPixelHeight priceIndex : priceIndices) {
				shortlistedPrices.add(prices.get(priceIndex.index).doubleValue());
			}
			for (Double value : shortlistedPrices) {
				product.price.add(value);
			}
		}
		double thePrice = 0.0;
		if (product.price.size() <= 1) {
			thePrice = (product.price.size() == 1) ? (double) product.price.get(0) : 0;
		} else {
			ArrayList<Double> prices = new ArrayList<>();
			ArrayList<Integer> priceCounter = new ArrayList<>();
			for (Double aPrice : product.price) {
				if (prices.contains(aPrice)) {
					int index = prices.indexOf(aPrice);
					int counter = priceCounter.get(index).intValue();
					priceCounter.set(index, counter);
				} else {
					prices.add(aPrice);
					priceCounter.add(1);
				}
			}
			int indexOfMax = 0;
			int maxCount = 0;
			for (int i = 0; i < priceCounter.size(); ++i) {
				if (priceCounter.get(i) >= maxCount) {
					indexOfMax = i;
				}
			}
			thePrice = prices.get(indexOfMax);
		}

		// Note that the productBoundingBoxes are not accurate as the product name is
		// usually split into multiple lines...this implementation only gets one of the
		// lines...it needs to be enhanced to get the bounding boxes of all the lines,
		// if that is ever required. Note though that the productName is the full valid
		// name of the product
		OCRProductDimensionsWrapper prodDimensionsWrapper = new OCRProductDimensionsWrapper(productPixelHeight,
				finalProduct, productBoundingBoxes);
		prodDimensionsWrapper.setDebugLevel(debugL);
		prodDimensionsWrapper.process(productPixelHeight, actualProductHeight);

		ArrayList<Rectangle> boundingBoxes = monthFound ? monthYearBoundingBoxes
				: (yearFound ? yearBoundingBoxes : null);
		int monthYearHeight = 0;
		if (boundingBoxes != null) {
			DescriptiveStatistics hStats = new DescriptiveStatistics();
			DescriptiveStatistics wStats = new DescriptiveStatistics();
			for (Rectangle box : boundingBoxes) {
				wStats.addValue(box.width);
			}
			double medianWidth = wStats.getPercentile(50);
			for (Rectangle box : boundingBoxes) {
				if (box.width > (0.35 * medianWidth)) {
					hStats.addValue(box.height);
				}
			}
			monthYearHeight = (int) hStats.getPercentile(50);
		}
		OCRDateDimensionsWrapper dateDimensionsWrapper = new OCRDateDimensionsWrapper(monthYearHeight,
				monthAndYear.toString(), boundingBoxes);
		dateDimensionsWrapper.setDebugLevel(debugL);
		dateDimensionsWrapper.process(productPixelHeight, actualProductHeight);

		int priceHeight = pricePixelHeight;
		if (priceBoundingBoxes != null) {
			DescriptiveStatistics hStats = new DescriptiveStatistics();
			DescriptiveStatistics wStats = new DescriptiveStatistics();
			for (Rectangle box : priceBoundingBoxes) {
				wStats.addValue(box.width);
				hStats.addValue(box.height);
			}
			double medianWidth = wStats.getPercentile(50);
			double medianHeight = hStats.getPercentile(50);
			hStats.clear();
			for (Rectangle box : priceBoundingBoxes) {
				if ((box.width > (0.35 * medianWidth)) && (box.height > (0.35 * medianHeight))) {
					hStats.addValue(box.height);
				}
			}
			priceHeight = (int) hStats.getPercentile(50);
		}
		OCRPriceDimensionsWrapper priceDimensionsWrapper = new OCRPriceDimensionsWrapper(priceHeight,
				likelyOriginalPriceString == null ? new StringBuffer().append(finalPrice).toString()
						: likelyOriginalPriceString,
				priceBoundingBoxes, "" + (int) thePrice);
		priceDimensionsWrapper.setDebugLevel(debugL);
		priceDimensionsWrapper.process(productPixelHeight, actualProductHeight);

		product.setProductName(finalProduct);
		product.setProductCharacterHeightActual(actualProductHeight);
		product.setProductCharacterHeightPixels(productPixelHeight);
		product.setPriceCharacterHeightActual(priceDimensionsWrapper.likelyActualHeight);
		product.setPriceCharacterHeightPixels(priceDimensionsWrapper.likelyPixelHeight);
		product.setPriceGapBetween1And2(priceDimensionsWrapper.gapBetween1And2);
		product.setMonth(bestMonthMatch == null ? ""
				: (bestMonthMatch.getValue() == null ? "" : bestMonthMatch.getValue().string2));
		product.setYear(year == null ? "" : year);
		product.setDateCharacterHeightActual(dateDimensionsWrapper.likelyActualHeight);
		product.setDateCharacterHeightPixels(dateDimensionsWrapper.likelyPixelHeight);

		if (debugL <= 2) {
			System.out.println("productFound = " + productFound + "; monthFound = " + monthFound + "; yearFound = "
					+ yearFound + "; dateHeightOK = " + dateDimensionsWrapper.heightOK + "; priceHeightOK = "
					+ priceDimensionsWrapper.heightOK + "; priceDistanceOK = " + priceDimensionsWrapper.distanceOK);
		}

		if (productFound && monthFound && yearFound) {
			// output.append("true");
			product.productOK = ProductDescription.ALL_OK;
		} else {
			// output.append("false");
			product.productOK = ProductDescription.ERROR_ONLY_ONE_THING_NOT_FOUND_OR_SOME_OTHER_ISSUE;
			if (!productFound) {
				rejectionReason += System.getProperty(productErrorKey);
			}
			if (priceIndices.size() == 0) {
				rejectionReason += System.getProperty(priceErrorKey);
			}
			if (!monthFound) {
				rejectionReason += System.getProperty(monthErrorKey);
			}
			if (!yearFound) {
				rejectionReason += System.getProperty(yearErrorKey);
			}
		}

		if (!dateDimensionsWrapper.heightOK) {
			if (product.productOK == ProductDescription.ALL_OK) {
				product.productOK = ProductDescription.ERROR_DIMENSION_PROBLEM;
			}
			rejectionReason += dateDimensionsWrapper.reasonForRejection;
		}

		if (!priceDimensionsWrapper.heightOK) {
			if (product.productOK == ProductDescription.ALL_OK) {
				product.productOK = ProductDescription.ERROR_DIMENSION_PROBLEM;
			}
			rejectionReason += priceDimensionsWrapper.reasonForRejection;
		}

		if (!priceDimensionsWrapper.distanceOK) {
			if (product.productOK == ProductDescription.ALL_OK) {
				product.productOK = ProductDescription.ERROR_DIMENSION_PROBLEM;
			}
			rejectionReason += priceDimensionsWrapper.reasonForDistanceRejection;
		}

		product.setRejectionReason(rejectionReason);

		return product;

		/*
		 * output.append(separator).append(finalProduct).append(separator).append(
		 * finalPrice).append(separator) .append(monthAndYear); return
		 * output.toString();
		 */

	}

	public static ProductDescription validateOld2(ArrayList<OCRStringWrapper> ocrResults, int debugL) {

		// StringBuffer output = new StringBuffer();
		ProductDescription product = new ProductDescription();
		String rejectionReason = "";

		ArrayList<Rectangle> monthYearBoundingBoxes = null;
		ArrayList<Rectangle> priceBoundingBoxes = null;
		ArrayList<Rectangle> productBoundingBoxes = null;

		boolean monthFound = false;
		boolean yearFound = false;
		StringBuffer monthAndYear = new StringBuffer();
		// int monthPixelHeight = 0;

		// find the best match for date and year
		ArrayList<Entry<Integer, StringAndPixelHeight>> monthMatches = new ArrayList<>();

		for (OCRStringWrapper wrapper : ocrResults) {
			for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
				String[] lines = sentence.split("\r\n|\r|\n");
				for (String line : lines) {
					Entry<Integer, StringAndPixelHeight> month = processForMonth(line, wrapper.getLikelyPixelHeight(),
							debugL);
					if (month != null) {
						monthMatches.add(month);
						if (wrapper.getBoundingBoxes() != null) {
							// monthYearBoundingBoxes = wrapper.getBoundingBoxes();
							month.getValue().boundingBoxes = wrapper.getBoundingBoxes();
						}
					}
					// if (month != null) {
					// monthAndYear.append(month).append(" ");
					// monthFound = true;
					// break outerloop;
					// }
				}
			}
		}

		Entry<Integer, StringAndPixelHeight> bestMonthMatch = null;
		for (Entry<Integer, StringAndPixelHeight> aMonth : monthMatches) {
			if (bestMonthMatch == null) {
				bestMonthMatch = aMonth;
				monthYearBoundingBoxes = aMonth.getValue().boundingBoxes;
				continue;
			}
			if (aMonth.getKey() >= bestMonthMatch.getKey()) {
				bestMonthMatch = aMonth;
				monthYearBoundingBoxes = aMonth.getValue().boundingBoxes;
			}
		}

		if (bestMonthMatch != null) {
			monthAndYear.append(bestMonthMatch.getValue().string2).append(" ");
			monthFound = bestMonthMatch.getValue().monthFound;
			// if (!monthFound) {
			// rejectionReason += System.getProperty(monthErrorKey);
			// ;
			// }
		}

		if (debugL <= 3) {
			if (bestMonthMatch != null) {
				System.out.println("1. In ProductPriceData.validate(). Month = " + monthAndYear + "; PixelHeight = "
						+ bestMonthMatch.getValue().pixelHeight2 + "; Rectangles = " + monthYearBoundingBoxes);
			}
		}

		String year = null;
		ArrayList<Rectangle> yearBoundingBoxes = null;
		outerloop: for (OCRStringWrapper wrapper : ocrResults) {
			for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
				String[] lines = sentence.split("\r\n|\r|\n");
				HashMap<String, Integer> yearStrings = new HashMap<>();
				for (String line : lines) {
					if (yearStrings.containsKey(line)) {
						int count = yearStrings.get(line);
						yearStrings.put(line, ++count);
					} else {
						yearStrings.put(line, 1);
					}
				}
				List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(
						yearStrings.entrySet());
				Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
					@Override
					public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
						return (o2.getValue()).compareTo(o1.getValue());
					}
				});
				for (Map.Entry<String, Integer> line : list) {
					year = processForYear(line.getKey(), debugL);
					if (year != null) {
						monthAndYear.append(year);
						yearFound = true;
						yearBoundingBoxes = wrapper.getBoundingBoxes();
						break outerloop;
					}
				}
			}
		}

		// find all the matches for price
		ArrayList<IndexAndPixelHeight> priceIndices = new ArrayList<>();
		ArrayList<String[]> priceLines = new ArrayList<>();
		for (OCRStringWrapper wrapper : ocrResults) {
			int pixelHeight = wrapper.getLikelyPixelHeight();
			for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
				String[] lines = sentence.split("\r\n|\r|\n");
				int priceIndicesLength = priceIndices.size();
				for (String line : lines) {
					processForPrice(line, pixelHeight, wrapper.getBoundingBoxes(), priceIndices, debugL);
				}
				if (priceIndices.size() > priceIndicesLength) {
					priceLines.add(lines);
				}
			}
		}

		HashMap<String, Integer> priceStringCount = new HashMap<>();
		if (priceLines.size() > 0) {
			for (String[] priceLineArray : priceLines) {
				for (String aPriceLine : priceLineArray) {
					Set<String> keys = priceStringCount.keySet();
					if (keys.contains(aPriceLine)) {
						int count = priceStringCount.get(aPriceLine);
						priceStringCount.put(aPriceLine, ++count);
					} else {
						priceStringCount.put(aPriceLine, 1);
					}
				}
			}
		}

		if (debugL <= 2) {
			StringBuffer out = new StringBuffer();
			for (IndexAndPixelHeight entry : priceIndices) {
				out.append("Rs ").append(prices.get(entry.index)).append(" ; ");
			}
			System.out.println("Price choices are - " + out.toString());
		}

		if (debugL <= 2) {
			System.out.println("2. In ProductPriceData.validate(). ");
			for (IndexAndPixelHeight entry : priceIndices) {
				System.out.println(entry);
			}
		}

		// find the 3 best matches for product
		// first, find all the relevant strings from the OCR results that matter
		/*
		 * ArrayList<String> productSubStrings = new ArrayList<>(); for
		 * (OCRStringWrapper wrapper : ocrResults) { for (String sentence :
		 * wrapper.getOcrString().split(System.lineSeparator())) { String[] lines =
		 * sentence.split("\r\n|\r|\n"); for (String line : lines) { if (debugL <= 2) {
		 * System.out.print("Rectifying string : " + line); } String lineUpper =
		 * rectifyString(line); if (debugL <= 2) {
		 * System.out.print(". Cleaning product line : " + lineUpper); } lineUpper =
		 * getCleanProductString(lineUpper, debugL); if (debugL <= 2) {
		 * System.out.println(" to get cleaned product line as : " + lineUpper); } //
		 * boolean couldBeProductLine = neitherPriceNorDateLine.equals( //
		 * this.checkIfLineIsDateOrPrice(lineUpper, bestMonthMatch, year, priceIndices,
		 * // debugL)); // boolean couldBeProductLine = checkIfProductLine(lineUpper,
		 * debugL); boolean couldBeProductLine = lineUpper.length() > 0; if
		 * (couldBeProductLine) { // String rectifiedLine = cleanProductLine(lineUpper,
		 * debugL); String rectifiedLine = lineUpper; if (rectifiedLine != null) {
		 * List<ExtractedResult> prodMatches = null; if (rectifiedLine.length() > 10) {
		 * prodMatches = findProductIndexUsingFuzzyWuzzy(rectifiedLine, 89, debugL); for
		 * (ExtractedResult result : prodMatches) { if
		 * (!productSubStrings.contains(rectifiedLine)) {
		 * productSubStrings.add(rectifiedLine); } } } } } } } }
		 */

		ArrayList<String> productSubStrings = new ArrayList<>();
		for (OCRStringWrapper wrapper : ocrResults) {
			for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
				String[] lines = sentence.split("\r\n|\r|\n");
				innerloop: for (String line : lines) {
					if (debugL <= 2) {
						System.out.print("Rectifying string : " + line);
					}
					String lineUpper = rectifyString(line);
					if (debugL <= 2) {
						System.out.print(". Cleaning product line : " + lineUpper);
					}
					lineUpper = getCleanProductString(lineUpper, debugL);
					if (debugL <= 2) {
						System.out.println(" to get cleaned product line as : " + lineUpper);
					}
					boolean couldBeProductLine = lineUpper.length() > 0;
					if (couldBeProductLine) {
						// check if a product substring exists with same content
						// if no, then add this line; else, even if its exists, replace with this line
						// if this line is of longer length
						int similarLineExists = 0;
						for (int i = productSubStrings.size() - 1; i >= 0; --i) {
							int newScore = FuzzySearch.weightedRatio(productSubStrings.get(i), lineUpper);
							if (newScore > similarLineExists) {
								similarLineExists = newScore;
							}
							if ((newScore > 89) && (lineUpper.length() > productSubStrings.get(i).length())) {
								productSubStrings.remove(i);
								productSubStrings.add(i, lineUpper);
								continue innerloop;
							}
						}
						productSubStrings.add(lineUpper);
					}
				}
			}
		}

		int size = productSubStrings.size();
		ArrayList<String> shortListedProducts = new ArrayList<>();

		if (size > 0) {
			StringBuffer allStrings = new StringBuffer(0);
			for (String subString : productSubStrings) {
				allStrings.append(subString).append(" ");
			}
			String finalString = allStrings.toString();
			List<ExtractedResult> prodMatches = null;
			prodMatches = findProductIndexUsingFuzzyWuzzy(finalString, 89, debugL);
			for (ExtractedResult result : prodMatches) {
				shortListedProducts.add(result.getString());
			}
		}

		if (size == 0) {
			// if no strings found, repeat the above sequence with a lesser cut-off
			/*
			 * for (OCRStringWrapper wrapper : ocrResults) { for (String sentence :
			 * wrapper.getOcrString().split(System.lineSeparator())) { String[] lines =
			 * sentence.split("\r\n|\r|\n"); for (String line : lines) { if (debugL <= 2) {
			 * System.out.print("Rectifying string : " + line); } String lineUpper =
			 * rectifyString(line); if (debugL <= 2) {
			 * System.out.print(". Cleaning product line : " + lineUpper); } lineUpper =
			 * getCleanProductString(lineUpper, debugL); if (debugL <= 2) {
			 * System.out.println(" to get cleaned product line as : " + lineUpper); } //
			 * boolean couldBeProductLine = checkIfProductLine(lineUpper, debugL); boolean
			 * couldBeProductLine = lineUpper.length() > 0; if (couldBeProductLine) { //
			 * String rectifiedLine = cleanProductLine(lineUpper, debugL); String
			 * rectifiedLine = lineUpper; if (rectifiedLine != null) { List<ExtractedResult>
			 * prodMatches = null; if (rectifiedLine.length() > 10) { prodMatches =
			 * findProductIndexUsingFuzzyWuzzy(rectifiedLine, 85, debugL); for
			 * (ExtractedResult result : prodMatches) { if
			 * (!productSubStrings.contains(rectifiedLine)) {
			 * productSubStrings.add(rectifiedLine); } } } } } } } }
			 */
			for (OCRStringWrapper wrapper : ocrResults) {
				for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
					String[] lines = sentence.split("\r\n|\r|\n");
					innerloop: for (String line : lines) {
						if (debugL <= 2) {
							System.out.print("Rectifying string : " + line);
						}
						String lineUpper = rectifyString(line);
						if (debugL <= 2) {
							System.out.print(". Cleaning product line : " + lineUpper);
						}
						lineUpper = getCleanProductString(lineUpper, debugL);
						if (debugL <= 2) {
							System.out.println(" to get cleaned product line as : " + lineUpper);
						}
						boolean couldBeProductLine = lineUpper.length() > 0;
						if (couldBeProductLine) {
							// check if a product substring exists with same content
							// if no, then add this line; else, even if its exists, replace with this line
							// if this line is of longer length
							int similarLineExists = 0;
							for (int i = productSubStrings.size() - 1; i >= 0; --i) {
								int newScore = FuzzySearch.weightedRatio(productSubStrings.get(i), lineUpper);
								if (newScore > similarLineExists) {
									similarLineExists = newScore;
								}
								if ((newScore > 85) && (lineUpper.length() > productSubStrings.get(i).length())) {
									productSubStrings.remove(i);
									productSubStrings.add(i, lineUpper);
									continue innerloop;
								}
							}
							productSubStrings.add(lineUpper);
						}
					}
				}
			}
		}

		size = productSubStrings.size();
		shortListedProducts.clear();
		if (size > 0) {
			StringBuffer allStrings = new StringBuffer(0);
			for (String subString : productSubStrings) {
				allStrings.append(subString).append(" ");
			}
			String finalString = allStrings.toString();
			List<ExtractedResult> prodMatches = null;
			prodMatches = findProductIndexUsingFuzzyWuzzy(finalString, 89, debugL);
			for (ExtractedResult result : prodMatches) {
				shortListedProducts.add(result.getString());
			}
		}

		if (size == 0) {
			// if no strings found, repeat the above sequence with an even lesser cut-off
			/*
			 * for (OCRStringWrapper wrapper : ocrResults) { for (String sentence :
			 * wrapper.getOcrString().split(System.lineSeparator())) { String[] lines =
			 * sentence.split("\r\n|\r|\n"); for (String line : lines) { if (debugL <= 2) {
			 * System.out.print("Rectifying string : " + line); } String lineUpper =
			 * rectifyString(line); if (debugL <= 2) {
			 * System.out.print(". Cleaning product line : " + lineUpper); } lineUpper =
			 * getCleanProductString(lineUpper, debugL); if (debugL <= 2) {
			 * System.out.println(" to get cleaned product line as : " + lineUpper); } //
			 * boolean couldBeProductLine = checkIfProductLine(lineUpper, debugL); boolean
			 * couldBeProductLine = lineUpper.length() > 0; if (couldBeProductLine) { //
			 * String rectifiedLine = cleanProductLine(lineUpper, debugL); String
			 * rectifiedLine = lineUpper; if (rectifiedLine != null) { List<ExtractedResult>
			 * prodMatches = null; if (rectifiedLine.length() > 10) { prodMatches =
			 * findProductIndexUsingFuzzyWuzzy(rectifiedLine, 80, debugL); for
			 * (ExtractedResult result : prodMatches) { if
			 * (!productSubStrings.contains(rectifiedLine)) {
			 * productSubStrings.add(rectifiedLine); } } } } } } } // each wrapper finishes
			 * here }
			 */
			for (OCRStringWrapper wrapper : ocrResults) {
				for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
					String[] lines = sentence.split("\r\n|\r|\n");
					innerloop: for (String line : lines) {
						if (debugL <= 2) {
							System.out.print("Rectifying string : " + line);
						}
						String lineUpper = rectifyString(line);
						if (debugL <= 2) {
							System.out.print(". Cleaning product line : " + lineUpper);
						}
						lineUpper = getCleanProductString(lineUpper, debugL);
						if (debugL <= 2) {
							System.out.println(" to get cleaned product line as : " + lineUpper);
						}
						boolean couldBeProductLine = lineUpper.length() > 0;
						if (couldBeProductLine) {
							// check if a product substring exists with same content
							// if no, then add this line; else, even if its exists, replace with this line
							// if this line is of longer length
							int similarLineExists = 0;
							for (int i = productSubStrings.size() - 1; i >= 0; --i) {
								int newScore = FuzzySearch.weightedRatio(productSubStrings.get(i), lineUpper);
								if (newScore > similarLineExists) {
									similarLineExists = newScore;
								}
								if ((newScore > 80) && (lineUpper.length() > productSubStrings.get(i).length())) {
									productSubStrings.remove(i);
									productSubStrings.add(i, lineUpper);
									continue innerloop;
								}
							}
							productSubStrings.add(lineUpper);
						}
					}
				}
			}
		}

		size = productSubStrings.size();
		shortListedProducts.clear();
		if (size > 0) {
			StringBuffer allStrings = new StringBuffer(0);
			for (String subString : productSubStrings) {
				allStrings.append(subString).append(" ");
			}
			String finalString = allStrings.toString();
			List<ExtractedResult> prodMatches = null;
			prodMatches = findProductIndexUsingFuzzyWuzzy(finalString, 89, debugL);
			for (ExtractedResult result : prodMatches) {
				shortListedProducts.add(result.getString());
			}
		}

		int finalPrice = 0;
		int pricePixelHeight = 0;
		String finalProduct = "";
		int actualProductHeight = 0;
		boolean productFound = false;
		int productPixelHeight = 0;
		boolean priceShortlistFound = true;

		if (size == 0) {
			// if no strings found, then no product match exists
			productFound = false;
			if (debugL <= 8) {
				System.out.println("No match found for product");
				for (IndexAndPixelHeight price : priceIndices) {
					System.out.println("Price : Rs. " + prices.get(price.index));
				}
				if (monthFound && yearFound) {
					System.out.println("Month of Manufacture : " + monthAndYear);
				}
			}

			/*
			 * output.append("false").append(separator).append("").append(separator); for
			 * (int index : priceIndices) {
			 * output.append(this.prices.get(index)).append(separator); }
			 * output.append(monthAndYear);
			 *
			 * return output.toString();
			 */
			// product.productOK = ProductDescription.ERROR_PRODUCT_AND_PRICE_NOT_FOUND;
			// product.productName = "";
			// product.monthAndYear = monthAndYear.toString();
			// unique values of price needed; hence, add values to a set, and then populate
			// the arraylist from the set
			// product.price.add(this.prices.get(index).doubleValue());
			priceShortlistFound = false;
			// return product;

		}

		String likelyOriginalPriceString = null;

		if (priceShortlistFound) {
			// pick unique strings from productSubStrings, and populate them in a new array
			// called productStrings
			ArrayList<String> productTempStrings = new ArrayList<>();
			outerloop: for (int i = 0; i < size; ++i) {
				int newSize = productTempStrings.size();
				String ocrString = productSubStrings.get(i);
				for (int j = 0; j < newSize; ++j) {
					// if a product string is there in the array that already matches another that
					// has been chosen, then ignore it. However, if the new string is longer than
					// the current string, then replace the current string with the new string
					if (FuzzySearch.weightedRatio(ocrString, productTempStrings.get(j)) > 70) {
						if (ocrString.length() > productTempStrings.get(j).length()) {
							productTempStrings.remove(j);
							productTempStrings.add(j, ocrString.trim());
						} else {
							continue outerloop;
						}
					}
				}
				productTempStrings.add(ocrString);
			}

			// go through the productStrings array and remove duplicates & close matches
			ArrayList<String> productStrings = new ArrayList<>();
			// First, sort the arraylist by length of the elements
			for (int i = 0; i < productTempStrings.size(); ++i) {
				String productString = productTempStrings.get(i);
				int newSize = productStrings.size();
				int index = 0;
				innerloop: for (int j = 0; j < newSize; ++j) {
					if (productString.length() > productStrings.get(j).length()) {
						index = j;
						break innerloop;
					}
					++index;
				}
				productStrings.add(index, productString);
			}

			if (debugL <= 2) {
				System.out.println("Product Strings = " + productStrings);
			}

			// ...and, then, go through and eliminate strings that match closely (>= 96)

			ArrayList<String> finalProductStrings = new ArrayList<>();
			outerloop: for (int i = 0; i < productStrings.size(); ++i) {
				String productString = productStrings.get(i);
				int newSize = finalProductStrings.size();
				for (int j = 0; j < newSize; ++j) {
					// if a product string is there in the array that already matches another that
					// has been chosen, then ignore it
					if (FuzzySearch.weightedRatio(productString, productStrings.get(j)) > 95) {
						continue outerloop;
					}
				}
				finalProductStrings.add(productString);
			}

			if (debugL <= 2) {
				System.out.println("Final Product Strings = " + finalProductStrings);
			}

			// create the final search string by concatenating unique strings from the array
			// productStrings
			StringBuffer finalSearchString = new StringBuffer();
			for (String string : finalProductStrings) {
				finalSearchString.append(string).append(" ");
			}

			if (debugL <= 7) {
				System.out.println("Final search string = " + finalSearchString.toString());
			}

			List<ExtractedResult> productMatches = FuzzySearch.extractTop(finalSearchString.toString(), products, 30,
					70);
			if (debugL <= 7) {
				System.out.println(productMatches);
			}

			/*
			 * ArrayList<ExtractedResult> productMatches = new ArrayList<>(3); for
			 * (OCRResult ocrResult : ocrResults) { for (String sentence :
			 * ocrResult.sentences) { String[] lines =
			 * sentence.lines().toArray(String[]::new); for (String line : lines) {
			 * List<ExtractedResult> prodMatches =
			 * this.findProductIndexUsingFuzzyWuzzy(line); for (ExtractedResult result :
			 * prodMatches) { if (!productSubStrings.contains(line)) {
			 * productSubStrings.add(line); } int size = productMatches.size(); if (size <
			 * 3) { productMatches.add(result); } else { if (result.getScore() >
			 * productMatches.get(0).getScore()) { productMatches.add(0, result);
			 * productMatches.remove(3); } else { if (result.getScore() >
			 * productMatches.get(1).getScore()) { productMatches.add(1, result);
			 * productMatches.remove(3); } else { if (result.getScore() >
			 * productMatches.get(2).getScore()) { productMatches.add(2, result);
			 * productMatches.remove(3); } } } } } } } System.out.println(productMatches); }
			 */

			// find which product and price combination is valid
			outerloop: for (ExtractedResult result : productMatches) {
				int productIndex = result.getIndex();
				for (IndexAndPixelHeight priceIndex : priceIndices) {
					if ((priceIndex.boundingBoxes != null) && (priceIndex.boundingBoxes.size() > 1)) {
						if (priceIndex.index == productIndex) {
							finalPrice = prices.get(priceIndex.index);
							finalProduct = originalProductStrings.get(productIndex);
							actualProductHeight = originalProductHeights.get(productIndex);
							priceBoundingBoxes = priceIndex.boundingBoxes;
							pricePixelHeight = priceIndex.pixelHeight1;
							productFound = true;
							break outerloop;
						}
					}
				}
			}

			if (debugL <= 8) {
				System.out.println("Product : " + finalProduct);
				System.out.println("Price : Rs. " + finalPrice);
				if (monthFound && yearFound) {
					System.out.println("Month of Manufacture : " + monthAndYear);
				}
			}

			if (productFound) {
				// determine now which OCRBufferedImageWrapper has a product name and get its
				// pixelHeight
				if (debugL <= 2) {
					System.out.println("1. In ProductPriceData.validate(). Comparing finalProduct = " + finalProduct
							+ " with the strings");
				}
				HashMap<OCRStringWrapper, Integer> bestProductFit = new HashMap<>();
				for (OCRStringWrapper wrapper : ocrResults) {
					for (String sentence : wrapper.getOcrString().split(System.lineSeparator())) {
						String[] lines = sentence.split("\r\n|\r|\n");
						for (String line : lines) {
							if (line.length() > 15) {
								int weightedRatio = FuzzySearch.weightedRatio(finalProduct, line);
								if (debugL <= 2) {
									System.out.println("1. In ProductPriceData.validate(). Compared with line = " + line
											+ " to get FuzzySearch.weightedRatio() value of " + weightedRatio);
								}
								if (weightedRatio >= 75) {
									if (bestProductFit.get(wrapper) == null) {
										bestProductFit.put(wrapper, weightedRatio);
									} else {
										int currentWeight = bestProductFit.get(wrapper);
										if (weightedRatio > currentWeight) {
											bestProductFit.put(wrapper, weightedRatio);
										}
									}
									// OCRStringWrapper found
									// productPixelHeight = wrapper.likelyPixelHeight;
									// productBoundingBoxes = wrapper.getBoundingBoxes();
									// break outerloop;
								}
							}
						}
					}
				}

				// Sort bestProductFit by weightedRatio

				List<Map.Entry<OCRStringWrapper, Integer>> productMatchList = new LinkedList<Map.Entry<OCRStringWrapper, Integer>>(
						bestProductFit.entrySet());
				// Sort the list
				Collections.sort(productMatchList, new Comparator<Map.Entry<OCRStringWrapper, Integer>>() {

					@Override
					public int compare(Map.Entry<OCRStringWrapper, Integer> o1,
							Map.Entry<OCRStringWrapper, Integer> o2) {
						return (o2.getValue()).compareTo(o1.getValue());
					}
				});

				OCRStringWrapper topProductFit = productMatchList.get(0).getKey();

				productPixelHeight = topProductFit.likelyPixelHeight;
				productBoundingBoxes = topProductFit.getBoundingBoxes();

				// Finding the likelyOriginalPriceString if product is found
				// First, remove those entries which do not match the finalPrice
				if (debugL <= 2) {
					System.out.println("priceStringCount before removing irrelevant keys = " + priceStringCount);
				}
				Set<String> keys = priceStringCount.keySet();
				List<String> keyList = keys.stream().collect(Collectors.toCollection(ArrayList::new));
				for (String key : keyList) {
					// String strippedDownKey = key.replace(" ", "").replace(".", "").replace("_",
					// "").replace("-", "")
					// .replace("O", "0").replace(",", "").replace(":", "").trim();
					String strippedDownKey = key.replace(" ", "").replace("_", ".").replace("-", ".").replace("O", "0")
							.replace(",", ".").replace("+", ".").replace(":", ".").replace("/", "").replace("=", ".")
							.replace("|", "").replace("\\", "").trim();

					int index = strippedDownKey.indexOf(new StringBuffer().append(finalPrice).toString());
					if (index == -1) {
						priceStringCount.remove(key);
					}
				}
				if (debugL <= 2) {
					System.out.println("priceStringCount after removing irrelevant keys = " + priceStringCount);
				}
				// From the remaining strings in priceStringCount, sort the HashMap, pick the
				// one with most occurrences, clean it and assign its value to
				// likelyOriginalPriceString

				HashMap<String, Integer> priceStringCountCleaned = new HashMap<>();

				for (Map.Entry<String, Integer> entry : priceStringCount.entrySet()) {
					String originalString = entry.getKey();
					// String newString = originalString.replace(" ", "").replace(".",
					// "").replace("_", "")
					// .replace("-", "").replace("O", "0").replace(",", "").replace("+",
					// "").replace(":", "")
					// .replace("/", "").replace("=", "").replace("|", "").replace("\\", "").trim();
					String newString = originalString.replace(" ", "").replace("_", ".").replace("-", ".")
							.replace("O", "0").replace(",", ".").replace("+", ".").replace(":", ".").replace("/", "")
							.replace("=", ".").replace("|", "").replace("\\", "").trim();
					if (priceStringCountCleaned.containsKey(newString)) {
						int currentCount = priceStringCountCleaned.get(newString);
						priceStringCountCleaned.put(newString, currentCount + 1);
					} else {
						priceStringCountCleaned.put(newString, entry.getValue());
					}
				}

				List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(
						priceStringCountCleaned.entrySet());
				// Sort the list
				Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
					@Override
					public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
						return (o2.getValue()).compareTo(o1.getValue());
					}
				});
				if (debugL <= 2) {
					System.out.println("priceStrings after cleaning and sorting = " + list);
				}
				if (list.size() > 0) {
					likelyOriginalPriceString = list.get(0).getKey();
				} else {
					likelyOriginalPriceString = "";
				}

				/*
				 * // put data from sorted list to hashmap HashMap<String, Integer> temp = new
				 * LinkedHashMap<String, Integer>(); for (Map.Entry<String, Integer> aa : list)
				 * { temp.put(aa.getKey(), aa.getValue()); }
				 */

				if (debugL <= 2) {
					System.out.println("1. In ProductPriceData.validate(). productFound = " + productFound
							+ "; Product PixelHeight = " + productPixelHeight + "; productActualHeight = "
							+ actualProductHeight + "; productBoundingBoxes = " + productBoundingBoxes);
				}

			} else {

				// Finding the likelyOriginalPriceString if product is NOT found
				// First, remove those entries which do not match the choices in the
				// priceIndices table
				Set<String> keys = priceStringCount.keySet();
				if (debugL <= 2) {
					System.out.println("1. In ProductPriceData.validate(). priceStringCount = " + priceStringCount);
				}
				List<String> keyList = keys.stream().collect(Collectors.toCollection(ArrayList::new));
				for (String key : keyList) {
					// String strippedDownKey = key.replace(" ", "").replace(".", "").replace("_",
					// "").replace("-", "")
					// .replace("O", "0").replace(",", "").replace("+", "").replace(":", "").trim();
					String strippedDownKey = key.replace(" ", "").replace("_", ".").replace("-", ".").replace("O", "0")
							.replace(",", ".").replace("+", ".").replace(":", ".").replace("/", "").replace("=", ".")
							.replace("|", "").replace("\\", "").trim();

					boolean pFound = false;
					innerloop: for (IndexAndPixelHeight pIndex : priceIndices) {
						int pr = prices.get(pIndex.index);
						int index = strippedDownKey.indexOf(new StringBuffer().append(pr).toString());
						if (index != -1) {
							pFound = true;
							break innerloop;
						}
					}
					if (!pFound) {
						priceStringCount.remove(key);
					}
				}
				// From the rest, sort the HashMap, pick the one with most occurrences, clean it
				// and assign its value to likelyOriginalPriceString

				List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(
						priceStringCount.entrySet());
				// Sort the list
				Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
					@Override
					public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
						return (o2.getValue()).compareTo(o1.getValue());
					}
				});

				if (list.size() > 0) {
					likelyOriginalPriceString = list.get(0).getKey();
				} else {
					likelyOriginalPriceString = "";
				}
			}

		}

		// likelyOriginalPriceString = likelyOriginalPriceString.replace(" ",
		// "").replace(".", "").replace("-", "")
		// .replace("_", "").replace(",", "").trim();
		// if (likelyOriginalPriceString != null) {

		if (likelyOriginalPriceString == null) {
			// Finding the likelyOriginalPriceString if likelyOriginalPriceString is NOT
			// populated
			// First, remove those entries which do not match the choices in the
			// priceIndices table
			Set<String> keys = priceStringCount.keySet();
			if (debugL <= 2) {
				System.out.println("1. In ProductPriceData.validate(). priceStringCount = " + priceStringCount);
			}
			List<String> keyList = keys.stream().collect(Collectors.toCollection(ArrayList::new));
			for (String key : keyList) {
				// String strippedDownKey = key.replace(" ", "").replace(".", "").replace("_",
				// "").replace("-", "")
				// .replace("O", "0").replace(",", "").replace("+", "").replace(":",
				// "").replace("=", "")
				// .replace("/", "").replace("|", "").trim();
				String strippedDownKey = key.replace(" ", "").replace("_", ".").replace("-", ".").replace("O", "0")
						.replace(",", ".").replace("+", ".").replace(":", ".").replace("/", "").replace("=", ".")
						.replace("|", "").replace("\\", "").trim();

				boolean pFound = false;
				innerloop: for (IndexAndPixelHeight pIndex : priceIndices) {
					int pr = prices.get(pIndex.index);
					int index = strippedDownKey.indexOf(new StringBuffer().append(pr).toString());
					if (index != -1) {
						pFound = true;
						break innerloop;
					}
				}
				if (!pFound) {
					priceStringCount.remove(key);
				}
			}
			// From the rest, sort the HashMap, pick the one with most occurrences, clean it
			// and assign its value to likelyOriginalPriceString

			List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(
					priceStringCount.entrySet());
			// Sort the list
			Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {

				@Override
				public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
					return (o2.getValue()).compareTo(o1.getValue());
				}
			});

			if ((list != null) && (list.size() > 0)) {
				likelyOriginalPriceString = list.get(0).getKey();
			} else {
				likelyOriginalPriceString = "";
			}
		}

		likelyOriginalPriceString = likelyOriginalPriceString.replace(" ", "").trim();
		// } else {
		// likelyOriginalPriceString = "";
		// }

		if (priceShortlistFound && productFound) {
			product.price.add(Double.valueOf(finalPrice * 1.0));
		} else {

			Set<Double> shortlistedPrices = new TreeSet<>();
			for (IndexAndPixelHeight priceIndex : priceIndices) {
				shortlistedPrices.add(prices.get(priceIndex.index).doubleValue());
			}
			for (Double value : shortlistedPrices) {
				product.price.add(value);
			}
		}

		if (debugL <= 2) {
			System.out.println("Attempting to find \"thePrice\"");
			System.out.println("ArrayList product.price is " + product.price);
		}

		double thePrice = 0.0;
		if (product.price.size() <= 1) {
			thePrice = (product.price.size() == 1) ? (double) product.price.get(0) : 0;
		} else {
			ArrayList<Double> prices = new ArrayList<>();
			ArrayList<Integer> priceCounter = new ArrayList<>();
			for (Double aPrice : product.price) {
				if (prices.contains(aPrice)) {
					int index = prices.indexOf(aPrice);
					int counter = priceCounter.get(index).intValue();
					priceCounter.set(index, counter);
				} else {
					prices.add(aPrice);
					priceCounter.add(1);
				}
			}
			if (debugL <= 2) {
				System.out.println("ArrayList prices is " + prices);
				System.out.println("ArrayList priceCounter is " + priceCounter);
			}
			int indexOfMax = 0;
			int maxCount = 0;
			for (int i = 0; i < priceCounter.size(); ++i) {
				if (priceCounter.get(i) >= maxCount) {
					indexOfMax = i;
				}
			}
			thePrice = prices.get(indexOfMax);
		}

		// Note that the productBoundingBoxes are not accurate as the product name is
		// usually split into multiple lines...this implementation only gets one of the
		// lines...it needs to be enhanced to get the bounding boxes of all the lines,
		// if that is ever required. Note though that the productName is the full valid
		// name of the product
		OCRProductDimensionsWrapper prodDimensionsWrapper = new OCRProductDimensionsWrapper(productPixelHeight,
				finalProduct, productBoundingBoxes);
		prodDimensionsWrapper.setDebugLevel(debugL);
		prodDimensionsWrapper.process(productPixelHeight, actualProductHeight);

		ArrayList<Rectangle> boundingBoxes = monthFound ? monthYearBoundingBoxes
				: (yearFound ? yearBoundingBoxes : null);
		int monthYearHeight = 0;
		if (boundingBoxes != null) {
			DescriptiveStatistics hStats = new DescriptiveStatistics();
			DescriptiveStatistics wStats = new DescriptiveStatistics();
			for (Rectangle box : boundingBoxes) {
				wStats.addValue(box.width);
			}
			double medianWidth = wStats.getPercentile(50);
			for (Rectangle box : boundingBoxes) {
				if (box.width > (0.35 * medianWidth)) {
					hStats.addValue(box.height);
				}
			}
			monthYearHeight = (int) hStats.getPercentile(50);
		}
		OCRDateDimensionsWrapper dateDimensionsWrapper = new OCRDateDimensionsWrapper(monthYearHeight,
				monthAndYear.toString(), boundingBoxes);
		dateDimensionsWrapper.setDebugLevel(debugL);
		dateDimensionsWrapper.process(productPixelHeight, actualProductHeight);

		int priceHeight = pricePixelHeight;
		if (priceBoundingBoxes != null) {
			DescriptiveStatistics hStats = new DescriptiveStatistics();
			DescriptiveStatistics wStats = new DescriptiveStatistics();
			for (Rectangle box : priceBoundingBoxes) {
				wStats.addValue(box.width);
				hStats.addValue(box.height);
			}
			double medianWidth = wStats.getPercentile(50);
			double medianHeight = hStats.getPercentile(50);
			hStats.clear();
			for (Rectangle box : priceBoundingBoxes) {
				if ((box.width > (0.35 * medianWidth)) && (box.height > (0.35 * medianHeight))) {
					hStats.addValue(box.height);
				}
			}
			priceHeight = (int) hStats.getPercentile(50);
		}
		OCRPriceDimensionsWrapper priceDimensionsWrapper = new OCRPriceDimensionsWrapper(priceHeight,
				likelyOriginalPriceString == null ? new StringBuffer().append(finalPrice).toString()
						: likelyOriginalPriceString,
				priceBoundingBoxes, "" + (int) thePrice);
		priceDimensionsWrapper.setDebugLevel(debugL);
		priceDimensionsWrapper.process(productPixelHeight, actualProductHeight);

		product.setProductName(finalProduct);
		product.setProductCharacterHeightActual(actualProductHeight);
		product.setProductCharacterHeightPixels(productPixelHeight);
		product.setPriceCharacterHeightActual(priceDimensionsWrapper.likelyActualHeight);
		product.setPriceCharacterHeightPixels(priceDimensionsWrapper.likelyPixelHeight);
		product.setPriceGapBetween1And2(priceDimensionsWrapper.gapBetween1And2);
		product.setMonth(bestMonthMatch == null ? ""
				: (bestMonthMatch.getValue() == null ? "" : bestMonthMatch.getValue().string2));
		product.setYear(year == null ? "" : year);
		product.setDateCharacterHeightActual(dateDimensionsWrapper.likelyActualHeight);
		product.setDateCharacterHeightPixels(dateDimensionsWrapper.likelyPixelHeight);

		if (debugL <= 2) {
			System.out.println("productFound = " + productFound + "; monthFound = " + monthFound + "; yearFound = "
					+ yearFound + "; dateHeightOK = " + dateDimensionsWrapper.heightOK + "; priceHeightOK = "
					+ priceDimensionsWrapper.heightOK + "; priceDistanceOK = " + priceDimensionsWrapper.distanceOK);
		}

		if (productFound && monthFound && yearFound) {
			// output.append("true");
			product.productOK = ProductDescription.ALL_OK;
		} else {
			// output.append("false");
			product.productOK = ProductDescription.ERROR_ONLY_ONE_THING_NOT_FOUND_OR_SOME_OTHER_ISSUE;
			if (!productFound) {
				rejectionReason += System.getProperty(productErrorKey);
			}
			if (priceIndices.size() == 0) {
				rejectionReason += System.getProperty(priceErrorKey);
			}
			if (!monthFound) {
				rejectionReason += System.getProperty(monthErrorKey);
			}
			if (!yearFound) {
				rejectionReason += System.getProperty(yearErrorKey);
			}
		}

		if (!dateDimensionsWrapper.heightOK) {
			if (product.productOK == ProductDescription.ALL_OK) {
				product.productOK = ProductDescription.ERROR_DIMENSION_PROBLEM;
			}
			rejectionReason += dateDimensionsWrapper.reasonForRejection;
		}

		if (!priceDimensionsWrapper.heightOK) {
			if (product.productOK == ProductDescription.ALL_OK) {
				product.productOK = ProductDescription.ERROR_DIMENSION_PROBLEM;
			}
			rejectionReason += priceDimensionsWrapper.reasonForRejection;
		}

		if (!priceDimensionsWrapper.distanceOK) {
			if (product.productOK == ProductDescription.ALL_OK) {
				product.productOK = ProductDescription.ERROR_DIMENSION_PROBLEM;
			}
			rejectionReason += priceDimensionsWrapper.reasonForDistanceRejection;
		}

		product.setRejectionReason(rejectionReason);

		return product;

		/*
		 * output.append(separator).append(finalProduct).append(separator).append(
		 * finalPrice).append(separator) .append(monthAndYear); return
		 * output.toString();
		 */

	}

	private static int noOfWords(String input) {
		if (input == null) {
			return 0;
		}
		if ("".equals(input)) {
			return 0;
		}
		String[] words = input.split("//s+");
		return words.length;
	}

	public static boolean doesPriceHaveADot(String priceLine) {
		String newLine = priceLine.toUpperCase().replace(" ", "").replace("-", ".").replace(",", ".").replace("_", ".")
				.replace("\"", ".").replace("'", ".").replace("=", ".").replace("*", ".").replace("O", "0").trim();
		Matcher m = null;
		synchronized (lock) {
			m = dotZeroPattern.matcher(newLine);
		}
		return m.find();
	}

	public static String rationalisePriceSearchString(String input) {
		String newKey = input.replace("Z", "2").replace("O", "0").replace("z", "2").replace("o", "0").replace("l", "1")
				.replace("(", " ").replace(")", " ").replace("[", " ").replace("]", " ").replace("'", " ")
				.replace("\"", " ").replace("-", " ").replace("_", " ").replace(",", " ").replace("{", " ")
				.replace("}", " ").replace(" ", "").replace("PRICE", "").replace("RETAIL", "").trim().toUpperCase();

		// Note: Cannot do a blanket replacement of S and s with 5 as Rs1120 gets
		// changed to R51120, !!

		if (CheckProductProperties.productIsGiven) {
			if (CheckProductProperties.givenProductPrice != 0) {
				String productPrice = "" + CheckProductProperties.givenProductPrice;
				if (productPrice.indexOf("2") != -1) {
					newKey = newKey.replace("Z", "2");
				}
				if ((productPrice.indexOf("7") != -1) && (productPrice.indexOf("1") != -1)) {
				} else {
					if (productPrice.indexOf("7") == -1) {
						newKey = newKey.replace("7", "1");
					}
					if (productPrice.indexOf("1") == -1) {
						newKey = newKey.replace("1", "7");
					}
				}
				if ((productPrice.indexOf("5") != -1) && (productPrice.indexOf("6") == -1)) {
					newKey = newKey.replace("6", "5");
				}
				if ((productPrice.indexOf("9") != -1) && (productPrice.indexOf("8") == -1)) {
					newKey = newKey.replace("8", "9");
				}
			}
		}

		Matcher priceMatcher = null;
		synchronized (lock) {
			priceMatcher = pricePattern.matcher(newKey);
		}
		if (priceMatcher.find()) {
			int start = priceMatcher.start();
			int end = priceMatcher.end();
			String tobeReplaced = newKey.substring(start, end).trim();
			newKey = newKey.replace(tobeReplaced, "");
		}

		Matcher retailMatcher = null;
		synchronized (lock) {
			retailMatcher = retailPattern.matcher(newKey);
		}
		if (retailMatcher.find()) {
			int start = retailMatcher.start();
			int end = retailMatcher.end();
			String tobeReplaced = newKey.substring(start, end).trim();
			newKey = newKey.replace(tobeReplaced, "");
		}

		Matcher taxesMatcher = null;
		synchronized (lock) {
			taxesMatcher = taxesPattern.matcher(newKey);
		}
		if (taxesMatcher.find()) {
			int start = taxesMatcher.start();
			int end = taxesMatcher.end();
			String tobeReplaced = newKey.substring(start, end).trim();
			newKey = newKey.replace(tobeReplaced, "");
		}

		Matcher ofAllMatcher = null;
		synchronized (lock) {
			ofAllMatcher = ofAllPattern.matcher(newKey);
		}
		if (ofAllMatcher.find()) {
			int start = ofAllMatcher.start();
			int end = ofAllMatcher.end();
			String tobeReplaced = newKey.substring(start, end).trim();
			newKey = newKey.replace(tobeReplaced, "");
		}

		Matcher rsMatcher = null;
		synchronized (lock) {
			rsMatcher = rsPattern.matcher(newKey);
		}
		if (rsMatcher.find()) {
			int start = rsMatcher.start();
			int end = rsMatcher.end();
			String tobeReplaced = newKey.substring(start, end).trim();
			// remove Rs, and change "S" to "5" in the string
			newKey = newKey.replace(tobeReplaced, "").replace("S", "5");
			// Add back RS, by first checking for a 4-digit sequence
			Pattern sequence = Pattern.compile("\\d{4,5}");
			Matcher aMatcher = sequence.matcher(newKey);
			if (aMatcher.find()) {
				start = aMatcher.start();
				newKey = newKey.substring(0, start) + "RS" + newKey.substring(start);
			}
		}

		for (Pattern aPattern : ttkPrestigeLtd) {
			Matcher aMatcher = null;
			synchronized (lock) {
				aMatcher = aPattern.matcher(newKey);
			}
			if (aMatcher.find()) {
				int start = aMatcher.start();
				int end = aMatcher.end();
				String tobeReplaced = newKey.substring(start, end).trim();
				newKey = newKey.replace(tobeReplaced, "");
			}
		}
		return newKey;
	}
}