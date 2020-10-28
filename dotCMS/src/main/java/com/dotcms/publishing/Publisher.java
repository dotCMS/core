package com.dotcms.publishing;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.pusher.PushUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.collections.map.CompositeMap;

public abstract class Publisher implements IPublisher {

	protected PublisherConfig config;

    protected static final String BUNDLE_ID = "BundleId";
    protected static final String ENDPOINT_NAME = "EndpointName";
	protected static final String LIVE_FOLDER = File.separator + "live";
    protected static final String CURRENT_HOST = "currentHost";
    protected static final String CURRENT_LANGUAGE = "currentLanguage";
    protected static final String HOST_ID                                  = "hostId";
    protected static final String HOST_NAME                                = "hostname";
    protected static final String LANGUAGE_ISO                             = "languageIso";
    protected static final String LANGUAGE_ID                              = "languageId";
    protected static final String LANGUAGE_COUNTRY                         = "languageCountry";
    protected static final String DATE                                     = "date";
    protected static final String DATE_FORMAT_DEFAULT                      = "yyyyMMdd-HHmmss";
    protected static final String DOT_STATIC_DATE = "dot-static-date";

    protected static final int GROUPS_SIZE = 3;

	/**
	 * This method gets called before any publisher processes
	 * the config
	 */
	public PublisherConfig init(PublisherConfig config)throws DotPublishingException {

		this.config = config;

		return config;
	}

	abstract public PublisherConfig process(PublishStatus status) throws DotPublishingException ;


	@Override
    public boolean shouldForcePush(String hostId, long languageId) {
    	return false;
    }


	protected void processDirectory(Folder folder) {

	}


	protected void processStructure(Structure struct) {

	}
	// returns true if an asset/path should be included (DENY, ALLOW)
	protected boolean includeAsset(String path) {
		return true;



	}

	// gets a fake request object with language and user set
	protected HttpServletRequest constructRequest() {

		return null;

	}

	// gets a fake response object
	protected HttpServletResponse constructResponse() {
		return null;
	}


	protected boolean bundleContains(String file){
		return false;
	}




	public Host getHostFromFilePath(File file) throws DotPublishingException{

		try{
			if(!file.getAbsolutePath().contains(config.getId())){
				throw new DotPublishingException("no bundle file found");
			}

			String fileSeparator = File.separator.equals("\\")?"\\\\":File.separator;
            List<String> path = Arrays.asList( file.getAbsolutePath().split( fileSeparator ) );

            String host = path.get(path.indexOf(config.getName())+2);

			return APILocator.getHostAPI().resolveHostName(host, APILocator.getUserAPI().getSystemUser(), true);
		}
		catch(Exception e){
			throw new DotPublishingException("error getting host:" + e.getMessage());
		}

	}

	public Language getLanguageFromFilePath(File file) throws DotPublishingException{
		try{
			if(!file.getAbsolutePath().contains(config.getId())){
				throw new DotPublishingException("no bundle file found");
			}

			String fileSeparator = File.separator.equals("\\")?"\\\\":File.separator;
			List<String> path = Arrays.asList( file.getAbsolutePath().split( fileSeparator ) );

			String language = path.get(path.indexOf(config.getName())+3);

			return APILocator.getLanguageAPI().getLanguage(language);
		}
		catch(Exception e){
			throw new DotPublishingException("Error getting Language:" + e.getMessage());
		}
	}

	public String getUriFromFilePath(File file) throws DotPublishingException{

		try{

			if(!file.getAbsolutePath().contains(config.getId())){
				throw new DotPublishingException("no bundle file found");
			}

			String fileSeparator = File.separator.equals("\\")?"\\\\":File.separator;
            List<String> path = Arrays.asList( file.getAbsolutePath().split( fileSeparator ) );
            path = path.subList(path.indexOf(config.getName())+4, path.size());
			StringBuilder bob = new StringBuilder();
			for(String x:path){
				bob.append("/" + x);
			}
			return bob.toString();

		}
		catch(Exception e){
			throw new DotPublishingException("error getting uri:" + e.getMessage());
		}

	}
	public String getUrlFromFilePath(File file) throws DotPublishingException{

		return getHostFromFilePath(file).getHostname()  + getUriFromFilePath(file);

	}

    /**
     * This method verifies if a given file should be process it in order to be add it to a site search index or not.
     *
     * @param assetFile
     * @return
     * @throws IOException
     * @throws DotPublishingException
     */
    protected boolean shouldProcess ( File assetFile ) throws IOException, DotPublishingException {

        if ( assetFile.isDirectory() ) {
            return false;
        } else if ( config.getStartDate() != null && assetFile.lastModified() < config.getStartDate().getTime() ) {
            return false;
        } else if (!doesPathContainLanguageId(assetFile)) {
            return false;
        }

        String filePath = getUriFromFilePath( assetFile );

        filePath = filePath.replace( File.pathSeparatorChar, '/' );
        if ( config.getIncludePatterns() != null && !config.getIncludePatterns().isEmpty() ) {
            for ( String x : config.getIncludePatterns() ) {
                boolean startsWith = x.startsWith( "*" );
                boolean endsWith = x.endsWith( "*" );
                x = x.replace( "*", "" );

                if ( endsWith && filePath.startsWith( x ) ) {
                    return true;
                } else if ( startsWith && filePath.indexOf( x ) >= 0 ) {
                    return true;
                }
            }
            return false;
        }
        if ( config.getExcludePatterns() != null && !config.getExcludePatterns().isEmpty() ) {
            for ( String x : config.getExcludePatterns() ) {
                boolean startsWith = x.startsWith( "*" );
                boolean endsWith = x.endsWith( "*" );
                x = x.replace( "*", "" );

                if ( endsWith && filePath.indexOf( x ) == 0 ) {
                    return false;
                } else if ( startsWith && filePath.contains( x + "." ) ) {
                    return false;
                }

            }
        }

        return true;
    }

    private boolean doesPathContainLanguageId(File assetFile) {
        return assetFile.getPath().contains(
                File.separatorChar + Long.toString(config.getLanguage()) + File.separatorChar);
    }

    public Set<String> getProtocols(){
    	return new HashSet<>();
	}

	public PublisherConfig setUpConfig(PublisherConfig config){
		return config;
	}

    /**
     * Compress the bundle root (set up in the config) into a tar.gz file. It will run only if
     * STATIC_PUBLISHING_GENERATE_TAR_GZ property is True. False by default.
     */
    protected void compressBundleIfNeeded() throws IOException {
        if (Config.getBooleanProperty("STATIC_PUBLISHING_GENERATE_TAR_GZ", false)) {
            final File bundleToCompress = BundlerUtil.getBundleRoot(config);
            final ArrayList<File> list = Lists.newArrayList(bundleToCompress);
            final File bundle = new File(
                    bundleToCompress + File.separator
                    + ".." + File.separator
                    + config.getId() + ".tar.gz");

            // Compressing the bundle.
            PushUtils.compressFiles(list, bundle, bundleToCompress.getAbsolutePath());
        }
    }

    /**
     * Reads the configuration properties from the specified static end-point.
     *
     * @param endpoint
     *            - The static {@link PublishingEndPoint}.
     * @return The {@link Properties} object containing the configuration
     *         parameters of the static end-point.
     * @throws DotDataException
     *             An error occurred when retrieving the end-point's properties
     *             or connecting to it.
     */
    protected Properties getEndPointProperties(final PublishingEndPoint endpoint)
            throws DotDataException {
        final String authToken = PublicEncryptionFactory
                .decryptString(endpoint.getAuthKey().toString());

        final Properties props = new Properties();
        try {
            props.load( new StringReader( authToken ) );
        } catch (IOException e) {
            throw new DotDataException(
                    "Can't read properties from Endpoint: " + endpoint.getAddress(), e);
        }

        return props;
    }

    /**
     * Get the context map for the bucket name interpolating
     * @param config {@link PublisherConfig}
     * @return Map
     */
    protected Map<String, Object> getContextMap(final String bucketProp,
            final PublisherConfig config) {

        final Map<String, Object> configMap;

        final Host host = (Host) config.get(CURRENT_HOST);
        final String languageId = (String)config.get(CURRENT_LANGUAGE);
        final Language language = APILocator.getLanguageAPI().getLanguage(languageId);

        if (null != host) {
            configMap  = new HashMap<>();
            configMap.put(HOST_ID,          host.getIdentifier());
            configMap.put(HOST_NAME,        host.getHostname());
            configMap.put(LANGUAGE_ISO,     language.toString());
            configMap.put(LANGUAGE_ID,      language.getId());
            configMap.put(LANGUAGE_COUNTRY, language.getCountry());

            // Timestamp variables: https://github.com/dotCMS/core/issues/10465
            final Date bucketDate = (Date) config.get(DOT_STATIC_DATE);
            if (bucketDate != null) {
                final SimpleDateFormat defaultDateFormat = new SimpleDateFormat(DATE_FORMAT_DEFAULT);
                configMap.put(DATE, defaultDateFormat.format(bucketDate));

                if (bucketProp != null) {
                    final List<RegExMatch> regExMatches = RegEX.find(bucketProp, "(\\{("+DATE+"-([^\\}]+))\\})");
                    for(final RegExMatch regExMatch : regExMatches) {
                        if (regExMatch.getMatch() != null
                                && regExMatch.getGroups().size() == GROUPS_SIZE) {

                            final String customDateVariableName = regExMatch.getGroups().get(1)
                                    .getMatch();
                            final String customDateFormatString = regExMatch.getGroups().get(2)
                                    .getMatch();

                            try {
                                final SimpleDateFormat customDateFormat = new SimpleDateFormat(
                                        customDateFormatString);

                                configMap.put(customDateVariableName,
                                        customDateFormat.format(bucketDate));
                            } catch (IllegalArgumentException e) {
                                Logger.debug(this.getClass(), "Could not parse date-pattern '"
                                        + customDateVariableName + "' in bucketId (" + e
                                        .getMessage() + ")", e);
                            }
                        }
                    }
                }
            }

            return new CompositeMap(config, configMap);
        }

        return config;
    } // getContextMap.

}
