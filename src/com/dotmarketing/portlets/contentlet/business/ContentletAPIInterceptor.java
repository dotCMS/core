/**
 * 
 */
package com.dotmarketing.portlets.contentlet.business;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryParser.ParseException;

import com.dotcms.content.business.DotMappingException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Interceptor;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * @author Jason Tesser
 * @since 1.6.5c
 *
 */
public class ContentletAPIInterceptor implements ContentletAPI, Interceptor {

	private List<ContentletAPIPreHook> preHooks = new ArrayList<ContentletAPIPreHook>();
	private List<ContentletAPIPostHook> postHooks = new ArrayList<ContentletAPIPostHook>();
	private ContentletAPI conAPI;
	
	public ContentletAPIInterceptor() {
		conAPI = APILocator.getContentletAPIImpl();
	}
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#addFileToContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, java.lang.String, java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public void addFileToContentlet(Contentlet contentlet, String fileInode, String relationName, User user, boolean respectFrontendRoles)	throws DotSecurityException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.addFileToContentlet(contentlet, fileInode, relationName, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.addFileToContentlet(contentlet, fileInode, relationName, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.addFileToContentlet(contentlet, fileInode, relationName, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#addImageToContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet,java.lang.String, java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public void addImageToContentlet(Contentlet contentlet, String imageInode, String relationName, User user, boolean respectFrontendRoles)	throws DotSecurityException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.addImageToContentlet(contentlet, imageInode, relationName, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.addImageToContentlet(contentlet, imageInode, relationName, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.addImageToContentlet(contentlet, imageInode, relationName, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#addLinkToContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet,java.lang.String, java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public void addLinkToContentlet(Contentlet contentlet, String linkInode,String relationName, User user, boolean respectFrontendRoles)	throws DotSecurityException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.addLinkToContentlet(contentlet, linkInode, relationName, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.addLinkToContentlet(contentlet, linkInode, relationName, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.addLinkToContentlet(contentlet, linkInode, relationName, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#archive(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public void archive(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.archive(contentlet, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.archive(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.archive(contentlet, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#archive(java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public void archive(List<Contentlet> contentlets, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.archive(contentlets, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.archive(contentlets, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.archive(contentlets, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#checkin(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.Map, java.util.List, java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public Contentlet checkin(Contentlet contentlet,Map<Relationship, List<Contentlet>> contentRelationships,List<Category> cats, List<Permission> permissions, User user,	boolean respectFrontendRoles) throws IllegalArgumentException,		DotDataException, DotSecurityException,		DotContentletStateException, DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkin(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Contentlet c = conAPI.checkin(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkin(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles, c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#checkin(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.ContentletRelationships, java.util.List, java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public Contentlet checkin(Contentlet currentContentlet,	ContentletRelationships relationshipsData, List<Category> cats,	List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles) throws IllegalArgumentException,	DotDataException, DotSecurityException,	DotContentletStateException, DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkin(currentContentlet, relationshipsData, cats, selectedPermissions, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Contentlet c = conAPI.checkin(currentContentlet, relationshipsData, cats, selectedPermissions, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkin(currentContentlet, relationshipsData, cats, selectedPermissions, user, respectFrontendRoles, c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#checkin(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.List, java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public Contentlet checkin(Contentlet contentlet, List<Category> cats,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,	DotDataException, DotSecurityException,	DotContentletStateException, DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkin(contentlet, cats, permissions, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Contentlet c = conAPI.checkin(contentlet, cats, permissions, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkin(contentlet, cats, permissions, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#checkin(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public Contentlet checkin(Contentlet contentlet,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,	DotDataException, DotSecurityException,DotContentletStateException, DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkin(contentlet, permissions, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Contentlet c = conAPI.checkin(contentlet, permissions, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkin(contentlet, permissions, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#checkin(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean, java.util.List)
	 */
	public Contentlet checkin(Contentlet contentlet, User user,boolean respectFrontendRoles, List<Category> cats) throws IllegalArgumentException, DotDataException,DotSecurityException, DotContentletStateException,	DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkin(contentlet, user, respectFrontendRoles, cats);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Contentlet c = conAPI.checkin(contentlet, user, respectFrontendRoles, cats);
		for(ContentletAPIPostHook post : postHooks){
			post.checkin(contentlet, user, respectFrontendRoles, cats, c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#checkin(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.Map, java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public Contentlet checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships,List<Category> cats, User user, boolean respectFrontendRoles)throws IllegalArgumentException, DotDataException,	DotSecurityException, DotContentletStateException,DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkin(contentlet, contentRelationships, cats, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Contentlet c = conAPI.checkin(contentlet, contentRelationships, cats, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkin(contentlet, contentRelationships, cats, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#checkin(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public Contentlet checkin(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws IllegalArgumentException, DotDataException, DotSecurityException,	DotContentletStateException, DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkin(contentlet, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Contentlet c = conAPI.checkin(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkin(contentlet, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#checkin(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.Map, com.liferay.portal.model.User, boolean)
	 */
	public Contentlet checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user, boolean respectFrontendRoles)	throws IllegalArgumentException, DotDataException,	DotSecurityException, DotContentletStateException,	DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkin(contentlet, contentRelationships, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Contentlet c = conAPI.checkin(contentlet, contentRelationships, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkin(contentlet, contentRelationships, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#checkinWithoutVersioning(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.Map, java.util.List, java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public Contentlet checkinWithoutVersioning(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships,	List<Category> cats, List<Permission> permissions, User user,	boolean respectFrontendRoles) throws DotDataException,	DotSecurityException, DotContentletStateException, DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkinWithoutVersioning(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Contentlet c = conAPI.checkinWithoutVersioning(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkinWithoutVersioning(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles, c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#checkout(String, com.liferay.portal.model.User, boolean)
	 */
	public Contentlet checkout(String contentletInode, User user,	boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkout(contentletInode, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Contentlet c = conAPI.checkout(contentletInode, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkout(contentletInode, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#checkout(java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public List<Contentlet> checkout(List<Contentlet> contentlets, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkout(contentlets, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.checkout(contentlets, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkout(contentlets, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#checkout(java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public List<Contentlet> checkoutWithQuery(String luceneQuery, User user,	boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException, ParseException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkoutWithQuery(luceneQuery, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.checkoutWithQuery(luceneQuery, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkout(luceneQuery, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#checkout(java.lang.String, com.liferay.portal.model.User, boolean, int, int)
	 */
	public List<Contentlet> checkout(String luceneQuery, User user, boolean respectFrontendRoles, int offset, int limit) throws DotDataException, DotSecurityException,	DotContentletStateException, ParseException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkout(luceneQuery, user, respectFrontendRoles, offset, limit);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.checkout(luceneQuery, user, respectFrontendRoles, offset, limit);
		for(ContentletAPIPostHook post : postHooks){
			post.checkout(luceneQuery, user, respectFrontendRoles, offset, limit,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#cleanField(com.dotmarketing.portlets.structure.model.Structure, com.dotmarketing.portlets.structure.model.Field, com.liferay.portal.model.User, boolean)
	 */
	public void cleanField(Structure structure, Field field, User user,	boolean respectFrontendRoles) throws DotSecurityException,	DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.cleanField(structure, field, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.cleanField(structure, field, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.cleanField(structure, field, user, respectFrontendRoles);
		}
	}


	public void cleanHostField(Structure structure, User user, boolean respectFrontendRoles)
			throws DotSecurityException, DotDataException, DotMappingException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.cleanHostField(structure, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.cleanHostField(structure, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.cleanHostField(structure, user, respectFrontendRoles);
		}
	}	
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#convertContentletToFatContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.contentlet.business.Contentlet)
	 */
	public com.dotmarketing.portlets.contentlet.business.Contentlet convertContentletToFatContentlet(Contentlet cont,com.dotmarketing.portlets.contentlet.business.Contentlet fatty)	throws DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.convertContentletToFatContentlet(cont, fatty);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		com.dotmarketing.portlets.contentlet.business.Contentlet c = conAPI.convertContentletToFatContentlet(cont, fatty);
		for(ContentletAPIPostHook post : postHooks){
			post.convertContentletToFatContentlet(cont, fatty, c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#convertFatContentletToContentlet(com.dotmarketing.portlets.contentlet.business.Contentlet)
	 */
	public Contentlet convertFatContentletToContentlet(com.dotmarketing.portlets.contentlet.business.Contentlet fatty) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.convertFatContentletToContentlet(fatty);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Contentlet c = conAPI.convertFatContentletToContentlet(fatty);
		for(ContentletAPIPostHook post : postHooks){
			post.convertFatContentletToContentlet(fatty, c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#copyProperties(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.Map)
	 */
	public void copyProperties(Contentlet contentlet, Map<String, Object> properties) throws DotContentletStateException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.copyProperties(contentlet, properties);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.copyProperties(contentlet, properties);
		for(ContentletAPIPostHook post : postHooks){
			post.copyProperties(contentlet, properties);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#delete(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public void delete(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException,	DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.delete(contentlet, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.delete(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.delete(contentlet, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#delete(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean, boolean)
	 */
	public void delete(Contentlet contentlet, User user, boolean respectFrontendRoles, boolean allVersions)	throws DotDataException, DotSecurityException,DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.delete(contentlet, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.delete(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.delete(contentlet, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#delete(java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public void delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.delete(contentlets, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.delete(contentlets, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.delete(contentlets, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#delete(java.util.List, com.liferay.portal.model.User, boolean, boolean)
	 */
	public void delete(List<Contentlet> contentlets, User user,	boolean respectFrontendRoles, boolean allVersions) throws DotDataException, DotSecurityException,	DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.delete(contentlets, user, respectFrontendRoles, allVersions);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.delete(contentlets, user, respectFrontendRoles, allVersions);
		for(ContentletAPIPostHook post : postHooks){
			post.delete(contentlets, user, respectFrontendRoles, allVersions);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#deleteOldContent(java.util.Date, int)
	 */
	public int deleteOldContent(Date deleteFrom, int offset) throws DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.deleteOldContent(deleteFrom, offset);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		int c = conAPI.deleteOldContent(deleteFrom, offset);
		for(ContentletAPIPostHook post : postHooks){
			post.deleteOldContent(deleteFrom, offset,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#deleteRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, com.liferay.portal.model.User, boolean)
	 */
	public void deleteRelatedContent(Contentlet contentlet, Relationship relationship, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.deleteRelatedContent(contentlet, relationship, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.deleteRelatedContent(contentlet, relationship, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.deleteRelatedContent(contentlet, relationship, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#deleteRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, boolean, com.liferay.portal.model.User, boolean)
	 */
	public void deleteRelatedContent(Contentlet contentlet, Relationship relationship, boolean hasParent, User user, boolean respectFrontendRoles) throws DotDataException,	DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.deleteRelatedContent(contentlet, relationship, hasParent, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.deleteRelatedContent(contentlet, relationship, hasParent, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.deleteRelatedContent(contentlet, relationship, hasParent, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#find(java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public Contentlet find(String inode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.find(inode, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Contentlet c = conAPI.find(inode, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.find(inode, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#find(long, com.liferay.portal.model.User, boolean)
	 */
	/*public Contentlet find(long inode, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.find(inode, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Contentlet c = conAPI.find(inode, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.find(inode, user, respectFrontendRoles,c);
		}
		return c;
	}*/

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#find(com.dotmarketing.portlets.categories.model.Category, long, boolean, java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public List<Contentlet> find(Category category, long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles) throws DotDataException,	DotContentletStateException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.find(category, languageId, live, orderBy, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.find(category, languageId, live, orderBy, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.find(category, languageId, live, orderBy, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#find(java.util.List, long, boolean, java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public List<Contentlet> find(List<Category> categories, long languageId, boolean live, String orderBy, User user,boolean respectFrontendRoles) throws DotDataException,	DotContentletStateException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.find(categories, languageId, live, orderBy, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.find(categories, languageId, live, orderBy, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.find(categories, languageId, live, orderBy, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#findAllContent(int, int)
	 */
	public List<Contentlet> findAllContent(int offset, int limit) throws DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findAllContent(offset, limit);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.findAllContent(offset, limit);
		for(ContentletAPIPostHook post : postHooks){
			post.findAllContent(offset, limit,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#findAllUserVersions(com.dotmarketing.beans.Identifier, com.liferay.portal.model.User, boolean)
	 */
	public List<Contentlet> findAllUserVersions(Identifier identifier,User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException, DotStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findAllUserVersions(identifier, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.findAllUserVersions(identifier, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findAllUserVersions(identifier, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#findAllVersions(com.dotmarketing.beans.Identifier, com.liferay.portal.model.User, boolean)
	 */
	public List<Contentlet> findAllVersions(Identifier identifier, User user,boolean respectFrontendRoles) throws DotSecurityException,	DotDataException, DotStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findAllVersions(identifier, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.findAllVersions(identifier, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findAllVersions(identifier, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#findByStructure(com.dotmarketing.portlets.structure.model.Structure, com.liferay.portal.model.User, boolean, int, int)
	 */
	public List<Contentlet> findByStructure(Structure structure, User user,	boolean respectFrontendRoles, int limit, int offset)	throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findByStructure(structure, user, respectFrontendRoles, limit, offset);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.findByStructure(structure, user, respectFrontendRoles, limit, offset);
		for(ContentletAPIPostHook post : postHooks){
			post.findByStructure(structure, user, respectFrontendRoles, limit, offset,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#findByStructure(java.lang.String, com.liferay.portal.model.User, boolean, int, int)
	 */
	public List<Contentlet> findByStructure(String structureInode, User user,	boolean respectFrontendRoles, int limit, int offset)	throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findByStructure(structureInode, user, respectFrontendRoles, limit, offset);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.findByStructure(structureInode, user, respectFrontendRoles, limit, offset);
		for(ContentletAPIPostHook post : postHooks){
			post.findByStructure(structureInode, user, respectFrontendRoles, limit, offset,c);
		}
		return c;
	}
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#findContentletByIdentifier(java.lang.String, boolean, long, com.liferay.portal.model.User, boolean)
	 */
	public Contentlet findContentletByIdentifier(String identifier, boolean live, long languageId, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException,	DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentletByIdentifier(identifier, live, languageId, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Contentlet c = conAPI.findContentletByIdentifier(identifier, live, languageId, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentletByIdentifier(identifier, live, languageId, user, respectFrontendRoles,c);
		}
		return c;
	}
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#findContentletForLanguage(long, com.dotmarketing.beans.Identifier)
	 */
	public Contentlet findContentletForLanguage(long languageId, Identifier contentletId) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentletForLanguage(languageId, contentletId);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Contentlet c = conAPI.findContentletForLanguage(languageId, contentletId);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentletForLanguage(languageId, contentletId,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#findContentlets(java.util.List)
	 */
	public List<Contentlet> findContentlets(List<String> inodes) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentlets(inodes);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.findContentlets(inodes);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentlets(inodes,c);
		}
		return c;
	}
	

	public List<Contentlet> findContentletsByFolder(Folder parentFolder, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentletsByFolder(parentFolder, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.findContentletsByFolder(parentFolder, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentletsByFolder(parentFolder, user, respectFrontendRoles);
		}
		return c;
	}

	public List<Contentlet> findContentletsByHost(Host parentHost, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentletsByHost(parentHost, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.findContentletsByHost(parentHost, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentletsByHost(parentHost, user, respectFrontendRoles);
		}
		return c;
	}	

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#findContentletsByIdentifiers(java.lang.String[], boolean, long, com.liferay.portal.model.User, boolean)
	 */
	public List<Contentlet> findContentletsByIdentifiers(String[] identifiers, boolean live, long languageId, User user,	boolean respectFrontendRoles) throws DotDataException,	DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentletsByIdentifiers(identifiers, live, languageId, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.findContentletsByIdentifiers(identifiers, live, languageId, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentletsByIdentifiers(identifiers, live, languageId, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#findFieldValues(java.lang.String, com.dotmarketing.portlets.structure.model.Field, com.liferay.portal.model.User, boolean)
	 */
	public List<String> findFieldValues(String structureInode, Field field,User user, boolean respectFrontEndRoles) throws DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findFieldValues(structureInode, field, user, respectFrontEndRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<String> c = conAPI.findFieldValues(structureInode, field, user, respectFrontEndRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findFieldValues(structureInode, field, user, respectFrontEndRoles,c);
		}
		return c;
	}

   /*
    * (non-Javadoc)
    * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#findPageContentlets(java.lang.String, java.lang.String, java.lang.String, boolean, long, com.liferay.portal.model.User, boolean)
    */
	public List<Contentlet> findPageContentlets(String HTMLPageIdentifier,String containerIdentifier, String orderby, boolean working,	long languageId, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findPageContentlets(HTMLPageIdentifier, containerIdentifier, orderby, working, languageId, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.findPageContentlets(HTMLPageIdentifier, containerIdentifier, orderby, working, languageId, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findPageContentlets(HTMLPageIdentifier, containerIdentifier, orderby, working, languageId, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#getAllLanguages(com.dotmarketing.portlets.contentlet.model.Contentlet, java.lang.Boolean, com.liferay.portal.model.User, boolean)
	 */
	public List<Contentlet> getAllLanguages(Contentlet contentlet, Boolean isLiveContent, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getAllLanguages(contentlet, isLiveContent, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.getAllLanguages(contentlet, isLiveContent, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getAllLanguages(contentlet, isLiveContent, user, respectFrontendRoles,c);
		}
		return c;
	}

	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#getAllRelationships(java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public ContentletRelationships getAllRelationships(String contentletInode, User user, boolean respectFrontendRoles) throws DotDataException,	DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getAllRelationships(contentletInode, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		ContentletRelationships c = conAPI.getAllRelationships(contentletInode, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getAllRelationships(contentletInode, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#getAllRelationships(com.dotmarketing.portlets.contentlet.model.Contentlet)
	 */
	public ContentletRelationships getAllRelationships(Contentlet contentlet) throws DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getAllRelationships(contentlet);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		ContentletRelationships c = conAPI.getAllRelationships(contentlet);
		for(ContentletAPIPostHook post : postHooks){
			post.getAllRelationships(contentlet,c);
		}
		return c;
	}

	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#getBinaryFile(java.lang.String, java.lang.String, com.liferay.portal.model.User)
	 */
	public File getBinaryFile(String contentletInode,	String velocityVariableName, User user) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getBinaryFile(contentletInode, velocityVariableName, user);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		File c = conAPI.getBinaryFile(contentletInode, velocityVariableName, user);
		for(ContentletAPIPostHook post : postHooks){
			post.getBinaryFile(contentletInode, velocityVariableName, user,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#getContentletReferences(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public List<Map<String, Object>> getContentletReferences(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException,	DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getContentletReferences(contentlet, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Map<String, Object>> c = conAPI.getContentletReferences(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getContentletReferences(contentlet, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#getFieldValue(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Field)
	 */
	public Object getFieldValue(Contentlet contentlet, Field theField) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getFieldValue(contentlet, theField);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Object c = conAPI.getFieldValue(contentlet, theField);
		for(ContentletAPIPostHook post : postHooks){
			post.getFieldValue(contentlet, theField,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#getName(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public String getName(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws DotSecurityException, DotContentletStateException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getName(contentlet, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		String c = conAPI.getName(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getName(contentlet, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#getNextReview(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public Date getNextReview(Contentlet content, User user, boolean respectFrontendRoles) throws DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getNextReview(content, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Date c = conAPI.getNextReview(content, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getNextReview(content, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#getRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, com.liferay.portal.model.User, boolean)
	 */
	public List<Contentlet> getRelatedContent(Contentlet contentlet,Relationship rel, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getRelatedContent(contentlet, rel, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.getRelatedContent(contentlet, rel, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getRelatedContent(contentlet, rel, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#getRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, boolean, com.liferay.portal.model.User, boolean)
	 */
	public List<Contentlet> getRelatedContent(Contentlet contentlet, Relationship rel, boolean pullByParent, User user,	boolean respectFrontendRoles) throws DotDataException,	DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getRelatedContent(contentlet, rel,pullByParent, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.getRelatedContent(contentlet, rel, pullByParent, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getRelatedContent(contentlet, rel, pullByParent, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#getRelatedFiles(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public List<com.dotmarketing.portlets.files.model.File> getRelatedFiles(Contentlet contentlet, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getRelatedFiles(contentlet, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<com.dotmarketing.portlets.files.model.File> c = conAPI.getRelatedFiles(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getRelatedFiles(contentlet, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#getRelatedIdentifier(com.dotmarketing.portlets.contentlet.model.Contentlet, java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public Identifier getRelatedIdentifier(Contentlet contentlet, String relationshipType, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getRelatedIdentifier(contentlet, relationshipType, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Identifier c = conAPI.getRelatedIdentifier(contentlet, relationshipType, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getRelatedIdentifier(contentlet, relationshipType, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#getRelatedLinks(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public List<Link> getRelatedLinks(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getRelatedLinks(contentlet, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Link> c = conAPI.getRelatedLinks(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getRelatedLinks(contentlet, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#indexSearch(java.lang.String, int, int, java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public List<ContentletSearch> searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles)	throws ParseException, DotSecurityException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.searchIndex(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<ContentletSearch> c = conAPI.searchIndex(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.searchIndex(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#isContentEqual(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public boolean isContentEqual(Contentlet contentlet1, Contentlet contentlet2, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isContentEqual(contentlet1, contentlet2, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		boolean c = conAPI.isContentEqual(contentlet1, contentlet2, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.isContentEqual(contentlet1, contentlet2, user, respectFrontendRoles,c);
		}
		return c;
	}

	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#isContentlet(java.lang.String)
	 */
	public boolean isContentlet(String inode) throws DotDataException, DotRuntimeException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isContentlet(inode);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		boolean c = conAPI.isContentlet(inode);
		for(ContentletAPIPostHook post : postHooks){
			post.isContentlet(inode,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#isFieldTypeBoolean(com.dotmarketing.portlets.structure.model.Field)
	 */
	public boolean isFieldTypeBoolean(Field field) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isFieldTypeBoolean(field);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		boolean c = conAPI.isFieldTypeBoolean(field);
		for(ContentletAPIPostHook post : postHooks){
			post.isFieldTypeBoolean(field,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#isFieldTypeDate(com.dotmarketing.portlets.structure.model.Field)
	 */
	public boolean isFieldTypeDate(Field field) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isFieldTypeDate(field);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		boolean c = conAPI.isFieldTypeDate(field);
		for(ContentletAPIPostHook post : postHooks){
			post.isFieldTypeDate(field,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#isFieldTypeFloat(com.dotmarketing.portlets.structure.model.Field)
	 */
	public boolean isFieldTypeFloat(Field field) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isFieldTypeFloat(field);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		boolean c = conAPI.isFieldTypeFloat(field);
		for(ContentletAPIPostHook post : postHooks){
			post.isFieldTypeFloat(field,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#isFieldTypeLong(com.dotmarketing.portlets.structure.model.Field)
	 */
	public boolean isFieldTypeLong(Field field) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isFieldTypeLong(field);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		boolean c = conAPI.isFieldTypeLong(field);
		for(ContentletAPIPostHook post : postHooks){
			post.isFieldTypeLong(field,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#isFieldTypeString(com.dotmarketing.portlets.structure.model.Field)
	 */
	public boolean isFieldTypeString(Field field) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isFieldTypeString(field);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		boolean c = conAPI.isFieldTypeString(field);
		for(ContentletAPIPostHook post : postHooks){
			post.isFieldTypeString(field,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#lock(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public void lock(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.lock(contentlet, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.lock(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.lock(contentlet, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#publish(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public void publish(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException, DotContentletStateException,	DotContentletStateException, DotStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.publish(contentlet, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.publish(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.publish(contentlet, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#publish(java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public void publish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException, DotContentletStateException, DotStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.publish(contentlets, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.publish(contentlets, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.publish(contentlets, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#publishRelatedHtmlPages(com.dotmarketing.portlets.contentlet.model.Contentlet)
	 */
	public void publishRelatedHtmlPages(Contentlet contentlet) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.publishRelatedHtmlPages(contentlet);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		try {
			conAPI.publishRelatedHtmlPages(contentlet);
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
		}
		for(ContentletAPIPostHook post : postHooks){
			post.publishRelatedHtmlPages(contentlet);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#reindex()
	 */
	@SuppressWarnings("deprecation")
	public void reindex() throws DotReindexStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.reindex();
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.reindex();
		for(ContentletAPIPostHook post : postHooks){
			post.reindex();
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#reindex(com.dotmarketing.portlets.structure.model.Structure)
	 */
	@SuppressWarnings("deprecation")
	public void reindex(Structure structure) throws DotReindexStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.reindex(structure);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.reindex(structure);
		for(ContentletAPIPostHook post : postHooks){
			post.reindex(structure);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#reindex(com.dotmarketing.portlets.contentlet.model.Contentlet)
	 */
	@SuppressWarnings("deprecation")
	public void reindex(Contentlet contentlet) throws DotReindexStateException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.reindex(contentlet);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.reindex(contentlet);
		for(ContentletAPIPostHook post : postHooks){
			post.reindex(contentlet);
		}
	}

	public void refresh(Structure structure) throws DotReindexStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.refresh(structure);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.refresh(structure);
		for(ContentletAPIPostHook post : postHooks){
			post.refresh(structure);
		}
		
	}

	public void refresh(Contentlet contentlet) throws DotReindexStateException,
			DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.refresh(contentlet);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.refresh(contentlet);
		for(ContentletAPIPostHook post : postHooks){
			post.refresh(contentlet);
		}
		
	}

	public void refreshAllContent() throws DotReindexStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.refreshAllContent();
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.refreshAllContent();
		for(ContentletAPIPostHook post : postHooks){
			post.refreshAllContent();
		}		
	}	

	public void refreshContentUnderHost(Host host) throws DotReindexStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.refreshContentUnderHost(host);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.refreshContentUnderHost(host);
		for(ContentletAPIPostHook post : postHooks){
			post.refreshContentUnderHost(host);
		}
		
	}
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#relateContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public void relateContent(Contentlet contentlet, Relationship rel, List<Contentlet> related, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException,	DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.relateContent(contentlet, rel, related, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.relateContent(contentlet, rel, related, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.relateContent(contentlet, rel, related, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#relateContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords, com.liferay.portal.model.User, boolean)
	 */
	public void relateContent(Contentlet contentlet, ContentletRelationshipRecords related, User user, boolean respectFrontendRoles) throws DotDataException,	DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.relateContent(contentlet, related, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.relateContent(contentlet, related, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.relateContent(contentlet, related, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#restoreVersion(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public void restoreVersion(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException,	DotContentletStateException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.restoreVersion(contentlet, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.restoreVersion(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.restoreVersion(contentlet, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#search(java.lang.String, int, int, java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public List<Contentlet> search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#search(java.lang.String, int, int, java.lang.String, com.liferay.portal.model.User, boolean, int)
	 */
	public List<Contentlet> search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException,	DotSecurityException, ParseException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission);
		for(ContentletAPIPostHook post : postHooks){
			post.search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#setContentletProperty(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Field, java.lang.Object)
	 */
	public void setContentletProperty(Contentlet contentlet, Field field, Object value) throws DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.setContentletProperty(contentlet, field, value);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.setContentletProperty(contentlet, field, value);
		for(ContentletAPIPostHook post : postHooks){
			post.setContentletProperty(contentlet, field, value);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#unarchive(java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public void unarchive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.unarchive(contentlets, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.unarchive(contentlets, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.unarchive(contentlets, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#unarchive(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public void unarchive(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.unarchive(contentlet, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.unarchive(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.unarchive(contentlet, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#unlock(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public void unlock(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException,	DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.unlock(contentlet, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.unlock(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.unlock(contentlet, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#unpublish(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public void unpublish(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws DotDataException,	DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.unpublish(contentlet, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.unpublish(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.unpublish(contentlet, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#unpublish(java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public void unpublish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.unpublish(contentlets, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.unpublish(contentlets, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.unpublish(contentlets, user, respectFrontendRoles);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#validateContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.List)
	 */
	public void validateContentlet(Contentlet contentlet, List<Category> cats) throws DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.validateContentlet(contentlet, cats);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.validateContentlet(contentlet, cats);
		for(ContentletAPIPostHook post : postHooks){
			post.validateContentlet(contentlet, cats);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#validateContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.Map, java.util.List)
	 */
	public void validateContentlet(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships,	List<Category> cats) throws DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.validateContentlet(contentlet, contentRelationships, cats);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.validateContentlet(contentlet, contentRelationships, cats);
		for(ContentletAPIPostHook post : postHooks){
			post.validateContentlet(contentlet, contentRelationships, cats);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#validateContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.ContentletRelationships, java.util.List)
	 */
	public void validateContentlet(Contentlet contentlet, ContentletRelationships contentRelationships, List<Category> cats) throws DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.validateContentlet(contentlet, contentRelationships, cats);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.validateContentlet(contentlet, contentRelationships, cats);
		for(ContentletAPIPostHook post : postHooks){
			post.validateContentlet(contentlet, contentRelationships, cats);
		}
	}

	public void addPreHook(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		Object o = Class.forName(className).newInstance();
		if(o instanceof ContentletAPIPreHook){
			preHooks.add((ContentletAPIPreHook)o);
		}else {
			throw new InstantiationException("This hook must implement ContentletAPIPrehook");
		}
	}

	public void addPostHook(String className, int indexToAddAt) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		Object o = Class.forName(className).newInstance();
		if(o instanceof ContentletAPIPostHook){
			postHooks.add(indexToAddAt,(ContentletAPIPostHook)o);
		}else {
			throw new InstantiationException("This hook must implement ContentletAPIPosthook");
		}
	}
	
	public void addPostHook(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		Object o = Class.forName(className).newInstance();
		if(o instanceof ContentletAPIPostHook){
			postHooks.add((ContentletAPIPostHook)o);
		}else {
			throw new InstantiationException("This hook must implement ContentletAPIPosthook");
		}		
	}
	
	public void addPreHook(String className, int indexToAddAt)	throws InstantiationException, IllegalAccessException,ClassNotFoundException {
		Object o = Class.forName(className).newInstance();
		if(o instanceof ContentletAPIPreHook){
			preHooks.add(indexToAddAt,(ContentletAPIPreHook)o);
		}else {
			throw new InstantiationException("This hook must implement ContentletAPIPrehook");
		}
	}

	public void delPreHood(int indexToRemAt) {
		postHooks.remove(indexToRemAt);
	}
	
	public void delPostHood(int indexToRemAt) {
		preHooks.remove(indexToRemAt);
	}

	public List<String> getPreHooks() {
		List<String> result = new ArrayList<String>();
		for (ContentletAPIPreHook hook : preHooks) {
			result.add(hook.getClass().getName());
		}
		return result;
	}
	
	public List<String> getPostHooks() {
		List<String> result = new ArrayList<String>();
		for (ContentletAPIPostHook hook : postHooks) {
			result.add(hook.getClass().getName());
		}
		return result;
	}

	public long contentletCount() throws DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.contentletCount();
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		long c = conAPI.contentletCount();
		for(ContentletAPIPostHook post : postHooks){
			post.contentletCount(c);
		}
		return c;
	}
	
	public long contentletIdentifierCount() throws DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.contentletIdentifierCount();
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		long c = conAPI.contentletIdentifierCount();
		for(ContentletAPIPostHook post : postHooks){
			post.contentletIdentifierCount(c);
		}
		return c;
	}
	
	public void deleteAllVersionsandBackup(List<Contentlet> contentlets,
			User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException, DotContentletStateException {

		
	}

	public List<Contentlet> getSiblings(String identifier)
			throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			 
			 boolean preResult = pre.getSiblings(identifier);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> contents= conAPI.getSiblings(identifier);
		for(ContentletAPIPostHook post : postHooks){
			post.getSiblings(identifier);
		}
		return contents;
	}

	public List<Map<String, Serializable>> DBSearch(Query query, User user,boolean respectFrontendRoles) throws ValidationException,
			DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.DBSearch(query, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Map<String, Serializable>> c = conAPI.DBSearch(query, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.DBSearch(query, user, respectFrontendRoles,c);
		}
		return c;
	}
	
	
	public Contentlet copyContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.copyContentlet(contentlet, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}	
		Contentlet c = conAPI.copyContentlet(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.copyContentlet(contentlet, user, respectFrontendRoles, c);
		}
		return c;
	}
	
	public Contentlet copyContentlet(Contentlet contentlet, Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.copyContentlet(contentlet, host, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}	
		Contentlet c = conAPI.copyContentlet(contentlet, host, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.copyContentlet(contentlet, host, user, respectFrontendRoles, c);
		}
		return c;
	}
	
	public Contentlet copyContentlet(Contentlet contentlet, Folder folder, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.copyContentlet(contentlet, folder, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}	
		Contentlet c = conAPI.copyContentlet(contentlet, folder, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.copyContentlet(contentlet, folder, user, respectFrontendRoles, c);
		}
		return c;
	}
	
	public Contentlet copyContentlet(Contentlet contentlet, Folder folder, User user, boolean appendCopyToFileName, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.copyContentlet(contentlet, folder, user, appendCopyToFileName, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}	
		Contentlet c = conAPI.copyContentlet(contentlet, folder, user, appendCopyToFileName, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.copyContentlet(contentlet, folder, user, appendCopyToFileName, respectFrontendRoles, c);
		}
		return c;
	}

	public boolean isInodeIndexed(String inode) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isInodeIndexed(inode);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		boolean c = conAPI.isInodeIndexed(inode);
		for(ContentletAPIPostHook post : postHooks){
			post.isInodeIndexed(inode,c);
		}
		return c;
	}

	public boolean isInodeIndexed(String inode, int secondsToWait) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isInodeIndexed(inode,secondsToWait);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		boolean c = conAPI.isInodeIndexed(inode,secondsToWait);
		for(ContentletAPIPostHook post : postHooks){
			post.isInodeIndexed(inode,secondsToWait,c);
		}
		return c;
	}

	public void UpdateContentWithSystemHost(String hostIdentifier)throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.UpdateContentWithSystemHost(hostIdentifier);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.UpdateContentWithSystemHost(hostIdentifier);
		for(ContentletAPIPostHook post : postHooks){
			post.UpdateContentWithSystemHost(hostIdentifier);
		}
	}
	
	public void removeUserReferences(String userId)throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.removeUserReferences(userId);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.removeUserReferences(userId);
		for(ContentletAPIPostHook post : postHooks){
			post.removeUserReferences(userId);
		}
		
	}

	public String getUrlMapForContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getUrlMapForContentlet(contentlet, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		String result = conAPI.getUrlMapForContentlet(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getUrlMapForContentlet(contentlet, user, respectFrontendRoles);
		}
		
		return result;
	}

	public void deleteVersion(Contentlet contentlet, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.deleteVersion(contentlet,user,respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.deleteVersion(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.deleteVersion(contentlet, user, respectFrontendRoles);
		}

		
	}

	public Contentlet saveDraft(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException{
		

		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.saveDraft(contentlet,contentRelationships, cats,permissions, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.saveDraft(contentlet,contentRelationships, cats,permissions, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.saveDraft(contentlet,contentRelationships, cats,permissions, user, respectFrontendRoles);
		}
		
		return contentlet;
	}
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#searchByIdentifier(java.lang.String, int, int, java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException, ParseException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,c);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#searchByIdentifier(java.lang.String, int, int, java.lang.String, com.liferay.portal.model.User, boolean, int)
	 */
	public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException,	DotSecurityException, ParseException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission);
		for(ContentletAPIPostHook post : postHooks){
			post.search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission,c);
		}
		return c;
	}
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#searchByIdentifier(java.lang.String, int, int, java.lang.String, com.liferay.portal.model.User, boolean, int, boolean)
	 */
	public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission, boolean anyLanguage) throws DotDataException,	DotSecurityException, ParseException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission,anyLanguage);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		List<Contentlet> c = conAPI.searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission,anyLanguage);
		for(ContentletAPIPostHook post : postHooks){
			post.searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission,anyLanguage);
		}
		return c;
	}	

	public void refreshContentUnderFolder(Folder folder)
			throws DotReindexStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.refreshContentUnderFolder(folder);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.refreshContentUnderFolder(folder);
		for(ContentletAPIPostHook post : postHooks){
			post.refreshContentUnderFolder(folder);
		}
	}
	
	public void removeFolderReferences(Folder folder) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.removeFolderReferences(folder);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		conAPI.removeFolderReferences(folder);
		for(ContentletAPIPostHook post : postHooks){
			post.removeFolderReferences(folder);
		}
	}
	
	public boolean canLock(Contentlet contentlet, User user) throws   DotLockException {
		boolean ret = true;
		for(ContentletAPIPreHook pre : preHooks){
			if(!pre.canLock(contentlet, user)){
				ret = false;
			}
		}
		if(!conAPI.canLock(contentlet, user)){
			ret = false;
		}
		for(ContentletAPIPostHook post : postHooks){
			if(!post.canLock(contentlet, user)){
				ret = false;
			}
		}
		return ret;
	}

	public Map<Relationship, List<Contentlet>> findContentRelationships(
			Contentlet contentlet, User user) throws DotDataException,
			DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentRelationships(contentlet, user);
			if(!preResult){
				Logger.error(this, "The following prehook failed " + pre.getClass().getName());
				throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
			}
		}
		Map<Relationship, List<Contentlet>> c = conAPI.findContentRelationships(contentlet, user);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentRelationships(contentlet, user);
		}
		return c;
	}

    public Object loadField(String inode, Field field) throws DotDataException {
        for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.loadField(inode,field);
            if(!preResult){
                Logger.error(this, "The following prehook failed " + pre.getClass().getName());
                throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
            }
        }
        
        Object value=conAPI.loadField(inode, field);
        
        for(ContentletAPIPostHook post : postHooks){
            post.loadField(inode,field,value);
        }
        return value;
    }

    @Override
    public long indexCount(String luceneQuery, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.indexCount(luceneQuery,user,respectFrontendRoles);
            if(!preResult){
                Logger.error(this, "The following prehook failed " + pre.getClass().getName());
                throw new DotRuntimeException("The following prehook failed " + pre.getClass().getName());
            }
        }
        
        long value=conAPI.indexCount(luceneQuery, user, respectFrontendRoles);
        
        for(ContentletAPIPostHook post : postHooks){
            post.indexCount(luceneQuery,user,respectFrontendRoles,value);
        }
        return value;
    }
	
}
