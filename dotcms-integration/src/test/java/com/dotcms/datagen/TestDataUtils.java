package com.dotcms.datagen;

import static org.hamcrest.Matchers.equalTo;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.HiddenField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.LineDividerField;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.PersonaContentType;
import com.dotcms.util.ConfigTestHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.common.reindex.ReindexQueueAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.image.focalpoint.FocalPointAPITest;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Assert;

/**
 * @author Jonathan Gamba 2019-04-16
 */
public class TestDataUtils {

    public static final String FILE_ASSET_1 = "fileAsset1";
    public static final String FILE_ASSET_2 = "fileAsset2";
    public static final String FILE_ASSET_3 = "fileAsset3";

    /**
     * This is a simple example of what the content of the Block Editor field with a simple
     * "Generic Content Body" message looks like.
     */
    public static final String BLOCK_EDITOR_DUMMY_CONTENT =
            "{" +
                    "\"type\":\"doc\"," +
                    "\"attrs\":" +
                        "{" +
                            "\"chartCount\":20," +
                            "\"wordCount\":3," +
                            "\"readingTime\":1" +
                        "}," +
                    "\"content\":" +
                        "[{" +
                            "\"type\":\"paragraph\"," +
                            "\"attrs\":" +
                                "{" +
                                    "\"textAlign\":\"left\"" +
                                "}," +
                            "\"content\":" +
                                "[{" +
                                    "\"type\":\"text\"," +
                                    "\"text\":\"Generic Content Body\"" +
                                "}]" +
                        "}]" +
            "}";

    /**
     * This is a simple example of what the content of the Block Editor field looks like, but it
     * allows you to add your own custom message via {@link String#format(String, Object...)}.
     */
    public static final String BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT =
            "{" +
                    "\"type\":\"doc\"," +
                    "\"attrs\":" +
                        "{" +
                            "\"chartCount\":20," +
                            "\"wordCount\":3," +
                            "\"readingTime\":1" +
                        "}," +
                    "\"content\":" +
                        "[{" +
                            "\"type\":\"paragraph\"," +
                            "\"attrs\":" +
                                "{" +
                                "\"textAlign\":\"left\"" +
                                "}," +
                            "\"content\":" +
                                "[{" +
                                    "\"type\":\"text\"," +
                                    "\"text\":\"%s\"" +
                                "}]" +
                        "}]" +
            "}";

    /**
     * This is a simple example of what the content of the Block Editor field with accented
     * characters such as "Numéro de téléphone" looks like.
     */
    public static final String BLOCK_EDITOR_DUMMY_CONTENT_UNICODE_CHARS =
            "{" +
                    "\"type\":\"doc\"," +
                    "\"attrs\":" +
                        "{" +
                            "\"chartCount\":20," +
                            "\"wordCount\":3," +
                            "\"readingTime\":1" +
                        "}," +
                    "\"content\":" +
                        "[{" +
                            "\"type\":\"paragraph\"," +
                            "\"attrs\":" +
                                "{" +
                                "\"textAlign\":\"left\"" +
                                "}," +
                            "\"content\":" +
                                "[{" +
                                    "\"type\":\"text\"," +
                                    "\"text\":\"Numéro de téléphone\"" +
                                "}]" +
                        "}]" +
            "}";

    public static ContentType getBlogLikeContentType() {
        return getBlogLikeContentType("Blog" + System.currentTimeMillis());
    }

    public static ContentType getBlogLikeContentType(final String contentTypeName) {
        return getBlogLikeContentType(contentTypeName, null, null);
    }

    public static ContentType getBlogLikeContentType(final String contentTypeName,
            final Host site) {
        return getBlogLikeContentType(contentTypeName, site, null);
    }

    public static ContentType getBlogLikeContentType(final Host site) {
        return getBlogLikeContentType("Blog" + System.currentTimeMillis(), site);
    }

    @WrapInTransaction
    public static ContentType getBlogLikeContentType(final String contentTypeName,
            final Host site, final Set<String> workflowIds) {

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

                if (null != site) {
                    fields.add(
                            new FieldDataGen()
                                    .name("Site or Folder")
                                    .velocityVarName("hostfolder")
                                    .required(Boolean.TRUE)
                                    .type(HostFolderField.class)
                                    .next()
                    );
                }
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
                                .searchable(true)
                                .indexed(true)
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
                                .name("postingDate")
                                .velocityVarName("publishDate")
                                .defaultValue(null)
                                .type(DateField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("expireDate")
                                .velocityVarName("expireDate")
                                .defaultValue(null)
                                .type(DateField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("seo")
                                .velocityVarName("seo")
                                .defaultValue(null)
                                .dataType(DataTypes.LONG_TEXT)
                                .searchable(true)
                                .indexed(true)
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
                        .workflowId(workflowIds)
                        .publishDateFieldVarName("publishDate")
                        .expireDateFieldVarName("expireDate")
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
        return getCommentsLikeContentType(contentTypeName, null);
    }

    @WrapInTransaction
    public static ContentType getCommentsLikeContentType(final String contentTypeName,
            final Set<String> workflowIds) {

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
                        .workflowId(workflowIds)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return commentsType;
    }

    public static ContentType getEmployeeLikeContentType() {
        return getEmployeeLikeContentType("Employee" + System.currentTimeMillis());
    }

    public static ContentType getEmployeeLikeContentType(final String contentTypeName) {
        return getEmployeeLikeContentType(contentTypeName, null, null);
    }

    public static ContentType getEmployeeLikeContentType(final String contentTypeName,
            final Host site) {
        return getEmployeeLikeContentType(contentTypeName, site, null);
    }

    @WrapInTransaction
    public static ContentType getEmployeeLikeContentType(final String contentTypeName,
            final Host site, final Set<String> workflowIds) {

        ContentType employeeType = null;
        try {
            try {
                employeeType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (employeeType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();

                if (null != site) {
                    fields.add(
                            new FieldDataGen()
                                    .name("Site or Folder")
                                    .velocityVarName("hostfolder")
                                    .required(Boolean.TRUE)
                                    .type(HostFolderField.class)
                                    .next()
                    );
                }
                fields.add(
                        new FieldDataGen()
                                .name("First Name")
                                .velocityVarName("firstName")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Last Name")
                                .velocityVarName("lastName")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Title")
                                .velocityVarName("jobTitle")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Phone")
                                .velocityVarName("phone")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Mobile")
                                .velocityVarName("mobile")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Fax")
                                .velocityVarName("fax")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Email")
                                .velocityVarName("email")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Photo")
                                .velocityVarName("photo")
                                .type(BinaryField.class)
                                .next()
                );

                final Host siteToUse = site != null ? site :
                        APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);

                employeeType = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .workflowId(workflowIds)
                        .host(siteToUse)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return employeeType;
    }

    public static ContentType getNewsLikeContentType() {
        return getNewsLikeContentType("News" + System.currentTimeMillis(), null, null, null, null,
                null);
    }

    public static ContentType getNewsLikeContentType(final String contentTypeName) {
        return getNewsLikeContentType(contentTypeName, null, null, null, null, null);
    }

    public static ContentType getNewsLikeContentType(final String contentTypeName,
            final String parentCategoryInode) {
        return getNewsLikeContentType(contentTypeName, null, null, null, null, parentCategoryInode);
    }

    public static ContentType getNewsLikeContentType(final String contentTypeName,
            final Host site) {
        return getNewsLikeContentType(contentTypeName, site, null, null, null, null);
    }

    public static ContentType getNewsLikeContentType(final Host site) {
        return getNewsLikeContentType("News" + System.currentTimeMillis(), site, null, null, null,
                null);
    }

    public static ContentType getNewsLikeContentType(final String contentTypeName,
            final Host site,
            final String detailPageIdentifier,
            final String urlMapPattern) {
        return getNewsLikeContentType(contentTypeName, site, detailPageIdentifier, urlMapPattern,
                null, null);
    }

    @WrapInTransaction
    public static ContentType getNewsLikeContentType(final String contentTypeName,
            final Host site,
            final String detailPageIdentifier,
            final String urlMapPattern,
            final Set<String> workflowIds,
            String parentCategoryInode) {

        final String publishDateFieldName = "sysPublishDate";
        final String expireDateFieldName = "sysExpireDate";

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
                if (null != site) {
                    fields.add(
                            new FieldDataGen()
                                    .name("Site or Folder")
                                    .velocityVarName("hostfolder")
                                    .required(Boolean.TRUE)
                                    .type(HostFolderField.class)
                                    .next()
                    );
                }
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
                                .name(publishDateFieldName)
                                .velocityVarName("sysPublishDate")
                                .defaultValue(null)
                                .type(DateField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name(expireDateFieldName)
                                .velocityVarName("sysExpireDate")
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
                if (!UtilMethods.isSet(parentCategoryInode)) {
                    parentCategoryInode = new CategoryDataGen().nextPersisted().getInode();
                }
                fields.add(
                        new FieldDataGen()
                                .name("Categories")
                                .velocityVarName("categories")
                                .type(CategoryField.class)
                                .defaultValue(null)
                                .values(parentCategoryInode)
                                .next()
                );

                ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .workflowId(workflowIds)
                        .expireDateFieldVarName(expireDateFieldName)
                        .publishDateFieldVarName(publishDateFieldName)
                        .fields(fields);

                if (null != site) {
                    contentTypeDataGen.host(site);
                }

                if (null != detailPageIdentifier) {
                    contentTypeDataGen.detailPage(detailPageIdentifier);
                }

                if (null != urlMapPattern) {
                    contentTypeDataGen.urlMapPattern(urlMapPattern);
                }

                newsType = contentTypeDataGen.nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return newsType;
    }

    @WrapInTransaction
    public static Contentlet getDotAssetLikeContentlet() {
        return getDotAssetLikeContentlet(APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                Try.of(() -> APILocator.getHostAPI().findSystemHost()).getOrNull());

    }

    @WrapInTransaction
    public static Contentlet getDotAssetLikeContentlet(final Treeable hostOrFolder) {
        return getDotAssetLikeContentlet(APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                hostOrFolder);

    }

    @WrapInTransaction
    public static Contentlet getDotAssetLikeContentlet(final boolean perist, final long languageId) {
        Contentlet dotAssetContentlet = getDotAssetLikeContentlet(languageId, new FolderDataGen().nextPersisted());
        return perist ? ContentletDataGen.publish(dotAssetContentlet) : dotAssetContentlet;
    }

    @WrapInTransaction
    public static Contentlet getDotAssetLikeContentlet(long languageId,
            final Treeable hostOrFolder) {

        Host host = null;
        Folder folder = null;

        host = Try.of(() -> APILocator.getHostAPI()
                .find(hostOrFolder.getIdentifier(), APILocator.systemUser(), false)).getOrNull();
        if (host == null) {
            folder = Try.of(() -> APILocator.getFolderAPI()
                    .find(hostOrFolder.getInode(), APILocator.systemUser(), false)).getOrNull();
            if (folder == null) {
                folder = Try.of(() -> APILocator.getFolderAPI()
                        .find(hostOrFolder.getInode(), APILocator.systemUser(), false)).getOrNull();
            }
            if (folder != null) {
                final Folder finalFolder = folder;
                host = Try.of(() -> APILocator.getHostAPI()
                        .find(finalFolder.getHostId(), APILocator.systemUser(), false)).getOrNull();
            }
        }

        if (host == null) {
            host = Try.of(() -> APILocator.getHostAPI().findSystemHost()).getOrNull();
        }

        ContentType dotAssetType = getDotAssetLikeContentType("dotAsset" + UUIDGenerator.shorty(),
                host);

        URL url = FocalPointAPITest.class.getResource("/images/test.jpg");

        File testImage = new File(url.getFile());

        ContentletDataGen contentletDataGen = new ContentletDataGen(dotAssetType.id())
                .languageId(languageId)
                .setProperty("asset", testImage)
                .setProperty("hostFolder",
                        folder != null ? folder.getInode() : host.getIdentifier())
                .setProperty("tags", "tag1, tag2");

        if (folder != null) {
            contentletDataGen.folder(folder);
        }
        if (host != null) {
            contentletDataGen.host(host);
        }

        return contentletDataGen.nextPersisted();


    }


    @WrapInTransaction
    public static ContentType getDotAssetLikeContentType(final String contentTypeVar,
            final Host host) {

        ContentType dotAssetType = Try.of(
                () -> APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeVar)).getOrNull();

        if (dotAssetType == null) {
            ContentTypeDataGen dataGen = new ContentTypeDataGen()
                    .name(contentTypeVar)
                    .host(host)
                    .velocityVarName(contentTypeVar)
                    .baseContentType(BaseContentType.DOTASSET);

            if (host != null) {
                dataGen.host(host);
            }
            dotAssetType = dataGen.nextPersisted();
        }

        return dotAssetType;
    }

    public static ContentType getRichTextLikeContentType() {
        return getRichTextLikeContentType("RichText" + System.currentTimeMillis(), null);
    }

    @WrapInTransaction
    public static ContentType getRichTextLikeContentType(final String contentTypeName,
            final Set<String> workflowIds) {

        ContentType richTextLike = null;
        try {
            try {
                richTextLike = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (richTextLike == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                fields.add(
                        new FieldDataGen()
                                .name("Title")
                                .velocityVarName("title")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Site")
                                .velocityVarName("contentHost")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Body")
                                .velocityVarName("body")
                                .next()
                );

                richTextLike = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .workflowId(workflowIds)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return richTextLike;
    }

    /**
     * Creates a simple Content Type similar to the Rich Text type with the following fields:
     * <ol>
     *     <li>Site or Folder.</li>
     *     <li>Title, of type "text".</li>
     *     <li>Description, of type "WYSIWYG".</li>
     * </ol>
     *
     * @param contentTypeName The name of the test Content Type.
     * @param site            The Site where this Content Type will live in.
     * @param workflowIds     The workflows assigned to this type. This can be null.
     * @return The new test Content Type.
     */
    @WrapInTransaction
    public static ContentType getWysiwygLikeContentType(final String contentTypeName,
            final Host site,
            final Set<String> workflowIds) {
        ContentType wysiwygType = Try.of(() -> APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(contentTypeName)).getOrNull();
        try {
            if (wysiwygType == null) {
                final List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                if (null != site) {
                    fields.add(new FieldDataGen()
                            .sortOrder(1)
                            .name("Site or Folder")
                            .velocityVarName("hostfolder")
                            .required(Boolean.TRUE)
                            .type(HostFolderField.class).next());
                }
                fields.add(new FieldDataGen()
                        .sortOrder(2)
                        .name("Title")
                        .velocityVarName("title").next());
                fields.add(new FieldDataGen()
                        .sortOrder(3)
                        .name("Description")
                        .velocityVarName("description")
                        .type(WysiwygField.class).next());
                final ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .workflowId(workflowIds)
                        .fields(fields);
                if (null != site) {
                    contentTypeDataGen.host(site);
                }
                wysiwygType = contentTypeDataGen.nextPersisted();
            }
        } catch (final Exception e) {
            throw new DotRuntimeException(
                    String.format("Error creating test type '%s'", contentTypeName), e);
        }
        return wysiwygType;
    }

    public static ContentType getWikiLikeContentType() {
        return getWikiLikeContentType("Wiki" + System.currentTimeMillis(), null);
    }

    @WrapInTransaction
    public static ContentType getWikiLikeContentType(final String contentTypeName,
            final Set<String> workflowIds) {

        ContentType wikiType = null;
        try {
            try {
                wikiType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
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
                        .workflowId(workflowIds)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return wikiType;
    }

    public static ContentType getWidgetLikeContentType() {
        return getWidgetLikeContentType("SimpleWidget" + System.currentTimeMillis(), null, null);
    }

    public static ContentType getWidgetLikeContentType(final Supplier<String> widgetCode) {
        return getWidgetLikeContentType("SimpleWidget" + System.currentTimeMillis(), null, widgetCode);
    }

    @WrapInTransaction
    public static ContentType getWidgetLikeContentType(final String contentTypeName,
            final Set<String> workflowIds, final Supplier<String> widgetCode) {

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
                if(null != widgetCode){ //If the widgetCode is not null, then add the field
                    fields.add(
                            new FieldDataGen()
                                    .name("WidgetCode")
                                    .velocityVarName("widgetCode")
                                    .type(ConstantField.class)
                                    .required(false)
                                    .values(widgetCode.get())
                                    .next()
                    );
                }

                simpleWidgetContentType = new ContentTypeDataGen()
                        .baseContentType(BaseContentType.WIDGET)
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .workflowId(workflowIds)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return simpleWidgetContentType;
    }

    public static ContentType getFormLikeContentType() {
        return getFormLikeContentType("Form" + System.currentTimeMillis(), null);
    }

    @WrapInTransaction
    public static ContentType getFormLikeContentType(final String contentTypeName,
            final Set<String> workflowIds) {

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
                        .workflowId(workflowIds)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return formContentType;
    }

    public static ContentType getFormWithRequiredFieldsLikeContentType() {
        return getFormWithRequiredFieldsLikeContentType("Form" + System.currentTimeMillis(), null);
    }

    @WrapInTransaction
    public static ContentType getFormWithRequiredFieldsLikeContentType(final String contentTypeName,
            Set<String> workFlowsId) {

        ContentType formContentType = null;
        try {
            try {
                formContentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (formContentType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                fields.add(
                        new FieldDataGen()
                                .name("formId")
                                .velocityVarName("formId")
                                .type(TextField.class)
                                .required(true)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("widgetTitle")
                                .velocityVarName("widgetTitle")
                                .type(TextField.class)
                                .required(true)
                                .next()
                );

                formContentType = new ContentTypeDataGen()
                        .baseContentType(BaseContentType.FORM)
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .workflowId(workFlowsId)
                        .fields(fields)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return formContentType;
    }

    public static Contentlet getEmptyFormWithRequiredFieldsContent(long languageId) {

        String contentTypeId = getFormWithRequiredFieldsLikeContentType().id();

        try {
            return new ContentletDataGen(contentTypeId)
                    .languageId(languageId)
                    .setProperty("formId", null)
                    .setProperty("widgetTitle", null).skipValidation(true).nextPersisted();
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static Contentlet getRichTextContent(Boolean persist, long languageId, String body) {
        String contentTypeId = getRichTextLikeContentType().id();
        ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                .languageId(languageId)
                .setProperty("title", "Rich Text Title")
                .setProperty("contentHost", "Some content host")
                .setProperty("body", body);

        if (persist) {
            return contentletDataGen.nextPersisted();
        } else {
            return contentletDataGen.next();
        }
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
        return getGenericContentContent(persist, languageId, null);
    }

    public static Contentlet getGenericContentContent(final Boolean persist, final long languageId, Host site) {

        try {

            if (null == site) {
                site = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
            }

            final ContentType webPageContentContentType = APILocator
                    .getContentTypeAPI(APILocator.systemUser())
                    .find("webPageContent");
            final ContentletDataGen contentletDataGen = new ContentletDataGen(
                    webPageContentContentType.id())
                    .languageId(languageId)
                    .host(site)
                    .setProperty("contentHost", site)
                    .setProperty("title", "genericContent")
                    .setProperty("author", "systemUser")
                    .setProperty("body", BLOCK_EDITOR_DUMMY_CONTENT);
            return persist ? contentletDataGen.nextPersisted() : contentletDataGen.next();
        } catch (final Exception e) {
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
            if ((parentContentType == childContentType) || (parentContentType.id()
                    .equals(childContentType.id()))) {
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
        return getFileAssetContent(persist, languageId, TestFile.JPG);
    }

    public static Contentlet getFileAssetSVGContent(Boolean persist, long languageId) {
        return getFileAssetContent(persist, languageId, TestFile.SVG);
    }

    public static Contentlet getFileAssetContent(final Boolean persist, final long languageId,
            final TestFile testFile) {

        try {
            final Folder folder = new FolderDataGen().nextPersisted();

            //Test file
            final String testFilePath = testFile.filePath;
            return createFileAsset(testFilePath, folder, languageId, persist);
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public enum TestFile {

        JPG("com/dotmarketing/portlets/contentlet/business/test_files/test_image1.jpg"),
        GIF("com/dotmarketing/portlets/contentlet/business/test_files/test.gif"),
        PNG("com/dotmarketing/portlets/contentlet/business/test_files/test_image2.png"),
        SVG("com/dotmarketing/portlets/contentlet/business/test_files/test_image.svg"),
        TEXT("com/dotmarketing/portlets/contentlet/business/test_files/test.txt"),
        PDF("com/dotmarketing/portlets/contentlet/business/test_files/test.pdf");

        private final String filePath;

        TestFile(final String filePath) {
            this.filePath = filePath;
        }

        public String getFilePath() {
            return filePath;
        }
    }

    private static Contentlet createFileAsset(final String testImagePath, final Folder folder,
            final long languageId, final boolean persist) throws Exception {
        //Test file
        final String extension = UtilMethods.getFileExtension(testImagePath);
        final File originalTestImage = new File(
                ConfigTestHelper.getUrlToTestResource(testImagePath).toURI());
        final File testImage = new File(Files.createTempDir(),
                "test_image1" + System.currentTimeMillis() + "." + extension);
        FileUtil.copyFile(originalTestImage, testImage);

        ContentletDataGen fileAssetDataGen = new FileAssetDataGen(folder, testImage)
                .languageId(languageId);

        if (persist) {
            return ContentletDataGen.publish(fileAssetDataGen.nextPersisted());
        } else {
            return fileAssetDataGen.next();
        }
    }

    public static Contentlet getBlogContent(Boolean persist, long languageId) {
        return getBlogContent(persist, languageId, null, null);
    }

    public static Contentlet getBlogContent(Boolean persist, long languageId,
            String contentTypeId) {
        return getBlogContent(persist, languageId, contentTypeId, null);
    }

    public static Contentlet withEmbeddings(final boolean persist,
                                            final Host host,
                                            final long languageId,
                                            final String contentTypeId,
                                            final String text) {

        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                .languageId(languageId)
                .setProperty("title", "blogContent")
                .setProperty("urlTitle", "blogContent")
                .setProperty("author", "systemUser")
                .setProperty("sysPublishDate", new Date())
                .setProperty("body", BLOCK_EDITOR_DUMMY_CONTENT)
                .setProperty("seo", text);

        return getBlogContent(persist, host, contentletDataGen);
    }

    public static Contentlet getBlogContent(Boolean persist, long languageId,
            String contentTypeId, final Host site) {

        if (null == contentTypeId) {
            contentTypeId = getBlogLikeContentType().id();
        }

        ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                .languageId(languageId)
                .setProperty("title", "blogContent")
                .setProperty("urlTitle", "blogContent")
                .setProperty("author", "systemUser")
                .setProperty("sysPublishDate", new Date())
                .setProperty("body", BLOCK_EDITOR_DUMMY_CONTENT);

        return getBlogContent(persist, site, contentletDataGen);
    }

    public static Contentlet getBlogContent(final boolean persist,
                                            final Host site,
                                            ContentletDataGen contentletDataGen) {

        if (null != site) {
            contentletDataGen = contentletDataGen.host(site)
                    .setProperty("hostfolder", site);
        }

        if (persist) {
            return contentletDataGen.nextPersisted();
        } else {
            return contentletDataGen.next();
        }
    }

    public static Contentlet getEmployeeContent(Boolean persist, long languageId,
            String contentTypeId) {
        return getEmployeeContent(persist, languageId,
                contentTypeId, null);
    }

    public static Contentlet getEmployeeContent(Boolean persist, long languageId,
            String contentTypeId, final Host site) {

        if (null == contentTypeId) {
            contentTypeId = getEmployeeLikeContentType(
                    "Employee" + System.currentTimeMillis(), site).id();
        }

        try {
            final long millis = System.currentTimeMillis();

            //Photo
            final String testImagePath = "com/dotmarketing/portlets/contentlet/business/test_files/test_image1.jpg";
            final File originalTestImage = new File(
                    ConfigTestHelper.getUrlToTestResource(testImagePath).toURI());
            final File testPhoto = new File(Files.createTempDir(),
                    "photo" + System.currentTimeMillis() + ".jpg");
            FileUtil.copyFile(originalTestImage, testPhoto);

            //Creating the content
            ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                    .languageId(languageId)
                    .setProperty("firstName", "Test Name" + millis)
                    .setProperty("lastName", "Test Last name" + millis)
                    .setProperty("phone", "99999999")
                    .setProperty("mobile", "99999999")
                    .setProperty("fax", "99999999")
                    .setProperty("email", "test@test" + millis + ".com")
                    .setProperty("photo", testPhoto);

            if (null != site) {
                contentletDataGen = contentletDataGen.host(site)
                        .setProperty("hostfolder", site);
            }

            if (persist) {
                return contentletDataGen.nextPersisted();
            } else {
                return contentletDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static Contentlet getNewsContent(Boolean persist, long languageId,
            String contentTypeId) {
        return getNewsContent(persist, languageId, contentTypeId, null);
    }

    public static Contentlet getNewsContent(Boolean persist, long languageId,
            String contentTypeId, Host site) {
        return getNewsContent(persist, languageId, contentTypeId, site, new Date());
    }

    public static Contentlet getNewsContent(Boolean persist, long languageId,
            String contentTypeId, Host site, Date sysPublishDate) {
        return getNewsContent(persist, languageId, contentTypeId, site, sysPublishDate, null);
    }

    public static Contentlet getNewsContent(Boolean persist, long languageId,
            String contentTypeId, Host site, Date sysPublishDate, String urlTitle) {

        final long millis = System.currentTimeMillis();

        if (null == contentTypeId) {
            contentTypeId = getNewsLikeContentType().id();
        }

        if (null == urlTitle) {
            urlTitle = "news-content-url-title" + millis;
        }

        try {

            ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                    .languageId(languageId)
                    .host(null != site ? site : APILocator.getHostAPI()
                            .findDefaultHost(APILocator.systemUser(), false))
                    .setProperty("title", "newsContent Title" + millis)
                    .setProperty("urlTitle", urlTitle)
                    .setProperty("byline", "byline")
                    .setProperty("sysPublishDate", sysPublishDate)
                    .setProperty("story", "newsStory")
                    .setProperty("tags", "test");

            if (null != site) {
                contentletDataGen = contentletDataGen.host(site)
                        .setProperty("hostfolder", site);
            }

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

        return getPageContent(persist, languageId, null);
    }

    public static Contentlet getPageContent(Boolean persist, long languageId, Folder folder) {

        try {
            //Create a container for the given contentlet
            Container container = new ContainerDataGen()
                    .nextPersisted();

            //Create a template
            Template template = new TemplateDataGen().withContainer(container.getIdentifier())
                    .nextPersisted();

            if (null == folder) {
                final Host defaultHost = APILocator.getHostAPI()
                        .findDefaultHost(APILocator.systemUser(), false);
                final User systemUser = APILocator.systemUser();

                //Create the html page
                folder = APILocator.getFolderAPI()
                        .createFolders("/folder" + System.currentTimeMillis() + "/",
                                defaultHost, systemUser, false);
            }
            ContentletDataGen contentletDataGen = new HTMLPageDataGen(folder, template)
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

    public static ContentType getDocumentLikeContentType() {
        return getDocumentLikeContentType("Document" + System.currentTimeMillis(), null);
    }

    @WrapInTransaction
    public static ContentType getDocumentLikeContentType(final String contentTypeName,
            Set<String> workflowIds) {

        ContentType simpleWidgetContentType = null;
        try {
            try {
                simpleWidgetContentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (simpleWidgetContentType == null) {

                final WorkflowScheme documentWorkflow = TestWorkflowUtils.getDocumentWorkflow();
                final Set<String> collectedWorkflowIds = new HashSet<>();
                collectedWorkflowIds.add(documentWorkflow.getId());
                if (null != workflowIds) {
                    collectedWorkflowIds.addAll(workflowIds);
                }

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                fields.add(
                        new FieldDataGen()
                                .name("hostFolder")
                                .velocityVarName("hostFolder")
                                .type(HostFolderField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Title")
                                .velocityVarName("title")
                                .type(TextField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("fileAsset")
                                .velocityVarName("fileAsset")
                                .type(BinaryField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("fileName")
                                .velocityVarName("fileName")
                                .type(TextField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("description")
                                .velocityVarName("description1")
                                .type(TextAreaField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("PublishDate")
                                .velocityVarName("sysPublishDate")
                                .defaultValue(null)
                                .type(DateField.class)
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
                                .name("Topic")
                                .velocityVarName("topic")
                                .type(CategoryField.class)
                                .next()
                );

                simpleWidgetContentType = new ContentTypeDataGen()
                        .baseContentType(BaseContentType.CONTENT)
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .workflowId(collectedWorkflowIds)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return simpleWidgetContentType;
    }


    public static Contentlet getDocumentLikeContent(Boolean persist, long languageId,
            String contentTypeId) {

        if (null == contentTypeId) {
            contentTypeId = getDocumentLikeContentType().id();
        }

        try {
            final String testImagePath = "com/dotmarketing/portlets/contentlet/business/test_files/test_image1.jpg";
            final File originalTestImage = new File(
                    ConfigTestHelper.getUrlToTestResource(testImagePath).toURI());
            final File testImage = new File(Files.createTempDir(),
                    "test_image1" + System.currentTimeMillis() + ".jpg");
            FileUtil.copyFile(originalTestImage, testImage);

            ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                    .languageId(languageId)
                    .setProperty("title", "document")
                    .setProperty("urlTitle", "blogContent")
                    .setProperty("sysPublishDate", new Date())
                    .setProperty("tags", "test")
                    .setProperty("fileAsset", testImage)
                    .setProperty("topic", "lol");

            if (persist) {
                return contentletDataGen.nextPersisted();
            } else {
                return contentletDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static ContentType getBannerLikeContentType() {
        return getBannerLikeContentType("Banner" + System.currentTimeMillis(),
                APILocator.systemHost(), null);
    }


    public static ContentType getBannerLikeContentType(final String contentTypeName,
            final Host site) {
        return getBannerLikeContentType(contentTypeName, site, null);
    }

    @WrapInTransaction
    public static ContentType getBannerLikeContentType(final String contentTypeName,
            final Host site,
            final Set<String> workflowIds) {

        ContentType bannerType = null;
        try {
            try {
                bannerType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (bannerType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                if (null != site) {
                    fields.add(
                            new FieldDataGen()
                                    .name("Site or Folder")
                                    .velocityVarName("hostfolder")
                                    .required(Boolean.TRUE)
                                    .type(HostFolderField.class)
                                    .next()
                    );
                }
                fields.add(
                        new FieldDataGen()
                                .name("Title")
                                .velocityVarName("title")
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
                                .name("Image")
                                .velocityVarName("image")
                                .type(BinaryField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Caption")
                                .velocityVarName("caption")
                                .type(WysiwygField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("TextColor")
                                .velocityVarName("textColor")
                                .type(SelectField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Layout")
                                .velocityVarName("layout")
                                .type(CustomField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("BackgroundColor")
                                .velocityVarName("BackgroundColor")
                                .type(CustomField.class)
                                .next()
                );

                ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .workflowId(workflowIds)
                        .fields(fields);

                if (null != site) {
                    contentTypeDataGen.host(site);
                }

                bannerType = contentTypeDataGen.nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return bannerType;
    }

    public static Contentlet getBannerLikeContent(Boolean persist, long languageId) {
        return getBannerLikeContent(persist, languageId, null, null);
    }

    public static Contentlet getBannerLikeContent(Boolean persist, long languageId,
            String contentTypeId, Host site) {

        if (null == contentTypeId) {
            contentTypeId = getBannerLikeContentType().id();
        }
        try {

            //Photo
            final String testImagePath = "com/dotmarketing/portlets/contentlet/business/test_files/test_image1.jpg";
            final File originalTestImage = new File(
                    ConfigTestHelper.getUrlToTestResource(testImagePath).toURI());
            final File testImage = new File(Files.createTempDir(),
                    "image" + System.currentTimeMillis() + ".jpg");
            FileUtil.copyFile(originalTestImage, testImage);


            final long millis = System.currentTimeMillis();
            ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                    .languageId(languageId)
                    .host(null != site ? site : APILocator.getHostAPI()
                            .findDefaultHost(APILocator.systemUser(), false))
                    .setProperty("title", "banner like Title" + millis)
                    .setProperty("sysPublishDate", new Date())
                    .setProperty("layout", "")
                    .setProperty("textColor", "")
                    .setProperty("image", testImage)
                    .setProperty("tags", "test");

            if (null != site) {
                contentletDataGen = contentletDataGen.host(site)
                        .setProperty("hostfolder", site);
            }

            if (persist) {
                return contentletDataGen.nextPersisted();
            } else {
                return contentletDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static ContentType getProductLikeContentType() {
        return getProductLikeContentType("Product" + System.currentTimeMillis(),
                APILocator.systemHost(), null);
    }

    @WrapInTransaction
    public static ContentType getProductLikeContentType(final String contentTypeName,
            final Host site,
            final Set<String> workflowIds) {

        ContentType productType = null;
        try {
            try {
                productType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (productType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                if (null != site) {
                    fields.add(
                            new FieldDataGen()
                                    .name("Site or Folder")
                                    .velocityVarName("hostfolder")
                                    .required(Boolean.TRUE)
                                    .type(HostFolderField.class)
                                    .next()
                    );
                }
                fields.add(
                        new FieldDataGen()
                                .name("Title")
                                .velocityVarName("title")
                                .type(TextField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("urlTitle")
                                .velocityVarName("urlTitle")
                                .type(TextField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Type")
                                .velocityVarName("type")
                                .type(RadioField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Inception")
                                .velocityVarName("inception")
                                .defaultValue(null)
                                .type(DateField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Description")
                                .velocityVarName("description")
                                .type(LineDividerField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Expense Ration")
                                .velocityVarName("expenseRatio")
                                .type(TextField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Risk")
                                .velocityVarName("risk")
                                .type(SelectField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Asset Class")
                                .velocityVarName("assetClass")
                                .type(CustomField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Market Cap")
                                .velocityVarName("marketCap")
                                .type(SelectField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Style")
                                .velocityVarName("style")
                                .type(SelectField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Quality")
                                .velocityVarName("quality")
                                .type(SelectField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Maturity")
                                .velocityVarName("maturity")
                                .type(SelectField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Summary")
                                .velocityVarName("summary")
                                .type(WysiwygField.class)
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
                ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .workflowId(workflowIds)
                        .fields(fields);

                if (null != site) {
                    contentTypeDataGen.host(site);
                }

                productType = contentTypeDataGen.nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return productType;
    }


    public static ContentType getYoutubeLikeContentType() {
        return getYoutubeLikeContentType("Youtube" + System.currentTimeMillis(),
                APILocator.systemHost(), null);
    }

    @WrapInTransaction
    public static ContentType getYoutubeLikeContentType(final String contentTypeName,
            final Host site,
            final Set<String> workflowIds) {

        ContentType youtubeType = null;
        try {
            try {
                youtubeType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (youtubeType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                if (null != site) {
                    fields.add(
                            new FieldDataGen()
                                    .name("Widget Usage")
                                    .velocityVarName("widgetUsage")
                                    .type(ConstantField.class)
                                    .next()
                    );
                }
                fields.add(
                        new FieldDataGen()
                                .name("Widget Code")
                                .velocityVarName("widgetCode")
                                .type(ConstantField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Widget Pre-Executed")
                                .velocityVarName("widgetPreExecuted")
                                .type(ConstantField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Search")
                                .velocityVarName("search")
                                .type(CustomField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("WidgetTitle")
                                .velocityVarName("widgetTitle")
                                .defaultValue(null)
                                .required(true)
                                .type(TextField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Author")
                                .velocityVarName("author")
                                .defaultValue(null)
                                .type(TextField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Length")
                                .velocityVarName("length")
                                .type(TextField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Thumbnail Small")
                                .velocityVarName("thumbnail")
                                .type(TextField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Thumbnail Large")
                                .velocityVarName("thumbnail2")
                                .type(TextField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Published")
                                .velocityVarName("published")
                                .type(TextField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("URL")
                                .velocityVarName("url")
                                .type(TextField.class)
                                .next()
                );

/*
                fields.add(
                        new FieldDataGen()
                                .name("Products-Youtube")
                                .velocityVarName("productsYoutube")
                                .defaultValue(null)
                                .type(RelationshipField.class)
                                .values(
                                    String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal())
                                 )
                                .relationType(contentTypeName + StringPool.PERIOD + "products")
                                .next()
                );
*/

                ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .workflowId(workflowIds)
                        .fields(fields);

                if (null != site) {
                    contentTypeDataGen.host(site);
                }

                youtubeType = contentTypeDataGen.nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return youtubeType;
    }

    @WrapInTransaction
    public static Contentlet createNewsLikeURLMappedContent(final String newsPatternPrefix,
            final Date sysPublishDate, final long languageId, final Host host,
            final String detailPageIdentifier) {

        final ContentType newsContentType = getNewsLikeContentType(
                "News" + System.currentTimeMillis(),
                host,
                detailPageIdentifier,
                newsPatternPrefix + "{urlTitle}");

        return getNewsContent(true, languageId,
                newsContentType.id(), host, sysPublishDate);
    }

    public static ContentType getMultipleBinariesContentType() {
        return getMultipleBinariesContentType("MultipleBinaries" + System.currentTimeMillis(),
                APILocator.systemHost(), null);
    }

    /**
     * This will give you a CT that has 3 non-required binaries. The default metadata generated gets
     * removed.
     *
     * @param contentTypeName
     * @param site
     * @param workflowIds
     * @return
     */
    @WrapInTransaction
    public static ContentType getMultipleBinariesContentType(final String contentTypeName,
            final Host site,
            final Set<String> workflowIds) {

        ContentType contentType = null;
        try {
            try {
                contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (contentType == null) {

                final List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                if (null != site) {
                    fields.add(
                            new FieldDataGen()
                                    .name("Site or Folder")
                                    .velocityVarName("hostfolder")
                                    .sortOrder(1)
                                    .required(Boolean.TRUE)
                                    .type(HostFolderField.class)
                                    .next()
                    );
                }

                fields.add(
                        new FieldDataGen()
                                .name("Title")
                                .velocityVarName("title")
                                .sortOrder(2)
                                .type(TextField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name(FILE_ASSET_1)
                                .velocityVarName(FILE_ASSET_1)
                                .sortOrder(3)
                                //The only way we can guarantee a field won't be indexed is by setting all these to false
                                .indexed(false).searchable(false).listed(false).unique(false)
                                .type(BinaryField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name(FILE_ASSET_2)
                                .velocityVarName(FILE_ASSET_2)
                                .sortOrder(4)
                                .indexed(true)
                                .type(BinaryField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name(FILE_ASSET_3)
                                .velocityVarName(FILE_ASSET_3)
                                .sortOrder(5)
                                .indexed(true)
                                .type(BinaryField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("image1")
                                .velocityVarName("image1")
                                .sortOrder(5)
                                .indexed(false)
                                .type(ImageField.class)
                                .next()
                );

                final ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .workflowId(workflowIds)
                        .fields(fields);

                if (null != site) {
                    contentTypeDataGen.host(site);
                }

                contentType = contentTypeDataGen.nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return contentType;
    }

    public static Contentlet getMultipleImageBinariesContent(Boolean persist, long languageId,
            String contentTypeId) {

        if (null == contentTypeId) {
            contentTypeId = getMultipleBinariesContentType().id();
        }

        final Contentlet fileAssetJpgContent = TestDataUtils
                .getFileAssetContent(true, languageId, TestFile.JPG);

        return persistBinaries(persist, languageId, contentTypeId, ImmutableMap
                .of(FILE_ASSET_1, TestFile.JPG, FILE_ASSET_2, TestFile.PNG, "image1",
                        fileAssetJpgContent.getIdentifier()));

    }

    public static Contentlet getMultipleBinariesContent(Boolean persist, long languageId,
            String contentTypeId) {

        if (null == contentTypeId) {
            contentTypeId = getMultipleBinariesContentType().id();
        }

        return persistBinaries(persist, languageId, contentTypeId,
                ImmutableMap.of(FILE_ASSET_1, TestFile.PDF, FILE_ASSET_2, TestFile.TEXT));
    }

    private static Contentlet persistBinaries(final Boolean persist, final long languageId,
            final String contentTypeId, final Map<String, Object> files) {
        try {

            final ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                    .languageId(languageId)
                    .setProperty("title", "blah");

            files.forEach((fileName, file) -> {
                        try {
                            if (file instanceof TestFile) {
                                final TestFile testFile = (TestFile) file;
                                contentletDataGen.setProperty(fileName, nextBinaryFile(testFile));
                            } else {
                                contentletDataGen.setProperty(fileName, file);
                            }

                        } catch (Exception e) {
                            Logger.error(TestDataUtils.class, e);
                        }
                    }
            );

            if (persist) {
                final Contentlet persisted = contentletDataGen.nextPersisted();
                files.forEach((fileName, testFile) -> {
                    final Object file = persisted.get(fileName);
                    if (file instanceof File) {
                        removeAnyMetadata((File) file);
                    }
                });
                return persisted;
            } else {
                return contentletDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * This will use one of the internal test-files to generate a temp copy
     *
     * @param testFile
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    public static File nextBinaryFile(final TestFile testFile)
            throws IOException, URISyntaxException {
        final String testImagePath = testFile.filePath;
        final String ext = UtilMethods.getFileExtension(testImagePath);
        final File originalTestImage = new File(
                ConfigTestHelper.getUrlToTestResource(testImagePath).toURI());
        final File testImage = new File(Files.createTempDir(),
                "test_binary" + System.currentTimeMillis() + "." + ext);
        FileUtil.copyFile(originalTestImage, testImage);
        return testImage;
    }

    /**
     * Test data's generated with medata. This removes it. For the new Api to be able to generate
     * new
     *
     * @param binary
     */
    public static void removeAnyMetadata(final File binary) {
        final File immediateParent = new File(binary.getParent());
        //delete any previously generated json
        final File[] files = new File(immediateParent.getParent()).listFiles();
        if (null != files) {
            final List<File> jsonFiles = Stream.of(files)
                    .filter(file -> file.getName().endsWith("json"))
                    .collect(Collectors.toList());
            jsonFiles.forEach(file -> file.delete());
        }
    }


    public static ContentType newContentTypeFieldTypesGalore() {
        final String contentTypeName = String.format("WithAllAvailableFieldTypes%s",
                System.nanoTime());
        return newContentTypeFieldTypesGalore(contentTypeName, null);
    }

    public static ContentType newContentTypeFieldTypesGalore(final Category parent) {
        final String contentTypeName = String.format("WithAllAvailableFieldTypes%s",
                System.nanoTime());
        return newContentTypeFieldTypesGalore(contentTypeName, null, parent);
    }

    public static ContentType newContentTypeFieldTypesGalore(final String contentTypeName,
            final Set<String> workflowIds) {
        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        final Optional<Category> anyTopLevelCategory = Try.of(
                () -> categoryAPI.findTopLevelCategories(APILocator.systemUser(), false).stream()
                        .findAny()).get();
        return newContentTypeFieldTypesGalore(contentTypeName, workflowIds,
                anyTopLevelCategory.orElse(null));
    }

    @WrapInTransaction
    public static ContentType newContentTypeFieldTypesGalore(final String contentTypeName,
            Set<String> workflowIds, final Category parent) {

        ContentType contentType = null;
        try {
            try {
                contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (contentType == null) {

                final WorkflowScheme systemWorkflow = TestWorkflowUtils.getSystemWorkflow();
                final Set<String> collectedWorkflowIds = new HashSet<>();
                collectedWorkflowIds.add(systemWorkflow.getId());
                if (null != workflowIds) {
                    collectedWorkflowIds.addAll(workflowIds);
                }

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();

                fields.add(
                        new FieldDataGen()
                                .name("hiddenBool")
                                .velocityVarName("hiddenBool")
                                .type(HiddenField.class)
                                .dataType(DataTypes.BOOL)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("hostFolder")
                                .velocityVarName("hostFolder")
                                .type(HostFolderField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("textFieldNumeric")
                                .velocityVarName("textFieldNumeric")
                                .type(TextField.class)
                                .dataType(DataTypes.INTEGER)
                                .defaultValue("0")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("textFieldFloat")
                                .velocityVarName("textFieldFloat")
                                .type(TextField.class)
                                .dataType(DataTypes.FLOAT)
                                .defaultValue("0")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("textField")
                                .velocityVarName("textField")
                                .type(TextField.class)
                                .dataType(DataTypes.TEXT)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("binaryField")
                                .velocityVarName("binaryField")
                                .type(BinaryField.class).dataType(DataTypes.SYSTEM)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("textAreaField")
                                .velocityVarName("textAreaField")
                                .type(TextAreaField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("dateField")
                                .velocityVarName("dateField")
                                .defaultValue(null)
                                .type(DateField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("dateTimeField")
                                .velocityVarName("dateTimeField")
                                .defaultValue(null)
                                .type(DateTimeField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("timeField")
                                .velocityVarName("timeField")
                                .defaultValue(null)
                                .type(TimeField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("tagField")
                                .velocityVarName("tagField")
                                .defaultValue(null)
                                .type(TagField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("keyValueField")
                                .velocityVarName("keyValueField")
                                .type(KeyValueField.class)
                                .defaultValue("{}")
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("imageField")
                                .velocityVarName("imageField")
                                .type(ImageField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("checkboxField")
                                .velocityVarName("checkboxField")
                                .type(CheckboxField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("selectField")
                                .velocityVarName("selectField")
                                .type(SelectField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("multiSelectField")
                                .velocityVarName("multiSelectField")
                                .type(MultiSelectField.class)
                                .next()

                );

                fields.add(
                        new FieldDataGen()
                                .name("radioField")
                                .velocityVarName("radioField")
                                .type(RadioField.class)
                                .next()

                );

                fields.add(
                        new FieldDataGen()
                                .name("wysiwygField")
                                .velocityVarName("wysiwygField")
                                .type(WysiwygField.class)
                                .next()

                );

                //Category field
                if (null != parent) {
                    fields.add(new FieldDataGen()
                            .name("categoryField")
                            .velocityVarName("categoryField")
                            .type(CategoryField.class)
                            .values(parent.getInode()).next());
                }

                contentType = new ContentTypeDataGen()
                        .baseContentType(BaseContentType.CONTENT)
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .workflowId(collectedWorkflowIds)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return contentType;
    }

    public static ContentType newContentTypeWithMultipleCategoryFields(final String contentTypeName,
            final Set<String> workflowIds) {
        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        final List<Category> categories = Try.of(
                        () -> categoryAPI.findTopLevelCategories(APILocator.systemUser(), false))
                .getOrElseThrow(DotRuntimeException::new);
        return newContentTypeWithMultipleCategoryFields(contentTypeName, workflowIds, categories);
    }

    @WrapInTransaction
    public static ContentType newContentTypeWithMultipleCategoryFields(final String contentTypeName,
            Set<String> workflowIds, final List<Category> parents) {

        ContentType contentType;
        try {

            contentType = Try.of(() -> APILocator.getContentTypeAPI(APILocator.systemUser())
                    .find(contentTypeName)).getOrNull();

            if (contentType == null) {

                final WorkflowScheme systemWorkflow = TestWorkflowUtils.getSystemWorkflow();
                final Set<String> collectedWorkflowIds = new HashSet<>();
                collectedWorkflowIds.add(systemWorkflow.getId());
                if (null != workflowIds) {
                    collectedWorkflowIds.addAll(workflowIds);
                }

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();

                fields.add(
                        new FieldDataGen()
                                .name("title")
                                .velocityVarName("title")
                                .type(TextField.class)
                                .dataType(DataTypes.TEXT)
                                .next()
                );

                //Category field
                int i = 0;
                for (Category parent : parents) {
                    final String name = "categoryField" + i;
                    fields.add(new FieldDataGen()
                            .name(name)
                            .velocityVarName(name)
                            .type(CategoryField.class)
                            .values(parent.getInode()).next());
                    i++;
                }

                contentType = new ContentTypeDataGen()
                        .baseContentType(BaseContentType.CONTENT)
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .workflowId(collectedWorkflowIds)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return contentType;
    }

    /**
     * This method creates a parent category and a couple of child categories.
     *
     * @return the parent category
     */
    public static Category createCategories() {

        final String parentCategoryName = String.format("Parent-Category-[%d]",
                System.currentTimeMillis());
        final Category parentCategory = new CategoryDataGen()
                .setCategoryName(parentCategoryName)
                .setKey(parentCategoryName + "Key")
                .setCategoryVelocityVarName(parentCategoryName)
                .setSortOrder(1)
                .nextPersisted();

        for (int i = 0; i <= 2; i++) {
            final String childCategoryName = String.format("Child-Category-[%d]-[%d]", i,
                    System.currentTimeMillis());

            new CategoryDataGen()
                    .setCategoryName(childCategoryName)
                    .setKey(childCategoryName + "Key")
                    .setCategoryVelocityVarName(childCategoryName)
                    .setSortOrder(1)
                    .parent(parentCategory).nextPersisted();

        }

        return parentCategory;
    }

    @WrapInTransaction
    public static ContentType createPersonaLikeContentType(final String contentTypeName, Set<String> workflowIds)
            throws DotDataException, DotSecurityException {
        final User user = APILocator.systemUser();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);

        return contentTypeAPI.save(ContentTypeBuilder.builder(PersonaContentType.class).folder(
                        FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("Persona2")
                .owner(user.getUserId()).build());
    }


        /**
         * This method waits for the reindex queue to be empty.
         *
         * @return the parent category
         */
    public static boolean waitForEmptyQueue(int waitTime) {
        final ReindexQueueAPI reindexQueueAPI = APILocator.getReindexQueueAPI();
        try {
            Awaitility.await().atMost(waitTime, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .until(reindexQueueAPI::areRecordsLeftToIndex, equalTo(false));
            return true;
        } catch (ConditionTimeoutException e) {
            Logger.warn(TestWorkflowUtils.class, "Reindex Queue is not empty after "+waitTime+"s", new Throwable());
            return false;
        }
    }

    public static boolean waitForEmptyQueue() {
        return waitForEmptyQueue(Config.getIntProperty("DEFAULT_INDEX_QUEUE_WAIT",30));
    }

    public static void assertEmptyQueue() {
        Assert.assertTrue("Queue should be empty", waitForEmptyQueue());
    }

}
