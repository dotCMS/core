package com.dotmarketing.factories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.hibernate.ObjectNotFoundException;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Parameter;
import com.dotmarketing.util.UtilMethods;

/**
 * 
 * @author will
 * @deprecated
 */
public class InodeFactory {


	/*
	 * Returns a single child inode of the specified class. If no child is
	 * found, a new instance is returned
	 */
	public static Object getChildOfClass(Inode inode, Class c) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);
			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.parent = ? and tree.child = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"'";

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getChildOfClass\n " + sql + "\n");

			Logger.debug(InodeFactory.class, "inode:  " + inode.getInode() + "\n");

			dh.setSQLQuery(sql);

			// dh.setQuery("from inode in class " + c.getName() + " where ? in
			// inode.parents.elements");
			dh.setParam(inode.getInode());

			return dh.load();
		} catch (Exception e) {
			Logger.error(InodeFactory.class, "getChildrenClass failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		/*
		 * try { return c.newInstance(); } catch (Exception e) { return new
		 * Object(); }
		 */
	}

	public static Object getChildOfClassbyCondition(Inode inode, Class c, String condition) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);
			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.parent = ? and tree.child = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' and " + condition;

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getChildOfClassbyCondition\n " + sql + "\n");

			Logger.debug(InodeFactory.class, "inode:  " + inode.getInode() + "\n");

			dh.setSQLQuery(sql);

			// dh.setQuery("from inode in class " + c.getName() + " where ? in
			// inode.parents.elements");
			dh.setParam(inode.getInode());

			return dh.load();
		} catch (Exception e) {
			Logger.error(InodeFactory.class, "getChildrenClass failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		/*
		 * try { return c.newInstance(); } catch (Exception e) { return new
		 * Object(); }
		 */
	}

	public static Object getChildOfClassByRelationTypeAndCondition(Inode inode, Class c, String relationType,
			String condition) {
		return getChildOfClassByRelationTypeAndCondition(inode.getInode(), c, relationType, condition);
	}

	public static Object getChildOfClassByRelationTypeAndCondition(String inode, Class c, String relationType,
			String condition) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);
			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.parent = ? and tree.child = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' and tree.relation_type=? and " + condition;

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getChildOfClassbyCondition\n " + sql + "\n");

			dh.setSQLQuery(sql);

			// dh.setQuery("from inode in class " + c.getName() + " where ? in
			// inode.parents.elements");
			dh.setParam(inode);
			dh.setParam(relationType);

			Logger.debug(InodeFactory.class, "inode:  " + inode + "\n");

			return dh.load();
		} catch (Exception e) {
			Logger.error(InodeFactory.class, "getChildrenClass failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		/*
		 * try { return c.newInstance(); } catch (Exception e) { return new
		 * Object(); }
		 */
	}

	/*
	 * Returns a single child inode of the specified class that also has the
	 * relation type. If no child is found, a new instance is returned
	 */
	public static Object getChildOfClassByRelationType(Inode p, Class c, String relationType) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			// dh.setQuery("from inode in class " + c.getName() + " where ? in
			// (select tree.parent from com.dotmarketing.beans.Tree tree
			// where inode.inode = tree.child and tree.relationType = ?)");
			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.parent = ? and tree.child = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' and tree.relation_type = ? ";

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getChildOfClassByRelationType\n " + sql + "\n");

			dh.setSQLQuery(sql);

			Logger.debug(InodeFactory.class, "inode:  " + p.getInode() + "\n");

			dh.setParam(p.getInode());
			dh.setParam(relationType);

			return dh.load();
		} catch (Exception e) {
			Logger.error(InodeFactory.class, "getChildrenClass failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		/*
		 * try { return c.newInstance(); } catch (Exception e) { return new
		 * Object(); }
		 */
	}

	public static java.util.List getChildrenClass(Inode p, Class c) {
		return getChildrenClass(p, c, 0, 0);
	}

	public static java.util.List getChildrenClass(Inode p, Class c, int limit, int offset) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.parent = ? and tree.child = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"'";

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getChildrenClass\n " + sql + "\n");

			dh.setSQLQuery(sql);

			Logger.debug(InodeFactory.class, "inode:  " + p.getInode() + "\n");

			dh.setParam(p.getInode());

			if (limit != 0) {
				dh.setFirstResult(offset);
				dh.setMaxResults(limit);
			}

			return dh.list();
		} catch (Exception e) {
			Logger.error(InodeFactory.class, "getChildrenClass failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		// return new java.util.ArrayList();
	}

	public static java.util.List getChildrenClass(Inode p, Class c, String orderBy) {
		return getChildrenClass(p, c, orderBy, 0, 0);
	}

	public static java.util.List getChildrenClass(Inode p, Class c, String orderBy, int limit, int offset) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {

			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.parent = ? and tree.child = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' order by " + orderBy;

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getChildrenClass\n " + sql + "\n");

			dh.setSQLQuery(sql);

			Logger.debug(InodeFactory.class, "inode:  " + p.getInode() + "\n");

			dh.setParam(p.getInode());

			if (limit != 0) {
				dh.setFirstResult(offset);
				dh.setMaxResults(limit);
			}

			return dh.list();
		} catch (Exception e) {
			Logger.error(InodeFactory.class, "getChildrenClass failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		// return new java.util.ArrayList();
	}

	public static java.util.List getChildrenClassByCondition(Inode p, Class c, String condition) {
		return getChildrenClassByCondition(p, c, condition, 0, 0);
	}

	public static java.util.List getChildrenClassByCondition(Inode p, Class c, String condition, int limit, int offset) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.parent = ? and tree.child = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' and " + condition;

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getChildrenClassByCondition\n " + sql);

			dh.setSQLQuery(sql);

			Logger.debug(InodeFactory.class, "inode:  " + p.getInode() + "\n");

			dh.setParam(p.getInode());

			if (limit != 0) {
				dh.setFirstResult(offset);
				dh.setMaxResults(limit);
			}

			return dh.list();
		} catch (Exception e) {
			Logger.error(InodeFactory.class, "getChildrenClass failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		// return new java.util.ArrayList();
	}

	public static java.util.List getChildrenClassByOrder(Inode p, Class c, String order) {
		return getChildrenClassByOrder(p, c, order, 0, 0);
	}

	public static java.util.List getChildrenClassByOrder(Inode p, Class c, String order, int limit, int offset) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.parent = ? and tree.child = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' order by  " + order;

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getChildrenClassByOrder\n " + sql);

			Logger.debug(InodeFactory.class, "inode:  " + p.getInode() + "\n");

			Logger.debug(InodeFactory.class, "order:  " + order + "\n");

			dh.setSQLQuery(sql);

			dh.setParam(p.getInode());

			if (limit != 0) {
				dh.setFirstResult(offset);
				dh.setMaxResults(limit);
			}

			return dh.list();
		} catch (Exception e) {
			Logger.error(InodeFactory.class, "getChildrenClassByOrder failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		// return new java.util.ArrayList();
	}

	public static java.util.List getChildrenClassOrderByRandom(Inode p, Class c) {
		return getChildrenClassOrderByRandom(p, c, 0, 0);
	}

	public static java.util.List getChildrenClassOrderByRandom(Inode p, Class c, int limit, int offset) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.parent = ? and tree.child = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' order by  rand()";

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getChildrenClassByOrder\n " + sql);

			Logger.debug(InodeFactory.class, "inode:  " + p.getInode() + "\n");

			dh.setFirstResult(offset);
			dh.setMaxResults(limit);

			dh.setSQLQuery(sql);

			dh.setParam(p.getInode());

			if (limit != 0) {
				dh.setFirstResult(offset);
				dh.setMaxResults(limit);
			}

			return dh.list();
		} catch (Exception e) {
			Logger.error(InodeFactory.class, "getChildrenClassByOrder failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		// return new java.util.ArrayList();
	}

	public static java.util.List getChildrenClassByCondition(Inode p1, Inode p2, Class c, String condition, int limit,
			int offset) {
		return getChildrenClassByCondition(p1.getInode(), p2.getInode(), c, condition, limit, offset);
	}

	public static java.util.List getChildrenClassByCondition(Inode p1, Inode p2, Class c, String condition) {
		return getChildrenClassByCondition(p1.getInode(), p2.getInode(), c, condition, 0, 0);
	}

	public static java.util.List getChildrenClassByCondition(String p1, String p2, Class c, String condition) {
		return getChildrenClassByCondition(p1, p2, c, condition, 0, 0);
	}

	public static java.util.List getChildrenClassByCondition(String p1, String p2, Class c, String condition, int limit,
			int offset) {
		try {
			if( c.equals(Identifier.class)){
				throw new DotStateException("Identifiers are no longer Inodes!");
			}
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName
			+ ", tree tree, tree tree2, inode " + tableName
			+ "_1_ where tree.parent = ? and tree2.parent = ?  and tree.child = " + tableName
			+ ".inode  and tree2.child = " + tableName + ".inode  and " + tableName + "_1_.inode = "
			+ tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' and " + condition;

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getChildrenClassByCondition\n " + sql + "\n");

			dh.setSQLQuery(sql);

			Logger.debug(InodeFactory.class, "inode:  " + p1 + "\n");
			Logger.debug(InodeFactory.class, "inode:  " + p2 + "\n");

			dh.setParam(p1);
			dh.setParam(p2);

			if (limit != 0) {
				dh.setFirstResult(offset);
				dh.setMaxResults(limit);
			}

			return dh.list();
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "getChildrenClassByCondition failed:" + e, e);

			// throw new DotRuntimeException(e.toString());
		}

		return new java.util.ArrayList();
	}

	public static java.util.List getChildrenClassByConditionAndOrderBy(String[] inodes, Class c, String condition,
			String orderBy) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		if (inodes == null || inodes.length == 0) {
			return InodeFactory.getInodesOfClassByConditionAndOrderBy(c, condition, orderBy);
		}

		try {

			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			StringBuffer sql = new StringBuffer("SELECT {" + tableName + ".*} from " + tableName + " " + tableName);

			for (int x = 1; x < inodes.length + 1; x++) {
				if (x == 1) {
					sql.append(", tree tree, ");
				} else {
					sql.append(", tree tree" + x + ", ");
				}
			}
			sql.append(" inode " + tableName + "_1_ ");
			sql.append(" where "+tableName+"_1_.type = '"+tableName+"' and ");

			sql.append(tableName + "_1_.inode =  " + tableName + ".inode and ");
			for (int x = 1; x < inodes.length + 1; x++) {
				if (x == 1) {
					sql.append(" ( tree.parent = ? and ");
					sql.append(" tree.child = " + tableName + ".inode ) and ");
				} else {
					sql.append(" (tree" + x + ".parent = ?  and ");
					sql.append(" tree" + x + ".child = " + tableName + ".inode ) and ");
				}
			}

			String query = sql.toString();
			query = query.substring(0, query.lastIndexOf("and"));

			// Validate condition
			condition = (UtilMethods.isSet(condition) ? " and " + condition : "");
			// Validate order
			orderBy = (UtilMethods.isSet(orderBy) ? " order by " + orderBy : "");

			// Create the final query
			query += condition + orderBy;

			// Set the query
			dh.setSQLQuery(query);

			for (int x = 0; x < inodes.length; x++) {
				dh.setParam(inodes[x]);
			}

			return dh.list();
		} catch (Exception e) {
			Logger.debug(InodeFactory.class, "getChildrenClassByConditionAndOrderBy failed:" + e, e);

			// throw new DotRuntimeException(e.toString());
		}

		return new java.util.ArrayList();
	}

	public static java.util.List getChildrenClassByConditionAndOrderBy(String p1, String p2, Class c, String condition,
			String orderBy) {

		return getChildrenClassByConditionAndOrderBy(p1, p2, c, condition, orderBy, 0, 0);

	}

	public static java.util.List getChildrenClassByConditionAndOrderBy(String p1, String p2, Class c, String condition,
			String orderBy, int limit, int offset) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName
			+ ", tree tree, tree tree2, inode " + tableName
			+ "_1_ where tree.parent = ? and tree2.parent = ?  and tree.child = " + tableName
			+ ".inode  and tree2.child = " + tableName + ".inode  and " + tableName + "_1_.inode = "
			+ tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' and " + condition + " order by " + orderBy;

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getChildrenClassByCondition\n " + sql + "\n");

			dh.setSQLQuery(sql);

			Logger.debug(InodeFactory.class, "inode 1:  " + p1 + "\n");

			Logger.debug(InodeFactory.class, "inode 2:  " + p2 + "\n");

			dh.setParam(p1);
			dh.setParam(p2);

			if (limit != 0) {
				dh.setFirstResult(offset);
				dh.setMaxResults(limit);
			}

			return dh.list();
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "getChildrenClass failed:" + e, e);

			// throw new DotRuntimeException(e.toString());
		}

		return new java.util.ArrayList();
	}

	public static java.util.List getChildrenClass(List inodes, Class c, String orderBy) {
		return getChildrenClass(inodes, c, orderBy, 0, 0);
	}

	public static java.util.List getChildrenClass(List inodes, Class c, int limit, int offset) {
		return getChildrenClassByConditionAndOrderBy(inodes, c, "", "", limit, offset);
	}

	public static java.util.List getChildrenClass(List inodes, Class c, String orderBy, int limit, int offset) {
		return getChildrenClassByConditionAndOrderBy(inodes, c, "", orderBy, limit, offset);
	}

	public static java.util.List getChildrenClassByConditionAndOrderBy(List inodes, Class c, String condition,
			String orderBy) {

		return getChildrenClassByConditionAndOrderBy(inodes, c, condition, orderBy, 0, 0);

	}

	public static java.util.List getChildrenClassByConditionAndOrderBy(List inodes, Class c, String condition,
			String orderBy, int limit, int offset) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		if (inodes == null || inodes.size() == 0) {
			return InodeFactory.getInodesOfClassByConditionAndOrderBy(c, condition, orderBy);

		}

		try {

			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			StringBuffer sql = new StringBuffer("SELECT {" + tableName + ".*} from " + tableName + " " + tableName);

			for (int x = 1; x < inodes.size() + 1; x++) {
				if (x == 1) {
					sql.append(", tree tree ");
				} else {
					sql.append(", tree tree" + x + " ");
				}
			}
			sql.append(", inode " + tableName + "_1_ ");

			sql.append(" where "+tableName+"_1_.type = '"+tableName+"' and ");

			sql.append(tableName + "_1_.inode =  " + tableName + ".inode and ");
			for (int x = 1; x < inodes.size() + 1; x++) {
				if (x == 1) {
					sql.append(" ( tree.parent = ? and ");
					sql.append(" tree.child = " + tableName + ".inode ) ");
				} else {
					sql.append(" and (tree" + x + ".parent = ?  and ");
					sql.append(" tree" + x + ".child = " + tableName + ".inode ) ");
				}
			}

			// only if we send condition
			if (UtilMethods.isSet(condition)) {
				sql.append(" and " + condition);
			}

			// only if we send orderby
			if (UtilMethods.isSet(orderBy)) {
				sql.append(" order by ");
				sql.append(orderBy);
			}

			dh.setSQLQuery(sql.toString());

			for (int x = 0; x < inodes.size(); x++) {
				Inode i = (Inode) inodes.get(x);
				dh.setParam(i.getInode());
			}
			if (limit != 0) {
				dh.setFirstResult(offset);
				dh.setMaxResults(limit);
			}

			return dh.list();
		} catch (Exception e) {
			Logger.debug(InodeFactory.class, "getChildrenClassByConditionAndOrderBy failed:" + e, e);

			// throw new DotRuntimeException(e.toString());
		}

		return new java.util.ArrayList();
	}

	public static java.util.List getChildrenClassByCondition(List inodes, Class c, String condition) {
		return getChildrenClassByCondition(inodes, c, condition, 0, 0);

	}

	public static java.util.List getChildrenClassByCondition(List inodes, Class c, String condition, int limit,
			int offset) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}

		try {

			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			StringBuffer sql = new StringBuffer("SELECT {" + tableName + ".*} from " + tableName + " " + tableName);

			for (int x = 1; x < inodes.size() + 1; x++) {
				if (x == 1) {
					sql.append(", tree tree, ");
				} else {
					sql.append(", tree tree" + x + ", ");
				}
			}
			sql.append(" inode " + tableName + "_1_ ");
			sql.append(" where "+tableName+"_1_.type = '"+tableName+"' and ");

			sql.append(tableName + "_1_.inode =  " + tableName + ".inode and ");
			for (int x = 1; x < inodes.size() + 1; x++) {
				if (x == 1) {
					sql.append(" ( tree.parent = ? and ");
					sql.append(" tree.child = " + tableName + ".inode ) and ");
				} else {
					sql.append(" (tree" + x + ".parent = ?  and ");
					sql.append(" tree" + x + ".child = " + tableName + ".inode ) and ");
				}
			}

			sql.append(condition);
			dh.setSQLQuery(sql.toString());

			for (int x = 0; x < inodes.size(); x++) {
				Inode i = (Inode) inodes.get(x);
				dh.setParam(i.getInode());
			}
			if (limit != 0) {
				dh.setFirstResult(offset);
				dh.setMaxResults(limit);
			}

			return dh.list();
		} catch (Exception e) {
			Logger.debug(InodeFactory.class, "getChildrenClassByConditionAndOrderBy failed:" + e, e);

			// throw new DotRuntimeException(e.toString());
		}

		return new java.util.ArrayList();
	}

	public static java.util.List getChildrenClassByConditionAndOrderBy(Inode p, Class c, String condition,
			String orderby) {

		return getChildrenClassByConditionAndOrderBy(p.getInode(), c, condition, orderby, 0, 0);
	}

	public static java.util.List getChildrenClassByConditionAndOrderBy(Inode p, Class c, String condition,
			String orderby, int limit, int offset) {

		return getChildrenClassByConditionAndOrderBy(p.getInode(), c, condition, orderby, limit, offset);
	}

	public static java.util.List getChildrenClassByConditionAndOrderBy(String p, Class c, String condition, String orderby) {

		return getChildrenClassByConditionAndOrderBy(p, c, condition, orderby, 0, 0);
	}

	public static java.util.List getChildrenClassByConditionAndOrderBy(String p, Class c, String condition,
			String orderby, int limit, int offset) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.parent = ? and tree.child = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' and " + condition + " order by " + orderby;

			if(!UtilMethods.isSet(orderby)) 
				sql +=  tableName + ".inode desc";

			//sql += (sql.toLowerCase().indexOf("limit") == -1 ? ", " + tableName + ".inode desc" : "");

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getChildrenClassByConditionAndOrderBy\n " + sql + "\n");

			dh.setSQLQuery(sql);

			Logger.debug(InodeFactory.class, "inode:  " + p + "\n");

			dh.setParam(p);

			if (limit != 0) {
				dh.setFirstResult(offset);
				dh.setMaxResults(limit);
			}

			return dh.list();
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "getChildrenClassByConditionAndOrderBy failed:" + e, e);

			// throw new DotRuntimeException(e.toString());
		}

		return new java.util.ArrayList();
	}

	public static Inode getInode(String x, Class c) {

		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}else if( c.equals(Folder.class)){
			throw new DotStateException("You should use the FolderAPI to get folder information");
		}else if(c.equals(Inode.class)){
			Logger.debug(InodeFactory.class, "You should not send Inode.class to getInode.  Send the extending class instead (inode:" + x + ")" );
			//Thread.dumpStack();
			DotConnect dc = new DotConnect();
			dc.setSQL("select type from inode where inode = ?");
			dc.addParam(x);
			try {
				if(dc.loadResults().size()>0){
					c = InodeUtils.getClassByDBType(dc.getString("type"));
				}
				else{
					Logger.debug(InodeFactory.class,  x + " is not an Inode " );
					return new Inode();
				}
			} catch (DotDataException e) {
				// this is not an INODE
				Logger.debug(InodeFactory.class,  x + " is not an Inode", e );
				return new Inode();
			}
		}
		
		
		Inode inode = null;
		try {
			inode = (Inode) new HibernateUtil(c).load(x);
		} catch (Exception e) {
			//return (Inode) new HibernateUtil(c).load(x);
		}
		return inode;
	}

	/*public static Inode getInode(long x, Class c) {
		return (Inode) new HibernateUtil(c).load(x);
	}*/

	public static java.util.List getInodesOfClass(Class c) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		return getInodesOfClass(c, 0, 0);
	}

	public static java.util.List getInodesOfClass(Class c, int limit, int offset) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			HibernateUtil dh = new HibernateUtil(c);
			String type = ((Inode) c.newInstance()).getType();
			dh.setQuery("from inode in class " + c.getName()+" where inode.type='"+type+"'");

			if (limit != 0) {
				dh.setFirstResult(offset);
				dh.setMaxResults(limit);
			}

			return dh.list();
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "getObjectsOfClass failed:" + e, e);
			// throw new DotRuntimeException(e.toString());
		}

		return new java.util.ArrayList();
	}

	public static java.util.List getInodesOfClassByCondition(Class c, String condition) {
		return getInodesOfClassByCondition(c, condition, 0, 0);
	}

	public static java.util.List getInodesOfClassByCondition(Class c, String condition, int limit, int offset) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			HibernateUtil dh = new HibernateUtil(c);
			String type = ((Inode) c.newInstance()).getType();
			dh.setQuery("from inode in class " + c.getName() + " where inode.type='"+type+"' and " + condition);

			if (limit != 0) {
				dh.setFirstResult(offset);
				dh.setMaxResults(limit);
			}

			return dh.list();
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "getInodesOfClassByCondition(Class c, String condition) failed:" + e, e);

			// throw new DotRuntimeException(e.toString());
		}

		return new java.util.ArrayList();
	}

	public static Object getInodeOfClassByCondition(Class c, String condition) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			HibernateUtil dh = new HibernateUtil(c);
			String type = ((Inode) c.newInstance()).getType();
			dh.setQuery("from inode in class " + c.getName() + " where inode.type='"+type+"' and " + condition);
			Logger.debug(InodeFactory.class, "getInodeOfClassByCondition query: " + dh.getQuery());
			return dh.load();
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "getInodeOfClassByCondition(Class c, String condition) failed:" + e, e);

			// throw new DotRuntimeException(e.toString());
		}

		try {
			return c.newInstance();
		} catch (Exception e) {
			throw new DotRuntimeException(e.toString());
		}
	}

	public static java.util.List getInodesOfClassByCondition(Class c, String condition, String orderby) {
		return getInodesOfClassByConditionAndOrderBy(c, condition, orderby, 0, 0);
	}

	public static java.util.List getInodesOfClassByConditionAndOrderBy(Class c, String condition, String orderby) {
		return getInodesOfClassByConditionAndOrderBy(c, condition, orderby, 0, 0);
	}

	public static java.util.List getInodesOfClassByConditionAndOrderBy(Class c, String condition, String orderby,
			int limit, int offset) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			HibernateUtil dh = new HibernateUtil(c);
			String type = ((Inode) c.newInstance()).getType();
			dh.setQuery("from inode in class " + c.getName() + " where inode.type='"+type+"' and " + condition + " order by " + orderby);
			if (limit != 0) {
				dh.setFirstResult(offset);
				dh.setMaxResults(limit);
			}

			return dh.list();
		} catch (Exception e) {
			Logger.warn(InodeFactory.class,
					"getInodesOfClassByCondition(Class c, String condition, String orderby) failed:" + e, e);
			// throw new DotRuntimeException(e.toString());
		}

		return new java.util.ArrayList();
	}

	public static java.util.List getInodesOfClass(Class c, int maxRows) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			HibernateUtil dh = new HibernateUtil(c);
			String type = ((Inode) c.newInstance()).getType();
			dh.setMaxResults(maxRows);
			dh.setQuery("from inode in class " + c.getName()+" where inode.type='"+type+"' ");

			return dh.list();
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "getObjectsOfClass failed:" + e, e);

			// throw new DotRuntimeException(e.toString());
		}

		return new java.util.ArrayList();
	}

	public static java.util.List getInodesOfClass(Class c, String orderBy) {
		return getInodesOfClass(c, orderBy, 0, 0);
	}

	public static java.util.List getInodesOfClass(Class c, String orderBy, int limit, int offset) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			HibernateUtil dh = new HibernateUtil(c);
			String type = ((Inode) c.newInstance()).getType();
			dh.setQuery("from inode in class " + c.getName() + " where inode.type='"+type+"' order by " + orderBy);

			if (limit != 0) {
				dh.setFirstResult(offset);
				dh.setMaxResults(limit);
			}

			return dh.list();
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "getInodesOfClass failed:" + e, e);
		}

		return new java.util.ArrayList();
	}

	public static Object getObject(long x, Class c) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		Object obj ;
		try {
			obj = new HibernateUtil(c).load(x);
		} catch (DotHibernateException e) {
			Logger.error(InodeFactory.class, "getObject failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
		return obj;
	}

	public static Object getParentOfClass(Inode i, Class c) {
		return getParentOfClass(String.valueOf(i.getInode()), c);
	}

	public static Object getParentOfClass(String i, Class c) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);
			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.child = ? and tree.parent = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"'";

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getParentOfClass:\n " + sql + "\n");

			dh.setSQLQuery(sql);
			dh.setParam(i);

			Logger.debug(InodeFactory.class, "inode:  " + i + "\n");

			List list = dh.list();

			if ((list != null) && (list.size() != 0)) {
				return list.get(0);
			}
		} catch (Exception e) {
			try {
				Logger.warn(InodeFactory.class, "getParentOfClass failed:" + e, e);

				return c.newInstance();
			} catch (Exception ex) {
				Logger.warn(InodeFactory.class, "getParentOfClass failed:" + e, e);

				// throw new DotRuntimeException(e.toString());
			}
		}

		try {
			return c.newInstance();
		} catch (Exception e) {
			return new Object();
		}
	}

	public static java.util.List getParentsOfClassNoLock(Inode p, Class c) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);
			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName
			+ " with (nolock), tree tree with (nolock), inode " + tableName + "_1_ with (nolock) where tree.child = ? and tree.parent = "
			+ tableName + ".inode and " + tableName + "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"'";

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getParentOfClass:\n " + sql + "\n");

			dh.setSQLQuery(sql);

			//dh.setQuery("from inode in class " + c.getName() + " where ? in
			// inode.children.elements");
			dh.setParam(p.getInode());

			Logger.debug(InodeFactory.class, "inode:  " + p.getInode() + "\n");

			return dh.list();
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "getParentsOfClass failed:" + e, e);

			//throw new DotRuntimeException(e.toString());
		}

		return new java.util.ArrayList();
	}

	public static Object getParentOfClassByRelationType(Inode p, Class c, String relationType) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			// dh.setQuery("from inode in class " + c.getName() + " where ? in
			// (select tree.parent from com.dotmarketing.beans.Tree tree
			// where inode.inode = tree.child and tree.relationType = ?)");
			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.child = ? and tree.parent = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and tree.relation_type = ? and "+tableName+"_1_.type = '"+tableName+"' ";

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getChildOfClassByRelationType\n " + sql + "\n");

			dh.setSQLQuery(sql);

			Logger.debug(InodeFactory.class, "inode:  " + p.getInode() + "\n");

			dh.setParam(p.getInode());
			dh.setParam(relationType);

			return dh.load();
		} catch (Exception e) {
			Logger.error(InodeFactory.class, "getChildrenClass failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}

	public static java.util.List getParentsOfClassByCondition(Inode p, Class c, String condition) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.child = ? and tree.parent = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' and " + condition;

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getParentsOfClassByCondition\n " + sql);

			Logger.debug(InodeFactory.class, "inode:  " + p.getInode() + "\n");

			Logger.debug(InodeFactory.class, "condition:  " + condition + "\n");

			dh.setSQLQuery(sql);
			dh.setParam(p.getInode());

			return dh.list();
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "getParentsOfClassByCondition failed:" + e, e);

			// throw new DotRuntimeException(e.toString());
		}

		return new java.util.ArrayList();
	}

	public static java.util.List getParentsOfClassByConditionSorted(Inode p, Class c, String condition, String sortOrder) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try 
		{
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.child = ? and tree.parent = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' and " + condition;

			if(UtilMethods.isSet(sortOrder))
			{
				sql = sql + " order by "+ sortOrder;	
			}

			//Logger
			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getParentsOfClassByCondition\n " + sql);
			Logger.debug(InodeFactory.class, "inode:  " + p.getInode() + "\n");
			Logger.debug(InodeFactory.class, "condition:  " + condition + "\n");
			// END Logger			

			dh.setSQLQuery(sql);
			dh.setParam(p.getInode());

			return dh.list();
		} 
		catch (Exception e)
		{
			Logger.warn(InodeFactory.class, "getParentsOfClassByCondition failed:" + e, e);
			// throw new DotRuntimeException(e.toString());
		}
		return new java.util.ArrayList();
	}

	public static java.util.List getParentsOfClass(Inode p, Class c) {
		return getParentsOfClass( p,  c, "");
	}
	public static java.util.List getParentsOfClass(Inode p, Class c, String sortOrder) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);
			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.child = ? and tree.parent = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' ";

			if(UtilMethods.isSet(sortOrder)){
				sql = sql + " order by "+ sortOrder;	
			}
			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getParentOfClass:\n " + sql + "\n");

			dh.setSQLQuery(sql);

			// dh.setQuery("from inode in class " + c.getName() + " where ? in
			// inode.children.elements");
			dh.setParam(p.getInode());

			Logger.debug(InodeFactory.class, "inode:  " + p.getInode() + "\n");

			return dh.list();
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "getParentsOfClass failed:" + e, e);

			// throw new DotRuntimeException(e.toString());
		}

		return new java.util.ArrayList();
	}

	public static Object getParentOfClassByCondition(Inode inode, Class c, String condition) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);
			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.child = ? and tree.parent = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' and " + condition;

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getParentOfClassbyCondition\n " + sql + "\n");

			Logger.debug(InodeFactory.class, "inode:  " + inode.getInode() + "\n");

			dh.setSQLQuery(sql);
			dh.setParam(inode.getInode());

			return dh.load();
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "getParentOfClassbyCondition failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		/*
		 * try { return c.newInstance(); } catch (Exception e) { return new
		 * Object(); }
		 */
	}

	public static void deleteInode(Object o) throws DotHibernateException {
		if(Identifier.class.equals(o.getClass())){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		Inode inode = (Inode) o;
		if(inode ==null || !UtilMethods.isSet(inode.getInode())){
			
			Logger.error(Inode.class, "Empty Inode Passed in");
			return;
		}
		
		
		if(o instanceof Permissionable){
			try {
				APILocator.getPermissionAPI().removePermissions((Permissionable)o);
			} catch (DotDataException e) {
				Logger.error(InodeFactory.class,"Cannot delete object because permissions not deleted : " + e.getMessage(),e);
				return;
			}
		}
		

				// workaround for dbs where we can't have more than one constraint
				// or triggers
				DotConnect db = new DotConnect();
				db.setSQL("delete from tree where child = ? or parent =?");
				db.addParam(inode.getInode());
				db.addParam(inode.getInode());
				db.getResult();
	
				// workaround for dbs where we can't have more than one constraint
				// or triggers
				db.setSQL("delete from multi_tree where child = ? or parent1 =? or parent2 = ?");
				db.addParam(inode.getInode());
				db.addParam(inode.getInode());
				db.addParam(inode.getInode());
				db.getResult();
            
		    
		    
			HibernateUtil.delete(o);
				


	}

	public static void deleteChildrenOfClass(Inode parent, Class c) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		java.util.List children = getChildrenClass(parent, c);
		java.util.Iterator childrenIter = children.iterator();

		while (childrenIter.hasNext()) {
			parent.deleteChild((Inode) childrenIter.next());
		}
	}

	public static void deleteChildrenOfClassByRelationType(Inode parent, Class c,String relationType) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		java.util.List children = getChildrenClass(parent, c);
		java.util.Iterator childrenIter = children.iterator();

		while (childrenIter.hasNext()) {
			parent.deleteChild((Inode) childrenIter.next());
		}
	}


	public static int countChildrenOfClass(Inode i, Class c) {
		return countChildrenOfClass(i, c, 0, 5);
	}

	public static int countChildrenOfClass(Inode i, Class c, int limit, int offset) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {

			String tableName = ((Inode) c.newInstance()).getType();
			DotConnect db = new DotConnect();
			db
			.setSQL("select count(*) as test from inode, tree where inode.inode = tree.child and tree.parent = ?  and inode.type = '"
					+ tableName + "' ");
			db.addParam(i.getInode());
			// db.addParam(tableName);
			return db.getInt("test");
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "countChildrenOfClass failed:" + e, e);
			throw new DotRuntimeException(e.toString());
			// return 0;
		}
	}

	public static Map<String,Integer> countChildrenOfClass(Inode i, Class<? extends Inode>[] inodeClasses) {
		if(Identifier.class.equals(i.getClass())){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			Map<String,Integer> statistics = new HashMap<String,Integer>();

			if( inodeClasses != null && inodeClasses.length > 0 ) {

				String[] types = new String[inodeClasses.length];
				StringBuffer arrayParams = new StringBuffer();
				String params = null;
				int n = 0;

				for(Class<? extends Inode> anInodeClass: inodeClasses) {
					types[n++] = anInodeClass.newInstance().getType();
					arrayParams.append("?,");
				}
				
				if( arrayParams.length() > 0 ) {
					params = arrayParams.toString().substring(0, arrayParams.length() - 1);
				}
	
				DotConnect db = new DotConnect();
				db
				.setSQL("select count(*) as test, inode.type from inode, tree where inode.inode = tree.child and tree.parent = ?  and inode.type IN (" + params + ") group by inode.type");
				db.addParam(i.getInode());
				
				for(String type: types) {
					db.addParam(type);
				}

				ArrayList<Map<String, Object>> results = db.getResults();
				int length = results.size();
				for(n = 0; n < length; n++)
				{
					Map<String, Object> hash = (Map<String, Object>) results.get(n);
					String type = (String) hash.get("type");
					Integer number = Integer.parseInt(((String) hash.get("test")));
					statistics.put(type, number);
				}
			}

			return statistics;
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "countChildrenOfClass(Inode,Class<Inode>[]) failed:" + e, e);
			throw new DotRuntimeException(e.toString());
			// return 0;
		}
	}

	
	public static List getChildrenOfClassByRelationType(Inode p, Class c, String relationType) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			// dh.setQuery("from inode in class " + c.getName() + " where ? in
			// (select tree.parent from com.dotmarketing.beans.Tree tree
			// where inode.inode = tree.child and tree.relationType = ?)");
			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.parent = ? and tree.child = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' and tree.relation_type = ?";

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getChildOfClassByRelationType\n " + sql + "\n");

			dh.setSQLQuery(sql);

			Logger.debug(InodeFactory.class, "inode:  " + p.getInode() + "\n");

			dh.setParam(p.getInode());
			dh.setParam(relationType);

			return dh.list();
		} catch (Exception e) {
			Logger.error(InodeFactory.class, "getChildrenClass failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		/*
		 * try { return c.newInstance(); } catch (Exception e) { return new
		 * Object(); }
		 */
	}

	public static List getChildrenOfClassByRelationTypeAndCondition(Inode inode, Class c, String relationType,
			String condition) {
		return getChildrenOfClassByRelationTypeAndCondition(inode.getInode(), c, relationType, condition);
	}

	public static List getChildrenOfClassByRelationTypeAndCondition(String inode, Class c, String relationType,
			String condition) {
		return getChildrenOfClassByRelationTypeAndCondition(inode, c, relationType, condition, null);
	}

	public static List getChildrenOfClassByRelationTypeAndCondition(String inode, Class c, String relationType,
			String condition, String orderBy) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);
			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
			+ tableName + "_1_ where tree.parent = ? and tree.child = " + tableName + ".inode and " + tableName
			+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' ";
			if(condition != null)
				sql += "and " + condition;

			if (orderBy != null)
				sql += " order by " + orderBy;

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getChildOfClassbyCondition\n " + sql + "\n");

			dh.setSQLQuery(sql);

			dh.setParam(inode);

			Logger.debug(InodeFactory.class, "inode:  " + inode + "\n");

			return dh.list();
		} catch (Exception e) {
			Logger.error(InodeFactory.class, "getChildrenClass failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		/*
		 * try { return c.newInstance(); } catch (Exception e) { return new
		 * Object(); }
		 */
	}

	public static java.util.List getInodesOfClassByConditionAndOrderBy(Class c, String condition, String orderby,int limit)
	{
		String direction = "asc";
		return getInodesOfClassByConditionAndOrderBy(c,condition,orderby,limit,0,direction);
	}

	public static java.util.List getInodesOfClassByConditionAndOrderBy(Class c, String condition, String orderby,int limit,int offset,String direction) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			HibernateUtil dh = new HibernateUtil(c);
			String type = ((Inode) c.newInstance()).getType();
			String query = "from inode in class " + c.getName();
			// condition
			query += (UtilMethods.isSet(condition) ? " where inode.type ='"+type+"' and " + condition : " where inode.type ='"+type+"'");
			// order
			query +=  (UtilMethods.isSet(orderby) ? " order by " + orderby + "" : "");
			query += ((UtilMethods.isSet(orderby) && UtilMethods.isSet(direction)) ? " " + direction : "");
			// Limit to retrieve the "limit" number of entries in the DB
			if (limit != 0) {
                dh.setFirstResult(offset);
				dh.setMaxResults(limit);
			}

			dh.setQuery(query);
			return dh.list();
		} catch (Exception e) {
			Logger.warn(InodeFactory.class,
					"getInodesOfClassByCondition(Class c, String condition, String orderby) failed:" + e, e);
			// throw new DotRuntimeException(e.toString());
		}

		return new java.util.ArrayList();
	}

	public static java.util.List getChildrenClassByRelationType(Inode p, Class c,String relationType) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName
			+ ", tree tree, inode " + tableName + "_1_ where tree.parent = ? and tree.child = "
			+ tableName + ".inode and " + tableName + "_1_.inode = " + tableName + ".inode and "+ tableName + "_1_.type = '" + tableName + "' and tree.relation_type = '" + relationType + "'";

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getChildrenClass\n " + sql + "\n");

			dh.setSQLQuery(sql);

			Logger.debug(InodeFactory.class, "inode:  " + p.getInode() + "\n");

			dh.setParam(p.getInode());
			return dh.list();
		} catch (Exception e) {
			Logger.error(InodeFactory.class, "getChildrenClass failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		//return new java.util.ArrayList();
	}

	public static java.util.List getParentsOfClassByRelationType(Inode p, Class c, String relationType) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName
			+ ", tree tree, inode " + tableName + "_1_ where tree.child = ? and tree.parent = "
			+ tableName + ".inode and " + tableName + "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' and tree.relation_type = ?";

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getParentsOfClassByCondition\n " + sql);

			Logger.debug(InodeFactory.class, "inode:  " + p.getInode() + "\n");

			Logger.debug(InodeFactory.class, "relation:  " + relationType + "\n");

			dh.setSQLQuery(sql);
			dh.setParam(p.getInode());
			dh.setParam(relationType);

			return dh.list();
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "getParentsOfClassByCondition failed:" + e, e);

			//throw new DotRuntimeException(e.toString());
		}

		return new java.util.ArrayList();
	}

	public static java.util.List getChildrenClassByConditionAndOrderBy(String[] inodes, Class c, String condition,
			String orderBy,int quantity) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		if (inodes == null || inodes.length == 0)
		{        	
			return InodeFactory.getInodesOfClassByConditionAndOrderBy(c, condition, orderBy,quantity);
		}

		try {

			String tableName = ((Inode) c.newInstance()).getType();
			HibernateUtil dh = new HibernateUtil(c);

			StringBuffer sql = new StringBuffer("SELECT {" + tableName + ".*} from " + tableName + " " + tableName);

			for (int x = 1; x < inodes.length + 1; x++) {
				if (x == 1) {
					sql.append(", tree tree ");
				} else {
					sql.append(", tree tree" + x + " ");
				}
			}
			sql.append(", inode " + tableName + "_1_ ");
			sql.append(" where and "+tableName+"_1_.type = '"+tableName+"' and ");

			sql.append(tableName + "_1_.inode =  " + tableName + ".inode and ");
			for (int x = 1; x < inodes.length + 1; x++) {
				if (x == 1) {
					sql.append(" ( tree.parent = ? and ");
					sql.append(" tree.child = " + tableName + ".inode ) and ");
				} else {
					sql.append(" (tree" + x + ".parent = ?  and ");
					sql.append(" tree" + x + ".child = " + tableName + ".inode ) and ");
				}
			}

			String query = sql.toString();
			query = query.substring(0,query.lastIndexOf("and"));

			//Validate condition
			condition = (UtilMethods.isSet(condition) ? " and " + condition : "");
			//Validate order
			orderBy = (UtilMethods.isSet(orderBy) ? " order by " + orderBy : "");

			//Create the final query
			query += condition + orderBy;

			//Set the query
			if(quantity >= 0)
			{
				dh.setFirstResult(0);
				dh.setMaxResults(quantity);
			}            
			dh.setSQLQuery(query);

			for (int x = 0; x < inodes.length; x++) {                
				dh.setParam(inodes[x]);
			}

			return dh.list();
		} catch (Exception e) {
			Logger.debug(InodeFactory.class, "getChildrenClassByConditionAndOrderBy failed:" + e, e);

			//throw new DotRuntimeException(e.toString());
		}

		return new java.util.ArrayList();
	}
	
	//http://jira.dotmarketing.net/browse/DOTCMS-3232
    //To Check whether given inode exists in DB or not
	public static boolean isInode(String inode){
		DotConnect dc = new DotConnect();
		String InodeQuery = "Select count(*) as count from Inode where inode = ?";
		dc.setSQL(InodeQuery);
		dc.addParam(inode);
		ArrayList<Map<String, String>> results = new ArrayList<Map<String, String>>();
		try {
			results = dc.getResults();
		} catch (DotDataException e) {
			Logger.error(InodeFactory.class,"isInode method failed:"+ e, e);
		}
		int count = Parameter.getInt(results.get(0).get("count"),0);
		if(count > 0){
			return true;
		}
		return false;
	}		
	public static Inode find(String id) throws DotDataException {
		Inode inode = null;
		try {
			inode = (Inode) HibernateUtil.load(Inode.class, id);
		} catch (DotHibernateException e) { 
			if(!(e.getCause() instanceof ObjectNotFoundException))
				throw e; 
		}
		return inode;
	}

}
