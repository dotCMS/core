package com.dotcms.publishing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class BundlerUtil {

	/**
	 * does bundle exist
	 * @param config
	 * @return
	 */
	public static boolean bundleExists(PublisherConfig config){
		if(config.getId() ==null){
			throw new DotStateException("publishing config.id is null.  Please set an id before publishing (it will be the folder name under which the bundle will be created)");
		}
		String bundlePath = ConfigUtils.getBundlePath()+ File.separator + config.getId()+ File.separator + "bundle.xml";
		
		
		return new File(bundlePath).exists();
	}
	
	/**
	 * This method takes a config and will create the bundle directory and
	 * write the bundle.xml file to it
	 * @param name Name of bundle
	 */
	public static File getBundleRoot(String name) {
		String bundlePath = ConfigUtils.getBundlePath()+ File.separator + name;
		File dir = new File(bundlePath);
		dir.mkdirs();
		return dir;
	}
	
	/**
	 * This method takes a config and will create the bundle directory and
	 * write the bundle.xml file to it
	 * @param config Config with the id of bundle
	 */
	public static File getBundleRoot(PublisherConfig config){
		return getBundleRoot(config.getId());
	}
	
	/**
	 * write bundle down
	 * @param config
	 */
	public static void writeBundleXML(PublisherConfig config){
		getBundleRoot(config);
		String bundlePath = ConfigUtils.getBundlePath()+ File.separator + config.getId();
		File xml = new File(bundlePath + File.separator + "bundle.xml");
		objectToXML(config, xml);

	}
	/**
	 * 
	 * @param config
	 * @return
	 */
	public static PublisherConfig readBundleXml(PublisherConfig config){
		getBundleRoot(config);
		String bundlePath = ConfigUtils.getBundlePath()+ File.separator + config.getId();
		File xml = new File(bundlePath + File.separator + "bundle.xml");
		if(xml.exists()){
			return (PublisherConfig) xmlToObject(xml);
		}
		else{
			return null;
		}
	}
	
	public static void objectToXML(Object obj, File f){
		objectToXML(obj, f, true);
	}

	/**
	 * 
	 * @param obj
	 * @param f File to write to
	 */
	public static void objectToXML(Object obj, File f, boolean removeFirst){

		if ( removeFirst && f.exists() )
			f.delete();
		
		XStream xstream = new XStream(new DomDriver());

		try {
			if(!f.exists())f.createNewFile();
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
			xstream.toXML(obj, out);
			out.close();
			
			
		} catch (FileNotFoundException e) {
			Logger.error(PublisherUtil.class,e.getMessage(),e);
		}catch (IOException e) {
			Logger.error(PublisherUtil.class,e.getMessage(),e);
		}	
	}
	
	
	/**
	 * 
	 * @param obj
	 * @param f File to write to
	 */
	public static Object xmlToObject(File f){
		XStream xstream = new XStream(new DomDriver());

		 BufferedInputStream input = null;
		try {
			input = new BufferedInputStream(new FileInputStream(f));
			Object ret = xstream.fromXML(input);
			return ret;
		} catch (FileNotFoundException e) {
			Logger.error(BundlerUtil.class,e.getMessage(),e);
			return null;
		}finally{
			try {
				input.close();
			}
			catch(Exception e){
				
			}
		}

	}
}
