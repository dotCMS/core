package com.dotcms.experiments.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
 * <p>{@code @JsonProperty(access = READ_ONLY)} is what makes the property <i>known but not
 * bound</i>, and {@link #payloadCarryingTheField_stillDeserializes()} is what established that it
 * holds when Jackson binds into the generated delegate rather than into the interface. Should that
 * ever stop being true, the answer is a {@code @JsonAppend} virtual property — <b>not</b> disabling
 * {@code FAIL_ON_UNKNOWN_PROPERTIES} on a shared mapper, which would weaken every other contract
 * that mapper serves.
 *
 * <p>These tests deliberately use {@link DotObjectMapperProvider#createDefaultMapper()} rather than
 * a bare {@code ObjectMapper}: the question is what the REST layer actually does, not what Jackson
 * does in general.
 */
class ExperimentCreatorNameTest {

    private static final String CREATOR_ID = "dotcms.org.1";
    private static final String CREATOR_NAME = "Admin User";

    /**
     * Deliberately different from {@link #CREATOR_ID}. If both ids were the same, every test here
     * would still pass with the accessor reading {@code lastModifiedBy()} instead of
     * {@code createdBy()} — the one mapping this feature exists to get right would be unasserted.
     */
    private static final String MODIFIER_ID = "dotcms.org.2";
    private static final String MODIFIER_NAME = "Other Person";

    /**
     * {@code lookBackWindowExpireTime} is set explicitly on purpose. Leaving it unset makes
     * Immutables evaluate its {@code @Value.Default}, which calls
     * {@code ConfigExperimentUtil.lookBackWindowDefaultExpireTime()} and through it
     * {@code APILocator.getExperimentsAPI()} — dragging in the CDI/OpenSearch wiring that no plain
     * unit-test JVM has, and which is mocked to a stub here anyway. Setting the value keeps the
     * test failing (and passing) for reasons that belong to #37304.
     */
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

    private static User adminUser() {
        return userNamed("Admin", "User");
    }

    /**
     * Runs the body with {@code APILocator.getUserAPI()} answering a stub that resolves
     * {@link #CREATOR_ID} and {@link #MODIFIER_ID} to <b>different</b> users, so an accessor
     * reading the wrong field is visible rather than silently equivalent.
     */
    private static void withResolvableCreator(final ThrowingRunnable body) throws Exception {
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.loadUserById(CREATOR_ID)).thenReturn(adminUser());
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
            assertNotEquals(MODIFIER_NAME, payload.get("createdByUserName").asText(),
                    "The name must come from createdBy, never from lastModifiedBy");
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
     * ExpectedResult: The field is present and carries {@code "unknown"} — the same label the
     *                 Content Drive folder view uses — so the column is never blank even at the
     *                 serialization layer (FR-003, FR-009).
     */
    @Test
    void serializedExperiment_withUnresolvableCreator_carriesUnknown() throws Exception {
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.loadUserById(anyString()))
                .thenThrow(new com.dotmarketing.business.NoSuchUserException("gone"));

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);

            final ObjectMapper mapper = DotObjectMapperProvider.createDefaultMapper();
            final JsonNode payload = mapper.readTree(mapper.writeValueAsString(anExperiment()));

            assertEquals("unknown", payload.get("createdByUserName").asText());
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
            assertEquals(MODIFIER_ID, payload.get("lastModifiedBy").asText(),
                    "lastModifiedBy is out of scope: still the raw id, and no name companion");
            assertFalse(payload.has("lastModifiedByUserName"),
                    "lastModifiedBy must not gain a name companion (FR-008)");

            for (final String computed : new String[]{"owner", "identifier", "permissionId",
                    "manifestInfo", "parentPermissionable", "acceptedPermissions"}) {
                assertFalse(payload.has(computed),
                        "The computed member '" + computed + "' must stay out of the payload");
            }
        });
    }

    /**
     * Method to test: {@code AbstractExperiment.createdByUserName()}
     * Given Scenario: Experiments are built and handled without ever being serialized — constructed
     *                 from the builder, rebuilt via {@code from(...)} the way
     *                 {@code addTargetingConditions} does on the find path, read for their owner,
     *                 and compared.
     * ExpectedResult: The user layer is never consulted. Only serializing the Experiment resolves
     *                 the name (FR-015).
     *
     * <p>This is the guard for the decision the whole feature rests on. The accessor is a plain
     * {@code default} method precisely so it stays inert on the paths that never serialize — the
     * database transformer behind every list row, the running-experiments cache fill on page
     * render, and the push-publish dependency walk. Turning it into a {@code @Value.Derived}
     * attribute would move the lookup into the constructor and break that silently: every other
     * test in this class would still pass, because they all serialize. This one would not.
     */
    @Test
    void buildingAnExperimentWithoutSerializing_neverResolvesTheCreator() throws Exception {
        final UserAPI userAPI = mock(UserAPI.class);

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);

            final Experiment experiment = anExperiment();
            final Experiment rebuilt = Experiment.builder().from(experiment).build();
            experiment.getOwner();
            experiment.equals(rebuilt);
            experiment.hashCode();
            experiment.toString();

            verifyNoInteractions(userAPI);

            DotObjectMapperProvider.createDefaultMapper().writeValueAsString(experiment);

            verify(userAPI).loadUserById(CREATOR_ID);
            verify(userAPI, never()).loadUserById(MODIFIER_ID);
        }
    }

    /**
     * Method to test: {@code AbstractExperiment.createdByUserName()}
     * Given Scenario: The same Experiment instance serialized twice, with the creator renaming
     *                 themselves in between.
     * ExpectedResult: The second payload reports the new name (FR-016).
     *
     * <p>The guard against memoization. A {@code @Value.Lazy} attribute would compute the name once
     * and keep it for the life of the instance — invisible on the REST path, where instances are
     * short-lived, but not on the running-experiments list cache, whose entries are long-lived and
     * shared. This test fails the moment the value starts being cached on the object.
     */
    @Test
    void renamingTheCreator_isReflectedOnTheNextSerialization() throws Exception {
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.loadUserById(CREATOR_ID))
                .thenReturn(adminUser(), userNamed("Renamed", "User"));

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);

            final Experiment experiment = anExperiment();
            final ObjectMapper mapper = DotObjectMapperProvider.createDefaultMapper();

            final JsonNode before = mapper.readTree(mapper.writeValueAsString(experiment));
            final JsonNode after = mapper.readTree(mapper.writeValueAsString(experiment));

            assertEquals(CREATOR_NAME, before.get("createdByUserName").asText());
            assertEquals("Renamed User", after.get("createdByUserName").asText(),
                    "The name must be resolved per serialization, never captured on the instance");
        }
    }

    private static User userNamed(final String first, final String last) {
        final User user = new User();
        user.setFirstName(first);
        user.setMiddleName("");
        user.setLastName(last);
        return user;
    }
}
