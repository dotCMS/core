package com.dotcms.ai.client.langchain4j;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Deserializable POJO for a single provider section in the {@code providerConfig} JSON.
 *
 * <p>Each section (chat, embeddings, image) in the JSON maps to one instance of this class.
 * Unknown fields are ignored to allow forward-compatible configuration.
 *
 * <p>Common fields (all providers):
 * <ul>
 *   <li>{@code provider} – identifier: {@code openai}, {@code azure_openai}, {@code bedrock}, {@code vertex_ai}</li>
 *   <li>{@code model} – model name or ID</li>
 *   <li>{@code maxTokens} – max output tokens</li>
 *   <li>{@code temperature} – sampling temperature (0.0–2.0)</li>
 *   <li>{@code maxRetries} – retry attempts on transient failures</li>
 *   <li>{@code timeout} – request timeout in seconds</li>
 * </ul>
 *
 * <p>OpenAI / Azure OpenAI:
 * <ul>
 *   <li>{@code apiKey}</li>
 *   <li>{@code size} – image size, e.g. {@code 1024x1024} (image only)</li>
 *   <li>{@code endpoint} – Azure base URL</li>
 *   <li>{@code deploymentName} – Azure deployment name</li>
 *   <li>{@code apiVersion} – Azure API version, e.g. {@code 2024-02-01}</li>
 * </ul>
 *
 * <p>AWS Bedrock:
 * <ul>
 *   <li>{@code region}</li>
 *   <li>{@code accessKeyId}</li>
 *   <li>{@code secretAccessKey}</li>
 * </ul>
 *
 * <p>Google Vertex AI:
 * <ul>
 *   <li>{@code projectId}</li>
 *   <li>{@code location}</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderConfig {

    private String provider;
    private String model;
    private Integer maxTokens;
    private Integer maxCompletionTokens;
    private Double temperature;
    private Integer maxRetries;
    private Integer timeout;

    // OpenAI / Azure OpenAI
    private String apiKey;
    private String size;
    private String endpoint;
    private String deploymentName;
    private String apiVersion;

    // AWS Bedrock
    private String region;
    private String accessKeyId;
    private String secretAccessKey;

    // Google Vertex AI
    private String projectId;
    private String location;

    public String getProvider() {
        return provider;
    }

    public void setProvider(final String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(final String model) {
        this.model = model;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(final Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Integer getMaxCompletionTokens() {
        return maxCompletionTokens;
    }

    public void setMaxCompletionTokens(final Integer maxCompletionTokens) {
        this.maxCompletionTokens = maxCompletionTokens;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(final Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(final Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(final Integer timeout) {
        this.timeout = timeout;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSize() {
        return size;
    }

    public void setSize(final String size) {
        this.size = size;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(final String endpoint) {
        this.endpoint = endpoint;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(final String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(final String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(final String region) {
        this.region = region;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(final String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(final String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(final String projectId) {
        this.projectId = projectId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

}
