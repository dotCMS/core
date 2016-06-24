package com.dotcms.publishing;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.publishing.bundlers.FileAssetBundler;
import com.dotcms.repackage.com.thoughtworks.xstream.XStream;
import com.dotcms.repackage.com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.dotcms.repackage.com.thoughtworks.xstream.io.xml.DomDriver;

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
     * Reads the main bundle.xml file inside a bundle directory in order to create based on that file a PublisherConfig object
     *
     * @param config This bundle current configuration
     * @return The Bundle configuration read from the mail Bundle xml file
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

    /**
     * Serialize a given object to xml
     *
     * @param obj Object to serialize
     * @param f   File to write to
     */
    public static void objectToXML ( Object obj, File f ) {
        objectToXML( obj, f, true );
    }

    /**
     * Serialize a given object to xml
     *
     * @param obj Object to serialize
     * @param f   File to write to
     */
    public static void objectToXML ( Object obj, File f, boolean removeFirst ) {

        if ( removeFirst && f.exists() )
            f.delete();

        XStream xstream = new XStream( new DomDriver("UTF-8") );

        try {
            if ( !f.exists() ){
                //Lets create the folders if necessary to avoid "No such file or directory" error.
                if(f.getParentFile() != null){
                    f.getParentFile().mkdirs();
                    }
                //Lets create the file.
            	f.createNewFile();
            }	
            
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream( f ), "UTF-8");
            HierarchicalStreamWriter xmlWriter = new DotPrettyPrintWriter(writer);
            xstream.marshal(obj, xmlWriter);
            writer.close();

        } catch ( FileNotFoundException e ) {
            Logger.error( PublisherUtil.class, e.getMessage(), e );
        } catch ( IOException e ) {
            Logger.error( PublisherUtil.class, e.getMessage(), e );
        }
    }


    /**
     * Deserialize an object back from XML
     *
     * @param f file to deserialize
     * @return A deserialized object
     */
    public static Object xmlToObject(File f){
    	XStream xstream = new XStream(new DomDriver("UTF-8"));

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
    
    public static void publisherConfigToLuceneQuery(StringBuilder bob, PublisherConfig config) {
        
        if(config.getExcludePatterns() != null && config.getExcludePatterns().size()>0){
            bob.append("-(" );
            for (String p : config.getExcludePatterns()) {
                if(!UtilMethods.isSet(p)){
                    continue;
                }
                //p = p.replace(" ", "+");
                bob.append("path:").append(p).append(" ");
            }
            bob.append(")" );
        }else if(config.getIncludePatterns() != null && config.getIncludePatterns().size()>0){
            bob.append("+(" );
            for (String p : config.getIncludePatterns()) {
                if(!UtilMethods.isSet(p)){
                    continue;
                }
                //p = p.replace(" ", "+");
                bob.append("path:").append(p).append(" ");
            }
            bob.append(")" );
        }
        
        if(config.isIncremental()) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, 1900);
            
            Date start;
            Date end;
            
            if(config.getStartDate() != null){
                start = config.getStartDate();
            } else {
                start = cal.getTime();                
            }
            
            if(config.getEndDate() != null){
                end = config.getEndDate();
            } else {
                end = cal.getTime();
            }
            
                        
            bob.append(" +versionTs:[").append(ESMappingAPIImpl.datetimeFormat.format(start)) 
                    .append(" TO ").append(ESMappingAPIImpl.datetimeFormat.format(end)).append("] ");
        }
        
        
        if(config.getHosts() != null && config.getHosts().size() > 0){
            bob.append(" +(" );
            for(Host h : config.getHosts()){
                bob.append("conhost:").append(h.getIdentifier()).append(" ");
            }
            bob.append(" ) " );
        }
    }
}
