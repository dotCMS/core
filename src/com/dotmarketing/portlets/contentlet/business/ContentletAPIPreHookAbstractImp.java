package com.dotmarketing.portlets.contentlet.business;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

/**
 * A dummy implementation of the interface ContentletAPIPreHook. Developers
 * should use this abstract class to override the needed methods instead of
 * implement the interface ContentletAPIPreHook and all its methods.
 * 
 * @author Armando Siem
 * @since 1.9.1.3
 * 
 */
public abstract class ContentletAPIPreHookAbstractImp implements ContentletAPIPreHook {

	@Override
	public boolean addFileToContentlet(Contentlet contentlet, String fileInode, String relationName, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean addImageToContentlet(Contentlet contentlet, String imageInode, String relationName, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean addLinkToContentlet(Contentlet contentlet, String linkInode, String relationName, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean applyStructurePermissionsToChildren(Structure structure, User user, List<Permission> permissions, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean archive(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean archive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean canLock(Contentlet contentlet, User user) throws   DotLockException{
		return true;
	}

	@Override
	public boolean checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean checkin(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean checkin(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean checkin(Contentlet contentlet, User user,boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean checkin(Contentlet contentlet ,User user,boolean respectFrontendRoles,List<Category> cats) {
		return true;
	}

	public boolean checkinWithNoIndex(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean checkinWithNoIndex(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean checkinWithNoIndex(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean checkinWithNoIndex(Contentlet contentlet, User user,boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean checkinWithNoIndex(Contentlet contentlet ,User user,boolean respectFrontendRoles,List<Category> cats) {
		return true;
	}

	@Override
	public boolean checkinWithoutVersioning(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean checkout(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean checkout(String contentletInode, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean checkout(String luceneQuery, User user, boolean respectFrontendRoles, int offset, int limit) {
		return true;
	}

	@Override
	public boolean checkoutWithQuery(String luceneQuery, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean cleanField(Structure structure, Field field, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean cleanHostField(Structure structure, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean contentletCount() throws DotDataException {
		return true;
	}

	@Override
	public boolean contentletIdentifierCount() throws DotDataException {
		return true;
	}

	@Override
	public boolean convertContentletToFatContentlet (Contentlet cont, com.dotmarketing.portlets.contentlet.business.Contentlet fatty) {
		return true;
	}

	@Override
	public boolean convertFatContentletToContentlet (com.dotmarketing.portlets.contentlet.business.Contentlet fatty) {
		return true;
	}

	@Override
	public boolean copyContentlet(Contentlet currentContentlet, Folder folder, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean copyContentlet(Contentlet currentContentlet, Folder folder, User user, boolean appendCopyToFileName, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean copyContentlet(Contentlet currentContentlet, Host host, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean copyContentlet(Contentlet currentContentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean copyProperties(Contentlet contentlet, Map<String, Object> properties) {
		return true;
	}

	@Override
	public boolean DBSearch(Query query, User user,boolean respectFrontendRoles) throws ValidationException,DotDataException {
		return true;
	}

	@Override
	public boolean delete(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean delete(Contentlet contentlet, User user, boolean respectFrontendRoles, boolean allVersions) {
		return true;
	}

	@Override
	public boolean delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles, boolean allVersions) {
		return true;
	}

	@Override
	public boolean deleteRelatedContent(Contentlet contentlet, Relationship relationship, boolean hasParent, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean deleteRelatedContent(Contentlet contentlet, Relationship relationship, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean deleteVersion(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws DotDataException,DotSecurityException{
		return true;
	}

	@Override
	public boolean find(Category category, long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean find(List<Category> categories,long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean find(String inode, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean findAllContent(int offset, int limit) {
		return true;
	}

	@Override
	public boolean findAllUserVersions(Identifier identifier, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean findAllVersions(Identifier identifier, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean findByStructure(String structureInode, User user, boolean respectFrontendRoles, int limit, int offset) {
		return true;
	}

	@Override
	public boolean findByStructure(Structure structure, User user, boolean respectFrontendRoles, int limit, int offset) {
		return true;
	}

	public boolean findContentletByIdentifier(String identifier, boolean live, long languageId, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean findContentletForLanguage(long languageId, Identifier contentletId) {
		return true;
	}

	@Override
	public boolean findContentlets(List<String> inodes) throws DotDataException {
		return true;
	}

	@Override
	public boolean findContentletsByFolder(Folder parentFolder, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean findContentletsByHost(Host parentHost, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean findContentletsByIdentifiers(String[] identifiers, boolean live, long languageId, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean findContentRelationships(Contentlet contentlet, User user) throws DotDataException, DotSecurityException{
		return true;
	}

	@Override
	public boolean findFieldValues(String structureInode, Field field, User user, boolean respectFrontEndRoles) {
		return true;
	}

	@Override
	public boolean findPageContentlets(String HTMLPageIdentifier, String containerIdentifier, String orderby, boolean working, long languageId, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean getAllLanguages(Contentlet contentlet, Boolean isLiveContent, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean getAllRelationships(Contentlet contentlet) {
		return true;
	}

	@Override
	public boolean getAllRelationships(String contentletInode, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean getBinaryFile(String contentletInode,String velocityVariableName,User user) {
		return true;
	}

	@Override
	public boolean getContentletReferences(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean getFieldValue(Contentlet contentlet, Field theField) {
		return true;
	}

	@Override
	public boolean getName(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean getNextReview(Contentlet content, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean getReferencingContentlet(File file, boolean live, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean getRelatedContent(Contentlet contentlet, Relationship rel, boolean pullByParent, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean getRelatedContent(Contentlet contentlet, Relationship rel, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean getRelatedFiles(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean getRelatedIdentifier(Contentlet contentlet, String relationshipType, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean getRelatedLinks(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean getSiblings(String identifier)throws DotDataException {
		return true;
	}

	@Override
	public boolean getUrlMapForContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		return true;
	}

	@Override
	public boolean isContentEqual(Contentlet contentlet1,Contentlet contentlet2, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean isContentlet(String inode) {
		return true;
	}

	@Override
	public boolean isFieldTypeBoolean(Field field) {
		return true;
	}

	@Override
	public boolean isFieldTypeDate(Field field) {
		return true;
	}

	@Override
	public boolean isFieldTypeFloat(Field field) {
		return true;
	}

	@Override
	public boolean isFieldTypeLong(Field field) {
		return true;
	}

	@Override
	public boolean isFieldTypeString(Field field) {
		return true;
	}

	@Override
	public boolean isInodeIndexed(String inode) {
		return true;
	}

	@Override
	public boolean isInodeIndexed(String inode,boolean live) {
        return true;
    }

	@Override
	public boolean isInodeIndexed(String inode, int secondsToWait) {
		return true;
	}

	@Override
	public boolean loadField(String inode, Field field) throws DotDataException {
		return true;
	}

	@Override
	public boolean lock(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean publish(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean publish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean publishRelatedHtmlPages(Contentlet contentlet) {
		return true;
	}

	@Override
	public boolean refresh(Contentlet content) {
		return true;
	}

	@Override
	public boolean refresh(Structure structure) {
		return true;
	}

	@Override
	public boolean refreshAllContent() {
		return true;
	}

	@Override
	public boolean refreshContentUnderFolder(Folder folder) throws DotReindexStateException {
		return true;
	}

	@Override
	public boolean refreshContentUnderFolderPath ( String hostId, String folderPath ) throws DotReindexStateException {
		return true;
	}

	@Override
	public boolean refreshContentUnderHost(Host host) throws DotReindexStateException {
		return true;
	}

	@Override
	public boolean refreshReferencingContentlets(File file, boolean live) {
		return true;
	}

	@Override
	public boolean reindex() {
		return true;
	}

	@Override
    public boolean reindex(Contentlet contentlet) {
		return true;
	}

	@Override
	public boolean reindex(Structure structure) {
		return true;
	}

	@Override
	public boolean reIndexForServerNode() {
		return true;
	}

	@Override
	public boolean relateContent(Contentlet contentlet, ContentletRelationshipRecords related, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean relateContent(Contentlet contentlet, Relationship rel,List<Contentlet> related, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean removeContentletFromIndex(String contentletInodeOrIdentifier) throws DotDataException {
		return true;
	}

	@Override
	public boolean removeFolderReferences(Folder folder) throws DotDataException{
	    return true;
	}

	@Override
	public boolean removeUserReferences(String userId)throws DotDataException {
		return true;
	}

	@Override
	public boolean restoreVersion(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean saveDraft(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException{
		return true;
	}

	@Override
	public boolean search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) {
		return true;
	}

	@Override
	public boolean searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
	      return true;
    }

	@Override
	public boolean searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException, DotSecurityException {
        return true;
    }

	@Override
	public boolean searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission, boolean anyLanguage) throws DotDataException, DotSecurityException {
        return true;
    }

	@Override
	public boolean searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean searchIndexCount(String luceneQuery, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean setContentletProperty(Contentlet contentlet, Field field, Object value) {
		return true;
	}

	@Override
	public boolean unarchive(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean unarchive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean unlock(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean unpublish(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean unpublish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean UpdateContentWithSystemHost(String hostIdentifier)throws DotDataException {
		return true;
	}

	@Override
	public boolean validateContentlet(Contentlet contentlet, ContentletRelationships contentRelationships, List<Category> cats) {
		return true;
	}

	@Override
	public boolean validateContentlet(Contentlet contentlet,List<Category> cats) {
		return true;
	}

	@Override
	public boolean validateContentlet(Contentlet contentlet,Map<Relationship, List<Contentlet>> contentRelationships,List<Category> cats) {
		return true;
	}

	@Override
	public boolean indexCount(String luceneQuery, User user,
            boolean respectFrontendRoles) {
		return true;
	}

	@Override
    public boolean esSearchRaw ( String esQuery, boolean live, User user, boolean respectFrontendRoles ) throws DotSecurityException, DotDataException {
        return true;
    }

	@Override
    public boolean esSearch ( String esQuery, boolean live, User user, boolean respectFrontendRoles ) throws DotSecurityException, DotDataException {
        return true;
    }

	@Override
	public boolean addPermissionsToQuery ( StringBuffer buffy, User user, List<Role> roles, boolean respectFrontendRoles ) throws DotSecurityException, DotDataException {
		return true;
	}

	@Override
	public boolean deleteOldContent(Date deleteFrom) {
		return true;
	}

	@Override
	public boolean getMostViewedContent(String structureVariableName,
			String startDate, String endDate, User user) {
		return true;
	}

	@Override
	public boolean publishAssociated(Contentlet contentlet, boolean isNew)
	        throws DotSecurityException, DotDataException,
	        DotContentletStateException, DotStateException {
	    return true;
	}
	
	@Override
	public boolean publishAssociated(Contentlet contentlet, boolean isNew,
	        boolean isNewVersion) throws DotSecurityException,
	        DotDataException, DotContentletStateException, DotStateException {
	    return true;
	}

	@Override
	public boolean deleteByHost(Host host, User user, boolean respectFrontendRoles) {
		return true;
	}

	@Override
	public boolean findContentletsByHost(Host parentHost, List<Integer> includingContentTypes,
			List<Integer> excludingContentTypes, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		return true;
	}

}
