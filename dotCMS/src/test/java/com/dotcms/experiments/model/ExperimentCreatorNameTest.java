package com.dotcms.experiments.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Serialization tests for the {@code createdByUserName} field added to the Experiment payload by
 * #37304.
 *
 * <p><b>Why the round-trip test carries the most weight.</b> The Experiment model is an Immutables
 * value type, and its generated {@code Experiment.Json} delegate holds <i>settable</i> attributes
 * only — it has no field for a computed one, and it is not annotated
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)}. The REST mapper leaves
 * {@code FAIL_ON_UNKNOWN_PROPERTIES} at Jackson's default, which is <i>enabled</i>. So a property
 * that serializes but cannot be bound is an unknown property on the way back in, and adding one
 * introduces an asymmetry the current payload does not have: today every computed member is
 * {@code @JsonIgnore}d, so an Experiment round-trips cleanly.
 *
 * <p>{@code @JsonProperty(access = READ_ONLY)} is the mechanism that is supposed to make the
 * property <i>known but not bound</i>. {@link #payloadCarryingTheField_stillDeserializes()} is what
 * decides whether that holds when Jackson binds into the generated delegate rather than into the
 * interface. If it cannot be made green, the agreed fallback is a {@code @JsonAppend} virtual
 * property — <b>not</b> disabling {@code FAIL_ON_UNKNOWN_PROPERTIES} on a shared mapper, which
 * would weaken every other contract that mapper serves.
 *
 * <p>These tests deliberately use {@link DotObjectMapperProvider#createDefaultMapper()} rather than
 * a bare {@code ObjectMapper}: the question is what the REST layer actually does, not what Jackson
 * does in general.
 */
class ExperimentCreatorNameTest {

    private static final String CREATOR_ID = "dotcms.org.1";
    private static final String CREATOR_NAME = "Admin User";

    /**
     * {@code lookBackWindowExpireTime} is set explicitly on purpose. Leaving it unset makes
     * Immutables evaluate its {@code @Value.Default}, which reads {@code ConfigExperimentUtil} and
     * therefore {@code Config} — unavailable in a plain unit-test JVM, where it fails with
     * {@code ExceptionInInitializerError} long before this feature gets a say. Setting the value
     * keeps the test failing (and passing) for reasons that belong to #37304.
     */
    private static Experiment anExperiment() {
        return Experiment.builder()
                .name("Homepage CTA test")
                .pageId("2d8b8b1e-0000-0000-0000-000000000001")
                .createdBy(CREATOR_ID)
                .lastModifiedBy(CREATOR_ID)
                .id("0e8b8b1e-0000-0000-0000-000000000002")
                .lookBackWindowExpireTime(1_800_000L)
                .build();
    }

    private static User adminUser() {
        final User user = new User();
        user.setFirstName("Admin");
        user.setMiddleName("");
        user.setLastName("User");
        return user;
    }

    /**
     * Runs the body with {@code APILocator.getUserAPI()} answering with a stub that resolves
     * {@link #CREATOR_ID} to {@link #adminUser()}.
     */
    private static void withResolvableCreator(final ThrowingRunnable body) throws Exception {
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.loadUserById(anyString())).thenReturn(adminUser());

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
     * Method to test: {@code AbstractExperiment.createdByUserName()}
     * Given Scenario: An Experiment whose creator resolves to a named user, serialized with the
     *                 REST mapper.
     * ExpectedResult: The payload carries {@code createdByUserName} with the creator's full name
     *                 (FR-001, FR-002).
     */
    @Test
    void serializedExperiment_carriesCreatorName() throws Exception {
        withResolvableCreator(() -> {
            final ObjectMapper mapper = DotObjectMapperProvider.createDefaultMapper();

            final JsonNode payload = mapper.readTree(mapper.writeValueAsString(anExperiment()));

            assertTrue(payload.has("createdByUserName"),
                    "The payload should carry createdByUserName");
            assertEquals(CREATOR_NAME, payload.get("createdByUserName").asText());
        });
    }

    /**
     * Method to test: {@code AbstractExperiment.createdByUserName()}
     * Given Scenario: The same serialized payload fed straight back to the same mapper.
     * ExpectedResult: It deserializes into an Experiment without raising. This is the gating test
     *                 described in the class Javadoc: a serialize-only property must not make a
     *                 round-tripped payload unreadable (FR-023).
     */
    @Test
    void payloadCarryingTheField_stillDeserializes() throws Exception {
        withResolvableCreator(() -> {
            final ObjectMapper mapper = DotObjectMapperProvider.createDefaultMapper();
            final String json = mapper.writeValueAsString(anExperiment());

            assertTrue(json.contains("createdByUserName"),
                    "Precondition: the payload under test must actually carry the new field");

            final Experiment parsed = assertDoesNotThrow(
                    () -> mapper.readValue(json, Experiment.class),
                    "A payload carrying createdByUserName must still deserialize");

            assertEquals(CREATOR_ID, parsed.createdBy(),
                    "The bound createdBy should survive the round trip unchanged");
        });
    }

    /**
     * Method to test: {@code AbstractExperiment.createdByUserName()}
     * Given Scenario: An Experiment whose creator cannot be resolved, serialized with the REST
     *                 mapper.
     * ExpectedResult: The field is present and carries the raw creator ID, so the column is never
     *                 blank even at the serialization layer (FR-003, FR-009).
     */
    @Test
    void serializedExperiment_withUnresolvableCreator_carriesRawId() throws Exception {
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.loadUserById(anyString()))
                .thenThrow(new com.dotmarketing.business.NoSuchUserException("gone"));

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);

            final ObjectMapper mapper = DotObjectMapperProvider.createDefaultMapper();
            final JsonNode payload = mapper.readTree(mapper.writeValueAsString(anExperiment()));

            assertEquals(CREATOR_ID, payload.get("createdByUserName").asText());
        }
    }

    /**
     * Method to test: {@code AbstractExperiment.createdBy()} / {@code getOwner()}
     * Given Scenario: The same serialization, inspected for what must NOT have changed.
     * ExpectedResult: {@code createdBy} keeps its key and its user-ID value, {@code getOwner()}
     *                 still resolves from it, and the computed permission members stay out of the
     *                 payload. This is the additive-change guarantee (FR-005, FR-006, FR-007).
     */
    @Test
    void addingTheField_leavesTheExistingContractAlone() throws Exception {
        withResolvableCreator(() -> {
            final Experiment experiment = anExperiment();
            final ObjectMapper mapper = DotObjectMapperProvider.createDefaultMapper();

            final JsonNode payload = mapper.readTree(mapper.writeValueAsString(experiment));

            assertEquals(CREATOR_ID, payload.get("createdBy").asText(),
                    "createdBy must still be the user ID");
            assertEquals(CREATOR_ID, experiment.getOwner(),
                    "getOwner() must still resolve from createdBy, not from the new field");
            assertEquals(CREATOR_ID, payload.get("lastModifiedBy").asText(),
                    "lastModifiedBy is out of scope and must be untouched");

            for (final String computed : new String[]{"owner", "identifier", "permissionId",
                    "manifestInfo", "parentPermissionable", "acceptedPermissions"}) {
                assertFalse(payload.has(computed),
                        "The computed member '" + computed + "' must stay out of the payload");
            }
        });
    }
}
