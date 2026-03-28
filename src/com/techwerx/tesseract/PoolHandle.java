/**
 *
 */
package com.techwerx.tesseract;

/**
 * @author Uddipan Bagchi
 *
 */
public interface PoolHandle {

	/**
	 * handle the element E and populate result in object T
	 *
	 * @param elementToBeHandled
	 * @result result
	 * @throws Exception
	 */
	public Object handle(Object object) throws Exception;

	/**
	 * Tells whether this handle is valid or not. This will ensure the we will never
	 * be using an invalid/corrupt handle.
	 *
	 * @return whether the handle is valid
	 */
	public boolean isValid();

	/**
	 * Reset handle state back to the original, so that it will be as good as a new
	 * handle.
	 *
	 * @return whether the handle has been successfully reset
	 */
	public boolean release();

	/**
	 * Destroy the original and release memory / resources
	 *
	 * @return whether the handle has been successfully destroyed
	 */
	public boolean destroy();

}
