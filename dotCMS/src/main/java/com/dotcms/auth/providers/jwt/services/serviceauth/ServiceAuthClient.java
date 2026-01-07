package com.dotcms.auth.providers.jwt.services.serviceauth;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-level HTTP client for authenticated service-to-service communication.
 * Automatically handles JWT generation and injection into requests.
 *
 * <h2>Example Usage:</h2>
 * <pre>
 * // Define your service
 * ServiceCredential wa11yService = ServiceCredential.builder()
 *     .serviceId("wa11y-checker")
 *     .baseUrl("https://wa11y.internal.example.com")
 *     .build();
 *
 * // Create a client
 * ServiceAuthClient client = new ServiceAuthClient(wa11yService);
 *
 * // Make authenticated requests
 * AccessibilityResult result = client.post(
 *     "/api/check",
 *     Map.of("url", "https://mysite.com/page"),
 *     AccessibilityResult.class
 * );
 * </pre>
 *
 * <h2>Configuration:</h2>
 * <ul>
 *   <li>{@code SERVICE_AUTH_HTTP_TIMEOUT_MS} - HTTP timeout (default: 30000)</li>
 *   <li>{@code SERVICE_AUTH_RETRY_ATTEMPTS} - Retry attempts (default: 3)</li>
 * </ul>
 *
 * @author dotCMS
 */
public class ServiceAuthClient implements Serializable {

    private static final long serialVersionUID = 1L;

    // Configuration keys
    public static final String HTTP_TIMEOUT_MS = "SERVICE_AUTH_HTTP_TIMEOUT_MS";
    public static final String RETRY_ATTEMPTS = "SERVICE_AUTH_RETRY_ATTEMPTS";

    // Defaults
    private static final int DEFAULT_TIMEOUT_MS = 30000;
    private static final int DEFAULT_RETRY_ATTEMPTS = 3;

    private final ServiceCredential serviceCredential;
    private final ServiceJwtService jwtService;
    private final ObjectMapper objectMapper;
    private final int timeoutMs;
    private final int retryAttempts;

    // Cache for registered services (optional service registry)
    private static final Map<String, ServiceCredential> SERVICE_REGISTRY = new ConcurrentHashMap<>();

    /**
     * Create a client for a specific service.
     *
     * @param serviceCredential The target service credential
     */
    public ServiceAuthClient(final ServiceCredential serviceCredential) {
        this.serviceCredential = serviceCredential;
        this.jwtService = ServiceJwtService.getInstance();
        this.objectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
        this.timeoutMs = Config.getIntProperty(HTTP_TIMEOUT_MS, DEFAULT_TIMEOUT_MS);
        this.retryAttempts = Config.getIntProperty(RETRY_ATTEMPTS, DEFAULT_RETRY_ATTEMPTS);
    }

    /**
     * Register a service credential for lookup by service ID.
     * Useful for defining services at startup.
     *
     * @param credential The service credential to register
     */
    public static void registerService(final ServiceCredential credential) {
        if (credential != null && UtilMethods.isSet(credential.serviceId())) {
            SERVICE_REGISTRY.put(credential.serviceId(), credential);
            Logger.info(ServiceAuthClient.class,
                    "Registered service: " + credential.serviceId() + " -> " + credential.baseUrl());
        }
    }

    /**
     * Get a registered service by ID.
     *
     * @param serviceId The service ID
     * @return The service credential or null if not found
     */
    public static ServiceCredential getService(final String serviceId) {
        return SERVICE_REGISTRY.get(serviceId);
    }

    /**
     * Create a client for a registered service.
     *
     * @param serviceId The service ID
     * @return A new ServiceAuthClient
     * @throws DotDataException if service is not registered
     */
    public static ServiceAuthClient forService(final String serviceId) throws DotDataException {
        final ServiceCredential credential = SERVICE_REGISTRY.get(serviceId);
        if (credential == null) {
            throw new DotDataException("Service not registered: " + serviceId);
        }
        return new ServiceAuthClient(credential);
    }

    /**
     * Perform an authenticated GET request.
     *
     * @param path The path to append to the base URL
     * @param responseType The expected response type
     * @return The deserialized response
     */
    public <T extends Serializable> T get(final String path, final Class<T> responseType)
            throws DotDataException, DotSecurityException {
        return get(path, Map.of(), responseType);
    }

    /**
     * Perform an authenticated GET request with query parameters.
     *
     * @param path The path to append to the base URL
     * @param params Query parameters
     * @param responseType The expected response type
     * @return The deserialized response
     */
    public <T extends Serializable> T get(final String path, final Map<String, String> params,
                                          final Class<T> responseType)
            throws DotDataException, DotSecurityException {

        final String url = buildUrl(path);
        final String token = jwtService.generateToken(serviceCredential);

        try {
            final CircuitBreakerUrl.Response<T> response = CircuitBreakerUrl.builder()
                    .setUrl(url)
                    .setMethod(CircuitBreakerUrl.Method.GET)
                    .setHeaders(ServiceJwtService.authHeaders(token))
                    .setParams(new HashMap<>(params))
                    .setTimeout(timeoutMs)
                    .setTryAgainAttempts(retryAttempts)
                    .setThrowWhenError(false)
                    .build()
                    .doResponse(responseType);

            return handleResponse(response, url);

        } catch (DotDataException | DotSecurityException e) {
            throw e;
        } catch (Exception e) {
            Logger.error(this, "GET request failed: " + url, e);
            throw new DotDataException("Service request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Perform an authenticated POST request.
     *
     * @param path The path to append to the base URL
     * @param body The request body (will be serialized to JSON)
     * @param responseType The expected response type
     * @return The deserialized response
     */
    public <T extends Serializable> T post(final String path, final Object body,
                                           final Class<T> responseType)
            throws DotDataException, DotSecurityException {

        final String url = buildUrl(path);
        final String token = jwtService.generateToken(serviceCredential);

        try {
            final String jsonBody = body != null ? objectMapper.writeValueAsString(body) : "";

            final CircuitBreakerUrl.Response<T> response = CircuitBreakerUrl.builder()
                    .setUrl(url)
                    .setMethod(CircuitBreakerUrl.Method.POST)
                    .setHeaders(ServiceJwtService.authHeaders(token))
                    .setRawData(jsonBody)
                    .setTimeout(timeoutMs)
                    .setTryAgainAttempts(retryAttempts)
                    .setThrowWhenError(false)
                    .build()
                    .doResponse(responseType);

            return handleResponse(response, url);

        } catch (JsonProcessingException e) {
            throw new DotDataException("Failed to serialize request body", e);
        } catch (DotDataException | DotSecurityException e) {
            throw e;
        } catch (Exception e) {
            Logger.error(this, "POST request failed: " + url, e);
            throw new DotDataException("Service request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Perform an authenticated PUT request.
     *
     * @param path The path to append to the base URL
     * @param body The request body (will be serialized to JSON)
     * @param responseType The expected response type
     * @return The deserialized response
     */
    public <T extends Serializable> T put(final String path, final Object body,
                                          final Class<T> responseType)
            throws DotDataException, DotSecurityException {

        final String url = buildUrl(path);
        final String token = jwtService.generateToken(serviceCredential);

        try {
            final String jsonBody = body != null ? objectMapper.writeValueAsString(body) : "";

            final CircuitBreakerUrl.Response<T> response = CircuitBreakerUrl.builder()
                    .setUrl(url)
                    .setMethod(CircuitBreakerUrl.Method.PUT)
                    .setHeaders(ServiceJwtService.authHeaders(token))
                    .setRawData(jsonBody)
                    .setTimeout(timeoutMs)
                    .setTryAgainAttempts(retryAttempts)
                    .setThrowWhenError(false)
                    .build()
                    .doResponse(responseType);

            return handleResponse(response, url);

        } catch (JsonProcessingException e) {
            throw new DotDataException("Failed to serialize request body", e);
        } catch (DotDataException | DotSecurityException e) {
            throw e;
        } catch (Exception e) {
            Logger.error(this, "PUT request failed: " + url, e);
            throw new DotDataException("Service request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Perform an authenticated DELETE request.
     *
     * @param path The path to append to the base URL
     * @param responseType The expected response type
     * @return The deserialized response
     */
    public <T extends Serializable> T delete(final String path, final Class<T> responseType)
            throws DotDataException, DotSecurityException {

        final String url = buildUrl(path);
        final String token = jwtService.generateToken(serviceCredential);

        try {
            final CircuitBreakerUrl.Response<T> response = CircuitBreakerUrl.builder()
                    .setUrl(url)
                    .setMethod(CircuitBreakerUrl.Method.DELETE)
                    .setHeaders(ServiceJwtService.authHeaders(token))
                    .setTimeout(timeoutMs)
                    .setTryAgainAttempts(retryAttempts)
                    .setThrowWhenError(false)
                    .build()
                    .doResponse(responseType);

            return handleResponse(response, url);

        } catch (DotDataException | DotSecurityException e) {
            throw e;
        } catch (Exception e) {
            Logger.error(this, "DELETE request failed: " + url, e);
            throw new DotDataException("Service request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Perform an authenticated POST and return raw string response.
     * Useful when you need the raw JSON response.
     *
     * @param path The path to append to the base URL
     * @param body The request body
     * @return The raw response string
     */
    public String postRaw(final String path, final Object body)
            throws DotDataException, DotSecurityException {

        final String url = buildUrl(path);
        final String token = jwtService.generateToken(serviceCredential);

        try {
            final String jsonBody = body != null ? objectMapper.writeValueAsString(body) : "";

            final CircuitBreakerUrl circuitBreakerUrl = CircuitBreakerUrl.builder()
                    .setUrl(url)
                    .setMethod(CircuitBreakerUrl.Method.POST)
                    .setHeaders(ServiceJwtService.authHeaders(token))
                    .setRawData(jsonBody)
                    .setTimeout(timeoutMs)
                    .setTryAgainAttempts(retryAttempts)
                    .setThrowWhenError(false)
                    .build();

            final String response = circuitBreakerUrl.doString();
            final int statusCode = circuitBreakerUrl.response();

            if (statusCode >= 400) {
                handleErrorStatus(statusCode, url, response);
            }

            return response;

        } catch (JsonProcessingException e) {
            throw new DotDataException("Failed to serialize request body", e);
        } catch (IOException e) {
            throw new DotDataException("Service request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check if the service is reachable.
     *
     * @return true if the service responds to a ping
     */
    public boolean ping() {
        try {
            return CircuitBreakerUrl.builder()
                    .setUrl(serviceCredential.baseUrl())
                    .doPing()
                    .setTimeout(5000)
                    .build()
                    .ping();
        } catch (Exception e) {
            Logger.debug(this, "Ping failed for " + serviceCredential.serviceId() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the service credential this client is configured for.
     */
    public ServiceCredential getServiceCredential() {
        return serviceCredential;
    }

    private String buildUrl(final String path) {
        final String baseUrl = serviceCredential.baseUrl();
        if (!UtilMethods.isSet(path)) {
            return baseUrl;
        }

        final String cleanBase = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        final String cleanPath = path.startsWith("/") ? path : "/" + path;

        return cleanBase + cleanPath;
    }

    private <T extends Serializable> T handleResponse(final CircuitBreakerUrl.Response<T> response,
                                                      final String url)
            throws DotDataException, DotSecurityException {

        if (response == null) {
            throw new DotDataException("No response from service: " + url);
        }

        final int statusCode = response.getStatusCode();

        if (statusCode >= 400) {
            handleErrorStatus(statusCode, url, String.valueOf(response.getResponse()));
        }

        return response.getResponse();
    }

    private void handleErrorStatus(final int statusCode, final String url, final String body)
            throws DotDataException, DotSecurityException {

        final String message = String.format(
                "Service %s returned error %d for %s: %s",
                serviceCredential.serviceId(), statusCode, url,
                body != null ? body.substring(0, Math.min(200, body.length())) : "no body"
        );

        if (statusCode == 401 || statusCode == 403) {
            throw new DotSecurityException(message);
        }

        throw new DotDataException(message);
    }
}
