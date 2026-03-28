package com.techwerx.text;

public class ProcessDataWrapper {

	public boolean linesNeededSplitting = false;
	public boolean linesNotSplitDueToHighOverlap = false;
	public KDEData kdeData = new KDEData();

	public void reset() {
		this.linesNeededSplitting = false;
		this.linesNotSplitDueToHighOverlap = false;
		this.kdeData = new KDEData();
	}

}
