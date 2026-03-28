package com.techwerx.labelprocessing;

public class ProcessingData {
	public int filesProcessed = 0;
	public long timeTaken = 0;
	public int okFiles = 0;

	public ProcessingData(int files, long time, int okFiles) {
		this.filesProcessed = files;
		this.timeTaken = time;
		this.okFiles = okFiles;
	}

	public String reportPerformance() {
		if (this.filesProcessed != 0) {
			String ok = ". OK Files = " + this.okFiles;
			return String.format("Processed %d files in %d ms, averaging %d ms per file", this.filesProcessed,
					this.timeTaken, this.timeTaken / this.filesProcessed) + ok;

		}
		return "";
	}

	public void update(ProcessingData input) {
		this.filesProcessed += input.filesProcessed;
		this.timeTaken += input.timeTaken;
		this.okFiles += input.okFiles;
	}

	public void reset() {
		this.filesProcessed = 0;
		this.timeTaken = 0;
		this.okFiles = 0;
	}
}
