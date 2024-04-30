package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class SaveContentAsDraftActionletTest {

    private SaveContentAsDraftActionlet saveContentAsDraftActionlet;
    private ContentletAPI contentletAPI;
    private CategoryAPI categoryAPI;
    private PermissionAPI permissionAPI;

    @Before
    public void init(){
        contentletAPI = mock(ContentletAPI.class);
        categoryAPI = mock(CategoryAPI.class);
        permissionAPI = mock(PermissionAPI.class);

        saveContentAsDraftActionlet = new SaveContentAsDraftActionlet(contentletAPI, categoryAPI, permissionAPI);
    }

    private static class TestCase {
        final private boolean respectFrontendRoles;

        private TestCase(final boolean respectFrontendRoles) {
            this.respectFrontendRoles = respectFrontendRoles;
        }
    }

    @DataProvider
    public static Object[] respectFrontendRolesValues() {
        return new TestCase[]{new TestCase(true), new TestCase(false)};
    }

    /**
     * When: {@link WorkflowProcessor#getContentletDependencies()} is not null
     * Should: call {@link ContentletAPI#saveDraft(Contentlet, Map, List, List, User, boolean)} with the right parameters
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    @UseDataProvider("respectFrontendRolesValues")
    public void shouldCallSaveDraftWithTheRightParameters(final TestCase testCase) throws DotSecurityException, DotDataException {
        final WorkflowProcessor processor = mock(WorkflowProcessor.class);
        final Map<String, WorkflowActionClassParameter> params = new HashMap<>();

        final Contentlet contentlet = mock(Contentlet.class);
        final User user  = mock(User.class);

        when(processor.getUser()).thenReturn(user);
        when(processor.getContentlet()).thenReturn(contentlet);

        final List<Category> categories = mock(List.class);

        final List<Permission> permissions = mock(List.class);
        when(this.permissionAPI.getPermissions(contentlet, false, true)).thenReturn(permissions);

        final ContentletDependencies contentletDependencies = mock(ContentletDependencies.class);
        when(processor.getContentletDependencies()).thenReturn(contentletDependencies);
        when(contentletDependencies.getPermissions()).thenReturn(permissions);

        final Contentlet contentletNew = mock(Contentlet.class);
        when(contentletDependencies.getCategories()).thenReturn(categories);
        when(contentletDependencies.getPermissions()).thenReturn(permissions);
        when(processor.getContentletDependencies().isRespectAnonymousPermissions()).thenReturn(testCase.respectFrontendRoles);

        when(this.contentletAPI.saveDraft(
                contentlet, (ContentletRelationships) null, categories, permissions, user, testCase.respectFrontendRoles))
                .thenReturn(contentletNew);

        saveContentAsDraftActionlet.executeAction(processor, params);

        verify(processor, times(1)).setContentlet(contentletNew);
        verify(this.contentletAPI, times(1)).saveDraft(
                contentlet, (ContentletRelationships) null, categories, permissions, user, testCase.respectFrontendRoles
        );

        verify(this.categoryAPI, never()).getParents(contentlet, user, false);
    }

    /**
     * When: {@link WorkflowProcessor#getContentletDependencies()} is null
     * Should: call {@link ContentletAPI#saveDraft(Contentlet, Map, List, List, User, boolean)} with the right parameters
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    @UseDataProvider("respectFrontendRolesValues")
    public void shouldCallSaveDraftWithTheRightParametersWhenContentletDependenciesIsNull(final TestCase testCase) throws DotSecurityException, DotDataException {
        final WorkflowProcessor processor = mock(WorkflowProcessor.class);
        final Map<String, WorkflowActionClassParameter> params = new HashMap<>();

        final Contentlet contentlet = mock(Contentlet.class);
        final User user  = mock(User.class);

        when(processor.getUser()).thenReturn(user);
        when(processor.getContentlet()).thenReturn(contentlet);

        List<Category> categories = mock(List.class);

        final List<Permission> permissions = mock(List.class);
        when(this.permissionAPI.getPermissions(contentlet, false, true)).thenReturn(permissions);

        when(this.categoryAPI.getParents(contentlet, user, testCase.respectFrontendRoles)).thenReturn(categories);

        when(processor.getContentletDependencies()).thenReturn(null);

        final Contentlet contentletNew = mock(Contentlet.class);

        when(user.isFrontendUser()).thenReturn(testCase.respectFrontendRoles);
        when(this.contentletAPI.saveDraft(
                contentlet, (ContentletRelationships) null, categories, permissions, user, testCase.respectFrontendRoles))
                .thenReturn(contentletNew);

        saveContentAsDraftActionlet.executeAction(processor, params);

        verify(processor, times(1)).setContentlet(contentletNew);
        verify(this.contentletAPI, times(1)).saveDraft(
                contentlet, (ContentletRelationships) null, categories, permissions, user, testCase.respectFrontendRoles
        );

        verify(this.categoryAPI, times(1)).getParents(contentlet, user, testCase.respectFrontendRoles);
    }
}
