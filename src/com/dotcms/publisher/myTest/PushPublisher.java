package com.dotcms.publisher.myTest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;

public class PushPublisher extends Publisher {

	public static final String SITE_SEARCH_INDEX = "SITE_SEARCH_INDEX";
	private PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
	private PublisherAPI pubAPI = PublisherAPI.getInstance();

	@Override
	public PublisherConfig init(PublisherConfig config) throws DotPublishingException {
	    if(LicenseUtil.getLevel()<200)
            throw new RuntimeException("need an enterprise licence to run this");
	    
		this.config = super.init(config);
		PushPublisherConfig myConf = (PushPublisherConfig) config;

		// if we don't specify an index, use the current one
		if (myConf.getIndexName() == null) {
			try {
				String index = APILocator.getIndiciesAPI().loadIndicies().working;
				if (index != null) {
					myConf.setIndexName(index);
					this.config = myConf;
				} else {
					throw new DotPublishingException("Active Site Search Index:null");
				}
			} catch (Exception e) {
				throw new DotPublishingException(
						"You must either specify a valid site search index in your PublishingConfig or have current active site search index.  " +
						"Make sure you have a current active site search index by going to the site search admin portlet and creating one");
			}

		}

		return this.config;

	}

	@Override
	public PublisherConfig process(final PublishStatus status) throws DotPublishingException {
	    if(LicenseUtil.getLevel()<200)
            throw new RuntimeException("need an enterprise licence to run this");
	    
	    PublishAuditHistory currentStatusHistory = null;
		try {
			//Updating audit table
			currentStatusHistory =
					PublishAuditHistory.getObjectFromString(
							(String)pubAuditAPI.getPublishAuditStatus(
									config.getId()).get("status_pojo"));
			currentStatusHistory.setPublishStart(new Date());
			pubAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.PUBLISHING, currentStatusHistory);
			
			
			//Compressing bundle
			File bundleRoot = BundlerUtil.getBundleRoot(config);

			ArrayList<File> list = new ArrayList<File>(1);
			list.add(bundleRoot);
			compressFiles(list, new File(bundleRoot+File.separator+".."+File.separator+config.getId()), bundleRoot.getAbsolutePath());
			
			
			//Updating audit table
			currentStatusHistory =
					PublishAuditHistory.getObjectFromString(
							(String)pubAuditAPI.getPublishAuditStatus(
									config.getId()).get("status_pojo"));
			currentStatusHistory.setPublishEnd(new Date());
			pubAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.SUCCESS, currentStatusHistory);
			
			//Deleting queue records
			pubAPI.deleteElementsFromPublishQueueTable(config.getId());

			return config;

		} catch (Exception e) {
			//Updating audit table
			try {
				pubAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.FAILED_TO_PUBLISH, currentStatusHistory);
			} catch (DotPublisherException e1) {
				throw new DotPublishingException(e.getMessage());
			}
			
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotPublishingException(e.getMessage());

		}
	}
	
	

	/**
	 * Compress (tar.gz) the input files to the output file
	 *
	 * @param files The files to compress
	 * @param output The resulting output file (should end in .tar.gz)
	 * @throws IOException
	 */
	private void compressFiles(Collection<File> files, File output, String bundleRoot)
		throws IOException
	{
		Logger.info(this.getClass(), "Compressing "+files.size() + " to "+output.getAbsoluteFile());
	               // Create the output stream for the output file
		FileOutputStream fos = new FileOutputStream(output);
	               // Wrap the output file stream in streams that will tar and gzip everything
		TarArchiveOutputStream taos = new TarArchiveOutputStream(
			new GZIPOutputStream(new BufferedOutputStream(fos)));

	               // TAR originally didn't support long file names, so enable the support for it
		taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

	               // Get to putting all the files in the compressed output file
		for (File f : files) {
			addFilesToCompression(taos, f, ".", bundleRoot);
		}

	               // Close everything up
		taos.close();
		fos.close();
	}

	/**
	 * Does the work of compression and going recursive for nested directories
	 * <p/>
	 *
	 * Borrowed heavily from http://www.thoughtspark.org/node/53
	 *
	 * @param taos The archive
	 * @param file The file to add to the archive
	        * @param dir The directory that should serve as the parent directory in the archivew
	 * @throws IOException
	 */
	private void addFilesToCompression(TarArchiveOutputStream taos, File file, String dir, String bundleRoot)
		throws IOException
	{
	    
			// Create an entry for the file
			taos.putArchiveEntry(new TarArchiveEntry(file, dir+File.separator+file.getName()));
			if (file.isFile()) {
		        // Add the file to the archive
				BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
				IOUtils.copy(new FileInputStream(file), taos);
				taos.closeArchiveEntry();
				bis.close();
			} else if (file.isDirectory()) {
				//Logger.info(this.getClass(),file.getPath().substring(bundleRoot.length()));
		         // close the archive entry
				taos.closeArchiveEntry();
		         // go through all the files in the directory and using recursion, add them to the archive
				for (File childFile : file.listFiles()) {
					addFilesToCompression(taos, childFile, file.getPath().substring(bundleRoot.length()), bundleRoot);
				}
			}
	    
	}

	
	@SuppressWarnings("rawtypes")
	@Override
	public List<Class> getBundlers() {
		List<Class> list = new ArrayList<Class>();

		list.add(PushPublisherBundler.class);
		return list;
	}

}
