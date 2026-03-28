package com.techwerx.tesseract;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class TechWerxTesseractHandleFactory extends BasePooledObjectFactory<PoolHandle> {

	public static boolean strictChecking = false;
	private int processInstance;
	private int debugLevel;
	private boolean useAutoOSD;

	public TechWerxTesseractHandleFactory(boolean useAutoOSD, int processInstance, int debugLevel) {
		super();
		this.useAutoOSD = useAutoOSD;
		this.processInstance = processInstance;
		this.debugLevel = debugLevel;
	}

	public TechWerxTesseractHandleFactory(int processInstance, int debugLevel) {
		this(false, processInstance, debugLevel);
	}

	@Override
	public TechWerxTesseractHandle create() {
		return new TechWerxTesseractHandle(this.useAutoOSD, this.processInstance, this.debugLevel);
	}

	/**
	 * Use the default PooledObject implementation.
	 */
	@Override
	public PooledObject<PoolHandle> wrap(PoolHandle handle) {
		return new DefaultPooledObject<PoolHandle>(handle);
	}

	/**
	 * When an object is returned to the pool, clear the handle.
	 */
	@Override
	public void passivateObject(PooledObject<PoolHandle> pooledObject) throws Exception {
		// System.out.println("Returned TechWerxTesseractHandle object - " +
		// pooledObject);
		// if (!pooledObject.getObject().release()) {
		// throw new Exception("Could not release object " + pooledObject);
		// }
		// NO NEED TO DO ANYTHING SPECIAL ON THE TECHWERXTESSERACT OBJECT AS IT RELEASES
		// RESOURCES ON ITS OWN VOLITION AFTER DOING OCR
	}

	@Override
	public void destroyObject(final PooledObject<PoolHandle> pooledObject) throws Exception {
		if (!pooledObject.getObject().destroy()) {
			throw new Exception("Could not destroy object " + pooledObject);
		}
	}

	@Override
	public boolean validateObject(final PooledObject<PoolHandle> pooledObject) {
		return pooledObject.getObject().isValid();
	}

	// for all other methods, the no-op implementation
	// in BasePooledObjectFactory will suffice
}