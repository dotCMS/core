package com.dotmarketing.factories;

import com.dotmarketing.util.ConvertToPOJOUtil;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

			updateHTMLPageVersionTS(inode1.getInode());
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
     * @see MultiTreeFactory#updateHTMLPageVersionTS(String)
     * @see MultiTreeFactory#refreshPageInCache(String)
     */
    public static void deleteMultiTreeByParent1(Identifier parent, Long languageId)
            throws DotDataException, DotSecurityException {
        DotConnect db = new DotConnect();

        try {
            if (languageId == null) {
                db.executeStatement("DELETE FROM multi_tree WHERE parent1 = '" + parent.getId()
                        + "';");
				updateHTMLPageVersionTS(parent.getId());
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

			updateHTMLPageVersionTS(parent.getId());
            refreshPageInCache(parent.getId());
            
        } catch (SQLException e) {
            throw new DotDataException(DELETE_MULTITREE_ERROR_MSG, e);
        } catch (DotContentletStateException e) {
            throw new DotContentletStateException(DELETE_MULTITREE_ERROR_MSG, e);
        } catch (DotSecurityException e) {
            throw new DotSecurityException(DELETE_MULTITREE_ERROR_MSG, e);
        }
    }
    
    /**
     * Deletes multi-tree relationship given a MultiTree object.
     * It also updates the version_ts of all versions of the htmlpage passed in (multiTree.parent1)
     *
     * @param multiTree
     * @throws DotDataException
     * @throws DotSecurityException 
     *            
     */
	public static void deleteMultiTree(MultiTree multiTree) throws DotDataException, DotSecurityException {
	    try {
	        String id = multiTree.getParent1();
	        HibernateUtil.delete(multiTree);
	        updateHTMLPageVersionTS(id);
	        refreshPageInCache(id);
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
			updateHTMLPageVersionTS(id);
			refreshPageInCache(id);
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
			StringBuilder sql = new StringBuilder();

			if(c.getName().contains("Identifier")){
				tableName = "identifier";
			}else{
				tableName = ((Inode) c.newInstance()).getType();
			}
			DotConnect dc = new DotConnect();

			if(tableName.equalsIgnoreCase("identifier")){
				sql.append("SELECT ");
				sql.append(tableName);
				sql.append(".* from ");
				sql.append(tableName);
				sql.append(", multi_tree multi_tree ")
						.append(" where multi_tree.parent1 = ? and multi_tree.parent2 = ? and multi_tree.child = ");
				sql.append(tableName);
				sql.append(".id order by multi_tree.tree_order");
			}else {
				sql.append("SELECT ");
				sql.append(tableName);
				sql.append(".* from ");
				sql.append(tableName);
				sql.append(", multi_tree multi_tree, inode ");
				sql.append(tableName);
				sql.append("_1_ where multi_tree.parent1 = ? and multi_tree.parent2 = ? and multi_tree.child = ");
				sql.append(tableName);
				sql.append(".inode and ");
				sql.append(tableName);
				sql.append("_1_.inode = ");
				sql.append(tableName);
				sql.append(".inode order by multi_tree.tree_order");
			}

			Logger.debug(MultiTreeFactory.class, "getChildrenClass\n " + sql+ "\n");

			dc.setSQL(sql.toString());
            
			Logger.debug(MultiTreeFactory.class, "inode p1:  " + p1.getId() + "\n");
            Logger.debug(MultiTreeFactory.class, "inode p2:  " + p2.getId() + "\n");

			dc.addParam(p1.getId());
			dc.addParam(p2.getId());

			return ConvertToPOJOUtil.convertDotConnectMapToPOJO(dc.loadResults(), c);
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
	private static void updateHTMLPageVersionTS(String id) throws DotDataException, DotSecurityException {
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
     * @throws DotSecurityException 
     *            
     */
    private static void refreshPageInCache(String pageIdentifier) throws DotDataException, DotSecurityException {
        Set<String> inodes = new HashSet<String>();
        List<ContentletVersionInfo> infos = APILocator.getVersionableAPI().findContentletVersionInfos(pageIdentifier);
        for (ContentletVersionInfo versionInfo : infos) {
            inodes.add(versionInfo.getWorkingInode());
            if(versionInfo.getLiveInode() != null){
              inodes.add(versionInfo.getLiveInode());
            }
        }

        List<Contentlet> contentlets = APILocator.getContentletAPIImpl().findContentlets(Lists.newArrayList(inodes));
		for (Contentlet pageContent : contentlets) {
			IHTMLPage htmlPage = APILocator.getHTMLPageAssetAPI().fromContentlet(pageContent);
			PageServices.invalidateAll(htmlPage);
		}
    }
	
}
