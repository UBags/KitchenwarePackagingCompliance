package com.techwerx.text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.techwerx.image.utils.SysOutController;

public class Resources {

	private static char dotValue = Character.valueOf('.');

	/*
	 * private static final String DEFAULT_INPUT_DIRECTORY = "Input"; private static
	 * final String DEFAULT_OUTPUT_DIRECTORY = "Output"; private static final String
	 * DEFAULT_OCR_DIRECTORY = "OCR_Results"; private static final String
	 * DEFAULT_OCR_FILE = "OCR_Output.txt"; private static final String
	 * DEFAULT_RESULTS_DIRECTORY = "Results"; private static final String
	 * DEFAULT_ERROR_DIRECTORY = "Error";
	 */

	private static String date = new SimpleDateFormat("ddMMyyyy").format(new Date());

	private static final String INPUT_DIRECTORY_KEY = "input.folder";
	private static final String OUTPUT_DIRECTORY_KEY = "output.folder";
	private static final String OCR_DIRECTORY_KEY = "rollingresult.folder";
	private static final String OCR_DEFAULT_ROLLINGRESULTS_DIRECTORY = "/OCR_Results";
	private static final String OCR_FILE_KEY = "rollingresult.file";
	private static final String OCR_DEFAULT_ROLLINGRESULTS_FILE = "OCR_Results";
	private static final String RESULTS_DIRECTORY_KEY = "results.folder";
	private static final String RESULTS_DEFAULT_DIRECTORY = "/Results";
	private static final String ERROR_DIRECTORY_KEY = "error.folder";
	private static final String ERROR_DEFAULT_DIRECTORY = "/Error";
	private static final String DEBUGGINGIMAGES_DIRECTORY_KEY = "debuggingimages.folder";
	private static final String DEBUGGINGIMAGES_DEFAULT_DIRECTORY = "/debuggingImages";

	private static final String DEFAULT_INPUT_DIRECTORY = System.getProperty(INPUT_DIRECTORY_KEY);
	private static final String DEFAULT_OUTPUT_DIRECTORY = System.getProperty(OUTPUT_DIRECTORY_KEY);
	private static final String DEFAULT_OCR_DIRECTORY = System.getProperty(OCR_DIRECTORY_KEY,
			DEFAULT_OUTPUT_DIRECTORY + OCR_DEFAULT_ROLLINGRESULTS_DIRECTORY);
	private static final String DEFAULT_OCR_FILE = System.getProperty(OCR_FILE_KEY, OCR_DEFAULT_ROLLINGRESULTS_FILE);
	private static final String DEFAULT_RESULTS_DIRECTORY = System.getProperty(RESULTS_DIRECTORY_KEY,
			DEFAULT_OUTPUT_DIRECTORY + RESULTS_DEFAULT_DIRECTORY);
	private static final String DEFAULT_ERROR_DIRECTORY = System.getProperty(ERROR_DIRECTORY_KEY,
			DEFAULT_OUTPUT_DIRECTORY + ERROR_DEFAULT_DIRECTORY);
	private static final String DEBUGGINGIMAGES_DIRECTORY = System.getProperty(DEBUGGINGIMAGES_DIRECTORY_KEY,
			DEFAULT_OUTPUT_DIRECTORY + DEBUGGINGIMAGES_DEFAULT_DIRECTORY);

	// public Path workingDirectoryPath = null;
	public Path inputDirectoryPath = null;
	public Path outputDirectoryPath = null;
	public Path ocrDirectoryPath = null;
	public Path ocrFilePath = null;
	public Path ocrResultsPath = null;
	public Path ocrErrorPath = null;
	public PrintWriter writer = null;

	public boolean writerIsOpen = false;
	private static File logFile = null;

	public Resources(Path inputDirectoryPath, Path outputDirectoryPath, Path ocrDirectoryPath, Path ocrFilePath,
			Path ocrResultsPath, Path ocrErrorPath, PrintWriter writer) {
		this.inputDirectoryPath = inputDirectoryPath;
		this.outputDirectoryPath = outputDirectoryPath;
		this.ocrDirectoryPath = ocrDirectoryPath;
		this.ocrFilePath = ocrFilePath;
		this.ocrResultsPath = ocrResultsPath;
		this.ocrErrorPath = ocrResultsPath;
		this.writer = writer;
		// System.out.println(this);
	}

	/*
	 * public Resources(String inputDirectory, String outputDirectory, String
	 * ocrDirectory, String ocrFile, String ocrResultsDirectory, String
	 * ocrErrorDirectory) {
	 *
	 * Path path0 = Paths.get(inputDirectory); //
	 * System.out.println("Input Directory is given as = " + path0); try {
	 * this.inputDirectoryPath = Files.createDirectories(path0); } catch
	 * (FileAlreadyExistsException e) { // the directory already exists.
	 * this.inputDirectoryPath = path0; } catch (IOException e) { // something else
	 * went wrong e.printStackTrace(); } //
	 * System.out.println("Input Directory Path = " + //
	 * this.inputDirectoryPath.toString()); Path path1 = null; int
	 * indexOfFileSeparator = inputDirectory.lastIndexOf(File.separator); String
	 * baseDirectory = inputDirectory.substring(0, indexOfFileSeparator); try { if
	 * (outputDirectory.indexOf(File.separator) == -1) { path1 =
	 * Paths.get(baseDirectory + File.separator + outputDirectory); } else { path1 =
	 * Paths.get(outputDirectory); } this.outputDirectoryPath =
	 * Files.createDirectories(path1); //
	 * System.out.println("Created output directory - " + //
	 * this.outputDirectoryPath.toString()); } catch (FileAlreadyExistsException e)
	 * { // the directory already exists. this.outputDirectoryPath = path1; } catch
	 * (IOException e) { // something else went wrong e.printStackTrace(); } //
	 * System.out.println("Output Directory Path = " + //
	 * this.outputDirectoryPath.toString()); Path path2 = null; try { if
	 * (ocrDirectory.indexOf(File.separator) == -1) { path2 =
	 * Paths.get(baseDirectory + File.separator + outputDirectory + File.separator +
	 * ocrDirectory); } else { path2 = Paths.get(ocrDirectory); }
	 * this.ocrDirectoryPath = Files.createDirectories(path2); //
	 * System.out.println("Created ocr directory inside output directory - " + //
	 * this.ocrDirectoryPath.toString()); } catch (FileAlreadyExistsException e) {
	 * // the directory already exists. this.ocrDirectoryPath = path2; } catch
	 * (IOException e) { // something else went wrong e.printStackTrace(); } //
	 * System.out.println("OCR Directory Path = " + //
	 * this.ocrDirectoryPath.toString()); Path path3 = null; try { path3 =
	 * Paths.get(baseDirectory + File.separator + outputDirectory + File.separator +
	 * ocrDirectory + File.separator + ocrFile + date + ".txt"); this.ocrFilePath =
	 * Files.createFile(path3); this.initialiseWriter(); //
	 * System.out.println("Created ocr file in ocr directory inside output //
	 * directory"); } catch (FileAlreadyExistsException e) { // the file already
	 * exists. this.ocrFilePath = path3; this.initialiseWriter(); } catch
	 * (IOException e) { // something else went wrong e.printStackTrace(); } //
	 * System.out.println("Output file path = " + this.ocrFilePath.toString()); //
	 * System.out.println(this); Path path4 = null; try {
	 *
	 * if (ocrDirectory.indexOf(File.separator) == -1) { path4 = Paths
	 * .get(baseDirectory + File.separator + outputDirectory + File.separator +
	 * ocrResultsDirectory); } else { path4 = Paths.get(ocrResultsDirectory); }
	 * this.ocrResultsPath = Files.createDirectories(path4); //
	 * System.out.println("Created ocr directory inside output directory - " + //
	 * this.ocrResultsPath.toString()); } catch (FileAlreadyExistsException e) { //
	 * the directory already exists. this.ocrResultsPath = path4; } catch
	 * (IOException e) { // something else went wrong e.printStackTrace(); }
	 *
	 * Path path5 = null; try { if (ocrDirectory.indexOf(File.separator) == -1) {
	 * path5 = Paths .get(baseDirectory + File.separator + outputDirectory +
	 * File.separator + ocrErrorDirectory); } else { path5 =
	 * Paths.get(ocrErrorDirectory); } this.ocrErrorPath =
	 * Files.createDirectories(path5); //
	 * System.out.println("Created ocr directory inside output directory - " + //
	 * this.ocrResultsPath.toString()); } catch (FileAlreadyExistsException e) { //
	 * the directory already exists. this.ocrResultsPath = path5; } catch
	 * (IOException e) { // something else went wrong e.printStackTrace(); } }
	 */

	public Resources(String inputDirectory, String outputDirectory, String ocrDirectory, String ocrFile,
			String ocrResultsDirectory, String ocrErrorDirectory) {

		Path path0 = Paths.get(inputDirectory);
		// System.out.println("Input Directory is given as = " + path0);
		try {
			this.inputDirectoryPath = Files.createDirectories(path0);
		} catch (FileAlreadyExistsException e) {
			// the directory already exists.
			this.inputDirectoryPath = path0;
		} catch (IOException e) {
			// something else went wrong
			e.printStackTrace();
		}
		// System.out.println("Input Directory Path = " +
		// this.inputDirectoryPath.toString());
		Path path1 = Paths.get(outputDirectory);
		try {
			this.outputDirectoryPath = Files.createDirectories(path1);
		} catch (FileAlreadyExistsException e) {
			// the directory already exists.
			this.outputDirectoryPath = path1;
		} catch (IOException e) {
			// something else went wrong
			e.printStackTrace();
		}
		// System.out.println("Output Directory Path = " +
		// this.outputDirectoryPath.toString());
		Path path2 = Paths.get(ocrDirectory);
		try {
			this.ocrDirectoryPath = Files.createDirectories(path2);
		} catch (FileAlreadyExistsException e) {
			// the directory already exists.
			this.ocrDirectoryPath = path2;
		} catch (IOException e) {
			// something else went wrong
			e.printStackTrace();
		}
		// System.out.println("OCR Directory Path = " +
		// this.ocrDirectoryPath.toString());
		Path path3 = Paths.get(ocrDirectory + File.separator + ocrFile + date + ".txt");
		try {
			this.ocrFilePath = Files.createFile(path3);
			this.initialiseWriter();
			// System.out.println("Created ocr file in ocr directory inside output
			// directory");
		} catch (FileAlreadyExistsException e) {
			// the file already exists.
			this.ocrFilePath = path3;
			this.initialiseWriter();
			this.println(""); // if the file exists, then print an empty line to move the cursor to beginning
								// of new line
		} catch (IOException e) {
			// something else went wrong
			e.printStackTrace();
		}
		// System.out.println("Output file path = " + this.ocrFilePath.toString());
		// System.out.println(this);
		Path path4 = Paths.get(ocrResultsDirectory);
		try {
			this.ocrResultsPath = Files.createDirectories(path4);
		} catch (FileAlreadyExistsException e) {
			// the directory already exists.
			this.ocrResultsPath = path4;
		} catch (IOException e) {
			// something else went wrong
			e.printStackTrace();
		}

		Path path5 = Paths.get(ocrErrorDirectory);
		try {
			this.ocrErrorPath = Files.createDirectories(path5);
		} catch (FileAlreadyExistsException e) {
			// the directory already exists.
			this.ocrErrorPath = path5;
		} catch (IOException e) {
			// something else went wrong
			e.printStackTrace();
		}

		// create the debugging directory, but no need to keep a reference to its path
		String DEBUGLEVEL_KEY = "dl";
		String debugL = System.getProperty(DEBUGLEVEL_KEY);
		int debugLevel = 99;
		if ((debugL == null) || "".equals(debugL)) {
			debugLevel = 99;
		} else {
			debugLevel = Integer.parseInt(System.getProperty(DEBUGLEVEL_KEY));
		}

		if (debugLevel < 99) {
			// create debuggingImages directory
			// also, create a loggingFile in the error directory
			Path path6 = Paths.get(DEBUGGINGIMAGES_DIRECTORY);
			try {
				Files.createDirectories(path6);
				System.setProperty(DEBUGGINGIMAGES_DIRECTORY_KEY, path6.toAbsolutePath().toString());
				logFile = new File(path5.toFile(), "logFile.log");
				SysOutController.redirectSysOut(logFile);
			} catch (FileAlreadyExistsException e) {
				// the directory already exists.
				System.setProperty(DEBUGGINGIMAGES_DIRECTORY_KEY, path6.toAbsolutePath().toString());
				logFile = new File(path5.toFile(), "logFile.log");
				SysOutController.redirectSysOut(logFile);
			} catch (IOException e) {
				// something else went wrong
				e.printStackTrace();
			}
		}
	}

	public Resources(String workingDir) {
		this(Paths.get(workingDir).toAbsolutePath().toString() + File.separator + DEFAULT_INPUT_DIRECTORY,
				DEFAULT_OUTPUT_DIRECTORY, DEFAULT_OCR_DIRECTORY, DEFAULT_OCR_FILE, DEFAULT_RESULTS_DIRECTORY,
				DEFAULT_ERROR_DIRECTORY);
	}

	public Resources() {
		this(DEFAULT_INPUT_DIRECTORY, DEFAULT_OUTPUT_DIRECTORY, DEFAULT_OCR_DIRECTORY, DEFAULT_OCR_FILE,
				DEFAULT_RESULTS_DIRECTORY, DEFAULT_ERROR_DIRECTORY);
	}

	public boolean initialiseWriter() {
		if (this.writer != null) {
			this.close();
			this.writer = null;
		}
		if ((this.writer == null) || (!this.writerIsOpen)) {
			try {
				this.writer = new PrintWriter(new BufferedWriter(new FileWriter(this.ocrFilePath.toString(), true)));
				this.writerIsOpen = true;
			} catch (IOException ioe) {
				ioe.printStackTrace();
				return false;
			}
		}
		return true;
	}

	public boolean isValid() {
		return ((this.inputDirectoryPath != null) && (this.outputDirectoryPath != null) && (this.ocrResultsPath != null)
				&& (this.ocrDirectoryPath != null) && (this.ocrFilePath != null) && (this.writer != null)
				&& (this.writerIsOpen)
				&& !this.inputDirectoryPath.toString().equals(this.outputDirectoryPath.toString()));
	}

	public boolean println(String line) {
		String currentDate = new SimpleDateFormat("ddMMyyyy").format(new Date());
		if (!date.contentEquals(currentDate)) {
			date = currentDate;
			this.close();
			Path path = null;
			try {
				path = Paths.get(DEFAULT_OCR_DIRECTORY + File.separator + DEFAULT_OCR_FILE + date + ".txt");
				this.ocrFilePath = Files.createFile(path);
				this.initialiseWriter();
				// System.out.println("Created ocr file in ocr directory inside output
				// directory");
			} catch (FileAlreadyExistsException e) {
				// the file already exists.
				this.ocrFilePath = path;
				this.initialiseWriter();
			} catch (IOException e) {
				// something else went wrong
				e.printStackTrace();
			}
		}
		if ((this.writer != null) && (this.writerIsOpen)) {
			try {
				this.writer.println(line);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public boolean close() {
		try {
			this.writer.flush();
			this.writer.close();
			this.writerIsOpen = false;
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean flush() {
		try {
			this.writer.flush();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public List<String> listFiles(int depth) {
		if (this.inputDirectoryPath != null) {
			try (Stream<Path> stream = Files.walk(this.inputDirectoryPath, depth)) {
				return stream.filter(file -> !Files.isDirectory(file)).map(Path::toAbsolutePath).map(Path::toString)
						.collect(Collectors.toList());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		return new ArrayList<String>();
	}

	public boolean moveFile(String file) {
		if (file == null) {
			return false;
		}
		Path sourcePath = null;
		if (file.indexOf(File.separator) != -1) {
			sourcePath = Paths.get(file);
		} else {
			sourcePath = Paths.get(this.inputDirectoryPath.toString(), file);
		}
		Path fileName = sourcePath.getFileName();
		Path destinationPath = Paths.get(this.outputDirectoryPath.toString(), fileName.toString());
		boolean destinationPathExists = Files.exists(destinationPath, new LinkOption[] { LinkOption.NOFOLLOW_LINKS });
		if (destinationPathExists) {
			String absPath = destinationPath.toString();
			int lastDot = absPath.lastIndexOf(dotValue);
			String nameWithoutExtension = absPath.substring(0, lastDot);
			String newName = nameWithoutExtension + "-" + new Random().nextInt();
			String newFullName = newName + absPath.substring(Math.min(lastDot, absPath.length()));
			destinationPath = Paths.get(newFullName);
		}
		try {
			Files.move(sourcePath, destinationPath, StandardCopyOption.ATOMIC_MOVE);
			return true;
		} catch (NoSuchFileException nsfe) {
			// do nothing
		} catch (IOException ioe) {
			if (!(ioe instanceof NoSuchFileException)) {
				ioe.printStackTrace();
			}
		}
		return false;
	}

	public boolean writeResultsFile(String fileName, String content) {
		PrintWriter pWriter = null;
		try {
			pWriter = new PrintWriter(new BufferedWriter(
					new FileWriter(this.ocrResultsPath.toString() + File.separator + fileName, false)));
			pWriter.print(content);
			pWriter.flush();
			pWriter.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.out.println("Could not write the OCR results to the file " + this.ocrResultsPath.toString()
					+ File.separator + fileName);
			return false;
		}
		return true;
	}

	public boolean moveErrorFile(String file) {
		if (file == null) {
			return false;
		}
		Path sourcePath = null;
		if (file.indexOf(File.separator) != -1) {
			sourcePath = Paths.get(file);
		} else {
			sourcePath = Paths.get(this.inputDirectoryPath.toString(), file);
		}
		Path fileName = sourcePath.getFileName();
		Path destinationPath = Paths.get(this.ocrErrorPath.toString(), fileName.toString());
		boolean destinationPathExists = Files.exists(destinationPath, new LinkOption[] { LinkOption.NOFOLLOW_LINKS });
		if (destinationPathExists) {
			String absPath = destinationPath.toString();
			int lastDot = absPath.lastIndexOf(dotValue);
			String nameWithoutExtension = absPath.substring(0, lastDot);
			String newName = nameWithoutExtension + "-" + new Random().nextInt();
			String newFullName = newName + absPath.substring(Math.min(lastDot, absPath.length()));
			destinationPath = Paths.get(newFullName);
		}
		try {
			Files.move(sourcePath, destinationPath, StandardCopyOption.ATOMIC_MOVE);
			return true;
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return false;
	}

	@Override
	public String toString() {
		return (new StringBuffer()).append(this.inputDirectoryPath.toString()).append(System.lineSeparator())
				.append(this.outputDirectoryPath.toString()).append(System.lineSeparator())
				.append(this.ocrDirectoryPath.toString()).append(System.lineSeparator())
				.append(this.ocrResultsPath.toString()).append(System.lineSeparator())
				.append(this.ocrErrorPath.toString()).append(System.lineSeparator()).append(this.ocrFilePath.toString())
				.toString();
	}

	public static File getLogFile() {
		return logFile;
	}

}
