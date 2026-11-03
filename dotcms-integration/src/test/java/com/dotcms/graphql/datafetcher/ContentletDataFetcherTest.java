package com.dotcms.graphql.datafetcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.graphql.business.GraphqlQueryRunner;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Covers the scope {@link ContentletDataFetcher} applies to the query it runs.
 *
 * <p>The fetcher hands the caller's Lucene query to {@code ContentletAPI.searchPaginated} after
 * prepending a type clause, and the search layer adds {@code +live:true} for a caller without
 * back-end access. Nothing in that path names a variant, so the version axis a content item is
 * stored on is decided entirely by the caller's own query text.
 *
 * <p>These tests pin the scope a front-end caller must get: content versioned on a named variant
 * belongs to that variant's audience, so a query arriving without back-end access resolves on the
 * default variant whatever its text says. Both root fields are exercised because they take
 * different branches inside the fetcher — a typed collection gets a {@code +contentType:} clause
 * prepended, {@code search} gets nothing.
 */
public class ContentletDataFetcherTest extends IntegrationTestBase {

    private static User anonymous;
    private static Host site;
    private static ContentType contentType;
    private static Variant variant;
    private static Contentlet onDefaultVariant;
    private static Contentlet onNamedVariant;
    private static User limitedBackEndUser;

    /**
     * Builds one content type holding two live items: one on the default variant and one on a
     * named variant, both readable by the anonymous role so the only thing separating them in a
     * query is the variant they are stored on.
     */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        anonymous = APILocator.getUserAPI().getAnonymousUser();
        site = new SiteDataGen().nextPersisted();
        contentType = new ContentTypeDataGen().host(site).nextPersisted();
        variant = new VariantDataGen().nextPersisted();

        onDefaultVariant = publishedItem(null);
        onNamedVariant = publishedItem(variant);

        // The back-end role is granted too, so the control below can use a user who holds it
        // without holding the administrator role: an administrator never reaches the scope at all.
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Role backEndRole = TestUserUtils.getBackendRole();
        for (final Role role : List.of(APILocator.getRoleAPI().loadCMSAnonymousRole(), backEndRole)) {
            permissionAPI.save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                            contentType.getPermissionId(), role.getId(),
                            PermissionAPI.PERMISSION_READ, true),
                    contentType, APILocator.systemUser(), false);
            for (final Contentlet contentlet : List.of(onDefaultVariant, onNamedVariant)) {
                permissionAPI.save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                                contentlet.getPermissionId(), role.getId(),
                                PermissionAPI.PERMISSION_READ, true),
                        contentlet, APILocator.systemUser(), false);
            }
        }
        limitedBackEndUser = new UserDataGen().roles(backEndRole).nextPersisted();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        for (final Contentlet contentlet : List.of(onNamedVariant, onDefaultVariant)) {
            ContentletDataGen.remove(contentlet);
        }
        ContentTypeDataGen.remove(contentType);
        APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
        APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
    }

    /**
     * Given: a live item stored on a named variant, readable by the anonymous role.
     * When: an anonymous caller asks the typed collection for it by identifier.
     * Then: it is not returned, while the item on the default variant still is.
     */
    @Test
    public void test_typedCollection_anonymousCaller_resolvesOnTheDefaultVariantOnly()
            throws Exception {
        assertEquals("an item versioned on a named variant must not reach a caller without "
                        + "back-end access", List.of(),
                identifiersFromCollection(onNamedVariant.getIdentifier()));
        assertEquals("the item on the default variant must still be returned",
                List.of(onDefaultVariant.getIdentifier()),
                identifiersFromCollection(onDefaultVariant.getIdentifier()));
    }

    /**
     * Given: the same item on a named variant.
     * When: an anonymous caller reaches it through the untyped {@code search} root field, which
     * is the branch of the fetcher that prepends no clause of its own.
     * Then: it is not returned either.
     */
    @Test
    public void test_search_anonymousCaller_resolvesOnTheDefaultVariantOnly() throws Exception {
        assertEquals("the untyped search field must apply the same scope as a typed collection",
                List.of(), identifiersFromSearch("+identifier:" + onNamedVariant.getIdentifier()));
        assertEquals(List.of(onDefaultVariant.getIdentifier()),
                identifiersFromSearch("+identifier:" + onDefaultVariant.getIdentifier()));
    }

    /**
     * Given: a caller that names the variant in its own query text, which is the one way the
     * query reaching the index can carry a variant today.
     * When: that caller has no back-end access.
     * Then: naming the variant changes nothing — the scope is imposed, not defaulted.
     */
    @Test
    public void test_search_anonymousCallerNamingTheVariant_isStillScopedToTheDefaultVariant()
            throws Exception {
        assertEquals("a caller without back-end access must not be able to choose the variant "
                        + "its query resolves on", List.of(),
                identifiersFromSearch("+identifier:" + onNamedVariant.getIdentifier()
                        + " +variant:" + variant.name()));
    }

    /**
     * Given: the same two items.
     * When: a user who holds the back-end role, and only that role, runs the query that names the
     * variant.
     * Then: the item is returned, because the scope applies to callers without back-end access and
     * must not take the variant away from the tooling that edits it.
     *
     * <p>The caller is deliberately not an administrator. An administrator is answered before the
     * scope is reached at all, so a control using one would stay green even if the scope were
     * narrowed to exclude every back-end user who is not an administrator.
     */
    @Test
    public void test_limitedBackEndUser_stillReachesTheNamedVariant() throws Exception {
        assertFalse("fixture problem: the caller is an administrator, which is answered before the "
                + "scope is reached", limitedBackEndUser.isAdmin());
        assertTrue("fixture problem: the caller does not hold the back-end role",
                limitedBackEndUser.isBackendUser());

        final List<String> found = identifiersFrom(GraphqlQueryRunner.executeAndExpectSuccess(
                String.format("{ search(query: \"+identifier:%s +variant:%s\") { identifier } }",
                        onNamedVariant.getIdentifier(), variant.name()),
                limitedBackEndUser), "search");
        assertEquals("a back-end caller must still reach the named variant",
                List.of(onNamedVariant.getIdentifier()), found);
    }

    /**
     * Saves a live item of the shared content type, on the given variant when one is passed.
     *
     * @param onVariant the variant to version the item on, or null for the default variant
     * @return the published contentlet
     */
    private static Contentlet publishedItem(final Variant onVariant) {
        final ContentletDataGen dataGen = new ContentletDataGen(contentType.id()).host(site)
                .setPolicy(IndexPolicy.WAIT_FOR);
        Optional.ofNullable(onVariant).ifPresent(dataGen::variant);
        final Contentlet contentlet = dataGen.nextPersisted();
        ContentletDataGen.publish(contentlet);
        return contentlet;
    }

    /**
     * Runs the typed collection field of the shared content type as the anonymous user.
     *
     * @param identifier the identifier to ask for
     * @return the identifiers the query returned
     */
    private static List<String> identifiersFromCollection(final String identifier)
            throws Exception {
        final String field = contentType.variable() + "Collection";
        return identifiersFrom(GraphqlQueryRunner.executeAndExpectSuccess(
                String.format("{ %s(query: \"+identifier:%s\") { identifier } }", field,
                        identifier), anonymous), field);
    }

    /**
     * Runs the untyped search root field as the anonymous user.
     *
     * @param query the Lucene query to run
     * @return the identifiers the query returned
     */
    private static List<String> identifiersFromSearch(final String query) throws Exception {
        return identifiersFrom(GraphqlQueryRunner.executeAndExpectSuccess(
                String.format("{ search(query: \"%s\") { identifier } }", query), anonymous),
                "search");
    }

    /**
     * Reads the identifier of every row a root field returned.
     *
     * @param data the data map of an execution result
     * @param field the root field the rows sit under
     * @return the identifiers, in the order the query returned them
     */
    @SuppressWarnings("unchecked")
    private static List<String> identifiersFrom(final Map<String, Object> data,
            final String field) {
        final Object rows = data.get(field);
        assertTrue("the root field " + field + " returned nothing at all: " + data,
                rows instanceof List);
        return ((List<Map<String, Object>>) rows).stream()
                .map(row -> String.valueOf(row.get("identifier"))).collect(
                        java.util.stream.Collectors.toList());
    }
}
