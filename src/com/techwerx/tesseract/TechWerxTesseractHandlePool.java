package com.techwerx.tesseract;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * An Apache Commons Pool2 {@link GenericObjectPool} of {@link PoolHandle}
 * instances, each wrapping a {@link TechWerxTesseract} OCR engine instance.
 *
 * <p>Pre-defined pool size configurations are provided as public constants.
 * Callers construct the pool directly via the constructors; the former
 * {@code createPool()} / {@code getPool()} factory methods were dead code
 * (never called from outside this class) and have been removed.
 */
public class TechWerxTesseractHandlePool extends GenericObjectPool<PoolHandle> {

	// -------------------------------------------------------------------------
	// Pre-defined pool configurations
	// -------------------------------------------------------------------------

	public static final GenericObjectPoolConfig<PoolHandle> singletonConfig = new GenericObjectPoolConfig<>();
	static {
		singletonConfig.setMaxIdle(1);
		singletonConfig.setMaxTotal(1);
		singletonConfig.setLifo(true);
		singletonConfig.setMinIdle(1);
		singletonConfig.setTestOnReturn(true);
	}

	public static final GenericObjectPoolConfig<PoolHandle> defaultConfig = new GenericObjectPoolConfig<>();
	static {
		defaultConfig.setMaxIdle(10);
		defaultConfig.setMaxTotal(25);
		defaultConfig.setLifo(false);
		defaultConfig.setMinIdle(5);
		defaultConfig.setTestOnReturn(true);
	}

	public static final GenericObjectPoolConfig<PoolHandle> oneMachineConfig = new GenericObjectPoolConfig<>();
	static {
		oneMachineConfig.setMaxIdle(30);
		oneMachineConfig.setMaxTotal(50);
		oneMachineConfig.setLifo(false);
		oneMachineConfig.setMinIdle(25);
		oneMachineConfig.setTestOnReturn(true);
	}

	public static final GenericObjectPoolConfig<PoolHandle> smallPoolConfig = new GenericObjectPoolConfig<>();
	static {
		smallPoolConfig.setMaxIdle(10);
		smallPoolConfig.setMaxTotal(15);
		smallPoolConfig.setLifo(false);
		smallPoolConfig.setMinIdle(9);
		smallPoolConfig.setTestOnReturn(true);
	}

	// -------------------------------------------------------------------------
	// Instance state
	// -------------------------------------------------------------------------

	private final int processInstance;
	private final int debugLevel;

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	/**
	 * Constructs a pool using the {@link #defaultConfig}.
	 *
	 * @param factory         object factory for creating/validating/destroying handles
	 * @param processInstance process instance identifier (used for logging)
	 * @param debugLevel      debug verbosity level
	 */
	public TechWerxTesseractHandlePool(PooledObjectFactory<PoolHandle> factory, int processInstance, int debugLevel) {
		this(factory, defaultConfig, processInstance, debugLevel);
	}

	/**
	 * Constructs a pool with a specific configuration.
	 *
	 * @param factory         object factory for creating/validating/destroying handles
	 * @param config          pool size and behaviour configuration
	 * @param processInstance process instance identifier (used for logging)
	 * @param debugLevel      debug verbosity level
	 */
	public TechWerxTesseractHandlePool(PooledObjectFactory<PoolHandle> factory,
			GenericObjectPoolConfig<PoolHandle> config, int processInstance, int debugLevel) {
		super(factory, config);
		this.processInstance = processInstance;
		this.debugLevel = debugLevel;
		try {
			this.preparePool();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (this.debugLevel <= 4) {
			System.out.println("Created a TechWerxTesseractHandlePool of size "
					+ ((this.getNumIdle() >= 0 ? this.getNumIdle() : 0)
							+ (this.getNumActive() >= 0 ? this.getNumActive() : 0)));
		}
	}

	// -------------------------------------------------------------------------
	// Accessors
	// -------------------------------------------------------------------------

	/**
	 * @return the process instance identifier supplied at construction time
	 */
	public int getProcessInstance() {
		return this.processInstance;
	}

}