package com.dotmarketing.portlets.categories.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.categories.struts.CategoryForm;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.PortletSessionImpl;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link EditCategoryAction}: a request with {@code cmd=delete} is handled like a
 * request with no command and deletes nothing.
 */
class EditCategoryActionTest {

    private static final String EDIT_FORM_FORWARD = "portlet.ext.categories.edit_category";

    private final CategoryAPI categoryAPI = mock(CategoryAPI.class);
    private final User user = new User();

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<InodeFactory> inodeFactory;

    @BeforeEach
    void setUp() {
        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getCategoryAPI).thenReturn(categoryAPI);
        inodeFactory = mockStatic(InodeFactory.class);
    }

    @AfterEach
    void tearDown() {
        inodeFactory.close();
        apiLocator.close();
    }

    /**
     * Method to test: {@link EditCategoryAction#processAction}
     * Given: a back-end user and a request with {@code cmd=delete} and the inode of an existing
     * category
     * Expected: no delete is performed (neither {@code InodeFactory.deleteInode} nor any
     * {@code CategoryAPI} delete method is called), the category is loaded into the request and
     * the request is forwarded to the edit form
     */
    @Test
    void processAction_deleteCommand_deletesNothingAndShowsTheEditForm() throws Exception {
        final Category category = new Category();
        category.setInode(UUID.randomUUID().toString());
        category.setCategoryName("Category A");
        category.setSortOrder(1);
        when(categoryAPI.find(category.getInode(), user, false)).thenReturn(category);
        when(categoryAPI.findTopLevelCategories(user, false)).thenReturn(Collections.emptyList());

        final Map<String, Object> requestAttributes = new HashMap<>();
        final ActionRequestImpl request = request(Constants.DELETE, category.getInode(), requestAttributes);

        new EditCategoryAction().processAction(mock(ActionMapping.class), new CategoryForm(), null,
                request, mock(ActionResponse.class));

        inodeFactory.verify(() -> InodeFactory.deleteInode(any()), never());
        verify(categoryAPI, never()).delete(any(Category.class), any(User.class), anyBoolean());
        verify(categoryAPI, never()).deleteAll(any(User.class), anyBoolean());
        verify(categoryAPI, never()).deleteCategoryAndChildren(anyList(), any(User.class), anyBoolean());
        assertSame(category, requestAttributes.get(com.dotmarketing.util.WebKeys.CATEGORY_EDIT));
        assertEquals(EDIT_FORM_FORWARD, requestAttributes.get(WebKeys.PORTLET_STRUTS_FORWARD));
    }

    /**
     * A request of {@link #user} with the given command and category inode, whose attributes are
     * kept in {@code attributes}, and a portlet session that keeps the messages the action adds.
     */
    private ActionRequestImpl request(final String cmd, final String inode,
            final Map<String, Object> attributes) {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getAttribute(WebKeys.USER)).thenReturn(user);

        final Map<String, Object> sessionAttributes = new HashMap<>();
        final HttpSession httpSession = mock(HttpSession.class);
        when(httpSession.getAttribute(anyString()))
                .thenAnswer(invocation -> sessionAttributes.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> sessionAttributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(httpSession).setAttribute(anyString(), any());
        when(httpRequest.getSession()).thenReturn(httpSession);
        when(httpRequest.getSession(false)).thenReturn(httpSession);
        final PortletSessionImpl portletSession = mock(PortletSessionImpl.class);
        when(portletSession.getSession()).thenReturn(httpSession);

        final ActionRequestImpl request = mock(ActionRequestImpl.class);
        when(request.getHttpServletRequest()).thenReturn(httpRequest);
        when(request.getPortletSession()).thenReturn(portletSession);
        when(request.getParameter(Constants.CMD)).thenReturn(cmd);
        when(request.getParameter("inode")).thenReturn(inode);
        when(request.getAttribute(anyString()))
                .thenAnswer(invocation -> attributes.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        return request;
    }
}
