package com.dotmarketing.factories;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;

import com.dotmarketing.exception.DotDataException;

import com.dotmarketing.util.Logger;

/**
 * 
 * @author maria
 */
public class TreeFactory {




	public static Tree getTree(final Inode parent,final  Inode child) {

		return getTree(parent.getInode(), child.getInode());
	}

	public static Tree getTree(final String parent, final String child) {
		String relationType = "child";
		return getTree(parent, child, relationType);

	}

	public static Tree getTree(final String parent, final String child, final String relationType) {
		try {
			DotConnect dc = new DotConnect();
			String query = "select * from tree where parent = ? and child = ? ";
			if(relationType != null) query += " and relation_type = ?";
			dc.setSQL(query);
			dc.addParam(parent);
			dc.addParam(child);
			if(relationType != null) dc.addParam(relationType);

			return new DBTreeTransformer(dc.loadObjectResults()).tree();


		} catch (DotDataException e) {
			Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
		}

		return new Tree();
	}

	public static Tree getTree(final Tree tree) {
		return getTree(tree.getParent(), tree.getChild(), tree.getRelationType());
	}
	
	public static List<Tree> getAllTrees(final int limit, final int offset) throws DotDataException {

			DotConnect dc = new DotConnect()
				.setSQL("select * from tree order by parent,child,relation_type ")
				.setMaxRows(limit)
				.setStartRow(offset);

			return new DBTreeTransformer(dc.loadObjectResults()).trees();



	}
	
	
	public static Tree getTree(final Inode parent, final Inode child,final String relationType) {
		return getTree(parent.getInode(), child.getInode(), relationType);
	}


	
	public static List<Tree> getTreesByParentAndRelationType(Identifier parent, String relationType) {
		return getTreesByParentAndRelationType(parent.getId(), relationType);
	}
	

	public static List<Tree> getTreesByParentAndRelationType(Inode parent, String relationType) {
		return getTreesByParentAndRelationType(parent.getInode(), relationType);
	}
	

	private static List<Tree> getTreesByParentAndRelationType(String parent, String relationType) {
		try {
			DotConnect dc = new DotConnect();
			dc.setSQL("select * from tree where  parent = ? and relation_type = ? order by tree_order asc");
			dc.addParam(parent);
			dc.addParam(relationType);

			return new DBTreeTransformer(dc.loadObjectResults()).trees();
		} catch (Exception e) {
			Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
		}

		return new ArrayList<Tree>();
	}
	
	

	private static List<Tree> getTreesByChildAndRelationType(String child, String relationType) {
		try {
			DotConnect dc = new DotConnect();
			dc.setSQL("select * from tree where  child = ? and relation_type = ? order by tree_order asc");
			dc.addParam(child);
			dc.addParam(relationType);

			return new DBTreeTransformer(dc.loadObjectResults()).trees();
		} catch (Exception e) {
			Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
		}

		return new ArrayList<Tree>();
	}


	public static List<Tree> getTreesByChildAndRelationType(Inode child, String relationType) {
		return getTreesByChildAndRelationType(child.getInode(), relationType);
	}
	
	public static List<Tree> getTreesByChildAndRelationType(Identifier child, String relationType) {
		return getTreesByChildAndRelationType(child.getId(), relationType);
	}


	public static List<Tree> getTreesByRelationType(String relationType) {
		try {
			DotConnect dc = new DotConnect();
			dc.setSQL("select * from tree where  relation_type = ?");
			dc.addParam(relationType);

			return new DBTreeTransformer(dc.loadObjectResults()).trees();
		} catch (Exception e) {
			Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
		}

		return new ArrayList<Tree>();
	}


	public static List<Tree> getTreesByParent(Inode inode) {
		return getTreesByParent(inode.getInode());
	}
	

	public static List<Tree> getTreesByParent(String inode) {
		try {
			DotConnect dc = new DotConnect();
			dc.setSQL("select * from tree where  parent = ?");
			dc.addParam(inode);

			return new DBTreeTransformer(dc.loadObjectResults()).trees();
		} catch (Exception e) {
			Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
		}

		return new ArrayList<Tree>();
	}

	public static List<Tree> getTreesByChild(Inode inode) {
		return getTreesByChild(inode.getInode());
	}
	

	public static List<Tree> getTreesByChild(String inode) {
		try {
			DotConnect dc = new DotConnect();
			dc.setSQL("select * from tree where  child = ?");
			dc.addParam(inode);

			return new DBTreeTransformer(dc.loadObjectResults()).trees();
		} catch (Exception e) {
			Logger.warn(TreeFactory.class, "getTree failed:" + e, e);
		}

		return new ArrayList<Tree>();
	}

	public static void swapTrees(final Inode i1, final Inode i2) throws DotDataException {

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
	}

	public static void deleteTree(Tree tree) {
		try {
			DotConnect dc = new DotConnect();

			dc.setSQL("delete from tree where parent = ? and child = ? and relation_type = ?");

			dc.addParam(tree.getParent());
			dc.addParam(tree.getChild());
			dc.addParam(tree.getRelationType());
			dc.loadResult();
		} catch (DotDataException e) {

		  throw new DotStateException(e);

		}
	}

	public static void deleteTreesByParent(Inode parent) {
		try {
			DotConnect dc = new DotConnect();
			dc.setSQL("delete from tree where parent = ?");
			dc.addParam(parent.getInode());
			dc.loadResult();
		} catch (DotDataException e) {
		  throw new DotStateException(e);
		}
	}

	public static void deleteTreesByParentById(final String parentId) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL("DELETE FROM tree WHERE parent = ?").addParam(parentId).loadResult();
	}

	public static void deleteTreesByParentAndRelationType(Inode parent, String relationType) {
		try {
			DotConnect dc = new DotConnect();
			dc.setSQL("delete from tree where parent = ?  and relation_type = ?");
			dc.addParam(parent.getInode());
			dc.addParam(relationType);
			dc.loadResult();
		} catch (DotDataException e) {
		  throw new DotStateException(e);
		}
	}


	public static void deleteTreesByParentAndChildAndRelationType(String parent, String child, String relationType) {
		try {

			DotConnect dc = new DotConnect();
			dc.setSQL("delete from tree where parent = ? and child = ? and relation_type = ?");

			dc.addParam(parent);
			dc.addParam(child);
			dc.addParam(relationType);
			dc.loadResult();
		} catch (DotDataException e) {
			throw new DotStateException(e);
		}
	}


	public static void deleteTreesByChildAndRelationType(Inode child, String relationType) {
		try {
			DotConnect dc = new DotConnect();
			dc.setSQL("delete from tree where child = ? and relation_type = ?");

			dc.addParam(child);
			dc.addParam(relationType);
			dc.loadResult();
		} catch (DotDataException e) {
		  throw new DotStateException(e);
		}
	}
	
	public static void deleteTreesByChild(Inode child) {
		try {
			DotConnect dc = new DotConnect();
			dc.setSQL("delete from tree where child =?");

			dc.addParam(child);

			dc.loadResult();
		} catch (DotDataException e) {
		  throw new DotStateException(e);
		}
	}

	public static void deleteTreesByChildId(final String childId) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL("DELETE FROM tree WHERE child = ?").addParam(childId).loadResult();
	}

	public static void deleteTreesByRelationType(String relationType) {
		try {
			DotConnect dc = new DotConnect();
			dc.setSQL("delete from tree where relation_type = ?");
			dc.addParam(relationType);
			dc.loadResult();
		} catch (DotDataException e) {
			throw new DotStateException(e);
		}
	}
	/**
	 * Does a upsert into the tree table
	 * @param tree
	 */
	public static void saveTree(Tree tree) {
		deleteTree(tree);
		insertTree(tree);
	}
	/**
	 * Does an insert into the tree table
	 * @param tree
	 */
	public static void insertTree(Tree tree) {
		DotConnect dc = new DotConnect();
		dc.setSQL("insert into tree (child,parent,relation_type,tree_order) values (?,?,?,?)");
		dc.addParam(tree.getChild());
		dc.addParam(tree.getParent());
		dc.addParam(tree.getRelationType());
		dc.addParam(tree.getTreeOrder());
		try {
			dc.loadResult();
		} catch (DotDataException e) {
			throw new DotStateException(e);
		}
	}
	
}
