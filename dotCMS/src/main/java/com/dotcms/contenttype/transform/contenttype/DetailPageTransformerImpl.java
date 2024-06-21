package com.dotcms.contenttype.transform.contenttype;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.api.v1.contenttype.ContentTypeHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class DetailPageTransformerImpl implements DetailPageTransformer {

    private static final String PAGE_SUBTYPE = "htmlpageasset";

    private final ContentType contentType;
    private final User user;

    public DetailPageTransformerImpl(final ContentType contentType, final User user) {
        this.contentType = contentType;
        this.user = user;
    }

    public Optional<String> uriToId()
            throws URISyntaxException, DotDataException, DotSecurityException {

        final var detailPage = contentType.detailPage();

        // Checking if the detail page is a URL
        if (UtilMethods.isSet(detailPage)) {

            if (!UUIDUtil.isUUID(detailPage)) {

                // Evaluate the detail page URI
                final var detailPageURI = new URI(detailPage);
                var path = detailPageURI.getRawPath();

                final Host site = APILocator.getHostAPI()
                        .findByName(detailPageURI.getRawAuthority(), user, false);
                if (null == site) {
                    throw new IllegalArgumentException(
                            String.format("Site [%s] in detail page URL [%s] not found.",
                                    detailPageURI.getRawAuthority(), detailPage));
                }

                if (null == detailPageURI.getRawPath()) {
                    throw new IllegalArgumentException(
                            String.format("Unable to determine detail page URL: [%s].",
                                    detailPage));
                }

                // And finally search for the identifier based on the site and path
                var detailPageIdentifier = APILocator.getIdentifierAPI().find(site, path);
                return validateIdentifier(detailPage, detailPageIdentifier);
            } else {

                // It is already an identifier, but we need to check the detail page exists and has the
                // correct type
                var detailPageIdentifier = APILocator.getIdentifierAPI().find(detailPage);
                return validateIdentifier(detailPage, detailPageIdentifier);
            }
        }

        return Optional.empty();
    }

    public Optional<String> idToUri() throws DotDataException, DotSecurityException {

        final var detailPage = contentType.detailPage();

        // Checking if the detail page is a UUID
        if (UtilMethods.isSet(detailPage) &&
                UUIDUtil.isUUID(detailPage)) {

            // Finding the identifier related to the detail page
            var detailPageIdentifier = APILocator.getIdentifierAPI().find(detailPage);
            if (null != detailPageIdentifier && detailPageIdentifier.exists()) {

                final Host detailPageSite = APILocator.getHostAPI().find(
                        detailPageIdentifier.getHostId(), user, false);

                // Building the detail page URI
                var detailPageURL = String.format(
                        "//%s%s",
                        detailPageSite.getHostname(),
                        detailPageIdentifier.getPath()
                );

                return Optional.of(detailPageURL);
            } else {

                final var message = String.format("Detail page identifier [%s] in "
                        + "Content Type [%s] not found.", detailPage, contentType.name());
                Logger.warn(ContentTypeHelper.class, message);
                throw new DoesNotExistException(message);
            }
        }

        return Optional.empty();
    }

    /**
     * Validates the identifier of a detail page.
     *
     * @param detailPage                The detail page.
     * @param foundDetailPageIdentifier The Identifier of the detail page.
     * @return An Optional containing the identifier value if it is valid.
     * @throws IllegalArgumentException if the identifier is invalid.
     */
    private Optional<String> validateIdentifier(
            String detailPage, Identifier foundDetailPageIdentifier) {

        if (null != foundDetailPageIdentifier &&
                foundDetailPageIdentifier.exists() &&
                foundDetailPageIdentifier.getAssetSubType().equals(PAGE_SUBTYPE)) {
            return Optional.of(foundDetailPageIdentifier.getId());
        } else {
            if (null != foundDetailPageIdentifier &&
                    foundDetailPageIdentifier.exists() &&
                    !foundDetailPageIdentifier.getAssetSubType().equals(PAGE_SUBTYPE)) {
                throw new IllegalArgumentException(
                        String.format("[%s] in Content Type [%s] is not a valid detail page.",
                                detailPage, contentType.name()));
            }

            throw new IllegalArgumentException(
                    String.format("Detail page [%s] in Content Type [%s] does not exist.",
                            detailPage, contentType.name()));
        }
    }

}
