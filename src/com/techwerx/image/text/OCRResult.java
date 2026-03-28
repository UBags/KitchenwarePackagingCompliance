package com.techwerx.text;

import java.util.ArrayList;

/**
 * Holds the sentences and confidence scores returned by a single Tesseract OCR
 * call. Replaces the former OCRResultBI and OCRResultPix classes, which were
 * structurally identical. The source type (BufferedImage vs Leptonica Pix) is a
 * calling-convention detail that does not affect the data stored here.
 */
public class OCRResult {

	public ArrayList<String> sentences;
	public ArrayList<Integer> confidences;

	public OCRResult() {
		this.sentences = new ArrayList<>();
		this.confidences = new ArrayList<>();
	}

	public OCRResult(String words, float confidence) {
		this(words, (int) confidence);
	}

	public OCRResult(String words, int confidence) {
		this.sentences = new ArrayList<>();
		this.confidences = new ArrayList<>();
		this.sentences.add(words);
		this.confidences.add(confidence);
	}

	public void add(String words, float confidence) {
		this.add(words, (int) confidence);
	}

	public void add(String words, int confidence) {
		this.sentences.add(words);
		this.confidences.add(confidence);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (String word : this.sentences) {
			sb.append(word).append(" ");
		}
		return sb.toString();
	}

}