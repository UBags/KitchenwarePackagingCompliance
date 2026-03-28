/**
 *
 */
package com.techwerx.text;

import java.util.ArrayList;

/**
 * @author Admin
 *
 */
public class OCRResultPix {

	public ArrayList<String> sentences;
	public ArrayList<Integer> confidences;

	public OCRResultPix() {
		this.sentences = new ArrayList<>();
		this.confidences = new ArrayList<>();
	}

	public OCRResultPix(String words, float confidence) {
		this(words, (int) confidence);
	}

	public OCRResultPix(String words, int confidence) {
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
