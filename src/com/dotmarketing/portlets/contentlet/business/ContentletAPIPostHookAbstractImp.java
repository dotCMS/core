package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.DotStateException;
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

import org.apache.lucene.queryParser.ParseException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Armando Siem
 * @since 1.9.1.3
 * A dummy implementation of the interface ContentletAPIPostHook. Developers should use this abstract class to override the needed methods instead of implement the interface
 * ContentletAPIPostHook and all its methods.
 */
public abstract class ContentletAPIPostHookAbstractImp implements ContentletAPIPostHook {

	public void findAllContent(int offset, int limit, List<Contentlet> returnValue) {
	}
	
	public void find(String inode, User user, boolean respectFrontendRoles,Contentlet returnValue) {
	}
	
	//public void find(long inode, User user, boolean respectFrontendRoles,Contentlet returnValue);
	
	public void findContentletForLanguage(long languageId, Identifier contentletId,Contentlet returnValue) {
	}
	
	public void findByStructure(Structure structure, User user, boolean respectFrontendRoles, int limit, int offset,List<Contentlet> returnValue) {
	}
	
	public void findByStructure(String structureInode, User user, boolean respectFrontendRoles, int limit, int offset,List<Contentlet> returnValue) {
	}
	
	public void findContentletByIdentifier(String identifier, boolean live, long languageId, User user, boolean respectFrontendRoles,Contentlet returnValue) {
	}

	public void findContentletsByIdentifiers(String[] identifiers, boolean live, long languageId, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}
		
	public void findContentlets(List<String> inodes,List<Contentlet> returnValue) {
	}
	
	public void findContentletsByFolder(Folder parentFolder, User user, boolean respectFrontendRoles) throws DotDataException {
	}

	public void findContentletsByHost(Host parentHost, User user, boolean respectFrontendRoles) throws DotDataException {
	}

	public void copyContentlet(Contentlet currentContentlet, User user, boolean respectFrontendRoles,Contentlet returnValue) {
	}

	public void copyContentlet(Contentlet currentContentlet, Host host, User user, boolean respectFrontendRoles,Contentlet returnValue) {
	}

	public void copyContentlet(Contentlet currentContentlet, Folder folder, User user, boolean respectFrontendRoles,Contentlet returnValue) {
	}

	public void search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}
	
	public void search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission,List<Contentlet> returnValue) {
	}

	public void searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles,List<ContentletSearch> returnValue) {
	}
	
	public void publishRelatedHtmlPages(Contentlet contentlet) {
	}
	
	public void cleanField(Structure structure, Field field, User user, boolean respectFrontendRoles) {
	}

	public void cleanHostField(Structure structure, User user, boolean respectFrontendRoles) {
	}
	
	public void getNextReview(Contentlet content, User user, boolean respectFrontendRoles,Date returnValue) {
	}

	public void getContentletReferences(Contentlet contentlet, User user, boolean respectFrontendRoles,List<Map<String, Object>> returnValue) {
	}
	
	public void getFieldValue(Contentlet contentlet, Field theField,Object returnValue) {
	}

	public void addLinkToContentlet(Contentlet contentlet, String linkInode, String relationName, User user, boolean respectFrontendRoles) {
	}
	
	public void addFileToContentlet(Contentlet contentlet, String fileInode, String relationName, User user, boolean respectFrontendRoles) {
	}
	
	public void addImageToContentlet(Contentlet contentlet, String imageInode, String relationName, User user, boolean respectFrontendRoles) {
	}

	public void findPageContentlets(String HTMLPageIdentifier, String containerIdentifier, String orderby, boolean working, long languageId, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	public void getAllRelationships(String contentletInode, User user, boolean respectFrontendRoles,ContentletRelationships returnValue) {
	}

	public void getAllRelationships(Contentlet contentlet,ContentletRelationships returnValue) {
	}

	public void getAllLanguages(Contentlet contentlet, Boolean isLiveContent, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	public void isContentEqual(Contentlet contentlet1,Contentlet contentlet2, User user, boolean respectFrontendRoles,boolean returnValue) {
	}

	public void archive(Contentlet contentlet, User user, boolean respectFrontendRoles) {
	}

	public void delete(Contentlet contentlet, User user, boolean respectFrontendRoles) {
	}
	
	public void delete(Contentlet contentlet, User user, boolean respectFrontendRoles, boolean allVersions) {
	}
	
	public void publish(Contentlet contentlet, User user, boolean respectFrontendRoles) {
	}
	
	public void publish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
	}

	public void unpublish(Contentlet contentlet, User user, boolean respectFrontendRoles) {
	}
	
	public void unpublish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
	}

	public void archive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
	}
	
	public void unarchive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
	}

	public void unarchive(Contentlet contentlet, User user, boolean respectFrontendRoles) {
	}
	
	public void delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
	}
	
	public void delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles, boolean allVersions) {
	}
	
	public void deleteRelatedContent(Contentlet contentlet, Relationship relationship, User user, boolean respectFrontendRoles) {
	}
	
	public void deleteRelatedContent(Contentlet contentlet, Relationship relationship, boolean hasParent, User user, boolean respectFrontendRoles) {
	}
	
	public void relateContent(Contentlet contentlet, Relationship rel,List<Contentlet> related, User user, boolean respectFrontendRoles) {
	}

	public void relateContent(Contentlet contentlet, ContentletRelationshipRecords related, User user, boolean respectFrontendRoles) {
	}

	public void getRelatedContent(Contentlet contentlet, Relationship rel, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	public void getRelatedContent(Contentlet contentlet, Relationship rel, boolean pullByParent, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	public void getReferencingContentlet(File file, boolean live, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}
	
	public void refreshReferencingContentlets(File file, boolean live) {
	}

	public void unlock(Contentlet contentlet, User user, boolean respectFrontendRoles) {
	}

	public void lock(Contentlet contentlet, User user, boolean respectFrontendRoles) {
	}
	
	public void reindex() {
	}
	
	public void reindex(Structure structure) {
	}
	
	public void reindex(Contentlet contentlet) {
	}
	
	public void reIndexForServerNode() {
	} 
	
	public void getRelatedFiles(Contentlet contentlet, User user, boolean respectFrontendRoles,List<File> returnValue) {
	}
	
	public void getRelatedIdentifier(Contentlet contentlet, String relationshipType, User user, boolean respectFrontendRoles,Identifier returnValue) {
	}
	
	public void getRelatedLinks(Contentlet contentlet, User user, boolean respectFrontendRoles,List<Link> returnValue) {
	}
	
	public void checkout(String contentletInode, User user, boolean respectFrontendRoles,Contentlet returnValue) {
	}
	
	public void checkout(List<Contentlet> contentlets, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}
	
	public void checkout(String luceneQuery, User user, boolean respectFrontendRoles, List<Contentlet> returnValue) {
	}
	
	public void checkout(String luceneQuery, User user, boolean respectFrontendRoles, int offset, int limit,List<Contentlet> returnValue) {
	}
	
	public void checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}
	
	public void checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles,Contentlet returnValue) {
	}
	
	public void checkin(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}
	
	public void checkin(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}
	
	public void checkin(Contentlet contentlet ,User user,boolean respectFrontendRoles,List<Category> cats,Contentlet returnValue) {
	}
	
	public void checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}
	
	public void checkin(Contentlet contentlet, User user,boolean respectFrontendRoles, Contentlet returnValue) {
	}
	
	public void checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}
	
	public void checkinWithoutVersioning(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}
	
	public void restoreVersion(Contentlet contentlet, User user, boolean respectFrontendRoles) {
	}
	
	public void findAllVersions(Identifier identifier, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}
	
	public void findAllUserVersions(Identifier identifier, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}
	
	public void getName(Contentlet contentlet, User user, boolean respectFrontendRoles,String returnValue) {
	}
	
	public void copyProperties(Contentlet contentlet, Map<String, Object> properties) {
	}
	
	public void isContentlet(String inode,boolean returnValue) {
	}
	
	public void find(Category category, long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	public void find(List<Category> categories,long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles,List<Contentlet> returnValue) {
	}

	public void setContentletProperty(Contentlet contentlet, Field field, Object value) {
	}
	
	public void validateContentlet(Contentlet contentlet,List<Category> cats) {
	} 
	
	public void validateContentlet(Contentlet contentlet,Map<Relationship, List<Contentlet>> contentRelationships,List<Category> cats) {
	} 
	
	public void validateContentlet(Contentlet contentlet, ContentletRelationships contentRelationships, List<Category> cats) {
	} 
	
	public void isFieldTypeString(Field field,boolean returnValue) {
	}
	
	public void isFieldTypeDate(Field field,boolean returnValue) {
	}
	
	public void isFieldTypeLong(Field field,boolean returnValue) {
	}
	
	public void isFieldTypeBoolean(Field field,boolean returnValue) {
	}
	
	public void isFieldTypeFloat(Field field,boolean returnValue) {
	}

	public void convertFatContentletToContentlet (com.dotmarketing.portlets.contentlet.business.Contentlet fatty,Contentlet returnValue) {
	}
	
	public void convertContentletToFatContentlet (Contentlet cont, com.dotmarketing.portlets.contentlet.business.Contentlet fatty,com.dotmarketing.portlets.contentlet.business.Contentlet returnValue) {
	}
    
	public void applyStructurePermissionsToChildren(Structure structure, User user, List<Permission> permissions, boolean respectFrontendRoles) {
	}
	
	public void deleteOldContent(Date deleteFrom, int offset,int returnValue) {
	}
	
	public void findFieldValues(String structureInode, Field field, User user, boolean respectFrontEndRoles,List<String> returnValue) {
	}
	
	public void getBinaryFile(String contentletInode,String velocityVariableName,User user,java.io.File returnValue) {
	}
	
	public long contentletCount(long returnValue) throws DotDataException {
		return 0;
	}

	public long contentletIdentifierCount(long returnValue) throws DotDataException {
		return 0;
	}
	
	public boolean removeContentletFromIndex(String contentletInodeOrIdentifier) throws DotDataException {
		return true;
	}

	public void refresh(Structure structure) {
	}

	public void refresh(Contentlet contentlet) {
	}

	public void refreshAllContent() {
	}

	public List<Contentlet> getSiblings(String identifier)throws DotDataException {
		return new ArrayList<Contentlet>();
	}
	
	public void checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}
	
	public void checkinWithNoIndex(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles,Contentlet returnValue) {
	}
	
	public void checkinWithNoIndex(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}
	
	public void checkinWithNoIndex(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}
	
	public void checkinWithNoIndex(Contentlet contentlet ,User user,boolean respectFrontendRoles,List<Category> cats,Contentlet returnValue) {
	}
	
	public void checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}
	
	public void checkinWithNoIndex(Contentlet contentlet, User user,boolean respectFrontendRoles, Contentlet returnValue) {
	}
	
	public void checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles,Contentlet returnValue) {
	}
	
	public void DBSearch(Query query, User user,boolean respectFrontendRoles, List<Map<String, Serializable>> returnValue) throws ValidationException,DotDataException {
	}
	
	public void isInodeIndexed(String inode, boolean returnValue) {
	}

	public void isInodeIndexed(String inode, int secondsToWait, boolean returnValue) {
	}
	
	public void UpdateContentWithSystemHost(String hostIdentifier)throws DotDataException {
	}
	
	public void removeUserReferences(String userId)throws DotDataException {
	}
	
	public void getUrlMapForContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
	}
	public void deleteVersion(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws DotDataException,DotSecurityException{
	}
	

	public void saveDraft(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException{	  	
	}
	
	public void searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, ParseException {     
    } 
	
	public void searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException, DotSecurityException, ParseException {
	}

	public void searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission, boolean anyLanguage) throws DotDataException, DotSecurityException, ParseException {
	}

	public void removeFolderReferences(Folder folder) throws DotDataException{
	}

	public void refreshContentUnderHost(Host host) throws DotReindexStateException {


	}

	public void refreshContentUnderFolder(Folder folder) throws DotReindexStateException {


	}
	public boolean canLock(Contentlet contentlet, User user) throws   DotLockException{
		return true;
	}
	
	public void findContentRelationships(Contentlet contentlet, User user) throws DotDataException, DotSecurityException{
		
	}

    public void copyContentlet ( Contentlet currentContentlet, Folder folder, User user, boolean appendCopyToFileName, boolean respectFrontendRoles, Contentlet returnValue ) {
    }

    public void deleteOldContent ( Date deleteFrom, int returnValue ) {
    }

    public void searchIndexCount ( String luceneQuery, User user, boolean respectFrontendRoles, long c ) {
    }

    public void loadField ( String inode, Field field, Object value ) {
    }

    public void indexCount ( String luceneQuery, User user, boolean respectFrontendRoles, long value ) {
    }

    public boolean getMostViewedContent ( String structureVariableName, String startDate, String endDate, User user ) {
        return true;
    }

    public void isInodeIndexed ( String inode, boolean live, boolean returnValue ) {
    }
    
    @Override
    public void finishPublish(Contentlet contentlet, boolean isNew)
            throws DotSecurityException, DotDataException,
            DotContentletStateException, DotStateException {
        
    }
    @Override
    public void finishPublish(Contentlet contentlet, boolean isNew,
            boolean isNewVersion) throws DotSecurityException,
            DotDataException, DotContentletStateException, DotStateException {        
    }

}