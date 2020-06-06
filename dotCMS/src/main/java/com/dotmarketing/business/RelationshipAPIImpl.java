package com.dotmarketing.business;


import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.RelationshipFactory;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImmutableRelationshipField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeIf;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;

import com.dotmarketing.portlets.structure.transform.ContentletRelationshipsTransformer;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.commons.beanutils.BeanUtils;
import java.util.Map;

// THIS IS A FAKE API SO PEOPLE CAN FIND AND USE THE RELATIONSHIPFACTORY
public class RelationshipAPIImpl implements RelationshipAPI {

    private final RelationshipFactory relationshipFactory;
    
    public RelationshipAPIImpl(RelationshipFactory relationshipFactory) {
        this.relationshipFactory = relationshipFactory;
    }
    
    
    public RelationshipAPIImpl() {
        this.relationshipFactory = FactoryLocator.getRelationshipFactory();
    }

    @WrapInTransaction
    @Override
    public void deleteByContentType(ContentTypeIf type) throws DotDataException {
        this.relationshipFactory.deleteByContentType(type);
    }

    @CloseDBIfOpened
    @Override
    public Relationship byInode(String inode) {
        return this.relationshipFactory.byInode(inode);
    }

    @CloseDBIfOpened
    @Override
    public Relationship byTypeValue(final String typeValue) {
        return this.relationshipFactory.byTypeValue(typeValue);
    }

    @CloseDBIfOpened
    @Override
    public Optional<Relationship> byParentChildRelationName(final ContentType contentType,
            final String relationName) {
        return this.relationshipFactory.byParentChildRelationName(contentType, relationName);
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> dbAllByTypeValue(final String typeValue) {
        return this.relationshipFactory.dbAllByTypeValue(typeValue);
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> byParent(ContentTypeIf parent) throws DotDataException {
        return this.relationshipFactory.byParent(parent);
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> byChild(ContentTypeIf child) throws DotDataException {
        return this.relationshipFactory.byChild(child);
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> byContentType(ContentTypeIf type) throws DotDataException {
        return this.relationshipFactory.byContentType(type);
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> byContentType(ContentTypeIf st, boolean hasParent) throws DotDataException{
        if(hasParent) {
            return this.relationshipFactory.byParent(st);
        }else{
            return this.relationshipFactory.byChild(st);
        }
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> byContentType(ContentTypeIf type, String orderBy){
        return this.relationshipFactory.byContentType(type, orderBy);
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet)
            throws DotDataException {
        return this.relationshipFactory.dbRelatedContent(relationship, contentlet);
    }

    @Override
    public List<Contentlet> dbRelatedContent(Relationship relationship, Contentlet contentlet, boolean hasParent)
            throws DotDataException {
        return this.relationshipFactory.dbRelatedContent(relationship, contentlet, hasParent);
    }

    @CloseDBIfOpened
    @Override
    public List<Tree> relatedContentTrees(Relationship relationship, Contentlet contentlet) throws DotDataException {
        return this.relationshipFactory.relatedContentTrees(relationship, contentlet);
    }

    @CloseDBIfOpened
    @Override
    public List<Tree> relatedContentTrees(Relationship relationship, Contentlet contentlet, boolean hasParent) throws DotDataException {
        return this.relationshipFactory.relatedContentTrees(relationship, contentlet, hasParent);
    }

    /**
     * @deprecated For relationship fields use {@link RelationshipAPI#isChildField(Relationship, com.dotcms.contenttype.model.field.Field)} instead
     * @param rel
     * @param st
     * @return
     */
    @Deprecated
    @Override
    public boolean isParent(Relationship rel, ContentTypeIf st) {
        return this.relationshipFactory.isParent(rel, st);
    }

    @Override
    public boolean isChildField(Relationship rel, com.dotcms.contenttype.model.field.Field field) {
        return this.relationshipFactory.isChildField(rel, field);
    }

    /**
     * @deprecated For relationship fields use {@link RelationshipAPI#isParentField(Relationship, com.dotcms.contenttype.model.field.Field)} instead
     * @param rel
     * @param st
     * @return
     */
    @Deprecated
    @Override
    public boolean isChild(Relationship rel, ContentTypeIf st) {
        return this.relationshipFactory.isChild(rel, st);
    }

    @Override
    public boolean isParentField(Relationship rel, com.dotcms.contenttype.model.field.Field field) {
        return this.relationshipFactory.isParentField(rel, field);
    }

    @Override
    public boolean sameParentAndChild(Relationship rel) {
        return this.relationshipFactory.sameParentAndChild(rel);
    }

    @WrapInTransaction
    @Override
    public void save(Relationship relationship) throws DotDataException {
        checkRelationshipConstraints(relationship);
        this.relationshipFactory.save(relationship);
    }

    /**
     * Guarantees a unique relationship for a field
     * @param relationship
     */
    private void checkRelationshipConstraints(final Relationship relationship)
            throws DotDataException {

        if (relationship.isRelationshipField() && doesRelationshipFieldAlreadyExist(relationship)) {
            Logger.error(this, "The relationship " + relationship.getRelationTypeValue() +" already exists");
            throw new DotValidationException(
                    "The relationship " + relationship.getRelationTypeValue() +" already exists");
        }
    }

    /**
     * @param relationship
     * @return
     * @throws DotDataException
     */
    private boolean doesRelationshipFieldAlreadyExist(final Relationship relationship)
            throws DotDataException {

        List<Relationship> relationships;
        //Checks if it is a self-joined relationship
        if (sameParentAndChild(relationship)){
            relationships = byContentType(relationship.getChildStructure());
            boolean duplicatesFound = false;

            if (relationship.getParentRelationName() != null) {

                //Verifies that the parent field does not belong to another relationship as parent
                duplicatesFound = relationships.stream().anyMatch(
                        rel -> relationship.getParentRelationName()
                                .equals(rel.getParentRelationName())
                                && !rel.getInode().equals(relationship.getInode()) && rel
                                .isRelationshipField());

                //Verifies that the parent field does not belong to another relationship as child
                duplicatesFound |= relationships.stream().anyMatch(
                        rel -> relationship.getParentRelationName()
                                .equals(rel.getChildRelationName())
                                && !rel.getInode().equals(relationship.getInode()) && rel
                                .isRelationshipField());
            }


            if (relationship.getChildRelationName() != null) {
                //Verifies that the child field does not belong to another relationship as parent
                duplicatesFound |= relationships.stream().anyMatch(
                        rel -> relationship.getChildRelationName()
                                .equals(rel.getParentRelationName())
                                && !rel.getInode().equals(relationship.getInode()) && rel
                                .isRelationshipField());

                //Verifies that the child field does not belong to another relationship as child
                duplicatesFound |= relationships.stream().anyMatch(
                        rel -> relationship.getChildRelationName()
                                .equals(rel.getChildRelationName())
                                && !rel.getInode().equals(relationship.getInode()) && rel
                                .isRelationshipField());
            }

            return duplicatesFound;
        }else {
            //Verifies that the parent field does not belong to another relationship
            if (relationship.getParentRelationName() != null) {
                relationships = byChild(relationship.getChildStructure());
                return relationships.stream().anyMatch(
                        rel -> relationship.getParentRelationName()
                                .equals(rel.getParentRelationName())
                                && !rel.getInode().equals(relationship.getInode()) && rel
                                .isRelationshipField());
            }

            //Verifies that the child field does not belong to another relationship
            if (relationship.getChildRelationName() != null) {
                relationships = byParent(relationship.getParentStructure());
                return relationships.stream().anyMatch(
                        rel -> relationship.getChildRelationName()
                                .equals(rel.getChildRelationName())
                                && !rel.getInode().equals(relationship.getInode()) && rel
                                .isRelationshipField());
            }
        }

        return false;
    }

    @WrapInTransaction
    @Override
    public void save(Relationship relationship, String inode) throws DotDataException {
        checkReadOnlyFields(relationship, inode);
        this.relationshipFactory.save(relationship);
    }

    private void checkReadOnlyFields(final Relationship relationship, final String inode) {
        if (UtilMethods.isSet(inode) && UtilMethods.isSet(relationship)) {

            //Check if the relationship already exists
            Relationship currentRelationship = this.relationshipFactory.byInode(inode);
            if (UtilMethods.isSet(currentRelationship) && UtilMethods.isSet(currentRelationship.getInode())) {

                //Check the parent has not been changed
                if (!relationship.getParentStructureInode().equals(currentRelationship.getParentStructureInode())) {
                    throw new DotValidationException("error.relationship.parent.structure.cannot.be.changed");
                }

                //Check the child has not been changed
                if (!relationship.getChildStructureInode().equals(currentRelationship.getChildStructureInode())) {
                    throw new DotValidationException("error.relationship.child.structure.cannot.be.changed");
                }

                //Check the typeValue has not been changed
                if (!relationship.getRelationTypeValue().equals(currentRelationship.getRelationTypeValue())) {
                    throw new DotValidationException("error.relationship.type.value.cannot.be.changed");
                }

            }
        }
    }

    @WrapInTransaction
    @Override
    public void create(Relationship relationship) throws DotDataException {
        save(relationship);
    }

    @WrapInTransaction
    @Override
    public void delete(String inode) throws DotDataException {
        this.relationshipFactory.delete(inode);
    }

    @WrapInTransaction
    @Override
    public void delete(Relationship relationship) throws DotDataException {
        this.relationshipFactory.delete(relationship);
    }

    @WrapInTransaction
    @Override
    public void deleteKeepTrees(Relationship relationship) throws DotDataException {
        this.relationshipFactory.deleteKeepTrees(relationship);
    }

    @CloseDBIfOpened
    @Override
    public int maxSortOrder(String parentInode, String relationType) {
        return this.relationshipFactory.maxSortOrder(parentInode, relationType);
    }

    @WrapInTransaction
    @Override
    public void deleteByContent(Contentlet contentlet, Relationship relationship, List<Contentlet> relatedContentlets) throws DotDataException {
        this.relationshipFactory.deleteByContent(contentlet, relationship, relatedContentlets);
    }

    @WrapInTransaction
    @Override
    public void addRelationship(String parent, String child, String relationType) throws DotDataException {
        this.relationshipFactory.addRelationship(parent, child, relationType);
    }

    @CloseDBIfOpened
    @Override
    public List<Relationship> getOneSidedRelationships(final ContentType contentType,
            final int limit, final int offset) throws DotDataException {
        DotPreconditions.checkNotNull(contentType, IllegalArgumentException.class, "Content Type is required");
        return this.relationshipFactory.getOneSidedRelationships(contentType, limit, offset);
    }

    @CloseDBIfOpened
    @Override
    public long getOneSidedRelationshipsCount(final ContentType contentType) throws DotDataException {
        return this.relationshipFactory.getOneSidedRelationshipsCount(contentType);
    }

    @Override
    public ContentletRelationships getContentletRelationshipsFromMap(Contentlet contentlet, final Map<Relationship, List<Contentlet>> contentRelationships) {
        return new ContentletRelationshipsTransformer(contentlet, contentRelationships).findFirst();
    }

    @Override
    public Relationship getRelationshipFromField(final Field field, final User user)
            throws DotDataException, DotSecurityException {

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);

        final String contentTypeVar    = contentTypeAPI.find(field.getStructureInode()).variable();
        final String fieldRelationType = field.getFieldRelationType();

        return APILocator.getRelationshipAPI().byTypeValue(
                fieldRelationType.contains(StringPool.PERIOD) ? fieldRelationType
                        : contentTypeVar + StringPool.PERIOD + field
                                .getVelocityVarName());


    }

    @Override
    public Relationship getRelationshipFromField(final com.dotcms.contenttype.model.field.Field field, final User user)
            throws DotDataException, DotSecurityException {

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);

        final String contentTypeVar    = contentTypeAPI.find(field.contentTypeId()).variable();

        final String fieldRelationType = field.relationType();
        return APILocator.getRelationshipAPI().byTypeValue(
                fieldRelationType.contains(StringPool.PERIOD) ? fieldRelationType
                        :contentTypeVar + StringPool.PERIOD + field
                                .variable());
    }

    
    
    @VisibleForTesting
    protected String suggestNewFieldName(final ContentType con, final Relationship relationship, final boolean isChild) {
        final String name = UtilMethods.capitalize(con.variable());
        final boolean selfRelated = sameParentAndChild(relationship);
        String suffix = selfRelated? isChild?"Child" : "Parent": "s";

        if(name.toLowerCase().endsWith(suffix)) {
            return name;
        }

        String finalSuffix = "";

        if(isChild){
            if (relationship.getCardinality() != WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal()){
                finalSuffix = selfRelated?"Children":suffix;
            } else if(selfRelated){
                finalSuffix = suffix;
            }
        } else{
            if (relationship.getCardinality()  == WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()){
                finalSuffix = selfRelated?"Parents":suffix;
            } else if(selfRelated){
                finalSuffix = suffix;
            }
        }

       return name + finalSuffix;
    }




    @WrapInTransaction
    public void convertRelationshipToRelationshipField(final Relationship oldRelationship) throws DotDataException, DotSecurityException{
        //Transform Structures to Content Types
        final ContentType parentContentType = new StructureTransformer(
                oldRelationship.getParentStructure()).from();
        final ContentType childContentType = new StructureTransformer(
                oldRelationship.getChildStructure()).from();
        
        //Create Relationship Fields
        
        final String parentFieldName = suggestNewFieldName(parentContentType, oldRelationship,
                false);
        final String childFieldName = suggestNewFieldName(childContentType, oldRelationship, true);

        final com.dotcms.contenttype.model.field.Field parentRelationshipField = createRelationshipField(
                childFieldName, parentContentType.id(),
                oldRelationship.getCardinality(), oldRelationship.isChildRequired(),
                childContentType.variable());

        final com.dotcms.contenttype.model.field.Field childRelationshipField =createRelationshipField(parentFieldName, childContentType.id(),
                oldRelationship.getCardinality(), oldRelationship.isParentRequired(),
                parentContentType.variable() + "." + parentRelationshipField.variable());


        Relationship newRelationship = (Relationship) Try.of(()->  BeanUtils.cloneBean(oldRelationship)).getOrElseThrow(e-> new DotRuntimeException(e));
        
        
        newRelationship.setRelationTypeValue(parentContentType.variable() + "." + parentRelationshipField.variable());
        newRelationship.setParentRelationName(childRelationshipField.variable());
        newRelationship.setChildRelationName(parentRelationshipField.variable());
        
        save(newRelationship);
        
        
        
        migrateOldRelationshipReferences(oldRelationship, newRelationship);

    }

    @Override
    public List<Relationship> dbAll() {
        return relationshipFactory.dbAll();
    }

    private void migrateOldRelationshipReferences(Relationship oldRelationship,
            Relationship newRelationship) throws DotDataException {

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final Date now = DbConnectionFactory.now();
        DotConnect dc = new DotConnect();

        //update version_ts on children
        dc.setSQL("update contentlet_version_info set version_ts = ? where identifier in (select child from tree where relation_type = ?)");
        dc.addParam(now);
        dc.addParam(oldRelationship.getRelationTypeValue());
        dc.loadResult();

        //update version_ts on parents
        dc = new DotConnect();
        dc.setSQL("update contentlet_version_info set version_ts = ? where identifier in (select parent from tree where relation_type = ?)");
        dc.addParam(now);
        dc.addParam(oldRelationship.getRelationTypeValue());
        dc.loadResult();

        //Update tree table entries with the new Relationship
        dc = new DotConnect();
        dc.setSQL("update tree set relation_type = ? where relation_type = ?");
        dc.addParam(newRelationship.getRelationTypeValue());
        dc.addParam(oldRelationship.getRelationTypeValue());
        dc.loadResult();

        //Delete the old relationship
        //APILocator.getRelationshipAPI().delete(oldRelationship);

        //Reindex both Content Types, so the content show the relationships
        contentletAPI.refresh(oldRelationship.getParentStructure());
        contentletAPI.refresh(oldRelationship.getChildStructure());
    }

    /**
     * This creates a relationship field but DOES NOT create try to create the underlying relationship
     * @param fieldName
     * @param parentContentTypeID
     * @param cardinality
     * @param isRequired
     * @param childContentTypeVariable
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private com.dotcms.contenttype.model.field.Field createRelationshipField(final String fieldName, final String parentContentTypeID, final int cardinality, final boolean isRequired, final String childContentTypeVariable)
            throws DotDataException, DotSecurityException {
        final com.dotcms.contenttype.model.field.RelationshipField field = ImmutableRelationshipField.builder()
                .name(fieldName)
                .contentTypeId(parentContentTypeID)
                .values(String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.values()[cardinality].ordinal()))
                .indexed(true)
                .listed(false)
                .required(isRequired)
                .relationType(childContentTypeVariable)
                .skipRelationshipCreation(true)
                .build();

        return APILocator.getContentTypeFieldAPI().save(field,APILocator.systemUser());
    }
}
