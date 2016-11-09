package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.base.Strings;
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
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TranslationActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;
    private ApiProvider apiProvider;
    private TranslationUtil translationUtil;
    private TranslationService translationService;
    private static final String TRANSLATE_TO_DEFAULT = "all";
    private static final String FIELD_TYPES_DEFAULT = "text,wysiwyg,textarea";
    private static final String IGNORE_FIELDS_DEFAULT = "";

    public TranslationActionlet() {
        this(new ApiProvider(), TranslationUtil.getUtil(), TranslationUtil.getService());
    }

    @VisibleForTesting
    protected TranslationActionlet(ApiProvider apiProvider, TranslationUtil translationUtil,
                                   TranslationService translationService) {
        this.apiProvider = apiProvider;
        this.translationUtil = translationUtil;
        this.translationService = translationService;
    }

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        List<WorkflowActionletParameter> params = new ArrayList<WorkflowActionletParameter>();

        params.add(new WorkflowActionletParameter("translateTo", "Translate to", TRANSLATE_TO_DEFAULT, true));
        params.add(new WorkflowActionletParameter("fieldTypes", "Translate Field Types", FIELD_TYPES_DEFAULT, true));
        params.add(new WorkflowActionletParameter("ignoreFields", "Ignore Fields (velocity var name)",
            IGNORE_FIELDS_DEFAULT, false));

        List<ServiceParameter> serviceParams = translationService.getServiceParameters();
        for (ServiceParameter param : serviceParams) {
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
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
        throws WorkflowActionFailureException {

        ContentletAPI contentAPI = apiProvider.contentletAPI();
        PermissionAPI permAPI = apiProvider.permissionAPI();
        CategoryAPI catAPI = apiProvider.categoryAPI();

        Preconditions.checkNotNull(params, "Workflow Action Params can't be null.");

        String translateToStr =
            params.get("translateTo") != null ? params.get("translateTo").getValue() : TRANSLATE_TO_DEFAULT;

        String fieldTypesStr =
            params.get("fieldTypes") != null ? params.get("fieldTypes").getValue() : FIELD_TYPES_DEFAULT;

        List<String> translateTo = Arrays.asList(translateToStr.split(" *(,| ) *"));
        List<String> fieldTypes = Arrays.asList(fieldTypesStr.split("\\s*(,|\\s)\\s*"));

        String ignoreFieldsStr =
            params.get("ignoreFields")!=null?params.get("ignoreFields").getValue():IGNORE_FIELDS_DEFAULT;

        List<String> ignoreFields = !Strings.isNullOrEmpty(ignoreFieldsStr)
            ? Arrays.asList(ignoreFieldsStr.split("\\s*(,|\\s)\\s*"))
            : new ArrayList<>();

        setServiceParameters(params);

        User user = processor.getUser();

        Contentlet sourceContentlet = processor.getContentlet();

        try {
            // Source content must be already persisted as Precondition. Let's check that out
            contentAPI
                .findContentletByIdentifier(sourceContentlet.getIdentifier(), false, sourceContentlet.getLanguageId(),
                    user, false);
        } catch (DotStateException e) {
            throw new WorkflowActionFailureException(
                "Unable to find source Content by identifier. Content must be persisted first.", e);
        } catch (Exception e) {
            throw new WorkflowActionFailureException("Error occurred trying to find Content by identifier.", e);
        }

        List<Field> translateFields = translationUtil.getFieldsOfContentlet(sourceContentlet, fieldTypes, ignoreFields);

        Preconditions.checkArgument(!translateFields.isEmpty(),
            "No Fields no translate. Please check the 'Translate Field Types' parameter in the actionlet config.");

        List<Language> translateLanguages = translationUtil.getLanguagesByLanguageCodes(translateTo);

        // let's remove the language of the source contentlet
        translateLanguages =
            translateLanguages.stream().filter(lang -> lang.getId() != sourceContentlet.getLanguageId())
                .collect(Collectors.toList());

        try {
            boolean live = sourceContentlet.isLive();

            List<Contentlet> translatedContents =
                translationService.translateContent(sourceContentlet, translateLanguages, translateFields, user);

            for (Contentlet translatedContent : translatedContents) {
                translatedContent.setProperty(Contentlet.DISABLE_WORKFLOW, true);

                sourceContentlet.setTags();
                copyBinariesAndTags(user, sourceContentlet, translatedContent);
                List<Category> cats = catAPI.getParents(sourceContentlet, user, true);
                ContentletRelationships rels = contentAPI.getAllRelationships(sourceContentlet);
                List<Permission> perms = permAPI.getPermissions(sourceContentlet, false, true);

                translatedContent = contentAPI.checkin(translatedContent, rels, cats, perms, user, false);

                if (live) {
                    contentAPI.publish(translatedContent, user, false);
                }
            }

            contentAPI.unlock(sourceContentlet, user, false);

        } catch (TranslationException e) {
            throw new WorkflowActionFailureException("Error executing Translation Actionlet", e);
        } catch (DotDataException | DotSecurityException e) {
            throw new WorkflowActionFailureException("Error saving translated content", e);
        }
    }

    private void setServiceParameters(Map<String, WorkflowActionClassParameter> actionParams) {
        if(translationService!=null) {
            List<ServiceParameter> serviceParams = translationService.getServiceParameters();

            if(serviceParams!=null) {
                for (ServiceParameter serviceParam : serviceParams) {
                    WorkflowActionClassParameter actionParam = actionParams.get(serviceParam.getKey());

                    if (actionParam != null && !Strings.isNullOrEmpty(actionParam.getValue())) {
                        serviceParam.setValue(actionParam.getValue());
                    }
                }

                translationService.setServiceParameters(serviceParams);
            }
        }
    }

    void copyBinariesAndTags(User user, Contentlet sourceContentlet, Contentlet translatedContent)
        throws DotDataException, DotSecurityException, TranslationException {
        Structure structure = translatedContent.getStructure();
        List<Field> list = FieldsCache.getFieldsByStructureInode(structure.getInode());
        for (Field field : list) {
            if (field.getFieldContentlet().startsWith("binary")) {
                java.io.File inputFile = APILocator
                    .getContentletAPI().getBinaryFile(sourceContentlet.getInode(), field.getVelocityVarName(), user);
                if (inputFile != null) {
                    java.io.File acopyFolder = new java.io.File(APILocator.getFileAPI().getRealAssetPathTmpBinary()
                        + java.io.File.separator + user.getUserId() + java.io.File.separator + field
                        .getFieldContentlet()
                        + java.io.File.separator + UUIDGenerator.generateUuid());

                    if (!acopyFolder.exists()) {
                        acopyFolder.mkdir();
                    }

                    String shortFileName = FileUtil.getShortFileName(inputFile.getAbsolutePath());

                    java.io.File binaryFile = new java.io.File(APILocator.getFileAPI().getRealAssetPathTmpBinary()
                        + java.io.File.separator + user.getUserId() + java.io.File.separator + field
                        .getFieldContentlet()
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