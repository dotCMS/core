package com.dotcms.publishing;

import java.io.File;

/**
 * The Purpose of the IGenerators is to provide a way to say how to write out the different parts and objects of the bundle
 * @author jasontesser
 *
 */
public interface IBundler {
	
	public String getName();
	
	public void setConfig(PublisherConfig pc);
	
	/**
	 * returns the number of objects generated
	 * @param bundleRoot
	 * @return
	 */
	public void generate(File bundleRoot, BundlerStatus status) throws DotBundleException;
	
}
