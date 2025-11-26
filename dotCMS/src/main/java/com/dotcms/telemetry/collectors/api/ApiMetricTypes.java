package com.dotcms.telemetry.collectors.api;

import com.dotcms.telemetry.collectors.image.CountOfContentAssetImageBEAPICalls;
import com.dotcms.telemetry.collectors.image.CountOfContentAssetImageFEAPICalls;
import com.dotcms.telemetry.collectors.image.CountOfDAImageBEAPICalls;
import com.dotcms.telemetry.collectors.image.CountOfDAImageFEAPICalls;
import com.dotcms.rest.api.v1.HTTPMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Collection of all the {@link ApiMetricType}
 */
public enum ApiMetricTypes {

    INSTANCE;

    final Collection<ApiMetricType> urlToIntercept;

    ApiMetricTypes() {
        urlToIntercept = new HashSet<>();
        urlToIntercept.add(new CountOfContentAssetImageFEAPICalls());
        urlToIntercept.add(new CountOfDAImageFEAPICalls());
        urlToIntercept.add(new CountOfContentAssetImageBEAPICalls());
        urlToIntercept.add(new CountOfDAImageBEAPICalls());

    }

    public List<ApiMetricType> interceptBy(final HttpServletRequest request) {

        final Optional<HTTPMethod> method = getHttpMethod(request);

        if (method.isPresent()) {
            final String uri = request.getRequestURI().replace("/api/", "");

            return urlToIntercept.stream()
                    .filter(apiMetricType -> apiMetricType.getHttpMethod().equals(method.get()) &&
                            uri.startsWith(apiMetricType.getAPIUrl()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private Optional<HTTPMethod> getHttpMethod(final HttpServletRequest request) {
        try {
            return Optional.of(HTTPMethod.valueOf(request.getMethod()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Collection<? extends ApiMetricType> get() {
        return Collections.unmodifiableCollection(urlToIntercept);
    }

}
