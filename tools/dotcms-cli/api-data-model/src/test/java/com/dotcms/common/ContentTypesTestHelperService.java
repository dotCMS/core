package com.dotcms.common;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.ResponseEntityView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

@ApplicationScoped
public class ContentTypesTestHelperService {

    private static final Duration MAX_WAIT_TIME = Duration.ofSeconds(15);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);

    @Inject
    RestClientFactory clientFactory;

    /**
     * Searches for a content type by its variable.
     *
     * @param variable The variable of the content type.
     * @return The content type if found, otherwise an empty optional.
     */
    public Optional<ContentType> findContentType(final String variable) {

        try {

            final AtomicReference<ContentType> contentTypeRef = new AtomicReference<>();

            await()
                    .atMost(MAX_WAIT_TIME)
                    .pollInterval(POLL_INTERVAL)
                    .until(() -> {
                        try {
                            var response = findContentTypeByVariable(variable);
                            if (response != null && response.entity() != null) {
                                contentTypeRef.set(response.entity());
                                return true;
                            }

                            return false;
                        } catch (NotFoundException e) {
                            return false;
                        }
                    });

            ContentType contentType = contentTypeRef.get();
            if (contentType != null) {
                return Optional.of(contentType);
            } else {
                return Optional.empty();
            }
        } catch (ConditionTimeoutException ex) {
            return Optional.empty();
        }
    }

    /**
     * Retrieves a content type by its variable.
     *
     * @param variable The variable of the content type.
     * @return The ResponseEntityView containing the content type.
     */
    @ActivateRequestContext
    public ResponseEntityView<ContentType> findContentTypeByVariable(final String variable) {

        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

        // Execute the REST call to retrieve folder contents
        return contentTypeAPI.getContentType(
                variable, null, null
        );
    }

}
