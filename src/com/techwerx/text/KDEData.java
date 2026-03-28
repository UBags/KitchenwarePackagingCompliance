package com.techwerx.text;

import java.util.ArrayList;

// class for storing KernelDensityEstimation data for width and height of bounding boxes

public class KDEData {

	public int heightKDE;
	public ArrayList<Integer> heightHistogram;
	public ArrayList<Integer> heightModes;
	public int mostLikelyHeight;

	public int widthKDE;
	public ArrayList<Integer> widthHistogram;
	public ArrayList<Integer> widthModes;
	public int mostLikelyWidth;

	/**
	 * @param mostLikelyWidth the mostLikelyWidth to set
	 */
	public void setMostLikelyWidth(int mostLikelyWidth) {
		this.mostLikelyWidth = mostLikelyWidth;
	}

	/**
	 * @param mostLikelyHeight the mostLikelyHeight to set
	 */
	public void setMostLikelyHeight(int mostLikelyHeight) {
		this.mostLikelyHeight = mostLikelyHeight;
	}

	/**
	 * @param heightKDE the heightKDE to set
	 */
	public void setHeightKDE(int heightKDE) {
		this.heightKDE = heightKDE;
	}

	/**
	 * @param heightHistogram the heightHistogram to set
	 */
	public void setHeightHistogram(ArrayList<Integer> heightHistogram) {
		this.heightHistogram = heightHistogram;
	}

	/**
	 * @param heightModes the heightModes to set
	 */
	public void setHeightModes(ArrayList<Integer> heightModes) {
		this.heightModes = heightModes;
	}

	/**
	 * @param widthKDE the widthKDE to set
	 */
	public void setWidthKDE(int widthKDE) {
		this.widthKDE = widthKDE;
	}

	/**
	 * @param widthHistogram the widthHistogram to set
	 */
	public void setWidthHistogram(ArrayList<Integer> widthHistogram) {
		this.widthHistogram = widthHistogram;
	}

	/**
	 * @param widthModes the widthModes to set
	 */
	public void setWidthModes(ArrayList<Integer> widthModes) {
		this.widthModes = widthModes;
	}

	public void reset() {
		this.heightKDE = 0;
		this.heightHistogram = null;
		this.heightModes = null;
		this.mostLikelyHeight = 0;

		this.widthKDE = 0;
		this.widthHistogram = null;
		this.widthModes = null;
		this.mostLikelyWidth = 0;
	}
}
