package com.dotcms.auth.providers.oauth.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotmarketing.exception.DotRuntimeException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OAuthGroupsFetcher} — the shared post-auth groups fetch used by
 * {@link OIDCProvider} and {@link GenericOAuth2Provider} for IdPs that cannot emit groups
 * in token claims (Google Workspace, GitHub). Pure logic + injected HTTP; no sockets.
 */
class OAuthGroupsFetcherTest {

    private static Map<String, Object> claims(final String email, final String sub) {
        final Map<String, Object> m = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (email != null) {
            m.put("email", email);
        }
        if (sub != null) {
            m.put("sub", sub);
        }
        return m;
    }

    // ---------- URL placeholder substitution ----------

    @Test
    void resolveUrl_substitutesAndEncodesEmailAndSub() {
        final String url = OAuthGroupsFetcher.resolveUrl(
                "https://cloudidentity.googleapis.com/v1/groups/-/memberships:searchDirectGroups"
                        + "?query=member_key_id=='{email}'&sub={sub}",
                claims("jane.doe@example.com", "abc 123"));
        assertTrue(url.contains("member_key_id=%27jane.doe%40example.com%27")
                        || url.contains("member_key_id=='jane.doe%40example.com'"),
                "email not substituted/encoded: " + url);
        assertTrue(url.contains("sub=abc+123") || url.contains("sub=abc%20123"),
                "sub not substituted/encoded: " + url);
        assertTrue(!url.contains("{email}") && !url.contains("{sub}"), "placeholders left behind: " + url);
    }

    @Test
    void resolveUrl_leavesUrlWithoutPlaceholdersUntouched() {
        final String plain = "https://idp.example.com/groups";
        assertEquals(plain, OAuthGroupsFetcher.resolveUrl(plain, claims(null, null)));
    }

    @Test
    void resolveUrl_throwsWhenPlaceholderClaimMissing() {
        assertThrows(DotRuntimeException.class, () -> OAuthGroupsFetcher.resolveUrl(
                "https://idp.example.com/groups?userKey={email}", claims(null, "sub-1")));
        assertThrows(DotRuntimeException.class, () -> OAuthGroupsFetcher.resolveUrl(
                "https://idp.example.com/groups?userKey={email}", null));
    }

    // ---------- dot-path extraction ----------

    @Test
    void extract_googleCloudIdentityShape() {
        final String body = "{\"memberships\":["
                + "{\"groupKey\":{\"id\":\"team-a@example.com\"},\"group\":\"groups/1\"},"
                + "{\"groupKey\":{\"id\":\"team-b@example.com\"},\"group\":\"groups/2\"}]}";
        final Collection<String> groups = fetchSinglePage(body, "memberships[].groupKey.id");
        assertEquals(List.of("team-a@example.com", "team-b@example.com"), new ArrayList<>(groups));
    }

    @Test
    void extract_googleDirectoryShape() {
        final String body = "{\"kind\":\"admin#directory#groups\",\"groups\":["
                + "{\"email\":\"eng@example.com\",\"name\":\"Engineering\"},"
                + "{\"email\":\"sales@example.com\",\"name\":\"Sales\"}]}";
        final Collection<String> groups = fetchSinglePage(body, "groups[].email");
        assertEquals(List.of("eng@example.com", "sales@example.com"), new ArrayList<>(groups));
    }

    @Test
    void extract_rootArrayOfObjects_githubTeamsShape() {
        final String body = "[{\"slug\":\"platform\",\"id\":1},{\"slug\":\"security\",\"id\":2}]";
        final Collection<String> groups = fetchSinglePage(body, "[].slug");
        assertEquals(List.of("platform", "security"), new ArrayList<>(groups));
    }

    @Test
    void extract_skipsElementsMissingTheField() {
        final String body = "{\"memberships\":[{\"groupKey\":{\"id\":\"a@x.com\"}},{\"other\":true}]}";
        assertEquals(List.of("a@x.com"), new ArrayList<>(fetchSinglePage(body, "memberships[].groupKey.id")));
    }

    @Test
    void extract_missingArrayKeyYieldsEmpty() {
        // Google omits "memberships" entirely for a user with no groups — not an error.
        assertTrue(fetchSinglePage("{}", "memberships[].groupKey.id").isEmpty());
    }

    @Test
    void extract_pathNotResolvingToArrayThrows() {
        assertThrows(DotRuntimeException.class,
                () -> fetchSinglePage("{\"memberships\":\"oops\"}", "memberships[].groupKey.id"));
    }

    @Test
    void extract_invalidPathWithoutArrayMarkerThrows() {
        assertThrows(DotRuntimeException.class,
                () -> fetchSinglePage("{\"groups\":[\"a\"]}", "groups.email"));
    }

    // ---------- legacy shapes (no responsePath) keep working unchanged ----------

    @Test
    void legacy_rootStringArray() {
        assertEquals(List.of("admins", "editors"),
                new ArrayList<>(fetchSinglePage("[\"admins\",\"editors\"]", null)));
    }

    @Test
    void legacy_groupsKeyObject() {
        assertEquals(List.of("admins"),
                new ArrayList<>(fetchSinglePage("{\"groups\":[\"admins\"]}", null)));
    }

    // ---------- pagination ----------

    @Test
    void fetch_followsNextPageTokenAndAccumulates() {
        final List<String> requestedUrls = new ArrayList<>();
        final Function<String, String> http = url -> {
            requestedUrls.add(url);
            if (!url.contains("pageToken=")) {
                return "{\"memberships\":[{\"groupKey\":{\"id\":\"page1@x.com\"}}],\"nextPageToken\":\"tok2\"}";
            }
            return "{\"memberships\":[{\"groupKey\":{\"id\":\"page2@x.com\"}}]}";
        };
        final Collection<String> groups = OAuthGroupsFetcher.fetch(
                "https://idp.example.com/groups?userKey={email}", "memberships[].groupKey.id",
                claims("u@x.com", null), "OIDC", http);
        assertEquals(List.of("page1@x.com", "page2@x.com"), new ArrayList<>(groups));
        assertEquals(2, requestedUrls.size());
        assertTrue(requestedUrls.get(1).contains("pageToken=tok2"), "second request missing pageToken: " + requestedUrls.get(1));
        // Placeholder substitution must apply to every page request.
        assertTrue(requestedUrls.get(1).contains("userKey=u%40x.com"));
    }

    @Test
    void fetch_throwsAtPageCapWhenTokenNeverEnds() {
        final AtomicInteger calls = new AtomicInteger();
        final Function<String, String> http = url -> {
            calls.incrementAndGet();
            return "{\"memberships\":[{\"groupKey\":{\"id\":\"g" + calls.get() + "@x.com\"}}],\"nextPageToken\":\"t\"}";
        };
        // A partial list would let the role rebuild silently strip the missing pages' roles,
        // so exceeding the cap must abort the login rather than return what it has.
        assertThrows(DotRuntimeException.class, () -> OAuthGroupsFetcher.fetch(
                "https://idp.example.com/groups", "memberships[].groupKey.id",
                claims("u@x.com", null), "OIDC", http));
        assertEquals(OAuthGroupsFetcher.maxPages(), calls.get(), "must stop requesting at the page cap");
    }

    @Test
    void fetch_deduplicatesAcrossPages() {
        final Function<String, String> http = url -> url.contains("pageToken=")
                ? "{\"groups\":[\"dup\"]}"
                : "{\"groups\":[\"dup\"],\"nextPageToken\":\"t2\"}";
        assertEquals(List.of("dup"), new ArrayList<>(OAuthGroupsFetcher.fetch(
                "https://idp.example.com/groups", null, claims(null, null), "OIDC", http)));
    }

    @Test
    void fetch_propagatesHttpFailures() {
        final Function<String, String> http = url -> {
            throw new DotRuntimeException("endpoint returned HTTP 500");
        };
        // Endpoint down must throw, never return empty — the caller aborts login so an IdP
        // outage cannot silently strip the user's roles during the role rebuild.
        assertThrows(DotRuntimeException.class, () -> OAuthGroupsFetcher.fetch(
                "https://idp.example.com/groups", null, claims(null, null), "OIDC", http));
    }

    @Test
    void fetch_propagatesMalformedJson() {
        assertThrows(DotRuntimeException.class, () -> OAuthGroupsFetcher.fetch(
                "https://idp.example.com/groups", null, claims(null, null), "OIDC", url -> "not-json"));
    }

    private static Collection<String> fetchSinglePage(final String body, final String responsePath) {
        return OAuthGroupsFetcher.fetch("https://idp.example.com/groups", responsePath,
                claims("user@example.com", "sub-1"), "OIDC", url -> body);
    }
}
