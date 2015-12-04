package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * A dummy implementation of the interface ContentletAPIPostHook. Developers
 * should use this abstract class to override the needed methods instead of
 * implement the interface ContentletAPIPostHook and all its methods.
 * 
 * @author Armando Siem
 * @since 1.9.1.3
 * 
 */
public abstract class ContentletAPIPostHookAbstractImp implements ContentletAPIPostHook {

	@Override
	public void findAllContent(int offset, int limit, List<Contentlet> returnValue) {
	}

	@Override
	public void find(String inode, User user, boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void findContentletForLanguage(long languageId, Identifier contentletId,Contentlet returnValue) {
	}

	@Override
	public void findByStructure(Structure structure, User user, boolean respectFrontendRoles, int limit, int offset,List<Contentlet> returnValue) {
	}

	@Override
	public void findByStructure(String structureInode, User user, boolean respectFrontendRoles, int limit, int offset,List<Contentlet> returnValue) {
	}

	@Override
	public void findContentletByIdentifier(String identifier, boolean live, long languageId, User user, boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void findContentletsByIdentifiers(String[] identifiers, boolean live, long languageId, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	@Override
	public void findContentlets(List<String> inodes,List<Contentlet> returnValue) {
	}

	@Override
	public void findContentletsByFolder(Folder parentFolder, User user, boolean respectFrontendRoles) throws DotDataException {
	}

	@Override
	public void findContentletsByHost(Host parentHost, User user, boolean respectFrontendRoles) throws DotDataException {
	}

	@Override
	public void copyContentlet(Contentlet currentContentlet, User user, boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void copyContentlet(Contentlet currentContentlet, Host host, User user, boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void copyContentlet(Contentlet currentContentlet, Folder folder, User user, boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	@Override
	public void search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission,List<Contentlet> returnValue) {
	}

	@Override
	public void searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles,List<ContentletSearch> returnValue) {
	}

	@Override
	public void publishRelatedHtmlPages(Contentlet contentlet) {
	}

	@Override
	public void cleanField(Structure structure, Field field, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void cleanHostField(Structure structure, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void getNextReview(Contentlet content, User user, boolean respectFrontendRoles,Date returnValue) {
	}

	@Override
	public void getContentletReferences(Contentlet contentlet, User user, boolean respectFrontendRoles,List<Map<String, Object>> returnValue) {
	}

	@Override
	public void getFieldValue(Contentlet contentlet, Field theField,Object returnValue) {
	}

	@Override
	public void addLinkToContentlet(Contentlet contentlet, String linkInode, String relationName, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void addFileToContentlet(Contentlet contentlet, String fileInode, String relationName, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void addImageToContentlet(Contentlet contentlet, String imageInode, String relationName, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void findPageContentlets(String HTMLPageIdentifier, String containerIdentifier, String orderby, boolean working, long languageId, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	@Override
	public void getAllRelationships(String contentletInode, User user, boolean respectFrontendRoles,ContentletRelationships returnValue) {
	}

	@Override
	public void getAllRelationships(Contentlet contentlet,ContentletRelationships returnValue) {
	}

	@Override
	public void getAllLanguages(Contentlet contentlet, Boolean isLiveContent, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	@Override
	public void isContentEqual(Contentlet contentlet1,Contentlet contentlet2, User user, boolean respectFrontendRoles,boolean returnValue) {
	}

	@Override
	public void archive(Contentlet contentlet, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void delete(Contentlet contentlet, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void delete(Contentlet contentlet, User user, boolean respectFrontendRoles, boolean allVersions) {
	}

	@Override
	public void publish(Contentlet contentlet, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void publish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void unpublish(Contentlet contentlet, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void unpublish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void archive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void unarchive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void unarchive(Contentlet contentlet, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles, boolean allVersions) {
	}

	@Override
	public void deleteRelatedContent(Contentlet contentlet, Relationship relationship, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void deleteRelatedContent(Contentlet contentlet, Relationship relationship, boolean hasParent, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void relateContent(Contentlet contentlet, Relationship rel,List<Contentlet> related, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void relateContent(Contentlet contentlet, ContentletRelationshipRecords related, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void getRelatedContent(Contentlet contentlet, Relationship rel, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	@Override
	public void getRelatedContent(Contentlet contentlet, Relationship rel, boolean pullByParent, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	@Override
	public void getReferencingContentlet(File file, boolean live, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	@Override
	public void refreshReferencingContentlets(File file, boolean live) {
	}

	@Override
	public void unlock(Contentlet contentlet, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void lock(Contentlet contentlet, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void reindex() {
	}

	@Override
	public void reindex(Structure structure) {
	}

	@Override
	public void reindex(Contentlet contentlet) {
	}

	@Override
	public void reIndexForServerNode() {
	} 

	@Override
	public void getRelatedFiles(Contentlet contentlet, User user, boolean respectFrontendRoles,List<File> returnValue) {
	}

	@Override
	public void getRelatedIdentifier(Contentlet contentlet, String relationshipType, User user, boolean respectFrontendRoles,Identifier returnValue) {
	}

	@Override
	public void getRelatedLinks(Contentlet contentlet, User user, boolean respectFrontendRoles,List<Link> returnValue) {
	}

	@Override
	public void checkout(String contentletInode, User user, boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void checkout(List<Contentlet> contentlets, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	@Override
	public void checkout(String luceneQuery, User user, boolean respectFrontendRoles, List<Contentlet> returnValue) {
	}

	@Override
	public void checkout(String luceneQuery, User user, boolean respectFrontendRoles, int offset, int limit,List<Contentlet> returnValue) {
	}

	@Override
	public void checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void checkin(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void checkin(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void checkin(Contentlet contentlet ,User user,boolean respectFrontendRoles,List<Category> cats,Contentlet returnValue) {
	}

	@Override
	public void checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void checkin(Contentlet contentlet, User user,boolean respectFrontendRoles, Contentlet returnValue) {
	}

	@Override
	public void checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void checkinWithoutVersioning(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void restoreVersion(Contentlet contentlet, User user, boolean respectFrontendRoles) {
	}

	@Override
	public void findAllVersions(Identifier identifier, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	@Override
	public void findAllUserVersions(Identifier identifier, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	@Override
	public void getName(Contentlet contentlet, User user, boolean respectFrontendRoles,String returnValue) {
	}

	@Override
	public void copyProperties(Contentlet contentlet, Map<String, Object> properties) {
	}

	@Override
	public void isContentlet(String inode,boolean returnValue) {
	}

	@Override
	public void find(Category category, long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	@Override
	public void find(List<Category> categories,long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	@Override
	public void setContentletProperty(Contentlet contentlet, Field field, Object value) {
	}

	@Override
	public void validateContentlet(Contentlet contentlet,List<Category> cats) {
	} 

	@Override
	public void validateContentlet(Contentlet contentlet,Map<Relationship, List<Contentlet>> contentRelationships,List<Category> cats) {
	} 

	@Override
	public void validateContentlet(Contentlet contentlet, ContentletRelationships contentRelationships, List<Category> cats) {
	} 

	@Override
	public void isFieldTypeString(Field field,boolean returnValue) {
	}

	@Override
	public void isFieldTypeDate(Field field,boolean returnValue) {
	}

	@Override
	public void isFieldTypeLong(Field field,boolean returnValue) {
	}

	@Override
	public void isFieldTypeBoolean(Field field,boolean returnValue) {
	}

	@Override
	public void isFieldTypeFloat(Field field,boolean returnValue) {
	}

	@Override
	public void convertFatContentletToContentlet (com.dotmarketing.portlets.contentlet.business.Contentlet fatty,Contentlet returnValue) {
	}

	@Override
	public void convertContentletToFatContentlet (Contentlet cont, com.dotmarketing.portlets.contentlet.business.Contentlet fatty,com.dotmarketing.portlets.contentlet.business.Contentlet returnValue) {
	}

	@Override
	public void applyStructurePermissionsToChildren(Structure structure, User user, List<Permission> permissions, boolean respectFrontendRoles) {
	}

	@Override
	public void findFieldValues(String structureInode, Field field, User user, boolean respectFrontEndRoles,List<String> returnValue) {
	}

	@Override
	public void getBinaryFile(String contentletInode,String velocityVariableName,User user,java.io.File returnValue) {
	}

	@Override
	public long contentletCount(long returnValue) throws DotDataException {
		return 0;
	}

	@Override
	public long contentletIdentifierCount(long returnValue) throws DotDataException {
		return 0;
	}

	@Override
	public boolean removeContentletFromIndex(String contentletInodeOrIdentifier) throws DotDataException {
		return true;
	}

	@Override
	public void refresh(Structure structure) {
	}

	@Override
	public void refresh(Contentlet contentlet) {
	}

	@Override
	public void refreshAllContent() {
	}

	@Override
	public List<Contentlet> getSiblings(String identifier)throws DotDataException {
		return new ArrayList<Contentlet>();
	}

	@Override
	public void checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void checkinWithNoIndex(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void checkinWithNoIndex(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void checkinWithNoIndex(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void checkinWithNoIndex(Contentlet contentlet ,User user,boolean respectFrontendRoles,List<Category> cats,Contentlet returnValue) {
	}

	@Override
	public void checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void checkinWithNoIndex(Contentlet contentlet, User user,boolean respectFrontendRoles, Contentlet returnValue) {
	}

	@Override
	public void checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}

	@Override
	public void DBSearch(Query query, User user,boolean respectFrontendRoles, List<Map<String, Serializable>> returnValue) throws ValidationException,DotDataException {
	}

	@Override
	public void isInodeIndexed(String inode, boolean returnValue) {
	}

	@Override
	public void isInodeIndexed(String inode, int secondsToWait, boolean returnValue) {
	}

	@Override
	public void UpdateContentWithSystemHost(String hostIdentifier)throws DotDataException {
	}

	@Override
	public void removeUserReferences(String userId)throws DotDataException {
	}

	@Override
	public void getUrlMapForContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
	}

	@Override
	public void deleteVersion(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws DotDataException,DotSecurityException{
	}

	@Override
	public void saveDraft(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException{	  	
	}

	@Override
	public void searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
    } 

	@Override
	public void searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException, DotSecurityException {
	}

	@Override
	public void searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission, boolean anyLanguage) throws DotDataException, DotSecurityException {
	}

	@Override
	public void removeFolderReferences(Folder folder) throws DotDataException{
	}

	@Override
	public void refreshContentUnderHost(Host host) throws DotReindexStateException {

	}

	@Override
	public void refreshContentUnderFolder(Folder folder) throws DotReindexStateException {

	}

	@Override
	public boolean canLock(Contentlet contentlet, User user) throws   DotLockException{
		return true;
	}

	@Override
	public void findContentRelationships(Contentlet contentlet, User user) throws DotDataException, DotSecurityException{
		
	}

	@Override
    public void copyContentlet ( Contentlet currentContentlet, Folder folder, User user, boolean appendCopyToFileName, boolean respectFrontendRoles, Contentlet returnValue ) {
    }

	@Override
    public void deleteOldContent ( Date deleteFrom, int returnValue ) {
    }

	@Override
    public void searchIndexCount ( String luceneQuery, User user, boolean respectFrontendRoles, long c ) {
    }

	@Override
    public void loadField ( String inode, Field field, Object value ) {
    }

	@Override
    public void indexCount ( String luceneQuery, User user, boolean respectFrontendRoles, long value ) {
    }

	@Override
    public boolean getMostViewedContent ( String structureVariableName, String startDate, String endDate, User user ) {
        return true;
    }

	@Override
    public void isInodeIndexed ( String inode, boolean live, boolean returnValue ) {
    }

	@Override
    public void esSearchRaw ( String esQuery, boolean live, User user, boolean respectFrontendRoles ) throws DotSecurityException, DotDataException {
    }

	@Override
    public void esSearch ( String esQuery, boolean live, User user, boolean respectFrontendRoles ) throws DotSecurityException, DotDataException {
    }

	@Override
	public void addPermissionsToQuery ( StringBuffer buffy, User user, List<Role> roles, boolean respectFrontendRoles ) throws DotSecurityException, DotDataException{
	}

    @Override
    public void publishAssociated(Contentlet contentlet, boolean isNew)
            throws DotSecurityException, DotDataException,
            DotContentletStateException, DotStateException {
        
    }
    
    @Override
    public void publishAssociated(Contentlet contentlet, boolean isNew,
            boolean isNewVersion) throws DotSecurityException,
            DotDataException, DotContentletStateException, DotStateException {        
    }

    @Override
    public void refreshContentUnderFolderPath ( String hostId, String folderPath ) throws DotReindexStateException {
    	
    }

	@Override
	public void deleteByHost(Host host, User user, boolean respectFrontendRoles) {

	}

	@Override
	public void findContentletsByHost(Host parentHost, List<Integer> includingContentTypes,
			List<Integer> excludingContentTypes, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {

	}

}