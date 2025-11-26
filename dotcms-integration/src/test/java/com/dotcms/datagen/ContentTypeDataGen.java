package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ContentTypeDataGen extends AbstractDataGen<ContentType> {

    private final long currentTime = System.currentTimeMillis();

    private BaseContentType baseContentType = BaseContentType.CONTENT;
    private String descriptionField = "testDescription" + currentTime;
    private boolean fixedField = Boolean.FALSE;
    private String name = "testName" + currentTime;
    private Date iDateField = new Date();
    private String detailPage;
    private String urlMapPattern;
    private String publishDateFieldVarName;
    private String expireDateFieldVarName;
    private boolean systemField = Boolean.FALSE;
    private String velocityVarName = "testVarname" + currentTime;
    private List<Field> fields = new ArrayList<>();
    private Set<String> workflowIds = new HashSet<>();
    private User owner = user;
    private List<Category> categories = new ArrayList<>();
    private String hostName;

    public static void addField(Field hostFolderField) {
        try {
            APILocator.getContentTypeFieldAPI().save(hostFolderField, APILocator.systemUser());
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public  ContentTypeDataGen addCategory(final Category category){
        categories.add(category);
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen baseContentType(final BaseContentType baseContentType) {
        this.baseContentType = baseContentType;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen description(final String description) {
        this.descriptionField = description;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen fixed(final boolean fixed) {
        this.fixedField = fixed;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen name(final String name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen iDate(final Date iDate) {
        this.iDateField = iDate;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen detailPage(final String detailPage) {
        this.detailPage = detailPage;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen urlMapPattern(final String urlMapPattern) {
        this.urlMapPattern = urlMapPattern;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen system(final boolean system) {
        this.systemField = system;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen velocityVarName(final String velocityVarName) {
        this.velocityVarName = velocityVarName;
        return this;
    }

    public ContentTypeDataGen publishDateFieldVarName(final String publishDateFieldVarName) {
        this.publishDateFieldVarName = publishDateFieldVarName;
        return this;
    }

    public ContentTypeDataGen expireDateFieldVarName(final String expireDateFieldVarName) {
        this.expireDateFieldVarName = expireDateFieldVarName;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen folder(final Folder folder) {
        this.folder = folder;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen host(final Host host) {
        this.host = host;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen hostName(final String hostName) {
        this.hostName = hostName;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen user(final User user) {
        this.user = user;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen fields(final List<Field> fields) {
        this.fields = fields;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen field(final Field field) {
        this.fields.add(field);
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen workflowId(final String... identifier) {
        if(null != identifier){
           this.workflowIds.addAll(Arrays.asList(identifier));
        }
        return this;
    }

    public ContentTypeDataGen workflowId(final Set<String> identifier) {
        if(null != identifier){
           this.workflowIds.addAll(identifier);
        }
        return this;
    }

    public ContentTypeDataGen owner(User owner) {
        this.owner = owner;
        return this;
    }

    @Override
    public ContentType next() {

        return ContentTypeBuilder.builder(baseContentType.immutableClass())
                .description(descriptionField)
                .fixed(fixedField)
                .name(name)
                .owner(owner.getUserId())
                .detailPage(detailPage)
                .urlMapPattern(urlMapPattern)
                .publishDateVar(publishDateFieldVarName)
                .expireDateVar(expireDateFieldVarName)
                .system(systemField)
                .variable(velocityVarName)
                .folder(folder.getInode())
                .siteName(hostName)
                .host(host.getIdentifier())
                .iDate(iDateField)
                .build();
    }

    @WrapInTransaction
    @Override
    public ContentType persist(final ContentType contentType) {
        try {
            final ContentType persistedContentType = APILocator
                    .getContentTypeAPI(APILocator.systemUser()).save(contentType, fields);
            workflowIds.add(APILocator.getWorkflowAPI().findSystemWorkflowScheme().getId());
            APILocator.getWorkflowAPI()
                    .saveSchemeIdsForContentType(persistedContentType, workflowIds);

            final User systemUser = APILocator.systemUser();
            for (final Category category : categories) {
                final Field categoryField = new FieldDataGen().type(CategoryField.class)
                        .name(category.getCategoryName())
                        .velocityVarName(category.getKey())
                        .values(category.getInode())
                        .contentTypeId(persistedContentType.id())
                        .next();

                APILocator.getContentTypeFieldAPI().save(categoryField, systemUser);
            }

            return APILocator.getContentTypeAPI(APILocator.systemUser()).find(persistedContentType.variable());
        } catch (Exception e) {
            throw new RuntimeException("Unable to persist ContentType.", e);
        }
    }

    /**
     * Creates a new {@link ContentType} instance and persists it in DB
     *
     * @return A new ContentType instance persisted in DB
     */
    @WrapInTransaction
    @Override
    public ContentType nextPersisted() {
        return persist(next());
    }

    /**
     * Creates a new {@link ContentType} instance and persists it in DB
     *
     * @return A new ContentType instance persisted in DB
     */
    @WrapInTransaction
    public ContentType nextPersistedWithSampleFields() {

        final ContentType contentType;

        try {

            //Now we need to add some fields to the content type
            for (int i = 0; i < 5; i++) {
                field(new FieldDataGen().next());
            }

            //Adding a category field
            final Collection<Category> topLevelCategories = APILocator.getCategoryAPI()
                    .findTopLevelCategories(APILocator.systemUser(), false);
            final Optional<Category> anyTopLevelCategory = topLevelCategories.stream().findAny();

            anyTopLevelCategory.map(category -> new FieldDataGen()
                    .type(CategoryField.class)
                    .defaultValue(null)
                    .values(category.getInode())
                    .next()).ifPresent(this::field);

            //Persist the content type with the sample fields
            contentType = persist(next());
        } catch (Exception e) {
            throw new RuntimeException("Unable to persist ContentType.", e);
        }

        return contentType;
    }

    public static void remove(final ContentType contentType) {
        remove(contentType, true);
    }

    public static void remove(final ContentType contentType, final Boolean failSilently) {

        if (null != contentType) {
            try {
                APILocator.getContentTypeAPI(APILocator.systemUser()).delete(contentType);
            } catch (Exception e) {
                if (failSilently) {
                    Logger.error(ContentTypeDataGen.class, "Unable to remove ContentType.", e);
                } else {
                    throw new RuntimeException("Unable to remove ContentType.", e);
                }
            }
        }
    }

    @WrapInTransaction
    public static ContentType createLanguageVariableContentType() {
        final User systemUser = APILocator.systemUser();

        try {
            ContentType languageVariableContentType = null;

            try {
                languageVariableContentType = APILocator.getContentTypeAPI(systemUser).find(LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME);
            } catch (NotFoundInDbException ex) {
              Logger.info(ContentTypeDataGen.class, "Content type "+LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME+" not found Creating language variable content type");
            }

            if (languageVariableContentType == null) {
                final ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen();

                languageVariableContentType =  contentTypeDataGen.baseContentType(BaseContentType.KEY_VALUE)
                        .name(LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME)
                        .nextPersisted();
            }

            PermissionUtilTest.addAnonymousUser(languageVariableContentType);

            return languageVariableContentType;
        } catch (DotSecurityException | DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    public static ContentTypeDataGen createWidgetContentType (final String code){
        return new WidgetContentTypeDataGen()
                .code(code)
                .baseContentType(BaseContentType.WIDGET);
    }

}