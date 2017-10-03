package com.dotmarketing.factories;

import java.sql.SQLException;
import java.util.Date;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.services.PageServices;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;

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
    private static final String SAVE_MULTITREE_ERROR_MSG = "Saving MultiTree Object failed:";

	public static void deleteMultiTree(Object o1, Object o2, Object o3) {
		Inode inode1 = (Inode) o1;
		Inode inode2 = (Inode) o2;
		Inode inode3 = (Inode) o3;

		try {


			DotConnect db = new DotConnect();
			db.setSQL("delete from multi_tree where parent1 =? and parent2 = ? and child = ? ");
			db.addParam(inode1.getInode());
			db.addParam(inode2.getInode());
			db.addParam(inode3.getInode());
			db.getResult();
			
			updateVersionTs(inode1.getInode());
			refreshPageInCache(inode1.getInode());
			
		}
		catch (Exception e) {
			throw new DotRuntimeException(e.getMessage());
		}

	}

    /**
     * Just invoking deleteMultiTreeByParent1 with null language.
     * @throws DotSecurityException 
     * 
     * @see MultiTreeFactory#deleteMultiTreeByParent1(Identifier parent, Long
     *      languageId)
     */
    public static void deleteMultiTreeByParent1(Contentlet contentlet) throws DotDataException, DotSecurityException {
        Identifier identifier = new Identifier();
        identifier.setId(contentlet.getIdentifier());

        deleteMultiTreeByParent1(identifier, contentlet.getLanguageId());
    }

    /**
     * Just invoking deleteMultiTreeByParent1 with null language.
     * @throws DotSecurityException 
     * 
     * @see MultiTreeFactory#deleteMultiTreeByParent1(Identifier parent, Long
     *      languageId)
     */
    public static void deleteMultiTreeByParent1(Identifier parent) throws DotDataException, DotSecurityException {
        deleteMultiTreeByParent1(parent, null);
    }

    /**
     * Just invoking deleteMultiTreeByParent1 and create an identifier method
     * with the contentlet identifier id
     * @throws DotSecurityException 
     * 
     * @see MultiTreeFactory#deleteMultiTreeByParent1(Identifier parent, Long
     *      languageId)
     */
    public static void deleteMultiTreeByParent1(Contentlet contentlet, Long languageId)
            throws DotDataException, DotSecurityException {
        Identifier identifier = new Identifier();
        identifier.setId(contentlet.getIdentifier());

        deleteMultiTreeByParent1(identifier, languageId);
    }

    /**
     * Deletes multi-tree relationships by identifier and language.
     * <p>
     * This method will remove all rows where parent1 equals to identifier
     * parameter where the identifier has same language as languageId parameter.
     * </p>
     * <p>
     * NOTE: This delete assumes that we are only using identifiers in
     * multi-tree table
     * </p>
     * 
     * @param parent
     *            identifier
     * @param languageId
     *            (optional parameter) if null it will delete all rows where
     *            parent1 equals identifier same as
     *            {@link #deleteMultiTreeFromContentletPage(String)}; otherwise
     *            it will search for those identifiers that match with the
     *            language and delete them from multi-tree table
     * @throws DotDataException
     * @throws DotSecurityException 
     * 
     * @see MultiTreeFactory#updateVersionTs(String, Long)
     * @see MultiTreeFactory#refreshPageInCache(String, Long)
     */
    public static void deleteMultiTreeByParent1(Identifier parent, Long languageId)
            throws DotDataException, DotSecurityException {
        DotConnect db = new DotConnect();

        try {
            if (languageId == null) {
                db.executeStatement("DELETE FROM multi_tree WHERE parent1 = '" + parent.getId()
                        + "';");
                updateVersionTs(parent.getId());
                refreshPageInCache(parent.getId());
                return;
            }

            // -- Query example --
            // DELETE FROM multi_tree m
            // WHERE m.parent1 = '6c2a7d84-a16d-4c12-8830-9d203c3dcc4c' AND
            // m.child IN (SELECT c.identifier FROM multi_tree AS m1
            // INNER JOIN contentlet_version_info AS c
            // ON m1.child = c.identifier
            // WHERE m1.parent1 = '6c2a7d84-a16d-4c12-8830-9d203c3dcc4c' AND
            // c.lang = 2);
            // -- End of query example --
            StringBuilder query = new StringBuilder("DELETE FROM multi_tree m WHERE ")
                    .append("m.parent1 = '").append(parent.getId()).append("' ")
                    .append("AND m.child IN (SELECT c.identifier FROM multi_tree AS m1 ")
                    .append("INNER JOIN contentlet_version_info AS c ON m1.child = c.identifier ")
                    .append("WHERE m1.parent1 = '").append(parent.getId()).append("' ")
                    .append("AND c.lang = ").append(languageId).append(");");

            db.executeStatement(query.toString());
            
            updateVersionTs(parent.getId(), languageId);
            refreshPageInCache(parent.getId(), languageId);
            
        } catch (SQLException e) {
            throw new DotDataException(DELETE_MULTITREE_ERROR_MSG, e);
        } catch (DotContentletStateException e) {
            throw new DotContentletStateException(DELETE_MULTITREE_ERROR_MSG, e);
        } catch (DotSecurityException e) {
            throw new DotSecurityException(DELETE_MULTITREE_ERROR_MSG, e);
        }
    }
    
    /**
     * Deletes multi-tree relationships given a MultiTree object.
     * 
     * @param multiTree
     * @throws DotDataException 
     * @throws DotSecurityException 
     *            
     */
    public static void deleteMultiTree (MultiTree o) throws DotDataException, DotSecurityException {
        deleteMultiTree(o, APILocator.getLanguageAPI().getDefaultLanguage()
                .getId());
    }

    /**
     * Deletes multi-tree relationships given a MultiTree object and a Language Id.
     * A language id is passed in so cleanup of cached resources of parent Page content
     * Needs to be cleaned, along with update its version_ts value
     * 
     * @param multiTree
     * @param languageId 
     * @throws DotDataException 
     * @throws DotSecurityException 
     *            
     */
	public static void deleteMultiTree(MultiTree o, Long languageId) throws DotDataException, DotSecurityException {
	    try {
	        String id = o.getParent1();
	        HibernateUtil.delete(o);
	        updateVersionTs(id, languageId);
	        refreshPageInCache(id,languageId);
	        return;	        
	    } catch (DotHibernateException e) {
	        Logger.error(MultiTreeFactory.class, DELETE_MULTITREE_ERROR_MSG + e, e);
	        throw new DotRuntimeException(e.getMessage());
	    } catch (DotStateException e) {
	        Logger.error(MultiTreeFactory.class, DELETE_MULTITREE_ERROR_MSG + e, e);
	        throw new DotStateException(e.getMessage());
        } catch (DotDataException e) {
            Logger.error(MultiTreeFactory.class, DELETE_MULTITREE_ERROR_MSG + e, e);
            throw new DotDataException(e.getMessage());
        } catch (DotSecurityException e) {
            Logger.error(MultiTreeFactory.class, DELETE_MULTITREE_ERROR_MSG + e, e);
            throw new DotSecurityException(e.getMessage());
        }
	}
	
	public static boolean existsMultiTree(Object o1, Object o2, Object o3) {
	    Inode inode1 = (Inode) o1;
	    Inode inode2 = (Inode) o2;
	    Inode inode3 = (Inode) o3;

	    try {

	        DotConnect db = new DotConnect();
	        db.setSQL("select count(*) mycount from multi_tree where parent1 =? and parent2 = ? and child = ? ");
	        db.addParam(inode1.getInode());
	        db.addParam(inode2.getInode());
	        db.addParam(inode3.getInode());

	        int count = db.getInt("mycount");

	        return (count > 0);
	    }
	    catch (Exception e) {
	        throw new DotRuntimeException(e.getMessage());
	    }
	}

	public static MultiTree getMultiTree(Identifier parent1, Identifier parent2, Identifier child) {
		try {
			HibernateUtil dh = new HibernateUtil(MultiTree.class);
			dh.setQuery("from multi_tree in class com.dotmarketing.beans.MultiTree where parent1 = ? and parent2 = ? and child = ?");
			dh.setParam(parent1.getInode());
			dh.setParam(parent2.getInode());
			dh.setParam(child.getInode());

			return (MultiTree) dh.load();
		} catch (Exception e) {
            Logger.warn(MultiTreeFactory.class, "getMultiTree failed:" + e, e);
		}
		return new MultiTree();
	}
    
	@SuppressWarnings("unchecked")
	public static java.util.List<MultiTree> getMultiTree(Inode parent) {
		try {
			HibernateUtil dh = new HibernateUtil(MultiTree.class);
			dh.setQuery("from multi_tree in class com.dotmarketing.beans.MultiTree where parent1 = ? or parent2 = ? ");
			dh.setParam(parent.getInode());
			dh.setParam(parent.getInode());

			return dh.list();
            
		} catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getMultiTree failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}
	
	public static java.util.List<MultiTree> getMultiTree(Identifier parent) {
		try {
			HibernateUtil dh = new HibernateUtil(MultiTree.class);
			dh.setQuery("from multi_tree in class com.dotmarketing.beans.MultiTree where parent1 = ? or parent2 = ? ");
			dh.setParam(parent.getInode());
			dh.setParam(parent.getInode());

			return dh.list();
            
		} catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getMultiTree failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}

	@SuppressWarnings("unchecked")
	public static java.util.List<MultiTree> getMultiTree(String parentInode) {
		try {
			HibernateUtil dh = new HibernateUtil(MultiTree.class);
			dh.setQuery("from multi_tree in class com.dotmarketing.beans.MultiTree where parent1 = ? or parent2 = ? ");
			dh.setParam(parentInode);
			dh.setParam(parentInode);

			return dh.list();
            
		} catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getMultiTree failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}
	@SuppressWarnings("unchecked")
	public static java.util.List<MultiTree> getMultiTree(IHTMLPage htmlPage, Container container) {
		try {
			HibernateUtil dh = new HibernateUtil(MultiTree.class);
			dh.setQuery("from multi_tree in class com.dotmarketing.beans.MultiTree where parent1 = ? and parent2 = ? ");
			dh.setParam(htmlPage.getIdentifier());
			dh.setParam(container.getIdentifier());

			return dh.list();
            
		} catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getMultiTree failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}
	/**
	 * Get the multi_tree by both parents given a containerId
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * Jun 26, 2013 - 12:34:29 PM
	 */
	@SuppressWarnings("unchecked")
	public static java.util.List<MultiTree> getContainerMultiTree(String containerIdentifier) {
		try {
			HibernateUtil dh = new HibernateUtil(MultiTree.class);
			dh.setQuery("from multi_tree in class com.dotmarketing.beans.MultiTree where parent1 = ? or parent2 = ? or child = ?");
			dh.setParam(containerIdentifier);
			dh.setParam(containerIdentifier);
			dh.setParam(containerIdentifier);

			return dh.list();
            
		} catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getContainerMultiTree failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}
	@SuppressWarnings("unchecked")
	public static java.util.List<MultiTree> getMultiTreeByChild(String contentIdentifier) {
		try {
			HibernateUtil dh = new HibernateUtil(MultiTree.class);
			dh.setQuery("from multi_tree in class com.dotmarketing.beans.MultiTree where child = ? ");
			dh.setParam(contentIdentifier);

			return dh.list();
            
		} catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getMultiTreeByChild failed:" + e, e);
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
	public static void saveMultiTree(MultiTree o) throws DotSecurityException {
		saveMultiTree(o, APILocator.getLanguageAPI().getDefaultLanguage()
				.getId());
	}

	/**
	 * Saves a Multi-Tree construct using a passed in language id in the system. A
	 * Muti-Tree is usually composed of the following five parts:
	 * <ol>
	 * <li>The identifier of the Content Page.</li>
	 * <li>The identifier of the container in the page.</li>
	 * <li>The identifier of the contentlet itself.</li>
	 * <li>The type of content relation.</li>
	 * <li>The order in which this construct is added to the database.</li>
	 * </ol>
	 * 
	 * @param o
	 *            - The Multi-Tree structure.
	 * @param languageId
	 *            - The language Id of the content page this contentlet will be
	 *            associated to.
	 * @throws DotSecurityException 
	 */
	public static void saveMultiTree(MultiTree o, long languageId) throws DotSecurityException {
	    if(!InodeUtils.isSet(o.getChild()) | !InodeUtils.isSet(o.getParent1()) || !InodeUtils.isSet(o.getParent2())) throw new DotRuntimeException("Make sure your Multitree is set!");
		try {
		    String id = o.getParent1();
			HibernateUtil.saveOrUpdate(o);
			updateVersionTs(id, languageId);
			refreshPageInCache(id,languageId);
		} catch (DotHibernateException e) {
			Logger.error(MultiTreeFactory.class, SAVE_MULTITREE_ERROR_MSG + e, e);
			throw new DotRuntimeException(e.getMessage());
		} catch (DotStateException e) {
			Logger.error(MultiTreeFactory.class, SAVE_MULTITREE_ERROR_MSG + e, e);
			throw new DotRuntimeException(e.getMessage());
		} catch (DotDataException e) {
            Logger.error(MultiTreeFactory.class, SAVE_MULTITREE_ERROR_MSG + e, e);
            throw new DotRuntimeException(e.getMessage());
        } catch (DotSecurityException e) {
            Logger.error(MultiTreeFactory.class, SAVE_MULTITREE_ERROR_MSG + e, e);
            throw new DotSecurityException(e.getMessage());
        }
	}

    public static java.util.List getChildrenClass(Inode p1, Inode p2, Class c) {

		try {
			String tableName =  ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);
			
			String sql = "SELECT {"  + tableName + ".*} from " + tableName + " " + tableName + ", multi_tree multi_tree, inode "+ tableName +"_1_ where multi_tree.parent1 = ? and multi_tree.parent2 = ? and multi_tree.child = " + tableName + ".inode and " + tableName + "_1_.inode = " + tableName + ".inode order by multi_tree.tree_order";
            Logger.debug(MultiTreeFactory.class, "getChildrenClass\n " + sql+ "\n");
			dh.setSQLQuery(sql);
            Logger.debug(MultiTreeFactory.class, "inode p1:  " + p1.getInode() + "\n");
            Logger.debug(MultiTreeFactory.class, "inode p2:  " + p2.getInode() + "\n");
			
			dh.setParam(p1.getInode());
			dh.setParam(p2.getInode());

			return dh.list();
		}
		catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getChildrenClass failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}
	
	public static java.util.List getChildrenClass(Identifier p1, Identifier p2, Class c) {

		try {
			String tableName = "";
			String sql = "";
			if(c.getName().contains("Identifier")){
			  tableName = "identifier"; 
			}else{
			  tableName = ((Inode) c.newInstance()).getType();
			}
			HibernateUtil dh = new HibernateUtil(c);
			if(tableName.equalsIgnoreCase("identifier")){
				sql = "SELECT {"  + tableName + ".*} from " + tableName + " " + tableName + ", multi_tree multi_tree "
				+ " where multi_tree.parent1 = ? and multi_tree.parent2 = ? and multi_tree.child = " + tableName + ".id and " 
				+ " order by multi_tree.tree_order";
			}else {
				sql = "SELECT {"  + tableName + ".*} from " + tableName + " " + tableName + ", multi_tree multi_tree, inode "
				+ tableName +"_1_ where multi_tree.parent1 = ? and multi_tree.parent2 = ? and multi_tree.child = " + tableName + ".inode and " 
				+ tableName + "_1_.inode = " + tableName + ".inode order by multi_tree.tree_order";
			}
            
			Logger.debug(MultiTreeFactory.class, "getChildrenClass\n " + sql+ "\n");
			
			dh.setSQLQuery(sql);
            
			Logger.debug(MultiTreeFactory.class, "inode p1:  " + p1.getInode() + "\n");
            Logger.debug(MultiTreeFactory.class, "inode p2:  " + p2.getInode() + "\n");
			
			dh.setParam(p1.getInode());
			dh.setParam(p2.getInode());

			return dh.list();
		}
		catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getChildrenClass failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}
	
	public static java.util.List getChildrenClass(Inode p1, Inode p2, Class c, String orderBy) {
		try {
			String tableName =  ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);
			
			String sql = "SELECT {"  + tableName + ".*} from " + tableName + " " + tableName + ", multi_tree multi_tree, inode "+ tableName +"_1_ where multi_tree.parent1 = ? and multi_tree.parent2 = ?  and multi_tree.child = " + tableName + ".inode and " + tableName + "_1_.inode = " + tableName + ".inode order by " + orderBy;
            Logger.debug(MultiTreeFactory.class, "hibernateUtilSQL:getChildrenClass\n " + sql+ "\n");
			dh.setSQLQuery(sql);
            Logger.debug(MultiTreeFactory.class, "inode p1:  " + p1.getInode() + "\n");
            Logger.debug(MultiTreeFactory.class, "inode p2:  " + p2.getInode() + "\n");

			dh.setParam(p1.getInode());
			dh.setParam(p2.getInode());

			return dh.list();
		}
		catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getChildrenClass failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}

	public static java.util.List getChildrenClassByCondition(Inode p1, Inode p2, Class c, String condition) {
		try {
			String tableName =  ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);
			
			String sql = "SELECT {"  + tableName + ".*} from " + tableName + " " + tableName + ", multi_tree multi_tree, inode "+ tableName +"_1_ where multi_tree.parent1 = ? and multi_tree.parent2 = ? and multi_tree.child = " + tableName + ".inode and " + tableName + "_1_.inode = " + tableName + ".inode and " + condition;
            Logger.debug(MultiTreeFactory.class, "hibernateUtilSQL:getChildrenClassByCondition\n " + sql);
			dh.setSQLQuery(sql);
            Logger.debug(MultiTreeFactory.class, "inode p1:  " + p1.getInode() + "\n");
            Logger.debug(MultiTreeFactory.class, "inode p2:  " + p2.getInode() + "\n");
			
			dh.setParam(p1.getInode());
			dh.setParam(p2.getInode());

			return dh.list();
		}
		catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getChildrenClassByCondition failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		//return new java.util.ArrayList();
	}

	public static java.util.List getChildrenClassByCondition(String p1, String p2, Class c, String condition) {
		try {
			String tableName =  ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);
			
			String sql = "SELECT {"  + tableName + ".*} from " + tableName + " " + tableName + ", multi_tree multi_tree, inode "+ tableName +"_1_ where multi_tree.parent1 = ? and multi_tree.parent2 = ? and multi_tree.child = " + tableName + ".inode and " + tableName + "_1_.inode = " + tableName + ".inode and " + condition;
            Logger.debug(MultiTreeFactory.class, "hibernateUtilSQL:getChildrenClassByCondition\n " + sql);
			dh.setSQLQuery(sql);
            Logger.debug(MultiTreeFactory.class, "inode p1:  " + p1 + "\n");
            Logger.debug(MultiTreeFactory.class, "inode p2:  " + p2 + "\n");
			
			dh.setParam(p1);
			dh.setParam(p2);

			return dh.list();
		}
		catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getChildrenClassByCondition failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}

	public static java.util.List getChildrenClassByConditionAndOrderBy(Inode p1, Inode p2, Class c, String condition, String orderby) {
		try {

			String tableName =  ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);
			
			String sql = "SELECT {"  + tableName + ".*} from " + tableName + " " + tableName + ", multi_tree multi_tree, inode "+ tableName +"_1_ where multi_tree.parent1 = ? and multi_tree.parent2 = ? and multi_tree.child = " + tableName + ".inode and " + tableName + "_1_.inode = " + tableName + ".inode and " + condition + " order by " + orderby;
            Logger.debug(MultiTreeFactory.class, "hibernateUtilSQL:getChildrenClassByConditionAndOrderBy\n " + sql+ "\n");
			dh.setSQLQuery(sql);
            Logger.debug(MultiTreeFactory.class, "inode p1:  " + p1.getInode() + "\n");
            Logger.debug(MultiTreeFactory.class, "inode p2:  " + p2.getInode() + "\n");
			
			dh.setParam(p1.getInode());
			dh.setParam(p2.getInode());

			return dh.list();
		}
		catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getChildrenClassByConditionAndOrderBy failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		//return new java.util.ArrayList();
	}
	
	public static java.util.List getChildrenClassByConditionAndOrderBy(String p1, String p2, Class c, String condition, String orderby) {
		try {

			String tableName =  ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);
			
			String sql = "SELECT {"  + tableName + ".*} from " + tableName + " " + tableName + ", multi_tree multi_tree, inode "+ tableName +"_1_ where multi_tree.parent1 = ? and multi_tree.parent2 = ? and multi_tree.child = " + tableName + ".inode and " + tableName + "_1_.inode = " + tableName + ".inode and " + condition + " order by " + orderby;
            Logger.debug(MultiTreeFactory.class, "hibernateUtilSQL:getChildrenClassByConditionAndOrderBy\n " + sql+ "\n");
			dh.setSQLQuery(sql);
            Logger.debug(MultiTreeFactory.class, "inode p1:  " + p1 + "\n");
            Logger.debug(MultiTreeFactory.class, "inode p2:  " + p2 + "\n");
			
			dh.setParam(p1);
			dh.setParam(p2);

			return dh.list();
		}
		catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getChildrenClassByConditionAndOrderBy failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}

	public static java.util.List getChildrenClassByOrder(Inode p1, Inode p2, Class c, String order) {
		try {
			String tableName =  ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);
			
			String sql = "SELECT {"  + tableName + ".*} from " + tableName + " " + tableName + ", multi_tree multi_tree, inode "+ tableName +"_1_ where multi_tree.parent1 = ? and multi_tree.parent2 = ? and multi_tree.child = " + tableName + ".inode and " + tableName + "_1_.inode = " + tableName + ".inode order by  " + order;
			
            Logger.debug(MultiTreeFactory.class, "hibernateUtilSQL:getChildrenClassByOrder\n " + sql);
			dh.setSQLQuery(sql);
            Logger.debug(MultiTreeFactory.class, "inode p1:  " + p1.getInode() + "\n");
            Logger.debug(MultiTreeFactory.class, "inode p2:  " + p2.getInode() + "\n");
            Logger.debug(MultiTreeFactory.class, "order:  " + order + "\n");
			
			dh.setParam(p1.getInode());
			dh.setParam(p2.getInode());

			return dh.list();
		}
		catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getChildrenClassByOrder failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}
	

	public static java.util.List getParentsOfClassByCondition(Inode p, Class c, String condition) {
		try {
			String tableName =  ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);
			
			String sql = "SELECT {"  + tableName + ".*} from " + tableName + " " + tableName + ", multi_tree multi_tree, inode "+ tableName +"_1_ where multi_tree.child = ? and (multi_tree.parent1 = " + tableName + ".inode or multi_tree.parent2 = " + tableName + ".inode) and " + tableName + "_1_.inode = " + tableName + ".inode and " + condition;
            Logger.debug(MultiTreeFactory.class, "hibernateUtilSQL:getParentsOfClassByCondition\n " + sql);
            Logger.debug(MultiTreeFactory.class, "inode:  " + p.getInode() + "\n");
            Logger.debug(MultiTreeFactory.class, "condition:  " + condition + "\n");
			dh.setSQLQuery(sql);
			dh.setParam(p.getInode());

			return dh.list();
		}
		catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getParentsOfClassByCondition failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}
	
	public static java.util.List getParentsOfClass(Inode p, Class c) {
		try {

			String tableName =  ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);
			String sql = "SELECT {"  + tableName + ".*} from " + tableName + " " + tableName + ", multi_tree multi_tree, inode "+ tableName +"_1_ where multi_tree.child = ? and (multi_tree.parent1 = " + tableName + ".inode or multi_tree.parent2 = " + tableName + ".inode) and " + tableName + "_1_.inode = " + tableName + ".inode ";
            Logger.debug(MultiTreeFactory.class, "hibernateUtilSQL:getParentOfClass:\n " + sql+ "\n");
			dh.setSQLQuery(sql);
			dh.setParam(p.getInode());
            Logger.debug(MultiTreeFactory.class, "inode:  " + p.getInode() + "\n");
			return dh.list();
		}
		catch (Exception e) {
            Logger.error(MultiTreeFactory.class, "getParentsOfClass failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}	
	
    /**
     * Update a HTML Page Version Info Timestamp given a HTMLPage Identifier.
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
    private static void updateVersionTs(String id) throws DotDataException {
        updateVersionTs(id, APILocator.getLanguageAPI().getDefaultLanguage().getId());    
    }
	
    /**
     * Update a HTML Page Version Info Timestamp given a HTMLPage Identifier and a Language Id.
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
	private static void updateVersionTs(String id, Long languageId) throws DotDataException {
	    Identifier ident = APILocator.getIdentifierAPI().find(id);
        ContentletVersionInfo versionInfo = APILocator.getVersionableAPI()
                .getContentletVersionInfo(ident.getId(), languageId);
        versionInfo.setVersionTs(new Date());
        APILocator.getVersionableAPI().saveContentletVersionInfo(
                versionInfo);
	}
	
    /**
     * Refresh Cached objects of page given a HTMLPage Identifier.
     * 
     * @param id The HTMLPage Identifier to pass in 
     * @throws DotContentletStateException
     * @throws DotDataException 
     * @throws DotSecurityException 
     *            
     */
    private static void refreshPageInCache(String id) throws DotDataException, DotSecurityException {
        refreshPageInCache(id, APILocator.getLanguageAPI().getDefaultLanguage().getId());
    }
    

    /**
     * Refresh Cached objects of page given a HTMLPage Identifier and a Language Id.
     * 
     * @param id The HTMLPage Identifier to pass in 
     * @param languageId A language Id that points to a specific version we'll clean up
     * @throws DotContentletStateException
     * @throws DotDataException 
     * @throws DotSecurityException 
     *            
     */
    private static void refreshPageInCache(String id, Long languageId) throws DotDataException, DotSecurityException {
        Identifier ident = APILocator.getIdentifierAPI().find(id);
        Contentlet content = APILocator.getContentletAPI()
                .findContentletByIdentifier(ident.getId(), true, languageId, APILocator.systemUser(), false);
        IHTMLPage htmlPage = APILocator.getHTMLPageAssetAPI().fromContentlet(content);
        PageServices.invalidateAll(htmlPage);
    }
	
}
