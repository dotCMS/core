package com.dotcms.publisher.receiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
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
import com.dotcms.publisher.myTest.FolderWrapper;
import com.dotcms.publisher.myTest.PushContentWrapper;
import com.dotcms.publisher.myTest.PushPublisherConfig;
import com.dotcms.publisher.myTest.bundler.ContentBundler;
import com.dotcms.publisher.myTest.bundler.FolderBundler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.rest.BundlePublisherResource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.services.PageServices;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class BundlePublisher extends Publisher {
    private ContentletAPI conAPI = null;
    private User systemUser = null;
    private UserAPI uAPI = null;
    private TagAPI tagAPI = null;
    private PublishAuditAPI auditAPI = null;
    private FolderAPI fAPI = null;
    private IdentifierAPI iAPI = null;
    Map<String,Long> infoToRemove = new HashMap<String, Long>();
    List<String> pagesToClear = new ArrayList<String>();
    List<String> assetIds = new ArrayList<String>();
    boolean bundleSuccess = true;

    @Override
    public PublisherConfig init(PublisherConfig config) throws DotPublishingException {
        if(LicenseUtil.getLevel()<200)
            throw new RuntimeException("need an enterprise licence to run this");

        conAPI = APILocator.getContentletAPI();
        uAPI = APILocator.getUserAPI();
        tagAPI = APILocator.getTagAPI();
        auditAPI = PublishAuditAPI.getInstance();
        fAPI = APILocator.getFolderAPI();
        iAPI = APILocator.getIdentifierAPI();

        try {
            systemUser = uAPI.getSystemUser();
        } catch (DotDataException e) {
            Logger.fatal(this,e.getMessage(),e);
        }

        this.config = super.init(config);
        return this.config;
    }

    @Override
    public PublisherConfig process(final PublishStatus status) throws DotPublishingException {
        if(LicenseUtil.getLevel()<200)
            throw new RuntimeException("need an enterprise licence to run this");

        String bundleName = config.getId();
        String bundleFolder = bundleName.substring(0, bundleName.indexOf(".tar.gz"));
        String bundlePath = ConfigUtils.getBundlePath()+File.separator+BundlePublisherResource.MY_TEMP;//FIXME
        

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
        
        
        try {
            HibernateUtil.startTransaction();

            //For each content take the wrapper and save it on DB
            Collection<File> contents = FileUtil.listFilesRecursively(folderOut, new ContentBundler().getFileFilter());
            Collection<File> folders = FileUtil.listFilesRecursively(new File(folderOut + File.separator + "ROOT"), new FolderBundler().getFileFilter());
   
            
            
            handleFolders(folders);
            handleContents(contents, folderOut);
            
           
            

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
            for (String ident : infoToRemove.keySet()) {
                APILocator.getVersionableAPI().removeContentletVersionInfoFromCache(ident, infoToRemove.get(ident));
                Contentlet c = conAPI.findContentletByIdentifier(ident, false, infoToRemove.get(ident), APILocator.getUserAPI().getSystemUser(), true);
                APILocator.getContentletAPI().refresh(c);
            }
        }catch (Exception e) {
            throw new DotPublishingException("Unable to update Cache or Reindex Content", e);
        }
        
        try{
        	for (String pageIdent : pagesToClear) {
				HTMLPage page = new HTMLPage();
				page.setIdentifier(pageIdent);
				PageServices.removePageFile(page, true);
				PageServices.removePageFile(page, false);
			}
        }catch (Exception e) {
        	throw new DotPublishingException("Unable to update Cache or Reindex Content", e);
		}
  
        try {
            HibernateUtil.commitTransaction();
        } catch (DotHibernateException e) {
            Logger.error(BundlePublisher.class,e.getMessage(),e);
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

    private void publish(Contentlet content, File folderOut, User userToUse, PushContentWrapper wrapper)
            throws Exception
    {
        //Copy asset files to bundle folder keeping original folders structure
        List<Field> fields=FieldsCache.getFieldsByStructureInode(content.getStructureInode());

        String inode=content.getInode();
        for(Field ff : fields) {
            if(ff.getFieldType().toString().equals(Field.FieldType.BINARY.toString())) {

                String folderTree = inode.charAt(0)+File.separator+inode.charAt(1)+File.separator+
                        inode+File.separator+ff.getVelocityVarName();

                File binaryFolder = new File(folderOut+File.separator+"assets"+File.separator+folderTree);

                if(binaryFolder != null && binaryFolder.exists())
                    content.setBinary(ff.getVelocityVarName(), binaryFolder.listFiles()[0]);
            }

        }

        content = conAPI.checkin(content, userToUse, false);

        //First we need to remove the "old" trees in order to add this new ones
        cleanTrees( content );

        //Tree
        for(Map<String, Object> tRow : wrapper.getTree()) {
            Tree tree = new Tree();
            tree.setChild((String) tRow.get("child"));
            tree.setParent((String) tRow.get("parent"));
            tree.setRelationType((String) tRow.get("relation_type"));
            tree.setTreeOrder(Integer.parseInt(tRow.get("tree_order").toString()));

            TreeFactory.saveTree(tree);
        }
        
        //Multitree
        for(Map<String, Object> mRow : wrapper.getMultiTree()) {
            MultiTree multiTree = new MultiTree();
            multiTree.setChild((String) mRow.get("child"));
            multiTree.setParent1((String) mRow.get("parent1"));
            multiTree.setParent2((String) mRow.get("parent2"));
            multiTree.setRelationType((String) mRow.get("relation_type"));
            multiTree.setTreeOrder(Integer.parseInt( mRow.get("tree_order").toString()));


            MultiTreeFactory.saveMultiTree(multiTree);
        }

        //Tags
        for(Tag tag: wrapper.getTags()) {
            tagAPI.addTagInode(tag.getTagName(), content.getInode(), tag.getHostId());
        }
    }

    private void unpublish(Contentlet content, File folderOut, User userToUse, PushContentWrapper wrapper)
            throws Exception
    {
        String luceneQuery = "+identifier:"+content.getIdentifier()+" +live:true";
        List<Contentlet> contents =
                conAPI.searchByIdentifier(luceneQuery, 0, -1, null, userToUse, false);

        for (Contentlet contentlet : contents) {
            conAPI.unpublish(contentlet, userToUse, false);
        }

        conAPI.delete(content, userToUse, false, true);
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

    /**
     * Delete the Trees related to this given contentlet, this is in order to add the new published Trees (Relationships and categories)
     *
     * @param contentlet
     * @throws DotPublishingException
     */
    private void cleanTrees ( Contentlet contentlet ) throws DotPublishingException {

    	try{
    		DotConnect dc = new DotConnect();
    		dc.setSQL("select parent1 from multi_tree where child = ?");
    		dc.addObject(contentlet.getIdentifier());
    		List<Map<String,Object>> pages = dc.loadObjectResults();
    		for (Map<String, Object> row : pages) {
				pagesToClear.add(row.get("parent1").toString());
			}
    	}catch (Exception e) {
			Logger.error(this, e.getMessage(),e);
		}
    	
        try {
            DotConnect dc = new DotConnect();
            //Parent -> identifier  for relationships
            //Child  -> inode       for categories
            dc.setSQL( "delete from tree where parent = '" + contentlet.getIdentifier() + "' or child = '" + contentlet.getInode() + "'" );
            dc.loadResult();
        } catch ( Exception e ) {
            throw new DotPublishingException( "Unable to delete tree records for Contentlet.", e );
        }
        
        try {
            DotConnect dc = new DotConnect();
            //Parent -> identifier  for relationships
            //Child  -> inode       for categories
            dc.setSQL( "delete from multi_tree where child = '" + contentlet.getIdentifier() + "'" );
            dc.loadResult();
        } catch ( Exception e ) {
            throw new DotPublishingException( "Unable to delete multi_tree records for Contentlet.", e );
        }
    }

    private void handleFolders(Collection<File> folders) throws DotPublishingException{
    	try{
	        XStream xstream=new XStream(new DomDriver());
	        //Handle folders
	        for(File folderFile: folders) {
	        	if(folderFile.isDirectory()) continue;
	        	FolderWrapper folderWrapper = (FolderWrapper)  xstream.fromXML(new FileInputStream(folderFile));
	        	
	        	Folder folder = folderWrapper.getFolder();
	        	Identifier folderId = folderWrapper.getFolderId();
	        	Host host = folderWrapper.getHost();
	        	Identifier hostId = folderWrapper.getHostId();
	        	
	        	
	        	
	        	//Check Host if exists otherwise create
	        	Host localHost = APILocator.getHostAPI().findByName(host.getHostname(), systemUser, false);
        		
        		if(localHost == null) {
        			host.setProperty("_dont_validate_me", true);
        			
        			Identifier idNew = iAPI.createNew(host, APILocator.getHostAPI().findSystemHost(), hostId.getId());
        			host.setIdentifier(idNew.getId());
        			localHost = APILocator.getHostAPI().save(host, systemUser, false);
        		}
	        	
	        	//Loop over the folder
        		if(!UtilMethods.isSet(fAPI.findFolderByPath(folderId.getPath(), localHost, systemUser, false).getInode())) {
        			Identifier id = iAPI.find(folder.getIdentifier());
        			if(id ==null || !UtilMethods.isSet(id.getId())){
        				Identifier folderIdNew = null;
        				if(folderId.getParentPath().equals("/")) {
	            			folderIdNew = iAPI.createNew(folder, 
	            					localHost, 
	            					folderId.getId());
        				} else {
        					folderIdNew = iAPI.createNew(folder, 
                					fAPI.findFolderByPath(folderId.getParentPath(), localHost, systemUser, false), 
                					folderId.getId());
        				}
            			folder.setIdentifier(folderIdNew.getId());
            		}
        			fAPI.save(folder, folder.getInode(), systemUser, false);
        		}
        			
	        }
        	
    	}
    	catch(Exception e){
    		throw new DotPublishingException(e.getMessage(),e);
    	}
    	
    	
    	
    }
    
    
    
    private void handleContents(Collection<File> contents, File folderOut) throws DotPublishingException{
    	try{
	        XStream xstream=new XStream(new DomDriver());
	        PushContentWrapper wrapper =null;
            for (File contentFile : contents) {
            	if(contentFile.isDirectory() ) continue;
            	 wrapper =
                        (PushContentWrapper)
                                xstream.fromXML(new FileInputStream(contentFile));

            	Contentlet content = wrapper.getContent();
            	
            	
            	
                content = wrapper.getContent();
                content.setProperty("_dont_validate_me", true);
                content.setProperty(Contentlet.WORKFLOW_ASSIGN_KEY, null);
                content.setProperty(Contentlet.WORKFLOW_ACTION_KEY, null);
                content.setProperty(Contentlet.WORKFLOW_COMMENTS_KEY, null);

                //Select user
                User userToUse = null;
                try {
                    userToUse = uAPI.loadUserById(content.getModUser());
                } catch(NoSuchUserException e) {
                    userToUse = systemUser;
                }

                assetIds.add(content.getIdentifier());
                
                if(wrapper.getOperation().equals(PushPublisherConfig.Operation.PUBLISH)) {
                    publish(content, folderOut, userToUse, wrapper);
                } else {
                    unpublish(content, folderOut, userToUse, wrapper);
                }
            }
            
            for (File contentFile : contents) {
            	if(contentFile.isDirectory() ) continue;
            	
            	wrapper = (PushContentWrapper)xstream.fromXML(new FileInputStream(contentFile));
                if(wrapper.getOperation().equals(PushPublisherConfig.Operation.PUBLISH)) {
	                ContentletVersionInfo info = wrapper.getInfo();
	                infoToRemove.put(info.getIdentifier(), info.getLang());
	                APILocator.getVersionableAPI().saveContentletVersionInfo(info);
                }
            }
        	
    	}
    	catch(Exception e){
    		throw new DotPublishingException(e.getMessage(),e);
    	}
    	
    	
    	
    }

}


