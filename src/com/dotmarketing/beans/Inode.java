/*
 * Bean.java
 *
 * Created on April 30, 2003, 4:36 PM
 */
package com.dotmarketing.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.categories.business.Categorizable;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;


/**
 * 
 * @author rocco
 * @author David H Torres (2009)
 */
public class Inode implements Serializable, Comparable, Permissionable,Versionable,
		Categorizable, UUIDable {

	private static final long serialVersionUID = -152856052702254985L;

	private java.util.Date iDate;

	private String type = "";

	protected String owner = "";

	protected String inode = "";
	
	protected String identifier = "";

	public Inode() {
		iDate = new java.util.Date();
		type = "inode";
	}
	public Date getModDate() {
		return Calendar.getInstance().getTime();
	}

	public String getModUser() {
		return "";
	}
	
	public String getCategoryId() {
		return getInode();
	}

	/**
	 * Sets the iDate.
	 * 
	 * @param iDate
	 *            The iDate to set
	 */
	public void setIDate(java.util.Date iDate) {
		this.iDate = iDate;
	}

	// sets the idate from the db
	public void setIDate(String x) {
		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(
				"yyyy-MM-dd H:mm:ss");
		java.text.ParsePosition pos = new java.text.ParsePosition(0);
		iDate = formatter.parse(x, pos);

		if (iDate == null) {
			iDate = new java.util.Date();
		}
	}

	/**
	 * Returns the iDate.
	 * 
	 * @return java.util.Date
	 */
	public java.util.Date getIDate() {
		return iDate;
	}

	/**
	 * Sets the inode.
	 * 
	 * @param inode
	 *            The inode to set
	 */
	public void setInode(String inode) {
		if(inode == null||inode == "")
			this.inode = null;
		else if (inode.contains("-")) {
			UUID uuid = UUID.fromString(inode);
			this.inode = uuid.toString();
		}else {
			try {
				long oldInode = Long.parseLong(inode);
				this.inode = Long.valueOf(oldInode).toString();
			} catch (Exception e) {
				this.inode = "";
			}
		}

	}

	/*
	 * public void setInode(long inode) { this.inode = inode + ""; }
	 */

	public String getInode() {
		if (inode != null) {
			if (inode.contains("-")) {
				UUID uuid = UUID.fromString(inode);
				return uuid.toString();
			}else {
				try {
					long oldInode = Long.valueOf(inode);
					return Long.valueOf(oldInode).toString();
				} catch (Exception e) {
					return "";
				}
			}
		} else
			return "";
	}
	
	/**
	 * @return Returns the identifier.
	 */
	public String getIdentifier() {
		if(InodeUtils.isSet(identifier))
		    return identifier;
		return null;
	}
	/*public String getIdentifier() {
		try {
			  if(InodeUtils.isSet(identifier))
				 return identifier;
			   if(InodeUtils.isSet(inode)){
		   		 Identifier id =  APILocator.getIdentifierAPI().find(inode);
		   		 setIdentifier(id.getInode());
		   		 return "";
		   	  } 
			 } catch (DotHibernateException e) {
				Logger.error(this, "Unable to retrieve the identifier.", e);
			}
			return null;
			if(InodeUtils.isSet(identifier))
				return identifier;
			else 
				return null;
	}*/
	
	/**
	 * @param identifier
	 *            The identifier to set.
	 */
	
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * Sets the owner.
	 * 
	 * @param owner
	 *            The owner to set
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	/**
	 * Returns the owner.
	 * 
	 * @return int
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * Sets the type.
	 * 
	 * @param type
	 *            The type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Returns the type.
	 * 
	 * @return String
	 */
	public String getType() {
		return type;
	}

	// Tree relationship methods
	// All these methods have been deprecated because apis should be used
	// instead,
	// the best example is the categoryapi that maintains caches of the
	// relationship and also
	// ensure permissions when associating categories, that's why the usage of
	// these methods
	// for categories is totally forbidden.

	/**
	 * @deprecated Association between inodes should be called through their
	 *             respective API, calling the API ensures the consistency of
	 *             the relationship and caches
	 */
	public void addChild(Inode i) {
		if (this instanceof Category || i instanceof Category) {
			throw new DotRuntimeException(
					"Usage of this method directly is forbidden please go through the APIs directly");
		}
		Tree tree = TreeFactory.getTree(this, i, "child");
		if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())) {
			tree.setParent(this.inode);
			tree.setChild(i.getInode());
			tree.setRelationType("child");
			TreeFactory.saveTree(tree);
		}
	}
	
	public void addChild(Identifier i) {
		Tree tree = TreeFactory.getTree(this.inode, i.getInode(), "child");
		if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())) {
			tree.setParent(this.inode);
			tree.setChild(i.getInode());
			tree.setRelationType("child");
			TreeFactory.saveTree(tree);
		}
	}

	/**
	 * @deprecated Association between inodes should be called through their
	 *             respective API, calling the API ensures the consistency of
	 *             the relationship and caches
	 */
	public void addChild(Inode i, String relationType) {
		if (this instanceof Category || i instanceof Category) {
			throw new DotRuntimeException(
					"Usage of this method directly is forbidden please go through the APIs directly");
		}
		Tree tree = TreeFactory.getTree(this, i, relationType);
		if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())){
			tree.setParent(this.inode);
			tree.setChild(i.getInode());
			tree.setRelationType(relationType);
			TreeFactory.saveTree(tree);
		} else {
			tree.setRelationType(relationType);
			TreeFactory.saveTree(tree);
		}
	}
	
	public void addChild(Identifier i, String relationType) {
		Tree tree = TreeFactory.getTree(this.inode, i.getInode(), relationType);
		if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())){
			tree.setParent(this.inode);
			tree.setChild(i.getInode());
			tree.setRelationType(relationType);
			TreeFactory.saveTree(tree);
		} else {
			tree.setRelationType(relationType);
			TreeFactory.saveTree(tree);
		}
	}

	/**
	 * @deprecated Association between inodes should be called through their
	 *             respective API, calling the API ensures the consistency of
	 *             the relationship and caches
	 */
	public void addChild(Inode i, String relationType, int sortOrder) {
		if (this instanceof Category || i instanceof Category) {
			throw new DotRuntimeException(
					"Usage of this method directly is forbidden please go through the APIs directly");
		}
		Tree tree = TreeFactory.getTree(this, i, relationType);
		if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())) {
			tree.setParent(this.inode);
			tree.setChild(i.getInode());
			tree.setRelationType(relationType);
			tree.setTreeOrder(sortOrder);
			TreeFactory.saveTree(tree);
		} else {
			tree.setRelationType(relationType);
			tree.setTreeOrder(sortOrder);
			TreeFactory.saveTree(tree);
		}
	}

	/**
	 * @deprecated Association between inodes should be called through their
	 *             respective API, calling the API ensures the consistency of
	 *             the relationship and caches
	 */
	public void addParent(Inode i) {
		if (this instanceof Category || i instanceof Category) {
			throw new DotRuntimeException(
					"Usage of this method directly is forbidden please go through the APIs directly");
		}
		Tree tree = TreeFactory.getTree(i, this, "child");
		if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())) {
			tree.setChild(this.inode);
			tree.setParent(i.getInode());
			tree.setRelationType("child");
			TreeFactory.saveTree(tree);
		}
	}
	
	public void addParent(Identifier i) {
		Tree tree = TreeFactory.getTree(i.getInode(), this.inode, "child");
		if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())) {
			tree.setChild(this.inode);
			tree.setParent(i.getInode());
			tree.setRelationType("child");
			TreeFactory.saveTree(tree);
		}
	}

	/**
	 * @deprecated Association between inodes should be called through their
	 *             respective API, calling the API ensures the consistency of
	 *             the relationship and caches
	 */
	public void addParent(Inode i, String relationType) {
		if (this instanceof Category || i instanceof Category) {
			throw new DotRuntimeException(
					"Usage of this method directly is forbidden please go through the APIs directly");
		}
		Tree tree = TreeFactory.getTree(i, this, relationType);
		if (!InodeUtils.isSet(tree.getParent()) ||!InodeUtils.isSet(tree.getChild())) {
			tree.setChild(this.inode);
			tree.setParent(i.getInode());
			tree.setRelationType(relationType);
			TreeFactory.saveTree(tree);
		} else {
			tree.setRelationType(relationType);
			TreeFactory.saveTree(tree);
		}
	}

	/**
	 * Remove the ASSOCIATION between a child and the inode
	 * 
	 * @param child
	 *            child to be dissociated
	 * @return
	 * @deprecated Association between inodes should be called through their
	 *             respective API, calling the API ensures the consistency of
	 *             the relationship and caches
	 */
	public boolean deleteChild(Inode child) {
		if (this instanceof Category || child instanceof Category) {
			throw new DotRuntimeException(
					"Usage of this method directly is forbidden please go through the APIs directly");
		}
		Tree tree = TreeFactory.getTree(this, child, "child");
		if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())) {
			return false;
		}
		TreeFactory.deleteTree(tree);
		return true;
	}
	
	public boolean deleteChild(Identifier child) {
		Tree tree = TreeFactory.getTree(this.inode, child.getInode(), "child");
		if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())) {
			return false;
		}
		TreeFactory.deleteTree(tree);
		return true;
	}

	/**
	 * Remove the ASSOCIATION between a child and the inode
	 * 
	 * @param child
	 *            child to be dissociated
	 * @return
	 * @deprecated Association between inodes should be called through their
	 *             respective API, calling the API ensures the consistency of
	 *             the relationship and caches
	 */
	public boolean deleteChild(Inode child, String relationType) {
		if (this instanceof Category || child instanceof Category) {
			throw new DotRuntimeException(
					"Usage of this method directly is forbidden please go through the APIs directly");
		}
		Tree tree = TreeFactory.getTree(this, child, relationType);
		if (!InodeUtils.isSet(tree.getParent())|| !InodeUtils.isSet(tree.getChild())) {
			return false;
		}
		TreeFactory.deleteTree(tree);
		return true;
	}
	
	public boolean deleteChild(Identifier child, String relationType) {
		Tree tree = TreeFactory.getTree(this.inode, child.getInode(), relationType);
		if (!InodeUtils.isSet(tree.getParent())|| !InodeUtils.isSet(tree.getChild())) {
			return false;
		}
		TreeFactory.deleteTree(tree);
		return true;
	}

	/**
	 * Remove the ASSOCIATION between a parent and the inode
	 * 
	 * @param parent
	 *            parent to be dissociated
	 * @deprecated Association between inodes should be called through their
	 *             respective API, calling the API ensures the consistency of
	 *             the relationship and caches
	 * @return
	 */
	public boolean deleteParent(Inode parent) {
		if (this instanceof Category || parent instanceof Category) {
			throw new DotRuntimeException(
					"Usage of this method directly is forbidden please go through the APIs directly");
		}
		Tree tree = TreeFactory.getTree(parent, this);
		if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())) {
			return false;
		}
		TreeFactory.deleteTree(tree);
		return true;
	}

	/**
	 * @deprecated Association between inodes should be called through their
	 *             respective API, calling the API ensures the consistency of
	 *             the relationship and caches
	 */
	public boolean deleteParent(Inode parent, String relationType) {
		if (this instanceof Category || parent instanceof Category) {
			throw new DotRuntimeException(
					"Usage of this method directly is forbidden please go through the APIs directly");
		}
		Tree tree = TreeFactory.getTree(parent, this, relationType);
		if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())) {
			return false;
		}
		TreeFactory.deleteTree(tree);
		return true;
	}

	/**
	 * Wipe out the old parents and associate a new parents set to the inode
	 * 
	 * @param newChildren
	 *            New children set
	 * @deprecated Association between inodes should be called through their
	 *             respective API, calling the API ensures the consistency of
	 *             the relationship and caches
	 */
	public void setParents(List newParents) {

		// Checking for forbidden usage
		boolean invalidParent = false;
		for (Object parent : newParents) {
			if (parent instanceof Category)
				invalidParent = true;
		}
		if (this instanceof Category || invalidParent) {
			throw new DotRuntimeException(
					"Usage of this method directly is forbidden please go through the APIs directly");
		}

		TreeFactory.deleteTreesByChild(this);
		Iterator it = newParents.iterator();
		while (it.hasNext()) {
			Inode parent = (Inode) it.next();
			addParent(parent);
		}
	}

	/**
	 * 
	 * @return If the inode has children return true, false otherwise
	 */
	public boolean hasChildren() {

		DotConnect dc = new DotConnect();
		String query = "select count(*) as c from tree where tree.parent = '"
				+ this.inode + "'";
		dc.setSQL(query);

		return dc.getInt("c") > 0;
	}

	/**
	 * 
	 * @return If the inode has children return true, false otherwise
	 */
	public boolean hasParents() {

		HibernateUtil dh = new HibernateUtil();
		String query = "select count(*) from " + Inode.class.getName()
				+ "  inode where inode.inode in (select tree.parent from "
				+ Tree.class.getName() + "  tree where tree.child = "
				+ this.inode + ")";
		List results = new ArrayList();
		try {
			dh.setQuery(query);
			results = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(this.getClass(), e.getMessage(), e);
		}
		return ((Integer) results.get(0)).intValue() > 0;
	}

	public boolean equals(Object other) {
		if (!(other instanceof Inode)) {
			return false;
		}

		Inode castOther = (Inode) other;

		return (this.getInode().equalsIgnoreCase(castOther.getInode()));
	}

	@Override
	public int hashCode() {
		return inode.hashCode();
	}

	public java.util.Date getiDate() {
		return iDate;
	}

	public void setiDate(java.util.Date iDate) {
		this.iDate = iDate;
	}

	public boolean isNew() {
		return (!InodeUtils.isSet(this.inode));
	}

	/**
	 * Returns a hashmap with all the inode fields
	 * 
	 * @return the map
	 */
	public Map<String, Object> getMap()  throws DotStateException, DotDataException, DotSecurityException {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("inode", inode);
		map.put("type", type);
		map.put("identifier", this.identifier);
		map.put("iDate", iDate);
		return map;
	}

	public int compareTo(Object compObject) {
		if (!(compObject instanceof Inode))
			return -1;

		Inode inode = (Inode) compObject;
		return (inode.getiDate().compareTo(this.getiDate()));
	}

	public String getPermissionId() {
		return getInode();
	}

	public List<PermissionSummary> acceptedPermissions() {
		return null;
	}

	public List<RelatedPermissionableGroup> permissionDependencies(
			int requiredPermission) {
		return null;
	}

	public Permissionable getParentPermissionable() throws DotDataException {
		return null;
	}

	public String getPermissionType() {
		return this.getClass().getCanonicalName();
	}

	public boolean isParentPermissionable() {
		return false;
	}
	public String getTitle() {
		return "";
	}
	public String getVersionId() {
		return getIdentifier();
	}
	public String getVersionType() {
		return getType();
	}
	public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
		return false;
	}
	public boolean isLive() throws DotStateException, DotDataException, DotSecurityException {
		return false;
	}
	public boolean isLocked() throws DotStateException, DotDataException, DotSecurityException {
		return false;
	}
	public boolean isWorking() throws DotStateException, DotDataException, DotSecurityException {
		return false;
	}
	public void setVersionId(String versionId) {
		setIdentifier(versionId);
	}

}