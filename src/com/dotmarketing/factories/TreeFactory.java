package com.dotmarketing.factories;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.hibernate.HibernateException;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;

/**
 * 
 * @author maria
 */
public class TreeFactory {

	public static Tree getTree(String x) {
		try {
			return (Tree) new HibernateUtil(Tree.class).load(Long.parseLong(x));
		} catch (Exception e) {
			Tree tree = null ;
			try {
				tree =  (Tree) new HibernateUtil(Tree.class).load(x);
			} catch (DotHibernateException e1) {
				Logger.error(TreeFactory.class, "getTree failed:" + e, e);
			}
			return tree;
		}
	}

	public static Tree getTree(Inode parent, Inode child) {
		String relationType = "child";
		return getTree(parent, child, relationType);
	}

	public static Tree getTree(String parent, String child) {
		String relationType = "child";
		return getTree(parent, child, relationType);
	}

	public static Tree getTree(String parent, String child, String relationType) {
		try {
			String query = "from tree in class com.dotmarketing.beans.Tree where parent = ? and child = ? ";
			if(relationType != null) query += " and relation_type = ?";
			HibernateUtil dh = new HibernateUtil(Tree.class);
			dh.setQuery(query);
			dh.setParam(parent);
			dh.setParam(child);
			if(relationType != null) dh.setParam(relationType);

			return (Tree) dh.load();
		} catch (Exception e) {
			Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
		}

		return new Tree();
	}

	public static Tree getTree(Inode parent, Inode child, String relationType) {
		return getTree(parent.getInode(), child.getInode(), relationType);
	}

	public static Tree getTreeByChildAndRelationType(Inode child, String relationType) {
		return getTreeByChildAndRelationType(child.getInode(), relationType);
	}
	
	public static Tree getTreeByChildAndRelationType(String child, String relationType) {
		try {
			HibernateUtil dh = new HibernateUtil(Tree.class);
			dh.setQuery("from tree in class com.dotmarketing.beans.Tree where child = ? and relation_type = ?");
			dh.setParam(child);
			dh.setParam(relationType);

			return (Tree) dh.load();
		} catch (Exception e) {
			Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
		}

		return new Tree();
	}

	public static Tree getTreeByParentAndRelationType(Inode parent, String relationType) {
		try {
			HibernateUtil dh = new HibernateUtil(Tree.class);
			dh.setQuery("from tree in class com.dotmarketing.beans.Tree where parent = ? and relation_type = ?");
			dh.setParam(parent.getInode());
			dh.setParam(relationType);

			return (Tree) dh.load();
		} catch (Exception e) {
			Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
		}

		return new Tree();
	}

	@SuppressWarnings("unchecked")
	public static List<Tree> getTreesByParentAndRelationType(Inode parent, String relationType) {
		try {
			HibernateUtil dh = new HibernateUtil(Tree.class);
			dh
					.setQuery("from tree in class com.dotmarketing.beans.Tree where parent = ? and relation_type = ? order by tree_order asc");
			dh.setParam(parent.getInode());
			dh.setParam(relationType);

			return dh.list();
		} catch (Exception e) {
			Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
		}

		return new ArrayList<Tree>();
	}
	
	public static List<Tree> getTreesByParentAndRelationType(Identifier parent, String relationType) {
		try {
			HibernateUtil dh = new HibernateUtil(Tree.class);
			dh
					.setQuery("from tree in class com.dotmarketing.beans.Tree where parent = ? and relation_type = ? order by tree_order asc");
			dh.setParam(parent.getInode());
			dh.setParam(relationType);
			return dh.list();
		} catch (Exception e) {
			Logger.warn(TreeFactory.class, "getTreesByParentAndRelationType failed:" + e, e);
		}

		return new ArrayList<Tree>();
	}

	@SuppressWarnings("unchecked")
	public static List<Tree> getTreesByChildAndRelationType(Inode child, String relationType) {
		try {
			HibernateUtil dh = new HibernateUtil(Tree.class);
			dh
					.setQuery("from tree in class com.dotmarketing.beans.Tree where child = ? and relation_type = ? order by tree_order asc");
			dh.setParam(child.getInode());
			dh.setParam(relationType);

			return dh.list();
		} catch (Exception e) {
			Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
		}

		return new ArrayList<Tree>();
	}
	
	public static List<Tree> getTreesByChildAndRelationType(Identifier child, String relationType) {
		try {
			HibernateUtil dh = new HibernateUtil(Tree.class);
			dh
					.setQuery("from tree in class com.dotmarketing.beans.Tree where child = ? and relation_type = ? order by tree_order asc");
			dh.setParam(child.getInode());
			dh.setParam(relationType);

			return dh.list();
		} catch (Exception e) {
			Logger.warn(TreeFactory.class, "getTreesByChildAndRelationType failed:" + e, e);
		}

		return new ArrayList<Tree>();
	}

	@SuppressWarnings("unchecked")
	public static List<Tree> getTreesByRelationType(String relationType) {
		try {
			HibernateUtil dh = new HibernateUtil(Tree.class);
			dh.setQuery("from tree in class com.dotmarketing.beans.Tree where relation_type = ?");
			dh.setParam(relationType);

			return dh.list();
		} catch (Exception e) {
			Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
		}

		return new ArrayList<Tree>();
	}

	@SuppressWarnings("unchecked")
	public static List<Tree> getTreesByParent(Inode inode) {
		return getTreesByParent(inode.getInode());
	}
	
	@SuppressWarnings("unchecked")
	public static List<Tree> getTreesByParent(String inode) {
		try {
			HibernateUtil dh = new HibernateUtil(Tree.class);
			dh.setQuery("from tree in class com.dotmarketing.beans.Tree where parent = ?");
			dh.setParam(inode);

			return dh.list();
		} catch (Exception e) {
			Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
		}

		return new ArrayList<Tree>();
	}

	public static List<Tree> getTreesByChild(Inode inode) {
		return getTreesByChild(inode.getInode());
	}
	
	@SuppressWarnings("unchecked")
	public static List<Tree> getTreesByChild(String inode) {
		try {
			HibernateUtil dh = new HibernateUtil(Tree.class);
			dh.setQuery("from tree in class com.dotmarketing.beans.Tree where child = ?");
			dh.setParam(inode);

			return dh.list();
		} catch (Exception e) {
			Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
		}

		return new ArrayList<Tree>();
	}

	public static void swapTrees(Inode i1, Inode i2) throws HibernateException {

		List<Tree> newTrees = new ArrayList<Tree>();

		// Removing actual trees and creating the new ones
		Iterator<Tree> it = getTreesByParent(i1).iterator();
		while (it.hasNext()) {
			Tree tree = (Tree) it.next();
			newTrees.add(new Tree(i2.getInode(), tree.getChild(), tree.getRelationType(), tree.getTreeOrder()));
			deleteTree(tree);
		}

		it = getTreesByChild(i1).iterator();
		while (it.hasNext()) {
			Tree tree = (Tree) it.next();
			newTrees.add(new Tree(tree.getParent(), i2.getInode(), tree.getRelationType(), tree.getTreeOrder()));
			deleteTree(tree);
		}

		it = getTreesByParent(i2).iterator();
		while (it.hasNext()) {
			Tree tree = (Tree) it.next();
			newTrees.add(new Tree(i1.getInode(), tree.getChild(), tree.getRelationType(), tree.getTreeOrder()));
			deleteTree(tree);
		}

		it = getTreesByChild(i2).iterator();
		while (it.hasNext()) {
			Tree tree = (Tree) it.next();
			newTrees.add(new Tree(tree.getParent(), i1.getInode(), tree.getRelationType(), tree.getTreeOrder()));
			deleteTree(tree);
		}

		// Saving new trees
		it = newTrees.iterator();
		while (it.hasNext()) {
			Tree tree = (Tree) it.next();
			saveTree(tree);
		}
		try {
			HibernateUtil.flush();
			HibernateUtil.getSession().refresh(i1);
			HibernateUtil.getSession().refresh(i2);
		} catch (DotHibernateException e) {
			Logger.error(TreeFactory.class,"swapTrees failed:" + e, e);
		}

	}

	public static void deleteTree(Tree tree) {
		try {
			HibernateUtil.delete(tree);
		} catch (DotHibernateException e) {
			Logger.error(TreeFactory.class, "deleteTree failed:" + e, e);
		}
	}

	public static void deleteTreesByParent(Inode parent) {
		try {
			HibernateUtil.delete("from tree in class com.dotmarketing.beans.Tree where tree.parent = '" + parent.getInode()+"'");
		} catch (DotHibernateException e) {
			Logger.error(TreeFactory.class, "deleteTreesByParent failed:" + e, e);
		}
	}

	public static void deleteTreesByParentAndRelationType(Inode parent, String relationType) {
		try {
			HibernateUtil.delete("from tree in class com.dotmarketing.beans.Tree where tree.parent = '" + parent.getInode() + 
					"' and tree.relationType = '" + relationType + "'");
		} catch (DotHibernateException e) {
			Logger.error(TreeFactory.class, "deleteTreesByParentAndRelationType failed:" + e, e);
		}
	}


	public static void deleteTreesByChildAndRelationType(Inode child, String relationType) {
		try {
			HibernateUtil.delete("from tree in class com.dotmarketing.beans.Tree where tree.child = '" + child.getInode() + 
					"' and tree.relationType = '" + relationType + "'");
		} catch (DotHibernateException e) {
			Logger.error(TreeFactory.class, "deleteTreesByChildAndRelationType failed:" + e, e);
		}
	}
	
	public static void deleteTreesByChild(Inode child) {
		try {
			HibernateUtil.delete("from tree in class com.dotmarketing.beans.Tree where tree.child = '" + child.getInode()+"'");
		} catch (DotHibernateException e) {
			Logger.error(TreeFactory.class, "deleteTreesByChild failed:" + e, e);
		}
	}

	public static void deleteTreesByRelationType(String relationType) {
		try {
			HibernateUtil
					.delete("from tree in class com.dotmarketing.beans.Tree where tree.relationType = '" + relationType + "'");
		} catch (DotHibernateException e) {
			Logger.error(TreeFactory.class, "deleteTreesByRelationType failed:" + e, e);
		}
	}

	public static void saveTree(Tree tree) {
		try {
			HibernateUtil.saveOrUpdate(tree);
		} catch (DotHibernateException e) {
			Logger.error(TreeFactory.class, "saveTree failed:" + e, e);
		}
	}

	
}
