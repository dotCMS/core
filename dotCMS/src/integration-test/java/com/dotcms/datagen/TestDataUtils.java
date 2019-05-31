package com.dotcms.datagen;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.ConfigTestHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.templates.model.Template;
import com.google.common.io.Files;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author Jonathan Gamba 2019-04-16
 */
public class TestDataUtils {

    public static ContentType getBlogLikeContentType() {
        return getBlogLikeContentType("Blog" + System.currentTimeMillis());
    }

    public static ContentType getBlogLikeContentType(final String contentTypeName) {

        ContentType blogType = null;
        try {
            try {
                blogType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (blogType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                fields.add(
                        new FieldDataGen()
                                .name("title")
                                .velocityVarName("title")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("urlTitle")
                                .velocityVarName("urlTitle")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("author")
                                .velocityVarName("author")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("body")
                                .velocityVarName("body")
                                .searchable(true).indexed(true)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Publish")
                                .velocityVarName("sysPublishDate")
                                .defaultValue(null)
                                .type(DateField.class)
                                .next()
                );

                //Category field
                final Collection<Category> topLevelCategories = APILocator.getCategoryAPI()
                        .findTopLevelCategories(APILocator.systemUser(), false);
                final Optional<Category> anyTopLevelCategory = topLevelCategories.stream()
                        .findAny();

                anyTopLevelCategory.map(category -> new FieldDataGen()
                        .type(CategoryField.class)
                        .defaultValue(null)
                        .values(category.getInode())
                        .next()).ifPresent(fields::add);

                /*//Relationships field
                fields.add(
                        new FieldDataGen()
                                .name("Blog-Comments")
                                .velocityVarName("blogComments")
                                .defaultValue(null)
                                .type(RelationshipField.class)
                                .values(String
                                        .valueOf(RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()))
                                .relationType("Comments")
                                .next()
                );*/

                /*//Relationships field
                fields.add(
                        new FieldDataGen()
                                .name("Blog-Blog")
                                .velocityVarName("blogBlog")
                                .defaultValue(null)
                                .type(RelationshipField.class)
                                .values(String
                                        .valueOf(RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()))
                                .relationType(contentTypeName + StringPool.PERIOD + "blogBlog")
                                .next()
                );*/

                blogType = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return blogType;
    }

    public static ContentType getCommentsLikeContentType() {
        return getCommentsLikeContentType("Comments" + System.currentTimeMillis());
    }

    public static ContentType getCommentsLikeContentType(final String contentTypeName) {

        ContentType commentsType = null;
        try {
            try {
                commentsType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (commentsType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                fields.add(
                        new FieldDataGen()
                                .name("title")
                                .velocityVarName("title")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("email")
                                .velocityVarName("email")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("comment")
                                .velocityVarName("comment")
                                .next()
                );

                commentsType = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return commentsType;
    }

    public static ContentType getNewsLikeContentType() {
        return getNewsLikeContentType("News" + System.currentTimeMillis());
    }

    public static ContentType getNewsLikeContentType(final String contentTypeName) {
        ContentType newsType = null;
        try {
            try {
                newsType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (newsType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                fields.add(
                        new FieldDataGen()
                                .name("Title")
                                .velocityVarName("title")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("urlTitle")
                                .velocityVarName("urlTitle")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("By line")
                                .velocityVarName("byline")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Publish")
                                .velocityVarName("sysPublishDate")
                                .defaultValue(null)
                                .type(DateField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Story")
                                .velocityVarName("story")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Tags")
                                .velocityVarName("tags")
                                .defaultValue(null)
                                .type(TagField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Geolocation")
                                .velocityVarName("latlong")
                                .type(TextField.class)
                                .indexed(true)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Categories")
                                .velocityVarName("categories")
                                .type(CategoryField.class)
                                .next()
                );

                newsType = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return newsType;
    }

    public static ContentType getWikiLikeContentType() {
        return getWikiLikeContentType("Wiki" + System.currentTimeMillis());
    }

    public static ContentType getWikiLikeContentType(final String contentTypeName) {

        ContentType wikiType = null;
        try {
            try {
                wikiType = APILocator.getContentTypeAPI(APILocator.systemUser()).find("Wiki");
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (wikiType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                fields.add(
                        new FieldDataGen()
                                .name("Title")
                                .velocityVarName("title")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("urlTitle")
                                .velocityVarName("urlTitle")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("By line")
                                .velocityVarName("byline")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Publish")
                                .velocityVarName("sysPublishDate")
                                .defaultValue(null)
                                .type(DateField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Story")
                                .velocityVarName("story")
                                .next()
                );

                wikiType = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return wikiType;
    }

    public static ContentType getWidgetLikeContentType() {
        return getWidgetLikeContentType("SimpleWidget" + System.currentTimeMillis());
    }

    public static ContentType getWidgetLikeContentType(final String contentTypeName) {

        ContentType simpleWidgetContentType = null;
        try {
            try {
                simpleWidgetContentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (simpleWidgetContentType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                fields.add(
                        new FieldDataGen()
                                .name("Code")
                                .velocityVarName("code")
                                .required(true)
                                .next()
                );

                simpleWidgetContentType = new ContentTypeDataGen()
                        .baseContentType(BaseContentType.WIDGET)
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return simpleWidgetContentType;
    }

    public static ContentType getFormLikeContentType() {
        return getFormLikeContentType("Form" + System.currentTimeMillis());
    }

    public static ContentType getFormLikeContentType(final String contentTypeName) {

        ContentType formContentType = null;
        try {
            try {
                formContentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (formContentType == null) {

                formContentType = new ContentTypeDataGen()
                        .baseContentType(BaseContentType.FORM)
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return formContentType;
    }

    public static Contentlet getWikiContent(Boolean persist, long languageId,
            String contentTypeId) {

        if (null == contentTypeId) {
            contentTypeId = getWikiLikeContentType().id();
        }

        ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                .languageId(languageId)
                .setProperty("title", "wikiContent")
                .setProperty("urlTitle", "wikiContent")
                .setProperty("story", "story")
                .setProperty("sysPublishDate", new Date())
                .setProperty("byline", "byline");

        if (persist) {
            return contentletDataGen.nextPersisted();
        } else {
            return contentletDataGen.next();
        }
    }

    public static Contentlet getGenericContentContent(Boolean persist, long languageId) {

        try {
            ContentType webPageContentContentType = APILocator
                    .getContentTypeAPI(APILocator.systemUser())
                    .find("webPageContent");

            ContentletDataGen contentletDataGen = new ContentletDataGen(
                    webPageContentContentType.id())
                    .languageId(languageId)
                    .host(APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false))
                    .setProperty("title", "genericContent")
                    .setProperty("author", "systemUser")
                    .setProperty("body", "Generic Content Body");

            if (persist) {
                return contentletDataGen.nextPersisted();
            } else {
                return contentletDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static Contentlet getWidgetContent(Boolean persist, long languageId,
            String contentTypeId) {

        if (null == contentTypeId) {
            contentTypeId = getWidgetLikeContentType().id();
        }

        try {
            ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                    .languageId(languageId)
                    .host(APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false))
                    .setProperty("widgetTitle", "titleContent")
                    .setProperty("code", "Widget code");

            if (persist) {
                return contentletDataGen.nextPersisted();
            } else {
                return contentletDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static Contentlet getFormContent(Boolean persist, long languageId,
            String contentTypeId) {

        if (null == contentTypeId) {
            contentTypeId = getFormLikeContentType().id();
        }

        try {
            ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                    .languageId(languageId)
                    .host(APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false))
                    .setProperty("formTitle", "title" + System.currentTimeMillis())
                    .setProperty("formEmail", "email@" + System.currentTimeMillis() + ".com")
                    .setProperty("formHost", APILocator.getHostAPI()
                            .findDefaultHost(APILocator.systemUser(), false));

            if (persist) {
                return contentletDataGen.nextPersisted();
            } else {
                return contentletDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static Relationship relateContentTypes(final ContentType parentContentType,
            final ContentType childContentType) {
        final String relationTypeValue = parentContentType.name() + "-" + childContentType.name();
        RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        Relationship relationship;
        relationship = relationshipAPI.byTypeValue(relationTypeValue);
        if (null != relationship) {
            return relationship;
        } else {
            relationship = new Relationship();
            if ((parentContentType == childContentType) || (parentContentType.id().equals(childContentType.id()))) {
                relationship.setParentRelationName("Child " + parentContentType.name());
                relationship.setChildRelationName("Parent " + childContentType.name());
            } else {
                relationship.setParentRelationName(parentContentType.name());
                relationship.setChildRelationName(childContentType.name());
            }
            relationship.setRelationTypeValue(relationTypeValue);
            relationship.setParentStructureInode(parentContentType.inode());
            relationship.setChildStructureInode(childContentType.id());
            try {
                APILocator.getRelationshipAPI().create(relationship);
            } catch (Exception e) {
                throw new DotRuntimeException(e);
            }
        }
        return relationship;
    }

    public static Contentlet getFileAssetContent(Boolean persist, long languageId) {

        try {
            Folder folder = new FolderDataGen().nextPersisted();

            //Test file
            final String testImagePath = "com/dotmarketing/portlets/contentlet/business/test_files/test_image1.jpg";
            final File originalTestImage = new File(
                    ConfigTestHelper.getUrlToTestResource(testImagePath).toURI());
            final File testImage = new File(Files.createTempDir(),
                    "test_image1" + System.currentTimeMillis() + ".jpg");
            FileUtil.copyFile(originalTestImage, testImage);

            ContentletDataGen fileAssetDataGen = new FileAssetDataGen(folder, testImage)
                    .languageId(languageId);

            if (persist) {
                return ContentletDataGen.publish(fileAssetDataGen.nextPersisted());
            } else {
                return fileAssetDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static Contentlet getBlogContent(Boolean persist, long languageId,
            String contentTypeId) {

        if (null == contentTypeId) {
            contentTypeId = getBlogLikeContentType().id();
        }

        ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                .languageId(languageId)
                .setProperty("title", "blogContent")
                .setProperty("urlTitle", "blogContent")
                .setProperty("author", "systemUser")
                .setProperty("sysPublishDate", new Date())
                .setProperty("body", "blogBody");

        if (persist) {
            return contentletDataGen.nextPersisted();
        } else {
            return contentletDataGen.next();
        }
    }

    public static Contentlet getNewsContent(Boolean persist, long languageId,
            String contentTypeId) {

        if (null == contentTypeId) {
            contentTypeId = getNewsLikeContentType().id();
        }

        try {
            ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                    .languageId(languageId)
                    .host(APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false))
                    .setProperty("title", "newsContent")
                    .setProperty("urlTitle", "newsContent")
                    .setProperty("byline", "byline")
                    .setProperty("sysPublishDate", new Date())
                    .setProperty("story", "newsStory")
                    .setProperty("tags", "test");

            if (persist) {
                return contentletDataGen.nextPersisted();
            } else {
                return contentletDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static Contentlet getPageContent(Boolean persist, long languageId) {

        try {

            final Host defaultHost = APILocator.getHostAPI()
                    .findDefaultHost(APILocator.systemUser(), false);
            final User systemUser = APILocator.systemUser();

            //Create a container for the given contentlet
            Container container = new ContainerDataGen()
                    .nextPersisted();

            //Create a template
            Template template = new TemplateDataGen().withContainer(container.getIdentifier())
                    .nextPersisted();

            //Create the html page
            Folder testFolder = APILocator.getFolderAPI()
                    .createFolders("/folder" + System.currentTimeMillis() + "/",
                            defaultHost, systemUser, false);
            ContentletDataGen contentletDataGen = new HTMLPageDataGen(testFolder, template)
                    .languageId(languageId);

            if (persist) {
                return contentletDataGen.nextPersisted();
            } else {
                return contentletDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static Language getSpanishLanguage() {

        //Search for the Spanish language, if does not exist we need to create it
        Language spanishLanguage = APILocator.getLanguageAPI().getLanguage("es", "ES");
        if (null == spanishLanguage || spanishLanguage.getId() < 1) {
            spanishLanguage = new LanguageDataGen()
                    .country("Spain")
                    .countryCode("ES")
                    .languageCode("es")
                    .languageName("Spanish").nextPersisted();
        }

        return spanishLanguage;
    }

}