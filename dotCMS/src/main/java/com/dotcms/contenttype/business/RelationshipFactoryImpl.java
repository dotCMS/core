package com.dotcms.contenttype.business;

import com.dotcms.contenttype.business.sql.RelationshipSQL;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeIf;
import com.dotcms.contenttype.transform.relationship.DbRelationshipTransformer;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.RelationshipCache;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RelationshipFactoryImpl implements RelationshipFactory{

	private static final RelationshipSQL sql= RelationshipSQL.getInstance();

    private static final RelationshipCache cache = CacheLocator.getRelationshipCache();

    private static class RelationshipFacotrySingleton {
        private static final RelationshipFactoryImpl INSTANCE = new RelationshipFactoryImpl();
    }
    
    private RelationshipFactoryImpl() {
        
    }
    
    
    public static RelationshipFactoryImpl  instance() {

        return RelationshipFacotrySingleton.INSTANCE;
    }
    
	@Override
	public void deleteByContentType(final ContentTypeIf type) throws DotDataException{
	    new DotConnect()
                .setSQL(sql.DELETE_RELATIONSHIP_BY_PARENT_OR_CHILD_INODE)
                .addParam(type.id())
                .addParam(type.id())
                .loadResults();
	}



    @Override
    public Relationship byInode(final String inode){
		Relationship rel = null;
		try {
			rel = cache.getRelationshipByInode(inode);
			if(rel != null)
				return rel;
		} catch (DotCacheException e) {
			Logger.debug(this.getClass(), "Unable to access the cache to obtain the relationship", e);
		}

		try {
		    final List<Map<String, Object>> results = new DotConnect()
                    .setSQL(sql.FIND_BY_INODE)
                    .addParam(inode)
		            .loadObjectResults();
            if (results.isEmpty()) {
                throw new NotFoundInDbException("Relationship with inode: " + inode + " not found");
            }
            rel = new DbRelationshipTransformer(results).from();
        } catch (NotFoundInDbException e){
            Logger.debug(this, e.getMessage());
        } catch (DotDataException e){
                Logger.error(this, "Error getting Relationship with inode: " + inode, e);
                throw new RuntimeException(e);
        }

        if(rel!= null && InodeUtils.isSet(rel.getInode())) {
            cache.putRelationshipByInode(rel);
        }

        return rel;
    }

    @Override
    @SuppressWarnings("unchecked")
	public  List<Relationship> byParent (final ContentTypeIf parent) throws DotDataException {
        final List<Map<String, Object>> results = new DotConnect()
                .setSQL(sql.FIND_BY_PARENT_INODE)
                .addParam(parent.id())
                .loadObjectResults();
        return new DbRelationshipTransformer(results).asList();
    }

    @Override
    @SuppressWarnings("unchecked")
	public  List<Relationship> byChild (final ContentTypeIf child) throws DotDataException {
        final List<Map<String, Object>> results = new DotConnect()
                .setSQL(sql.FIND_BY_CHILD_INODE)
                .addParam(child.id())
                .loadObjectResults();
        return new DbRelationshipTransformer(results).asList();
    }
    @Override
    public List<Relationship> dbAll() {
        return dbAll("inode");
    }
    @Override
    @SuppressWarnings("unchecked")
    public  List<Relationship> dbAll(String orderBy) {
        orderBy = SQLUtil.sanitizeSortBy(orderBy);

	    final DotConnect dc = new DotConnect();
	    dc.setSQL(sql.SELECT_ALL_FIELDS + " order by " + orderBy);

        List<Map<String, Object>> results = new ArrayList<>();
        try {
            results = dc.loadObjectResults();
        } catch (DotDataException e) {
            Logger.error(this, "Error getting All Relationships", e);
        }
        return new DbRelationshipTransformer(results).asList();
    }

    
    
    @Override
    public Relationship byTypeValue(final String typeValue) {

        Relationship rel;
        try {
            rel = cache.getRelationshipByName(typeValue);
            if(rel != null)
                return rel;
        } catch (DotCacheException e) {
            Logger.debug(this.getClass(), "Unable to access the cache to obtain the relationship", e);
        }

        rel=dbByTypeValue(typeValue);

        if(rel!= null && InodeUtils.isSet(rel.getInode())) {
            cache.putRelationshipByInode(rel);
        }

        return rel;
    }
    
    
    
    @Override
    public  Relationship dbByTypeValue(final String typeValue){
        if(typeValue==null) {
            return null;
        }
        Relationship relationship = null;

        
        List<Map<String, Object>> results;
        try {
            final DotConnect dc = new DotConnect();
            dc.setSQL(sql.FIND_BY_TYPE_VALUE);
            dc.addParam(typeValue.toLowerCase());
            results = dc.loadObjectResults();
            if (results.size() == 0) {
                return relationship;
            }

            relationship = new DbRelationshipTransformer(results).from();

            if(relationship!= null && InodeUtils.isSet(relationship.getInode())){
                cache.putRelationshipByInode(relationship);
            }

        } catch (DotDataException e) {
            Logger.error(this,"Error getting relationships with typeValue: " + typeValue.toLowerCase(),e);
        }

        return relationship;
    }

    @Override
    public Optional<Relationship> byParentChildRelationName(final ContentType contentType,
            final String relationName) {
        if (contentType == null){
            Optional.empty();
        }

        List<Map<String, Object>> results;
        try {
            final DotConnect dc = new DotConnect();
            dc.setSQL(sql.FIND_BY_PARENT_CHILD_AND_RELATION_NAME);
            dc.addParam(contentType.id());
            dc.addParam(contentType.id());
            dc.addParam(relationName);
            dc.addParam(relationName);
            results = dc.loadObjectResults();
            if (results.size() == 0) {
                return Optional.empty();
            }

            final Relationship relationship = new DbRelationshipTransformer(results).from();

            if(relationship!= null && InodeUtils.isSet(relationship.getInode())){
                cache.putRelationshipByInode(relationship);
            }

            return Optional.of(relationship);
        } catch (DotDataException e) {
            Logger.error(this,"Error getting relationships for content type: " + contentType.name(),e);
        }

        return Optional.empty();
    }

    @Override
    public List<Relationship> dbAllByTypeValue(final String typeValue){
        if(typeValue==null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> results;

        List<Relationship> rel = null;
        try {
            final DotConnect dc = new DotConnect();
            dc.setSQL(sql.FIND_BY_TYPE_VALUE_LIKE);
            dc.addParam(StringPool.PERCENT + typeValue.toLowerCase() +  StringPool.PERCENT);

            results = dc.loadObjectResults();
            if (results.size() == 0) {
             return Collections.emptyList();
            }

            rel = new DbRelationshipTransformer(results).asList();

            rel.forEach(item -> cache.putRelationshipByInode(item));

        } catch (DotDataException e) {
            Logger.error(this,"Error getting relationships with typeValue: " + typeValue.toLowerCase(),e);
        }

		return rel;

    }

    @Override
    public List<Relationship> byContentType(final String contentType){
        return byContentType(contentType,"inode");
    }

    @Override
    public List<Relationship> byContentType(final ContentTypeIf contentType){
        return byContentType(contentType.id(),"inode");
    }

    @Override
    public List<Relationship> byContentType(final ContentTypeIf contentType, final String orderBy){
	    return byContentType(contentType.id(),orderBy);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Relationship> byContentType(final String contentTypeInode, String orderBy){

        List<Relationship> relationships = new ArrayList<>();
        ContentTypeIf contentTypeIf = null;
        try {
            contentTypeIf = APILocator.getContentTypeAPI(APILocator.systemUser()).find(contentTypeInode);
            relationships = cache.getRelationshipsByType(contentTypeIf);
            if(relationships != null && !relationships.isEmpty()) {
                return relationships;
            }

            orderBy = SQLUtil.sanitizeSortBy(orderBy);

            final DotConnect dc = new DotConnect();
            dc.setSQL(sql.FIND_BY_PARENT_OR_CHILD_INODE + " order by " + orderBy);
            dc.addParam(contentTypeInode);
            dc.addParam(contentTypeInode);
            List<Map<String, Object>> results;
            results = dc.loadObjectResults();

            relationships = new DbRelationshipTransformer(results).asList();

            if(!relationships.isEmpty()){
                cache.putRelationshipsByType(contentTypeIf,relationships);
            }

        } catch (DotCacheException e) {
            Logger.debug(this.getClass(), "Unable to access the cache to obtain the relationship", e);
        }  catch (DotSecurityException e) {
            Logger.error(this, "User does not have the permissions required", e);
        } catch (DotDataException e){
            Logger.error(this, "Error Getting Relationships for ContentType: " + contentTypeInode, e);
        }

        return relationships;
    }
    @Override
    public  List<Contentlet> dbRelatedContent(final Relationship relationship, final Contentlet contentlet) throws DotDataException {

        final String stInode = contentlet.getContentTypeId();

        final boolean selfJoinRelationship = relationship.getParentStructureInode().equalsIgnoreCase(stInode)
                && relationship.getChildStructureInode().equalsIgnoreCase(stInode);

        if(!selfJoinRelationship) {
            final boolean hasParent = relationship.getParentStructureInode()
                    .equalsIgnoreCase(stInode);

            return dbRelatedContent(relationship, contentlet, hasParent);
        } else{//if the relationship is self joined, get the related content where the contentlet is parent or child
            List<Contentlet> contentletList = dbRelatedContent(relationship, contentlet, true);
            contentletList.addAll(dbRelatedContent(relationship, contentlet, false));
            return contentletList;
        }
    }

    @Override
    public  List<Contentlet> dbRelatedContent(final Relationship relationship, final Contentlet contentlet,
            final boolean hasParent) throws DotDataException {
        return dbRelatedContent (relationship, contentlet, hasParent, false, "tree_order", -1, -1);
    }

    @Override
    public  List<Contentlet> dbRelatedContent(final Relationship relationship, final Contentlet contentlet,
            final boolean hasParent, final boolean live, final String orderBy) throws DotDataException {
        return dbRelatedContent (relationship, contentlet, hasParent, live, orderBy, -1, -1);
    }

    @SuppressWarnings("deprecation")
    public  List<Contentlet> dbRelatedContent(final Relationship relationship, final Contentlet contentlet,
            final boolean hasParent, final boolean live, final String orderBy, int limit, int offset)
            throws DotDataException {
        List<Contentlet> matches = new ArrayList<>();

        if(contentlet == null || !InodeUtils.isSet(contentlet.getIdentifier())) {
            return matches;
        }
        String iden = "";
        try{
            iden = APILocator.getIdentifierAPI().find(contentlet).getInode();
        }catch(DotHibernateException dhe){
            Logger.error(this.getClass(), "Unable to retrieve Identifier", dhe);
        }

        if(!InodeUtils.isSet(iden)) {
            return matches;
        }

        return hasParent? dbRelatedContentByParent(iden, relationship.getRelationTypeValue(),live, orderBy, limit, offset):
                dbRelatedContentByChild(iden, relationship.getRelationTypeValue(),live, orderBy, limit, offset);
    }

    @Override
    public  List<Tree> relatedContentTrees(final Relationship relationship, final Contentlet contentlet) throws  DotDataException {
        final String stInode = contentlet.getContentTypeId();
        List<Tree> matches = new ArrayList<>();
        if (relationship.getParentStructureInode().equalsIgnoreCase(stInode)) {
            matches = relatedContentTrees(relationship, contentlet, true);
        } else if (relationship.getChildStructureInode().equalsIgnoreCase(stInode)) {
            matches = relatedContentTrees(relationship, contentlet, false);
        }
        return matches;
    }
    @Override
    @SuppressWarnings("deprecation")
	public  List<Tree> relatedContentTrees(final Relationship relationship, final Contentlet contentlet, final boolean hasParent) throws  DotDataException {
        final List<Tree> matches = new ArrayList<>();
        List<Tree> trees;
        final Identifier iden = APILocator.getIdentifierAPI().find(contentlet);
        if (hasParent) {
            trees = TreeFactory.getTreesByParentAndRelationType(iden, relationship.getRelationTypeValue());
            for (Tree tree : trees) {
                matches.add(tree);
            }
        } else {
            trees = TreeFactory.getTreesByChildAndRelationType(iden, relationship.getRelationTypeValue());
            for (Tree tree : trees) {
                matches.add(tree);
            }
        }
        return matches;
    }

    public  boolean isChildField(final Relationship relationship, final Field field) {
        try {
            return sameParentAndChild(relationship) ? field.variable()
                    .equalsIgnoreCase(relationship.getChildRelationName())
                    : isParent(relationship,
                            FactoryLocator.getContentTypeFactory().find(field.contentTypeId()));
        } catch (DotDataException e) {
            Logger.warnAndDebug(this.getClass(),
                    "Error searching content type with id " + field.contentTypeId(), e);
        }

        return false;
    }

    /**
     * @deprecated For relationship fields use {@link RelationshipFactory#isChildField(Relationship, Field)} instead
     * @param relationship
     * @param contentTypeIf
     * @return
     */
    @Deprecated
    @Override
    public  boolean isParent(final Relationship relationship, final ContentTypeIf contentTypeIf) {
        return relationship.getParentStructureInode().equalsIgnoreCase(contentTypeIf.id()) &&
                !(sameParentAndChildRelationName(relationship) && relationship
                        .getChildStructureInode()
                        .equalsIgnoreCase(relationship.getParentStructureInode()));
    }

    /**
     * @deprecated For relationship fields use {@link RelationshipFactory#isParentField(Relationship, Field)} instead
     * @param relationship
     * @param contentTypeIf
     * @return
     */
    @Deprecated
    @Override
    public  boolean isChild(final Relationship relationship, final ContentTypeIf contentTypeIf) {
        return relationship.getChildStructureInode().equalsIgnoreCase(contentTypeIf.id()) &&
                !(sameParentAndChildRelationName(relationship) && relationship
                        .getChildStructureInode()
                        .equalsIgnoreCase(relationship.getParentStructureInode()));
    }

    @Override
    public boolean isParentField(final Relationship relationship, final Field field) {
	    return !isChildField(relationship, field);
    }

    private boolean sameParentAndChildRelationName(final Relationship relationship){
        return relationship.getParentRelationName() != null
                && relationship.getChildRelationName() != null && relationship
                .getParentRelationName().equals(relationship.getChildRelationName());
    }
    @Override
    public  boolean sameParentAndChild(final Relationship rel) {
        return rel.getChildStructureInode().equalsIgnoreCase(rel.getParentStructureInode());
    }

    @Override
    public void save(final Relationship relationship) throws DotDataException {
        relationship.setModDate(new Date());

        if(UtilMethods.isSet(relationship.getInode())){
            if(relationshipExists(relationship.getInode())){
                updateRelationshipInDB(relationship);
            } else {
                insertRelationshipInDB(relationship);
            }
        } else{
            relationship.setInode(UUIDGenerator.generateUuid());
            insertRelationshipInDB(relationship);
        }

        cache.removeRelationshipByInode(relationship);
        try{
            cache.removeRelationshipsByStruct(relationship.getParentStructure());
            cache.removeRelationshipsByStruct(relationship.getChildStructure());
        }
        catch(Exception e){
            Logger.error(this.getClass(), e.getMessage(),e);
        }
    }

    private boolean relationshipExists(final String inode) throws DotDataException {
        final List<Map<String, Object>> results = new DotConnect()
                .setSQL(sql.FIND_BY_INODE)
                .addParam(inode)
                .loadObjectResults();
        return UtilMethods.isSet(results);
    }

    private void insertRelationshipInDB(final Relationship relationship) throws DotDataException{
	    DotConnect dc = new DotConnect();
	    dc.setSQL(sql.INSERT_RELATIONSHIP);
	    dc.addParam(relationship.getInode());
	    dc.addParam(relationship.getParentStructureInode());
	    dc.addParam(relationship.getChildStructureInode());
        dc.addParam(relationship.getParentRelationName());
        dc.addParam(relationship.getChildRelationName());
        dc.addParam(relationship.getRelationTypeValue());
        dc.addParam(relationship.getCardinality());
        dc.addParam(relationship.isParentRequired());
        dc.addParam(relationship.isChildRequired());
        dc.addParam(relationship.isFixed());
        dc.addParam(relationship.getModDate());
        dc.loadResult();
    }

    private void updateRelationshipInDB(final Relationship relationship) throws DotDataException{
        DotConnect dc = new DotConnect();
        dc.setSQL(sql.UPDATE_RELATIONSHIP);
        dc.addParam(relationship.getParentStructureInode());
        dc.addParam(relationship.getChildStructureInode());
        dc.addParam(relationship.getParentRelationName());
        dc.addParam(relationship.getChildRelationName());
        dc.addParam(relationship.getRelationTypeValue());
        dc.addParam(relationship.getCardinality());
        dc.addParam(relationship.isParentRequired());
        dc.addParam(relationship.isChildRequired());
        dc.addParam(relationship.isFixed());
        dc.addParam(relationship.getModDate());
        dc.addParam(relationship.getInode());
        dc.loadResult();
    }

   @Override
    public void delete(final String inode) throws DotDataException {
        delete(byInode(inode));
    }

    @Override
    public void delete(final Relationship relationship) throws DotDataException {
        delete(relationship, false);
    }

    @Override
    public void deleteKeepTrees(final Relationship relationship) throws DotDataException {
        delete(relationship, true);
    }

    private void delete(final Relationship relationship, final Boolean keepTreeRecords) throws DotDataException {

	    deleteRelationshipInDB(relationship.getInode());

        if ( !keepTreeRecords ) {
            TreeFactory.deleteTreesByRelationType(relationship.getRelationTypeValue());
        }

        cache.removeRelationshipByInode(relationship);
        try {
            cache.removeRelationshipsByStruct(relationship.getParentStructure());
            cache.removeRelationshipsByStruct(relationship.getChildStructure());
        } catch (DotCacheException e) {
            Logger.error(this.getClass(), e.getMessage(), e);

        }
    }

    private void deleteRelationshipInDB(final String inode) throws DotDataException{
        DotConnect dc = new DotConnect();
        dc.setSQL(sql.DELETE_RELATIONSHIP_BY_INODE);
        dc.addParam(inode);
        dc.loadResult();
    }

    public  List<Contentlet> dbRelatedContentByParent(final String parentIdentifier, final String relationType, final boolean live,
            final String orderBy) throws DotDataException{
	    return dbRelatedContentByParent(parentIdentifier, relationType,live,orderBy, -1, -1);
    }

    public List<Contentlet> dbRelatedContentByParent(final String parentIdentifier,
            final String relationType, final boolean live,
            final String orderByIn, final int limit, final int offset) throws DotDataException {

        String liveOrWorking = live ? "cvi.live_inode" : "cvi.working_inode";
        String orderBy = SQLUtil.sanitizeSortBy(orderByIn);
        // we only include contentlet for sorting
        boolean includeContentlet = !("sort_order".equalsIgnoreCase(orderBy) || "tree_order".equalsIgnoreCase(orderBy));

        final StringBuilder query = new StringBuilder("select " + liveOrWorking + " as inode ")
                .append(" from contentlet_version_info cvi, tree t ");

        if (includeContentlet) {
            query.append(", contentlet c ");
        }

        query.append(" where t.parent= ? "
                + " and t.relation_type = ? "
                + " and  " + liveOrWorking + " is not null "
                + " and t.child = cvi.identifier ");

        if (includeContentlet) {
            query.append(" and " + liveOrWorking + " = c.inode ");
        }

        if (includeContentlet) {
            query.append(" order by c.").append(orderBy);
        } else {
            query.append(" order by t.tree_order, cvi.version_ts");
        }

        final DotConnect dc = new DotConnect(query.toString());
        dc.addParam(parentIdentifier);
        dc.addParam(relationType);

        if (limit > -1){
            dc.setMaxRows(limit);
        }

        if (offset > -1){
            dc.setStartRow(offset);
        }

        final List<Map<String, Object>> results = dc.loadObjectResults();
        final List<Contentlet> contentlets = new ArrayList<>();

        for(final Map<String,Object> map : results){
            try {
                contentlets.add(APILocator.getContentletAPI().find((String) map.get("inode"),APILocator.systemUser(),false));
            } catch (DotSecurityException e) {//Never Should throw DotSecurityException since is using systemUser but just in case
                Logger.error(this, e.getMessage() + "inode: " + map.get("inode"));
            }
        }

        return contentlets;
    }

    public  List<Contentlet> dbRelatedContentByChild(final String childIdentifier, final String relationType, final boolean live,
            final String orderBy) throws DotDataException {
	    return dbRelatedContentByChild(childIdentifier, relationType, live,orderBy, -1, -1);
    }

    @SuppressWarnings("unchecked")
    public List<Contentlet> dbRelatedContentByChild(final String childIdentifier,
            final String relationType, final boolean live,
            final String orderByIn, final int limit, final int offset) throws DotDataException {

        String orderBy = SQLUtil.sanitizeSortBy(orderByIn);
        String liveOrWorking = live ? "cvi.live_inode" : "cvi.working_inode";

        // we only include contentlet for sorting
        boolean includeContentlet = !("sort_order".equalsIgnoreCase(orderBy) || "tree_order".equalsIgnoreCase(orderBy));

        final StringBuilder query = new StringBuilder();

        query.append(" select " + liveOrWorking + " as inode ");
        query.append(" from tree t, contentlet_version_info cvi ");

        if (includeContentlet) {
            query.append(" , contentlet c ");
        }

        query.append(" where t.child = ? "
                + " and t.relation_type = ? "
                + " and  " + liveOrWorking + " is not null "
                + " and t.parent = cvi.identifier ");

        if (includeContentlet) {
            query.append(" and " + liveOrWorking + " = c.inode ");
        }

        if (includeContentlet) {
            query.append(" order by c.").append(orderBy);
        } else {
            query.append(" order by t.tree_order, cvi.version_ts");
        }

        final DotConnect dc = new DotConnect();
        dc.setSQL(query.toString());
        dc.addParam(childIdentifier);
        dc.addParam(relationType);

        if (limit > -1){
            dc.setMaxRows(limit);
        }

        if (offset > -1){
            dc.setStartRow(offset);
        }

        final List<Map<String, Object>> results = dc.loadObjectResults();
        final List<Contentlet> contentlets = new ArrayList<>();

        for(final Map<String,Object> map : results){
            try {
                contentlets.add(APILocator.getContentletAPI().find((String) map.get("inode"),APILocator.systemUser(),false));
            } catch (DotSecurityException e) {//Never Should throw DotSecurityException since is using systemUser but just in case
                Logger.error(this, e.getMessage() + "inode: " + map.get("inode"));
            }
        }

        return contentlets;

    }

    /**
     * This method can be used to find the next in a sort order
     * @param parentInode The parent Relationship
     * @param relationType
     * @return the max in the sort order
     */
    @Override
    public int maxSortOrder(final String parentInode, final String relationType) {

        DotConnect dc = new DotConnect();
        dc.setSQL(sql.SELECT_MAX_TREE_ORDER);
        dc.addParam(parentInode);
        dc.addParam(relationType);

        return dc.getInt("tree_order");
    }

    /**
     * Removes the relationships from the list of related contentlets to the passed in contentlet
     * @param contentlet
     * @param relationship
     * @param relatedContentlets
     * @throws DotDataException
     */
    @Override
    public  void deleteByContent(final Contentlet contentlet, final Relationship relationship,
            final List<Contentlet> relatedContentlets) throws DotDataException{
        if(contentlet.getIdentifier()!=null){
            for (Contentlet con : relatedContentlets) {
                if(con.getIdentifier()!=null){
                    TreeFactory.deleteTreesByParentAndChildAndRelationType(contentlet.getIdentifier(),
                            con.getIdentifier(), relationship.getRelationTypeValue());
                    TreeFactory.deleteTreesByParentAndChildAndRelationType(con.getIdentifier(),
                            contentlet.getIdentifier(), relationship.getRelationTypeValue());
                }
            }
        }
    }

    /**
     * Creates a relationship between 2 contentlets
     *
     * @param parent contentlet parent
     * @param child contentlet child
     * @param relationType
     * @throws DotDataException
     */
    @Override
    public void addRelationship(final String parent, final String child, final String relationType)throws DotDataException {
        Tree tree = TreeFactory.getTree(parent, child,relationType);
        if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())) {
            tree.setParent(parent);
            tree.setChild(child);
            tree.setRelationType(relationType);
            TreeFactory.saveTree(tree);
        } else {
            tree.setRelationType(relationType);
            TreeFactory.saveTree(tree);
        }
    }

    @Override
    public List<Relationship> getOneSidedRelationships(final ContentTypeIf contentType, final int limit, final int offset) throws DotDataException {

        DotConnect dc = new DotConnect();
        DotPreconditions.checkArgument(limit != 0, "limit param must be more than 0");
        dc.setSQL(sql.SELECT_ONE_SIDE_RELATIONSHIP);
        dc.addParam(contentType.id());
        dc.addParam(contentType.id());
        dc.setMaxRows((limit < 0) ? 10000 : limit);
        dc.setStartRow(offset);

        return new DbRelationshipTransformer(dc.loadObjectResults()).asList();
    }

    @Override
    public long getOneSidedRelationshipsCount(final ContentType contentType) throws DotDataException {
        final DotConnect dc = new DotConnect();
        dc.setSQL(sql.SELECT_ONE_SIDE_RELATIONSHIP_COUNT);
        dc.addParam(contentType.id());
        dc.addParam(contentType.id());
        final List<Map<String,String>> results = dc.loadResults();
        return ConversionUtils.toLong(results.get(0).get("relationship_count"));
    }
}
