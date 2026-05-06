package com.dotcms.cdn.api;

import com.dotcms.cdn.CDNConstants;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotcms.http.CircuitBreakerUrlBuilder;
import com.dotcms.rest.RestClientBuilder;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DotCDNAPIImpl implements DotCDNAPI {

    private final String accessKey;
    private final long pullZoneId;
    private final String cdnDomain;

    /**
     * Method to load the app secrets into the variables.
     * @param host if is not sent will throw IllegalArgumentException, if sent will try to
     * find the secrets for it, if there is no secrets for the host will use the ones for the System_Host
     */
    DotCDNAPIImpl(final Host host) {
        if (host == null || UtilMethods.isNotSet(host.getIdentifier())) {
            Logger.warn(DotCDNAPIImpl.class, "There is no host sent or found");
            throw new IllegalArgumentException("There is no host sent or found");
        }
        final Optional<AppSecrets> appSecrets = Try.of(() -> APILocator.getAppsAPI()
                .getSecrets(CDNConstants.DOT_CDN_APP_KEY, true, host, APILocator.systemUser()))
                .getOrElse(Optional.empty());
        if (!appSecrets.isPresent()) {
            Logger.warn(DotCDNAPIImpl.class, "There is no config set, please set it via Apps Tool");
            throw new NotFoundException("There is no config set, please set it via Apps Tool");
        }
        this.accessKey = appSecrets.get().getSecrets().get(CDNConstants.DOT_CDN_API_KEY).getString();
        this.cdnDomain = appSecrets.get().getSecrets().get(CDNConstants.DOT_CDN_DOMAIN).getString();
        this.pullZoneId = Long.parseLong(
                appSecrets.get().getSecrets().get(CDNConstants.DOT_CDN_ZONEID).getString());
    }

    /**
     * Url from bunny api to get the stats
     * @param from The start date of the statistics.
     * @param to The end date of the statistics
     * @return url from bunny api with the passed dates and the pullzone
     */
    private static final DateTimeFormatter UTC_DATE_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

    private String statsUrl(final Instant from, final Instant to, final boolean hourly) {
        String url = "https://api.bunny.net/statistics?dateFrom=" + UTC_DATE_FORMATTER.format(from)
                + "&dateTo=" + UTC_DATE_FORMATTER.format(to) + "&pullZone=" + pullZoneId
                + "&loadErrors=true&loadOriginResponseTimes=true";
        if (hourly) {
            url += "&hourly=true";
        }
        Logger.debug(DotCDNAPIImpl.class, "Sending URL:" + url);
        return url;
    }

    CircuitBreakerUrlBuilder urlBuilder(final String url) {
        return new CircuitBreakerUrlBuilder()
                .setHeaders(Map.of("AccessKey", accessKey, "accept", "application/json"))
                .setTimeout(10000)
                .setUrl(url)
                .setMethod(Method.GET);
    }

    /**
     * Method to get the response from the url that was hit.
     * @return if the response was successful the object is returned, if not FAILURE is returned.
     */
    private Map<String, Object> getData(CircuitBreakerUrlBuilder url) {
        final String response = Try.of(() -> url.build().doString())
                .getOrElse("{\"response\":\"FAILURE\"}");
        final JSONObject jsonObject = new JSONObject(response);
        return new HashMap<>(jsonObject);
    }

    /**
     * Logic to get and parse the Stats from bunny to {@link DotCDNStats}
     * @param dateFromStr The start date of the statistics.
     * @param dateToStr The end date of the statistics
     * @return {@link DotCDNStats}
     */
    @Override
    public DotCDNStats getStats(final String dateFromStr, final String dateToStr,
            final boolean hourly) {
        final ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC)
                .withSecond(0).withMinute(0).withHour(0);
        final Instant parsedFrom = Try.of(
                () -> ZonedDateTime.parse(dateFromStr + "T00:00:00Z").toInstant()).getOrNull();
        final Instant parsedTo = Try.of(
                () -> ZonedDateTime.parse(dateToStr + "T00:00:00Z").toInstant()).getOrNull();
        if (parsedFrom == null) {
            Logger.debug(this.getClass().getName(),
                    "dateFrom is not sent or does not comply with the format yyyy-MM-dd, using date of 15 days ago");
        }
        final Instant dateFrom = parsedFrom != null
                ? parsedFrom : nowUtc.minusDays(15).toInstant();
        if (parsedTo == null) {
            Logger.debug(this.getClass().getName(),
                    "dateTo is not sent or does not comply with the format yyyy-MM-dd, using today's date instead");
        }
        final Instant dateTo = parsedTo != null ? parsedTo : nowUtc.toInstant();

        final String statsUrl = statsUrl(dateFrom, dateTo, hourly);
        final Map<String, Object> data = getData(urlBuilder(statsUrl));
        if ("FAILURE".equals(data.get("response"))) {
            final String message = "Failed to get the Stats using the url: " + statsUrl
                    + " , please check the App Configuration";
            Logger.info(this.getClass().getName(), message);
            throw new BadRequestException(message);
        }
        final DotCDNStats.DotStatsBuilder builder = DotCDNStats.builder()
                .withCDNDomain(cdnDomain)
                .withDateFrom(dateFrom.toString())
                .withDateTo(dateTo.toString())
                .withCacheHitRate(numberOrDefault(data.get("CacheHitRate"), 0).doubleValue())
                .withTotalRequestsServed(numberOrDefault(data.get("TotalRequestsServed"), 0).longValue())
                .withTotalBandwidthUsed(numberOrDefault(data.get("TotalBandwidthUsed"), 0).longValue())
                .withAverageOriginResponseTime(
                        numberOrDefault(data.get("AverageOriginResponseTime"), 0).intValue());

        builder.withBandwidthUsedChart(mapOrEmpty(data, "BandwidthUsedChart"));
        builder.withRequestsServedChart(mapOrEmpty(data, "RequestsServedChart"));
        builder.withCacheHitRateChart(mapOrEmpty(data, "CacheHitRateChart"));
        builder.withOriginResponseTimeChart(mapOrEmpty(data, "OriginResponseTimeChart"));
        builder.withError4xxChart(mapOrEmpty(data, "Error4xxChart"));
        builder.withError5xxChart(mapOrEmpty(data, "Error5xxChart"));

        @SuppressWarnings("unchecked")
        final Map<String, Object> incomingGeo =
                (Map<String, Object>) data.getOrDefault("GeoTrafficDistribution",
                        Collections.emptyMap());

        final Map<String, Map<String, Long>> geoMap = new LinkedHashMap<>();
        for (final String key : incomingGeo.keySet()) {
            final String[] keySplit = key.split(":");
            if (keySplit.length < 2) {
                continue;
            }
            final long traffic = numberOrDefault(incomingGeo.get(key), 0).longValue();
            final Map<String, Long> geoEntry = geoMap.computeIfAbsent(keySplit[0],
                    e -> new HashMap<>());
            geoEntry.put(keySplit[1], traffic);
        }
        builder.withGeographicDistribution(geoMap);

        return builder.build();
    }

    @Override
    public boolean invalidateContentlet(final Contentlet contentlet) {
        return this.invalidateContentlet(contentlet, Collections.emptyList());
    }

    @Override
    public boolean invalidateContentlet(final Contentlet contentlet,
            final List<String> urlsToPurgeParam) {

        final List<String> urlsToPurge = new ArrayList<>();
        final List<Contentlet> contentletList = new ArrayList<>();
        contentletList.add(contentlet);

        if (UtilMethods.isSet(urlsToPurgeParam)) {
            urlsToPurge.addAll(urlsToPurgeParam);
        }

        if (contentlet.isHTMLPage()) {
            contentletList.addAll(findPageContent(contentlet));
        } else {
            contentletList.addAll(findPagesUsingContent(contentlet));
        }

        contentletList.forEach(contentletItem -> {
            Try.of(() -> urlsToPurge.addAll(createUrlsToPurgeForContentlet(contentletItem)))
                    .onFailure(e -> Logger.warn(this.getClass().getName(),
                            "Unable to create urls to purge based on the contentlet: "
                                    + e.getMessage()));
        });

        return this.invalidate(urlsToPurge);
    }

    @Override
    public boolean invalidateRelatedPages(final Contentlet contentlet,
            final List<String> urlsToPurgeParam) {

        final List<String> urlsToPurge = new ArrayList<>();

        if (UtilMethods.isSet(urlsToPurgeParam)) {
            urlsToPurge.addAll(urlsToPurgeParam);
        }

        findPagesUsingContent(contentlet).forEach(contentletItem -> {
            Try.of(() -> urlsToPurge.addAll(createUrlsToPurgeForContentlet(contentletItem)))
                    .onFailure(e -> Logger.warn(this.getClass().getName(),
                            "Unable to create urls to purge based on the contentlet: "
                                    + e.getMessage()));
        });

        return this.invalidate(urlsToPurge);
    }

    /**
     * Creates a List of strings (urls) based on the contentlet properties (path, identifier, inode).
     * @param contentlet contentlet which the actionlet is being fired
     * @return list of strings urls
     */
    private List<String> createUrlsToPurgeForContentlet(final Contentlet contentlet) {

        final List<String> urlsForContentlet = new ArrayList<>();

        final Identifier identifier = Try.of(
                () -> APILocator.getIdentifierAPI().find(contentlet.getIdentifier())).getOrNull();
        if (identifier == null) {
            Logger.debug(this.getClass().getName(),
                    "No identifier found for contentlet: " + contentlet.getIdentifier());
            return urlsForContentlet;
        }

        final String path = identifier.getPath();
        urlsForContentlet.add(path);

        if (path.endsWith("/index")) {
            urlsForContentlet.add(path.substring(0, path.length() - 5));
            urlsForContentlet.add(path.substring(0, path.length() - 6));
        }

        final ShortyIdAPI shorty = APILocator.getShortyAPI();
        final String inode = contentlet.getInode();
        final String id = identifier.getId();

        urlsForContentlet.add(buildUrl(id, "*"));
        urlsForContentlet.add(buildUrl(inode, "*"));

        urlsForContentlet.add(buildUrl(shorty.shortify(id), "*"));
        urlsForContentlet.add(buildUrl(shorty.shortify(inode), "*"));

        final String urlMap = Try.of(() -> APILocator.getContentletAPI()
                .getUrlMapForContentlet(contentlet, APILocator.systemUser(), false)).getOrNull();
        if (urlMap != null) {
            urlsForContentlet.add(urlMap);
        }

        urlsForContentlet.removeIf(UtilMethods::isEmpty);

        Logger.debug(this.getClass().getName(),
                () -> "URLs to purge for contentlet: " + urlsForContentlet);

        return urlsForContentlet;
    }

    private List<Contentlet> findPagesUsingContent(final Contentlet contentlet) {
        final List<MultiTree> trees = Try.of(
                () -> APILocator.getMultiTreeAPI().getMultiTreesByChild(contentlet.getIdentifier()))
                .getOrElse(new ArrayList<>());

        return trees.stream()
                .map(tree -> Try.of(() -> APILocator.getContentletAPI()
                        .findContentletByIdentifierAnyLanguage(tree.getHtmlPage())).getOrNull())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<Contentlet> findPageContent(final Contentlet contentlet) {
        final List<MultiTree> trees = Try.of(
                () -> APILocator.getMultiTreeAPI().getMultiTreesByPage(contentlet.getIdentifier()))
                .getOrElse(new ArrayList<>());

        return trees.stream()
                .map(tree -> Try.of(() -> APILocator.getContentletAPI()
                        .findContentletByIdentifierAnyLanguage(tree.getContentlet())).getOrNull())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private String buildUrl(final String... concat) {
        return "/dA/" + String.join(StringPool.SLASH, concat);
    }

    /**
     * Logic to invalidate the List of urls.
     * @param urls List of url to invalidate
     * @return true if all the urls were purged successfully, false if one or more failed.
     */
    @Override
    public boolean invalidate(final List<String> urls) {
        boolean results = true;
        for (final String url : urls) {

            final String urlToPurge = url.startsWith(cdnDomain) ? url :
                    url.startsWith("/") ? cdnDomain + url :
                            cdnDomain + "/" + url;

            Logger.debug(this.getClass().getName(), "Purging URL: " + urlToPurge);

            final CircuitBreakerUrl.Response<String> response = Try.of(() ->
                    this.urlBuilder(invalidateUrl(urlToPurge))
                            .setMethod(Method.POST)
                            .setThrowWhenError(false)
                            .build()
                            .doResponse()
            ).getOrNull();

            if (response == null) {
                Logger.warn(this.getClass().getName(),
                        "Purge failed (no response) for: " + urlToPurge);
                results = false;
            } else if (!CircuitBreakerUrl.isSuccessResponse(response.getStatusCode())) {
                Logger.warn(this.getClass().getName(),
                        "Purge failed for: " + urlToPurge
                                + " - HTTP " + response.getStatusCode()
                                + " - " + response.getResponse());
                results = false;
            } else {
                Logger.debug(this.getClass().getName(), "Purge successful for: " + urlToPurge);
            }
        }
        return results;
    }

    /**
     * Logic to invalidate the entire cache.
     * @return true if the entire cache was invalidated successfully.
     */
    @Override
    public boolean invalidateAll() {
        try (final Response response = RestClientBuilder.newClient().target(invalidateAllUrl())
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header("AccessKey", accessKey)
                .post(Entity.entity("", MediaType.TEXT_PLAIN))) {
            Logger.debug(this.getClass().getName(), "Response: " + response);
            if (response.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
                final String message = "Failed to Purge the entire Cache: " + invalidateAllUrl()
                        + " , please check the App Configuration";
                Logger.info(this.getClass().getName(), message);
                throw new BadRequestException(message);
            }
            return true;
        }
    }

    private String invalidateAllUrl() {
        return "https://api.bunny.net/pullzone/" + this.pullZoneId + "/purgeCache";
    }

    private String invalidateUrl(String url) {
        try {
            return "https://api.bunny.net/purge?url="
                    + URLEncoder.encode(url, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding is not available", e);
        }
    }

    String getCdnDomain() {
        return cdnDomain;
    }

    private static Number numberOrDefault(final Object value, final Number defaultValue) {
        if (value instanceof Number) {
            return (Number) value;
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static <V> Map<String, V> mapOrEmpty(final Map<String, Object> data,
            final String key) {
        final Object value = data.get(key);
        if (value instanceof Map) {
            return (Map<String, V>) value;
        }
        return Collections.emptyMap();
    }
}
