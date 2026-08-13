package com.dotcms.ai.client.langchain4j;

import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link LangChain4jModelFactory#listProviderMetadata()} and the per-strategy
 * {@code supportedCapabilities()}/{@code configFields()} declarations it aggregates.
 *
 * <p>The declared fields/capabilities restate knowledge already enforced imperatively in each
 * strategy's {@code validate()}/builder logic, so nothing in the compiler keeps the two in sync.
 * The {@code assertMissingRequiredFieldBreaksChatBuild} checks below cross-check that by
 * construction: for a field the metadata declares required, building without it must actually fail.
 */
public class ProviderMetadataTest {

    @Test
    public void test_listProviderMetadata_returnsAllSevenProviders() {
        final List<ProviderMetadata> metadata = LangChain4jModelFactory.listProviderMetadata();
        final Set<String> providers = metadata.stream()
                .map(ProviderMetadata::provider)
                .collect(Collectors.toSet());
        assertEquals(7, metadata.size());
        assertEquals(Set.of("openai", "azure_openai", "bedrock", "vertex_ai", "anthropic", "openrouter", "google_ai"),
                providers);
    }

    @Test
    public void test_listProviderMetadata_capabilitiesMatchKnownSupport() {
        final Map<String, ProviderMetadata> byProvider = indexByProvider();
        assertEquals(Set.of(Capability.CHAT, Capability.EMBEDDINGS, Capability.IMAGE),
                byProvider.get("openai").supportedCapabilities());
        assertEquals(Set.of(Capability.CHAT, Capability.EMBEDDINGS, Capability.IMAGE),
                byProvider.get("azure_openai").supportedCapabilities());
        assertEquals(Set.of(Capability.CHAT, Capability.EMBEDDINGS),
                byProvider.get("bedrock").supportedCapabilities());
        assertEquals(Set.of(Capability.CHAT), byProvider.get("vertex_ai").supportedCapabilities());
        assertEquals(Set.of(Capability.CHAT), byProvider.get("anthropic").supportedCapabilities());
        assertEquals(Set.of(Capability.CHAT, Capability.EMBEDDINGS),
                byProvider.get("openrouter").supportedCapabilities());
        assertEquals(Set.of(Capability.CHAT, Capability.EMBEDDINGS, Capability.IMAGE),
                byProvider.get("google_ai").supportedCapabilities());
    }

    @Test
    public void test_listProviderMetadata_fieldsKeyedOnlyBySupportedCapabilities() {
        for (final ProviderMetadata metadata : LangChain4jModelFactory.listProviderMetadata()) {
            assertEquals(metadata.supportedCapabilities(), metadata.fields().keySet());
            for (final Capability capability : metadata.supportedCapabilities()) {
                assertTrue("provider " + metadata.provider() + " declares no fields for " + capability,
                        !metadata.fields().get(capability).isEmpty());
            }
        }
    }

    @Test
    public void test_configFields_calledForUnsupportedCapability_throws() {
        assertThrows(UnsupportedOperationException.class, () -> strategyFor("bedrock").configFields(Capability.IMAGE));
        assertThrows(UnsupportedOperationException.class, () -> strategyFor("vertex_ai").configFields(Capability.EMBEDDINGS));
        assertThrows(UnsupportedOperationException.class, () -> strategyFor("vertex_ai").configFields(Capability.IMAGE));
        assertThrows(UnsupportedOperationException.class, () -> strategyFor("anthropic").configFields(Capability.EMBEDDINGS));
        assertThrows(UnsupportedOperationException.class, () -> strategyFor("anthropic").configFields(Capability.IMAGE));
        assertThrows(UnsupportedOperationException.class, () -> strategyFor("openrouter").configFields(Capability.IMAGE));
    }

    // ── Declared-required-field cross-checks (chat capability, common to all 7 providers) ──────

    @Test
    public void test_openai_chatRequiredFields_missingApiKey_throws() {
        assertMissingRequiredFieldBreaksChatBuild("openai",
                ImmutableProviderConfig.builder().provider("openai").model("gpt-4o-mini"), "apiKey");
    }

    @Test
    public void test_openai_chatRequiredFields_missingModel_throws() {
        assertMissingRequiredFieldBreaksChatBuild("openai",
                ImmutableProviderConfig.builder().provider("openai").apiKey("test-key"), "model");
    }

    @Test
    public void test_azureOpenAi_chatRequiredFields_missingApiKey_throws() {
        assertMissingRequiredFieldBreaksChatBuild("azure_openai",
                ImmutableProviderConfig.builder().provider("azure_openai").model("gpt-4o")
                        .endpoint("https://my-company.openai.azure.com/"),
                "apiKey");
    }

    @Test
    public void test_azureOpenAi_chatRequiredFields_missingEndpoint_throws() {
        assertMissingRequiredFieldBreaksChatBuild("azure_openai",
                ImmutableProviderConfig.builder().provider("azure_openai").model("gpt-4o").apiKey("test-key"),
                "endpoint");
    }

    @Test
    public void test_azureOpenAi_modelAndDeploymentName_declaredOptionalWithCrossHints() {
        final ProviderMetadata metadata = indexByProvider().get("azure_openai");
        final List<ProviderField> chatFields = metadata.fields().get(Capability.CHAT);
        final ProviderField model = fieldNamed(chatFields, "model");
        final ProviderField deploymentName = fieldNamed(chatFields, "deploymentName");
        assertTrue(!model.required());
        assertTrue(!deploymentName.required());
        assertTrue(model.hint().contains("deploymentName"));
        assertTrue(deploymentName.hint().contains("model"));
    }

    @Test
    public void test_bedrock_chatRequiredFields_missingRegion_throws() {
        assertMissingRequiredFieldBreaksChatBuild("bedrock",
                ImmutableProviderConfig.builder().provider("bedrock").model("anthropic.claude-3-5-sonnet-20241022-v2:0"),
                "region");
    }

    @Test
    public void test_bedrock_chatRequiredFields_missingModel_throws() {
        assertMissingRequiredFieldBreaksChatBuild("bedrock",
                ImmutableProviderConfig.builder().provider("bedrock").region("us-east-1"), "model");
    }

    @Test
    public void test_vertexAi_chatRequiredFields_missingProjectId_throws() {
        assertMissingRequiredFieldBreaksChatBuild("vertex_ai",
                ImmutableProviderConfig.builder().provider("vertex_ai").model("gemini-1.5-pro").location("us-central1"),
                "projectId");
    }

    @Test
    public void test_vertexAi_chatRequiredFields_missingLocation_throws() {
        assertMissingRequiredFieldBreaksChatBuild("vertex_ai",
                ImmutableProviderConfig.builder().provider("vertex_ai").model("gemini-1.5-pro").projectId("my-gcp-project"),
                "location");
    }

    @Test
    public void test_anthropic_chatRequiredFields_missingApiKey_throws() {
        assertMissingRequiredFieldBreaksChatBuild("anthropic",
                ImmutableProviderConfig.builder().provider("anthropic").model("claude-sonnet-4-6"), "apiKey");
    }

    @Test
    public void test_anthropic_chatRequiredFields_missingModel_throws() {
        assertMissingRequiredFieldBreaksChatBuild("anthropic",
                ImmutableProviderConfig.builder().provider("anthropic").apiKey("test-key"), "model");
    }

    @Test
    public void test_openRouter_chatRequiredFields_missingModel_throws() {
        assertMissingRequiredFieldBreaksChatBuild("openrouter",
                ImmutableProviderConfig.builder().provider("openrouter").apiKey("test-key"), "model");
    }

    @Test
    public void test_openRouter_chatRequiredFields_missingApiKey_throws() {
        assertMissingRequiredFieldBreaksChatBuild("openrouter",
                ImmutableProviderConfig.builder().provider("openrouter").model("openai/gpt-4o"), "apiKey");
    }

    @Test
    public void test_googleAi_chatRequiredFields_missingApiKey_throws() {
        assertMissingRequiredFieldBreaksChatBuild("google_ai",
                ImmutableProviderConfig.builder().provider("google_ai").model("gemini-2.0-flash"), "apiKey");
    }

    @Test
    public void test_googleAi_chatRequiredFields_missingModel_throws() {
        assertMissingRequiredFieldBreaksChatBuild("google_ai",
                ImmutableProviderConfig.builder().provider("google_ai").apiKey("test-key"), "model");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Asserts that {@code provider}'s metadata declares {@code missingFieldName} as required for
     * CHAT, and that the given (deliberately incomplete) builder — missing exactly that field —
     * indeed fails to build a chat model. Ties the declarative metadata to the imperative
     * validation every strategy already performs.
     */
    private static void assertMissingRequiredFieldBreaksChatBuild(final String provider,
                                                                  final ImmutableProviderConfig.Builder incompleteBuilder,
                                                                  final String missingFieldName) {
        final ProviderMetadata metadata = indexByProvider().get(provider);
        final ProviderField field = fieldNamed(metadata.fields().get(Capability.CHAT), missingFieldName);
        assertTrue(provider + "." + missingFieldName + " must be declared required for CHAT in ProviderMetadata",
                field.required());
        assertThrows(IllegalArgumentException.class,
                () -> LangChain4jModelFactory.buildChatModel(incompleteBuilder.build()));
    }

    private static ProviderField fieldNamed(final List<ProviderField> fields, final String name) {
        return fields.stream()
                .filter(f -> f.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no field named '" + name + "' in " + fields));
    }

    private static ModelProviderStrategy strategyFor(final String provider) {
        return LangChain4jModelFactory.STRATEGIES.stream()
                .filter(s -> s.providerName().equals(provider))
                .findFirst()
                .orElseThrow();
    }

    private static Map<String, ProviderMetadata> indexByProvider() {
        return LangChain4jModelFactory.listProviderMetadata().stream()
                .collect(Collectors.toMap(ProviderMetadata::provider, m -> m));
    }

}
