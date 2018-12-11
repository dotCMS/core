package com.dotmarketing.portlets.contentlet.business;

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
import com.liferay.portal.model.User;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
          asset.setFileName(file.getName());
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
          if (folder1 != null) {
              folderAPI.delete(folder1, user, false);
          }
      }
  }


  @Test(expected = DotContentletValidationException.class)
  public void test_checkinContentlet_RelationshipOneToOneCardinality() throws DotSecurityException, DotDataException{
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
          final Contentlet contentletParent = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"parent Contentlet").nextPersisted();
          final Contentlet contentletChild = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet").nextPersisted();

          //Find Relationship
          final Relationship relationship = relationshipAPI.byParent(parentContentType).get(0);

          //Relate contentlets
          contentletAPI.relateContent(contentletParent, relationship,
                  CollectionsUtils.list(contentletChild), user, false);

          //Checkin of the parent to validate Relationships
          contentletParent.setInode("");
          contentletAPI.checkin(contentletParent,contentletAPI.getAllRelationships(contentletParent),null,null,user,false);

          //List Related Contentlets
          final List<Contentlet> relatedContent = contentletAPI.getRelatedContent(contentletParent,relationship,user,false);
          assertNotNull(relatedContent);
          assertEquals(1,relatedContent.size());

          //Create another contentlet Child
          final Contentlet contentletChild2 = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet 2").nextPersisted();

          //Relate 2 child contentlets to 1 parent
          contentletAPI.relateContent(contentletParent, relationship,
                  CollectionsUtils.list(contentletChild,contentletChild2), user, false);

          //Checkin of the parent to validate Relationships, should throw an Exception
          contentletParent.setInode("");
          contentletAPI.checkin(contentletParent,contentletAPI.getAllRelationships(contentletParent),null,null,user,false);

      }finally {
          if(parentContentType != null){
              contentTypeAPI.delete(parentContentType);
          }
          if(childContentType != null){
              contentTypeAPI.delete(childContentType);
          }
      }
  }

    @Test(expected = DotContentletValidationException.class)
    public void test_checkinContentlet_RelationshipOneToManyCardinality() throws DotSecurityException, DotDataException{
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
            final Contentlet contentletParent = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"parent Contentlet").nextPersisted();
            final Contentlet contentletChild = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet").nextPersisted();

            //Find Relationship
            final Relationship relationship = relationshipAPI.byParent(parentContentType).get(0);

            //Relate contentlets
            contentletAPI.relateContent(contentletParent, relationship,
                    CollectionsUtils.list(contentletChild), user, false);

            //Checkin of the parent to validate Relationships
            contentletParent.setInode("");
            contentletAPI.checkin(contentletParent,contentletAPI.getAllRelationships(contentletParent),null,null,user,false);

            //List Related Contentlets
            List<Contentlet> relatedContent = contentletAPI.getRelatedContent(contentletParent,relationship,user,false);
            assertNotNull(relatedContent);
            assertEquals(1,relatedContent.size());

            //Create another contentlet Child
            final Contentlet contentletChild2 = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet 2").nextPersisted();

            //Relate 2 child contentlets to 1 parent
            contentletAPI.relateContent(contentletParent, relationship,
                    CollectionsUtils.list(contentletChild,contentletChild2), user, false);

            //Checkin of the parent to validate Relationships
            contentletParent.setInode("");
            contentletAPI.checkin(contentletParent,contentletAPI.getAllRelationships(contentletParent),null,null,user,false);

            //List Related Contentlets
            relatedContent = contentletAPI.getRelatedContent(contentletParent,relationship,user,false);
            assertNotNull(relatedContent);
            assertEquals(2,relatedContent.size());

            //Create another contentlet Parent
            final Contentlet contentletParent2 = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"parent Contentlet 2").nextPersisted();

            //Relate 1 Child to the new Parent contentlet
            contentletAPI.relateContent(contentletParent2, relationship,
                    CollectionsUtils.list(contentletChild), user, false);

            //Checkin of the parent to validate Relationships, should throw an Exception
            contentletParent2.setInode("");
            contentletAPI.checkin(contentletParent2,contentletAPI.getAllRelationships(contentletParent2),null,null,user,false);

        }finally {
            if(parentContentType != null){
                contentTypeAPI.delete(parentContentType);
            }
            if(childContentType != null){
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test
    public void test_checkinContentlet_RelationshipManyToManyCardinality() throws DotSecurityException, DotDataException{
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
            final Contentlet contentletParent = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"parent Contentlet").nextPersisted();
            final Contentlet contentletChild = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet").nextPersisted();

            //Find Relationship
            final Relationship relationship = relationshipAPI.byParent(parentContentType).get(0);

            //Relate contentlets
            contentletAPI.relateContent(contentletParent, relationship,
                    CollectionsUtils.list(contentletChild), user, false);

            //Checkin of the parent to validate Relationships
            contentletParent.setInode("");
            contentletAPI.checkin(contentletParent,contentletAPI.getAllRelationships(contentletParent),null,null,user,false);

            //List Related Contentlets
            List<Contentlet> relatedContent = contentletAPI.getRelatedContent(contentletParent,relationship,user,false);
            assertNotNull(relatedContent);
            assertEquals(1,relatedContent.size());

            //Create another contentlet Child
            final Contentlet contentletChild2 = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet 2").nextPersisted();

            //Relate 2 child contentlets to 1 parent
            contentletAPI.relateContent(contentletParent, relationship,
                    CollectionsUtils.list(contentletChild,contentletChild2), user, false);

            //Checkin of the parent to validate Relationships
            contentletParent.setInode("");
            contentletAPI.checkin(contentletParent,contentletAPI.getAllRelationships(contentletParent),null,null,user,false);

            //List Related Contentlets
            relatedContent = contentletAPI.getRelatedContent(contentletParent,relationship,user,false);
            assertNotNull(relatedContent);
            assertEquals(2,relatedContent.size());

            //Create another contentlet Parent
            final Contentlet contentletParent2 = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"parent Contentlet 2").nextPersisted();

            //Relate 1 Child to the new Parent contentlet
            contentletAPI.relateContent(contentletParent2, relationship,
                    CollectionsUtils.list(contentletChild), user, false);

            //Checkin of the parent to validate Relationships, should throw an Exception
            contentletParent2.setInode("");
            contentletAPI.checkin(contentletParent2,contentletAPI.getAllRelationships(contentletParent2),null,null,user,false);

            //List Related Contentlets
            relatedContent = contentletAPI.getRelatedContent(contentletParent2,relationship,user,false);
            assertNotNull(relatedContent);
            assertEquals(1,relatedContent.size());

        }finally {
            if(parentContentType != null){
                contentTypeAPI.delete(parentContentType);
            }
            if(childContentType != null){
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    private ContentType createContentType(final String name) throws DotSecurityException, DotDataException {
        return contentTypeAPI.save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                .owner(user.getUserId()).build());
    }

    private Field createRelationshipField(final String relationshipName, final String parentTypeId,
                                               final String childTypeVar, String cardinality)
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