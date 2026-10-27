package com.dotmarketing.portlets.structure.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.rendering.velocity.services.ContentTypeLoader;
import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cache.ContentTypeCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.structure.struts.StructureForm;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.Validator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.PortletSessionImpl;
import com.liferay.util.servlet.SessionMessages;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Unit tests for saving a content type ({@code cmd=add}) and making it the default one
 * ({@code cmd=SET_DEFAULT}) through {@link EditStructureAction}: both run with the permissions of
 * the logged-in user, and what the user is not allowed to do changes nothing. The logged-in user
 * comes from a mocked portlet request and every collaborator is mocked.
 */
class EditStructureActionTest {

    private static final String TYPE_INODE = "type-inode";
    private static final String SCHEME_ID = "scheme-id";
    private static final String REFERER = "/c/portal/layout?p_l_id=content-types";
    /** Session key of the error message the action shows. */
    private static final String ERROR_KEY = "error";
    /** Session key of the action's other messages: the success message or a warning. */
    private static final String MESSAGE_KEY = "message";
    /** The message shown when the content type was saved. */
    private static final String SAVED_MESSAGE = "message.structure.savestructure";
    /** The message shown when the user may not save the type. */
    private static final String NOT_ALLOWED_MESSAGE = "message.insufficient.permissions.to.save";
    /** The message shown when the content type was made the default one. */
    private static final String DEFAULT_SET_MESSAGE = "message.structure.defaultstructure";
    /** The message shown when the user may not make the type the default one. */
    private static final String DEFAULT_NOT_ALLOWED_MESSAGE = "dont-have-permissions-msg";
    /** Text carried by the refusal or the data error, which must not reach the page. */
    private static final String EXCEPTION_TEXT = "exception-text";

    private final ContentTypeAPI contentTypeAPI = mock(ContentTypeAPI.class);
    private final WorkflowAPI workflowAPI = mock(WorkflowAPI.class);
    private final ContentTypeCache contentTypeCache = mock(ContentTypeCache.class);
    private final User user = mock(User.class);
    private final User systemUser = mock(User.class);
    private final Map<String, Object> sessionAttributes = new HashMap<>();

    /** Names of the {@link SecurityLogger} methods called during a test. */
    private final List<String> securityLoggerCalls = new ArrayList<>();

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<CacheLocator> cacheLocator;
    private MockedStatic<StructureFactory> structureFactory;
    private MockedStatic<HibernateUtil> hibernateUtil;
    private MockedStatic<Validator> validator;
    private MockedStatic<PortalUtil> portalUtil;
    private MockedStatic<SecurityUtils> securityUtils;
    private MockedStatic<Logger> logger;
    private MockedStatic<SecurityLogger> securityLogger;
    private MockedConstruction<ContentTypeLoader> contentTypeLoader;

    @BeforeEach
    void setUp() throws Exception {
        when(user.getUserId()).thenReturn("back-end-user");

        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(() -> APILocator.getContentTypeAPI(user)).thenReturn(contentTypeAPI);
        apiLocator.when(APILocator::getWorkflowAPI).thenReturn(workflowAPI);
        apiLocator.when(APILocator::systemUser).thenReturn(systemUser);
        cacheLocator = mockStatic(CacheLocator.class);
        cacheLocator.when(CacheLocator::getContentTypeCache).thenReturn(contentTypeCache);
        structureFactory = mockStatic(StructureFactory.class);
        hibernateUtil = mockStatic(HibernateUtil.class);
        validator = mockStatic(Validator.class);
        validator.when(() -> Validator.validate(any(ActionRequest.class), any(), any())).thenReturn(true);
        portalUtil = mockStatic(PortalUtil.class);
        securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(() -> SecurityUtils.stripReferer(any(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        logger = mockStatic(Logger.class);
        securityLogger = mockStatic(SecurityLogger.class, invocation -> {
            securityLoggerCalls.add(invocation.getMethod().getName());
            return RETURNS_DEFAULTS.answer(invocation);
        });
        contentTypeLoader = mockConstruction(ContentTypeLoader.class);

        final ContentType type = contentType(TYPE_INODE, "SavedType");
        when(contentTypeAPI.find(TYPE_INODE)).thenReturn(type);
        when(contentTypeAPI.findDefault()).thenReturn(contentType("default-type-inode", "DefaultType"));
        when(workflowAPI.findScheme(SCHEME_ID)).thenReturn(mock(WorkflowScheme.class));
    }

    @AfterEach
    void tearDown() {
        contentTypeLoader.close();
        securityLogger.close();
        logger.close();
        securityUtils.close();
        portalUtil.close();
        validator.close();
        hibernateUtil.close();
        structureFactory.close();
        cacheLocator.close();
        apiLocator.close();
    }

    /**
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=add}
     * Given: a logged-in back-end user posts changes to an existing content type
     * Expected: the type is saved with that user's permissions, not through the unchecked save
     * used by internal callers, and its workflow schemes are saved after it
     */
    @Test
    void save_usesRequestingUser() throws Exception {
        final ActionRequestImpl request = saveRequest();
        new EditStructureAction().processAction(null, structureForm(), null, request,
                mock(ActionResponse.class));

        structureFactory.verify(() -> StructureFactory.saveStructure(any(Structure.class)), never());
        structureFactory.verify(() -> StructureFactory.saveStructure(any(Structure.class), eq(user)));
        verify(workflowAPI).saveSchemesForStruct(any(Structure.class), any());
        verify(contentTypeCache).add(any(Structure.class));
        assertEquals(SAVED_MESSAGE, SessionMessages.get(request, MESSAGE_KEY));
    }

    /**
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=add}
     * Given: the user posts workflow schemes for a content type they are not allowed to save
     * Expected: no workflow scheme is saved for the type and the content type cache is not updated
     */
    @Test
    void save_refused_keepsWorkflowSchemes() throws Exception {
        givenSaveFailsWith(new DotSecurityException(EXCEPTION_TEXT));

        new EditStructureAction().processAction(null, structureForm(), null, saveRequest(),
                mock(ActionResponse.class));

        verify(workflowAPI, never()).saveSchemesForStruct(any(Structure.class), any());
        verify(contentTypeCache, never()).add(any(Structure.class));
    }

    /**
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=add}
     * Given: the user posts changes to a content type they are not allowed to save
     * Expected: the page shows the fixed not-allowed message instead of the exception text, no
     * success message, and the refusal is written once to the security log
     */
    @Test
    void save_refused_showsFixedMessageAndLogsOnce() throws Exception {
        givenSaveFailsWith(new DotSecurityException(EXCEPTION_TEXT));

        final ActionRequestImpl request = saveRequest();
        new EditStructureAction().processAction(null, structureForm(), null, request,
                mock(ActionResponse.class));

        assertEquals(NOT_ALLOWED_MESSAGE, SessionMessages.get(request, ERROR_KEY));
        assertNotEquals(SAVED_MESSAGE, SessionMessages.get(request, MESSAGE_KEY));
        assertEquals(1, securityLoggerCalls.size(),
                "one security log entry expected, got " + securityLoggerCalls);
    }

    /**
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=add}
     * Given: saving the content type fails with a data error
     * Expected: no workflow scheme is saved, an error message other than the not-allowed one is
     * shown, and nothing is written to the security log
     */
    @Test
    void save_dataError_isNotLoggedAsRefusal() throws Exception {
        givenSaveFailsWith(new DotHibernateException(EXCEPTION_TEXT));

        final ActionRequestImpl request = saveRequest();
        new EditStructureAction().processAction(null, structureForm(), null, request,
                mock(ActionResponse.class));

        verify(workflowAPI, never()).saveSchemesForStruct(any(Structure.class), any());
        assertTrue(SessionMessages.contains(request, ERROR_KEY));
        assertNotEquals(NOT_ALLOWED_MESSAGE, SessionMessages.get(request, ERROR_KEY));
        assertTrue(securityLoggerCalls.isEmpty(),
                "no security log entry expected, got " + securityLoggerCalls);
    }

    /**
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=add}
     * Given: the user may not read the default content type, which the save looks up first
     * Expected: nothing is saved, the page shows the fixed not-allowed message instead of the
     * exception text, and the refusal is written once to the security log
     */
    @Test
    void save_defaultTypeLookupRefused_showsFixedMessageAndSavesNothing() throws Exception {
        when(contentTypeAPI.findDefault()).thenThrow(new DotSecurityException(EXCEPTION_TEXT));

        final ActionRequestImpl request = saveRequest();
        new EditStructureAction().processAction(null, structureForm(), null, request,
                mock(ActionResponse.class));

        structureFactory.verify(() -> StructureFactory.saveStructure(any(Structure.class), eq(user)),
                never());
        verify(workflowAPI, never()).saveSchemesForStruct(any(Structure.class), any());
        assertEquals(NOT_ALLOWED_MESSAGE, SessionMessages.get(request, ERROR_KEY));
        assertEquals(1, securityLoggerCalls.size(),
                "one security log entry expected, got " + securityLoggerCalls);
    }

    /**
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=add}
     * Given: the type is saved, but looking up one of the posted workflow schemes is refused
     * Expected: an error is shown, but not the not-allowed message for the save, and nothing is
     * written to the security log
     */
    @Test
    void save_schemeLookupRefusedAfterSave_isNotReportedAsSaveRefusal() throws Exception {
        when(workflowAPI.findScheme(SCHEME_ID)).thenThrow(new DotSecurityException(EXCEPTION_TEXT));

        final ActionRequestImpl request = saveRequest();
        new EditStructureAction().processAction(null, structureForm(), null, request,
                mock(ActionResponse.class));

        structureFactory.verify(() -> StructureFactory.saveStructure(any(Structure.class), eq(user)));
        assertTrue(SessionMessages.contains(request, ERROR_KEY));
        assertNotEquals(NOT_ALLOWED_MESSAGE, SessionMessages.get(request, ERROR_KEY));
        assertTrue(securityLoggerCalls.isEmpty(),
                "no security log entry expected, got " + securityLoggerCalls);
    }

    /**
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=SET_DEFAULT}
     * Given: the user may make the loaded content type the default one
     * Expected: the content type is made the default one as that user, the success message and
     * nothing written to the security log
     */
    @Test
    void setDefault_allowed_showsSuccessMessage() throws Exception {
        final ActionRequestImpl request = setDefaultRequest();
        new EditStructureAction().processAction(null, structureForm(), null, request,
                mock(ActionResponse.class));

        verify(contentTypeAPI).setAsDefault(any(ContentType.class));
        assertEquals(DEFAULT_SET_MESSAGE, SessionMessages.get(request, MESSAGE_KEY));
        assertTrue(securityLoggerCalls.isEmpty(),
                "no security log entry expected, got " + securityLoggerCalls);
    }

    /**
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=SET_DEFAULT}
     * Given: the user may not make the loaded content type the default one
     * Expected: the page shows the fixed not-allowed message instead of the exception text, no
     * success message, and the refusal is written once to the security log
     */
    @Test
    void setDefault_refused_showsFixedMessageAndLogsOnce() throws Exception {
        when(contentTypeAPI.setAsDefault(any(ContentType.class)))
                .thenThrow(new DotSecurityException(EXCEPTION_TEXT));

        final ActionRequestImpl request = setDefaultRequest();
        new EditStructureAction().processAction(null, structureForm(), null, request,
                mock(ActionResponse.class));

        assertEquals(DEFAULT_NOT_ALLOWED_MESSAGE, SessionMessages.get(request, ERROR_KEY));
        assertNotEquals(DEFAULT_SET_MESSAGE, SessionMessages.get(request, MESSAGE_KEY));
        assertEquals(1, securityLoggerCalls.size(),
                "one security log entry expected, got " + securityLoggerCalls);
    }

    /**
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=SET_DEFAULT}
     * Given: making the loaded content type the default one fails with a data error
     * Expected: no success message and nothing written to the security log
     */
    @Test
    void setDefault_dataError_isNotLoggedAsRefusal() throws Exception {
        when(contentTypeAPI.setAsDefault(any(ContentType.class)))
                .thenThrow(new DotDataException(EXCEPTION_TEXT));

        final ActionRequestImpl request = setDefaultRequest();
        new EditStructureAction().processAction(null, structureForm(), null, request,
                mock(ActionResponse.class));

        assertNotEquals(DEFAULT_SET_MESSAGE, SessionMessages.get(request, MESSAGE_KEY));
        assertTrue(securityLoggerCalls.isEmpty(),
                "no security log entry expected, got " + securityLoggerCalls);
    }

    /** Makes the user save of any structure throw the given exception. */
    private void givenSaveFailsWith(final Exception exception) {
        structureFactory.when(() -> StructureFactory.saveStructure(any(Structure.class), eq(user)))
                .thenThrow(exception);
    }

    /** A content type stored on the System Host, with no fields. */
    private static ContentType contentType(final String inode, final String name) {
        final ContentType type = ContentTypeBuilder.builder(SimpleContentType.class)
                .id(inode)
                .name(name)
                .variable(name)
                .host(Host.SYSTEM_HOST)
                .folder(Folder.SYSTEM_FOLDER)
                .build();
        type.constructWithFields(Collections.emptyList());
        return type;
    }

    /** The posted form: the existing type, renamed, on the System Host. */
    private static StructureForm structureForm() {
        final StructureForm form = new StructureForm();
        form.setInode(TYPE_INODE);
        form.setName("RenamedType");
        form.setHost(Host.SYSTEM_HOST);
        form.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
        return form;
    }

    /** A {@code cmd=SET_DEFAULT} request of the logged-in user for the existing type. */
    private ActionRequestImpl setDefaultRequest() {
        final ActionRequestImpl request = saveRequest();
        when(request.getParameter("cmd")).thenReturn(com.dotmarketing.util.WebKeys.Structure.SET_DEFAULT);
        return request;
    }

    /**
     * A {@code cmd=add} request of the logged-in user for the existing type, with one workflow
     * scheme and a referer, and a portlet session that keeps the messages the action adds.
     */
    private ActionRequestImpl saveRequest() {
        final HttpSession httpSession = mock(HttpSession.class);
        when(httpSession.getAttribute(anyString()))
                .thenAnswer(invocation -> sessionAttributes.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> sessionAttributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(httpSession).setAttribute(anyString(), any());
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getSession()).thenReturn(httpSession);
        when(httpRequest.getSession(false)).thenReturn(httpSession);
        final PortletSessionImpl portletSession = mock(PortletSessionImpl.class);
        when(portletSession.getSession()).thenReturn(httpSession);

        final ActionRequestImpl request = mock(ActionRequestImpl.class);
        when(request.getHttpServletRequest()).thenReturn(httpRequest);
        when(request.getPortletSession()).thenReturn(portletSession);
        when(request.getParameter("cmd")).thenReturn("add");
        when(request.getParameter("inode")).thenReturn(TYPE_INODE);
        when(request.getParameter("referer")).thenReturn(REFERER);
        when(request.getParameterValues("workflowScheme")).thenReturn(new String[]{SCHEME_ID});
        final Map<String, Object> requestAttributes = new HashMap<>();
        when(request.getAttribute(anyString()))
                .thenAnswer(invocation -> requestAttributes.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> requestAttributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        portalUtil.when(() -> PortalUtil.getUser((ActionRequest) request)).thenReturn(user);
        return request;
    }
}
