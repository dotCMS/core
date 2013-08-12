package com.dotcms.publishing;

import java.io.File;
import java.io.FileFilter;

/**
 * The Purpose of the IGenerators is to provide a way to say how to write out the different parts and objects of the bundle
 *
 * @author jasontesser
 */
public interface IBundler {
	
	public String getName();
	
	public void setConfig(PublisherConfig pc);
	
	/**
     * Generates depending of the type of content this Bundler handles parts and objects that will be add it later
     * to a Bundle.
     *
     * @param bundleRoot Where the Bundle we are creating will live.
     * @param status     Object to keep track of the generation process inside this Bundler
     * @throws DotBundleException If there is an exception while this Bundles is generating the Bundle content
	 */
	public void generate(File bundleRoot, BundlerStatus status) throws DotBundleException;

	public FileFilter getFileFilter();
	
}
