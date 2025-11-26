package com.dotmarketing.portlets.contentlet.business;

import static com.dotcms.content.business.json.ContentletJsonAPI.SAVE_CONTENTLET_AS_JSON;
import static com.dotcms.contenttype.model.type.BaseContentType.FILEASSET;
import static com.dotmarketing.business.APILocator.getContentTypeFieldAPI;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.VARIANT_ID;
import static java.io.File.separator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.api.system.event.ContentletSystemEventUtil;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl;
import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.JSONField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.PersonaDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.StructureDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestDataUtils.TestFile;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.TestWorkflowUtils;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.mock.request.MockInternalRequest;
import com.dotcms.rendering.velocity.services.VelocityResourceKey;
import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.model.Metadata;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotcms.uuid.shorty.ShortyIdCache;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.AssetUtil;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.contentlet.transform.ContentletTransformer;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.io.Files;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.awaitility.Awaitility;
import org.elasticsearch.action.search.SearchResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Created by Jonathan Gamba. Date: 3/20/12 Time: 12:12 PM
 */

@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentletAPITest extends ContentletBaseTest {

    @Test
    public void testDotAsset_Checkin() throws DotDataException, DotSecurityException, IOException {
        // 1) creates a dotasset for test
        final String variable = "testDotAsset" + System.currentTimeMillis();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        ContentType dotAssetContentType = contentTypeAPI
                .save(ContentTypeBuilder.builder(DotAssetContentType.class)
                        .folder(FolderAPI.SYSTEM_FOLDER)
                        .host(Host.SYSTEM_HOST).name(variable)
                        .owner(user.getUserId()).build());
        final Map<String, com.dotcms.contenttype.model.field.Field> fieldMap = dotAssetContentType.fieldMap();
        com.dotcms.contenttype.model.field.Field binaryField = fieldMap.get(
                DotAssetContentType.ASSET_FIELD_VAR);
        final FieldVariable allowFileTypes = ImmutableFieldVariable.builder()
                .key(BinaryField.ALLOWED_FILE_TYPES)
                .value("application/*, text/*").fieldId(binaryField.id()).build();
        binaryField.constructFieldVariables(Arrays.asList(allowFileTypes));

        dotAssetContentType = contentTypeAPI.save(dotAssetContentType);
        binaryField = fieldAPI.save(binaryField, user);
        fieldAPI.save(allowFileTypes, user);

        final File tempTestFile = File
                .createTempFile("fileTest_" + new Date().getTime(), ".txt");
        FileUtils.writeStringToFile(tempTestFile, "Test hi this a test longer than ten characters");

        Contentlet dotAssetContentlet = new Contentlet();
        dotAssetContentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());
        dotAssetContentlet.setModUser(user.getUserId());
        dotAssetContentlet.setHost(APILocator.systemHost().getIdentifier());
        dotAssetContentlet.setStringProperty(Contentlet.BASE_TYPE_KEY,
                BaseContentType.DOTASSET.getAlternateName());
        dotAssetContentlet.setBinary(binaryField, tempTestFile);
        dotAssetContentlet.setIndexPolicy(IndexPolicy.FORCE);
        dotAssetContentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);
        dotAssetContentlet = contentletAPI.checkin(dotAssetContentlet, user, false);
        dotAssetContentlet = contentletAPI.find(dotAssetContentlet.getInode(), user, false);

        assertNotNull(dotAssetContentlet);
        assertEquals("The Content Type should be: " + variable,
                dotAssetContentType.variable(), dotAssetContentlet.getContentType().variable());

        final String contentletTitle = dotAssetContentlet.getTitle();

        assertEquals("the contentlet title should be the binary field name", contentletTitle,
                tempTestFile.getName());

        dotAssetContentlet.getMap().remove(Contentlet.TITTLE_KEY);

        final String contentletTitle2 = dotAssetContentlet.getTitle();

        assertEquals("the contentlet title should be the binary field name", contentletTitle2,
                contentletTitle);
    }

    @Test
    public void testCheckinDefaultActionsSkipBySettingActionId()
            throws DotDataException, DotSecurityException {

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        Contentlet contentletToDestroy = null;
        ContentType type = null;

        try {

            //1) create a content type
            final String velocityContentTypeName =
                    "DefaultActionContentTypeSkip" + System.currentTimeMillis();
            type = contentTypeAPI.save(
                    ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                            .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER)
                            .host(Host.SYSTEM_HOST)
                            .name("DefaultActionContentTypeSkip")
                            .owner(APILocator.systemUser().toString())
                            .variable(velocityContentTypeName).build());

            final List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>(
                    type.fields());

            fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                    .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
            fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                    .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());

            final ContentType contentTypeSaved = contentTypeAPI.save(type, fields);

            final WorkflowScheme systemWorkflow = workflowAPI.findSystemWorkflowScheme();
            workflowAPI.saveSchemeIdsForContentType(contentTypeSaved,
                    CollectionsUtils.set(systemWorkflow.getId()));
            final List<WorkflowScheme> contentTypeSchemes = workflowAPI.findSchemesForContentType(
                    contentTypeSaved);
            Assert.assertNotNull(contentTypeSchemes);
            Assert.assertEquals(1, contentTypeSchemes.size());

            //2) associated system workflow to the scheme
            final WorkflowAction saveWorkflowAction = workflowAPI.findAction(
                    SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID, APILocator.systemUser());
            Assert.assertNotNull(saveWorkflowAction);
            final SystemActionWorkflowActionMapping mapping = workflowAPI.mapSystemActionToWorkflowActionForContentType
                    (WorkflowAPI.SystemAction.NEW, saveWorkflowAction, contentTypeSaved);
            Logger.info(this, "mapping: " + mapping);
            final SystemActionWorkflowActionMapping mappingEdit = workflowAPI.mapSystemActionToWorkflowActionForContentType
                    (WorkflowAPI.SystemAction.EDIT, saveWorkflowAction, contentTypeSaved);
            Logger.info(this, "mappingEdit: " + mappingEdit);

            //3) map the save content to the new
            final Contentlet contentlet = new Contentlet();
            final User user = APILocator.systemUser();
            contentlet.setContentTypeId(type.id());
            contentlet.setOwner(APILocator.systemUser().toString());
            contentlet.setModDate(new Date());
            contentlet.setLanguageId(1);
            contentlet.setStringProperty("title", "Test Save");
            contentlet.setStringProperty("txt", "Test Save Text");
            contentlet.setHost(Host.SYSTEM_HOST);
            contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);
            contentlet.setIndexPolicy(IndexPolicy.FORCE);

            // save
            contentlet.setActionId(SystemWorkflowConstants.WORKFLOW_SAVE_PUBLISH_ACTION_ID);
            final Contentlet contentlet1 = contentletAPI.checkin(contentlet, user, false);
            contentletToDestroy = contentlet1;

            //4) check the contentlet is on the step unpublish
            final WorkflowStep unpublishStep = workflowAPI.findStepByContentlet(contentlet1);
            Assert.assertEquals(SystemWorkflowConstants.WORKFLOW_PUBLISHED_STEP_ID,
                    unpublishStep.getId());
        } finally {

            try {
                if (null != contentletToDestroy) {
                    try {
                        this.contentletAPI.destroy(contentletToDestroy, user, false);
                    } catch (Exception e) {
                        // quiet
                    }
                }

                if (null != type) {
                    try {
                        contentTypeAPI.delete(type);
                    } catch (Exception e) {
                        // quiet
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testCheckinDefaultActionsSkip() throws DotDataException, DotSecurityException {

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        Contentlet contentletToDestroy = null;
        ContentType type = null;

        try {

            //1) create a content type
            final String velocityContentTypeName =
                    "DefaultActionContentTypeSkip" + System.currentTimeMillis();
            type = contentTypeAPI.save(
                    ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                            .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER)
                            .host(Host.SYSTEM_HOST)
                            .name("DefaultActionContentTypeSkip")
                            .owner(APILocator.systemUser().toString())
                            .variable(velocityContentTypeName).build());

            final List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>(
                    type.fields());

            fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                    .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
            fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                    .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());

            final ContentType contentTypeSaved = contentTypeAPI.save(type, fields);

            final WorkflowScheme systemWorkflow = workflowAPI.findSystemWorkflowScheme();
            workflowAPI.saveSchemeIdsForContentType(contentTypeSaved,
                    CollectionsUtils.set(systemWorkflow.getId()));
            final List<WorkflowScheme> contentTypeSchemes = workflowAPI.findSchemesForContentType(
                    contentTypeSaved);
            Assert.assertNotNull(contentTypeSchemes);
            Assert.assertEquals(1, contentTypeSchemes.size());

            //2) associated system workflow to the scheme
            final WorkflowAction saveWorkflowAction = workflowAPI.findAction(
                    SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID, APILocator.systemUser());
            Assert.assertNotNull(saveWorkflowAction);
            final SystemActionWorkflowActionMapping mapping = workflowAPI.mapSystemActionToWorkflowActionForContentType
                    (WorkflowAPI.SystemAction.NEW, saveWorkflowAction, contentTypeSaved);
            Logger.info(this, "mapping: " + mapping);
            final SystemActionWorkflowActionMapping mappingEdit = workflowAPI.mapSystemActionToWorkflowActionForContentType
                    (WorkflowAPI.SystemAction.EDIT, saveWorkflowAction, contentTypeSaved);
            Logger.info(this, "mappingEdit: " + mappingEdit);

            //3) map the save content to the new
            final Contentlet contentlet = new Contentlet();
            final User user = APILocator.systemUser();
            contentlet.setContentTypeId(type.id());
            contentlet.setOwner(APILocator.systemUser().toString());
            contentlet.setModDate(new Date());
            contentlet.setLanguageId(1);
            contentlet.setStringProperty("title", "Test Save");
            contentlet.setStringProperty("txt", "Test Save Text");
            contentlet.setHost(Host.SYSTEM_HOST);
            contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);
            contentlet.setIndexPolicy(IndexPolicy.FORCE);

            // save
            contentlet.setProperty(Contentlet.DISABLE_WORKFLOW, true);
            final Contentlet contentlet1 = contentletAPI.checkin(contentlet, user, false);
            contentletToDestroy = contentlet1;

            //4) check the contentlet is on the step unpublish
            final WorkflowStep unpublishStep = workflowAPI.findStepByContentlet(contentlet1);
            Assert.assertEquals(SystemWorkflowConstants.WORKFLOW_NEW_STEP_ID,
                    unpublishStep.getId());
        } finally {

            try {
                if (null != contentletToDestroy) {
                    try {
                        this.contentletAPI.destroy(contentletToDestroy, user, false);
                    } catch (Exception e) {
                        // quiet
                    }
                }

                if (null != type) {
                    try {
                        contentTypeAPI.delete(type);
                    } catch (Exception e) {
                        // quiet
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testCheckinDefaultActions() throws DotDataException, DotSecurityException {

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        Contentlet contentletToDestroy = null;
        ContentType type = null;

        try {

            //1) create a content type
            final String velocityContentTypeName = "DefaultActionContentType";
            type = contentTypeAPI.save(
                    ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                            .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER)
                            .host(Host.SYSTEM_HOST)
                            .name("DefaultActionContentType")
                            .owner(APILocator.systemUser().toString())
                            .variable(velocityContentTypeName).build());

            final List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>(
                    type.fields());

            fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                    .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
            fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                    .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());

            final ContentType contentTypeSaved = contentTypeAPI.save(type, fields);

            final WorkflowScheme systemWorkflow = workflowAPI.findSystemWorkflowScheme();
            workflowAPI.saveSchemeIdsForContentType(contentTypeSaved,
                    CollectionsUtils.set(systemWorkflow.getId()));
            final List<WorkflowScheme> contentTypeSchemes = workflowAPI.findSchemesForContentType(
                    contentTypeSaved);
            Assert.assertNotNull(contentTypeSchemes);
            Assert.assertEquals(1, contentTypeSchemes.size());

            //2) associated system workflow to the scheme
            final WorkflowAction saveWorkflowAction = workflowAPI.findAction(
                    SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID, APILocator.systemUser());
            Assert.assertNotNull(saveWorkflowAction);
            final SystemActionWorkflowActionMapping mapping = workflowAPI.mapSystemActionToWorkflowActionForContentType
                    (WorkflowAPI.SystemAction.NEW, saveWorkflowAction, contentTypeSaved);
            Logger.info(this, "mapping: " + mapping);
            final SystemActionWorkflowActionMapping mappingEdit = workflowAPI.mapSystemActionToWorkflowActionForContentType
                    (WorkflowAPI.SystemAction.EDIT, saveWorkflowAction, contentTypeSaved);
            Logger.info(this, "mappingEdit: " + mappingEdit);

            //3) map the save content to the new
            final Contentlet contentlet = new Contentlet();
            final User user = APILocator.systemUser();
            contentlet.setContentTypeId(type.id());
            contentlet.setOwner(APILocator.systemUser().toString());
            contentlet.setModDate(new Date());
            contentlet.setLanguageId(1);
            contentlet.setStringProperty("title", "Test Save");
            contentlet.setStringProperty("txt", "Test Save Text");
            contentlet.setHost(Host.SYSTEM_HOST);
            contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);
            contentlet.setIndexPolicy(IndexPolicy.FORCE);

            // save
            final Contentlet contentlet1 = contentletAPI.checkin(contentlet, user, false);
            contentletToDestroy = contentlet1;

            //4) check the contentlet is on the step unpublish
            final WorkflowStep unpublishStep = workflowAPI.findStepByContentlet(contentlet1);
            Assert.assertEquals(SystemWorkflowConstants.WORKFLOW_UNPUBLISHED_STEP_ID,
                    unpublishStep.getId());
        } finally {

            try {
                if (null != contentletToDestroy) {
                    try {
                        this.contentletAPI.destroy(contentletToDestroy, user, false);
                    } catch (Exception e) {
                        // quiet
                    }
                }

                if (null != type) {
                    try {
                        contentTypeAPI.delete(type);
                    } catch (Exception e) {
                        // quiet
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testCheckinNoDefaultActions() throws DotDataException, DotSecurityException {

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        Contentlet contentletToDestroy = null;
        ContentType type = null;

        try {

            //1) create a content type
            final String velocityContentTypeName = "NoDefaultActionContentType";
            type = contentTypeAPI.save(
                    ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                            .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER)
                            .host(Host.SYSTEM_HOST)
                            .name("NoDefaultActionContentType")
                            .owner(APILocator.systemUser().toString())
                            .variable(velocityContentTypeName).build());

            final List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>(
                    type.fields());

            fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                    .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
            fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                    .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());

            final ContentType contentTypeSaved = contentTypeAPI.save(type, fields);

            final WorkflowScheme systemWorkflow = TestWorkflowUtils.getDocumentWorkflow(
                    "TestWF" + System.currentTimeMillis());
            workflowAPI.saveSchemeIdsForContentType(contentTypeSaved,
                    CollectionsUtils.set(systemWorkflow.getId()));
            final List<WorkflowScheme> contentTypeSchemes = workflowAPI.findSchemesForContentType(
                    contentTypeSaved);
            Assert.assertNotNull(contentTypeSchemes);
            Assert.assertEquals(1, contentTypeSchemes.size());

            //2) associated system workflow to the scheme
            final WorkflowAction saveWorkflowAction = workflowAPI.findAction(
                    SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID, APILocator.systemUser());
            Assert.assertNotNull(saveWorkflowAction);

            //3) map the save content to the new
            final Contentlet contentlet = new Contentlet();
            final User user = APILocator.systemUser();
            contentlet.setContentTypeId(type.id());
            contentlet.setOwner(APILocator.systemUser().toString());
            contentlet.setModDate(new Date());
            contentlet.setLanguageId(1);
            contentlet.setStringProperty("title", "Test Save");
            contentlet.setStringProperty("txt", "Test Save Text");
            contentlet.setHost(Host.SYSTEM_HOST);
            contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);
            contentlet.setIndexPolicy(IndexPolicy.FORCE);

            // save
            final Contentlet contentlet1 = contentletAPI.checkin(contentlet, user, false);
            contentletToDestroy = contentlet1;

            //4) check the contentlet is on the step unpublish
            final WorkflowStep firstStep = workflowAPI.findFirstStep(systemWorkflow.getId()).get();
            final WorkflowStep currentStep = workflowAPI.findStepByContentlet(contentlet1);
            Assert.assertEquals(firstStep.getId(), currentStep.getId());
        } finally {

            try {
                if (null != contentletToDestroy) {
                    try {
                        this.contentletAPI.destroy(contentletToDestroy, user, false);
                    } catch (Exception e) {
                        // quiet
                    }
                }

                if (null != type) {
                    try {
                        contentTypeAPI.delete(type);
                    } catch (Exception e) {
                        // quiet
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Testing {@link ContentletAPI#findAllContent(int, int)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Ignore("Not Ready to Run.")
    @Test
    public void findAllContent() throws DotDataException, DotSecurityException {

        //Getting all contentlets live/working contentlets
        List<Contentlet> contentlets = contentletAPI.findAllContent(0, 5);

        //Validations
        assertTrue(contentlets != null && !contentlets.isEmpty());
        assertEquals(contentlets.size(), 5);

        //Validate the integrity of the array
        Contentlet contentlet = contentletAPI.find(contentlets.iterator().next().getInode(), user,
                false);

        //Validations
        assertTrue(contentlet != null && (contentlet.getInode() != null && !contentlet.getInode()
                .isEmpty()));
    }

    @Test
    public void test_invalidate_shorty_cache() throws DotDataException, DotSecurityException {

        Contentlet contentletToDestroy = null;
        ContentType type = null;
        try {
            final String velocityContentTypeName = "InvalidateShortyCacheContentType";
            type = contentTypeAPI.save(
                    ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                            .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER)
                            .host(Host.SYSTEM_HOST)
                            .name("InvalidateShortyCache").owner(APILocator.systemUser().toString())
                            .variable(velocityContentTypeName).build());

            final List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>(
                    type.fields());

            fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                    .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
            fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                    .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());

            contentTypeAPI.save(type, fields);

            final Contentlet contentlet = new Contentlet();
            final User user = APILocator.systemUser();
            contentlet.setContentTypeId(type.id());
            contentlet.setOwner(APILocator.systemUser().toString());
            contentlet.setModDate(new Date());
            contentlet.setLanguageId(1);
            contentlet.setStringProperty("title", "Test Save");
            contentlet.setStringProperty("txt", "Test Save Text");
            contentlet.setHost(Host.SYSTEM_HOST);
            contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);
            contentlet.setIndexPolicy(IndexPolicy.FORCE);

            // first save
            final ShortyIdAPI shortyIdAPI = APILocator.getShortyAPI();
            final Contentlet contentlet1 = contentletAPI.checkin(contentlet, user, false);
            contentletToDestroy = contentlet1;
            final Optional<ShortyId> shortyId = shortyIdAPI.getShorty(contentlet1.getIdentifier());
            Assert.assertTrue(shortyId.isPresent());
            Assert.assertTrue(new ShortyIdCache().get(shortyId.get().shortId).isPresent());
            final Contentlet contentletCheckout = contentletAPI.checkout(contentlet1.getInode(),
                    user, false);
            final String inode = contentletCheckout.getInode();
            this.contentletAPI.copyProperties(contentletCheckout, contentlet.getMap());
            contentletCheckout.setIdentifier(contentlet1.getIdentifier());
            contentletCheckout.setInode(inode);
            contentletAPI.checkin(contentletCheckout, user, false);
            Assert.assertFalse(new ShortyIdCache().get(shortyId.get().shortId).isPresent());
        } finally {

            try {
                if (null != contentletToDestroy) {
                    try {
                        this.contentletAPI.destroy(contentletToDestroy, user, false);
                    } catch (Exception e) {
                        // quiet
                    }
                }

                if (null != type) {
                    try {
                        contentTypeAPI.delete(type);
                    } catch (Exception e) {
                        // quiet
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Ignore("Not Ready to Run.")
    @Test
    public void run_listener_after_save_result_called_all_listeners()
            throws DotDataException, DotSecurityException {

        ContentType typeResult = null;
        final int numberOfContents = 1000;//20000;
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfContents);
        DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance()
                .getSubmitter(DotConcurrentFactory.DOT_SYSTEM_THREAD_POOL);
        try {

            final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final String velocityContentTypeName = "RunListenerAfterSaveTest8";

            LocalTransaction.wrap(() -> {

                final ContentType type = contentTypeAPI.save(
                        ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                                .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER)
                                .host(Host.SYSTEM_HOST)
                                .name("RunListenerAfterSaveTest")
                                .owner(APILocator.systemUser().toString())
                                .variable(velocityContentTypeName).build());

                final List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>(
                        type.fields());

                fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                        .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
                fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                        .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());

                contentTypeAPI.save(type, fields);
            });

            final ContentType type = contentTypeAPI.find(velocityContentTypeName);
            final List<Future> futures = new ArrayList<>();

            for (int i = 0; i < numberOfContents; ++i) {

                futures.add(dotSubmitter.submit(() -> {

                    try {
                        List<Contentlet> contentlets =
                                APILocator.getContentletAPI()
                                        .search("+type:" + velocityContentTypeName, 1000, 0, null,
                                                APILocator.systemUser(), false);
                        if (UtilMethods.isSet(contentlets)) {

                            for (final Contentlet contentlet1 : contentlets) {
                                APILocator.getContentletAPI()
                                        .find(contentlet1.getInode(), APILocator.systemUser(),
                                                false);
                            }
                        }
                    } catch (DotDataException | DotSecurityException e) {
                        e.printStackTrace();
                    }
                }));

                LocalTransaction.wrapReturnWithListeners(() -> {

                    final Contentlet contentlet = new Contentlet();
                    final User user = APILocator.systemUser();
                    contentlet.setContentTypeId(type.id());
                    contentlet.setOwner(APILocator.systemUser().toString());
                    contentlet.setModDate(new Date());
                    contentlet.setLanguageId(languageId);
                    contentlet.setStringProperty("title", "Test Save");
                    contentlet.setStringProperty("txt", "Test Save Text");
                    contentlet.setHost(Host.SYSTEM_HOST);
                    contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);
                    contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);

                    // first save
                    final Contentlet contentlet1 = contentletAPI.checkin(contentlet, user, false);
                    if (null != contentlet1) {
                        HibernateUtil.addCommitListener(() -> {

                            try {
                                assertTrue(APILocator.getContentletAPI()
                                        .indexCount("+inode:" + contentlet1.getInode(), user, false)
                                        > 0);
                            } catch (DotDataException | DotSecurityException e) {
                                fail(e.getMessage());
                            }

                            if (APILocator.getContentletAPI()
                                    .isInodeIndexed(contentlet1.getInode())) {
                                ContentletSystemEventUtil.getInstance()
                                        .pushSaveEvent(contentlet1, true);
                            }

                            countDownLatch.countDown();
                        }, 1000);
                    }

                    return null;
                });
            }

            for (final Future future : futures) {

                future.get();
                countDownLatch.await(1, TimeUnit.SECONDS);
            }

            if (countDownLatch.getCount() > 0) {

                countDownLatch.await(30, TimeUnit.SECONDS);
            }

            assertEquals(0, countDownLatch.getCount());

            typeResult = type;
        } catch (Exception e) {

            fail(e.getMessage());
        } finally {

            if (null != typeResult) {

                ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(
                        APILocator.systemUser());
                contentTypeAPI.delete(typeResult);
            }
        }

    }


    /**
     * Testing {@link ContentletAPI#find(String, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void find() throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Search the contentlet
        Contentlet foundContentlet = contentletAPI.find(contentlet.getInode(), user, false);

        //Validations
        assertNotNull(foundContentlet);
        assertEquals(foundContentlet.getInode(), contentlet.getInode());
    }

    /**
     * Testing
     * {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User,
     * boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void test_findContentletByIdentifierOrFallback_non_existing_DotContentletStateException_expected()
            throws DotDataException, DotSecurityException {

        assertFalse(contentletAPI.findContentletByIdentifierOrFallback("noexisting", false, 1, user,
                false).isPresent());
    }

    /**
     * Testing
     * {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User,
     * boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_DotContentletStateException_expected_true()
            throws DotDataException, DotSecurityException {

        //Getting our test contentlet
        Contentlet contentletWidget = TestDataUtils
                .getWidgetContent(true, languageAPI.getDefaultLanguage().getId(),
                        simpleWidgetContentType.id());
        assertNotNull(contentletWidget);

        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI()
                        .findContentletByIdentifierOrFallback(contentletWidget.getIdentifier(),
                                false, spanishLanguage.getId(), user, false);

        assertTrue(optionalContentlet.isPresent());
        assertEquals(languageAPI.getDefaultLanguage().getId(),
                optionalContentlet.get().getLanguageId());
    }

    /**
     * Testing
     * {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User,
     * boolean)}
     * <p>
     * English version, no Spanish, fallback true, request Spanish -> Return english
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_English_version_no_Spanish_fallback_false_expecting_false()
            throws DotDataException, DotSecurityException {

        //Getting our test contentlet
        Contentlet genericContent = TestDataUtils.getGenericContentContent(true, 1);

        assertNotNull(genericContent);
        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI()
                        .findContentletByIdentifierOrFallback(genericContent.getIdentifier(),
                                false, spanishLanguage.getId(), user, false);

        assertFalse(optionalContentlet.isPresent());
    }

    /**
     * Testing
     * {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User,
     * boolean)}
     * <p>
     * English version, with Spanish, fallback true, request Spanish -> Return Spanish
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_EnglishSpanish_fallback_true_expecting_Spanish_true()
            throws DotDataException, DotSecurityException {

        //Getting our test contentlet
        Contentlet contentletWidget = TestDataUtils.getPageContent(true, spanishLanguage.getId());
        assertNotNull(contentletWidget);

        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI()
                        .findContentletByIdentifierOrFallback(contentletWidget.getIdentifier(),
                                false, spanishLanguage.getId(), user, false);

        assertTrue(optionalContentlet.isPresent());
        assertEquals(contentletWidget.getInode(), optionalContentlet.get().getInode());
    }

    /**
     * Testing
     * {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User,
     * boolean)}
     * <p>
     * English version, no Spanish, fallback false, request Spanish -> 404
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_English_No_Spanish_fallback_false_expecting_False()
            throws DotDataException, DotSecurityException {

        //Getting our test contentlet
        Contentlet newsContentlet = TestDataUtils
                .getNewsContent(true, languageAPI.getDefaultLanguage().getId(),
                        newsContentType.id());

        assertNotNull(newsContentlet);
        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI()
                        .findContentletByIdentifierOrFallback(newsContentlet.getIdentifier(),
                                false,
                                spanishLanguage.getId(), user, false);

        assertFalse(optionalContentlet.isPresent());
    }

    /**
     * Testing
     * {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User,
     * boolean)}
     * <p>
     * English version, with Spanish, fallback false, request Spanish -> Return Spanish
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_EnglishSpanish_fallback_false_expecting_Spanish_true()
            throws DotDataException, DotSecurityException {

        //Getting our test contentlet
        Contentlet newsContentlet = TestDataUtils
                .getNewsContent(true, languageAPI.getDefaultLanguage().getId(),
                        newsContentType.id());
        assertNotNull(newsContentlet);

        final Contentlet checkoutContentlet =
                APILocator.getContentletAPI()
                        .checkout(newsContentlet.getInode(), user, false);
        assertNotNull(checkoutContentlet);
        checkoutContentlet.setIdentifier(newsContentlet.getIdentifier());
        checkoutContentlet.setLanguageId(spanishLanguage.getId()); // spanish
        checkoutContentlet.setIndexPolicy(IndexPolicy.FORCE);

        Contentlet spanishNewsContentlet =
                APILocator.getContentletAPI()
                        .checkin(checkoutContentlet, user, false);

        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI()
                        .findContentletByIdentifierOrFallback(newsContentlet.getIdentifier(),
                                false, spanishLanguage.getId(), user, false);

        assertTrue(optionalContentlet.isPresent());
        assertEquals(spanishNewsContentlet.getInode(), optionalContentlet.get().getInode());
    }

    /**
     * Testing
     * {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User,
     * boolean)}
     * <p>
     * No English version, with Spanish, fallback true, request Spanish -> Return Spanish
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_NonEnglish_WithSpanish_fallback_true_expecting_Spanish_true() {

        //Getting our test contentlet
        Contentlet spanishGenericContentContentlet = TestDataUtils.getFileAssetContent(true,
                spanishLanguage.getId());

        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI().findContentletByIdentifierOrFallback(
                        spanishGenericContentContentlet.getIdentifier(), false,
                        spanishLanguage.getId(), user, false);

        assertTrue(optionalContentlet.isPresent());
        assertEquals(spanishGenericContentContentlet.getInode(),
                optionalContentlet.get().getInode());
    }

    /**
     * Testing
     * {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User,
     * boolean)}
     * <p>
     * No English version, with Spanish, fallback false, request Spanish -> Return Spanish
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_NonEnglish_WithSpanish_fallback_false_expecting_Spanish_true()
            throws DotDataException, DotSecurityException, IOException {

        //Getting our test contentlet
        Contentlet spanishGenericContentContentlet = TestDataUtils.getGenericContentContent(true,
                spanishLanguage.getId());

        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI().findContentletByIdentifierOrFallback(
                        spanishGenericContentContentlet.getIdentifier(), false,
                        spanishLanguage.getId(), user, false);

        assertTrue(optionalContentlet.isPresent());
        assertEquals(spanishGenericContentContentlet.getInode(),
                optionalContentlet.get().getInode());
    }

    /**
     * Testing
     * {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User,
     * boolean)}
     * <p>
     * English version LIVE, with Spanish WORKING, fallback true, request Spanish WORKING -> Return
     * Spanish WORKING
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_EnglishLive_WithSpanishWorking_fallback_true_expecting_SpanishWorking_true()
            throws DotDataException, DotSecurityException, IOException {

        //Getting our test contentlet
        Contentlet fileAssetContentletEngLive = TestDataUtils.getFileAssetContent(true,
                languageAPI.getDefaultLanguage().getId());

        assertNotNull(fileAssetContentletEngLive);
        Contentlet spanishFileAssetContentletWorking = new Contentlet(); // no eng version
        APILocator.getContentletAPI().copyProperties(spanishFileAssetContentletWorking,
                fileAssetContentletEngLive.getMap());
        spanishFileAssetContentletWorking.setHost(fileAssetContentletEngLive.getHost());
        spanishFileAssetContentletWorking
                .setContentType(fileAssetContentletEngLive.getContentType());
        spanishFileAssetContentletWorking.setIdentifier(fileAssetContentletEngLive.getIdentifier());
        spanishFileAssetContentletWorking.setInode(null);
        spanishFileAssetContentletWorking.setIndexPolicy(IndexPolicy.FORCE);
        spanishFileAssetContentletWorking.setLanguageId(spanishLanguage.getId()); // spanish
        spanishFileAssetContentletWorking.setProperty("title", "Spanish Main.scss");

        final File file = fileAssetContentletEngLive.getBinary(FileAssetAPI.BINARY_FIELD);
        final File copy = new File(org.apache.commons.io.FileUtils.getTempDirectory(),
                UUIDGenerator.generateUuid());
        org.apache.commons.io.FileUtils.copyFile(file, copy);
        spanishFileAssetContentletWorking.setBinary(FileAssetAPI.BINARY_FIELD, copy);

        spanishFileAssetContentletWorking =
                APILocator.getContentletAPI()
                        .checkin(spanishFileAssetContentletWorking, user, false);

        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI().findContentletByIdentifierOrFallback(
                        fileAssetContentletEngLive.getIdentifier(), false,
                        spanishLanguage.getId(), user, false);

        assertTrue(optionalContentlet.isPresent());
        assertEquals(spanishFileAssetContentletWorking.getInode(),
                optionalContentlet.get().getInode());
    }

    /**
     * Testing
     * {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User,
     * boolean)}
     * <p>
     * English version LIVE, with Spanish WORKING, fallback true, request Spanish LIVE -> Return
     * English
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_EnglishLive_WithSpanishWorking_fallback_true_expecting_English_true()
            throws DotDataException, DotSecurityException, IOException {

        //Getting our test contentlet
        Contentlet fileAssetContentletEngLive = TestDataUtils.getFileAssetContent(true,
                languageAPI.getDefaultLanguage().getId());

        assertNotNull(fileAssetContentletEngLive);
        Contentlet spanishFileAssetContentletWorking = new Contentlet(); // no eng version
        APILocator.getContentletAPI().copyProperties(spanishFileAssetContentletWorking,
                fileAssetContentletEngLive.getMap());
        spanishFileAssetContentletWorking.setIdentifier(fileAssetContentletEngLive.getIdentifier());
        spanishFileAssetContentletWorking.setHost(fileAssetContentletEngLive.getHost());
        spanishFileAssetContentletWorking
                .setContentType(fileAssetContentletEngLive.getContentType());
        spanishFileAssetContentletWorking.setInode(null);
        spanishFileAssetContentletWorking.setIndexPolicy(IndexPolicy.FORCE);
        spanishFileAssetContentletWorking.setLanguageId(spanishLanguage.getId()); // spanish
        spanishFileAssetContentletWorking.setProperty("title", "Spanish Main.scss");

        final File file = fileAssetContentletEngLive.getBinary(FileAssetAPI.BINARY_FIELD);
        final File copy = new File(org.apache.commons.io.FileUtils.getTempDirectory(),
                UUIDGenerator.generateUuid());
        org.apache.commons.io.FileUtils.copyFile(file, copy);
        spanishFileAssetContentletWorking.setBinary(FileAssetAPI.BINARY_FIELD, copy);

        spanishFileAssetContentletWorking =
                APILocator.getContentletAPI()
                        .checkin(spanishFileAssetContentletWorking, user, false);

        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI().findContentletByIdentifierOrFallback(
                        fileAssetContentletEngLive.getIdentifier(), true,
                        spanishLanguage.getId(), user, false);

        assertTrue(optionalContentlet.isPresent());
        assertNotEquals(spanishFileAssetContentletWorking.getInode(),
                optionalContentlet.get().getInode());
        assertEquals(fileAssetContentletEngLive.getInode(), optionalContentlet.get().getInode());
    }

    /**
     * Testing
     * {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User,
     * boolean)}
     * <p>
     * Only Spanish WORKING, fallback false, request English Working -> Return Empty
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_OnlySpanishWorking_fallback_true_expecting_English_true()
            throws DotDataException, DotSecurityException, IOException {

        //Getting our test contentlet
        Contentlet newsContentletEng = TestDataUtils
                .getNewsContent(true, languageAPI.getDefaultLanguage().getId(),
                        newsContentType.id());

        assertNotNull(newsContentletEng);
        Contentlet spanishNewNewsContentlet = new Contentlet(); // no eng version
        APILocator.getContentletAPI()
                .copyProperties(spanishNewNewsContentlet, newsContentletEng.getMap());
        spanishNewNewsContentlet.setHost(newsContentletEng.getHost());
        spanishNewNewsContentlet.setContentType(newsContentletEng.getContentType());
        spanishNewNewsContentlet.setIdentifier(null);
        spanishNewNewsContentlet.setInode(null);
        spanishNewNewsContentlet.setIndexPolicy(IndexPolicy.FORCE);
        spanishNewNewsContentlet.setLanguageId(spanishLanguage.getId()); // spanish
        spanishNewNewsContentlet.setProperty("title", "Spanish Test2");
        spanishNewNewsContentlet.setProperty("urlTitle", "/news/spanish_test2");
        spanishNewNewsContentlet
                .setProperty("byline", newsContentletEng.getStringProperty("byline"));
        spanishNewNewsContentlet
                .setProperty("sysPublishDate", newsContentletEng.getMap().get("sysPublishDate"));
        spanishNewNewsContentlet.setProperty("story", newsContentletEng.getMap().get("story"));

        spanishNewNewsContentlet =
                APILocator.getContentletAPI()
                        .checkin(spanishNewNewsContentlet, user, false);

        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI().findContentletByIdentifierOrFallback(
                        spanishNewNewsContentlet.getIdentifier(), false, 1, user, false);

        assertFalse(optionalContentlet.isPresent());
    }

    /**
     * Testing
     * {@link ContentletAPI#findContentletForLanguage(long, com.dotmarketing.beans.Identifier)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentletForLanguage() throws DotDataException, DotSecurityException {

        //Getting our test contentlet
        Contentlet contentletWithLanguage = null;
        for (Contentlet contentlet : contentlets) {

            //Verify if we have a contentlet with a language set
            if (contentlet.getLanguageId() != 0) {
                contentletWithLanguage = contentlet;
                break;
            }
        }

        Identifier contentletIdentifier = APILocator.getIdentifierAPI()
                .find(contentletWithLanguage);

        //Search the contentlet
        assertNotNull(contentletWithLanguage);
        Contentlet foundContentlet = contentletAPI.findContentletForLanguage(
                contentletWithLanguage.getLanguageId(), contentletIdentifier);

        //Validations
        assertNotNull(foundContentlet);
        assertEquals(foundContentlet.getInode(), contentletWithLanguage.getInode());
    }

    /**
     * Testing
     * {@link ContentletAPI#findByStructure(com.dotmarketing.portlets.structure.model.Structure,
     * com.liferay.portal.model.User, boolean, int, int)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findByStructure() throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Search the contentlet
        List<Contentlet> foundContentlets = contentletAPI.findByStructure(contentlet.getStructure(),
                user, false, 0, 0);

        //Validations
        assertTrue(foundContentlets != null && !foundContentlets.isEmpty());
    }

    /**
     * Testing
     * {@link ContentletAPI#findByStructure(String, com.liferay.portal.model.User, boolean, int,
     * int)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findByStructureInode() throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Search the contentlets
        List<Contentlet> foundContentlets = contentletAPI.findByStructure(
                contentlet.getStructureInode(), user, false, 0, 0);

        //Validations
        assertTrue(foundContentlets != null && !foundContentlets.isEmpty());
    }

    /**
     * Testing
     * {@link ContentletAPI#findContentletByIdentifier(String, boolean, long,
     * com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentletByIdentifier() throws DotSecurityException, DotDataException {

        //Getting our test contentlet
        Contentlet contentletWithLanguage = null;
        for (Contentlet contentlet : contentlets) {

            //Verify if we have a contentlet with a language set
            if (contentlet.getLanguageId() != 0) {
                contentletWithLanguage = contentlet;
                break;
            }
        }

        //Search the contentlet
        assertNotNull(contentletWithLanguage);
        Contentlet foundContentlet = contentletAPI.findContentletByIdentifier(
                contentletWithLanguage.getIdentifier(), false,
                contentletWithLanguage.getLanguageId(), user, false);

        //Validations
        assertNotNull(foundContentlet);
        assertEquals(foundContentlet.getInode(), contentletWithLanguage.getInode());
    }

    /**
     * Testing
     * {@link ContentletAPI#findContentletsByIdentifiers(String[], boolean, long,
     * com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentletByIdentifiers() throws DotSecurityException, DotDataException {

        //Getting our test contentlet
        Contentlet contentletWithLanguage = null;
        for (Contentlet contentlet : contentlets) {

            //Verify if we have a contentlet with a language set
            if (contentlet.getLanguageId() != 0) {
                contentletWithLanguage = contentlet;
                break;
            }
        }

        //Search the contentlet
        assertNotNull(contentletWithLanguage);
        List<Contentlet> foundContentlets = contentletAPI.findContentletsByIdentifiers(
                new String[]{contentletWithLanguage.getIdentifier()}, false,
                contentletWithLanguage.getLanguageId(), user, false);

        //Validations
        assertTrue(foundContentlets != null && !foundContentlets.isEmpty());
    }

    /**
     * Testing {@link ContentletAPI#findContentlets(java.util.List)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentlets() throws DotSecurityException, DotDataException {

        //Getting our test inodes
        List<String> inodes = new ArrayList<>();
        for (Contentlet contentlet : contentlets) {
            inodes.add(contentlet.getInode());
        }

        //Search for the contentlets
        List<Contentlet> foundContentlets = contentletAPI.findContentlets(inodes);

        //Validations
        assertTrue(foundContentlets != null && !foundContentlets.isEmpty());
        assertEquals(foundContentlets.size(), contentlets.size());
    }

    /**
     * Testing
     * {@link ContentletAPI#findContentletsByFolder(com.dotmarketing.portlets.folders.model.Folder,
     * com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentletsByFolder() throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Getting the folder of the test contentlet
        Folder folder = APILocator.getFolderAPI().find(contentlet.getFolder(), user, false);

        //Search the contentlets
        List<Contentlet> foundContentlets = contentletAPI.findContentletsByFolder(folder, user,
                false);

        //Validations
        assertTrue(foundContentlets != null && !foundContentlets.isEmpty());
    }

    /**
     * Testing
     * {@link ContentletAPI#findContentletsByHost(com.dotmarketing.beans.Host,
     * com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentletsByHost() throws DotDataException, DotSecurityException {

        //Search the contentlets
        List<Contentlet> foundContentlets = contentletAPI.findContentletsByHost(defaultHost, user,
                false);

        //Validations
        assertTrue(foundContentlets != null && !foundContentlets.isEmpty());
    }

    /**
     * Testing
     * {@link ContentletAPI#copyContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void copyContentlet() throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Copy the test contentlet
        Contentlet copyContentlet = contentletAPI.copyContentlet(contentlet, user, false);

        //validations
        assertTrue(copyContentlet != null && !copyContentlet.getInode().isEmpty());
        assertEquals(copyContentlet.getStructureInode(), contentlet.getStructureInode());
        assertEquals(copyContentlet.getFolder(), contentlet.getFolder());
        assertEquals(copyContentlet.getHost(), contentlet.getHost());

        try {
            contentletAPI.destroy(copyContentlet, user, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Testing
     * {@link ContentletAPI#copyContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * com.dotmarketing.portlets.folders.model.Folder, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void copyContentletWithFolder() throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Getting the folder of the test contentlet
        Folder folder = APILocator.getFolderAPI().find(contentlet.getFolder(), user, false);

        //Copy the test contentlet
        Contentlet copyContentlet = contentletAPI.copyContentlet(contentlet, folder, user, false);

        //validations
        assertTrue(copyContentlet != null && !copyContentlet.getInode().isEmpty());
        assertEquals(copyContentlet.getStructureInode(), contentlet.getStructureInode());
        assertEquals(copyContentlet.getFolder(), contentlet.getFolder());
        assertEquals(copyContentlet.get("junitTestWysiwyg"), contentlet.get("junitTestWysiwyg"));

        contentletAPI.destroy(copyContentlet, user, false);
    }

    /**
     * Testing
     * {@link ContentletAPI#copyContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * com.dotmarketing.beans.Host, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void copyContentletWithHost() throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Copy the test contentlet
        Contentlet copyContentlet = contentletAPI.copyContentlet(contentlet, defaultHost, user,
                false);

        //validations
        assertTrue(copyContentlet != null && !copyContentlet.getInode().isEmpty());
        assertEquals(copyContentlet.getStructureInode(), contentlet.getStructureInode());
        assertEquals(copyContentlet.getFolder(), contentlet.getFolder());
        assertEquals(copyContentlet.get("junitTestWysiwyg"), contentlet.get("junitTestWysiwyg"));
        assertEquals(copyContentlet.getHost(), contentlet.getHost());

        try {
            contentletAPI.destroy(copyContentlet, user, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * This tests that when a page is copied, we also include the personalized multitrees with it
     * See https://github.com/dotCMS/core/issues/16977
     *
     * @throws Exception
     */
    @Test
    public void TestCopyHTMLPageIncludesPersonalizedMultiTrees() throws Exception {

        final Template template = new TemplateDataGen().body("body").nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen().maxContentlets(1)
                .withStructure(structure, "").nextPersisted();
        final Contentlet content1 = new ContentletDataGen(structure.getInode()).nextPersisted();
        final Contentlet content2 = new ContentletDataGen(structure.getInode()).nextPersisted();

        final Persona persona = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();
        final String uniqueId = UUIDGenerator.shorty();

        MultiTree multiTree = new MultiTree();
        multiTree.setHtmlPage(page);
        multiTree.setContainer(container);
        multiTree.setContentlet(content1);
        multiTree.setInstanceId(uniqueId);
        multiTree.setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT);
        multiTree.setTreeOrder(1);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        multiTree = new MultiTree();
        multiTree.setHtmlPage(page);
        multiTree.setContainer(container);
        multiTree.setContentlet(content2);
        multiTree.setInstanceId(uniqueId);
        multiTree.setPersonalization(persona.getKeyTag());
        multiTree.setTreeOrder(1);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        Table<String, String, Set<PersonalizedContentlet>> pageContents = APILocator.getMultiTreeAPI()
                .getPageMultiTrees(page, false);

        for (final String containerId : pageContents.rowKeySet()) {
            assertEquals(
                    "containers match. Saved:" + container.getIdentifier() + ", got:" + containerId,
                    containerId, container.getIdentifier());

            for (final String uuid : pageContents.row(containerId).keySet()) {
                assertEquals("containers uuids match. Saved:" + uniqueId + ", got:" + uuid,
                        uniqueId, uuid);
                Set<PersonalizedContentlet> personalizedContentletSet = pageContents.get(
                        containerId, uniqueId);

                assertTrue("container should have 2 personalized contents - got :"
                                + personalizedContentletSet.size(),
                        personalizedContentletSet.size() == 2);
                assertTrue("container should have contentlet for keyTag:"
                        + MultiTree.DOT_PERSONALIZATION_DEFAULT, personalizedContentletSet
                        .contains(new PersonalizedContentlet(content1.getIdentifier(),
                                MultiTree.DOT_PERSONALIZATION_DEFAULT)));
                assertTrue("container should have contentlet for persona:" + persona.getKeyTag(),
                        personalizedContentletSet.contains(
                                new PersonalizedContentlet(content2.getIdentifier(),
                                        persona.getKeyTag())));
            }

        }

        HTMLPageAsset copyPage = APILocator.getHTMLPageAssetAPI()
                .fromContentlet(APILocator.getContentletAPI().copyContentlet(page, user, false));

        pageContents = APILocator.getMultiTreeAPI().getPageMultiTrees(copyPage, false);
        for (final String containerId : pageContents.rowKeySet()) {
            assertEquals(
                    "containers match. Saved:" + container.getIdentifier() + ", got:" + containerId,
                    containerId, container.getIdentifier());

            for (final String uuid : pageContents.row(containerId).keySet()) {
                assertEquals("containers uuids match. Saved:" + uniqueId + ", got:" + uuid,
                        uniqueId, uuid);
                Set<PersonalizedContentlet> personalizedContentletSet = pageContents.get(
                        containerId, uniqueId);

                assertTrue("container should have 2 personalized contents - got :"
                                + personalizedContentletSet.size(),
                        personalizedContentletSet.size() == 2);
                assertTrue("container should have contentlet for keyTag:"
                        + MultiTree.DOT_PERSONALIZATION_DEFAULT, personalizedContentletSet
                        .contains(new PersonalizedContentlet(content1.getIdentifier(),
                                MultiTree.DOT_PERSONALIZATION_DEFAULT)));
                assertTrue("container should have contentlet for persona:" + persona.getKeyTag(),
                        personalizedContentletSet.contains(
                                new PersonalizedContentlet(content2.getIdentifier(),
                                        persona.getKeyTag())));
            }
        }


    }

    /**
     * Testing
     * {@link ContentletAPI#copyContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * com.dotmarketing.portlets.folders.model.Folder, com.liferay.portal.model.User, boolean,
     * boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void copyContentletWithFolderAppendCopy() throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Getting the folder of the test contentlet
        Folder folder = APILocator.getFolderAPI().find(contentlet.getFolder(), user, false);

        //Copy the test contentlet
        Contentlet copyContentlet = contentletAPI.copyContentlet(contentlet, folder, user, true,
                false);

        //validations
        assertTrue(copyContentlet != null && !copyContentlet.getInode().isEmpty());
        assertEquals(copyContentlet.getStructureInode(), contentlet.getStructureInode());
        assertEquals(copyContentlet.getFolder(), contentlet.getFolder());
        assertEquals(copyContentlet.get("junitTestWysiwyg"), contentlet.get("junitTestWysiwyg"));

        try {
            contentletAPI.destroy(copyContentlet, user, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void copyContentletWithSeveralVersionsOrderIssue() throws Exception {
        long defLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();

        Host host1 = new Host();
        host1.setHostname("copy.contentlet.t1." + System.currentTimeMillis());
        host1.setDefault(false);
        host1.setLanguageId(defLang);
        host1.setIndexPolicy(IndexPolicy.FORCE);
        host1 = APILocator.getHostAPI().save(host1, user, false);

        Host host2 = new Host();
        host2.setHostname("copy.contentlet.t2." + System.currentTimeMillis());
        host2.setDefault(false);
        host2.setLanguageId(defLang);
        host2.setIndexPolicy(IndexPolicy.FORCE);
        host2 = APILocator.getHostAPI().save(host2, user, false);

        java.io.File bin = new java.io.File(
                APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + separator
                        + UUIDGenerator.generateUuid() + separator + "hello.txt");
        bin.getParentFile().mkdirs();

        bin.createNewFile();
        FileUtils.writeStringToFile(bin, "this is the content of the file");

        Contentlet file = new Contentlet();
        file.setHost(host1.getIdentifier());
        file.setFolder("SYSTEM_FOLDER");
        file.setStructureInode(
                CacheLocator.getContentTypeCache().getStructureByVelocityVarName("FileAsset")
                        .getInode());
        file.setLanguageId(defLang);
        file.setStringProperty(FileAssetAPI.TITLE_FIELD, "test copy");
        file.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, "hello.txt");
        file.setBinary(FileAssetAPI.BINARY_FIELD, bin);
        file.setIndexPolicy(IndexPolicy.FORCE);
        file = contentletAPI.checkin(file, user, false);
        final String ident = file.getIdentifier();

        // create 20 versions
        for (int i = 1; i <= 20; i++) {
            file = contentletAPI.findContentletByIdentifier(ident, false, defLang, user, false);
            file.setIndexPolicy(IndexPolicy.FORCE);
            APILocator.getFileAssetAPI().renameFile(file, "hello" + i, user, false);
        }

        file = contentletAPI.findContentletByIdentifier(ident, false, defLang, user, false);

        // the issue https://github.com/dotCMS/dotCMS/issues/5007 is caused by an order issue
        // when we call copy it saves all the versions in the new location. but it should
        // do it in older-to-newer order. because the last save will be the asset_name in the
        // identifier and the data that will have the "working" (or live) version.
        Contentlet copy = contentletAPI.copyContentlet(file, host1, user, false);
        Identifier copyIdent = APILocator.getIdentifierAPI().find(copy);

        copy = contentletAPI.findContentletByIdentifier(copyIdent.getId(), false, defLang, user,
                false);

        assertEquals("hello20_copy.txt", copyIdent.getAssetName());
        assertEquals("hello20_copy.txt", copy.getStringProperty(FileAssetAPI.FILE_NAME_FIELD));
        //FileAssetAPI.renameFile no longer renames the binary - so skipping assert
        //assertEquals("hello20_copy.txt",copy.getBinary(FileAssetAPI.BINARY_FIELD).getName());
        assertEquals("this is the content of the file",
                FileUtils.readFileToString(copy.getBinary(FileAssetAPI.BINARY_FIELD)));

        Contentlet copy2 = contentletAPI.copyContentlet(file, host2, user, false);
        Identifier copyIdent2 = APILocator.getIdentifierAPI().find(copy2);

        assertEquals("hello20.txt", copyIdent2.getAssetName());
        assertEquals("hello20.txt", copy2.getStringProperty(FileAssetAPI.FILE_NAME_FIELD));
        //FileAssetAPI.renameFile no longer renames the binary - so skipping assert
        //assertEquals("hello20.txt",copy2.getBinary(FileAssetAPI.BINARY_FIELD).getName());
        assertEquals("this is the content of the file",
                FileUtils.readFileToString(copy2.getBinary(FileAssetAPI.BINARY_FIELD)));

        try {
            contentletAPI.destroy(file, user, false);
            contentletAPI.destroy(copy, user, false);
            contentletAPI.destroy(copy2, user, false);
            contentletAPI.destroy(host1, user, false);
            contentletAPI.destroy(host2, user, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Testing
     * {@link ContentletAPI#search(String, int, int, String, com.liferay.portal.model.User,
     * boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void search() throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Create the lucene query
        String luceneQuery =
                "+structureinode:" + contentlet.getStructureInode() + " +deleted:false";

        //Search the contentlets
        List<Contentlet> foundContentlets = contentletAPI.search(luceneQuery, 1000, -1, "inode",
                user, false);

        //Validations
        assertTrue(foundContentlets != null && !foundContentlets.isEmpty());
    }

    /**
     * Testing
     * {@link ContentletAPI#search(String, int, int, String, com.liferay.portal.model.User, boolean,
     * int)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void searchWithPermissions() throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Create the lucene query
        String luceneQuery =
                "+structureinode:" + contentlet.getStructureInode() + " +deleted:false";

        //Search the contentlets
        List<Contentlet> foundContentlets = contentletAPI.search(luceneQuery, 1000, -1, "inode",
                user, false, PermissionAPI.PERMISSION_READ);

        //Validations
        assertTrue(foundContentlets != null && !foundContentlets.isEmpty());
    }

    /**
     * Testing
     * {@link ContentletAPI#searchIndex(String, int, int, String, com.liferay.portal.model.User,
     * boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void searchIndex() throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Create the lucene query
        String luceneQuery =
                "+structureinode:" + contentlet.getStructureInode() + " +deleted:false";

        //Search the contentlets
        List<ContentletSearch> foundContentlets = contentletAPI.searchIndex(luceneQuery, 1000, -1,
                "inode", user, false);

        //Validations
        assertTrue(foundContentlets != null && !foundContentlets.isEmpty());
    }

    /**
     * Testing
     * {@link
     * ContentletAPI#publishRelatedHtmlPages(com.dotmarketing.portlets.contentlet.model.Contentlet)}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws DotCacheException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void publishRelatedHtmlPages()
            throws DotDataException, DotSecurityException, DotCacheException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Making it live
        APILocator.getVersionableAPI().setLive(contentlet);

        //Publish html pages for this contentlet
        contentletAPI.publishRelatedHtmlPages(contentlet);

        //TODO: How to validate this???, good question, basically checking that the html page is not in cache basically the method publishRelatedHtmlPages(...) will just remove the htmlPage from cache

        //Get the contentlet Identifier to gather the related pages
        Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
        //Get the identifier's number of the related pages
        List<MultiTree> multiTrees = APILocator.getMultiTreeAPI()
                .getMultiTreesByChild(identifier.getId());
        for (MultiTree multitree : multiTrees) {
            //Get the Identifiers of the related pages
            Identifier htmlPageIdentifier = APILocator.getIdentifierAPI()
                    .find(multitree.getParent1());

            //OK..., lets try to find this page in the cache...
            HTMLPageAsset foundPage = (HTMLPageAsset) CacheLocator.getCacheAdministrator()
                    .get("HTMLPageCache" + identifier, "HTMLPageCache");

            //Validations
            assertTrue(foundPage == null || (foundPage.getInode() == null || foundPage.getInode()
                    .equals("")));
        }
    }

    /**
     * Testing
     * {@link ContentletAPI#cleanField(com.dotmarketing.portlets.structure.model.Structure,
     * com.dotmarketing.portlets.structure.model.Field, com.liferay.portal.model.User, boolean)}
     * with a binary field
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void cleanBinaryField() throws DotDataException, DotSecurityException {
        //Getting a known structure
        Structure structure = structures.iterator().next();

        Long identifier = uniqueIdentifier.get(structure.getName());

        //Search the contentlet for this structure
        List<Contentlet> contentletList = contentletAPI.findByStructure(structure, user, false, 0,
                0);
        Contentlet contentlet = contentletList.iterator().next();

        //Getting a known binary field for this structure
        //TODO: The definition of the method getFieldByName receive a parameter named "String:structureType", some examples I saw send the Inode, but actually what it needs is the structure name....

        Field foundBinaryField = FieldFactory.getFieldByVariableName(structure.getInode(),
                "junitTestBinary" + identifier);

        //Getting the current value for this field
        Object value = contentletAPI.getFieldValue(contentlet, foundBinaryField);

        //Validations
        assertNotNull(value);
        assertTrue(((java.io.File) value).exists());

        //Cleaning the binary field
        contentletAPI.cleanField(structure, foundBinaryField, user, false);

        assertFalse(((java.io.File) value).exists());
    }

    /**
     * Testing
     * {@link ContentletAPI#cleanField(com.dotmarketing.portlets.structure.model.Structure,
     * com.dotmarketing.portlets.structure.model.Field, com.liferay.portal.model.User, boolean)}
     * with a tag field
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void cleanTagField() throws DotDataException, DotSecurityException {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        Long identifier = uniqueIdentifier.get(structure.getName());

        //Search the contentlet for this structure
        List<Contentlet> contentletList = contentletAPI.findByStructure(structure, user, false, 0,
                0);
        Contentlet contentlet = contentletList.iterator().next();

        //Getting a known tag field for this structure
        //TODO: The definition of the method getFieldByName receive a parameter named "String:structureType", some examples I saw send the Inode, but actually what it needs is the structure name....
        Field foundTagField = FieldFactory.getFieldByVariableName(structure.getInode(),
                "junitTestTag" + identifier);

        //Getting the current value for this field
        List<Tag> value = tagAPI.getTagsByInodeAndFieldVarName(contentlet.getInode(),
                foundTagField.getVelocityVarName());

        //Validations
        assertNotNull(value);
        assertFalse(value.isEmpty());

        //Cleaning the tag field
        contentletAPI.cleanField(structure, foundTagField, user, false);

        //Getting the current value for this field
        List<Tag> value2 = tagAPI.getTagsByInodeAndFieldVarName(contentlet.getInode(),
                foundTagField.getVelocityVarName());

        //Validations
        assertTrue(value2.isEmpty());
    }

    /**
     * Tests method {@link ContentletAPI#getContentletReferences(Contentlet, User, boolean)}.
     * <p>
     * Checks that expected containers and pages (in the correct language) are returned by the
     * method.
     */

    @Test
    public void getContentletReferences() throws Exception {
        int english = 1;
        long spanish = spanishLanguage.getId();

        try {
            HibernateUtil.startTransaction();
            final String UUID = UUIDGenerator.generateUuid();
            Structure structure = new StructureDataGen().nextPersisted();
            Container container = new ContainerDataGen().withStructure(structure, "")
                    .nextPersisted();
            Template template = new TemplateDataGen().withContainer(container.getIdentifier(), UUID)
                    .nextPersisted();
            Folder folder = new FolderDataGen().nextPersisted();

            HTMLPageDataGen htmlPageDataGen = new HTMLPageDataGen(folder, template);
            HTMLPageAsset englishPage = htmlPageDataGen.languageId(english).nextPersisted();
            HTMLPageAsset spanishPage = htmlPageDataGen.pageURL(englishPage.getPageUrl() + "SP")
                    .languageId(spanish)
                    .nextPersisted();

            ContentletDataGen contentletDataGen = new ContentletDataGen(structure.getInode());
            Contentlet contentInEnglish = contentletDataGen.languageId(english).nextPersisted();
            Contentlet contentInSpanish = contentletDataGen.languageId(spanish).nextPersisted();

            // let's add the content to the page in english (create the page-container-content relationship)
            MultiTree multiTreeEN = new MultiTree(englishPage.getIdentifier(),
                    container.getIdentifier(),
                    contentInEnglish.getIdentifier(), UUID, 0);
            APILocator.getMultiTreeAPI().saveMultiTree(multiTreeEN);

            // let's add the content to the page in spanish (create the page-container-content relationship)
            MultiTree multiTreeSP = new MultiTree(spanishPage.getIdentifier(),
                    container.getIdentifier(),
                    contentInSpanish.getIdentifier(), UUID, 0);
            APILocator.getMultiTreeAPI().saveMultiTree(multiTreeSP);

            // let's get the references for english content
            List<Map<String, Object>> references = contentletAPI.getContentletReferences(
                    contentInEnglish, user, false);

            assertNotNull(references);
            assertTrue(!references.isEmpty());
            // let's check if the referenced page is in the expected language
            assertEquals(((IHTMLPage) references.get(0).get("page")).getLanguageId(), english);
            // let's check the referenced container is the expected
            assertEquals(((Container) references.get(0).get("container")).getInode(),
                    container.getInode());

            // let's get the references for spanish content
            references = contentletAPI.getContentletReferences(contentInSpanish, user, false);

            assertNotNull(references);
            assertTrue(!references.isEmpty());
            // let's check if the referenced page is in the expected language
            assertEquals(((IHTMLPage) references.get(0).get("page")).getLanguageId(), spanish);
            // let's check the referenced container is the expected
            assertEquals(((Container) references.get(0).get("container")).getInode(),
                    container.getInode());

            ContentletDataGen.remove(contentInEnglish);
            ContentletDataGen.remove(contentInSpanish);
            HTMLPageDataGen.remove(englishPage);
            HTMLPageDataGen.remove(spanishPage);
            TemplateDataGen.remove(template);
            ContainerDataGen.remove(container);
            StructureDataGen.remove(structure);
            FolderDataGen.remove(folder);

            HibernateUtil.closeAndCommitTransaction();
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            throw e;
        }
    }

    /**
     * Method to test: {@link ContentletAPI#getContentletReferences(Contentlet, User, boolean)} Test
     * case: Checks that the fallback page is returned by the method when there is no page for the
     * source content in its language (Spanish) Expected result: The page in the fallback language
     * (English) should be returned for the Spanish content
     */
    @Test
    public void getContentletReferencesForMonolingualPages() throws Exception {
        int english = 1;
        long spanish = spanishLanguage.getId();

        try {
            HibernateUtil.startTransaction();
            final String UUID = UUIDGenerator.generateUuid();
            Structure structure = new StructureDataGen().nextPersisted();
            Container container = new ContainerDataGen().withStructure(structure, "")
                    .nextPersisted();
            Template template = new TemplateDataGen().withContainer(container.getIdentifier(), UUID)
                    .nextPersisted();
            Folder folder = new FolderDataGen().nextPersisted();

            HTMLPageDataGen htmlPageDataGen = new HTMLPageDataGen(folder, template);
            HTMLPageAsset englishPage = htmlPageDataGen.languageId(english).nextPersisted();

            ContentletDataGen contentletDataGen = new ContentletDataGen(structure.getInode());
            Contentlet contentInEnglish = contentletDataGen.languageId(english).nextPersisted();
            Contentlet contentInSpanish = contentletDataGen.languageId(spanish).nextPersisted();

            // let's add the English content to the page in English (create the page-container-content relationship)
            MultiTree multiTreeEN = new MultiTree(englishPage.getIdentifier(),
                    container.getIdentifier(),
                    contentInEnglish.getIdentifier(), UUID, 0);
            APILocator.getMultiTreeAPI().saveMultiTree(multiTreeEN);

            // let's add the Spanish content to the page in English (create the page-container-content relationship)
            MultiTree multiTreeSP = new MultiTree(englishPage.getIdentifier(),
                    container.getIdentifier(),
                    contentInSpanish.getIdentifier(), UUID, 0);
            APILocator.getMultiTreeAPI().saveMultiTree(multiTreeSP);

            // let's get the references for english content
            List<Map<String, Object>> references = contentletAPI.getContentletReferences(
                    contentInEnglish, user, false);

            assertNotNull(references);
            assertTrue(!references.isEmpty());
            // let's check if the referenced page is in the expected language
            assertEquals(((IHTMLPage) references.get(0).get("page")).getLanguageId(), english);
            // let's check the referenced container is the expected
            assertEquals(((Container) references.get(0).get("container")).getInode(),
                    container.getInode());

            // let's get the references for spanish content
            references = contentletAPI.getContentletReferences(contentInSpanish, user, false);

            assertNotNull(references);
            assertTrue(!references.isEmpty());
            // let's check if the referenced page is in the expected language
            assertEquals(english, ((IHTMLPage) references.get(0).get("page")).getLanguageId());
            // let's check the referenced container is the expected
            assertEquals(container.getInode(),
                    ((Container) references.get(0).get("container")).getInode());

            ContentletDataGen.remove(contentInEnglish);
            ContentletDataGen.remove(contentInSpanish);
            HTMLPageDataGen.remove(englishPage);
            TemplateDataGen.remove(template);
            ContainerDataGen.remove(container);
            StructureDataGen.remove(structure);
            FolderDataGen.remove(folder);

            HibernateUtil.closeAndCommitTransaction();
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            throw e;
        }
    }

    /**
     * Method to test: {@link ContentletAPI#getContentletReferences(Contentlet, User, boolean)} Test
     * case: References from pages in different language than the contentlet one Expected result:
     * The multitree is excluded from the results
     */
    @Test
    public void getContentletReferences_FilterOutReferencesByContentLang() throws Exception {
        final int english = 1;
        final long spanish = spanishLanguage.getId();

        final String UUID = UUIDGenerator.generateUuid();
        Structure structure = new StructureDataGen().nextPersisted();
        Container container = new ContainerDataGen().withStructure(structure, "").nextPersisted();
        Template template = new TemplateDataGen().withContainer(container.getIdentifier(), UUID)
                .nextPersisted();
        Folder folder = new FolderDataGen().nextPersisted();

        HTMLPageDataGen htmlPageDataGen = new HTMLPageDataGen(folder, template);
        HTMLPageAsset spanishPage = htmlPageDataGen.languageId(spanish).nextPersisted();

        ContentletDataGen contentletDataGen = new ContentletDataGen(structure.getInode());
        Contentlet contentInEnglish = contentletDataGen.languageId(english).nextPersisted();
        Contentlet contentInSpanish = contentletDataGen.languageId(spanish).nextPersisted();

        // let's add the Spanish content to the page in Spanish (create the page-container-content relationship)
        MultiTree multiTreeSP = new MultiTree(spanishPage.getIdentifier(),
                container.getIdentifier(),
                contentInSpanish.getIdentifier(), UUID, 0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTreeSP);

        // let's get the references for english content
        List<Map<String, Object>> references = contentletAPI
                .getContentletReferences(contentInEnglish, user, false);

        assertNotNull(references);
        assertTrue(references.isEmpty());
    }

    /**
     * Method to test: {@link ContentletAPI#getContentletReferences(Contentlet, User, boolean)} Test
     * case: References from pages in same language than the contentlet one Expected result: The
     * multitree is present in the the results
     */
    @Test
    public void getContentletReferences_ReferencesByContentLang() throws Exception {
        final int english = 1;
        final long spanish = spanishLanguage.getId();

        final String UUID = UUIDGenerator.generateUuid();
        Structure structure = new StructureDataGen().nextPersisted();
        Container container = new ContainerDataGen().withStructure(structure, "").nextPersisted();
        Template template = new TemplateDataGen().withContainer(container.getIdentifier(), UUID)
                .nextPersisted();
        Folder folder = new FolderDataGen().nextPersisted();

        HTMLPageDataGen htmlPageDataGen = new HTMLPageDataGen(folder, template);
        HTMLPageAsset spanishPage = htmlPageDataGen.languageId(spanish).nextPersisted();

        ContentletDataGen contentletDataGen = new ContentletDataGen(structure.getInode());
        contentletDataGen.languageId(english).nextPersisted();
        Contentlet contentInSpanish = contentletDataGen.languageId(spanish).nextPersisted();

        // let's add the Spanish content to the page in Spanish (create the page-container-content relationship)
        MultiTree multiTreeSP = new MultiTree(spanishPage.getIdentifier(),
                container.getIdentifier(),
                contentInSpanish.getIdentifier(), UUID, 0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTreeSP);

        // let's get the references for spanish content
        List<Map<String, Object>> references = contentletAPI
                .getContentletReferences(contentInSpanish, user, false);

        assertNotNull(references);
        assertFalse(references.isEmpty());
        assertTrue(
                references.stream().allMatch((ref) -> ((IHTMLPage) ref.get("page")).getIdentifier()
                        .equals(spanishPage.getIdentifier())));
    }

    /**
     * Method to test: {@link ContentletAPI#getContentletReferences(Contentlet, User, boolean)} Test
     * case: Reference from page for a certain Persona Expected result: The results include the
     * Persona
     */
    @Test
    public void getContentletReferences_PersonaIncluded() throws Exception {
        final long spanish = spanishLanguage.getId();

        final String UUID = UUIDGenerator.generateUuid();
        Structure structure = new StructureDataGen().nextPersisted();
        Container container = new ContainerDataGen().withStructure(structure, "").nextPersisted();
        Template template = new TemplateDataGen().withContainer(container.getIdentifier(), UUID)
                .nextPersisted();
        Folder folder = new FolderDataGen().nextPersisted();

        HTMLPageDataGen htmlPageDataGen = new HTMLPageDataGen(folder, template);
        HTMLPageAsset spanishPage = htmlPageDataGen.languageId(spanish).nextPersisted();

        ContentletDataGen contentletDataGen = new ContentletDataGen(structure.getInode());
        Contentlet contentInSpanish = contentletDataGen.languageId(spanish).nextPersisted();

        final Persona persona = new PersonaDataGen().keyTag(UUIDGenerator.shorty()).nextPersisted();
        final String personalization = Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON
                + persona.getKeyTag();

        // let's add the Spanish content to the page in Spanish (create the page-container-content relationship)
        MultiTree multiTreeSP = new MultiTree(spanishPage.getIdentifier(),
                container.getIdentifier(),
                contentInSpanish.getIdentifier(), UUID, 0, personalization);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTreeSP);

        // let's get the references for spanish content
        List<Map<String, Object>> references = contentletAPI
                .getContentletReferences(contentInSpanish, user, false);

        assertNotNull(references);
        assertFalse(references.isEmpty());
        assertTrue(references.stream().allMatch((ref) -> ref.get("persona").
                equals(persona.getName())));
    }

    /**
     * Testing
     * {@link ContentletAPI#getFieldValue(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * com.dotmarketing.portlets.structure.model.Field)}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getFieldValue() throws DotSecurityException, DotDataException {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        Long identifier = uniqueIdentifier.get(structure.getName());

        //Getting a know field for this structure
        //TODO: The definition of the method getFieldByName receive a parameter named "String:structureType", some examples I saw send the Inode, but actually what it needs is the structure name....
        Field foundWysiwygField = FieldFactory.getFieldByVariableName(structure.getInode(),
                "junitTestWysiwyg" + identifier);

        //Search the contentlets for this structure
        List<Contentlet> contentletList = contentletAPI.findByStructure(structure, user, false, 0,
                0);

        //Getting the current value for this field
        Object value = contentletAPI.getFieldValue(contentletList.iterator().next(),
                foundWysiwygField);

        //Validations
        assertNotNull(value);
        assertTrue(!((String) value).isEmpty());
    }

    /**
     * Testing
     * {@link
     * ContentletAPI#addLinkToContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * String, String, com.liferay.portal.model.User, boolean)}
     *
     * @throws Exception
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void addLinkToContentlet() throws Exception {

        String RELATION_TYPE = new Link().getType();

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Create a menu link
        Link menuLink = createMenuLink();

        //Search the contentlets for this structure
        List<Contentlet> contentletList = contentletAPI.findByStructure(structure, user, false, 0,
                0);
        Contentlet contentlet = contentletList.iterator().next();

        //Add to this contentlet a link
        contentletAPI.addLinkToContentlet(contentlet, menuLink.getInode(), RELATION_TYPE, user,
                false);

        //Verify if the link was associated
        //List<Link> relatedLinks = contentletAPI.getRelatedLinks( contentlet, user, false );//TODO: This method is not working but we are not testing it on this call....

        //Get the contentlet Identifier to gather the menu links
        Identifier menuLinkIdentifier = APILocator.getIdentifierAPI().find(menuLink);

        //Verify if the relation was created
        Tree tree = TreeFactory.getTree(contentlet.getInode(), menuLinkIdentifier.getId(),
                RELATION_TYPE);

        //Validations
        assertNotNull(tree);
        assertNotNull(tree.getParent());
        assertNotNull(tree.getChild());
        assertEquals(tree.getParent(), contentlet.getInode());
        assertEquals(tree.getChild(), menuLinkIdentifier.getId());
        assertEquals(tree.getRelationType(), RELATION_TYPE);

        try {
            HibernateUtil.startTransaction();
            menuLinkAPI.delete(menuLink, user, false);
            HibernateUtil.closeAndCommitTransaction();
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            Logger.error(ContentletAPITest.class, e.getMessage());
        }


    }

    /**
     * Testing
     * {@link ContentletAPI#findPageContentlets(String, String, String, boolean, long,
     * com.liferay.portal.model.User, boolean)}
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findPageContentlets() throws DotDataException, DotSecurityException {

        //Iterate throw the test contentles
        for (Contentlet contentlet : contentlets) {

            //Get the identifier for this contentlet
            Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);

            //Search for related html pages and containers
            List<MultiTree> multiTrees = APILocator.getMultiTreeAPI()
                    .getMultiTreesByChild(identifier.getId());
            if (multiTrees != null && !multiTrees.isEmpty()) {

                for (MultiTree multiTree : multiTrees) {

                    //Getting the identifiers
                    Identifier htmlPageIdentifier = APILocator.getIdentifierAPI()
                            .find(multiTree.getParent1());
                    Identifier containerPageIdentifier = APILocator.getIdentifierAPI()
                            .find(multiTree.getParent2());

                    //Find the related contentlets, at this point should return something....
                    List<Contentlet> pageContentlets = contentletAPI.findPageContentlets(
                            htmlPageIdentifier.getId(), containerPageIdentifier.getId(), null, true,
                            -1, user, false);

                    //Validations
                    assertTrue(pageContentlets != null && !pageContentlets.isEmpty());
                }

                break;
            }
        }
    }

    /**
     * This test is for ordering purposes. When you get the related content of a content, the
     * ordering should be the same as it was stored.
     */
    @Test
    public void test_getAllRelationships_checkOrdering()
            throws DotSecurityException, DotDataException {
        ContentType parentContentType = null;
        ContentType childContentType = null;
        try {
            parentContentType = createContentType("parentContentType", BaseContentType.CONTENT);
            childContentType = createContentType("childContentType", BaseContentType.CONTENT);

            //Create Relationship Field and Text Fields
            createRelationshipField("testRelationship", parentContentType.id(),
                    childContentType.variable());
            final String textFieldString = "title";
            createTextField(textFieldString, parentContentType.id());
            createTextField(textFieldString, childContentType.id());

            //Create Contentlets
            Contentlet contentletParent = new ContentletDataGen(parentContentType.id()).setProperty(
                    textFieldString, "parent Contentlet").next();
            final Contentlet contentletChild1 = new ContentletDataGen(
                    childContentType.id()).setProperty(textFieldString, "child Contentlet")
                    .nextPersisted();
            final Contentlet contentletChild2 = new ContentletDataGen(
                    childContentType.id()).setProperty(textFieldString, "child Contentlet 2")
                    .nextPersisted();
            final Contentlet contentletChild3 = new ContentletDataGen(
                    childContentType.id()).setProperty(textFieldString, "child Contentlet 3")
                    .nextPersisted();

            //Find Relationship
            final Relationship relationship = relationshipAPI.byParent(parentContentType).get(0);

            //Relate contentlets
            Map<Relationship, List<Contentlet>> relationshipListMap = Maps.newHashMap();
            relationshipListMap.put(relationship,
                    CollectionsUtils.list(contentletChild1, contentletChild2, contentletChild3));

            //Checkin of the parent to save Relationships
            contentletParent = contentletAPI.checkin(contentletParent, relationshipListMap, user,
                    false);

            //Get All Relationships of the parent contentlet
            ContentletRelationships cRelationships = contentletAPI.getAllRelationships(
                    contentletParent);

            //Check that the content is related and the order of the related content (child1 - child2 - child3)
            assertNotNull(cRelationships);
            assertEquals(contentletChild1,
                    cRelationships.getRelationshipsRecords().get(0).getRecords().get(0));
            assertEquals(contentletChild2,
                    cRelationships.getRelationshipsRecords().get(0).getRecords().get(1));
            assertEquals(contentletChild3,
                    cRelationships.getRelationshipsRecords().get(0).getRecords().get(2));

            //Reorder Relationships
            relationshipListMap.put(relationship,
                    CollectionsUtils.list(contentletChild3, contentletChild1, contentletChild2));
            contentletParent = contentletAPI.checkout(contentletParent.getInode(), user, false);
            contentletParent = contentletAPI.checkin(contentletParent, relationshipListMap, user,
                    false);

            //Get All Relationships of the parent contentlet
            cRelationships = contentletAPI.getAllRelationships(contentletParent);

            //Check that the content is related and the order of the related content (child3 - child1 - child2)
            assertNotNull(cRelationships);
            assertEquals(contentletChild3,
                    cRelationships.getRelationshipsRecords().get(0).getRecords().get(0));
            assertEquals(contentletChild1,
                    cRelationships.getRelationshipsRecords().get(0).getRecords().get(1));
            assertEquals(contentletChild2,
                    cRelationships.getRelationshipsRecords().get(0).getRecords().get(2));
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
     * This test is for ordering purposes. When you get the related content of a content, the
     * ordering should be the same as it was stored. For selfRelated Content Types.
     */
    @Test
    public void test_getAllRelationships_checkOrdering_selfRelatedContentType()
            throws DotSecurityException, DotDataException {
        ContentType parentContentType = null;
        try {
            parentContentType = createContentType("parentContentType", BaseContentType.CONTENT);

            //Create Relationship Field and Text Fields
            createRelationshipField("testRelationship", parentContentType.id(),
                    parentContentType.variable());
            final String textFieldString = "title";
            createTextField(textFieldString, parentContentType.id());

            //Create Contentlets
            Contentlet contentletParent = new ContentletDataGen(parentContentType.id()).setProperty(
                    textFieldString, "parent Contentlet").next();
            final Contentlet contentletChild1 = new ContentletDataGen(
                    parentContentType.id()).setProperty(textFieldString, "child Contentlet")
                    .nextPersisted();
            final Contentlet contentletChild2 = new ContentletDataGen(
                    parentContentType.id()).setProperty(textFieldString, "child Contentlet 2")
                    .nextPersisted();
            final Contentlet contentletChild3 = new ContentletDataGen(
                    parentContentType.id()).setProperty(textFieldString, "child Contentlet 3")
                    .nextPersisted();

            //Find Relationship
            final Relationship relationship = relationshipAPI.byParent(parentContentType).get(0);

            //Relate contentlets
            Map<Relationship, List<Contentlet>> relationshipListMap = Maps.newHashMap();
            relationshipListMap.put(relationship,
                    CollectionsUtils.list(contentletChild1, contentletChild2, contentletChild3));

            //Checkin of the parent to save Relationships
            contentletParent = contentletAPI.checkin(contentletParent, relationshipListMap, user,
                    false);

            //Get All Relationships of the parent contentlet
            ContentletRelationships cRelationships = contentletAPI.getAllRelationships(
                    contentletParent);

            //Check that the content is related and the order of the related content (child1 - child2 - child3)
            assertNotNull(cRelationships);
            assertEquals(contentletChild1,
                    cRelationships.getRelationshipsRecords().get(1).getRecords().get(0));
            assertEquals(contentletChild2,
                    cRelationships.getRelationshipsRecords().get(1).getRecords().get(1));
            assertEquals(contentletChild3,
                    cRelationships.getRelationshipsRecords().get(1).getRecords().get(2));

            //Reorder Relationships
            relationshipListMap.put(relationship,
                    CollectionsUtils.list(contentletChild3, contentletChild1, contentletChild2));
            contentletParent = contentletAPI.checkout(contentletParent.getInode(), user, false);
            contentletParent = contentletAPI.checkin(contentletParent, relationshipListMap, user,
                    false);

            //Get All Relationships of the parent contentlet
            cRelationships = contentletAPI.getAllRelationships(contentletParent);

            //Check that the content is related and the order of the related content (child3 - child1 - child2)
            assertNotNull(cRelationships);
            assertEquals(contentletChild3,
                    cRelationships.getRelationshipsRecords().get(1).getRecords().get(0));
            assertEquals(contentletChild1,
                    cRelationships.getRelationshipsRecords().get(1).getRecords().get(1));
            assertEquals(contentletChild2,
                    cRelationships.getRelationshipsRecords().get(1).getRecords().get(2));
        } finally {
            if (parentContentType != null) {
                contentTypeAPI.delete(parentContentType);
            }
        }
    }

    /**
     * Create a relationship field with the given name and cardinality
     */
    private com.dotcms.contenttype.model.field.Field createRelationshipField(final String relationName,
                                                                             final ContentType parentContentType, final String childContentTypeVar,
                                                                             final int cardinality) throws DotSecurityException, DotDataException {

        final com.dotcms.contenttype.model.field.Field newField = FieldBuilder.builder(RelationshipField.class).name(relationName)
                .contentTypeId(parentContentType.id()).values(String.valueOf(cardinality))
                .relationType(childContentTypeVar).build();

        return fieldAPI.save(newField, user);
    }

    private com.dotcms.contenttype.model.field.Field createRelationshipField(final String fieldName,
            final String parentContentTypeID, final String childContentTypeVariable)
            throws DotDataException, DotSecurityException {
        final com.dotcms.contenttype.model.field.Field field = FieldBuilder.builder(
                        RelationshipField.class)
                .name(fieldName)
                .contentTypeId(parentContentTypeID)
                .values(String.valueOf(
                        WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                .indexed(true)
                .listed(false)
                .relationType(childContentTypeVariable)
                .build();

        return getContentTypeFieldAPI().save(field, APILocator.systemUser());
    }

    /**
     * Testing
     * {@link
     * ContentletAPI#getAllRelationships(com.dotmarketing.portlets.contentlet.model.Contentlet)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getAllRelationships() throws DotSecurityException, DotDataException {

        Relationship testRelationship = null;
        try {
            //Getting a known contentlet
            Contentlet contentlet = contentlets.iterator().next();

            //Create the test relationship
            testRelationship = createRelationShip(contentlet.getStructure(), false);

            //Find all the relationships for this contentlet
            ContentletRelationships contentletRelationships = contentletAPI
                    .getAllRelationships(contentlet.getInode(), user, false);

            //Validations
            assertNotNull(contentletRelationships);
            assertTrue(contentletRelationships.getRelationshipsRecords() != null
                    && !contentletRelationships.getRelationshipsRecords().isEmpty());

        } finally {
            if (testRelationship != null && testRelationship.getInode() != null) {
                relationshipAPI.delete(testRelationship);
            }
        }
    }

    @Test
    public void testCreateSelfJoinedRelationship_contentletAddedAsChild()
            throws DotDataException, DotSecurityException {

        Contentlet parent = null;
        Contentlet child = null;
        Structure structure = null;
        Relationship selfRelationship = null;

        try {
            //Get Default Language
            Language language = languageAPI.getDefaultLanguage();

            //Create a Structure
            structure = createStructure(
                    "JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                    "junit_test_structure_" + String.valueOf(new Date().getTime()));

            //Create the Contentlets
            parent = createContentlet(structure, language, false);
            child = createContentlet(structure, language, false);

            //Create the Self contained relationship
            selfRelationship = createRelationShip(structure.getInode(),
                    structure.getInode(), false);

            //Create the contentlet relationships
            List<Contentlet> contentRelationships = new ArrayList<>();
            contentRelationships.add(child);

            //Relate the content
            contentletAPI
                    .relateContent(parent, selfRelationship, contentRelationships, user, false);

            //Find all the relationships for this contentlet
            ContentletRelationships contentletRelationships = contentletAPI
                    .getAllRelationships(parent.getInode(), user, false);

            //Validations
            assertNotNull(contentletRelationships);
            assertTrue(contentletRelationships.getRelationshipsRecords() != null
                    && !contentletRelationships.getRelationshipsRecords().isEmpty());
            for (ContentletRelationshipRecords record : contentletRelationships
                    .getRelationshipsRecords()) {
                if (record.isHasParent()) {
                    assertNotNull(record.getRecords());
                }
            }
        } finally {
            try {
                //Clean up
                if (parent != null) {
                /*contentletAPI.archive(parent, user, false);
                contentletAPI.delete(parent, user, false);*/
                    contentletAPI.destroy(parent, user, false);
                }
                if (child != null) {
                /*contentletAPI.archive(child, user, false);
                contentletAPI.delete(child, user, false);*/
                    contentletAPI.destroy(child, user, false);
                }
                if (selfRelationship != null) {
                    relationshipAPI.delete(selfRelationship);
                }
                if (structure != null) {
                    APILocator.getStructureAPI().delete(structure, user);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Testing
     * {@link
     * ContentletAPI#getAllRelationships(com.dotmarketing.portlets.contentlet.model.Contentlet)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getAllRelationshipsByContentlet() throws DotSecurityException, DotDataException {

        Structure testStructure = null;
        Relationship testRelationship = null;
        try {
            //First lets create a test structure
            testStructure = createStructure(
                    "JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                    "junit_test_structure_" + String.valueOf(new Date().getTime()));

            //Now a new test contentlets
            Contentlet parentContentlet = createContentlet(testStructure, null, false);
            Contentlet childContentlet = createContentlet(testStructure, null, false);

            //Create the relationship
            testRelationship = createRelationShip(testStructure, false);

            //Create the contentlet relationships
            List<Contentlet> contentRelationships = new ArrayList<>();
            contentRelationships.add(childContentlet);

            //Relate the content
            contentletAPI
                    .relateContent(parentContentlet, testRelationship, contentRelationships, user,
                            false);

            //Getting a known contentlet
//        Contentlet contentlet = contentlets.iterator().next();

            //Find all the relationships for this contentlet
            ContentletRelationships contentletRelationships = contentletAPI
                    .getAllRelationships(parentContentlet);

            //Validations
            assertNotNull(contentletRelationships);
            assertTrue(contentletRelationships.getRelationshipsRecords() != null
                    && !contentletRelationships.getRelationshipsRecords().isEmpty());
        } finally {
            if (testRelationship != null && testRelationship.getInode() != null) {
                relationshipAPI.delete(testRelationship);
            }

            if (testStructure != null && testStructure.getInode() != null) {
                APILocator.getStructureAPI().delete(testStructure, user);
            }
        }

    }

    /**
     * Testing
     * {@link ContentletAPI#getAllLanguages(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * Boolean, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getAllLanguages() throws DotSecurityException, DotDataException {

        Structure st = new Structure();
        st.setStructureType(BaseContentType.CONTENT.getType());
        st.setName("JUNIT-test-getAllLanguages" + System.currentTimeMillis());
        st.setVelocityVarName("testAllLanguages" + System.currentTimeMillis());
        st.setHost(defaultHost.getIdentifier());
        StructureFactory.saveStructure(st);

        Field ff = new Field("title", Field.FieldType.TEXT, Field.DataType.TEXT, st, true, true,
                true, 1, false, false, true);
        FieldFactory.saveField(ff);

        String identifier = null;
        List<Language> list = APILocator.getLanguageAPI().getLanguages();
        Contentlet last = null;
        for (Language ll : list) {
            Contentlet con = new Contentlet();
            con.setStructureInode(st.getInode());
            if (identifier != null) {
                con.setIdentifier(identifier);
            }
            con.setStringProperty(ff.getVelocityVarName(),
                    "test text " + System.currentTimeMillis());
            con.setLanguageId(ll.getId());
            con.setIndexPolicy(IndexPolicy.FORCE);
            con = contentletAPI.checkin(con, user, false);
            if (identifier == null) {
                identifier = con.getIdentifier();
            }
            APILocator.getVersionableAPI().setLive(con);
            last = con;
        }

        //Get all the contentles siblings for this contentlet (contentlet for all the languages)
        List<Contentlet> forAllLanguages = contentletAPI.getAllLanguages(last, true, user, false);

        //Validations
        assertNotNull(forAllLanguages);
        assertTrue(!forAllLanguages.isEmpty());
        assertEquals(list.size(), forAllLanguages.size());

    }

    /**
     * Testing
     * {@link ContentletAPI#isContentEqual(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User,
     * boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void isContentEqual() throws DotDataException, DotSecurityException {

        Iterator<Contentlet> contentletIterator = contentlets.iterator();

        //Getting test contentlets
        Contentlet contentlet1 = contentletIterator.next();
        Contentlet contentlet2 = contentletIterator.next();

        //Compare if the contentlets are equal
        Boolean areEqual = contentletAPI.isContentEqual(contentlet1, contentlet2, user, false);

        //Validations
        assertNotNull(areEqual);
        assertFalse(areEqual);
    }

    /**
     * Testing
     * {@link ContentletAPI#archive(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void archive() throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        try {
            //Archive this given contentlet (means it will be mark it as deleted)
            contentletAPI.archive(contentlet, user, false);

            //Verify if it was deleted
            Boolean isDeleted = APILocator.getVersionableAPI().isDeleted(contentlet);

            //Validations
            assertNotNull(isDeleted);
            assertTrue(isDeleted);
        } finally {
            contentletAPI.unarchive(contentlet, user, false);
        }
    }

    /**
     * https://github.com/dotCMS/core/issues/11716
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */

    @Test
    public void addRemoveContentFromIndex()
            throws DotDataException, DotSecurityException {
        // respect CMS Anonymous permissions
        boolean respectFrontendRoles = false;
        int num = 5;

        //clean up old reindexed records
        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();

        final ContentType type = new ContentTypeDataGen().nextPersisted();
        List<Contentlet> origCons = new ArrayList<>();

        //add 5 contentlets
        for (int i = 0; i < num; i++) {
            final Contentlet contentlet = new ContentletDataGen(type.id())
                    .setProperty("title", i + "my test title")
                    .nextPersisted();

            assertNotNull(contentlet.getIdentifier());
            assertTrue(contentlet.isWorking());
            assertFalse(contentlet.isLive());
            origCons.add(contentlet);
        }

        for (Contentlet c : origCons) {
            assertEquals(1, contentletAPI.indexCount(
                    "+live:false +identifier:" + c.getIdentifier() + " +inode:" + c.getInode(),
                    user, respectFrontendRoles));
            assertEquals(0, contentletAPI.indexCount(
                    "+live:true +identifier:" + c.getIdentifier() + " +inode:" + c.getInode(),
                    user, respectFrontendRoles));
        }

        HibernateUtil.startTransaction();
        boolean isNewConn = !DbConnectionFactory.connectionExists();
        try {
            for (Contentlet c : origCons) {
                Contentlet newContentlet = new Contentlet(c);
                newContentlet.setInode("");
                newContentlet.setIndexPolicy(IndexPolicy.FORCE);
                newContentlet.setStringProperty("title", c.getStringProperty("title") + " new");
                newContentlet.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
                newContentlet = contentletAPI.checkin(newContentlet, user, respectFrontendRoles);
                newContentlet.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
                contentletAPI.publish(newContentlet, user, respectFrontendRoles);
                assertTrue(newContentlet.isLive());
            }
            throw new DotDataException("uh oh, what happened?");
        } catch (DotDataException e) {
            HibernateUtil.rollbackTransaction();
        } finally {
            if (isNewConn) {
                HibernateUtil.closeSession();
            }
        }

        for (Contentlet c : origCons) {
            assertEquals(1, contentletAPI.indexCount(
                    "+live:false +identifier:" + c.getIdentifier() + " +inode:" + c.getInode(),
                    user, respectFrontendRoles));
            assertEquals(0, contentletAPI
                    .indexCount("+live:true +identifier:" + c.getIdentifier(), user,
                            respectFrontendRoles));
        }

    }

    /**
     * Testing
     * {@link ContentletAPI#delete(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void delete() throws Exception {

        //First lets create a test structure
        Structure testStructure = createStructure(
                "JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                "junit_test_structure_" + String.valueOf(new Date().getTime()));

        //Now a new contentlet
        Contentlet newContentlet = createContentlet(testStructure, null, false);

        //Now we need to delete it
        contentletAPI.archive(newContentlet, user, false);
        contentletAPI.delete(newContentlet, user, false);

        //Try to find the deleted Contentlet
        Contentlet foundContentlet = contentletAPI.find(newContentlet.getInode(), user, false);

        //Validations
        assertTrue(foundContentlet == null || foundContentlet.getInode() == null
                || foundContentlet.getInode().isEmpty());

        // make sure the db is totally clean up

        AssetUtil.assertDeleted(newContentlet.getInode(), newContentlet.getIdentifier(),
                "contentlet");

        APILocator.getStructureAPI().delete(testStructure, user);
    }

    @Test
    public void delete_GivenUnarchivedContentAndDontValidateMeInTrue_ShouldDeleteSuccessfully()
            throws Exception {
        Structure testStructure = null;
        try {
            testStructure = createStructure(
                    "JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                    "junit_test_structure_" + String.valueOf(new Date().getTime()));
            final Contentlet newContentlet = createContentlet(testStructure, null, false);
            newContentlet.setStringProperty(Contentlet.DONT_VALIDATE_ME, "anarchy");
            contentletAPI.delete(newContentlet, user, false);
            
            // Wait for the contentlet to be deleted asynchronously
            Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
                final Contentlet foundContentlet = contentletAPI.find(newContentlet.getInode(), user, false);
                return !UtilMethods.isSet(foundContentlet) || !UtilMethods.isSet(foundContentlet.getInode());
            });

            AssetUtil.assertDeleted(newContentlet.getInode(), newContentlet.getIdentifier(),
                    "contentlet");
        } finally {
            APILocator.getStructureAPI().delete(testStructure, user);
        }
    }

    /**
     * Creates a content on english and spanish. Locked the english content by admin Tries to
     * destroy spanish content Throws an exception
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test(expected = DotLockException.class)
    public void no_destroy_limited_content_locked_by_admin_by_limited_user()
            throws DotSecurityException, DotDataException {

        final User chrisPublisher = TestUserUtils.getChrisPublisherUser();
        final User adminUser = TestUserUtils.getAdminUser();

        final Language language1 = new LanguageDataGen()
                .countryCode("IT").nextPersisted();

        final Language language2 = new LanguageDataGen()
                .countryCode("IT").nextPersisted();
        final Structure testStructure = createStructure(
                "JUnit Test Destroy Structure_" + new Date().getTime(),
                "junit_test_destroy_structure_" + new Date().getTime());
        Contentlet newContentlet1 = createContentlet(testStructure, language1, false);

        Contentlet anotherLanguage = contentletAPI.checkout(newContentlet1.getInode(), user, false);
        anotherLanguage.setLanguageId(language2.getId());
        anotherLanguage = contentletAPI.checkin(anotherLanguage, user, false);

        final Permission permission = new Permission(
                newContentlet1.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(chrisPublisher).getId(),
                (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                        | PermissionAPI.PERMISSION_WRITE
                        | PermissionAPI.PERMISSION_PUBLISH),
                true);

        APILocator.getPermissionAPI().save(permission, newContentlet1, user, false);

        // new inode to create a new version
        final String newInode = UUIDGenerator.generateUuid();
        newContentlet1.setInode(newInode);
        newContentlet1 = contentletAPI.checkin(newContentlet1, adminUser, false);

        //Now we need archive and lock by admin
        contentletAPI.archive(newContentlet1, adminUser, false);
        contentletAPI.lock(newContentlet1, adminUser, false);

        //  Tries to destroy by limited user
        contentletAPI.destroy(anotherLanguage, chrisPublisher, false);
    }

    /**
     * Creates a content on english and spanish. Locked the english content by admin Tries to
     * destroy spanish content by admin Everything successfully destroyed
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void destroy_content_locked_by_admin_by_other_admin_user()
            throws DotSecurityException, DotDataException {

        final User adminUser2 = TestUserUtils.getAdminUser();
        final User adminUser = TestUserUtils.getAdminUser();

        final Language language1 = new LanguageDataGen()
                .countryCode("IT").nextPersisted();

        final Language language2 = new LanguageDataGen()
                .countryCode("IT").nextPersisted();
        final Structure testStructure = createStructure(
                "JUnit Test Destroy Structure_" + new Date().getTime(),
                "junit_test_destroy_structure_" + new Date().getTime());

        Contentlet newContentlet1 = createContentlet(testStructure, language1, false);

        Contentlet anotherLanguage = contentletAPI.checkout(newContentlet1.getInode(), user, false);
        anotherLanguage.setLanguageId(language2.getId());
        anotherLanguage = contentletAPI.checkin(anotherLanguage, user, false);

        final Permission permission = new Permission(
                newContentlet1.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(adminUser2).getId(),
                (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                        | PermissionAPI.PERMISSION_WRITE
                        | PermissionAPI.PERMISSION_PUBLISH),
                true);

        APILocator.getPermissionAPI().save(permission, newContentlet1, user, false);

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(newContentlet1.getIdentifier());
        // new inode to create a new version
        final String newInode = UUIDGenerator.generateUuid();
        newContentlet1.setInode(newInode);
        newContentlet1 = contentletAPI.checkin(newContentlet1, adminUser, false);

        //Now we need archive and lock by admin
        contentletAPI.archive(newContentlet1, adminUser, false);
        contentletAPI.lock(newContentlet1, adminUser, false);

        //  Tries to destroy by limited user
        contentletAPI.destroy(anotherLanguage, adminUser2, false);

        final List<Contentlet> contentlets =
                contentletAPI.findAllVersions(identifier, true, user, false);

        assertFalse(UtilMethods.isSet(contentlets));
    }

    /**
     * Testing
     * {@link ContentletAPI#delete(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * com.liferay.portal.model.User, boolean, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteForAllVersions() throws DotSecurityException, DotDataException {

        Language language = APILocator.getLanguageAPI().getDefaultLanguage();

        //First lets create a test structure
        Structure testStructure = createStructure(
                "JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                "junit_test_structure_" + String.valueOf(new Date().getTime()));

        //Now a new contentlet
        Contentlet newContentlet = createContentlet(testStructure, language, false);

        // new inode to create a new version
        String newInode = UUIDGenerator.generateUuid();
        newContentlet.setInode(newInode);

        List<Language> languages = APILocator.getLanguageAPI().getLanguages();
        for (Language localLanguage : languages) {
            if (localLanguage.getId() != language.getId()) {
                language = localLanguage;
                break;
            }
        }

        newContentlet.setLanguageId(language.getId());

        newContentlet = contentletAPI.checkin(newContentlet, user, false);

        //Now we need to delete it
        contentletAPI.archive(newContentlet, user, false);
        contentletAPI.delete(newContentlet, user, false, true);

        //Try to find the deleted Contentlet
        Identifier contentletIdentifier = APILocator.getIdentifierAPI()
                .find(newContentlet.getIdentifier());
        List<Contentlet> foundContentlets = contentletAPI.findAllVersions(contentletIdentifier,
                user, false);

        //Validations
        assertTrue(foundContentlets == null || foundContentlets.isEmpty());
        APILocator.getStructureAPI().delete(testStructure, user);
    }

    /**
     * Testing
     * {@link ContentletAPI#publish(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void publish() throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Publish the test contentlet
        contentletAPI.publish(contentlet, user, false);

        //Verify if it was published
        final boolean isLive = APILocator.getVersionableAPI().isLive(contentlet);

        //Validations
        assertNotNull(isLive);
        assertTrue(isLive);

        final Optional<ContentletVersionInfo> versionInfo = APILocator.getVersionableAPI()
                .getContentletVersionInfo(contentlet.getIdentifier(),
                        contentlet.getLanguageId(), contentlet.getVariantId());
        final Date publishDate = versionInfo.map(ContentletVersionInfo::getPublishDate).orElse(null);
        assertNotNull(publishDate);

    }

    /**
     * Testing
     * {@link ContentletAPI#publish(java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void publishCollection() throws DotDataException, DotSecurityException {

        //Publish all the test contentlets
        contentletAPI.publish(contentlets, user, false);

        for (Contentlet contentlet : contentlets) {

            //Verify if it was published
            Boolean isLive = APILocator.getVersionableAPI().isLive(contentlet);

            //Validations
            assertNotNull(isLive);
            assertTrue(isLive);
        }
    }

    /**
     * Testing
     * {@link ContentletAPI#unpublish(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void unpublish() throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Verify if it is published
        Boolean isLive = APILocator.getVersionableAPI().isLive(contentlet);
        if (!isLive) {
            //Publish the test contentlet
            contentletAPI.publish(contentlet, user, false);

            //Verify if it was published
            isLive = APILocator.getVersionableAPI().isLive(contentlet);

            //Validations
            assertNotNull(isLive);
            assertTrue(isLive);
        }

        //Unpublish the test contentlet
        contentletAPI.unpublish(contentlet, user, false);

        //Verify if it was unpublished
        isLive = APILocator.getVersionableAPI().isLive(contentlet);

        //Validations
        assertNotNull(isLive);
        assertFalse(isLive);

    }

    /**
     * Testing
     * {@link ContentletAPI#unpublish(java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void unpublishCollection() throws DotDataException, DotSecurityException {

        contentletAPI.publish(contentlets, user, false);

        //Unpublish all the test contentlets
        contentletAPI.unpublish(contentlets, user, false);

        for (Contentlet contentlet : contentlets) {

            //Verify if it was unpublished
            Boolean isLive = APILocator.getVersionableAPI().isLive(contentlet);

            //Validations
            assertNotNull(isLive);
            assertFalse(isLive);
        }
    }

    /**
     * Testing
     * {@link ContentletAPI#archive(java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void archiveCollection() throws DotDataException, DotSecurityException {

        try {
            //Archive this given contentlet collection (means it will be mark them as deleted)
            contentletAPI.archive(contentlets, user, false);

            for (Contentlet contentlet : contentlets) {

                //Verify if it was deleted
                Boolean isDeleted = APILocator.getVersionableAPI().isDeleted(contentlet);

                //Validations
                assertNotNull(isDeleted);
                assertTrue(isDeleted);
            }
        } finally {
            contentletAPI.unarchive(contentlets, user, false);
        }
    }

    /**
     * Testing
     * {@link ContentletAPI#unarchive(java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void unarchiveCollection() throws DotDataException, DotSecurityException {

        //First lets archive this given contentlet collection (means it will be mark them as deleted)
        contentletAPI.archive(contentlets, user, false);

        //Now lets test the unarchive
        contentletAPI.unarchive(contentlets, user, false);

        for (Contentlet contentlet : contentlets) {

            //Verify if it continues as deleted
            Boolean isDeleted = APILocator.getVersionableAPI().isDeleted(contentlet);

            //Validations
            assertNotNull(isDeleted);
            assertFalse(isDeleted);
        }
    }

    /**
     * Testing
     * {@link ContentletAPI#unarchive(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void unarchive() throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //First lets archive this given contentlet (means it will be mark it as deleted)
        contentletAPI.archive(contentlet, user, false);

        //Now lets test the unarchive
        contentletAPI.unarchive(contentlet, user, false);

        //Verify if it continues as deleted
        Boolean isDeleted = APILocator.getVersionableAPI().isDeleted(contentlet);

        //Validations
        assertNotNull(isDeleted);
        assertFalse(isDeleted);
    }

    /**
     * Testing
     * {@link ContentletAPI#deleteAllVersionsandBackup(java.util.List,
     * com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteAllVersionsAndBackup() throws DotSecurityException, DotDataException {

        //First lets create a test structure
        Structure testStructure = createStructure(
                "JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                "junit_test_structure_" + String.valueOf(new Date().getTime()));

        //Now a new contentlet
        Contentlet newContentlet = createContentlet(testStructure, null, false);
        Identifier contentletIdentifier = APILocator.getIdentifierAPI()
                .find(newContentlet.getIdentifier());

        //Now test this delete
        List<Contentlet> testContentlets = new ArrayList<>();
        testContentlets.add(newContentlet);
        contentletAPI.deleteAllVersionsandBackup(testContentlets, user, false);

        //Try to find the versions for this Contentlet (Must be only one version)
        List<Contentlet> versions = contentletAPI.findAllVersions(contentletIdentifier, user,
                false);

        //Validations
        assertNotNull(versions);
        assertEquals(versions.size(), 1);
        APILocator.getStructureAPI().delete(testStructure, user);
    }

    /**
     * Testing {@link ContentletAPI#delete(java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteCollection() throws DotSecurityException, DotDataException {

        //First lets create a test structure
        Structure testStructure = createStructure(
                "JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                "junit_test_structure_" + String.valueOf(new Date().getTime()));

        //Now a new contentlet
        Contentlet newContentlet = createContentlet(testStructure, null, false);

        //Now test this delete
        contentletAPI.archive(newContentlet, user, false);
        List<Contentlet> testContentlets = new ArrayList<>();
        testContentlets.add(newContentlet);
        contentletAPI.delete(testContentlets, user, false);

        //Try to find the deleted Contentlet
        Contentlet foundContentlet = contentletAPI.find(newContentlet.getInode(), user, false);

        //Validations
        assertTrue(foundContentlet == null || foundContentlet.getInode() == null
                || foundContentlet.getInode().isEmpty());
        APILocator.getStructureAPI().delete(testStructure, user);
    }

    /**
     * Testing
     * {@link ContentletAPI#delete(java.util.List, com.liferay.portal.model.User, boolean,
     * boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteCollectionAllVersions() throws DotSecurityException, DotDataException {

        Language language = APILocator.getLanguageAPI().getDefaultLanguage();

        //First lets create a test structure
        Structure testStructure = createStructure(
                "JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                "junit_test_structure_" + String.valueOf(new Date().getTime()));

        //Create a new contentlet with one version
        Contentlet newContentlet1 = createContentlet(testStructure, language, false);

        //Create a new contentlet with two versions
        Contentlet newContentlet2 = createContentlet(testStructure, language, false);

        // new inode to create the second version
        String newInode = UUIDGenerator.generateUuid();
        newContentlet2.setInode(newInode);

        List<Language> languages = APILocator.getLanguageAPI().getLanguages();
        for (Language localLanguage : languages) {
            if (localLanguage.getId() != language.getId()) {
                language = localLanguage;
                break;
            }
        }

        newContentlet2.setLanguageId(language.getId());

        newContentlet2 = contentletAPI.checkin(newContentlet2, user, false);

        //Now test this delete
        List<Contentlet> testContentlets = new ArrayList<>();
        testContentlets.add(newContentlet1);
        testContentlets.add(newContentlet2);
        contentletAPI.delete(testContentlets, user, false, true);

        //Try to find the deleted Contentlets
        Identifier contentletIdentifier = APILocator.getIdentifierAPI()
                .find(newContentlet1.getIdentifier());
        List<Contentlet> foundContentlets = contentletAPI.findAllVersions(contentletIdentifier,
                user, false);

        //Validations for newContentlet1
        assertTrue(foundContentlets == null || foundContentlets.isEmpty());

        contentletIdentifier = APILocator.getIdentifierAPI().find(newContentlet2.getIdentifier());
        foundContentlets = contentletAPI.findAllVersions(contentletIdentifier, user, false);

        //Validations for newContentlet2
        assertTrue(foundContentlets == null || foundContentlets.isEmpty());
        APILocator.getStructureAPI().delete(testStructure, user);
    }

    /**
     * Testing
     * {@link
     * ContentletAPI#deleteRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * com.dotmarketing.portlets.structure.model.Relationship, com.liferay.portal.model.User,
     * boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteRelatedContent() throws DotSecurityException, DotDataException {

        Structure testStructure = null;
        Relationship testRelationship = null;

        try {
            //First lets create a test structure
            testStructure =
                    createStructure("JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                            "junit_test_structure_" + String.valueOf(new Date().getTime()));

            //Now a new test contentlets
            Contentlet baseContentlet = createContentlet(testStructure, null, false);
            Contentlet contentToRelateAsChild = createContentlet(testStructure, null, false);
            Contentlet contentToRelateAsParent = createContentlet(testStructure, null, false);

            //Create the relationship
            testRelationship = createRelationShip(testStructure.getInode(),
                    testStructure.getInode(),
                    false, 1);

            //Relate content as child
            List<Contentlet> childrenList = new ArrayList<>();
            childrenList.add(contentToRelateAsChild);
            ContentletRelationships childrenRelationships = createContentletRelationships(
                    testRelationship,
                    baseContentlet, testStructure, childrenList, true);

            //Relate content as parent
            List<Contentlet> parentList = new ArrayList<>();
            parentList.add(contentToRelateAsParent);
            ContentletRelationships parentRelationshis = createContentletRelationships(
                    testRelationship,
                    baseContentlet, testStructure, parentList, false);

            //Relate contents to our test contentlet
            for (final ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords : childrenRelationships
                    .getRelationshipsRecords()) {
                contentletAPI.relateContent(baseContentlet, contentletRelationshipRecords, user,
                        false);
            }

            //Relate contents to our test contentlet
            for (ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords : parentRelationshis
                    .getRelationshipsRecords()) {
                contentletAPI.relateContent(baseContentlet, contentletRelationshipRecords, user,
                        false);
            }

            // Let's delete only the children (1 child)
            contentletAPI.deleteRelatedContent(baseContentlet, testRelationship, true, user, false);

            // we should have only one content (1 parent) since we just deleted the one child
            List<Contentlet> foundContentlets = relationshipAPI.dbRelatedContent(testRelationship,
                    baseContentlet,
                    false);
            assertTrue(!foundContentlets.isEmpty() && foundContentlets.size() == 1);

            // Let's now delete the parent
            contentletAPI.deleteRelatedContent(baseContentlet, testRelationship, false, user,
                    false);

            // we should get no content back for both hasParent `true` and `false` since the one child and one parent were deleted
            foundContentlets = relationshipAPI.dbRelatedContent(testRelationship, baseContentlet,
                    false);
            assertTrue(!UtilMethods.isSet(foundContentlets));

            foundContentlets = relationshipAPI.dbRelatedContent(testRelationship, baseContentlet,
                    true);
            assertTrue(!UtilMethods.isSet(foundContentlets));
        } finally {
            try {
                if (testRelationship != null && testRelationship.getInode() != null) {
                    relationshipAPI.delete(testRelationship);
                }
                if (testStructure != null && testStructure != null) {
                    APILocator.getStructureAPI().delete(testStructure, user);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Testing
     * {@link
     * ContentletAPI#deleteRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * com.dotmarketing.portlets.structure.model.Relationship, boolean,
     * com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteRelatedContentWithParent() throws DotSecurityException, DotDataException {

        Structure testStructure = null;
        Relationship testRelationship = null;

        try {
            //First lets create a test structure
            testStructure = createStructure(
                    "JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                    "junit_test_structure_" + String.valueOf(new Date().getTime()));

            //Now a new test contentlets
            Contentlet parentContentlet = createContentlet(testStructure, null, false);
            Contentlet childContentlet = createContentlet(testStructure, null, false);

            //Create the relationship
            testRelationship = createRelationShip(testStructure, false);

            //Create the contentlet relationships
            List<Contentlet> contentRelationships = new ArrayList<>();
            contentRelationships.add(childContentlet);
            ContentletRelationships contentletRelationships = createContentletRelationships(
                    testRelationship, parentContentlet, testStructure, contentRelationships);

            //Relate contents to our test contentlet
            for (ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords : contentletRelationships
                    .getRelationshipsRecords()) {
                contentletAPI.relateContent(parentContentlet, contentletRelationshipRecords, user,
                        false);
            }

            Boolean hasParent = APILocator.getRelationshipAPI()
                    .isParent(testRelationship, parentContentlet.getStructure());

            //Now test this delete
            contentletAPI.deleteRelatedContent(parentContentlet, testRelationship, hasParent, user,
                    false);

            //Try to find the deleted Contentlet
            List<Contentlet> foundContentlets = contentletAPI
                    .getRelatedContent(parentContentlet, testRelationship, user, false);

            //Validations
            assertTrue(foundContentlets == null || foundContentlets.isEmpty());
        } finally {
            if (testRelationship != null && testRelationship.getInode() != null) {
                relationshipAPI.delete(testRelationship);
            }

            if (testStructure != null && testStructure.getInode() != null) {
                APILocator.getStructureAPI().delete(testStructure, user);
            }
        }
    }

    /**
     * Testing
     * {@link ContentletAPI#relateContent(Contentlet,
     * ContentletRelationships.ContentletRelationshipRecords, com.liferay.portal.model.User,
     * boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void relateContent() throws DotSecurityException, DotDataException {

        Structure testStructure = null;
        Relationship testRelationship = null;
        try {
            //First lets create a test structure
            testStructure = createStructure(
                    "JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                    "junit_test_structure_" + String.valueOf(new Date().getTime()));

            //Now a new test contentlets
            Contentlet parentContentlet = createContentlet(testStructure, null, false);
            Contentlet childContentlet = createContentlet(testStructure, null, false);

            //Create the relationship
            testRelationship = createRelationShip(testStructure, false);

            //Create the contentlet relationships
            List<Contentlet> contentRelationships = new ArrayList<>();
            contentRelationships.add(childContentlet);
            ContentletRelationships contentletRelationships = createContentletRelationships(
                    testRelationship, parentContentlet, testStructure, contentRelationships);

            //Relate contents to our test contentlet
            for (ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords : contentletRelationships
                    .getRelationshipsRecords()) {
                //Testing the relate content...
                contentletAPI.relateContent(parentContentlet, contentletRelationshipRecords, user,
                        false);
            }

            //Verify if the content was related
            Tree tree = TreeFactory
                    .getTree(parentContentlet.getIdentifier(), childContentlet.getIdentifier(),
                            testRelationship.getRelationTypeValue());

            //Validations
            assertNotNull(tree);
            assertNotNull(tree.getParent());
            assertNotNull(tree.getChild());
            assertEquals(tree.getParent(), parentContentlet.getIdentifier());
            assertEquals(tree.getChild(), childContentlet.getIdentifier());
            assertEquals(tree.getRelationType(), testRelationship.getRelationTypeValue());

        } finally {
            if (testRelationship != null && testRelationship.getInode() != null) {
                relationshipAPI.delete(testRelationship);
            }

            if (testStructure != null && testStructure.getInode() != null) {
                APILocator.getStructureAPI().delete(testStructure, user);
            }
        }
    }

    /**
     * Testing
     * {@link ContentletAPI#relateContent(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * com.dotmarketing.portlets.structure.model.Relationship, java.util.List,
     * com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void relateContentDirect() throws DotSecurityException, DotDataException {

        Relationship testRelationship = null;
        Structure testStructure = null;
        try {
            //First lets create a test structure
            testStructure = createStructure(
                    "JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                    "junit_test_structure_" + String.valueOf(new Date().getTime()));

            //Now a new test contentlets
            Contentlet parentContentlet = createContentlet(testStructure, null, false);
            Contentlet childContentlet = createContentlet(testStructure, null, false);

            //Create the relationship
            testRelationship = createRelationShip(testStructure, false);

            //Create the contentlet relationships
            List<Contentlet> contentRelationships = new ArrayList<>();
            contentRelationships.add(childContentlet);

            //Relate the content
            contentletAPI.relateContent(parentContentlet, testRelationship, contentRelationships,
                    user, false);

            //Verify if the content was related
            Tree tree = TreeFactory.getTree(parentContentlet.getIdentifier(),
                    childContentlet.getIdentifier(), testRelationship.getRelationTypeValue());

            //Validations
            assertNotNull(tree);
            assertNotNull(tree.getParent());
            assertNotNull(tree.getChild());
            assertEquals(tree.getParent(), parentContentlet.getIdentifier());
            assertEquals(tree.getChild(), childContentlet.getIdentifier());
            assertEquals(tree.getRelationType(), testRelationship.getRelationTypeValue());

        } finally {
            try {
                if (testRelationship != null && testRelationship.getInode() != null) {
                    relationshipAPI.delete(testRelationship);
                }

                if (testStructure != null && testStructure.getInode() != null) {
                    APILocator.getStructureAPI().delete(testStructure, user);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Testing
     * {@link ContentletAPI#getRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet,
     * com.dotmarketing.portlets.structure.model.Relationship, com.liferay.portal.model.User,
     * boolean)}
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getRelatedContent() throws DotSecurityException, DotDataException {

        Relationship testRelationship = null;
        Structure testStructure = null;
        final long timeMillis = new Date().getTime();
        try {
            //First lets create a test structure
            testStructure = createStructure("JUnit Test Structure_" + timeMillis,
                    "junit_test_structure_" + timeMillis);

            //Now a new test contentlets
            final Contentlet parentContentlet = createContentlet(testStructure, null, false);
            final Contentlet childContentlet = createContentlet(testStructure, null, false);

            //Create the relationship
            testRelationship = createRelationShip(testStructure, false);

            //Create the contentlet relationships
            final List<Contentlet> contentRelationships = new ArrayList<>();
            contentRelationships.add(childContentlet);

            //Relate the content
            contentletAPI
                    .relateContent(parentContentlet, testRelationship, contentRelationships, user,
                            false);

            final List<Relationship> relationships = APILocator.getRelationshipAPI()
                    .byContentType(testStructure);
            //Validations
            assertTrue(relationships != null && !relationships.isEmpty());

            List<Contentlet> foundContentlets;
            for (Relationship relationship : relationships) {
                foundContentlets = contentletAPI
                        .getRelatedContent(parentContentlet, relationship, user, true);
                //Validations
                assertTrue(foundContentlets != null && !foundContentlets.isEmpty());
            }
        } finally {
            if (testRelationship != null && testRelationship.getInode() != null) {
                relationshipAPI.delete(testRelationship);
            }

            if (testStructure != null && testStructure.getInode() != null) {
                APILocator.getStructureAPI().delete(testStructure, user);
            }
        }
    }

    @Test
    public void testCheckInWithSelfRelationInBothParentsAndChildren()
            throws DotSecurityException, DotDataException {

        Relationship testRelationship = null;
        Structure testStructure = null;
        final long timeMillis = new Date().getTime();
        try {
            //First lets create a test structure
            testStructure = createStructure(
                    "JUnit Test Structure_" + timeMillis,
                    "junit_test_structure_" + timeMillis);

            //Now a new test contentlets
            final Contentlet grandParentContentlet = createContentlet(testStructure, null, false);
            Contentlet parentContentlet = new ContentletDataGen(testStructure.id())
                    .setPolicy(IndexPolicy.FORCE)
                    .next();//createContentlet( testStructure, null, false );
            final Contentlet childContentlet = createContentlet(testStructure, null, false);

            //Create the relationship
            testRelationship = createRelationShip(testStructure, false);

            //Create the contentlet relationships
            final ContentletRelationships contentletRelationships = new ContentletRelationships(
                    parentContentlet);
            final List<ContentletRelationshipRecords> records = new ArrayList<>();

            final ContentletRelationshipRecords grandParentRel = contentletRelationships.new ContentletRelationshipRecords(
                    testRelationship, false);
            grandParentRel.setRecords(CollectionsUtils.list(grandParentContentlet));
            records.add(grandParentRel);

            final ContentletRelationshipRecords childRel = contentletRelationships.new ContentletRelationshipRecords(
                    testRelationship, true);
            childRel.setRecords(CollectionsUtils.list(childContentlet));
            records.add(childRel);

            contentletRelationships.setRelationshipsRecords(records);

            parentContentlet = contentletAPI
                    .checkin(parentContentlet, contentletRelationships, null, null, user, false);

            //Validate child content was added correctly
            List<Contentlet> foundContentlets = contentletAPI
                    .getRelatedContent(parentContentlet, testRelationship, true, user, true);
            assertTrue(foundContentlets != null && !foundContentlets.isEmpty());
            assertEquals(childContentlet.getIdentifier(), foundContentlets.get(0).getIdentifier());

            //Validate grandParent content was added correctly
            foundContentlets = contentletAPI
                    .getRelatedContent(parentContentlet, testRelationship, false, user, true);
            assertTrue(foundContentlets != null && !foundContentlets.isEmpty());
            assertEquals(grandParentContentlet.getIdentifier(),
                    foundContentlets.get(0).getIdentifier());

        } finally {
            if (testRelationship != null && testRelationship.getInode() != null) {
                relationshipAPI.delete(testRelationship);
            }

            if (testStructure != null && testStructure.getInode() != null) {
                APILocator.getStructureAPI().delete(testStructure, user);
            }
        }
    }

    /**
     * Testing {@link ContentletAPI#getRelatedContent(Contentlet, Relationship, User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getRelatedContentPullByParent() throws DotSecurityException, DotDataException {

        Relationship testRelationship = null;
        Structure testStructure = null;
        final long timeMillis = new Date().getTime();
        try {
            //First lets create a test structure
            testStructure = createStructure(
                    "JUnit Test Structure_" + timeMillis,
                    "junit_test_structure_" + timeMillis);

            //Now a new test contentlets
            final Contentlet parentContentlet = createContentlet(testStructure, null, false);
            final Contentlet childContentlet = createContentlet(testStructure, null, false);

            //Create the relationship
            testRelationship = createRelationShip(testStructure, false);

            //Create the contentlet relationships
            final List<Contentlet> contentRelationships = new ArrayList<>();
            contentRelationships.add(childContentlet);

            //Relate the content
            contentletAPI
                    .relateContent(parentContentlet, testRelationship, contentRelationships, user,
                            false);

            final boolean hasParent = APILocator.getRelationshipAPI()
                    .isParent(testRelationship, parentContentlet.getStructure());

            final List<Relationship> relationships = APILocator.getRelationshipAPI()
                    .byContentType(testStructure);
            //Validations
            assertTrue(relationships != null && !relationships.isEmpty());

            List<Contentlet> foundContentlets = null;
            for (Relationship relationship : relationships) {
                foundContentlets = contentletAPI
                        .getRelatedContent(parentContentlet, relationship, hasParent, user, true);
                //Validations
                assertTrue(foundContentlets != null && !foundContentlets.isEmpty());
            }


        } finally {
            if (testRelationship != null && testRelationship.getInode() != null) {
                relationshipAPI.delete(testRelationship);
            }

            if (testStructure != null && testStructure.getInode() != null) {
                APILocator.getStructureAPI().delete(testStructure, user);
            }
        }
    }

    /**
     * Now we introduce the case when we wanna add content with the inode & identifier we set. The
     * content should not exists for that inode nor the identifier.
     *
     * @throws Exception if test fails
     */
    @Test
    public void saveContentWithExistingIdentifier() throws Exception {
        Structure testStructure = createStructure(
                "JUnit Test Structure_" + String.valueOf(new Date().getTime()) + "zzz",
                "junit_test_structure_" + String.valueOf(new Date().getTime()) + "zzz");

        Field field = new Field("JUnit Test Text", Field.FieldType.TEXT, Field.DataType.TEXT,
                testStructure, false, true, false, 1, false, false, false);
        FieldFactory.saveField(field);

        Contentlet cont = new Contentlet();
        cont.setStructureInode(testStructure.getInode());
        cont.setStringProperty(field.getVelocityVarName(), "a value");
        cont.setStructureInode(testStructure.getInode());
        cont.setHost(defaultHost.getIdentifier());

        // here comes the existing inode and identifier
        // for this test we generate them using the normal
        // generator but the use case for this is when
        // the content comes from another dotCMS instance
        String inode = UUIDGenerator.generateUuid();
        String identifier = UUIDGenerator.generateUuid();
        cont.setInode(inode);
        cont.setIdentifier(identifier);

        Contentlet saved = contentletAPI.checkin(cont, user, false);

        assertEquals(saved.getInode(), inode);
        assertEquals(saved.getIdentifier(), identifier);

        // the inode should hit the index
        contentletAPI.isInodeIndexed(inode, 2);

        CacheLocator.getContentletCache().clearCache();

        // now lets test with existing content
        Contentlet existing = contentletAPI.find(inode, user, false);
        assertEquals(inode, existing.getInode());
        assertEquals(identifier, existing.getIdentifier());

        // new inode to create a new version
        String newInode = UUIDGenerator.generateUuid();
        existing.setInode(newInode);
        existing.setIndexPolicy(IndexPolicy.FORCE);

        saved = contentletAPI.checkin(existing, user, false);

        assertEquals(newInode, saved.getInode());
        assertEquals(identifier, saved.getIdentifier());

        try {
            APILocator.getStructureAPI().delete(testStructure, user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Making sure we set pub/exp dates on identifier when saving content and we set them back to
     * the content when reading.
     * <p>
     * https://github.com/dotCMS/dotCMS/issues/1763
     */
    @Test
    public void testUpdatePublishExpireDatesFromIdentifier() throws Exception {
        final boolean uniquePublishExpireDatePerLanguages = ContentletTransformer.isUniquePublishExpireDatePerLanguages();
        ContentletTransformer.setUniquePublishExpireDatePerLanguages(true);

        try {
            final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            com.dotcms.contenttype.model.field.Field publishField = new FieldDataGen()
                    .name("Pub Date")
                    .velocityVarName("sysPublishDate")
                    .defaultValue(null)
                    .type(DateField.class)
                    .next();

            com.dotcms.contenttype.model.field.Field expireField = new FieldDataGen()
                    .name("Exp Date")
                    .velocityVarName("sysExpireDate")
                    .defaultValue(null)
                    .type(DateField.class)
                    .next();

            com.dotcms.contenttype.model.field.Field textField = new FieldDataGen()
                    .name("JUnit Test Text")
                    .velocityVarName("title")
                    .next();

            // Creating the test content type
            final ContentType testContentType = new ContentTypeDataGen()
                    .fields(CollectionsUtils.list(textField, expireField, publishField))
                    .publishDateFieldVarName(publishField.variable())
                    .expireDateFieldVarName(expireField.variable())
                    .nextPersisted();

            // some dates to play with

            String date = "2222-08-11 10:20:56";
            Date d1 = dateFormat.parse(date);
            Date d2 = new Date(d1.getTime() + 60000L);
            Date d3 = new Date(d2.getTime() + 60000L);
            Date d4 = new Date(d3.getTime() + 60000L);

            // get default lang and one alternate to play with sibblings
            long deflang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final Language altlang = new LanguageDataGen().nextPersisted();

            // if we save using d1 & d1 then the identifier should
            // have those values after save
            Contentlet c1 = new Contentlet();
            c1.setStructureInode(testContentType.inode());
            c1.setStringProperty(textField.variable(), "c1");
            c1.setDateProperty(publishField.variable(), d1);
            c1.setDateProperty(expireField.variable(), d2);
            c1.setLanguageId(deflang);
            c1.setIndexPolicy(IndexPolicy.FORCE);
            c1 = APILocator.getContentletAPI().checkin(c1, user, false);

            Identifier idenFromCache = APILocator.getIdentifierAPI()
                    .loadFromCache(c1.getIdentifier());
            Logger.info(this, "IdentifierFromCache:" + idenFromCache);

            Identifier ident = APILocator.getIdentifierAPI().find(c1);
            assertNotNull(ident.getSysPublishDate());
            assertNotNull(ident.getSysExpireDate());

            assertEquals(dateFormat.format(d1), dateFormat.format(ident.getSysPublishDate()));
            assertEquals(dateFormat.format(d2), dateFormat.format(ident.getSysExpireDate()));

            // if we save another language version for the same identifier
            // then the identifier should be updated with those dates d3&d4
            Contentlet c2 = new Contentlet();
            c2.setStructureInode(testContentType.inode());
            c2.setStringProperty(textField.variable(), "c2");
            c2.setIdentifier(c1.getIdentifier());
            c2.setDateProperty(publishField.variable(), d3);
            c2.setDateProperty(expireField.variable(), d4);
            c2.setLanguageId(altlang.getId());
            c2.setIndexPolicy(IndexPolicy.FORCE);
            c2 = APILocator.getContentletAPI().checkin(c2, user, false);

            Identifier ident2 = APILocator.getIdentifierAPI().find(c2);
            assertNotNull(ident2.getSysPublishDate());
            assertNotNull(ident2.getSysExpireDate());

            assertEquals(dateFormat.format(d3), dateFormat.format(ident2.getSysPublishDate()));
            assertEquals(dateFormat.format(d4), dateFormat.format(ident2.getSysExpireDate()));

            // the other contentlet should have the same dates if we read it again
            Contentlet c11 = APILocator.getContentletAPI().find(c1.getInode(), user, false);
            assertEquals(dateFormat.format(d3),
                    dateFormat.format(c11.getDateProperty(publishField.variable())));
            assertEquals(dateFormat.format(d4),
                    dateFormat.format(c11.getDateProperty(expireField.variable())));

            Contentlet c21 = APILocator.getContentletAPI().find(c2.getInode(), user, false);
            assertEquals(dateFormat.format(d3),
                    dateFormat.format(c21.getDateProperty(publishField.variable())));
            assertEquals(dateFormat.format(d4),
                    dateFormat.format(c21.getDateProperty(expireField.variable())));

            // also it should be in the index update with the new dates
            String q = "+structureName:" + testContentType.variable() +
                    " +inode:" + c11.getInode() +
                    " +" + testContentType.variable() + "." + publishField.variable() + ":"
                    + DateUtil
                    .toLuceneDateTime(d3) +
                    " +" + testContentType.variable() + "." + expireField.variable() + ":"
                    + DateUtil
                    .toLuceneDateTime(d4);

            final long count = APILocator.getContentletAPI().indexCount(q, user, false);
            assertEquals(1, count);
        } finally {
            ContentletTransformer.setUniquePublishExpireDatePerLanguages(
                    uniquePublishExpireDatePerLanguages);
        }
    }

    @Test
    public void rangeQuery() throws Exception {
        // https://github.com/dotCMS/dotCMS/issues/2630
        Structure testStructure = createStructure(
                "JUnit Test Structure_" + String.valueOf(new Date().getTime()) + "zzzvv",
                "junit_test_structure_" + String.valueOf(new Date().getTime()) + "zzzvv");
        Field field = new Field("JUnit Test Text", Field.FieldType.TEXT, Field.DataType.TEXT,
                testStructure, false, true, true, 1, false, false, false);
        field = FieldFactory.saveField(field);

        List<Contentlet> list = new ArrayList<>();
        String[] letters = {"a", "b", "c", "d", "e", "f", "g"};
        for (String letter : letters) {
            Contentlet conn = new Contentlet();
            conn.setStructureInode(testStructure.getInode());
            conn.setStringProperty(field.getVelocityVarName(), letter);
            conn.setIndexPolicy(IndexPolicy.FORCE);
            conn = contentletAPI.checkin(conn, user, false);
            list.add(conn);
        }
        String query = "+structurename:" + testStructure.getVelocityVarName() +
                " +" + testStructure.getVelocityVarName() + "." + field.getVelocityVarName()
                + ":[b   TO f ]";
        String sort =
                testStructure.getVelocityVarName() + "." + field.getVelocityVarName() + " asc";
        List<Contentlet> search = contentletAPI.search(query, 100, 0, sort, user, false);
        assertEquals(5, search.size());
        assertEquals("b", search.get(0).getStringProperty(field.getVelocityVarName()));
        assertEquals("c", search.get(1).getStringProperty(field.getVelocityVarName()));
        assertEquals("d", search.get(2).getStringProperty(field.getVelocityVarName()));
        assertEquals("e", search.get(3).getStringProperty(field.getVelocityVarName()));
        assertEquals("f", search.get(4).getStringProperty(field.getVelocityVarName()));

        try {
            contentletAPI.archive(list, user, false);
            contentletAPI.delete(list, user, false);
            FieldFactory.deleteField(field);
            APILocator.getStructureAPI().delete(testStructure, user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void widgetInvalidateAllLang() throws Exception {

        boolean defaultContentToDefaultLangOriginalValue =
                Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false);
        try {

            Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", true);

            HttpServletRequest requestProxy = new MockInternalRequest().request();
            HttpServletResponse responseProxy = Mockito.mock(HttpServletResponse.class);

            initMessages();

            Language def = APILocator.getLanguageAPI().getDefaultLanguage();
            Contentlet w = new Contentlet();
            w.setStructureInode(simpleWidgetContentType.id());
            w.setStringProperty("widgetTitle", "A testing widget " + UUIDGenerator.generateUuid());
            w.setStringProperty("code", "Initial code");
            w.setLanguageId(def.getId());
            w.setIndexPolicy(IndexPolicy.FORCE);
            w.setIndexPolicyDependencies(IndexPolicy.FORCE);
            w = contentletAPI.checkin(w, user, false);
            contentletAPI.publish(w, user, false);

            /*
             * For every language we should get the same content and contentMap template code
             */
            VelocityEngine engine = VelocityUtil.getEngine();
            SimpleNode contentTester = engine.getRuntimeServices()
                    .parse(new StringReader("code:$code"), "tester1");

            contentTester.init(null, null);

            requestProxy.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
            requestProxy.setAttribute(com.liferay.portal.util.WebKeys.USER,
                    APILocator.getUserAPI().getSystemUser());

            org.apache.velocity.Template teng1 = engine.getTemplate(
                    File.separator + PageMode.LIVE.name() + File.separator + w.getIdentifier()
                            + "_1" +
                            StringPool.UNDERLINE + VariantAPI.DEFAULT_VARIANT.name() + "."
                            + VelocityType.CONTENT.fileExtension);
            org.apache.velocity.Template tesp1 = engine.getTemplate(
                    File.separator + PageMode.LIVE.name() + File.separator + w.getIdentifier() + "_"
                            + spanishLanguage.getId() + StringPool.UNDERLINE
                            + VariantAPI.DEFAULT_VARIANT.name() + "."
                            + VelocityType.CONTENT.fileExtension);

            Context ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
            StringWriter writer = new StringWriter();
            teng1.merge(ctx, writer);
            contentTester.render(new InternalContextAdapterImpl(ctx), writer);
            assertEquals("code:Initial code", writer.toString());
            ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
            writer = new StringWriter();
            tesp1.merge(ctx, writer);
            contentTester.render(new InternalContextAdapterImpl(ctx), writer);
            assertEquals("code:Initial code", writer.toString());

            Contentlet w2 = contentletAPI.checkout(w.getInode(), user, false);
            w2.setStringProperty("code", "Modified Code to make templates different");
            w2 = contentletAPI.checkin(w2, user, false);
            contentletAPI.publish(w2, user, false);
            contentletAPI.isInodeIndexed(w2.getInode(), true);
            VelocityResourceKey key = new VelocityResourceKey(w2, PageMode.LIVE,
                    spanishLanguage.getId());
            CacheLocator.getVeloctyResourceCache().remove(key);

            // now if everything have been cleared correctly those should match again
            org.apache.velocity.Template teng3 = engine.getTemplate(
                    File.separator + PageMode.LIVE.name() + File.separator + w.getIdentifier()
                            + "_1"
                            + StringPool.UNDERLINE + VariantAPI.DEFAULT_VARIANT.name() + "."
                            + VelocityType.CONTENT.fileExtension);
            org.apache.velocity.Template tesp3 = engine.getTemplate(
                    File.separator + PageMode.LIVE.name() + File.separator + w.getIdentifier() + "_"
                            + spanishLanguage.getId() + StringPool.UNDERLINE
                            + VariantAPI.DEFAULT_VARIANT.name() + "."
                            + VelocityType.CONTENT.fileExtension);
            ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
            writer = new StringWriter();
            teng3.merge(ctx, writer);
            contentTester.render(new InternalContextAdapterImpl(ctx), writer);
            assertEquals("code:Modified Code to make templates different", writer.toString());
            ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
            writer = new StringWriter();
            tesp3.merge(ctx, writer);
            contentTester.render(new InternalContextAdapterImpl(ctx), writer);
            assertEquals("code:Modified Code to make templates different", writer.toString());

            // clean up

        } finally {
            Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE",
                    defaultContentToDefaultLangOriginalValue);
        }

    }

    @Test
    public void testFileCopyOnSecondLanguageVersion()
            throws DotDataException, DotSecurityException {

        final String contentTypeName = "structure2709" + System.currentTimeMillis();

        // Structure
        Structure testStructure = new Structure();

        testStructure.setDefaultStructure(false);
        testStructure.setDescription(contentTypeName);
        testStructure.setFixed(false);
        testStructure.setIDate(new Date());
        testStructure.setName(contentTypeName);
        testStructure.setOwner(user.getUserId());
        testStructure.setDetailPage("");
        testStructure.setStructureType(BaseContentType.CONTENT.getType());
        testStructure.setType("structure");
        testStructure.setVelocityVarName(contentTypeName);

        StructureFactory.saveStructure(testStructure);

        Permission permissionRead = new Permission(testStructure.getInode(),
                APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                PermissionAPI.PERMISSION_READ);
        Permission permissionEdit = new Permission(testStructure.getInode(),
                APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                PermissionAPI.PERMISSION_EDIT);
        Permission permissionWrite = new Permission(testStructure.getInode(),
                APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                PermissionAPI.PERMISSION_WRITE);

        APILocator.getPermissionAPI().save(permissionRead, testStructure, user, false);
        APILocator.getPermissionAPI().save(permissionEdit, testStructure, user, false);
        APILocator.getPermissionAPI().save(permissionWrite, testStructure, user, false);

        // Fields

        // title
        Field title = new Field();
        title.setFieldName("testTitle2709");
        title.setFieldType(FieldType.TEXT.toString());
        title.setListed(true);
        title.setRequired(true);
        title.setSearchable(true);
        title.setStructureInode(testStructure.getInode());
        title.setType("field");
        title.setValues("");
        title.setVelocityVarName("testTitle2709");
        title.setIndexed(true);
        title.setFieldContentlet("text4");
        FieldFactory.saveField(title);

        // file
        Field file = new Field();
        file.setFieldName("testFile2709");
        file.setFieldType(FieldType.FILE.toString());
        file.setListed(true);
        file.setRequired(true);
        file.setSearchable(true);
        file.setStructureInode(testStructure.getInode());
        file.setType("field");
        file.setValues("");
        file.setVelocityVarName("testFile2709");
        file.setIndexed(true);
        file.setFieldContentlet("text1");
        FieldFactory.saveField(file);

        // ENGLISH CONTENT
        Contentlet englishContent = new Contentlet();
        englishContent.setStructureInode(testStructure.getInode());
        englishContent.setLanguageId(1);

        //Create a test file asset
        Contentlet fileA = TestDataUtils
                .getFileAssetContent(true, languageAPI.getDefaultLanguage().getId());

        contentletAPI.setContentletProperty(englishContent, title, "englishTitle2709");
        contentletAPI.setContentletProperty(englishContent, file, fileA.getInode());

        englishContent = contentletAPI.checkin(englishContent, null,
                APILocator.getPermissionAPI().getPermissions(testStructure), user, false);

        // SPANISH CONTENT
        Contentlet spanishContent = new Contentlet();
        spanishContent.setStructureInode(testStructure.getInode());
        spanishContent.setLanguageId(spanishLanguage.getId());
        spanishContent.setIdentifier(englishContent.getIdentifier());

        contentletAPI.setContentletProperty(spanishContent, title, "spanishTitle2709");
        contentletAPI.setContentletProperty(spanishContent, file, fileA.getInode());

        spanishContent = contentletAPI.checkin(spanishContent, null,
                APILocator.getPermissionAPI().getPermissions(testStructure), user, false);
        Object retrivedFile = spanishContent.get("testFile2709");
        assertTrue(retrivedFile != null);
        try {
            HibernateUtil.startTransaction();
            APILocator.getStructureAPI().delete(testStructure, user);
            HibernateUtil.closeAndCommitTransaction();
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            Logger.error(ContentletAPITest.class, e.getMessage());
        }


    }

    @Test
    public void newFileAssetLanguageDifferentThanDefault()
            throws DotSecurityException, DotDataException, IOException {
        long spanish = spanishLanguage.getId();
        Folder folder = APILocator.getFolderAPI().findSystemFolder();
        java.io.File file = java.io.File.createTempFile("texto", ".txt");
        FileUtil.write(file, "helloworld");

        FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(folder, file);
        Contentlet fileInSpanish = fileAssetDataGen.languageId(spanish).nextPersisted();
        Contentlet
                result =
                contentletAPI.findContentletByIdentifier(fileInSpanish.getIdentifier(), false,
                        spanish, user, false);
        assertEquals(fileInSpanish.getInode(), result.getInode());

        fileAssetDataGen.remove(fileInSpanish);
    }

    @Test
    public void newVersionFileAssetLanguageDifferentThanDefault()
            throws DotDataException, IOException, DotSecurityException {
        int english = 1;
        long spanish = spanishLanguage.getId();

        Folder folder = APILocator.getFolderAPI().findSystemFolder();
        java.io.File file = java.io.File.createTempFile("file", ".txt");
        FileUtil.write(file, "helloworld");

        FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(folder, file);
        Contentlet fileAsset = fileAssetDataGen.languageId(english).nextPersisted();

        Contentlet contentletSpanish = contentletAPI.findContentletByIdentifier(
                fileAsset.getIdentifier(), false, english, user, false);
        contentletSpanish = contentletAPI.checkout(contentletSpanish.getInode(), user, false);
        contentletSpanish.setLanguageId(spanish);
        contentletSpanish = contentletAPI.checkin(contentletSpanish, user, false);

        Contentlet resultSpanish = contentletAPI.findContentletByIdentifier(
                fileAsset.getIdentifier(), false, spanish, user, false);
        assertNotNull(resultSpanish);

        Contentlet resultEnglish = contentletAPI.findContentletByIdentifier(
                fileAsset.getIdentifier(), false, english, user, false);
        fileAssetDataGen.remove(resultSpanish);
        fileAssetDataGen.remove(resultEnglish);
    }

    /**
     * Deletes a list of contents
     *
     * @throws Exception
     */
    @Test
    public void deleteMultipleContents()
            throws Exception { // https://github.com/dotCMS/core/issues/7678

        // languages
        int english = 1;
        long spanish = spanishLanguage.getId();

        // new template
        Template template = new TemplateDataGen().nextPersisted();
        // new test folder
        Folder testFolder = new FolderDataGen().nextPersisted();
        // sample pages
        HTMLPageAsset pageEnglish1 = new HTMLPageDataGen(testFolder, template).languageId(english)
                .nextPersisted();
        HTMLPageAsset pageEnglish2 = new HTMLPageDataGen(testFolder, template).languageId(english)
                .nextPersisted();
        contentletAPI.publish(pageEnglish1, user, false);
        contentletAPI.publish(pageEnglish2, user, false);
        // delete counter
        int deleted = 0;
        // Page list
        List<HTMLPageAsset> liveHTMLPages = new ArrayList<>();
        // List of contentlets created for this test.
        List<Contentlet> contentletsCreated = new ArrayList<>();

        liveHTMLPages.add(pageEnglish1);
        liveHTMLPages.add(pageEnglish2);

        //We need to create a new copy of pages for Spanish.
        for (HTMLPageAsset liveHTMLPage : liveHTMLPages) {
            Contentlet htmlPageContentlet = APILocator.getContentletAPI()
                    .checkout(liveHTMLPage.getInode(), user, false);
            htmlPageContentlet.getMap().put("languageId", Long.valueOf(spanish));

            //Checkin and Publish.
            Contentlet working = APILocator.getContentletAPI()
                    .checkin(htmlPageContentlet, user, false);
            APILocator.getContentletAPI().publish(working, user, false);
            APILocator.getContentletAPI().isInodeIndexed(working.getInode(), true);

            contentletsCreated.add(working);
        }

        //Now remove all the pages that we created for this tests.
        APILocator.getContentletAPI().unpublish(contentletsCreated, user, false);
        APILocator.getContentletAPI().archive(contentletsCreated, user, false);
        APILocator.getContentletAPI().delete(contentletsCreated, user, false);

        for (Contentlet contentlet : contentletsCreated) {
            if (APILocator.getContentletAPI().find(contentlet.getInode(), user, false) == null) {
                deleted++;
            }
        }
        // 2 Spanish pages created, 2 should have been deleted
        assertEquals(2, deleted);

        List<Contentlet> liveEnglish = new ArrayList<>();
        for (IHTMLPage page : liveHTMLPages) {
            liveEnglish.add(APILocator.getContentletAPI().find(page.getInode(), user, false));
        }

        APILocator.getContentletAPI().unpublish(liveEnglish, user, false);
        APILocator.getContentletAPI().archive(liveEnglish, user, false);
        APILocator.getContentletAPI().delete(liveEnglish, user, false);

        deleted = 0;
        for (Contentlet contentlet : liveEnglish) {
            if (APILocator.getContentletAPI().find(contentlet.getInode(), user, false) == null) {
                deleted++;
            }
        }

        // 2 English pages created, 2 should have been deleted
        assertEquals(2, deleted);

        // dispose other objects
        FolderDataGen.remove(testFolder);
        TemplateDataGen.remove(template);

    }

    /*
    Creates one content with 3 versions in English and 3 versions Spanish. Delete the Spanish one,
    should delete all the versions in Spanish not only the live/working version.
     */
    @Test
    public void testDelete_GivenMultiLangMultiVersionContent_WhenDeletingOneSpanishVersion_ShouldDeleteAllSpanishVersions()
            throws Exception {
        // languages
        int english = 1;
        long spanish = spanishLanguage.getId();

        ContentType contentType = null;
        com.dotcms.contenttype.model.field.Field textField1 = null;
        com.dotcms.contenttype.model.field.Field textField2 = null;

        Contentlet contentletEnglish = null;
        Contentlet contentletSpanish = null;

        try {
            //Create Content Type.
            contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                    .description("Test ContentType Two Text Fields")
                    .host(defaultHost.getIdentifier())
                    .name("Test ContentType Two Text Fields")
                    .owner("owner")
                    .variable("testContentTypeWithTwoTextFields")
                    .build();

            contentType = contentTypeAPI.save(contentType);

            //Creating Text Field.
            textField1 = ImmutableTextField.builder()
                    .name("Title")
                    .variable("title")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.TEXT)
                    .build();

            textField1 = fieldAPI.save(textField1, user);

            //Creating Text Field.
            textField2 = ImmutableTextField.builder()
                    .name("Body")
                    .variable("body")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.TEXT)
                    .build();

            textField2 = fieldAPI.save(textField2, user);

            contentletEnglish = new ContentletDataGen(contentType.id()).languageId(english)
                    .nextPersisted();
            //new Version
            contentletEnglish = contentletAPI.checkout(contentletEnglish.getInode(), user, false);
            contentletEnglish = contentletAPI.checkin(contentletEnglish, user, false);
            //new Version
            contentletEnglish = contentletAPI.checkout(contentletEnglish.getInode(), user, false);
            contentletEnglish = contentletAPI.checkin(contentletEnglish, user, false);

            Identifier contentletIdentifier = APILocator.getIdentifierAPI()
                    .find(contentletEnglish.getIdentifier());

            int quantityVersions = contentletAPI.findAllVersions(contentletIdentifier, user, false)
                    .size();

            assertEquals(3, quantityVersions);

            contentletSpanish = contentletAPI.checkout(contentletEnglish.getInode(), user, false);
            contentletSpanish.setLanguageId(spanish);
            contentletSpanish = contentletAPI.checkin(contentletSpanish, user, false);
            //new Version
            contentletSpanish = contentletAPI.checkout(contentletSpanish.getInode(), user, false);
            contentletSpanish.setLanguageId(spanish);
            contentletSpanish = contentletAPI.checkin(contentletSpanish, user, false);
            //new Version
            contentletSpanish = contentletAPI.checkout(contentletSpanish.getInode(), user, false);
            contentletSpanish.setLanguageId(spanish);
            contentletSpanish = contentletAPI.checkin(contentletSpanish, user, false);

            quantityVersions = contentletAPI.findAllVersions(contentletIdentifier, user, false)
                    .size();

            assertEquals(6, quantityVersions);

            contentletAPI.archive(contentletSpanish, user, false);
            contentletAPI.delete(contentletSpanish, user, false);

            quantityVersions = contentletAPI.findAllVersions(contentletIdentifier, user, false)
                    .size();

            assertEquals(3, quantityVersions);

        } finally {
            contentTypeAPI.delete(contentType);
        }

    }

    /**
     * This JUnit is to check the fix on Issue 10797 (https://github.com/dotCMS/core/issues/10797)
     * It executes the following: 1) create a new structure 2) create a new field 3) create a
     * contentlet 4) set the contentlet property 5) check the contentlet 6) deletes it all in the
     * end
     *
     * @throws Exception Any exception that may happen
     */
    @Test
    public void test_validateContentlet_contentWithTabDividerField() throws Exception {
        Structure testStructure = null;
        Field tabDividerField = null;

        try {
            // Create test structure
            testStructure = createStructure(
                    "Tab Divider Test Structure_" + String.valueOf(new Date().getTime())
                            + "tab_divider",
                    "tab_divider_test_structure_" + String.valueOf(new Date().getTime())
                            + "tab_divider");

            // Create tab divider field
            tabDividerField = new Field("JUnit Test TabDividerField", FieldType.TAB_DIVIDER,
                    Field.DataType.SECTION_DIVIDER, testStructure, false, true, true, 1, false,
                    false, false);
            tabDividerField = FieldFactory.saveField(tabDividerField);

            // Create the test contentlet
            Contentlet testContentlet = new Contentlet();
            testContentlet.setStructureInode(testStructure.getInode());

            // Set the contentlet property
            contentletAPI.setContentletProperty(testContentlet, tabDividerField,
                    "tabDividerFieldValue");

            // Checking the contentlet
            testContentlet.setIndexPolicy(IndexPolicy.FORCE);
            testContentlet = contentletAPI.checkin(testContentlet, user, false);
        } catch (Exception ex) {
            Logger.error(this,
                    "An error occurred during test_validateContentlet_contentWithTabDividerField",
                    ex);
            throw ex;
        } finally {
            try {
                // Delete field
                FieldFactory.deleteField(tabDividerField);

                // Delete structure
                APILocator.getStructureAPI().delete(testStructure, user);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * https://github.com/dotCMS/core/issues/11950
     */
    @Test
    public void testContentWithTwoBinaryFieldsAndSameFile_afterCheckinShouldContainBothFields() {

        ContentType contentType = null;
        com.dotcms.contenttype.model.field.Field textField = null;
        com.dotcms.contenttype.model.field.Field binaryField1 = null;
        com.dotcms.contenttype.model.field.Field binaryField2 = null;

        Contentlet contentlet = null;

        try {
            //Create Content Type.
            contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                    .description("Test ContentType Two Fields")
                    .host(defaultHost.getIdentifier())
                    .name("Test ContentType Two Fields")
                    .owner("owner")
                    .variable("testContentTypeWithTwoBinaryFields")
                    .build();

            contentType = contentTypeAPI.save(contentType);

            //Save Fields. 1. Text, 2. Binary, 3. Binary.
            //Creating Text Field.
            textField = ImmutableTextField.builder()
                    .name("Title")
                    .variable("title")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.TEXT)
                    .build();

            textField = fieldAPI.save(textField, user);

            //Creating First Binary Field.
            binaryField1 = ImmutableBinaryField.builder()
                    .name("Image 1")
                    .variable("image1")
                    .contentTypeId(contentType.id())
                    .build();

            binaryField1 = fieldAPI.save(binaryField1, user);

            //Creating Second Binary Field.
            binaryField2 = ImmutableBinaryField.builder()
                    .name("Image 2")
                    .variable("image2")
                    .contentTypeId(contentType.id())
                    .build();

            binaryField2 = fieldAPI.save(binaryField2, user);

            //Creating a temporary File to use in the binary fields.
            File imageFile = getTemporaryFolder().newFile("ImageFile.png");
            writeTextIntoFile(imageFile, "This is the same image");

            contentlet = new Contentlet();
            contentlet.setStructureInode(contentType.inode());
            contentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());

            contentlet.setStringProperty(textField.variable(), "Test Content with Same Image");
            contentlet.setBinary(binaryField1.variable(), imageFile);
            contentlet.setBinary(binaryField2.variable(), imageFile);

            contentlet.setIndexPolicy(IndexPolicy.FORCE);
            contentlet = contentletAPI.checkin(contentlet, user, false);

            //Check that the properties still exist.
            assertTrue(contentlet.getMap().containsKey(binaryField1.variable()));
            assertTrue(contentlet.getMap().containsKey(binaryField2.variable()));

            //Check that the properties have value.
            assertTrue(UtilMethods.isSet(contentlet.getMap().get(binaryField1.variable())));
            assertTrue(UtilMethods.isSet(contentlet.getMap().get(binaryField2.variable())));

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            try {
                //Delete Contentlet.
                if (contentlet != null) {

                    contentletAPI.destroy(contentlet, user, false);
                }
                //Deleting Fields.
                if (textField != null) {
                    fieldAPI.delete(textField);
                }
                if (binaryField1 != null) {
                    fieldAPI.delete(binaryField1);
                }
                if (binaryField2 != null) {
                    fieldAPI.delete(binaryField2);
                }
                //Deleting Content Type
                if (contentType != null) {
                    contentTypeAPI.delete(contentType);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * This case should run once this ticket https://github.com/dotCMS/core/issues/12116 is solved
     */
    @Test
    @Ignore
    public void test_saveMultilingualFileAssetBasedOnLegacyFile_shouldKeepBinaryFile()
            throws IOException, DotSecurityException, DotDataException {

        ContentType contentType = null;
        com.dotcms.contenttype.model.field.Field textField = null;
        com.dotcms.contenttype.model.field.Field binaryField = null;

        File imageFile;
        FileAssetDataGen fileAssetDataGen = null;
        Contentlet initialContent = null;
        Contentlet spanishContent = null;
        Contentlet englishContent = null;

        try {

            //Create Content Type.
            contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                    .description("ContentType for Legacy File")
                    .host(defaultHost.getIdentifier())
                    .name("ContentType for Legacy File")
                    .owner("owner")
                    .variable("testContentTypeForLegacyFile")
                    .build();

            contentType = contentTypeAPI.save(contentType);

            //Save Fields. 1. Text, 2. Binary
            //Creating Text Field.
            textField = ImmutableTextField.builder()
                    .name("Title")
                    .variable("title")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.TEXT)
                    .build();

            textField = fieldAPI.save(textField, user);

            //Creating First Binary Field.
            binaryField = ImmutableBinaryField.builder()
                    .name("File")
                    .variable("file")
                    .contentTypeId(contentType.id())
                    .build();

            binaryField = fieldAPI.save(binaryField, user);

            //Creating a temporary binary file
            imageFile = getTemporaryFolder().newFile("BinaryFile.txt");
            writeTextIntoFile(imageFile, "This is the same file");

            initialContent = new Contentlet();
            initialContent.setStructureInode(contentType.inode());
            initialContent.setLanguageId(languageAPI.getDefaultLanguage().getId());

            initialContent.setStringProperty(textField.variable(), "Test Content with Same File");
            initialContent.setBinary(binaryField.variable(), imageFile);

            //Saving initial contentlet
            initialContent = contentletAPI.checkin(initialContent, user, false);

            //File assets creation based on the initial content
            fileAssetDataGen = new FileAssetDataGen(testFolder,
                    initialContent.getBinary(binaryField.variable()));

            //Creating file asset content in Spanish
            spanishContent = fileAssetDataGen.languageId(spanishLanguage.getId()).nextPersisted();

            //Creating content version in English
            englishContent = contentletAPI.checkout(spanishContent.getInode(), user, false);
            englishContent.setLanguageId(1);
            englishContent = contentletAPI.checkin(englishContent, user, false);

            //Check that the properties still exist.
            assertTrue(initialContent.getMap().containsKey(binaryField.variable()));
            assertTrue(spanishContent.getMap().containsKey(FileAssetAPI.BINARY_FIELD));
            assertTrue(englishContent.getMap().containsKey(FileAssetAPI.BINARY_FIELD));

            //Check that the properties have value.
            assertTrue(UtilMethods.isSet(initialContent.getMap().get(binaryField.variable())));
            assertTrue(UtilMethods.isSet(spanishContent.getMap().get(FileAssetAPI.BINARY_FIELD)));
            assertTrue(UtilMethods.isSet(englishContent.getMap().get(FileAssetAPI.BINARY_FIELD)));

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {

            try {
                //Delete initial Contentlet.
                if (initialContent != null) {
                    /*contentletAPI.archive(initialContent, user, false);
                    contentletAPI.delete(initialContent, user, false);*/
                    contentletAPI.destroy(initialContent, user, false);
                }
                //Deleting Fields.
                if (textField != null) {
                    fieldAPI.delete(textField);
                }
                if (binaryField != null) {
                    fieldAPI.delete(binaryField);
                }
                //Deleting Content Type
                if (contentType != null) {
                    contentTypeAPI.delete(contentType);
                }

                if (fileAssetDataGen != null) {

                    if (spanishContent != null) {
                        fileAssetDataGen.remove(spanishContent);
                    }

                    if (englishContent != null) {
                        fileAssetDataGen.remove(englishContent);
                    }

                }
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }
    }


    /*
     * https://github.com/dotCMS/core/issues/11978
     *
     * Creates a new Content Type with a DateTimeField and sets it as Expire Field, saves a new Content a checks that
     * the value of the expire field is set and retrieve correctly
     */
    @Test
    public void contentOnlyWithExpireFieldTest() throws Exception {
        ContentTypeAPIImpl contentTypeApi = (ContentTypeAPIImpl) APILocator.getContentTypeAPI(user);
        long time = System.currentTimeMillis();

        ContentType contentType = ContentTypeBuilder.builder(
                        BaseContentType.getContentTypeClass(BaseContentType.CONTENT.ordinal()))
                .description("ContentTypeWithPublishExpireFields " + time)
                .folder(FolderAPI.SYSTEM_FOLDER)
                .host(Host.SYSTEM_HOST).name("ContentTypeWithPublishExpireFields " + time)
                .owner(APILocator.systemUser().toString()).variable("CTVariable11")
                .expireDateVar("expireDate").build();
        contentType = contentTypeApi.save(contentType);

        assertThat("ContentType exists", contentTypeApi.find(contentType.inode()) != null);

        List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>(
                contentType.fields());

        com.dotcms.contenttype.model.field.Field fieldToSave = FieldBuilder.builder(
                        DateTimeField.class).name("Expire Date").variable("expireDate")
                .contentTypeId(contentType.id()).dataType(DataTypes.DATE).indexed(true).build();
        fields.add(fieldToSave);

        contentType = contentTypeApi.save(contentType, fields);

        Contentlet contentlet = new Contentlet();
        contentlet.setStructureInode(contentType.inode());
        contentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());

        contentlet.setDateProperty(fieldToSave.variable(), new Date(new Date().getTime() + 60000L));

        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet = contentletAPI.checkin(contentlet, user, false);

        contentlet = contentletAPI.find(contentlet.getInode(), user, false);
        Date expireDate = contentlet.getDateProperty("expireDate");

        assertNotNull(expireDate);

        // Deleting content type.
        contentTypeApi.delete(contentType);
    }

    /**
     * This test will: --- Create a content type called "Nested". --- Add  1 Text field called Title
     * --- Add  1 Binary field called File --- Set an application/* as a
     * {@link com.dotcms.contenttype.model.field.BinaryField#ALLOWED_FILE_TYPES} --- Upload a wrong
     * file --- expects DotContentletValidationException
     */
    @Test(expected = DotContentletValidationException.class)
    public void test_validateContentlet_wrong_size_expect_DotContentletValidationException()
            throws Exception {

        ContentType contentType = null;
        com.dotcms.contenttype.model.field.Field textField = null;
        com.dotcms.contenttype.model.field.Field binaryField = null;

        Contentlet contentletA = null;

        // Create Content Type.
        contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                .description("Nested" + System.currentTimeMillis())
                .host(defaultHost.getIdentifier())
                .name("Nested" + System.currentTimeMillis())
                .owner("owner")
                .variable("nested" + System.currentTimeMillis())
                .build();

        contentType = contentTypeAPI.save(contentType);

        // Save Fields. 1. Text
        // Creating Text Field: Title.
        textField = ImmutableTextField.builder()
                .name("Title")
                .variable("title")
                .contentTypeId(contentType.id())
                .dataType(DataTypes.TEXT)
                .build();

        textField = fieldAPI.save(textField, user);

        // Save Fields. 1. Binary
        // Creating Text Field: File.
        binaryField = ImmutableBinaryField.builder()
                .name("file")
                .variable("file")
                .contentTypeId(contentType.id())
                .build();

        binaryField = fieldAPI.save(binaryField, user);

        FieldVariable maxLength = ImmutableFieldVariable.builder().key(BinaryField.MAX_FILE_LENGTH)
                .value("10").fieldId(binaryField.id()).build();
        FieldVariable allowFileTypes = ImmutableFieldVariable.builder()
                .key(BinaryField.ALLOWED_FILE_TYPES).value("application/*, text/*")
                .fieldId(binaryField.id()).build();
        binaryField.constructFieldVariables(Arrays.asList(maxLength, allowFileTypes));
        fieldAPI.save(maxLength, user);
        fieldAPI.save(allowFileTypes, user);

        final File tempTestFile = File
                .createTempFile("csvTest_" + new Date().getTime(), ".txt");
        FileUtils.writeStringToFile(tempTestFile, "Test hi this a test longer than ten characters");

        contentletA = new Contentlet();
        contentletA.setStructureInode(contentType.inode());
        contentletA.setLanguageId(languageAPI.getDefaultLanguage().getId());
        contentletA.setStringProperty(textField.variable(), "A");
        contentletA.setBinary(binaryField, tempTestFile);
        contentletA.setIndexPolicy(IndexPolicy.FORCE);
        contentletA.setIndexPolicyDependencies(IndexPolicy.FORCE);
        contentletA = contentletAPI.checkin(contentletA, user, false);
    }

    /**
     * This test will: --- Create a content type called "Nested". --- Add  1 Text field called Title
     * --- Add  1 Binary field called File --- Set an application/* as a
     * {@link com.dotcms.contenttype.model.field.BinaryField#ALLOWED_FILE_TYPES} --- Upload a wrong
     * file --- expects DotContentletValidationException
     */
    @Test(expected = DotContentletValidationException.class)
    public void test_validateContentlet_wrong_mime_type_expect_DotContentletValidationException()
            throws Exception {

        ContentType contentType = null;
        com.dotcms.contenttype.model.field.Field textField = null;
        com.dotcms.contenttype.model.field.Field binaryField = null;

        Contentlet contentletA = null;

        // Create Content Type.
        contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                .description("Nested" + System.currentTimeMillis())
                .host(defaultHost.getIdentifier())
                .name("Nested" + System.currentTimeMillis())
                .owner("owner")
                .variable("nested" + System.currentTimeMillis())
                .build();

        contentType = contentTypeAPI.save(contentType);

        // Save Fields. 1. Text
        // Creating Text Field: Title.
        textField = ImmutableTextField.builder()
                .name("Title")
                .variable("title")
                .contentTypeId(contentType.id())
                .dataType(DataTypes.TEXT)
                .build();

        textField = fieldAPI.save(textField, user);

        // Save Fields. 1. Binary
        // Creating Text Field: File.
        binaryField = ImmutableBinaryField.builder()
                .name("file")
                .variable("file")
                .contentTypeId(contentType.id())
                .build();

        binaryField = fieldAPI.save(binaryField, user);

        FieldVariable maxLength = ImmutableFieldVariable.builder().key(BinaryField.MAX_FILE_LENGTH)
                .value("10").fieldId(binaryField.id()).build();
        FieldVariable allowFileTypes = ImmutableFieldVariable.builder()
                .key(BinaryField.ALLOWED_FILE_TYPES).value("application/*")
                .fieldId(binaryField.id()).build();
        binaryField.constructFieldVariables(Arrays.asList(maxLength, allowFileTypes));
        fieldAPI.save(maxLength, user);
        fieldAPI.save(allowFileTypes, user);

        final File tempTestFile = File
                .createTempFile("csvTest_" + new Date().getTime(), ".txt");
        FileUtils.writeStringToFile(tempTestFile, "Test");

        contentletA = new Contentlet();
        contentletA.setStructureInode(contentType.inode());
        contentletA.setLanguageId(languageAPI.getDefaultLanguage().getId());
        contentletA.setStringProperty(textField.variable(), "A");
        contentletA.setBinary(binaryField, tempTestFile);
        contentletA.setIndexPolicy(IndexPolicy.FORCE);
        contentletA.setIndexPolicyDependencies(IndexPolicy.FORCE);
        contentletA = contentletAPI.checkin(contentletA, user, false);
    }

    /**
     * This test will: --- Create a content type called "Nested". --- Add only 1 Text field called
     * Title --- Create a Content "A". Save/publish it. --- Create a Content "B". Save/publish it.
     * --- Create a Content "C". Save/publish it. --- Create a 1:N Relationship, Parent and Child
     * same Content Type: Nested --- Relate Content: Parent: A, Child B. --- Relate Content: Parent:
     * B, Child C. --- Edit Content A, update title to "ABC"
     * <p>
     * Before the fix we were getting an exception when editing content A because validateContentlet
     * validates that if there's a 1-N relationship the parent content can't relate to a child that
     * already has a parent; but we were pulling other related content, not just the parents.
     * <p>
     * https://github.com/dotCMS/core/issues/10656
     */
    @Test
    public void test_validateContentlet_noErrors_whenRelationChainSameContentType() {
        ContentType contentType = null;
        com.dotcms.contenttype.model.field.Field textField = null;

        Contentlet contentletA = null;
        Contentlet contentletB = null;
        Contentlet contentletC = null;

        Relationship relationShip = null;

        try {
            // Create Content Type.
            contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                    .description("Nested" + System.currentTimeMillis())
                    .host(defaultHost.getIdentifier())
                    .name("Nested" + System.currentTimeMillis())
                    .owner("owner")
                    .variable("nested" + System.currentTimeMillis())
                    .build();

            contentType = contentTypeAPI.save(contentType);

            // Save Fields. 1. Text
            // Creating Text Field: Title.
            textField = ImmutableTextField.builder()
                    .name("Title")
                    .variable("title")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.TEXT)
                    .build();

            textField = fieldAPI.save(textField, user);

            contentletA = new Contentlet();
            contentletA.setStructureInode(contentType.inode());
            contentletA.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentletA.setStringProperty(textField.variable(), "A");
            contentletA.setIndexPolicy(IndexPolicy.FORCE);
            contentletA.setIndexPolicyDependencies(IndexPolicy.FORCE);
            contentletA = contentletAPI.checkin(contentletA, user, false);

            contentletB = new Contentlet();
            contentletB.setStructureInode(contentType.inode());
            contentletB.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentletB.setStringProperty(textField.variable(), "B");
            contentletB.setIndexPolicy(IndexPolicy.FORCE);
            contentletB.setIndexPolicyDependencies(IndexPolicy.FORCE);
            contentletB = contentletAPI.checkin(contentletB, user, false);

            contentletC = new Contentlet();
            contentletC.setStructureInode(contentType.inode());
            contentletC.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentletC.setStringProperty(textField.variable(), "B");
            contentletC.setIndexPolicy(IndexPolicy.FORCE);
            contentletC.setIndexPolicyDependencies(IndexPolicy.FORCE);
            contentletC = contentletAPI.checkin(contentletC, user, false);

            relationShip = createRelationShip(contentType.inode(),
                    contentType.inode(), false);

            // Relate the content.
            contentletAPI
                    .relateContent(contentletA, relationShip, Lists.newArrayList(contentletB), user,
                            false);
            contentletAPI
                    .relateContent(contentletB, relationShip, Lists.newArrayList(contentletC), user,
                            false);

            Map<Relationship, List<Contentlet>> relationshipListMap = Maps.newHashMap();
            relationshipListMap.put(relationShip, Lists.newArrayList(contentletB));

            contentletA = contentletAPI.checkout(contentletA.getInode(), user, false);
            contentletA.setStringProperty(textField.variable(), "ABC");
            contentletA.setIndexPolicy(IndexPolicy.FORCE);
            contentletA.setIndexPolicyDependencies(IndexPolicy.FORCE);
            contentletA = contentletAPI.checkin(contentletA, relationshipListMap, user, false);
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            try {
                // Delete Relationship.
                if (relationShip != null) {
                    relationshipAPI.delete(relationShip);
                }
                // Delete Contentlet.
                if (contentletA != null) {

                    contentletAPI.destroy(contentletA, user, false);
                }
                if (contentletB != null) {

                    contentletAPI.destroy(contentletA, user, false);
                }
                if (contentletC != null) {

                    contentletAPI.destroy(contentletA, user, false);
                }
                // Deleting Fields.
                if (textField != null) {
                    fieldAPI.delete(textField);
                }
                // Deleting Content Type
                if (contentType != null) {
                    contentTypeAPI.delete(contentType);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testCheckinWithoutVersioning_ShouldDeletePreviousBinary_WhenBinaryIsUpdated()
            throws DotSecurityException, DotDataException, IOException {

        final String FILE_V1_NAME = "textFileVersion1.txt";
        final String FILE_V2_NAME = "textFileVersion2.txt";
        final String FILE_V2_CONTENT = "textFileVersion2 CONTENT";
        ContentType typeWithBinary = null;

        try {
            typeWithBinary = createContentType("testCheckinWithoutVersioning",
                    BaseContentType.CONTENT);
            com.dotcms.contenttype.model.field.Field textField = createTextField("Title",
                    typeWithBinary.id());
            com.dotcms.contenttype.model.field.Field binaryField = createBinaryField("File",
                    typeWithBinary.id());
            File textFileVersion1 = createTempFileWithText(FILE_V1_NAME, FILE_V1_NAME);
            Map<String, Object> fieldValues = Map.of(textField.variable(), "contentV1",
                    binaryField.variable(), textFileVersion1);
            Contentlet contentletWithBinary = createContentWithFieldValues(typeWithBinary.id(),
                    fieldValues);

            // let's verify that newly saved file exists
            assertTrue(getBinaryAsset(contentletWithBinary.getInode(), binaryField.variable(),
                    FILE_V1_NAME).exists());

            File textFileVersion2 = createTempFileWithText(FILE_V2_NAME, FILE_V2_CONTENT);
            // replace old binary with new one
            contentletWithBinary.setBinary(binaryField.variable(), textFileVersion2);
            Contentlet contentWithoutVersioning = contentletAPI.checkinWithoutVersioning(
                    contentletWithBinary,
                    new HashMap<>(), null, permissionAPI.getPermissions(contentletWithBinary), user,
                    false);

            // we've just checkedIn without versioning, so old binary should not exist
            assertFalse(getBinaryAsset(contentletWithBinary.getInode(), binaryField.variable(),
                    FILE_V1_NAME).exists());

            File newBinary = getBinaryAsset(contentWithoutVersioning.getInode(),
                    binaryField.variable(), FILE_V2_NAME);
            // new binary should exist
            assertTrue(newBinary.exists());
            // and content should be the expected
            BufferedReader reader = Files.newReader(newBinary, Charset.defaultCharset());
            String fileContent = reader.readLine();
            assertEquals(fileContent, FILE_V2_CONTENT);

        } finally {
            if (typeWithBinary != null) {
                contentTypeAPI.delete(typeWithBinary);
            }
        }
    }

    @Test
    public void testCheckinWithoutVersioning_ShouldPreserveBinary_WhenOtherFieldsAreUpdated()
            throws DotDataException, DotSecurityException, IOException {
        final String BINARY_NAME = "testCheckinWithoutVersioningBinary.txt";
        final String BINARY_CONTENT = "testCheckinWithoutVersioningBinary CONTENT";
        ContentType typeWithBinary = null;

        try {
            typeWithBinary = createContentType("testCheckinWithoutVersioning",
                    BaseContentType.CONTENT);
            com.dotcms.contenttype.model.field.Field textField = createTextField("Title",
                    typeWithBinary.id());
            com.dotcms.contenttype.model.field.Field binaryField = createBinaryField("File",
                    typeWithBinary.id());
            File textFileVersion1 = createTempFileWithText(BINARY_NAME, BINARY_CONTENT);
            Map<String, Object> fieldValues = Map.of(textField.variable(), "contentV1",
                    binaryField.variable(), textFileVersion1);
            Contentlet contentletWithBinary = createContentWithFieldValues(typeWithBinary.id(),
                    fieldValues);

            // let's verify that newly saved file exists
            assertTrue(getBinaryAsset(contentletWithBinary.getInode(), binaryField.variable(),
                    BINARY_NAME).exists());

            //let's update a field different from the binary
            contentletWithBinary.setStringProperty(textField.variable(), "contentV2");
            Contentlet contentWithoutVersioning = contentletAPI.checkinWithoutVersioning(
                    contentletWithBinary,
                    new HashMap<>(), null, permissionAPI.getPermissions(contentletWithBinary), user,
                    false);

            // let's verify the binary is still in FS
            File binaryFromAssetsDir = getBinaryAsset(contentWithoutVersioning.getInode(),
                    binaryField.variable(), BINARY_NAME);
            assertTrue(binaryFromAssetsDir.exists());

            // let's also verify file content remains the same
            BufferedReader reader = Files.newReader(binaryFromAssetsDir, Charset.defaultCharset());
            String fileContent = reader.readLine();
            assertEquals(fileContent, BINARY_CONTENT);

            // let's verify the reference is still ok
            File binaryFromContentlet = contentWithoutVersioning.getBinary(binaryField.variable());
            assertEquals(binaryFromContentlet.getName(), BINARY_NAME);

        } finally {
            if (typeWithBinary != null) {
                contentTypeAPI.delete(typeWithBinary);
            }
        }

    }

    @Test(expected = DotContentletValidationException.class)
    public void testUniqueTextFieldWithDataTypeWholeNumber()
            throws DotDataException, DotSecurityException {
        String contentTypeName = "contentTypeTxtField" + System.currentTimeMillis();
        ContentType contentType = null;
        try {
            contentType = createContentType(contentTypeName, BaseContentType.CONTENT);
            com.dotcms.contenttype.model.field.Field field = ImmutableTextField.builder()
                    .name("Whole Number Unique")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.INTEGER)
                    .unique(true)
                    .build();
            field = fieldAPI.save(field, user);

            Contentlet contentlet = new Contentlet();
            contentlet.setContentTypeId(contentType.inode());
            contentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentlet.setLongProperty(field.variable(), 1);
            contentlet.setIndexPolicy(IndexPolicy.FORCE);
            contentlet = contentletAPI.checkin(contentlet, user, false);
            contentlet = contentletAPI.find(contentlet.getInode(), user, false);

            Contentlet contentlet2 = new Contentlet();
            contentlet2.setContentTypeId(contentType.inode());
            contentlet2.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentlet2.setLongProperty(field.variable(), 1);
            contentlet2 = contentletAPI.checkin(contentlet2, user, false);


        } finally {
            if (contentType != null) {
                contentTypeAPI.delete(contentType);
            }
        }
    }

    private static class testCaseUniqueTextField {

        String fieldValue1;

        testCaseUniqueTextField(final String fieldValue1) {
            this.fieldValue1 = fieldValue1;
        }
    }

    @DataProvider
    public static Object[] testCasesUniqueTextField() {
        return new Object[]{
                new testCaseUniqueTextField("diffLang"),//test for same value diff lang
                new testCaseUniqueTextField("A+ Student"),
                new testCaseUniqueTextField("\"A+” student"),
                new testCaseUniqueTextField("aaa-bbb-ccc"),
                new testCaseUniqueTextField("valid - field"),
                new testCaseUniqueTextField("with \" quotes"),
                new testCaseUniqueTextField(
                        "with special characters + [ ] { } * ( ) : && ! | ^ ~ ?"),
                new testCaseUniqueTextField("CASEINSENSITIVE"),
                new testCaseUniqueTextField("with chinese characters 好 心 面")
        };
    }

    /**
     * This test is for the unique feature of a text field. It creates a content type with a text
     * field marked as unique, and creates a contentlet in English, then a contentlet in Spanish
     * (unique value does not matter the lang) and finally another contentlet in English and this is
     * when it throws the exception (value it's in lowercase because needs to be case insentitive)
     */
    @Test(expected = DotContentletValidationException.class)
    @UseDataProvider("testCasesUniqueTextField")
    public void testUniqueTextFieldContentlets(final testCaseUniqueTextField testCase)
            throws DotDataException, DotSecurityException {
        String contentTypeName = "contentTypeTxtField" + System.currentTimeMillis();
        ContentType contentType = null;
        try {
            contentType = createContentType(contentTypeName, BaseContentType.CONTENT);
            com.dotcms.contenttype.model.field.Field field = ImmutableTextField.builder()
                    .name("Text Unique")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.TEXT)
                    .unique(true)
                    .build();
            field = fieldAPI.save(field, user);

            //Contentlet in English
            Contentlet contentlet = new Contentlet();
            contentlet.setContentTypeId(contentType.inode());
            contentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentlet.setStringProperty(field.variable(), testCase.fieldValue1);
            contentlet.setIndexPolicy(IndexPolicy.FORCE);
            contentlet.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
            contentlet = contentletAPI.checkin(contentlet, user, false);
            contentlet = contentletAPI.find(contentlet.getInode(), user, false);

            //Contentlet in Spanish (should not be an issue since the unique is per lang)
            Contentlet contentlet2 = new Contentlet();
            contentlet2.setContentTypeId(contentType.inode());
            contentlet2.setLanguageId(spanishLanguage.getId());
            contentlet2.setStringProperty(field.variable(), testCase.fieldValue1);
            contentlet2.setIndexPolicy(IndexPolicy.FORCE);
            contentlet2.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
            contentlet2 = contentletAPI.checkin(contentlet2, user, false);

            //Contentlet in English (throws the error)
            Contentlet contentlet3 = new Contentlet();
            contentlet3.setContentTypeId(contentType.inode());
            contentlet3.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentlet3.setStringProperty(field.variable(), testCase.fieldValue1.toLowerCase());
            contentlet3.setIndexPolicy(IndexPolicy.FORCE);
            contentlet3.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
            contentlet3 = contentletAPI.checkin(contentlet3, user, false);
        } catch (Exception e) {

            if (ExceptionUtil.causedBy(e, DotContentletValidationException.class)) {
                throw new DotContentletValidationException(e.getMessage());
            }

            fail(e.getMessage());
        } finally {
            if (contentType != null) {
                contentTypeAPI.delete(contentType);
            }
        }
    }

    /**
     * Creates a contentlet with a working and a live version with diff values, and tries to create
     * another contentlet with the live value this throws the exception (before we were only
     * checking on the working index).
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test(expected = DotContentletValidationException.class)
    public void testUniqueTextFieldLiveAndWorkingWithDiffValues()
            throws DotDataException, DotSecurityException {
        final String contentTypeName = "contentTypeTxtField" + System.currentTimeMillis();
        final String fieldValueLive = "Live Value";
        final String fieldValueWorking = "Working Value";
        ContentType contentType = null;
        try {
            contentType = createContentType(contentTypeName, BaseContentType.CONTENT);
            com.dotcms.contenttype.model.field.Field field = ImmutableTextField.builder()
                    .name("Text Unique")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.TEXT)
                    .unique(true)
                    .build();
            field = fieldAPI.save(field, user);

            //Contentlet in English
            Contentlet contentlet = new Contentlet();
            contentlet.setContentTypeId(contentType.inode());
            contentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentlet.setStringProperty(field.variable(), fieldValueLive);
            contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
            contentlet = contentletAPI.checkin(contentlet, user, false);
            contentletAPI.publish(contentlet, user, false);

            contentlet = contentletAPI.checkout(contentlet.getInode(), user, false);
            contentlet.setStringProperty(field.variable(), fieldValueWorking);
            contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
            contentletAPI.checkin(contentlet, user, false);

            //Contentlet in English (throws the error)
            Contentlet contentlet3 = new Contentlet();
            contentlet3.setContentTypeId(contentType.inode());
            contentlet3.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentlet3.setStringProperty(field.variable(), fieldValueLive);
            contentlet3.setIndexPolicy(IndexPolicy.WAIT_FOR);
            contentletAPI.checkin(contentlet3, user, false);

        } finally {
            if (contentType != null) {
                contentTypeAPI.delete(contentType);
            }
        }
    }

    /**
     * This one tests the ContentletAPI.checkin with the following signature:
     * <p>
     * checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData,
     * List<Category> cats, List<Permission> selectedPermissions, User user,	boolean
     * respectFrontendRoles)
     */
    @Test
    public void testCheckin1_ExistingContentWithCats_NullCats_ShouldKeepExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet blogContent = null;

        try {
            blogContent = TestDataUtils
                    .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            blogContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            blogContent = contentletAPI.checkin(blogContent, (ContentletRelationships) null,
                    categories,
                    null, user, false);

            // let's check cats saved fine
            List<Category> contentCats = APILocator.getCategoryAPI().getParents(blogContent, user,
                    false);

            assertTrue(contentCats.containsAll(categories));

            Contentlet checkedoutBlogContent = contentletAPI.checkout(blogContent.getInode(), user,
                    false);

            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent,
                    (ContentletRelationships) null,
                    null, null, user, false);

            List<Category> existingCats = APILocator.getCategoryAPI()
                    .getParents(reCheckedinContent, user,
                            false);

            assertTrue(existingCats.containsAll(categories));
        } finally {
            try {
                if (blogContent != null && UtilMethods.isSet(blogContent.getIdentifier())) {
                    contentletAPI.destroy(blogContent, user, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This one tests the ContentletAPI.checkin with the following signature:
     * <p>
     * checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData,
     * List<Category> cats, List<Permission> selectedPermissions, User user, boolean
     * respectFrontendRoles, boolean generateSystemEvent)
     */
    @Test
    public void testCheckin2_ExistingContentWithCats_NullCats_ShouldKeepExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet blogContent = null;

        try {
            blogContent = TestDataUtils
                    .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            blogContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            blogContent =
                    contentletAPI.checkin(blogContent, (Map<Relationship, List<Contentlet>>) null,
                            categories,
                            null, user, false);

            Contentlet checkedoutBlogContent = contentletAPI.checkout(blogContent.getInode(), user,
                    false);

            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent, null,
                    null, null, user, false, false);

            List<Category> existingCats = APILocator.getCategoryAPI()
                    .getParents(reCheckedinContent, user,
                            false);

            assertTrue(existingCats.containsAll(categories));
        } finally {
            try {
                if (blogContent != null && UtilMethods.isSet(blogContent.getIdentifier())) {
                    contentletAPI.destroy(blogContent, user, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This one tests the ContentletAPI.checkin with the following signature:
     * <p>
     * checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships,
     * List<Category> cats , List<Permission> permissions, User user,boolean respectFrontendRoles)
     */
    @Test
    public void testCheckin3_ExistingContentWithCats_NullCats_ShouldKeepExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet blogContent = null;

        try {
            blogContent = TestDataUtils
                    .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            blogContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            blogContent = contentletAPI.checkin(blogContent,
                    (Map<Relationship, List<Contentlet>>) null, categories,
                    null, user, false);

            Contentlet checkedoutBlogContent = contentletAPI.checkout(blogContent.getInode(), user,
                    false);

            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent,
                    (Map<Relationship, List<Contentlet>>) null, null, null, user, false);

            List<Category> existingCats = APILocator.getCategoryAPI()
                    .getParents(reCheckedinContent, user,
                            false);

            assertTrue(existingCats.containsAll(categories));
        } finally {
            try {
                if (blogContent != null && UtilMethods.isSet(blogContent.getIdentifier())) {
                    contentletAPI.destroy(blogContent, user, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This one tests the ContentletAPI.checkin with the following signature:
     * <p>
     * checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships,
     * List<Category> cats, User user,boolean respectFrontendRoles)
     */
    @Test
    public void testCheckin4_ExistingContentWithCats_NullCats_ShouldKeepExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet blogContent = null;

        try {
            blogContent = TestDataUtils
                    .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            blogContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            blogContent = contentletAPI.checkin(blogContent,
                    (Map<Relationship, List<Contentlet>>) null, categories,
                    null, user, false);

            Contentlet checkedoutBlogContent = contentletAPI.checkout(blogContent.getInode(), user,
                    false);

            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent,
                    (Map<Relationship, List<Contentlet>>) null, null, user, false);

            List<Category> existingCats = APILocator.getCategoryAPI()
                    .getParents(reCheckedinContent, user,
                            false);

            assertTrue(existingCats.containsAll(categories));
        } finally {
            try {
                if (blogContent != null && UtilMethods.isSet(blogContent.getIdentifier())) {
                    contentletAPI.destroy(blogContent, user, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testCheckinWithoutVersioning_ExistingContentWithCats_NullCats_ShouldKeepExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet blogContent = null;

        try {
            blogContent = TestDataUtils
                    .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            blogContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            blogContent = contentletAPI.checkin(blogContent,
                    (Map<Relationship, List<Contentlet>>) null, categories,
                    null, user, false);

            Contentlet reCheckedinContent = contentletAPI.checkinWithoutVersioning(blogContent,
                    (Map<Relationship, List<Contentlet>>) null, null, null, user, false);

            List<Category> existingCats = APILocator.getCategoryAPI()
                    .getParents(reCheckedinContent, user,
                            false);

            assertTrue(existingCats.containsAll(categories));
        } finally {
            try {
                if (blogContent != null && UtilMethods.isSet(blogContent.getIdentifier())) {
                    contentletAPI.destroy(blogContent, user, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Test checkin with a non-existing contentlet identifier, that should fail
     */
    @Test(expected = DotDataException.class)
    public void testCheckin_Non_Existing_Identifier_With_Validate_Should_FAIL()
            throws DotDataException {
        Contentlet newsContent = null;

        try {
            newsContent = TestDataUtils
                    .getNewsContent(false, languageAPI.getDefaultLanguage().getId(),
                            newsContentType.id());
            newsContent.setIdentifier(UUIDGenerator.generateUuid());

            final List<Category> categories = getACoupleOfCategories();

            newsContent = contentletAPI.checkin(newsContent, (ContentletRelationships) null,
                    categories,
                    null, user, false);

            fail("Should throw a constrain exception for an unexisting id");
        } catch (Exception e) {

            if (e instanceof DotDataException || ExceptionUtil.causedBy(e,
                    DotDataException.class)) {

                throw new DotDataException(e.getMessage());
            }

            fail("The exception catch should: DotHibernateException and is: " + e.getClass());
        } finally {
            try {
                if (newsContent != null && UtilMethods.isSet(newsContent.getIdentifier())
                        && UtilMethods.isSet(newsContent.getInode())) {
                    contentletAPI.destroy(newsContent, user, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Test checkin with a non-existing contentlet identifier, that should not fail since the non
     * validate is activated
     */
    @Test
    public void testCheckin_Non_Existing_Identifier_With_Not_Validate_Success()
            throws DotDataException, DotSecurityException {
        Contentlet newsContent = null;

        try {
            newsContent = TestDataUtils
                    .getNewsContent(false, languageAPI.getDefaultLanguage().getId(),
                            newsContentType.id());
            String identifier = UUIDGenerator.generateUuid();
            newsContent.setIdentifier(identifier);
            newsContent.setInode(UUIDGenerator.generateUuid());
            newsContent.setBoolProperty(Contentlet.DONT_VALIDATE_ME, true);
            newsContent.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);

            final List<Category> categories = getACoupleOfCategories();

            final Contentlet newsContentReturned = contentletAPI.checkin(newsContent,
                    (ContentletRelationships) null, categories,
                    null, user, false);

            assertNotNull(newsContentReturned);
            assertEquals(newsContentReturned.getIdentifier(), identifier);
            newsContent = newsContentReturned;
        } finally {
            try {
                if (newsContent != null && UtilMethods.isSet(newsContent.getIdentifier())) {
                    contentletAPI.destroy(newsContent, user, false);
                }
            } catch (Exception e) {
                // quiet
            }
        }
    }

    /**
     * This one tests the ContentletAPI.checkin with the following signature:
     * <p>
     * checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData,
     * List<Category> cats, List<Permission> selectedPermissions, User user,	boolean
     * respectFrontendRoles)
     */
    @Test
    public void testCheckin1_ExistingContentWithCats_EmptyCatsList_ShouldWipeExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet newsContent = null;

        try {
            newsContent = TestDataUtils
                    .getNewsContent(false, languageAPI.getDefaultLanguage().getId(),
                            newsContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            newsContent = contentletAPI.checkin(newsContent, (ContentletRelationships) null,
                    categories,
                    null, user, false);

            Contentlet checkedoutNewsContent = contentletAPI.checkout(newsContent.getInode(), user,
                    false);

            checkedoutNewsContent.setIndexPolicy(IndexPolicy.FORCE);
            checkedoutNewsContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutNewsContent,
                    (ContentletRelationships) null,
                    new ArrayList<>(), null, user, false);

            List<Category> existingCats = APILocator.getCategoryAPI()
                    .getParents(reCheckedinContent, user,
                            false);

            assertFalse(UtilMethods.isSet(existingCats));
        } finally {
            try {
                if (newsContent != null && UtilMethods.isSet(newsContent.getIdentifier())) {
                    contentletAPI.destroy(newsContent, user, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This one tests the ContentletAPI.checkin with the following signature:
     * <p>
     * checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData,
     * List<Category> cats, List<Permission> selectedPermissions, User user, boolean
     * respectFrontendRoles, boolean generateSystemEvent)
     */
    @Test
    public void testCheckin2_ExistingContentWithCats_EmptyCatsList_ShouldWipeExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet newsContent = null;

        try {
            newsContent = TestDataUtils
                    .getNewsContent(false, languageAPI.getDefaultLanguage().getId(),
                            newsContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            newsContent = contentletAPI.checkin(newsContent, null, categories,
                    null, user, false, false);

            Contentlet checkedoutNewsContent = contentletAPI.checkout(newsContent.getInode(), user,
                    false);

            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutNewsContent, null,
                    new ArrayList<>(), null, user, false, false);

            List<Category> existingCats = APILocator.getCategoryAPI()
                    .getParents(reCheckedinContent, user,
                            false);

            assertFalse(UtilMethods.isSet(existingCats));
        } finally {
            try {
                if (newsContent != null && UtilMethods.isSet(newsContent.getIdentifier())) {
                    contentletAPI.destroy(newsContent, user, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This one tests the ContentletAPI.checkin with the following signature:
     * <p>
     * checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships,
     * List<Category> cats , List<Permission> permissions, User user,boolean respectFrontendRoles)
     */
    @Test
    public void testCheckin3_ExistingContentWithCats_EmptyCatsList_ShouldWipeExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet newsContent = null;

        try {
            newsContent = TestDataUtils
                    .getNewsContent(false, languageAPI.getDefaultLanguage().getId(),
                            newsContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            newsContent = contentletAPI.checkin(newsContent,
                    (Map<Relationship, List<Contentlet>>) null, categories,
                    null, user, false);

            Contentlet checkedoutNewsContent = contentletAPI.checkout(newsContent.getInode(), user,
                    false);

            checkedoutNewsContent.setIndexPolicy(IndexPolicy.FORCE);
            checkedoutNewsContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutNewsContent,
                    (Map<Relationship, List<Contentlet>>) null, new ArrayList<>(), null, user,
                    false);

            List<Category> existingCats = APILocator.getCategoryAPI()
                    .getParents(reCheckedinContent, user,
                            false);

            assertFalse(UtilMethods.isSet(existingCats));
        } finally {
            try {
                if (newsContent != null && UtilMethods.isSet(newsContent.getIdentifier())) {
                    contentletAPI.destroy(newsContent, user, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This one tests the ContentletAPI.checkin with the following signature:
     * <p>
     * checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships,
     * List<Category> cats, User user,boolean respectFrontendRoles)
     */
    @Test
    public void testCheckin4_ExistingContentWithCats_EmptyCatsList_ShouldWipeExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet newsContent = null;

        try {
            newsContent = TestDataUtils
                    .getNewsContent(false, languageAPI.getDefaultLanguage().getId(),
                            newsContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            newsContent = contentletAPI.checkin(newsContent,
                    (Map<Relationship, List<Contentlet>>) null, categories,
                    user, false);

            Contentlet checkedoutNewsContent = contentletAPI.checkout(newsContent.getInode(), user,
                    false);

            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutNewsContent,
                    (Map<Relationship, List<Contentlet>>) null, new ArrayList<>(), user, false);

            List<Category> existingCats = APILocator.getCategoryAPI()
                    .getParents(reCheckedinContent, user,
                            false);

            assertFalse(UtilMethods.isSet(existingCats));
        } finally {
            try {
                if (newsContent != null && UtilMethods.isSet(newsContent.getIdentifier())) {
                    contentletAPI.destroy(newsContent, user, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testCheckinWithoutVersioning_ExistingContentWithCats_EmptyCats_ShouldWipeExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet newsContent = null;

        try {
            newsContent = TestDataUtils
                    .getNewsContent(false, languageAPI.getDefaultLanguage().getId(),
                            newsContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            newsContent = contentletAPI.checkin(newsContent,
                    (Map<Relationship, List<Contentlet>>) null, categories, null, user,
                    false);

            Contentlet reCheckedinContent = contentletAPI.checkinWithoutVersioning(newsContent,
                    (ContentletRelationships) null, new ArrayList<>(), null, user, false);

            List<Category> existingCats = APILocator.getCategoryAPI()
                    .getParents(reCheckedinContent, user,
                            false);

            assertFalse(UtilMethods.isSet(existingCats));
        } finally {
            try {
                if (newsContent != null && UtilMethods.isSet(newsContent.getIdentifier())) {
                    contentletAPI.destroy(newsContent, user, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testCheckin1_ExistingContentWithRels_NullRels_ShouldKeepExistingRels()
            throws DotDataException, DotSecurityException {

        Contentlet blogContent = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());

        final ContentletRelationships relationships = getACoupleOfChildRelationships(blogContent);
        final Relationship relationship = relationships.getRelationshipsRecords().get(0)
                .getRelationship();
        List<Contentlet> relatedContent = relationships.getRelationshipsRecords().get(0)
                .getRecords();

        final List<Category> categories = getACoupleOfCategories();

        blogContent = contentletAPI.checkin(blogContent, relationships, categories, null, user,
                false);
        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);

        List<Contentlet> relatedContentFromDB = relationshipAPI
                .dbRelatedContent(relationship, blogContent);

        assertTrue(relatedContentFromDB.containsAll(relatedContent));

        Contentlet checkedoutBlogContent = contentletAPI
                .checkout(blogContent.getInode(), user, false);

        checkedoutBlogContent.setIndexPolicy(IndexPolicy.FORCE);
        checkedoutBlogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
        Contentlet reCheckedinContent = contentletAPI
                .checkin(checkedoutBlogContent, (ContentletRelationships) null,
                        null, null, user, false);

        List<Contentlet> existingRelationships = relationshipAPI
                .dbRelatedContent(relationship, reCheckedinContent);

        assertTrue(existingRelationships.containsAll(relatedContent));
    }

    @Test
    public void testCheckin1_ExistingContentWithChildAndParentRels_NullRels_ShouldKeepExistingRels()
            throws DotDataException, DotSecurityException {
        Contentlet blogContent = null;
        Relationship relationship = null;
        List<Contentlet> relatedContent = null;

        try {

            blogContent = TestDataUtils
                    .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            blogContentType.id());

            final ContentletRelationships relationships = getACoupleOfParentAndChildrenSelfJoinRelationships(
                    blogContent);
            relationship = relationships.getRelationshipsRecords().get(0).getRelationship();
            relatedContent = relationships.getRelationshipsRecords().get(0).getRecords();

            final List<Category> categories = getACoupleOfCategories();
            blogContent.setIndexPolicy(IndexPolicy.FORCE);
            blogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
            blogContent = contentletAPI.checkin(blogContent, relationships, categories, null, user,
                    false);

            List<Contentlet> relatedContentFromDB = relationshipAPI.dbRelatedContent(relationship,
                    blogContent, false);

            assertTrue(relatedContentFromDB.containsAll(relatedContent));

            Contentlet checkedoutBlogContent = contentletAPI.checkout(blogContent.getInode(), user,
                    false);

            checkedoutBlogContent.setIndexPolicy(IndexPolicy.FORCE);
            checkedoutBlogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent,
                    (ContentletRelationships) null,
                    null, null, user, false);

            List<Contentlet> existingRelationships = relationshipAPI.dbRelatedContent(relationship,
                    reCheckedinContent, false);

            assertTrue(existingRelationships.containsAll(relatedContent));
        } finally {

            try {
                if (InodeUtils.isSet(blogContent.getInode())) {
                /*contentletAPI.archive(blogContent, user, false);
                contentletAPI.delete(blogContent, user, false);*/
                    contentletAPI.destroy(blogContent, user, false);
                }

                if (UtilMethods.isSet(relatedContent)) {
                    relatedContent.forEach(content -> {
                        try {
                        /*contentletAPI.archive(content, user, false);
                        contentletAPI.delete(content, user, false);*/
                            contentletAPI.destroy(content, user, false);
                        } catch (DotDataException | DotSecurityException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }

                if (relationship != null && relationship.getInode() != null) {
                    relationshipAPI.delete(relationship);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testCheckin2_ExistingContentWithRels_NullRels_ShouldKeepExistingRels()
            throws DotDataException, DotSecurityException {

        Contentlet blogContent = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());

        final ContentletRelationships relationships = getACoupleOfChildRelationships(blogContent);

        final List<Category> categories = getACoupleOfCategories();
        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
        blogContent = contentletAPI.checkin(blogContent, relationships,
                categories, null, user, false, false);

        Contentlet checkedoutBlogContent = contentletAPI
                .checkout(blogContent.getInode(), user, false);

        checkedoutBlogContent.setIndexPolicy(IndexPolicy.FORCE);
        checkedoutBlogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
        Contentlet reCheckedinContent = contentletAPI
                .checkin(checkedoutBlogContent, (ContentletRelationships) null,
                        null, null, user, false, false);

        Relationship relationship = relationships.getRelationshipsRecords().get(0)
                .getRelationship();

        List<Contentlet> relatedContent = relationships.getRelationshipsRecords().get(0)
                .getRecords();
        assertFalse(relatedContent.isEmpty());

        RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        List<Contentlet> existingRelationships = relationshipAPI
                .dbRelatedContent(relationship, reCheckedinContent);
        assertTrue(existingRelationships.containsAll(relatedContent));
    }

    @Test
    public void testCheckin3_ExistingContentWithRels_NullRels_ShouldKeepExistingRels()
            throws DotDataException, DotSecurityException {

        Contentlet blogContent = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());

        final ContentletRelationships relationships = getACoupleOfChildRelationships(blogContent);

        final List<Category> categories = getACoupleOfCategories();

        final Map<Relationship, List<Contentlet>> relationshipsMap = new HashMap<>();
        relationshipsMap.put(relationships.getRelationshipsRecords().get(0).getRelationship(),
                relationships.getRelationshipsRecords().get(0).getRecords());

        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent = contentletAPI.checkin(blogContent, relationshipsMap, categories,
                null, user, false);

        Contentlet checkedoutBlogContent = contentletAPI
                .checkout(blogContent.getInode(), user, false);

        Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent,
                (Map<Relationship, List<Contentlet>>) null, null, null, user, false);

        Relationship relationship = relationships.getRelationshipsRecords().get(0)
                .getRelationship();

        List<Contentlet> relatedContent = relationships.getRelationshipsRecords().get(0)
                .getRecords();
        assertFalse(relatedContent.isEmpty());

        RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        List<Contentlet> existingRelationships = relationshipAPI
                .dbRelatedContent(relationship, reCheckedinContent);
        assertTrue(existingRelationships.containsAll(relatedContent));
    }

    @Test
    public void testCheckin4_ExistingContentWithRels_NullRels_ShouldKeepExistingRels()
            throws DotDataException, DotSecurityException {

        Contentlet blogContent = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());

        final ContentletRelationships relationships = getACoupleOfChildRelationships(blogContent);

        final List<Category> categories = getACoupleOfCategories();

        final Map<Relationship, List<Contentlet>> relationshipsMap = new HashMap<>();
        relationshipsMap.put(relationships.getRelationshipsRecords().get(0).getRelationship(),
                relationships.getRelationshipsRecords().get(0).getRecords());

        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
        blogContent = contentletAPI.checkin(blogContent, relationshipsMap, categories, user, false);

        Contentlet checkedoutBlogContent = contentletAPI
                .checkout(blogContent.getInode(), user, false);

        checkedoutBlogContent.setIndexPolicy(IndexPolicy.FORCE);
        checkedoutBlogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
        Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent,
                (Map<Relationship, List<Contentlet>>) null, null, user, false);

        Relationship relationship = relationships.getRelationshipsRecords().get(0)
                .getRelationship();

        List<Contentlet> relatedContent = relationships.getRelationshipsRecords().get(0)
                .getRecords();
        assertFalse(relatedContent.isEmpty());

        RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        List<Contentlet> existingRelationships = relationshipAPI
                .dbRelatedContent(relationship, reCheckedinContent);
        assertTrue(existingRelationships.containsAll(relatedContent));
    }

    @Test
    public void testCheckinWithoutVersioning_ExistingContentWithRels_NullRels_ShouldKeepExistingRels()
            throws DotDataException, DotSecurityException {

        Contentlet blogContent = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());

        final ContentletRelationships relationships = getACoupleOfChildRelationships(blogContent);

        final List<Category> categories = getACoupleOfCategories();

        final Map<Relationship, List<Contentlet>> relationshipsMap = new HashMap<>();
        relationshipsMap.put(relationships.getRelationshipsRecords().get(0).getRelationship(),
                relationships.getRelationshipsRecords().get(0).getRecords());
        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
        blogContent = contentletAPI.checkin(blogContent, relationshipsMap, categories, user, false);

        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
        Contentlet reCheckedinContent = contentletAPI.checkinWithoutVersioning(blogContent,
                (ContentletRelationships) null, null, null, user, false);

        Relationship relationship = relationships.getRelationshipsRecords().get(0)
                .getRelationship();

        List<Contentlet> relatedContent = relationships.getRelationshipsRecords().get(0)
                .getRecords();
        assertFalse(relatedContent.isEmpty());

        RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        List<Contentlet> existingRelationships = relationshipAPI
                .dbRelatedContent(relationship, reCheckedinContent);
        assertTrue(existingRelationships.containsAll(relatedContent));
    }

    @Test
    public void testCheckin3_ExistingContentWithRels_EmptyRels_ShouldWipeExistingRels()
            throws DotDataException, DotSecurityException {

        Contentlet blogContent = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());

        final ContentletRelationships relationships = getACoupleOfChildRelationships(blogContent);

        final List<Category> categories = getACoupleOfCategories();

        final ContentletRelationships.ContentletRelationshipRecords relationshipRecords =
                relationships.getRelationshipsRecords().get(0);

        final Relationship relationship = relationshipRecords.getRelationship();

        final Map<Relationship, List<Contentlet>> relationshipsMap = new HashMap<>();
        relationshipsMap.put(relationship,
                relationshipRecords.getRecords());

        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent = contentletAPI.checkin(blogContent, relationshipsMap, categories,
                null, user, false);

        Contentlet checkedoutBlogContent = contentletAPI
                .checkout(blogContent.getInode(), user, false);

        Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent,
                new HashMap<>(), null, null, user, false);

        RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();

        List<Contentlet> existingRelationships = relationshipAPI
                .dbRelatedContent(relationship, reCheckedinContent);
        List<Contentlet> relatedContent = relationships.getRelationshipsRecords().get(0)
                .getRecords();

        assertFalse(relatedContent.isEmpty());
        assertFalse(UtilMethods.isSet(existingRelationships));
    }

    @Test
    public void testCheckin4_ExistingContentWithRels_EmptyRels_ShouldWipeExistingRels()
            throws DotDataException, DotSecurityException {

        Contentlet blogContent = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());

        final ContentletRelationships relationships = getACoupleOfChildRelationships(blogContent);

        final List<Category> categories = getACoupleOfCategories();

        final Map<Relationship, List<Contentlet>> relationshipsMap = new HashMap<>();
        relationshipsMap.put(relationships.getRelationshipsRecords().get(0).getRelationship(),
                relationships.getRelationshipsRecords().get(0).getRecords());

        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent = contentletAPI.checkin(blogContent, relationshipsMap, categories, user, false);

        Contentlet checkedoutBlogContent = contentletAPI
                .checkout(blogContent.getInode(), user, false);

        Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent,
                new HashMap<>(), null, user, false);

        Relationship relationship = relationships.getRelationshipsRecords().get(0)
                .getRelationship();

        List<Contentlet> relatedContent = relationships.getRelationshipsRecords().get(0)
                .getRecords();
        assertFalse(relatedContent.isEmpty());

        RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        List<Contentlet> existingRelationships = relationshipAPI
                .dbRelatedContent(relationship, reCheckedinContent);
        assertFalse(UtilMethods.isSet(existingRelationships));
    }

    @Test
    public void testCheckinWithoutVersioning_ExistingContentWithRels_EmptyRels_ShouldWipeExistingRels()
            throws DotDataException, DotSecurityException {

        Contentlet blogContent = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());

        final ContentletRelationships relationships = getACoupleOfChildRelationships(blogContent);

        final List<Category> categories = getACoupleOfCategories();

        final Map<Relationship, List<Contentlet>> relationshipsMap = new HashMap<>();
        relationshipsMap.put(relationships.getRelationshipsRecords().get(0).getRelationship(),
                relationships.getRelationshipsRecords().get(0).getRecords());

        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent = contentletAPI.checkin(blogContent, relationshipsMap, categories, user, false);

        Contentlet reCheckedinContent = contentletAPI.checkinWithoutVersioning(blogContent,
                new HashMap<>(), null, null, user, false);

        Relationship relationship = relationships.getRelationshipsRecords().get(0)
                .getRelationship();
        List<Contentlet> relatedContent = relationships.getRelationshipsRecords().get(0)
                .getRecords();
        assertFalse(relatedContent.isEmpty());

        RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        List<Contentlet> existingRelationships = relationshipAPI
                .dbRelatedContent(relationship, reCheckedinContent);
        assertFalse(UtilMethods.isSet(existingRelationships));
    }

    @Test
    public void testCopyProperties_TypeWithTagField_shouldCopyTagFieldValue()
            throws DotSecurityException, DotDataException {
        Contentlet newsContent = null;
        try {
            newsContent = TestDataUtils
                    .getNewsContent(false, languageAPI.getDefaultLanguage().getId(),
                            newsContentType.id());
            Map<String, Object> innerMap = new HashMap<>(newsContent.getMap());
            newsContent = contentletAPI.checkin(newsContent, user, false);
            Contentlet checkedoutNewsContent = contentletAPI.checkout(newsContent.getInode(), user,
                    false);
            assertEquals(checkedoutNewsContent.getStringProperty("tags"), innerMap.get("tags"));
            innerMap.put("tags", "newTag");
            contentletAPI.copyProperties(checkedoutNewsContent, innerMap);
            assertEquals(checkedoutNewsContent.getStringProperty("tags"), "newTag");

        } finally {
            try {
                if (newsContent != null && UtilMethods.isSet(newsContent.getIdentifier())) {
                    contentletAPI.destroy(newsContent, user, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ContentletRelationships getACoupleOfParentAndChildrenSelfJoinRelationships(
            final Contentlet contentlet)
            throws DotDataException, DotSecurityException {

        final Relationship selfJoinRelationship = new Relationship();
        selfJoinRelationship.setRelationTypeValue("ParentBlog-ChildBlog");
        selfJoinRelationship.setParentStructureInode(contentlet.getContentTypeId());
        selfJoinRelationship.setChildStructureInode(contentlet.getContentTypeId());
        selfJoinRelationship.setCardinality(1);
        selfJoinRelationship.setParentRelationName("ParentBlog");
        selfJoinRelationship.setChildRelationName("ChildBlog");
        relationshipAPI.save(selfJoinRelationship);

        final ContentletRelationships contentletRelationships = new ContentletRelationships(
                contentlet);

        final ContentletRelationships.ContentletRelationshipRecords parentRecords =
                contentletRelationships.new ContentletRelationshipRecords(selfJoinRelationship,
                        false);
        Contentlet parentBlog1 = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());
        parentBlog1 = contentletAPI.checkin(parentBlog1, new HashMap<>(), getACoupleOfCategories(),
                user, false);
        Contentlet parentBlog2 = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());
        parentBlog2 = contentletAPI.checkin(parentBlog2, new HashMap<>(), getACoupleOfCategories(),
                user, false);
        parentRecords.setRecords(Arrays.asList(parentBlog1, parentBlog2));

        final ContentletRelationships.ContentletRelationshipRecords childRecords =
                contentletRelationships.new ContentletRelationshipRecords(selfJoinRelationship,
                        true);
        Contentlet childBlog1 = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());
        childBlog1 = contentletAPI.checkin(childBlog1, new HashMap<>(), getACoupleOfCategories(),
                user, false);
        Contentlet childBlog2 = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());
        childBlog2 = contentletAPI.checkin(childBlog2, new HashMap<>(), getACoupleOfCategories(),
                user, false);
        childRecords.setRecords(Arrays.asList(childBlog1, childBlog2));

        contentletRelationships.setRelationshipsRecords(
                new ArrayList<>(Arrays.asList(parentRecords, childRecords)));

        return contentletRelationships;
    }

    private ContentletRelationships getACoupleOfChildRelationships(Contentlet contentlet)
            throws DotDataException, DotSecurityException {

        ContentletRelationships relationships = new ContentletRelationships(contentlet);

        Relationship blogComments = APILocator.getRelationshipAPI().byTypeValue(
                "Parent" + blogContentType.variable() + "-" + "Child" + commentsContentType
                        .variable());
        if (blogComments == null) {
            blogComments = new Relationship();
            blogComments.setRelationTypeValue(
                    "Parent" + blogContentType.variable() + "-" + "Child" + commentsContentType
                            .variable());
            blogComments.setParentStructureInode(contentlet.getContentTypeId());
            blogComments.setChildStructureInode(commentsContentType.id());
            blogComments.setCardinality(1);
            blogComments.setParentRelationName("Parent" + blogContentType.variable());
            blogComments.setChildRelationName("Child" + commentsContentType.variable());
            relationshipAPI.save(blogComments);
        }

        ContentletRelationships.ContentletRelationshipRecords records =
                relationships.new ContentletRelationshipRecords(blogComments, true);

        Contentlet comment1 = new Contentlet();
        comment1.setContentTypeId(commentsContentType.id());
        comment1.setStringProperty("title", "comment1");
        comment1.setStringProperty("email", "email");
        comment1.setStringProperty("comment", "comment");
        comment1.setIndexPolicy(IndexPolicy.FORCE);
        comment1.setIndexPolicyDependencies(IndexPolicy.FORCE);

        comment1 = contentletAPI.checkin(comment1, new HashMap<>(), user, false);
        comment1.setIndexPolicy(IndexPolicy.FORCE);
        comment1.setIndexPolicyDependencies(IndexPolicy.FORCE);

        Contentlet comment2 = new Contentlet();
        comment2.setContentTypeId(commentsContentType.id());
        comment2.setStringProperty("title", "comment2");
        comment2.setStringProperty("email", "email");
        comment2.setStringProperty("comment", "comment");
        comment2.setIndexPolicy(IndexPolicy.FORCE);
        comment2.setIndexPolicyDependencies(IndexPolicy.FORCE);

        comment2 = contentletAPI.checkin(comment2, new HashMap<>(), user, false);
        comment2.setIndexPolicy(IndexPolicy.FORCE);
        comment2.setIndexPolicyDependencies(IndexPolicy.FORCE);

        records.setRecords(Arrays.asList(comment1, comment2));
        relationships.setRelationshipsRecords(CollectionsUtils.list(records));

        return relationships;
    }

    @NotNull
    private List<Category> getACoupleOfCategories() throws DotDataException, DotSecurityException {
        return APILocator.getCategoryAPI()
                .findTopLevelCategories(APILocator.systemUser(), false);
    }

    @Test
    public void testDeletePageDefinedAsDetailPage() throws DotSecurityException, DotDataException {

        long time = System.currentTimeMillis();
        ContentType type = null;
        Folder testFolder = null;
        HTMLPageAsset htmlPage = null;
        Template template = null;

        try {
            // new template
            template = new TemplateDataGen().nextPersisted();

            // new test folder
            testFolder = new FolderDataGen().nextPersisted();

            //new html page
            htmlPage = new HTMLPageDataGen(testFolder, template)
                    .languageId(languageAPI.getDefaultLanguage().getId()).nextPersisted();

            //new content type with detail page
            type = ContentTypeBuilder
                    .builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.ordinal()))
                    .description("description" + time).folder(FolderAPI.SYSTEM_FOLDER)
                    .host(Host.SYSTEM_HOST)
                    .name("ContentTypeWithDetailPage" + time).owner("owner")
                    .variable("velocityVarNameTesting" + time)
                    .detailPage(htmlPage.getIdentifier()).urlMapPattern("mapPatternForTesting")
                    .build();
            type = contentTypeAPI.save(type, null, null);

            //html page is removed
            contentletAPI.archive(htmlPage, user, false);

            try {
                contentletAPI.delete(htmlPage, user, false);
                assertTrue("DotWorkflowException expected", false);
            } catch (DotWorkflowException e) {
                //Expected
            }

            //verify that the content type was unlinked from the deleted page
            type = contentTypeAPI.find(type.id());
            assertEquals(htmlPage.getIdentifier(), type.detailPage());
            assertEquals("/mapPatternForTesting", type.urlMapPattern());
        } finally {
            if (type != null) {
                contentTypeAPI.delete(type);
            }

            if (testFolder != null) {
                FolderDataGen.remove(testFolder);
            }

            if (template != null) {
                TemplateDataGen.remove(template);
            }
        }
    }

    @Test
    public void testSearchFileAssetByMetadata()
            throws DotSecurityException, DotDataException {

        //Creating a test file asset
        TestDataUtils.getFileAssetContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId());

        final String query = "+contentType:FileAsset +metaData.contentType:*image/jpeg*";
        List<Contentlet> result = contentletAPI.search(query, 100, 0, null, user, false);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        assertTrue(result.stream().anyMatch(contentlet -> {
            final String fileName = contentlet.getStringProperty("fileName").toLowerCase();
            return fileName.endsWith("jpg") || fileName.endsWith("jpeg");
        }));
    }

    @Test
    public void testSetTemplateForAPageMustKeepTheSameTemplateForWorkingVersionsOnly()
            throws Exception {

        Contentlet spanishPage = null;
        Container container = null;
        Folder folder = null;
        Structure structure = null;

        Template englishTemplate = null;
        Template spanishTemplate = null;

        final String UUID = UUIDGenerator.generateUuid();

        try {
            structure = new StructureDataGen().nextPersisted();
            container = new ContainerDataGen().withStructure(structure, "")
                    .nextPersisted();
            englishTemplate = new TemplateDataGen().title("English Template")
                    .withContainer(container.getIdentifier(), UUID).nextPersisted();
            spanishTemplate = new TemplateDataGen().title("Spanish Template")
                    .withContainer(container.getIdentifier(), UUID).nextPersisted();
            folder = new FolderDataGen().nextPersisted();

            //Create a page in English
            final HTMLPageDataGen htmlPageDataGen = new HTMLPageDataGen(folder, englishTemplate);
            final HTMLPageAsset englishPage = htmlPageDataGen.languageId(1).nextPersisted();

            //Create the Spanish version of the page using a different template
            spanishPage = HTMLPageDataGen.checkout(englishPage);

            spanishPage.setLanguageId(spanishLanguage.getId());
            spanishPage.setProperty(HTMLPageAssetAPI.TEMPLATE_FIELD,
                    spanishTemplate.getIdentifier());
            spanishPage.setProperty(HTMLPageAssetAPI.URL_FIELD, englishPage.getPageUrl() + "SP");

            spanishPage = HTMLPageDataGen.checkin(spanishPage);

            //Verify that both pages have the same template
            assertEquals(spanishPage.get(HTMLPageAssetAPI.TEMPLATE_FIELD),
                    spanishTemplate.getIdentifier());

            //Verify that the initial English version was not modified
            assertEquals(englishTemplate.getIdentifier(),
                    contentletAPI.find(englishPage.getInode(), user, false)
                            .get(HTMLPageAssetAPI.TEMPLATE_FIELD));

            //Verify that a new English version with the Spanish template was created
            Optional<ContentletVersionInfo> info = APILocator.getVersionableAPI()
                    .getContentletVersionInfo(englishPage.getIdentifier(),
                            englishPage.getLanguageId());

            assertTrue(info.isPresent());
            final String newEnglishInode = info.get().getWorkingInode();

            assertEquals(spanishPage.get(HTMLPageAssetAPI.TEMPLATE_FIELD),
                    contentletAPI.find(newEnglishInode, user, false)
                            .get(HTMLPageAssetAPI.TEMPLATE_FIELD));


        } finally {

            //Clean up environment
            try {
                contentletAPI.destroy(spanishPage, user, false);

                if (UtilMethods.isSet(folder) && UtilMethods.isSet(folder.getInode())) {
                    FolderDataGen.remove(folder);
                }

                if (UtilMethods.isSet(spanishTemplate) && UtilMethods.isSet(
                        spanishTemplate.getInode())) {
                    TemplateDataGen.remove(spanishTemplate);
                }

                if (UtilMethods.isSet(englishTemplate) && UtilMethods.isSet(
                        englishTemplate.getInode())) {
                    TemplateDataGen.remove(englishTemplate);
                }

                if (UtilMethods.isSet(container) && UtilMethods.isSet(container.getInode())) {
                    ContainerDataGen.remove(container);
                }

                if (UtilMethods.isSet(structure) && UtilMethods.isSet(structure.getInode())) {
                    StructureDataGen.remove(structure);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Test
    public void testSetTemplateForAPageMustKeepTheSameTemplateForWorkingVersionsNoLive()
            throws Exception {

        Contentlet result = null;
        Container container = null;
        Folder folder = null;
        Structure structure = null;

        Template englishTemplate = null;
        Template spanishTemplate = null;
        final String UUID = UUIDGenerator.generateUuid();

        try {
            structure = new StructureDataGen().nextPersisted();
            container = new ContainerDataGen().withStructure(structure, "")
                    .nextPersisted();
            englishTemplate = new TemplateDataGen().title("English Template")
                    .withContainer(container.getIdentifier(), UUID).nextPersisted();
            spanishTemplate = new TemplateDataGen().title("Spanish Template")
                    .withContainer(container.getIdentifier(), UUID).nextPersisted();
            folder = new FolderDataGen().nextPersisted();

            //Create a page in English
            HTMLPageDataGen htmlPageDataGen = new HTMLPageDataGen(folder, englishTemplate);
            HTMLPageAsset englishPage = htmlPageDataGen.languageId(1).nextPersisted();

            //Publish this page to set it as working and live
            contentletAPI.publish(englishPage, user, false);
            contentletAPI.isInodeIndexed(englishPage.getInode(), true);
            //Create the Spanish version of the page using a different template
            Contentlet spanishPage = HTMLPageDataGen.checkout(englishPage);

            spanishPage.setLanguageId(spanishLanguage.getId());
            spanishPage.setProperty(HTMLPageAssetAPI.TEMPLATE_FIELD,
                    spanishTemplate.getIdentifier());
            spanishPage.setProperty(HTMLPageAssetAPI.URL_FIELD, englishPage.getPageUrl() + "SP");

            result = HTMLPageDataGen.checkin(spanishPage);

            //Verify that the Spanish page has the same template
            assertEquals(result.get(HTMLPageAssetAPI.TEMPLATE_FIELD),
                    spanishTemplate.getIdentifier());

            //Verify that the initial English version was not modified
            assertEquals(englishTemplate.getIdentifier(),
                    contentletAPI.find(englishPage.getInode(), user, false)
                            .get(HTMLPageAssetAPI.TEMPLATE_FIELD));

            //Verify that a new English version with the Spanish template was created
            Optional<ContentletVersionInfo> info = APILocator.getVersionableAPI()
                    .getContentletVersionInfo(englishPage.getIdentifier(),
                            englishPage.getLanguageId());

            assertTrue(info.isPresent());

            final String newEnglishInode = info.get().getWorkingInode();

            assertEquals(spanishPage.get(HTMLPageAssetAPI.TEMPLATE_FIELD),
                    contentletAPI.find(newEnglishInode, user, false)
                            .get(HTMLPageAssetAPI.TEMPLATE_FIELD));

        } finally {

            try {
                //Clean up environment
                contentletAPI.destroy(result, user, false);

                if (UtilMethods.isSet(folder) && UtilMethods.isSet(folder.getInode())) {
                    FolderDataGen.remove(folder);
                }

                if (UtilMethods.isSet(spanishTemplate) && UtilMethods.isSet(
                        spanishTemplate.getInode())) {
                    TemplateDataGen.remove(spanishTemplate);
                }

                if (UtilMethods.isSet(englishTemplate) && UtilMethods.isSet(
                        englishTemplate.getInode())) {
                    TemplateDataGen.remove(englishTemplate);
                }

                if (UtilMethods.isSet(container) && UtilMethods.isSet(container.getInode())) {
                    ContainerDataGen.remove(container);
                }

                if (UtilMethods.isSet(structure) && UtilMethods.isSet(structure.getInode())) {
                    StructureDataGen.remove(structure);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Test
    public void testMoveContentDependenciesFromChild()
            throws DotSecurityException, DotDataException {
        ContentType parentContentType = null;
        ContentType childContentType = null;
        try {
            final long time = System.currentTimeMillis();
            parentContentType = createContentType("parentContentType" + time,
                    BaseContentType.CONTENT);
            childContentType = createContentType("childContentType" + time,
                    BaseContentType.CONTENT);

            //One side of the relationship is set parentContentType --> childContentType
            com.dotcms.contenttype.model.field.Field parentField = createRelationshipField("child",
                    parentContentType.id(), childContentType.variable());

            final Relationship relationship = relationshipAPI
                    .getRelationshipFromField(parentField, user);

            Contentlet childContent = new ContentletDataGen(childContentType.id())
                    .languageId(languageAPI.getDefaultLanguage().getId()).nextPersisted();

            Contentlet parentContent = new ContentletDataGen(parentContentType.id())
                    .languageId(languageAPI.getDefaultLanguage().getId()).next();

            parentContent = contentletAPI.checkin(parentContent, Map.of(relationship, CollectionsUtils.list(childContent)), user, false);

            Map<Relationship, List<Contentlet>> relationshipRecords = contentletAPI
                    .findContentRelationships(parentContent, user);

            assertTrue(relationshipRecords.containsKey(relationship));
            assertEquals(1, relationshipRecords.get(relationship).size());
            assertEquals(childContent.getIdentifier(),
                    relationshipRecords.get(relationship).get(0).getIdentifier());

            //creates a new version of the child
            childContent = contentletAPI.checkout(childContent.getInode(), user, false);
            childContent = contentletAPI
                    .checkin(childContent, (ContentletRelationships) null, null, null,
                            user, false);

            relationshipRecords = contentletAPI
                    .findContentRelationships(parentContent, user);

            assertTrue(relationshipRecords.containsKey(relationship));
            assertEquals(1, relationshipRecords.get(relationship).size());
            assertEquals(childContent.getIdentifier(),
                    relationshipRecords.get(relationship).get(0).getIdentifier());

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
     * Method to test: {@link ContentletAPI#copyContentlet(Contentlet, User, boolean)}
     * Given Scenario: When the user copy a content with a cardinality 1 to 1 the content copied should not have any relationship
     * ExpectedResult: The content copied should not have any relationship.
     *
     */
    @Test
    public void test_resultOfCopyContWithCardinality_OneToOne_shouldNotHaveRelationWithCotents() throws DotDataException, DotSecurityException {

        //Create a child with a text field
        final ContentType childCT = new ContentTypeDataGen().name("child").nextPersisted();
        final com.dotcms.contenttype.model.field.Field uniqueTextField = new FieldDataGen()
                .contentTypeId(childCT.id())
                .unique(false)
                .type(TextField.class)
                .nextPersisted();

        //Create a parent with a relationship to the child
        final ContentType parentCT = new ContentTypeDataGen().nextPersisted();
        //create the relation field
        final com.dotcms.contenttype.model.field.Field parentField = createRelationshipField("children", parentCT,
                childCT.variable(), WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal());
        //get the relationship
        final Relationship relationship = relationshipAPI
                .getRelationshipFromField(parentField, user);


        //create the contentlets
        final Contentlet childContent = new ContentletDataGen(childCT).setProperty("title", "childCont").nextPersisted();

        Contentlet parentContent = new ContentletDataGen(parentCT.id())
                .languageId(languageAPI.getDefaultLanguage().getId()).next();


        parentContent = contentletAPI.checkin(parentContent, Map.of(relationship, CollectionsUtils.list(childContent)), user, false);

        Map<Relationship, List<Contentlet>> relationshipRecords = contentletAPI
                .findContentRelationships(parentContent, user);

        //old content relationship
        final Map<Relationship, List<Contentlet>> oldContRelationShip = contentletAPI.findContentRelationships(childContent, user);
        assertEquals(1, oldContRelationShip.get(relationship).size());


        final Contentlet copiedContentlet = contentletAPI.copyContentlet(childContent, user, false);
        //copy content relationship
        final Map<Relationship, List<Contentlet>> contentRelationships = contentletAPI.findContentRelationships(copiedContentlet, user);

        assertEquals(0, contentRelationships.get(relationship).size());
    }

    /**
     * Method to test: {@link ContentletAPI#copyContentlet(Contentlet, User, boolean)}
     * Given Scenario: When the user copy a content with a cardinality N to 1 the content copied should not have any relationship
     * ExpectedResult: The content copied should not have any relationship.
     *
     */
    @Test
    public void test_resultOfCopyContWithCardinality_ManyToOne_shouldNotHaveRelationWithCotents() throws DotDataException, DotSecurityException {

        //Create a child with a text field
        final ContentType childCT = new ContentTypeDataGen().name("child").nextPersisted();
        final com.dotcms.contenttype.model.field.Field uniqueTextField = new FieldDataGen()
                .contentTypeId(childCT.id())
                .unique(false)
                .type(TextField.class)
                .nextPersisted();

        //Create a parent with a relationship to the child
        final ContentType parentCT = new ContentTypeDataGen().nextPersisted();
        //create the relation field
        final com.dotcms.contenttype.model.field.Field parentField = createRelationshipField("children", parentCT,
                childCT.variable(), WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_ONE.ordinal());
        //get the relationship
        final Relationship relationship = relationshipAPI
                .getRelationshipFromField(parentField, user);


        //create the contentlets
        final Contentlet childContent = new ContentletDataGen(childCT).setProperty("title", "childCont").nextPersisted();

        Contentlet parentContent = new ContentletDataGen(parentCT.id())
                .languageId(languageAPI.getDefaultLanguage().getId()).next();


        parentContent = contentletAPI.checkin(parentContent, Map.of(relationship, CollectionsUtils.list(childContent)), user, false);

        Map<Relationship, List<Contentlet>> relationshipRecords = contentletAPI
                .findContentRelationships(parentContent, user);

        //old content relationship
        final Map<Relationship, List<Contentlet>> oldContRelationShip = contentletAPI.findContentRelationships(childContent, user);
        assertEquals(1, oldContRelationShip.get(relationship).size());


        final Contentlet copiedContentlet = contentletAPI.copyContentlet(childContent, user, false);
        //copy content relationship
        final Map<Relationship, List<Contentlet>> contentRelationships = contentletAPI.findContentRelationships(copiedContentlet, user);

        assertEquals(0, contentRelationships.get(relationship).size());
    }

    @Test
    public void testMoveContentDependenciesFromChildSelfRelated()
            throws DotDataException, DotSecurityException {
        ContentType parentContentType = null;
        try {
            final long time = System.currentTimeMillis();
            parentContentType = createContentType("parentContentType" + time,
                    BaseContentType.CONTENT);

            //One side of the relationship is set parentContentType --> parentContentType (self-related)
            com.dotcms.contenttype.model.field.Field parentField = createRelationshipField("child",
                    parentContentType.id(), parentContentType.variable());

            final Relationship relationship = relationshipAPI
                    .getRelationshipFromField(parentField, user);

            final ContentletDataGen dataGen = new ContentletDataGen(parentContentType.id());
            Contentlet childContent = dataGen.languageId(languageAPI.getDefaultLanguage().getId())
                    .nextPersisted();

            Contentlet parentContent = dataGen.languageId(languageAPI.getDefaultLanguage().getId())
                    .next();

            parentContent = contentletAPI.checkin(parentContent, Map.of(relationship, CollectionsUtils.list(childContent)), user, false);

            List<Contentlet> relatedContent = relationshipAPI.dbRelatedContent(relationship,
                    parentContent, true);

            assertEquals(1, relatedContent.size());
            assertEquals(childContent.getIdentifier(), relatedContent.get(0).getIdentifier());

            //creates a new version of the child
            childContent = contentletAPI.checkout(childContent.getInode(), user, false);
            childContent = contentletAPI
                    .checkin(childContent, (ContentletRelationships) null, null, null,
                            user, false);

            relatedContent = relationshipAPI.dbRelatedContent(relationship, parentContent, true);

            assertEquals(1, relatedContent.size());
            assertEquals(childContent.getIdentifier(), relatedContent.get(0).getIdentifier());

        } finally {
            if (parentContentType != null) {
                contentTypeAPI.delete(parentContentType);
            }
        }

    }

    @Test
    public void testMoveContentDependenciesFromParent()
            throws DotDataException, DotSecurityException {
        ContentType parentContentType = null;
        ContentType childContentType = null;
        try {
            final long time = System.currentTimeMillis();
            parentContentType = createContentType("parentContentType" + time,
                    BaseContentType.CONTENT);
            childContentType = createContentType("childContentType" + time,
                    BaseContentType.CONTENT);

            //One side of the relationship is set parentContentType --> childContentType
            com.dotcms.contenttype.model.field.Field parentField = createRelationshipField("child",
                    parentContentType.id(), childContentType.variable());

            //Setting the other side of the relationship childContentType --> parentContentType
            com.dotcms.contenttype.model.field.Field childField = createRelationshipField("parent",
                    childContentType.id(),
                    parentContentType.variable() + StringPool.PERIOD + parentField.variable());

            //Removing parent field
            fieldAPI.delete(parentField, user);

            final Relationship relationship = relationshipAPI
                    .getRelationshipFromField(childField, user);

            Contentlet childContent = new ContentletDataGen(childContentType.id())
                    .languageId(languageAPI.getDefaultLanguage().getId()).next();

            Contentlet parentContent = new ContentletDataGen(parentContentType.id())
                    .languageId(languageAPI.getDefaultLanguage().getId()).nextPersisted();

            childContent = contentletAPI.checkin(childContent, Map.of(relationship, CollectionsUtils.list(parentContent)), user, false);

            Map<Relationship, List<Contentlet>> relationshipRecords = contentletAPI
                    .findContentRelationships(childContent, user);

            assertTrue(relationshipRecords.containsKey(relationship));
            assertEquals(1, relationshipRecords.get(relationship).size());
            assertEquals(parentContent.getIdentifier(),
                    relationshipRecords.get(relationship).get(0).getIdentifier());

            //creates a new version of the child
            parentContent = contentletAPI.checkout(parentContent.getInode(), user, false);
            parentContent = contentletAPI
                    .checkin(parentContent, (ContentletRelationships) null, null, null,
                            user, false);

            relationshipRecords = contentletAPI
                    .findContentRelationships(childContent, user);

            assertTrue(relationshipRecords.containsKey(relationship));
            assertEquals(1, relationshipRecords.get(relationship).size());
            assertEquals(parentContent.getIdentifier(),
                    relationshipRecords.get(relationship).get(0).getIdentifier());

        } finally {
            if (parentContentType != null) {
                contentTypeAPI.delete(parentContentType);
            }

            if (childContentType != null) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test
    public void testMoveContentDependenciesFromParentSelfRelated()
            throws DotDataException, DotSecurityException {
        ContentType parentContentType = null;
        try {
            final long time = System.currentTimeMillis();
            parentContentType = createContentType("parentContentType" + time,
                    BaseContentType.CONTENT);

            //One side of the relationship is set parentContentType --> parentContentType (self-related as parent)
            com.dotcms.contenttype.model.field.Field parentField = createRelationshipField("child",
                    parentContentType.id(), parentContentType.variable());

            //Setting the other side of the relationship parentContentType --> parentContentType (self-related as child)
            com.dotcms.contenttype.model.field.Field childField = createRelationshipField("parent",
                    parentContentType.id(),
                    parentContentType.variable() + StringPool.PERIOD + parentField.variable());

            //Removing parent field
            fieldAPI.delete(parentField, user);

            final Relationship relationship = relationshipAPI
                    .getRelationshipFromField(childField, user);

            final ContentletDataGen dataGen = new ContentletDataGen(parentContentType.id());
            Contentlet parentContent = dataGen.languageId(languageAPI.getDefaultLanguage().getId())
                    .nextPersisted();

            Contentlet childContent = dataGen.languageId(languageAPI.getDefaultLanguage().getId())
                    .next();
            childContent.setRelated(childField.variable(), CollectionsUtils.list(parentContent));

            childContent = contentletAPI.checkin(childContent, user, false);

            List<Contentlet> relatedContent = relationshipAPI.dbRelatedContent(relationship,
                    childContent, false);

            assertEquals(1, relatedContent.size());
            assertEquals(parentContent.getIdentifier(), relatedContent.get(0).getIdentifier());

            //creates a new version of the parent
            parentContent = contentletAPI.checkout(parentContent.getInode(), user, false);
            parentContent = contentletAPI
                    .checkin(parentContent, (ContentletRelationships) null, null, null,
                            user, false);

            relatedContent = relationshipAPI.dbRelatedContent(relationship, childContent, false);

            assertEquals(1, relatedContent.size());
            assertEquals(parentContent.getIdentifier(), relatedContent.get(0).getIdentifier());

        } finally {
            if (parentContentType != null) {
                contentTypeAPI.delete(parentContentType);
            }
        }
    }

    @Test
    public void test_update_mod_date_contentlet_expect_success() throws Exception {

        //Creating dummy content for testing
        final Contentlet beforeTouch = TestDataUtils.getGenericContentContent(true,
                languageAPI.getDefaultLanguage().getId());
        assertNotNull(beforeTouch);

        final Set<String> inodes = Stream.of(beforeTouch).map(Contentlet::getInode)
                .collect(Collectors.toSet());
        contentletAPI.updateModDate(inodes, user);

        final Contentlet afterTouch = contentletAPI.find(beforeTouch.getInode(),
                APILocator.getUserAPI().getSystemUser(), false);
        assertEquals(beforeTouch.getInode(), afterTouch.getInode());
        assertNotEquals(afterTouch.getModDate(), beforeTouch.getModDate());
        assertEquals(user.getUserId(), afterTouch.getModUser());
    }

    @DataProvider
    @SuppressWarnings("unchecked")
    public static Object[] testCasesNullRequiredFieldValues() {

        // case 1 setStringProperty
        final TestCaseNullFieldvalues case1 = new TestCaseNullFieldvalues();
        case1.fieldType = TextField.class;
        case1.dataType = DataTypes.TEXT;
        case1.assertion =
                // contentTypeAndField is a Tuple of contentType Id and Field variable
                (contentTypeIdAndFieldVar) -> {
                    try {
                        final Contentlet testContentlet
                                = tryToCheckinContentWithNullValueForRequiredField(
                                contentTypeIdAndFieldVar);
                        // lets give a value to the required field using setStringProperty

                        testContentlet.setStringProperty(contentTypeIdAndFieldVar._2,
                                "this is a valid value");

                        // this time should succeed
                        contentletAPI.checkin(testContentlet, user, false);
                    } catch (DotDataException | DotSecurityException e) {
                        throw new DotRuntimeException(e);
                    }
                };

        // case 2 setLongProperty
        final TestCaseNullFieldvalues case2 = new TestCaseNullFieldvalues();
        case2.fieldType = TextField.class;
        case2.dataType = DataTypes.INTEGER;
        case2.assertion =
                // contentTypeAndField is a Tuple of contentType and Field
                (contentTypeIdAndFieldVar) -> {
                    try {
                        final Contentlet testContentlet
                                = tryToCheckinContentWithNullValueForRequiredField(
                                contentTypeIdAndFieldVar);
                        // lets give a value to the required field using setStringProperty

                        testContentlet.setLongProperty(contentTypeIdAndFieldVar._2,
                                10000L);

                        // this time should succeed
                        contentletAPI.checkin(testContentlet, user, false);
                    } catch (DotDataException | DotSecurityException e) {
                        throw new DotRuntimeException(e);
                    }
                };

        // case 3 setBoolProperty
        final TestCaseNullFieldvalues case3 = new TestCaseNullFieldvalues();
        case3.fieldType = RadioField.class;
        case3.dataType = DataTypes.BOOL;

        case3.assertion =
                // contentTypeAndField is a Tuple of contentType and Field
                (contentTypeIdAndFieldVar) -> {
                    try {
                        final Contentlet testContentlet
                                = tryToCheckinContentWithNullValueForRequiredField(
                                contentTypeIdAndFieldVar);
                        // lets give a value to the required field using setStringProperty

                        testContentlet.setBoolProperty(contentTypeIdAndFieldVar._2,
                                true);

                        // this time should succeed
                        contentletAPI.checkin(testContentlet, user, false);
                    } catch (DotDataException | DotSecurityException e) {
                        throw new DotRuntimeException(e);
                    }
                };

        // case 3 setFloatProperty
        final TestCaseNullFieldvalues case4 = new TestCaseNullFieldvalues();
        case4.fieldType = TextField.class;
        case4.dataType = DataTypes.FLOAT;
        case4.assertion =
                // contentTypeAndField is a Tuple of contentType and Field
                (contentTypeIdAndFieldVar) -> {
                    try {
                        final Contentlet testContentlet
                                = tryToCheckinContentWithNullValueForRequiredField(
                                contentTypeIdAndFieldVar);
                        // lets give a value to the required field using setStringProperty

                        testContentlet.setFloatProperty(contentTypeIdAndFieldVar._2,
                                1500);

                        // this time should succeed
                        contentletAPI.checkin(testContentlet, user, false);
                    } catch (DotDataException | DotSecurityException e) {
                        throw new DotRuntimeException(e);
                    }
                };

        return new TestCaseNullFieldvalues[]{
                case1,
                case2,
                case3,
                case4
        };

    }

    private static class TestCaseNullFieldvalues {

        Consumer<Tuple2<String, String>> assertion;
        Class<? extends com.dotcms.contenttype.model.field.Field> fieldType;
        DataTypes dataType;
    }


    private static Contentlet tryToCheckinContentWithNullValueForRequiredField(
            final Tuple2<String, String> typeIdFieldVar)
            throws DotSecurityException, DotDataException {

        final String testTypeId = typeIdFieldVar._1;

        final String testFieldVar = typeIdFieldVar._2;

        final ContentletDataGen contentletDataGen = new ContentletDataGen(
                testTypeId);
        final Contentlet testContentlet = contentletDataGen.next();
        testContentlet.setProperty(testFieldVar, null);

        try {
            contentletAPI.checkin(testContentlet, user, false);
        } catch (DotContentletValidationException e) {
            // expected because of required field
            Logger.info(ContentletAPITest.class, "All good");
        }

        return testContentlet;
    }

    @Test
    @UseDataProvider("testCasesNullRequiredFieldValues")
    public void testCheckin_nullRequiredFieldValue(final TestCaseNullFieldvalues testCase)
            throws DotDataException, DotSecurityException {

        long time = System.currentTimeMillis();

        ContentType contentType = ContentTypeBuilder
                .builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.getType()))
                .description("ContentTypeWithPublishExpireFields " + time)
                .folder(FolderAPI.SYSTEM_FOLDER)
                .host(Host.SYSTEM_HOST)
                .name("ContentTypeWithPublishExpireFields " + time)
                .owner(APILocator.systemUser().toString())
                .variable("CTVariable711").publishDateVar("publishDate")
                .expireDateVar("expireDate")
                .build();

        final ContentTypeAPI contentTypeApi = APILocator.getContentTypeAPI(APILocator.systemUser());

        try {
            contentType = contentTypeApi.save(contentType);

            List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>(
                    contentType.fields());

            final String titleFieldVarname = "testTitle" + time;

            final com.dotcms.contenttype.model.field.Field titleField = FieldBuilder
                    .builder(TextField.class)
                    .name(titleFieldVarname)
                    .variable(titleFieldVarname)
                    .contentTypeId(contentType.id())
                    .build();

            fields.add(titleField);

            final String secondFieldVarName = "testSecondField"
                    + System.currentTimeMillis();

            final com.dotcms.contenttype.model.field.Field secondField = FieldBuilder
                    .builder(testCase.fieldType)
                    .dataType(testCase.dataType)
                    .name(secondFieldVarName)
                    .variable(secondFieldVarName)
                    .contentTypeId(contentType.id())
                    .required(true)
                    .build();

            fields.add(secondField);

            contentType = contentTypeApi.save(contentType, fields);

            testCase.assertion.accept(new Tuple2<>(contentType.id(), secondField.variable()));

        } finally {
            // Deleting content type.
            contentTypeApi.delete(contentType);
        }
    }

    private File getBinaryAsset(String inode, String varName, String binaryName) {

        FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();

        File binaryFromAssetsFolder = new File(fileAssetAPI.getRealAssetsRootPath()
                + separator
                + inode.charAt(0)
                + separator
                + inode.charAt(1)
                + separator
                + inode
                + separator
                + varName
                + separator
                + binaryName);

        return binaryFromAssetsFolder;
    }

    private Contentlet createContentWithFieldValues(String contentTypeId,
            Map<String, Object> fieldValues)
            throws DotSecurityException, DotDataException {
        Contentlet contentlet = new Contentlet();
        contentlet.setContentTypeId(contentTypeId);
        contentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());

        for (String fieldVariable : fieldValues.keySet()) {
            contentlet.setProperty(fieldVariable, fieldValues.get(fieldVariable));
        }

        return contentletAPI.checkin(contentlet, user, false);
    }

    private File createTempFileWithText(String name, String text) throws IOException {
        File tempFile = getTemporaryFolder().newFile(name);
        writeTextIntoFile(tempFile, text);
        return tempFile;
    }

    private ContentType createContentType(String name, BaseContentType baseType)
            throws DotSecurityException, DotDataException {
        ContentType contentType = ContentTypeBuilder.builder(baseType.immutableClass())
                .description(name)
                .host(defaultHost.getIdentifier())
                .name(name)
                .owner("owner")
                .build();

        return contentTypeAPI.save(contentType);
    }

    private com.dotcms.contenttype.model.field.Field createTextField(String name,
            String contentTypeId)
            throws DotSecurityException, DotDataException {
        com.dotcms.contenttype.model.field.Field field = ImmutableTextField.builder()
                .name(name)
                .contentTypeId(contentTypeId)
                .dataType(DataTypes.TEXT)
                .build();

        return fieldAPI.save(field, user);
    }

    private com.dotcms.contenttype.model.field.Field createBinaryField(String name,
            String contentTypeId)
            throws DotSecurityException, DotDataException {
        com.dotcms.contenttype.model.field.Field field = ImmutableBinaryField.builder()
                .name(name)
                .contentTypeId(contentTypeId)
                .build();

        return fieldAPI.save(field, user);
    }

    /**
     * Util method to write dummy text into a file.
     *
     * @param file        that we need to write. File should be empty.
     * @param textToWrite text that we are going to write into the file.
     */
    private void writeTextIntoFile(File file, final String textToWrite) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(textToWrite);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_findInDb_returns_properly() throws Exception {

        ContentType type = new ContentTypeDataGen()
                .fields(ImmutableList
                        .of(ImmutableTextField.builder().name("Title").variable("title")
                                .searchable(true).listed(true).build()))
                .nextPersisted();

        // test null value
        Optional<Contentlet> conOpt = contentletAPI.findInDb(null);
        assert (conOpt.isPresent() == false);

        // test non-existing
        conOpt = contentletAPI.findInDb("not-here");
        assert (conOpt.isPresent() == false);

        final Contentlet contentlet = new ContentletDataGen(type.id()).setProperty("title",
                "contentTest " + System.currentTimeMillis()).nextPersisted();
        contentlet.setStringProperty("title", "nope");

        conOpt = contentletAPI.findInDb(contentlet.getInode());
        assert (conOpt.isPresent());

        assertNotEquals(conOpt.get().getTitle(), contentlet.getTitle());


    }

    @Test
    public void testCheckInContentletWithoutHost_shouldUseContentTypeHost()
            throws DotDataException, DotSecurityException {
        ContentType contentType = null;

        try {
            contentType = new ContentTypeDataGen()
                    .fields(ImmutableList
                            .of(ImmutableTextField.builder().name("Title").variable("title")
                                    .build()))
                    .nextPersisted();
            Contentlet contentlet = new Contentlet();
            contentlet.setContentType(contentType);
            contentlet.setProperty("title", "contentTest " + System.currentTimeMillis());
            contentlet = contentletAPI.checkin(contentlet, user, false);

            assertEquals(contentType.host(), contentlet.getHost());
        } finally {
            if (contentType != null) {
                contentTypeAPI.delete(contentType);
            }
        }
    }

    /**
     * This test creates one content with versions in English and Spanish when one of these versions
     * is deleted should be removed from the index.
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test
    public void testRemoveContentFromIndexMultilingualContent()
            throws DotDataException, DotSecurityException, InterruptedException {
        ContentType contentType = null;

        try {
            contentType = new ContentTypeDataGen()
                    .name("testRemoveContentFromIndexMultilingualContent"
                            + System.currentTimeMillis())
                    .fields(ImmutableList
                            .of(ImmutableTextField.builder().name("Title").variable("title")
                                    .build()))
                    .nextPersisted();

            final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());
            final Contentlet contentInEnglish = contentletDataGen.languageId(
                    languageAPI.getDefaultLanguage().getId()).nextPersisted();
            Contentlet contentInSpanish = ContentletDataGen.checkout(contentInEnglish);
            contentInSpanish.setLanguageId(spanishLanguage.getId());
            contentInSpanish = ContentletDataGen.checkin(contentInSpanish);

            Logger.info(this, "searching after create");

            assertEquals(2,
                    contentletAPI.searchIndex(
                            "+identifier:" + contentInEnglish.getIdentifier() + " "
                                    + UUIDGenerator.uuid(), -1, 0, "", user, true).size());

            contentInSpanish.setIndexPolicy(IndexPolicy.FORCE);
            ContentletDataGen.archive(contentInSpanish);
            ContentletDataGen.delete(contentInSpanish);

            Logger.info(this, "searching after delete spanish");

            assertEquals(1,
                    contentletAPI.searchIndex(
                            "+identifier:" + contentInEnglish.getIdentifier() + " "
                                    + UUIDGenerator.uuid(), -1, 0, "", user, true).size());

            contentInEnglish.setIndexPolicy(IndexPolicy.FORCE);
            ContentletDataGen.archive(contentInEnglish);
            ContentletDataGen.delete(contentInEnglish);

            Logger.info(this, "searching after delete english");

            int size = Awaitility.await().atMost(10, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .until(() -> contentletAPI.searchIndex(
                                    "+identifier:" + contentInEnglish.getIdentifier() + " "
                                            + UUIDGenerator.uuid(), -1, 0, "", user, true).size(),
                            equalTo(0));

            assertEquals(0, size);

        } finally {
            if (contentType != null) {
                contentTypeAPI.delete(contentType);
            }
        }
    }

    @Test
    public void testGetRelatedForSelfJoinsDoesNotCacheWhenPullByParentIsNull()
            throws DotDataException, DotSecurityException, DotCacheException {
        ContentType parentContentType = null;
        try {
            final long time = System.currentTimeMillis();
            parentContentType = createContentType("parentContentType" + time,
                    BaseContentType.CONTENT);

            //One side of the relationship is set parentContentType --> parentContentType (self-related as parent)
            com.dotcms.contenttype.model.field.Field parentField = createRelationshipField("child",
                    parentContentType.id(), parentContentType.variable());

            //Setting the other side of the relationship parentContentType --> parentContentType (self-related as child)
            com.dotcms.contenttype.model.field.Field childField = createRelationshipField("parent",
                    parentContentType.id(),
                    parentContentType.variable() + StringPool.PERIOD + parentField.variable());

            final Relationship relationship = relationshipAPI
                    .getRelationshipFromField(childField, user);

            final ContentletDataGen dataGen = new ContentletDataGen(parentContentType.id());
            Contentlet parentContent = dataGen.languageId(languageAPI.getDefaultLanguage().getId())
                    .nextPersisted();

            Contentlet childContent = dataGen.languageId(languageAPI.getDefaultLanguage().getId())
                    .next();
            childContent.setRelated(childField.variable(), CollectionsUtils.list(parentContent));

            childContent = contentletAPI.checkin(childContent, user, false);

            //Flush cache
            CacheLocator.getRelationshipCache().removeRelationshipsByType(parentContentType);

            contentletAPI.getRelatedContent(parentContent, relationship, null, user, false);
            contentletAPI.getRelatedContent(childContent, relationship, null, user, false);

            assertFalse(UtilMethods.isSet(CacheLocator.getRelationshipCache()
                    .getRelatedContentMap(parentContent.getIdentifier())));

            assertFalse(UtilMethods.isSet(CacheLocator.getRelationshipCache()
                    .getRelatedContentMap(childContent.getIdentifier())));

        } finally {
            if (parentContentType != null) {
                contentTypeAPI.delete(parentContentType);
            }
        }
    }

    @Test
    public void testCopyContentletDoesNotCopyWorkflowHistory()
            throws DotDataException, DotSecurityException {
        final User systemUser = APILocator.systemUser();
        final Language language = APILocator.getLanguageAPI().getDefaultLanguage();
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();

        Contentlet contentlet = null;
        Contentlet newContentlet = null;

        try {
            contentlet = TestDataUtils.getPageContent(true, language.getId());

            //save workflow task
            final WorkflowStep workflowStep = workflowAPI.findStep(
                    SystemWorkflowConstants.WORKFLOW_NEW_STEP_ID);
            workflowAPI.deleteWorkflowTaskByContentletIdAnyLanguage(contentlet, systemUser);
            final WorkflowTask workflowTask = workflowAPI
                    .createWorkflowTask(contentlet, systemUser, workflowStep, "test", "test");
            workflowAPI.saveWorkflowTask(workflowTask);

            //save workflow comment
            final WorkflowComment comment = new WorkflowComment();
            comment.setComment(workflowTask.getDescription());
            comment.setCreationDate(new Date());
            comment.setPostedBy(systemUser.getUserId());
            comment.setWorkflowtaskId(workflowTask.getId());
            workflowAPI.saveComment(comment);

            //save workflow history
            final WorkflowHistory hist = new WorkflowHistory();
            hist.setChangeDescription("workflow history description");
            hist.setCreationDate(new Date());
            hist.setMadeBy(systemUser.getUserId());
            hist.setWorkflowtaskId(workflowTask.getId());
            workflowAPI.saveWorkflowHistory(hist);

            //save workflow task file
            final Contentlet fileAsset = TestDataUtils.getFileAssetContent(true, language.getId());
            workflowAPI.attachFileToTask(workflowTask, fileAsset.getInode());

            newContentlet = contentletAPI
                    .copyContentlet(contentlet, systemUser, false);

            assertEquals(contentlet.getTitle(), newContentlet.getTitle());

            //verify workflow task and comments were copied (ignoring workflow history and files)
            final WorkflowTask newWorkflowTask = workflowAPI.findTaskByContentlet(newContentlet);

            assertNotNull(newWorkflowTask.getId());

            final List<WorkflowComment> comments = workflowAPI
                    .findWorkFlowComments(newWorkflowTask);
            assertTrue(UtilMethods.isSet(comments) && comments.size() == 1);
            assertEquals("Content copied from content id: " + contentlet.getIdentifier(),
                    comments.get(0).getComment());

            assertEquals(systemUser.getUserId(), comments.get(0).getPostedBy());

            assertFalse(UtilMethods.isSet(workflowAPI.findWorkflowHistory(newWorkflowTask)));

            assertFalse(UtilMethods
                    .isSet(workflowAPI
                            .findWorkflowTaskFilesAsContent(newWorkflowTask, systemUser)));

        } finally {
            if (contentlet != null && contentlet.getInode() != null) {
                ContentletDataGen.destroy(contentlet);
            }

            if (newContentlet != null && newContentlet.getInode() != null) {
                ContentletDataGen.destroy(newContentlet);
            }
        }

    }

    @Test
    public void test_copyContentlet_contentletArchivedShouldCopyAsArchived()
            throws DotDataException, DotSecurityException {
        //Create Contentlet
        final Contentlet originalContentlet = TestDataUtils.getGenericContentContent(true, 1);
        contentletAPI.publish(originalContentlet, user, false);

        //Copy contentlet
        final Contentlet copyContentletPublished = contentletAPI.copyContentlet(originalContentlet,
                user, false);
        //Check that the copy Contentlet is published
        assertTrue(copyContentletPublished.isLive());

        //Archive original Contentlet
        contentletAPI.unpublish(originalContentlet, user, false);
        contentletAPI.archive(originalContentlet, user, false);
        assertTrue(originalContentlet.isArchived());

        //Copy Contentlet
        final Contentlet copyContentletArchived = contentletAPI.copyContentlet(originalContentlet,
                user, false);
        //Check that the copy Contentlet is published
        assertTrue(copyContentletArchived.isArchived());
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)} Given scenario: We
     * create a file asset add some custom attributes to it then we create a new version Expected
     * result: After creating a newer version the custom meta is still there.
     *
     * @throws Exception
     */
    @Test
    public void Test_Copy_Metadata_On_CheckIn()
            throws DotDataException, DotSecurityException {

        final String fileAsset = FileAssetAPI.BINARY_FIELD;
        final FileMetadataAPI fileMetadataAPI = APILocator.getFileMetadataAPI();
        //Create Contentlet
        final Contentlet originalContentlet = TestDataUtils.getFileAssetContent(true, 1L,
                TestFile.GIF);

        fileMetadataAPI.putCustomMetadataAttributes(originalContentlet,
                ImmutableMap.of(fileAsset,
                        ImmutableMap.of("focalPoint", "2.66,2.44", "foo", "bar", "bar", "foo")
                )
        );

        final Contentlet checkout1 = contentletAPI
                .checkout(originalContentlet.getInode(), user, false);

        //Force a change
        checkout1.setStringProperty(FileAssetAPI.TITLE_FIELD, "v2");

        final Contentlet v2 = contentletAPI.checkin(checkout1, user, false);
        assertNotEquals(originalContentlet.getInode(), v2.getInode());

        final Metadata metadata1 = v2.getBinaryMetadata(fileAsset);
        assertNotNull(metadata1);
        final Map<String, Serializable> customMeta = metadata1.getCustomMeta();
        assertEquals(customMeta.get("focalPoint"), "2.66,2.44");
        assertEquals(customMeta.get("foo"), "bar");
        assertEquals(customMeta.get("bar"), "foo");


    }

    @Test
    public void testContentCheckin_mapIncludeExpectedProperties() throws IOException {
        final ContentType fileType =
                new ContentTypeDataGen().baseContentType(FILEASSET).nextPersisted();

        File tempFile = File.createTempFile("testMissingProps", ".jpg");

        final Contentlet fileAsset = new ContentletDataGen(fileType)
                .setProperty(FileAssetAPI.BINARY_FIELD, tempFile)
                .setProperty(FileAssetAPI.TITLE_FIELD, "testMissingProps").nextPersisted();

        Optional<Contentlet> fileAsContentOptional = APILocator.getContentletAPI()
                .findContentletByIdentifierOrFallback(fileAsset.getIdentifier(),
                        Try.of(fileAsset::isLive).getOrElse(false), fileAsset.getLanguageId(),
                        user, true);

        assertTrue(fileAsContentOptional.isPresent());
        assertNotNull(fileAsContentOptional.get().getMap().get("fileName"));
        assertTrue(((String) fileAsContentOptional.get().getMap().get("fileName")).startsWith(
                "testMissingProps"));

    }


    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)} Given scenario: We
     * create a file asset under the default site then we update the site/folder then we call
     * checkin again Expected result: After the checkin the contentlet should have been moved
     * properly.
     *
     * @throws Exception
     */
    @Test
    public void Test_Move_Content_Different_Host_And_Folder_On_CheckIn()
            throws DotDataException, DotSecurityException {

        final Host nextSite = new SiteDataGen().nextPersisted();
        final Folder nextFolder = new FolderDataGen().site(nextSite).name("any").nextPersisted();

        //Create Contentlet
        final Contentlet originalContentlet = TestDataUtils.getFileAssetContent(true, 1L,
                TestFile.GIF);

        final Contentlet checkout1 = contentletAPI
                .checkout(originalContentlet.getInode(), user, false);

        checkout1.setHost(nextSite.getIdentifier());
        checkout1.setFolder(nextFolder.getInode());

        Contentlet moved = contentletAPI.checkin(checkout1, user, false);
        final Identifier identifier = APILocator.getIdentifierAPI().find(moved.getIdentifier());
        assertEquals(nextSite.getIdentifier(), identifier.getHostId());
        moved = APILocator.getContentletAPI()
                .find(moved.getInode(), APILocator.systemUser(), false);
        assertEquals(moved.getHost(), identifier.getHostId());
    }

    /**
     * Method to test: {@link ContentletAPI#checkin(Contentlet, User, boolean)} Given scenario: We
     * create CT that holds a keyValue then we create two instances. First one takes a well-formed
     * json second one is messed up. Expected result: This test validates that messed-up json dont
     * break stuff. when keyValues are returned.
     *
     * @throws Exception
     */
    @Test
    public void Test_Key_Value_As_Map_Test() throws DotDataException, DotSecurityException {

        final Host nextSite = new SiteDataGen().nextPersisted();
        final Folder nextFolder = new FolderDataGen().site(nextSite).name("any").nextPersisted();

        final boolean defaultValue = Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);
        Config.setProperty(SAVE_CONTENTLET_AS_JSON, false);

        try {
            String contentTypeName = "KeyValueTest" + System.currentTimeMillis();

            final List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();

            fields.add(
                    new FieldDataGen()
                            .name("hostFolder")
                            .velocityVarName("hostFolder")
                            .type(HostFolderField.class)
                            .next()
            );

            fields.add(
                    new FieldDataGen()
                            .name("title")
                            .velocityVarName("TitleField")
                            .type(TextField.class)
                            .next()
            );

            fields.add(
                    new FieldDataGen()
                            .name("keyValueField")
                            .velocityVarName("keyValueField")
                            .type(KeyValueField.class)
                            .next()
            );

            ContentType contentType = new ContentTypeDataGen()
                    .baseContentType(BaseContentType.CONTENT)
                    .name(contentTypeName)
                    .velocityVarName(contentTypeName)
                    .fields(fields)
                    .nextPersisted();

            //This is a piece of content with valid json
            Contentlet withValidKeyValue = new ContentletDataGen(contentType).host(nextSite)
                    .languageId(1)
                    .setProperty("title", "lol")
                    .setProperty("hostFolder", nextFolder.getIdentifier())
                    .setProperty("keyValueField", "{\"key1\":\"val1\"}")
                    .nextPersisted();

            Contentlet retrieved = APILocator.getContentletAPI()
                    .find(withValidKeyValue.getInode(), APILocator.systemUser(), false);

            assertTrue(retrieved.get("keyValueField") instanceof Map);
            assertFalse(((Map) retrieved.get("keyValueField")).isEmpty());

            //This is a piece of content with invalid json set on the keyValue
            Contentlet withInvalidKeyValue = new ContentletDataGen(contentType).host(nextSite)
                    .languageId(1)
                    .setProperty("title", "lol")
                    .setProperty("hostFolder", nextFolder.getIdentifier())
                    .setProperty("keyValueField", "messed-up-json")
                    .nextPersisted();

            retrieved = APILocator.getContentletAPI()
                    .find(withInvalidKeyValue.getInode(), APILocator.systemUser(), false);

            assertTrue(retrieved.get("keyValueField") instanceof Map);
            assertTrue(((Map) retrieved.get("keyValueField")).isEmpty());

        } finally {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, defaultValue);
        }
    }

    /**
     * Method to test
     * {@link ContentletAPI#loadField(String, com.dotcms.contenttype.model.field.Field)} Basically
     * we're testing this method works on both types of saved contentlets. using json scheme or the
     * columns
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Load_Field_Works_On_Contentlet_Saved_as_Json_Or_Saved_As_Columns()
            throws DotDataException, DotSecurityException {
        //Test when contet is saved as json
        testLoadFieldWontReturnNull(true);
        //Test when contet is saved as columns
        testLoadFieldWontReturnNull(false);
    }

    private void testLoadFieldWontReturnNull(final boolean saveAsJson)
            throws DotDataException, DotSecurityException {

        final DotConnect dotConnect = new DotConnect();

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final boolean defaultValue = Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);
        Config.setProperty(SAVE_CONTENTLET_AS_JSON, saveAsJson);
        try {
            String contentTypeName = "SavedInColumnsCT" + System.currentTimeMillis();

            final List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();

            fields.add(
                    new FieldDataGen()
                            .name("title")
                            .velocityVarName("titleField")
                            .type(TextField.class)
                            .next()
            );

            ContentType contentType = new ContentTypeDataGen()
                    .baseContentType(BaseContentType.CONTENT)
                    .name(contentTypeName)
                    .velocityVarName(contentTypeName)
                    .fields(fields)
                    .nextPersisted();

            Contentlet contentlet = new ContentletDataGen(contentType)
                    .languageId(1)
                    .setProperty("title", "lol")
                    .nextPersisted();

            //Verify stuff is getting saved as we suppose it is
            String contentletAsJson = dotConnect.setSQL(
                            "select c.contentlet_as_json as json from contentlet c where c.inode = ?")
                    .addParam(contentlet.getInode()).getString("json");

            if (saveAsJson) {
                assertTrue(UtilMethods.isSet(contentletAsJson));
            } else {
                assertTrue(UtilMethods.isNotSet(contentletAsJson));
            }

            final Optional<com.dotcms.contenttype.model.field.Field> titleField = contentlet
                    .getContentType().fields().stream()
                    .filter(field -> "titleField".equals(field.variable())).findFirst();

            assertTrue(titleField.isPresent());
            assertNotNull(contentletAPI.loadField(contentlet.getInode(), titleField.get()));

            final Template template = new TemplateDataGen().body("body").nextPersisted();
            final Folder folder = new FolderDataGen().nextPersisted();
            final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();

            //Verify stuff is getting saved as we suppose it is
            contentletAsJson = dotConnect.setSQL(
                            "select c.contentlet_as_json as json from contentlet c where c.inode = ?")
                    .addParam(page.getInode()).getString("json");
            if (saveAsJson) {
                assertTrue(UtilMethods.isSet(contentletAsJson));
            } else {
                assertTrue(UtilMethods.isNotSet(contentletAsJson));
            }

            final Optional<com.dotcms.contenttype.model.field.Field> templateField = page
                    .getContentType().fields().stream()
                    .filter(field -> "template".equals(field.variable())).findFirst();

            assertTrue(templateField.isPresent());
            assertNotNull(contentletAPI.loadField(page.getInode(), templateField.get()));

        } finally {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, defaultValue);
        }
    }

    /**
     * Given scenario: Contentlet with a {@link JSONField} and a VALID value for the field Expected
     * result: should persist the contentlet with the provided value
     */
    @Test
    public void saveContentWithValidJSONField_ShouldSucceed() throws Exception {
        // create content type with JSON field
        ContentType typeWithJSONField = new ContentTypeDataGen().nextPersisted();
        com.dotcms.contenttype.model.field.Field jsonField = new FieldDataGen()
                .type(JSONField.class)
                .contentTypeId(typeWithJSONField.id())
                .nextPersisted();

        final String testJSON = "{\n"
                + "                \"percentages\": {},\n"
                + "                \"type\": \"SPLIT_EVENLY\"\n"
                + "            }";

        final Contentlet contentletWithJSON = new ContentletDataGen(typeWithJSONField)
                .setProperty(jsonField.variable(), testJSON).nextPersisted();

        assertEquals(testJSON, contentletWithJSON.get(jsonField.variable()));
    }

    /**
     * Given scenario: Contentlet with a {@link JSONField} and a INVALID value for the field
     * Expected result: should throw ValidationException
     */
    @Test(expected = DotContentletValidationException.class)
    public void saveContentWithInvalidJSONField_ShouldThrowException() throws Exception {
        // create content type with JSON field
        ContentType typeWithJSONField = new ContentTypeDataGen().nextPersisted();
        com.dotcms.contenttype.model.field.Field jsonField = new FieldDataGen()
                .type(JSONField.class)
                .contentTypeId(typeWithJSONField.id())
                .nextPersisted();

        final String testJSON = "{\n"
                + "                \"INVALID JASON {},\n"
                + "                \"type\": \"SPLIT_EVENLY\"\n"
                + "            }";

        try {
            final Contentlet contentletWithJSON = new ContentletDataGen(typeWithJSONField)
                    .setProperty(jsonField.variable(), testJSON).nextPersisted();
        } catch (Exception e) {
            if (ExceptionUtil.causedBy(e, DotContentletValidationException.class)) {
                throw new DotContentletValidationException(e.getMessage());
            }
            fail("Should have thrown ValidationException");
        }
    }

    @DataProvider
    public static Object[] testCasesJSON() {
        final Tuple2<String, String> case1 = new Tuple2<>("{} {}", "{}");
        final Tuple2<String, String> case2 = new Tuple2<>("{} LOL", "{}");

        return new Object[]{case1, case2};
    }

    /**
     * Given scenario: Contentlet with a {@link JSONField} with value "{} {}" Expected result:
     * should save as "{}"
     */
    @Test
    @UseDataProvider("testCasesJSON")
    public void saveContentWithJSONField(final Tuple2<String, String> testCase) throws Exception {
        // create content type with JSON field
        ContentType typeWithJSONField = new ContentTypeDataGen().nextPersisted();
        com.dotcms.contenttype.model.field.Field jsonField = new FieldDataGen()
                .type(JSONField.class)
                .contentTypeId(typeWithJSONField.id())
                .nextPersisted();

        Contentlet contentletWithJSON = new ContentletDataGen(typeWithJSONField)
                .next();

        contentletAPI.setContentletProperty(contentletWithJSON,
                new LegacyFieldTransformer(jsonField).asOldField(), testCase._1);

        // Save the content
        contentletWithJSON = contentletAPI.checkin(contentletWithJSON, user, Boolean.TRUE);

        assertEquals(testCase._2, contentletWithJSON.getStringProperty(jsonField.variable()));
    }

    /**
     * Method to test: checkIn content Given scenario: given indexed content in different variants
     * Expected result: each document contain the proper variant in the id
     */
    @Test
    public void test_variant_present_in_document_id() throws Exception {
        final ContentType type = new ContentTypeDataGen()
                .fields(ImmutableList
                        .of(ImmutableTextField.builder().name("Title").variable("title")
                                .searchable(true).listed(true).build()))
                .nextPersisted();

        final Variant newVariant = new VariantDataGen().nextPersisted();

        // saves with DEFAULT variant
        final Contentlet contentDefaultVariant = new ContentletDataGen(type.id())
                .setProperty("title", "contentTest " + System.currentTimeMillis())
                .nextPersisted();

        // saves with newly created variant
        final Contentlet contentNewVariant =
                new ContentletDataGen(type.id())
                        .setProperty("title", "contentTest " + System.currentTimeMillis())
                        .setProperty(VARIANT_ID, newVariant.name())
                        .nextPersisted();

        final String queryContentOnDefaultVariant = "{"
                + "query: {"
                + "   query_string: {"
                + "        query: \"+identifier: " + contentDefaultVariant.getIdentifier() + "\""
                + "     }"
                + "  },"
                + "}";

        final SearchResponse responseDefaultVariant = APILocator.getContentletAPI().esSearchRaw(
                StringUtils.lowercaseStringExceptMatchingTokens(queryContentOnDefaultVariant,
                        ESContentFactoryImpl.LUCENE_RESERVED_KEYWORDS_REGEX),
                false, APILocator.systemUser(), false);

        assertEquals(contentDefaultVariant.getIdentifier() + "_"
                        + contentDefaultVariant.getLanguageId() + "_"
                        + contentDefaultVariant.getVariantId(),
                responseDefaultVariant.getHits().iterator().next().getId());

        final String queryContentOnNewVariant = "{"
                + "query: {"
                + "   query_string: {"
                + "        query: \"+identifier: " + contentNewVariant.getIdentifier() + "\""
                + "     }"
                + "  },"
                + "}";

        final SearchResponse responseNewVariant = APILocator.getContentletAPI().esSearchRaw(
                StringUtils.lowercaseStringExceptMatchingTokens(queryContentOnNewVariant,
                        ESContentFactoryImpl.LUCENE_RESERVED_KEYWORDS_REGEX),
                false, APILocator.systemUser(), false);

        assertEquals(contentNewVariant.getIdentifier() + "_"
                        + contentNewVariant.getLanguageId() + "_" + contentNewVariant.getVariantId(),
                responseNewVariant.getHits().iterator().next().getId());
    }

    @DataProvider
    public static Object[] testCasesClearDateTime() {
        return new Object[]{"", null};
    }

    /**
     * Given scenario: Contentlet with a {@link DateTimeField} where it was initially saved with
     * correct value, and then it was tried to be cleared. Expected result: the field should be
     * properly cleared
     */
    @Test
    @UseDataProvider("testCasesClearDateTime")
    public void clearDateTimeFieldValue(final String testCase) throws Exception {
        // create content type with JSON field
        ContentType typeWithDateTimeField = new ContentTypeDataGen().nextPersisted();
        com.dotcms.contenttype.model.field.Field dateTimeField = new FieldDataGen()
                .type(DateTimeField.class)
                .contentTypeId(typeWithDateTimeField.id())
                .defaultValue(null)
                .nextPersisted();

        Contentlet contentletWithDate = new ContentletDataGen(typeWithDateTimeField)
                .next();

        contentletAPI.setContentletProperty(contentletWithDate,
                new LegacyFieldTransformer(dateTimeField).asOldField(), testCase);

        // Save the content
        contentletWithDate = contentletAPI.checkin(contentletWithDate, user, Boolean.TRUE);

        assertEquals(null, contentletWithDate.getStringProperty(dateTimeField.variable()));
    }

    @DataProvider
    public static Object[][] testCasesDateTimeWithTimeZone() {
        return new Object[][]{
                {"UTC", "2023-03-15 15:45 +0000"},                    // UTC
                {"America/New_York", "2023-02-15 15:45 -0500"},       // EST
                {"America/New_York", "2023-03-15 16:45 -0400"},       // EDT
                {"America/Chicago", "2023-02-15 14:45 -0600"},        // CST
                {"America/Chicago", "2023-03-15 15:45 -0500"},        // CDT
                {"America/Los_Angeles", "2023-02-15 12:45 -0800"},    // PST
                {"America/Los_Angeles", "2023-03-15 13:45 -0700"}     // PDT
        };
    }

    /**
     * Given scenario: Contentlet with a {@link DateTimeField} when the system timezone is different from UTC (+0000)
     */
    @Test
    @UseDataProvider("testCasesDateTimeWithTimeZone")
    public void setDateTimeFieldValueWithTimeZone(String timeZoneId, String dateToSave) throws Exception {

        // save current timezone and language
        PrincipalThreadLocal.setName(APILocator.systemUser().getUserId());
        final TimeZone currentTimeZone = APILocator.systemTimeZone();
        final String currentLanguageId = APILocator.getCompanyAPI().getDefaultCompany().getLocale().getLanguage();
        try {

            // set system timezone
            APILocator.getCompanyAPI().updateDefaultUserSettings(currentLanguageId, timeZoneId,
                    null, false, false, null);

            // create content type with JSON field
            final ContentType typeWithDateTimeField = new ContentTypeDataGen().nextPersisted();
            com.dotcms.contenttype.model.field.Field dateTimeField = new FieldDataGen()
                    .type(DateTimeField.class)
                    .contentTypeId(typeWithDateTimeField.id())
                    .defaultValue(null)
                    .nextPersisted();

            Contentlet contentletWithDate = new ContentletDataGen(typeWithDateTimeField)
                    .next();

            contentletAPI.setContentletProperty(contentletWithDate,
                    new LegacyFieldTransformer(dateTimeField).asOldField(), dateToSave);

            // Save the content
            contentletWithDate = contentletAPI.checkin(contentletWithDate, user, Boolean.TRUE);

            final Date expectedDate = DateUtil.convertDate(dateToSave);

            assertEquals(expectedDate, contentletWithDate.getDateProperty(dateTimeField.variable()));

        } finally {
            // restore timezone and language
            APILocator.getCompanyAPI().updateDefaultUserSettings(currentLanguageId, currentTimeZone.getID(),
                    null, false, false, null);
            PrincipalThreadLocal.setName(null);
        }
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#publish(Contentlet, User, boolean)} When: You have
     * a Contentlet with two different version in different {@link Variant} Should: Publish each
     * version by its own
     *
     * @throws DotDataException
     */
    @Test
    public void publichWithToDifferentVarianst() throws DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet checkout = ContentletDataGen.checkout(contentlet);
        checkout.setVariantId(variant.name());

        final Contentlet contentletInVariant = ContentletDataGen.checkin(checkout);

        ArrayList<Map<String, Object>> results = new DotConnect()
                .setSQL("SELECT * FROM contentlet_version_info WHERE identifier = ?")
                .addParam(contentlet.getIdentifier())
                .loadResults();

        assertEquals(2, results.size());
        assertFalse(UtilMethods.isSet(results.get(0).get("live_inode").toString()));
        assertFalse(UtilMethods.isSet(results.get(1).get("live_inode").toString()));

        ContentletDataGen.publish(contentlet);

        results = new DotConnect()
                .setSQL("SELECT * FROM contentlet_version_info WHERE identifier = ?")
                .addParam(contentlet.getIdentifier())
                .loadResults();

        assertEquals(2, results.size());

        for (Map<String, Object> result : results) {
            if (result.get("variant_id").toString().equals("DEFAULT")) {
                assertEquals(contentlet.getInode(), result.get("live_inode"));
            } else {
                assertFalse(UtilMethods.isSet(result.get("live_inode").toString()));
            }
        }

        ContentletDataGen.publish(checkout);

        results = new DotConnect()
                .setSQL("SELECT * FROM contentlet_version_info WHERE identifier = ?")
                .addParam(contentlet.getIdentifier())
                .loadResults();

        assertEquals(2, results.size());

        for (Map<String, Object> result : results) {
            if (result.get("variant_id").toString().equals("DEFAULT")) {
                assertEquals(contentlet.getInode(), result.get("live_inode"));
            } else {
                assertEquals(checkout.getInode(), result.get("live_inode"));
            }
        }
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#publish(Contentlet, User, boolean)}
     * When: You have live and not live contentlets
     * Should: Update publish_date when contentlet is published
     */
    @Test
    public void getMostRecentPublishedContent() throws Exception {
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final String contentTypeVarName = contentType.variable();

        // publish contentlet
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType);
        final Contentlet publishedContentlet = contentletDataGen.nextPersisted();
        ContentletDataGen.publish(publishedContentlet);
        final String contentInode = publishedContentlet.getInode();
        contentletDataGen.nextPersisted();
        contentletDataGen.nextPersisted();

        // query published contentlet in the last half hour
        final Date currentDate = new Date();
        final FastDateFormat datetimeFormat = FastDateFormat.getInstance(
                "yyyy-MM-dd't'HH:mm:ssZ", APILocator.systemTimeZone());
        final String currentDateForQuery = datetimeFormat.format(currentDate);

        final Date currentDateLessHalfHour = DateUtils.addMinutes(currentDate, -30);
        final String currentDateLessHalfHourForQuery = datetimeFormat.format(currentDateLessHalfHour);

        final Contentlet mostRecentPublishedContent = contentletAPI.search(
                String.format( "+contentType:%s +sysPublishDate:[%s TO %s]",
                        contentTypeVarName, currentDateLessHalfHourForQuery, currentDateForQuery),
                        -1, 0, "", user, false)
                .stream()
                .findFirst()
                .orElse(null);

        assertNotNull(mostRecentPublishedContent);
        assertEquals(contentInode, mostRecentPublishedContent.getInode());
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#setContentletProperty(Contentlet, Field, Object)}
     * When: Create a content type with key value and title and sets to it
     * Should: should be properly set, taking a Map as an input
     */
    @Test
    public void test_setContentletProperty_key_value() throws Exception {

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final String keyValuePropName = "mykeyvalue";
        ContentTypeDataGen.addField(new FieldDataGen()
                .velocityVarName("title")
                .contentTypeId(contentType.id())
                .type(TextField.class)
                .nextPersisted());

        final com.dotcms.contenttype.model.field.Field field = new FieldDataGen()
                .velocityVarName(keyValuePropName)
                .contentTypeId(contentType.id())
                .type(KeyValueField.class)
                .nextPersisted();
        ContentTypeDataGen.addField(field);

        // publish contentlet
        final Map<String, Object> keyValueObj = Map.of("key1", "value2");
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType);
        final Contentlet contentlet = contentletDataGen.next();
        contentlet.setProperty("title", "Test");
        APILocator.getContentletAPI().setContentletProperty(contentlet, new LegacyFieldTransformer(field).asOldField(), keyValueObj);
        final Contentlet publishedContentlet = ContentletDataGen.checkin(contentlet);
        ContentletDataGen.publish(publishedContentlet);
        final String contentInode = publishedContentlet.getInode();

        final Contentlet contentRetrieved = APILocator.getContentletAPI().find(contentInode, user, false);

        assertNotNull(contentRetrieved);
        assertEquals(contentInode, contentRetrieved.getInode());

        final Map keyValueRetrieved = (Map) contentRetrieved.get(keyValuePropName);
        assertEquals(keyValueRetrieved, keyValueObj);
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#setContentletProperty(Contentlet, Field, Object)}
     * When: Create a content type with json field and title and sets to it
     * Should: should be properly set, taking a Map as an input
     */
    @Test
    public void test_setContentletProperty_json_field() throws Exception {

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final String keyValuePropName = "mykeyvalue";
        ContentTypeDataGen.addField(new FieldDataGen()
                .velocityVarName("title")
                .contentTypeId(contentType.id())
                .type(TextField.class)
                .nextPersisted());

        final com.dotcms.contenttype.model.field.Field field = new FieldDataGen()
                .velocityVarName(keyValuePropName)
                .contentTypeId(contentType.id())
                .type(JSONField.class)
                .nextPersisted();
        ContentTypeDataGen.addField(field);

        // publish contentlet
        final Map<String, Object> keyValueObj = Map.of("key1", "value2");
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType);
        final Contentlet contentlet = contentletDataGen.next();
        APILocator.getContentletAPI().setContentletProperty(contentlet, new LegacyFieldTransformer(field).asOldField(), keyValueObj);
        final Contentlet publishedContentlet = ContentletDataGen.checkin(contentlet);
        ContentletDataGen.publish(publishedContentlet);
        final String contentInode = publishedContentlet.getInode();

        final Contentlet contentRetrieved = APILocator.getContentletAPI().find(contentInode, user, false);

        assertNotNull(contentRetrieved);
        assertEquals(contentInode, contentRetrieved.getInode());

        Object keyValueRetrieved = contentRetrieved.get(keyValuePropName);
        if (keyValueRetrieved instanceof String) {
            keyValueRetrieved = com.dotmarketing.portlets.structure.model.KeyValueFieldUtil
                    .JSONValueToHashMap(keyValueRetrieved.toString());
        }
        assertEquals(keyValueRetrieved, keyValueObj);
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#setContentletProperty(Contentlet, Field, Object)}
     * When: Create a content type with long text and title and sets to it
     * Should: should be properly set, taking a String as an input
     */
    @Test
    public void test_setContentletProperty_long_text() throws Exception {

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final String keyPropName = "description";
        ContentTypeDataGen.addField(new FieldDataGen()
                .velocityVarName("title")
                .contentTypeId(contentType.id())
                .type(TextField.class)
                .nextPersisted());

        final com.dotcms.contenttype.model.field.Field field = new FieldDataGen()
                .velocityVarName(keyPropName)
                .contentTypeId(contentType.id())
                .type(TextAreaField.class)
                .nextPersisted();

        ContentTypeDataGen.addField(field);

        // publish contentlet
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType);
        final String value = "LOoooooooooooooooooooooooong teeeeeeeeeeeeext";
        Contentlet publishedContentlet = contentletDataGen.nextPersisted();
        APILocator.getContentletAPI().setContentletProperty(publishedContentlet, new LegacyFieldTransformer(field).asOldField(), value);

        final String valueRetrieved = (String) publishedContentlet.get(keyPropName);
        assertEquals(value, valueRetrieved);
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#setContentletProperty(Contentlet, Field, Object)}
     * When: Create a content type with date and title and sets to it
     * Should: should be properly set, taking a Date as an input
     */
    @Test
    public void test_setContentletProperty_date() throws Exception {

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final String keyPropName = "date";
        ContentTypeDataGen.addField(new FieldDataGen()
                .velocityVarName("title")
                .contentTypeId(contentType.id())
                .type(TextField.class)
                .nextPersisted());

        final com.dotcms.contenttype.model.field.Field field = new FieldDataGen()
                .velocityVarName(keyPropName)
                .contentTypeId(contentType.id())
                .type(DateField.class)
                .defaultValue("now")
                .nextPersisted();
        ContentTypeDataGen.addField(field);

        // publish contentlet
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType);
        final Date value = new Date();
        Contentlet publishedContentlet = contentletDataGen.nextPersisted();
        APILocator.getContentletAPI().setContentletProperty(publishedContentlet, new LegacyFieldTransformer(field).asOldField(), value);

        final Date valueRetrieved = (Date) publishedContentlet.get(keyPropName);
        assertEquals(value, valueRetrieved);
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#setContentletProperty(Contentlet, Field, Object)}
     * When: Create a content type with float and title and sets to it
     * Should: should be properly set, taking a float as an input
     */
    @Test
    public void test_setContentletProperty_float() throws Exception {

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final String keyPropName = "number";
        ContentTypeDataGen.addField(new FieldDataGen()
                .velocityVarName("title")
                .contentTypeId(contentType.id())
                .type(TextField.class)
                .nextPersisted());

        final com.dotcms.contenttype.model.field.Field field = new FieldDataGen()
                .velocityVarName(keyPropName)
                .contentTypeId(contentType.id())
                .type(TextField.class)
                .dataType(DataTypes.FLOAT)
                .defaultValue("0")
                .nextPersisted();
        ContentTypeDataGen.addField(field);

        // publish contentlet
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType);
        final Float value = 3.14f;
        Contentlet publishedContentlet = contentletDataGen.nextPersisted();
        assertNotNull(publishedContentlet);
        APILocator.getContentletAPI().setContentletProperty(publishedContentlet, new LegacyFieldTransformer(field).asOldField(), value);

        final Number valueRetrieved = (Number) publishedContentlet.get(keyPropName);
        assertEquals(value, valueRetrieved);
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#setContentletProperty(Contentlet, Field, Object)}
     * When: Create a content type with binary and title and sets to it
     * Should: should be properly set, taking a file as an input
     */
    @Test
    public void test_setContentletProperty_file() throws Exception {

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final String keyPropName = "file";
        ContentTypeDataGen.addField(new FieldDataGen()
                .velocityVarName("title")
                .contentTypeId(contentType.id())
                .type(TextField.class)
                .nextPersisted());

        ContentTypeDataGen.addField(new FieldDataGen()
                .velocityVarName(keyPropName)
                .contentTypeId(contentType.id())
                .type(BinaryField.class)
                .nextPersisted());

        // publish contentlet
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType);
        final File value = com.dotmarketing.util.FileUtil.createTemporaryFile("test", ".txt");
        contentletDataGen.setProperty(keyPropName, value);
        final Contentlet publishedContentlet = contentletDataGen.nextPersisted();
        ContentletDataGen.publish(publishedContentlet);
        final String contentInode = publishedContentlet.getInode();

        final Contentlet contentRetrieved = APILocator.getContentletAPI().find(contentInode, user, false);

        assertNotNull(contentRetrieved);
        assertEquals(contentInode, contentRetrieved.getInode());

        final File valueRetrieved = (File) contentRetrieved.get(keyPropName);
        assertEquals(value.getName(), valueRetrieved.getName());
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#setContentletProperty(Contentlet, Field, Object)}
     * When: Create a content type with text and description and sets to it
     * Should: should be properly set, taking a String as an input and returned as a boolean
     */
    @Test
    public void test_setContentletProperty_bool() throws Exception {

        String testVal = "true|true\r\nfalse|false";
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final String keyPropName = "description";
        ContentTypeDataGen.addField(new FieldDataGen()
                .velocityVarName("title")
                .contentTypeId(contentType.id())
                .type(TextField.class)
                .nextPersisted());

        final com.dotcms.contenttype.model.field.Field field = new FieldDataGen()
                .velocityVarName(keyPropName)
                .contentTypeId(contentType.id())
                .type(RadioField.class)
                .dataType(DataTypes.BOOL)
                .values(testVal)
                .nextPersisted();

        ContentTypeDataGen.addField(field);

        // publish contentlet
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType);
        final String value = "t";
        Contentlet publishedContentlet = contentletDataGen.nextPersisted();
        APILocator.getContentletAPI().setContentletProperty(publishedContentlet, new LegacyFieldTransformer(field).asOldField(), value);

        final Boolean valueRetrieved = (Boolean) publishedContentlet.get(keyPropName);
        assertTrue(valueRetrieved);
    }

    /**
     * Method to test: {@link ESContentletAPIImpl#setContentletProperty(Contentlet, Field, Object)}
     * When: Create a content type with int and title and sets to it
     * Should: should be properly set, taking a int as an input
     */
    @Test
    public void test_setContentletProperty_int() throws Exception {

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final String keyPropName = "number";
        ContentTypeDataGen.addField(new FieldDataGen()
                .velocityVarName("title")
                .contentTypeId(contentType.id())
                .type(TextField.class)
                .nextPersisted());

        final com.dotcms.contenttype.model.field.Field field = new FieldDataGen()
                .velocityVarName(keyPropName)
                .contentTypeId(contentType.id())
                .type(TextField.class)
                .dataType(DataTypes.INTEGER)
                .nextPersisted();
        ContentTypeDataGen.addField(field);

        // publish contentlet
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType);
        final Integer value = 3;
        Contentlet publishedContentlet = contentletDataGen.nextPersisted();
        APILocator.getContentletAPI().setContentletProperty(publishedContentlet, new LegacyFieldTransformer(field).asOldField(), value);

        final Number valueRetrieved = (Number) publishedContentlet.get(keyPropName);
        assertEquals(value.longValue(), valueRetrieved);
    }
}
