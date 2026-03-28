package com.techwerx.tesseract;

public class TechWerxTesseractHandle implements PoolHandle {

	// public static final String language = "eng+hin";
	private TechWerxTesseract handle;
	private int processInstance;
	private int debugLevel;
	private boolean useAutoOSD;

	public TechWerxTesseractHandle(boolean useAutoOSD, int processInstance, int debugLevel) {
		this.useAutoOSD = useAutoOSD;
		this.processInstance = processInstance;
		this.debugLevel = debugLevel;
		this.handle = new TechWerxTesseract(useAutoOSD, debugLevel);
		if (this.debugLevel <= 4) {
			System.out.println("Created a TechWerxTesseractHandle");
		}
	}

	public TechWerxTesseractHandle(int processInstance, int debugLevel) {
		this(false, processInstance, debugLevel);
	}

	@Override
	public Object handle(Object image) throws Exception {
		return null;
	}

	@Override
	public boolean isValid() {
		return this.handle.isValid();
	}

	@Override
	public boolean release() {
		// return this.handle.release();
		// NOTHING NEEDS TO BE DONE HERE AS THE TECHWERXTESSERACT OBJECT TAKES CARE OF
		// RELEASING RESOURCES OF ITS OWN VOLITION AFTER FINISHING OCR, THEREBY READYING
		// IT FOR REUSE
		return true;
	}

	@Override
	public boolean destroy() {
		return this.handle.destroy();
	}

	public TechWerxTesseract getHandle() {
		return this.handle;
	}

	public int getProcessInstance() {
		return this.processInstance;
	}

}
