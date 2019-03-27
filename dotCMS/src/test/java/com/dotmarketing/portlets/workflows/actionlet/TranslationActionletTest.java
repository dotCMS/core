package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.UnitTestBase;
import com.dotcms.translate.TranslationService;
import com.dotcms.translate.TranslationUtil;
import com.dotcms.translate.TranslationUtilTest;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.liferay.portal.model.User;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dotcms.translate.TranslateTestUtil.getEnglishContent;
import static com.dotcms.translate.TranslateTestUtil.getFieldsForContent;
import static com.dotcms.translate.TranslateTestUtil.getTranslateToAsList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TranslationActionletTest extends UnitTestBase {

    private Contentlet spanishTranslatedContent = mock(Contentlet.class);
    private Contentlet frenchTranslatedContent = mock(Contentlet.class);

    @Before
    public void mockContents() {
        spanishTranslatedContent = mock(Contentlet.class);
        frenchTranslatedContent = mock(Contentlet.class);
    }

    @Test(expected = WorkflowActionFailureException.class)
    public void testExecuteAction_UnpersistedContent() throws Exception {
        User systemUser = new User("systemUser");
        Contentlet unpersisted = new Contentlet();
        unpersisted.setIdentifier("unpersisted");
        unpersisted.setLanguageId(0);

        ContentletAPI contentletAPI = mock(ContentletAPI.class);
        when(contentletAPI
            .findContentletByIdentifier(unpersisted.getIdentifier(), false, unpersisted.getLanguageId(), systemUser,
                false)).thenThrow(DotStateException.class);

        ApiProvider apiProvider = mock(ApiProvider.class);
        when(apiProvider.contentletAPI()).thenReturn(contentletAPI);

        WorkflowProcessor processor = mock(WorkflowProcessor.class);
        when(processor.getContentlet()).thenReturn(unpersisted);
        when(processor.getUser()).thenReturn(systemUser);

        TranslationActionlet actionlet = new TranslationActionlet(apiProvider, null, null);
        actionlet.executeAction(processor, getParams());
    }

    @Test
    public void testExecuteAction_LiveContent() throws Exception {
        User systemUser = new User("systemUser");
        Contentlet englishContent = spy(getEnglishContent());
        doReturn(true).when(englishContent).isLive();
        doNothing().when(englishContent).setTags();

        // Mock TranslationUtil
        TranslationUtil translationUtil = getMockedTranslationUtil(englishContent);

        // Mock TranslationService
        TranslationService translationService = getMockedTranslationService(englishContent, systemUser);

        // Mock WorkflowProcessor
        WorkflowProcessor processor = getMockedWorkflowProcessor(englishContent, systemUser);

        ApiProvider apiProvider = getMockedApiProvider(englishContent, systemUser);

        ContentletAPI contentletAPI = apiProvider.contentletAPI();
        ContentletRelationships conRel = new ContentletRelationships(englishContent);
        when(contentletAPI.getAllRelationships(englishContent)).thenReturn(conRel);

        List<Category> cats = apiProvider.categoryAPI().getParents(englishContent, systemUser, false);
        List<Permission> perms = apiProvider.permissionAPI().getPermissions(englishContent, false, true);

        TranslationActionlet actionlet = spy(new TranslationActionlet(apiProvider, translationUtil, translationService));

        doNothing().when(actionlet).copyBinariesAndTags(any(User.class), any(Contentlet.class), any(Contentlet.class));

        actionlet.executeAction(processor, getParams());

        List<Contentlet> translatedContents = getTranslatedContents();

        for (Contentlet translatedContent : translatedContents) {
            verify(translatedContent).setProperty(Contentlet.DISABLE_WORKFLOW, true);
            verify(contentletAPI)
                .checkin(translatedContent, conRel, cats,
                    perms, systemUser,
                    false);
        }

        verify(contentletAPI, times(translatedContents.size()))
            .publish(any(Contentlet.class), any(User.class), anyBoolean());

        verify(contentletAPI).unlock(englishContent, systemUser, false);

    }

    @Test
    public void testExecuteAction_WorkingContent() throws Exception {
        User systemUser = new User("systemUser");
        Contentlet englishContent = spy(getEnglishContent());
        doReturn(false).when(englishContent).isLive();
        doNothing().when(englishContent).setTags();

        // Mock TranslationUtil
        TranslationUtil translationUtil = getMockedTranslationUtil(englishContent);

        // Mock TranslationService
        TranslationService translationService = getMockedTranslationService(englishContent, systemUser);

        // Mock WorkflowProcessor
        WorkflowProcessor processor = getMockedWorkflowProcessor(englishContent, systemUser);

        ApiProvider apiProvider = getMockedApiProvider(englishContent, systemUser);

        ContentletAPI contentletAPI = apiProvider.contentletAPI();
        ContentletRelationships conRel = new ContentletRelationships(englishContent);
        when(contentletAPI.getAllRelationships(englishContent)).thenReturn(conRel);

        List<Category> cats = apiProvider.categoryAPI().getParents(englishContent, systemUser, false);
        List<Permission> perms = apiProvider.permissionAPI().getPermissions(englishContent, false, true);

        TranslationActionlet actionlet = spy(new TranslationActionlet(apiProvider, translationUtil, translationService));

        doNothing().when(actionlet).copyBinariesAndTags(any(User.class), any(Contentlet.class), any(Contentlet.class));

        actionlet.executeAction(processor, getParams());

        List<Contentlet> translatedContents = getTranslatedContents();

        for (Contentlet translatedContent : translatedContents) {
            verify(translatedContent).setProperty(Contentlet.DISABLE_WORKFLOW, true);
            verify(contentletAPI)
                .checkin(translatedContent, conRel, cats,
                    perms, systemUser,
                    false);
        }

        verify(contentletAPI, never())
            .publish(any(Contentlet.class), any(User.class), anyBoolean());

        verify(contentletAPI).unlock(englishContent, systemUser, false);
    }

    private Map<String, WorkflowActionClassParameter> getParams() {
        Map<String, WorkflowActionClassParameter> params = new HashMap<>();

        WorkflowActionClassParameter translateTo = new WorkflowActionClassParameter();
        translateTo.setValue("all");

        WorkflowActionClassParameter fieldTypes = new WorkflowActionClassParameter();
        fieldTypes.setValue("text,wysiwyg,textarea");

        WorkflowActionClassParameter ignoreFields = new WorkflowActionClassParameter();
        ignoreFields.setValue("");

        params.put("translateTo", translateTo);
        params.put("fieldTypes", fieldTypes);
        params.put("ignoreFields", ignoreFields);

        return params;
    }

    private ContentletAPI getMockedContentletAPI(Contentlet content, User user) throws Exception {
        ContentletAPI contentletAPI = mock(ContentletAPI.class);
        when(contentletAPI
            .findContentletByIdentifier(content.getIdentifier(), false, content.getLanguageId(),
                user, false)).thenReturn(content);

        return contentletAPI;
    }

    public TranslationUtil getMockedTranslationUtil(Contentlet contentlet) {
        TranslationUtil translationUtil = mock(TranslationUtil.class);
        List<Field> fieldsOfContent = getFieldsForContent();
        when(
            translationUtil.getFieldsOfContentlet(contentlet, TranslationUtilTest.filterTypes, new ArrayList<>()))
            .thenReturn(fieldsOfContent);
        List<Language> translateTo = getTranslateToAsList();
        when(translationUtil.getLanguagesByLanguageCodes(Collections.singletonList("all")))
            .thenReturn(translateTo);

        return translationUtil;
    }

    private TranslationService getMockedTranslationService(Contentlet contentlet, User user) throws Exception {
        TranslationService translationService = mock(TranslationService.class);
        when(translationService.translateContent(contentlet, getTranslateToAsList(), getFieldsForContent(), user))
            .thenReturn(getTranslatedContents());

        return translationService;
    }

    private WorkflowProcessor getMockedWorkflowProcessor(Contentlet contentlet, User user) {
        WorkflowProcessor processor = mock(WorkflowProcessor.class);
        when(processor.getContentlet()).thenReturn(contentlet);
        when(processor.getUser()).thenReturn(user);
        return processor;
    }

    private CategoryAPI getMockedCategoryAPI(Contentlet contentlet, User user) throws Exception {
        CategoryAPI categoryAPI = mock(CategoryAPI.class);
        List<Category> cats = new ArrayList<>();
        when(categoryAPI.getParents(contentlet, user, false)).thenReturn(cats);
        return categoryAPI;
    }

    private PermissionAPI getMockedPermissionAPI (Contentlet contentlet) throws Exception {
        PermissionAPI permAPI = mock(PermissionAPI.class);
        List<Permission> perms = new ArrayList<>();
        when(permAPI.getPermissions(contentlet, false, true)).thenReturn(perms);
        return permAPI;
    }

    private ApiProvider getMockedApiProvider(Contentlet contentlet, User user) throws Exception {
        // Mock ContentletAPI
        ContentletAPI contentletAPI = getMockedContentletAPI(contentlet, user);

        // Mock Cat API
        CategoryAPI categoryAPI = getMockedCategoryAPI(contentlet, user);

        // Mock Perm API
        PermissionAPI permAPI = getMockedPermissionAPI(contentlet);

        ApiProvider apiProvider = mock(ApiProvider.class);
        when(apiProvider.contentletAPI()).thenReturn(contentletAPI);
        when(apiProvider.categoryAPI()).thenReturn(categoryAPI);
        when(apiProvider.permissionAPI()).thenReturn(permAPI);
        return apiProvider;
    }

    private List<Contentlet> getTranslatedContents() {
        return Arrays.asList(spanishTranslatedContent, frenchTranslatedContent);
    }
}