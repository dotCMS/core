package com.dotmarketing.portlets.calendar.business;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.ContentTypeCache;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link EventFactoryImpl#find}: an event whose base event no longer exists is saved again without
 * that link, and the save keeps all of its categories whoever runs the search.
 */
class EventFactoryImplTest {

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<CacheLocator> cacheLocator;

    private ContentletAPI contentletAPI;
    private CategoryAPI categoryAPI;
    private User systemUser;

    @BeforeEach
    void setUp() throws Exception {
        apiLocator = mockStatic(APILocator.class);
        cacheLocator = mockStatic(CacheLocator.class);

        contentletAPI = mock(ContentletAPI.class);
        categoryAPI = mock(CategoryAPI.class);
        final HostAPI hostAPI = mock(HostAPI.class);
        final UserAPI userAPI = mock(UserAPI.class);
        systemUser = mock(User.class);
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(hostAPI.findSystemHost(any(User.class), anyBoolean())).thenReturn(mock(Host.class));

        apiLocator.when(APILocator::getContentletAPI).thenReturn(contentletAPI);
        apiLocator.when(APILocator::getCategoryAPI).thenReturn(categoryAPI);
        apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
        apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);
        apiLocator.when(APILocator::getLanguageAPI).thenReturn(mock(LanguageAPI.class));
        apiLocator.when(APILocator::getPermissionAPI).thenReturn(mock(PermissionAPI.class));

        final Structure eventType = mock(Structure.class);
        final Field field = mock(Field.class);
        when(field.getFieldContentlet()).thenReturn("date1");
        when(field.getVelocityVarName()).thenReturn("field");
        when(eventType.getFieldVar(anyString())).thenReturn(field);
        when(eventType.getInode()).thenReturn("event-type");
        when(eventType.getVelocityVarName()).thenReturn("calendarEvent");
        final ContentTypeCache contentTypeCache = mock(ContentTypeCache.class);
        when(contentTypeCache.getStructureByVelocityVarName(EventAPI.EVENT_STRUCTURE_VAR)).thenReturn(eventType);
        cacheLocator.when(CacheLocator::getContentTypeCache).thenReturn(contentTypeCache);
    }

    @AfterEach
    void tearDown() {
        cacheLocator.close();
        apiLocator.close();
    }

    @Test
    void find_eventWithMissingBaseEvent_isSavedWithAllItsCategories() throws Exception {
        // Given an event linked to a base event that no longer exists, with a category the viewer cannot read
        final Event event = mock(Event.class);
        when(event.getDisconnectedFrom()).thenReturn("missing-base-event");
        final Contentlet hit = mock(Contentlet.class);
        when(hit.getInode()).thenReturn("event-inode");
        when(contentletAPI.search(anyString(), anyInt(), anyInt(), anyString(), any(User.class), anyBoolean()))
                .thenReturn(List.of(hit));
        when(contentletAPI.findContentlets(anyList())).thenReturn(List.of(hit));

        final User viewer = mock(User.class);
        final Category visible = mock(Category.class);
        final Category hidden = mock(Category.class);
        final List<Category> viewerCategories = List.of(visible);
        final List<Category> allCategories = List.of(visible, hidden);
        when(categoryAPI.getParents(event, viewer, true)).thenReturn(viewerCategories);
        when(categoryAPI.getParents(event, systemUser, false)).thenReturn(allCategories);

        final EventFactoryImpl factory = new EventFactoryImpl() {
            @Override
            protected Event convertToEvent(final Contentlet contentlet) {
                return event;
            }

            @Override
            protected Event find(final String id, final boolean live, final User user,
                    final boolean respectFrontendRoles) {
                return null;
            }
        };

        // When a viewer searches the events
        factory.find(new Date(), new Date(), null, null, new ArrayList<>(), true, false, 0, 5, viewer, true);

        // Then the event is saved again keeping every category, not only the ones the viewer can read
        verify(contentletAPI).checkin(eq(event), eq(allCategories), any(), eq(systemUser), eq(false));
    }
}
