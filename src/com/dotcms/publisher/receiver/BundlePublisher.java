package com.dotcms.publisher.receiver;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.business.*;
import com.dotcms.publisher.myTest.PushContentWrapper;
import com.dotcms.publisher.myTest.PushPublisherConfig;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.rest.BundlePublisherResource;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
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
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class BundlePublisher extends Publisher {
    private ContentletAPI conAPI = null;
    private User systemUser = null;
    private UserAPI uAPI = null;
    private TagAPI tagAPI = null;
    private PublishAuditAPI auditAPI = null;

    @Override
    public PublisherConfig init(PublisherConfig config) throws DotPublishingException {
        if(LicenseUtil.getLevel()<200)
            throw new RuntimeException("need an enterprise licence to run this");

        conAPI = APILocator.getContentletAPI();
        uAPI = APILocator.getUserAPI();
        tagAPI = APILocator.getTagAPI();
        auditAPI = PublishAuditAPI.getInstance();

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
        Map<String,Long> infoToRemove = new HashMap<String, Long>();

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
        PushContentWrapper wrapper = null;
        Contentlet content = null;
        PublishAuditHistory currentStatusHistory = null;
        EndpointDetail detail = new EndpointDetail();
        try {
            HibernateUtil.startTransaction();


            //Update audit
            currentStatusHistory =
                    PublishAuditHistory.getObjectFromString(
                            (String)auditAPI.getPublishAuditStatus(
                                    bundleFolder).get("status_pojo"));
            currentStatusHistory.setPublishStart(new Date());
            detail.setStatus(PublishAuditStatus.Status.PUBLISHING_BUNDLE.getCode());
            detail.setInfo("Publishing bundle");
            currentStatusHistory.addOrUpdateEndpoint(config.getEndpoint(), detail);

            auditAPI.updatePublishAuditStatus(bundleFolder,
                    PublishAuditStatus.Status.PUBLISHING_BUNDLE,
                    currentStatusHistory);

            //For each content take the wrapper and save it on DB
            Collection<File> contentFiles = FileUtils.listFiles(folderOut, new String[]{"content"}, true);
            XStream xstream=new XStream(new DomDriver());

            for (File contentFile : contentFiles) {

                wrapper =
                        (PushContentWrapper)
                                xstream.fromXML(new FileInputStream(contentFile));

                content = wrapper.getContent();
                content.setProperty("_dont_validate_me", true);
                content.setProperty(Contentlet.WORKFLOW_ASSIGN_KEY, null);
                content.setProperty(Contentlet.WORKFLOW_ACTION_KEY, null);
                content.setProperty(Contentlet.WORKFLOW_COMMENTS_KEY, null);
                ContentletVersionInfo info = wrapper.getInfo();
                infoToRemove.put(info.getIdentifier(), info.getLang());

                //Select user
                User userToUse = null;
                try {
                    userToUse = uAPI.loadUserById(content.getModUser());
                } catch(NoSuchUserException e) {
                    userToUse = systemUser;
                }

                if(wrapper.getOperation().equals(PushPublisherConfig.Operation.PUBLISH)) {
                    publish(content, info, folderOut, userToUse, wrapper);
                } else {
                    unpublish(content, folderOut, userToUse, wrapper);
                }
            }

            //Update audit
            detail.setStatus(PublishAuditStatus.Status.SUCCESS.getCode());
            detail.setInfo("Everything ok");
            currentStatusHistory.addOrUpdateEndpoint(config.getEndpoint(), detail);
            currentStatusHistory.setBundleEnd(new Date());
            auditAPI.updatePublishAuditStatus(bundleFolder,
                    PublishAuditStatus.Status.SUCCESS, currentStatusHistory);

            HibernateUtil.commitTransaction();
        } catch (Exception e) {
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
                currentStatusHistory.addOrUpdateEndpoint(config.getEndpoint(), detail);
                currentStatusHistory.setBundleEnd(new Date());

                auditAPI.updatePublishAuditStatus(bundleFolder,
                        PublishAuditStatus.Status.FAILED_TO_PUBLISH,
                        currentStatusHistory);

            } catch (DotPublisherException e1) {
                throw new DotPublishingException("Cannot check in the content with inode: " +
                        content.getInode(), e);
            }
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
        try {
            HibernateUtil.commitTransaction();
        } catch (DotHibernateException e) {
            Logger.error(BundlePublisher.class,e.getMessage(),e);
        }
        DbConnectionFactory.closeConnection();

        return config;
    }

    private void publish(Contentlet content, ContentletVersionInfo info, File folderOut, User userToUse, PushContentWrapper wrapper)
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
        APILocator.getVersionableAPI().saveContentletVersionInfo(info);

        //First we need to remove the "old" trees in order to add this new ones
        cleanTrees( content );

        //Tree
        for(Map<String, Object> tRow : wrapper.getTree()) {
            Tree tree = new Tree();
            tree.setChild((String) tRow.get("child"));
            tree.setParent((String) tRow.get("parent"));
            tree.setRelationType((String) tRow.get("relation_type"));
            tree.setTreeOrder((Integer) tRow.get("tree_order"));

            TreeFactory.saveTree(tree);
        }

        //Multitree
        for(Map<String, Object> mRow : wrapper.getMultiTree()) {
            MultiTree multiTree = new MultiTree();
            multiTree.setChild((String) mRow.get("child"));
            multiTree.setParent1((String) mRow.get("parent1"));
            multiTree.setParent2((String) mRow.get("parent2"));
            multiTree.setRelationType((String) mRow.get("relation_type"));
            multiTree.setTreeOrder((Integer) mRow.get("tree_order"));


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

        try {
            DotConnect dc = new DotConnect();
            //Parent -> identifier  for relationships
            //Child  -> inode       for categories
            dc.setSQL( "delete from tree where parent = '" + contentlet.getIdentifier() + "' or child = '" + contentlet.getInode() + "'" );
            dc.loadResult();
        } catch ( Exception e ) {
            throw new DotPublishingException( "Unable delete trees for Contentlet.", e );
        }
    }



//	BufferedInputStream buffIS = new BufferedInputStream(inputStream);
//    BufferedOutputStream buffOS = new BufferedOutputStream(outputStream);
//
//    try {
//        IOUtils.copy(buffIS, buffOS);
//    } finally {
//    	buffOS.close();
//    	buffIS.close();
//    }

}


