package com.dotcms.publishing;

import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.XMLUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Date;

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

		String bundlePath = ConfigUtils.getBundlePath()+ File.separator + config.getName();
		bundlePath += File.separator + "bundle.xml";

		return new File(bundlePath).exists();
	}

    public static File getBundleRoot(String name) {
        return getBundleRoot(name, true);
    }

	public static File getBundleRoot(String name, boolean createDir) {
		String bundlePath = ConfigUtils.getBundlePath()+ File.separator + name;
		File dir = new File(bundlePath);
		if (createDir){
            dir.mkdirs();
        }
		return dir;
	}

    public static File getStaticBundleRoot(String name) {
        return getStaticBundleRoot(name, true);
    }

    public static File getStaticBundleRoot(String name, boolean createDir) {
        String bundlePath = ConfigUtils.getStaticPublishPath() + File.separator + name;
        File dir = new File(bundlePath);
        if (createDir) {
            dir.mkdirs();
        }
        return dir;
    }

	/**
	 * This method takes a config and will create the bundle directory and
	 * write the bundle.xml file to it
	 * @param config Config with the id of bundle
	 */
	public static File getBundleRoot(PublisherConfig config){
		return getBundleRoot(config.getName());
	}

    /**
     * This method takes a config and will create the static bundle directory and
     * write the bundle.xml file to it
     *
     * @param config Config with the id of bundle
     */
    public static File getStaticBundleRoot(PublisherConfig config) {
        return getStaticBundleRoot(config.getName());
    }

	/**
	 * write bundle down
	 * @param config
	 */
	public static void writeBundleXML(PublisherConfig config){
		getBundleRoot(config);

		String bundlePath = ConfigUtils.getBundlePath()+ File.separator + config.getName();

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

		String bundlePath = ConfigUtils.getBundlePath()+ File.separator + config.getName();

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
            
            try(OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(f.toPath()), "UTF-8")){
                HierarchicalStreamWriter xmlWriter = new DotPrettyPrintWriter(writer);
                xstream.marshal(obj, xmlWriter);
            }

        } catch ( FileNotFoundException e ) {
            Logger.error( PublisherUtil.class, e.getMessage(), e );
        } catch ( IOException e ) {
            Logger.error( PublisherUtil.class, e.getMessage(), e );
        }
    }

    /**
     * Serialize a given object to json (using jackson)
     *
     * @param obj Object to serialize
     * @param f   File to write to
     */
    public static void objectToJSON( Object obj, File f ) {
        objectToJSON( obj, f, true );
    }

    /**
     * Serialize a given object to json (using jackson)
     *
     * @param obj Object to serialize
     * @param f   File to write to
     */
    public static void objectToJSON( Object obj, File f, boolean removeFirst ) {

        if ( removeFirst && f.exists() )
            f.delete();

        ObjectMapper mapper = new ObjectMapper();

        try {
            if ( !f.exists() ){
                //Lets create the folders if necessary to avoid "No such file or directory" error.
                if(f.getParentFile() != null){
                    f.getParentFile().mkdirs();
                    }
                //Lets create the file.
            	f.createNewFile();
            }	

            mapper.writeValue(f, obj);

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

        try (InputStream input = new BufferedInputStream(Files.newInputStream(f.toPath()))) {
            return xstream.fromXML(input);
		} catch (Exception e) {
			Logger.warnAndDebug(BundlerUtil.class,e.getMessage(),e);
			return xmlToObjectWithPrologue(f);
		}
	}

    /**
     * Adds a XML 1.1 Prologue before trying to deserialize
     * @param file
     * @return
     */
    private static Object xmlToObjectWithPrologue(File file) {
        XStream xstream = new XStream(new DomDriver("UTF-8"));
        XMLUtils.addPrologueIfNeeded(file);
        try (InputStream input = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            return xstream.fromXML(input);
        } catch (Exception e) {
           throw new DotRuntimeException("Unable to deserialize XML: " + file + " " + e.getMessage(), e);
        }
        


    }
    
    

    /**
     * Deserialize an object back from JSON (using jackson)
     *
     * @param f file to deserialize
     * @param clazz value type to deserialize
     * @return A deserialized object
     */
    public static <T> T jsonToObject(File f, Class<T> clazz){
    	ObjectMapper mapper = new ObjectMapper();

    	BufferedInputStream input = null;
		try {
			input = new BufferedInputStream(Files.newInputStream(f.toPath()));
			T ret = mapper.readValue(input, clazz);
			return ret;
		} catch (IOException e) {
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

    /**
     * Checks the name of the bundle file coming from the request to make sure that it is correct.
     * For example, in Microsoft Edge on Windows 10, the name includes the absolute path to the file
     * instead of the file name only. We need to strip the absolute path away.
     *
     * @param bundleName The name of the bundle file.
     * @return The sanitized bundle file name.
     */
    public static String sanitizeBundleName(String bundleName) throws DotPublisherException {

        if (!UtilMethods.isSet(bundleName)) {
            final String errorMsg = "The name for the uploaded bundle is null or empty";
            Logger.error(BundlerUtil.class, errorMsg);
            throw new DotPublisherException(errorMsg);
        }
        if (!bundleName.contains(File.separator)) {
            return bundleName;
        } else {
            final int indexOfSeparator = bundleName.lastIndexOf(File.separator);
            bundleName = bundleName.substring(indexOfSeparator + 1);
            return bundleName;
        }
    }

}