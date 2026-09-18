package com.dotcms.rest.api.v1.experiments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Serialization tests for {@link ExperimentView}, the REST-layer wrapper that publishes
 * {@code createdByUserName} beside an Experiment (#37304).
 *
 * <p><b>Why the field lives here and not on the model.</b> An earlier revision of this work put the
 * accessor on {@code AbstractExperiment} itself. That made every serialization of an Experiment
 * resolve a user — including push-publish bundling and starter export, which are not REST responses
 * at all, and which are read back by a receiver that rejects unknown properties and rolls back the
 * whole bundle. Keeping the derivation in the view means the model stays a plain data object and
 * only the endpoint response is affected, which is what review asked for.
 *
 * <p>The wire shape must not change as a result: {@code createdByUserName} is a sibling of the
 * Experiment's own fields, not a nested object. {@link #view_flattensTheExperimentAlongsideTheName()}
 * is what pins that.
 */
class ExperimentViewTest {

    private static final String CREATOR_ID = "dotcms.org.1";
    private static final String CREATOR_NAME = "Admin User";
    private static final String MODIFIER_ID = "dotcms.org.2";

    private static Experiment anExperiment() {
        return Experiment.builder()
                .name("Homepage CTA test")
                .pageId("2d8b8b1e-0000-0000-0000-000000000001")
                .createdBy(CREATOR_ID)
                .lastModifiedBy(MODIFIER_ID)
                .id("0e8b8b1e-0000-0000-0000-000000000002")
                .lookBackWindowExpireTime(1_800_000L)
                .build();
    }

    private static User userNamed(final String first, final String last) {
        final User user = new User();
        user.setFirstName(first);
        user.setMiddleName("");
        user.setLastName(last);
        return user;
    }

    private static void withResolvableCreator(final ThrowingRunnable body) throws Exception {
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.loadUserById(CREATOR_ID)).thenReturn(userNamed("Admin", "User"));
        when(userAPI.loadUserById(MODIFIER_ID)).thenReturn(userNamed("Other", "Person"));

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);
            body.run();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    /**
     * Method to test: {@link ExperimentView}
     * Given Scenario: A view wrapping an Experiment whose creator resolves to a named user.
     * ExpectedResult: The payload carries the Experiment's own fields at the top level AND
     *                 {@code createdByUserName} beside them — the same shape the model-level
     *                 revision produced, so the API contract does not move.
     */
    @Test
    void view_flattensTheExperimentAlongsideTheName() throws Exception {
        withResolvableCreator(() -> {
            final ObjectMapper mapper = DotObjectMapperProvider.createDefaultMapper();

            final JsonNode payload =
                    mapper.readTree(mapper.writeValueAsString(ExperimentView.of(anExperiment())));

            assertEquals(CREATOR_NAME, payload.get("createdByUserName").asText());
            assertEquals(CREATOR_ID, payload.get("createdBy").asText(),
                    "createdBy must still be a top-level field, not nested under the experiment");
            assertEquals("Homepage CTA test", payload.get("name").asText(),
                    "The Experiment's own fields must stay at the top level");
            assertFalse(payload.has("experiment"),
                    "The Experiment must be unwrapped, not nested under a property");
        });
    }

    /**
     * Method to test: {@link ExperimentView}
     * Given Scenario: The same Experiment serialized WITHOUT the view, as the push-publish bundler
     *                 and starter export do.
     * ExpectedResult: No {@code createdByUserName}, and the user layer is never consulted. This is
     *                 the guarantee that moving the derivation to the REST layer buys, and it needs
     *                 no mix-in in BundlerUtil to hold.
     */
    @Test
    void bareExperiment_carriesNoNameAndCostsNoLookup() throws Exception {
        final UserAPI userAPI = mock(UserAPI.class);

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            BundlerUtil.objectToJSON(anExperiment(), out);
            final String bundled = out.toString(StandardCharsets.UTF_8);

            assertFalse(bundled.contains("createdByUserName"),
                    "A bare Experiment must not carry the derived name");
            assertTrue(bundled.contains("createdBy"),
                    "Precondition: the bundled shape should still carry createdBy");
            verifyNoInteractions(userAPI);
        }
    }

    /**
     * Method to test: {@link ExperimentView}
     * Given Scenario: The view is built but never serialized.
     * ExpectedResult: No user lookup. The name is resolved when the response is written, so the
     *                 value cannot go stale and building a view costs nothing.
     */
    @Test
    void buildingTheViewWithoutSerializing_costsNoLookup() throws Exception {
        final UserAPI userAPI = mock(UserAPI.class);

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);

            ExperimentView.of(anExperiment());

            verifyNoInteractions(userAPI);
        }
    }

    /**
     * Method to test: {@link ExperimentView}
     * Given Scenario: An Experiment whose creator cannot be resolved.
     * ExpectedResult: {@code "unknown"} — the column is never blank, and the response still
     *                 serializes (FR-003, FR-009).
     */
    @Test
    void view_withUnresolvableCreator_reportsUnknown() throws Exception {
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.loadUserById(anyString()))
                .thenThrow(new com.dotmarketing.business.NoSuchUserException("gone"));

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);

            final ObjectMapper mapper = DotObjectMapperProvider.createDefaultMapper();
            final JsonNode payload =
                    mapper.readTree(mapper.writeValueAsString(ExperimentView.of(anExperiment())));

            assertEquals("unknown", payload.get("createdByUserName").asText());
        }
    }

    /**
     * Method to test: {@link ExperimentView}
     * Given Scenario: The same view serialized twice, with the creator renaming in between.
     * ExpectedResult: The second payload reports the new name. The value is resolved per
     *                 serialization, so it cannot go stale (FR-016).
     */
    @Test
    void renamingTheCreator_isReflectedOnTheNextSerialization() throws Exception {
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.loadUserById(CREATOR_ID))
                .thenReturn(userNamed("Admin", "User"), userNamed("Renamed", "User"));

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);

            final ObjectMapper mapper = DotObjectMapperProvider.createDefaultMapper();
            final ExperimentView view = ExperimentView.of(anExperiment());

            final JsonNode before = mapper.readTree(mapper.writeValueAsString(view));
            final JsonNode after = mapper.readTree(mapper.writeValueAsString(view));

            assertEquals(CREATOR_NAME, before.get("createdByUserName").asText());
            assertEquals("Renamed User", after.get("createdByUserName").asText(),
                    "The name must be resolved per serialization, never captured on the view");
        }
    }

    /**
     * Method to test: the Experiment model itself.
     * Given Scenario: A bare Experiment round-tripped through the REST mapper.
     * ExpectedResult: It parses back cleanly and keeps createdBy, getOwner() and lastModifiedBy.
     *                 This is the guard that the model stayed a plain data object: with the derived
     *                 field on the model, a serialize-only property made the payload unreadable by
     *                 any strict reader, which is the whole reason the field moved to this view.
     */
    @Test
    void bareExperiment_stillRoundTripsAndKeepsItsContract() throws Exception {
        // The APILocator mock is scaffolding, not subject: AbstractExperiment's pre-existing
        // @Value.Derived getParentPermissionable() calls the ContentletAPI at build time, so an
        // Experiment cannot be constructed in a bare unit-test JVM. That it fires on build is
        // incidentally the very behaviour this feature avoided by not being a derived attribute.
        final UserAPI userAPI = mock(UserAPI.class);

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);

            final ObjectMapper mapper = DotObjectMapperProvider.createDefaultMapper();
            final Experiment experiment = anExperiment();

            final String json = mapper.writeValueAsString(experiment);
            final Experiment parsed = mapper.readValue(json, Experiment.class);

            assertFalse(json.contains("createdByUserName"),
                    "The model must not carry the derived field");
            assertEquals(CREATOR_ID, parsed.createdBy());
            assertEquals(CREATOR_ID, parsed.getOwner(),
                    "getOwner() must still resolve from createdBy");
            assertEquals(MODIFIER_ID, parsed.lastModifiedBy());
            verifyNoInteractions(userAPI);
        }
    }

    /**
     * Method to test: {@link ExperimentView}
     * Given Scenario: An Experiment whose creator resolves, serialized through the view.
     * ExpectedResult: The name comes from {@code createdBy} and never from {@code lastModifiedBy},
     *                 and the modifier is never looked up.
     */
    @Test
    void view_resolvesTheCreatorAndNotTheModifier() throws Exception {
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.loadUserById(anyString())).thenReturn(userNamed("Admin", "User"));

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);

            DotObjectMapperProvider.createDefaultMapper()
                    .writeValueAsString(ExperimentView.of(anExperiment()));

            verify(userAPI).loadUserById(CREATOR_ID);
            verify(userAPI, never()).loadUserById(MODIFIER_ID);
        }
    }
}
