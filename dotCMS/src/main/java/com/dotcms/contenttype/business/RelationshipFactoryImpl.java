package com.dotcms.contenttype.business;

import com.dotcms.contenttype.business.sql.RelationshipSQL;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentTypeIf;
import com.dotcms.contenttype.transform.relationship.DbRelationshipTransformer;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.ContentletTransformer;
import com.dotmarketing.portlets.structure.factories.RelationshipCache;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RelationshipFactoryImpl implements RelationshipFactory{

	private static final RelationshipSQL sql= RelationshipSQL.getInstance();

    private static final RelationshipCache cache = CacheLocator.getRelationshipCache();
	
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
        }catch (DotDataException e){
                Logger.error(this, "Error getting Relationship with inode: " + inode, e);
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

        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try {
            results = dc.loadObjectResults();
        } catch (DotDataException e) {
            Logger.error(this, "Error getting All Relationships", e);
        }
        return new DbRelationshipTransformer(results).asList();
    }
    @Override
    public  Relationship byTypeValue(final String typeValue){
        if(typeValue==null) {
            return null;
        }
		Relationship rel = null;
		try {
			rel = cache.getRelationshipByName(typeValue);
			if(rel != null && rel.getRelationTypeValue().equals(typeValue))
				return rel;
		} catch (DotCacheException e) {
			Logger.debug(this.getClass(), "Unable to access the cache to obtain the relationship", e);
		}

        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try {
        final DotConnect dc = new DotConnect();
        dc.setSQL(sql.FIND_BY_TYPE_VALUE);
        dc.addParam(typeValue.toLowerCase());
        results = dc.loadObjectResults();
        if (results.size() == 0) {
         return rel;
        }
        } catch (DotDataException e) {
            Logger.error(this,"Error getting relationships with typeValue: " + typeValue.toLowerCase(),e);
        }
        rel = new DbRelationshipTransformer(results).from();

		if(rel!= null && InodeUtils.isSet(rel.getInode())){
            cache.putRelationshipByInode(rel);
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

        final boolean hasParent = !selfJoinRelationship && relationship.getParentStructureInode().equalsIgnoreCase(stInode);

        return dbRelatedContent(relationship, contentlet, hasParent);
    }

    @Override
    public  List<Contentlet> dbRelatedContent(final Relationship relationship, final Contentlet contentlet,
            final boolean hasParent) throws  DotDataException {
        return dbRelatedContent (relationship, contentlet, hasParent, false, "tree_order");
    }

    @SuppressWarnings("deprecation")
    public  List<Contentlet> dbRelatedContent(final Relationship relationship, final Contentlet contentlet,
            final boolean hasParent, final boolean live, final String orderBy) throws  DotDataException {
        List<Contentlet> matches = new ArrayList<Contentlet>();

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

        if (hasParent) {
            matches = live ? dbRelatedContentByParent(iden, relationship.getRelationTypeValue(),true, orderBy):
                    dbRelatedContentByParent(iden, relationship.getRelationTypeValue(),false, orderBy);
        } else {
            matches = live ? dbRelatedContentByChild(iden, relationship.getRelationTypeValue(),true, orderBy):
                    dbRelatedContentByChild(iden, relationship.getRelationTypeValue(),false, orderBy);
        }
        return matches;
    }

    @Override
    public  List<Tree> relatedContentTrees(final Relationship relationship, final Contentlet contentlet) throws  DotDataException {
        final String stInode = contentlet.getContentTypeId();
        List<Tree> matches = new ArrayList<Tree>();
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
        final List<Tree> matches = new ArrayList<Tree>();
        List<Tree> trees = new ArrayList<Tree>();
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

    @Override
    public  boolean isParent(final Relationship relationship, final ContentTypeIf contentTypeIf) {
        if (relationship.getParentStructureInode().equalsIgnoreCase(contentTypeIf.id()) &&
                !(relationship.getParentRelationName().equals(relationship.getChildRelationName()) && relationship.getChildStructureInode().equalsIgnoreCase(relationship.getParentStructureInode()))) {
            return true;
        }
        return false;
    }
    @Override
    public  boolean isChild(final Relationship relationship, final ContentTypeIf contentTypeIf) {
        if (relationship.getChildStructureInode().equalsIgnoreCase(contentTypeIf.id()) &&
                !(relationship.getParentRelationName().equals(relationship.getChildRelationName()) && relationship.getChildStructureInode().equalsIgnoreCase(relationship.getParentStructureInode()))) {
            return true;
        }
        return false;
    }
    @Override
    public  boolean sameParentAndChild(final Relationship rel) {
        if (rel.getChildStructureInode().equalsIgnoreCase(rel.getParentStructureInode()) ) {
            return true;
        }
        return false;
    }

    @Override
    public void save(final Relationship relationship) throws DotDataException {

        if(UtilMethods.isSet(relationship.getInode())){
            if(relationshipExists(relationship.getInode())){
                insertInodeInDB(relationship);
                insertRelationshipInDB(relationship);
            } else {
                updateInodeInDB(relationship);
                updateRelationshipInDB(relationship);
            }
        } else{
            relationship.setInode(UUIDGenerator.generateUuid());
            insertInodeInDB(relationship);
            insertRelationshipInDB(relationship);
        }

        CacheLocator.getRelationshipCache().removeRelationshipByInode(relationship);
        try{
            CacheLocator.getRelationshipCache().removeRelationshipsByStruct(relationship.getParentStructure());
            CacheLocator.getRelationshipCache().removeRelationshipsByStruct(relationship.getChildStructure());
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
        return results.isEmpty();
    }

    private void insertInodeInDB(final Relationship relationship) throws DotDataException{
	    DotConnect dc = new DotConnect();
	    dc.setSQL(sql.INSERT_INODE);
	    dc.addParam(relationship.getInode());
	    dc.addParam(relationship.getiDate());
	    dc.addParam(relationship.getOwner());
	    dc.loadResult();
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
        dc.loadResult();
    }

    private void updateInodeInDB(final Relationship relationship) throws DotDataException{
	    DotConnect dc = new DotConnect();
	    dc.setSQL(sql.UPDATE_INODE);
	    dc.addParam(relationship.getInode());
	    dc.addParam(relationship.getiDate());
        dc.addParam(relationship.getOwner());
        dc.addParam(relationship.getInode());
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
	    deleteInodeInDB(relationship.getInode());

        if ( !keepTreeRecords ) {
            TreeFactory.deleteTreesByRelationType(relationship.getRelationTypeValue());
        }

        CacheLocator.getRelationshipCache().removeRelationshipByInode(relationship);
        try {
            CacheLocator.getRelationshipCache().removeRelationshipsByStruct(relationship.getParentStructure());
            CacheLocator.getRelationshipCache().removeRelationshipsByStruct(relationship.getChildStructure());
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

    private void deleteInodeInDB(final String inode) throws DotDataException{
        DotConnect dc = new DotConnect();
        dc.setSQL(sql.DELETE_INODE);
        dc.addParam(inode);
        dc.loadResult();
    }

	public  List<Contentlet> dbRelatedContentByParent(final String parentInode, final String relationType, final boolean live,
            final String orderBy) throws DotDataException {

	    final StringBuilder query = new StringBuilder("select cont1.inode, show_on_menu, title, mod_date, mod_user, sort_order, friendly_name, structure_inode, last_review, next_review, "
                + "review_interval, disabled_wysiwyg, cont1.identifier, language_id, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, "
                + "date11, date12, date13, date14, date15, date16, date17, date18, date19, date20, date21, date22, date23, date24, date25, text1, text2, "
                + "text3, text4, text5, text6, text7, text8, text9, text10, text11, text12, text13, text14, text15, text16, text17, text18, text19, "
                + "text20, text21, text22, text23, text24, text25, text_area1, text_area2, text_area3, text_area4, text_area5, text_area6, text_area7, "
                + "text_area8, text_area9, text_area10, text_area11, text_area12, text_area13, text_area14, text_area15, text_area16, text_area17, "
                + "text_area18, text_area19, text_area20, text_area21, text_area22, text_area23, text_area24, text_area25, integer1, integer2, integer3, "
                + "integer4, integer5, integer6, integer7, integer8, integer9, integer10, integer11, integer12, integer13, integer14, integer15, "
                + "integer16, integer17, integer18, integer19, integer20, integer21, integer22, integer23, integer24, integer25, \"float1\", "
                + "\"float2\", \"float3\", \"float4\", \"float5\", \"float6\", \"float7\", \"float8\", \"float9\", \"float10\", \"float11\", \"float12\", "
                + "\"float13\", \"float14\", \"float15\", \"float16\", \"float17\", \"float18\", \"float19\", \"float20\", \"float21\", \"float22\", "
                + "\"float23\", \"float24\", \"float25\", bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10, bool11, bool12, bool13, "
                + "bool14, bool15, bool16, bool17, bool18, bool19, bool20, bool21, bool22, bool23, bool24, bool25, ")
                .append("owner from contentlet cont1, inode ci1, tree tree1, contentlet_version_info vi1 where tree1.parent = ? and tree1.relation_type = ? ")
                .append("and tree1.child = cont1.identifier and cont1.inode = ci1.inode and vi1.identifier = cont1.identifier and " + (live?"vi1.live_inode":"vi1.working_inode"))
                .append(" = cont1.inode");

            if (UtilMethods.isSet(orderBy) && !(orderBy.trim().equals("sort_order") || orderBy.trim().equals("tree_order"))) {
            	query.append(" order by cont1.")
                        .append(orderBy);
            } else {
            	query.append(" order by tree1.tree_order");
            }

            DotConnect dc = new DotConnect();
            dc.setSQL(query.toString());
            dc.addParam(parentInode);
            dc.addParam(relationType);

            return new ContentletTransformer(dc.loadObjectResults()).asList();
    }

    @SuppressWarnings("unchecked")
    public  List<Contentlet> dbRelatedContentByChild(final String childInode, final String relationType, final boolean live,
            final String orderBy) throws DotDataException {

        final StringBuilder query = new StringBuilder("select cont1.inode, show_on_menu, title, mod_date, mod_user, sort_order, friendly_name, structure_inode, last_review, next_review, "
                + "review_interval, disabled_wysiwyg, cont1.identifier, language_id, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, "
                + "date11, date12, date13, date14, date15, date16, date17, date18, date19, date20, date21, date22, date23, date24, date25, text1, text2, "
                + "text3, text4, text5, text6, text7, text8, text9, text10, text11, text12, text13, text14, text15, text16, text17, text18, text19, "
                + "text20, text21, text22, text23, text24, text25, text_area1, text_area2, text_area3, text_area4, text_area5, text_area6, text_area7, "
                + "text_area8, text_area9, text_area10, text_area11, text_area12, text_area13, text_area14, text_area15, text_area16, text_area17, "
                + "text_area18, text_area19, text_area20, text_area21, text_area22, text_area23, text_area24, text_area25, integer1, integer2, integer3, "
                + "integer4, integer5, integer6, integer7, integer8, integer9, integer10, integer11, integer12, integer13, integer14, integer15, "
                + "integer16, integer17, integer18, integer19, integer20, integer21, integer22, integer23, integer24, integer25, \"float1\", "
                + "\"float2\", \"float3\", \"float4\", \"float5\", \"float6\", \"float7\", \"float8\", \"float9\", \"float10\", \"float11\", \"float12\", "
                + "\"float13\", \"float14\", \"float15\", \"float16\", \"float17\", \"float18\", \"float19\", \"float20\", \"float21\", \"float22\", "
                + "\"float23\", \"float24\", \"float25\", bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10, bool11, bool12, bool13, "
                + "bool14, bool15, bool16, bool17, bool18, bool19, bool20, bool21, bool22, bool23, bool24, bool25, ")
                .append("owner from contentlet cont1 join inode ci1 on (cont1.inode = ci1.inode) join contentlet_version_info vi1 on "
                        + "(" + (live?"vi1.live_inode":"vi1.working_inode") + " = cont1.inode) join tree tree1 on (tree1.parent = cont1.identifier) ")
                .append("where tree1.child = ? and tree1.relation_type = ?");


        if (UtilMethods.isSet(orderBy) && !(orderBy.trim().equals("sort_order") || orderBy.trim().equals("tree_order"))) {
            query.append(" order by cont1.")
                    .append(orderBy);
        } else {
            query.append(" order by tree1.tree_order");
        }

        DotConnect dc = new DotConnect();
        dc.setSQL(query.toString());
        dc.addParam(childInode);
        dc.addParam(relationType);

        return new ContentletTransformer(dc.loadObjectResults()).asList();
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
}
