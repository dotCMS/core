package com.dotcms.publishing;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publishing.manifest.ManifestUtil;
import com.dotcms.publishing.output.BundleOutput;
import com.dotcms.publishing.output.TarGzipBundleOutput;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.XMLUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BundlerUtil {


    private static ObjectMapper customMapper;

    public static final List<Status> STATUS_TO_RETRY = list(
            Status.FAILED_TO_PUBLISH, Status.SUCCESS, Status.SUCCESS_WITH_WARNINGS,
            Status.FAILED_TO_SEND_TO_ALL_GROUPS, Status.FAILED_TO_SEND_TO_SOME_GROUPS, Status.FAILED_TO_SENT
    );

	/**
	 * does bundle exist
	 * @param config
	 * @return
	 */
	public static boolean bundleExists(final PublisherConfig config){
		if(config.getId() ==null){
			throw new DotStateException("publishing config.id is null.  Please set an id before publishing (it will be the folder name under which the bundle will be created)");
		}

		if (config.isStatic()) {
            String bundlePath = ConfigUtils.getBundlePath()+ File.separator + config.getName();
            bundlePath += File.separator + "bundle.xml";

            return new File(bundlePath).exists();
        } else {
		    return ManifestUtil.manifestExists(config.getId());
        }

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

    public static boolean isRetryable(final PublishAuditStatus status) {
        return UtilMethods.isSet(status) && isRetryable(status.getStatus());
    }

    /**
     * Return true if a bundle ca ne retry.
     * A bundle can be retry if:
     *
     * - If the method {@link BundlerUtil#isRetryable(Status)} return true
     * - And The Bundle is not in the Publish Queue, it meean that the
     * {@link PublisherAPI#getQueueElementsByBundleId(String)}  return a empty list
     * - If the bundle's file exists, it mean:
     *      - The bundle's tar.gzip file exists if it no a static bundle.
     *      - The bundle's root folder exists if it a static bundle.
     *
     * @param bundleId
     * @return
     */
    public static boolean isRetryable(final String bundleId) {
        try {
            final PublishAuditStatus publishAuditStatus = PublishAuditAPI.getInstance()
                    .getPublishAuditStatus(bundleId, 0);

            return isRetryable(publishAuditStatus) && !isInPublishQueue(bundleId)
                    && bundleExists(bundleId);
        } catch (DotPublisherException e) {
            Logger.error(BundlerUtil.class, e.getMessage());
            return false;
        }
    }

    private static boolean bundleExists(String bundleId) {
        final PublisherConfig basicConfig = new PublisherConfig();
        basicConfig.setId(bundleId);
        final File bundleRoot = BundlerUtil.getBundleRoot( basicConfig.getName(), false );

        final File bundleStaticFile = new File(bundleRoot.getAbsolutePath() + PublisherConfig.STATIC_SUFFIX);
        if ( !bundleStaticFile.exists() ) {
            return true;
        }

        final File bundleFile = new File( ConfigUtils.getBundlePath() + File.separator + basicConfig.getId() + ".tar.gz" );
        if ( !bundleFile.exists() ) {
            return true;
        }
        return false;
    }

    private static boolean isInPublishQueue(String bundleId) throws DotPublisherException {
        List<PublishQueueElement> foundBundles = PublisherAPI.getInstance()
                .getQueueElementsByBundleId(bundleId);

        if ( foundBundles != null && !foundBundles.isEmpty() ) {
            return true;
        }
        return false;
    }

    /**
     * Return true if <code>status</code> is one of the follow {@link PublishAuditStatus.Status}:
     *
     * - {@link Status#FAILED_TO_PUBLISH}
     * - {@link Status#SUCCESS}
     * - {@link Status#SUCCESS_WITH_WARNINGS}
     * - {@link Status#FAILED_TO_SEND_TO_ALL_GROUPS}
     * - {@link Status#FAILED_TO_SEND_TO_SOME_GROUPS}
     * - {@link Status#FAILED_TO_SENT}
     *
     * @param status
     * @return
     */
    public static boolean isRetryable(final PublishAuditStatus.Status status) {
        return UtilMethods.isSet(status) && STATUS_TO_RETRY.contains(status);
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
     * @param output
     */
	public static void writeBundleXML(final PublisherConfig config, final BundleOutput output){
		final String bundleXmlFilePath = File.separator + "bundle.xml";

		try (final OutputStream outputStream = output.addFile(bundleXmlFilePath)) {
            objectToXML(config, outputStream);
        } catch ( IOException e ) {
            Logger.error( BundlerUtil.class, e.getMessage(), e );
        }
	}

    /**
     * Reads the main bundle.xml file inside a bundle directory in order to create based on that file a PublisherConfig object
     *
     * @param config This bundle current configuration
     * @return The Bundle configuration read from the mail Bundle xml file
     */
    public static PublisherConfig readBundleXml(PublisherConfig config){
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

        try {
            if ( !f.exists() ){
                //Lets create the folders if necessary to avoid "No such file or directory" error.
                if(f.getParentFile() != null){
                    f.getParentFile().mkdirs();
                    }
                //Lets create the file.
            	f.createNewFile();
            }	


            try(final OutputStream outputStream = Files.newOutputStream(f.toPath())){
                objectToXML(obj, outputStream);
            }

        } catch ( IOException e ) {
            Logger.error( PublisherUtil.class, e.getMessage(), e );
        }
    }

    public static void objectToXML(final Object obj, final OutputStream outputStream) {
        HierarchicalStreamWriter xmlWriter = new DotPrettyPrintWriter(new OutputStreamWriter(outputStream));
        XMLSerializerUtil.getInstance().marshal(obj, xmlWriter);
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

        final ObjectMapper mapper = getCustomMapper();

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

    private static ObjectMapper getObjectMapper() {
        return DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
    }

    private static ObjectMapper getCustomMapper() {
        if (customMapper == null) {
            customMapper = getObjectMapper();
        }
        return customMapper;
    }


    public static void objectToJSON( final Object obj, final OutputStream outputStream) {
        final ObjectMapper mapper = getObjectMapper();

        try {
            mapper.writeValue(outputStream, obj);
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
        try (InputStream input = Files.newInputStream(f.toPath())) {
            return xmlToObject(input);
		} catch (Exception e) {
			Logger.warnAndDebug(BundlerUtil.class,e.getMessage(),e);
			return xmlToObjectWithPrologue(f);
		}
	}

    public static Object xmlToObject(final InputStream inputStream) throws IOException {
        final XStream xstream = XMLSerializerUtil.getInstance().getXmlSerializer();

        try (InputStream input = new BufferedInputStream(inputStream)) {
            return xstream.fromXML(input);
        }
    }

    /**
     * Adds a XML 1.1 Prologue before trying to deserialize
     * @param file
     * @return
     */
    private static Object xmlToObjectWithPrologue(File file) {
        final XStream xstream = XMLSerializerUtil.getInstance().getXmlSerializer();
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
    	final ObjectMapper mapper = getObjectMapper();

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

    /**
     * Deserialize an object back from JSON (using jackson)
     *
     * @param f file to deserialize
     * @param typeReference value type to deserialize
     * @return A deserialized object
     */
    public static <T> T jsonToObject(File f, TypeReference<T> typeReference){
        ObjectMapper mapper = new ObjectMapper();

        try (BufferedInputStream input = new BufferedInputStream(Files.newInputStream(f.toPath()))){
            T ret = mapper.readValue(input, typeReference);
            return ret;
        } catch (IOException e) {
            Logger.error(BundlerUtil.class,e.getMessage(),e);
            return null;
        }
    }

    /**
     * Collects the pieces of content that will be included in this Bundle.
     *
     * @param luceneQuery The Lucene query that will retrieve the content added to this Bundle.
     * @param config      The configuration parameters of the Bundle.
     */
    public static void publisherConfigToLuceneQuery(StringBuilder luceneQuery, PublisherConfig config) {
        
        if (UtilMethods.isSet(config.getExcludePatterns())) {
            luceneQuery.append("-(" );
            for (final String pattern : config.getExcludePatterns()) {
                if (UtilMethods.isSet(pattern)) {
                    // Adding double quotes for the query to work with paths containing blank spaces
                    luceneQuery.append("path:\"").append(pattern).append("\" ");
                }
            }
            luceneQuery.append(")" );
        }else if (UtilMethods.isSet(config.getIncludePatterns())) {
            luceneQuery.append("+(" );
            for (final String pattern : config.getIncludePatterns()) {
                if (UtilMethods.isSet(pattern)) {
                    // Adding double quotes for the query to work with paths containing blank spaces
                    luceneQuery.append("path:\"").append(pattern).append("\" ");
                }
            }
            luceneQuery.append(")" );
        }
        
        if (config.isIncremental()) {
            final Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, 1900);
            
            Date start;
            Date end;
            
            if (config.getStartDate() != null) {
                start = config.getStartDate();
            } else {
                start = cal.getTime();                
            }
            
            if (config.getEndDate() != null) {
                end = config.getEndDate();
            } else {
                end = cal.getTime();
            }
                        
            luceneQuery.append(" +versionTs:[").append(ESMappingAPIImpl.datetimeFormat.format(start))
                    .append(" TO ").append(ESMappingAPIImpl.datetimeFormat.format(end)).append("] ");
        }

        if (UtilMethods.isSet(config.getHosts())) {
            luceneQuery.append(" +(" );
            for (final Host site : config.getHosts()) {
                luceneQuery.append("conhost:").append(site.getIdentifier()).append(" ");
            }
            luceneQuery.append(" ) " );
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

    public static boolean tarGzipExists(final String bundleId) {
        final File bundleTarGzip = TarGzipBundleOutput.getBundleTarGzipFile(bundleId);
        return bundleTarGzip.exists();
    }

}
