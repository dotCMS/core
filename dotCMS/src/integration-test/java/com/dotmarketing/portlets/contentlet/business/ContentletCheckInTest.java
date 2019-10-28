package com.dotmarketing.portlets.contentlet.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.Maps;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Created by Jonathan Gamba.
 * Date: 3/20/12
 * Time: 12:12 PM
 */
public class ContentletCheckInTest extends ContentletBaseTest{

  @Test
  public void checkinInvalidFileContent () throws Exception {

      Folder folder1  = null;

      final boolean respectFrontendRoles=false;
      final String fileTypeId=APILocator.getContentTypeAPI(APILocator.systemUser()).find("fileAsset").id();
      final String uuid1 = APILocator.getShortyAPI().randomShorty();
      final String uuid3 = APILocator.getShortyAPI().randomShorty();
      
      final User user = APILocator.systemUser();
      final Host host = hostAPI.findDefaultHost(user, false);

      Contentlet con2 = null;

      try {
          folder1 = folderAPI.createFolders("/" + uuid1, host, user, false);
          final Language lang = languageAPI.getDefaultLanguage();
          folder1.setFilesMasks("*.txt");
          folder1.setDefaultFileType(fileTypeId);
          folderAPI.save(folder1, user, false);

          File file = File.createTempFile(uuid3, ".txt");
          try (FileWriter writer = new FileWriter(file)) {
              while (file.length() < 1024 * 10) {
                  writer.write("Im writing\n");
              }
          }

          final FileAsset asset = new FileAsset();
          asset.setLanguageId(lang.getId());
          asset.setBinary("fileAsset", file);
          asset.setTitle(file.getName());
          asset.setUnderlyingFileName(file.getName());
          asset.setContentTypeId(fileTypeId);
          asset.setHost(host.getIdentifier());
          asset.setFolder(folder1.getIdentifier());
          Contentlet con = contentletAPI.checkin(asset, user, true);

          con2 = contentletAPI.find(con.getInode(), user, true);

          assert (con.equals(con2));

          final FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(con2);

          // this fails becuase the name does not match the folder's file mask
          // but it leaves the original file removed from the index
          APILocator.getFileAssetAPI().renameFile(fileAsset, "test.fail", user, respectFrontendRoles);
          assert (!folderAPI.getContent(folder1, user, respectFrontendRoles)
                  .isEmpty());

      }finally{
          try {
              if (folder1 != null) {
                  folderAPI.delete(folder1, user, false);
              }
          }catch (Exception e) { e.printStackTrace(); }
      }
  }

    /**
     * This test is meant to test Relationship Cardinality One to One
     *
     * It creates 2 content types and create a relationship between them, then create 2 contentlets
     * one of each content type and relate them.
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void test_checkinContentlet_RelationshipOneToOneCardinality_Success() throws DotSecurityException, DotDataException{
        ContentType parentContentType = null;
        ContentType childContentType = null;
        try{
            //Create content types
            parentContentType = createContentType("parentContentType");
            childContentType = createContentType("childContentType");

            //Create Text and Relationship Fields
            final String textFieldString = "title";
            final String relationshipFieldString = "relationship";
            createTextField(textFieldString,parentContentType.id());
            createRelationshipField(relationshipFieldString,parentContentType.id(),childContentType.variable(), String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal()));
            createTextField(textFieldString,childContentType.id());

            //Create Contentlets
            Contentlet contentletParent = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"parent Contentlet").next();
            final Contentlet contentletChild = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet").nextPersisted();

            //Find Relationship
            final Relationship relationship = relationshipAPI.byParent(parentContentType).get(0);

            //Relate contentlets
            Map<Relationship, List<Contentlet>> relationshipListMap = Maps.newHashMap();
            relationshipListMap.put(relationship,CollectionsUtils.list(contentletChild));

            //Checkin of the parent to validate Relationships
            contentletParent = contentletAPI.checkin(contentletParent,relationshipListMap,user,false);

            //List Related Contentlets
            final List<Contentlet> relatedContent = contentletAPI.getRelatedContent(contentletParent,relationship,user,false);
            assertNotNull(relatedContent);
            assertEquals(1,relatedContent.size());
            assertEquals(contentletChild.getIdentifier(),relatedContent.get(0).getIdentifier());


        }finally {
            try {
                if (parentContentType != null) {
                    contentTypeAPI.delete(parentContentType);
                }
                if (childContentType != null) {
                    contentTypeAPI.delete(childContentType);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This test is meant to test Relationship Cardinality One to One
     *
     * It creates 2 content types and create a relationship between them, then create 2 child contentlets
     * and try to relate to the parent contentlet and since the cardinality is
     * one to one, it throws a DotContentletValidationException.
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
  @Test(expected = DotContentletValidationException.class)
  public void test_checkinContentlet_relationshipOneToOneCardinality_fails_whenRelatingMoreThanOneContentlet()
          throws DotSecurityException, DotDataException {
      ContentType parentContentType = null;
      ContentType childContentType = null;
      try{
          //Create content types
          parentContentType = createContentType("parentContentType");
          childContentType = createContentType("childContentType");

          //Create Text and Relationship Fields
          final String textFieldString = "title";
          final String relationshipFieldString = "relationship";
          createTextField(textFieldString,parentContentType.id());
          createRelationshipField(relationshipFieldString,parentContentType.id(),childContentType.variable(), String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal()));
          createTextField(textFieldString,childContentType.id());

          //Create Contentlets
          Contentlet contentletParent = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"parent Contentlet").next();
          final Contentlet contentletChild = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet").nextPersisted();
          final Contentlet contentletChild2 = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet 2").nextPersisted();

          //Find Relationship
          final Relationship relationship = relationshipAPI.byParent(parentContentType).get(0);

          //Relate contentlets
          Map<Relationship, List<Contentlet>> relationshipListMap = Maps.newHashMap();
          relationshipListMap.put(relationship,CollectionsUtils.list(contentletChild,contentletChild2));

          //Checkin of the parent to validate Relationships
          contentletAPI.checkin(contentletParent,relationshipListMap,user,false);


      }finally {
          if(parentContentType != null){
              contentTypeAPI.delete(parentContentType);
          }
          if(childContentType != null){
              contentTypeAPI.delete(childContentType);
          }
      }
  }

    /**
     * This test is meant to test Relationship Cardinality One to One
     *
     * It creates 2 content types and create a one to one relationship between them, then create 2
     * contentlets and relates to each other. When a third content tries to relate to the parent,
     * it should fail
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test(expected = DotContentletValidationException.class)
    public void test_checkinContentlet_relationshipOneToOneCardinality_fails_whenRelatedParentBelongsToAnotherRelationship()
            throws DotSecurityException, DotDataException {
        ContentType parentContentType = null;
        ContentType childContentType = null;
        try {
            //Create content types
            parentContentType = createContentType("parentContentType");
            childContentType = createContentType("childContentType");

            //Create Text and Relationship Fields
            final String textFieldString = "title";
            final String relationshipFieldString = "relationship";
            createTextField(textFieldString, parentContentType.id());
            createRelationshipField(relationshipFieldString, parentContentType.id(),
                    childContentType.variable(), String.valueOf(
                            WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal()));
            createTextField(textFieldString, childContentType.id());

            //Create Contentlets
            Contentlet contentletParent = new ContentletDataGen(parentContentType.id())
                    .setProperty(textFieldString, "parent Contentlet").next();
            final Contentlet contentletChild = new ContentletDataGen(childContentType.id())
                    .setProperty(textFieldString, "child Contentlet").nextPersisted();
            final Contentlet contentletChild2 = new ContentletDataGen(childContentType.id())
                    .setProperty(textFieldString, "child Contentlet 2").next();

            //Find Relationship
            final Relationship relationship = relationshipAPI.byParent(parentContentType).get(0);

            //Checkin of the parent to validate Relationships
            contentletParent = contentletAPI.checkin(contentletParent,
                    CollectionsUtils.map(relationship, CollectionsUtils.list(contentletChild)),
                    user, false);

            //This checkin should fail because of cardinality violation
            contentletAPI.checkin(contentletChild2,
                    CollectionsUtils.map(relationship, CollectionsUtils.list(contentletParent)),
                    user, false);

        } finally {
            if (parentContentType != null) {
                contentTypeAPI.delete(parentContentType);
            }
            if (childContentType != null) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    /**
     * This test is meant to test Relationship Cardinality One to One
     *
     * It creates 2 content types and create a one to one relationship between them, then create 2
     * contentlets and relates to each other. When a third content tries to relate to the child, it
     * should fail
     */
    @Test(expected = DotContentletValidationException.class)
    public void test_checkinContentlet_relationshipOneToOneCardinality_fails_whenRelatedChildBelongsToAnotherRelationship()
            throws DotSecurityException, DotDataException {
        ContentType parentContentType = null;
        ContentType childContentType = null;
        try {
            //Create content types
            parentContentType = createContentType("parentContentType");
            childContentType = createContentType("childContentType");

            //Create Text and Relationship Fields
            final String textFieldString = "title";
            final String relationshipFieldString = "relationship";
            createTextField(textFieldString, parentContentType.id());
            createRelationshipField(relationshipFieldString, parentContentType.id(),
                    childContentType.variable(), String.valueOf(
                            WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal()));
            createTextField(textFieldString, childContentType.id());

            //Create Contentlets
            Contentlet contentletParent = new ContentletDataGen(parentContentType.id())
                    .setProperty(textFieldString, "parent Contentlet").next();
            Contentlet contentletParent2 = new ContentletDataGen(parentContentType.id())
                    .setProperty(textFieldString, "parent Contentlet 2").next();

            final Contentlet contentletChild = new ContentletDataGen(childContentType.id())
                    .setProperty(textFieldString, "child Contentlet").nextPersisted();

            //Find Relationship
            final Relationship relationship = relationshipAPI.byParent(parentContentType).get(0);

            //Relate parent to child
            contentletAPI.checkin(contentletParent,
                    CollectionsUtils.map(relationship, CollectionsUtils.list(contentletChild)),
                    user, false);

            //This checkin should fail because of cardinality violation
            contentletAPI.checkin(contentletParent2,
                    CollectionsUtils.map(relationship, CollectionsUtils.list(contentletChild)),
                    user, false);

        } finally {
            if (parentContentType != null) {
                contentTypeAPI.delete(parentContentType);
            }
            if (childContentType != null) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    /**
     * This test is meant to test Relationship Cardinality One to Many
     *
     * It creates 2 content types and create a relationship between them, then create 2 child contentlets
     * and relate to the parent contentlet.
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
  @Test
    public void test_checkinContentlet_RelationshipOneToManyCardinality_Success() throws DotSecurityException, DotDataException{
        ContentType parentContentType = null;
        ContentType childContentType = null;
        try{
            //Create content types
            parentContentType = createContentType("parentContentType");
            childContentType = createContentType("childContentType");

            //Create Text and Relationship Fields
            final String textFieldString = "title";
            final String relationshipFieldString = "relationship";
            createTextField(textFieldString,parentContentType.id());
            createRelationshipField(relationshipFieldString,parentContentType.id(),childContentType.variable(), String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()));
            createTextField(textFieldString,childContentType.id());

            //Create Contentlets
            Contentlet contentletParent = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"parent Contentlet").next();
            final Contentlet contentletChild = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet").nextPersisted();
            final Contentlet contentletChild2 = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet 2").nextPersisted();

            //Find Relationship
            final Relationship relationship = relationshipAPI.byParent(parentContentType).get(0);

            //Relate contentlets
            Map<Relationship, List<Contentlet>> relationshipListMap = Maps.newHashMap();
            relationshipListMap.put(relationship,CollectionsUtils.list(contentletChild,contentletChild2));

            //Checkin of the parent to validate Relationships
            contentletParent = contentletAPI.checkin(contentletParent,relationshipListMap,user,false);

            //List Related Contentlets
            final List<Contentlet> relatedContent = contentletAPI.getRelatedContent(contentletParent,relationship,user,false);
            assertNotNull(relatedContent);
            assertEquals(2,relatedContent.size());
            assertEquals(contentletChild.getIdentifier(),relatedContent.get(0).getIdentifier());
            assertEquals(contentletChild2.getIdentifier(),relatedContent.get(1).getIdentifier());


        }finally {
            if(parentContentType != null){
                contentTypeAPI.delete(parentContentType);
            }
            if(childContentType != null){
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    /**
     * This test is meant to test Relationship Cardinality One to Many
     *
     * It creates 2 content types and create a relationship between them, then create 2 child contentlets
     * and 2 parent contentlets.
     * Then try to relate both child contentlets to both parent contentlets and since the cardinality is
     * one to many, it throws a DotContentletValidationException when trying to relate to the 2nd
     * parent contentlet.
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test(expected = DotContentletValidationException.class)
    public void test_checkinContentlet_RelationshipOneToManyCardinality_throwsDotContentletValidationException() throws DotSecurityException, DotDataException{
        ContentType parentContentType = null;
        ContentType childContentType = null;
        try{
            //Create content types
            parentContentType = createContentType("parentContentType");
            childContentType = createContentType("childContentType");

            //Create Text and Relationship Fields
            final String textFieldString = "title";
            final String relationshipFieldString = "relationship";
            createTextField(textFieldString,parentContentType.id());
            createRelationshipField(relationshipFieldString,parentContentType.id(),childContentType.variable(), String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()));
            createTextField(textFieldString,childContentType.id());

            //Create Contentlets
            Contentlet contentletParent = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"parent Contentlet").next();
            Contentlet contentletParent2 = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"parent Contentlet").next();
            final Contentlet contentletChild = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet").nextPersisted();
            final Contentlet contentletChild2 = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet 2").nextPersisted();

            //Find Relationship
            final Relationship relationship = relationshipAPI.byParent(parentContentType).get(0);

            //Relate contentlets
            Map<Relationship, List<Contentlet>> relationshipListMap = Maps.newHashMap();
            relationshipListMap.put(relationship,CollectionsUtils.list(contentletChild,contentletChild2));

            //Checkin of the parent to validate Relationships
            contentletParent = contentletAPI.checkin(contentletParent,relationshipListMap,user,false);

            //List Related Contentlets
            final List<Contentlet> relatedContent = contentletAPI.getRelatedContent(contentletParent,relationship,user,false);
            assertNotNull(relatedContent);
            assertEquals(2,relatedContent.size());
            assertEquals(contentletChild.getIdentifier(),relatedContent.get(0).getIdentifier());
            assertEquals(contentletChild2.getIdentifier(),relatedContent.get(1).getIdentifier());


            //Checkin of the parent to validate Relationships
            contentletParent2 = contentletAPI.checkin(contentletParent2,relationshipListMap,user,false);


        }finally {
            if(parentContentType != null){
                contentTypeAPI.delete(parentContentType);
            }
            if(childContentType != null){
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    /**
     * This test is meant to test Relationship Cardinality Many to Many
     *
     * It creates 2 content types and create a relationship between them, then create 2 child contentlets
     * and 2 parent contentlets.
     * Then try to relate both child contentlets to both parent contentlets.
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void test_checkinContentlet_RelationshipManyToManyCardinality_Success() throws DotSecurityException, DotDataException{
        ContentType parentContentType = null;
        ContentType childContentType = null;
        try{
            //Create content types
            parentContentType = createContentType("parentContentType");
            childContentType = createContentType("childContentType");

            //Create Text and Relationship Fields
            final String textFieldString = "title";
            final String relationshipFieldString = "relationship";
            createTextField(textFieldString,parentContentType.id());
            createRelationshipField(relationshipFieldString,parentContentType.id(),childContentType.variable(), String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()));
            createTextField(textFieldString,childContentType.id());

            //Create Contentlets
            Contentlet contentletParent = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"parent Contentlet").next();
            Contentlet contentletParent2 = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"parent Contentlet 2").next();
            final Contentlet contentletChild = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet").nextPersisted();
            final Contentlet contentletChild2 = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet 2").nextPersisted();

            //Find Relationship
            final Relationship relationship = relationshipAPI.byParent(parentContentType).get(0);

            //Relate contentlets
            Map<Relationship, List<Contentlet>> relationshipListMap = Maps.newHashMap();
            relationshipListMap.put(relationship,CollectionsUtils.list(contentletChild,contentletChild2));

            //Checkin of the parent to validate Relationships
            contentletParent = contentletAPI.checkin(contentletParent,relationshipListMap,user,false);

            //List Related Contentlets
            List<Contentlet> relatedContent = contentletAPI.getRelatedContent(contentletParent,relationship,user,false);
            assertNotNull(relatedContent);
            assertEquals(2,relatedContent.size());
            assertEquals(contentletChild.getIdentifier(),relatedContent.get(0).getIdentifier());
            assertEquals(contentletChild2.getIdentifier(),relatedContent.get(1).getIdentifier());

            //Checkin of the parent to validate Relationships
            contentletParent2 = contentletAPI.checkin(contentletParent2,relationshipListMap,user,false);

            relatedContent = contentletAPI.getRelatedContent(contentletParent2,relationship,user,false);
            assertNotNull(relatedContent);
            assertEquals(2,relatedContent.size());
            assertEquals(contentletChild.getIdentifier(),relatedContent.get(0).getIdentifier());
            assertEquals(contentletChild2.getIdentifier(),relatedContent.get(1).getIdentifier());


        }finally {
            if(parentContentType != null){
                contentTypeAPI.delete(parentContentType);
            }
            if(childContentType != null){
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    /**
     * This test is meant to test that you can not relate the contentlet to itself
     *
     * It creates a content type and create a relationship, then create a child contentlet
     * and try to relate to itself the contentlet, it throws a DotContentletValidationException.
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test(expected = DotContentletValidationException.class)
    public void test_checkinContentlet_RelationshipToItself_throwsDotContentletValidationException() throws DotSecurityException, DotDataException{
        ContentType parentContentType = null;
        try{
            //Create content type
            parentContentType = createContentType("parentContentType");

            //Create Text and Relationship Fields
            final String textFieldString = "title";
            final String relationshipFieldString = "relationship";
            createTextField(textFieldString,parentContentType.id());
            createRelationshipField(relationshipFieldString,parentContentType.id(),parentContentType.variable(), String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal()));

            //Create Contentlets
            Contentlet contentlet = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"parent Contentlet").nextPersisted();

            //Find Contentlet
            contentlet = contentletAPI.checkout(contentlet.getInode(),user,false);

            //Find Relationship
            final Relationship relationship = relationshipAPI.byParent(parentContentType).get(0);

            //Relate contentlets
            Map<Relationship, List<Contentlet>> relationshipListMap = Maps.newHashMap();
            relationshipListMap.put(relationship,CollectionsUtils.list(contentlet));

            //Checkin again the contentlet with the relationship to itself
            contentletAPI.checkin(contentlet,relationshipListMap,user,false);

        }finally {
            if(parentContentType != null){
                contentTypeAPI.delete(parentContentType);
            }
        }
    }

    private ContentType createContentType(final String name) throws DotSecurityException, DotDataException {
        return contentTypeAPI.save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                .owner(user.getUserId()).build());
    }

    private Field createRelationshipField(final String relationshipName, final String parentTypeId,
                                               final String childTypeVar, final String cardinality)
            throws DotSecurityException, DotDataException {

        final Field field = FieldBuilder.builder(RelationshipField.class).name(relationshipName)
                .contentTypeId(parentTypeId).values(cardinality).relationType(childTypeVar).build();

        return fieldAPI.save(field, user);
    }

    private Field createTextField(final String fieldName, final String contentTypeId)
            throws DotSecurityException, DotDataException {

        final Field field = FieldBuilder.builder(TextField.class).name(fieldName).contentTypeId(contentTypeId).build();

        return fieldAPI.save(field, user);
    }
}