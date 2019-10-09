package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotcms.repackage.com.google.common.base.Supplier;
import com.dotcms.translate.ServiceParameter;
import com.dotcms.translate.TranslationException;
import com.dotcms.translate.TranslationService;
import com.dotcms.translate.TranslationUtil;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.*;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

import javax.ws.rs.HEAD;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TranslationActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;
    private final Supplier<WorkflowAPI>   workflowAPI;
    private final ApiProvider apiProvider;
    private final TranslationUtil translationUtil;
    private final TranslationService translationService;
    private static final String TRANSLATE_TO_DEFAULT = "all";
    private static final String FIELD_TYPES_DEFAULT = "text,wysiwyg,textarea";
    private static final String IGNORE_FIELDS_DEFAULT = "";

    public TranslationActionlet() {
        this(new ApiProvider(), TranslationUtil.getUtil(), TranslationUtil.getService(), ()->APILocator.getWorkflowAPI());
    }

    @VisibleForTesting
    protected TranslationActionlet(final ApiProvider apiProvider, final TranslationUtil translationUtil,
                                   final TranslationService translationService, final Supplier<WorkflowAPI> workflowAPI) {

        super ();
        this.apiProvider        = apiProvider;
        this.translationUtil    = translationUtil;
        this.translationService = translationService;
        this.workflowAPI        = workflowAPI;
    }

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        final List<WorkflowActionletParameter> params = new ArrayList<>();

        params.add(new WorkflowActionletParameter("translateTo", "Translate to", TRANSLATE_TO_DEFAULT, true));
        params.add(new WorkflowActionletParameter("fieldTypes", "Translate Field Types", FIELD_TYPES_DEFAULT, true));
        params.add(new WorkflowActionletParameter("ignoreFields", "Ignore Fields (velocity var name)",
            IGNORE_FIELDS_DEFAULT, false));

        final List<ServiceParameter> serviceParams = translationService.getServiceParameters();
        for (final ServiceParameter param : serviceParams) {
            params.add(new WorkflowActionletParameter(param.getKey(), param.getName(), param.getValue(), true));
        }

        return params;
    }

    @Override
    public String getName() {
        String name = null;
        try {
            name = LanguageUtil
                .get((User) null, "com.dotmarketing.portlets.workflows.actionlet.TranslationActionlet.name");
        } catch(LanguageException e) {
            Logger.error(this, "Unable to get actionlet name.", e);
        }

        return name;
    }

    @Override
    public String getHowTo() {
        String desc = null;
        try {
            desc = LanguageUtil
                .get((User) null, "com.dotmarketing.portlets.workflows.actionlet.TranslationActionlet.desc");
        } catch(LanguageException e) {
            Logger.error(this, "Unable to get actionlet name.", e);
        }

        return desc;
    }

    @Override
    public void executeAction(final WorkflowProcessor processor, final Map<String, WorkflowActionClassParameter> params)
        throws WorkflowActionFailureException {

        Preconditions.checkNotNull(params, "Workflow Action Params can't be null.");

        final ContentletAPI contentletAPI = this.apiProvider.contentletAPI();
        final PermissionAPI permissionAPI = this.apiProvider.permissionAPI();
        final CategoryAPI   categoryAPI   = this.apiProvider.categoryAPI();

        final String translateToStr =
            params.get("translateTo") != null ? params.get("translateTo").getValue() : TRANSLATE_TO_DEFAULT;
        final String fieldTypesStr =
            params.get("fieldTypes") != null ?  params.get("fieldTypes").getValue() :       FIELD_TYPES_DEFAULT;
        final List<String> translateTo = Arrays.asList(translateToStr.split(" *(,| ) *"));
        final List<String> fieldTypes  = Arrays.asList(fieldTypesStr.split ("\\s*(,|\\s)\\s*"));
        final String ignoreFieldsStr   =
            params.get("ignoreFields")!=null? params.get("ignoreFields").getValue():        IGNORE_FIELDS_DEFAULT;
        final List<String> ignoreFields = !Strings.isNullOrEmpty(ignoreFieldsStr)
            ? Arrays.asList(ignoreFieldsStr.split("\\s*(,|\\s)\\s*"))
            : new ArrayList<>();

        setServiceParameters(params);

        final User user = processor.getUser();
        final Contentlet sourceContentlet = processor.getContentlet();

        try {
            // Source content must be already persisted as Precondition. Let's check that out
            contentletAPI
                .findContentletByIdentifier(sourceContentlet.getIdentifier(), false, sourceContentlet.getLanguageId(),
                    user, false);
        } catch (DotStateException e) {
            throw new WorkflowActionFailureException(
                "Unable to find source Content by identifier. Content must be persisted first.", e);
        } catch (Exception e) {
            throw new WorkflowActionFailureException("Error occurred trying to find Content by identifier.", e);
        }

        final List<Field> translateFields = this.translationUtil.getFieldsOfContentlet(sourceContentlet, fieldTypes, ignoreFields);

        Preconditions.checkArgument(!translateFields.isEmpty(),
            "No Fields no translate. Please check the 'Translate Field Types' parameter in the actionlet config.");

        final List<Language> translateLanguages = this.translationUtil.getLanguagesByLanguageCodes(translateTo);

        // let's remove the language of the source contentlet
        this.translateContents(processor, contentletAPI, permissionAPI,
                categoryAPI, user, sourceContentlet, translateFields, translateLanguages);
    }

    private void translateContents(final WorkflowProcessor processor,
                                   final ContentletAPI contentletAPI,
                                   final PermissionAPI permissionAPI,
                                   final CategoryAPI categoryAPI,
                                   final User user,
                                   final Contentlet sourceContentlet,
                                   final List<Field> translateFields,
                                   final List<Language> translateLanguages) {

        final List<Language> otherLanguages =
            translateLanguages.stream().filter(lang -> lang.getId() != sourceContentlet.getLanguageId())
                .collect(Collectors.toList());

        try {

            final boolean live = sourceContentlet.isLive();
            final List<Contentlet> translatedContents =
                this.translationService.translateContent(sourceContentlet, otherLanguages, translateFields, user);

            for (final Contentlet translatedContent : translatedContents) {

                sourceContentlet.setTags();
                copyBinariesAndTags(user, sourceContentlet, translatedContent);
                final List<Category> categories = categoryAPI.getParents(sourceContentlet, user, true);
                final ContentletRelationships contentletRelationships =
                        processor.getContentletDependencies() != null ? processor
                                .getContentletDependencies().getRelationships()
                                : contentletAPI.getAllRelationships(sourceContentlet);
                final List<Permission> permissions = permissionAPI.getPermissions(sourceContentlet, false, true);

                this.runWorkflowIfCould (contentletAPI, translatedContent, contentletRelationships, categories, permissions, user, live);
            }

            contentletAPI.unlock(sourceContentlet, user, false);
        } catch (TranslationException e) {
            throw new WorkflowActionFailureException("Error executing Translation Actionlet", e);
        } catch (DotDataException | DotSecurityException e) {
            throw new WorkflowActionFailureException("Error saving translated content", e);
        }
    }

    private Contentlet runWorkflowIfCould (final ContentletAPI contentletAPI, final Contentlet translatedContent,
                                 final ContentletRelationships contentletRelationships, final List<Category> categories,
                                 final List<Permission> permissions, final User user, final boolean live) throws DotSecurityException, DotDataException {

        final Optional<WorkflowAction> workflowActionSaveOpt =
                workflowAPI.get().findActionMappedBySystemActionContentlet
                        (translatedContent, WorkflowAPI.SystemAction.NEW, user);

        if (workflowActionSaveOpt.isPresent()) {

            final boolean hasSave     = workflowActionSaveOpt.get().hasSaveActionlet();
            final boolean noRecursive = !this.hasTranslationActionlet(workflowActionSaveOpt.get());

            if (hasSave && noRecursive) {

                Logger.debug(this, ()-> "Translating a contentlet with the save action: "
                        + workflowActionSaveOpt.get().getName());
                final Contentlet saveTranslatedContent = workflowAPI.get().fireContentWorkflow
                        (translatedContent, new ContentletDependencies.Builder()
                        .workflowActionId(workflowActionSaveOpt.get().getId())
                        .relationships(contentletRelationships).categories(categories)
                        .permissions(permissions).modUser(user).build());

                return live && !workflowActionSaveOpt.get().hasPublishActionlet()?
                        runWorkflowPublishIfCould(contentletAPI, contentletRelationships,
                                categories, permissions, user, saveTranslatedContent):
                        saveTranslatedContent;
            } else if (noRecursive) {

                // runs the checkin with workflow
                translatedContent.setActionId(workflowActionSaveOpt.get().getId());
            }
        }

        return this.checkinPublish(contentletAPI, translatedContent, contentletRelationships, categories, permissions, user, live);
    }

    private Contentlet runWorkflowPublishIfCould(final ContentletAPI contentletAPI,
                                                 final ContentletRelationships contentletRelationships,
                                                 final List<Category>   categories,
                                                 final List<Permission> permissions,
                                                 final User user,
                                                 final Contentlet saveTranslatedContent) throws DotDataException, DotSecurityException {

        final Optional<WorkflowAction> workflowActionPublishOpt =
                workflowAPI.get().findActionMappedBySystemActionContentlet
                        (saveTranslatedContent, WorkflowAPI.SystemAction.PUBLISH, user);

        if (workflowActionPublishOpt.isPresent()) {

            final boolean noRecursive = !this.hasTranslationActionlet(workflowActionPublishOpt.get());

            if (workflowActionPublishOpt.get().hasPublishActionlet() && noRecursive) {

                Logger.debug(this, () -> "Translating a contentlet with the publish action: "
                        + workflowActionPublishOpt.get().getName());
                return workflowAPI.get().fireContentWorkflow
                        (saveTranslatedContent, new ContentletDependencies.Builder()
                                .workflowActionId(workflowActionPublishOpt.get().getId())
                                .relationships(contentletRelationships).categories(categories)
                                .permissions(permissions).modUser(user).build());
            } else if (noRecursive) {

                saveTranslatedContent.setActionId(workflowActionPublishOpt.get().getId());
            }
        }

        if (null == saveTranslatedContent.getActionId()) {
            saveTranslatedContent.setProperty(Contentlet.DISABLE_WORKFLOW, true); // it is needed to avoid recursive call
        }
        contentletAPI.publish(saveTranslatedContent, user, false);
        return saveTranslatedContent;
    }

    // this method avoid the recursive translation
    private boolean hasTranslationActionlet(final WorkflowAction workflowAction) throws DotDataException {

        final List<WorkflowActionClass> workflowActionClasses =
                APILocator.getWorkflowAPI().findActionClasses(workflowAction);

        return workflowActionClasses.stream().anyMatch(
                workflowActionClass -> workflowActionClass.getClazz().equals(this.getClass().getName()));
    }

    private Contentlet checkinPublish (final ContentletAPI contentletAPI, final Contentlet translatedContent,
                                 final ContentletRelationships contentletRelationships, final List<Category> categories,
                                 final List<Permission> permissions, final User user, final boolean live) throws DotSecurityException, DotDataException {

        if (null == translatedContent.getActionId()) {
            translatedContent.setProperty(Contentlet.DISABLE_WORKFLOW, true); // it is needed to avoid recursive call
        }
        final Contentlet savedTranslatedContent = contentletAPI.checkin(translatedContent, contentletRelationships,
                categories, permissions, user, false);

        if (live) {
            contentletAPI.publish(savedTranslatedContent, user, false);
        }

        return savedTranslatedContent;
    }

    private void setServiceParameters(final Map<String, WorkflowActionClassParameter> actionParams) {

        if(translationService!=null) {

            final List<ServiceParameter> serviceParams = translationService.getServiceParameters();

            if(serviceParams!=null) {
                for (final ServiceParameter serviceParam : serviceParams) {
                    final WorkflowActionClassParameter actionParam = actionParams.get(serviceParam.getKey());

                    if (actionParam != null && !Strings.isNullOrEmpty(actionParam.getValue())) {
                        serviceParam.setValue(actionParam.getValue());
                    }
                }

                translationService.setServiceParameters(serviceParams);
            }
        }
    }

    void copyBinariesAndTags(final User user, final Contentlet sourceContentlet, final Contentlet translatedContent)
        throws DotDataException, DotSecurityException, TranslationException {

        final Structure structure = translatedContent.getStructure();
        final List<Field> list    = FieldsCache.getFieldsByStructureInode(structure.getInode());

        for (final Field field : list) {
            if (Field.FieldType.BINARY.toString().equals(field.getFieldType())) {

                final java.io.File inputFile = APILocator
                    .getContentletAPI().getBinaryFile(sourceContentlet.getInode(), field.getVelocityVarName(), user);
                if (inputFile != null) {

                    final java.io.File acopyFolder = new java.io.File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary()
                        + java.io.File.separator + user.getUserId() + java.io.File.separator + field.getFieldContentlet()
                        + java.io.File.separator + UUIDGenerator.generateUuid());

                    if (!acopyFolder.exists()) {
                        acopyFolder.mkdir();
                    }

                    final String shortFileName = FileUtil.getShortFileName(inputFile.getAbsolutePath());
                    final java.io.File binaryFile = new java.io.File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary()
                        + java.io.File.separator + user.getUserId() + java.io.File.separator + field.getFieldContentlet()
                        + java.io.File.separator + shortFileName.trim());

                    try {

                        FileUtil.copyFile(inputFile, binaryFile);
                        translatedContent.setBinary(field.getVelocityVarName(), binaryFile);
                    } catch (IOException e) {
                        throw new TranslationException(e);
                    }
                }
            } else if ( field.getFieldType().equals(Field.FieldType.TAG.toString()) ) {

                translatedContent.setStringProperty(field.getVelocityVarName(),
                    sourceContentlet.getStringProperty(field.getVelocityVarName()));
            }
        }
    }

}