package com.dotcms.rest.api.v1.reportissue;

import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.util.ReleaseInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Try;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Endpoint used by dotCMS UI to report product issues without exposing the upstream reporter token.
 */
@Path("/v1/report-issue")
@Tag(name = "Workflow")
public class ReportIssueResource {

    static final String WORKFLOW_URL_PROPERTY = "REPORT_ISSUE_WORKFLOW_URL";
    static final String DEFAULT_WORKFLOW_URL =
            "https://corpsites-headless.dotcms.cloud/api/v1/workflow/actions/default/fire/NEW";

    static final String ERROR_INVALID_REQUEST = "REPORT_ISSUE_INVALID_REQUEST";
    static final String ERROR_PROXY_NOT_AUTHORIZED = "REPORT_ISSUE_PROXY_NOT_AUTHORIZED";
    static final String ERROR_SERVICE_UNAVAILABLE = "REPORT_ISSUE_SERVICE_UNAVAILABLE";
    static final String ERROR_UPSTREAM_FAILED = "REPORT_ISSUE_UPSTREAM_FAILED";

    static final String REPORT_ISSUE_INCLUDE_USER_PII_PROPERTY = "REPORT_ISSUE_INCLUDE_USER_PII";
    static final boolean DEFAULT_REPORT_ISSUE_INCLUDE_USER_PII = true;

    private static final String DESCRIPTION_FIELD = "description";
    private static final String METADATA_FIELD = "metadata";
    private static final String ANONYMOUS_FIELD = "anonymous";
    private static final String SCREENSHOT_FIELD = "screenshot";
    private static final String FILE_FIELD = "file";
    private static final String CONTENT_TYPE = "Bug";
    private static final String SCREENSHOT_CONTENT_TYPE_FIELD = "screenshot";
    private static final long DEFAULT_MAX_SCREENSHOT_BYTES = 10L * 1024L * 1024L;
    private static final String MAX_SCREENSHOT_BYTES_PROPERTY = "REPORT_ISSUE_SCREENSHOT_MAX_BYTES";
    private static final Set<String> ALLOWED_SCREENSHOT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp"
    );

    private final WebResource webResource;
    private final ReportIssueForwarder forwarder;
    private final ObjectMapper objectMapper;

    public ReportIssueResource() {
        this(new WebResource(), new HttpReportIssueForwarder(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build()));
    }

    ReportIssueResource(final WebResource webResource, final ReportIssueForwarder forwarder) {
        this.webResource = webResource;
        this.forwarder = forwarder;
        this.objectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
    }

    @POST
    @NoCache
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "reportIssue",
            summary = "Report a dotCMS UI issue",
            description = "Creates a Bug contentlet in the configured upstream reporting dotCMS instance.",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Issue reported",
                            content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Invalid report payload",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "502", description = "Reporting service unavailable or unauthorized",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class)))
            }
    )
    public Response reportIssue(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final FormDataMultiPart multipart) {

        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .rejectWhenNoUser(true)
                .init();

        try {
            final ReportIssuePayload payload = buildPayload(request, initDataObject.getUser(), multipart);
            final URI upstreamUri = URI.create(Config.getStringProperty(WORKFLOW_URL_PROPERTY, DEFAULT_WORKFLOW_URL));
            final ReportIssueForwardResponse upstreamResponse = this.forwarder.forward(
                    new ReportIssueForwardRequest(
                            upstreamUri,
                            payload.contentlet(),
                            payload.screenshot(),
                            payload.binaryFields()));

            return mapUpstreamResponse(upstreamResponse);
        } catch (ReportIssueValidationException e) {
            return error(Response.Status.BAD_REQUEST, ERROR_INVALID_REQUEST, e.getMessage());
        } catch (IllegalArgumentException e) {
            Logger.warn(ReportIssueResource.class, "Invalid Report Issue configuration: " + e.getMessage());
            return error(Response.Status.BAD_GATEWAY, ERROR_SERVICE_UNAVAILABLE,
                    "Report issue service is not available.");
        } catch (IOException e) {
            Logger.error(ReportIssueResource.class, "Unable to reach Report Issue service: " + e.getMessage(), e);
            return error(Response.Status.BAD_GATEWAY, ERROR_SERVICE_UNAVAILABLE,
                    "Report issue service is not available.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.error(ReportIssueResource.class, "Report Issue service request was interrupted.", e);
            return error(Response.Status.BAD_GATEWAY, ERROR_SERVICE_UNAVAILABLE,
                    "Report issue service is not available.");
        } catch (Exception e) {
            Logger.error(ReportIssueResource.class, "Unexpected Report Issue error: " + e.getMessage(), e);
            return error(Response.Status.BAD_GATEWAY, ERROR_SERVICE_UNAVAILABLE,
                    "Report issue service is not available.");
        }
    }

    private ReportIssuePayload buildPayload(
            final HttpServletRequest request,
            final User user,
            final FormDataMultiPart multipart) throws IOException {

        if (multipart == null) {
            throw new ReportIssueValidationException("Report issue payload is required.");
        }

        final String description = getRequiredTextField(multipart, DESCRIPTION_FIELD);
        final Map<String, Object> clientMetadata = getClientMetadata(multipart);
        final Optional<ReportIssueScreenshot> screenshot = getScreenshot(multipart);
        final boolean includeUserIdentity = shouldIncludeUserIdentity(multipart);
        final Map<String, Object> metadata = buildMetadata(request, user, clientMetadata, includeUserIdentity);

        final Map<String, Object> contentlet = new LinkedHashMap<>();
        contentlet.put("contentType", CONTENT_TYPE);
        contentlet.put("title", deriveTitle(request, metadata));
        contentlet.put("description", description);
        contentlet.put("metadata", metadata);

        final List<String> binaryFields = screenshot.isPresent()
                ? List.of(SCREENSHOT_CONTENT_TYPE_FIELD)
                : List.of();

        return new ReportIssuePayload(contentlet, screenshot, binaryFields);
    }

    private String getRequiredTextField(final FormDataMultiPart multipart, final String fieldName) {
        final FormDataBodyPart field = multipart.getField(fieldName);
        final String value = field == null ? null : field.getValue();
        if (!UtilMethods.isSet(value) || value.trim().isEmpty()) {
            throw new ReportIssueValidationException("Description is required.");
        }
        return value.trim();
    }

    private Map<String, Object> getClientMetadata(final FormDataMultiPart multipart) throws IOException {
        final FormDataBodyPart metadataField = multipart.getField(METADATA_FIELD);
        if (metadataField == null || !UtilMethods.isSet(metadataField.getValue())) {
            return Map.of();
        }

        try {
            return this.objectMapper.readValue(metadataField.getValue(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new ReportIssueValidationException("Metadata must be a valid JSON object.");
        }
    }

    private Optional<ReportIssueScreenshot> getScreenshot(final FormDataMultiPart multipart) throws IOException {
        final List<FormDataBodyPart> screenshots = new ArrayList<>();
        addAll(screenshots, multipart.getFields(SCREENSHOT_FIELD));
        addAll(screenshots, multipart.getFields(FILE_FIELD));

        if (screenshots.isEmpty()) {
            return Optional.empty();
        }
        if (screenshots.size() > 1) {
            throw new ReportIssueValidationException("Only one screenshot can be uploaded.");
        }

        final FormDataBodyPart screenshot = screenshots.get(0);
        final String mediaType = screenshot.getMediaType() == null
                ? ""
                : screenshot.getMediaType().toString().toLowerCase();
        if (!ALLOWED_SCREENSHOT_TYPES.contains(mediaType)) {
            throw new ReportIssueValidationException("Screenshot must be a PNG, JPEG, or WebP image.");
        }

        final String rawFileName = screenshot.getContentDisposition() == null
                ? "screenshot"
                : screenshot.getContentDisposition().getFileName();
        final String fileName = UtilMethods.isSet(rawFileName)
                ? FileUtil.sanitizeFileName(rawFileName)
                : "screenshot";

        final byte[] bytes;
        try (InputStream inputStream = getScreenshotStream(screenshot)) {
            bytes = readBytesWithLimit(inputStream,
                    Config.getLongProperty(MAX_SCREENSHOT_BYTES_PROPERTY, DEFAULT_MAX_SCREENSHOT_BYTES));
        }

        return Optional.of(new ReportIssueScreenshot(fileName, mediaType, bytes));
    }

    private static void addAll(final List<FormDataBodyPart> destination, final List<FormDataBodyPart> source) {
        if (source != null) {
            destination.addAll(source);
        }
    }

    private static byte[] readBytesWithLimit(final InputStream inputStream, final long maxBytes)
            throws IOException {
        if (inputStream == null) {
            throw new ReportIssueValidationException("Screenshot is invalid.");
        }

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final byte[] buffer = new byte[8192];
        long totalBytes = 0L;
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            totalBytes += bytesRead;
            if (totalBytes > maxBytes) {
                throw new ReportIssueValidationException("Screenshot exceeds the maximum allowed size.");
            }
            outputStream.write(buffer, 0, bytesRead);
        }

        return outputStream.toByteArray();
    }

    private static InputStream getScreenshotStream(final FormDataBodyPart screenshot) {
        if (screenshot instanceof StreamDataBodyPart) {
            return ((StreamDataBodyPart) screenshot).getStreamEntity();
        }

        return screenshot.getEntityAs(InputStream.class);
    }

    private String deriveTitle(final HttpServletRequest request, final Map<String, Object> metadata) {
        final String path = getReportedPath(metadata, request);
        final BrowserDescriptor browserDescriptor = getBrowserDescriptor(metadata);
        final String version = ReleaseInfo.getVersion();

        final StringBuilder title = new StringBuilder(path);
        if (UtilMethods.isSet(version) && browserDescriptor.isPresent()) {
            title.append(" [")
                    .append(version)
                    .append(" - ")
                    .append(browserDescriptor.browser())
                    .append("]");
        } else if (UtilMethods.isSet(version)) {
            title.append(" [").append(version).append("]");
        }

        return title.length() <= 120 ? title.toString() : title.substring(0, 120);
    }

    private String getReportedPath(final Map<String, Object> metadata, final HttpServletRequest request) {
        final String metadataUrl = stringValue(clientMetadata(metadata).get("url"));
        if (UtilMethods.isSet(metadataUrl)) {
            try {
                final URI uri = new URI(metadataUrl);
                if (UtilMethods.isSet(uri.getFragment())) {
                    return normalizeReportedPath(uri.getFragment());
                }
                return normalizeReportedPath(uri.getPath());
            } catch (URISyntaxException e) {
                Logger.debug(ReportIssueResource.class, "Unable to parse report issue URL metadata: " + metadataUrl);
            }
        }

        return normalizeReportedPath(request.getRequestURI());
    }

    private String normalizeReportedPath(final String rawPath) {
        if (!UtilMethods.isSet(rawPath)) {
            return "/unknown";
        }

        String normalized = rawPath.trim();
        final int querySeparator = normalized.indexOf('?');
        if (querySeparator >= 0) {
            normalized = normalized.substring(0, querySeparator);
        }

        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        if (normalized.startsWith("/dotAdmin")) {
            normalized = normalized.substring("/dotAdmin".length());
        }

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        return UtilMethods.isSet(normalized) ? normalized : "/unknown";
    }

    private BrowserDescriptor getBrowserDescriptor(final Map<String, Object> metadata) {
        final String browserMetadata = stringValue(clientMetadata(metadata).get("browser"));
        if (!UtilMethods.isSet(browserMetadata)) {
            return BrowserDescriptor.empty();
        }

        final BrowserDescriptor directDescriptor = parseDirectBrowserDescriptor(browserMetadata);
        if (directDescriptor.isPresent()) {
            return directDescriptor;
        }

        return parseUserAgent(browserMetadata);
    }

    private BrowserDescriptor parseDirectBrowserDescriptor(final String browserMetadata) {
        final String trimmed = browserMetadata.trim();
        final String[] parts = trimmed.split("\\s+");
        if (parts.length < 2) {
            return BrowserDescriptor.empty();
        }

        final String version = parts[parts.length - 1];
        if (!version.matches("\\d+(?:[._]\\d+)*")) {
            return BrowserDescriptor.empty();
        }

        final String browser = trimmed.substring(0, trimmed.length() - version.length()).trim();
        if (!UtilMethods.isSet(browser)) {
            return BrowserDescriptor.empty();
        }

        return new BrowserDescriptor(browser);
    }

    private BrowserDescriptor parseUserAgent(final String userAgent) {
        final List<BrowserPattern> patterns = List.of(
                new BrowserPattern("Edg/", "Edge"),
                new BrowserPattern("OPR/", "Opera"),
                new BrowserPattern("Chrome/", "Chrome"),
                new BrowserPattern("Firefox/", "Firefox"),
                new BrowserPattern("Version/", "Safari")
        );

        for (final BrowserPattern pattern : patterns) {
            final String version = extractVersion(userAgent, pattern.token());
            if (UtilMethods.isSet(version)) {
                return new BrowserDescriptor(pattern.browser());
            }
        }

        return BrowserDescriptor.empty();
    }

    private String extractVersion(final String source, final String token) {
        final int index = source.indexOf(token);
        if (index < 0) {
            return null;
        }

        final int start = index + token.length();
        final StringBuilder version = new StringBuilder();
        for (int i = start; i < source.length(); i++) {
            final char current = source.charAt(i);
            if (Character.isDigit(current) || current == '.' || current == '_') {
                version.append(current);
            } else {
                break;
            }
        }

        return version.length() == 0 ? null : version.toString();
    }

    private String stringValue(final Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> clientMetadata(final Map<String, Object> metadata) {
        final Object client = metadata.get("client");
        return client instanceof Map ? (Map<String, Object>) client : Map.of();
    }

    private Map<String, Object> buildMetadata(
            final HttpServletRequest request,
            final User user,
            final Map<String, Object> clientMetadata,
            final boolean includeUserIdentity) {

        final Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("submittedAt", Instant.now().toString());
        metadata.put("dotcmsVersion", ReleaseInfo.getVersion());
        metadata.put("dotcmsBuildDate", ReleaseInfo.getBuildDateString());
        metadata.put("userAgent", request.getHeader("User-Agent"));
        metadata.put("referer", request.getHeader("Referer"));
        metadata.put("requestUrl", getRequestUrl(request));
        metadata.put("remoteAddress", request.getRemoteAddr());
        metadata.put("serverName", request.getServerName());
        metadata.put("anonymous", !includeUserIdentity);

        if (user != null && includeUserIdentity) {
            final Map<String, Object> userMetadata = new LinkedHashMap<>();
            userMetadata.put("userId", Try.of(user::getUserId).getOrNull());
            userMetadata.put("email", Try.of(user::getEmailAddress).getOrNull());
            userMetadata.put("fullName", Try.of(user::getFullName).getOrNull());
            metadata.put("user", userMetadata);
        }

        if (clientMetadata != null && !clientMetadata.isEmpty()) {
            metadata.put("client", clientMetadata);
        }

        return metadata;
    }

    /**
     * Decides whether to include user identity (userId, email, fullName) in the forwarded
     * payload. The operator config flag is authoritative — when disabled, the user's
     * "anonymous" toggle cannot opt back into sending PII.
     */
    private boolean shouldIncludeUserIdentity(final FormDataMultiPart multipart) {
        final boolean operatorAllowsPII = Config.getBooleanProperty(
                REPORT_ISSUE_INCLUDE_USER_PII_PROPERTY, DEFAULT_REPORT_ISSUE_INCLUDE_USER_PII);
        if (!operatorAllowsPII) {
            return false;
        }

        final FormDataBodyPart anonymousField = multipart.getField(ANONYMOUS_FIELD);
        if (anonymousField == null) {
            return true;
        }
        final String value = anonymousField.getValue();
        return !"true".equalsIgnoreCase(value == null ? "" : value.trim());
    }

    private String getRequestUrl(final HttpServletRequest request) {
        final StringBuffer requestURL = request.getRequestURL();
        if (requestURL == null) {
            return null;
        }

        final String queryString = request.getQueryString();
        return queryString == null ? requestURL.toString() : requestURL + "?" + queryString;
    }

    private Response mapUpstreamResponse(final ReportIssueForwardResponse upstreamResponse) {
        final int statusCode = upstreamResponse.statusCode();

        if (statusCode == Response.Status.UNAUTHORIZED.getStatusCode()
                || statusCode == Response.Status.FORBIDDEN.getStatusCode()) {
            return error(Response.Status.BAD_GATEWAY, ERROR_PROXY_NOT_AUTHORIZED,
                    "Report issue service is not authorized. Check the User Proxy plugin configuration.");
        }

        if (statusCode >= 200 && statusCode < 300) {
            return Response.status(statusCode)
                    .entity(upstreamResponse.body())
                    .type(upstreamResponse.contentType().orElse(MediaType.APPLICATION_JSON))
                    .build();
        }

        if (statusCode >= 400 && statusCode < 500 && UtilMethods.isSet(upstreamResponse.body())) {
            return Response.status(statusCode)
                    .entity(upstreamResponse.body())
                    .type(upstreamResponse.contentType().orElse(MediaType.APPLICATION_JSON))
                    .build();
        }

        Logger.warn(ReportIssueResource.class, "Report Issue upstream failed with status: " + statusCode);
        return error(Response.Status.BAD_GATEWAY, ERROR_UPSTREAM_FAILED,
                "Report issue service failed to process the report.");
    }

    private Response error(final Response.Status status, final String code, final String message) {
        return Response.status(status)
                .entity(new ResponseEntityView<>(List.of(new ErrorEntity(code, message))))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}

final class ReportIssuePayload {
    private final Map<String, Object> contentlet;
    private final Optional<ReportIssueScreenshot> screenshot;
    private final List<String> binaryFields;

    ReportIssuePayload(
            final Map<String, Object> contentlet,
            final Optional<ReportIssueScreenshot> screenshot,
            final List<String> binaryFields) {
        this.contentlet = contentlet;
        this.screenshot = screenshot;
        this.binaryFields = binaryFields;
    }

    Map<String, Object> contentlet() {
        return contentlet;
    }

    Optional<ReportIssueScreenshot> screenshot() {
        return screenshot;
    }

    List<String> binaryFields() {
        return binaryFields;
    }
}

final class ReportIssueScreenshot {
    private final String fileName;
    private final String mediaType;
    private final byte[] bytes;

    ReportIssueScreenshot(final String fileName, final String mediaType, final byte[] bytes) {
        this.fileName = fileName;
        this.mediaType = mediaType;
        this.bytes = bytes;
    }

    String fileName() {
        return fileName;
    }

    String mediaType() {
        return mediaType;
    }

    byte[] bytes() {
        return bytes;
    }
}

final class ReportIssueForwardRequest {
    private final URI upstreamUri;
    private final Map<String, Object> contentlet;
    private final Optional<ReportIssueScreenshot> screenshot;
    private final List<String> binaryFields;

    ReportIssueForwardRequest(
            final URI upstreamUri,
            final Map<String, Object> contentlet,
            final Optional<ReportIssueScreenshot> screenshot,
            final List<String> binaryFields) {
        this.upstreamUri = upstreamUri;
        this.contentlet = contentlet;
        this.screenshot = screenshot;
        this.binaryFields = binaryFields;
    }

    URI upstreamUri() {
        return upstreamUri;
    }

    Map<String, Object> contentlet() {
        return contentlet;
    }

    Optional<ReportIssueScreenshot> screenshot() {
        return screenshot;
    }

    List<String> binaryFields() {
        return binaryFields;
    }
}

final class ReportIssueForwardResponse {
    private final int statusCode;
    private final String body;
    private final Optional<String> contentType;

    ReportIssueForwardResponse(
            final int statusCode,
            final String body,
            final Optional<String> contentType) {
        this.statusCode = statusCode;
        this.body = body;
        this.contentType = contentType;
    }

    int statusCode() {
        return statusCode;
    }

    String body() {
        return body;
    }

    Optional<String> contentType() {
        return contentType;
    }
}

interface ReportIssueForwarder {
    ReportIssueForwardResponse forward(ReportIssueForwardRequest request) throws IOException, InterruptedException;
}

class ReportIssueValidationException extends RuntimeException {
    ReportIssueValidationException(final String message) {
        super(message);
    }
}

class HttpReportIssueForwarder implements ReportIssueForwarder {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    HttpReportIssueForwarder(final HttpClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
    }

    @Override
    public ReportIssueForwardResponse forward(final ReportIssueForwardRequest request)
            throws IOException, InterruptedException {

        final HttpRequest httpRequest = request.screenshot().isPresent()
                ? buildMultipartRequest(request)
                : buildJsonRequest(request);

        final HttpResponse<String> response = this.httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString());

        return new ReportIssueForwardResponse(
                response.statusCode(),
                response.body(),
                response.headers().firstValue("Content-Type"));
    }

    private HttpRequest buildJsonRequest(final ReportIssueForwardRequest request) throws IOException {
        final Map<String, Object> body = Map.of("contentlet", request.contentlet());
        return HttpRequest.newBuilder()
                .uri(request.upstreamUri())
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(this.objectMapper.writeValueAsString(body)))
                .build();
    }

    private HttpRequest buildMultipartRequest(final ReportIssueForwardRequest request) throws IOException {
        final String boundary = "dotcms-report-issue-" + UUID.randomUUID();
        final byte[] body = buildMultipartBody(boundary, request);

        // PUT is required by the upstream dotCMS workflow fire endpoint for multipart
        // submissions (binary fields). The JSON branch above uses POST.
        return HttpRequest.newBuilder()
                .uri(request.upstreamUri())
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
    }

    private byte[] buildMultipartBody(final String boundary, final ReportIssueForwardRequest request)
            throws IOException {

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final String lineBreak = "\r\n";
        final ReportIssueScreenshot screenshot = request.screenshot().orElseThrow();

        final Map<String, Object> jsonBody = new LinkedHashMap<>();
        jsonBody.put("contentlet", request.contentlet());
        jsonBody.put("binaryFields", request.binaryFields());

        outputStream.write(("--" + boundary + lineBreak).getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Disposition: form-data; name=\"json\"" + lineBreak).getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Type: application/json" + lineBreak + lineBreak).getBytes(StandardCharsets.UTF_8));
        outputStream.write(this.objectMapper.writeValueAsBytes(jsonBody));
        outputStream.write(lineBreak.getBytes(StandardCharsets.UTF_8));

        outputStream.write(("--" + boundary + lineBreak).getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\""
                + escapeHeaderValue(screenshot.fileName()) + "\"" + lineBreak).getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Type: " + screenshot.mediaType() + lineBreak + lineBreak).getBytes(StandardCharsets.UTF_8));
        outputStream.write(screenshot.bytes());
        outputStream.write(lineBreak.getBytes(StandardCharsets.UTF_8));

        outputStream.write(("--" + boundary + "--" + lineBreak).getBytes(StandardCharsets.UTF_8));

        return outputStream.toByteArray();
    }

    private String escapeHeaderValue(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

final class BrowserDescriptor {
    private final String browser;

    BrowserDescriptor(final String browser) {
        this.browser = browser;
    }

    static BrowserDescriptor empty() {
        return new BrowserDescriptor(null);
    }

    boolean isPresent() {
        return UtilMethods.isSet(browser);
    }

    String browser() {
        return browser;
    }
}

final class BrowserPattern {
    private final String token;
    private final String browser;

    BrowserPattern(final String token, final String browser) {
        this.token = token;
        this.browser = browser;
    }

    String token() {
        return token;
    }

    String browser() {
        return browser;
    }
}
