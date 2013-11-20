package com.dotmarketing.portlets.contentlet.business;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryParser.ParseException;
import org.elasticsearch.common.inject.ImplementedBy;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.DotStateException;
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
 * @author Armando Siem
 * @since 1.9.1.3
 * A dummy implementation of the interface ContentletAPIPreHook. Developers should use this abstract class to override the needed methods instead of implement the interface
 * ContentletAPIPostHook and all its methods.
 */
public abstract class ContentletAPIPreHookAbstractImp implements ContentletAPIPreHook {

	public boolean addFileToContentlet(Contentlet contentlet, String fileInode, String relationName, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean addImageToContentlet(Contentlet contentlet, String imageInode, String relationName, User user, boolean respectFrontendRoles) {
		return true;
	}

	//public boolean find(long inode, User user, boolean respectFrontendRoles);

	public boolean addLinkToContentlet(Contentlet contentlet, String linkInode, String relationName, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean applyStructurePermissionsToChildren(Structure structure, User user, List<Permission> permissions, boolean respectFrontendRoles) {
		return true;
	}

	public boolean archive(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean archive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean canLock(Contentlet contentlet, User user) throws   DotLockException{
		return true;
	}

	public boolean checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles) {
		return true;
	}

	public boolean checkin(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) {
		return true;
	}

	public boolean checkin(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles) {
		return true;
	}

	public boolean checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) {
		return true;
	}

	public boolean checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles) {
		return true;
	}

	public boolean checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles) {
		return true;
	}

	public boolean checkin(Contentlet contentlet, User user,boolean respectFrontendRoles) {
		return true;
	}

	public boolean checkin(Contentlet contentlet ,User user,boolean respectFrontendRoles,List<Category> cats) {
		return true;
	}

	public boolean checkinWithNoIndex(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles) {
		return true;
	}

	public boolean checkinWithNoIndex(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) {
		return true;
	}

	public boolean checkinWithNoIndex(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles) {
		return true;
	}

	public boolean checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) {
		return true;
	}

	public boolean checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles) {
		return true;
	}

	public boolean checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles) {
		return true;
	}

	public boolean checkinWithNoIndex(Contentlet contentlet, User user,boolean respectFrontendRoles) {
		return true;
	}

	public boolean checkinWithNoIndex(Contentlet contentlet ,User user,boolean respectFrontendRoles,List<Category> cats) {
		return true;
	}

	public boolean checkinWithoutVersioning(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) {
		return true;
	}

	public boolean checkout(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean checkout(String contentletInode, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean checkout(String luceneQuery, User user, boolean respectFrontendRoles, int offset, int limit) {
		return true;
	}

	public boolean checkoutWithQuery(String luceneQuery, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean cleanField(Structure structure, Field field, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean cleanHostField(Structure structure, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean contentletCount() throws DotDataException {
		return true;
	}

	public boolean contentletIdentifierCount() throws DotDataException {
		return true;
	}

	public boolean convertContentletToFatContentlet (Contentlet cont, com.dotmarketing.portlets.contentlet.business.Contentlet fatty) {
		return true;
	}

	public boolean convertFatContentletToContentlet (com.dotmarketing.portlets.contentlet.business.Contentlet fatty) {
		return true;
	}

	public boolean copyContentlet(Contentlet currentContentlet, Folder folder, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean copyContentlet(Contentlet currentContentlet, Folder folder, User user, boolean appendCopyToFileName, boolean respectFrontendRoles) {
		return true;
	}

	public boolean copyContentlet(Contentlet currentContentlet, Host host, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean copyContentlet(Contentlet currentContentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean copyProperties(Contentlet contentlet, Map<String, Object> properties) {
		return true;
	}

	public boolean DBSearch(Query query, User user,boolean respectFrontendRoles) throws ValidationException,DotDataException {
		return true;
	}

	public boolean delete(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean delete(Contentlet contentlet, User user, boolean respectFrontendRoles, boolean allVersions) {
		return true;
	}

	public boolean delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles, boolean allVersions) {
		return true;
	}

	public boolean deleteOldContent(Date deleteFrom, int offset) {
		return true;
	}

	public boolean deleteRelatedContent(Contentlet contentlet, Relationship relationship, boolean hasParent, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean deleteRelatedContent(Contentlet contentlet, Relationship relationship, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean deleteVersion(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws DotDataException,DotSecurityException{
		return true;
	}

	public boolean find(Category category, long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean find(List<Category> categories,long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean find(String inode, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean findAllContent(int offset, int limit) {
		return true;
	}

	public boolean findAllUserVersions(Identifier identifier, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean findAllVersions(Identifier identifier, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean findByStructure(String structureInode, User user, boolean respectFrontendRoles, int limit, int offset) {
		return true;
	}

	public boolean findByStructure(Structure structure, User user, boolean respectFrontendRoles, int limit, int offset) {
		return true;
	}

	public boolean findContentletByIdentifier(String identifier, boolean live, long languageId, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean findContentletForLanguage(long languageId, Identifier contentletId) {
		return true;
	}

	public boolean findContentlets(List<String> inodes) throws DotDataException {
		return true;
	}

	public boolean findContentletsByFolder(Folder parentFolder, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean findContentletsByHost(Host parentHost, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean findContentletsByIdentifiers(String[] identifiers, boolean live, long languageId, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean findContentRelationships(Contentlet contentlet, User user) throws DotDataException, DotSecurityException{
		return true;
	}

	public boolean findFieldValues(String structureInode, Field field, User user, boolean respectFrontEndRoles) {
		return true;
	}

	public boolean findPageContentlets(String HTMLPageIdentifier, String containerIdentifier, String orderby, boolean working, long languageId, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean getAllLanguages(Contentlet contentlet, Boolean isLiveContent, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean getAllRelationships(Contentlet contentlet) {
		return true;
	}

	public boolean getAllRelationships(String contentletInode, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean getBinaryFile(String contentletInode,String velocityVariableName,User user) {
		return true;
	}

	public boolean getContentletReferences(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean getFieldValue(Contentlet contentlet, Field theField) {
		return true;
	}

	public boolean getName(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean getNextReview(Contentlet content, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean getReferencingContentlet(File file, boolean live, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean getRelatedContent(Contentlet contentlet, Relationship rel, boolean pullByParent, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean getRelatedContent(Contentlet contentlet, Relationship rel, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean getRelatedFiles(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean getRelatedIdentifier(Contentlet contentlet, String relationshipType, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean getRelatedLinks(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean getSiblings(String identifier)throws DotDataException {
		return true;
	}

	public boolean getUrlMapForContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		return true;
	}

	public boolean isContentEqual(Contentlet contentlet1,Contentlet contentlet2, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean isContentlet(String inode) {
		return true;
	}

	public boolean isFieldTypeBoolean(Field field) {
		return true;
	}

	public boolean isFieldTypeDate(Field field) {
		return true;
	}

	public boolean isFieldTypeFloat(Field field) {
		return true;
	}

	public boolean isFieldTypeLong(Field field) {
		return true;
	}

	public boolean isFieldTypeString(Field field) {
		return true;
	}

	public boolean isInodeIndexed(String inode) {
		return true;
	}
	
	public boolean isInodeIndexed(String inode,boolean live) {
        return true;
    }

	public boolean isInodeIndexed(String inode, int secondsToWait) {
		return true;
	}

	public boolean loadField(String inode, Field field) throws DotDataException {
		return true;
	}

	public boolean lock(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}


	public boolean publish(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean publish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean publishRelatedHtmlPages(Contentlet contentlet) {
		return true;
	}

	public boolean refresh(Contentlet content) {
		return true;
	}

	public boolean refresh(Structure structure) {
		return true;
	}

	public boolean refreshAllContent() {
		return true;
	}

	public boolean refreshContentUnderFolder(Folder folder) throws DotReindexStateException {
		return true;
	}

	public boolean refreshContentUnderHost(Host host) throws DotReindexStateException {
		return true;
	}

	public boolean refreshReferencingContentlets(File file, boolean live) {
		return true;
	}

	public boolean reindex() {
		return true;
	}

    public boolean reindex(Contentlet contentlet) {
		return true;
	}

	public boolean reindex(Structure structure) {
		return true;
	}

	public boolean reIndexForServerNode() {
		return true;
	}

	public boolean relateContent(Contentlet contentlet, ContentletRelationshipRecords related, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean relateContent(Contentlet contentlet, Relationship rel,List<Contentlet> related, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean removeContentletFromIndex(String contentletInodeOrIdentifier) throws DotDataException {
		return true;
	}

	public boolean removeFolderReferences(Folder folder) throws DotDataException{
	    return true;
	}

	public boolean removeUserReferences(String userId)throws DotDataException {
		return true;
	}

	public boolean restoreVersion(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean saveDraft(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException{
		return true;
	}

	public boolean search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) {
		return true;
	}

	public boolean searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, ParseException {
	      return true;
    }

	public boolean searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException, DotSecurityException, ParseException {
        return true;
    }
	
	public boolean searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission, boolean anyLanguage) throws DotDataException, DotSecurityException, ParseException {
        return true;
    }

	public boolean searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean searchIndexCount(String luceneQuery, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean setContentletProperty(Contentlet contentlet, Field field, Object value) {
		return true;
	}

	public boolean unarchive(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean unarchive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean unlock(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean unpublish(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		return true;
	}
	public boolean unpublish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
		return true;
	}

	public boolean UpdateContentWithSystemHost(String hostIdentifier)throws DotDataException {
		return true;
	}

	public boolean validateContentlet(Contentlet contentlet, ContentletRelationships contentRelationships, List<Category> cats) {
		return true;
	}

	public boolean validateContentlet(Contentlet contentlet,List<Category> cats) {
		return true;
	}

	public boolean validateContentlet(Contentlet contentlet,Map<Relationship, List<Contentlet>> contentRelationships,List<Category> cats) {
		return true;
	}

	public boolean indexCount(String luceneQuery, User user,
            boolean respectFrontendRoles) {
		return true;
	}

	public boolean deleteOldContent(Date deleteFrom) {
		return true;
	}
	
	public boolean getMostViewedContent(String structureVariableName,
			String startDate, String endDate, User user) {
		return true;
	}

	@Override
	public boolean finishPublish(Contentlet contentlet, boolean isNew)
	        throws DotSecurityException, DotDataException,
	        DotContentletStateException, DotStateException {
	    return true;
	}
	
	@Override
	public boolean finishPublish(Contentlet contentlet, boolean isNew,
	        boolean isNewVersion) throws DotSecurityException,
	        DotDataException, DotContentletStateException, DotStateException {
	    return true;
	}
}
