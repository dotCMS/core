/**
 * // DOTCMS - 3800
 */
package com.dotmarketing.plugins.hello.world;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryParser.ParseException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * @author jasontesser
 *
 */
public class HelloWorldContentletPreHook implements ContentletAPIPreHook {
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#addFileToContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, java.lang.String, java.lang.String, com.liferay.portal.model.User, boolean)
     */
	public boolean addFileToContentlet(Contentlet contentlet, String fileInode,
			String relationName, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#addImageToContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, java.lang.String, java.lang.String, com.liferay.portal.model.User, boolean)
     */
	public boolean addImageToContentlet(Contentlet contentlet,
			String imageInode, String relationName, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#addLinkToContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, java.lang.String, java.lang.String, com.liferay.portal.model.User, boolean)
     */
	public boolean addLinkToContentlet(Contentlet contentlet, String linkInode,
			String relationName, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#applyStructurePermissionsToChildren(com.dotmarketing.portlets.structure.model.Structure, com.liferay.portal.model.User, java.util.List, boolean)
     */
	public boolean applyStructurePermissionsToChildren(Structure structure,
			User user, List<Permission> permissions,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#archive(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
     */
	public boolean archive(Contentlet contentlet, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#archive(java.util.List, com.liferay.portal.model.User, boolean)
     */
	public boolean archive(List<Contentlet> contentlets, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#checkin(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.Map, java.util.List, java.util.List, com.liferay.portal.model.User, boolean)
     */
	public boolean checkin(Contentlet contentlet,
			Map<Relationship, List<Contentlet>> contentRelationships,
			List<Category> cats, List<Permission> permissions, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#checkin(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.ContentletRelationships, java.util.List, java.util.List, com.liferay.portal.model.User, boolean)
     */
	public boolean checkin(Contentlet currentContentlet,
			ContentletRelationships relationshipsData, List<Category> cats,
			List<Permission> selectedPermissions, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#checkin(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.List, java.util.List, com.liferay.portal.model.User, boolean)
     */
	public boolean checkin(Contentlet contentlet, List<Category> cats,
			List<Permission> permissions, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#checkin(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.List, com.liferay.portal.model.User, boolean)
     */
	public boolean checkin(Contentlet contentlet, List<Permission> permissions,
			User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#checkin(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean, java.util.List)
     */
	public boolean checkin(Contentlet contentlet, User user,
			boolean respectFrontendRoles, List<Category> cats) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#checkin(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.Map, java.util.List, com.liferay.portal.model.User, boolean)
     */
	public boolean checkin(Contentlet contentlet,
			Map<Relationship, List<Contentlet>> contentRelationships,
			List<Category> cats, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#checkin(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
     */
	public boolean checkin(Contentlet contentlet, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#checkin(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.Map, com.liferay.portal.model.User, boolean)
     */
	public boolean checkin(Contentlet contentlet,
			Map<Relationship, List<Contentlet>> contentRelationships,
			User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#checkinWithoutVersioning(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.Map, java.util.List, java.util.List, com.liferay.portal.model.User, boolean)
     */
	public boolean checkinWithoutVersioning(Contentlet contentlet,
			Map<Relationship, List<Contentlet>> contentRelationships,
			List<Category> cats, List<Permission> permissions, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#checkout(java.lang.String, com.liferay.portal.model.User, boolean)
     */
	public boolean checkout(String contentletInode, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		Logger.info(this, "The helloworld plugin prehook about to get contentlet with inode " + contentletInode);
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#checkout(java.util.List, com.liferay.portal.model.User, boolean)
     */
	public boolean checkout(List<Contentlet> contentlets, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#checkout(java.lang.String, com.liferay.portal.model.User, boolean, int, int)
     */
	public boolean checkout(String luceneQuery, User user,
			boolean respectFrontendRoles, int offset, int limit) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#checkoutWithQuery(java.lang.String, com.liferay.portal.model.User, boolean)
     */
	public boolean checkoutWithQuery(String luceneQuery, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#cleanField(com.dotmarketing.portlets.structure.model.Structure, com.dotmarketing.portlets.structure.model.Field, com.liferay.portal.model.User, boolean)
     */
	public boolean cleanField(Structure structure, Field field, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#contentletCount()
     */
	public boolean contentletCount() throws DotDataException {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#contentletIdentifierCount()
     */
	public boolean contentletIdentifierCount() throws DotDataException {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#convertContentletToFatContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.contentlet.business.Contentlet)
     */
	public boolean convertContentletToFatContentlet(Contentlet cont,
			com.dotmarketing.portlets.contentlet.business.Contentlet fatty) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#convertFatContentletToContentlet(com.dotmarketing.portlets.contentlet.business.Contentlet)
     */
	public boolean convertFatContentletToContentlet(
			com.dotmarketing.portlets.contentlet.business.Contentlet fatty) {
		// TODO Auto-generated method stub
		return true;
	}
   /*
    * (non-Javadoc)
    * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#copyContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
    */
	public boolean copyContentlet(Contentlet currentContentlet, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#copyProperties(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.Map)
     */
	public boolean copyProperties(Contentlet contentlet,
			Map<String, Object> properties) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#delete(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
     */
	public boolean delete(Contentlet contentlet, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
    /*
     * (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#delete(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean, boolean)
     */
	public boolean delete(Contentlet contentlet, User user,
			boolean respectFrontendRoles, boolean allVersions) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#delete(java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public boolean delete(List<Contentlet> contentlets, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#delete(java.util.List, com.liferay.portal.model.User, boolean, boolean)
	 */
	public boolean delete(List<Contentlet> contentlets, User user,
			boolean respectFrontendRoles, boolean allVersions) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#deleteOldContent(java.util.Date, int)
	 */
	public boolean deleteOldContent(Date deleteFrom, int offset) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#deleteRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, com.liferay.portal.model.User, boolean)
	 */
	public boolean deleteRelatedContent(Contentlet contentlet,
			Relationship relationship, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#deleteRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, boolean, com.liferay.portal.model.User, boolean)
	 */
	public boolean deleteRelatedContent(Contentlet contentlet,
			Relationship relationship, boolean hasParent, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#find(java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public boolean find(String inode, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#find(com.dotmarketing.portlets.categories.model.Category, long, boolean, java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public boolean find(Category category, long languageId, boolean live,
			String orderBy, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#find(java.util.List, long, boolean, java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public boolean find(List<Category> categories, long languageId,
			boolean live, String orderBy, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#findAllContent(int, int)
	 */
	public boolean findAllContent(int offset, int limit) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#findAllUserVersions(com.dotmarketing.beans.Identifier, com.liferay.portal.model.User, boolean)
	 */
	public boolean findAllUserVersions(Identifier identifier, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#findAllVersions(com.dotmarketing.beans.Identifier, com.liferay.portal.model.User, boolean)
	 */
	public boolean findAllVersions(Identifier identifier, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#findByStructure(com.dotmarketing.portlets.structure.model.Structure, com.liferay.portal.model.User, boolean, int, int)
	 */
	public boolean findByStructure(Structure structure, User user,
			boolean respectFrontendRoles, int limit, int offset) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#findByStructure(java.lang.String, com.liferay.portal.model.User, boolean, int, int)
	 */
	public boolean findByStructure(String structureInode, User user,
			boolean respectFrontendRoles, int limit, int offset) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#findContentletByIdentifier(java.lang.String, boolean, long, com.liferay.portal.model.User, boolean)
	 */
	public boolean findContentletByIdentifier(String identifier, boolean live,
			long languageId, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#findContentletForLanguage(long, com.dotmarketing.beans.Identifier)
	 */
	public boolean findContentletForLanguage(long languageId,
			Identifier contentletId) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#findContentlets(java.util.List)
	 */
	public boolean findContentlets(List<String> inodes) throws DotDataException {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#findContentletsByIdentifiers(java.lang.String[], boolean, long, com.liferay.portal.model.User, boolean)
	 */
	public boolean findContentletsByIdentifiers(String[] identifiers,
			boolean live, long languageId, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#findFieldValues(java.lang.String, com.dotmarketing.portlets.structure.model.Field, com.liferay.portal.model.User, boolean)
	 */
	public boolean findFieldValues(String structureInode, Field field,
			User user, boolean respectFrontEndRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#findPageContentlets(java.lang.String, java.lang.String, java.lang.String, boolean, long, com.liferay.portal.model.User, boolean)
	 */
	public boolean findPageContentlets(String HTMLPageIdentifier,
			String containerIdentifier, String orderby, boolean working,
			long languageId, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#getAllLanguages(com.dotmarketing.portlets.contentlet.model.Contentlet, java.lang.Boolean, com.liferay.portal.model.User, boolean)
	 */
	public boolean getAllLanguages(Contentlet contentlet,
			Boolean isLiveContent, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#getAllRelationships(java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public boolean getAllRelationships(String contentletInode, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#getAllRelationships(com.dotmarketing.portlets.contentlet.model.Contentlet)
	 */
	public boolean getAllRelationships(Contentlet contentlet) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#getBinaryFile(java.lang.String, java.lang.String, com.liferay.portal.model.User)
	 */
	public boolean getBinaryFile(String contentletInode,
			String velocityVariableName, User user) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#getContentletReferences(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public boolean getContentletReferences(Contentlet contentlet, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#getFieldValue(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Field)
	 */
	public boolean getFieldValue(Contentlet contentlet, Field theField) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#getName(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public boolean getName(Contentlet contentlet, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#getNextReview(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public boolean getNextReview(Contentlet content, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#getReferencingContentlet(com.dotmarketing.portlets.files.model.File, boolean, com.liferay.portal.model.User, boolean)
	 */
	public boolean getReferencingContentlet(File file, boolean live, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#getRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, com.liferay.portal.model.User, boolean)
	 */
	public boolean getRelatedContent(Contentlet contentlet, Relationship rel,
			User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#getRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, boolean, com.liferay.portal.model.User, boolean)
	 */
	public boolean getRelatedContent(Contentlet contentlet, Relationship rel,
			boolean pullByParent, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#getRelatedFiles(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public boolean getRelatedFiles(Contentlet contentlet, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#getRelatedIdentifier(com.dotmarketing.portlets.contentlet.model.Contentlet, java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public boolean getRelatedIdentifier(Contentlet contentlet,
			String relationshipType, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#getRelatedLinks(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public boolean getRelatedLinks(Contentlet contentlet, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#indexSearch(java.lang.String, int, int, java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public boolean indexSearch(String luceneQuery, int limit, int offset,
			String sortBy, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#isContentEqual(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public boolean isContentEqual(Contentlet contentlet1,
			Contentlet contentlet2, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#isContentlet(java.lang.String)
	 */
	public boolean isContentlet(String inode) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#isFieldTypeBoolean(com.dotmarketing.portlets.structure.model.Field)
	 */
	public boolean isFieldTypeBoolean(Field field) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#isFieldTypeDate(com.dotmarketing.portlets.structure.model.Field)
	 */
	public boolean isFieldTypeDate(Field field) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#isFieldTypeFloat(com.dotmarketing.portlets.structure.model.Field)
	 */
	public boolean isFieldTypeFloat(Field field) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#isFieldTypeLong(com.dotmarketing.portlets.structure.model.Field)
	 */
	public boolean isFieldTypeLong(Field field) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#isFieldTypeString(com.dotmarketing.portlets.structure.model.Field)
	 */
	public boolean isFieldTypeString(Field field) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#lock(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public boolean lock(Contentlet contentlet, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#publish(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public boolean publish(Contentlet contentlet, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#publish(java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public boolean publish(List<Contentlet> contentlets, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#publishRelatedHtmlPages(com.dotmarketing.portlets.contentlet.model.Contentlet)
	 */
	public boolean publishRelatedHtmlPages(Contentlet contentlet) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#reIndexForServerNode()
	 */
	public boolean reIndexForServerNode() {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#refreshReferencingContentlets(com.dotmarketing.portlets.files.model.File, boolean)
	 */
	public boolean refreshReferencingContentlets(File file, boolean live) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#reindex()
	 */
	public boolean reindex() {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#reindex(com.dotmarketing.portlets.structure.model.Structure)
	 */
	public boolean reindex(Structure structure) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#reindex(com.dotmarketing.portlets.contentlet.model.Contentlet)
	 */
	public boolean reindex(Contentlet contentlet) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#relateContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public boolean relateContent(Contentlet contentlet, Relationship rel,
			List<Contentlet> related, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#relateContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords, com.liferay.portal.model.User, boolean)
	 */
	public boolean relateContent(Contentlet contentlet,
			ContentletRelationshipRecords related, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#removeContentletFromIndex(java.lang.String)
	 */
	public boolean removeContentletFromIndex(String contentletInodeOrIdentifier)
			throws DotDataException {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#restoreVersion(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public boolean restoreVersion(Contentlet contentlet, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#search(java.lang.String, int, int, java.lang.String, com.liferay.portal.model.User, boolean)
	 */
	public boolean search(String luceneQuery, int limit, int offset,
			String sortBy, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#search(java.lang.String, int, int, java.lang.String, com.liferay.portal.model.User, boolean, int)
	 */
	public boolean search(String luceneQuery, int limit, int offset,
			String sortBy, User user, boolean respectFrontendRoles,
			int requiredPermission) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#setContentletProperty(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Field, java.lang.Object)
	 */
	public boolean setContentletProperty(Contentlet contentlet, Field field,
			Object value) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#unarchive(java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public boolean unarchive(List<Contentlet> contentlets, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#unarchive(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public boolean unarchive(Contentlet contentlet, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#unlock(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public boolean unlock(Contentlet contentlet, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#unpublish(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */
	public boolean unpublish(Contentlet contentlet, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#unpublish(java.util.List, com.liferay.portal.model.User, boolean)
	 */
	public boolean unpublish(List<Contentlet> contentlets, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#validateContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.List)
	 */
	public boolean validateContentlet(Contentlet contentlet, List<Category> cats) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#validateContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, java.util.Map, java.util.List)
	 */
	public boolean validateContentlet(Contentlet contentlet,
			Map<Relationship, List<Contentlet>> contentRelationships,
			List<Category> cats) {
		// TODO Auto-generated method stub
		return true;
	}
	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook#validateContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.ContentletRelationships, java.util.List)
	 */
	public boolean validateContentlet(Contentlet contentlet,
			ContentletRelationships contentRelationships, List<Category> cats) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean checkinWithNoIndex(Contentlet contentlet,
			Map<Relationship, List<Contentlet>> contentRelationships,
			List<Category> cats, List<Permission> permissions, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean checkinWithNoIndex(Contentlet currentContentlet,
			ContentletRelationships relationshipsData, List<Category> cats,
			List<Permission> selectedPermissions, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean checkinWithNoIndex(Contentlet contentlet,
			List<Category> cats, List<Permission> permissions, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean checkinWithNoIndex(Contentlet contentlet,
			List<Permission> permissions, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean checkinWithNoIndex(Contentlet contentlet, User user,
			boolean respectFrontendRoles, List<Category> cats) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean checkinWithNoIndex(Contentlet contentlet,
			Map<Relationship, List<Contentlet>> contentRelationships,
			List<Category> cats, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean checkinWithNoIndex(Contentlet contentlet, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean checkinWithNoIndex(Contentlet contentlet,
			Map<Relationship, List<Contentlet>> contentRelationships,
			User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean findContentletsByFolder(Folder parentFolder, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean findContentletsByHost(Host parentHost, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean getSiblings(String identifier) throws DotDataException {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean refresh(Structure structure) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean refresh(Contentlet content) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean refreshAllContent() {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean DBSearch(Query query, User user, boolean respectFrontendRoles)
			throws ValidationException, DotDataException {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean copyContentlet(Contentlet currentContentlet, Host host,
			User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean copyContentlet(Contentlet currentContentlet, Folder folder,
			User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean searchIndex(String luceneQuery, int limit, int offset,
			String sortBy, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean cleanHostField(Structure structure, User user,
			boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean isInodeIndexed(String inode) {
		// TODO Auto-generated method stub
		return true;
	}
	public boolean isInodeIndexed(String inode, int secondsToWait) {
		// TODO Auto-generated method stub
		return true;
	}
	
	/**
	 * Method will update hostInode of content to systemhost
	 * @param identifier
	 */	
	public boolean UpdateContentWithSystemHost(String hostIdentifier)throws DotDataException{
		return true;
	}
	/**
	 * Method will remove User References of the given userId in Contentlet  
	 * @param userId
	 */	
	public boolean removeUserReferences(String userId)throws DotDataException{
		return true;
	}

	/**
	 * Return the URL Map for the specified content if the structure associated to the content has the URL Map Pattern set.
	 * 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean getUrlMapForContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException{
		return true;
	}
	
	public boolean deleteVersion(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws DotDataException,DotSecurityException{
		return true;
	}
	
	public boolean  saveDraft(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException{
	       return true;
	}
	
	public boolean searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, ParseException {
	      return true;
    } 
	
	public boolean searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException, DotSecurityException, ParseException {
          return true;
    }

  
	
}
