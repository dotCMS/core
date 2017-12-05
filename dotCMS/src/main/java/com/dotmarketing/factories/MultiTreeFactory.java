package com.dotmarketing.factories;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.services.PageServices;
import com.dotmarketing.util.Logger;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

/**
 * This class provides utility routines to interact with the Multi-Tree
 * structures in the system. A Multi-Tree represents the relationship between a
 * Legacy or Content Page, a container, and a contentlet.
 * <p>
 * Therefore, the content of a page can be described as the sum of several
 * Multi-Tree records which represent each piece of information contained in it.
 * </p>
 * 
 * @author will
 */
public class MultiTreeFactory {
    
    private static final String DELETE_MULTITREE_ERROR_MSG = "Deleting MultiTree Object failed:";



    
    /**
     * Deletes multi-tree relationship given a MultiTree object.
     * It also updates the version_ts of all versions of the htmlpage passed in (multiTree.parent1)
     *
     * @param multiTree
     * @throws DotDataException
     * @throws DotSecurityException 
     *            
     */
	public static void deleteMultiTree(final MultiTree multiTree) throws DotDataException {
	    try {
	        
	        StringBuilder sql = new StringBuilder("delete from multi_tree where parent1=? and parent2=? and child=? and (relation_type=?");
            if(null == multiTree.getRelationType()) {
               sql.append(" or relation_type is null");
            }
	        sql.append(")");
	        
           new DotConnect()
           .setSQL(sql.toString())
           .addParam(multiTree.getParent1())
           .addParam(multiTree.getParent2())
           .addParam(multiTree.getChild())
           .addParam(multiTree.getRelationType())
           .loadResult();
	        
	        updateHTMLPageVersionTS(multiTree.getParent1());
	        refreshPageInCache(multiTree.getParent1());
   
        } catch (DotDataException e) {
            Logger.error(MultiTreeFactory.class, DELETE_MULTITREE_ERROR_MSG + e, e);
            throw new DotDataException(e.getMessage());
        } 
    	}
	
	
    public static MultiTree getMultiTree(Identifier htmlPage, Identifier container, Identifier childContent) {
        return getMultiTree(htmlPage, container, childContent, null);
    }


	public static MultiTree getMultiTree(Identifier parent1, Identifier parent2, Identifier child, String relationType) {
		try {
            StringBuilder sql = new StringBuilder("select * from multi_tree where parent1 = ? and parent2 = ? and child = ? and ( relation_type=?");
            
            if(null == relationType) {
               sql.append(" or relation_type is null");
            }
            sql.append(")");
            
		    DotConnect db= new DotConnect()
    			.setSQL(sql.toString())
    			.addParam(parent1.getId())
    			.addParam(parent2.getId())
    			.addParam(child.getId())
    			.addParam(relationType);

            List<Map<String, Object>> l = db.loadObjectResults();
            if(!l.isEmpty()) {
                return dbToMultiTree(l.get(0));
            }
		} catch (Exception e) {
            Logger.warn(MultiTreeFactory.class, "getMultiTree failed:" + e, e);
		}
		return new MultiTree();
	}
    

	public static java.util.List<MultiTree> getMultiTree(Inode parent) {
        return getMultiTree(parent.getInode());
	}
	
	public static java.util.List<MultiTree> getMultiTree(Identifier parent) {
	    return getMultiTree(parent.getId());
	}


	public static java.util.List<MultiTree> getMultiTree(String parentInode) {
		try {
            DotConnect db= new DotConnect()
            .setSQL("select * from multi_tree where parent1 = ? or parent2 = ? ")
            .addParam(parentInode)
            .addParam(parentInode);

            return dbToMultiTree(db.loadObjectResults());
            
		} catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getMultiTree failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}

	public static java.util.List<MultiTree> getMultiTree(IHTMLPage htmlPage, Container container) {
		try {
            DotConnect db= new DotConnect()
            .setSQL("select * from multi_tree where parent1 = ? and parent2 = ?  ")
            .addParam(htmlPage.getIdentifier())
            .addParam(container.getIdentifier());

            return dbToMultiTree(db.loadObjectResults());
		} catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getMultiTree failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}

	public static java.util.List<MultiTree> getContainerMultiTree(String containerIdentifier) {
	    return getMultiTree(containerIdentifier);
	}
	
	public static java.util.List<MultiTree> getMultiTreeByChild(String contentIdentifier) {
        try {
            DotConnect db= new DotConnect()
            .setSQL("select * from multi_tree where child = ?   ")
            .addParam(contentIdentifier);

            return dbToMultiTree(db.loadObjectResults());
        } catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getMultiTree failed:" + e, e);
            throw new DotRuntimeException(e.toString());
        }
	}

	/**
	 * Saves a multi-tree construct using the default language in the system. A
	 * muti-tree is usually composed of the following five parts:
	 * <ol>
	 * <li>The identifier of the Content Page.</li>
	 * <li>The identifier of the container in the page.</li>
	 * <li>The identifier of the contentlet itself.</li>
	 * <li>The type of content relation.</li>
	 * <li>The order in which this construct is added to the database.</li>
	 * </ol>
	 * 
	 * @param o
	 *            - The multi-tree structure.
	 * @throws DotSecurityException 
	 */
	public static void saveMultiTree(MultiTree o)  {
	     try {
            deleteMultiTree(o);
            insertMultiTree(o);
        } catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getMultiTree failed:" + e, e);
            throw new DotRuntimeException(e.toString());
        }
	}
	
   public static void insertMultiTree(MultiTree o) throws DotDataException  {
       new DotConnect()
       .setSQL("insert into multi_tree (parent1, parent2, child, relation_type, tree_order ) values (?,?,?,?,?)  ")
       .addParam(o.getParent1())
       .addParam(o.getParent2())
       .addParam(o.getChild())
       .addParam(o.getRelationType())
       .addParam(o.getTreeOrder())
       .loadResult();
   }

	/**
	 * MultiTree is not multi-lingual
	 * @param o
	 * @param languageIds
	 * @throws DotSecurityException
	 */
	@Deprecated
	public static void saveMultiTree(MultiTree o, long languageIds)  {
	    saveMultiTree(o);
	}
	

  
	
    /**
     * Update the version_ts of all versions of the HTML Page with the given id.
     * If a MultiTree Object has been added or deleted from this page,
     * its version_ts value needs to be updated so it can be included
     * in future Push Publishing tasks
     * 
     * @param id The HTMLPage Identifier to pass in 
     * @throws DotContentletStateException
     * @throws DotDataException 
     * @throws DotSecurityException 
     *            
     */
	private static void updateHTMLPageVersionTS(String id) throws DotDataException {
	  List<ContentletVersionInfo> infos = APILocator.getVersionableAPI().findContentletVersionInfos(id);
		for (ContentletVersionInfo versionInfo : infos) {
			if(versionInfo!=null) {
				versionInfo.setVersionTs(new Date());
				APILocator.getVersionableAPI().saveContentletVersionInfo(versionInfo);
			}
		}
	}
	
    /**
     * Refresh cached objects for all versions of the HTMLPage with the given pageIdentifier.
     * 
     * @param pageIdentifier The HTMLPage Identifier to pass in
     * @throws DotContentletStateException
     * @throws DotDataException 
     *            
     */
    private static void refreshPageInCache(String pageIdentifier) throws DotDataException {
        Set<String> inodes = new HashSet<String>();
        List<ContentletVersionInfo> infos = APILocator.getVersionableAPI().findContentletVersionInfos(pageIdentifier);
        for (ContentletVersionInfo versionInfo : infos) {
            inodes.add(versionInfo.getWorkingInode());
            if(versionInfo.getLiveInode() != null){
              inodes.add(versionInfo.getLiveInode());
            }
        }
        try {
            List<Contentlet> contentlets = APILocator.getContentletAPIImpl().findContentlets(Lists.newArrayList(inodes));
    		    for (Contentlet pageContent : contentlets) {
    		        IHTMLPage htmlPage = APILocator.getHTMLPageAssetAPI().fromContentlet(pageContent);
    		        PageServices.invalidateAll(htmlPage);
    		    }
        } catch (DotStateException | DotSecurityException e) {
            Logger.warn(MultiTreeFactory.class,"unable to refresh page cache:" + e.getMessage());
        }
    }
    
    
    public static MultiTree dbToMultiTree(Map<String, Object> row) {
        final String relationType = (row.get("relation_type") !=null) ? String.valueOf(row.get("relation_type")) : null;
        final String parent1 = (row.get("parent1") !=null) ? String.valueOf(row.get("parent1")) : null;
        final String parent2 = (row.get("parent2") !=null) ? String.valueOf(row.get("parent2")) : null;
        final String child = (row.get("child") !=null) ? String.valueOf(row.get("child")) : null;
        final int order = (row.get("tree_order") !=null) ? Integer.valueOf(row.get("tree_order").toString()) : 0;
        return new MultiTree(parent1, parent2, child, relationType, order );
        
        
    }
    
    public static List<MultiTree> dbToMultiTree(List<Map<String, Object>> dbRows) {
        return (List<MultiTree>) dbRows
                .stream()
                .map(row -> dbToMultiTree(row))
                .collect(Collectors.toList());
    }
    
	
}
