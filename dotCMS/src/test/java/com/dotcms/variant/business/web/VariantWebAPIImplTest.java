package com.dotcms.variant.business.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.liferay.portal.model.User;
import org.junit.Test;

/**
 * Unit tests for how {@link VariantWebAPIImpl} scopes a caller's Lucene query to a variant.
 *
 * <p>The rewrite is pure string work and is worth testing on its own, because the shapes a
 * caller can write a variant term in are the whole point: the clause imposed further down the
 * search path is what keeps an unrecognised shape from reading another variant, but a shape that
 * is dropped correctly is the difference between answering with the default version and
 * answering with nothing.
 */
public class VariantWebAPIImplTest {

    private final VariantWebAPIImpl variantWebAPI = new VariantWebAPIImpl();

    /**
     * @return a {@link User} that is allowed to name its own variant
     */
    private static User backEndUser() {
        final User user = mock(User.class);
        when(user.isBackendUser()).thenReturn(true);
        return user;
    }

    /**
     * @return a {@link User} that is not allowed to name its own variant
     */
    private static User frontEndUser() {
        final User user = mock(User.class);
        when(user.isBackendUser()).thenReturn(false);
        return user;
    }

    @Test
    public void testScopeQueryToDefaultVariant_WhenBackEndUser_ShouldLeaveTheQueryAlone() {
        final String query = "+identifier:abc +variant:someVariant";
        assertSame(query, variantWebAPI.scopeQueryToDefaultVariant(query, backEndUser()));
    }

    @Test
    public void testScopeQueryToDefaultVariant_WhenNoUser_ShouldImposeTheDefaultVariant() {
        assertEquals("+variant:default +identifier:abc",
                variantWebAPI.scopeQueryToDefaultVariant("+identifier:abc", null));
    }

    @Test
    public void testScopeQueryToDefaultVariant_WhenQueryIsEmpty_ShouldStillImposeTheClause() {
        assertEquals("+variant:default ", variantWebAPI.scopeQueryToDefaultVariant("", null));
        assertEquals("+variant:default ", variantWebAPI.scopeQueryToDefaultVariant(null, null));
    }

    @Test
    public void testScopeQueryToDefaultVariant_WhenCallerNamesAVariant_ShouldDropItsTerm() {
        final User user = frontEndUser();

        assertEquals("+variant:default +identifier:abc",
                variantWebAPI.scopeQueryToDefaultVariant("+identifier:abc +variant:someVariant", user));
        assertEquals("+variant:default +identifier:abc",
                variantWebAPI.scopeQueryToDefaultVariant("+variant:someVariant +identifier:abc", user));
        assertEquals("+variant:default +identifier:abc",
                variantWebAPI.scopeQueryToDefaultVariant("variant:someVariant +identifier:abc", user));
    }

    /**
     * The negated form is the one the public reproduction of this behaviour uses, so it is the
     * shape most likely to arrive.
     */
    @Test
    public void testScopeQueryToDefaultVariant_WhenCallerNegatesTheDefault_ShouldDropItsTerm() {
        assertEquals("+variant:default +contentType:*",
                variantWebAPI.scopeQueryToDefaultVariant("+contentType:* -variant:DEFAULT",
                        frontEndUser()));
    }

    @Test
    public void testScopeQueryToDefaultVariant_WhenTermIsQuotedOrGrouped_ShouldDropItWhole() {
        final User user = frontEndUser();

        assertEquals("+variant:default +identifier:abc",
                variantWebAPI.scopeQueryToDefaultVariant("+identifier:abc +variant:\"some variant\"", user));
        assertEquals("+variant:default +identifier:abc",
                variantWebAPI.scopeQueryToDefaultVariant("+identifier:abc +variant:(one OR two)", user));
    }

    @Test
    public void testScopeQueryToDefaultVariant_WhenFieldNameOnlyContainsVariant_ShouldKeepIt() {
        assertEquals("+variant:default +variantName:someVariant +myvariant:other",
                variantWebAPI.scopeQueryToDefaultVariant(
                        "+variantName:someVariant +myvariant:other", frontEndUser()));
    }

    /**
     * A phrase that happens to contain the field name is the caller's data, not a variant term.
     */
    @Test
    public void testScopeQueryToDefaultVariant_WhenTermAppearsInsideAPhrase_ShouldKeepIt() {
        assertEquals("+variant:default +title:\"variant:someVariant\"",
                variantWebAPI.scopeQueryToDefaultVariant("+title:\"variant:someVariant\"",
                        frontEndUser()));
    }

    /**
     * Taking a term out of a group would leave the group malformed, so it is left in place and
     * the clause imposed further down the search path is what keeps it from reading anything.
     */
    @Test
    public void testScopeQueryToDefaultVariant_WhenTermIsInsideAGroup_ShouldKeepItAndStillImposeTheClause() {
        assertEquals("+variant:default +(variant:one OR variant:two)",
                variantWebAPI.scopeQueryToDefaultVariant("+(variant:one OR variant:two)",
                        frontEndUser()));
    }
}
