package com.dotcms.publisher.receiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.EndpointDetail;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublisherAPIImpl;
import com.dotcms.publisher.receiver.handler.ContainerHandler;
import com.dotcms.publisher.receiver.handler.ContentHandler;
import com.dotcms.publisher.receiver.handler.FolderHandler;
import com.dotcms.publisher.receiver.handler.HTMLPageHandler;
import com.dotcms.publisher.receiver.handler.HostHandler;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publisher.receiver.handler.LanguageHandler;
import com.dotcms.publisher.receiver.handler.LinkHandler;
import com.dotcms.publisher.receiver.handler.StructureHandler;
import com.dotcms.publisher.receiver.handler.TemplateHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.rest.BundlePublisherResource;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;

public class BundlePublisher extends Publisher {
    private PublishAuditAPI auditAPI = null;
    Map<String,Long> infoToRemove = new HashMap<String, Long>();
    //List<String> pagesToClear = new ArrayList<String>();
    List<String> assetIds = new ArrayList<String>();
    boolean bundleSuccess = true;
    
    private List<IHandler> handlers = new ArrayList<IHandler>();

    @Override
    public PublisherConfig init(PublisherConfig config) throws DotPublishingException {
        if(LicenseUtil.getLevel()<200)
            throw new RuntimeException("need an enterprise licence to run this");
        
        handlers = new ArrayList<IHandler>();
        //The order is really important
        handlers.add(new HostHandler());
        handlers.add(new FolderHandler());
        
        if(Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_STRUCTURES"))
        	handlers.add(new StructureHandler());

        handlers.add(new ContainerHandler());
        handlers.add(new TemplateHandler());
        handlers.add(new HTMLPageHandler());
        
        handlers.add(new ContentHandler());
        handlers.add(new LanguageHandler());
        handlers.add(new LinkHandler());
        
        auditAPI = PublishAuditAPI.getInstance();

        this.config = super.init(config);
        return this.config;
    }

    @Override
    public PublisherConfig process(final PublishStatus status) throws DotPublishingException {
        if(LicenseUtil.getLevel()<300)
            throw new RuntimeException("need an enterprise licence to run this");

        String bundleName = config.getId();
        String bundleFolder = bundleName.substring(0, bundleName.indexOf(".tar.gz"));
        String bundlePath = ConfigUtils.getBundlePath()+File.separator+BundlePublisherResource.MY_TEMP;//FIXME
        
      //Publish the bundle extracted
        PublishAuditHistory currentStatusHistory = null;
        EndpointDetail detail = new EndpointDetail();
        
        try{
        	//Update audit
        	 currentStatusHistory = auditAPI.getPublishAuditStatus(bundleFolder).getStatusPojo();
             
             currentStatusHistory.setPublishStart(new Date());
             detail.setStatus(PublishAuditStatus.Status.PUBLISHING_BUNDLE.getCode());
             detail.setInfo("Publishing bundle");
             currentStatusHistory.addOrUpdateEndpoint(config.getGroupId(), config.getEndpoint(), detail);

             auditAPI.updatePublishAuditStatus(bundleFolder,
                     PublishAuditStatus.Status.PUBLISHING_BUNDLE,
                     currentStatusHistory);
        }catch (Exception e) {
        	Logger.error(BundlePublisher.class,"Unable to update audit table : " + e.getMessage(),e);
		}
        

        File folderOut = new File(bundlePath+bundleFolder);
        folderOut.mkdir();

        // Extract file to a directory
        InputStream bundleIS = null;
        try {
            bundleIS = new FileInputStream(bundlePath+bundleName);
            untar(bundleIS,
                    folderOut.getAbsolutePath()+File.separator+bundleName,
                    bundleName);
        } catch (FileNotFoundException e) {
            throw new DotPublishingException("Cannot extract the selected archive", e);
        }
        
        
        try {
            HibernateUtil.startTransaction();
            
            //Execute the handlers
            for(IHandler handler : handlers )
            	handler.handle(folderOut);


            HibernateUtil.commitTransaction();
        } catch (Exception e) {
        	bundleSuccess = false;
            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e1) {
                Logger.error(PublisherAPIImpl.class,e.getMessage(),e1);
            }
            Logger.error(PublisherAPIImpl.class,e.getMessage(),e);

            //Update audit
            try {
                detail.setStatus(PublishAuditStatus.Status.FAILED_TO_PUBLISH.getCode());
                detail.setInfo("Failed to publish because an error occurred: "+e.getMessage());
                detail.setStackTrace(ExceptionUtils.getStackTrace(e));
                currentStatusHistory.addOrUpdateEndpoint(config.getGroupId(), config.getEndpoint(), detail);
                currentStatusHistory.setBundleEnd(new Date());

                auditAPI.updatePublishAuditStatus(bundleFolder,
                        PublishAuditStatus.Status.FAILED_TO_PUBLISH,
                        currentStatusHistory);

            } catch (DotPublisherException e1) {
                throw new DotPublishingException("Cannot update audit: ", e);
            }
            throw new DotPublishingException("Error Publishing: " +  e, e);
        }

        
        try{
		    //Update audit
		    detail.setStatus(PublishAuditStatus.Status.SUCCESS.getCode());
		    detail.setInfo("Everything ok");
		    currentStatusHistory.addOrUpdateEndpoint(config.getGroupId(), config.getEndpoint(), detail);
		    currentStatusHistory.setBundleEnd(new Date());
		    currentStatusHistory.setAssets(assetIds);
		    auditAPI.updatePublishAuditStatus(bundleFolder,
		            PublishAuditStatus.Status.SUCCESS, currentStatusHistory);
		    HibernateUtil.commitTransaction();
        }catch (Exception e) {
			Logger.error(BundlePublisher.class,"Unable to update audit table : " + e.getMessage(),e);
		}
        
        DbConnectionFactory.closeConnection();

        return config;
    }

    

    @SuppressWarnings("rawtypes")
    @Override
    public List<Class> getBundlers() {
        List<Class> list = new ArrayList<Class>();

        return list;
    }


    private void untar(InputStream bundle, String path, String fileName) {
        TarEntry entry;
        TarInputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            // get a stream to tar file
            InputStream gstream = new GZIPInputStream(bundle);
            inputStream = new TarInputStream(gstream);

            // For each entry in the tar, extract and save the entry to the file
            // system
            while (null != (entry = inputStream.getNextEntry())) {
                // for each entry to be extracted
                int bytesRead;

                String pathWithoutName = path.substring(0,
                        path.indexOf(fileName));

                // if the entry is a directory, create the directory
                if (entry.isDirectory()) {
                    File fileOrDir = new File(pathWithoutName + entry.getName());
                    fileOrDir.mkdir();
                    continue;
                }

                // write to file
                byte[] buf = new byte[1024];
                outputStream = new FileOutputStream(pathWithoutName
                        + entry.getName());
                while ((bytesRead = inputStream.read(buf, 0, 1024)) > -1)
                    outputStream.write(buf, 0, bytesRead);
                try {
                    if (null != outputStream)
                        outputStream.close();
                } catch (Exception e) {
                }
            }// while

        } catch (Exception e) {
            e.printStackTrace();
        } finally { // close your streams
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }
}


