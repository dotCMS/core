package com.dotmarketing.portlets.contentlet.model;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.liferay.portal.model.User;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ContentletTest {

    @Test
    public void testGetTitle_WhenExistingTitleField_returnItsValue() throws DotDataException, DotSecurityException {
        final ContentletAPI contentletAPI = Mockito.mock(ContentletAPI.class);
        final UserAPI userAPI = Mockito.mock(UserAPI.class);
        Mockito.when(userAPI.getSystemUser()).thenReturn(new User());
        final Contentlet contentlet = Mockito.spy(Contentlet.class);
        contentlet.setUserAPI(userAPI);
        contentlet.setContentletAPI(contentletAPI);
        final ContentType mockContentType = Mockito.mock(ContentType.class);
        final Field fieldWithTitle = createFieldWithVarname("title");
        final List<Field> mockFieldWithTitle = List.of(fieldWithTitle);
        Mockito.doReturn(mockContentType).when(contentlet).getContentType();
        Mockito.when(mockContentType.fields()).thenReturn(mockFieldWithTitle);

        final String expectedValue = "ManuallyAddedFieldValue";
        contentlet.getMap().put("title", expectedValue );

        assertEquals(contentlet.getTitle(), expectedValue);
    }

    /**
     * Regression test for <a href="https://github.com/dotCMS/core/issues/35084">Issue #35084</a>.
     * <p>
     * When a Host content type has the "aliases" field listed before "hostName" in the field
     * order, {@link Contentlet#getName()} must still return the hostName value — not the aliases.
     * The bug: {@code buildName()} iterates listed text fields in order and returns the first match,
     * so aliases (coming first) was incorrectly used as the title.
     */
    @Test
    public void testGetName_WhenHostTypeHasAliasesListedBeforeHostName_ShouldReturnHostName() throws DotDataException, DotSecurityException {
        final ContentletAPI contentletAPI = Mockito.mock(ContentletAPI.class);
        final UserAPI userAPI = Mockito.mock(UserAPI.class);
        Mockito.when(userAPI.getSystemUser()).thenReturn(new User());
        final Contentlet contentlet = Mockito.spy(Contentlet.class);
        contentlet.setUserAPI(userAPI);
        contentlet.setContentletAPI(contentletAPI);

        // Mock content type with velocity variable "Host" and fields in order: aliases first, hostName second.
        // Neither field variable starts with "title", so getFieldWithVarStartingWithTitleWord() returns empty.
        final ContentType mockContentType = Mockito.mock(ContentType.class);
        Mockito.when(mockContentType.variable()).thenReturn("Host");
        Mockito.doReturn(mockContentType).when(contentlet).getContentType();
        Mockito.when(mockContentType.fields()).thenReturn(List.of(
                createFieldWithVarname("aliases"),
                createFieldWithVarname("hostName")
        ));

        // Set field values — aliases first, hostName second — but leave the title blank.
        final String aliasValue = "alias1.com\nalias2.com";
        final String hostNameValue = "mysite.com";
        contentlet.getMap().put("aliases", aliasValue);
        contentlet.getMap().put("hostName", hostNameValue);
        contentlet.getMap().put(Contentlet.STRUCTURE_INODE_KEY, "fake-host-structure-inode");

        // Legacy Field mocks returned by FieldsCache: aliases listed first, hostName second.
        final com.dotmarketing.portlets.structure.model.Field aliasesLegacyField =
                Mockito.mock(com.dotmarketing.portlets.structure.model.Field.class);
        Mockito.when(aliasesLegacyField.isListed()).thenReturn(true);
        Mockito.when(aliasesLegacyField.getVelocityVarName()).thenReturn("aliases");

        final com.dotmarketing.portlets.structure.model.Field hostNameLegacyField =
                Mockito.mock(com.dotmarketing.portlets.structure.model.Field.class);
        Mockito.when(hostNameLegacyField.isListed()).thenReturn(true);
        Mockito.when(hostNameLegacyField.getVelocityVarName()).thenReturn("hostName");

        Mockito.when(contentletAPI.isFieldTypeString(aliasesLegacyField)).thenReturn(true);
        Mockito.when(contentletAPI.isFieldTypeString(hostNameLegacyField)).thenReturn(true);

        try (MockedStatic<FieldsCache> fieldsCacheMock = Mockito.mockStatic(FieldsCache.class);
             MockedStatic<APILocator> apiLocatorMock = Mockito.mockStatic(APILocator.class)) {

            // aliases is listed first in the field order — this is the bug trigger
            fieldsCacheMock.when(() -> FieldsCache.getFieldsByStructureInode(Mockito.any()))
                    .thenReturn(List.of(aliasesLegacyField, hostNameLegacyField));

            apiLocatorMock.when(APILocator::getContentletAPI).thenReturn(contentletAPI);

            // getName() must return the hostName, not the aliases.
            // This assertion currently FAILS because buildName() picks the first listed text field
            // (aliases) instead of using hostName as the authoritative name for Host content,
            // causing DOT_NAME_KEY to be set to the aliases value.
            assertEquals(hostNameValue, contentlet.getName());
        }
    }

    /**
     * Regression test for <a href="https://github.com/dotCMS/core/issues/35188">Issue #35188</a>.
     * <p>
     * Before the fix, {@code BinaryViewStrategy} wrote {@code Collections.emptyMap()} into the
     * contentlet's backing map under the bare field variable key for empty binary fields. A
     * subsequent call to {@link Contentlet#getBinary} would then try to cast that {@code Map} to
     * {@code File}, throwing a {@code ClassCastException} (surfaced as
     * {@code DotDataException: null}).
     * <p>
     * After the fix, {@code getBinary} uses an {@code instanceof} guard and returns {@code null}
     * for any non-{@code File} value rather than throwing.
     */
    @Test
    public void testGetBinary_whenMapContainsNonFileValue_returnsNullWithoutException()
            throws Exception {

        final Contentlet contentlet = new Contentlet();
        // Simulate the map-poisoning that BinaryViewStrategy caused before the fix.
        contentlet.getMap().put("productImage", Collections.emptyMap());

        final java.io.File result = contentlet.getBinary("productImage");

        assertNull("getBinary() must return null — not throw ClassCastException — "
                + "when the backing map holds a non-File value for the field key", result);
    }

    private Field createFieldWithVarname(final String varname) {
        return ImmutableTextField.builder()
                .name(varname)
                .variable(varname)
                .contentTypeId("123")
                .dataType(DataTypes.TEXT)
                .build();
    }
}
