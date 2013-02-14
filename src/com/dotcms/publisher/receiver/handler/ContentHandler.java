package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.bundler.ContentBundler;
import com.dotcms.publisher.pusher.bundler.HostBundler;
import com.dotcms.publisher.pusher.wrapper.ContentWrapper;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.services.PageServices;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;


public class ContentHandler implements IHandler {
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private UserAPI uAPI = APILocator.getUserAPI();
	private TagAPI tagAPI = APILocator.getTagAPI();
	private List<String> pagesToClear = new ArrayList<String>();
	private Map<String,Long> infoToRemove = new HashMap<String, Long>();
	
	@Override
	public String getName() {
		return this.getClass().getName();
	}
	
	@Override
	public void handle(File bundleFolder) throws Exception {
		handle(bundleFolder, false);
	}
	
	public void handle(File bundleFolder, Boolean isHost) throws Exception {
		List<File> contents = isHost?FileUtil.listFilesRecursively(bundleFolder, new HostBundler().getFileFilter()):
				FileUtil.listFilesRecursively(bundleFolder, new ContentBundler().getFileFilter());
		Collections.sort(contents);
		
		handleContents(contents, bundleFolder);
		
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
	}
	
	private void handleContents(Collection<File> contents, File folderOut) throws DotPublishingException, DotDataException{
		User systemUser = uAPI.getSystemUser();
		List<String> assetIds = new ArrayList<String>();
		
    	try{
	        XStream xstream=new XStream(new DomDriver());
	        ContentWrapper wrapper =null;
            for (File contentFile : contents) {
            	if(contentFile.isDirectory() ) continue;
            	 wrapper =
                        (ContentWrapper)
                                xstream.fromXML(new FileInputStream(contentFile));

            	Contentlet content = wrapper.getContent();
            	
            	
            	
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
            	
            	wrapper = (ContentWrapper)xstream.fromXML(new FileInputStream(contentFile));
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
	
	private void publish(Contentlet content, File folderOut, User userToUse, ContentWrapper wrapper)
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

                if(binaryFolder != null && binaryFolder.exists() && binaryFolder.listFiles().length > 0)
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
            
            Tree temp = TreeFactory.getTree(tree);
            if(temp == null || !UtilMethods.isSet(temp.getParent()))
            	TreeFactory.saveTree(tree);
        }
        
        // deletes multiTree entries to avoid duplicates
        for(Map<String, Object> mRow : wrapper.getMultiTree()) {
            Identifier parent1 = APILocator.getIdentifierAPI().find((String)mRow.get("parent1"));
            Identifier parent2 = APILocator.getIdentifierAPI().find((String)mRow.get("parent2"));
            Identifier child = APILocator.getIdentifierAPI().find((String)mRow.get("child"));

            MultiTree t = MultiTreeFactory.getMultiTree(parent1, parent2, child);
            if(t!=null && UtilMethods.isSet(t.getChild()))
            	MultiTreeFactory.deleteMultiTree(t);
        }
        
        // saves the muliTree entries 
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

    private void unpublish(Contentlet content, File folderOut, User userToUse, ContentWrapper wrapper)
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
            dc.setSQL( "delete from tree where parent = '" + contentlet.getInode() + "' or child = '" + contentlet.getIdentifier() + "'" );
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

}
