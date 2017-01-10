package com.dotcms.publishing;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotcms.repackage.edu.emory.mathcs.backport.java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class Publisher implements IPublisher {

	protected PublisherConfig config;


	/**
	 * This method gets called before any publisher processes
	 * the config
	 */
	public PublisherConfig init(PublisherConfig config)throws DotPublishingException {

		this.config = config;

		return config;
	}

	abstract public PublisherConfig process(PublishStatus status) throws DotPublishingException ;



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
            String host = path.get(path.indexOf(config.getId())+2);

			return APILocator.getHostAPI().resolveHostName(host, APILocator.getUserAPI().getSystemUser(), true);
		}
		catch(Exception e){
			throw new DotPublishingException("error getting host:" + e.getMessage());
		}

	}

	public String getUriFromFilePath(File file) throws DotPublishingException{

		try{

			if(!file.getAbsolutePath().contains(config.getId())){
				throw new DotPublishingException("no bundle file found");
			}

			String fileSeparator = File.separator.equals("\\")?"\\\\":File.separator;
            List<String> path = Arrays.asList( file.getAbsolutePath().split( fileSeparator ) );
            path = path.subList(path.indexOf(config.getId())+4, path.size());
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


}
